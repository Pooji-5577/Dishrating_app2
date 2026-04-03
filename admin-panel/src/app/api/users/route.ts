import { NextRequest, NextResponse } from 'next/server'
import { createServiceClient } from '@/lib/supabase'

export async function GET(req: NextRequest) {
  try {
    const supabase = createServiceClient()
    const { searchParams } = new URL(req.url)
    const search = searchParams.get('search') || ''
    const page = parseInt(searchParams.get('page') || '0')
    const pageSize = 50

    let query = supabase
      .from('profiles')
      .select('id, name, username, email, level, xp, created_at, last_rating_at, is_admin')
      .order('created_at', { ascending: false })
      .range(page * pageSize, (page + 1) * pageSize - 1)

    if (search.trim()) {
      query = query.or(
        `name.ilike.%${search}%,username.ilike.%${search}%,email.ilike.%${search}%`
      )
    }

    const { data, error } = await query

    if (error) {
      console.error('Users API error:', error)
      return NextResponse.json({ error: error.message }, { status: 500 })
    }

    return NextResponse.json({ users: data || [] })
  } catch (error) {
    console.error('Users API error:', error)
    return NextResponse.json({ error: (error as Error).message }, { status: 500 })
  }
}
