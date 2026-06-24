/** 指南详情页 */
import { useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { useRequest } from '../../hooks/useRequest'
import { getGuideDetail } from '../../api/modules/public'
import { ROUTES } from '../../constants/routes'
import type { GuideDetailView } from '../../types/view-models'

export function GuideDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data, loading, error, run } = useRequest<GuideDetailView>()

  useEffect(() => { if (id) run(() => getGuideDetail(id)) }, [run, id])

  return (
    <PageLayout variant="public">
      <Link to={ROUTES.GUIDES} className="text-xs text-mc-gold hover:text-mc-grass mb-4 inline-block">← 返回指南</Link>
      {loading && <LoadingState />}
      {error && <ErrorState message="指南加载失败" />}
      {data && (
        <article>
          <h1 className="font-minecraft text-2xl text-mc-grass mb-2">{data.title}</h1>
          <p className="text-xs text-text-muted mb-4">更新于 <TimeDisplay iso={data.updatedAt} /></p>
          <div className="text-text-secondary text-sm leading-relaxed">{data.body ?? data.content ?? '暂无内容'}</div>
        </article>
      )}
    </PageLayout>
  )
}
