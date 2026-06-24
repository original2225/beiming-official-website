/**
 * 官网公开页 API — content / server-status / resource / guide / material / activity / calendar / changelog / online-map 公开接口
 * 供 pages/public/ 引用
 */

import { get, post } from '../client'
import * as ep from '../endpoints'
import type { PageResult } from '../../types/api'
import type { ContentSummary, ServerStatusSummary, ResourceSummary, GuideSummary, MaterialSummary, ActivitySummary, CalendarEventSummary, ChangelogSummary, LineSummary, MemberSummary } from '../../types/domain'
import type { ActivityDetailView, ContentDetailView, GuideDetailView, ResourceDetailView } from '../../types/view-models'

type BackendCategory = string | { name?: unknown; title?: unknown } | null | undefined

type BackendLine = Partial<LineSummary> & {
  entryAddress?: unknown
  status?: unknown
  latencyMs?: unknown
}

type BackendServerOverview = Partial<ServerStatusSummary> & {
  overallStatus?: unknown
  onlinePlayers?: unknown
  lines?: BackendLine[]
}

type BackendVersion = {
  versionName?: unknown
  version?: unknown
  downloadCount?: unknown
  downloads?: unknown
  downloadUrl?: unknown
}

type BackendResource = Omit<Partial<ResourceSummary>, 'category'> & {
  category?: BackendCategory
  latestVersion?: BackendVersion | null
  downloadUrl?: unknown
}

type BackendGuide = Omit<Partial<GuideSummary>, 'category'> & {
  category?: BackendCategory
}

function categoryName(category: BackendCategory): string {
  if (typeof category === 'string') return category
  if (category && typeof category.name === 'string') return category.name
  if (category && typeof category.title === 'string') return category.title
  return ''
}

function numberOrZero(value: unknown): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : 0
}

function isOnlineStatus(value: unknown): boolean {
  return value === true || value === 'ONLINE' || value === 'AVAILABLE'
}

function mapLine(line: BackendLine): LineSummary {
  return {
    lineId: String(line.lineId ?? ''),
    name: String(line.name ?? ''),
    address: String(line.address ?? line.entryAddress ?? ''),
    latency: numberOrZero(line.latency ?? line.latencyMs),
    online: isOnlineStatus(line.online ?? line.status),
  }
}

function mapResource<T extends BackendResource>(resource: T): T & ResourceSummary {
  const latestVersion = resource.latestVersion
  return {
    ...resource,
    resourceId: String(resource.resourceId ?? ''),
    title: String(resource.title ?? ''),
    slug: String(resource.slug ?? ''),
    category: categoryName(resource.category),
    version: String(resource.version ?? latestVersion?.versionName ?? latestVersion?.version ?? ''),
    downloads: numberOrZero(resource.downloads ?? latestVersion?.downloadCount),
  }
}

function mapResourceDetail(resource: BackendResource): ResourceDetailView {
  const latestVersion = resource.latestVersion
  return {
    ...resource,
    title: String(resource.title ?? ''),
    category: categoryName(resource.category),
    version: String(resource.version ?? latestVersion?.versionName ?? latestVersion?.version ?? ''),
    downloadUrl: typeof resource.downloadUrl === 'string'
      ? resource.downloadUrl
      : typeof latestVersion?.downloadUrl === 'string'
        ? latestVersion.downloadUrl
        : undefined,
  }
}

function mapGuide<T extends BackendGuide>(guide: T): T & GuideSummary {
  return {
    ...guide,
    guideId: String(guide.guideId ?? ''),
    title: String(guide.title ?? ''),
    slug: String(guide.slug ?? ''),
    category: categoryName(guide.category),
    updatedAt: String(guide.updatedAt ?? ''),
  }
}

function mapPage<T, U>(page: PageResult<T>, mapper: (item: T) => U): PageResult<U> {
  return {
    ...page,
    items: page.items.map(mapper),
  }
}

// ── 内容 ──
export const getHome = () => get<unknown>(ep.content.home())
export const getContentItems = (params?: Record<string, string | number | boolean | undefined>) => get<PageResult<ContentSummary>>(ep.content.items(), { params })
export const getContentDetail = (contentId: string) => get<ContentDetailView>(ep.content.item(contentId))
export const getContentBySlug = (slug: string) => get<unknown>(ep.content.itemBySlug(slug))
export const getCategories = () => get<unknown>(ep.content.categories())

// ── 服务器状态 ──
export const getServerOverview = async () => {
  const overview = await get<BackendServerOverview>(ep.serverStatus.overview())
  return {
    online: isOnlineStatus(overview.online ?? overview.overallStatus),
    playerCount: numberOrZero(overview.playerCount ?? overview.onlinePlayers),
    maxPlayers: numberOrZero(overview.maxPlayers),
    version: String(overview.version ?? ''),
    motd: String(overview.motd ?? ''),
    lines: (overview.lines ?? []).map(mapLine),
  } satisfies ServerStatusSummary
}
export const getServerInstances = () => get<unknown>(ep.serverStatus.instances())
export const getServerLines = async () => (await get<PageResult<BackendLine>>(ep.serverStatus.lines())).items.map(mapLine)
export const getOutages = () => get<unknown>(ep.serverStatus.outages())

// ── 资源 ──
export const getResources = async (params?: Record<string, string | number | boolean | undefined>) => mapPage(await get<PageResult<BackendResource>>(ep.resource.list(), { params }), mapResource)
export const getResourceDetail = async (resourceId: string) => mapResourceDetail(await get<BackendResource>(ep.resource.detail(resourceId)))
export const getResourceBySlug = (slug: string) => get<unknown>(ep.resource.bySlug(slug))
export const getResourceCategories = () => get<unknown>(ep.resource.categories())

// ── 指南 ──
export const getGuideHome = () => get<unknown>(ep.guide.home())
export const getGuides = async (params?: Record<string, string | number | boolean | undefined>) => mapPage(await get<PageResult<BackendGuide>>(ep.guide.articles(), { params }), mapGuide)
export const getGuideDetail = async (guideId: string) => mapGuide(await get<BackendGuide>(ep.guide.article(guideId))) as GuideDetailView
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
