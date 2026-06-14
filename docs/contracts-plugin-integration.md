# 北冥官网 plugin-integration API 契约

版本：0.1

## 文档定位

本文档是 `plugin-integration` 模块的正式 API 契约。后续前端插件联动页面、管理后台、`online-map`、`alerting`、`changelog`、`ops-control`、`external-node-executor` 和其他业务模块只能通过本文档定义的接口读取或管理插件 provider、插件实例快照、事件 schema、事件、路由规则、同步任务、健康快照、对象映射和审计摘要，不能直接读取或修改 `plugin-integration` 数据，也不能把真实 Minecraft 插件运行、节点命令、地图主数据、告警规则、通知渠道或服务器文件操作塞进本服务。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、运维能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `plugin-integration` 的职责边界、数据归属、前序模块兼容、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考 Paper、Bukkit/Spigot、Velocity、BlueMap、Dynmap、LuckPerms、PlaceholderAPI、DiscordSRV、Prometheus exporter、Modrinth、CurseForge 和 Hangar 的公开设计。Paper、Bukkit 和 Velocity 的事件监听模型说明事件必须有来源、类型、版本和受控 payload；BlueMap 和 Dynmap 的 marker 模型说明插件对象只能同步为地图对象建议或快照，不能直接接管地图服务主数据；LuckPerms 说明外部权限插件只能作为权限来源摘要，不能替代官网 `auth`；PlaceholderAPI 说明跨插件变量要有命名空间和白名单；DiscordSRV 说明跨平台通知必须走受控渠道，不能泄露外部 token；Prometheus exporter 说明插件指标应作为指标快照，不应混入业务主数据；Modrinth、CurseForge 和 Hangar 的项目、版本、文件、依赖和平台拆分说明插件来源、插件版本和分发元数据要结构化。本文档只吸收这些生态的设计思路，不导入它们的 Java API、插件 jar、平台 SDK、私有 token、Webhook secret、配置文件或命令能力。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Paper Plugin API](https://docs.papermc.io/paper/dev/plugin-api/) | 插件事件、生命周期和服务端 API 需要和官网控制面分离。 |
| [Velocity Event API](https://docs.papermc.io/velocity/dev/event-api/) | 代理服事件和后端服事件要区分来源、server kind 和实例上下文。 |
| [BlueMap Markers](https://bluemap.bluecolored.de/wiki/customization/Markers.html) | 地图插件对象适合作为 marker、region、layer 快照或同步建议。 |
| [Dynmap Markers](https://github.com/webbukkit/dynmap/wiki/Using-markers) | 地图 marker 和 area 的外部来源需要保留 provider 和世界引用。 |
| [LuckPerms Developer API](https://luckperms.net/wiki/Developer-API) | 权限插件只可作为外部权限摘要，不能替代官网权限主数据。 |
| [PlaceholderAPI Expansion](https://wiki.placeholderapi.com/developers/creating-a-placeholderexpansion/) | 跨插件变量需要命名空间、变量白名单和 payload 脱敏。 |
| [DiscordSRV Documentation](https://docs.discordsrv.com/) | 跨平台通知要走受控路由，不保存 Discord token 或 webhook secret。 |
| [Prometheus Exposition Formats](https://prometheus.io/docs/instrumenting/exposition_formats/) | 插件指标应以指标快照暴露，不直接写入业务主数据。 |
| [Modrinth API](https://docs.modrinth.com/api/) | 插件项目、版本、文件和依赖适合作为 provider 与实例版本摘要参考。 |
| [CurseForge Core API](https://docs.curseforge.com/) | 分发平台的游戏、mod、文件与依赖关系需要和本地运行态分开。 |
| [Hangar API Docs](https://hangar.papermc.io/api-docs) | Paper 生态插件平台强调平台、版本、通道和发布元数据拆分。 |

## 职责边界

`plugin-integration` 负责插件联动控制面，包括插件 provider 注册摘要、插件实例快照、插件能力声明、事件 schema、事件接收和脱敏、事件路由规则、同步任务摘要、插件健康快照、插件对象映射、依赖调用摘要、幂等记录、审计日志和自检摘要。

`plugin-integration` 不负责注册、登录、权限主数据、站内通知主数据、告警规则主数据、地图主数据、服务器状态主数据、真实插件运行、真实插件安装卸载、真实插件命令、真实世界目录读取、真实节点命令执行、真实文件写入、真实容器或虚拟机操作、真实跨平台消息发送、玩家资源下载或官网内容发布。

第一版固定为安全控制面和事件快照。它可以接收后台或测试适配器提交的插件事件，保存脱敏 payload 摘要，生成路由结果和模拟同步任务。它不能开放无鉴权公网 webhook，不能保存完整 raw payload，不能保存插件后台 token，不能调用真实 `external-node-executor`，不能写真实插件配置，不能执行 Minecraft 命令，不能直接改 `online-map`、`alerting`、`notification`、`server-status` 或 `changelog` 的主数据。

## 数据归属

`plugin-integration` 拥有以下主数据：PluginProvider、PluginInstanceSnapshot、PluginCapability、PluginEventSchema、PluginEvent、PluginRouteRule、PluginSyncTask、PluginHealthSnapshot、PluginObjectMapping、PluginAuditLog 和幂等记录。

`plugin-integration` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和用户状态快照；可以保存来自 `ops-control` 的节点和 Minecraft 实例只读摘要；可以保存来自 `external-node-executor` 的安全回写摘要；可以保存来自 `server-status` 的公开实例状态摘要；可以保存来自 `online-map` 的 provider、world、layer、marker 和 region 引用摘要；可以保存来自 `changelog` 的插件版本变更摘要；可以保存来自 `notification` 的投递结果摘要；可以保存供 `alerting` 消费的插件健康和事件异常摘要。所有快照只用于展示、过滤、降级、审计和后续同步建议，不能成为来源模块主数据，也不能反写来源模块。

`plugin-integration` 不能直接读取其他服务数据库，不能导入前序模块 Java package，不能复用前序模块内存 store，不能调用 `external-node-executor` 执行真实命令，不能读取插件目录、世界目录、服务端配置、RCON 密码或节点文件，不能保存 Cloudreve token、Webhook secret、Discord token、插件后台密码、完整请求头、内部 URL 或绝对路径。

## 基础路径、端口和认证

所有接口默认使用 `/api/v1/plugin-integration` 前缀。第四批合并后当前运行入口为 `ops-core-service:8133`。历史原服务端口 `8122` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或测试入口。

健康检查 `GET /api/v1/plugin-integration/health` 不要求认证，只返回 `service`、`version`、`status` 和 `requestId`，不得返回 provider 数量、插件 endpoint、内部依赖错误、事件 payload、内部 URL 或敏感字段。

后台接口使用 `/api/v1/plugin-integration/admin` 前缀，全部要求 `Authorization: Bearer <token>`。后台读取接口要求基础角色为 `HELPER`、`ADMIN` 或 `OWNER`，且具备 `NODE_READ`。后台写接口要求 `ADMIN` 或 `OWNER`，且具备 `NODE_WRITE`。涉及外部 endpoint、来源 allowlist、公开对象映射、provider 启用禁用、事件重放、路由规则启用、同步任务创建和高风险同步策略时要求 `HIGH_RISK_APPROVE` 或 `OWNER`，或携带固定二次确认文本。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`beforeState`、`afterState`、`auditResult`、`createdBy`、`updatedBy`、`enabledBy`、`disabledBy`、`archivedBy`、`rawPayload`、`rawPayloadStored`、`rawToken`、`pluginToken`、`pluginSecret`、`webhookSecret`、`discordToken`、`nodeToken`、`credential`、`secretKey`、`internalUrl`、`internalPath`、`resolvedPath`、`worldDirectory`、`serverPassword`、`Authorization`、`requestHeaders` 和 `fullException` 等服务端可信字段。可信字段必须递归检查，嵌套在 `payload`、`payloadSummary`、`samplePayloadSummary`、`sourceRef`、`targetRef`、`paramsSummary`、`metadata`、`matchers` 或任意数组对象中也必须拒绝。出现可信字段时返回 `40001`。

## 本地测试控制头

`plugin-integration` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Ops-Control-Mode`、`X-Test-External-Node-Executor-Mode`、`X-Test-Server-Status-Mode`、`X-Test-Online-Map-Mode`、`X-Test-Changelog-Mode`、`X-Test-Notification-Mode`、`X-Test-Alerting-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Now` 模拟认证失败、依赖不可用、依赖超时、依赖坏 schema、通知失败、审计失败、状态写入失败和时间边界。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发认证失败、依赖失败、通知失败、审计失败、存储失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 纳入生产化硬化项。

## 前序模块兼容契约

`auth` 是后台接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。auth 不可用返回 `47050`，auth 超时返回 `47051`，字段或枚举不兼容返回 `47052`。

`ops-control` 是 Minecraft 实例、节点和资产摘要来源。`plugin-integration` 只能读取实例和节点快照，不能创建运维任务，不能通过 ops-control 执行命令，不能修改容器、文件或实例状态。ops-control 不可用返回 `47060`，超时返回 `47061`，schema 不兼容返回 `47062`。ops-control 不可用时，读取类接口可以使用已有快照并标记 `stale=true`；写入类同步任务不得假装成功。

`external-node-executor` 是后续真实节点执行边界。第一版只保存 external-node-executor 安全摘要或使用本服务测试适配器，不直接调用真实节点，不执行插件命令，不写插件配置。external-node-executor 不可用返回 `47070`，超时返回 `47071`，schema 不兼容返回 `47072`。

`server-status` 是玩家可见状态来源。插件上报的在线人数、TPS、MOTD 或延迟不能直接覆盖 server-status 主数据，只能作为插件事件或指标摘要保存。server-status 不可用返回 `47075`，超时返回 `47076`，schema 不兼容返回 `47077`。

`online-map` 是地图对象主数据来源。`plugin-integration` 可以把插件事件转换为地图对象同步建议或对象映射摘要，但不能直接改 online-map 内存 store。需要创建或更新地图 marker、region、provider 时，必须走 online-map 正式 API，并保存调用结果摘要。第一版默认只做映射预览和模拟同步。online-map 不可用返回 `47080`，超时返回 `47081`，schema 不兼容返回 `47082`。

`changelog` 可以提供插件版本变更摘要。`plugin-integration` 不发布 changelog，不修改 changelog 状态，只保存版本引用快照。changelog 不可用返回 `47085`，超时返回 `47086`，schema 不兼容返回 `47087`。

`notification` 是通知投递依赖。插件异常、同步失败、事件 schema 失配、provider 禁用和高风险路由变更可以触发通知摘要。通知失败不回滚插件事件主状态，但必须记录脱敏失败摘要。notification 不可用返回 `47090`，超时返回 `47091`，schema 不兼容返回 `47092`。

`alerting` 是后续消费方。`plugin-integration` 第一版只暴露健康、自检、事件异常和同步失败摘要，不直接创建 alerting 规则，也不直接关闭告警。alerting 不可用返回 `47100`，超时返回 `47101`，schema 不兼容返回 `47102`。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `PluginProviderType` | `PAPER`、`SPIGOT`、`BUKKIT`、`VELOCITY`、`BLUEMAP`、`DYNMAP`、`SQUAREMAP`、`LUCKPERMS`、`PLACEHOLDER_API`、`DISCORDSRV`、`PROMETHEUS_EXPORTER`、`MODRINTH`、`CURSEFORGE`、`HANGAR`、`CUSTOM` | 插件来源或平台类型。 |
| `PluginServerKind` | `SERVER`、`PROXY`、`MAP_PROVIDER`、`PERMISSION_PROVIDER`、`NOTIFICATION_BRIDGE`、`METRIC_EXPORTER`、`MARKETPLACE`、`CUSTOM` | 插件运行或来源类别。 |
| `PluginProviderStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`DEGRADED`、`ARCHIVED` | provider 控制面状态。 |
| `PluginHealthStatus` | `ONLINE`、`DEGRADED`、`OFFLINE`、`UNKNOWN` | 插件或 provider 健康状态。 |
| `PluginSchemaStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`ARCHIVED` | 事件 schema 状态。 |
| `PluginEventValidationStatus` | `RECEIVED`、`VALIDATED`、`REJECTED` | 事件校验状态。 |
| `PluginEventRouteStatus` | `PENDING`、`ROUTED`、`IGNORED`、`FAILED` | 事件路由状态。 |
| `PluginEventSyncStatus` | `SKIPPED`、`QUEUED`、`SIMULATED_BLOCKED`、`SYNCED`、`FAILED` | 同步状态。第一版真实写入默认 `SIMULATED_BLOCKED`。 |
| `PluginRouteRuleStatus` | `ENABLED`、`DISABLED`、`ARCHIVED` | 路由规则状态。 |
| `PluginRouteTargetModule` | `ONLINE_MAP`、`SERVER_STATUS`、`CHANGELOG`、`NOTIFICATION`、`ALERTING`、`OPS_CONTROL`、`PLUGIN_INTEGRATION` | 路由目标模块。 |
| `PluginSyncTaskStatus` | `QUEUED`、`RUNNING`、`SUCCEEDED`、`FAILED`、`CANCELED`、`TIMEOUT`、`SIMULATED_BLOCKED` | 同步任务状态。 |
| `PluginObjectMappingStatus` | `ACTIVE`、`STALE`、`CONFLICTED`、`ARCHIVED` | 插件对象映射状态。 |
| `PluginObjectVisibility` | `PUBLIC`、`MEMBER_ONLY`、`STAFF_ONLY`、`PRIVATE` | 映射对象可见范围。 |
| `PluginDependencyStatus` | `AVAILABLE`、`UNAVAILABLE`、`TIMEOUT`、`BAD_SCHEMA`、`STALE`、`SKIPPED` | 依赖摘要状态。 |
| `PluginAuditResult` | `SUCCESS`、`FAILED` | 审计结果。 |

## 通用对象

### PluginProvider

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `providerId` | string | 是 | provider ID。 |
| `providerType` | string | 是 | `PluginProviderType`。 |
| `displayName` | string | 是 | 展示名称，2 到 80 位。 |
| `pluginName` | string | 是 | 插件或平台项目名，1 到 80 位。 |
| `pluginVersion` | string 或 null | 是 | 插件版本摘要。 |
| `serverKind` | string | 是 | `PluginServerKind`。 |
| `instanceRef` | object 或 null | 是 | ops-control Minecraft 实例只读引用摘要。 |
| `nodeRef` | object 或 null | 是 | ops-control 节点只读引用摘要。 |
| `status` | string | 是 | `PluginProviderStatus`。 |
| `publicVisible` | boolean | 是 | 是否允许出现在公开或用户侧展示。第一版无公开 provider 列表，仅用于后续适配。 |
| `eventEndpointSummary` | string 或 null | 是 | 脱敏事件入口摘要，不返回完整内部 URL 或 token。 |
| `allowedEventTypes` | string[] | 是 | 允许事件类型，最多 100 个。 |
| `allowedOrigins` | string[] | 是 | 允许事件来源摘要，最多 20 个，不允许 `*`、localhost、内网地址和文件协议。 |
| `healthStatus` | string | 是 | 最近健康状态。 |
| `lastEventAt` | string 或 null | 是 | 最近事件接收时间。 |
| `lastSyncAt` | string 或 null | 是 | 最近同步任务时间。 |
| `degradeReasons` | string[] | 是 | 降级原因，必须脱敏。 |
| `adminNote` | string 或 null | 后台可见 | 后台备注，最多 1000 位，不得进入公开响应。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### PluginInstanceSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `instanceId` | string | 是 | 插件实例快照 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `opsInstanceRef` | object 或 null | 是 | ops-control 实例摘要。 |
| `serverStatusRef` | object 或 null | 是 | server-status 公开实例摘要。 |
| `pluginName` | string | 是 | 插件名。 |
| `pluginVersion` | string 或 null | 是 | 插件版本。 |
| `serverVersion` | string 或 null | 是 | 服务端版本。 |
| `loaded` | boolean | 是 | 插件是否被加载。 |
| `enabled` | boolean | 是 | 插件是否启用。 |
| `dependencyPlugins` | string[] | 是 | 插件依赖摘要。 |
| `capabilities` | PluginCapability[] | 是 | 能力声明。 |
| `metricsSummary` | object | 是 | 指标摘要，必须脱敏。 |
| `lastSeenAt` | string 或 null | 是 | 最近上报时间。 |
| `stale` | boolean | 是 | 是否使用过期快照。 |

### PluginCapability

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `capabilityId` | string | 是 | 能力 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `namespace` | string | 是 | 命名空间，1 到 60 位。 |
| `name` | string | 是 | 能力名，1 到 80 位。 |
| `version` | string 或 null | 是 | 能力版本。 |
| `riskLevel` | string | 是 | 公共风险等级。 |
| `available` | boolean | 是 | 当前是否可用。 |
| `summary` | object | 是 | 能力安全摘要。 |

### PluginEventSchema

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `schemaId` | string | 是 | schema ID。 |
| `eventType` | string | 是 | 事件类型，3 到 120 位，建议使用 `namespace.event`。 |
| `sourcePlugin` | string | 是 | 来源插件名。 |
| `version` | string | 是 | schema 版本。 |
| `status` | string | 是 | `PluginSchemaStatus`。 |
| `requiredFields` | string[] | 是 | 必填字段，最多 100 个。 |
| `optionalFields` | string[] | 是 | 可选字段，最多 100 个。 |
| `sensitiveFields` | string[] | 是 | 需要脱敏或拒绝保存的字段，最多 100 个。 |
| `routingHints` | object | 是 | 路由提示摘要。 |
| `samplePayloadSummary` | object | 是 | 样例 payload 脱敏摘要，不得含 raw payload。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### PluginEvent

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `eventId` | string | 是 | 事件 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `eventType` | string | 是 | 事件类型。 |
| `schemaId` | string 或 null | 是 | 命中的 schema ID。 |
| `sourcePlugin` | string | 是 | 来源插件。 |
| `sourceInstanceId` | string 或 null | 是 | 插件实例快照 ID。 |
| `dedupeKey` | string | 是 | 事件去重键。 |
| `payloadSummary` | object | 是 | 结构化脱敏摘要。 |
| `rawPayloadStored` | boolean | 是 | 第一版固定 `false`。 |
| `validationStatus` | string | 是 | `PluginEventValidationStatus`。 |
| `routeStatus` | string | 是 | `PluginEventRouteStatus`。 |
| `syncStatus` | string | 是 | `PluginEventSyncStatus`。 |
| `notificationStatus` | string | 是 | `SKIPPED`、`DELIVERED` 或 `FAILED`。 |
| `receivedAt` | string | 是 | 接收时间。 |
| `processedAt` | string 或 null | 是 | 处理时间。 |
| `failureReason` | string 或 null | 是 | 脱敏失败原因。 |
| `requestId` | string | 是 | HTTP 请求编号。 |

### PluginRouteRule

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ruleId` | string | 是 | 路由规则 ID。 |
| `displayName` | string | 是 | 规则名称，2 到 80 位。 |
| `eventType` | string | 是 | 匹配事件类型。 |
| `matchers` | object | 是 | 匹配器，第一版支持 `providerId`、`sourcePlugin`、`eventType`、`payload` 精确匹配。 |
| `targetModule` | string | 是 | `PluginRouteTargetModule`。 |
| `targetAction` | string | 是 | 目标动作摘要，1 到 80 位。 |
| `enabled` | boolean | 是 | 是否启用。 |
| `riskLevel` | string | 是 | 公共风险等级。 |
| `rateLimitSummary` | object | 是 | 限流摘要。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### PluginSyncTask

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `taskId` | string | 是 | 同步任务 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `eventId` | string 或 null | 是 | 来源事件 ID。 |
| `targetModule` | string | 是 | 目标模块。 |
| `targetAction` | string | 是 | 目标动作。 |
| `status` | string | 是 | `PluginSyncTaskStatus`。 |
| `riskLevel` | string | 是 | 公共风险等级。 |
| `paramsSummary` | object | 是 | 脱敏参数摘要。 |
| `resultSummary` | object 或 null | 是 | 结果摘要。 |
| `failureReason` | string 或 null | 是 | 脱敏失败原因。 |
| `idempotencyKey` | string 或 null | 是 | 幂等键。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `expiresAt` | string | 是 | 任务过期时间。 |

### PluginHealthSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `snapshotId` | string | 是 | 健康快照 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `healthStatus` | string | 是 | `PluginHealthStatus`。 |
| `dependencyStatus` | object | 是 | 依赖摘要。 |
| `metricsSummary` | object | 是 | 指标摘要。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |
| `checkedAt` | string | 是 | 检查时间。 |

### PluginObjectMapping

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `mappingId` | string | 是 | 映射 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `sourcePlugin` | string | 是 | 来源插件。 |
| `sourceObjectType` | string | 是 | 来源对象类型，1 到 80 位。 |
| `sourceObjectKey` | string | 是 | 来源对象 key，1 到 160 位。 |
| `targetModule` | string | 是 | 目标模块。 |
| `targetObjectType` | string | 是 | 目标对象类型。 |
| `targetObjectId` | string | 是 | 目标对象 ID。 |
| `status` | string | 是 | `PluginObjectMappingStatus`。 |
| `visibility` | string | 是 | `PluginObjectVisibility`。 |
| `lastSyncedAt` | string 或 null | 是 | 最近同步时间。 |
| `syncHash` | string 或 null | 是 | 同步摘要 hash。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### PluginAuditLog

审计字段继承公共契约，允许补充 `providerId`、`eventId`、`schemaId`、`ruleId`、`taskId`、`mappingId`、`dependencyStatus`、`notificationStatus`、`idempotencyKey` 和 `failureReason`。审计列表不得提供删除、修改或恢复接口。审计响应不得返回 token、密钥、webhook secret、完整请求头、完整 payload、内部 URL、内部路径、真实世界目录、节点地址、完整异常栈或前序模块私有数据。

### PluginIntegrationOpsSummary

自检摘要至少包含 `service`、`port`、`storageMode`、`authMode`、`opsControlAdapterMode`、`externalNodeExecutorAdapterMode`、`onlineMapAdapterMode`、`notificationAdapterMode`、`alertingAdapterMode`、`testControlsEnabled`、`providersTotal`、`enabledProvidersTotal`、`instancesTotal`、`schemasTotal`、`eventsTotal`、`routeRulesTotal`、`syncTasksTotal`、`objectMappingsTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastEventAt`、`lastSyncAt`、`degraded`、`degradeReasons` 和 `productionGaps`。

## 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `47050` | 502 | auth 认证上下文不可用。 |
| `47051` | 504 | auth 认证上下文调用超时。 |
| `47052` | 502 | auth 字段或枚举不兼容。 |
| `47060` | 502 | ops-control 不可用。 |
| `47061` | 504 | ops-control 超时。 |
| `47062` | 502 | ops-control schema 不兼容。 |
| `47070` | 502 | external-node-executor 不可用。 |
| `47071` | 504 | external-node-executor 超时。 |
| `47072` | 502 | external-node-executor schema 不兼容。 |
| `47075` | 502 | server-status 不可用。 |
| `47076` | 504 | server-status 超时。 |
| `47077` | 502 | server-status schema 不兼容。 |
| `47080` | 502 | online-map 不可用。 |
| `47081` | 504 | online-map 超时。 |
| `47082` | 502 | online-map schema 不兼容。 |
| `47085` | 502 | changelog 不可用。 |
| `47086` | 504 | changelog 超时。 |
| `47087` | 502 | changelog schema 不兼容。 |
| `47090` | 502 | notification 不可用。 |
| `47091` | 504 | notification 超时。 |
| `47092` | 502 | notification schema 不兼容。 |
| `47100` | 502 | alerting 不可用。 |
| `47101` | 504 | alerting 超时。 |
| `47102` | 502 | alerting schema 不兼容。 |
| `49800` | 404 | provider 不存在。 |
| `49801` | 404 | 插件实例不存在。 |
| `49802` | 404 | 事件 schema 不存在。 |
| `49803` | 404 | 插件事件不存在。 |
| `49804` | 404 | 路由规则不存在。 |
| `49805` | 404 | 同步任务不存在。 |
| `49806` | 404 | 对象映射不存在。 |
| `49810` | 409 | 状态不允许当前操作。 |
| `49811` | 409 | provider、schema、路由规则或对象映射冲突。 |
| `49812` | 409 | 幂等键请求指纹冲突。 |
| `49813` | 400 | 插件 endpoint、来源或外部 URL 不安全。 |
| `49814` | 400 | 事件 payload 不符合 schema。 |
| `49815` | 403 | 事件签名、来源或类型不允许。 |
| `49816` | 409 | 事件重放窗口已过、重复或状态不允许。 |
| `49817` | 409 | 同步目标不允许或第一版真实同步被阻断。 |
| `55700` | 500 | plugin-integration 内部错误。 |
| `55701` | 500 | 审计写入失败。 |
| `55702` | 500 | 状态写入失败。 |
| `55703` | 500 | 事件写入失败。 |
| `55704` | 500 | 同步任务写入失败。 |

字段校验、未登录、令牌格式错误、角色权限不足、能力点不足、高风险确认缺失、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。plugin-integration 自有幂等指纹冲突使用 `49812`。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/plugin-integration/health` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/plugin-integration/admin/ops/summary` | 是 | `NODE_READ` | LOW |
| provider 列表 | GET | `/api/v1/plugin-integration/admin/providers` | 是 | `NODE_READ` | LOW |
| provider 详情 | GET | `/api/v1/plugin-integration/admin/providers/{providerId}` | 是 | `NODE_READ` | LOW |
| 创建 provider | POST | `/api/v1/plugin-integration/admin/providers` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 修改 provider | PATCH | `/api/v1/plugin-integration/admin/providers/{providerId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 启用 provider | PATCH | `/api/v1/plugin-integration/admin/providers/{providerId}/enable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | HIGH |
| 禁用 provider | PATCH | `/api/v1/plugin-integration/admin/providers/{providerId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档 provider | PATCH | `/api/v1/plugin-integration/admin/providers/{providerId}/archive` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | HIGH |
| 插件实例列表 | GET | `/api/v1/plugin-integration/admin/instances` | 是 | `NODE_READ` | LOW |
| 插件实例详情 | GET | `/api/v1/plugin-integration/admin/instances/{instanceId}` | 是 | `NODE_READ` | LOW |
| 插件能力列表 | GET | `/api/v1/plugin-integration/admin/capabilities` | 是 | `NODE_READ` | LOW |
| schema 列表 | GET | `/api/v1/plugin-integration/admin/event-schemas` | 是 | `NODE_READ` | LOW |
| schema 详情 | GET | `/api/v1/plugin-integration/admin/event-schemas/{schemaId}` | 是 | `NODE_READ` | LOW |
| 创建 schema | POST | `/api/v1/plugin-integration/admin/event-schemas` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 修改 schema | PATCH | `/api/v1/plugin-integration/admin/event-schemas/{schemaId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 启用 schema | PATCH | `/api/v1/plugin-integration/admin/event-schemas/{schemaId}/enable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 禁用 schema | PATCH | `/api/v1/plugin-integration/admin/event-schemas/{schemaId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 事件接收 | POST | `/api/v1/plugin-integration/admin/events/ingest` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 事件列表 | GET | `/api/v1/plugin-integration/admin/events` | 是 | `NODE_READ` | LOW |
| 事件详情 | GET | `/api/v1/plugin-integration/admin/events/{eventId}` | 是 | `NODE_READ` | LOW |
| 事件重放 | POST | `/api/v1/plugin-integration/admin/events/{eventId}/replay` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE`，高风险时 `HIGH_RISK_APPROVE` | HIGH |
| 路由规则列表 | GET | `/api/v1/plugin-integration/admin/route-rules` | 是 | `NODE_READ` | LOW |
| 路由规则详情 | GET | `/api/v1/plugin-integration/admin/route-rules/{ruleId}` | 是 | `NODE_READ` | LOW |
| 创建路由规则 | POST | `/api/v1/plugin-integration/admin/route-rules` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 修改路由规则 | PATCH | `/api/v1/plugin-integration/admin/route-rules/{ruleId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 启用路由规则 | PATCH | `/api/v1/plugin-integration/admin/route-rules/{ruleId}/enable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 禁用路由规则 | PATCH | `/api/v1/plugin-integration/admin/route-rules/{ruleId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 创建同步任务 | POST | `/api/v1/plugin-integration/admin/sync-tasks` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 同步任务列表 | GET | `/api/v1/plugin-integration/admin/sync-tasks` | 是 | `NODE_READ` | LOW |
| 同步任务详情 | GET | `/api/v1/plugin-integration/admin/sync-tasks/{taskId}` | 是 | `NODE_READ` | LOW |
| 取消同步任务 | PATCH | `/api/v1/plugin-integration/admin/sync-tasks/{taskId}/cancel` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 健康快照列表 | GET | `/api/v1/plugin-integration/admin/providers/{providerId}/health-snapshots` | 是 | `NODE_READ` | LOW |
| 对象映射列表 | GET | `/api/v1/plugin-integration/admin/object-mappings` | 是 | `NODE_READ` | LOW |
| 对象映射详情 | GET | `/api/v1/plugin-integration/admin/object-mappings/{mappingId}` | 是 | `NODE_READ` | LOW |
| 创建或更新对象映射 | PUT | `/api/v1/plugin-integration/admin/object-mappings/{mappingId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 归档对象映射 | PATCH | `/api/v1/plugin-integration/admin/object-mappings/{mappingId}/archive` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 审计列表 | GET | `/api/v1/plugin-integration/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 后台 provider 接口

`GET /api/v1/plugin-integration/admin/ops/summary` 返回 `PluginIntegrationOpsSummary`。摘要不得返回 token、请求头、内部 URL、真实路径、插件后台密码、完整 payload 或异常栈。读取失败返回 `55700`。

`GET /api/v1/plugin-integration/admin/providers` 支持 `page`、`pageSize`、`keyword`、`providerType`、`serverKind`、`status`、`healthStatus`、`publicVisible`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`、`lastEventAt_desc`、`lastSyncAt_desc`。成功响应分页 `items` 为 `PluginProvider[]`。

`GET /api/v1/plugin-integration/admin/providers/{providerId}` 返回 provider、最近健康快照、实例摘要、能力摘要、最近事件、依赖摘要和最近审计。provider 不存在返回 `49800`。

`POST /api/v1/plugin-integration/admin/providers` 请求字段包括 `providerType`、`displayName`、`pluginName`、`pluginVersion`、`serverKind`、`instanceRef`、`nodeRef`、`publicVisible`、`eventEndpointSummary`、`allowedEventTypes`、`allowedOrigins`、`adminNote`、`reason` 和 `idempotencyKey`。创建默认状态为 `DRAFT`，成功响应 HTTP `201`。`providerType`、`serverKind` 和后续状态枚举必须命中本文档枚举表，非法枚举返回 `40001`。同一未归档 provider 下 `displayName`、`pluginName + serverKind + instanceRef` 或规范化 `eventEndpointSummary` 冲突返回 `49811`。外部 endpoint、allowed origins 或来源不安全返回 `49813`。涉及公开来源、外部 endpoint 或 allowlist 时必须携带 `confirmText=REGISTER_PLUGIN_PROVIDER_ENDPOINT`，否则返回 `42003`。

`PATCH /api/v1/plugin-integration/admin/providers/{providerId}` 可修改创建字段中的业务字段，`reason` 必填。修改 `eventEndpointSummary`、`allowedOrigins`、`allowedEventTypes` 或把 `publicVisible` 从 `false` 改为 `true` 属于 `HIGH` 风险，必须携带 `confirmText=UPDATE_PLUGIN_PROVIDER_ENDPOINT`。`ARCHIVED` provider 不允许修改，返回 `49810`。

`PATCH /api/v1/plugin-integration/admin/providers/{providerId}/enable` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ENABLE_PLUGIN_PROVIDER`。`DRAFT`、`DISABLED` 和 `DEGRADED` 可流转为 `ENABLED`。启用时必须校验 allowed event types 非空、allowed origins 均安全、至少存在一个同 provider 的 `ENABLED` schema 或一个可用实例摘要。不满足字段完整性返回 `40001`，不满足状态或依赖前置返回 `49810`。重复启用保持幂等。

`PATCH /api/v1/plugin-integration/admin/providers/{providerId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 或 `DEGRADED` 可流转为 `DISABLED`。禁用后事件接收返回 `49815` 或 `49810`。重复禁用保持幂等。

`PATCH /api/v1/plugin-integration/admin/providers/{providerId}/archive` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ARCHIVE_PLUGIN_PROVIDER`。只有 `DRAFT`、`DISABLED` 或 `DEGRADED` 可归档；`ENABLED` provider 必须先禁用后归档；仍有活动路由规则、未终态同步任务或 `ACTIVE` 对象映射时返回 `49810`，不得自动静默归档子对象。`ARCHIVED` 为终态。

## 插件实例和能力接口

`GET /api/v1/plugin-integration/admin/instances` 支持 `page`、`pageSize`、`providerId`、`pluginName`、`loaded`、`enabled`、`stale`、`keyword` 和 `sort`。`sort` 允许 `lastSeenAt_desc`、`pluginName_asc`、`pluginVersion_desc`。成功响应分页 `items` 为 `PluginInstanceSnapshot[]`。实例快照只读，第一版通过种子或事件接收更新摘要，不提供后台直接写接口。

`GET /api/v1/plugin-integration/admin/instances/{instanceId}` 返回实例详情、能力列表、最近事件和降级摘要。实例不存在返回 `49801`。

`GET /api/v1/plugin-integration/admin/capabilities` 支持 `page`、`pageSize`、`providerId`、`namespace`、`riskLevel`、`available`、`keyword` 和 `sort`。成功响应分页 `items` 为 `PluginCapability[]`。能力来源于 provider 和实例快照，不可由浏览器写入服务端可信字段。

## 事件 schema 接口

`GET /api/v1/plugin-integration/admin/event-schemas` 支持 `page`、`pageSize`、`providerId`、`eventType`、`sourcePlugin`、`status`、`keyword` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`eventType_asc`。成功响应分页 `items` 为 `PluginEventSchema[]`。

`GET /api/v1/plugin-integration/admin/event-schemas/{schemaId}` 返回 schema 详情和最近校验摘要。不存在返回 `49802`。

`POST /api/v1/plugin-integration/admin/event-schemas` 请求字段包括 `providerId`、`eventType`、`sourcePlugin`、`version`、`requiredFields`、`optionalFields`、`sensitiveFields`、`routingHints`、`samplePayloadSummary`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，默认状态为 `DRAFT`。同一 provider、eventType、sourcePlugin 和 version 的未归档 schema 冲突返回 `49811`。`sensitiveFields` 不得为空时却让样例摘要含敏感值，违规返回 `40001`。

`PATCH /api/v1/plugin-integration/admin/event-schemas/{schemaId}` 可修改 schema 字段，`reason` 必填。`ARCHIVED` schema 不允许修改。启用中的 schema 修改后仍保持原状态，但必须通过字段和敏感字段校验。

`PATCH /api/v1/plugin-integration/admin/event-schemas/{schemaId}/enable` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT` 或 `DISABLED` 可启用为 `ENABLED`。只有 `ENABLED` schema 可用于事件校验。

`PATCH /api/v1/plugin-integration/admin/event-schemas/{schemaId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 可禁用为 `DISABLED`。重复禁用保持幂等。

## 事件接口

`POST /api/v1/plugin-integration/admin/events/ingest` 请求字段包括 `providerId`、`eventType`、`sourcePlugin`、`sourceInstanceId`、`dedupeKey`、`payload`、`occurredAt`、`reason` 和 `idempotencyKey`。第一版只允许后台受控 token 或测试适配器模拟，不开放无鉴权公网 webhook。provider 必须为 `ENABLED`，事件类型必须在 `allowedEventTypes` 中，来源必须命中 `allowedOrigins` 或后台受控来源。事件必须命中 `ENABLED` schema，且 payload 必须包含 `requiredFields`，不得包含 `sensitiveFields`、服务端可信字段、内部路径、token 或完整请求头。成功响应 HTTP `201`，`rawPayloadStored=false`，只保存脱敏 `payloadSummary`。schema 不存在返回 `49802`，payload 不符合 schema 返回 `49814`，来源或类型不允许返回 `49815`，事件写入失败返回 `55703`。

`GET /api/v1/plugin-integration/admin/events` 支持 `page`、`pageSize`、`providerId`、`eventType`、`sourcePlugin`、`validationStatus`、`routeStatus`、`syncStatus`、`notificationStatus`、`from`、`to`、`keyword` 和 `sort`。`sort` 允许 `receivedAt_desc`、`receivedAt_asc`、`processedAt_desc`。成功响应分页 `items` 为 `PluginEvent[]`。

`GET /api/v1/plugin-integration/admin/events/{eventId}` 返回事件详情、路由摘要、同步任务摘要和审计摘要。事件不存在返回 `49803`。响应不得返回 raw payload、token、内部 URL、内部路径、完整请求头或异常堆栈。

`POST /api/v1/plugin-integration/admin/events/{eventId}/replay` 请求字段包括 `reason`、`confirmText`、`idempotencyKey` 和可选 `targetRuleIds`。`confirmText` 必须为 `REPLAY_PLUGIN_EVENT`。只有 `VALIDATED` 或 `REJECTED` 后经修复且仍在 7 天窗口内的事件可重放；重复重放同一幂等键返回同一结果；窗口过期或事件状态不允许返回 `49816`。`targetRuleIds` 传入时每个规则必须存在、启用、事件类型匹配且不得指向 `OPS_CONTROL`，否则返回 `49804`、`49810` 或 `49817`。重放不修改原事件 payload，只创建新的处理摘要、同步任务或失败摘要。

## 路由规则接口

`GET /api/v1/plugin-integration/admin/route-rules` 支持 `page`、`pageSize`、`eventType`、`targetModule`、`enabled`、`riskLevel`、`keyword` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`。成功响应分页 `items` 为 `PluginRouteRule[]`。

`GET /api/v1/plugin-integration/admin/route-rules/{ruleId}` 返回规则详情、最近命中事件和最近同步摘要。不存在返回 `49804`。

`POST /api/v1/plugin-integration/admin/route-rules` 请求字段包括 `displayName`、`eventType`、`matchers`、`targetModule`、`targetAction`、`enabled`、`riskLevel`、`rateLimitSummary`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`。`targetModule` 和 `riskLevel` 必须命中本文档枚举表，非法枚举返回 `40001`。同一 eventType、targetModule、targetAction 和 matcher 指纹冲突返回 `49811`。`targetModule=ONLINE_MAP` 且会公开创建对象、`targetModule=OPS_CONTROL`、`riskLevel=HIGH` 或 `CRITICAL` 时必须携带 `confirmText=CONFIGURE_PLUGIN_ROUTE`，否则返回 `42003`。第一版不允许真实写 `OPS_CONTROL`，对应创建或启用返回 `49817` 或保存为禁用规则。

`PATCH /api/v1/plugin-integration/admin/route-rules/{ruleId}` 可修改规则字段，`reason` 必填。修改 target、matcher、riskLevel 或启用高风险路由必须携带 `confirmText=UPDATE_PLUGIN_ROUTE`。`ARCHIVED` 规则不可修改。

`PATCH /api/v1/plugin-integration/admin/route-rules/{ruleId}/enable` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。中低风险规则无需 confirmText，高风险规则必须为 `ENABLE_PLUGIN_ROUTE`。`DISABLED` 可启用为 `ENABLED`。重复启用保持幂等。

`PATCH /api/v1/plugin-integration/admin/route-rules/{ruleId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 可禁用为 `DISABLED`。禁用不会删除历史事件和同步任务。

## 同步任务接口

`POST /api/v1/plugin-integration/admin/sync-tasks` 请求字段包括 `providerId`、`eventId`、`targetModule`、`targetAction`、`params`、`riskLevel`、`reason`、`confirmText` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `PluginSyncTask`。`providerId` 和 `eventId` 必须存在且属于同一 provider，不存在返回 `49800` 或 `49803`，不匹配返回 `49810`。`targetModule` 和 `riskLevel` 必须命中本文档枚举表，非法枚举返回 `40001`。第一版对真实写 `ONLINE_MAP`、`NOTIFICATION`、`ALERTING` 以外的目标默认返回 `SIMULATED_BLOCKED` 或 `49817`，不得伪造真实成功。`riskLevel=HIGH` 或目标会公开对象时必须携带 `confirmText=CREATE_PLUGIN_SYNC_TASK`。同步任务写入失败返回 `55704`。

`GET /api/v1/plugin-integration/admin/sync-tasks` 支持 `page`、`pageSize`、`providerId`、`eventId`、`targetModule`、`status`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`riskLevel_desc`。成功响应分页 `items` 为 `PluginSyncTask[]`。

`GET /api/v1/plugin-integration/admin/sync-tasks/{taskId}` 返回任务详情、事件摘要、目标摘要和依赖摘要。不存在返回 `49805`。

`PATCH /api/v1/plugin-integration/admin/sync-tasks/{taskId}/cancel` 请求字段为 `reason` 和 `idempotencyKey`。只有 `QUEUED`、`RUNNING` 或 `SIMULATED_BLOCKED` 可取消。`SUCCEEDED`、`FAILED`、`CANCELED` 和 `TIMEOUT` 为终态，重复取消按状态冲突返回 `49810`。

## 健康快照和对象映射接口

`GET /api/v1/plugin-integration/admin/providers/{providerId}/health-snapshots` 支持 `page`、`pageSize`、`healthStatus`、`from`、`to` 和 `sort`。`sort` 允许 `checkedAt_desc`、`checkedAt_asc`。provider 不存在返回 `49800`。成功响应分页 `items` 为 `PluginHealthSnapshot[]`。

`GET /api/v1/plugin-integration/admin/object-mappings` 支持 `page`、`pageSize`、`providerId`、`sourcePlugin`、`sourceObjectType`、`targetModule`、`targetObjectType`、`status`、`visibility`、`keyword` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`lastSyncedAt_desc`。成功响应分页 `items` 为 `PluginObjectMapping[]`。

`GET /api/v1/plugin-integration/admin/object-mappings/{mappingId}` 返回映射详情和最近同步摘要。不存在返回 `49806`。

`PUT /api/v1/plugin-integration/admin/object-mappings/{mappingId}` 请求字段包括 `providerId`、`sourcePlugin`、`sourceObjectType`、`sourceObjectKey`、`targetModule`、`targetObjectType`、`targetObjectId`、`status`、`visibility`、`syncHash`、`reason` 和 `idempotencyKey`。成功响应 HTTP `200` 或首次创建 HTTP `201`。同一 provider、sourcePlugin、sourceObjectType、sourceObjectKey 不能映射到不同未归档 target，冲突返回 `49811`。公开映射或 `targetModule=ONLINE_MAP` 且 `visibility=PUBLIC` 时必须携带 `confirmText=UPSERT_PLUGIN_OBJECT_MAPPING`。

`PATCH /api/v1/plugin-integration/admin/object-mappings/{mappingId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。`ACTIVE`、`STALE` 或 `CONFLICTED` 可归档为 `ARCHIVED`。归档后不再被同步任务自动选中。重复归档保持幂等。

## 审计接口

`GET /api/v1/plugin-integration/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`providerId`、`eventId`、`schemaId`、`ruleId`、`taskId`、`mappingId`、`result`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`、`riskLevel_desc`。只有 `ADMIN` 和 `OWNER` 可访问。审计列表只读，不提供删除、修改或恢复接口。

后台写操作必须记录调用者、`reason`、操作前状态、操作后状态、请求编号、结果和失败原因。审计写入失败时，provider、schema、事件、路由规则、同步任务、对象映射和健康快照的写操作不得假装成功，必须返回 `55701` 并保持业务状态不变。notification 失败不回滚事件主状态，但必须记录脱敏失败摘要。

## 状态、幂等和并发

provider 状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DEGRADED` 或 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`DEGRADED` 可恢复为 `ENABLED`、禁用或归档；`ARCHIVED` 为终态。

schema 状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

事件状态由校验、路由、同步和通知摘要共同组成。事件接收后先进入 `RECEIVED`，校验通过为 `VALIDATED`，失败为 `REJECTED`。路由状态可为 `PENDING`、`ROUTED`、`IGNORED` 或 `FAILED`。同步状态可为 `SKIPPED`、`QUEUED`、`SIMULATED_BLOCKED`、`SYNCED` 或 `FAILED`。事件一旦保存，不允许直接修改 payload；重放必须使用重放接口生成新的处理摘要。

同步任务状态流转为 `QUEUED` 到 `RUNNING`、`SIMULATED_BLOCKED`、`FAILED` 或 `CANCELED`；`RUNNING` 到 `SUCCEEDED`、`FAILED`、`TIMEOUT` 或 `CANCELED`；`SIMULATED_BLOCKED` 可取消或在后续真实适配后重新入队；`SUCCEEDED`、`FAILED`、`CANCELED` 和 `TIMEOUT` 为终态。

对象映射状态流转为 `ACTIVE` 到 `STALE`、`CONFLICTED` 或 `ARCHIVED`；`STALE` 和 `CONFLICTED` 可回到 `ACTIVE` 或归档；`ARCHIVED` 为终态。

创建、修改、状态流转、事件接收、事件重放、同步任务创建、取消任务、对象映射 upsert 和归档均支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `49812`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序，数组保留顺序，不能依赖 Java `Map.toString()` 或浏览器字段顺序。

并发创建相同 provider、schema、路由规则、事件 dedupeKey、同步任务或对象映射时只能一个成功，其余返回冲突或相同幂等结果。所有写接口必须在同一个临界区内完成状态校验、业务写入、审计写入、幂等记录和响应快照保存。后续数据库实现必须迁移为事务、唯一约束、条件更新或等效机制，不能降低并发口径。

## 安全、降级和脱敏

任何请求体和响应都不得包含访问 token、插件 token、插件 secret、webhook secret、Discord token、SMTP 密码、短信 token、完整 Authorization 请求头、完整请求 headers、完整 raw payload、内部 URL、内部路径、真实世界目录、节点地址、服务器密码、RCON 密码、完整异常栈、数据库连接串、`.env`、`authorized_keys`、`id_rsa`、shell 命令或前序模块私有数据。检查必须递归覆盖嵌套对象和数组。

外部 endpoint 和 allowed origins 必须拒绝 `file:`、`data:`、`javascript:`、带用户名密码 URL、localhost、回环 IP、内网 IP、链路本地地址、未解析 host、通配符 `*`、空 host、控制字符和非法 URI。`eventEndpointSummary` 只能是公开安全摘要或站内受控路径，不得是内网完整地址。站内受控路径必须以 `/` 开头，不能以 `//` 开头，不能包含反斜杠或控制字符。

事件 payload 必须按 schema 脱敏和摘要化。第一版 `rawPayloadStored` 固定为 `false`。`payloadSummary` 只能保存字段名、类型、必要业务摘要和已脱敏值，不得保存完整玩家 IP、完整请求头、外部 token、路径或密钥。

依赖不可用时，读取类接口可以返回已有快照并标记 `stale=true`、`degraded=true` 和 `degradeReasons`。写入类接口不得假装成功。通知失败不得伪造成投递成功。真实同步被第一版阻断时必须返回 `SIMULATED_BLOCKED` 或 `49817`。

第一版不得提供真实删除 provider、schema、事件、规则、任务、映射或审计的接口。确需清理历史记录时，必须在后续独立契约中增加归档或保留策略接口，并重新完成文档、测试红灯、实现和回归闭环。

## 验收口径

`plugin-integration` API 文档必须按 `docs/contracts-plugin-integration.md` 独立存在，并由 `.local-docs/tests-ops-core.md` 记录合并后的本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求、敏感字段脱敏、测试控制头默认关闭和模块验收口径。

`plugin-integration` 完成时必须满足以下条件：当前运行入口为 `ops-core-service:8133`，历史端口 `8122` 只作为 `legacyPort` 返回；健康检查公开且不泄露敏感信息；后台接口按角色和能力点限制；provider、实例、能力、schema、事件、路由规则、同步任务、健康快照、对象映射、审计、幂等、状态流转、来源 allowlist、payload 脱敏、依赖降级、通知失败摘要、审计失败回滚、敏感字段脱敏、测试控制头默认关闭和自检摘要都有自动化验证；自动化测试必须先红灯；实现后 `plugin-integration` 在 `ops-core-service` 中全部测试通过；当前后端运行入口回归测试通过；边界扫描无违规命中；不修改前序模块稳定接口；不直接读取前序模块数据库；不导入前序模块 Java package；不调用真实 `external-node-executor`；不执行真实插件命令；不写真实插件配置；不保存真实插件 token、webhook secret 或外部平台密钥；不把地图主数据、告警规则、通知渠道、资源下载、节点文件管理、终端、备份恢复或跨平台通知主数据塞进 `plugin-integration`。
