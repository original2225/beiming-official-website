/**
 * 登录态 hook — 封装 authStore 的常用操作
 * 供登录页、导航组件和登出逻辑使用
 */

import { useCallback } from 'react'
import { useAuthStore } from '../store/authStore'
import type { CurrentUser } from '../types/common'

export function useAuth() {
  const token = useAuthStore((s) => s.token)
  const user = useAuthStore((s) => s.user)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const setAuth = useAuthStore((s) => s.setAuth)
  const clearAuth = useAuthStore((s) => s.clearAuth)

  const login = useCallback((token: string, user: CurrentUser) => {
    setAuth(token, user)
  }, [setAuth])

  const logout = useCallback(() => {
    clearAuth()
  }, [clearAuth])

  return { token, user, isAuthenticated, login, logout }
}
