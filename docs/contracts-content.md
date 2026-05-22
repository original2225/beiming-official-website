# 北冥官网 content API 契约

版本：0.2

## 文档定位

本文档是 `content` 微服务的正式 API 契约。后续 `server-status`、`resource`、`admin`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar` 和 `changelog` 只能通过本文档定义的接口适配官网内容，不能直接读取或修改 `content` 数据库。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、能力点、审计字段、风险等级、通用状态模型和通用错误码均以公共契约为准。本文档只补充 `content` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

`content` 适配 `auth`、`profile` 和 `notification`，不要求前序服务反向适配 `content`。`content` 通过后端入口传入的认证上下文、`/api/v1/auth/me`、`/api/v1/auth/session/verify` 或测试环境 auth stub 读取当前用户、角色和能力点。成员作品只保存来自 `profile` 的公开快照。审核结果需要通知时，只能调用 `notification` 的正式投递接口或受控适配层，不能自建通知主数据、未读数或模板系统。

## 职责边界

`content` 负责官网首页配置、公告、文章、页面内容、摄影作品、成员作品快照、服务器进度、成就、里程碑、专题页、内容分类、标签和 SEO 配置。

`content` 不负责注册、登录、会话、角色、能力点、邀请码、成员主数据、成员组主数据、站内通知主数据、玩家可见 Minecraft 实时状态、资源下载、Cloudreve 分享、社区帖子、活动报名、日历事件、后台聚合入口或真实服务器运维操作。

首页可以展示服务器状态入口和资源入口，但只保存展示位、标题、说明和跳转配置。真实在线人数、MOTD、线路状态、资源下载链接和 Cloudreve 分享链接分别归后续 `server-status` 和 `resource`。

## 数据归属

`content` 拥有以下主数据：首页草稿配置、首页已发布配置、内容条目、分类、标签、专题、SEO 配置、成员作品快照、幂等记录、内容审计日志和内容发布记录。

成员作品快照字段只用于展示，至少包括 `memberId`、`displayName`、`avatarUrl`、`minecraftId`、`groupName` 和 `profileSnapshotAt`。快照不是 `profile` 主数据，不能用于成员资格判断，不能反写 `profile`。

通知调用结果只能作为审计摘要或业务操作结果摘要保存。`content` 不保存收件人读取状态、不计算未读数、不维护通知模板。

## 基础路径与认证

公开接口使用 `/api/v1/content` 前缀，不要求登录，只能返回公开可见、已发布、未下架、未归档、未软删除且处于可见时间范围内的数据。公开接口不得返回后台备注、审核意见、审计字段、作者用户敏感字段、幂等键、通知结果、内部快照来源和未发布配置。

后台接口使用 `/api/v1/content/admin` 前缀，全部要求登录。后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`。后台写操作要求 `ADMIN` 或 `OWNER`，必须携带 `reason` 并写入审计。`HELPER` 可以读取待审核内容和预览，但不能创建、修改、审核、发布、下架、归档或删除。

## auth 兼容契约

当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。

后台写操作的 `createdBy`、`updatedBy`、`reviewedBy`、`publishedBy`、`offlineBy`、`archivedBy` 和 `deletedBy` 均来自服务端认证上下文，浏览器请求体传入同名字段时必须忽略或返回字段校验失败。

auth 上下文不可用返回 `46420`，auth 调用超时返回 `46421`，auth 返回字段缺失或枚举不兼容返回 `46422`。`content` 不能导入 auth 的内存存储、实体、Repository 或测试种子实现。

## profile 兼容契约

创建或修改 `MEMBER_WORK` 类型内容时，如果请求包含 `memberId`，`content` 必须通过 profile 适配层读取成员公开快照。profile 返回成员不存在或不可公开时返回 `46400`。profile 调用超时返回 `46401`。profile 返回字段缺失或枚举不兼容返回 `46402`。

客户端不得通过请求体伪造成员展示名、头像、Minecraft ID 或成员组作为可信字段。实现可以允许请求体携带这些字段作为编辑草稿参考，但保存的可信快照必须来自 profile 适配层。profile 不可用时不得创建新的可信成员作品；已发布内容公开读取可以继续返回已保存快照，并在后台详情中标记快照来源时间。

## notification 兼容契约

审核通过、拒绝、要求修改、发布、下架、归档和软删除都可以触发通知。本文档规定审核通过、拒绝、要求修改必须通知内容作者；作者为空时跳过通知并在审计中记录 `NO_AUTHOR_TO_NOTIFY`。发布、下架、归档和软删除的通知为辅助提醒，通知失败时主流程可以成功，但必须在审计中记录 `notificationStatus: FAILED` 和失败原因。

强制通知失败返回 `46410` 或 `46411`，业务状态不得变化。辅助通知失败不得伪造通知成功，也不得影响公开读取。`content` 不能自建通知表、未读数、模板和投递记录。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `ContentType` | `ANNOUNCEMENT`、`ARTICLE`、`PAGE`、`PHOTO`、`MEMBER_WORK`、`PROGRESS`、`ACHIEVEMENT`、`MILESTONE`、`TOPIC_ENTRY` | 内容条目类型。专题主体由 `TopicPage` 管理，专题内条目可使用 `TOPIC_ENTRY`。 |
| `ContentStatus` | `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` | 继承公共状态模型。公开可见需要 `APPROVED` 且 `publishedAt` 不为空。 |
| `ContentVisibility` | `PUBLIC`、`MEMBER_ONLY`、`PRIVATE` | P0 公开接口只返回 `PUBLIC`。`MEMBER_ONLY` 保留给后续登录用户展示。 |
| `HomeSectionType` | `HERO`、`ANNOUNCEMENTS`、`FEATURED_ARTICLES`、`MEMBER_WORKS`、`MOMENTS`、`MILESTONES`、`TOPICS`、`SERVER_ENTRY`、`RESOURCE_ENTRY`、`CUSTOM_LINKS` | 首页区块类型。 |
| `ContentAuditResult` | `SUCCESS`、`FAILED` | content 审计执行结果。 |
| `SeoRobots` | `INDEX_FOLLOW`、`NOINDEX_FOLLOW`、`NOINDEX_NOFOLLOW` | SEO robots 策略。 |

## 通用对象

### ContentItemSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `contentId` | string | 是 | 内容 ID。 |
| `type` | string | 是 | `ContentType`。 |
| `slug` | string | 是 | 公开路径标识，同一未软删除内容中唯一。 |
| `title` | string | 是 | 标题，2 到 120 位。 |
| `summary` | string 或 null | 是 | 摘要，最多 300 位。 |
| `coverUrl` | string 或 null | 是 | 封面 URL，只允许 http、https 或站内资源路径。 |
| `category` | ContentCategory 或 null | 是 | 分类公开摘要。 |
| `tags` | ContentTag[] | 是 | 标签公开摘要。 |
| `visibility` | string | 是 | 公开接口只返回 `PUBLIC`。 |
| `authorDisplayName` | string 或 null | 是 | 作者展示名快照。 |
| `memberSnapshot` | MemberContentSnapshot 或 null | 是 | 成员作品快照。 |
| `publishedAt` | string | 是 | 发布时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ContentItemDetail

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `contentId` | string | 是 | 内容 ID。 |
| `type` | string | 是 | 内容类型。 |
| `slug` | string | 是 | 公开路径标识。 |
| `title` | string | 是 | 标题。 |
| `summary` | string 或 null | 是 | 摘要。 |
| `body` | string | 是 | 正文，公开接口只返回已发布正文。 |
| `coverUrl` | string 或 null | 是 | 封面 URL。 |
| `category` | ContentCategory 或 null | 是 | 分类摘要。 |
| `tags` | ContentTag[] | 是 | 标签摘要。 |
| `visibility` | string | 是 | 公开接口只返回 `PUBLIC`。 |
| `authorDisplayName` | string 或 null | 是 | 作者展示名快照。 |
| `memberSnapshot` | MemberContentSnapshot 或 null | 是 | 成员作品快照。 |
| `seo` | SeoPayload 或 null | 是 | 内容级 SEO 摘要。 |
| `publishedAt` | string | 是 | 发布时间。 |
| `visibleFrom` | string 或 null | 是 | 可见开始时间。 |
| `visibleUntil` | string 或 null | 是 | 可见结束时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### AdminContentItem

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `contentId` | string | 是 | 内容 ID。 |
| `type` | string | 是 | 内容类型。 |
| `status` | string | 是 | 内容状态。 |
| `visibility` | string | 是 | 可见性。 |
| `slug` | string | 是 | slug。 |
| `title` | string | 是 | 标题。 |
| `summary` | string 或 null | 是 | 摘要。 |
| `body` | string | 是 | 正文草稿或当前内容。 |
| `coverUrl` | string 或 null | 是 | 封面 URL。 |
| `categoryId` | string 或 null | 是 | 分类 ID。 |
| `tagIds` | string[] | 是 | 标签 ID 列表。 |
| `authorUserId` | string 或 null | 是 | 作者 auth 用户 ID。 |
| `authorDisplayNameSnapshot` | string 或 null | 是 | 作者展示名快照。 |
| `memberSnapshot` | MemberContentSnapshot 或 null | 是 | 成员作品快照。 |
| `seo` | SeoPayload 或 null | 是 | 内容级 SEO。 |
| `adminNote` | string 或 null | 是 | 后台备注，不得出现在公开接口。 |
| `reviewOpinion` | string 或 null | 是 | 最近审核意见，不得出现在公开接口。 |
| `notificationStatus` | string 或 null | 是 | 最近一次通知结果摘要。 |
| `submittedAt` | string 或 null | 是 | 提交审核时间。 |
| `reviewedAt` | string 或 null | 是 | 审核时间。 |
| `publishedAt` | string 或 null | 是 | 发布时间。 |
| `visibleFrom` | string 或 null | 是 | 可见开始时间。 |
| `visibleUntil` | string 或 null | 是 | 可见结束时间。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `deletedAt` | string 或 null | 是 | 软删除时间。 |

### ContentItemVersion

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `contentId` | string | 是 | 内容 ID。 |
| `version` | integer | 是 | 内容版本号，从 1 开始递增。 |
| `sourceAction` | string | 是 | 产生版本的动作，如 `CREATED`、`UPDATED`、`PUBLISHED`、`RESTORED`。 |
| `snapshot` | AdminContentItem | 是 | 该版本的后台内容快照。 |
| `createdBy` | string | 是 | 创建该版本的用户 ID。 |
| `createdAt` | string | 是 | 版本创建时间。 |
| `reason` | string 或 null | 是 | 创建该版本的操作原因。 |
| `restoredFromVersion` | integer 或 null | 是 | 如果该版本来自恢复操作，记录来源版本。 |

### ContentCategory

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `categoryId` | string | 是 | 分类 ID。 |
| `name` | string | 是 | 分类名称，2 到 40 位，同一未归档分类中唯一。 |
| `slug` | string | 是 | 分类 slug。 |
| `description` | string 或 null | 是 | 分类说明，最多 200 位。 |
| `sortOrder` | integer | 是 | 排序值，数字越小越靠前。 |
| `archived` | boolean | 是 | 是否归档。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ContentTag

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `tagId` | string | 是 | 标签 ID。 |
| `name` | string | 是 | 标签名称，1 到 24 位，同一未归档标签中唯一。 |
| `slug` | string | 是 | 标签 slug。 |
| `archived` | boolean | 是 | 是否归档。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### MemberContentSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `memberId` | string | 是 | profile 成员 ID。 |
| `displayName` | string | 是 | 成员展示名快照。 |
| `avatarUrl` | string 或 null | 是 | 头像快照。 |
| `minecraftId` | string 或 null | 是 | Minecraft ID 快照。 |
| `groupName` | string 或 null | 是 | 成员组名称快照。 |
| `profileSnapshotAt` | string | 是 | 快照获取时间。 |

### HomeContentView

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `homeConfigId` | string | 是 | 已发布首页配置 ID。 |
| `version` | integer | 是 | 已发布版本号。 |
| `sections` | HomeSection[] | 是 | 首页区块。 |
| `degraded` | boolean | 是 | 是否存在局部降级。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |
| `publishedAt` | string | 是 | 首页配置发布时间。 |
| `seo` | SeoPayload 或 null | 是 | 首页 SEO。 |

### HomeSection

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sectionId` | string | 是 | 区块 ID。 |
| `type` | string | 是 | `HomeSectionType`。 |
| `title` | string 或 null | 是 | 区块标题。 |
| `subtitle` | string 或 null | 是 | 区块副标题。 |
| `enabled` | boolean | 是 | 是否启用。 |
| `sortOrder` | integer | 是 | 排序值。 |
| `items` | array | 是 | 区块内容，引用不存在、下架或不可公开时局部跳过。 |
| `degraded` | boolean | 是 | 区块是否降级。 |
| `degradeReason` | string 或 null | 是 | 降级原因。 |

### TopicPage

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `topicId` | string | 是 | 专题 ID。 |
| `slug` | string | 是 | 专题 slug，同一未软删除专题中唯一。 |
| `title` | string | 是 | 专题标题，2 到 120 位。 |
| `summary` | string 或 null | 是 | 专题摘要。 |
| `coverUrl` | string 或 null | 是 | 封面 URL。 |
| `status` | string | 是 | 专题状态。 |
| `visibility` | string | 是 | 可见性。 |
| `contentIds` | string[] | 是 | 专题引用内容 ID。 |
| `items` | ContentItemSummary[] | 是 | 公开专题中可展示的内容摘要。 |
| `seo` | SeoPayload 或 null | 是 | 专题 SEO。 |
| `publishedAt` | string 或 null | 是 | 发布时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### SeoPayload

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `seoId` | string 或 null | 是 | SEO 配置 ID。默认 SEO 可为 `null`。 |
| `route` | string | 是 | 站内路由，以 `/` 开头。 |
| `title` | string | 是 | SEO 标题，2 到 80 位。 |
| `description` | string | 是 | SEO 描述，1 到 200 位。 |
| `keywords` | string[] | 是 | 关键词，最多 20 个。 |
| `coverUrl` | string 或 null | 是 | 分享封面。 |
| `robots` | string | 是 | `SeoRobots`。 |
| `canonicalUrl` | string 或 null | 是 | canonical URL。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ContentAuditLog

审计字段继承公共契约，允许补充 `contentId`、`homeConfigId`、`topicId`、`seoId`、`idempotencyKey`、`notificationStatus`、`profileSnapshotStatus`、`stateFrom`、`stateTo`、`version`、`sourceVersion` 和 `newVersion`。审计日志不得通过 content API 删除。

## content 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `43400` | 404 | 内容不存在，或公开接口无权访问该内容。 |
| `43401` | 404 | 分类不存在。 |
| `43402` | 404 | 专题不存在，或公开接口无权访问该专题。 |
| `43403` | 404 | SEO 配置不存在。 |
| `43404` | 404 | 首页配置不存在。 |
| `43405` | 404 | 标签不存在。 |
| `43410` | 409 | 内容状态不允许当前操作。 |
| `43411` | 409 | slug 冲突。 |
| `43412` | 409 | 内容不可公开访问。 |
| `43413` | 409 | 首页配置状态不允许当前操作。 |
| `43414` | 409 | 专题状态不允许当前操作。 |
| `43415` | 409 | 分类或标签仍被内容引用，不能归档。 |
| `43416` | 409 | SEO 路由冲突。 |
| `43417` | 404 | 内容版本不存在。 |
| `43418` | 409 | 内容版本状态不允许当前操作。 |
| `46400` | 502 | profile 成员快照不可用。 |
| `46401` | 504 | profile 成员快照调用超时。 |
| `46402` | 502 | profile 成员快照不兼容 content 契约。 |
| `46410` | 502 | notification 强制投递不可用。 |
| `46411` | 504 | notification 强制投递超时。 |
| `46420` | 502 | auth 认证上下文不可用。 |
| `46421` | 504 | auth 认证上下文调用超时。 |
| `46422` | 502 | auth 认证上下文不兼容 content 契约。 |
| `51400` | 500 | content 内部错误。 |
| `51401` | 500 | content 审计写入失败。 |
| `51402` | 500 | content 首页配置发布失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误、幂等键冲突和通用服务端错误优先使用公共错误码。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开首页内容 | GET | `/api/v1/content/home` | 否 | 无 | LOW |
| 公开内容列表 | GET | `/api/v1/content/items` | 否 | 无 | LOW |
| 公开内容详情 | GET | `/api/v1/content/items/{contentId}` | 否 | 无 | LOW |
| 公开 slug 内容详情 | GET | `/api/v1/content/items/by-slug/{slug}` | 否 | 无 | LOW |
| 公开分类列表 | GET | `/api/v1/content/categories` | 否 | 无 | LOW |
| 公开标签列表 | GET | `/api/v1/content/tags` | 否 | 无 | LOW |
| 公开专题列表 | GET | `/api/v1/content/topics` | 否 | 无 | LOW |
| 公开专题详情 | GET | `/api/v1/content/topics/{topicId}` | 否 | 无 | LOW |
| 公开 slug 专题详情 | GET | `/api/v1/content/topics/by-slug/{slug}` | 否 | 无 | LOW |
| 公开 SEO 配置 | GET | `/api/v1/content/seo` | 否 | 无 | LOW |
| 后台内容列表 | GET | `/api/v1/content/admin/items` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台内容详情 | GET | `/api/v1/content/admin/items/{contentId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建内容 | POST | `/api/v1/content/admin/items` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改内容 | PATCH | `/api/v1/content/admin/items/{contentId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 提交审核 | PATCH | `/api/v1/content/admin/items/{contentId}/submit-review` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核通过 | PATCH | `/api/v1/content/admin/items/{contentId}/approve` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/content/admin/items/{contentId}/reject` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 要求修改 | PATCH | `/api/v1/content/admin/items/{contentId}/request-changes` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 发布内容 | PATCH | `/api/v1/content/admin/items/{contentId}/publish` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 下架内容 | PATCH | `/api/v1/content/admin/items/{contentId}/offline` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档内容 | PATCH | `/api/v1/content/admin/items/{contentId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 软删除内容 | PATCH | `/api/v1/content/admin/items/{contentId}/delete` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 内容版本列表 | GET | `/api/v1/content/admin/items/{contentId}/versions` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 内容版本详情 | GET | `/api/v1/content/admin/items/{contentId}/versions/{version}` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 恢复内容版本 | PATCH | `/api/v1/content/admin/items/{contentId}/versions/{version}/restore` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 内容审计列表 | GET | `/api/v1/content/admin/items/{contentId}/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 后台首页配置详情 | GET | `/api/v1/content/admin/home` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 保存首页草稿配置 | PUT | `/api/v1/content/admin/home` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 首页配置预览 | POST | `/api/v1/content/admin/home/preview` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 发布首页配置 | PATCH | `/api/v1/content/admin/home/publish` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 回滚首页配置 | PATCH | `/api/v1/content/admin/home/rollback` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台分类列表 | GET | `/api/v1/content/admin/categories` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建分类 | POST | `/api/v1/content/admin/categories` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改分类 | PATCH | `/api/v1/content/admin/categories/{categoryId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档分类 | PATCH | `/api/v1/content/admin/categories/{categoryId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台标签列表 | GET | `/api/v1/content/admin/tags` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建标签 | POST | `/api/v1/content/admin/tags` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改标签 | PATCH | `/api/v1/content/admin/tags/{tagId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档标签 | PATCH | `/api/v1/content/admin/tags/{tagId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台专题列表 | GET | `/api/v1/content/admin/topics` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台专题详情 | GET | `/api/v1/content/admin/topics/{topicId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建专题 | POST | `/api/v1/content/admin/topics` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改专题 | PATCH | `/api/v1/content/admin/topics/{topicId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 发布专题 | PATCH | `/api/v1/content/admin/topics/{topicId}/publish` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 下架专题 | PATCH | `/api/v1/content/admin/topics/{topicId}/offline` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档专题 | PATCH | `/api/v1/content/admin/topics/{topicId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 软删除专题 | PATCH | `/api/v1/content/admin/topics/{topicId}/delete` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台 SEO 列表 | GET | `/api/v1/content/admin/seo` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台 SEO 详情 | GET | `/api/v1/content/admin/seo/{seoId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 保存路由 SEO | PUT | `/api/v1/content/admin/seo/by-route` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 禁用路由 SEO | PATCH | `/api/v1/content/admin/seo/{seoId}/disable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| content 自检摘要 | GET | `/api/v1/content/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 公开接口

### 公开首页内容

`GET /api/v1/content/home`

成功响应 HTTP `200`，`data` 为 `HomeContentView`。

业务规则：只返回最近一次已发布首页配置。配置不存在时返回默认空首页视图，`sections` 为空数组，`degraded` 为 `true`，`degradeReasons` 包含 `NO_PUBLISHED_HOME_CONFIG`。首页引用的内容、专题、分类或标签不存在、未发布、不可公开、已下架、已归档、已软删除或不在可见时间范围内时，只跳过对应引用并标记对应区块降级。`SERVER_ENTRY` 和 `RESOURCE_ENTRY` 只能返回入口配置和说明，不能返回伪造服务器在线状态或资源下载链接。

降级规则：公开首页不得因为单个引用失效整页失败。存储读取失败或首页配置无法解析时返回 `51400` 或 `51402`，前端按首页区域降级。

### 公开内容列表

`GET /api/v1/content/items`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `type` | string | 否 | 任一 `ContentType`。 |
| `categoryId` | string | 否 | 分类 ID。 |
| `tag` | string | 否 | 标签 slug 或名称，最多 40 位。 |
| `keyword` | string | 否 | 匹配标题、摘要或正文，最多 80 位。 |
| `sort` | string | 否 | 允许 `publishedAt_desc`、`publishedAt_asc`、`updatedAt_desc`、`title_asc`。默认 `publishedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `ContentItemSummary[]`。

业务规则：只返回 `status=APPROVED`、`publishedAt` 不为空、`visibility=PUBLIC`、未归档、未软删除、未下架且在可见时间范围内的内容。草稿、待审核、已拒绝、需修改、已下架、已归档、已软删除和 `PRIVATE`、`MEMBER_ONLY` 内容不得出现在公开列表。公开列表不得返回 `body`、`adminNote`、`reviewOpinion`、审计字段、通知结果和幂等字段。

### 公开内容详情

`GET /api/v1/content/items/{contentId}`

成功响应 HTTP `200`，`data` 为 `ContentItemDetail`。

业务规则：只有公开可见内容可以访问。内容不存在返回 `43400`。内容存在但不可公开访问时返回 `43412`，实现也可以出于防枚举考虑返回 `43400`，但同一版本内必须保持一致并写入测试。

### 公开 slug 内容详情

`GET /api/v1/content/items/by-slug/{slug}`

成功响应 HTTP `200`，`data` 为 `ContentItemDetail`。

业务规则：slug 必须匹配公开可见内容。slug 不存在返回 `43400`。slug 对应内容不可公开时返回 `43412` 或 `43400`，同一版本内保持一致。

### 公开分类列表

`GET /api/v1/content/categories`

成功响应 HTTP `200`，`data.items` 为未归档 `ContentCategory[]`，按 `sortOrder`、`name` 稳定排序。不得返回后台备注或审计字段。

### 公开标签列表

`GET /api/v1/content/tags`

成功响应 HTTP `200`，`data.items` 为未归档 `ContentTag[]`，按 `name` 稳定排序。不得返回后台备注或审计字段。

### 公开专题列表

`GET /api/v1/content/topics`

查询参数同公开内容列表的分页参数，额外支持 `keyword` 和 `sort`，允许排序为 `publishedAt_desc`、`updatedAt_desc`、`title_asc`。

成功响应 HTTP `200`，分页 `items` 为 `TopicPage[]` 的公开摘要字段。

业务规则：只返回 `status=APPROVED`、`publishedAt` 不为空、`visibility=PUBLIC`、未归档、未软删除且在可见时间范围内的专题。专题中的失效内容引用不应导致专题列表失败。

### 公开专题详情

`GET /api/v1/content/topics/{topicId}`

成功响应 HTTP `200`，`data` 为 `TopicPage`。

业务规则：专题不存在返回 `43402`。专题不可公开访问返回 `43414` 或 `43402`，同一版本内保持一致。专题内引用内容不存在、下架或不可公开时从 `items` 局部跳过，并在后台详情中保留引用问题；公开响应不泄露后台原因。

### 公开 slug 专题详情

`GET /api/v1/content/topics/by-slug/{slug}`

成功响应 HTTP `200`，`data` 为 `TopicPage`。业务规则同公开专题详情。

### 公开 SEO 配置

`GET /api/v1/content/seo`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `route` | string | 是 | 站内路由，以 `/` 开头，最多 200 位。 |

成功响应 HTTP `200`，`data` 为 `SeoPayload`。

业务规则：命中启用 SEO 配置时返回该配置。未配置时返回模块默认 SEO，`seoId` 为 `null`，不得返回后台草稿、禁用配置或审核信息。route 非法返回 `40001`。

## 后台内容接口

### 后台内容列表

`GET /api/v1/content/admin/items`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `type` | string | 否 | 任一 `ContentType`。 |
| `status` | string | 否 | 任一 `ContentStatus`。 |
| `visibility` | string | 否 | 任一 `ContentVisibility`。 |
| `categoryId` | string | 否 | 分类 ID。 |
| `tagId` | string | 否 | 标签 ID。 |
| `keyword` | string | 否 | 匹配标题、摘要、正文、slug 或后台备注，最多 80 位。 |
| `createdBy` | string | 否 | 创建者用户 ID。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`updatedAt_desc`、`publishedAt_desc`、`title_asc`。 |

成功响应 HTTP `200`，分页 `items` 为 `AdminContentItem[]`。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。未登录返回 `41000`。

### 后台内容详情

`GET /api/v1/content/admin/items/{contentId}`

成功响应 HTTP `200`，`data` 为 `AdminContentItem`。内容不存在返回 `43400`。

### 创建内容

`POST /api/v1/content/admin/items`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `type` | string | 是 | 任一 `ContentType`。 |
| `slug` | string | 是 | 3 到 120 位，只允许小写字母、数字、短横线和斜杠，不能以斜杠结尾。 |
| `title` | string | 是 | 2 到 120 位。 |
| `summary` | string 或 null | 否 | 最多 300 位。 |
| `body` | string | 是 | 1 到 50000 位，脚本内容按普通文本保存，不执行。 |
| `coverUrl` | string 或 null | 否 | http、https 或站内资源路径，最多 500 位。 |
| `categoryId` | string 或 null | 否 | 分类 ID。 |
| `tagIds` | string[] | 否 | 最多 20 个标签 ID。 |
| `visibility` | string | 否 | 默认 `PUBLIC`。 |
| `authorUserId` | string 或 null | 否 | 作者 auth 用户 ID，未传时使用当前操作者。 |
| `memberId` | string 或 null | 否 | `MEMBER_WORK` 类型可传，用于获取 profile 快照。 |
| `seo` | object 或 null | 否 | 内容级 SEO。 |
| `adminNote` | string 或 null | 否 | 后台备注，最多 1000 位。 |
| `visibleFrom` | string 或 null | 否 | 可见开始时间。 |
| `visibleUntil` | string 或 null | 否 | 可见结束时间，必须晚于 `visibleFrom`。 |
| `reason` | string | 是 | 1 到 200 位，创建原因。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `AdminContentItem`。

业务规则：新建内容默认状态为 `DRAFT`。slug 在未软删除内容中唯一，冲突返回 `43411`。分类或标签不存在返回 `43401` 或 `43405`。`MEMBER_WORK` 且传入 `memberId` 时必须保存 profile 快照，profile 不可用时不得创建可信成员作品。请求体中的操作者字段不可信。URL 必须校验协议或站内路径，危险脚本不得作为可执行内容公开。

幂等规则：同一操作者、同一 `idempotencyKey`、同一请求体重复提交时返回同一个内容。相同幂等键搭配不同请求体返回 `43002`。

审计要求：成功写入 `CONTENT_ITEM_CREATED`。审计失败返回 `51401`，不得创建内容。

### 修改内容

`PATCH /api/v1/content/admin/items/{contentId}`

请求字段同创建内容，除 `reason` 必填外其余字段按需修改。`type` 创建后不允许修改。

成功响应 HTTP `200`，`data` 为更新后的 `AdminContentItem`。

业务规则：不存在返回 `43400`。`ARCHIVED` 和 `DELETED` 不允许修改，返回 `43410`。已发布内容修改后仍保持原公开版本，除非实现选择单版本模型；P0 允许单版本模型，但修改已发布内容必须写审计，公开接口不得返回半更新对象。slug 冲突返回 `43411`。分类、标签和 profile 快照规则同创建。

审计要求：成功写入 `CONTENT_ITEM_UPDATED`，记录变更前后摘要和原因。审计失败不得改变内容。

### 提交审核

`PATCH /api/v1/content/admin/items/{contentId}/submit-review`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，提交说明。 |

成功响应 HTTP `200`，`data` 为更新后的 `AdminContentItem`。

状态流转：`DRAFT`、`REJECTED`、`NEEDS_CHANGES` 可流转为 `PENDING_REVIEW`。其他状态返回 `43410`。重复提交已经处于 `PENDING_REVIEW` 的内容返回成功，保持幂等，不重复写审计。

审计要求：首次提交写入 `CONTENT_ITEM_SUBMITTED`。

### 审核通过

`PATCH /api/v1/content/admin/items/{contentId}/approve`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reviewOpinion` | string | 是 | 1 到 500 位，审核意见。 |
| `reason` | string | 是 | 1 到 200 位，审核原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `AdminContentItem`。

状态流转：`PENDING_REVIEW` 可流转为 `APPROVED`。重复审核已 `APPROVED` 内容返回成功，保持幂等，不重复写审计，不重复投递通知。

通知规则：存在 `authorUserId` 时必须向作者投递审核通过通知。强制通知失败时返回 `46410` 或 `46411`，状态保持 `PENDING_REVIEW`。

审计要求：成功写入 `CONTENT_ITEM_APPROVED`。

### 审核拒绝

`PATCH /api/v1/content/admin/items/{contentId}/reject`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reviewOpinion` | string | 是 | 1 到 500 位，拒绝原因。 |
| `reason` | string | 是 | 1 到 200 位，后台操作原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `AdminContentItem`。

状态流转：`PENDING_REVIEW` 可流转为 `REJECTED`。重复拒绝已 `REJECTED` 内容返回成功，保持幂等，不重复写审计，不重复投递通知。

通知规则：存在 `authorUserId` 时必须向作者投递拒绝通知。强制通知失败时状态保持 `PENDING_REVIEW`。

审计要求：成功写入 `CONTENT_ITEM_REJECTED`。

### 要求修改

`PATCH /api/v1/content/admin/items/{contentId}/request-changes`

请求字段同审核拒绝。

成功响应 HTTP `200`，`data` 为更新后的 `AdminContentItem`。

状态流转：`PENDING_REVIEW` 可流转为 `NEEDS_CHANGES`。存在 `authorUserId` 时必须投递要求修改通知。强制通知失败时状态保持 `PENDING_REVIEW`。

审计要求：成功写入 `CONTENT_ITEM_CHANGES_REQUESTED`。

### 发布内容

`PATCH /api/v1/content/admin/items/{contentId}/publish`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `visibleFrom` | string 或 null | 否 | 覆盖可见开始时间。 |
| `visibleUntil` | string 或 null | 否 | 覆盖可见结束时间。 |
| `reason` | string | 是 | 1 到 200 位，发布原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `AdminContentItem`。

状态流转：`APPROVED` 和 `OFFLINE` 可发布。发布后状态保持 `APPROVED`，写入或更新 `publishedAt`。重复发布已公开可见内容返回成功，保持幂等，不重复写审计。`DRAFT`、`PENDING_REVIEW`、`REJECTED`、`NEEDS_CHANGES`、`ARCHIVED`、`DELETED` 返回 `43410`。

通知规则：发布通知为辅助提醒，失败不阻塞发布，但必须写入审计摘要。

### 下架内容

`PATCH /api/v1/content/admin/items/{contentId}/offline`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，下架原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `AdminContentItem`。

状态流转：公开中的 `APPROVED` 内容可流转为 `OFFLINE`，并从公开接口消失。重复下架 `OFFLINE` 内容返回成功，保持幂等，不重复写审计。

### 归档内容

`PATCH /api/v1/content/admin/items/{contentId}/archive`

请求字段同下架内容。

成功响应 HTTP `200`，`data` 为更新后的 `AdminContentItem`。

状态流转：`DRAFT`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE` 可流转为 `ARCHIVED`。`APPROVED` 且已公开内容必须先下架再归档。重复归档返回成功，保持幂等，不重复写审计。

### 软删除内容

`PATCH /api/v1/content/admin/items/{contentId}/delete`

请求字段同下架内容。

成功响应 HTTP `200`，`data` 为更新后的 `AdminContentItem`。

业务规则：只做软删除，状态为 `DELETED`，写入 `deletedAt`。已公开内容必须先下架。重复软删除返回成功，保持幂等，不重复写审计。真实删除不在 P0 content API 中提供。

### 内容版本列表

`GET /api/v1/content/admin/items/{contentId}/versions`

查询参数为公共分页参数，默认按 `version_desc` 排序。成功响应 HTTP `200`，分页 `items` 为 `ContentItemVersion[]`。只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 返回 `42001`。内容不存在返回 `43400`。

业务规则：内容创建时必须生成第 1 个版本。每次成功创建、修改、发布和恢复内容都必须生成新版本。提交审核、审核通过、审核拒绝、要求修改、下架、归档和软删除只改变状态流转，不强制生成内容版本，但必须写审计。公开接口不得返回版本历史。

### 内容版本详情

`GET /api/v1/content/admin/items/{contentId}/versions/{version}`

成功响应 HTTP `200`，`data` 为 `ContentItemVersion`。内容不存在返回 `43400`。版本不存在返回 `43417`。只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 返回 `42001`。

### 恢复内容版本

`PATCH /api/v1/content/admin/items/{contentId}/versions/{version}/restore`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，恢复原因。 |

成功响应 HTTP `200`，`data` 为恢复后的 `AdminContentItem`。

业务规则：恢复不会修改历史版本本身，而是把目标版本的可编辑字段复制到当前内容并生成一个新的当前版本。可编辑字段包括 `type`、`slug`、`title`、`summary`、`body`、`coverUrl`、`categoryId`、`tagIds`、`visibility`、`memberSnapshot`、`seo`、`adminNote`、`visibleFrom` 和 `visibleUntil`。恢复后状态必须为 `DRAFT`，`publishedAt`、`submittedAt`、`reviewedAt`、`reviewOpinion`、`notificationStatus` 和 `deletedAt` 清空，必须重新走审核和发布流程。当前内容为 `ARCHIVED` 或 `DELETED` 时返回 `43418`。目标版本不存在返回 `43417`。恢复时如果目标版本 slug 已被其他未删除内容占用，返回 `43411`。恢复成功写审计 `CONTENT_ITEM_VERSION_RESTORED`，审计中记录来源版本和新版本号。

### 内容审计列表

`GET /api/v1/content/admin/items/{contentId}/audit-logs`

查询参数为公共分页参数。成功响应 HTTP `200`，分页 `items` 为 `ContentAuditLog[]`。只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 返回 `42001`。内容不存在返回 `43400`。审计日志不得通过 content API 删除。

## 后台首页配置接口

### 后台首页配置详情

`GET /api/v1/content/admin/home`

成功响应 HTTP `200`，返回草稿配置、已发布配置、版本号和最近审计摘要。没有配置时返回空草稿和空发布配置，不返回错误。

### 保存首页草稿配置

`PUT /api/v1/content/admin/home`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `sections` | array | 是 | `HomeSection` 草稿，最多 20 个区块。 |
| `seo` | object 或 null | 否 | 首页 SEO。 |
| `reason` | string | 是 | 1 到 200 位，保存原因。 |
| `idempotencyKey` | string | 否 | 保存重试幂等键，24 小时内有效。 |

成功响应 HTTP `200`，返回草稿配置。

业务规则：引用的内容或专题可以暂时不存在，但预览和发布必须返回引用校验结果。保存草稿不影响公开首页。相同幂等键和请求体重复提交返回同一版本；幂等键冲突返回 `43002`。

### 首页配置预览

`POST /api/v1/content/admin/home/preview`

请求字段同保存首页草稿配置，`reason` 非必填。

成功响应 HTTP `200`，`data` 为预览渲染结果，必须包含引用校验结果和 `createdPublishedVersion: false`。

业务规则：预览不保存草稿、不发布配置、不写发布审计、不改变公开首页。

### 发布首页配置

`PATCH /api/v1/content/admin/home/publish`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，发布原因。 |

成功响应 HTTP `200`，返回已发布首页配置。

业务规则：把当前草稿发布为新的公开版本。没有草稿返回 `43404`。重复发布未变化草稿返回成功，保持幂等，不重复创建版本。发布后公开首页只读取已发布版本。

### 回滚首页配置

`PATCH /api/v1/content/admin/home/rollback`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `version` | integer | 是 | 要回滚到的已发布版本。 |
| `reason` | string | 是 | 1 到 200 位，回滚原因。 |

成功响应 HTTP `200`，返回回滚后的已发布配置。目标版本不存在返回 `43404`。回滚必须写审计。

## 后台分类接口

### 后台分类列表

`GET /api/v1/content/admin/categories`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `includeArchived` | boolean | 否 | 默认 `true`，后台可查看归档分类。 |

成功响应 HTTP `200`，`data.items` 为 `ContentCategory[]`，按 `sortOrder`、`name` 稳定排序。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。未登录返回 `41000`。

业务规则：后台分类列表可以返回归档分类，但不得返回审计原因、幂等键或内部错误摘要。

### 创建分类

`POST /api/v1/content/admin/categories`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `name` | string | 是 | 2 到 40 位，同一未归档分类中唯一。 |
| `slug` | string | 是 | 3 到 80 位，只允许小写字母、数字和短横线，同一未归档分类中唯一。 |
| `description` | string 或 null | 否 | 最多 200 位。 |
| `sortOrder` | integer | 否 | 默认 `100`。 |
| `reason` | string | 是 | 1 到 200 位，创建原因。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `ContentCategory`。

业务规则：分类名称或 slug 与未归档分类冲突时返回 `43001` 或 `43411`，实现必须在同一版本内固定冲突码。归档分类的名称或 slug 是否允许复用由实现决定，但必须写入测试，且不得恢复为两个未归档同名分类。

幂等规则：同一操作者、同一 `idempotencyKey`、同一请求体重复提交时返回同一分类。相同幂等键搭配不同请求体返回 `43002`。

审计要求：成功写入 `CONTENT_CATEGORY_CREATED`。审计失败返回 `51401`，不得创建分类。

### 修改分类

`PATCH /api/v1/content/admin/categories/{categoryId}`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `name` | string | 否 | 2 到 40 位。 |
| `slug` | string | 否 | 3 到 80 位，只允许小写字母、数字和短横线。 |
| `description` | string 或 null | 否 | 最多 200 位。 |
| `sortOrder` | integer | 否 | 展示排序。 |
| `reason` | string | 是 | 1 到 200 位，修改原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `ContentCategory`。

业务规则：分类不存在返回 `43401`。名称或 slug 冲突返回 `43001` 或 `43411`。已归档分类允许修改展示说明和排序，但不得通过修改接口取消归档。

审计要求：成功写入 `CONTENT_CATEGORY_UPDATED`，记录变更前后摘要和原因。审计失败时不得改变分类。

### 归档分类

`PATCH /api/v1/content/admin/categories/{categoryId}/archive`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，归档原因。 |

成功响应 HTTP `200`，`data` 为归档后的 `ContentCategory`。

业务规则：分类不存在返回 `43401`。仍被未归档、未软删除内容引用时返回 `43415`。重复归档已归档分类返回成功，保持幂等，不重复写审计。归档后公开分类列表不得返回该分类。

审计要求：首次归档写入 `CONTENT_CATEGORY_ARCHIVED`。

## 后台标签接口

### 后台标签列表

`GET /api/v1/content/admin/tags`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `includeArchived` | boolean | 否 | 默认 `true`，后台可查看归档标签。 |

成功响应 HTTP `200`，`data.items` 为 `ContentTag[]`，按 `name` 稳定排序。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。未登录返回 `41000`。

### 创建标签

`POST /api/v1/content/admin/tags`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `name` | string | 是 | 1 到 24 位，同一未归档标签中唯一。 |
| `slug` | string | 是 | 2 到 80 位，只允许小写字母、数字和短横线，同一未归档标签中唯一。 |
| `reason` | string | 是 | 1 到 200 位，创建原因。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `ContentTag`。

业务规则：标签名称或 slug 冲突返回 `43001` 或 `43411`，实现必须在测试中固定返回码。字段非法返回 `40001`。

幂等规则：同一操作者、同一 `idempotencyKey`、同一请求体重复提交时返回同一标签。相同幂等键搭配不同请求体返回 `43002`。

审计要求：成功写入 `CONTENT_TAG_CREATED`。审计失败返回 `51401`，不得创建标签。

### 修改标签

`PATCH /api/v1/content/admin/tags/{tagId}`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `name` | string | 否 | 1 到 24 位。 |
| `slug` | string | 否 | 2 到 80 位，只允许小写字母、数字和短横线。 |
| `reason` | string | 是 | 1 到 200 位，修改原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `ContentTag`。

业务规则：标签不存在返回 `43405`。名称或 slug 冲突返回 `43001` 或 `43411`。已归档标签不得通过修改接口取消归档。

审计要求：成功写入 `CONTENT_TAG_UPDATED`，记录变更前后摘要和原因。审计失败时不得改变标签。

### 归档标签

`PATCH /api/v1/content/admin/tags/{tagId}/archive`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，归档原因。 |

成功响应 HTTP `200`，`data` 为归档后的 `ContentTag`。

业务规则：标签不存在返回 `43405`。仍被未归档、未软删除内容引用时返回 `43415`。重复归档已归档标签返回成功，保持幂等，不重复写审计。归档后公开标签列表不得返回该标签。

审计要求：首次归档写入 `CONTENT_TAG_ARCHIVED`。

## 后台专题接口

### 后台专题列表

`GET /api/v1/content/admin/topics`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `status` | string | 否 | 任一 `ContentStatus`。 |
| `visibility` | string | 否 | 任一 `ContentVisibility`。 |
| `keyword` | string | 否 | 匹配标题、摘要或 slug，最多 80 位。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`updatedAt_desc`、`publishedAt_desc`、`title_asc`。 |

成功响应 HTTP `200`，分页 `items` 为 `TopicPage[]`。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。未登录返回 `41000`。

### 后台专题详情

`GET /api/v1/content/admin/topics/{topicId}`

成功响应 HTTP `200`，`data` 为 `TopicPage`，允许包含后台引用校验摘要。

业务规则：专题不存在返回 `43402`。后台详情不得返回幂等键或审计参数摘要。

### 创建专题

`POST /api/v1/content/admin/topics`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `slug` | string | 是 | 3 到 120 位，只允许小写字母、数字、短横线和斜杠，不能以斜杠结尾。 |
| `title` | string | 是 | 2 到 120 位。 |
| `summary` | string 或 null | 否 | 最多 300 位。 |
| `coverUrl` | string 或 null | 否 | http、https 或站内资源路径，最多 500 位。 |
| `visibility` | string | 否 | 默认 `PUBLIC`。 |
| `contentIds` | string[] | 否 | 专题引用内容 ID，最多 50 个。 |
| `seo` | object 或 null | 否 | 专题 SEO。 |
| `reason` | string | 是 | 1 到 200 位，创建原因。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `TopicPage`，状态默认为 `DRAFT`。

业务规则：slug 在未软删除专题中唯一，冲突返回 `43411`。`contentIds` 引用不存在内容时，P0 允许保存草稿并在后台详情和预览中记录引用问题；发布时必须只展示可公开引用，公开接口不得泄露引用失败原因。字段非法返回 `40001`。

幂等规则：同一操作者、同一 `idempotencyKey`、同一请求体重复提交时返回同一专题。相同幂等键搭配不同请求体返回 `43002`。

审计要求：成功写入 `CONTENT_TOPIC_CREATED`。审计失败返回 `51401`，不得创建专题。

### 修改专题

`PATCH /api/v1/content/admin/topics/{topicId}`

请求字段同创建专题，除 `reason` 必填外其余字段按需修改。

成功响应 HTTP `200`，`data` 为更新后的 `TopicPage`。

业务规则：专题不存在返回 `43402`。`ARCHIVED` 和 `DELETED` 专题不允许修改，返回 `43414`。slug 冲突返回 `43411`。已发布专题修改后必须保证公开接口不返回半更新状态；P0 可以采用单版本模型，但必须写审计。

审计要求：成功写入 `CONTENT_TOPIC_UPDATED`，记录变更前后摘要和原因。审计失败时不得改变专题。

### 发布专题

`PATCH /api/v1/content/admin/topics/{topicId}/publish`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，发布原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `TopicPage`。

状态流转：`DRAFT` 和 `OFFLINE` 可发布为 `APPROVED`，并写入或更新 `publishedAt`。重复发布已 `APPROVED` 专题返回成功，保持幂等，不重复写审计。`ARCHIVED` 和 `DELETED` 返回 `43414`。

审计要求：首次发布写入 `CONTENT_TOPIC_PUBLISHED`。

### 下架专题

`PATCH /api/v1/content/admin/topics/{topicId}/offline`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，下架原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `TopicPage`。

状态流转：`APPROVED` 可流转为 `OFFLINE`，并从公开专题接口消失。重复下架 `OFFLINE` 专题返回成功，保持幂等，不重复写审计。`DRAFT`、`ARCHIVED` 和 `DELETED` 返回 `43414`。

审计要求：首次下架写入 `CONTENT_TOPIC_OFFLINED`。

### 归档专题

`PATCH /api/v1/content/admin/topics/{topicId}/archive`

请求字段同下架专题。

成功响应 HTTP `200`，`data` 为更新后的 `TopicPage`。

状态流转：`DRAFT` 和 `OFFLINE` 可流转为 `ARCHIVED`。已公开 `APPROVED` 专题必须先下架再归档，直接归档返回 `43414`。重复归档返回成功，保持幂等，不重复写审计。

审计要求：首次归档写入 `CONTENT_TOPIC_ARCHIVED`。

### 软删除专题

`PATCH /api/v1/content/admin/topics/{topicId}/delete`

请求字段同下架专题。

成功响应 HTTP `200`，`data` 为更新后的 `TopicPage`。

业务规则：只做软删除，状态为 `DELETED`。已公开 `APPROVED` 专题必须先下架再软删除，直接删除返回 `43414`。重复软删除返回成功，保持幂等，不重复写审计。真实删除不在 P0 content API 中提供。

审计要求：首次软删除写入 `CONTENT_TOPIC_DELETED`。

## 后台 SEO 接口

### 后台 SEO 列表

`GET /api/v1/content/admin/seo`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `route` | string | 否 | 精确匹配站内路由。 |
| `keyword` | string | 否 | 匹配标题、描述或关键词，最多 80 位。 |
| `sort` | string | 否 | 允许 `updatedAt_desc`、`route_asc`。 |

成功响应 HTTP `200`，分页 `items` 为 `SeoPayload[]`。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。未登录返回 `41000`。

### 后台 SEO 详情

`GET /api/v1/content/admin/seo/{seoId}`

成功响应 HTTP `200`，`data` 为 `SeoPayload`。

业务规则：SEO 配置不存在返回 `43403`。后台详情不得返回审计参数摘要或幂等键。

### 保存路由 SEO

`PUT /api/v1/content/admin/seo/by-route`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `route` | string | 是 | 站内路由，以 `/` 开头，最多 200 位。 |
| `title` | string | 是 | SEO 标题，2 到 80 位。 |
| `description` | string | 是 | SEO 描述，1 到 200 位。 |
| `keywords` | string[] | 否 | 最多 20 个关键词，每个最多 40 位。 |
| `coverUrl` | string 或 null | 否 | 分享封面，http、https 或站内资源路径。 |
| `robots` | string | 是 | 任一 `SeoRobots`。 |
| `canonicalUrl` | string 或 null | 否 | canonical URL，最多 500 位。 |
| `reason` | string | 是 | 1 到 200 位，保存原因。 |
| `idempotencyKey` | string | 否 | 保存重试幂等键，24 小时内有效。 |

成功响应 HTTP `200`，`data` 为启用后的 `SeoPayload`。

业务规则：同一路由只能有一条启用 SEO 配置。路由不存在时创建；路由已存在时覆盖启用配置并更新 `updatedAt`。route 非法、robots 非法、URL 非法或关键词超限返回 `40001`。保存后公开 SEO 接口命中该配置。

幂等规则：同一操作者、同一 `idempotencyKey`、同一请求体重复提交时返回同一 SEO 配置。相同幂等键搭配不同请求体返回 `43002`。

审计要求：创建时写入 `CONTENT_SEO_CREATED`，覆盖时写入 `CONTENT_SEO_UPDATED`。审计失败返回 `51401`，不得改变 SEO 配置。

### 禁用路由 SEO

`PATCH /api/v1/content/admin/seo/{seoId}/disable`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，禁用原因。 |

成功响应 HTTP `200`，`data` 为禁用后的 `SeoPayload`。

业务规则：SEO 配置不存在返回 `43403`。重复禁用已禁用配置返回成功，保持幂等，不重复写审计。禁用后公开 SEO 接口返回模块默认 SEO，`seoId` 为 `null`。

审计要求：首次禁用写入 `CONTENT_SEO_DISABLED`。

## content 自检摘要

`GET /api/v1/content/admin/ops/summary`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "content",
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "profileMode": "TEST_STUB",
    "notificationMode": "TEST_STUB",
    "itemsTotal": 12,
    "publishedItemsTotal": 6,
    "topicsTotal": 2,
    "homeVersionsTotal": 3,
    "auditsTotal": 20,
    "lastAuditAt": "2026-05-22T00:00:00Z",
    "warnings": [
      "P0_IN_MEMORY_STORAGE",
      "P0_AUTH_STUB"
    ]
  }
}
```

业务规则：自检摘要用于后台确认 content 当前运行模式、数据规模、适配层状态和生产化缺口。摘要不得返回 token、请求头、正文、后台备注、审核意见、审计原因、通知正文或用户敏感字段。只有 `ADMIN` 和 `OWNER` 可访问。

## 状态、幂等和并发

内容状态流转为 `DRAFT` 到 `PENDING_REVIEW`，`PENDING_REVIEW` 到 `APPROVED`、`REJECTED` 或 `NEEDS_CHANGES`，`REJECTED` 和 `NEEDS_CHANGES` 可重新提交审核，`APPROVED` 发布后公开可见，公开中的 `APPROVED` 可下架为 `OFFLINE`，`OFFLINE` 可重新发布或归档，`DRAFT`、`REJECTED`、`NEEDS_CHANGES` 和 `OFFLINE` 可归档或软删除，`ARCHIVED` 原则上不可恢复，`DELETED` 为软删除终态。

创建内容、创建分类、创建标签、创建专题、保存首页草稿和保存 SEO 配置支持 `idempotencyKey`。并发使用同一幂等键只能创建或更新一次。并发创建相同 slug、分类名称、标签名称或 SEO route 时只能一个成功，其余返回冲突。

公开读取接口允许读到更新前或更新后的完整版本，但不能返回半更新对象。发布首页配置和内容发布必须以服务端当前状态为准，不得因为并发写入产生两个相同版本号或两个公开 slug。

## 审计要求

必须审计的动作包括创建内容、修改内容、提交审核、审核通过、审核拒绝、要求修改、发布、下架、归档、软删除、保存首页草稿、发布首页配置、回滚首页配置、创建分类、修改分类、归档分类、创建标签、修改标签、归档标签、创建专题、修改专题、发布专题、下架专题、归档专题、软删除专题、保存 SEO 和禁用 SEO。

后台写操作必须记录 `reason`。审计字段继承公共契约。审计写入失败时，后台写操作不得假装成功，必须返回 `51401` 或 `51400`，并保持业务数据不变。

公开读取和后台低风险读取不强制写审计。

## 失败降级

公开首页必须支持局部降级，单个引用失效不能导致整页空白。公开内容、专题、分类、标签和 SEO 读取失败时，前端按对应区域局部降级，content 不得伪造成功数据。

auth 认证上下文失败时，后台接口不得使用旧用户上下文继续写入。profile 快照失败时，不得创建新的可信成员作品。notification 强制投递失败时，审核通过、拒绝和要求修改不得改变状态；辅助通知失败时主流程可成功，但必须保留失败摘要和审计记录。

## 验收口径

`content` API 文档按 `docs/contracts-content.md` 独立存在，并由 `.local-docs/tests-content.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`content` 完成时必须满足以下条件：全部接口按本文档实现；公开接口不泄露后台字段；后台接口按角色限制；首页配置由后端返回且公开首页支持局部降级；成员作品只保存 profile 快照且不直接读 profile 数据库；审核通知按强制或辅助规则处理；分类、标签、专题、SEO、状态流转、幂等、审计和自检摘要都有自动化测试；`.local-docs/tests-content.md` 中全部测试用例都有对应自动化验证；未实现时自动化测试必须先失败；实现后 content 全部测试通过；auth、profile 和 notification 前序服务回归测试通过；没有修改前序服务稳定接口；没有把 `.local-docs/` 提交到仓库。
