/**
 * 认证状态 — accessToken、当前用户、登录状态
 * 供 App.tsx、router.tsx、登录页和导航组件使用
 */

import { create } from 'zustand'
import type { CurrentUser } from '../types/common'

interface AuthState {
  token: string | null
  user: CurrentUser | null
  isAuthenticated: boolean

  setAuth: (token: string, user: CurrentUser) => void
  clearAuth: () => void
  setUser: (user: CurrentUser) => void
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  user: null,
  isAuthenticated: false,

  setAuth: (token, user) =>
    set({ token, user, isAuthenticated: true }),

  clearAuth: () =>
    set({ token: null, user: null, isAuthenticated: false }),

  setUser: (user) => set({ user }),
}))
