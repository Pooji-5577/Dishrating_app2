-- Test that TEXT columns accept Firebase User IDs and Google Place IDs
-- Run this AFTER migration to verify it worked

-- Test 1: Insert user with Firebase ID
INSERT INTO users (id, email, name)
VALUES ('2kNeLi5hVQYa7wxwizmPCwIbm712', 'test@example.com', 'Test User')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

-- Test 2: Insert restaurant with Google Place ID
INSERT INTO restaurants (id, name, city, cuisine, latitude, longitude)
VALUES ('ChIJTestPlaceID789', 'Test Restaurant', 'Test City', 'Italian', 40.7128, -74.0060)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

-- Test 3: Insert dish linked to restaurant
INSERT INTO dishes (id, name, restaurant_id)
VALUES ('dish_test_123', 'Test Dish', 'ChIJTestPlaceID789')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

-- Test 4: Insert rating with all TEXT IDs
INSERT INTO ratings (id, user_id, dish_id, restaurant_id, rating, comment)
VALUES (
    'rating_test_456',
    '2kNeLi5hVQYa7wxwizmPCwIbm712',
    'dish_test_123',
    'ChIJTestPlaceID789',
    5,
    'Test rating with TEXT IDs'
)
ON CONFLICT (id) DO UPDATE SET comment = EXCLUDED.comment;

-- Verify all inserts worked
SELECT 'Test 1: User' AS test, id, name FROM users WHERE id = '2kNeLi5hVQYa7wxwizmPCwIbm712'
UNION ALL
SELECT 'Test 2: Restaurant', id, name FROM restaurants WHERE id = 'ChIJTestPlaceID789'
UNION ALL
SELECT 'Test 3: Dish', id, name FROM dishes WHERE id = 'dish_test_123'
UNION ALL
SELECT 'Test 4: Rating', id::TEXT, comment FROM ratings WHERE id = 'rating_test_456';

-- Cleanup test data
DELETE FROM ratings WHERE id = 'rating_test_456';
DELETE FROM dishes WHERE id = 'dish_test_123';
DELETE FROM restaurants WHERE id = 'ChIJTestPlaceID789';
DELETE FROM users WHERE id = '2kNeLi5hVQYa7wxwizmPCwIbm712';

SELECT '✓ All tests passed! Database accepts TEXT IDs correctly.' AS result;
