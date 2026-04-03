'use client'

import Link from 'next/link'

export interface UserRow {
  id: string
  name: string
  username: string | null
  email: string
  level: number
  xp: number
  created_at: string
  last_rating_at: string | null
  is_admin: boolean
}

interface UserTableProps {
  users: UserRow[]
  searchQuery: string
  onSearchChange: (query: string) => void
}

export function UserTable({ users, searchQuery, onSearchChange }: UserTableProps) {
  return (
    <div>
      <div className="mb-6">
        <input
          type="text"
          placeholder="Search by name, username, or email..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          className="w-full max-w-md px-4 py-2.5 bg-gray-900 border border-gray-700 rounded-lg text-gray-200 placeholder-gray-500 focus:outline-none focus:border-orange-500 focus:ring-1 focus:ring-orange-500"
        />
      </div>
      <div className="bg-gray-900 border border-gray-800 rounded-xl overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-gray-800">
              <th className="text-left px-6 py-4 text-xs font-medium text-gray-400 uppercase tracking-wider">User</th>
              <th className="text-left px-6 py-4 text-xs font-medium text-gray-400 uppercase tracking-wider">Username</th>
              <th className="text-left px-6 py-4 text-xs font-medium text-gray-400 uppercase tracking-wider">Level</th>
              <th className="text-left px-6 py-4 text-xs font-medium text-gray-400 uppercase tracking-wider">XP</th>
              <th className="text-left px-6 py-4 text-xs font-medium text-gray-400 uppercase tracking-wider">Last Active</th>
              <th className="text-left px-6 py-4 text-xs font-medium text-gray-400 uppercase tracking-wider">Joined</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-800">
            {users.map((user) => (
              <tr key={user.id} className="hover:bg-gray-800/50 transition-colors">
                <td className="px-6 py-4">
                  <Link href={`/users/${user.id}`} className="hover:text-orange-400 transition-colors">
                    <div>
                      <p className="font-medium text-gray-200">{user.name || 'Unknown'}</p>
                      <p className="text-xs text-gray-500">{user.email}</p>
                    </div>
                  </Link>
                </td>
                <td className="px-6 py-4 text-sm text-gray-400">
                  {user.username ? `@${user.username}` : '-'}
                </td>
                <td className="px-6 py-4 text-sm text-gray-300">{user.level || 1}</td>
                <td className="px-6 py-4 text-sm text-gray-300">{user.xp || 0}</td>
                <td className="px-6 py-4 text-sm text-gray-400">
                  {user.last_rating_at
                    ? new Date(user.last_rating_at).toLocaleDateString()
                    : 'Never'}
                </td>
                <td className="px-6 py-4 text-sm text-gray-400">
                  {new Date(user.created_at).toLocaleDateString()}
                </td>
              </tr>
            ))}
            {users.length === 0 && (
              <tr>
                <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                  No users found
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
