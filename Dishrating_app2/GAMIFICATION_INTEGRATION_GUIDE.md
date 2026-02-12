# Gamification System Integration Guide

This guide explains how to integrate the gamification animations and features into your UI screens.

## Overview

The gamification system now includes:
1. **Variable XP rewards** (10-25 XP based on rating quality)
2. **Streak system** (consecutive daily ratings)
3. **Real-time challenges** (progress based on actual stats)
4. **Auto-unlock achievements**
5. **Animated notifications** (XP gain, level up, achievement unlock)
6. **Challenge completion XP awards**

## Animation Components

All animation components are available in `ui/components/GamificationAnimations.kt`:

### 1. XP Gain Animation
Displays a floating notification when user earns XP.

```kotlin
if (uiState.showXpNotification && uiState.xpEarned != null) {
    XpGainAnimation(
        xpAmount = uiState.xpEarned,
        onAnimationComplete = { viewModel.clearXpNotification() }
    )
}
```

### 2. Level Up Animation
Full-screen celebration when user levels up.

```kotlin
if (uiState.showLevelUpAnimation && uiState.newLevel != null) {
    LevelUpAnimation(
        newLevel = uiState.newLevel,
        onAnimationComplete = { viewModel.clearLevelUpAnimation() }
    )
}
```

### 3. Achievement Unlock Animation
Slides in from top when achievement is unlocked.

```kotlin
if (gameUiState.showAchievementUnlock && gameUiState.newAchievement != null) {
    AchievementUnlockAnimation(
        achievementTitle = gameUiState.newAchievement.title,
        achievementIcon = gameUiState.newAchievement.icon,
        onAnimationComplete = { gameViewModel.clearAchievementUnlock() }
    )
}
```

### 4. Animated XP Progress Bar
Shows XP progress with smooth animations.

```kotlin
AnimatedXpProgressBar(
    currentXp = user.xp % 100,
    maxXp = 100,
    level = user.level
)
```

## Integration Examples

### DishRatingScreen Integration

Add to your `DishRatingScreen` or `DarkDishRatingScreen`:

```kotlin
@Composable
fun DarkDishRatingScreen(
    dishName: String,
    imageUri: String,
    viewModel: DishRatingViewModel,
    profileViewModel: ProfileViewModel,
    gameViewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Your existing content...

        // Add XP gain notification
        if (uiState.showXpNotification && uiState.xpEarned != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                XpGainAnimation(
                    xpAmount = uiState.xpEarned,
                    onAnimationComplete = { viewModel.clearXpNotification() }
                )
            }
        }
    }

    // Handle successful submission
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            // Refresh profile and game data
            profileViewModel.refresh()
            gameViewModel.loadGameData()
            delay(500) // Wait for XP animation
            onNavigateBack()
        }
    }
}
```

### ProfileScreen Integration

Add to your profile screen to show level up animations:

```kotlin
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    authViewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val progressState by progressViewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Your existing content...

        // Show level up animation
        if (progressState.showLevelUpAnimation && progressState.newLevel != null) {
            LevelUpAnimation(
                newLevel = progressState.newLevel,
                onAnimationComplete = { progressViewModel.clearLevelUpAnimation() }
            )
        }

        // Use animated XP progress bar
        AnimatedXpProgressBar(
            currentXp = progressState.currentXp % 100,
            maxXp = 100,
            level = progressState.level,
            modifier = Modifier.padding(16.dp)
        )
    }
}
```

### GameScreen Integration

Add to your game screen to show achievement unlocks:

```kotlin
@Composable
fun GameScreen(
    viewModel: GameViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Your existing content...

        // Show achievement unlock animation
        if (uiState.showAchievementUnlock && uiState.newAchievement != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                AchievementUnlockAnimation(
                    achievementTitle = uiState.newAchievement.title,
                    achievementIcon = uiState.newAchievement.icon,
                    onAnimationComplete = { viewModel.clearAchievementUnlock() }
                )
            }
        }

        // Show level up animation
        if (uiState.showLevelUpAnimation && uiState.newLevel != null) {
            LevelUpAnimation(
                newLevel = uiState.newLevel,
                onAnimationComplete = { viewModel.clearLevelUpAnimation() }
            )
        }
    }
}
```

## Navigation Integration

Update your navigation host to pass ViewModels and refresh after rating:

```kotlin
// In NavHost.kt or wherever navigation is defined
composable("dishRating/{dishName}/{imageUri}") { backStackEntry ->
    val dishName = backStackEntry.arguments?.getString("dishName") ?: ""
    val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""

    DarkDishRatingScreen(
        dishName = dishName,
        imageUri = imageUri,
        viewModel = dishRatingViewModel,
        profileViewModel = profileViewModel,
        gameViewModel = gameViewModel,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

## How It Works

### 1. Rating Submission Flow
1. User rates a dish
2. `DishRatingViewModel` calculates XP (10 base + 5 photo + 10 comment)
3. XP is awarded via `addXpToUser()` (properly awaited)
4. Streak is updated via `updateUserStreak()`
5. Achievements are checked via `AchievementService`
6. UI state updates with `xpEarned` and `showXpNotification = true`
7. XP gain animation displays
8. Profile and game data refresh

### 2. Level Up Detection
1. Profile refreshes after XP gain
2. `UserProgressViewModel.loadProgress()` compares old vs new level
3. If `newLevel > oldLevel`, sets `showLevelUpAnimation = true`
4. Level up animation displays full-screen
5. User clicks/taps to dismiss, calling `clearLevelUpAnimation()`

### 3. Challenge Completion
1. User activity updates stats (ratings, photos, etc.)
2. `ChallengeRepository.getUserChallenges()` loads challenges with real progress
3. Detects newly completed challenges (progress = 1.0, not previously completed)
4. Automatically awards XP via `addXpToUser()`
5. Marks challenge as completed in local state
6. GameViewModel reloads to show updated challenges

### 4. Achievement Unlock
1. After each rating, `AchievementService.checkAndAwardAchievements()` runs
2. Checks user stats against achievement criteria
3. Awards badge if criteria met and not already awarded
4. Returns list of newly unlocked achievements
5. ViewModel can trigger unlock animation if needed

## Testing the System

1. **Test XP Gain:**
   - Rate a dish → See "+10 XP" notification
   - Add photo → See "+15 XP" notification
   - Add detailed comment → See "+25 XP" notification

2. **Test Streak:**
   - Rate on consecutive days → Streak increments
   - Check profile → Streak count increases
   - Skip a day → Streak resets to 1

3. **Test Level Up:**
   - Submit 10 ratings (10 XP each = 100 XP)
   - Level increases from 1 to 2
   - See level up animation

4. **Test Challenges:**
   - Rate 3 dishes in one day
   - "Daily Explorer" progress goes 0.33 → 0.66 → 1.0
   - Challenge completes, awards 50 XP automatically

5. **Test Achievements:**
   - Submit first rating → "First Bite" unlocks
   - Rate 10 unique restaurants → "Foodie Explorer" unlocks
   - Check profile → Badge count increases

## Database Schema

Ensure your Supabase tables have these columns:

### profiles table
- `xp` (integer, default 0)
- `level` (integer, default 1)
- `streak_count` (integer, default 0)

### badges table
- `id` (text, primary key)
- `name` (text)
- `description` (text)
- `icon_url` (text, optional)

### user_badges table
- `user_id` (uuid, foreign key to profiles)
- `badge_id` (text, foreign key to badges)
- `earned_at` (timestamp)

### ratings table
- `user_id` (uuid)
- `dish_id` (uuid)
- `restaurant_id` (uuid)
- `rating` (float)
- `comment` (text)
- `image_url` (text, optional)
- `created_at` (timestamp)

## Customization

### Adjust XP Rewards
Edit in `DishRatingViewModel.kt`:
```kotlin
val baseXp = 10        // Change base reward
val photoBonus = 5     // Change photo bonus
val commentBonus = 10  // Change comment bonus (if > 50 chars)
```

### Adjust Level Thresholds
Edit in `DatabaseRepository.kt`:
```kotlin
private fun calculateLevel(xp: Int): Int {
    return (xp / 100) + 1  // Change 100 to desired XP per level
}
```

### Modify Challenge Criteria
Edit in `ChallengeRepository.kt`:
```kotlin
Challenge(
    id = "daily_rate_3",
    title = "Daily Explorer",
    description = "Rate 3 dishes today",  // Change goal
    xpReward = 50,  // Change reward
    progress = (ratingsToday / 3f).coerceIn(0f, 1f),
    isCompleted = ratingsToday >= 3
)
```

### Adjust Achievement Requirements
Edit in `AchievementService.kt`:
```kotlin
val achievements = mapOf(
    "first_bite" to (ratingsCount >= 1),     // Change to >= 5 for harder
    "foodie_explorer" to (ratingsCount >= 10), // Change to >= 20
    "rating_streak" to (streak >= 7),        // Change to >= 14
    "cuisine_master" to (cuisines.size >= 15) // Change to >= 25
)
```

## Troubleshooting

**XP not updating:**
- Check that `addXpToUser()` result is properly awaited
- Verify profile table has `xp` and `level` columns
- Check console logs for errors

**Streak not incrementing:**
- Verify `created_at` timestamp is properly saved in ratings table
- Check that timestamp parsing works (ISO 8601 format)
- Ensure `updateUserStreak()` is called after rating

**Challenges not completing:**
- Verify stats methods return correct counts
- Check that challenge criteria match actual data
- Ensure ChallengeRepository detects new completions

**Animations not showing:**
- Verify UI state includes animation fields
- Check that `showXpNotification`, `showLevelUpAnimation` are set to true
- Ensure animation components are added to screen composables
- Call clear methods after animations complete

## Next Steps

Consider adding:
1. Daily/weekly challenge reset logic (cron job or scheduled task)
2. Leaderboard real-time updates
3. Social sharing XP bonuses
4. Combo multipliers for rapid ratings
5. Special event challenges with time limits
6. Prestige system after reaching max level
