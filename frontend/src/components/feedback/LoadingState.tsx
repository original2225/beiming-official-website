/** 加载状态 — 接口请求中展示 */
export function LoadingState({ text = '加载中...' }: { text?: string }) {
  return (
    <div className="flex items-center justify-center py-12">
      <span className="font-minecraft text-text-muted animate-pulse">{text}</span>
    </div>
  )
}
