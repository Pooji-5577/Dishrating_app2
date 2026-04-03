-- Rename 'type' column to 'event_type' in notifications table
-- to match the column name used by the app and admin panel.
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

-- Drop old CHECK constraint if it exists (was already dropped in 008 but just in case)
ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS notifications_event_type_check;
