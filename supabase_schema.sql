-- Supabase SQL Schema for Smackcheck2 App
-- Run this in your Supabase SQL Editor to create all required tables

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==================== PROFILES TABLE ====================
-- Stores user profile information (linked to auth.users)
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    profile_photo_url TEXT,
    level INTEGER DEFAULT 1,
    xp INTEGER DEFAULT 0,
    streak_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable RLS on profiles
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- Profiles policies
CREATE POLICY "Users can view all profiles" ON profiles
    FOR SELECT USING (true);

CREATE POLICY "Users can update own profile" ON profiles
    FOR UPDATE USING (auth.uid() = id);

CREATE POLICY "Users can insert own profile" ON profiles
    FOR INSERT WITH CHECK (auth.uid() = id);

-- ==================== BADGES TABLE ====================
-- Stores available badges/achievements
CREATE TABLE IF NOT EXISTS badges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    icon_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable RLS on badges
ALTER TABLE badges ENABLE ROW LEVEL SECURITY;

-- Badges policies (everyone can read)
CREATE POLICY "Anyone can view badges" ON badges
    FOR SELECT USING (true);

-- ==================== USER BADGES TABLE ====================
-- Junction table for user-badge relationships
CREATE TABLE IF NOT EXISTS user_badges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    badge_id UUID NOT NULL REFERENCES badges(id) ON DELETE CASCADE,
    earned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, badge_id)
);

-- Enable RLS on user_badges
ALTER TABLE user_badges ENABLE ROW LEVEL SECURITY;

-- User badges policies
CREATE POLICY "Users can view all user badges" ON user_badges
    FOR SELECT USING (true);

CREATE POLICY "System can insert user badges" ON user_badges
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- ==================== RESTAURANTS TABLE ====================
-- Stores restaurant information
CREATE TABLE IF NOT EXISTS restaurants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    city TEXT NOT NULL,
    cuisine TEXT NOT NULL,
    image_urls TEXT[] DEFAULT '{}',
    average_rating REAL DEFAULT 0,
    review_count INTEGER DEFAULT 0,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable RLS on restaurants
ALTER TABLE restaurants ENABLE ROW LEVEL SECURITY;

-- Restaurant policies
CREATE POLICY "Anyone can view restaurants" ON restaurants
    FOR SELECT USING (true);

CREATE POLICY "Authenticated users can create restaurants" ON restaurants
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');

CREATE POLICY "Authenticated users can update restaurants" ON restaurants
    FOR UPDATE USING (auth.role() = 'authenticated');

-- ==================== DISHES TABLE ====================
-- Stores dish information
CREATE TABLE IF NOT EXISTS dishes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    image_url TEXT,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable RLS on dishes
ALTER TABLE dishes ENABLE ROW LEVEL SECURITY;

-- Dishes policies
CREATE POLICY "Anyone can view dishes" ON dishes
    FOR SELECT USING (true);

CREATE POLICY "Authenticated users can create dishes" ON dishes
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- ==================== RATINGS TABLE ====================
-- Stores user ratings/reviews
CREATE TABLE IF NOT EXISTS ratings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    dish_id UUID NOT NULL REFERENCES dishes(id) ON DELETE CASCADE,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    rating REAL NOT NULL CHECK (rating >= 0 AND rating <= 5),
    comment TEXT DEFAULT '',
    image_url TEXT,
    likes_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable RLS on ratings
ALTER TABLE ratings ENABLE ROW LEVEL SECURITY;

-- Ratings policies
CREATE POLICY "Anyone can view ratings" ON ratings
    FOR SELECT USING (true);

CREATE POLICY "Authenticated users can create ratings" ON ratings
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own ratings" ON ratings
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own ratings" ON ratings
    FOR DELETE USING (auth.uid() = user_id);

-- ==================== LIKES TABLE ====================
-- Stores likes on ratings
CREATE TABLE IF NOT EXISTS likes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    rating_id UUID NOT NULL REFERENCES ratings(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, rating_id)
);

-- Enable RLS on likes
ALTER TABLE likes ENABLE ROW LEVEL SECURITY;

-- Likes policies
CREATE POLICY "Anyone can view likes" ON likes
    FOR SELECT USING (true);

CREATE POLICY "Authenticated users can create likes" ON likes
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own likes" ON likes
    FOR DELETE USING (auth.uid() = user_id);

-- ==================== INDEXES ====================
-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_ratings_user_id ON ratings(user_id);
CREATE INDEX IF NOT EXISTS idx_ratings_dish_id ON ratings(dish_id);
CREATE INDEX IF NOT EXISTS idx_ratings_restaurant_id ON ratings(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_ratings_created_at ON ratings(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_dishes_restaurant_id ON dishes(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_restaurants_city ON restaurants(city);
CREATE INDEX IF NOT EXISTS idx_restaurants_cuisine ON restaurants(cuisine);
CREATE INDEX IF NOT EXISTS idx_restaurants_average_rating ON restaurants(average_rating DESC);
CREATE INDEX IF NOT EXISTS idx_likes_rating_id ON likes(rating_id);
CREATE INDEX IF NOT EXISTS idx_user_badges_user_id ON user_badges(user_id);

-- ==================== FUNCTIONS ====================
-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to update updated_at on profiles
CREATE TRIGGER update_profiles_updated_at
    BEFORE UPDATE ON profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==================== STORAGE BUCKETS ====================
-- Run these in the Supabase Dashboard > Storage section
-- Or use the following SQL (requires admin privileges)

-- Insert storage buckets (you may need to do this via Dashboard)
-- INSERT INTO storage.buckets (id, name, public) VALUES ('dish-images', 'dish-images', true);
-- INSERT INTO storage.buckets (id, name, public) VALUES ('profile-images', 'profile-images', true);
-- INSERT INTO storage.buckets (id, name, public) VALUES ('restaurant-images', 'restaurant-images', true);

-- ==================== SAMPLE DATA (OPTIONAL) ====================
-- Insert some sample badges
INSERT INTO badges (name, description, icon_url) VALUES
    ('First Bite', 'Submit your first dish rating', null),
    ('Food Explorer', 'Rate dishes at 5 different restaurants', null),
    ('Streak Master', 'Maintain a 7-day rating streak', null),
    ('Top Critic', 'Receive 50 likes on your ratings', null),
    ('Cuisine Connoisseur', 'Rate dishes from 10 different cuisines', null)
ON CONFLICT DO NOTHING;

-- Insert some sample restaurants
INSERT INTO restaurants (name, city, cuisine, average_rating, review_count) VALUES
    ('Pasta Paradise', 'New York', 'Italian', 4.5, 120),
    ('Sushi Supreme', 'Los Angeles', 'Japanese', 4.7, 89),
    ('Curry House', 'Chicago', 'Indian', 4.3, 156),
    ('Taco Town', 'Houston', 'Mexican', 4.2, 78),
    ('Dragon Palace', 'San Francisco', 'Chinese', 4.4, 203)
ON CONFLICT DO NOTHING;
