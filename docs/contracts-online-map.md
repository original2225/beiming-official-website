# 北冥官网 online-map API 契约

版本：0.2

## 文档定位

本文档是 `online-map` 微服务的正式 API 契约。后续前端在线地图页面、管理后台、`admin` 聚合、`alerting`、`ops-control` 和其他业务模块只能通过本文档定义的接口读取或管理在线地图公开入口、世界、图层、marker、区域、嵌入配置、健康快照和审计摘要，不能直接读取或修改 `online-map` 数据。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、运维能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `online-map` 的职责边界、数据归属、前序服务兼容、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考 BlueMap、Dynmap、squaremap 和 Minecraft Overviewer 的公开设计。BlueMap 的 marker set、POI、HTML、line、shape 和 extrusion marker 适合抽象 marker 图层；Dynmap 的世界、地图视图、marker、area 和 line 适合作为二维地图 provider 兼容参考；squaremap 的轻量地图、marker、shape 和 icon API 适合第一版 provider 摘要；Minecraft Overviewer 的静态地图生成和浏览器查看模型说明官网不应接管渲染任务。本文档只吸收 provider、world、layer、marker、region、embed 和健康摘要的设计思路，不直接依赖这些项目的 Java API、配置文件、插件命令或内部数据结构。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [BlueMap Markers](https://bluemap.bluecolored.de/wiki/customization/Markers.html) | marker set 和多种 marker 类型适合作为图层与展示对象抽象。 |
| [Dynmap Wiki](https://dynmap.wiki.gg/) | Web 地图、世界视图、marker、area 和 line 需要和插件运行态分离。 |
| [squaremap GitHub](https://github.com/jpenilla/squaremap) | 轻量地图 provider 可以只暴露 marker、shape、icon 和公开入口摘要。 |
| [Minecraft Overviewer](https://overviewer.org/) | 静态地图渲染和 Web 查看可以分离，官网只保存入口与安全快照。 |

## 职责边界

`online-map` 负责在线地图接入控制面和公开展示快照，包括地图 provider、公开地图入口、世界列表、图层、marker set、marker、区域、嵌入配置、健康快照、降级原因、依赖调用摘要、幂等记录、后台审计和自检摘要。

`online-map` 不负责地图渲染，不负责 Minecraft 插件运行，不负责瓦片文件代理，不负责真实世界目录读取，不负责节点命令执行，不负责容器、终端、文件管理、备份恢复、Cloudreve 管理、资源下载、告警规则创建或官网首页配置。

第一版固定为安全模拟和配置快照。可以保存 BlueMap、Dynmap、squaremap、Minecraft Overviewer 或自研地图源的公开入口摘要和 marker 快照，但不能保存地图服务后台密码、插件 token、内网 URL、真实世界路径、节点地址、完整异常栈或外部密钥。真实地图 provider 探测、真实 marker 同步、真实瓦片托管和真实插件联动必须后续单独闭环。

## 数据归属

`online-map` 拥有以下主数据：MapProvider、MapWorld、MapLayer、MapMarker、MapRegion、MapEmbedConfig、MapHealthSnapshot、MapDependencySnapshot、MapAuditLog 和幂等记录。

`online-map` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和用户状态快照；可以保存来自 `server-status` 的公开实例状态摘要；可以保存来自 `ops-control` 的只读节点或 Minecraft 实例健康摘要；可以保存来自 `content` 的公开页面引用摘要；可以保存来自 `changelog` 的地图版本摘要；可以保存供 `alerting` 读取的健康摘要；可以保存来自 `notification` 的投递结果摘要。所有快照只用于展示、过滤、降级和审计，不能成为来源模块主数据，也不能反写来源模块。

`online-map` 不能直接读取其他服务数据库，不能导入前序服务 Java package，不能复用前序服务内存 store，不能调用 `node-daemon` 执行真实命令，不能代理真实瓦片目录，不能读取 Minecraft 世界文件，不能保存 Cloudreve token，不能把玩家资源下载塞进地图服务。

## 基础路径、端口和认证

所有接口默认使用 `/api/v1/online-map` 前缀。第一版本地端口固定为 `8121`，自检摘要必须返回该端口。

健康检查 `GET /api/v1/online-map/health` 不要求认证，只返回服务名、版本、状态和请求编号。`status` 只允许 `READY` 或 `DEGRADED`，不返回 provider 明细、内部依赖错误、地图入口、内网地址或任何敏感字段。

公开接口不要求登录，只返回 `publicVisible=true`、provider `ENABLED`、对象未归档、未过期且安全可展示的数据。公开接口不得返回后台备注、内部 URL、allowlist 规则原文、审计字段、幂等键、操作者字段、依赖错误详情、节点摘要、真实路径、插件配置、请求头、token 或异常堆栈。

后台接口使用同一前缀下的 `/admin` 子路径，全部要求 `Authorization: Bearer <token>`。后台读取接口要求基础角色为 `HELPER`、`ADMIN` 或 `OWNER`，且具备 `NODE_READ`。后台写接口要求 `ADMIN` 或 `OWNER`，且具备 `NODE_WRITE`。修改公开入口、外部嵌入域名、allowed origins、provider 启用禁用、provider 归档和健康刷新属于 `MEDIUM` 风险；把 provider 从内部草稿改成公开可见、修改跨域嵌入来源、允许第三方域名嵌入或归档仍有公开对象的 provider 属于 `HIGH` 风险，必须携带二次确认。涉及高风险审批时要求 `HIGH_RISK_APPROVE` 或 `OWNER`。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`beforeState`、`afterState`、`auditResult`、`healthStatus`、`createdBy`、`updatedBy`、`enabledBy`、`disabledBy`、`archivedBy`、`refreshedBy`、`rawToken`、`credential`、`secretKey`、`nodeToken`、`mapAdminPassword`、`internalUrl`、`internalPath`、`resolvedPath`、`worldDirectory`、`fullException`、`Authorization`、`requestHeaders` 等服务端可信字段。可信字段必须递归检查，嵌套在 `metadata`、`styleSummary`、`sourceRef`、`providerProbe`、`dependencySnapshot`、`points`、`iconRef` 或任意数组对象中也必须拒绝。出现可信字段时返回 `40001`。

## 本地测试控制头

`online-map` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Provider-Mode`、`X-Test-Server-Status-Mode`、`X-Test-Ops-Control-Mode`、`X-Test-Content-Mode`、`X-Test-Changelog-Mode`、`X-Test-Notification-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Now` 模拟认证失败、provider 不可用、依赖不可用、依赖超时、依赖坏 schema、通知失败、审计失败、状态写入失败和时间边界。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发认证失败、provider 失败、依赖失败、通知失败、审计失败、存储失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 纳入生产化硬化项。

## 前序服务兼容契约

`auth` 是后台接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。auth 不可用返回 `46820`，auth 超时返回 `46821`，auth 字段或枚举不兼容返回 `46822`。

`server-status` 是玩家可见服务器状态来源。`online-map` 可以引用 server-status 的公开实例状态和线路摘要，不能伪造在线人数、MOTD、延迟和宕机状态。server-status 不可用返回 `46800`，超时返回 `46801`，字段不兼容返回 `46802`。公开地图总览可以使用已有地图快照并标记 `serverStatusStale=true`，不得假装状态实时。

`ops-control` 是运维只读摘要来源。`online-map` 可以读取节点或 Minecraft 实例只读健康摘要，不能创建运维任务，不能读取文件，不能执行终端，不能通过 ops-control 修改真实服务器状态。ops-control 不可用返回 `46810`，超时返回 `46811`，字段不兼容返回 `46812`。ops-control 不可用时，后台自检必须标记 `OPS_CONTROL_UNAVAILABLE`，不能伪造 provider 健康。

`content` 可以展示地图入口或地图说明页。`online-map` 只保存 content 公开引用摘要，不把文章、SEO、站点地图或首页配置主数据复制进自己的 store。content 不可用返回 `46830`，超时返回 `46831`，字段不兼容返回 `46832`。公开读取已有 provider 时可以使用已保存的 content 快照并标记 stale。

`changelog` 可以提供地图版本和更新记录摘要。`online-map` 不创建 changelog，不修改 changelog 发布状态。changelog 不可用返回 `46840`，超时返回 `46841`，字段不兼容返回 `46842`。

`notification` 是辅助依赖。地图公开入口变更、provider 禁用、健康刷新失败或后台高风险变更可以触发通知摘要。notification 不可用返回 `46850`，超时返回 `46851`，字段不兼容返回 `46852`。通知失败不得伪造成功，必须保存脱敏失败摘要并写入审计；是否回滚主状态由具体接口规则固定。

`alerting` 是后续消费方。`online-map` 第一版只暴露健康、自检和 provider 状态摘要，不直接创建告警规则，不直接写 alerting 告警实例。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `MapProviderType` | `BLUEMAP`、`DYNMAP`、`SQUAREMAP`、`OVERVIEWER`、`CUSTOM` | 地图来源类型。 |
| `MapProviderStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`DEGRADED`、`ARCHIVED` | provider 配置状态。 |
| `MapHealthStatus` | `ONLINE`、`DEGRADED`、`OFFLINE`、`UNKNOWN` | provider 最近健康状态。 |
| `MapWorldDimension` | `OVERWORLD`、`NETHER`、`END`、`CUSTOM` | 世界维度摘要。 |
| `MapRenderStatus` | `READY`、`RENDERING`、`STALE`、`FAILED`、`UNKNOWN` | 世界渲染状态摘要。 |
| `MapLayerType` | `BASE`、`MARKER_SET`、`POI`、`REGION`、`ROUTE`、`CLAIM`、`SYSTEM`、`CUSTOM` | 图层类型。 |
| `MapLayerStatus` | `VISIBLE`、`HIDDEN`、`ARCHIVED` | 图层状态。 |
| `MapMarkerType` | `POI`、`HTML`、`LINE`、`SHAPE`、`EXTRUDE`、`ICON`、`PLAYER_SNAPSHOT`、`CUSTOM` | marker 类型。 |
| `MapObjectStatus` | `PUBLISHED`、`HIDDEN`、`ARCHIVED` | marker 和区域状态。 |
| `MapVisibility` | `PUBLIC`、`MEMBER_ONLY`、`STAFF_ONLY` | 可见范围。第一版公开接口只返回 `PUBLIC`。 |
| `MapSourceModule` | `MANUAL`、`CONTENT`、`SERVER_STATUS`、`OPS_CONTROL`、`CHANGELOG`、`ALERTING` | 来源模块摘要。 |
| `MapDependencyStatus` | `AVAILABLE`、`UNAVAILABLE`、`TIMEOUT`、`BAD_SCHEMA`、`STALE`、`SKIPPED` | 依赖状态摘要。 |
| `OnlineMapAuditResult` | `SUCCESS`、`FAILED` | 审计结果。 |

## 通用对象

### PublicMapOverview

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `providers` | PublicMapProvider[] | 是 | 可公开 provider 摘要。 |
| `worlds` | PublicMapWorld[] | 是 | 可公开世界摘要。 |
| `primaryProviderId` | string 或 null | 是 | 默认 provider。 |
| `primaryWorldId` | string 或 null | 是 | 默认世界。 |
| `embed` | PublicMapEmbedConfig 或 null | 是 | 公开嵌入配置。 |
| `serverStatusStale` | boolean | 是 | server-status 快照是否过期或不可用。 |
| `healthStatus` | string | 是 | 整体地图健康状态。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReasons` | string[] | 是 | 降级原因，必须脱敏。 |
| `lastSuccessfulSnapshotAt` | string 或 null | 是 | 最近成功快照时间。 |

### PublicMapProvider

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `providerId` | string | 是 | provider ID。 |
| `providerType` | string | 是 | `MapProviderType`。 |
| `displayName` | string | 是 | 展示名称，2 到 80 位。 |
| `publicBaseUrl` | string | 是 | 玩家可访问的公开地图 URL，只允许 http、https 或站内路径。 |
| `embedUrl` | string 或 null | 是 | 可嵌入 URL，只允许公开安全地址。 |
| `status` | string | 是 | `ENABLED` 或 `DEGRADED`。 |
| `healthStatus` | string | 是 | 最近健康状态。 |
| `worldCount` | integer | 是 | 公开世界数。 |
| `markerCount` | integer | 是 | 公开 marker 数。 |
| `regionCount` | integer | 是 | 公开区域数。 |
| `lastHealthCheckAt` | string 或 null | 是 | 最近健康检查时间。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |
| `sortOrder` | integer | 是 | 展示排序。 |

### AdminMapProvider

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `providerId` | string | 是 | provider ID。 |
| `providerType` | string | 是 | provider 类型。 |
| `displayName` | string | 是 | 后台展示名称。 |
| `publicBaseUrl` | string | 是 | 公开地图 URL。不得是内网地址、localhost、链路本地地址、文件协议或带凭据 URL。 |
| `embedUrl` | string 或 null | 是 | 嵌入 URL。 |
| `status` | string | 是 | `MapProviderStatus`。 |
| `healthStatus` | string | 是 | `MapHealthStatus`。 |
| `publicVisible` | boolean | 是 | 是否允许公开展示。 |
| `allowedOrigins` | string[] | 是 | 允许嵌入来源的脱敏公开域名列表，最多 20 个，不允许 `*`。 |
| `contentRef` | object 或 null | 是 | content 公开说明页快照。 |
| `serverStatusRef` | object 或 null | 是 | server-status 公开实例摘要。 |
| `opsRef` | object 或 null | 是 | ops-control 只读实例健康摘要，必须脱敏。 |
| `changelogRef` | object 或 null | 是 | changelog 地图版本摘要。 |
| `worldCount` | integer | 是 | 世界总数。 |
| `layerCount` | integer | 是 | 图层总数。 |
| `markerCount` | integer | 是 | marker 总数。 |
| `regionCount` | integer | 是 | 区域总数。 |
| `lastHealthCheckAt` | string 或 null | 是 | 最近健康检查时间。 |
| `lastSuccessfulSnapshotAt` | string 或 null | 是 | 最近成功快照时间。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |
| `adminNote` | string 或 null | 是 | 后台备注，不得出现在公开接口。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### PublicMapWorld

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `worldId` | string | 是 | 世界 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `worldName` | string | 是 | 世界公开标识。 |
| `dimension` | string | 是 | `MapWorldDimension`。 |
| `displayName` | string | 是 | 展示名称。 |
| `center` | object | 是 | 地图中心坐标，包含 `x`、`z`，可选 `y`。 |
| `bounds` | object 或 null | 是 | 公开边界摘要。 |
| `renderStatus` | string | 是 | 渲染状态摘要。 |
| `lastRenderedAt` | string 或 null | 是 | 最近渲染完成时间。 |
| `sortOrder` | integer | 是 | 展示排序。 |

### AdminMapWorld

`AdminMapWorld` 在 `PublicMapWorld` 基础上补充 `enabled`、`publicVisible`、`sourceWorldKey`、`styleSummary`、`degradeReasons`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。`sourceWorldKey` 只能是脱敏 provider 世界标识，不得是真实世界目录或绝对路径。

### MapLayer

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `layerId` | string | 是 | 图层 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `worldId` | string | 是 | 世界 ID。 |
| `displayName` | string | 是 | 图层展示名称，2 到 80 位。 |
| `layerType` | string | 是 | `MapLayerType`。 |
| `status` | string | 是 | `MapLayerStatus`。 |
| `defaultVisible` | boolean | 是 | 默认是否展示。 |
| `toggleable` | boolean | 是 | 前端是否允许切换。 |
| `visibility` | string | 是 | `MapVisibility`。 |
| `styleSummary` | object | 是 | 安全样式摘要，不允许脚本、事件处理器或外部 secret。 |
| `sortOrder` | integer | 是 | 展示排序。 |
| `createdBy` | string | 后台可见 | 创建者用户 ID。 |
| `updatedBy` | string | 后台可见 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### MapMarker

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `markerId` | string | 是 | marker ID。 |
| `providerId` | string | 是 | provider ID。 |
| `worldId` | string | 是 | 世界 ID。 |
| `layerId` | string | 是 | 图层 ID。 |
| `markerType` | string | 是 | `MapMarkerType`。 |
| `title` | string | 是 | 标题，1 到 120 位。 |
| `summary` | string 或 null | 是 | 摘要，最多 500 位。 |
| `position` | object 或 null | 是 | 点位坐标，`POI`、`HTML`、`ICON` 必填。 |
| `points` | object[] | 是 | 折线、形状或拉伸区域点位，`LINE` 至少 2 点，`SHAPE` 和 `EXTRUDE` 至少 3 点。 |
| `iconRef` | object 或 null | 是 | 图标引用摘要，只允许站内资源路径或公开 https URL。 |
| `styleSummary` | object | 是 | 安全样式摘要。 |
| `visibility` | string | 是 | 可见范围。 |
| `status` | string | 是 | `MapObjectStatus`。 |
| `sourceModule` | string | 是 | `MapSourceModule`。 |
| `sourceRef` | object 或 null | 是 | 来源对象安全摘要。 |
| `expiresAt` | string 或 null | 是 | 过期后不再公开展示。 |
| `createdBy` | string | 后台可见 | 创建者。 |
| `updatedBy` | string | 后台可见 | 最近修改者。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### MapRegion

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `regionId` | string | 是 | 区域 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `worldId` | string | 是 | 世界 ID。 |
| `layerId` | string | 是 | 图层 ID。 |
| `title` | string | 是 | 区域标题，1 到 120 位。 |
| `summary` | string 或 null | 是 | 摘要，最多 500 位。 |
| `points` | object[] | 是 | 多边形点位，至少 3 点。 |
| `minY` | number 或 null | 是 | 最低高度，可为 null。 |
| `maxY` | number 或 null | 是 | 最高高度，可为 null，不能小于 `minY`。 |
| `styleSummary` | object | 是 | 安全样式摘要。 |
| `visibility` | string | 是 | 可见范围。 |
| `status` | string | 是 | `MapObjectStatus`。 |
| `sourceModule` | string | 是 | 来源模块。 |
| `sourceRef` | object 或 null | 是 | 来源对象安全摘要。 |
| `expiresAt` | string 或 null | 是 | 过期后不公开展示。 |
| `createdBy` | string | 后台可见 | 创建者。 |
| `updatedBy` | string | 后台可见 | 最近修改者。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### PublicMapEmbedConfig

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `providerId` | string | 是 | provider ID。 |
| `embedUrl` | string | 是 | 嵌入 URL。 |
| `allowedOrigins` | string[] | 是 | 允许嵌入来源摘要。 |
| `defaultWorldId` | string 或 null | 是 | 默认世界。 |
| `defaultLayerIds` | string[] | 是 | 默认打开图层。 |
| `defaultCenter` | object 或 null | 是 | 默认中心坐标。 |
| `minZoom` | number 或 null | 是 | 最小缩放。 |
| `maxZoom` | number 或 null | 是 | 最大缩放。 |
| `updatedAt` | string | 是 | 最近更新时间。 |

### MapHealthSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `snapshotId` | string | 是 | 健康快照 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `healthStatus` | string | 是 | `MapHealthStatus`。 |
| `httpReachable` | boolean | 是 | 公开入口是否可达摘要。 |
| `worldCount` | integer | 是 | 世界数摘要。 |
| `markerCount` | integer | 是 | marker 数摘要。 |
| `regionCount` | integer | 是 | 区域数摘要。 |
| `latencyMs` | integer 或 null | 是 | 健康探测耗时。 |
| `checkedAt` | string | 是 | 检查时间。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReasons` | string[] | 是 | 脱敏降级原因。 |
| `dependencyStatus` | object | 是 | server-status、ops-control、content、changelog 和 notification 的脱敏状态摘要。 |

### OnlineMapAuditLog

审计字段继承公共契约，允许补充 `providerId`、`worldId`、`layerId`、`markerId`、`regionId`、`healthSnapshotId`、`stateFrom`、`stateTo`、`idempotencyKey`、`dependencyStatus`、`notificationStatus` 和 `providerType`。审计列表不得提供删除接口。审计响应不得返回 token、密钥、完整请求头、内部 URL、真实世界路径、节点地址、完整异常栈、完整请求体、地图后台密码或插件配置。

### OnlineMapOpsSummary

自检摘要至少包含 `service`、`port`、`storageMode`、`authMode`、`providerAdapterMode`、`serverStatusMode`、`opsControlMode`、`contentMode`、`changelogMode`、`notificationMode`、`testControlsEnabled`、`providersTotal`、`enabledProvidersTotal`、`worldsTotal`、`layersTotal`、`markersTotal`、`regionsTotal`、`healthSnapshotsTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastHealthCheckAt`、`lastAuditAt`、`degraded`、`degradeReasons` 和 `productionGaps`。第一版必须返回 `port=8121`、`storageMode=IN_MEMORY`、`providerAdapterMode=TEST_STUB`，并明确真实 provider、真实 auth HTTP、真实持久化、真实跨服务 HTTP 和真实 marker 同步尚未接入。

## online-map 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `46800` | 502 | server-status 摘要不可用。 |
| `46801` | 504 | server-status 摘要调用超时。 |
| `46802` | 502 | server-status 响应字段不兼容。 |
| `46810` | 502 | ops-control 只读摘要不可用。 |
| `46811` | 504 | ops-control 只读摘要调用超时。 |
| `46812` | 502 | ops-control 响应字段不兼容。 |
| `46820` | 502 | auth 认证上下文不可用。 |
| `46821` | 504 | auth 认证上下文调用超时。 |
| `46822` | 502 | auth 认证上下文字段不兼容。 |
| `46830` | 502 | content 公开引用摘要不可用。 |
| `46831` | 504 | content 公开引用摘要调用超时。 |
| `46832` | 502 | content 响应字段不兼容。 |
| `46840` | 502 | changelog 地图版本摘要不可用。 |
| `46841` | 504 | changelog 地图版本摘要调用超时。 |
| `46842` | 502 | changelog 响应字段不兼容。 |
| `46850` | 502 | notification 投递不可用。 |
| `46851` | 504 | notification 投递超时。 |
| `46852` | 502 | notification 响应字段不兼容。 |
| `49700` | 404 | provider 不存在，或公开接口不可访问该 provider。 |
| `49701` | 404 | world 不存在，或公开接口不可访问该 world。 |
| `49702` | 404 | layer 不存在，或公开接口不可访问该 layer。 |
| `49703` | 404 | marker 不存在，或公开接口不可访问该 marker。 |
| `49704` | 404 | region 不存在，或公开接口不可访问该 region。 |
| `49705` | 404 | 健康快照不存在。 |
| `49710` | 409 | provider、layer、marker 或 region 状态不允许当前操作。 |
| `49711` | 409 | provider 名称、公开入口、世界标识或图层名称冲突。 |
| `49712` | 409 | 幂等键请求指纹冲突。 |
| `49713` | 400 | provider URL 不合法或不安全。 |
| `49714` | 400 | 坐标、点位、边界或高度范围不合法。 |
| `49715` | 400 | 嵌入来源不在 allowlist 或 allowlist 不合法。 |
| `49716` | 409 | provider 健康刷新过于频繁或已有刷新进行中。 |
| `49717` | 409 | 归档 provider 前仍存在公开对象。 |
| `55600` | 500 | online-map 内部错误。 |
| `55601` | 500 | online-map 审计写入失败。 |
| `55602` | 500 | online-map 状态写入失败。 |
| `55603` | 500 | online-map 健康快照写入失败。 |

字段校验、未登录、令牌格式错误、角色权限不足、能力点不足、高风险操作未确认、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。online-map 自有幂等指纹冲突使用 `49712`。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/online-map/health` | 否 | 无 | LOW |
| 公开地图总览 | GET | `/api/v1/online-map/overview` | 否 | 无 | LOW |
| 公开 provider 列表 | GET | `/api/v1/online-map/providers` | 否 | 无 | LOW |
| 公开 provider 详情 | GET | `/api/v1/online-map/providers/{providerId}` | 否 | 无 | LOW |
| 公开世界列表 | GET | `/api/v1/online-map/worlds` | 否 | 无 | LOW |
| 公开图层列表 | GET | `/api/v1/online-map/layers` | 否 | 无 | LOW |
| 公开 marker 列表 | GET | `/api/v1/online-map/markers` | 否 | 无 | LOW |
| 公开区域列表 | GET | `/api/v1/online-map/regions` | 否 | 无 | LOW |
| 公开嵌入配置 | GET | `/api/v1/online-map/embed` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/online-map/admin/ops/summary` | 是 | `NODE_READ` | LOW |
| 后台 provider 列表 | GET | `/api/v1/online-map/admin/providers` | 是 | `NODE_READ` | LOW |
| 后台 provider 详情 | GET | `/api/v1/online-map/admin/providers/{providerId}` | 是 | `NODE_READ` | LOW |
| 创建 provider | POST | `/api/v1/online-map/admin/providers` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 修改 provider | PATCH | `/api/v1/online-map/admin/providers/{providerId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 启用 provider | PATCH | `/api/v1/online-map/admin/providers/{providerId}/enable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | HIGH |
| 禁用 provider | PATCH | `/api/v1/online-map/admin/providers/{providerId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档 provider | PATCH | `/api/v1/online-map/admin/providers/{providerId}/archive` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | HIGH |
| 刷新 provider 健康 | POST | `/api/v1/online-map/admin/providers/{providerId}/health/refresh` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 健康快照列表 | GET | `/api/v1/online-map/admin/providers/{providerId}/health/snapshots` | 是 | `NODE_READ` | LOW |
| 后台世界列表 | GET | `/api/v1/online-map/admin/worlds` | 是 | `NODE_READ` | LOW |
| 保存世界快照 | PUT | `/api/v1/online-map/admin/worlds/{worldId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 后台图层列表 | GET | `/api/v1/online-map/admin/layers` | 是 | `NODE_READ` | LOW |
| 创建图层 | POST | `/api/v1/online-map/admin/layers` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 修改图层 | PATCH | `/api/v1/online-map/admin/layers/{layerId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档图层 | PATCH | `/api/v1/online-map/admin/layers/{layerId}/archive` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 后台 marker 列表 | GET | `/api/v1/online-map/admin/markers` | 是 | `NODE_READ` | LOW |
| 创建 marker | POST | `/api/v1/online-map/admin/markers` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 修改 marker | PATCH | `/api/v1/online-map/admin/markers/{markerId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档 marker | PATCH | `/api/v1/online-map/admin/markers/{markerId}/archive` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 后台区域列表 | GET | `/api/v1/online-map/admin/regions` | 是 | `NODE_READ` | LOW |
| 创建区域 | POST | `/api/v1/online-map/admin/regions` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 修改区域 | PATCH | `/api/v1/online-map/admin/regions/{regionId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档区域 | PATCH | `/api/v1/online-map/admin/regions/{regionId}/archive` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 审计列表 | GET | `/api/v1/online-map/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 公开接口

`GET /api/v1/online-map/overview` 返回 `PublicMapOverview`。总览只汇总 `ENABLED` 或 `DEGRADED`、`publicVisible=true`、未归档且未过期的数据。provider 健康失败时可以返回最近一次成功快照并标记 `degraded=true`，不得伪造实时成功。

`GET /api/v1/online-map/providers` 支持 `providerType`、`healthStatus`、`keyword` 和 `sort`。`sort` 允许 `sortOrder_asc`、`displayName_asc`、`lastHealthCheckAt_desc`。成功响应分页 `items` 为 `PublicMapProvider[]`。

`GET /api/v1/online-map/providers/{providerId}` 返回公开 provider 详情。provider 不存在、未启用、不可公开或已归档时返回 `49700`。

`GET /api/v1/online-map/worlds` 支持 `providerId`、`dimension`、`renderStatus`、`keyword` 和 `sort`。`sort` 允许 `sortOrder_asc`、`displayName_asc`、`lastRenderedAt_desc`。成功响应分页 `items` 为 `PublicMapWorld[]`。只返回公开 provider 下公开可见世界。

`GET /api/v1/online-map/layers` 支持 `providerId`、`worldId`、`layerType`、`visibility` 和 `sort`。`sort` 允许 `sortOrder_asc`、`displayName_asc`。只返回 `VISIBLE`、`visibility=PUBLIC`、公开 provider 和公开 world 下的图层。

`GET /api/v1/online-map/markers` 支持 `providerId`、`worldId`、`layerId`、`markerType`、`sourceModule`、`keyword`、`bounds`、`from`、`to` 和 `sort`。`bounds` 格式为 `minX,minZ,maxX,maxZ`，坐标必须是有限数字且 `minX<=maxX`、`minZ<=maxZ`。`sort` 允许 `updatedAt_desc`、`title_asc`、`createdAt_desc`。只返回 `PUBLISHED`、`visibility=PUBLIC`、未过期且坐标合法的 marker。

`GET /api/v1/online-map/regions` 支持 `providerId`、`worldId`、`layerId`、`sourceModule`、`keyword`、`bounds` 和 `sort`。只返回 `PUBLISHED`、`visibility=PUBLIC`、未过期且点位合法的区域。

`GET /api/v1/online-map/embed` 支持 `providerId`、`worldId` 和 `origin`。当传入 `origin` 时，必须命中 provider 的 `allowedOrigins`，否则返回 `49715`。传入 `worldId` 时，必须选择该 provider 下启用、公开且未归档的 world 作为默认世界；world 不存在、属于其他 provider、未公开、未启用或已归档时返回 `49701`。成功返回 `PublicMapEmbedConfig`。没有可用 provider 时返回 `data=null`，不能返回内部默认地址，也不能因为默认 provider 为空抛出内部错误。

## 后台 provider 接口

`GET /api/v1/online-map/admin/ops/summary` 返回 `OnlineMapOpsSummary`。读取失败返回 `55600`。摘要不得返回 token、请求头、内部 URL、真实路径、provider 后台密码或异常栈。

`GET /api/v1/online-map/admin/providers` 支持 `page`、`pageSize`、`keyword`、`providerType`、`status`、`healthStatus`、`publicVisible`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`、`lastHealthCheckAt_desc`。成功响应分页 `items` 为 `AdminMapProvider[]`。

`GET /api/v1/online-map/admin/providers/{providerId}` 返回 `AdminMapProvider`、最近健康快照、依赖摘要和最近审计摘要。provider 不存在返回 `49700`。

`POST /api/v1/online-map/admin/providers` 请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `providerType` | string | 是 | 任一 `MapProviderType`。 |
| `displayName` | string | 是 | 2 到 80 位，同一未归档 provider 中唯一。 |
| `publicBaseUrl` | string | 是 | http、https 或站内路径，不能是内网、localhost、file、data、javascript、带用户名密码 URL。 |
| `embedUrl` | string 或 null | 否 | 不传时可由 `publicBaseUrl` 推导。 |
| `publicVisible` | boolean | 否 | 默认 `false`。 |
| `allowedOrigins` | string[] | 否 | 最多 20 个公开 origin，不允许 `*`。 |
| `contentRef` | object 或 null | 否 | content 公开引用 ID。 |
| `serverStatusRef` | object 或 null | 否 | server-status 公开实例 ID。 |
| `opsRef` | object 或 null | 否 | ops-control 只读实例 ID。 |
| `changelogRef` | object 或 null | 否 | changelog 版本 ID。 |
| `adminNote` | string 或 null | 否 | 最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `AdminMapProvider`，默认状态为 `DRAFT`。同名、公开入口或 embed URL 冲突返回 `49711`。URL 冲突比较必须基于规范化后的公开 URL 和 embed URL，忽略末尾 `/`、协议和 host 大小写差异，但不能把不同路径误判为同一入口。URL 不安全返回 `49713`。审计失败返回 `55601`，不得创建 provider。

`PATCH /api/v1/online-map/admin/providers/{providerId}` 可修改创建字段中的业务字段，`reason` 必填。修改 `publicBaseUrl`、`embedUrl`、`allowedOrigins` 或把 `publicVisible` 从 `false` 改为 `true` 时属于 `HIGH` 风险，必须携带 `confirmText=UPDATE_PUBLIC_MAP_ENTRY`，否则返回 `42003`。`ARCHIVED` provider 不允许修改，返回 `49710`。

`PATCH /api/v1/online-map/admin/providers/{providerId}/enable` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ENABLE_PUBLIC_MAP_PROVIDER`。`DRAFT`、`DISABLED` 和 `DEGRADED` 可流转为 `ENABLED`。启用时必须校验公开 URL、embed URL、allowed origins 和至少一个公开 world 快照；不满足返回 `40001` 或 `49710`。重复启用保持幂等。

`PATCH /api/v1/online-map/admin/providers/{providerId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 或 `DEGRADED` 可流转为 `DISABLED`。禁用后公开接口不再返回该 provider。重复禁用保持幂等。

`PATCH /api/v1/online-map/admin/providers/{providerId}/archive` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ARCHIVE_MAP_PROVIDER`。只有 `DRAFT`、`DISABLED` 或 `DEGRADED` 可归档；`ENABLED` provider 必须先禁用后归档；仍有 `publicVisible=true` 或 `visibility=PUBLIC` 且未归档的 world、layer、marker 或 region 时返回 `49717`，不得自动静默归档子对象。`ARCHIVED` 为终态。

`POST /api/v1/online-map/admin/providers/{providerId}/health/refresh` 请求字段为 `reason` 和 `idempotencyKey`。只允许 `ENABLED` 或 `DEGRADED` provider 刷新健康。相同 provider 刷新必须加锁；已有刷新进行中或冷却窗口内重复刷新返回 `49716`。第一版冷却窗口固定为 60 秒，幂等重放同一请求不受冷却影响；不同幂等键或无幂等键在冷却窗口内重复刷新必须返回 `49716`。provider 探测失败不得清空旧公开入口，只写入降级健康快照；快照写入失败返回 `55603`。

`GET /api/v1/online-map/admin/providers/{providerId}/health/snapshots` 支持 `page`、`pageSize`、`healthStatus`、`from`、`to` 和 `sort`。`sort` 允许 `checkedAt_desc`、`checkedAt_asc`、`latencyMs_asc`。成功响应分页 `items` 为 `MapHealthSnapshot[]`。

## 后台世界接口

`GET /api/v1/online-map/admin/worlds` 支持 `page`、`pageSize`、`providerId`、`dimension`、`renderStatus`、`enabled`、`publicVisible`、`keyword` 和 `sort`。成功响应分页 `items` 为 `AdminMapWorld[]`。

`PUT /api/v1/online-map/admin/worlds/{worldId}` 用于保存或更新 provider 同步到控制面的世界快照。请求字段包括 `providerId`、`worldName`、`dimension`、`displayName`、`enabled`、`publicVisible`、`sourceWorldKey`、`center`、`bounds`、`renderStatus`、`lastRenderedAt`、`styleSummary`、`sortOrder`、`reason` 和 `idempotencyKey`。`sourceWorldKey` 不得是绝对路径、真实目录或包含路径穿越。provider 不存在返回 `49700`。坐标不合法返回 `49714`。

## 后台图层接口

`GET /api/v1/online-map/admin/layers` 支持 `page`、`pageSize`、`providerId`、`worldId`、`layerType`、`status`、`visibility`、`keyword` 和 `sort`。成功响应分页 `items` 为 `MapLayer[]`。

`POST /api/v1/online-map/admin/layers` 请求字段包括 `providerId`、`worldId`、`displayName`、`layerType`、`defaultVisible`、`toggleable`、`visibility`、`styleSummary`、`sortOrder`、`reason` 和 `idempotencyKey`。provider 或 world 不存在返回 `49700` 或 `49701`。同一 world 下未归档图层同名冲突返回 `49711`。

`PATCH /api/v1/online-map/admin/layers/{layerId}` 可修改图层展示字段、状态、可见范围和样式摘要，`reason` 必填。`ARCHIVED` 图层不可修改，返回 `49710`。

`PATCH /api/v1/online-map/admin/layers/{layerId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。重复归档保持幂等。归档后公开接口不再返回该图层，也不返回挂在该图层下的 marker 和 region。

## 后台 marker 接口

`GET /api/v1/online-map/admin/markers` 支持 `page`、`pageSize`、`providerId`、`worldId`、`layerId`、`markerType`、`status`、`visibility`、`sourceModule`、`keyword`、`bounds`、`from`、`to` 和 `sort`。成功响应分页 `items` 为 `MapMarker[]`。

`POST /api/v1/online-map/admin/markers` 请求字段包括 `providerId`、`worldId`、`layerId`、`markerType`、`title`、`summary`、`position`、`points`、`iconRef`、`styleSummary`、`visibility`、`status`、`sourceModule`、`sourceRef`、`expiresAt`、`reason` 和 `idempotencyKey`。不同 marker 类型必须按本文档要求校验 `position` 和 `points`。坐标、点位、图标 URL 或样式不合法返回 `49714` 或 `40001`。创建默认状态为 `PUBLISHED`，但公开接口仍按 provider、world、layer 和 visibility 过滤。

`PATCH /api/v1/online-map/admin/markers/{markerId}` 可修改 marker 展示字段、点位、样式、可见范围、状态、来源摘要和过期时间，`reason` 必填。`ARCHIVED` marker 不可修改，返回 `49710`。

`PATCH /api/v1/online-map/admin/markers/{markerId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。重复归档保持幂等。归档后公开接口不可见。

## 后台区域接口

`GET /api/v1/online-map/admin/regions` 支持 `page`、`pageSize`、`providerId`、`worldId`、`layerId`、`status`、`visibility`、`sourceModule`、`keyword`、`bounds` 和 `sort`。成功响应分页 `items` 为 `MapRegion[]`。

`POST /api/v1/online-map/admin/regions` 请求字段包括 `providerId`、`worldId`、`layerId`、`title`、`summary`、`points`、`minY`、`maxY`、`styleSummary`、`visibility`、`status`、`sourceModule`、`sourceRef`、`expiresAt`、`reason` 和 `idempotencyKey`。区域至少 3 个点，点位必须是有限数字，`maxY` 不得小于 `minY`。不合法返回 `49714`。

`PATCH /api/v1/online-map/admin/regions/{regionId}` 可修改区域展示字段、点位、高度、样式、可见范围、状态、来源摘要和过期时间，`reason` 必填。`ARCHIVED` 区域不可修改，返回 `49710`。

`PATCH /api/v1/online-map/admin/regions/{regionId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。重复归档保持幂等。归档后公开接口不可见。

## 审计接口

`GET /api/v1/online-map/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`providerId`、`layerId`、`markerId`、`regionId`、`result`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`、`riskLevel_desc`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志只读，不提供删除、修改或恢复接口。

审计中的 `requestId` 必须来自当前 HTTP 请求，不能使用固定占位值。后台写操作必须记录调用者、`reason`、操作前状态、操作后状态、请求编号、结果和失败原因。审计写入失败时，provider、world、layer、marker、region 和健康快照的写操作不得假装成功，必须返回 `55601` 并保持业务状态不变。notification 失败不回滚 provider 主状态，但必须记录脱敏失败摘要。

## 状态、幂等和并发

provider 状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DEGRADED`、`DISABLED` 或在无公开对象时归档；`DEGRADED` 可恢复为 `ENABLED`、禁用或归档；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

图层状态流转为 `VISIBLE` 到 `HIDDEN` 或 `ARCHIVED`，`HIDDEN` 到 `VISIBLE` 或 `ARCHIVED`，`ARCHIVED` 为终态。marker 和区域状态流转为 `PUBLISHED` 到 `HIDDEN` 或 `ARCHIVED`，`HIDDEN` 到 `PUBLISHED` 或 `ARCHIVED`，`ARCHIVED` 为终态。

创建、修改、状态流转、归档、保存世界快照和健康刷新支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `49712`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序，数组保留顺序，不能依赖 Java `Map.toString()` 或浏览器字段顺序。

并发创建相同 provider URL、同名图层、相同 marker 来源引用或相同区域来源引用时只能一个成功，其余返回冲突。marker 和区域来源引用冲突以同一 provider、world、layer、sourceModule 和结构化规范化后的 sourceRef 为口径，`sourceRef=null` 或空对象不参与唯一约束。健康刷新必须以 provider 为粒度加锁并执行冷却窗口判断。所有写接口必须在同一个临界区内完成状态校验、业务写入、审计写入、幂等记录和响应快照保存。后续数据库实现必须迁移为事务、唯一约束、条件更新或等效机制，不能降低并发口径。

## 安全、降级和脱敏

所有 URL 必须拒绝 `file:`、`data:`、`javascript:`、带用户名密码 URL、内网 IP、localhost、链路本地地址、未解析主机、空 host 和控制字符。公开接口只允许返回公开 URL 或站内路径。allowed origins 不允许 `*`，不允许内网 origin。

坐标必须是有限数字。公开 marker 查询的 `bounds` 必须限制在合理范围内，不能因为异常范围导致全量扫描。providerType、dimension、renderStatus、layerType、layer status、markerType、visibility、object status 和 sourceModule 必须严格匹配本文档枚举，未知枚举返回 `40001`。HTML marker 的 `summary` 和所有对象的 `styleSummary` 不允许 `<script>`、事件处理器、危险协议、CSS `expression()`、`url(javascript:...)` 或内联敏感数据。

任何请求体和响应都不得包含访问 token、节点密钥、地图后台密码、Cloudreve 管理 token、分享密码、外部 webhook secret、SMTP 密码、短信 token、完整 Authorization 请求头、内部绝对路径、真实世界目录、节点地址、异常堆栈、数据库连接串、`.env`、`authorized_keys`、`id_rsa`、服务器密码或 shell 命令。检查必须递归覆盖嵌套对象和数组。

provider 探测失败时不清空旧公开入口。公开接口可以返回最近一次成功快照并标记 `degraded=true` 和 `degradeReasons`。没有任何成功快照时返回 `UNKNOWN`、空数组或 `data=null`，不得伪造在线地图。依赖不可用时，读取类接口可以使用已有快照并标记 stale；写入类接口不得假装成功。

## 验收口径

`online-map` API 文档必须按 `docs/contracts-online-map.md` 独立存在，并由 `.local-docs/tests-online-map.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求、敏感字段脱敏、测试控制头默认关闭和模块验收口径。

`online-map` 完成时必须满足以下条件：端口固定为 `8121`；健康检查公开且不泄露敏感信息；公开接口只返回公开可见、已启用、未归档且脱敏的数据；后台接口按角色和能力点限制；provider、world、layer、marker、region、embed、健康快照、审计、幂等、状态流转、URL 安全、坐标边界、依赖降级、审计失败回滚、敏感字段脱敏、测试控制头默认关闭和自检摘要都有自动化验证；自动化测试必须先红灯；实现后 `online-map` 全量测试通过；前序 20 个稳定服务回归通过；边界扫描无违规命中；不修改前序服务稳定接口；不直接调用 `node-daemon`；不读取真实世界目录；不代理真实瓦片；不执行真实地图插件命令；不把地图渲染、资源下载、节点文件管理、终端、备份恢复或告警规则塞进 `online-map`。
