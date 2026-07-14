/** 搜索筛选栏 — 后台列表页通用 */
import { useState } from 'react'

interface SearchFilterProps {
  placeholder?: string
  onSearch: (query: string) => void
}

export function SearchFilter({ placeholder = '搜索...', onSearch }: SearchFilterProps) {
  const [value, setValue] = useState('')

  return (
    <div className="flex gap-2">
      <input
        type="text"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={(e) => e.key === 'Enter' && onSearch(value)}
        placeholder={placeholder}
        className="bg-surface border border-ink-600 text-text-primary px-3 py-1 text-sm w-64
          placeholder:text-text-muted focus:border-indigo outline-none"
      />
      <button onClick={() => onSearch(value)} className="btn-ink text-xs px-3 py-1">
        搜索
      </button>
    </div>
  )
}
