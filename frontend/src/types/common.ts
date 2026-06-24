/**
 * 通用类型 — 角色、能力点、风险等级、状态、审计摘要、当前用户
 * 供 store/、hooks/usePermissions.ts、后台页面、审计组件引用
 */

/** 基础角色，前端用于隐藏入口和路由守卫，最终权限以后端返回为准 */
export type Role = 'OWNER' | 'ADMIN' | 'HELPER' | 'USER'

/** 运维能力点，进入运维控制台和操作节点/容器/文件/终端时校验 */
export type OpsCapability =
  | 'NODE_READ'
  | 'NODE_WRITE'
  | 'CONTAINER_OPERATE'
  | 'VM_OPERATE'
  | 'FILE_MANAGE'
  | 'TERMINAL_ACCESS'
  | 'HIGH_RISK_APPROVE'

/** 操作风险等级，后台/运维页在确认弹窗和按钮样式上使用 */
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

/** 通用内容状态，content/resource/material/guide/community 共用 */
export type Status = 'DRAFT' | 'ACTIVE' | 'PUBLISHED' | 'OFFLINE' | 'ARCHIVED' | 'DELETED'

/** 审计时间字段，后台详情页和审计组件展示用 */
export interface AuditSummary {
  createdAt: string
  updatedAt: string
  createdBy?: string
  updatedBy?: string
}

/** 当前登录用户摘要，登录成功后写入 authStore */
export interface CurrentUser {
  userId: string
  username: string
  role: Role
  permissions: OpsCapability[]
  minecraftUuid?: string
  minecraftName?: string
}
