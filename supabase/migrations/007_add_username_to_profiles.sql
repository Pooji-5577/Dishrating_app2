-- Add username column to profiles table
-- Nullable so existing rows are not broken; enforced at app level for new sign-ups.
ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS username TEXT;

-- Case-insensitive unique index (skips NULL rows, so existing accounts are unaffected)
CREATE UNIQUE INDEX IF NOT EXISTS profiles_username_lower_idx
  ON public.profiles (lower(username))
  WHERE username IS NOT NULL;
