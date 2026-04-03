import type { Metadata } from 'next'
import './globals.css'
import { Sidebar } from '@/components/Sidebar'
import { PasswordGateWrapper } from '@/components/PasswordGateWrapper'

export const metadata: Metadata = {
  title: 'SmackCheck Admin',
  description: 'Admin panel for SmackCheck app',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className="bg-gray-950 text-gray-100">
        <PasswordGateWrapper>
          <div className="flex h-screen">
            <Sidebar />
            <main className="flex-1 overflow-y-auto p-8">
              {children}
            </main>
          </div>
        </PasswordGateWrapper>
      </body>
    </html>
  )
}
