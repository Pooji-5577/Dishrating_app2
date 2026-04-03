import { NextRequest, NextResponse } from 'next/server'
import { createServiceClient } from '@/lib/supabase'

export async function POST(req: NextRequest) {
  try {
    const { targetUserId, title, body, senderUserId } = await req.json()

    if (!title || !body || !senderUserId) {
      return NextResponse.json(
        { error: 'Missing required fields: title, body, senderUserId' },
        { status: 400 }
      )
    }

    const supabase = createServiceClient()

    // Verify the sender is an admin
    const { data: profile, error: profileError } = await supabase
      .from('profiles')
      .select('is_admin')
      .eq('id', senderUserId)
      .single()

    if (profileError || !profile?.is_admin) {
      return NextResponse.json(
        { error: 'Unauthorized: admin access required' },
        { status: 403 }
      )
    }

    let targetUsers: { id: string }[] = []

    if (targetUserId) {
      targetUsers = [{ id: targetUserId }]
    } else {
      // Send to all users
      const { data: allUsers, error: usersError } = await supabase
        .from('profiles')
        .select('id')

      if (usersError) {
        return NextResponse.json(
          { error: 'Failed to fetch users', details: usersError.message },
          { status: 500 }
        )
      }
      targetUsers = allUsers || []
    }

    let sentCount = 0

    for (const user of targetUsers) {
      try {
        const { error: insertError } = await supabase
          .from('notifications')
          .insert({
            user_id: user.id,
            title,
            body,
            event_type: 'admin_broadcast',
            data: {
              source_id: `admin_${senderUserId}_${Date.now()}_${user.id}`,
              screen: 'Home',
              sent_by: senderUserId,
            },
          })

        if (insertError) {
          if (insertError.code === '23505') {
            console.log(`Duplicate notification skipped for user ${user.id}`)
          } else {
            console.error(`Failed to insert notification for ${user.id}:`, insertError.message)
          }
        } else {
          sentCount++
        }
      } catch (e) {
        console.error(`Error sending to ${user.id}:`, (e as Error).message)
      }
    }

    return NextResponse.json({
      success: true,
      message: `Notification sent to ${sentCount} user(s)`,
      totalTargets: targetUsers.length,
      sent: sentCount,
    })
  } catch (error) {
    console.error('Send notification error:', error)
    return NextResponse.json(
      { error: 'Internal server error', message: (error as Error).message },
      { status: 500 }
    )
  }
}
