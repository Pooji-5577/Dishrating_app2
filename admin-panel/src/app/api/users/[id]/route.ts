import { NextRequest, NextResponse } from 'next/server'
import { createServiceClient } from '@/lib/supabase'

export async function GET(
  req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params
    const supabase = createServiceClient()

    const [profileRes, ratingCountRes, ratingsRes] = await Promise.all([
      supabase.from('profiles').select('*').eq('id', id).single(),
      supabase.from('ratings').select('*', { count: 'exact', head: true }).eq('user_id', id),
      supabase.from('ratings')
        .select('id, dish_id, restaurant_id, rating, comment, image_url, created_at')
        .eq('user_id', id)
        .order('created_at', { ascending: false })
        .limit(10),
    ])

    if (profileRes.error) {
      return NextResponse.json({ error: profileRes.error.message }, { status: 404 })
    }

    return NextResponse.json({
      profile: profileRes.data,
      ratingCount: ratingCountRes.count || 0,
      ratings: ratingsRes.data || [],
    })
  } catch (error) {
    console.error('User detail API error:', error)
    return NextResponse.json({ error: (error as Error).message }, { status: 500 })
  }
}
