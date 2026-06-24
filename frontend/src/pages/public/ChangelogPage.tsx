/** 更新日志页 */
import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { Pagination } from '../../components/data-display/Pagination'
import { useRequest } from '../../hooks/useRequest'
import { usePagination } from '../../hooks/usePagination'
import { getChangelogReleases } from '../../api/modules/public'
import type { ChangelogSummary } from '../../types/domain'
import type { PageResult } from '../../types/api'

export function ChangelogPage() {
  const { page, pageSize, goTo } = usePagination()
  const { data, loading, run } = useRequest<PageResult<ChangelogSummary>>()

  useEffect(() => { run(() => getChangelogReleases({ page, pageSize })) }, [run, page, pageSize])

  return (
    <PageLayout variant="public">
      <h1 className="font-minecraft text-2xl text-mc-grass mb-6">更新日志</h1>
      {loading && <LoadingState />}
      {!loading && !data?.items.length && <EmptyState text="暂无更新" />}
      {data?.items.map((c) => (
        <Link key={c.releaseId} to={`/changelog/${c.releaseId}`} className="block panel-mc p-3 mb-2 hover:border-mc-grass">
          <div className="flex items-center gap-3">
            <span className="font-minecraft text-sm text-mc-gold">{c.version}</span>
            <span className="text-sm text-text-primary">{c.title}</span>
            <span className="text-xs text-text-muted ml-auto"><TimeDisplay iso={c.publishedAt} /></span>
          </div>
          {c.tags.length > 0 && <div className="mt-1 flex gap-1">{c.tags.map((t) => <span key={t} className="text-xs text-text-muted border border-mc-stone px-1">{t}</span>)}</div>}
        </Link>
      ))}
      {data && data.total > pageSize && <Pagination page={page} pageSize={pageSize} total={data.total} onChange={goTo} />}
    </PageLayout>
  )
}
