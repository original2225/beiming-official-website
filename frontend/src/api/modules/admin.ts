/**
 * 后台管理 API — admin 聚合 + 各业务模块后台接口
 * 供 pages/admin/ 引用
 */

import { get, post, put, patch } from '../client'
import type {
  AdminContentPage,
  AdminOverviewView,
  AdminTodoPage,
  AdminUserPage,
  MetricsView,
} from '../../types/view-models'

// ── admin 聚合 ──
export const getAdminOverview = () => get<AdminOverviewView>('/api/v1/admin/overview')
export const getAdminModules = () => get<unknown>('/api/v1/admin/modules')
export const getAdminTodos = () => get<AdminTodoPage>('/api/v1/admin/todos')
export const getAdminMetrics = () => get<MetricsView>('/api/v1/admin/metrics/summary')
export const getAdminAuditLogs = (params?: Record<string, string | number | boolean | undefined>) => get<unknown>('/api/v1/admin/audit-logs', { params })
export const getAdminSettings = () => get<unknown>('/api/v1/admin/settings')
export const updateAdminSettings = (body: unknown) => patch<unknown>('/api/v1/admin/settings', body)

// ── auth 后台 ──
export const getAdminUsers = (params?: Record<string, string | number | boolean | undefined>) => get<AdminUserPage>('/api/v1/auth/admin/users', { params })
export const getAdminUser = (userId: string) => get<unknown>(`/api/v1/auth/admin/users/${userId}`)
export const updateAdminUser = (userId: string, body: unknown) => patch<unknown>(`/api/v1/auth/admin/users/${userId}`, body)
export const updateUserRoles = (userId: string, body: unknown) => put<unknown>(`/api/v1/auth/admin/users/${userId}/roles`, body)
export const getAdminInvitations = (params?: Record<string, string | number | boolean | undefined>) => get<unknown>('/api/v1/auth/admin/invitations', { params })
export const createInvitation = (body: unknown) => post<unknown>('/api/v1/auth/admin/invitations', body)
export const disableInvitation = (invitationId: string) => patch<unknown>(`/api/v1/auth/admin/invitations/${invitationId}/disable`)

// ── content 后台 ──
export const getAdminContent = (params?: Record<string, string | number | boolean | undefined>) => get<AdminContentPage>('/api/v1/content/admin/items', { params })
export const createContent = (body: unknown) => post<unknown>('/api/v1/content/admin/items', body)
export const updateContent = (contentId: string, body: unknown) => patch<unknown>(`/api/v1/content/admin/items/${contentId}`, body)
export const publishContent = (contentId: string) => patch<unknown>(`/api/v1/content/admin/items/${contentId}/publish`)
export const offlineContent = (contentId: string) => patch<unknown>(`/api/v1/content/admin/items/${contentId}/offline`)
export const archiveContent = (contentId: string) => patch<unknown>(`/api/v1/content/admin/items/${contentId}/archive`)
export const deleteContent = (contentId: string) => patch<unknown>(`/api/v1/content/admin/items/${contentId}/delete`)

// ── community 后台 ──
export const getAdminBoards = () => get<unknown>('/api/v1/community/admin/boards')
export const createBoard = (body: unknown) => post<unknown>('/api/v1/community/admin/boards', body)
export const getAdminPosts = (params?: Record<string, string | number | boolean | undefined>) => get<unknown>('/api/v1/community/admin/posts', { params })
export const approvePost = (postId: string) => patch<unknown>(`/api/v1/community/admin/posts/${postId}/approve`)
export const rejectPost = (postId: string) => patch<unknown>(`/api/v1/community/admin/posts/${postId}/reject`)
export const offlinePost = (postId: string) => patch<unknown>(`/api/v1/community/admin/posts/${postId}/offline`)
export const getAdminTickets = (params?: Record<string, string | number | boolean | undefined>) => get<unknown>('/api/v1/community/admin/tickets', { params })
export const getAdminTicket = (ticketId: string) => get<unknown>(`/api/v1/community/admin/tickets/${ticketId}`)
export const assignTicket = (ticketId: string, body: unknown) => patch<unknown>(`/api/v1/community/admin/tickets/${ticketId}/assign`, body)
export const replyTicket = (ticketId: string, body: unknown) => post<unknown>(`/api/v1/community/admin/tickets/${ticketId}/messages`, body)
export const getAdminReports = (params?: Record<string, string | number | boolean | undefined>) => get<unknown>('/api/v1/community/admin/reports', { params })

// ── whitelist 后台 ──
export const getAdminWhitelistApplications = (params?: Record<string, string | number | boolean | undefined>) => get<unknown>('/api/v1/whitelist/admin/applications', { params })
export const getAdminWhitelistApplication = (applicationId: string) => get<unknown>(`/api/v1/whitelist/admin/applications/${applicationId}`)
export const approveWhitelist = (applicationId: string) => patch<unknown>(`/api/v1/whitelist/admin/applications/${applicationId}/approve`)
export const rejectWhitelist = (applicationId: string) => patch<unknown>(`/api/v1/whitelist/admin/applications/${applicationId}/reject`)
export const requestSupplementWhitelist = (applicationId: string) => patch<unknown>(`/api/v1/whitelist/admin/applications/${applicationId}/request-supplement`)
export const removeWhitelist = (applicationId: string) => patch<unknown>(`/api/v1/whitelist/admin/applications/${applicationId}/remove`)

// ── attendance 后台 ──
export const getAdminAttendanceAccounts = (params?: Record<string, string | number | boolean | undefined>) => get<unknown>('/api/v1/attendance/admin/accounts', { params })
export const adjustAttendancePoints = (accountId: string, body: unknown) => post<unknown>(`/api/v1/attendance/admin/accounts/${accountId}/adjustments`, body)
export const getAdminRemovalCandidates = () => get<unknown>('/api/v1/attendance/admin/removal-candidates')
