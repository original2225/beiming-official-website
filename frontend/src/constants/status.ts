/**
 * 状态常量 — 通用状态、风险等级展示文案
 * 供状态徽标、风险徽标和数据展示组件引用
 */

import type { Status, RiskLevel } from '../types/common'

/** 状态展示文案 */
export const STATUS_LABELS: Record<Status, string> = {
  DRAFT: '草稿',
  ACTIVE: '待审核',
  PUBLISHED: '已发布',
  OFFLINE: '已下架',
  ARCHIVED: '已归档',
  DELETED: '已删除',
}

/** 风险等级展示文案和颜色 */
export const RISK_LABELS: Record<RiskLevel, { label: string; color: string }> = {
  LOW: { label: '低风险', color: '#17DD62' },
  MEDIUM: { label: '中风险', color: '#F5A623' },
  HIGH: { label: '高风险', color: '#C11E1E' },
  CRITICAL: { label: '严重', color: '#AA00FF' },
}
