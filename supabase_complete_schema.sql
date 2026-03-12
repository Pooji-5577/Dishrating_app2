-- ═══════════════════════════════════════════════════════════════════════════════
-- SmackCheck Complete Database Schema
-- Run this SQL in your Supabase SQL Editor to create ALL required tables
-- This includes: followers, comments, rating_images, notifications, realtime
-- ═══════════════════════════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 1: ADD MISSING COLUMNS TO PROFILES
-- ═══════════════════════════════════════════════════════════════════════════════

-- Add social columns to profiles
ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS bio TEXT,
  ADD COLUMN IF NOT EXISTS followers_count INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS following_count INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS last_location TEXT,
  ADD COLUMN IF NOT EXISTS fcm_token TEXT,  -- For Firebase Cloud Messaging
  ADD COLUMN IF NOT EXISTS apns_token TEXT; -- For Apple Push Notification Service

-- Index for push token lookups
CREATE INDEX IF NOT EXISTS idx_profiles_fcm_token ON public.profiles(id) WHERE fcm_token IS NOT NULL;

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 2: FOLLOWERS TABLE (Social Feature)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.followers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(follower_id, following_id),
    CHECK (follower_id != following_id)  -- Prevent self-following
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_followers_follower_id ON public.followers(follower_id);
CREATE INDEX IF NOT EXISTS idx_followers_following_id ON public.followers(following_id);
CREATE INDEX IF NOT EXISTS idx_followers_created_at ON public.followers(created_at DESC);

-- Enable RLS
ALTER TABLE public.followers ENABLE ROW LEVEL SECURITY;

-- Policies for followers
DROP POLICY IF EXISTS "followers_select" ON public.followers;
DROP POLICY IF EXISTS "followers_insert" ON public.followers;
DROP POLICY IF EXISTS "followers_delete" ON public.followers;

CREATE POLICY "followers_select" ON public.followers 
    FOR SELECT USING (true);

CREATE POLICY "followers_insert" ON public.followers 
    FOR INSERT WITH CHECK (auth.uid() = follower_id);

CREATE POLICY "followers_delete" ON public.followers 
    FOR DELETE USING (auth.uid() = follower_id);

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 3: COMMENTS TABLE (Nested Comments on Ratings)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rating_id UUID NOT NULL REFERENCES public.ratings(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    parent_comment_id UUID REFERENCES public.comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_comments_rating_id ON public.comments(rating_id);
CREATE INDEX IF NOT EXISTS idx_comments_user_id ON public.comments(user_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent_id ON public.comments(parent_comment_id);
CREATE INDEX IF NOT EXISTS idx_comments_created_at ON public.comments(rating_id, created_at);

-- Enable RLS
ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;

-- Policies for comments
DROP POLICY IF EXISTS "comments_select" ON public.comments;
DROP POLICY IF EXISTS "comments_insert" ON public.comments;
DROP POLICY IF EXISTS "comments_update" ON public.comments;
DROP POLICY IF EXISTS "comments_delete" ON public.comments;

CREATE POLICY "comments_select" ON public.comments 
    FOR SELECT USING (true);

CREATE POLICY "comments_insert" ON public.comments 
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "comments_update" ON public.comments 
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "comments_delete" ON public.comments 
    FOR DELETE USING (auth.uid() = user_id);

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 4: RATING IMAGES TABLE (Multiple Photos per Rating)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.rating_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rating_id UUID NOT NULL REFERENCES public.ratings(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for efficient querying
CREATE INDEX IF NOT EXISTS idx_rating_images_rating_id ON public.rating_images(rating_id, sort_order);

-- Enable RLS
ALTER TABLE public.rating_images ENABLE ROW LEVEL SECURITY;

-- Policies for rating_images
DROP POLICY IF EXISTS "rating_images_select" ON public.rating_images;
DROP POLICY IF EXISTS "rating_images_insert" ON public.rating_images;
DROP POLICY IF EXISTS "rating_images_delete" ON public.rating_images;

CREATE POLICY "rating_images_select" ON public.rating_images 
    FOR SELECT USING (true);

CREATE POLICY "rating_images_insert" ON public.rating_images 
    FOR INSERT WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.ratings 
            WHERE id = rating_id AND user_id = auth.uid()
        )
    );

CREATE POLICY "rating_images_delete" ON public.rating_images 
    FOR DELETE USING (
        EXISTS (
            SELECT 1 FROM public.ratings 
            WHERE id = rating_id AND user_id = auth.uid()
        )
    );

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 5: NOTIFICATIONS TABLE (Push Notifications)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    type TEXT NOT NULL,  -- 'like', 'comment', 'follow', 'mention', 'achievement', 'challenge'
    title TEXT NOT NULL DEFAULT 'SmackCheck',
    body TEXT NOT NULL,
    data JSONB DEFAULT '{}',  -- For deep linking: {"screen": "DishDetail", "id": "..."}
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON public.notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON public.notifications(user_id) WHERE is_read = FALSE;

-- Enable RLS
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- Policies for notifications
DROP POLICY IF EXISTS "notifications_select" ON public.notifications;
DROP POLICY IF EXISTS "notifications_insert" ON public.notifications;
DROP POLICY IF EXISTS "notifications_update" ON public.notifications;
DROP POLICY IF EXISTS "notifications_delete" ON public.notifications;

CREATE POLICY "notifications_select" ON public.notifications 
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "notifications_insert" ON public.notifications 
    FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "notifications_update" ON public.notifications 
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "notifications_delete" ON public.notifications 
    FOR DELETE USING (auth.uid() = user_id);

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 6: RESTAURANT VISITS TABLE (Geofencing)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.restaurant_visits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    restaurant_id UUID NOT NULL REFERENCES public.restaurants(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    entered_at TIMESTAMPTZ DEFAULT NOW(),
    exited_at TIMESTAMPTZ,
    duration_minutes INTEGER,
    has_rated BOOLEAN DEFAULT FALSE,
    UNIQUE(user_id, restaurant_id, entered_at::date)  -- One visit per restaurant per day
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_restaurant_visits_user ON public.restaurant_visits(user_id, entered_at DESC);
CREATE INDEX IF NOT EXISTS idx_restaurant_visits_restaurant ON public.restaurant_visits(restaurant_id);

-- Enable RLS
ALTER TABLE public.restaurant_visits ENABLE ROW LEVEL SECURITY;

-- Policies for restaurant_visits
DROP POLICY IF EXISTS "restaurant_visits_select" ON public.restaurant_visits;
DROP POLICY IF EXISTS "restaurant_visits_insert" ON public.restaurant_visits;
DROP POLICY IF EXISTS "restaurant_visits_update" ON public.restaurant_visits;

CREATE POLICY "restaurant_visits_select" ON public.restaurant_visits 
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "restaurant_visits_insert" ON public.restaurant_visits 
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "restaurant_visits_update" ON public.restaurant_visits 
    FOR UPDATE USING (auth.uid() = user_id);

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 7: DATABASE FUNCTIONS FOR AUTOMATIC UPDATES
-- ═══════════════════════════════════════════════════════════════════════════════

-- Function to update follower counts
CREATE OR REPLACE FUNCTION public.update_follower_counts()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        -- Increment following_count for the follower
        UPDATE public.profiles 
        SET following_count = following_count + 1 
        WHERE id = NEW.follower_id;
        
        -- Increment followers_count for the followed user
        UPDATE public.profiles 
        SET followers_count = followers_count + 1 
        WHERE id = NEW.following_id;
        
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        -- Decrement following_count for the follower
        UPDATE public.profiles 
        SET following_count = GREATEST(following_count - 1, 0) 
        WHERE id = OLD.follower_id;
        
        -- Decrement followers_count for the unfollowed user
        UPDATE public.profiles 
        SET followers_count = GREATEST(followers_count - 1, 0) 
        WHERE id = OLD.following_id;
        
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger for follower counts
DROP TRIGGER IF EXISTS trigger_update_follower_counts ON public.followers;
CREATE TRIGGER trigger_update_follower_counts
    AFTER INSERT OR DELETE ON public.followers
    FOR EACH ROW
    EXECUTE FUNCTION public.update_follower_counts();

-- Function to update likes count on ratings
CREATE OR REPLACE FUNCTION public.update_likes_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE public.ratings 
        SET likes_count = likes_count + 1 
        WHERE id = NEW.rating_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE public.ratings 
        SET likes_count = GREATEST(likes_count - 1, 0) 
        WHERE id = OLD.rating_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger for likes count
DROP TRIGGER IF EXISTS trigger_update_likes_count ON public.likes;
CREATE TRIGGER trigger_update_likes_count
    AFTER INSERT OR DELETE ON public.likes
    FOR EACH ROW
    EXECUTE FUNCTION public.update_likes_count();

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 8: NOTIFICATION TRIGGERS (Auto-create notifications)
-- ═══════════════════════════════════════════════════════════════════════════════

-- Function to create notification on new like
CREATE OR REPLACE FUNCTION public.notify_on_like()
RETURNS TRIGGER AS $$
DECLARE
    rating_owner_id UUID;
    liker_name TEXT;
    dish_name TEXT;
BEGIN
    -- Get rating owner
    SELECT user_id INTO rating_owner_id FROM public.ratings WHERE id = NEW.rating_id;
    
    -- Don't notify if user likes their own rating
    IF rating_owner_id = NEW.user_id THEN
        RETURN NEW;
    END IF;
    
    -- Get liker name
    SELECT name INTO liker_name FROM public.profiles WHERE id = NEW.user_id;
    
    -- Get dish name
    SELECT d.name INTO dish_name 
    FROM public.ratings r
    JOIN public.dishes d ON r.dish_id = d.id
    WHERE r.id = NEW.rating_id;
    
    -- Create notification
    INSERT INTO public.notifications (user_id, type, title, body, data)
    VALUES (
        rating_owner_id,
        'like',
        'New Like',
        liker_name || ' liked your rating of ' || dish_name,
        jsonb_build_object('rating_id', NEW.rating_id, 'user_id', NEW.user_id)
    );
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_notify_on_like ON public.likes;
CREATE TRIGGER trigger_notify_on_like
    AFTER INSERT ON public.likes
    FOR EACH ROW
    EXECUTE FUNCTION public.notify_on_like();

-- Function to create notification on new comment
CREATE OR REPLACE FUNCTION public.notify_on_comment()
RETURNS TRIGGER AS $$
DECLARE
    rating_owner_id UUID;
    commenter_name TEXT;
    dish_name TEXT;
BEGIN
    -- Get rating owner
    SELECT user_id INTO rating_owner_id FROM public.ratings WHERE id = NEW.rating_id;
    
    -- Don't notify if user comments on their own rating
    IF rating_owner_id = NEW.user_id THEN
        RETURN NEW;
    END IF;
    
    -- Get commenter name
    SELECT name INTO commenter_name FROM public.profiles WHERE id = NEW.user_id;
    
    -- Get dish name
    SELECT d.name INTO dish_name 
    FROM public.ratings r
    JOIN public.dishes d ON r.dish_id = d.id
    WHERE r.id = NEW.rating_id;
    
    -- Create notification
    INSERT INTO public.notifications (user_id, type, title, body, data)
    VALUES (
        rating_owner_id,
        'comment',
        'New Comment',
        commenter_name || ' commented on your rating of ' || dish_name,
        jsonb_build_object('rating_id', NEW.rating_id, 'comment_id', NEW.id)
    );
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_notify_on_comment ON public.comments;
CREATE TRIGGER trigger_notify_on_comment
    AFTER INSERT ON public.comments
    FOR EACH ROW
    EXECUTE FUNCTION public.notify_on_comment();

-- Function to create notification on new follower
CREATE OR REPLACE FUNCTION public.notify_on_follow()
RETURNS TRIGGER AS $$
DECLARE
    follower_name TEXT;
BEGIN
    -- Get follower name
    SELECT name INTO follower_name FROM public.profiles WHERE id = NEW.follower_id;
    
    -- Create notification
    INSERT INTO public.notifications (user_id, type, title, body, data)
    VALUES (
        NEW.following_id,
        'follow',
        'New Follower',
        follower_name || ' started following you',
        jsonb_build_object('follower_id', NEW.follower_id)
    );
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_notify_on_follow ON public.followers;
CREATE TRIGGER trigger_notify_on_follow
    AFTER INSERT ON public.followers
    FOR EACH ROW
    EXECUTE FUNCTION public.notify_on_follow();

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 9: ENABLE REALTIME FOR TABLES
-- ═══════════════════════════════════════════════════════════════════════════════

-- Enable realtime for feed-related tables
-- Run these in Supabase Dashboard > Database > Replication if needed:

-- ALTER PUBLICATION supabase_realtime ADD TABLE public.ratings;
-- ALTER PUBLICATION supabase_realtime ADD TABLE public.likes;
-- ALTER PUBLICATION supabase_realtime ADD TABLE public.comments;
-- ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
-- ALTER PUBLICATION supabase_realtime ADD TABLE public.followers;

-- Or use this single command (requires superuser):
DO $$
BEGIN
    -- Check if publication exists and add tables
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        -- Add tables to realtime publication
        EXECUTE 'ALTER PUBLICATION supabase_realtime ADD TABLE public.ratings';
        EXECUTE 'ALTER PUBLICATION supabase_realtime ADD TABLE public.likes';
        EXECUTE 'ALTER PUBLICATION supabase_realtime ADD TABLE public.comments';
        EXECUTE 'ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications';
        EXECUTE 'ALTER PUBLICATION supabase_realtime ADD TABLE public.followers';
    END IF;
EXCEPTION
    WHEN duplicate_object THEN
        -- Tables already in publication, ignore
        NULL;
END $$;

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 10: HELPER FUNCTIONS
-- ═══════════════════════════════════════════════════════════════════════════════

-- Function to get feed for a user (following + own posts)
CREATE OR REPLACE FUNCTION public.get_following_feed(
    p_user_id UUID,
    p_limit INTEGER DEFAULT 20,
    p_offset INTEGER DEFAULT 0
)
RETURNS TABLE (
    id UUID,
    user_id UUID,
    user_name TEXT,
    user_profile_url TEXT,
    dish_id UUID,
    dish_name TEXT,
    dish_image_url TEXT,
    restaurant_id UUID,
    restaurant_name TEXT,
    rating REAL,
    comment TEXT,
    image_url TEXT,
    likes_count INTEGER,
    comments_count BIGINT,
    is_liked BOOLEAN,
    created_at TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        r.id,
        r.user_id,
        p.name AS user_name,
        p.profile_photo_url AS user_profile_url,
        r.dish_id,
        d.name AS dish_name,
        COALESCE(r.image_url, d.image_url) AS dish_image_url,
        r.restaurant_id,
        rest.name AS restaurant_name,
        r.rating,
        r.comment,
        r.image_url,
        r.likes_count,
        (SELECT COUNT(*) FROM public.comments c WHERE c.rating_id = r.id) AS comments_count,
        EXISTS (SELECT 1 FROM public.likes l WHERE l.rating_id = r.id AND l.user_id = p_user_id) AS is_liked,
        r.created_at
    FROM public.ratings r
    JOIN public.profiles p ON r.user_id = p.id
    JOIN public.dishes d ON r.dish_id = d.id
    JOIN public.restaurants rest ON r.restaurant_id = rest.id
    WHERE r.user_id = p_user_id 
       OR r.user_id IN (SELECT following_id FROM public.followers WHERE follower_id = p_user_id)
    ORDER BY r.created_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get comments count for a rating
CREATE OR REPLACE FUNCTION public.get_comments_count(p_rating_id UUID)
RETURNS INTEGER AS $$
BEGIN
    RETURN (SELECT COUNT(*)::INTEGER FROM public.comments WHERE rating_id = p_rating_id);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════════════════════════
-- VERIFICATION: Check all tables exist
-- ═══════════════════════════════════════════════════════════════════════════════

DO $$
DECLARE
    missing_tables TEXT := '';
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'followers') THEN
        missing_tables := missing_tables || 'followers, ';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'comments') THEN
        missing_tables := missing_tables || 'comments, ';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'rating_images') THEN
        missing_tables := missing_tables || 'rating_images, ';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'notifications') THEN
        missing_tables := missing_tables || 'notifications, ';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'restaurant_visits') THEN
        missing_tables := missing_tables || 'restaurant_visits, ';
    END IF;
    
    IF missing_tables = '' THEN
        RAISE NOTICE 'SUCCESS: All required tables have been created!';
    ELSE
        RAISE NOTICE 'WARNING: Some tables may not have been created: %', missing_tables;
    END IF;
END $$;
