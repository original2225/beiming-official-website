# 北冥官网 changelog API 契约

版本：0.1

## 文档定位

本文档是 `changelog` 微服务的正式 API 契约。后续前端适配、`admin` 聚合、`ops-control`、`node-daemon` 和其他业务模块只能通过本文档定义的接口读取或管理更新日志，不能直接读取或修改 `changelog` 数据库，也不能把公告、资源下载、服务器运维、日历主数据或活动报名逻辑塞进 `changelog`。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `changelog` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了成熟更新发布平台和开源生态的公开做法。Keep a Changelog 1.1.0 强调变更应按版本组织，并按 `Added`、`Changed`、`Deprecated`、`Removed`、`Fixed`、`Security` 分组，说明更新日志要让人读懂，而不是只堆原始提交。GitHub Releases API 把 release、tag、draft、prerelease 和 assets 分开，说明发布对象、发布状态和附件链接需要结构化。GitLab Releases API 把 release、milestones 和 asset links 分开，说明发布记录可以关联外部资产但不应吞掉来源系统。Kubernetes release notes 把版本、升级影响、功能状态、已知问题和变更项分层表达，说明面向运维和玩家的更新说明需要明确影响范围。本项目只吸收这些产品思路，不接入 GitHub、GitLab、Kubernetes、CI 发布、Git tag 或外部仓库主数据。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) | 更新日志按版本和人类可读分组组织，避免把 raw commit 当正式说明。 |
| [GitHub REST API Releases](https://docs.github.com/en/rest/releases/releases) | release、draft、prerelease、tag 和资产链接需要分离。 |
| [GitLab Releases API](https://docs.gitlab.com/api/releases/) | 发布对象可以关联 milestone 和 asset links，但不接管外部系统主数据。 |
| [Kubernetes Release Notes](https://kubernetes.io/releases/notes/) | 版本说明要表达影响范围、升级注意事项、已知问题和变更等级。 |

## 职责边界

`changelog` 负责服务器版本、插件变更、规则调整、资源包更新、地图更新、重要维护记录、发布说明、变更分组、影响范围、关联资源快照、关联日程快照、关联内容页快照、通知投递摘要、changelog 审计、自检摘要和自身幂等记录。

`changelog` 不负责官网公告主发布、内容专题主数据、资源文件下载、资源版本创建、Cloudreve 分享链接生成、玩家可见服务器状态采集、真实服务器维护、容器、终端、文件管理、节点守护、日历事件主数据、活动报名结果、社区帖子、考勤积分、白名单或运维审批。

`changelog` 只能后序适配前序服务。它通过 `auth` 认证上下文读取当前用户、角色、能力点和用户状态；通过 `resource` 公开或后台正式接口保存资源和版本快照；通过 `server-status` 正式接口保存实例名称、Minecraft 版本和线路摘要；通过 `content` 正式接口保存公开说明页快照；通过 `calendar` 兼容接口或本服务内同步摘要保存版本发布日期程引用；通过 `notification` 投递发布、下架、安全修复和规则调整通知摘要。`changelog` 不导入前序服务内部类、Repository、内存存储、测试种子或数据库表。

## 数据归属

`changelog` 拥有以下主数据：发布记录、版本记录、变更分组、变更项、影响范围、兼容说明、已知问题、回滚说明、关联资源快照、关联服务器实例快照、关联日历摘要、关联内容快照、通知投递摘要、依赖调用摘要、幂等记录、changelog 审计日志和自检统计。

`changelog` 可以保存来自 `auth` 的 `userId`、`displayName`、`roles`、`permissions` 和用户状态快照；可以保存来自 `resource` 的 `resourceId`、`slug`、`versionName`、`visibility` 和可用性摘要；可以保存来自 `server-status` 的 `instanceId`、`name`、`minecraftVersion` 和状态快照摘要；可以保存来自 `content` 的 `contentId`、`slug`、`title` 和公开地址；可以保存来自 `calendar` 的 `eventId`、时间和同步状态；可以保存来自 `notification` 的投递结果摘要。所有快照只服务展示、检索和审计，不能成为来源模块主数据，也不能反写来源模块。

## 基础路径与认证

所有接口默认使用 `/api/v1/changelog` 前缀。P1 本地端口固定为 `8115`，自检摘要必须返回该端口。

公开接口允许游客读取 `PUBLISHED` 且 `visibility=PUBLIC` 的发布记录。公开接口不得返回内部备注、后台审核字段、通知失败详情、完整依赖错误、审计参数、安全修复 exploit 细节、服务器内部路径、节点地址、token、真实运维命令或 Cloudreve 管理信息。

当前用户接口使用 `/api/v1/changelog/me` 前缀，全部要求 `Authorization: Bearer <token>`。当前用户只能读取和维护自己的收藏记录。浏览器请求体不得传入 `userId`、`roles`、`permissions`、`bookmarkCount`、`status`、`operatorUserId`、`reviewerUserId`、`auditResult`、`notificationStatus` 等服务端可信字段。

后台接口使用 `/api/v1/changelog/admin` 前缀，全部要求登录。后台读取发布记录、自检摘要和普通管理详情要求 `HELPER`、`ADMIN` 或 `OWNER`。创建草稿、修改自己创建的未发布草稿、提交审核、初审意见允许 `HELPER`、`ADMIN` 或 `OWNER`。发布、下架、归档、软删除、日历同步、审计读取和安全修复公开策略调整只允许 `ADMIN` 或 `OWNER`。

## 本地测试控制头

`changelog` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Resource-Mode`、`X-Test-Server-Status-Mode`、`X-Test-Content-Mode`、`X-Test-Calendar-Mode`、`X-Test-Notification-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store`、`X-Test-Fail-Bookmark` 和 `X-Test-Now` 模拟依赖失败、通知失败、写入失败、收藏并发冲突和时间边界。该能力只服务本地测试闭环，不属于正式业务 API。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、状态失败、收藏失败、通知失败、时间模拟或快照 stale。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 视为已满足的生产化硬化项。

## 前序服务兼容契约

`auth` 是所有登录接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `ACTIVE` 时可收藏和后台写入；`PENDING_PROFILE` 可以收藏公开版本但不能后台写入；`DISABLED`、`BANNED`、`DELETED` 不允许写入。auth 不可用返回 `49100`，auth 超时返回 `49101`，字段或枚举不兼容返回 `49102`。

`resource` 是资源包、地图文件、整合包和文档资源的可选关联来源。创建或修改关联资源时只能读取正式接口返回的公开或后台快照，不能生成下载票据，不能修改资源状态，不能保存 Cloudreve token。resource 不可用返回 `49110`，超时返回 `49111`，字段或枚举不兼容返回 `49112`。公开读取已有发布记录时可以使用旧资源快照并标记 `resourceSnapshotStale=true`。

`server-status` 是玩家可见服务器实例状态和版本摘要来源。`changelog` 可以保存实例名称、当前 Minecraft 版本和线路摘要，不能刷新状态、记录在线人数历史或执行宕机处理。server-status 不可用返回 `49120`，超时返回 `49121`，字段或枚举不兼容返回 `49122`。读取已有记录可展示旧快照并标记 stale。

`content` 是说明页、公告页、专题页和 SEO 的归属服务。`changelog` 可以关联公开说明页快照，不能创建公告，不能管理首页配置，不能吞掉 content 审核发布流。content 不可用返回 `49130`，超时返回 `49131`，字段或枚举不兼容返回 `49132`。

`calendar` 是版本发布日程和维护窗口日程的归属服务。P1 中 `changelog` 默认不反向写 calendar 主数据，只保存 `calendarSyncStatus=SKIPPED` 或测试控制下的失败摘要。若后续需要真实写入 calendar，必须先确认 `docs/contracts-calendar.md` 已有兼容写接口；接口不足时先按前序兼容变更流程处理。calendar 不可用记录或返回 `49140`，超时记录或返回 `49141`，字段不兼容记录或返回 `49142`。calendar 同步失败不得回滚 changelog 主状态。

`notification` 是辅助依赖。发布、下架、安全修复、规则调整、资源包更新可以触发通知。通知失败不得回滚发布或下架主状态，但必须保存 `notificationStatus=FAILED`、失败原因摘要并写入审计。notification 不可用记录或返回 `49150`，超时记录或返回 `49151`，字段不兼容记录或返回 `49152`。通知失败摘要不得包含通知正文、完整请求头、认证 token、内部 URL 或异常堆栈。

`admin` 当前只是聚合入口。本文档不修改 `admin` 契约。若后续需要 admin 展示 changelog 待办、指标或审计索引，必须作为 admin 兼容变更单独走契约、测试、红灯、实现和回归闭环。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `ChangelogType` | `SERVER_VERSION`、`PLUGIN_CHANGE`、`RULE_CHANGE`、`RESOURCE_PACK`、`MAP_UPDATE`、`MAINTENANCE`、`SECURITY`、`OTHER` | 发布记录主类型。 |
| `ChangelogGroupType` | `ADDED`、`CHANGED`、`DEPRECATED`、`REMOVED`、`FIXED`、`SECURITY`、`PERFORMANCE`、`KNOWN_ISSUE` | 变更分组类型。 |
| `ChangelogStatus` | `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`PUBLISHED`、`OFFLINE`、`ARCHIVED`、`DELETED` | 发布记录主状态。 |
| `ChangelogVisibility` | `PUBLIC`、`MEMBER_ONLY`、`STAFF_ONLY` | 可见范围。P1 公开接口只返回 `PUBLIC`。 |
| `ChangelogImpactLevel` | `LOW`、`MEDIUM`、`HIGH`、`CRITICAL` | 影响等级。`SECURITY` 类型不能低于 `MEDIUM`。 |
| `ChangelogItemSeverity` | `INFO`、`MINOR`、`MAJOR`、`BREAKING`、`SECURITY` | 单条变更严重度。 |
| `ChangelogNotificationStatus` | `SKIPPED`、`PENDING`、`DELIVERED`、`FAILED` | 最近通知摘要。P1 默认 `SKIPPED`。 |
| `ChangelogSyncStatus` | `SKIPPED`、`SYNCED`、`FAILED`、`STALE` | 关联日历或来源快照同步状态。 |
| `ChangelogBookmarkStatus` | `ACTIVE`、`CANCELED` | 当前用户收藏状态。 |
| `ChangelogAuditResult` | `SUCCESS`、`FAILED` | 审计结果。 |

## 通用对象

### ChangelogRelease

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `releaseId` | string | 是 | 发布记录 ID。 |
| `slug` | string | 是 | 2 到 100 位，小写字母、数字和短横线，全局唯一。 |
| `versionName` | string | 是 | 1 到 80 位，展示版本名，例如 `v1.20.4-2026.06`。 |
| `title` | string | 是 | 2 到 120 位。 |
| `summary` | string | 是 | 1 到 300 位。 |
| `body` | string | 是 | 1 到 10000 位，公开正文。 |
| `type` | string | 是 | `ChangelogType`。 |
| `status` | string | 是 | `ChangelogStatus`。 |
| `visibility` | string | 是 | `ChangelogVisibility`。 |
| `impactLevel` | string | 是 | `ChangelogImpactLevel`。 |
| `releasedAt` | string 或 null | 是 | 对玩家宣告发布时间。 |
| `effectiveAt` | string 或 null | 是 | 实际生效时间。 |
| `minecraftVersion` | string 或 null | 是 | Minecraft 版本摘要。 |
| `pluginVersions` | object[] | 是 | 插件名称、版本、动作和公开备注。 |
| `resourcePackVersions` | object[] | 是 | 资源包名称、版本、资源快照 ID。 |
| `mapVersion` | string 或 null | 是 | 地图版本摘要。 |
| `groups` | ChangelogGroup[] | 是 | 变更分组。至少 1 组，每组至少 1 项。 |
| `compatibilityNotes` | string 或 null | 是 | 兼容说明，最多 2000 位。 |
| `knownIssues` | string 或 null | 是 | 已知问题，最多 2000 位。 |
| `rollbackNotes` | string 或 null | 后台可见或公开摘要 | 回滚说明，最多 2000 位。公开视图不得泄露运维内部路径或命令。 |
| `securityPublicSummary` | string 或 null | `SECURITY` 时必填 | 安全修复公开摘要，不含 exploit 细节。 |
| `relatedResources` | ChangelogRelatedResource[] | 是 | 关联资源快照。 |
| `relatedServerInstances` | object[] | 是 | 关联服务器实例快照。 |
| `relatedCalendarEvent` | ChangelogCalendarRef 或 null | 是 | 关联日程摘要。 |
| `relatedContent` | object 或 null | 是 | 说明页公开快照。 |
| `notificationSummary` | ChangelogNotificationSummary | 是 | 最近通知摘要。 |
| `bookmarkCount` | integer | 是 | 有效收藏数。 |
| `bookmarkedByCurrentUser` | boolean | 当前用户视图可见 | 当前用户是否收藏。 |
| `createdBy` | string | 后台可见 | 创建者用户 ID。 |
| `updatedBy` | string | 后台可见 | 最近修改者用户 ID。 |
| `reviewedBy` | string 或 null | 后台可见 | 审核者用户 ID。 |
| `reviewComment` | string 或 null | 是 | 给创建者的审核意见。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注，公开和当前用户接口不得返回。 |
| `submittedAt` | string 或 null | 是 | 提交审核时间。 |
| `reviewedAt` | string 或 null | 是 | 审核时间。 |
| `publishedAt` | string 或 null | 是 | 发布时间。 |
| `offlineAt` | string 或 null | 是 | 下架时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |
| `deletedAt` | string 或 null | 后台可见 | 软删除时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ChangelogGroup

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `groupId` | string | 是 | 分组 ID。 |
| `type` | string | 是 | `ChangelogGroupType`。 |
| `title` | string | 是 | 1 到 80 位。 |
| `description` | string 或 null | 是 | 分组说明，最多 1000 位。 |
| `items` | ChangelogItem[] | 是 | 变更项，至少 1 项。 |
| `sortOrder` | integer | 是 | 0 到 999。 |

### ChangelogItem

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `itemId` | string | 是 | 变更项 ID。 |
| `title` | string | 是 | 1 到 120 位。 |
| `description` | string | 是 | 1 到 2000 位。 |
| `severity` | string | 是 | `ChangelogItemSeverity`。 |
| `component` | string 或 null | 是 | 影响组件，例如 `server`、`plugin:CoreProtect`、`resource-pack`。 |
| `publicSafe` | boolean | 是 | 是否可在公开接口完整展示。为 `false` 时公开接口只展示脱敏摘要。 |
| `sortOrder` | integer | 是 | 0 到 999。 |

### ChangelogRelatedResource

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `resourceId` | string | 是 | resource 服务资源 ID。 |
| `slug` | string | 是 | resource slug 快照。 |
| `versionName` | string 或 null | 是 | 资源版本名快照。 |
| `visibility` | string | 是 | 资源可见范围快照。 |
| `downloadAvailable` | boolean | 是 | 下载入口是否可用摘要。 |
| `resourceSnapshotStale` | boolean | 是 | 是否使用旧快照。 |
| `failure` | object 或 null | 是 | 脱敏失败摘要。 |

### ChangelogCalendarRef

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `eventId` | string 或 null | 是 | calendar 事件 ID。 |
| `title` | string 或 null | 是 | 日程标题快照。 |
| `startAt` | string 或 null | 是 | 日程开始时间。 |
| `syncStatus` | string | 是 | `ChangelogSyncStatus`。 |
| `lastSyncedAt` | string 或 null | 是 | 最近同步时间。 |
| `failure` | object 或 null | 是 | 脱敏失败摘要。 |

### ChangelogNotificationSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | `ChangelogNotificationStatus`。 |
| `targetAudience` | string | 是 | `PUBLIC`、`MEMBERS` 或 `STAFF`。 |
| `lastAttemptAt` | string 或 null | 是 | 最近投递尝试时间。 |
| `failure` | object 或 null | 是 | 脱敏失败摘要，不含通知正文、token、请求头、内部 URL 和异常堆栈。 |

### ChangelogBookmark

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bookmarkId` | string | 是 | 收藏记录 ID。 |
| `releaseId` | string | 是 | 发布记录 ID。 |
| `userId` | string | 是 | auth 用户 ID。 |
| `displayNameSnapshot` | string | 是 | 用户展示名快照。 |
| `status` | string | 是 | `ChangelogBookmarkStatus`。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `canceledAt` | string 或 null | 是 | 取消收藏时间。 |

### ChangelogAuditLog

审计字段继承公共契约，允许补充 `releaseId`、`slug`、`versionName`、`stateFrom`、`stateTo`、`idempotencyKey`、`dependencyStatus`、`calendarSyncStatus`、`notificationStatus`、`resourceSnapshotStale` 和 `securityRedactionApplied`。审计日志不得通过 changelog API 删除。

## changelog 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `49100` | 502 | auth 认证上下文不可用。 |
| `49101` | 504 | auth 认证上下文调用超时。 |
| `49102` | 502 | auth 返回字段不兼容 changelog 契约。 |
| `49110` | 502 | resource 快照不可用。 |
| `49111` | 504 | resource 快照调用超时。 |
| `49112` | 502 | resource 返回字段不兼容 changelog 契约。 |
| `49120` | 502 | server-status 实例摘要不可用。 |
| `49121` | 504 | server-status 实例摘要调用超时。 |
| `49122` | 502 | server-status 返回字段不兼容 changelog 契约。 |
| `49130` | 502 | content 公开说明页快照不可用。 |
| `49131` | 504 | content 公开说明页快照调用超时。 |
| `49132` | 502 | content 返回字段不兼容 changelog 契约。 |
| `49140` | 502 | calendar 同步不可用。 |
| `49141` | 504 | calendar 同步超时。 |
| `49142` | 502 | calendar 返回字段不兼容 changelog 契约。 |
| `49150` | 502 | notification 投递不可用。 |
| `49151` | 504 | notification 投递超时。 |
| `49152` | 502 | notification 返回字段不兼容 changelog 契约。 |
| `49300` | 404 | 发布记录不存在或不可见。 |
| `49301` | 404 | 收藏记录不存在或不可见。 |
| `49302` | 404 | 关联快照不存在。 |
| `49310` | 409 | 发布记录状态不允许当前操作。 |
| `49311` | 409 | 发布 slug 或版本名冲突。 |
| `49312` | 409 | 幂等键请求指纹冲突。 |
| `49313` | 409 | 同一用户已收藏该发布记录。 |
| `49314` | 409 | 安全修复公开摘要不满足脱敏要求。 |
| `49315` | 409 | 变更分组或变更项不满足发布要求。 |
| `49316` | 409 | 生效时间或发布时间不允许当前操作。 |
| `54900` | 500 | changelog 内部错误。 |
| `54901` | 500 | changelog 审计写入失败。 |
| `54902` | 500 | changelog 状态写入失败。 |
| `54903` | 500 | changelog 收藏写入失败。 |
| `54904` | 500 | changelog 关联快照写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。changelog 自有幂等指纹冲突使用 `49312`。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开发布列表 | GET | `/api/v1/changelog/releases` | 否 | 无 | LOW |
| 公开发布详情 | GET | `/api/v1/changelog/releases/{releaseIdOrSlug}` | 否 | 无 | LOW |
| 最新版本 | GET | `/api/v1/changelog/versions/latest` | 否 | 无 | LOW |
| 标签和筛选项 | GET | `/api/v1/changelog/tags` | 否 | 无 | LOW |
| 公开变更项搜索 | GET | `/api/v1/changelog/changes` | 否 | 无 | LOW |
| 我的收藏列表 | GET | `/api/v1/changelog/me/bookmarks` | 是 | 当前用户 | LOW |
| 收藏发布记录 | POST | `/api/v1/changelog/me/releases/{releaseId}/bookmark` | 是 | 当前用户 | LOW |
| 取消收藏发布记录 | POST | `/api/v1/changelog/me/releases/{releaseId}/unbookmark` | 是 | 当前用户 | LOW |
| 后台发布列表 | GET | `/api/v1/changelog/admin/releases` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 后台发布详情 | GET | `/api/v1/changelog/admin/releases/{releaseId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 创建发布草稿 | POST | `/api/v1/changelog/admin/releases` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 修改发布记录 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 提交审核 | POST | `/api/v1/changelog/admin/releases/{releaseId}/submit` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核通过 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/approve` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/reject` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 要求修改 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/request-changes` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 发布 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/publish` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 下架 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/offline` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 归档 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/archive` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 软删除 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/delete` | 是 | `ADMIN`、`OWNER` | HIGH |
| 日历同步摘要 | POST | `/api/v1/changelog/admin/releases/{releaseId}/calendar-sync` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 审计列表 | GET | `/api/v1/changelog/admin/audit-logs` | 是 | `ADMIN`、`OWNER` | LOW |
| changelog 自检摘要 | GET | `/api/v1/changelog/admin/ops/summary` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |

## 公开接口

### 公开发布列表

`GET /api/v1/changelog/releases`

查询参数：`page`、`pageSize`、`keyword`、`type`、`visibility`、`impactLevel`、`minecraftVersion`、`tag`、`from`、`to` 和 `sort`。`pageSize` 最大 `100`。`sort` 允许 `releasedAt_desc`、`releasedAt_asc`、`effectiveAt_desc`、`updatedAt_desc`、`impactLevel_desc`。

成功响应 HTTP `200`，分页 `items` 为公开视图 `ChangelogRelease[]`。游客只看到 `PUBLISHED` 且 `visibility=PUBLIC` 的发布记录。列表不得返回内部备注、后台审核字段、通知失败详情、审计字段、完整依赖错误和未脱敏安全细节。

时间范围规则：`from` 和 `to` 使用 ISO 8601，按 `releasedAt` 重叠查询。未设置 `releasedAt` 的非发布记录不得出现在公开列表。

### 公开发布详情

`GET /api/v1/changelog/releases/{releaseIdOrSlug}`

成功响应 HTTP `200`，`data` 为公开视图 `ChangelogRelease`。发布记录不存在、不可见、已下架、已归档或已删除时返回 `49300`。`SECURITY` 类型只返回 `securityPublicSummary` 和 `publicSafe=true` 的变更项；`publicSafe=false` 的项只返回脱敏摘要。

### 最新版本

`GET /api/v1/changelog/versions/latest`

查询参数：`type`、`minecraftVersion` 和 `visibility`。成功响应 HTTP `200`，`data` 为最近一条符合条件的公开发布记录摘要；没有记录时返回 `data=null`。排序按 `releasedAt_desc`、`createdAt_desc` 稳定排序。

### 标签和筛选项

`GET /api/v1/changelog/tags`

成功响应 HTTP `200`，`data` 包含 `types`、`groupTypes`、`impactLevels`、`minecraftVersions`、`components` 和 `tags`。只统计公开可见发布记录，不暴露后台草稿、内部组件名、服务器内部路径或安全修复敏感组件。

### 公开变更项搜索

`GET /api/v1/changelog/changes`

查询参数：`page`、`pageSize`、`keyword`、`groupType`、`severity`、`component`、`releaseType`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为公开变更项摘要，包含发布记录摘要、分组和变更项。只返回公开可见发布记录中 `publicSafe=true` 或已脱敏的变更项。

## 当前用户接口

### 我的收藏列表

`GET /api/v1/changelog/me/bookmarks`

查询参数：`page`、`pageSize`、`status`、`type`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为当前用户收藏记录与发布摘要。只能返回当前认证用户自己的收藏，不得通过请求参数传入 `userId`。非法 `status` 返回 `40001`，非法 `sort` 返回 `40003`，非法时间返回 `40001`，非法范围返回 `49316`。

### 收藏发布记录

`POST /api/v1/changelog/me/releases/{releaseId}/bookmark`

请求字段：`idempotencyKey` 可选，8 到 80 位。成功响应 HTTP `201` 或重复收藏幂等 HTTP `200`，`data` 为 `ChangelogBookmark` 和发布摘要。只允许收藏公开可见、未下架、未归档、未删除的发布记录。并发收藏同一用户同一发布记录只能产生一条有效记录。相同幂等键同请求体重复提交返回同一结果，相同键不同体返回 `49312`。

### 取消收藏发布记录

`POST /api/v1/changelog/me/releases/{releaseId}/unbookmark`

请求字段：`reason` 可选，最多 200 位；`idempotencyKey` 可选。成功响应 HTTP `200`，`data` 为取消后的收藏摘要。未收藏时返回幂等成功，不能把 `bookmarkCount` 扣成负数。取消收藏不删除历史记录，只标记为 `CANCELED`。

## 后台接口

### 后台发布列表和详情

`GET /api/v1/changelog/admin/releases` 支持 `page`、`pageSize`、`keyword`、`type`、`status`、`visibility`、`impactLevel`、`createdBy`、`minecraftVersion`、`from`、`to` 和 `sort`。后台可查看全部非物理删除记录，默认按 `updatedAt_desc`。`GET /api/v1/changelog/admin/releases/{releaseId}` 返回发布记录、收藏统计、关联快照、通知摘要、日历同步摘要、依赖摘要和最近审计。响应不得返回 token、完整请求头、通知正文、前序服务内部路径、异常堆栈、真实服务器命令、节点凭据或 Cloudreve token。

### 创建发布草稿

`POST /api/v1/changelog/admin/releases`

请求字段包括 `slug`、`versionName`、`title`、`summary`、`body`、`type`、`visibility`、`impactLevel`、`releasedAt`、`effectiveAt`、`minecraftVersion`、`pluginVersions`、`resourcePackVersions`、`mapVersion`、`groups`、`compatibilityNotes`、`knownIssues`、`rollbackNotes`、`securityPublicSummary`、`relatedResourceIds`、`relatedServerInstanceIds`、`relatedContentId`、`internalNote`、`reason` 和 `idempotencyKey`。`reason` 必填，1 到 200 位。成功响应 HTTP `201`，状态为 `DRAFT`。slug 或版本名冲突返回 `49311`。`groups` 至少 1 组，每组至少 1 个 `items`，否则返回 `49315`。`SECURITY` 类型必须提供 `securityPublicSummary`，且公开摘要不得包含内部路径、token、节点地址、命令或 exploit 细节。

### 修改发布记录

`PATCH /api/v1/changelog/admin/releases/{releaseId}`

请求字段同创建发布草稿，除 `reason` 必填外其余字段按需修改。只允许 `DRAFT`、`NEEDS_CHANGES`、`REJECTED` 和未发布的 `APPROVED` 修改主体字段。`PUBLISHED` 记录如需改正文，P1 必须先下架后修改再发布，避免公开读到半更新状态。`HELPER` 只能修改自己创建且未发布的发布记录。

### 审核发布状态

`POST /api/v1/changelog/admin/releases/{releaseId}/submit` 使 `DRAFT`、`NEEDS_CHANGES` 或 `REJECTED` 进入 `PENDING_REVIEW`。

`PATCH /api/v1/changelog/admin/releases/{releaseId}/approve` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，使 `PENDING_REVIEW` 或 `NEEDS_CHANGES` 进入 `APPROVED`。

`PATCH /api/v1/changelog/admin/releases/{releaseId}/reject` 请求字段同审核通过，使 `PENDING_REVIEW` 进入 `REJECTED`。

`PATCH /api/v1/changelog/admin/releases/{releaseId}/request-changes` 请求字段同审核通过，使 `PENDING_REVIEW` 进入 `NEEDS_CHANGES`。

`PATCH /api/v1/changelog/admin/releases/{releaseId}/publish` 请求字段为 `releasedAt`、`effectiveAt`、`notificationAudience`、`reason`、`idempotencyKey`，使 `APPROVED` 或 `OFFLINE` 进入 `PUBLISHED`，写入 `publishedAt`。发布前必须重新校验分组、公开安全摘要、发布时间和可见范围。notification 或 calendar 失败不回滚主状态，但必须保存脱敏失败摘要和审计。

`PATCH /api/v1/changelog/admin/releases/{releaseId}/offline` 请求字段为 `publicReason`、`reason`、`idempotencyKey`，使 `PUBLISHED` 进入 `OFFLINE`。下架后公开接口不可见，但收藏记录保留。

`PATCH /api/v1/changelog/admin/releases/{releaseId}/archive` 请求字段为 `reason`、`idempotencyKey`，使 `DRAFT`、`REJECTED`、`NEEDS_CHANGES` 或 `OFFLINE` 进入 `ARCHIVED`。已发布记录必须先下架再归档。

`PATCH /api/v1/changelog/admin/releases/{releaseId}/delete` 是 `HIGH` 风险，请求字段为 `reason`、`confirmText`、`idempotencyKey`，P1 固定要求 `DELETE_CHANGELOG_RELEASE`。成功后状态为 `DELETED`，只做软删除，不物理删除发布记录、收藏记录和审计线索。

### 日历同步摘要

`POST /api/v1/changelog/admin/releases/{releaseId}/calendar-sync`

请求字段：`mode` 可选，取值 `DRY_RUN` 或 `UPSERT_SNAPSHOT`，默认 `DRY_RUN`；`reason` 必填；`idempotencyKey` 可选。P1 默认不真实写入 calendar 主数据。`DRY_RUN` 返回将要同步的日程摘要和 `syncStatus=SKIPPED`。测试控制头模拟可用时，`UPSERT_SNAPSHOT` 返回 `syncStatus=SYNCED` 并保存本服务 `relatedCalendarEvent` 摘要；模拟失败时返回 `49140` 或保存失败摘要，同一实现必须固定并测试。calendar 同步失败不得删除已有日程摘要，不得回滚发布记录状态。

## 审计和自检

`GET /api/v1/changelog/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`releaseId`、`result`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为 `ChangelogAuditLog[]`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 changelog API 删除。

`GET /api/v1/changelog/admin/ops/summary` 成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "changelog",
    "port": 8115,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "resourceMode": "TEST_STUB",
    "serverStatusMode": "TEST_STUB",
    "contentMode": "TEST_STUB",
    "calendarSyncMode": "SKIPPED",
    "notificationMode": "SKIPPED",
    "testControlsEnabled": false,
    "releasesTotal": 8,
    "publishedReleasesTotal": 3,
    "bookmarksTotal": 6,
    "auditsTotal": 24,
    "idempotencyRecordsTotal": 12,
    "lastPublishedAt": "2026-05-25T12:00:00Z",
    "lastAuditAt": "2026-05-25T12:05:00Z",
    "productionGaps": [
      "P1_IN_MEMORY_STORAGE",
      "P1_AUTH_STUB",
      "P1_RESOURCE_STUB",
      "P1_SERVER_STATUS_STUB",
      "P1_CONTENT_STUB",
      "CALENDAR_WRITE_NOT_CONNECTED",
      "NOTIFICATION_DELIVERY_NOT_CONNECTED",
      "TEST_CONTROLS_DISABLED_OUTSIDE_TEST"
    ]
  }
}
```

## 状态、幂等和并发

发布记录创建后为 `DRAFT`。`DRAFT` 可提交为 `PENDING_REVIEW`。`PENDING_REVIEW` 可通过为 `APPROVED`、拒绝为 `REJECTED`、要求修改为 `NEEDS_CHANGES`。`NEEDS_CHANGES` 和 `REJECTED` 可修改后再次提交。`APPROVED` 可发布为 `PUBLISHED`。`PUBLISHED` 可下架为 `OFFLINE`。`OFFLINE` 可重新发布、归档或软删除。`DRAFT`、`REJECTED`、`NEEDS_CHANGES` 和 `OFFLINE` 可归档或软删除。`ARCHIVED` 和 `DELETED` 为终态，不得回到公开状态。

写接口支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `49312`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序。

并发收藏同一用户同一发布记录只能产生一条有效收藏记录。重复取消收藏保持幂等。收藏计数必须和有效收藏记录一致，不得小于 0。并发审核、发布、下架、归档和软删除同一发布记录只能有一个成功状态推进。读取接口允许读到更新前或更新后的完整状态，不能返回半更新对象。

P1 内存实现必须用本服务内的串行临界区保护发布记录状态、收藏计数、关联快照、日历同步摘要和审计写入。后续持久化实现必须迁移为数据库事务、唯一约束、条件更新或等效机制，不能降低上述并发口径。

## 审计要求

必须审计的动作包括发布记录创建、修改、提交审核、审核通过、审核拒绝、要求修改、发布、下架、归档、软删除、收藏、取消收藏、日历同步、通知失败、依赖降级、自检读取、审计写入失败和状态写入失败。

后台写操作必须记录 `reason`、操作者、目标对象、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计不得泄露 token、完整请求头、通知正文、前序服务内部路径、真实服务器命令、节点凭据、Cloudreve token、内部异常堆栈、安全 exploit 细节或未脱敏运维参数。

审计写入失败时，发布记录创建、修改、审核、发布、下架、归档、软删除和日历同步不得假装成功，必须返回 `54901` 或 `54900`，并保持业务数据不变。普通用户收藏和取消收藏在 P1 也必须保证审计和收藏计数一致，失败返回 `54903` 或 `54901`，不得产生半收藏状态。通知失败不回滚主状态，但必须记录失败摘要和审计。

## 失败降级

auth 是所有登录接口强依赖。auth 不可用、超时、用户状态不允许或字段不兼容时不得伪造成功。

resource、server-status 和 content 是关联快照依赖。创建或修改关联时，快照不可用不得保存伪造关联。读取已存在发布记录时，来源服务失败可以使用已保存快照降级并标记 stale。

calendar 是辅助同步依赖。同步失败不得回滚发布主状态，不得删除已有日程摘要。P1 默认只保存同步摘要，不反向写 calendar 主数据。

notification 是辅助依赖。发布、下架和安全修复通知失败不得回滚主状态，但必须保存脱敏失败摘要和审计。

安全修复公开字段必须优先保护服务器、玩家和运维安全。公开接口不得泄露 exploit 细节、内部路径、拓扑、token、节点地址、服务命令或回滚脚本。

## 验收口径

`changelog` API 文档按 `docs/contracts-changelog.md` 独立存在，并由 `.local-docs/tests-changelog.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`changelog` 完成时必须满足以下条件：全部接口按本文档实现；公开接口只返回公开可见发布记录和脱敏变更项；当前用户只能维护自己的收藏；后台接口按角色限制；发布状态机不可非法回退；安全修复公开摘要不泄露敏感信息；资源、server-status、content、calendar 和 notification 都只走正式契约或受控适配层；calendar 同步失败不影响 changelog 主状态；notification 失败记录脱敏摘要；所有写操作有审计；端口固定为 `8115`；`.local-docs/tests-changelog.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 changelog 全部测试通过；auth、profile、notification、content、server-status、resource、admin、onboarding、exam、whitelist、attendance、community、activity 和 calendar 前序服务回归测试通过；没有修改前序服务稳定接口；没有把官网公告、资源下载、日历主数据、活动报名、后台聚合、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复或 Cloudreve 管理能力塞进 changelog。

生产化硬化验收还必须满足：测试控制头默认关闭，只有本地自动化测试显式启用时才生效；关闭状态下依赖失败模拟头、写入失败模拟头、时间模拟头和通知失败模拟头全部被忽略；自检摘要明确返回当前测试控制头开关状态。
