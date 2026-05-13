-- Feed read model RPC for low-latency Home/Explore feeds.
-- Returns one pre-shaped page for every feed tab. The app remains responsible
-- for display-specific image transformation via ImageDelivery.

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
    price double precision
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
    SELECT
        r.id,
        r.user_id,
        p.profile_photo_url,
        coalesce(p.name, 'Unknown'),
        coalesce(r.image_url, d.image_url),
        coalesce(d.name, 'Unknown Dish'),
        r.dish_id,
        coalesce(rest.name, 'Unknown Restaurant'),
        coalesce(rest.city, ''),
        r.rating,
        coalesce(r.likes_count, 0),
        coalesce(cc.comments_count, 0)::integer,
        (v_user_id IS NOT NULL AND l.rating_id IS NOT NULL),
        r.created_at,
        coalesce(r.comment, ''),
        array_remove(
            array_cat(
                ARRAY[coalesce(r.image_url, d.image_url)],
                coalesce(ri.image_urls, '{}'::text[])
            ),
            NULL
        ),
        r.price::double precision
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
    WHERE (
        (
            v_filter = 'FOLLOWING'
            AND v_user_id IS NOT NULL
            AND EXISTS (
                SELECT 1
                FROM public.followers f
                WHERE f.follower_id = v_user_id
                  AND f.following_id = r.user_id
            )
        )
        OR (
            v_filter = 'MY_RATINGS'
            AND v_user_id IS NOT NULL
            AND r.user_id = v_user_id
        )
        OR (
            v_filter = 'TRENDING'
            AND r.rating >= 4.0
        )
        OR (
            v_filter = 'NEARBY'
            AND (
                (
                    v_user_point IS NOT NULL
                    AND (
                        (r.location IS NOT NULL AND ST_DWithin(r.location::geography, v_user_point, p_radius_km * 1000))
                        OR (
                            rest.latitude IS NOT NULL
                            AND rest.longitude IS NOT NULL
                            AND ST_DWithin(
                                ST_SetSRID(ST_MakePoint(rest.longitude, rest.latitude), 4326)::geography,
                                v_user_point,
                                p_radius_km * 1000
                            )
                        )
                    )
                )
                OR (
                    v_user_city IS NOT NULL
                    AND lower(rest.city) = v_user_city
                )
            )
        )
    )
    AND (
        p_cursor_created_at IS NULL
        OR (
            v_filter = 'TRENDING'
            AND (
                r.rating < coalesce(p_cursor_rating, r.rating)
                OR (
                    r.rating = coalesce(p_cursor_rating, r.rating)
                    AND (r.created_at, r.id) < (p_cursor_created_at, coalesce(p_cursor_id, ''))
                )
            )
        )
        OR (
            v_filter <> 'TRENDING'
            AND (r.created_at, r.id) < (p_cursor_created_at, coalesce(p_cursor_id, ''))
        )
    )
    ORDER BY
        CASE WHEN v_filter = 'TRENDING' THEN r.rating END DESC NULLS LAST,
        r.created_at DESC,
        r.id DESC
    LIMIT v_limit;
END;
$$;

CREATE INDEX IF NOT EXISTS idx_ratings_rating_created_id
    ON public.ratings (rating DESC, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_ratings_user_created_id
    ON public.ratings (user_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_ratings_restaurant_created_id
    ON public.ratings (restaurant_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_followers_follower_following
    ON public.followers (follower_id, following_id);

CREATE INDEX IF NOT EXISTS idx_comments_rating_id
    ON public.comments (rating_id);

CREATE INDEX IF NOT EXISTS idx_likes_user_rating
    ON public.likes (user_id, rating_id);

CREATE INDEX IF NOT EXISTS idx_rating_images_rating_sort
    ON public.rating_images (rating_id, sort_order);

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
