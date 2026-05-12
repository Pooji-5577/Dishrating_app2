import { NextRequest, NextResponse } from 'next/server'
import { requireAdmin } from '@/lib/adminAuth'
import { createServiceClient } from '@/lib/supabase'

export async function GET(req: NextRequest) {
  try {
    const auth = await requireAdmin(req)
    if (!auth.ok) {
      return NextResponse.json({ error: auth.error }, { status: auth.status })
    }

    const supabase = createServiceClient()

    const [usersRes, ratingsRes, activeRes, newUsersRes, recentUsersRes] = await Promise.all([
      supabase.from('profiles').select('*', { count: 'exact', head: true }),
      supabase.from('ratings').select('*', { count: 'exact', head: true }),
      supabase.from('profiles').select('*', { count: 'exact', head: true })
        .gte('last_rating_at', new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString()),
      supabase.from('profiles').select('*', { count: 'exact', head: true })
        .gte('created_at', new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString()),
      supabase.from('profiles').select('id, name, username, email, created_at')
        .order('created_at', { ascending: false })
        .limit(5),
    ])

    return NextResponse.json({
      totalUsers: usersRes.count || 0,
      totalRatings: ratingsRes.count || 0,
      activeUsers24h: activeRes.count || 0,
      newUsers7d: newUsersRes.count || 0,
      recentUsers: recentUsersRes.data || [],
    })
  } catch (error) {
    console.error('Stats API error:', error)
    return NextResponse.json({ error: (error as Error).message }, { status: 500 })
  }
}
