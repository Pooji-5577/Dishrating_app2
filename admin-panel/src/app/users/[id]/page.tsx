'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { supabase } from '@/lib/supabase'
import { StatsCard } from '@/components/StatsCard'
import { NotificationForm } from '@/components/NotificationForm'
import Link from 'next/link'

interface UserProfile {
  id: string
  name: string
  username: string | null
  email: string
  level: number
  xp: number
  total_points: number
  current_streak: number
  followers_count: number
  following_count: number
  bio: string | null
  profile_photo_url: string | null
  is_admin: boolean
  created_at: string
  last_rating_at: string | null
}

interface Rating {
  id: string
  dish_id: string
  restaurant_id: string
  rating: number
  comment: string
  image_url: string | null
  created_at: string
}

export default function UserDetailPage() {
  const params = useParams()
  const router = useRouter()
  const userId = params.id as string
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [ratings, setRatings] = useState<Rating[]>([])
  const [ratingCount, setRatingCount] = useState(0)
  const [loading, setLoading] = useState(true)
  const [showNotifForm, setShowNotifForm] = useState(false)

  useEffect(() => {
    checkAuthAndLoad()
  }, [userId])

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

    await loadUserData()
  }

  async function loadUserData() {
    try {
      const res = await fetch(`/api/users/${userId}`)
      const data = await res.json()

      if (data.error) {
        console.error('Failed to load user:', data.error)
        return
      }

      setProfile(data.profile)
      setRatingCount(data.ratingCount)
      setRatings(data.ratings)
    } catch (error) {
      console.error('Failed to load user data:', error)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-gray-500">Loading user...</p>
      </div>
    )
  }

  if (!profile) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-400">User not found</p>
        <Link href="/users" className="text-orange-400 hover:underline text-sm mt-2 inline-block">
          Back to users
        </Link>
      </div>
    )
  }

  return (
    <div>
      <Link href="/users" className="text-sm text-gray-500 hover:text-gray-300 mb-6 inline-block">
        &larr; Back to Users
      </Link>

      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6 mb-6">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-4">
            {profile.profile_photo_url ? (
              <img
                src={profile.profile_photo_url}
                alt={profile.name}
                className="w-16 h-16 rounded-full object-cover"
              />
            ) : (
              <div className="w-16 h-16 rounded-full bg-gray-700 flex items-center justify-center text-2xl">
                {(profile.name || '?')[0].toUpperCase()}
              </div>
            )}
            <div>
              <h1 className="text-xl font-bold text-white">{profile.name || 'Unknown'}</h1>
              {profile.username && (
                <p className="text-sm text-gray-400">@{profile.username}</p>
              )}
              <p className="text-sm text-gray-500">{profile.email}</p>
              {profile.bio && (
                <p className="text-sm text-gray-400 mt-1">{profile.bio}</p>
              )}
            </div>
          </div>
          <div className="flex gap-3">
            {profile.is_admin && (
              <span className="px-3 py-1 text-xs bg-orange-500/20 text-orange-400 rounded-full">Admin</span>
            )}
            <button
              onClick={() => setShowNotifForm(!showNotifForm)}
              className="px-4 py-2 text-sm bg-orange-500 hover:bg-orange-600 text-white rounded-lg transition-colors"
            >
              {showNotifForm ? 'Hide' : 'Send Notification'}
            </button>
          </div>
        </div>

        <div className="mt-4 flex gap-6 text-sm text-gray-400">
          <span>Joined: {new Date(profile.created_at).toLocaleDateString()}</span>
          <span>Last active: {profile.last_rating_at ? new Date(profile.last_rating_at).toLocaleDateString() : 'Never'}</span>
          <span>Followers: {profile.followers_count || 0}</span>
          <span>Following: {profile.following_count || 0}</span>
        </div>
      </div>

      {showNotifForm && (
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-6 mb-6">
          <h2 className="text-lg font-semibold text-white mb-4">Send Notification</h2>
          <NotificationForm
            targetUserId={profile.id}
            targetUserName={profile.name || profile.email}
            onSuccess={() => setShowNotifForm(false)}
          />
        </div>
      )}

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <StatsCard icon="⭐" title="Total Ratings" value={ratingCount} />
        <StatsCard icon="🏆" title="Level" value={profile.level || 1} />
        <StatsCard icon="✨" title="XP" value={profile.xp || 0} />
        <StatsCard icon="🔥" title="Streak" value={`${profile.current_streak || 0} days`} />
      </div>

      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <h2 className="text-lg font-semibold text-white mb-4">Recent Ratings</h2>
        <div className="space-y-3">
          {ratings.map((rating) => (
            <div key={rating.id} className="flex items-center justify-between py-3 border-b border-gray-800 last:border-0">
              <div className="flex items-center gap-3">
                {rating.image_url && (
                  <img src={rating.image_url} alt="Dish" className="w-10 h-10 rounded-lg object-cover" />
                )}
                <div>
                  <p className="text-sm text-gray-200">
                    {'⭐'.repeat(Math.round(rating.rating))} ({rating.rating})
                  </p>
                  {rating.comment && (
                    <p className="text-xs text-gray-500 max-w-md truncate">{rating.comment}</p>
                  )}
                </div>
              </div>
              <p className="text-xs text-gray-500">
                {new Date(rating.created_at).toLocaleDateString()}
              </p>
            </div>
          ))}
          {ratings.length === 0 && (
            <p className="text-gray-500 text-sm">No ratings yet</p>
          )}
        </div>
      </div>
    </div>
  )
}
