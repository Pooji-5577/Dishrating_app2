-- Add price column to ratings table
-- Stores the price the user paid for the dish at the time of rating
ALTER TABLE public.ratings ADD COLUMN IF NOT EXISTS price DECIMAL(10,2);
