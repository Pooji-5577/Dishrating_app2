-- Add Google Places photo caching columns to restaurants table
-- google_place_id: stores the Google Places place ID for fast photo lookups
-- photo_urls: stores cached photo URLs so we don't call the Edge Function repeatedly

ALTER TABLE public.restaurants
    ADD COLUMN IF NOT EXISTS google_place_id TEXT,
    ADD COLUMN IF NOT EXISTS photo_urls TEXT[] DEFAULT '{}';

-- Index for faster lookups by place ID
CREATE INDEX IF NOT EXISTS idx_restaurants_google_place_id
    ON public.restaurants (google_place_id)
    WHERE google_place_id IS NOT NULL;
