/** 空状态 — 列表无数据时展示 */
export function EmptyState({ text = '暂无数据' }: { text?: string }) {
  return (
    <div className="flex items-center justify-center py-12">
      <span className="text-text-muted">{text}</span>
    </div>
  )
}
