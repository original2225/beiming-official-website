/**
 * 权限判断 hook — 角色和能力点检查
 * 供路由守卫、按钮显隐、后台组件使用
 */

import { useCallback } from 'react'
import { useAuthStore } from '../store/authStore'
import type { Role, OpsCapability } from '../types/common'

const ROLE_RANK: Record<Role, number> = { OWNER: 3, ADMIN: 2, HELPER: 1, USER: 0 }

export function usePermissions() {
  const user = useAuthStore((s) => s.user)

  /** 当前用户角色是否 >= 指定角色 */
  const hasRole = useCallback((required: Role): boolean => {
    if (!user) return false
    return ROLE_RANK[user.role] >= ROLE_RANK[required]
  }, [user])

  /** 当前用户是否拥有指定运维能力点 */
  const hasCapability = useCallback((capability: OpsCapability): boolean => {
    if (!user) return false
    return user.permissions.includes(capability)
  }, [user])

  /** 是否可进入后台（HELPER+） */
  const canAccessAdmin = hasRole('HELPER')

  /** 是否可进入运维控制台 */
  const canAccessOps = hasCapability('NODE_READ')

  return { user, hasRole, hasCapability, canAccessAdmin, canAccessOps }
}
