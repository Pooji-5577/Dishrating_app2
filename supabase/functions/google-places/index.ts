import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const GOOGLE_PLACES_API_KEY = Deno.env.get('GOOGLE_PLACES_API_KEY')

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const { restaurantName, city, placeId } = await req.json()

    let photos: string[] = []

    if (placeId) {
      // ── Fetch photos using Place ID directly ──
      photos = await getPhotosByPlaceId(placeId)
    } else if (restaurantName) {
      // ── Step 1: Search for the place by name + city ──
      const searchQuery = `${restaurantName} ${city || ''} restaurant`
      const searchUrl = `https://maps.googleapis.com/maps/api/place/findplacefromtext/json?` +
        `input=${encodeURIComponent(searchQuery)}` +
        `&inputtype=textquery` +
        `&fields=place_id,name,photos,rating,formatted_address` +
        `&key=${GOOGLE_PLACES_API_KEY}`

      const searchResponse = await fetch(searchUrl)
      const searchData = await searchResponse.json()

      const candidate = searchData.candidates?.[0]
      if (!candidate) {
        return new Response(
          JSON.stringify({ photos: [], error: 'Place not found' }),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        )
      }

      // ── Step 2: Get photos using place_id ──
      photos = await getPhotosByPlaceId(candidate.place_id)
    }

    return new Response(
      JSON.stringify({ photos }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 200
      }
    )

  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message, photos: [] }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 500
      }
    )
  }
})

/**
 * Get photo URLs from a Google Place ID.
 * Fetches place details, extracts photo references,
 * and builds direct image URLs.
 */
async function getPhotosByPlaceId(placeId: string): Promise<string[]> {
  // Get place details with photo references
  const detailsUrl = `https://maps.googleapis.com/maps/api/place/details/json?` +
    `place_id=${placeId}` +
    `&fields=photos` +
    `&key=${GOOGLE_PLACES_API_KEY}`

  const detailsResponse = await fetch(detailsUrl)
  const detailsData = await detailsResponse.json()

  // Get up to 5 photo references
  const photoRefs = detailsData.result?.photos?.slice(0, 5) || []

  // Convert photo references to direct image URLs
  const photoUrls = photoRefs.map((photo: any) =>
    `https://maps.googleapis.com/maps/api/place/photo?` +
    `maxwidth=800` +
    `&photo_reference=${photo.photo_reference}` +
    `&key=${GOOGLE_PLACES_API_KEY}`
  )

  return photoUrls
}
