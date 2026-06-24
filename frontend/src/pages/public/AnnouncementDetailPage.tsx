/** 公告详情页 */
import { useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { useRequest } from '../../hooks/useRequest'
import { getContentDetail } from '../../api/modules/public'
import { ROUTES } from '../../constants/routes'
import type { ContentDetailView } from '../../types/view-models'

export function AnnouncementDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data, loading, error, run } = useRequest<ContentDetailView>()

  useEffect(() => { if (id) run(() => getContentDetail(id)) }, [run, id])

  return (
    <PageLayout variant="public">
      <Link to={ROUTES.ANNOUNCEMENTS} className="text-xs text-mc-gold hover:text-mc-grass mb-4 inline-block">← 返回公告</Link>
      {loading && <LoadingState />}
      {error && <ErrorState message="公告加载失败" />}
      {data && (
        <article>
          <h1 className="font-minecraft text-2xl text-mc-grass mb-2">{data.title}</h1>
          {data.publishedAt && <p className="text-xs text-text-muted mb-4"><TimeDisplay iso={data.publishedAt} /></p>}
          <div className="text-text-secondary text-sm leading-relaxed">{data.body ?? data.content ?? '暂无内容'}</div>
        </article>
      )}
    </PageLayout>
  )
}
