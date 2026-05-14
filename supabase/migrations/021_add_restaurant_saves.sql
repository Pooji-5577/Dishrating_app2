-- Save/bookmark restaurants per user
create table if not exists public.restaurant_saves (
    user_id text not null references public.profiles(id) on delete cascade,
    restaurant_id text not null references public.restaurants(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, restaurant_id)
);

create index if not exists idx_restaurant_saves_user_created
    on public.restaurant_saves (user_id, created_at desc);

alter table public.restaurant_saves enable row level security;

drop policy if exists restaurant_saves_select on public.restaurant_saves;
drop policy if exists restaurant_saves_insert on public.restaurant_saves;
drop policy if exists restaurant_saves_delete on public.restaurant_saves;

create policy restaurant_saves_select
    on public.restaurant_saves
    for select
    using (auth.uid()::text = user_id);

create policy restaurant_saves_insert
    on public.restaurant_saves
    for insert
    with check (auth.uid()::text = user_id);

create policy restaurant_saves_delete
    on public.restaurant_saves
    for delete
    using (auth.uid()::text = user_id);

