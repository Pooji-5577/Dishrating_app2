-- Migration: Auto-create profile on user signup
-- This ensures every authenticated user has a profile row
-- Note: profiles.id is TEXT (Firebase migration), auth.users.id is UUID

-- Create or replace the function to handle new user signups
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, name, email)
    VALUES (
        NEW.id::text,  -- Cast UUID to TEXT
        COALESCE(NEW.raw_user_meta_data->>'name', split_part(NEW.email, '@', 1)),
        NEW.email
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Drop existing trigger if exists
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;

-- Create trigger for new user signup
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();

-- Backfill: Create profile rows for any existing users who don't have one
-- Skip users whose email already exists in profiles (Firebase migrated users)
INSERT INTO public.profiles (id, name, email)
SELECT 
    u.id::text,  -- Cast UUID to TEXT
    COALESCE(u.raw_user_meta_data->>'name', split_part(u.email::text, '@', 1)),
    u.email
FROM auth.users u
WHERE NOT EXISTS (
    SELECT 1 FROM public.profiles p WHERE p.id = u.id::text  -- No profile with this id
)
AND NOT EXISTS (
    SELECT 1 FROM public.profiles p WHERE p.email = u.email  -- No profile with this email
)
ON CONFLICT (id) DO NOTHING;

-- ==================== FIX FOREIGN KEYS FOR CASCADE ====================
-- Make FKs cascade on update so we can update profile ids without constraint errors

-- Ratings FK
ALTER TABLE public.ratings DROP CONSTRAINT IF EXISTS ratings_user_id_fkey;
ALTER TABLE public.ratings
    ADD CONSTRAINT ratings_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES public.profiles(id)
    ON UPDATE CASCADE ON DELETE CASCADE;

-- Likes FK (if exists)
ALTER TABLE public.likes DROP CONSTRAINT IF EXISTS likes_user_id_fkey;
ALTER TABLE public.likes
    ADD CONSTRAINT likes_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES public.profiles(id)
    ON UPDATE CASCADE ON DELETE CASCADE;

-- User badges FK (if exists)
ALTER TABLE public.user_badges DROP CONSTRAINT IF EXISTS user_badges_user_id_fkey;
ALTER TABLE public.user_badges
    ADD CONSTRAINT user_badges_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES public.profiles(id)
    ON UPDATE CASCADE ON DELETE CASCADE;

-- ==================== UPDATE PROFILE IDS ====================
-- Update existing profiles that have matching email but different id
-- This links Firebase-migrated profiles to their Supabase auth accounts
UPDATE public.profiles p
SET id = u.id::text
FROM auth.users u
WHERE p.email = u.email
AND p.id != u.id::text;
