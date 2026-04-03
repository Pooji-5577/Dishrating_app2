'use client'

import { useEffect, useState, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import { supabase } from '@/lib/supabase'
import { UserTable, UserRow } from '@/components/UserTable'

export default function UsersPage() {
  const router = useRouter()
  const [users, setUsers] = useState<UserRow[]>([])
  const [searchQuery, setSearchQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const PAGE_SIZE = 50

  useEffect(() => {
    checkAuth()
  }, [])

  useEffect(() => {
    loadUsers()
  }, [searchQuery, page])

  async function checkAuth() {
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
    }
  }

  const loadUsers = useCallback(async () => {
    setLoading(true)
    try {
      const params = new URLSearchParams({ page: String(page) })
      if (searchQuery.trim()) params.set('search', searchQuery)

      const res = await fetch(`/api/users?${params}`)
      const data = await res.json()

      setUsers(data.users || [])
      setHasMore((data.users?.length || 0) === PAGE_SIZE)
    } finally {
      setLoading(false)
    }
  }, [searchQuery, page])

  const handleSearchChange = (query: string) => {
    setSearchQuery(query)
    setPage(0)
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-white mb-8">Users</h1>

      {loading && users.length === 0 ? (
        <div className="flex items-center justify-center h-64">
          <p className="text-gray-500">Loading users...</p>
        </div>
      ) : (
        <>
          <UserTable
            users={users}
            searchQuery={searchQuery}
            onSearchChange={handleSearchChange}
          />

          <div className="flex items-center justify-between mt-6">
            <button
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              className="px-4 py-2 text-sm bg-gray-800 hover:bg-gray-700 disabled:opacity-30 disabled:cursor-not-allowed text-gray-300 rounded-lg transition-colors"
            >
              Previous
            </button>
            <span className="text-sm text-gray-500">Page {page + 1}</span>
            <button
              onClick={() => setPage(page + 1)}
              disabled={!hasMore}
              className="px-4 py-2 text-sm bg-gray-800 hover:bg-gray-700 disabled:opacity-30 disabled:cursor-not-allowed text-gray-300 rounded-lg transition-colors"
            >
              Next
            </button>
          </div>
        </>
      )}
    </div>
  )
}
