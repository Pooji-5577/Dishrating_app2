---
slug: dish-click-dish-not-found
status: resolved
trigger: Clicking on a dish in "Discover Foodies" screen (map view with bottom sheet showing dish like "Fish Curry") redirects to RestaurantDetailScreen showing "Dish not found" error instead of the dish detail.
created: 2026-05-06
updated: 2026-05-06
---

## Symptoms

- **Expected behavior**: Clicking a dish card in the bottom sheet (on "Discover Foodies" map screen) should navigate to a dish detail screen showing the dish info.
- **Actual behavior**: Navigates to `RestaurantDetailScreen` which shows "Dish not found" error with a Retry button.
- **Error messages**: "Dish not found" displayed on screen with a Retry button
- **Navigation target**: `RestaurantDetailScreen` (visible in IDE breadcrumb at bottom of screen)
- **Example dish**: "Fish Curry" at "Domino's Pizza | Anekal Town, Bangalore", rated 5.0
- **Reproduction**: Open Discover Foodies map → tap on a map marker → bottom sheet appears with dish info → tap on dish card

## Current Focus

hypothesis: Wrong ID type passed to DishDetailScreen — rating ID used instead of dish ID.
test: null
expecting: null
next_action: resolved
reasoning_checkpoint: null

## Evidence

- timestamp: 2026-05-06
  finding: SocialMapScreen.kt line 659 passes `latestRatingId` to `onDishDetailClick`, but the nav host stores it as `dishId` and passes it to `DishDetailViewModel.loadDish()`, which queries the `dishes` table by ID. The rating ID does not exist in the dishes table, so the query returns null → "Dish not found".
  file: SocialMapScreen.kt:659
  
- timestamp: 2026-05-06
  finding: MapUserMarker has two distinct fields — `latestRatingId` (UUID in ratings table) and `latestDishId` (UUID in dishes table). Both are populated by SocialMapRepository from the backend DTOs. The fix is to use `latestDishId` in the onViewDish callback.
  file: Models.kt:189-190, SocialMapRepository.kt:51-52

## Eliminated

- Wrong screen navigated to: NavHost correctly routes to Screen.DishDetail (not RestaurantDetail). The breadcrumb in the screenshot was misleading — actual screen shown is DishDetailScreen displaying "Dish not found" error state.
- Missing route wiring: NavHost at line 743-748 correctly wires onDishDetailClick → Screen.DishDetail.

## Resolution

root_cause: In SocialMapScreen.kt onViewDish lambda (line 659), `latestRatingId` was passed to `onDishDetailClick` instead of `latestDishId`. The rating ID (from the `ratings` table) was then used to query the `dishes` table, which returned null, triggering the "Dish not found" error.
fix: Changed `uiState.selectedUser?.latestRatingId` to `uiState.selectedUser?.latestDishId` in the `onViewDish` lambda in SocialMapScreen.kt. The variable was also renamed from `ratingId` to `dishId` for clarity.
verification: The fix passes the correct dish UUID to DishDetailViewModel.loadDish(), which queries dishes table with the right ID and returns the dish data.
files_changed: composeApp/src/commonMain/kotlin/com/example/smackcheck2/ui/screens/SocialMapScreen.kt
