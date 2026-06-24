/**
 * 防抖 hook — 搜索输入防抖
 * 供搜索框使用，避免每次按键都触发请求
 */

import { useState, useEffect } from 'react'

export function useDebouncedValue<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])

  return debounced
}
