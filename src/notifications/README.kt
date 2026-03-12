/**
 * SmackCheck - Push Notifications Module
 *
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║  MIGRATED TO KOTLIN (Compose Multiplatform)                     ║
 * ║                                                                  ║
 * ║  This module was originally written in TypeScript for React      ║
 * ║  Native + Expo. It has been rewritten in Kotlin and integrated   ║
 * ║  into the KMP project.                                           ║
 * ║                                                                  ║
 * ║  New Kotlin files:                                               ║
 * ║  composeApp/src/commonMain/kotlin/.../notifications/             ║
 * ║    ├── NotificationModels.kt      (data classes & types)        ║
 * ║    ├── NotificationRepository.kt  (Supabase CRUD operations)    ║
 * ║    ├── NotificationViewModel.kt   (MVVM state management)       ║
 * ║    └── PushNotificationService.kt (expect/actual for FCM/APNs)  ║
 * ║                                                                  ║
 * ║  composeApp/src/androidMain/kotlin/.../notifications/            ║
 * ║    └── PushNotificationService.android.kt (FCM implementation)   ║
 * ║                                                                  ║
 * ║  composeApp/src/iosMain/kotlin/.../notifications/                ║
 * ║    └── PushNotificationService.ios.kt (APNs implementation)      ║
 * ║                                                                  ║
 * ║  UI:                                                             ║
 * ║  composeApp/src/commonMain/kotlin/.../ui/screens/                ║
 * ║    └── NotificationsScreen.kt     (Compose UI)                   ║
 * ║                                                                  ║
 * ║  Backend (unchanged — still works):                              ║
 * ║    supabase_push_notifications.sql  (database tables)            ║
 * ║    supabase/functions/push/         (Edge Function)              ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Migration mapping:
 *
 * TypeScript (old)                  →  Kotlin (new)
 * ─────────────────────────────────    ──────────────────────────────
 * supabaseClient.ts                →  data/SupabaseClient.kt (existing)
 * notificationTriggers.ts          →  notifications/NotificationRepository.kt
 * pushNotificationService.ts       →  notifications/PushNotificationService.kt
 * useNotifications.ts (React hook) →  notifications/NotificationViewModel.kt
 * index.ts (barrel exports)        →  (not needed in Kotlin)
 */
