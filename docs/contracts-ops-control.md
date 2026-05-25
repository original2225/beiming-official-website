# 北冥官网 ops-control API 契约

版本：0.1

## 文档定位

本文档是 `ops-control` 微服务的正式 API 契约。后续前端运维控制台、`admin` 聚合、`node-daemon` 和其他业务模块只能通过本文档定义的接口读取或管理服务器运维控制面，不能直接读取或修改 `ops-control` 数据，也不能把真实服务器命令、文件系统、Docker、虚拟机、Minecraft 实例或终端执行能力塞进其他服务。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、运维能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `ops-control` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、任务流转、审批流转、节点降级、审计和验收口径。

本文档参考 Docker Engine API、Kubernetes API、Proxmox VE API、Portainer、Cockpit 和 MCSManager 的公开设计。Docker Engine API 将容器、日志、生命周期动作和 exec 会话分开，说明控制面不能使用万能操作接口。Kubernetes API 强调资源对象、状态、watch 和 RBAC，说明对象状态和动作任务应分离。Proxmox VE API 的节点和任务模型说明运维动作应异步化并可追踪。Portainer 和 Cockpit 都强调控制面、权限、审计和受控端分离。MCSManager 的面板和 Daemon 分离适合 Minecraft 实例、日志和控制台视图。本项目只吸收这些产品思路，不接入它们的主数据，也不在 `ops-control` 内直接执行宿主机命令。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| Docker Engine API | 容器对象、日志、生命周期动作和 exec 会话拆分。 |
| Kubernetes API | 资源对象、状态、权限和 watch 分层。 |
| Proxmox VE API | 节点、虚拟机、存储和异步任务可追踪。 |
| Portainer API | 控制面负责端点、资源视图、权限和审计，执行交给受控环境。 |
| Cockpit Guide | Web 管理控制台以受控通道管理主机、服务、终端和日志。 |
| MCSManager 文档 | Minecraft 实例面板、节点 Daemon、日志和控制台能力分离。 |

## 职责边界

`ops-control` 负责后台服务器与资源运维控制面，包括节点注册、节点启用禁用、节点能力、心跳摘要、资产清单、节点指标快照、容器快照、虚拟机快照、Minecraft 实例快照、授权目录文件视图、文本文件读取请求、日志摘要请求、受控操作任务、高风险审批、任务取消、节点回写、运维审计和自检摘要。

`ops-control` 不负责注册、登录、角色能力点主数据、玩家资源下载、玩家可见服务器状态采集、官网公告、活动日历主数据、更新日志主数据、真实 Docker 操作、真实 Proxmox 操作、真实 MCSManager 操作、真实 shell 命令、真实文件上传下载、真实文件删除、真实终端 WebSocket、真实备份恢复或节点守护进程执行逻辑。

真实服务器上的系统资源、进程、容器、文件、日志、Minecraft 实例和终端命令必须由后续 `node-daemon` 或受控适配器执行。P1 `ops-control` 只做控制面契约、授权、审计、任务状态、模拟节点适配和离线降级，不直接调用宿主机。

## 数据归属

`ops-control` 拥有以下主数据：节点、节点认证摘要、节点能力、节点心跳、资产、资产分组、指标快照、容器快照、虚拟机快照、Minecraft 实例快照、授权目录快照、文件读取请求、日志查询请求、受控操作任务、高风险审批、节点回写摘要、幂等记录、运维审计和自检统计。

`ops-control` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和用户状态快照；可以保存来自 `admin` 的入口引用和聚合摘要；可以保存来自 `server-status` 的玩家可见实例 ID 和公开名称快照；可以保存来自 `resource` 的 Cloudreve 服务资产引用和公开资源关联快照；可以保存来自 `calendar`、`changelog` 的维护窗口或版本发布引用快照。所有跨服务字段只能是快照或正式接口结果，不得直接读取前序服务数据库、内存 store、测试种子或私有类。

## 基础路径与认证

所有接口默认使用 `/api/v1/ops-control` 前缀。P1 本地端口固定为 `8116`，自检摘要必须返回该端口。

全部接口都要求 `Authorization: Bearer <token>`。读取类接口要求基础角色为 `HELPER`、`ADMIN` 或 `OWNER`，且具备对应读取能力点。写入或操作类接口要求 `ADMIN` 或 `OWNER`，并按目标能力点校验。高风险操作必须携带二次确认。严重风险操作必须由 `OWNER` 或具备 `HIGH_RISK_APPROVE` 的审批记录授权。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`nodeTokenDigest`、`credential`、`beforeState`、`afterState`、`taskStatus`、`approvalStatus`、`auditResult`、`createdBy`、`updatedBy` 等服务端可信字段。出现可信字段时，P1 可以忽略，但不得信任；涉及高风险写操作时推荐返回 `40001`。

## 本地测试控制头

`ops-control` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Node-Mode`、`X-Test-Admin-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store`、`X-Test-Now` 模拟认证失败、节点离线、节点超时、节点坏 schema、审计失败、存储失败和时间边界。该能力只服务本地契约测试，不属于正式业务 API。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、节点失败、状态失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 视为已满足的生产化硬化项。

## 前序服务兼容契约

`auth` 是强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问任何接口。auth 不可用返回 `49200`，auth 超时返回 `49201`，字段或枚举不兼容返回 `49202`。

`admin` 是聚合入口。`ops-control` 可以向 admin 暴露模块健康、待办、审计索引和运维摘要，但不能要求 admin 直接写运维主数据。admin 不可用时，自检和运维主接口不应伪造 admin 已同步成功，只能返回降级摘要或 `49210`。

`server-status` 是玩家可见状态服务。`ops-control` 可以保存玩家可见实例名称快照，不能要求 `server-status` 执行启停、终端、容器或文件操作。快照不可用返回 `49220`，已有快照读取可标记 stale。

`resource` 是玩家资源下载服务。`ops-control` 可以登记 Cloudreve、数据盘或备份盘资产，不能通过 `resource` 读取后台文件或把玩家资源权限当作服务器文件权限。resource 快照不可用返回 `49230`。

`calendar` 和 `changelog` 是只读辅助关联来源。维护窗口、版本发布影响可保存为快照。不可用时不阻断运维任务创建，但必须在任务或审计中记录降级摘要。

`node-daemon` 尚未开发。P1 节点调用模式固定为 `SIMULATED`。节点离线时只允许读取最后快照，任何需要实时执行的操作必须返回任务失败或 `49260`，不能假装成功。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `OpsNodeStatus` | `PENDING_REGISTRATION`、`ONLINE`、`DEGRADED`、`OFFLINE`、`DISABLED`、`REVOKED` | 节点控制面状态。 |
| `OpsAssetType` | `PHYSICAL_SERVER`、`CLOUD_SERVER`、`LXC_CONTAINER`、`DOCKER_CONTAINER`、`VIRTUAL_MACHINE`、`MINECRAFT_INSTANCE`、`CLOUDREVE_SERVICE`、`REVERSE_PROXY`、`DATABASE`、`CACHE`、`DATA_DISK`、`BACKUP_DISK`、`DOMAIN`、`LINE` | 资产类型。 |
| `OpsAssetStatus` | `ACTIVE`、`MAINTENANCE`、`DISABLED`、`ARCHIVED` | 资产状态。 |
| `OpsCapability` | `NODE_READ`、`NODE_WRITE`、`CONTAINER_OPERATE`、`VM_OPERATE`、`FILE_MANAGE`、`TERMINAL_ACCESS`、`HIGH_RISK_APPROVE` | 运维能力点。 |
| `OpsRuntimeStatus` | `RUNNING`、`STOPPED`、`PAUSED`、`STARTING`、`STOPPING`、`FAILED`、`UNKNOWN` | 容器、虚拟机和实例运行状态。 |
| `OpsTaskType` | `NODE_REGISTER`、`NODE_DISABLE`、`NODE_ENABLE`、`NODE_TOKEN_ROTATE`、`CONTAINER_START`、`CONTAINER_STOP`、`CONTAINER_RESTART`、`CONTAINER_DELETE`、`VM_START`、`VM_SHUTDOWN`、`VM_REBOOT`、`VM_FORCE_STOP`、`MC_START`、`MC_STOP`、`MC_RESTART`、`MC_COMMAND`、`FILE_READ`、`FILE_WRITE`、`FILE_RENAME`、`FILE_MOVE`、`FILE_DELETE`、`LOG_QUERY`、`TERMINAL_COMMAND`、`BACKUP_CREATE`、`BACKUP_RESTORE` | 受控任务类型。 |
| `OpsTaskStatus` | `PENDING_APPROVAL`、`QUEUED`、`DISPATCHED`、`RUNNING`、`SUCCEEDED`、`FAILED`、`CANCELED`、`TIMEOUT` | 任务状态。 |
| `OpsApprovalStatus` | `PENDING`、`APPROVED`、`REJECTED`、`EXPIRED`、`CANCELED` | 高风险审批状态。 |
| `OpsRiskLevel` | `LOW`、`MEDIUM`、`HIGH`、`CRITICAL` | 继承公共风险等级并用于任务和审计。 |
| `OpsAuditResult` | `SUCCESS`、`FAILED` | 运维审计结果。 |

## 通用对象

### OpsNode

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `nodeId` | string | 是 | 节点 ID。 |
| `displayName` | string | 是 | 节点名称，2 到 80 位。 |
| `status` | string | 是 | `OpsNodeStatus`。 |
| `endpointSummary` | string | 是 | 脱敏端点摘要，不得返回完整 token、密码或私钥。 |
| `version` | string 或 null | 是 | 节点守护进程版本快照。 |
| `capabilities` | string[] | 是 | 节点上报能力。 |
| `labels` | object | 是 | 标签，最多 20 个键值。 |
| `lastHeartbeatAt` | string 或 null | 是 | 最近心跳时间。 |
| `lastSeenRequestId` | string 或 null | 是 | 最近节点请求编号。 |
| `tokenDigest` | string | 后台详情可见 | 节点 token 摘要，不能返回原文。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### OpsAsset

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `assetId` | string | 是 | 资产 ID。 |
| `nodeId` | string 或 null | 是 | 所属节点。 |
| `assetType` | string | 是 | `OpsAssetType`。 |
| `displayName` | string | 是 | 资产展示名。 |
| `status` | string | 是 | `OpsAssetStatus`。 |
| `ownerModule` | string 或 null | 是 | 关联来源模块，例如 `RESOURCE`、`SERVER_STATUS`。 |
| `sourceRef` | object 或 null | 是 | 来源快照引用。 |
| `runtimeStatus` | string 或 null | 是 | 运行态摘要。 |
| `publicVisible` | boolean | 是 | 是否允许展示给后台非 OWNER 人员。 |
| `riskTags` | string[] | 是 | 风险标签。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### OpsMetricSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `snapshotId` | string | 是 | 指标快照 ID。 |
| `nodeId` | string | 是 | 节点 ID。 |
| `cpuUsagePercent` | number | 是 | CPU 使用率。 |
| `memoryUsagePercent` | number | 是 | 内存使用率。 |
| `diskUsagePercent` | number | 是 | 磁盘使用率。 |
| `networkRxBytes` | integer | 是 | 网络接收字节。 |
| `networkTxBytes` | integer | 是 | 网络发送字节。 |
| `loadAverage` | number[] | 是 | 系统负载。 |
| `recentEvents` | object[] | 是 | 最近异常事件，必须脱敏。 |
| `collectedAt` | string | 是 | 采集时间。 |
| `degraded` | boolean | 是 | 是否为降级快照。 |

### OpsContainerSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `containerId` | string | 是 | 容器 ID。 |
| `nodeId` | string | 是 | 节点 ID。 |
| `name` | string | 是 | 容器名。 |
| `image` | string | 是 | 镜像。 |
| `runtime` | string | 是 | `DOCKER`、`CONTAINERD` 或 `LXC`。 |
| `status` | string | 是 | `OpsRuntimeStatus`。 |
| `ports` | object[] | 是 | 端口摘要。 |
| `mounts` | object[] | 是 | 卷挂载脱敏摘要，不返回宿主绝对路径。 |
| `resourceUsage` | object | 是 | CPU、内存、网络摘要。 |
| `lastSyncedAt` | string | 是 | 最近同步时间。 |

### OpsVmSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `vmId` | string | 是 | 虚拟机 ID。 |
| `nodeId` | string | 是 | 节点 ID。 |
| `name` | string | 是 | 虚拟机名称。 |
| `platform` | string | 是 | `PROXMOX`、`LXD`、`CLOUD` 或 `SIMULATED`。 |
| `status` | string | 是 | `OpsRuntimeStatus`。 |
| `cpuCores` | integer | 是 | CPU 核数。 |
| `memoryMb` | integer | 是 | 内存 MB。 |
| `diskGb` | integer | 是 | 磁盘 GB。 |
| `networkSummary` | object | 是 | 网络摘要。 |
| `lastSyncedAt` | string | 是 | 最近同步时间。 |

### OpsMinecraftInstanceSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `instanceId` | string | 是 | 实例 ID。 |
| `nodeId` | string | 是 | 节点 ID。 |
| `publicInstanceId` | string 或 null | 是 | `server-status` 公开实例快照 ID。 |
| `name` | string | 是 | 实例名称。 |
| `version` | string | 是 | Minecraft 版本。 |
| `status` | string | 是 | `OpsRuntimeStatus`。 |
| `onlinePlayers` | integer | 是 | 在线人数摘要。 |
| `directoryAlias` | string | 是 | 授权目录别名，不返回真实绝对路径。 |
| `startCommandSummary` | string | 是 | 启动命令脱敏摘要。 |
| `lastSyncedAt` | string | 是 | 最近同步时间。 |

### OpsFileEntry

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `nodeId` | string | 是 | 节点 ID。 |
| `rootAlias` | string | 是 | 授权根目录别名。 |
| `path` | string | 是 | 根目录内相对路径，以 `/` 开头。 |
| `name` | string | 是 | 文件名。 |
| `type` | string | 是 | `FILE` 或 `DIRECTORY`。 |
| `sizeBytes` | integer 或 null | 是 | 文件大小。 |
| `editableText` | boolean | 是 | 是否可作为文本读取。 |
| `modifiedAt` | string 或 null | 是 | 修改时间。 |

### OpsTask

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `taskId` | string | 是 | 任务 ID。 |
| `taskType` | string | 是 | `OpsTaskType`。 |
| `status` | string | 是 | `OpsTaskStatus`。 |
| `riskLevel` | string | 是 | `OpsRiskLevel`。 |
| `nodeId` | string | 是 | 目标节点。 |
| `targetType` | string | 是 | 目标类型。 |
| `targetId` | string | 是 | 目标 ID。 |
| `reason` | string | 是 | 操作原因。 |
| `paramsSummary` | object | 是 | 参数摘要，必须脱敏。 |
| `approvalId` | string 或 null | 是 | 高风险审批 ID。 |
| `idempotencyKey` | string 或 null | 是 | 幂等键。 |
| `nodeRequestId` | string 或 null | 是 | 派发到节点的请求编号。 |
| `resultSummary` | object 或 null | 是 | 节点结果摘要。 |
| `failureReason` | string 或 null | 是 | 失败原因脱敏摘要。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `expiresAt` | string | 是 | 任务超时时间。 |

### OpsApproval

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `approvalId` | string | 是 | 审批 ID。 |
| `taskId` | string | 是 | 关联任务。 |
| `status` | string | 是 | `OpsApprovalStatus`。 |
| `riskLevel` | string | 是 | `HIGH` 或 `CRITICAL`。 |
| `requestedBy` | string | 是 | 申请人。 |
| `approvedBy` | string 或 null | 是 | 审批人。 |
| `reviewComment` | string 或 null | 是 | 审批意见。 |
| `createdAt` | string | 是 | 创建时间。 |
| `reviewedAt` | string 或 null | 是 | 审批时间。 |
| `expiresAt` | string | 是 | 过期时间。 |

### OpsAuditLog

审计字段继承公共契约，允许补充 `nodeId`、`assetId`、`taskId`、`approvalId`、`nodeRequestId`、`stateFrom`、`stateTo`、`dependencyStatus` 和 `idempotencyKey`。审计列表不得提供删除接口。审计响应不得返回节点 token 原文、完整请求头、内部路径、真实命令、文件内容、异常堆栈、私钥、Cloudreve 管理凭据或服务器密码。

### OpsSummary

自检摘要至少包含 `service`、`port`、`storageMode`、`authMode`、`adminAdapterMode`、`nodeAdapterMode`、`nodeDaemonConnected`、`testControlsEnabled`、`nodesTotal`、`assetsTotal`、`tasksTotal`、`pendingApprovalsTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastHeartbeatAt`、`lastAuditAt` 和 `productionGaps`。

## ops-control 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `49200` | 502 | auth 认证上下文不可用。 |
| `49201` | 504 | auth 认证上下文调用超时。 |
| `49202` | 502 | auth 认证上下文不兼容。 |
| `49210` | 502 | admin 聚合适配不可用。 |
| `49220` | 502 | server-status 实例快照不可用。 |
| `49230` | 502 | resource 资产快照不可用。 |
| `49260` | 502 | node-daemon 未连接或节点离线。 |
| `49261` | 504 | 节点调用超时。 |
| `49262` | 502 | 节点响应字段不兼容。 |
| `49400` | 404 | 节点、资产、快照、文件、任务、审批或审计不存在。 |
| `49401` | 404 | 节点不存在或当前用户不可见。 |
| `49402` | 404 | 任务不存在或当前用户不可见。 |
| `49403` | 404 | 审批不存在或当前用户不可见。 |
| `49410` | 409 | 节点、资产、任务或审批状态不允许当前操作。 |
| `49411` | 409 | 节点名称、资产标识或任务目标冲突。 |
| `49412` | 409 | 幂等键请求指纹冲突。 |
| `49413` | 409 | 高风险二次确认文本不匹配。 |
| `49414` | 409 | 文件路径越过授权根目录。 |
| `49415` | 409 | 节点离线，操作只能读取最后快照。 |
| `49416` | 409 | 审批人不能审批自己的严重风险任务。 |
| `55000` | 500 | ops-control 内部错误。 |
| `55001` | 500 | ops-control 审计写入失败。 |
| `55002` | 500 | ops-control 状态写入失败。 |
| `55003` | 500 | ops-control 任务写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、能力点不足、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。ops-control 自有幂等指纹冲突使用 `49412`。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 运维总览 | GET | `/api/v1/ops-control/overview` | 是 | `NODE_READ` | LOW |
| 资产列表 | GET | `/api/v1/ops-control/assets` | 是 | `NODE_READ` | LOW |
| 资产详情 | GET | `/api/v1/ops-control/assets/{assetId}` | 是 | `NODE_READ` | LOW |
| 节点列表 | GET | `/api/v1/ops-control/nodes` | 是 | `NODE_READ` | LOW |
| 节点详情 | GET | `/api/v1/ops-control/nodes/{nodeId}` | 是 | `NODE_READ` | LOW |
| 注册节点 | POST | `/api/v1/ops-control/nodes` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 禁用节点 | PATCH | `/api/v1/ops-control/nodes/{nodeId}/disable` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | HIGH |
| 启用节点 | PATCH | `/api/v1/ops-control/nodes/{nodeId}/enable` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 节点心跳摘要 | POST | `/api/v1/ops-control/nodes/{nodeId}/heartbeat` | 是 | 节点认证或 `NODE_WRITE` | MEDIUM |
| 节点能力 | GET | `/api/v1/ops-control/nodes/{nodeId}/capabilities` | 是 | `NODE_READ` | LOW |
| 节点指标 | GET | `/api/v1/ops-control/nodes/{nodeId}/metrics/latest` | 是 | `NODE_READ` | LOW |
| 容器列表 | GET | `/api/v1/ops-control/nodes/{nodeId}/containers` | 是 | `NODE_READ` | LOW |
| 容器详情 | GET | `/api/v1/ops-control/nodes/{nodeId}/containers/{containerId}` | 是 | `NODE_READ` | LOW |
| 虚拟机列表 | GET | `/api/v1/ops-control/nodes/{nodeId}/vms` | 是 | `NODE_READ` | LOW |
| 虚拟机详情 | GET | `/api/v1/ops-control/nodes/{nodeId}/vms/{vmId}` | 是 | `NODE_READ` | LOW |
| Minecraft 实例列表 | GET | `/api/v1/ops-control/nodes/{nodeId}/minecraft-instances` | 是 | `NODE_READ` | LOW |
| Minecraft 实例详情 | GET | `/api/v1/ops-control/nodes/{nodeId}/minecraft-instances/{instanceId}` | 是 | `NODE_READ` | LOW |
| 授权目录文件列表 | GET | `/api/v1/ops-control/nodes/{nodeId}/files` | 是 | `FILE_MANAGE` | LOW |
| 文本文件读取请求 | POST | `/api/v1/ops-control/nodes/{nodeId}/files/read` | 是 | `FILE_MANAGE` | MEDIUM |
| 日志摘要请求 | POST | `/api/v1/ops-control/nodes/{nodeId}/logs/query` | 是 | `NODE_READ` | MEDIUM |
| 创建受控任务 | POST | `/api/v1/ops-control/tasks` | 是 | 按任务类型校验能力点 | MEDIUM 到 CRITICAL |
| 任务列表 | GET | `/api/v1/ops-control/tasks` | 是 | `NODE_READ` | LOW |
| 任务详情 | GET | `/api/v1/ops-control/tasks/{taskId}` | 是 | `NODE_READ` | LOW |
| 取消任务 | PATCH | `/api/v1/ops-control/tasks/{taskId}/cancel` | 是 | 创建者、`ADMIN` 或 `OWNER` | MEDIUM |
| 节点任务回写 | POST | `/api/v1/ops-control/tasks/{taskId}/node-result` | 是 | 节点认证或 `NODE_WRITE` | MEDIUM |
| 审批列表 | GET | `/api/v1/ops-control/approvals` | 是 | `HIGH_RISK_APPROVE` 或 `OWNER` | LOW |
| 审批详情 | GET | `/api/v1/ops-control/approvals/{approvalId}` | 是 | `HIGH_RISK_APPROVE` 或 `OWNER` | LOW |
| 审批通过 | PATCH | `/api/v1/ops-control/approvals/{approvalId}/approve` | 是 | `HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 审批拒绝 | PATCH | `/api/v1/ops-control/approvals/{approvalId}/reject` | 是 | `HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 审计列表 | GET | `/api/v1/ops-control/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 自检摘要 | GET | `/api/v1/ops-control/ops/summary` | 是 | `NODE_READ` | LOW |

## 读取接口

`GET /api/v1/ops-control/overview` 返回节点数量、在线节点、离线节点、资产数量、待审批数量、运行中任务、最近异常事件、降级模块和最近审计摘要。节点、资产和任务都只返回当前用户有能力查看的范围。节点离线时必须标记 `degraded=true`，不能伪造实时状态。

`GET /api/v1/ops-control/assets` 支持 `page`、`pageSize`、`keyword`、`nodeId`、`assetType`、`status`、`ownerModule`、`riskTag` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`、`assetType_asc`。成功响应分页 `items` 为 `OpsAsset[]`。

`GET /api/v1/ops-control/assets/{assetId}` 返回资产详情和关联快照。资产不存在返回 `49400`。响应不得返回真实内部路径、服务器密码、Cloudreve 管理 token、节点密钥或完整命令。

`GET /api/v1/ops-control/nodes` 支持 `page`、`pageSize`、`keyword`、`status`、`capability` 和 `sort`。成功响应分页 `items` 为 `OpsNode[]`。`HELPER` 仅可读节点和资产摘要，不能看到 token 摘要以外的敏感字段。

`GET /api/v1/ops-control/nodes/{nodeId}` 返回节点详情、最近心跳、能力、指标摘要和降级状态。节点不存在返回 `49401`。

`GET /api/v1/ops-control/nodes/{nodeId}/capabilities` 返回节点上报能力、控制面允许能力和当前用户可用能力的交集。

`GET /api/v1/ops-control/nodes/{nodeId}/metrics/latest` 返回最近 `OpsMetricSnapshot`。没有快照时返回包含 `nodeId` 和 `degraded=true` 的降级摘要；节点不存在返回 `49401`。

`GET /api/v1/ops-control/nodes/{nodeId}/containers`、`GET /vms`、`GET /minecraft-instances` 均返回最后快照分页。节点离线时仍可读最后快照，但必须标记 `stale=true`。详情不存在返回 `49400`。

`GET /api/v1/ops-control/nodes/{nodeId}/files` 查询参数包括 `rootAlias`、`path`。`path` 必须是 `/` 开头的授权根目录内相对路径，不允许 `..`、反斜杠、控制字符或编码绕过。路径越界返回 `49414`。目录匹配必须按路径段边界判断，不能让 `/foo` 命中 `/foobar`。P1 只返回模拟授权目录快照。

## 节点管理接口

`POST /api/v1/ops-control/nodes` 请求字段包括 `displayName`、`endpointSummary`、`capabilities`、`labels`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，创建状态为 `PENDING_REGISTRATION` 或 `ONLINE` 的节点，并返回一次性 `registrationToken` 的脱敏摘要。P1 不返回真实密钥明文。节点名称冲突返回 `49411`。

`PATCH /api/v1/ops-control/nodes/{nodeId}/disable` 请求字段包括 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `DISABLE_OPS_NODE`。成功后节点状态为 `DISABLED`，后续实时任务返回 `49415` 或 `49260`。重复禁用保持幂等。

`PATCH /api/v1/ops-control/nodes/{nodeId}/enable` 请求字段包括 `reason` 和 `idempotencyKey`。`DISABLED` 可回到 `OFFLINE`，等待下一次心跳确认在线。`REVOKED` 不允许启用。

`POST /api/v1/ops-control/nodes/{nodeId}/heartbeat` 请求字段包括 `status`、`version`、`capabilities`、`metrics`、`containers`、`vms`、`minecraftInstances`、`files` 和 `nodeRequestId`。`status` 必须属于 `OpsNodeStatus`，不允许浏览器或节点写入服务端可信字段。P1 允许 `NODE_WRITE` 用户模拟节点回写。心跳成功更新最后快照和审计摘要，不执行真实系统操作。

## 文件和日志请求

`POST /api/v1/ops-control/nodes/{nodeId}/files/read` 请求字段包括 `rootAlias`、`path`、`reason` 和 `idempotencyKey`。只允许读取 `editableText=true` 的文本文件快照，路径必须通过根目录保护。P1 返回模拟文本摘要，不返回真实文件内容。节点离线时返回最后快照或 `49415`，不能创建真实读取。

`POST /api/v1/ops-control/nodes/{nodeId}/logs/query` 请求字段包括 `targetType`、`targetId`、`tailLines`、`keyword`、`reason` 和 `idempotencyKey`。`tailLines` 范围为 `1` 到 `1000`。P1 返回日志摘要任务或模拟日志片段，不提供 WebSocket 流。

## 任务接口

`POST /api/v1/ops-control/tasks` 请求字段包括 `taskType`、`nodeId`、`targetType`、`targetId`、`params`、`reason`、`confirmText`、`approvalId` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `OpsTask`。请求不得携带 `actorUserId`、`actorRole`、`actorPermissions`、`taskStatus`、`approvalStatus`、`auditResult`、`createdBy`、`updatedBy`、`beforeState` 或 `afterState` 等服务端可信字段，出现时返回 `40001`。

任务能力规则：节点注册、启用、禁用和 token 轮换要求 `NODE_WRITE`。容器启动、停止、重启要求 `CONTAINER_OPERATE`。容器删除为 `CRITICAL`，必须审批。虚拟机动作要求 `VM_OPERATE`。Minecraft 实例启停要求 `CONTAINER_OPERATE` 或后续专用能力；`MC_COMMAND` 和 `TERMINAL_COMMAND` 要求 `TERMINAL_ACCESS` 且为 `CRITICAL`。文件读写、重命名、移动和删除要求 `FILE_MANAGE`，删除为 `HIGH`。备份恢复为 `CRITICAL`。

风险规则：`LOW` 和 `MEDIUM` 任务不需要审批。`HIGH` 任务必须有二次确认，`confirmText` 根据任务类型固定，例如 `DELETE_FILE`、`STOP_INSTANCE`。`CRITICAL` 任务可以先创建为 `PENDING_APPROVAL` 等待审批；真正派发或执行前必须有有效审批，或由 `OWNER` 在控制面直接授权。审批人不能审批自己的 `CRITICAL` 任务。

节点规则：节点不存在返回 `49401`。节点 `OFFLINE`、`DISABLED` 或 `REVOKED` 时，需要实时执行的任务返回 `49415` 或创建为 `FAILED`，不能进入 `SUCCEEDED`。容器、虚拟机、Minecraft 实例和文件任务必须校验目标快照存在，目标不存在返回 `49400`。P1 `nodeAdapterMode=SIMULATED` 下只允许白名单安全任务进入 `SUCCEEDED`，高风险真实执行任务进入 `PENDING_APPROVAL` 或 `FAILED`。

幂等规则：同一操作者、同一接口语义、同一 `idempotencyKey`、同一请求体重复提交返回同一任务。相同键不同体返回 `49412`。请求体指纹必须使用结构化 JSON 规范化，嵌套对象按字段名递归排序。

`GET /api/v1/ops-control/tasks` 支持 `page`、`pageSize`、`nodeId`、`taskType`、`status`、`riskLevel`、`createdBy`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`riskLevel_desc`。成功响应分页 `items` 为 `OpsTask[]`。

`GET /api/v1/ops-control/tasks/{taskId}` 返回任务详情和审批摘要。不存在返回 `49402`。

`PATCH /api/v1/ops-control/tasks/{taskId}/cancel` 请求字段包括 `reason` 和 `idempotencyKey`。只有 `PENDING_APPROVAL`、`QUEUED`、`DISPATCHED` 可以取消。`RUNNING` 任务 P1 不支持强制取消，返回 `49410`。终态任务重复取消返回状态冲突。

`POST /api/v1/ops-control/tasks/{taskId}/node-result` 请求字段包括 `nodeRequestId`、`status`、`resultSummary`、`failureReason` 和 `finishedAt`。`status` 只允许 `SUCCEEDED`、`FAILED` 或 `TIMEOUT`。P1 允许 `NODE_WRITE` 用户模拟节点结果回写。只允许 `DISPATCHED` 或 `RUNNING` 任务回写。回写必须审计并脱敏，审计写入失败时任务状态不得变化。

## 审批接口

`GET /api/v1/ops-control/approvals` 支持 `page`、`pageSize`、`status`、`riskLevel`、`requestedBy`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`reviewedAt_desc`、`riskLevel_desc`。只有 `OWNER` 或具备 `HIGH_RISK_APPROVE` 的用户可访问。

`PATCH /api/v1/ops-control/approvals/{approvalId}/approve` 请求字段包括 `reviewComment`、`reason` 和 `idempotencyKey`。审批通过后，关联任务从 `PENDING_APPROVAL` 进入 `QUEUED` 或在 P1 模拟模式下进入 `DISPATCHED`、`SUCCEEDED`、`FAILED`。审批不存在返回 `49403`。审批不是 `PENDING` 返回 `49410`。审批自己的 `CRITICAL` 任务返回 `49416`。审计写入失败时审批和任务状态必须保持不变。

`PATCH /api/v1/ops-control/approvals/{approvalId}/reject` 请求字段同审批通过。拒绝后任务进入 `FAILED`，失败原因为 `APPROVAL_REJECTED`。拒绝必须写审计，审计写入失败时审批和任务状态必须保持不变。

## 审计和自检接口

`GET /api/v1/ops-control/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`nodeId`、`taskId`、`result`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`riskLevel_desc`。只有 `ADMIN` 和 `OWNER` 可访问。审计列表是只读接口，不提供删除、修改或恢复。审计中的 `requestId` 必须来自当前 HTTP 请求或节点回写请求，不能使用固定占位值。

`GET /api/v1/ops-control/ops/summary` 成功响应 HTTP `200`，`data` 为 `OpsSummary`。P1 必须返回 `storageMode=IN_MEMORY`、`authMode=TEST_STUB`、`adminAdapterMode=TEST_STUB`、`nodeAdapterMode=SIMULATED`、`nodeDaemonConnected=false`、`testControlsEnabled=false` 和生产化缺口。摘要不得返回 token、密码、请求头、内部路径、真实命令、文件内容、异常堆栈或节点密钥。

## 状态、幂等和并发

节点状态由注册、启用禁用、心跳和 token 状态共同决定。`PENDING_REGISTRATION` 收到有效心跳后进入 `ONLINE`。心跳缺失可进入 `OFFLINE`。异常能力缺失可进入 `DEGRADED`。`DISABLED` 只能由启用回到 `OFFLINE`。`REVOKED` 为终态，P1 不提供恢复接口。

资产状态流转为 `ACTIVE`、`MAINTENANCE`、`DISABLED` 和 `ARCHIVED`。P1 主要通过节点心跳和模拟种子维护快照，不提供物理删除接口。

任务状态流转为 `PENDING_APPROVAL` 到 `QUEUED`、`FAILED` 或 `CANCELED`；`QUEUED` 到 `DISPATCHED`、`CANCELED` 或 `FAILED`；`DISPATCHED` 到 `RUNNING`、`SUCCEEDED`、`FAILED` 或 `TIMEOUT`；`RUNNING` 到 `SUCCEEDED`、`FAILED` 或 `TIMEOUT`。`SUCCEEDED`、`FAILED`、`CANCELED` 和 `TIMEOUT` 为终态。

审批状态流转为 `PENDING` 到 `APPROVED`、`REJECTED`、`EXPIRED` 或 `CANCELED`。审批通过和拒绝必须与任务状态变化在同一个临界区内完成，不能出现审批通过但任务仍待审批的半状态。

所有写接口使用本服务内串行临界区保护状态推进、幂等记录、审计和响应快照。后续数据库实现必须迁移为事务、唯一约束、条件更新或等效机制，不能降低并发口径。

## 安全、降级和脱敏

路径安全必须拒绝 `..`、反斜杠、控制字符、空路径、非 `/` 开头路径和编码绕过。P1 只在授权根目录别名内返回模拟快照，不访问真实文件系统。

敏感字段不得出现在任何响应中，包括节点 token 原文、私钥、服务器密码、完整 Authorization 请求头、Cloudreve 管理 token、真实宿主路径、真实终端命令、完整文件内容、异常堆栈和数据库连接串。

节点离线时，只允许返回最后快照或创建失败任务。外部依赖不可用时，读取类接口可以局部降级并标记 `degraded=true`，写入类接口不得假装成功。

审计写入失败时，节点注册、节点状态变更、任务创建、任务取消、节点回写、审批通过和审批拒绝不得假装成功，必须返回 `55001` 并保持业务数据不变。

## 验收口径

`ops-control` API 文档按 `docs/contracts-ops-control.md` 独立存在，并由 `.local-docs/tests-ops-control.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`ops-control` 完成时必须满足以下条件：全部接口按本文档实现；端口固定为 `8116`；所有接口要求登录；读取接口校验 `NODE_READ` 或对应能力；操作接口按任务类型校验能力点；高风险操作要求二次确认；严重风险操作要求审批或 `OWNER` 授权；节点 token、内部路径、命令参数、异常堆栈和凭据脱敏；节点离线时不假装成功；路径穿越被拦截；任务幂等和并发边界可复现；审计失败能回滚状态；自检摘要暴露存储模式、节点适配模式、测试控制状态和生产化缺口；`.local-docs/tests-ops-control.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 ops-control 全部测试通过；前序 15 个稳定服务回归测试通过；没有修改前序服务稳定接口；没有把真实服务器操作、Docker、Proxmox、MCSManager、文件删除、终端命令、备份恢复、Cloudreve 管理 token 或 `node-daemon` 执行能力塞进控制面。
