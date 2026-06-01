-- Backend owns push dispatch now that Supabase Edge Functions are removed.
-- Keep Supabase Realtime for notification row subscriptions, but stop DB
-- triggers from calling deleted Edge Functions or creating duplicate social
-- notifications beside the Node backend routes.

drop trigger if exists on_notification_insert_push on public.notifications;
drop function if exists public.notify_push_on_new_notification();

drop trigger if exists on_like_insert_notify on public.likes;
drop function if exists public.notify_on_new_like();

drop trigger if exists on_comment_insert_notify on public.comments;
drop function if exists public.notify_on_new_comment();
