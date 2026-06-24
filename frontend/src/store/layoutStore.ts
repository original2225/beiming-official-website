/**
 * 布局状态 — 侧边栏、主题偏好、移动端菜单
 * 供 layout/ 组件使用
 */

import { create } from 'zustand'

interface LayoutState {
  sidebarOpen: boolean
  mobileMenuOpen: boolean
  theme: 'dark' | 'light'

  toggleSidebar: () => void
  setSidebarOpen: (open: boolean) => void
  toggleMobileMenu: () => void
  setMobileMenuOpen: (open: boolean) => void
  setTheme: (theme: 'dark' | 'light') => void
}

export const useLayoutStore = create<LayoutState>((set) => ({
  sidebarOpen: true,
  mobileMenuOpen: false,
  theme: 'dark',

  toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),
  setSidebarOpen: (open) => set({ sidebarOpen: open }),
  toggleMobileMenu: () => set((s) => ({ mobileMenuOpen: !s.mobileMenuOpen })),
  setMobileMenuOpen: (open) => set({ mobileMenuOpen: open }),
  setTheme: (theme) => set({ theme }),
}))
