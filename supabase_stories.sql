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

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_stories_user_id ON public.stories(user_id);
CREATE INDEX IF NOT EXISTS idx_stories_created_at ON public.stories(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_stories_expires_at ON public.stories(expires_at);

-- Enable RLS
ALTER TABLE public.stories ENABLE ROW LEVEL SECURITY;

-- Policies for stories
DROP POLICY IF EXISTS "stories_select" ON public.stories;
DROP POLICY IF EXISTS "stories_insert" ON public.stories;
DROP POLICY IF EXISTS "stories_delete" ON public.stories;

-- Anyone can view stories from users they follow (or their own)
CREATE POLICY "stories_select" ON public.stories 
    FOR SELECT USING (
        user_id = auth.uid()::text
        OR user_id IN (SELECT following_id FROM public.followers WHERE follower_id = auth.uid()::text)
    );

-- Users can only create their own stories
CREATE POLICY "stories_insert" ON public.stories 
    FOR INSERT WITH CHECK (auth.uid()::text = user_id);

-- Users can only delete their own stories
CREATE POLICY "stories_delete" ON public.stories 
    FOR DELETE USING (auth.uid()::text = user_id);

-- Function to auto-delete expired stories
CREATE OR REPLACE FUNCTION public.cleanup_expired_stories()
RETURNS void AS $$
BEGIN
    DELETE FROM public.stories WHERE expires_at < NOW();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Enable realtime for stories
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        EXECUTE 'ALTER PUBLICATION supabase_realtime ADD TABLE public.stories';
    END IF;
EXCEPTION
    WHEN duplicate_object THEN
        NULL;
END $$;
