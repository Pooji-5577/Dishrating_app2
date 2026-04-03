'use client'

import { useState, useEffect, Suspense } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { supabase } from '@/lib/supabase'

function LoginForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)
  const [mode, setMode] = useState<'login' | 'reset' | 'loading'>('loading')

  useEffect(() => {
    const { data: { subscription } } = supabase.auth.onAuthStateChange((event: string) => {
      if (event === 'PASSWORD_RECOVERY') {
        setMode('reset')
      }
    })

    // Check if URL has hash tokens from Supabase reset link (#access_token=...&type=recovery)
    const hash = window.location.hash
    if (hash && hash.includes('type=recovery')) {
      // Give Supabase client time to process the hash tokens
      setTimeout(() => {
        setMode((current) => current === 'loading' ? 'reset' : current)
      }, 1500)
      return () => subscription.unsubscribe()
    }

    // Check if redirected with ?reset=true
    if (searchParams.get('reset') === 'true') {
      setMode('reset')
    } else {
      setMode('login')
    }

    return () => subscription.unsubscribe()
  }, [searchParams])

  const handleResetPassword = async () => {
    if (!email.trim()) {
      setError('Enter your email address first')
      return
    }
    setLoading(true)
    setError('')
    setSuccess('')

    try {
      const { error: resetError } = await supabase.auth.resetPasswordForEmail(email, {
        redirectTo: `${window.location.origin}/login`,
      })
      if (resetError) {
        setError(resetError.message)
      } else {
        setSuccess('Password reset link sent! Check your email.')
      }
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  const handleSetNewPassword = async (e: React.FormEvent) => {
    e.preventDefault()
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match')
      return
    }
    if (newPassword.length < 6) {
      setError('Password must be at least 6 characters')
      return
    }

    setLoading(true)
    setError('')

    try {
      const { error: updateError } = await supabase.auth.updateUser({
        password: newPassword,
      })

      if (updateError) {
        setError(updateError.message)
        return
      }

      setSuccess('Password updated successfully! You can now sign in.')
      setMode('login')
      setNewPassword('')
      setConfirmPassword('')
      await supabase.auth.signOut()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    setSuccess('')

    try {
      const { data, error: authError } = await supabase.auth.signInWithPassword({
        email,
        password,
      })

      if (authError) {
        setError(authError.message)
        return
      }

      if (!data.user) {
        setError('Login failed')
        return
      }

      // Check admin status via server-side API (bypasses RLS)
      const adminCheck = await fetch('/api/check-admin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: data.user.id }),
      })
      const adminResult = await adminCheck.json()

      if (!adminResult.isAdmin) {
        await supabase.auth.signOut()
        setError(adminResult.error
          ? `Admin check failed: ${adminResult.error}`
          : 'Access denied: admin privileges required')
        return
      }

      router.push('/')
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  if (mode === 'loading') {
    return <p className="text-gray-500 text-center">Loading...</p>
  }

  if (mode === 'reset') {
    return (
      <form onSubmit={handleSetNewPassword} className="bg-gray-900 border border-gray-800 rounded-xl p-8 space-y-5">
        <p className="text-sm text-gray-400 text-center">Enter your new password</p>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">New Password</label>
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
            minLength={6}
            className="w-full px-4 py-2.5 bg-gray-800 border border-gray-700 rounded-lg text-gray-200 placeholder-gray-500 focus:outline-none focus:border-orange-500 focus:ring-1 focus:ring-orange-500"
            placeholder="New password"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Confirm Password</label>
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
            minLength={6}
            className="w-full px-4 py-2.5 bg-gray-800 border border-gray-700 rounded-lg text-gray-200 placeholder-gray-500 focus:outline-none focus:border-orange-500 focus:ring-1 focus:ring-orange-500"
            placeholder="Confirm password"
          />
        </div>

        {error && (
          <div className="bg-red-900/30 border border-red-800 text-red-400 text-sm px-4 py-3 rounded-lg">{error}</div>
        )}
        {success && (
          <div className="bg-green-900/30 border border-green-800 text-green-400 text-sm px-4 py-3 rounded-lg">{success}</div>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full py-2.5 bg-orange-500 hover:bg-orange-600 disabled:opacity-50 text-white font-medium rounded-lg transition-colors"
        >
          {loading ? 'Updating...' : 'Update Password'}
        </button>

        <button
          type="button"
          onClick={() => { setMode('login'); setError(''); setSuccess('') }}
          className="w-full text-sm text-gray-500 hover:text-orange-400 transition-colors"
        >
          Back to login
        </button>
      </form>
    )
  }

  return (
    <form onSubmit={handleLogin} className="bg-gray-900 border border-gray-800 rounded-xl p-8 space-y-5">
      <div>
        <label className="block text-sm font-medium text-gray-300 mb-2">Email</label>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          className="w-full px-4 py-2.5 bg-gray-800 border border-gray-700 rounded-lg text-gray-200 placeholder-gray-500 focus:outline-none focus:border-orange-500 focus:ring-1 focus:ring-orange-500"
          placeholder="admin@example.com"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-2">Password</label>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          className="w-full px-4 py-2.5 bg-gray-800 border border-gray-700 rounded-lg text-gray-200 placeholder-gray-500 focus:outline-none focus:border-orange-500 focus:ring-1 focus:ring-orange-500"
          placeholder="••••••••"
        />
      </div>

      {error && (
        <div className="bg-red-900/30 border border-red-800 text-red-400 text-sm px-4 py-3 rounded-lg">{error}</div>
      )}
      {success && (
        <div className="bg-green-900/30 border border-green-800 text-green-400 text-sm px-4 py-3 rounded-lg">{success}</div>
      )}

      <button
        type="submit"
        disabled={loading}
        className="w-full py-2.5 bg-orange-500 hover:bg-orange-600 disabled:opacity-50 text-white font-medium rounded-lg transition-colors"
      >
        {loading ? 'Signing in...' : 'Sign In'}
      </button>

      <button
        type="button"
        onClick={handleResetPassword}
        disabled={loading}
        className="w-full text-sm text-gray-500 hover:text-orange-400 transition-colors disabled:opacity-50"
      >
        Forgot password?
      </button>
    </form>
  )
}

export default function LoginPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-950">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-orange-400">SmackCheck</h1>
          <p className="text-gray-500 text-sm mt-1">Admin Panel</p>
        </div>
        <Suspense fallback={<p className="text-gray-500 text-center">Loading...</p>}>
          <LoginForm />
        </Suspense>
      </div>
    </div>
  )
}
