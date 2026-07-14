/** 活动详情页 */
import { useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { StatusBadge } from '../../components/data-display/StatusBadge'
import { DetailPanel } from '../../components/data-display/DetailPanel'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { useRequest } from '../../hooks/useRequest'
import { getActivityDetail } from '../../api/modules/public'
import { ROUTES } from '../../constants/routes'
import type { ActivityDetailView } from '../../types/view-models'

export function ActivityDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data, loading, error, run } = useRequest<ActivityDetailView>()

  useEffect(() => { if (id) run(() => getActivityDetail(id)) }, [run, id])

  return (
    <PageLayout variant="public">
      <Link to={ROUTES.ACTIVITIES} className="text-xs text-ochre hover:text-indigo mb-4 inline-block">← 返回活动</Link>
      {loading && <LoadingState />}
      {error && <ErrorState message="活动加载失败" />}
      {data && (
        <article>
          <h1 className="font-display text-2xl text-indigo mb-2 flex items-center gap-2">
            <Seal text="详" />
            {data.title}
          </h1>
          <InkStroke className="mb-4" />
          <StatusBadge status={data.status} />
          <div className="mt-4">
            <DetailPanel fields={[
              { label: '开始时间', value: data.startTime },
              { label: '结束时间', value: data.endTime },
              { label: '报名状态', value: data.registrationOpen ? <span className="text-jade">开放中</span> : <span className="text-text-muted">已关闭</span> },
            ]} />
          </div>
          <div className="text-text-secondary text-sm leading-relaxed mt-4">{data.description ?? '暂无详情'}</div>
        </article>
      )}
    </PageLayout>
  )
}
