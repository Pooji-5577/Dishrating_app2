-- FIX DEFAULT ID GENERATORS
-- Replace gen_random_uuid() with text-based ID generation
-- This fixes the "invalid input syntax for type uuid" error

BEGIN;

-- ==================== CREATE TEXT ID GENERATOR FUNCTION ====================
-- This function generates random text IDs similar to Firebase/Firestore style
CREATE OR REPLACE FUNCTION generate_text_id(prefix TEXT DEFAULT '')
RETURNS TEXT AS $$
DECLARE
    chars TEXT := 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    result TEXT := prefix;
    i INTEGER;
BEGIN
    -- Generate 20 random characters
    FOR i IN 1..20 LOOP
        result := result || substr(chars, floor(random() * length(chars) + 1)::int, 1);
    END LOOP;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- ==================== UPDATE DEFAULT VALUES ====================

-- Profiles table (user IDs are set by Firebase, no default needed)
ALTER TABLE public.profiles ALTER COLUMN id DROP DEFAULT;

-- Restaurants table (can be Google Place IDs or generated)
ALTER TABLE public.restaurants ALTER COLUMN id SET DEFAULT generate_text_id('rst_');

-- Dishes table
ALTER TABLE public.dishes ALTER COLUMN id SET DEFAULT generate_text_id('dish_');

-- Ratings table
ALTER TABLE public.ratings ALTER COLUMN id SET DEFAULT generate_text_id('rating_');

-- Badges table
ALTER TABLE public.badges ALTER COLUMN id SET DEFAULT generate_text_id('badge_');

-- User Badges table
ALTER TABLE public.user_badges ALTER COLUMN id SET DEFAULT generate_text_id('ubadge_');

-- Likes table
ALTER TABLE public.likes ALTER COLUMN id SET DEFAULT generate_text_id('like_');

-- Reviews table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'reviews') THEN
        EXECUTE 'ALTER TABLE public.reviews ALTER COLUMN id SET DEFAULT generate_text_id(''review_'')';
        RAISE NOTICE '✓ Reviews table default updated';
    END IF;
END $$;

-- ==================== VERIFICATION ====================
DO $$
BEGIN
    RAISE NOTICE '✓ Text ID generator function created';
    RAISE NOTICE '✓ Default ID generators updated for all tables';
    RAISE NOTICE '';
    RAISE NOTICE 'Testing ID generation...';
    RAISE NOTICE 'Sample dish ID: %', generate_text_id('dish_');
    RAISE NOTICE 'Sample rating ID: %', generate_text_id('rating_');
    RAISE NOTICE 'Sample restaurant ID: %', generate_text_id('rst_');
    RAISE NOTICE '';
    RAISE NOTICE '✓✓✓ ALL DEFAULT GENERATORS FIXED! ✓✓✓';
END $$;

COMMIT;

-- ==================== COMPLETE ====================
-- Migration complete!
-- Database will now generate TEXT-format IDs instead of UUIDs
-- Example IDs:
-- - dish_a7K9xP2mN4vQ8wR3sT
-- - rating_B5nM8xQ2vK9wP7cR
-- - rst_C3pL6yT9mN2kQ8vX
