/**
 * 社区首页 — 板块列表
 */

import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { PageLayout } from '../../components/layout/PageLayout'
import { EmptyState } from '../../components/feedback/EmptyState'
import { InkBanner } from '../../components/community/InkBanner'
import { useRequest } from '../../hooks/useRequest'
import { getBoards } from '../../api/modules/community'
import { BoardCard } from '../../components/community/BoardCard'
import { SkeletonCard } from '../../components/community/SkeletonCard'
import { ROUTES } from '../../constants/routes'
import type { BoardView } from '../../types/view-models'

export function BoardListPage() {
  const { data, loading, run } = useRequest<BoardView[]>()

  useEffect(() => { run(() => getBoards()) }, [run])

  return (
    <PageLayout variant="public">
      <InkBanner
        title="社区"
        seal="社"
        subtitle="探索板块，加入讨论，分享你的 Minecraft 故事。"
        action={
          <Link
            to={ROUTES.NEW_POST}
            className="btn-ink text-xs inline-flex items-center gap-2 self-start"
          >
            <Plus size={14} />
            发帖
          </Link>
        }
      />

      {loading && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          <SkeletonCard variant="board" count={8} />
        </div>
      )}

      {!loading && !data?.length && (
        <EmptyState
          title="暂无板块"
          text="社区还在建设中，来做第一个发帖的人吧！"
          action={
            <Link
              to={ROUTES.NEW_POST}
              className="btn-ink text-xs inline-flex items-center gap-2 mt-2"
            >
              <Plus size={14} />
              发布帖子
            </Link>
          }
        />
      )}

      {!!data?.length && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {data.map((b: BoardView) => (
            <BoardCard key={b.boardId} board={b} />
          ))}
        </div>
      )}
    </PageLayout>
  )
}
