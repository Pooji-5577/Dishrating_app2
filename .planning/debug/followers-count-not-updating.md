---
slug: followers-count-not-updating
status: resolved
trigger: "Following an account does not update the followers count on the profile screen. The button shows Following (meaning the follow action succeeded), but Followers count remains 0."
created: 2026-05-06
updated: 2026-05-06
---

## Symptoms

- expected: Following an account should increment the followed account's Followers count
- actual: Followers count stays at 0 even after the follow action completes
- errors: None — follow action appears to succeed (button changes to "Following")
- timeline: Observed in current build
- reproduction: Navigate to any user profile → tap Follow → button shows "Following" but Followers count remains 0

## Current Focus

- hypothesis: "onFollowClick handler does not re-fetch the user profile after follow/unfollow, so the stale followersCount from initial load persists in the UI"
- test: "Navigate to a user profile, tap Follow, observe Followers count"
- expecting: "Followers count increments by 1 after tapping Follow"
- next_action: "fix applied"
- reasoning_checkpoint: "DB trigger (trigger_update_follow_counts) correctly increments profiles.followers_count on INSERT to followers table. The client never reads the updated row back after following."
- tdd_checkpoint: ""

## Evidence

- timestamp: 2026-05-06T00:00:00Z
  file: composeApp/src/commonMain/kotlin/com/example/smackcheck2/navigation/NavHost.kt
  lines: 1551-1568
  note: "onFollowClick only toggles isFollowing boolean locally — never calls getUserProfile() after follow succeeds"

- timestamp: 2026-05-06T00:00:00Z
  file: supabase/migrations/001_followers_comments_notifications.sql
  lines: 141-158
  note: "trigger_update_follow_counts trigger exists and is correct — it increments profiles.followers_count on INSERT to followers table"

- timestamp: 2026-05-06T00:00:00Z
  file: composeApp/src/commonMain/kotlin/com/example/smackcheck2/data/repository/SocialRepository.kt
  lines: 632-656
  note: "getUserProfile() reads fresh data from profiles table including followers_count — just was never called after follow action"

## Eliminated

- Supabase DB trigger not existing: trigger is present and correct in migration 001
- followUser() failing silently: button changes to "Following" proving the insert succeeded
- ProfileDto/User model missing followersCount: field exists in both DTO and User model

## Resolution

- root_cause: "In NavHost.kt Screen.UserProfile, the onFollowClick handler toggles isFollowing locally after a successful follow/unfollow but never re-fetches the user profile from Supabase. The DB trigger correctly updates profiles.followers_count, but the client never reads the updated value back."
- fix: "After follow/unfollow succeeds, call socialRepository.getUserProfile(targetUserId) and update userProfileState.user with the refreshed profile, which carries the DB-updated followersCount."
- verification: "Navigate to any user profile, tap Follow — Followers count should increment immediately. Tap Unfollow — count should decrement."
- files_changed: "composeApp/src/commonMain/kotlin/com/example/smackcheck2/navigation/NavHost.kt"
