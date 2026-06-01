-- Ensure notification creation respects server-side preferences and blocks.

create or replace function public.create_notification(
    p_user_id text,
    p_type text,
    p_title text,
    p_body text,
    p_data jsonb default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_caller uuid;
    v_result record;
    v_settings public.user_notification_settings%rowtype;
begin
    v_caller := auth.uid();
    if v_caller is null then
        raise exception 'Not authenticated';
    end if;

    if p_user_id = v_caller::text then
        return jsonb_build_object('success', true, 'skipped', true);
    end if;

    if private.is_blocked_between(v_caller::text, p_user_id) then
        return jsonb_build_object('success', true, 'skipped', true, 'reason', 'blocked');
    end if;

    insert into public.user_notification_settings (user_id)
    values (p_user_id)
    on conflict (user_id) do nothing;

    select *
    into v_settings
    from public.user_notification_settings
    where user_id = p_user_id;

    if not coalesce(v_settings.push_enabled, true) then
        return jsonb_build_object('success', true, 'skipped', true, 'reason', 'push_disabled');
    end if;

    if p_type in ('follow', 'new_follower') and not coalesce(v_settings.new_follower_notif, true) then
        return jsonb_build_object('success', true, 'skipped', true, 'reason', 'follower_disabled');
    end if;

    if p_type = 'review_liked' and not coalesce(v_settings.new_like_notif, true) then
        return jsonb_build_object('success', true, 'skipped', true, 'reason', 'like_disabled');
    end if;

    if p_type = 'dish_comment' and not coalesce(v_settings.new_comment_notif, true) then
        return jsonb_build_object('success', true, 'skipped', true, 'reason', 'comment_disabled');
    end if;

    if p_type in ('points_earned', 'challenge_completed', 'first_dish') and not coalesce(v_settings.achievement_notif, true) then
        return jsonb_build_object('success', true, 'skipped', true, 'reason', 'achievement_disabled');
    end if;

    if p_type in ('weekly_digest', 'trending_dish') and not coalesce(v_settings.weekly_digest, true) then
        return jsonb_build_object('success', true, 'skipped', true, 'reason', 'digest_disabled');
    end if;

    insert into public.notifications (user_id, event_type, title, body, data)
    values (p_user_id, p_type, p_title, p_body, coalesce(p_data, '{}'::jsonb))
    returning id, user_id, event_type into v_result;

    return jsonb_build_object(
        'success', true,
        'id', v_result.id,
        'user_id', v_result.user_id
    );
exception when unique_violation then
    return jsonb_build_object('success', true, 'duplicate', true);
end;
$$;
