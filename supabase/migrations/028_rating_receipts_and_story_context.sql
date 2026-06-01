-- Receipt validation data and story context metadata.

create table if not exists public.rating_receipts (
    id uuid primary key default gen_random_uuid(),
    user_id text not null references public.profiles(id) on delete cascade,
    rating_id text not null references public.ratings(id) on delete cascade,
    image_url text not null,
    source text not null check (source in ('camera', 'gallery', 'screenshot')),
    validation_status text not null default 'pending'
        check (validation_status in ('pending', 'validated', 'rejected', 'needs_review')),
    extracted_data jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_rating_receipts_rating_id
on public.rating_receipts (rating_id);

create index if not exists idx_rating_receipts_user_created
on public.rating_receipts (user_id, created_at desc);

alter table public.rating_receipts enable row level security;

drop policy if exists rating_receipts_select_own on public.rating_receipts;
drop policy if exists rating_receipts_insert_own on public.rating_receipts;

create policy rating_receipts_select_own
on public.rating_receipts
for select
to authenticated
using (((select auth.uid())::text = user_id));

create policy rating_receipts_insert_own
on public.rating_receipts
for insert
to authenticated
with check (((select auth.uid())::text = user_id));

alter table public.stories
    add column if not exists dish_name text,
    add column if not exists rating numeric,
    add column if not exists city text;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'receipt-images',
    'receipt-images',
    false,
    10485760,
    array['image/jpeg', 'image/png', 'image/webp', 'image/heic']
)
on conflict (id) do update set
    name = excluded.name,
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "upload own to receipt-images" on storage.objects;
drop policy if exists "read own receipt-images" on storage.objects;
drop policy if exists "delete own receipt-images" on storage.objects;

create policy "upload own to receipt-images"
on storage.objects
for insert
to authenticated
with check (
    bucket_id = 'receipt-images'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

create policy "read own receipt-images"
on storage.objects
for select
to authenticated
using (
    bucket_id = 'receipt-images'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

create policy "delete own receipt-images"
on storage.objects
for delete
to authenticated
using (
    bucket_id = 'receipt-images'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);
