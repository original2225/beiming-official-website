/**
 * 路由常量 — 路径和权限要求
 * 供 router.tsx 和导航组件引用
 */

/** 前端路由路径 */
export const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  REGISTER: '/register',

  // 公开内容
  ANNOUNCEMENTS: '/announcements',
  ANNOUNCEMENT_DETAIL: '/announcements/:id',
  GUIDES: '/guides',
  GUIDE_DETAIL: '/guides/:id',
  RESOURCES: '/resources',
  RESOURCE_DETAIL: '/resources/:id',
  SERVER_STATUS: '/status',
  MEMBERS: '/members',
  MATERIALS: '/materials',
  ACTIVITIES: '/activities',
  ACTIVITY_DETAIL: '/activities/:id',
  CALENDAR: '/calendar',
  CHANGELOG: '/changelog',

  // 用户中心
  PROFILE: '/me/profile',
  NOTIFICATIONS: '/me/notifications',
  SECURITY: '/me/security',
  ONBOARDING: '/me/onboarding',
  MY_EXAMS: '/me/exams',
  MY_WHITELIST: '/me/whitelist',
  MY_ATTENDANCE: '/me/attendance',

  // 社区
  BOARDS: '/community',
  BOARD: '/community/boards/:boardId',
  POST: '/community/posts/:postId',
  NEW_POST: '/community/new',

  // 后台
  ADMIN: '/admin',
  ADMIN_USERS: '/admin/users',
  ADMIN_CONTENT: '/admin/content',
  ADMIN_APPROVALS: '/admin/approvals',

  // 运维
  OPS: '/ops',
  OPS_NODES: '/ops/nodes',
  OPS_ASSETS: '/ops/assets',
} as const

/** 需要登录才能访问的路由前缀 */
export const PROTECTED_PREFIXES = ['/me', '/admin', '/ops']

/** 需要后台角色（HELPER+）的路由前缀 */
export const ADMIN_PREFIXES = ['/admin']

/** 需要运维能力点的路由前缀 */
export const OPS_PREFIXES = ['/ops']
