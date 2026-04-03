import { createClient } from 'npm:@supabase/supabase-js@2'

console.log('SmackCheck Admin Send Notification Function initialized')

const supabase = createClient(
  Deno.env.get('SUPABASE_URL')!,
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
)

interface SendNotificationRequest {
  targetUserId?: string  // If omitted, sends to all users
  title: string
  body: string
}

Deno.serve(async (req) => {
  try {
    // 1. Verify the caller is an admin
    const authHeader = req.headers.get('Authorization')
    if (!authHeader) {
      return new Response(
        JSON.stringify({ error: 'Missing authorization header' }),
        { status: 401, headers: { 'Content-Type': 'application/json' } }
      )
    }

    const token = authHeader.replace('Bearer ', '')
    const { data: { user }, error: authError } = await supabase.auth.getUser(token)

    if (authError || !user) {
      return new Response(
        JSON.stringify({ error: 'Invalid or expired token' }),
        { status: 401, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Check if the user is an admin
    const { data: profile, error: profileError } = await supabase
      .from('profiles')
      .select('is_admin')
      .eq('id', user.id)
      .single()

    if (profileError || !profile?.is_admin) {
      return new Response(
        JSON.stringify({ error: 'Unauthorized: admin access required' }),
        { status: 403, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // 2. Parse the request
    const { targetUserId, title, body }: SendNotificationRequest = await req.json()

    if (!title || !body) {
      return new Response(
        JSON.stringify({ error: 'Missing required fields: title, body' }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    let sentCount = 0
    let targetUsers: { id: string }[] = []

    if (targetUserId) {
      // 3a. Send to a specific user
      targetUsers = [{ id: targetUserId }]
    } else {
      // 3b. Send to all users with push tokens
      const { data: allUsers, error: usersError } = await supabase
        .from('profiles')
        .select('id')
        .not('push_token', 'is', null)
        .not('push_token', 'eq', '')

      if (usersError) {
        return new Response(
          JSON.stringify({ error: 'Failed to fetch users', details: usersError.message }),
          { status: 500, headers: { 'Content-Type': 'application/json' } }
        )
      }

      targetUsers = allUsers || []
    }

    console.log(`Sending admin notification to ${targetUsers.length} user(s): "${title}"`)

    // 4. Insert notifications for each target user
    for (const targetUser of targetUsers) {
      try {
        const { error: insertError } = await supabase
          .from('notifications')
          .insert({
            user_id: targetUser.id,
            title,
            body,
            event_type: 'admin_broadcast',
            data: {
              source_id: `admin_${user.id}_${Date.now()}`,
              screen: 'Home',
              sent_by: user.id,
            },
          })

        if (insertError) {
          if (insertError.code === '23505') {
            console.log(`Duplicate notification skipped for user ${targetUser.id}`)
          } else {
            console.error(`Failed to insert notification for ${targetUser.id}:`, insertError.message)
          }
        } else {
          sentCount++
        }
      } catch (e) {
        console.error(`Error sending to ${targetUser.id}:`, (e as Error).message)
      }
    }

    return new Response(
      JSON.stringify({
        success: true,
        message: `Notification sent to ${sentCount} user(s)`,
        totalTargets: targetUsers.length,
        sent: sentCount,
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    )
  } catch (error) {
    console.error('Unhandled error in admin-send-notification:', error)
    return new Response(
      JSON.stringify({ error: 'Internal server error', message: (error as Error).message }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    )
  }
})
