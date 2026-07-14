/** 公告列表页 */
import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { Pagination } from '../../components/data-display/Pagination'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { useRequest } from '../../hooks/useRequest'
import { usePagination } from '../../hooks/usePagination'
import { getContentItems } from '../../api/modules/public'
import type { ContentSummary } from '../../types/domain'
import type { PageResult } from '../../types/api'

export function AnnouncementListPage() {
  const { page, pageSize, goTo } = usePagination()
  const { data, loading, run } = useRequest<PageResult<ContentSummary>>()

  useEffect(() => { run(() => getContentItems({ page, pageSize })) }, [run, page, pageSize])

  return (
    <PageLayout variant="public">
      <h1 className="font-display text-2xl text-indigo mb-2 flex items-center gap-2">
        <Seal text="公" />
        公告
      </h1>
      <InkStroke className="mb-6" />
      {loading && <LoadingState />}
      {!loading && !data?.items.length && <EmptyState text="暂无公告" />}
      {data?.items.map((item) => (
        <Link key={item.contentId} to={`/announcements/${item.contentId}`} className="block panel-ink p-3 mb-2 hover:border-indigo">
          <span className="text-sm text-text-primary">{item.title}</span>
          <span className="text-xs text-text-muted ml-3">{item.publishedAt && <TimeDisplay iso={item.publishedAt} />}</span>
        </Link>
      ))}
      {data && data.total > pageSize && <Pagination page={page} pageSize={pageSize} total={data.total} onChange={goTo} />}
    </PageLayout>
  )
}
