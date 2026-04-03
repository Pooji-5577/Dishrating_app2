import { createClient } from 'npm:@supabase/supabase-js@2'

console.log('SmackCheck Push Notification Function initialized (FCM + Expo)')

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

interface FCMMessage {
  message: {
    token: string
    notification: {
      title: string
      body: string
    }
    data?: Record<string, string>
    android?: {
      priority: 'high' | 'normal'
      notification?: {
        channel_id: string
        sound: string
        click_action: string
      }
    }
    apns?: {
      payload: {
        aps: {
          sound: string
          badge?: number
        }
      }
    }
  }
}

interface ExpoPushTicket {
  status: 'ok' | 'error'
  id?: string
  message?: string
  details?: { error?: string }
}

interface FCMResponse {
  name?: string
  error?: {
    code: number
    message: string
    status: string
    details?: unknown[]
  }
}

// ─── Supabase Client ─────────────────────────────────────────────

const supabase = createClient(
  Deno.env.get('SUPABASE_URL')!,
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
)

// ─── Channel ID mapping for Android ─────────────────────────────

const EVENT_CHANNEL_MAP: Record<string, string> = {
  review_liked: 'social',
  new_follower: 'social',
  dish_comment: 'social',
  comment_reply: 'social',
  points_earned: 'gamification',
  challenge_completed: 'gamification',
  level_up: 'gamification',
  badge_earned: 'gamification',
  trending_dish: 'discovery',
  nearby_restaurant: 'discovery',
  geofence_enter: 'discovery',
  welcome: 'default',
  first_dish: 'default',
  inactivity_reminder: 'default',
  admin_broadcast: 'default',
}

// ─── Token Type Detection ────────────────────────────────────────

type TokenType = 'expo' | 'fcm' | 'apns' | 'unknown'

function detectTokenType(token: string): TokenType {
  if (!token) return 'unknown'
  
  // Expo tokens start with ExponentPushToken[ or ExpoPushToken[
  if (token.startsWith('ExponentPushToken[') || token.startsWith('ExpoPushToken[')) {
    return 'expo'
  }
  
  // FCM tokens are typically long alphanumeric strings with colons
  // They're usually 152+ characters
  if (token.length > 100 && token.includes(':')) {
    return 'fcm'
  }
  
  // APNs device tokens are 64-character hex strings
  if (/^[a-fA-F0-9]{64}$/.test(token)) {
    return 'apns'
  }
  
  // Default to FCM for other long tokens (most common case)
  if (token.length > 100) {
    return 'fcm'
  }
  
  return 'unknown'
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

    // Fetch user's push token
    const { data: profile, error: profileError } = await supabase
      .from('profiles')
      .select('push_token')
      .eq('id', record.user_id)
      .single()

    if (profileError) {
      console.error('Error fetching profile:', profileError.message)
      return new Response(
        JSON.stringify({ error: 'Failed to fetch user profile', details: profileError.message }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Use the push_token column (FCM for Android, APNs for iOS)
    const pushToken: string | null = profile?.push_token || null
    const tokenType: TokenType = pushToken ? detectTokenType(pushToken) : 'unknown'

    if (!pushToken) {
      console.warn(`No push token for user ${record.user_id}, skipping notification`)
      return new Response(
        JSON.stringify({ message: 'No push token registered for user', user_id: record.user_id }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      )
    }

    console.log(`Sending ${tokenType} push notification for user ${record.user_id}`)

    let result: { success: boolean; ticket?: unknown; error?: string }

    // Send notification based on token type
    if (tokenType === 'fcm') {
      result = await sendFCMNotification(pushToken, record)
    } else if (tokenType === 'expo') {
      result = await sendExpoNotification(pushToken, record)
    } else {
      // Try FCM as default for unknown tokens (most common case)
      console.warn(`Unknown token type for user ${record.user_id}, trying FCM`)
      result = await sendFCMNotification(pushToken, record)
    }

    console.log(`Push notification result for user ${record.user_id}:`, result)

    return new Response(
      JSON.stringify({
        success: result.success,
        ticket: result.ticket,
        user_id: record.user_id,
        event_type: record.event_type,
        token_type: tokenType,
      }),
      { status: result.success ? 200 : 500, headers: { 'Content-Type': 'application/json' } }
    )
  } catch (error) {
    console.error('Unhandled error in push function:', error)
    return new Response(
      JSON.stringify({ error: 'Internal server error', message: (error as Error).message }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    )
  }
})

// ─── FCM Push Notification ───────────────────────────────────────

async function sendFCMNotification(
  token: string,
  notification: Notification
): Promise<{ success: boolean; ticket?: FCMResponse; error?: string }> {
  const projectId = Deno.env.get('FIREBASE_PROJECT_ID')
  const serviceAccountJson = Deno.env.get('FIREBASE_SERVICE_ACCOUNT_JSON')

  if (!projectId || !serviceAccountJson) {
    console.error('Firebase configuration missing')
    return {
      success: false,
      error: 'Firebase configuration missing (PROJECT_ID or SERVICE_ACCOUNT_JSON)',
    }
  }

  try {
    // Get OAuth2 access token for Firebase
    const accessToken = await getFirebaseAccessToken(serviceAccountJson)

    const channelId = EVENT_CHANNEL_MAP[notification.event_type] || 'default'

    const fcmMessage: FCMMessage = {
      message: {
        token: token,
        notification: {
          title: notification.title || 'SmackCheck',
          body: notification.body,
        },
        data: {
          notificationId: notification.id,
          eventType: notification.event_type,
          ...Object.fromEntries(
            Object.entries(notification.data || {}).map(([k, v]) => [k, String(v)])
          ),
        },
        android: {
          priority: 'high',
          notification: {
            channel_id: channelId,
            sound: 'default',
            click_action: 'FLUTTER_NOTIFICATION_CLICK',
          },
        },
        apns: {
          payload: {
            aps: {
              sound: 'default',
            },
          },
        },
      },
    }

    const response = await fetch(
      `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(fcmMessage),
      }
    )

    const result: FCMResponse = await response.json()

    if (!response.ok) {
      console.error('FCM API error:', result)
      
      // Handle invalid token errors
      if (result.error?.message?.includes('not found') ||
          result.error?.message?.includes('not registered')) {
        console.warn(`Removing invalid FCM token for user`)
        await supabase
          .from('profiles')
          .update({ push_token: null })
          .eq('push_token', token)
      }
      
      return {
        success: false,
        ticket: result,
        error: result.error?.message || 'FCM send failed',
      }
    }

    return { success: true, ticket: result }
  } catch (error) {
    console.error('FCM notification error:', error)
    return {
      success: false,
      error: (error as Error).message,
    }
  }
}

// ─── Firebase OAuth2 Access Token ────────────────────────────────

async function getFirebaseAccessToken(serviceAccountJson: string): Promise<string> {
  const serviceAccount = JSON.parse(serviceAccountJson)
  
  // Create JWT header and claim set
  const header = {
    alg: 'RS256',
    typ: 'JWT',
  }

  const now = Math.floor(Date.now() / 1000)
  const claim = {
    iss: serviceAccount.client_email,
    scope: 'https://www.googleapis.com/auth/firebase.messaging',
    aud: 'https://oauth2.googleapis.com/token',
    exp: now + 3600,
    iat: now,
  }

  // Encode and sign JWT
  const encodedHeader = base64UrlEncode(JSON.stringify(header))
  const encodedClaim = base64UrlEncode(JSON.stringify(claim))
  const signatureInput = `${encodedHeader}.${encodedClaim}`
  
  const signature = await signWithRS256(signatureInput, serviceAccount.private_key)
  const jwt = `${signatureInput}.${signature}`

  // Exchange JWT for access token
  const tokenResponse = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: jwt,
    }),
  })

  const tokenData = await tokenResponse.json()
  
  if (!tokenResponse.ok) {
    throw new Error(`Failed to get Firebase access token: ${JSON.stringify(tokenData)}`)
  }

  return tokenData.access_token
}

// ─── Crypto Helpers ──────────────────────────────────────────────

function base64UrlEncode(str: string): string {
  const base64 = btoa(str)
  return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function arrayBufferToBase64Url(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  for (const byte of bytes) {
    binary += String.fromCharCode(byte)
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

async function signWithRS256(data: string, privateKeyPem: string): Promise<string> {
  // Remove PEM headers and decode
  const pemContent = privateKeyPem
    .replace('-----BEGIN PRIVATE KEY-----', '')
    .replace('-----END PRIVATE KEY-----', '')
    .replace(/\n/g, '')
  
  const binaryKey = Uint8Array.from(atob(pemContent), c => c.charCodeAt(0))
  
  const cryptoKey = await crypto.subtle.importKey(
    'pkcs8',
    binaryKey,
    {
      name: 'RSASSA-PKCS1-v1_5',
      hash: 'SHA-256',
    },
    false,
    ['sign']
  )

  const encoder = new TextEncoder()
  const signature = await crypto.subtle.sign(
    'RSASSA-PKCS1-v1_5',
    cryptoKey,
    encoder.encode(data)
  )

  return arrayBufferToBase64Url(signature)
}

// ─── Expo Push Notification (Legacy/Fallback) ────────────────────

async function sendExpoNotification(
  token: string,
  notification: Notification
): Promise<{ success: boolean; ticket?: ExpoPushTicket; error?: string }> {
  const accessToken = Deno.env.get('EXPO_ACCESS_TOKEN')

  const message: ExpoPushMessage = {
    to: token,
    sound: 'default',
    title: notification.title || 'SmackCheck',
    body: notification.body,
    data: {
      ...notification.data,
      notificationId: notification.id,
      eventType: notification.event_type,
    },
    channelId: EVENT_CHANNEL_MAP[notification.event_type] || 'default',
    priority: 'high',
  }

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    'Accept-Encoding': 'gzip, deflate',
  }

  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`
  }

  try {
    const response = await fetch('https://exp.host/--/api/v2/push/send', {
      method: 'POST',
      headers,
      body: JSON.stringify(message),
    })

    const result = await response.json()

    if (!response.ok) {
      console.error('Expo Push API error:', result)
      return {
        success: false,
        error: `Expo Push API returned ${response.status}`,
      }
    }

    const ticket: ExpoPushTicket = result.data || result

    if (ticket.status === 'error') {
      console.error('Push ticket error:', ticket.message, ticket.details)

      // Remove invalid token
      if (ticket.details?.error === 'DeviceNotRegistered') {
        await supabase
          .from('profiles')
          .update({ push_token: null })
          .eq('push_token', token)
      }

      return {
        success: false,
        ticket,
        error: ticket.message,
      }
    }

    return { success: true, ticket }
  } catch (error) {
    console.error('Expo notification error:', error)
    return {
      success: false,
      error: (error as Error).message,
    }
  }
}
