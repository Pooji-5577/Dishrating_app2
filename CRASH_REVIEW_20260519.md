# SmackCheck Android Crash Review - 2026-05-19

## Scope

- Device: Android emulator `emulator-5554`, Android 16 / SDK 36.
- Package: `com.example.smackcheck2`.
- APK reproduced: `SmackCheck_V00.60.apk` (`application-label: SmackCheck00.52`, `versionName: 1.0`).
- Artifact directory: `/private/tmp/smackcheck-crash-review-20260519`.

## Reproduction

Commands used:

```bash
/Users/teja/Library/Android/sdk/platform-tools/adb -s emulator-5554 install -r SmackCheck_V00.60.apk
/Users/teja/Library/Android/sdk/platform-tools/adb -s emulator-5554 logcat -c
/Users/teja/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell am force-stop com.example.smackcheck2
/Users/teja/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell monkey -p com.example.smackcheck2 1
```

Result: app started, then crashed during initial `MainActivity` composition.

## Primary Crash

Artifact: `/private/tmp/smackcheck-crash-review-20260519/smackcheck-v0060-startup-logcat.txt`

Fatal stack:

```text
FATAL EXCEPTION: main
Process: com.example.smackcheck2, PID: 14099
java.lang.NullPointerException: Attempt to invoke virtual method
'java.lang.Object com.example.smackcheck2.data.repository.DatabaseRepository.getSavedRestaurantIds-gIAlu-s(...)'
on a null object reference
  at com.example.smackcheck2.viewmodel.LocationHomeViewModel$loadSavedRestaurants$1.invokeSuspend(LocationGameViewModel.kt:111)
  at com.example.smackcheck2.viewmodel.LocationHomeViewModel.loadSavedRestaurants(LocationGameViewModel.kt:109)
  at com.example.smackcheck2.viewmodel.LocationHomeViewModel.<init>(LocationGameViewModel.kt:104)
  at com.example.smackcheck2.navigation.NavHostKt.SmackCheckNavHost$lambda$8$lambda$7(NavHost.kt:316)
```

Root cause: `LocationHomeViewModel.init` called `loadSavedRestaurants()` before `databaseRepository` was initialized. The property was declared later in the class, so Kotlin initialization order left it null when the coroutine started during release/APK startup.

## Fix Applied

File: `composeApp/src/commonMain/kotlin/com/example/smackcheck2/viewmodel/LocationGameViewModel.kt`

- Moved `private val databaseRepository = DatabaseRepository()` above `init`.
- Added `.onFailure` logging for saved-restaurant loading.

## Verification

Commands:

```bash
./gradlew :composeApp:assembleDebug
/Users/teja/Library/Android/sdk/platform-tools/adb -s emulator-5554 install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/Users/teja/Library/Android/sdk/platform-tools/adb -s emulator-5554 logcat -c
/Users/teja/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell am force-stop com.example.smackcheck2
/Users/teja/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell monkey -p com.example.smackcheck2 1
/Users/teja/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell pidof com.example.smackcheck2
```

Result: process stayed alive as PID `14484`. No `FATAL EXCEPTION` appeared in the startup log after the fix.

Artifact: `/private/tmp/smackcheck-crash-review-20260519/smackcheck-fixed-debug-startup-logcat.txt`

## Remaining Issues Observed

- Backend calls originally failed repeatedly with `CLEARTEXT communication to 10.0.2.2 not permitted by network security policy`. `local.properties` sets `BACKEND_URL=http://10.0.2.2:3000`, but the manifest had no cleartext/network-security exception. This has been fixed for debug builds only with `composeApp/src/debug/AndroidManifest.xml` and `composeApp/src/debug/res/xml/debug_network_security_config.xml`.
- Startup does too much work immediately: logcat shows repeated backend calls, Supabase calls, location detection, maps initialization, and `Skipped 37 frames`. `gfxinfo` after startup: 521 frames, 16 janky frames (3.07%), 99th percentile 73 ms, 10 slow UI thread frames.
- Memory after startup is high for an idle screen: total PSS about 249 MB, RSS about 356 MB. A major contributor is Maps/Web/Firebase/Clarity startup overhead plus multiple concurrent feature ViewModels created at `SmackCheckNavHost` startup.

## Follow-up Fixes Applied

- Added backend JSON parsing support in `ApiClient.getJsonElement`.
- Updated client parsing for backend response shapes:
  - `/api/saves` returns a raw `List<String>`.
  - `/api/dishes/top` and `/api/dishes/top-for-restaurants` return raw `List<DishDto>`.
  - Places endpoints return raw arrays/direct objects with snake_case fields.
  - Gamification challenge progress returns raw rows with nested `challenges`.
  - Leaderboard parsing now accepts raw arrays or common wrapper keys.
- Fixed the profile setup loop by routing successful profile setup directly to `DarkHome` instead of back through `Splash`. The previous path re-read stale in-memory auth state and could send the user back to profile setup even after a successful save.

## Full Smoke Test - Fixed Debug APK

Artifact directory: `/private/tmp/smackcheck-android-smoke-20260519`.

Validated flows:

- Build: `./gradlew :composeApp:assembleDebug` passed.
- Install and launch: debug APK installed on `emulator-5554`; app process stayed alive.
- Auth/onboarding: after profile setup, app landed on Home instead of returning to Profile Setup.
- Home: rendered greeting, search entry point, filters, top dishes, and bottom navigation.
- Search: query `pizza` returned restaurant results including Domino's/Pizza Hut rows.
- Map: rendered Google Map, empty `0 dish posts` state, and list bottom sheet (`My Ratings (0)`, `No dish posts found.`).
- Profile: rendered profile for `@codexqa202605191632`.
- Rate flow: opened `Add a dish photo`, `Take Photo`, and `Choose from Gallery`; tapping `Take Photo` opened `com.android.camera2/com.android.camera.CaptureActivity`.

Final log scan:

- Crash buffer: empty (`final-crash.txt` size 0).
- No matches for the original fatal `NullPointerException`.
- No matches for cleartext network-policy failures.
- No matches for the JSON serializer/shape failures previously seen.
- Remaining non-fatal issue: `SocialMapRepository` timed out against `get_nearby_users_with_dishes` three times while on the map screen. The UI handled this as an empty state and did not crash.

Final quick performance snapshot after the smoke flow:

- `Total frames rendered`: 5152.
- `Janky frames`: 715 / 13.88% (`legacy`: 1662 / 32.26%).
- Frame percentiles: p50 19 ms, p90 25 ms, p95 32 ms, p99 85 ms.
- Slow UI thread frames: 156.
- `TOTAL PSS`: 460205 KB, `TOTAL RSS`: 251956 KB, `TOTAL SWAP PSS`: 291313 KB.
- Active app state: 1 Activity, 1 ViewRootImpl, 37 Views.
