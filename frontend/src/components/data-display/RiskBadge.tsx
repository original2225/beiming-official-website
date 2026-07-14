/** 风险徽标 — 展示操作风险等级 */
import type { RiskLevel } from '../../types/common'
import { RISK_LABELS } from '../../constants/status'

const RISK_COLORS: Record<RiskLevel, string> = {
  LOW: 'text-jade border-jade',
  MEDIUM: 'text-ochre border-ochre',
  HIGH: 'text-cinnabar border-cinnabar',
  CRITICAL: 'text-cinnabar border-cinnabar',
}

export function RiskBadge({ level }: { level: RiskLevel }) {
  return (
    <span className={`inline-block px-2 py-0.5 text-xs font-display border rounded-sm ${RISK_COLORS[level]}`}>
      {RISK_LABELS[level].label}
    </span>
  )
}
