/** 指南中心 */
import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { Pagination } from '../../components/data-display/Pagination'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { useRequest } from '../../hooks/useRequest'
import { usePagination } from '../../hooks/usePagination'
import { getGuides } from '../../api/modules/public'
import type { GuideSummary } from '../../types/domain'
import type { PageResult } from '../../types/api'

export function GuideListPage() {
  const { page, pageSize, goTo } = usePagination()
  const { data, loading, run } = useRequest<PageResult<GuideSummary>>()

  useEffect(() => { run(() => getGuides({ page, pageSize })) }, [run, page, pageSize])

  return (
    <PageLayout variant="public">
      <h1 className="font-display text-2xl text-indigo mb-2 flex items-center gap-2">
        <Seal text="指" />
        指南中心
      </h1>
      <InkStroke className="mb-6" />
      {loading && <LoadingState />}
      {!loading && !data?.items.length && <EmptyState text="暂无指南" />}
      <div className="grid grid-cols-2 gap-3">
        {data?.items.map((g) => (
          <Link key={g.guideId} to={`/guides/${g.guideId}`} className="panel-ink p-3 hover:border-indigo">
            <p className="text-sm text-text-primary">{g.title}</p>
            <p className="text-xs text-text-muted mt-1">{g.category}</p>
          </Link>
        ))}
      </div>
      {data && data.total > pageSize && <Pagination page={page} pageSize={pageSize} total={data.total} onChange={goTo} />}
    </PageLayout>
  )
}
