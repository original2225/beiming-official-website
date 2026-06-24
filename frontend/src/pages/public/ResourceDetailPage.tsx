/** 资源详情页 */
import { useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { useRequest } from '../../hooks/useRequest'
import { getResourceDetail } from '../../api/modules/public'
import { ROUTES } from '../../constants/routes'

export function ResourceDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data, loading, error, run } = useRequest<any>()

  useEffect(() => { if (id) run(() => getResourceDetail(id)) }, [run, id])

  return (
    <PageLayout variant="public">
      <Link to={ROUTES.RESOURCES} className="text-xs text-mc-gold hover:text-mc-grass mb-4 inline-block">← 返回资源</Link>
      {loading && <LoadingState />}
      {error && <ErrorState message="资源加载失败" />}
      {data && (
        <article>
          <h1 className="font-minecraft text-2xl text-mc-grass mb-2">{data.title}</h1>
          <p className="text-xs text-text-muted mb-2">{data.category} · v{data.version}</p>
          <div className="text-text-secondary text-sm leading-relaxed mb-4">{data.description ?? '暂无简介'}</div>
          {data.downloadUrl && <a href={data.downloadUrl as string} className="btn-mc text-sm inline-block">下载</a>}
        </article>
      )}
    </PageLayout>
  )
}
