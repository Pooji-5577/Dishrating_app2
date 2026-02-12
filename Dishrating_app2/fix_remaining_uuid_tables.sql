-- FIX REMAINING UUID TABLES
-- Fixes challenges, review_likes, and user_challenges tables

BEGIN;

-- ==================== STEP 1: DROP CONSTRAINTS ====================
ALTER TABLE public.challenges DROP CONSTRAINT IF EXISTS challenges_pkey CASCADE;
ALTER TABLE public.review_likes DROP CONSTRAINT IF EXISTS review_likes_pkey CASCADE;
ALTER TABLE public.review_likes DROP CONSTRAINT IF EXISTS review_likes_user_id_fkey;
ALTER TABLE public.review_likes DROP CONSTRAINT IF EXISTS review_likes_review_id_fkey;
ALTER TABLE public.user_challenges DROP CONSTRAINT IF EXISTS user_challenges_pkey CASCADE;
ALTER TABLE public.user_challenges DROP CONSTRAINT IF EXISTS user_challenges_user_id_fkey;
ALTER TABLE public.user_challenges DROP CONSTRAINT IF EXISTS user_challenges_challenge_id_fkey;

-- ==================== STEP 2: CONVERT COLUMNS TO TEXT ====================

-- Challenges table
ALTER TABLE public.challenges
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE TEXT USING CAST(id AS TEXT);
ALTER TABLE public.challenges ADD PRIMARY KEY (id);
ALTER TABLE public.challenges ALTER COLUMN id SET DEFAULT generate_text_id('challenge_');

-- Review_likes table
ALTER TABLE public.review_likes
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE TEXT USING CAST(id AS TEXT);

-- Check if user_id and review_id exist and convert them
DO $$
BEGIN
    -- Convert user_id if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'review_likes' AND column_name = 'user_id'
    ) THEN
        EXECUTE 'ALTER TABLE public.review_likes ALTER COLUMN user_id TYPE TEXT USING CAST(user_id AS TEXT)';
    END IF;

    -- Convert review_id if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'review_likes' AND column_name = 'review_id'
    ) THEN
        EXECUTE 'ALTER TABLE public.review_likes ALTER COLUMN review_id TYPE TEXT USING CAST(review_id AS TEXT)';
    END IF;
END $$;

ALTER TABLE public.review_likes ADD PRIMARY KEY (id);
ALTER TABLE public.review_likes ALTER COLUMN id SET DEFAULT generate_text_id('revlike_');

-- User_challenges table
ALTER TABLE public.user_challenges
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE TEXT USING CAST(id AS TEXT);

-- Check if user_id and challenge_id exist and convert them
DO $$
BEGIN
    -- Convert user_id if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_challenges' AND column_name = 'user_id'
    ) THEN
        EXECUTE 'ALTER TABLE public.user_challenges ALTER COLUMN user_id TYPE TEXT USING CAST(user_id AS TEXT)';
    END IF;

    -- Convert challenge_id if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_challenges' AND column_name = 'challenge_id'
    ) THEN
        EXECUTE 'ALTER TABLE public.user_challenges ALTER COLUMN challenge_id TYPE TEXT USING CAST(challenge_id AS TEXT)';
    END IF;
END $$;

ALTER TABLE public.user_challenges ADD PRIMARY KEY (id);
ALTER TABLE public.user_challenges ALTER COLUMN id SET DEFAULT generate_text_id('userchall_');

-- ==================== STEP 3: RECREATE INDEXES ====================
DROP INDEX IF EXISTS idx_review_likes_user_id;
DROP INDEX IF EXISTS idx_review_likes_review_id;
DROP INDEX IF EXISTS idx_user_challenges_user_id;
DROP INDEX IF EXISTS idx_user_challenges_challenge_id;

CREATE INDEX idx_review_likes_user_id ON public.review_likes(user_id);
CREATE INDEX idx_review_likes_review_id ON public.review_likes(review_id);
CREATE INDEX idx_user_challenges_user_id ON public.user_challenges(user_id);
CREATE INDEX idx_user_challenges_challenge_id ON public.user_challenges(challenge_id);

-- ==================== STEP 4: ENABLE RLS ====================
ALTER TABLE public.challenges ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.review_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_challenges ENABLE ROW LEVEL SECURITY;

-- ==================== STEP 5: VERIFICATION ====================
DO $$
DECLARE
    error_count INTEGER := 0;
BEGIN
    RAISE NOTICE '==================== VERIFICATION ====================';

    -- Check challenges.id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'challenges' AND column_name = 'id' AND data_type = 'text'
    ) THEN
        RAISE WARNING '✗ challenges.id is not TEXT';
        error_count := error_count + 1;
    ELSE
        RAISE NOTICE '✓ challenges.id is TEXT';
    END IF;

    -- Check review_likes.id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'review_likes' AND column_name = 'id' AND data_type = 'text'
    ) THEN
        RAISE WARNING '✗ review_likes.id is not TEXT';
        error_count := error_count + 1;
    ELSE
        RAISE NOTICE '✓ review_likes.id is TEXT';
    END IF;

    -- Check user_challenges.id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_challenges' AND column_name = 'id' AND data_type = 'text'
    ) THEN
        RAISE WARNING '✗ user_challenges.id is not TEXT';
        error_count := error_count + 1;
    ELSE
        RAISE NOTICE '✓ user_challenges.id is TEXT';
    END IF;

    RAISE NOTICE '';
    IF error_count = 0 THEN
        RAISE NOTICE '✓✓✓ ALL REMAINING TABLES FIXED! ✓✓✓';
        RAISE NOTICE 'All UUID tables have been converted to TEXT';
    ELSE
        RAISE WARNING '✗✗✗ Conversion completed with % errors ✗✗✗', error_count;
    END IF;
END $$;

COMMIT;

-- ==================== COMPLETE ====================
-- All remaining UUID tables have been converted to TEXT!
