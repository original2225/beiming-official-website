import type { ReactNode } from 'react'
import type { PageResult } from './api'
import type { Status } from './common'

export interface ExamSessionView {
  sessionId: string
  title?: string
  direction?: string
  status?: Status
  createdAt: string
}

export interface AttendanceAccountView {
  totalPoints?: number
  points?: number
  monthlyPoints?: number
  activityLevel?: string
}

export interface RankingView {
  rank?: number
  position?: number
}

export interface OnboardingProgressView {
  currentStep?: string
  status?: string
}

export interface NextActionView {
  description?: string
  action?: string
}

export interface ProfileView {
  groupName?: string
  milestones?: unknown[]
}

export interface WhitelistApplicationView {
  applicationId: string
  status?: Status
  createdAt: string
}

export interface AdminTodoView {
  todoId: string
  title: string
  type?: string
  status?: Status
  createdAt: string
}

export interface AdminContentView {
  contentId: string
  title: string
  status?: Status
  category?: string
  updatedAt?: string
  createdAt: string
}

export interface AdminUserView {
  userId: string
  username: string
  status?: Status
  createdAt?: string
}

export type MetricsView = Record<string, ReactNode>

export interface AdminOverviewView {
  pendingApprovals?: number
  activeUsers?: number
  version?: string
}

export interface BoardView {
  boardId: string
  name: string
  description?: string
  postCount?: number
}

export interface BoardDetailView {
  name?: string
}

export interface PostDetailView {
  title: string
  authorName: string
  createdAt: string
  body?: string
  content?: string
}

export interface CommentView {
  commentId: string
  authorName: string
  createdAt: string
  body: string
}

export interface ActivityDetailView {
  title: string
  status: Status
  startTime: string
  endTime: string
  registrationOpen: boolean
  description?: string
}

export interface ContentDetailView {
  title: string
  publishedAt?: string
  body?: string
  content?: string
}

export interface GuideDetailView {
  title: string
  updatedAt: string
  body?: string
  content?: string
}

export interface ResourceDetailView {
  title: string
  category: string
  version: string
  description?: string
  downloadUrl?: string
}

export type ExamSessionPage = PageResult<ExamSessionView>
export type WhitelistApplicationPage = PageResult<WhitelistApplicationView>
export type AdminTodoPage = PageResult<AdminTodoView>
export type AdminContentPage = PageResult<AdminContentView>
export type AdminUserPage = PageResult<AdminUserView>
