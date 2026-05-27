# UI/UX QA Review - 2026-05-19

Artifact folder: `/private/tmp/smackcheck-uiux-qa-20260519-175136`

Build under test: `composeApp-debug.apk`, package `com.example.smackcheck2`, emulator `emulator-5554`, Android 16, viewport `1080x2400`.

## Coverage

Covered launch/session routing, Home, Search empty/query/results, top-dish detail tap, Map tab, map settings dropdown, locate/settings controls, Explore tab, Profile tab, Edit Profile, Settings menu, Account settings, Preferences settings, Support, Danger Zone, rate-a-dish sheet, camera handoff, Android photo picker/gallery handoff, bottom navigation, and back navigation for the covered flows.

## Verdict

Shippable with known issues. No crash/ANR/blocker reproduced in this pass, but dish-detail routing from a top dish can land on `Dish not found`, the edit-profile save action is awkwardly below the fold/under the persistent nav at initial position, and frame/memory numbers remain high enough to require follow-up before calling the experience polished.

## P0 Crash/Blocker

None found.

Evidence:
- `/private/tmp/smackcheck-uiux-qa-20260519-175136/final-crash.txt`
- `/private/tmp/smackcheck-uiux-qa-20260519-175136/final-logcat.txt`

## P1 Broken Core Flow

### Top dish opens `Dish not found`

- Screen/flow: Home `Top Dishes Today` first card tap.
- Evidence: `/private/tmp/smackcheck-uiux-qa-20260519-175136/28-dish-detail.png`, `/private/tmp/smackcheck-uiux-qa-20260519-175136/28-dish-detail-summary.txt`
- What happened: Tapping the visible top dish navigated to a detail screen that only rendered `Dish not found` plus `Retry`.
- Why it matters: Top dishes are primary discovery content; a prominent card that dead-ends breaks trust and prevents rating/detail engagement.
- Recommended fix: Verify whether the carousel item carries a rating id or dish id, then normalize navigation so top-dish cards always pass a resolvable dish id, or route to a rating/post detail when only a rating id exists.
- Needs grill-me/user decision: No.

### Edit Profile primary save is not discoverable at initial position

- Screen/flow: Profile -> Edit Profile.
- Evidence: `/private/tmp/smackcheck-uiux-qa-20260519-175136/21-edit-profile.png`, `/private/tmp/smackcheck-uiux-qa-20260519-175136/21-edit-profile-summary.txt`, `/private/tmp/smackcheck-uiux-qa-20260519-175136/42-final-edit-profile-summary.txt`
- What happened: The `Update Profile` button sits at the very bottom of the scroll content and is initially represented only as a clipped clickable region near `[63,2383][1017,2400]`, while the persistent bottom nav overlays the screen.
- Why it matters: Users editing fields may not understand how to save, and the bottom nav competes with an in-progress form.
- Recommended fix: Make edit profile a focused subpage without the main bottom nav, or pin a visible save action in the top bar/bottom action area above navigation insets.
- Needs grill-me/user decision: Yes, if choosing between focused subpage vs persistent nav. Recommended: remove bottom nav from edit/profile settings subpages.

## P2 Confusing/Bad UX

### Display name was percent-encoded

- Screen/flow: Home greeting, Profile, Edit Profile.
- Evidence before fix: `/private/tmp/smackcheck-uiux-qa-20260519-175136/00-launch-summary.txt`, `/private/tmp/smackcheck-uiux-qa-20260519-175136/09-profile-summary.txt`, `/private/tmp/smackcheck-uiux-qa-20260519-175136/21-edit-profile-summary.txt`
- Evidence after fix: `/private/tmp/smackcheck-uiux-qa-20260519-175136/41-final-profile-summary.txt`, `/private/tmp/smackcheck-uiux-qa-20260519-175136/42-final-edit-profile-summary.txt`
- What happened: User name rendered as `Codex%20QA`. After the fix it renders as `Codex QA`.
- Why it matters: Raw encoding makes the app look broken and can leak implementation details into user-facing profile UI.
- Recommended fix: Done. Percent-decoding now happens when `ProfileDto` is converted to `User`.
- Needs grill-me/user decision: No.

### Search empty state is passive

- Screen/flow: Home -> Search with empty query.
- Evidence: `/private/tmp/smackcheck-uiux-qa-20260519-175136/02-search-empty.png`, `/private/tmp/smackcheck-uiux-qa-20260519-175136/02-search-empty-summary.txt`
- What happened: Empty state says `Search for Restaurants` and has filters, but no examples or action beyond the text field.
- Why it matters: Search is a core discovery path; empty states should help users get to results quickly.
- Recommended fix: Add 2-3 tappable suggested searches/cuisines or recently popular restaurants below the filter chips.
- Needs grill-me/user decision: Yes. Recommended: add suggested searches because this is an activation surface.

### Rate-a-dish gallery handoff enters system photo picker without app context

- Screen/flow: Rate a dish -> Choose from Gallery.
- Evidence: `/private/tmp/smackcheck-uiux-qa-20260519-175136/45-final-rate-sheet.png`, `/private/tmp/smackcheck-uiux-qa-20260519-175136/46-final-home-redump-summary.txt`
- What happened: The sheet cleanly opens Android photo selection, but the app provides no in-app pre-step explaining selected-photo-only access or what happens after choosing a photo.
- Why it matters: The OS picker text is correct, but the transition is abrupt for a core creation flow.
- Recommended fix: Add a short in-app title/subtitle on the sheet that frames the choice as starting a dish rating, not just selecting media.
- Needs grill-me/user decision: Yes. Recommended: keep the sheet concise and action-oriented, not a separate onboarding page.

## P3 Polish/Accessibility/Performance

### Home/Explore top-dishes carousel exposes clipped partial card text

- Screen/flow: Home and Explore `Top Dishes Today`.
- Evidence: `/private/tmp/smackcheck-uiux-qa-20260519-175136/00-launch-summary.txt`, `/private/tmp/smackcheck-uiux-qa-20260519-175136/08-explore-summary.txt`
- What happened: The next carousel card peeks at the right edge, but its title/restaurant text is visibly clipped in the UI tree bounds.
- Why it matters: Peeking cards can be useful, but clipped text reads like a layout defect rather than an intentional affordance.
- Recommended fix: Decide whether the carousel should show full cards only, or keep the peek but hide text until the card is mostly visible.
- Needs grill-me/user decision: Yes. Recommended: keep card peeking for discoverability, but avoid showing clipped text.

### High jank in final navigation pass 

- Screen/flow: final validation pass across Home/Profile/Edit/Map/Explore/Rate sheet.
- Evidence: `/private/tmp/smackcheck-uiux-qa-20260519-175136/gfxinfo.txt`, `/private/tmp/smackcheck-uiux-qa-20260519-175136/gfxinfo-framestats.txt`
- What happened: `Total frames rendered: 408`, `Janky frames: 122 (29.90%)`, p50 `22ms`, p90 `105ms`, p95 `150ms`, p99 `700ms`.
- Why it matters: This level of jank is visible during tab transitions and heavy screens, even with emulator noise.
- Recommended fix: Capture a focused Perfetto trace for Home -> Map and Profile -> Edit Profile, then reduce heavyweight image/map composition, repeated loading, and excessive recomposition.
- Needs grill-me/user decision: No.

### Memory remains high after smoke/navigation flow

- Screen/flow: final validation after tab/settings/rate flow.
- Evidence: `/private/tmp/smackcheck-uiux-qa-20260519-175136/meminfo.txt`
- What happened: `TOTAL PSS` was about `437,594 KB`.
- Why it matters: This is slightly better than the previous ~460 MB note, but still high for a consumer app after a short navigation pass.
- Recommended fix: Run heap/native allocation profiling around map and image-heavy feed screens; check retained map, image loader, and feed card objects after leaving screens.
- Needs grill-me/user decision: No.

### Several icon-only controls are ambiguous in the UI tree

- Screen/flow: Home/Profile top bars and some decorative/action icons.
- Evidence: `/private/tmp/smackcheck-uiux-qa-20260519-175136/00-launch-summary.txt`, `/private/tmp/smackcheck-uiux-qa-20260519-175136/41-final-profile-summary.txt`
- What happened: Some icon-only clickable nodes have descriptions (`Notifications`, `Settings`, `Search`), but profile/avatar and some nested action icon semantics are sparse or absent.
- Why it matters: Accessibility services need stable labels for icon-only actions.
- Recommended fix: Audit clickable icon/image wrappers and ensure every action has a clear `contentDescription`; decorative children should be hidden from accessibility.
- Needs grill-me/user decision: No.

## Fixes Made

- Added `decodePercentEncodedText` in `composeApp/src/commonMain/kotlin/com/example/smackcheck2/util/TextDecodeUtils.kt`.
- Applied decoding in `AuthRepository.ProfileDto.toUser()` for `name`, `username`, `lastLocation`, and `bio`.

## Validation

- `./gradlew :composeApp:assembleDebug`: pass.
- Patched APK installed and affected Profile/Edit Profile route rechecked.
- Final crash buffer: no crash entries.
- Final scan found no app `FATAL EXCEPTION`, `ANR`, `CLEARTEXT`, serialization, JSON-token, or timeout match. The remaining matched `AndroidRuntime` lines are `uiautomator` helper process startup/shutdown noise.
