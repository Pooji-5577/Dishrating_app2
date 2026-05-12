# SmackCheck Codebase Architecture and Security Report

Review date: 2026-05-12  
Scope: repository-wide review of `Dishrating_app2`, including Kotlin Multiplatform app code, Supabase Edge Functions and migrations, Next.js admin panel, static website, iOS shell, and release artifacts.

## Executive summary

The highest-risk problems are in the admin panel. Several Next.js route Modules create a Supabase service-role Adapter and either perform no server-side authentication or trust caller-supplied user IDs. That bypasses RLS and exposes user PII and notification controls directly from HTTP routes.

Architecturally, the codebase has started moving in the right direction with `DishRatingSubmissionService`, `FeedAssembler`, and `NotificationService`, but many seams are still shallow because ViewModels and screens instantiate repositories directly, `NavHost` owns too much application coordination, and the important behavior is not testable through stable interfaces.

## Security findings

### S1. Critical: Admin panel routes bypass RLS without authenticating the caller

**Files**
- `admin-panel/src/lib/supabase.ts:11`
- `admin-panel/src/app/api/stats/route.ts:4`
- `admin-panel/src/app/api/users/route.ts:4`
- `admin-panel/src/app/api/users/[id]/route.ts:4`
- `admin-panel/src/app/api/notifications/route.ts:4`
- `admin-panel/src/app/api/notifications/send/route.ts:4`

**Problem**
`createServiceClient()` uses `SUPABASE_SERVICE_ROLE_KEY`, which bypasses RLS. The stats, users, user detail, and notification listing routes call it without validating a Supabase session or checking admin status on the server. The send-notification route checks `senderUserId` from the request body instead of deriving the identity from a verified session.

**Impact**
An unauthenticated attacker can call:
- `GET /api/users` to retrieve user IDs, names, usernames, emails, admin flags, XP, and activity timestamps.
- `GET /api/users/{id}` to retrieve a full profile plus recent ratings and comments.
- `GET /api/stats` to retrieve admin dashboard data and recent signup emails.
- `POST /api/notifications/send` with any known admin `senderUserId` to send notifications to any user or all users.

**Taxonomy**
CWE-862 Missing Authorization, CWE-639 Authorization Bypass Through User-Controlled Key.

**Solution**
Create one server-side admin guard Module used by every admin route:
- Read the `Authorization` bearer token or Supabase auth cookies from the request.
- Call `supabase.auth.getUser(token)` with a non-service client.
- Query `profiles.is_admin` for the verified `user.id`.
- Only after that, create the service-role Adapter for the privileged operation.
- Remove all request-body trust in `senderUserId`; use the verified admin user ID.
- Add integration tests that prove anonymous requests and non-admin sessions receive `401` or `403`.

### S2. High: Hardcoded fallback admin password and client-side session gate

**Files**
- `admin-panel/src/app/api/verify-password/route.ts:5`
- `admin-panel/src/components/PasswordGate.tsx:12`
- `admin-panel/src/components/PasswordGate.tsx:33`

**Problem**
The admin panel has a static fallback password, `smackcheck2026`, when `SITE_PASSWORD` is missing. The unlocked state is stored in `sessionStorage`, so it only hides UI and does not protect route Modules.

**Impact**
If the deployment misses `SITE_PASSWORD`, the password is public from source. Even when configured, this gate does not secure the service-role routes above.

**Taxonomy**
CWE-798 Hardcoded Credentials, CWE-602 Client-Side Enforcement of Server-Side Security.

**Solution**
Delete the password gate or keep it only as cosmetic friction. Server routes must enforce Supabase admin auth. If a second factor is wanted, require `SITE_PASSWORD` to be present at startup and rate-limit attempts, but do not treat it as the only control.

### S3. High: Notification RLS permits arbitrary notification insertion

**Files**
- `supabase/migrations/002_fix_text_ids_full_setup.sql:217`
- `supabase/migrations/002_fix_text_ids_full_setup.sql:218`
- `supabase/migrations/001_followers_comments_notifications.sql:80`
- `supabase/migrations/001_followers_comments_notifications.sql:81`

**Problem**
The `notifications_insert` policy uses `WITH CHECK (true)`. Earlier schema also says "System can insert notifications" but allows any authenticated caller. Later admin-specific policies are added, but the permissive policy remains unless explicitly dropped.

**Impact**
Any authenticated user can create notifications for any `user_id`, enabling spam, phishing-style in-app messages, push notification abuse, and polluted notification history.

**Taxonomy**
CWE-863 Incorrect Authorization.

**Solution**
Drop the permissive insert policy. Move user-triggered notification creation into database triggers or server Edge Functions. If direct insert must remain, restrict it to `auth.uid()::text = user_id` for self notifications and use separate trigger/function paths for cross-user events.

### S4. Medium: Public unauthenticated Edge Functions can burn paid API quota

**Files**
- `supabase/config.toml:386`
- `supabase/config.toml:397`
- `supabase/functions/analyze-dish/index.ts:53`
- `supabase/functions/google-places/index.ts:156`

**Problem**
`analyze-dish` and `google-places` have `verify_jwt = false`, `Access-Control-Allow-Origin: *`, and call paid external APIs using server-side secrets. The functions do not enforce app authentication, per-user quota, body-size limits beyond platform defaults, or abuse throttling in code.

**Impact**
Anyone who knows the function URL can consume Gemini and Google Places quota. `analyze-dish` accepts large base64 image input; `google-places` exposes text search, nearby search, details, geocode, search photos, and photo proxy paths.

**Taxonomy**
CWE-306 Missing Authentication for Critical Function, CWE-770 Allocation of Resources Without Limits.

**Solution**
Set `verify_jwt = true` for app-only functions where possible. For public endpoints, add signed app attestation or a server-issued short-lived token, per-user/IP quotas, request body limits, strict action-specific validation, and monitoring alerts on API spend.

### S5. Medium: Build and release settings weaken production hardening

**Files**
- `composeApp/build.gradle.kts:145`
- `composeApp/build.gradle.kts:156`
- `composeApp/build.gradle.kts:158`
- `composeApp/src/androidMain/AndroidManifest.xml:15`
- repository root APK files and `website/downloads/SmackCheck.apk`

**Problem**
Android lint does not fail the build, release minification is disabled, backups are enabled, dynamic dependency versions are used for analytics libraries, and many built APKs are checked into the repo.

**Impact**
Release builds are easier to reverse engineer, may include stale secrets/config, and can drift due to dynamic dependencies. Checked-in binaries increase repository size and make it harder to know what is source of truth.

**Solution**
Enable release minification/resource shrinking, set `android:allowBackup="false"` unless backup is intentionally supported, pin analytics versions, make lint fail in CI for release builds, and remove generated APKs from source control. Keep release artifacts in a release store instead.

## Architecture issues

### A1. Admin authorization needs a deep Module

**Files**
- `admin-panel/src/app/api/*/route.ts`
- `admin-panel/src/lib/supabase.ts`

**Problem**
Every admin route owns its own security story. The service-role Adapter is a shallow seam because callers must remember when it is safe to bypass RLS.

**Solution**
Introduce one admin route guard Module that verifies identity, admin status, and then exposes a small privileged operation interface. Route Modules should receive `adminUser` plus a scoped service-role Adapter only after the guard succeeds.

**Benefits**
Locality improves because admin auth bugs live in one place. Leverage improves because every route gets the same tested guard. Tests can target the guard interface once, then assert each route calls it.

### A2. Repository and ViewModel seams are still too shallow

**Files**
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/data/repository/*.kt`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/viewmodel/*.kt`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/service/DishRatingSubmissionService.kt:87`

**Problem**
Many ViewModels and services instantiate concrete repositories directly. That makes the interface effectively "know the singleton Supabase world", even when constructors exist. The deletion test says removing the repository constructors would move setup complexity into many callers and tests.

**Solution**
Add a small application dependency Module for common repositories and pass dependencies into ViewModels/services through constructors or factories. Keep one Adapter per external system at the seam, not scattered singleton access.

**Benefits**
Locality improves because wiring changes are centralized. Leverage improves because tests can replace repositories without live Supabase clients.

### A3. `NavHost` is a shallow application coordinator

**Files**
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/navigation/NavHost.kt:1`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/navigation/NavHost.kt:600`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/navigation/NavHost.kt:1195`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/navigation/NavHost.kt:1534`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/navigation/NavHost.kt:1724`

**Problem**
`NavHost.kt` is 1,874 lines and owns navigation state, route arguments, auth reactions, repository construction, screen wiring, notification routes, comment flows, profile flows, and placeholders. The interface is nearly as complex as the implementation.

**Solution**
Split route orchestration into feature route Modules: auth routes, dish capture/rating routes, social/feed routes, profile/settings routes, and notifications routes. `NavHost` should only select route Modules and render their output.

**Benefits**
Locality improves because changing a feature route does not require editing the whole app shell. Leverage improves because each route Module can be tested with fake state and fake adapters.

### A4. Feed assembly is deeper, but query policy is still split

**Files**
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/data/repository/FeedAssembler.kt:19`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/data/repository/SocialRepository.kt:146`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/data/repository/SocialRepository.kt:184`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/data/repository/RealtimeFeedRepository.kt:31`

**Problem**
`FeedAssembler` centralizes mapping, which is good. But feed selection policy still lives in `SocialRepository` and realtime cache update logic. The seam is partly deep and partly leaky.

**Solution**
Create a feed read Module that owns both feed selection and assembly for `all`, `following`, `highestRated`, `nearby`, and realtime refresh. Realtime should pass changed IDs into the same read Module instead of maintaining separate cache policy.

**Benefits**
Locality improves for feed ranking, filters, fallback images, and count behavior. Leverage improves because every feed caller shares one interface and one test suite.

### A5. Notification behavior is still split between client inserts and server triggers

**Files**
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/data/repository/NotificationService.kt:43`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/data/repository/SocialRepository.kt:28`
- `supabase/migrations/013_server_side_notification_triggers.sql`
- `supabase/functions/push/index.ts`

**Problem**
There is a unified `NotificationService`, but notification creation also exists in `SocialRepository`, SQL triggers, admin routes, and Edge Functions. The interface does not clearly say which module owns event-to-notification mapping.

**Solution**
Make database triggers or a server-side notification command Module the only cross-user notification writer. The app can request domain actions, but it should not directly construct notification rows for other users.

**Benefits**
Locality improves for notification security, deduplication, and push delivery. Leverage improves because all notification events pass through one audited seam.

### A6. Test surface does not exercise important behavior

**Files**
- `composeApp/src/commonTest/kotlin/com/example/smackcheck2/service/DishRatingSubmissionServiceTest.kt:73`
- `composeApp/src/commonTest/kotlin/com/example/smackcheck2/data/repository/FeedAssemblerTest.kt:6`
- `composeApp/src/commonTest/kotlin/com/example/smackcheck2/data/repository/SupabaseSchemaAdapterTest.kt:6`

**Problem**
Several tests only assert that a class exists because constructors reach live Supabase state. The interface is not the test surface yet; tests cannot cross the seam without external infrastructure.

**Solution**
Define interfaces or constructor-injected adapters only where there are real alternate adapters in tests. Start with admin guard, dish submission, feed read, schema adapter, and notification command Modules.

**Benefits**
Locality improves because regression tests point to one module. Leverage improves because behavior can be tested without booting the whole app or connecting to Supabase.

## Prioritized fix plan

1. Fix the admin panel auth guard first. This closes the service-role RLS bypass and protects PII.
2. Drop permissive notification insert RLS and move cross-user notification writes server-side.
3. Lock down unauthenticated Edge Functions with JWT verification or quota controls.
4. Refactor admin service-role access into one deep guard Module with tests.
5. Split `NavHost` into route Modules and introduce app-level dependency wiring.
6. Harden release settings and remove generated APKs from the repo.

## Review limits

This was a static repository review. I did not run the app, deploy Supabase locally, or execute exploit requests against a live environment. Findings are based on source-level reachability and the repository configuration.
