-- ============================================================
-- Migration 012: Fix Push Notifications
--
-- 1. Rename 'type' → 'event_type' so app code and edge function match
-- 2. Drop constraints that may block new event types
-- 3. Drop admin_profiles_select_all RLS (causes infinite recursion)
-- 4. Recreate push trigger with proper Authorization header
-- ============================================================

-- 1. Rename type → event_type if needed
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'notifications' AND column_name = 'type'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'notifications' AND column_name = 'event_type'
  ) THEN
    ALTER TABLE public.notifications RENAME COLUMN "type" TO event_type;
  END IF;
END $$;

-- 2. Drop old constraints
ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS notifications_event_type_check;

-- 3. Drop the RLS policy that causes infinite recursion
DROP POLICY IF EXISTS admin_profiles_select_all ON public.profiles;

-- 4. Recreate push trigger function with Authorization header
-- Uses the project anon key so the edge function accepts the request
CREATE OR REPLACE FUNCTION public.notify_push_on_new_notification()
RETURNS TRIGGER AS $$
DECLARE
  _headers jsonb;
BEGIN
  -- Build headers as a variable to avoid Windows \r\n inside JSON string literals
  _headers := jsonb_build_object(
    'Content-Type', 'application/json',
    'Authorization', 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF5b3Btdmh0ZnV3YnNqeGhwZmdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjkyNjAyMTksImV4cCI6MjA4NDgzNjIxOX0.2siGUJfE3iLoaEKae5gycw_6mo748KKyi5C7YEHuUlQ'
  );

  PERFORM net.http_post(
    url := 'https://ayopmvhtfuwbsjxhpfgd.supabase.co/functions/v1/push',
    headers := _headers,
    body := jsonb_build_object(
      'type', 'INSERT',
      'table', 'notifications',
      'schema', 'public',
      'record', row_to_json(NEW),
      'old_record', NULL
    )
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_notification_insert_push ON public.notifications;
CREATE TRIGGER on_notification_insert_push
  AFTER INSERT ON public.notifications
  FOR EACH ROW
  EXECUTE FUNCTION public.notify_push_on_new_notification();
