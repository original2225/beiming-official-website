/** 成员展示页 */
import { useEffect } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { Pagination } from '../../components/data-display/Pagination'
import { useRequest } from '../../hooks/useRequest'
import { usePagination } from '../../hooks/usePagination'
import { getMembers } from '../../api/modules/public'
import type { MemberSummary } from '../../types/domain'
import type { PageResult } from '../../types/api'

export function MemberListPage() {
  const { page, pageSize, goTo } = usePagination()
  const { data, loading, run } = useRequest<PageResult<MemberSummary>>()

  useEffect(() => { run(() => getMembers({ page, pageSize })) }, [run, page, pageSize])

  return (
    <PageLayout variant="public">
      <h1 className="font-minecraft text-2xl text-mc-grass mb-6">社区成员</h1>
      {loading && <LoadingState />}
      {!loading && !data?.items.length && <EmptyState text="暂无成员" />}
      <div className="grid grid-cols-4 gap-3">
        {data?.items.map((m) => (
          <div key={m.memberId} className="panel-mc p-4 text-center">
            <p className="font-minecraft text-sm text-text-primary">{m.displayName}</p>
            {m.minecraftName && <p className="text-xs text-mc-grass mt-1">{m.minecraftName}</p>}
            {m.groupName && <p className="text-xs text-text-muted">{m.groupName}</p>}
          </div>
        ))}
      </div>
      {data && data.total > pageSize && <Pagination page={page} pageSize={pageSize} total={data.total} onChange={goTo} />}
    </PageLayout>
  )
}
