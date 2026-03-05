# Plan: Migrate Google Places API to Supabase Edge Function & Fix All Secret Leaks

## Overview
Move all direct Google Places API calls behind a Supabase Edge Function (same pattern as Gemini/analyze-dish), fix iOS hardcoded secrets, clean up git leaks, and remove legacy code.

## Step 1: Create `google-places` Supabase Edge Function

### New file: `supabase/functions/google-places/index.ts`
- Reads `GOOGLE_PLACES_API_KEY` from `Deno.env.get()`
- Accepts two actions: `nearby-search` and `place-details`
- Proxies requests to `maps.googleapis.com` server-side
- Maps Google response to our `NearbyRestaurant` model
- CORS headers, error handling (same pattern as analyze-dish)

### New file: `supabase/functions/google-places/deno.json`
- Import map (same as analyze-dish)

### Edit: `supabase/config.toml`
- Add `[functions.google-places]` config block

### CLI: Set secret
```bash
supabase secrets set GOOGLE_PLACES_API_KEY=AIzaSyBDFsGAGgmK1O1d7hjYmHxmxsWTpiOXf0A
```

## Step 2: Refactor PlacesService to use Edge Function

### Rewrite: `composeApp/src/commonMain/.../platform/PlacesService.kt`
- Convert from `expect class` to a regular class
- Uses `supabase.functions.invoke("google-places", ...)` 
- No more platform-specific HTTP clients
- `NearbyRestaurant` data class stays the same

### Delete: `composeApp/src/androidMain/.../platform/PlacesService.android.kt`
### Delete: `composeApp/src/iosMain/.../platform/PlacesService.ios.kt`

### Edit: `composeApp/src/commonMain/.../App.kt`
- Update PlacesService instantiation (no longer platform-specific)

### Edit: `composeApp/build.gradle.kts` line 116
- Remove `buildConfigField("String", "GOOGLE_MAPS_API_KEY", ...)` 
- KEEP line 117 `manifestPlaceholders["GOOGLE_MAPS_API_KEY"]` (needed for Maps SDK in AndroidManifest.xml)

## Step 3: Fix iOS Hardcoded Supabase Keys

### Edit: `composeApp/src/iosMain/.../SupabaseConfig.ios.kt`
- Read SUPABASE_URL and SUPABASE_ANON_KEY from Info.plist via NSBundle.mainBundle
- No more hardcoded values in source

### Edit: `iosApp/iosApp/Info.plist`
- Add SUPABASE_URL and SUPABASE_ANON_KEY entries
- Change GOOGLE_MAPS_API_KEY to use xcconfig variable: `$(GOOGLE_MAPS_API_KEY)`

## Step 4: Fix Secret Leaks in Git

### Edit: `.gitignore`
- Add `GoogleService-Info*.plist` pattern (catches filenames with spaces/parentheses)

### Git rm: `Dishrating_app2/iosApp/iosApp/GoogleService-Info (1).plist`
- Remove from git tracking

### Rotate keys (manual):
- Generate new Google Maps API key in Google Cloud Console
- Restrict old key or delete it
- Apply API restrictions (bundle ID for Maps SDK, no restrictions needed for server-side Places key)

## Step 5: Delete Legacy Subdirectory

### Delete: `Dishrating_app2/Dishrating_app2/` (entire directory)
- Contains stale code with client-side Gemini API key
- Contains hardcoded full JWT Supabase anon key
- No longer used by the active codebase

## File Change Summary

| Action | File |
|--------|------|
| CREATE | `supabase/functions/google-places/index.ts` |
| CREATE | `supabase/functions/google-places/deno.json` |
| EDIT   | `supabase/config.toml` |
| REWRITE | `composeApp/src/commonMain/.../PlacesService.kt` |
| DELETE | `composeApp/src/androidMain/.../PlacesService.android.kt` |
| DELETE | `composeApp/src/iosMain/.../PlacesService.ios.kt` |
| EDIT   | `composeApp/src/commonMain/.../App.kt` |
| EDIT   | `composeApp/build.gradle.kts` |
| EDIT   | `composeApp/src/iosMain/.../SupabaseConfig.ios.kt` |
| EDIT   | `iosApp/iosApp/Info.plist` |
| EDIT   | `.gitignore` |
| GIT RM | `Dishrating_app2/iosApp/iosApp/GoogleService-Info (1).plist` |
| DELETE | `Dishrating_app2/Dishrating_app2/` (entire subdirectory) |
