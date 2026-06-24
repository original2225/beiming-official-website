/**
 * 页面布局壳 — 根据 variant 决定结构
 */

import type { ReactNode } from 'react'
import { PublicNav } from './PublicNav'
import { AdminSidebar } from './AdminSidebar'

interface PageLayoutProps {
  children: ReactNode
  /** public=公开页 | account=用户中心 | admin=后台 | ops=运维 */
  variant?: 'public' | 'account' | 'admin' | 'ops'
}

export function PageLayout({ children, variant = 'public' }: PageLayoutProps) {
  if (variant === 'admin' || variant === 'ops') {
    return (
      <div className="min-h-screen bg-surface-dark flex flex-col">
        <PublicNav />
        <div className="flex flex-1">
          <AdminSidebar />
          <main className="flex-1 p-4 overflow-auto">
            {children}
          </main>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-surface-dark flex flex-col">
      <PublicNav />
      <main className="flex-1 max-w-7xl mx-auto w-full p-4">
        {children}
      </main>
    </div>
  )
}
