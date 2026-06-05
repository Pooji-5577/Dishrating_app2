-- Shared admin backend support.
-- Admin actions use safe status transitions and audit rows instead of hard deletes.

alter table public.restaurants
    add column if not exists catalog_status text not null default 'verified'
        check (catalog_status in ('pending', 'verified', 'hidden', 'duplicate'));

alter table public.restaurants
    add column if not exists featured boolean not null default false;

alter table public.dishes
    add column if not exists catalog_status text not null default 'approved'
        check (catalog_status in ('pending', 'approved', 'hidden', 'rejected'));

do $$
begin
    alter table public.moderation_actions
        drop constraint if exists moderation_actions_target_type_check;

    alter table public.moderation_actions
        add constraint moderation_actions_target_type_check
        check (target_type in ('rating', 'comment', 'profile', 'story', 'user', 'restaurant', 'dish'));

    alter table public.moderation_actions
        drop constraint if exists moderation_actions_action_check;

    alter table public.moderation_actions
        add constraint moderation_actions_action_check
        check (action in ('approve', 'hide', 'reject', 'restore', 'warn', 'suspend', 'ban'));
end $$;

create index if not exists idx_restaurants_catalog_status
    on public.restaurants (catalog_status, created_at desc);

create index if not exists idx_dishes_catalog_status
    on public.dishes (catalog_status, created_at desc);

create index if not exists idx_moderation_actions_target_created
    on public.moderation_actions (target_type, target_id, created_at desc);
