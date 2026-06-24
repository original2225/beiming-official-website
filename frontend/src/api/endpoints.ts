/**
 * API 路径集中管理 — 所有接口路径生成函数
 * 供 api/modules/*.ts 引用，避免页面里直接拼路径
 */

const V1 = '/api/v1'

// ── auth ──
export const auth = {
  register: () => `${V1}/auth/register`,
  login: () => `${V1}/auth/login`,
  logout: () => `${V1}/auth/logout`,
  me: () => `${V1}/auth/me`,
  sessionVerify: () => `${V1}/auth/session/verify`,
  meSessions: () => `${V1}/auth/me/sessions`,
  meSession: (sessionId: string) => `${V1}/auth/me/sessions/${sessionId}`,
  mePassword: () => `${V1}/auth/me/password`,
  passwordResetRequest: () => `${V1}/auth/password-reset/request`,
  passwordResetConfirm: () => `${V1}/auth/password-reset/confirm`,
  minecraftBinding: () => `${V1}/auth/me/minecraft-binding`,
  adminUsers: () => `${V1}/auth/admin/users`,
  adminUser: (userId: string) => `${V1}/auth/admin/users/${userId}`,
  adminUserRoles: (userId: string) => `${V1}/auth/admin/users/${userId}/roles`,
  adminInvitations: () => `${V1}/auth/admin/invitations`,
  adminInvitationDisable: (invitationId: string) => `${V1}/auth/admin/invitations/${invitationId}/disable`,
  adminInvitationUsageRecords: (invitationId: string) => `${V1}/auth/admin/invitations/${invitationId}/usage-records`,
} as const

// ── content ──
export const content = {
  home: () => `${V1}/content/home`,
  items: () => `${V1}/content/items`,
  item: (contentId: string) => `${V1}/content/items/${contentId}`,
  itemBySlug: (slug: string) => `${V1}/content/items/by-slug/${slug}`,
  categories: () => `${V1}/content/categories`,
  tags: () => `${V1}/content/tags`,
  topics: () => `${V1}/content/topics`,
  topic: (topicId: string) => `${V1}/content/topics/${topicId}`,
  topicBySlug: (slug: string) => `${V1}/content/topics/by-slug/${slug}`,
  seo: () => `${V1}/content/seo`,
  sitemap: () => `${V1}/content/seo/sitemap`,
} as const

// ── server-status ──
export const serverStatus = {
  overview: () => `${V1}/server-status/overview`,
  instances: () => `${V1}/server-status/instances`,
  instance: (instanceId: string) => `${V1}/server-status/instances/${instanceId}`,
  lines: () => `${V1}/server-status/lines`,
  history: () => `${V1}/server-status/history/snapshots`,
  outages: () => `${V1}/server-status/outages`,
} as const

// ── resource ──
export const resource = {
  list: () => `${V1}/resources`,
  detail: (resourceId: string) => `${V1}/resources/${resourceId}`,
  bySlug: (slug: string) => `${V1}/resources/by-slug/${slug}`,
  categories: () => `${V1}/resources/categories`,
  versions: (resourceId: string) => `${V1}/resources/${resourceId}/versions`,
  download: (resourceId: string, versionId: string) => `${V1}/resources/${resourceId}/versions/${versionId}/download`,
} as const

// ── guide ──
export const guide = {
  home: () => `${V1}/guides/home`,
  categories: () => `${V1}/guides/categories`,
  articles: () => `${V1}/guides/articles`,
  article: (guideId: string) => `${V1}/guides/articles/${guideId}`,
  articleBySlug: (slug: string) => `${V1}/guides/articles/by-slug/${slug}`,
  search: () => `${V1}/guides/search`,
  commands: () => `${V1}/guides/commands`,
  externalChannels: () => `${V1}/guides/external-channels`,
  rulesCurrent: () => `${V1}/guides/rules/current`,
  rulesVersion: (ruleVersion: string) => `${V1}/guides/rules/versions/${ruleVersion}`,
  feedback: (guideId: string) => `${V1}/guides/articles/${guideId}/feedback`,
} as const

// ── material ──
export const material = {
  featured: () => `${V1}/materials/featured`,
  list: () => `${V1}/materials`,
  detail: (materialId: string) => `${V1}/materials/${materialId}`,
  bySlug: (slug: string) => `${V1}/materials/by-slug/${slug}`,
  categories: () => `${V1}/materials/categories`,
  assets: (materialId: string) => `${V1}/materials/${materialId}/assets`,
} as const

// ── activity ──
export const activity = {
  events: () => `${V1}/activity/events`,
  event: (activityIdOrSlug: string) => `${V1}/activity/events/${activityIdOrSlug}`,
  result: (activityId: string) => `${V1}/activity/events/${activityId}/result`,
  calendarSummary: () => `${V1}/activity/calendar-summary`,
  meRegistrations: () => `${V1}/activity/me/registrations`,
  meRegistration: (registrationId: string) => `${V1}/activity/me/registrations/${registrationId}`,
  register: (activityId: string) => `${V1}/activity/me/events/${activityId}/registrations`,
  cancelRegistration: (registrationId: string) => `${V1}/activity/me/registrations/${registrationId}/cancel`,
  meCheckIn: (activityId: string) => `${V1}/activity/me/events/${activityId}/check-in`,
  meRewards: () => `${V1}/activity/me/rewards`,
} as const

// ── calendar ──
export const calendar = {
  events: () => `${V1}/calendar/events`,
  event: (eventId: string) => `${V1}/calendar/events/${eventId}`,
  month: () => `${V1}/calendar/month`,
  upcoming: () => `${V1}/calendar/upcoming`,
  meWatchlist: () => `${V1}/calendar/me/watchlist`,
  watch: (eventId: string) => `${V1}/calendar/me/events/${eventId}/watch`,
  unwatch: (eventId: string) => `${V1}/calendar/me/events/${eventId}/unwatch`,
} as const

// ── changelog ──
export const changelog = {
  releases: () => `${V1}/changelog/releases`,
  release: (releaseIdOrSlug: string) => `${V1}/changelog/releases/${releaseIdOrSlug}`,
  latest: () => `${V1}/changelog/versions/latest`,
  tags: () => `${V1}/changelog/tags`,
  changes: () => `${V1}/changelog/changes`,
  meBookmarks: () => `${V1}/changelog/me/bookmarks`,
  bookmark: (releaseId: string) => `${V1}/changelog/me/releases/${releaseId}/bookmark`,
  unbookmark: (releaseId: string) => `${V1}/changelog/me/releases/${releaseId}/unbookmark`,
} as const

// ── online-map ──
export const onlineMap = {
  health: () => `${V1}/online-map/health`,
  overview: () => `${V1}/online-map/overview`,
  providers: () => `${V1}/online-map/providers`,
  provider: (providerId: string) => `${V1}/online-map/providers/${providerId}`,
  worlds: () => `${V1}/online-map/worlds`,
  layers: () => `${V1}/online-map/layers`,
  markers: () => `${V1}/online-map/markers`,
  regions: () => `${V1}/online-map/regions`,
  embed: () => `${V1}/online-map/embed`,
} as const

// ── profile ──
export const profile = {
  members: () => `${V1}/profile/members`,
  member: (memberId: string) => `${V1}/profile/members/${memberId}`,
  me: () => `${V1}/profile/me`,
} as const

// ── notification ──
export const notification = {
  meList: () => `${V1}/notifications/me`,
  meUnreadCount: () => `${V1}/notifications/me/unread-count`,
  meDetail: (notificationId: string) => `${V1}/notifications/me/${notificationId}`,
  meRead: (notificationId: string) => `${V1}/notifications/me/${notificationId}/read`,
  meReadAll: () => `${V1}/notifications/me/read-all`,
  meArchive: (notificationId: string) => `${V1}/notifications/me/${notificationId}/archive`,
} as const

// ── onboarding ──
export const onboarding = {
  meProgress: () => `${V1}/onboarding/me/progress`,
  meStart: () => `${V1}/onboarding/me/start`,
  meProfileConfirmation: () => `${V1}/onboarding/me/profile-confirmation`,
  meRulesConfirmation: () => `${V1}/onboarding/me/rules-confirmation`,
  meDirection: () => `${V1}/onboarding/me/direction`,
  meAdvance: () => `${V1}/onboarding/me/advance`,
  meNextAction: () => `${V1}/onboarding/me/next-action`,
} as const

// ── exam ──
export const exam = {
  meSessions: () => `${V1}/exams/me/sessions`,
  meCurrentSession: () => `${V1}/exams/me/sessions/current`,
  meSessionPaper: (sessionId: string) => `${V1}/exams/me/sessions/${sessionId}/paper`,
  meSessionAnswers: (sessionId: string) => `${V1}/exams/me/sessions/${sessionId}/answers`,
  meSessionSubmit: (sessionId: string) => `${V1}/exams/me/sessions/${sessionId}/submit`,
  meSessionSupplement: (sessionId: string) => `${V1}/exams/me/sessions/${sessionId}/supplement`,
  meSessionResult: (sessionId: string) => `${V1}/exams/me/sessions/${sessionId}/result`,
} as const

// ── whitelist ──
export const whitelist = {
  meApplications: () => `${V1}/whitelist/me/applications`,
  meCurrentApplication: () => `${V1}/whitelist/me/applications/current`,
  meApplication: (applicationId: string) => `${V1}/whitelist/me/applications/${applicationId}`,
  meSubmit: (applicationId: string) => `${V1}/whitelist/me/applications/${applicationId}/submit`,
  meSupplement: (applicationId: string) => `${V1}/whitelist/me/applications/${applicationId}/supplement`,
  meWithdraw: (applicationId: string) => `${V1}/whitelist/me/applications/${applicationId}/withdraw`,
  meResult: (applicationId: string) => `${V1}/whitelist/me/applications/${applicationId}/result`,
} as const

// ── attendance ──
export const attendance = {
  leaderboard: () => `${V1}/attendance/leaderboard`,
  meAccount: () => `${V1}/attendance/me/account`,
  meLedger: () => `${V1}/attendance/me/ledger`,
  meContributions: () => `${V1}/attendance/me/contributions`,
  meRanking: () => `${V1}/attendance/me/ranking`,
} as const

// ── community ──
export const community = {
  boards: () => `${V1}/community/boards`,
  board: (boardId: string) => `${V1}/community/boards/${boardId}`,
  posts: () => `${V1}/community/posts`,
  post: (postId: string) => `${V1}/community/posts/${postId}`,
  comments: (postId: string) => `${V1}/community/posts/${postId}/comments`,
  search: () => `${V1}/community/search`,
  mePosts: () => `${V1}/community/me/posts`,
  mePost: (postId: string) => `${V1}/community/me/posts/${postId}`,
  meSubmitPost: (postId: string) => `${V1}/community/me/posts/${postId}/submit`,
  meWithdrawPost: (postId: string) => `${V1}/community/me/posts/${postId}/withdraw`,
  meComment: (postId: string) => `${V1}/community/me/posts/${postId}/comments`,
  meLikePost: (postId: string) => `${V1}/community/me/posts/${postId}/like`,
  meFavoritePost: (postId: string) => `${V1}/community/me/posts/${postId}/favorite`,
  meReportPost: (postId: string) => `${V1}/community/me/posts/${postId}/reports`,
  meReports: () => `${V1}/community/me/reports`,
  meTickets: () => `${V1}/community/me/tickets`,
  meTicket: (ticketId: string) => `${V1}/community/me/tickets/${ticketId}`,
} as const
