/** 表单字段 — 统一包裹 label + 错误提示 */
import type { ReactNode } from 'react'
import type { FieldError } from '../../types/api'

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

/** 从 ApiError.errors 中提取指定字段的错误文案 */
export function getFieldError(errors: FieldError[] | undefined, field: string): string | undefined {
  return errors?.find((e) => e.field === field)?.reason
}
