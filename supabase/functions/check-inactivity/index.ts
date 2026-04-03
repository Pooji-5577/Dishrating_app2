import { createClient } from 'npm:@supabase/supabase-js@2'

console.log('SmackCheck Inactivity Check Function initialized')

const supabase = createClient(
  Deno.env.get('SUPABASE_URL')!,
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
)

const INACTIVITY_HOURS = 8

Deno.serve(async (req) => {
  try {
    // 1. Find users who haven't rated in 8+ hours and have a push token
    const cutoff = new Date(Date.now() - INACTIVITY_HOURS * 60 * 60 * 1000).toISOString()

    const { data: inactiveUsers, error: usersError } = await supabase
      .from('profiles')
      .select('id, name, push_token, last_rating_at')
      .not('push_token', 'is', null)
      .not('push_token', 'eq', '')
      .or(`last_rating_at.lt.${cutoff},last_rating_at.is.null`)

    if (usersError) {
      console.error('Error fetching inactive users:', usersError.message)
      return new Response(
        JSON.stringify({ error: 'Failed to fetch inactive users', details: usersError.message }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      )
    }

    if (!inactiveUsers || inactiveUsers.length === 0) {
      return new Response(
        JSON.stringify({ message: 'No inactive users found', count: 0 }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      )
    }

    console.log(`Found ${inactiveUsers.length} inactive users`)

    // 2. Filter out users who already received an inactivity reminder in the last 8 hours
    const reminderCutoff = new Date(Date.now() - INACTIVITY_HOURS * 60 * 60 * 1000).toISOString()
    const userIds = inactiveUsers.map((u: { id: string }) => u.id)

    const { data: recentReminders, error: remindersError } = await supabase
      .from('notifications')
      .select('user_id')
      .in('user_id', userIds)
      .eq('event_type', 'inactivity_reminder')
      .gte('created_at', reminderCutoff)

    if (remindersError) {
      console.error('Error checking recent reminders:', remindersError.message)
    }

    const alreadyReminded = new Set(
      (recentReminders || []).map((r: { user_id: string }) => r.user_id)
    )

    const eligibleUsers = inactiveUsers.filter(
      (u: { id: string }) => !alreadyReminded.has(u.id)
    )

    console.log(`${eligibleUsers.length} users eligible for inactivity reminder (${alreadyReminded.size} already reminded)`)

    // 3. Insert inactivity reminder notifications
    let sentCount = 0
    for (const user of eligibleUsers) {
      try {
        const { error: insertError } = await supabase
          .from('notifications')
          .insert({
            user_id: user.id,
            title: 'We miss you!',
            body: `Hey ${user.name || 'foodie'}! Share a dish review today and keep your streak alive!`,
            event_type: 'inactivity_reminder',
            data: {
              source_id: `inactivity_${user.id}_${Date.now()}`,
              screen: 'DishCapture',
            },
          })

        if (insertError) {
          // Skip duplicates silently
          if (insertError.code === '23505') {
            console.log(`Duplicate inactivity reminder skipped for user ${user.id}`)
          } else {
            console.error(`Failed to insert notification for user ${user.id}:`, insertError.message)
          }
        } else {
          sentCount++
        }
      } catch (e) {
        console.error(`Error sending inactivity reminder to ${user.id}:`, (e as Error).message)
      }
    }

    return new Response(
      JSON.stringify({
        message: 'Inactivity check complete',
        totalInactive: inactiveUsers.length,
        alreadyReminded: alreadyReminded.size,
        eligible: eligibleUsers.length,
        sent: sentCount,
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    )
  } catch (error) {
    console.error('Unhandled error in check-inactivity:', error)
    return new Response(
      JSON.stringify({ error: 'Internal server error', message: (error as Error).message }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    )
  }
})
