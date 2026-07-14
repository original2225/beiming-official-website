/** 详情面板 — 键值对展示 */
import type { ReactNode } from 'react'

interface Field {
  label: string
  value: ReactNode
}

interface DetailPanelProps {
  fields: Field[]
}

export function DetailPanel({ fields }: DetailPanelProps) {
  return (
    <div className="panel-ink p-4">
      {fields.map((f, i) => (
        <div key={i} className={`flex py-2 ${i > 0 ? 'border-t border-surface-light' : ''}`}>
          <span className="w-32 shrink-0 text-xs text-text-muted font-display uppercase">{f.label}</span>
          <span className="text-sm text-text-primary">{f.value}</span>
        </div>
      ))}
    </div>
  )
}
