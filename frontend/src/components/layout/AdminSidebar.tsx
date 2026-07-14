/**
 * 后台侧边栏
 */

import { Link, useLocation } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'

const ITEMS = [
  { label: '概览', to: ROUTES.ADMIN },
  { label: '用户管理', to: ROUTES.ADMIN_USERS },
  { label: '内容管理', to: ROUTES.ADMIN_CONTENT },
  { label: '审核待办', to: ROUTES.ADMIN_APPROVALS },
]

export function AdminSidebar() {
  const location = useLocation()

  return (
    <aside className="w-48 bg-surface min-h-full border-r border-ink-600/30 p-3 flex flex-col gap-1">
      <h3 className="font-display text-xs text-text-muted uppercase tracking-wider mb-2 px-2">
        后台管理
      </h3>
      {ITEMS.map((item) => (
        <Link
          key={item.to}
          to={item.to}
          className={`px-2 py-2 text-sm rounded-md
            ${location.pathname === item.to
              ? 'bg-indigo text-rice-50'
              : 'text-text-secondary hover:text-text-primary hover:bg-surface-light'
            }`}
        >
          {item.label}
        </Link>
      ))}
    </aside>
  )
}
