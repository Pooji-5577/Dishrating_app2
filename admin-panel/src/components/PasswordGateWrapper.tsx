'use client'

import { PasswordGate } from './PasswordGate'

export function PasswordGateWrapper({ children }: { children: React.ReactNode }) {
  return <PasswordGate>{children}</PasswordGate>
}
