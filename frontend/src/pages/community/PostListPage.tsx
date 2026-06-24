/**
 * 板块帖子列表
 */

import { useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { Pagination } from '../../components/data-display/Pagination'
import { useRequest } from '../../hooks/useRequest'
import { usePagination } from '../../hooks/usePagination'
import { getBoard, getPosts } from '../../api/modules/community'
import { ROUTES } from '../../constants/routes'
import type { PostSummary } from '../../types/domain'
import type { PageResult } from '../../types/api'

export function PostListPage() {
  const { boardId } = useParams<{ boardId: string }>()
  const { page, pageSize, goTo } = usePagination()
  const { data, loading, run } = useRequest<PageResult<PostSummary>>()
  const { data: board, run: runBoard } = useRequest<any>()

  useEffect(() => { if (boardId) { run(() => getPosts({ boardId, page, pageSize })); runBoard(() => getBoard(boardId)) } }, [run, runBoard, boardId, page, pageSize])

  return (
    <PageLayout variant="public">
      <div className="flex items-center justify-between mb-6">
        <div>
          <Link to={ROUTES.BOARDS} className="text-xs text-mc-gold hover:text-mc-grass mb-1 inline-block">← 社区</Link>
          <h1 className="font-minecraft text-2xl text-mc-grass">{board?.name ?? '帖子'}</h1>
        </div>
        <Link to={ROUTES.NEW_POST} className="btn-mc text-xs">发帖</Link>
      </div>
      {loading && <LoadingState />}
      {!loading && !data?.items.length && <EmptyState text="暂无帖子" />}
      {data?.items.map((p) => (
        <Link key={p.postId} to={`/community/posts/${p.postId}`} className="block panel-mc p-3 mb-2 hover:border-mc-grass">
          <span className="text-sm text-text-primary">{p.title}</span>
          <div className="flex items-center gap-3 mt-1 text-xs text-text-muted">
            <span>{p.authorName}</span>
            <span>👍 {p.likes}</span>
            <span>💬 {p.comments}</span>
            <span className="ml-auto"><TimeDisplay iso={p.createdAt} /></span>
          </div>
        </Link>
      ))}
      {data && data.total > pageSize && <Pagination page={page} pageSize={pageSize} total={data.total} onChange={goTo} />}
    </PageLayout>
  )
}
