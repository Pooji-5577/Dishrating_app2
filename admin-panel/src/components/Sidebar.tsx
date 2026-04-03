'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'

const navItems = [
  { href: '/', label: 'Dashboard', icon: '📊' },
  { href: '/users', label: 'Users', icon: '👥' },
  { href: '/notifications', label: 'Notifications', icon: '🔔' },
]

export function Sidebar() {
  const pathname = usePathname()

  // Hide sidebar on login page
  if (pathname === '/login') return null

  return (
    <aside className="w-64 bg-gray-900 border-r border-gray-800 flex flex-col">
      <div className="p-6 border-b border-gray-800">
        <h1 className="text-xl font-bold text-orange-400">SmackCheck</h1>
        <p className="text-xs text-gray-500 mt-1">Admin Panel</p>
      </div>
      <nav className="flex-1 p-4 space-y-1">
        {navItems.map((item) => {
          const isActive = pathname === item.href ||
            (item.href !== '/' && pathname.startsWith(item.href))
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-orange-500/10 text-orange-400'
                  : 'text-gray-400 hover:text-gray-200 hover:bg-gray-800'
              }`}
            >
              <span>{item.icon}</span>
              {item.label}
            </Link>
          )
        })}
      </nav>
      <div className="p-4 border-t border-gray-800">
        <button
          onClick={() => {
            // Sign out handled by supabase
            import('@/lib/supabase').then(({ supabase }) => {
              supabase.auth.signOut().then(() => {
                window.location.href = '/login'
              })
            })
          }}
          className="w-full px-4 py-2 text-sm text-gray-400 hover:text-red-400 hover:bg-gray-800 rounded-lg transition-colors text-left"
        >
          Sign Out
        </button>
      </div>
    </aside>
  )
}
