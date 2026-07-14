/** 加载状态 — 接口请求中展示 */
export function LoadingState({ text = '加载中...' }: { text?: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-12 gap-3">
      <div className="relative w-8 h-8">
        <div className="absolute inset-0 border-2 border-indigo rounded-full animate-[ink-ripple_1.2s_ease-out_infinite]" />
        <div className="absolute inset-0 border-2 border-indigo rounded-full animate-[ink-ripple_1.2s_ease-out_infinite_0.6s]" />
      </div>
      <span className="font-display text-text-muted">{text}</span>
    </div>
  )
}
