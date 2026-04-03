'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { supabase } from '@/lib/supabase'
import { NotificationForm } from '@/components/NotificationForm'

interface SentNotification {
  id: string
  user_id: string
  title: string
  body: string
  event_type: string
  created_at: string
}

export default function NotificationsPage() {
  const router = useRouter()
  const [recentNotifications, setRecentNotifications] = useState<SentNotification[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    checkAuthAndLoad()
  }, [])

  async function checkAuthAndLoad() {
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
      router.push('/login')
      return
    }

    await loadRecentNotifications()
  }

  async function loadRecentNotifications() {
    try {
      const res = await fetch('/api/notifications')
      const data = await res.json()
      setRecentNotifications(data.notifications || [])
    } catch (error) {
      console.error('Failed to load notifications:', error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-white mb-8">Send Notifications</h1>

      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6 mb-8">
        <h2 className="text-lg font-semibold text-white mb-4">Compose Notification</h2>
        <NotificationForm onSuccess={loadRecentNotifications} />
      </div>

      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <h2 className="text-lg font-semibold text-white mb-4">Recent Admin Notifications</h2>
        {loading ? (
          <p className="text-gray-500 text-sm">Loading...</p>
        ) : (
          <div className="space-y-3">
            {recentNotifications.map((notif) => (
              <div key={notif.id} className="flex items-start justify-between py-3 border-b border-gray-800 last:border-0">
                <div>
                  <p className="text-sm font-medium text-gray-200">{notif.title}</p>
                  <p className="text-xs text-gray-400 mt-1">{notif.body}</p>
                </div>
                <p className="text-xs text-gray-500 whitespace-nowrap ml-4">
                  {new Date(notif.created_at).toLocaleString()}
                </p>
              </div>
            ))}
            {recentNotifications.length === 0 && (
              <p className="text-gray-500 text-sm">No admin notifications sent yet</p>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
