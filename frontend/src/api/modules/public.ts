/**
 * 官网公开页 API — content / server-status / resource / guide / material / activity / calendar / changelog / online-map 公开接口
 * 供 pages/public/ 引用
 */

import { get, post } from '../client'
import * as ep from '../endpoints'
import type { PageResult } from '../../types/api'
import type { ContentSummary, ServerStatusSummary, ResourceSummary, GuideSummary, MaterialSummary, ActivitySummary, CalendarEventSummary, ChangelogSummary, LineSummary, MemberSummary } from '../../types/domain'
import type { ActivityDetailView, ContentDetailView, GuideDetailView, ResourceDetailView } from '../../types/view-models'

// ── 内容 ──
export const getHome = () => get<unknown>(ep.content.home())
export const getContentItems = (params?: Record<string, string | number | boolean | undefined>) => get<PageResult<ContentSummary>>(ep.content.items(), { params })
export const getContentDetail = (contentId: string) => get<ContentDetailView>(ep.content.item(contentId))
export const getContentBySlug = (slug: string) => get<unknown>(ep.content.itemBySlug(slug))
export const getCategories = () => get<unknown>(ep.content.categories())

// ── 服务器状态 ──
export const getServerOverview = () => get<ServerStatusSummary>(ep.serverStatus.overview())
export const getServerInstances = () => get<unknown>(ep.serverStatus.instances())
export const getServerLines = () => get<LineSummary[]>(ep.serverStatus.lines())
export const getOutages = () => get<unknown>(ep.serverStatus.outages())

// ── 资源 ──
export const getResources = (params?: Record<string, string | number | boolean | undefined>) => get<PageResult<ResourceSummary>>(ep.resource.list(), { params })
export const getResourceDetail = (resourceId: string) => get<ResourceDetailView>(ep.resource.detail(resourceId))
export const getResourceBySlug = (slug: string) => get<unknown>(ep.resource.bySlug(slug))
export const getResourceCategories = () => get<unknown>(ep.resource.categories())

// ── 指南 ──
export const getGuideHome = () => get<unknown>(ep.guide.home())
export const getGuides = (params?: Record<string, string | number | boolean | undefined>) => get<PageResult<GuideSummary>>(ep.guide.articles(), { params })
export const getGuideDetail = (guideId: string) => get<GuideDetailView>(ep.guide.article(guideId))
export const getGuideBySlug = (slug: string) => get<unknown>(ep.guide.articleBySlug(slug))
export const getGuideCategories = () => get<unknown>(ep.guide.categories())
export const getGuideSearch = (q: string) => get<unknown>(ep.guide.search(), { params: { q } })
export const getCommands = () => get<unknown>(ep.guide.commands())
export const getExternalChannels = () => get<unknown>(ep.guide.externalChannels())
export const getRulesCurrent = () => get<unknown>(ep.guide.rulesCurrent())

// ── 素材 ──
export const getFeaturedMaterials = () => get<MaterialSummary[]>(ep.material.featured())
export const getMaterials = (params?: Record<string, string | number | boolean | undefined>) => get<PageResult<MaterialSummary>>(ep.material.list(), { params })
export const getMaterialDetail = (materialId: string) => get<unknown>(ep.material.detail(materialId))

// ── 活动 ──
export const getActivities = (params?: Record<string, string | number | boolean | undefined>) => get<PageResult<ActivitySummary>>(ep.activity.events(), { params })
export const getActivityDetail = (activityIdOrSlug: string) => get<ActivityDetailView>(ep.activity.event(activityIdOrSlug))
export const getActivityResult = (activityId: string) => get<unknown>(ep.activity.result(activityId))
export const getActivityCalendarSummary = () => get<unknown>(ep.activity.calendarSummary())

// ── 日历 ──
export const getCalendarEvents = (params?: Record<string, string | number | boolean | undefined>) => get<PageResult<CalendarEventSummary>>(ep.calendar.events(), { params })
export const getCalendarMonth = (year: number, month: number) => get<unknown>(ep.calendar.month(), { params: { year, month } })
export const getCalendarUpcoming = () => get<unknown>(ep.calendar.upcoming())

// ── 更新日志 ──
export const getChangelogReleases = (params?: Record<string, string | number | boolean | undefined>) => get<PageResult<ChangelogSummary>>(ep.changelog.releases(), { params })
export const getChangelogRelease = (releaseIdOrSlug: string) => get<unknown>(ep.changelog.release(releaseIdOrSlug))
export const getChangelogLatest = () => get<unknown>(ep.changelog.latest())

// ── 在线地图 ──
export const getOnlineMapOverview = () => get<unknown>(ep.onlineMap.overview())
export const getOnlineMapEmbed = () => get<unknown>(ep.onlineMap.embed())

// ── 成员 ──
export const getMembers = (params?: Record<string, string | number | boolean | undefined>) => get<PageResult<MemberSummary>>(ep.profile.members(), { params })
export const getMemberDetail = (memberId: string) => get<unknown>(ep.profile.member(memberId))

// ── 指南反馈 ──
export const submitGuideFeedback = (guideId: string, body: unknown) => post<unknown>(ep.guide.feedback(guideId), body)
