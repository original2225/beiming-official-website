# 北冥官网 resource API 契约

版本：0.1

## 文档定位

本文档是 `resource` 微服务的正式 API 契约。后续 `admin`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、前端适配和 `ops-control` 只能通过本文档定义的接口读取或管理玩家可见资源，不能直接读取或修改 `resource` 数据库。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、能力点、审计字段、风险等级、通用状态模型和通用错误码均以公共契约为准。本文档只补充 `resource` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

`resource` 适配 `auth`、`profile` 和 `notification`，不要求前序服务反向适配 `resource`。它通过后端入口传入的认证上下文、`/api/v1/auth/me`、`/api/v1/auth/session/verify` 或测试环境 auth stub 读取当前用户、角色、能力点和用户状态。它通过 profile 正式接口或受控 profile stub 判断 `MEMBER_ONLY` 资源的成员资格。资源审核、发布、下架、版本更新需要通知时，只能调用 notification 正式投递接口或受控适配层，不能自建通知主数据、未读数或模板系统。

## 参考口径

resource 的分区参考了 MCSManager 管理面板的实例、文件、终端、监控分层，Cloudreve 的云盘分享和外链分发模式，以及 Nextcloud、Google Drive、GitHub Releases 这类资源库常见的分类、标签、版本、可见范围、分享权限和修订记录。本文档只吸收资源展示、版本管理、分享链接、权限校验和审计这些适合玩家资源库的能力。MCSManager 式实例控制、服务器文件管理、终端命令、节点注册、容器启停、日志流和监控指标全部属于后续 `ops-control` 与 `external-node-executor`，不得进入 `resource`。

## 职责边界

`resource` 负责玩家可见资源下载、资源分类、资源条目、资源版本、下载入口、Cloudreve 分享链接快照、外部链接快照、可见范围、下载权限、资源状态、版本记录、下载记录摘要、资源审计和后台资源元数据管理。

`resource` 不负责注册、登录、会话、角色能力点主数据、成员档案主数据、站内通知主数据、首页内容配置、服务器状态展示、服务器文件浏览、真实文件上传下载、容器管理、虚拟机管理、Minecraft 实例管理、终端命令、日志流、节点密钥、备份恢复和高风险运维审批。

首页资源入口仍由 `content` 的 `RESOURCE_ENTRY` 配置展示位提供。真实资源列表、版本和下载入口由 `resource` 提供。玩家可见 Minecraft 状态仍由 `server-status` 提供。后台文件管理、授权目录操作和 Cloudreve 管理 API 深度同步属于后续 `ops-control` 或 P3 resource 兼容变更，不在 P0 范围内。

## 数据归属

`resource` 拥有以下主数据：资源分类、资源条目、资源版本、下载入口、Cloudreve 分享快照、外部链接快照、下载记录摘要、幂等记录、资源审计日志和运行自检摘要。

`resource` 可以保存创建者、维护人或成员作者的展示快照，例如 `displayName`、`avatarUrl`、`minecraftId`、`memberStatus` 和 `snapshotAt`。快照来自 auth、profile 或服务端可信上下文，不来自浏览器可篡改字段。快照不是 auth 或 profile 主数据，不能用于账号权限或成员资格的最终判断。

Cloudreve P0 只保存公开分享链接快照和安全摘要，不保存真实 Cloudreve 管理 token、内部文件绝对路径、分享密码明文、服务端请求头或云盘管理凭据。后续接入 Cloudreve API 时，必须新增或更新契约、测试文档、自动化测试和失败降级规则，再进入实现。

## 基础路径与认证

公开接口使用 `/api/v1/resources` 前缀。公开读取接口不要求登录，但只能返回 `PUBLISHED`、未下架、未归档、未软删除、处于可见时间范围内且公开字段允许展示的数据。

下载解析接口使用 `POST /api/v1/resources/{resourceId}/versions/{versionId}/download`。该接口会校验可见范围、刷新或读取分享快照、写入下载记录摘要，因此使用 `POST`。`PUBLIC` 资源允许未登录下载。`AUTHENTICATED`、`MEMBER_ONLY` 和 `ADMIN_ONLY` 资源必须按本文档校验身份。

后台接口使用 `/api/v1/resources/admin` 前缀，全部要求登录。后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`。后台写操作要求 `ADMIN` 或 `OWNER`，必须携带 `reason` 并写入审计。`HELPER` 可以读取资源、分类、版本、审计摘要和自检摘要，但不能创建、修改、审核、发布、下架、归档、删除、创建版本、修改版本、启用禁用版本或维护分类。

`GET /api/v1/resources/admin/items/{resourceId}/audit-logs` 和 `GET /api/v1/resources/admin/ops/summary` 只允许 `ADMIN` 或 `OWNER` 访问。

## auth 兼容契约

当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口，也不得解析受限下载入口。

后台写操作里的 `createdBy`、`updatedBy`、`submittedBy`、`reviewedBy`、`publishedBy`、`offlineBy`、`archivedBy`、`deletedBy` 和 `disabledBy` 均来自服务端认证上下文。浏览器请求体传入同名字段时必须忽略或返回字段校验失败。

auth 上下文不可用返回 `46600`，auth 调用超时返回 `46601`，auth 返回字段缺失或枚举不兼容返回 `46602`。`resource` 不能导入 auth 的内存存储、实体、Repository 或测试种子实现。

## profile 兼容契约

`MEMBER_ONLY` 下载解析必须通过 profile 正式接口、后端入口可信成员上下文或测试环境 profile stub 判断当前用户是否有有效成员档案。允许下载的成员状态为 `ACTIVE` 和 `INACTIVE`。`SUSPENDED`、`REMOVED`、`ARCHIVED`、无档案、profile 不可用或 profile 字段不兼容时不得返回下载链接。

创建或修改资源时，如果请求包含 `maintainerMemberId`，`resource` 可以通过 profile 读取维护人公开快照并保存。profile 成员不存在或不可公开返回 `46610`。profile 调用超时返回 `46611`。profile 字段缺失或枚举不兼容返回 `46612`。

客户端不得通过请求体伪造成员展示名、头像、Minecraft ID 或成员状态作为可信字段。profile 不可用时不得创建新的可信成员维护人快照；已发布资源公开读取可以继续返回已保存快照，并在后台详情中标记快照时间。

## notification 兼容契约

审核通过、拒绝、要求修改、发布、下架、归档、软删除和版本更新都可以触发通知。本文档规定审核拒绝、要求修改必须通知资源创建者或维护人；没有可通知用户时跳过通知并在审计中记录 `NO_RECIPIENT_TO_NOTIFY`。发布、下架、归档、软删除和版本更新通知为辅助提醒，通知失败时主流程可以成功，但必须在审计中记录 `notificationStatus=FAILED` 和失败原因。

强制通知失败返回 `46620` 或 `46621`，业务状态不得变化。辅助通知失败不得伪造通知成功，也不得影响公开读取。`resource` 不能自建通知表、未读数、模板和投递记录。

## Cloudreve 和外部下载适配

P0 支持 `CLOUDREVE_SHARE`、`EXTERNAL_URL` 和 `LOCAL_STUB` 三类下载入口。`CLOUDREVE_SHARE` 只保存分享链接快照。`EXTERNAL_URL` 只用于可信外部资源镜像或文档下载，不得保存需要服务端密钥的私有下载 URL。`LOCAL_STUB` 只用于 P0 测试和本地演示，不代表真实文件服务。

下载解析时必须以资源状态、版本状态、下载入口状态、可见范围、时间窗口和依赖健康为准。下载入口过期、禁用、不可用或 Cloudreve 不可用时，公开列表和详情仍可返回资源说明，并标记 `downloadAvailable=false` 与 `degradeReasons`。下载解析返回明确错误，不伪造可下载链接。

如果存在未过期且未禁用的旧分享快照，Cloudreve 当前检查不可用时允许降级返回旧快照，但必须返回 `degraded=true`、`stale=true` 和 `degradeReasons`。如果没有可用旧快照，返回 `46630` 或 `46631`。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `ResourceType` | `CLIENT_PACK`、`RESOURCE_PACK`、`SHADER_PACK`、`MAP_FILE`、`RULE_DOCUMENT`、`ACTIVITY_RESOURCE`、`GUIDE_ATTACHMENT`、`OTHER` | 玩家资源类型。 |
| `ResourceStatus` | `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`PUBLISHED`、`OFFLINE`、`ARCHIVED`、`DELETED` | 资源主体状态。`PUBLISHED` 是公开或按可见范围可访问状态。 |
| `ResourceVisibility` | `PUBLIC`、`AUTHENTICATED`、`MEMBER_ONLY`、`ADMIN_ONLY` | 下载可见范围。 |
| `DownloadProvider` | `CLOUDREVE_SHARE`、`EXTERNAL_URL`、`LOCAL_STUB` | 下载入口提供方。 |
| `DownloadEntryStatus` | `ACTIVE`、`EXPIRED`、`DISABLED`、`UNAVAILABLE` | 下载入口状态。 |
| `ResourceVersionStatus` | `ENABLED`、`DISABLED`、`ARCHIVED` | 资源版本状态。 |
| `CloudreveSyncStatus` | `NOT_CONFIGURED`、`LINK_ONLY`、`SYNCED`、`FAILED` | Cloudreve 适配状态。P0 默认为 `LINK_ONLY` 或 `NOT_CONFIGURED`。 |
| `DownloadRecordResult` | `SUCCESS`、`DENIED`、`FAILED`、`DEGRADED` | 下载解析记录结果。 |
| `ResourceAuditResult` | `SUCCESS`、`FAILED` | resource 审计结果。 |

## 通用对象

### ResourceCategory

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

### PublicResourceSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `resourceId` | string | 是 | 资源 ID。 |
| `slug` | string | 是 | 公开路径标识，同一未软删除资源中唯一。 |
| `type` | string | 是 | `ResourceType`。 |
| `title` | string | 是 | 标题，2 到 120 位。 |
| `summary` | string 或 null | 是 | 摘要，最多 300 位。 |
| `coverUrl` | string 或 null | 是 | 封面 URL，只允许 http、https 或站内资源路径。 |
| `category` | ResourceCategory 或 null | 是 | 分类公开摘要。 |
| `tags` | string[] | 是 | 标签，最多 20 个。 |
| `visibility` | string | 是 | 可见范围。 |
| `latestVersion` | PublicResourceVersion 或 null | 是 | 最新可见版本。 |
| `downloadAvailable` | boolean | 是 | 当前是否存在可解析下载入口。 |
| `degraded` | boolean | 是 | 是否存在降级。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |
| `publishedAt` | string | 是 | 发布时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### PublicResourceDetail

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `resourceId` | string | 是 | 资源 ID。 |
| `slug` | string | 是 | 公开路径标识。 |
| `type` | string | 是 | 资源类型。 |
| `title` | string | 是 | 标题。 |
| `summary` | string 或 null | 是 | 摘要。 |
| `description` | string | 是 | 资源说明，公开只返回已发布说明。 |
| `coverUrl` | string 或 null | 是 | 封面 URL。 |
| `category` | ResourceCategory 或 null | 是 | 分类摘要。 |
| `tags` | string[] | 是 | 标签。 |
| `visibility` | string | 是 | 可见范围。 |
| `maintainerSnapshot` | ResourceMaintainerSnapshot 或 null | 是 | 维护人公开快照。 |
| `versions` | PublicResourceVersion[] | 是 | 可见版本摘要。 |
| `downloadAvailable` | boolean | 是 | 是否有可解析入口。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |
| `publishedAt` | string | 是 | 发布时间。 |
| `visibleFrom` | string 或 null | 是 | 可见开始时间。 |
| `visibleUntil` | string 或 null | 是 | 可见结束时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### PublicResourceVersion

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `versionId` | string | 是 | 版本 ID。 |
| `versionName` | string | 是 | 版本号或版本名，1 到 60 位，同一资源内唯一。 |
| `title` | string 或 null | 是 | 版本标题。 |
| `changelog` | string 或 null | 是 | 公开更新说明，最多 2000 位。 |
| `minecraftVersions` | string[] | 是 | 兼容 Minecraft 版本。 |
| `loader` | string 或 null | 是 | 加载器，例如 `Fabric`、`Forge`、`NeoForge`、`Vanilla`。 |
| `fileSizeBytes` | integer 或 null | 是 | 文件大小，未知为 `null`。 |
| `checksumSha256` | string 或 null | 是 | SHA-256 校验值，未知为 `null`。 |
| `downloadEntryId` | string 或 null | 是 | 可用下载入口 ID。 |
| `downloadAvailable` | boolean | 是 | 此版本是否可解析下载。 |
| `releasedAt` | string 或 null | 是 | 版本发布时间。 |
| `createdAt` | string | 是 | 创建时间。 |

### AdminResourceItem

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `resourceId` | string | 是 | 资源 ID。 |
| `status` | string | 是 | `ResourceStatus`。 |
| `type` | string | 是 | `ResourceType`。 |
| `visibility` | string | 是 | `ResourceVisibility`。 |
| `slug` | string | 是 | slug。 |
| `title` | string | 是 | 标题。 |
| `summary` | string 或 null | 是 | 摘要。 |
| `description` | string | 是 | 草稿或当前说明。 |
| `coverUrl` | string 或 null | 是 | 封面 URL。 |
| `categoryId` | string 或 null | 是 | 分类 ID。 |
| `tags` | string[] | 是 | 标签。 |
| `maintainerMemberId` | string 或 null | 是 | 维护人成员 ID。 |
| `maintainerSnapshot` | ResourceMaintainerSnapshot 或 null | 是 | 维护人快照。 |
| `adminNote` | string 或 null | 是 | 后台备注，不得出现在公开接口。 |
| `reviewOpinion` | string 或 null | 是 | 最近审核意见，不得出现在公开接口。 |
| `notificationStatus` | string 或 null | 是 | 最近通知结果摘要。 |
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

### AdminResourceVersion

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `versionId` | string | 是 | 版本 ID。 |
| `resourceId` | string | 是 | 资源 ID。 |
| `status` | string | 是 | `ResourceVersionStatus`。 |
| `versionName` | string | 是 | 版本名，同一资源内唯一。 |
| `title` | string 或 null | 是 | 版本标题。 |
| `changelog` | string 或 null | 是 | 更新说明。 |
| `minecraftVersions` | string[] | 是 | 兼容 Minecraft 版本。 |
| `loader` | string 或 null | 是 | 加载器。 |
| `fileSizeBytes` | integer 或 null | 是 | 文件大小。 |
| `checksumSha256` | string 或 null | 是 | SHA-256。 |
| `downloadEntry` | ResourceDownloadEntry | 是 | 下载入口。 |
| `releasedAt` | string 或 null | 是 | 版本发布时间。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ResourceDownloadEntry

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `downloadEntryId` | string | 是 | 下载入口 ID。 |
| `provider` | string | 是 | `DownloadProvider`。 |
| `status` | string | 是 | `DownloadEntryStatus`。 |
| `displayName` | string | 是 | 下载入口展示名。 |
| `shareSnapshot` | CloudreveShareSnapshot 或 ExternalLinkSnapshot | 是 | 安全快照。 |
| `lastCheckedAt` | string 或 null | 是 | 最近检查时间。 |
| `expiresAt` | string 或 null | 是 | 入口过期时间。 |
| `adminNote` | string 或 null | 是 | 后台备注，不得公开。 |

### CloudreveShareSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `provider` | string | 是 | 固定为 `CLOUDREVE_SHARE`。 |
| `shareUrl` | string | 是 | Cloudreve 公开分享 URL。 |
| `shareId` | string 或 null | 是 | 分享 ID 或安全摘要。 |
| `maskedPasswordRequired` | boolean | 是 | 是否需要提取码。不得返回提取码明文。 |
| `expiresAt` | string 或 null | 是 | 分享过期时间。 |
| `status` | string | 是 | `DownloadEntryStatus`。 |
| `lastCheckedAt` | string 或 null | 是 | 最近检查时间。 |
| `syncStatus` | string | 是 | `CloudreveSyncStatus`。 |

### ExternalLinkSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `provider` | string | 是 | 固定为 `EXTERNAL_URL` 或 `LOCAL_STUB`。 |
| `url` | string | 是 | 公开下载 URL 或测试 stub URL。 |
| `status` | string | 是 | `DownloadEntryStatus`。 |
| `expiresAt` | string 或 null | 是 | 过期时间。 |
| `lastCheckedAt` | string 或 null | 是 | 最近检查时间。 |

### ResourceDownloadTicket

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ticketId` | string | 是 | 下载解析记录 ID。 |
| `resourceId` | string | 是 | 资源 ID。 |
| `versionId` | string | 是 | 版本 ID。 |
| `downloadEntryId` | string | 是 | 下载入口 ID。 |
| `provider` | string | 是 | 下载提供方。 |
| `downloadUrl` | string | 是 | 前端可打开的下载或分享 URL。 |
| `expiresAt` | string 或 null | 是 | 此次解析结果过期时间。 |
| `degraded` | boolean | 是 | 是否降级返回。 |
| `stale` | boolean | 是 | 是否使用旧快照。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |
| `createdAt` | string | 是 | 解析时间。 |

### ResourceDownloadRecordSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ticketId` | string | 是 | 下载解析记录 ID。 |
| `resourceId` | string | 是 | 资源 ID。 |
| `versionId` | string | 是 | 版本 ID。 |
| `downloadEntryId` | string 或 null | 是 | 下载入口 ID。 |
| `actorUserId` | string 或 null | 是 | 登录用户 ID，匿名下载为 `null`。 |
| `anonymous` | boolean | 是 | 是否匿名下载。 |
| `clientLabel` | string 或 null | 是 | 客户端标识摘要。 |
| `provider` | string 或 null | 是 | 下载提供方。 |
| `result` | string | 是 | `DownloadRecordResult`。 |
| `degraded` | boolean | 是 | 是否降级返回。 |
| `requestId` | string | 是 | 请求编号。 |
| `createdAt` | string | 是 | 记录时间。 |

### ResourceMaintainerSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `memberId` | string | 是 | profile 成员 ID。 |
| `displayName` | string | 是 | 成员展示名快照。 |
| `avatarUrl` | string 或 null | 是 | 头像快照。 |
| `minecraftId` | string 或 null | 是 | Minecraft ID 快照。 |
| `memberStatus` | string | 是 | 成员状态快照。 |
| `snapshotAt` | string | 是 | 快照时间。 |

### ResourceAuditLog

审计字段继承公共契约，允许补充 `resourceId`、`versionId`、`categoryId`、`downloadEntryId`、`ticketId`、`idempotencyKey`、`stateFrom`、`stateTo`、`notificationStatus`、`profileSnapshotStatus`、`cloudreveStatus` 和 `downloadRecordResult`。审计日志不得通过 resource API 删除。

### ResourceOpsSummary

自检摘要至少包含 `service`、`storageMode`、`authMode`、`profileMode`、`notificationMode`、`cloudreveMode`、`resourcesTotal`、`publishedResourcesTotal`、`versionsTotal`、`categoriesTotal`、`downloadEntriesTotal`、`downloadRecordsTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastAuditAt`、`lastDownloadAt`、`warnings` 和 `productionGaps`。不得返回 token、请求头、后台备注、审核意见、分享密码、Cloudreve 管理凭据、内部文件路径或审计原因全文。

## resource 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `43600` | 404 | 资源不存在，或公开接口不可访问该资源。 |
| `43601` | 404 | 资源分类不存在。 |
| `43602` | 404 | 资源版本不存在。 |
| `43603` | 404 | 下载入口不存在或不可访问。 |
| `43610` | 409 | 资源状态不允许当前操作。 |
| `43611` | 409 | slug、版本号、分类 slug 或下载入口冲突。 |
| `43612` | 409 | 幂等键请求指纹冲突。 |
| `43613` | 409 | 下载入口已过期、禁用或不可用。 |
| `43614` | 409 | 资源发布条件不满足。 |
| `43615` | 409 | 分类仍被未归档资源引用，不能归档。 |
| `46600` | 502 | auth 认证上下文不可用。 |
| `46601` | 504 | auth 认证上下文调用超时。 |
| `46602` | 502 | auth 认证上下文不兼容 resource 契约。 |
| `46610` | 502 | profile 成员状态或成员快照不可用。 |
| `46611` | 504 | profile 成员状态或成员快照调用超时。 |
| `46612` | 502 | profile 成员状态或成员快照不兼容 resource 契约。 |
| `46620` | 502 | notification 强制投递不可用。 |
| `46621` | 504 | notification 强制投递超时。 |
| `46630` | 502 | Cloudreve 分享链接不可用。 |
| `46631` | 504 | Cloudreve 分享链接调用超时。 |
| `51600` | 500 | resource 内部错误。 |
| `51601` | 500 | resource 审计写入失败。 |
| `51602` | 500 | resource 下载记录写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误和通用服务端错误优先使用公共错误码。resource 内创建资源、创建版本、创建分类和下载解析的幂等请求指纹冲突统一返回 `43612`。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开资源列表 | GET | `/api/v1/resources` | 否 | 无 | LOW |
| 公开资源详情 | GET | `/api/v1/resources/{resourceId}` | 否 | 无 | LOW |
| 公开 slug 资源详情 | GET | `/api/v1/resources/by-slug/{slug}` | 否 | 无 | LOW |
| 公开分类列表 | GET | `/api/v1/resources/categories` | 否 | 无 | LOW |
| 公开资源版本列表 | GET | `/api/v1/resources/{resourceId}/versions` | 否 | 无 | LOW |
| 下载入口解析 | POST | `/api/v1/resources/{resourceId}/versions/{versionId}/download` | 视可见范围 | 当前用户或公开 | LOW |
| 后台资源列表 | GET | `/api/v1/resources/admin/items` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台资源详情 | GET | `/api/v1/resources/admin/items/{resourceId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建资源 | POST | `/api/v1/resources/admin/items` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改资源 | PATCH | `/api/v1/resources/admin/items/{resourceId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 提交审核 | PATCH | `/api/v1/resources/admin/items/{resourceId}/submit-review` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核通过 | PATCH | `/api/v1/resources/admin/items/{resourceId}/approve` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/resources/admin/items/{resourceId}/reject` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 要求修改 | PATCH | `/api/v1/resources/admin/items/{resourceId}/request-changes` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 发布资源 | PATCH | `/api/v1/resources/admin/items/{resourceId}/publish` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 下架资源 | PATCH | `/api/v1/resources/admin/items/{resourceId}/offline` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档资源 | PATCH | `/api/v1/resources/admin/items/{resourceId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 软删除资源 | PATCH | `/api/v1/resources/admin/items/{resourceId}/delete` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台版本列表 | GET | `/api/v1/resources/admin/items/{resourceId}/versions` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建版本 | POST | `/api/v1/resources/admin/items/{resourceId}/versions` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改版本 | PATCH | `/api/v1/resources/admin/items/{resourceId}/versions/{versionId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 禁用版本 | PATCH | `/api/v1/resources/admin/items/{resourceId}/versions/{versionId}/disable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 启用版本 | PATCH | `/api/v1/resources/admin/items/{resourceId}/versions/{versionId}/enable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台分类列表 | GET | `/api/v1/resources/admin/categories` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建分类 | POST | `/api/v1/resources/admin/categories` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改分类 | PATCH | `/api/v1/resources/admin/categories/{categoryId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档分类 | PATCH | `/api/v1/resources/admin/categories/{categoryId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 资源审计列表 | GET | `/api/v1/resources/admin/items/{resourceId}/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| resource 自检摘要 | GET | `/api/v1/resources/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 公开资源接口

### 公开资源列表

`GET /api/v1/resources`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `type` | string | 否 | 任一 `ResourceType`。 |
| `categoryId` | string | 否 | 分类 ID。 |
| `tag` | string | 否 | 标签，最多 40 位。 |
| `keyword` | string | 否 | 匹配标题、摘要和说明，最多 80 位。 |
| `visibility` | string | 否 | 只允许 `PUBLIC`、`AUTHENTICATED`、`MEMBER_ONLY`。公开列表不得用该参数查询 `ADMIN_ONLY`。 |
| `minecraftVersion` | string | 否 | 兼容 Minecraft 版本，最多 40 位。 |
| `sort` | string | 否 | 允许 `publishedAt_desc`、`updatedAt_desc`、`title_asc`、`downloadUpdatedAt_desc`。默认 `publishedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `PublicResourceSummary[]`。

业务规则：只返回 `PUBLISHED`、未软删除、未归档、未下架、当前时间处于 `visibleFrom` 和 `visibleUntil` 范围内的资源。公开列表可以展示 `AUTHENTICATED` 和 `MEMBER_ONLY` 资源摘要，但不得返回真实下载 URL。`ADMIN_ONLY` 资源永远不进入公开列表。草稿、待审核、已通过但未发布、已拒绝、需修改、下架、归档和软删除资源不得出现。

字段隔离：列表不得返回 `adminNote`、`reviewOpinion`、审计字段、幂等键、Cloudreve 管理路径、分享密码、内部文件 ID、内部路径、请求头或 token。

### 公开资源详情

`GET /api/v1/resources/{resourceId}`

成功响应 HTTP `200`，`data` 为 `PublicResourceDetail`。资源不存在或不可公开返回 `43600`。

业务规则：只允许读取公开可见资源详情。详情可以展示所有可见版本摘要，但不得返回后台备注、审核意见、内部下载配置或真实下载 URL。下载入口只能通过下载解析接口获得。

### 公开 slug 资源详情

`GET /api/v1/resources/by-slug/{slug}`

成功响应 HTTP `200`，`data` 为 `PublicResourceDetail`。slug 不存在或资源不可公开返回 `43600`。

业务规则：供前端稳定路由使用，返回语义与 ID 详情一致。

### 公开分类列表

`GET /api/v1/resources/categories`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `type` | string | 否 | 资源类型筛选。 |

成功响应 HTTP `200`，`data.items` 为 `ResourceCategory[]`。

业务规则：只返回 `enabled=true` 且 `archived=false` 的分类。分类按 `sortOrder`、`name` 稳定排序。公开分类不得返回审计、后台备注或引用中的不可公开资源数量。

### 公开资源版本列表

`GET /api/v1/resources/{resourceId}/versions`

成功响应 HTTP `200`，`data.items` 为 `PublicResourceVersion[]`。

业务规则：资源必须公开可见。只返回 `ENABLED` 且有关联下载入口的版本。版本按 `releasedAt_desc`、`createdAt_desc` 稳定排序。资源不存在或不可公开返回 `43600`。

### 下载入口解析

`POST /api/v1/resources/{resourceId}/versions/{versionId}/download`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `downloadEntryId` | string | 否 | 指定下载入口，不传时使用版本默认入口。 |
| `clientLabel` | string | 否 | 客户端标识，最多 80 位，用于下载记录摘要。 |
| `idempotencyKey` | string | 否 | 下载解析重试幂等键，10 分钟内有效。 |

成功响应 HTTP `200`，`data` 为 `ResourceDownloadTicket`。

权限规则：`PUBLIC` 资源允许未登录解析。`AUTHENTICATED` 资源必须登录，未登录返回 `41000`。`MEMBER_ONLY` 资源必须登录且 profile 判断成员状态允许下载，非成员返回 `42001` 或 `42002`。`ADMIN_ONLY` 资源只允许 `ADMIN` 或 `OWNER`，普通用户返回 `42001`，公开接口不得泄露后台资源细节。

业务规则：资源必须为 `PUBLISHED`，版本必须为 `ENABLED`，下载入口必须为 `ACTIVE` 且未过期。解析成功写入 `ResourceDownloadRecordSummary`，至少记录用户或匿名摘要、资源、版本、入口、provider、结果、请求编号和时间。受限下载拒绝、Cloudreve 降级、Cloudreve 失败和下载记录写入失败也必须留下审计或下载记录摘要。下载记录写入失败返回 `51602`，不得假装成功。

幂等规则：同一访问者、同一资源版本、同一 `idempotencyKey` 和同一请求体重复提交时返回同一个 `ticketId` 和下载结果，不重复增加下载记录。相同幂等键搭配不同请求体返回 `43612`。幂等指纹必须使用字段名排序后的稳定 JSON 语义，嵌套对象和数组也必须稳定计算，不能依赖 Java `Map.toString()` 或浏览器字段顺序。下载解析幂等记录有效期为 10 分钟，创建类幂等记录有效期为 24 小时。

失败降级：Cloudreve 不可用时，如果可使用旧快照，返回成功但 `degraded=true`、`stale=true`。没有可用旧快照时返回 `46630` 或 `46631`。过期、禁用或不可用入口返回 `43613`。

## 后台资源接口

### 后台资源列表

`GET /api/v1/resources/admin/items`

查询参数包括 `page`、`pageSize`、`keyword`、`type`、`status`、`visibility`、`categoryId`、`tag`、`createdBy`、`maintainerMemberId` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`publishedAt_desc`、`title_asc`。成功响应 HTTP `200`，分页 `items` 为 `AdminResourceItem[]`。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。未登录返回 `41000`。

分页规则：分页接口必须按过滤后的全集计算 `total`，按请求的 `page` 和 `pageSize` 返回切片。空页返回空数组，不得回退第一页。默认 `page=1`、`pageSize=20`。公开资源列表默认按 `publishedAt_desc`，后台资源列表默认按 `updatedAt_desc`，审计列表默认按 `createdAt_desc`。排序字段相同必须追加稳定 ID 排序，避免翻页重复或遗漏。

### 后台资源详情

`GET /api/v1/resources/admin/items/{resourceId}`

成功响应 HTTP `200`，`data` 为 `AdminResourceItem`，允许附带版本、下载入口和审计摘要。资源不存在返回 `43600`。

### 创建资源

`POST /api/v1/resources/admin/items`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `type` | string | 是 | 任一 `ResourceType`。 |
| `visibility` | string | 是 | 任一 `ResourceVisibility`。 |
| `slug` | string | 是 | 3 到 120 位，只允许小写字母、数字、短横线和斜杠，不能以斜杠结尾。 |
| `title` | string | 是 | 2 到 120 位。 |
| `summary` | string 或 null | 否 | 最多 300 位。 |
| `description` | string | 是 | 1 到 10000 位。 |
| `coverUrl` | string 或 null | 否 | http、https 或站内资源路径，最多 500 位。 |
| `categoryId` | string 或 null | 否 | 分类 ID。 |
| `tags` | string[] | 否 | 最多 20 个，每个最多 40 位。 |
| `maintainerMemberId` | string 或 null | 否 | 维护人成员 ID。 |
| `visibleFrom` | string 或 null | 否 | 可见开始时间。 |
| `visibleUntil` | string 或 null | 否 | 可见结束时间，必须晚于 `visibleFrom`。 |
| `adminNote` | string 或 null | 否 | 后台备注，最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `AdminResourceItem`，默认状态为 `DRAFT`。

业务规则：slug 在未软删除资源中唯一，冲突返回 `43611`。分类不存在返回 `43601`。维护人快照由 profile 适配层获取。`ADMIN_ONLY` 资源允许后台创建，但永远不进入公开列表。审计失败返回 `51601`，不得创建资源。

幂等规则：同一操作者、同一 `idempotencyKey`、同一请求体重复提交时返回同一资源。相同幂等键搭配不同请求体返回 `43612`。

### 修改资源

`PATCH /api/v1/resources/admin/items/{resourceId}`

请求字段同创建资源，除 `reason` 必填外其余字段按需修改。

成功响应 HTTP `200`，`data` 为更新后的 `AdminResourceItem`。

业务规则：资源不存在返回 `43600`。`ARCHIVED` 和 `DELETED` 资源不允许修改主体字段，返回 `43610`。slug 冲突返回 `43611`。修改已发布资源必须保证公开读取不会返回半更新状态。审计失败时不得改变资源。

### 提交审核

`PATCH /api/v1/resources/admin/items/{resourceId}/submit-review`

请求字段只有必填 `reason`。`DRAFT`、`REJECTED` 和 `NEEDS_CHANGES` 可流转为 `PENDING_REVIEW`。重复提交 `PENDING_REVIEW` 返回成功，保持幂等，不重复写审计。其他状态返回 `43610`。

### 审核通过

`PATCH /api/v1/resources/admin/items/{resourceId}/approve`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reviewOpinion` | string | 是 | 1 到 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |

`PENDING_REVIEW` 可流转为 `APPROVED`。重复审核已 `APPROVED` 资源返回成功，保持幂等，不重复写审计。其他状态返回 `43610`。

### 审核拒绝

`PATCH /api/v1/resources/admin/items/{resourceId}/reject`

请求字段同审核通过。`PENDING_REVIEW` 可流转为 `REJECTED`。审核拒绝必须通知创建者或维护人；强制通知失败时状态不变化并返回 `46620` 或 `46621`。重复拒绝保持幂等。

### 要求修改

`PATCH /api/v1/resources/admin/items/{resourceId}/request-changes`

请求字段同审核通过。`PENDING_REVIEW` 可流转为 `NEEDS_CHANGES`。要求修改必须通知创建者或维护人；强制通知失败时状态不变化。重复要求修改保持幂等。

### 发布资源

`PATCH /api/v1/resources/admin/items/{resourceId}/publish`

请求字段只有必填 `reason`。

业务规则：`APPROVED` 和 `OFFLINE` 可发布为 `PUBLISHED`，并写入或更新 `publishedAt`。发布前必须至少存在一个 `ENABLED` 版本，且该版本存在 `ACTIVE` 下载入口。否则返回 `43614`。重复发布 `PUBLISHED` 返回成功，保持幂等，不重复写审计。辅助通知失败时主流程可成功，但必须记录通知失败摘要。

### 下架资源

`PATCH /api/v1/resources/admin/items/{resourceId}/offline`

请求字段只有必填 `reason`。`PUBLISHED` 可流转为 `OFFLINE`，并从公开接口消失。重复下架 `OFFLINE` 返回成功，保持幂等，不重复写审计。`DRAFT`、`PENDING_REVIEW`、`ARCHIVED` 和 `DELETED` 返回 `43610`。

### 归档资源

`PATCH /api/v1/resources/admin/items/{resourceId}/archive`

请求字段只有必填 `reason`。`DRAFT`、`REJECTED`、`NEEDS_CHANGES` 和 `OFFLINE` 可流转为 `ARCHIVED`。已发布资源必须先下架再归档，直接归档返回 `43610`。重复归档返回成功，保持幂等。

### 软删除资源

`PATCH /api/v1/resources/admin/items/{resourceId}/delete`

请求字段只有必填 `reason`。只做软删除，状态为 `DELETED`，写入 `deletedAt`。已发布资源必须先下架再软删除，直接删除返回 `43610`。重复软删除返回成功，保持幂等。P0 不提供真实删除接口。

## 后台版本接口

### 后台版本列表

`GET /api/v1/resources/admin/items/{resourceId}/versions`

成功响应 HTTP `200`，`data.items` 为 `AdminResourceVersion[]`。资源不存在返回 `43600`。

### 创建版本

`POST /api/v1/resources/admin/items/{resourceId}/versions`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `versionName` | string | 是 | 1 到 60 位，同一资源内唯一。 |
| `title` | string 或 null | 否 | 最多 120 位。 |
| `changelog` | string 或 null | 否 | 最多 2000 位。 |
| `minecraftVersions` | string[] | 否 | 最多 20 个。 |
| `loader` | string 或 null | 否 | 最多 40 位。 |
| `fileSizeBytes` | integer 或 null | 否 | 大于等于 0。 |
| `checksumSha256` | string 或 null | 否 | 64 位小写十六进制。 |
| `downloadEntry` | object | 是 | 下载入口配置。 |
| `releasedAt` | string 或 null | 否 | 发布时间。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

`downloadEntry` 字段至少包含 `provider`、`displayName`、`url` 或 `shareUrl`、`expiresAt`、`status` 和 `adminNote`。成功响应 HTTP `201`，`data` 为 `AdminResourceVersion`，默认 `status=ENABLED`。

业务规则：资源不存在返回 `43600`。`ARCHIVED` 和 `DELETED` 资源不能创建版本。版本号冲突返回 `43611`。下载入口字段非法返回 `40001`。审计失败不得创建版本。

### 修改版本

`PATCH /api/v1/resources/admin/items/{resourceId}/versions/{versionId}`

请求字段同创建版本，除 `reason` 必填外其余字段按需修改。

成功响应 HTTP `200`，`data` 为更新后的 `AdminResourceVersion`。版本不存在返回 `43602`。版本号冲突返回 `43611`。`ARCHIVED` 版本不可修改下载入口，返回 `43610`。

### 禁用版本

`PATCH /api/v1/resources/admin/items/{resourceId}/versions/{versionId}/disable`

请求字段只有必填 `reason`。`ENABLED` 可流转为 `DISABLED`。重复禁用返回成功，保持幂等。禁用后公开版本列表和下载解析不再返回该版本。

### 启用版本

`PATCH /api/v1/resources/admin/items/{resourceId}/versions/{versionId}/enable`

请求字段只有必填 `reason`。`DISABLED` 可流转为 `ENABLED`。重复启用返回成功，保持幂等。启用前必须存在 `ACTIVE` 且未过期下载入口，否则返回 `43613`。

## 后台分类接口

### 后台分类列表

`GET /api/v1/resources/admin/categories`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `includeArchived` | boolean | 否 | 默认 `true`。 |
| `enabled` | boolean | 否 | 是否启用。 |
| `keyword` | string | 否 | 匹配名称或 slug，最多 80 位。 |

成功响应 HTTP `200`，`data.items` 为 `ResourceCategory[]`，按 `sortOrder`、`name` 稳定排序。

### 创建分类

`POST /api/v1/resources/admin/categories`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `name` | string | 是 | 2 到 40 位，同一未归档分类中唯一。 |
| `slug` | string | 是 | 2 到 80 位，只允许小写字母、数字和短横线。 |
| `description` | string 或 null | 否 | 最多 200 位。 |
| `icon` | string 或 null | 否 | 最多 120 位。 |
| `sortOrder` | integer | 否 | 默认 `100`。 |
| `enabled` | boolean | 否 | 默认 `true`。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `ResourceCategory`。分类名称或 slug 冲突返回 `43611`。审计失败不得创建分类。

### 修改分类

`PATCH /api/v1/resources/admin/categories/{categoryId}`

请求字段同创建分类，除 `reason` 必填外其余字段按需修改。

成功响应 HTTP `200`，`data` 为更新后的 `ResourceCategory`。分类不存在返回 `43601`。已归档分类不得通过修改接口取消归档。

### 归档分类

`PATCH /api/v1/resources/admin/categories/{categoryId}/archive`

请求字段只有必填 `reason`。仍被未归档、未软删除资源引用的分类不能归档，返回 `43615`。重复归档保持幂等，不重复写审计。归档后公开分类列表不再返回该分类。

## 审计和自检接口

### 资源审计列表

`GET /api/v1/resources/admin/items/{resourceId}/audit-logs`

查询参数包括 `page`、`pageSize`、`action`、`actorUserId`、`result`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`。成功响应分页 `items` 为 `ResourceAuditLog[]`。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 可读后台资源详情，但不能读取审计列表。资源不存在返回 `43600`。审计日志不得通过 resource API 删除。

### resource 自检摘要

`GET /api/v1/resources/admin/ops/summary`

成功响应 HTTP `200`，`data` 为 `ResourceOpsSummary`。

业务规则：自检摘要用于后台确认 `resource` 当前运行模式、数据规模、适配层状态和生产化缺口。P0 可返回 `storageMode=IN_MEMORY`、`authMode=TEST_STUB`、`profileMode=TEST_STUB`、`notificationMode=TEST_STUB` 和 `cloudreveMode=LINK_ONLY_STUB`。`productionGaps` 至少说明真实持久化、真实认证适配、真实 profile 适配、真实 notification 适配和 Cloudreve API 深度同步是否未启用。摘要不得返回 token、请求头、分享密码、后台备注、内部路径或审计原因全文。读取失败返回 `51600`，不得伪造健康。

## 状态、幂等和并发

资源状态流转为 `DRAFT` 到 `PENDING_REVIEW`，`PENDING_REVIEW` 到 `APPROVED`、`REJECTED` 或 `NEEDS_CHANGES`，`REJECTED` 和 `NEEDS_CHANGES` 可重新提交审核，`APPROVED` 可发布为 `PUBLISHED`，`PUBLISHED` 可下架为 `OFFLINE`，`OFFLINE` 可重新发布或归档，`DRAFT`、`REJECTED`、`NEEDS_CHANGES` 和 `OFFLINE` 可归档或软删除，`ARCHIVED` 原则上不可恢复，`DELETED` 为软删除终态。

版本状态流转为 `ENABLED` 到 `DISABLED`，`DISABLED` 到 `ENABLED`。`ARCHIVED` 保留给后续迁移或长期留存，P0 不提供版本归档接口。资源发布要求至少一个可用版本和可用下载入口。

下载入口状态由后台配置和适配器检查共同决定。`ACTIVE` 且未过期才允许下载解析。`EXPIRED`、`DISABLED` 和 `UNAVAILABLE` 不允许解析。Cloudreve 当前检查失败但旧快照可用时可以降级返回旧快照。

创建资源、创建版本、创建分类和下载解析支持 `idempotencyKey`。同一操作者或访问者、同一幂等键、同一请求体重复提交时返回同一结果。相同幂等键搭配不同请求体返回 `43612`。幂等请求体指纹必须使用字段名排序后的稳定 JSON 语义计算，不能依赖浏览器字段顺序或 Java `Map.toString()`。

并发创建相同资源 slug、分类 slug、分类名称或同一资源版本名时只能一个请求成功，其余返回冲突。公开读取接口允许读到更新前或更新后的完整状态，但不能返回半更新对象。

请求校验必须优先于业务写入。枚举、时间、URL、布尔值、长度、数字范围和可信字段都必须返回 `40001` 或对应公共错误，不得落入 `51600`。后台写接口请求体出现 `createdBy`、`updatedBy`、`submittedBy`、`reviewedBy`、`publishedBy`、`offlineBy`、`archivedBy`、`deletedBy`、`disabledBy`、`maintainerSnapshot`、`downloadRecordsTotal`、`auditsTotal` 等服务端字段时，必须返回字段校验失败或忽略并以服务端上下文为准；生产实现推荐返回字段级 `errors`，P0 至少不得信任这些字段。

## 审计要求

必须审计的动作包括创建资源、修改资源、提交审核、审核通过、审核拒绝、要求修改、发布、下架、归档、软删除、创建版本、修改版本、禁用版本、启用版本、创建分类、修改分类、归档分类、下载解析失败、受限下载拒绝、Cloudreve 降级和下载记录写入失败。

后台写操作必须记录调用者、`reason`、操作前状态、操作后状态、请求编号和结果。审计字段继承公共契约。审计写入失败时，后台写操作不得假装成功，必须返回 `51601` 或 `51600`，并保持业务数据不变。

公开读取不强制写审计。下载解析必须写下载记录摘要。受限下载拒绝可以写低风险审计或下载拒绝记录，但不得暴露敏感资源是否存在给无权限用户。

## 失败降级

公开列表、详情、分类和版本接口在 Cloudreve 不可用时不得整页失败。接口应返回资源说明、`downloadAvailable=false`、`degraded=true` 和明确 `degradeReasons`。资源主数据存储不可用时不能伪造成功。

auth 认证上下文失败时，后台接口和受限下载解析不得使用旧用户上下文继续写入。profile 失败时，`MEMBER_ONLY` 下载解析不得放行；创建维护人快照失败时不得保存伪造快照。notification 强制投递失败时，审核拒绝和要求修改不得改变状态；辅助通知失败时主流程可成功，但必须保留失败摘要和审计记录。

Cloudreve 分享链接不可用时，下载解析可以在旧快照仍合法时降级返回旧快照；否则返回依赖错误。任何情况下不得返回分享密码明文、Cloudreve 管理 token、内部文件路径或需要服务端密钥的私有下载 URL。

## 验收口径

`resource` API 文档按 `docs/contracts-resource.md` 独立存在，并由 `.local-docs/tests-resource.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`resource` 完成时必须满足以下条件：全部接口按本文档实现；公开接口不泄露后台字段、Cloudreve 管理字段、分享密码、内部路径、审计字段和运维入口；后台接口按角色限制；受限下载按 `PUBLIC`、`AUTHENTICATED`、`MEMBER_ONLY` 和 `ADMIN_ONLY` 正确校验；Cloudreve 分享链接失败可测试降级；分类、资源、版本、下载入口、状态流转、幂等、审计、自检摘要、requestId、端口配置和前序服务适配都有自动化测试；`.local-docs/tests-resource.md` 中全部测试用例都有对应自动化验证；未实现时自动化测试必须先失败；实现后 `resource` 全部测试通过；`auth`、`profile`、`notification`、`content` 和 `server-status` 前序服务回归测试通过；没有修改前序服务稳定接口；没有把后台文件管理、容器、终端、日志流、节点注册、external-node-executor、真实服务器操作或 ops-control 能力塞进 `resource`。
