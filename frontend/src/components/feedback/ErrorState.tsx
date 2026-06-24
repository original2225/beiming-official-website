/** 错误状态 — 接口失败或异常时展示 */
export function ErrorState({ message = '加载失败', onRetry }: { message?: string; onRetry?: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center py-12 gap-3">
      <span className="font-minecraft text-mc-redstone">{message}</span>
      {onRetry && (
        <button onClick={onRetry} className="btn-mc text-sm px-4 py-1">
          重试
        </button>
      )}
    </div>
  )
}
