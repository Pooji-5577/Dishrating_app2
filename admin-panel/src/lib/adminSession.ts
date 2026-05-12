'use client'

import { supabase } from '@/lib/supabase'
import { fetchWithAdminAuth } from '@/lib/adminApi'

type RouterLike = {
  push: (href: string) => void
}

export async function ensureAdminSession(
  router: RouterLike
): Promise<{ ok: true } | { ok: false }> {
  const { data: { session } } = await supabase.auth.getSession()
  if (!session) {
    router.push('/login')
    return { ok: false }
  }

  try {
    const res = await fetchWithAdminAuth('/api/check-admin', { method: 'POST' })
    const body = await res.json()
    if (!res.ok || !body.isAdmin) {
      await supabase.auth.signOut()
      router.push('/login')
      return { ok: false }
    }
    return { ok: true }
  } catch {
    await supabase.auth.signOut()
    router.push('/login')
    return { ok: false }
  }
}
