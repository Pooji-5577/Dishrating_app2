alter table public.profiles
  add column if not exists profile_setup_completed boolean not null default false;

update public.profiles
set profile_setup_completed = true
where created_at < now()
  and profile_setup_completed = false;
