package com.example.smackcheck2.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume

/**
 * iOS implementation of ImagePicker using UIImagePickerController (camera)
 * and PHPickerViewController (gallery).
 */
@OptIn(ExperimentalForeignApi::class)
actual class ImagePicker {

    // Strong references to prevent Kotlin/Native GC from collecting delegates.
    // UIKit holds only a weak reference to delegates, so without these the delegate
    // gets garbage collected before the picker calls back.
    private var singlePickerDelegate: NSObject? = null
    private var multiplePickerDelegate: NSObject? = null

    /**
     * Capture an image using the device camera.
     * Presents UIImagePickerController with camera source type.
     * Returns null if capture is cancelled or fails.
     */
    actual suspend fun captureImage(): ImageResult? {
        if (!UIImagePickerController.isSourceTypeAvailable(
                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
            )
        ) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            var resumed = false

            val delegate = object : NSObject(),
                UIImagePickerControllerDelegateProtocol,
                UINavigationControllerDelegateProtocol {

                override fun imagePickerController(
                    picker: UIImagePickerController,
                    didFinishPickingMediaWithInfo: Map<Any?, *>
                ) {
                    if (!resumed) {
                        resumed = true
                        picker.dismissViewControllerAnimated(true, completion = null)

                        val image = (didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage]
                            ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage]) as? UIImage

                        if (image != null) {
                            val result = uiImageToImageResult(image)
                            continuation.resume(result)
                        } else {
                            continuation.resume(null)
                        }
                    }
                }

                override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                    if (!resumed) {
                        resumed = true
                        picker.dismissViewControllerAnimated(true, completion = null)
                        continuation.resume(null)
                    }
                }
            }

            val rootViewController = getRootViewController()
            if (rootViewController == null) {
                if (!resumed) {
                    resumed = true
                    continuation.resume(null)
                }
                return@suspendCancellableCoroutine
            }

            val pickerController = UIImagePickerController().apply {
                this.sourceType =
                    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                this.delegate = delegate
            }

            rootViewController.presentViewController(pickerController, animated = true, completion = null)

            continuation.invokeOnCancellation {
                pickerController.dismissViewControllerAnimated(true, completion = null)
            }
        }
    }

    /**
     * Pick an image from the device gallery.
     * Presents PHPickerViewController for image selection.
     * Returns null if selection is cancelled or fails.
     */
    actual suspend fun pickFromGallery(): ImageResult? {
        return suspendCancellableCoroutine { continuation ->
            var resumed = false

            val configuration = PHPickerConfiguration().apply {
                this.selectionLimit = 1
                this.filter = PHPickerFilter.imagesFilter
            }

            val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
                override fun picker(
                    picker: PHPickerViewController,
                    didFinishPicking: List<*>
                ) {
                    singlePickerDelegate = null
                    picker.dismissViewControllerAnimated(true, completion = null)

                    if (resumed) return

                    val results = didFinishPicking.filterIsInstance<PHPickerResult>()
                    val firstResult = results.firstOrNull()

                    if (firstResult == null) {
                        resumed = true
                        continuation.resume(null)
                        return
                    }

                    val itemProvider = firstResult.itemProvider
                    if (itemProvider.hasItemConformingToTypeIdentifier(UTTypeImage.identifier)) {
                        itemProvider.loadDataRepresentationForTypeIdentifier(
                            UTTypeImage.identifier
                        ) { data, error ->
                            if (!resumed) {
                                resumed = true
                                if (data != null && error == null) {
                                    val bytes = nsDataToByteArray(data)
                                    if (bytes != null) {
                                        continuation.resume(
                                            ImageResult(
                                                uri = "picker://${firstResult.hashCode()}",
                                                bytes = bytes,
                                                mimeType = "image/jpeg"
                                            )
                                        )
                                    } else {
                                        continuation.resume(null)
                                    }
                                } else {
                                    continuation.resume(null)
                                }
                            }
                        }
                    } else {
                        resumed = true
                        continuation.resume(null)
                    }
                }
            }
            singlePickerDelegate = delegate

            val rootViewController = getRootViewController()
            if (rootViewController == null) {
                singlePickerDelegate = null
                if (!resumed) {
                    resumed = true
                    continuation.resume(null)
                }
                return@suspendCancellableCoroutine
            }

            val pickerVC = PHPickerViewController(configuration = configuration).apply {
                this.delegate = delegate
            }

            rootViewController.presentViewController(pickerVC, animated = true, completion = null)

            continuation.invokeOnCancellation {
                singlePickerDelegate = null
                pickerVC.dismissViewControllerAnimated(true, completion = null)
            }
        }
    }
    
    /**
     * Pick multiple images from the device gallery.
     * Presents PHPickerViewController for multiple image selection.
     * Returns empty list if selection is cancelled or fails.
     * @param maxImages Maximum number of images to select (default 5)
     */
    actual suspend fun pickMultipleFromGallery(maxImages: Int): List<ImageResult> {
        return suspendCancellableCoroutine { continuation ->
            var resumed = false

            val configuration = PHPickerConfiguration().apply {
                this.selectionLimit = maxImages.toLong()
                this.filter = PHPickerFilter.imagesFilter
            }

            val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
                override fun picker(
                    picker: PHPickerViewController,
                    didFinishPicking: List<*>
                ) {
                    multiplePickerDelegate = null
                    picker.dismissViewControllerAnimated(true, completion = null)

                    if (resumed) return

                    val results = didFinishPicking.filterIsInstance<PHPickerResult>()

                    if (results.isEmpty()) {
                        resumed = true
                        continuation.resume(emptyList())
                        return
                    }

                    val imageResults = mutableListOf<ImageResult>()
                    var processedCount = 0
                    val totalCount = results.size

                    results.forEach { result ->
                        val itemProvider = result.itemProvider
                        if (itemProvider.hasItemConformingToTypeIdentifier(UTTypeImage.identifier)) {
                            itemProvider.loadDataRepresentationForTypeIdentifier(
                                UTTypeImage.identifier
                            ) { data, error ->
                                processedCount++
                                if (data != null && error == null) {
                                    val bytes = nsDataToByteArray(data)
                                    if (bytes != null) {
                                        // Note: This callback runs on main thread, so direct access is safe
                                        imageResults.add(
                                            ImageResult(
                                                uri = "picker://${result.hashCode()}",
                                                bytes = bytes,
                                                mimeType = "image/jpeg"
                                            )
                                        )
                                    }
                                }

                                // When all items are processed, resume
                                if (processedCount == totalCount && !resumed) {
                                    resumed = true
                                    continuation.resume(imageResults.toList())
                                }
                            }
                        } else {
                            processedCount++
                            if (processedCount == totalCount && !resumed) {
                                resumed = true
                                continuation.resume(imageResults.toList())
                            }
                        }
                    }
                }
            }
            multiplePickerDelegate = delegate

            val rootViewController = getRootViewController()
            if (rootViewController == null) {
                multiplePickerDelegate = null
                if (!resumed) {
                    resumed = true
                    continuation.resume(emptyList())
                }
                return@suspendCancellableCoroutine
            }

            val pickerVC = PHPickerViewController(configuration = configuration).apply {
                this.delegate = delegate
            }

            rootViewController.presentViewController(pickerVC, animated = true, completion = null)

            continuation.invokeOnCancellation {
                multiplePickerDelegate = null
                pickerVC.dismissViewControllerAnimated(true, completion = null)
            }
        }
    }

    /**
     * Check if camera permission is granted.
     * Uses AVCaptureDevice.authorizationStatusForMediaType.
     */
    actual fun hasCameraPermission(): Boolean {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        return status == AVAuthorizationStatusAuthorized
    }

    // --- Private helpers ---

    /**
     * Convert a UIImage to an ImageResult by encoding it as JPEG.
     */
    private fun uiImageToImageResult(image: UIImage): ImageResult? {
        val jpegData = UIImageJPEGRepresentation(image, 0.85) ?: return null
        val bytes = nsDataToByteArray(jpegData) ?: return null
        return ImageResult(
            uri = "camera://${image.hashCode()}",
            bytes = bytes,
            mimeType = "image/jpeg"
        )
    }

    /**
     * Convert NSData to ByteArray.
     */
    private fun nsDataToByteArray(data: NSData): ByteArray? {
        val length = data.length.toInt()
        if (length == 0) return null
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        return bytes
    }

    /**
     * Get the root UIViewController for presenting controllers.
     */
    @Suppress("DEPRECATION")
    private fun getRootViewController(): UIViewController? {
        val window = UIApplication.sharedApplication.keyWindow
        return window?.rootViewController
    }
}
