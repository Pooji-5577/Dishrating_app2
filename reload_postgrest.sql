-- Force PostgREST to reload schema after migration
-- Run this AFTER the migration completes successfully

-- Terminate all PostgREST connections
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE application_name LIKE '%postgrest%'
  AND pid <> pg_backend_pid();

-- Send reload notifications
SELECT pg_notify('pgrst', 'reload schema');
SELECT pg_notify('pgrst', 'reload config');

-- Verify notification was sent
SELECT 'PostgREST reload notifications sent successfully' AS status;
