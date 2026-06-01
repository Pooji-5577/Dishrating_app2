-- RLS policies and public feed RPCs call private helper functions.
-- Grant schema usage without exposing private functions through the Data API.

grant usage on schema private to anon, authenticated;
