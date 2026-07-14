/** 状态徽标 — 展示内容/资源/素材等通用状态 */
import type { Status } from '../../types/common'
import { STATUS_LABELS } from '../../constants/status'

const STATUS_COLORS: Record<Status, string> = {
  DRAFT: 'bg-ink-600 text-rice-50',
  ACTIVE: 'bg-ochre text-rice-50',
  PUBLISHED: 'bg-indigo text-rice-50',
  OFFLINE: 'bg-ink-wash text-rice-50',
  ARCHIVED: 'bg-ink-700 text-text-muted',
  DELETED: 'bg-cinnabar text-rice-50',
}

export function StatusBadge({ status }: { status: Status }) {
  return (
    <span className={`inline-block px-2 py-0.5 text-xs font-display rounded-sm ${STATUS_COLORS[status]}`}>
      {STATUS_LABELS[status]}
    </span>
  )
}
