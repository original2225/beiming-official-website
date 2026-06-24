/** 风险徽标 — 展示操作风险等级 */
import type { RiskLevel } from '../../types/common'
import { RISK_LABELS } from '../../constants/status'

const RISK_COLORS: Record<RiskLevel, string> = {
  LOW: 'text-mc-emerald border-mc-emerald',
  MEDIUM: 'text-mc-gold border-mc-gold',
  HIGH: 'text-mc-redstone border-mc-redstone',
  CRITICAL: 'text-purple-500 border-purple-500',
}

export function RiskBadge({ level }: { level: RiskLevel }) {
  return (
    <span className={`inline-block px-2 py-0.5 text-xs font-minecraft border ${RISK_COLORS[level]}`}>
      {RISK_LABELS[level].label}
    </span>
  )
}
