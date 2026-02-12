# Firebase Authentication Migration Guide

This guide walks you through setting up Firebase Authentication for the SmackCheck app after migrating from Supabase Auth.

## Table of Contents
1. [Firebase Project Setup](#firebase-project-setup)
2. [Download Configuration Files](#download-configuration-files)
3. [Configure Environment Variables](#configure-environment-variables)
4. [Android Configuration](#android-configuration)
5. [iOS Configuration](#ios-configuration)
6. [OAuth Provider Setup](#oauth-provider-setup)
7. [Testing](#testing)
8. [Troubleshooting](#troubleshooting)

---

## Firebase Project Setup

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" or select an existing project
3. Enter project name (e.g., "SmackCheck")
4. (Optional) Enable Google Analytics
5. Click "Create project"

### 2. Enable Authentication Methods

1. In Firebase Console, go to **Authentication** → **Sign-in method**
2. Enable the following providers:
   - **Email/Password**: Click, toggle "Enable", Save
   - **Google**: Click, toggle "Enable", Save
   - **Facebook**: Click, toggle "Enable", enter Facebook App ID and App Secret (see Facebook setup below), Save
   - **Apple**: Click, toggle "Enable", Save

---

## Download Configuration Files

### Android Configuration File

1. In Firebase Console, click the **Settings** gear icon → **Project settings**
2. Scroll to "Your apps" section
3. Click the **Android** icon to add an Android app (or select existing)
4. Enter Android package name: `com.example.smackcheck2`
5. Download `google-services.json`
6. Place it in: `Dishrating_app2/composeApp/google-services.json`

### iOS Configuration File

1. In Firebase Console, go to **Project settings**
2. Scroll to "Your apps" section
3. Click the **iOS** icon to add an iOS app (or select existing)
4. Enter iOS bundle ID: `com.example.smackcheck2`
5. Download `GoogleService-Info.plist`
6. Place it in: `Dishrating_app2/iosApp/iosApp/GoogleService-Info.plist`

---

## Configure Environment Variables

### 1. Update `local.properties` (Android)

Add these lines to `Dishrating_app2/local.properties`:

```properties
# Existing Supabase config (keep for database)
SUPABASE_URL=your_supabase_url
SUPABASE_ANON_KEY=your_supabase_anon_key

# Existing other keys
GOOGLE_MAPS_API_KEY=your_google_maps_key
GEMINI_API_KEY=your_gemini_key

# NEW: Firebase configuration
FIREBASE_WEB_CLIENT_ID=your_firebase_web_client_id
FACEBOOK_APP_ID=your_facebook_app_id
```

### 2. Get Firebase Web Client ID

1. Open `google-services.json`
2. Find the `"oauth_client"` array
3. Look for the client with `"client_type": 3` (Web client)
4. Copy the `"client_id"` value
5. Use this as `FIREBASE_WEB_CLIENT_ID`

Example:
```json
{
  "client_id": "123456789-abcdefghijk.apps.googleusercontent.com",
  "client_type": 3,
  ...
}
```

### 3. Update iOS Configuration

Edit `Dishrating_app2/composeApp/src/iosMain/kotlin/com/example/smackcheck2/data/FirebaseConfig.ios.kt`:

```kotlin
actual object FirebaseConfig {
    actual val FIREBASE_WEB_CLIENT_ID: String = "YOUR_FIREBASE_WEB_CLIENT_ID_HERE"
    actual val FACEBOOK_APP_ID: String = "YOUR_FACEBOOK_APP_ID_HERE"
}
```

Or use a plist file for better security.

---

## Android Configuration

### 1. Add SHA-1 Fingerprint (for Google Sign-In)

#### Get Debug SHA-1:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

#### Get Release SHA-1:
```bash
keytool -list -v -keystore /path/to/your/release.keystore -alias your_alias
```

#### Add to Firebase:
1. Go to Firebase Console → **Project settings**
2. Scroll to "Your apps" → Select Android app
3. Click "Add fingerprint"
4. Paste SHA-1 fingerprint
5. Download updated `google-services.json`

### 2. Update AndroidManifest.xml

Add Facebook metadata to `Dishrating_app2/composeApp/src/androidMain/AndroidManifest.xml`:

```xml
<application>
    <!-- Existing content -->

    <!-- Facebook Configuration -->
    <meta-data
        android:name="com.facebook.sdk.ApplicationId"
        android:value="@string/facebook_app_id" />

    <meta-data
        android:name="com.facebook.sdk.ClientToken"
        android:value="@string/facebook_client_token" />
</application>
```

---

## iOS Configuration

### 1. Add GoogleService-Info.plist to Xcode

1. Open `Dishrating_app2/iosApp/iosApp.xcodeproj` in Xcode
2. Drag `GoogleService-Info.plist` into the project (check "Copy items if needed")
3. Ensure it's added to the target

### 2. Configure URL Schemes

Edit `Dishrating_app2/iosApp/iosApp/Info.plist`:

```xml
<key>CFBundleURLTypes</key>
<array>
    <!-- Google Sign-In -->
    <dict>
        <key>CFBundleTypeRole</key>
        <string>Editor</string>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>com.googleusercontent.apps.YOUR_REVERSED_CLIENT_ID</string>
        </array>
    </dict>

    <!-- Facebook -->
    <dict>
        <key>CFBundleTypeRole</key>
        <string>Editor</string>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>fbYOUR_FACEBOOK_APP_ID</string>
        </array>
    </dict>
</array>

<!-- Google Sign-In -->
<key>GIDClientID</key>
<string>YOUR_CLIENT_ID.apps.googleusercontent.com</string>

<!-- Facebook App ID -->
<key>FacebookAppID</key>
<string>YOUR_FACEBOOK_APP_ID</string>
<key>FacebookClientToken</key>
<string>YOUR_FACEBOOK_CLIENT_TOKEN</string>
<key>FacebookDisplayName</key>
<string>SmackCheck</string>

<!-- For iOS 9+ -->
<key>LSApplicationQueriesSchemes</key>
<array>
    <string>fbapi</string>
    <string>fb-messenger-share-api</string>
    <string>fbauth2</string>
    <string>fbshareextension</string>
</array>
```

**To get Reversed Client ID:**
1. Open `GoogleService-Info.plist`
2. Find `REVERSED_CLIENT_ID`
3. Use this value (e.g., `com.googleusercontent.apps.123456789-abc`)

---

## OAuth Provider Setup

### Google Sign-In

#### Firebase Console:
1. Authentication → Sign-in method → Google
2. Enable provider
3. Add support email
4. Save

#### Android:
- SHA-1 fingerprint must be added (see above)
- `FIREBASE_WEB_CLIENT_ID` must be in `local.properties`

#### iOS:
- URL scheme must be added to Info.plist
- `GIDClientID` must be set in Info.plist

### Facebook Sign-In

#### 1. Create Facebook App:
1. Go to [Facebook Developers](https://developers.facebook.com/)
2. Create a new app (type: "Consumer")
3. Add "Facebook Login" product
4. Configure settings:
   - **Valid OAuth Redirect URIs**: `https://YOUR_PROJECT_ID.firebaseapp.com/__/auth/handler`
   - Get Project ID from Firebase Console URL

#### 2. Get Facebook Credentials:
- **App ID**: Found in App Settings → Basic
- **App Secret**: Found in App Settings → Basic
- **Client Token**: Found in App Settings → Advanced

#### 3. Configure Firebase:
1. Firebase Console → Authentication → Sign-in method → Facebook
2. Enable Facebook
3. Enter App ID and App Secret
4. Copy OAuth redirect URI
5. Add redirect URI to Facebook App settings

#### 4. Add to Environment:
```properties
# local.properties
FACEBOOK_APP_ID=your_facebook_app_id
```

#### 5. Android Configuration:
Ensure `AndroidManifest.xml` has Facebook metadata (see above)

#### 6. iOS Configuration:
Ensure `Info.plist` has Facebook settings (see above)

### Apple Sign-In

#### Firebase Console:
1. Authentication → Sign-in method → Apple
2. Enable provider
3. Save

#### iOS Configuration:
1. In Xcode, go to target → Signing & Capabilities
2. Click "+ Capability"
3. Add "Sign in with Apple"

#### Android Configuration:
Apple Sign-In on Android uses web-based flow. The implementation in `FirebaseAuthProvider.android.kt` needs additional configuration, which is marked as "not_implemented" and requires web OAuth flow setup.

---

## Testing

### Email/Password Authentication

1. Run the app
2. Go to Sign Up screen
3. Enter name, email, password
4. Submit
5. Verify user appears in:
   - Firebase Console → Authentication → Users
   - Supabase → Table Editor → profiles

### Google Sign-In

1. Run the app
2. Click "Sign in with Google"
3. Select Google account
4. Verify successful sign-in
5. Check Firebase and Supabase for user record

### Facebook Sign-In

1. Run the app
2. Click "Sign in with Facebook"
3. Enter Facebook credentials
4. Verify successful sign-in
5. Check Firebase and Supabase for user record

### Apple Sign-In

1. Run the app on iOS device (simulator may have limitations)
2. Click "Sign in with Apple"
3. Use Face ID / Touch ID
4. Verify successful sign-in

---

## Troubleshooting

### Google Sign-In Errors

**Error: "12500" (Sign-in failed)**
- Ensure SHA-1 fingerprint is added to Firebase Console
- Download and replace `google-services.json`
- Rebuild the app

**Error: "Invalid audience"**
- Check `FIREBASE_WEB_CLIENT_ID` matches the Web client ID from `google-services.json`
- Ensure you're using the correct client (type 3)

### Facebook Sign-In Errors

**Error: "App not configured"**
- Verify Facebook App ID is correct in `local.properties` / `FirebaseConfig.ios.kt`
- Check `AndroidManifest.xml` / `Info.plist` has Facebook metadata
- Ensure OAuth redirect URI is added to Facebook App settings

**Error: "Invalid scopes"**
- Facebook App must have "email" permission
- Check App Review status in Facebook Developer Console

### Apple Sign-In Errors

**Error: "not_implemented"**
- iOS: Ensure "Sign in with Apple" capability is added in Xcode
- Android: Requires additional web OAuth configuration

### Build Errors

**"BuildConfig not found"**
- Ensure `buildConfig = true` in `build.gradle.kts`
- Sync Gradle files
- Rebuild project

**"FIREBASE_WEB_CLIENT_ID not found"**
- Add to `local.properties` for Android
- Update `FirebaseConfig.ios.kt` for iOS

### Session Not Persisting

- Firebase handles session persistence automatically
- Check that `FirebaseClientProvider.initialize()` is called before any auth operations
- For Android: Check in `MainActivity.onCreate()`
- For iOS: Check in `MainViewController()`

---

## Migration Checklist

- [ ] Created Firebase project
- [ ] Enabled Email/Password, Google, Facebook, Apple auth methods
- [ ] Downloaded `google-services.json` (Android)
- [ ] Downloaded `GoogleService-Info.plist` (iOS)
- [ ] Added configuration files to project
- [ ] Updated `local.properties` with Firebase config
- [ ] Updated `FirebaseConfig.ios.kt` for iOS
- [ ] Added SHA-1 fingerprint to Firebase (Android)
- [ ] Configured Google Sign-In URL scheme (iOS)
- [ ] Created Facebook App and configured OAuth
- [ ] Added Facebook credentials to environment
- [ ] Configured Facebook in AndroidManifest.xml / Info.plist
- [ ] Added Apple Sign-In capability (iOS)
- [ ] Tested email/password authentication
- [ ] Tested Google Sign-In
- [ ] Tested Facebook Sign-In
- [ ] Tested Apple Sign-In
- [ ] Verified users appear in both Firebase and Supabase

---

## Additional Resources

- [Firebase Authentication Documentation](https://firebase.google.com/docs/auth)
- [GitLive Firebase KMP](https://github.com/GitLiveApp/firebase-kotlin-sdk)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android)
- [Google Sign-In for iOS](https://developers.google.com/identity/sign-in/ios)
- [Facebook Login Documentation](https://developers.facebook.com/docs/facebook-login)
- [Apple Sign In Documentation](https://developer.apple.com/sign-in-with-apple/)

---

## Support

If you encounter issues:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review Firebase Console logs
3. Check Android Logcat / iOS Console for error messages
4. Verify all configuration files are in the correct locations
5. Ensure all environment variables are set correctly

**Important Notes:**
- Keep `google-services.json` and `GoogleService-Info.plist` out of version control
- Add them to `.gitignore`
- Use separate Firebase projects for development and production
- Rotate Facebook App Secret regularly
- Test on both debug and release builds
