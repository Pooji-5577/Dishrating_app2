-- ============================================================
-- SmackCheck: Full fix script for TEXT-id (Firebase-migrated) tables
-- Run this entire file in Supabase SQL Editor
-- ============================================================

-- ==================== ENABLE RLS (idempotent) ====================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.badges ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_badges ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.restaurants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.dishes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ratings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.likes ENABLE ROW LEVEL SECURITY;

-- ==================== DROP ALL OLD POLICIES ====================
DO $$ DECLARE r RECORD;
BEGIN
  FOR r IN SELECT policyname, tablename FROM pg_policies WHERE schemaname = 'public' LOOP
    EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', r.policyname, r.tablename);
  END LOOP;
END $$;

-- ==================== RECREATE POLICIES (TEXT id cast) ====================
-- auth.uid() returns uuid, id columns are TEXT — cast required

-- Profiles
CREATE POLICY "profiles_select" ON public.profiles FOR SELECT USING (true);
CREATE POLICY "profiles_insert" ON public.profiles FOR INSERT WITH CHECK (auth.uid()::text = id);
CREATE POLICY "profiles_update" ON public.profiles FOR UPDATE USING (auth.uid()::text = id);

-- Badges
CREATE POLICY "badges_select" ON public.badges FOR SELECT USING (true);

-- User badges
CREATE POLICY "user_badges_select" ON public.user_badges FOR SELECT USING (true);
CREATE POLICY "user_badges_insert" ON public.user_badges FOR INSERT WITH CHECK (auth.uid()::text = user_id);

-- Restaurants
CREATE POLICY "restaurants_select" ON public.restaurants FOR SELECT USING (true);
CREATE POLICY "restaurants_insert" ON public.restaurants FOR INSERT WITH CHECK (auth.role() = 'authenticated');
CREATE POLICY "restaurants_update" ON public.restaurants FOR UPDATE USING (auth.role() = 'authenticated');

-- Dishes
CREATE POLICY "dishes_select" ON public.dishes FOR SELECT USING (true);
CREATE POLICY "dishes_insert" ON public.dishes FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- Ratings
CREATE POLICY "ratings_select" ON public.ratings FOR SELECT USING (true);
CREATE POLICY "ratings_insert" ON public.ratings FOR INSERT WITH CHECK (auth.uid()::text = user_id);
CREATE POLICY "ratings_update" ON public.ratings FOR UPDATE USING (auth.uid()::text = user_id);
CREATE POLICY "ratings_delete" ON public.ratings FOR DELETE USING (auth.uid()::text = user_id);

-- Likes
CREATE POLICY "likes_select" ON public.likes FOR SELECT USING (true);
CREATE POLICY "likes_insert" ON public.likes FOR INSERT WITH CHECK (auth.uid()::text = user_id);
CREATE POLICY "likes_delete" ON public.likes FOR DELETE USING (auth.uid()::text = user_id);

-- ==================== ADD MISSING COLUMNS TO PROFILES ====================
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS last_rating_date TIMESTAMPTZ;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS bio TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS last_location TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS followers_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS following_count INTEGER NOT NULL DEFAULT 0;

-- ==================== INDEXES ====================
CREATE INDEX IF NOT EXISTS idx_ratings_user_id ON public.ratings(user_id);
CREATE INDEX IF NOT EXISTS idx_ratings_dish_id ON public.ratings(dish_id);
CREATE INDEX IF NOT EXISTS idx_ratings_restaurant_id ON public.ratings(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_ratings_created_at ON public.ratings(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_dishes_restaurant_id ON public.dishes(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_restaurants_city ON public.restaurants(city);
CREATE INDEX IF NOT EXISTS idx_restaurants_cuisine ON public.restaurants(cuisine);
CREATE INDEX IF NOT EXISTS idx_likes_rating_id ON public.likes(rating_id);
CREATE INDEX IF NOT EXISTS idx_user_badges_user_id ON public.user_badges(user_id);

-- ==================== UPDATED_AT TRIGGER ====================
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_profiles_updated_at ON public.profiles;
CREATE TRIGGER update_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

-- ==================== BADGES: fix schema, add icon_url, seed ====================
-- badges.id has no DEFAULT — set one based on actual column type
DO $$
BEGIN
  IF (SELECT data_type FROM information_schema.columns
      WHERE table_schema='public' AND table_name='badges' AND column_name='id') = 'text' THEN
    ALTER TABLE public.badges ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;
  ELSE
    ALTER TABLE public.badges ALTER COLUMN id SET DEFAULT gen_random_uuid();
  END IF;
END $$;

-- Dynamically drop NOT NULL on ALL non-PK badges columns so seed inserts don't fail
DO $$
DECLARE col RECORD;
BEGIN
  FOR col IN
    SELECT c.column_name
    FROM information_schema.columns c
    WHERE c.table_schema = 'public'
      AND c.table_name = 'badges'
      AND c.is_nullable = 'NO'
      AND c.column_name NOT IN ('id', 'name', 'description')
  LOOP
    EXECUTE format('ALTER TABLE public.badges ALTER COLUMN %I DROP NOT NULL', col.column_name);
  END LOOP;
END $$;

ALTER TABLE public.badges ADD COLUMN IF NOT EXISTS icon_url TEXT;
ALTER TABLE public.badges ADD COLUMN IF NOT EXISTS category TEXT;
ALTER TABLE public.badges DROP CONSTRAINT IF EXISTS badges_name_key;
ALTER TABLE public.badges ADD CONSTRAINT badges_name_key UNIQUE (name);

INSERT INTO public.badges (id, name, description, category, icon_url)
SELECT gen_random_uuid(), 'First Bite', 'Submit your first dish rating', 'milestone', null
WHERE NOT EXISTS (SELECT 1 FROM public.badges WHERE name = 'First Bite');

INSERT INTO public.badges (id, name, description, category, icon_url)
SELECT gen_random_uuid(), 'Food Explorer', 'Rate dishes at 5 different restaurants', 'exploration', null
WHERE NOT EXISTS (SELECT 1 FROM public.badges WHERE name = 'Food Explorer');

INSERT INTO public.badges (id, name, description, category, icon_url)
SELECT gen_random_uuid(), 'Streak Master', 'Maintain a 7-day rating streak', 'streak', null
WHERE NOT EXISTS (SELECT 1 FROM public.badges WHERE name = 'Streak Master');

INSERT INTO public.badges (id, name, description, category, icon_url)
SELECT gen_random_uuid(), 'Top Critic', 'Receive 50 likes on your ratings', 'social', null
WHERE NOT EXISTS (SELECT 1 FROM public.badges WHERE name = 'Top Critic');

INSERT INTO public.badges (id, name, description, category, icon_url)
SELECT gen_random_uuid(), 'Cuisine Connoisseur', 'Rate dishes from 10 different cuisines', 'exploration', null
WHERE NOT EXISTS (SELECT 1 FROM public.badges WHERE name = 'Cuisine Connoisseur');

-- ==================== FOLLOWERS TABLE (TEXT ids) ====================
CREATE TABLE IF NOT EXISTS public.followers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id TEXT NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    following_id TEXT NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(follower_id, following_id),
    CHECK (follower_id != following_id)
);

ALTER TABLE public.followers ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "followers_select" ON public.followers;
DROP POLICY IF EXISTS "followers_insert" ON public.followers;
DROP POLICY IF EXISTS "followers_delete" ON public.followers;

CREATE POLICY "followers_select" ON public.followers FOR SELECT USING (true);
CREATE POLICY "followers_insert" ON public.followers FOR INSERT WITH CHECK (auth.uid()::text = follower_id);
CREATE POLICY "followers_delete" ON public.followers FOR DELETE USING (auth.uid()::text = follower_id);

CREATE INDEX IF NOT EXISTS idx_followers_follower_id ON public.followers(follower_id);
CREATE INDEX IF NOT EXISTS idx_followers_following_id ON public.followers(following_id);

-- ==================== COMMENTS TABLE (TEXT ids) ====================
CREATE TABLE IF NOT EXISTS public.comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rating_id TEXT NOT NULL REFERENCES public.ratings(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    parent_comment_id UUID REFERENCES public.comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL CHECK (char_length(content) > 0 AND char_length(content) <= 1000),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "comments_select" ON public.comments;
DROP POLICY IF EXISTS "comments_insert" ON public.comments;
DROP POLICY IF EXISTS "comments_update" ON public.comments;
DROP POLICY IF EXISTS "comments_delete" ON public.comments;

CREATE POLICY "comments_select" ON public.comments FOR SELECT USING (true);
CREATE POLICY "comments_insert" ON public.comments FOR INSERT WITH CHECK (auth.uid()::text = user_id);
CREATE POLICY "comments_update" ON public.comments FOR UPDATE USING (auth.uid()::text = user_id);
CREATE POLICY "comments_delete" ON public.comments FOR DELETE USING (auth.uid()::text = user_id);

CREATE INDEX IF NOT EXISTS idx_comments_rating_id ON public.comments(rating_id);
CREATE INDEX IF NOT EXISTS idx_comments_user_id ON public.comments(user_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent_id ON public.comments(parent_comment_id);

DROP TRIGGER IF EXISTS update_comments_updated_at ON public.comments;
CREATE TRIGGER update_comments_updated_at
    BEFORE UPDATE ON public.comments
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

-- ==================== NOTIFICATIONS TABLE (TEXT ids) ====================
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('follow', 'like', 'comment', 'achievement', 'visit')),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    data JSONB DEFAULT '{}',
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "notifications_select" ON public.notifications;
DROP POLICY IF EXISTS "notifications_insert" ON public.notifications;
DROP POLICY IF EXISTS "notifications_update" ON public.notifications;

CREATE POLICY "notifications_select" ON public.notifications FOR SELECT USING (auth.uid()::text = user_id);
CREATE POLICY "notifications_insert" ON public.notifications FOR INSERT WITH CHECK (true);
CREATE POLICY "notifications_update" ON public.notifications FOR UPDATE USING (auth.uid()::text = user_id);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON public.notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON public.notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON public.notifications(user_id, is_read);

-- ==================== RESTAURANT VISITS TABLE (TEXT ids) ====================
CREATE TABLE IF NOT EXISTS public.restaurant_visits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    restaurant_id TEXT NOT NULL REFERENCES public.restaurants(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    entered_at TIMESTAMPTZ DEFAULT NOW(),
    exited_at TIMESTAMPTZ,
    duration_minutes INTEGER
);

ALTER TABLE public.restaurant_visits ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "visits_select" ON public.restaurant_visits;
DROP POLICY IF EXISTS "visits_insert" ON public.restaurant_visits;
DROP POLICY IF EXISTS "visits_update" ON public.restaurant_visits;

CREATE POLICY "visits_select" ON public.restaurant_visits FOR SELECT USING (auth.uid()::text = user_id);
CREATE POLICY "visits_insert" ON public.restaurant_visits FOR INSERT WITH CHECK (auth.uid()::text = user_id);
CREATE POLICY "visits_update" ON public.restaurant_visits FOR UPDATE USING (auth.uid()::text = user_id);

CREATE INDEX IF NOT EXISTS idx_visits_user_id ON public.restaurant_visits(user_id);
CREATE INDEX IF NOT EXISTS idx_visits_restaurant_id ON public.restaurant_visits(restaurant_id);

-- ==================== RATING IMAGES TABLE (TEXT ids) ====================
CREATE TABLE IF NOT EXISTS public.rating_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rating_id TEXT NOT NULL REFERENCES public.ratings(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.rating_images ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "rating_images_select" ON public.rating_images;
DROP POLICY IF EXISTS "rating_images_insert" ON public.rating_images;

CREATE POLICY "rating_images_select" ON public.rating_images FOR SELECT USING (true);
CREATE POLICY "rating_images_insert" ON public.rating_images FOR INSERT WITH CHECK (
    auth.uid()::text = (SELECT user_id FROM public.ratings WHERE id = rating_id)
);

CREATE INDEX IF NOT EXISTS idx_rating_images_rating_id ON public.rating_images(rating_id);

-- ==================== FOLLOW COUNTS TRIGGER ====================
CREATE OR REPLACE FUNCTION public.update_follow_counts()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE public.profiles SET following_count = following_count + 1 WHERE id = NEW.follower_id;
        UPDATE public.profiles SET followers_count = followers_count + 1 WHERE id = NEW.following_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE public.profiles SET following_count = GREATEST(following_count - 1, 0) WHERE id = OLD.follower_id;
        UPDATE public.profiles SET followers_count = GREATEST(followers_count - 1, 0) WHERE id = OLD.following_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_follow_counts ON public.followers;
CREATE TRIGGER trigger_update_follow_counts
    AFTER INSERT OR DELETE ON public.followers
    FOR EACH ROW EXECUTE FUNCTION public.update_follow_counts();

-- ==================== REALTIME FOR NOTIFICATIONS ====================
ALTER TABLE public.notifications REPLICA IDENTITY FULL;
ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
