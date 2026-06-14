# 北冥官网 guide API 契约

版本：0.1

## 文档定位

本文档是 `guide` 模块的正式 API 契约。后续 `content`、`onboarding`、`exam`、`whitelist`、`resource`、`server-status`、`community`、`admin` 和前端指南中心只能通过本文档定义的接口读取或维护指南与知识库主数据，不能直接读取或修改 `guide` 数据库。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、能力点、审计字段、风险等级、通用状态模型和通用错误码均以公共契约为准。本文档只补充 `guide` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了成熟文档和知识库平台的设计，但只吸收适合北冥官网当前阶段的边界。GitBook 的 Space、Collection 和多版本发布说明适合借鉴为分类、文档集和版本化发布。Docusaurus 的 docs versioning 与 sidebar 思路适合借鉴为冻结版本、目录和侧边栏排序。Confluence 的空间、页面树、标签、权限和内容分析适合借鉴为后台维护、可见性和维护反馈。Notion Wiki 的 owner、verified page 和过期验证适合借鉴为负责人、验证时间和过期提醒。Algolia DocSearch 的 facet、版本过滤、命中摘要和 no-result feedback 适合借鉴为本地搜索结果摘要与反馈闭环。Discord Rules Screening 和 Community Onboarding 适合借鉴为规则确认、外部入口准入条件和社区加入说明。本文档不引入外部文档平台、外部搜索服务、跨平台 SSO 或第三方主数据同步。

参考来源包括 [GitBook collections](https://gitbook.com/docs/creating-content/content-structure/collection)、[GitBook multiple versions](https://gitbook.com/docs/help-center/published-documentation/publishing/how-can-i-publish-a-site-with-multiple-versions)、[Docusaurus docs versioning](https://docusaurus.io/docs/versioning)、[Docusaurus sidebar](https://docusaurus.io/docs/sidebar)、[Confluence spaces](https://support.atlassian.com/confluence-cloud/docs/create-a-space/)、[Confluence labels](https://support.atlassian.com/confluence-cloud/docs/use-labels-to-organize-your-content/)、[Notion wikis and verified pages](https://www.notion.com/help/wikis-and-verified-pages)、[Algolia DocSearch facets](https://docsearch.algolia.com/docs/legacy/faceting/)、[Algolia DocSearch insights](https://docsearch.algolia.com/docs/insights/)、[Discord Rules Screening FAQ](https://support.discord.com/hc/en-us/articles/1500000466882-Rules-Screening-FAQ) 和 [Discord Community Onboarding FAQ](https://support.discord.com/hc/en-us/articles/11074987197975-Community-Onboarding-FAQ)。

## 职责边界

`guide` 负责指南分类、指南文章、章节目录、版本记录、规则版本、客户端环境说明、常用指令索引、服务器地址说明、资源说明引用、外部交流入口、搜索摘要、用户反馈、后台审核、发布、下架、归档、软删除、审计和自检摘要。

`guide` 不负责注册、登录、会话、角色权限主数据、成员档案主数据、站内通知主数据、首页配置、公告文章、专题页、真实资源下载、Cloudreve 分享票据、素材投稿、社区讨论、活动报名、考试判分、白名单审核、实时服务器状态采集、真实服务器运维、节点执行、文件管理、容器、终端、备份恢复或外部聊天记录同步。

`content` 继续拥有首页、公告、页面、专题和 SEO。`guide` 不反写 content 首页配置，不把指南复制成 content 文章。`resource` 继续拥有玩家资源和下载入口。`guide` 可以保存资源公开快照引用，但不能生成下载票据，不能保存 Cloudreve 分享密码，不能修改 resource 主数据。`server-status` 继续拥有实时状态和线路。`guide` 可以保存服务器地址说明和线路说明引用，但不能缓存实时在线人数，不能伪造健康状态。`notification` 继续拥有通知主数据。`guide` 只能保存投递结果摘要。`profile` 只提供作者、维护人或负责人公开快照。

外部交流入口第一版归入 `guide`。它只展示 Oopz、QQ群、游戏内聊天等渠道的用途、加入条件、规则和注意事项，不同步外部聊天记录，不做跨平台 SSO，不替代工单、举报、审核和运营记录。

## 数据归属

`guide` 拥有以下主数据：指南分类、指南文章、指南版本、章节目录、命令索引条目、外部渠道入口、规则版本索引、用户反馈、搜索摘要、幂等记录、指南审计日志和运行自检摘要。

`guide` 可以保存当前用户、作者、维护人、资源、线路和通知的安全快照。快照只用于展示、检索、审计和降级，不是来源模块主数据，不能作为权限、成员资格、资源下载或实时状态的最终判断。

浏览器请求体不得覆盖 `createdBy`、`updatedBy`、`submittedBy`、`reviewedBy`、`publishedBy`、`offlineBy`、`archivedBy`、`deletedBy`、`authorSnapshot`、`maintainerSnapshot`、`status`、`publishedAt`、`version`、`auditResult`、`notificationStatus`、`searchWeight` 等服务端可信字段。实现可以忽略这些字段，但生产实现推荐返回字段级校验错误。

## 基础路径与认证

公开接口使用 `/api/v1/guides` 前缀，不要求登录。公开接口只能返回 `PUBLISHED`、`PUBLIC`、未下架、未归档、未软删除、处于可见时间范围内且分类启用的数据。公开响应不得返回后台备注、审核意见、审计字段、通知结果、幂等键、内部引用失败堆栈、作者敏感字段、资源下载密钥或外部渠道管理凭据。

当前用户反馈接口使用 `/api/v1/guides` 下的文章反馈路径，要求登录。反馈只用于知识库维护队列，不替代 `community` 的工单、举报和讨论。

后台接口使用 `/api/v1/guides/admin` 前缀，全部要求登录。后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`。创建、修改、提交审核、审核、发布、下架、归档、软删除、版本恢复、分类维护、渠道维护和反馈处理要求 `ADMIN` 或 `OWNER`，其中审核通过、拒绝和要求修改可以由 `HELPER` 执行，便于协管参与知识库审核。审计列表和自检摘要只允许 `ADMIN` 或 `OWNER`。

`guide` 当前由 `portal-core-service:8134` 承载。历史原服务端口 `8127` 只作为对照记录，不再作为当前网关默认上游。`api-gateway` 必须以兼容方式保留 `/api/v1/guides` 路由，不能改变已有服务路径、认证方式、响应格式或测试。

## 前序模块兼容契约

`guide` 适配 `auth`。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions`、`status` 和可选 Minecraft 绑定。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得提交反馈或访问后台接口。auth 不可用返回 `46940`，auth 超时返回 `46941`，auth 字段或枚举不兼容返回 `46942`。

`guide` 通过 api-gateway 访问时，优先读取网关注入的可信身份头：`X-Beiming-Actor-User-Id`、`X-Beiming-Actor-Roles`、`X-Beiming-Actor-Permissions`、`X-Beiming-Actor-Minecraft-Id`、`X-Beiming-Actor-Minecraft-Uuid` 和 `X-Gateway-Internal-Request-Id`。P0 只有 `X-Gateway-Internal-Request-Id` 与当前 `X-Request-Id` 一致时，才接受这些 actor 头。浏览器伪造 actor 头必须忽略，不得覆盖 Bearer 认证结果。生产接入内部签名或 mTLS 前，自检摘要必须暴露 `GATEWAY_INTERNAL_SIGNATURE_NOT_ENABLED` 缺口。

`guide` 适配 `profile` 读取作者、维护人或负责人公开快照。profile 成员不存在、不可公开或状态不允许作为维护人时返回 `46950`。profile 超时返回 `46951`，字段或枚举不兼容返回 `46952`。profile 不可用时不得创建新的可信维护人快照；已发布指南公开读取可以使用旧快照并在后台详情标记 `snapshotStale=true`。

`guide` 适配 `notification` 投递审核拒绝、要求修改、发布提醒、下架提醒和反馈处理提醒。审核拒绝和要求修改是强制通知，通知失败返回 `46960` 或 `46961`，状态不得变化。发布、下架、归档、软删除和反馈处理为辅助通知，通知失败时主流程可以成功，但必须记录失败摘要。`guide` 不保存通知主数据、未读数、模板或外部渠道投递记录。

`guide` 适配 `resource` 读取玩家资源公开快照，用于客户端整合包、Java 环境、下载加速、地图包、材质包或规则文档说明。resource 不可用返回 `46970`，超时返回 `46971`，字段不兼容返回 `46972`。公开读取可以在资源不可用时返回指南正文并标记 `degraded=true`，不得生成下载票据或返回下载 URL。

`guide` 适配 `server-status` 读取公开线路说明快照，用于服务器地址和线路指南。server-status 不可用返回 `46980`，超时返回 `46981`，字段不兼容返回 `46982`。`guide` 只保存线路说明引用和地址文案，不保存实时在线人数、MOTD、延迟和健康结果。

`guide` 不要求 `content` 反向适配。首页或专题需要展示指南时，由前端或后续 content 兼容变更读取 guide 公开接口。`onboarding` 当前已有规则确认和 `guideRoute` 字段，guide 第一版不修改 onboarding。等 guide 自身闭环完成后，onboarding 引用 guide 规则版本必须作为单独兼容变更处理。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `GuideType` | `SERVER_RULE`、`JOIN_GUIDE`、`PLAY_GUIDE`、`PLAYER_GUIDE`、`ENGINEERING`、`ACTIVITY_GUIDE`、`COMMAND_REFERENCE`、`CLIENT_SETUP`、`JAVA_ENVIRONMENT`、`SERVER_ADDRESS`、`DOWNLOAD_ACCELERATION`、`EXTERNAL_CHANNEL`、`TROUBLESHOOTING`、`PLUGIN_GUIDE`、`OTHER` | 指南类型。 |
| `GuideStatus` | `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`PUBLISHED`、`OFFLINE`、`ARCHIVED`、`DELETED` | 指南主体状态。公开可见必须为 `PUBLISHED`。 |
| `GuideVisibility` | `PUBLIC`、`AUTHENTICATED`、`MEMBER_ONLY`、`ADMIN_ONLY` | 可见范围。P0 公开接口只返回 `PUBLIC`。 |
| `GuideAudience` | `VISITOR`、`REGISTERED_USER`、`MEMBER`、`HELPER`、`ADMIN`、`OPERATOR` | 面向人群。 |
| `GuideReviewDecision` | `APPROVE`、`REJECT`、`REQUEST_CHANGES` | 审核决策。 |
| `GuideChannelType` | `OOPZ`、`QQ_GROUP`、`IN_GAME_CHAT`、`DISCORD`、`WEBSITE`、`OTHER` | 外部交流入口类型。 |
| `GuideChannelStatus` | `ENABLED`、`DISABLED`、`ARCHIVED` | 外部入口状态。 |
| `GuideFeedbackType` | `HELPFUL`、`NOT_HELPFUL`、`OUTDATED`、`BROKEN_LINK`、`UNCLEAR_STEP`、`WRONG_COMMAND`、`OTHER` | 用户反馈类型。 |
| `GuideFeedbackStatus` | `OPEN`、`RESOLVED`、`IGNORED` | 反馈处理状态。 |
| `GuideReferenceType` | `RESOURCE`、`SERVER_LINE`、`CONTENT`、`COMMUNITY_POST`、`EXTERNAL_URL` | 指南引用类型。 |
| `GuideAuditResult` | `SUCCESS`、`FAILED` | 审计执行结果。 |

## 通用对象

### GuideCategory

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `categoryId` | string | 是 | 分类 ID。 |
| `name` | string | 是 | 分类名称，2 到 40 位，同一未归档分类中唯一。 |
| `slug` | string | 是 | 分类 slug，2 到 80 位，只允许小写字母、数字和短横线。 |
| `description` | string 或 null | 是 | 分类说明，最多 200 位。 |
| `icon` | string 或 null | 是 | 图标标识或站内图标路径，最多 120 位。 |
| `sortOrder` | integer | 是 | 排序值，数字越小越靠前。 |
| `enabled` | boolean | 是 | 是否启用。 |
| `archived` | boolean | 是 | 是否归档。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |

### GuideMaintainerSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | string | 是 | auth 用户 ID。 |
| `memberId` | string 或 null | 是 | profile 成员 ID。 |
| `displayName` | string | 是 | 展示名快照。 |
| `avatarUrl` | string 或 null | 是 | 头像快照。 |
| `minecraftId` | string 或 null | 是 | Minecraft ID 快照。 |
| `memberStatus` | string 或 null | 是 | 成员状态快照。 |
| `profileSnapshotAt` | string | 是 | 快照获取时间。 |
| `snapshotStale` | boolean | 是 | 是否为旧快照降级展示。 |

### GuideTocNode

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `nodeId` | string | 是 | 目录节点 ID。 |
| `title` | string | 是 | 节点标题，1 到 80 位。 |
| `anchor` | string | 是 | 页面锚点，只允许小写字母、数字和短横线。 |
| `level` | integer | 是 | 1 到 4。 |
| `sortOrder` | integer | 是 | 同级排序。 |

### GuideCommandEntry

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `commandId` | string | 是 | 指令条目 ID。 |
| `guideId` | string | 是 | 来源指南 ID。 |
| `command` | string | 是 | 玩家可见指令，例如 `/spawn`，最多 120 位。 |
| `usage` | string | 是 | 用法说明，最多 500 位。 |
| `permissionHint` | string 或 null | 是 | 玩家侧权限提示，不等同后台能力点。 |
| `examples` | string[] | 是 | 示例，最多 10 条。 |
| `tags` | string[] | 是 | 指令标签。 |
| `updatedAt` | string | 是 | 更新时间。 |

### GuideReferenceSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `referenceId` | string | 是 | 引用 ID。 |
| `type` | string | 是 | `GuideReferenceType`。 |
| `sourceModule` | string | 是 | 来源模块。 |
| `sourceId` | string | 是 | 来源业务 ID 或外部摘要 ID。 |
| `title` | string | 是 | 展示标题。 |
| `summary` | string 或 null | 是 | 摘要。 |
| `targetRoute` | string 或 null | 是 | 站内路由。 |
| `externalUrl` | string 或 null | 是 | 外部公开 URL。不得包含私有 token。 |
| `snapshotAt` | string | 是 | 快照时间。 |
| `degraded` | boolean | 是 | 是否降级。 |

### PublicGuideSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `guideId` | string | 是 | 指南 ID。 |
| `type` | string | 是 | `GuideType`。 |
| `slug` | string | 是 | 公开 slug，同一未软删除指南中唯一。 |
| `title` | string | 是 | 标题，2 到 120 位。 |
| `summary` | string 或 null | 是 | 摘要，最多 300 位。 |
| `category` | GuideCategory 或 null | 是 | 分类公开摘要。 |
| `tags` | string[] | 是 | 标签，最多 20 个。 |
| `audience` | string[] | 是 | 面向人群。 |
| `visibility` | string | 是 | 公开接口只返回 `PUBLIC`。 |
| `pinned` | boolean | 是 | 是否置顶。 |
| `verifiedAt` | string 或 null | 是 | 最近验证时间。 |
| `expiresAt` | string 或 null | 是 | 内容建议复核时间。 |
| `currentVersion` | integer | 是 | 当前发布版本号。 |
| `ruleVersion` | string 或 null | 是 | 规则类指南版本。 |
| `maintainerSnapshot` | GuideMaintainerSnapshot 或 null | 是 | 维护人公开快照。 |
| `publishedAt` | string | 是 | 发布时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `degraded` | boolean | 是 | 是否存在局部降级。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |

### PublicGuideDetail

`PublicGuideDetail` 在 `PublicGuideSummary` 基础上补充 `body`、`toc`、`commandEntries`、`references`、`externalChannelRefs`、`visibleFrom`、`visibleUntil`、`createdAt`。公开详情不得返回后台备注、审核意见、审计字段、通知摘要、内部引用失败堆栈、幂等键或服务端可信字段。

### AdminGuideArticle

后台指南视图包含公开字段、草稿正文、状态、可见性、目录、指令条目、引用快照、外部入口引用、外部入口 ID 快照、维护人快照、后台备注、审核意见、通知摘要、验证时间、复核时间、版本号、状态时间、操作者 ID、删除时间和生产化提示。后台详情可以返回引用降级摘要，但不得返回 token、分享密码、外部渠道管理凭据、完整请求头或异常堆栈。

### GuideVersion

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `guideId` | string | 是 | 指南 ID。 |
| `version` | integer | 是 | 版本号，从 1 开始递增。 |
| `sourceAction` | string | 是 | `CREATED`、`UPDATED`、`PUBLISHED`、`RESTORED` 等。 |
| `snapshot` | AdminGuideArticle | 是 | 后台安全恢复快照，必须冻结标题、摘要、正文、类型、slug、分类、标签、受众、可见性、置顶、目录、指令条目、外部入口 ID、规则版本、可见时间窗、验证时间、复核时间、后台备注和维护人快照；不得包含版本摘要、反馈摘要、通知正文、token、密码、分享密钥、完整请求头或异常堆栈。 |
| `createdBy` | string | 是 | 创建版本的用户 ID。 |
| `createdAt` | string | 是 | 版本创建时间。 |
| `reason` | string 或 null | 是 | 操作原因。 |
| `restoredFromVersion` | integer 或 null | 是 | 若来自恢复，记录来源版本。 |

### GuideExternalChannel

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `channelId` | string | 是 | 外部入口 ID。 |
| `type` | string | 是 | `GuideChannelType`。 |
| `status` | string | 是 | `GuideChannelStatus`。 |
| `name` | string | 是 | 展示名称，2 到 60 位。 |
| `slug` | string | 是 | 公开 slug。 |
| `purpose` | string | 是 | 渠道用途，1 到 300 位。 |
| `joinCondition` | string | 是 | 加入条件，1 到 500 位。 |
| `rules` | string[] | 是 | 渠道规则，最多 20 条。 |
| `entryUrl` | string 或 null | 是 | 公开入口 URL。不得包含管理 token。 |
| `entryHint` | string 或 null | 是 | 入口提示，例如群号脱敏展示。 |
| `visibility` | string | 是 | 可见范围。 |
| `sortOrder` | integer | 是 | 排序值。 |
| `adminNote` | string 或 null | 是 | 后台备注，公开接口不得返回。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |

### GuideFeedback

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `feedbackId` | string | 是 | 反馈 ID。 |
| `guideId` | string | 是 | 指南 ID。 |
| `guideVersion` | integer | 是 | 用户反馈时看到的版本。 |
| `type` | string | 是 | `GuideFeedbackType`。 |
| `status` | string | 是 | `GuideFeedbackStatus`。 |
| `message` | string 或 null | 是 | 反馈说明，最多 1000 位。 |
| `anchor` | string 或 null | 是 | 页面锚点。 |
| `actorUserId` | string | 是 | 反馈用户 ID。 |
| `actorDisplayNameSnapshot` | string | 是 | 反馈用户展示名快照。 |
| `resolvedBy` | string 或 null | 是 | 处理人。 |
| `resolutionNote` | string 或 null | 是 | 处理说明。 |
| `createdAt` | string | 是 | 创建时间。 |
| `resolvedAt` | string 或 null | 是 | 处理时间。 |

### GuideSearchResult

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `guideId` | string | 是 | 指南 ID。 |
| `title` | string | 是 | 标题。 |
| `slug` | string | 是 | slug。 |
| `type` | string | 是 | 指南类型。 |
| `category` | GuideCategory 或 null | 是 | 分类。 |
| `matchedFields` | string[] | 是 | 命中的字段摘要。 |
| `highlight` | string | 是 | 命中摘要，最多 240 位，不含 HTML 脚本。 |
| `score` | number | 是 | 本地搜索分数摘要。 |
| `version` | integer | 是 | 当前发布版本。 |
| `publishedAt` | string | 是 | 发布时间。 |

### GuideAuditLog

审计字段继承公共契约，允许补充 `guideId`、`categoryId`、`channelId`、`feedbackId`、`version`、`ruleVersion`、`idempotencyKey`、`stateFrom`、`stateTo`、`notificationStatus`、`profileSnapshotStatus`、`resourceReferenceStatus` 和 `serverStatusReferenceStatus`。审计日志不得通过 guide API 删除。

## guide 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `43900` | 404 | 指南不存在，或公开接口无权访问该指南。 |
| `43901` | 404 | 指南分类不存在。 |
| `43902` | 404 | 指南版本不存在。 |
| `43903` | 404 | 外部渠道入口不存在。 |
| `43904` | 404 | 反馈不存在。 |
| `43910` | 409 | 指南、分类、渠道或反馈状态不允许当前操作。 |
| `43911` | 409 | slug、分类名称、渠道入口或规则版本冲突。 |
| `43912` | 409 | 指南不可公开访问。 |
| `43913` | 409 | 规则版本已过期、重复或不允许发布。 |
| `43914` | 409 | 幂等键请求指纹冲突。 |
| `43915` | 409 | 分类或渠道仍被未归档指南引用，不能归档。 |
| `46940` | 502 | auth 认证上下文不可用。 |
| `46941` | 504 | auth 认证上下文超时。 |
| `46942` | 502 | auth 认证上下文字段或枚举不兼容 guide 契约。 |
| `46950` | 502 | profile 维护人快照不可用。 |
| `46951` | 504 | profile 维护人快照超时。 |
| `46952` | 502 | profile 维护人快照字段或枚举不兼容 guide 契约。 |
| `46960` | 502 | notification 强制投递不可用。 |
| `46961` | 504 | notification 强制投递超时。 |
| `46970` | 502 | resource 公开引用不可用。 |
| `46971` | 504 | resource 公开引用超时。 |
| `46972` | 502 | resource 公开引用字段不兼容 guide 契约。 |
| `46980` | 502 | server-status 公开引用不可用。 |
| `46981` | 504 | server-status 公开引用超时。 |
| `46982` | 502 | server-status 公开引用字段不兼容 guide 契约。 |
| `51900` | 500 | guide 内部错误。 |
| `51901` | 500 | guide 审计写入失败。 |
| `51902` | 500 | guide 搜索摘要写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误和通用服务端错误优先使用公共错误码。guide 自有幂等冲突使用 `43914`。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 指南首页摘要 | GET | `/api/v1/guides/home` | 否 | 无 | LOW |
| 公开分类列表 | GET | `/api/v1/guides/categories` | 否 | 无 | LOW |
| 公开指南列表 | GET | `/api/v1/guides/articles` | 否 | 无 | LOW |
| 公开指南详情 | GET | `/api/v1/guides/articles/{guideId}` | 否 | 无 | LOW |
| 公开 slug 指南详情 | GET | `/api/v1/guides/articles/by-slug/{slug}` | 否 | 无 | LOW |
| 指南搜索 | GET | `/api/v1/guides/search` | 否 | 无 | LOW |
| 常用指令索引 | GET | `/api/v1/guides/commands` | 否 | 无 | LOW |
| 外部交流入口 | GET | `/api/v1/guides/external-channels` | 否 | 无 | LOW |
| 当前规则版本 | GET | `/api/v1/guides/rules/current` | 否 | 无 | LOW |
| 指定规则版本 | GET | `/api/v1/guides/rules/versions/{ruleVersion}` | 否 | 无 | LOW |
| 提交指南反馈 | POST | `/api/v1/guides/articles/{guideId}/feedback` | 是 | 当前用户 | LOW |
| 后台指南列表 | GET | `/api/v1/guides/admin/articles` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台指南详情 | GET | `/api/v1/guides/admin/articles/{guideId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建指南 | POST | `/api/v1/guides/admin/articles` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改指南 | PATCH | `/api/v1/guides/admin/articles/{guideId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 提交审核 | PATCH | `/api/v1/guides/admin/articles/{guideId}/submit-review` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核通过 | PATCH | `/api/v1/guides/admin/articles/{guideId}/approve` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/guides/admin/articles/{guideId}/reject` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | MEDIUM |
| 要求修改 | PATCH | `/api/v1/guides/admin/articles/{guideId}/request-changes` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | MEDIUM |
| 发布指南 | PATCH | `/api/v1/guides/admin/articles/{guideId}/publish` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 下架指南 | PATCH | `/api/v1/guides/admin/articles/{guideId}/offline` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档指南 | PATCH | `/api/v1/guides/admin/articles/{guideId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 软删除指南 | PATCH | `/api/v1/guides/admin/articles/{guideId}/delete` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 指南版本列表 | GET | `/api/v1/guides/admin/articles/{guideId}/versions` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 指南版本详情 | GET | `/api/v1/guides/admin/articles/{guideId}/versions/{version}` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 恢复指南版本 | PATCH | `/api/v1/guides/admin/articles/{guideId}/versions/{version}/restore` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 指南审计列表 | GET | `/api/v1/guides/admin/articles/{guideId}/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 后台分类列表 | GET | `/api/v1/guides/admin/categories` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建分类 | POST | `/api/v1/guides/admin/categories` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改分类 | PATCH | `/api/v1/guides/admin/categories/{categoryId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档分类 | PATCH | `/api/v1/guides/admin/categories/{categoryId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台渠道列表 | GET | `/api/v1/guides/admin/external-channels` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建渠道 | POST | `/api/v1/guides/admin/external-channels` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改渠道 | PATCH | `/api/v1/guides/admin/external-channels/{channelId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 启用渠道 | PATCH | `/api/v1/guides/admin/external-channels/{channelId}/enable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 禁用渠道 | PATCH | `/api/v1/guides/admin/external-channels/{channelId}/disable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档渠道 | PATCH | `/api/v1/guides/admin/external-channels/{channelId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台反馈列表 | GET | `/api/v1/guides/admin/feedback` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 解决反馈 | PATCH | `/api/v1/guides/admin/feedback/{feedbackId}/resolve` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 忽略反馈 | PATCH | `/api/v1/guides/admin/feedback/{feedbackId}/ignore` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| guide 自检摘要 | GET | `/api/v1/guides/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 公开接口

### 指南首页摘要

`GET /api/v1/guides/home`

成功响应 HTTP `200`，`data` 至少包含 `featuredGuides`、`pinnedGuides`、`categories`、`latestUpdatedGuides`、`currentRule`、`externalChannels`、`degraded` 和 `degradeReasons`。

业务规则：只汇总公开可见指南、启用分类和启用外部渠道。单个资源引用或线路引用失败时，可以返回指南主体并标记局部降级，不能整页失败，不能返回后台字段。

### 公开分类列表

`GET /api/v1/guides/categories`

查询参数包括 `type`、`audience` 和 `keyword`。成功响应 HTTP `200`，`data.items` 为启用且未归档分类，按 `sortOrder`、`name` 稳定排序。非法枚举返回 `40001`。分类公开响应不得返回审计、后台备注或内部引用数量。

### 公开指南列表

`GET /api/v1/guides/articles`

查询参数包括 `page`、`pageSize`、`type`、`categoryId`、`tag`、`audience`、`keyword`、`pinned` 和 `sort`。`sort` 允许 `publishedAt_desc`、`updatedAt_desc`、`title_asc`、`verifiedAt_desc`、`pinned_desc`。成功响应 HTTP `200`，分页 `items` 为 `PublicGuideSummary[]`。

业务规则：公开列表只返回 `PUBLISHED`、`PUBLIC`、未下架、未归档、未软删除、分类启用且处于可见时间范围内的指南。`AUTHENTICATED`、`MEMBER_ONLY` 和 `ADMIN_ONLY` 不进入公开列表。分页必须按过滤后的全集计算 `total`，空页返回空数组，不回退第一页。

### 公开指南详情

`GET /api/v1/guides/articles/{guideId}` 和 `GET /api/v1/guides/articles/by-slug/{slug}`

成功响应 HTTP `200`，`data` 为 `PublicGuideDetail`。指南不存在、不可公开、未发布、已下架、已归档、已删除或可见时间不匹配时返回 `43900` 或 `43912`，同一实现版本内必须固定。公开详情不得返回后台字段和服务端可信字段。

### 指南搜索

`GET /api/v1/guides/search`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `q` | string | 是 | 1 到 80 位。 |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `50`。 |
| `type` | string | 否 | 任一 `GuideType`。 |
| `categoryId` | string | 否 | 分类 ID。 |
| `tag` | string | 否 | 标签。 |
| `audience` | string | 否 | 任一 `GuideAudience`。 |
| `ruleVersion` | string | 否 | 规则版本。 |

成功响应 HTTP `200`，分页 `items` 为 `GuideSearchResult[]`，并返回 `facets` 和 `noResultFeedbackEnabled`。搜索只基于公开可见指南。高亮摘要必须脱敏，不能返回 HTML 脚本、后台备注或审计参数。P0 可以使用本地内存搜索摘要，不接入外部搜索服务。

### 常用指令索引

`GET /api/v1/guides/commands`

查询参数包括 `page`、`pageSize`、`keyword`、`tag`、`guideId` 和 `sort`。`sort` 允许 `updatedAt_desc`、`command_asc`。成功响应分页 `items` 为 `GuideCommandEntry[]`。

业务规则：只返回来自公开已发布指南的指令条目。下架、归档、删除或非公开指南中的指令不出现。指令响应不得被用于后台权限判定，`permissionHint` 只作为玩家说明。

### 外部交流入口

`GET /api/v1/guides/external-channels`

查询参数包括 `type`、`audience` 和 `keyword`。成功响应 HTTP `200`，`data.items` 为公开可见、`ENABLED`、未归档的 `GuideExternalChannel[]`，但必须移除 `adminNote`。入口可以返回公开 URL、群号脱敏提示或游戏内频道说明，不得返回管理 token、机器人 token、审核后台链接或外部聊天记录。

### 当前规则版本

`GET /api/v1/guides/rules/current`

成功响应 HTTP `200`，`data` 为当前公开生效的 `SERVER_RULE` 指南详情和 `ruleVersion`。如果没有已发布规则版本，返回 `43900` 或空降级视图，具体实现固定并写入测试。当前规则只能来自 `PUBLISHED`、`PUBLIC`、未下架、未归档、未删除的指南。

### 指定规则版本

`GET /api/v1/guides/rules/versions/{ruleVersion}`

成功响应 HTTP `200`，`data` 为指定规则版本对应的公开指南详情。不存在、未发布或不可公开返回 `43900` 或 `43913`。历史规则版本可以公开展示，但必须标记 `current=false`。

### 提交指南反馈

`POST /api/v1/guides/articles/{guideId}/feedback`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `type` | string | 是 | 任一 `GuideFeedbackType`。 |
| `message` | string 或 null | 否 | 最多 1000 位。`OTHER`、`BROKEN_LINK`、`UNCLEAR_STEP`、`WRONG_COMMAND` 建议必填。 |
| `anchor` | string 或 null | 否 | 页面锚点，最多 120 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `GuideFeedback`。业务规则：必须登录。只能对公开可见指南提交反馈。反馈记录服务端当前指南版本、用户快照和请求编号。相同用户、相同指南、相同幂等键和相同请求体重复提交返回同一反馈；同键不同请求体返回 `43914`。反馈写入失败返回 `51900`，不得伪造成功。

## 后台指南接口

### 后台指南列表

`GET /api/v1/guides/admin/articles`

查询参数包括 `page`、`pageSize`、`type`、`status`、`visibility`、`categoryId`、`tag`、`maintainerUserId`、`ruleVersion`、`expired`、`keyword` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`publishedAt_desc`、`verifiedAt_desc`、`title_asc`。成功响应分页 `items` 为 `AdminGuideArticle[]`。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。

### 后台指南详情

`GET /api/v1/guides/admin/articles/{guideId}`

成功响应 HTTP `200`，`data` 为 `AdminGuideArticle`，允许返回版本摘要、反馈摘要和引用降级摘要。指南不存在返回 `43900`。后台详情不得返回 token、Cloudreve 分享密码、外部渠道管理凭据、完整通知正文或异常堆栈。

### 创建指南

`POST /api/v1/guides/admin/articles`

请求字段包括 `type`、`slug`、`title`、`summary`、`body`、`categoryId`、`tags`、`audience`、`visibility`、`pinned`、`toc`、`commandEntries`、`references`、`externalChannelIds`、`maintainerMemberId`、`ruleVersion`、`visibleFrom`、`visibleUntil`、`verifiedAt`、`expiresAt`、`adminNote`、`reason` 和 `idempotencyKey`。

成功响应 HTTP `201`，`data` 为 `AdminGuideArticle`，默认状态为 `DRAFT`，初始版本为 `1`。`guideId` 由服务端生成且创建后不可变，旧 slug 被修改释放后再次创建同 slug 时必须生成新的 `guideId`，不得覆盖已有指南。slug 在未软删除指南中唯一，冲突返回 `43911`。分类不存在返回 `43901`。`SERVER_RULE` 类型必须提供 `ruleVersion`。非规则类型不得占用同一规则版本。审计失败返回 `51901`，不得创建指南。

### 修改指南

`PATCH /api/v1/guides/admin/articles/{guideId}`

请求字段同创建指南，除 `reason` 必填外其余字段按需修改。`ARCHIVED` 和 `DELETED` 不允许修改主体字段。修改已发布指南必须保证公开读取不会返回半更新状态。保存成功创建新版本。审计失败时不得改变指南。

### 提交审核

`PATCH /api/v1/guides/admin/articles/{guideId}/submit-review`

请求字段包括必填 `reason` 和可选 `idempotencyKey`。`DRAFT`、`REJECTED` 和 `NEEDS_CHANGES` 可流转为 `PENDING_REVIEW`。重复提交 `PENDING_REVIEW` 返回成功，保持幂等，不重复写审计。其他状态返回 `43910`。

### 审核通过

`PATCH /api/v1/guides/admin/articles/{guideId}/approve`

请求字段包括 `reviewOpinion`、`reason` 和可选 `idempotencyKey`。`PENDING_REVIEW` 可流转为 `APPROVED`。重复审核已 `APPROVED` 返回成功。`HELPER` 可执行审核通过，但不能发布。审计失败不得改变状态。

### 审核拒绝

`PATCH /api/v1/guides/admin/articles/{guideId}/reject`

请求字段包括 `reviewOpinion`、`reason` 和可选 `idempotencyKey`。`PENDING_REVIEW` 可流转为 `REJECTED`。审核拒绝必须通知创建者或维护人；强制通知失败时状态不变并返回 `46960` 或 `46961`。重复拒绝保持幂等。

### 要求修改

`PATCH /api/v1/guides/admin/articles/{guideId}/request-changes`

请求字段包括 `reviewOpinion`、`publicComment`、`reason` 和可选 `idempotencyKey`。`PENDING_REVIEW` 可流转为 `NEEDS_CHANGES`。要求修改必须通知创建者或维护人；强制通知失败时状态不变。

### 发布指南

`PATCH /api/v1/guides/admin/articles/{guideId}/publish`

请求字段包括必填 `reason` 和可选 `idempotencyKey`。`APPROVED` 和 `OFFLINE` 可流转为 `PUBLISHED`。发布写入或更新 `publishedAt`、`currentVersion` 和搜索摘要。`SERVER_RULE` 发布时 `ruleVersion` 必须唯一；若设为当前规则，旧当前规则自动变为历史规则但仍保持可读。搜索摘要写入失败返回 `51902`，不得发布。辅助通知失败不阻塞，但必须写审计摘要。

### 下架、归档和软删除

`PATCH /api/v1/guides/admin/articles/{guideId}/offline` 允许 `PUBLISHED` 流转为 `OFFLINE`。重复下架保持幂等。

`PATCH /api/v1/guides/admin/articles/{guideId}/archive` 允许 `DRAFT`、`REJECTED`、`NEEDS_CHANGES` 和 `OFFLINE` 流转为 `ARCHIVED`。公开中的指南必须先下架再归档。

`PATCH /api/v1/guides/admin/articles/{guideId}/delete` 只做软删除，状态为 `DELETED`，写入 `deletedAt`。公开中的指南必须先下架再软删除。P0 不提供真实删除接口。

这些接口请求字段均包含必填 `reason` 和可选 `idempotencyKey`。审计失败时不得改变业务状态。

## 版本、分类、渠道、反馈、审计和自检

`GET /api/v1/guides/admin/articles/{guideId}/versions` 返回指南版本分页。`GET /api/v1/guides/admin/articles/{guideId}/versions/{version}` 返回指定版本。`PATCH /api/v1/guides/admin/articles/{guideId}/versions/{version}/restore` 用指定历史版本生成新版本；恢复必须原子应用历史快照中的可恢复字段，包括标题、摘要、正文、类型、slug、分类、标签、受众、可见性、置顶、目录、指令条目、外部入口 ID、规则版本、可见时间窗、验证时间、复核时间、后台备注和维护人快照。恢复不得覆盖 `guideId`、当前状态、当前版本号、创建人、发布时间、删除时间、版本摘要、反馈摘要和审计记录；成功后递增当前版本，写入 `RESTORED` 版本并记录 `restoredFromVersion`。`ARCHIVED` 和 `DELETED` 指南不得恢复；历史 slug 被其他未删除指南占用返回 `43911`，历史规则版本被其他指南占用返回 `43913`，历史分类不存在或已归档返回 `43901`，历史外部入口不存在或已归档返回 `43903`。审计失败、幂等冲突或任一校验失败时不得改变业务状态。

后台分类接口支持列表、创建、修改和归档。创建和修改字段包括 `name`、`slug`、`description`、`icon`、`sortOrder`、`enabled`、`reason` 和可选 `idempotencyKey`。仍被未归档指南引用的分类不能归档，返回 `43915`。归档后公开分类列表不再返回。

后台渠道接口支持列表、创建、修改、启用、禁用和归档。创建和修改字段包括 `type`、`name`、`slug`、`purpose`、`joinCondition`、`rules`、`entryUrl`、`entryHint`、`visibility`、`sortOrder`、`adminNote`、`reason` 和可选 `idempotencyKey`。公开入口不得保存管理 token。被未归档指南引用的渠道不能归档，返回 `43915`。禁用后公开入口列表不再返回。

后台反馈列表支持 `page`、`pageSize`、`guideId`、`type`、`status`、`actorUserId`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`、`resolvedAt_desc`。`PATCH /api/v1/guides/admin/feedback/{feedbackId}/resolve` 和 `/ignore` 请求字段包括 `resolutionNote`、`notifyUser`、`reason` 和可选 `idempotencyKey`。重复处理保持幂等。`notifyUser=true` 时通知失败不回滚反馈处理，但必须记录失败摘要。

`GET /api/v1/guides/admin/articles/{guideId}/audit-logs` 支持 `page`、`pageSize`、`action`、`actorUserId`、`result`、`from`、`to` 和 `sort`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 guide API 删除。

`GET /api/v1/guides/admin/ops/summary` 返回服务运行模式、端口、存储模式、auth/profile/notification/resource/server-status 适配模式、指南数量、已发布数量、规则版本数量、外部入口数量、反馈数量、待处理反馈数量、搜索摘要数量、审计数量、幂等记录数量、生产化缺口和最近审计时间。摘要不得返回 token、请求头、外部渠道 secret、Cloudreve 分享密码、后台备注、审核意见全文、通知正文、内部引用堆栈或异常堆栈。

## 状态、幂等和并发

指南状态流转为 `DRAFT` 到 `PENDING_REVIEW`，`PENDING_REVIEW` 到 `APPROVED`、`REJECTED` 或 `NEEDS_CHANGES`，`REJECTED` 和 `NEEDS_CHANGES` 可重新提交，`APPROVED` 可发布为 `PUBLISHED`，`PUBLISHED` 可下架为 `OFFLINE`，`OFFLINE` 可重新发布、归档或软删除，`DRAFT`、`REJECTED` 和 `NEEDS_CHANGES` 可归档或软删除，`ARCHIVED` 原则上不可恢复，`DELETED` 为软删除终态。

创建指南、修改指南、提交审核、审核、发布、下架、归档、软删除、版本恢复、分类维护、渠道维护、提交反馈和处理反馈支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果。相同幂等键搭配不同请求体返回 `43914`。请求体指纹必须基于结构化 JSON 规范化结果，嵌套对象按字段名递归排序，数组保留顺序，不能依赖浏览器字段顺序或 Java `Map.toString()`。

并发创建相同 slug、相同规则版本、相同分类 slug 或相同渠道 slug 时只能一个成功，其余返回冲突。发布规则版本时必须保证同时只有一个当前规则。公开读取允许读到更新前或更新后的完整版本，不能返回半更新对象。

## 审计要求

必须审计的动作包括创建指南、修改指南、提交审核、审核通过、审核拒绝、要求修改、发布、下架、归档、软删除、恢复版本、创建分类、修改分类、归档分类、创建渠道、修改渠道、启用渠道、禁用渠道、归档渠道、提交反馈、解决反馈、忽略反馈、通知失败、引用降级、搜索摘要写入失败和审计写入失败。

后台写操作必须记录 `reason`。审计字段继承公共契约。审计写入失败时，后台写操作和当前用户反馈写操作不得假装成功，必须返回 `51901` 或 `51900`，并保持业务数据不变。

审计响应必须脱敏，不得返回完整请求体、Authorization、Cookie、外部渠道管理 token、Cloudreve 分享密码、资源下载票据、服务器检测目标、异常堆栈或通知正文。

## 失败降级

公开首页、公开指南列表、详情、搜索、指令索引和外部入口读取在单个资源引用、线路引用或维护人快照不可用时可以局部降级，返回指南主体和 `degraded=true`。指南主数据存储不可用时不能伪造成功。

auth 是当前用户反馈和后台接口强依赖。auth 不可用、超时、用户状态不允许或上下文字段不兼容时，不得使用旧用户上下文继续写入。

profile 是创建维护人快照和发布责任人展示的强依赖。profile 失败时不得创建新的可信维护人快照。已发布指南可使用旧快照公开展示。

notification 强制投递失败时，审核拒绝和要求修改不得改变状态。辅助通知失败时主流程可成功，但必须保留失败摘要和审计记录。

resource 和 server-status 引用失败时不得影响指南正文公开读取，但引用摘要必须标记降级。任何情况下不得返回资源下载 URL、Cloudreve 分享密码、服务器检测目标、后台运维入口或实时服务器状态。

## 验收口径

`guide` API 文档按 `docs/contracts-guide.md` 独立存在，并由 `.local-docs/tests-guide.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`guide` 完成时必须满足以下条件：全部接口按本文档实现；公开接口不泄露后台字段、审计字段、通知结果、资源下载密钥、服务器检测目标和外部渠道管理凭据；当前用户反馈只能写入自己的反馈；后台接口按角色限制；指南分类、文章、版本、规则版本、指令索引、外部入口、反馈、搜索摘要、状态流转、幂等、审计、自检摘要、端口配置和前序模块适配都有自动化测试；`.local-docs/tests-guide.md` 中全部测试用例都有对应自动化验证；未实现时自动化测试必须先失败；实现后 guide 在 `portal-core-service:8134` 中全部测试通过；api-gateway `/api/v1/guides` 路由指向 `8134` 并测试通过；旧 `guide-service:8127` Maven 入口已退役且不得恢复；auth、profile、notification、content、server-status、resource、admin 和 material 回归测试通过；没有修改前序模块稳定接口；没有把 `.local-docs/` 提交到仓库；没有把首页配置、公告文章、真实资源下载、素材投稿、社区讨论、考试判分、白名单审核、实时状态采集、服务器文件管理、容器、终端、节点执行、备份恢复或外部聊天同步塞进 `guide`。
