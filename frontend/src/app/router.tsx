/**
 * 路由配置 — 公开页 / 用户中心 / 后台 / 运维四个分区
 */

import { Routes, Route } from 'react-router-dom'
import { ROUTES } from '../constants/routes'
import { ProtectedRoute } from '../components/layout/ProtectedRoute'

// 公开页
import { HomePage } from '../pages/public/HomePage'
import { AnnouncementListPage } from '../pages/public/AnnouncementListPage'
import { AnnouncementDetailPage } from '../pages/public/AnnouncementDetailPage'
import { GuideListPage } from '../pages/public/GuideListPage'
import { GuideDetailPage } from '../pages/public/GuideDetailPage'
import { ResourceListPage } from '../pages/public/ResourceListPage'
import { ResourceDetailPage } from '../pages/public/ResourceDetailPage'
import { ServerStatusPage } from '../pages/public/ServerStatusPage'
import { MemberListPage } from '../pages/public/MemberListPage'
import { MaterialListPage } from '../pages/public/MaterialListPage'
import { ActivityListPage } from '../pages/public/ActivityListPage'
import { ActivityDetailPage } from '../pages/public/ActivityDetailPage'
import { CalendarPage } from '../pages/public/CalendarPage'
import { ChangelogPage } from '../pages/public/ChangelogPage'

// 账号
import { LoginPage } from '../pages/account/LoginPage'
import { ProfilePage } from '../pages/account/ProfilePage'
import { NotificationPage } from '../pages/account/NotificationPage'
import { SecurityPage } from '../pages/account/SecurityPage'
import { OnboardingPage } from '../pages/account/OnboardingPage'
import { ExamPage } from '../pages/account/ExamPage'
import { WhitelistPage } from '../pages/account/WhitelistPage'
import { AttendancePage } from '../pages/account/AttendancePage'

// 社区
import { BoardListPage } from '../pages/community/BoardListPage'
import { PostListPage } from '../pages/community/PostListPage'
import { PostDetailPage } from '../pages/community/PostDetailPage'
import { NewPostPage } from '../pages/community/NewPostPage'

// 后台
import { AdminOverviewPage } from '../pages/admin/AdminOverviewPage'
import { AdminUsersPage } from '../pages/admin/AdminUsersPage'
import { AdminContentPage } from '../pages/admin/AdminContentPage'
import { AdminApprovalsPage } from '../pages/admin/AdminApprovalsPage'
import { AdminInvitationsPage } from '../pages/admin/AdminInvitationsPage'

// 运维
import { OpsOverviewPage } from '../pages/ops/OpsOverviewPage'
import { OpsNodesPage } from '../pages/ops/OpsNodesPage'
import { OpsAssetsPage } from '../pages/ops/OpsAssetsPage'

export function AppRouter() {
  return (
    <Routes>
      {/* 公开页 */}
      <Route path={ROUTES.HOME} element={<HomePage />} />
      {/* 登录/注册以弹窗承载，/login 作为未登录落地页（ProtectedRoute 回跳目标） */}
      <Route path={ROUTES.LOGIN} element={<LoginPage />} />
      <Route path={ROUTES.ANNOUNCEMENTS} element={<AnnouncementListPage />} />
      <Route path={ROUTES.ANNOUNCEMENT_DETAIL} element={<AnnouncementDetailPage />} />
      <Route path={ROUTES.GUIDES} element={<GuideListPage />} />
      <Route path={ROUTES.GUIDE_DETAIL} element={<GuideDetailPage />} />
      <Route path={ROUTES.RESOURCES} element={<ResourceListPage />} />
      <Route path={ROUTES.RESOURCE_DETAIL} element={<ResourceDetailPage />} />
      <Route path={ROUTES.SERVER_STATUS} element={<ServerStatusPage />} />
      <Route path={ROUTES.MEMBERS} element={<MemberListPage />} />
      <Route path={ROUTES.MATERIALS} element={<MaterialListPage />} />
      <Route path={ROUTES.ACTIVITIES} element={<ActivityListPage />} />
      <Route path={ROUTES.ACTIVITY_DETAIL} element={<ActivityDetailPage />} />
      <Route path={ROUTES.CALENDAR} element={<CalendarPage />} />
      <Route path={ROUTES.CHANGELOG} element={<ChangelogPage />} />

      {/* 社区 */}
      <Route path={ROUTES.BOARDS} element={<BoardListPage />} />
      <Route path={ROUTES.BOARD} element={<PostListPage />} />
      <Route path={ROUTES.POST} element={<PostDetailPage />} />
      <Route path={ROUTES.NEW_POST} element={<ProtectedRoute><NewPostPage /></ProtectedRoute>} />

      {/* 用户中心 */}
      <Route path={ROUTES.PROFILE} element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
      <Route path={ROUTES.NOTIFICATIONS} element={<ProtectedRoute><NotificationPage /></ProtectedRoute>} />
      <Route path={ROUTES.SECURITY} element={<ProtectedRoute><SecurityPage /></ProtectedRoute>} />
      <Route path={ROUTES.ONBOARDING} element={<ProtectedRoute><OnboardingPage /></ProtectedRoute>} />
      <Route path={ROUTES.MY_EXAMS} element={<ProtectedRoute><ExamPage /></ProtectedRoute>} />
      <Route path={ROUTES.MY_WHITELIST} element={<ProtectedRoute><WhitelistPage /></ProtectedRoute>} />
      <Route path={ROUTES.MY_ATTENDANCE} element={<ProtectedRoute><AttendancePage /></ProtectedRoute>} />

      {/* 后台 */}
      <Route path={ROUTES.ADMIN} element={<ProtectedRoute requiredRole="HELPER"><AdminOverviewPage /></ProtectedRoute>} />
      <Route path={ROUTES.ADMIN_USERS} element={<ProtectedRoute requiredRole="HELPER"><AdminUsersPage /></ProtectedRoute>} />
      <Route path={ROUTES.ADMIN_CONTENT} element={<ProtectedRoute requiredRole="HELPER"><AdminContentPage /></ProtectedRoute>} />
      <Route path={ROUTES.ADMIN_APPROVALS} element={<ProtectedRoute requiredRole="HELPER"><AdminApprovalsPage /></ProtectedRoute>} />
      <Route path={ROUTES.ADMIN_INVITATIONS} element={<ProtectedRoute requiredRole="HELPER"><AdminInvitationsPage /></ProtectedRoute>} />

      {/* 运维 */}
      <Route path={ROUTES.OPS} element={<ProtectedRoute requiredRole="HELPER"><OpsOverviewPage /></ProtectedRoute>} />
      <Route path={ROUTES.OPS_NODES} element={<ProtectedRoute requiredRole="HELPER"><OpsNodesPage /></ProtectedRoute>} />
      <Route path={ROUTES.OPS_ASSETS} element={<ProtectedRoute requiredRole="HELPER"><OpsAssetsPage /></ProtectedRoute>} />
    </Routes>
  )
}
