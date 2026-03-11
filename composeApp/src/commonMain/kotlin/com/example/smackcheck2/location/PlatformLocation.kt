package com.example.smackcheck2.location

/**
 * Platform-specific function to request location detection.
 *
 * On Android: Checks permission, requests if needed, then triggers FusedLocationProviderClient.
 * On iOS: Would use CLLocationManager (not implemented in this version).
 *
 * The result will be pushed into [SharedLocationState] asynchronously.
 */
expect fun requestCurrentLocationDetection()
