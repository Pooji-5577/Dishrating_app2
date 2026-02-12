# Firebase Setup Steps for SmackCheck

## ✅ What's Already Done

The Gradle configuration is complete:
- ✅ Google Services plugin added to root build.gradle.kts
- ✅ Google Services plugin applied in app-level build.gradle.kts
- ✅ Firebase KMP dependencies configured
- ✅ OAuth provider dependencies added

---

## 📋 What You Need to Do Now

### Step 1: Register New App in Firebase (Package Name Mismatch)

Your current Firebase project has:
- Android: `com.example.smackcheck.android`
- Your code uses: `com.example.smackcheck2`

**You need to add a NEW Android app with the correct package name:**

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your "SmackCheck" project
3. Click the **Settings** gear icon → **Project settings**
4. Scroll to "Your apps" section
5. Click **"Add app"** → Select **Android**
6. Enter:
   - **Package name**: `com.example.smackcheck2`
   - **App nickname**: SmackCheck (optional)
   - Click **"Register app"**

### Step 2: Download google-services.json

1. After registering, Firebase will show you the download button
2. Click **"Download google-services.json"**
3. Place the file here:
   ```
   Dishrating_app2/composeApp/google-services.json
   ```

**Important**: This file should NOT be committed to Git (already in .gitignore)

### Step 3: Add SHA-1 Fingerprint (Required for Google Sign-In)

#### Generate SHA-1:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy the SHA-1 fingerprint from the output (looks like: `A1:B2:C3:...`)

#### Add to Firebase:
1. In Firebase Console → **Project settings**
2. Find your Android app (`com.example.smackcheck2`)
3. Scroll down to **"SHA certificate fingerprints"**
4. Click **"Add fingerprint"**
5. Paste the SHA-1
6. Click **"Save"**
7. **Download the updated google-services.json again** (important!)

### Step 4: Get Firebase Web Client ID

1. Open the `google-services.json` file you just downloaded
2. Find the `"oauth_client"` array
3. Look for the entry with `"client_type": 3` (Web client)
4. Copy the `"client_id"` value

Example:
```json
{
  "oauth_client": [
    {
      "client_id": "123456789-abc123.apps.googleusercontent.com",
      "client_type": 3
    }
  ]
}
```

### Step 5: Update local.properties

Create/edit `Dishrating_app2/local.properties` and add:

```properties
# Existing Supabase config
SUPABASE_URL=https://ayopmvhtfuwbsjxhpfgd.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF5b3Btdmh0ZnV3YnNqeGhwZmdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjkyNjAyMTksImV4cCI6MjA4NDgzNjIxOX0.2siGUJfE3iLoaEKae5gycw_6mo748KKyi5C7YEHuUlQ

# Existing other keys
GOOGLE_MAPS_API_KEY=your_google_maps_api_key
GEMINI_API_KEY=your_gemini_api_key

# NEW: Firebase Web Client ID (from google-services.json)
FIREBASE_WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID_FROM_STEP_4

# NEW: Facebook App ID (get from Facebook Developers Console)
FACEBOOK_APP_ID=your_facebook_app_id
```

### Step 6: Configure iOS App (if not already done)

1. In Firebase Console → **Project settings**
2. Click **"Add app"** → Select **iOS**
3. Enter:
   - **Bundle ID**: `com.example.smackcheck2`
   - Click **"Register app"**
4. Download `GoogleService-Info.plist`
5. Place here:
   ```
   Dishrating_app2/iosApp/iosApp/GoogleService-Info.plist
   ```

### Step 7: Update iOS FirebaseConfig

Edit `Dishrating_app2/composeApp/src/iosMain/kotlin/com/example/smackcheck2/data/FirebaseConfig.ios.kt`:

```kotlin
actual object FirebaseConfig {
    actual val FIREBASE_WEB_CLIENT_ID: String = "YOUR_WEB_CLIENT_ID_HERE"
    actual val FACEBOOK_APP_ID: String = "YOUR_FACEBOOK_APP_ID_HERE"
}
```

### Step 8: Enable Authentication Methods in Firebase

1. In Firebase Console, go to **Authentication** → **Sign-in method**
2. Enable:
   - ✅ **Email/Password** - Click, toggle on, Save
   - ✅ **Google** - Click, toggle on, add support email, Save
   - ✅ **Facebook** - Click, toggle on (you'll configure this next)
   - ✅ **Apple** - Click, toggle on, Save

### Step 9: Configure Facebook Sign-In

Now go back to the Facebook provider configuration screen (the one in your screenshot):

1. **Get Facebook App ID and Secret**:
   - Go to [Facebook Developers](https://developers.facebook.com/)
   - Select your app or create a new one
   - Go to **Settings** → **Basic**
   - Copy **App ID** and **App Secret**

2. **Add to Firebase**:
   - In Firebase Console → **Authentication** → **Sign-in method** → **Facebook**
   - Paste App ID and App Secret
   - Copy the OAuth redirect URI shown
   - Click **"Save"**

3. **Configure Facebook App**:
   - Go back to Facebook Developers Console
   - Click **Facebook Login** → **Settings**
   - Add the Firebase OAuth redirect URI to **"Valid OAuth Redirect URIs"**
   - Save changes

4. **Add Facebook credentials to your app**:
   - Update `local.properties` with `FACEBOOK_APP_ID`
   - Update `FirebaseConfig.ios.kt` with Facebook App ID

---

## 📁 File Checklist

After completing all steps, verify these files exist:

```
Dishrating_app2/
├── composeApp/
│   └── google-services.json          ✅ Downloaded from Firebase
├── iosApp/iosApp/
│   └── GoogleService-Info.plist      ✅ Downloaded from Firebase
├── local.properties                   ✅ Contains all keys
└── composeApp/src/iosMain/.../
    └── FirebaseConfig.ios.kt         ✅ Updated with values
```

---

## 🧪 Testing Authentication

After setup, test each method:

### Test Email/Password:
1. Run the app
2. Go to Sign Up
3. Create account with email/password
4. Check Firebase Console → **Authentication** → **Users** (should see new user)
5. Check Supabase → **Table Editor** → **profiles** (should see profile)

### Test Google Sign-In:
1. Click "Sign in with Google"
2. Select account
3. Verify successful login
4. Check both Firebase and Supabase for user data

### Test Facebook Sign-In:
1. Click "Sign in with Facebook"
2. Login with Facebook
3. Verify successful login

---

## 🚨 Common Issues

### "Error 12500" or "Sign-in failed"
- **Cause**: SHA-1 fingerprint not added or wrong
- **Fix**: Re-generate SHA-1, add to Firebase, download new google-services.json

### "FIREBASE_WEB_CLIENT_ID not found"
- **Cause**: Missing from local.properties
- **Fix**: Extract from google-services.json and add to local.properties

### "Package name mismatch"
- **Cause**: google-services.json is for wrong package
- **Fix**: Register new app with `com.example.smackcheck2`, download correct file

### "Facebook Sign-In not working"
- **Cause**: OAuth redirect URI not added to Facebook app
- **Fix**: Copy URI from Firebase, add to Facebook Login settings

---

## 🎯 Current Status

✅ Gradle configuration complete
✅ Code migration complete
⏳ Firebase project setup needed (follow steps above)
⏳ Configuration files needed (google-services.json, GoogleService-Info.plist)
⏳ Environment variables needed (local.properties, FirebaseConfig.ios.kt)
⏳ OAuth providers need configuration

---

## 📚 Next Steps

1. **Complete Step 1-9 above** to configure Firebase
2. **Run the app** and test authentication
3. **Verify** users appear in both Firebase and Supabase
4. **Deploy** to production with production Firebase project

For detailed instructions, see `FIREBASE_MIGRATION_GUIDE.md`
