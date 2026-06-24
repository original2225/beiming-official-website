/**
 * 路由配置 — 公开页 / 用户中心 / 后台 / 运维四个分区
 */

import { Routes, Route } from 'react-router-dom'
import { ROUTES } from '../constants/routes'
import { ProtectedRoute } from '../components/layout/ProtectedRoute'
import { PageLayout } from '../components/layout/PageLayout'

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

// 占位（运维 — 后续阶段）
function Placeholder({ title, variant = 'public' }: { title: string; variant?: 'public' | 'account' | 'admin' | 'ops' }) {
  const inner = (
    <PageLayout variant={variant}>
      <h1 className="font-minecraft text-2xl text-mc-grass">{title}</h1>
      <p className="text-text-secondary mt-2">页面建设中</p>
    </PageLayout>
  )
  if (variant === 'account') return <ProtectedRoute>{inner}</ProtectedRoute>
  if (variant === 'admin' || variant === 'ops') return <ProtectedRoute requiredRole="HELPER">{inner}</ProtectedRoute>
  return inner
}

export function AppRouter() {
  return (
    <Routes>
      {/* 公开页 */}
      <Route path={ROUTES.HOME} element={<HomePage />} />
      {/* 登录/注册改为弹窗，不再有独立页面 */}
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

      {/* 运维 — 占位，后续 #11 */}
      <Route path={ROUTES.OPS} element={<Placeholder title="运维总览" variant="ops" />} />
      <Route path={ROUTES.OPS_NODES} element={<Placeholder title="节点管理" variant="ops" />} />
      <Route path={ROUTES.OPS_ASSETS} element={<Placeholder title="资产管理" variant="ops" />} />
    </Routes>
  )
}
