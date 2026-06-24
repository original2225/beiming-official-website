/**
 * 社区与活动 API — community / activity / calendar 用户侧接口
 * 供 pages/community/ 引用
 */

import { get, post, patch, del } from '../client'
import * as ep from '../endpoints'
import type { PageResult } from '../../types/api'
import type { PostSummary } from '../../types/domain'

// ── 板块 ──
export const getBoards = () => get<unknown>(ep.community.boards())
export const getBoard = (boardId: string) => get<unknown>(ep.community.board(boardId))

// ── 帖子 ──
export const getPosts = (params?: Record<string, string | number | boolean | undefined>) => get<PageResult<PostSummary>>(ep.community.posts(), { params })
export const getPost = (postId: string) => get<unknown>(ep.community.post(postId))
export const getComments = (postId: string, params?: Record<string, string | number | boolean | undefined>) => get<unknown>(ep.community.comments(postId), { params })
export const searchCommunity = (q: string) => get<unknown>(ep.community.search(), { params: { q } })

// ── 我的帖子 ──
export const getMyPosts = () => get<unknown>(ep.community.mePosts())
export const createPost = (body: unknown) => post<unknown>(ep.community.mePosts(), body)
export const updateMyPost = (postId: string, body: unknown) => patch<unknown>(ep.community.mePost(postId), body)
export const submitPost = (postId: string) => post<unknown>(ep.community.meSubmitPost(postId))
export const withdrawPost = (postId: string) => patch<unknown>(ep.community.meWithdrawPost(postId))

// ── 评论 ──
export const createComment = (postId: string, body: unknown) => post<unknown>(ep.community.meComment(postId), body)

// ── 互动 ──
export const likePost = (postId: string) => post<unknown>(ep.community.meLikePost(postId))
export const unlikePost = (postId: string) => del<unknown>(ep.community.meLikePost(postId))
export const favoritePost = (postId: string) => post<unknown>(ep.community.meFavoritePost(postId))
export const unfavoritePost = (postId: string) => del<unknown>(ep.community.meFavoritePost(postId))

// ── 举报 ──
export const reportPost = (postId: string, body: unknown) => post<unknown>(ep.community.meReportPost(postId), body)
export const getMyReports = () => get<unknown>(ep.community.meReports())

// ── 工单 ──
export const getMyTickets = () => get<unknown>(ep.community.meTickets())
export const createTicket = (body: unknown) => post<unknown>(ep.community.meTickets(), body)
export const getMyTicket = (ticketId: string) => get<unknown>(ep.community.meTicket(ticketId))

// ── 活动（用户侧） ──
export const getMyRegistrations = () => get<unknown>(ep.activity.meRegistrations())
export const registerActivity = (activityId: string) => post<unknown>(ep.activity.register(activityId))
export const cancelRegistration = (registrationId: string) => post<unknown>(ep.activity.cancelRegistration(registrationId))
export const getMyCheckIn = (activityId: string) => get<unknown>(ep.activity.meCheckIn(activityId))
export const getMyRewards = () => get<unknown>(ep.activity.meRewards())

// ── 日历（用户侧） ──
export const getMyWatchlist = () => get<unknown>(ep.calendar.meWatchlist())
export const watchEvent = (eventId: string) => post<unknown>(ep.calendar.watch(eventId))
export const unwatchEvent = (eventId: string) => post<unknown>(ep.calendar.unwatch(eventId))

// ── 更新日志（用户侧） ──
export const getMyBookmarks = () => get<unknown>(ep.changelog.meBookmarks())
export const bookmarkRelease = (releaseId: string) => post<unknown>(ep.changelog.bookmark(releaseId))
export const unbookmarkRelease = (releaseId: string) => post<unknown>(ep.changelog.unbookmark(releaseId))
