/**
 * 全局 Provider 层 — 初始化 tokenGetter，让 api client 能自动读取 token
 *
 * 关键：必须在模块加载时就注册，不能放在 useEffect 里。
 * React effect 执行顺序是「子组件先、父组件后」，子页面首个请求会早于
 * Provider 的 effect，那时 getter 还没注册，请求就不带 Authorization，
 * 后端只能回 unauthenticated。getter 内部读 zustand 实时 state，
 * 提前注册不会读到过期 token。
 */

import type { ReactNode } from 'react'
import { useAuthStore } from '../store/authStore'
import { setTokenGetter } from '../api/client'

setTokenGetter(() => useAuthStore.getState().token)

export function AppProviders({ children }: { children: ReactNode }) {
  return <>{children}</>
}