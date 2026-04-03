'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { supabase } from '@/lib/supabase'

export default function AuthCallbackPage() {
  const router = useRouter()

  useEffect(() => {
    // Supabase puts tokens in the URL hash (#access_token=...)
    // We need to let the Supabase client pick them up, then redirect
    const handleCallback = async () => {
      const { data: { session }, error } = await supabase.auth.getSession()

      if (error) {
        console.error('Auth callback error:', error)
        router.push('/login')
        return
      }

      // Check if this is a password recovery flow
      const hashParams = new URLSearchParams(window.location.hash.substring(1))
      const type = hashParams.get('type')

      if (type === 'recovery' || session) {
        // Redirect to login with a flag so it shows the reset form
        router.push('/login?reset=true')
      } else {
        router.push('/login')
      }
    }

    handleCallback()
  }, [router])

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-950">
      <p className="text-gray-500">Processing... please wait</p>
    </div>
  )
}
