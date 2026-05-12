-- Tighten notification insert permissions.
-- Removes permissive policies that allowed any authenticated user to
-- insert notifications for arbitrary recipients.

DO $$
BEGIN
  -- Legacy permissive policies from older migrations
  DROP POLICY IF EXISTS "notifications_insert" ON public.notifications;
  DROP POLICY IF EXISTS "System can insert notifications" ON public.notifications;

  -- Recreate with least privilege: authenticated users can only insert
  -- notifications addressed to themselves.
  CREATE POLICY "notifications_insert_self"
    ON public.notifications
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid()::text = user_id);
END $$;
