/** 状态徽标 — 展示内容/资源/素材等通用状态 */
import type { Status } from '../../types/common'
import { STATUS_LABELS } from '../../constants/status'

const STATUS_COLORS: Record<Status, string> = {
  DRAFT: 'bg-gray-600 text-white',
  ACTIVE: 'bg-yellow-700 text-white',
  PUBLISHED: 'bg-mc-grass text-white',
  OFFLINE: 'bg-gray-500 text-white',
  ARCHIVED: 'bg-stone-700 text-text-muted',
  DELETED: 'bg-mc-redstone text-white',
}

export function StatusBadge({ status }: { status: Status }) {
  return (
    <span className={`inline-block px-2 py-0.5 text-xs font-minecraft ${STATUS_COLORS[status]}`}>
      {STATUS_LABELS[status]}
    </span>
  )
}
