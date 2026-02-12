-- COMPLETE UUID TO TEXT MIGRATION
-- This fixes ALL columns that need to support both UUID and TEXT formats
-- (Firebase UIDs and Google Place IDs)

-- ==================== BACKUP REMINDER ====================
-- IMPORTANT: This migration will modify your data structure
-- Consider backing up your database first if you have important data
-- ==================== ==================== ====================

BEGIN;

-- ==================== STEP 1: DROP ALL FOREIGN KEY CONSTRAINTS ====================
ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_id_fkey;
ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_pkey CASCADE;

ALTER TABLE public.user_badges DROP CONSTRAINT IF EXISTS user_badges_user_id_fkey;
ALTER TABLE public.user_badges DROP CONSTRAINT IF EXISTS user_badges_badge_id_fkey;

ALTER TABLE public.dishes DROP CONSTRAINT IF EXISTS dishes_restaurant_id_fkey;

ALTER TABLE public.ratings DROP CONSTRAINT IF EXISTS ratings_user_id_fkey;
ALTER TABLE public.ratings DROP CONSTRAINT IF EXISTS ratings_dish_id_fkey;
ALTER TABLE public.ratings DROP CONSTRAINT IF EXISTS ratings_restaurant_id_fkey;

ALTER TABLE public.likes DROP CONSTRAINT IF EXISTS likes_user_id_fkey;
ALTER TABLE public.likes DROP CONSTRAINT IF EXISTS likes_rating_id_fkey;

-- ==================== STEP 2: CONVERT PRIMARY KEYS TO TEXT ====================

-- Convert profiles.id (user IDs from Firebase)
ALTER TABLE public.profiles ALTER COLUMN id TYPE TEXT USING id::TEXT;
ALTER TABLE public.profiles ADD PRIMARY KEY (id);

-- Convert dishes.id
ALTER TABLE public.dishes ALTER COLUMN id TYPE TEXT USING id::TEXT;

-- Convert ratings.id
ALTER TABLE public.ratings ALTER COLUMN id TYPE TEXT USING id::TEXT;

-- Convert restaurants.id (for Google Place IDs)
ALTER TABLE public.restaurants ALTER COLUMN id TYPE TEXT USING id::TEXT;

-- Convert badges.id
ALTER TABLE public.badges ALTER COLUMN id TYPE TEXT USING id::TEXT;

-- Convert user_badges.id
ALTER TABLE public.user_badges ALTER COLUMN id TYPE TEXT USING id::TEXT;

-- Convert likes.id
ALTER TABLE public.likes ALTER COLUMN id TYPE TEXT USING id::TEXT;

-- ==================== STEP 3: CONVERT FOREIGN KEY COLUMNS ====================

-- Convert user_id columns (references profiles.id - Firebase UIDs)
ALTER TABLE public.ratings ALTER COLUMN user_id TYPE TEXT USING user_id::TEXT;
ALTER TABLE public.user_badges ALTER COLUMN user_id TYPE TEXT USING user_id::TEXT;
ALTER TABLE public.likes ALTER COLUMN user_id TYPE TEXT USING user_id::TEXT;

-- Convert dish_id columns
ALTER TABLE public.ratings ALTER COLUMN dish_id TYPE TEXT USING dish_id::TEXT;

-- Convert restaurant_id columns (for Google Place IDs)
ALTER TABLE public.dishes ALTER COLUMN restaurant_id TYPE TEXT USING restaurant_id::TEXT;
ALTER TABLE public.ratings ALTER COLUMN restaurant_id TYPE TEXT USING restaurant_id::TEXT;

-- Convert badge_id columns
ALTER TABLE public.user_badges ALTER COLUMN badge_id TYPE TEXT USING badge_id::TEXT;

-- Convert rating_id columns
ALTER TABLE public.likes ALTER COLUMN rating_id TYPE TEXT USING rating_id::TEXT;

-- ==================== STEP 4: RECREATE INDEXES ====================

-- Drop old UUID-based indexes
DROP INDEX IF EXISTS idx_ratings_user_id;
DROP INDEX IF EXISTS idx_ratings_dish_id;
DROP INDEX IF EXISTS idx_ratings_restaurant_id;
DROP INDEX IF EXISTS idx_dishes_restaurant_id;
DROP INDEX IF EXISTS idx_likes_rating_id;
DROP INDEX IF EXISTS idx_user_badges_user_id;
DROP INDEX IF EXISTS idx_restaurants_city;
DROP INDEX IF EXISTS idx_restaurants_cuisine;

-- Recreate indexes with TEXT type
CREATE INDEX idx_ratings_user_id ON public.ratings(user_id);
CREATE INDEX idx_ratings_dish_id ON public.ratings(dish_id);
CREATE INDEX idx_ratings_restaurant_id ON public.ratings(restaurant_id);
CREATE INDEX idx_dishes_restaurant_id ON public.dishes(restaurant_id);
CREATE INDEX idx_likes_rating_id ON public.likes(rating_id);
CREATE INDEX idx_user_badges_user_id ON public.user_badges(user_id);
CREATE INDEX idx_restaurants_city ON public.restaurants(city);
CREATE INDEX idx_restaurants_cuisine ON public.restaurants(cuisine);

-- ==================== STEP 5: RE-ENABLE RLS ====================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.badges ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_badges ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.restaurants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.dishes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ratings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.likes ENABLE ROW LEVEL SECURITY;

-- ==================== STEP 6: VERIFICATION ====================
DO $$
DECLARE
    error_count INTEGER := 0;
BEGIN
    -- Check all critical columns
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'profiles' AND column_name = 'id' AND data_type = 'text'
    ) THEN
        RAISE WARNING '✗ profiles.id is not TEXT';
        error_count := error_count + 1;
    ELSE
        RAISE NOTICE '✓ profiles.id is TEXT';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'ratings' AND column_name = 'user_id' AND data_type = 'text'
    ) THEN
        RAISE WARNING '✗ ratings.user_id is not TEXT';
        error_count := error_count + 1;
    ELSE
        RAISE NOTICE '✓ ratings.user_id is TEXT';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'ratings' AND column_name = 'dish_id' AND data_type = 'text'
    ) THEN
        RAISE WARNING '✗ ratings.dish_id is not TEXT';
        error_count := error_count + 1;
    ELSE
        RAISE NOTICE '✓ ratings.dish_id is TEXT';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'ratings' AND column_name = 'restaurant_id' AND data_type = 'text'
    ) THEN
        RAISE WARNING '✗ ratings.restaurant_id is not TEXT';
        error_count := error_count + 1;
    ELSE
        RAISE NOTICE '✓ ratings.restaurant_id is TEXT';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'dishes' AND column_name = 'restaurant_id' AND data_type = 'text'
    ) THEN
        RAISE WARNING '✗ dishes.restaurant_id is not TEXT';
        error_count := error_count + 1;
    ELSE
        RAISE NOTICE '✓ dishes.restaurant_id is TEXT';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'restaurants' AND column_name = 'id' AND data_type = 'text'
    ) THEN
        RAISE WARNING '✗ restaurants.id is not TEXT';
        error_count := error_count + 1;
    ELSE
        RAISE NOTICE '✓ restaurants.id is TEXT';
    END IF;

    IF error_count = 0 THEN
        RAISE NOTICE '✓✓✓ ALL MIGRATIONS SUCCESSFUL! ✓✓✓';
    ELSE
        RAISE WARNING '✗✗✗ Migration completed with % errors ✗✗✗', error_count;
    END IF;
END $$;

COMMIT;

-- ==================== COMPLETE ====================
-- Migration complete!
-- All UUID columns have been converted to TEXT to support:
-- - Firebase UIDs (user authentication)
-- - Google Place IDs (restaurant locations)
-- - Future text-based identifiers
