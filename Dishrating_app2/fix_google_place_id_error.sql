-- Fix for Google Place ID Error in Dishes and Ratings Tables
-- This migration changes restaurant_id columns from UUID to TEXT
-- to support both database UUIDs and Google Place IDs

-- ==================== STEP 1: DROP FOREIGN KEY CONSTRAINTS ====================
-- Drop the foreign key constraint on dishes.restaurant_id
ALTER TABLE public.dishes DROP CONSTRAINT IF EXISTS dishes_restaurant_id_fkey;

-- Drop the foreign key constraint on ratings.restaurant_id
ALTER TABLE public.ratings DROP CONSTRAINT IF EXISTS ratings_restaurant_id_fkey;

-- ==================== STEP 2: CHANGE COLUMN TYPES ====================
-- Change dishes.restaurant_id from UUID to TEXT
ALTER TABLE public.dishes ALTER COLUMN restaurant_id TYPE TEXT USING restaurant_id::TEXT;

-- Change ratings.restaurant_id from UUID to TEXT
ALTER TABLE public.ratings ALTER COLUMN restaurant_id TYPE TEXT USING restaurant_id::TEXT;

-- ==================== STEP 3: UPDATE INDEXES ====================
-- Drop old index if exists
DROP INDEX IF EXISTS idx_dishes_restaurant_id;
DROP INDEX IF EXISTS idx_ratings_restaurant_id;

-- Recreate indexes with TEXT type
CREATE INDEX idx_dishes_restaurant_id ON public.dishes(restaurant_id);
CREATE INDEX idx_ratings_restaurant_id ON public.ratings(restaurant_id);

-- ==================== STEP 4: VERIFICATION ====================
-- Verify the changes
DO $$
BEGIN
    -- Check dishes.restaurant_id column type
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'dishes'
        AND column_name = 'restaurant_id'
        AND data_type = 'text'
    ) THEN
        RAISE NOTICE '✓ dishes.restaurant_id is now TEXT';
    ELSE
        RAISE WARNING '✗ dishes.restaurant_id type change failed';
    END IF;

    -- Check ratings.restaurant_id column type
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'ratings'
        AND column_name = 'restaurant_id'
        AND data_type = 'text'
    ) THEN
        RAISE NOTICE '✓ ratings.restaurant_id is now TEXT';
    ELSE
        RAISE WARNING '✗ ratings.restaurant_id type change failed';
    END IF;
END $$;

-- ==================== STEP 5: RE-ENABLE RLS (if needed) ====================
-- Ensure RLS is enabled on all tables
ALTER TABLE public.dishes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ratings ENABLE ROW LEVEL SECURITY;

-- ==================== COMPLETE ====================
-- Migration complete!
-- The app can now accept both UUID format (database restaurants)
-- and TEXT format (Google Place IDs) for restaurant_id fields.
