/**
 * 业务域类型 — 跨页面复用的业务摘要
 * 供 pages/ 各页面和 api/modules/ 引用
 */

import type { Status } from './common'

/** 通知列表项，通知中心和导航栏未读气泡用 */
export interface NotificationSummary {
  notificationId: string
  title: string
  read: boolean
  archived: boolean
  createdAt: string
}

/** 内容列表项，首页公告区、公告列表页、后台内容管理共用 */
export interface ContentSummary {
  contentId: string
  title: string
  slug: string
  category: string
  status: Status
  publishedAt?: string
  coverUrl?: string
}

/** 服务器状态总览，首页状态卡片和状态详情页用 */
export interface ServerStatusSummary {
  online: boolean
  playerCount: number
  maxPlayers: number
  version: string
  motd: string
  lines: LineSummary[]
}

/** 线路摘要，状态卡片内线路列表用 */
export interface LineSummary {
  lineId: string
  name: string
  address: string
  latency: number
  online: boolean
}

/** 资源列表项，资源中心和首页资源区块用 */
export interface ResourceSummary {
  resourceId: string
  title: string
  slug: string
  category: string
  version: string
  downloads: number
  coverUrl?: string
}

/** 指南列表项，指南中心和首页指南入口用 */
export interface GuideSummary {
  guideId: string
  title: string
  slug: string
  category: string
  updatedAt: string
}

/** 素材列表项，素材展示页和首页精选区用 */
export interface MaterialSummary {
  materialId: string
  title: string
  slug: string
  featured: boolean
  authorName: string
  coverUrl?: string
}

/** 活动列表项，活动列表页和首页活动日历用 */
export interface ActivitySummary {
  activityId: string
  title: string
  slug: string
  startTime: string
  endTime: string
  registrationOpen: boolean
  status: Status
}

/** 日历事件摘要，日历页和首页日程区用 */
export interface CalendarEventSummary {
  eventId: string
  title: string
  type: string
  startTime: string
  endTime: string
}

/** 更新日志列表项，更新日志页和首页版本信息用 */
export interface ChangelogSummary {
  releaseId: string
  version: string
  title: string
  slug: string
  publishedAt: string
  tags: string[]
}

/** 帖子列表项，板块帖子列表和搜索结果用 */
export interface PostSummary {
  postId: string
  title: string
  boardName: string
  authorName: string
  likes: number
  comments: number
  createdAt: string
}

/** 成员列表项，成员展示页和首页成员区用 */
export interface MemberSummary {
  memberId: string
  displayName: string
  groupName?: string
  avatarUrl?: string
  minecraftName?: string
}
