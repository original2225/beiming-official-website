/** 活动详情页 */
import { useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { StatusBadge } from '../../components/data-display/StatusBadge'
import { DetailPanel } from '../../components/data-display/DetailPanel'
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
      <Link to={ROUTES.ACTIVITIES} className="text-xs text-mc-gold hover:text-mc-grass mb-4 inline-block">← 返回活动</Link>
      {loading && <LoadingState />}
      {error && <ErrorState message="活动加载失败" />}
      {data && (
        <article>
          <h1 className="font-minecraft text-2xl text-mc-grass mb-2">{data.title}</h1>
          <StatusBadge status={data.status} />
          <div className="mt-4">
            <DetailPanel fields={[
              { label: '开始时间', value: data.startTime },
              { label: '结束时间', value: data.endTime },
              { label: '报名状态', value: data.registrationOpen ? <span className="text-mc-emerald">开放中</span> : <span className="text-text-muted">已关闭</span> },
            ]} />
          </div>
          <div className="text-text-secondary text-sm leading-relaxed mt-4">{data.description ?? '暂无详情'}</div>
        </article>
      )}
    </PageLayout>
  )
}
