-- Speed up the dish rating submission path.
-- RLS changes follow Supabase's current guidance to scope policies to authenticated
-- users and wrap auth helpers in SELECT so Postgres can cache them per statement.

create index if not exists idx_dishes_restaurant_name
on public.dishes (restaurant_id, name);

alter policy dishes_insert on public.dishes
to authenticated
with check (true);

alter policy restaurants_insert on public.restaurants
to authenticated
with check (true);

alter policy restaurants_update on public.restaurants
to authenticated
using (true);

alter policy ratings_insert on public.ratings
to authenticated
with check (((select auth.uid())::text = user_id));

alter policy ratings_update on public.ratings
to authenticated
using (((select auth.uid())::text = user_id));

alter policy ratings_delete on public.ratings
to authenticated
using (((select auth.uid())::text = user_id));

alter policy profiles_insert on public.profiles
to authenticated
with check (((select auth.uid())::text = id));

alter policy profiles_update on public.profiles
to authenticated
using (((select auth.uid())::text = id));

alter policy user_badges_insert on public.user_badges
to authenticated
with check (((select auth.uid())::text = user_id));

alter policy followers_insert on public.followers
to authenticated
with check (((select auth.uid())::text = follower_id));

alter policy followers_delete on public.followers
to authenticated
using (((select auth.uid())::text = follower_id));

alter policy notifications_select on public.notifications
to authenticated
using (((select auth.uid())::text = user_id));

alter policy notifications_update on public.notifications
to authenticated
using (((select auth.uid())::text = user_id));

alter policy rating_images_insert on public.rating_images
to authenticated
with check (
    ((select auth.uid())::text = (
        select ratings.user_id
        from public.ratings
        where ratings.id = rating_images.rating_id
    ))
);
