-- ============================================================
-- Migration 008: Admin Panel & Enhanced Notifications
-- ============================================================

-- 1. Add is_admin column to profiles
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS is_admin BOOLEAN DEFAULT false;

-- 2. Add last_rating_at column to profiles (for inactivity tracking)
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS last_rating_at TIMESTAMPTZ;

-- 3. Create trigger to auto-update last_rating_at when a new rating is inserted
CREATE OR REPLACE FUNCTION public.update_last_rating_at()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE public.profiles SET last_rating_at = NOW() WHERE id = NEW.user_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_update_last_rating_at ON public.ratings;
CREATE TRIGGER trigger_update_last_rating_at
  AFTER INSERT ON public.ratings
  FOR EACH ROW EXECUTE FUNCTION public.update_last_rating_at();

-- 4. Drop old restrictive CHECK constraint on notifications.type (if it exists)
--    and replace with an expanded one that covers all event types
ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

-- The event_type column may be named 'type' or 'event_type' depending on migration history.
-- Add constraint on whichever column exists:
DO $$
BEGIN
  -- Try adding constraint on event_type column
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'notifications' AND column_name = 'event_type'
  ) THEN
    ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS notifications_event_type_check;
    -- No restrictive CHECK — allow any event type string
  END IF;

  -- Try adding constraint on type column
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'notifications' AND column_name = 'type'
  ) THEN
    ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
    -- No restrictive CHECK — allow any event type string
  END IF;
END $$;

-- 5. RLS policies for admin access
-- Admin can read all profiles
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies WHERE policyname = 'admin_profiles_select_all' AND tablename = 'profiles'
  ) THEN
    CREATE POLICY admin_profiles_select_all ON public.profiles
      FOR SELECT USING (
        EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid()::text AND p.is_admin = true)
      );
  END IF;
END $$;

-- Admin can insert notifications for any user
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies WHERE policyname = 'admin_notifications_insert' AND tablename = 'notifications'
  ) THEN
    CREATE POLICY admin_notifications_insert ON public.notifications
      FOR INSERT WITH CHECK (
        EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = auth.uid()::text AND p.is_admin = true)
      );
  END IF;
END $$;

-- 6. Backfill last_rating_at for existing users from their most recent rating
UPDATE public.profiles p
SET last_rating_at = sub.max_created
FROM (
  SELECT user_id, MAX(created_at) as max_created
  FROM public.ratings
  GROUP BY user_id
) sub
WHERE p.id = sub.user_id AND p.last_rating_at IS NULL;
