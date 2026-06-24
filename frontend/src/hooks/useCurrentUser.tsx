/**
 * 当前用户 hook — 从 authStore 读取用户信息
 * 供需要用户信息的页面和组件使用
 */

import { useAuthStore } from '../store/authStore'
import type { CurrentUser } from '../types/common'

export function useCurrentUser(): CurrentUser | null {
  return useAuthStore((s) => s.user)
}
