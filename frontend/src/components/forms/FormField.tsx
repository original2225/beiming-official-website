/** 表单字段 — 统一包裹 label + 错误提示 */
import type { ReactNode } from 'react'

interface FormFieldProps {
  label: string
  error?: string
  children: ReactNode
}

export function FormField({ label, error, children }: FormFieldProps) {
  return (
    <div className="flex flex-col gap-1">
      <label className="text-xs text-text-muted font-minecraft uppercase tracking-wider">{label}</label>
      {children}
      {error && <span className="text-xs text-mc-redstone">{error}</span>}
    </div>
  )
}
