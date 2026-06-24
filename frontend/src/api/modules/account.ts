/**
 * 账号与用户中心 API — auth / profile / notification / onboarding / whitelist / exam / attendance
 * 供 pages/account/、pages/onboarding/、store/authStore.ts 引用
 */

import { get, post, put, patch, del } from '../client'
import * as ep from '../endpoints'
import type { PageResult } from '../../types/api'
import type { CurrentUser } from '../../types/common'
import type { NotificationSummary } from '../../types/domain'

// ── auth ──
export const register = (body: unknown) => post<unknown>(ep.auth.register(), body)
export const login = (body: unknown) => post<{ token: string; user: CurrentUser }>(ep.auth.login(), body)
export const logout = () => post<unknown>(ep.auth.logout())
export const getMe = () => get<CurrentUser>(ep.auth.me())
export const verifySession = () => get<unknown>(ep.auth.sessionVerify())
export const getMeSessions = () => get<unknown>(ep.auth.meSessions())
export const deleteMeSession = (sessionId: string) => del<unknown>(ep.auth.meSession(sessionId))
export const changePassword = (body: unknown) => post<unknown>(ep.auth.mePassword(), body)
export const requestPasswordReset = (body: unknown) => post<unknown>(ep.auth.passwordResetRequest(), body)
export const confirmPasswordReset = (body: unknown) => post<unknown>(ep.auth.passwordResetConfirm(), body)
export const bindMinecraft = (body: unknown) => put<unknown>(ep.auth.minecraftBinding(), body)
export const unbindMinecraft = () => del<unknown>(ep.auth.minecraftBinding())

// ── profile ──
export const getMyProfile = () => get<unknown>(ep.profile.me())
export const updateMyProfile = (body: unknown) => patch<unknown>(ep.profile.me(), body)

// ── notification ──
export const getMyNotifications = (params?: Record<string, string | number | boolean | undefined>) => get<PageResult<NotificationSummary>>(ep.notification.meList(), { params })
export const getUnreadCount = () => get<{ count: number }>(ep.notification.meUnreadCount())
export const getNotificationDetail = (notificationId: string) => get<unknown>(ep.notification.meDetail(notificationId))
export const markNotificationRead = (notificationId: string) => patch<unknown>(ep.notification.meRead(notificationId))
export const markAllNotificationsRead = () => patch<unknown>(ep.notification.meReadAll())
export const archiveNotification = (notificationId: string) => patch<unknown>(ep.notification.meArchive(notificationId))

// ── onboarding ──
export const getOnboardingProgress = () => get<unknown>(ep.onboarding.meProgress())
export const startOnboarding = () => post<unknown>(ep.onboarding.meStart())
export const confirmProfile = (body: unknown) => patch<unknown>(ep.onboarding.meProfileConfirmation(), body)
export const confirmRules = (body: unknown) => patch<unknown>(ep.onboarding.meRulesConfirmation(), body)
export const chooseDirection = (body: unknown) => patch<unknown>(ep.onboarding.meDirection(), body)
export const advanceOnboarding = () => post<unknown>(ep.onboarding.meAdvance())
export const getNextAction = () => get<unknown>(ep.onboarding.meNextAction())

// ── exam ──
export const getMyExamSessions = () => get<unknown>(ep.exam.meSessions())
export const getCurrentExamSession = () => get<unknown>(ep.exam.meCurrentSession())
export const createExamSession = () => post<unknown>(ep.exam.meSessions())
export const getExamPaper = (sessionId: string) => get<unknown>(ep.exam.meSessionPaper(sessionId))
export const saveExamAnswers = (sessionId: string, body: unknown) => put<unknown>(ep.exam.meSessionAnswers(sessionId), body)
export const submitExam = (sessionId: string) => post<unknown>(ep.exam.meSessionSubmit(sessionId))
export const supplementExam = (sessionId: string, body: unknown) => patch<unknown>(ep.exam.meSessionSupplement(sessionId), body)
export const getExamResult = (sessionId: string) => get<unknown>(ep.exam.meSessionResult(sessionId))

// ── whitelist ──
export const getMyWhitelistApplications = () => get<unknown>(ep.whitelist.meApplications())
export const getCurrentWhitelistApplication = () => get<unknown>(ep.whitelist.meCurrentApplication())
export const createWhitelistApplication = (body: unknown) => post<unknown>(ep.whitelist.meApplications(), body)
export const getWhitelistApplication = (applicationId: string) => get<unknown>(ep.whitelist.meApplication(applicationId))
export const updateWhitelistApplication = (applicationId: string, body: unknown) => patch<unknown>(ep.whitelist.meApplication(applicationId), body)
export const submitWhitelist = (applicationId: string) => post<unknown>(ep.whitelist.meSubmit(applicationId))
export const supplementWhitelist = (applicationId: string, body: unknown) => patch<unknown>(ep.whitelist.meSupplement(applicationId), body)
export const withdrawWhitelist = (applicationId: string) => patch<unknown>(ep.whitelist.meWithdraw(applicationId))
export const getWhitelistResult = (applicationId: string) => get<unknown>(ep.whitelist.meResult(applicationId))

// ── attendance ──
export const getLeaderboard = () => get<unknown>(ep.attendance.leaderboard())
export const getMyAttendanceAccount = () => get<unknown>(ep.attendance.meAccount())
export const getMyAttendanceLedger = (params?: Record<string, string | number | boolean | undefined>) => get<unknown>(ep.attendance.meLedger(), { params })
export const getMyContributions = () => get<unknown>(ep.attendance.meContributions())
export const getMyRanking = () => get<unknown>(ep.attendance.meRanking())
