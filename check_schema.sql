-- Check current column types in all tables
SELECT
    table_name,
    column_name,
    data_type,
    udt_name
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('users', 'restaurants', 'dishes', 'ratings')
  AND column_name IN ('id', 'user_id', 'restaurant_id', 'dish_id')
ORDER BY table_name, column_name;
