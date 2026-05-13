import { NextRequest, NextResponse } from 'next/server'
import { requireAdmin } from '@/lib/adminAuth'

export async function POST(req: NextRequest) {
  try {
    const auth = await requireAdmin(req)
    if (!auth.ok) {
      return NextResponse.json({ isAdmin: false, error: auth.error }, { status: auth.status })
    }
    return NextResponse.json({ isAdmin: true, adminUserId: auth.adminUserId })
  } catch (err) {
    console.error('Admin check failed:', err)
    return NextResponse.json({ isAdmin: false, error: (err as Error).message }, { status: 500 })
  }
}
