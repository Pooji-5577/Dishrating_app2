import { createClient } from 'npm:@supabase/supabase-js@2'

console.log('SmackCheck Push Notification Function initialized')

// ─── Types ───────────────────────────────────────────────────────

interface Notification {
  id: string
  user_id: string
  title: string
  body: string
  data: Record<string, unknown>
  event_type: string
}

interface WebhookPayload {
  type: 'INSERT' | 'UPDATE' | 'DELETE'
  table: string
  record: Notification
  schema: 'public'
  old_record: null | Notification
}

interface ExpoPushMessage {
  to: string
  sound: 'default' | null
  title: string
  body: string
  data?: Record<string, unknown>
  channelId?: string
  priority?: 'default' | 'normal' | 'high'
  badge?: number
}

interface ExpoPushTicket {
  status: 'ok' | 'error'
  id?: string
  message?: string
  details?: { error?: string }
}

// ─── Supabase Client ─────────────────────────────────────────────

const supabase = createClient(
  Deno.env.get('SUPABASE_URL')!,
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
)

// ─── Channel ID mapping for Android ─────────────────────────────

const EVENT_CHANNEL_MAP: Record<string, string> = {
  review_liked: 'social',
  dish_comment: 'social',
  points_earned: 'gamification',
  challenge_completed: 'gamification',
  trending_dish: 'discovery',
}

// ─── Main Handler ────────────────────────────────────────────────

Deno.serve(async (req) => {
  try {
    // Parse the webhook payload
    const payload: WebhookPayload = await req.json()

    // Only process INSERT events on the notifications table
    if (payload.type !== 'INSERT' || payload.table !== 'notifications') {
      return new Response(
        JSON.stringify({ message: 'Ignored: not an INSERT on notifications' }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      )
    }

    const { record } = payload

    // Validate required fields
    if (!record.user_id || !record.body) {
      return new Response(
        JSON.stringify({ error: 'Missing user_id or body in notification record' }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Fetch user's Expo push token
    const { data: profile, error: profileError } = await supabase
      .from('profiles')
      .select('expo_push_token')
      .eq('id', record.user_id)
      .single()

    if (profileError) {
      console.error('Error fetching profile:', profileError.message)
      return new Response(
        JSON.stringify({ error: 'Failed to fetch user profile', details: profileError.message }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      )
    }

    const pushToken = profile?.expo_push_token

    // Validate push token exists and is a valid Expo token
    if (!pushToken) {
      console.warn(`No push token for user ${record.user_id}, skipping notification`)
      return new Response(
        JSON.stringify({ message: 'No push token registered for user', user_id: record.user_id }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      )
    }

    if (!isValidExpoPushToken(pushToken)) {
      console.error(`Invalid Expo push token for user ${record.user_id}: ${pushToken}`)
      return new Response(
        JSON.stringify({ error: 'Invalid Expo push token format' }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Build the push message
    const message: ExpoPushMessage = {
      to: pushToken,
      sound: 'default',
      title: record.title || 'SmackCheck',
      body: record.body,
      data: {
        ...record.data,
        notificationId: record.id,
        eventType: record.event_type,
      },
      channelId: EVENT_CHANNEL_MAP[record.event_type] || 'default',
      priority: 'high',
    }

    // Send push notification via Expo Push API
    const ticket = await sendExpoPushNotification(message)

    console.log(`Push notification sent for user ${record.user_id}:`, ticket)

    return new Response(
      JSON.stringify({
        success: true,
        ticket,
        user_id: record.user_id,
        event_type: record.event_type,
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    )
  } catch (error) {
    console.error('Unhandled error in push function:', error)
    return new Response(
      JSON.stringify({ error: 'Internal server error', message: (error as Error).message }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    )
  }
})

// ─── Helpers ─────────────────────────────────────────────────────

function isValidExpoPushToken(token: string): boolean {
  return (
    typeof token === 'string' &&
    (token.startsWith('ExponentPushToken[') || token.startsWith('ExpoPushToken[')) &&
    token.endsWith(']')
  )
}

async function sendExpoPushNotification(message: ExpoPushMessage): Promise<ExpoPushTicket> {
  const accessToken = Deno.env.get('EXPO_ACCESS_TOKEN')

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    'Accept-Encoding': 'gzip, deflate',
  }

  // Use enhanced security if access token is available
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`
  }

  const response = await fetch('https://exp.host/--/api/v2/push/send', {
    method: 'POST',
    headers,
    body: JSON.stringify(message),
  })

  const result = await response.json()

  if (!response.ok) {
    console.error('Expo Push API error:', result)
    throw new Error(`Expo Push API returned ${response.status}: ${JSON.stringify(result)}`)
  }

  // Expo returns { data: { ... } } for single messages
  const ticket: ExpoPushTicket = result.data || result

  if (ticket.status === 'error') {
    console.error('Push ticket error:', ticket.message, ticket.details)

    // If the token is invalid, remove it from the profile to prevent future errors
    if (ticket.details?.error === 'DeviceNotRegistered') {
      console.warn(`Removing invalid push token for message to: ${message.to}`)
      await supabase
        .from('profiles')
        .update({ expo_push_token: null })
        .eq('expo_push_token', message.to)
    }
  }

  return ticket
}
