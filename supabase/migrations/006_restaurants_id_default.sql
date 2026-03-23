-- Add DEFAULT gen_random_uuid() to restaurants.id column
-- This ensures inserts without an explicit id auto-generate a valid UUID
-- gen_random_uuid() is built-in on PostgreSQL 13+ (Supabase uses 15+)
ALTER TABLE public.restaurants ALTER COLUMN id SET DEFAULT gen_random_uuid();
