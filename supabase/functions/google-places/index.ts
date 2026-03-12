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

interface GeocodeCityRequest {
  action: 'geocode-city'
  cityName: string
}

interface SearchPhotosRequest {
  action?: 'search-photos'  // Optional for backward compatibility
  restaurantName: string
  city: string
  placeId?: string | null
}

type PlacesRequest = NearbySearchRequest | PlaceDetailsRequest | GeocodeCityRequest | SearchPhotosRequest

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

interface SearchPhotosResponse {
  photos: string[]
  error?: string
}

interface GeocodeCityResponse {
  latitude: number | null
  longitude: number | null
  formattedAddress: string | null
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
    } else if (body.action === 'geocode-city') {
      return await handleGeocodeCity(body as GeocodeCityRequest, GOOGLE_PLACES_API_KEY)
    } else if (body.action === 'search-photos' || 'restaurantName' in body) {
      // Support both explicit action and legacy format (no action, just restaurantName/city)
      return await handleSearchPhotos(body as SearchPhotosRequest, GOOGLE_PLACES_API_KEY)
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

/**
 * Search for a restaurant by name/city and return photo URLs.
 * If placeId is provided, uses Place Details directly.
 * Otherwise, searches by text query first.
 */
async function handleSearchPhotos(
  body: SearchPhotosRequest,
  apiKey: string
): Promise<Response> {
  const { restaurantName, city, placeId } = body

  console.log(`Search photos: name="${restaurantName}", city="${city}", placeId=${placeId ?? 'none'}`)

  try {
    let targetPlaceId = placeId

    // If no placeId provided, search for the restaurant first
    if (!targetPlaceId && restaurantName) {
      const query = city ? `${restaurantName} ${city}` : restaurantName
      const searchUrl = `https://maps.googleapis.com/maps/api/place/textsearch/json` +
        `?query=${encodeURIComponent(query)}` +
        `&type=restaurant` +
        `&key=${apiKey}`

      const searchResponse = await fetch(searchUrl)
      if (!searchResponse.ok) {
        throw new Error(`Google Places search error: ${searchResponse.status}`)
      }

      const searchData = await searchResponse.json()
      if (searchData.status !== 'OK' || !searchData.results?.length) {
        console.log(`No results found for query: ${query}`)
        return new Response(
          JSON.stringify({ photos: [] } as SearchPhotosResponse),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        )
      }

      targetPlaceId = searchData.results[0].place_id
      console.log(`Found place: ${searchData.results[0].name} (${targetPlaceId})`)
    }

    if (!targetPlaceId) {
      return new Response(
        JSON.stringify({ photos: [], error: 'No placeId or restaurantName provided' } as SearchPhotosResponse),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Get place details to retrieve photo references
    const detailsUrl = `https://maps.googleapis.com/maps/api/place/details/json` +
      `?place_id=${encodeURIComponent(targetPlaceId)}` +
      `&fields=photos` +
      `&key=${apiKey}`

    const detailsResponse = await fetch(detailsUrl)
    if (!detailsResponse.ok) {
      throw new Error(`Google Places details error: ${detailsResponse.status}`)
    }

    const detailsData = await detailsResponse.json()
    if (detailsData.status !== 'OK' || !detailsData.result?.photos?.length) {
      console.log(`No photos found for placeId: ${targetPlaceId}`)
      return new Response(
        JSON.stringify({ photos: [] } as SearchPhotosResponse),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Convert photo references to URLs (max 5 photos)
    const photoUrls: string[] = detailsData.result.photos
      .slice(0, 5)
      .map((photo: { photo_reference: string }) =>
        `https://maps.googleapis.com/maps/api/place/photo` +
        `?maxwidth=800` +
        `&photo_reference=${photo.photo_reference}` +
        `&key=${apiKey}`
      )

    console.log(`Returning ${photoUrls.length} photo URLs`)

    return new Response(
      JSON.stringify({ photos: photoUrls } as SearchPhotosResponse),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )

  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error'
    console.error(`Search photos error: ${errorMessage}`)
    return new Response(
      JSON.stringify({ photos: [], error: errorMessage } as SearchPhotosResponse),
      {
        status: 400,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      }
    )
  }
}

/**
 * Geocode a city name to get its coordinates using Google Places Text Search.
 * This is more reliable than device geocoders for smaller cities.
 */
async function handleGeocodeCity(
  body: GeocodeCityRequest,
  apiKey: string
): Promise<Response> {
  const { cityName } = body

  console.log(`Geocoding city: "${cityName}"`)

  try {
    // Use Text Search API which works better for cities/localities
    const searchUrl = `https://maps.googleapis.com/maps/api/place/textsearch/json` +
      `?query=${encodeURIComponent(cityName)}` +
      `&type=locality` +
      `&key=${apiKey}`

    const searchResponse = await fetch(searchUrl)
    if (!searchResponse.ok) {
      throw new Error(`Google Places API error: ${searchResponse.status}`)
    }

    const searchData = await searchResponse.json()

    // If no results with locality type, try without type restriction
    if (searchData.status === 'ZERO_RESULTS' || !searchData.results?.length) {
      console.log(`No locality results for "${cityName}", trying general search`)
      
      const generalUrl = `https://maps.googleapis.com/maps/api/place/textsearch/json` +
        `?query=${encodeURIComponent(cityName)}` +
        `&key=${apiKey}`

      const generalResponse = await fetch(generalUrl)
      if (!generalResponse.ok) {
        throw new Error(`Google Places API error: ${generalResponse.status}`)
      }

      const generalData = await generalResponse.json()
      
      if (generalData.status === 'ZERO_RESULTS' || !generalData.results?.length) {
        console.log(`No results found for city: ${cityName}`)
        return new Response(
          JSON.stringify({ 
            latitude: null, 
            longitude: null, 
            formattedAddress: null 
          } as GeocodeCityResponse),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        )
      }

      const place = generalData.results[0]
      console.log(`Geocoded "${cityName}" to: ${place.formatted_address} (${place.geometry.location.lat}, ${place.geometry.location.lng})`)

      return new Response(
        JSON.stringify({
          latitude: place.geometry.location.lat,
          longitude: place.geometry.location.lng,
          formattedAddress: place.formatted_address
        } as GeocodeCityResponse),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    const place = searchData.results[0]
    console.log(`Geocoded "${cityName}" to: ${place.formatted_address} (${place.geometry.location.lat}, ${place.geometry.location.lng})`)

    return new Response(
      JSON.stringify({
        latitude: place.geometry.location.lat,
        longitude: place.geometry.location.lng,
        formattedAddress: place.formatted_address
      } as GeocodeCityResponse),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )

  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error'
    console.error(`Geocode city error: ${errorMessage}`)
    return new Response(
      JSON.stringify({ 
        latitude: null, 
        longitude: null, 
        formattedAddress: null, 
        error: errorMessage 
      } as GeocodeCityResponse),
      {
        status: 400,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      }
    )
  }
}
