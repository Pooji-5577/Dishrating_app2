-- Stories Table - Simple photo sharing for followers
-- Stories expire after 24 hours
-- NOTE: profiles.id is TEXT (Firebase migration), not UUID

CREATE TABLE IF NOT EXISTS public.stories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ DEFAULT (NOW() + INTERVAL '24 hours')
);

CREATE INDEX IF NOT EXISTS idx_stories_user_id ON public.stories(user_id);
CREATE INDEX IF NOT EXISTS idx_stories_created_at ON public.stories(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_stories_expires_at ON public.stories(expires_at);

ALTER TABLE public.stories ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "stories_select" ON public.stories;
DROP POLICY IF EXISTS "stories_insert" ON public.stories;
DROP POLICY IF EXISTS "stories_delete" ON public.stories;

CREATE POLICY "stories_select" ON public.stories
    FOR SELECT USING (
        user_id = auth.uid()::text
        OR user_id IN (
            SELECT following_id
            FROM public.followers
            WHERE follower_id = auth.uid()::text
        )
    );

CREATE POLICY "stories_insert" ON public.stories
    FOR INSERT WITH CHECK (auth.uid()::text = user_id);

CREATE POLICY "stories_delete" ON public.stories
    FOR DELETE USING (auth.uid()::text = user_id);

CREATE OR REPLACE FUNCTION public.cleanup_expired_stories()
RETURNS void AS $$
BEGIN
    DELETE FROM public.stories WHERE expires_at < NOW();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        EXECUTE 'ALTER PUBLICATION supabase_realtime ADD TABLE public.stories';
    END IF;
EXCEPTION
    WHEN duplicate_object THEN
        NULL;
END $$;

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'story-images',
    'story-images',
    true,
    10485760,
    ARRAY['image/jpeg', 'image/png', 'image/webp']
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    public = EXCLUDED.public,
    file_size_limit = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

DROP POLICY IF EXISTS "public read story-images" ON storage.objects;
DROP POLICY IF EXISTS "upload own to story-images" ON storage.objects;
DROP POLICY IF EXISTS "update own story-images" ON storage.objects;
DROP POLICY IF EXISTS "delete own story-images" ON storage.objects;

CREATE POLICY "public read story-images"
ON storage.objects
FOR SELECT
TO public
USING (bucket_id = 'story-images');

CREATE POLICY "upload own to story-images"
ON storage.objects
FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'story-images'
    AND (storage.foldername(name))[1] = (SELECT auth.uid())::text
);

CREATE POLICY "update own story-images"
ON storage.objects
FOR UPDATE
TO authenticated
USING (
    bucket_id = 'story-images'
    AND (storage.foldername(name))[1] = (SELECT auth.uid())::text
)
WITH CHECK (
    bucket_id = 'story-images'
    AND (storage.foldername(name))[1] = (SELECT auth.uid())::text
);

CREATE POLICY "delete own story-images"
ON storage.objects
FOR DELETE
TO authenticated
USING (
    bucket_id = 'story-images'
    AND (storage.foldername(name))[1] = (SELECT auth.uid())::text
);
