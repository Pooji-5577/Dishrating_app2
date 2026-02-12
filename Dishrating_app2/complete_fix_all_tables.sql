-- COMPLETE FIX FOR ALL TABLES
-- This script forcefully converts ALL columns and sets TEXT defaults
-- Run this as a complete replacement for all previous migrations

BEGIN;

-- ==================== STEP 1: CREATE TEXT ID GENERATOR ====================
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

-- ==================== STEP 2: DROP ALL CONSTRAINTS ====================
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

-- Drop reviews constraints if table exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'reviews') THEN
        EXECUTE 'ALTER TABLE public.reviews DROP CONSTRAINT IF EXISTS reviews_pkey CASCADE';
        EXECUTE 'ALTER TABLE public.reviews DROP CONSTRAINT IF EXISTS reviews_user_id_fkey';
        EXECUTE 'ALTER TABLE public.reviews DROP CONSTRAINT IF EXISTS reviews_dish_id_fkey';
        EXECUTE 'ALTER TABLE public.reviews DROP CONSTRAINT IF EXISTS reviews_restaurant_id_fkey';
    END IF;
END $$;

-- ==================== STEP 3: CONVERT ALL COLUMNS TO TEXT ====================

-- Profiles table
ALTER TABLE public.profiles
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE TEXT USING id::TEXT;
ALTER TABLE public.profiles ADD PRIMARY KEY (id);

-- Badges table
ALTER TABLE public.badges
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE TEXT USING id::TEXT;
ALTER TABLE public.badges ADD PRIMARY KEY (id);
ALTER TABLE public.badges ALTER COLUMN id SET DEFAULT generate_text_id('badge_');

-- User badges table
ALTER TABLE public.user_badges
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE TEXT USING id::TEXT,
    ALTER COLUMN user_id TYPE TEXT USING user_id::TEXT,
    ALTER COLUMN badge_id TYPE TEXT USING badge_id::TEXT;
ALTER TABLE public.user_badges ADD PRIMARY KEY (id);
ALTER TABLE public.user_badges ALTER COLUMN id SET DEFAULT generate_text_id('ubadge_');

-- Restaurants table
ALTER TABLE public.restaurants
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE TEXT USING id::TEXT;
ALTER TABLE public.restaurants ADD PRIMARY KEY (id);
ALTER TABLE public.restaurants ALTER COLUMN id SET DEFAULT generate_text_id('rst_');

-- Dishes table
ALTER TABLE public.dishes
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE TEXT USING id::TEXT,
    ALTER COLUMN restaurant_id TYPE TEXT USING restaurant_id::TEXT;
ALTER TABLE public.dishes ADD PRIMARY KEY (id);
ALTER TABLE public.dishes ALTER COLUMN id SET DEFAULT generate_text_id('dish_');

-- Ratings table
ALTER TABLE public.ratings
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE TEXT USING id::TEXT,
    ALTER COLUMN user_id TYPE TEXT USING user_id::TEXT,
    ALTER COLUMN dish_id TYPE TEXT USING dish_id::TEXT,
    ALTER COLUMN restaurant_id TYPE TEXT USING restaurant_id::TEXT;
ALTER TABLE public.ratings ADD PRIMARY KEY (id);
ALTER TABLE public.ratings ALTER COLUMN id SET DEFAULT generate_text_id('rating_');

-- Likes table
ALTER TABLE public.likes
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE TEXT USING id::TEXT,
    ALTER COLUMN user_id TYPE TEXT USING user_id::TEXT,
    ALTER COLUMN rating_id TYPE TEXT USING rating_id::TEXT;
ALTER TABLE public.likes ADD PRIMARY KEY (id);
ALTER TABLE public.likes ALTER COLUMN id SET DEFAULT generate_text_id('like_');

-- Reviews table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'reviews') THEN
        EXECUTE 'ALTER TABLE public.reviews ALTER COLUMN id DROP DEFAULT';
        EXECUTE 'ALTER TABLE public.reviews ALTER COLUMN id TYPE TEXT USING id::TEXT';
        EXECUTE 'ALTER TABLE public.reviews ALTER COLUMN user_id TYPE TEXT USING user_id::TEXT';
        EXECUTE 'ALTER TABLE public.reviews ALTER COLUMN dish_id TYPE TEXT USING dish_id::TEXT';
        EXECUTE 'ALTER TABLE public.reviews ALTER COLUMN restaurant_id TYPE TEXT USING restaurant_id::TEXT';
        EXECUTE 'ALTER TABLE public.reviews ADD PRIMARY KEY (id)';
        EXECUTE 'ALTER TABLE public.reviews ALTER COLUMN id SET DEFAULT generate_text_id(''review_'')';
        RAISE NOTICE '✓ Reviews table converted to TEXT';
    END IF;
END $$;

-- ==================== STEP 4: RECREATE INDEXES ====================
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

-- Reviews indexes if table exists
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
END $$;

-- ==================== STEP 5: ENABLE RLS ====================
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
END $$;

-- ==================== STEP 6: VERIFICATION ====================
DO $$
DECLARE
    error_count INTEGER := 0;
    table_name TEXT;
    column_name TEXT;
    data_type TEXT;
BEGIN
    RAISE NOTICE '==================== VERIFICATION ====================';

    -- Check all critical columns
    FOR table_name, column_name, data_type IN
        SELECT t.table_name, t.column_name, t.data_type
        FROM information_schema.columns t
        WHERE t.table_schema = 'public'
        AND t.column_name IN ('id', 'user_id', 'dish_id', 'restaurant_id', 'rating_id', 'badge_id')
        ORDER BY t.table_name, t.column_name
    LOOP
        IF data_type != 'text' THEN
            RAISE WARNING '✗ %.% is % (should be text)', table_name, column_name, data_type;
            error_count := error_count + 1;
        ELSE
            RAISE NOTICE '✓ %.% is TEXT', table_name, column_name;
        END IF;
    END LOOP;

    RAISE NOTICE '';
    RAISE NOTICE 'Testing ID generation...';
    RAISE NOTICE 'Sample dish ID: %', generate_text_id('dish_');
    RAISE NOTICE 'Sample rating ID: %', generate_text_id('rating_');
    RAISE NOTICE 'Sample restaurant ID: %', generate_text_id('rst_');
    RAISE NOTICE '';

    IF error_count = 0 THEN
        RAISE NOTICE '✓✓✓ ALL CONVERSIONS SUCCESSFUL! ✓✓✓';
        RAISE NOTICE 'All UUID columns converted to TEXT';
        RAISE NOTICE 'All DEFAULT generators updated';
        RAISE NOTICE 'Database is ready for Firebase UIDs and Google Place IDs';
    ELSE
        RAISE WARNING '✗✗✗ Conversion completed with % errors ✗✗✗', error_count;
        RAISE WARNING 'Please check the warnings above';
    END IF;
END $$;

COMMIT;

-- ==================== COMPLETE ====================
-- Migration complete! Your database now supports:
-- ✓ Firebase UIDs (user authentication)
-- ✓ Google Place IDs (restaurant locations)
-- ✓ Generated TEXT IDs (dishes, ratings, etc.)
-- ✓ No more UUID format errors
