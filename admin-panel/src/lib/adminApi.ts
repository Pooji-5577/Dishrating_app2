'use client'

import { supabase } from '@/lib/supabase'

export async function authHeaders(): Promise<Record<string, string> | null> {
  const { data: { session } } = await supabase.auth.getSession()
  const token = session?.access_token
  if (!token) return null
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  }
}

export async function fetchWithAdminAuth(
  input: string,
  init: RequestInit = {}
): Promise<Response> {
  const headers = await authHeaders()
  if (!headers) {
    throw new Error('Not authenticated')
  }
  return fetch(input, {
    ...init,
    headers: {
      ...headers,
      ...(init.headers || {}),
    },
  })
}
