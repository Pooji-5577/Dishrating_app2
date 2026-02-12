-- FIX REVIEWS TABLE - Supplementary Migration
-- This fixes the reviews table that was missed in the main migration

BEGIN;

-- ==================== STEP 1: DROP REVIEWS TABLE CONSTRAINTS ====================
ALTER TABLE public.reviews DROP CONSTRAINT IF EXISTS reviews_user_id_fkey;
ALTER TABLE public.reviews DROP CONSTRAINT IF EXISTS reviews_dish_id_fkey;
ALTER TABLE public.reviews DROP CONSTRAINT IF EXISTS reviews_restaurant_id_fkey;
ALTER TABLE public.reviews DROP CONSTRAINT IF EXISTS reviews_pkey CASCADE;

-- ==================== STEP 2: CONVERT REVIEWS TABLE COLUMNS TO TEXT ====================

-- Convert reviews.id (primary key)
ALTER TABLE public.reviews ALTER COLUMN id TYPE TEXT USING id::TEXT;
ALTER TABLE public.reviews ADD PRIMARY KEY (id);

-- Convert reviews.user_id (references profiles.id - Firebase UID)
ALTER TABLE public.reviews ALTER COLUMN user_id TYPE TEXT USING user_id::TEXT;

-- Convert reviews.dish_id (references dishes.id)
ALTER TABLE public.reviews ALTER COLUMN dish_id TYPE TEXT USING dish_id::TEXT;

-- Convert reviews.restaurant_id (references restaurants.id - can be Google Place ID)
ALTER TABLE public.reviews ALTER COLUMN restaurant_id TYPE TEXT USING restaurant_id::TEXT;

-- ==================== STEP 3: RECREATE INDEXES FOR REVIEWS ====================

-- Drop old indexes if they exist
DROP INDEX IF EXISTS idx_reviews_user_id;
DROP INDEX IF EXISTS idx_reviews_dish_id;
DROP INDEX IF EXISTS idx_reviews_restaurant_id;
DROP INDEX IF EXISTS idx_reviews_created_at;

-- Create new indexes with TEXT type
CREATE INDEX idx_reviews_user_id ON public.reviews(user_id);
CREATE INDEX idx_reviews_dish_id ON public.reviews(dish_id);
CREATE INDEX idx_reviews_restaurant_id ON public.reviews(restaurant_id);
CREATE INDEX idx_reviews_created_at ON public.reviews(created_at DESC);

-- ==================== STEP 4: RE-ENABLE RLS ====================
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;

-- ==================== STEP 5: VERIFICATION ====================
DO $$
DECLARE
    error_count INTEGER := 0;
BEGIN
    -- Check reviews table columns
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'reviews' AND column_name = 'id' AND data_type = 'text'
    ) THEN
        RAISE WARNING '✗ reviews.id is not TEXT';
        error_count := error_count + 1;
    ELSE
        RAISE NOTICE '✓ reviews.id is TEXT';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'reviews' AND column_name = 'user_id' AND data_type = 'text'
    ) THEN
        RAISE WARNING '✗ reviews.user_id is not TEXT';
        error_count := error_count + 1;
    ELSE
        RAISE NOTICE '✓ reviews.user_id is TEXT';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'reviews' AND column_name = 'dish_id' AND data_type = 'text'
    ) THEN
        RAISE WARNING '✗ reviews.dish_id is not TEXT';
        error_count := error_count + 1;
    ELSE
        RAISE NOTICE '✓ reviews.dish_id is TEXT';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'reviews' AND column_name = 'restaurant_id' AND data_type = 'text'
    ) THEN
        RAISE WARNING '✗ reviews.restaurant_id is not TEXT';
        error_count := error_count + 1;
    ELSE
        RAISE NOTICE '✓ reviews.restaurant_id is TEXT';
    END IF;

    IF error_count = 0 THEN
        RAISE NOTICE '✓✓✓ REVIEWS TABLE MIGRATION SUCCESSFUL! ✓✓✓';
    ELSE
        RAISE WARNING '✗✗✗ Migration completed with % errors ✗✗✗', error_count;
    END IF;
END $$;

COMMIT;

-- ==================== COMPLETE ====================
-- Reviews table migration complete!
-- All UUID columns have been converted to TEXT.
