# SmackCheck Architecture Issues

This note captures the main architectural friction points found in the codebase.
The goal is not to re-litigate feature work, but to identify where the current
Modules are shallow, duplicated, or too coupled to be easy to test and evolve.

## 1. Rating submission is too broad for one Module

### Files
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/viewmodel/DishRatingViewModel.kt`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/data/repository/DatabaseRepository.kt`

### Problem
Submitting a rating requires the caller to understand auth verification,
profile repair, image upload, restaurant creation, dish deduplication, rating
insert, XP rewards, streak updates, achievements, analytics, and notifications.
The Interface is almost as complex as the implementation, so the Module is
shallow.

### Why it hurts
- The orchestration logic is spread across UI, ViewModel, repository, and
  notification code.
- Partial failure behavior is hard to reason about because the steps are not
  owned by one seam.
- Tests have to reconstruct the entire flow instead of testing one deep Module.

### What a deeper shape would look like
Create one Module that owns the post-a-dish workflow end to end. The caller
would pass the submission intent once, and the Module would handle the internal
steps and side effects.

### Benefit
- Better locality: rating bugs live in one place.
- Better leverage: callers learn one Interface instead of many tables and side
  effects.
- Better tests: the submission flow can be tested as one behavior, including
  success, validation, and partial-failure cases.

## 2. Feed assembly is duplicated across two Modules

### Files
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/data/repository/SocialRepository.kt`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/data/repository/RealtimeFeedRepository.kt`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/viewmodel/SocialFeedViewModel.kt`

### Problem
Feed building, rating-to-feed mapping, fallback logic, image aggregation, and
filter behavior are implemented in more than one place. The realtime path and
the normal path do not share one authoritative feed read model.

### Why it hurts
- Changes to feed shape have to be repeated.
- One path batches data, another path fetches per item.
- It is easy for the feed to drift in behavior between realtime and reloads.

### What a deeper shape would look like
Create one feed read Module that maps ratings into feed items. Realtime updates
should invalidate or update feed IDs, then reuse the same assembler.

### Benefit
- Better locality for feed bugs and ranking changes.
- Better leverage because all feed callers reuse one behavior.
- Better tests because feed policy can be validated once.

## 3. `NavHost` is doing application coordination work

### Files
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/navigation/NavHost.kt`

### Problem
Navigation, auth reactions, notification routing, location propagation, analytics
hooks, and screen-local state are all handled in one place. The Module has become
an app coordinator rather than a navigation seam.

### Why it hurts
- The file is hard to reason about because unrelated concerns are interleaved.
- Simple screen changes risk touching the top-level app wiring.
- UI behavior is difficult to test without rendering the whole app shell.

### What a deeper shape would look like
Split route-specific orchestration into smaller Modules around authentication,
notification deep links, profile/follow flows, and feed startup behavior.

### Benefit
- Better locality: route behavior is owned near the route.
- Better leverage: `NavHost` only coordinates navigation, not business logic.
- Better tests: each route-level Module can be tested in isolation.

## 4. Schema drift is leaking into runtime logic

### Files
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/data/repository/DatabaseRepository.kt`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/data/dto/SupabaseDto.kt`
- `supabase/migrations/*`

### Problem
The app carries schema compatibility logic in normal runtime code, including
fallback inserts, nullable DTOs for old columns, and repeated retry logic. That
means callers need to know database history, not just domain behavior.

### Why it hurts
- Table-shape changes spread across the app.
- The same compatibility checks are repeated in multiple Modules.
- Schema migration mistakes are harder to isolate.

### What a deeper shape would look like
Add a Supabase schema Adapter layer that owns compatibility and returns stable
domain models to the rest of the app.

### Benefit
- Better locality for migration work.
- Better leverage because callers see one stable Interface.
- Better tests around schema evolution and fallback behavior.

## 5. Notifications are split across overlapping Modules

### Files
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/notifications/NotificationRepository.kt`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/data/repository/NotificationRepository.kt`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/viewmodel/SocialFeedViewModel.kt`
- `composeApp/src/commonMain/kotlin/com/example/smackcheck2/viewmodel/DishRatingViewModel.kt`

### Problem
There are two notification Modules with different responsibilities, while feed
and rating flows also create notifications directly. The seam is real, but the
behavior is fragmented.

### Why it hurts
- Notification behavior is easy to duplicate or miss.
- Delivery, deduplication, and read-state handling are spread out.
- Callers need to know whether to insert a row, call a helper, or both.

### What a deeper shape would look like
Consolidate notification command/query behavior into one Module. Domain events
from rating, like, and comment flows should go through that Module instead of
direct table access.

### Benefit
- Better locality for notification bugs.
- Better leverage because one Module owns delivery and read state.
- Better tests for event-to-notification mapping.

## 6. The test surface is far too thin

### Files
- `composeApp/src/commonTest/kotlin/com/example/smackcheck2/ComposeAppCommonTest.kt`

### Problem
The common test suite does not cover the Modules with the most behavior. Most
of the meaningful logic is still wired to singletons and self-instantiated
dependencies, which makes seams hard to replace in tests.

### Why it hurts
- Architectural regressions will slip through easily.
- The app relies on manual verification for core flows.
- There is little protection around the most coupled Modules.

### What a deeper shape would look like
Prioritize testable seams around rating submission, feed assembly, notification
events, and schema adaptation.

### Benefit
- Better leverage from each test because the Interface is smaller.
- Better locality because regressions point to one Module.
- Better confidence when changing database or flow orchestration.

## Recommended first exploration

The highest-value candidate is the rating submission flow. It has the largest
blast radius, the most side effects, and the clearest opportunity to become a
deep Module.

