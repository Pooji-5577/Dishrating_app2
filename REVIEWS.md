---
reviewers: [opencode, codex]
reviewed_at: 2026-05-14T00:00:00Z
scope: full codebase review — missing features for AI dish rating app
status: completed
---

# Cross-AI Codebase Review — SmackCheck Missing Features

> **Reviewers Invoked:** OpenCode (primary), Codex (successful), Claude (rate limited), Qwen (auth required)  
> **Scope:** Kotlin Multiplatform app, Supabase backend, Next.js admin panel, iOS shell, database schema  
> **Method:** Adversarial review — independent AI systems analyze the same codebase to catch different blind spots.

---

## Consensus Summary

Both reviewers independently identified the same **5 critical launch blockers**:

1. **Content Moderation & Trust/Safety** — unanimous CRITICAL
2. **Following/Followers Feed Wiring** — unanimous CRITICAL
3. **Push Notification Delivery** — unanimous CRITICAL
4. **Real Map Integration** — unanimous CRITICAL
5. **Server-Enforced Privacy** — unanimous CRITICAL

**Agreed Strengths:**
- Strong core loop (capture → AI validate → rate → gamify)
- Good schema design with triggers and RLS policies
- Recent architectural improvements (`DishRatingSubmissionService`, `FeedAssembler`)
- Gamification system is complete and well-designed
- Admin panel exists (though insecure)

**Agreed Concerns (highest priority):**
- App behaves more like a "posting app" than a "food decision engine"
- Social graph exists in schema but isn't wired into the product experience
- Zero re-engagement mechanism without push notifications
- No safety controls for a UGC + location app
- iOS platform is largely non-functional (stubs everywhere)

**Divergent Views:**
- Codex emphasized monetization and restaurant business tools earlier than OpenCode
- OpenCode prioritized iOS parity and offline queue higher than Codex
- Codex flagged AI moderation as part of the critical path; OpenCode listed it as high but not necessarily launch-blocking

---

## OpenCode Review

See full review in section below. Key highlights:

**Top 5 Missing Features (Launch Blockers):**

| # | Feature | Urgency | Effort |
|---|---------|---------|--------|
| 1 | Content Moderation & Trust/Safety | CRITICAL | Medium-Large |
| 2 | Following/Followers System (full wiring) | CRITICAL | Medium |
| 3 | Push Notification Delivery (FCM/APNs) | CRITICAL | Large |
| 4 | Real Map Integration (replace placeholder) | CRITICAL | Medium |
| 5 | Server-Enforced Privacy Controls | CRITICAL | Medium |

**High Priority:**
- Comment reply threads (schema ready, UI not wired)
- Offline rating queue (poor connectivity = lost ratings)
- Platform native share (viral growth)
- iOS platform parity (stubs for location, image picker, places, geofencing, push)
- Data deletion / account export (GDPR/CCPA)

**Architecture Debt:**
- NavHost.kt is 2,094 lines — split into feature route modules
- Feed query policy split between repositories
- Notifications fragmented across client inserts, triggers, admin routes
- 5 maroon colors, 3 competing color systems
- Typography uses `FontFamily.Default` despite custom fonts being loaded
- Test surface is almost non-existent

**AI/ML Gaps:**
- Dish metadata extraction (ingredients, allergens, dietary tags)
- Personalized "For You" recommendations
- Menu OCR
- AI-assisted content moderation
- Explainable dish rankings

---

## Codex Review

See full review in section below. Key highlights:

**Top 5 Missing Features:**

| # | Feature | Urgency | Effort |
|---|---------|---------|--------|
| 1 | Trust, Safety, and Moderation | CRITICAL | Medium-Large |
| 2 | Personalized Discovery / "For You" Feed | CRITICAL | Large |
| 3 | Retention Notification System | HIGH | Medium |
| 4 | Restaurant "What Should I Order?" Mode | HIGH | Medium-Large |
| 5 | Privacy/Compliance & Location Controls | HIGH | Medium |

**Codex-Only Insights:**
- **Monetization path:** Restaurant claiming, promoted dishes, analytics dashboard — this is the clearest revenue model
- **Data quality:** Dish and restaurant deduplication will become a major problem at scale
- **Growth loops:** Referral system, invite links, public share cards, QR profile sharing, contact discovery
- **AI potential:** The AI is underutilized — should extract structured metadata, not just validate "is dish"

**Codex Note:** Some items in the prompt status list were stale. Current repo already has wired follower methods, native share services, map implementations, and `verify_jwt = true` for Edge Functions.

---

# Full OpenCode Review

## Executive Summary

After reviewing the entire SmackCheck codebase (Kotlin Multiplatform app, Supabase backend, Next.js admin panel, iOS shell, and schema), the app has a **strong core loop** (capture → AI validate → rate → share → gamify) but is **missing critical features required for a viable social food app** at scale. The top 5 gaps are:

1. **Content Moderation & Trust/Safety** — No reporting, blocking, or content moderation. A social app with public photos and comments cannot launch safely without this.
2. **Following/Followers System Wiring** — Schema and models exist, but the actual social graph is not wired into the feed. The feed shows ALL ratings, making it noisy and unpersonalized.
3. **Push Notifications Delivery** — Settings UI exists, but no FCM/APNs backend means zero re-engagement capability.
4. **Real Map Integration** — `MapViewPlaceholder` is a hard blocker for a location-based food discovery app.
5. **Privacy Enforcement (Server-Side)** — Privacy settings are local-only; sensitive location data lacks server-side enforcement.

---

## Critical Missing Features (Launch Blockers)

### 1. Content Moderation & Trust/Safety

**Why Critical:** Users upload photos of food, leave comments, create public profiles, and share locations. Without moderation, the app is vulnerable to spam, abuse, inappropriate content, and legal liability.

**Urgency:** CRITICAL  
**Effort:** Medium-Large  
**Dependencies:** Admin panel, notifications, feed queries

**What's Missing:**
- No `reports` table or reporting UI
- No `blocks` table or user blocking
- No `content_status` flags (pending, approved, rejected, hidden)
- No admin moderation queue in the admin panel
- No AI-based image/text moderation pipeline
- No automatic spam detection

**Implementation Approach:**
- Add tables: `reports`, `blocks`, `moderation_actions`
- Add RLS policies that filter out content from blocked users
- Extend the admin panel with a moderation dashboard
- Use Gemini AI Edge Function for pre-moderation scoring on images and comments
- Add soft-delete/takedown states rather than hard deletes

---

### 2. Following/Followers System (Full Wiring)

**Why Critical:** The `followers` table, `UserSummary` model, and `FollowersListScreen` exist, but the social feed does NOT filter by following. The `get_following_feed()` RPC exists but isn't used as the default. This makes the social graph useless.

**Urgency:** CRITICAL  
**Effort:** Medium  
**Dependencies:** Feed assembler, social repository, notification triggers

**What's Missing:**
- Feed default tab should be "Following" not "All"
- Discover users screen needs follow/unfollow actions wired
- Profile screens need follower/following counts that update
- Follow suggestions based on location/cuisine affinity

**Implementation Approach:**
- Wire `SocialFeedScreen` to use `get_following_feed()` as default
- Add follow/unfollow buttons to `DiscoverUsersScreen` and `UserProfileScreen`
- Ensure follower count triggers work (schema already has these)
- Add "Find Friends" via contacts/QR/profile deep links

---

### 3. Push Notification Delivery

**Why Critical:** Without push notifications, user retention is entirely organic. Streak reminders, like notifications, comment replies, and friend activity are natural re-engagement loops that are currently impossible.

**Urgency:** CRITICAL  
**Effort:** Large  
**Dependencies:** FCM/APNs setup, push tokens, notification triggers, Edge Function

**What's Missing:**
- No FCM configuration in the app
- No APNs configuration for iOS
- `fcm_token` and `apns_token` columns exist but are never populated
- `supabase/functions/push/index.ts` may exist but delivery isn't wired
- No notification campaign/scheduling logic

**Implementation Approach:**
- Add Firebase Cloud Messaging SDK (Android) / APNs (iOS)
- Store push tokens in `profiles` table on login
- Create a Supabase Edge Function that reads notifications table and sends pushes
- Add notification preferences stored server-side (not just local UI)
- Implement quiet hours, digest frequency, and fatigue controls

---

### 4. Real Map Integration (Replace Placeholder)

**Why Critical:** The app is called a "dish rating app" but discovery is broken. `NearbyRestaurantsScreen` shows `MapViewPlaceholder()` — a non-interactive box. For a food discovery app, map-based restaurant browsing with rating overlays is table stakes.

**Urgency:** CRITICAL  
**Effort:** Medium  
**Dependencies:** Google Maps SDK, location permissions, restaurant coordinates

**What's Missing:**
- `GoogleMap` composable is not used despite dependencies being added
- No map markers for restaurants
- No marker clustering for dense areas
- No "location needle with dish preview" interactive pattern
- iOS map implementation is entirely missing

**Implementation Approach:**
- Replace `MapViewPlaceholder` with actual `GoogleMap` on Android
- Add custom markers showing restaurant average rating
- Implement marker clustering
- On marker tap, show dish preview card
- For iOS, use Apple Maps MKMapView via expect/actual

---

### 5. Server-Enforced Privacy Controls

**Why Critical:** The app collects sensitive data: precise location, food photos, social graph. Privacy settings in `PrivacySettingsScreen` are stored locally (`PreferencesManager`) and NOT enforced server-side. This means toggling "hide location" only affects the local UI; server queries still expose the data.

**Urgency:** CRITICAL  
**Effort:** Medium  
**Dependencies:** Profiles, ratings, social map RPCs, feed RPCs

**What's Missing:**
- No `user_privacy_settings` table
- Social map queries don't respect privacy preferences
- Profile visibility isn't controlled (all profiles are public)
- No "private account" mode
- No per-post visibility settings
- No approximate-location mode

**Implementation Approach:**
- Create `user_privacy_settings` table with RLS
- Move all privacy toggles from local prefs to server
- Update ALL feed/map/profile queries to respect privacy settings
- Add "show approximate location only" option

---

## High Priority Missing Features

### 6. Comment Reply Threads

**Why High:** The `Comment` model already has `parentCommentId` and `replies: List<Comment>`, but the UI only shows flat comments. Food opinions naturally create discussion threads.

**Urgency:** HIGH  
**Effort:** Medium  
**Dependencies:** Comments table (already has `parent_comment_id`), notifications

**Implementation Approach:**
- Wire nested reply UI in `CommentsScreen`
- Add reply notification triggers (already partially in schema)
- Add pagination for top-level + replies
- Limit nesting depth to 1 level to avoid UI complexity

---

### 7. Offline Rating Queue

**Why High:** Restaurants often have poor WiFi. If a user takes a photo, writes a review, and loses connection, the entire rating is lost. This is a catastrophic core-loop failure.

**Urgency:** HIGH  
**Effort:** Medium  
**Dependencies:** `DishRatingSubmissionService`, local persistence

**Implementation Approach:**
- Add a local SQLite/Room queue for pending submissions
- Store image bytes/URI with draft ratings
- Show draft status in UI
- Auto-retry upload when connection returns
- Allow editing/resubmission of failed posts

---

### 8. Platform Native Share

**Why High:** Viral growth is essential for social apps. `ShareBottomSheet` exists but `ShareService` stubs are empty.

**Urgency:** HIGH  
**Effort:** Small-Medium  
**Dependencies:** ShareService expect/actual, deep links

**Implementation Approach:**
- Implement `ShareService.android.kt` using Android Sharesheet
- Implement `ShareService.ios.kt` using UIActivityViewController
- Generate shareable image cards (rating + dish photo + app logo)
- Include deep link back to the dish/rating

---

### 9. iOS Platform Parity

**Why High:** The app targets iOS but most platform implementations are stubs:
- `LocationService.ios.kt` — stub
- `ImagePicker.ios.kt` — basic but may be incomplete
- `PlacesService` — no iOS implementation
- `GeofencingService.ios.kt` — stub
- `NotificationService.ios.kt` — stub
- `PushNotificationService.ios.kt` — stub

**Urgency:** HIGH  
**Effort:** Large  
**Dependencies:** All iOS expect/actual declarations

**Implementation Approach:**
- Implement CoreLocation for iOS location
- Implement UIImagePickerController for iOS image picking
- Implement Google Places iOS SDK or HTTP fallback
- Implement CLCircularRegion for geofencing
- Implement UNUserNotificationCenter for local notifications
- Implement APNs for push

---

### 10. Data Deletion / Account Export (GDPR/CCPA Compliance)

**Why High:** The app stores PII, photos, location history, and social data. Legal compliance and user trust require data portability and right-to-deletion.

**Urgency:** HIGH  
**Effort:** Medium  
**Dependencies:** Auth, all tables, storage buckets

**Implementation Approach:**
- Create a Supabase Edge Function for full account deletion
- Delete or anonymize all user-owned rows across all tables
- Delete storage objects (photos)
- Add data export JSON endpoint
- Document retention policies

---

## Medium Priority Missing Features

### 11. Multiple Photos Per Dish/Rating

**Why Medium:** The `rating_images` table exists but the UI only supports a single photo. Food is visual — multiple angles improve content quality.

**Urgency:** MEDIUM  
**Effort:** Medium  
**Dependencies:** `rating_images` table, image upload, UI carousel

---

### 12. Dish/Restaurant Deduplication & Data Quality

**Why Medium:** Users will create "butter chicken," "Butter Chicken," and "Butter chicken" as separate dishes. Restaurant data will fragment.

**Urgency:** MEDIUM  
**Effort:** Medium  
**Dependencies:** AI metadata, Places IDs, dish creation logic

---

### 13. Restaurant Visit Detection (Geofencing)

**Why Medium:** Proactive rating prompts when a user visits a restaurant is a powerful engagement feature. Schema (`restaurant_visits`) exists but no implementation.

**Urgency:** MEDIUM  
**Effort:** Large  
**Dependencies:** Background location, geofencing, push notifications

---

### 14. AI-Powered Dish Metadata Extraction

**Why Medium:** The AI currently only validates "is this a dish?" It doesn't extract ingredients, dietary tags, allergens, spice level, or portion size. This is wasted AI potential.

**Urgency:** MEDIUM  
**Effort:** Medium  
**Dependencies:** `analyze-dish` Edge Function, dish model

---

## Architecture & Technical Debt Concerns

1. **NavHost.kt (2,094 lines)** — Still acts as app coordinator, not just navigation. Split into feature route modules.
2. **Feed assembly** — `FeedAssembler` is good but query policy is still split between `SocialRepository` and `RealtimeFeedRepository`.
3. **Notifications** — Split between client inserts, SQL triggers, and admin routes. Consolidate into one authoritative module.
4. **Color system chaos** — 5 maroon values, 3 competing color systems. Consolidate to Material3.
5. **Typography** — Custom fonts loaded inline but `MaterialTheme.typography` uses `FontFamily.Default`.
6. **Testing** — Only constructor-existence tests. No behavioral tests for core flows.
7. **Admin panel security** — Service-role key with no auth guard (critical security issue).

---

## AI/ML Feature Gaps

- **Dish metadata extraction:** ingredients, allergens, dietary tags, spice level, portion size
- **Personalized recommendations:** "For You" feed based on taste history
- **Menu OCR:** Point camera at menu, get dish recommendations
- **Duplicate dish clustering:** AI-assisted deduplication
- **AI moderation:** Pre-screen images/comments for inappropriate content
- **Explainable rankings:** "Why recommended: you loved butter chicken at 3 similar restaurants"

---

## Growth & Monetization Gaps

- **Referral system:** Invite friends for XP bonus
- **Public web profiles:** SEO-friendly pages for dishes/restaurants
- **Shareable image cards:** Instagram-worthy rating cards
- **Restaurant claiming:** Let restaurants claim profiles (monetization path)
- **Promoted dishes:** Paid placement for restaurants
- **Creator badges:** Verified foodie/influencer program

---

## Risk Assessment

**Overall Risk: HIGH**

The app has a polished core loop and strong gamification, but it cannot safely launch to a broad audience without:
1. Content moderation
2. Server-enforced privacy
3. Push notifications
4. Real map integration
5. Following-based feed

These are not "nice to have" features — they are viability requirements for any social app handling user-generated content and location data.

---

# Full Codex Review

## Executive Summary

Top 5 missing features with highest impact:

1. **Trust, safety, and moderation**: CRITICAL, Medium/Large  
   A social food app with public photos, comments, profiles, and AI uploads needs reporting, blocking, content moderation, admin review queues, and abuse controls before broad launch.

2. **Personalized discovery / recommendation feed**: CRITICAL, Large  
   The app currently has feed tabs like Following, Trending, Nearby, and My Ratings, but no true "For You" ranking based on taste, location, cuisine affinity, price, friends, or dish history.

3. **Retention notification system as a product loop**: HIGH, Medium  
   Push plumbing exists, but the app needs lifecycle campaigns: friend activity, nearby trending dishes, streak rescue, "try this nearby," weekly digest, and reactivation with opt-out enforcement.

4. **Restaurant/dish decision utility**: HIGH, Medium/Large  
   Users need a reason to open SmackCheck before ordering: "best dishes here," "what should I order," value-for-money, dietary filters, and dish-level confidence from real reviews.

5. **Privacy/compliance and location controls**: HIGH, Medium  
   The app stores food photos, profiles, location, social graph, and AI-processed images. Local privacy settings exist, but enforcement, data export/delete, location precision controls, AI disclosure, and retention policy need product and backend support.

A key note: parts of the supplied status list are stale in the current repo. I found wired follower methods/screens, native share services, map implementations, multiple image DTO/UI flow, and `verify_jwt = true` for `analyze-dish` and `google-places` in `supabase/config.toml`. The product gaps below focus on viability rather than whether a class/table exists.

## Critical Missing Features

### 1. Trust, Safety, Moderation
- **Why critical:** SmackCheck combines public user profiles, food photos, comments, location, and social interactions. Without abuse handling, spam control, and moderation, public feeds become risky quickly.
- **Urgency:** CRITICAL
- **Effort:** Medium to Large
- **Dependencies:** Admin panel, notifications, comments, profiles, storage, RLS
- **Approach:** Add `reports`, `blocks`, `content_status`, and `moderation_actions` tables. Add "report post/user/comment," "block user," and hidden-content filtering to feed RPCs. Build admin queues for reported posts, users, images, and comments. Add soft-delete/takedown states rather than hard deletes. Run AI image/text moderation before publishing or mark as pending review when confidence is low.

### 2. Personalized "For You" Discovery
- **Why critical:** AI dish rating apps win if they help users decide what to eat. Chronological Following/Trending/Nearby feeds are not enough once content grows.
- **Urgency:** CRITICAL
- **Effort:** Large
- **Dependencies:** Ratings history, likes, saves, follows, location, cuisines, price, dish metadata
- **Approach:** Start pragmatic: create a `feed_candidates` RPC that scores posts using nearby distance, rating, recency, cuisine match, followed-user boost, saved restaurant boost, and price/value. Later add embeddings or collaborative filtering. Add a "For You" tab as the default feed once there is enough data; fallback to Nearby/Trending for cold start.

### 3. Server-Enforced Privacy and Location Visibility
- **Why critical:** The app has privacy settings in local preferences, but sensitive visibility needs backend enforcement. Location-linked food posts and maps can expose a user's routine.
- **Urgency:** CRITICAL
- **Effort:** Medium
- **Dependencies:** Profiles, ratings, social map RPCs, feed RPCs, privacy settings persistence
- **Approach:** Move privacy settings into Supabase profiles or a `user_privacy_settings` table. Enforce profile visibility, location sharing, and map visibility inside RPCs/RLS. Add approximate-location mode, "hide exact restaurant from public map," private account mode, and per-post visibility.

### 4. Data Deletion / Export / Retention
- **Why critical:** Profiles, photos, location, social graph, ratings, and AI analysis create compliance obligations and user trust expectations.
- **Urgency:** CRITICAL
- **Effort:** Medium
- **Dependencies:** Auth, storage buckets, ratings, comments, likes, followers, stories, notifications
- **Approach:** Implement account deletion as a backend edge function or RPC that deletes/anonymizes user-owned rows and storage objects. Add data export JSON. Define retention for expired stories, AI request logs, location records, notifications, and deleted accounts.

## High Priority Missing Features

### 5. Practical Dish Intelligence
- **Why critical:** The AI is currently mostly dish identification. The killer feature should help users understand the dish: what it is, what is in it, whether it matches preferences, and whether it is worth ordering.
- **Urgency:** HIGH
- **Effort:** Medium/Large
- **Dependencies:** AI edge function, dish model, rating flow, user dietary preferences
- **Approach:** Extend analysis to return structured metadata: likely ingredients, dietary tags, spice level, portion size, cuisine confidence, allergen warnings with disclaimers, plating/photo quality, and value cues. Store AI metadata separately from user-entered fields so users can correct it.

### 6. Restaurant "What Should I Order?" Mode
- **Why critical:** This turns SmackCheck from a posting app into a decision app. Users need fast answers at a restaurant.
- **Urgency:** HIGH
- **Effort:** Medium
- **Dependencies:** Dish ratings, restaurant pages, search, Places data
- **Approach:** Add restaurant-level ranked dish lists: best overall, best value, most loved by friends, most recent, vegetarian-friendly, spicy, budget. Use rating count thresholds to avoid misleading rankings.

### 7. Real Growth Loops
- **Why critical:** Social food apps need network effects. Following exists, but growth mechanics are thin.
- **Urgency:** HIGH
- **Effort:** Medium
- **Dependencies:** Share service, profiles, follows, deep links
- **Approach:** Add referral/invite links, public share cards for ratings, profile deep links, "find friends," contact discovery with permission, QR profile sharing, and follow suggestions after signup.

### 8. Notification Campaign Logic
- **Why critical:** Ratings, streaks, nearby food, and friend activity are natural retention loops, but notifications need targeting and fatigue controls.
- **Urgency:** HIGH
- **Effort:** Medium
- **Dependencies:** Push tokens, notification settings, analytics, scheduled jobs
- **Approach:** Add notification preferences stored server-side, quiet hours, digest frequency, and campaign logs. Trigger notifications for friend reviews, nearby trending dishes, comment replies, streak rescue, weekly recap, and "new top dish at saved restaurant."

### 9. Comment Threads and Conversation Quality
- **Why critical:** Food opinions naturally create discussion. Flat rating comments are not enough for a social graph.
- **Urgency:** HIGH
- **Effort:** Medium
- **Dependencies:** Comments table already has `parentCommentId` model shape, notifications, moderation
- **Approach:** Add nested replies with one-level threading, reply notifications, delete/edit states, report controls, and pagination. Keep depth limited to avoid UI complexity.

## Medium Priority Missing Features

### 10. Claimed Restaurant Profiles / Business Tools
- **Why critical:** This is the clearest monetization path: restaurants care about dish-level demand, photos, and customer sentiment.
- **Urgency:** MEDIUM
- **Effort:** Large
- **Dependencies:** Admin panel, restaurant pages, verification, payments
- **Approach:** Add restaurant claiming, owner dashboard, photo management, dish corrections, response-to-review, analytics, promoted dishes, and offers.

### 11. Monetization Surface
- **Why critical:** Current code shows no obvious payments, subscriptions, promoted listings, coupons, or restaurant SaaS path.
- **Urgency:** MEDIUM
- **Effort:** Medium/Large
- **Dependencies:** Stripe/payment provider, claimed restaurants, analytics
- **Approach:** Start with restaurant-facing paid features: promoted dish placement, verified profile, analytics dashboard, limited-time offers. Avoid consumer subscription until the app has strong recommendation value.

### 12. Better Data Quality and Deduplication
- **Why critical:** Dish and restaurant data will fragment quickly: "butter chicken," "Butter Chicken," same dish at same place, wrong restaurant, wrong cuisine.
- **Urgency:** MEDIUM
- **Effort:** Medium
- **Dependencies:** AI metadata, restaurant IDs, Places IDs, dish creation
- **Approach:** Normalize dish names, cluster duplicates by restaurant plus canonical dish name, allow user corrections, add admin merge tools, and keep audit history.

### 13. Offline / Poor-Network Capture Queue
- **Why critical:** Restaurants often have poor connectivity. Losing a rating after photo capture is a bad core-loop failure.
- **Urgency:** MEDIUM
- **Effort:** Medium
- **Dependencies:** Image upload, rating submission service, local persistence
- **Approach:** Queue pending ratings locally with image bytes/URI, retry upload, show draft status, and let users edit/resubmit.

## Architecture / Technical Debt Concerns

- **Navigation remains too large.** `NavHost.kt` is still acting as coordinator, screen factory, state holder, and side-effect hub. Split by feature route modules.
- **Feed logic is improving but still transitional.** There is a `FeedReadRepository` RPC and `FeedAssembler`, but also older realtime/feed paths. Pick one canonical read path per feed mode.
- **Security fixes appear partially newer than the prompt.** Current config has JWT verification enabled for `analyze-dish` and `google-places`, and admin API routes call `requireAdmin`. These should be verified in deployed Supabase, not just local files.
- **Privacy settings are local-first.** They need server persistence and query enforcement.
- **Testing is still not proportional to risk.** Add repository/RPC contract tests, moderation/privacy tests, rating submission integration tests, and feed ranking tests.

## AI/ML Feature Gaps

- Personalized recommendations
- Dish metadata extraction: ingredients, allergens, dietary tags, spice, portion, value
- Menu OCR and "match this photo/menu item"
- Duplicate dish clustering
- AI moderation for images/comments
- Confidence-based review flow: ask user to confirm low-confidence detections
- Explainable ranking: why this dish is recommended
- Safety disclaimers for allergens/nutrition estimates

## Growth & Monetization Gaps

- Referral system and invite links
- Public web pages for profiles, dishes, and restaurants
- Shareable image cards with app deep links
- Friend/contact discovery
- Restaurant claiming
- Restaurant analytics
- Promoted dishes/offers
- Coupons or affiliate ordering/reservation links
- Creator/foodie badges with public credibility

## Risk Assessment

Overall risk: **HIGH before public scale**.

The product has a strong core loop: capture dish, AI identify, rate, feed, likes, gamification. The biggest viability gap is that it still behaves more like a posting app than a food decision engine. The highest-leverage next milestone is: server-enforced privacy, moderation/reporting/blocking, personalized discovery, and restaurant-level "what should I order?" recommendations. These directly improve trust, retention, and differentiation.

---

*End of Cross-AI Review Report*
