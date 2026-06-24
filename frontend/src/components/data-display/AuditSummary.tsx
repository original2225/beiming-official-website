/** 审计摘要 — 展示创建/修改时间和操作人 */
import type { AuditSummary as AuditSummaryType } from '../../types/common'
import { TimeDisplay } from './TimeDisplay'

interface AuditSummaryProps {
  audit: AuditSummaryType
}

export function AuditSummary({ audit }: AuditSummaryProps) {
  return (
    <div className="text-xs text-text-muted flex flex-wrap gap-x-4 gap-y-1">
      <span>创建: <TimeDisplay iso={audit.createdAt} /></span>
      {audit.createdBy && <span>创建人: {audit.createdBy}</span>}
      <span>更新: <TimeDisplay iso={audit.updatedAt} /></span>
      {audit.updatedBy && <span>更新人: {audit.updatedBy}</span>}
    </div>
  )
}
