/**
 * 社区首页 — 板块列表
 */

import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { useRequest } from '../../hooks/useRequest'
import { getBoards } from '../../api/modules/community'
import type { BoardView } from '../../types/view-models'

export function BoardListPage() {
  const { data, loading, run } = useRequest<BoardView[]>()

  useEffect(() => { run(() => getBoards()) }, [run])

  return (
    <PageLayout variant="public">
      <h1 className="font-minecraft text-2xl text-mc-grass mb-6">社区</h1>
      {loading && <LoadingState />}
      {!loading && !data?.length && <EmptyState text="暂无板块" />}
      <div className="grid grid-cols-2 gap-3">
        {data?.map((b: BoardView) => (
          <Link key={b.boardId} to={`/community/boards/${b.boardId}`} className="panel-mc p-4 hover:border-mc-grass">
            <h2 className="font-minecraft text-sm text-text-primary">{b.name}</h2>
            <p className="text-xs text-text-muted mt-1">{b.description ?? ''}</p>
            {b.postCount !== undefined && <p className="text-xs text-text-muted mt-2">帖子: {b.postCount}</p>}
          </Link>
        ))}
      </div>
    </PageLayout>
  )
}
