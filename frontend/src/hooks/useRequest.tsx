/**
 * 页面级请求 hook — 管理 loading / error / data 三个状态
 * 供页面组件调用 API 时使用
 */

import { useState, useCallback } from 'react'
import { ApiError } from '../types/api'

interface UseRequestState<T> {
  data: T | null
  loading: boolean
  error: ApiError | null
}

interface UseRequestReturn<T> extends UseRequestState<T> {
  run: (fn: () => Promise<T>) => Promise<T | undefined>
  reset: () => void
}

export function useRequest<T>(): UseRequestReturn<T> {
  const [state, setState] = useState<UseRequestState<T>>({
    data: null,
    loading: false,
    error: null,
  })

  const run = useCallback(async (fn: () => Promise<T>): Promise<T | undefined> => {
    setState({ data: null, loading: true, error: null })
    try {
      const data = await fn()
      setState({ data, loading: false, error: null })
      return data
    } catch (e) {
      const error = e instanceof ApiError ? e : new ApiError(-1, String(e))
      setState({ data: null, loading: false, error })
    }
  }, [])

  const reset = useCallback(() => {
    setState({ data: null, loading: false, error: null })
  }, [])

  return { ...state, run, reset }
}
