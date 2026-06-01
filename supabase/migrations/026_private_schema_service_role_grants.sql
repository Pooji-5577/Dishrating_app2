-- Backend feed/photo paths call RPCs with the service_role key.
-- Private helper functions referenced by SECURITY INVOKER RPCs must be
-- executable by service_role as well.

grant usage on schema private to anon, authenticated, service_role;
grant execute on all functions in schema private to anon, authenticated, service_role;

alter default privileges in schema private
grant execute on functions to anon, authenticated, service_role;
