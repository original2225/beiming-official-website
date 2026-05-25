# 北冥官网 node-daemon API 契约

版本：0.1

## 文档定位

本文档是 `node-daemon` 节点守护进程的正式 API 契约。`node-daemon` 部署在被管理服务器上，只负责节点本地健康检查、能力上报、运行时快照、受控任务接收、受控任务执行摘要、授权目录只读文件视图、文本文件摘要、日志摘要、本地审计摘要和向 `ops-control` 回写心跳或任务结果。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、请求编号、时间格式、错误结构、审计字段、风险等级和脱敏要求均以公共契约为准。节点侧协议不是普通玩家 API，也不是后台浏览器 API。除健康检查外，所有接口都必须使用节点级认证，不能接受浏览器用户 token 直接执行宿主机操作。

本文档参考 Docker Engine API、Kubernetes API、Proxmox VE API、Portainer、Cockpit 和 MCSManager 的公开设计。Docker Engine API 将容器列表、日志、生命周期动作和 exec 能力拆开，说明节点不能提供万能执行接口。Kubernetes API 把资源对象、状态和 RBAC 分层，说明状态快照和动作授权必须分离。Proxmox VE API 的节点和异步任务思路适合本项目的任务追踪。Portainer 强调端点、环境和受控资源视图分离。Cockpit 通过受控 bridge 访问主机能力，提示节点侧通道必须受限。MCSManager 的 Web 面板和 Daemon 分层适合 Minecraft 实例、日志和控制台能力边界。本项目只借鉴这些设计，不接入它们的主数据，也不在第一版执行真实高风险宿主机修改。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Docker Engine API](https://docs.docker.com/reference/api/engine/) | 容器对象、日志、生命周期和 exec 分离。 |
| [Kubernetes Objects](https://kubernetes.io/docs/concepts/overview/working-with-objects/) 与 [RBAC](https://kubernetes.io/docs/reference/access-authn-authz/rbac/) | 对象状态、授权和动作边界分离。 |
| [Proxmox VE API Viewer](https://pve.proxmox.com/pve-docs/api-viewer/) | 节点、虚拟机和异步任务可追踪。 |
| [Portainer API 文档](https://docs.portainer.io/api/docs) | 控制面、环境端点和执行环境分离。 |
| [Cockpit Guide](https://cockpit-project.org/guide/latest/) | Web 管理、认证、bridge 和主机能力分离。 |
| [MCSManager 文档](https://docs.mcsmanager.com/) | 面板和 Daemon 分层，适合 Minecraft 实例托管。 |

## 职责边界

`node-daemon` 负责节点本地执行边界。它可以读取本机运行态摘要，接收 `ops-control` 已授权任务，执行第一版允许的模拟或只读任务，生成脱敏结果摘要，并把心跳和任务结果回写到 `ops-control` 的正式接口。

`node-daemon` 不负责用户登录、角色主数据、高风险审批、后台任务创建、玩家资源下载、玩家可见服务器状态、白名单审核、内容发布、活动日历、更新日志、Cloudreve 管理、运维审计主索引或控制面任务最终状态。上述能力都归前序服务或 `ops-control` 所有。

第一版固定为本地安全模拟节点。允许真实读取本进程配置中的安全摘要，不允许真实执行 shell、Docker CLI、Proxmox CLI、MCSManager 写接口、文件写入、文件删除、文件移动、备份恢复、虚拟机生命周期控制或终端命令。容器、虚拟机和 Minecraft 实例操作只返回 `SIMULATED` 摘要，或在能力不可用时返回明确错误。

## 基础路径、端口和调用模式

所有节点侧 HTTP 接口默认使用 `/api/v1/node-daemon` 前缀。第一版本地端口固定为 `8117`，自检摘要必须返回该端口。

调用模式固定为 `CONTROL_PLANE_PUSH`。`ops-control` 在未来真实联调时把已授权任务推送到节点的 `POST /api/v1/node-daemon/tasks`。`node-daemon` 定期或按测试触发向 `ops-control` 既有接口回写 `POST /api/v1/ops-control/nodes/{nodeId}/heartbeat` 和 `POST /api/v1/ops-control/tasks/{taskId}/node-result`。第一版不要求 `ops-control` 新增任务拉取接口，不破坏 `docs/contracts-ops-control.md` 已稳定路径。

健康检查 `GET /api/v1/node-daemon/health` 不要求认证，但只能返回存活、版本和请求编号，不返回节点 ID、token 摘要、内部路径、环境变量、能力明细或依赖地址。其他接口必须校验节点认证。

## 节点认证和可信字段

节点侧认证使用 `X-Node-Id`、`X-Node-Request-Id`、`X-Node-Timestamp`、`X-Node-Signature` 和 `Authorization: Bearer <node token>`。第一版本地测试可以使用测试 token 桩，但响应和日志不得返回 token 原文。生产化实现应使用 HMAC 或证书签名，签名内容至少覆盖方法、路径、规范化请求体、时间戳和节点请求编号。

节点请求编号 `X-Node-Request-Id` 必须全局可追踪。没有传入普通 `X-Request-Id` 时，服务端仍按公共契约生成 HTTP 请求编号，并把节点请求编号放入 `data.nodeRequestId` 或本地审计字段。

时间戳允许偏差默认 300 秒。超出偏差返回 `49602`。签名错误返回 `49601`。节点未注册或本地配置未绑定节点 ID 返回 `49603`。重复 `nodeRequestId` 且请求体一致时按幂等重放处理；同一 `nodeRequestId` 不同请求体返回 `49612`。

请求体不得传入并覆盖 `trusted`、`localRootPath`、`resolvedPath`、`tokenDigest`、`credential`、`beforeState`、`afterState`、`auditResult`、`createdBy`、`updatedBy`、`finishedAt` 等节点本地可信字段。出现可信字段时返回 `40001`。

## 本地测试控制头

`node-daemon` 允许在本地自动化测试中使用 `X-Test-Node-Auth`、`X-Test-Runtime-Mode`、`X-Test-Ops-Control-Mode`、`X-Test-Fail-Audit`、`X-Test-Now` 模拟认证失败、签名失败、运行时不可用、控制面不可用、控制面超时、审计失败和时间边界。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发认证失败、运行时失败、控制面失败、审计失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 纳入生产化硬化项。

## 与 ops-control 的兼容契约

`node-daemon` 只通过 `ops-control` 正式接口适配控制面。心跳回写必须兼容 `POST /api/v1/ops-control/nodes/{nodeId}/heartbeat` 的字段。任务结果回写必须兼容 `POST /api/v1/ops-control/tasks/{taskId}/node-result` 的字段。回写状态只能使用 `SUCCEEDED`、`FAILED` 或 `TIMEOUT`。

节点接收的任务对象必须兼容 `OpsTask` 的 `taskId`、`taskType`、`nodeId`、`targetType`、`targetId`、`paramsSummary`、`nodeRequestId`、`riskLevel`、`reason` 和 `expiresAt`。节点不得自造控制面任务状态，不得把本地 `RUNNING` 当成控制面终态。

节点生成的容器、虚拟机、Minecraft 实例和文件快照必须兼容 `ops-control` 心跳已接受的 `containers`、`vms`、`minecraftInstances` 和 `files` 列表。宿主机绝对路径、真实启动命令、真实凭据和完整日志不得出现在心跳中。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `NodeDaemonMode` | `SIMULATED`、`READ_ONLY`、`CONTROLLED_RUNTIME` | 第一版固定 `SIMULATED`，后续真实接入再扩展。 |
| `NodeDaemonStatus` | `STARTING`、`READY`、`DEGRADED`、`DRAINING`、`STOPPED` | 节点本地进程状态。 |
| `NodeRuntimeStatus` | `RUNNING`、`STOPPED`、`PAUSED`、`STARTING`、`STOPPING`、`FAILED`、`UNKNOWN` | 兼容 `OpsRuntimeStatus`。 |
| `NodeCapability` | `NODE_READ`、`NODE_WRITE`、`CONTAINER_OPERATE`、`VM_OPERATE`、`FILE_MANAGE`、`TERMINAL_ACCESS` | 第一版只允许 `NODE_READ`、`NODE_WRITE`、`FILE_MANAGE` 和模拟 `CONTAINER_OPERATE`。 |
| `NodeTaskType` | `CONTAINER_START`、`CONTAINER_STOP`、`CONTAINER_RESTART`、`MC_START`、`MC_STOP`、`MC_RESTART`、`FILE_READ`、`LOG_QUERY` | 第一版可接收的安全任务。其他 `OpsTaskType` 返回能力不支持。 |
| `NodeTaskStatus` | `RECEIVED`、`RUNNING`、`SUCCEEDED`、`FAILED`、`CANCELED`、`TIMEOUT` | 节点本地任务状态。 |
| `NodeAuditResult` | `SUCCESS`、`FAILED` | 本地审计写入结果。 |
| `NodeDependencyStatus` | `AVAILABLE`、`UNAVAILABLE`、`TIMEOUT`、`BAD_SCHEMA`、`DISABLED` | 控制面和本地运行时依赖摘要。 |

## 通用对象

### NodeDaemonSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `service` | string | 是 | 固定为 `node-daemon`。 |
| `port` | integer | 是 | 第一版固定 `8117`。 |
| `nodeId` | string | 是 | 节点 ID，只返回配置 ID，不返回 token。 |
| `mode` | string | 是 | `NodeDaemonMode`。 |
| `status` | string | 是 | `NodeDaemonStatus`。 |
| `version` | string | 是 | 节点版本。 |
| `opsControlEndpointSummary` | string | 是 | 控制面端点脱敏摘要。 |
| `testControlsEnabled` | boolean | 是 | 测试控制头是否启用。 |
| `runtimeAdapters` | object | 是 | docker、vm、minecraft、file、log 适配状态摘要。 |
| `productionGaps` | string[] | 是 | 第一版未接真实能力的缺口。 |

### NodeRuntimeSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `nodeId` | string | 是 | 节点 ID。 |
| `status` | string | 是 | `NodeDaemonStatus`。 |
| `version` | string | 是 | 节点版本。 |
| `capabilities` | string[] | 是 | 节点上报能力。 |
| `metrics` | object | 是 | CPU、内存、磁盘、网络、负载摘要。 |
| `containers` | object[] | 是 | 兼容 `OpsContainerSnapshot` 的脱敏摘要。 |
| `vms` | object[] | 是 | 兼容 `OpsVmSnapshot` 的脱敏摘要。 |
| `minecraftInstances` | object[] | 是 | 兼容 `OpsMinecraftInstanceSnapshot` 的脱敏摘要。 |
| `files` | object[] | 是 | 兼容 `OpsFileEntry` 的授权目录摘要。 |
| `recentEvents` | object[] | 是 | 最近异常事件，必须脱敏。 |
| `collectedAt` | string | 是 | 采集时间。 |
| `degraded` | boolean | 是 | 是否降级。 |

### NodeTask

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `taskId` | string | 是 | `ops-control` 任务 ID。 |
| `nodeRequestId` | string | 是 | 派发到节点的请求编号。 |
| `taskType` | string | 是 | `NodeTaskType`。 |
| `status` | string | 是 | `NodeTaskStatus`。 |
| `riskLevel` | string | 是 | `LOW`、`MEDIUM`、`HIGH` 或 `CRITICAL`。第一版 `HIGH` 和 `CRITICAL` 不执行真实动作。 |
| `nodeId` | string | 是 | 目标节点 ID，必须等于本节点 ID。 |
| `targetType` | string | 是 | `NODE`、`CONTAINER`、`MINECRAFT_INSTANCE`、`FILE` 或 `LOG_TARGET`。 |
| `targetId` | string | 是 | 目标 ID 或授权目录相对路径。 |
| `paramsSummary` | object | 是 | 脱敏参数摘要。 |
| `reason` | string | 是 | 控制面传入的操作原因。 |
| `receivedAt` | string | 是 | 接收时间。 |
| `startedAt` | string 或 null | 是 | 开始时间。 |
| `finishedAt` | string 或 null | 是 | 完成时间。 |
| `expiresAt` | string | 是 | 超时时间。 |
| `resultSummary` | object 或 null | 是 | 脱敏执行结果。 |
| `failureReason` | string 或 null | 是 | 脱敏失败原因。 |

### NodeFileEntry

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `rootAlias` | string | 是 | 授权根别名。 |
| `path` | string | 是 | 授权根内相对路径，以 `/` 开头。 |
| `name` | string | 是 | 文件名。 |
| `type` | string | 是 | `FILE` 或 `DIRECTORY`。 |
| `sizeBytes` | integer 或 null | 是 | 文件大小。 |
| `editableText` | boolean | 是 | 是否可读文本摘要。 |
| `modifiedAt` | string 或 null | 是 | 修改时间。 |

### NodeAuditLog

本地审计字段继承公共契约，允许补充 `nodeId`、`taskId`、`nodeRequestId`、`localAction`、`dependencyStatus`、`idempotencyKey` 和 `sanitizedParamsSummary`。本地审计只用于节点排障和回写佐证，不能替代 `ops-control` 审计主索引。

## node-daemon 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `49600` | 401 | 节点认证缺失。 |
| `49601` | 401 | 节点 token 或签名无效。 |
| `49602` | 401 | 节点请求时间戳过期或超前。 |
| `49603` | 403 | 节点未注册、节点 ID 不匹配或本地配置未绑定。 |
| `49604` | 403 | 节点能力不支持当前任务。 |
| `49605` | 409 | 授权目录路径越界。 |
| `49606` | 409 | 任务状态不允许当前操作。 |
| `49607` | 409 | 本地运行时不可用。 |
| `49608` | 404 | 本地任务、目标、授权根或文件不存在。 |
| `49609` | 409 | 文本文件不可读取或超过摘要限制。 |
| `49610` | 504 | 本地任务执行超时。 |
| `49611` | 502 | ops-control 回写不可用或响应不兼容。 |
| `49612` | 409 | 节点幂等请求指纹冲突。 |
| `55200` | 500 | node-daemon 内部错误。 |
| `55201` | 500 | 本地审计写入失败，任务状态不得推进。 |
| `55202` | 500 | 本地任务状态写入失败。 |

字段校验、分页、排序、通用认证格式和通用服务端错误优先使用公共错误码。节点协议认证、签名、路径、运行时、回写和本地状态错误使用本文档错误码。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/node-daemon/health` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/node-daemon/ops/summary` | 是 | 节点认证 | LOW |
| 能力列表 | GET | `/api/v1/node-daemon/capabilities` | 是 | 节点认证 | LOW |
| 注册握手摘要 | POST | `/api/v1/node-daemon/registration/handshake` | 是 | 节点认证或本地 bootstrap | MEDIUM |
| 运行时快照 | GET | `/api/v1/node-daemon/runtime/snapshot` | 是 | 节点认证 | LOW |
| 触发心跳回写 | POST | `/api/v1/node-daemon/runtime/heartbeat` | 是 | 节点认证 | MEDIUM |
| 接收受控任务 | POST | `/api/v1/node-daemon/tasks` | 是 | 节点认证，控制面签名 | MEDIUM 到 CRITICAL |
| 本地任务列表 | GET | `/api/v1/node-daemon/tasks` | 是 | 节点认证 | LOW |
| 本地任务详情 | GET | `/api/v1/node-daemon/tasks/{nodeRequestId}` | 是 | 节点认证 | LOW |
| 取消本地任务 | PATCH | `/api/v1/node-daemon/tasks/{nodeRequestId}/cancel` | 是 | 节点认证，控制面签名 | MEDIUM |
| 任务结果摘要 | GET | `/api/v1/node-daemon/tasks/{nodeRequestId}/result` | 是 | 节点认证 | LOW |
| 授权目录文件列表 | GET | `/api/v1/node-daemon/files` | 是 | `FILE_MANAGE` 节点能力 | LOW |
| 文本文件读取摘要 | POST | `/api/v1/node-daemon/files/read` | 是 | `FILE_MANAGE` 节点能力 | MEDIUM |
| 日志摘要查询 | POST | `/api/v1/node-daemon/logs/query` | 是 | `NODE_READ` 节点能力 | MEDIUM |
| 本地审计列表 | GET | `/api/v1/node-daemon/audit-logs` | 是 | 节点认证 | LOW |

## 健康和自检接口

`GET /api/v1/node-daemon/health` 返回 `status`、`version`、`service` 和 `requestId`。该接口不得返回节点 token、控制面地址、授权根、宿主机路径、环境变量或能力明细。进程存活但依赖不可用时仍可返回 HTTP `200`，并用 `status=DEGRADED` 标记。

`GET /api/v1/node-daemon/ops/summary` 返回 `NodeDaemonSummary`。第一版必须返回 `mode=SIMULATED`、`port=8117`、`testControlsEnabled=false`、`runtimeAdapters` 和 `productionGaps`。摘要不得返回 token、密码、请求头、内部路径、真实命令、完整日志、文件内容、异常堆栈或控制面凭据。

`GET /api/v1/node-daemon/capabilities` 返回节点本地支持能力、控制面允许能力和第一版实际可执行能力。第一版真实写能力不得开放，`TERMINAL_ACCESS` 只能返回 `planned=false` 或 `available=false`。

`POST /api/v1/node-daemon/registration/handshake` 请求字段包括 `controlPlaneNodeId`、`registrationNonce`、`daemonVersion`、`capabilities` 和 `idempotencyKey`。成功返回节点本地绑定摘要和签名能力，不返回 token 原文。节点 ID 与本地配置冲突返回 `49603`。同 key 同体重放返回同一握手摘要，同 key 不同体返回 `49612`。

## 运行时和心跳接口

`GET /api/v1/node-daemon/runtime/snapshot` 返回 `NodeRuntimeSnapshot`。第一版使用模拟适配器，不访问真实 Docker、Proxmox、MCSManager 或宿主机敏感目录。运行时不可用时返回带 `degraded=true` 的摘要，或在核心采集失败时返回 `49607`。

`POST /api/v1/node-daemon/runtime/heartbeat` 请求字段包括 `reason`、`idempotencyKey` 和可选 `dryRun`。`dryRun=true` 只生成兼容 `ops-control` 的心跳请求体摘要，不发起回写。`dryRun=false` 时节点尝试回写 `ops-control` 心跳接口；控制面不可用返回 `49611`，不得假装回写成功。心跳摘要必须脱敏。

## 任务接口

`POST /api/v1/node-daemon/tasks` 请求字段包括 `taskId`、`taskType`、`nodeId`、`targetType`、`targetId`、`paramsSummary`、`riskLevel`、`reason`、`nodeRequestId`、`expiresAt` 和 `idempotencyKey`。`nodeId` 必须等于本节点 ID。任务类型必须属于第一版 `NodeTaskType`。高风险或严重风险任务第一版不得执行真实动作，只能返回 `FAILED`、`PENDING_CONTROL_PLANE_APPROVAL_NOT_EXECUTABLE` 摘要或模拟结果。任务接收成功返回 HTTP `201` 和 `NodeTask`。

任务幂等按 `nodeRequestId` 和 `idempotencyKey` 双重保护。同一 `nodeRequestId` 同体重放返回同一任务；不同体返回 `49612`。任务过期返回 `49606`。本地审计写入失败返回 `55201`，任务不得进入 `RUNNING` 或终态。

第一版允许安全模拟的任务为 `CONTAINER_START`、`CONTAINER_STOP`、`CONTAINER_RESTART`、`MC_START`、`MC_STOP`、`MC_RESTART`、`FILE_READ` 和 `LOG_QUERY`。`FILE_WRITE`、`FILE_DELETE`、`FILE_MOVE`、`FILE_RENAME`、`TERMINAL_COMMAND`、`MC_COMMAND`、`CONTAINER_DELETE`、`VM_FORCE_STOP`、`BACKUP_RESTORE` 等高风险真实执行任务返回 `49604`。

`GET /api/v1/node-daemon/tasks` 支持 `page`、`pageSize`、`status`、`taskType`、`targetType`、`from`、`to` 和 `sort`。`sort` 允许 `receivedAt_desc`、`finishedAt_desc`。成功响应分页 `items` 为 `NodeTask[]`。

`GET /api/v1/node-daemon/tasks/{nodeRequestId}` 返回本地任务详情。不存在返回 `49608`。响应不得返回完整命令、原始参数、token、宿主机路径或异常堆栈。

`PATCH /api/v1/node-daemon/tasks/{nodeRequestId}/cancel` 请求字段包括 `reason`、`idempotencyKey` 和 `controlPlaneTaskId`。只有 `RECEIVED` 和未开始的 `RUNNING` 前阶段可以取消。已完成、已失败、已超时或已取消任务返回 `49606`。取消成功必须写本地审计，并尝试向 `ops-control` 回写 `FAILED` 或 `TIMEOUT` 之外的取消摘要；若控制面不可用，保留本地取消状态并返回降级摘要。

`GET /api/v1/node-daemon/tasks/{nodeRequestId}/result` 返回本地结果摘要和最近一次控制面回写状态。重复查询不得重新执行任务。

## 文件和日志接口

`GET /api/v1/node-daemon/files` 查询参数包括 `rootAlias` 和 `path`。`path` 必须以 `/` 开头，禁止 `..`、反斜杠、控制字符、URL 编码绕过、符号链接越界、大小写混淆越界和路径段前缀误判。路径越界返回 `49605`。第一版只返回授权目录模拟或只读快照，不返回真实绝对路径。

`POST /api/v1/node-daemon/files/read` 请求字段包括 `rootAlias`、`path`、`maxBytes`、`reason` 和 `idempotencyKey`。只允许 `editableText=true` 且大小不超过契约限制的文本摘要。响应字段为 `contentSummary`、`truncated`、`sizeBytes`、`hashSummary` 和 `redactionApplied`。响应不得返回 `.env`、`authorized_keys`、`id_rsa`、`server.properties` 敏感项、token、密码、RCON 密码或完整文件内容。

`POST /api/v1/node-daemon/logs/query` 请求字段包括 `targetType`、`targetId`、`tailLines`、`keyword`、`since`、`reason` 和 `idempotencyKey`。`tailLines` 范围为 `1` 到 `1000`。第一版只返回日志摘要和模拟片段，不提供 WebSocket 实时流，不返回完整命令行、完整环境变量、认证头或 token。

## 本地审计接口

`GET /api/v1/node-daemon/audit-logs` 支持 `page`、`pageSize`、`taskId`、`nodeRequestId`、`localAction`、`result`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`。审计列表只读，不提供删除、修改或恢复接口。审计响应必须脱敏，并且必须保留当前 HTTP `requestId` 或节点请求编号。

## 状态、幂等和并发

节点本地状态由进程启动、自检、运行时适配器、控制面回写和 drain 状态共同决定。`STARTING` 可进入 `READY` 或 `DEGRADED`。`READY` 在依赖不可用时进入 `DEGRADED`。节点停止接收新任务时进入 `DRAINING`。`STOPPED` 为进程退出前摘要状态。

本地任务状态为 `RECEIVED` 到 `RUNNING`、`CANCELED`、`FAILED` 或 `TIMEOUT`；`RUNNING` 到 `SUCCEEDED`、`FAILED`、`CANCELED` 或 `TIMEOUT`。`SUCCEEDED`、`FAILED`、`CANCELED` 和 `TIMEOUT` 为本地终态。控制面只接受 `SUCCEEDED`、`FAILED` 或 `TIMEOUT` 回写，取消任务需要以脱敏失败摘要回写。

所有写接口必须用本地串行临界区保护状态推进、幂等记录、审计和响应快照。后续落盘实现必须使用事务、文件锁、唯一约束、条件更新或等效机制，不能降低并发口径。

## 安全、降级和脱敏

节点必须限制授权根目录。所有文件路径先规范化，再校验仍在授权根下。禁止路径穿越、反斜杠绕过、控制字符、URL 编码绕过、符号链接越界和路径段前缀误判。

敏感字段不得出现在任何响应中，包括节点 token 原文、私钥、服务器密码、完整 Authorization 请求头、Cloudreve 管理 token、真实宿主路径、真实终端命令、完整文件内容、异常堆栈、数据库连接串、`.env`、`authorized_keys`、`id_rsa`、RCON 密码和 Minecraft 敏感配置。

控制面不可用时，读取类接口可以返回本地摘要并标记 `opsControlStatus=UNAVAILABLE`。任务接收、心跳回写和结果回写不能假装控制面成功。审计写入失败时，任务接收、任务取消、文件读取、日志查询和心跳触发不得推进状态，必须返回 `55201`。

## 验收口径

`node-daemon` API 文档必须按 `docs/contracts-node-daemon.md` 独立存在，并由 `.local-docs/tests-node-daemon.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、签名失败、节点未注册、能力不足、目标不存在、状态冲突、幂等、并发、超时、取消、运行时不可用、控制面不可用、路径越界、敏感字段脱敏、审计失败回滚、测试控制头默认关闭和模块验收口径。

`node-daemon` 完成时必须满足以下条件：端口固定为 `8117`；健康检查不泄露敏感信息；除健康检查外全部接口要求节点认证；请求编号和节点请求编号可追踪；签名、时间戳和幂等被验证；任务接收兼容 `ops-control` 的任务模型；心跳和任务结果回写兼容 `ops-control` 既有接口；路径守卫、日志摘要、文件摘要和敏感字段脱敏都有自动化验证；测试控制头默认关闭；不接收浏览器 token 直接执行请求；不执行未授权真实宿主机命令；不做真实删除、真实终端、真实备份恢复或真实虚拟机强制操作；自动化测试必须先红灯；实现后 `node-daemon` 全量测试通过；前序 16 个稳定服务回归通过；边界扫描无违规命中；测试过程记录完整。
