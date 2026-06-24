/**
 * 运维控制台 API — ops-control / node-daemon / cloudreve-sync / backup-recovery / alerting / plugin-integration / ops-image-market / api-gateway
 * 供 pages/ops/ 引用（P1/P2 阶段实现）
 */

import { get, patch } from '../client'

// ── api-gateway ──
export const getGatewayHealth = () => get<unknown>('/api/v1/gateway/health')
export const getGatewayRoutes = () => get<unknown>('/api/v1/gateway/admin/routes')
export const getGatewayUpstreams = () => get<unknown>('/api/v1/gateway/admin/upstreams')

// ── ops-control ──
export const getOpsOverview = () => get<unknown>('/api/v1/ops-control/overview')
export const getOpsAssets = (params?: Record<string, string | number | boolean | undefined>) => get<unknown>('/api/v1/ops-control/assets', { params })
export const getOpsNodes = () => get<unknown>('/api/v1/ops-control/nodes')
export const getOpsNode = (nodeId: string) => get<unknown>(`/api/v1/ops-control/nodes/${nodeId}`)
export const getOpsNodeMetrics = (nodeId: string) => get<unknown>(`/api/v1/ops-control/nodes/${nodeId}/metrics/latest`)

// ── backup-recovery ──
export const getBackupDomains = () => get<unknown>('/api/v1/backup-recovery/domains')
export const getBackupPolicies = () => get<unknown>('/api/v1/backup-recovery/policies')
export const getBackupJobs = (params?: Record<string, string | number | boolean | undefined>) => get<unknown>('/api/v1/backup-recovery/jobs', { params })
export const getBackupPoints = () => get<unknown>('/api/v1/backup-recovery/backup-points')

// ── alerting ──
export const getAlertSources = () => get<unknown>('/api/v1/alerting/sources')
export const getAlertRules = () => get<unknown>('/api/v1/alerting/rules')
export const getAlerts = (params?: Record<string, string | number | boolean | undefined>) => get<unknown>('/api/v1/alerting/alerts', { params })
export const acknowledgeAlert = (alertId: string) => patch<unknown>(`/api/v1/alerting/alerts/${alertId}/acknowledge`)
export const closeAlert = (alertId: string) => patch<unknown>(`/api/v1/alerting/alerts/${alertId}/close`)
