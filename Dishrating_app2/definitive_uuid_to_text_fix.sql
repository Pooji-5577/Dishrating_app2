-- DEFINITIVE UUID TO TEXT MIGRATION
-- This script comprehensively fixes ALL UUID columns in the database
-- Run this ONCE in Supabase SQL Editor

BEGIN;

-- ==================== AUDIT PHASE ====================
-- First, let's see what we're dealing with
DO $$
DECLARE
    r RECORD;
BEGIN
    RAISE NOTICE '==================== PRE-MIGRATION AUDIT ====================';
    RAISE NOTICE 'Checking all columns with UUID or TEXT types...';
    RAISE NOTICE '';

    FOR r IN
        SELECT table_name, column_name, data_type, column_default
        FROM information_schema.columns
        WHERE table_schema = 'public'
        AND column_name IN ('id', 'user_id', 'dish_id', 'restaurant_id', 'rating_id', 'badge_id', 'challenge_id', 'review_id')
        ORDER BY table_name, column_name
    LOOP
        RAISE NOTICE '  %.% - Type: %, Default: %', r.table_name, r.column_name, r.data_type, COALESCE(r.column_default, 'none');
    END LOOP;
    RAISE NOTICE '';
END $$;

-- ==================== FUNCTION CREATION ====================
-- Create the TEXT ID generator function (idempotent)
CREATE OR REPLACE FUNCTION generate_text_id(prefix TEXT DEFAULT '')
RETURNS TEXT AS $$
DECLARE
    chars TEXT := 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    result TEXT := prefix;
    i INTEGER;
BEGIN
    FOR i IN 1..20 LOOP
        result := result || substr(chars, floor(random() * length(chars) + 1)::int, 1);
    END LOOP;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- ==================== DROP ALL CONSTRAINTS ====================
-- Drop all foreign key and primary key constraints
ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_pkey CASCADE;
ALTER TABLE public.badges DROP CONSTRAINT IF EXISTS badges_pkey CASCADE;
ALTER TABLE public.user_badges DROP CONSTRAINT IF EXISTS user_badges_pkey CASCADE;
ALTER TABLE public.user_badges DROP CONSTRAINT IF EXISTS user_badges_user_id_fkey;
ALTER TABLE public.user_badges DROP CONSTRAINT IF EXISTS user_badges_badge_id_fkey;
ALTER TABLE public.restaurants DROP CONSTRAINT IF EXISTS restaurants_pkey CASCADE;
ALTER TABLE public.dishes DROP CONSTRAINT IF EXISTS dishes_pkey CASCADE;
ALTER TABLE public.dishes DROP CONSTRAINT IF EXISTS dishes_restaurant_id_fkey;
ALTER TABLE public.ratings DROP CONSTRAINT IF EXISTS ratings_pkey CASCADE;
ALTER TABLE public.ratings DROP CONSTRAINT IF EXISTS ratings_user_id_fkey;
ALTER TABLE public.ratings DROP CONSTRAINT IF EXISTS ratings_dish_id_fkey;
ALTER TABLE public.ratings DROP CONSTRAINT IF EXISTS ratings_restaurant_id_fkey;
ALTER TABLE public.likes DROP CONSTRAINT IF EXISTS likes_pkey CASCADE;
ALTER TABLE public.likes DROP CONSTRAINT IF EXISTS likes_user_id_fkey;
ALTER TABLE public.likes DROP CONSTRAINT IF EXISTS likes_rating_id_fkey;

-- Drop constraints for optional tables
DO $$
BEGIN
    -- Reviews table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'reviews') THEN
        EXECUTE 'ALTER TABLE public.reviews DROP CONSTRAINT IF EXISTS reviews_pkey CASCADE';
        EXECUTE 'ALTER TABLE public.reviews DROP CONSTRAINT IF EXISTS reviews_user_id_fkey';
        EXECUTE 'ALTER TABLE public.reviews DROP CONSTRAINT IF EXISTS reviews_dish_id_fkey';
        EXECUTE 'ALTER TABLE public.reviews DROP CONSTRAINT IF EXISTS reviews_restaurant_id_fkey';
    END IF;

    -- Challenges table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'challenges') THEN
        EXECUTE 'ALTER TABLE public.challenges DROP CONSTRAINT IF EXISTS challenges_pkey CASCADE';
    END IF;

    -- Review_likes table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'review_likes') THEN
        EXECUTE 'ALTER TABLE public.review_likes DROP CONSTRAINT IF EXISTS review_likes_pkey CASCADE';
        EXECUTE 'ALTER TABLE public.review_likes DROP CONSTRAINT IF EXISTS review_likes_user_id_fkey';
        EXECUTE 'ALTER TABLE public.review_likes DROP CONSTRAINT IF EXISTS review_likes_review_id_fkey';
    END IF;

    -- User_challenges table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_challenges') THEN
        EXECUTE 'ALTER TABLE public.user_challenges DROP CONSTRAINT IF EXISTS user_challenges_pkey CASCADE';
        EXECUTE 'ALTER TABLE public.user_challenges DROP CONSTRAINT IF EXISTS user_challenges_user_id_fkey';
        EXECUTE 'ALTER TABLE public.user_challenges DROP CONSTRAINT IF EXISTS user_challenges_challenge_id_fkey';
    END IF;
END $$;

-- ==================== CONVERT ALL COLUMNS TO TEXT ====================

-- 1. Profiles table (Firebase UIDs)
ALTER TABLE public.profiles ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.profiles ALTER COLUMN id TYPE TEXT USING CAST(id AS TEXT);
ALTER TABLE public.profiles ADD PRIMARY KEY (id);
-- No default for profiles.id (Firebase provides it)

-- 2. Badges table
ALTER TABLE public.badges ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.badges ALTER COLUMN id TYPE TEXT USING CAST(id AS TEXT);
ALTER TABLE public.badges ADD PRIMARY KEY (id);
ALTER TABLE public.badges ALTER COLUMN id SET DEFAULT generate_text_id('badge_');

-- 3. User_badges table
ALTER TABLE public.user_badges ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.user_badges ALTER COLUMN id TYPE TEXT USING CAST(id AS TEXT);
ALTER TABLE public.user_badges ALTER COLUMN user_id TYPE TEXT USING CAST(user_id AS TEXT);
ALTER TABLE public.user_badges ALTER COLUMN badge_id TYPE TEXT USING CAST(badge_id AS TEXT);
ALTER TABLE public.user_badges ADD PRIMARY KEY (id);
ALTER TABLE public.user_badges ALTER COLUMN id SET DEFAULT generate_text_id('ubadge_');

-- 4. Restaurants table (supports Google Place IDs)
ALTER TABLE public.restaurants ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.restaurants ALTER COLUMN id TYPE TEXT USING CAST(id AS TEXT);
ALTER TABLE public.restaurants ADD PRIMARY KEY (id);
ALTER TABLE public.restaurants ALTER COLUMN id SET DEFAULT generate_text_id('rst_');

-- 5. Dishes table
ALTER TABLE public.dishes ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.dishes ALTER COLUMN id TYPE TEXT USING CAST(id AS TEXT);
ALTER TABLE public.dishes ALTER COLUMN restaurant_id TYPE TEXT USING CAST(restaurant_id AS TEXT);
ALTER TABLE public.dishes ADD PRIMARY KEY (id);
ALTER TABLE public.dishes ALTER COLUMN id SET DEFAULT generate_text_id('dish_');

-- 6. Ratings table (CRITICAL - this is where the error occurs)
ALTER TABLE public.ratings ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.ratings ALTER COLUMN id TYPE TEXT USING CAST(id AS TEXT);
ALTER TABLE public.ratings ALTER COLUMN user_id TYPE TEXT USING CAST(user_id AS TEXT);
ALTER TABLE public.ratings ALTER COLUMN dish_id TYPE TEXT USING CAST(dish_id AS TEXT);
ALTER TABLE public.ratings ALTER COLUMN restaurant_id TYPE TEXT USING CAST(restaurant_id AS TEXT);
ALTER TABLE public.ratings ADD PRIMARY KEY (id);
ALTER TABLE public.ratings ALTER COLUMN id SET DEFAULT generate_text_id('rating_');

-- 7. Likes table
ALTER TABLE public.likes ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.likes ALTER COLUMN id TYPE TEXT USING CAST(id AS TEXT);
ALTER TABLE public.likes ALTER COLUMN user_id TYPE TEXT USING CAST(user_id AS TEXT);
ALTER TABLE public.likes ALTER COLUMN rating_id TYPE TEXT USING CAST(rating_id AS TEXT);
ALTER TABLE public.likes ADD PRIMARY KEY (id);
ALTER TABLE public.likes ALTER COLUMN id SET DEFAULT generate_text_id('like_');

-- 8. Optional tables (reviews, challenges, review_likes, user_challenges)
DO $$
BEGIN
    -- Reviews table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'reviews') THEN
        EXECUTE 'ALTER TABLE public.reviews ALTER COLUMN id DROP DEFAULT';
        EXECUTE 'ALTER TABLE public.reviews ALTER COLUMN id TYPE TEXT USING CAST(id AS TEXT)';
        EXECUTE 'ALTER TABLE public.reviews ALTER COLUMN user_id TYPE TEXT USING CAST(user_id AS TEXT)';
        EXECUTE 'ALTER TABLE public.reviews ALTER COLUMN dish_id TYPE TEXT USING CAST(dish_id AS TEXT)';
        EXECUTE 'ALTER TABLE public.reviews ALTER COLUMN restaurant_id TYPE TEXT USING CAST(restaurant_id AS TEXT)';
        EXECUTE 'ALTER TABLE public.reviews ADD PRIMARY KEY (id)';
        EXECUTE 'ALTER TABLE public.reviews ALTER COLUMN id SET DEFAULT generate_text_id(''review_'')';
        RAISE NOTICE '✓ Reviews table converted';
    END IF;

    -- Challenges table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'challenges') THEN
        EXECUTE 'ALTER TABLE public.challenges ALTER COLUMN id DROP DEFAULT';
        EXECUTE 'ALTER TABLE public.challenges ALTER COLUMN id TYPE TEXT USING CAST(id AS TEXT)';
        EXECUTE 'ALTER TABLE public.challenges ADD PRIMARY KEY (id)';
        EXECUTE 'ALTER TABLE public.challenges ALTER COLUMN id SET DEFAULT generate_text_id(''challenge_'')';
        RAISE NOTICE '✓ Challenges table converted';
    END IF;

    -- Review_likes table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'review_likes') THEN
        EXECUTE 'ALTER TABLE public.review_likes ALTER COLUMN id DROP DEFAULT';
        EXECUTE 'ALTER TABLE public.review_likes ALTER COLUMN id TYPE TEXT USING CAST(id AS TEXT)';
        -- Only convert user_id and review_id if they exist
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'review_likes' AND column_name = 'user_id') THEN
            EXECUTE 'ALTER TABLE public.review_likes ALTER COLUMN user_id TYPE TEXT USING CAST(user_id AS TEXT)';
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'review_likes' AND column_name = 'review_id') THEN
            EXECUTE 'ALTER TABLE public.review_likes ALTER COLUMN review_id TYPE TEXT USING CAST(review_id AS TEXT)';
        END IF;
        EXECUTE 'ALTER TABLE public.review_likes ADD PRIMARY KEY (id)';
        EXECUTE 'ALTER TABLE public.review_likes ALTER COLUMN id SET DEFAULT generate_text_id(''revlike_'')';
        RAISE NOTICE '✓ Review_likes table converted';
    END IF;

    -- User_challenges table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_challenges') THEN
        EXECUTE 'ALTER TABLE public.user_challenges ALTER COLUMN id DROP DEFAULT';
        EXECUTE 'ALTER TABLE public.user_challenges ALTER COLUMN id TYPE TEXT USING CAST(id AS TEXT)';
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'user_challenges' AND column_name = 'user_id') THEN
            EXECUTE 'ALTER TABLE public.user_challenges ALTER COLUMN user_id TYPE TEXT USING CAST(user_id AS TEXT)';
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'user_challenges' AND column_name = 'challenge_id') THEN
            EXECUTE 'ALTER TABLE public.user_challenges ALTER COLUMN challenge_id TYPE TEXT USING CAST(challenge_id AS TEXT)';
        END IF;
        EXECUTE 'ALTER TABLE public.user_challenges ADD PRIMARY KEY (id)';
        EXECUTE 'ALTER TABLE public.user_challenges ALTER COLUMN id SET DEFAULT generate_text_id(''userchall_'')';
        RAISE NOTICE '✓ User_challenges table converted';
    END IF;
END $$;

-- ==================== RECREATE FOREIGN KEY CONSTRAINTS ====================
-- Add foreign key constraints with TEXT types
ALTER TABLE public.user_badges ADD CONSTRAINT user_badges_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.profiles(id) ON DELETE CASCADE;
ALTER TABLE public.user_badges ADD CONSTRAINT user_badges_badge_id_fkey FOREIGN KEY (badge_id) REFERENCES public.badges(id) ON DELETE CASCADE;
ALTER TABLE public.dishes ADD CONSTRAINT dishes_restaurant_id_fkey FOREIGN KEY (restaurant_id) REFERENCES public.restaurants(id) ON DELETE CASCADE;
ALTER TABLE public.ratings ADD CONSTRAINT ratings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.profiles(id) ON DELETE CASCADE;
ALTER TABLE public.ratings ADD CONSTRAINT ratings_dish_id_fkey FOREIGN KEY (dish_id) REFERENCES public.dishes(id) ON DELETE CASCADE;
ALTER TABLE public.ratings ADD CONSTRAINT ratings_restaurant_id_fkey FOREIGN KEY (restaurant_id) REFERENCES public.restaurants(id) ON DELETE CASCADE;
ALTER TABLE public.likes ADD CONSTRAINT likes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.profiles(id) ON DELETE CASCADE;
ALTER TABLE public.likes ADD CONSTRAINT likes_rating_id_fkey FOREIGN KEY (rating_id) REFERENCES public.ratings(id) ON DELETE CASCADE;

-- Optional table foreign keys
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'reviews') THEN
        EXECUTE 'ALTER TABLE public.reviews ADD CONSTRAINT reviews_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.profiles(id) ON DELETE CASCADE';
        EXECUTE 'ALTER TABLE public.reviews ADD CONSTRAINT reviews_dish_id_fkey FOREIGN KEY (dish_id) REFERENCES public.dishes(id) ON DELETE CASCADE';
        EXECUTE 'ALTER TABLE public.reviews ADD CONSTRAINT reviews_restaurant_id_fkey FOREIGN KEY (restaurant_id) REFERENCES public.restaurants(id) ON DELETE CASCADE';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'review_likes') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'review_likes' AND column_name = 'user_id') THEN
            EXECUTE 'ALTER TABLE public.review_likes ADD CONSTRAINT review_likes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.profiles(id) ON DELETE CASCADE';
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'review_likes' AND column_name = 'review_id') THEN
            EXECUTE 'ALTER TABLE public.review_likes ADD CONSTRAINT review_likes_review_id_fkey FOREIGN KEY (review_id) REFERENCES public.reviews(id) ON DELETE CASCADE';
        END IF;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_challenges') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'user_challenges' AND column_name = 'user_id') THEN
            EXECUTE 'ALTER TABLE public.user_challenges ADD CONSTRAINT user_challenges_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.profiles(id) ON DELETE CASCADE';
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'user_challenges' AND column_name = 'challenge_id') THEN
            EXECUTE 'ALTER TABLE public.user_challenges ADD CONSTRAINT user_challenges_challenge_id_fkey FOREIGN KEY (challenge_id) REFERENCES public.challenges(id) ON DELETE CASCADE';
        END IF;
    END IF;
END $$;

-- ==================== RECREATE INDEXES ====================
-- Drop old indexes
DROP INDEX IF EXISTS idx_ratings_user_id;
DROP INDEX IF EXISTS idx_ratings_dish_id;
DROP INDEX IF EXISTS idx_ratings_restaurant_id;
DROP INDEX IF EXISTS idx_ratings_created_at;
DROP INDEX IF EXISTS idx_dishes_restaurant_id;
DROP INDEX IF EXISTS idx_likes_rating_id;
DROP INDEX IF EXISTS idx_likes_user_id;
DROP INDEX IF EXISTS idx_user_badges_user_id;
DROP INDEX IF EXISTS idx_user_badges_badge_id;
DROP INDEX IF EXISTS idx_restaurants_city;
DROP INDEX IF EXISTS idx_restaurants_cuisine;

-- Recreate indexes for TEXT types
CREATE INDEX idx_ratings_user_id ON public.ratings(user_id);
CREATE INDEX idx_ratings_dish_id ON public.ratings(dish_id);
CREATE INDEX idx_ratings_restaurant_id ON public.ratings(restaurant_id);
CREATE INDEX idx_ratings_created_at ON public.ratings(created_at DESC);
CREATE INDEX idx_dishes_restaurant_id ON public.dishes(restaurant_id);
CREATE INDEX idx_likes_rating_id ON public.likes(rating_id);
CREATE INDEX idx_likes_user_id ON public.likes(user_id);
CREATE INDEX idx_user_badges_user_id ON public.user_badges(user_id);
CREATE INDEX idx_user_badges_badge_id ON public.user_badges(badge_id);
CREATE INDEX idx_restaurants_city ON public.restaurants(city);
CREATE INDEX idx_restaurants_cuisine ON public.restaurants(cuisine);

-- Optional table indexes
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'reviews') THEN
        EXECUTE 'DROP INDEX IF EXISTS idx_reviews_user_id';
        EXECUTE 'DROP INDEX IF EXISTS idx_reviews_dish_id';
        EXECUTE 'DROP INDEX IF EXISTS idx_reviews_restaurant_id';
        EXECUTE 'CREATE INDEX idx_reviews_user_id ON public.reviews(user_id)';
        EXECUTE 'CREATE INDEX idx_reviews_dish_id ON public.reviews(dish_id)';
        EXECUTE 'CREATE INDEX idx_reviews_restaurant_id ON public.reviews(restaurant_id)';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'review_likes') THEN
        EXECUTE 'DROP INDEX IF EXISTS idx_review_likes_user_id';
        EXECUTE 'DROP INDEX IF EXISTS idx_review_likes_review_id';
        EXECUTE 'CREATE INDEX idx_review_likes_user_id ON public.review_likes(user_id)';
        EXECUTE 'CREATE INDEX idx_review_likes_review_id ON public.review_likes(review_id)';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_challenges') THEN
        EXECUTE 'DROP INDEX IF EXISTS idx_user_challenges_user_id';
        EXECUTE 'DROP INDEX IF EXISTS idx_user_challenges_challenge_id';
        EXECUTE 'CREATE INDEX idx_user_challenges_user_id ON public.user_challenges(user_id)';
        EXECUTE 'CREATE INDEX idx_user_challenges_challenge_id ON public.user_challenges(challenge_id)';
    END IF;
END $$;

-- ==================== RE-ENABLE RLS ====================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.badges ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_badges ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.restaurants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.dishes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ratings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.likes ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'reviews') THEN
        EXECUTE 'ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'challenges') THEN
        EXECUTE 'ALTER TABLE public.challenges ENABLE ROW LEVEL SECURITY';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'review_likes') THEN
        EXECUTE 'ALTER TABLE public.review_likes ENABLE ROW LEVEL SECURITY';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_challenges') THEN
        EXECUTE 'ALTER TABLE public.user_challenges ENABLE ROW LEVEL SECURITY';
    END IF;
END $$;

-- ==================== POST-MIGRATION VERIFICATION ====================
DO $$
DECLARE
    r RECORD;
    error_count INTEGER := 0;
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '==================== POST-MIGRATION AUDIT ====================';
    RAISE NOTICE 'Verifying all ID columns are now TEXT type...';
    RAISE NOTICE '';

    FOR r IN
        SELECT table_name, column_name, data_type, column_default
        FROM information_schema.columns
        WHERE table_schema = 'public'
        AND column_name IN ('id', 'user_id', 'dish_id', 'restaurant_id', 'rating_id', 'badge_id', 'challenge_id', 'review_id')
        ORDER BY table_name, column_name
    LOOP
        IF r.data_type != 'text' THEN
            RAISE WARNING '  ✗ %.% is still % (should be TEXT)', r.table_name, r.column_name, r.data_type;
            error_count := error_count + 1;
        ELSE
            RAISE NOTICE '  ✓ %.% - Type: TEXT, Default: %', r.table_name, r.column_name, COALESCE(r.column_default, 'none');
        END IF;
    END LOOP;

    RAISE NOTICE '';
    RAISE NOTICE 'Testing ID generation functions...';
    RAISE NOTICE '  Sample dish ID: %', generate_text_id('dish_');
    RAISE NOTICE '  Sample rating ID: %', generate_text_id('rating_');
    RAISE NOTICE '  Sample restaurant ID: %', generate_text_id('rst_');
    RAISE NOTICE '';

    IF error_count = 0 THEN
        RAISE NOTICE '✓✓✓ MIGRATION SUCCESSFUL! ✓✓✓';
        RAISE NOTICE 'All UUID columns converted to TEXT';
        RAISE NOTICE 'All DEFAULT generators updated to generate_text_id()';
        RAISE NOTICE 'Database ready for Firebase UIDs and Google Place IDs';
    ELSE
        RAISE WARNING '✗✗✗ MIGRATION FAILED with % errors ✗✗✗', error_count;
        RAISE WARNING 'Please review the warnings above';
    END IF;
END $$;

COMMIT;
