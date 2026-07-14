import { AlertCircle } from 'lucide-react'

/** 错误状态 — 接口失败或异常时展示 */
interface ErrorStateProps {
  message?: string
  onRetry?: () => void
}

export function ErrorState({ message = '加载失败', onRetry }: ErrorStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-12 gap-3">
      <AlertCircle size={32} className="text-cinnabar" />
      <span className="font-display text-cinnabar">{message}</span>
      {onRetry && (
        <button onClick={onRetry} className="btn-ink text-sm px-4 py-1">
          重试
        </button>
      )}
    </div>
  )
}
