/**
 * 板块帖子列表
 */

import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ChevronLeft, Edit3, Grid3X3, List } from 'lucide-react'
import { PageLayout } from '../../components/layout/PageLayout'
import { EmptyState } from '../../components/feedback/EmptyState'
import { Pagination } from '../../components/data-display/Pagination'
import { InkBanner } from '../../components/community/InkBanner'
import { useRequest } from '../../hooks/useRequest'
import { usePagination } from '../../hooks/usePagination'
import { getBoard, getPosts } from '../../api/modules/community'
import { PostCard } from '../../components/community/PostCard'
import { SkeletonCard } from '../../components/community/SkeletonCard'
import { ROUTES } from '../../constants/routes'
import type { PostSummary } from '../../types/domain'
import type { PageResult } from '../../types/api'
import type { BoardDetailView } from '../../types/view-models'

export function PostListPage() {
  const { boardId } = useParams<{ boardId: string }>()
  const { page, pageSize, goTo } = usePagination()
  const { data, loading, run } = useRequest<PageResult<PostSummary>>()
  const { data: board, run: runBoard } = useRequest<BoardDetailView>()
  const [compact, setCompact] = useState(false)

  useEffect(() => {
    if (boardId) {
      run(() => getPosts({ boardId, page, pageSize }))
      runBoard(() => getBoard(boardId))
    }
  }, [run, runBoard, boardId, page, pageSize])

  return (
    <PageLayout variant="public">
      <Link
        to={ROUTES.BOARDS}
        className="inline-flex items-center gap-1 text-xs text-ochre hover:text-indigo mb-3"
      >
        <ChevronLeft size={14} />
        社区
      </Link>

      <InkBanner
        title={board?.name ?? '帖子'}
        seal="帖"
        action={
          <div className="flex items-center gap-2 self-start">
            <button
              type="button"
              onClick={() => setCompact(!compact)}
              className="btn-ink-ghost p-2"
              title={compact ? '切换列表视图' : '切换紧凑视图'}
            >
              {compact ? <List size={16} /> : <Grid3X3 size={16} />}
            </button>
            <Link
              to={ROUTES.NEW_POST}
              className="btn-ink text-xs inline-flex items-center gap-2"
            >
              <Edit3 size={14} />
              发帖
            </Link>
          </div>
        }
      />

      {loading && (
        <div className="flex flex-col gap-3">
          <SkeletonCard variant="post" count={5} />
        </div>
      )}

      {!loading && !data?.items.length && (
        <EmptyState
          title="暂无帖子"
          text="这个板块还没有内容，来发布第一条讨论吧。"
          action={
            <Link
              to={ROUTES.NEW_POST}
              className="btn-ink text-xs inline-flex items-center gap-2 mt-2"
            >
              <Edit3 size={14} />
              发布帖子
            </Link>
          }
        />
      )}

      {!!data?.items.length && (
        <div className={compact ? 'grid grid-cols-1 sm:grid-cols-2 gap-3' : 'flex flex-col gap-3'}>
          {data.items.map((p: PostSummary) => (
            <PostCard key={p.postId} post={p} compact={compact} />
          ))}
        </div>
      )}

      {data && data.total > pageSize && (
        <Pagination page={page} pageSize={pageSize} total={data.total} onChange={goTo} />
      )}
    </PageLayout>
  )
}
