-- Fix: Drop the admin_profiles_select_all policy that causes infinite recursion.
-- This policy queries the profiles table from within a profiles RLS policy,
-- creating a circular dependency. It is also redundant because the existing
-- profiles_select policy already allows all SELECTs with USING (true).
DROP POLICY IF EXISTS admin_profiles_select_all ON public.profiles;
