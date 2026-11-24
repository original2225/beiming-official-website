/**
 * 认证状态 — accessToken、当前用户、登录状态
 * 供 App.tsx、router.tsx、登录页和导航组件使用
 *
 * 持久化到 localStorage，刷新页面/直接访问受保护路由不会丢登录态。
 * 只存 token / user / isAuthenticated，不存任何密码。
 */

import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { CurrentUser } from '../types/common'

interface AuthState {
  token: string | null
  user: CurrentUser | null
  isAuthenticated: boolean

  setAuth: (token: string, user: CurrentUser) => void
  clearAuth: () => void
  setUser: (user: CurrentUser) => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      isAuthenticated: false,

      setAuth: (token, user) =>
        set({ token, user, isAuthenticated: true }),

      clearAuth: () =>
        set({ token: null, user: null, isAuthenticated: false }),

      setUser: (user) => set({ user }),
    }),
    {
      name: 'beiming-auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        token: state.token,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
)