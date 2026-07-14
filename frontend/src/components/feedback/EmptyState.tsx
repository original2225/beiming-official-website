import type { ReactNode } from 'react'

interface EmptyStateProps {
  text?: string
  title?: string
  icon?: ReactNode
  action?: ReactNode
}

export function EmptyState({ text = '暂无数据', title, icon, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-12 gap-3 text-center">
      {icon && <div className="text-indigo">{icon}</div>}
      {title && <span className="font-display text-text-primary">{title}</span>}
      <span className="text-text-muted text-sm max-w-md">{text}</span>
      {action}
    </div>
  )
}
