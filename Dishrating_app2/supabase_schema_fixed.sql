-- SmackCheck Supabase Database Schema (Fixed Version)
-- This script handles existing tables and policies gracefully

-- ==================== PROFILES TABLE ====================
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL DEFAULT '',
    email TEXT NOT NULL,
    profile_photo_url TEXT,
    level INTEGER NOT NULL DEFAULT 1,
    xp INTEGER NOT NULL DEFAULT 0,
    streak_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==================== BADGES TABLE ====================
CREATE TABLE IF NOT EXISTS public.badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    icon_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==================== USER BADGES TABLE ====================
CREATE TABLE IF NOT EXISTS public.user_badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    badge_id UUID NOT NULL REFERENCES public.badges(id) ON DELETE CASCADE,
    earned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, badge_id)
);

-- ==================== RESTAURANTS TABLE ====================
CREATE TABLE IF NOT EXISTS public.restaurants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    city TEXT NOT NULL,
    cuisine TEXT NOT NULL,
    image_urls TEXT[] DEFAULT '{}',
    average_rating REAL DEFAULT 0,
    review_count INTEGER DEFAULT 0,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==================== DISHES TABLE ====================
CREATE TABLE IF NOT EXISTS public.dishes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    image_url TEXT,
    restaurant_id UUID NOT NULL REFERENCES public.restaurants(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==================== RATINGS TABLE ====================
CREATE TABLE IF NOT EXISTS public.ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    dish_id UUID NOT NULL REFERENCES public.dishes(id) ON DELETE CASCADE,
    restaurant_id UUID NOT NULL REFERENCES public.restaurants(id) ON DELETE CASCADE,
    rating REAL NOT NULL CHECK (rating >= 0 AND rating <= 5),
    comment TEXT DEFAULT '',
    image_url TEXT,
    likes_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==================== LIKES TABLE ====================
CREATE TABLE IF NOT EXISTS public.likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    rating_id UUID NOT NULL REFERENCES public.ratings(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, rating_id)
);

-- ==================== ENABLE RLS ====================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.badges ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_badges ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.restaurants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.dishes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ratings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.likes ENABLE ROW LEVEL SECURITY;

-- ==================== DROP EXISTING POLICIES ====================
DROP POLICY IF EXISTS "Users can view all profiles" ON public.profiles;
DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
DROP POLICY IF EXISTS "Profiles are viewable by everyone" ON public.profiles;
DROP POLICY IF EXISTS "Users can update their own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users can insert their own profile" ON public.profiles;

DROP POLICY IF EXISTS "Anyone can view badges" ON public.badges;
DROP POLICY IF EXISTS "Badges are viewable by everyone" ON public.badges;

DROP POLICY IF EXISTS "Users can view all user badges" ON public.user_badges;
DROP POLICY IF EXISTS "System can insert user badges" ON public.user_badges;
DROP POLICY IF EXISTS "User badges are viewable by everyone" ON public.user_badges;
DROP POLICY IF EXISTS "Users can insert their own badges" ON public.user_badges;

DROP POLICY IF EXISTS "Anyone can view restaurants" ON public.restaurants;
DROP POLICY IF EXISTS "Authenticated users can create restaurants" ON public.restaurants;
DROP POLICY IF EXISTS "Authenticated users can update restaurants" ON public.restaurants;
DROP POLICY IF EXISTS "Restaurants are viewable by everyone" ON public.restaurants;

DROP POLICY IF EXISTS "Anyone can view dishes" ON public.dishes;
DROP POLICY IF EXISTS "Authenticated users can create dishes" ON public.dishes;
DROP POLICY IF EXISTS "Dishes are viewable by everyone" ON public.dishes;

DROP POLICY IF EXISTS "Anyone can view ratings" ON public.ratings;
DROP POLICY IF EXISTS "Authenticated users can create ratings" ON public.ratings;
DROP POLICY IF EXISTS "Users can update own ratings" ON public.ratings;
DROP POLICY IF EXISTS "Users can delete own ratings" ON public.ratings;
DROP POLICY IF EXISTS "Ratings are viewable by everyone" ON public.ratings;

DROP POLICY IF EXISTS "Anyone can view likes" ON public.likes;
DROP POLICY IF EXISTS "Authenticated users can create likes" ON public.likes;
DROP POLICY IF EXISTS "Users can delete own likes" ON public.likes;
DROP POLICY IF EXISTS "Likes are viewable by everyone" ON public.likes;

-- ==================== CREATE POLICIES ====================

-- Profiles policies
CREATE POLICY "profiles_select" ON public.profiles FOR SELECT USING (true);
CREATE POLICY "profiles_insert" ON public.profiles FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "profiles_update" ON public.profiles FOR UPDATE USING (auth.uid() = id);

-- Badges policies
CREATE POLICY "badges_select" ON public.badges FOR SELECT USING (true);

-- User badges policies
CREATE POLICY "user_badges_select" ON public.user_badges FOR SELECT USING (true);
CREATE POLICY "user_badges_insert" ON public.user_badges FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Restaurants policies
CREATE POLICY "restaurants_select" ON public.restaurants FOR SELECT USING (true);
CREATE POLICY "restaurants_insert" ON public.restaurants FOR INSERT WITH CHECK (auth.role() = 'authenticated');
CREATE POLICY "restaurants_update" ON public.restaurants FOR UPDATE USING (auth.role() = 'authenticated');

-- Dishes policies
CREATE POLICY "dishes_select" ON public.dishes FOR SELECT USING (true);
CREATE POLICY "dishes_insert" ON public.dishes FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- Ratings policies
CREATE POLICY "ratings_select" ON public.ratings FOR SELECT USING (true);
CREATE POLICY "ratings_insert" ON public.ratings FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "ratings_update" ON public.ratings FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "ratings_delete" ON public.ratings FOR DELETE USING (auth.uid() = user_id);

-- Likes policies
CREATE POLICY "likes_select" ON public.likes FOR SELECT USING (true);
CREATE POLICY "likes_insert" ON public.likes FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "likes_delete" ON public.likes FOR DELETE USING (auth.uid() = user_id);

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

-- ==================== TRIGGER FOR UPDATED_AT ====================
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
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- ==================== SAMPLE DATA ====================
INSERT INTO public.badges (name, description, icon_url) VALUES
    ('First Bite', 'Submit your first dish rating', null),
    ('Food Explorer', 'Rate dishes at 5 different restaurants', null),
    ('Streak Master', 'Maintain a 7-day rating streak', null),
    ('Top Critic', 'Receive 50 likes on your ratings', null),
    ('Cuisine Connoisseur', 'Rate dishes from 10 different cuisines', null)
ON CONFLICT DO NOTHING;

INSERT INTO public.restaurants (name, city, cuisine, average_rating, review_count) VALUES
    ('Pasta Paradise', 'New York', 'Italian', 4.5, 120),
    ('Sushi Supreme', 'Los Angeles', 'Japanese', 4.7, 89),
    ('Curry House', 'Chicago', 'Indian', 4.3, 156),
    ('Taco Town', 'Houston', 'Mexican', 4.2, 78),
    ('Dragon Palace', 'San Francisco', 'Chinese', 4.4, 203)
ON CONFLICT DO NOTHING;
