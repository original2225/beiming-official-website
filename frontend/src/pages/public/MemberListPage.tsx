/** 成员展示页 */
import { useEffect } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { Pagination } from '../../components/data-display/Pagination'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
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
      <h1 className="font-display text-2xl text-indigo mb-2 flex items-center gap-2">
        <Seal text="成" />
        社区成员
      </h1>
      <InkStroke className="mb-6" />
      {loading && <LoadingState />}
      {!loading && !data?.items.length && <EmptyState text="暂无成员" />}
      <div className="grid grid-cols-4 gap-3">
        {data?.items.map((m) => (
          <div key={m.memberId} className="panel-ink p-4 text-center">
            <p className="font-display text-sm text-text-primary">{m.displayName}</p>
            {m.minecraftName && <p className="text-xs text-indigo mt-1">{m.minecraftName}</p>}
            {m.groupName && <p className="text-xs text-text-muted">{m.groupName}</p>}
          </div>
        ))}
      </div>
      {data && data.total > pageSize && <Pagination page={page} pageSize={pageSize} total={data.total} onChange={goTo} />}
    </PageLayout>
  )
}
