do $$
begin
  if (
    select data_type
    from information_schema.columns
    where table_schema = 'public'
      and table_name = 'user_badges'
      and column_name = 'id'
  ) = 'uuid' then
    alter table public.user_badges
      alter column id set default gen_random_uuid();
  else
    alter table public.user_badges
      alter column id set default gen_random_uuid()::text;
  end if;
end $$;
