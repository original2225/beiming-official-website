/**
 * 审核待办 — 聚合待审核内容
 */

import { useEffect } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { StatusBadge } from '../../components/data-display/StatusBadge'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { Pagination } from '../../components/data-display/Pagination'
import { useRequest } from '../../hooks/useRequest'
import { usePagination } from '../../hooks/usePagination'
import { getAdminTodos } from '../../api/modules/admin'
import type { AdminTodoPage, AdminTodoView } from '../../types/view-models'

export function AdminApprovalsPage() {
  const { page, pageSize, goTo } = usePagination()
  const { data, loading, run } = useRequest<AdminTodoPage>()

  useEffect(() => { run(() => getAdminTodos()) }, [run])

  return (
    <PageLayout variant="admin">
      <h1 className="font-minecraft text-2xl text-mc-grass mb-6">审核待办</h1>
      {loading && <LoadingState />}
      {!loading && !data?.items?.length && <EmptyState text="暂无待办" />}
      {data?.items?.map((todo: AdminTodoView) => (
        <div key={todo.todoId} className="panel-mc p-3 mb-2 flex items-center justify-between hover:border-mc-grass">
          <div>
            <p className="text-sm text-text-primary">{todo.title}</p>
            <p className="text-xs text-text-muted">{todo.type ?? '审核'} · <TimeDisplay iso={todo.createdAt} /></p>
          </div>
          <StatusBadge status={todo.status ?? 'DRAFT'} />
        </div>
      ))}
      {data && data.total > pageSize && <Pagination page={page} pageSize={pageSize} total={data.total} onChange={goTo} />}
    </PageLayout>
  )
}
