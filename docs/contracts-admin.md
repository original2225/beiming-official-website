# 北冥官网 admin API 契约

版本：0.3

## 文档定位

本文档是 `admin` 微服务的正式 API 契约。当前 `admin` 兼容刷新负责让后台入口识别已经闭环的 26 个后端服务，并把 `api-gateway` 作为平台依赖展示。后续前端管理后台只能通过本文档定义的接口读取后台聚合入口、模块能力、待办摘要、指标摘要、审计索引、平台依赖摘要和 admin 自有配置，不能把业务主数据或真实运维控制塞进 `admin`。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `admin` 模块自己的职责边界、路径、字段、状态、权限、错误码、幂等、降级、审计和验收口径。

本文档参考了现有前序服务契约和成熟在线平台的后台信息架构。GitLab Admin Area、Audit Events、Todos 和 Health Check 的设计强调后台总览、审计索引、待办队列和健康摘要分离。Grafana HTTP API 和 RBAC 文档强调健康检查与权限模型分离，适合 admin 自检和模块权限裁剪。Discourse 管理和审核队列强调运营待办只做审核入口，不把处理逻辑塞进总览。MCSManager 的面板和 Daemon 分离思路用于确认游戏服控制面边界，真实实例、文件、终端、容器和节点操作仍归 `ops-control` 与 `node-daemon`。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [GitLab Admin Area](https://docs.gitlab.com/administration/admin_area/) | 后台总览只组织入口和状态，不替代具体业务模块。 |
| [GitLab Audit Events API](https://docs.gitlab.com/api/audit_events/) | 审计应作为可筛选的只读索引，不允许普通后台入口修改来源审计。 |
| [GitLab Todos API](https://docs.gitlab.com/api/todos/) | 待办聚合要保留来源、目标入口和只读状态，处理动作回到来源域。 |
| [GitLab Health Check](https://docs.gitlab.com/administration/monitoring/health_check/) | 健康摘要用于运行状态判断，不等同于业务成功。 |
| [Grafana Health API](https://grafana.com/docs/grafana/latest/developers/http_api/other/) | 平台自检应返回运行模式和健康结果，并避免泄露敏感配置。 |
| [Grafana RBAC](https://grafana.com/docs/grafana/latest/administration/roles-and-permissions/access-control/) | 后台入口需要按角色和能力点裁剪，不能只靠前端隐藏按钮。 |
| [Discourse Moderation Guide](https://meta.discourse.org/t/discourse-moderation-guide/63116) | 审核待办应作为运营入口，审核主状态和处理逻辑仍归来源模块。 |
| [MCSManager Instance API](https://docs.mcsmanager.com/apis/api_instance.html) | 游戏服面板区分实例、运行状态和操作边界，admin 只展示入口与摘要。 |

## 职责边界

`admin` 负责管理后台统一入口、模块注册表、模块能力发现、后台导航配置、Dashboard 看板摘要、待办聚合摘要、审计索引、系统配置、模块健康摘要、平台依赖摘要和 admin 自身审计。

`admin` 不负责注册、登录、会话、角色能力点主数据、邀请码主数据、成员档案主数据、通知投递主数据、内容审核实现、资源审核实现、入服流程判定、考试判分、白名单审核、考勤积分计算、社区处罚、活动报名、日历事件、更新日志发布、素材文件安全、指南正文维护、Cloudreve 同步、备份恢复、告警规则、在线地图主数据、插件事件、跨平台通知、镜像市场、服务器状态采集、Cloudreve 分享生成、节点注册、容器启停、虚拟机控制、Minecraft 实例控制、文件管理、终端命令、日志流、高风险运维审批或任何真实服务器操作。

`admin` 只能通过正式 API、后端入口可信认证上下文或测试环境适配器读取各服务摘要。它不能直接读取或修改其他服务的数据库、内存存储、实体、Repository、测试种子或内部实现。

## 数据归属

`admin` 拥有以下主数据：模块注册表、模块入口显隐配置、后台导航配置、看板布局配置、系统配置项、待办聚合快照、模块健康摘要快照、平台依赖摘要快照、审计索引快照、幂等记录和 admin 自身审计日志。

`admin` 保存的待办、指标和审计索引都是聚合摘要或只读索引，不是来源模块主数据。来源模块的业务状态、审核状态、通知状态、资源状态、线路状态和审计主记录仍由来源模块负责。`admin` 不提供删除来源审计、改写来源业务状态或关闭来源待办的接口。

## 基础路径与认证

所有接口默认使用 `/api/v1/admin` 前缀，全部要求 `Authorization: Bearer <token>`。

后台读取接口要求当前用户具备 `HELPER`、`ADMIN` 或 `OWNER` 任一基础角色。系统配置读取、审计索引读取和 admin 自检摘要要求 `ADMIN` 或 `OWNER`。普通配置写操作要求 `ADMIN` 或 `OWNER`。全局高影响配置写操作只允许 `OWNER`。

`USER` 不能访问任何 admin 接口，返回 `42001`。未登录返回 `41000`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问 admin 接口，按认证上下文错误返回 `46703`、`46704` 或公共认证错误。

### 可信认证上下文

生产环境中，`admin` 必须优先消费后端入口注入的可信身份头。浏览器请求体和普通客户端不得通过请求体覆盖操作者、角色、能力点、来源 IP、请求编号或模块摘要。没有可信身份头时，可以继续用 `Authorization` 交给认证适配器校验；本地固定 token 只允许在测试模式或本地开发模式启用。

| 请求头 | 必填 | 说明 |
| --- | --- | --- |
| `X-Beiming-Actor-User-Id` | 是 | 网关或后端入口从 auth 会话校验结果注入的当前用户 ID。 |
| `X-Beiming-Actor-Roles` | 是 | 逗号分隔基础角色，只接受 `OWNER`、`ADMIN`、`HELPER`、`USER`。 |
| `X-Beiming-Actor-Permissions` | 否 | 逗号分隔能力点，可为空字符串。 |
| `X-Gateway-Internal-Request-Id` | 否 | 网关注入的内部请求编号，用于链路关联；响应仍以 `X-Request-Id` 为对外请求编号。 |

可信身份头存在时，`admin` 必须按该上下文做角色和能力点裁剪。`roles` 为空、包含未知角色、缺少 `userId` 或字段格式不兼容时返回 `46703` 或 `46702`，不得回退到浏览器传入的伪造字段，也不得再用本地固定 token 兜底提升权限。

### 测试钩子边界

`X-Test-Module-Mode`、`X-Test-Platform-Mode`、`X-Test-Fail-Audit` 和 `X-Test-Fail-Settings` 只允许在 admin 测试模式启用。生产模式必须忽略这些测试头，不能允许外部请求通过测试头伪造模块降级、平台降级、审计失败或配置写入失败。

## 来源服务兼容契约

`admin` 适配当前已经闭环的 26 个服务，不要求任何来源服务反向适配 `admin`。生产环境可以通过后端入口传入可信认证上下文和模块摘要，也可以调用来源服务正式后台接口。测试环境使用模块适配器 stub。

当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。浏览器请求体不得传入并覆盖当前操作者、角色、权限、来源 IP、请求编号、模块健康、待办来源、审计来源或来源业务摘要。

来源模块不可用时，聚合读取接口优先返回局部降级结果，并在 `degradedModules`、`moduleHealth` 或对应条目中标记 `DEGRADED` 或 `UNAVAILABLE`。只有认证上下文不可用、admin 自有存储不可用、字段不兼容导致无法构造契约响应时，才返回错误。

当前已闭环模块包括 `AUTH`、`PROFILE`、`NOTIFICATION`、`CONTENT`、`SERVER_STATUS`、`RESOURCE`、`ADMIN`、`ONBOARDING`、`EXAM`、`WHITELIST`、`ATTENDANCE`、`COMMUNITY`、`ACTIVITY`、`CALENDAR`、`CHANGELOG`、`OPS_CONTROL`、`NODE_DAEMON`、`CLOUDREVE_SYNC`、`BACKUP_RECOVERY`、`ALERTING`、`ONLINE_MAP`、`PLUGIN_INTEGRATION`、`CROSS_PLATFORM_NOTIFICATION`、`OPS_IMAGE_MARKET`、`MATERIAL` 和 `GUIDE`。这些模块在测试适配器正常时返回 `AVAILABLE`，适配器报告生产化缺口或局部失败时返回 `DEGRADED`，不可达或超时时返回 `UNAVAILABLE`。只有未来真正没有契约和服务目录的模块，才允许返回 `NOT_IMPLEMENTED`。

`API_GATEWAY` 不是业务模块，不参与普通业务待办和模块注册表。它只作为平台依赖摘要出现在总览和自检中，展示端口、健康、路由数量和生产化缺口，避免 admin 到 gateway 再回到 admin 的循环依赖。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `AdminModuleKey` | `AUTH`、`PROFILE`、`NOTIFICATION`、`CONTENT`、`SERVER_STATUS`、`RESOURCE`、`ADMIN`、`ONBOARDING`、`EXAM`、`WHITELIST`、`ATTENDANCE`、`COMMUNITY`、`ACTIVITY`、`CALENDAR`、`CHANGELOG`、`OPS_CONTROL`、`NODE_DAEMON`、`CLOUDREVE_SYNC`、`BACKUP_RECOVERY`、`ALERTING`、`ONLINE_MAP`、`PLUGIN_INTEGRATION`、`CROSS_PLATFORM_NOTIFICATION`、`OPS_IMAGE_MARKET`、`MATERIAL`、`GUIDE` | 管理后台模块键。 |
| `AdminModuleStatus` | `AVAILABLE`、`DEGRADED`、`UNAVAILABLE`、`NOT_IMPLEMENTED`、`DISABLED` | 模块在后台入口中的可用状态。 |
| `AdminCapabilityType` | `ENTRY`、`READ`、`WRITE`、`REVIEW`、`CONFIG`、`AUDIT`、`OPS_PLACEHOLDER`、`PLATFORM` | 模块能力类型。 |
| `AdminTodoType` | `REVIEW`、`CONFIG`、`FAILURE`、`HEALTH`、`SECURITY`、`FOLLOW_UP` | 待办类型。 |
| `AdminTodoSeverity` | `LOW`、`MEDIUM`、`HIGH`、`CRITICAL` | 待办严重程度。 |
| `AdminTodoStatus` | `OPEN`、`READ_ONLY`、`SOURCE_UNAVAILABLE`、`STALE` | 聚合待办当前可读状态。 |
| `AdminSettingScope` | `GLOBAL`、`MODULE`、`DASHBOARD`、`NAVIGATION`、`AUDIT` | admin 自有配置作用域。 |
| `AdminSettingValueType` | `STRING`、`BOOLEAN`、`INTEGER`、`JSON` | 配置值类型。 |
| `AdminAuditIndexSource` | `ADMIN`、`AUTH`、`PROFILE`、`NOTIFICATION`、`CONTENT`、`SERVER_STATUS`、`RESOURCE`、`ONBOARDING`、`EXAM`、`WHITELIST`、`ATTENDANCE`、`COMMUNITY`、`ACTIVITY`、`CALENDAR`、`CHANGELOG`、`OPS_CONTROL`、`NODE_DAEMON`、`CLOUDREVE_SYNC`、`BACKUP_RECOVERY`、`ALERTING`、`ONLINE_MAP`、`PLUGIN_INTEGRATION`、`CROSS_PLATFORM_NOTIFICATION`、`OPS_IMAGE_MARKET`、`MATERIAL`、`GUIDE`、`API_GATEWAY` | 审计索引来源。 |
| `AdminAuditResult` | `SUCCESS`、`FAILED` | admin 审计结果。 |

## 通用对象

### AdminModuleEntry

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `moduleKey` | string | 是 | 模块键。 |
| `name` | string | 是 | 后台展示名称。 |
| `description` | string | 是 | 模块说明。 |
| `status` | string | 是 | `AdminModuleStatus`。 |
| `implemented` | boolean | 是 | 是否已有后端服务或 admin 自身实现。 |
| `enabled` | boolean | 是 | 是否在后台导航中启用。 |
| `requiredRoles` | string[] | 是 | 访问该模块入口需要的基础角色。 |
| `requiredPermissions` | string[] | 是 | 访问该模块入口需要的能力点，普通业务模块通常为空数组，运维和平台模块按正式契约填写。 |
| `frontendRoute` | string | 是 | 推荐前端路由，例如 `/admin/content`。 |
| `targetApiBase` | string 或 null | 是 | 来源模块后台 API 前缀。当前已闭环模块必须为非空，未来未实现模块为 `null`。 |
| `sortOrder` | integer | 是 | 导航排序，数字越小越靠前。 |
| `badgeCount` | integer | 是 | 模块待办数或异常数。 |
| `capabilities` | AdminCapabilityEntry[] | 是 | 模块能力列表。 |
| `health` | AdminModuleHealth | 是 | 模块健康摘要。 |
| `updatedAt` | string | 是 | 入口配置更新时间。 |

### AdminCapabilityEntry

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `key` | string | 是 | 能力键，同一模块内唯一。 |
| `type` | string | 是 | `AdminCapabilityType`。 |
| `label` | string | 是 | 展示名称。 |
| `targetRoute` | string | 是 | 前端目标路由。 |
| `targetApi` | string 或 null | 是 | 来源模块目标 API。 |
| `requiredRoles` | string[] | 是 | 需要的基础角色。 |
| `requiredPermissions` | string[] | 是 | 需要的能力点。 |
| `riskLevel` | string | 是 | 公共风险等级。 |
| `available` | boolean | 是 | 当前是否可用。 |
| `readOnly` | boolean | 是 | 是否只读。 |

### AdminModuleHealth

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `moduleKey` | string | 是 | 模块键。 |
| `status` | string | 是 | `AdminModuleStatus`。 |
| `service` | string | 是 | 服务名。 |
| `port` | integer 或 null | 是 | 当前运行入口端口。当前已闭环模块必须为非空，未来未实现模块为 `null`。第二批 `ONBOARDING`、`EXAM`、`WHITELIST` 和 `ATTENDANCE` 已由 `admission-core-service` 承载，必须返回 `8131`，历史原服务端口只保留在各自业务契约的 `legacyPort` 中。 |
| `storageMode` | string 或 null | 是 | 例如 `IN_MEMORY`。 |
| `authMode` | string 或 null | 是 | 认证适配模式。 |
| `lastCheckedAt` | string | 是 | 最近检查时间。 |
| `latencyMs` | integer 或 null | 是 | 最近检查耗时。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReason` | string 或 null | 是 | 降级原因，不能包含 token、密码、内部路径或原始异常堆栈。 |
| `productionGaps` | string[] | 是 | 生产化缺口摘要。 |

### AdminPlatformDependency

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `key` | string | 是 | 平台依赖键。当前固定可返回 `API_GATEWAY`。 |
| `name` | string | 是 | 展示名称。 |
| `status` | string | 是 | `AVAILABLE`、`DEGRADED` 或 `UNAVAILABLE`。 |
| `service` | string | 是 | 服务名，例如 `api-gateway`。 |
| `port` | integer | 是 | 固定端口。`API_GATEWAY` 为 `8125`。 |
| `targetApiBase` | string | 是 | 平台依赖的只读后台 API 前缀，例如 `/api/v1/gateway/admin`。 |
| `frontendRoute` | string | 是 | 推荐前端平台依赖入口，例如 `/admin/platform/api-gateway`。 |
| `routeCount` | integer | 是 | 网关路由摘要数量。未知时为 `0` 且必须标记降级。 |
| `lastCheckedAt` | string | 是 | 最近检查时间。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReason` | string 或 null | 是 | 降级原因，不能包含 token、密码、内部路径或原始异常堆栈。 |
| `productionGaps` | string[] | 是 | 平台依赖生产化缺口摘要。 |

### AdminTodoItem

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `todoId` | string | 是 | admin 聚合待办 ID。 |
| `sourceModule` | string | 是 | 来源模块。 |
| `sourceType` | string | 是 | 来源类型，例如 `CONTENT_REVIEW`、`RESOURCE_REVIEW`、`SERVER_OUTAGE`。 |
| `sourceId` | string | 是 | 来源业务 ID。 |
| `type` | string | 是 | `AdminTodoType`。 |
| `severity` | string | 是 | `AdminTodoSeverity`。 |
| `status` | string | 是 | `AdminTodoStatus`。 |
| `title` | string | 是 | 待办标题，最多 80 位。 |
| `summary` | string | 是 | 待办摘要，最多 300 位。 |
| `targetRoute` | string | 是 | 处理入口前端路由。 |
| `targetApi` | string 或 null | 是 | 来源模块查询或处理 API。 |
| `readOnly` | boolean | 是 | 聚合待办固定为 `true`，处理动作回到来源模块。 |
| `createdAt` | string | 是 | 来源待办创建时间。 |
| `updatedAt` | string | 是 | 来源待办更新时间。 |
| `indexedAt` | string | 是 | admin 聚合时间。 |

### AdminMetricSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `metricKey` | string | 是 | 指标键。 |
| `label` | string | 是 | 指标名称。 |
| `sourceModule` | string | 是 | 来源模块。 |
| `value` | integer | 是 | 指标值。 |
| `unit` | string | 是 | 单位，例如 `count`。 |
| `trend` | string 或 null | 是 | 当前可为 `null`。 |
| `targetRoute` | string | 是 | 点击后进入的前端路由。 |
| `degraded` | boolean | 是 | 指标是否来自降级摘要。 |
| `updatedAt` | string | 是 | 指标更新时间。 |

### AdminAuditIndexEntry

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 是 | admin 审计索引 ID。 |
| `sourceModule` | string | 是 | `AdminAuditIndexSource`。 |
| `sourceAuditId` | string | 是 | 来源审计 ID。 |
| `requestId` | string | 是 | 请求编号。 |
| `actorUserId` | string | 是 | 操作者用户 ID。 |
| `actorDisplayName` | string 或 null | 是 | 操作者展示名快照。 |
| `actorRole` | string | 是 | 操作者主角色。 |
| `targetType` | string | 是 | 目标类型。 |
| `targetId` | string | 是 | 目标 ID。 |
| `action` | string | 是 | 动作。 |
| `riskLevel` | string | 是 | 风险等级。 |
| `result` | string | 是 | `SUCCESS` 或 `FAILED`。 |
| `reasonSummary` | string 或 null | 是 | 操作原因摘要，最长 120 位，不返回原因全文。 |
| `failureReason` | string 或 null | 是 | 失败原因摘要，不返回堆栈。 |
| `targetRoute` | string | 是 | 目标前端路由。 |
| `indexedAt` | string | 是 | 索引时间。 |
| `createdAt` | string | 是 | 来源审计创建时间。 |

### AdminSettingItem

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `key` | string | 是 | 配置键，格式为小写字母、数字、点和短横线。 |
| `scope` | string | 是 | `AdminSettingScope`。 |
| `valueType` | string | 是 | `AdminSettingValueType`。 |
| `value` | string、boolean、integer 或 object | 是 | 配置值。 |
| `description` | string | 是 | 配置说明。 |
| `sensitive` | boolean | 是 | 是否敏感。admin 接口不允许返回敏感明文。 |
| `highImpact` | boolean | 是 | 是否高影响配置。 |
| `updatedBy` | string 或 null | 是 | 最近修改人。 |
| `updatedAt` | string | 是 | 最近修改时间。 |

### AdminSettingsSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `items` | AdminSettingItem[] | 是 | 配置项。 |
| `layout` | AdminLayoutConfig | 是 | 看板布局。 |
| `modules` | AdminModuleEntry[] | 是 | 模块入口配置摘要。 |
| `updatedAt` | string | 是 | 快照更新时间。 |

### AdminLayoutConfig

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `dashboardCards` | string[] | 是 | 看板卡片顺序。 |
| `navigationModuleOrder` | string[] | 是 | 导航模块顺序。 |
| `hiddenModules` | string[] | 是 | 隐藏模块键。默认模块列表和总览不得返回被隐藏模块；只有 `OWNER` 使用 `includeDisabled=true` 时可以看到被隐藏模块，状态为 `DISABLED`。 |
| `quickActions` | AdminQuickAction[] | 是 | 快捷入口，只能指向已实现且当前用户有权访问的来源模块路由。 |

### AdminQuickAction

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `key` | string | 是 | 快捷入口键，格式为小写字母、数字和短横线，同一布局内唯一。 |
| `targetRoute` | string | 是 | 必须指向已实现模块的后台入口或其下级路由，例如 `/admin/content`。不得指向未实现模块、运维真实操作、外部 URL 或 `/api` 路径。 |

### AdminOverview

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `modules` | AdminModuleEntry[] | 是 | 当前用户可见模块入口。 |
| `todoSummary` | object | 是 | 按严重程度和来源模块统计的待办数。 |
| `metrics` | AdminMetricSummary[] | 是 | 关键指标。 |
| `recentAudits` | AdminAuditIndexEntry[] | 是 | 最近审计索引，最多 10 条。 |
| `degradedModules` | string[] | 是 | 降级或不可用模块。 |
| `notImplementedModules` | string[] | 是 | 未实现模块。 |
| `platformDependencies` | AdminPlatformDependency[] | 是 | 平台依赖摘要。`API_GATEWAY` 只在这里展示，不参与普通业务待办。 |
| `generatedAt` | string | 是 | 生成时间。 |

### AdminOpsSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `service` | string | 是 | 固定为 `admin`。 |
| `port` | integer | 是 | 固定为 `8107`。 |
| `storageMode` | string | 是 | 当前可为 `IN_MEMORY`，生产化后应返回真实持久化模式。 |
| `authMode` | string | 是 | 当前可为 `TEST_STUB` 或生产适配名。 |
| `moduleAdapterMode` | string | 是 | 模块适配模式。 |
| `modulesTotal` | integer | 是 | 模块总数。 |
| `availableModulesTotal` | integer | 是 | 可用模块数。 |
| `degradedModulesTotal` | integer | 是 | 降级模块数。 |
| `notImplementedModulesTotal` | integer | 是 | 未实现模块数。 |
| `todosIndexedTotal` | integer | 是 | 当前待办索引数。 |
| `auditIndexesTotal` | integer | 是 | 审计索引数。 |
| `settingsTotal` | integer | 是 | 配置项数。 |
| `idempotencyRecordsTotal` | integer | 是 | 幂等记录数。 |
| `lastAggregatedAt` | string | 是 | 最近聚合时间。 |
| `productionGaps` | string[] | 是 | 生产化缺口。 |
| `platformDependencies` | AdminPlatformDependency[] | 是 | 平台依赖摘要。 |

## admin 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `43700` | 404 | admin 配置、模块入口或审计索引不存在。 |
| `43701` | 404 | 待办项不存在或已不可见。 |
| `43710` | 409 | admin 配置状态不允许当前操作。 |
| `43711` | 409 | admin 配置键或模块入口冲突。 |
| `43712` | 409 | 幂等键请求指纹冲突。 |
| `43713` | 409 | 模块未实现、已禁用或当前不可用，不能作为可操作入口。 |
| `46700` | 502 | 来源模块不可用。 |
| `46701` | 504 | 来源模块调用超时。 |
| `46702` | 502 | 来源模块响应字段或枚举不兼容 admin 契约。 |
| `46703` | 502 | auth 认证上下文不可用。 |
| `46704` | 504 | auth 认证上下文调用超时。 |
| `51700` | 500 | admin 内部错误。 |
| `51701` | 500 | admin 审计写入失败。 |
| `51702` | 500 | admin 配置写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误、通用幂等键冲突和通用服务端错误优先使用公共错误码。admin 自有幂等指纹冲突使用 `43712`。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 管理后台总览 | GET | `/api/v1/admin/overview` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 模块注册表 | GET | `/api/v1/admin/modules` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 模块详情 | GET | `/api/v1/admin/modules/{moduleKey}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 待办聚合列表 | GET | `/api/v1/admin/todos` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 待办详情 | GET | `/api/v1/admin/todos/{todoId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 看板指标摘要 | GET | `/api/v1/admin/metrics/summary` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 审计索引列表 | GET | `/api/v1/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 系统配置快照 | GET | `/api/v1/admin/settings` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 更新系统配置 | PATCH | `/api/v1/admin/settings` | 是 | `ADMIN` 或 `OWNER`，高影响配置只允许 `OWNER` | MEDIUM |
| admin 自检摘要 | GET | `/api/v1/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 总览接口

### 管理后台总览

`GET /api/v1/admin/overview`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `includeDisabled` | boolean | 否 | 默认 `false`。只有 `OWNER` 可传 `true`。 |
| `moduleLimit` | integer | 否 | 默认返回全部可见模块，范围 `1` 到 `50`。 |
| `todoLimit` | integer | 否 | 默认 `10`，范围 `0` 到 `50`。 |
| `auditLimit` | integer | 否 | 默认 `10`，范围 `0` 到 `50`。 |

成功响应 HTTP `200`，`data` 为 `AdminOverview`。

业务规则：总览只返回当前用户有权访问的模块入口和能力。`HELPER` 可以看到可读模块、只读待办和可读指标，但不能看到系统配置入口和审计索引入口。`ADMIN` 可以看到普通配置入口和审计索引入口。`OWNER` 可以看到全局高影响配置入口、运维控制入口和平台依赖摘要。当前 26 个已闭环服务不得再出现在 `notImplementedModules` 中。`API_GATEWAY` 只能出现在 `platformDependencies`，不能作为普通业务模块返回。

降级规则：任一来源模块不可用时，总览仍应返回其他模块数据，并将该模块加入 `degradedModules`，对应指标值为 `0` 且 `degraded=true`。auth 认证上下文不可用时不得返回总览，返回 `46703` 或 `46704`。

敏感字段：总览不得返回访问令牌、邀请码原文、Cloudreve 密码、内部文件路径、通知正文、模板正文、后台备注全文、审计参数全文或异常堆栈。

## 模块接口

### 模块注册表

`GET /api/v1/admin/modules`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `includeDisabled` | boolean | 否 | 默认 `false`。只有 `OWNER` 可传 `true`。 |
| `includeNotImplemented` | boolean | 否 | 默认 `true`。 |
| `status` | string | 否 | 任一 `AdminModuleStatus`。 |
| `keyword` | string | 否 | 匹配模块名、说明或模块键，最多 50 位。 |
| `sort` | string | 否 | 允许 `sortOrder_asc`、`moduleKey_asc`、`updatedAt_desc`。默认 `sortOrder_asc`。 |

成功响应 HTTP `200`，`data.items` 为 `AdminModuleEntry[]`。

业务规则：已实现模块必须包含 `AUTH`、`PROFILE`、`NOTIFICATION`、`CONTENT`、`SERVER_STATUS`、`RESOURCE`、`ADMIN`、`ONBOARDING`、`EXAM`、`WHITELIST`、`ATTENDANCE`、`COMMUNITY`、`ACTIVITY`、`CALENDAR`、`CHANGELOG`、`OPS_CONTROL`、`NODE_DAEMON`、`CLOUDREVE_SYNC`、`BACKUP_RECOVERY`、`ALERTING`、`ONLINE_MAP`、`PLUGIN_INTEGRATION`、`CROSS_PLATFORM_NOTIFICATION`、`OPS_IMAGE_MARKET`、`MATERIAL` 和 `GUIDE`。这些模块正常时 `implemented=true` 且 `status=AVAILABLE`，`targetApiBase` 必须指向对应正式 API 前缀。未实现状态只留给未来没有正式契约和服务目录的模块。被 `hiddenModules` 隐藏的模块视为 admin 自有配置禁用，默认不返回；只有 `OWNER` 传 `includeDisabled=true` 时可返回，且状态必须为 `DISABLED`。

### 模块详情

`GET /api/v1/admin/modules/{moduleKey}`

成功响应 HTTP `200`，`data` 为 `AdminModuleEntry`。

业务规则：`moduleKey` 必须是 `AdminModuleKey`。不存在或非法返回 `40001` 或 `43700`。当前用户无权查看该模块入口时返回 `42001`。当前 26 个已闭环服务详情不得返回 `NOT_IMPLEMENTED`。未来未实现模块详情可以返回 `NOT_IMPLEMENTED`，但不能返回真实业务指标、待办或写能力。

降级规则：来源模块自检失败时，详情仍返回入口配置，`health.status` 为 `DEGRADED` 或 `UNAVAILABLE`，`capabilities[].available=false`。

## 待办接口

### 待办聚合列表

`GET /api/v1/admin/todos`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `sourceModule` | string | 否 | 任一 `AdminModuleKey` 中已实现模块。 |
| `type` | string | 否 | 任一 `AdminTodoType`。 |
| `severity` | string | 否 | 任一 `AdminTodoSeverity`。 |
| `status` | string | 否 | 任一 `AdminTodoStatus`。 |
| `keyword` | string | 否 | 匹配标题、摘要、来源 ID，最多 80 位。 |
| `sort` | string | 否 | 允许 `severity_desc`、`updatedAt_desc`、`createdAt_desc`、`sourceModule_asc`。默认 `severity_desc` 后 `updatedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `AdminTodoItem[]`。

业务规则：待办只聚合已实现来源模块和 admin 自身的只读摘要。`content` 可产生待审核内容、首页草稿未发布、SEO 配置异常。`resource` 可产生待审核资源、下载入口过期、Cloudreve 降级。`notification` 可产生模板禁用、投递失败、外部投递缺口。`server-status` 可产生未确认宕机、采集失败、线路异常。`profile` 可产生待激活成员或成员资料异常。`auth` 可产生管理员邀请码风险、禁用用户安全事件或会话异常摘要。`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、`material` 和 `guide` 可以返回审核、补充、反馈、活动、日程、素材或指南类只读待办。`ops-control`、`node-daemon`、`cloudreve-sync`、`backup-recovery`、`alerting`、`online-map`、`plugin-integration`、`cross-platform-notification` 和 `ops-image-market` 只能返回健康、审批、同步、告警或兼容计划类摘要，不能让 admin 代替它们执行真实操作。`API_GATEWAY` 不产生普通业务待办。

待办处理动作不在 `admin` 内完成。`readOnly` 必须为 `true`，前端根据 `targetRoute` 和 `targetApi` 跳回来源模块。

降级规则：来源模块不可用时，该来源模块待办可以省略，也可以返回 `SOURCE_UNAVAILABLE` 占位待办，但必须标记 `degraded=true` 或 `status=SOURCE_UNAVAILABLE`，不能伪造真实数量。

### 待办详情

`GET /api/v1/admin/todos/{todoId}`

成功响应 HTTP `200`，`data` 为 `AdminTodoItem`，可补充 `context` 对象。

业务规则：`context` 只能包含只读摘要，例如来源状态、来源标题、来源更新时间、建议入口和下一步说明。不得包含来源模块后台备注全文、通知正文、模板正文、Cloudreve 分享密码、内部路径、token、请求头或审计参数全文。待办不存在或当前用户无权查看来源模块时返回 `43701` 或 `42001`。

## 指标接口

### 看板指标摘要

`GET /api/v1/admin/metrics/summary`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `sourceModule` | string | 否 | 任一已实现模块。 |
| `includeDegraded` | boolean | 否 | 默认 `true`。 |

成功响应 HTTP `200`，`data.items` 为 `AdminMetricSummary[]`。

业务规则：指标摘要可以包括用户、成员、通知、内容、服务器状态、资源、入服、考试、白名单、考勤、社区、活动、日历、更新日志、运维控制、节点守护、Cloudreve 同步、备份恢复、告警、在线地图、插件集成、跨平台通知、镜像市场、素材、指南和 admin 自身的计数或健康摘要。指标只用于后台看板，不作为业务判定来源。未知或降级指标不得伪装成真实值。

降级规则：部分模块不可用时，其他模块指标正常返回；不可用模块指标返回 `value=0`、`degraded=true` 或被省略，并在响应中保留模块健康摘要。不得把未知值伪造成真实 0 而不标记降级。

## 审计索引接口

### 审计索引列表

`GET /api/v1/admin/audit-logs`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `sourceModule` | string | 否 | 任一 `AdminAuditIndexSource`。 |
| `actorUserId` | string | 否 | 操作者用户 ID。 |
| `action` | string | 否 | 动作，最多 80 位。 |
| `result` | string | 否 | `SUCCESS` 或 `FAILED`。 |
| `riskLevel` | string | 否 | 公共风险等级。 |
| `targetType` | string | 否 | 目标类型，最多 80 位。 |
| `targetId` | string | 否 | 目标 ID，最多 80 位。 |
| `from` | string | 否 | ISO 8601 起始时间。 |
| `to` | string | 否 | ISO 8601 结束时间。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`、`riskLevel_desc`。默认 `createdAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `AdminAuditIndexEntry[]`。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 返回 `42001`。

业务规则：审计索引是只读摘要，不是审计主数据。admin 不提供删除、修改或恢复审计索引的接口。来源可以是 26 个已闭环服务和 `API_GATEWAY` 平台依赖。`from` 和 `to` 必须按来源审计 `createdAt` 做闭区间过滤，不能只校验格式后忽略范围。返回结果必须脱敏，不能返回访问令牌、完整请求头、邀请码原文、Cloudreve 密码、内部文件路径、通知正文、模板正文、审计参数全文、节点密钥、外部 webhook、registry token 或异常堆栈。

降级规则：某个来源模块审计不可用时，列表仍返回可用来源和 admin 自身审计，并在响应中标记来源模块降级。若请求指定的唯一来源模块不可用，可返回 `46700` 或空分页加降级摘要；同一实现版本内必须固定并写入测试。

## 系统配置接口

### 系统配置快照

`GET /api/v1/admin/settings`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `scope` | string | 否 | 任一 `AdminSettingScope`。 |
| `includeHighImpact` | boolean | 否 | 默认 `false`。只有 `OWNER` 可传 `true`。 |

成功响应 HTTP `200`，`data` 为 `AdminSettingsSnapshot`。

业务规则：只返回 admin 自有配置，例如模块菜单显隐、导航排序、看板卡片排序、快捷入口、模块降级展示策略、平台依赖展示策略、审计索引保留天数和聚合刷新间隔。`quickActions` 必须按当前用户权限和模块显隐过滤，不能返回当前用户无权访问、已隐藏、未实现或不可用模块的入口。不得返回或修改任何来源服务的业务配置、运维配置、节点配置、外部平台凭据或素材上传配置。

敏感字段：不允许通过该接口返回敏感明文。若未来出现敏感配置，只能返回 `sensitive=true` 和脱敏摘要。

### 更新系统配置

`PATCH /api/v1/admin/settings`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `items` | array | 否 | 要更新的配置项，最多 50 条。每条包含 `key` 和 `value`。 |
| `layout` | object | 否 | 可更新 `dashboardCards`、`navigationModuleOrder`、`hiddenModules` 和 `quickActions`。 |
| `reason` | string | 是 | 1 到 200 位，修改原因。 |
| `idempotencyKey` | string | 是 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为更新后的 `AdminSettingsSnapshot`。

权限规则：普通配置允许 `ADMIN` 或 `OWNER` 修改。`audit.retentionDays`、全局模块禁用、隐藏 `AUTH`、隐藏 `ADMIN`、隐藏全部已实现模块、调整运维入口可见性等高影响配置只允许 `OWNER` 修改。`HELPER` 和 `USER` 返回 `42001`。

业务规则：配置键必须属于 admin 已声明配置。未知键返回 `43700` 或 `40001`。配置值类型必须匹配 `valueType`。`layout.quickActions` 必须是 `AdminQuickAction[]`，每个入口必须指向已实现、未隐藏且当前操作者有权访问的后台模块路由；指向未实现模块、运维真实操作、外部 URL、`/api` 路径或缺少必填字段时返回 `40001` 或 `43713`，同一实现版本内固定。`layout.hiddenModules` 隐藏模块后，默认模块注册表、总览和设置快照中的模块列表必须同步体现禁用状态。同一 `idempotencyKey`、同一操作者、同一请求体重复提交返回同一结果。相同幂等键搭配不同请求体返回 `43712`。配置写入和审计写入必须全有或全无，任一失败返回 `51701` 或 `51702`，不得半更新。

审计要求：成功写入 `ADMIN_SETTINGS_UPDATED`，记录变更配置键、变更前后摘要、操作者、原因和风险等级。高影响配置写入风险等级为 `MEDIUM`，后续如涉及运维入口真实启用，应升级到 `HIGH` 或 `CRITICAL` 并转交 `ops-control`。

## 自检接口

### admin 自检摘要

`GET /api/v1/admin/ops/summary`

成功响应 HTTP `200`，`data` 为 `AdminOpsSummary`，可补充 `moduleHealth` 数组。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。

业务规则：自检摘要用于确认 admin 当前运行模式、模块适配状态、平台依赖状态、配置规模、待办索引规模、审计索引规模和生产化缺口。当前可返回 `storageMode=IN_MEMORY`、`authMode=TEST_STUB`、`moduleAdapterMode=TEST_STUB`。`productionGaps` 至少说明真实持久化、真实认证适配、真实来源模块 HTTP 适配、真实审计索引同步、真实定时聚合是否未启用。`platformDependencies` 必须包含 `API_GATEWAY`，但不得把 gateway 当作业务待办来源。

生产化边界：自检摘要必须返回 `testMode`。测试模式下 `testMode=true`，允许测试钩子驱动降级和失败分支；生产模式下 `testMode=false`，测试钩子不生效。`authMode` 在消费可信身份头时应返回 `TRUSTED_GATEWAY_CONTEXT` 或等价生产适配名，不能继续声称只有 `TEST_STUB`。

敏感字段：自检摘要不得返回 token、密码、请求头、邀请码原文、Cloudreve 密码、内部文件路径、后台备注全文、审计参数全文、节点密钥、服务器系统路径或异常堆栈。

## 状态、幂等和并发

模块状态由 admin 自有配置和模块适配器结果共同决定。模块未实现时为 `NOT_IMPLEMENTED`。模块被 admin 自有配置关闭时为 `DISABLED`。模块适配器成功返回兼容摘要时为 `AVAILABLE`。适配器部分失败或来源模块自检报告降级时为 `DEGRADED`。适配器不可达或超时时为 `UNAVAILABLE`。当前 26 个已闭环服务默认不允许被标记为 `NOT_IMPLEMENTED`。

配置更新支持 `idempotencyKey`。同一操作者、同一幂等键、同一请求体重复提交时返回同一个结果；相同幂等键搭配不同请求体返回 `43712`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象都要按字段名递归排序，不能受任意层级的字段顺序影响。

配置更新、审计写入和幂等记录写入必须保持一致。并发更新同一配置时必须以服务端当前状态为准，不能产生半更新快照。实现可以使用版本号、更新时间或事务锁保证配置键唯一和幂等记录唯一。

## 审计要求

必须审计的动作包括系统配置更新、高影响配置更新、模块显隐调整、看板布局更新、审计索引策略更新、聚合适配失败被管理员触发刷新时的失败记录。后台低风险读取不强制写审计。

审计字段继承公共契约。admin 自身审计写入失败时，配置写操作不得假装成功，必须返回 `51701` 并保持业务数据不变。审计索引读取失败不得删除来源审计，也不得修改来源模块主数据。

## 失败降级

总览、模块列表、模块详情、待办列表、指标摘要和审计索引读取都应支持局部降级。单个来源模块不可用时，不影响其他模块入口和 admin 自有配置读取。响应必须清楚标记降级模块，不能把未知值当真实值返回。

认证上下文不可用、认证上下文超时、admin 自有配置存储不可用、admin 审计写入失败、配置写入失败和响应字段无法满足契约时，不允许伪造成功。

`admin` 不得因为某个来源模块或 `api-gateway` 不可用而整体失败。来源模块降级时返回局部降级摘要；`API_GATEWAY` 降级时只影响平台依赖摘要，不影响普通模块注册表。

## 验收口径

`admin` API 文档按 `docs/contracts-admin.md` 独立存在，并由 `.local-docs/tests-admin.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`admin` 完成时必须满足以下条件：全部接口按本文档实现；后台接口全部要求登录；`USER` 不能访问 admin；`HELPER` 只能读允许的聚合入口、待办和指标；`ADMIN` 可读审计和改普通配置；`OWNER` 才能改高影响配置；总览、模块、待办、指标、审计索引、设置和自检都不泄露敏感字段；当前 26 个已闭环服务全部被识别为已实现；`API_GATEWAY` 只作为平台依赖摘要出现；来源模块不可用时局部降级；可信认证上下文优先于本地测试 token；生产模式下测试钩子不生效；配置更新幂等、审计和回滚有测试；端口固定为 `8107`；`.local-docs/tests-admin.md` 中全部测试用例都有对应自动化验证；未实现或行为未满足时自动化测试必须先失败；实现后 `admin` 全部测试通过；`api-gateway` 和 26 个后端服务回归测试通过；没有修改来源服务稳定接口；没有把业务写代理、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复、Cloudreve 管理 token、外部平台凭据或真实运维执行能力塞进 `admin`。
