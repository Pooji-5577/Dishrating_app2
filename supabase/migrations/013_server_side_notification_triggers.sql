-- ============================================================
-- Migration 013: Server-side notification triggers for likes & comments
--
-- Instead of relying on the client app to insert notifications,
-- these triggers automatically create notification rows when:
--   1. A like is added (notify the post owner)
--   2. A comment is added (notify the post owner)
--
-- The existing push trigger (on_notification_insert_push) then
-- fires and sends the FCM push notification via the edge function.
-- ============================================================

-- ─── Trigger: Auto-notify on new LIKE ───────────────────────────────

CREATE OR REPLACE FUNCTION public.notify_on_new_like()
RETURNS TRIGGER AS $$
DECLARE
  _rating RECORD;
  _liker_name TEXT;
  _dish_name TEXT;
BEGIN
  -- Skip if user liked their own post
  SELECT user_id, dish_id INTO _rating
    FROM public.ratings WHERE id = NEW.rating_id;

  IF _rating.user_id IS NULL OR _rating.user_id = NEW.user_id THEN
    RETURN NEW;
  END IF;

  -- Get liker name
  SELECT COALESCE(name, 'Someone') INTO _liker_name
    FROM public.profiles WHERE id = NEW.user_id;

  -- Get dish name
  SELECT COALESCE(name, 'a dish') INTO _dish_name
    FROM public.dishes WHERE id = _rating.dish_id;

  -- Insert notification (ignore duplicates)
  INSERT INTO public.notifications (user_id, event_type, title, body, data)
  VALUES (
    _rating.user_id,
    'review_liked',
    '❤️ Review Liked!',
    _liker_name || ' liked your review of ' || _dish_name,
    jsonb_build_object(
      'source_id', 'like_' || NEW.rating_id || '_' || NEW.user_id,
      'screen', 'SocialFeed',
      'reviewId', NEW.rating_id,
      'dishName', _dish_name
    )
  )
  ON CONFLICT DO NOTHING;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_like_insert_notify ON public.likes;
CREATE TRIGGER on_like_insert_notify
  AFTER INSERT ON public.likes
  FOR EACH ROW
  EXECUTE FUNCTION public.notify_on_new_like();

-- ─── Trigger: Auto-notify on new COMMENT ────────────────────────────

CREATE OR REPLACE FUNCTION public.notify_on_new_comment()
RETURNS TRIGGER AS $$
DECLARE
  _rating RECORD;
  _commenter_name TEXT;
  _dish_name TEXT;
BEGIN
  -- Get the rating owner
  SELECT user_id, dish_id INTO _rating
    FROM public.ratings WHERE id = NEW.rating_id;

  -- Skip if user commented on their own post
  IF _rating.user_id IS NULL OR _rating.user_id = NEW.user_id THEN
    RETURN NEW;
  END IF;

  -- Get commenter name
  SELECT COALESCE(name, 'Someone') INTO _commenter_name
    FROM public.profiles WHERE id = NEW.user_id;

  -- Get dish name
  SELECT COALESCE(name, 'a dish') INTO _dish_name
    FROM public.dishes WHERE id = _rating.dish_id;

  -- Insert notification (ignore duplicates)
  INSERT INTO public.notifications (user_id, event_type, title, body, data)
  VALUES (
    _rating.user_id,
    'dish_comment',
    '💬 New Comment',
    _commenter_name || ' commented on your review of ' || _dish_name,
    jsonb_build_object(
      'source_id', 'comment_' || NEW.rating_id || '_' || NEW.user_id,
      'screen', 'SocialFeed',
      'ratingId', NEW.rating_id,
      'dishName', _dish_name
    )
  )
  ON CONFLICT DO NOTHING;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_comment_insert_notify ON public.comments;
CREATE TRIGGER on_comment_insert_notify
  AFTER INSERT ON public.comments
  FOR EACH ROW
  EXECUTE FUNCTION public.notify_on_new_comment();
