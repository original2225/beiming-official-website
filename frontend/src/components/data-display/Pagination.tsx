/** 分页器 — 列表分页 */
interface PaginationProps {
  page: number
  pageSize: number
  total: number
  onChange: (page: number) => void
}

export function Pagination({ page, pageSize, total, onChange }: PaginationProps) {
  const totalPages = Math.max(1, Math.ceil(total / pageSize))

  return (
    <div className="flex items-center justify-center gap-2 py-4">
      <button
        onClick={() => onChange(page - 1)}
        disabled={page <= 1}
        className="btn-ink-ghost text-xs px-3 py-1 rounded-md disabled:opacity-40"
      >
        上一页
      </button>
      <span className="text-sm text-indigo bg-indigo/20 border border-indigo px-3 py-1 rounded-md">
        {page} / {totalPages}
      </span>
      <button
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages}
        className="btn-ink-ghost text-xs px-3 py-1 rounded-md disabled:opacity-40"
      >
        下一页
      </button>
    </div>
  )
}
