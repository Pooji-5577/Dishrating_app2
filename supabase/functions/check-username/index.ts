import "@supabase/functions-js/edge-runtime.d.ts"
import { createClient } from 'npm:@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

const USERNAME_REGEX = /^[a-zA-Z0-9_]{3,20}$/

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  if (req.method !== 'POST') {
    return new Response(
      JSON.stringify({ available: false, error: 'Method not allowed.' }),
      { status: 405, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  let username: string
  try {
    const body = await req.json()
    username = (body?.username ?? '').trim()
  } catch {
    return new Response(
      JSON.stringify({ available: false, error: 'Invalid request body.' }),
      { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  // Format validation
  if (!USERNAME_REGEX.test(username)) {
    const error = username.length === 0
      ? 'Username is required.'
      : username.length < 3
        ? 'Username must be at least 3 characters.'
        : username.length > 20
          ? 'Username must be 20 characters or fewer.'
          : 'Username can only contain letters, numbers, and underscores.'
    return new Response(
      JSON.stringify({ available: false, error }),
      { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  // Use service role key to bypass RLS — this check runs before the user exists
  const supabase = createClient(
    Deno.env.get('SUPABASE_URL') ?? '',
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
  )

  const { data, error: dbError } = await supabase
    .from('profiles')
    .select('id')
    .ilike('username', username)
    .maybeSingle()

  if (dbError) {
    console.error('check-username DB error:', dbError.message)
    return new Response(
      JSON.stringify({ available: false, error: 'Could not check username availability. Please try again.' }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  if (data) {
    return new Response(
      JSON.stringify({ available: false, error: `Username '${username}' is already taken.` }),
      { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  return new Response(
    JSON.stringify({ available: true }),
    { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
  )
})
