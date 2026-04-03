'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { supabase } from '@/lib/supabase'
import { StatsCard } from '@/components/StatsCard'

interface Stats {
  totalUsers: number
  totalRatings: number
  activeUsers24h: number
  newUsers7d: number
  recentUsers: any[]
}

export default function DashboardPage() {
  const router = useRouter()
  const [stats, setStats] = useState<Stats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    checkAuthAndLoadData()
  }, [])

  async function checkAuthAndLoadData() {
    const { data: { session } } = await supabase.auth.getSession()
    if (!session) {
      router.push('/login')
      return
    }

    const res = await fetch('/api/check-admin', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: session.user.id }),
    })
    const { isAdmin } = await res.json()
    if (!isAdmin) {
      await supabase.auth.signOut()
      router.push('/login')
      return
    }

    await loadStats()
  }

  async function loadStats() {
    try {
      const res = await fetch('/api/stats')
      const data = await res.json()
      setStats(data)
    } catch (error) {
      console.error('Failed to load stats:', error)
    } finally {
      setLoading(false)
    }
  }

  // Auto-refresh stats every 30 seconds
  useEffect(() => {
    if (!loading) {
      const interval = setInterval(loadStats, 30000)
      return () => clearInterval(interval)
    }
  }, [loading])

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-gray-500">Loading dashboard...</p>
      </div>
    )
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold text-white">Dashboard</h1>
        <button
          onClick={loadStats}
          className="px-4 py-2 text-sm bg-gray-800 hover:bg-gray-700 text-gray-300 rounded-lg transition-colors"
        >
          Refresh
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
        <StatsCard icon="👥" title="Total Users" value={stats?.totalUsers ?? 0} />
        <StatsCard icon="⭐" title="Total Ratings" value={stats?.totalRatings ?? 0} />
        <StatsCard
          icon="🟢"
          title="Active (24h)"
          value={stats?.activeUsers24h ?? 0}
          subtitle="Users who rated in last 24h"
        />
        <StatsCard
          icon="🆕"
          title="New Users (7d)"
          value={stats?.newUsers7d ?? 0}
          subtitle="Joined in last 7 days"
        />
      </div>

      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <h2 className="text-lg font-semibold text-white mb-4">Recent Signups</h2>
        <div className="space-y-3">
          {(stats?.recentUsers || []).map((user: any) => (
            <div
              key={user.id}
              className="flex items-center justify-between py-2 border-b border-gray-800 last:border-0"
            >
              <div>
                <p className="text-sm font-medium text-gray-200">{user.name || 'Unknown'}</p>
                <p className="text-xs text-gray-500">{user.email}</p>
              </div>
              <p className="text-xs text-gray-500">
                {new Date(user.created_at).toLocaleDateString()}
              </p>
            </div>
          ))}
          {(!stats?.recentUsers || stats.recentUsers.length === 0) && (
            <p className="text-gray-500 text-sm">No users yet</p>
          )}
        </div>
      </div>
    </div>
  )
}
