-- Add push_token column to profiles table
-- This stores the FCM (Android) or APNs (iOS) device token for push notifications
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS push_token TEXT;

-- Enable pg_net extension for making HTTP calls from triggers
CREATE EXTENSION IF NOT EXISTS pg_net WITH SCHEMA extensions;

-- Function: calls the 'push' Edge Function whenever a new notification is inserted
CREATE OR REPLACE FUNCTION public.notify_push_on_new_notification()
RETURNS TRIGGER AS $$
BEGIN
  PERFORM extensions.net.http_post(
    url := 'https://ayopmvhtfuwbsjxhpfgd.supabase.co/functions/v1/push',
    headers := '{"Content-Type": "application/json"}'::jsonb,
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

-- Trigger: fires after each INSERT on the notifications table
DROP TRIGGER IF EXISTS on_notification_insert_push ON public.notifications;
CREATE TRIGGER on_notification_insert_push
  AFTER INSERT ON public.notifications
  FOR EACH ROW
  EXECUTE FUNCTION public.notify_push_on_new_notification();
