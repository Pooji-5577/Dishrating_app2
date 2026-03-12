# SmackCheck Push Notification System

Complete push notification setup using **Supabase Edge Functions** and **Expo Push Notifications**.

---

## Architecture

```
┌──────────────────┐    INSERT    ┌──────────────────┐
│  React Native    │ ──────────► │  notifications    │
│  App (Expo)      │             │  table (Supabase) │
└──────────────────┘             └────────┬─────────┘
        ▲                                 │
        │                        Database Webhook
        │                                 ▼
        │                        ┌──────────────────┐
        │    Push Notification   │  Edge Function    │
        │◄─────────────────────  │  (push/index.ts)  │
        │                        └────────┬─────────┘
        │                                 │
        │                        Fetches expo_push_token
        │                        from profiles table
        │                                 │
        │                                 ▼
        │                        ┌──────────────────┐
        └────────────────────────│  Expo Push API    │
                                 │  exp.host/--/api  │
                                 └──────────────────┘
```

**Flow:**
1. App event occurs (like, comment, points earned, etc.)
2. A row is inserted into the `notifications` table
3. Database webhook fires, calling the `push` Edge Function
4. Edge Function fetches the user's Expo push token from `profiles`
5. Edge Function sends the notification via Expo Push API
6. Device receives the push notification

---

## Setup Guide

### 1. Database Setup

Run the SQL migration in your Supabase SQL Editor:

```bash
# The file is at the project root
cat supabase_push_notifications.sql
```

This creates:
- `expo_push_token` column on `profiles` table
- `notifications` table with RLS policies
- Deduplication index to prevent duplicate notifications
- `create_notification()` helper function

> **Run this AFTER** `supabase_gamification_tables.sql`

---

### 2. Deploy the Edge Function

```bash
# Navigate to project root
cd Dishrating_app2

# Create environment file
cp supabase/.env.local.example supabase/.env.local
# Edit .env.local and add your EXPO_ACCESS_TOKEN

# Deploy the push Edge Function
supabase functions deploy push

# Set the secrets
supabase secrets set --env-file supabase/.env.local
```

**Get your Expo Access Token:**
1. Go to [Expo Access Tokens](https://expo.dev/accounts/_/settings/access-tokens)
2. Create a new token
3. Enable "Enhanced Security for Push Notifications"
4. Add it to `.env.local`

---

### 3. Create Database Webhook

In your [Supabase Dashboard](https://supabase.com/dashboard):

1. Navigate to **Database → Webhooks**
2. Click **Create a new hook**
3. **Table:** `notifications`
4. **Events:** ✅ Insert
5. **Type:** Supabase Edge Functions
6. **Function:** `push`
7. **Method:** POST
8. **Timeout:** 1000ms
9. **Headers:** Add auth header with service key + `Content-Type: application/json`
10. Click **Create webhook**

---

### 4. Frontend Setup (React Native with Expo)

#### Required Dependencies

```bash
npx expo install expo-notifications expo-device expo-constants
npm install @supabase/supabase-js @react-native-async-storage/async-storage
npm install @react-navigation/native
```

#### App Config (`app.config.js`)

Add your Supabase and EAS config:

```js
export default {
  expo: {
    // ... existing config
    extra: {
      supabaseUrl: 'https://your-project.supabase.co',
      supabaseAnonKey: 'your-anon-key',
      eas: {
        projectId: 'your-expo-project-id',
      },
    },
    plugins: ['expo-notifications'],
    android: {
      // ... existing config
      useNextNotificationsApi: true,
    },
  },
}
```

---

## Usage Examples

### Initialize on App Start

```tsx
import { useNotifications } from './src/notifications'

function App() {
  const isAuthenticated = true // from your auth state
  
  const {
    pushToken,
    notifications,
    unreadCount,
    isLoading,
    refresh,
    markAsRead,
    markAllAsRead,
    cleanupOnLogout,
  } = useNotifications(isAuthenticated)

  return (
    // Your app with notification badge showing unreadCount
  )
}
```

### Trigger Notifications from App Events

```tsx
import {
  notifyReviewLiked,
  notifyDishComment,
  notifyPointsEarned,
  notifyChallengeCompleted,
  notifyTrendingDish,
} from './src/notifications'

// When someone likes a review
await notifyReviewLiked({
  reviewOwnerId: 'user-uuid',
  likerName: 'John',
  dishName: 'Butter Chicken',
  reviewId: 'review-uuid',
})

// When someone comments on a dish
await notifyDishComment({
  reviewOwnerId: 'user-uuid',
  commenterName: 'Jane',
  dishName: 'Pad Thai',
  dishId: 'dish-uuid',
  commentId: 'comment-uuid',
})

// When user earns points
await notifyPointsEarned({
  userId: 'user-uuid',
  points: 25,
  reason: 'rating a dish',
  actionId: 'action-uuid',
})

// When user completes a challenge
await notifyChallengeCompleted({
  userId: 'user-uuid',
  challengeTitle: 'Rate 3 Dishes',
  xpReward: 20,
  challengeId: 'challenge-uuid',
})

// When a dish is trending nearby
await notifyTrendingDish({
  userId: 'user-uuid',
  dishName: 'Margherita Pizza',
  restaurantName: 'Pizza Palace',
  dishId: 'dish-uuid',
})
```

### Handle Logout

```tsx
const handleLogout = async () => {
  await cleanupOnLogout() // Removes push token from Supabase
  await supabase.auth.signOut()
}
```

---

## File Structure

```
supabase_push_notifications.sql     ← Database migration
supabase/
  .env.local.example                ← Environment template
  functions/
    push/
      index.ts                      ← Edge Function (webhook handler)
src/
  notifications/
    index.ts                        ← Barrel exports
    supabaseClient.ts               ← Shared Supabase client
    pushNotificationService.ts      ← Token registration & management
    notificationTriggers.ts         ← Event-specific notification creators
    useNotifications.ts             ← React hook for component integration
```

---

## Features

| Feature | Implementation |
|---------|---------------|
| Permission request | `registerForPushNotifications()` |
| Token generation | Expo Push Token via `expo-notifications` |
| Token persistence | Saved to `profiles.expo_push_token` in Supabase |
| Token dedup | Only updates if token has changed |
| Foreground notifications | Configured via `setNotificationHandler` |
| Background/tap handling | `addNotificationResponseReceivedListener` |
| Cold start handling | `getLastNotificationResponseAsync` |
| Deep linking | Maps `data.screen` to navigation routes |
| Android channels | Social, Gamification, Discovery channels |
| Duplicate prevention | Unique DB index on `(user_id, event_type, source_id)` |
| Invalid token cleanup | Edge Function removes `DeviceNotRegistered` tokens |
| Logout cleanup | `removePushToken()` clears token from DB |
| Error handling | Every function returns typed `{success, error}` results |
| Unread count | `getUnreadNotificationCount()` with badge support |

---

## Notification Events

| Event | Trigger | Emoji |
|-------|---------|-------|
| `review_liked` | Someone likes a user's dish review | ❤️ |
| `dish_comment` | Someone comments on a dish | 💬 |
| `points_earned` | User earns gamification points | 🏆 |
| `challenge_completed` | User completes a daily/weekly challenge | 🎯 |
| `trending_dish` | A dish is trending near the user | 🔥 |

---

## Testing

1. Insert a test notification in the Supabase Table Editor:
   ```sql
   INSERT INTO notifications (user_id, title, body, event_type, data)
   VALUES (
     'your-user-uuid',
     '🧪 Test Notification',
     'This is a test push notification from SmackCheck!',
     'points_earned',
     '{"screen": "GameScreen", "source_id": "test_1"}'
   );
   ```

2. You should receive a push notification on your device immediately.
