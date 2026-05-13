import { NextRequest, NextResponse } from 'next/server'
import { requireAdmin } from '@/lib/adminAuth'
import { createServiceClient } from '@/lib/supabase'

export async function POST(req: NextRequest) {
  try {
    const auth = await requireAdmin(req)
    if (!auth.ok) {
      return NextResponse.json({ error: auth.error }, { status: auth.status })
    }

    const { targetUserId, title, body } = await req.json()

    if (!title || !body) {
      return NextResponse.json(
        { error: 'Missing required fields: title, body' },
        { status: 400 }
      )
    }

    const supabase = createServiceClient()

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
              source_id: `admin_${auth.adminUserId}_${Date.now()}_${user.id}`,
              screen: 'Home',
              sent_by: auth.adminUserId,
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
