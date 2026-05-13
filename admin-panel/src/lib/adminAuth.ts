import { NextRequest } from 'next/server'
import { createClient } from '@supabase/supabase-js'
import { createServiceClient } from '@/lib/supabase'

type AdminAuthSuccess = {
  ok: true
  adminUserId: string
}

type AdminAuthFailure = {
  ok: false
  status: number
  error: string
}

export type AdminAuthResult = AdminAuthSuccess | AdminAuthFailure

function extractBearerToken(req: NextRequest): string | null {
  const header = req.headers.get('authorization')
  if (!header) return null
  const [scheme, token] = header.split(' ')
  if (scheme?.toLowerCase() !== 'bearer' || !token) return null
  return token.trim()
}

export async function requireAdmin(req: NextRequest): Promise<AdminAuthResult> {
  const token = extractBearerToken(req)
  if (!token) {
    return { ok: false, status: 401, error: 'Missing bearer token' }
  }

  const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || ''
  const anonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || ''
  if (!supabaseUrl || !anonKey) {
    return { ok: false, status: 500, error: 'Supabase public env not configured' }
  }

  const authClient = createClient(supabaseUrl, anonKey)
  const { data: authData, error: authError } = await authClient.auth.getUser(token)
  if (authError || !authData.user) {
    return { ok: false, status: 401, error: 'Invalid or expired token' }
  }

  const service = createServiceClient()
  const { data: profile, error: profileError } = await service
    .from('profiles')
    .select('is_admin')
    .eq('id', authData.user.id)
    .single()

  if (profileError) {
    return { ok: false, status: 500, error: profileError.message }
  }

  if (!profile?.is_admin) {
    return { ok: false, status: 403, error: 'Admin access required' }
  }

  return { ok: true, adminUserId: authData.user.id }
}
