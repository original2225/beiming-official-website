/**
 * 权限保护路由 — 未登录跳转登录页，角色不足展示无权限
 */

import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '../../store/authStore'
import { ROUTES } from '../../constants/routes'
import type { Role } from '../../types/common'

interface ProtectedRouteProps {
  children: ReactNode
  requiredRole?: Role
}

export function ProtectedRoute({ children, requiredRole }: ProtectedRouteProps) {
  const { isAuthenticated, user } = useAuthStore()

  // 开发模式：后端未启动时绕过登录校验，用 mock 用户
  const devBypass = import.meta.env.DEV && !isAuthenticated && import.meta.env.VITE_DEV_BYPASS_AUTH !== 'false'

  if (!isAuthenticated && !devBypass) {
    return <Navigate to={ROUTES.LOGIN} replace />
  }

  if (devBypass && !user) {
    return <>{children}</>
  }

  if (requiredRole && user) {
    const rank: Record<Role, number> = { OWNER: 3, ADMIN: 2, HELPER: 1, USER: 0 }
    if (rank[user.role] < rank[requiredRole]) {
      return (
        <div className="flex items-center justify-center min-h-[50vh]">
          <p className="font-minecraft text-mc-redstone text-xl">权限不足</p>
        </div>
      )
    }
  }

  return <>{children}</>
}
