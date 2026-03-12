-- ═══════════════════════════════════════════════════════════════════
-- SmackCheck Push Notifications Tables
-- Run this SQL in your Supabase SQL Editor (https://supabase.com/dashboard)
-- Run AFTER supabase_gamification_tables.sql (profiles table must exist)
-- ═══════════════════════════════════════════════════════════════════

-- 1. Add expo_push_token column to existing profiles table
ALTER TABLE profiles
  ADD COLUMN IF NOT EXISTS expo_push_token TEXT;

-- Index for fast token lookups
CREATE INDEX IF NOT EXISTS idx_profiles_expo_push_token
  ON profiles (id)
  WHERE expo_push_token IS NOT NULL;

-- 2. Notifications table — each row triggers a push notification via webhook
CREATE TABLE IF NOT EXISTS notifications (
  id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  title       TEXT NOT NULL DEFAULT 'SmackCheck',
  body        TEXT NOT NULL,
  data        JSONB DEFAULT '{}',       -- payload for deep linking: {"screen": "DishDetail", "dishId": "..."}
  event_type  TEXT NOT NULL,             -- 'review_liked', 'dish_comment', 'points_earned', 'challenge_completed', 'trending_dish'
  is_read     BOOLEAN DEFAULT false,
  created_at  TIMESTAMPTZ DEFAULT now()
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_notifications_user_id
  ON notifications (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_unread
  ON notifications (user_id, is_read)
  WHERE is_read = false;

-- Deduplicate: prevent the same event from generating duplicate notifications
-- e.g., same user liking the same review shouldn't create two notifications
CREATE UNIQUE INDEX IF NOT EXISTS idx_notifications_dedup
  ON notifications (user_id, event_type, (data->>'source_id'))
  WHERE data->>'source_id' IS NOT NULL;

-- ═══════════════════════════════════════════════════════════════════
-- Enable RLS (Row Level Security)
-- ═══════════════════════════════════════════════════════════════════

ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- Users can read their own notifications
CREATE POLICY "Users can read own notifications"
  ON notifications FOR SELECT
  USING (auth.uid() = user_id);

-- Users can mark their own notifications as read
CREATE POLICY "Users can update own notifications"
  ON notifications FOR UPDATE
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

-- Service role (Edge Functions) and authenticated users can insert notifications
-- The insert is typically done server-side or via triggers, but we allow it for flexibility
CREATE POLICY "Authenticated users can insert notifications"
  ON notifications FOR INSERT
  WITH CHECK (auth.uid() IS NOT NULL);

-- Users can delete their own notifications
CREATE POLICY "Users can delete own notifications"
  ON notifications FOR DELETE
  USING (auth.uid() = user_id);

-- ═══════════════════════════════════════════════════════════════════
-- Policy for profiles expo_push_token updates
-- (Only if not already covered by an existing profiles policy)
-- ═══════════════════════════════════════════════════════════════════

-- Users can update their own push token
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE tablename = 'profiles'
      AND policyname = 'Users can update own push token'
  ) THEN
    EXECUTE 'CREATE POLICY "Users can update own push token"
      ON profiles FOR UPDATE
      USING (auth.uid() = id)
      WITH CHECK (auth.uid() = id)';
  END IF;
END $$;

-- ═══════════════════════════════════════════════════════════════════
-- Helper function: Create a notification (callable from app or triggers)
-- ═══════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION create_notification(
  p_user_id   UUID,
  p_title     TEXT,
  p_body      TEXT,
  p_event_type TEXT,
  p_data      JSONB DEFAULT '{}'
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_notification_id UUID;
  v_has_token BOOLEAN;
BEGIN
  -- Only create notification if user has a push token registered
  SELECT (expo_push_token IS NOT NULL) INTO v_has_token
  FROM profiles
  WHERE id = p_user_id;

  IF v_has_token IS NULL OR v_has_token = false THEN
    RETURN NULL; -- No token, skip notification
  END IF;

  INSERT INTO notifications (user_id, title, body, event_type, data)
  VALUES (p_user_id, p_title, p_body, p_event_type, p_data)
  ON CONFLICT DO NOTHING  -- Respect dedup index
  RETURNING id INTO v_notification_id;

  RETURN v_notification_id;
END;
$$;
