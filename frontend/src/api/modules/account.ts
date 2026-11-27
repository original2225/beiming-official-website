/**
 * 账号与用户中心 API — auth / profile / notification / onboarding / whitelist / exam / attendance
 * 供 pages/account/、pages/onboarding/、store/authStore.ts 引用
 */

import { get, post, put, patch, del } from '../client'
import * as ep from '../endpoints'
import { ApiError, type PageResult } from '../../types/api'
import type { CurrentUser } from '../../types/common'
import type { OpsCapability, Role } from '../../types/common'
import type { NotificationSummary } from '../../types/domain'
import type {
  AttendanceAccountView,
  ExamSessionPage,
  NextActionView,
  OnboardingProgressView,
  ProfileView,
  RankingView,
  WhitelistApplicationPage,
} from '../../types/view-models'

const ROLE_PRIORITY: Role[] = ['OWNER', 'ADMIN', 'HELPER', 'USER']

interface BackendMinecraftBinding {
  minecraftId?: string
  minecraftUuid?: string
}

interface BackendUserSummary {
  id: string
  username: string
  roles?: string[]
  permissions?: string[]
  minecraftBinding?: BackendMinecraftBinding | null
}

interface BackendSessionPayload {
  accessToken: string
  user: BackendUserSummary
}

interface LoginResult {
  token: string
  user: CurrentUser
}

function mapBackendUser(user: BackendUserSummary): CurrentUser {
  const roles = user.roles ?? []
  const role = ROLE_PRIORITY.find((candidate) => roles.includes(candidate)) ?? 'USER'
  const minecraftBinding = user.minecraftBinding ?? null

  return {
    userId: user.id,
    username: user.username,
    role,
    permissions: (user.permissions ?? []) as OpsCapability[],
    minecraftName: minecraftBinding?.minecraftId,
    minecraftUuid: minecraftBinding?.minecraftUuid,
  }
}

function ensureObject(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' ? value as Record<string, unknown> : {}
}

// ── auth ──
export const register = async (body: unknown): Promise<LoginResult> => {
  const payload = ensureObject(body)
  // 后端 register 直接返回 accessToken + 完整 user，和 login 是同一种 payload。
  // 之前这里吞掉了 token 只当 unknown 透传，导致注册成功后还得让用户再登录一遍。
  const session = await post<BackendSessionPayload>(ep.auth.register(), {
    ...payload,
    displayName: payload.displayName ?? payload.username,
  })
  return { token: session.accessToken, user: mapBackendUser(session.user) }
}
export const login = async (body: unknown): Promise<LoginResult> => {
  const session = await post<BackendSessionPayload>(ep.auth.login(), body)
  return { token: session.accessToken, user: mapBackendUser(session.user) }
}
export const logout = () => post<unknown>(ep.auth.logout())
export const getMe = async () => mapBackendUser(await get<BackendUserSummary>(ep.auth.me()))
export const verifySession = () => get<unknown>(ep.auth.sessionVerify())
export const getMeSessions = () => get<unknown>(ep.auth.meSessions())
export const deleteMeSession = (sessionId: string) => del<unknown>(ep.auth.meSession(sessionId), { reason: 'USER_SESSION_REVOKE' })
export const changePassword = (body: unknown) => {
  const payload = ensureObject(body)
  return post<unknown>(ep.auth.mePassword(), {
    currentPassword: payload.currentPassword ?? payload.oldPassword,
    newPassword: payload.newPassword,
    reason: payload.reason ?? 'USER_PASSWORD_CHANGE',
  })
}
export const requestPasswordReset = (body: unknown) => post<unknown>(ep.auth.passwordResetRequest(), body)
export const confirmPasswordReset = (body: unknown) => post<unknown>(ep.auth.passwordResetConfirm(), body)
/**
 * 绑定游戏账号。
 *
 * 两条路径：
 * - 主路径：只传 minecraftName + minecraftPlatform，UUID 由服务端按平台去查。
 *   玩家不需要知道自己的 UUID 是多少，也不该被要求填。
 * - 兼容路径：调用方自带 minecraftId + minecraftUuid（管理员手工写入场景）时直传。
 *
 * 旧版这里强制要求 minecraftUuid 和 verificationCode，而 verificationCode 后端
 * 从未校验过——等于逼前端凑一个假验证码，主路径反而走不通。
 */
export const bindMinecraft = (body: unknown) => {
  const payload = ensureObject(body)
  if (payload.minecraftId && payload.minecraftUuid) {
    return put<unknown>(ep.auth.minecraftBinding(), {
      minecraftId: payload.minecraftId,
      minecraftUuid: payload.minecraftUuid,
    })
  }
  const name = payload.minecraftName
  const platform = payload.minecraftPlatform
  if (!name || !platform) {
    return Promise.reject(new ApiError(40001, '请选择游戏平台并填写游戏昵称'))
  }
  return put<unknown>(ep.auth.minecraftBinding(), {
    minecraftName: name,
    minecraftPlatform: platform,
  })
}
export const unbindMinecraft = () => del<unknown>(ep.auth.minecraftBinding(), { reason: 'USER_MINECRAFT_UNBIND' })

// ── profile ──
export const getMyProfile = () => get<ProfileView>(ep.profile.me())
export const updateMyProfile = (body: unknown) => patch<unknown>(ep.profile.me(), body)

// ── notification ──
export const getMyNotifications = (params?: Record<string, string | number | boolean | undefined>) => get<PageResult<NotificationSummary>>(ep.notification.meList(), { params })
export const getUnreadCount = () => get<{ unreadCount: number }>(ep.notification.meUnreadCount())
export const getNotificationDetail = (notificationId: string) => get<unknown>(ep.notification.meDetail(notificationId))
export const markNotificationRead = (notificationId: string) => patch<unknown>(ep.notification.meRead(notificationId))
export const markAllNotificationsRead = () => patch<unknown>(ep.notification.meReadAll())
export const archiveNotification = (notificationId: string) => patch<unknown>(ep.notification.meArchive(notificationId))

// ── onboarding ──
export const getOnboardingProgress = () => get<OnboardingProgressView>(ep.onboarding.meProgress())
export const startOnboarding = () => post<unknown>(ep.onboarding.meStart())
export const confirmProfile = (body: unknown) => patch<unknown>(ep.onboarding.meProfileConfirmation(), body)
export const confirmRules = (body: unknown) => patch<unknown>(ep.onboarding.meRulesConfirmation(), body)
export const chooseDirection = (body: unknown) => patch<unknown>(ep.onboarding.meDirection(), body)
export const advanceOnboarding = () => post<unknown>(ep.onboarding.meAdvance())
export const getNextAction = () => get<NextActionView>(ep.onboarding.meNextAction())

// ── exam ──
export const getMyExamSessions = () => get<ExamSessionPage>(ep.exam.meSessions())
export const getCurrentExamSession = () => get<unknown>(ep.exam.meCurrentSession())
export const createExamSession = () => post<unknown>(ep.exam.meSessions())
export const getExamPaper = (sessionId: string) => get<unknown>(ep.exam.meSessionPaper(sessionId))
export const saveExamAnswers = (sessionId: string, body: unknown) => put<unknown>(ep.exam.meSessionAnswers(sessionId), body)
export const submitExam = (sessionId: string) => post<unknown>(ep.exam.meSessionSubmit(sessionId))
export const supplementExam = (sessionId: string, body: unknown) => patch<unknown>(ep.exam.meSessionSupplement(sessionId), body)
export const getExamResult = (sessionId: string) => get<unknown>(ep.exam.meSessionResult(sessionId))

// ── whitelist ──
export const getMyWhitelistApplications = () => get<WhitelistApplicationPage>(ep.whitelist.meApplications())
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
export const getMyAttendanceAccount = () => get<AttendanceAccountView>(ep.attendance.meAccount())
export const getMyAttendanceLedger = (params?: Record<string, string | number | boolean | undefined>) => get<unknown>(ep.attendance.meLedger(), { params })
export const getMyContributions = () => get<unknown>(ep.attendance.meContributions())
export const getMyRanking = () => get<RankingView>(ep.attendance.meRanking())
