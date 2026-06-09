# 北冥官网 calendar API 契约

版本：0.1

## 文档定位

本文档是 `calendar` 微服务的正式 API 契约。后续 `changelog`、前端适配、`admin` 聚合、`ops-control` 和 `external-node-executor` 只能通过本文档定义的接口读取或管理日程事件、关注、提醒摘要、来源同步、审计和自检摘要，不能直接读取或修改 `calendar` 数据库，也不能把活动报名、社区投票、更新日志主数据或真实服务器运维能力塞进 `calendar`。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `calendar` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了成熟日历和社区日程系统的公开设计。Google Calendar API 的事件资源说明事件时间、时区、状态、可见性、提醒和扩展属性应结构化保存。Microsoft Graph Calendar API 的 event 与 calendarView 说明事件详情和时间范围视图应分开。Discord Guild Scheduled Event 的状态、实体类型和隐私等级说明社区日程要有明确状态和可见性。CalDAV RFC 4791 的 calendar-query 和 time-range 说明时间窗口查询是日历服务基础能力。FullCalendar 的 Event Object 和 JSON feed 思路说明前端日历需要稳定的 `start`、`end`、`allDay` 和来源字段。Nextcloud Calendar 作为开源日历项目说明 CalDAV、提醒和共享是完整日历系统常见方向，但本轮只实现北冥官网 P1 所需的 HTTP 契约、关注和提醒摘要，不实现 CalDAV、ICS 导入导出、递归规则、会议邀请和外部账号同步。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Google Calendar API Events](https://developers.google.com/calendar/api/v3/reference/events) | 事件时间、时区、状态、可见性、提醒和来源字段需要分开维护。 |
| [Microsoft Graph event resource](https://learn.microsoft.com/en-us/graph/api/resources/event) | 事件详情和日历视图查询应分离，时间窗口读取不能依赖前端过滤。 |
| [Discord Guild Scheduled Event](https://discord.com/developers/docs/resources/guild-scheduled-event) | 社区日程需要状态、开始结束时间、隐私等级和实体类型。 |
| [CalDAV RFC 4791](https://www.rfc-editor.org/rfc/rfc4791) | 时间范围查询和集合访问是日历服务基础能力。 |
| [FullCalendar Event Object](https://fullcalendar.io/docs/event-object) | 前端日历视图需要稳定的 start、end、allDay、id 和 extendedProps。 |
| [Nextcloud Calendar](https://github.com/nextcloud/calendar) | 开源日历项目通常把事件、提醒、共享和 CalDAV 扩展分层，本轮只借鉴边界。 |

## 职责边界

`calendar` 负责日程事件、维护窗口、工程节点、投票截止、版本更新时间、服务器日程、时间线视图、当前用户关注、提醒摘要、来源同步快照、依赖摘要、幂等记录、calendar 审计和自检摘要。

`calendar` 不负责活动报名、活动结果、活动奖励、站内通知主数据、官网内容主发布、资源下载主数据、社区投票主数据、考勤积分、真实服务器运维、容器、终端、文件管理、备份、节点守护、Cloudreve 管理或更新日志主数据。

`activity` 已经稳定，`calendar` 只能通过 `GET /api/v1/activity/calendar-summary` 读取活动时间摘要，并保存本服务自己的导入快照、来源引用和同步状态。`calendar` 不能读取 `activity` 内部类、测试种子、内存存储或未来数据库，不能修改活动状态、报名和结果。

`notification` 是提醒投递的未来正式来源。P1 中 `calendar` 只保存提醒摘要和投递意图，不创建 notification 主数据、不维护未读数、不写通知模板。后续需要真实投递时，必须通过 `notification` 正式接口适配。

`changelog` 已经由 `engagement-core-service` 承载。本轮 `calendar` 可以保存 `VERSION_RELEASE` 手工事件和 changelog 来源占位，自检摘要中必须暴露 `CHANGELOG_NOT_CONNECTED`，直到 calendar 与 changelog 的正式写入适配单独完成闭环；calendar 不能把版本更新日志正文、插件变更、规则调整和地图更新主数据塞进自己。

维护窗口在本模块只是日程元数据。任何真实服务器启动、停止、重启、命令执行、日志流、文件管理、备份恢复和节点操作都属于后续 `ops-control` 与 `external-node-executor`。

## 数据归属

`calendar` 拥有以下主数据：日程事件、事件版本摘要、事件来源引用、事件来源同步快照、用户关注记录、提醒策略摘要、提醒投递摘要、依赖调用摘要、幂等记录、calendar 审计日志和自检统计。

`calendar` 可以保存来自 `auth` 的 `userId`、`displayName`、`roles`、`permissions` 和用户状态快照；可以保存来自 `activity` 的 `activityId`、`slug`、`title`、`type`、`visibility`、`status`、`startAt`、`endAt`、`registrationCloseAt` 和 `summary` 快照；可以保存 future `changelog` 的来源 ID 和占位同步状态。快照只服务展示、检索、提醒和审计，不能成为来源模块主数据，也不能反写来源模块。

## 基础路径与认证

所有接口默认使用 `/api/v1/calendar` 前缀。当前运行入口为 `engagement-core-service:8132`。历史原服务端口 `8114` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或回归测试命令。

公开接口允许游客读取 `PUBLISHED` 且符合可见范围的事件。公开接口不得返回内部备注、审核参数、管理员 ID、提醒失败详情、完整依赖错误、审计参数、幂等键或来源模块内部路径。

当前用户接口使用 `/api/v1/calendar/me` 前缀，全部要求 `Authorization: Bearer <token>`。当前用户只能读取和维护自己的关注记录、提醒偏好和关注列表。浏览器请求体不得传入 `userId`、`roles`、`permissions`、`watchCount`、`status`、`notificationStatus`、`operatorUserId`、`reviewerUserId`、`auditResult`、`sourceVersion` 等服务端可信字段。

后台接口使用 `/api/v1/calendar/admin` 前缀，全部要求登录。后台读取事件、同步摘要、自检摘要和审计列表要求 `HELPER`、`ADMIN` 或 `OWNER`，但审计列表只允许 `ADMIN` 或 `OWNER`。事件创建、修改、提交、审核、发布、下架、归档、软删除和 activity 同步要求 `HELPER`、`ADMIN` 或 `OWNER`，但 `HELPER` 只能创建草稿、修改自己创建的未发布事件、提交审核和执行被授权的初审。发布、下架、归档、软删除、来源同步和审计读取要求 `ADMIN` 或 `OWNER`。

P1 契约补强要求：当前用户关注列表、后台事件列表和审计列表必须完整实现本文档列出的过滤参数，不能只返回未过滤全量数据。后台手工创建事件时，P1 只接受 `MANUAL` 和未来 `CHANGELOG` 占位来源；`ACTIVITY` 只能通过 `/api/v1/calendar/admin/sync/activity` 导入，`COMMUNITY_POLL` 和 `OPS_PLACEHOLDER` 只作为未来来源枚举保留，直接创建必须返回字段校验错误。`HELPER` 修改事件时必须限制为自己创建且未发布的事件，不能修改其他后台人员创建的事件。

## 本地测试控制头

`calendar` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Activity-Mode`、`X-Test-Notification-Mode`、`X-Test-Changelog-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store`、`X-Test-Fail-Watch` 和 `X-Test-Now` 模拟依赖失败、通知失败、写入失败、关注并发冲突和时间边界。该能力只服务本地测试闭环，不属于正式业务 API。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、状态失败、关注失败、通知失败、时间模拟或快照 stale。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 视为已满足的生产化硬化项。

## 前序服务兼容契约

`auth` 是所有登录接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions`、`status` 和可选 `minecraftBinding`。用户状态为 `ACTIVE` 时可关注和后台写入；`PENDING_PROFILE` 可以关注公开事件但不能创建后台事件；`DISABLED`、`BANNED`、`DELETED` 不允许写入。auth 不可用返回 `49800`，auth 超时返回 `49801`，字段或枚举不兼容返回 `49802`。

`activity` 是活动日程只读来源。activity 同步只能读取 `GET /api/v1/activity/calendar-summary`。activity 不可用返回 `49810`，超时返回 `49811`，字段或枚举不兼容返回 `49812`。后台同步失败不得删除已有 calendar 事件，只能记录同步失败摘要。公开读取可继续展示已导入快照，并标记 `sourceSnapshotStale=true`。

`notification` 是提醒辅助依赖。关注事件、事件发布时间变更、维护窗口发布和事件下架可以形成提醒摘要。P1 不真实投递通知，只记录 `notificationStatus=SKIPPED` 或在测试控制下记录 `FAILED` 脱敏摘要。notification 不可用记录或返回 `49820`，超时记录或返回 `49821`，字段不兼容记录或返回 `49822`。通知失败不得回滚事件主状态或关注状态。

`changelog` 是未来版本更新来源。本轮未连接时必须在自检摘要返回 `CHANGELOG_NOT_CONNECTED`。如果测试控制头模拟 changelog 不可用，手工 `VERSION_RELEASE` 事件仍可创建，来源同步只返回降级摘要，不得创建 changelog 主数据。changelog 不可用记录 `49830`，超时记录 `49831`，字段不兼容记录 `49832`。

`admin` 当前只是聚合入口。本文档不修改 `admin` 契约。若后续需要 admin 展示 calendar 待办、指标或审计索引，必须作为 admin 兼容变更单独走契约、测试、红灯、实现和回归闭环。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `CalendarEventType` | `ACTIVITY`、`MAINTENANCE`、`ENGINEERING_MILESTONE`、`VOTE_DEADLINE`、`VERSION_RELEASE`、`SERVER_SCHEDULE` | 日程事件类型。 |
| `CalendarEventStatus` | `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`PUBLISHED`、`OFFLINE`、`ARCHIVED`、`DELETED` | 事件主状态。公开查询只返回 `PUBLISHED`。 |
| `CalendarVisibility` | `PUBLIC`、`MEMBER_ONLY`、`STAFF_ONLY` | 事件可见范围。 |
| `CalendarSourceType` | `MANUAL`、`ACTIVITY`、`CHANGELOG`、`COMMUNITY_POLL`、`OPS_PLACEHOLDER` | 来源类型。P1 只真实支持 `MANUAL` 和 `ACTIVITY`。 |
| `CalendarReminderChannel` | `IN_APP`、`EMAIL`、`QQ`、`OOPZ`、`GAME` | 提醒渠道。P1 只保存摘要，不真实投递外部渠道。 |
| `CalendarReminderStatus` | `SKIPPED`、`PENDING`、`DELIVERED`、`FAILED` | 最近一次提醒摘要。P1 默认 `SKIPPED`。 |
| `CalendarSyncStatus` | `NOT_CONFIGURED`、`SYNCED`、`FAILED`、`STALE`、`SKIPPED` | 来源同步状态。 |
| `CalendarAuditResult` | `SUCCESS`、`FAILED` | calendar 审计结果。 |

## 通用对象

### CalendarReminderPolicy

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `enabled` | boolean | 是 | 是否启用提醒摘要。 |
| `offsetMinutes` | integer[] | 是 | 提前提醒分钟数，0 到 10080，最多 5 个。 |
| `channels` | string[] | 是 | `CalendarReminderChannel`。P1 只能保存 `IN_APP`。 |
| `lastReminderStatus` | string | 是 | `CalendarReminderStatus`。 |
| `lastReminderAt` | string 或 null | 是 | 最近提醒摘要时间。 |
| `failure` | CalendarReminderFailureSummary 或 null | 是 | 脱敏失败摘要。 |

### CalendarReminderFailureSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | 固定为 `FAILED`。 |
| `failureCode` | string | 是 | `49820`、`49821` 或 `49822`。 |
| `failureType` | string | 是 | `UNAVAILABLE`、`TIMEOUT` 或 `BAD_SCHEMA`。 |
| `failureReason` | string | 是 | 脱敏失败摘要。 |
| `failedAt` | string | 是 | 失败发生时间。 |

### CalendarSourceRef

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sourceType` | string | 是 | `CalendarSourceType`。 |
| `sourceId` | string 或 null | 是 | 来源业务 ID。 |
| `sourceVersion` | string 或 null | 是 | 来源版本或更新时间摘要。 |
| `sourceUrl` | string 或 null | 是 | 来源公开或后台跳转地址。 |
| `syncStatus` | string | 是 | `CalendarSyncStatus`。 |
| `sourceSnapshotStale` | boolean | 是 | 是否使用旧来源快照。 |
| `lastSyncedAt` | string 或 null | 是 | 最近同步时间。 |
| `failure` | object 或 null | 是 | 脱敏同步失败摘要，不包含 token、内部路径和堆栈。 |

### CalendarEvent

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `eventId` | string | 是 | 事件 ID。 |
| `source` | CalendarSourceRef | 是 | 来源引用。 |
| `title` | string | 是 | 2 到 100 位。 |
| `summary` | string | 是 | 1 到 300 位。 |
| `description` | string | 否 | 最多 5000 位，公开详情可见。 |
| `type` | string | 是 | `CalendarEventType`。 |
| `status` | string | 是 | `CalendarEventStatus`。 |
| `visibility` | string | 是 | `CalendarVisibility`。 |
| `startAt` | string | 是 | ISO 8601，事件开始时间。 |
| `endAt` | string | 是 | ISO 8601，必须晚于 `startAt`。全天事件由服务端写入完整 UTC 范围。 |
| `timezone` | string | 是 | IANA 时区，P1 默认 `Asia/Shanghai`。 |
| `allDay` | boolean | 是 | 是否全天事件。 |
| `location` | string 或 null | 是 | 地点或线上说明，最多 200 位。 |
| `relatedUrl` | string 或 null | 是 | http、https 或站内路径，最多 500 位。 |
| `labels` | string[] | 是 | 0 到 8 个标签，每个 1 到 24 位。 |
| `priority` | integer | 是 | 0 到 100，越大越靠前。 |
| `watchCount` | integer | 是 | 当前关注人数。 |
| `watchedByCurrentUser` | boolean | 当前用户接口或可选登录视图 | 当前用户是否关注。 |
| `reminderPolicy` | CalendarReminderPolicy | 是 | 提醒摘要。 |
| `createdBy` | string | 后台可见 | 创建者用户 ID。 |
| `updatedBy` | string | 后台可见 | 最近修改者用户 ID。 |
| `reviewedBy` | string 或 null | 后台可见 | 审核者用户 ID。 |
| `reviewComment` | string 或 null | 是 | 给创建者的审核意见。公开接口仅可返回可展示摘要。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注，公开和当前用户接口不得返回。 |
| `submittedAt` | string 或 null | 是 | 提交审核时间。 |
| `reviewedAt` | string 或 null | 是 | 审核时间。 |
| `publishedAt` | string 或 null | 是 | 发布时间。 |
| `offlineAt` | string 或 null | 是 | 下架时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |
| `deletedAt` | string 或 null | 后台可见 | 软删除时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `lastSyncedAt` | string 或 null | 是 | 最近来源同步时间。 |

### CalendarWatch

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `watchId` | string | 是 | 关注记录 ID。 |
| `eventId` | string | 是 | 事件 ID。 |
| `userId` | string | 是 | auth 用户 ID。 |
| `displayNameSnapshot` | string | 是 | 用户展示名快照。 |
| `reminderEnabled` | boolean | 是 | 当前用户是否启用提醒摘要。 |
| `reminderOffsets` | integer[] | 是 | 当前用户提醒分钟数。 |
| `status` | string | 是 | P1 固定为 `ACTIVE` 或 `CANCELED`。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `canceledAt` | string 或 null | 是 | 取消关注时间。 |

### CalendarAuditLog

审计字段继承公共契约，允许补充 `eventId`、`watchId`、`sourceType`、`sourceId`、`stateFrom`、`stateTo`、`idempotencyKey`、`syncStatus`、`reminderStatus`、`dependencyStatus` 和 `sourceSnapshotStale`。审计日志不得通过 calendar API 删除。

## calendar 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `49900` | 404 | 日程事件不存在，或公开接口无权访问。 |
| `49901` | 404 | 关注记录不存在，或当前用户无权访问。 |
| `49902` | 404 | 来源快照不存在。 |
| `49910` | 409 | 事件状态不允许当前操作。 |
| `49911` | 409 | 事件时间范围冲突。 |
| `49912` | 409 | 同一来源事件已存在。 |
| `49913` | 409 | 事件关注状态冲突。 |
| `49914` | 409 | 幂等键请求指纹冲突。 |
| `49915` | 409 | 提醒策略不允许当前操作。 |
| `49916` | 409 | 同一用户已关注该事件。 |
| `49800` | 502 | auth 认证上下文不可用。 |
| `49801` | 504 | auth 认证上下文超时。 |
| `49802` | 502 | auth 认证上下文字段或枚举不兼容。 |
| `49810` | 502 | activity 日历摘要不可用。 |
| `49811` | 504 | activity 日历摘要调用超时。 |
| `49812` | 502 | activity 日历摘要字段或枚举不兼容。 |
| `49820` | 502 | notification 提醒依赖不可用。 |
| `49821` | 504 | notification 提醒依赖超时。 |
| `49822` | 502 | notification 提醒依赖字段或枚举不兼容。 |
| `49830` | 502 | changelog 来源不可用或未连接。 |
| `49831` | 504 | changelog 来源超时。 |
| `49832` | 502 | changelog 来源字段或枚举不兼容。 |
| `54800` | 500 | calendar 内部错误。 |
| `54801` | 500 | calendar 审计写入失败。 |
| `54802` | 500 | calendar 状态写入失败。 |
| `54803` | 500 | calendar 关注写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误、通用幂等键冲突和通用服务端错误优先使用公共错误码。calendar 自有幂等指纹冲突使用 `49914`。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开事件列表 | GET | `/api/v1/calendar/events` | 否 | 无 | LOW |
| 公开事件详情 | GET | `/api/v1/calendar/events/{eventId}` | 否 | 无 | LOW |
| 月视图 | GET | `/api/v1/calendar/month` | 否 | 无 | LOW |
| 即将开始 | GET | `/api/v1/calendar/upcoming` | 否 | 无 | LOW |
| 我的关注列表 | GET | `/api/v1/calendar/me/watchlist` | 是 | 当前用户 | LOW |
| 关注事件 | POST | `/api/v1/calendar/me/events/{eventId}/watch` | 是 | 当前用户 | LOW |
| 取消关注事件 | POST | `/api/v1/calendar/me/events/{eventId}/unwatch` | 是 | 当前用户 | LOW |
| 后台事件列表 | GET | `/api/v1/calendar/admin/events` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 后台事件详情 | GET | `/api/v1/calendar/admin/events/{eventId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 创建事件 | POST | `/api/v1/calendar/admin/events` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 修改事件 | PATCH | `/api/v1/calendar/admin/events/{eventId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 提交审核 | POST | `/api/v1/calendar/admin/events/{eventId}/submit` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核通过 | PATCH | `/api/v1/calendar/admin/events/{eventId}/approve` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/calendar/admin/events/{eventId}/reject` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 发布事件 | PATCH | `/api/v1/calendar/admin/events/{eventId}/publish` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 下架事件 | PATCH | `/api/v1/calendar/admin/events/{eventId}/offline` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 归档事件 | PATCH | `/api/v1/calendar/admin/events/{eventId}/archive` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 软删除事件 | PATCH | `/api/v1/calendar/admin/events/{eventId}/delete` | 是 | `ADMIN`、`OWNER` | HIGH |
| 同步 activity 摘要 | POST | `/api/v1/calendar/admin/sync/activity` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 审计列表 | GET | `/api/v1/calendar/admin/audit-logs` | 是 | `ADMIN`、`OWNER` | LOW |
| calendar 自检摘要 | GET | `/api/v1/calendar/admin/ops/summary` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |

## 公开接口

### 公开事件列表

`GET /api/v1/calendar/events`

查询参数：`page`、`pageSize`、`keyword`、`type`、`visibility`、`from`、`to`、`label`、`sourceType` 和 `sort`。`pageSize` 最大 `100`。`sort` 允许 `startAt_asc`、`startAt_desc`、`priority_desc`、`publishedAt_desc`、`updatedAt_desc`。

成功响应 HTTP `200`，分页 `items` 为公开视图 `CalendarEvent[]`。

业务规则：游客只看到 `PUBLISHED` 且 `visibility=PUBLIC` 的事件。登录用户的成员和工作人员可见范围可以由后续前端或网关适配，但 P1 公开接口默认不读取登录态扩展可见性，避免游客和登录视图混淆。列表不得返回内部备注、后台审核字段、通知失败详情、审计字段和未发布来源失败。

时间范围规则：`from` 和 `to` 使用 ISO 8601。事件只要与查询范围重叠就必须返回，条件为 `event.endAt > from && event.startAt < to`。跨月事件必须出现在涉及月份。全天事件按服务端保存的完整时间范围参与重叠判断。

### 公开事件详情

`GET /api/v1/calendar/events/{eventId}`

成功响应 HTTP `200`，`data` 为公开视图 `CalendarEvent`。事件不存在、不可见、已下架、已归档或已删除时返回 `49900`。公开详情不得返回内部备注、操作者 ID、审计参数、完整依赖失败或来源内部路径。

### 月视图

`GET /api/v1/calendar/month`

查询参数：`month` 必填，格式 `YYYY-MM`；可选 `type`、`visibility` 和 `sourceType`。成功响应 HTTP `200`，`data` 包含 `month`、`timezone`、`rangeStart`、`rangeEnd`、`items` 和 `degraded`。`items` 为与该月重叠的公开事件摘要。`rangeStart` 和 `rangeEnd` 由服务端按 `Asia/Shanghai` 计算，不能要求前端自己拼时间。

### 即将开始

`GET /api/v1/calendar/upcoming`

查询参数：`limit` 默认 `10`，最大 `50`；可选 `from`、`days`、`type` 和 `sourceType`。成功响应 HTTP `200`，`data.items` 为从服务端当前时间或 `from` 开始的未来公开事件摘要。`days` 默认 `30`，最大 `180`。同一开始时间按 `priority` 和 `eventId` 稳定排序。

## 当前用户接口

### 我的关注列表

`GET /api/v1/calendar/me/watchlist`

查询参数：`page`、`pageSize`、`status`、`type`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为当前用户关注记录与事件摘要。只能返回当前认证用户自己的关注，不得通过请求参数传入 `userId`。

过滤规则：`status` 只允许 `ACTIVE` 和 `CANCELED`；`type` 按事件类型过滤；`from` 和 `to` 使用事件时间范围重叠规则；`sort` 允许 `updatedAt_desc`、`createdAt_desc` 和 `startAt_asc`。非法 `status` 返回 `40001`，非法 `sort` 返回 `40003`，非法时间返回 `40001`，非法范围返回 `49911`。

### 关注事件

`POST /api/v1/calendar/me/events/{eventId}/watch`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reminderEnabled` | boolean | 否 | 默认 `true`。 |
| `reminderOffsets` | integer[] | 否 | 默认 `[60]`，最多 5 个，范围 0 到 10080。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `201` 或重复关注幂等 HTTP `200`，`data` 为 `CalendarWatch` 和事件摘要。只允许关注公开可见、未下架、未归档、未删除的事件。并发关注同一用户同一事件只能产生一条有效记录。相同幂等键同请求体重复提交返回同一结果，相同键不同体返回 `49914`。

### 取消关注事件

`POST /api/v1/calendar/me/events/{eventId}/unwatch`

请求字段：`reason` 可选，最多 200 位；`idempotencyKey` 可选。成功响应 HTTP `200`，`data` 为取消后的关注摘要。未关注时返回幂等成功，不能把 `watchCount` 扣成负数。取消关注不删除历史记录，只标记为 `CANCELED`。

## 后台事件接口

### 后台事件列表和详情

`GET /api/v1/calendar/admin/events` 支持 `page`、`pageSize`、`keyword`、`type`、`status`、`visibility`、`sourceType`、`createdBy`、`from`、`to` 和 `sort`。后台可查看全部非物理删除事件，默认按 `updatedAt_desc`。`GET /api/v1/calendar/admin/events/{eventId}` 返回事件、关注统计、来源同步摘要、提醒摘要、最近审计和依赖摘要。响应不得返回 token、完整请求头、通知正文、前序服务内部路径、异常堆栈、真实服务器命令、节点凭据或 Cloudreve token。

后台事件列表过滤规则：`keyword` 匹配标题或摘要；`type`、`status`、`visibility`、`sourceType` 和 `createdBy` 精确匹配；`from` 和 `to` 使用事件时间范围重叠规则；非法枚举返回 `40001`；非法时间返回 `40001`；非法范围返回 `49911`。分页和排序继续遵守公共契约。

### 创建事件

`POST /api/v1/calendar/admin/events`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `title` | string | 是 | 2 到 100 位。 |
| `summary` | string | 是 | 1 到 300 位。 |
| `description` | string | 否 | 最多 5000 位。 |
| `type` | string | 是 | `CalendarEventType`。 |
| `visibility` | string | 是 | `CalendarVisibility`。 |
| `startAt` | string | 是 | ISO 8601。 |
| `endAt` | string | 是 | ISO 8601，必须晚于 `startAt`。 |
| `timezone` | string | 否 | 默认 `Asia/Shanghai`。 |
| `allDay` | boolean | 否 | 默认 `false`。 |
| `location` | string 或 null | 否 | 最多 200 位。 |
| `relatedUrl` | string 或 null | 否 | http、https 或站内路径。 |
| `labels` | string[] | 否 | 最多 8 个。 |
| `priority` | integer | 否 | 0 到 100，默认 50。 |
| `reminderPolicy` | object | 否 | 按 `CalendarReminderPolicy` 校验。 |
| `sourceType` | string | 否 | 默认 `MANUAL`。P1 后台创建只允许 `MANUAL` 和未来占位 `CHANGELOG`。 |
| `sourceId` | string 或 null | 否 | 来源 ID。`MANUAL` 可为空。 |
| `internalNote` | string 或 null | 否 | 后台备注，最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `201`，状态为 `DRAFT`。同一 `sourceType` 和 `sourceId` 的非终态事件不得重复创建，返回 `49912`。`type=MAINTENANCE` 和 `SERVER_SCHEDULE` 只能保存日程元数据，不得触发服务器操作。

P1 来源创建限制：后台创建接口传入 `ACTIVITY`、`COMMUNITY_POLL` 或 `OPS_PLACEHOLDER` 时返回 `40001`，提示来源只能由对应来源同步或后续模块适配产生。`CHANGELOG` 只允许 `type=VERSION_RELEASE`，并且必须带 `sourceId`，不得保存 changelog 正文主数据。

### 修改事件

`PATCH /api/v1/calendar/admin/events/{eventId}`

请求字段同创建事件，除 `reason` 必填外其余字段按需修改。只允许 `DRAFT`、`NEEDS_CHANGES`、`REJECTED` 和未发布的 `APPROVED` 修改主体字段。`PUBLISHED` 事件如需改时间，P1 必须先下架后修改再发布，避免用户关注和提醒失真。非法状态返回 `49910`。

### 审核发布状态

`POST /api/v1/calendar/admin/events/{eventId}/submit` 使 `DRAFT`、`NEEDS_CHANGES` 或 `REJECTED` 进入 `PENDING_REVIEW`。

`PATCH /api/v1/calendar/admin/events/{eventId}/approve` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，使 `PENDING_REVIEW` 或 `NEEDS_CHANGES` 进入 `APPROVED`。

`PATCH /api/v1/calendar/admin/events/{eventId}/reject` 请求字段同审核通过，使 `PENDING_REVIEW` 进入 `REJECTED`。

`PATCH /api/v1/calendar/admin/events/{eventId}/publish` 请求字段为 `reason`、`idempotencyKey`，使 `APPROVED` 或 `OFFLINE` 进入 `PUBLISHED`，写入 `publishedAt`。辅助通知失败不回滚主状态，但必须保存提醒失败摘要和审计。

`PATCH /api/v1/calendar/admin/events/{eventId}/offline` 请求字段为 `publicReason`、`reason`、`idempotencyKey`，使 `PUBLISHED` 进入 `OFFLINE`。下架后公开接口不可见，但关注记录保留。

`PATCH /api/v1/calendar/admin/events/{eventId}/archive` 请求字段为 `reason`、`idempotencyKey`，使 `DRAFT`、`REJECTED`、`NEEDS_CHANGES` 或 `OFFLINE` 进入 `ARCHIVED`。已发布事件必须先下架再归档。

`PATCH /api/v1/calendar/admin/events/{eventId}/delete` 是 `HIGH` 风险，请求字段为 `reason`、`confirmText`、`idempotencyKey`，P1 固定要求 `DELETE_CALENDAR_EVENT`。成功后状态为 `DELETED`，只做软删除，不物理删除事件、关注记录和审计线索。

### 同步 activity 摘要

`POST /api/v1/calendar/admin/sync/activity`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `from` | string | 是 | ISO 8601 起始时间。 |
| `to` | string | 是 | ISO 8601 结束时间，必须晚于 `from`。 |
| `mode` | string | 否 | `UPSERT_SNAPSHOT` 或 `DRY_RUN`，默认 `UPSERT_SNAPSHOT`。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 包含 `syncStatus`、`createdTotal`、`updatedTotal`、`skippedTotal`、`failedTotal`、`items`、`activityMode` 和 `lastSyncedAt`。`DRY_RUN` 只返回差异，不写事件、不写关注、不改变来源状态。`UPSERT_SNAPSHOT` 按 `sourceType=ACTIVITY` 和 `sourceId=activityId` 创建或更新 calendar 事件快照，状态默认 `PUBLISHED`，但不得修改 activity 主数据。

失败降级：activity 不可用时返回 `49810` 或在实现固定策略下返回 `syncStatus=FAILED` 的成功响应；同一版本内必须固定并测试。无论哪种策略，失败不得删除已有 activity 来源事件。

## 审计和自检

`GET /api/v1/calendar/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`eventId`、`sourceType`、`result`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为 `CalendarAuditLog[]`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 calendar API 删除。

审计列表过滤规则：`actorUserId`、`action`、`targetType`、`targetId`、`eventId`、`sourceType` 和 `result` 精确匹配；`from` 和 `to` 按审计 `createdAt` 范围过滤；`sort` 允许 `createdAt_desc` 和 `createdAt_asc`。非法 `result` 返回 `40001`，非法 `sort` 返回 `40003`，非法时间返回 `40001`，非法范围返回 `49911`。

`GET /api/v1/calendar/admin/ops/summary` 成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "calendar",
    "port": 8132,
    "legacyPort": 8114,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "activityMode": "TEST_STUB",
    "notificationMode": "SKIPPED",
    "changelogMode": "NOT_CONNECTED",
    "testControlsEnabled": false,
    "eventsTotal": 8,
    "publishedEventsTotal": 4,
    "watchesTotal": 6,
    "activitySourceEventsTotal": 2,
    "manualEventsTotal": 6,
    "auditsTotal": 20,
    "idempotencyRecordsTotal": 12,
    "lastActivitySyncAt": "2026-05-25T12:00:00Z",
    "lastAuditAt": "2026-05-25T12:05:00Z",
    "productionGaps": [
      "P1_IN_MEMORY_STORAGE",
      "P1_AUTH_STUB",
      "P1_ACTIVITY_STUB",
      "NOTIFICATION_DELIVERY_NOT_CONNECTED",
      "CHANGELOG_NOT_CONNECTED",
      "TEST_CONTROLS_DISABLED_OUTSIDE_TEST"
    ]
  }
}
```

## 状态、幂等和并发

事件创建后为 `DRAFT`。`DRAFT` 可提交为 `PENDING_REVIEW`。`PENDING_REVIEW` 可通过为 `APPROVED`、拒绝为 `REJECTED`。`NEEDS_CHANGES` 和 `REJECTED` 可修改后再次提交。`APPROVED` 可发布为 `PUBLISHED`。`PUBLISHED` 可下架为 `OFFLINE`。`OFFLINE` 可重新发布、归档或软删除。`DRAFT`、`REJECTED`、`NEEDS_CHANGES` 和 `OFFLINE` 可归档或软删除。`ARCHIVED` 和 `DELETED` 为终态，不得回到公开状态。

写接口支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `49914`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序。

并发关注同一用户同一事件只能产生一条有效关注记录。重复取消关注保持幂等。关注计数必须和有效关注记录一致，不得小于 0。并发审核、发布、下架、归档和软删除同一事件只能有一个成功状态推进。读取接口允许读到更新前或更新后的完整状态，不能返回半更新对象。

P1 内存实现必须用本服务内的串行临界区保护事件状态、关注计数、来源同步和审计写入。后续持久化实现必须迁移为数据库事务、唯一约束、条件更新或等效机制，不能降低上述并发口径。

## 审计要求

必须审计的动作包括事件创建、事件修改、提交审核、审核通过、审核拒绝、发布、下架、归档、软删除、关注事件、取消关注、activity 同步、同步失败、提醒失败、依赖降级、自检读取、审计写入失败和状态写入失败。

后台写操作必须记录 `reason`、操作者、目标对象、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计不得泄露 token、完整请求头、通知正文、前序服务内部路径、真实服务器命令、节点凭据、Cloudreve token、内部异常堆栈或 activity 内部实现。

审计写入失败时，事件创建、修改、审核、发布、下架、归档、软删除、activity 同步和后台状态写入不得假装成功，必须返回 `54801` 或 `54800`，并保持业务数据不变。普通用户关注和取消关注在 P1 也必须保证审计和关注计数一致，失败返回 `54803` 或 `54801`，不得产生半关注状态。

## 失败降级

auth 是所有登录接口强依赖。auth 不可用、超时、用户状态不允许或字段不兼容时不得伪造成功。

activity 是只读同步来源。同步失败不得删除已有事件；公开读取可展示已保存快照并标记 stale。字段不兼容必须记录依赖失败摘要，不能保存伪造时间。

notification 是辅助依赖。提醒摘要失败不得回滚事件发布或关注主状态，但必须保存脱敏失败摘要和审计。

changelog 当前未连接。手工版本更新事件不依赖 changelog；未来 changelog 同步必须作为兼容变更补契约、测试和适配器。

维护窗口和服务器日程只是日程展示。任何真实服务器操作不可通过 calendar 降级或回退执行。

## 验收口径

`calendar` API 文档按 `docs/contracts-calendar.md` 独立存在，并由 `.local-docs/tests-calendar.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`calendar` 完成时必须满足以下条件：全部接口按本文档实现；公开接口只返回公开可见事件；当前用户只能维护自己的关注；后台接口按角色限制；事件状态机不可非法回退；跨月、时间范围重叠和全天事件查询正确；activity 同步只读且有失败降级；changelog 只保留占位；维护窗口不触发真实运维；所有写操作有审计；当前运行端口固定为 `8132`，自检摘要返回 `port=8132` 和 `legacyPort=8114`；`.local-docs/tests-calendar.md` 与 `.local-docs/tests-engagement-core.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 calendar 在 `engagement-core-service` 中全部测试通过；auth、profile、notification、content、server-status、resource、admin、onboarding、exam、whitelist、attendance、community 和 activity 前序服务回归测试通过；不恢复 `backend/calendar-service` 旧入口；没有修改前序服务稳定接口；没有把更新日志主数据、活动报名、社区投票、后台聚合、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复或 Cloudreve 管理能力塞进 calendar。

生产化硬化验收还必须满足：测试控制头默认关闭，只有本地自动化测试显式启用时才生效；关闭状态下依赖失败模拟头、写入失败模拟头、时间模拟头和通知失败模拟头全部被忽略；自检摘要明确返回当前测试控制头开关状态。
