/** 素材精选页 */
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
import { getMaterials } from '../../api/modules/public'
import type { MaterialSummary } from '../../types/domain'
import type { PageResult } from '../../types/api'

export function MaterialListPage() {
  const { page, pageSize, goTo } = usePagination()
  const { data, loading, run } = useRequest<PageResult<MaterialSummary>>()

  useEffect(() => { run(() => getMaterials({ page, pageSize })) }, [run, page, pageSize])

  return (
    <PageLayout variant="public">
      <h1 className="font-display text-2xl text-indigo mb-2 flex items-center gap-2">
        <Seal text="素" />
        素材精选
      </h1>
      <InkStroke className="mb-6" />
      {loading && <LoadingState />}
      {!loading && !data?.items.length && <EmptyState text="暂无素材" />}
      <div className="grid grid-cols-3 gap-3">
        {data?.items.map((m) => (
          <Link key={m.materialId} to={`/materials/${m.materialId}`} className="panel-ink p-3 hover:border-indigo text-center">
            <p className="text-sm text-text-primary">{m.title}</p>
            <p className="text-xs text-text-muted mt-1">{m.authorName}</p>
            {m.featured && <span className="text-xs text-ochre">★ 精选</span>}
          </Link>
        ))}
      </div>
      {data && data.total > pageSize && <Pagination page={page} pageSize={pageSize} total={data.total} onChange={goTo} />}
    </PageLayout>
  )
}
