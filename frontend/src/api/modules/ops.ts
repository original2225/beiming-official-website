/**
 * 运维控制台 API — ops-control / backup-recovery / alerting / api-gateway
 * 供 pages/ops/ 引用。字段结构以 docs/api-reference.md 的 ops-control 章节为准。
 */

import { get, patch, post } from '../client'

// ── api-gateway ──
export const getGatewayHealth = () => get<unknown>('/api/v1/gateway/health')
export const getGatewayRoutes = () => get<unknown>('/api/v1/gateway/admin/routes')
export const getGatewayUpstreams = () => get<unknown>('/api/v1/gateway/admin/upstreams')

// ── ops-control：概览与资产 ──
export const getOpsOverview = () => get<OpsOverview>('/api/v1/ops-control/overview')

export const getOpsAssets = (params?: QueryParams) =>
  get<PageResult<OpsAsset>>('/api/v1/ops-control/assets', { params })

export const getOpsAsset = (assetId: string) =>
  get<OpsAsset>(`/api/v1/ops-control/assets/${assetId}`)

// ── ops-control：节点 ──
export const getOpsNodes = (params?: QueryParams) =>
  get<PageResult<OpsNode>>('/api/v1/ops-control/nodes', { params })

export const getOpsNode = (nodeId: string) =>
  get<OpsNode>(`/api/v1/ops-control/nodes/${nodeId}`)

export const getOpsNodeMetrics = (nodeId: string) =>
  get<OpsNodeMetric>(`/api/v1/ops-control/nodes/${nodeId}/metrics/latest`)

export const getOpsNodeContainers = (nodeId: string) =>
  get<PageResult<OpsContainer>>(`/api/v1/ops-control/nodes/${nodeId}/containers`)

export const getOpsNodeVms = (nodeId: string) =>
  get<PageResult<OpsVm>>(`/api/v1/ops-control/nodes/${nodeId}/vms`)

export const getOpsNodeMinecraft = (nodeId: string) =>
  get<PageResult<OpsMinecraftInstance>>(`/api/v1/ops-control/nodes/${nodeId}/minecraft-instances`)

export const getOpsNodeCapabilities = (nodeId: string) =>
  get<{ nodeId: string; nodeCapabilities: string[]; currentUserCapabilities: string[] }>(
    `/api/v1/ops-control/nodes/${nodeId}/capabilities`,
  )

// ── ops-control：任务与审批 ──
export const getOpsTasks = (params?: QueryParams) =>
  get<PageResult<OpsTask>>('/api/v1/ops-control/tasks', { params })

export const createOpsTask = (body: Record<string, unknown>) =>
  post<OpsTask>('/api/v1/ops-control/tasks', body)

export const cancelOpsTask = (taskId: string, body: Record<string, unknown>) =>
  patch<OpsTask>(`/api/v1/ops-control/tasks/${taskId}/cancel`, body)

export const getOpsApprovals = (params?: QueryParams) =>
  get<PageResult<OpsApproval>>('/api/v1/ops-control/approvals', { params })

export const approveOpsApproval = (approvalId: string, body: Record<string, unknown>) =>
  patch<{ approval: OpsApproval; task: OpsTask }>(
    `/api/v1/ops-control/approvals/${approvalId}/approve`,
    body,
  )

export const rejectOpsApproval = (approvalId: string, body: Record<string, unknown>) =>
  patch<{ approval: OpsApproval; task: OpsTask }>(
    `/api/v1/ops-control/approvals/${approvalId}/reject`,
    body,
  )

export const getOpsAuditLogs = (params?: QueryParams) =>
  get<PageResult<OpsAuditLog>>('/api/v1/ops-control/audit-logs', { params })

export const getOpsSummary = () => get<OpsSummary>('/api/v1/ops-control/ops/summary')

// ── backup-recovery ──
export const getBackupDomains = () => get<unknown>('/api/v1/backup-recovery/domains')
export const getBackupPolicies = () => get<unknown>('/api/v1/backup-recovery/policies')
export const getBackupJobs = (params?: QueryParams) => get<unknown>('/api/v1/backup-recovery/jobs', { params })
export const getBackupPoints = () => get<unknown>('/api/v1/backup-recovery/backup-points')

// ── alerting ──
export const getAlertSources = () => get<unknown>('/api/v1/alerting/sources')
export const getAlertRules = () => get<unknown>('/api/v1/alerting/rules')
export const getAlerts = (params?: QueryParams) => get<unknown>('/api/v1/alerting/alerts', { params })
export const acknowledgeAlert = (alertId: string) => patch<unknown>(`/api/v1/alerting/alerts/${alertId}/acknowledge`)
export const closeAlert = (alertId: string) => patch<unknown>(`/api/v1/alerting/alerts/${alertId}/close`)

// ── 类型 ──
export type QueryParams = Record<string, string | number | boolean | undefined>

export interface PageResult<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}

export interface OpsOverview {
  nodesTotal: number
  onlineNodesTotal: number
  offlineNodesTotal: number
  assetsTotal: number
  pendingApprovalsTotal: number
  runningTasksTotal: number
  degradedModules: string[]
  recentAudits: OpsAuditLog[]
  generatedFor: string
}

export interface OpsNode {
  nodeId: string
  displayName: string
  status: string
  endpointSummary: string
  version: string | null
  capabilities: string[]
  labels: Record<string, string>
  lastHeartbeatAt: string | null
  lastSeenRequestId: string | null
  tokenDigest: string
  createdBy: string
  updatedBy: string
  createdAt: string
  updatedAt: string
}

export interface OpsNodeMetric {
  snapshotId?: string
  nodeId?: string
  cpuUsagePercent?: number
  memoryUsagePercent?: number
  diskUsagePercent?: number
  networkRxBytes?: number
  networkTxBytes?: number
  collectedAt?: string
  degraded?: boolean
}

export interface OpsAsset {
  assetId: string
  nodeId: string
  assetType: string
  displayName: string
  status: string
  ownerModule: string | null
  publicVisible: boolean
  riskTags: string[]
  createdAt: string
  updatedAt: string
}

export interface OpsContainer {
  containerId: string
  nodeId: string
  name: string
  image: string
  runtime: string
  status: string
  lastSyncedAt: string
  stale: boolean
}

export interface OpsVm {
  vmId: string
  nodeId: string
  name: string
  platform: string
  status: string
  lastSyncedAt: string
}

export interface OpsMinecraftInstance {
  instanceId: string
  nodeId: string
  publicInstanceId: string
  name: string
  version: string
  status: string
  onlinePlayers: number
  lastSyncedAt: string
}

export interface OpsTask {
  taskId: string
  taskType: string
  status: string
  riskLevel: string
  nodeId: string
  targetType: string
  targetId: string
  reason: string
  approvalId: string | null
  nodeRequestId: string | null
  failureReason: string | null
  createdBy: string
  createdAt: string
  updatedAt: string
  expiresAt: string
}

export interface OpsApproval {
  approvalId: string
  taskId: string
  status: string
  riskLevel: string
  requestedBy: string
  approvedBy: string | null
  reviewComment: string | null
  createdAt: string
  reviewedAt: string | null
  expiresAt: string
}

export interface OpsAuditLog {
  id: string
  requestId: string
  actorUserId: string
  actorRole: string
  actorPermissions: string[]
  targetType: string
  targetId: string
  action: string
  riskLevel: string
  reason: string | null
  result: string
  failureReason: string | null
  createdAt: string
}

export interface OpsSummary {
  service: string
  port: number
  storageMode: string
  authMode: string
  adminAdapterMode: string
  nodeAdapterMode: string
  externalExecutorConnected: boolean
  nodesTotal: number
  assetsTotal: number
  tasksTotal: number
  pendingApprovalsTotal: number
  productionGaps: string[]
  lastHeartbeatAt: string | null
  lastAuditAt: string | null
}