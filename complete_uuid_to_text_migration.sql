-- =====================================================
-- COMPLETE UUID TO TEXT MIGRATION
-- Converts ALL UUID columns to TEXT across all tables
-- =====================================================

BEGIN;

-- Step 1: Drop all foreign key constraints
-- =====================================================
ALTER TABLE ratings DROP CONSTRAINT IF EXISTS ratings_user_id_fkey;
ALTER TABLE ratings DROP CONSTRAINT IF EXISTS ratings_dish_id_fkey;
ALTER TABLE ratings DROP CONSTRAINT IF EXISTS ratings_restaurant_id_fkey;
ALTER TABLE dishes DROP CONSTRAINT IF EXISTS dishes_restaurant_id_fkey;

-- Step 2: Convert USERS table
-- =====================================================
-- Drop any default UUID generation
ALTER TABLE users ALTER COLUMN id DROP DEFAULT;

-- Convert id column to TEXT
ALTER TABLE users ALTER COLUMN id TYPE TEXT USING id::TEXT;

-- Step 3: Convert RESTAURANTS table
-- =====================================================
-- Drop any default UUID generation
ALTER TABLE restaurants ALTER COLUMN id DROP DEFAULT;

-- Convert id column to TEXT
ALTER TABLE restaurants ALTER COLUMN id TYPE TEXT USING id::TEXT;

-- Step 4: Convert DISHES table
-- =====================================================
-- Drop any default UUID generation
ALTER TABLE dishes ALTER COLUMN id DROP DEFAULT;

-- Convert id and restaurant_id columns to TEXT
ALTER TABLE dishes ALTER COLUMN id TYPE TEXT USING id::TEXT;
ALTER TABLE dishes ALTER COLUMN restaurant_id TYPE TEXT USING restaurant_id::TEXT;

-- Step 5: Convert RATINGS table
-- =====================================================
-- Drop any default UUID generation
ALTER TABLE ratings ALTER COLUMN id DROP DEFAULT;

-- Convert all UUID columns to TEXT
ALTER TABLE ratings ALTER COLUMN id TYPE TEXT USING id::TEXT;
ALTER TABLE ratings ALTER COLUMN user_id TYPE TEXT USING user_id::TEXT;
ALTER TABLE ratings ALTER COLUMN dish_id TYPE TEXT USING dish_id::TEXT;
ALTER TABLE ratings ALTER COLUMN restaurant_id TYPE TEXT USING restaurant_id::TEXT;

-- Step 6: Recreate foreign key constraints
-- =====================================================
ALTER TABLE ratings
    ADD CONSTRAINT ratings_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE ratings
    ADD CONSTRAINT ratings_dish_id_fkey
    FOREIGN KEY (dish_id) REFERENCES dishes(id) ON DELETE CASCADE;

ALTER TABLE ratings
    ADD CONSTRAINT ratings_restaurant_id_fkey
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE;

ALTER TABLE dishes
    ADD CONSTRAINT dishes_restaurant_id_fkey
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE;

-- Step 7: Create indexes for performance
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_ratings_user_id ON ratings(user_id);
CREATE INDEX IF NOT EXISTS idx_ratings_dish_id ON ratings(dish_id);
CREATE INDEX IF NOT EXISTS idx_ratings_restaurant_id ON ratings(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_dishes_restaurant_id ON dishes(restaurant_id);

-- Step 8: Verify the migration
-- =====================================================
DO $$
DECLARE
    uuid_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO uuid_count
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name IN ('users', 'restaurants', 'dishes', 'ratings')
      AND data_type = 'uuid';

    IF uuid_count > 0 THEN
        RAISE EXCEPTION 'Migration failed: % UUID columns still exist', uuid_count;
    ELSE
        RAISE NOTICE 'Migration successful: All UUID columns converted to TEXT';
    END IF;
END $$;

COMMIT;

-- Final verification query
SELECT
    table_name,
    column_name,
    data_type
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('users', 'restaurants', 'dishes', 'ratings')
  AND column_name IN ('id', 'user_id', 'restaurant_id', 'dish_id')
ORDER BY table_name, column_name;
