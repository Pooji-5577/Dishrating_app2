-- Clear stale cached photo URLs so the app re-fetches fresh ones
-- from Google Places via the edge function.
UPDATE public.restaurants
SET photo_urls = NULL,
    image_urls = NULL
WHERE photo_urls IS NOT NULL OR image_urls IS NOT NULL;
