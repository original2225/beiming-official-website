/**
 * 全局 Provider 层 — 初始化 tokenGetter，让 api client 能自动读取 token
 */

import { useEffect, type ReactNode } from 'react'
import { useAuthStore } from '../store/authStore'
import { setTokenGetter } from '../api/client'

export function AppProviders({ children }: { children: ReactNode }) {
  const token = useAuthStore((s) => s.token)

  useEffect(() => {
    setTokenGetter(() => useAuthStore.getState().token)
  }, [])

  // token 变化时无需重设 getter，因为 getter 读取的是 zustand 实时 state
  void token

  return <>{children}</>
}
