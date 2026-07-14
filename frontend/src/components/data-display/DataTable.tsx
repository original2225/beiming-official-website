/** 数据表格 — 后台列表页通用 */
import type { ReactNode } from 'react'

interface Column<T> {
  key: string
  header: string
  render: (row: T) => ReactNode
  width?: string
}

interface DataTableProps<T> {
  columns: Column<T>[]
  data: T[]
  rowKey: (row: T) => string
}

export function DataTable<T>({ columns, data, rowKey }: DataTableProps<T>) {
  return (
    <div className="overflow-x-auto border border-ink-600/30 rounded-lg">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-surface border-b border-ink-600/30">
            {columns.map((col) => (
              <th key={col.key} className="text-left px-3 py-2 font-display text-xs text-text-muted uppercase" style={{ width: col.width }}>
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row) => (
            <tr key={rowKey(row)} className="border-b border-surface-light hover:bg-surface-light transition-colors">
              {columns.map((col) => (
                <td key={col.key} className="px-3 py-2 text-text-secondary">
                  {col.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
