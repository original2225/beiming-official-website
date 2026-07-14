/** 资源中心 */
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
import { getResources } from '../../api/modules/public'
import type { ResourceSummary } from '../../types/domain'
import type { PageResult } from '../../types/api'

export function ResourceListPage() {
  const { page, pageSize, goTo } = usePagination()
  const { data, loading, run } = useRequest<PageResult<ResourceSummary>>()

  useEffect(() => { run(() => getResources({ page, pageSize })) }, [run, page, pageSize])

  return (
    <PageLayout variant="public">
      <h1 className="font-display text-2xl text-indigo mb-2 flex items-center gap-2">
        <Seal text="资" />
        资源中心
      </h1>
      <InkStroke className="mb-6" />
      {loading && <LoadingState />}
      {!loading && !data?.items.length && <EmptyState text="暂无资源" />}
      <div className="grid grid-cols-3 gap-3">
        {data?.items.map((r) => (
          <Link key={r.resourceId} to={`/resources/${r.resourceId}`} className="panel-ink p-3 hover:border-indigo text-center">
            <p className="font-display text-sm text-text-primary">{r.title}</p>
            <p className="text-xs text-text-muted mt-1">{r.category} · v{r.version}</p>
            <p className="text-xs text-text-muted">下载 {r.downloads}</p>
          </Link>
        ))}
      </div>
      {data && data.total > pageSize && <Pagination page={page} pageSize={pageSize} total={data.total} onChange={goTo} />}
    </PageLayout>
  )
}
