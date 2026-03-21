-- ═══════════════════════════════════════════════════════════════════════════════
-- SmackCheck Social Map Schema Extension
-- Run this SQL in your Supabase SQL Editor AFTER running supabase_complete_schema.sql
-- Adds support for Snapchat-style map with user locations and recent dish posts
-- ═══════════════════════════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 1: ENABLE PostGIS EXTENSION (Required for location queries)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE EXTENSION IF NOT EXISTS postgis;

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 2: ADD LOCATION COLUMNS TO PROFILES
-- ═══════════════════════════════════════════════════════════════════════════════

-- Add location columns to profiles for user tracking on map
ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
  ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION,
  ADD COLUMN IF NOT EXISTS location_updated_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS location_sharing_enabled BOOLEAN DEFAULT TRUE;

-- Create spatial index for fast location queries
CREATE INDEX IF NOT EXISTS idx_profiles_location 
  ON public.profiles(latitude, longitude) 
  WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Index for location update time (for filtering recent users)
CREATE INDEX IF NOT EXISTS idx_profiles_location_updated 
  ON public.profiles(location_updated_at DESC) 
  WHERE location_updated_at IS NOT NULL;

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 3: ADD LOCATION COLUMNS TO RATINGS (for dish posts with location)
-- ═══════════════════════════════════════════════════════════════════════════════

ALTER TABLE public.ratings
  ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
  ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- Spatial index for ratings location
CREATE INDEX IF NOT EXISTS idx_ratings_location 
  ON public.ratings(latitude, longitude) 
  WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 4: FUNCTION TO GET NEARBY USERS WITH RECENT DISH POSTS
-- ═══════════════════════════════════════════════════════════════════════════════

-- Drop existing functions first to allow return type changes
DROP FUNCTION IF EXISTS public.get_nearby_users_with_dishes(DOUBLE PRECISION, DOUBLE PRECISION, INTEGER, INTEGER);
DROP FUNCTION IF EXISTS public.get_nearby_dish_posts_postgis(DOUBLE PRECISION, DOUBLE PRECISION, INTEGER, INTEGER);
DROP FUNCTION IF EXISTS public.update_user_location(DOUBLE PRECISION, DOUBLE PRECISION);
DROP FUNCTION IF EXISTS public.toggle_location_sharing(BOOLEAN);
DROP FUNCTION IF EXISTS public.get_current_user_map_profile();

-- Get nearby users who have posted dishes recently (within specified radius in meters)
CREATE OR REPLACE FUNCTION public.get_nearby_users_with_dishes(
  p_user_lat DOUBLE PRECISION,
  p_user_lng DOUBLE PRECISION,
  p_radius_meters INTEGER DEFAULT 3000,  -- Default 3km radius
  p_hours_ago INTEGER DEFAULT 168        -- Default 7 days (168 hours)
)
RETURNS TABLE (
  user_id TEXT,
  username TEXT,
  avatar_url TEXT,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  distance_meters DOUBLE PRECISION,
  latest_rating_id TEXT,
  latest_dish_id TEXT,
  latest_dish_name TEXT,
  latest_dish_image TEXT,
  latest_rating REAL,
  latest_restaurant_id TEXT,
  latest_restaurant_name TEXT,
  latest_post_time TIMESTAMPTZ
) AS $$
BEGIN
  RETURN QUERY
  WITH latest_ratings AS (
    -- Get the most recent rating per user within time window
    SELECT DISTINCT ON (r.user_id)
      r.user_id,
      r.id AS rating_id,
      r.dish_id,
      r.rating,
      r.image_url AS rating_image_url,
      r.restaurant_id,
      r.created_at,
      r.latitude AS rating_lat,
      r.longitude AS rating_lng
    FROM public.ratings r
    WHERE r.created_at >= NOW() - (p_hours_ago || ' hours')::INTERVAL
    ORDER BY r.user_id, r.created_at DESC
  )
  SELECT 
    p.id AS user_id,
    p.name AS username,
    p.profile_photo_url AS avatar_url,
    COALESCE(lr.rating_lat, p.latitude) AS latitude,
    COALESCE(lr.rating_lng, p.longitude) AS longitude,
    -- Calculate distance using Haversine formula (approximation)
    (
      6371000 * acos(
        cos(radians(p_user_lat)) * cos(radians(COALESCE(lr.rating_lat, p.latitude))) *
        cos(radians(COALESCE(lr.rating_lng, p.longitude)) - radians(p_user_lng)) +
        sin(radians(p_user_lat)) * sin(radians(COALESCE(lr.rating_lat, p.latitude)))
      )
    ) AS distance_meters,
    lr.rating_id AS latest_rating_id,
    lr.dish_id AS latest_dish_id,
    d.name AS latest_dish_name,
    COALESCE(lr.rating_image_url, d.image_url) AS latest_dish_image,
    lr.rating::REAL AS latest_rating,
    lr.restaurant_id AS latest_restaurant_id,
    rest.name AS latest_restaurant_name,
    lr.created_at AS latest_post_time
  FROM public.profiles p
  INNER JOIN latest_ratings lr ON p.id = lr.user_id
  INNER JOIN public.dishes d ON lr.dish_id = d.id
  INNER JOIN public.restaurants rest ON lr.restaurant_id = rest.id
  WHERE 
    -- User has location sharing enabled
    p.location_sharing_enabled = TRUE
    -- Either rating has location or profile has location
    AND (lr.rating_lat IS NOT NULL OR p.latitude IS NOT NULL)
    AND (lr.rating_lng IS NOT NULL OR p.longitude IS NOT NULL)
    -- Filter by radius
    AND (
      6371000 * acos(
        cos(radians(p_user_lat)) * cos(radians(COALESCE(lr.rating_lat, p.latitude))) *
        cos(radians(COALESCE(lr.rating_lng, p.longitude)) - radians(p_user_lng)) +
        sin(radians(p_user_lat)) * sin(radians(COALESCE(lr.rating_lat, p.latitude)))
      )
    ) <= p_radius_meters
  ORDER BY lr.created_at DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 5: FUNCTION TO GET NEARBY DISH POSTS (Alternative for PostGIS)
-- ═══════════════════════════════════════════════════════════════════════════════

-- If PostGIS is enabled, this version uses ST_DWithin for better performance
CREATE OR REPLACE FUNCTION public.get_nearby_dish_posts_postgis(
  p_user_lat DOUBLE PRECISION,
  p_user_lng DOUBLE PRECISION,
  p_radius_meters INTEGER DEFAULT 3000,
  p_limit INTEGER DEFAULT 50
)
RETURNS TABLE (
  user_id TEXT,
  username TEXT,
  avatar_url TEXT,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  rating_id TEXT,
  dish_id TEXT,
  dish_name TEXT,
  dish_image TEXT,
  rating REAL,
  restaurant_id TEXT,
  restaurant_name TEXT,
  posted_at TIMESTAMPTZ
) AS $$
BEGIN
  RETURN QUERY
  SELECT 
    p.id AS user_id,
    p.name AS username,
    p.profile_photo_url AS avatar_url,
    COALESCE(r.latitude, p.latitude) AS latitude,
    COALESCE(r.longitude, p.longitude) AS longitude,
    r.id AS rating_id,
    r.dish_id,
    d.name AS dish_name,
    COALESCE(r.image_url, d.image_url) AS dish_image,
    r.rating::REAL AS rating,
    r.restaurant_id,
    rest.name AS restaurant_name,
    r.created_at AS posted_at
  FROM public.ratings r
  INNER JOIN public.profiles p ON r.user_id = p.id
  INNER JOIN public.dishes d ON r.dish_id = d.id
  INNER JOIN public.restaurants rest ON r.restaurant_id = rest.id
  WHERE 
    p.location_sharing_enabled = TRUE
    AND (r.latitude IS NOT NULL OR p.latitude IS NOT NULL)
    AND (r.longitude IS NOT NULL OR p.longitude IS NOT NULL)
    AND ST_DWithin(
      ST_MakePoint(COALESCE(r.longitude, p.longitude), COALESCE(r.latitude, p.latitude))::geography,
      ST_MakePoint(p_user_lng, p_user_lat)::geography,
      p_radius_meters
    )
  ORDER BY r.created_at DESC
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 6: FUNCTION TO UPDATE USER LOCATION
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION public.update_user_location(
  p_latitude DOUBLE PRECISION,
  p_longitude DOUBLE PRECISION
)
RETURNS VOID AS $$
BEGIN
  UPDATE public.profiles
  SET 
    latitude = p_latitude,
    longitude = p_longitude,
    location_updated_at = NOW()
  WHERE id = auth.uid()::TEXT;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 7: FUNCTION TO TOGGLE LOCATION SHARING
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION public.toggle_location_sharing(
  p_enabled BOOLEAN
)
RETURNS VOID AS $$
BEGIN
  UPDATE public.profiles
  SET location_sharing_enabled = p_enabled
  WHERE id = auth.uid()::TEXT;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 8: GET CURRENT USER MAP PROFILE (for displaying self on map)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION public.get_current_user_map_profile()
RETURNS TABLE (
  user_id TEXT,
  username TEXT,
  avatar_url TEXT,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  location_sharing_enabled BOOLEAN,
  total_ratings BIGINT,
  latest_rating_id TEXT,
  latest_dish_name TEXT,
  latest_dish_image TEXT
) AS $$
BEGIN
  RETURN QUERY
  SELECT 
    p.id AS user_id,
    p.name AS username,
    p.profile_photo_url AS avatar_url,
    p.latitude,
    p.longitude,
    p.location_sharing_enabled,
    (SELECT COUNT(*) FROM public.ratings r WHERE r.user_id = p.id) AS total_ratings,
    (SELECT r.id FROM public.ratings r WHERE r.user_id = p.id ORDER BY r.created_at DESC LIMIT 1) AS latest_rating_id,
    (SELECT d.name FROM public.ratings r JOIN public.dishes d ON r.dish_id = d.id WHERE r.user_id = p.id ORDER BY r.created_at DESC LIMIT 1) AS latest_dish_name,
    (SELECT COALESCE(r.image_url, d.image_url) FROM public.ratings r JOIN public.dishes d ON r.dish_id = d.id WHERE r.user_id = p.id ORDER BY r.created_at DESC LIMIT 1) AS latest_dish_image
  FROM public.profiles p
  WHERE p.id = auth.uid()::TEXT;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 9: FUNCTION TO GET ALL DISH POSTS FOR WORLD MAP
-- Returns every post placed at its restaurant's coordinates (world map view)
-- ═══════════════════════════════════════════════════════════════════════════════

DROP FUNCTION IF EXISTS public.get_all_dish_posts(INTEGER);

CREATE OR REPLACE FUNCTION public.get_all_dish_posts(
  p_limit INTEGER DEFAULT 500
)
RETURNS TABLE (
  user_id TEXT,
  username TEXT,
  avatar_url TEXT,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  rating_id TEXT,
  dish_id TEXT,
  dish_name TEXT,
  dish_image TEXT,
  rating REAL,
  restaurant_id TEXT,
  restaurant_name TEXT,
  posted_at TIMESTAMPTZ
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    p.id AS user_id,
    p.name AS username,
    p.profile_photo_url AS avatar_url,
    -- Prefer restaurant location, fall back to where the rating was captured, then user profile
    COALESCE(rest.latitude, r.latitude, p.latitude) AS latitude,
    COALESCE(rest.longitude, r.longitude, p.longitude) AS longitude,
    r.id AS rating_id,
    r.dish_id,
    d.name AS dish_name,
    COALESCE(r.image_url, d.image_url) AS dish_image,
    r.rating::REAL AS rating,
    r.restaurant_id,
    rest.name AS restaurant_name,
    r.created_at AS posted_at
  FROM public.ratings r
  INNER JOIN public.profiles p ON r.user_id = p.id
  INNER JOIN public.dishes d ON r.dish_id = d.id
  INNER JOIN public.restaurants rest ON r.restaurant_id = rest.id
  WHERE
    -- Only include posts that have resolvable coordinates
    COALESCE(rest.latitude, r.latitude, p.latitude) IS NOT NULL
    AND COALESCE(rest.longitude, r.longitude, p.longitude) IS NOT NULL
  ORDER BY r.created_at DESC
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════════════════════════
-- VERIFICATION
-- ═══════════════════════════════════════════════════════════════════════════════

DO $$
BEGIN
  -- Check if PostGIS is enabled
  IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'postgis') THEN
    RAISE NOTICE 'SUCCESS: PostGIS extension is enabled. Use get_nearby_dish_posts_postgis for best performance.';
  ELSE
    RAISE NOTICE 'WARNING: PostGIS not enabled. Use get_nearby_users_with_dishes (Haversine formula).';
  END IF;
  
  -- Check if location columns exist
  IF EXISTS (
    SELECT 1 FROM information_schema.columns 
    WHERE table_name = 'profiles' AND column_name = 'latitude'
  ) THEN
    RAISE NOTICE 'SUCCESS: Location columns added to profiles table.';
  END IF;
  
  RAISE NOTICE 'Social Map schema is ready!';
END $$;
