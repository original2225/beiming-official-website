/** 活动列表页 */
import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { StatusBadge } from '../../components/data-display/StatusBadge'
import { Pagination } from '../../components/data-display/Pagination'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { useRequest } from '../../hooks/useRequest'
import { usePagination } from '../../hooks/usePagination'
import { getActivities } from '../../api/modules/public'
import type { ActivitySummary } from '../../types/domain'
import type { PageResult } from '../../types/api'

export function ActivityListPage() {
  const { page, pageSize, goTo } = usePagination()
  const { data, loading, run } = useRequest<PageResult<ActivitySummary>>()

  useEffect(() => { run(() => getActivities({ page, pageSize })) }, [run, page, pageSize])

  return (
    <PageLayout variant="public">
      <h1 className="font-display text-2xl text-indigo mb-2 flex items-center gap-2">
        <Seal text="动" />
        活动
      </h1>
      <InkStroke className="mb-6" />
      {loading && <LoadingState />}
      {!loading && !data?.items.length && <EmptyState text="暂无活动" />}
      {data?.items.map((a) => (
        <Link key={a.activityId} to={`/activities/${a.activityId}`} className="block panel-ink p-3 mb-2 hover:border-indigo">
          <div className="flex items-center gap-3">
            <span className="text-sm text-text-primary">{a.title}</span>
            <StatusBadge status={a.status} />
            {a.registrationOpen && <span className="text-xs text-jade">报名中</span>}
          </div>
          <p className="text-xs text-text-muted mt-1">{a.startTime} - {a.endTime}</p>
        </Link>
      ))}
      {data && data.total > pageSize && <Pagination page={page} pageSize={pageSize} total={data.total} onChange={goTo} />}
    </PageLayout>
  )
}
