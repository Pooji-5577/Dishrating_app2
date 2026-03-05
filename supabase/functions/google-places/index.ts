// Supabase Edge Function for Google Places API proxy
// Keeps the GOOGLE_PLACES_API_KEY server-side, away from client apps
import "@supabase/functions-js/edge-runtime.d.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

// --- Request types ---

interface NearbySearchRequest {
  action: 'nearby-search'
  latitude: number
  longitude: number
  radiusInMeters: number
  keyword?: string | null
}

interface PlaceDetailsRequest {
  action: 'place-details'
  placeId: string
}

type PlacesRequest = NearbySearchRequest | PlaceDetailsRequest

// --- Response types (matching client NearbyRestaurant model) ---

interface NearbyRestaurant {
  id: string
  name: string
  address: string | null
  latitude: number
  longitude: number
  rating: number | null
  userRatingsTotal: number | null
  priceLevel: number | null
  photoReference: string | null
  isOpen: boolean | null
}

interface PlacesResponse {
  results: NearbyRestaurant[]
  error?: string
}

interface PlaceDetailsResponse {
  result: NearbyRestaurant | null
  error?: string
}

// --- Google Places API response types ---

interface GooglePlaceResult {
  place_id: string
  name: string
  vicinity?: string
  formatted_address?: string
  geometry: {
    location: {
      lat: number
      lng: number
    }
  }
  rating?: number
  user_ratings_total?: number
  price_level?: number
  photos?: Array<{ photo_reference: string }>
  opening_hours?: { open_now?: boolean }
}

Deno.serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const GOOGLE_PLACES_API_KEY = Deno.env.get('GOOGLE_PLACES_API_KEY')
    if (!GOOGLE_PLACES_API_KEY) {
      throw new Error('GOOGLE_PLACES_API_KEY not configured on server')
    }

    const body: PlacesRequest = await req.json()

    if (body.action === 'nearby-search') {
      return await handleNearbySearch(body, GOOGLE_PLACES_API_KEY)
    } else if (body.action === 'place-details') {
      return await handlePlaceDetails(body, GOOGLE_PLACES_API_KEY)
    } else {
      throw new Error(`Unknown action: ${(body as Record<string, unknown>).action}`)
    }

  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error'
    console.error('Error in google-places function:', errorMessage)

    return new Response(
      JSON.stringify({ results: [], error: errorMessage }),
      {
        status: 400,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      }
    )
  }
})

async function handleNearbySearch(
  body: NearbySearchRequest,
  apiKey: string
): Promise<Response> {
  const { latitude, longitude, radiusInMeters, keyword } = body

  let url = `https://maps.googleapis.com/maps/api/place/nearbysearch/json` +
    `?location=${latitude},${longitude}` +
    `&radius=${radiusInMeters}` +
    `&type=restaurant` +
    `&key=${apiKey}`

  if (keyword) {
    url += `&keyword=${encodeURIComponent(keyword)}`
  }

  console.log(`Nearby search: lat=${latitude}, lng=${longitude}, radius=${radiusInMeters}, keyword=${keyword ?? 'none'}`)

  const googleResponse = await fetch(url)
  if (!googleResponse.ok) {
    const errorText = await googleResponse.text()
    console.error(`Google Places API error (${googleResponse.status}): ${errorText}`)
    throw new Error(`Google Places API error: ${googleResponse.status}`)
  }

  const data = await googleResponse.json()

  if (data.status !== 'OK' && data.status !== 'ZERO_RESULTS') {
    console.error(`Google Places API status: ${data.status}, error: ${data.error_message ?? 'none'}`)
    throw new Error(`Google Places API: ${data.error_message ?? data.status}`)
  }

  const results: NearbyRestaurant[] = (data.results ?? []).map((place: GooglePlaceResult) => ({
    id: place.place_id,
    name: place.name,
    address: place.vicinity ?? null,
    latitude: place.geometry.location.lat,
    longitude: place.geometry.location.lng,
    rating: place.rating ?? null,
    userRatingsTotal: place.user_ratings_total ?? null,
    priceLevel: place.price_level ?? null,
    photoReference: place.photos?.[0]?.photo_reference ?? null,
    isOpen: place.opening_hours?.open_now ?? null,
  }))

  console.log(`Nearby search returned ${results.length} restaurants`)

  const response: PlacesResponse = { results }
  return new Response(
    JSON.stringify(response),
    { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
  )
}

async function handlePlaceDetails(
  body: PlaceDetailsRequest,
  apiKey: string
): Promise<Response> {
  const { placeId } = body

  const url = `https://maps.googleapis.com/maps/api/place/details/json` +
    `?place_id=${encodeURIComponent(placeId)}` +
    `&fields=place_id,name,formatted_address,vicinity,geometry,rating,user_ratings_total,price_level,photos,opening_hours` +
    `&key=${apiKey}`

  console.log(`Place details for: ${placeId}`)

  const googleResponse = await fetch(url)
  if (!googleResponse.ok) {
    const errorText = await googleResponse.text()
    console.error(`Google Places API error (${googleResponse.status}): ${errorText}`)
    throw new Error(`Google Places API error: ${googleResponse.status}`)
  }

  const data = await googleResponse.json()

  if (data.status !== 'OK') {
    console.error(`Google Places API status: ${data.status}, error: ${data.error_message ?? 'none'}`)
    throw new Error(`Google Places API: ${data.error_message ?? data.status}`)
  }

  const place: GooglePlaceResult | undefined = data.result
  const result: NearbyRestaurant | null = place
    ? {
        id: place.place_id,
        name: place.name,
        address: place.formatted_address ?? place.vicinity ?? null,
        latitude: place.geometry.location.lat,
        longitude: place.geometry.location.lng,
        rating: place.rating ?? null,
        userRatingsTotal: place.user_ratings_total ?? null,
        priceLevel: place.price_level ?? null,
        photoReference: place.photos?.[0]?.photo_reference ?? null,
        isOpen: place.opening_hours?.open_now ?? null,
      }
    : null

  console.log(`Place details: ${result?.name ?? 'not found'}`)

  const response: PlaceDetailsResponse = { result }
  return new Response(
    JSON.stringify(response),
    { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
  )
}
