CREATE OR REPLACE FUNCTION public.cleanup_expired_stories()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    DELETE FROM public.stories WHERE expires_at < NOW();
END;
$$;

REVOKE ALL ON FUNCTION public.cleanup_expired_stories() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.cleanup_expired_stories() FROM anon;
REVOKE ALL ON FUNCTION public.cleanup_expired_stories() FROM authenticated;
