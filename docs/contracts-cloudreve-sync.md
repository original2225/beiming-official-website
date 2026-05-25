# 北冥官网 cloudreve-sync API 契约

版本：0.1

## 文档定位

本文档是 `cloudreve-sync` 微服务的正式 API 契约。`cloudreve-sync` 负责 Cloudreve API 深度接入、provider 配置摘要、目录同步任务、文件元数据快照、分享链接解析、分享状态探测、失效降级、幂等记录和同步审计。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `cloudreve-sync` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、同步任务流转、失败降级、审计和验收口径。

`cloudreve-sync` 不是玩家资源服务，不拥有资源分类、资源条目、资源版本、可见范围或下载权限主数据。`resource` 仍然是玩家可见资源下载的唯一主数据服务。`cloudreve-sync` 不是后台服务器文件管理服务，不执行宿主机文件浏览、上传、下载、重命名、移动、删除、编辑、终端命令或备份恢复。后台服务器文件管理仍归 `ops-control` 和 `node-daemon`。

本文档参考 Cloudreve v4 API、Cloudreve 文件 URI、Cloudreve 文件事件、rclone、Nextcloud WebDAV 与 OCS 分享、Google Drive API、Microsoft Graph OneDrive driveItem、Dropbox API 的公开设计。Cloudreve v4 说明上游使用 `/api/v4/`、JSON 响应和文件 URI；rclone 的 remote/backend 模型说明 provider adapter 要隔离；Nextcloud WebDAV 和分享接口说明文件元数据与分享状态应分开；Google Drive、OneDrive 和 Dropbox 都把文件、权限、增量变更、分享链接与游标或任务结果拆开。本文档只吸收 provider、文件快照、分享快照、异步同步任务、增量/降级和权限边界这些思路，不接入这些平台的主数据。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Cloudreve API Introduction](https://docs.cloudreve.org/en/api/overview) | Cloudreve v4 REST API 路径、响应结构和上游错误需要映射到本项目统一响应。 |
| [Cloudreve File URI](https://docs.cloudreve.org/en/api/file-uri) | Cloudreve 使用 URI 描述文件系统、分享、密码、路径和搜索条件，本服务需保存安全摘要并拒绝越界路径。 |
| [Cloudreve File Change Events](https://docs.cloudreve.org/en/api/events) | 上游可提供文件变化事件，但本服务第一版以任务同步和快照为准，不把 SSE 当作唯一数据源。 |
| [rclone 文档](https://rclone.org/docs/) | 多云盘 remote/backend 思路适合 provider adapter，不让业务层依赖单个云盘实现细节。 |
| [Nextcloud WebDAV](https://docs.nextcloud.com/server/latest/developer_manual/client_apis/WebDAV/index.html) | 文件元数据读取与分享能力分层，适合本服务拆分文件快照和分享快照。 |
| [Google Drive files](https://developers.google.com/workspace/drive/api/guides/about-files) | 文件对象、权限和变更跟踪分离，适合作为同步快照和变更检测参考。 |
| [Microsoft Graph driveItem](https://learn.microsoft.com/en-us/graph/api/resources/driveitem?view=graph-rest-1.0) | driveItem 统一文件和文件夹元数据，权限、共享和内容能力分离。 |
| [Dropbox HTTP API](https://www.dropbox.com/developers/documentation/http/documentation) | list、cursor、shared link 与错误响应分层，适合作为同步任务和幂等失败映射参考。 |
| [Google One plans](https://one.google.com/about/plans) | 云盘产品把免费额度、付费容量和共享额度显式展示，适合本服务提供配额摘要但不做真实计费。 |
| [Microsoft OneDrive plans](https://www.microsoft.com/en-us/microsoft-365/onedrive/onedrive-plans-and-pricing) | OneDrive 按免费、个人和家庭容量分层，适合 provider 展示套餐摘要和容量告警。 |
| [Dropbox plans](https://www.dropbox.com/plans) | Dropbox 团队套餐按起始容量和用户规模描述，适合本服务保留团队容量来源和超额策略摘要。 |
| [Cloudflare R2 pricing](https://developers.cloudflare.com/r2/pricing/) | R2 按 GB-month、读写操作和免费额度拆分费用，适合本服务输出估算字段和告警，不把估算当账单。 |

## 职责边界

`cloudreve-sync` 负责 Cloudreve provider 配置摘要、provider 健康状态、目录同步任务、文件元数据快照、分享链接快照、分享解析、链接失效探测、旧快照降级、同步任务状态、任务步骤摘要、幂等记录、同步审计和后台自检摘要。

`cloudreve-sync` 不负责注册、登录、会话、角色能力点主数据、玩家资源主数据、资源审核发布、下载权限判定、真实文件内容代理、浏览器直连 Cloudreve 管理凭据、后台服务器文件操作、宿主机文件系统、节点守护进程、运维审批、通知主数据、Cloudreve 管理 token 展示或真实 Cloudreve 删除操作。

第一版固定为内存存储和受控 fake Cloudreve adapter。它只建立可测试的 API 边界、安全快照、降级规则和同步任务模型。后续若要让 `resource` 消费本服务快照，必须按前序服务兼容变更流程更新 `docs/contracts-resource.md`、`.local-docs/tests-resource.md` 和自动化测试，确认红灯后再改 `resource`。

## 数据归属

`cloudreve-sync` 拥有以下主数据：CloudreveProvider、CloudreveFileSnapshot、CloudreveShareSnapshot、CloudreveSyncJob、CloudreveSyncJobStep、CloudreveResolveResult、CloudreveAuditLog、CloudreveOpsSummary 和幂等记录。

`cloudreve-sync` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和状态快照；可以保存来自 `resource` 的资源兼容引用快照；可以保存来自 `ops-control` 的 Cloudreve 服务资产引用摘要。所有跨服务字段只能来自正式接口、后端入口可信上下文或契约允许的本地测试 stub，不能直接读取前序服务数据库、内存 store、测试种子或私有类。

Cloudreve 真实凭据只能通过环境变量、启动参数或受控配置注入。仓库内只能保存配置键名和测试假值，不得提交真实 token、cookie、刷新 token、管理密码、分享密码明文、私有直链、内部绝对路径或完整上游响应。

## 基础路径、端口和认证

所有接口默认使用 `/api/v1/cloudreve-sync` 前缀。第一版本地端口固定为 `8118`，自检摘要必须返回该端口。

健康检查 `GET /api/v1/cloudreve-sync/health` 不要求认证，但只能返回存活、版本、服务名和请求编号，不返回 provider ID、Cloudreve 地址、token 摘要、内部路径、能力明细、任务数量或上游错误。

除健康检查外，全部接口要求 `Authorization: Bearer <token>`。读取 provider、文件、分享、任务和自检摘要要求后台角色 `HELPER`、`ADMIN` 或 `OWNER`，且具备 `NODE_READ` 或 `FILE_MANAGE`。写入 provider、同步任务、分享解析和取消任务要求 `ADMIN` 或 `OWNER`，并按动作要求 `NODE_WRITE` 或 `FILE_MANAGE`。审计列表只允许 `ADMIN` 或 `OWNER`。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`credential`、`tokenDigest`、`rawToken`、`cookie`、`refreshToken`、`authorizationHeader`、`internalPath`、`resolvedPath`、`sharePassword`、`beforeState`、`afterState`、`auditResult`、`createdBy`、`updatedBy`、`lastSyncedAt`、`taskStatus` 等服务端可信字段。出现可信字段时返回 `40001`。

## 本地测试控制头

`cloudreve-sync` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Cloudreve-Mode`、`X-Test-Resource-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Now` 模拟认证失败、上游不可用、上游超时、上游坏 schema、上游 401、资源兼容快照不可用、审计失败、状态写入失败和时间边界。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发认证失败、Cloudreve 失败、resource 失败、审计失败、存储失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 纳入生产化硬化项。

## 前序服务兼容契约

`auth` 是强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。auth 不可用返回 `46710`，auth 超时返回 `46711`，auth 字段或枚举不兼容返回 `46712`。

`resource` 是玩家资源主数据。`cloudreve-sync` 可以为 `resource` 提供 Cloudreve 文件和分享快照，但不能创建、修改或发布玩家资源，不能判断 `PUBLIC`、`AUTHENTICATED`、`MEMBER_ONLY` 或 `ADMIN_ONLY` 的下载权限，不能绕过 `resource` 直接给玩家返回下载结果。resource 兼容快照不可用返回 `46720`，resource 超时返回 `46721`，字段不兼容返回 `46722`。

`ops-control` 拥有 Cloudreve 服务资产和后台运维资产。`cloudreve-sync` 可以保存 Cloudreve 服务资产引用摘要，但不能把玩家资源权限当作服务器文件权限，也不能调用 `node-daemon` 执行文件操作。ops-control 资产快照不可用时，provider 仍可读取已有摘要，但创建或更新资产引用返回 `46730`。

`notification` 只负责通知投递。同步失败、链接失效或 provider 异常是否通知由调用方策略决定。第一版只在审计中记录 `notificationHint`，不自建通知主数据。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `ProviderStatus` | `ENABLED`、`DISABLED`、`DEGRADED`、`UNAVAILABLE` | Cloudreve provider 状态。 |
| `ProviderAuthMode` | `TOKEN`、`COOKIE`、`APP_PASSWORD`、`TEST_FAKE` | Cloudreve 认证模式。第一版只执行 `TEST_FAKE`。 |
| `ProviderCapability` | `FILE_LIST`、`FILE_METADATA`、`SHARE_RESOLVE`、`SHARE_REFRESH`、`EVENTS` | provider 能力摘要。 |
| `FileSnapshotStatus` | `ACTIVE`、`MISSING`、`DELETED_UPSTREAM`、`INACCESSIBLE`、`STALE` | 文件快照状态。 |
| `ShareStatus` | `ACTIVE`、`EXPIRED`、`DISABLED`、`PASSWORD_REQUIRED`、`UPSTREAM_MISSING`、`UNKNOWN` | 分享快照状态。 |
| `SyncJobType` | `PROVIDER_HEALTH_CHECK`、`DIRECTORY_SYNC`、`SHARE_REFRESH`、`RESOURCE_LINK_VERIFY` | 同步任务类型。 |
| `SyncJobStatus` | `PENDING`、`RUNNING`、`SUCCEEDED`、`FAILED`、`CANCELLED`、`TIMEOUT` | 同步任务状态。 |
| `SyncTrigger` | `ADMIN_MANUAL`、`SCHEDULED`、`RESOURCE_COMPATIBILITY_CHECK`、`TEST_CONTROL` | 同步触发来源。 |
| `DependencyStatus` | `AVAILABLE`、`UNAVAILABLE`、`TIMEOUT`、`BAD_SCHEMA`、`UNAUTHORIZED`、`DISABLED` | 外部依赖摘要。 |
| `CloudreveAuditResult` | `SUCCESS`、`FAILED` | 审计结果。 |
| `ProviderQuotaStatus` | `OK`、`WARNING`、`EXCEEDED`、`UNKNOWN` | provider 配额状态。 |

## 通用对象

### CloudreveProvider

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `providerId` | string | 是 | provider ID。 |
| `displayName` | string | 是 | 展示名，2 到 80 位。 |
| `baseUrlSummary` | string | 是 | Cloudreve 地址摘要，只能是脱敏域名或备注，不返回完整管理凭据。 |
| `authMode` | string | 是 | `ProviderAuthMode`。 |
| `status` | string | 是 | `ProviderStatus`。 |
| `capabilities` | string[] | 是 | `ProviderCapability` 数组。 |
| `timeoutMs` | integer | 是 | 上游请求超时，1000 到 30000。 |
| `opsAssetRef` | object 或 null | 是 | ops-control Cloudreve 服务资产引用摘要。 |
| `quotaTotalBytes` | integer 或 null | 是 | provider 可用总容量，未知时为 null。 |
| `quotaUsedBytes` | integer 或 null | 是 | provider 已用容量，未知时为 null。 |
| `quotaUsagePercent` | number 或 null | 是 | 已用容量百分比，保留一位小数，未知时为 null。 |
| `quotaWarningThresholdPercent` | integer | 是 | 配额告警阈值，默认 85。 |
| `quotaStatus` | string | 是 | `ProviderQuotaStatus`。 |
| `estimatedMonthlyCostCents` | integer 或 null | 是 | 按当前摘要估算的月成本，单位为分。该字段不是账单。 |
| `pricingPlanSummary` | object | 是 | 套餐摘要，包含 `planName`、`billingModel`、`currency`、`includedStorageBytes`、`overagePolicy` 和 `source`。 |
| `lastHealthStatus` | string 或 null | 是 | 最近依赖状态。 |
| `lastCheckedAt` | string 或 null | 是 | 最近检查时间。 |
| `lastSyncJobId` | string 或 null | 是 | 最近同步任务 ID。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### CloudreveProviderSummary

字段为 `providerId`、`displayName`、`baseUrlSummary`、`authMode`、`status`、`capabilities`、`quotaTotalBytes`、`quotaUsedBytes`、`quotaUsagePercent`、`quotaWarningThresholdPercent`、`quotaStatus`、`estimatedMonthlyCostCents`、`pricingPlanSummary`、`lastHealthStatus`、`lastCheckedAt`、`lastSyncJobId`、`degraded`、`degradeReasons`、`createdAt` 和 `updatedAt`。摘要不得返回 token、cookie、刷新 token、管理密码、分享密码、Authorization 头、完整上游 URL 查询串或内部路径。

### CloudreveFileSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `fileId` | string | 是 | 文件快照 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `cloudreveUriSummary` | string | 是 | Cloudreve URI 安全摘要，不返回分享密码明文。 |
| `parentPath` | string | 是 | provider 内相对目录，必须以 `/` 开头。 |
| `name` | string | 是 | 文件或目录名。 |
| `type` | string | 是 | `FILE` 或 `DIRECTORY`。 |
| `status` | string | 是 | `FileSnapshotStatus`。 |
| `sizeBytes` | integer 或 null | 是 | 文件大小。 |
| `mimeType` | string 或 null | 是 | MIME 摘要。 |
| `checksumSha256` | string 或 null | 是 | SHA-256 校验值。 |
| `etag` | string 或 null | 是 | 上游 etag 或版本摘要。 |
| `resourceRef` | object 或 null | 是 | resource 兼容引用快照。 |
| `shareSnapshotId` | string 或 null | 是 | 关联分享快照 ID。 |
| `lastSyncedAt` | string | 是 | 最近同步时间。 |
| `stale` | boolean | 是 | 是否旧快照。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |

### CloudreveShareSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `shareSnapshotId` | string | 是 | 分享快照 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `fileId` | string 或 null | 是 | 关联文件快照 ID。 |
| `shareId` | string | 是 | Cloudreve 分享 ID 或安全摘要。 |
| `shareUrlSummary` | string | 是 | 分享链接脱敏摘要，不包含分享密码明文。 |
| `status` | string | 是 | `ShareStatus`。 |
| `passwordRequired` | boolean | 是 | 是否需要提取码。 |
| `passwordStored` | boolean | 是 | 服务端是否有受控密码摘要。不得返回密码明文。 |
| `expiresAt` | string 或 null | 是 | 分享过期时间。 |
| `lastResolvedAt` | string 或 null | 是 | 最近解析时间。 |
| `lastCheckedAt` | string 或 null | 是 | 最近检查时间。 |
| `downloadAvailable` | boolean | 是 | 是否可供上游资源服务使用。 |
| `stale` | boolean | 是 | 是否旧快照。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |

### CloudreveSyncJob

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `jobId` | string | 是 | 同步任务 ID。 |
| `jobType` | string | 是 | `SyncJobType`。 |
| `status` | string | 是 | `SyncJobStatus`。 |
| `trigger` | string | 是 | `SyncTrigger`。 |
| `providerId` | string | 是 | provider ID。 |
| `target` | object | 是 | 任务目标摘要。 |
| `idempotencyKey` | string 或 null | 是 | 幂等键。 |
| `steps` | CloudreveSyncJobStep[] | 是 | 步骤摘要。 |
| `resultSummary` | object 或 null | 是 | 同步结果摘要。 |
| `failureReason` | string 或 null | 是 | 脱敏失败原因。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `startedAt` | string 或 null | 是 | 开始时间。 |
| `finishedAt` | string 或 null | 是 | 完成时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### CloudreveSyncJobStep

字段为 `stepId`、`name`、`status`、`dependencyStatus`、`startedAt`、`finishedAt`、`message` 和 `sanitizedPayloadSummary`。步骤摘要不得返回完整上游响应、token、cookie、分享密码、内部路径或异常堆栈。

### CloudreveResolveResult

字段为 `providerId`、`fileId`、`shareSnapshotId`、`shareStatus`、`downloadAvailable`、`shareUrlSummary`、`expiresAt`、`stale`、`degraded`、`degradeReasons`、`resolvedAt` 和 `resourceCompatibility`。该对象只供后台或后续 `resource` 兼容适配使用，不等于玩家下载票据。

### CloudreveAuditLog

审计字段继承公共契约，允许补充 `providerId`、`fileId`、`shareSnapshotId`、`jobId`、`dependencyStatus`、`stateFrom`、`stateTo`、`idempotencyKey` 和 `notificationHint`。审计列表不得提供删除接口。审计响应不得返回 Cloudreve token、cookie、刷新 token、管理密码、完整 Authorization 请求头、分享密码明文、私有直链、内部路径、完整上游响应或异常堆栈。

### CloudreveOpsSummary

字段至少包含 `service`、`port`、`storageMode`、`authMode`、`providerAdapterMode`、`resourceAdapterMode`、`opsAssetAdapterMode`、`testControlsEnabled`、`providersTotal`、`filesTotal`、`sharesTotal`、`jobsTotal`、`runningJobsTotal`、`failedJobsTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`quotaTotalBytes`、`quotaUsedBytes`、`quotaUsagePercent`、`quotaWarningProvidersTotal`、`quotaExceededProvidersTotal`、`estimatedMonthlyCostCents`、`pricingModelSummary`、`lastSyncAt`、`lastFailureAt`、`degraded`、`degradeReasons` 和 `productionGaps`。

## 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `46700` | 502 | Cloudreve provider 不可用。 |
| `46701` | 504 | Cloudreve provider 调用超时。 |
| `46702` | 502 | Cloudreve 返回结构不兼容。 |
| `46703` | 502 | Cloudreve 返回未授权或凭据失效。 |
| `46710` | 502 | auth 认证上下文不可用。 |
| `46711` | 504 | auth 认证上下文调用超时。 |
| `46712` | 502 | auth 认证上下文字段不兼容。 |
| `46720` | 502 | resource 兼容快照不可用。 |
| `46721` | 504 | resource 兼容快照调用超时。 |
| `46722` | 502 | resource 兼容快照字段不兼容。 |
| `46730` | 502 | ops-control Cloudreve 资产快照不可用。 |
| `49700` | 404 | provider 不存在。 |
| `49701` | 404 | 文件快照不存在。 |
| `49702` | 404 | 分享快照不存在。 |
| `49703` | 404 | 同步任务不存在。 |
| `49710` | 409 | provider 状态不允许当前操作。 |
| `49711` | 409 | 同步任务状态不允许当前操作。 |
| `49712` | 409 | 幂等键请求指纹冲突。 |
| `49713` | 409 | 分享链接已失效且无可用旧快照。 |
| `49714` | 400 | Cloudreve 路径不合法或越界。 |
| `55300` | 500 | cloudreve-sync 内部错误。 |
| `55301` | 500 | cloudreve-sync 审计写入失败。 |
| `55302` | 500 | cloudreve-sync 同步状态写入失败。 |

字段校验、未登录、令牌格式错误、角色权限不足、能力点不足、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/cloudreve-sync/health` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/cloudreve-sync/ops/summary` | 是 | `NODE_READ` 或 `FILE_MANAGE` | LOW |
| provider 列表 | GET | `/api/v1/cloudreve-sync/providers` | 是 | `NODE_READ` 或 `FILE_MANAGE` | LOW |
| provider 详情 | GET | `/api/v1/cloudreve-sync/providers/{providerId}` | 是 | `NODE_READ` 或 `FILE_MANAGE` | LOW |
| 创建 provider | POST | `/api/v1/cloudreve-sync/providers` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 更新 provider | PATCH | `/api/v1/cloudreve-sync/providers/{providerId}` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 禁用 provider | PATCH | `/api/v1/cloudreve-sync/providers/{providerId}/disable` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 启用 provider | PATCH | `/api/v1/cloudreve-sync/providers/{providerId}/enable` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 文件快照列表 | GET | `/api/v1/cloudreve-sync/files` | 是 | `FILE_MANAGE` 或 `NODE_READ` | LOW |
| 分享快照列表 | GET | `/api/v1/cloudreve-sync/shares` | 是 | `FILE_MANAGE` 或 `NODE_READ` | LOW |
| 分享解析 | POST | `/api/v1/cloudreve-sync/shares/resolve` | 是 | `FILE_MANAGE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 创建同步任务 | POST | `/api/v1/cloudreve-sync/sync-jobs` | 是 | `FILE_MANAGE` 或 `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 同步任务列表 | GET | `/api/v1/cloudreve-sync/sync-jobs` | 是 | `FILE_MANAGE` 或 `NODE_READ` | LOW |
| 同步任务详情 | GET | `/api/v1/cloudreve-sync/sync-jobs/{jobId}` | 是 | `FILE_MANAGE` 或 `NODE_READ` | LOW |
| 取消同步任务 | PATCH | `/api/v1/cloudreve-sync/sync-jobs/{jobId}/cancel` | 是 | 任务创建者、`ADMIN` 或 `OWNER` | MEDIUM |
| 审计列表 | GET | `/api/v1/cloudreve-sync/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 健康和自检接口

`GET /api/v1/cloudreve-sync/health` 成功返回 `service=cloudreve-sync`、`status`、`version` 和 `requestId`。进程存活但上游不可用时仍可返回 HTTP `200`，并用 `status=DEGRADED` 标记。该接口不得泄露 provider、Cloudreve 地址、凭据摘要、文件数量或任务数量。

`GET /api/v1/cloudreve-sync/ops/summary` 成功返回 `CloudreveOpsSummary`。第一版必须返回 `port=8118`、`storageMode=IN_MEMORY`、`providerAdapterMode=TEST_FAKE`、`resourceAdapterMode=TEST_STUB`、`opsAssetAdapterMode=TEST_STUB` 和生产化缺口。读取失败返回 `55300`，不得伪造健康。摘要不得返回 token、cookie、分享密码、完整 URL 查询串、后台备注、内部路径或审计原因全文。

自检摘要必须提供配额和成本估算摘要。第一版只根据 provider 快照计算 `quotaUsagePercent`、告警数量和 `estimatedMonthlyCostCents`，不连接真实账单、不生成扣费、不保存支付信息。配额达到告警阈值时 provider 返回 `quotaStatus=WARNING`，已用容量大于总容量时返回 `EXCEEDED`。同步读取旧快照仍允许，但写入类接口不得把超额状态伪装为健康。

## provider 接口

`GET /api/v1/cloudreve-sync/providers` 支持 `page`、`pageSize`、`keyword`、`status`、`authMode`、`capability` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`。成功响应分页 `items` 为 `CloudreveProviderSummary[]`。

`GET /api/v1/cloudreve-sync/providers/{providerId}` 返回 `CloudreveProvider`、最近任务摘要和降级原因。provider 不存在返回 `49700`。

`POST /api/v1/cloudreve-sync/providers` 请求字段为 `displayName`、`baseUrl`、`authMode`、`credential`、`capabilities`、`timeoutMs`、`opsAssetRef`、`quotaTotalBytes`、`quotaUsedBytes`、`quotaWarningThresholdPercent`、`estimatedMonthlyCostCents`、`pricingPlanSummary`、`enabled`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `CloudreveProvider`。`credential` 只写入受控配置或测试桩，不回显；响应只返回 `credentialStored=true` 或 `credentialRotated=true` 摘要。provider 名称冲突返回 `49710`。同一操作者、同一幂等键、同一请求体重复提交返回同一 provider，相同键不同体返回 `49712`。审计失败返回 `55301`，不得创建 provider。

`PATCH /api/v1/cloudreve-sync/providers/{providerId}` 可修改 `displayName`、`baseUrl`、`authMode`、`credential`、`capabilities`、`timeoutMs`、`opsAssetRef`、`quotaTotalBytes`、`quotaUsedBytes`、`quotaWarningThresholdPercent`、`estimatedMonthlyCostCents`、`pricingPlanSummary`、`reason` 和 `idempotencyKey`。`DISABLED` provider 可以更新配置摘要，但不能触发同步任务。更新凭据只记录轮换摘要，不返回原文。provider 不存在返回 `49700`，审计失败不得改变状态。

`PATCH /api/v1/cloudreve-sync/providers/{providerId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。禁用后状态为 `DISABLED`，不删除历史文件和分享快照，不允许创建新的同步任务。重复禁用保持幂等。

`PATCH /api/v1/cloudreve-sync/providers/{providerId}/enable` 请求字段为 `reason` 和 `idempotencyKey`。启用前校验配置摘要，成功后状态为 `ENABLED` 或在健康检查失败时 `DEGRADED`。`UNAVAILABLE` 且凭据无效的 provider 返回 `46703`。

## 文件和分享接口

`GET /api/v1/cloudreve-sync/files` 支持 `page`、`pageSize`、`providerId`、`parentPath`、`status`、`type`、`keyword`、`resourceId` 和 `sort`。`sort` 允许 `lastSyncedAt_desc`、`name_asc`、`sizeBytes_desc`。`parentPath` 必须以 `/` 开头，禁止 `..`、反斜杠、控制字符、URL 编码绕过和路径段前缀误判，路径非法返回 `49714`。provider 不存在返回 `49700`。

`GET /api/v1/cloudreve-sync/shares` 支持 `page`、`pageSize`、`providerId`、`fileId`、`status`、`downloadAvailable`、`keyword` 和 `sort`。`sort` 允许 `lastCheckedAt_desc`、`expiresAt_asc`、`createdAt_desc`。成功响应分页 `items` 为 `CloudreveShareSnapshot[]`。分享快照不得返回提取码明文、私有直链或完整上游响应。

`POST /api/v1/cloudreve-sync/shares/resolve` 请求字段为 `providerId`、`fileId`、`path`、`shareUrl`、`resourceRef`、`allowStale`、`reason` 和 `idempotencyKey`。`fileId`、`path` 和 `shareUrl` 至少传一个；`path` 必须通过 Cloudreve 路径守卫；`shareUrl` 只允许 http 或 https。成功返回 `CloudreveResolveResult`。provider 不存在返回 `49700`，文件不存在返回 `49701`，上游未授权返回 `46703`，上游不可用且 `allowStale=true` 且存在可用旧快照时返回成功并标记 `stale=true`、`degraded=true`；没有旧快照返回 `49713` 或 `46700`。同 key 同体返回同一解析结果，同 key 不同体返回 `49712`。审计失败不得写入新快照。

## 同步任务接口

`POST /api/v1/cloudreve-sync/sync-jobs` 请求字段为 `jobType`、`providerId`、`target`、`trigger`、`reason` 和 `idempotencyKey`。`DIRECTORY_SYNC` 的 `target` 至少包含 `path`；`SHARE_REFRESH` 至少包含 `shareSnapshotId` 或 `fileId`；`RESOURCE_LINK_VERIFY` 至少包含 `resourceRef`；`PROVIDER_HEALTH_CHECK` 只需要 provider，`target` 可以省略，由服务端生成 provider 目标摘要。成功响应 HTTP `201`，`data` 为 `CloudreveSyncJob`。

同步任务创建时必须校验 provider 存在且未禁用。`DISABLED` provider 返回 `49710`。路径越界返回 `49714`。同一操作者、同一 provider、同一 jobType、同一幂等键和同一请求体重复提交返回同一任务；相同键不同体返回 `49712`。审计失败或任务状态写入失败时不得返回成功。

第一版 fake adapter 可以同步完成任务，但接口语义仍按异步任务建模。`PROVIDER_HEALTH_CHECK` 可把 provider 更新为 `ENABLED`、`DEGRADED` 或 `UNAVAILABLE`。`DIRECTORY_SYNC` 可生成或更新文件快照。`SHARE_REFRESH` 可生成或更新分享快照。`RESOURCE_LINK_VERIFY` 只写入兼容摘要，不修改 `resource` 主数据。

`GET /api/v1/cloudreve-sync/sync-jobs` 支持 `page`、`pageSize`、`providerId`、`jobType`、`status`、`trigger`、`createdBy`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`finishedAt_desc`。成功响应分页 `items` 为 `CloudreveSyncJob[]`。

`GET /api/v1/cloudreve-sync/sync-jobs/{jobId}` 返回任务详情、步骤、结果摘要和失败原因。任务不存在返回 `49703`。

`PATCH /api/v1/cloudreve-sync/sync-jobs/{jobId}/cancel` 请求字段为 `reason` 和 `idempotencyKey`。只有 `PENDING` 和 `RUNNING` 任务可取消。`SUCCEEDED`、`FAILED`、`CANCELLED` 和 `TIMEOUT` 为终态，取消返回 `49711`。取消成功必须写审计；审计失败时任务状态保持不变。

## 审计接口

`GET /api/v1/cloudreve-sync/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`providerId`、`fileId`、`shareSnapshotId`、`jobId`、`action`、`result`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`。只有 `ADMIN` 和 `OWNER` 可访问。审计列表只读，不提供删除、修改或恢复接口。

审计中的 `requestId` 必须来自当前 HTTP 请求，不能使用固定占位值。后台写操作必须记录调用者、`reason`、操作前状态、操作后状态、请求编号、结果和失败原因。审计写入失败时，provider 创建修改启停、分享解析、同步任务创建和取消不得假装成功，必须返回 `55301` 并保持业务状态不变。

## 状态、幂等和并发

provider 状态从 `ENABLED` 开始，健康检查失败可进入 `DEGRADED` 或 `UNAVAILABLE`，管理员可禁用为 `DISABLED`。`DISABLED` 不删除历史快照，只阻止新同步任务。启用 provider 需要配置摘要有效，凭据失效返回 `46703`。

同步任务状态为 `PENDING` 到 `RUNNING`、`CANCELLED`、`FAILED` 或 `TIMEOUT`；`RUNNING` 到 `SUCCEEDED`、`FAILED`、`CANCELLED` 或 `TIMEOUT`。`SUCCEEDED`、`FAILED`、`CANCELLED` 和 `TIMEOUT` 为终态，不得重新执行。

文件快照状态由同步任务更新。上游找不到文件时进入 `MISSING` 或 `DELETED_UPSTREAM`；权限不足进入 `INACCESSIBLE`；上游不可用但保留旧信息时进入 `STALE`。快照不能反向修改 `resource` 主数据。

分享快照状态由解析和刷新任务更新。上游不可用时，如果旧快照仍在契约允许窗口内，可以返回 `stale=true` 和 `degraded=true`；旧快照过期、禁用或不可用时返回 `49713`、`46700` 或 `46701`，不能伪造可用下载。

写接口使用幂等键时，必须使用字段名排序后的稳定 JSON 语义计算请求体指纹，嵌套对象按字段名递归排序，数组保留顺序，不能依赖 Java `Map.toString()` 或浏览器字段顺序。所有写接口必须用本服务内串行临界区保护状态推进、幂等记录、审计和响应快照。后续数据库实现必须使用事务、唯一约束、条件更新或等效机制，不能降低并发口径。

## 安全、降级和脱敏

路径安全必须拒绝 `..`、反斜杠、控制字符、空路径、非 `/` 开头路径、URL 编码绕过和路径段前缀误判。Cloudreve URI 和分享链接只保存安全摘要。分享密码只能保存受控摘要，不返回明文。

任何响应不得包含 Cloudreve token、cookie、刷新 token、管理密码、完整 Authorization 请求头、分享密码明文、私有直链、内部文件系统路径、完整上游响应、异常堆栈、数据库连接串、`.env`、`authorized_keys`、`id_rsa` 或服务器密码。

Cloudreve 不可用时，读取类接口可以返回旧快照并标记 `degraded=true`、`stale=true` 和 `degradeReasons`。写入类接口不得假装成功。`resource`、`ops-control` 或 `auth` 不可用时，必须返回明确依赖错误或降级摘要，不能使用浏览器字段伪造可信上下文。

## 验收口径

`cloudreve-sync` API 文档必须按 `docs/contracts-cloudreve-sync.md` 独立存在，并由 `.local-docs/tests-cloudreve-sync.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求、敏感字段脱敏、测试控制头默认关闭和模块验收口径。

`cloudreve-sync` 完成时必须满足以下条件：端口固定为 `8118`；健康检查不泄露敏感信息；除健康检查外全部接口要求后台认证；provider、文件快照、分享快照、同步任务、幂等、状态流转、上游失败降级、旧快照降级、配额成本摘要、审计失败回滚、敏感字段脱敏、测试控制头默认关闭和自检摘要都有自动化验证；不修改前序服务稳定接口；不把玩家资源主数据、后台文件管理、节点守护进程或真实宿主机操作塞进本模块；自动化测试必须先红灯；实现后 `cloudreve-sync` 全量测试通过；前序 17 个稳定服务回归通过；边界扫描无违规命中；测试过程记录完整。
