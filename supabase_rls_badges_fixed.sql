-- SmackCheck RLS & Badge Automation (Fixed Version)
-- Run this AFTER the main schema script

-- ==================== CLEAN UP ALL EXISTING POLICIES ====================
-- This removes ALL existing policies to avoid conflicts

DO $$
DECLARE
    r RECORD;
BEGIN
    -- Drop all policies on our tables
    FOR r IN (SELECT policyname, tablename FROM pg_policies WHERE schemaname = 'public') LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', r.policyname, r.tablename);
    END LOOP;
END $$;

-- ==================== ENABLE RLS ON ALL TABLES ====================
ALTER TABLE IF EXISTS public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.badges ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.user_badges ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.restaurants ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.dishes ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.ratings ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.likes ENABLE ROW LEVEL SECURITY;

-- ==================== PROFILES POLICIES ====================
CREATE POLICY "profiles_select_policy" ON public.profiles
    FOR SELECT USING (true);

CREATE POLICY "profiles_insert_policy" ON public.profiles
    FOR INSERT WITH CHECK (auth.uid() = id);

CREATE POLICY "profiles_update_policy" ON public.profiles
    FOR UPDATE USING (auth.uid() = id);

-- ==================== BADGES POLICIES ====================
CREATE POLICY "badges_select_policy" ON public.badges
    FOR SELECT USING (true);

-- ==================== USER BADGES POLICIES ====================
CREATE POLICY "user_badges_select_policy" ON public.user_badges
    FOR SELECT USING (true);

CREATE POLICY "user_badges_insert_policy" ON public.user_badges
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- ==================== RESTAURANTS POLICIES ====================
CREATE POLICY "restaurants_select_policy" ON public.restaurants
    FOR SELECT USING (true);

CREATE POLICY "restaurants_insert_policy" ON public.restaurants
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');

CREATE POLICY "restaurants_update_policy" ON public.restaurants
    FOR UPDATE USING (auth.role() = 'authenticated');

-- ==================== DISHES POLICIES ====================
CREATE POLICY "dishes_select_policy" ON public.dishes
    FOR SELECT USING (true);

CREATE POLICY "dishes_insert_policy" ON public.dishes
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- ==================== RATINGS POLICIES ====================
CREATE POLICY "ratings_select_policy" ON public.ratings
    FOR SELECT USING (true);

CREATE POLICY "ratings_insert_policy" ON public.ratings
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "ratings_update_policy" ON public.ratings
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "ratings_delete_policy" ON public.ratings
    FOR DELETE USING (auth.uid() = user_id);

-- ==================== LIKES POLICIES ====================
CREATE POLICY "likes_select_policy" ON public.likes
    FOR SELECT USING (true);

CREATE POLICY "likes_insert_policy" ON public.likes
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "likes_delete_policy" ON public.likes
    FOR DELETE USING (auth.uid() = user_id);

-- ==================== AUTO-CREATE PROFILE ON SIGNUP ====================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, name, email)
    VALUES (
        NEW.id,
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

-- ==================== BADGE AUTOMATION FUNCTIONS ====================

-- Function to check and award badges
CREATE OR REPLACE FUNCTION public.check_and_award_badges()
RETURNS TRIGGER AS $$
DECLARE
    v_user_id UUID;
    v_rating_count INTEGER;
    v_restaurant_count INTEGER;
    v_streak INTEGER;
    v_total_likes INTEGER;
    v_cuisine_count INTEGER;
    v_badge_id UUID;
BEGIN
    v_user_id := NEW.user_id;

    -- Count user's total ratings
    SELECT COUNT(*) INTO v_rating_count
    FROM public.ratings WHERE user_id = v_user_id;

    -- Count unique restaurants rated
    SELECT COUNT(DISTINCT restaurant_id) INTO v_restaurant_count
    FROM public.ratings WHERE user_id = v_user_id;

    -- Get user's streak
    SELECT streak_count INTO v_streak
    FROM public.profiles WHERE id = v_user_id;

    -- Count total likes received
    SELECT COALESCE(SUM(likes_count), 0) INTO v_total_likes
    FROM public.ratings WHERE user_id = v_user_id;

    -- Count unique cuisines rated
    SELECT COUNT(DISTINCT r.cuisine) INTO v_cuisine_count
    FROM public.ratings rt
    JOIN public.restaurants r ON rt.restaurant_id = r.id
    WHERE rt.user_id = v_user_id;

    -- Award "First Bite" badge (first rating)
    IF v_rating_count = 1 THEN
        SELECT id INTO v_badge_id FROM public.badges WHERE name = 'First Bite';
        IF v_badge_id IS NOT NULL THEN
            INSERT INTO public.user_badges (user_id, badge_id)
            VALUES (v_user_id, v_badge_id)
            ON CONFLICT DO NOTHING;
        END IF;
    END IF;

    -- Award "Food Explorer" badge (5 different restaurants)
    IF v_restaurant_count >= 5 THEN
        SELECT id INTO v_badge_id FROM public.badges WHERE name = 'Food Explorer';
        IF v_badge_id IS NOT NULL THEN
            INSERT INTO public.user_badges (user_id, badge_id)
            VALUES (v_user_id, v_badge_id)
            ON CONFLICT DO NOTHING;
        END IF;
    END IF;

    -- Award "Streak Master" badge (7-day streak)
    IF v_streak >= 7 THEN
        SELECT id INTO v_badge_id FROM public.badges WHERE name = 'Streak Master';
        IF v_badge_id IS NOT NULL THEN
            INSERT INTO public.user_badges (user_id, badge_id)
            VALUES (v_user_id, v_badge_id)
            ON CONFLICT DO NOTHING;
        END IF;
    END IF;

    -- Award "Top Critic" badge (50 likes)
    IF v_total_likes >= 50 THEN
        SELECT id INTO v_badge_id FROM public.badges WHERE name = 'Top Critic';
        IF v_badge_id IS NOT NULL THEN
            INSERT INTO public.user_badges (user_id, badge_id)
            VALUES (v_user_id, v_badge_id)
            ON CONFLICT DO NOTHING;
        END IF;
    END IF;

    -- Award "Cuisine Connoisseur" badge (10 cuisines)
    IF v_cuisine_count >= 10 THEN
        SELECT id INTO v_badge_id FROM public.badges WHERE name = 'Cuisine Connoisseur';
        IF v_badge_id IS NOT NULL THEN
            INSERT INTO public.user_badges (user_id, badge_id)
            VALUES (v_user_id, v_badge_id)
            ON CONFLICT DO NOTHING;
        END IF;
    END IF;

    -- Add XP for submitting a rating (10 XP per rating)
    UPDATE public.profiles
    SET xp = xp + 10,
        level = (xp + 10) / 100 + 1
    WHERE id = v_user_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Drop existing trigger if exists
DROP TRIGGER IF EXISTS on_rating_created ON public.ratings;

-- Create trigger for badge automation
CREATE TRIGGER on_rating_created
    AFTER INSERT ON public.ratings
    FOR EACH ROW
    EXECUTE FUNCTION public.check_and_award_badges();

-- ==================== STREAK TRACKING FUNCTION ====================
CREATE OR REPLACE FUNCTION public.update_user_streak()
RETURNS TRIGGER AS $$
DECLARE
    v_last_activity DATE;
    v_current_streak INTEGER;
BEGIN
    -- Get user's last activity date and current streak
    SELECT
        COALESCE((
            SELECT DATE(created_at)
            FROM public.ratings
            WHERE user_id = NEW.user_id
            ORDER BY created_at DESC
            OFFSET 1 LIMIT 1
        ), CURRENT_DATE - INTERVAL '2 days'),
        streak_count
    INTO v_last_activity, v_current_streak
    FROM public.profiles
    WHERE id = NEW.user_id;

    -- Check if this continues the streak or breaks it
    IF DATE(NEW.created_at) = v_last_activity + INTERVAL '1 day' THEN
        -- Continue streak
        UPDATE public.profiles
        SET streak_count = streak_count + 1
        WHERE id = NEW.user_id;
    ELSIF DATE(NEW.created_at) > v_last_activity + INTERVAL '1 day' THEN
        -- Streak broken, reset to 1
        UPDATE public.profiles
        SET streak_count = 1
        WHERE id = NEW.user_id;
    END IF;
    -- If same day, don't change streak

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Drop existing trigger if exists
DROP TRIGGER IF EXISTS on_rating_streak ON public.ratings;

-- Create trigger for streak tracking
CREATE TRIGGER on_rating_streak
    AFTER INSERT ON public.ratings
    FOR EACH ROW
    EXECUTE FUNCTION public.update_user_streak();

-- ==================== LIKES COUNT UPDATE ====================
CREATE OR REPLACE FUNCTION public.update_likes_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE public.ratings
        SET likes_count = likes_count + 1
        WHERE id = NEW.rating_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE public.ratings
        SET likes_count = GREATEST(likes_count - 1, 0)
        WHERE id = OLD.rating_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Drop existing triggers if exist
DROP TRIGGER IF EXISTS on_like_added ON public.likes;
DROP TRIGGER IF EXISTS on_like_removed ON public.likes;

-- Create triggers for likes count
CREATE TRIGGER on_like_added
    AFTER INSERT ON public.likes
    FOR EACH ROW
    EXECUTE FUNCTION public.update_likes_count();

CREATE TRIGGER on_like_removed
    AFTER DELETE ON public.likes
    FOR EACH ROW
    EXECUTE FUNCTION public.update_likes_count();

-- ==================== SUCCESS MESSAGE ====================
DO $$ BEGIN RAISE NOTICE 'RLS policies and badge automation setup complete!'; END $$;
