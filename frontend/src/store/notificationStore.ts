/**
 * 通知状态 — 未读数、最近刷新时间
 * 供导航栏通知图标和通知中心使用
 */

import { create } from 'zustand'

interface NotificationState {
  unreadCount: number
  lastFetchedAt: number | null

  setUnreadCount: (count: number) => void
  incrementUnread: (by?: number) => void
  decrementUnread: (by?: number) => void
  markFetched: () => void
}

export const useNotificationStore = create<NotificationState>((set) => ({
  unreadCount: 0,
  lastFetchedAt: null,

  setUnreadCount: (count) => set({ unreadCount: count }),
  incrementUnread: (by = 1) => set((s) => ({ unreadCount: s.unreadCount + by })),
  decrementUnread: (by = 1) => set((s) => ({ unreadCount: Math.max(0, s.unreadCount - by) })),
  markFetched: () => set({ lastFetchedAt: Date.now() }),
}))
