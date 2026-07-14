/**
 * 页面布局壳 — 根据 variant 决定结构
 */

import type { ReactNode } from 'react'
import { useLocation } from 'react-router-dom'
import { PublicNav } from './PublicNav'
import { AdminSidebar } from './AdminSidebar'
import { cn } from '../../utils/cn'

import { InkWashBg } from '../community/InkWashBg'

interface PageLayoutProps {
  children: ReactNode
  /** public=公开页 | account=用户中心 | admin=后台 | ops=运维 */
  variant?: 'public' | 'account' | 'admin' | 'ops'
}

export function PageLayout({ children, variant = 'public' }: PageLayoutProps) {
  const location = useLocation()
  const isCommunity = location.pathname.startsWith('/community')

  if (variant === 'admin' || variant === 'ops') {
    return (
      <div className="min-h-screen bg-surface-dark flex flex-col">
        <PublicNav />
        <div className="flex flex-1">
          <AdminSidebar />
          <main className="flex-1 p-4 overflow-auto">{children}</main>
        </div>
      </div>
    )
  }

  return (
    <div
      className={cn(
        'min-h-screen bg-surface-dark flex flex-col',
        isCommunity && 'community-light'
      )}
    >
      {isCommunity && <InkWashBg />}
      <PublicNav />
      <main className="flex-1 max-w-7xl mx-auto w-full p-4 relative z-10">
        {children}
      </main>
    </div>
  )
}
