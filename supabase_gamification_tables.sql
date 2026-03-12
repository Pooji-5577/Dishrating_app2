-- ═══════════════════════════════════════════════════════════════════
-- SmackCheck Gamification Tables
-- Run this SQL in your Supabase SQL Editor (https://supabase.com/dashboard)
-- ═══════════════════════════════════════════════════════════════════

-- 1. Add gamification columns to existing profiles table
ALTER TABLE profiles
  ADD COLUMN IF NOT EXISTS total_points   INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS current_streak INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS longest_streak INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS last_active_date DATE,
  ADD COLUMN IF NOT EXISTS profile_picture_uploaded BOOLEAN DEFAULT false;

-- 2. User Actions log — every point-earning action is recorded here
CREATE TABLE IF NOT EXISTS user_actions (
  id            UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id       UUID NOT NULL,
  action_type   TEXT NOT NULL,        -- 'upload_photo', 'rate_dish', 'write_review', 'add_restaurant', 'first_profile_pic'
  points_earned INTEGER NOT NULL,
  metadata      JSONB DEFAULT '{}',   -- e.g. {"dish_id": "...", "restaurant_name": "..."}
  created_at    TIMESTAMPTZ DEFAULT now()
);

-- Index for fast user + date queries (challenge progress, streak)
CREATE INDEX IF NOT EXISTS idx_user_actions_user_date
  ON user_actions (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_actions_type
  ON user_actions (user_id, action_type, created_at);

-- 3. Challenge definitions — seeded once, reused daily/weekly
CREATE TABLE IF NOT EXISTS challenges (
  id            UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  title         TEXT NOT NULL,
  description   TEXT NOT NULL,
  challenge_type TEXT NOT NULL,       -- 'daily' or 'weekly'
  action_type   TEXT NOT NULL,        -- which action_type counts toward this
  target_count  INTEGER NOT NULL,     -- how many actions to complete it
  xp_reward     INTEGER NOT NULL,     -- bonus XP on completion
  icon_name     TEXT DEFAULT 'Star',  -- Material icon name for the UI
  is_active     BOOLEAN DEFAULT true
);

-- 4. User challenge progress — tracks each user's progress per challenge per period
CREATE TABLE IF NOT EXISTS user_challenges (
  id            UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id       UUID NOT NULL,
  challenge_id  UUID NOT NULL REFERENCES challenges(id),
  period_start  DATE NOT NULL,        -- start of the day (daily) or week (weekly)
  current_count INTEGER DEFAULT 0,
  is_completed  BOOLEAN DEFAULT false,
  completed_at  TIMESTAMPTZ,
  created_at    TIMESTAMPTZ DEFAULT now(),
  UNIQUE(user_id, challenge_id, period_start)
);

CREATE INDEX IF NOT EXISTS idx_user_challenges_user_period
  ON user_challenges (user_id, period_start);

-- 5. Streak rewards log — prevents double-awarding streak milestones
CREATE TABLE IF NOT EXISTS streak_rewards (
  id            UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id       UUID NOT NULL,
  streak_days   INTEGER NOT NULL,     -- 3 or 7
  points_earned INTEGER NOT NULL,
  awarded_at    TIMESTAMPTZ DEFAULT now()
);

-- Expression-based unique index (prevents double-awarding same milestone on same day)
CREATE UNIQUE INDEX IF NOT EXISTS idx_streak_rewards_unique_per_day
  ON streak_rewards (user_id, streak_days, (awarded_at::date));

-- ═══════════════════════════════════════════════════════════════════
-- Seed default challenges
-- ═══════════════════════════════════════════════════════════════════

INSERT INTO challenges (title, description, challenge_type, action_type, target_count, xp_reward, icon_name) VALUES
  -- Daily challenges
  ('Rate 3 Dishes',       'Rate any 3 dishes today',               'daily',  'rate_dish',     3, 20, 'Star'),
  ('Upload a Photo',      'Upload 1 dish photo today',             'daily',  'upload_photo',  1, 15, 'CameraAlt'),
  ('Write a Review',      'Write 1 detailed review today',         'daily',  'write_review',  1, 15, 'RateReview'),
  -- Weekly challenges
  ('Visit 3 Restaurants', 'Rate dishes at 3 different restaurants', 'weekly', 'add_restaurant', 3, 40, 'Restaurant'),
  ('Upload 5 Dishes',     'Upload 5 dish photos this week',        'weekly', 'upload_photo',   5, 50, 'CameraAlt'),
  ('Review Master',       'Write 5 reviews this week',             'weekly', 'write_review',   5, 40, 'RateReview')
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════
-- Enable RLS (Row Level Security)
-- ═══════════════════════════════════════════════════════════════════

ALTER TABLE user_actions    ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_challenges ENABLE ROW LEVEL SECURITY;
ALTER TABLE streak_rewards  ENABLE ROW LEVEL SECURITY;

-- Users can insert their own actions
CREATE POLICY "Users can insert own actions"
  ON user_actions FOR INSERT
  WITH CHECK (auth.uid() = user_id);

-- Users can read their own actions
CREATE POLICY "Users can read own actions"
  ON user_actions FOR SELECT
  USING (auth.uid() = user_id);

-- Users can manage their own challenge progress
CREATE POLICY "Users can manage own challenges"
  ON user_challenges FOR ALL
  USING (auth.uid() = user_id);

-- Users can manage their own streak rewards
CREATE POLICY "Users can manage own streak rewards"
  ON streak_rewards FOR ALL
  USING (auth.uid() = user_id);

-- Challenges are readable by everyone
CREATE POLICY "Challenges are public"
  ON challenges FOR SELECT
  USING (true);

-- Leaderboard view: everyone can read all profiles (for leaderboard)
-- (profiles RLS should already allow select; if not, add:)
-- CREATE POLICY "Public profiles for leaderboard"
--   ON profiles FOR SELECT USING (true);
