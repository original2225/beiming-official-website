# 北冥官网 server-status API 契约

版本：0.1

## 文档定位

本文档是 `server-status` 微服务的正式 API 契约。后续 `resource`、`admin`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、前端适配和 `ops-control` 只能通过本文档定义的接口读取玩家可见服务器状态和线路状态，不能直接读取或修改 `server-status` 数据库。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、能力点、审计字段、风险等级、通用错误码和请求编号均以公共契约为准。本文档只补充 `server-status` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

`server-status` 只适配 `auth`。它通过后端入口传入的认证上下文、`/api/v1/auth/me`、`/api/v1/auth/session/verify` 或测试环境 auth stub 获取当前用户、角色、能力点和用户状态。它不要求 `auth`、`profile`、`notification` 或 `content` 反向适配。

## 职责边界

`server-status` 负责玩家可见的 Minecraft 服务器状态、版本、MOTD、在线人数、最大人数、延迟、线路状态、历史快照、历史峰值、宕机记录和开服时长。

`server-status` 不负责账号、会话、成员档案、通知投递、首页内容配置、玩家资源下载、Cloudreve 分享、后台运维控制、节点注册、容器启停、虚拟机管理、文件操作、日志流、终端命令、节点密钥、备份恢复和高风险审批。真实服务器运维操作属于后续 `ops-control` 和 `node-daemon`。

首页可以读取 `content` 的 `SERVER_ENTRY` 展示入口，也可以读取本文档的公开状态接口。`content` 不得伪造在线人数、MOTD、线路延迟或状态结果。`server-status` 也不得写入 content 首页配置。

## 数据归属

`server-status` 拥有以下主数据：玩家可见服务器实例、状态源、线路配置、当前状态缓存、历史状态快照、历史峰值统计、宕机记录、手动刷新幂等记录、审计日志和运行自检摘要。

状态源可以保存公开展示字段、检测类型和检测目标。检测目标、内部备注、采集凭据、节点信息和后台审计参数不得出现在公开接口。历史峰值只能由快照统计产生，不能由浏览器请求体写入。

## 基础路径与认证

公开接口使用 `/api/v1/server-status` 前缀，不要求登录，只返回玩家可见字段。

后台接口使用 `/api/v1/server-status/admin` 前缀，全部要求登录。后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`。后台写操作要求 `ADMIN` 或 `OWNER`，必须携带 `reason` 并写入审计。`HELPER` 可以读取状态源、线路、宕机记录和审计摘要，但不能创建、修改、启用、禁用、刷新或流转宕机记录。

`GET /api/v1/server-status/admin/audit-logs` 和 `GET /api/v1/server-status/admin/ops/summary` 只允许 `ADMIN` 或 `OWNER` 访问。

## auth 兼容契约

当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。

后台写操作里的 `createdBy`、`updatedBy`、`refreshedBy`、`disabledBy`、`enabledBy`、`acknowledgedBy`、`resolvedBy` 和 `archivedBy` 均来自服务端认证上下文。浏览器请求体传入同名字段时必须忽略或返回字段校验失败。

auth 上下文不可用返回 `46500`，auth 调用超时返回 `46501`，auth 返回字段缺失或枚举不兼容返回 `46502`。`server-status` 不能导入 auth 的内存存储、实体、Repository 或测试种子实现。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `ServerReachability` | `ONLINE`、`DEGRADED`、`OFFLINE`、`UNKNOWN` | 实例和整体在线状态。 |
| `InstanceKind` | `SURVIVAL`、`CREATIVE`、`TEST`、`LOBBY`、`OTHER` | Minecraft 实例类型。 |
| `StatusSourceType` | `MINECRAFT_PING`、`HTTP_HEALTH`、`MANUAL`、`STUB` | 状态采集源类型。P0 可以使用 `STUB` 和 `MANUAL`。 |
| `ConfigStatus` | `ENABLED`、`DISABLED`、`ARCHIVED` | 状态源和线路配置状态。 |
| `LineReachability` | `AVAILABLE`、`DEGRADED`、`UNAVAILABLE`、`UNKNOWN` | 线路实时状态。 |
| `OutageStatus` | `OPEN`、`ACKNOWLEDGED`、`RESOLVED`、`ARCHIVED` | 宕机记录状态。 |
| `SnapshotSource` | `SCHEDULED`、`MANUAL_REFRESH`、`SEED` | 快照来源。 |
| `ServerStatusAuditResult` | `SUCCESS`、`FAILED` | 审计执行结果。 |

## 通用对象

### PublicServerStatusOverview

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `overallStatus` | string | 是 | `ServerReachability`。 |
| `primaryInstance` | PublicServerInstanceStatus 或 null | 是 | 主实例公开状态。 |
| `primaryLine` | PublicServerLineStatus 或 null | 是 | 主线路公开状态。 |
| `onlinePlayers` | integer | 是 | 当前公开在线人数，未知时为 `0`。 |
| `maxPlayers` | integer | 是 | 当前公开最大人数，未知时为 `0`。 |
| `version` | string 或 null | 是 | Minecraft 版本。 |
| `motd` | string 或 null | 是 | 公开 MOTD。 |
| `latencyMs` | integer 或 null | 是 | 主实例延迟毫秒。 |
| `uptimeSeconds` | integer 或 null | 是 | 基于 `startedAt` 或连续在线区间计算的开服时长。 |
| `peakOnlinePlayers` | integer | 是 | 历史峰值在线人数。 |
| `lastSuccessfulSnapshotAt` | string 或 null | 是 | 最近成功采集时间。 |
| `lastCheckedAt` | string 或 null | 是 | 最近一次采集尝试时间。 |
| `instances` | PublicServerInstanceStatus[] | 是 | 公开实例状态。 |
| `lines` | PublicServerLineStatus[] | 是 | 公开线路状态。 |
| `openOutages` | ServerOutagePublicRecord[] | 是 | 公开可见的未归档宕机记录。 |
| `degraded` | boolean | 是 | 是否使用降级结果。 |
| `degradeReasons` | string[] | 是 | 降级原因，例如 `COLLECTOR_TIMEOUT`、`NO_RECENT_SNAPSHOT`。 |

### PublicServerInstanceStatus

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `instanceId` | string | 是 | 实例 ID。 |
| `name` | string | 是 | 公开展示名称，2 到 80 位。 |
| `kind` | string | 是 | `InstanceKind`。 |
| `status` | string | 是 | `ServerReachability`。 |
| `version` | string 或 null | 是 | Minecraft 版本。 |
| `motd` | string 或 null | 是 | 公开 MOTD。 |
| `onlinePlayers` | integer | 是 | 在线人数。 |
| `maxPlayers` | integer | 是 | 最大人数。 |
| `latencyMs` | integer 或 null | 是 | 延迟毫秒。 |
| `startedAt` | string 或 null | 是 | 连续在线区间开始时间。 |
| `lastSuccessfulSnapshotAt` | string 或 null | 是 | 最近成功快照时间。 |
| `sortOrder` | integer | 是 | 展示排序。 |

### PublicServerLineStatus

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `lineId` | string | 是 | 线路 ID。 |
| `name` | string | 是 | 线路展示名称，2 到 80 位。 |
| `entryAddress` | string | 是 | 玩家可见连接地址。 |
| `description` | string 或 null | 是 | 公开说明，最多 300 位。 |
| `status` | string | 是 | `LineReachability`。 |
| `latencyMs` | integer 或 null | 是 | 线路延迟毫秒。 |
| `packetLossPercent` | number 或 null | 是 | 丢包率，0 到 100。 |
| `lastCheckedAt` | string 或 null | 是 | 最近检测时间。 |
| `sortOrder` | integer | 是 | 展示排序。 |

### ServerStatusSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `snapshotId` | string | 是 | 快照 ID。 |
| `sourceId` | string | 是 | 状态源 ID。 |
| `instanceId` | string | 是 | 实例 ID。 |
| `lineId` | string 或 null | 是 | 关联线路 ID。 |
| `source` | string | 是 | `SnapshotSource`。 |
| `status` | string | 是 | `ServerReachability`。 |
| `lineStatus` | string 或 null | 是 | `LineReachability`。 |
| `version` | string 或 null | 是 | Minecraft 版本。 |
| `motd` | string 或 null | 是 | 公开 MOTD。 |
| `onlinePlayers` | integer | 是 | 在线人数。 |
| `maxPlayers` | integer | 是 | 最大人数。 |
| `latencyMs` | integer 或 null | 是 | 实例延迟。 |
| `lineLatencyMs` | integer 或 null | 是 | 线路延迟。 |
| `checkedAt` | string | 是 | 采集时间。 |
| `degraded` | boolean | 是 | 是否为降级快照视图。 |

### ServerOutagePublicRecord

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `outageId` | string | 是 | 宕机记录 ID。 |
| `title` | string | 是 | 公开标题，2 到 120 位。 |
| `publicMessage` | string | 是 | 公开说明，1 到 1000 位。 |
| `status` | string | 是 | `OPEN`、`ACKNOWLEDGED` 或 `RESOLVED`。 |
| `severity` | string | 是 | `LOW`、`MEDIUM`、`HIGH` 或 `CRITICAL`。 |
| `startedAt` | string | 是 | 影响开始时间。 |
| `resolvedAt` | string 或 null | 是 | 恢复时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### AdminStatusSource

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sourceId` | string | 是 | 状态源 ID。 |
| `instanceId` | string | 是 | 实例 ID。 |
| `displayName` | string | 是 | 后台显示名，2 到 80 位。 |
| `instanceName` | string | 是 | 公开实例名称。 |
| `instanceKind` | string | 是 | `InstanceKind`。 |
| `sourceType` | string | 是 | `StatusSourceType`。 |
| `configStatus` | string | 是 | `ConfigStatus`。 |
| `publicVisible` | boolean | 是 | 是否出现在公开接口。 |
| `primary` | boolean | 是 | 是否主实例。 |
| `target` | string | 是 | 后台检测目标，不得公开返回。 |
| `timeoutMs` | integer | 是 | 采集超时，500 到 10000。 |
| `sortOrder` | integer | 是 | 展示排序。 |
| `startedAt` | string 或 null | 是 | 手工配置的开服开始时间。 |
| `adminNote` | string 或 null | 是 | 后台备注，不得公开返回。 |
| `lastSnapshotAt` | string 或 null | 是 | 最近快照时间。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### AdminLineConfig

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `lineId` | string | 是 | 线路 ID。 |
| `name` | string | 是 | 展示名称。 |
| `entryAddress` | string | 是 | 玩家入口地址，同一未归档线路内唯一。 |
| `checkTarget` | string | 是 | 后台检测目标，不得公开返回。 |
| `description` | string 或 null | 是 | 公开说明。 |
| `configStatus` | string | 是 | `ConfigStatus`。 |
| `currentStatus` | string | 是 | `LineReachability`。 |
| `publicVisible` | boolean | 是 | 是否公开。 |
| `primary` | boolean | 是 | 是否主线路。 |
| `sortOrder` | integer | 是 | 展示排序。 |
| `adminNote` | string 或 null | 是 | 后台备注，不得公开返回。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### AdminOutageRecord

`AdminOutageRecord` 在 `ServerOutagePublicRecord` 基础上补充 `instanceId`、`lineId`、`internalReason`、`adminNote`、`createdBy`、`updatedBy`、`acknowledgedBy`、`resolvedBy`、`archivedBy`、`acknowledgedAt`、`archivedAt`、`createdAt`。后台内部字段不得出现在公开接口。

### ServerStatusAuditLog

审计字段继承公共契约，允许补充 `sourceId`、`lineId`、`outageId`、`snapshotId`、`idempotencyKey`、`stateFrom`、`stateTo` 和 `collectorStatus`。审计日志不得通过 server-status API 删除。

### ServerStatusOpsSummary

自检摘要至少包含 `service`、`storageMode`、`collectorMode`、`authMode`、`sourcesTotal`、`instancesTotal`、`linesTotal`、`snapshotsTotal`、`outagesTotal`、`auditsTotal`、`lastSnapshotAt`、`lastAuditAt` 和 `warnings`。不得返回 token、请求头、检测凭据、后台备注、内部检测目标密码或审计原因全文。

## server-status 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `43500` | 404 | 服务器实例不存在，或公开接口不可访问该实例。 |
| `43501` | 404 | 线路不存在，或公开接口不可访问该线路。 |
| `43502` | 404 | 状态源不存在。 |
| `43503` | 404 | 状态快照不存在。 |
| `43504` | 404 | 宕机记录不存在，或公开接口不可访问该记录。 |
| `43510` | 409 | 状态源、线路或宕机记录状态不允许当前操作。 |
| `43511` | 409 | 线路入口、实例标识或状态源标识冲突。 |
| `43512` | 409 | 手动刷新过于频繁或已有刷新进行中。 |
| `46500` | 502 | auth 认证上下文不可用。 |
| `46501` | 504 | auth 认证上下文调用超时。 |
| `46502` | 502 | auth 认证上下文不兼容 server-status 契约。 |
| `46510` | 502 | 状态采集不可用。 |
| `46511` | 504 | 状态采集超时。 |
| `51500` | 500 | server-status 内部错误。 |
| `51501` | 500 | server-status 审计写入失败。 |
| `51502` | 500 | server-status 快照写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误、幂等键冲突和通用服务端错误优先使用公共错误码。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 状态总览 | GET | `/api/v1/server-status/overview` | 否 | 无 | LOW |
| 公开实例列表 | GET | `/api/v1/server-status/instances` | 否 | 无 | LOW |
| 公开实例详情 | GET | `/api/v1/server-status/instances/{instanceId}` | 否 | 无 | LOW |
| 公开线路列表 | GET | `/api/v1/server-status/lines` | 否 | 无 | LOW |
| 历史快照 | GET | `/api/v1/server-status/history/snapshots` | 否 | 无 | LOW |
| 公开宕机记录 | GET | `/api/v1/server-status/outages` | 否 | 无 | LOW |
| 状态源列表 | GET | `/api/v1/server-status/admin/sources` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建状态源 | POST | `/api/v1/server-status/admin/sources` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改状态源 | PATCH | `/api/v1/server-status/admin/sources/{sourceId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 禁用状态源 | PATCH | `/api/v1/server-status/admin/sources/{sourceId}/disable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 启用状态源 | PATCH | `/api/v1/server-status/admin/sources/{sourceId}/enable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 手动刷新 | POST | `/api/v1/server-status/admin/sources/{sourceId}/refresh` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台线路列表 | GET | `/api/v1/server-status/admin/lines` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建线路 | POST | `/api/v1/server-status/admin/lines` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改线路 | PATCH | `/api/v1/server-status/admin/lines/{lineId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 禁用线路 | PATCH | `/api/v1/server-status/admin/lines/{lineId}/disable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 启用线路 | PATCH | `/api/v1/server-status/admin/lines/{lineId}/enable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台宕机列表 | GET | `/api/v1/server-status/admin/outages` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建宕机记录 | POST | `/api/v1/server-status/admin/outages` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改宕机记录 | PATCH | `/api/v1/server-status/admin/outages/{outageId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 确认宕机记录 | PATCH | `/api/v1/server-status/admin/outages/{outageId}/acknowledge` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 解决宕机记录 | PATCH | `/api/v1/server-status/admin/outages/{outageId}/resolve` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档宕机记录 | PATCH | `/api/v1/server-status/admin/outages/{outageId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审计列表 | GET | `/api/v1/server-status/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 自检摘要 | GET | `/api/v1/server-status/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 公开接口

### 状态总览

`GET /api/v1/server-status/overview`

成功响应 HTTP `200`，`data` 为 `PublicServerStatusOverview`。

业务规则：总览只能汇总 `ENABLED`、`publicVisible=true` 且未归档的状态源和线路。整体状态根据关键实例和关键线路计算：全部未知为 `UNKNOWN`，任一关键实例离线为 `OFFLINE`，任一关键实例或关键线路降级为 `DEGRADED`，否则为 `ONLINE`。采集失败时可以返回最近一次成功快照并标记 `degraded=true`，不得伪造实时成功。没有任何快照时返回 `UNKNOWN`、空数组和 `NO_RECENT_SNAPSHOT`。

公开字段不得包含 `target`、`checkTarget`、`adminNote`、`internalReason`、审计字段、幂等键、节点信息、采集凭据或运维控制入口。

### 公开实例列表

`GET /api/v1/server-status/instances`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `kind` | string | 否 | 任一 `InstanceKind`。 |
| `status` | string | 否 | 任一 `ServerReachability`。 |
| `sort` | string | 否 | 允许 `sortOrder_asc`、`name_asc`、`onlinePlayers_desc`。默认 `sortOrder_asc`。 |

成功响应 HTTP `200`，`data.items` 为 `PublicServerInstanceStatus[]`。

业务规则：只返回公开启用实例。`DISABLED`、`ARCHIVED` 或 `publicVisible=false` 的状态源不得出现在公开列表。

### 公开实例详情

`GET /api/v1/server-status/instances/{instanceId}`

成功响应 HTTP `200`，`data` 为 `PublicServerInstanceStatus`。实例不存在、未启用、已归档或不可公开时返回 `43500`。

### 公开线路列表

`GET /api/v1/server-status/lines`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `status` | string | 否 | 任一 `LineReachability`。 |
| `sort` | string | 否 | 允许 `sortOrder_asc`、`latencyMs_asc`、`name_asc`。默认 `sortOrder_asc`。 |

成功响应 HTTP `200`，`data.items` 为 `PublicServerLineStatus[]`。只返回 `ENABLED` 且公开的线路，不返回 `checkTarget` 和后台备注。

### 历史快照

`GET /api/v1/server-status/history/snapshots`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `instanceId` | string | 否 | 只返回指定公开实例快照。 |
| `lineId` | string | 否 | 只返回指定公开线路快照。 |
| `status` | string | 否 | 任一 `ServerReachability`。 |
| `from` | string | 否 | ISO 8601 开始时间。 |
| `to` | string | 否 | ISO 8601 结束时间，必须不早于 `from`。 |
| `sort` | string | 否 | 允许 `checkedAt_desc`、`checkedAt_asc`、`onlinePlayers_desc`。默认 `checkedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `ServerStatusSnapshot[]`。

业务规则：公开历史只返回公开实例和公开线路的快照。非法分页返回 `40002`，非法排序返回 `40003`，不存在或不可公开的实例返回 `43500`，不存在或不可公开的线路返回 `43501`。

### 公开宕机记录

`GET /api/v1/server-status/outages`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `status` | string | 否 | 只允许 `OPEN`、`ACKNOWLEDGED`、`RESOLVED`。 |
| `severity` | string | 否 | 任一风险等级。 |
| `sort` | string | 否 | 允许 `startedAt_desc`、`updatedAt_desc`。默认 `startedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `ServerOutagePublicRecord[]`。

业务规则：只返回公开可见且未归档的宕机记录。公开接口不得返回 `internalReason`、`adminNote`、操作者字段和审计字段。

## 后台状态源接口

### 状态源列表

`GET /api/v1/server-status/admin/sources`

查询参数包括 `page`、`pageSize`、`keyword`、`sourceType`、`configStatus`、`instanceKind`、`publicVisible` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`sortOrder_asc`、`displayName_asc`。成功响应分页 `items` 为 `AdminStatusSource[]`。

### 创建状态源

`POST /api/v1/server-status/admin/sources`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `instanceName` | string | 是 | 2 到 80 位。 |
| `instanceKind` | string | 是 | 任一 `InstanceKind`。 |
| `sourceType` | string | 是 | 任一 `StatusSourceType`。 |
| `target` | string | 是 | 1 到 500 位，后台检测目标。 |
| `publicVisible` | boolean | 否 | 默认 `true`。 |
| `primary` | boolean | 否 | 默认 `false`。 |
| `timeoutMs` | integer | 否 | 默认 `3000`，范围 500 到 10000。 |
| `sortOrder` | integer | 否 | 默认 `100`。 |
| `startedAt` | string 或 null | 否 | 开服开始时间。 |
| `adminNote` | string 或 null | 否 | 最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `AdminStatusSource`，默认 `configStatus=ENABLED`。

业务规则：同一未归档实例展示名或同一状态源目标冲突返回 `43511`。同一操作者、同一 `idempotencyKey`、同一请求体重复提交返回同一结果；同一幂等键搭配不同请求体返回 `43002`。审计失败返回 `51501`，不得创建状态源。

### 修改状态源

`PATCH /api/v1/server-status/admin/sources/{sourceId}`

请求字段同创建状态源，除 `reason` 必填外其余字段按需修改。`ARCHIVED` 状态源不可修改检测目标，返回 `43510`。状态源不存在返回 `43502`。成功写入 `SERVER_STATUS_SOURCE_UPDATED` 审计。

### 禁用状态源

`PATCH /api/v1/server-status/admin/sources/{sourceId}/disable`

请求字段只有必填 `reason`。`ENABLED` 可流转为 `DISABLED`。重复禁用返回成功，保持幂等，不重复写审计。`ARCHIVED` 返回 `43510`。

### 启用状态源

`PATCH /api/v1/server-status/admin/sources/{sourceId}/enable`

请求字段只有必填 `reason`。`DISABLED` 可流转为 `ENABLED`。重复启用返回成功，保持幂等，不重复写审计。`ARCHIVED` 返回 `43510`。

### 手动刷新

`POST /api/v1/server-status/admin/sources/{sourceId}/refresh`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 刷新重试幂等键，10 分钟内有效。 |

成功响应 HTTP `200`，`data` 为 `ServerStatusSnapshot`。

业务规则：只有 `ENABLED` 状态源可刷新。刷新过于频繁、相同状态源已有刷新进行中或同一幂等键请求体冲突返回 `43512` 或 `43002`。采集不可用返回 `46510`，采集超时返回 `46511`。采集失败不能写入伪造成功快照。快照写入失败返回 `51502`。刷新成功写入 `SERVER_STATUS_SOURCE_REFRESHED` 审计。

## 后台线路接口

### 后台线路列表

`GET /api/v1/server-status/admin/lines`

查询参数包括 `page`、`pageSize`、`keyword`、`configStatus`、`currentStatus`、`publicVisible` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`sortOrder_asc`、`name_asc`。成功响应分页 `items` 为 `AdminLineConfig[]`。

### 创建线路

`POST /api/v1/server-status/admin/lines`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `name` | string | 是 | 2 到 80 位。 |
| `entryAddress` | string | 是 | 1 到 255 位，玩家可见入口。 |
| `checkTarget` | string | 是 | 1 到 500 位，后台检测目标。 |
| `description` | string 或 null | 否 | 最多 300 位。 |
| `publicVisible` | boolean | 否 | 默认 `true`。 |
| `primary` | boolean | 否 | 默认 `false`。 |
| `sortOrder` | integer | 否 | 默认 `100`。 |
| `adminNote` | string 或 null | 否 | 最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `AdminLineConfig`。同一未归档线路入口冲突返回 `43511`。审计失败返回 `51501`，不得创建线路。

### 修改线路

`PATCH /api/v1/server-status/admin/lines/{lineId}`

请求字段同创建线路，除 `reason` 必填外其余字段按需修改。线路不存在返回 `43501`。`ARCHIVED` 线路不可修改检测目标，返回 `43510`。成功写入 `SERVER_STATUS_LINE_UPDATED` 审计。

### 禁用线路

`PATCH /api/v1/server-status/admin/lines/{lineId}/disable`

请求字段只有必填 `reason`。禁用后公开线路列表不再返回该线路。重复禁用保持幂等，不重复写审计。

### 启用线路

`PATCH /api/v1/server-status/admin/lines/{lineId}/enable`

请求字段只有必填 `reason`。启用后按 `publicVisible` 决定是否公开。重复启用保持幂等，不重复写审计。

## 后台宕机接口

### 后台宕机列表

`GET /api/v1/server-status/admin/outages`

查询参数包括 `page`、`pageSize`、`status`、`severity`、`instanceId`、`lineId`、`keyword` 和 `sort`。`sort` 允许 `startedAt_desc`、`updatedAt_desc`、`resolvedAt_desc`。成功响应分页 `items` 为 `AdminOutageRecord[]`。

### 创建宕机记录

`POST /api/v1/server-status/admin/outages`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `title` | string | 是 | 2 到 120 位。 |
| `publicMessage` | string | 是 | 1 到 1000 位。 |
| `severity` | string | 是 | 任一风险等级。 |
| `instanceId` | string 或 null | 否 | 关联实例。 |
| `lineId` | string 或 null | 否 | 关联线路。 |
| `startedAt` | string | 是 | 影响开始时间，不得晚于当前请求时间太多。 |
| `internalReason` | string 或 null | 否 | 内部原因，最多 1000 位。 |
| `adminNote` | string 或 null | 否 | 后台备注，最多 1000 位。 |
| `publicVisible` | boolean | 否 | 默认 `true`。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `AdminOutageRecord`，默认 `status=OPEN`。关联实例或线路不存在时分别返回 `43500` 或 `43501`。审计失败不得创建记录。

### 修改宕机记录

`PATCH /api/v1/server-status/admin/outages/{outageId}`

请求字段同创建宕机记录，除 `reason` 必填外其余字段按需修改。`ARCHIVED` 记录不可修改，返回 `43510`。

### 确认宕机记录

`PATCH /api/v1/server-status/admin/outages/{outageId}/acknowledge`

请求字段只有必填 `reason`。`OPEN` 可流转为 `ACKNOWLEDGED`。重复确认 `ACKNOWLEDGED` 返回成功，保持幂等，不重复写审计。`RESOLVED` 和 `ARCHIVED` 返回 `43510`。

### 解决宕机记录

`PATCH /api/v1/server-status/admin/outages/{outageId}/resolve`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `resolvedAt` | string | 否 | 默认当前时间，必须不早于 `startedAt`。 |
| `publicMessage` | string | 否 | 可更新恢复说明，最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |

`OPEN` 和 `ACKNOWLEDGED` 可流转为 `RESOLVED`。重复解决同一记录返回成功，保持 `resolvedAt` 不变，不重复写审计。`ARCHIVED` 返回 `43510`。

### 归档宕机记录

`PATCH /api/v1/server-status/admin/outages/{outageId}/archive`

请求字段只有必填 `reason`。只有 `RESOLVED` 可流转为 `ARCHIVED`。重复归档返回成功，保持幂等，不重复写审计。`OPEN` 和 `ACKNOWLEDGED` 返回 `43510`。

## 审计和自检接口

### 审计列表

`GET /api/v1/server-status/admin/audit-logs`

查询参数包括 `page`、`pageSize`、`targetType`、`targetId`、`action`、`actorUserId`、`result`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`。成功响应分页 `items` 为 `ServerStatusAuditLog[]`。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 server-status API 删除。

### 自检摘要

`GET /api/v1/server-status/admin/ops/summary`

成功响应 HTTP `200`，`data` 为 `ServerStatusOpsSummary`。

业务规则：自检摘要用于后台确认 `server-status` 当前运行模式、数据量、采集器模式和生产化缺口。P0 可返回 `storageMode=IN_MEMORY`、`collectorMode=TEST_STUB` 和 `authMode=TEST_STUB`。摘要不得返回 token、请求头、检测凭据、后台备注、内部检测目标密码或审计原因全文。读取失败返回 `51500`，不得伪造健康。

## 状态、幂等和并发

状态源和线路配置状态流转为 `ENABLED` 到 `DISABLED`，`DISABLED` 到 `ENABLED`。`ARCHIVED` 保留给后续清理和迁移，P0 不提供归档接口。`ARCHIVED` 只保留历史，不参与采集，不公开展示，不允许修改检测目标。

宕机记录状态流转为 `OPEN` 到 `ACKNOWLEDGED` 或 `RESOLVED`，`ACKNOWLEDGED` 到 `RESOLVED`，`RESOLVED` 到 `ARCHIVED`。`ARCHIVED` 不可修改。重复确认、重复解决和重复归档按本文档保持幂等。

创建状态源、创建线路、创建宕机记录和手动刷新支持 `idempotencyKey`。同一操作者、同一幂等键、同一请求体重复提交时返回同一结果。相同幂等键搭配不同请求体返回 `43002`。并发创建相同状态源目标、线路入口或重复刷新同一状态源时只能一个请求成功，其余返回冲突。

## 状态采集与失败降级

采集器必须和 store 分开。采集成功时写入当前状态和历史快照。采集失败时不清空旧状态，公开接口优先返回最近一次成功快照，并标记 `degraded=true`。没有任何快照时返回 `UNKNOWN`，不得伪造在线。

降级原因允许 `COLLECTOR_UNAVAILABLE`、`COLLECTOR_TIMEOUT`、`NO_RECENT_SNAPSHOT`、`PARTIAL_LINE_FAILURE`、`SOURCE_DISABLED` 和 `IN_MEMORY_STALE_DATA`。采集不可用、超时和快照写入失败必须有可测试错误码。

## 审计要求

必须审计的动作包括创建状态源、修改状态源、禁用状态源、启用状态源、手动刷新、创建线路、修改线路、禁用线路、启用线路、创建宕机记录、修改宕机记录、确认宕机记录、解决宕机记录和归档宕机记录。

后台写操作必须记录 `reason`。审计字段继承公共契约。审计写入失败时，后台写操作不得假装成功，必须返回 `51501` 或 `51500`，并保持业务数据不变。公开读取和后台低风险读取不强制写审计。

## 验收口径

`server-status` API 文档按 `docs/contracts-server-status.md` 独立存在，并由 `.local-docs/tests-server-status.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`server-status` 完成时必须满足以下条件：全部接口按本文档实现；公开接口不泄露后台检测目标、内部备注、采集凭据、审计字段和运维入口；后台接口按角色限制；手动刷新遵守采集失败和降级规则；历史峰值和开服时长由服务端计算；状态源、线路、宕机记录、快照、审计、自检摘要、requestId、端口配置和 auth 适配都有自动化测试；`.local-docs/tests-server-status.md` 中全部测试用例都有对应自动化验证；未实现时自动化测试必须先失败；实现后 `server-status` 全部测试通过；`auth`、`profile`、`notification` 和 `content` 前序服务回归测试通过；没有修改前序服务稳定接口；没有把资源下载、Cloudreve 分享、容器启停、文件、日志、终端或节点控制能力塞进 `server-status`。
