/**
 * 通知中心 — 通知列表、标记已读、归档
 */

import { useEffect, useState } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { Pagination } from '../../components/data-display/Pagination'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { useRequest } from '../../hooks/useRequest'
import { usePagination } from '../../hooks/usePagination'
import { getMyNotifications, markNotificationRead, markAllNotificationsRead } from '../../api/modules/account'
import type { NotificationSummary } from '../../types/domain'
import type { PageResult } from '../../types/api'

export function NotificationPage() {
  const { page, pageSize, goTo } = usePagination()
  const { data, loading, run } = useRequest<PageResult<NotificationSummary>>()
  const [marked, setMarked] = useState<Set<string>>(new Set())

  useEffect(() => { run(() => getMyNotifications({ page, pageSize })) }, [run, page, pageSize])

  const handleMarkRead = async (id: string) => {
    try { await markNotificationRead(id); setMarked((s) => new Set(s).add(id)) } catch { /* ignore */ }
  }

  const handleMarkAll = async () => {
    try { await markAllNotificationsRead(); run(() => getMyNotifications({ page, pageSize })) } catch { /* ignore */ }
  }

  return (
    <PageLayout variant="account">
      <div className="mb-6">
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-3">
            <Seal text="消" />
            <h1 className="font-display text-2xl text-indigo">通知中心</h1>
          </div>
          <button onClick={handleMarkAll} className="btn-ink-ghost text-xs">全部标记已读</button>
        </div>
        <InkStroke />
      </div>
      {loading && <LoadingState />}
      {!loading && !data?.items.length && <EmptyState text="暂无通知" />}
      {data?.items.map((n) => (
        <div key={n.notificationId} className={`panel-ink p-3 mb-2 flex items-center justify-between ${!n.read && !marked.has(n.notificationId) ? 'border-ochre' : ''}`}>
          <div>
            <span className="text-sm text-text-primary">{n.title}</span>
            <span className="text-xs text-text-muted ml-3"><TimeDisplay iso={n.createdAt} /></span>
          </div>
          {!n.read && !marked.has(n.notificationId) && (
            <button onClick={() => handleMarkRead(n.notificationId)} className="btn-ink-ghost text-xs">标记已读</button>
          )}
        </div>
      ))}
      {data && data.total > pageSize && <Pagination page={page} pageSize={pageSize} total={data.total} onChange={goTo} />}
    </PageLayout>
  )
}
