/**
 * 权限常量 — 基础角色和运维能力点
 * 供 hooks/usePermissions.ts 和路由守卫引用
 */

import type { Role, OpsCapability } from '../types/common'

/** 所有角色 */
export const ROLES: Role[] = ['OWNER', 'ADMIN', 'HELPER', 'USER']

/** 允许进入后台的角色 */
export const ADMIN_ROLES: Role[] = ['OWNER', 'ADMIN', 'HELPER']

/** 允许后台写操作的角色 */
export const ADMIN_WRITE_ROLES: Role[] = ['OWNER', 'ADMIN']

/** 所有运维能力点 */
export const OPS_CAPABILITIES: OpsCapability[] = [
  'NODE_READ',
  'NODE_WRITE',
  'CONTAINER_OPERATE',
  'VM_OPERATE',
  'FILE_MANAGE',
  'TERMINAL_ACCESS',
  'HIGH_RISK_APPROVE',
]
