-- Server-enforced privacy, blocking, reporting, and moderation foundations.
-- These rules are intentionally centralized in private functions so feed RPCs,
-- direct table reads, and future Edge Functions share the same trust seam.

create schema if not exists private;

create table if not exists public.user_privacy_settings (
    user_id text primary key references public.profiles(id) on delete cascade,
    profile_visibility text not null default 'PUBLIC'
        check (profile_visibility in ('PUBLIC', 'FRIENDS_ONLY', 'PRIVATE')),
    show_email boolean not null default false,
    show_location boolean not null default true,
    allow_tagging boolean not null default true,
    data_collection boolean not null default true,
    share_exact_location boolean not null default false,
    share_approximate_location boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.user_notification_settings (
    user_id text primary key references public.profiles(id) on delete cascade,
    push_enabled boolean not null default true,
    email_enabled boolean not null default true,
    new_follower_notif boolean not null default true,
    new_like_notif boolean not null default true,
    new_comment_notif boolean not null default true,
    weekly_digest boolean not null default true,
    achievement_notif boolean not null default true,
    quiet_hours_start time,
    quiet_hours_end time,
    digest_frequency text not null default 'weekly'
        check (digest_frequency in ('never', 'daily', 'weekly')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.blocks (
    id uuid primary key default gen_random_uuid(),
    blocker_id text not null references public.profiles(id) on delete cascade,
    blocked_id text not null references public.profiles(id) on delete cascade,
    reason text,
    created_at timestamptz not null default now(),
    unique (blocker_id, blocked_id),
    check (blocker_id <> blocked_id)
);

create table if not exists public.reports (
    id uuid primary key default gen_random_uuid(),
    reporter_id text not null references public.profiles(id) on delete cascade,
    target_type text not null check (target_type in ('rating', 'comment', 'profile', 'story', 'user')),
    target_id text not null,
    reason text not null check (char_length(trim(reason)) between 3 and 80),
    details text check (details is null or char_length(details) <= 2000),
    status text not null default 'open' check (status in ('open', 'reviewing', 'resolved', 'dismissed')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.moderation_actions (
    id uuid primary key default gen_random_uuid(),
    moderator_id text references public.profiles(id) on delete set null,
    target_type text not null check (target_type in ('rating', 'comment', 'profile', 'story', 'user')),
    target_id text not null,
    action text not null check (action in ('approve', 'hide', 'reject', 'restore', 'warn', 'ban')),
    reason text,
    created_at timestamptz not null default now()
);

alter table public.ratings
    add column if not exists content_status text not null default 'approved'
        check (content_status in ('pending', 'approved', 'hidden', 'rejected'));

alter table public.comments
    add column if not exists content_status text not null default 'approved'
        check (content_status in ('pending', 'approved', 'hidden', 'rejected'));

alter table public.stories
    add column if not exists content_status text not null default 'approved'
        check (content_status in ('pending', 'approved', 'hidden', 'rejected'));

alter table public.profiles
    add column if not exists account_status text not null default 'active'
        check (account_status in ('active', 'restricted', 'banned'));

create index if not exists idx_user_privacy_settings_visibility
    on public.user_privacy_settings (profile_visibility);

create index if not exists idx_blocks_blocker_blocked
    on public.blocks (blocker_id, blocked_id);

create index if not exists idx_blocks_blocked_blocker
    on public.blocks (blocked_id, blocker_id);

create index if not exists idx_reports_target_status
    on public.reports (target_type, target_id, status, created_at desc);

create index if not exists idx_ratings_status_user_created
    on public.ratings (content_status, user_id, created_at desc);

create index if not exists idx_comments_status_rating_created
    on public.comments (content_status, rating_id, created_at);

alter table public.user_privacy_settings enable row level security;
alter table public.user_notification_settings enable row level security;
alter table public.blocks enable row level security;
alter table public.reports enable row level security;
alter table public.moderation_actions enable row level security;

create or replace function private.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select coalesce(
        (select p.is_admin from public.profiles p where p.id = auth.uid()::text),
        false
    );
$$;

create or replace function private.is_blocked_between(p_user_a text, p_user_b text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
        from public.blocks b
        where (b.blocker_id = p_user_a and b.blocked_id = p_user_b)
           or (b.blocker_id = p_user_b and b.blocked_id = p_user_a)
    );
$$;

create or replace function private.user_can_view_profile(p_target_user_id text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select
        p_target_user_id = auth.uid()::text
        or (
            not private.is_blocked_between(coalesce(auth.uid()::text, ''), p_target_user_id)
            and coalesce((select p.account_status = 'active' from public.profiles p where p.id = p_target_user_id), false)
            and (
                coalesce(
                    (select ups.profile_visibility from public.user_privacy_settings ups where ups.user_id = p_target_user_id),
                    'PUBLIC'
                ) = 'PUBLIC'
                or (
                    coalesce(
                        (select ups.profile_visibility from public.user_privacy_settings ups where ups.user_id = p_target_user_id),
                        'PUBLIC'
                    ) = 'FRIENDS_ONLY'
                    and auth.uid() is not null
                    and exists (
                        select 1
                        from public.followers f
                        where f.following_id = p_target_user_id
                          and f.follower_id = auth.uid()::text
                    )
                )
            )
        );
$$;

create or replace function private.user_allows_location_visibility(p_target_user_id text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select p_target_user_id = auth.uid()::text
        or coalesce(
            (select ups.show_location from public.user_privacy_settings ups where ups.user_id = p_target_user_id),
            true
        );
$$;

create or replace function private.rating_is_visible(p_rating_user_id text, p_content_status text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select
        p_rating_user_id = auth.uid()::text
        or (
            p_content_status = 'approved'
            and private.user_can_view_profile(p_rating_user_id)
        );
$$;

create or replace function public.get_my_privacy_settings()
returns table (
    user_id text,
    profile_visibility text,
    show_email boolean,
    show_location boolean,
    allow_tagging boolean,
    data_collection boolean,
    share_exact_location boolean,
    share_approximate_location boolean
)
language sql
security invoker
set search_path = public
as $$
    insert into public.user_privacy_settings (user_id)
    values (auth.uid()::text)
    on conflict (user_id) do nothing;

    select
        ups.user_id,
        ups.profile_visibility,
        ups.show_email,
        ups.show_location,
        ups.allow_tagging,
        ups.data_collection,
        ups.share_exact_location,
        ups.share_approximate_location
    from public.user_privacy_settings ups
    where ups.user_id = auth.uid()::text;
$$;

create or replace function public.get_my_notification_settings()
returns table (
    user_id text,
    push_enabled boolean,
    email_enabled boolean,
    new_follower_notif boolean,
    new_like_notif boolean,
    new_comment_notif boolean,
    weekly_digest boolean,
    achievement_notif boolean,
    digest_frequency text
)
language sql
security invoker
set search_path = public
as $$
    insert into public.user_notification_settings (user_id)
    values (auth.uid()::text)
    on conflict (user_id) do nothing;

    select
        uns.user_id,
        uns.push_enabled,
        uns.email_enabled,
        uns.new_follower_notif,
        uns.new_like_notif,
        uns.new_comment_notif,
        uns.weekly_digest,
        uns.achievement_notif,
        uns.digest_frequency
    from public.user_notification_settings uns
    where uns.user_id = auth.uid()::text;
$$;

create or replace function public.report_content(
    p_target_type text,
    p_target_id text,
    p_reason text,
    p_details text default null
)
returns uuid
language plpgsql
security invoker
set search_path = public
as $$
declare
    v_report_id uuid;
begin
    if auth.uid() is null then
        raise exception 'Authentication required';
    end if;

    insert into public.reports (reporter_id, target_type, target_id, reason, details)
    values (auth.uid()::text, p_target_type, p_target_id, trim(p_reason), nullif(trim(coalesce(p_details, '')), ''))
    returning id into v_report_id;

    return v_report_id;
end;
$$;

create or replace function public.block_user(p_blocked_id text, p_reason text default null)
returns void
language plpgsql
security invoker
set search_path = public
as $$
begin
    if auth.uid() is null then
        raise exception 'Authentication required';
    end if;

    insert into public.blocks (blocker_id, blocked_id, reason)
    values (auth.uid()::text, p_blocked_id, nullif(trim(coalesce(p_reason, '')), ''))
    on conflict (blocker_id, blocked_id) do nothing;

    delete from public.followers
    where (follower_id = auth.uid()::text and following_id = p_blocked_id)
       or (follower_id = p_blocked_id and following_id = auth.uid()::text);
end;
$$;

drop policy if exists user_privacy_settings_select on public.user_privacy_settings;
drop policy if exists user_privacy_settings_insert on public.user_privacy_settings;
drop policy if exists user_privacy_settings_update on public.user_privacy_settings;
drop policy if exists user_notification_settings_select on public.user_notification_settings;
drop policy if exists user_notification_settings_insert on public.user_notification_settings;
drop policy if exists user_notification_settings_update on public.user_notification_settings;
drop policy if exists blocks_select on public.blocks;
drop policy if exists blocks_insert on public.blocks;
drop policy if exists blocks_delete on public.blocks;
drop policy if exists reports_insert on public.reports;
drop policy if exists reports_select_owner_or_admin on public.reports;
drop policy if exists reports_update_admin on public.reports;
drop policy if exists moderation_actions_select_admin on public.moderation_actions;
drop policy if exists moderation_actions_insert_admin on public.moderation_actions;

create policy user_privacy_settings_select
    on public.user_privacy_settings for select
    using (auth.uid()::text = user_id);

create policy user_privacy_settings_insert
    on public.user_privacy_settings for insert
    with check (auth.uid()::text = user_id);

create policy user_privacy_settings_update
    on public.user_privacy_settings for update
    using (auth.uid()::text = user_id)
    with check (auth.uid()::text = user_id);

create policy user_notification_settings_select
    on public.user_notification_settings for select
    using (auth.uid()::text = user_id);

create policy user_notification_settings_insert
    on public.user_notification_settings for insert
    with check (auth.uid()::text = user_id);

create policy user_notification_settings_update
    on public.user_notification_settings for update
    using (auth.uid()::text = user_id)
    with check (auth.uid()::text = user_id);

create policy blocks_select
    on public.blocks for select
    using (auth.uid()::text = blocker_id);

create policy blocks_insert
    on public.blocks for insert
    with check (auth.uid()::text = blocker_id);

create policy blocks_delete
    on public.blocks for delete
    using (auth.uid()::text = blocker_id);

create policy reports_insert
    on public.reports for insert
    with check (auth.uid()::text = reporter_id);

create policy reports_select_owner_or_admin
    on public.reports for select
    using (auth.uid()::text = reporter_id or private.is_admin());

create policy reports_update_admin
    on public.reports for update
    using (private.is_admin())
    with check (private.is_admin());

create policy moderation_actions_select_admin
    on public.moderation_actions for select
    using (private.is_admin());

create policy moderation_actions_insert_admin
    on public.moderation_actions for insert
    with check (private.is_admin());

drop policy if exists profiles_select on public.profiles;
create policy profiles_select
    on public.profiles for select
    using (private.user_can_view_profile(id) or private.is_admin());

drop policy if exists ratings_select on public.ratings;
create policy ratings_select
    on public.ratings for select
    using (private.rating_is_visible(user_id, content_status) or private.is_admin());

drop policy if exists comments_select on public.comments;
create policy comments_select
    on public.comments for select
    using (
        content_status = 'approved'
        and exists (
            select 1
            from public.ratings r
            where r.id = comments.rating_id
              and private.rating_is_visible(r.user_id, r.content_status)
        )
    );

drop policy if exists followers_select on public.followers;
create policy followers_select
    on public.followers for select
    using (
        auth.uid()::text in (follower_id, following_id)
        or (
            private.user_can_view_profile(follower_id)
            and private.user_can_view_profile(following_id)
        )
    );

grant usage on schema private to authenticated;
grant execute on function public.get_my_privacy_settings() to authenticated;
grant execute on function public.get_my_notification_settings() to authenticated;
grant execute on function public.report_content(text, text, text, text) to authenticated;
grant execute on function public.block_user(text, text) to authenticated;

create or replace function public.get_feed_page(
    p_filter text default 'FOLLOWING',
    p_limit integer default 20,
    p_cursor_created_at timestamptz default null,
    p_cursor_id text default null,
    p_cursor_rating double precision default null,
    p_user_lat double precision default null,
    p_user_lon double precision default null,
    p_user_city text default null,
    p_radius_km double precision default 25,
    p_current_user_id text default null
)
returns table (
    id text,
    user_id text,
    user_profile_image_url text,
    user_name text,
    dish_image_url text,
    dish_name text,
    dish_id text,
    restaurant_name text,
    restaurant_city text,
    rating double precision,
    likes_count integer,
    comments_count integer,
    is_liked boolean,
    created_at timestamptz,
    comment text,
    image_urls text[],
    price double precision
)
language plpgsql
stable
security invoker
set search_path = public
as $$
declare
    v_filter text := upper(coalesce(nullif(p_filter, ''), 'FOLLOWING'));
    v_limit integer := least(greatest(coalesce(p_limit, 20), 1), 50);
    v_user_id text := coalesce(auth.uid()::text, nullif(p_current_user_id, ''));
    v_user_city text := lower(nullif(trim(coalesce(p_user_city, '')), ''));
    v_user_point geography := case
        when p_user_lat is not null and p_user_lon is not null
            then ST_SetSRID(ST_MakePoint(p_user_lon, p_user_lat), 4326)::geography
        else null
    end;
begin
    return query
    select
        r.id,
        r.user_id,
        p.profile_photo_url,
        coalesce(p.name, 'Unknown'),
        coalesce(r.image_url, d.image_url),
        coalesce(d.name, 'Unknown Dish'),
        r.dish_id,
        coalesce(rest.name, 'Unknown Restaurant'),
        case
            when private.user_allows_location_visibility(r.user_id) then coalesce(rest.city, '')
            else ''
        end,
        r.rating,
        coalesce(r.likes_count, 0),
        coalesce(cc.comments_count, 0)::integer,
        (v_user_id is not null and l.rating_id is not null),
        r.created_at,
        coalesce(r.comment, ''),
        array_remove(
            array_cat(
                array[coalesce(r.image_url, d.image_url)],
                coalesce(ri.image_urls, '{}'::text[])
            ),
            null
        ),
        r.price::double precision
    from public.ratings r
    left join public.profiles p on p.id = r.user_id
    left join public.dishes d on d.id = r.dish_id
    left join public.restaurants rest on rest.id = coalesce(nullif(r.restaurant_id, ''), d.restaurant_id)
    left join (
        select rating_id, count(*)::integer as comments_count
        from public.comments
        where content_status = 'approved'
        group by rating_id
    ) cc on cc.rating_id = r.id
    left join (
        select ri_src.rating_id, array_agg(ri_src.image_url order by ri_src.sort_order, ri_src.created_at) as image_urls
        from public.rating_images ri_src
        group by ri_src.rating_id
    ) ri on ri.rating_id = r.id
    left join public.likes l on l.rating_id = r.id and l.user_id = v_user_id
    left join public.user_privacy_settings ups on ups.user_id = r.user_id
    where private.rating_is_visible(r.user_id, r.content_status)
      and (
        (
            v_filter = 'FOLLOWING'
            and v_user_id is not null
            and exists (
                select 1
                from public.followers f
                where f.follower_id = v_user_id
                  and f.following_id = r.user_id
            )
        )
        or (
            v_filter = 'MY_RATINGS'
            and v_user_id is not null
            and r.user_id = v_user_id
        )
        or (
            v_filter = 'TRENDING'
            and r.rating >= 4.0
        )
        or (
            v_filter = 'NEARBY'
            and coalesce(ups.show_location, true)
            and (
                (
                    v_user_point is not null
                    and (
                        (
                            coalesce(ups.share_exact_location, false)
                            and r.location is not null
                            and ST_DWithin(r.location::geography, v_user_point, p_radius_km * 1000)
                        )
                        or (
                            rest.latitude is not null
                            and rest.longitude is not null
                            and ST_DWithin(
                                ST_SetSRID(ST_MakePoint(rest.longitude, rest.latitude), 4326)::geography,
                                v_user_point,
                                p_radius_km * 1000
                            )
                        )
                    )
                )
                or (
                    v_user_city is not null
                    and coalesce(ups.share_approximate_location, true)
                    and lower(rest.city) = v_user_city
                )
            )
        )
    )
    and (
        p_cursor_created_at is null
        or (
            v_filter = 'TRENDING'
            and (
                r.rating < coalesce(p_cursor_rating, r.rating)
                or (
                    r.rating = coalesce(p_cursor_rating, r.rating)
                    and (r.created_at, r.id) < (p_cursor_created_at, coalesce(p_cursor_id, ''))
                )
            )
        )
        or (
            v_filter <> 'TRENDING'
            and (r.created_at, r.id) < (p_cursor_created_at, coalesce(p_cursor_id, ''))
        )
    )
    order by
        case when v_filter = 'TRENDING' then r.rating end desc nulls last,
        r.created_at desc,
        r.id desc
    limit v_limit;
end;
$$;

grant execute on function public.get_feed_page(
    text,
    integer,
    timestamptz,
    text,
    double precision,
    double precision,
    double precision,
    text,
    double precision,
    text
) to anon, authenticated;
