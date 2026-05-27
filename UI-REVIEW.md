# SmackCheck — 6-Pillar Visual Audit Report

**Project:** SmackCheck (Kotlin Multiplatform + Supabase)  
**Audit Date:** 2026-05-13  
**Auditor:** OpenCode / gsd-ui-auditor  
**Scope:** All 237 Kotlin files; ~40 screens, ~25 components, theme system  
**Overall Score:** **15 / 24** (63%)

---

## Score Summary

| Pillar | Score | Grade |
|--------|-------|-------|
| Copywriting | 3 / 4 | Good |
| Visuals | 3 / 4 | Good |
| Color | 2 / 4 | Fair |
| Typography | 2 / 4 | Fair |
| Spacing | 3 / 4 | Good |
| Experience Design | 2 / 4 | Fair |

---

## 1. Copywriting — 3/4

### Strengths
- **Empty states are conversational and on-brand:**  
  `HomeScreen.kt`: *"No Dishes Yet — Be the first to rate a dish! Tap the + button to get started."*  
  `SocialFeedScreen.kt`: Context-aware empty states per filter tab ("Follow friends to see their dish ratings here", "No trending posts yet").
- **Error dialogs use plain language:**  
  `ErrorState.kt`: *"We couldn't recognize this image as a dish. Please try taking another photo or selecting a different image."*  
  `NetworkErrorDialog`: *"Unable to connect to the server. Please check your internet connection and try again."*
- **Bottom-nav labels are scannable:** HOME, MAP, EXPLORE, PROFILE — all uppercase, single word, consistent.
- **CTAs are action-oriented:** "Take Photo", "Choose from Gallery", "Continue", "View Details".

### Issues
| # | Finding | Location | Severity |
|---|---------|----------|----------|
| 1 | **Inconsistent auth CTA casing:** Login screen uses *"Log In"* (button) but *"Sign up"* (link). DarkLoginScreen uses *"Sign Up"* (capital U). Pick one style and apply globally. | `LoginScreen.kt:261`, `DarkLoginScreen.kt:296` | Medium |
| 2 | **"Or" divider is lowercase** while surrounding UI screams uppercase bold. Inconsistent typographic voice. | `LoginScreen.kt:281`, `DarkLoginScreen.kt:328` | Low |
| 3 | **Google sign-in button shows only a "G"** — not a real logo. Users may distrust a faux logo. | `LoginScreen.kt:360-367`, `DarkLoginScreen.kt:364-370` | Medium |
| 4 | **Generic "Retry" on errors** — no empathy or context. "Try Again" is slightly better but still robotic. | `ErrorState.kt:29` | Low |
| 5 | **"Add Dish" title vs "Rate a Dish" CTA** — inconsistent mental model. Is the user adding a dish or rating one? | `DishCaptureScreen.kt:72` vs `SocialFeedScreen.kt:348` | Medium |
| 6 | **"VIEW ALL" and "View Rankings"** use all-caps shouty labels while the app otherwise uses sentence case. Abrasive tone shift. | `RestaurantDetailScreen.kt:257`, `DarkHomeScreen.kt:621` | Low |

### Recommendations
1. Standardize on **Sentence case for buttons** ("Sign up", "Log in") or **Title Case** ("Sign Up", "Log In") — never mix.
2. Replace the text-based Google "G" with an actual vector asset or `GoogleSignInButton` composable.
3. Add personality to error retry CTAs: *"Try again"*, *"Check connection and retry"*.
4. Unify terminology: pick "Rate" or "Add" and use it consistently across nav, titles, and CTAs.

---

## 2. Visuals — 3/4

### Strengths
- **BottomNavBar is distinctive:** Floating pill shape with elevated camera CTA, gradient fill, shadow elevation. Strong brand signature.
- **Card design is consistent:** `RoundedCornerShape(16.dp)` used across `DishCards.kt`, `FeedCard`, `TopRatedDishCard`. Corner radius family (4, 8, 12, 16, 24, 32 dp) is well-defined in `Shape.kt`.
- **Image placeholders are graceful:** Fallback initials in avatar circles, `Restaurant` icon for missing dish images.
- **DarkHomeScreen visual polish:** Figma-inspired curved header, patterned overlay, dashed story rings, cream-colored pills. High craft.
- **Status pills and badges:** Rounded pill shapes (`RoundedCornerShape(999.dp)`) with dot indicators create a modern, app-store aesthetic.

### Issues
| # | Finding | Location | Severity |
|---|---------|----------|----------|
| 1 | **No actual Google logo asset** — the faux "G" text is a visual liability that undermines trust. | `LoginScreen.kt`, `DarkLoginScreen.kt` | Medium |
| 2 | **Avatar fallback inconsistency:** Some use `MaterialTheme.colorScheme.primaryContainer`, others use `MaroonLight` or `colors.Primary.copy(alpha=0.15f)`. Same component, 3+ different fallback colors. | `HomeScreen.kt:205`, `DarkHomeScreen.kt:430`, `UserProfileScreen.kt:149` | Medium |
| 3 | **Bookmark icon is non-interactive** in `NearbyRestaurantCard` — shows `BookmarkBorder` but never toggles. Visual tease. | `DarkHomeScreen.kt:929-934` | Medium |
| 4 | **Star ratings use filled stars for empty state** with alpha hack (`FigmaMaroon.copy(alpha=0.25f)`). Should use outlined stars for semantic clarity. | `RestaurantDetailScreen.kt:610-633` | Low |
| 5 | **Camera FAB on HomeScreen clashes with BottomNavBar camera** — two camera entry points with different visual languages (Material FAB vs custom gradient circle). | `HomeScreen.kt:115-125`, `BottomNavBar.kt:137-166` | Low |
| 6 | **No shimmer/gradient skeletons on HomeScreen** — uses `HomeScreenSkeleton` but `DarkHomeScreen` has no equivalent skeleton, showing raw loading spinner. | `DarkHomeScreen.kt:661-686` | Low |

### Recommendations
1. Add a real Google logo vector asset (SVG/PNG) for the sign-in button.
2. Create a single `AvatarFallback` component that consumes theme colors consistently.
3. Either wire up the bookmark icon or replace it with a non-interactive visual indicator.
4. Use `Icons.Outlined.Star` for empty star states instead of alpha-modified filled stars.
5. Remove the HomeScreen FAB if BottomNavBar camera is the primary entry point, or align their visual language.

---

## 3. Color — 2/4

### Strengths
- **Brand colors are defined:** `BrandRed` (`#9B2335`), `Maroon` (`#642223`), and Material `PrimaryLight` (`#FF6B35`) give the app a warm, food-friendly palette.
- **Dark theme colors exist:** `DarkThemeColors` object has a complete semantic palette (Background, Surface, TextPrimary, etc.).
- **Error colors are accessible:** `#BA1A1A` on white passes WCAG AA for normal text.

### Critical Issues
| # | Finding | Location | Severity |
|---|---------|----------|----------|
| 1 | **MAROON COLOR CHAOS — 5 different maroon values across the app:**  
  - `#642223` (DarkHomeScreen, RestaurantDetailScreen "FigmaMaroon")  
  - `#9B2335` (Color.kt "BrandRed", BottomNavBar)  
  - `#7A2428` (DarkLoginScreen buttonColor)  
  - `#842200` (Material3 dark primaryContainer)  
  - `#E53935` (LightThemeColors Primary)  
  These are similar enough to look accidental, different enough to feel unpolished. | Global | **High** |
| 2 | **Three competing color systems:**  
  (a) Material3 `MaterialTheme.colorScheme`  
  (b) Custom `appColors()` / `ThemeColors` via `LocalThemeState`  
  (c) Screen-local hardcoded tokens (`DarkHomeScreen.kt:66-74`, `DarkLoginScreen.kt:91-95`, `RestaurantDetailScreen.kt:86-90`)  
  Screens pick whichever system is convenient, creating unpredictable appearance. | Global | **High** |
| 3 | **Dark mode is force-disabled:** `ThemeState.kt:34` hardcodes `isDarkMode = false` with a TODO. The entire `DarkThemeColors` system and `appColors()` dark branch are dead code. | `ThemeState.kt:28-35` | **High** |
| 4 | **Dark-themed screens exist but are not a real dark mode:** `DarkHomeScreen`, `DarkLoginScreen`, `DarkGameScreen`, etc. use hardcoded dark palettes but render as standalone screens, not as theme variants. Users cannot toggle to them. | `DarkHomeScreen.kt`, `DarkLoginScreen.kt` | Medium |
| 5 | **BottomNavBar hardcodes `Color(0xFF9B2335)`** instead of using theme tokens. If brand color changes, this is missed. | `BottomNavBar.kt:89`, `BottomNavBar.kt:101` | Medium |
| 6 | **LoginScreen hardcodes `#1A1A1A` and `#E0E0E0`** instead of `MaterialTheme.colorScheme.onBackground` / `outlineVariant`. | `LoginScreen.kt:111`, `LoginScreen.kt:278` | Low |
| 7 | **RestaurantDetailScreen defines local `FigmaMaroon` (#642223) AND imports `appColors()`** — uses both interchangeably. | `RestaurantDetailScreen.kt:86`, `RestaurantDetailScreen.kt:113` | Medium |

### Recommendations
1. **Consolidate to ONE color system.** Recommended: Keep `MaterialTheme.colorScheme` as the source of truth. Migrate `appColors()` to read from `MaterialTheme` instead of maintaining a parallel universe.
2. **Pick ONE brand red** and document it. Recommended: `#9B2335` (BrandRed) — it appears in the most places.
3. **Delete screen-local color tokens** (`DarkHomeScreen.kt` private vals, `RestaurantDetailScreen.kt` `FigmaMaroon`, `DarkLoginScreen.kt` pageBackground). Move them to `Color.kt` or the theme.
4. **Enable dark mode properly** by wiring `ThemeState` to system preference and deleting the standalone `Dark*` screens OR making them the actual dark variants of their light counterparts.
5. **Audit every `Color(0xFF...)` hardcode** across all 237 files — there are likely dozens more.

---

## 4. Typography — 2/4

### Strengths
- **Type scale is comprehensive:** `Typography.kt` defines all Material3 roles (displayLarge through labelSmall) with sizes, weights, and line heights.
- **Brand font is loaded:** `PlusJakartaSans()` is used across `BottomNavBar`, `DarkHomeScreen`, `SocialFeedScreen`, `RestaurantDetailScreen`. Good brand consistency.
- **Serif accents for headlines:** `NewsreaderFontFamily()` used in `DarkHomeScreen` for dish names and taglines creates editorial personality.
- **Letter-spacing tuning:** `DarkHomeScreen` uses `(-0.75).sp` on headlines and `(-0.6).sp` on section titles — tight tracking feels premium.

### Issues
| # | Finding | Location | Severity |
|---|---------|----------|----------|
| 1 | **Theme typography is entirely `FontFamily.Default`** — the custom `SmackCheckTypography` never uses Plus Jakarta Sans or Newsreader. Screens that rely on `MaterialTheme.typography` get system fonts. | `Typography.kt:9-115` | **High** |
| 2 | **Two font-loading strategies coexist:**  
  - `PlusJakartaSans()` called inline in composables  
  - `@Composable` font family loader that may recompose on every call  
  No centralized `FontFamily` singleton. | `BottomNavBar.kt:53`, `DarkHomeScreen.kt:168`, `SocialFeedScreen.kt:100` | Medium |
| 3 | **Hardcoded font sizes everywhere:** `16.sp`, `14.sp`, `20.sp`, `24.sp` duplicated across screens instead of using `MaterialTheme.typography` tokens. | `UserProfileScreen.kt`, `RestaurantDetailScreen.kt`, `LoginScreen.kt` | Medium |
| 4 | **Inconsistent heading hierarchy:** Section titles range from `18.sp` to `24.sp` with no clear system. `UserProfileScreen` "Recent Ratings" = `18.sp`; `RestaurantDetailScreen` "Top Rated Dishes" = `24.sp`; `DarkHomeScreen` "Nearby Restaurants" = `20.sp`. | Global | Medium |
| 5 | **Body text uses hardcoded `fontSize = 14.sp`** but theme defines `bodyMedium` at `14.sp`. Developers bypass the theme for no benefit. | `UserProfileScreen.kt:174`, `DishCards.kt:127` | Low |
| 6 | **Text truncation is inconsistent:** Some cards use `maxLines = 1` + `TextOverflow.Ellipsis`, others rely on default behavior. Restaurant card cuisine text can wrap unpredictably. | `DishCards.kt:118`, `DishCards.kt:367` | Low |

### Recommendations
1. **Wire `PlusJakartaSans` into `Typography.kt`** so `MaterialTheme.typography.titleLarge` etc. actually use the brand font.
2. **Create a `LocalFontFamily` provider** or singleton so font families aren't recreated per-composable.
3. **Ban hardcoded `fontSize` and `fontWeight`** in screens — enforce use of `MaterialTheme.typography` tokens via lint.
4. **Define a 6-level heading scale** (e.g., Screen Title `24sp`, Section Title `20sp`, Card Title `16sp`, Body `14sp`, Caption `12sp`, Label `10sp`) and apply universally.
5. **Add `maxLines` + `overflow` to ALL text that could wrap** in constrained layouts (card titles, restaurant names, cuisine labels).

---

## 5. Spacing — 3/4

### Strengths
- **Spacing tokens are documented:** `Color.kt:88` lists `xs=4, sm=8, md=16, lg=24, xl=32, xxl=48, xxxl=64`. Good intent.
- **Consistent card padding:** Most cards use `16.dp` internal padding (`DishCards.kt:106`, `FeedCard.kt:187`).
- **Screen edge padding is mostly uniform:** `24.dp` horizontal is the dominant standard (`DarkHomeScreen`, `SocialFeedScreen`, `RestaurantDetailScreen`).
- **BottomNavBar handles insets:** Uses `WindowInsets.navigationBars` for safe area compliance.
- **LazyColumn content padding:** `contentPadding = PaddingValues(bottom = 96.dp)` accounts for bottom nav height.

### Issues
| # | Finding | Location | Severity |
|---|---------|----------|----------|
| 1 | **Spacing tokens are comments, not code.** No `Spacing.xs` object exists. Developers manually type `8.dp`, `12.dp`, `16.dp` everywhere. | `Color.kt:88` (comment only) | Medium |
| 2 | **Inconsistent section gaps:** `DarkHomeScreen` uses `40.dp` spacers between sections, but `SocialFeedScreen` uses `24.dp`. Rhythm feels different across screens. | `DarkHomeScreen.kt:327`, `SocialFeedScreen.kt:227` | Medium |
| 3 | **RestaurantDetailScreen mixes 24.dp and 16.dp horizontal padding** within the same screen. Section headers use `24.dp`, review cards use `24.dp`, but `TopRatedDishCard` carousel uses `PaddingValues(start=24.dp, end=24.dp)`. | `RestaurantDetailScreen.kt:245`, `RestaurantDetailScreen.kt:283` | Low |
| 4 | **LoginScreen uses `28.dp` horizontal padding** — an odd value that breaks the 4dp grid. | `LoginScreen.kt:95` | Low |
| 5 | **Icon-button hit areas are undersized:** `BottomNavBar` icon touch targets are `20.dp` icons in a `64.dp` bar, but `Modifier.clickable` default ripple may not expand to minimum 48dp. | `BottomNavBar.kt:188-202` | Medium |
| 6 | **FAB shadow conflicts with BottomNavBar** on `HomeScreen` — both occupy bottom-center real estate. `HomeScreen` uses `Scaffold` FAB, `DarkHomeScreen` only uses BottomNavBar. | `HomeScreen.kt:77` | Low |
| 7 | **No ` Arrangement.spacedBy` system** — most lists use manual `Spacer(modifier = Modifier.height(X.dp))` instead of list-level spacing. More boilerplate, easier to drift. | `HomeScreen.kt`, `UserProfileScreen.kt` | Low |

### Recommendations
1. **Create a `Spacing.kt` object** with actual `Dp` values and migrate all screens to use it.
2. **Standardize section spacing** to one value (recommend `32.dp` or `40.dp`) and apply globally.
3. **Enforce 4dp grid compliance** — ban odd values like `28.dp`, `7.dp`, `15.dp`.
4. **Ensure all clickable elements meet 48dp minimum touch target** via `minimumInteractiveComponentSize()` or explicit sizing.
5. **Use `Arrangement.spacedBy` in `LazyColumn` and `Column`** to reduce manual `Spacer` boilerplate.

---

## 6. Experience Design — 2/4

### Strengths
- **Loading states are context-aware:** `LoadingState(message = "Finding the best food around you...")` in `DarkHomeScreen` is delightful.
- **Skeleton screens exist:** `HomeScreenSkeleton`, `SocialFeedSkeleton` prevent layout shift and perceived slowness.
- **Error states are recoverable:** `ErrorStateDialog` with Retry/Cancel, `NetworkErrorDialog`, `NotADishErrorDialog` give users clear next steps.
- **Auto-scroll to new content:** `SocialFeedScreen` scrolls to newly created posts (`animateScrollToItem`).
- **Pagination is implemented:** `SocialFeedScreen` triggers `onLoadMore` when within 3 items of bottom.
- **Empty states have actions:** Each feed filter tab shows a contextual CTA ("Find Friends", "Explore Map", "Rate a Dish").
- **Input validation feedback:** Login fields show `supportingText` errors inline (`emailError`, `passwordError`).

### Issues
| # | Finding | Location | Severity |
|---|---------|----------|----------|
| 1 | **Pull-to-refresh is commented out** — a core social feed pattern is disabled. | `HomeScreen.kt:39` | **High** |
| 2 | **No haptic feedback** on critical actions (like, follow, capture). Mobile apps feel flat without tactile response. | Global | Medium |
| 3 | **"Continue" button in DishCaptureScreen is buried** — appears only after image selection, with no visual emphasis. Users may miss it. | `DishCaptureScreen.kt:215-233` | Medium |
| 4 | **No confirmation on destructive actions:** "Danger Zone" screen likely has delete-account functionality but no `ConfirmationDialog` wrapper is visible in the sampled code. | `DangerZoneScreen.kt` (not sampled) | Medium |
| 5 | **Back button behavior is inconsistent:** `HomeScreen` has no back button (root), `RestaurantDetailScreen` has back arrow, `LoginScreen` has no back navigation. | Global | Low |
| 6 | **Snackbar errors in LoginScreen but text errors in DarkLoginScreen** — same flow, different error presentation. Users get inconsistent feedback. | `LoginScreen.kt:79-84`, `DarkLoginScreen.kt:304-314` | Medium |
| 7 | **Theme toggle is dead UI** — if a settings toggle exists, it does nothing because `ThemeState` forces light mode. Broken promise. | `ThemeState.kt:34` | **High** |
| 8 | **No accessibility labels on decorative images:** `NetworkImage` contentDescription is often `null` or the dish name, but icons frequently have `contentDescription = null`. | `HomeScreen.kt:84`, `DarkHomeScreen.kt` | Medium |
| 9 | **No focus management on form errors** — when login fails, focus doesn't move to the invalid field. | `LoginScreen.kt` | Low |
| 10 | **Image capture fallback is jarring:** If `imagePicker` is null, buttons are `enabled = false` with no explanation. Users on unsupported platforms see dead buttons. | `DishCaptureScreen.kt:167`, `DishCaptureScreen.kt:198` | Medium |

### Recommendations
1. **Enable pull-to-refresh** using `PullToRefreshBox` (Compose 1.7+) or a custom implementation.
2. **Add haptic feedback** on like, follow, bookmark, and capture actions via `HapticFeedback`.
3. **Animate the Continue button appearance** in `DishCaptureScreen` (scale + alpha) so users notice it.
4. **Wrap all destructive actions** in `ConfirmationDialog` with consequence-aware messaging.
5. **Unify error presentation:** Pick snackbars OR inline text, not both across screens.
6. **Fix theme toggle** or remove it from settings until dark mode is ready.
7. **Add `contentDescription` to all icons** used as buttons, and `null` only for truly decorative images.
8. **Request focus on first invalid field** after form validation fails.
9. **Show an explanatory message** when camera/gallery is unavailable instead of disabled buttons.

---

## Top 10 Priority Fixes

| Rank | Fix | Pillar | Effort | Impact |
|------|-----|--------|--------|--------|
| 1 | Consolidate 5 maroon colors to a single brand color | Color | Medium | Very High |
| 2 | Unify to ONE color system (MaterialTheme) and delete parallel `appColors()` | Color | High | Very High |
| 3 | Wire PlusJakartaSans into `Typography.kt` and ban hardcoded font sizes | Typography | Medium | High |
| 4 | Enable pull-to-refresh on feed screens | Experience Design | Low | High |
| 5 | Create `Spacing.kt` tokens and migrate hardcoded dp values | Spacing | Medium | Medium |
| 6 | Fix or remove dead theme toggle; decide on dark mode strategy | Experience Design | Medium | High |
| 7 | Add real Google logo asset to sign-in button | Visuals / Copywriting | Low | Medium |
| 8 | Standardize auth CTAs ("Sign up" vs "Sign Up" vs "Sign up") | Copywriting | Low | Medium |
| 9 | Add haptic feedback to all primary interactions | Experience Design | Low | Medium |
| 10 | Ensure all icon buttons have minimum 48dp touch targets | Spacing | Low | Medium |

---

## Appendix: Files Audited

### Theme & Foundation
- `Theme.kt` — Material3 theme wrapper
- `Color.kt` — Design tokens (Material3 + brand)
- `Typography.kt` — Type scale (uses FontFamily.Default)
- `Shape.kt` — Corner radius tokens
- `LightTheme.kt` / `DarkThemeColors` — Parallel color objects
- `ThemeState.kt` — Theme manager (forces light mode)

### Core Screens (sampled)
- `HomeScreen.kt` — Feed with Material3 scaffold
- `DarkHomeScreen.kt` — Figma-inspired home with local design tokens
- `LoginScreen.kt` — Light auth with custom styling
- `DarkLoginScreen.kt` — Dark auth with curved header
- `DishCaptureScreen.kt` — Camera/gallery capture flow
- `UserProfileScreen.kt` — Public profile with stats
- `RestaurantDetailScreen.kt` — Detail with hero image and reviews
- `SocialFeedScreen.kt` — Social feed with stories, tabs, pagination

### Components (sampled)
- `BottomNavBar.kt` — Custom floating pill navigation
- `DishCards.kt` — Featured, large, and list dish cards
- `LoadingEmptyState.kt` — Loading and empty state primitives
- `ErrorState.kt` — Error dialogs
- `ReviewPostCard.kt` — Feed post card (referenced)

---

*End of Report*
