import { Link } from 'react-router-dom'
import { LayoutGrid } from 'lucide-react'
import type { BoardView } from '../../types/view-models'

interface BoardCardProps {
  board: BoardView
}

export function BoardCard({ board }: BoardCardProps) {
  return (
    <Link
      to={`/community/boards/${board.boardId}`}
      className="group flex flex-col gap-3 p-4 panel-glass rim-light border-l-4 border-l-indigo/30 hover:border-l-indigo transition-all"
    >
      <div className="flex items-center gap-3">
        <div className="p-2 bg-indigo/15 text-indigo rounded-md">
          <LayoutGrid size={20} strokeWidth={2.5} />
        </div>
        <h2 className="font-display text-base text-text-primary group-hover:text-indigo transition-colors">
          {board.name}
        </h2>
      </div>
      <p className="text-xs text-text-secondary line-clamp-2">
        {board.description || '暂无描述'}
      </p>
      <div className="mt-auto pt-2 border-t border-rim flex items-center justify-between text-xs text-text-muted">
        <span>帖子 {board.postCount ?? 0}</span>
        <span className="text-indigo opacity-0 group-hover:opacity-100 transition-opacity">
          进入 →
        </span>
      </div>
    </Link>
  )
}
