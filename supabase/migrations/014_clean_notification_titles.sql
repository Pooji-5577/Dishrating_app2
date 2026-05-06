-- ============================================================
-- Migration 014: Normalize notification titles for likes and comments
--
-- Rewrites trigger functions from migration 013 as single-statement
-- SQL-language functions (no semicolons inside the body) so they
-- run correctly in the Supabase SQL editor.
-- Removes exclamation marks and leading emojis from headlines.
-- ============================================================

-- ─── Like notification ───────────────────────────────────────────────

CREATE OR REPLACE FUNCTION public.notify_on_new_like()
RETURNS TRIGGER
LANGUAGE sql
SECURITY DEFINER
AS $function$
  INSERT INTO public.notifications (user_id, event_type, title, body, data)
  SELECT
    r.user_id,
    'review_liked',
    'Review Liked',
    COALESCE(p.name, 'Someone') || ' liked your review of ' || COALESCE(d.name, 'a dish'),
    jsonb_build_object(
      'source_id', 'like_' || NEW.rating_id || '_' || NEW.user_id,
      'screen',    'SocialFeed',
      'reviewId',  NEW.rating_id,
      'dishName',  COALESCE(d.name, 'a dish')
    )
  FROM public.ratings r
  LEFT JOIN public.profiles p ON p.id = NEW.user_id
  LEFT JOIN public.dishes   d ON d.id = r.dish_id
  WHERE r.id        = NEW.rating_id
    AND r.user_id  IS NOT NULL
    AND r.user_id  <> NEW.user_id
  ON CONFLICT DO NOTHING
$function$;

-- ─── Comment notification ─────────────────────────────────────────────

CREATE OR REPLACE FUNCTION public.notify_on_new_comment()
RETURNS TRIGGER
LANGUAGE sql
SECURITY DEFINER
AS $function$
  INSERT INTO public.notifications (user_id, event_type, title, body, data)
  SELECT
    r.user_id,
    'dish_comment',
    'New Comment',
    COALESCE(p.name, 'Someone') || ' commented on your review of ' || COALESCE(d.name, 'a dish'),
    jsonb_build_object(
      'source_id', 'comment_' || NEW.rating_id || '_' || NEW.user_id,
      'screen',    'SocialFeed',
      'ratingId',  NEW.rating_id,
      'dishName',  COALESCE(d.name, 'a dish')
    )
  FROM public.ratings r
  LEFT JOIN public.profiles p ON p.id = NEW.user_id
  LEFT JOIN public.dishes   d ON d.id = r.dish_id
  WHERE r.id        = NEW.rating_id
    AND r.user_id  IS NOT NULL
    AND r.user_id  <> NEW.user_id
  ON CONFLICT DO NOTHING
$function$;
