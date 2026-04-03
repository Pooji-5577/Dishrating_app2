import { NextRequest, NextResponse } from 'next/server'
import { createServiceClient } from '@/lib/supabase'

export async function POST(req: NextRequest) {
  try {
    const { userId } = await req.json()

    if (!userId) {
      return NextResponse.json({ isAdmin: false, error: 'Missing userId' }, { status: 400 })
    }

    // Use service role client to bypass RLS
    const supabase = createServiceClient()

    const { data: profile, error } = await supabase
      .from('profiles')
      .select('is_admin')
      .eq('id', userId)
      .single()

    if (error) {
      console.error('Admin check error:', error)
      return NextResponse.json({ isAdmin: false, error: error.message }, { status: 500 })
    }

    return NextResponse.json({ isAdmin: profile?.is_admin === true })
  } catch (err) {
    console.error('Admin check failed:', err)
    return NextResponse.json({ isAdmin: false, error: (err as Error).message }, { status: 500 })
  }
}
