-- Grouped dish reviews: one visible feed post with multiple rated dishes.

ALTER TABLE public.ratings ADD COLUMN IF NOT EXISTS group_id TEXT;

CREATE TABLE IF NOT EXISTS public.review_groups (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id TEXT NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    restaurant_id TEXT NOT NULL REFERENCES public.restaurants(id) ON DELETE CASCADE,
    rating DOUBLE PRECISION NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT DEFAULT '',
    tags TEXT[] DEFAULT '{}',
    receipt_image_url TEXT,
    receipt_extracted_data TEXT,
    primary_rating_id TEXT REFERENCES public.ratings(id) ON DELETE SET NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.review_group_items (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    group_id TEXT NOT NULL REFERENCES public.review_groups(id) ON DELETE CASCADE,
    rating_id TEXT NOT NULL REFERENCES public.ratings(id) ON DELETE CASCADE,
    dish_id TEXT NOT NULL REFERENCES public.dishes(id) ON DELETE CASCADE,
    dish_name TEXT NOT NULL,
    image_url TEXT,
    price DOUBLE PRECISION,
    sort_order INTEGER DEFAULT 0,
    ai_confidence DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ratings_group_id ON public.ratings(group_id);
CREATE INDEX IF NOT EXISTS idx_review_groups_user_created ON public.review_groups(user_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_review_groups_restaurant_created ON public.review_groups(restaurant_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_review_group_items_group_sort ON public.review_group_items(group_id, sort_order);

ALTER TABLE public.review_groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.review_group_items ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON public.review_groups TO anon;
GRANT SELECT, INSERT, UPDATE ON public.review_groups TO authenticated;
GRANT SELECT ON public.review_group_items TO anon;
GRANT SELECT, INSERT ON public.review_group_items TO authenticated;

DROP POLICY IF EXISTS "review_groups_select" ON public.review_groups;
DROP POLICY IF EXISTS "review_groups_insert" ON public.review_groups;
DROP POLICY IF EXISTS "review_groups_update" ON public.review_groups;
DROP POLICY IF EXISTS "review_group_items_select" ON public.review_group_items;
DROP POLICY IF EXISTS "review_group_items_insert" ON public.review_group_items;

CREATE POLICY "review_groups_select" ON public.review_groups FOR SELECT USING (true);
CREATE POLICY "review_groups_insert" ON public.review_groups FOR INSERT WITH CHECK (auth.uid()::text = user_id);
CREATE POLICY "review_groups_update" ON public.review_groups FOR UPDATE USING (auth.uid()::text = user_id);

CREATE POLICY "review_group_items_select" ON public.review_group_items FOR SELECT USING (true);
CREATE POLICY "review_group_items_insert" ON public.review_group_items FOR INSERT WITH CHECK (
    auth.uid()::text = (
        SELECT user_id FROM public.review_groups WHERE id = group_id
    )
);

DROP FUNCTION IF EXISTS public.get_feed_page(
    text,
    integer,
    timestamptz,
    text,
    double precision,
    double precision,
    double precision,
    text,
    double precision,
    text
);

CREATE OR REPLACE FUNCTION public.get_feed_page(
    p_filter text DEFAULT 'FOLLOWING',
    p_limit integer DEFAULT 20,
    p_cursor_created_at timestamptz DEFAULT NULL,
    p_cursor_id text DEFAULT NULL,
    p_cursor_rating double precision DEFAULT NULL,
    p_user_lat double precision DEFAULT NULL,
    p_user_lon double precision DEFAULT NULL,
    p_user_city text DEFAULT NULL,
    p_radius_km double precision DEFAULT 25,
    p_current_user_id text DEFAULT NULL
)
RETURNS TABLE (
    id text,
    user_id text,
    user_profile_image_url text,
    user_name text,
    dish_image_url text,
    dish_name text,
    dish_id text,
    restaurant_name text,
    restaurant_city text,
    rating double precision,
    likes_count integer,
    comments_count integer,
    is_liked boolean,
    created_at timestamptz,
    comment text,
    image_urls text[],
    price double precision,
    currency_code text,
    is_grouped boolean,
    group_id text,
    grouped_dishes jsonb
)
LANGUAGE plpgsql
STABLE
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
    v_filter text := upper(coalesce(nullif(p_filter, ''), 'FOLLOWING'));
    v_limit integer := least(greatest(coalesce(p_limit, 20), 1), 50);
    v_user_id text := coalesce(auth.uid()::text, nullif(p_current_user_id, ''));
    v_user_city text := lower(nullif(trim(coalesce(p_user_city, '')), ''));
    v_user_point geography := CASE
        WHEN p_user_lat IS NOT NULL AND p_user_lon IS NOT NULL
            THEN ST_SetSRID(ST_MakePoint(p_user_lon, p_user_lat), 4326)::geography
        ELSE NULL
    END;
BEGIN
    RETURN QUERY
    WITH grouped AS (
        SELECT
            coalesce(g.primary_rating_id, g.id) AS row_id,
            g.user_id,
            p.profile_photo_url,
            coalesce(p.name, 'Unknown') AS profile_name,
            first_item.image_url AS row_image_url,
            CASE
                WHEN count_items.item_count > 1 THEN count_items.item_count::text || ' dishes'
                ELSE coalesce(first_item.dish_name, 'Unknown Dish')
            END AS row_dish_name,
            coalesce(first_item.dish_id, '') AS row_dish_id,
            coalesce(rest.name, 'Unknown Restaurant') AS row_restaurant_name,
            coalesce(rest.city, '') AS row_restaurant_city,
            g.rating,
            coalesce(r.likes_count, 0) AS row_likes_count,
            coalesce(cc.comments_count, 0)::integer AS row_comments_count,
            (v_user_id IS NOT NULL AND l.rating_id IS NOT NULL) AS row_is_liked,
            g.created_at,
            coalesce(g.comment, '') AS row_comment,
            coalesce(images.image_urls, '{}'::text[]) AS row_image_urls,
            NULL::double precision AS row_price,
            NULL::text AS row_currency_code,
            true AS row_is_grouped,
            g.id AS row_group_id,
            coalesce(items.grouped_dishes, '[]'::jsonb) AS row_grouped_dishes,
            g.restaurant_id,
            g.latitude,
            g.longitude
        FROM public.review_groups g
        LEFT JOIN public.profiles p ON p.id = g.user_id
        LEFT JOIN public.restaurants rest ON rest.id = g.restaurant_id
        LEFT JOIN public.ratings r ON r.id = g.primary_rating_id
        LEFT JOIN LATERAL (
            SELECT *
            FROM public.review_group_items i
            WHERE i.group_id = g.id
            ORDER BY i.sort_order, i.created_at
            LIMIT 1
        ) first_item ON true
        LEFT JOIN LATERAL (
            SELECT count(*)::integer AS item_count
            FROM public.review_group_items i
            WHERE i.group_id = g.id
        ) count_items ON true
        LEFT JOIN LATERAL (
            SELECT array_agg(i.image_url ORDER BY i.sort_order, i.created_at) FILTER (WHERE i.image_url IS NOT NULL) AS image_urls
            FROM public.review_group_items i
            WHERE i.group_id = g.id
        ) images ON true
        LEFT JOIN LATERAL (
            SELECT jsonb_agg(
                jsonb_build_object(
                    'dish_id', i.dish_id,
                    'dish_name', i.dish_name,
                    'image_url', i.image_url,
                    'price', i.price,
                    'currency_code', i.currency_code,
                    'rating_id', i.rating_id
                )
                ORDER BY i.sort_order, i.created_at
            ) AS grouped_dishes
            FROM public.review_group_items i
            WHERE i.group_id = g.id
        ) items ON true
        LEFT JOIN (
            SELECT rating_id, count(*)::integer AS comments_count
            FROM public.comments
            GROUP BY rating_id
        ) cc ON cc.rating_id = g.primary_rating_id
        LEFT JOIN public.likes l ON l.rating_id = g.primary_rating_id AND l.user_id = v_user_id
    ),
    standalone AS (
        SELECT
            r.id AS row_id,
            r.user_id,
            p.profile_photo_url,
            coalesce(p.name, 'Unknown') AS profile_name,
            coalesce(r.image_url, d.image_url) AS row_image_url,
            coalesce(d.name, 'Unknown Dish') AS row_dish_name,
            r.dish_id AS row_dish_id,
            coalesce(rest.name, 'Unknown Restaurant') AS row_restaurant_name,
            coalesce(rest.city, '') AS row_restaurant_city,
            r.rating::double precision,
            coalesce(r.likes_count, 0) AS row_likes_count,
            coalesce(cc.comments_count, 0)::integer AS row_comments_count,
            (v_user_id IS NOT NULL AND l.rating_id IS NOT NULL) AS row_is_liked,
            r.created_at,
            coalesce(r.comment, '') AS row_comment,
            array_remove(array_cat(ARRAY[coalesce(r.image_url, d.image_url)], coalesce(ri.image_urls, '{}'::text[])), NULL) AS row_image_urls,
            r.price::double precision AS row_price,
            r.currency_code AS row_currency_code,
            false AS row_is_grouped,
            NULL::text AS row_group_id,
            '[]'::jsonb AS row_grouped_dishes,
            r.restaurant_id,
            r.latitude,
            r.longitude
        FROM public.ratings r
        LEFT JOIN public.profiles p ON p.id = r.user_id
        LEFT JOIN public.dishes d ON d.id = r.dish_id
        LEFT JOIN public.restaurants rest ON rest.id = coalesce(nullif(r.restaurant_id, ''), d.restaurant_id)
        LEFT JOIN (
            SELECT rating_id, count(*)::integer AS comments_count
            FROM public.comments
            GROUP BY rating_id
        ) cc ON cc.rating_id = r.id
        LEFT JOIN (
            SELECT ri_src.rating_id, array_agg(ri_src.image_url ORDER BY ri_src.sort_order, ri_src.created_at) AS image_urls
            FROM public.rating_images ri_src
            GROUP BY ri_src.rating_id
        ) ri ON ri.rating_id = r.id
        LEFT JOIN public.likes l ON l.rating_id = r.id AND l.user_id = v_user_id
        WHERE r.group_id IS NULL
    ),
    all_rows AS (
        SELECT * FROM grouped
        UNION ALL
        SELECT * FROM standalone
    )
    SELECT
        row_id,
        all_rows.user_id,
        profile_photo_url,
        profile_name,
        row_image_url,
        row_dish_name,
        row_dish_id,
        row_restaurant_name,
        row_restaurant_city,
        all_rows.rating,
        row_likes_count,
        row_comments_count,
        row_is_liked,
        all_rows.created_at,
        row_comment,
        row_image_urls,
        row_price,
        row_currency_code,
        row_is_grouped,
        row_group_id,
        row_grouped_dishes
    FROM all_rows
    LEFT JOIN public.restaurants rest_filter ON rest_filter.id = all_rows.restaurant_id
    WHERE (
        (
            v_filter = 'FOLLOWING'
            AND v_user_id IS NOT NULL
            AND EXISTS (
                SELECT 1 FROM public.followers f
                WHERE f.follower_id = v_user_id
                  AND f.following_id = all_rows.user_id
            )
        )
        OR (v_filter = 'MY_RATINGS' AND v_user_id IS NOT NULL AND all_rows.user_id = v_user_id)
        OR (v_filter = 'TRENDING' AND all_rows.rating >= 4.0)
        OR (
            v_filter = 'NEARBY'
            AND (
                (
                    v_user_point IS NOT NULL
                    AND (
                        (all_rows.latitude IS NOT NULL AND all_rows.longitude IS NOT NULL AND ST_DWithin(ST_SetSRID(ST_MakePoint(all_rows.longitude, all_rows.latitude), 4326)::geography, v_user_point, p_radius_km * 1000))
                        OR (
                            rest_filter.latitude IS NOT NULL
                            AND rest_filter.longitude IS NOT NULL
                            AND ST_DWithin(ST_SetSRID(ST_MakePoint(rest_filter.longitude, rest_filter.latitude), 4326)::geography, v_user_point, p_radius_km * 1000)
                        )
                    )
                )
                OR (v_user_city IS NOT NULL AND lower(rest_filter.city) = v_user_city)
            )
        )
    )
    AND (
        p_cursor_created_at IS NULL
        OR (
            v_filter = 'TRENDING'
            AND (
                all_rows.rating < coalesce(p_cursor_rating, all_rows.rating)
                OR (
                    all_rows.rating = coalesce(p_cursor_rating, all_rows.rating)
                    AND (all_rows.created_at, row_id) < (p_cursor_created_at, coalesce(p_cursor_id, ''))
                )
            )
        )
        OR (
            v_filter <> 'TRENDING'
            AND (all_rows.created_at, row_id) < (p_cursor_created_at, coalesce(p_cursor_id, ''))
        )
    )
    ORDER BY
        CASE WHEN v_filter = 'TRENDING' THEN all_rows.rating END DESC NULLS LAST,
        all_rows.created_at DESC,
        row_id DESC
    LIMIT v_limit;
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_feed_page(
    text,
    integer,
    timestamptz,
    text,
    double precision,
    double precision,
    double precision,
    text,
    double precision,
    text
) TO anon, authenticated;
