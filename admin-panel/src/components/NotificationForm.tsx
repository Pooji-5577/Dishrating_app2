'use client'

import { useState } from 'react'
import { supabase } from '@/lib/supabase'

interface NotificationFormProps {
  targetUserId?: string
  targetUserName?: string
  onSuccess?: () => void
}

export function NotificationForm({ targetUserId, targetUserName, onSuccess }: NotificationFormProps) {
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [sendToAll, setSendToAll] = useState(!targetUserId)
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState<{ success: boolean; message: string } | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim() || !body.trim()) return

    const confirmed = window.confirm(
      sendToAll
        ? `Send notification to ALL users?\n\nTitle: ${title}\nBody: ${body}`
        : `Send notification to ${targetUserName || targetUserId}?\n\nTitle: ${title}\nBody: ${body}`
    )
    if (!confirmed) return

    setSending(true)
    setResult(null)

    try {
      const { data: { session } } = await supabase.auth.getSession()
      if (!session) {
        setResult({ success: false, message: 'Not authenticated' })
        return
      }

      const response = await fetch('/api/notifications/send', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          targetUserId: sendToAll ? undefined : targetUserId,
          title: title.trim(),
          body: body.trim(),
          senderUserId: session.user.id,
        }),
      })

      const data = await response.json()

      if (response.ok) {
        setResult({ success: true, message: data.message || 'Notification sent!' })
        setTitle('')
        setBody('')
        onSuccess?.()
      } else {
        setResult({ success: false, message: data.error || 'Failed to send notification' })
      }
    } catch (error) {
      setResult({ success: false, message: (error as Error).message })
    } finally {
      setSending(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {!targetUserId && (
        <div className="flex gap-4">
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="radio"
              checked={sendToAll}
              onChange={() => setSendToAll(true)}
              className="text-orange-500 focus:ring-orange-500"
            />
            <span className="text-sm text-gray-300">All Users</span>
          </label>
        </div>
      )}

      {targetUserId && (
        <div className="bg-gray-800 rounded-lg px-4 py-3 text-sm text-gray-300">
          Sending to: <span className="font-medium text-orange-400">{targetUserName || targetUserId}</span>
        </div>
      )}

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-2">Title</label>
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Notification title..."
          required
          className="w-full px-4 py-2.5 bg-gray-900 border border-gray-700 rounded-lg text-gray-200 placeholder-gray-500 focus:outline-none focus:border-orange-500 focus:ring-1 focus:ring-orange-500"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-2">Body</label>
        <textarea
          value={body}
          onChange={(e) => setBody(e.target.value)}
          placeholder="Notification message..."
          required
          rows={4}
          className="w-full px-4 py-2.5 bg-gray-900 border border-gray-700 rounded-lg text-gray-200 placeholder-gray-500 focus:outline-none focus:border-orange-500 focus:ring-1 focus:ring-orange-500 resize-none"
        />
      </div>

      {result && (
        <div className={`px-4 py-3 rounded-lg text-sm ${
          result.success
            ? 'bg-green-900/30 border border-green-800 text-green-400'
            : 'bg-red-900/30 border border-red-800 text-red-400'
        }`}>
          {result.message}
        </div>
      )}

      <button
        type="submit"
        disabled={sending || !title.trim() || !body.trim()}
        className="px-6 py-2.5 bg-orange-500 hover:bg-orange-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium rounded-lg transition-colors"
      >
        {sending ? 'Sending...' : sendToAll ? 'Send to All Users' : 'Send Notification'}
      </button>
    </form>
  )
}
