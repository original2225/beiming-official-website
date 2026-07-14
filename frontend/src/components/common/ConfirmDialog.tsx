/** 确认弹窗 — 后台/运维写操作二次确认 */
import type { RiskLevel } from '../../types/common'
import { RISK_LABELS } from '../../constants/status'

interface ConfirmDialogProps {
  open: boolean
  title: string
  message: string
  risk?: RiskLevel
  confirmText?: string
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({ open, title, message, risk, confirmText = '确认', onConfirm, onCancel }: ConfirmDialogProps) {
  if (!open) return null

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
      <div className="panel-ink p-6 max-w-md w-full mx-4">
        <h3 className="font-display text-lg mb-2">{title}</h3>
        {risk && (
          <span className="inline-block mb-2 px-2 py-0.5 text-xs font-display border rounded-sm text-cinnabar border-cinnabar">
            {RISK_LABELS[risk].label}
          </span>
        )}
        <p className="text-text-secondary text-sm mb-4">{message}</p>
        <div className="flex gap-3 justify-end">
          <button onClick={onCancel} className="btn-ink-ghost text-sm">
            取消
          </button>
          <button onClick={onConfirm} className={`btn-ink text-sm ${risk === 'HIGH' || risk === 'CRITICAL' ? 'btn-ink-danger' : ''}`}>
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  )
}
