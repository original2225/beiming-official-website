/**
 * 分页 hook — 管理页码和每页条数
 * 供列表页面使用
 */

import { useState, useCallback } from 'react'

interface UsePaginationOptions {
  defaultPage?: number
  defaultPageSize?: number
}

export function usePagination({ defaultPage = 1, defaultPageSize = 20 }: UsePaginationOptions = {}) {
  const [page, setPage] = useState(defaultPage)
  const [pageSize] = useState(defaultPageSize)

  const goTo = useCallback((p: number) => setPage(p), [])
  const reset = useCallback(() => setPage(1), [])

  return { page, pageSize, goTo, reset }
}
