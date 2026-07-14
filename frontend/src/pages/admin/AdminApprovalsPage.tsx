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
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
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
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Seal text="审" />
          <h1 className="font-display text-2xl text-indigo">审核待办</h1>
        </div>
        <InkStroke />
      </div>
      {loading && <LoadingState />}
      {!loading && !data?.items?.length && <EmptyState text="暂无待办" />}
      {data?.items?.map((todo: AdminTodoView) => (
        <div key={todo.todoId} className="panel-ink p-3 mb-2 flex items-center justify-between hover:border-indigo">
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
