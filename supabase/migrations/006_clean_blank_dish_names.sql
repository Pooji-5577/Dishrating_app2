-- Migration: 006_clean_blank_dish_names.sql
-- Purpose : Remove ratings and dishes where the dish name is NULL or blank.
--           Safe to re-run — all DELETEs are guarded by WHERE conditions.
-- Run via : Supabase SQL editor or psql

BEGIN;

-- ── STEP 1: Validate before deleting ────────────────────────────────────────
DO $$
DECLARE
  v_null_dish_ratings   BIGINT;
  v_blank_dish_ratings  BIGINT;
  v_blank_dishes        BIGINT;
  v_dangling_ratings    BIGINT;
BEGIN
  SELECT COUNT(*) INTO v_null_dish_ratings   FROM ratings WHERE dish_id IS NULL;
  SELECT COUNT(*) INTO v_blank_dish_ratings  FROM ratings r
    JOIN dishes d ON d.id::text = r.dish_id::text WHERE TRIM(d.name) = '';
  SELECT COUNT(*) INTO v_blank_dishes        FROM dishes WHERE TRIM(name) = '';
  SELECT COUNT(*) INTO v_dangling_ratings    FROM ratings r
    LEFT JOIN dishes d ON d.id::text = r.dish_id::text
    WHERE r.dish_id IS NOT NULL AND d.id IS NULL;

  RAISE NOTICE '=== Validation Report ===';
  RAISE NOTICE 'Ratings with NULL dish_id        : %', v_null_dish_ratings;
  RAISE NOTICE 'Ratings linked to blank dish name: %', v_blank_dish_ratings;
  RAISE NOTICE 'Dishes with blank/empty name     : %', v_blank_dishes;
  RAISE NOTICE 'Dangling ratings (missing dish)  : %', v_dangling_ratings;
  RAISE NOTICE 'Total to delete: %',
    v_null_dish_ratings + v_blank_dish_ratings + v_blank_dishes + v_dangling_ratings;
END $$;

-- ── STEP 2: Delete ratings with NULL dish_id ────────────────────────────────
DELETE FROM ratings
WHERE dish_id IS NULL;

-- ── STEP 3: Delete ratings whose dish has a blank/whitespace-only name ──────
DELETE FROM ratings
WHERE dish_id IN (
  SELECT id FROM dishes WHERE TRIM(name) = ''
);

-- ── STEP 4: Delete dangling ratings (dish_id set but dish row missing) ──────
DELETE FROM ratings
WHERE dish_id IS NOT NULL
  AND dish_id::text NOT IN (SELECT id::text FROM dishes);

-- ── STEP 5: Delete dishes with blank/whitespace-only names ──────────────────
DELETE FROM dishes
WHERE TRIM(name) = '';

COMMIT;
