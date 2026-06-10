# 北冥官网前端 API 总文档

版本：0.2

## 文档定位

本文档是面向前端开发的一体化 API 查阅文档，由 `docs/contracts-common.md`、27 个业务或平台模块独立契约，以及 `business-core`、`admission-core`、`engagement-core`、`ops-core`、`portal-core` 和 `unified-backend` 六个运行入口或候选入口契约合并生成。它用于前端统一查路径、字段、权限、错误码、状态流转、失败降级和验收口径。

各模块独立契约仍是后端实现和变更的源文档。任一接口发生变化时，必须先更新对应 `docs/contracts-<module>.md`，再同步更新本文档。

当前合并范围包含 27 个业务或平台模块，另包含六个运行入口或候选入口自检契约。第三批四个业务模块在 `engagement-core-service:8132` 中承载 149 个业务路由，第四批六个后台运维控制面模块和第六期跨平台通知控制面在 `ops-core-service:8133` 中承载 219 个业务路由，第五批后三个玩家门户体验模块在 `portal-core-service:8134` 中承载 108 个业务路由。`ops-core` 自身当前提供 5 个运行单元自检、诊断和 HTTP smoke 路由，`portal-core` 自身当前提供 5 个运行单元自检、诊断和 HTTP smoke 路由，`unified-backend` 自身提供 5 个候选入口自检、挂载和 HTTP smoke 路由，并在 readiness 中暴露生产替换预演预检矩阵、后端单服务准备 evidence、最终单服务后端侧收束 evidence、入口切换执行 evidence、生产入口切换阻塞 evidence、`api-gateway` 退役门禁 evidence、五个 core 退役前置矩阵、生产化硬化前置矩阵和生产集中配置前置矩阵。第二十八轮生产集中配置前置准备下，候选入口继续记录从 `http://127.0.0.1:8125` 切到 `http://127.0.0.1:8135` 的后端侧演练证据，业务路径保持原样；仓库内没有真实前端、反向代理或部署入口配置可更新，集中配置 provider、生产 profile、敏感配置外置和持久化审计也未接入，所以 `productionEntrypointCutoverPrecheckStatus=BLOCKED_BY_MISSING_EXTERNAL_ENTRYPOINT_CONFIG`，`apiGatewayRetirementPrecheckStatus=BLOCKED_BY_TRAFFIC_NOT_SWITCHED`，`coreEntrypointRetirementPrecheckStatus=BLOCKED_BY_PROTECTED_ROLLBACK_ROLE`，`productionHardeningPrecheckStatus=BLOCKED_BY_EXTERNAL_PRODUCTION_PREREQUISITES`，`productionCentralConfigPrecheckStatus=BLOCKED_BY_PRODUCTION_CONFIG_PROVIDER_NOT_CONNECTED`，不代表前端、外部代理、生产流量、集中配置、持久化审计、`api-gateway` 或五个 core 已经完成生产切换或退役。

## 前端接入要点

所有普通接口默认返回统一响应结构，`code=0` 表示成功，业务数据放在 `data`。分页接口统一返回 `items`、`page`、`pageSize` 和 `total`。需要登录的接口统一携带 `Authorization: Bearer <token>`。后台和运维接口还必须按模块契约校验角色、能力点、风险等级、二次确认和审计字段。

前端不得把首页内容、服务器状态、资源数据、用户权限、审核状态或后台操作结果写死。接口失败时按对应模块契约做局部降级，不能把跨模块业务判断塞进前端状态。

## 合并模块

`auth`、`profile`、`notification`、`content`、`server-status`、`resource`、`admin`、`business-core`、`onboarding`、`exam`、`whitelist`、`attendance`、`admission-core`、`community`、`activity`、`calendar`、`changelog`、`engagement-core`、`ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`online-map`、`plugin-integration`、`cross-platform-notification`、`ops-image-market`、`ops-core`、`api-gateway`、`material`、`guide`、`portal-core`、`unified-backend`

## 正文

## 北冥官网 P0 公共契约

来源：`docs/contracts-common.md`

版本：0.1

### 文档定位

本文档定义 P0 阶段所有接口、模块和服务必须共同遵守的基础契约。后续模块接口契约不得绕开本文档另起一套响应格式、认证方式、分页格式、权限模型、审计字段、状态模型或时间格式。

本文档适用于官网公开页、用户中心、管理后台、运维控制台、后端入口、业务模块和后续节点守护进程的控制面交互。节点守护进程的系统级执行协议可以在 `ops-control` 模块契约中继续细化，但不得破坏本文档定义的认证、审计和风险控制边界。

### 基础原则

所有正式接口默认使用 JSON 作为请求和响应格式。文件上传、文件下载、日志流和 WebSocket 通道可以使用专门协议，但入口认证、权限校验、请求编号、错误结构和审计要求仍然遵守本文档。

接口路径必须先写入对应模块契约，再进入实现。未写入正式契约的路径、角色名、能力点、响应字段、数据库结构和服务依赖不得直接进入代码。

业务模块不能直接读取其他模块的数据表。跨模块读取必须通过明确接口。允许保存高频展示快照字段，快照不能成为主数据来源。

删除类业务默认采用软删除、下架或归档。真实删除必须满足权限、审计、备份和二次确认要求。高风险删除还需要进入审批或授权流程。

### API 路径

P0 阶段所有后端 HTTP 接口默认使用 `/api/v1` 作为版本前缀。

公开接口可以不带认证，但仍需要统一响应格式、错误码和请求编号。需要登录的接口必须校验 `Authorization: Bearer <token>`。后台接口必须额外校验基础角色和业务权限点。运维接口必须额外校验运维能力点、风险等级和二次确认状态。

模块路径按业务边界命名，例如 `/api/v1/auth`、`/api/v1/profile`、`/api/v1/content`、`/api/v1/admin`、`/api/v1/ops`。模块内部路径由对应模块契约继续定义。

### 请求头

客户端请求可以携带 `Authorization`、`Content-Type`、`Accept-Language` 和 `X-Request-Id`。没有传入 `X-Request-Id` 时，后端入口必须生成请求编号，并在响应头和响应体调试字段中保持一致。请求编号用于日志、审计、排障和跨模块追踪。

管理后台和运维控制台发起高风险操作时，还需要携带二次确认凭据或审批编号。具体字段由对应模块契约定义，但审计记录必须能追溯操作者、目标对象、操作原因、操作前状态、操作后状态和执行结果。

### 统一成功响应

普通成功响应使用以下结构。

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

`code` 为 `0` 表示业务成功。`message` 默认使用 `success`。`data` 根据接口返回对象、数组、分页对象或 `null`。

创建成功的接口使用 HTTP `201`。普通读取和修改成功使用 HTTP `200`。删除、归档、下架等无返回主体的成功操作可以使用 HTTP `200`，并返回 `data: null`。

### 统一错误响应

错误响应使用以下结构。

```json
{
  "code": 40001,
  "message": "invalid request",
  "data": null,
  "errors": [
    {
      "field": "username",
      "reason": "username is required"
    }
  ],
  "requestId": "req_202605211430000001"
}
```

`code` 使用业务错误码。`message` 给前端展示或调试使用。`errors` 用于字段级校验错误，没有字段错误时可以省略。`requestId` 必须和服务端日志中的请求编号一致。

### 错误码分段

错误码按分段维护。通用错误码由本文档定义，模块错误码由模块契约定义，但不能复用已经占用的语义。

| 范围 | 含义 |
| --- | --- |
| 0 | 成功 |
| 40000-40999 | 通用请求错误 |
| 41000-41999 | 认证与会话错误 |
| 42000-42999 | 权限与风险控制错误 |
| 43000-43999 | 资源不存在、状态冲突和幂等冲突 |
| 44000-44999 | 限流、频率和风控错误 |
| 45000-45999 | 上传、文件和资源分发错误 |
| 46000-46999 | 跨模块调用和外部依赖错误 |
| 50000-50999 | 通用服务端错误 |
| 51000-59999 | 模块内部服务端错误 |

P0 通用错误码如下。

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| 40000 | 400 | 请求格式错误 |
| 40001 | 400 | 字段校验失败 |
| 40002 | 400 | 分页参数错误 |
| 40003 | 400 | 排序参数错误 |
| 41000 | 401 | 未登录 |
| 41001 | 401 | 登录状态无效 |
| 41002 | 401 | 登录已过期 |
| 41003 | 401 | 令牌格式错误 |
| 42000 | 403 | 无权限访问 |
| 42001 | 403 | 角色权限不足 |
| 42002 | 403 | 能力点不足 |
| 42003 | 403 | 高风险操作未确认 |
| 42004 | 403 | 高风险操作未审批 |
| 43000 | 404 | 资源不存在 |
| 43001 | 409 | 资源状态冲突 |
| 43002 | 409 | 幂等键冲突 |
| 44000 | 429 | 请求过于频繁 |
| 46000 | 502 | 外部依赖不可用 |
| 46001 | 504 | 跨模块调用超时 |
| 50000 | 500 | 服务端内部错误 |

### 分页格式

分页请求统一使用 `page` 和 `pageSize`。`page` 从 `1` 开始。`pageSize` 默认 `20`，最大值由模块契约指定，未指定时最大 `100`。

分页响应统一放入 `data`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [],
    "page": 1,
    "pageSize": 20,
    "total": 0
  }
}
```

分页接口需要支持稳定排序。默认按 `createdAt` 倒序。允许前端指定排序时，模块契约必须列出可排序字段，不能把数据库字段直接暴露为任意排序参数。

### 时间、ID 和枚举

所有时间字段使用 ISO 8601 字符串，默认保存和传输 UTC 时间。前端根据用户所在地显示本地时间。字段命名统一使用 `createdAt`、`updatedAt`、`deletedAt`、`submittedAt`、`reviewedAt`、`expiresAt`、`lastLoginAt` 等驼峰格式。

业务 ID 对外使用字符串。实现可以选择 UUID、雪花 ID 或数据库生成 ID，但接口契约只承诺字符串 ID，不允许前端依赖 ID 的生成规则。

枚举值使用大写英文和下划线，例如 `OWNER`、`PENDING_REVIEW`、`ARCHIVED`。枚举含义必须写入正式契约。

### 基础角色

系统基础角色固定为 `OWNER`、`ADMIN`、`HELPER`、`USER`。

`USER` 是普通注册用户，不能访问后台。`HELPER` 可以处理被授权的审核、协助和运营事项。`ADMIN` 可以管理用户、内容、资源、活动、通知、后台配置和数据看板。`OWNER` 可以管理角色权限、管理员邀请码、系统配置、节点密钥和高风险授权。

基础角色只决定默认能力范围。运维控制能力必须使用细粒度能力点，不能因为拥有后台登录权限就默认允许执行节点、容器、文件、终端或实例操作。

### 运维能力点

P0 先保留运维能力点模型，不要求实现完整运维控制。能力点名称必须兼容后续 `ops-control`。

| 能力点 | 含义 |
| --- | --- |
| NODE_READ | 查看节点、资产和运行状态 |
| NODE_WRITE | 注册、禁用、编辑节点和资产配置 |
| CONTAINER_OPERATE | 操作容器生命周期 |
| VM_OPERATE | 操作虚拟机生命周期 |
| FILE_MANAGE | 管理授权目录内文件 |
| TERMINAL_ACCESS | 使用终端和命令执行能力 |
| HIGH_RISK_APPROVE | 审批高风险操作 |

### 通用状态模型

内容、素材、指南、帖子、工单、举报、白名单、活动和资源默认使用统一状态模型。

| 状态 | 含义 |
| --- | --- |
| DRAFT | 草稿 |
| PENDING_REVIEW | 待审核 |
| APPROVED | 已通过 |
| REJECTED | 已拒绝 |
| NEEDS_CHANGES | 需修改 |
| OFFLINE | 已下架 |
| ARCHIVED | 已归档 |
| DELETED | 已软删除 |

模块可以扩展状态，但必须在模块契约中说明状态来源、允许流转、触发通知和审计要求。

### 审计字段

所有后台关键操作和高风险操作必须写入审计记录。审计记录至少包含 `id`、`requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`sourceIp`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason` 和 `createdAt`。

普通用户的公开浏览行为不强制写审计记录。登录、退出、注册、密码重置、用户状态修改、角色修改、邀请码创建、邀请码禁用、审核、处罚、资源上下架、节点密钥修改和高风险操作必须审计。

审计日志不得由普通管理员直接删除。归档和备份策略由后续 admin 或 ops-control 契约细化。

### 风险等级

后台操作默认分为 `LOW`、`MEDIUM`、`HIGH` 和 `CRITICAL`。查看列表、查看详情属于低风险。修改配置、审核通过、禁用用户属于中风险。删除文件、删除容器、强制停止实例、执行终端命令、修改节点密钥属于高风险或严重风险。

`HIGH` 操作必须二次确认。`CRITICAL` 操作必须具备高风险审批记录或 `OWNER` 授权。模块契约必须列出接口风险等级。

### 降级规则

公开页面读取内容、资源、服务器状态或成员展示失败时，前端必须局部降级，不能整页空白。接口需要返回明确错误码，前端根据错误码展示暂不可用状态。

登录、权限、审核、积分、白名单、通知和高风险运维操作不能只靠前端降级判断。后端必须以服务端状态为准，失败时不得假装成功。

外部依赖不可用时，模块需要返回 `46000` 或模块内更具体错误码。跨模块调用超时时返回 `46001` 或模块内更具体错误码。

### 验收口径

进入任意模块实现前，必须确认该模块契约已经引用本文档，并明确接口路径、认证要求、权限点、请求字段、响应字段、错误码、分页规则、幂等规则、状态流转、失败降级和审计要求。

P0 基础工程完成时，统一响应、统一错误、分页、认证头解析、请求编号、基础角色、能力点模型和审计字段必须能在代码中找到对应实现或明确的测试覆盖。

## 北冥官网 auth API 契约

来源：`docs/contracts-auth.md`

版本：0.2

### 文档定位

本文档是 `auth` 微服务的正式 API 契约。后续 `profile`、`notification`、`content`、`admin`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`ops-control` 等服务只能通过本文档定义的接口或后端入口提供的认证上下文适配 `auth`，不能直接读取 `auth` 数据库，也不能自行实现登录、会话或权限判断。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、运维能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `auth` 模块自己的路径、字段、状态、权限、错误码、审计和验收口径。

### 职责边界

`auth` 负责账号、凭证、登录会话、当前用户、角色、能力点、邀请码、密码重置、账号级 Minecraft 绑定和认证上下文输出。

`auth` 不负责成员公开档案、入服进度、考试、白名单审核、考勤积分、内容审核、通知投递、后台待办聚合、服务器状态展示或真实运维操作。账号级 Minecraft 绑定只表示某个账号绑定了 Minecraft 身份，成员主数据仍归后续 `profile`。

### 数据归属

`auth` 拥有以下主数据：用户、登录凭证、会话、角色能力点、邀请码、邀请码使用记录、密码重置令牌、Minecraft 绑定、安全日志和审计日志。

邀请码原始码不得完整明文长期存储。创建邀请码成功时可以返回原始码一次，之后只能返回前缀、类型、状态、使用次数、使用上限和过期时间。

密码不得明文存储。实现必须使用成熟密码哈希算法，例如 bcrypt 或 Argon2。密码必须拒绝明显常见弱密码，不能只依赖字符组合规则。退出登录、用户禁用、用户封禁、密码重置、主动修改密码和角色降权后，相关会话必须能失效。

### 基础路径与认证

所有接口默认使用 `/api/v1/auth` 前缀。公开接口无需 `Authorization`，但仍需要统一响应、请求编号、字段校验和限流。登录后接口必须使用 `Authorization: Bearer <token>`。后台接口必须额外校验基础角色和能力点。

`auth` 返回的访问令牌可以是服务端会话令牌或签名令牌。无论采用哪种实现，服务端都必须能主动吊销会话。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `UserStatus` | `PENDING_PROFILE`、`ACTIVE`、`DISABLED`、`BANNED`、`DELETED` | 注册后默认 `PENDING_PROFILE`，完成账号基础资料后可进入 `ACTIVE`。`DELETED` 为软删除状态。 |
| `Role` | `OWNER`、`ADMIN`、`HELPER`、`USER` | 继承公共契约基础角色。 |
| `Permission` | `NODE_READ`、`NODE_WRITE`、`CONTAINER_OPERATE`、`VM_OPERATE`、`FILE_MANAGE`、`TERMINAL_ACCESS`、`HIGH_RISK_APPROVE` | 继承公共契约运维能力点，可继续用于后续 `ops-control`。 |
| `InvitationType` | `PLAYER`、`ADMIN` | 玩家邀请码默认绑定 `USER`。管理员邀请码可绑定 `ADMIN` 或 `HELPER`，只能由 `OWNER` 创建。 |
| `InvitationStatus` | `ACTIVE`、`DISABLED`、`EXPIRED`、`EXHAUSTED` | 根据禁用、过期时间和使用次数计算。 |
| `AuditResult` | `SUCCESS`、`FAILED` | 审计执行结果。 |

### 通用对象

#### UserSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 是 | 用户 ID。 |
| `username` | string | 是 | 登录用户名，大小写不敏感，展示时保留注册格式。 |
| `displayName` | string | 是 | 站内展示名。 |
| `roles` | string[] | 是 | 基础角色列表。 |
| `permissions` | string[] | 是 | 细粒度能力点列表。 |
| `status` | string | 是 | 用户状态。 |
| `minecraftBinding` | object 或 null | 是 | 账号级 Minecraft 绑定摘要。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `lastLoginAt` | string 或 null | 是 | 最近登录时间。 |

#### SessionPayload

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `accessToken` | string | 是 | 访问令牌。 |
| `tokenType` | string | 是 | 固定为 `Bearer`。 |
| `expiresAt` | string | 是 | 会话过期时间。 |
| `user` | UserSummary | 是 | 当前用户摘要。 |

#### SessionSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 是 | 会话 ID，不等同于访问令牌。 |
| `current` | boolean | 是 | 是否为当前请求使用的会话。 |
| `createdAt` | string | 是 | 会话创建时间。 |
| `lastSeenAt` | string | 是 | 最近一次通过认证访问的时间。 |
| `expiresAt` | string | 是 | 会话绝对过期时间。 |
| `revoked` | boolean | 是 | 会话是否已吊销。 |

#### MinecraftBinding

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `minecraftId` | string | 是 | Minecraft Java 版名称。 |
| `minecraftUuid` | string | 是 | Minecraft UUID，使用无连字符小写格式。 |
| `verifiedAt` | string | 是 | 绑定验证通过时间。 |
| `source` | string | 是 | 绑定来源，P0 固定为 `MANUAL_VERIFICATION`。 |

#### InvitationSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 是 | 邀请码 ID。 |
| `codePrefix` | string | 是 | 邀请码前缀，不返回完整原始码。 |
| `type` | string | 是 | 邀请码类型。 |
| `status` | string | 是 | 邀请码状态。 |
| `boundRoles` | string[] | 是 | 使用后授予的基础角色。 |
| `boundPermissions` | string[] | 是 | 使用后授予的能力点。 |
| `maxUses` | integer | 是 | 最大使用次数。 |
| `usedCount` | integer | 是 | 已使用次数。 |
| `expiresAt` | string 或 null | 是 | 过期时间，`null` 表示不过期。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `disabledAt` | string 或 null | 是 | 禁用时间。 |

### auth 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `41100` | 401 | 用户名或密码错误。 |
| `41101` | 401 | 用户已禁用，不能登录。 |
| `41102` | 401 | 用户已封禁，不能登录。 |
| `41103` | 401 | 会话已被主动吊销。 |
| `41104` | 401 | 密码重置令牌无效或过期。 |
| `41105` | 401 | 当前密码错误。 |
| `41106` | 401 | 会话不属于当前用户或已不可操作。 |
| `42100` | 403 | 不能修改 `OWNER`。 |
| `42101` | 403 | 不能移除或禁用唯一 `OWNER`。 |
| `42102` | 403 | 管理员邀请码只能由 `OWNER` 创建。 |
| `42103` | 403 | 不能授予自己不具备的能力点。 |
| `43100` | 404 | 用户不存在。 |
| `43101` | 404 | 邀请码不存在。 |
| `43102` | 404 | Minecraft 绑定不存在。 |
| `43103` | 404 | 密码重置请求不存在。 |
| `43110` | 409 | 用户名已存在。 |
| `43111` | 409 | 展示名已存在。 |
| `43112` | 409 | 邀请码已禁用。 |
| `43113` | 409 | 邀请码已过期。 |
| `43114` | 409 | 邀请码次数已用完。 |
| `43115` | 409 | Minecraft ID 或 UUID 已绑定。 |
| `43116` | 409 | 用户状态不允许当前操作。 |
| `43117` | 409 | 会话状态已变化。 |
| `44100` | 429 | 登录尝试过于频繁。 |
| `44101` | 429 | 注册尝试过于频繁。 |
| `44102` | 429 | 密码重置尝试过于频繁。 |
| `44103` | 429 | 邀请码校验过于频繁。 |
| `51100` | 500 | auth 内部错误。 |

字段校验、未登录、权限不足、分页错误、幂等键冲突和服务端通用错误优先使用公共错误码。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 注册 | POST | `/api/v1/auth/register` | 否 | 无 | LOW |
| 登录 | POST | `/api/v1/auth/login` | 否 | 无 | LOW |
| 退出登录 | POST | `/api/v1/auth/logout` | 是 | 当前用户 | LOW |
| 当前用户 | GET | `/api/v1/auth/me` | 是 | 当前用户 | LOW |
| 会话校验 | GET | `/api/v1/auth/session/verify` | 是 | 当前用户 | LOW |
| 当前用户会话列表 | GET | `/api/v1/auth/me/sessions` | 是 | 当前用户 | LOW |
| 吊销当前用户指定会话 | DELETE | `/api/v1/auth/me/sessions/{sessionId}` | 是 | 当前用户 | MEDIUM |
| 修改当前用户密码 | POST | `/api/v1/auth/me/password` | 是 | 当前用户 | MEDIUM |
| 申请密码重置 | POST | `/api/v1/auth/password-reset/request` | 否 | 无 | LOW |
| 确认密码重置 | POST | `/api/v1/auth/password-reset/confirm` | 否 | 无 | MEDIUM |
| 绑定 Minecraft 身份 | PUT | `/api/v1/auth/me/minecraft-binding` | 是 | 当前用户 | MEDIUM |
| 解绑 Minecraft 身份 | DELETE | `/api/v1/auth/me/minecraft-binding` | 是 | 当前用户 | MEDIUM |
| 用户列表 | GET | `/api/v1/auth/admin/users` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 用户详情 | GET | `/api/v1/auth/admin/users/{userId}` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 修改用户基础信息和状态 | PATCH | `/api/v1/auth/admin/users/{userId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改角色和能力点 | PUT | `/api/v1/auth/admin/users/{userId}/roles` | 是 | `OWNER` | MEDIUM |
| 邀请码列表 | GET | `/api/v1/auth/admin/invitations` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 创建邀请码 | POST | `/api/v1/auth/admin/invitations` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 禁用邀请码 | PATCH | `/api/v1/auth/admin/invitations/{invitationId}/disable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 邀请码使用记录 | GET | `/api/v1/auth/admin/invitations/{invitationId}/usage-records` | 是 | `ADMIN` 或 `OWNER` | LOW |

### 公开账号接口

#### 注册

`POST /api/v1/auth/register`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `invitationCode` | string | 是 | 8 到 64 位，只允许字母、数字、短横线和下划线。 |
| `username` | string | 是 | 3 到 32 位，只允许字母、数字、下划线，大小写不敏感唯一。 |
| `password` | string | 是 | 10 到 128 位，必须包含字母和数字，不得为常见弱密码。 |
| `displayName` | string | 是 | 2 到 24 位，站内唯一。 |
| `idempotencyKey` | string | 否 | 同一客户端重试注册时使用，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `SessionPayload`。

业务规则：注册必须在同一事务内完成邀请码校验、用户创建、角色能力点授予、邀请码使用记录写入、使用次数递增和会话创建。邀请码并发使用时必须只允许剩余次数内的请求成功。`PLAYER` 邀请码只能创建普通用户。`ADMIN` 邀请码不能通过普通管理员创建，使用后可授予 `ADMIN` 或 `HELPER`。`OWNER` 不能通过注册接口创建。

幂等规则：同一 `idempotencyKey`、同一邀请码、同一用户名和同一请求体重复提交时返回同一个注册结果。相同 `idempotencyKey` 搭配不同请求体返回 `43002`。

降级规则：注册失败不得创建半成品用户，不得消耗邀请码次数。登录凭证创建失败时整笔事务回滚。

审计要求：成功注册写入 `AUTH_REGISTER_SUCCESS`。邀请码异常、字段校验失败不强制审计。管理员邀请码使用成功必须额外写入 `AUTH_ADMIN_INVITATION_USED`。

#### 登录

`POST /api/v1/auth/login`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `username` | string | 是 | 用户名。 |
| `password` | string | 是 | 密码。 |
| `idempotencyKey` | string | 否 | 同一登录请求重试时使用，10 分钟内有效。 |

成功响应 HTTP `200`，`data` 为 `SessionPayload`。

业务规则：用户名不存在或密码错误统一返回 `41100`，不得暴露用户是否存在。`DISABLED` 返回 `41101`，`BANNED` 返回 `41102`，`DELETED` 返回 `43116`。登录成功更新 `lastLoginAt` 并创建新会话。

限流规则：同一 IP、同一用户名维度均需要登录限流。触发限流返回 `44100`。

审计要求：登录成功写入 `AUTH_LOGIN_SUCCESS`。连续失败触发限流或风控时写入 `AUTH_LOGIN_RISK_BLOCKED`。

#### 退出登录

`POST /api/v1/auth/logout`

请求体为空。成功响应 HTTP `200`，`data: null`。

业务规则：只吊销当前访问令牌对应会话。重复退出同一会话返回成功，保持幂等。

审计要求：首次成功吊销写入 `AUTH_LOGOUT_SUCCESS`。重复退出不重复写审计。

#### 当前用户

`GET /api/v1/auth/me`

成功响应 HTTP `200`，`data` 为 `UserSummary`。

业务规则：会话无效、过期、被吊销或用户状态变为 `DISABLED`、`BANNED`、`DELETED` 时不得返回用户摘要，分别返回公共认证错误码或 auth 状态错误码。

降级规则：这是认证基础接口，失败时不得伪造游客态成功响应。

#### 会话校验

`GET /api/v1/auth/session/verify`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "valid": true,
    "expiresAt": "2026-05-21T12:00:00Z",
    "user": {}
  }
}
```

业务规则：只要会话不可继续使用，就返回对应错误，不返回 `valid: false` 的成功响应。后端入口和后序服务可用该接口验证认证上下文。

#### 当前用户会话列表

`GET /api/v1/auth/me/sessions`

成功响应 HTTP `200`，`data.items` 为 `SessionSummary[]`。只返回当前用户自己的会话，不返回访问令牌明文。

业务规则：列表包含当前可见的未过期会话和已吊销会话摘要，必须标记 `current`。`lastSeenAt` 在每次成功认证请求时更新。

审计要求：该接口为低风险读取，不强制写审计。

#### 吊销当前用户指定会话

`DELETE /api/v1/auth/me/sessions/{sessionId}`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，吊销原因。 |

成功响应 HTTP `200`，`data: null`。

业务规则：只能吊销当前用户自己的会话。目标会话不存在、已过期、属于其他用户或已不可操作时返回 `41106` 或资源错误。允许吊销当前会话，成功后当前令牌立即失效。重复吊销同一会话保持幂等，不重复写审计。

审计要求：首次成功吊销写入 `AUTH_SESSION_REVOKED`，记录操作者、目标会话和原因。

#### 修改当前用户密码

`POST /api/v1/auth/me/password`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `currentPassword` | string | 是 | 当前密码。 |
| `newPassword` | string | 是 | 10 到 128 位，必须包含字母和数字，不得为常见弱密码。 |
| `reason` | string | 是 | 1 到 200 位，修改原因。 |

成功响应 HTTP `200`，`data: null`。

业务规则：当前密码错误返回 `41105`。新密码与当前密码相同返回 `43001`。修改成功后更新密码哈希，吊销该用户除当前会话以外的全部会话，避免其他设备继续使用旧登录态。当前会话保持可用，便于用户继续操作。

审计要求：成功写入 `AUTH_PASSWORD_CHANGED`。当前密码错误、弱密码或相同密码不得修改密码，也不得吊销任何会话。

### 密码重置接口

#### 申请密码重置

`POST /api/v1/auth/password-reset/request`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `username` | string | 是 | 用户名。 |

成功响应 HTTP `200`，`data` 固定为 `null`。

业务规则：无论用户名是否存在，都返回成功，避免枚举账号。存在且状态允许重置的用户创建一次性重置令牌。P0 可以只落库令牌和审计，不要求接入邮件或短信。

限流规则：同一 IP、同一用户名维度需要限流。触发限流返回 `44102`。

审计要求：存在用户时写入 `AUTH_PASSWORD_RESET_REQUESTED`。

#### 确认密码重置

`POST /api/v1/auth/password-reset/confirm`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `resetToken` | string | 是 | 一次性重置令牌。 |
| `newPassword` | string | 是 | 10 到 128 位，必须包含字母和数字，不得为常见弱密码。 |

成功响应 HTTP `200`，`data: null`。

业务规则：令牌不存在、过期或已使用返回 `41104`。成功后更新密码哈希，标记令牌已使用，并吊销该用户全部现有会话。新密码不能和当前密码相同，相同时返回 `43001`。

审计要求：成功写入 `AUTH_PASSWORD_RESET_CONFIRMED`，失败写入 `AUTH_PASSWORD_RESET_FAILED`。

### Minecraft 绑定接口

#### 绑定 Minecraft 身份

`PUT /api/v1/auth/me/minecraft-binding`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `minecraftId` | string | 是 | 3 到 16 位，符合 Minecraft Java 版名称规则。 |
| `minecraftUuid` | string | 是 | 32 位小写十六进制 UUID，不带连字符。 |
| `verificationCode` | string | 是 | 绑定验证凭据，P0 可由服务端校验预置或临时凭据。 |

成功响应 HTTP `200`，`data` 为 `MinecraftBinding`。

业务规则：同一 `minecraftId` 或 `minecraftUuid` 只能绑定一个未删除账号。用户已有绑定时，再次绑定同一个身份返回成功；绑定不同身份返回 `43116`，必须先解绑。绑定只更新账号级字段，不创建成员档案。

审计要求：首次绑定写入 `AUTH_MINECRAFT_BOUND`。重复绑定同一身份不重复写审计。

#### 解绑 Minecraft 身份

`DELETE /api/v1/auth/me/minecraft-binding`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，说明解绑原因。 |

成功响应 HTTP `200`，`data: null`。

业务规则：没有绑定返回 `43102`。解绑只清除账号级 Minecraft 绑定，不修改后续 `profile` 成员主数据。若后续服务需要同步，应通过正式事件或接口适配，不能由 `auth` 直接写其他服务数据库。

审计要求：成功写入 `AUTH_MINECRAFT_UNBOUND`。

### 后台用户接口

#### 用户列表

`GET /api/v1/auth/admin/users`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配用户名、展示名或 Minecraft ID。 |
| `status` | string | 否 | 用户状态。 |
| `role` | string | 否 | 基础角色。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`、`lastLoginAt_desc`、`updatedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `UserSummary[]`。

权限规则：`ADMIN` 和 `OWNER` 可访问。`HELPER` 和 `USER` 返回 `42001`。

#### 用户详情

`GET /api/v1/auth/admin/users/{userId}`

成功响应 HTTP `200`，`data` 为 `UserSummary`，可额外包含 `securitySummary`。

资源不存在返回 `43100`。

#### 修改用户基础信息和状态

`PATCH /api/v1/auth/admin/users/{userId}`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `displayName` | string | 否 | 2 到 24 位。 |
| `status` | string | 否 | 允许 `PENDING_PROFILE`、`ACTIVE`、`DISABLED`、`BANNED`、`DELETED`。 |
| `reason` | string | 是 | 1 到 200 位，后台修改原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `UserSummary`。

业务规则：`ADMIN` 不能修改 `OWNER`。任何人不能禁用、封禁、删除或降级唯一 `OWNER`。修改为 `DISABLED`、`BANNED`、`DELETED` 后必须吊销该用户全部会话。`DELETED` 是软删除，用户名保留占用。

审计要求：成功写入 `AUTH_USER_UPDATED`。涉及状态变化时记录操作前后状态。失败不改变用户状态，不吊销会话。

#### 修改角色和能力点

`PUT /api/v1/auth/admin/users/{userId}/roles`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `roles` | string[] | 是 | 至少一个基础角色。 |
| `permissions` | string[] | 是 | 能力点列表，可为空数组。 |
| `reason` | string | 是 | 1 到 200 位，修改原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `UserSummary`。

权限规则：只能由 `OWNER` 执行。`ADMIN`、`HELPER`、`USER` 返回 `42001`。

业务规则：不能移除唯一 `OWNER` 的 `OWNER` 角色。不能把 `OWNER` 降级为普通角色，除非系统中仍有其他 `OWNER`。角色或能力点降权后必须吊销目标用户全部会话，避免旧会话继续携带旧权限。

审计要求：成功写入 `AUTH_ROLE_PERMISSION_UPDATED`，必须记录变更前后角色和能力点。

### 后台邀请码接口

#### 邀请码列表

`GET /api/v1/auth/admin/invitations`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `type` | string | 否 | `PLAYER` 或 `ADMIN`。 |
| `status` | string | 否 | 邀请码状态。 |
| `createdBy` | string | 否 | 创建者用户 ID。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`expiresAt_asc`、`usedCount_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `InvitationSummary[]`。不得返回完整邀请码原始码。

权限规则：`ADMIN` 和 `OWNER` 可访问。`ADMIN` 只能看到自己创建的 `PLAYER` 邀请码和全部 `PLAYER` 邀请码的非敏感摘要；`OWNER` 可查看全部摘要。实现若选择更严格策略，必须保持不低于此权限要求。

#### 创建邀请码

`POST /api/v1/auth/admin/invitations`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `type` | string | 是 | `PLAYER` 或 `ADMIN`。 |
| `boundRoles` | string[] | 是 | 使用后授予的基础角色。 |
| `boundPermissions` | string[] | 否 | 使用后授予的能力点，默认空数组。 |
| `maxUses` | integer | 是 | 1 到 1000。 |
| `expiresAt` | string | 否 | ISO 8601 时间，必须晚于当前时间。 |
| `reason` | string | 是 | 1 到 200 位，创建原因。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "invitation": {},
    "rawCode": "BM-EXAMPLE-ONCE"
  }
}
```

业务规则：`PLAYER` 邀请码可由 `ADMIN` 或 `OWNER` 创建，默认绑定 `USER`。`ADMIN` 邀请码只能由 `OWNER` 创建。`ADMIN` 不能创建会授予 `OWNER`、`ADMIN` 或任一运维能力点的邀请码。任何邀请码都不能授予 `OWNER`。

幂等规则：同一创建者、同一 `idempotencyKey`、同一请求体重复提交时返回同一个邀请码和同一个原始码。相同幂等键搭配不同请求体返回 `43002`。

审计要求：成功写入 `AUTH_INVITATION_CREATED`。创建 `ADMIN` 邀请码必须记录为 `MEDIUM` 风险。

#### 禁用邀请码

`PATCH /api/v1/auth/admin/invitations/{invitationId}/disable`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，禁用原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `InvitationSummary`。

业务规则：已禁用的邀请码重复禁用返回成功，保持幂等。已过期或已用完的邀请码允许禁用，但状态展示按禁用优先。

权限规则：`ADMIN` 只能禁用自己创建的 `PLAYER` 邀请码。`OWNER` 可禁用所有邀请码。

审计要求：首次禁用写入 `AUTH_INVITATION_DISABLED`。重复禁用不重复写审计。

#### 邀请码使用记录

`GET /api/v1/auth/admin/invitations/{invitationId}/usage-records`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |

成功响应 HTTP `200`，分页 `items` 包含 `id`、`invitationId`、`usedByUserId`、`usedByUsername`、`usedAt`、`sourceIp`、`requestId`。

权限规则：`ADMIN` 只能查看自己创建的 `PLAYER` 邀请码使用记录。`OWNER` 可查看全部使用记录。

### 状态流转

用户状态允许流转如下：`PENDING_PROFILE` 可流转为 `ACTIVE`、`DISABLED`、`BANNED`、`DELETED`；`ACTIVE` 可流转为 `DISABLED`、`BANNED`、`DELETED`；`DISABLED` 可恢复为 `ACTIVE`；`BANNED` 可恢复为 `ACTIVE`；`DELETED` 不可恢复。唯一 `OWNER` 不允许进入 `DISABLED`、`BANNED` 或 `DELETED`。

邀请码状态由字段计算。未禁用、未过期、使用次数未满时为 `ACTIVE`。手动禁用后为 `DISABLED`。超过 `expiresAt` 为 `EXPIRED`。`usedCount >= maxUses` 为 `EXHAUSTED`。状态冲突时展示优先级为 `DISABLED`、`EXPIRED`、`EXHAUSTED`、`ACTIVE`。

会话状态至少包含可用、过期和吊销。会话必须区分会话 ID 和访问令牌，任何列表或审计接口不得返回访问令牌明文。密码重置、用户禁用、用户封禁、用户软删除、角色能力点降权都必须吊销相关会话；当前用户主动修改密码时，必须吊销该用户除当前会话以外的其他会话。

### 限流要求

注册、登录、密码重置申请、邀请码校验和敏感后台写操作必须限流。限流维度至少包含来源 IP，登录和密码重置还必须包含用户名维度。限流命中时返回对应 `44xxx` 错误码。限流不能消耗邀请码次数，不能创建用户，不能修改密码。

### 审计要求

必须审计的动作包括注册成功、管理员邀请码使用、登录成功、登录失败触发风控、退出、密码重置申请、密码重置确认、用户状态修改、角色能力点修改、邀请码创建、邀请码禁用、Minecraft 绑定和解绑。

审计字段继承公共契约。后台写操作必须记录 `reason`。审计失败时，后台写操作不得假装成功；登录成功、退出和普通注册可先完成主流程，但必须至少写安全日志，后续实现如果支持审计重试，需要记录补偿状态。

### 验收口径

`auth` API 文档按 `docs/contracts-auth.md` 独立存在，并由 `.local-docs/tests-auth.md` 记录本地测试闭环。本文档列出的每个接口都有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级和审计要求。

注册、登录、当前用户、会话校验和退出形成最小账号闭环。后台用户管理、角色能力点、邀请码管理、密码重置和 Minecraft 绑定均按本文档实现。本地测试文档中的全部用例最终必须全部通过，且不能为了通过测试降低本文档、公共契约、需求文档或系统设计的要求。

## 北冥官网 profile API 契约

来源：`docs/contracts-profile.md`

版本：0.1

### 文档定位

本文档是 `profile` 微服务的正式 API 契约。后续 `notification`、`content`、`admin`、`onboarding`、`exam`、`whitelist`、`attendance`、`community` 等服务只能通过本文档定义的接口适配成员档案，不能直接读取或修改 `profile` 数据库。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、运维能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `profile` 模块自己的路径、字段、状态、权限、错误码、审计和验收口径。

`profile` 适配 `auth`，不要求 `auth` 反向适配 `profile`。`profile` 只能通过后端入口提供的认证上下文、`/api/v1/auth/me`、`/api/v1/auth/session/verify` 或测试环境 auth stub 读取当前用户摘要，不能直接读取 auth 数据表，不能修改 auth 用户状态。

### 职责边界

`profile` 负责成员公开档案、公开展示字段、Minecraft 展示信息快照、成员组、成员状态、加入时间、个人简介、成员事迹、代表作品快照、活动和贡献展示入口。

`profile` 不负责注册、登录、邀请码、会话、账号角色能力点、账号级 Minecraft 绑定验证、考试判分、白名单审核、通知投递、考勤积分计算、资源上传、社区内容审核或运维操作。

`auth` 拥有用户、会话、角色、能力点和账号级 Minecraft 绑定。`profile` 只保存创建或更新档案时取得的账号展示快照和 Minecraft 展示快照。账号绑定变化不会由 `auth` 直接写入 `profile`，后续如需同步必须通过正式事件或本文档新增接口。

### 基础路径与认证

所有接口默认使用 `/api/v1/profile` 前缀。

公开读取接口无需 `Authorization`，但只能返回公开字段，并且必须遵守统一响应、请求编号、分页和错误码。

当前用户接口必须使用已认证上下文，只能读取或维护当前认证用户自己的成员档案。认证上下文优先来自后端入口注入的网关可信身份头；没有完整网关可信上下文时，保留 `Authorization: Bearer <token>` 本地兼容路径。

后台接口统一使用 `/api/v1/profile/admin` 前缀。后台读取至少要求 `HELPER`、`ADMIN` 或 `OWNER`。后台写操作至少要求 `ADMIN` 或 `OWNER`。成员移除、归档、恢复公开、恢复状态等会影响成员资格或公开展示的操作必须携带 `reason` 并写入审计。

### 请求编号和输入边界

profile 必须接收或生成 `X-Request-Id`。客户端或网关传入的请求编号只允许 1 到 128 位字母、数字、下划线、短横线、点和冒号。缺失或空白时由 profile 生成请求编号，并在响应头和错误响应体 `requestId` 中保持一致。

当直连 profile 且 `X-Request-Id` 格式非法时，profile 返回 HTTP `400`、错误码 `40001`，`errors.field` 为 `X-Request-Id`，响应头和响应体使用服务端兜底请求编号，不得把非法请求编号写入审计。经 `api-gateway` 访问时，非法请求编号应由网关先按 `docs/contracts-api-gateway.md` 返回 `46205`，正常情况下不应到达 profile。

所有请求体中的时间字段必须是 ISO 8601 字符串。当前契约涉及 `joinedAt` 和 `happenedAt`。时间字段缺失时按各接口默认规则处理；时间字段存在但格式非法时必须返回 HTTP `400`、错误码 `40001`，不得落入 `51200` 内部错误。

所有列表接口的枚举筛选、排序参数和长度受限查询参数必须严格校验。`keyword` 超过 50 位、`sort` 不在接口列出的允许值内、`status` 或 `visibility` 不在允许枚举内时，profile 必须返回 HTTP `400`、错误码 `40001` 或分页类公共错误码，不得静默回退默认排序或忽略非法筛选。

### auth 兼容契约

profile 必须通过 `ProfileAuthContextProvider`、`AuthContextProvider` 或等价适配层读取 auth 信息。生产环境优先消费后端入口传入的已校验认证上下文，也可以调用 auth 正式 API。测试环境使用 auth stub。任何实现都不能导入 auth 的内存存储类、数据表实体、Repository 或测试种子实现。

当前请求认证上下文至少包含以下字段：`userId`、`displayName`、`roles`、`permissions`、`status`、`minecraftBinding`。`minecraftBinding` 字段结构必须兼容 `docs/contracts-auth.md` 中的 `MinecraftBinding`。

后台激活目标用户时，profile 必须通过 auth 适配层读取目标用户快照。目标用户快照至少包含 `id`、`displayName`、`roles`、`permissions`、`status` 和 `minecraftBinding`。客户端请求体不得传入并覆盖 `displayNameSnapshot`、`authUserStatusSnapshot`、`authRolesSnapshot` 或 `minecraftBinding` 作为可信来源；这些字段只能来自 auth 适配层返回值。若后续网关已经传入可信目标用户快照，profile 仍必须校验来源为服务端上下文，而不是浏览器请求体。

auth 用户状态为 `PENDING_PROFILE` 或 `ACTIVE` 时允许创建或激活成员档案。`DISABLED`、`BANNED`、`DELETED` 不允许激活。目标用户不存在返回 `43204`。目标用户状态不允许激活返回 `43215`。auth 不可用返回 `46200`，auth 调用超时返回 `46201`，auth 返回字段缺失或枚举不兼容返回 `46202`。

profile 不能修改 auth 用户状态、角色、权限或 Minecraft 绑定。profile 写接口完成后，auth 用户快照只允许作为 profile 本地快照保存，不得反写 auth。

### 网关可信认证上下文

profile 对网关可信认证上下文的消费只补充认证来源，不新增业务 API，不改变现有路径、响应结构、角色规则、端口或 Bearer stub 兼容行为。

本轮适配参考成熟网关生态的通用边界。Spring Cloud Gateway 使用过滤器向下游请求新增请求头，也支持在转发前移除请求头；Kong Correlation ID 插件把请求相关编号放入 HTTP 头并可传给上游；Nginx 反向代理通过 `proxy_set_header` 显式设置发往上游的头；Traefik ForwardAuth 支持把认证服务响应头复制到转发请求，并强调可信转发头应由入口层清洗。profile 只吸收这些边界思路：入口统一认证并注入上下文，业务服务只消费格式完整的服务端上下文，缺失时回退既有兼容路径。

网关可信上下文必须同时具备 `X-Gateway-Internal-Request-Id` 和 `X-Beiming-Actor-User-Id`。只有 `X-Gateway-Internal-Request-Id` 存在时，profile 才进入网关上下文解析；若该头缺失，即使请求带有 `X-Beiming-Actor-*`，profile 也必须忽略这些头并继续走 `Authorization: Bearer <token>` 兼容路径。这样可以保持本地直连测试和旧调用不受影响。

profile 需要消费的网关上下文字段如下。

| 请求头 | 必填 | 说明 |
| --- | --- | --- |
| `X-Gateway-Internal-Request-Id` | 是 | 网关注入的内部请求编号，格式必须与公共请求编号规则一致。 |
| `X-Beiming-Actor-User-Id` | 是 | 当前认证用户 ID。 |
| `X-Beiming-Actor-Roles` | 否 | 逗号分隔基础角色。可为空；为空时后台接口按角色不足返回 `42001`。非空项必须是 `OWNER`、`ADMIN`、`HELPER` 或 `USER`。 |
| `X-Beiming-Actor-Permissions` | 否 | 逗号分隔能力点。可为空；非空项必须兼容公共契约中的能力点。 |
| `X-Beiming-Actor-Minecraft-Id` | 否 | 当前认证用户账号级 Minecraft 展示 ID，只能作为当前 actor 上下文快照。 |
| `X-Beiming-Actor-Minecraft-Uuid` | 否 | 当前认证用户账号级 Minecraft UUID，只能作为当前 actor 上下文快照。 |

逗号分隔字段必须先 trim，再丢弃空白项。角色头为空不代表后台权限通过，只能形成没有后台角色的当前用户上下文。`X-Beiming-Actor-Minecraft-Id` 和 `X-Beiming-Actor-Minecraft-Uuid` 只属于当前 actor，不得用于覆盖后台激活目标用户的快照。后台激活成员档案仍必须读取目标用户快照，不能把当前 actor 的 Minecraft 头当成目标用户资料。

当 `X-Gateway-Internal-Request-Id` 存在但缺少 `X-Beiming-Actor-User-Id`、内部请求编号格式非法、角色或能力点格式非法、Minecraft UUID 格式非法，或任一必需字段无法解析时，profile 返回 HTTP `502`、错误码 `46202`。当网关上下文不存在且 Bearer 缺失或 Bearer 格式非法时，仍按公共认证错误返回 `41000` 或 `41003`。

安全边界固定为：浏览器伪造可信头的剥离责任归 `api-gateway`；profile 的责任是只消费格式完整的服务端上下文，并在上下文缺失时继续走 Bearer 兼容路径。当前 P0 没有内部签名，不能把本轮适配宣称为生产级内部认证。生产部署必须要求 profile 只暴露给网关或可信内网；后续若增加网关到上游共享密钥或内部签名，需要先更新 `api-gateway`、`profile` 契约和对应测试闭环。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `MemberStatus` | `PENDING_ACTIVATION`、`ACTIVE`、`INACTIVE`、`SUSPENDED`、`REMOVED`、`ARCHIVED` | 成员档案状态。`PENDING_ACTIVATION` 表示账号存在但档案未正式激活，`ACTIVE` 表示正式成员，`REMOVED` 表示已离开或被移除白名单后的历史成员，`ARCHIVED` 表示档案归档。 |
| `ProfileVisibility` | `PUBLIC`、`PRIVATE` | 公开可见性。`PRIVATE` 不进入公开列表，公开详情返回不存在。 |
| `MilestoneType` | `JOINED`、`PROJECT`、`EVENT`、`AWARD`、`MANAGEMENT`、`OTHER` | 成员事迹类型。 |
| `WorkSnapshotType` | `BUILD`、`REDSTONE`、`FARM`、`ARTICLE`、`IMAGE`、`VIDEO`、`OTHER` | 代表作品快照类型。 |
| `ProfileAuditResult` | `SUCCESS`、`FAILED` | profile 审计结果。 |

### 通用对象

#### MemberGroup

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 是 | 成员组 ID。 |
| `name` | string | 是 | 成员组名称，2 到 24 位，同一未归档成员组内唯一。 |
| `description` | string 或 null | 是 | 成员组说明，最多 200 位。 |
| `color` | string 或 null | 是 | 展示色，使用十六进制颜色，例如 `#2F80ED`。 |
| `sortOrder` | integer | 是 | 展示排序，数字越小越靠前。 |
| `archived` | boolean | 是 | 是否归档。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |

#### PublicMemberSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `memberId` | string | 是 | 成员档案 ID。 |
| `displayName` | string | 是 | 成员展示名快照。 |
| `avatarUrl` | string 或 null | 是 | 头像地址。 |
| `minecraftId` | string 或 null | 是 | Minecraft 展示 ID。 |
| `minecraftUuid` | string 或 null | 是 | Minecraft UUID，无连字符小写格式。 |
| `skinUrl` | string 或 null | 是 | 皮肤展示地址。 |
| `group` | MemberGroup 或 null | 是 | 成员组公开摘要。 |
| `status` | string | 是 | 公开可展示状态，只返回 `ACTIVE`、`INACTIVE` 或 `SUSPENDED`。 |
| `joinedAt` | string 或 null | 是 | 加入时间。 |
| `bio` | string 或 null | 是 | 个人简介，公开列表中最多返回 160 位。 |
| `featuredWorkCount` | integer | 是 | 公开代表作品数量。 |
| `milestoneCount` | integer | 是 | 公开事迹数量。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### PublicMemberDetail

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `memberId` | string | 是 | 成员档案 ID。 |
| `displayName` | string | 是 | 成员展示名快照。 |
| `avatarUrl` | string 或 null | 是 | 头像地址。 |
| `minecraftId` | string 或 null | 是 | Minecraft 展示 ID。 |
| `minecraftUuid` | string 或 null | 是 | Minecraft UUID，无连字符小写格式。 |
| `skinUrl` | string 或 null | 是 | 皮肤展示地址。 |
| `group` | MemberGroup 或 null | 是 | 成员组公开摘要。 |
| `status` | string | 是 | 公开可展示状态。 |
| `joinedAt` | string 或 null | 是 | 加入时间。 |
| `bio` | string 或 null | 是 | 个人简介，最多 1000 位。 |
| `milestones` | MemberMilestone[] | 是 | 公开成员事迹。 |
| `workSnapshots` | MemberWorkSnapshot[] | 是 | 公开代表作品快照。 |
| `activitySummary` | object 或 null | 是 | 活动展示入口摘要，P0 可为 `null`。 |
| `contributionSummary` | object 或 null | 是 | 贡献展示入口摘要，P0 可为 `null`。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### AdminMemberProfile

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `memberId` | string | 是 | 成员档案 ID。 |
| `userId` | string | 是 | auth 用户 ID。 |
| `displayNameSnapshot` | string | 是 | auth 展示名快照。 |
| `authUserStatusSnapshot` | string | 是 | 创建或最近同步时的 auth 用户状态快照。 |
| `authRolesSnapshot` | string[] | 是 | 创建或最近同步时的 auth 基础角色快照。 |
| `avatarUrl` | string 或 null | 是 | 头像地址。 |
| `minecraftId` | string 或 null | 是 | Minecraft 展示 ID。 |
| `minecraftUuid` | string 或 null | 是 | Minecraft UUID。 |
| `skinUrl` | string 或 null | 是 | 皮肤展示地址。 |
| `group` | MemberGroup 或 null | 是 | 成员组。 |
| `status` | string | 是 | 成员状态。 |
| `visibility` | string | 是 | 公开可见性。 |
| `joinedAt` | string 或 null | 是 | 加入时间。 |
| `bio` | string 或 null | 是 | 个人简介。 |
| `adminNote` | string 或 null | 是 | 后台备注，不得出现在公开接口。 |
| `milestones` | MemberMilestone[] | 是 | 成员事迹。 |
| `workSnapshots` | MemberWorkSnapshot[] | 是 | 代表作品快照。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |

#### MemberMilestone

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 是 | 事迹 ID。 |
| `type` | string | 是 | 事迹类型。 |
| `title` | string | 是 | 标题，2 到 80 位。 |
| `description` | string 或 null | 是 | 说明，最多 500 位。 |
| `happenedAt` | string | 是 | 事迹发生时间。 |
| `publicVisible` | boolean | 是 | 是否公开展示。 |
| `sortOrder` | integer | 是 | 展示排序。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### MemberWorkSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 是 | 作品快照 ID。 |
| `type` | string | 是 | 作品类型。 |
| `title` | string | 是 | 标题，2 到 80 位。 |
| `summary` | string 或 null | 是 | 摘要，最多 300 位。 |
| `coverUrl` | string 或 null | 是 | 封面地址。 |
| `sourceModule` | string 或 null | 是 | 来源模块，例如 `content`、`activity`，P0 手工维护时可为 `null`。 |
| `sourceId` | string 或 null | 是 | 来源业务 ID，P0 手工维护时可为 `null`。 |
| `publicVisible` | boolean | 是 | 是否公开展示。 |
| `sortOrder` | integer | 是 | 展示排序。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### profile 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `43200` | 404 | 成员档案不存在。 |
| `43201` | 404 | 成员组不存在。 |
| `43202` | 404 | 成员事迹不存在。 |
| `43203` | 404 | 代表作品快照不存在。 |
| `43204` | 404 | auth 目标用户不存在。 |
| `43210` | 409 | 当前 auth 用户已存在成员档案。 |
| `43211` | 409 | Minecraft ID 或 UUID 已被其他成员档案使用。 |
| `43212` | 409 | 成员状态不允许当前流转。 |
| `43213` | 409 | 成员档案不可公开访问。 |
| `43214` | 409 | 成员组仍被未归档成员使用，不能归档。 |
| `43215` | 409 | auth 目标用户状态不允许激活成员档案。 |
| `46200` | 502 | auth 认证上下文不可用。 |
| `46201` | 504 | auth 认证上下文调用超时。 |
| `46202` | 502 | auth 返回的认证上下文、用户快照或网关可信身份头不兼容 profile 契约。 |
| `51200` | 500 | profile 内部错误。 |
| `51201` | 500 | profile 审计写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、幂等键冲突和通用服务端错误优先使用公共错误码。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开成员列表 | GET | `/api/v1/profile/members` | 否 | 无 | LOW |
| 公开成员详情 | GET | `/api/v1/profile/members/{memberId}` | 否 | 无 | LOW |
| 当前用户成员档案 | GET | `/api/v1/profile/me` | 是 | 当前用户 | LOW |
| 当前用户维护公开资料 | PATCH | `/api/v1/profile/me` | 是 | 当前用户 | MEDIUM |
| 后台成员列表 | GET | `/api/v1/profile/admin/members` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台成员详情 | GET | `/api/v1/profile/admin/members/{memberId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建或激活成员档案 | POST | `/api/v1/profile/admin/members/activate` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台修改成员档案 | PATCH | `/api/v1/profile/admin/members/{memberId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改成员状态 | PATCH | `/api/v1/profile/admin/members/{memberId}/status` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 成员组列表 | GET | `/api/v1/profile/admin/groups` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建成员组 | POST | `/api/v1/profile/admin/groups` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改成员组 | PATCH | `/api/v1/profile/admin/groups/{groupId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档成员组 | PATCH | `/api/v1/profile/admin/groups/{groupId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 维护成员事迹 | PUT | `/api/v1/profile/admin/members/{memberId}/milestones` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 维护代表作品快照 | PUT | `/api/v1/profile/admin/members/{memberId}/work-snapshots` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 成员审计列表 | GET | `/api/v1/profile/admin/members/{memberId}/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

### 公开成员接口

#### 公开成员列表

`GET /api/v1/profile/members`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配展示名、Minecraft ID 或简介，最多 50 位。 |
| `groupId` | string | 否 | 成员组 ID。 |
| `status` | string | 否 | 只允许 `ACTIVE`、`INACTIVE`、`SUSPENDED`。 |
| `sort` | string | 否 | 允许 `joinedAt_desc`、`joinedAt_asc`、`updatedAt_desc`、`displayName_asc`。默认 `joinedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `PublicMemberSummary[]`。

业务规则：只返回 `visibility` 为 `PUBLIC` 且状态为 `ACTIVE`、`INACTIVE` 或 `SUSPENDED` 的成员档案。`PENDING_ACTIVATION`、`REMOVED`、`ARCHIVED` 和软删除档案不得出现在公开列表。公开列表不得返回 `userId`、`authRolesSnapshot`、`authUserStatusSnapshot`、`adminNote`、审计原因、后台备注、登录名、邮箱、手机号或权限点。

降级规则：profile 服务不可用时返回通用服务端错误或模块内部错误，前端只能局部展示成员暂不可用，不能整页空白。

#### 公开成员详情

`GET /api/v1/profile/members/{memberId}`

成功响应 HTTP `200`，`data` 为 `PublicMemberDetail`。

业务规则：只有公开可见且状态允许公开展示的成员档案可以访问。成员不存在返回 `43200`。成员存在但不可公开访问时返回 `43213`，实现也可以出于防枚举考虑返回 `43200`，但同一版本内必须保持一致并写入测试。

降级规则：公开详情不得因活动或贡献入口暂不可用而整体失败。`activitySummary` 和 `contributionSummary` 在依赖不可用时返回 `null`。

### 当前用户接口

#### 当前用户成员档案

`GET /api/v1/profile/me`

成功响应 HTTP `200`，`data` 为 `AdminMemberProfile` 去除 `adminNote` 和审计字段后的当前用户视图。

业务规则：必须要求登录。只返回当前认证用户 `userId` 对应的成员档案。当前用户没有成员档案时返回 `43200`，不得伪造空档案成功。auth 上下文不可用返回 `46200` 或 `46201`。

#### 当前用户维护公开资料

`PATCH /api/v1/profile/me`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `avatarUrl` | string 或 null | 否 | 最多 500 位，必须是 http、https 或站内资源路径。 |
| `skinUrl` | string 或 null | 否 | 最多 500 位，必须是 http、https 或站内资源路径。 |
| `bio` | string 或 null | 否 | 最多 1000 位。 |
| `visibility` | string | 否 | 允许 `PUBLIC` 或 `PRIVATE`。 |
| `reason` | string | 是 | 1 到 200 位，修改原因。 |

成功响应 HTTP `200`，`data` 为当前用户视图。

业务规则：当前用户只能维护自己的公开展示字段，不能修改 `userId`、`displayNameSnapshot`、`minecraftId`、`minecraftUuid`、`memberGroupId`、`status`、`joinedAt`、`adminNote`、事迹或代表作品快照。当前用户档案为 `REMOVED`、`ARCHIVED` 或软删除时返回 `43212`。

审计要求：成功写入 `PROFILE_SELF_UPDATED`，记录操作者、目标档案、修改字段和原因。失败不得改变档案。

### 后台成员接口

#### 后台成员列表

`GET /api/v1/profile/admin/members`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配展示名、Minecraft ID、用户 ID 或后台备注，最多 50 位。 |
| `groupId` | string | 否 | 成员组 ID。 |
| `status` | string | 否 | 任一 `MemberStatus`。 |
| `visibility` | string | 否 | `PUBLIC` 或 `PRIVATE`。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`updatedAt_desc`、`joinedAt_desc`、`displayName_asc`。 |

成功响应 HTTP `200`，分页 `items` 为 `AdminMemberProfile[]`。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。未登录返回 `41000`。

#### 后台成员详情

`GET /api/v1/profile/admin/members/{memberId}`

成功响应 HTTP `200`，`data` 为 `AdminMemberProfile`。

资源不存在返回 `43200`。权限不足返回公共权限错误码。

#### 创建或激活成员档案

`POST /api/v1/profile/admin/members/activate`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `userId` | string | 是 | auth 用户 ID。 |
| `avatarUrl` | string 或 null | 否 | 头像地址。 |
| `skinUrl` | string 或 null | 否 | 皮肤地址。 |
| `groupId` | string | 否 | 成员组 ID。 |
| `joinedAt` | string | 否 | 加入时间，默认当前时间。 |
| `bio` | string 或 null | 否 | 个人简介。 |
| `visibility` | string | 否 | 默认 `PUBLIC`。 |
| `reason` | string | 是 | 1 到 200 位，激活原因。 |
| `idempotencyKey` | string | 否 | 激活重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `AdminMemberProfile`。

业务规则：同一 `userId` 只能有一个未软删除成员档案。重复提交同一 `idempotencyKey` 和同一请求体返回同一个结果。相同幂等键搭配不同请求体返回 `43002`。profile 必须从 auth 目标用户快照生成 `displayNameSnapshot`、`authUserStatusSnapshot`、`authRolesSnapshot`、`minecraftId` 和 `minecraftUuid`，不能信任浏览器请求体中的同名字段。同一 Minecraft ID 或 UUID 不能绑定到两个未归档成员档案。成员组不存在返回 `43201`。创建成功后状态为 `ACTIVE`，除非后续白名单流程通过正式变更明确需要 `PENDING_ACTIVATION`；P0 默认直接 `ACTIVE`。

降级规则：auth 上下文不可用时不得创建档案，返回 `46200` 或 `46201`。审计写入失败时不得返回成功。

审计要求：成功写入 `PROFILE_MEMBER_ACTIVATED`，记录操作者、目标用户、目标成员档案、成员组、初始状态和原因。

#### 后台修改成员档案

`PATCH /api/v1/profile/admin/members/{memberId}`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `displayNameSnapshot` | string | 否 | 2 到 24 位，用于手工同步 auth 展示名快照。 |
| `avatarUrl` | string 或 null | 否 | 头像地址。 |
| `minecraftId` | string 或 null | 否 | Minecraft 展示 ID。 |
| `minecraftUuid` | string 或 null | 否 | Minecraft UUID。 |
| `skinUrl` | string 或 null | 否 | 皮肤地址。 |
| `groupId` | string 或 null | 否 | 成员组 ID，`null` 表示清空成员组。 |
| `joinedAt` | string 或 null | 否 | 加入时间。 |
| `bio` | string 或 null | 否 | 个人简介。 |
| `visibility` | string | 否 | `PUBLIC` 或 `PRIVATE`。 |
| `adminNote` | string 或 null | 否 | 后台备注，最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位，修改原因。 |

成功响应 HTTP `200`，`data` 为 `AdminMemberProfile`。

业务规则：成员不存在返回 `43200`。成员组不存在返回 `43201`。Minecraft ID 或 UUID 与其他未归档成员冲突返回 `43211`。后台修改不能改变 `userId`，不能绕过状态流转直接把归档档案改回公开。

审计要求：成功写入 `PROFILE_MEMBER_UPDATED`，记录变更前后摘要和原因。审计失败时不得改变档案。

#### 修改成员状态

`PATCH /api/v1/profile/admin/members/{memberId}/status`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `status` | string | 是 | 目标 `MemberStatus`。 |
| `reason` | string | 是 | 1 到 200 位，状态变化原因。 |

成功响应 HTTP `200`，`data` 为 `AdminMemberProfile`。

状态流转规则：`PENDING_ACTIVATION` 可流转为 `ACTIVE`、`REMOVED`、`ARCHIVED`；`ACTIVE` 可流转为 `INACTIVE`、`SUSPENDED`、`REMOVED`、`ARCHIVED`；`INACTIVE` 可流转为 `ACTIVE`、`SUSPENDED`、`REMOVED`、`ARCHIVED`；`SUSPENDED` 可流转为 `ACTIVE`、`INACTIVE`、`REMOVED`、`ARCHIVED`；`REMOVED` 只可流转为 `ARCHIVED`；`ARCHIVED` 不可恢复。

业务规则：非法流转返回 `43212`，失败时保持原状态。切换为 `REMOVED` 或 `ARCHIVED` 后公开接口不得再返回该成员。归档时写入 `archivedAt`。

审计要求：成功写入 `PROFILE_MEMBER_STATUS_CHANGED`，记录状态前后值和原因。

### 后台成员组接口

#### 成员组列表

`GET /api/v1/profile/admin/groups`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `includeArchived` | boolean | 否 | 默认 `false`。 |

成功响应 HTTP `200`，`data.items` 为 `MemberGroup[]`，按 `sortOrder` 升序排序。

#### 创建成员组

`POST /api/v1/profile/admin/groups`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `name` | string | 是 | 2 到 24 位，同一未归档成员组内唯一。 |
| `description` | string 或 null | 否 | 最多 200 位。 |
| `color` | string 或 null | 否 | 十六进制颜色。 |
| `sortOrder` | integer | 否 | 默认 `100`。 |
| `reason` | string | 是 | 1 到 200 位，创建原因。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `MemberGroup`。

业务规则：成员组名称冲突返回 `43001`。幂等键冲突返回 `43002`。

审计要求：成功写入 `PROFILE_GROUP_CREATED`。

#### 修改成员组

`PATCH /api/v1/profile/admin/groups/{groupId}`

请求字段同创建成员组，`reason` 必填，其余字段按需修改。

成功响应 HTTP `200`，`data` 为 `MemberGroup`。成员组不存在返回 `43201`。名称冲突返回 `43001`。

审计要求：成功写入 `PROFILE_GROUP_UPDATED`。

#### 归档成员组

`PATCH /api/v1/profile/admin/groups/{groupId}/archive`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，归档原因。 |

成功响应 HTTP `200`，`data` 为归档后的 `MemberGroup`。

业务规则：成员组不存在返回 `43201`。仍被未归档成员使用时返回 `43214`。重复归档同一成员组返回成功，保持幂等，不重复写审计。

审计要求：首次归档写入 `PROFILE_GROUP_ARCHIVED`。

### 后台事迹与作品接口

#### 维护成员事迹

`PUT /api/v1/profile/admin/members/{memberId}/milestones`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `items` | array | 是 | 最多 50 条。每条包含 `id`、`type`、`title`、`description`、`happenedAt`、`publicVisible`、`sortOrder`。新建项 `id` 可为空。 |
| `reason` | string | 是 | 1 到 200 位，维护原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `AdminMemberProfile`。

业务规则：该接口采用整体替换语义。请求中的 `items` 即为成员事迹的完整新集合。不存在于请求中的旧事迹视为移除，但必须通过审计保留操作记录。成员不存在返回 `43200`。字段非法返回 `40001`。同一请求内 `sortOrder` 可以重复，但返回时必须按 `sortOrder`、`happenedAt` 稳定排序。

审计要求：成功写入 `PROFILE_MEMBER_MILESTONES_REPLACED`，记录新增、修改、移除数量和原因。

#### 维护代表作品快照

`PUT /api/v1/profile/admin/members/{memberId}/work-snapshots`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `items` | array | 是 | 最多 30 条。每条包含 `id`、`type`、`title`、`summary`、`coverUrl`、`sourceModule`、`sourceId`、`publicVisible`、`sortOrder`。新建项 `id` 可为空。 |
| `reason` | string | 是 | 1 到 200 位，维护原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `AdminMemberProfile`。

业务规则：该接口采用整体替换语义。P0 可手工维护快照，后续 `content`、`activity` 等模块接入时只能通过正式接口或事件维护快照，不能直接写 profile 数据库。成员不存在返回 `43200`。字段非法返回 `40001`。

审计要求：成功写入 `PROFILE_MEMBER_WORKS_REPLACED`，记录新增、修改、移除数量和原因。

### 审计接口

#### 成员审计列表

`GET /api/v1/profile/admin/members/{memberId}/audit-logs`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |

成功响应 HTTP `200`，分页 `items` 使用公共审计字段，允许补充 `action`、`beforeState`、`afterState`、`reason`、`result`。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 可读成员详情，但不能读取审计列表。

业务规则：成员不存在返回 `43200`。审计日志不得通过 profile API 删除。

### 状态、幂等和并发

创建成员档案和创建成员组支持 `idempotencyKey`。同一操作者、同一幂等键、同一请求体重复提交时返回同一个结果。相同幂等键搭配不同请求体返回 `43002`。

幂等签名必须基于规范化后的请求语义生成，而不是基于 JSON 字段原始顺序。对象字段顺序不同但字段和值完全一致时，必须视为同一请求体；数组顺序仍然保留业务含义，不得排序。字段值不同、数组顺序不同或缺失字段不同，均视为不同请求体。

后台更新接口必须以服务端当前状态为准。状态流转和归档操作失败时不能写入部分变更。实现可使用版本号、更新时间或事务锁保证并发下同一 `userId`、Minecraft ID、Minecraft UUID、成员组名称不会产生重复主数据。

公开读取接口允许读到更新前或更新后的完整状态，但不能返回半更新对象。

### 审计要求

必须审计的动作包括当前用户维护公开资料、创建或激活成员档案、后台修改成员档案、修改成员状态、创建成员组、修改成员组、归档成员组、维护成员事迹和维护代表作品快照。

后台写操作必须记录 `reason`。审计字段继承公共契约。审计写入失败时，后台写操作和当前用户资料修改不得假装成功，必须返回 `51201` 或 `51200`，并保持业务数据不变。

profile 审计返回必须至少包含 `id`、`requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`sourceIp`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason` 和 `createdAt`。`paramsSummary` 只能保存脱敏摘要，不得保存完整请求体、`Authorization`、Cookie、邀请码、密码、Minecraft 验证凭据或其他秘密。

公开读取、当前用户读取、后台低风险读取不强制写审计。

### 失败降级

公开成员列表和公开成员详情失败时，前端按成员展示区局部降级。profile 不得返回伪造成功数据。

当前用户接口依赖认证上下文。认证失败、会话过期、用户禁用或 auth 调用失败时不得返回旧档案当作成功。

活动、贡献、作品来源模块不可用时，profile 公开详情可以返回已有快照和 `null` 摘要，不得因为后序模块未实现而阻塞 profile P0 闭环。

### 验收口径

`profile` API 文档按 `docs/contracts-profile.md` 独立存在，并由 `.local-docs/tests-profile.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级和审计要求。

`profile` 完成时必须满足以下条件：全部接口按本文档实现；公开接口不泄露后台字段和 auth 安全字段；当前用户接口只能访问当前用户自己的档案；后台接口按角色限制；创建或激活成员档案不直接读取 auth 数据库；受保护接口同时支持网关可信认证上下文和旧 Bearer 兼容路径；状态流转、成员组、事迹、作品快照、审计和网关认证上下文消费全部有自动化测试；`.local-docs/tests-profile.md` 中记录的全部测试用例最终通过；未实现时自动化测试必须先失败，不能跳过红灯验证。

## 北冥官网 notification API 契约

来源：`docs/contracts-notification.md`

版本：0.3

### 文档定位

本文档是 `notification` 微服务的正式 API 契约。后续 `content`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、`admin` 和 `ops-control` 只能通过本文档定义的接口投递或读取通知结果，不能在各自模块内自建通知主数据、未读数或模板系统。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `notification` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、降级、审计和验收口径。

`notification` 适配 `auth`，不要求 `auth` 反向适配 `notification`。它通过后端入口传入的认证上下文、`/api/v1/auth/me`、`/api/v1/auth/session/verify` 或测试环境 auth stub 读取当前用户、角色、能力点和目标用户快照。它不能直接读取 auth 数据表，不能修改 auth 用户状态，不能自行实现登录、会话或权限判断。

本轮补强参考成熟通知和网关生态的稳定做法。Firebase Cloud Messaging HTTP v1 把服务端发送请求限定在可信服务端凭据和短期访问令牌链路中；OneSignal 和 Courier 都强调通知创建重试必须使用幂等键，避免网络超时后重复发送；Novu 把工作流触发、订阅者和载荷分开处理，并支持用事务编号去重；OneSignal 的消息 API 还把目标受众、消息内容、调度和响应处理拆成清晰边界。notification 只吸收这些边界思路：服务端认证上下文来自入口层，创建类请求保持幂等，目标收件人先解析再投递，批量投递保持全有或全无，投递结果和用户读取状态分开维护，审计只保存安全摘要，幂等记录只在有限窗口内有效。P0 不引入外部推送服务、真实渠道发送、动态受众规则或跨平台工作流引擎。

### 职责边界

`notification` 负责站内通知、收件人状态、未读数、已读状态、归档状态、通知模板、模板变量、投递记录、失败原因和通知审计。

`notification` 不负责判定考试是否通过，不负责白名单审核，不负责积分计算，不负责处罚逻辑，不负责内容审核，不负责运维高风险审批，不负责修改任何业务结果。业务模块只把已经发生的业务结果传给 `notification`，`notification` 只按契约落通知、维护收件人的读取状态和返回投递结果。

P0 只实现站内通知。邮件、短信、QQ、Oopz 和游戏内消息只保留渠道枚举和后续扩展位置，不进行真实发送。P0 写接口只允许 `IN_APP` 渠道；提交其他渠道返回字段校验错误。

### 数据归属

`notification` 拥有以下主数据：通知主体、收件人状态、模板、模板版本、模板变量定义、投递记录、幂等记录和通知审计日志。

`notification` 可以保存接收人展示名快照，用于通知列表展示。快照来自 auth 目标用户快照、后端入口可信上下文或受信服务端调用方，不来自浏览器可篡改字段。快照不是 auth 或 profile 主数据，不能用于权限判断。

`notification` 不直接依赖 `profile`。需要展示成员名、头像或 Minecraft 身份时，由调用方传入可信服务端快照，或后续通过正式 profile 接口扩展，不能读取 profile 数据表。

### 基础路径与认证

当前用户接口使用 `/api/v1/notifications/me` 前缀。当前用户接口全部要求登录，只能读取和维护当前认证用户自己的收件人状态，不能通过请求参数传入 `userId` 读取别人通知。

后台接口使用 `/api/v1/notifications/admin` 前缀。后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`。后台创建通知、按模板投递、创建模板、修改模板、启用模板和禁用模板要求 `ADMIN` 或 `OWNER`。模板和批量投递写操作必须携带 `reason` 并写审计。

供后续服务调用的投递能力在 P0 以后台受控 HTTP API 表达，即 `POST /api/v1/notifications/admin/messages` 和 `POST /api/v1/notifications/admin/messages/from-template`。后续如果改成服务间内部接口或消息队列，不能破坏本文档定义的请求语义、幂等语义、投递状态和审计要求。

### auth 兼容契约

当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。后台创建通知时，服务端必须对 `recipientUserIds` 逐个解析目标用户快照。目标用户快照至少包含 `id`、`displayName`、`roles`、`permissions` 和 `status`。

目标用户不存在返回 `43315`。目标用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不允许投递，返回 `43315`，不得为该用户创建收件人记录。auth 不可用返回 `46300`，auth 调用超时返回 `46301`，auth 返回字段缺失或枚举不兼容返回 `46302`。

批量投递采用全有或全无语义。只要任一收件人不存在、状态不可投递、auth 不可用、模板渲染失败、审计写入失败或存储写入失败，本次请求不得创建半成品通知、不得更新未读数、不得写入部分收件人成功状态。

浏览器请求体不得覆盖当前登录用户、当前用户角色、当前用户能力点、收件人读取状态、收件人归档状态、可信展示名快照或投递状态。

### 网关可信认证上下文

notification 对网关可信认证上下文的消费只补充认证来源，不新增业务 API，不改变现有路径、响应结构、角色规则、端口或 Bearer stub 兼容行为。

认证来源优先级固定为：后端入口注入的可信认证上下文优先；缺少完整网关上下文时，继续保留 `Authorization: Bearer <token>` 本地兼容路径。只有 `X-Gateway-Internal-Request-Id` 存在时，notification 才进入网关上下文解析。若该头缺失，即使请求带有 `X-Beiming-Actor-*`，notification 也必须忽略这些头并回退 Bearer 兼容路径。

notification 需要消费的网关上下文字段如下。

| 请求头 | 必填 | 说明 |
| --- | --- | --- |
| `X-Gateway-Internal-Request-Id` | 是 | 网关注入的内部请求编号，格式必须与公共请求编号规则一致。 |
| `X-Beiming-Actor-User-Id` | 是 | 当前认证用户 ID。 |
| `X-Beiming-Actor-Roles` | 否 | 逗号分隔基础角色。可为空；为空时后台接口按角色不足返回 `42001`。非空项必须是 `OWNER`、`ADMIN`、`HELPER` 或 `USER`。 |
| `X-Beiming-Actor-Permissions` | 否 | 逗号分隔能力点。可为空；非空项必须兼容公共契约中的能力点。 |
| `X-Beiming-Actor-Minecraft-Id` | 否 | 当前认证用户账号级 Minecraft 展示 ID，只能作为当前 actor 快照。 |
| `X-Beiming-Actor-Minecraft-Uuid` | 否 | 当前认证用户账号级 Minecraft UUID，只能作为当前 actor 快照。 |

逗号分隔字段必须先 trim，再丢弃空白项。角色头为空不代表后台权限通过，只能形成没有后台角色的当前用户上下文。`X-Beiming-Actor-Minecraft-Id` 和 `X-Beiming-Actor-Minecraft-Uuid` 只属于当前 actor，不得用于覆盖通知目标收件人的展示名、状态、已读状态、归档状态、投递状态或任何收件人快照。后台创建通知仍必须通过 notification 自己的 auth 适配层解析 `recipientUserIds` 的目标用户快照，不能把当前 actor 的网关头当作目标用户资料。

当 `X-Gateway-Internal-Request-Id` 存在但缺少 `X-Beiming-Actor-User-Id`、内部请求编号格式非法、角色或能力点枚举不兼容、Minecraft UUID 格式非法，或任一必需字段无法解析时，notification 返回 HTTP `502`、错误码 `46302`。当网关上下文不存在且 Bearer 缺失或 Bearer 格式非法时，仍按公共认证错误返回 `41000` 或 `41003`。

安全边界固定为：浏览器伪造可信头的剥离责任归 `api-gateway`；notification 的责任是只消费格式完整的服务端上下文，并在上下文缺失时继续走 Bearer 兼容路径。当前 P0 没有内部签名，不能把本轮适配宣称为生产级内部认证。生产部署必须要求 notification 只暴露给网关或可信内网。后续若增加网关到上游共享密钥或内部签名，需要先更新 `api-gateway`、`notification` 契约和对应测试闭环。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `NotificationType` | `SYSTEM`、`AUDIT`、`WHITELIST`、`EXAM`、`CONTENT`、`RESOURCE`、`ATTENDANCE`、`COMMUNITY`、`ACTIVITY`、`OPS` | 通知类型，用于列表过滤和前端展示。 |
| `NotificationChannel` | `IN_APP`、`EMAIL`、`SMS`、`QQ`、`OOPZ`、`GAME` | 通知渠道。P0 写接口只允许 `IN_APP`。 |
| `RecipientStatus` | `UNREAD`、`READ`、`ARCHIVED` | 当前用户看到的收件人状态。归档是终态，默认从普通列表隐藏。 |
| `DeliveryStatus` | `PENDING`、`DELIVERED`、`FAILED`、`CANCELED` | 单个收件人的站内投递状态。P0 成功写入收件人记录即为 `DELIVERED`。 |
| `TemplateStatus` | `ENABLED`、`DISABLED` | 模板状态。禁用模板不可用于投递。 |
| `NotificationAuditResult` | `SUCCESS`、`FAILED` | notification 审计执行结果。 |

`sourceModule` 使用模块英文名，例如 `auth`、`profile`、`notification`、`content`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、`admin`、`ops-control`、`external-node-executor`。P0 允许 `sourceModule` 为空，但后台创建通知时建议提供来源，便于后续追踪。

### 通用对象

#### NotificationRecipientView

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `notificationId` | string | 是 | 通知 ID。 |
| `recipientUserId` | string | 是 | 当前用户 ID。当前用户接口中固定为认证用户。 |
| `recipientDisplayNameSnapshot` | string | 是 | 收件人展示名快照。 |
| `title` | string | 是 | 通知标题，2 到 80 位。 |
| `body` | string | 是 | 通知正文，1 到 2000 位。 |
| `type` | string | 是 | `NotificationType`。 |
| `sourceModule` | string 或 null | 是 | 来源模块。 |
| `sourceId` | string 或 null | 是 | 来源业务 ID。 |
| `riskLevel` | string | 是 | 公共风险等级，默认 `LOW`。 |
| `actionUrl` | string 或 null | 是 | 站内操作链接，只允许 http、https 或站内路径，最多 500 位。 |
| `status` | string | 是 | `RecipientStatus`。 |
| `deliveryStatus` | string | 是 | `DeliveryStatus`。 |
| `failureReason` | string 或 null | 是 | 投递失败原因。成功投递为 `null`。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `readAt` | string 或 null | 是 | 已读时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |
| `expiresAt` | string 或 null | 是 | 过期时间。过期通知默认不出现在普通列表。 |

#### AdminNotificationMessage

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `notificationId` | string | 是 | 通知 ID。 |
| `title` | string | 是 | 通知标题。 |
| `body` | string | 是 | 通知正文。 |
| `type` | string | 是 | 通知类型。 |
| `channels` | string[] | 是 | 通知渠道，P0 固定只包含 `IN_APP`。 |
| `sourceModule` | string 或 null | 是 | 来源模块。 |
| `sourceId` | string 或 null | 是 | 来源业务 ID。 |
| `riskLevel` | string | 是 | 风险等级。 |
| `actionUrl` | string 或 null | 是 | 操作链接。 |
| `templateId` | string 或 null | 是 | 来源模板 ID。直接创建时为 `null`。 |
| `templateCode` | string 或 null | 是 | 来源模板编码。直接创建时为 `null`。 |
| `variables` | object 或 null | 是 | 模板变量快照。直接创建时为 `null`。 |
| `recipientTotal` | integer | 是 | 收件人总数。 |
| `deliveredTotal` | integer | 是 | 成功投递数量。 |
| `failedTotal` | integer | 是 | 失败数量。 |
| `recipients` | AdminNotificationRecipient[] | 是 | 收件人投递摘要。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `expiresAt` | string 或 null | 是 | 过期时间。 |

#### AdminNotificationRecipient

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `recipientUserId` | string | 是 | 收件人用户 ID。 |
| `recipientDisplayNameSnapshot` | string | 是 | 收件人展示名快照。 |
| `status` | string | 是 | `RecipientStatus`。 |
| `deliveryStatus` | string | 是 | `DeliveryStatus`。 |
| `failureReason` | string 或 null | 是 | 失败原因。 |
| `readAt` | string 或 null | 是 | 已读时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |
| `deliveredAt` | string 或 null | 是 | 站内投递成功时间。 |

#### NotificationTemplate

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `templateId` | string | 是 | 模板 ID。 |
| `code` | string | 是 | 模板编码，3 到 64 位，只允许大写字母、数字、下划线和点号，同一未删除模板中唯一。 |
| `name` | string | 是 | 模板名称，2 到 50 位。 |
| `titleTemplate` | string | 是 | 标题模板，2 到 120 位。变量格式为 `${variableName}`。 |
| `bodyTemplate` | string | 是 | 正文模板，1 到 3000 位。变量格式为 `${variableName}`。 |
| `variableDefinitions` | TemplateVariableDefinition[] | 是 | 变量定义。 |
| `type` | string | 是 | 默认通知类型。 |
| `channels` | string[] | 是 | 模板允许渠道，P0 只能包含 `IN_APP`。 |
| `status` | string | 是 | `TemplateStatus`。 |
| `version` | integer | 是 | 模板版本，从 `1` 开始，修改模板时递增。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `disabledAt` | string 或 null | 是 | 禁用时间。 |

#### TemplateVariableDefinition

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `name` | string | 是 | 变量名，1 到 40 位，只允许字母、数字和下划线，必须以字母开头。 |
| `required` | boolean | 是 | 是否必填。 |
| `description` | string 或 null | 是 | 变量说明，最多 120 位。 |
| `example` | string 或 null | 是 | 示例值，最多 120 位。 |

#### NotificationAuditLog

审计字段继承公共契约，至少返回 `id`、`requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`sourceIp`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason` 和 `createdAt`。允许补充 `notificationId`、`templateId`、`recipientUserIds`、`deliveryStatus`、`templateVersion`、`idempotencyKey` 和 `sourceModule`。通知审计日志不得通过 notification API 删除。

`actorPermissions` 必须来自已解析认证上下文，不能从浏览器请求体读取。`sourceIp` 来自请求上下文，优先使用网关传入的安全来源摘要，缺失时使用服务端看到的远端地址或 `unknown`。`riskLevel` 必须使用实际操作风险等级，不能在审计响应中固定为 `MEDIUM`。

`paramsSummary`、`beforeState` 和 `afterState` 只能保存安全摘要。创建通知可以记录 `sourceModule`、`sourceId`、`recipientTotal`、`channels`、`riskLevel`、`idempotencyKeyPresent` 和 `templateCode` 等字段；模板修改可以记录模板 ID、编码、版本和状态变化。审计摘要不得包含完整通知正文、完整模板正文、完整模板变量值、完整请求头、Authorization、token、外部渠道凭据、内部 URL、异常堆栈或前序服务私有字段。

### notification 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `43300` | 404 | 通知不存在，或当前用户无权访问该通知。 |
| `43301` | 404 | 通知模板不存在。 |
| `43302` | 404 | 通知收件人记录不存在。 |
| `43310` | 409 | 当前通知不属于当前用户或不可见。 |
| `43311` | 409 | 通知或收件人状态不允许当前操作。 |
| `43312` | 409 | 模板已禁用，不能用于投递。 |
| `43313` | 400 | 模板变量缺失或变量名非法。 |
| `43314` | 400 | 模板渲染失败。 |
| `43315` | 404 | 目标收件人不存在或状态不可投递。 |
| `43316` | 400 | 收件人数量非法。 |
| `43317` | 409 | 模板编码已存在。 |
| `43318` | 409 | 模板状态不允许当前操作。 |
| `46300` | 502 | auth 认证上下文或目标用户快照不可用。 |
| `46301` | 504 | auth 认证上下文或目标用户快照调用超时。 |
| `46302` | 502 | auth 返回的认证上下文或目标用户快照不兼容 notification 契约。 |
| `51300` | 500 | notification 内部错误。 |
| `51301` | 500 | notification 审计写入失败。 |
| `51302` | 500 | notification 投递写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误、幂等键冲突和通用服务端错误优先使用公共错误码。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 当前用户通知列表 | GET | `/api/v1/notifications/me` | 是 | 当前用户 | LOW |
| 当前用户未读数 | GET | `/api/v1/notifications/me/unread-count` | 是 | 当前用户 | LOW |
| 当前用户通知详情 | GET | `/api/v1/notifications/me/{notificationId}` | 是 | 当前用户 | LOW |
| 标记单条已读 | PATCH | `/api/v1/notifications/me/{notificationId}/read` | 是 | 当前用户 | LOW |
| 全部标记已读 | PATCH | `/api/v1/notifications/me/read-all` | 是 | 当前用户 | LOW |
| 归档单条通知 | PATCH | `/api/v1/notifications/me/{notificationId}/archive` | 是 | 当前用户 | LOW |
| 后台通知列表 | GET | `/api/v1/notifications/admin/messages` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台通知详情 | GET | `/api/v1/notifications/admin/messages/{notificationId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台创建站内通知 | POST | `/api/v1/notifications/admin/messages` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台按模板创建通知 | POST | `/api/v1/notifications/admin/messages/from-template` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 模板列表 | GET | `/api/v1/notifications/admin/templates` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 模板详情 | GET | `/api/v1/notifications/admin/templates/{templateId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 模板预览 | POST | `/api/v1/notifications/admin/templates/preview` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建模板 | POST | `/api/v1/notifications/admin/templates` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改模板 | PATCH | `/api/v1/notifications/admin/templates/{templateId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 禁用模板 | PATCH | `/api/v1/notifications/admin/templates/{templateId}/disable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 启用模板 | PATCH | `/api/v1/notifications/admin/templates/{templateId}/enable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 通知审计列表 | GET | `/api/v1/notifications/admin/messages/{notificationId}/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| notification 自检摘要 | GET | `/api/v1/notifications/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

### 当前用户接口

#### 当前用户通知列表

`GET /api/v1/notifications/me`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `status` | string | 否 | `UNREAD`、`READ`、`ARCHIVED`。不传时只返回未归档且未过期通知。 |
| `type` | string | 否 | 任一 `NotificationType`。 |
| `sourceModule` | string | 否 | 来源模块，最多 40 位。 |
| `includeExpired` | boolean | 否 | 默认 `false`。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`、`readAt_desc`。默认 `createdAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `NotificationRecipientView[]`。

业务规则：只能返回当前认证用户的收件人记录。默认列表排除 `ARCHIVED` 和已过期通知。归档通知只有 `status=ARCHIVED` 时返回。列表不得返回其他收件人、模板变量中的敏感字段、后台 `reason`、审计参数摘要或其他用户快照。

失败降级：auth 上下文不可用、超时或不兼容时不得返回旧缓存通知，必须返回 `46300`、`46301` 或 `46302`。

#### 当前用户未读数

`GET /api/v1/notifications/me/unread-count`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "unreadCount": 3
  }
}
```

业务规则：只统计当前用户 `UNREAD`、未归档且未过期的站内通知。读取失败时不得伪造 `0`。auth 失败时返回认证或依赖错误。

#### 当前用户通知详情

`GET /api/v1/notifications/me/{notificationId}`

成功响应 HTTP `200`，`data` 为 `NotificationRecipientView`。

业务规则：只能读取当前用户自己的收件人记录。通知不存在、已不属于当前用户或收件人记录不存在时返回 `43300`，不得暴露该通知是否属于其他用户。已归档通知允许通过详情接口读取，状态返回 `ARCHIVED`。

#### 标记单条已读

`PATCH /api/v1/notifications/me/{notificationId}/read`

请求体为空。成功响应 HTTP `200`，`data` 为更新后的 `NotificationRecipientView`。

业务规则：只允许当前用户标记自己的未归档通知。`UNREAD` 流转为 `READ` 并写入 `readAt`。重复标记已读返回成功，保持 `readAt` 不变，不重复写审计。`ARCHIVED` 通知返回 `43311`。

幂等规则：重复调用同一已读通知保持幂等。

#### 全部标记已读

`PATCH /api/v1/notifications/me/read-all`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `type` | string | 否 | 只标记指定类型。 |
| `sourceModule` | string | 否 | 只标记指定来源模块。 |

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "updatedCount": 5
  }
}
```

业务规则：只标记当前用户未归档、未过期且当前为 `UNREAD` 的通知。没有可更新通知时返回 `updatedCount: 0`，保持幂等。

#### 归档单条通知

`PATCH /api/v1/notifications/me/{notificationId}/archive`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 否 | 当前用户归档原因，最多 200 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `NotificationRecipientView`。

业务规则：只允许当前用户归档自己的通知。归档后状态为 `ARCHIVED`，写入 `archivedAt`，默认不再出现在普通列表，未读数不再统计该通知。重复归档返回成功，保持幂等，不重复写审计。

### 后台通知接口

#### 后台通知列表

`GET /api/v1/notifications/admin/messages`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配标题、正文、来源业务 ID 或收件人展示名，最多 80 位。 |
| `type` | string | 否 | 任一 `NotificationType`。 |
| `sourceModule` | string | 否 | 来源模块。 |
| `recipientUserId` | string | 否 | 收件人用户 ID。 |
| `deliveryStatus` | string | 否 | 任一 `DeliveryStatus`。 |
| `createdBy` | string | 否 | 创建者用户 ID。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`、`recipientTotal_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `AdminNotificationMessage[]`。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。未登录返回 `41000`。

#### 后台通知详情

`GET /api/v1/notifications/admin/messages/{notificationId}`

成功响应 HTTP `200`，`data` 为 `AdminNotificationMessage`。通知不存在返回 `43300`。

#### 后台创建站内通知

`POST /api/v1/notifications/admin/messages`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `recipientUserIds` | string[] | 是 | 1 到 200 个用户 ID，去重后仍需至少 1 个。 |
| `title` | string | 是 | 2 到 80 位。 |
| `body` | string | 是 | 1 到 2000 位。 |
| `type` | string | 是 | 任一 `NotificationType`。 |
| `channels` | string[] | 否 | 默认 `["IN_APP"]`。P0 只能为 `["IN_APP"]`。 |
| `sourceModule` | string 或 null | 否 | 来源模块，最多 40 位。 |
| `sourceId` | string 或 null | 否 | 来源业务 ID，最多 80 位。 |
| `riskLevel` | string | 否 | 默认 `LOW`。 |
| `actionUrl` | string 或 null | 否 | http、https 或站内路径，最多 500 位。 |
| `expiresAt` | string 或 null | 否 | 必须晚于当前时间。 |
| `reason` | string | 是 | 1 到 200 位，创建原因。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `AdminNotificationMessage`。

业务规则：创建前必须通过 auth 适配层解析全部收件人快照。任一收件人不存在、状态不可投递、auth 失败或审计失败时整笔请求失败并回滚。收件人 ID 去重，重复 ID 只创建一条收件人记录。创建成功后每个收件人的 `deliveryStatus` 为 `DELIVERED`，`status` 为 `UNREAD`。

幂等规则：同一创建者、同一 `idempotencyKey`、同一请求体重复提交时返回同一个通知结果，不重复投递、不重复增加未读数、不重复写创建审计。相同幂等键搭配不同请求体返回 `43002`。

审计要求：成功写入 `NOTIFICATION_MESSAGE_CREATED`，记录操作者、来源模块、来源业务 ID、收件人数量、风险等级、原因和请求编号。失败不写成功审计，可写失败审计或安全日志。

#### 后台按模板创建通知

`POST /api/v1/notifications/admin/messages/from-template`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `templateCode` | string | 是 | 已启用模板编码。 |
| `recipientUserIds` | string[] | 是 | 1 到 200 个用户 ID。 |
| `variables` | object | 是 | 模板变量键值。值统一按字符串渲染，单个值最多 500 位。 |
| `channels` | string[] | 否 | 默认使用模板渠道。P0 只能为 `["IN_APP"]`。 |
| `sourceModule` | string 或 null | 否 | 来源模块，未传时使用模板默认来源或 `notification`。 |
| `sourceId` | string 或 null | 否 | 来源业务 ID。 |
| `riskLevel` | string | 否 | 默认 `LOW`。 |
| `actionUrl` | string 或 null | 否 | http、https 或站内路径。 |
| `expiresAt` | string 或 null | 否 | 必须晚于当前时间。 |
| `reason` | string | 是 | 1 到 200 位，投递原因。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `AdminNotificationMessage`。

业务规则：模板不存在返回 `43301`。模板已禁用返回 `43312`。缺少必填变量返回 `43313`。模板渲染后标题或正文仍包含未解析变量，或渲染结果超出字段长度，返回 `43314`。渲染成功后按后台创建站内通知的规则投递。保存的通知必须记录 `templateId`、`templateCode`、模板 `version` 和变量快照。

### 模板接口

#### 模板列表

`GET /api/v1/notifications/admin/templates`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配编码或名称，最多 80 位。 |
| `status` | string | 否 | `ENABLED` 或 `DISABLED`。 |
| `type` | string | 否 | 任一 `NotificationType`。 |
| `sort` | string | 否 | 允许 `updatedAt_desc`、`createdAt_desc`、`code_asc`。 |

成功响应 HTTP `200`，分页 `items` 为 `NotificationTemplate[]`。

#### 模板详情

`GET /api/v1/notifications/admin/templates/{templateId}`

成功响应 HTTP `200`，`data` 为 `NotificationTemplate`。模板不存在返回 `43301`。

#### 模板预览

`POST /api/v1/notifications/admin/templates/preview`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `templateCode` | string | 是 | 已存在模板编码。允许预览启用或禁用模板。 |
| `variables` | object | 是 | 模板变量键值。值统一按字符串渲染，单个值最多 500 位。 |

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "templateId": "tpl_xxx",
    "templateCode": "WHITELIST_APPROVED",
    "templateVersion": 2,
    "templateStatus": "ENABLED",
    "sendable": true,
    "title": "白名单审核已通过",
    "body": "Steve，你的白名单审核已通过。",
    "variables": {
      "playerName": "Steve"
    },
    "createdNotification": false
  }
}
```

业务规则：模板预览只渲染模板，不创建通知、不创建收件人、不更新未读数、不写投递审计、不修改模板版本。模板不存在返回 `43301`。缺少必填变量、变量名非法或提交未定义变量返回 `43313`。渲染后仍存在未解析变量，或标题正文超出投递字段长度，返回 `43314`。禁用模板允许预览，但 `sendable` 必须为 `false`，按模板投递仍返回 `43312`。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。未登录返回 `41000`。

#### 创建模板

`POST /api/v1/notifications/admin/templates`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `code` | string | 是 | 3 到 64 位，只允许大写字母、数字、下划线和点号。 |
| `name` | string | 是 | 2 到 50 位。 |
| `titleTemplate` | string | 是 | 2 到 120 位。 |
| `bodyTemplate` | string | 是 | 1 到 3000 位。 |
| `variableDefinitions` | array | 是 | 最多 30 个变量定义，变量名不能重复。 |
| `type` | string | 是 | 任一 `NotificationType`。 |
| `channels` | string[] | 否 | 默认 `["IN_APP"]`。P0 只能包含 `IN_APP`。 |
| `reason` | string | 是 | 1 到 200 位，创建原因。 |
| `idempotencyKey` | string | 否 | 创建重试幂等键，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `NotificationTemplate`。

业务规则：模板编码唯一。模板必须至少引用一个已定义变量，或明确允许无变量模板。模板内容中出现的变量必须全部在 `variableDefinitions` 中定义。编码重复返回 `43317`。

幂等规则：同一创建者、同一 `idempotencyKey`、同一请求体重复提交时返回同一个模板。相同幂等键搭配不同请求体返回 `43002`。

审计要求：成功写入 `NOTIFICATION_TEMPLATE_CREATED`。

#### 修改模板

`PATCH /api/v1/notifications/admin/templates/{templateId}`

请求字段同创建模板，除 `reason` 必填外其余字段按需修改。`code` 可以修改，但仍必须唯一。

成功响应 HTTP `200`，`data` 为更新后的 `NotificationTemplate`。

业务规则：模板不存在返回 `43301`。模板修改成功后 `version` 加一，已创建通知的模板快照不受影响。字段非法或模板变量不一致返回 `40001` 或 `43313`。编码冲突返回 `43317`。

审计要求：成功写入 `NOTIFICATION_TEMPLATE_UPDATED`，记录变更前后摘要和原因。审计失败时不得改变模板。

#### 禁用模板

`PATCH /api/v1/notifications/admin/templates/{templateId}/disable`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，禁用原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `NotificationTemplate`。

业务规则：重复禁用已禁用模板返回成功，保持幂等，不重复写审计。禁用后按模板投递返回 `43312`。

审计要求：首次禁用写入 `NOTIFICATION_TEMPLATE_DISABLED`。

#### 启用模板

`PATCH /api/v1/notifications/admin/templates/{templateId}/enable`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，启用原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `NotificationTemplate`。

业务规则：重复启用已启用模板返回成功，保持幂等，不重复写审计。启用前仍需校验模板变量定义和模板内容一致，失败返回 `43313` 或 `43314`。

审计要求：首次启用写入 `NOTIFICATION_TEMPLATE_ENABLED`。

### 审计接口

#### 通知审计列表

`GET /api/v1/notifications/admin/messages/{notificationId}/audit-logs`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |

成功响应 HTTP `200`，分页 `items` 为 `NotificationAuditLog[]`。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 可读后台通知列表和详情，但不能读取审计列表。通知不存在返回 `43300`。审计日志不得通过 notification API 删除。

### 运维自检接口

#### notification 自检摘要

`GET /api/v1/notifications/admin/ops/summary`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "notification",
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "messagesTotal": 12,
    "templatesTotal": 4,
    "auditsTotal": 20,
    "recipientsTotal": 18,
    "unreadTotal": 6,
    "archivedTotal": 1,
    "deliveredTotal": 18,
    "failedTotal": 0,
    "pendingExternalDeliveries": 0,
    "idempotencyRecordsTotal": 3,
    "idempotencyRetentionHours": 24,
    "auditCompletenessMode": "SAFE_SUMMARY",
    "lastAuditAt": "2026-05-22T00:00:00Z",
    "warnings": [
      "P0_IN_MEMORY_STORAGE",
      "P0_AUTH_STUB"
    ]
  }
}
```

业务规则：自检摘要用于后台确认 notification 当前运行模式、数据规模、投递状态、审计摘要模式、幂等记录数量和生产化缺口。P0 `storageMode` 固定为 `IN_MEMORY`，`authMode` 固定为 `TEST_STUB`，`pendingExternalDeliveries` 固定为 `0`，`idempotencyRetentionHours` 固定为 `24`，`auditCompletenessMode` 固定为 `SAFE_SUMMARY`。摘要读取前必须先清理已过期幂等记录，`idempotencyRecordsTotal` 只统计仍在有效期内的记录。摘要不得返回 token、请求头、用户敏感字段、通知正文、模板正文、模板变量、幂等响应快照或审计原因。数据读取失败返回 `51300`，不得伪造健康。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。`HELPER`、`USER` 返回 `42001`。未登录返回 `41000`。

### 状态、幂等和并发

收件人状态流转为 `UNREAD` 到 `READ`，`UNREAD` 或 `READ` 到 `ARCHIVED`。`ARCHIVED` 为当前用户视角终态，不允许再标记已读。重复已读和重复归档保持幂等。

投递状态 P0 成功写入收件人记录即为 `DELIVERED`。`PENDING`、`FAILED` 和 `CANCELED` 保留给后续异步渠道和失败补偿。P0 任何创建接口必须全有或全无，不允许返回部分成功的 `FAILED` 收件人记录。

创建通知、按模板创建通知和创建模板支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一个结果。相同幂等键搭配不同请求体返回 `43002`。请求体指纹必须基于结构化 JSON 规范化结果，嵌套对象按字段名递归排序，数组保留顺序，不能依赖浏览器字段顺序或 Java `Map.toString()`。

幂等键有效期为 24 小时。有效期内必须保存请求指纹、响应快照、创建时间和过期时间。过期记录必须被忽略，并在后续创建请求或自检摘要读取时被机会式清理；同一操作者在旧记录过期后复用同一幂等键时，按新请求处理。后续持久化实现必须用数据库唯一约束、事务和 TTL 清理或等效机制保持同一口径。

并发创建同一幂等键时只能创建一条通知或模板。并发标记已读、全部已读和归档时必须以服务端当前状态为准，不得重复增加未读数、不得把归档通知重新变为已读。

### 审计要求

必须审计的动作包括后台创建站内通知、后台按模板创建通知、创建模板、修改模板、禁用模板、启用模板、当前用户归档通知、批量标记已读、投递失败回滚和审计写入失败补偿记录。

后台写操作必须记录 `reason`。审计字段继承公共契约，必须记录 actor、actor 权限摘要、来源 IP、目标、动作、风险等级、请求编号、参数安全摘要、操作前安全摘要、操作后安全摘要、结果和失败原因。审计写入失败时，后台写操作和模板写操作不得假装成功，必须返回 `51301` 或 `51300`，并保持业务数据不变。当前用户已读操作不强制写审计，归档操作建议写低风险审计或用户行为日志。

生产化硬化验收还必须满足：审计响应不再返回空的 `actorPermissions`、`sourceIp` 和 `paramsSummary`；风险等级按真实操作写入；幂等记录 24 小时过期并可清理；自检摘要暴露幂等记录数量和审计摘要模式但不泄露响应快照；测试控制、测试桩和边界扫描不得引入真实外部渠道发送、节点运维、文件管理、批量删除或前序服务内部实现依赖。

### 失败降级

当前用户通知列表、详情、未读数、已读和归档都依赖认证上下文。认证失败、会话过期、用户禁用、auth 不可用或 auth 超时时，不得返回旧通知、旧未读数或伪造成功。

后台创建通知依赖目标用户快照、审计和本地投递写入。任一依赖失败时必须全量回滚，不得产生半通知、半收件人或错误未读数。

模板渲染失败时不得创建通知。禁用模板不得用于投递。外部渠道在 P0 不真实发送，提交非 `IN_APP` 渠道时返回字段校验失败，不进入投递流程。

### 验收口径

`notification` API 文档按 `docs/contracts-notification.md` 独立存在，并由 `.local-docs/tests-notification.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级和审计要求。

`notification` 完成时必须满足以下条件：全部接口按本文档实现；当前用户接口只能访问当前用户自己的通知；未读数准确且失败时不伪造 0；后台接口按角色限制；创建通知和模板写操作全有或全无；模板变量校验、模板预览和渲染失败可测试；自检摘要能暴露当前运行模式、审计摘要模式和幂等记录数量但不泄露敏感数据；auth 适配不直接读取 auth 实现；受保护接口同时支持网关可信认证上下文和旧 Bearer 兼容路径；审计 actor、权限判断、当前用户隔离、未读数、目标收件人快照、actor 权限摘要、来源 IP、安全参数摘要和幂等过期语义均以服务端解析结果为准；`.local-docs/tests-notification.md` 中全部测试用例都有对应自动化验证；未实现时自动化测试必须先失败；实现后 notification 全部测试通过；api-gateway、auth 和 profile 前序服务回归测试通过；没有修改前序服务稳定接口。

## 北冥官网 content API 契约

来源：`docs/contracts-content.md`

版本：0.3

### 文档定位

本文档是 `content` 微服务的正式 API 契约。后续 `server-status`、`resource`、`admin`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar` 和 `changelog` 只能通过本文档定义的接口适配官网内容，不能直接读取或修改 `content` 数据库。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、能力点、审计字段、风险等级、通用状态模型和通用错误码均以公共契约为准。本文档只补充 `content` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

`content` 适配 `auth`、`profile` 和 `notification`，不要求前序服务反向适配 `content`。`content` 通过后端入口传入的认证上下文、`/api/v1/auth/me`、`/api/v1/auth/session/verify` 或测试环境 auth stub 读取当前用户、角色和能力点。成员作品只保存来自 `profile` 的公开快照。审核结果需要通知时，只能调用 `notification` 的正式投递接口或受控适配层，不能自建通知主数据、未读数或模板系统。

### 职责边界

`content` 负责官网首页配置、公告、文章、页面内容、摄影作品、成员作品快照、服务器进度、成就、里程碑、专题页、内容分类、标签和 SEO 配置。

`content` 不负责注册、登录、会话、角色、能力点、邀请码、成员主数据、成员组主数据、站内通知主数据、玩家可见 Minecraft 实时状态、资源下载、Cloudreve 分享、社区帖子、活动报名、日历事件、后台聚合入口或真实服务器运维操作。

首页可以展示服务器状态入口和资源入口，但只保存展示位、标题、说明和跳转配置。真实在线人数、MOTD、线路状态、资源下载链接和 Cloudreve 分享链接分别归后续 `server-status` 和 `resource`。

### 数据归属

`content` 拥有以下主数据：首页草稿配置、首页已发布配置、内容条目、分类、标签、专题、SEO 配置、成员作品快照、幂等记录、内容审计日志和内容发布记录。

成员作品快照字段只用于展示，至少包括 `memberId`、`displayName`、`avatarUrl`、`minecraftId`、`groupName` 和 `profileSnapshotAt`。快照不是 `profile` 主数据，不能用于成员资格判断，不能反写 `profile`。

通知调用结果只能作为审计摘要或业务操作结果摘要保存。`content` 不保存收件人读取状态、不计算未读数、不维护通知模板。

### 基础路径与认证

公开接口使用 `/api/v1/content` 前缀，不要求登录，只能返回公开可见、已发布、未下架、未归档、未软删除且处于可见时间范围内的数据。公开接口不得返回后台备注、审核意见、审计字段、作者用户敏感字段、幂等键、通知结果、内部快照来源和未发布配置。

后台接口使用 `/api/v1/content/admin` 前缀，全部要求登录。后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`。后台写操作要求 `ADMIN` 或 `OWNER`，必须携带 `reason` 并写入审计。`HELPER` 可以读取待审核内容和预览，但不能创建、修改、审核、发布、下架、归档或删除。

### auth 兼容契约

当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。

后台写操作的 `createdBy`、`updatedBy`、`reviewedBy`、`publishedBy`、`offlineBy`、`archivedBy` 和 `deletedBy` 均来自服务端认证上下文，浏览器请求体传入同名字段时必须忽略或返回字段校验失败。

auth 上下文不可用返回 `46420`，auth 调用超时返回 `46421`，auth 返回字段缺失或枚举不兼容返回 `46422`。`content` 不能导入 auth 的内存存储、实体、Repository 或测试种子实现。

### profile 兼容契约

创建或修改 `MEMBER_WORK` 类型内容时，如果请求包含 `memberId`，`content` 必须通过 profile 适配层读取成员公开快照。profile 返回成员不存在或不可公开时返回 `46400`。profile 调用超时返回 `46401`。profile 返回字段缺失或枚举不兼容返回 `46402`。

客户端不得通过请求体伪造成员展示名、头像、Minecraft ID 或成员组作为可信字段。实现可以允许请求体携带这些字段作为编辑草稿参考，但保存的可信快照必须来自 profile 适配层。profile 不可用时不得创建新的可信成员作品；已发布内容公开读取可以继续返回已保存快照，并在后台详情中标记快照来源时间。

### notification 兼容契约

审核通过、拒绝、要求修改、发布、下架、归档和软删除都可以触发通知。本文档规定审核通过、拒绝、要求修改必须通知内容作者；作者为空时跳过通知并在审计中记录 `NO_AUTHOR_TO_NOTIFY`。发布、下架、归档和软删除的通知为辅助提醒，通知失败时主流程可以成功，但必须在审计中记录 `notificationStatus: FAILED` 和失败原因。

强制通知失败返回 `46410` 或 `46411`，业务状态不得变化。辅助通知失败不得伪造通知成功，也不得影响公开读取。`content` 不能自建通知表、未读数、模板和投递记录。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `ContentType` | `ANNOUNCEMENT`、`ARTICLE`、`PAGE`、`PHOTO`、`MEMBER_WORK`、`PROGRESS`、`ACHIEVEMENT`、`MILESTONE`、`TOPIC_ENTRY` | 内容条目类型。专题主体由 `TopicPage` 管理，专题内条目可使用 `TOPIC_ENTRY`。 |
| `ContentStatus` | `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` | 继承公共状态模型。公开可见需要 `APPROVED` 且 `publishedAt` 不为空。 |
| `ContentVisibility` | `PUBLIC`、`MEMBER_ONLY`、`PRIVATE` | P0 公开接口只返回 `PUBLIC`。`MEMBER_ONLY` 保留给后续登录用户展示。 |
| `HomeSectionType` | `HERO`、`ANNOUNCEMENTS`、`FEATURED_ARTICLES`、`MEMBER_WORKS`、`MOMENTS`、`MILESTONES`、`TOPICS`、`SERVER_ENTRY`、`RESOURCE_ENTRY`、`CUSTOM_LINKS` | 首页区块类型。 |
| `ContentAuditResult` | `SUCCESS`、`FAILED` | content 审计执行结果。 |
| `SeoRobots` | `INDEX_FOLLOW`、`NOINDEX_FOLLOW`、`NOINDEX_NOFOLLOW` | SEO robots 策略。 |

### 通用对象

#### ContentItemSummary

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

#### ContentItemDetail

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

#### AdminContentItem

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

#### ContentItemVersion

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

#### ContentCategory

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

#### ContentTag

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `tagId` | string | 是 | 标签 ID。 |
| `name` | string | 是 | 标签名称，1 到 24 位，同一未归档标签中唯一。 |
| `slug` | string | 是 | 标签 slug。 |
| `archived` | boolean | 是 | 是否归档。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### MemberContentSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `memberId` | string | 是 | profile 成员 ID。 |
| `displayName` | string | 是 | 成员展示名快照。 |
| `avatarUrl` | string 或 null | 是 | 头像快照。 |
| `minecraftId` | string 或 null | 是 | Minecraft ID 快照。 |
| `groupName` | string 或 null | 是 | 成员组名称快照。 |
| `profileSnapshotAt` | string | 是 | 快照获取时间。 |

#### HomeContentView

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `homeConfigId` | string | 是 | 已发布首页配置 ID。 |
| `version` | integer | 是 | 已发布版本号。 |
| `sections` | HomeSection[] | 是 | 首页区块。 |
| `degraded` | boolean | 是 | 是否存在局部降级。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |
| `publishedAt` | string | 是 | 首页配置发布时间。 |
| `seo` | SeoPayload 或 null | 是 | 首页 SEO。 |

#### HomeSection

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

#### TopicPage

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

#### SeoPayload

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

#### SiteMapEntry

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `route` | string | 是 | 公开站内路径。 |
| `targetType` | string | 是 | `HOME`、`CONTENT` 或 `TOPIC`。 |
| `targetId` | string 或 null | 是 | 内容 ID 或专题 ID，首页为 `null`。 |
| `lastModifiedAt` | string | 是 | 最近更新时间，用于搜索引擎增量抓取。 |
| `changeFrequency` | string | 是 | `daily`、`weekly` 或 `monthly`。 |
| `priority` | number | 是 | 0 到 1，首页优先级最高。 |

#### ContentPreviewToken

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `contentId` | string | 是 | 预览目标内容 ID。 |
| `token` | string | 是 | 预览令牌，只返回一次，不写入审计明文。 |
| `previewUrl` | string | 是 | 可直接访问的预览接口路径。 |
| `expiresAt` | string | 是 | 令牌过期时间。 |
| `createdAt` | string | 是 | 创建时间。 |

#### ContentAuditLog

审计字段继承公共契约，允许补充 `contentId`、`homeConfigId`、`topicId`、`seoId`、`idempotencyKey`、`notificationStatus`、`profileSnapshotStatus`、`stateFrom`、`stateTo`、`version`、`sourceVersion` 和 `newVersion`。审计日志不得通过 content API 删除。

### content 错误码

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
| `43419` | 404 | 内容预览令牌不存在、过期或不匹配。 |
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

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开首页内容 | GET | `/api/v1/content/home` | 否 | 无 | LOW |
| 公开内容列表 | GET | `/api/v1/content/items` | 否 | 无 | LOW |
| 公开内容详情 | GET | `/api/v1/content/items/{contentId}` | 否 | 无 | LOW |
| 公开 slug 内容详情 | GET | `/api/v1/content/items/by-slug/{slug}` | 否 | 无 | LOW |
| 内容令牌预览 | GET | `/api/v1/content/items/{contentId}/preview` | 否 | 预览令牌 | LOW |
| 公开分类列表 | GET | `/api/v1/content/categories` | 否 | 无 | LOW |
| 公开标签列表 | GET | `/api/v1/content/tags` | 否 | 无 | LOW |
| 公开专题列表 | GET | `/api/v1/content/topics` | 否 | 无 | LOW |
| 公开专题详情 | GET | `/api/v1/content/topics/{topicId}` | 否 | 无 | LOW |
| 公开 slug 专题详情 | GET | `/api/v1/content/topics/by-slug/{slug}` | 否 | 无 | LOW |
| 公开 SEO 配置 | GET | `/api/v1/content/seo` | 否 | 无 | LOW |
| 公开站点地图 | GET | `/api/v1/content/seo/sitemap` | 否 | 无 | LOW |
| 后台内容列表 | GET | `/api/v1/content/admin/items` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台内容详情 | GET | `/api/v1/content/admin/items/{contentId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建内容 | POST | `/api/v1/content/admin/items` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改内容 | PATCH | `/api/v1/content/admin/items/{contentId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 创建内容预览令牌 | POST | `/api/v1/content/admin/items/{contentId}/preview-token` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
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

### 公开接口

#### 公开首页内容

`GET /api/v1/content/home`

成功响应 HTTP `200`，`data` 为 `HomeContentView`。

业务规则：只返回最近一次已发布首页配置。配置不存在时返回默认空首页视图，`sections` 为空数组，`degraded` 为 `true`，`degradeReasons` 包含 `NO_PUBLISHED_HOME_CONFIG`。首页引用的内容、专题、分类或标签不存在、未发布、不可公开、已下架、已归档、已软删除或不在可见时间范围内时，只跳过对应引用并标记对应区块降级。`SERVER_ENTRY` 和 `RESOURCE_ENTRY` 只能返回入口配置和说明，不能返回伪造服务器在线状态或资源下载链接。

降级规则：公开首页不得因为单个引用失效整页失败。存储读取失败或首页配置无法解析时返回 `51400` 或 `51402`，前端按首页区域降级。

#### 公开内容列表

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

#### 公开内容详情

`GET /api/v1/content/items/{contentId}`

成功响应 HTTP `200`，`data` 为 `ContentItemDetail`。

业务规则：只有公开可见内容可以访问。内容不存在返回 `43400`。内容存在但不可公开访问时返回 `43412`，实现也可以出于防枚举考虑返回 `43400`，但同一版本内必须保持一致并写入测试。

#### 公开 slug 内容详情

`GET /api/v1/content/items/by-slug/{slug}`

成功响应 HTTP `200`，`data` 为 `ContentItemDetail`。

业务规则：slug 必须匹配公开可见内容。slug 不存在返回 `43400`。slug 对应内容不可公开时返回 `43412` 或 `43400`，同一版本内保持一致。

#### 内容令牌预览

`GET /api/v1/content/items/{contentId}/preview`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `token` | string | 是 | 后台创建的预览令牌。 |

成功响应 HTTP `200`，`data` 为 `ContentItemDetail`。

业务规则：预览令牌只用于后台人员检查未公开内容展示效果，不要求登录，但必须携带有效令牌。令牌不存在、过期、目标内容不匹配或目标内容已归档、已软删除时返回 `43419`。预览响应可以返回 `DRAFT`、`PENDING_REVIEW`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE` 和 `APPROVED` 内容的正文，但不得返回 `adminNote`、`reviewOpinion`、审计字段、通知结果、幂等键或成员敏感字段。预览接口不得改变内容状态，不得写发布审计。

#### 公开分类列表

`GET /api/v1/content/categories`

成功响应 HTTP `200`，`data.items` 为未归档 `ContentCategory[]`，按 `sortOrder`、`name` 稳定排序。不得返回后台备注或审计字段。

#### 公开标签列表

`GET /api/v1/content/tags`

成功响应 HTTP `200`，`data.items` 为未归档 `ContentTag[]`，按 `name` 稳定排序。不得返回后台备注或审计字段。

#### 公开专题列表

`GET /api/v1/content/topics`

查询参数同公开内容列表的分页参数，额外支持 `keyword` 和 `sort`，允许排序为 `publishedAt_desc`、`updatedAt_desc`、`title_asc`。

成功响应 HTTP `200`，分页 `items` 为 `TopicPage[]` 的公开摘要字段。

业务规则：只返回 `status=APPROVED`、`publishedAt` 不为空、`visibility=PUBLIC`、未归档、未软删除且在可见时间范围内的专题。专题中的失效内容引用不应导致专题列表失败。

#### 公开专题详情

`GET /api/v1/content/topics/{topicId}`

成功响应 HTTP `200`，`data` 为 `TopicPage`。

业务规则：专题不存在返回 `43402`。专题不可公开访问返回 `43414` 或 `43402`，同一版本内保持一致。专题内引用内容不存在、下架或不可公开时从 `items` 局部跳过，并在后台详情中保留引用问题；公开响应不泄露后台原因。

#### 公开 slug 专题详情

`GET /api/v1/content/topics/by-slug/{slug}`

成功响应 HTTP `200`，`data` 为 `TopicPage`。业务规则同公开专题详情。

#### 公开 SEO 配置

`GET /api/v1/content/seo`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `route` | string | 是 | 站内路由，以 `/` 开头，最多 200 位。 |

成功响应 HTTP `200`，`data` 为 `SeoPayload`。

业务规则：命中启用 SEO 配置时返回该配置。未配置时返回模块默认 SEO，`seoId` 为 `null`，不得返回后台草稿、禁用配置或审核信息。route 非法返回 `40001`。

#### 公开站点地图

`GET /api/v1/content/seo/sitemap`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `type` | string | 否 | 允许 `CONTENT`、`TOPIC`、`HOME`，不传返回全部。 |

成功响应 HTTP `200`，`data.items` 为 `SiteMapEntry[]`。

业务规则：站点地图只返回公开可见的首页、内容和专题路径。草稿、待审核、已拒绝、需修改、下架、归档、软删除、`PRIVATE`、`MEMBER_ONLY`、未到可见时间或已超过可见时间的数据不得进入站点地图。站点地图不得返回正文、后台字段、审计字段、通知结果、幂等键或内部引用失败原因。`type` 非法返回 `40001`。首页配置不存在时可以只返回内容和专题，不得伪造不存在的业务路径。

### 后台内容接口

#### 后台内容列表

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

#### 后台内容详情

`GET /api/v1/content/admin/items/{contentId}`

成功响应 HTTP `200`，`data` 为 `AdminContentItem`。内容不存在返回 `43400`。

#### 创建内容

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

#### 修改内容

`PATCH /api/v1/content/admin/items/{contentId}`

请求字段同创建内容，除 `reason` 必填外其余字段按需修改。`type` 创建后不允许修改。

成功响应 HTTP `200`，`data` 为更新后的 `AdminContentItem`。

业务规则：不存在返回 `43400`。`ARCHIVED` 和 `DELETED` 不允许修改，返回 `43410`。已发布内容修改后仍保持原公开版本，除非实现选择单版本模型；P0 允许单版本模型，但修改已发布内容必须写审计，公开接口不得返回半更新对象。slug 冲突返回 `43411`。分类、标签和 profile 快照规则同创建。

审计要求：成功写入 `CONTENT_ITEM_UPDATED`，记录变更前后摘要和原因。审计失败不得改变内容。

#### 创建内容预览令牌

`POST /api/v1/content/admin/items/{contentId}/preview-token`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `expiresInMinutes` | integer | 否 | 默认 `30`，最小 `5`，最大 `1440`。 |
| `reason` | string | 是 | 1 到 200 位，创建预览令牌原因。 |

成功响应 HTTP `201`，`data` 为 `ContentPreviewToken`。

权限规则：只有 `ADMIN` 和 `OWNER` 可以创建预览令牌。`HELPER` 和 `USER` 返回 `42001`，未登录返回 `41000`。

业务规则：内容不存在返回 `43400`。`ARCHIVED` 和 `DELETED` 内容不能创建预览令牌，返回 `43410`。同一内容可以创建多个未过期令牌。令牌只用于读取预览，不允许修改内容状态，也不能绕过后台写权限。响应体返回令牌明文，审计日志不得保存令牌明文。

审计要求：成功写入 `CONTENT_ITEM_PREVIEW_TOKEN_CREATED`，记录目标内容、过期时间和原因。审计失败返回 `51401`，不得创建令牌。

#### 提交审核

`PATCH /api/v1/content/admin/items/{contentId}/submit-review`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，提交说明。 |

成功响应 HTTP `200`，`data` 为更新后的 `AdminContentItem`。

状态流转：`DRAFT`、`REJECTED`、`NEEDS_CHANGES` 可流转为 `PENDING_REVIEW`。其他状态返回 `43410`。重复提交已经处于 `PENDING_REVIEW` 的内容返回成功，保持幂等，不重复写审计。

审计要求：首次提交写入 `CONTENT_ITEM_SUBMITTED`。

#### 审核通过

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

#### 审核拒绝

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

#### 要求修改

`PATCH /api/v1/content/admin/items/{contentId}/request-changes`

请求字段同审核拒绝。

成功响应 HTTP `200`，`data` 为更新后的 `AdminContentItem`。

状态流转：`PENDING_REVIEW` 可流转为 `NEEDS_CHANGES`。存在 `authorUserId` 时必须投递要求修改通知。强制通知失败时状态保持 `PENDING_REVIEW`。

审计要求：成功写入 `CONTENT_ITEM_CHANGES_REQUESTED`。

#### 发布内容

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

#### 下架内容

`PATCH /api/v1/content/admin/items/{contentId}/offline`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，下架原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `AdminContentItem`。

状态流转：公开中的 `APPROVED` 内容可流转为 `OFFLINE`，并从公开接口消失。重复下架 `OFFLINE` 内容返回成功，保持幂等，不重复写审计。

#### 归档内容

`PATCH /api/v1/content/admin/items/{contentId}/archive`

请求字段同下架内容。

成功响应 HTTP `200`，`data` 为更新后的 `AdminContentItem`。

状态流转：`DRAFT`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE` 可流转为 `ARCHIVED`。`APPROVED` 且已公开内容必须先下架再归档。重复归档返回成功，保持幂等，不重复写审计。

#### 软删除内容

`PATCH /api/v1/content/admin/items/{contentId}/delete`

请求字段同下架内容。

成功响应 HTTP `200`，`data` 为更新后的 `AdminContentItem`。

业务规则：只做软删除，状态为 `DELETED`，写入 `deletedAt`。已公开内容必须先下架。重复软删除返回成功，保持幂等，不重复写审计。真实删除不在 P0 content API 中提供。

#### 内容版本列表

`GET /api/v1/content/admin/items/{contentId}/versions`

查询参数为公共分页参数，默认按 `version_desc` 排序。成功响应 HTTP `200`，分页 `items` 为 `ContentItemVersion[]`。只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 返回 `42001`。内容不存在返回 `43400`。

业务规则：内容创建时必须生成第 1 个版本。每次成功创建、修改、发布和恢复内容都必须生成新版本。提交审核、审核通过、审核拒绝、要求修改、下架、归档和软删除只改变状态流转，不强制生成内容版本，但必须写审计。公开接口不得返回版本历史。

#### 内容版本详情

`GET /api/v1/content/admin/items/{contentId}/versions/{version}`

成功响应 HTTP `200`，`data` 为 `ContentItemVersion`。内容不存在返回 `43400`。版本不存在返回 `43417`。只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 返回 `42001`。

#### 恢复内容版本

`PATCH /api/v1/content/admin/items/{contentId}/versions/{version}/restore`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，恢复原因。 |

成功响应 HTTP `200`，`data` 为恢复后的 `AdminContentItem`。

业务规则：恢复不会修改历史版本本身，而是把目标版本的可编辑字段复制到当前内容并生成一个新的当前版本。可编辑字段包括 `type`、`slug`、`title`、`summary`、`body`、`coverUrl`、`categoryId`、`tagIds`、`visibility`、`memberSnapshot`、`seo`、`adminNote`、`visibleFrom` 和 `visibleUntil`。恢复后状态必须为 `DRAFT`，`publishedAt`、`submittedAt`、`reviewedAt`、`reviewOpinion`、`notificationStatus` 和 `deletedAt` 清空，必须重新走审核和发布流程。当前内容为 `ARCHIVED` 或 `DELETED` 时返回 `43418`。目标版本不存在返回 `43417`。恢复时如果目标版本 slug 已被其他未删除内容占用，返回 `43411`。恢复成功写审计 `CONTENT_ITEM_VERSION_RESTORED`，审计中记录来源版本和新版本号。

#### 内容审计列表

`GET /api/v1/content/admin/items/{contentId}/audit-logs`

查询参数为公共分页参数。成功响应 HTTP `200`，分页 `items` 为 `ContentAuditLog[]`。只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 返回 `42001`。内容不存在返回 `43400`。审计日志不得通过 content API 删除。

### 后台首页配置接口

#### 后台首页配置详情

`GET /api/v1/content/admin/home`

成功响应 HTTP `200`，返回草稿配置、已发布配置、版本号和最近审计摘要。没有配置时返回空草稿和空发布配置，不返回错误。

#### 保存首页草稿配置

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

#### 首页配置预览

`POST /api/v1/content/admin/home/preview`

请求字段同保存首页草稿配置，`reason` 非必填。

成功响应 HTTP `200`，`data` 为预览渲染结果，必须包含引用校验结果和 `createdPublishedVersion: false`。

业务规则：预览不保存草稿、不发布配置、不写发布审计、不改变公开首页。

#### 发布首页配置

`PATCH /api/v1/content/admin/home/publish`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，发布原因。 |

成功响应 HTTP `200`，返回已发布首页配置。

业务规则：把当前草稿发布为新的公开版本。没有草稿返回 `43404`。重复发布未变化草稿返回成功，保持幂等，不重复创建版本。发布后公开首页只读取已发布版本。

#### 回滚首页配置

`PATCH /api/v1/content/admin/home/rollback`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `version` | integer | 是 | 要回滚到的已发布版本。 |
| `reason` | string | 是 | 1 到 200 位，回滚原因。 |

成功响应 HTTP `200`，返回回滚后的已发布配置。目标版本不存在返回 `43404`。回滚必须写审计。

### 后台分类接口

#### 后台分类列表

`GET /api/v1/content/admin/categories`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `includeArchived` | boolean | 否 | 默认 `true`，后台可查看归档分类。 |

成功响应 HTTP `200`，`data.items` 为 `ContentCategory[]`，按 `sortOrder`、`name` 稳定排序。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。未登录返回 `41000`。

业务规则：后台分类列表可以返回归档分类，但不得返回审计原因、幂等键或内部错误摘要。

#### 创建分类

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

#### 修改分类

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

#### 归档分类

`PATCH /api/v1/content/admin/categories/{categoryId}/archive`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，归档原因。 |

成功响应 HTTP `200`，`data` 为归档后的 `ContentCategory`。

业务规则：分类不存在返回 `43401`。仍被未归档、未软删除内容引用时返回 `43415`。重复归档已归档分类返回成功，保持幂等，不重复写审计。归档后公开分类列表不得返回该分类。

审计要求：首次归档写入 `CONTENT_CATEGORY_ARCHIVED`。

### 后台标签接口

#### 后台标签列表

`GET /api/v1/content/admin/tags`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `includeArchived` | boolean | 否 | 默认 `true`，后台可查看归档标签。 |

成功响应 HTTP `200`，`data.items` 为 `ContentTag[]`，按 `name` 稳定排序。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。未登录返回 `41000`。

#### 创建标签

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

#### 修改标签

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

#### 归档标签

`PATCH /api/v1/content/admin/tags/{tagId}/archive`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，归档原因。 |

成功响应 HTTP `200`，`data` 为归档后的 `ContentTag`。

业务规则：标签不存在返回 `43405`。仍被未归档、未软删除内容引用时返回 `43415`。重复归档已归档标签返回成功，保持幂等，不重复写审计。归档后公开标签列表不得返回该标签。

审计要求：首次归档写入 `CONTENT_TAG_ARCHIVED`。

### 后台专题接口

#### 后台专题列表

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

#### 后台专题详情

`GET /api/v1/content/admin/topics/{topicId}`

成功响应 HTTP `200`，`data` 为 `TopicPage`，允许包含后台引用校验摘要。

业务规则：专题不存在返回 `43402`。后台详情不得返回幂等键或审计参数摘要。

#### 创建专题

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

#### 修改专题

`PATCH /api/v1/content/admin/topics/{topicId}`

请求字段同创建专题，除 `reason` 必填外其余字段按需修改。

成功响应 HTTP `200`，`data` 为更新后的 `TopicPage`。

业务规则：专题不存在返回 `43402`。`ARCHIVED` 和 `DELETED` 专题不允许修改，返回 `43414`。slug 冲突返回 `43411`。已发布专题修改后必须保证公开接口不返回半更新状态；P0 可以采用单版本模型，但必须写审计。

审计要求：成功写入 `CONTENT_TOPIC_UPDATED`，记录变更前后摘要和原因。审计失败时不得改变专题。

#### 发布专题

`PATCH /api/v1/content/admin/topics/{topicId}/publish`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，发布原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `TopicPage`。

状态流转：`DRAFT` 和 `OFFLINE` 可发布为 `APPROVED`，并写入或更新 `publishedAt`。重复发布已 `APPROVED` 专题返回成功，保持幂等，不重复写审计。`ARCHIVED` 和 `DELETED` 返回 `43414`。

审计要求：首次发布写入 `CONTENT_TOPIC_PUBLISHED`。

#### 下架专题

`PATCH /api/v1/content/admin/topics/{topicId}/offline`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，下架原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `TopicPage`。

状态流转：`APPROVED` 可流转为 `OFFLINE`，并从公开专题接口消失。重复下架 `OFFLINE` 专题返回成功，保持幂等，不重复写审计。`DRAFT`、`ARCHIVED` 和 `DELETED` 返回 `43414`。

审计要求：首次下架写入 `CONTENT_TOPIC_OFFLINED`。

#### 归档专题

`PATCH /api/v1/content/admin/topics/{topicId}/archive`

请求字段同下架专题。

成功响应 HTTP `200`，`data` 为更新后的 `TopicPage`。

状态流转：`DRAFT` 和 `OFFLINE` 可流转为 `ARCHIVED`。已公开 `APPROVED` 专题必须先下架再归档，直接归档返回 `43414`。重复归档返回成功，保持幂等，不重复写审计。

审计要求：首次归档写入 `CONTENT_TOPIC_ARCHIVED`。

#### 软删除专题

`PATCH /api/v1/content/admin/topics/{topicId}/delete`

请求字段同下架专题。

成功响应 HTTP `200`，`data` 为更新后的 `TopicPage`。

业务规则：只做软删除，状态为 `DELETED`。已公开 `APPROVED` 专题必须先下架再软删除，直接删除返回 `43414`。重复软删除返回成功，保持幂等，不重复写审计。真实删除不在 P0 content API 中提供。

审计要求：首次软删除写入 `CONTENT_TOPIC_DELETED`。

### 后台 SEO 接口

#### 后台 SEO 列表

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

#### 后台 SEO 详情

`GET /api/v1/content/admin/seo/{seoId}`

成功响应 HTTP `200`，`data` 为 `SeoPayload`。

业务规则：SEO 配置不存在返回 `43403`。后台详情不得返回审计参数摘要或幂等键。

#### 保存路由 SEO

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

#### 禁用路由 SEO

`PATCH /api/v1/content/admin/seo/{seoId}/disable`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，禁用原因。 |

成功响应 HTTP `200`，`data` 为禁用后的 `SeoPayload`。

业务规则：SEO 配置不存在返回 `43403`。重复禁用已禁用配置返回成功，保持幂等，不重复写审计。禁用后公开 SEO 接口返回模块默认 SEO，`seoId` 为 `null`。

审计要求：首次禁用写入 `CONTENT_SEO_DISABLED`。

### content 自检摘要

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

### 状态、幂等和并发

内容状态流转为 `DRAFT` 到 `PENDING_REVIEW`，`PENDING_REVIEW` 到 `APPROVED`、`REJECTED` 或 `NEEDS_CHANGES`，`REJECTED` 和 `NEEDS_CHANGES` 可重新提交审核，`APPROVED` 发布后公开可见，公开中的 `APPROVED` 可下架为 `OFFLINE`，`OFFLINE` 可重新发布或归档，`DRAFT`、`REJECTED`、`NEEDS_CHANGES` 和 `OFFLINE` 可归档或软删除，`ARCHIVED` 原则上不可恢复，`DELETED` 为软删除终态。

创建内容、创建分类、创建标签、创建专题、保存首页草稿和保存 SEO 配置支持 `idempotencyKey`。并发使用同一幂等键只能创建或更新一次。并发创建相同 slug、分类名称、标签名称或 SEO route 时只能一个成功，其余返回冲突。

公开读取接口允许读到更新前或更新后的完整版本，但不能返回半更新对象。发布首页配置和内容发布必须以服务端当前状态为准，不得因为并发写入产生两个相同版本号或两个公开 slug。

### 审计要求

必须审计的动作包括创建内容、修改内容、提交审核、审核通过、审核拒绝、要求修改、发布、下架、归档、软删除、保存首页草稿、发布首页配置、回滚首页配置、创建分类、修改分类、归档分类、创建标签、修改标签、归档标签、创建专题、修改专题、发布专题、下架专题、归档专题、软删除专题、保存 SEO 和禁用 SEO。

后台写操作必须记录 `reason`。审计字段继承公共契约。审计写入失败时，后台写操作不得假装成功，必须返回 `51401` 或 `51400`，并保持业务数据不变。

公开读取和后台低风险读取不强制写审计。

### 失败降级

公开首页必须支持局部降级，单个引用失效不能导致整页空白。公开内容、专题、分类、标签和 SEO 读取失败时，前端按对应区域局部降级，content 不得伪造成功数据。

auth 认证上下文失败时，后台接口不得使用旧用户上下文继续写入。profile 快照失败时，不得创建新的可信成员作品。notification 强制投递失败时，审核通过、拒绝和要求修改不得改变状态；辅助通知失败时主流程可成功，但必须保留失败摘要和审计记录。

### 验收口径

`content` API 文档按 `docs/contracts-content.md` 独立存在，并由 `.local-docs/tests-content.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`content` 完成时必须满足以下条件：全部接口按本文档实现；公开接口不泄露后台字段；后台接口按角色限制；首页配置由后端返回且公开首页支持局部降级；成员作品只保存 profile 快照且不直接读 profile 数据库；审核通知按强制或辅助规则处理；分类、标签、专题、SEO、站点地图、预览令牌、状态流转、幂等、审计和自检摘要都有自动化测试；`.local-docs/tests-content.md` 中全部测试用例都有对应自动化验证；未实现时自动化测试必须先失败；实现后 content 全部测试通过；auth、profile 和 notification 前序服务回归测试通过；没有修改前序服务稳定接口；没有把 `.local-docs/` 提交到仓库。

## 北冥官网 server-status API 契约

来源：`docs/contracts-server-status.md`

版本：0.1

### 文档定位

本文档是 `server-status` 微服务的正式 API 契约。后续 `resource`、`admin`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、前端适配和 `ops-control` 只能通过本文档定义的接口读取玩家可见服务器状态和线路状态，不能直接读取或修改 `server-status` 数据库。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、能力点、审计字段、风险等级、通用错误码和请求编号均以公共契约为准。本文档只补充 `server-status` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

`server-status` 只适配 `auth`。它通过后端入口传入的认证上下文、`/api/v1/auth/me`、`/api/v1/auth/session/verify` 或测试环境 auth stub 获取当前用户、角色、能力点和用户状态。它不要求 `auth`、`profile`、`notification` 或 `content` 反向适配。

### 职责边界

`server-status` 负责玩家可见的 Minecraft 服务器状态、版本、MOTD、在线人数、最大人数、延迟、线路状态、历史快照、历史峰值、宕机记录和开服时长。

`server-status` 不负责账号、会话、成员档案、通知投递、首页内容配置、玩家资源下载、Cloudreve 分享、后台运维控制、节点注册、容器启停、虚拟机管理、文件操作、日志流、终端命令、节点密钥、备份恢复和高风险审批。真实服务器运维操作属于后续 `ops-control` 和 `external-node-executor`。

首页可以读取 `content` 的 `SERVER_ENTRY` 展示入口，也可以读取本文档的公开状态接口。`content` 不得伪造在线人数、MOTD、线路延迟或状态结果。`server-status` 也不得写入 content 首页配置。

### 数据归属

`server-status` 拥有以下主数据：玩家可见服务器实例、状态源、线路配置、当前状态缓存、历史状态快照、历史峰值统计、宕机记录、手动刷新幂等记录、审计日志和运行自检摘要。

状态源可以保存公开展示字段、检测类型和检测目标。检测目标、内部备注、采集凭据、节点信息和后台审计参数不得出现在公开接口。历史峰值只能由快照统计产生，不能由浏览器请求体写入。

### 基础路径与认证

公开接口使用 `/api/v1/server-status` 前缀，不要求登录，只返回玩家可见字段。

后台接口使用 `/api/v1/server-status/admin` 前缀，全部要求登录。后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`。后台写操作要求 `ADMIN` 或 `OWNER`，必须携带 `reason` 并写入审计。`HELPER` 可以读取状态源、线路、宕机记录和审计摘要，但不能创建、修改、启用、禁用、刷新或流转宕机记录。

`GET /api/v1/server-status/admin/audit-logs` 和 `GET /api/v1/server-status/admin/ops/summary` 只允许 `ADMIN` 或 `OWNER` 访问。

### auth 兼容契约

当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。

后台写操作里的 `createdBy`、`updatedBy`、`refreshedBy`、`disabledBy`、`enabledBy`、`acknowledgedBy`、`resolvedBy` 和 `archivedBy` 均来自服务端认证上下文。浏览器请求体传入同名字段时必须忽略或返回字段校验失败。

auth 上下文不可用返回 `46500`，auth 调用超时返回 `46501`，auth 返回字段缺失或枚举不兼容返回 `46502`。`server-status` 不能导入 auth 的内存存储、实体、Repository 或测试种子实现。

布尔字段只能接收 JSON boolean 或字符串 `true`、`false`。时间字段必须是合法 ISO 8601 instant。布尔值和时间格式不合法时返回 `40001`，不得落入 `51500`。

### 枚举

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

### 通用对象

#### PublicServerStatusOverview

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

#### PublicServerInstanceStatus

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

#### PublicServerLineStatus

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

#### ServerStatusSnapshot

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

#### ServerOutagePublicRecord

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

#### AdminStatusSource

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

#### AdminLineConfig

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

#### AdminOutageRecord

`AdminOutageRecord` 在 `ServerOutagePublicRecord` 基础上补充 `instanceId`、`lineId`、`internalReason`、`adminNote`、`createdBy`、`updatedBy`、`acknowledgedBy`、`resolvedBy`、`archivedBy`、`acknowledgedAt`、`archivedAt`、`createdAt`。后台内部字段不得出现在公开接口。

#### ServerStatusAuditLog

审计字段继承公共契约，允许补充 `sourceId`、`lineId`、`outageId`、`snapshotId`、`idempotencyKey`、`stateFrom`、`stateTo` 和 `collectorStatus`。审计日志不得通过 server-status API 删除。

审计列表返回字段必须至少包含公共契约要求的 `id`、`requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`sourceIp`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason` 和 `createdAt`。P0 内存实现拿不到来源 IP 或状态摘要时可以返回 `null`，但字段必须存在，方便后续持久化实现无破坏升级。

#### ServerStatusOpsSummary

自检摘要至少包含 `service`、`storageMode`、`collectorMode`、`authMode`、`sourcesTotal`、`instancesTotal`、`linesTotal`、`snapshotsTotal`、`outagesTotal`、`auditsTotal`、`lastSnapshotAt`、`lastAuditAt` 和 `warnings`。不得返回 token、请求头、检测凭据、后台备注、内部检测目标密码或审计原因全文。

### server-status 错误码

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

### 接口总览

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

### 公开接口

#### 状态总览

`GET /api/v1/server-status/overview`

成功响应 HTTP `200`，`data` 为 `PublicServerStatusOverview`。

业务规则：总览只能汇总 `ENABLED`、`publicVisible=true` 且未归档的状态源和线路。整体状态根据关键实例和关键线路计算：全部未知为 `UNKNOWN`，任一关键实例离线为 `OFFLINE`，任一关键实例或关键线路降级为 `DEGRADED`，否则为 `ONLINE`。采集失败时可以返回最近一次成功快照并标记 `degraded=true`，不得伪造实时成功。没有任何快照时返回 `UNKNOWN`、空数组和 `NO_RECENT_SNAPSHOT`。

公开字段不得包含 `target`、`checkTarget`、`adminNote`、`internalReason`、审计字段、幂等键、节点信息、采集凭据或运维控制入口。

#### 公开实例列表

`GET /api/v1/server-status/instances`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `kind` | string | 否 | 任一 `InstanceKind`。 |
| `status` | string | 否 | 任一 `ServerReachability`。 |
| `sort` | string | 否 | 允许 `sortOrder_asc`、`name_asc`、`onlinePlayers_desc`。默认 `sortOrder_asc`。 |

成功响应 HTTP `200`，`data.items` 为 `PublicServerInstanceStatus[]`。

业务规则：只返回公开启用实例。`DISABLED`、`ARCHIVED` 或 `publicVisible=false` 的状态源不得出现在公开列表。

#### 公开实例详情

`GET /api/v1/server-status/instances/{instanceId}`

成功响应 HTTP `200`，`data` 为 `PublicServerInstanceStatus`。实例不存在、未启用、已归档或不可公开时返回 `43500`。

#### 公开线路列表

`GET /api/v1/server-status/lines`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `status` | string | 否 | 任一 `LineReachability`。 |
| `sort` | string | 否 | 允许 `sortOrder_asc`、`latencyMs_asc`、`name_asc`。默认 `sortOrder_asc`。 |

成功响应 HTTP `200`，`data.items` 为 `PublicServerLineStatus[]`。只返回 `ENABLED` 且公开的线路，不返回 `checkTarget` 和后台备注。

#### 历史快照

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

#### 公开宕机记录

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

### 后台状态源接口

#### 状态源列表

`GET /api/v1/server-status/admin/sources`

查询参数包括 `page`、`pageSize`、`keyword`、`sourceType`、`configStatus`、`instanceKind`、`publicVisible` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`sortOrder_asc`、`displayName_asc`。成功响应分页 `items` 为 `AdminStatusSource[]`。

#### 创建状态源

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

#### 修改状态源

`PATCH /api/v1/server-status/admin/sources/{sourceId}`

请求字段同创建状态源，除 `reason` 必填外其余字段按需修改。`ARCHIVED` 状态源不可修改检测目标，返回 `43510`。状态源不存在返回 `43502`。成功写入 `SERVER_STATUS_SOURCE_UPDATED` 审计。

#### 禁用状态源

`PATCH /api/v1/server-status/admin/sources/{sourceId}/disable`

请求字段只有必填 `reason`。`ENABLED` 可流转为 `DISABLED`。重复禁用返回成功，保持幂等，不重复写审计。`ARCHIVED` 返回 `43510`。

#### 启用状态源

`PATCH /api/v1/server-status/admin/sources/{sourceId}/enable`

请求字段只有必填 `reason`。`DISABLED` 可流转为 `ENABLED`。重复启用返回成功，保持幂等，不重复写审计。`ARCHIVED` 返回 `43510`。

#### 手动刷新

`POST /api/v1/server-status/admin/sources/{sourceId}/refresh`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 刷新重试幂等键，10 分钟内有效。 |

成功响应 HTTP `200`，`data` 为 `ServerStatusSnapshot`。

业务规则：只有 `ENABLED` 状态源可刷新。刷新过于频繁、相同状态源已有刷新进行中或同一幂等键请求体冲突返回 `43512` 或 `43002`。采集不可用返回 `46510`，采集超时返回 `46511`。采集失败不能写入伪造成功快照。快照写入失败返回 `51502`。刷新成功写入 `SERVER_STATUS_SOURCE_REFRESHED` 审计。

手动刷新必须以状态源为粒度加锁。同一状态源已有刷新进行中时，后续刷新请求返回 `43512`。无幂等键的连续刷新必须受冷却窗口限制，P0 默认冷却窗口为 10 分钟。携带相同 `idempotencyKey` 且请求体一致的重试应优先返回第一次刷新结果，不受冷却窗口影响。

### 后台线路接口

#### 后台线路列表

`GET /api/v1/server-status/admin/lines`

查询参数包括 `page`、`pageSize`、`keyword`、`configStatus`、`currentStatus`、`publicVisible` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`sortOrder_asc`、`name_asc`。成功响应分页 `items` 为 `AdminLineConfig[]`。

#### 创建线路

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

#### 修改线路

`PATCH /api/v1/server-status/admin/lines/{lineId}`

请求字段同创建线路，除 `reason` 必填外其余字段按需修改。线路不存在返回 `43501`。`ARCHIVED` 线路不可修改检测目标，返回 `43510`。成功写入 `SERVER_STATUS_LINE_UPDATED` 审计。

#### 禁用线路

`PATCH /api/v1/server-status/admin/lines/{lineId}/disable`

请求字段只有必填 `reason`。禁用后公开线路列表不再返回该线路。重复禁用保持幂等，不重复写审计。

#### 启用线路

`PATCH /api/v1/server-status/admin/lines/{lineId}/enable`

请求字段只有必填 `reason`。启用后按 `publicVisible` 决定是否公开。重复启用保持幂等，不重复写审计。

### 后台宕机接口

#### 后台宕机列表

`GET /api/v1/server-status/admin/outages`

查询参数包括 `page`、`pageSize`、`status`、`severity`、`instanceId`、`lineId`、`keyword` 和 `sort`。`sort` 允许 `startedAt_desc`、`updatedAt_desc`、`resolvedAt_desc`。成功响应分页 `items` 为 `AdminOutageRecord[]`。

#### 创建宕机记录

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

#### 修改宕机记录

`PATCH /api/v1/server-status/admin/outages/{outageId}`

请求字段同创建宕机记录，除 `reason` 必填外其余字段按需修改。`ARCHIVED` 记录不可修改，返回 `43510`。

#### 确认宕机记录

`PATCH /api/v1/server-status/admin/outages/{outageId}/acknowledge`

请求字段只有必填 `reason`。`OPEN` 可流转为 `ACKNOWLEDGED`。重复确认 `ACKNOWLEDGED` 返回成功，保持幂等，不重复写审计。`RESOLVED` 和 `ARCHIVED` 返回 `43510`。

#### 解决宕机记录

`PATCH /api/v1/server-status/admin/outages/{outageId}/resolve`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `resolvedAt` | string | 否 | 默认当前时间，必须不早于 `startedAt`。 |
| `publicMessage` | string | 否 | 可更新恢复说明，最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |

`OPEN` 和 `ACKNOWLEDGED` 可流转为 `RESOLVED`。重复解决同一记录返回成功，保持 `resolvedAt` 不变，不重复写审计。`ARCHIVED` 返回 `43510`。

#### 归档宕机记录

`PATCH /api/v1/server-status/admin/outages/{outageId}/archive`

请求字段只有必填 `reason`。只有 `RESOLVED` 可流转为 `ARCHIVED`。重复归档返回成功，保持幂等，不重复写审计。`OPEN` 和 `ACKNOWLEDGED` 返回 `43510`。

### 审计和自检接口

#### 审计列表

`GET /api/v1/server-status/admin/audit-logs`

查询参数包括 `page`、`pageSize`、`targetType`、`targetId`、`action`、`actorUserId`、`result`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`。成功响应分页 `items` 为 `ServerStatusAuditLog[]`。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 server-status API 删除。

#### 自检摘要

`GET /api/v1/server-status/admin/ops/summary`

成功响应 HTTP `200`，`data` 为 `ServerStatusOpsSummary`。

业务规则：自检摘要用于后台确认 `server-status` 当前运行模式、数据量、采集器模式和生产化缺口。P0 可返回 `storageMode=IN_MEMORY`、`collectorMode=TEST_STUB` 和 `authMode=TEST_STUB`。摘要不得返回 token、请求头、检测凭据、后台备注、内部检测目标密码或审计原因全文。读取失败返回 `51500`，不得伪造健康。

### 状态、幂等和并发

状态源和线路配置状态流转为 `ENABLED` 到 `DISABLED`，`DISABLED` 到 `ENABLED`。`ARCHIVED` 保留给后续清理和迁移，P0 不提供归档接口。`ARCHIVED` 只保留历史，不参与采集，不公开展示，不允许修改检测目标。

宕机记录状态流转为 `OPEN` 到 `ACKNOWLEDGED` 或 `RESOLVED`，`ACKNOWLEDGED` 到 `RESOLVED`，`RESOLVED` 到 `ARCHIVED`。`ARCHIVED` 不可修改。重复确认、重复解决和重复归档按本文档保持幂等。

创建状态源、创建线路、创建宕机记录和手动刷新支持 `idempotencyKey`。同一操作者、同一幂等键、同一请求体重复提交时返回同一结果。相同幂等键搭配不同请求体返回 `43002`。并发创建相同状态源目标、线路入口或重复刷新同一状态源时只能一个请求成功，其余返回冲突。

幂等请求体指纹必须使用字段名排序后的稳定 JSON 语义计算，不能依赖浏览器字段顺序或 Java `Map.toString()`。创建类幂等记录有效期为 24 小时，手动刷新幂等记录有效期为 10 分钟。过期记录不得继续影响新请求。

### 状态采集与失败降级

采集器必须和 store 分开。采集成功时写入当前状态和历史快照。采集失败时不清空旧状态，公开接口优先返回最近一次成功快照，并标记 `degraded=true`。没有任何快照时返回 `UNKNOWN`，不得伪造在线。

降级原因允许 `COLLECTOR_UNAVAILABLE`、`COLLECTOR_TIMEOUT`、`NO_RECENT_SNAPSHOT`、`PARTIAL_LINE_FAILURE`、`SOURCE_DISABLED` 和 `IN_MEMORY_STALE_DATA`。采集不可用、超时和快照写入失败必须有可测试错误码。

### 审计要求

必须审计的动作包括创建状态源、修改状态源、禁用状态源、启用状态源、手动刷新、创建线路、修改线路、禁用线路、启用线路、创建宕机记录、修改宕机记录、确认宕机记录、解决宕机记录和归档宕机记录。

后台写操作必须记录 `reason`。审计字段继承公共契约。审计写入失败时，后台写操作不得假装成功，必须返回 `51501` 或 `51500`，并保持业务数据不变。公开读取和后台低风险读取不强制写审计。

### 验收口径

`server-status` API 文档按 `docs/contracts-server-status.md` 独立存在，并由 `.local-docs/tests-server-status.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`server-status` 完成时必须满足以下条件：全部接口按本文档实现；公开接口不泄露后台检测目标、内部备注、采集凭据、审计字段和运维入口；后台接口按角色限制；手动刷新遵守采集失败和降级规则；历史峰值和开服时长由服务端计算；状态源、线路、宕机记录、快照、审计、自检摘要、requestId、端口配置和 auth 适配都有自动化测试；`.local-docs/tests-server-status.md` 中全部测试用例都有对应自动化验证；未实现时自动化测试必须先失败；实现后 `server-status` 全部测试通过；`auth`、`profile`、`notification` 和 `content` 前序服务回归测试通过；没有修改前序服务稳定接口；没有把资源下载、Cloudreve 分享、容器启停、文件、日志、终端或节点控制能力塞进 `server-status`。

## 北冥官网 resource API 契约

来源：`docs/contracts-resource.md`

版本：0.1

### 文档定位

本文档是 `resource` 微服务的正式 API 契约。后续 `admin`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、前端适配和 `ops-control` 只能通过本文档定义的接口读取或管理玩家可见资源，不能直接读取或修改 `resource` 数据库。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、能力点、审计字段、风险等级、通用状态模型和通用错误码均以公共契约为准。本文档只补充 `resource` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

`resource` 适配 `auth`、`profile` 和 `notification`，不要求前序服务反向适配 `resource`。它通过后端入口传入的认证上下文、`/api/v1/auth/me`、`/api/v1/auth/session/verify` 或测试环境 auth stub 读取当前用户、角色、能力点和用户状态。它通过 profile 正式接口或受控 profile stub 判断 `MEMBER_ONLY` 资源的成员资格。资源审核、发布、下架、版本更新需要通知时，只能调用 notification 正式投递接口或受控适配层，不能自建通知主数据、未读数或模板系统。

### 参考口径

resource 的分区参考了 MCSManager 管理面板的实例、文件、终端、监控分层，Cloudreve 的云盘分享和外链分发模式，以及 Nextcloud、Google Drive、GitHub Releases 这类资源库常见的分类、标签、版本、可见范围、分享权限和修订记录。本文档只吸收资源展示、版本管理、分享链接、权限校验和审计这些适合玩家资源库的能力。MCSManager 式实例控制、服务器文件管理、终端命令、节点注册、容器启停、日志流和监控指标全部属于后续 `ops-control` 与 `external-node-executor`，不得进入 `resource`。

### 职责边界

`resource` 负责玩家可见资源下载、资源分类、资源条目、资源版本、下载入口、Cloudreve 分享链接快照、外部链接快照、可见范围、下载权限、资源状态、版本记录、下载记录摘要、资源审计和后台资源元数据管理。

`resource` 不负责注册、登录、会话、角色能力点主数据、成员档案主数据、站内通知主数据、首页内容配置、服务器状态展示、服务器文件浏览、真实文件上传下载、容器管理、虚拟机管理、Minecraft 实例管理、终端命令、日志流、节点密钥、备份恢复和高风险运维审批。

首页资源入口仍由 `content` 的 `RESOURCE_ENTRY` 配置展示位提供。真实资源列表、版本和下载入口由 `resource` 提供。玩家可见 Minecraft 状态仍由 `server-status` 提供。后台文件管理、授权目录操作和 Cloudreve 管理 API 深度同步属于后续 `ops-control` 或 P3 resource 兼容变更，不在 P0 范围内。

### 数据归属

`resource` 拥有以下主数据：资源分类、资源条目、资源版本、下载入口、Cloudreve 分享快照、外部链接快照、下载记录摘要、幂等记录、资源审计日志和运行自检摘要。

`resource` 可以保存创建者、维护人或成员作者的展示快照，例如 `displayName`、`avatarUrl`、`minecraftId`、`memberStatus` 和 `snapshotAt`。快照来自 auth、profile 或服务端可信上下文，不来自浏览器可篡改字段。快照不是 auth 或 profile 主数据，不能用于账号权限或成员资格的最终判断。

Cloudreve P0 只保存公开分享链接快照和安全摘要，不保存真实 Cloudreve 管理 token、内部文件绝对路径、分享密码明文、服务端请求头或云盘管理凭据。后续接入 Cloudreve API 时，必须新增或更新契约、测试文档、自动化测试和失败降级规则，再进入实现。

### 基础路径与认证

公开接口使用 `/api/v1/resources` 前缀。公开读取接口不要求登录，但只能返回 `PUBLISHED`、未下架、未归档、未软删除、处于可见时间范围内且公开字段允许展示的数据。

下载解析接口使用 `POST /api/v1/resources/{resourceId}/versions/{versionId}/download`。该接口会校验可见范围、刷新或读取分享快照、写入下载记录摘要，因此使用 `POST`。`PUBLIC` 资源允许未登录下载。`AUTHENTICATED`、`MEMBER_ONLY` 和 `ADMIN_ONLY` 资源必须按本文档校验身份。

后台接口使用 `/api/v1/resources/admin` 前缀，全部要求登录。后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`。后台写操作要求 `ADMIN` 或 `OWNER`，必须携带 `reason` 并写入审计。`HELPER` 可以读取资源、分类、版本、审计摘要和自检摘要，但不能创建、修改、审核、发布、下架、归档、删除、创建版本、修改版本、启用禁用版本或维护分类。

`GET /api/v1/resources/admin/items/{resourceId}/audit-logs` 和 `GET /api/v1/resources/admin/ops/summary` 只允许 `ADMIN` 或 `OWNER` 访问。

### auth 兼容契约

当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口，也不得解析受限下载入口。

后台写操作里的 `createdBy`、`updatedBy`、`submittedBy`、`reviewedBy`、`publishedBy`、`offlineBy`、`archivedBy`、`deletedBy` 和 `disabledBy` 均来自服务端认证上下文。浏览器请求体传入同名字段时必须忽略或返回字段校验失败。

auth 上下文不可用返回 `46600`，auth 调用超时返回 `46601`，auth 返回字段缺失或枚举不兼容返回 `46602`。`resource` 不能导入 auth 的内存存储、实体、Repository 或测试种子实现。

### profile 兼容契约

`MEMBER_ONLY` 下载解析必须通过 profile 正式接口、后端入口可信成员上下文或测试环境 profile stub 判断当前用户是否有有效成员档案。允许下载的成员状态为 `ACTIVE` 和 `INACTIVE`。`SUSPENDED`、`REMOVED`、`ARCHIVED`、无档案、profile 不可用或 profile 字段不兼容时不得返回下载链接。

创建或修改资源时，如果请求包含 `maintainerMemberId`，`resource` 可以通过 profile 读取维护人公开快照并保存。profile 成员不存在或不可公开返回 `46610`。profile 调用超时返回 `46611`。profile 字段缺失或枚举不兼容返回 `46612`。

客户端不得通过请求体伪造成员展示名、头像、Minecraft ID 或成员状态作为可信字段。profile 不可用时不得创建新的可信成员维护人快照；已发布资源公开读取可以继续返回已保存快照，并在后台详情中标记快照时间。

### notification 兼容契约

审核通过、拒绝、要求修改、发布、下架、归档、软删除和版本更新都可以触发通知。本文档规定审核拒绝、要求修改必须通知资源创建者或维护人；没有可通知用户时跳过通知并在审计中记录 `NO_RECIPIENT_TO_NOTIFY`。发布、下架、归档、软删除和版本更新通知为辅助提醒，通知失败时主流程可以成功，但必须在审计中记录 `notificationStatus=FAILED` 和失败原因。

强制通知失败返回 `46620` 或 `46621`，业务状态不得变化。辅助通知失败不得伪造通知成功，也不得影响公开读取。`resource` 不能自建通知表、未读数、模板和投递记录。

### Cloudreve 和外部下载适配

P0 支持 `CLOUDREVE_SHARE`、`EXTERNAL_URL` 和 `LOCAL_STUB` 三类下载入口。`CLOUDREVE_SHARE` 只保存分享链接快照。`EXTERNAL_URL` 只用于可信外部资源镜像或文档下载，不得保存需要服务端密钥的私有下载 URL。`LOCAL_STUB` 只用于 P0 测试和本地演示，不代表真实文件服务。

下载解析时必须以资源状态、版本状态、下载入口状态、可见范围、时间窗口和依赖健康为准。下载入口过期、禁用、不可用或 Cloudreve 不可用时，公开列表和详情仍可返回资源说明，并标记 `downloadAvailable=false` 与 `degradeReasons`。下载解析返回明确错误，不伪造可下载链接。

如果存在未过期且未禁用的旧分享快照，Cloudreve 当前检查不可用时允许降级返回旧快照，但必须返回 `degraded=true`、`stale=true` 和 `degradeReasons`。如果没有可用旧快照，返回 `46630` 或 `46631`。

### 枚举

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

### 通用对象

#### ResourceCategory

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

#### PublicResourceSummary

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

#### PublicResourceDetail

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

#### PublicResourceVersion

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

#### AdminResourceItem

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

#### AdminResourceVersion

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

#### ResourceDownloadEntry

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

#### CloudreveShareSnapshot

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

#### ExternalLinkSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `provider` | string | 是 | 固定为 `EXTERNAL_URL` 或 `LOCAL_STUB`。 |
| `url` | string | 是 | 公开下载 URL 或测试 stub URL。 |
| `status` | string | 是 | `DownloadEntryStatus`。 |
| `expiresAt` | string 或 null | 是 | 过期时间。 |
| `lastCheckedAt` | string 或 null | 是 | 最近检查时间。 |

#### ResourceDownloadTicket

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

#### ResourceDownloadRecordSummary

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

#### ResourceMaintainerSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `memberId` | string | 是 | profile 成员 ID。 |
| `displayName` | string | 是 | 成员展示名快照。 |
| `avatarUrl` | string 或 null | 是 | 头像快照。 |
| `minecraftId` | string 或 null | 是 | Minecraft ID 快照。 |
| `memberStatus` | string | 是 | 成员状态快照。 |
| `snapshotAt` | string | 是 | 快照时间。 |

#### ResourceAuditLog

审计字段继承公共契约，允许补充 `resourceId`、`versionId`、`categoryId`、`downloadEntryId`、`ticketId`、`idempotencyKey`、`stateFrom`、`stateTo`、`notificationStatus`、`profileSnapshotStatus`、`cloudreveStatus` 和 `downloadRecordResult`。审计日志不得通过 resource API 删除。

#### ResourceOpsSummary

自检摘要至少包含 `service`、`storageMode`、`authMode`、`profileMode`、`notificationMode`、`cloudreveMode`、`resourcesTotal`、`publishedResourcesTotal`、`versionsTotal`、`categoriesTotal`、`downloadEntriesTotal`、`downloadRecordsTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastAuditAt`、`lastDownloadAt`、`warnings` 和 `productionGaps`。不得返回 token、请求头、后台备注、审核意见、分享密码、Cloudreve 管理凭据、内部文件路径或审计原因全文。

### resource 错误码

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

### 接口总览

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

### 公开资源接口

#### 公开资源列表

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

#### 公开资源详情

`GET /api/v1/resources/{resourceId}`

成功响应 HTTP `200`，`data` 为 `PublicResourceDetail`。资源不存在或不可公开返回 `43600`。

业务规则：只允许读取公开可见资源详情。详情可以展示所有可见版本摘要，但不得返回后台备注、审核意见、内部下载配置或真实下载 URL。下载入口只能通过下载解析接口获得。

#### 公开 slug 资源详情

`GET /api/v1/resources/by-slug/{slug}`

成功响应 HTTP `200`，`data` 为 `PublicResourceDetail`。slug 不存在或资源不可公开返回 `43600`。

业务规则：供前端稳定路由使用，返回语义与 ID 详情一致。

#### 公开分类列表

`GET /api/v1/resources/categories`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `type` | string | 否 | 资源类型筛选。 |

成功响应 HTTP `200`，`data.items` 为 `ResourceCategory[]`。

业务规则：只返回 `enabled=true` 且 `archived=false` 的分类。分类按 `sortOrder`、`name` 稳定排序。公开分类不得返回审计、后台备注或引用中的不可公开资源数量。

#### 公开资源版本列表

`GET /api/v1/resources/{resourceId}/versions`

成功响应 HTTP `200`，`data.items` 为 `PublicResourceVersion[]`。

业务规则：资源必须公开可见。只返回 `ENABLED` 且有关联下载入口的版本。版本按 `releasedAt_desc`、`createdAt_desc` 稳定排序。资源不存在或不可公开返回 `43600`。

#### 下载入口解析

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

### 后台资源接口

#### 后台资源列表

`GET /api/v1/resources/admin/items`

查询参数包括 `page`、`pageSize`、`keyword`、`type`、`status`、`visibility`、`categoryId`、`tag`、`createdBy`、`maintainerMemberId` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`publishedAt_desc`、`title_asc`。成功响应 HTTP `200`，分页 `items` 为 `AdminResourceItem[]`。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。未登录返回 `41000`。

分页规则：分页接口必须按过滤后的全集计算 `total`，按请求的 `page` 和 `pageSize` 返回切片。空页返回空数组，不得回退第一页。默认 `page=1`、`pageSize=20`。公开资源列表默认按 `publishedAt_desc`，后台资源列表默认按 `updatedAt_desc`，审计列表默认按 `createdAt_desc`。排序字段相同必须追加稳定 ID 排序，避免翻页重复或遗漏。

#### 后台资源详情

`GET /api/v1/resources/admin/items/{resourceId}`

成功响应 HTTP `200`，`data` 为 `AdminResourceItem`，允许附带版本、下载入口和审计摘要。资源不存在返回 `43600`。

#### 创建资源

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

#### 修改资源

`PATCH /api/v1/resources/admin/items/{resourceId}`

请求字段同创建资源，除 `reason` 必填外其余字段按需修改。

成功响应 HTTP `200`，`data` 为更新后的 `AdminResourceItem`。

业务规则：资源不存在返回 `43600`。`ARCHIVED` 和 `DELETED` 资源不允许修改主体字段，返回 `43610`。slug 冲突返回 `43611`。修改已发布资源必须保证公开读取不会返回半更新状态。审计失败时不得改变资源。

#### 提交审核

`PATCH /api/v1/resources/admin/items/{resourceId}/submit-review`

请求字段只有必填 `reason`。`DRAFT`、`REJECTED` 和 `NEEDS_CHANGES` 可流转为 `PENDING_REVIEW`。重复提交 `PENDING_REVIEW` 返回成功，保持幂等，不重复写审计。其他状态返回 `43610`。

#### 审核通过

`PATCH /api/v1/resources/admin/items/{resourceId}/approve`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reviewOpinion` | string | 是 | 1 到 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |

`PENDING_REVIEW` 可流转为 `APPROVED`。重复审核已 `APPROVED` 资源返回成功，保持幂等，不重复写审计。其他状态返回 `43610`。

#### 审核拒绝

`PATCH /api/v1/resources/admin/items/{resourceId}/reject`

请求字段同审核通过。`PENDING_REVIEW` 可流转为 `REJECTED`。审核拒绝必须通知创建者或维护人；强制通知失败时状态不变化并返回 `46620` 或 `46621`。重复拒绝保持幂等。

#### 要求修改

`PATCH /api/v1/resources/admin/items/{resourceId}/request-changes`

请求字段同审核通过。`PENDING_REVIEW` 可流转为 `NEEDS_CHANGES`。要求修改必须通知创建者或维护人；强制通知失败时状态不变化。重复要求修改保持幂等。

#### 发布资源

`PATCH /api/v1/resources/admin/items/{resourceId}/publish`

请求字段只有必填 `reason`。

业务规则：`APPROVED` 和 `OFFLINE` 可发布为 `PUBLISHED`，并写入或更新 `publishedAt`。发布前必须至少存在一个 `ENABLED` 版本，且该版本存在 `ACTIVE` 下载入口。否则返回 `43614`。重复发布 `PUBLISHED` 返回成功，保持幂等，不重复写审计。辅助通知失败时主流程可成功，但必须记录通知失败摘要。

#### 下架资源

`PATCH /api/v1/resources/admin/items/{resourceId}/offline`

请求字段只有必填 `reason`。`PUBLISHED` 可流转为 `OFFLINE`，并从公开接口消失。重复下架 `OFFLINE` 返回成功，保持幂等，不重复写审计。`DRAFT`、`PENDING_REVIEW`、`ARCHIVED` 和 `DELETED` 返回 `43610`。

#### 归档资源

`PATCH /api/v1/resources/admin/items/{resourceId}/archive`

请求字段只有必填 `reason`。`DRAFT`、`REJECTED`、`NEEDS_CHANGES` 和 `OFFLINE` 可流转为 `ARCHIVED`。已发布资源必须先下架再归档，直接归档返回 `43610`。重复归档返回成功，保持幂等。

#### 软删除资源

`PATCH /api/v1/resources/admin/items/{resourceId}/delete`

请求字段只有必填 `reason`。只做软删除，状态为 `DELETED`，写入 `deletedAt`。已发布资源必须先下架再软删除，直接删除返回 `43610`。重复软删除返回成功，保持幂等。P0 不提供真实删除接口。

### 后台版本接口

#### 后台版本列表

`GET /api/v1/resources/admin/items/{resourceId}/versions`

成功响应 HTTP `200`，`data.items` 为 `AdminResourceVersion[]`。资源不存在返回 `43600`。

#### 创建版本

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

#### 修改版本

`PATCH /api/v1/resources/admin/items/{resourceId}/versions/{versionId}`

请求字段同创建版本，除 `reason` 必填外其余字段按需修改。

成功响应 HTTP `200`，`data` 为更新后的 `AdminResourceVersion`。版本不存在返回 `43602`。版本号冲突返回 `43611`。`ARCHIVED` 版本不可修改下载入口，返回 `43610`。

#### 禁用版本

`PATCH /api/v1/resources/admin/items/{resourceId}/versions/{versionId}/disable`

请求字段只有必填 `reason`。`ENABLED` 可流转为 `DISABLED`。重复禁用返回成功，保持幂等。禁用后公开版本列表和下载解析不再返回该版本。

#### 启用版本

`PATCH /api/v1/resources/admin/items/{resourceId}/versions/{versionId}/enable`

请求字段只有必填 `reason`。`DISABLED` 可流转为 `ENABLED`。重复启用返回成功，保持幂等。启用前必须存在 `ACTIVE` 且未过期下载入口，否则返回 `43613`。

### 后台分类接口

#### 后台分类列表

`GET /api/v1/resources/admin/categories`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `includeArchived` | boolean | 否 | 默认 `true`。 |
| `enabled` | boolean | 否 | 是否启用。 |
| `keyword` | string | 否 | 匹配名称或 slug，最多 80 位。 |

成功响应 HTTP `200`，`data.items` 为 `ResourceCategory[]`，按 `sortOrder`、`name` 稳定排序。

#### 创建分类

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

#### 修改分类

`PATCH /api/v1/resources/admin/categories/{categoryId}`

请求字段同创建分类，除 `reason` 必填外其余字段按需修改。

成功响应 HTTP `200`，`data` 为更新后的 `ResourceCategory`。分类不存在返回 `43601`。已归档分类不得通过修改接口取消归档。

#### 归档分类

`PATCH /api/v1/resources/admin/categories/{categoryId}/archive`

请求字段只有必填 `reason`。仍被未归档、未软删除资源引用的分类不能归档，返回 `43615`。重复归档保持幂等，不重复写审计。归档后公开分类列表不再返回该分类。

### 审计和自检接口

#### 资源审计列表

`GET /api/v1/resources/admin/items/{resourceId}/audit-logs`

查询参数包括 `page`、`pageSize`、`action`、`actorUserId`、`result`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`。成功响应分页 `items` 为 `ResourceAuditLog[]`。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 可读后台资源详情，但不能读取审计列表。资源不存在返回 `43600`。审计日志不得通过 resource API 删除。

#### resource 自检摘要

`GET /api/v1/resources/admin/ops/summary`

成功响应 HTTP `200`，`data` 为 `ResourceOpsSummary`。

业务规则：自检摘要用于后台确认 `resource` 当前运行模式、数据规模、适配层状态和生产化缺口。P0 可返回 `storageMode=IN_MEMORY`、`authMode=TEST_STUB`、`profileMode=TEST_STUB`、`notificationMode=TEST_STUB` 和 `cloudreveMode=LINK_ONLY_STUB`。`productionGaps` 至少说明真实持久化、真实认证适配、真实 profile 适配、真实 notification 适配和 Cloudreve API 深度同步是否未启用。摘要不得返回 token、请求头、分享密码、后台备注、内部路径或审计原因全文。读取失败返回 `51600`，不得伪造健康。

### 状态、幂等和并发

资源状态流转为 `DRAFT` 到 `PENDING_REVIEW`，`PENDING_REVIEW` 到 `APPROVED`、`REJECTED` 或 `NEEDS_CHANGES`，`REJECTED` 和 `NEEDS_CHANGES` 可重新提交审核，`APPROVED` 可发布为 `PUBLISHED`，`PUBLISHED` 可下架为 `OFFLINE`，`OFFLINE` 可重新发布或归档，`DRAFT`、`REJECTED`、`NEEDS_CHANGES` 和 `OFFLINE` 可归档或软删除，`ARCHIVED` 原则上不可恢复，`DELETED` 为软删除终态。

版本状态流转为 `ENABLED` 到 `DISABLED`，`DISABLED` 到 `ENABLED`。`ARCHIVED` 保留给后续迁移或长期留存，P0 不提供版本归档接口。资源发布要求至少一个可用版本和可用下载入口。

下载入口状态由后台配置和适配器检查共同决定。`ACTIVE` 且未过期才允许下载解析。`EXPIRED`、`DISABLED` 和 `UNAVAILABLE` 不允许解析。Cloudreve 当前检查失败但旧快照可用时可以降级返回旧快照。

创建资源、创建版本、创建分类和下载解析支持 `idempotencyKey`。同一操作者或访问者、同一幂等键、同一请求体重复提交时返回同一结果。相同幂等键搭配不同请求体返回 `43612`。幂等请求体指纹必须使用字段名排序后的稳定 JSON 语义计算，不能依赖浏览器字段顺序或 Java `Map.toString()`。

并发创建相同资源 slug、分类 slug、分类名称或同一资源版本名时只能一个请求成功，其余返回冲突。公开读取接口允许读到更新前或更新后的完整状态，但不能返回半更新对象。

请求校验必须优先于业务写入。枚举、时间、URL、布尔值、长度、数字范围和可信字段都必须返回 `40001` 或对应公共错误，不得落入 `51600`。后台写接口请求体出现 `createdBy`、`updatedBy`、`submittedBy`、`reviewedBy`、`publishedBy`、`offlineBy`、`archivedBy`、`deletedBy`、`disabledBy`、`maintainerSnapshot`、`downloadRecordsTotal`、`auditsTotal` 等服务端字段时，必须返回字段校验失败或忽略并以服务端上下文为准；生产实现推荐返回字段级 `errors`，P0 至少不得信任这些字段。

### 审计要求

必须审计的动作包括创建资源、修改资源、提交审核、审核通过、审核拒绝、要求修改、发布、下架、归档、软删除、创建版本、修改版本、禁用版本、启用版本、创建分类、修改分类、归档分类、下载解析失败、受限下载拒绝、Cloudreve 降级和下载记录写入失败。

后台写操作必须记录调用者、`reason`、操作前状态、操作后状态、请求编号和结果。审计字段继承公共契约。审计写入失败时，后台写操作不得假装成功，必须返回 `51601` 或 `51600`，并保持业务数据不变。

公开读取不强制写审计。下载解析必须写下载记录摘要。受限下载拒绝可以写低风险审计或下载拒绝记录，但不得暴露敏感资源是否存在给无权限用户。

### 失败降级

公开列表、详情、分类和版本接口在 Cloudreve 不可用时不得整页失败。接口应返回资源说明、`downloadAvailable=false`、`degraded=true` 和明确 `degradeReasons`。资源主数据存储不可用时不能伪造成功。

auth 认证上下文失败时，后台接口和受限下载解析不得使用旧用户上下文继续写入。profile 失败时，`MEMBER_ONLY` 下载解析不得放行；创建维护人快照失败时不得保存伪造快照。notification 强制投递失败时，审核拒绝和要求修改不得改变状态；辅助通知失败时主流程可成功，但必须保留失败摘要和审计记录。

Cloudreve 分享链接不可用时，下载解析可以在旧快照仍合法时降级返回旧快照；否则返回依赖错误。任何情况下不得返回分享密码明文、Cloudreve 管理 token、内部文件路径或需要服务端密钥的私有下载 URL。

### 验收口径

`resource` API 文档按 `docs/contracts-resource.md` 独立存在，并由 `.local-docs/tests-resource.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`resource` 完成时必须满足以下条件：全部接口按本文档实现；公开接口不泄露后台字段、Cloudreve 管理字段、分享密码、内部路径、审计字段和运维入口；后台接口按角色限制；受限下载按 `PUBLIC`、`AUTHENTICATED`、`MEMBER_ONLY` 和 `ADMIN_ONLY` 正确校验；Cloudreve 分享链接失败可测试降级；分类、资源、版本、下载入口、状态流转、幂等、审计、自检摘要、requestId、端口配置和前序服务适配都有自动化测试；`.local-docs/tests-resource.md` 中全部测试用例都有对应自动化验证；未实现时自动化测试必须先失败；实现后 `resource` 全部测试通过；`auth`、`profile`、`notification`、`content` 和 `server-status` 前序服务回归测试通过；没有修改前序服务稳定接口；没有把后台文件管理、容器、终端、日志流、节点注册、external-node-executor、真实服务器操作或 ops-control 能力塞进 `resource`。

## 北冥官网 admin API 契约

来源：`docs/contracts-admin.md`

版本：0.3

### 文档定位

本文档是 `admin` 微服务的正式 API 契约。当前 `admin` 兼容刷新负责让后台入口识别已经闭环的 26 个后端服务，并把 `api-gateway` 作为平台依赖展示。后续前端管理后台只能通过本文档定义的接口读取后台聚合入口、模块能力、待办摘要、指标摘要、审计索引、平台依赖摘要和 admin 自有配置，不能把业务主数据或真实运维控制塞进 `admin`。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `admin` 模块自己的职责边界、路径、字段、状态、权限、错误码、幂等、降级、审计和验收口径。

本文档参考了现有前序服务契约和成熟在线平台的后台信息架构。GitLab Admin Area、Audit Events、Todos 和 Health Check 的设计强调后台总览、审计索引、待办队列和健康摘要分离。Grafana HTTP API 和 RBAC 文档强调健康检查与权限模型分离，适合 admin 自检和模块权限裁剪。Discourse 管理和审核队列强调运营待办只做审核入口，不把处理逻辑塞进总览。MCSManager 的面板和 Daemon 分离思路用于确认游戏服控制面边界，真实实例、文件、终端、容器和节点操作仍归 `ops-control` 与 `external-node-executor`。

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

### 职责边界

`admin` 负责管理后台统一入口、模块注册表、模块能力发现、后台导航配置、Dashboard 看板摘要、待办聚合摘要、审计索引、系统配置、模块健康摘要、平台依赖摘要和 admin 自身审计。

`admin` 不负责注册、登录、会话、角色能力点主数据、邀请码主数据、成员档案主数据、通知投递主数据、内容审核实现、资源审核实现、入服流程判定、考试判分、白名单审核、考勤积分计算、社区处罚、活动报名、日历事件、更新日志发布、素材文件安全、指南正文维护、Cloudreve 同步、备份恢复、告警规则、在线地图主数据、插件事件、跨平台通知、镜像市场、服务器状态采集、Cloudreve 分享生成、节点注册、容器启停、虚拟机控制、Minecraft 实例控制、文件管理、终端命令、日志流、高风险运维审批或任何真实服务器操作。

`admin` 只能通过正式 API、后端入口可信认证上下文或测试环境适配器读取各服务摘要。它不能直接读取或修改其他服务的数据库、内存存储、实体、Repository、测试种子或内部实现。

### 数据归属

`admin` 拥有以下主数据：模块注册表、模块入口显隐配置、后台导航配置、看板布局配置、系统配置项、待办聚合快照、模块健康摘要快照、平台依赖摘要快照、审计索引快照、幂等记录和 admin 自身审计日志。

`admin` 保存的待办、指标和审计索引都是聚合摘要或只读索引，不是来源模块主数据。来源模块的业务状态、审核状态、通知状态、资源状态、线路状态和审计主记录仍由来源模块负责。`admin` 不提供删除来源审计、改写来源业务状态或关闭来源待办的接口。

### 基础路径与认证

所有接口默认使用 `/api/v1/admin` 前缀，全部要求 `Authorization: Bearer <token>`。

后台读取接口要求当前用户具备 `HELPER`、`ADMIN` 或 `OWNER` 任一基础角色。系统配置读取、审计索引读取和 admin 自检摘要要求 `ADMIN` 或 `OWNER`。普通配置写操作要求 `ADMIN` 或 `OWNER`。全局高影响配置写操作只允许 `OWNER`。

`USER` 不能访问任何 admin 接口，返回 `42001`。未登录返回 `41000`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问 admin 接口，按认证上下文错误返回 `46703`、`46704` 或公共认证错误。

#### 可信认证上下文

生产环境中，`admin` 必须优先消费后端入口注入的可信身份头。浏览器请求体和普通客户端不得通过请求体覆盖操作者、角色、能力点、来源 IP、请求编号或模块摘要。没有可信身份头时，可以继续用 `Authorization` 交给认证适配器校验；本地固定 token 只允许在测试模式或本地开发模式启用。

| 请求头 | 必填 | 说明 |
| --- | --- | --- |
| `X-Beiming-Actor-User-Id` | 是 | 网关或后端入口从 auth 会话校验结果注入的当前用户 ID。 |
| `X-Beiming-Actor-Roles` | 是 | 逗号分隔基础角色，只接受 `OWNER`、`ADMIN`、`HELPER`、`USER`。 |
| `X-Beiming-Actor-Permissions` | 否 | 逗号分隔能力点，可为空字符串。 |
| `X-Gateway-Internal-Request-Id` | 否 | 网关注入的内部请求编号，用于链路关联；响应仍以 `X-Request-Id` 为对外请求编号。 |

可信身份头存在时，`admin` 必须按该上下文做角色和能力点裁剪。`roles` 为空、包含未知角色、缺少 `userId` 或字段格式不兼容时返回 `46703` 或 `46702`，不得回退到浏览器传入的伪造字段，也不得再用本地固定 token 兜底提升权限。

#### 测试钩子边界

`X-Test-Module-Mode`、`X-Test-Platform-Mode`、`X-Test-Fail-Audit` 和 `X-Test-Fail-Settings` 只允许在 admin 测试模式启用。生产模式必须忽略这些测试头，不能允许外部请求通过测试头伪造模块降级、平台降级、审计失败或配置写入失败。

### 来源服务兼容契约

`admin` 适配当前已经闭环的 26 个服务，不要求任何来源服务反向适配 `admin`。生产环境可以通过后端入口传入可信认证上下文和模块摘要，也可以调用来源服务正式后台接口。测试环境使用模块适配器 stub。

当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。浏览器请求体不得传入并覆盖当前操作者、角色、权限、来源 IP、请求编号、模块健康、待办来源、审计来源或来源业务摘要。

来源模块不可用时，聚合读取接口优先返回局部降级结果，并在 `degradedModules`、`moduleHealth` 或对应条目中标记 `DEGRADED` 或 `UNAVAILABLE`。只有认证上下文不可用、admin 自有存储不可用、字段不兼容导致无法构造契约响应时，才返回错误。

当前已闭环模块包括 `AUTH`、`PROFILE`、`NOTIFICATION`、`CONTENT`、`SERVER_STATUS`、`RESOURCE`、`ADMIN`、`ONBOARDING`、`EXAM`、`WHITELIST`、`ATTENDANCE`、`COMMUNITY`、`ACTIVITY`、`CALENDAR`、`CHANGELOG`、`OPS_CONTROL`、`CLOUDREVE_SYNC`、`BACKUP_RECOVERY`、`ALERTING`、`ONLINE_MAP`、`PLUGIN_INTEGRATION`、`CROSS_PLATFORM_NOTIFICATION`、`OPS_IMAGE_MARKET`、`MATERIAL` 和 `GUIDE`。这些模块在测试适配器正常时返回 `AVAILABLE`，适配器报告生产化缺口或局部失败时返回 `DEGRADED`，不可达或超时时返回 `UNAVAILABLE`。只有未来真正没有契约和服务目录的模块，才允许返回 `NOT_IMPLEMENTED`。

`API_GATEWAY` 不是业务模块，不参与普通业务待办和模块注册表。它只作为平台依赖摘要出现在总览和自检中，展示端口、健康、路由数量和生产化缺口，避免 admin 到 gateway 再回到 admin 的循环依赖。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `AdminModuleKey` | `AUTH`、`PROFILE`、`NOTIFICATION`、`CONTENT`、`SERVER_STATUS`、`RESOURCE`、`ADMIN`、`ONBOARDING`、`EXAM`、`WHITELIST`、`ATTENDANCE`、`COMMUNITY`、`ACTIVITY`、`CALENDAR`、`CHANGELOG`、`OPS_CONTROL`、`CLOUDREVE_SYNC`、`BACKUP_RECOVERY`、`ALERTING`、`ONLINE_MAP`、`PLUGIN_INTEGRATION`、`CROSS_PLATFORM_NOTIFICATION`、`OPS_IMAGE_MARKET`、`MATERIAL`、`GUIDE` | 管理后台模块键。 |
| `AdminModuleStatus` | `AVAILABLE`、`DEGRADED`、`UNAVAILABLE`、`NOT_IMPLEMENTED`、`DISABLED` | 模块在后台入口中的可用状态。 |
| `AdminCapabilityType` | `ENTRY`、`READ`、`WRITE`、`REVIEW`、`CONFIG`、`AUDIT`、`OPS_PLACEHOLDER`、`PLATFORM` | 模块能力类型。 |
| `AdminTodoType` | `REVIEW`、`CONFIG`、`FAILURE`、`HEALTH`、`SECURITY`、`FOLLOW_UP` | 待办类型。 |
| `AdminTodoSeverity` | `LOW`、`MEDIUM`、`HIGH`、`CRITICAL` | 待办严重程度。 |
| `AdminTodoStatus` | `OPEN`、`READ_ONLY`、`SOURCE_UNAVAILABLE`、`STALE` | 聚合待办当前可读状态。 |
| `AdminSettingScope` | `GLOBAL`、`MODULE`、`DASHBOARD`、`NAVIGATION`、`AUDIT` | admin 自有配置作用域。 |
| `AdminSettingValueType` | `STRING`、`BOOLEAN`、`INTEGER`、`JSON` | 配置值类型。 |
| `AdminAuditIndexSource` | `ADMIN`、`AUTH`、`PROFILE`、`NOTIFICATION`、`CONTENT`、`SERVER_STATUS`、`RESOURCE`、`ONBOARDING`、`EXAM`、`WHITELIST`、`ATTENDANCE`、`COMMUNITY`、`ACTIVITY`、`CALENDAR`、`CHANGELOG`、`OPS_CONTROL`、`CLOUDREVE_SYNC`、`BACKUP_RECOVERY`、`ALERTING`、`ONLINE_MAP`、`PLUGIN_INTEGRATION`、`CROSS_PLATFORM_NOTIFICATION`、`OPS_IMAGE_MARKET`、`MATERIAL`、`GUIDE`、`API_GATEWAY` | 审计索引来源。 |
| `AdminAuditResult` | `SUCCESS`、`FAILED` | admin 审计结果。 |

### 通用对象

#### AdminModuleEntry

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

#### AdminCapabilityEntry

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

#### AdminModuleHealth

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `moduleKey` | string | 是 | 模块键。 |
| `status` | string | 是 | `AdminModuleStatus`。 |
| `service` | string | 是 | 服务名。 |
| `port` | integer 或 null | 是 | 当前运行入口端口。当前已闭环模块必须为非空，未来未实现模块为 `null`。第二批 `ONBOARDING`、`EXAM`、`WHITELIST` 和 `ATTENDANCE` 已由 `admission-core-service` 承载，必须返回 `8131`；第六期后 `CROSS_PLATFORM_NOTIFICATION` 已由 `ops-core-service` 承载，必须返回 `8133`。历史原服务端口只保留在各自业务契约的 `legacyPort` 中。 |
| `storageMode` | string 或 null | 是 | 例如 `IN_MEMORY`。 |
| `authMode` | string 或 null | 是 | 认证适配模式。 |
| `lastCheckedAt` | string | 是 | 最近检查时间。 |
| `latencyMs` | integer 或 null | 是 | 最近检查耗时。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReason` | string 或 null | 是 | 降级原因，不能包含 token、密码、内部路径或原始异常堆栈。 |
| `productionGaps` | string[] | 是 | 生产化缺口摘要。 |

#### AdminPlatformDependency

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

#### AdminTodoItem

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

#### AdminMetricSummary

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

#### AdminAuditIndexEntry

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

#### AdminSettingItem

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

#### AdminSettingsSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `items` | AdminSettingItem[] | 是 | 配置项。 |
| `layout` | AdminLayoutConfig | 是 | 看板布局。 |
| `modules` | AdminModuleEntry[] | 是 | 模块入口配置摘要。 |
| `updatedAt` | string | 是 | 快照更新时间。 |

#### AdminLayoutConfig

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `dashboardCards` | string[] | 是 | 看板卡片顺序。 |
| `navigationModuleOrder` | string[] | 是 | 导航模块顺序。 |
| `hiddenModules` | string[] | 是 | 隐藏模块键。默认模块列表和总览不得返回被隐藏模块；只有 `OWNER` 使用 `includeDisabled=true` 时可以看到被隐藏模块，状态为 `DISABLED`。 |
| `quickActions` | AdminQuickAction[] | 是 | 快捷入口，只能指向已实现且当前用户有权访问的来源模块路由。 |

#### AdminQuickAction

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `key` | string | 是 | 快捷入口键，格式为小写字母、数字和短横线，同一布局内唯一。 |
| `targetRoute` | string | 是 | 必须指向已实现模块的后台入口或其下级路由，例如 `/admin/content`。不得指向未实现模块、运维真实操作、外部 URL 或 `/api` 路径。 |

#### AdminOverview

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

#### AdminOpsSummary

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

### admin 错误码

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

### 接口总览

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

### 总览接口

#### 管理后台总览

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

### 模块接口

#### 模块注册表

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

业务规则：已实现模块必须包含 `AUTH`、`PROFILE`、`NOTIFICATION`、`CONTENT`、`SERVER_STATUS`、`RESOURCE`、`ADMIN`、`ONBOARDING`、`EXAM`、`WHITELIST`、`ATTENDANCE`、`COMMUNITY`、`ACTIVITY`、`CALENDAR`、`CHANGELOG`、`OPS_CONTROL`、`CLOUDREVE_SYNC`、`BACKUP_RECOVERY`、`ALERTING`、`ONLINE_MAP`、`PLUGIN_INTEGRATION`、`CROSS_PLATFORM_NOTIFICATION`、`OPS_IMAGE_MARKET`、`MATERIAL` 和 `GUIDE`。这些模块正常时 `implemented=true` 且 `status=AVAILABLE`，`targetApiBase` 必须指向对应正式 API 前缀。`CROSS_PLATFORM_NOTIFICATION` 的前端入口仍为 `/admin/cross-platform-notification`，后台 API 仍为 `/api/v1/cross-platform-notification/admin`，健康端口必须返回当前入口 `8133`，历史端口 `8123` 只留在模块契约或追溯字段中。未实现状态只留给未来没有正式契约和服务目录的模块。被 `hiddenModules` 隐藏的模块视为 admin 自有配置禁用，默认不返回；只有 `OWNER` 传 `includeDisabled=true` 时可返回，且状态必须为 `DISABLED`。

#### 模块详情

`GET /api/v1/admin/modules/{moduleKey}`

成功响应 HTTP `200`，`data` 为 `AdminModuleEntry`。

业务规则：`moduleKey` 必须是 `AdminModuleKey`。不存在或非法返回 `40001` 或 `43700`。当前用户无权查看该模块入口时返回 `42001`。当前 26 个已闭环服务详情不得返回 `NOT_IMPLEMENTED`。未来未实现模块详情可以返回 `NOT_IMPLEMENTED`，但不能返回真实业务指标、待办或写能力。

降级规则：来源模块自检失败时，详情仍返回入口配置，`health.status` 为 `DEGRADED` 或 `UNAVAILABLE`，`capabilities[].available=false`。

### 待办接口

#### 待办聚合列表

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

业务规则：待办只聚合已实现来源模块和 admin 自身的只读摘要。`content` 可产生待审核内容、首页草稿未发布、SEO 配置异常。`resource` 可产生待审核资源、下载入口过期、Cloudreve 降级。`notification` 可产生模板禁用、投递失败、外部投递缺口。`server-status` 可产生未确认宕机、采集失败、线路异常。`profile` 可产生待激活成员或成员资料异常。`auth` 可产生管理员邀请码风险、禁用用户安全事件或会话异常摘要。`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、`material` 和 `guide` 可以返回审核、补充、反馈、活动、日程、素材或指南类只读待办。`ops-control`、`external-node-executor`、`cloudreve-sync`、`backup-recovery`、`alerting`、`online-map`、`plugin-integration`、`cross-platform-notification` 和 `ops-image-market` 只能返回健康、审批、同步、告警或兼容计划类摘要，不能让 admin 代替它们执行真实操作。`API_GATEWAY` 不产生普通业务待办。

待办处理动作不在 `admin` 内完成。`readOnly` 必须为 `true`，前端根据 `targetRoute` 和 `targetApi` 跳回来源模块。

降级规则：来源模块不可用时，该来源模块待办可以省略，也可以返回 `SOURCE_UNAVAILABLE` 占位待办，但必须标记 `degraded=true` 或 `status=SOURCE_UNAVAILABLE`，不能伪造真实数量。

#### 待办详情

`GET /api/v1/admin/todos/{todoId}`

成功响应 HTTP `200`，`data` 为 `AdminTodoItem`，可补充 `context` 对象。

业务规则：`context` 只能包含只读摘要，例如来源状态、来源标题、来源更新时间、建议入口和下一步说明。不得包含来源模块后台备注全文、通知正文、模板正文、Cloudreve 分享密码、内部路径、token、请求头或审计参数全文。待办不存在或当前用户无权查看来源模块时返回 `43701` 或 `42001`。

### 指标接口

#### 看板指标摘要

`GET /api/v1/admin/metrics/summary`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `sourceModule` | string | 否 | 任一已实现模块。 |
| `includeDegraded` | boolean | 否 | 默认 `true`。 |

成功响应 HTTP `200`，`data.items` 为 `AdminMetricSummary[]`。

业务规则：指标摘要可以包括用户、成员、通知、内容、服务器状态、资源、入服、考试、白名单、考勤、社区、活动、日历、更新日志、运维控制、节点守护、Cloudreve 同步、备份恢复、告警、在线地图、插件集成、跨平台通知、镜像市场、素材、指南和 admin 自身的计数或健康摘要。指标只用于后台看板，不作为业务判定来源。未知或降级指标不得伪装成真实值。

降级规则：部分模块不可用时，其他模块指标正常返回；不可用模块指标返回 `value=0`、`degraded=true` 或被省略，并在响应中保留模块健康摘要。不得把未知值伪造成真实 0 而不标记降级。

### 审计索引接口

#### 审计索引列表

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

### 系统配置接口

#### 系统配置快照

`GET /api/v1/admin/settings`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `scope` | string | 否 | 任一 `AdminSettingScope`。 |
| `includeHighImpact` | boolean | 否 | 默认 `false`。只有 `OWNER` 可传 `true`。 |

成功响应 HTTP `200`，`data` 为 `AdminSettingsSnapshot`。

业务规则：只返回 admin 自有配置，例如模块菜单显隐、导航排序、看板卡片排序、快捷入口、模块降级展示策略、平台依赖展示策略、审计索引保留天数和聚合刷新间隔。`quickActions` 必须按当前用户权限和模块显隐过滤，不能返回当前用户无权访问、已隐藏、未实现或不可用模块的入口。不得返回或修改任何来源服务的业务配置、运维配置、节点配置、外部平台凭据或素材上传配置。

敏感字段：不允许通过该接口返回敏感明文。若未来出现敏感配置，只能返回 `sensitive=true` 和脱敏摘要。

#### 更新系统配置

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

### 自检接口

#### admin 自检摘要

`GET /api/v1/admin/ops/summary`

成功响应 HTTP `200`，`data` 为 `AdminOpsSummary`，可补充 `moduleHealth` 数组。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。

业务规则：自检摘要用于确认 admin 当前运行模式、模块适配状态、平台依赖状态、配置规模、待办索引规模、审计索引规模和生产化缺口。当前可返回 `storageMode=IN_MEMORY`、`authMode=TEST_STUB`、`moduleAdapterMode=TEST_STUB`。`productionGaps` 至少说明真实持久化、真实认证适配、真实来源模块 HTTP 适配、真实审计索引同步、真实定时聚合是否未启用。`platformDependencies` 必须包含 `API_GATEWAY`，但不得把 gateway 当作业务待办来源。

生产化边界：自检摘要必须返回 `testMode`。测试模式下 `testMode=true`，允许测试钩子驱动降级和失败分支；生产模式下 `testMode=false`，测试钩子不生效。`authMode` 在消费可信身份头时应返回 `TRUSTED_GATEWAY_CONTEXT` 或等价生产适配名，不能继续声称只有 `TEST_STUB`。

敏感字段：自检摘要不得返回 token、密码、请求头、邀请码原文、Cloudreve 密码、内部文件路径、后台备注全文、审计参数全文、节点密钥、服务器系统路径或异常堆栈。

### 状态、幂等和并发

模块状态由 admin 自有配置和模块适配器结果共同决定。模块未实现时为 `NOT_IMPLEMENTED`。模块被 admin 自有配置关闭时为 `DISABLED`。模块适配器成功返回兼容摘要时为 `AVAILABLE`。适配器部分失败或来源模块自检报告降级时为 `DEGRADED`。适配器不可达或超时时为 `UNAVAILABLE`。当前 26 个已闭环服务默认不允许被标记为 `NOT_IMPLEMENTED`。

配置更新支持 `idempotencyKey`。同一操作者、同一幂等键、同一请求体重复提交时返回同一个结果；相同幂等键搭配不同请求体返回 `43712`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象都要按字段名递归排序，不能受任意层级的字段顺序影响。

配置更新、审计写入和幂等记录写入必须保持一致。并发更新同一配置时必须以服务端当前状态为准，不能产生半更新快照。实现可以使用版本号、更新时间或事务锁保证配置键唯一和幂等记录唯一。

### 审计要求

必须审计的动作包括系统配置更新、高影响配置更新、模块显隐调整、看板布局更新、审计索引策略更新、聚合适配失败被管理员触发刷新时的失败记录。后台低风险读取不强制写审计。

审计字段继承公共契约。admin 自身审计写入失败时，配置写操作不得假装成功，必须返回 `51701` 并保持业务数据不变。审计索引读取失败不得删除来源审计，也不得修改来源模块主数据。

### 失败降级

总览、模块列表、模块详情、待办列表、指标摘要和审计索引读取都应支持局部降级。单个来源模块不可用时，不影响其他模块入口和 admin 自有配置读取。响应必须清楚标记降级模块，不能把未知值当真实值返回。

认证上下文不可用、认证上下文超时、admin 自有配置存储不可用、admin 审计写入失败、配置写入失败和响应字段无法满足契约时，不允许伪造成功。

`admin` 不得因为某个来源模块或 `api-gateway` 不可用而整体失败。来源模块降级时返回局部降级摘要；`API_GATEWAY` 降级时只影响平台依赖摘要，不影响普通模块注册表。

### 验收口径

`admin` API 文档按 `docs/contracts-admin.md` 独立存在，并由 `.local-docs/tests-admin.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`admin` 完成时必须满足以下条件：全部接口按本文档实现；后台接口全部要求登录；`USER` 不能访问 admin；`HELPER` 只能读允许的聚合入口、待办和指标；`ADMIN` 可读审计和改普通配置；`OWNER` 才能改高影响配置；总览、模块、待办、指标、审计索引、设置和自检都不泄露敏感字段；当前 26 个已闭环服务全部被识别为已实现；`API_GATEWAY` 只作为平台依赖摘要出现；来源模块不可用时局部降级；可信认证上下文优先于本地测试 token；生产模式下测试钩子不生效；配置更新幂等、审计和回滚有测试；端口固定为 `8107`；`.local-docs/tests-admin.md` 中全部测试用例都有对应自动化验证；未实现或行为未满足时自动化测试必须先失败；实现后 `admin` 全部测试通过；`api-gateway` 和 26 个后端服务回归测试通过；没有修改来源服务稳定接口；没有把业务写代理、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复、Cloudreve 管理 token、外部平台凭据或真实运维执行能力塞进 `admin`。

## 北冥官网 onboarding API 契约

来源：`docs/contracts-onboarding.md`

版本：0.2

### 文档定位

本文档是 `onboarding` 微服务的正式 API 契约。后续 `exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、前端适配和 `ops-control` 只能通过本文档定义的接口读取或维护入服引导状态，不能直接读取或修改 `onboarding` 数据库，也不能把考试、白名单、成员激活或考勤积分逻辑塞进 `onboarding`。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `onboarding` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、降级、审计和验收口径。

本文档参考了成熟平台的 onboarding 和流程状态设计。Stripe Connect onboarding 强调由服务端读取当前要求、生成下一步组件、处理状态和校验，避免前端自行判定完成状态。Discord Community Onboarding 和 Rules Screening 强调新成员先完成必要规则确认，再做个性化方向选择，并避免一次塞入太多问题。Jira Workflow 强调状态、单向流转、条件、校验器和后置动作的分离。本文档只吸收这些适合北冥入服流程的做法，不引入支付、Discord 角色分配或 Jira 工单模型。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Stripe Embedded onboarding](https://docs.stripe.com/connect/embedded-onboarding?locale=en-GB) | onboarding 应由服务端根据要求生成下一步和校验状态，前端只渲染入口。 |
| [Stripe onboarding configuration](https://docs.stripe.com/connect/onboarding?locale=en-GB) | onboarding 可以分为托管、嵌入和自定义 API，北冥选择自有 API，但仍保留服务端状态判定。 |
| [Discord Community Onboarding FAQ](https://support.discord.com/hc/en-us/articles/11074987197975-Community-Onboarding-FAQ) | 新成员流程应包含默认入口、简单问题、必要项和完成后的下一步。 |
| [Discord Rules Screening FAQ](https://support.discord.com/hc/en-us/articles/1500000466882-Rules-Screening-FAQ) | 规则确认应作为明确门槛，确认前不能进入后续参与步骤。 |
| [Atlassian Jira workflows overview](https://www.atlassian.com/software/jira/guides/workflows/overview) | 流程由状态和单向流转组成，非法跳转必须被拒绝。 |
| [Atlassian workflow validators](https://support.atlassian.com/jira-cloud-administration/docs/use-workflow-validators-with-custom-fields/) | 状态推进前必须先做输入和条件校验，校验失败不得执行后置动作。 |

### 职责边界

`onboarding` 负责新玩家从注册后到进入考试前的入服引导状态。它保存流程实例、步骤完成状态、资料确认、规则确认、审核方向选择、流程阻塞原因、下一步入口、后续模块占位、幂等记录和自身审计。

`onboarding` 不负责注册、登录、邀请码、会话、账号角色、Minecraft 绑定验证、成员档案主数据、考试题库、试卷、判分、人工阅卷、白名单申请、白名单审核、成员激活、考勤积分、通知主数据、内容主数据、资源下载、服务器状态采集或真实运维操作。

`onboarding` 可以读取前序模块摘要，但不能要求前序模块反向适配。它只能适配 `auth`、`profile`、`notification` 和 `content` 的正式契约或后端入口传入的可信上下文。`server-status`、`resource` 和 `admin` 不是 onboarding 状态判定依赖。

### 数据归属

`onboarding` 拥有以下主数据：入服流程实例、步骤状态、资料确认记录、规则版本确认记录、审核方向选择、流程阻塞记录、后续模块移交摘要、幂等记录、通知调用摘要和 onboarding 审计日志。

`onboarding` 可以保存当前用户快照、Minecraft 绑定快照、成员档案摘要、规则内容摘要和通知投递结果摘要。快照不是来源模块主数据，不能用于替代来源模块权限判断，也不能反写 `auth`、`profile`、`notification` 或 `content`。

### 基础路径与认证

所有接口默认使用 `/api/v1/onboarding` 前缀。

当前用户接口使用 `/api/v1/onboarding/me` 前缀，全部要求 `Authorization: Bearer <token>`，只能读取或维护当前认证用户自己的入服流程。浏览器请求体不得传入 `userId`、`roles`、`permissions`、`minecraftId`、`minecraftUuid`、`status`、`currentStep`、`createdBy`、`updatedBy`、`blockedBy` 等服务端可信字段。

后台接口使用 `/api/v1/onboarding/admin` 前缀，全部要求登录。后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`。后台写操作要求 `ADMIN` 或 `OWNER`，必须携带 `reason` 并写入审计。`HELPER` 可以查看流程和审计摘要，但不能重置、阻塞或解除阻塞。

第二批合并后当前运行入口由 `admission-core-service` 承载，端口固定为 `8131`。历史原服务端口 `8108` 只作为 `legacyPort` 返回，不作为当前运行入口、网关上游或测试入口。

### 本地测试控制头

`onboarding` 允许在本地自动化测试中使用 `X-Test-Dependency-Mode`、`X-Test-Notification-Mode`、`X-Test-Fail-Audit` 和 `X-Test-Fail-Store` 模拟依赖失败、通知失败、审计失败和状态写入失败。生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、状态失败或通知失败。

### 网关可信身份上下文

只有 `X-Gateway-Internal-Request-Id` 存在时，`onboarding` 才进入可信上下文解析；若该头缺失，即使请求带有 `X-Beiming-Actor-*`，也必须忽略这些头并继续走 `Authorization: Bearer <token>` 兼容路径。可信上下文缺少 `X-Beiming-Actor-User-Id`、角色枚举不兼容或字段无法解析时返回 HTTP `502` 和 `46802`。

### 前序服务兼容契约

`onboarding` 适配 `auth`。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions`、`status` 和 `minecraftBinding`。`minecraftBinding` 结构必须兼容 `docs/contracts-auth.md`。用户状态为 `PENDING_PROFILE` 或 `ACTIVE` 时允许启动或读取流程；`DISABLED`、`BANNED`、`DELETED` 不允许启动、确认或推进流程。auth 不可用返回 `46800`，auth 超时返回 `46801`，auth 字段或枚举不兼容返回 `46802`。

`onboarding` 可以适配 `profile` 读取当前用户成员档案摘要。用户已有 `ACTIVE` 或 `INACTIVE` 成员档案时，默认不允许开启新玩家流程，返回 `43812`，除非后续 `exam` 和 `whitelist` 契约明确引入二次入服流程。profile 不可用时，读取进度可以返回降级摘要；涉及资料确认、成员状态判定和推进的写操作必须返回 `46810`、`46811` 或 `46812`，不得伪造成员状态。

`onboarding` 可以适配 `content` 获取当前生效规则摘要。规则摘要至少包含 `ruleContentId`、`ruleVersion`、`title`、`updatedAt` 和 `guideRoute`。content 不可用时，读取进度可以使用已缓存规则摘要并标记降级；没有缓存时，规则确认和推进必须返回 `46820` 或 `46821`。content 字段不兼容返回 `46822`。

`onboarding` 可以适配 `notification` 在关键状态变化后投递站内通知。通知是辅助动作，除后台重置、阻塞和解除阻塞明确要求通知目标用户外，通知失败不得回滚用户主流程，但必须记录 `notificationStatus=FAILED` 和失败原因摘要。通知强制投递不可用返回 `46830`，通知超时返回 `46831`，字段不兼容返回 `46832`。

后续 `exam` 和 `whitelist` 未实现时，`onboarding` 仍可把流程推进到 `READY_FOR_EXAM`，并在下一步入口中返回 `targetModuleStatus=NOT_IMPLEMENTED` 或 `WAITING_MODULE`。它不能伪造考试已开始、考试已通过、白名单已申请、白名单已通过、成员已激活或积分已初始化。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `OnboardingStatus` | `NOT_STARTED`、`IN_PROGRESS`、`BLOCKED`、`READY_FOR_EXAM`、`WAITING_EXAM`、`READY_FOR_WHITELIST`、`WAITING_WHITELIST`、`COMPLETED`、`CANCELLED` | 入服流程状态。P0 实现到 `READY_FOR_EXAM`，后续状态作为兼容占位，不得伪造下游结果。 |
| `OnboardingStepKey` | `ACCOUNT_READY`、`MINECRAFT_BOUND`、`PROFILE_CONFIRMED`、`RULES_CONFIRMED`、`DIRECTION_SELECTED`、`EXAM_READY`、`WHITELIST_READY` | 入服步骤。 |
| `OnboardingStepStatus` | `LOCKED`、`AVAILABLE`、`COMPLETED`、`BLOCKED`、`WAITING_MODULE` | 单步状态。 |
| `ReviewDirection` | `REDSTONE`、`LATE_GAME`、`BUILDING`、`GENERAL` | 审核方向，对应红石、后期、建筑和通用。 |
| `TargetModuleStatus` | `AVAILABLE`、`NOT_IMPLEMENTED`、`DEGRADED`、`UNAVAILABLE`、`WAITING_MODULE` | 下一步目标模块状态。 |
| `OnboardingAuditResult` | `SUCCESS`、`FAILED` | onboarding 审计结果。 |

### 通用对象

#### OnboardingApplication

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `applicationId` | string | 是 | 入服流程实例 ID。 |
| `userId` | string | 是 | auth 用户 ID。当前用户接口中固定为认证用户。 |
| `displayNameSnapshot` | string | 是 | auth 展示名快照。 |
| `authStatusSnapshot` | string | 是 | 最近一次读取 auth 时的用户状态。 |
| `minecraftBindingSnapshot` | object 或 null | 是 | Minecraft 绑定快照，字段兼容 auth 的 `MinecraftBinding`。 |
| `profileSummary` | OnboardingProfileSummary 或 null | 是 | profile 摘要。 |
| `status` | string | 是 | `OnboardingStatus`。 |
| `currentStep` | string | 是 | 当前建议处理步骤。 |
| `steps` | OnboardingStep[] | 是 | 步骤状态列表。 |
| `reviewDirection` | string 或 null | 是 | 审核方向。 |
| `ruleConfirmation` | RuleConfirmation 或 null | 是 | 规则确认记录。 |
| `profileConfirmation` | ProfileConfirmation 或 null | 是 | 资料确认记录。 |
| `nextAction` | OnboardingNextAction | 是 | 下一步入口。 |
| `blockedReason` | string 或 null | 是 | 阻塞原因摘要。 |
| `blockedBy` | string 或 null | 是 | 阻塞操作者用户 ID。 |
| `blockedAt` | string 或 null | 是 | 阻塞时间。 |
| `notificationStatus` | string 或 null | 是 | 最近一次通知摘要，允许 `DELIVERED`、`FAILED`、`SKIPPED`。 |
| `degraded` | boolean | 是 | 是否存在局部降级。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `completedAt` | string 或 null | 是 | 完成时间。P0 不会因下游未实现而写入。 |
| `cancelledAt` | string 或 null | 是 | 取消时间。 |

#### OnboardingStep

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `key` | string | 是 | `OnboardingStepKey`。 |
| `status` | string | 是 | `OnboardingStepStatus`。 |
| `title` | string | 是 | 前端展示标题。 |
| `required` | boolean | 是 | 是否必做。 |
| `completedAt` | string 或 null | 是 | 完成时间。 |
| `blockReason` | string 或 null | 是 | 该步骤阻塞原因。 |
| `targetRoute` | string 或 null | 是 | 前端建议入口。 |
| `targetApi` | string 或 null | 是 | 对应 API 或后续模块接口。未实现模块可为 `null`。 |

#### ProfileConfirmation

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `confirmed` | boolean | 是 | 是否已确认资料。 |
| `displayNameSnapshot` | string | 是 | 确认时的展示名快照。 |
| `minecraftIdSnapshot` | string 或 null | 是 | 确认时的 Minecraft ID。 |
| `minecraftUuidSnapshot` | string 或 null | 是 | 确认时的 Minecraft UUID。 |
| `confirmedAt` | string | 是 | 确认时间。 |

#### RuleConfirmation

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `confirmed` | boolean | 是 | 是否已确认规则。 |
| `ruleContentId` | string | 是 | content 中规则或指南内容 ID。 |
| `ruleVersion` | string | 是 | 规则版本。 |
| `ruleTitle` | string | 是 | 规则标题。 |
| `guideRoute` | string | 是 | 前端规则页面路由。 |
| `confirmedAt` | string | 是 | 确认时间。 |

#### OnboardingProfileSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `memberId` | string | 是 | profile 成员档案 ID。 |
| `status` | string | 是 | profile 成员状态。 |
| `displayName` | string | 是 | 成员展示名快照。 |
| `minecraftId` | string 或 null | 是 | Minecraft 展示 ID。 |
| `snapshotAt` | string | 是 | 快照时间。 |

#### OnboardingNextAction

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `step` | string | 是 | 建议处理步骤。 |
| `label` | string | 是 | 前端展示文案。 |
| `targetRoute` | string 或 null | 是 | 前端路由。 |
| `targetApi` | string 或 null | 是 | 当前模块或后续模块 API。 |
| `targetModule` | string | 是 | `ONBOARDING`、`EXAM` 或 `WHITELIST`。 |
| `targetModuleStatus` | string | 是 | `TargetModuleStatus`。 |
| `enabled` | boolean | 是 | 当前是否可点击进入。 |
| `disabledReason` | string 或 null | 是 | 不可用原因。 |

#### OnboardingExamHandoffSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `applicationId` | string | 是 | 入服流程实例 ID。 |
| `userId` | string | 是 | auth 用户 ID。 |
| `displayNameSnapshot` | string | 是 | auth 展示名快照。 |
| `minecraftBindingSnapshot` | object | 是 | Minecraft 绑定快照，字段兼容 auth 的 `MinecraftBinding`。 |
| `profileConfirmation` | ProfileConfirmation | 是 | 资料确认记录。 |
| `ruleConfirmation` | RuleConfirmation | 是 | 规则确认记录。 |
| `reviewDirection` | string | 是 | 审核方向。 |
| `status` | string | 是 | 当前必须为 `READY_FOR_EXAM`。 |
| `readyForExam` | boolean | 是 | 是否满足 exam 可读取前置条件。 |
| `handoffAllowed` | boolean | 是 | 是否允许后续 exam 读取并创建考试流程。P0 在 exam 未实现时仍可为 `true`，但不代表考试已开始。 |
| `targetModule` | string | 是 | 固定为 `EXAM`。 |
| `targetModuleStatus` | string | 是 | P0 固定为 `NOT_IMPLEMENTED`，后续 exam 闭环后可变更为 `AVAILABLE` 或 `WAITING_MODULE`。 |
| `blocked` | boolean | 是 | 当前流程是否被后台阻塞。 |
| `blockedReason` | string 或 null | 是 | 阻塞摘要。 |
| `handoffVersion` | integer | 是 | 交接快照版本，从 `1` 开始，后续字段兼容扩展时递增。 |
| `generatedAt` | string | 是 | 生成快照时间。 |

#### OnboardingAuditLog

审计字段继承公共契约，允许补充 `applicationId`、`stateFrom`、`stateTo`、`stepKey`、`reviewDirection`、`ruleVersion`、`idempotencyKey`、`notificationStatus` 和 `dependencyStatus`。审计日志不得通过 onboarding API 删除。

### onboarding 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `43800` | 404 | 入服流程不存在，或当前用户无权访问该流程。 |
| `43801` | 404 | 规则摘要不存在或不可确认。 |
| `43802` | 404 | 审计记录不存在。 |
| `43810` | 409 | 当前流程状态不允许该操作。 |
| `43811` | 409 | 步骤前置条件未满足。 |
| `43812` | 409 | 当前用户已有成员档案，不允许开启新玩家流程。 |
| `43813` | 409 | Minecraft 身份未绑定或绑定快照不完整。 |
| `43814` | 409 | 规则版本已过期，需要重新读取并确认。 |
| `43815` | 409 | 审核方向不允许修改。 |
| `43816` | 409 | 流程已被后台阻塞。 |
| `43817` | 409 | onboarding 幂等键请求指纹冲突。 |
| `43818` | 409 | 后续模块未实现，不能继续推进到下游已完成状态。 |
| `46800` | 502 | auth 认证上下文不可用。 |
| `46801` | 504 | auth 认证上下文调用超时。 |
| `46802` | 502 | auth 认证上下文字段或枚举不兼容 onboarding 契约。 |
| `46810` | 502 | profile 摘要不可用。 |
| `46811` | 504 | profile 摘要调用超时。 |
| `46812` | 502 | profile 摘要字段或枚举不兼容 onboarding 契约。 |
| `46820` | 502 | content 规则摘要不可用。 |
| `46821` | 504 | content 规则摘要调用超时。 |
| `46822` | 502 | content 规则摘要字段不兼容 onboarding 契约。 |
| `46830` | 502 | notification 强制投递不可用。 |
| `46831` | 504 | notification 强制投递超时。 |
| `46832` | 502 | notification 投递响应不兼容 onboarding 契约。 |
| `51800` | 500 | onboarding 内部错误。 |
| `51801` | 500 | onboarding 审计写入失败。 |
| `51802` | 500 | onboarding 状态写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误、通用幂等键冲突和通用服务端错误优先使用公共错误码。onboarding 自有幂等指纹冲突使用 `43817`。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 当前用户进度 | GET | `/api/v1/onboarding/me/progress` | 是 | 当前用户 | LOW |
| 创建或恢复流程 | POST | `/api/v1/onboarding/me/start` | 是 | 当前用户 | LOW |
| 确认账号和 Minecraft 资料 | PATCH | `/api/v1/onboarding/me/profile-confirmation` | 是 | 当前用户 | LOW |
| 确认阅读规则 | PATCH | `/api/v1/onboarding/me/rules-confirmation` | 是 | 当前用户 | LOW |
| 选择审核方向 | PATCH | `/api/v1/onboarding/me/direction` | 是 | 当前用户 | LOW |
| 推进下一步 | POST | `/api/v1/onboarding/me/advance` | 是 | 当前用户 | LOW |
| 当前用户下一步入口 | GET | `/api/v1/onboarding/me/next-action` | 是 | 当前用户 | LOW |
| 后台流程列表 | GET | `/api/v1/onboarding/admin/applications` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台流程详情 | GET | `/api/v1/onboarding/admin/applications/{applicationId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 考试交接快照 | GET | `/api/v1/onboarding/admin/applications/{applicationId}/exam-handoff` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 后台重置流程 | PATCH | `/api/v1/onboarding/admin/applications/{applicationId}/reset` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台阻塞流程 | PATCH | `/api/v1/onboarding/admin/applications/{applicationId}/block` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台解除阻塞 | PATCH | `/api/v1/onboarding/admin/applications/{applicationId}/unblock` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| onboarding 审计列表 | GET | `/api/v1/onboarding/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| onboarding 自检摘要 | GET | `/api/v1/onboarding/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

### 当前用户接口

#### 当前用户进度

`GET /api/v1/onboarding/me/progress`

成功响应 HTTP `200`，`data` 为 `OnboardingApplication`。当前用户没有流程时返回一个只读进度视图，`applicationId=null`、`status=NOT_STARTED`、`currentStep=ACCOUNT_READY`，不得创建流程。

业务规则：必须从 auth 获取当前用户和 Minecraft 绑定状态。profile 不可用时允许返回进度并标记 `degraded=true`，但 `PROFILE_CONFIRMED` 不得被伪造成完成。content 规则摘要不可用且无缓存时，规则步骤为 `BLOCKED`。已有成员档案且状态为 `ACTIVE` 或 `INACTIVE` 时，下一步返回已是成员的说明，不自动创建新流程。

#### 创建或恢复流程

`POST /api/v1/onboarding/me/start`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `OnboardingApplication`。若流程已存在且未取消，返回 HTTP `200` 和现有流程。

业务规则：未绑定 Minecraft 身份时允许创建流程，但 `MINECRAFT_BOUND` 步骤为 `BLOCKED`，后续确认和推进不得跳过。已有 `ACTIVE` 或 `INACTIVE` 成员档案返回 `43812`。用户状态不允许启动时返回认证或状态错误。创建流程必须写入 `ONBOARDING_STARTED` 审计，审计失败返回 `51801`，不得创建流程。

幂等规则：同一用户、同一 `idempotencyKey`、同一请求体重复提交时返回同一流程。相同幂等键搭配不同请求体返回 `43817`。

#### 确认账号和 Minecraft 资料

`PATCH /api/v1/onboarding/me/profile-confirmation`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `confirmed` | boolean | 是 | 必须为 `true`。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为 `OnboardingApplication`。

业务规则：流程必须已创建且未被阻塞。auth 必须存在完整 Minecraft 绑定，否则返回 `43813`。profile 不可用、已有正式成员档案或字段不兼容时不得确认。确认只保存服务端读取到的展示名和 Minecraft 快照，浏览器传入同名字段必须忽略或返回字段校验失败。重复确认同一快照返回成功并保持幂等。绑定快照发生变化时，必须重新确认并记录新快照。

#### 确认阅读规则

`PATCH /api/v1/onboarding/me/rules-confirmation`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `confirmed` | boolean | 是 | 必须为 `true`。 |
| `ruleContentId` | string | 是 | 前端展示的规则内容 ID，必须等于服务端当前规则摘要。 |
| `ruleVersion` | string | 是 | 前端确认的规则版本，必须等于服务端当前规则版本或服务端允许的缓存版本。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为 `OnboardingApplication`。

业务规则：流程必须已创建且未被阻塞。content 当前规则不可用且没有可用缓存时返回 `46820` 或 `46821`。提交的规则版本不是当前版本或允许缓存版本时返回 `43814`，不得写确认。规则确认只表示用户读过规则，不代表考试或白名单通过。

#### 选择审核方向

`PATCH /api/v1/onboarding/me/direction`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reviewDirection` | string | 是 | `REDSTONE`、`LATE_GAME`、`BUILDING`、`GENERAL`。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为 `OnboardingApplication`。

业务规则：流程必须已创建且未被阻塞。资料确认和规则确认必须已完成，否则返回 `43811`。方向首次选择后允许在进入 `READY_FOR_EXAM` 前修改；一旦状态达到 `READY_FOR_EXAM`、`WAITING_EXAM` 或更后状态，修改方向返回 `43815`。方向只作为后续 exam 的输入摘要，onboarding 不生成试卷。

#### 推进下一步

`POST /api/v1/onboarding/me/advance`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为 `OnboardingApplication`。

业务规则：推进由服务端根据 auth、profile、content、资料确认、规则确认和方向选择计算。流程未创建返回 `43800`。被阻塞返回 `43816`。资料、规则或方向缺失返回 `43811`。全部前置步骤完成后，P0 将状态推进为 `READY_FOR_EXAM`，`EXAM_READY` 步骤为 `WAITING_MODULE`，下一步入口指向 exam 占位并返回 `targetModuleStatus=NOT_IMPLEMENTED`。不得推进到 `WAITING_EXAM`、`READY_FOR_WHITELIST`、`WAITING_WHITELIST` 或 `COMPLETED`，除非后续模块正式契约和实现已经闭环。

#### 当前用户下一步入口

`GET /api/v1/onboarding/me/next-action`

成功响应 HTTP `200`，`data` 为 `OnboardingNextAction`。

业务规则：下一步入口只由服务端状态计算。未开始时指向 `/onboarding/start`。资料未确认时指向资料确认。规则未确认时指向规则页。方向未选时指向方向选择。准备考试时指向 `/exam/start`，但在 exam 未实现时 `enabled=false`、`targetModuleStatus=NOT_IMPLEMENTED`。被阻塞时 `enabled=false` 并返回阻塞摘要。

### 后台接口

#### 后台流程列表

`GET /api/v1/onboarding/admin/applications`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配用户 ID、展示名、Minecraft ID 或流程 ID，最多 80 位。 |
| `status` | string | 否 | 任一 `OnboardingStatus`。 |
| `reviewDirection` | string | 否 | 任一 `ReviewDirection`。 |
| `blocked` | boolean | 否 | 是否只看阻塞流程。 |
| `sort` | string | 否 | 允许 `updatedAt_desc`、`createdAt_desc`、`status_asc`、`displayName_asc`。默认 `updatedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `OnboardingApplication[]`。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。未登录返回 `41000`。

#### 后台流程详情

`GET /api/v1/onboarding/admin/applications/{applicationId}`

成功响应 HTTP `200`，`data` 为 `OnboardingApplication`。流程不存在返回 `43800`。后台详情不得返回访问令牌、完整请求头、Minecraft 验证凭据、通知正文、content 正文、后台审计参数全文或异常堆栈。

#### 考试交接快照

`GET /api/v1/onboarding/admin/applications/{applicationId}/exam-handoff`

成功响应 HTTP `200`，`data` 为 `OnboardingExamHandoffSnapshot`。

权限规则：只有 `ADMIN` 和 `OWNER` 可读取。`HELPER` 返回 `42001`，未登录返回 `41000`。

业务规则：该接口只提供后续 `exam` 创建考试流程所需的只读快照，不创建试卷，不推进 onboarding 状态，不写考试记录，不写白名单申请。流程不存在返回 `43800`。流程被后台阻塞返回 `43816`。流程未达到 `READY_FOR_EXAM`、资料确认缺失、规则确认缺失、审核方向缺失、Minecraft 绑定快照不完整或规则版本已过期时返回 `43811` 或 `43814`，不得返回可用交接快照。P0 因 exam 未实现时，`targetModuleStatus` 固定为 `NOT_IMPLEMENTED`，但 `handoffAllowed` 可以为 `true`，表示 onboarding 自身前置条件已经满足。

降级规则：auth 是强依赖，认证上下文不可用、超时或字段不兼容时返回 `46800`、`46801` 或 `46802`。content 当前规则不可用时，如果无法确认已保存规则版本仍有效，返回 `46820` 或 `46821`，不得生成交接快照。profile 不可用时，若无法确认当前用户没有 `ACTIVE` 或 `INACTIVE` 成员档案，返回 `46810` 或 `46811`。

审计要求：交接快照读取是低风险读取，不强制写审计，不得增加 `auditsTotal`。后续 exam 创建考试流程时必须在 exam 自己的契约和审计中记录来源 `applicationId` 和 `handoffVersion`。

#### 后台重置流程

`PATCH /api/v1/onboarding/admin/applications/{applicationId}/reset`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `resetToStep` | string | 否 | 允许 `ACCOUNT_READY`、`PROFILE_CONFIRMED`、`RULES_CONFIRMED`、`DIRECTION_SELECTED`，默认 `ACCOUNT_READY`。 |
| `notifyUser` | boolean | 否 | 默认 `true`。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为重置后的 `OnboardingApplication`。

业务规则：`COMPLETED` 流程 P0 不允许重置，返回 `43810`。重置会清除目标步骤之后的完成记录，并把状态改为 `IN_PROGRESS`。若 `notifyUser=true`，通知失败返回 `46830` 或 `46831`，状态不得变化。审计失败返回 `51801`，状态不得变化。

#### 后台阻塞流程

`PATCH /api/v1/onboarding/admin/applications/{applicationId}/block`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `blockReason` | string | 是 | 1 到 500 位，阻塞原因。 |
| `notifyUser` | boolean | 否 | 默认 `true`。 |
| `reason` | string | 是 | 1 到 200 位，后台操作原因。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为阻塞后的 `OnboardingApplication`。

业务规则：重复阻塞同一流程且阻塞原因相同返回成功，保持幂等，不重复写审计。已取消或已完成流程不能阻塞，返回 `43810`。阻塞后当前用户写接口返回 `43816`，读取接口仍可返回当前状态。

#### 后台解除阻塞

`PATCH /api/v1/onboarding/admin/applications/{applicationId}/unblock`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `notifyUser` | boolean | 否 | 默认 `true`。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为解除阻塞后的 `OnboardingApplication`。

业务规则：只有 `BLOCKED` 状态可解除阻塞。重复解除未阻塞流程返回成功，但不得重复写审计。解除后状态回到阻塞前状态，若阻塞前状态不可判断则回到 `IN_PROGRESS` 并重新计算当前步骤。

#### onboarding 审计列表

`GET /api/v1/onboarding/admin/audit-logs`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `applicationId` | string | 否 | 流程 ID。 |
| `actorUserId` | string | 否 | 操作者用户 ID。 |
| `action` | string | 否 | 动作，最多 80 位。 |
| `result` | string | 否 | `SUCCESS` 或 `FAILED`。 |
| `from` | string | 否 | ISO 8601 起始时间。 |
| `to` | string | 否 | ISO 8601 结束时间。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`。默认 `createdAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `OnboardingAuditLog[]`。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 返回 `42001`。审计日志不得通过 onboarding API 删除。返回结果必须脱敏。

#### onboarding 自检摘要

`GET /api/v1/onboarding/admin/ops/summary`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "onboarding",
    "port": 8131,
    "legacyPort": 8108,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "profileMode": "TEST_STUB",
    "contentMode": "TEST_STUB",
    "notificationMode": "TEST_STUB",
    "applicationsTotal": 8,
    "blockedTotal": 1,
    "readyForExamTotal": 2,
    "stateMachineMode": "EXPLICIT_P0",
    "handoffSnapshotsTotal": 0,
    "auditsTotal": 20,
    "idempotencyRecordsTotal": 5,
    "lastAuditAt": "2026-05-22T12:00:00Z",
    "productionGaps": [
      "P0_IN_MEMORY_STORAGE",
      "P0_AUTH_STUB",
      "P0_PROFILE_STUB",
      "P0_CONTENT_STUB",
      "P0_NOTIFICATION_STUB",
      "EXAM_NOT_IMPLEMENTED",
      "WHITELIST_NOT_IMPLEMENTED"
    ]
  }
}
```

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。`stateMachineMode` 固定为 `EXPLICIT_P0`，表示流程推进集中按契约状态机判定。`handoffSnapshotsTotal` 统计本进程生成过的 exam 交接快照次数。自检摘要不得返回 token、请求头、Minecraft 验证凭据、通知正文、content 正文、后台备注全文、审计参数全文或异常堆栈。

### 状态、幂等和并发

新流程初始状态为 `IN_PROGRESS`。未创建流程时只读视图为 `NOT_STARTED`。`IN_PROGRESS` 在后台阻塞后进入 `BLOCKED`，解除阻塞后回到阻塞前状态或重新计算为 `IN_PROGRESS`。当前用户完成资料确认、规则确认和方向选择后，通过推进接口进入 `READY_FOR_EXAM`。`READY_FOR_EXAM` 之后的 `WAITING_EXAM`、`READY_FOR_WHITELIST`、`WAITING_WHITELIST`、`COMPLETED` 和 `CANCELLED` 只作为后续兼容占位，P0 不主动写入。

步骤推进顺序固定为 `ACCOUNT_READY`、`MINECRAFT_BOUND`、`PROFILE_CONFIRMED`、`RULES_CONFIRMED`、`DIRECTION_SELECTED`、`EXAM_READY`、`WHITELIST_READY`。服务端可以根据 auth Minecraft 绑定自动完成 `MINECRAFT_BOUND`，但不能自动完成资料确认、规则确认或方向选择。非法跳跃返回 `43811` 或 `43810`。

创建或恢复流程、资料确认、规则确认、方向选择、推进下一步、后台重置、阻塞和解除阻塞支持 `idempotencyKey`。同一操作者、同一幂等键、同一请求体重复提交时返回同一结果。相同幂等键搭配不同请求体返回 `43817`。幂等请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象都按字段名递归排序，不能受浏览器字段顺序影响。

并发启动同一用户流程只能产生一个未取消流程。并发确认、方向选择和推进必须以服务端当前状态为准，不得产生半完成步骤。公开读取允许读到更新前或更新后的完整状态，不能返回半更新对象。

### 审计要求

必须审计的动作包括启动流程、确认资料、确认规则、选择方向、推进到考试准备、后台重置、后台阻塞、后台解除阻塞、通知强制投递失败、审计写入失败和依赖降级导致状态不可推进。

当前用户关键动作写低风险审计。后台写操作必须记录 `reason`、操作者、目标流程、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计写入失败时，状态写操作不得假装成功，必须返回 `51801` 或 `51800`，并保持业务数据不变。

### 失败降级

auth 是强依赖。auth 不可用、超时、用户状态不允许或上下文字段不兼容时，当前用户接口和后台接口不得伪造成功。

profile 是资料确认和成员状态判定强依赖。读取进度可局部降级，写操作不得在 profile 不可用时确认资料或推进。已有缓存成员摘要只用于展示，不能作为准入判断。

content 是规则确认强依赖。读取进度可返回缓存规则摘要并标记降级；没有可用规则摘要时不得确认规则。规则版本过期必须要求用户重新确认。

notification 对当前用户普通流程是辅助依赖。通知失败时主状态可以成功，但必须写失败摘要。后台重置、阻塞、解除阻塞若 `notifyUser=true`，通知是强制依赖，失败时状态不得变化。

exam、whitelist 和 attendance 未实现时，onboarding 只返回下一步占位，不得整体失败，也不得伪造下游结果。

### 验收口径

`onboarding` API 文档按 `docs/contracts-onboarding.md` 独立存在，并由 `.local-docs/tests-onboarding.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`onboarding` 完成时必须满足以下条件：全部接口按本文档实现；当前用户接口只能访问当前用户自己的流程；后台接口按角色限制；服务端决定状态和下一步，不信任浏览器传入的可信字段；auth、profile、content 和 notification 适配不直接读取前序服务数据库或内部类；规则确认、资料确认、方向选择、推进、阻塞、重置、幂等、审计、自检摘要、requestId 和端口配置都有自动化测试；`.local-docs/tests-onboarding.md` 中全部测试用例都有对应自动化验证；未实现时自动化测试必须先失败；实现后 onboarding 全部测试通过；auth、profile、notification、content、server-status、resource 和 admin 前序服务回归测试通过；没有修改前序服务稳定接口；没有把考试判分、白名单审核、成员激活、考勤积分、社区、资源下载、服务器状态采集、后台聚合、真实运维、节点、容器、终端、文件管理、备份恢复或 Cloudreve 管理能力塞进 onboarding。

## 北冥官网 exam API 契约

来源：`docs/contracts-exam.md`

版本：0.1

### 文档定位

本文档是 `exam` 微服务的正式 API 契约。后续 `whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、前端适配和 `admin` 聚合只能通过本文档定义的接口读取考试结果、考试待办和考试审计摘要，不能直接读取或修改 `exam` 数据库，也不能把白名单审核、成员激活、考勤积分或社区工单逻辑塞进 `exam`。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `exam` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了现有 Minecraft 服务器白名单审核和成熟在线考试平台的通用做法。Minecraft 服务器通过白名单控制入服资格，`enforce-whitelist` 会让不在白名单中的在线玩家在白名单重载后被踢出；北冥的考试只负责准入评估，不直接操作服务器白名单。Moodle Quiz 把题库和测验分开，支持时间限制、尝试次数、提交后反馈、随机题和人工评分题；Google Forms Quiz 支持自动评分，也支持人工审核后再发布成绩。北冥吸收这些适合入服审核的边界：题库与试卷分离、题库版本冻结、客观题自动判分、简答题人工阅卷、考试结果延迟发布、重考冷却和审计追踪。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Minecraft Wiki server.properties](https://minecraft.fandom.com/wiki/Server.properties) | 白名单是服务器准入控制，不应由考试服务直接操作。 |
| [Moodle Quiz activity](https://docs.moodle.org/en/Quiz) | 题库与测验分离，题目可复用，测验有独立设置。 |
| [Moodle Quiz settings](https://docs.moodle.org/en/Quiz_settings) | 测验可配置时间限制、尝试间隔和提交策略。 |
| [Moodle Using Quiz](https://docs.moodle.org/en/Using_Quiz) | 考生可保存尝试并提交，作文题需要人工评分。 |
| [Google Forms quizzes](https://support.google.com/docs/answer/7032287) | 成绩可自动发布，也可人工审核后发布。 |

### 职责边界

`exam` 负责入服考试方向、题库、题目版本、试卷模板、试卷实例、答题草稿、提交记录、客观题自动判分、简答题人工阅卷、考试结果、重考策略、二次考核标记、给 `whitelist` 的只读考试结果交接摘要、幂等记录和自身审计。

`exam` 不负责注册、登录、邀请码、会话、账号角色能力点、Minecraft 绑定主数据、入服前置流程主数据、成员档案主数据、站内通知主数据、官网内容主数据、白名单申请、白名单审核、成员激活、考勤积分初始化、社区工单、服务器状态展示、资源下载、Cloudreve 分享、后台聚合入口或任何真实服务器运维操作。

`exam` 只能适配前序服务。它通过 `auth` 认证上下文读取当前用户和后台操作者，通过 `onboarding` 的考试交接快照创建考试，通过 `profile` 摘要判断已有成员与二次考核，通过 `content` 摘要读取考试说明版本，通过 `notification` 投递考试状态通知。它不能要求前序服务反向写入 exam 状态，不能导入前序服务的内存存储、实体、Repository、测试种子或内部类。

### 数据归属

`exam` 拥有以下主数据：考试流程、题库题目、题目版本、试卷模板、试卷实例、答题记录、自动判分结果、人工阅卷记录、考试结果、重考策略、给 whitelist 的只读交接快照、幂等记录、通知调用摘要和 exam 审计日志。

`exam` 可以保存来自 `onboarding` 的 `applicationId`、`handoffVersion`、`userId`、`displayNameSnapshot`、`minecraftBindingSnapshot`、`profileConfirmation`、`ruleConfirmation` 和 `reviewDirection` 快照。它可以保存来自 `profile` 的成员状态摘要、来自 `content` 的考试说明摘要、来自 `notification` 的投递结果摘要。快照不是来源模块主数据，不能用于反写来源模块。

### 基础路径与认证

所有接口默认使用 `/api/v1/exams` 前缀。第二批合并后当前运行入口由 `admission-core-service` 承载，端口固定为 `8131`。历史原服务端口 `8109` 只作为 `legacyPort` 返回，不作为当前运行入口、网关上游或测试入口。

### 本地测试控制头

`exam` 允许在本地自动化测试中使用 `X-Test-Dependency-Mode`、`X-Test-Notification-Mode`、`X-Test-Fail-Audit` 和 `X-Test-Fail-Store` 模拟依赖失败、通知失败、审计失败和状态写入失败。生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、状态失败或通知失败。

### 网关可信身份上下文

只有 `X-Gateway-Internal-Request-Id` 存在时，`exam` 才进入可信上下文解析；若该头缺失，即使请求带有 `X-Beiming-Actor-*`，也必须忽略这些头并继续走 `Authorization: Bearer <token>` 兼容路径。可信上下文缺少 `X-Beiming-Actor-User-Id`、角色枚举不兼容或字段无法解析时返回 HTTP `502` 和 `46902`。

当前用户接口使用 `/api/v1/exams/me` 前缀，全部要求 `Authorization: Bearer <token>`，只能访问当前认证用户自己的考试。浏览器请求体不得传入 `userId`、`roles`、`permissions`、`minecraftBindingSnapshot`、`score`、`passed`、`reviewerId`、`status`、`createdBy`、`updatedBy` 等服务端可信字段。

后台接口使用 `/api/v1/exams/admin` 前缀，全部要求登录。后台读取考试、题库、模板、审计和自检摘要要求 `HELPER`、`ADMIN` 或 `OWNER`。题库维护、模板维护、人工阅卷、要求补充、取消考试、结果修正和策略更新要求 `ADMIN` 或 `OWNER`。`HELPER` 可读取待阅卷和考试详情，但不能写题库、模板或最终结果。

### 前序服务兼容契约

`auth` 是强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions`、`status` 和 `minecraftBinding`。用户状态为 `PENDING_PROFILE` 或 `ACTIVE` 时可参加入服考试；`DISABLED`、`BANNED`、`DELETED` 不允许创建、答题或提交。auth 不可用返回 `46900`，auth 超时返回 `46901`，字段或枚举不兼容返回 `46902`。

`onboarding` 是创建考试的强依赖。创建考试时必须读取 `GET /api/v1/onboarding/admin/applications/{applicationId}/exam-handoff` 或未来等价服务间适配器。只有交接快照满足 `readyForExam=true`、`handoffAllowed=true`、`status=READY_FOR_EXAM`、`targetModule=EXAM`，且 `reviewDirection` 为 `REDSTONE`、`LATE_GAME`、`BUILDING` 或 `GENERAL` 时，才允许创建考试。onboarding 不可用返回 `46910`，超时返回 `46911`，字段不兼容返回 `46912`，交接状态不满足返回 `43910`。

`profile` 是创建考试时的成员状态判定依赖。已有 `ACTIVE` 或 `INACTIVE` 成员档案的用户不允许创建新玩家考试，返回 `43911`。`REMOVED` 成员允许创建二次考核，`attemptType=RECHECK`，`difficulty=RECHECK`，试卷必须使用二次考核模板。profile 不可用返回 `46920`，超时返回 `46921`，字段不兼容返回 `46922`。读取已有考试、试卷、结果时可以使用 exam 已保存快照降级返回。

`content` 用于读取考试说明和规则说明摘要。创建考试时如模板要求绑定说明版本，content 是强依赖；已生成的试卷读取、保存草稿和提交不因 content 当前不可用而失败。content 不可用返回 `46930`，超时返回 `46931`，字段不兼容返回 `46932`，规则说明版本不匹配返回 `43912`。

`notification` 用于投递考试状态通知。考试创建、提交、自动失败、进入人工阅卷、人工阅卷通过、人工阅卷失败、要求补充、补充提交、考试过期和后台取消都应尝试通知。通知是辅助依赖，除本文档单独说明的强制通知场景外，通知失败不得回滚考试主状态，但必须记录 `notificationStatus=FAILED`、失败原因摘要和审计。notification 不可用返回或记录 `46940`，超时返回或记录 `46941`，字段不兼容返回或记录 `46942`。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `ReviewDirection` | `REDSTONE`、`LATE_GAME`、`BUILDING`、`GENERAL` | 考试方向，必须兼容 onboarding。 |
| `QuestionType` | `SINGLE_CHOICE`、`MULTIPLE_CHOICE`、`TRUE_FALSE`、`SHORT_TEXT` | P0 题型。 |
| `QuestionStatus` | `DRAFT`、`ACTIVE`、`ARCHIVED` | 题目状态。已进入试卷的题目版本仍可追溯。 |
| `PaperTemplateStatus` | `DRAFT`、`PUBLISHED`、`ARCHIVED` | 试卷模板状态。只有已发布模板可生成考试。 |
| `AttemptType` | `FIRST_TIME`、`RECHECK` | 首次入服考试或二次考核。 |
| `ExamDifficulty` | `NORMAL`、`RECHECK` | 二次考核必须使用 `RECHECK`。 |
| `ExamSessionStatus` | `CREATED`、`IN_PROGRESS`、`SUBMITTED`、`AUTO_PASSED`、`AUTO_FAILED`、`PENDING_MANUAL_REVIEW`、`NEEDS_SUPPLEMENT`、`SUPPLEMENT_SUBMITTED`、`MANUAL_PASSED`、`MANUAL_FAILED`、`EXPIRED`、`CANCELLED` | 考试内部状态。 |
| `ExamResult` | `PENDING`、`PASSED`、`FAILED`、`NEEDS_SUPPLEMENT`、`EXPIRED`、`CANCELLED` | 对外归一结果。 |
| `NotificationStatus` | `DELIVERED`、`FAILED`、`SKIPPED` | 最近一次通知摘要。 |
| `ExamAuditResult` | `SUCCESS`、`FAILED` | exam 审计结果。 |

### 通用对象

#### ExamSession

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sessionId` | string | 是 | 考试实例 ID。 |
| `applicationId` | string | 是 | onboarding 流程实例 ID。 |
| `handoffVersion` | integer | 是 | onboarding 交接快照版本。 |
| `userId` | string | 是 | auth 用户 ID。当前用户接口固定为认证用户。 |
| `displayNameSnapshot` | string | 是 | 创建考试时的展示名快照。 |
| `minecraftBindingSnapshot` | object | 是 | Minecraft 绑定快照，兼容 auth 的 `MinecraftBinding`。 |
| `reviewDirection` | string | 是 | `ReviewDirection`。 |
| `attemptType` | string | 是 | `FIRST_TIME` 或 `RECHECK`。 |
| `difficulty` | string | 是 | `NORMAL` 或 `RECHECK`。 |
| `status` | string | 是 | `ExamSessionStatus`。 |
| `result` | string | 是 | `ExamResult`。 |
| `templateId` | string | 是 | 使用的试卷模板 ID。 |
| `templateVersion` | integer | 是 | 使用的模板版本。 |
| `paperId` | string | 是 | 试卷实例 ID。 |
| `scoreSummary` | ExamScoreSummary 或 null | 是 | 成绩摘要。未提交时为 `null`。 |
| `manualReview` | ExamManualReview 或 null | 是 | 最近一次人工阅卷记录。 |
| `supplementRequest` | object 或 null | 是 | 补充要求摘要。 |
| `notificationStatus` | string 或 null | 是 | 最近一次通知结果。 |
| `degraded` | boolean | 是 | 是否存在局部降级。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |
| `startedAt` | string | 是 | 考试创建时间。 |
| `lastSavedAt` | string 或 null | 是 | 最近保存草稿时间。 |
| `submittedAt` | string 或 null | 是 | 首次提交时间。 |
| `reviewedAt` | string 或 null | 是 | 最近人工阅卷时间。 |
| `expiresAt` | string | 是 | 考试截止时间。 |
| `passedAt` | string 或 null | 是 | 通过时间。 |
| `cancelledAt` | string 或 null | 是 | 取消时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### ExamQuestion

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `questionId` | string | 是 | 题目 ID。 |
| `version` | integer | 是 | 题目版本，从 `1` 开始。 |
| `type` | string | 是 | `QuestionType`。 |
| `reviewDirection` | string | 是 | 适用方向。 |
| `difficulty` | string | 是 | `ExamDifficulty`。 |
| `stem` | string | 是 | 题干，最多 2000 位。 |
| `options` | ExamQuestionOption[] | 是 | 客观题选项，简答题为空数组。 |
| `correctOptionIds` | string[] | 后台必返 | 正确选项 ID。考生视图不得返回。 |
| `referenceAnswer` | string 或 null | 后台必返 | 简答参考答案。考生视图不得返回。 |
| `score` | integer | 是 | 分值，1 到 100。 |
| `tags` | string[] | 是 | 标签，最多 10 个。 |
| `status` | string | 是 | `QuestionStatus`。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |

#### ExamQuestionOption

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `optionId` | string | 是 | 选项 ID。 |
| `label` | string | 是 | 展示标签，例如 `A`。 |
| `text` | string | 是 | 选项文本，最多 500 位。 |

#### ExamPaper

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `paperId` | string | 是 | 试卷实例 ID。 |
| `sessionId` | string | 是 | 考试实例 ID。 |
| `templateId` | string | 是 | 模板 ID。 |
| `templateVersion` | integer | 是 | 模板版本。 |
| `reviewDirection` | string | 是 | 考试方向。 |
| `attemptType` | string | 是 | 考试类型。 |
| `timeLimitMinutes` | integer | 是 | 考试时长。 |
| `questions` | ExamQuestion[] | 是 | 考生视图不含正确答案和参考答案。 |
| `totalScore` | integer | 是 | 总分。 |
| `objectiveTotalScore` | integer | 是 | 客观题总分。 |
| `manualTotalScore` | integer | 是 | 人工阅卷题总分。 |
| `generatedAt` | string | 是 | 生成时间。 |

#### ExamAnswerItem

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `questionId` | string | 是 | 题目 ID。 |
| `selectedOptionIds` | string[] | 视题型 | 客观题选项。单选和判断必须为 1 个，多选至少 1 个。 |
| `textAnswer` | string 或 null | 视题型 | 简答题答案，最多 2000 位。 |

#### ExamAnswerSheet

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sessionId` | string | 是 | 考试实例 ID。 |
| `answers` | ExamAnswerItem[] | 是 | 答案列表。 |
| `draft` | boolean | 是 | 是否为草稿。 |
| `savedAt` | string | 是 | 保存时间。 |
| `submittedAt` | string 或 null | 是 | 提交时间。 |

#### ExamScoreSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `objectiveScore` | integer | 是 | 客观题得分。 |
| `manualScore` | integer 或 null | 是 | 人工题得分。未阅卷为 `null`。 |
| `totalScore` | integer 或 null | 是 | 总分。未阅卷为 `null`。 |
| `objectivePassed` | boolean | 是 | 客观题是否达到客观题最低线。 |
| `finalPassed` | boolean 或 null | 是 | 最终是否通过。未完成阅卷为 `null`。 |
| `passScore` | integer | 是 | 最终通过线。 |
| `objectivePassScore` | integer | 是 | 客观题最低线。 |
| `manualRequired` | boolean | 是 | 是否需要人工阅卷。 |
| `scoredAt` | string | 是 | 最近判分时间。 |

#### ExamManualReview

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `reviewId` | string | 是 | 阅卷记录 ID。 |
| `reviewerUserId` | string | 是 | 阅卷人用户 ID。 |
| `reviewerDisplayNameSnapshot` | string | 是 | 阅卷人展示名快照。 |
| `manualScores` | object[] | 是 | 每道人工题得分和评语。 |
| `publicComment` | string | 是 | 给考生看的评语，最多 1000 位。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注。考生视图不得返回。 |
| `result` | string | 是 | `PASSED`、`FAILED` 或 `NEEDS_SUPPLEMENT`。 |
| `reviewedAt` | string | 是 | 阅卷时间。 |

#### PaperTemplate

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `templateId` | string | 是 | 模板 ID。 |
| `version` | integer | 是 | 模板版本。 |
| `name` | string | 是 | 模板名称，最多 80 位。 |
| `reviewDirection` | string | 是 | 适用方向。 |
| `difficulty` | string | 是 | `NORMAL` 或 `RECHECK`。 |
| `status` | string | 是 | `PaperTemplateStatus`。 |
| `timeLimitMinutes` | integer | 是 | 15 到 180。 |
| `passScore` | integer | 是 | 最终通过线。 |
| `objectivePassScore` | integer | 是 | 客观题最低线。 |
| `questionRules` | object[] | 是 | 按题型、标签、数量和分值配置的抽题规则。 |
| `contentRuleVersion` | string 或 null | 是 | 绑定的考试说明版本。 |
| `retakeCooldownHours` | integer | 是 | 重考冷却小时数。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `publishedAt` | string 或 null | 是 | 发布时间。 |

#### ExamWhitelistHandoffSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sessionId` | string | 是 | 考试实例 ID。 |
| `applicationId` | string | 是 | onboarding 流程实例 ID。 |
| `handoffVersion` | integer | 是 | exam 交接快照版本，从 `1` 开始。 |
| `onboardingHandoffVersion` | integer | 是 | 来源 onboarding 交接版本。 |
| `userId` | string | 是 | 用户 ID。 |
| `minecraftBindingSnapshot` | object | 是 | Minecraft 绑定快照。 |
| `reviewDirection` | string | 是 | 审核方向。 |
| `attemptType` | string | 是 | 首次或二次考核。 |
| `result` | string | 是 | 必须为 `PASSED` 才允许 whitelist 创建申请。 |
| `scoreSummary` | ExamScoreSummary | 是 | 成绩摘要。 |
| `passedAt` | string | 是 | 通过时间。 |
| `reviewerSnapshot` | object 或 null | 是 | 人工阅卷人快照。客观题自动通过时为 `null`。 |
| `generatedAt` | string | 是 | 生成时间。 |

#### ExamAuditLog

审计字段继承公共契约，允许补充 `sessionId`、`questionId`、`templateId`、`applicationId`、`handoffVersion`、`stateFrom`、`stateTo`、`reviewDirection`、`attemptType`、`idempotencyKey`、`notificationStatus`、`dependencyStatus` 和 `scoreSummary`。审计日志不得通过 exam API 删除。

### exam 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `43900` | 404 | 考试实例不存在，或当前用户无权访问。 |
| `43901` | 404 | 试卷不存在。 |
| `43902` | 404 | 题目不存在。 |
| `43903` | 404 | 试卷模板不存在。 |
| `43904` | 404 | 审计记录不存在。 |
| `43910` | 409 | onboarding 交接状态不允许创建考试。 |
| `43911` | 409 | 当前用户已有正式成员档案，不允许创建新玩家考试。 |
| `43912` | 409 | 考试说明或规则版本不匹配。 |
| `43913` | 409 | 当前考试状态不允许该操作。 |
| `43914` | 409 | 没有可用已发布试卷模板。 |
| `43915` | 409 | 题库题目不足，无法生成试卷。 |
| `43916` | 409 | 答案缺失、题型不匹配或选项非法。 |
| `43917` | 409 | 考试已过期。 |
| `43918` | 409 | 考试提交后不可修改答案。 |
| `43919` | 409 | exam 幂等键请求指纹冲突。 |
| `43920` | 409 | 重考冷却未结束。 |
| `43921` | 409 | 题目已被发布模板或历史试卷引用，不能破坏历史版本。 |
| `43922` | 409 | 模板状态不允许该操作。 |
| `43923` | 409 | 人工阅卷分数超出题目分值。 |
| `43924` | 409 | 只有通过结果可生成 whitelist 交接快照。 |
| `43925` | 409 | 考试结果已经生成下游交接快照，不允许直接修正。 |
| `46900` | 502 | auth 认证上下文不可用。 |
| `46901` | 504 | auth 认证上下文调用超时。 |
| `46902` | 502 | auth 认证上下文字段或枚举不兼容 exam 契约。 |
| `46910` | 502 | onboarding 交接快照不可用。 |
| `46911` | 504 | onboarding 交接快照调用超时。 |
| `46912` | 502 | onboarding 交接快照字段不兼容 exam 契约。 |
| `46920` | 502 | profile 摘要不可用。 |
| `46921` | 504 | profile 摘要调用超时。 |
| `46922` | 502 | profile 摘要字段或枚举不兼容 exam 契约。 |
| `46930` | 502 | content 考试说明不可用。 |
| `46931` | 504 | content 考试说明调用超时。 |
| `46932` | 502 | content 考试说明字段不兼容 exam 契约。 |
| `46940` | 502 | notification 强制投递不可用。 |
| `46941` | 504 | notification 强制投递超时。 |
| `46942` | 502 | notification 投递响应不兼容 exam 契约。 |
| `51900` | 500 | exam 内部错误。 |
| `51901` | 500 | exam 审计写入失败。 |
| `51902` | 500 | exam 状态写入失败。 |
| `51903` | 500 | exam 判分失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误和通用服务端错误优先使用公共错误码。exam 自有幂等指纹冲突使用 `43919`。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 创建或恢复当前考试 | POST | `/api/v1/exams/me/sessions` | 是 | 当前用户 | LOW |
| 当前考试 | GET | `/api/v1/exams/me/sessions/current` | 是 | 当前用户 | LOW |
| 当前用户考试历史 | GET | `/api/v1/exams/me/sessions` | 是 | 当前用户 | LOW |
| 读取试卷 | GET | `/api/v1/exams/me/sessions/{sessionId}/paper` | 是 | 当前用户 | LOW |
| 保存答案草稿 | PUT | `/api/v1/exams/me/sessions/{sessionId}/answers` | 是 | 当前用户 | LOW |
| 提交考试 | POST | `/api/v1/exams/me/sessions/{sessionId}/submit` | 是 | 当前用户 | MEDIUM |
| 补充答案 | PATCH | `/api/v1/exams/me/sessions/{sessionId}/supplement` | 是 | 当前用户 | MEDIUM |
| 读取结果 | GET | `/api/v1/exams/me/sessions/{sessionId}/result` | 是 | 当前用户 | LOW |
| 后台考试列表 | GET | `/api/v1/exams/admin/sessions` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台考试详情 | GET | `/api/v1/exams/admin/sessions/{sessionId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 人工阅卷 | PATCH | `/api/v1/exams/admin/sessions/{sessionId}/manual-review` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 结果修正 | PATCH | `/api/v1/exams/admin/sessions/{sessionId}/result-correction` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 要求补充 | PATCH | `/api/v1/exams/admin/sessions/{sessionId}/request-supplement` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 取消考试 | PATCH | `/api/v1/exams/admin/sessions/{sessionId}/cancel` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| whitelist 交接快照 | GET | `/api/v1/exams/admin/sessions/{sessionId}/whitelist-handoff` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 后台题目列表 | GET | `/api/v1/exams/admin/question-bank/questions` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 题目版本历史 | GET | `/api/v1/exams/admin/question-bank/questions/{questionId}/versions` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建题目 | POST | `/api/v1/exams/admin/question-bank/questions` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改题目 | PATCH | `/api/v1/exams/admin/question-bank/questions/{questionId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档题目 | PATCH | `/api/v1/exams/admin/question-bank/questions/{questionId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 模板列表 | GET | `/api/v1/exams/admin/paper-templates` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建模板 | POST | `/api/v1/exams/admin/paper-templates` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改模板 | PATCH | `/api/v1/exams/admin/paper-templates/{templateId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 模板发布预检 | GET | `/api/v1/exams/admin/paper-templates/{templateId}/publish-preview` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 发布模板 | PATCH | `/api/v1/exams/admin/paper-templates/{templateId}/publish` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档模板 | PATCH | `/api/v1/exams/admin/paper-templates/{templateId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| exam 审计列表 | GET | `/api/v1/exams/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| exam 自检摘要 | GET | `/api/v1/exams/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

### 当前用户接口

#### 创建或恢复当前考试

`POST /api/v1/exams/me/sessions`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `applicationId` | string | 是 | onboarding 流程实例 ID。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `ExamSession`。若当前用户已有同一 `applicationId` 下未结束考试，返回 HTTP `200` 和现有考试。若最近一次考试失败但重考冷却未结束，返回 `43920`。

业务规则：服务端必须读取 onboarding 交接快照和 profile 成员状态，选择与 `reviewDirection`、`attemptType`、`difficulty` 匹配的已发布模板，生成确定性试卷实例，并冻结题目版本、模板版本、考试说明版本和交接快照。题库不足、模板缺失或前序依赖失败不得创建半成品考试。创建成功后状态为 `IN_PROGRESS`。创建必须写入 `EXAM_SESSION_CREATED` 审计，审计失败返回 `51901`，不得创建考试。

幂等规则：同一用户、同一 `idempotencyKey`、同一请求体重复提交时返回同一考试。相同幂等键搭配不同请求体返回 `43919`。请求体指纹必须基于结构化 JSON 规范化结果。

#### 当前考试

`GET /api/v1/exams/me/sessions/current`

成功响应 HTTP `200`，`data` 为 `ExamSession` 或 `null`。只返回当前用户最近一个未结束考试。已通过、已失败、已取消或已过期的考试不作为当前考试返回。

业务规则：profile、content 或 notification 当前不可用时，可返回 exam 已保存快照并标记 `degraded=true`。auth 不可用不得返回成功。读取不会创建考试，不写审计。

#### 当前用户考试历史

`GET /api/v1/exams/me/sessions`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `status` | string | 否 | 任一 `ExamSessionStatus`。 |
| `result` | string | 否 | 任一 `ExamResult`。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`、`submittedAt_desc`、`updatedAt_desc`。默认 `createdAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `ExamSession[]`。只能返回当前用户自己的考试，不返回正确答案、参考答案、内部备注或审计参数。

#### 读取试卷

`GET /api/v1/exams/me/sessions/{sessionId}/paper`

成功响应 HTTP `200`，`data` 为 `ExamPaper` 的考生视图。考生视图永远不得返回 `correctOptionIds`、`referenceAnswer`、判分规则内部细节、管理员备注、审计参数或其他用户信息。

业务规则：考试必须属于当前用户，状态必须为 `IN_PROGRESS` 或 `NEEDS_SUPPLEMENT`。考试过期时返回 `43917`，并把状态推进为 `EXPIRED`，写入审计和通知失败摘要。已提交但待人工阅卷、已补充提交等待复审、已最终通过、已最终失败或已取消的考试不能重新读取可编辑试卷，返回 `43913`。

#### 保存答案草稿

`PUT /api/v1/exams/me/sessions/{sessionId}/answers`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `answers` | ExamAnswerItem[] | 是 | 只能包含试卷中的题目。未答题可传空数组。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为 `ExamAnswerSheet`。

业务规则：只允许 `IN_PROGRESS` 状态保存草稿。`NEEDS_SUPPLEMENT` 必须使用补充接口。提交后、过期后、取消后不可保存，分别返回 `43918`、`43917` 或 `43913`。草稿答案必须校验题型、选项、简答长度和重复题目。保存草稿不判分，不通知，不泄露正确答案。

#### 提交考试

`POST /api/v1/exams/me/sessions/{sessionId}/submit`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `answers` | ExamAnswerItem[] | 是 | 必须覆盖全部必答题。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为 `ExamSession`。

业务规则：提交只允许 `IN_PROGRESS` 状态。提交时必须重新校验考试未过期和答案完整性。单选和判断完全匹配得分，否则 0 分。多选必须完全匹配正确选项才得分，漏选或错选均 0 分。客观题达到最终通过线且无简答题时进入 `AUTO_PASSED`，结果为 `PASSED`。客观题低于客观题最低线时进入 `AUTO_FAILED`，结果为 `FAILED`。存在简答题且客观题达到最低线时进入 `PENDING_MANUAL_REVIEW`，结果为 `PENDING`。提交成功写入审计，通知失败不回滚主状态。

#### 补充答案

`PATCH /api/v1/exams/me/sessions/{sessionId}/supplement`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `answers` | ExamAnswerItem[] | 是 | 只允许补充被要求补充的简答题。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为 `ExamSession`。

业务规则：只允许 `NEEDS_SUPPLEMENT` 状态。补充不得修改客观题，不得修改未被要求补充的题目。提交后状态为 `SUPPLEMENT_SUBMITTED`，结果为 `PENDING`，等待后台重新人工阅卷。补充截止时间沿用补充要求中的 `supplementDueAt`，超时返回 `43917` 并进入 `EXPIRED`。

#### 读取结果

`GET /api/v1/exams/me/sessions/{sessionId}/result`

成功响应 HTTP `200`，`data` 为 `ExamSession` 的结果视图。

业务规则：未提交考试返回 `43913`。`PENDING_MANUAL_REVIEW` 和 `SUPPLEMENT_SUBMITTED` 返回 `result=PENDING`，不得提前泄露人工阅卷内部备注。`NEEDS_SUPPLEMENT` 返回公开补充要求。最终失败可返回公开评语和重考冷却时间。最终通过可返回 `passedAt`，但不得返回 whitelist 申请已创建。

### 后台考试接口

#### 后台考试列表

`GET /api/v1/exams/admin/sessions`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配考试 ID、流程 ID、用户 ID、展示名或 Minecraft ID，最多 80 位。 |
| `status` | string | 否 | 任一 `ExamSessionStatus`。 |
| `result` | string | 否 | 任一 `ExamResult`。 |
| `reviewDirection` | string | 否 | 任一 `ReviewDirection`。 |
| `attemptType` | string | 否 | `FIRST_TIME` 或 `RECHECK`。 |
| `needsManualReview` | boolean | 否 | 是否只看待阅卷。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`submittedAt_desc`、`updatedAt_desc`、`status_asc`、`score_desc`。默认 `updatedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `ExamSession[]`。列表不得返回正确答案、参考答案、完整答卷正文、内部备注或审计参数全文。

#### 后台考试详情

`GET /api/v1/exams/admin/sessions/{sessionId}`

成功响应 HTTP `200`，`data` 包含 `ExamSession`、管理员视图试卷、答卷、判分明细和人工阅卷记录。`HELPER` 可读详情但看不到 `internalNote`。`ADMIN` 和 `OWNER` 可见内部备注。考试不存在返回 `43900`。

#### 人工阅卷

`PATCH /api/v1/exams/admin/sessions/{sessionId}/manual-review`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `manualScores` | object[] | 是 | 每道简答题得分，不能超过题目分值。 |
| `result` | string | 是 | `PASSED` 或 `FAILED`。要求补充必须调用 request-supplement。 |
| `publicComment` | string | 是 | 1 到 1000 位。 |
| `internalNote` | string | 否 | 最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为更新后的 `ExamSession`。

业务规则：只允许 `PENDING_MANUAL_REVIEW` 或 `SUPPLEMENT_SUBMITTED`。阅卷人不能修改客观题原始答案和客观题得分。最终总分达到通过线且 `result=PASSED` 时状态为 `MANUAL_PASSED`，结果为 `PASSED`；否则状态为 `MANUAL_FAILED`，结果为 `FAILED`。审计失败或状态写入失败时不得改变结果。通知失败不回滚，但必须记录。

#### 结果修正

`PATCH /api/v1/exams/admin/sessions/{sessionId}/result-correction`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `manualScores` | object[] | 否 | 只允许修正简答题得分，不能超过题目分值。全客观自动判分考试可不传。 |
| `result` | string | 是 | `PASSED` 或 `FAILED`。 |
| `publicComment` | string | 是 | 1 到 1000 位，说明修正后给考生可见的结论。 |
| `internalNote` | string | 否 | 最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位，必须说明修正原因。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为更新后的 `ExamSession`。

业务规则：只允许修正 `AUTO_PASSED`、`AUTO_FAILED`、`MANUAL_PASSED` 或 `MANUAL_FAILED` 的最终结果。已取消、已过期、待阅卷、需补充或补充已提交的考试不得走结果修正，返回 `43913`。已经生成 whitelist 交接快照的考试不得直接修正，返回 `43925`，后续如需撤销应由 whitelist 自己的契约处理下游状态。结果修正不得修改原始答卷和客观题得分；修正简答题时必须重新计算总分。修正为通过时总分必须达到 `passScore`，修正为失败时可以因为分数不足或复核原因失败。成功后写入 `EXAM_RESULT_CORRECTED` 审计，`manualReview` 中追加 `correction=true`、`correctedFromStatus`、`correctedFromResult` 和修正人快照。通知失败不回滚，但必须记录 `notificationStatus=FAILED`。

#### 要求补充

`PATCH /api/v1/exams/admin/sessions/{sessionId}/request-supplement`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `questionIds` | string[] | 是 | 只允许简答题。 |
| `publicComment` | string | 是 | 1 到 1000 位，说明要补充什么。 |
| `supplementDueAt` | string | 是 | ISO 8601，必须晚于当前时间且不超过 14 天。 |
| `internalNote` | string | 否 | 最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为更新后的 `ExamSession`。只允许 `PENDING_MANUAL_REVIEW` 或 `SUPPLEMENT_SUBMITTED`。成功后状态为 `NEEDS_SUPPLEMENT`，结果为 `NEEDS_SUPPLEMENT`。

#### 取消考试

`PATCH /api/v1/exams/admin/sessions/{sessionId}/cancel`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `notifyUser` | boolean | 否 | 默认 `true`。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为更新后的 `ExamSession`。只允许取消 `IN_PROGRESS`、`PENDING_MANUAL_REVIEW`、`NEEDS_SUPPLEMENT` 或 `SUPPLEMENT_SUBMITTED` 状态的考试。已通过、已失败、已过期、已取消或已生成 whitelist 交接快照的考试不能取消，返回 `43913`。若 `notifyUser=true`，通知失败是辅助失败，不回滚取消，但必须记录失败摘要。

#### whitelist 交接快照

`GET /api/v1/exams/admin/sessions/{sessionId}/whitelist-handoff`

成功响应 HTTP `200`，`data` 为 `ExamWhitelistHandoffSnapshot`。

业务规则：只有 `AUTO_PASSED` 或 `MANUAL_PASSED` 可生成快照，其他状态返回 `43924`。该接口只提供后续 `whitelist` 创建申请所需的只读快照，不创建白名单申请，不推进 onboarding 状态，不创建成员档案，不初始化考勤积分。读取是低风险读取，不强制写审计，但自检中的 `whitelistHandoffSnapshotsTotal` 必须递增。

### 后台题库接口

#### 后台题目列表

`GET /api/v1/exams/admin/question-bank/questions`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配题干或标签，最多 80 位。 |
| `type` | string | 否 | 任一 `QuestionType`。 |
| `reviewDirection` | string | 否 | 任一 `ReviewDirection`。 |
| `difficulty` | string | 否 | 任一 `ExamDifficulty`。 |
| `status` | string | 否 | 任一 `QuestionStatus`。 |
| `tag` | string | 否 | 标签。 |
| `sort` | string | 否 | 允许 `updatedAt_desc`、`createdAt_desc`、`score_desc`、`type_asc`。默认 `updatedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `ExamQuestion[]`。`HELPER` 可读题目和答案，用于阅卷准备，但不能修改题库。

#### 题目版本历史

`GET /api/v1/exams/admin/question-bank/questions/{questionId}/versions`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `sort` | string | 否 | 允许 `version_desc`、`version_asc`。默认 `version_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `ExamQuestion[]` 的后台视图，包含每个历史版本的题干、选项、正确答案、参考答案、分值、标签、状态和时间字段。题目不存在返回 `43902`。

业务规则：修改题干、选项、答案、参考答案、分值、方向、难度或题型时必须保留旧版本，并让版本历史可查询。历史版本只能读取，不能通过版本历史接口修改或删除。已生成试卷仍使用创建时冻结的题目版本；题目版本历史用于后台复核争议、审计题库变化和解释历史试卷来源。`HELPER` 可读版本历史但不能修改题库。读取版本历史不强制写审计，响应不得包含前序服务内部字段、token、请求头或异常堆栈。

#### 创建题目

`POST /api/v1/exams/admin/question-bank/questions`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `type` | string | 是 | 任一 `QuestionType`。 |
| `reviewDirection` | string | 是 | 任一 `ReviewDirection`。 |
| `difficulty` | string | 是 | 任一 `ExamDifficulty`。 |
| `stem` | string | 是 | 1 到 2000 位。 |
| `options` | ExamQuestionOption[] | 视题型 | 客观题 2 到 6 个选项，判断题固定 2 个选项，简答题为空数组。 |
| `correctOptionIds` | string[] | 视题型 | 客观题必填。 |
| `referenceAnswer` | string | 视题型 | 简答题必填，最多 2000 位。 |
| `score` | integer | 是 | 1 到 100。 |
| `tags` | string[] | 否 | 最多 10 个。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `ExamQuestion`。创建后状态为 `DRAFT`。题目创建写审计。

#### 修改题目

`PATCH /api/v1/exams/admin/question-bank/questions/{questionId}`

请求字段同创建题目，均可选，但 `reason` 必填。成功响应 HTTP `200`。

业务规则：修改题干、选项、答案、参考答案、分值、方向、难度或题型时必须创建新版本。已被历史试卷引用的旧版本不可被覆盖。题目处于 `ARCHIVED` 时不可修改，返回 `43913`。只修改标签或状态说明也要写审计。

#### 归档题目

`PATCH /api/v1/exams/admin/question-bank/questions/{questionId}/archive`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为归档后的 `ExamQuestion`。归档不会删除历史版本。已发布模板仍可使用冻结版本生成历史追溯，但新发布模板不得引用已归档题目。

### 后台模板接口

#### 模板列表

`GET /api/v1/exams/admin/paper-templates`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `reviewDirection` | string | 否 | 任一 `ReviewDirection`。 |
| `difficulty` | string | 否 | 任一 `ExamDifficulty`。 |
| `status` | string | 否 | 任一 `PaperTemplateStatus`。 |
| `sort` | string | 否 | 允许 `updatedAt_desc`、`publishedAt_desc`、`name_asc`。默认 `updatedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `PaperTemplate[]`。

#### 创建模板

`POST /api/v1/exams/admin/paper-templates`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `name` | string | 是 | 1 到 80 位。 |
| `reviewDirection` | string | 是 | 任一 `ReviewDirection`。 |
| `difficulty` | string | 是 | 任一 `ExamDifficulty`。 |
| `timeLimitMinutes` | integer | 是 | 15 到 180。 |
| `passScore` | integer | 是 | 1 到总分。 |
| `objectivePassScore` | integer | 是 | 0 到客观题总分。 |
| `questionRules` | object[] | 是 | 每条包含 `type`、`count`、`scoreEach`、`tags`。 |
| `contentRuleVersion` | string 或 null | 否 | 考试说明版本。 |
| `retakeCooldownHours` | integer | 是 | 0 到 720。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `201`，`data` 为 `PaperTemplate`，状态为 `DRAFT`。模板总分由规则计算。创建不得要求题库足量，发布时必须校验题库足量。

#### 修改模板

`PATCH /api/v1/exams/admin/paper-templates/{templateId}`

请求字段同创建模板，均可选，但 `reason` 必填。成功响应 HTTP `200`。修改已发布模板时必须创建新版本并回到 `DRAFT`，旧版本仍供历史试卷追溯。

#### 模板发布预检

`GET /api/v1/exams/admin/paper-templates/{templateId}/publish-preview`

成功响应 HTTP `200`，`data` 为发布预检结果。

```json
{
  "templateId": "tpl-redstone",
  "templateVersion": 1,
  "status": "DRAFT",
  "readyToPublish": true,
  "totalScore": 50,
  "objectiveTotalScore": 20,
  "manualTotalScore": 30,
  "contentRuleStatus": "VALID",
  "rules": [
    {
      "type": "SINGLE_CHOICE",
      "count": 1,
      "scoreEach": 10,
      "tags": ["redstone"],
      "matchedQuestionCount": 4,
      "enough": true
    }
  ],
  "samplePaper": {
    "questions": []
  },
  "warnings": []
}
```

业务规则：发布预检只读，不改变模板状态，不写发布审计，不占用幂等键。它必须复用正式发布的题库足量、题型、标签、分值、content 版本可用性和归档题过滤规则。题库不足时仍返回 HTTP `200`，但 `readyToPublish=false`，对应规则 `enough=false`，`warnings` 包含稳定原因；模板不存在返回 `43903`，已归档模板返回 `43922`。`contentRuleVersion` 不为空且 content 不可用时，预检返回 `readyToPublish=false` 和 `contentRuleStatus=UNAVAILABLE`，不得把错误吞成可发布。`samplePaper.questions` 使用后台可见题目摘要，供管理员确认题型和分值分布；不得包含前序服务内部字段、通知正文、token、请求头或异常堆栈。

#### 发布模板

`PATCH /api/v1/exams/admin/paper-templates/{templateId}/publish`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为 `PaperTemplate`。发布前必须校验每条抽题规则都有足够 `ACTIVE` 题目，`contentRuleVersion` 若不为空必须能从 content 读取且仍有效。每个 `reviewDirection+difficulty` 同一时间至少允许一个已发布模板；如存在多个，创建考试选择最新 `publishedAt`。

#### 归档模板

`PATCH /api/v1/exams/admin/paper-templates/{templateId}/archive`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为归档后的 `PaperTemplate`。归档不影响已生成试卷。若归档导致某个方向和难度没有可用模板，后续创建考试返回 `43914`。

### 审计与自检接口

#### exam 审计列表

`GET /api/v1/exams/admin/audit-logs`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `sessionId` | string | 否 | 考试 ID。 |
| `actorUserId` | string | 否 | 操作者用户 ID。 |
| `action` | string | 否 | 动作，最多 80 位。 |
| `result` | string | 否 | `SUCCESS` 或 `FAILED`。 |
| `from` | string | 否 | ISO 8601 起始时间。 |
| `to` | string | 否 | ISO 8601 结束时间。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`。默认 `createdAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `ExamAuditLog[]`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 exam API 删除，返回结果必须脱敏。

#### exam 自检摘要

`GET /api/v1/exams/admin/ops/summary`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "exam",
    "port": 8131,
    "legacyPort": 8109,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "onboardingMode": "TEST_STUB",
    "profileMode": "TEST_STUB",
    "contentMode": "TEST_STUB",
    "notificationMode": "TEST_STUB",
    "sessionsTotal": 8,
    "pendingManualReviewTotal": 2,
    "passedTotal": 3,
    "failedTotal": 2,
    "questionsTotal": 32,
    "publishedTemplatesTotal": 8,
    "whitelistHandoffSnapshotsTotal": 1,
    "auditsTotal": 40,
    "idempotencyRecordsTotal": 10,
    "lastAuditAt": "2026-05-23T12:00:00Z",
    "productionGaps": [
      "P0_IN_MEMORY_STORAGE",
      "P0_AUTH_STUB",
      "P0_ONBOARDING_STUB",
      "P0_PROFILE_STUB",
      "P0_CONTENT_STUB",
      "P0_NOTIFICATION_STUB",
      "WHITELIST_NOT_IMPLEMENTED"
    ]
  }
}
```

业务规则：自检摘要用于后台确认 exam 当前运行模式、题库规模、待阅卷规模、交接快照次数和生产化缺口。摘要不得返回 token、请求头、正确答案、参考答案、完整答卷、内部备注、审计参数全文、通知正文、content 正文、Minecraft 验证凭据或异常堆栈。

### 状态、判分、幂等和并发

考试创建成功后进入 `IN_PROGRESS`。`IN_PROGRESS` 可保存草稿或提交。提交后按判分结果进入 `AUTO_PASSED`、`AUTO_FAILED` 或 `PENDING_MANUAL_REVIEW`。`PENDING_MANUAL_REVIEW` 可进入 `MANUAL_PASSED`、`MANUAL_FAILED` 或 `NEEDS_SUPPLEMENT`。`NEEDS_SUPPLEMENT` 可由用户补充为 `SUPPLEMENT_SUBMITTED`，再由管理员进入最终通过、失败或再次要求补充。任何未最终结束的考试到期后进入 `EXPIRED`。后台可把尚未最终出结果且未生成 whitelist 交接快照的考试取消为 `CANCELLED`。

状态推进只能由服务端根据交接快照、模板、题库、时间、答案、自动判分、人工阅卷和后台动作决定。非法状态跳跃返回 `43913`。浏览器传入可信字段必须忽略或返回字段校验失败。

客观题判分固定为完全匹配。单选、判断只有选中唯一正确选项才得分。多选必须选项集合完全等于正确集合才得分，漏选、错选、多选均 0 分。简答题不自动通过，只由人工阅卷给分。

创建考试、保存草稿、提交考试、补充答案、人工阅卷、结果修正、要求补充、取消考试、创建题目、修改题目、归档题目、创建模板、修改模板、发布模板和归档模板支持 `idempotencyKey`。同一操作者、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `43919`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序。

并发创建同一用户同一 `applicationId` 下只能产生一个未结束考试。并发提交同一考试只能有一个成功判分结果。并发人工阅卷必须以服务端当前状态为准，不能产生两个最终结果。读取接口允许读到更新前或更新后的完整状态，不能返回半更新对象。

### 审计要求

必须审计的动作包括创建考试、过期推进、保存草稿失败、提交考试、自动判分通过、自动判分失败、进入人工阅卷、人工阅卷通过、人工阅卷失败、结果修正、要求补充、补充提交、后台取消、题目创建、题目修改、题目归档、模板创建、模板修改、模板发布、模板归档、依赖降级导致操作不可继续、通知失败和审计写入失败。

后台写操作必须记录 `reason`、操作者、目标对象、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计不得泄露 token、完整请求头、正确答案给考生、参考答案给考生、内部备注给 `HELPER` 或考生、完整通知正文、content 正文、异常堆栈或前序服务内部路径。

审计写入失败时，创建考试、提交考试、人工阅卷、要求补充、取消、题库维护和模板维护不得假装成功，必须返回 `51901` 或 `51900`，并保持业务数据不变。

### 失败降级

auth 是所有接口强依赖。auth 不可用、超时、用户状态不允许或字段不兼容时不得伪造成功。

onboarding 是创建考试强依赖。交接快照不可用、阻塞、过期、规则版本不匹配、Minecraft 绑定不完整或字段不兼容时，不得创建考试。

profile 是创建考试强依赖。读取当前考试、历史和结果时可使用已保存快照降级；创建考试时不能伪造成员状态。

content 对已冻结试卷不是强依赖。读取试卷、保存草稿、提交和阅卷不得因为 content 当前不可用而失败。创建考试或发布模板若绑定 content 说明版本，content 不可用或版本不匹配必须失败。

notification 默认是辅助依赖。通知失败不得回滚考试主状态，但必须记录失败摘要和审计。若未来引入强制通知动作，必须在本文档中新增说明和测试。

题库为空、模板缺失、试卷已过期、重复提交、答案缺失、题型不匹配、非法选项、简答超长、人工阅卷状态冲突、幂等键指纹冲突和审计失败都必须返回稳定错误码，不能吞成成功。

### 验收口径

`exam` API 文档按 `docs/contracts-exam.md` 独立存在，并由 `.local-docs/tests-exam.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`exam` 完成时必须满足以下条件：全部接口按本文档实现；当前用户接口只能访问自己的考试；后台接口按角色限制；考生视图不泄露正确答案、参考答案和内部备注；题库、模板、题目版本、试卷实例和答案快照可追溯；自动判分规则可测试；简答题人工阅卷和补充闭环可测试；创建考试只通过 onboarding handoff 和前序适配读取快照，不直接读前序服务实现；通过结果只暴露 whitelist 只读交接快照，不创建白名单申请；通知失败按辅助降级记录；当前运行入口为 `admission-core-service:8131`，历史端口只作为 `legacyPort=8109` 返回；`.local-docs/tests-exam.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 exam 全部测试通过；auth、profile、notification、content、server-status、resource、admin 和 onboarding 前序服务回归测试通过；没有修改前序服务稳定接口；没有把白名单审核、成员激活、考勤积分、社区工单、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复或 Cloudreve 管理能力塞进 exam。

## 北冥官网 whitelist API 契约

来源：`docs/contracts-whitelist.md`

版本：0.2

### 文档定位

本文档是 `whitelist` 微服务的正式 API 契约。后续 `attendance`、`community`、`activity`、`calendar`、`changelog`、前端适配、`admin` 聚合、`ops-control` 和 `external-node-executor` 只能通过本文档定义的接口读取白名单申请、审核结果、移除记录、审计和考勤初始化交接摘要，不能直接读取或修改 `whitelist` 数据库，也不能把白名单审核逻辑塞进其他服务。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `whitelist` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了现有优秀平台的流程设计。Discord Rules Screening 和 Community Onboarding 强调新成员先完成明确规则门槛，再进入后续流程。Atlassian Jira Workflow 强调状态、流转、条件、校验器和后置动作分离。Moodle Quiz 和 Google Forms Quiz 的考试流程证明考试结果应由考试模块冻结并交接，白名单模块只消费通过结果。Minecraft 和 Spigot 的白名单资料说明白名单最终会影响真实服务器准入，但真实服务器文件、命令、reload 和节点操作不属于本服务 P0 边界。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Discord Rules Screening FAQ](https://support.discord.com/hc/en-us/articles/1500000466882-Rules-Screening-FAQ) | 规则确认是进入社区前的明确门槛，不应只靠前端提示。 |
| [Discord Community Onboarding FAQ](https://support.discord.com/hc/en-us/articles/11074987197975-Community-Onboarding-FAQ) | 入门流程需要分步、可追溯，并根据成员选择进入后续路径。 |
| [Atlassian Jira workflows overview](https://www.atlassian.com/software/jira/guides/workflows/overview) | 审核流程应由状态和单向流转组成，非法跳转必须拒绝。 |
| [Atlassian workflow validators](https://support.atlassian.com/jira-cloud-administration/docs/use-workflow-validators-with-custom-fields/) | 状态推进前必须做字段、权限和条件校验。 |
| [Moodle Quiz activity](https://docs.moodle.org/en/Quiz) | 考试结果和人工阅卷由考试模块负责，白名单只消费冻结结果。 |
| [Google Forms quizzes](https://support.google.com/docs/answer/7032287) | 自动评分和人工审核后的结果发布应可追溯。 |
| [SpigotMC whitelist commands](https://www.spigotmc.org/wiki/spigot-commands-and-permissions/) | 真实 Minecraft 白名单写入是服务器操作，不属于申请审核服务。 |

### 职责边界

`whitelist` 负责考试通过后的白名单申请、申请材料、补充材料、审核分配、审核通过、审核拒绝、要求补充、撤回、移除白名单、允许重新申请、二次入服标记、给 `attendance` 的初始化交接摘要、幂等记录和自身审计。

`whitelist` 不负责注册、登录、邀请码、会话、账号角色能力点、Minecraft 绑定主数据、入服前置流程、题库、试卷、判分、人工阅卷、成员档案主数据、站内通知主数据、考勤积分主数据、社区工单、服务器状态展示、资源下载、后台聚合入口、真实 Minecraft 服务器白名单文件写入、控制台命令、节点守护进程、容器、终端、文件管理、备份恢复或 Cloudreve 管理。

`whitelist` 只能适配前序服务。它通过 `auth` 认证上下文读取当前用户和后台操作者，通过 `exam` 的 whitelist 交接快照创建申请，通过 `profile` 正式接口创建或激活成员档案、移除后更新成员状态，通过 `notification` 投递站内通知，通过保存的只读快照和后续接口给 `attendance` 提供初始化材料。它不能要求前序服务反向写入 whitelist 状态，不能导入前序服务内存存储、实体、Repository、测试种子或内部类。

### 数据归属

`whitelist` 拥有以下主数据：白名单申请、申请材料、补充材料、审核状态、审核意见、审核分配、移除记录、允许重新申请记录、profile 调用摘要、notification 调用摘要、attendance 初始化交接摘要、幂等记录和 whitelist 审计日志。

`whitelist` 可以保存来自 `exam` 的 `sessionId`、`applicationId`、`handoffVersion`、`onboardingHandoffVersion`、`userId`、`minecraftBindingSnapshot`、`reviewDirection`、`attemptType`、`result`、`scoreSummary`、`passedAt` 和 `reviewerSnapshot` 快照。它可以保存来自 `profile` 的成员激活或状态变更结果摘要，可以保存来自 `notification` 的投递结果摘要。快照不是来源模块主数据，不能用于反写来源模块。

### 基础路径与认证

所有接口默认使用 `/api/v1/whitelist` 前缀。第二批合并后当前运行入口由 `admission-core-service` 承载，端口固定为 `8131`。历史原服务端口 `8110` 只作为 `legacyPort` 返回，不作为当前运行入口、网关上游或测试入口。

### 本地测试控制头

`whitelist` 允许在本地自动化测试中使用 `X-Test-Profile-Mode`、`X-Test-Notification-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Fail-After-Profile` 模拟依赖失败、通知失败、审计失败、状态写入失败和 profile 激活后补偿失败。生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、状态失败、通知失败或补偿失败。

### 网关可信身份上下文

只有 `X-Gateway-Internal-Request-Id` 存在时，`whitelist` 才进入可信上下文解析；若该头缺失，即使请求带有 `X-Beiming-Actor-*`，也必须忽略这些头并继续走 `Authorization: Bearer <token>` 兼容路径。可信上下文缺少 `X-Beiming-Actor-User-Id`、角色枚举不兼容或字段无法解析时返回 HTTP `502` 和 `47002`。

当前用户接口使用 `/api/v1/whitelist/me` 前缀，全部要求 `Authorization: Bearer <token>`，只能访问当前认证用户自己的白名单申请。浏览器请求体不得传入 `userId`、`roles`、`permissions`、`minecraftBindingSnapshot`、`examResult`、`scoreSummary`、`reviewerId`、`reviewerSnapshot`、`status`、`profileStatus`、`attendanceStatus`、`createdBy`、`updatedBy` 等服务端可信字段。

后台接口使用 `/api/v1/whitelist/admin` 前缀，全部要求登录。后台读取申请列表和详情要求 `HELPER`、`ADMIN` 或 `OWNER`。审核通过、审核拒绝、要求补充、移除、重开、attendance handoff 读取、审计读取和自检摘要要求 `ADMIN` 或 `OWNER`。`HELPER` 可以查看审核队列和详情，可以领取或分配给自己做初审记录，但不能执行最终通过、拒绝、移除或重开。

### 前序服务兼容契约

`auth` 是强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions`、`status` 和 `minecraftBinding`。用户状态为 `PENDING_PROFILE` 或 `ACTIVE` 时可创建、补充、撤回和读取自己的申请；`DISABLED`、`BANNED`、`DELETED` 不允许写入。auth 不可用返回 `47000`，auth 超时返回 `47001`，字段或枚举不兼容返回 `47002`。

`exam` 是创建申请的强依赖。创建白名单申请时必须读取 `GET /api/v1/exams/admin/sessions/{sessionId}/whitelist-handoff` 或未来等价服务间适配器。只有交接快照满足 `result=PASSED`、`passedAt` 不为空、`userId` 与当前用户一致、`minecraftBindingSnapshot` 完整、`handoffVersion` 未被当前 whitelist 申请消费过时，才允许创建申请。exam 不可用返回 `47010`，超时返回 `47011`，字段不兼容返回 `47012`，交接状态不允许返回 `44010`。

当用户历史申请已被移除或重开为 `REAPPLYING` 时，新建申请必须消费 `attemptType=RECHECK` 的 exam handoff。再次使用 `FIRST_TIME` handoff 创建二次入服申请必须返回 `44019` 或 `44010`，实现固定为 `44019`，并不得创建申请。

`profile` 是审核通过和移除白名单的强依赖。审核通过时必须通过 `POST /api/v1/profile/admin/members/activate` 或未来等价服务间适配器创建或激活成员档案，并保存调用结果摘要。移除白名单时必须通过 `PATCH /api/v1/profile/admin/members/{memberId}/status` 或未来等价适配器把成员状态流转为 `REMOVED` 或契约允许的目标状态。profile 不可用返回 `47020`，超时返回 `47021`，字段不兼容返回 `47022`，成员激活冲突返回 `44020`。profile 激活失败时申请不得进入 `APPROVED`，必须进入 `APPROVAL_BLOCKED` 或保持可复核状态。

`notification` 用于投递申请提交、要求补充、补充提交、审核通过、审核拒绝、移除和允许重新申请通知。P0 中通知是辅助依赖，失败不得回滚主状态，但必须保存 `notificationStatus=FAILED`、失败原因摘要并写入审计。notification 不可用记录或返回 `47030`，超时记录或返回 `47031`，字段不兼容记录或返回 `47032`。

通知失败摘要必须能区分不可用、超时和字段不兼容三类失败。当前用户接口和后台接口都可以返回 `notificationFailure` 脱敏摘要，便于前端展示和后台排障；摘要不得包含通知正文、完整请求头、认证 token、内部 URL 或异常堆栈。

`attendance` 当前未实现。审核通过后 `whitelist` 不得伪造积分初始化成功，只能生成 `attendanceInitializationStatus=WAITING_MODULE` 的交接摘要。后续 `attendance` 开发时只能通过本文档的 attendance handoff 接口或未来正式服务间接口消费摘要，不能直接读取 whitelist 数据库。

`onboarding` 不是白名单主数据来源。`whitelist` 可以保存 exam 快照中携带的 onboarding `applicationId` 和 `onboardingHandoffVersion`，但不能要求 onboarding 回写白名单状态。

`admin` 当前只是聚合入口。本文档不修改 `admin` 契约。若后续需要 admin 展示白名单待办、指标或审计索引，必须作为 admin 兼容变更单独走契约、测试、红灯、实现和回归闭环。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `WhitelistApplicationStatus` | `DRAFT`、`PENDING_REVIEW`、`UNDER_REVIEW`、`NEEDS_SUPPLEMENT`、`SUPPLEMENT_SUBMITTED`、`APPROVAL_BLOCKED`、`APPROVED`、`REJECTED`、`WITHDRAWN`、`REMOVED`、`REAPPLYING`、`ARCHIVED` | 白名单申请状态。P0 创建后默认进入 `PENDING_REVIEW`，保留 `DRAFT` 兼容前端草稿。 |
| `WhitelistResult` | `PENDING`、`NEEDS_SUPPLEMENT`、`APPROVED`、`REJECTED`、`WITHDRAWN`、`REMOVED` | 对外归一结果。 |
| `ReviewDirection` | `REDSTONE`、`LATE_GAME`、`BUILDING`、`GENERAL` | 审核方向，必须兼容 onboarding 和 exam。 |
| `WhitelistAttemptType` | `FIRST_TIME`、`RECHECK` | 首次入服或二次考核，来自 exam。 |
| `NotificationStatus` | `DELIVERED`、`FAILED`、`SKIPPED` | 最近一次通知摘要。 |
| `ProfileActivationStatus` | `PENDING`、`ACTIVATED`、`FAILED`、`SKIPPED` | profile 激活摘要。 |
| `AttendanceInitializationStatus` | `WAITING_MODULE`、`READY_FOR_CONSUME`、`CONSUMED`、`FAILED` | P0 审核通过后固定为 `WAITING_MODULE` 或 `READY_FOR_CONSUME`，不得返回已初始化积分。 |
| `WhitelistAuditResult` | `SUCCESS`、`FAILED` | whitelist 审计结果。 |

### 通用对象

#### WhitelistApplication

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `applicationId` | string | 是 | 白名单申请 ID。 |
| `examSessionId` | string | 是 | 来源考试实例 ID。 |
| `onboardingApplicationId` | string | 是 | 来源 onboarding 流程实例 ID。 |
| `examHandoffVersion` | integer | 是 | 来源 exam 交接版本。 |
| `onboardingHandoffVersion` | integer | 是 | 来源 onboarding 交接版本。 |
| `userId` | string | 是 | auth 用户 ID。当前用户接口中固定为认证用户。 |
| `displayNameSnapshot` | string | 是 | 创建申请时的展示名快照。 |
| `minecraftBindingSnapshot` | object | 是 | Minecraft 绑定快照，兼容 auth 的 `MinecraftBinding`。 |
| `reviewDirection` | string | 是 | 审核方向。 |
| `attemptType` | string | 是 | 首次或二次考核。 |
| `status` | string | 是 | `WhitelistApplicationStatus`。 |
| `result` | string | 是 | `WhitelistResult`。 |
| `materials` | WhitelistMaterial[] | 是 | 申请材料。 |
| `scoreSummary` | object | 是 | exam 成绩摘要。 |
| `examPassedAt` | string | 是 | exam 通过时间。 |
| `reviewerUserId` | string 或 null | 是 | 当前审核人。 |
| `reviewerDisplayNameSnapshot` | string 或 null | 是 | 审核人展示名快照。 |
| `reviewComment` | string 或 null | 是 | 给申请人的审核意见。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注，当前用户接口不得返回。 |
| `supplementRequest` | WhitelistSupplementRequest 或 null | 是 | 最近一次补充要求。 |
| `profileActivation` | WhitelistProfileActivationSummary 或 null | 是 | 成员档案激活摘要。 |
| `attendanceHandoff` | WhitelistAttendanceHandoffSnapshot 或 null | 后台可见 | 给 attendance 的初始化交接摘要。当前用户结果只返回状态摘要。 |
| `notificationStatus` | string 或 null | 是 | 最近一次通知结果。 |
| `notificationFailure` | WhitelistNotificationFailureSummary 或 null | 是 | 最近一次通知失败脱敏摘要。通知成功时为 `null`。 |
| `removedAt` | string 或 null | 是 | 移除时间。 |
| `removedBy` | string 或 null | 是 | 移除操作者。 |
| `removalReason` | string 或 null | 后台可见 | 移除原因。当前用户结果只返回公开说明。 |
| `reapplyRequired` | boolean | 是 | 是否需要重新申请。 |
| `nextExamAttemptType` | string 或 null | 是 | 移除后建议的下一次考试类型。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `submittedAt` | string 或 null | 是 | 提交审核时间。 |
| `reviewedAt` | string 或 null | 是 | 最近审核时间。 |
| `approvedAt` | string 或 null | 是 | 审核通过时间。 |
| `rejectedAt` | string 或 null | 是 | 审核拒绝时间。 |
| `withdrawnAt` | string 或 null | 是 | 撤回时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |

#### WhitelistMaterial

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `materialId` | string | 是 | 材料 ID。 |
| `type` | string | 是 | `TEXT`、`LINK`、`IMAGE` 或 `OTHER`。P0 可只接受 `TEXT` 和 `LINK`。 |
| `title` | string | 是 | 2 到 80 位。 |
| `content` | string | 是 | 1 到 2000 位。链接必须为 http、https 或站内路径。 |
| `publicVisibleToApplicant` | boolean | 是 | 是否对申请人可见。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### WhitelistSupplementRequest

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `requestId` | string | 是 | 补充要求 ID。 |
| `publicComment` | string | 是 | 给申请人的补充说明，1 到 1000 位。 |
| `dueAt` | string 或 null | 是 | 补充截止时间，必须晚于当前时间。 |
| `requestedBy` | string | 是 | 操作者用户 ID。 |
| `requestedAt` | string | 是 | 要求补充时间。 |
| `submittedAt` | string 或 null | 是 | 用户提交补充时间。 |
| `materials` | WhitelistMaterial[] | 是 | 本次补充材料。 |

#### WhitelistProfileActivationSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | `PENDING`、`ACTIVATED`、`FAILED` 或 `SKIPPED`。 |
| `memberId` | string 或 null | 是 | profile 成员档案 ID。 |
| `profileStatus` | string 或 null | 是 | profile 返回的成员状态。 |
| `calledAt` | string 或 null | 是 | 调用 profile 时间。 |
| `failureCode` | string 或 null | 是 | 失败码摘要。 |
| `failureReason` | string 或 null | 是 | 脱敏失败原因。 |

#### WhitelistNotificationFailureSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | 固定为 `FAILED`。 |
| `failureCode` | string | 是 | `47030`、`47031` 或 `47032`。 |
| `failureType` | string | 是 | `UNAVAILABLE`、`TIMEOUT` 或 `BAD_SCHEMA`。 |
| `failureReason` | string | 是 | 脱敏失败摘要，不得包含通知正文、请求头、token 或堆栈。 |
| `failedAt` | string | 是 | 失败发生时间。 |

#### WhitelistAttendanceHandoffSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `handoffId` | string | 是 | attendance 交接 ID。 |
| `applicationId` | string | 是 | 白名单申请 ID。 |
| `userId` | string | 是 | 用户 ID。 |
| `memberId` | string | 是 | profile 成员档案 ID。 |
| `minecraftBindingSnapshot` | object | 是 | Minecraft 绑定快照。 |
| `reviewDirection` | string | 是 | 审核方向。 |
| `attemptType` | string | 是 | 首次或二次考核。 |
| `approvedAt` | string | 是 | 白名单通过时间。 |
| `scoreSummary` | object | 是 | exam 成绩摘要。 |
| `initializationStatus` | string | 是 | P0 固定为 `WAITING_MODULE` 或 `READY_FOR_CONSUME`。 |
| `handoffVersion` | integer | 是 | 交接版本，从 `1` 开始。 |
| `generatedAt` | string | 是 | 生成时间。 |
| `consumedAt` | string 或 null | 是 | P0 固定为 `null`。 |

#### WhitelistAuditLog

审计字段继承公共契约，允许补充 `applicationId`、`examSessionId`、`stateFrom`、`stateTo`、`reviewDirection`、`attemptType`、`idempotencyKey`、`notificationStatus`、`profileActivationStatus`、`attendanceHandoffStatus` 和 `dependencyStatus`。审计日志不得通过 whitelist API 删除。

### whitelist 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `44000` | 404 | 白名单申请不存在，或当前用户无权访问。 |
| `44001` | 404 | 补充要求不存在。 |
| `44002` | 404 | 白名单审计记录不存在。 |
| `44010` | 409 | exam 交接状态不允许创建白名单申请。 |
| `44011` | 409 | 当前用户与 exam 交接用户不一致。 |
| `44012` | 409 | exam 交接快照已被消费。 |
| `44013` | 409 | 当前用户已有未归档白名单申请。 |
| `44014` | 409 | 当前申请状态不允许该操作。 |
| `44015` | 409 | 申请材料不完整或不允许编辑。 |
| `44016` | 409 | 补充材料不满足要求。 |
| `44017` | 409 | whitelist 幂等键请求指纹冲突。 |
| `44018` | 409 | 当前申请已产生下游结果，不能撤回或重复审核。 |
| `44019` | 409 | 当前申请已移除，必须走重新申请。 |
| `44020` | 409 | profile 激活冲突，白名单不能进入通过终态。 |
| `44021` | 409 | attendance 交接摘要尚未生成。 |
| `44022` | 409 | 当前申请不允许重开或重新申请。 |
| `47000` | 502 | auth 认证上下文不可用。 |
| `47001` | 504 | auth 认证上下文调用超时。 |
| `47002` | 502 | auth 认证上下文字段或枚举不兼容 whitelist 契约。 |
| `47010` | 502 | exam whitelist 交接快照不可用。 |
| `47011` | 504 | exam whitelist 交接快照调用超时。 |
| `47012` | 502 | exam whitelist 交接快照字段不兼容 whitelist 契约。 |
| `47020` | 502 | profile 成员激活或状态变更不可用。 |
| `47021` | 504 | profile 成员激活或状态变更调用超时。 |
| `47022` | 502 | profile 响应字段不兼容 whitelist 契约。 |
| `47030` | 502 | notification 投递不可用。 |
| `47031` | 504 | notification 投递超时。 |
| `47032` | 502 | notification 投递响应不兼容 whitelist 契约。 |
| `52000` | 500 | whitelist 内部错误。 |
| `52001` | 500 | whitelist 审计写入失败。 |
| `52002` | 500 | whitelist 状态写入失败。 |
| `52003` | 500 | whitelist 下游补偿状态写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误和通用服务端错误优先使用公共错误码。whitelist 自有幂等指纹冲突使用 `44017`。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 创建白名单申请 | POST | `/api/v1/whitelist/me/applications` | 是 | 当前用户 | LOW |
| 当前申请 | GET | `/api/v1/whitelist/me/applications/current` | 是 | 当前用户 | LOW |
| 当前用户申请历史 | GET | `/api/v1/whitelist/me/applications` | 是 | 当前用户 | LOW |
| 当前用户申请详情 | GET | `/api/v1/whitelist/me/applications/{applicationId}` | 是 | 当前用户 | LOW |
| 修改申请材料 | PATCH | `/api/v1/whitelist/me/applications/{applicationId}` | 是 | 当前用户 | LOW |
| 提交审核 | POST | `/api/v1/whitelist/me/applications/{applicationId}/submit` | 是 | 当前用户 | LOW |
| 提交补充材料 | PATCH | `/api/v1/whitelist/me/applications/{applicationId}/supplement` | 是 | 当前用户 | LOW |
| 撤回申请 | PATCH | `/api/v1/whitelist/me/applications/{applicationId}/withdraw` | 是 | 当前用户 | MEDIUM |
| 读取审核结果 | GET | `/api/v1/whitelist/me/applications/{applicationId}/result` | 是 | 当前用户 | LOW |
| 后台申请列表 | GET | `/api/v1/whitelist/admin/applications` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台申请详情 | GET | `/api/v1/whitelist/admin/applications/{applicationId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 分配审核人 | PATCH | `/api/v1/whitelist/admin/applications/{applicationId}/assign` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 要求补充 | PATCH | `/api/v1/whitelist/admin/applications/{applicationId}/request-supplement` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核通过 | PATCH | `/api/v1/whitelist/admin/applications/{applicationId}/approve` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/whitelist/admin/applications/{applicationId}/reject` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 移除白名单 | PATCH | `/api/v1/whitelist/admin/applications/{applicationId}/remove` | 是 | `ADMIN` 或 `OWNER` | HIGH |
| 允许重新申请 | POST | `/api/v1/whitelist/admin/applications/{applicationId}/reopen` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| attendance 交接摘要 | GET | `/api/v1/whitelist/admin/applications/{applicationId}/attendance-handoff` | 是 | `ADMIN` 或 `OWNER` | LOW |
| whitelist 审计列表 | GET | `/api/v1/whitelist/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| whitelist 自检摘要 | GET | `/api/v1/whitelist/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

### 当前用户接口

#### 创建白名单申请

`POST /api/v1/whitelist/me/applications`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `examSessionId` | string | 是 | 已通过考试实例 ID。 |
| `materials` | array | 否 | 创建时提交的材料，最多 20 条。P0 可为空。 |
| `publicComment` | string | 否 | 申请说明，最多 1000 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `WhitelistApplication`。如果同一用户同一通过考试已经存在未归档申请，返回 HTTP `200` 和现有申请，除非已有终态要求重开。

业务规则：服务端必须从 exam 读取 whitelist handoff，不能信任浏览器传入的考试结果。只允许 `result=PASSED` 创建申请。交接用户必须等于当前用户。Minecraft 绑定快照必须完整。一个 exam handoff 只能消费一次。创建后默认进入 `PENDING_REVIEW`，`result=PENDING`。创建成功后尝试发送通知，通知失败不回滚。

幂等规则：同一用户、同一 `idempotencyKey`、同一请求体重复提交时返回同一申请。相同幂等键搭配不同请求体返回 `44017`。并发创建同一 exam handoff 只能成功产生一个申请。

审计要求：成功写入 `WHITELIST_APPLICATION_CREATED`。exam 交接失败、用户不匹配、交接已消费和审计失败都必须可追踪。审计失败返回 `52001`，不得创建申请。

#### 当前申请

`GET /api/v1/whitelist/me/applications/current`

成功响应 HTTP `200`，`data` 为当前未归档、未撤回且仍可见的 `WhitelistApplication`，没有当前申请时 `data=null`。

业务规则：只返回当前用户自己的申请。`APPROVED`、`REJECTED`、`REMOVED` 可按最新结果作为当前结果返回，`ARCHIVED` 不作为当前申请。当前用户视图不得返回 `internalNote`、完整移除原因、审计参数、profile 失败堆栈或 notification 正文。

#### 当前用户申请历史

`GET /api/v1/whitelist/me/applications`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `status` | string | 否 | 任一 `WhitelistApplicationStatus`。 |
| `result` | string | 否 | 任一 `WhitelistResult`。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`updatedAt_desc`、`reviewedAt_desc`、`approvedAt_desc`。默认 `createdAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为当前用户可见的 `WhitelistApplication[]`。

#### 当前用户申请详情

`GET /api/v1/whitelist/me/applications/{applicationId}`

成功响应 HTTP `200`，`data` 为当前用户视图的 `WhitelistApplication`。申请不存在、已归档不可见或不属于当前用户返回 `44000`，不得暴露他人申请是否存在。

#### 修改申请材料

`PATCH /api/v1/whitelist/me/applications/{applicationId}`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `materials` | array | 是 | 0 到 20 条，整体替换当前可编辑材料。 |
| `publicComment` | string | 否 | 申请说明，最多 1000 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只允许 `DRAFT`、`PENDING_REVIEW`、`NEEDS_SUPPLEMENT` 和 `SUPPLEMENT_SUBMITTED` 的当前用户申请修改材料。`UNDER_REVIEW` 是否允许修改由实现固定，推荐返回 `44014`。`APPROVED`、`REJECTED`、`WITHDRAWN`、`REMOVED`、`ARCHIVED` 不允许修改。浏览器不得传入可信状态字段。

审计要求：成功写入 `WHITELIST_MATERIALS_UPDATED`。

#### 提交审核

`POST /api/v1/whitelist/me/applications/{applicationId}/submit`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：`DRAFT` 可提交到 `PENDING_REVIEW`。P0 创建后已经是 `PENDING_REVIEW` 时重复提交返回成功并保持幂等。`NEEDS_SUPPLEMENT` 不允许走 submit，必须走 supplement。终态申请返回 `44014` 或 `44018`。

#### 提交补充材料

`PATCH /api/v1/whitelist/me/applications/{applicationId}/supplement`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `materials` | array | 是 | 1 到 20 条补充材料。 |
| `publicComment` | string | 否 | 补充说明，最多 1000 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只有 `NEEDS_SUPPLEMENT` 可提交补充。成功后进入 `SUPPLEMENT_SUBMITTED`，`result=PENDING`。补充截止已过时返回 `44014` 或模块固定错误。通知失败不回滚。

审计要求：成功写入 `WHITELIST_SUPPLEMENT_SUBMITTED`。

#### 撤回申请

`PATCH /api/v1/whitelist/me/applications/{applicationId}/withdraw`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，撤回原因。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只允许撤回 `DRAFT`、`PENDING_REVIEW`、`NEEDS_SUPPLEMENT` 和 `SUPPLEMENT_SUBMITTED`。`UNDER_REVIEW` 可否撤回由实现固定，推荐返回 `44014`。已通过、已拒绝、已移除、已归档或已产生 profile 激活结果的申请不得撤回。

审计要求：成功写入 `WHITELIST_APPLICATION_WITHDRAWN`。

#### 读取审核结果

`GET /api/v1/whitelist/me/applications/{applicationId}/result`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "applicationId": "wl_xxx",
    "status": "APPROVED",
    "result": "APPROVED",
    "reviewComment": "审核通过",
    "profileActivationStatus": "ACTIVATED",
    "attendanceInitializationStatus": "WAITING_MODULE",
    "notificationStatus": "DELIVERED",
    "reviewedAt": "2026-05-23T12:00:00Z"
  }
}
```

业务规则：只返回当前用户自己的结果摘要。不得返回内部备注、profile 失败堆栈、审计参数、通知正文、考题答案或后台移除原因全文。

### 后台接口

#### 后台申请列表

`GET /api/v1/whitelist/admin/applications`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配申请 ID、用户 ID、展示名、Minecraft ID 或 examSessionId，最多 80 位。 |
| `status` | string | 否 | 任一 `WhitelistApplicationStatus`。 |
| `result` | string | 否 | 任一 `WhitelistResult`。 |
| `reviewDirection` | string | 否 | 任一 `ReviewDirection`。 |
| `attemptType` | string | 否 | `FIRST_TIME` 或 `RECHECK`。 |
| `reviewerUserId` | string | 否 | 审核人用户 ID。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`updatedAt_desc`、`submittedAt_desc`、`reviewedAt_desc`、`approvedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为后台视图 `WhitelistApplication[]`。

#### 后台申请详情

`GET /api/v1/whitelist/admin/applications/{applicationId}`

成功响应 HTTP `200`，`data` 为后台视图 `WhitelistApplication`。申请不存在返回 `44000`。响应不得返回 token、完整请求头、Minecraft 验证凭据、考试正确答案、通知正文、profile 内部堆栈、真实服务器命令或节点凭据。

#### 分配审核人

`PATCH /api/v1/whitelist/admin/applications/{applicationId}/assign`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reviewerUserId` | string | 否 | 目标审核人。为空时默认当前操作者。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只允许 `PENDING_REVIEW`、`SUPPLEMENT_SUBMITTED` 和 `UNDER_REVIEW` 分配审核人。成功后进入 `UNDER_REVIEW`。`HELPER` 只能把申请分配给自己，不能分配给其他人。`ADMIN` 和 `OWNER` 可分配给任一具备后台读取权限的用户。

审计要求：成功写入 `WHITELIST_REVIEW_ASSIGNED`。

#### 要求补充

`PATCH /api/v1/whitelist/admin/applications/{applicationId}/request-supplement`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `publicComment` | string | 是 | 1 到 1000 位，说明需补充内容。 |
| `dueAt` | string | 否 | 必须晚于当前时间且不超过 14 天。 |
| `internalNote` | string | 否 | 最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只允许 `PENDING_REVIEW`、`UNDER_REVIEW` 和 `SUPPLEMENT_SUBMITTED`。成功后进入 `NEEDS_SUPPLEMENT`，`result=NEEDS_SUPPLEMENT`。通知失败不回滚，但必须记录。

`dueAt` 必须晚于服务端当前时间，且不得超过服务端当前时间后 14 天。超过 14 天、格式非法或早于当前时间时返回 `40001`，不得改变申请状态。

#### 审核通过

`PATCH /api/v1/whitelist/admin/applications/{applicationId}/approve`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reviewComment` | string | 是 | 1 到 1000 位，给申请人看的通过说明。 |
| `internalNote` | string | 否 | 最多 1000 位。 |
| `profileGroupId` | string | 否 | 传给 profile 的成员组 ID。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只允许 `PENDING_REVIEW`、`UNDER_REVIEW` 和 `SUPPLEMENT_SUBMITTED`。审核通过必须先完成本地校验，再调用 profile 激活成员档案。profile 激活成功后进入 `APPROVED`，`result=APPROVED`，生成 `attendanceHandoff`，`attendanceInitializationStatus=WAITING_MODULE` 或 `READY_FOR_CONSUME`，并尝试发送通知。profile 激活失败时不得进入 `APPROVED`，必须进入 `APPROVAL_BLOCKED`，保存失败摘要并写审计。notification 失败不回滚已通过状态。

profile 不可用、超时或响应字段不兼容时，审核通过不得进入 `APPROVED`，必须返回 `47020`、`47021` 或 `47022`，并保持申请可复核。若 profile 已确认激活成功但 whitelist 本地状态写入失败，必须返回 `52003`，把申请保留为 `APPROVAL_BLOCKED` 或等价可复核状态，保存 `profileActivation.status=ACTIVATED`、失败摘要和审计线索，后续不得对用户伪造成已通过。

审计要求：成功写入 `WHITELIST_APPROVED` 和 `WHITELIST_PROFILE_ACTIVATED`。profile 失败写入 `WHITELIST_PROFILE_ACTIVATION_FAILED`。审计失败时不得调用 profile，不得改变状态。

#### 审核拒绝

`PATCH /api/v1/whitelist/admin/applications/{applicationId}/reject`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reviewComment` | string | 是 | 1 到 1000 位，给申请人看的拒绝说明。 |
| `internalNote` | string | 否 | 最多 1000 位。 |
| `allowReapply` | boolean | 否 | 默认 `true`。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只允许 `PENDING_REVIEW`、`UNDER_REVIEW`、`SUPPLEMENT_SUBMITTED` 和 `APPROVAL_BLOCKED`。成功后进入 `REJECTED`，`result=REJECTED`。不得调用 profile 激活，不得生成 attendance handoff。通知失败不回滚。

#### 移除白名单

`PATCH /api/v1/whitelist/admin/applications/{applicationId}/remove`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `publicComment` | string | 是 | 1 到 1000 位，给成员看的移除说明。 |
| `reason` | string | 是 | 1 到 200 位，后台移除原因。 |
| `confirmText` | string | 是 | 二次确认文本，P0 固定要求 `REMOVE_WHITELIST`。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：该接口是 `HIGH` 风险，必须要求 `ADMIN` 或 `OWNER`，并校验二次确认。只允许 `APPROVED` 申请移除。成功后状态进入 `REMOVED`，`result=REMOVED`，`reapplyRequired=true`，`nextExamAttemptType=RECHECK`。必须调用 profile 状态接口把成员档案流转为 `REMOVED` 或契约允许目标状态。不得直接执行真实服务器白名单命令，不得写服务器文件。profile 状态变更失败时不得进入 `REMOVED`，必须返回依赖错误或保持原状态。

profile 移除状态接口不可用、超时或响应字段不兼容时，移除操作不得进入 `REMOVED`。若 profile 已确认状态变更成功但 whitelist 本地状态写入失败，必须返回 `52003`，保留可复核摘要，避免官网状态和成员档案状态长期不一致且无法追踪。

审计要求：成功写入 `WHITELIST_REMOVED`，记录为 `HIGH` 风险。重复移除同一申请保持幂等，不重复写审计。

#### 允许重新申请

`POST /api/v1/whitelist/admin/applications/{applicationId}/reopen`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `publicComment` | string | 是 | 1 到 1000 位，给用户看的重新申请说明。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只允许 `REJECTED`、`WITHDRAWN` 或 `REMOVED` 的历史申请重开为 `REAPPLYING`，并标记下一次考试类型。重开不创建新考试、不创建新白名单申请、不修改 exam 题库、不初始化积分。用户仍必须重新通过 exam 交接创建新申请。通知失败不回滚。

#### attendance 交接摘要

`GET /api/v1/whitelist/admin/applications/{applicationId}/attendance-handoff`

成功响应 HTTP `200`，`data` 为 `WhitelistAttendanceHandoffSnapshot`。

业务规则：只有 `APPROVED` 且 profile 激活成功的申请可以读取。未通过、未激活或交接摘要未生成返回 `44021` 或 `44014`。该接口只提供后续 `attendance` 初始化积分所需只读快照，不创建积分流水，不创建榜单，不推进 attendance 状态。读取是低风险，但必须写入可追溯的读取审计或自检计数，具体实现固定。

#### whitelist 审计列表

`GET /api/v1/whitelist/admin/audit-logs`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `applicationId` | string | 否 | 白名单申请 ID。 |
| `actorUserId` | string | 否 | 操作者用户 ID。 |
| `action` | string | 否 | 动作，最多 80 位。 |
| `result` | string | 否 | `SUCCESS` 或 `FAILED`。 |
| `from` | string | 否 | ISO 8601 起始时间。 |
| `to` | string | 否 | ISO 8601 结束时间。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`。默认 `createdAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `WhitelistAuditLog[]`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 whitelist API 删除，返回结果必须脱敏。

#### whitelist 自检摘要

`GET /api/v1/whitelist/admin/ops/summary`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "whitelist",
    "port": 8131,
    "legacyPort": 8110,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "examMode": "TEST_STUB",
    "profileMode": "TEST_STUB",
    "notificationMode": "TEST_STUB",
    "applicationsTotal": 8,
    "pendingReviewTotal": 2,
    "approvedTotal": 3,
    "rejectedTotal": 1,
    "removedTotal": 1,
    "approvalBlockedTotal": 1,
    "attendanceHandoffsTotal": 3,
    "auditsTotal": 40,
    "idempotencyRecordsTotal": 10,
    "lastAuditAt": "2026-05-23T12:00:00Z",
    "productionGaps": [
      "P0_IN_MEMORY_STORAGE",
      "P0_AUTH_STUB",
      "P0_EXAM_STUB",
      "P0_PROFILE_STUB",
      "P0_NOTIFICATION_STUB",
      "ATTENDANCE_NOT_IMPLEMENTED",
      "REAL_SERVER_WHITELIST_NOT_CONNECTED"
    ]
  }
}
```

业务规则：自检摘要用于后台确认 whitelist 当前运行模式、待审核规模、通过规模、移除规模、profile 阻塞规模、attendance 交接规模和生产化缺口。摘要不得返回 token、请求头、Minecraft 验证凭据、考试答案、profile 内部备注、通知正文、审计参数全文、真实服务器命令、节点凭据或异常堆栈。

### 状态、幂等和并发

创建成功后 P0 默认进入 `PENDING_REVIEW`。`DRAFT` 可提交为 `PENDING_REVIEW`。`PENDING_REVIEW` 可分配为 `UNDER_REVIEW`，可要求补充为 `NEEDS_SUPPLEMENT`，可审核通过为 `APPROVED`，可审核拒绝为 `REJECTED`，可撤回为 `WITHDRAWN`。`UNDER_REVIEW` 可要求补充、通过或拒绝。`NEEDS_SUPPLEMENT` 只可由用户补充为 `SUPPLEMENT_SUBMITTED` 或撤回。`SUPPLEMENT_SUBMITTED` 可重新进入审核、通过、拒绝或再次要求补充。`APPROVAL_BLOCKED` 可重试通过流程或拒绝，不能对用户伪造成已通过。`APPROVED` 可移除为 `REMOVED`。`REJECTED`、`WITHDRAWN` 和 `REMOVED` 可由后台标记为 `REAPPLYING`，但新申请必须重新消费新的 exam 通过交接。`ARCHIVED` 为终态。

状态推进只能由服务端根据 exam handoff、当前申请状态、审核动作、profile 调用结果、二次确认和权限判断决定。非法状态跳跃返回 `44014`。浏览器传入可信字段必须忽略或返回字段校验失败。

创建申请、修改材料、提交审核、提交补充、撤回、分配审核人、要求补充、审核通过、审核拒绝、移除和重开支持 `idempotencyKey`。同一操作者、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `44017`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序。

并发创建同一 exam handoff 只能产生一个申请。并发审核同一申请只能有一个最终结果。并发补充和审核必须以服务端当前状态为准，不得产生补充后又被旧审核覆盖的半状态。读取接口允许读到更新前或更新后的完整状态，不能返回半更新对象。

### 审计要求

必须审计的动作包括创建申请、修改材料、提交审核、提交补充、撤回、分配审核人、要求补充、审核通过、审核拒绝、profile 激活成功、profile 激活失败、notification 投递失败、移除白名单、允许重新申请、attendance handoff 读取、依赖降级导致操作不可继续、审计写入失败和状态写入失败。

后台写操作必须记录 `reason`、操作者、目标对象、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计不得泄露 token、完整请求头、Minecraft 验证凭据、考试正确答案、profile 后台备注全文、通知正文全文、真实服务器命令、节点凭据、内部异常堆栈或前序服务内部路径。

审计写入失败时，创建申请、材料修改、提交补充、撤回、审核、移除和重开不得假装成功，必须返回 `52001` 或 `52000`，并保持业务数据不变。

### 失败降级

auth 是所有接口强依赖。auth 不可用、超时、用户状态不允许或字段不兼容时不得伪造成功。

exam 是创建申请强依赖。交接快照不可用、未通过、已消费、用户不匹配、Minecraft 绑定缺失或字段不兼容时不得创建申请。

profile 是审核通过和移除白名单强依赖。profile 激活失败时申请不得进入 `APPROVED`。profile 移除状态失败时申请不得进入 `REMOVED`。如果 profile 已激活但 whitelist 状态写入失败，必须记录 `52003` 风险并提供可复核状态，不能吞成成功。

notification 默认是辅助依赖。通知失败不得回滚创建、补充、通过、拒绝、移除或重开，但必须记录失败摘要和审计。

通知失败摘要至少要能区分不可用、超时和字段不兼容三类失败。P0 可统一保存为 `notificationStatus=FAILED`，但必须同时保存 `notificationFailure.failureCode`、`notificationFailure.failureType` 和脱敏 `failureReason`，审计中也必须保留同等级别的脱敏失败线索，不得保存通知正文或完整请求头。

attendance 未实现时，审核通过仍可完成 whitelist 和 profile 激活，但只能返回 `attendanceInitializationStatus=WAITING_MODULE` 或 `READY_FOR_CONSUME`。不得返回积分已初始化、不得创建积分流水、不得维护榜单。

真实服务器白名单写入未接入时，审核通过只代表官网业务白名单通过和 profile 激活，不代表已经执行 Minecraft 服务器命令。P0 必须在自检摘要中暴露 `REAL_SERVER_WHITELIST_NOT_CONNECTED`。

### 验收口径

`whitelist` API 文档按 `docs/contracts-whitelist.md` 独立存在，并由 `.local-docs/tests-whitelist.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`whitelist` 完成时必须满足以下条件：全部接口按本文档实现；当前用户接口只能访问自己的申请；后台接口按角色限制；创建申请只通过 exam handoff 和前序适配读取快照，不直接读前序服务实现；审核通过必须通过 profile 正式接口激活成员档案；profile 激活失败不进入通过终态；通知失败按辅助降级记录；attendance 未实现时只生成交接摘要；移除白名单不执行真实服务器命令；当前运行入口为 `admission-core-service:8131`，历史端口只作为 `legacyPort=8110` 返回；`.local-docs/tests-whitelist.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 whitelist 全部测试通过；auth、profile、notification、content、server-status、resource、admin、onboarding 和 exam 前序服务回归测试通过；没有修改前序服务稳定接口；没有把考勤积分、社区工单、活动、日历、更新日志、后台聚合、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复或 Cloudreve 管理能力塞进 whitelist。

## 北冥官网 attendance API 契约

来源：`docs/contracts-attendance.md`

版本：0.1

### 文档定位

本文档是 `attendance` 微服务的正式 API 契约。后续 `community`、`activity`、`calendar`、`changelog`、前端适配、`admin` 聚合、`ops-control` 和 `external-node-executor` 只能通过本文档定义的接口读取考勤账户、积分流水、贡献记录、月度扣分、榜单、白名单移除候选、审计和自检摘要，不能直接读取或修改 `attendance` 数据库，也不能把考勤积分逻辑塞进其他服务。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `attendance` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了现有优秀平台的成熟做法。Stack Overflow 的声望、徽章和权限说明证明积分必须来自可解释的行为，并且高积分只解锁更高信任能力，不应变成人工随意奖惩。GitHub 贡献图说明公开活跃展示需要明确什么行为计入贡献，且展示口径和真实数据来源要分开。Atlassian Jira workflow 的条件、校验器和后置动作说明状态推进前要先校验，通知、审计等后置动作不能反过来绕开状态规则。Discord Rules Screening 说明准入流程中的门槛应由服务端记录，不只靠前端提示。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Stack Overflow reputation](https://stackoverflow.com/help/whats-reputation) | 积分代表社区信任，增减都要有可解释原因，并影响后续能力。 |
| [Stack Overflow privileges](https://stackoverflow.com/help/privileges) | 权限或能力应随可信度分层解锁，后台写操作不能只靠展示积分判断。 |
| [Stack Overflow badges](https://stackoverflow.com/help/what-are-badges) | 激励项应基于可量化行为，不应由人工随意发放。 |
| [GitHub profile contributions reference](https://docs.github.com/en/account-and-profile/reference/profile-contributions-reference) | 活跃展示必须定义计入口径、可见性和时间口径。 |
| [Atlassian Jira workflow validators](https://support.atlassian.com/jira-cloud-administration/docs/use-workflow-validators-with-custom-fields/) | 状态流转前必须做字段、权限和条件校验，失败时不得执行后置动作。 |
| [Discord Rules Screening FAQ](https://support.discord.com/hc/en-us/articles/1500000466882-Rules-Screening-FAQ) | 入门门槛需要被服务端记录和追踪，不能只依赖前端展示。 |

### 职责边界

`attendance` 负责成员考勤账户、积分余额、积分流水、贡献记录、月度扣分任务、榜单、白名单移除候选、初始化交接消费、幂等记录和自身审计。

`attendance` 不负责注册、登录、邀请码、会话、账号角色能力点、Minecraft 绑定主数据、成员档案主数据、考试判分、白名单申请审核、真实服务器在线采集、真实 Minecraft 白名单移除命令、社区帖子、活动报名、内容审核、资源下载、后台聚合入口、服务器运维控制、节点守护进程、容器、终端、文件管理、备份恢复或 Cloudreve 管理。

`attendance` 只能适配前序服务。它通过 `auth` 认证上下文读取当前用户和后台操作者，通过 `whitelist` 的 attendance handoff 初始化考勤账户，通过 `profile` 正式接口或未来服务间适配器校验成员状态并保存展示快照，通过 `notification` 投递积分变化、扣分预警和候选提醒。它不能要求前序服务反向写入 attendance 状态，不能导入前序服务内存存储、实体、Repository、测试种子或内部类。

### 数据归属

`attendance` 拥有以下主数据：成员考勤账户、积分余额、积分流水、贡献记录、月度周期、扣分运行记录、榜单快照、白名单移除候选、初始化交接消费记录、幂等记录、依赖调用摘要和 attendance 审计日志。

`attendance` 可以保存来自 `whitelist` 的 `handoffId`、`applicationId`、`userId`、`memberId`、`minecraftBindingSnapshot`、`reviewDirection`、`attemptType`、`approvedAt`、`scoreSummary`、`handoffVersion` 和 `generatedAt` 快照。它可以保存来自 `profile` 的成员展示名、头像、成员组和成员状态快照，可以保存来自 `notification` 的投递结果摘要。快照不是来源模块主数据，不能用于反写来源模块。

### 基础路径与认证

所有接口默认使用 `/api/v1/attendance` 前缀。第二批合并后当前运行入口由 `admission-core-service` 承载，端口固定为 `8131`。历史原服务端口 `8111` 只作为 `legacyPort` 返回，不作为当前运行入口、网关上游或测试入口。

公开接口只包括公开榜单，路径为 `/api/v1/attendance/leaderboard`，允许游客访问，但不得返回内部备注、扣分原因全文、管理员 ID、审计参数、通知失败详情或白名单移除候选详情。

当前用户接口使用 `/api/v1/attendance/me` 前缀，全部要求 `Authorization: Bearer <token>`，只能访问当前认证用户自己的考勤账户、流水、贡献记录和榜单位置。浏览器请求体不得传入 `userId`、`memberId`、`roles`、`permissions`、`score`、`balanceAfter`、`status`、`operatorUserId`、`sourceModule`、`sourceId`、`notificationStatus`、`profileSnapshot` 等服务端可信字段。

后台接口使用 `/api/v1/attendance/admin` 前缀，全部要求登录。后台读取账户、贡献、榜单、候选和自检摘要要求 `HELPER`、`ADMIN` 或 `OWNER`。初始化账户、积分调整、流水撤销、贡献记录创建或修正、月度扣分、候选确认或驳回、榜单重算和审计读取要求 `ADMIN` 或 `OWNER`。`HELPER` 可以读取低风险后台汇总，不能写积分，不能运行扣分任务，不能确认白名单移除候选。

### 本地测试控制头

attendance 允许在本地自动化测试中使用 `X-Test-Whitelist-Mode`、`X-Test-Profile-Mode`、`X-Test-Notification-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Fail-Ledger` 模拟依赖失败、通知失败和写入失败。该能力只服务测试闭环，不属于正式业务 API。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、状态失败、流水失败、通知失败或 profile stale。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 视为已满足的生产化硬化项。

### 网关可信身份上下文

只有 `X-Gateway-Internal-Request-Id` 存在时，`attendance` 才进入可信上下文解析；若该头缺失，即使请求带有 `X-Beiming-Actor-*`，也必须忽略这些头并继续走 `Authorization: Bearer <token>` 兼容路径。可信上下文缺少 `X-Beiming-Actor-User-Id`、角色枚举不兼容或字段无法解析时返回 HTTP `502` 和 `48002`。

### 前序服务兼容契约

`auth` 是强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `ACTIVE` 时可读取自己的考勤数据；`DISABLED`、`BANNED`、`DELETED` 不允许读取或写入。auth 不可用返回 `48000`，auth 超时返回 `48001`，字段或枚举不兼容返回 `48002`。

`whitelist` 是初始化强依赖。初始化考勤账户时必须读取 `GET /api/v1/whitelist/admin/applications/{applicationId}/attendance-handoff` 或未来等价服务间适配器。只有交接快照满足 `initializationStatus=WAITING_MODULE` 或 `READY_FOR_CONSUME`、`memberId` 和 `userId` 完整、`handoffVersion` 未被当前 attendance 消费过、申请仍允许初始化时，才允许创建考勤账户。同一 handoff 重放必须幂等返回同一个初始化结果，不能重复加初始积分。whitelist 不可用返回 `48010`，超时返回 `48011`，字段不兼容返回 `48012`，交接状态不允许返回 `45010`，交接已被其他初始化消费返回 `45011`。

`profile` 是初始化和后台详情的强依赖。初始化时必须校验 `memberId` 对应成员仍可激活考勤，推荐状态为 `ACTIVE`。profile 不可用、超时或字段不兼容时，初始化不得伪造成功，分别返回 `48020`、`48021` 或 `48022`。榜单和只读详情可以在 profile 不可用时降级使用已保存成员快照，但必须返回 `profileSnapshotStale=true` 或依赖摘要，且不得刷新为伪造资料。

`notification` 是辅助依赖。初始化成功、管理员调整、流水撤销、月度扣分、候选生成、候选确认和候选驳回可以触发通知。通知失败不得回滚积分主状态，但必须保存 `notificationStatus=FAILED`、失败原因摘要并写入审计。notification 不可用记录或返回 `48030`，超时记录或返回 `48031`，字段不兼容记录或返回 `48032`。通知失败摘要不得包含通知正文、完整请求头、认证 token、内部 URL 或异常堆栈。

`admin` 当前只是聚合入口。本文档不修改 `admin` 契约。若后续需要 admin 展示 attendance 待办、指标或审计索引，必须作为 admin 兼容变更单独走契约、测试、红灯、实现和回归闭环。

`server-status` 当前不作为 attendance 强依赖。真实在线时长未来可以通过独立事件或正式状态采集适配进入，但 P0 不得直接读取 server-status 历史快照扣分，避免把展示状态当作考勤事实。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `AttendanceAccountStatus` | `PENDING_INITIALIZATION`、`ACTIVE`、`FROZEN`、`REMOVAL_CANDIDATE`、`REMOVED`、`ARCHIVED` | 考勤账户状态。初始化成功后默认 `ACTIVE`。 |
| `AttendanceLedgerType` | `INITIAL_GRANT`、`ADMIN_ADJUSTMENT`、`ACTIVITY_REWARD`、`CONTRIBUTION_REWARD`、`MONTHLY_DEDUCTION`、`REVERSAL` | 积分流水类型。P0 活动和贡献奖励可由后台受控写入。 |
| `AttendanceLedgerStatus` | `POSTED`、`REVERSED` | 流水状态。撤销不会删除原流水，只生成反向流水。 |
| `ContributionType` | `ONLINE_ACTIVE`、`PROJECT_BUILD`、`EVENT_PARTICIPATION`、`WORK_SUBMISSION`、`HELPER_SUPPORT`、`MANUAL` | 贡献记录类型。P0 通过后台受控写入，未来由对应模块适配。 |
| `MonthlyRunStatus` | `PENDING`、`RUNNING`、`COMPLETED`、`FAILED`、`PARTIAL_FAILED` | 月度扣分运行状态。 |
| `RemovalCandidateStatus` | `OPEN`、`CONFIRMED`、`DISMISSED`、`EXPIRED` | 白名单移除候选状态。候选只代表考勤建议，不直接执行 whitelist 移除。 |
| `AttendanceNotificationStatus` | `DELIVERED`、`FAILED`、`SKIPPED` | 最近一次通知摘要。 |
| `AttendanceAuditResult` | `SUCCESS`、`FAILED` | attendance 审计结果。 |

### 通用对象

#### AttendanceAccount

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `accountId` | string | 是 | 考勤账户 ID。 |
| `userId` | string | 是 | auth 用户 ID。 |
| `memberId` | string | 是 | profile 成员 ID。 |
| `displayNameSnapshot` | string | 是 | 成员展示名快照。 |
| `avatarUrlSnapshot` | string 或 null | 是 | 头像快照。 |
| `memberGroupSnapshot` | string 或 null | 是 | 成员组快照。 |
| `memberStatusSnapshot` | string | 是 | 成员状态快照。 |
| `minecraftBindingSnapshot` | object | 是 | Minecraft 绑定快照。 |
| `status` | string | 是 | `AttendanceAccountStatus`。 |
| `scoreBalance` | integer | 是 | 当前积分余额，最低为 `0`。 |
| `initialScore` | integer | 是 | 初始化积分，P0 默认 `100`。 |
| `totalEarned` | integer | 是 | 历史累计正向积分。 |
| `totalDeducted` | integer | 是 | 历史累计扣分绝对值。 |
| `lastPositiveActivityAt` | string 或 null | 是 | 最近一次正向贡献或活跃时间。 |
| `lastDeductedAt` | string 或 null | 是 | 最近一次扣分时间。 |
| `lastLedgerId` | string 或 null | 是 | 最近流水 ID。 |
| `whitelistApplicationId` | string | 是 | 来源白名单申请 ID。 |
| `whitelistHandoffId` | string | 是 | 来源 handoff ID。 |
| `whitelistHandoffVersion` | integer | 是 | 来源 handoff 版本。 |
| `reviewDirection` | string | 是 | 审核方向。 |
| `attemptType` | string | 是 | 首次入服或二次考核。 |
| `notificationStatus` | string 或 null | 是 | 最近一次通知状态。 |
| `notificationFailure` | AttendanceNotificationFailureSummary 或 null | 是 | 通知失败脱敏摘要。 |
| `profileSnapshotStale` | boolean | 是 | profile 降级时是否使用旧快照。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |

#### AttendanceLedgerEntry

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ledgerId` | string | 是 | 流水 ID。 |
| `accountId` | string | 是 | 考勤账户 ID。 |
| `memberId` | string | 是 | 成员 ID。 |
| `userId` | string | 是 | 用户 ID。 |
| `type` | string | 是 | `AttendanceLedgerType`。 |
| `status` | string | 是 | `AttendanceLedgerStatus`。 |
| `delta` | integer | 是 | 积分变化值，正数加分，负数扣分。 |
| `balanceBefore` | integer | 是 | 变更前余额。 |
| `balanceAfter` | integer | 是 | 变更后余额。 |
| `sourceModule` | string | 是 | 来源模块。P0 允许 `attendance`、`whitelist`、`manual`。 |
| `sourceId` | string | 是 | 来源对象 ID。 |
| `cycleKey` | string 或 null | 是 | 月度周期，例如 `2026-05`。 |
| `reason` | string | 是 | 原因，后台可见，1 到 500 位。 |
| `publicReason` | string | 是 | 对成员可见原因，1 到 200 位。 |
| `operatorUserId` | string 或 null | 是 | 操作者用户 ID。系统任务可为 `null`。 |
| `idempotencyKey` | string 或 null | 是 | 幂等键。 |
| `reversalOfLedgerId` | string 或 null | 是 | 撤销来源流水 ID。 |
| `reversedByLedgerId` | string 或 null | 是 | 被哪条反向流水撤销。 |
| `notificationStatus` | string 或 null | 是 | 通知状态。 |
| `notificationFailure` | AttendanceNotificationFailureSummary 或 null | 是 | 通知失败摘要。 |
| `createdAt` | string | 是 | 创建时间。 |
| `reversedAt` | string 或 null | 是 | 原流水被撤销时间。 |

#### ContributionRecord

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `contributionId` | string | 是 | 贡献记录 ID。 |
| `accountId` | string | 是 | 考勤账户 ID。 |
| `memberId` | string | 是 | 成员 ID。 |
| `userId` | string | 是 | 用户 ID。 |
| `type` | string | 是 | `ContributionType`。 |
| `sourceModule` | string | 是 | 来源模块。P0 后台写入时为 `attendance` 或 `manual`。 |
| `sourceId` | string | 是 | 来源对象 ID。 |
| `title` | string | 是 | 2 到 80 位。 |
| `description` | string | 否 | 最多 1000 位。 |
| `occurredAt` | string | 是 | 贡献发生时间。 |
| `scoreDelta` | integer | 是 | 该贡献对应积分变化，可为 `0`。 |
| `ledgerId` | string 或 null | 是 | 关联流水。 |
| `operatorUserId` | string | 是 | 录入或修正操作者。 |
| `correctionOfContributionId` | string 或 null | 是 | 修正来源贡献 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### MonthlyDeductionRun

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `runId` | string | 是 | 扣分运行 ID。 |
| `cycleKey` | string | 是 | 月度周期，格式 `YYYY-MM`。 |
| `status` | string | 是 | `MonthlyRunStatus`。 |
| `dryRun` | boolean | 是 | 是否只预检。 |
| `reason` | string | 是 | 后台原因。 |
| `deductionScore` | integer | 是 | 本周期扣分，P0 默认 `20`。 |
| `eligibleAccounts` | integer | 是 | 纳入检查账户数。 |
| `deductedAccounts` | integer | 是 | 实际扣分账户数。 |
| `skippedAccounts` | integer | 是 | 因有活跃或状态不符跳过账户数。 |
| `candidateCreated` | integer | 是 | 本次生成候选数。 |
| `previewItems` | MonthlyDeductionPreviewItem[] | 是 | 预览或执行结果摘要。 |
| `idempotencyKey` | string 或 null | 是 | 幂等键。 |
| `startedAt` | string 或 null | 是 | 开始时间。 |
| `completedAt` | string 或 null | 是 | 完成时间。 |
| `failureReason` | string 或 null | 是 | 脱敏失败摘要。 |
| `createdBy` | string | 是 | 操作者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |

#### RemovalCandidate

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `candidateId` | string | 是 | 候选 ID。 |
| `accountId` | string | 是 | 考勤账户 ID。 |
| `memberId` | string | 是 | 成员 ID。 |
| `userId` | string | 是 | 用户 ID。 |
| `displayNameSnapshot` | string | 是 | 成员展示名快照。 |
| `scoreBalance` | integer | 是 | 生成候选时余额。 |
| `cycleKey` | string | 是 | 触发周期。 |
| `status` | string | 是 | `RemovalCandidateStatus`。 |
| `reason` | string | 是 | 后台原因。 |
| `publicReason` | string | 是 | 对成员可见说明。 |
| `recommendedAction` | string | 是 | P0 固定为 `WHITELIST_REVIEW_REQUIRED`。 |
| `confirmedBy` | string 或 null | 是 | 确认操作者。 |
| `confirmedAt` | string 或 null | 是 | 确认时间。 |
| `dismissedBy` | string 或 null | 是 | 驳回操作者。 |
| `dismissedAt` | string 或 null | 是 | 驳回时间。 |
| `dismissReason` | string 或 null | 是 | 驳回原因。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### LeaderboardEntry

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `rank` | integer | 是 | 当前排名，从 `1` 开始。 |
| `accountId` | string | 是 | 考勤账户 ID。 |
| `memberId` | string | 是 | 成员 ID。 |
| `displayNameSnapshot` | string | 是 | 展示名快照。 |
| `avatarUrlSnapshot` | string 或 null | 是 | 头像快照。 |
| `memberGroupSnapshot` | string 或 null | 是 | 成员组快照。 |
| `scoreBalance` | integer | 是 | 当前余额。 |
| `totalEarned` | integer | 是 | 累计正向积分。 |
| `lastPositiveActivityAt` | string 或 null | 是 | 最近正向活跃。 |
| `profileSnapshotStale` | boolean | 是 | 是否使用旧快照。 |
| `generatedAt` | string | 是 | 榜单生成时间。 |

#### AttendanceNotificationFailureSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | 固定为 `FAILED`。 |
| `failureCode` | string | 是 | `48030`、`48031` 或 `48032`。 |
| `failureType` | string | 是 | `UNAVAILABLE`、`TIMEOUT` 或 `BAD_SCHEMA`。 |
| `failureReason` | string | 是 | 脱敏失败摘要。 |
| `failedAt` | string | 是 | 失败发生时间。 |

#### AttendanceAuditLog

审计字段继承公共契约，允许补充 `accountId`、`memberId`、`userId`、`ledgerId`、`contributionId`、`runId`、`candidateId`、`cycleKey`、`stateFrom`、`stateTo`、`delta`、`balanceBefore`、`balanceAfter`、`idempotencyKey`、`notificationStatus`、`dependencyStatus` 和 `profileSnapshotStale`。审计日志不得通过 attendance API 删除。

### 积分规则

P0 默认审核通过初始化为 `100` 分。月度无上线活跃、无工程贡献、无活动参与、无作品投稿、无协助管理记录时扣 `20` 分。积分最低为 `0`。积分小于等于 `0` 后，账户进入 `REMOVAL_CANDIDATE`，生成白名单移除候选和通知摘要。

所有积分变化必须有流水。任何直接改余额但没有流水的实现都不合格。管理员调整必须要求 `reason` 和 `publicReason`。撤销流水必须生成反向流水，不能直接删除原流水。月度扣分必须按 `cycleKey` 幂等，不能重复扣同一个账户同一个周期。

P0 的正向贡献可以由后台受控写入。未来接入 `activity`、`community`、`content`、`server-status` 或真实在线事件时，必须作为兼容变更补充契约、测试和适配器，不能绕过流水。

### attendance 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `45000` | 404 | 考勤账户不存在，或当前用户无权访问。 |
| `45001` | 404 | 积分流水不存在。 |
| `45002` | 404 | 贡献记录不存在。 |
| `45003` | 404 | 月度扣分运行不存在。 |
| `45004` | 404 | 白名单移除候选不存在。 |
| `45010` | 409 | whitelist 交接状态不允许初始化。 |
| `45011` | 409 | whitelist 交接快照已被消费。 |
| `45012` | 409 | 当前成员已存在考勤账户。 |
| `45013` | 409 | 当前账户状态不允许该操作。 |
| `45014` | 409 | 积分调整会造成非法余额。 |
| `45015` | 409 | 积分流水状态不允许撤销。 |
| `45016` | 409 | 月度周期已执行，不能重复扣分。 |
| `45017` | 409 | attendance 幂等键请求指纹冲突。 |
| `45018` | 409 | 候选状态不允许确认或驳回。 |
| `45019` | 409 | 贡献记录来源冲突或重复。 |
| `48000` | 502 | auth 认证上下文不可用。 |
| `48001` | 504 | auth 认证上下文调用超时。 |
| `48002` | 502 | auth 认证上下文字段或枚举不兼容 attendance 契约。 |
| `48010` | 502 | whitelist attendance 交接快照不可用。 |
| `48011` | 504 | whitelist attendance 交接快照调用超时。 |
| `48012` | 502 | whitelist attendance 交接快照字段不兼容 attendance 契约。 |
| `48020` | 502 | profile 成员校验或快照刷新不可用。 |
| `48021` | 504 | profile 成员校验或快照刷新超时。 |
| `48022` | 502 | profile 响应字段不兼容 attendance 契约。 |
| `48030` | 502 | notification 投递不可用。 |
| `48031` | 504 | notification 投递超时。 |
| `48032` | 502 | notification 投递响应不兼容 attendance 契约。 |
| `53000` | 500 | attendance 内部错误。 |
| `53001` | 500 | attendance 审计写入失败。 |
| `53002` | 500 | attendance 状态写入失败。 |
| `53003` | 500 | attendance 流水和余额写入不一致。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误和通用服务端错误优先使用公共错误码。attendance 自有幂等指纹冲突使用 `45017`。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开榜单 | GET | `/api/v1/attendance/leaderboard` | 否 | 游客 | LOW |
| 我的考勤账户 | GET | `/api/v1/attendance/me/account` | 是 | 当前用户 | LOW |
| 我的积分流水 | GET | `/api/v1/attendance/me/ledger` | 是 | 当前用户 | LOW |
| 我的贡献记录 | GET | `/api/v1/attendance/me/contributions` | 是 | 当前用户 | LOW |
| 我的榜单位置 | GET | `/api/v1/attendance/me/ranking` | 是 | 当前用户 | LOW |
| 后台账户列表 | GET | `/api/v1/attendance/admin/accounts` | 是 | HELPER、ADMIN、OWNER | LOW |
| 后台账户详情 | GET | `/api/v1/attendance/admin/accounts/{accountId}` | 是 | HELPER、ADMIN、OWNER | LOW |
| 消费 whitelist 初始化交接 | POST | `/api/v1/attendance/admin/initializations` | 是 | ADMIN、OWNER | MEDIUM |
| 管理员积分调整 | POST | `/api/v1/attendance/admin/accounts/{accountId}/adjustments` | 是 | ADMIN、OWNER | MEDIUM |
| 撤销积分流水 | POST | `/api/v1/attendance/admin/ledger/{ledgerId}/reverse` | 是 | ADMIN、OWNER | MEDIUM |
| 创建贡献记录 | POST | `/api/v1/attendance/admin/contributions` | 是 | ADMIN、OWNER | MEDIUM |
| 修正贡献记录 | PATCH | `/api/v1/attendance/admin/contributions/{contributionId}` | 是 | ADMIN、OWNER | MEDIUM |
| 月度扣分预检 | POST | `/api/v1/attendance/admin/monthly-runs/preview` | 是 | ADMIN、OWNER | MEDIUM |
| 执行月度扣分 | POST | `/api/v1/attendance/admin/monthly-runs` | 是 | ADMIN、OWNER | HIGH |
| 月度扣分运行详情 | GET | `/api/v1/attendance/admin/monthly-runs/{runId}` | 是 | ADMIN、OWNER | LOW |
| 白名单移除候选列表 | GET | `/api/v1/attendance/admin/removal-candidates` | 是 | HELPER、ADMIN、OWNER | LOW |
| 确认移除候选 | PATCH | `/api/v1/attendance/admin/removal-candidates/{candidateId}/confirm` | 是 | ADMIN、OWNER | HIGH |
| 驳回移除候选 | PATCH | `/api/v1/attendance/admin/removal-candidates/{candidateId}/dismiss` | 是 | ADMIN、OWNER | MEDIUM |
| 榜单重算 | POST | `/api/v1/attendance/admin/leaderboard/rebuild` | 是 | ADMIN、OWNER | MEDIUM |
| attendance 审计列表 | GET | `/api/v1/attendance/admin/audit-logs` | 是 | ADMIN、OWNER | LOW |
| attendance 自检摘要 | GET | `/api/v1/attendance/admin/ops/summary` | 是 | HELPER、ADMIN、OWNER | LOW |

### 公开和当前用户接口

#### 公开榜单

`GET /api/v1/attendance/leaderboard`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `cycleKey` | string | 否 | 格式 `YYYY-MM`，为空时返回当前榜单。 |
| `memberGroup` | string | 否 | 成员组快照，最多 80 位。 |
| `sort` | string | 否 | 允许 `score_desc`、`earned_desc`、`lastActivity_desc`。默认 `score_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `LeaderboardEntry[]`。

业务规则：只展示 `ACTIVE` 和 `REMOVAL_CANDIDATE` 账户的公开字段。不得返回扣分原因全文、内部备注、审计参数、通知失败详情、管理员 ID 或候选确认状态。profile 不可用时可使用已保存快照降级，并标记 `profileSnapshotStale=true`。

#### 我的考勤账户

`GET /api/v1/attendance/me/account`

成功响应 HTTP `200`，`data` 为当前用户 `AttendanceAccount`。当前用户尚未初始化时返回 `data=null`，不得自动创建账户。

业务规则：只能读取认证用户自己的账户。响应不得包含后台内部原因、候选确认备注、通知正文、完整 profile 失败详情或审计参数。

#### 我的积分流水

`GET /api/v1/attendance/me/ledger`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `type` | string | 否 | 任一 `AttendanceLedgerType`。 |
| `cycleKey` | string | 否 | 格式 `YYYY-MM`。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`。 |

成功响应 HTTP `200`，分页 `items` 为当前用户可见的 `AttendanceLedgerEntry[]`。

业务规则：只返回当前用户流水。成员视图使用 `publicReason`，不得返回后台 `reason` 全文、操作者内部备注或通知失败详情。

#### 我的贡献记录

`GET /api/v1/attendance/me/contributions`

查询参数同积分流水，额外支持 `type` 为 `ContributionType`。

成功响应 HTTP `200`，分页 `items` 为当前用户可见的 `ContributionRecord[]`。

业务规则：只返回当前用户贡献。后台修正历史可以显示为被修正摘要，但不得泄露内部备注。

#### 我的榜单位置

`GET /api/v1/attendance/me/ranking`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "rank": 3,
    "totalRanked": 28,
    "entry": {
      "rank": 3,
      "accountId": "att_acc_001",
      "memberId": "mem_001",
      "displayNameSnapshot": "BeiMingPlayer",
      "avatarUrlSnapshot": null,
      "memberGroupSnapshot": "default",
      "scoreBalance": 120,
      "totalEarned": 140,
      "lastPositiveActivityAt": "2026-05-23T08:00:00Z",
      "profileSnapshotStale": false,
      "generatedAt": "2026-05-23T08:30:00Z"
    }
  }
}
```

业务规则：当前用户没有考勤账户时返回 `data=null`。

### 后台接口

#### 后台账户列表

`GET /api/v1/attendance/admin/accounts`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配账户 ID、用户 ID、成员 ID、展示名或 Minecraft ID，最多 80 位。 |
| `status` | string | 否 | 任一 `AttendanceAccountStatus`。 |
| `reviewDirection` | string | 否 | 任一审核方向。 |
| `attemptType` | string | 否 | `FIRST_TIME` 或 `RECHECK`。 |
| `minScore` | integer | 否 | 最低余额，不能小于 `0`。 |
| `maxScore` | integer | 否 | 最高余额，不能小于 `minScore`。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`updatedAt_desc`、`score_desc`、`score_asc`、`lastActivity_desc`。 |

成功响应 HTTP `200`，分页 `items` 为后台视图 `AttendanceAccount[]`。

#### 后台账户详情

`GET /api/v1/attendance/admin/accounts/{accountId}`

成功响应 HTTP `200`，`data` 包含 `AttendanceAccount`、最近流水、最近贡献、打开的候选和依赖摘要。账户不存在返回 `45000`。响应不得返回 token、完整请求头、通知正文、profile 内部堆栈、真实服务器命令或节点凭据。

#### 消费 whitelist 初始化交接

`POST /api/v1/attendance/admin/initializations`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `applicationId` | string | 条件必填 | 白名单申请 ID。`applicationId` 和 `handoffId` 至少传一个。 |
| `handoffId` | string | 条件必填 | whitelist handoff ID，用于幂等校验。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `201`，`data` 为 `WhitelistAttendanceInitialization`，包含创建的 `AttendanceAccount` 和 `INITIAL_GRANT` 流水。重复消费同一 handoff 成功响应 HTTP `200`，返回同一初始化结果。

业务规则：初始化接口不能接受前端传入 `memberId`、`userId`、`scoreSummary`、`approvedAt` 等可信字段作为最终依据。必须从 whitelist handoff 读取，并用 profile 校验成员状态。初始化成功后创建账户、写入初始积分流水、记录 handoff 消费、尝试通知。审计失败时不得创建账户。通知失败不回滚主状态，但必须记录摘要。

#### 管理员积分调整

`POST /api/v1/attendance/admin/accounts/{accountId}/adjustments`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `delta` | integer | 是 | 不能为 `0`，范围 `-1000` 到 `1000`。 |
| `publicReason` | string | 是 | 1 到 200 位，成员可见。 |
| `reason` | string | 是 | 1 到 500 位，后台原因。 |
| `sourceId` | string | 否 | 外部或手工来源 ID，默认生成。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为新 `AttendanceLedgerEntry` 和更新后的 `AttendanceAccount`。

业务规则：只允许 `ACTIVE`、`FROZEN` 和 `REMOVAL_CANDIDATE` 账户调整。扣分后最低余额为 `0`。余额变为 `0` 时必须生成或保持打开的移除候选。审计失败、流水写入失败或余额写入失败不得假装成功。

#### 撤销积分流水

`POST /api/v1/attendance/admin/ledger/{ledgerId}/reverse`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `publicReason` | string | 是 | 1 到 200 位。 |
| `reason` | string | 是 | 1 到 500 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为反向 `REVERSAL` 流水和更新后的 `AttendanceAccount`。

业务规则：只允许撤销 `POSTED` 且未被撤销的流水。`INITIAL_GRANT` 是否允许撤销由实现固定，P0 推荐禁止并返回 `45015`。撤销不会删除原流水，原流水标记 `REVERSED` 并关联反向流水。

#### 创建贡献记录

`POST /api/v1/attendance/admin/contributions`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `accountId` | string | 是 | 考勤账户 ID。 |
| `type` | string | 是 | 任一 `ContributionType`。 |
| `sourceModule` | string | 否 | P0 允许 `attendance`、`manual`。 |
| `sourceId` | string | 否 | 来源 ID，缺省生成。 |
| `title` | string | 是 | 2 到 80 位。 |
| `description` | string | 否 | 最多 1000 位。 |
| `occurredAt` | string | 是 | ISO 8601，不能晚于当前时间后 5 分钟。 |
| `scoreDelta` | integer | 是 | 范围 `0` 到 `1000`。为 `0` 时只记录贡献，不加分。 |
| `publicReason` | string | 否 | 加分时必填，1 到 200 位。 |
| `reason` | string | 是 | 1 到 500 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `201`，`data` 为 `ContributionRecord`，加分时同时返回关联流水。

业务规则：同一 `sourceModule`、`sourceId`、`accountId` 不能重复创建正向贡献，重复请求用幂等返回同一结果。未来其他模块接入前不得接受任意来源绕过后台权限。

#### 修正贡献记录

`PATCH /api/v1/attendance/admin/contributions/{contributionId}`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `title` | string | 否 | 2 到 80 位。 |
| `description` | string | 否 | 最多 1000 位。 |
| `occurredAt` | string | 否 | ISO 8601。 |
| `publicReason` | string | 是 | 1 到 200 位。 |
| `reason` | string | 是 | 1 到 500 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为修正后的 `ContributionRecord`。修正不直接改历史积分，如需改分必须通过流水撤销和新调整完成。

#### 月度扣分预检

`POST /api/v1/attendance/admin/monthly-runs/preview`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `cycleKey` | string | 是 | 格式 `YYYY-MM`。 |
| `deductionScore` | integer | 否 | 默认 `20`，范围 `1` 到 `100`。 |
| `reason` | string | 是 | 1 到 500 位。 |

成功响应 HTTP `200`，`data` 为 `MonthlyDeductionRun`，`dryRun=true`。预检只返回影响范围，不写积分流水，不改变账户状态，不生成候选。

#### 执行月度扣分

`POST /api/v1/attendance/admin/monthly-runs`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `cycleKey` | string | 是 | 格式 `YYYY-MM`。 |
| `deductionScore` | integer | 否 | 默认 `20`，范围 `1` 到 `100`。 |
| `reason` | string | 是 | 1 到 500 位。 |
| `confirmText` | string | 是 | 二次确认文本，P0 固定要求 `RUN_MONTHLY_DEDUCTION`。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `201`，`data` 为 `MonthlyDeductionRun`。重复执行同一 `cycleKey` 返回 `45016`，同一幂等键同一请求体重复提交返回同一运行结果。

业务规则：该接口是 `HIGH` 风险。只扣 `ACTIVE` 或 `REMOVAL_CANDIDATE` 账户；当周期内无正向贡献、活动、作品或协助记录时扣分。扣分后余额小于等于 `0` 时生成或保持打开的移除候选。候选只代表建议，不调用 whitelist 移除接口，不执行服务器命令。

#### 月度扣分运行详情

`GET /api/v1/attendance/admin/monthly-runs/{runId}`

成功响应 HTTP `200`，`data` 为 `MonthlyDeductionRun`。不存在返回 `45003`。

#### 白名单移除候选列表

`GET /api/v1/attendance/admin/removal-candidates`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `status` | string | 否 | 任一 `RemovalCandidateStatus`。 |
| `cycleKey` | string | 否 | 格式 `YYYY-MM`。 |
| `keyword` | string | 否 | 匹配候选 ID、账户 ID、成员 ID、展示名。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`updatedAt_desc`、`score_asc`。 |

成功响应 HTTP `200`，分页 `items` 为 `RemovalCandidate[]`。

#### 确认移除候选

`PATCH /api/v1/attendance/admin/removal-candidates/{candidateId}/confirm`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `publicReason` | string | 是 | 1 到 200 位。 |
| `reason` | string | 是 | 1 到 500 位。 |
| `confirmText` | string | 是 | 二次确认文本，P0 固定要求 `CONFIRM_REMOVAL_CANDIDATE`。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `RemovalCandidate` 和 `AttendanceAccount`。

业务规则：该接口是 `HIGH` 风险。确认后候选进入 `CONFIRMED`，账户保持或进入 `REMOVAL_CANDIDATE`，`recommendedAction=WHITELIST_REVIEW_REQUIRED`。P0 不调用 whitelist 移除接口，不执行服务器命令。

#### 驳回移除候选

`PATCH /api/v1/attendance/admin/removal-candidates/{candidateId}/dismiss`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `publicReason` | string | 是 | 1 到 200 位。 |
| `reason` | string | 是 | 1 到 500 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `RemovalCandidate` 和 `AttendanceAccount`。

业务规则：只允许 `OPEN` 候选驳回。驳回后候选进入 `DISMISSED`。若账户余额仍为 `0`，账户可以保持 `REMOVAL_CANDIDATE`，但不能自动移除白名单。

#### 榜单重算

`POST /api/v1/attendance/admin/leaderboard/rebuild`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `cycleKey` | string | 否 | 格式 `YYYY-MM`，为空表示当前榜单。 |
| `reason` | string | 是 | 1 到 500 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 包含 `rebuiltAt`、`entriesTotal` 和前 `20` 条 `LeaderboardEntry`。

#### attendance 审计列表

`GET /api/v1/attendance/admin/audit-logs`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `accountId` | string | 否 | 考勤账户 ID。 |
| `memberId` | string | 否 | 成员 ID。 |
| `actorUserId` | string | 否 | 操作者用户 ID。 |
| `action` | string | 否 | 动作，最多 80 位。 |
| `result` | string | 否 | `SUCCESS` 或 `FAILED`。 |
| `from` | string | 否 | ISO 8601 起始时间。 |
| `to` | string | 否 | ISO 8601 结束时间。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`。默认 `createdAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `AttendanceAuditLog[]`。审计日志不得通过 attendance API 删除，返回结果必须脱敏。

#### attendance 自检摘要

`GET /api/v1/attendance/admin/ops/summary`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "attendance",
    "port": 8131,
    "legacyPort": 8111,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "whitelistMode": "TEST_STUB",
    "profileMode": "TEST_STUB",
    "notificationMode": "TEST_STUB",
    "testControlsEnabled": false,
    "accountsTotal": 10,
    "activeAccountsTotal": 8,
    "removalCandidatesOpenTotal": 1,
    "monthlyRunsTotal": 2,
    "ledgerEntriesTotal": 30,
    "contributionsTotal": 12,
    "auditsTotal": 80,
    "idempotencyRecordsTotal": 18,
    "lastMonthlyRunAt": "2026-05-23T12:00:00Z",
    "lastAuditAt": "2026-05-23T12:05:00Z",
    "productionGaps": [
      "P0_IN_MEMORY_STORAGE",
      "P0_AUTH_STUB",
      "P0_WHITELIST_STUB",
      "P0_PROFILE_STUB",
      "P0_NOTIFICATION_STUB",
      "TEST_CONTROLS_DISABLED_OUTSIDE_TEST",
      "REAL_ACTIVITY_EVENTS_NOT_CONNECTED",
      "REAL_ONLINE_TIME_NOT_CONNECTED",
      "WHITELIST_REMOVAL_NOT_CONNECTED"
    ]
  }
}
```

业务规则：自检摘要用于后台确认 attendance 当前运行模式、账户规模、扣分任务状态、移除候选数量、测试控制头开关和生产化缺口。摘要不得返回 token、请求头、通知正文、审计参数全文、真实服务器命令、节点凭据或异常堆栈。

### 状态、幂等和并发

考勤账户初始化成功后进入 `ACTIVE`。管理员可以把账户冻结为 `FROZEN` 或未来兼容变更归档，但 P0 不提供冻结写接口。扣分后余额为 `0` 时进入 `REMOVAL_CANDIDATE`。确认候选不直接进入 `REMOVED`，只有未来 whitelist 正式移除回传或兼容变更才允许进入 `REMOVED`。`ARCHIVED` 为终态。

状态推进只能由服务端根据 whitelist handoff、profile 校验、积分流水、月度扣分、候选动作、权限和二次确认判断。非法状态跳跃返回 `45013`。浏览器传入可信字段必须忽略或返回字段校验失败。

初始化、积分调整、流水撤销、贡献创建、贡献修正、月度扣分执行、候选确认、候选驳回和榜单重算支持 `idempotencyKey`。同一操作者、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `45017`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序。

并发初始化同一 handoff 只能产生一个账户和一条初始流水。并发调整同一账户必须串行化余额计算，不能出现流水余额断裂。并发月度扣分同一 `cycleKey` 只能有一个成功运行。读取接口允许读到更新前或更新后的完整状态，不能返回半更新对象。

### 审计要求

必须审计的动作包括初始化考勤账户、初始化 handoff 重放、初始化失败、管理员积分调整、流水撤销、贡献记录创建、贡献记录修正、月度扣分预检、月度扣分执行、扣分任务部分失败、候选生成、候选确认、候选驳回、榜单重算、通知失败、依赖降级、自检读取、审计写入失败和状态写入失败。

后台写操作必须记录 `reason`、操作者、目标对象、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计不得泄露 token、完整请求头、Minecraft 验证凭据、profile 后台备注全文、通知正文全文、真实服务器命令、节点凭据、内部异常堆栈或前序服务内部路径。

审计写入失败时，初始化、积分调整、流水撤销、贡献创建、贡献修正、月度扣分、候选确认、候选驳回和榜单重算不得假装成功，必须返回 `53001` 或 `53000`，并保持业务数据不变。

### 失败降级

auth 是所有登录接口强依赖。auth 不可用、超时、用户状态不允许或字段不兼容时不得伪造成功。

whitelist 是初始化强依赖。交接快照不可用、未通过、已消费、字段不兼容、用户或成员缺失时不得创建考勤账户。

profile 是初始化强依赖，也是只读展示的辅助刷新依赖。初始化时 profile 失败不得创建账户。榜单和详情读取时 profile 刷新失败可以使用已有快照降级，但必须标记 `profileSnapshotStale=true` 并写入依赖降级审计或计数。

notification 是辅助依赖。通知失败不得回滚初始化、积分调整、流水撤销、月度扣分、候选确认或候选驳回，但必须记录失败摘要和审计。

流水写入和余额写入必须保持一致。任何半成功风险都必须返回 `53003` 或进入可复核失败状态，不能出现余额变化但没有流水，或流水成功但余额未更新却返回成功。

### 验收口径

`attendance` API 文档按 `docs/contracts-attendance.md` 独立存在，并由 `.local-docs/tests-attendance.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`attendance` 完成时必须满足以下条件：全部接口按本文档实现；当前用户接口只能访问自己的账户、流水、贡献和排名；后台接口按角色限制；初始化只通过 whitelist handoff 和 profile 正式适配读取快照，不直接读前序服务实现；所有积分变化都有流水；月度扣分按 `cycleKey` 幂等；移除候选只生成建议，不执行真实 whitelist 移除或服务器命令；通知失败按辅助降级记录；当前运行入口为 `admission-core-service:8131`，历史端口只作为 `legacyPort=8111` 返回；`.local-docs/tests-attendance.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 attendance 全部测试通过；auth、profile、notification、content、server-status、resource、admin、onboarding、exam 和 whitelist 前序服务回归测试通过；没有修改前序服务稳定接口；没有把社区、活动、日历、更新日志、后台聚合、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复或 Cloudreve 管理能力塞进 attendance。

生产化硬化验收还必须满足：测试控制头默认关闭，只有本地自动化测试显式启用时才生效；关闭状态下依赖失败模拟头、写入失败模拟头和通知失败模拟头全部被忽略；自检摘要明确返回当前测试控制头开关状态。

## 北冥官网 community API 契约

来源：`docs/contracts-community.md`

版本：0.2

### 版本记录

`0.2` 补充 P1 硬化验收：公开浏览计数必须按服务端访问指纹去重；投票开放时间、关闭时间和可投资格必须生效；举报证据链接必须校验协议，举报处理必须保存关联处罚；工单必须保存并返回站内安全附件摘要和关联对象摘要；高风险处罚解除必须纳入审计失败回滚；工单后台状态推进必须遵守固定状态机。

### 文档定位

本文档是 `community` 微服务的正式 API 契约。后续 `activity`、`calendar`、`changelog`、前端适配、`admin` 聚合、`ops-control` 和 `external-node-executor` 只能通过本文档定义的接口读取论坛、帖子、评论、互动、投票、举报、工单、处罚、审计和自检摘要，不能直接读取或修改 `community` 数据库，也不能把社区治理逻辑塞进其他服务。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `community` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了现有高活跃社区的成熟做法。Reddit 的社区、版主管理、举报和 Mod Queue 说明举报处理要有独立队列和处理状态，不能只把举报挂在帖子上。Discourse 的分类、标签、信任等级和 flag 机制说明论坛需要可治理的版块、标签、用户可信度和可追溯审核。GitHub Discussions 的分类、问答标记、评论和投票说明讨论型社区可以同时承载建议、问答、公告讨论和轻量投票。Stack Overflow 的声望、权限和 flag 说明互动权重、举报和处罚要基于服务端可信状态，不能靠前端显示决定。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Reddit Help: Mod Queue](https://support.reddithelp.com/hc/en-us/articles/15484440494356-Moderation-Queue) | 举报和审核应进入独立处理队列，有明确处理结果和处理人。 |
| [Discourse Meta: Trust Levels](https://meta.discourse.org/t/understanding-discourse-trust-levels/90752) | 社区写权限、互动频率和治理能力要随可信度分层，不能只看登录态。 |
| [Discourse Meta: Flagging](https://meta.discourse.org/t/discourse-moderation-guide/63116) | 用户举报、版主审核和处罚要保留原因、证据和状态。 |
| [GitHub Docs: About Discussions](https://docs.github.com/en/discussions/collaborating-with-your-community-using-discussions/about-discussions) | 讨论区可按分类组织，支持问答、公告讨论、评论和投票反馈。 |
| [Stack Overflow Help: Reputation](https://stackoverflow.com/help/whats-reputation) | 互动权重和可信能力应来自可解释行为，不能由人工随意变更。 |
| [Stack Overflow Help: Privileges](https://stackoverflow.com/help/privileges) | 社区治理能力需要按权限分层解锁，后台处罚仍必须走角色和审计。 |

### 职责边界

`community` 负责论坛板块、帖子、评论、点赞、收藏、轻量投票、举报、工单、处罚、社区审计、社区自检和自身幂等记录。

`community` 不负责注册、登录、邀请码、会话、账号角色能力点、Minecraft 绑定主数据、成员档案主数据、站内通知主数据、官网公告主发布、资源下载主数据、考勤积分主数据、白名单审核主流程、考试判分、服务器状态采集、后台聚合入口、真实服务器运维控制、节点守护进程、容器、终端、文件管理、备份恢复或 Cloudreve 管理。

`community` 只能后序适配前序服务。它通过 `auth` 认证上下文读取当前用户、角色、能力点和用户状态；通过 `profile` 的正式接口或未来服务间适配器读取成员展示快照；通过 `notification` 投递帖子审核、举报处理、工单回复和处罚通知；通过 `content` 与 `resource` 的公开快照关联公告、专题或资源讨论；通过 `attendance` 未来正式贡献入口或兼容变更产生社区贡献候选。`community` 不能导入前序服务内存存储、实体、Repository、测试种子或内部类，不能要求前序服务为了社区反向改稳定接口。

### 数据归属

`community` 拥有以下主数据：社区板块、板块权限规则、帖子、帖子版本、评论、评论版本、帖子互动、评论互动、收藏、轻量投票、投票选项、投票记录、举报、举报证据、工单、工单消息、处罚记录、处罚解除记录、幂等记录、依赖调用摘要、社区审计日志和自检统计。

`community` 可以保存来自 `auth` 的 `userId`、`displayName`、`roles`、`permissions` 和用户状态快照；可以保存来自 `profile` 的成员展示名、头像、成员组、成员状态和 Minecraft ID 快照；可以保存来自 `content` 的内容 ID、标题、slug 和公开状态快照；可以保存来自 `resource` 的资源 ID、标题、slug、版本和公开状态快照；可以保存来自 `notification` 的投递结果摘要；可以保存 future `attendance` 的贡献接收摘要。快照只服务展示、检索和审计，不能成为来源模块主数据，也不能反写来源模块。

### 基础路径与认证

所有接口默认使用 `/api/v1/community` 前缀。当前运行入口为 `engagement-core-service:8132`。历史原服务端口 `8112` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或回归测试命令。

公开接口允许游客读取已公开、已通过、未下架、未软删除的数据。公开接口不得返回内部备注、举报详情、处罚证据、工单内容、审核参数、管理员 ID、通知失败详情、完整依赖错误或审计参数。

当前用户接口使用 `/api/v1/community/me` 前缀，全部要求 `Authorization: Bearer <token>`。当前用户只能创建和维护自己的帖子草稿、需修改帖子、评论、互动、收藏、投票、举报和工单。浏览器请求体不得传入 `userId`、`memberId`、`roles`、`permissions`、`authorSnapshot`、`moderatorUserId`、`status`、`reviewStatus`、`voteCount`、`likeCount`、`favoriteCount`、`reportStatus`、`ticketAssigneeId`、`penaltyStatus`、`notificationStatus`、`sourceModule`、`auditResult` 等服务端可信字段。

后台接口使用 `/api/v1/community/admin` 前缀，全部要求登录。后台读取板块、帖子、评论、举报、工单、处罚、审计和自检摘要要求 `HELPER`、`ADMIN` 或 `OWNER`。板块写入、帖子审核、评论审核、举报处理、工单分配和回复要求 `HELPER`、`ADMIN` 或 `OWNER`，但 `HELPER` 只能处理被授权的初审、回复和协助事项。处罚创建、处罚修正、处罚解除、投票管理、强制下架、归档、软删除和系统配置要求 `ADMIN` 或 `OWNER`。高风险处罚、批量状态变更和跨模块贡献接入在 P1 不开放。

### 本地测试控制头

`community` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Profile-Mode`、`X-Test-Notification-Mode`、`X-Test-Content-Mode`、`X-Test-Resource-Mode`、`X-Test-Attendance-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Fail-Reaction` 模拟依赖失败、通知失败、写入失败和互动并发冲突。该能力只服务本地测试闭环，不属于正式业务 API。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、状态失败、互动失败、通知失败或快照 stale。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 视为已满足的生产化硬化项。

### 前序服务兼容契约

`auth` 是所有登录接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions`、`status` 和可选 `minecraftBinding`。用户状态为 `ACTIVE` 时可参与社区；`PENDING_PROFILE` 可以创建工单和查看自己的举报进度，但不能发布公开帖子；`DISABLED`、`BANNED`、`DELETED` 不允许写入。auth 不可用返回 `49200`，auth 超时返回 `49201`，字段或枚举不兼容返回 `49202`。

`profile` 是作者展示快照的强依赖。发帖、评论、工单和后台处罚创建时必须读取成员展示快照。profile 不可用、超时或字段不兼容时，发帖和评论不得伪造成功，分别返回 `49210`、`49211` 或 `49212`。只读公开列表可以使用已有快照降级，但必须返回 `profileSnapshotStale=true` 或依赖摘要，且不得刷新为伪造资料。

`notification` 是辅助依赖。帖子审核结果、评论审核结果、举报处理、工单回复、工单关闭、处罚生效和处罚解除可以触发通知。通知失败不得回滚社区主状态，但必须保存 `notificationStatus=FAILED`、失败原因摘要并写入审计。notification 不可用记录或返回 `49220`，超时记录或返回 `49221`，字段不兼容记录或返回 `49222`。通知失败摘要不得包含通知正文、完整请求头、认证 token、内部 URL 或异常堆栈。

`content` 是关联公告、专题和内容讨论的辅助依赖。帖子引用 `content` 对象时只能保存公开快照，不能读取 content 内部存储。content 不可用时，发帖引用校验返回 `49230`；公开读取可以展示已保存快照并标记 `linkedContentSnapshotStale=true`。

`resource` 是关联资源讨论的辅助依赖。帖子引用 `resource` 对象时只能保存公开快照，不能读取 resource 内部存储，不能生成下载票据。resource 不可用时，发帖引用校验返回 `49240`；公开读取可以展示已保存快照并标记 `linkedResourceSnapshotStale=true`。

`attendance` 当前只作为未来贡献入口。P1 中 `community` 只能记录 `communityContributionCandidate`，不得直接写 attendance 积分余额、流水或榜单。后续需要贡献积分时，必须作为 `attendance` 兼容变更先补充契约、测试和回归，再由 `community` 通过正式接口适配。attendance 不可用不得影响帖子、评论、举报和工单主流程，只能记录 `attendanceSyncStatus=SKIPPED` 或 `FAILED`。

`admin` 当前只是聚合入口。本文档不修改 `admin` 契约。若后续需要 admin 展示 community 待办、指标或审计索引，必须作为 admin 兼容变更单独走契约、测试、红灯、实现和回归闭环。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `CommunityBoardStatus` | `DRAFT`、`ACTIVE`、`LOCKED`、`ARCHIVED` | 板块状态。`ACTIVE` 可公开读取和发帖，`LOCKED` 只读，`ARCHIVED` 不接受新内容。 |
| `CommunityBoardVisibility` | `PUBLIC`、`MEMBER_ONLY`、`STAFF_ONLY` | 板块可见范围。 |
| `CommunityPostType` | `DISCUSSION`、`QUESTION`、`GUIDE`、`SHOWCASE`、`SUGGESTION`、`ANNOUNCEMENT_DISCUSSION`、`RESOURCE_DISCUSSION` | 帖子类型。公告和资源讨论只保存关联快照，不吞 content 或 resource 主数据。 |
| `CommunityPostStatus` | `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`NEEDS_CHANGES`、`REJECTED`、`LOCKED`、`OFFLINE`、`ARCHIVED`、`DELETED` | 帖子状态。公开列表只展示 `APPROVED` 和未下架内容。 |
| `CommunityCommentStatus` | `PENDING_REVIEW`、`APPROVED`、`NEEDS_CHANGES`、`REJECTED`、`OFFLINE`、`ARCHIVED`、`DELETED` | 评论状态。 |
| `CommunityReactionTargetType` | `POST`、`COMMENT` | 互动目标。 |
| `CommunityVoteStatus` | `DRAFT`、`OPEN`、`CLOSED`、`ARCHIVED` | 轻量投票状态。 |
| `CommunityReportTargetType` | `POST`、`COMMENT`、`USER`、`RESOURCE_SNAPSHOT`、`GAME_BEHAVIOR` | 举报目标。 |
| `CommunityReportReason` | `SPAM`、`HARASSMENT`、`INAPPROPRIATE`、`COPYRIGHT`、`IMPERSONATION`、`GAME_VIOLATION`、`OTHER` | 举报原因。 |
| `CommunityReportStatus` | `OPEN`、`UNDER_REVIEW`、`RESOLVED`、`DISMISSED`、`ESCALATED`、`ARCHIVED` | 举报处理状态。 |
| `CommunityTicketType` | `BAN_APPEAL`、`WHITELIST_ISSUE`、`ACCOUNT_ISSUE`、`RESOURCE_ISSUE`、`BUG_REPORT`、`CONTENT_DISPUTE`、`OTHER` | 工单类型。 |
| `CommunityTicketStatus` | `OPEN`、`WAITING_STAFF`、`WAITING_USER`、`RESOLVED`、`CLOSED`、`ARCHIVED` | 工单状态。 |
| `CommunityTicketMessageType` | `USER_REPLY`、`STAFF_REPLY`、`INTERNAL_NOTE`、`SYSTEM_EVENT` | 工单消息类型。 |
| `CommunityPenaltyType` | `WARNING`、`MUTE`、`BAN`、`WHITELIST_REVIEW_REQUIRED`、`POST_RESTRICTED`、`SUBMISSION_RESTRICTED` | 处罚类型。`WHITELIST_REVIEW_REQUIRED` 只生成社区治理建议，不执行白名单移除。 |
| `CommunityPenaltyStatus` | `ACTIVE`、`EXPIRED`、`REVOKED`、`ARCHIVED` | 处罚状态。 |
| `CommunityAuditResult` | `SUCCESS`、`FAILED` | community 审计结果。 |
| `CommunityNotificationStatus` | `DELIVERED`、`FAILED`、`SKIPPED` | 最近一次通知摘要。 |

### 通用对象

#### CommunityAuthorSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | string | 是 | auth 用户 ID。 |
| `memberId` | string 或 null | 是 | profile 成员 ID。 |
| `displayNameSnapshot` | string | 是 | 展示名快照。 |
| `avatarUrlSnapshot` | string 或 null | 是 | 头像快照。 |
| `memberGroupSnapshot` | string 或 null | 是 | 成员组快照。 |
| `memberStatusSnapshot` | string 或 null | 是 | 成员状态快照。 |
| `minecraftIdSnapshot` | string 或 null | 是 | Minecraft ID 快照。 |
| `profileSnapshotStale` | boolean | 是 | 是否使用旧快照。 |

#### CommunityBoard

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `boardId` | string | 是 | 板块 ID。 |
| `slug` | string | 是 | 板块 slug，2 到 60 位，小写字母、数字和短横线。 |
| `name` | string | 是 | 2 到 40 位。 |
| `description` | string | 是 | 1 到 300 位。 |
| `visibility` | string | 是 | `CommunityBoardVisibility`。 |
| `status` | string | 是 | `CommunityBoardStatus`。 |
| `allowedPostTypes` | string[] | 是 | 允许的帖子类型。 |
| `tags` | string[] | 是 | 默认标签。 |
| `sortOrder` | integer | 是 | 后台排序。 |
| `postCount` | integer | 是 | 已通过帖子数量。 |
| `lastPostAt` | string 或 null | 是 | 最近帖子时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |

#### CommunityPost

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `postId` | string | 是 | 帖子 ID。 |
| `boardId` | string | 是 | 所属板块。 |
| `type` | string | 是 | `CommunityPostType`。 |
| `title` | string | 是 | 2 到 80 位。 |
| `summary` | string | 否 | 最多 200 位。 |
| `body` | string | 是 | Markdown 或纯文本，1 到 20000 位。公开摘要不得返回未审核草稿。 |
| `tags` | string[] | 是 | 0 到 8 个标签，每个 1 到 24 位。 |
| `status` | string | 是 | `CommunityPostStatus`。 |
| `author` | CommunityAuthorSnapshot | 是 | 作者快照。 |
| `linkedContentSnapshot` | object 或 null | 是 | 关联 content 的公开快照。 |
| `linkedResourceSnapshot` | object 或 null | 是 | 关联 resource 的公开快照。 |
| `pollId` | string 或 null | 是 | 关联轻量投票。 |
| `commentCount` | integer | 是 | 已通过评论数。 |
| `likeCount` | integer | 是 | 点赞数。 |
| `favoriteCount` | integer | 是 | 收藏数。 |
| `viewCount` | integer | 是 | 服务端统计浏览数。公开详情读取可以增加该值，但必须按服务端访问指纹去重，不能让同一访客在短时间内无限累加。 |
| `acceptedCommentId` | string 或 null | 是 | 问答帖的采纳评论。 |
| `lastCommentAt` | string 或 null | 是 | 最近评论时间。 |
| `submittedAt` | string 或 null | 是 | 提交审核时间。 |
| `reviewedAt` | string 或 null | 是 | 审核时间。 |
| `reviewerUserId` | string 或 null | 后台可见 | 审核人。公开接口不得返回。 |
| `reviewComment` | string 或 null | 是 | 给作者的审核意见。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注。当前用户和公开接口不得返回。 |
| `notificationStatus` | string 或 null | 是 | 最近通知状态。 |
| `notificationFailure` | CommunityNotificationFailureSummary 或 null | 后台可见 | 通知失败脱敏摘要。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `offlineAt` | string 或 null | 是 | 下架时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |
| `deletedAt` | string 或 null | 是 | 软删除时间。 |

#### CommunityComment

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `commentId` | string | 是 | 评论 ID。 |
| `postId` | string | 是 | 所属帖子。 |
| `parentCommentId` | string 或 null | 是 | 父评论。P1 最多允许二级回复。 |
| `body` | string | 是 | 1 到 10000 位。 |
| `status` | string | 是 | `CommunityCommentStatus`。 |
| `author` | CommunityAuthorSnapshot | 是 | 作者快照。 |
| `likeCount` | integer | 是 | 点赞数。 |
| `isAcceptedAnswer` | boolean | 是 | 是否被采纳。 |
| `submittedAt` | string | 是 | 提交时间。 |
| `reviewedAt` | string 或 null | 是 | 审核时间。 |
| `reviewComment` | string 或 null | 是 | 给作者的审核意见。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `deletedAt` | string 或 null | 是 | 软删除时间。 |

#### CommunityPoll

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `pollId` | string | 是 | 投票 ID。 |
| `postId` | string 或 null | 是 | 关联帖子。 |
| `title` | string | 是 | 2 到 80 位。 |
| `description` | string | 否 | 最多 500 位。 |
| `status` | string | 是 | `CommunityVoteStatus`。 |
| `options` | CommunityPollOption[] | 是 | 2 到 10 个选项。 |
| `multipleChoice` | boolean | 是 | 是否多选。 |
| `minChoices` | integer | 是 | 最少选择数。 |
| `maxChoices` | integer | 是 | 最多选择数。 |
| `eligibleVisibility` | string | 是 | `PUBLIC`、`MEMBER_ONLY` 或 `STAFF_ONLY`。 |
| `anonymousResult` | boolean | 是 | 是否隐藏投票人。 |
| `voteCount` | integer | 是 | 投票人数。 |
| `opensAt` | string 或 null | 是 | 开放时间。非空时，早于该时间不得投票。 |
| `closesAt` | string 或 null | 是 | 关闭时间。非空时，达到或晚于该时间不得投票。`closesAt` 必须晚于 `opensAt`。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### CommunityPollOption

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `optionId` | string | 是 | 选项 ID。 |
| `label` | string | 是 | 1 到 80 位。 |
| `description` | string | 否 | 最多 300 位。 |
| `voteCount` | integer | 后台或公开结果可见 | 投票数。 |

#### CommunityReport

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `reportId` | string | 是 | 举报 ID。 |
| `targetType` | string | 是 | `CommunityReportTargetType`。 |
| `targetId` | string | 是 | 目标 ID。 |
| `reasonType` | string | 是 | `CommunityReportReason`。 |
| `description` | string | 是 | 5 到 2000 位。 |
| `evidenceLinks` | string[] | 是 | 0 到 10 个 http、https 或站内链接。站内链接必须以 `/` 开头，禁止 `javascript:`、`file:`、`ftp:` 和空白链接。 |
| `status` | string | 是 | `CommunityReportStatus`。 |
| `reporter` | CommunityAuthorSnapshot | 是 | 举报人快照。 |
| `assigneeUserId` | string 或 null | 后台可见 | 处理人。 |
| `resolution` | string 或 null | 是 | 处理结果摘要。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注。 |
| `linkedPenaltyId` | string 或 null | 后台可见 | 关联处罚。 |
| `notificationStatus` | string 或 null | 是 | 最近通知状态。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `resolvedAt` | string 或 null | 是 | 处理时间。 |

#### CommunityTicket

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ticketId` | string | 是 | 工单 ID。 |
| `type` | string | 是 | `CommunityTicketType`。 |
| `title` | string | 是 | 2 到 80 位。 |
| `status` | string | 是 | `CommunityTicketStatus`。 |
| `priority` | string | 是 | `LOW`、`NORMAL`、`HIGH` 或 `URGENT`。 |
| `creator` | CommunityAuthorSnapshot | 是 | 创建人快照。 |
| `assigneeUserId` | string 或 null | 后台可见 | 处理人。 |
| `relatedObject` | object 或 null | 是 | 关联帖子、举报、资源、白名单申请或账号问题摘要。P1 只保存摘要字段，禁止保存 token、完整请求头、内部 URL、服务器命令和前序服务内部路径。 |
| `messages` | CommunityTicketMessage[] | 详情可见 | 工单消息。列表只返回最近摘要。 |
| `lastReplyAt` | string 或 null | 是 | 最近回复时间。 |
| `resolvedAt` | string 或 null | 是 | 解决时间。 |
| `closedAt` | string 或 null | 是 | 关闭时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### CommunityTicketMessage

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `messageId` | string | 是 | 消息 ID。 |
| `ticketId` | string | 是 | 工单 ID。 |
| `messageType` | string | 是 | `CommunityTicketMessageType`。 |
| `body` | string | 是 | 1 到 10000 位。 |
| `author` | CommunityAuthorSnapshot 或 null | 是 | 系统事件可为 `null`。 |
| `attachments` | object[] | 是 | P1 只允许站内安全附件摘要，不上传原文件。每个附件必须包含 `attachmentId`、`name` 和以 `/` 开头的站内 `url`，最多 5 个。 |
| `createdAt` | string | 是 | 创建时间。 |

#### CommunityPenalty

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `penaltyId` | string | 是 | 处罚 ID。 |
| `targetUserId` | string | 是 | 被处罚用户。 |
| `targetMemberId` | string 或 null | 是 | 被处罚成员。 |
| `type` | string | 是 | `CommunityPenaltyType`。 |
| `status` | string | 是 | `CommunityPenaltyStatus`。 |
| `reason` | string | 是 | 后台原因，1 到 500 位。 |
| `publicReason` | string | 是 | 对用户可见原因，1 到 200 位。 |
| `evidenceReportId` | string 或 null | 是 | 来源举报。 |
| `relatedPostId` | string 或 null | 是 | 关联帖子。 |
| `relatedCommentId` | string 或 null | 是 | 关联评论。 |
| `startsAt` | string | 是 | 生效时间。 |
| `expiresAt` | string 或 null | 是 | 过期时间。警告可为空。 |
| `createdBy` | string | 是 | 创建操作者。 |
| `revokedBy` | string 或 null | 是 | 解除操作者。 |
| `revokedAt` | string 或 null | 是 | 解除时间。 |
| `revokeReason` | string 或 null | 是 | 解除原因。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### CommunityNotificationFailureSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | 固定为 `FAILED`。 |
| `failureCode` | string | 是 | `49220`、`49221` 或 `49222`。 |
| `failureType` | string | 是 | `UNAVAILABLE`、`TIMEOUT` 或 `BAD_SCHEMA`。 |
| `failureReason` | string | 是 | 脱敏失败摘要。 |
| `failedAt` | string | 是 | 失败发生时间。 |

#### CommunityAuditLog

审计字段继承公共契约，允许补充 `boardId`、`postId`、`commentId`、`pollId`、`reportId`、`ticketId`、`ticketMessageId`、`penaltyId`、`targetUserId`、`stateFrom`、`stateTo`、`idempotencyKey`、`notificationStatus`、`dependencyStatus`、`profileSnapshotStale`、`linkedContentSnapshotStale` 和 `linkedResourceSnapshotStale`。审计日志不得通过 community API 删除。

### community 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `49000` | 404 | 板块不存在，或当前用户无权访问。 |
| `49001` | 404 | 帖子不存在，或当前用户无权访问。 |
| `49002` | 404 | 评论不存在，或当前用户无权访问。 |
| `49003` | 404 | 投票不存在，或当前用户无权访问。 |
| `49004` | 404 | 举报不存在，或当前用户无权访问。 |
| `49005` | 404 | 工单不存在，或当前用户无权访问。 |
| `49006` | 404 | 处罚记录不存在。 |
| `49010` | 409 | 板块状态不允许该操作。 |
| `49011` | 409 | 帖子状态不允许该操作。 |
| `49012` | 409 | 评论状态不允许该操作。 |
| `49013` | 409 | 投票状态不允许该操作。 |
| `49014` | 409 | 举报状态不允许该操作。 |
| `49015` | 409 | 工单状态不允许该操作。 |
| `49016` | 409 | 处罚状态不允许该操作。 |
| `49017` | 409 | community 幂等键请求指纹冲突。 |
| `49018` | 409 | 重复互动或互动状态冲突。 |
| `49019` | 409 | 重复收藏或收藏状态冲突。 |
| `49020` | 409 | 投票资格不足或选项数量不符合规则。 |
| `49021` | 409 | 重复举报或举报目标状态不允许。 |
| `49022` | 409 | 当前用户处于社区处罚限制期。 |
| `49023` | 409 | 评论层级超过限制。 |
| `49024` | 409 | 关联内容或资源状态不允许公开讨论。 |
| `49200` | 502 | auth 认证上下文不可用。 |
| `49201` | 504 | auth 认证上下文调用超时。 |
| `49202` | 502 | auth 响应字段或枚举不兼容 community 契约。 |
| `49210` | 502 | profile 作者快照不可用。 |
| `49211` | 504 | profile 作者快照调用超时。 |
| `49212` | 502 | profile 响应字段不兼容 community 契约。 |
| `49220` | 502 | notification 投递不可用。 |
| `49221` | 504 | notification 投递超时。 |
| `49222` | 502 | notification 响应字段不兼容 community 契约。 |
| `49230` | 502 | content 公开快照不可用。 |
| `49231` | 504 | content 公开快照调用超时。 |
| `49232` | 502 | content 响应字段不兼容 community 契约。 |
| `49240` | 502 | resource 公开快照不可用。 |
| `49241` | 504 | resource 公开快照调用超时。 |
| `49242` | 502 | resource 响应字段不兼容 community 契约。 |
| `49250` | 502 | attendance 贡献入口不可用。P1 不阻断社区主流程。 |
| `54000` | 500 | community 内部错误。 |
| `54001` | 500 | community 审计写入失败。 |
| `54002` | 500 | community 状态写入失败。 |
| `54003` | 500 | community 互动写入失败。 |
| `54004` | 500 | community 工单消息写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误、通用依赖错误和通用服务端错误优先使用公共错误码。community 自有幂等指纹冲突使用 `49017`。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开板块列表 | GET | `/api/v1/community/boards` | 否 | 游客可读公开板块 | LOW |
| 公开板块详情 | GET | `/api/v1/community/boards/{boardId}` | 否 | 游客可读公开板块 | LOW |
| 公开帖子列表 | GET | `/api/v1/community/posts` | 否 | 游客可读公开帖子 | LOW |
| 公开帖子详情 | GET | `/api/v1/community/posts/{postId}` | 否 | 游客可读公开帖子 | LOW |
| 公开评论列表 | GET | `/api/v1/community/posts/{postId}/comments` | 否 | 游客可读公开评论 | LOW |
| 公开投票详情 | GET | `/api/v1/community/polls/{pollId}` | 否 | 游客可读公开投票 | LOW |
| 公开社区搜索 | GET | `/api/v1/community/search` | 否 | 游客可搜公开内容 | LOW |
| 创建帖子 | POST | `/api/v1/community/me/posts` | 是 | 当前用户 | LOW |
| 修改自己的帖子 | PATCH | `/api/v1/community/me/posts/{postId}` | 是 | 当前用户本人 | LOW |
| 提交帖子审核 | POST | `/api/v1/community/me/posts/{postId}/submit` | 是 | 当前用户本人 | LOW |
| 撤回自己的帖子 | PATCH | `/api/v1/community/me/posts/{postId}/withdraw` | 是 | 当前用户本人 | LOW |
| 创建评论 | POST | `/api/v1/community/me/posts/{postId}/comments` | 是 | 当前用户 | LOW |
| 修改自己的评论 | PATCH | `/api/v1/community/me/comments/{commentId}` | 是 | 当前用户本人 | LOW |
| 归档自己的评论 | PATCH | `/api/v1/community/me/comments/{commentId}/archive` | 是 | 当前用户本人 | LOW |
| 点赞帖子 | POST | `/api/v1/community/me/posts/{postId}/like` | 是 | 当前用户 | LOW |
| 取消点赞帖子 | DELETE | `/api/v1/community/me/posts/{postId}/like` | 是 | 当前用户 | LOW |
| 点赞评论 | POST | `/api/v1/community/me/comments/{commentId}/like` | 是 | 当前用户 | LOW |
| 取消点赞评论 | DELETE | `/api/v1/community/me/comments/{commentId}/like` | 是 | 当前用户 | LOW |
| 收藏帖子 | POST | `/api/v1/community/me/posts/{postId}/favorite` | 是 | 当前用户 | LOW |
| 取消收藏帖子 | DELETE | `/api/v1/community/me/posts/{postId}/favorite` | 是 | 当前用户 | LOW |
| 投票 | POST | `/api/v1/community/me/polls/{pollId}/votes` | 是 | 当前用户 | LOW |
| 举报帖子 | POST | `/api/v1/community/me/posts/{postId}/reports` | 是 | 当前用户 | LOW |
| 举报评论 | POST | `/api/v1/community/me/comments/{commentId}/reports` | 是 | 当前用户 | LOW |
| 我的举报进度 | GET | `/api/v1/community/me/reports` | 是 | 当前用户 | LOW |
| 创建工单 | POST | `/api/v1/community/me/tickets` | 是 | 当前用户 | LOW |
| 我的工单列表 | GET | `/api/v1/community/me/tickets` | 是 | 当前用户 | LOW |
| 我的工单详情 | GET | `/api/v1/community/me/tickets/{ticketId}` | 是 | 当前用户本人 | LOW |
| 补充工单 | PATCH | `/api/v1/community/me/tickets/{ticketId}` | 是 | 当前用户本人 | LOW |
| 关闭自己的工单 | POST | `/api/v1/community/me/tickets/{ticketId}/close` | 是 | 当前用户本人 | LOW |
| 后台板块列表 | GET | `/api/v1/community/admin/boards` | 是 | HELPER+ 读 | LOW |
| 创建板块 | POST | `/api/v1/community/admin/boards` | 是 | ADMIN+ | MEDIUM |
| 修改板块 | PATCH | `/api/v1/community/admin/boards/{boardId}` | 是 | ADMIN+ | MEDIUM |
| 归档板块 | PATCH | `/api/v1/community/admin/boards/{boardId}/archive` | 是 | ADMIN+ | MEDIUM |
| 后台帖子列表 | GET | `/api/v1/community/admin/posts` | 是 | HELPER+ | LOW |
| 后台帖子详情 | GET | `/api/v1/community/admin/posts/{postId}` | 是 | HELPER+ | LOW |
| 审核通过帖子 | PATCH | `/api/v1/community/admin/posts/{postId}/approve` | 是 | HELPER+ | MEDIUM |
| 审核拒绝帖子 | PATCH | `/api/v1/community/admin/posts/{postId}/reject` | 是 | HELPER+ | MEDIUM |
| 要求修改帖子 | PATCH | `/api/v1/community/admin/posts/{postId}/request-changes` | 是 | HELPER+ | MEDIUM |
| 下架帖子 | PATCH | `/api/v1/community/admin/posts/{postId}/offline` | 是 | ADMIN+ | MEDIUM |
| 归档帖子 | PATCH | `/api/v1/community/admin/posts/{postId}/archive` | 是 | ADMIN+ | MEDIUM |
| 软删除帖子 | PATCH | `/api/v1/community/admin/posts/{postId}/delete` | 是 | ADMIN+ | HIGH |
| 后台评论列表 | GET | `/api/v1/community/admin/comments` | 是 | HELPER+ | LOW |
| 审核通过评论 | PATCH | `/api/v1/community/admin/comments/{commentId}/approve` | 是 | HELPER+ | MEDIUM |
| 审核拒绝评论 | PATCH | `/api/v1/community/admin/comments/{commentId}/reject` | 是 | HELPER+ | MEDIUM |
| 下架评论 | PATCH | `/api/v1/community/admin/comments/{commentId}/offline` | 是 | ADMIN+ | MEDIUM |
| 后台举报列表 | GET | `/api/v1/community/admin/reports` | 是 | HELPER+ | LOW |
| 后台举报详情 | GET | `/api/v1/community/admin/reports/{reportId}` | 是 | HELPER+ | LOW |
| 分配举报 | PATCH | `/api/v1/community/admin/reports/{reportId}/assign` | 是 | HELPER+ | MEDIUM |
| 处理举报 | PATCH | `/api/v1/community/admin/reports/{reportId}/resolve` | 是 | HELPER+ | MEDIUM |
| 驳回举报 | PATCH | `/api/v1/community/admin/reports/{reportId}/dismiss` | 是 | HELPER+ | MEDIUM |
| 后台工单列表 | GET | `/api/v1/community/admin/tickets` | 是 | HELPER+ | LOW |
| 后台工单详情 | GET | `/api/v1/community/admin/tickets/{ticketId}` | 是 | HELPER+ | LOW |
| 分配工单 | PATCH | `/api/v1/community/admin/tickets/{ticketId}/assign` | 是 | HELPER+ | MEDIUM |
| 回复工单 | POST | `/api/v1/community/admin/tickets/{ticketId}/messages` | 是 | HELPER+ | MEDIUM |
| 推进工单状态 | PATCH | `/api/v1/community/admin/tickets/{ticketId}/status` | 是 | HELPER+ | MEDIUM |
| 创建处罚 | POST | `/api/v1/community/admin/penalties` | 是 | ADMIN+ | HIGH |
| 修正处罚 | PATCH | `/api/v1/community/admin/penalties/{penaltyId}` | 是 | ADMIN+ | HIGH |
| 解除处罚 | PATCH | `/api/v1/community/admin/penalties/{penaltyId}/revoke` | 是 | ADMIN+ | HIGH |
| 创建投票 | POST | `/api/v1/community/admin/polls` | 是 | ADMIN+ | MEDIUM |
| 修改投票 | PATCH | `/api/v1/community/admin/polls/{pollId}` | 是 | ADMIN+ | MEDIUM |
| 开放投票 | PATCH | `/api/v1/community/admin/polls/{pollId}/open` | 是 | ADMIN+ | MEDIUM |
| 关闭投票 | PATCH | `/api/v1/community/admin/polls/{pollId}/close` | 是 | ADMIN+ | MEDIUM |
| community 审计列表 | GET | `/api/v1/community/admin/audit-logs` | 是 | ADMIN+ | LOW |
| community 自检摘要 | GET | `/api/v1/community/admin/ops/summary` | 是 | HELPER+ | LOW |

### 公开接口

#### 公开板块列表

`GET /api/v1/community/boards`

查询参数：`visibility`、`keyword`、`page`、`pageSize` 和 `sort`。`pageSize` 最大 `100`。`sort` 允许 `sortOrder_asc`、`lastPostAt_desc`、`postCount_desc` 和 `createdAt_desc`。

成功响应 HTTP `200`，分页 `items` 为 `CommunityBoard[]`。只返回 `ACTIVE` 且当前访问者可见的板块。

#### 公开板块详情

`GET /api/v1/community/boards/{boardId}`

成功响应 HTTP `200`，`data` 为 `CommunityBoard`，并包含最近已通过帖子摘要。不存在或不可见返回 `49000`。

#### 公开帖子列表

`GET /api/v1/community/posts`

查询参数：`boardId`、`type`、`tag`、`keyword`、`authorUserId`、`linkedContentId`、`linkedResourceId`、`page`、`pageSize` 和 `sort`。`sort` 允许 `lastCommentAt_desc`、`createdAt_desc`、`likeCount_desc`、`viewCount_desc`。

成功响应 HTTP `200`，分页 `items` 为公开视图 `CommunityPost[]`。只返回 `APPROVED`、未下架、未归档、未软删除帖子。响应必须脱敏，不返回 `internalNote`、`reviewerUserId`、举报和处罚字段。

#### 公开帖子详情

`GET /api/v1/community/posts/{postId}`

成功响应 HTTP `200`，`data` 为公开视图 `CommunityPost`。读取可增加 `viewCount`，但该计数必须服务端限流和去重，不能由前端直接传入。帖子不存在或不可见返回 `49001`。

#### 公开评论列表

`GET /api/v1/community/posts/{postId}/comments`

查询参数：`page`、`pageSize`、`parentCommentId` 和 `sort`。`sort` 允许 `createdAt_asc`、`createdAt_desc`、`likeCount_desc`。

成功响应 HTTP `200`，分页 `items` 为公开视图 `CommunityComment[]`。只返回 `APPROVED` 评论。帖子不存在或不可见返回 `49001`。

#### 公开投票详情

`GET /api/v1/community/polls/{pollId}`

成功响应 HTTP `200`，`data` 为 `CommunityPoll`。`OPEN` 投票按 `anonymousResult` 和展示策略返回结果摘要；未到公开时间或不可见返回 `49003`。

#### 公开社区搜索

`GET /api/v1/community/search`

查询参数：`keyword` 必填 1 到 80 位，`scope` 允许 `ALL`、`POST`、`COMMENT`、`BOARD`，`page`、`pageSize` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为公开搜索摘要，不返回草稿、待审核、工单、举报、处罚和内部备注。

### 当前用户接口

#### 创建帖子

`POST /api/v1/community/me/posts`

请求字段：`boardId`、`type`、`title`、`summary`、`body`、`tags`、`linkedContentId`、`linkedResourceId`、`pollDraft` 和 `idempotencyKey`。`title` 2 到 80 位，`body` 1 到 20000 位，`tags` 0 到 8 个。成功响应 HTTP `201`，`data` 为 `CommunityPost`，默认状态为 `DRAFT`。重复同一幂等键和同一请求体返回 HTTP `200`。

业务规则：必须校验板块可见、板块允许该帖子类型、当前用户未被处罚限制、profile 快照可用。引用 content 或 resource 时只能保存公开快照。浏览器传入服务端可信字段必须忽略或返回字段校验失败。

#### 修改自己的帖子

`PATCH /api/v1/community/me/posts/{postId}`

请求字段同创建帖子，可部分更新，必须包含 `idempotencyKey` 时按幂等处理。只允许作者修改 `DRAFT` 或 `NEEDS_CHANGES` 帖子。成功响应 HTTP `200`，`data` 为更新后的 `CommunityPost`。状态不允许返回 `49011`。

#### 提交帖子审核

`POST /api/v1/community/me/posts/{postId}/submit`

请求字段：`idempotencyKey` 可选。`DRAFT` 或 `NEEDS_CHANGES` 可提交为 `PENDING_REVIEW`。已是 `PENDING_REVIEW` 时同请求幂等成功。成功响应 HTTP `200`。

#### 撤回自己的帖子

`PATCH /api/v1/community/me/posts/{postId}/withdraw`

请求字段：`reason` 1 到 200 位，`idempotencyKey` 可选。只允许作者撤回 `DRAFT`、`PENDING_REVIEW` 或 `NEEDS_CHANGES` 帖子，成功后回到 `DRAFT` 或进入 `ARCHIVED`，实现必须固定一种策略并测试。已公开、已下架或终态帖子返回 `49011`。

#### 创建评论

`POST /api/v1/community/me/posts/{postId}/comments`

请求字段：`body`、`parentCommentId`、`idempotencyKey`。成功响应 HTTP `201`，`data` 为 `CommunityComment`。普通用户评论默认 `PENDING_REVIEW`，管理员或可信成员是否可直接 `APPROVED` 由实现固定并测试。锁定帖子、下架帖子、超过二级回复、被处罚限制时返回对应冲突错误。

#### 修改自己的评论

`PATCH /api/v1/community/me/comments/{commentId}`

请求字段：`body`、`idempotencyKey`。只允许作者修改 `PENDING_REVIEW`、`NEEDS_CHANGES` 或实现固定的可编辑 `APPROVED` 评论。成功后需要保留评论版本，且重新进入审核策略必须固定。

#### 归档自己的评论

`PATCH /api/v1/community/me/comments/{commentId}/archive`

请求字段：`reason` 和 `idempotencyKey`。只允许作者归档自己的非终态评论。成功响应 HTTP `200`，状态为 `ARCHIVED` 或 `DELETED`，实现必须固定一种软删除策略。

#### 帖子和评论互动

`POST /api/v1/community/me/posts/{postId}/like`、`DELETE /api/v1/community/me/posts/{postId}/like`、`POST /api/v1/community/me/comments/{commentId}/like`、`DELETE /api/v1/community/me/comments/{commentId}/like`

请求字段：`idempotencyKey` 可选。点赞已点赞目标返回幂等成功，不得重复增加计数。取消未点赞目标返回幂等成功。目标不存在或不可见返回 `49001` 或 `49002`。并发点赞必须保证每个用户每个目标最多一条有效互动。

#### 收藏帖子

`POST /api/v1/community/me/posts/{postId}/favorite`、`DELETE /api/v1/community/me/posts/{postId}/favorite`

请求字段：`idempotencyKey` 可选。收藏和取消收藏必须幂等。只能收藏可见帖子，不得收藏草稿、待审核、下架或软删除帖子。

#### 投票

`POST /api/v1/community/me/polls/{pollId}/votes`

请求字段：`optionIds` 必填，1 到 10 个，`idempotencyKey` 可选。成功响应 HTTP `200`，`data` 包含投票记录摘要和更新后的投票结果。只允许 `OPEN` 投票；资格不足、重复投票、选项数量不满足规则返回 `49020`。投票记录必须服务端按用户去重。是否允许改票由实现固定，推荐 P1 禁止改票。

`eligibleVisibility` 必须在投票时生效。`PUBLIC` 允许所有可写入社区的登录用户投票，`MEMBER_ONLY` 允许普通成员和工作人员投票，`STAFF_ONLY` 只允许 `HELPER`、`ADMIN` 或 `OWNER` 投票。`opensAt` 和 `closesAt` 必须按服务端时间判断，未开放或已关闭返回 `49020`，权限不足返回 `42001`。

#### 举报帖子和评论

`POST /api/v1/community/me/posts/{postId}/reports`、`POST /api/v1/community/me/comments/{commentId}/reports`

请求字段：`reasonType`、`description`、`evidenceLinks`、`idempotencyKey`。成功响应 HTTP `201`，`data` 为 `CommunityReport` 当前用户视图。重复举报同一目标和同一原因在未处理前返回 `49021` 或幂等结果。举报不得向被举报用户泄露举报人。

`evidenceLinks` 必须逐条校验协议，只允许 `http://`、`https://` 或以 `/` 开头的站内链接。非法链接返回 `40001`，不得创建举报。

#### 我的举报进度

`GET /api/v1/community/me/reports`

查询参数：`status`、`targetType`、`page`、`pageSize` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为当前用户自己的举报摘要。响应不得返回处理人内部备注、处罚证据和其他举报人信息。

#### 创建工单

`POST /api/v1/community/me/tickets`

请求字段：`type`、`title`、`body`、`relatedObject`、`attachments`、`idempotencyKey`。成功响应 HTTP `201`，`data` 为 `CommunityTicket` 当前用户视图，状态为 `OPEN` 或 `WAITING_STAFF`。账号状态为 `PENDING_PROFILE` 的用户可以创建账号、白名单和资源问题工单。

`relatedObject` 和 `attachments` 必须保存到工单详情。公开给创建人的视图和后台视图都可以返回安全摘要，但不得返回内部备注、token、请求头、异常堆栈、内部 URL 或服务器命令。

#### 我的工单列表和详情

`GET /api/v1/community/me/tickets`、`GET /api/v1/community/me/tickets/{ticketId}`

列表查询支持 `status`、`type`、`page`、`pageSize` 和 `sort`。详情只返回当前用户自己的工单，消息中不得返回 `INTERNAL_NOTE`。

#### 补充和关闭自己的工单

`PATCH /api/v1/community/me/tickets/{ticketId}` 用于补充回复，请求字段为 `body`、`attachments`、`idempotencyKey`，只允许 `OPEN`、`WAITING_USER` 或 `WAITING_STAFF`。成功后状态进入 `WAITING_STAFF`。

`POST /api/v1/community/me/tickets/{ticketId}/close` 请求字段为 `reason` 和 `idempotencyKey`，只允许创建人关闭自己的非终态工单。成功后状态为 `CLOSED`。

### 后台接口

后台列表接口默认支持 `page`、`pageSize`、`keyword`、状态筛选和稳定排序，`pageSize` 最大 `100`。后台详情可返回内部备注、依赖摘要、通知失败摘要和审计关联，但仍不得返回 token、完整请求头、异常堆栈、通知正文、前序服务内部路径、真实服务器命令、节点凭据或 Cloudreve token。

#### 板块管理

`GET /api/v1/community/admin/boards` 返回全部板块分页。`POST /api/v1/community/admin/boards` 创建板块，请求字段为 `slug`、`name`、`description`、`visibility`、`status`、`allowedPostTypes`、`tags`、`sortOrder`、`reason` 和 `idempotencyKey`。`PATCH /api/v1/community/admin/boards/{boardId}` 修改板块。`PATCH /api/v1/community/admin/boards/{boardId}/archive` 归档板块，请求字段为 `reason` 和 `idempotencyKey`。归档板块后不得接收新帖子。

#### 帖子审核和治理

`GET /api/v1/community/admin/posts` 和 `GET /api/v1/community/admin/posts/{postId}` 用于后台帖子列表和详情。

`PATCH /api/v1/community/admin/posts/{postId}/approve` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，只允许 `PENDING_REVIEW` 或 `NEEDS_CHANGES` 帖子通过，成功后状态为 `APPROVED`，可触发通知。

`PATCH /api/v1/community/admin/posts/{postId}/reject` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，成功后状态为 `REJECTED`。

`PATCH /api/v1/community/admin/posts/{postId}/request-changes` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，成功后状态为 `NEEDS_CHANGES`。

`PATCH /api/v1/community/admin/posts/{postId}/offline` 请求字段为 `publicReason`、`reason`、`idempotencyKey`，只允许 `APPROVED` 或 `LOCKED` 帖子，成功后状态为 `OFFLINE`。

`PATCH /api/v1/community/admin/posts/{postId}/archive` 请求字段为 `reason` 和 `idempotencyKey`，成功后状态为 `ARCHIVED`。

`PATCH /api/v1/community/admin/posts/{postId}/delete` 是 `HIGH` 风险，请求字段为 `reason`、`confirmText` 和 `idempotencyKey`，P1 固定要求 `DELETE_COMMUNITY_POST`。成功后状态为 `DELETED`，只做软删除。

#### 评论审核和治理

`GET /api/v1/community/admin/comments` 返回后台评论分页，可按 `postId`、`authorUserId`、`status`、`keyword` 筛选。`PATCH /api/v1/community/admin/comments/{commentId}/approve`、`reject` 和 `offline` 分别用于通过、拒绝和下架评论，请求字段包含 `reviewComment` 或 `publicReason`、`internalNote`、`reason` 和 `idempotencyKey`。非法状态返回 `49012`。

#### 举报处理

`GET /api/v1/community/admin/reports` 和 `GET /api/v1/community/admin/reports/{reportId}` 用于举报队列。

`PATCH /api/v1/community/admin/reports/{reportId}/assign` 请求字段为 `assigneeUserId`、`reason`、`idempotencyKey`。`HELPER` 只能分配给自己，`ADMIN` 和 `OWNER` 可分配给任一具备后台权限的用户。

`PATCH /api/v1/community/admin/reports/{reportId}/resolve` 请求字段为 `resolution`、`internalNote`、`linkedPenaltyId`、`reason`、`idempotencyKey`。成功后状态为 `RESOLVED`，可以关联已创建处罚。

如果传入 `linkedPenaltyId`，服务端必须确认处罚存在，并把该 ID 保存到举报后台视图。该字段只在后台可见，不得出现在举报人视图或公开接口中。

`PATCH /api/v1/community/admin/reports/{reportId}/dismiss` 请求字段为 `resolution`、`internalNote`、`reason`、`idempotencyKey`。成功后状态为 `DISMISSED`。

#### 工单处理

`GET /api/v1/community/admin/tickets` 和 `GET /api/v1/community/admin/tickets/{ticketId}` 用于工单队列。

`PATCH /api/v1/community/admin/tickets/{ticketId}/assign` 请求字段为 `assigneeUserId`、`reason`、`idempotencyKey`。

`POST /api/v1/community/admin/tickets/{ticketId}/messages` 请求字段为 `messageType`、`body`、`attachments`、`reason`、`idempotencyKey`。`HELPER` 可以写 `STAFF_REPLY`，只有 `ADMIN` 和 `OWNER` 可写 `INTERNAL_NOTE`。

`PATCH /api/v1/community/admin/tickets/{ticketId}/status` 请求字段为 `status`、`publicComment`、`reason`、`idempotencyKey`。只允许在 `OPEN`、`WAITING_STAFF`、`WAITING_USER`、`RESOLVED`、`CLOSED` 之间按服务端状态机推进。非法跳转返回 `49015`。

后台工单状态推进使用固定状态机。`OPEN`、`WAITING_STAFF` 和 `WAITING_USER` 可以进入 `WAITING_STAFF`、`WAITING_USER`、`RESOLVED` 或 `CLOSED`；`RESOLVED` 只能进入 `CLOSED`；`CLOSED` 和 `ARCHIVED` 是终态，不得再改为非终态；`ARCHIVED` 只允许 `ADMIN` 或 `OWNER` 从 `CLOSED` 推进。

#### 处罚管理

`POST /api/v1/community/admin/penalties` 是 `HIGH` 风险，请求字段为 `targetUserId`、`type`、`publicReason`、`reason`、`evidenceReportId`、`relatedPostId`、`relatedCommentId`、`startsAt`、`expiresAt`、`confirmText`、`idempotencyKey`，P1 固定要求 `CREATE_COMMUNITY_PENALTY`。成功响应 HTTP `201`，`data` 为 `CommunityPenalty`。处罚只影响 community 写权限和社区治理状态，不执行真实服务器命令，不移除白名单，不改 attendance 积分。

`PATCH /api/v1/community/admin/penalties/{penaltyId}` 是 `HIGH` 风险，用于修正公开原因、后台原因、过期时间和关联证据。请求必须包含 `reason` 和 `idempotencyKey`。

`PATCH /api/v1/community/admin/penalties/{penaltyId}/revoke` 是 `HIGH` 风险，请求字段为 `publicReason`、`reason`、`confirmText`、`idempotencyKey`，P1 固定要求 `REVOKE_COMMUNITY_PENALTY`。成功后状态为 `REVOKED`。

#### 投票管理

`POST /api/v1/community/admin/polls` 创建轻量投票，请求字段为 `title`、`description`、`postId`、`options`、`multipleChoice`、`minChoices`、`maxChoices`、`eligibleVisibility`、`anonymousResult`、`opensAt`、`closesAt`、`reason` 和 `idempotencyKey`，成功响应 HTTP `201`，默认 `DRAFT`。

`PATCH /api/v1/community/admin/polls/{pollId}` 只允许修改 `DRAFT` 投票。`PATCH /api/v1/community/admin/polls/{pollId}/open` 使投票进入 `OPEN`。`PATCH /api/v1/community/admin/polls/{pollId}/close` 使投票进入 `CLOSED`。关闭后不得再投票。

#### 审计和自检

`GET /api/v1/community/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`result`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为 `CommunityAuditLog[]`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 community API 删除。

`GET /api/v1/community/admin/ops/summary` 成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "community",
    "port": 8132,
    "legacyPort": 8112,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "profileMode": "TEST_STUB",
    "notificationMode": "TEST_STUB",
    "contentMode": "TEST_STUB",
    "resourceMode": "TEST_STUB",
    "attendanceMode": "SKIPPED",
    "testControlsEnabled": false,
    "boardsTotal": 4,
    "postsTotal": 20,
    "pendingReviewPostsTotal": 3,
    "commentsTotal": 60,
    "openReportsTotal": 2,
    "openTicketsTotal": 4,
    "activePenaltiesTotal": 1,
    "pollsOpenTotal": 1,
    "auditsTotal": 100,
    "idempotencyRecordsTotal": 30,
    "lastAuditAt": "2026-05-24T12:00:00Z",
    "productionGaps": [
      "P1_IN_MEMORY_STORAGE",
      "P1_AUTH_STUB",
      "P1_PROFILE_STUB",
      "P1_NOTIFICATION_STUB",
      "P1_CONTENT_STUB",
      "P1_RESOURCE_STUB",
      "ATTENDANCE_CONTRIBUTION_NOT_CONNECTED",
      "TEST_CONTROLS_DISABLED_OUTSIDE_TEST"
    ]
  }
}
```

### 状态、幂等和并发

板块创建后可为 `DRAFT` 或 `ACTIVE`。`ACTIVE` 可锁定为 `LOCKED`，可归档为 `ARCHIVED`。`ARCHIVED` 为终态，不接受新帖子。

帖子创建后默认 `DRAFT`。`DRAFT` 可提交为 `PENDING_REVIEW`。`PENDING_REVIEW` 可通过为 `APPROVED`、拒绝为 `REJECTED`、要求修改为 `NEEDS_CHANGES`，也可由作者撤回。`NEEDS_CHANGES` 可由作者修改后再次提交。`APPROVED` 可锁定、下架、归档或软删除。`REJECTED`、`OFFLINE`、`ARCHIVED` 和 `DELETED` 不得直接回到公开状态，除非未来兼容变更补充恢复接口。

评论创建后默认 `PENDING_REVIEW` 或按可信策略直接 `APPROVED`，实现必须固定并测试。`PENDING_REVIEW` 可通过、拒绝或要求修改。`APPROVED` 可下架、归档或软删除。评论不得在帖子已锁定、下架、归档或删除后继续创建。

举报创建后为 `OPEN`，可分配为 `UNDER_REVIEW`，可处理为 `RESOLVED`、驳回为 `DISMISSED`、升级为 `ESCALATED`，最终可归档。举报处理不得自动创建处罚，必须通过处罚接口明确创建。

工单创建后为 `OPEN` 或 `WAITING_STAFF`。用户补充后进入 `WAITING_STAFF`，工作人员回复后进入 `WAITING_USER` 或 `RESOLVED`，用户或后台可关闭为 `CLOSED`。`ARCHIVED` 为终态。

处罚创建后为 `ACTIVE`。到期后可进入 `EXPIRED`，后台解除后进入 `REVOKED`，归档后进入 `ARCHIVED`。处罚只影响 community 写权限，不调用 whitelist、attendance、真实服务器或 ops-control。

写接口支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `49017`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序。

并发发帖同一幂等键只能产生一个帖子。并发点赞、取消点赞、收藏和取消收藏必须按用户和目标去重。并发审核同一帖子或评论只能有一个最终状态。并发处理同一举报、工单或处罚只能有一个成功状态推进。读取接口允许读到更新前或更新后的完整状态，不能返回半更新对象。

### 审计要求

必须审计的动作包括板块创建、板块修改、板块归档、帖子创建、帖子修改、帖子提交审核、帖子撤回、帖子审核通过、帖子审核拒绝、帖子要求修改、帖子下架、帖子归档、帖子软删除、评论创建、评论修改、评论审核、评论下架、点赞、取消点赞、收藏、取消收藏、投票、举报创建、举报分配、举报处理、举报驳回、工单创建、工单补充、工单分配、工单回复、工单状态推进、工单关闭、处罚创建、处罚修正、处罚解除、投票创建、投票开放、投票关闭、通知失败、依赖降级、自检读取、审计写入失败和状态写入失败。

后台写操作必须记录 `reason`、操作者、目标对象、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计不得泄露 token、完整请求头、Minecraft 验证凭据、profile 后台备注全文、通知正文全文、举报人不该公开的信息、处罚证据原文、真实服务器命令、节点凭据、内部异常堆栈或前序服务内部路径。

审计写入失败时，板块写入、帖子审核、评论审核、举报处理、工单回复、处罚创建、处罚修正、处罚解除、投票管理和高风险软删除不得假装成功，必须返回 `54001` 或 `54000`，并保持业务数据不变。通知失败不回滚主状态，但必须记录失败摘要和审计。

### 失败降级

auth 是所有登录接口强依赖。auth 不可用、超时、用户状态不允许或字段不兼容时不得伪造成功。

profile 是创建帖子、评论、工单、举报和处罚目标快照的强依赖。写入时 profile 失败不得伪造作者或目标资料。公开读取时 profile 失败可以使用旧快照降级，但必须标记 stale。

notification 是辅助依赖。帖子审核、评论审核、举报处理、工单回复、处罚生效和处罚解除的通知失败不得回滚社区主状态，但必须保存脱敏失败摘要和审计。

content 和 resource 是关联讨论的辅助依赖。创建关联讨论时，公开快照不可用不得创建关联帖子。读取已存在帖子时，来源服务失败可以使用已保存快照降级并标记 stale。

attendance 在 P1 不作为社区主流程依赖。社区贡献候选可以记录为 `SKIPPED` 或 `FAILED`，但不得直接写积分余额、流水或榜单。

任何状态写入和互动计数必须保持一致。不能出现点赞记录写入失败但计数增加，或计数更新成功但返回错误后无法追踪的半状态。半成功风险必须返回 `54002` 或 `54003` 并保持可复核状态。

### 验收口径

`community` API 文档按 `docs/contracts-community.md` 独立存在，并由 `.local-docs/tests-community.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`community` 完成时必须满足以下条件：全部接口按本文档实现；公开接口只返回公开可见数据；当前用户接口只能访问自己的帖子草稿、评论、互动、收藏、投票、举报和工单；后台接口按角色限制；帖子、评论、举报、工单、处罚和投票有服务端状态机；举报和工单不是前端假状态；处罚只影响 community 写权限，不执行白名单移除、服务器命令或 attendance 积分修改；所有后台写操作和高风险操作有审计；通知失败按辅助降级记录；当前运行端口固定为 `8132`，自检摘要返回 `port=8132` 和 `legacyPort=8112`；`.local-docs/tests-community.md` 与 `.local-docs/tests-engagement-core.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 community 在 `engagement-core-service` 中全部测试通过；auth、profile、notification、content、server-status、resource、admin、onboarding、exam、whitelist 和 attendance 前序服务回归测试通过；不恢复 `backend/community-service` 旧入口；没有修改前序服务稳定接口；没有把活动、日历、更新日志、后台聚合、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复或 Cloudreve 管理能力塞进 community。

生产化硬化验收还必须满足：测试控制头默认关闭，只有本地自动化测试显式启用时才生效；关闭状态下依赖失败模拟头、写入失败模拟头和通知失败模拟头全部被忽略；自检摘要明确返回当前测试控制头开关状态。

## 北冥官网 activity API 契约

来源：`docs/contracts-activity.md`

版本：0.1

### 文档定位

本文档是 `activity` 微服务的正式 API 契约。后续 `calendar`、`changelog`、前端适配、`admin` 聚合、`ops-control` 和 `external-node-executor` 只能通过本文档定义的接口读取活动、报名、参与确认、结果、奖励、贡献候选、审计和自检摘要，不能直接读取或修改 `activity` 数据库，也不能把活动报名、活动结果或活动奖励逻辑塞进其他服务。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `activity` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了成熟活动平台和社区平台的做法。Discord Guild Scheduled Event 的状态和时间模型说明活动必须由服务端维护明确状态机，终态不能随意回滚。Eventbrite 的 ticket class 和 inventory tier 思路说明名额不能只靠前端按钮限制，服务端必须按活动总名额、分组名额和已确认人数判断。Meetup 的 RSVP、候补和签到说明活动开始前、进行中、结束后要分开处理报名、候补、签到和缺席。Luma 的活动、报名审批、候补和 API 说明活动管理应保留审批、邀请、报名和数据同步边界。activity 本轮只吸收这些设计思路，不引入付费票务、外部支付、外部日历主数据或商业活动组织模型。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Discord Guild Scheduled Event](https://docs.discord.com/developers/resources/guild-scheduled-event) | 活动有开始、结束、状态和终态，服务端应限制状态流转。 |
| [Eventbrite Ticket Classes](https://www.eventbrite.com/platform/docs/ticket-classes) | 名额应由服务端库存模型和售出数量共同约束，不能只靠前端展示。 |
| [Meetup attendee and attendance management](https://help.meetup.com/hc/en-us/articles/9389668230541-Manage-attendees-and-track-attendance-for-your-Meetup-event-on-the-web) | 报名名单、候补名单、签到、缺席和活动后状态需要分开记录。 |
| [Meetup GraphQL API guide](https://www.meetup.com/graphql/guide/) | 活动草稿发布、RSVP 和报名问题应作为结构化接口，不写死在页面。 |
| [Luma API](https://help.luma.com/p/luma-api) | 活动 API 应覆盖活动、日历、邀请、报名、候补和统计，但本项目只保留 activity 自身主数据。 |

### 职责边界

`activity` 负责活动草稿、活动审核、活动发布、报名、候补、参与确认、签到、缺席、活动结果、获奖名单、奖励记录、贡献候选、通知投递摘要、活动审计、自检摘要和自身幂等记录。

`activity` 不负责注册、登录、邀请码、会话、账号角色能力点、Minecraft 绑定主数据、成员档案主数据、站内通知主数据、官网公告主发布、资源下载主数据、考勤积分主数据、社区帖子评论、工单举报处罚、日历主数据、更新日志主数据、后台聚合入口、真实服务器运维控制、节点守护进程、容器、终端、文件管理、备份恢复、Cloudreve 管理或外部支付。

`activity` 只能后序适配前序服务。它通过 `auth` 认证上下文读取当前用户、角色、能力点和用户状态；通过 `profile` 的正式接口或未来服务间适配器读取成员展示快照和成员状态；通过 `notification` 投递报名、候补、取消、签到、结果和奖励通知；通过 `attendance` 未来正式贡献入口接收贡献结果；通过 `community`、`content` 和 `resource` 的公开快照关联讨论、说明页和活动资源。`activity` 不能导入前序服务内存存储、实体、Repository、测试种子或内部类，不能要求前序服务为了 activity 反向修改稳定接口。

### 数据归属

`activity` 拥有以下主数据：活动、活动版本记录、报名记录、候补记录、参与确认记录、签到记录、活动结果、获奖名单、奖励记录、奖励发放摘要、活动贡献候选、通知投递摘要、依赖调用摘要、幂等记录、活动审计日志和自检统计。

`activity` 可以保存来自 `auth` 的 `userId`、`displayName`、`roles`、`permissions` 和用户状态快照；可以保存来自 `profile` 的成员展示名、头像、成员组、成员状态和 Minecraft ID 快照；可以保存来自 `notification` 的投递结果摘要；可以保存来自 `attendance` 的贡献接收或拒绝摘要；可以保存来自 `community`、`content` 和 `resource` 的公开对象快照。快照只服务展示、检索和审计，不能成为来源模块主数据，也不能反写来源模块。

### 基础路径与认证

所有接口默认使用 `/api/v1/activity` 前缀。当前运行入口为 `engagement-core-service:8132`。历史原服务端口 `8113` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或回归测试命令。

公开接口允许游客读取已发布、未下架、未归档、未软删除的数据。公开接口不得返回内部备注、报名审核参数、管理员 ID、通知失败详情、完整依赖错误、奖励后台备注、贡献候选内部原因或审计参数。

当前用户接口使用 `/api/v1/activity/me` 前缀，全部要求 `Authorization: Bearer <token>`。当前用户只能读取和维护自己的报名、候补、签到结果、获奖记录和奖励记录。浏览器请求体不得传入 `userId`、`memberId`、`roles`、`permissions`、`participantSnapshot`、`status`、`registrationStatus`、`checkInStatus`、`winnerStatus`、`rewardStatus`、`attendanceContributionStatus`、`notificationStatus`、`reviewerUserId`、`operatorUserId`、`auditResult` 等服务端可信字段。

后台接口使用 `/api/v1/activity/admin` 前缀，全部要求登录。后台读取活动、报名、结果、奖励、审计和自检摘要要求 `HELPER`、`ADMIN` 或 `OWNER`。活动创建、修改、审核、发布、下架、归档、报名管理、签到确认、结果录入、结果发布、奖励创建和奖励发放标记要求 `HELPER`、`ADMIN` 或 `OWNER`，但 `HELPER` 只能处理被授权的初审、报名确认和签到协助。活动软删除、奖励撤销、批量参与确认和贡献候选生成要求 `ADMIN` 或 `OWNER`。真实积分入账在本轮不开放。

### 本地测试控制头

`activity` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Profile-Mode`、`X-Test-Notification-Mode`、`X-Test-Attendance-Mode`、`X-Test-Community-Mode`、`X-Test-Content-Mode`、`X-Test-Resource-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store`、`X-Test-Now` 和 `X-Test-Fail-Registration` 模拟依赖失败、通知失败、写入失败、时间边界和报名并发冲突。该能力只服务本地测试闭环，不属于正式业务 API。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、状态失败、通知失败、报名失败或快照 stale。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 视为已满足的生产化硬化项。

### 前序服务兼容契约

`auth` 是所有登录接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions`、`status` 和可选 `minecraftBinding`。用户状态为 `ACTIVE` 时可报名和签到；`PENDING_PROFILE` 可以查看公开活动但不能报名成员限定活动；`DISABLED`、`BANNED`、`DELETED` 不允许写入活动状态。auth 不可用返回 `49400`，auth 超时返回 `49401`，字段或枚举不兼容返回 `49402`。

`profile` 是成员资格和展示快照的强依赖。成员限定活动、报名、获奖名单和签到确认必须读取成员快照。profile 不可用、超时或字段不兼容时，写入不得伪造成功，分别返回 `49410`、`49411` 或 `49412`。只读公开列表可以使用已有快照降级，但必须返回 `profileSnapshotStale=true` 或依赖摘要，且不得刷新为伪造资料。

`notification` 是辅助依赖。报名确认、报名拒绝、候补转正、取消报名、签到结果、结果发布、获奖和奖励发放可以触发通知。通知失败不得回滚活动主状态，但必须保存 `notificationStatus=FAILED`、失败原因摘要并写入审计。notification 不可用记录或返回 `49420`，超时记录或返回 `49421`，字段不兼容记录或返回 `49422`。通知失败摘要不得包含通知正文、完整请求头、认证 token、内部 URL 或异常堆栈。

`attendance` 当前只作为未来贡献入口。P1 中 `activity` 只能记录 `ActivityContributionCandidate`，不得直接写 attendance 积分余额、流水或榜单。后续需要活动奖励入账时，必须作为 `attendance` 兼容变更先补充契约、测试和回归，再由 `activity` 通过正式接口适配。attendance 不可用不得影响活动结果和奖励主流程，只能记录 `attendanceContributionStatus=SKIPPED` 或 `FAILED`。

`community` 是活动讨论、反馈和投票的可选关联来源。activity 可以保存 community 公开帖子、投票或反馈对象快照，但不能创建社区处罚，不能处理举报工单，不能直接读 community 内存存储或修改社区主状态。community 不可用时，创建关联活动返回 `49430`；读取已保存活动可展示旧快照并标记 stale。

`content` 是活动说明页、公告页和专题页的可选关联来源。activity 不能吞掉 content 的发布审核流程，不能直接创建官网公告。content 不可用时，创建关联活动返回 `49440`；公开读取可以展示已保存快照并标记 stale。

`resource` 是活动资源包、地图、规则文档或报名材料下载的可选关联来源。activity 不能生成 resource 下载票据，不能保存 Cloudreve 管理 token，不能做后台文件管理。resource 不可用时，创建关联活动返回 `49450`；公开读取可以展示已保存快照并标记 stale。

`admin` 当前只是聚合入口。本文档不修改 `admin` 契约。若后续需要 admin 展示 activity 待办、指标或审计索引，必须作为 admin 兼容变更单独走契约、测试、红灯、实现和回归闭环。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `ActivityType` | `BUILD`、`REDSTONE`、`SURVIVAL`、`PVP`、`COMMUNITY`、`MEETING`、`MAINTENANCE_PREP`、`OTHER` | 活动类型。`MAINTENANCE_PREP` 只表示活动准备，不等同运维维护窗口。 |
| `ActivityVisibility` | `PUBLIC`、`MEMBER_ONLY`、`STAFF_ONLY`、`INVITE_ONLY` | 活动可见和报名范围。 |
| `ActivityStatus` | `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`PUBLISHED`、`REGISTRATION_OPEN`、`REGISTRATION_CLOSED`、`RUNNING`、`COMPLETED`、`RESULT_PUBLISHED`、`OFFLINE`、`ARCHIVED`、`DELETED` | 活动主状态。 |
| `RegistrationPolicy` | `OPEN`、`APPROVAL_REQUIRED`、`INVITE_ONLY`、`STAFF_ASSIGNED` | 报名策略。 |
| `ActivityRegistrationStatus` | `SUBMITTED`、`CONFIRMED`、`WAITLISTED`、`REJECTED`、`CANCELED`、`CHECKED_IN`、`NO_SHOW` | 报名和参与状态。 |
| `CheckInMethod` | `MANUAL`、`CODE`、`ADMIN_CONFIRM` | 签到方式。P1 不开放二维码真实校验，只保留 `CODE` 字段和测试。 |
| `ActivityResultStatus` | `DRAFT`、`PUBLISHED`、`ARCHIVED` | 活动结果状态。 |
| `ActivityRewardType` | `POINTS_CANDIDATE`、`TITLE`、`ITEM`、`RESOURCE_ACCESS`、`CUSTOM` | 奖励类型。`POINTS_CANDIDATE` 只生成贡献候选，不直接加分。 |
| `ActivityRewardStatus` | `DRAFT`、`PENDING_ISSUE`、`ISSUED`、`REVOKED`、`FAILED` | 奖励状态。 |
| `ActivityContributionStatus` | `PENDING`、`SKIPPED`、`FAILED`、`ACCEPTED` | 贡献候选状态。P1 默认不进入 `ACCEPTED`。 |
| `ActivityNotificationStatus` | `DELIVERED`、`FAILED`、`SKIPPED` | 最近一次通知摘要。 |
| `ActivityAuditResult` | `SUCCESS`、`FAILED` | activity 审计结果。 |

### 通用对象

#### ActivityParticipantSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | string | 是 | auth 用户 ID。 |
| `memberId` | string 或 null | 是 | profile 成员 ID。 |
| `displayNameSnapshot` | string | 是 | 展示名快照。 |
| `avatarUrlSnapshot` | string 或 null | 是 | 头像快照。 |
| `memberGroupSnapshot` | string 或 null | 是 | 成员组快照。 |
| `memberStatusSnapshot` | string 或 null | 是 | 成员状态快照。 |
| `minecraftIdSnapshot` | string 或 null | 是 | Minecraft ID 快照。 |
| `profileSnapshotStale` | boolean | 是 | 是否使用旧快照。 |

#### Activity

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `activityId` | string | 是 | 活动 ID。 |
| `slug` | string | 是 | 活动 slug，2 到 80 位，小写字母、数字和短横线。 |
| `title` | string | 是 | 2 到 80 位。 |
| `summary` | string | 是 | 1 到 200 位。 |
| `description` | string | 是 | 1 到 5000 位，公开详情可见。 |
| `type` | string | 是 | `ActivityType`。 |
| `visibility` | string | 是 | `ActivityVisibility`。 |
| `registrationPolicy` | string | 是 | `RegistrationPolicy`。 |
| `status` | string | 是 | `ActivityStatus`。 |
| `startAt` | string | 是 | ISO 8601，活动开始时间。 |
| `endAt` | string | 是 | ISO 8601，必须晚于 `startAt`。 |
| `registrationOpenAt` | string 或 null | 是 | 报名开放时间。为空时随发布后开放。 |
| `registrationCloseAt` | string 或 null | 是 | 报名关闭时间。必须早于或等于 `startAt`。 |
| `capacity` | integer 或 null | 是 | 确认报名名额。为空表示不限制。 |
| `waitlistCapacity` | integer | 是 | 候补名额，`0` 表示不开候补。 |
| `confirmedCount` | integer | 是 | 已确认人数。 |
| `waitlistedCount` | integer | 是 | 候补人数。 |
| `checkedInCount` | integer | 是 | 已签到人数。 |
| `noShowCount` | integer | 是 | 缺席人数。 |
| `locationText` | string 或 null | 是 | 活动地点或线上说明。 |
| `coverImageUrl` | string 或 null | 是 | 活动封面，必须是 http、https 或站内路径。 |
| `tags` | string[] | 是 | 0 到 8 个标签，每个 1 到 24 位。 |
| `discussionSnapshot` | object 或 null | 是 | community 公开讨论快照。 |
| `contentSnapshot` | object 或 null | 是 | content 公开说明页快照。 |
| `resourceSnapshots` | object[] | 是 | resource 公开资源快照。 |
| `reviewComment` | string 或 null | 是 | 给创建者的审核意见。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注，公开和当前用户接口不得返回。 |
| `notificationStatus` | string 或 null | 是 | 最近一次通知状态。 |
| `notificationFailure` | ActivityNotificationFailureSummary 或 null | 后台可见 | 通知失败脱敏摘要。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `submittedAt` | string 或 null | 是 | 提交审核时间。 |
| `reviewedAt` | string 或 null | 是 | 审核时间。 |
| `publishedAt` | string 或 null | 是 | 发布时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `offlineAt` | string 或 null | 是 | 下架时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |
| `deletedAt` | string 或 null | 是 | 软删除时间。 |

#### ActivityRegistration

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `registrationId` | string | 是 | 报名 ID。 |
| `activityId` | string | 是 | 活动 ID。 |
| `participant` | ActivityParticipantSnapshot | 是 | 参与者快照。 |
| `status` | string | 是 | `ActivityRegistrationStatus`。 |
| `answers` | object | 是 | 报名问题答案。P1 最多 20 个键值。 |
| `guestCount` | integer | 是 | 随行人数，P1 默认只允许 `0`。 |
| `waitlistRank` | integer 或 null | 是 | 候补排序。 |
| `reviewComment` | string 或 null | 是 | 报名处理意见。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注。 |
| `checkedInAt` | string 或 null | 是 | 签到时间。 |
| `noShowAt` | string 或 null | 是 | 缺席标记时间。 |
| `canceledAt` | string 或 null | 是 | 取消时间。 |
| `notificationStatus` | string 或 null | 是 | 最近一次通知状态。 |
| `notificationFailure` | ActivityNotificationFailureSummary 或 null | 后台可见 | 通知失败脱敏摘要。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### ActivityResult

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `resultId` | string | 是 | 结果 ID。 |
| `activityId` | string | 是 | 活动 ID。 |
| `status` | string | 是 | `ActivityResultStatus`。 |
| `title` | string | 是 | 2 到 80 位。 |
| `summary` | string | 是 | 1 到 500 位。 |
| `details` | string | 否 | 最多 5000 位。 |
| `participantTotal` | integer | 是 | 参与总数。 |
| `winnerTotal` | integer | 是 | 获奖人数。 |
| `publishedAt` | string 或 null | 是 | 发布时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### ActivityReward

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `rewardId` | string | 是 | 奖励 ID。 |
| `activityId` | string | 是 | 活动 ID。 |
| `registrationId` | string | 是 | 报名 ID。 |
| `recipient` | ActivityParticipantSnapshot | 是 | 获奖人快照。 |
| `type` | string | 是 | `ActivityRewardType`。 |
| `title` | string | 是 | 2 到 80 位。 |
| `description` | string | 否 | 最多 1000 位。 |
| `quantity` | integer | 是 | 数量，1 到 999。 |
| `scoreCandidateDelta` | integer | 是 | 积分候选值，0 到 1000。P1 不直接入账。 |
| `status` | string | 是 | `ActivityRewardStatus`。 |
| `issuedAt` | string 或 null | 是 | 发放标记时间。 |
| `revokedAt` | string 或 null | 是 | 撤销时间。 |
| `notificationStatus` | string 或 null | 是 | 最近一次通知状态。 |
| `notificationFailure` | ActivityNotificationFailureSummary 或 null | 后台可见 | 通知失败脱敏摘要。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### ActivityContributionCandidate

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `candidateId` | string | 是 | 贡献候选 ID。 |
| `activityId` | string | 是 | 活动 ID。 |
| `registrationId` | string | 是 | 报名 ID。 |
| `rewardId` | string 或 null | 是 | 来源奖励 ID。 |
| `memberId` | string | 是 | 成员 ID。 |
| `userId` | string | 是 | 用户 ID。 |
| `title` | string | 是 | 候选标题。 |
| `description` | string | 否 | 候选说明。 |
| `scoreDelta` | integer | 是 | 候选积分。 |
| `status` | string | 是 | `ActivityContributionStatus`。 |
| `attendanceResponseSummary` | object 或 null | 是 | future attendance 接收摘要。P1 默认为 `null`。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### ActivityNotificationFailureSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | 固定为 `FAILED`。 |
| `failureCode` | string | 是 | `49420`、`49421` 或 `49422`。 |
| `failureType` | string | 是 | `UNAVAILABLE`、`TIMEOUT` 或 `BAD_SCHEMA`。 |
| `failureReason` | string | 是 | 脱敏失败摘要。 |
| `failedAt` | string | 是 | 失败发生时间。 |

#### ActivityAuditLog

审计字段继承公共契约，允许补充 `activityId`、`registrationId`、`resultId`、`rewardId`、`candidateId`、`stateFrom`、`stateTo`、`idempotencyKey`、`notificationStatus`、`dependencyStatus`、`profileSnapshotStale` 和 `registrationRank`。审计日志不得通过 activity API 删除。

### activity 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `49400` | 502 | auth 认证上下文不可用。 |
| `49401` | 504 | auth 认证上下文调用超时。 |
| `49402` | 502 | auth 返回字段不兼容 activity 契约。 |
| `49410` | 502 | profile 成员快照不可用。 |
| `49411` | 504 | profile 成员快照调用超时。 |
| `49412` | 502 | profile 返回字段不兼容 activity 契约。 |
| `49420` | 502 | notification 投递不可用。 |
| `49421` | 504 | notification 投递超时。 |
| `49422` | 502 | notification 返回字段不兼容 activity 契约。 |
| `49430` | 502 | community 公开快照不可用。 |
| `49440` | 502 | content 公开快照不可用。 |
| `49450` | 502 | resource 公开快照不可用。 |
| `49600` | 404 | 活动不存在或不可见。 |
| `49601` | 404 | 报名记录不存在或不可见。 |
| `49602` | 404 | 活动结果不存在或不可见。 |
| `49603` | 404 | 奖励记录不存在或不可见。 |
| `49610` | 409 | 活动状态不允许当前操作。 |
| `49611` | 409 | 报名状态不允许当前操作。 |
| `49612` | 409 | 活动未开放报名或报名已截止。 |
| `49613` | 409 | 活动名额已满且候补不可用。 |
| `49614` | 409 | 重复报名或重复签到。 |
| `49615` | 409 | 候补转正条件不满足。 |
| `49616` | 409 | 结果或奖励状态不允许当前操作。 |
| `49617` | 409 | 幂等键冲突。 |
| `49618` | 409 | 活动时间窗口不允许签到。 |
| `49619` | 409 | 活动 slug 已存在。 |
| `49620` | 403 | 活动参与资格不足。 |
| `49621` | 400 | 报名问题答案不满足要求。 |
| `49622` | 409 | 贡献候选已生成或当前不允许生成。 |
| `54600` | 500 | activity 内部错误。 |
| `54601` | 500 | activity 审计写入失败。 |
| `54602` | 500 | activity 状态写入失败。 |
| `54603` | 500 | activity 报名计数写入失败。 |
| `54604` | 500 | activity 奖励或贡献候选写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。幂等冲突可返回公共 `43002` 或 activity 细分 `49617`，同一接口实现必须固定并测试。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开活动列表 | GET | `/api/v1/activity/events` | 否 | 公开 | LOW |
| 公开活动详情 | GET | `/api/v1/activity/events/{activityIdOrSlug}` | 否 | 公开 | LOW |
| 公开活动结果 | GET | `/api/v1/activity/events/{activityId}/result` | 否 | 公开 | LOW |
| 公开活动日历摘要 | GET | `/api/v1/activity/calendar-summary` | 否 | 公开 | LOW |
| 我的报名列表 | GET | `/api/v1/activity/me/registrations` | 是 | 当前用户 | LOW |
| 我的活动详情 | GET | `/api/v1/activity/me/registrations/{registrationId}` | 是 | 当前用户 | LOW |
| 报名活动 | POST | `/api/v1/activity/me/events/{activityId}/registrations` | 是 | 当前用户 | LOW |
| 取消报名 | POST | `/api/v1/activity/me/registrations/{registrationId}/cancel` | 是 | 当前用户 | LOW |
| 我的签到结果 | GET | `/api/v1/activity/me/events/{activityId}/check-in` | 是 | 当前用户 | LOW |
| 我的奖励记录 | GET | `/api/v1/activity/me/rewards` | 是 | 当前用户 | LOW |
| 后台活动列表 | GET | `/api/v1/activity/admin/events` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 后台活动详情 | GET | `/api/v1/activity/admin/events/{activityId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 创建活动草稿 | POST | `/api/v1/activity/admin/events` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 修改活动 | PATCH | `/api/v1/activity/admin/events/{activityId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 提交活动审核 | POST | `/api/v1/activity/admin/events/{activityId}/submit` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核通过活动 | PATCH | `/api/v1/activity/admin/events/{activityId}/approve` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核拒绝活动 | PATCH | `/api/v1/activity/admin/events/{activityId}/reject` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 要求修改活动 | PATCH | `/api/v1/activity/admin/events/{activityId}/request-changes` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 发布活动 | PATCH | `/api/v1/activity/admin/events/{activityId}/publish` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 开放报名 | PATCH | `/api/v1/activity/admin/events/{activityId}/open-registration` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 关闭报名 | PATCH | `/api/v1/activity/admin/events/{activityId}/close-registration` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 标记活动进行中 | PATCH | `/api/v1/activity/admin/events/{activityId}/start` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 标记活动完成 | PATCH | `/api/v1/activity/admin/events/{activityId}/complete` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 下架活动 | PATCH | `/api/v1/activity/admin/events/{activityId}/offline` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 归档活动 | PATCH | `/api/v1/activity/admin/events/{activityId}/archive` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 软删除活动 | PATCH | `/api/v1/activity/admin/events/{activityId}/delete` | 是 | `ADMIN`、`OWNER` | HIGH |
| 报名名单 | GET | `/api/v1/activity/admin/events/{activityId}/registrations` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 确认报名 | PATCH | `/api/v1/activity/admin/registrations/{registrationId}/confirm` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 拒绝报名 | PATCH | `/api/v1/activity/admin/registrations/{registrationId}/reject` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 候补转正 | PATCH | `/api/v1/activity/admin/registrations/{registrationId}/promote` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 管理员取消报名 | PATCH | `/api/v1/activity/admin/registrations/{registrationId}/cancel` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 签到或参与确认 | PATCH | `/api/v1/activity/admin/registrations/{registrationId}/check-in` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 标记缺席 | PATCH | `/api/v1/activity/admin/registrations/{registrationId}/no-show` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 创建或修改结果 | PUT | `/api/v1/activity/admin/events/{activityId}/result` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 发布结果 | PATCH | `/api/v1/activity/admin/events/{activityId}/result/publish` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 创建奖励 | POST | `/api/v1/activity/admin/events/{activityId}/rewards` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 标记奖励已发放 | PATCH | `/api/v1/activity/admin/rewards/{rewardId}/issue` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 撤销奖励 | PATCH | `/api/v1/activity/admin/rewards/{rewardId}/revoke` | 是 | `ADMIN`、`OWNER` | HIGH |
| 生成贡献候选 | POST | `/api/v1/activity/admin/events/{activityId}/contribution-candidates` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 审计列表 | GET | `/api/v1/activity/admin/audit-logs` | 是 | `ADMIN`、`OWNER` | LOW |
| activity 自检摘要 | GET | `/api/v1/activity/admin/ops/summary` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |

### 公开接口

#### 公开活动列表

`GET /api/v1/activity/events`

查询参数：`page`、`pageSize`、`keyword`、`type`、`visibility`、`status`、`from`、`to`、`tag` 和 `sort`。`pageSize` 最大 `100`。`sort` 允许 `startAt_asc`、`startAt_desc`、`publishedAt_desc`、`createdAt_desc`。成功响应 HTTP `200`，分页 `items` 为公开视图 `Activity[]`。

业务规则：游客只看到 `PUBLISHED`、`REGISTRATION_OPEN`、`REGISTRATION_CLOSED`、`RUNNING`、`COMPLETED` 和 `RESULT_PUBLISHED` 的公开活动。`MEMBER_ONLY` 活动可展示摘要，但报名资格只在登录报名时判定。列表不得返回内部备注、通知失败详情、审计字段和未发布结果。

#### 公开活动详情

`GET /api/v1/activity/events/{activityIdOrSlug}`

成功响应 HTTP `200`，`data` 为公开视图 `Activity`，包含报名名额、候补名额、活动时间、公开说明和公开关联快照。活动不存在、不可见、已下架、已归档或已删除时返回 `49600`。

#### 公开活动结果

`GET /api/v1/activity/events/{activityId}/result`

成功响应 HTTP `200`，`data` 包含 `ActivityResult`、公开获奖摘要和公开奖励摘要。只有活动状态为 `RESULT_PUBLISHED` 且结果状态为 `PUBLISHED` 时可见。结果未发布返回 `49602`。

#### 公开活动日历摘要

`GET /api/v1/activity/calendar-summary`

查询参数：`from`、`to`、`type`、`visibility`。成功响应 HTTP `200`，`data.items` 为活动时间摘要，字段包含 `activityId`、`slug`、`title`、`type`、`visibility`、`status`、`startAt`、`endAt`、`registrationCloseAt` 和 `summary`。该接口只提供后续 `calendar` 适配的只读摘要，不创建 calendar 主数据。

### 当前用户接口

#### 我的报名列表和详情

`GET /api/v1/activity/me/registrations` 支持 `page`、`pageSize`、`status`、`activityStatus`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为当前用户自己的 `ActivityRegistration[]`。

`GET /api/v1/activity/me/registrations/{registrationId}` 只返回当前用户自己的报名详情。不存在或不属于当前用户返回 `49601`。

#### 报名活动

`POST /api/v1/activity/me/events/{activityId}/registrations`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `answers` | object | 否 | 报名问题答案，最多 20 个键值。 |
| `guestCount` | integer | 否 | P1 固定为 `0`。 |
| `note` | string | 否 | 最多 500 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `201`，`data` 为 `ActivityRegistration`。`OPEN` 报名策略在名额未满时进入 `CONFIRMED`；需要审批时进入 `SUBMITTED`；名额已满且候补可用时进入 `WAITLISTED`；名额和候补都满返回 `49613`。重复报名返回 `49614` 或同幂等结果。报名必须按服务端时间判断报名窗口，未开放或已截止返回 `49612`。

#### 取消报名

`POST /api/v1/activity/me/registrations/{registrationId}/cancel`

请求字段：`reason` 1 到 200 位，`idempotencyKey` 可选。只允许当前用户取消 `SUBMITTED`、`CONFIRMED` 或 `WAITLISTED` 报名。活动已 `RUNNING`、`COMPLETED` 或 `RESULT_PUBLISHED` 后普通用户不得取消，返回 `49611`。取消成功后释放名额，但不会自动转正候补，候补转正必须由后台明确执行。

#### 我的签到结果

`GET /api/v1/activity/me/events/{activityId}/check-in`

成功响应 HTTP `200`。当前用户无报名时返回 `data=null`。已签到返回 `status=CHECKED_IN` 和 `checkedInAt`，缺席返回 `status=NO_SHOW`。

#### 我的奖励记录

`GET /api/v1/activity/me/rewards`

查询参数：`page`、`pageSize`、`status`、`activityId` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为当前用户可见的 `ActivityReward[]`。响应不得返回后台备注、贡献候选内部原因或通知失败详情。

### 后台接口

#### 活动管理

`GET /api/v1/activity/admin/events` 返回后台活动分页，支持 `page`、`pageSize`、`keyword`、`type`、`visibility`、`status`、`from`、`to`、`createdBy` 和 `sort`。

`GET /api/v1/activity/admin/events/{activityId}` 返回后台活动详情，包含活动、报名统计、结果、奖励摘要、贡献候选摘要、依赖摘要和最近审计。响应不得返回 token、完整请求头、通知正文、前序服务内部路径、异常堆栈、真实服务器命令、节点凭据或 Cloudreve token。

`POST /api/v1/activity/admin/events` 创建草稿。请求字段为 `slug`、`title`、`summary`、`description`、`type`、`visibility`、`registrationPolicy`、`startAt`、`endAt`、`registrationOpenAt`、`registrationCloseAt`、`capacity`、`waitlistCapacity`、`locationText`、`coverImageUrl`、`tags`、`linkedCommunityId`、`linkedContentId`、`linkedResourceIds`、`internalNote`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，状态为 `DRAFT`。slug 冲突返回 `49619`。

`PATCH /api/v1/activity/admin/events/{activityId}` 修改活动。只允许 `DRAFT`、`NEEDS_CHANGES`、`REJECTED` 和未发布的 `APPROVED`。请求字段同创建，按需修改，`reason` 必填。已发布活动如需修改时间、名额或报名策略，P1 必须先下架或归档后新建活动，避免破坏已报名成员预期。

#### 活动审核发布状态

`POST /api/v1/activity/admin/events/{activityId}/submit` 使 `DRAFT`、`NEEDS_CHANGES` 或 `REJECTED` 进入 `PENDING_REVIEW`。

`PATCH /api/v1/activity/admin/events/{activityId}/approve` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，使 `PENDING_REVIEW` 或 `NEEDS_CHANGES` 进入 `APPROVED`。

`PATCH /api/v1/activity/admin/events/{activityId}/reject` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，使 `PENDING_REVIEW` 进入 `REJECTED`。

`PATCH /api/v1/activity/admin/events/{activityId}/request-changes` 请求字段同拒绝，使 `PENDING_REVIEW` 进入 `NEEDS_CHANGES`。

`PATCH /api/v1/activity/admin/events/{activityId}/publish` 使 `APPROVED` 进入 `PUBLISHED` 或 `REGISTRATION_OPEN`。如果当前时间在报名窗口内且策略允许报名，推荐直接进入 `REGISTRATION_OPEN`。发布成功可以触发通知，通知失败不回滚主状态。

`PATCH /api/v1/activity/admin/events/{activityId}/open-registration` 使 `PUBLISHED` 或 `REGISTRATION_CLOSED` 进入 `REGISTRATION_OPEN`。

`PATCH /api/v1/activity/admin/events/{activityId}/close-registration` 使 `REGISTRATION_OPEN` 进入 `REGISTRATION_CLOSED`。

`PATCH /api/v1/activity/admin/events/{activityId}/start` 使 `PUBLISHED`、`REGISTRATION_OPEN` 或 `REGISTRATION_CLOSED` 进入 `RUNNING`。

`PATCH /api/v1/activity/admin/events/{activityId}/complete` 使 `RUNNING` 进入 `COMPLETED`。

`PATCH /api/v1/activity/admin/events/{activityId}/offline` 请求字段为 `publicReason`、`reason`、`idempotencyKey`，使公开活动进入 `OFFLINE`。

`PATCH /api/v1/activity/admin/events/{activityId}/archive` 请求字段为 `reason` 和 `idempotencyKey`，使 `COMPLETED`、`RESULT_PUBLISHED` 或 `OFFLINE` 进入 `ARCHIVED`。

`PATCH /api/v1/activity/admin/events/{activityId}/delete` 是 `HIGH` 风险，请求字段为 `reason`、`confirmText` 和 `idempotencyKey`，P1 固定要求 `DELETE_ACTIVITY_EVENT`。成功后状态为 `DELETED`，只做软删除。

#### 报名名单和参与确认

`GET /api/v1/activity/admin/events/{activityId}/registrations` 返回报名分页，支持 `status`、`keyword`、`memberGroup`、`page`、`pageSize` 和 `sort`。

`PATCH /api/v1/activity/admin/registrations/{registrationId}/confirm` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，只允许 `SUBMITTED` 或 `WAITLISTED`。确认时必须再次检查名额。名额不足返回 `49613`。

`PATCH /api/v1/activity/admin/registrations/{registrationId}/reject` 请求字段同确认，使 `SUBMITTED` 或 `WAITLISTED` 进入 `REJECTED`。

`PATCH /api/v1/activity/admin/registrations/{registrationId}/promote` 请求字段为 `reviewComment`、`reason`、`idempotencyKey`，只允许 `WAITLISTED` 且名额可用。成功后进入 `CONFIRMED`，保留原候补排序。

`PATCH /api/v1/activity/admin/registrations/{registrationId}/cancel` 请求字段为 `publicReason`、`reason`、`idempotencyKey`，后台可取消 `SUBMITTED`、`CONFIRMED` 或 `WAITLISTED`。

`PATCH /api/v1/activity/admin/registrations/{registrationId}/check-in` 请求字段为 `method`、`checkInCode`、`reason`、`idempotencyKey`，只允许 `CONFIRMED` 报名。签到窗口默认从活动开始前 1 小时到活动结束后 24 小时，超出窗口返回 `49618`。重复签到返回幂等成功，不重复计数。

`PATCH /api/v1/activity/admin/registrations/{registrationId}/no-show` 请求字段为 `publicReason`、`reason`、`idempotencyKey`，只允许 `CONFIRMED` 或按实现固定是否允许 `CHECKED_IN` 修正。默认只允许从 `CONFIRMED` 进入 `NO_SHOW`。

#### 活动结果和奖励

`PUT /api/v1/activity/admin/events/{activityId}/result` 创建或修改结果。请求字段为 `title`、`summary`、`details`、`reason` 和 `idempotencyKey`。只允许 `COMPLETED` 或 `RESULT_PUBLISHED` 活动。首次创建结果状态为 `DRAFT`。

`PATCH /api/v1/activity/admin/events/{activityId}/result/publish` 请求字段为 `reason` 和 `idempotencyKey`。成功后结果状态为 `PUBLISHED`，活动状态进入 `RESULT_PUBLISHED`，可触发通知。重复发布返回幂等成功。

`POST /api/v1/activity/admin/events/{activityId}/rewards` 创建奖励。请求字段为 `registrationId`、`type`、`title`、`description`、`quantity`、`scoreCandidateDelta`、`reason` 和 `idempotencyKey`。只允许给 `CHECKED_IN` 或后台明确允许的 `CONFIRMED` 报名创建奖励。成功响应 HTTP `201`，状态为 `PENDING_ISSUE` 或 `DRAFT`，实现必须固定并测试。

`PATCH /api/v1/activity/admin/rewards/{rewardId}/issue` 请求字段为 `publicComment`、`reason`、`idempotencyKey`，使 `PENDING_ISSUE` 进入 `ISSUED`。通知失败不回滚主状态。

`PATCH /api/v1/activity/admin/rewards/{rewardId}/revoke` 是 `HIGH` 风险，请求字段为 `publicReason`、`reason`、`confirmText`、`idempotencyKey`，P1 固定要求 `REVOKE_ACTIVITY_REWARD`。成功后状态为 `REVOKED`。P1 如已生成贡献候选，只把候选标记为 `SKIPPED` 或保留可复核状态，不调用 attendance 撤销。

`POST /api/v1/activity/admin/events/{activityId}/contribution-candidates` 请求字段为 `reason` 和 `idempotencyKey`。只允许 `RESULT_PUBLISHED` 活动。成功响应 HTTP `201`，`data` 包含生成的 `ActivityContributionCandidate[]`。P1 候选状态默认为 `PENDING` 或 `SKIPPED`，不得直接写 attendance 积分。

#### 审计和自检

`GET /api/v1/activity/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`activityId`、`result`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为 `ActivityAuditLog[]`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 activity API 删除。

`GET /api/v1/activity/admin/ops/summary` 成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "activity",
    "port": 8132,
    "legacyPort": 8113,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "profileMode": "TEST_STUB",
    "notificationMode": "TEST_STUB",
    "attendanceMode": "SKIPPED",
    "communityMode": "TEST_STUB",
    "contentMode": "TEST_STUB",
    "resourceMode": "TEST_STUB",
    "testControlsEnabled": false,
    "activitiesTotal": 8,
    "publishedActivitiesTotal": 3,
    "openRegistrationsTotal": 20,
    "waitlistedRegistrationsTotal": 2,
    "checkedInRegistrationsTotal": 12,
    "resultsPublishedTotal": 1,
    "rewardsTotal": 6,
    "contributionCandidatesTotal": 6,
    "auditsTotal": 40,
    "idempotencyRecordsTotal": 16,
    "lastAuditAt": "2026-05-24T12:00:00Z",
    "productionGaps": [
      "P1_IN_MEMORY_STORAGE",
      "P1_AUTH_STUB",
      "P1_PROFILE_STUB",
      "P1_NOTIFICATION_STUB",
      "P1_COMMUNITY_STUB",
      "P1_CONTENT_STUB",
      "P1_RESOURCE_STUB",
      "ATTENDANCE_CONTRIBUTION_NOT_CONNECTED",
      "CALENDAR_NOT_CONNECTED",
      "TEST_CONTROLS_DISABLED_OUTSIDE_TEST"
    ]
  }
}
```

### 状态、幂等和并发

活动创建后为 `DRAFT`。`DRAFT` 可提交为 `PENDING_REVIEW`。`PENDING_REVIEW` 可通过为 `APPROVED`、拒绝为 `REJECTED`、要求修改为 `NEEDS_CHANGES`。`NEEDS_CHANGES` 和 `REJECTED` 可修改后再次提交。`APPROVED` 可发布为 `PUBLISHED` 或 `REGISTRATION_OPEN`。`PUBLISHED` 可开放报名，`REGISTRATION_OPEN` 可关闭报名或进入进行中，`REGISTRATION_CLOSED` 可进入进行中，`RUNNING` 可完成，`COMPLETED` 可发布结果。`RESULT_PUBLISHED`、`OFFLINE` 和 `ARCHIVED` 不得直接回到公开报名状态。`DELETED` 为软删除终态。

报名在 `OPEN` 策略下默认进入 `CONFIRMED`，在 `APPROVAL_REQUIRED` 下进入 `SUBMITTED`，在名额满且候补开启时进入 `WAITLISTED`。`SUBMITTED` 可确认或拒绝。`WAITLISTED` 可转正、拒绝或取消。`CONFIRMED` 可签到、缺席或取消。`CHECKED_IN` 和 `NO_SHOW` 是参与事实状态，除非后台修正接口未来补充契约，否则不得回到报名状态。

写接口支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `49617`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序。

后台活动修改、审核、发布、报名管理、签到、结果、奖励、撤销和贡献候选生成均属于写接口，P1 内存实现也必须使用同一套幂等记录机制，不能只校验 `idempotencyKey` 长度。重复提交已经完成的状态流转时，如果操作者、接口语义、幂等键和请求体一致，必须 replay 首次结果；如果请求体不同，必须返回 `49617` 且业务状态不变。

活动创建和活动修改必须使用同一套字段校验规则。`capacity` 必须大于等于 1，`waitlistCapacity` 必须大于等于 0，`registrationOpenAt` 不得晚于 `registrationCloseAt`，`registrationCloseAt` 必须早于活动开始时间。修改活动时不得通过局部字段更新绕过这些规则；校验失败必须返回公共字段校验错误，不能写入半更新活动。

并发报名同一活动同一用户只能产生一条有效报名。并发确认报名必须串行检查活动名额，不能出现 `confirmedCount` 超过 `capacity`。并发取消和候补转正不能让同一候补被转正两次。并发签到同一报名只能增加一次 `checkedInCount`。读取接口允许读到更新前或更新后的完整状态，不能返回半更新对象。

P1 内存实现必须用本服务内的串行临界区保护名额、候补、签到、结果、奖励和贡献候选这些共享状态。后续持久化实现必须把这些保护迁移为数据库事务、唯一约束、条件更新或等效机制，不能降低上述并发口径。

### 审计要求

必须审计的动作包括活动创建、活动修改、提交审核、审核通过、审核拒绝、要求修改、发布、开放报名、关闭报名、开始、完成、下架、归档、软删除、报名创建、报名取消、报名确认、报名拒绝、候补转正、签到、缺席、结果创建或修改、结果发布、奖励创建、奖励发放、奖励撤销、贡献候选生成、通知失败、依赖降级、自检读取、审计写入失败和状态写入失败。

后台写操作必须记录 `reason`、操作者、目标对象、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计不得泄露 token、完整请求头、Minecraft 验证凭据、profile 后台备注全文、通知正文全文、真实服务器命令、节点凭据、Cloudreve token、内部异常堆栈或前序服务内部路径。

审计写入失败时，活动审核、发布、下架、归档、软删除、报名确认、报名拒绝、候补转正、签到、缺席、结果发布、奖励创建、奖励发放、奖励撤销和贡献候选生成不得假装成功，必须返回 `54601` 或 `54600`，并保持业务数据不变。通知失败不回滚主状态，但必须记录失败摘要和审计。

P1 内存实现允许用本地自动化测试控制头模拟审计失败，但失败检查必须发生在业务状态写入前，或者业务写入和审计写入必须处于同一可回滚临界区。审计失败后，不得新增活动、不得改变状态、不得改变报名计数、不得新增奖励或贡献候选，也不得写入幂等成功记录。

### 失败降级

auth 是所有登录接口强依赖。auth 不可用、超时、用户状态不允许或字段不兼容时不得伪造成功。

profile 是报名、签到、获奖和奖励归属的强依赖。写入时 profile 失败不得伪造成员资料。公开读取时 profile 失败可以使用旧快照降级，但必须标记 stale。

notification 是辅助依赖。报名确认、候补转正、取消、签到、结果发布和奖励发放的通知失败不得回滚活动主状态，但必须保存脱敏失败摘要和审计。

attendance 在 P1 不作为活动主流程依赖。活动贡献候选可以记录为 `PENDING`、`SKIPPED` 或 `FAILED`，但不得直接写积分余额、流水或榜单。

community、content 和 resource 是关联快照辅助依赖。创建或修改关联时，公开快照不可用不得保存伪造关联。读取已存在活动时，来源服务失败可以使用已保存快照降级并标记 stale。

状态写入、报名计数、候补排序、签到计数、奖励状态和贡献候选写入必须保持一致。不能出现报名记录写入失败但名额增加，或签到记录失败但签到人数增加。半成功风险必须返回 `54602`、`54603` 或 `54604` 并保持可复核状态。

### 验收口径

`activity` API 文档按 `docs/contracts-activity.md` 独立存在，并由 `.local-docs/tests-activity.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`activity` 完成时必须满足以下条件：全部接口按本文档实现；公开接口只返回公开可见活动、结果和摘要；当前用户接口只能访问自己的报名、签到和奖励；后台接口按角色限制；活动、报名、签到、结果和奖励有服务端状态机；名额、候补和签到由服务端计数和幂等保证；所有后台写操作和高风险操作有审计；通知失败按辅助降级记录；贡献奖励只生成 activity 贡献候选，不直接写 attendance 积分；当前运行端口固定为 `8132`，自检摘要返回 `port=8132` 和 `legacyPort=8113`；`.local-docs/tests-activity.md` 与 `.local-docs/tests-engagement-core.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 activity 在 `engagement-core-service` 中全部测试通过；auth、profile、notification、content、server-status、resource、admin、onboarding、exam、whitelist、attendance 和 community 前序服务回归测试通过；不恢复 `backend/activity-service` 旧入口；没有修改前序服务稳定接口；没有把日历、更新日志、后台聚合、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复或 Cloudreve 管理能力塞进 activity。

生产化硬化验收还必须满足：测试控制头默认关闭，只有本地自动化测试显式启用时才生效；关闭状态下依赖失败模拟头、写入失败模拟头、时间模拟头和通知失败模拟头全部被忽略；自检摘要明确返回当前测试控制头开关状态。

## 北冥官网 calendar API 契约

来源：`docs/contracts-calendar.md`

版本：0.1

### 文档定位

本文档是 `calendar` 微服务的正式 API 契约。后续 `changelog`、前端适配、`admin` 聚合、`ops-control` 和 `external-node-executor` 只能通过本文档定义的接口读取或管理日程事件、关注、提醒摘要、来源同步、审计和自检摘要，不能直接读取或修改 `calendar` 数据库，也不能把活动报名、社区投票、更新日志主数据或真实服务器运维能力塞进 `calendar`。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `calendar` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了成熟日历和社区日程系统的公开设计。Google Calendar API 的事件资源说明事件时间、时区、状态、可见性、提醒和扩展属性应结构化保存。Microsoft Graph Calendar API 的 event 与 calendarView 说明事件详情和时间范围视图应分开。Discord Guild Scheduled Event 的状态、实体类型和隐私等级说明社区日程要有明确状态和可见性。CalDAV RFC 4791 的 calendar-query 和 time-range 说明时间窗口查询是日历服务基础能力。FullCalendar 的 Event Object 和 JSON feed 思路说明前端日历需要稳定的 `start`、`end`、`allDay` 和来源字段。Nextcloud Calendar 作为开源日历项目说明 CalDAV、提醒和共享是完整日历系统常见方向，但本轮只实现北冥官网 P1 所需的 HTTP 契约、关注和提醒摘要，不实现 CalDAV、ICS 导入导出、递归规则、会议邀请和外部账号同步。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Google Calendar API Events](https://developers.google.com/calendar/api/v3/reference/events) | 事件时间、时区、状态、可见性、提醒和来源字段需要分开维护。 |
| [Microsoft Graph event resource](https://learn.microsoft.com/en-us/graph/api/resources/event) | 事件详情和日历视图查询应分离，时间窗口读取不能依赖前端过滤。 |
| [Discord Guild Scheduled Event](https://discord.com/developers/docs/resources/guild-scheduled-event) | 社区日程需要状态、开始结束时间、隐私等级和实体类型。 |
| [CalDAV RFC 4791](https://www.rfc-editor.org/rfc/rfc4791) | 时间范围查询和集合访问是日历服务基础能力。 |
| [FullCalendar Event Object](https://fullcalendar.io/docs/event-object) | 前端日历视图需要稳定的 start、end、allDay、id 和 extendedProps。 |
| [Nextcloud Calendar](https://github.com/nextcloud/calendar) | 开源日历项目通常把事件、提醒、共享和 CalDAV 扩展分层，本轮只借鉴边界。 |

### 职责边界

`calendar` 负责日程事件、维护窗口、工程节点、投票截止、版本更新时间、服务器日程、时间线视图、当前用户关注、提醒摘要、来源同步快照、依赖摘要、幂等记录、calendar 审计和自检摘要。

`calendar` 不负责活动报名、活动结果、活动奖励、站内通知主数据、官网内容主发布、资源下载主数据、社区投票主数据、考勤积分、真实服务器运维、容器、终端、文件管理、备份、节点守护、Cloudreve 管理或更新日志主数据。

`activity` 已经稳定，`calendar` 只能通过 `GET /api/v1/activity/calendar-summary` 读取活动时间摘要，并保存本服务自己的导入快照、来源引用和同步状态。`calendar` 不能读取 `activity` 内部类、测试种子、内存存储或未来数据库，不能修改活动状态、报名和结果。

`notification` 是提醒投递的未来正式来源。P1 中 `calendar` 只保存提醒摘要和投递意图，不创建 notification 主数据、不维护未读数、不写通知模板。后续需要真实投递时，必须通过 `notification` 正式接口适配。

`changelog` 已经由 `engagement-core-service` 承载。本轮 `calendar` 可以保存 `VERSION_RELEASE` 手工事件和 changelog 来源占位，自检摘要中必须暴露 `CHANGELOG_NOT_CONNECTED`，直到 calendar 与 changelog 的正式写入适配单独完成闭环；calendar 不能把版本更新日志正文、插件变更、规则调整和地图更新主数据塞进自己。

维护窗口在本模块只是日程元数据。任何真实服务器启动、停止、重启、命令执行、日志流、文件管理、备份恢复和节点操作都属于后续 `ops-control` 与 `external-node-executor`。

### 数据归属

`calendar` 拥有以下主数据：日程事件、事件版本摘要、事件来源引用、事件来源同步快照、用户关注记录、提醒策略摘要、提醒投递摘要、依赖调用摘要、幂等记录、calendar 审计日志和自检统计。

`calendar` 可以保存来自 `auth` 的 `userId`、`displayName`、`roles`、`permissions` 和用户状态快照；可以保存来自 `activity` 的 `activityId`、`slug`、`title`、`type`、`visibility`、`status`、`startAt`、`endAt`、`registrationCloseAt` 和 `summary` 快照；可以保存 future `changelog` 的来源 ID 和占位同步状态。快照只服务展示、检索、提醒和审计，不能成为来源模块主数据，也不能反写来源模块。

### 基础路径与认证

所有接口默认使用 `/api/v1/calendar` 前缀。当前运行入口为 `engagement-core-service:8132`。历史原服务端口 `8114` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或回归测试命令。

公开接口允许游客读取 `PUBLISHED` 且符合可见范围的事件。公开接口不得返回内部备注、审核参数、管理员 ID、提醒失败详情、完整依赖错误、审计参数、幂等键或来源模块内部路径。

当前用户接口使用 `/api/v1/calendar/me` 前缀，全部要求 `Authorization: Bearer <token>`。当前用户只能读取和维护自己的关注记录、提醒偏好和关注列表。浏览器请求体不得传入 `userId`、`roles`、`permissions`、`watchCount`、`status`、`notificationStatus`、`operatorUserId`、`reviewerUserId`、`auditResult`、`sourceVersion` 等服务端可信字段。

后台接口使用 `/api/v1/calendar/admin` 前缀，全部要求登录。后台读取事件、同步摘要、自检摘要和审计列表要求 `HELPER`、`ADMIN` 或 `OWNER`，但审计列表只允许 `ADMIN` 或 `OWNER`。事件创建、修改、提交、审核、发布、下架、归档、软删除和 activity 同步要求 `HELPER`、`ADMIN` 或 `OWNER`，但 `HELPER` 只能创建草稿、修改自己创建的未发布事件、提交审核和执行被授权的初审。发布、下架、归档、软删除、来源同步和审计读取要求 `ADMIN` 或 `OWNER`。

P1 契约补强要求：当前用户关注列表、后台事件列表和审计列表必须完整实现本文档列出的过滤参数，不能只返回未过滤全量数据。后台手工创建事件时，P1 只接受 `MANUAL` 和未来 `CHANGELOG` 占位来源；`ACTIVITY` 只能通过 `/api/v1/calendar/admin/sync/activity` 导入，`COMMUNITY_POLL` 和 `OPS_PLACEHOLDER` 只作为未来来源枚举保留，直接创建必须返回字段校验错误。`HELPER` 修改事件时必须限制为自己创建且未发布的事件，不能修改其他后台人员创建的事件。

### 本地测试控制头

`calendar` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Activity-Mode`、`X-Test-Notification-Mode`、`X-Test-Changelog-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store`、`X-Test-Fail-Watch` 和 `X-Test-Now` 模拟依赖失败、通知失败、写入失败、关注并发冲突和时间边界。该能力只服务本地测试闭环，不属于正式业务 API。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、状态失败、关注失败、通知失败、时间模拟或快照 stale。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 视为已满足的生产化硬化项。

### 前序服务兼容契约

`auth` 是所有登录接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions`、`status` 和可选 `minecraftBinding`。用户状态为 `ACTIVE` 时可关注和后台写入；`PENDING_PROFILE` 可以关注公开事件但不能创建后台事件；`DISABLED`、`BANNED`、`DELETED` 不允许写入。auth 不可用返回 `49800`，auth 超时返回 `49801`，字段或枚举不兼容返回 `49802`。

`activity` 是活动日程只读来源。activity 同步只能读取 `GET /api/v1/activity/calendar-summary`。activity 不可用返回 `49810`，超时返回 `49811`，字段或枚举不兼容返回 `49812`。后台同步失败不得删除已有 calendar 事件，只能记录同步失败摘要。公开读取可继续展示已导入快照，并标记 `sourceSnapshotStale=true`。

`notification` 是提醒辅助依赖。关注事件、事件发布时间变更、维护窗口发布和事件下架可以形成提醒摘要。P1 不真实投递通知，只记录 `notificationStatus=SKIPPED` 或在测试控制下记录 `FAILED` 脱敏摘要。notification 不可用记录或返回 `49820`，超时记录或返回 `49821`，字段不兼容记录或返回 `49822`。通知失败不得回滚事件主状态或关注状态。

`changelog` 是未来版本更新来源。本轮未连接时必须在自检摘要返回 `CHANGELOG_NOT_CONNECTED`。如果测试控制头模拟 changelog 不可用，手工 `VERSION_RELEASE` 事件仍可创建，来源同步只返回降级摘要，不得创建 changelog 主数据。changelog 不可用记录 `49830`，超时记录 `49831`，字段不兼容记录 `49832`。

`admin` 当前只是聚合入口。本文档不修改 `admin` 契约。若后续需要 admin 展示 calendar 待办、指标或审计索引，必须作为 admin 兼容变更单独走契约、测试、红灯、实现和回归闭环。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `CalendarEventType` | `ACTIVITY`、`MAINTENANCE`、`ENGINEERING_MILESTONE`、`VOTE_DEADLINE`、`VERSION_RELEASE`、`SERVER_SCHEDULE` | 日程事件类型。 |
| `CalendarEventStatus` | `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`PUBLISHED`、`OFFLINE`、`ARCHIVED`、`DELETED` | 事件主状态。公开查询只返回 `PUBLISHED`。 |
| `CalendarVisibility` | `PUBLIC`、`MEMBER_ONLY`、`STAFF_ONLY` | 事件可见范围。 |
| `CalendarSourceType` | `MANUAL`、`ACTIVITY`、`CHANGELOG`、`COMMUNITY_POLL`、`OPS_PLACEHOLDER` | 来源类型。P1 只真实支持 `MANUAL` 和 `ACTIVITY`。 |
| `CalendarReminderChannel` | `IN_APP`、`EMAIL`、`QQ`、`OOPZ`、`GAME` | 提醒渠道。P1 只保存摘要，不真实投递外部渠道。 |
| `CalendarReminderStatus` | `SKIPPED`、`PENDING`、`DELIVERED`、`FAILED` | 最近一次提醒摘要。P1 默认 `SKIPPED`。 |
| `CalendarSyncStatus` | `NOT_CONFIGURED`、`SYNCED`、`FAILED`、`STALE`、`SKIPPED` | 来源同步状态。 |
| `CalendarAuditResult` | `SUCCESS`、`FAILED` | calendar 审计结果。 |

### 通用对象

#### CalendarReminderPolicy

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `enabled` | boolean | 是 | 是否启用提醒摘要。 |
| `offsetMinutes` | integer[] | 是 | 提前提醒分钟数，0 到 10080，最多 5 个。 |
| `channels` | string[] | 是 | `CalendarReminderChannel`。P1 只能保存 `IN_APP`。 |
| `lastReminderStatus` | string | 是 | `CalendarReminderStatus`。 |
| `lastReminderAt` | string 或 null | 是 | 最近提醒摘要时间。 |
| `failure` | CalendarReminderFailureSummary 或 null | 是 | 脱敏失败摘要。 |

#### CalendarReminderFailureSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | 固定为 `FAILED`。 |
| `failureCode` | string | 是 | `49820`、`49821` 或 `49822`。 |
| `failureType` | string | 是 | `UNAVAILABLE`、`TIMEOUT` 或 `BAD_SCHEMA`。 |
| `failureReason` | string | 是 | 脱敏失败摘要。 |
| `failedAt` | string | 是 | 失败发生时间。 |

#### CalendarSourceRef

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sourceType` | string | 是 | `CalendarSourceType`。 |
| `sourceId` | string 或 null | 是 | 来源业务 ID。 |
| `sourceVersion` | string 或 null | 是 | 来源版本或更新时间摘要。 |
| `sourceUrl` | string 或 null | 是 | 来源公开或后台跳转地址。 |
| `syncStatus` | string | 是 | `CalendarSyncStatus`。 |
| `sourceSnapshotStale` | boolean | 是 | 是否使用旧来源快照。 |
| `lastSyncedAt` | string 或 null | 是 | 最近同步时间。 |
| `failure` | object 或 null | 是 | 脱敏同步失败摘要，不包含 token、内部路径和堆栈。 |

#### CalendarEvent

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `eventId` | string | 是 | 事件 ID。 |
| `source` | CalendarSourceRef | 是 | 来源引用。 |
| `title` | string | 是 | 2 到 100 位。 |
| `summary` | string | 是 | 1 到 300 位。 |
| `description` | string | 否 | 最多 5000 位，公开详情可见。 |
| `type` | string | 是 | `CalendarEventType`。 |
| `status` | string | 是 | `CalendarEventStatus`。 |
| `visibility` | string | 是 | `CalendarVisibility`。 |
| `startAt` | string | 是 | ISO 8601，事件开始时间。 |
| `endAt` | string | 是 | ISO 8601，必须晚于 `startAt`。全天事件由服务端写入完整 UTC 范围。 |
| `timezone` | string | 是 | IANA 时区，P1 默认 `Asia/Shanghai`。 |
| `allDay` | boolean | 是 | 是否全天事件。 |
| `location` | string 或 null | 是 | 地点或线上说明，最多 200 位。 |
| `relatedUrl` | string 或 null | 是 | http、https 或站内路径，最多 500 位。 |
| `labels` | string[] | 是 | 0 到 8 个标签，每个 1 到 24 位。 |
| `priority` | integer | 是 | 0 到 100，越大越靠前。 |
| `watchCount` | integer | 是 | 当前关注人数。 |
| `watchedByCurrentUser` | boolean | 当前用户接口或可选登录视图 | 当前用户是否关注。 |
| `reminderPolicy` | CalendarReminderPolicy | 是 | 提醒摘要。 |
| `createdBy` | string | 后台可见 | 创建者用户 ID。 |
| `updatedBy` | string | 后台可见 | 最近修改者用户 ID。 |
| `reviewedBy` | string 或 null | 后台可见 | 审核者用户 ID。 |
| `reviewComment` | string 或 null | 是 | 给创建者的审核意见。公开接口仅可返回可展示摘要。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注，公开和当前用户接口不得返回。 |
| `submittedAt` | string 或 null | 是 | 提交审核时间。 |
| `reviewedAt` | string 或 null | 是 | 审核时间。 |
| `publishedAt` | string 或 null | 是 | 发布时间。 |
| `offlineAt` | string 或 null | 是 | 下架时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |
| `deletedAt` | string 或 null | 后台可见 | 软删除时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `lastSyncedAt` | string 或 null | 是 | 最近来源同步时间。 |

#### CalendarWatch

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `watchId` | string | 是 | 关注记录 ID。 |
| `eventId` | string | 是 | 事件 ID。 |
| `userId` | string | 是 | auth 用户 ID。 |
| `displayNameSnapshot` | string | 是 | 用户展示名快照。 |
| `reminderEnabled` | boolean | 是 | 当前用户是否启用提醒摘要。 |
| `reminderOffsets` | integer[] | 是 | 当前用户提醒分钟数。 |
| `status` | string | 是 | P1 固定为 `ACTIVE` 或 `CANCELED`。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `canceledAt` | string 或 null | 是 | 取消关注时间。 |

#### CalendarAuditLog

审计字段继承公共契约，允许补充 `eventId`、`watchId`、`sourceType`、`sourceId`、`stateFrom`、`stateTo`、`idempotencyKey`、`syncStatus`、`reminderStatus`、`dependencyStatus` 和 `sourceSnapshotStale`。审计日志不得通过 calendar API 删除。

### calendar 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `49900` | 404 | 日程事件不存在，或公开接口无权访问。 |
| `49901` | 404 | 关注记录不存在，或当前用户无权访问。 |
| `49902` | 404 | 来源快照不存在。 |
| `49910` | 409 | 事件状态不允许当前操作。 |
| `49911` | 409 | 事件时间范围冲突。 |
| `49912` | 409 | 同一来源事件已存在。 |
| `49913` | 409 | 事件关注状态冲突。 |
| `49914` | 409 | 幂等键请求指纹冲突。 |
| `49915` | 409 | 提醒策略不允许当前操作。 |
| `49916` | 409 | 同一用户已关注该事件。 |
| `49800` | 502 | auth 认证上下文不可用。 |
| `49801` | 504 | auth 认证上下文超时。 |
| `49802` | 502 | auth 认证上下文字段或枚举不兼容。 |
| `49810` | 502 | activity 日历摘要不可用。 |
| `49811` | 504 | activity 日历摘要调用超时。 |
| `49812` | 502 | activity 日历摘要字段或枚举不兼容。 |
| `49820` | 502 | notification 提醒依赖不可用。 |
| `49821` | 504 | notification 提醒依赖超时。 |
| `49822` | 502 | notification 提醒依赖字段或枚举不兼容。 |
| `49830` | 502 | changelog 来源不可用或未连接。 |
| `49831` | 504 | changelog 来源超时。 |
| `49832` | 502 | changelog 来源字段或枚举不兼容。 |
| `54800` | 500 | calendar 内部错误。 |
| `54801` | 500 | calendar 审计写入失败。 |
| `54802` | 500 | calendar 状态写入失败。 |
| `54803` | 500 | calendar 关注写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误、通用幂等键冲突和通用服务端错误优先使用公共错误码。calendar 自有幂等指纹冲突使用 `49914`。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开事件列表 | GET | `/api/v1/calendar/events` | 否 | 无 | LOW |
| 公开事件详情 | GET | `/api/v1/calendar/events/{eventId}` | 否 | 无 | LOW |
| 月视图 | GET | `/api/v1/calendar/month` | 否 | 无 | LOW |
| 即将开始 | GET | `/api/v1/calendar/upcoming` | 否 | 无 | LOW |
| 我的关注列表 | GET | `/api/v1/calendar/me/watchlist` | 是 | 当前用户 | LOW |
| 关注事件 | POST | `/api/v1/calendar/me/events/{eventId}/watch` | 是 | 当前用户 | LOW |
| 取消关注事件 | POST | `/api/v1/calendar/me/events/{eventId}/unwatch` | 是 | 当前用户 | LOW |
| 后台事件列表 | GET | `/api/v1/calendar/admin/events` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 后台事件详情 | GET | `/api/v1/calendar/admin/events/{eventId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 创建事件 | POST | `/api/v1/calendar/admin/events` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 修改事件 | PATCH | `/api/v1/calendar/admin/events/{eventId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 提交审核 | POST | `/api/v1/calendar/admin/events/{eventId}/submit` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核通过 | PATCH | `/api/v1/calendar/admin/events/{eventId}/approve` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/calendar/admin/events/{eventId}/reject` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 发布事件 | PATCH | `/api/v1/calendar/admin/events/{eventId}/publish` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 下架事件 | PATCH | `/api/v1/calendar/admin/events/{eventId}/offline` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 归档事件 | PATCH | `/api/v1/calendar/admin/events/{eventId}/archive` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 软删除事件 | PATCH | `/api/v1/calendar/admin/events/{eventId}/delete` | 是 | `ADMIN`、`OWNER` | HIGH |
| 同步 activity 摘要 | POST | `/api/v1/calendar/admin/sync/activity` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 审计列表 | GET | `/api/v1/calendar/admin/audit-logs` | 是 | `ADMIN`、`OWNER` | LOW |
| calendar 自检摘要 | GET | `/api/v1/calendar/admin/ops/summary` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |

### 公开接口

#### 公开事件列表

`GET /api/v1/calendar/events`

查询参数：`page`、`pageSize`、`keyword`、`type`、`visibility`、`from`、`to`、`label`、`sourceType` 和 `sort`。`pageSize` 最大 `100`。`sort` 允许 `startAt_asc`、`startAt_desc`、`priority_desc`、`publishedAt_desc`、`updatedAt_desc`。

成功响应 HTTP `200`，分页 `items` 为公开视图 `CalendarEvent[]`。

业务规则：游客只看到 `PUBLISHED` 且 `visibility=PUBLIC` 的事件。登录用户的成员和工作人员可见范围可以由后续前端或网关适配，但 P1 公开接口默认不读取登录态扩展可见性，避免游客和登录视图混淆。列表不得返回内部备注、后台审核字段、通知失败详情、审计字段和未发布来源失败。

时间范围规则：`from` 和 `to` 使用 ISO 8601。事件只要与查询范围重叠就必须返回，条件为 `event.endAt > from && event.startAt < to`。跨月事件必须出现在涉及月份。全天事件按服务端保存的完整时间范围参与重叠判断。

#### 公开事件详情

`GET /api/v1/calendar/events/{eventId}`

成功响应 HTTP `200`，`data` 为公开视图 `CalendarEvent`。事件不存在、不可见、已下架、已归档或已删除时返回 `49900`。公开详情不得返回内部备注、操作者 ID、审计参数、完整依赖失败或来源内部路径。

#### 月视图

`GET /api/v1/calendar/month`

查询参数：`month` 必填，格式 `YYYY-MM`；可选 `type`、`visibility` 和 `sourceType`。成功响应 HTTP `200`，`data` 包含 `month`、`timezone`、`rangeStart`、`rangeEnd`、`items` 和 `degraded`。`items` 为与该月重叠的公开事件摘要。`rangeStart` 和 `rangeEnd` 由服务端按 `Asia/Shanghai` 计算，不能要求前端自己拼时间。

#### 即将开始

`GET /api/v1/calendar/upcoming`

查询参数：`limit` 默认 `10`，最大 `50`；可选 `from`、`days`、`type` 和 `sourceType`。成功响应 HTTP `200`，`data.items` 为从服务端当前时间或 `from` 开始的未来公开事件摘要。`days` 默认 `30`，最大 `180`。同一开始时间按 `priority` 和 `eventId` 稳定排序。

### 当前用户接口

#### 我的关注列表

`GET /api/v1/calendar/me/watchlist`

查询参数：`page`、`pageSize`、`status`、`type`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为当前用户关注记录与事件摘要。只能返回当前认证用户自己的关注，不得通过请求参数传入 `userId`。

过滤规则：`status` 只允许 `ACTIVE` 和 `CANCELED`；`type` 按事件类型过滤；`from` 和 `to` 使用事件时间范围重叠规则；`sort` 允许 `updatedAt_desc`、`createdAt_desc` 和 `startAt_asc`。非法 `status` 返回 `40001`，非法 `sort` 返回 `40003`，非法时间返回 `40001`，非法范围返回 `49911`。

#### 关注事件

`POST /api/v1/calendar/me/events/{eventId}/watch`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reminderEnabled` | boolean | 否 | 默认 `true`。 |
| `reminderOffsets` | integer[] | 否 | 默认 `[60]`，最多 5 个，范围 0 到 10080。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `201` 或重复关注幂等 HTTP `200`，`data` 为 `CalendarWatch` 和事件摘要。只允许关注公开可见、未下架、未归档、未删除的事件。并发关注同一用户同一事件只能产生一条有效记录。相同幂等键同请求体重复提交返回同一结果，相同键不同体返回 `49914`。

#### 取消关注事件

`POST /api/v1/calendar/me/events/{eventId}/unwatch`

请求字段：`reason` 可选，最多 200 位；`idempotencyKey` 可选。成功响应 HTTP `200`，`data` 为取消后的关注摘要。未关注时返回幂等成功，不能把 `watchCount` 扣成负数。取消关注不删除历史记录，只标记为 `CANCELED`。

### 后台事件接口

#### 后台事件列表和详情

`GET /api/v1/calendar/admin/events` 支持 `page`、`pageSize`、`keyword`、`type`、`status`、`visibility`、`sourceType`、`createdBy`、`from`、`to` 和 `sort`。后台可查看全部非物理删除事件，默认按 `updatedAt_desc`。`GET /api/v1/calendar/admin/events/{eventId}` 返回事件、关注统计、来源同步摘要、提醒摘要、最近审计和依赖摘要。响应不得返回 token、完整请求头、通知正文、前序服务内部路径、异常堆栈、真实服务器命令、节点凭据或 Cloudreve token。

后台事件列表过滤规则：`keyword` 匹配标题或摘要；`type`、`status`、`visibility`、`sourceType` 和 `createdBy` 精确匹配；`from` 和 `to` 使用事件时间范围重叠规则；非法枚举返回 `40001`；非法时间返回 `40001`；非法范围返回 `49911`。分页和排序继续遵守公共契约。

#### 创建事件

`POST /api/v1/calendar/admin/events`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `title` | string | 是 | 2 到 100 位。 |
| `summary` | string | 是 | 1 到 300 位。 |
| `description` | string | 否 | 最多 5000 位。 |
| `type` | string | 是 | `CalendarEventType`。 |
| `visibility` | string | 是 | `CalendarVisibility`。 |
| `startAt` | string | 是 | ISO 8601。 |
| `endAt` | string | 是 | ISO 8601，必须晚于 `startAt`。 |
| `timezone` | string | 否 | 默认 `Asia/Shanghai`。 |
| `allDay` | boolean | 否 | 默认 `false`。 |
| `location` | string 或 null | 否 | 最多 200 位。 |
| `relatedUrl` | string 或 null | 否 | http、https 或站内路径。 |
| `labels` | string[] | 否 | 最多 8 个。 |
| `priority` | integer | 否 | 0 到 100，默认 50。 |
| `reminderPolicy` | object | 否 | 按 `CalendarReminderPolicy` 校验。 |
| `sourceType` | string | 否 | 默认 `MANUAL`。P1 后台创建只允许 `MANUAL` 和未来占位 `CHANGELOG`。 |
| `sourceId` | string 或 null | 否 | 来源 ID。`MANUAL` 可为空。 |
| `internalNote` | string 或 null | 否 | 后台备注，最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `201`，状态为 `DRAFT`。同一 `sourceType` 和 `sourceId` 的非终态事件不得重复创建，返回 `49912`。`type=MAINTENANCE` 和 `SERVER_SCHEDULE` 只能保存日程元数据，不得触发服务器操作。

P1 来源创建限制：后台创建接口传入 `ACTIVITY`、`COMMUNITY_POLL` 或 `OPS_PLACEHOLDER` 时返回 `40001`，提示来源只能由对应来源同步或后续模块适配产生。`CHANGELOG` 只允许 `type=VERSION_RELEASE`，并且必须带 `sourceId`，不得保存 changelog 正文主数据。

#### 修改事件

`PATCH /api/v1/calendar/admin/events/{eventId}`

请求字段同创建事件，除 `reason` 必填外其余字段按需修改。只允许 `DRAFT`、`NEEDS_CHANGES`、`REJECTED` 和未发布的 `APPROVED` 修改主体字段。`PUBLISHED` 事件如需改时间，P1 必须先下架后修改再发布，避免用户关注和提醒失真。非法状态返回 `49910`。

#### 审核发布状态

`POST /api/v1/calendar/admin/events/{eventId}/submit` 使 `DRAFT`、`NEEDS_CHANGES` 或 `REJECTED` 进入 `PENDING_REVIEW`。

`PATCH /api/v1/calendar/admin/events/{eventId}/approve` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，使 `PENDING_REVIEW` 或 `NEEDS_CHANGES` 进入 `APPROVED`。

`PATCH /api/v1/calendar/admin/events/{eventId}/reject` 请求字段同审核通过，使 `PENDING_REVIEW` 进入 `REJECTED`。

`PATCH /api/v1/calendar/admin/events/{eventId}/publish` 请求字段为 `reason`、`idempotencyKey`，使 `APPROVED` 或 `OFFLINE` 进入 `PUBLISHED`，写入 `publishedAt`。辅助通知失败不回滚主状态，但必须保存提醒失败摘要和审计。

`PATCH /api/v1/calendar/admin/events/{eventId}/offline` 请求字段为 `publicReason`、`reason`、`idempotencyKey`，使 `PUBLISHED` 进入 `OFFLINE`。下架后公开接口不可见，但关注记录保留。

`PATCH /api/v1/calendar/admin/events/{eventId}/archive` 请求字段为 `reason`、`idempotencyKey`，使 `DRAFT`、`REJECTED`、`NEEDS_CHANGES` 或 `OFFLINE` 进入 `ARCHIVED`。已发布事件必须先下架再归档。

`PATCH /api/v1/calendar/admin/events/{eventId}/delete` 是 `HIGH` 风险，请求字段为 `reason`、`confirmText`、`idempotencyKey`，P1 固定要求 `DELETE_CALENDAR_EVENT`。成功后状态为 `DELETED`，只做软删除，不物理删除事件、关注记录和审计线索。

#### 同步 activity 摘要

`POST /api/v1/calendar/admin/sync/activity`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `from` | string | 是 | ISO 8601 起始时间。 |
| `to` | string | 是 | ISO 8601 结束时间，必须晚于 `from`。 |
| `mode` | string | 否 | `UPSERT_SNAPSHOT` 或 `DRY_RUN`，默认 `UPSERT_SNAPSHOT`。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 包含 `syncStatus`、`createdTotal`、`updatedTotal`、`skippedTotal`、`failedTotal`、`items`、`activityMode` 和 `lastSyncedAt`。`DRY_RUN` 只返回差异，不写事件、不写关注、不改变来源状态。`UPSERT_SNAPSHOT` 按 `sourceType=ACTIVITY` 和 `sourceId=activityId` 创建或更新 calendar 事件快照，状态默认 `PUBLISHED`，但不得修改 activity 主数据。

失败降级：activity 不可用时返回 `49810` 或在实现固定策略下返回 `syncStatus=FAILED` 的成功响应；同一版本内必须固定并测试。无论哪种策略，失败不得删除已有 activity 来源事件。

### 审计和自检

`GET /api/v1/calendar/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`eventId`、`sourceType`、`result`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为 `CalendarAuditLog[]`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 calendar API 删除。

审计列表过滤规则：`actorUserId`、`action`、`targetType`、`targetId`、`eventId`、`sourceType` 和 `result` 精确匹配；`from` 和 `to` 按审计 `createdAt` 范围过滤；`sort` 允许 `createdAt_desc` 和 `createdAt_asc`。非法 `result` 返回 `40001`，非法 `sort` 返回 `40003`，非法时间返回 `40001`，非法范围返回 `49911`。

`GET /api/v1/calendar/admin/ops/summary` 成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "calendar",
    "port": 8132,
    "legacyPort": 8114,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "activityMode": "TEST_STUB",
    "notificationMode": "SKIPPED",
    "changelogMode": "NOT_CONNECTED",
    "testControlsEnabled": false,
    "eventsTotal": 8,
    "publishedEventsTotal": 4,
    "watchesTotal": 6,
    "activitySourceEventsTotal": 2,
    "manualEventsTotal": 6,
    "auditsTotal": 20,
    "idempotencyRecordsTotal": 12,
    "lastActivitySyncAt": "2026-05-25T12:00:00Z",
    "lastAuditAt": "2026-05-25T12:05:00Z",
    "productionGaps": [
      "P1_IN_MEMORY_STORAGE",
      "P1_AUTH_STUB",
      "P1_ACTIVITY_STUB",
      "NOTIFICATION_DELIVERY_NOT_CONNECTED",
      "CHANGELOG_NOT_CONNECTED",
      "TEST_CONTROLS_DISABLED_OUTSIDE_TEST"
    ]
  }
}
```

### 状态、幂等和并发

事件创建后为 `DRAFT`。`DRAFT` 可提交为 `PENDING_REVIEW`。`PENDING_REVIEW` 可通过为 `APPROVED`、拒绝为 `REJECTED`。`NEEDS_CHANGES` 和 `REJECTED` 可修改后再次提交。`APPROVED` 可发布为 `PUBLISHED`。`PUBLISHED` 可下架为 `OFFLINE`。`OFFLINE` 可重新发布、归档或软删除。`DRAFT`、`REJECTED`、`NEEDS_CHANGES` 和 `OFFLINE` 可归档或软删除。`ARCHIVED` 和 `DELETED` 为终态，不得回到公开状态。

写接口支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `49914`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序。

并发关注同一用户同一事件只能产生一条有效关注记录。重复取消关注保持幂等。关注计数必须和有效关注记录一致，不得小于 0。并发审核、发布、下架、归档和软删除同一事件只能有一个成功状态推进。读取接口允许读到更新前或更新后的完整状态，不能返回半更新对象。

P1 内存实现必须用本服务内的串行临界区保护事件状态、关注计数、来源同步和审计写入。后续持久化实现必须迁移为数据库事务、唯一约束、条件更新或等效机制，不能降低上述并发口径。

### 审计要求

必须审计的动作包括事件创建、事件修改、提交审核、审核通过、审核拒绝、发布、下架、归档、软删除、关注事件、取消关注、activity 同步、同步失败、提醒失败、依赖降级、自检读取、审计写入失败和状态写入失败。

后台写操作必须记录 `reason`、操作者、目标对象、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计不得泄露 token、完整请求头、通知正文、前序服务内部路径、真实服务器命令、节点凭据、Cloudreve token、内部异常堆栈或 activity 内部实现。

审计写入失败时，事件创建、修改、审核、发布、下架、归档、软删除、activity 同步和后台状态写入不得假装成功，必须返回 `54801` 或 `54800`，并保持业务数据不变。普通用户关注和取消关注在 P1 也必须保证审计和关注计数一致，失败返回 `54803` 或 `54801`，不得产生半关注状态。

### 失败降级

auth 是所有登录接口强依赖。auth 不可用、超时、用户状态不允许或字段不兼容时不得伪造成功。

activity 是只读同步来源。同步失败不得删除已有事件；公开读取可展示已保存快照并标记 stale。字段不兼容必须记录依赖失败摘要，不能保存伪造时间。

notification 是辅助依赖。提醒摘要失败不得回滚事件发布或关注主状态，但必须保存脱敏失败摘要和审计。

changelog 当前未连接。手工版本更新事件不依赖 changelog；未来 changelog 同步必须作为兼容变更补契约、测试和适配器。

维护窗口和服务器日程只是日程展示。任何真实服务器操作不可通过 calendar 降级或回退执行。

### 验收口径

`calendar` API 文档按 `docs/contracts-calendar.md` 独立存在，并由 `.local-docs/tests-calendar.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`calendar` 完成时必须满足以下条件：全部接口按本文档实现；公开接口只返回公开可见事件；当前用户只能维护自己的关注；后台接口按角色限制；事件状态机不可非法回退；跨月、时间范围重叠和全天事件查询正确；activity 同步只读且有失败降级；changelog 只保留占位；维护窗口不触发真实运维；所有写操作有审计；当前运行端口固定为 `8132`，自检摘要返回 `port=8132` 和 `legacyPort=8114`；`.local-docs/tests-calendar.md` 与 `.local-docs/tests-engagement-core.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 calendar 在 `engagement-core-service` 中全部测试通过；auth、profile、notification、content、server-status、resource、admin、onboarding、exam、whitelist、attendance、community 和 activity 前序服务回归测试通过；不恢复 `backend/calendar-service` 旧入口；没有修改前序服务稳定接口；没有把更新日志主数据、活动报名、社区投票、后台聚合、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复或 Cloudreve 管理能力塞进 calendar。

生产化硬化验收还必须满足：测试控制头默认关闭，只有本地自动化测试显式启用时才生效；关闭状态下依赖失败模拟头、写入失败模拟头、时间模拟头和通知失败模拟头全部被忽略；自检摘要明确返回当前测试控制头开关状态。

## 北冥官网 changelog API 契约

来源：`docs/contracts-changelog.md`

版本：0.2

### 文档定位

本文档是 `changelog` 微服务的正式 API 契约。后续前端适配、`admin` 聚合、`ops-control`、`external-node-executor` 和其他业务模块只能通过本文档定义的接口读取或管理更新日志，不能直接读取或修改 `changelog` 数据库，也不能把公告、资源下载、服务器运维、日历主数据或活动报名逻辑塞进 `changelog`。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `changelog` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了成熟更新发布平台和开源生态的公开做法。Keep a Changelog 1.1.0 强调变更应按版本组织，并按 `Added`、`Changed`、`Deprecated`、`Removed`、`Fixed`、`Security` 分组，说明更新日志要让人读懂，而不是只堆原始提交。GitHub Releases API 把 release、tag、draft、prerelease 和 assets 分开，说明发布对象、发布状态和附件链接需要结构化。GitLab Releases API 把 release、milestones 和 asset links 分开，说明发布记录可以关联外部资产但不应吞掉来源系统。Kubernetes release notes 把版本、升级影响、功能状态、已知问题和变更项分层表达，说明面向运维和玩家的更新说明需要明确影响范围。本项目只吸收这些产品思路，不接入 GitHub、GitLab、Kubernetes、CI 发布、Git tag 或外部仓库主数据。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) | 更新日志按版本和人类可读分组组织，避免把 raw commit 当正式说明。 |
| [GitHub REST API Releases](https://docs.github.com/en/rest/releases/releases) | release、draft、prerelease、tag 和资产链接需要分离。 |
| [GitLab Releases API](https://docs.gitlab.com/api/releases/) | 发布对象可以关联 milestone 和 asset links，但不接管外部系统主数据。 |
| [Kubernetes Release Notes](https://kubernetes.io/releases/notes/) | 版本说明要表达影响范围、升级注意事项、已知问题和变更等级。 |

### 职责边界

`changelog` 负责服务器版本、插件变更、规则调整、资源包更新、地图更新、重要维护记录、发布说明、变更分组、影响范围、关联资源快照、关联日程快照、关联内容页快照、通知投递摘要、changelog 审计、自检摘要和自身幂等记录。

`changelog` 不负责官网公告主发布、内容专题主数据、资源文件下载、资源版本创建、Cloudreve 分享链接生成、玩家可见服务器状态采集、真实服务器维护、容器、终端、文件管理、节点守护、日历事件主数据、活动报名结果、社区帖子、考勤积分、白名单或运维审批。

`changelog` 只能后序适配前序服务。它通过 `auth` 认证上下文读取当前用户、角色、能力点和用户状态；通过 `resource` 公开或后台正式接口保存资源和版本快照；通过 `server-status` 正式接口保存实例名称、Minecraft 版本和线路摘要；通过 `content` 正式接口保存公开说明页快照；通过 `calendar` 兼容接口或本服务内同步摘要保存版本发布日期程引用；通过 `notification` 投递发布、下架、安全修复和规则调整通知摘要。`changelog` 不导入前序服务内部类、Repository、内存存储、测试种子或数据库表。

### 数据归属

`changelog` 拥有以下主数据：发布记录、版本记录、变更分组、变更项、影响范围、兼容说明、已知问题、回滚说明、关联资源快照、关联服务器实例快照、关联日历摘要、关联内容快照、通知投递摘要、依赖调用摘要、幂等记录、changelog 审计日志和自检统计。

`changelog` 可以保存来自 `auth` 的 `userId`、`displayName`、`roles`、`permissions` 和用户状态快照；可以保存来自 `resource` 的 `resourceId`、`slug`、`versionName`、`visibility` 和可用性摘要；可以保存来自 `server-status` 的 `instanceId`、`name`、`minecraftVersion` 和状态快照摘要；可以保存来自 `content` 的 `contentId`、`slug`、`title` 和公开地址；可以保存来自 `calendar` 的 `eventId`、时间和同步状态；可以保存来自 `notification` 的投递结果摘要。所有快照只服务展示、检索和审计，不能成为来源模块主数据，也不能反写来源模块。

P1 内存实现必须完整兑现本文档已经承诺的 HTTP 行为，包括筛选参数、字段存储、用户态字段、审计查询和并发口径。真实数据库持久化、真实网关 auth、真实服务间 HTTP、真实 calendar 写入和真实 notification 投递属于生产化后续变更，必须继续通过自检摘要暴露缺口，不得伪装为已接通。

### 基础路径与认证

所有接口默认使用 `/api/v1/changelog` 前缀。当前运行入口为 `engagement-core-service:8132`。历史原服务端口 `8115` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或回归测试命令。

公开接口允许游客读取 `PUBLISHED` 且 `visibility=PUBLIC` 的发布记录。公开接口不得返回内部备注、后台审核字段、通知失败详情、完整依赖错误、审计参数、安全修复 exploit 细节、服务器内部路径、节点地址、token、真实运维命令或 Cloudreve 管理信息。

当前用户接口使用 `/api/v1/changelog/me` 前缀，全部要求 `Authorization: Bearer <token>`。当前用户只能读取和维护自己的收藏记录。浏览器请求体不得传入 `userId`、`roles`、`permissions`、`bookmarkCount`、`status`、`operatorUserId`、`reviewerUserId`、`auditResult`、`notificationStatus` 等服务端可信字段。

后台接口使用 `/api/v1/changelog/admin` 前缀，全部要求登录。后台读取发布记录、自检摘要和普通管理详情要求 `HELPER`、`ADMIN` 或 `OWNER`。创建草稿、修改自己创建的未发布草稿、提交审核、初审意见允许 `HELPER`、`ADMIN` 或 `OWNER`。发布、下架、归档、软删除、日历同步、审计读取和安全修复公开策略调整只允许 `ADMIN` 或 `OWNER`。

### 本地测试控制头

`changelog` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Resource-Mode`、`X-Test-Server-Status-Mode`、`X-Test-Content-Mode`、`X-Test-Calendar-Mode`、`X-Test-Notification-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store`、`X-Test-Fail-Bookmark` 和 `X-Test-Now` 模拟依赖失败、通知失败、写入失败、收藏并发冲突和时间边界。该能力只服务本地测试闭环，不属于正式业务 API。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、状态失败、收藏失败、通知失败、时间模拟或快照 stale。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 视为已满足的生产化硬化项。

### 前序服务兼容契约

`auth` 是所有登录接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `ACTIVE` 时可收藏和后台写入；`PENDING_PROFILE` 可以收藏公开版本但不能后台写入；`DISABLED`、`BANNED`、`DELETED` 不允许写入。auth 不可用返回 `49100`，auth 超时返回 `49101`，字段或枚举不兼容返回 `49102`。

`resource` 是资源包、地图文件、整合包和文档资源的可选关联来源。创建或修改关联资源时只能读取正式接口返回的公开或后台快照，不能生成下载票据，不能修改资源状态，不能保存 Cloudreve token。resource 不可用返回 `49110`，超时返回 `49111`，字段或枚举不兼容返回 `49112`。公开读取已有发布记录时可以使用旧资源快照并标记 `resourceSnapshotStale=true`。

`server-status` 是玩家可见服务器实例状态和版本摘要来源。`changelog` 可以保存实例名称、当前 Minecraft 版本和线路摘要，不能刷新状态、记录在线人数历史或执行宕机处理。server-status 不可用返回 `49120`，超时返回 `49121`，字段或枚举不兼容返回 `49122`。读取已有记录可展示旧快照并标记 stale。

`content` 是说明页、公告页、专题页和 SEO 的归属服务。`changelog` 可以关联公开说明页快照，不能创建公告，不能管理首页配置，不能吞掉 content 审核发布流。content 不可用返回 `49130`，超时返回 `49131`，字段或枚举不兼容返回 `49132`。

`calendar` 是版本发布日程和维护窗口日程的归属服务。P1 中 `changelog` 默认不反向写 calendar 主数据，只保存 `calendarSyncStatus=SKIPPED` 或测试控制下的失败摘要。若后续需要真实写入 calendar，必须先确认 `docs/contracts-calendar.md` 已有兼容写接口；接口不足时先按前序兼容变更流程处理。calendar 不可用记录或返回 `49140`，超时记录或返回 `49141`，字段不兼容记录或返回 `49142`。calendar 同步失败不得回滚 changelog 主状态。

`notification` 是辅助依赖。发布、下架、安全修复、规则调整、资源包更新可以触发通知。通知失败不得回滚发布或下架主状态，但必须保存 `notificationStatus=FAILED`、失败原因摘要并写入审计。notification 不可用记录或返回 `49150`，超时记录或返回 `49151`，字段不兼容记录或返回 `49152`。通知失败摘要不得包含通知正文、完整请求头、认证 token、内部 URL 或异常堆栈。

`admin` 当前只是聚合入口。本文档不修改 `admin` 契约。若后续需要 admin 展示 changelog 待办、指标或审计索引，必须作为 admin 兼容变更单独走契约、测试、红灯、实现和回归闭环。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `ChangelogType` | `SERVER_VERSION`、`PLUGIN_CHANGE`、`RULE_CHANGE`、`RESOURCE_PACK`、`MAP_UPDATE`、`MAINTENANCE`、`SECURITY`、`OTHER` | 发布记录主类型。 |
| `ChangelogGroupType` | `ADDED`、`CHANGED`、`DEPRECATED`、`REMOVED`、`FIXED`、`SECURITY`、`PERFORMANCE`、`KNOWN_ISSUE` | 变更分组类型。 |
| `ChangelogStatus` | `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`PUBLISHED`、`OFFLINE`、`ARCHIVED`、`DELETED` | 发布记录主状态。 |
| `ChangelogVisibility` | `PUBLIC`、`MEMBER_ONLY`、`STAFF_ONLY` | 可见范围。P1 公开接口只返回 `PUBLIC`。 |
| `ChangelogImpactLevel` | `LOW`、`MEDIUM`、`HIGH`、`CRITICAL` | 影响等级。`SECURITY` 类型不能低于 `MEDIUM`。 |
| `ChangelogItemSeverity` | `INFO`、`MINOR`、`MAJOR`、`BREAKING`、`SECURITY` | 单条变更严重度。 |
| `ChangelogNotificationStatus` | `SKIPPED`、`PENDING`、`DELIVERED`、`FAILED` | 最近通知摘要。P1 默认 `SKIPPED`。 |
| `ChangelogSyncStatus` | `SKIPPED`、`SYNCED`、`FAILED`、`STALE` | 关联日历或来源快照同步状态。 |
| `ChangelogBookmarkStatus` | `ACTIVE`、`CANCELED` | 当前用户收藏状态。 |
| `ChangelogAuditResult` | `SUCCESS`、`FAILED` | 审计结果。 |

### 通用对象

#### ChangelogRelease

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `releaseId` | string | 是 | 发布记录 ID。 |
| `slug` | string | 是 | 2 到 100 位，小写字母、数字和短横线，全局唯一。 |
| `versionName` | string | 是 | 1 到 80 位，展示版本名，例如 `v1.20.4-2026.06`。 |
| `title` | string | 是 | 2 到 120 位。 |
| `summary` | string | 是 | 1 到 300 位。 |
| `body` | string | 是 | 1 到 10000 位，公开正文。 |
| `type` | string | 是 | `ChangelogType`。 |
| `status` | string | 是 | `ChangelogStatus`。 |
| `visibility` | string | 是 | `ChangelogVisibility`。 |
| `impactLevel` | string | 是 | `ChangelogImpactLevel`。 |
| `releasedAt` | string 或 null | 是 | 对玩家宣告发布时间。 |
| `effectiveAt` | string 或 null | 是 | 实际生效时间。 |
| `minecraftVersion` | string 或 null | 是 | Minecraft 版本摘要。 |
| `pluginVersions` | object[] | 是 | 插件名称、版本、动作和公开备注。创建和修改时必须按请求体结构化保存。 |
| `resourcePackVersions` | object[] | 是 | 资源包名称、版本、资源快照 ID。创建和修改时必须按请求体结构化保存。 |
| `mapVersion` | string 或 null | 是 | 地图版本摘要。 |
| `groups` | ChangelogGroup[] | 是 | 变更分组。至少 1 组，每组至少 1 项。 |
| `compatibilityNotes` | string 或 null | 是 | 兼容说明，最多 2000 位。 |
| `knownIssues` | string 或 null | 是 | 已知问题，最多 2000 位。 |
| `rollbackNotes` | string 或 null | 后台可见或公开摘要 | 回滚说明，最多 2000 位。公开视图不得泄露运维内部路径或命令。 |
| `securityPublicSummary` | string 或 null | `SECURITY` 时必填 | 安全修复公开摘要，不含 exploit 细节。 |
| `relatedResources` | ChangelogRelatedResource[] | 是 | 关联资源快照。 |
| `relatedServerInstances` | object[] | 是 | 关联服务器实例快照。 |
| `relatedCalendarEvent` | ChangelogCalendarRef 或 null | 是 | 关联日程摘要。 |
| `relatedContent` | object 或 null | 是 | 说明页公开快照。 |
| `notificationSummary` | ChangelogNotificationSummary | 是 | 最近通知摘要。 |
| `bookmarkCount` | integer | 是 | 有效收藏数。 |
| `bookmarkedByCurrentUser` | boolean | 当前用户视图可见 | 当前用户是否收藏。 |
| `createdBy` | string | 后台可见 | 创建者用户 ID。 |
| `updatedBy` | string | 后台可见 | 最近修改者用户 ID。 |
| `reviewedBy` | string 或 null | 后台可见 | 审核者用户 ID。 |
| `reviewComment` | string 或 null | 是 | 给创建者的审核意见。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注，公开和当前用户接口不得返回。 |
| `submittedAt` | string 或 null | 是 | 提交审核时间。 |
| `reviewedAt` | string 或 null | 是 | 审核时间。 |
| `publishedAt` | string 或 null | 是 | 发布时间。 |
| `offlineAt` | string 或 null | 是 | 下架时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |
| `deletedAt` | string 或 null | 后台可见 | 软删除时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### ChangelogGroup

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `groupId` | string | 是 | 分组 ID。 |
| `type` | string | 是 | `ChangelogGroupType`。 |
| `title` | string | 是 | 1 到 80 位。 |
| `description` | string 或 null | 是 | 分组说明，最多 1000 位。 |
| `items` | ChangelogItem[] | 是 | 变更项，至少 1 项。 |
| `sortOrder` | integer | 是 | 0 到 999。 |

#### ChangelogItem

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `itemId` | string | 是 | 变更项 ID。 |
| `title` | string | 是 | 1 到 120 位。 |
| `description` | string | 是 | 1 到 2000 位。 |
| `severity` | string | 是 | `ChangelogItemSeverity`。 |
| `component` | string 或 null | 是 | 影响组件，例如 `server`、`plugin:CoreProtect`、`resource-pack`。 |
| `publicSafe` | boolean | 是 | 是否可在公开接口完整展示。为 `false` 时公开接口只展示脱敏摘要。 |
| `sortOrder` | integer | 是 | 0 到 999。 |

#### ChangelogRelatedResource

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `resourceId` | string | 是 | resource 服务资源 ID。 |
| `slug` | string | 是 | resource slug 快照。 |
| `versionName` | string 或 null | 是 | 资源版本名快照。 |
| `visibility` | string | 是 | 资源可见范围快照。 |
| `downloadAvailable` | boolean | 是 | 下载入口是否可用摘要。 |
| `resourceSnapshotStale` | boolean | 是 | 是否使用旧快照。 |
| `failure` | object 或 null | 是 | 脱敏失败摘要。 |

#### ChangelogCalendarRef

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `eventId` | string 或 null | 是 | calendar 事件 ID。 |
| `title` | string 或 null | 是 | 日程标题快照。 |
| `startAt` | string 或 null | 是 | 日程开始时间。 |
| `syncStatus` | string | 是 | `ChangelogSyncStatus`。 |
| `lastSyncedAt` | string 或 null | 是 | 最近同步时间。 |
| `failure` | object 或 null | 是 | 脱敏失败摘要。 |

#### ChangelogNotificationSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | `ChangelogNotificationStatus`。 |
| `targetAudience` | string | 是 | `PUBLIC`、`MEMBERS` 或 `STAFF`。 |
| `lastAttemptAt` | string 或 null | 是 | 最近投递尝试时间。 |
| `failure` | object 或 null | 是 | 脱敏失败摘要，不含通知正文、token、请求头、内部 URL 和异常堆栈。 |

#### ChangelogBookmark

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bookmarkId` | string | 是 | 收藏记录 ID。 |
| `releaseId` | string | 是 | 发布记录 ID。 |
| `userId` | string | 是 | auth 用户 ID。 |
| `displayNameSnapshot` | string | 是 | 用户展示名快照。 |
| `status` | string | 是 | `ChangelogBookmarkStatus`。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `canceledAt` | string 或 null | 是 | 取消收藏时间。 |

#### ChangelogAuditLog

审计字段继承公共契约，允许补充 `releaseId`、`slug`、`versionName`、`stateFrom`、`stateTo`、`idempotencyKey`、`dependencyStatus`、`calendarSyncStatus`、`notificationStatus`、`resourceSnapshotStale` 和 `securityRedactionApplied`。审计日志不得通过 changelog API 删除。

### changelog 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `49100` | 502 | auth 认证上下文不可用。 |
| `49101` | 504 | auth 认证上下文调用超时。 |
| `49102` | 502 | auth 返回字段不兼容 changelog 契约。 |
| `49110` | 502 | resource 快照不可用。 |
| `49111` | 504 | resource 快照调用超时。 |
| `49112` | 502 | resource 返回字段不兼容 changelog 契约。 |
| `49120` | 502 | server-status 实例摘要不可用。 |
| `49121` | 504 | server-status 实例摘要调用超时。 |
| `49122` | 502 | server-status 返回字段不兼容 changelog 契约。 |
| `49130` | 502 | content 公开说明页快照不可用。 |
| `49131` | 504 | content 公开说明页快照调用超时。 |
| `49132` | 502 | content 返回字段不兼容 changelog 契约。 |
| `49140` | 502 | calendar 同步不可用。 |
| `49141` | 504 | calendar 同步超时。 |
| `49142` | 502 | calendar 返回字段不兼容 changelog 契约。 |
| `49150` | 502 | notification 投递不可用。 |
| `49151` | 504 | notification 投递超时。 |
| `49152` | 502 | notification 返回字段不兼容 changelog 契约。 |
| `49300` | 404 | 发布记录不存在或不可见。 |
| `49301` | 404 | 收藏记录不存在或不可见。 |
| `49302` | 404 | 关联快照不存在。 |
| `49310` | 409 | 发布记录状态不允许当前操作。 |
| `49311` | 409 | 发布 slug 或版本名冲突。 |
| `49312` | 409 | 幂等键请求指纹冲突。 |
| `49313` | 409 | 同一用户已收藏该发布记录。 |
| `49314` | 409 | 安全修复公开摘要不满足脱敏要求。 |
| `49315` | 409 | 变更分组或变更项不满足发布要求。 |
| `49316` | 409 | 生效时间或发布时间不允许当前操作。 |
| `54900` | 500 | changelog 内部错误。 |
| `54901` | 500 | changelog 审计写入失败。 |
| `54902` | 500 | changelog 状态写入失败。 |
| `54903` | 500 | changelog 收藏写入失败。 |
| `54904` | 500 | changelog 关联快照写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。changelog 自有幂等指纹冲突使用 `49312`。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开发布列表 | GET | `/api/v1/changelog/releases` | 否 | 无 | LOW |
| 公开发布详情 | GET | `/api/v1/changelog/releases/{releaseIdOrSlug}` | 否 | 无 | LOW |
| 最新版本 | GET | `/api/v1/changelog/versions/latest` | 否 | 无 | LOW |
| 标签和筛选项 | GET | `/api/v1/changelog/tags` | 否 | 无 | LOW |
| 公开变更项搜索 | GET | `/api/v1/changelog/changes` | 否 | 无 | LOW |
| 我的收藏列表 | GET | `/api/v1/changelog/me/bookmarks` | 是 | 当前用户 | LOW |
| 收藏发布记录 | POST | `/api/v1/changelog/me/releases/{releaseId}/bookmark` | 是 | 当前用户 | LOW |
| 取消收藏发布记录 | POST | `/api/v1/changelog/me/releases/{releaseId}/unbookmark` | 是 | 当前用户 | LOW |
| 后台发布列表 | GET | `/api/v1/changelog/admin/releases` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 后台发布详情 | GET | `/api/v1/changelog/admin/releases/{releaseId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 创建发布草稿 | POST | `/api/v1/changelog/admin/releases` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 修改发布记录 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 提交审核 | POST | `/api/v1/changelog/admin/releases/{releaseId}/submit` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核通过 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/approve` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/reject` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 要求修改 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/request-changes` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 发布 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/publish` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 下架 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/offline` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 归档 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/archive` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 软删除 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/delete` | 是 | `ADMIN`、`OWNER` | HIGH |
| 日历同步摘要 | POST | `/api/v1/changelog/admin/releases/{releaseId}/calendar-sync` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 审计列表 | GET | `/api/v1/changelog/admin/audit-logs` | 是 | `ADMIN`、`OWNER` | LOW |
| changelog 自检摘要 | GET | `/api/v1/changelog/admin/ops/summary` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |

### 公开接口

#### 公开发布列表

`GET /api/v1/changelog/releases`

查询参数：`page`、`pageSize`、`keyword`、`type`、`visibility`、`impactLevel`、`minecraftVersion`、`tag`、`from`、`to` 和 `sort`。`pageSize` 最大 `100`。`sort` 允许 `releasedAt_desc`、`releasedAt_asc`、`effectiveAt_desc`、`updatedAt_desc`、`impactLevel_desc`。`tag` 使用公开标签聚合结果中的值，P1 中来源为公开安全变更项的 `component`、插件名称和资源包名称。

成功响应 HTTP `200`，分页 `items` 为公开视图 `ChangelogRelease[]`。游客只看到 `PUBLISHED` 且 `visibility=PUBLIC` 的发布记录。列表不得返回内部备注、后台审核字段、通知失败详情、审计字段、完整依赖错误和未脱敏安全细节。

时间范围规则：`from` 和 `to` 使用 ISO 8601，按 `releasedAt` 重叠查询。未设置 `releasedAt` 的非发布记录不得出现在公开列表。

#### 公开发布详情

`GET /api/v1/changelog/releases/{releaseIdOrSlug}`

成功响应 HTTP `200`，`data` 为公开视图 `ChangelogRelease`。发布记录不存在、不可见、已下架、已归档或已删除时返回 `49300`。`SECURITY` 类型只返回 `securityPublicSummary` 和 `publicSafe=true` 的变更项；`publicSafe=false` 的项只返回脱敏摘要。

#### 最新版本

`GET /api/v1/changelog/versions/latest`

查询参数：`type`、`minecraftVersion` 和 `visibility`。成功响应 HTTP `200`，`data` 为最近一条符合条件的公开发布记录摘要；没有记录时返回 `data=null`。排序按 `releasedAt_desc`、`createdAt_desc` 稳定排序。

#### 标签和筛选项

`GET /api/v1/changelog/tags`

成功响应 HTTP `200`，`data` 包含 `types`、`groupTypes`、`impactLevels`、`minecraftVersions`、`components` 和 `tags`。只统计公开可见发布记录，不暴露后台草稿、内部组件名、服务器内部路径或安全修复敏感组件。

#### 公开变更项搜索

`GET /api/v1/changelog/changes`

查询参数：`page`、`pageSize`、`keyword`、`groupType`、`severity`、`component`、`releaseType`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为公开变更项摘要，包含发布记录摘要、分组和变更项。只返回公开可见发布记录中 `publicSafe=true` 或已脱敏的变更项。`from` 和 `to` 使用 ISO 8601，按所属发布记录的 `releasedAt` 查询，非法时间返回 `40001`，非法范围返回 `49316`。`severity_desc` 必须先按严重度从高到低排序，再按发布时间倒序稳定排序。

### 当前用户接口

#### 我的收藏列表

`GET /api/v1/changelog/me/bookmarks`

查询参数：`page`、`pageSize`、`status`、`type`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为当前用户收藏记录与发布摘要。只能返回当前认证用户自己的收藏，不得通过请求参数传入 `userId`。当前用户视图中的发布摘要必须包含 `bookmarkedByCurrentUser`。非法 `status` 或 `type` 返回 `40001`，非法 `sort` 返回 `40003`，非法时间返回 `40001`，非法范围返回 `49316`。

#### 收藏发布记录

`POST /api/v1/changelog/me/releases/{releaseId}/bookmark`

请求字段：`idempotencyKey` 可选，8 到 80 位。成功响应 HTTP `201` 或重复收藏幂等 HTTP `200`，`data` 为 `ChangelogBookmark` 和发布摘要。只允许收藏公开可见、未下架、未归档、未删除的发布记录。并发收藏同一用户同一发布记录只能产生一条有效记录。相同幂等键同请求体重复提交返回同一结果，相同键不同体返回 `49312`。

#### 取消收藏发布记录

`POST /api/v1/changelog/me/releases/{releaseId}/unbookmark`

请求字段：`reason` 可选，最多 200 位；`idempotencyKey` 可选。成功响应 HTTP `200`，`data` 为取消后的收藏摘要。未收藏时返回幂等成功，不能把 `bookmarkCount` 扣成负数。取消收藏不删除历史记录，只标记为 `CANCELED`。

### 后台接口

#### 后台发布列表和详情

`GET /api/v1/changelog/admin/releases` 支持 `page`、`pageSize`、`keyword`、`type`、`status`、`visibility`、`impactLevel`、`createdBy`、`minecraftVersion`、`from`、`to` 和 `sort`。后台可查看全部非物理删除记录，默认按 `updatedAt_desc`。`from` 和 `to` 按 `createdAt` 查询。`GET /api/v1/changelog/admin/releases/{releaseId}` 返回发布记录、收藏统计、关联快照、通知摘要、日历同步摘要、依赖摘要和最近审计。响应不得返回 token、完整请求头、通知正文、前序服务内部路径、异常堆栈、真实服务器命令、节点凭据或 Cloudreve token。

#### 创建发布草稿

`POST /api/v1/changelog/admin/releases`

请求字段包括 `slug`、`versionName`、`title`、`summary`、`body`、`type`、`visibility`、`impactLevel`、`releasedAt`、`effectiveAt`、`minecraftVersion`、`pluginVersions`、`resourcePackVersions`、`mapVersion`、`groups`、`compatibilityNotes`、`knownIssues`、`rollbackNotes`、`securityPublicSummary`、`relatedResourceIds`、`relatedServerInstanceIds`、`relatedContentId`、`internalNote`、`reason` 和 `idempotencyKey`。`reason` 必填，1 到 200 位。成功响应 HTTP `201`，状态为 `DRAFT`。slug 或版本名冲突返回 `49311`。`groups` 至少 1 组，每组至少 1 个 `items`，否则返回 `49315`。`SECURITY` 类型必须提供 `securityPublicSummary`，且公开摘要不得包含内部路径、token、节点地址、命令或 exploit 细节。

#### 修改发布记录

`PATCH /api/v1/changelog/admin/releases/{releaseId}`

请求字段同创建发布草稿，除 `reason` 必填外其余字段按需修改。只允许 `DRAFT`、`NEEDS_CHANGES`、`REJECTED` 和未发布的 `APPROVED` 修改主体字段。`PUBLISHED` 记录如需改正文，P1 必须先下架后修改再发布，避免公开读到半更新状态。`HELPER` 只能修改自己创建且未发布的发布记录。

#### 审核发布状态

`POST /api/v1/changelog/admin/releases/{releaseId}/submit` 使 `DRAFT`、`NEEDS_CHANGES` 或 `REJECTED` 进入 `PENDING_REVIEW`。

`PATCH /api/v1/changelog/admin/releases/{releaseId}/approve` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，使 `PENDING_REVIEW` 或 `NEEDS_CHANGES` 进入 `APPROVED`。

`PATCH /api/v1/changelog/admin/releases/{releaseId}/reject` 请求字段同审核通过，使 `PENDING_REVIEW` 进入 `REJECTED`。

`PATCH /api/v1/changelog/admin/releases/{releaseId}/request-changes` 请求字段同审核通过，使 `PENDING_REVIEW` 进入 `NEEDS_CHANGES`。

`PATCH /api/v1/changelog/admin/releases/{releaseId}/publish` 请求字段为 `releasedAt`、`effectiveAt`、`notificationAudience`、`reason`、`idempotencyKey`，使 `APPROVED` 或 `OFFLINE` 进入 `PUBLISHED`，写入 `publishedAt`。发布前必须重新校验分组、公开安全摘要、发布时间和可见范围。notification 或 calendar 失败不回滚主状态，但必须保存脱敏失败摘要和审计。

`PATCH /api/v1/changelog/admin/releases/{releaseId}/offline` 请求字段为 `publicReason`、`reason`、`idempotencyKey`，使 `PUBLISHED` 进入 `OFFLINE`。下架后公开接口不可见，但收藏记录保留。

`PATCH /api/v1/changelog/admin/releases/{releaseId}/archive` 请求字段为 `reason`、`idempotencyKey`，使 `DRAFT`、`REJECTED`、`NEEDS_CHANGES` 或 `OFFLINE` 进入 `ARCHIVED`。已发布记录必须先下架再归档。

`PATCH /api/v1/changelog/admin/releases/{releaseId}/delete` 是 `HIGH` 风险，请求字段为 `reason`、`confirmText`、`idempotencyKey`，P1 固定要求 `DELETE_CHANGELOG_RELEASE`。成功后状态为 `DELETED`，只做软删除，不物理删除发布记录、收藏记录和审计线索。

#### 日历同步摘要

`POST /api/v1/changelog/admin/releases/{releaseId}/calendar-sync`

请求字段：`mode` 可选，取值 `DRY_RUN` 或 `UPSERT_SNAPSHOT`，默认 `DRY_RUN`；`reason` 必填；`idempotencyKey` 可选。P1 默认不真实写入 calendar 主数据。`DRY_RUN` 返回将要同步的日程摘要和 `syncStatus=SKIPPED`。测试控制头模拟可用时，`UPSERT_SNAPSHOT` 返回 `syncStatus=SYNCED` 并保存本服务 `relatedCalendarEvent` 摘要；模拟失败时返回 `49140` 或保存失败摘要，同一实现必须固定并测试。calendar 同步失败不得删除已有日程摘要，不得回滚发布记录状态。

### 审计和自检

`GET /api/v1/changelog/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`releaseId`、`result`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为 `ChangelogAuditLog[]`。只有 `ADMIN` 和 `OWNER` 可访问。`from` 和 `to` 按 `createdAt` 查询。审计日志必须至少返回公共契约要求的 `id`、`requestId`、`actorUserId`、`actorRole`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason` 和 `createdAt`。审计日志不得通过 changelog API 删除。

`GET /api/v1/changelog/admin/ops/summary` 成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "changelog",
    "port": 8132,
    "legacyPort": 8115,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "resourceMode": "TEST_STUB",
    "serverStatusMode": "TEST_STUB",
    "contentMode": "TEST_STUB",
    "calendarSyncMode": "SKIPPED",
    "notificationMode": "SKIPPED",
    "testControlsEnabled": false,
    "releasesTotal": 8,
    "publishedReleasesTotal": 3,
    "bookmarksTotal": 6,
    "auditsTotal": 24,
    "idempotencyRecordsTotal": 12,
    "lastPublishedAt": "2026-05-25T12:00:00Z",
    "lastAuditAt": "2026-05-25T12:05:00Z",
    "productionGaps": [
      "P1_IN_MEMORY_STORAGE",
      "P1_AUTH_STUB",
      "P1_RESOURCE_STUB",
      "P1_SERVER_STATUS_STUB",
      "P1_CONTENT_STUB",
      "CALENDAR_WRITE_NOT_CONNECTED",
      "NOTIFICATION_DELIVERY_NOT_CONNECTED",
      "TEST_CONTROLS_DISABLED_OUTSIDE_TEST"
    ]
  }
}
```

### 状态、幂等和并发

发布记录创建后为 `DRAFT`。`DRAFT` 可提交为 `PENDING_REVIEW`。`PENDING_REVIEW` 可通过为 `APPROVED`、拒绝为 `REJECTED`、要求修改为 `NEEDS_CHANGES`。`NEEDS_CHANGES` 和 `REJECTED` 可修改后再次提交。`APPROVED` 可发布为 `PUBLISHED`。`PUBLISHED` 可下架为 `OFFLINE`。`OFFLINE` 可重新发布、归档或软删除。`DRAFT`、`REJECTED`、`NEEDS_CHANGES` 和 `OFFLINE` 可归档或软删除。`ARCHIVED` 和 `DELETED` 为终态，不得回到公开状态。

写接口支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `49312`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序。

并发收藏同一用户同一发布记录只能产生一条有效收藏记录。重复取消收藏保持幂等。收藏计数必须和有效收藏记录一致，不得小于 0。并发审核、发布、下架、归档和软删除同一发布记录只能有一个成功状态推进。读取接口允许读到更新前或更新后的完整状态，不能返回半更新对象。

P1 内存实现必须用本服务内的串行临界区保护发布记录状态、收藏计数、关联快照、日历同步摘要和审计写入。所有状态流转写操作必须在同一个临界区内完成状态校验、状态修改、审计写入和响应快照生成。后续持久化实现必须迁移为数据库事务、唯一约束、条件更新或等效机制，不能降低上述并发口径。

### 审计要求

必须审计的动作包括发布记录创建、修改、提交审核、审核通过、审核拒绝、要求修改、发布、下架、归档、软删除、收藏、取消收藏、日历同步、通知失败、依赖降级、自检读取、审计写入失败和状态写入失败。

后台写操作必须记录 `reason`、操作者、目标对象、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计不得泄露 token、完整请求头、通知正文、前序服务内部路径、真实服务器命令、节点凭据、Cloudreve token、内部异常堆栈、安全 exploit 细节或未脱敏运维参数。

审计写入失败时，发布记录创建、修改、审核、发布、下架、归档、软删除和日历同步不得假装成功，必须返回 `54901` 或 `54900`，并保持业务数据不变。普通用户收藏和取消收藏在 P1 也必须保证审计和收藏计数一致，失败返回 `54903` 或 `54901`，不得产生半收藏状态。通知失败不回滚主状态，但必须记录失败摘要和审计。

### 失败降级

auth 是所有登录接口强依赖。auth 不可用、超时、用户状态不允许或字段不兼容时不得伪造成功。

resource、server-status 和 content 是关联快照依赖。创建或修改关联时，快照不可用不得保存伪造关联。读取已存在发布记录时，来源服务失败可以使用已保存快照降级并标记 stale。

calendar 是辅助同步依赖。同步失败不得回滚发布主状态，不得删除已有日程摘要。P1 默认只保存同步摘要，不反向写 calendar 主数据。

notification 是辅助依赖。发布、下架和安全修复通知失败不得回滚主状态，但必须保存脱敏失败摘要和审计。

安全修复公开字段必须优先保护服务器、玩家和运维安全。公开接口不得泄露 exploit 细节、内部路径、拓扑、token、节点地址、服务命令或回滚脚本。

### 验收口径

`changelog` API 文档按 `docs/contracts-changelog.md` 独立存在，并由 `.local-docs/tests-changelog.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`changelog` 完成时必须满足以下条件：全部接口按本文档实现；公开接口只返回公开可见发布记录和脱敏变更项；当前用户只能维护自己的收藏；后台接口按角色限制；发布状态机不可非法回退；安全修复公开摘要不泄露敏感信息；资源、server-status、content、calendar 和 notification 都只走正式契约或受控适配层；calendar 同步失败不影响 changelog 主状态；notification 失败记录脱敏摘要；所有写操作有审计；当前运行端口固定为 `8132`，自检摘要返回 `port=8132` 和 `legacyPort=8115`；`.local-docs/tests-changelog.md` 与 `.local-docs/tests-engagement-core.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 changelog 在 `engagement-core-service` 中全部测试通过；auth、profile、notification、content、server-status、resource、admin、onboarding、exam、whitelist、attendance、community、activity 和 calendar 前序服务回归测试通过；不恢复 `backend/changelog-service` 旧入口；没有修改前序服务稳定接口；没有把官网公告、资源下载、日历主数据、活动报名、后台聚合、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复或 Cloudreve 管理能力塞进 changelog。

生产化硬化验收还必须满足：测试控制头默认关闭，只有本地自动化测试显式启用时才生效；关闭状态下依赖失败模拟头、写入失败模拟头、时间模拟头和通知失败模拟头全部被忽略；自检摘要明确返回当前测试控制头开关状态。

## 北冥官网 engagement-core API 契约

来源：`docs/contracts-engagement-core.md`

版本：0.1

### 文档定位

`engagement-core` 是第三批社区运营模块的运行合并单元，承载 `community`、`activity`、`calendar` 和 `changelog`。它不新增社区、活动、日程或更新日志业务语义，只提供运行单元健康检查、后台装配摘要、路由签名覆盖状态和第三批合并边界说明。

四个业务模块的路径、方法、认证、权限、请求字段、响应字段、错误码、分页、幂等、状态流转、降级、审计和验收口径仍以 `docs/contracts-community.md`、`docs/contracts-activity.md`、`docs/contracts-calendar.md` 和 `docs/contracts-changelog.md` 为准。业务路径保持 `/api/v1/community/**`、`/api/v1/activity/**`、`/api/v1/calendar/**` 和 `/api/v1/changelog/**`，不得改成 `/api/v1/engagement-core/<module>/**`。

当前运行入口为 `engagement-core-service:8132`。历史端口 `8112` 到 `8115` 只作为 `legacyPort` 追溯字段，不再作为独立服务入口、网关上游或回归测试命令。

第九轮允许 `unified-backend-service:8135` 以 in-process 方式挂载 `engagement-core`。该候选挂载不改变 `engagement-core-service:8132` 的独立入口，不改变 `/api/v1/engagement-core/**`、`/api/v1/community/**`、`/api/v1/activity/**`、`/api/v1/calendar/**` 或 `/api/v1/changelog/**` 的路径、认证、响应格式、错误码、业务行为和测试口径。

### 自有接口

| 接口 | 方法 | 路径 | 认证 | 权限 | 说明 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/engagement-core/health` | 否 | 无 | 返回运行单元低敏健康摘要，不代表四个业务模块完整行为契约已全绿。 |
| 后台装配摘要 | GET | `/api/v1/engagement-core/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | 返回第三批模块装配、路由签名覆盖、前序基线、旧服务退役和生产化缺口摘要。 |
| 生产就绪诊断 | GET | `/api/v1/engagement-core/admin/production-readiness` | 是 | `ADMIN` 或 `OWNER` | 返回生产阻塞项、路由漂移状态、旧服务恢复保护和真实适配缺口摘要。 |

### 健康检查

`GET /api/v1/engagement-core/health`

成功响应 HTTP `200`。响应至少包含 `service=engagement-core`、`status`、`port=8132`、`modulesTotal=4`、`modulesMounted=4`、`engagementRoutesTotal=149`、`selfRoutesTotal=3`、`routeContractRoutesVerifiedTotal=149`、`routeContractCoverageStatus=ROUTE_CONTRACT_VERIFIED`、`behaviorContractCoverageStatus=COMPLETE_BEHAVIOR_CONTRACT_TESTS`、`moduleRoutes` 和 `generatedAt`。

该接口不得返回 token、Cookie、数据库连接串、异常栈、外部凭据、请求头原文、举报证据详情、工单内部备注、通知正文、真实服务器命令、节点凭据或 Cloudreve token。

### 后台装配摘要

`GET /api/v1/engagement-core/admin/ops/summary`

未登录返回 `41000`，令牌格式错误返回 `41003`，权限不足返回 `42001`。只有 `ADMIN` 和 `OWNER` 可访问。

成功响应 HTTP `200`，`data` 至少包含 `service`、`port`、`status`、`modulesTotal`、`modulesMounted`、`routesTotal=152`、`engagementRoutesTotal=149`、`selfRoutesTotal=3`、`routeContractRoutesVerifiedTotal=149`、`routeContractCoverageStatus=ROUTE_CONTRACT_VERIFIED`、`behaviorContractCoverageStatus=COMPLETE_BEHAVIOR_CONTRACT_TESTS`、`moduleRoutes`、`adapterChain`、`businessCoreDependency`、`admissionCoreDependency`、`gatewaySwitchReady`、`gatewaySwitchStatus`、`legacyBaselines`、`retiredLegacyServices`、`productionGaps` 和 `generatedAt`。

`moduleRoutes` 中每个模块必须返回 `port=8132`、对应 `legacyPort`、`contractRoutesTotal`、`routeContractRoutesVerifiedTotal`、`routeContractCoverageStatus` 和 `behaviorContractCoverageStatus`。当前四个模块路由签名覆盖数为 community `64`、activity `41`、calendar `21`、changelog `23`。

`productionGaps` 不得再保留 `complete inherited behavior contract tests are not all mounted in engagement-core`。这表示 149 个业务 `METHOD path` 路由签名和四个模块完整行为契约已经装配验证。五个后台自检摘要入口已经支持可信网关上下文，摘要缺口必须保留 `gateway trusted context is mounted for ops summaries only; complete business behavior auth coverage is still pending`，不得继续暴露 `real auth and gateway trusted context adapters are not connected`。后续仍要补真实持久化、审计持久化、真实跨服务 adapter、真实通知投递和真实 HTTP smoke。

可信网关上下文规则继承 `docs/contracts-engagement-core.md`。只有存在 `X-Gateway-Internal-Request-Id` 时，`engagement-core` 才解析 `X-Beiming-Actor-*`；缺少该内部请求编号时，直连伪造 actor 头必须被忽略并回退 Bearer 兼容路径。当前已覆盖 `/api/v1/engagement-core/admin/ops/summary`、`/api/v1/community/admin/ops/summary`、`/api/v1/activity/admin/ops/summary`、`/api/v1/calendar/admin/ops/summary` 和 `/api/v1/changelog/admin/ops/summary`。字段缺失、requestId 格式非法、角色或能力点不兼容、Minecraft UUID 不兼容必须失败，不得当成匿名或本地 token 成功。

`legacyBaselines` 不得包含 `community-service`、`activity-service`、`calendar-service` 或 `changelog-service`。`retiredLegacyServices` 必须返回这四个旧服务的退役摘要，且 `testCommand=null`。

### 生产就绪诊断

`GET /api/v1/engagement-core/admin/production-readiness`

该接口需要 `ADMIN` 或 `OWNER`，可使用 Bearer 本地兼容 token 或可信网关上下文。成功响应 HTTP `200`，`data` 至少包含 `service=engagement-core`、`port=8132`、`readyForProduction=false`、`readinessStatus=NOT_READY`、`routesTotal=152`、`engagementRoutesTotal=149`、`selfRoutesTotal=3`、`routeContractCoverageStatus=ROUTE_CONTRACT_VERIFIED`、`behaviorContractCoverageStatus=COMPLETE_BEHAVIOR_CONTRACT_TESTS`、`trustedGatewayCoverageStatus=OPS_SUMMARIES_ONLY`、`routeDriftStatus=NO_DRIFT`、`legacyServiceRestoreStatus=NOT_RESTORED`、`completeBehaviorContractRoutesVerifiedTotal=149`、`pendingBehaviorContractRoutesTotal=0`、`behaviorCoverageByModule`、`checks`、`productionBlockers` 和 `generatedAt`。

`checks` 必须包含 `ROUTE_SIGNATURES=PASS`、`BEHAVIOR_CONTRACTS=PASS`、`TRUSTED_GATEWAY_CONTEXT=PARTIAL`、`PERSISTENCE=BLOCKED`、`AUDIT_PERSISTENCE=BLOCKED`、`CROSS_SERVICE_ADAPTERS=BLOCKED`、`NOTIFICATION_DELIVERY=BLOCKED`、`LIVE_HTTP_SMOKE=BLOCKED` 和 `LEGACY_SERVICES=PASS`。该接口只读诊断摘要，不调用旧服务，不执行真实 HTTP smoke，不触发业务写操作，不返回 token、Cookie、完整请求头、真实数据库连接串、内部 URL、异常栈、节点凭据、服务器命令、举报证据、工单内部备注、通知正文或 Cloudreve token。

`behaviorCoverageByModule` 必须按 community、activity、calendar 和 changelog 返回完整行为契约覆盖进度。当前 community 64 个、activity 41 个、calendar 21 个和 changelog 23 个业务方法路由已迁入 `engagement-core-service` 完整行为契约测试，待补数均为 `0`。代表路由测试、后台自检测试和路由签名测试不得计入完整行为契约覆盖。

### 验收口径

`engagement-core` API 文档按 `docs/contracts-engagement-core.md` 独立存在，并由 `.local-docs/tests-engagement-core.md` 记录本地测试闭环。`mvn -f backend/engagement-core-service/pom.xml test` 必须覆盖三个自有接口、149 个第三批业务 `METHOD path` 路由签名、community、activity、calendar 和 changelog 完整行为契约迁入、旧服务不恢复保护和后续生产化缺口公开。

第三批旧服务已经清理。后续测试不得恢复、重建或执行 `backend/community-service`、`backend/activity-service`、`backend/calendar-service` 和 `backend/changelog-service` 的 Maven 入口。

## 北冥官网 ops-control API 契约

来源：`docs/contracts-ops-control.md`

版本：0.1

### 文档定位

本文档是 `ops-control` 微服务的正式 API 契约。后续前端运维控制台、`admin` 聚合、`external-node-executor` 和其他业务模块只能通过本文档定义的接口读取或管理服务器运维控制面，不能直接读取或修改 `ops-control` 数据，也不能把真实服务器命令、文件系统、Docker、虚拟机、Minecraft 实例或终端执行能力塞进其他服务。

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

### 职责边界

`ops-control` 负责后台服务器与资源运维控制面，包括节点注册、节点启用禁用、节点能力、心跳摘要、资产清单、节点指标快照、容器快照、虚拟机快照、Minecraft 实例快照、授权目录文件视图、文本文件读取请求、日志摘要请求、受控操作任务、高风险审批、任务取消、节点回写、运维审计和自检摘要。

`ops-control` 不负责注册、登录、角色能力点主数据、玩家资源下载、玩家可见服务器状态采集、官网公告、活动日历主数据、更新日志主数据、真实 Docker 操作、真实 Proxmox 操作、真实 MCSManager 操作、真实 shell 命令、真实文件上传下载、真实文件删除、真实终端 WebSocket、真实备份恢复或节点守护进程执行逻辑。

真实服务器上的系统资源、进程、容器、文件、日志、Minecraft 实例和终端命令必须由后续 `external-node-executor` 或受控适配器执行。P1 `ops-control` 只做控制面契约、授权、审计、任务状态、模拟节点适配和离线降级，不直接调用宿主机。

### 数据归属

`ops-control` 拥有以下主数据：节点、节点认证摘要、节点能力、节点心跳、资产、资产分组、指标快照、容器快照、虚拟机快照、Minecraft 实例快照、授权目录快照、文件读取请求、日志查询请求、受控操作任务、高风险审批、节点回写摘要、幂等记录、运维审计和自检统计。

`ops-control` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和用户状态快照；可以保存来自 `admin` 的入口引用和聚合摘要；可以保存来自 `server-status` 的玩家可见实例 ID 和公开名称快照；可以保存来自 `resource` 的 Cloudreve 服务资产引用和公开资源关联快照；可以保存来自 `calendar`、`changelog` 的维护窗口或版本发布引用快照。所有跨服务字段只能是快照或正式接口结果，不得直接读取前序服务数据库、内存 store、测试种子或私有类。

### 基础路径与认证

所有接口默认使用 `/api/v1/ops-control` 前缀。第四批合并后当前运行入口为 `ops-core-service:8133`。历史原服务端口 `8116` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或测试入口。

全部接口都要求 `Authorization: Bearer <token>`。读取类接口要求基础角色为 `HELPER`、`ADMIN` 或 `OWNER`，且具备对应读取能力点。写入或操作类接口要求 `ADMIN` 或 `OWNER`，并按目标能力点校验。高风险操作必须携带二次确认。严重风险操作必须由 `OWNER` 或具备 `HIGH_RISK_APPROVE` 的审批记录授权。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`nodeTokenDigest`、`credential`、`beforeState`、`afterState`、`taskStatus`、`approvalStatus`、`auditResult`、`createdBy`、`updatedBy` 等服务端可信字段。出现可信字段时，P1 可以忽略，但不得信任；涉及高风险写操作时推荐返回 `40001`。

### 本地测试控制头

`ops-control` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Node-Mode`、`X-Test-Admin-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store`、`X-Test-Now` 模拟认证失败、节点离线、节点超时、节点坏 schema、审计失败、存储失败和时间边界。该能力只服务本地契约测试，不属于正式业务 API。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、节点失败、状态失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 视为已满足的生产化硬化项。

### 前序服务兼容契约

`auth` 是强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问任何接口。auth 不可用返回 `49200`，auth 超时返回 `49201`，字段或枚举不兼容返回 `49202`。

`admin` 是聚合入口。`ops-control` 可以向 admin 暴露模块健康、待办、审计索引和运维摘要，但不能要求 admin 直接写运维主数据。admin 不可用时，自检和运维主接口不应伪造 admin 已同步成功，只能返回降级摘要或 `49210`。

`server-status` 是玩家可见状态服务。`ops-control` 可以保存玩家可见实例名称快照，不能要求 `server-status` 执行启停、终端、容器或文件操作。快照不可用返回 `49220`，已有快照读取可标记 stale。

`resource` 是玩家资源下载服务。`ops-control` 可以登记 Cloudreve、数据盘或备份盘资产，不能通过 `resource` 读取后台文件或把玩家资源权限当作服务器文件权限。resource 快照不可用返回 `49230`。

`calendar` 和 `changelog` 是只读辅助关联来源。维护窗口、版本发布影响可保存为快照。不可用时不阻断运维任务创建，但必须在任务或审计中记录降级摘要。

`external-node-executor` 尚未开发。P1 节点调用模式固定为 `SIMULATED`。节点离线时只允许读取最后快照，任何需要实时执行的操作必须返回任务失败或 `49260`，不能假装成功。

### 枚举

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

### 通用对象

#### OpsNode

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

#### OpsAsset

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

#### OpsMetricSnapshot

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

#### OpsContainerSnapshot

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

#### OpsVmSnapshot

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

#### OpsMinecraftInstanceSnapshot

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

#### OpsFileEntry

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

#### OpsTask

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

#### OpsApproval

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

#### OpsAuditLog

审计字段继承公共契约，允许补充 `nodeId`、`assetId`、`taskId`、`approvalId`、`nodeRequestId`、`stateFrom`、`stateTo`、`dependencyStatus` 和 `idempotencyKey`。审计列表不得提供删除接口。审计响应不得返回节点 token 原文、完整请求头、内部路径、真实命令、文件内容、异常堆栈、私钥、Cloudreve 管理凭据或服务器密码。

#### OpsSummary

自检摘要至少包含 `service`、`port`、`storageMode`、`authMode`、`adminAdapterMode`、`nodeAdapterMode`、`externalExecutorConnected`、`testControlsEnabled`、`nodesTotal`、`assetsTotal`、`tasksTotal`、`pendingApprovalsTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastHeartbeatAt`、`lastAuditAt` 和 `productionGaps`。

### ops-control 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `49200` | 502 | auth 认证上下文不可用。 |
| `49201` | 504 | auth 认证上下文调用超时。 |
| `49202` | 502 | auth 认证上下文不兼容。 |
| `49210` | 502 | admin 聚合适配不可用。 |
| `49220` | 502 | server-status 实例快照不可用。 |
| `49230` | 502 | resource 资产快照不可用。 |
| `49260` | 502 | external-node-executor 未连接或节点离线。 |
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

### 接口总览

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

### 读取接口

`GET /api/v1/ops-control/overview` 返回节点数量、在线节点、离线节点、资产数量、待审批数量、运行中任务、最近异常事件、降级模块和最近审计摘要。节点、资产和任务都只返回当前用户有能力查看的范围。节点离线时必须标记 `degraded=true`，不能伪造实时状态。

`GET /api/v1/ops-control/assets` 支持 `page`、`pageSize`、`keyword`、`nodeId`、`assetType`、`status`、`ownerModule`、`riskTag` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`、`assetType_asc`。成功响应分页 `items` 为 `OpsAsset[]`。

`GET /api/v1/ops-control/assets/{assetId}` 返回资产详情和关联快照。资产不存在返回 `49400`。响应不得返回真实内部路径、服务器密码、Cloudreve 管理 token、节点密钥或完整命令。

`GET /api/v1/ops-control/nodes` 支持 `page`、`pageSize`、`keyword`、`status`、`capability` 和 `sort`。成功响应分页 `items` 为 `OpsNode[]`。`HELPER` 仅可读节点和资产摘要，不能看到 token 摘要以外的敏感字段。

`GET /api/v1/ops-control/nodes/{nodeId}` 返回节点详情、最近心跳、能力、指标摘要和降级状态。节点不存在返回 `49401`。

`GET /api/v1/ops-control/nodes/{nodeId}/capabilities` 返回节点上报能力、控制面允许能力和当前用户可用能力的交集。

`GET /api/v1/ops-control/nodes/{nodeId}/metrics/latest` 返回最近 `OpsMetricSnapshot`。没有快照时返回包含 `nodeId` 和 `degraded=true` 的降级摘要；节点不存在返回 `49401`。

`GET /api/v1/ops-control/nodes/{nodeId}/containers`、`GET /vms`、`GET /minecraft-instances` 均返回最后快照分页。节点离线时仍可读最后快照，但必须标记 `stale=true`。详情不存在返回 `49400`。

`GET /api/v1/ops-control/nodes/{nodeId}/files` 查询参数包括 `rootAlias`、`path`。`path` 必须是 `/` 开头的授权根目录内相对路径，不允许 `..`、反斜杠、控制字符或编码绕过。路径越界返回 `49414`。目录匹配必须按路径段边界判断，不能让 `/foo` 命中 `/foobar`。P1 只返回模拟授权目录快照。

### 节点管理接口

`POST /api/v1/ops-control/nodes` 请求字段包括 `displayName`、`endpointSummary`、`capabilities`、`labels`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，创建状态为 `PENDING_REGISTRATION` 或 `ONLINE` 的节点，并返回一次性 `registrationToken` 的脱敏摘要。P1 不返回真实密钥明文。节点名称冲突返回 `49411`。

`PATCH /api/v1/ops-control/nodes/{nodeId}/disable` 请求字段包括 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `DISABLE_OPS_NODE`。成功后节点状态为 `DISABLED`，后续实时任务返回 `49415` 或 `49260`。重复禁用保持幂等。

`PATCH /api/v1/ops-control/nodes/{nodeId}/enable` 请求字段包括 `reason` 和 `idempotencyKey`。`DISABLED` 可回到 `OFFLINE`，等待下一次心跳确认在线。`REVOKED` 不允许启用。

`POST /api/v1/ops-control/nodes/{nodeId}/heartbeat` 请求字段包括 `status`、`version`、`capabilities`、`metrics`、`containers`、`vms`、`minecraftInstances`、`files` 和 `nodeRequestId`。`status` 必须属于 `OpsNodeStatus`，不允许浏览器或节点写入服务端可信字段。P1 允许 `NODE_WRITE` 用户模拟节点回写。心跳成功更新最后快照和审计摘要，不执行真实系统操作。

### 文件和日志请求

`POST /api/v1/ops-control/nodes/{nodeId}/files/read` 请求字段包括 `rootAlias`、`path`、`reason` 和 `idempotencyKey`。只允许读取 `editableText=true` 的文本文件快照，路径必须通过根目录保护。P1 返回模拟文本摘要，不返回真实文件内容。节点离线时返回最后快照或 `49415`，不能创建真实读取。

`POST /api/v1/ops-control/nodes/{nodeId}/logs/query` 请求字段包括 `targetType`、`targetId`、`tailLines`、`keyword`、`reason` 和 `idempotencyKey`。`tailLines` 范围为 `1` 到 `1000`。P1 返回日志摘要任务或模拟日志片段，不提供 WebSocket 流。

### 任务接口

`POST /api/v1/ops-control/tasks` 请求字段包括 `taskType`、`nodeId`、`targetType`、`targetId`、`params`、`reason`、`confirmText`、`approvalId` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `OpsTask`。请求不得携带 `actorUserId`、`actorRole`、`actorPermissions`、`taskStatus`、`approvalStatus`、`auditResult`、`createdBy`、`updatedBy`、`beforeState` 或 `afterState` 等服务端可信字段，出现时返回 `40001`。

任务能力规则：节点注册、启用、禁用和 token 轮换要求 `NODE_WRITE`。容器启动、停止、重启要求 `CONTAINER_OPERATE`。容器删除为 `CRITICAL`，必须审批。虚拟机动作要求 `VM_OPERATE`。Minecraft 实例启停要求 `CONTAINER_OPERATE` 或后续专用能力；`MC_COMMAND` 和 `TERMINAL_COMMAND` 要求 `TERMINAL_ACCESS` 且为 `CRITICAL`。文件读写、重命名、移动和删除要求 `FILE_MANAGE`，删除为 `HIGH`。备份恢复为 `CRITICAL`。

风险规则：`LOW` 和 `MEDIUM` 任务不需要审批。`HIGH` 任务必须有二次确认，`confirmText` 根据任务类型固定，例如 `DELETE_FILE`、`STOP_INSTANCE`。`CRITICAL` 任务可以先创建为 `PENDING_APPROVAL` 等待审批；真正派发或执行前必须有有效审批，或由 `OWNER` 在控制面直接授权。审批人不能审批自己的 `CRITICAL` 任务。

节点规则：节点不存在返回 `49401`。节点 `OFFLINE`、`DISABLED` 或 `REVOKED` 时，需要实时执行的任务返回 `49415` 或创建为 `FAILED`，不能进入 `SUCCEEDED`。容器、虚拟机、Minecraft 实例和文件任务必须校验目标快照存在，目标不存在返回 `49400`。P1 `nodeAdapterMode=SIMULATED` 下只允许白名单安全任务进入 `SUCCEEDED`，高风险真实执行任务进入 `PENDING_APPROVAL` 或 `FAILED`。

幂等规则：同一操作者、同一接口语义、同一 `idempotencyKey`、同一请求体重复提交返回同一任务。相同键不同体返回 `49412`。请求体指纹必须使用结构化 JSON 规范化，嵌套对象按字段名递归排序。

`GET /api/v1/ops-control/tasks` 支持 `page`、`pageSize`、`nodeId`、`taskType`、`status`、`riskLevel`、`createdBy`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`riskLevel_desc`。成功响应分页 `items` 为 `OpsTask[]`。

`GET /api/v1/ops-control/tasks/{taskId}` 返回任务详情和审批摘要。不存在返回 `49402`。

`PATCH /api/v1/ops-control/tasks/{taskId}/cancel` 请求字段包括 `reason` 和 `idempotencyKey`。只有 `PENDING_APPROVAL`、`QUEUED`、`DISPATCHED` 可以取消。`RUNNING` 任务 P1 不支持强制取消，返回 `49410`。终态任务重复取消返回状态冲突。

`POST /api/v1/ops-control/tasks/{taskId}/node-result` 请求字段包括 `nodeRequestId`、`status`、`resultSummary`、`failureReason` 和 `finishedAt`。`status` 只允许 `SUCCEEDED`、`FAILED` 或 `TIMEOUT`。P1 允许 `NODE_WRITE` 用户模拟节点结果回写。只允许 `DISPATCHED` 或 `RUNNING` 任务回写。回写必须审计并脱敏，审计写入失败时任务状态不得变化。

### 审批接口

`GET /api/v1/ops-control/approvals` 支持 `page`、`pageSize`、`status`、`riskLevel`、`requestedBy`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`reviewedAt_desc`、`riskLevel_desc`。只有 `OWNER` 或具备 `HIGH_RISK_APPROVE` 的用户可访问。

`PATCH /api/v1/ops-control/approvals/{approvalId}/approve` 请求字段包括 `reviewComment`、`reason` 和 `idempotencyKey`。审批通过后，关联任务从 `PENDING_APPROVAL` 进入 `QUEUED` 或在 P1 模拟模式下进入 `DISPATCHED`、`SUCCEEDED`、`FAILED`。审批不存在返回 `49403`。审批不是 `PENDING` 返回 `49410`。审批自己的 `CRITICAL` 任务返回 `49416`。审计写入失败时审批和任务状态必须保持不变。

`PATCH /api/v1/ops-control/approvals/{approvalId}/reject` 请求字段同审批通过。拒绝后任务进入 `FAILED`，失败原因为 `APPROVAL_REJECTED`。拒绝必须写审计，审计写入失败时审批和任务状态必须保持不变。

### 审计和自检接口

`GET /api/v1/ops-control/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`nodeId`、`taskId`、`result`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`riskLevel_desc`。只有 `ADMIN` 和 `OWNER` 可访问。审计列表是只读接口，不提供删除、修改或恢复。审计中的 `requestId` 必须来自当前 HTTP 请求或节点回写请求，不能使用固定占位值。

`GET /api/v1/ops-control/ops/summary` 成功响应 HTTP `200`，`data` 为 `OpsSummary`。P1 必须返回 `storageMode=IN_MEMORY`、`authMode=TEST_STUB`、`adminAdapterMode=TEST_STUB`、`nodeAdapterMode=SIMULATED`、`externalExecutorConnected=false`、`testControlsEnabled=false` 和生产化缺口。摘要不得返回 token、密码、请求头、内部路径、真实命令、文件内容、异常堆栈或节点密钥。

### 状态、幂等和并发

节点状态由注册、启用禁用、心跳和 token 状态共同决定。`PENDING_REGISTRATION` 收到有效心跳后进入 `ONLINE`。心跳缺失可进入 `OFFLINE`。异常能力缺失可进入 `DEGRADED`。`DISABLED` 只能由启用回到 `OFFLINE`。`REVOKED` 为终态，P1 不提供恢复接口。

资产状态流转为 `ACTIVE`、`MAINTENANCE`、`DISABLED` 和 `ARCHIVED`。P1 主要通过节点心跳和模拟种子维护快照，不提供物理删除接口。

任务状态流转为 `PENDING_APPROVAL` 到 `QUEUED`、`FAILED` 或 `CANCELED`；`QUEUED` 到 `DISPATCHED`、`CANCELED` 或 `FAILED`；`DISPATCHED` 到 `RUNNING`、`SUCCEEDED`、`FAILED` 或 `TIMEOUT`；`RUNNING` 到 `SUCCEEDED`、`FAILED` 或 `TIMEOUT`。`SUCCEEDED`、`FAILED`、`CANCELED` 和 `TIMEOUT` 为终态。

审批状态流转为 `PENDING` 到 `APPROVED`、`REJECTED`、`EXPIRED` 或 `CANCELED`。审批通过和拒绝必须与任务状态变化在同一个临界区内完成，不能出现审批通过但任务仍待审批的半状态。

所有写接口使用本服务内串行临界区保护状态推进、幂等记录、审计和响应快照。后续数据库实现必须迁移为事务、唯一约束、条件更新或等效机制，不能降低并发口径。

### 安全、降级和脱敏

路径安全必须拒绝 `..`、反斜杠、控制字符、空路径、非 `/` 开头路径和编码绕过。P1 只在授权根目录别名内返回模拟快照，不访问真实文件系统。

敏感字段不得出现在任何响应中，包括节点 token 原文、私钥、服务器密码、完整 Authorization 请求头、Cloudreve 管理 token、真实宿主路径、真实终端命令、完整文件内容、异常堆栈和数据库连接串。

节点离线时，只允许返回最后快照或创建失败任务。外部依赖不可用时，读取类接口可以局部降级并标记 `degraded=true`，写入类接口不得假装成功。

审计写入失败时，节点注册、节点状态变更、任务创建、任务取消、节点回写、审批通过和审批拒绝不得假装成功，必须返回 `55001` 并保持业务数据不变。

### 验收口径

`ops-control` API 文档按 `docs/contracts-ops-control.md` 独立存在，并由 `.local-docs/tests-ops-core.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`ops-control` 完成时必须满足以下条件：全部接口按本文档实现；当前运行入口为 `ops-core-service:8133`，历史端口 `8116` 只作为 `legacyPort` 返回；所有接口要求登录；读取接口校验 `NODE_READ` 或对应能力；操作接口按任务类型校验能力点；高风险操作要求二次确认；严重风险操作要求审批或 `OWNER` 授权；节点 token、内部路径、命令参数、异常堆栈和凭据脱敏；节点离线时不假装成功；路径穿越被拦截；任务幂等和并发边界可复现；审计失败能回滚状态；自检摘要暴露存储模式、节点适配模式、测试控制状态和生产化缺口；`.local-docs/tests-ops-core.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 `ops-control` 在 `ops-core-service` 中全部测试通过；当前后端运行入口回归测试通过；没有修改前序服务稳定接口；没有把真实服务器操作、Docker、Proxmox、MCSManager、文件删除、终端命令、备份恢复、Cloudreve 管理 token 或 `external-node-executor` 执行能力塞进控制面。

## 北冥官网 cloudreve-sync API 契约

来源：`docs/contracts-cloudreve-sync.md`

版本：0.1

### 文档定位

本文档是 `cloudreve-sync` 微服务的正式 API 契约。`cloudreve-sync` 负责 Cloudreve API 深度接入、provider 配置摘要、目录同步任务、文件元数据快照、分享链接解析、分享状态探测、失效降级、幂等记录和同步审计。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `cloudreve-sync` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、同步任务流转、失败降级、审计和验收口径。

`cloudreve-sync` 不是玩家资源服务，不拥有资源分类、资源条目、资源版本、可见范围或下载权限主数据。`resource` 仍然是玩家可见资源下载的唯一主数据服务。`cloudreve-sync` 不是后台服务器文件管理服务，不执行宿主机文件浏览、上传、下载、重命名、移动、删除、编辑、终端命令或备份恢复。后台服务器文件管理仍归 `ops-control` 和 `external-node-executor`。

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

### 职责边界

`cloudreve-sync` 负责 Cloudreve provider 配置摘要、provider 健康状态、目录同步任务、文件元数据快照、分享链接快照、分享解析、链接失效探测、旧快照降级、同步任务状态、任务步骤摘要、幂等记录、同步审计和后台自检摘要。

`cloudreve-sync` 不负责注册、登录、会话、角色能力点主数据、玩家资源主数据、资源审核发布、下载权限判定、真实文件内容代理、浏览器直连 Cloudreve 管理凭据、后台服务器文件操作、宿主机文件系统、节点守护进程、运维审批、通知主数据、Cloudreve 管理 token 展示或真实 Cloudreve 删除操作。

第一版固定为内存存储和受控 fake Cloudreve adapter。它只建立可测试的 API 边界、安全快照、降级规则和同步任务模型。后续若要让 `resource` 消费本服务快照，必须按前序服务兼容变更流程更新 `docs/contracts-resource.md`、`.local-docs/tests-resource.md` 和自动化测试，确认红灯后再改 `resource`。

### 数据归属

`cloudreve-sync` 拥有以下主数据：CloudreveProvider、CloudreveFileSnapshot、CloudreveShareSnapshot、CloudreveSyncJob、CloudreveSyncJobStep、CloudreveResolveResult、CloudreveAuditLog、CloudreveOpsSummary 和幂等记录。

`cloudreve-sync` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和状态快照；可以保存来自 `resource` 的资源兼容引用快照；可以保存来自 `ops-control` 的 Cloudreve 服务资产引用摘要。所有跨服务字段只能来自正式接口、后端入口可信上下文或契约允许的本地测试 stub，不能直接读取前序服务数据库、内存 store、测试种子或私有类。

Cloudreve 真实凭据只能通过环境变量、启动参数或受控配置注入。仓库内只能保存配置键名和测试假值，不得提交真实 token、cookie、刷新 token、管理密码、分享密码明文、私有直链、内部绝对路径或完整上游响应。

### 基础路径、端口和认证

所有接口默认使用 `/api/v1/cloudreve-sync` 前缀。第四批合并后当前运行入口为 `ops-core-service:8133`。历史原服务端口 `8118` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或测试入口。

健康检查 `GET /api/v1/cloudreve-sync/health` 不要求认证，但只能返回存活、版本、服务名和请求编号，不返回 provider ID、Cloudreve 地址、token 摘要、内部路径、能力明细、任务数量或上游错误。

除健康检查外，全部接口要求 `Authorization: Bearer <token>`。读取 provider、文件、分享、任务和自检摘要要求后台角色 `HELPER`、`ADMIN` 或 `OWNER`，且具备 `NODE_READ` 或 `FILE_MANAGE`。写入 provider、同步任务、分享解析和取消任务要求 `ADMIN` 或 `OWNER`，并按动作要求 `NODE_WRITE` 或 `FILE_MANAGE`。审计列表只允许 `ADMIN` 或 `OWNER`。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`credential`、`tokenDigest`、`rawToken`、`cookie`、`refreshToken`、`authorizationHeader`、`internalPath`、`resolvedPath`、`sharePassword`、`beforeState`、`afterState`、`auditResult`、`createdBy`、`updatedBy`、`lastSyncedAt`、`taskStatus` 等服务端可信字段。出现可信字段时返回 `40001`。

所有写接口都必须执行同一套可信字段拒绝规则，包括 provider 创建、provider 更新、provider 禁用、provider 启用、分享解析、同步任务创建和同步任务取消。禁用、启用和取消接口虽然请求体较小，也不能只校验 `reason`，必须拒绝浏览器伪造的操作者、审计、任务状态、内部路径和凭据字段。

### 本地测试控制头

`cloudreve-sync` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Cloudreve-Mode`、`X-Test-Resource-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Now` 模拟认证失败、上游不可用、上游超时、上游坏 schema、上游 401、资源兼容快照不可用、审计失败、状态写入失败和时间边界。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发认证失败、Cloudreve 失败、resource 失败、审计失败、存储失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 纳入生产化硬化项。

### 前序服务兼容契约

`auth` 是强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。auth 不可用返回 `46710`，auth 超时返回 `46711`，auth 字段或枚举不兼容返回 `46712`。

`resource` 是玩家资源主数据。`cloudreve-sync` 可以为 `resource` 提供 Cloudreve 文件和分享快照，但不能创建、修改或发布玩家资源，不能判断 `PUBLIC`、`AUTHENTICATED`、`MEMBER_ONLY` 或 `ADMIN_ONLY` 的下载权限，不能绕过 `resource` 直接给玩家返回下载结果。resource 兼容快照不可用返回 `46720`，resource 超时返回 `46721`，字段不兼容返回 `46722`。

`ops-control` 拥有 Cloudreve 服务资产和后台运维资产。`cloudreve-sync` 可以保存 Cloudreve 服务资产引用摘要，但不能把玩家资源权限当作服务器文件权限，也不能调用 `external-node-executor` 执行文件操作。ops-control 资产快照不可用时，provider 仍可读取已有摘要，但创建或更新资产引用返回 `46730`。

`notification` 只负责通知投递。同步失败、链接失效或 provider 异常是否通知由调用方策略决定。第一版只在审计中记录 `notificationHint`，不自建通知主数据。

### 枚举

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

### 通用对象

#### CloudreveProvider

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

#### CloudreveProviderSummary

字段为 `providerId`、`displayName`、`baseUrlSummary`、`authMode`、`status`、`capabilities`、`quotaTotalBytes`、`quotaUsedBytes`、`quotaUsagePercent`、`quotaWarningThresholdPercent`、`quotaStatus`、`estimatedMonthlyCostCents`、`pricingPlanSummary`、`lastHealthStatus`、`lastCheckedAt`、`lastSyncJobId`、`degraded`、`degradeReasons`、`createdAt` 和 `updatedAt`。摘要不得返回 token、cookie、刷新 token、管理密码、分享密码、Authorization 头、完整上游 URL 查询串或内部路径。

#### CloudreveFileSnapshot

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

#### CloudreveShareSnapshot

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

#### CloudreveSyncJob

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

#### CloudreveSyncJobStep

字段为 `stepId`、`name`、`status`、`dependencyStatus`、`startedAt`、`finishedAt`、`message` 和 `sanitizedPayloadSummary`。步骤摘要不得返回完整上游响应、token、cookie、分享密码、内部路径或异常堆栈。

#### CloudreveResolveResult

字段为 `providerId`、`fileId`、`shareSnapshotId`、`shareStatus`、`downloadAvailable`、`shareUrlSummary`、`expiresAt`、`stale`、`degraded`、`degradeReasons`、`resolvedAt` 和 `resourceCompatibility`。该对象只供后台或后续 `resource` 兼容适配使用，不等于玩家下载票据。

#### CloudreveAuditLog

审计字段继承公共契约，允许补充 `providerId`、`fileId`、`shareSnapshotId`、`jobId`、`dependencyStatus`、`stateFrom`、`stateTo`、`idempotencyKey` 和 `notificationHint`。审计列表不得提供删除接口。审计响应不得返回 Cloudreve token、cookie、刷新 token、管理密码、完整 Authorization 请求头、分享密码明文、私有直链、内部路径、完整上游响应或异常堆栈。

#### CloudreveOpsSummary

字段至少包含 `service`、`port`、`storageMode`、`authMode`、`providerAdapterMode`、`resourceAdapterMode`、`opsAssetAdapterMode`、`testControlsEnabled`、`providersTotal`、`filesTotal`、`sharesTotal`、`jobsTotal`、`runningJobsTotal`、`failedJobsTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`quotaTotalBytes`、`quotaUsedBytes`、`quotaUsagePercent`、`quotaWarningProvidersTotal`、`quotaExceededProvidersTotal`、`estimatedMonthlyCostCents`、`pricingModelSummary`、`lastSyncAt`、`lastFailureAt`、`degraded`、`degradeReasons` 和 `productionGaps`。

### 错误码

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

### 接口总览

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

### 健康和自检接口

`GET /api/v1/cloudreve-sync/health` 成功返回 `service=cloudreve-sync`、`status`、`version` 和 `requestId`。进程存活但上游不可用时仍可返回 HTTP `200`，并用 `status=DEGRADED` 标记。该接口不得泄露 provider、Cloudreve 地址、凭据摘要、文件数量或任务数量。

`GET /api/v1/cloudreve-sync/ops/summary` 成功返回 `CloudreveOpsSummary`。合并后必须返回 `port=8133`、`legacyPort=8118`、`storageMode=IN_MEMORY`、`providerAdapterMode=TEST_FAKE`、`resourceAdapterMode=TEST_STUB`、`opsAssetAdapterMode=TEST_STUB` 和生产化缺口。读取失败返回 `55300`，不得伪造健康。摘要不得返回 token、cookie、分享密码、完整 URL 查询串、后台备注、内部路径或审计原因全文。

自检摘要必须提供配额和成本估算摘要。第一版只根据 provider 快照计算 `quotaUsagePercent`、告警数量和 `estimatedMonthlyCostCents`，不连接真实账单、不生成扣费、不保存支付信息。配额达到告警阈值时 provider 返回 `quotaStatus=WARNING`，已用容量大于总容量时返回 `EXCEEDED`。同步读取旧快照仍允许，但写入类接口不得把超额状态伪装为健康。

`quotaStatus=EXCEEDED` 时，本服务必须进入容量保护模式。读取 provider、文件、分享、任务和审计仍然允许，`PROVIDER_HEALTH_CHECK` 仍然允许用于恢复状态；新的 `DIRECTORY_SYNC` 和 `SHARE_REFRESH` 必须返回 `49710`，避免继续扩大快照、分享刷新和上游资源消耗。`RESOURCE_LINK_VERIFY` 只写兼容摘要，不新增上游文件或分享，可继续按依赖状态处理。

### provider 接口

`GET /api/v1/cloudreve-sync/providers` 支持 `page`、`pageSize`、`keyword`、`status`、`authMode`、`capability` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`。成功响应分页 `items` 为 `CloudreveProviderSummary[]`。

`GET /api/v1/cloudreve-sync/providers/{providerId}` 返回 `CloudreveProvider`、最近任务摘要和降级原因。provider 不存在返回 `49700`。

`POST /api/v1/cloudreve-sync/providers` 请求字段为 `displayName`、`baseUrl`、`authMode`、`credential`、`capabilities`、`timeoutMs`、`opsAssetRef`、`quotaTotalBytes`、`quotaUsedBytes`、`quotaWarningThresholdPercent`、`estimatedMonthlyCostCents`、`pricingPlanSummary`、`enabled`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `CloudreveProvider`。`credential` 只写入受控配置或测试桩，不回显；响应只返回 `credentialStored=true` 或 `credentialRotated=true` 摘要。provider 名称冲突返回 `49710`。同一操作者、同一幂等键、同一请求体重复提交返回同一 provider，相同键不同体返回 `49712`。审计失败返回 `55301`，不得创建 provider。

`PATCH /api/v1/cloudreve-sync/providers/{providerId}` 可修改 `displayName`、`baseUrl`、`authMode`、`credential`、`capabilities`、`timeoutMs`、`opsAssetRef`、`quotaTotalBytes`、`quotaUsedBytes`、`quotaWarningThresholdPercent`、`estimatedMonthlyCostCents`、`pricingPlanSummary`、`reason` 和 `idempotencyKey`。`DISABLED` provider 可以更新配置摘要，但不能触发同步任务。更新凭据只记录轮换摘要，不返回原文。provider 不存在返回 `49700`，审计失败不得改变状态。

`opsAssetRef` 必须按请求体写入 provider 快照，创建和更新都要生效。服务端只保留安全摘要，至少允许 `assetId`、`assetType`、`displayName`、`source` 和 `syncedAt`，不得保存节点密钥、内部绝对路径、运维凭据或完整资产响应。未传 `opsAssetRef` 时使用默认 Cloudreve 服务资产摘要；传入 null 时清空引用摘要。

`PATCH /api/v1/cloudreve-sync/providers/{providerId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。禁用后状态为 `DISABLED`，不删除历史文件和分享快照，不允许创建新的同步任务。重复禁用保持幂等。

`PATCH /api/v1/cloudreve-sync/providers/{providerId}/enable` 请求字段为 `reason` 和 `idempotencyKey`。启用前校验配置摘要，成功后状态为 `ENABLED` 或在健康检查失败时 `DEGRADED`。`UNAVAILABLE` 且凭据无效的 provider 返回 `46703`。

### 文件和分享接口

`GET /api/v1/cloudreve-sync/files` 支持 `page`、`pageSize`、`providerId`、`parentPath`、`status`、`type`、`keyword`、`resourceId` 和 `sort`。`sort` 允许 `lastSyncedAt_desc`、`name_asc`、`sizeBytes_desc`。`parentPath` 必须以 `/` 开头，禁止 `..`、反斜杠、控制字符、URL 编码绕过和路径段前缀误判，路径非法返回 `49714`。provider 不存在返回 `49700`。

`GET /api/v1/cloudreve-sync/shares` 支持 `page`、`pageSize`、`providerId`、`fileId`、`status`、`downloadAvailable`、`keyword` 和 `sort`。`sort` 允许 `lastCheckedAt_desc`、`expiresAt_asc`、`createdAt_desc`。成功响应分页 `items` 为 `CloudreveShareSnapshot[]`。分享快照不得返回提取码明文、私有直链或完整上游响应。

`POST /api/v1/cloudreve-sync/shares/resolve` 请求字段为 `providerId`、`fileId`、`path`、`shareUrl`、`resourceRef`、`allowStale`、`reason` 和 `idempotencyKey`。`fileId`、`path` 和 `shareUrl` 至少传一个；`path` 必须通过 Cloudreve 路径守卫；`shareUrl` 只允许 http 或 https。成功返回 `CloudreveResolveResult`。provider 不存在返回 `49700`，文件不存在返回 `49701`，上游未授权返回 `46703`，上游不可用且 `allowStale=true` 且存在可用旧快照时返回成功并标记 `stale=true`、`degraded=true`；没有旧快照返回 `49713` 或 `46700`。同 key 同体返回同一解析结果，同 key 不同体返回 `49712`。审计失败不得写入新快照。

### 同步任务接口

`POST /api/v1/cloudreve-sync/sync-jobs` 请求字段为 `jobType`、`providerId`、`target`、`trigger`、`reason` 和 `idempotencyKey`。`DIRECTORY_SYNC` 的 `target` 至少包含 `path`；`SHARE_REFRESH` 至少包含 `shareSnapshotId` 或 `fileId`；`RESOURCE_LINK_VERIFY` 至少包含 `resourceRef`；`PROVIDER_HEALTH_CHECK` 只需要 provider，`target` 可以省略，由服务端生成 provider 目标摘要。成功响应 HTTP `201`，`data` 为 `CloudreveSyncJob`。

同步任务创建时必须校验 provider 存在且未禁用。`DISABLED` provider 返回 `49710`。路径越界返回 `49714`。同一操作者、同一 provider、同一 jobType、同一幂等键和同一请求体重复提交返回同一任务；相同键不同体返回 `49712`。审计失败或任务状态写入失败时不得返回成功。

第一版 fake adapter 可以同步完成任务，但接口语义仍按异步任务建模。`PROVIDER_HEALTH_CHECK` 可把 provider 更新为 `ENABLED`、`DEGRADED` 或 `UNAVAILABLE`。`DIRECTORY_SYNC` 可生成或更新文件快照。`SHARE_REFRESH` 可生成或更新分享快照。`RESOURCE_LINK_VERIFY` 只写入兼容摘要，不修改 `resource` 主数据。

`GET /api/v1/cloudreve-sync/sync-jobs` 支持 `page`、`pageSize`、`providerId`、`jobType`、`status`、`trigger`、`createdBy`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`finishedAt_desc`。成功响应分页 `items` 为 `CloudreveSyncJob[]`。

`from` 和 `to` 使用 UTC ISO-8601 时间，反向时间范围返回 `40001`。时间范围必须实际过滤任务 `createdAt`，不能只校验格式或只校验反向时间。

`GET /api/v1/cloudreve-sync/sync-jobs/{jobId}` 返回任务详情、步骤、结果摘要和失败原因。任务不存在返回 `49703`。

`PATCH /api/v1/cloudreve-sync/sync-jobs/{jobId}/cancel` 请求字段为 `reason` 和 `idempotencyKey`。只有 `PENDING` 和 `RUNNING` 任务可取消。`SUCCEEDED`、`FAILED`、`CANCELLED` 和 `TIMEOUT` 为终态，取消返回 `49711`。取消成功必须写审计；审计失败时任务状态保持不变。

### 审计接口

`GET /api/v1/cloudreve-sync/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`providerId`、`fileId`、`shareSnapshotId`、`jobId`、`action`、`result`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`。只有 `ADMIN` 和 `OWNER` 可访问。审计列表只读，不提供删除、修改或恢复接口。

审计中的 `requestId` 必须来自当前 HTTP 请求，不能使用固定占位值。后台写操作必须记录调用者、`reason`、操作前状态、操作后状态、请求编号、结果和失败原因。审计写入失败时，provider 创建修改启停、分享解析、同步任务创建和取消不得假装成功，必须返回 `55301` 并保持业务状态不变。

审计列表的 `from` 和 `to` 使用 UTC ISO-8601 时间，反向时间范围返回 `40001`。时间范围必须实际过滤审计 `createdAt`，不能把范围外审计混入结果。

### 状态、幂等和并发

provider 状态从 `ENABLED` 开始，健康检查失败可进入 `DEGRADED` 或 `UNAVAILABLE`，管理员可禁用为 `DISABLED`。`DISABLED` 不删除历史快照，只阻止新同步任务。启用 provider 需要配置摘要有效，凭据失效返回 `46703`。

同步任务状态为 `PENDING` 到 `RUNNING`、`CANCELLED`、`FAILED` 或 `TIMEOUT`；`RUNNING` 到 `SUCCEEDED`、`FAILED`、`CANCELLED` 或 `TIMEOUT`。`SUCCEEDED`、`FAILED`、`CANCELLED` 和 `TIMEOUT` 为终态，不得重新执行。

文件快照状态由同步任务更新。上游找不到文件时进入 `MISSING` 或 `DELETED_UPSTREAM`；权限不足进入 `INACCESSIBLE`；上游不可用但保留旧信息时进入 `STALE`。快照不能反向修改 `resource` 主数据。

分享快照状态由解析和刷新任务更新。上游不可用时，如果旧快照仍在契约允许窗口内，可以返回 `stale=true` 和 `degraded=true`；旧快照过期、禁用或不可用时返回 `49713`、`46700` 或 `46701`，不能伪造可用下载。

写接口使用幂等键时，必须使用字段名排序后的稳定 JSON 语义计算请求体指纹，嵌套对象按字段名递归排序，数组保留顺序，不能依赖 Java `Map.toString()` 或浏览器字段顺序。所有写接口必须用本服务内串行临界区保护状态推进、幂等记录、审计和响应快照。后续数据库实现必须使用事务、唯一约束、条件更新或等效机制，不能降低并发口径。

### 安全、降级和脱敏

路径安全必须拒绝 `..`、反斜杠、控制字符、空路径、非 `/` 开头路径、URL 编码绕过和路径段前缀误判。Cloudreve URI 和分享链接只保存安全摘要。分享密码只能保存受控摘要，不返回明文。

任何响应不得包含 Cloudreve token、cookie、刷新 token、管理密码、完整 Authorization 请求头、分享密码明文、私有直链、内部文件系统路径、完整上游响应、异常堆栈、数据库连接串、`.env`、`authorized_keys`、`id_rsa` 或服务器密码。

Cloudreve 不可用时，读取类接口可以返回旧快照并标记 `degraded=true`、`stale=true` 和 `degradeReasons`。写入类接口不得假装成功。`resource`、`ops-control` 或 `auth` 不可用时，必须返回明确依赖错误或降级摘要，不能使用浏览器字段伪造可信上下文。

### 验收口径

`cloudreve-sync` API 文档必须按 `docs/contracts-cloudreve-sync.md` 独立存在，并由 `.local-docs/tests-ops-core.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求、敏感字段脱敏、测试控制头默认关闭和模块验收口径。

`cloudreve-sync` 完成时必须满足以下条件：当前运行入口为 `ops-core-service:8133`，历史端口 `8118` 只作为 `legacyPort` 返回；健康检查不泄露敏感信息；除健康检查外全部接口要求后台认证；provider、文件快照、分享快照、同步任务、幂等、状态流转、上游失败降级、旧快照降级、配额成本摘要、审计失败回滚、敏感字段脱敏、测试控制头默认关闭和自检摘要都有自动化验证；不修改前序服务稳定接口；不把玩家资源主数据、后台文件管理、节点守护进程或真实宿主机操作塞进本模块；自动化测试必须先红灯；实现后 `cloudreve-sync` 在 `ops-core-service` 中全部测试通过；当前后端运行入口回归测试通过；边界扫描无违规命中；测试过程记录完整。

## 北冥官网 backup-recovery API 契约

来源：`docs/contracts-backup-recovery.md`

版本：0.1

### 文档定位

本文档是 `backup-recovery` 微服务的正式 API 契约。`backup-recovery` 负责备份域、备份策略、备份任务、备份点索引、备份校验、恢复演练、恢复申请、审批摘要、保留策略、加密摘要、依赖健康摘要、风险审计和自检摘要。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `backup-recovery` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、任务流转、恢复流转、失败降级、审计和验收口径。

`backup-recovery` 不是 `ops-control` 的任务子页面，也不是 `external-node-executor` 的执行器。第一版只做安全模拟和控制面快照，不执行真实数据库导出、真实文件复制、真实 Cloudreve 操作、真实备份删除、真实恢复写入、真实 shell 命令或真实节点调用。真实执行只能在后续独立闭环中，通过 `ops-control` 审批、`external-node-executor` 授权任务、路径限制、完整性校验和回滚审计再打开。

本文档参考 GitHub Enterprise Server Backup Utilities、GitLab 备份恢复、AWS Backup、AWS Backup Restore Testing 和 Velero 的公开设计。GitHub Enterprise Server 强调独立备份主机、异地存放和版本兼容；GitLab 强调关键数据、灾备、回滚、迁移和测试环境；AWS Backup 把备份计划、保留生命周期、恢复点和恢复测试分开；Velero 把备份、计划、恢复对象、恢复顺序和对象存储数据分开。本项目只吸收策略、恢复点、恢复演练、异地冗余、状态机、审计和非破坏性恢复这些思路，不接入这些平台的主数据。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [GitHub Enterprise Server Backup Utilities](https://docs.github.com/en/enterprise-server%403.17/admin/backing-up-and-restoring-your-instance/configuring-backups-on-your-instance?learn=increase_fault_tolerance&learnProduct=admin) | 备份主机应和主实例分离，异地存放，工具版本要和实例版本保持兼容。 |
| [GitLab Backup and Restore](https://docs.gitlab.com/administration/backup_restore/) | 备份恢复服务要覆盖数据保护、灾难恢复、历史快照、合规、迁移和测试开发用途。 |
| [AWS Backup Plans](https://docs.aws.amazon.com/aws-backup/latest/devguide/about-backup-plans.html) | 备份计划定义资源、窗口、保留生命周期和恢复点管理，适合本服务拆分策略和任务。 |
| [AWS Backup Restore Testing](https://docs.aws.amazon.com/aws-backup/latest/devguide/restore-testing.html) | 恢复测试应和真实恢复分离，测试资源要带标记、可清理、可审计，并暴露验证结果。 |
| [Velero Backup Reference](https://velero.io/docs/v1.18/backup-reference/) | 备份支持排除项、计划触发、手动触发、分页和删除语义区分。 |
| [Velero Restore Reference](https://velero.io/docs/v1.18/restore-reference/) | 恢复是独立对象，创建后由控制器校验、读取备份元数据、排序并执行恢复流程。 |

### 职责边界

`backup-recovery` 负责备份域注册视图、策略管理、策略启停、按策略创建备份任务、任务列表与详情、任务取消、备份点索引、备份点详情、备份点校验、恢复演练、恢复申请、恢复审批摘要、恢复拒绝、审计列表和服务自检摘要。

`backup-recovery` 不负责用户登录、角色能力点主数据、业务模块主数据、真实数据库导出、真实文件复制、真实对象存储写入、真实 Cloudreve 管理、真实服务器文件操作、真实 shell 命令、真实容器或虚拟机控制、真实备份删除、真实恢复执行、节点守护进程执行逻辑、玩家资源下载或通知主数据。

第一版使用内存存储和受控 fake adapter。它的目标是把备份恢复 API、权限、状态机、审批、审计、脱敏和失败降级先定稳。所有恢复申请审批通过后只能进入 `COMPLETED_SIMULATED` 或 `EXECUTION_BLOCKED`，不得写入业务服务。

### 数据归属

`backup-recovery` 拥有以下主数据：BackupDomain、BackupPolicy、BackupJob、BackupPoint、BackupVerification、RestoreDrill、RestoreRequest、BackupRecoveryAuditLog、BackupRecoveryOpsSummary 和幂等记录。

`backup-recovery` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和状态快照；可以保存来自 `ops-control` 的节点、资产、备份盘和受控任务引用摘要；可以保存来自 `admin` 的模块健康聚合引用；可以保存来自 `notification` 的投递降级摘要。所有跨服务字段只能来自正式接口、后端入口可信上下文或契约允许的本地测试 stub，不能直接读取前序服务数据库、内存 store、测试种子或私有类。

备份点中的 `storageRef` 只能返回安全摘要，例如存储别名、区域摘要、保留分层和加密模式。不得返回真实绝对路径、数据库连接串、对象存储密钥、加密密钥、节点 token、Cloudreve 管理凭据或完整上游响应。

### 基础路径、端口和认证

所有接口默认使用 `/api/v1/backup-recovery` 前缀。第四批合并后当前运行入口为 `ops-core-service:8133`。历史原服务端口 `8119` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或测试入口。

健康检查 `GET /api/v1/backup-recovery/health` 不要求认证，但只能返回存活、版本、服务名和请求编号，不返回策略数量、备份点数量、存储摘要、节点摘要、内部路径或依赖错误细节。

除健康检查外，全部接口要求 `Authorization: Bearer <token>`。读取类接口要求后台角色 `HELPER`、`ADMIN` 或 `OWNER`，并具备 `NODE_READ` 或 `HIGH_RISK_APPROVE`。创建、更新、启停策略和创建备份任务要求 `ADMIN` 或 `OWNER`，并具备 `NODE_WRITE`。校验备份点、创建恢复演练、创建恢复申请、审批和拒绝恢复申请要求 `ADMIN` 或 `OWNER`，并具备 `HIGH_RISK_APPROVE`，其中恢复申请审批为 `CRITICAL` 风险。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`beforeState`、`afterState`、`auditResult`、`internalPath`、`resolvedPath`、`rawToken`、`credential`、`secretKey`、`backupEncryptionKey`、`nodeToken`、`taskStatus`、`createdBy`、`updatedBy`、`verifiedBy`、`approvedBy`、`finishedAt` 等服务端可信字段。出现可信字段时返回 `40001`。

### 本地测试控制头

`backup-recovery` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Ops-Control-Mode`、`X-Test-Notification-Mode`、`X-Test-Backup-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Now` 模拟认证失败、运维控制面不可用、通知不可用、备份 adapter 失败、超时、待审批、审计失败、状态写入失败和时间边界。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发认证失败、ops-control 失败、notification 失败、备份任务失败、审计失败、存储失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 纳入生产化硬化项。

### 前序服务兼容契约

`auth` 是强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。auth 不可用返回 `46810`，auth 超时返回 `46811`，auth 字段或枚举不兼容返回 `46812`。

`admin` 是后台聚合入口。`backup-recovery` 可以向 admin 暴露模块健康、待办摘要、恢复申请待审批数量和审计摘要，但不能让 admin 修改备份主状态。admin 不可用时，自检摘要可以返回降级摘要，业务写操作不得伪造 admin 已同步成功。

`admin` 目前的稳定契约尚未声明 `BACKUP_RECOVERY` 模块入口。本轮不得直接修改 admin 稳定接口或让 admin 写入备份恢复主状态。backup-recovery 完成本轮闭环后，如果需要后台聚合入口，必须作为 admin 的兼容增强单独走文档、测试红灯、实现和回归流程，且只能增加只读入口、待办摘要和审计索引摘要。

`ops-control` 是运维控制面。`backup-recovery` 可以读取节点、资产、备份盘和任务摘要快照，也可以保存 `opsControlTaskRef` 摘要。第一版不得直接调用 `external-node-executor`，不得通过 `ops-control` 真实执行 `BACKUP_RESTORE`。ops-control 不可用返回 `46820`，超时返回 `46821`，字段不兼容返回 `46822`。

`external-node-executor` 只接受 `ops-control` 已授权任务。`backup-recovery` 第一版不得直接调用 `external-node-executor`。

`notification` 是辅助依赖。备份失败、恢复演练失败、恢复申请待审批和高风险完成可以形成通知提示。通知失败只记录降级摘要，不能改变备份任务、备份点或恢复申请主状态。notification 不可用返回降级摘要或 `46830`，不得导致已完成的备份任务被改成失败。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `BackupDomainType` | `DATABASE_AUTH`、`DATABASE_PROFILE`、`UPLOAD_CONTENT`、`RESOURCE_METADATA`、`INVITATION_DATA`、`WHITELIST_AUDIT`、`ATTENDANCE_LEDGER`、`PUNISHMENT_RECORD`、`REVIEW_RECORD`、`OPS_CONTROL_CONFIG`、`OPS_AUDIT_INDEX`、`CLOUDREVE_SNAPSHOT` | 备份域。第一版只做域摘要，不读取真实数据。 |
| `BackupDomainCriticality` | `LOW`、`MEDIUM`、`HIGH`、`CRITICAL` | 备份域重要性。 |
| `BackupPolicyStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`ARCHIVED` | 策略状态。 |
| `BackupTrigger` | `ADMIN_MANUAL`、`SCHEDULED`、`PRE_CHANGE_SAFETY_POINT`、`TEST_CONTROL` | 任务触发来源。 |
| `BackupJobStatus` | `PENDING`、`RUNNING`、`SUCCEEDED`、`FAILED`、`CANCELLED`、`TIMEOUT`、`PENDING_APPROVAL` | 备份任务状态。 |
| `BackupPointStatus` | `AVAILABLE`、`VERIFYING`、`VERIFIED`、`CORRUPTED`、`EXPIRED`、`DELETED_LOGICAL`、`INACCESSIBLE` | 备份点状态。 |
| `BackupVerificationStatus` | `PENDING`、`RUNNING`、`PASSED`、`FAILED`、`TIMEOUT` | 校验状态。 |
| `RestoreDrillStatus` | `PENDING`、`RUNNING`、`PASSED`、`FAILED`、`TIMEOUT`、`CANCELLED` | 恢复演练状态。 |
| `RestoreMode` | `DRY_RUN`、`SANDBOX_RESTORE`、`FULL_RESTORE_BLOCKED` | 第一版只允许 `DRY_RUN` 和 `SANDBOX_RESTORE`，`FULL_RESTORE_BLOCKED` 用于表达真实恢复被阻断。 |
| `RestoreRequestStatus` | `DRAFT`、`PENDING_APPROVAL`、`APPROVED`、`REJECTED`、`DRILL_REQUIRED`、`EXECUTION_BLOCKED`、`COMPLETED_SIMULATED`、`CANCELLED` | 恢复申请状态。 |
| `BackupRecoveryAuditResult` | `SUCCESS`、`FAILED` | 审计结果。 |
| `BackupDependencyStatus` | `AVAILABLE`、`UNAVAILABLE`、`TIMEOUT`、`BAD_SCHEMA`、`DISABLED` | 依赖摘要。 |

### 通用对象

#### BackupDomain

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `domainKey` | string | 是 | 备份域键。 |
| `displayName` | string | 是 | 展示名。 |
| `sourceService` | string | 是 | 来源服务，例如 `auth`、`resource`、`ops-control`。 |
| `domainType` | string | 是 | `BackupDomainType`。 |
| `criticality` | string | 是 | 重要性。 |
| `enabled` | boolean | 是 | 是否可被策略选择。 |
| `dependencySummary` | object | 是 | 依赖摘要，只返回状态和安全说明。 |
| `lastBackupPointId` | string 或 null | 是 | 最近备份点。 |
| `lastVerifiedAt` | string 或 null | 是 | 最近校验时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### BackupPolicy

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `policyId` | string | 是 | 策略 ID。 |
| `displayName` | string | 是 | 展示名，2 到 80 位。 |
| `domains` | string[] | 是 | 备份域键，至少一个。 |
| `scheduleSummary` | object | 是 | 计划摘要，包含 `mode`、`cron`、`timezone` 和 `windowMinutes`。 |
| `retentionDays` | integer | 是 | 保留天数，1 到 3650。 |
| `minimumCopies` | integer | 是 | 最少保留份数，1 到 30。 |
| `storageRef` | object | 是 | 存储安全摘要。 |
| `encryptionMode` | string | 是 | `NONE`、`MANAGED_KEY` 或 `EXTERNAL_KMS_SUMMARY`。不得返回密钥。 |
| `status` | string | 是 | `BackupPolicyStatus`。 |
| `lastRunStatus` | string 或 null | 是 | 最近任务状态。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### BackupJob

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `jobId` | string | 是 | 备份任务 ID。 |
| `policyId` | string | 是 | 关联策略。 |
| `trigger` | string | 是 | `BackupTrigger`。 |
| `status` | string | 是 | `BackupJobStatus`。 |
| `domains` | string[] | 是 | 本次任务覆盖域。 |
| `startedAt` | string 或 null | 是 | 开始时间。 |
| `finishedAt` | string 或 null | 是 | 完成时间。 |
| `resultSummary` | object 或 null | 是 | 结果摘要，包含备份点和大小估算。 |
| `failureReason` | string 或 null | 是 | 脱敏失败原因。 |
| `idempotencyKey` | string 或 null | 是 | 幂等键。 |
| `opsControlTaskRef` | object 或 null | 是 | ops-control 任务摘要，不代表真实执行。 |
| `createdBy` | string | 是 | 创建者。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### BackupPoint

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `backupPointId` | string | 是 | 备份点 ID。 |
| `policyId` | string | 是 | 策略 ID。 |
| `jobId` | string | 是 | 来源任务 ID。 |
| `domains` | string[] | 是 | 覆盖域。 |
| `storageRef` | object | 是 | 存储安全摘要，不返回真实路径或密钥。 |
| `checksumSummary` | object | 是 | 校验摘要，包含算法和摘要前缀。 |
| `sizeBytes` | integer | 是 | 估算大小。 |
| `encrypted` | boolean | 是 | 是否加密。 |
| `verified` | boolean | 是 | 是否校验通过。 |
| `verifiedAt` | string 或 null | 是 | 校验时间。 |
| `expiresAt` | string | 是 | 过期时间。 |
| `status` | string | 是 | `BackupPointStatus`。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |
| `createdAt` | string | 是 | 创建时间。 |

#### BackupVerification

字段为 `verificationId`、`backupPointId`、`status`、`validationSummary`、`failureReason`、`startedAt`、`finishedAt`、`createdBy` 和 `createdAt`。校验只读备份点摘要和校验摘要，不执行恢复写入。

#### RestoreDrill

字段为 `drillId`、`backupPointId`、`domains`、`status`、`validationSummary`、`startedAt`、`finishedAt`、`failureReason`、`createdBy` 和 `createdAt`。第一版恢复演练只在 fake sandbox 中模拟校验，不写业务服务。

#### RestoreRequest

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `restoreRequestId` | string | 是 | 恢复申请 ID。 |
| `backupPointId` | string | 是 | 目标备份点。 |
| `domains` | string[] | 是 | 恢复域。 |
| `restoreMode` | string | 是 | `RestoreMode`。 |
| `riskLevel` | string | 是 | `HIGH` 或 `CRITICAL`。 |
| `status` | string | 是 | `RestoreRequestStatus`。 |
| `approvalSummary` | object 或 null | 是 | 审批摘要。 |
| `requestedBy` | string | 是 | 申请人。 |
| `approvedBy` | string 或 null | 是 | 审批人。 |
| `reason` | string | 是 | 申请原因。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### BackupRecoveryAuditLog

审计字段继承公共契约，允许补充 `policyId`、`jobId`、`backupPointId`、`verificationId`、`drillId`、`restoreRequestId`、`dependencyStatus`、`stateFrom`、`stateTo`、`idempotencyKey` 和 `notificationHint`。审计列表不得提供删除接口。审计响应不得返回真实路径、数据库连接串、对象存储凭据、加密密钥、节点 token、完整请求头、备份内容、异常堆栈或恢复参数全文。

#### BackupRecoveryOpsSummary

字段至少包含 `service`、`port`、`storageMode`、`authMode`、`opsControlAdapterMode`、`notificationAdapterMode`、`backupAdapterMode`、`testControlsEnabled`、`domainsTotal`、`policiesTotal`、`enabledPoliciesTotal`、`jobsTotal`、`backupPointsTotal`、`verifiedBackupPointsTotal`、`restoreDrillsTotal`、`restoreRequestsTotal`、`pendingRestoreRequestsTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastSuccessfulBackupAt`、`lastFailedBackupAt`、`degraded`、`degradeReasons` 和 `productionGaps`。

### 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `46810` | 502 | auth 认证上下文不可用。 |
| `46811` | 504 | auth 认证上下文调用超时。 |
| `46812` | 502 | auth 认证上下文字段不兼容。 |
| `46820` | 502 | ops-control 资产或任务摘要不可用。 |
| `46821` | 504 | ops-control 调用超时。 |
| `46822` | 502 | ops-control 响应字段不兼容。 |
| `46830` | 502 | notification 投递摘要不可用。 |
| `46840` | 502 | 备份 adapter 不可用。 |
| `46841` | 504 | 备份 adapter 超时。 |
| `49800` | 404 | 备份域、策略、任务、备份点、校验、演练、恢复申请或审计不存在。 |
| `49801` | 404 | 备份策略不存在。 |
| `49802` | 404 | 备份任务不存在。 |
| `49803` | 404 | 备份点不存在。 |
| `49804` | 404 | 恢复申请不存在。 |
| `49810` | 409 | 策略、任务、备份点或恢复申请状态不允许当前操作。 |
| `49811` | 409 | 备份策略名称或域组合冲突。 |
| `49812` | 409 | 幂等键请求指纹冲突。 |
| `49813` | 409 | 备份点校验失败或不可用于恢复。 |
| `49814` | 409 | 恢复申请缺少通过的恢复演练。 |
| `55400` | 500 | backup-recovery 内部错误。 |
| `55401` | 500 | backup-recovery 审计写入失败。 |
| `55402` | 500 | backup-recovery 状态写入失败。 |

字段校验、未登录、令牌格式错误、角色权限不足、能力点不足、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/backup-recovery/health` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/backup-recovery/ops/summary` | 是 | `NODE_READ` 或 `HIGH_RISK_APPROVE` | LOW |
| 备份域列表 | GET | `/api/v1/backup-recovery/domains` | 是 | `NODE_READ` | LOW |
| 策略列表 | GET | `/api/v1/backup-recovery/policies` | 是 | `NODE_READ` | LOW |
| 策略详情 | GET | `/api/v1/backup-recovery/policies/{policyId}` | 是 | `NODE_READ` | LOW |
| 创建策略 | POST | `/api/v1/backup-recovery/policies` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 更新策略 | PATCH | `/api/v1/backup-recovery/policies/{policyId}` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 启用策略 | PATCH | `/api/v1/backup-recovery/policies/{policyId}/enable` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 禁用策略 | PATCH | `/api/v1/backup-recovery/policies/{policyId}/disable` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 创建备份任务 | POST | `/api/v1/backup-recovery/jobs` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | HIGH |
| 任务列表 | GET | `/api/v1/backup-recovery/jobs` | 是 | `NODE_READ` | LOW |
| 任务详情 | GET | `/api/v1/backup-recovery/jobs/{jobId}` | 是 | `NODE_READ` | LOW |
| 取消任务 | PATCH | `/api/v1/backup-recovery/jobs/{jobId}/cancel` | 是 | 创建者、`ADMIN` 或 `OWNER` | MEDIUM |
| 备份点列表 | GET | `/api/v1/backup-recovery/backup-points` | 是 | `NODE_READ` | LOW |
| 备份点详情 | GET | `/api/v1/backup-recovery/backup-points/{backupPointId}` | 是 | `NODE_READ` | LOW |
| 校验备份点 | POST | `/api/v1/backup-recovery/backup-points/{backupPointId}/verify` | 是 | `HIGH_RISK_APPROVE`，`ADMIN` 或 `OWNER` | HIGH |
| 创建恢复演练 | POST | `/api/v1/backup-recovery/restore-drills` | 是 | `HIGH_RISK_APPROVE`，`ADMIN` 或 `OWNER` | HIGH |
| 恢复演练列表 | GET | `/api/v1/backup-recovery/restore-drills` | 是 | `NODE_READ` | LOW |
| 恢复演练详情 | GET | `/api/v1/backup-recovery/restore-drills/{drillId}` | 是 | `NODE_READ` | LOW |
| 创建恢复申请 | POST | `/api/v1/backup-recovery/restore-requests` | 是 | `HIGH_RISK_APPROVE`，`ADMIN` 或 `OWNER` | CRITICAL |
| 恢复申请列表 | GET | `/api/v1/backup-recovery/restore-requests` | 是 | `NODE_READ` 或 `HIGH_RISK_APPROVE` | LOW |
| 恢复申请详情 | GET | `/api/v1/backup-recovery/restore-requests/{restoreRequestId}` | 是 | `NODE_READ` 或 `HIGH_RISK_APPROVE` | LOW |
| 审批恢复申请 | PATCH | `/api/v1/backup-recovery/restore-requests/{restoreRequestId}/approve` | 是 | `HIGH_RISK_APPROVE` 或 `OWNER` | CRITICAL |
| 拒绝恢复申请 | PATCH | `/api/v1/backup-recovery/restore-requests/{restoreRequestId}/reject` | 是 | `HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 审计列表 | GET | `/api/v1/backup-recovery/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

### 健康、自检和备份域接口

`GET /api/v1/backup-recovery/health` 成功返回 `service=backup-recovery`、`status`、`version` 和 `requestId`。进程存活但依赖不可用时仍可返回 HTTP `200`，并用 `status=DEGRADED` 标记。该接口不得泄露策略、备份点、存储引用、节点、内部路径或依赖错误细节。

`GET /api/v1/backup-recovery/ops/summary` 成功返回 `BackupRecoveryOpsSummary`。合并后必须返回 `port=8133`、`legacyPort=8119`、`storageMode=IN_MEMORY`、`backupAdapterMode=SIMULATED`、`opsControlAdapterMode=TEST_STUB`、`notificationAdapterMode=TEST_STUB` 和生产化缺口。读取失败返回 `55400`，不得伪造健康。

自检摘要必须暴露正式系统设计同步状态。`productionGaps` 在第一版至少包含真实持久化未接入、真实备份介质未接入、真实跨服务 HTTP 未接入、真实恢复执行被阻断、admin 只读入口未适配和 external-node-executor 直连禁止等项。该摘要用于提醒后续闭环，不允许前端把这些缺口当作可执行能力。

`GET /api/v1/backup-recovery/domains` 支持 `page`、`pageSize`、`keyword`、`sourceService`、`criticality`、`enabled` 和 `sort`。`sort` 允许 `updatedAt_desc`、`displayName_asc` 和 `criticality_desc`。成功响应分页 `items` 为 `BackupDomain[]`。备份域只表达可备份范围，不读取真实数据。

### 策略接口

`GET /api/v1/backup-recovery/policies` 支持 `page`、`pageSize`、`keyword`、`status`、`domain` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`。成功响应分页 `items` 为 `BackupPolicy[]`。

`GET /api/v1/backup-recovery/policies/{policyId}` 返回策略详情、最近任务和最近备份点摘要。策略不存在返回 `49801`。

`POST /api/v1/backup-recovery/policies` 请求字段为 `displayName`、`domains`、`scheduleSummary`、`retentionDays`、`minimumCopies`、`storageRef`、`encryptionMode`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `BackupPolicy`。策略名称冲突或同域同存储策略冲突返回 `49811`。同一操作者、同一幂等键、同一请求体重复提交返回同一策略，相同键不同体返回 `49812`。审计失败返回 `55401`，不得创建策略。

`PATCH /api/v1/backup-recovery/policies/{policyId}` 可修改 `displayName`、`domains`、`scheduleSummary`、`retentionDays`、`minimumCopies`、`storageRef`、`encryptionMode`、`reason` 和 `idempotencyKey`。`ARCHIVED` 策略不可修改。更新后必须写审计。

`PATCH /api/v1/backup-recovery/policies/{policyId}/enable` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT` 和 `DISABLED` 可启用为 `ENABLED`。`ARCHIVED` 返回 `49810`。重复启用保持幂等。

`PATCH /api/v1/backup-recovery/policies/{policyId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 可禁用为 `DISABLED`。禁用后不影响历史备份点读取。重复禁用保持幂等。

### 任务和备份点接口

`POST /api/v1/backup-recovery/jobs` 请求字段为 `policyId`、`trigger`、`domains`、`reason`、`idempotencyKey` 和可选 `opsControlTaskRef`。策略必须存在且为 `ENABLED`，否则返回 `49810`。第一版 fake adapter 可以同步完成为 `SUCCEEDED`，也可以在本地测试控制下返回 `FAILED`、`TIMEOUT` 或 `PENDING_APPROVAL`。任务成功时生成 `BackupPoint`。任务失败不得生成可用备份点。

`GET /api/v1/backup-recovery/jobs` 支持 `page`、`pageSize`、`policyId`、`status`、`trigger`、`createdBy`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`finishedAt_desc`。时间范围必须实际过滤任务 `createdAt`。

`GET /api/v1/backup-recovery/jobs/{jobId}` 返回任务详情。任务不存在返回 `49802`。

`PATCH /api/v1/backup-recovery/jobs/{jobId}/cancel` 请求字段为 `reason` 和 `idempotencyKey`。只有 `PENDING`、`RUNNING` 和 `PENDING_APPROVAL` 可取消。`SUCCEEDED`、`FAILED`、`CANCELLED` 和 `TIMEOUT` 为终态，取消返回 `49810`。

`GET /api/v1/backup-recovery/backup-points` 支持 `page`、`pageSize`、`policyId`、`jobId`、`domain`、`status`、`verified`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`expiresAt_asc`、`sizeBytes_desc`。时间范围必须实际过滤备份点 `createdAt`。

`GET /api/v1/backup-recovery/backup-points/{backupPointId}` 返回备份点详情。备份点不存在返回 `49803`。响应不得返回真实路径、密钥、连接串、完整对象存储地址或备份内容。

`POST /api/v1/backup-recovery/backup-points/{backupPointId}/verify` 请求字段为 `validationLevel`、`reason` 和 `idempotencyKey`。`validationLevel` 允许 `METADATA_ONLY`、`CHECKSUM` 和 `SANDBOX_READ`。`AVAILABLE` 或 `VERIFIED` 备份点可校验。`CORRUPTED`、`EXPIRED`、`DELETED_LOGICAL` 和 `INACCESSIBLE` 返回 `49813`。校验通过后备份点状态为 `VERIFIED`，失败时为 `CORRUPTED` 或保持原状态并记录失败摘要，同一实现版本内必须固定。

### 恢复演练和恢复申请接口

`POST /api/v1/backup-recovery/restore-drills` 请求字段为 `backupPointId`、`domains`、`validationPlan`、`reason` 和 `idempotencyKey`。备份点必须 `VERIFIED` 或 `AVAILABLE` 且未过期。第一版只模拟沙箱校验，不恢复真实业务数据。成功响应 HTTP `201`，`data` 为 `RestoreDrill`。

`GET /api/v1/backup-recovery/restore-drills` 支持 `page`、`pageSize`、`backupPointId`、`status`、`createdBy`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`finishedAt_desc`。

`GET /api/v1/backup-recovery/restore-drills/{drillId}` 返回演练详情。演练不存在返回 `49800`。

`POST /api/v1/backup-recovery/restore-requests` 请求字段为 `backupPointId`、`domains`、`restoreMode`、`drillId`、`impactSummary`、`confirmText`、`reason` 和 `idempotencyKey`。`confirmText` 必须为 `REQUEST_RESTORE_REVIEW`。`SANDBOX_RESTORE` 要求目标备份点有通过的恢复演练，缺少时返回 `49814`。第一版 `FULL_RESTORE_BLOCKED` 只能创建为 `EXECUTION_BLOCKED` 或字段校验失败，不得真实恢复。

恢复申请的 `domains` 必须是目标备份点 `domains` 的子集，不能申请恢复备份点不包含的数据域。`impactSummary.writesProduction` 在第一版必须为 `false`；出现 `true` 时返回 `40001` 或创建为 `EXECUTION_BLOCKED`，同一实现版本内必须固定并写入测试。申请请求、审批请求和响应都不得包含真实恢复目标路径、数据库连接、对象存储路径、节点地址或 shell 命令。

`GET /api/v1/backup-recovery/restore-requests` 支持 `page`、`pageSize`、`backupPointId`、`status`、`requestedBy`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`riskLevel_desc`。

`GET /api/v1/backup-recovery/restore-requests/{restoreRequestId}` 返回申请详情。申请不存在返回 `49804`。

`PATCH /api/v1/backup-recovery/restore-requests/{restoreRequestId}/approve` 请求字段为 `reviewComment`、`confirmText`、`reason` 和 `idempotencyKey`。`confirmText` 必须为 `APPROVE_SIMULATED_RESTORE`。只有 `PENDING_APPROVAL` 可审批。审批人不能审批自己创建的 `CRITICAL` 申请，返回 `49810`。审批通过后第一版只进入 `COMPLETED_SIMULATED` 或 `EXECUTION_BLOCKED`，并写入审批摘要。

审批通过不得创建 `ops-control` 的真实 `BACKUP_RESTORE` 任务，不得调用 `external-node-executor`，不得修改任何业务模块数据。响应中的 `approvalSummary` 必须明确 `executionMode=SIMULATED_ONLY` 或 `executionMode=BLOCKED_BY_CONTRACT`，方便前端和审计区分审批完成与真实恢复完成。

`PATCH /api/v1/backup-recovery/restore-requests/{restoreRequestId}/reject` 请求字段为 `reviewComment`、`reason` 和 `idempotencyKey`。只有 `PENDING_APPROVAL` 可拒绝。拒绝后状态为 `REJECTED`。

### 审计接口

`GET /api/v1/backup-recovery/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`policyId`、`jobId`、`backupPointId`、`drillId`、`restoreRequestId`、`action`、`result`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`、`riskLevel_desc`。只有 `ADMIN` 和 `OWNER` 可访问。

审计中的 `requestId` 必须来自当前 HTTP 请求，不能使用固定占位值。后台写操作必须记录调用者、`reason`、操作前状态、操作后状态、请求编号、结果和失败原因。审计写入失败时，策略创建修改启停、任务创建取消、备份点校验、恢复演练、恢复申请创建、审批和拒绝不得假装成功，必须返回 `55401` 并保持业务状态不变。

### 状态、幂等和并发

策略状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

任务状态流转为 `PENDING` 到 `RUNNING`、`CANCELLED`、`FAILED` 或 `PENDING_APPROVAL`；`RUNNING` 到 `SUCCEEDED`、`FAILED`、`CANCELLED` 或 `TIMEOUT`；`PENDING_APPROVAL` 到 `RUNNING`、`FAILED` 或 `CANCELLED`；`SUCCEEDED`、`FAILED`、`CANCELLED` 和 `TIMEOUT` 为终态。

备份点状态流转为 `AVAILABLE` 到 `VERIFYING`、`VERIFIED`、`CORRUPTED`、`EXPIRED`、`INACCESSIBLE` 或 `DELETED_LOGICAL`；`VERIFIED` 可因后续校验失败进入 `CORRUPTED`；`EXPIRED` 和 `DELETED_LOGICAL` 第一版只做逻辑状态，不删除真实数据。

恢复申请状态流转为 `PENDING_APPROVAL` 到 `APPROVED`、`REJECTED`、`DRILL_REQUIRED`、`EXECUTION_BLOCKED` 或 `COMPLETED_SIMULATED`。第一版不得进入真实执行成功状态。

写接口使用幂等键时，必须使用字段名排序后的稳定 JSON 语义计算请求体指纹，嵌套对象按字段名递归排序，数组保留顺序，不能依赖 Java `Map.toString()` 或浏览器字段顺序。所有写接口必须用本服务内串行临界区保护状态推进、幂等记录、审计和响应快照。后续数据库实现必须使用事务、唯一约束、条件更新或等效机制，不能降低并发口径。

### 安全、降级和脱敏

任何响应不得包含真实文件路径、数据库连接串、对象存储凭据、加密密钥、节点 token、Cloudreve 管理 token、完整 Authorization 请求头、备份内容、恢复参数全文、异常堆栈、`.env`、`authorized_keys`、`id_rsa`、服务器密码或 shell 命令。

外部依赖不可用时，读取类接口可以返回已有快照并标记 `degraded=true` 和 `degradeReasons`。写入类接口不得假装成功。备份 adapter 失败时，任务必须明确为 `FAILED` 或接口返回 `46840`、`46841`。notification 失败只影响通知提示，不改变备份任务和恢复申请主状态。

第一版不得提供真实删除备份点接口。确需清理过期备份点时，只能在后续独立契约中增加逻辑删除或保留策略执行接口，并重新完成文档、测试红灯、实现和回归闭环。

### 验收口径

`backup-recovery` API 文档必须按 `docs/contracts-backup-recovery.md` 独立存在，并由 `.local-docs/tests-ops-core.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求、敏感字段脱敏、测试控制头默认关闭和模块验收口径。

`backup-recovery` 完成时必须满足以下条件：当前运行入口为 `ops-core-service:8133`，历史端口 `8119` 只作为 `legacyPort` 返回；健康检查不泄露敏感信息；除健康检查外全部接口要求后台认证；备份域、策略、任务、备份点、校验、恢复演练、恢复申请、审批、审计、幂等、状态流转、依赖失败降级、审计失败回滚、敏感字段脱敏、测试控制头默认关闭和自检摘要都有自动化验证；不修改前序服务稳定接口；不直接调用 `external-node-executor`；不执行真实恢复；不把备份恢复能力塞回 `ops-control`、`admin`、`resource` 或 `cloudreve-sync`；自动化测试必须先红灯；实现后 `backup-recovery` 在 `ops-core-service` 中全部测试通过；当前后端运行入口回归测试通过；边界扫描无违规命中；测试过程记录完整。

## 北冥官网 alerting API 契约

来源：`docs/contracts-alerting.md`

版本：0.3

### 文档定位

本文档是 `alerting` 微服务的正式 API 契约。`alerting` 负责告警源摘要、告警规则、规则评估、告警实例、去重分组、静默、通知路由、投递摘要、确认关闭、审计列表和自检摘要。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `alerting` 的职责边界、数据归属、前序服务兼容、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考 Prometheus Alertmanager、Grafana Alerting、PagerDuty、Opsgenie、Amazon CloudWatch 和 Datadog Monitor 的公开设计。Alertmanager 的 grouping、deduplication、routing、silencing 和 inhibition 适合告警风暴控制；Grafana 的 contact point、notification policy、silence 和 mute timing 适合把告警规则与通知渠道拆开；PagerDuty 的事件编排、`dedup_key` 和通用事件字段适合把来源事件整理成可处理事件；Opsgenie 的 `alias` 去重和 acknowledge、close、snooze 生命周期适合告警处理闭环；CloudWatch 的复合告警和抑制等待窗口适合维护窗口和上游故障抑制；Datadog 的 monitor、renotify 和 escalation message 适合重复提醒和升级摘要。本文档只吸收规则、事件、抑制、路由、去重键和闭环模型，不接入这些平台的主数据，也不在第一版发送真实外部通知。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Prometheus Alertmanager](https://prometheus.io/docs/alerting/latest/alertmanager/) | 告警服务负责去重、分组、路由、静默和抑制，不直接替代指标采集。 |
| [Grafana Alerting notifications](https://grafana.com/docs/grafana/latest/alerting/fundamentals/notifications/) | 通知目的地和通知策略独立于规则，避免规则里硬编码渠道。 |
| [PagerDuty Event Orchestration](https://support.pagerduty.com/main/docs/event-orchestration) | 来源事件先经过编排和路由，再进入处理闭环。 |
| [Opsgenie Alert API](https://docs.opsgenie.com/docs/alert-api) | 告警别名可作为客户端定义的去重键，确认、关闭和静默类操作需要保留处理记录。 |
| [Amazon CloudWatch alarm suppression](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/alarm-suppression.html) | 复合告警、等待期和扩展期可减少维护窗口和上游故障产生的噪音。 |
| [Datadog monitor notifications](https://docs.datadoghq.com/monitors/notify/) | 重复提醒、升级消息和通知变量应是告警路由策略的一部分。 |

### 职责边界

`alerting` 负责统一告警控制面。它保存来源服务健康摘要、规则定义、规则评估摘要、告警实例、去重指纹、分组键、静默规则、抑制摘要、通知路由、投递摘要、确认关闭记录、幂等记录和告警审计。

`alerting` 不负责用户登录、角色能力点主数据、站内通知主数据、真实短信、真实邮件、真实 Webhook、真实指标采集、真实节点操作、真实备份恢复、真实 Cloudreve 调用、真实容器或虚拟机控制、玩家资源下载、社区工单主数据或后台聚合主数据。

第一版固定为内存存储和受控测试适配器。它可以使用前序服务健康、指标、任务失败和风险摘要的快照，不能直接导入前序服务 Java 类、内存 store、测试种子或私有数据结构。跨服务字段只能来自正式 API、后端入口可信认证上下文或契约允许的本地测试 stub。

### 数据归属

`alerting` 拥有以下主数据：AlertSource、AlertRule、AlertEvaluation、AlertInstance、AlertSilence、AlertRoute、AlertDelivery、AlertingAuditLog、AlertingOpsSummary 和幂等记录。

`alerting` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和状态快照；可以保存来自 `ops-control`、`external-node-executor`、`server-status`、`cloudreve-sync` 和 `backup-recovery` 的健康或异常摘要；可以保存来自 `notification` 的站内投递引用摘要；可以保存来自 `cross-platform-notification` 的外部模拟投递摘要。所有保存内容都只能是安全摘要，不得保存访问 token、节点密钥、Cloudreve 管理凭据、内部绝对路径、完整通知正文、完整请求头、外部渠道凭据或异常堆栈。

### 基础路径、端口和认证

所有接口默认使用 `/api/v1/alerting` 前缀。第四批合并后当前运行入口为 `ops-core-service:8133`。历史原服务端口 `8120` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或测试入口。

健康检查 `GET /api/v1/alerting/health` 不要求认证，但只能返回存活、版本、服务名、状态和请求编号，不返回告警数量、路由详情、来源摘要、依赖错误细节或任何敏感字段。

除健康检查外，全部接口要求 `Authorization: Bearer <token>`。读取类接口要求后台角色 `HELPER`、`ADMIN` 或 `OWNER`，并具备 `NODE_READ`、`HIGH_RISK_APPROVE` 或等效后台读取能力。规则、静默、通知路由、确认和关闭写接口要求 `ADMIN` 或 `OWNER`。高风险升级策略、强制关闭严重告警和路由测试要求 `HIGH_RISK_APPROVE` 或 `OWNER`。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`beforeState`、`afterState`、`auditResult`、`internalPath`、`resolvedPath`、`rawToken`、`credential`、`secretKey`、`nodeToken`、`notificationToken`、`webhookSecret`、`smtpPassword`、`smsToken`、`deliveryStatus`、`createdBy`、`updatedBy`、`acknowledgedBy`、`closedBy` 和 `suppressedBy` 等服务端可信字段。可信字段必须递归检查，嵌套在 `sourceSnapshot`、`labels`、`conditionSummary`、`matchers`、`notificationTemplateRef`、`receiverSummary` 或任意数组对象中也必须拒绝。出现可信字段时返回 `40001`。

### 本地测试控制头

`alerting` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Source-Mode`、`X-Test-Notification-Mode`、`X-Test-Cross-Platform-Notification-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Now` 模拟认证失败、来源服务不可用、来源超时、来源坏 schema、通知不可用、通知超时、跨平台通知不可用、跨平台通知模拟失败、审计失败、状态写入失败和时间边界。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发认证失败、来源失败、通知失败、审计失败、存储失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 纳入生产化硬化项。

### 前序服务兼容契约

`auth` 是强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。auth 不可用返回 `46920`，auth 超时返回 `46921`，auth 字段或枚举不兼容返回 `46922`。

`notification` 是站内通知依赖。`alerting` 只保存站内通知引用摘要，不保存 notification 通知正文主数据，不绕过 notification 自建未读数或模板主数据。notification 不可用返回 `46900`，notification 超时返回 `46901`，notification 字段不兼容返回 `46902`。站内通知失败只影响投递摘要，不得自动关闭告警实例。

`cross-platform-notification` 是外部模拟投递控制面。`alerting` 告警命中并且路由声明需要外部通知时，只能向 CPN 传入安全摘要，包括 `sourceModule=alerting`、`sourceId=<alertId>`、`eventType=alert.firing`、`riskLevel`、`routeId` 或 provider、template、receiver 摘要、`payloadSummary`、`expiresAt`、`reason` 和 `idempotencyKey`。不得传入完整告警正文、完整日志、内部路径、请求头、token 或异常堆栈。CPN 不可用、路由不匹配、模板变量不合法、模拟发送失败或审计失败时，只影响投递摘要，不得自动关闭告警实例，不得把失败伪造成真实发送成功。

`admin` 是后台聚合入口。`alerting` 可以向 admin 暴露模块健康、待处理告警数量、严重级别摘要和审计摘要，不能让 admin 修改告警规则或告警状态。admin 尚未声明 `ALERTING` 入口时，本轮不得修改 admin 稳定接口。

`ops-control` 和 `external-node-executor` 是运维状态来源。`alerting` 可以消费节点离线、指标超阈值、任务失败、审批超时和心跳异常摘要，不能创建节点任务，不能执行终端、文件、容器、虚拟机或实例操作。

`server-status` 是玩家可见状态来源。`alerting` 可以把公开服务状态异常作为来源快照，但不能替代 server-status 展示接口，不能执行线路或实例操作。

`cloudreve-sync` 可以提供 provider 不可用、配额 WARNING、配额 EXCEEDED、同步任务失败和分享失效摘要。`alerting` 不能保存 Cloudreve token、分享密码、私有直链或真实文件路径。

`backup-recovery` 可以提供备份失败、备份点校验失败、恢复演练失败、恢复申请待审批和生产恢复阻断摘要。`alerting` 不能触发真实恢复，也不能绕过 backup-recovery 的恢复审批。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `AlertSourceService` | `OPS_CONTROL`、`SERVER_STATUS`、`CLOUDREVE_SYNC`、`BACKUP_RECOVERY`、`NOTIFICATION`、`ADMIN` | 第一版可登记的告警来源服务。 |
| `AlertSourceType` | `HEALTH`、`METRIC`、`TASK`、`QUOTA`、`SCHEDULE`、`AUDIT`、`DEPENDENCY` | 来源类型。 |
| `AlertSeverity` | `INFO`、`WARNING`、`CRITICAL`、`BLOCKER` | 告警严重级别。 |
| `AlertRuleStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`ARCHIVED` | 规则状态。 |
| `AlertConditionType` | `THRESHOLD`、`MISSING_HEARTBEAT`、`TASK_FAILED`、`QUOTA_EXCEEDED`、`DEPENDENCY_DOWN`、`RESTORE_PENDING`、`MANUAL_EVENT` | 第一版规则条件类型。 |
| `AlertEvaluationStatus` | `MATCHED`、`NOT_MATCHED`、`SUPPRESSED`、`SOURCE_UNAVAILABLE`、`FAILED` | 手动评估结果。 |
| `AlertInstanceStatus` | `FIRING`、`ACKNOWLEDGED`、`SUPPRESSED`、`RESOLVED`、`CLOSED` | 告警实例状态。 |
| `AlertSilenceStatus` | `ACTIVE`、`EXPIRED`、`CANCELLED` | 静默状态。 |
| `AlertRouteStatus` | `ENABLED`、`DISABLED` | 通知路由状态。 |
| `AlertDeliveryStatus` | `PENDING`、`SENT`、`FAILED`、`RETRYING`、`SUPPRESSED` | 投递摘要状态。 |
| `AlertDependencyStatus` | `AVAILABLE`、`UNAVAILABLE`、`TIMEOUT`、`BAD_SCHEMA`、`DISABLED` | 依赖摘要状态。 |
| `AlertingAuditResult` | `SUCCESS`、`FAILED` | 审计结果。 |

### 通用对象

#### AlertSource

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sourceId` | string | 是 | 告警来源 ID。 |
| `sourceService` | string | 是 | `AlertSourceService`。 |
| `sourceType` | string | 是 | `AlertSourceType`。 |
| `displayName` | string | 是 | 展示名，2 到 80 位。 |
| `enabled` | boolean | 是 | 是否启用。 |
| `healthStatus` | string | 是 | `AlertDependencyStatus` 或来源状态摘要。 |
| `lastEventAt` | string 或 null | 是 | 最近事件时间。 |
| `lastSnapshotAt` | string 或 null | 是 | 最近来源快照时间。 |
| `capabilities` | string[] | 是 | 来源能力摘要。 |
| `labels` | object | 是 | 标签，最多 20 个键值。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReasons` | string[] | 是 | 降级原因，必须脱敏。 |

#### AlertRule

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ruleId` | string | 是 | 规则 ID。 |
| `displayName` | string | 是 | 规则名称，2 到 80 位。 |
| `sourceService` | string | 是 | 来源服务。 |
| `sourceType` | string | 是 | 来源类型。 |
| `severity` | string | 是 | `AlertSeverity`。 |
| `labels` | object | 是 | 标签，最多 20 个键值。 |
| `conditionType` | string | 是 | `AlertConditionType`。 |
| `conditionSummary` | object | 是 | 条件摘要，不保存原始查询密钥。 |
| `evaluationWindowSeconds` | integer | 是 | 评估窗口，60 到 86400。 |
| `forDurationSeconds` | integer | 是 | 持续触发时间，0 到 86400。 |
| `dedupeKeyTemplate` | string | 是 | 去重键模板，最多 200 位。第一版支持 `{{sourceService}}`、`{{sourceType}}`、`{{severity}}`、`{{sourceRef}}`、`{{nodeId}}`、`{{groupKey}}`、`{{labels.<key>}}` 和 `{{snapshot.<key>}}` 占位符。无法解析的占位符按空字符串处理，生成后的空白字符归一为 `_`，连续空值不得导致指纹为空。 |
| `routeId` | string 或 null | 是 | 默认通知路由。 |
| `runbookUrl` | string 或 null | 是 | 处理说明链接，只允许 http、https 或站内路径。 |
| `status` | string | 是 | `AlertRuleStatus`。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### AlertEvaluation

字段为 `evaluationId`、`ruleId`、`status`、`matchedSourceId`、`createdAlertId`、`dedupeHit`、`suppressed`、`dependencyStatus`、`resultSummary`、`failureReason`、`evaluatedBy` 和 `evaluatedAt`。手动评估只读取来源快照，不主动采集真实指标。命中时必须按 `dedupeKeyTemplate` 生成稳定指纹；未静默且规则绑定启用路由、路由匹配成功时，必须生成 `SENT` 投递摘要；未静默但路由缺失、禁用或不匹配时，不得伪造投递成功，告警的 `notificationSummary.status` 应返回 `PENDING` 并带安全原因摘要。

#### AlertInstance

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `alertId` | string | 是 | 告警实例 ID。 |
| `ruleId` | string | 是 | 来源规则 ID。 |
| `sourceService` | string | 是 | 来源服务。 |
| `sourceRef` | object | 是 | 来源对象安全摘要。 |
| `severity` | string | 是 | 告警级别。 |
| `status` | string | 是 | `AlertInstanceStatus`。 |
| `labels` | object | 是 | 标签快照。 |
| `fingerprint` | string | 是 | 去重指纹摘要。 |
| `groupKey` | string | 是 | 分组键。 |
| `firstFiredAt` | string | 是 | 首次触发时间。 |
| `lastFiredAt` | string | 是 | 最近触发时间。 |
| `acknowledgedBy` | string 或 null | 是 | 确认人。 |
| `acknowledgedAt` | string 或 null | 是 | 确认时间。 |
| `closedBy` | string 或 null | 是 | 关闭人。 |
| `closedAt` | string 或 null | 是 | 关闭时间。 |
| `summary` | string | 是 | 告警摘要，最多 300 位。 |
| `runbookUrl` | string 或 null | 是 | 处理说明链接。 |
| `notificationSummary` | object | 是 | 投递摘要。 |
| `suppressionSummary` | object | 是 | 静默或抑制摘要。 |

#### AlertSilence

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `silenceId` | string | 是 | 静默 ID。 |
| `matchers` | object | 是 | 标签、来源、级别匹配器。第一版支持 `sourceService`、`severity`、`groupKey` 和 `labels` 精确匹配。 |
| `startsAt` | string | 是 | 开始时间。 |
| `endsAt` | string | 是 | 结束时间，必须晚于开始时间。 |
| `reason` | string | 是 | 静默原因，1 到 200 位。 |
| `status` | string | 是 | `AlertSilenceStatus`。 |
| `createdBy` | string | 是 | 创建者。 |
| `cancelledBy` | string 或 null | 是 | 取消者。 |
| `createdAt` | string | 是 | 创建时间。 |
| `cancelledAt` | string 或 null | 是 | 取消时间。 |

#### AlertRoute

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `routeId` | string | 是 | 路由 ID。 |
| `displayName` | string | 是 | 路由名称。 |
| `matchers` | object | 是 | 匹配器。第一版支持 `sourceService`、`severity`、`groupKey` 和 `labels` 精确匹配，路由必须启用且全部 matcher 命中后才允许生成正常投递摘要。 |
| `groupBy` | string[] | 是 | 分组字段。 |
| `groupWaitSeconds` | integer | 是 | 首次分组等待，0 到 3600。 |
| `groupIntervalSeconds` | integer | 是 | 分组重复间隔，60 到 86400。 |
| `repeatIntervalSeconds` | integer | 是 | 重复提醒间隔，300 到 604800。 |
| `notificationTemplateRef` | object | 是 | notification 模板引用摘要。 |
| `receiverSummary` | object | 是 | 接收方摘要，不返回外部渠道 secret。 |
| `status` | string | 是 | `AlertRouteStatus`。 |
| `createdBy` | string | 是 | 创建者。 |
| `updatedBy` | string | 是 | 最近修改者。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### AlertDelivery

字段为 `deliveryId`、`alertId`、`routeId`、`notificationRef`、`status`、`deliveryMode`、`externalModule`、`externalDeliveryId`、`externalAttemptStatus`、`realExternalSend`、`attempts`、`lastAttemptAt`、`failureCode`、`failureSummary`、`nextRetryAt` 和 `createdAt`。不得保存真实外部 webhook secret、邮件密码、短信 token 或完整通知正文。

`status` 为兼容字段，CPN 模拟成功时仍返回 `SENT`。同时必须返回 `deliveryMode=SIMULATED_EXTERNAL`、`externalModule=cross-platform-notification`、`externalDeliveryId`、`externalAttemptStatus=SIMULATED_SUCCESS` 和 `realExternalSend=false`。CPN 模拟失败时返回 `status=FAILED` 或 `RETRYING`，`failureCode` 和 `failureSummary` 只能是脱敏摘要，告警实例仍保持 `FIRING`、`ACKNOWLEDGED` 或契约允许状态。

#### AlertingAuditLog

审计字段继承公共契约，允许补充 `ruleId`、`alertId`、`silenceId`、`routeId`、`deliveryId`、`dependencyStatus`、`stateFrom`、`stateTo`、`idempotencyKey` 和 `notificationHint`。写接口传入的 `reason` 必须进入审计响应；`paramsSummary` 只能返回脱敏后的字段名、幂等键是否存在和安全摘要，不能回显完整请求体。审计列表不得提供删除接口。审计响应不得返回 token、密钥、外部渠道 secret、完整请求头、完整通知正文、内部路径、完整来源 payload 或异常堆栈。

#### AlertingOpsSummary

字段至少包含 `service`、`port`、`storageMode`、`authMode`、`sourceAdapterMode`、`notificationAdapterMode`、`externalDeliveryAdapterMode`、`testControlsEnabled`、`sourcesTotal`、`rulesTotal`、`enabledRulesTotal`、`alertsTotal`、`firingAlertsTotal`、`acknowledgedAlertsTotal`、`silencesTotal`、`activeSilencesTotal`、`routesTotal`、`deliveriesTotal`、`failedDeliveriesTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastAlertAt`、`lastDeliveryFailureAt`、`degraded`、`degradeReasons` 和 `productionGaps`。`externalDeliveryAdapterMode` 第一版固定为 `CPN_SIMULATED_EXTERNAL`。

### 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `46900` | 502 | notification 不可用。 |
| `46901` | 504 | notification 调用超时。 |
| `46902` | 502 | notification 响应不兼容。 |
| `46910` | 502 | 来源服务不可用。 |
| `46911` | 504 | 来源服务调用超时。 |
| `46912` | 502 | 来源服务快照不兼容。 |
| `46920` | 502 | auth 认证上下文不可用。 |
| `46921` | 504 | auth 认证上下文调用超时。 |
| `46922` | 502 | auth 认证上下文字段不兼容。 |
| `49900` | 404 | 告警源不存在。 |
| `49901` | 404 | 告警规则不存在。 |
| `49902` | 404 | 告警实例不存在。 |
| `49903` | 404 | 静默不存在。 |
| `49904` | 404 | 通知路由不存在。 |
| `49905` | 404 | 投递摘要不存在。 |
| `49910` | 409 | 状态不允许当前操作。 |
| `49911` | 400 | 告警规则条件不合法。 |
| `49912` | 409 | 幂等键请求指纹冲突。 |
| `49913` | 400 | 静默时间范围不合法。 |
| `49914` | 400 | 标签匹配器不合法。 |
| `49915` | 409 | 告警已被静默或抑制，不能重复投递。 |
| `55500` | 500 | alerting 内部错误。 |
| `55501` | 500 | alerting 审计写入失败。 |
| `55502` | 500 | alerting 状态写入失败。 |
| `55503` | 500 | alerting 投递状态写入失败。 |

字段校验、未登录、令牌格式错误、角色权限不足、能力点不足、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/alerting/health` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/alerting/ops/summary` | 是 | `NODE_READ` 或 `HIGH_RISK_APPROVE` | LOW |
| 告警源列表 | GET | `/api/v1/alerting/sources` | 是 | `NODE_READ` | LOW |
| 告警源详情 | GET | `/api/v1/alerting/sources/{sourceId}` | 是 | `NODE_READ` | LOW |
| 规则列表 | GET | `/api/v1/alerting/rules` | 是 | `NODE_READ` | LOW |
| 规则详情 | GET | `/api/v1/alerting/rules/{ruleId}` | 是 | `NODE_READ` | LOW |
| 创建规则 | POST | `/api/v1/alerting/rules` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 更新规则 | PATCH | `/api/v1/alerting/rules/{ruleId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 启用规则 | PATCH | `/api/v1/alerting/rules/{ruleId}/enable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 禁用规则 | PATCH | `/api/v1/alerting/rules/{ruleId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 手动评估规则 | POST | `/api/v1/alerting/rules/{ruleId}/evaluate` | 是 | `ADMIN` 或 `OWNER`，`NODE_READ` | MEDIUM |
| 告警实例列表 | GET | `/api/v1/alerting/alerts` | 是 | `NODE_READ` | LOW |
| 告警实例详情 | GET | `/api/v1/alerting/alerts/{alertId}` | 是 | `NODE_READ` | LOW |
| 确认告警 | PATCH | `/api/v1/alerting/alerts/{alertId}/acknowledge` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 关闭告警 | PATCH | `/api/v1/alerting/alerts/{alertId}/close` | 是 | `ADMIN` 或 `OWNER`，严重告警要求 `HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 静默列表 | GET | `/api/v1/alerting/silences` | 是 | `NODE_READ` | LOW |
| 创建静默 | POST | `/api/v1/alerting/silences` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 取消静默 | PATCH | `/api/v1/alerting/silences/{silenceId}/cancel` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 通知路由列表 | GET | `/api/v1/alerting/routes` | 是 | `NODE_READ` | LOW |
| 创建通知路由 | POST | `/api/v1/alerting/routes` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 更新通知路由 | PATCH | `/api/v1/alerting/routes/{routeId}` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 测试通知路由 | POST | `/api/v1/alerting/routes/{routeId}/test` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 投递记录列表 | GET | `/api/v1/alerting/deliveries` | 是 | `NODE_READ` | LOW |
| 审计列表 | GET | `/api/v1/alerting/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

### 健康、自检和来源接口

`GET /api/v1/alerting/health` 成功返回 `service=alerting`、`status`、`version` 和 `requestId`。进程存活但依赖不可用时仍可返回 HTTP `200`，并用 `status=DEGRADED` 标记。该接口不得泄露来源详情、规则数量、告警数量、通知路由、token 或依赖错误细节。

`GET /api/v1/alerting/ops/summary` 成功返回 `AlertingOpsSummary`。合并后必须返回 `port=8133`、`legacyPort=8120`、`storageMode=IN_MEMORY`、`sourceAdapterMode=TEST_STUB`、`notificationAdapterMode=TEST_STUB`、`externalDeliveryAdapterMode=CPN_SIMULATED_EXTERNAL` 和生产化缺口。读取失败返回 `55500`，不得伪造健康。

`GET /api/v1/alerting/sources` 支持 `page`、`pageSize`、`keyword`、`sourceService`、`sourceType`、`healthStatus`、`enabled` 和 `sort`。`sort` 允许 `lastEventAt_desc`、`lastSnapshotAt_desc`、`displayName_asc`。成功响应分页 `items` 为 `AlertSource[]`。

`GET /api/v1/alerting/sources/{sourceId}` 返回来源详情、最近来源快照摘要和降级原因。来源不存在返回 `49900`。响应不得返回来源服务私有数据、内部路径、token 或完整 payload。

### 规则接口

`GET /api/v1/alerting/rules` 支持 `page`、`pageSize`、`keyword`、`sourceService`、`sourceType`、`severity`、`status`、`routeId`、`labelKey`、`labelValue` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`、`severity_desc`。成功响应分页 `items` 为 `AlertRule[]`。

`GET /api/v1/alerting/rules/{ruleId}` 返回规则详情、最近评估摘要、最近告警实例摘要和路由摘要。规则不存在返回 `49901`。

`POST /api/v1/alerting/rules` 请求字段为 `displayName`、`sourceService`、`sourceType`、`severity`、`labels`、`conditionType`、`conditionSummary`、`evaluationWindowSeconds`、`forDurationSeconds`、`dedupeKeyTemplate`、`routeId`、`runbookUrl`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `AlertRule`。规则名称在同一来源服务内不能重复。条件摘要必须匹配条件类型，非法返回 `49911`。路由不存在返回 `49904`。

`PATCH /api/v1/alerting/rules/{ruleId}` 可修改 `displayName`、`sourceService`、`sourceType`、`severity`、`labels`、`conditionType`、`conditionSummary`、`evaluationWindowSeconds`、`forDurationSeconds`、`dedupeKeyTemplate`、`routeId`、`runbookUrl`、`reason` 和 `idempotencyKey`。`ARCHIVED` 规则不可修改。

`PATCH /api/v1/alerting/rules/{ruleId}/enable` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT` 和 `DISABLED` 可启用为 `ENABLED`。重复启用保持幂等。`ARCHIVED` 返回 `49910`。

`PATCH /api/v1/alerting/rules/{ruleId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 可禁用为 `DISABLED`。重复禁用保持幂等。禁用规则不删除已有告警实例。

`POST /api/v1/alerting/rules/{ruleId}/evaluate` 请求字段为 `sourceSnapshot`、`dryRun`、`reason` 和 `idempotencyKey`。`sourceSnapshot` 只能是测试或服务端适配器提供的安全摘要，不能包含 token、内部路径、完整日志或任何嵌套可信字段。成功返回 `AlertEvaluation`。规则未启用返回 `49910`。来源不可用返回 `46910`，来源超时返回 `46911`，来源 schema 不兼容返回 `46912`。命中时按 `dedupeKeyTemplate` 生成指纹，重复指纹更新已有 `AlertInstance.lastFiredAt`，不新建告警实例。未静默告警必须先按规则 `routeId` 找到启用路由，再按 route matcher 匹配来源、级别、分组和标签；匹配成功生成投递摘要。路由声明外部通知时必须经 CPN 生成模拟外部 delivery 和 attempt 摘要；匹配失败保留 `PENDING` 摘要并记录不投递原因。

### 告警实例接口

`GET /api/v1/alerting/alerts` 支持 `page`、`pageSize`、`ruleId`、`sourceService`、`severity`、`status`、`groupKey`、`labelKey`、`labelValue`、`keyword`、`from`、`to` 和 `sort`。`sort` 允许 `lastFiredAt_desc`、`firstFiredAt_desc`、`severity_desc`、`status_asc`。时间范围按 `firstFiredAt` 过滤，反向范围返回 `40001`。

`GET /api/v1/alerting/alerts/{alertId}` 返回告警详情、最近投递摘要和关联静默摘要。告警不存在返回 `49902`。

`PATCH /api/v1/alerting/alerts/{alertId}/acknowledge` 请求字段为 `reason` 和 `idempotencyKey`。`FIRING` 和 `SUPPRESSED` 可确认为 `ACKNOWLEDGED`。重复确认已确认告警保持幂等。`CLOSED` 返回 `49910`。确认不会关闭告警，也不会取消后续重复提醒，只改变处理状态。

`PATCH /api/v1/alerting/alerts/{alertId}/close` 请求字段为 `resolutionSummary`、`reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `CLOSE_ALERT`。`FIRING`、`ACKNOWLEDGED`、`SUPPRESSED` 和 `RESOLVED` 可关闭为 `CLOSED`。`BLOCKER` 严重级别关闭要求 `HIGH_RISK_APPROVE` 或 `OWNER`。重复关闭返回成功且保持原关闭时间。

### 静默接口

`GET /api/v1/alerting/silences` 支持 `page`、`pageSize`、`status`、`sourceService`、`severity`、`labelKey`、`labelValue`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`startsAt_asc`、`endsAt_asc`。读取和匹配静默前必须懒更新过期状态，`endsAt` 早于当前时间且仍为 `ACTIVE` 的静默必须转为 `EXPIRED`。

`POST /api/v1/alerting/silences` 请求字段为 `matchers`、`startsAt`、`endsAt`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `AlertSilence`。`endsAt` 必须晚于 `startsAt`，否则返回 `49913`。匹配器必须至少包含来源、级别、标签或 groupKey 中的一类，非法返回 `49914`。静默只暂停通知，不删除告警实例，不停止规则评估。

`PATCH /api/v1/alerting/silences/{silenceId}/cancel` 请求字段为 `reason` 和 `idempotencyKey`。只有 `ACTIVE` 可取消。`EXPIRED` 和 `CANCELLED` 返回 `49910` 或按已取消幂等返回同一结果，同一实现版本内必须固定并写入测试。

### 通知路由和投递接口

`GET /api/v1/alerting/routes` 支持 `page`、`pageSize`、`keyword`、`status`、`severity`、`sourceService` 和 `sort`。`sort` 允许 `updatedAt_desc`、`displayName_asc`。

`POST /api/v1/alerting/routes` 请求字段为 `displayName`、`matchers`、`groupBy`、`groupWaitSeconds`、`groupIntervalSeconds`、`repeatIntervalSeconds`、`notificationTemplateRef`、`receiverSummary`、`enabled`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `AlertRoute`。接收方摘要只能保存站内通知或外部渠道的脱敏描述，不得保存 webhook secret、邮件密码或短信 token。

`PATCH /api/v1/alerting/routes/{routeId}` 可修改创建接口中的字段，`reason` 和 `idempotencyKey` 必填。路由不存在返回 `49904`。审计失败时不得改变路由。

`POST /api/v1/alerting/routes/{routeId}/test` 请求字段为 `sampleAlert`、`reason` 和 `idempotencyKey`。成功返回 `AlertDelivery`。第一版只调用 notification 测试适配器、CPN 模拟投递适配器或生成投递摘要，不发送真实外部渠道。notification 或 CPN 不可用可以返回依赖错误或创建 `FAILED` 投递摘要，同一实现版本内必须固定并写入测试。

`GET /api/v1/alerting/deliveries` 支持 `page`、`pageSize`、`alertId`、`routeId`、`status`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`lastAttemptAt_desc`、`status_asc`。成功响应分页 `items` 为 `AlertDelivery[]`。

### 审计接口

`GET /api/v1/alerting/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`ruleId`、`alertId`、`silenceId`、`routeId`、`action`、`result`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`、`riskLevel_desc`。只有 `ADMIN` 和 `OWNER` 可访问。审计列表只读，不提供删除、修改或恢复接口。

审计中的 `requestId` 必须来自当前 HTTP 请求，不能使用固定占位值。后台写操作必须记录调用者、`reason`、操作前状态、操作后状态、请求编号、结果和失败原因。审计写入失败时，规则创建修改启停、规则评估生成告警、确认关闭、静默创建取消、路由创建修改和路由测试不得假装成功，必须返回 `55501` 并保持业务状态不变。

### 状态、幂等和并发

规则状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

告警实例状态流转为 `FIRING` 到 `ACKNOWLEDGED`、`SUPPRESSED`、`RESOLVED` 或 `CLOSED`；`ACKNOWLEDGED` 到 `RESOLVED` 或 `CLOSED`；`SUPPRESSED` 在静默结束后可回到 `FIRING` 或 `RESOLVED`；`CLOSED` 为人工终态。关闭不会修改来源服务状态。

静默状态流转为 `ACTIVE` 到 `EXPIRED` 或 `CANCELLED`。过期和取消都不删除历史记录。路由状态为 `ENABLED` 或 `DISABLED`，禁用路由不删除历史投递。

写接口使用幂等键时，必须使用字段名排序后的稳定 JSON 语义计算请求体指纹，嵌套对象按字段名递归排序，数组保留顺序，不能依赖 Java `Map.toString()` 或浏览器字段顺序。幂等键的查找、冲突判断、业务状态推进、审计写入、响应快照保存和幂等记录写入必须处于同一临界区内；并发相同幂等键同请求体只能执行一次业务动作，并返回同一响应快照；并发相同幂等键不同请求体必须返回 `49912`。后续数据库实现必须使用事务、唯一约束、条件更新或等效机制，不能降低并发口径。

### 安全、降级和脱敏

任何请求体都不得包含访问 token、节点密钥、Cloudreve 管理 token、分享密码、外部 webhook secret、SMTP 密码、短信 token、完整 Authorization 请求头、完整通知正文、内部绝对路径、完整来源 payload、异常堆栈、数据库连接串、`.env`、`authorized_keys`、`id_rsa`、服务器密码或 shell 命令。任何响应也不得包含这些字段或值。检查必须递归覆盖嵌套对象和数组。

外部依赖不可用时，读取类接口可以返回已有快照并标记 `degraded=true` 和 `degradeReasons`。写入类接口不得假装成功。通知投递失败和 CPN 模拟外部投递失败都不得关闭告警，也不得把告警主状态改成已处理。来源服务不可用时，规则评估必须返回明确依赖错误或降级评估摘要。审计失败时，告警实例、alerting 投递摘要、CPN delivery、CPN attempt 和两边审计不得出现半成功。

第一版不得提供真实删除规则、告警、静默、路由或投递记录的接口。确需清理历史记录时，必须在后续独立契约中增加归档接口，并重新完成文档、测试红灯、实现和回归闭环。

### 验收口径

`alerting` API 文档必须按 `docs/contracts-alerting.md` 独立存在，并由 `.local-docs/tests-ops-core.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求、敏感字段脱敏、测试控制头默认关闭和模块验收口径。

`alerting` 完成时必须满足以下条件：当前运行入口为 `ops-core-service:8133`，历史端口 `8120` 只作为 `legacyPort` 返回；健康检查不泄露敏感信息；除健康检查外全部接口要求后台认证；告警源、规则、评估、实例、确认关闭、静默、通知路由、投递摘要、审计、幂等、状态流转、去重分组、通知失败降级、审计失败回滚、敏感字段脱敏、测试控制头默认关闭和自检摘要都有自动化验证；不修改前序服务稳定接口；不直接调用 `external-node-executor`；不执行真实外部通知发送；不把告警规则塞进 `notification`、`admin`、`ops-control`、`server-status`、`cloudreve-sync` 或 `backup-recovery`；自动化测试必须先红灯；实现后 `alerting` 在 `ops-core-service` 中全部测试通过；当前后端运行入口回归测试通过；边界扫描无违规命中；测试过程记录完整。

## 北冥官网 online-map API 契约

来源：`docs/contracts-online-map.md`

版本：0.2

### 文档定位

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

### 职责边界

`online-map` 负责在线地图接入控制面和公开展示快照，包括地图 provider、公开地图入口、世界列表、图层、marker set、marker、区域、嵌入配置、健康快照、降级原因、依赖调用摘要、幂等记录、后台审计和自检摘要。

`online-map` 不负责地图渲染，不负责 Minecraft 插件运行，不负责瓦片文件代理，不负责真实世界目录读取，不负责节点命令执行，不负责容器、终端、文件管理、备份恢复、Cloudreve 管理、资源下载、告警规则创建或官网首页配置。

第一版固定为安全模拟和配置快照。可以保存 BlueMap、Dynmap、squaremap、Minecraft Overviewer 或自研地图源的公开入口摘要和 marker 快照，但不能保存地图服务后台密码、插件 token、内网 URL、真实世界路径、节点地址、完整异常栈或外部密钥。真实地图 provider 探测、真实 marker 同步、真实瓦片托管和真实插件联动必须后续单独闭环。

### 数据归属

`online-map` 拥有以下主数据：MapProvider、MapWorld、MapLayer、MapMarker、MapRegion、MapEmbedConfig、MapHealthSnapshot、MapDependencySnapshot、MapAuditLog 和幂等记录。

`online-map` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和用户状态快照；可以保存来自 `server-status` 的公开实例状态摘要；可以保存来自 `ops-control` 的只读节点或 Minecraft 实例健康摘要；可以保存来自 `content` 的公开页面引用摘要；可以保存来自 `changelog` 的地图版本摘要；可以保存供 `alerting` 读取的健康摘要；可以保存来自 `notification` 的投递结果摘要。所有快照只用于展示、过滤、降级和审计，不能成为来源模块主数据，也不能反写来源模块。

`online-map` 不能直接读取其他服务数据库，不能导入前序服务 Java package，不能复用前序服务内存 store，不能调用 `external-node-executor` 执行真实命令，不能代理真实瓦片目录，不能读取 Minecraft 世界文件，不能保存 Cloudreve token，不能把玩家资源下载塞进地图服务。

### 基础路径、端口和认证

所有接口默认使用 `/api/v1/online-map` 前缀。当前运行入口为 `portal-core-service:8134`，历史独立入口为 `online-map-service:8121`。自检摘要必须返回当前运行端口 `8134`，并可在生产化缺口或模块装配摘要中保留历史端口 `8121` 作为迁移来源记录。

健康检查 `GET /api/v1/online-map/health` 不要求认证，只返回服务名、版本、状态和请求编号。`status` 只允许 `READY` 或 `DEGRADED`，不返回 provider 明细、内部依赖错误、地图入口、内网地址或任何敏感字段。

公开接口不要求登录，只返回 `publicVisible=true`、provider `ENABLED`、对象未归档、未过期且安全可展示的数据。公开接口不得返回后台备注、内部 URL、allowlist 规则原文、审计字段、幂等键、操作者字段、依赖错误详情、节点摘要、真实路径、插件配置、请求头、token 或异常堆栈。

后台接口使用同一前缀下的 `/admin` 子路径，全部要求 `Authorization: Bearer <token>`。后台读取接口要求基础角色为 `HELPER`、`ADMIN` 或 `OWNER`，且具备 `NODE_READ`。后台写接口要求 `ADMIN` 或 `OWNER`，且具备 `NODE_WRITE`。修改公开入口、外部嵌入域名、allowed origins、provider 启用禁用、provider 归档和健康刷新属于 `MEDIUM` 风险；把 provider 从内部草稿改成公开可见、修改跨域嵌入来源、允许第三方域名嵌入或归档仍有公开对象的 provider 属于 `HIGH` 风险，必须携带二次确认。涉及高风险审批时要求 `HIGH_RISK_APPROVE` 或 `OWNER`。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`beforeState`、`afterState`、`auditResult`、`healthStatus`、`createdBy`、`updatedBy`、`enabledBy`、`disabledBy`、`archivedBy`、`refreshedBy`、`rawToken`、`credential`、`secretKey`、`nodeToken`、`mapAdminPassword`、`internalUrl`、`internalPath`、`resolvedPath`、`worldDirectory`、`fullException`、`Authorization`、`requestHeaders` 等服务端可信字段。可信字段必须递归检查，嵌套在 `metadata`、`styleSummary`、`sourceRef`、`providerProbe`、`dependencySnapshot`、`points`、`iconRef` 或任意数组对象中也必须拒绝。出现可信字段时返回 `40001`。

### 本地测试控制头

`online-map` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Provider-Mode`、`X-Test-Server-Status-Mode`、`X-Test-Ops-Control-Mode`、`X-Test-Content-Mode`、`X-Test-Changelog-Mode`、`X-Test-Notification-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Now` 模拟认证失败、provider 不可用、依赖不可用、依赖超时、依赖坏 schema、通知失败、审计失败、状态写入失败和时间边界。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发认证失败、provider 失败、依赖失败、通知失败、审计失败、存储失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 纳入生产化硬化项。

### 前序服务兼容契约

`auth` 是后台接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。auth 不可用返回 `46820`，auth 超时返回 `46821`，auth 字段或枚举不兼容返回 `46822`。

`server-status` 是玩家可见服务器状态来源。`online-map` 可以引用 server-status 的公开实例状态和线路摘要，不能伪造在线人数、MOTD、延迟和宕机状态。server-status 不可用返回 `46800`，超时返回 `46801`，字段不兼容返回 `46802`。公开地图总览可以使用已有地图快照并标记 `serverStatusStale=true`，不得假装状态实时。

`ops-control` 是运维只读摘要来源。`online-map` 可以读取节点或 Minecraft 实例只读健康摘要，不能创建运维任务，不能读取文件，不能执行终端，不能通过 ops-control 修改真实服务器状态。ops-control 不可用返回 `46810`，超时返回 `46811`，字段不兼容返回 `46812`。ops-control 不可用时，后台自检必须标记 `OPS_CONTROL_UNAVAILABLE`，不能伪造 provider 健康。

`content` 可以展示地图入口或地图说明页。`online-map` 只保存 content 公开引用摘要，不把文章、SEO、站点地图或首页配置主数据复制进自己的 store。content 不可用返回 `46830`，超时返回 `46831`，字段不兼容返回 `46832`。公开读取已有 provider 时可以使用已保存的 content 快照并标记 stale。

`changelog` 可以提供地图版本和更新记录摘要。`online-map` 不创建 changelog，不修改 changelog 发布状态。changelog 不可用返回 `46840`，超时返回 `46841`，字段不兼容返回 `46842`。

`notification` 是辅助依赖。地图公开入口变更、provider 禁用、健康刷新失败或后台高风险变更可以触发通知摘要。notification 不可用返回 `46850`，超时返回 `46851`，字段不兼容返回 `46852`。通知失败不得伪造成功，必须保存脱敏失败摘要并写入审计；是否回滚主状态由具体接口规则固定。

`alerting` 是后续消费方。`online-map` 第一版只暴露健康、自检和 provider 状态摘要，不直接创建告警规则，不直接写 alerting 告警实例。

### 枚举

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

### 通用对象

#### PublicMapOverview

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

#### PublicMapProvider

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

#### AdminMapProvider

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

#### PublicMapWorld

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

#### AdminMapWorld

`AdminMapWorld` 在 `PublicMapWorld` 基础上补充 `enabled`、`publicVisible`、`sourceWorldKey`、`styleSummary`、`degradeReasons`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。`sourceWorldKey` 只能是脱敏 provider 世界标识，不得是真实世界目录或绝对路径。

#### MapLayer

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

#### MapMarker

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

#### MapRegion

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

#### PublicMapEmbedConfig

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

#### MapHealthSnapshot

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

#### OnlineMapAuditLog

审计字段继承公共契约，允许补充 `providerId`、`worldId`、`layerId`、`markerId`、`regionId`、`healthSnapshotId`、`stateFrom`、`stateTo`、`idempotencyKey`、`dependencyStatus`、`notificationStatus` 和 `providerType`。审计列表不得提供删除接口。审计响应不得返回 token、密钥、完整请求头、内部 URL、真实世界路径、节点地址、完整异常栈、完整请求体、地图后台密码或插件配置。

#### OnlineMapOpsSummary

自检摘要至少包含 `service`、`port`、`storageMode`、`authMode`、`providerAdapterMode`、`serverStatusMode`、`opsControlMode`、`contentMode`、`changelogMode`、`notificationMode`、`testControlsEnabled`、`providersTotal`、`enabledProvidersTotal`、`worldsTotal`、`layersTotal`、`markersTotal`、`regionsTotal`、`healthSnapshotsTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastHealthCheckAt`、`lastAuditAt`、`degraded`、`degradeReasons` 和 `productionGaps`。当前版本必须返回 `port=8134`、`storageMode=IN_MEMORY`、`providerAdapterMode=TEST_STUB`，并明确真实 provider、真实 auth HTTP、真实持久化、真实跨服务 HTTP 和真实 marker 同步尚未接入。

### online-map 错误码

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

### 接口总览

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

### 公开接口

`GET /api/v1/online-map/overview` 返回 `PublicMapOverview`。总览只汇总 `ENABLED` 或 `DEGRADED`、`publicVisible=true`、未归档且未过期的数据。provider 健康失败时可以返回最近一次成功快照并标记 `degraded=true`，不得伪造实时成功。

`GET /api/v1/online-map/providers` 支持 `providerType`、`healthStatus`、`keyword` 和 `sort`。`sort` 允许 `sortOrder_asc`、`displayName_asc`、`lastHealthCheckAt_desc`。成功响应分页 `items` 为 `PublicMapProvider[]`。

`GET /api/v1/online-map/providers/{providerId}` 返回公开 provider 详情。provider 不存在、未启用、不可公开或已归档时返回 `49700`。

`GET /api/v1/online-map/worlds` 支持 `providerId`、`dimension`、`renderStatus`、`keyword` 和 `sort`。`sort` 允许 `sortOrder_asc`、`displayName_asc`、`lastRenderedAt_desc`。成功响应分页 `items` 为 `PublicMapWorld[]`。只返回公开 provider 下公开可见世界。

`GET /api/v1/online-map/layers` 支持 `providerId`、`worldId`、`layerType`、`visibility` 和 `sort`。`sort` 允许 `sortOrder_asc`、`displayName_asc`。只返回 `VISIBLE`、`visibility=PUBLIC`、公开 provider 和公开 world 下的图层。

`GET /api/v1/online-map/markers` 支持 `providerId`、`worldId`、`layerId`、`markerType`、`sourceModule`、`keyword`、`bounds`、`from`、`to` 和 `sort`。`bounds` 格式为 `minX,minZ,maxX,maxZ`，坐标必须是有限数字且 `minX<=maxX`、`minZ<=maxZ`。`sort` 允许 `updatedAt_desc`、`title_asc`、`createdAt_desc`。只返回 `PUBLISHED`、`visibility=PUBLIC`、未过期且坐标合法的 marker。

`GET /api/v1/online-map/regions` 支持 `providerId`、`worldId`、`layerId`、`sourceModule`、`keyword`、`bounds` 和 `sort`。只返回 `PUBLISHED`、`visibility=PUBLIC`、未过期且点位合法的区域。

`GET /api/v1/online-map/embed` 支持 `providerId`、`worldId` 和 `origin`。当传入 `origin` 时，必须命中 provider 的 `allowedOrigins`，否则返回 `49715`。传入 `worldId` 时，必须选择该 provider 下启用、公开且未归档的 world 作为默认世界；world 不存在、属于其他 provider、未公开、未启用或已归档时返回 `49701`。成功返回 `PublicMapEmbedConfig`。没有可用 provider 时返回 `data=null`，不能返回内部默认地址，也不能因为默认 provider 为空抛出内部错误。

### 后台 provider 接口

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

### 后台世界接口

`GET /api/v1/online-map/admin/worlds` 支持 `page`、`pageSize`、`providerId`、`dimension`、`renderStatus`、`enabled`、`publicVisible`、`keyword` 和 `sort`。成功响应分页 `items` 为 `AdminMapWorld[]`。

`PUT /api/v1/online-map/admin/worlds/{worldId}` 用于保存或更新 provider 同步到控制面的世界快照。请求字段包括 `providerId`、`worldName`、`dimension`、`displayName`、`enabled`、`publicVisible`、`sourceWorldKey`、`center`、`bounds`、`renderStatus`、`lastRenderedAt`、`styleSummary`、`sortOrder`、`reason` 和 `idempotencyKey`。`sourceWorldKey` 不得是绝对路径、真实目录或包含路径穿越。provider 不存在返回 `49700`。坐标不合法返回 `49714`。

### 后台图层接口

`GET /api/v1/online-map/admin/layers` 支持 `page`、`pageSize`、`providerId`、`worldId`、`layerType`、`status`、`visibility`、`keyword` 和 `sort`。成功响应分页 `items` 为 `MapLayer[]`。

`POST /api/v1/online-map/admin/layers` 请求字段包括 `providerId`、`worldId`、`displayName`、`layerType`、`defaultVisible`、`toggleable`、`visibility`、`styleSummary`、`sortOrder`、`reason` 和 `idempotencyKey`。provider 或 world 不存在返回 `49700` 或 `49701`。同一 world 下未归档图层同名冲突返回 `49711`。

`PATCH /api/v1/online-map/admin/layers/{layerId}` 可修改图层展示字段、状态、可见范围和样式摘要，`reason` 必填。`ARCHIVED` 图层不可修改，返回 `49710`。

`PATCH /api/v1/online-map/admin/layers/{layerId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。重复归档保持幂等。归档后公开接口不再返回该图层，也不返回挂在该图层下的 marker 和 region。

### 后台 marker 接口

`GET /api/v1/online-map/admin/markers` 支持 `page`、`pageSize`、`providerId`、`worldId`、`layerId`、`markerType`、`status`、`visibility`、`sourceModule`、`keyword`、`bounds`、`from`、`to` 和 `sort`。成功响应分页 `items` 为 `MapMarker[]`。

`POST /api/v1/online-map/admin/markers` 请求字段包括 `providerId`、`worldId`、`layerId`、`markerType`、`title`、`summary`、`position`、`points`、`iconRef`、`styleSummary`、`visibility`、`status`、`sourceModule`、`sourceRef`、`expiresAt`、`reason` 和 `idempotencyKey`。不同 marker 类型必须按本文档要求校验 `position` 和 `points`。坐标、点位、图标 URL 或样式不合法返回 `49714` 或 `40001`。创建默认状态为 `PUBLISHED`，但公开接口仍按 provider、world、layer 和 visibility 过滤。

`PATCH /api/v1/online-map/admin/markers/{markerId}` 可修改 marker 展示字段、点位、样式、可见范围、状态、来源摘要和过期时间，`reason` 必填。`ARCHIVED` marker 不可修改，返回 `49710`。

`PATCH /api/v1/online-map/admin/markers/{markerId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。重复归档保持幂等。归档后公开接口不可见。

### 后台区域接口

`GET /api/v1/online-map/admin/regions` 支持 `page`、`pageSize`、`providerId`、`worldId`、`layerId`、`status`、`visibility`、`sourceModule`、`keyword`、`bounds` 和 `sort`。成功响应分页 `items` 为 `MapRegion[]`。

`POST /api/v1/online-map/admin/regions` 请求字段包括 `providerId`、`worldId`、`layerId`、`title`、`summary`、`points`、`minY`、`maxY`、`styleSummary`、`visibility`、`status`、`sourceModule`、`sourceRef`、`expiresAt`、`reason` 和 `idempotencyKey`。区域至少 3 个点，点位必须是有限数字，`maxY` 不得小于 `minY`。不合法返回 `49714`。

`PATCH /api/v1/online-map/admin/regions/{regionId}` 可修改区域展示字段、点位、高度、样式、可见范围、状态、来源摘要和过期时间，`reason` 必填。`ARCHIVED` 区域不可修改，返回 `49710`。

`PATCH /api/v1/online-map/admin/regions/{regionId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。重复归档保持幂等。归档后公开接口不可见。

### 审计接口

`GET /api/v1/online-map/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`providerId`、`layerId`、`markerId`、`regionId`、`result`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`、`riskLevel_desc`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志只读，不提供删除、修改或恢复接口。

审计中的 `requestId` 必须来自当前 HTTP 请求，不能使用固定占位值。后台写操作必须记录调用者、`reason`、操作前状态、操作后状态、请求编号、结果和失败原因。审计写入失败时，provider、world、layer、marker、region 和健康快照的写操作不得假装成功，必须返回 `55601` 并保持业务状态不变。notification 失败不回滚 provider 主状态，但必须记录脱敏失败摘要。

### 状态、幂等和并发

provider 状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DEGRADED`、`DISABLED` 或在无公开对象时归档；`DEGRADED` 可恢复为 `ENABLED`、禁用或归档；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

图层状态流转为 `VISIBLE` 到 `HIDDEN` 或 `ARCHIVED`，`HIDDEN` 到 `VISIBLE` 或 `ARCHIVED`，`ARCHIVED` 为终态。marker 和区域状态流转为 `PUBLISHED` 到 `HIDDEN` 或 `ARCHIVED`，`HIDDEN` 到 `PUBLISHED` 或 `ARCHIVED`，`ARCHIVED` 为终态。

创建、修改、状态流转、归档、保存世界快照和健康刷新支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `49712`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序，数组保留顺序，不能依赖 Java `Map.toString()` 或浏览器字段顺序。

并发创建相同 provider URL、同名图层、相同 marker 来源引用或相同区域来源引用时只能一个成功，其余返回冲突。marker 和区域来源引用冲突以同一 provider、world、layer、sourceModule 和结构化规范化后的 sourceRef 为口径，`sourceRef=null` 或空对象不参与唯一约束。健康刷新必须以 provider 为粒度加锁并执行冷却窗口判断。所有写接口必须在同一个临界区内完成状态校验、业务写入、审计写入、幂等记录和响应快照保存。后续数据库实现必须迁移为事务、唯一约束、条件更新或等效机制，不能降低并发口径。

### 安全、降级和脱敏

所有 URL 必须拒绝 `file:`、`data:`、`javascript:`、带用户名密码 URL、内网 IP、localhost、链路本地地址、未解析主机、空 host 和控制字符。公开接口只允许返回公开 URL 或站内路径。allowed origins 不允许 `*`，不允许内网 origin。

坐标必须是有限数字。公开 marker 查询的 `bounds` 必须限制在合理范围内，不能因为异常范围导致全量扫描。providerType、dimension、renderStatus、layerType、layer status、markerType、visibility、object status 和 sourceModule 必须严格匹配本文档枚举，未知枚举返回 `40001`。HTML marker 的 `summary` 和所有对象的 `styleSummary` 不允许 `<script>`、事件处理器、危险协议、CSS `expression()`、`url(javascript:...)` 或内联敏感数据。

任何请求体和响应都不得包含访问 token、节点密钥、地图后台密码、Cloudreve 管理 token、分享密码、外部 webhook secret、SMTP 密码、短信 token、完整 Authorization 请求头、内部绝对路径、真实世界目录、节点地址、异常堆栈、数据库连接串、`.env`、`authorized_keys`、`id_rsa`、服务器密码或 shell 命令。检查必须递归覆盖嵌套对象和数组。

provider 探测失败时不清空旧公开入口。公开接口可以返回最近一次成功快照并标记 `degraded=true` 和 `degradeReasons`。没有任何成功快照时返回 `UNKNOWN`、空数组或 `data=null`，不得伪造在线地图。依赖不可用时，读取类接口可以使用已有快照并标记 stale；写入类接口不得假装成功。

### 验收口径

`online-map` API 文档必须按 `docs/contracts-online-map.md` 独立存在，并由 `.local-docs/tests-online-map.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求、敏感字段脱敏、测试控制头默认关闭和模块验收口径。

`online-map` 完成时必须满足以下条件：当前运行入口为 `portal-core-service:8134`，历史 `online-map-service:8121` Maven 入口已退役且不得恢复；健康检查公开且不泄露敏感信息；公开接口只返回公开可见、已启用、未归档且脱敏的数据；后台接口按角色和能力点限制；provider、world、layer、marker、region、embed、健康快照、审计、幂等、状态流转、URL 安全、坐标边界、依赖降级、审计失败回滚、敏感字段脱敏、测试控制头默认关闭和自检摘要都有自动化验证；自动化测试必须先红灯；实现后 `mvn -q -f backend/portal-core-service/pom.xml test` 通过并覆盖 `online-map` 全部 34 个 API；当前后端运行入口回归通过；边界扫描无违规命中；不修改前序服务稳定接口；不直接调用 `external-node-executor`；不读取真实世界目录；不代理真实瓦片；不执行真实地图插件命令；不把地图渲染、资源下载、节点文件管理、终端、备份恢复或告警规则塞进 `online-map`。

## 北冥官网 plugin-integration API 契约

来源：`docs/contracts-plugin-integration.md`

版本：0.1

### 文档定位

本文档是 `plugin-integration` 微服务的正式 API 契约。后续前端插件联动页面、管理后台、`online-map`、`alerting`、`changelog`、`ops-control`、`external-node-executor` 和其他业务模块只能通过本文档定义的接口读取或管理插件 provider、插件实例快照、事件 schema、事件、路由规则、同步任务、健康快照、对象映射和审计摘要，不能直接读取或修改 `plugin-integration` 数据，也不能把真实 Minecraft 插件运行、节点命令、地图主数据、告警规则、通知渠道或服务器文件操作塞进本服务。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、运维能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `plugin-integration` 的职责边界、数据归属、前序服务兼容、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

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

### 职责边界

`plugin-integration` 负责插件联动控制面，包括插件 provider 注册摘要、插件实例快照、插件能力声明、事件 schema、事件接收和脱敏、事件路由规则、同步任务摘要、插件健康快照、插件对象映射、依赖调用摘要、幂等记录、审计日志和自检摘要。

`plugin-integration` 不负责注册、登录、权限主数据、站内通知主数据、告警规则主数据、地图主数据、服务器状态主数据、真实插件运行、真实插件安装卸载、真实插件命令、真实世界目录读取、真实节点命令执行、真实文件写入、真实容器或虚拟机操作、真实跨平台消息发送、玩家资源下载或官网内容发布。

第一版固定为安全控制面和事件快照。它可以接收后台或测试适配器提交的插件事件，保存脱敏 payload 摘要，生成路由结果和模拟同步任务。它不能开放无鉴权公网 webhook，不能保存完整 raw payload，不能保存插件后台 token，不能调用真实 `external-node-executor`，不能写真实插件配置，不能执行 Minecraft 命令，不能直接改 `online-map`、`alerting`、`notification`、`server-status` 或 `changelog` 的主数据。

### 数据归属

`plugin-integration` 拥有以下主数据：PluginProvider、PluginInstanceSnapshot、PluginCapability、PluginEventSchema、PluginEvent、PluginRouteRule、PluginSyncTask、PluginHealthSnapshot、PluginObjectMapping、PluginAuditLog 和幂等记录。

`plugin-integration` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和用户状态快照；可以保存来自 `ops-control` 的节点和 Minecraft 实例只读摘要；可以保存来自 `external-node-executor` 的安全回写摘要；可以保存来自 `server-status` 的公开实例状态摘要；可以保存来自 `online-map` 的 provider、world、layer、marker 和 region 引用摘要；可以保存来自 `changelog` 的插件版本变更摘要；可以保存来自 `notification` 的投递结果摘要；可以保存供 `alerting` 消费的插件健康和事件异常摘要。所有快照只用于展示、过滤、降级、审计和后续同步建议，不能成为来源模块主数据，也不能反写来源模块。

`plugin-integration` 不能直接读取其他服务数据库，不能导入前序服务 Java package，不能复用前序服务内存 store，不能调用 `external-node-executor` 执行真实命令，不能读取插件目录、世界目录、服务端配置、RCON 密码或节点文件，不能保存 Cloudreve token、Webhook secret、Discord token、插件后台密码、完整请求头、内部 URL 或绝对路径。

### 基础路径、端口和认证

所有接口默认使用 `/api/v1/plugin-integration` 前缀。第四批合并后当前运行入口为 `ops-core-service:8133`。历史原服务端口 `8122` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或测试入口。

健康检查 `GET /api/v1/plugin-integration/health` 不要求认证，只返回 `service`、`version`、`status` 和 `requestId`，不得返回 provider 数量、插件 endpoint、内部依赖错误、事件 payload、内部 URL 或敏感字段。

后台接口使用 `/api/v1/plugin-integration/admin` 前缀，全部要求 `Authorization: Bearer <token>`。后台读取接口要求基础角色为 `HELPER`、`ADMIN` 或 `OWNER`，且具备 `NODE_READ`。后台写接口要求 `ADMIN` 或 `OWNER`，且具备 `NODE_WRITE`。涉及外部 endpoint、来源 allowlist、公开对象映射、provider 启用禁用、事件重放、路由规则启用、同步任务创建和高风险同步策略时要求 `HIGH_RISK_APPROVE` 或 `OWNER`，或携带固定二次确认文本。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`beforeState`、`afterState`、`auditResult`、`createdBy`、`updatedBy`、`enabledBy`、`disabledBy`、`archivedBy`、`rawPayload`、`rawPayloadStored`、`rawToken`、`pluginToken`、`pluginSecret`、`webhookSecret`、`discordToken`、`nodeToken`、`credential`、`secretKey`、`internalUrl`、`internalPath`、`resolvedPath`、`worldDirectory`、`serverPassword`、`Authorization`、`requestHeaders` 和 `fullException` 等服务端可信字段。可信字段必须递归检查，嵌套在 `payload`、`payloadSummary`、`samplePayloadSummary`、`sourceRef`、`targetRef`、`paramsSummary`、`metadata`、`matchers` 或任意数组对象中也必须拒绝。出现可信字段时返回 `40001`。

### 本地测试控制头

`plugin-integration` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Ops-Control-Mode`、`X-Test-External-Node-Executor-Mode`、`X-Test-Server-Status-Mode`、`X-Test-Online-Map-Mode`、`X-Test-Changelog-Mode`、`X-Test-Notification-Mode`、`X-Test-Alerting-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Now` 模拟认证失败、依赖不可用、依赖超时、依赖坏 schema、通知失败、审计失败、状态写入失败和时间边界。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发认证失败、依赖失败、通知失败、审计失败、存储失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 纳入生产化硬化项。

### 前序服务兼容契约

`auth` 是后台接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。auth 不可用返回 `47050`，auth 超时返回 `47051`，字段或枚举不兼容返回 `47052`。

`ops-control` 是 Minecraft 实例、节点和资产摘要来源。`plugin-integration` 只能读取实例和节点快照，不能创建运维任务，不能通过 ops-control 执行命令，不能修改容器、文件或实例状态。ops-control 不可用返回 `47060`，超时返回 `47061`，schema 不兼容返回 `47062`。ops-control 不可用时，读取类接口可以使用已有快照并标记 `stale=true`；写入类同步任务不得假装成功。

`external-node-executor` 是后续真实节点执行边界。第一版只保存 external-node-executor 安全摘要或使用本服务测试适配器，不直接调用真实节点，不执行插件命令，不写插件配置。external-node-executor 不可用返回 `47070`，超时返回 `47071`，schema 不兼容返回 `47072`。

`server-status` 是玩家可见状态来源。插件上报的在线人数、TPS、MOTD 或延迟不能直接覆盖 server-status 主数据，只能作为插件事件或指标摘要保存。server-status 不可用返回 `47075`，超时返回 `47076`，schema 不兼容返回 `47077`。

`online-map` 是地图对象主数据来源。`plugin-integration` 可以把插件事件转换为地图对象同步建议或对象映射摘要，但不能直接改 online-map 内存 store。需要创建或更新地图 marker、region、provider 时，必须走 online-map 正式 API，并保存调用结果摘要。第一版默认只做映射预览和模拟同步。online-map 不可用返回 `47080`，超时返回 `47081`，schema 不兼容返回 `47082`。

`changelog` 可以提供插件版本变更摘要。`plugin-integration` 不发布 changelog，不修改 changelog 状态，只保存版本引用快照。changelog 不可用返回 `47085`，超时返回 `47086`，schema 不兼容返回 `47087`。

`notification` 是通知投递依赖。插件异常、同步失败、事件 schema 失配、provider 禁用和高风险路由变更可以触发通知摘要。通知失败不回滚插件事件主状态，但必须记录脱敏失败摘要。notification 不可用返回 `47090`，超时返回 `47091`，schema 不兼容返回 `47092`。

`alerting` 是后续消费方。`plugin-integration` 第一版只暴露健康、自检、事件异常和同步失败摘要，不直接创建 alerting 规则，也不直接关闭告警。alerting 不可用返回 `47100`，超时返回 `47101`，schema 不兼容返回 `47102`。

### 枚举

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

### 通用对象

#### PluginProvider

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

#### PluginInstanceSnapshot

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

#### PluginCapability

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

#### PluginEventSchema

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

#### PluginEvent

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

#### PluginRouteRule

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

#### PluginSyncTask

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

#### PluginHealthSnapshot

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

#### PluginObjectMapping

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

#### PluginAuditLog

审计字段继承公共契约，允许补充 `providerId`、`eventId`、`schemaId`、`ruleId`、`taskId`、`mappingId`、`dependencyStatus`、`notificationStatus`、`idempotencyKey` 和 `failureReason`。审计列表不得提供删除、修改或恢复接口。审计响应不得返回 token、密钥、webhook secret、完整请求头、完整 payload、内部 URL、内部路径、真实世界目录、节点地址、完整异常栈或前序服务私有数据。

#### PluginIntegrationOpsSummary

自检摘要至少包含 `service`、`port`、`storageMode`、`authMode`、`opsControlAdapterMode`、`externalNodeExecutorAdapterMode`、`onlineMapAdapterMode`、`notificationAdapterMode`、`alertingAdapterMode`、`testControlsEnabled`、`providersTotal`、`enabledProvidersTotal`、`instancesTotal`、`schemasTotal`、`eventsTotal`、`routeRulesTotal`、`syncTasksTotal`、`objectMappingsTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastEventAt`、`lastSyncAt`、`degraded`、`degradeReasons` 和 `productionGaps`。

### 错误码

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

### 接口总览

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

### 后台 provider 接口

`GET /api/v1/plugin-integration/admin/ops/summary` 返回 `PluginIntegrationOpsSummary`。摘要不得返回 token、请求头、内部 URL、真实路径、插件后台密码、完整 payload 或异常栈。读取失败返回 `55700`。

`GET /api/v1/plugin-integration/admin/providers` 支持 `page`、`pageSize`、`keyword`、`providerType`、`serverKind`、`status`、`healthStatus`、`publicVisible`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`、`lastEventAt_desc`、`lastSyncAt_desc`。成功响应分页 `items` 为 `PluginProvider[]`。

`GET /api/v1/plugin-integration/admin/providers/{providerId}` 返回 provider、最近健康快照、实例摘要、能力摘要、最近事件、依赖摘要和最近审计。provider 不存在返回 `49800`。

`POST /api/v1/plugin-integration/admin/providers` 请求字段包括 `providerType`、`displayName`、`pluginName`、`pluginVersion`、`serverKind`、`instanceRef`、`nodeRef`、`publicVisible`、`eventEndpointSummary`、`allowedEventTypes`、`allowedOrigins`、`adminNote`、`reason` 和 `idempotencyKey`。创建默认状态为 `DRAFT`，成功响应 HTTP `201`。`providerType`、`serverKind` 和后续状态枚举必须命中本文档枚举表，非法枚举返回 `40001`。同一未归档 provider 下 `displayName`、`pluginName + serverKind + instanceRef` 或规范化 `eventEndpointSummary` 冲突返回 `49811`。外部 endpoint、allowed origins 或来源不安全返回 `49813`。涉及公开来源、外部 endpoint 或 allowlist 时必须携带 `confirmText=REGISTER_PLUGIN_PROVIDER_ENDPOINT`，否则返回 `42003`。

`PATCH /api/v1/plugin-integration/admin/providers/{providerId}` 可修改创建字段中的业务字段，`reason` 必填。修改 `eventEndpointSummary`、`allowedOrigins`、`allowedEventTypes` 或把 `publicVisible` 从 `false` 改为 `true` 属于 `HIGH` 风险，必须携带 `confirmText=UPDATE_PLUGIN_PROVIDER_ENDPOINT`。`ARCHIVED` provider 不允许修改，返回 `49810`。

`PATCH /api/v1/plugin-integration/admin/providers/{providerId}/enable` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ENABLE_PLUGIN_PROVIDER`。`DRAFT`、`DISABLED` 和 `DEGRADED` 可流转为 `ENABLED`。启用时必须校验 allowed event types 非空、allowed origins 均安全、至少存在一个同 provider 的 `ENABLED` schema 或一个可用实例摘要。不满足字段完整性返回 `40001`，不满足状态或依赖前置返回 `49810`。重复启用保持幂等。

`PATCH /api/v1/plugin-integration/admin/providers/{providerId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 或 `DEGRADED` 可流转为 `DISABLED`。禁用后事件接收返回 `49815` 或 `49810`。重复禁用保持幂等。

`PATCH /api/v1/plugin-integration/admin/providers/{providerId}/archive` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ARCHIVE_PLUGIN_PROVIDER`。只有 `DRAFT`、`DISABLED` 或 `DEGRADED` 可归档；`ENABLED` provider 必须先禁用后归档；仍有活动路由规则、未终态同步任务或 `ACTIVE` 对象映射时返回 `49810`，不得自动静默归档子对象。`ARCHIVED` 为终态。

### 插件实例和能力接口

`GET /api/v1/plugin-integration/admin/instances` 支持 `page`、`pageSize`、`providerId`、`pluginName`、`loaded`、`enabled`、`stale`、`keyword` 和 `sort`。`sort` 允许 `lastSeenAt_desc`、`pluginName_asc`、`pluginVersion_desc`。成功响应分页 `items` 为 `PluginInstanceSnapshot[]`。实例快照只读，第一版通过种子或事件接收更新摘要，不提供后台直接写接口。

`GET /api/v1/plugin-integration/admin/instances/{instanceId}` 返回实例详情、能力列表、最近事件和降级摘要。实例不存在返回 `49801`。

`GET /api/v1/plugin-integration/admin/capabilities` 支持 `page`、`pageSize`、`providerId`、`namespace`、`riskLevel`、`available`、`keyword` 和 `sort`。成功响应分页 `items` 为 `PluginCapability[]`。能力来源于 provider 和实例快照，不可由浏览器写入服务端可信字段。

### 事件 schema 接口

`GET /api/v1/plugin-integration/admin/event-schemas` 支持 `page`、`pageSize`、`providerId`、`eventType`、`sourcePlugin`、`status`、`keyword` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`eventType_asc`。成功响应分页 `items` 为 `PluginEventSchema[]`。

`GET /api/v1/plugin-integration/admin/event-schemas/{schemaId}` 返回 schema 详情和最近校验摘要。不存在返回 `49802`。

`POST /api/v1/plugin-integration/admin/event-schemas` 请求字段包括 `providerId`、`eventType`、`sourcePlugin`、`version`、`requiredFields`、`optionalFields`、`sensitiveFields`、`routingHints`、`samplePayloadSummary`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，默认状态为 `DRAFT`。同一 provider、eventType、sourcePlugin 和 version 的未归档 schema 冲突返回 `49811`。`sensitiveFields` 不得为空时却让样例摘要含敏感值，违规返回 `40001`。

`PATCH /api/v1/plugin-integration/admin/event-schemas/{schemaId}` 可修改 schema 字段，`reason` 必填。`ARCHIVED` schema 不允许修改。启用中的 schema 修改后仍保持原状态，但必须通过字段和敏感字段校验。

`PATCH /api/v1/plugin-integration/admin/event-schemas/{schemaId}/enable` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT` 或 `DISABLED` 可启用为 `ENABLED`。只有 `ENABLED` schema 可用于事件校验。

`PATCH /api/v1/plugin-integration/admin/event-schemas/{schemaId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 可禁用为 `DISABLED`。重复禁用保持幂等。

### 事件接口

`POST /api/v1/plugin-integration/admin/events/ingest` 请求字段包括 `providerId`、`eventType`、`sourcePlugin`、`sourceInstanceId`、`dedupeKey`、`payload`、`occurredAt`、`reason` 和 `idempotencyKey`。第一版只允许后台受控 token 或测试适配器模拟，不开放无鉴权公网 webhook。provider 必须为 `ENABLED`，事件类型必须在 `allowedEventTypes` 中，来源必须命中 `allowedOrigins` 或后台受控来源。事件必须命中 `ENABLED` schema，且 payload 必须包含 `requiredFields`，不得包含 `sensitiveFields`、服务端可信字段、内部路径、token 或完整请求头。成功响应 HTTP `201`，`rawPayloadStored=false`，只保存脱敏 `payloadSummary`。schema 不存在返回 `49802`，payload 不符合 schema 返回 `49814`，来源或类型不允许返回 `49815`，事件写入失败返回 `55703`。

`GET /api/v1/plugin-integration/admin/events` 支持 `page`、`pageSize`、`providerId`、`eventType`、`sourcePlugin`、`validationStatus`、`routeStatus`、`syncStatus`、`notificationStatus`、`from`、`to`、`keyword` 和 `sort`。`sort` 允许 `receivedAt_desc`、`receivedAt_asc`、`processedAt_desc`。成功响应分页 `items` 为 `PluginEvent[]`。

`GET /api/v1/plugin-integration/admin/events/{eventId}` 返回事件详情、路由摘要、同步任务摘要和审计摘要。事件不存在返回 `49803`。响应不得返回 raw payload、token、内部 URL、内部路径、完整请求头或异常堆栈。

`POST /api/v1/plugin-integration/admin/events/{eventId}/replay` 请求字段包括 `reason`、`confirmText`、`idempotencyKey` 和可选 `targetRuleIds`。`confirmText` 必须为 `REPLAY_PLUGIN_EVENT`。只有 `VALIDATED` 或 `REJECTED` 后经修复且仍在 7 天窗口内的事件可重放；重复重放同一幂等键返回同一结果；窗口过期或事件状态不允许返回 `49816`。`targetRuleIds` 传入时每个规则必须存在、启用、事件类型匹配且不得指向 `OPS_CONTROL`，否则返回 `49804`、`49810` 或 `49817`。重放不修改原事件 payload，只创建新的处理摘要、同步任务或失败摘要。

### 路由规则接口

`GET /api/v1/plugin-integration/admin/route-rules` 支持 `page`、`pageSize`、`eventType`、`targetModule`、`enabled`、`riskLevel`、`keyword` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`。成功响应分页 `items` 为 `PluginRouteRule[]`。

`GET /api/v1/plugin-integration/admin/route-rules/{ruleId}` 返回规则详情、最近命中事件和最近同步摘要。不存在返回 `49804`。

`POST /api/v1/plugin-integration/admin/route-rules` 请求字段包括 `displayName`、`eventType`、`matchers`、`targetModule`、`targetAction`、`enabled`、`riskLevel`、`rateLimitSummary`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`。`targetModule` 和 `riskLevel` 必须命中本文档枚举表，非法枚举返回 `40001`。同一 eventType、targetModule、targetAction 和 matcher 指纹冲突返回 `49811`。`targetModule=ONLINE_MAP` 且会公开创建对象、`targetModule=OPS_CONTROL`、`riskLevel=HIGH` 或 `CRITICAL` 时必须携带 `confirmText=CONFIGURE_PLUGIN_ROUTE`，否则返回 `42003`。第一版不允许真实写 `OPS_CONTROL`，对应创建或启用返回 `49817` 或保存为禁用规则。

`PATCH /api/v1/plugin-integration/admin/route-rules/{ruleId}` 可修改规则字段，`reason` 必填。修改 target、matcher、riskLevel 或启用高风险路由必须携带 `confirmText=UPDATE_PLUGIN_ROUTE`。`ARCHIVED` 规则不可修改。

`PATCH /api/v1/plugin-integration/admin/route-rules/{ruleId}/enable` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。中低风险规则无需 confirmText，高风险规则必须为 `ENABLE_PLUGIN_ROUTE`。`DISABLED` 可启用为 `ENABLED`。重复启用保持幂等。

`PATCH /api/v1/plugin-integration/admin/route-rules/{ruleId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 可禁用为 `DISABLED`。禁用不会删除历史事件和同步任务。

### 同步任务接口

`POST /api/v1/plugin-integration/admin/sync-tasks` 请求字段包括 `providerId`、`eventId`、`targetModule`、`targetAction`、`params`、`riskLevel`、`reason`、`confirmText` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `PluginSyncTask`。`providerId` 和 `eventId` 必须存在且属于同一 provider，不存在返回 `49800` 或 `49803`，不匹配返回 `49810`。`targetModule` 和 `riskLevel` 必须命中本文档枚举表，非法枚举返回 `40001`。第一版对真实写 `ONLINE_MAP`、`NOTIFICATION`、`ALERTING` 以外的目标默认返回 `SIMULATED_BLOCKED` 或 `49817`，不得伪造真实成功。`riskLevel=HIGH` 或目标会公开对象时必须携带 `confirmText=CREATE_PLUGIN_SYNC_TASK`。同步任务写入失败返回 `55704`。

`GET /api/v1/plugin-integration/admin/sync-tasks` 支持 `page`、`pageSize`、`providerId`、`eventId`、`targetModule`、`status`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`riskLevel_desc`。成功响应分页 `items` 为 `PluginSyncTask[]`。

`GET /api/v1/plugin-integration/admin/sync-tasks/{taskId}` 返回任务详情、事件摘要、目标摘要和依赖摘要。不存在返回 `49805`。

`PATCH /api/v1/plugin-integration/admin/sync-tasks/{taskId}/cancel` 请求字段为 `reason` 和 `idempotencyKey`。只有 `QUEUED`、`RUNNING` 或 `SIMULATED_BLOCKED` 可取消。`SUCCEEDED`、`FAILED`、`CANCELED` 和 `TIMEOUT` 为终态，重复取消按状态冲突返回 `49810`。

### 健康快照和对象映射接口

`GET /api/v1/plugin-integration/admin/providers/{providerId}/health-snapshots` 支持 `page`、`pageSize`、`healthStatus`、`from`、`to` 和 `sort`。`sort` 允许 `checkedAt_desc`、`checkedAt_asc`。provider 不存在返回 `49800`。成功响应分页 `items` 为 `PluginHealthSnapshot[]`。

`GET /api/v1/plugin-integration/admin/object-mappings` 支持 `page`、`pageSize`、`providerId`、`sourcePlugin`、`sourceObjectType`、`targetModule`、`targetObjectType`、`status`、`visibility`、`keyword` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`lastSyncedAt_desc`。成功响应分页 `items` 为 `PluginObjectMapping[]`。

`GET /api/v1/plugin-integration/admin/object-mappings/{mappingId}` 返回映射详情和最近同步摘要。不存在返回 `49806`。

`PUT /api/v1/plugin-integration/admin/object-mappings/{mappingId}` 请求字段包括 `providerId`、`sourcePlugin`、`sourceObjectType`、`sourceObjectKey`、`targetModule`、`targetObjectType`、`targetObjectId`、`status`、`visibility`、`syncHash`、`reason` 和 `idempotencyKey`。成功响应 HTTP `200` 或首次创建 HTTP `201`。同一 provider、sourcePlugin、sourceObjectType、sourceObjectKey 不能映射到不同未归档 target，冲突返回 `49811`。公开映射或 `targetModule=ONLINE_MAP` 且 `visibility=PUBLIC` 时必须携带 `confirmText=UPSERT_PLUGIN_OBJECT_MAPPING`。

`PATCH /api/v1/plugin-integration/admin/object-mappings/{mappingId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。`ACTIVE`、`STALE` 或 `CONFLICTED` 可归档为 `ARCHIVED`。归档后不再被同步任务自动选中。重复归档保持幂等。

### 审计接口

`GET /api/v1/plugin-integration/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`providerId`、`eventId`、`schemaId`、`ruleId`、`taskId`、`mappingId`、`result`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`、`riskLevel_desc`。只有 `ADMIN` 和 `OWNER` 可访问。审计列表只读，不提供删除、修改或恢复接口。

后台写操作必须记录调用者、`reason`、操作前状态、操作后状态、请求编号、结果和失败原因。审计写入失败时，provider、schema、事件、路由规则、同步任务、对象映射和健康快照的写操作不得假装成功，必须返回 `55701` 并保持业务状态不变。notification 失败不回滚事件主状态，但必须记录脱敏失败摘要。

### 状态、幂等和并发

provider 状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DEGRADED` 或 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`DEGRADED` 可恢复为 `ENABLED`、禁用或归档；`ARCHIVED` 为终态。

schema 状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

事件状态由校验、路由、同步和通知摘要共同组成。事件接收后先进入 `RECEIVED`，校验通过为 `VALIDATED`，失败为 `REJECTED`。路由状态可为 `PENDING`、`ROUTED`、`IGNORED` 或 `FAILED`。同步状态可为 `SKIPPED`、`QUEUED`、`SIMULATED_BLOCKED`、`SYNCED` 或 `FAILED`。事件一旦保存，不允许直接修改 payload；重放必须使用重放接口生成新的处理摘要。

同步任务状态流转为 `QUEUED` 到 `RUNNING`、`SIMULATED_BLOCKED`、`FAILED` 或 `CANCELED`；`RUNNING` 到 `SUCCEEDED`、`FAILED`、`TIMEOUT` 或 `CANCELED`；`SIMULATED_BLOCKED` 可取消或在后续真实适配后重新入队；`SUCCEEDED`、`FAILED`、`CANCELED` 和 `TIMEOUT` 为终态。

对象映射状态流转为 `ACTIVE` 到 `STALE`、`CONFLICTED` 或 `ARCHIVED`；`STALE` 和 `CONFLICTED` 可回到 `ACTIVE` 或归档；`ARCHIVED` 为终态。

创建、修改、状态流转、事件接收、事件重放、同步任务创建、取消任务、对象映射 upsert 和归档均支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `49812`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序，数组保留顺序，不能依赖 Java `Map.toString()` 或浏览器字段顺序。

并发创建相同 provider、schema、路由规则、事件 dedupeKey、同步任务或对象映射时只能一个成功，其余返回冲突或相同幂等结果。所有写接口必须在同一个临界区内完成状态校验、业务写入、审计写入、幂等记录和响应快照保存。后续数据库实现必须迁移为事务、唯一约束、条件更新或等效机制，不能降低并发口径。

### 安全、降级和脱敏

任何请求体和响应都不得包含访问 token、插件 token、插件 secret、webhook secret、Discord token、SMTP 密码、短信 token、完整 Authorization 请求头、完整请求 headers、完整 raw payload、内部 URL、内部路径、真实世界目录、节点地址、服务器密码、RCON 密码、完整异常栈、数据库连接串、`.env`、`authorized_keys`、`id_rsa`、shell 命令或前序服务私有数据。检查必须递归覆盖嵌套对象和数组。

外部 endpoint 和 allowed origins 必须拒绝 `file:`、`data:`、`javascript:`、带用户名密码 URL、localhost、回环 IP、内网 IP、链路本地地址、未解析 host、通配符 `*`、空 host、控制字符和非法 URI。`eventEndpointSummary` 只能是公开安全摘要或站内受控路径，不得是内网完整地址。站内受控路径必须以 `/` 开头，不能以 `//` 开头，不能包含反斜杠或控制字符。

事件 payload 必须按 schema 脱敏和摘要化。第一版 `rawPayloadStored` 固定为 `false`。`payloadSummary` 只能保存字段名、类型、必要业务摘要和已脱敏值，不得保存完整玩家 IP、完整请求头、外部 token、路径或密钥。

依赖不可用时，读取类接口可以返回已有快照并标记 `stale=true`、`degraded=true` 和 `degradeReasons`。写入类接口不得假装成功。通知失败不得伪造成投递成功。真实同步被第一版阻断时必须返回 `SIMULATED_BLOCKED` 或 `49817`。

第一版不得提供真实删除 provider、schema、事件、规则、任务、映射或审计的接口。确需清理历史记录时，必须在后续独立契约中增加归档或保留策略接口，并重新完成文档、测试红灯、实现和回归闭环。

### 验收口径

`plugin-integration` API 文档必须按 `docs/contracts-plugin-integration.md` 独立存在，并由 `.local-docs/tests-ops-core.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求、敏感字段脱敏、测试控制头默认关闭和模块验收口径。

`plugin-integration` 完成时必须满足以下条件：当前运行入口为 `ops-core-service:8133`，历史端口 `8122` 只作为 `legacyPort` 返回；健康检查公开且不泄露敏感信息；后台接口按角色和能力点限制；provider、实例、能力、schema、事件、路由规则、同步任务、健康快照、对象映射、审计、幂等、状态流转、来源 allowlist、payload 脱敏、依赖降级、通知失败摘要、审计失败回滚、敏感字段脱敏、测试控制头默认关闭和自检摘要都有自动化验证；自动化测试必须先红灯；实现后 `plugin-integration` 在 `ops-core-service` 中全部测试通过；当前后端运行入口回归测试通过；边界扫描无违规命中；不修改前序服务稳定接口；不直接读取前序服务数据库；不导入前序服务 Java package；不调用真实 `external-node-executor`；不执行真实插件命令；不写真实插件配置；不保存真实插件 token、webhook secret 或外部平台密钥；不把地图主数据、告警规则、通知渠道、资源下载、节点文件管理、终端、备份恢复或跨平台通知主数据塞进 `plugin-integration`。

## 北冥官网 cross-platform-notification API 契约

来源：`docs/contracts-cross-platform-notification.md`

版本：0.8

### 文档定位

本文档是 `cross-platform-notification` 微服务的正式 API 契约。`cross-platform-notification` 负责跨平台外部通知控制面，包括外部渠道 provider 摘要、渠道能力、模板映射、路由策略、投递请求、投递尝试、receiver 摘要、重试摘要、失败降级、幂等记录、审计日志和自检摘要。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、运维能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `cross-platform-notification` 的职责边界、数据归属、前序服务兼容、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考 Slack Incoming Webhooks、Discord Webhooks、Telegram Bot API、Twilio Messaging、Firebase Cloud Messaging、OneSignal、Novu 和 Grafana Alerting 的公开设计。Slack Incoming Webhooks 明确 webhook URL 本身包含 secret，适合约束本服务不回显完整 webhook。Discord Webhooks 说明 webhook token、内容长度、allowed mentions 和服务端确认差异，适合本服务建立渠道能力和内容降级模型。Telegram Bot API 说明 bot token、HTTPS 请求、webhook secret token 和更新重试，适合本服务区分发送 token、回调签名和失败摘要。Twilio Messaging 的状态回调说明外部投递会经历 queued、sent、delivered、failed 等状态，适合本服务保留投递尝试和后续回调适配位置。FCM 的跨平台消息、topic、device token 和平台差异化配置，适合本服务抽象 `PUSH` 渠道能力和 receiver 摘要。OneSignal 的 Push、In-App、Email、SMS 和 Live Activities 多渠道模型，适合本服务拆分渠道、订阅和模板。Novu 的 workflow、channel steps、delay、digest 和 subscriber preference 说明通知编排应独立于业务主数据。Grafana Alerting 的 contact point、notification policy、grouping、silence 和 template 说明路由策略、分组抑制和接收方配置要和告警规则分离。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Slack Incoming Webhooks](https://docs.slack.dev/messaging/sending-messages-using-incoming-webhooks/) | webhook URL 是 secret，响应和审计不得回显完整 URL 或 token。 |
| [Discord Webhook Resource](https://discord.com/developers/docs/resources/webhook) | webhook token、内容长度、allowed mentions、wait 确认和执行响应差异需要抽象为渠道能力。 |
| [Telegram Bot API](https://core.telegram.org/bots/api) | bot token、HTTPS Bot API、webhook secret token 和更新重试都必须归入凭据和回调边界。 |
| [Twilio Messaging Webhooks](https://www.twilio.com/docs/usage/webhooks/messaging-webhooks) | 外部消息投递状态应支持状态回调、失败码和生命周期摘要。 |
| [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging) | 跨平台推送需要区分 topic、device token、平台配置和 payload 限制。 |
| [OneSignal Channel Setup](https://documentation.onesignal.com/docs/channel-setup) | Push、In-App、Email、SMS 等渠道有不同配置、receiver 和发送约束。 |
| [Novu Workflows](https://docs.novu.co/platform/concepts/workflows) | workflow、channel steps、delay、digest 和 preference 需要和业务事件解耦。 |
| [Grafana Alerting Notifications](https://grafana.com/docs/grafana/latest/alerting/fundamentals/notifications/) | contact point、notification policy、grouping、silence 和 template 应独立建模。 |

本文档只吸收这些生态的设计思路，不接入它们的 SDK，不保存真实外部平台 token，不发送真实外部消息，不开放真实 webhook 回调入口。

### 职责边界

`cross-platform-notification` 负责外部通知渠道控制面。它保存外部渠道 provider 的脱敏配置摘要、渠道能力、模板映射、路由策略、投递请求摘要、投递尝试记录、receiver 摘要、重试摘要、依赖摘要、幂等记录、审计日志和自检摘要。

`cross-platform-notification` 不负责注册、登录、会话、用户角色、站内通知主数据、站内未读数、站内已读归档、告警规则、告警实例、插件事件、社区工单、活动报名、日历事件、白名单审核、考勤积分、运维任务、节点执行、Minecraft 命令、真实 SMTP、真实短信网关、真实 QQ/Oopz/Discord/Slack/Telegram/企业微信机器人托管、真实推送 token 管理或外部平台账号绑定主数据。

第一版固定为安全控制面和投递模拟。它可以创建 `SIMULATED_SENT`、`SIMULATED_FAILED`、`BLOCKED`、`RETRY_SCHEDULED`、`CANCELED` 和 `EXPIRED` 状态，不能返回真实 `SENT`，不能对外发邮件、短信、聊天消息、机器人消息、Webhook、移动推送或游戏内消息。后续开启真实发送必须重新补充正式契约、测试文档、红灯测试、生产凭据托管、回调签名、速率限制、死信队列、隐私脱敏和回归记录。

### 数据归属

`cross-platform-notification` 拥有以下主数据：ExternalChannelProvider、ExternalChannelCapability、ExternalTemplateMapping、ExternalRoutePolicy、ExternalDeliveryRequest、ExternalDeliveryAttempt、ExternalReceiverSummary、ExternalNotificationAuditLog、CrossPlatformNotificationOpsSummary 和幂等记录。

`cross-platform-notification` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和状态快照；可以保存来自 `notification` 的站内通知引用、模板引用和投递偏好安全摘要；可以保存来自 `alerting` 的告警投递请求摘要；可以保存来自 `plugin-integration` 的插件事件和插件健康异常摘要；可以保存来自 `ops-control` 的高风险审批、节点异常和任务失败摘要；可以保存来自 `community`、`activity`、`calendar`、`changelog`、`whitelist`、`attendance`、`resource` 和 `server-status` 的业务来源摘要。

所有跨模块字段只能是安全快照，不得成为来源模块主数据，不得用于绕过来源模块权限，不得反向修改来源模块状态。`cross-platform-notification` 不能直接读取其他服务数据库，不能导入前序服务 Java package，不能复用前序服务内存 store，不能修改 `notification` 未读数，不能关闭 `alerting` 告警，不能重放 `plugin-integration` 事件，不能创建 `ops-control` 任务。

### 基础路径、端口和认证

所有接口默认使用 `/api/v1/cross-platform-notification` 前缀。第六期运行合并后当前运行入口为 `ops-core-service:8133`，自检摘要必须返回 `port=8133` 和 `legacyPort=8123`。历史独立端口 `8123` 只作为追溯字段，不再作为当前网关上游、当前 Maven 测试入口或独立部署入口。

健康检查 `GET /api/v1/cross-platform-notification/health` 不要求认证，只能返回 `service`、`version`、`status` 和 `requestId`，不得返回 provider 数量、receiver、endpoint、外部平台错误详情、依赖明细或任何敏感字段。

后台接口统一使用 `/api/v1/cross-platform-notification/admin` 前缀，全部要求 `Authorization: Bearer <token>`。后台读取接口要求基础角色为 `HELPER`、`ADMIN` 或 `OWNER`，并具备 `NODE_READ` 或等效后台读取能力。后台写接口要求 `ADMIN` 或 `OWNER`，并具备 `NODE_WRITE`。涉及外部 endpoint、receiver、provider 启用、路由启用、测试路由、投递请求、批量重试、取消高风险投递或真实发送开关的接口要求 `HIGH_RISK_APPROVE` 或 `OWNER`，并要求固定 `confirmText`。`ADMIN` 具备 `NODE_WRITE` 但缺少 `HIGH_RISK_APPROVE` 时，高风险写接口必须返回 `42002`；`OWNER` 可绕过该能力点但仍必须满足确认文本、状态流转和审计规则。

第一版支持公共风险等级 `LOW`、`MEDIUM`、`HIGH` 和 `CRITICAL`。`HIGH` 写操作要求 `HIGH_RISK_APPROVE` 或 `OWNER`。`CRITICAL` 只允许 `OWNER` 执行；非 `OWNER` 账号即使具备 `HIGH_RISK_APPROVE`，在创建或更新 provider 允许风险等级、创建或更新路由、创建投递时传入 `CRITICAL` 也必须返回 `42004`。后续如果接入独立审批记录，必须先补充本契约、测试文档、红灯测试和回归记录。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`beforeState`、`afterState`、`auditResult`、`createdBy`、`updatedBy`、`enabledBy`、`disabledBy`、`archivedBy`、`rawPayload`、`rawToken`、`webhookSecret`、`discordToken`、`qqToken`、`oopzToken`、`smtpPassword`、`smsToken`、`botToken`、`rconPassword`、`credential`、`secretKey`、`Authorization`、`requestHeaders`、`internalUrl`、`internalPath`、`resolvedPath`、`fullException`、`databaseUrl`、`deliveryStatus`、`attemptStatus`、`externalMessageId` 和 `providerRawResponse` 等服务端可信字段。可信字段必须递归检查，嵌套在 `payloadSummary`、`receiverSummary`、`endpointSummary`、`metadata`、`matchers`、`requestSummary`、`responseSummary` 或任意数组对象中也必须拒绝。出现可信字段时返回 `40001`。

### 本地测试控制头

`cross-platform-notification` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Notification-Mode`、`X-Test-Alerting-Mode`、`X-Test-Plugin-Integration-Mode`、`X-Test-Source-Mode`、`X-Test-Provider-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store`、`X-Test-Fail-Delivery` 和 `X-Test-Now` 模拟认证失败、依赖不可用、依赖超时、schema 不兼容、provider 降级、模拟投递失败、审计失败、状态写入失败、投递记录写入失败和时间边界。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发认证失败、依赖失败、provider 失败、审计失败、存储失败、投递失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 纳入生产化硬化项。

### 前序服务兼容契约

`auth` 是后台接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。auth 不可用返回 `47150`，auth 超时返回 `47151`，auth 字段或枚举不兼容返回 `47152`。

`notification` 是站内通知和站内模板主数据来源。`cross-platform-notification` 可以读取站内通知引用、模板引用、模板变量白名单和通知偏好安全摘要，不能修改 notification 的站内通知、未读数、已读归档或模板主数据。notification 不可用返回 `47120`，超时返回 `47121`，schema 不兼容返回 `47122`。notification 不可用时，读取类接口可以返回已有脱敏快照并标记 `degraded=true`；创建关联站内通知的投递请求不得伪造关联成功。

`alerting` 是告警来源方。`cross-platform-notification` 可以接收或模拟来自 alerting 的外部投递请求摘要，不能修改告警规则、告警实例、静默、确认或关闭状态。alerting 不可用返回 `47130`，超时返回 `47131`，schema 不兼容返回 `47132`。外部投递失败不能自动关闭告警，也不能把告警投递摘要伪造成成功。

来自 `alerting` 的内部适配请求必须使用 `sourceModule=alerting`。请求字段只允许安全摘要，至少包括 `sourceId`、`eventType=alert.firing`、`riskLevel`、`routeId` 或 `providerId` 与 `templateMappingId`、`receiverSummary`、`payloadSummary`、`expiresAt`、`reason` 和 `idempotencyKey`。`payloadSummary` 只能包含模板允许变量的摘要值，不能包含完整日志、完整告警正文、完整请求头、内部路径、token、外部渠道凭据或异常堆栈。`riskLevel` 映射规则为 `INFO -> LOW`、`WARNING -> MEDIUM`、`CRITICAL -> HIGH`、`BLOCKER -> CRITICAL`。相同 `alertId + routeId + fingerprint + idempotencyKey` 只能创建一条 delivery 和一条 attempt；同一幂等键不同请求体返回 `49962`。审计必须记录 `sourceModule=alerting`、`sourceId`、`routeId`、`deliveryId`、`attemptId`、风险等级和脱敏参数摘要。

`plugin-integration` 是插件事件来源方。`cross-platform-notification` 可以保存插件事件通知摘要和模拟投递结果，不能修改插件 provider、事件、路由规则、同步任务或对象映射。plugin-integration 不可用返回 `47140`，超时返回 `47141`，schema 不兼容返回 `47142`。

其他业务来源模块包括 `ops-control`、`external-node-executor`、`community`、`activity`、`calendar`、`changelog`、`whitelist`、`attendance`、`resource` 和 `server-status`。本服务只能保存来源模块传入或正式 API 返回的安全摘要。来源模块不可用返回 `47160`，超时返回 `47161`，schema 不兼容返回 `47162`。`external-node-executor` 只能作为来源摘要出现，本服务不得直连节点，不得执行命令。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `ExternalChannel` | `EMAIL`、`SMS`、`QQ`、`OOPZ`、`DISCORD`、`SLACK`、`TELEGRAM`、`WECHAT_WORK`、`GAME`、`PUSH`、`WEBHOOK`、`CUSTOM` | 外部渠道类型。第一版只模拟投递。 |
| `ExternalProviderStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`DEGRADED`、`ARCHIVED` | provider 控制面状态。 |
| `ExternalProviderHealthStatus` | `HEALTHY`、`DEGRADED`、`UNAVAILABLE`、`UNKNOWN` | provider 健康摘要。 |
| `ExternalTemplateMappingStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`ARCHIVED` | 模板映射状态。 |
| `ExternalRenderMode` | `PLAIN_TEXT`、`MARKDOWN`、`RICH_BLOCK`、`PLATFORM_TEMPLATE` | 渲染模式。第一版只保存摘要和模拟渲染。 |
| `ExternalRoutePolicyStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`ARCHIVED` | 路由策略状态。 |
| `ExternalDeliveryStatus` | `QUEUED`、`SIMULATED_SENT`、`SIMULATED_FAILED`、`RETRY_SCHEDULED`、`CANCELED`、`EXPIRED`、`BLOCKED` | 投递请求状态。第一版不得出现真实 `SENT`。 |
| `ExternalAttemptStatus` | `SIMULATED_SUCCESS`、`SIMULATED_FAILURE`、`BLOCKED_BY_POLICY`、`RATE_LIMITED`、`DEPENDENCY_FAILED` | 投递尝试状态。 |
| `ExternalReceiverType` | `USER`、`ROLE`、`GROUP`、`CHANNEL`、`TOPIC`、`WEBHOOK_ENDPOINT`、`DEVICE_TOKEN`、`EMAIL_ADDRESS`、`PHONE_NUMBER`、`GAME_TARGET`、`CUSTOM` | receiver 摘要类型。 |
| `ExternalDependencyStatus` | `AVAILABLE`、`UNAVAILABLE`、`TIMEOUT`、`BAD_SCHEMA`、`STALE`、`SKIPPED` | 依赖摘要状态。 |
| `ExternalNotificationAuditResult` | `SUCCESS`、`FAILED` | 审计结果。 |

`sourceModule` 使用模块英文名，例如 `notification`、`alerting`、`plugin-integration`、`ops-control`、`community`、`activity`、`calendar`、`changelog`、`whitelist`、`attendance`、`resource`、`server-status` 和 `custom`。第一版不允许浏览器伪装为 `auth`、`external-node-executor` 或内部系统用户。浏览器传入未列入本契约的来源模块、`auth`、`external-node-executor` 或以 `internal`、`system` 开头的来源模块时必须返回 `40001`，不得创建投递、路由、模板映射或审计记录。`sourceModule=alerting` 可以由后台接口或同进程受控适配器创建，但必须继续执行 provider、模板、路由、receiver、payload 白名单、幂等和审计校验。

### 通用对象

#### ExternalChannelProvider

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `providerId` | string | 是 | provider ID。 |
| `channel` | string | 是 | `ExternalChannel`。 |
| `displayName` | string | 是 | 展示名称，2 到 80 位。 |
| `status` | string | 是 | `ExternalProviderStatus`。 |
| `endpointSummary` | object 或 null | 是 | endpoint 脱敏摘要，只保存域名、路径类型和协议摘要，不返回完整 URL。 |
| `credentialRefSummary` | object 或 null | 是 | 凭据引用摘要，只能是凭据别名、托管方式和轮换时间，不返回真实凭据。 |
| `receiverPolicy` | object | 是 | receiver 允许类型、最大数量、隐私脱敏和来源限制。 |
| `allowedSourceModules` | string[] | 是 | 允许来源模块，最多 30 个。 |
| `allowedRiskLevels` | string[] | 是 | 允许风险等级，取公共风险等级。 |
| `rateLimitSummary` | object | 是 | 速率限制摘要，包含窗口、容量和当前降级原因，不返回平台 secret。 |
| `healthStatus` | string | 是 | `ExternalProviderHealthStatus`。 |
| `lastTestAt` | string 或 null | 是 | 最近测试时间。 |
| `lastDeliveryAt` | string 或 null | 是 | 最近模拟投递时间。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReasons` | string[] | 是 | 降级原因，必须脱敏。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### ExternalChannelCapability

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `capabilityId` | string | 是 | 能力 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `channel` | string | 是 | `ExternalChannel`。 |
| `supportsMarkdown` | boolean | 是 | 是否支持 Markdown 或等价文本格式。 |
| `supportsRichBlocks` | boolean | 是 | 是否支持 rich block、embed、button 或卡片摘要。 |
| `supportsImages` | boolean | 是 | 是否支持图片摘要。第一版不上传真实图片。 |
| `supportsThreads` | boolean | 是 | 是否支持 thread/topic 回复。 |
| `supportsMentions` | boolean | 是 | 是否支持 @ 提及。第一版必须默认禁用危险提及。 |
| `supportsDeliveryCallback` | boolean | 是 | 是否支持后续状态回调。第一版只保存能力摘要，不开放真实回调。 |
| `maxTitleLength` | integer | 是 | 标题最大长度。 |
| `maxBodyLength` | integer | 是 | 正文最大长度。 |
| `maxReceiversPerRequest` | integer | 是 | 单次 receiver 最大数量。 |
| `rateLimitSummary` | object | 是 | 速率限制摘要。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### ExternalTemplateMapping

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `mappingId` | string | 是 | 模板映射 ID。 |
| `sourceModule` | string | 是 | 来源模块。 |
| `sourceTemplateRef` | object | 是 | 来源模板引用摘要，例如 notification 模板 code 或 alerting 模板名。 |
| `providerId` | string | 是 | provider ID。 |
| `channel` | string | 是 | `ExternalChannel`。 |
| `externalTemplateKey` | string 或 null | 是 | 外部平台模板键摘要，最多 120 位。第一版不调用真实平台模板。 |
| `allowedVariables` | string[] | 是 | 允许变量名，最多 50 个。 |
| `renderMode` | string | 是 | `ExternalRenderMode`。 |
| `fallbackTitleTemplate` | string | 是 | 脱敏标题模板，2 到 120 位。 |
| `fallbackBodyTemplate` | string | 是 | 脱敏正文模板，1 到 3000 位。 |
| `status` | string | 是 | `ExternalTemplateMappingStatus`。 |
| `version` | integer | 是 | 版本，从 `1` 开始。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### ExternalRoutePolicy

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `routeId` | string | 是 | 路由策略 ID。 |
| `displayName` | string | 是 | 路由名称，2 到 80 位。 |
| `sourceModule` | string | 是 | 来源模块。 |
| `eventType` | string | 是 | 事件类型，3 到 120 位。 |
| `riskLevel` | string | 是 | 公共风险等级。 |
| `matchers` | object | 是 | 匹配器，第一版支持 `sourceModule`、`eventType`、`riskLevel`、`labels` 和 `receiverType` 精确匹配。 |
| `providerId` | string | 是 | provider ID。 |
| `templateMappingId` | string | 是 | 模板映射 ID。 |
| `receiverSummary` | object | 是 | 默认 receiver 摘要，不得包含完整账号、手机号、邮箱、token 或 webhook。 |
| `groupingPolicy` | object | 是 | 分组摘要，包括 `groupBy`、`groupWaitSeconds`、`groupIntervalSeconds`。 |
| `retryPolicySummary` | object | 是 | 重试摘要，包括最大尝试数、退避窗口和过期窗口。 |
| `status` | string | 是 | `ExternalRoutePolicyStatus`。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### ExternalDeliveryRequest

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `deliveryId` | string | 是 | 投递请求 ID。 |
| `sourceModule` | string | 是 | 来源模块。 |
| `sourceId` | string 或 null | 是 | 来源业务 ID。 |
| `eventType` | string | 是 | 事件类型。 |
| `riskLevel` | string | 是 | 公共风险等级。 |
| `routeId` | string 或 null | 是 | 命中的路由策略 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `channel` | string | 是 | `ExternalChannel`。 |
| `templateMappingId` | string 或 null | 是 | 模板映射 ID。 |
| `receiverSummary` | object | 是 | receiver 脱敏摘要。 |
| `payloadSummary` | object | 是 | 载荷摘要，只保存变量白名单、字段名、字段数量、字符串长度、值类型和短 hash，不保存完整通知正文或完整变量值。 |
| `status` | string | 是 | `ExternalDeliveryStatus`。 |
| `attempts` | integer | 是 | 尝试次数。 |
| `lastAttemptAt` | string 或 null | 是 | 最近尝试时间。 |
| `nextRetryAt` | string 或 null | 是 | 下次重试时间。 |
| `expiresAt` | string 或 null | 是 | 投递过期时间。 |
| `failureCode` | string 或 null | 是 | 失败码摘要。 |
| `failureSummary` | string 或 null | 是 | 失败摘要，必须脱敏。 |
| `idempotencyKey` | string 或 null | 是 | 幂等键摘要，可脱敏显示。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### ExternalDeliveryAttempt

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `attemptId` | string | 是 | 投递尝试 ID。 |
| `deliveryId` | string | 是 | 投递请求 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `channel` | string | 是 | `ExternalChannel`。 |
| `attemptNo` | integer | 是 | 尝试序号，从 `1` 开始。 |
| `status` | string | 是 | `ExternalAttemptStatus`。 |
| `requestSummary` | object | 是 | 请求摘要，不返回完整正文、headers、URL 或 secret。 |
| `responseSummary` | object | 是 | 响应摘要，不返回完整平台响应或 token。 |
| `failureCode` | string 或 null | 是 | 失败码摘要。 |
| `failureSummary` | string 或 null | 是 | 失败摘要。 |
| `startedAt` | string | 是 | 开始时间。 |
| `finishedAt` | string 或 null | 是 | 完成时间。 |
| `simulated` | boolean | 是 | 第一版固定为 `true`。 |

#### ExternalReceiverSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `receiverId` | string | 是 | receiver 摘要 ID。 |
| `receiverType` | string | 是 | `ExternalReceiverType`。 |
| `channel` | string | 是 | `ExternalChannel`。 |
| `displayName` | string | 是 | 脱敏展示名，2 到 80 位。 |
| `providerId` | string 或 null | 是 | 关联 provider。 |
| `sourceModule` | string 或 null | 是 | 来源模块。 |
| `targetRefSummary` | object | 是 | 目标引用摘要，不返回完整邮箱、手机号、账号、webhook、token 或设备 token。 |
| `verified` | boolean | 是 | 是否已由来源模块或 provider 摘要确认。 |
| `lastUsedAt` | string 或 null | 是 | 最近使用时间。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |

#### ExternalNotificationAuditLog

审计字段继承公共契约，允许补充 `providerId`、`capabilityId`、`mappingId`、`routeId`、`deliveryId`、`attemptId`、`receiverId`、`sourceModule`、`dependencyStatus`、`stateFrom`、`stateTo`、`idempotencyKey`、`confirmTextMatched` 和 `failureReason`。审计列表只读，不提供删除接口。审计响应不得返回 token、secret、完整 webhook、完整请求头、完整通知正文、内部 URL、内部路径、完整异常栈、数据库连接串或前序服务私有数据。

#### CrossPlatformNotificationOpsSummary

字段至少包含 `service`、`port`、`storageMode`、`authMode`、`providerAdapterMode`、`notificationAdapterMode`、`testControlsEnabled`、`providersTotal`、`enabledProvidersTotal`、`templateMappingsTotal`、`enabledTemplateMappingsTotal`、`routesTotal`、`enabledRoutesTotal`、`deliveriesTotal`、`simulatedSentTotal`、`simulatedFailedTotal`、`retryScheduledTotal`、`attemptsTotal`、`receiversTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastDeliveryAt`、`lastFailureAt`、`degraded`、`degradeReasons` 和 `productionGaps`。

### 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `47120` | 502 | notification 不可用。 |
| `47121` | 504 | notification 调用超时。 |
| `47122` | 502 | notification 响应不兼容。 |
| `47130` | 502 | alerting 不可用。 |
| `47131` | 504 | alerting 调用超时。 |
| `47132` | 502 | alerting 响应不兼容。 |
| `47140` | 502 | plugin-integration 不可用。 |
| `47141` | 504 | plugin-integration 调用超时。 |
| `47142` | 502 | plugin-integration 响应不兼容。 |
| `47150` | 502 | auth 认证上下文不可用。 |
| `47151` | 504 | auth 认证上下文调用超时。 |
| `47152` | 502 | auth 认证上下文字段不兼容。 |
| `47160` | 502 | 来源模块不可用。 |
| `47161` | 504 | 来源模块调用超时。 |
| `47162` | 502 | 来源模块摘要不兼容。 |
| `49950` | 404 | provider 不存在。 |
| `49951` | 404 | 模板映射不存在。 |
| `49952` | 404 | 路由策略不存在。 |
| `49953` | 404 | 投递请求不存在。 |
| `49954` | 404 | 投递尝试不存在。 |
| `49955` | 404 | receiver 摘要不存在。 |
| `49956` | 404 | 渠道能力不存在。 |
| `49960` | 409 | 状态不允许当前操作。 |
| `49961` | 409 | provider、模板映射、路由或 receiver 冲突。 |
| `49962` | 409 | 幂等键请求指纹冲突。 |
| `49963` | 400 | endpoint 或 receiver 不安全。 |
| `49964` | 400 | receiver 摘要不合法。 |
| `49965` | 400 | 模板变量不允许。 |
| `49966` | 400 | 渠道或能力不支持。 |
| `49967` | 409 | 真实发送被第一版策略阻断。 |
| `49968` | 409 | 投递重试窗口已过。 |
| `49969` | 409 | 速率限制策略冲突。 |
| `55800` | 500 | cross-platform-notification 内部错误。 |
| `55801` | 500 | cross-platform-notification 审计写入失败。 |
| `55802` | 500 | cross-platform-notification 状态写入失败。 |
| `55803` | 500 | cross-platform-notification 投递记录写入失败。 |
| `55804` | 500 | cross-platform-notification 依赖摘要写入失败。 |

字段校验、未登录、令牌格式错误、角色权限不足、能力点不足、高风险确认缺失、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/cross-platform-notification/health` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/cross-platform-notification/admin/ops/summary` | 是 | `NODE_READ` | LOW |
| provider 列表 | GET | `/api/v1/cross-platform-notification/admin/providers` | 是 | `NODE_READ` | LOW |
| provider 详情 | GET | `/api/v1/cross-platform-notification/admin/providers/{providerId}` | 是 | `NODE_READ` | LOW |
| 创建 provider | POST | `/api/v1/cross-platform-notification/admin/providers` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | HIGH |
| 更新 provider | PATCH | `/api/v1/cross-platform-notification/admin/providers/{providerId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | HIGH |
| 启用 provider | PATCH | `/api/v1/cross-platform-notification/admin/providers/{providerId}/enable` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 禁用 provider | PATCH | `/api/v1/cross-platform-notification/admin/providers/{providerId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档 provider | PATCH | `/api/v1/cross-platform-notification/admin/providers/{providerId}/archive` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 渠道能力列表 | GET | `/api/v1/cross-platform-notification/admin/capabilities` | 是 | `NODE_READ` | LOW |
| 渠道能力详情 | GET | `/api/v1/cross-platform-notification/admin/capabilities/{capabilityId}` | 是 | `NODE_READ` | LOW |
| 模板映射列表 | GET | `/api/v1/cross-platform-notification/admin/template-mappings` | 是 | `NODE_READ` | LOW |
| 模板映射详情 | GET | `/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}` | 是 | `NODE_READ` | LOW |
| 创建模板映射 | POST | `/api/v1/cross-platform-notification/admin/template-mappings` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 更新模板映射 | PATCH | `/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 启用模板映射 | PATCH | `/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/enable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 禁用模板映射 | PATCH | `/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档模板映射 | PATCH | `/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/archive` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 路由策略列表 | GET | `/api/v1/cross-platform-notification/admin/routes` | 是 | `NODE_READ` | LOW |
| 路由策略详情 | GET | `/api/v1/cross-platform-notification/admin/routes/{routeId}` | 是 | `NODE_READ` | LOW |
| 创建路由策略 | POST | `/api/v1/cross-platform-notification/admin/routes` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 更新路由策略 | PATCH | `/api/v1/cross-platform-notification/admin/routes/{routeId}` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 启用路由策略 | PATCH | `/api/v1/cross-platform-notification/admin/routes/{routeId}/enable` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 禁用路由策略 | PATCH | `/api/v1/cross-platform-notification/admin/routes/{routeId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档路由策略 | PATCH | `/api/v1/cross-platform-notification/admin/routes/{routeId}/archive` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 测试路由策略 | POST | `/api/v1/cross-platform-notification/admin/routes/{routeId}/test` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 创建投递请求 | POST | `/api/v1/cross-platform-notification/admin/deliveries` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 投递请求列表 | GET | `/api/v1/cross-platform-notification/admin/deliveries` | 是 | `NODE_READ` | LOW |
| 投递请求详情 | GET | `/api/v1/cross-platform-notification/admin/deliveries/{deliveryId}` | 是 | `NODE_READ` | LOW |
| 重试投递 | PATCH | `/api/v1/cross-platform-notification/admin/deliveries/{deliveryId}/retry` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 取消投递 | PATCH | `/api/v1/cross-platform-notification/admin/deliveries/{deliveryId}/cancel` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE`；取消 `HIGH` 或 `CRITICAL` 投递时还要求 `HIGH_RISK_APPROVE` 或 `OWNER` | 按投递风险 |
| 投递尝试列表 | GET | `/api/v1/cross-platform-notification/admin/attempts` | 是 | `NODE_READ` | LOW |
| 投递尝试详情 | GET | `/api/v1/cross-platform-notification/admin/attempts/{attemptId}` | 是 | `NODE_READ` | LOW |
| receiver 摘要列表 | GET | `/api/v1/cross-platform-notification/admin/receivers` | 是 | `NODE_READ` | LOW |
| receiver 摘要详情 | GET | `/api/v1/cross-platform-notification/admin/receivers/{receiverId}` | 是 | `NODE_READ` | LOW |
| 审计列表 | GET | `/api/v1/cross-platform-notification/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

### 健康和自检接口

`GET /api/v1/cross-platform-notification/health` 成功返回 HTTP `200`，`data` 至少包含 `service=cross-platform-notification`、`status`、`version` 和 `requestId`。进程存活但依赖不可用时可以返回 `status=DEGRADED`，但不得返回 provider、receiver、外部 endpoint、依赖详细错误、投递数量或敏感字段。

`GET /api/v1/cross-platform-notification/admin/ops/summary` 成功返回 `CrossPlatformNotificationOpsSummary`。第六期合并后必须返回 `port=8133`、`legacyPort=8123`、`storageMode=IN_MEMORY`、`providerAdapterMode=SIMULATION_ONLY`、`notificationAdapterMode=TEST_STUB`、`testControlsEnabled` 和生产化缺口。读取失败返回 `55800`，不得伪造健康。

### Provider 接口

`GET /api/v1/cross-platform-notification/admin/providers` 支持 `page`、`pageSize`、`keyword`、`channel`、`status`、`healthStatus`、`sourceModule`、`degraded`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`、`lastTestAt_desc` 和 `lastDeliveryAt_desc`。成功响应分页 `items` 为 `ExternalChannelProvider[]`。

`GET /api/v1/cross-platform-notification/admin/providers/{providerId}` 返回 provider 详情、能力摘要、最近投递摘要、最近失败摘要、依赖摘要和最近审计摘要。provider 不存在返回 `49950`。响应不得返回完整 endpoint、token、secret、真实外部账号、请求 headers 或内部 URL。

`POST /api/v1/cross-platform-notification/admin/providers` 请求字段包括 `channel`、`displayName`、`endpointSummary`、`credentialRefSummary`、`receiverPolicy`、`allowedSourceModules`、`allowedRiskLevels`、`rateLimitSummary`、`reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `REGISTER_EXTERNAL_PROVIDER`。创建默认状态为 `DRAFT`，成功响应 HTTP `201`，`data` 为 `ExternalChannelProvider`。endpoint 或 receiver 不安全返回 `49963`。同一未归档 provider 下 `channel + displayName`、规范化 `endpointSummary` 或凭据引用冲突返回 `49961`。真实 token、secret 或完整 webhook 字段出现在任意层级返回 `40001`。

`PATCH /api/v1/cross-platform-notification/admin/providers/{providerId}` 可修改创建接口中的业务字段，`reason` 必填。修改 `endpointSummary`、`credentialRefSummary`、`receiverPolicy`、`allowedSourceModules` 或 `allowedRiskLevels` 时必须携带 `confirmText=UPDATE_EXTERNAL_PROVIDER`。`ARCHIVED` provider 不允许修改，返回 `49960`。审计失败返回 `55801` 且状态不变。

`PATCH /api/v1/cross-platform-notification/admin/providers/{providerId}/enable` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ENABLE_EXTERNAL_PROVIDER`。`DRAFT`、`DISABLED` 和 `DEGRADED` 可流转为 `ENABLED`。启用前必须校验 endpoint 摘要安全、凭据引用只为摘要、receiver policy 合法、allowed source modules 非空、allowed risk levels 非空、能力摘要存在。重复启用保持幂等。

`PATCH /api/v1/cross-platform-notification/admin/providers/{providerId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 或 `DEGRADED` 可流转为 `DISABLED`。禁用后不再被路由策略或新投递请求使用，历史投递不删除。重复禁用保持幂等。

`PATCH /api/v1/cross-platform-notification/admin/providers/{providerId}/archive` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ARCHIVE_EXTERNAL_PROVIDER`。只有 `DRAFT`、`DISABLED` 或 `DEGRADED` 可归档；`ENABLED` provider 必须先禁用后归档；存在启用模板映射、启用路由策略、未终态投递时返回 `49960`。`ARCHIVED` 为终态。

### 渠道能力接口

`GET /api/v1/cross-platform-notification/admin/capabilities` 支持 `page`、`pageSize`、`providerId`、`channel`、`supportsMarkdown`、`supportsRichBlocks`、`supportsDeliveryCallback`、`keyword` 和 `sort`。`sort` 允许 `updatedAt_desc`、`channel_asc`、`maxBodyLength_desc`。成功响应分页 `items` 为 `ExternalChannelCapability[]`。能力由 provider 初始化、测试适配器或后续真实适配器维护，第一版不提供浏览器写接口。

`GET /api/v1/cross-platform-notification/admin/capabilities/{capabilityId}` 返回能力详情、provider 摘要和最近降级原因。能力不存在返回 `49956`。响应不得返回外部平台私有能力 payload。

### 模板映射接口

`GET /api/v1/cross-platform-notification/admin/template-mappings` 支持 `page`、`pageSize`、`keyword`、`sourceModule`、`providerId`、`channel`、`status`、`renderMode` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`sourceModule_asc`、`version_desc`。成功响应分页 `items` 为 `ExternalTemplateMapping[]`。

`GET /api/v1/cross-platform-notification/admin/template-mappings/{mappingId}` 返回模板映射详情、provider 摘要、来源模板摘要、最近投递摘要和最近审计摘要。模板映射不存在返回 `49951`。

`POST /api/v1/cross-platform-notification/admin/template-mappings` 请求字段包括 `sourceModule`、`sourceTemplateRef`、`providerId`、`externalTemplateKey`、`allowedVariables`、`renderMode`、`fallbackTitleTemplate`、`fallbackBodyTemplate`、`reason` 和 `idempotencyKey`。创建默认状态为 `DRAFT`，成功响应 HTTP `201`。provider 不存在返回 `49950`。provider 与 channel 不匹配返回 `49966`。模板字段引用未列入 `allowedVariables` 的变量返回 `49965`。同一来源模板、provider 和 render mode 的未归档映射冲突返回 `49961`。

`PATCH /api/v1/cross-platform-notification/admin/template-mappings/{mappingId}` 可修改创建接口中的业务字段，`reason` 必填。修改成功后 `version` 加一，已有投递的模板快照不受影响。`ARCHIVED` 映射不可修改。审计失败返回 `55801` 且状态不变。

`PATCH /api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/enable` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT` 或 `DISABLED` 可启用为 `ENABLED`。启用前必须校验 provider 为 `ENABLED`、变量定义和模板内容一致、channel 能力支持 render mode。重复启用保持幂等。

`PATCH /api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 可禁用为 `DISABLED`。禁用后新投递不再使用该映射。重复禁用保持幂等。

`PATCH /api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。只有 `DRAFT` 或 `DISABLED` 可归档；`ENABLED` 映射必须先禁用后归档；仍被启用路由策略引用时返回 `49960`。`ARCHIVED` 为终态。

### 路由策略接口

`GET /api/v1/cross-platform-notification/admin/routes` 支持 `page`、`pageSize`、`keyword`、`sourceModule`、`eventType`、`riskLevel`、`providerId`、`templateMappingId`、`status`、`receiverType` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`、`riskLevel_desc`。成功响应分页 `items` 为 `ExternalRoutePolicy[]`。

`GET /api/v1/cross-platform-notification/admin/routes/{routeId}` 返回路由策略详情、provider 摘要、模板映射摘要、最近投递摘要、最近测试摘要和最近审计摘要。路由不存在返回 `49952`。

`POST /api/v1/cross-platform-notification/admin/routes` 请求字段包括 `displayName`、`sourceModule`、`eventType`、`riskLevel`、`matchers`、`providerId`、`templateMappingId`、`receiverSummary`、`groupingPolicy`、`retryPolicySummary`、`reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `CONFIGURE_EXTERNAL_ROUTE`。创建默认状态为 `DRAFT`，成功响应 HTTP `201`。provider 不存在返回 `49950`，模板映射不存在返回 `49951`，receiver 不合法返回 `49964`，receiver 或 endpoint 不安全返回 `49963`，路由冲突返回 `49961`。

`PATCH /api/v1/cross-platform-notification/admin/routes/{routeId}` 可修改创建接口中的业务字段，`reason` 必填。修改 provider、模板映射、receiver、matchers、riskLevel、retry policy 或 grouping policy 时必须携带 `confirmText=UPDATE_EXTERNAL_ROUTE`。`ARCHIVED` 路由不可修改。审计失败返回 `55801` 且状态不变。

`PATCH /api/v1/cross-platform-notification/admin/routes/{routeId}/enable` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ENABLE_EXTERNAL_ROUTE`。`DRAFT` 或 `DISABLED` 可启用为 `ENABLED`。启用前必须校验 provider 为 `ENABLED`、模板映射为 `ENABLED`、receiver 安全、retry policy 合法、risk level 被 provider 允许。重复启用保持幂等。

`PATCH /api/v1/cross-platform-notification/admin/routes/{routeId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 可禁用为 `DISABLED`。禁用不会删除历史投递。重复禁用保持幂等。

`PATCH /api/v1/cross-platform-notification/admin/routes/{routeId}/archive` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ARCHIVE_EXTERNAL_ROUTE`。只有 `DRAFT` 或 `DISABLED` 可归档；`ENABLED` 路由必须先禁用后归档；存在未终态投递时返回 `49960`。

`POST /api/v1/cross-platform-notification/admin/routes/{routeId}/test` 请求字段包括 `samplePayloadSummary`、`sampleReceiverSummary`、`dryRun`、`reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `TEST_EXTERNAL_ROUTE`。成功响应 `ExternalDeliveryRequest` 和最近 `ExternalDeliveryAttempt` 摘要。第一版只生成模拟投递，不发送真实外部消息。provider 降级或测试控制头模拟失败时生成 `SIMULATED_FAILED` 或返回依赖错误，同一实现版本必须固定并写入测试。

### 投递接口

`POST /api/v1/cross-platform-notification/admin/deliveries` 请求字段包括 `sourceModule`、`sourceId`、`eventType`、`riskLevel`、`routeId`、`providerId`、`templateMappingId`、`receiverSummary`、`payloadSummary`、`expiresAt`、`reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `CREATE_EXTERNAL_DELIVERY`。成功响应 HTTP `201`，`data` 为 `ExternalDeliveryRequest`。第一版必须创建模拟 attempt，结果只能为 `SIMULATED_SENT`、`SIMULATED_FAILED`、`BLOCKED` 或 `RETRY_SCHEDULED`，不得返回真实 `SENT`。路由启用时优先使用路由的 provider、模板映射、receiver 和 retry policy；显式 provider 或模板与路由冲突返回 `49961`。未传入 `routeId` 时，投递必须使用请求中的 `sourceModule`、`sourceId`、`eventType`、`riskLevel`、`receiverSummary` 和 `expiresAt` 生成投递快照，不能降级为固定 `custom`、`manual.external` 或 `MEDIUM`。未传入 `routeId` 时必须校验 provider 已启用、模板映射已启用、provider 允许该 `sourceModule` 和 `riskLevel`、receiver 类型在 provider 的 `receiverPolicy.allowedReceiverTypes` 内，且 payload 字段只包含模板映射允许变量；不满足时返回 `40001`、`49960`、`49964`、`49965` 或 `49966`。`sourceModule=alerting` 的请求必须额外校验 `eventType=alert.firing`、`sourceId` 为告警 ID 摘要、`payloadSummary` 不含完整日志或原始告警正文，并把返回 attempt 摘要交给 alerting 保存为 `externalAttemptStatus`。

`GET /api/v1/cross-platform-notification/admin/deliveries` 支持 `page`、`pageSize`、`sourceModule`、`sourceId`、`eventType`、`riskLevel`、`routeId`、`providerId`、`channel`、`status`、`receiverType`、`from`、`to`、`keyword` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`lastAttemptAt_desc`、`riskLevel_desc`、`status_asc`。成功响应分页 `items` 为 `ExternalDeliveryRequest[]`。

`GET /api/v1/cross-platform-notification/admin/deliveries/{deliveryId}` 返回投递详情、attempt 摘要、route 摘要、provider 摘要、receiver 摘要、依赖摘要和审计摘要。投递不存在返回 `49953`。

`PATCH /api/v1/cross-platform-notification/admin/deliveries/{deliveryId}/retry` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `RETRY_EXTERNAL_DELIVERY`。只有 `SIMULATED_FAILED`、`RETRY_SCHEDULED` 和可重试的 `BLOCKED` 可重试；`SIMULATED_SENT`、`CANCELED`、`EXPIRED` 不可重试。超过 retry window 返回 `49968`。重试仍只生成模拟 attempt。重复同幂等键返回同一结果。

`PATCH /api/v1/cross-platform-notification/admin/deliveries/{deliveryId}/cancel` 请求字段为 `reason` 和 `idempotencyKey`。只有 `QUEUED`、`RETRY_SCHEDULED` 和未过期的 `BLOCKED` 可取消为 `CANCELED`。取消 `HIGH` 或 `CRITICAL` 投递必须校验 `HIGH_RISK_APPROVE` 或 `OWNER`，缺少时返回 `42002`。审计风险等级必须使用被取消投递自身的 `riskLevel`，不能固定为 `MEDIUM`。`SIMULATED_SENT`、`SIMULATED_FAILED`、`EXPIRED` 和已 `CANCELED` 为终态或按固定幂等语义返回。取消不删除 attempt。

### 投递尝试和 receiver 接口

`GET /api/v1/cross-platform-notification/admin/attempts` 支持 `page`、`pageSize`、`deliveryId`、`providerId`、`channel`、`status`、`from`、`to` 和 `sort`。`sort` 允许 `startedAt_desc`、`finishedAt_desc`、`attemptNo_asc`、`status_asc`。成功响应分页 `items` 为 `ExternalDeliveryAttempt[]`。

`GET /api/v1/cross-platform-notification/admin/attempts/{attemptId}` 返回 attempt 详情、投递摘要、provider 摘要和脱敏 request/response 摘要。attempt 不存在返回 `49954`。响应不得返回完整外部请求、完整响应、headers、token、secret、完整正文或内部 URL。

`GET /api/v1/cross-platform-notification/admin/receivers` 支持 `page`、`pageSize`、`providerId`、`channel`、`receiverType`、`sourceModule`、`verified`、`degraded`、`keyword` 和 `sort`。`sort` 允许 `lastUsedAt_desc`、`displayName_asc`、`channel_asc`。成功响应分页 `items` 为 `ExternalReceiverSummary[]`。receiver 摘要由 route、delivery 或来源模块快照派生，第一版不提供浏览器直接创建真实 receiver 的接口。

`GET /api/v1/cross-platform-notification/admin/receivers/{receiverId}` 返回 receiver 详情、最近投递摘要和降级原因。receiver 不存在返回 `49955`。响应必须脱敏邮箱、手机号、外部账号、设备 token、webhook URL 和游戏目标。

### 审计接口

`GET /api/v1/cross-platform-notification/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`providerId`、`mappingId`、`routeId`、`deliveryId`、`attemptId`、`receiverId`、`sourceModule`、`result`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`、`riskLevel_desc`。只有 `ADMIN` 和 `OWNER` 可访问。审计列表只读，不提供删除、修改或恢复接口。

后台写操作必须记录调用者、调用者角色、调用者能力点摘要、来源 IP、`reason`、操作前状态、操作后状态、请求编号、结果和失败原因。审计响应字段必须至少包含公共契约要求的 `id`、`requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`sourceIp`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason` 和 `createdAt`，并可补充本模块的 provider、route、delivery、attempt 和 receiver 摘要 ID。审计写入失败时，provider、模板映射、路由策略、投递请求、重试、取消和测试路由不得假装成功，必须返回 `55801` 并保持业务状态不变。投递模拟失败可以保存失败 attempt 和失败审计，但不得返回真实发送成功。

### 状态、幂等和并发

provider 状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DEGRADED` 或 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`DEGRADED` 可恢复为 `ENABLED`、禁用或归档；`ARCHIVED` 为终态。

模板映射状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

路由策略状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

投递请求状态流转为 `QUEUED` 到 `SIMULATED_SENT`、`SIMULATED_FAILED`、`RETRY_SCHEDULED`、`BLOCKED`、`CANCELED` 或 `EXPIRED`；`SIMULATED_FAILED` 和 `RETRY_SCHEDULED` 可重试；`QUEUED`、`RETRY_SCHEDULED` 和未过期 `BLOCKED` 可取消；`SIMULATED_SENT`、`CANCELED`、`EXPIRED` 为终态。第一版不得出现真实 `SENT`、`DELIVERED` 或 `READ`。

写接口使用幂等键时，必须使用字段名排序后的稳定 JSON 语义计算请求体指纹，嵌套对象按字段名递归排序，数组保留顺序，不能依赖 Java `Map.toString()` 或浏览器字段顺序。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一响应快照；相同幂等键搭配不同请求体返回 `49962`。幂等键查找、状态校验、业务写入、审计写入、响应快照保存和幂等记录写入必须处于同一临界区内。

并发创建相同 provider、模板映射、路由策略、delivery 或 receiver 摘要时只能一个成功，其余返回冲突或相同幂等结果。后续数据库实现必须迁移为事务、唯一约束、条件更新或等效机制，不能降低并发口径。

所有支持 `from` 和 `to` 的列表接口必须按该资源主时间字段过滤时间范围。provider 使用 `updatedAt`，投递使用 `createdAt`，attempt 使用 `startedAt`，审计使用 `createdAt`。只传 `from` 时返回主时间大于等于 `from` 的记录，只传 `to` 时返回主时间小于等于 `to` 的记录，同时传入且 `from` 晚于 `to` 时返回 `40001`。`from` 或 `to` 不是 ISO 8601 时间字符串时返回 `40001`。

### 安全、降级和脱敏

任何请求体和响应都不得包含外部平台 token、webhook secret、完整 webhook URL、Discord token、Slack webhook URL、Telegram bot token、QQ token、Oopz token、企业微信 key、SMTP 密码、短信 token、推送 server key、设备 token、RCON 密码、完整 Authorization 请求头、完整请求 headers、完整通知正文、完整 raw payload、内部 URL、内部路径、节点地址、服务器密码、shell 命令、异常栈、数据库连接串、`.env`、`authorized_keys`、`id_rsa` 或前序服务私有数据。检查必须递归覆盖嵌套对象和数组。

endpoint 和 receiver URL 必须拒绝 `file:`、`data:`、`javascript:`、带用户名密码 URL、localhost、回环 IP、内网 IP、链路本地地址、未解析 host、通配符 `*`、空 host、控制字符和非法 URI。站内受控路径必须以 `/` 开头，不能以 `//` 开头，不能包含反斜杠或控制字符。

receiver 摘要必须按类型脱敏。邮箱最多显示域名和首尾字符，手机号最多显示国家码和后四位，外部用户 ID 只显示短 hash，webhook endpoint 只显示 provider 和域名摘要，设备 token 不得返回原值，游戏目标不得返回 RCON、命令或服务器内部地址。

外部依赖不可用时，读取类接口可以返回已有快照并标记 `degraded=true`、`stale=true` 和 `degradeReasons`。写入类接口不得假装成功。provider 降级、速率限制或模拟发送失败必须写入投递失败摘要，不能返回真实外部投递成功。真实发送被第一版阻断时返回 `49967` 或创建 `BLOCKED` 投递摘要，同一实现版本必须固定并写入测试。

第一版不得提供真实删除 provider、模板映射、路由策略、投递、attempt、receiver 或审计的接口。确需清理历史记录时，必须在后续独立契约中增加归档或保留策略接口，并重新完成文档、测试红灯、实现和回归闭环。

### 验收口径

`cross-platform-notification` API 文档必须按 `docs/contracts-cross-platform-notification.md` 独立存在，并由 `.local-docs/tests-cross-platform-notification.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、高风险确认缺失、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求、敏感字段脱敏、测试控制头默认关闭和模块验收口径。

`cross-platform-notification` 完成时必须满足以下条件：当前运行入口为 `ops-core-service:8133`，历史端口 `8123` 只作为 `legacyPort` 返回；健康检查公开且不泄露敏感信息；后台接口按角色和能力点限制；provider、渠道能力、模板映射、路由策略、投递请求、投递尝试、receiver 摘要、审计、幂等、状态流转、依赖降级、审计失败回滚、敏感字段脱敏、测试控制头默认关闭和自检摘要都有自动化验证；自动化测试必须先红灯；实现后在 `ops-core-service` 中全量测试通过；当前后端运行入口回归通过；边界扫描无违规命中；不修改前序服务稳定接口；不直接读取前序服务数据库；不导入前序服务 Java package；不调用真实 `external-node-executor`；不执行真实外部通知发送；不保存真实外部 token、完整 webhook、SMTP 密码、短信 token、机器人 token、设备 token、RCON 密码或完整请求头；不把站内通知主数据、告警规则、插件事件、社区工单、活动、日历、白名单、考勤、资源下载、运维任务、节点文件管理或终端能力塞进本服务；不得恢复 `backend/cross-platform-notification-service` 独立 Maven 入口。

## 北冥官网 ops-image-market API 契约

来源：`docs/contracts-ops-image-market.md`

版本：0.1

### 文档定位

本文档是 `ops-image-market` 微服务的正式 API 契约。`ops-image-market` 负责运维镜像市场控制面，包括 registry provider 摘要、镜像目录、镜像版本、兼容性配置、部署模板摘要、风险扫描摘要、拉取计划、节点镜像缓存快照、依赖健康摘要、幂等记录、审计日志和自检摘要。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、运维能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `ops-image-market` 的职责边界、数据归属、前序服务兼容、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考 Docker Hub、OCI Image Specification、Harbor、GitHub Container Registry、Portainer App Templates、Kubernetes ImagePolicyWebhook、Trivy 和 Renovate 的公开设计。Docker Hub 和 GitHub Container Registry 的 repository、tag、package 权限和访问控制适合本服务拆分 provider、repository 和可见范围。OCI Image Specification 的 manifest、image index、platform 和 digest 适合本服务保存脱敏镜像版本摘要。Harbor 的项目、机器人账号、扫描、复制策略和信任模型适合本服务建立 provider、凭据引用、扫描摘要和高风险启用规则。Portainer App Templates 的 image、env、ports、volumes 和 stack 模板思路适合本服务建立部署模板摘要。Kubernetes ImagePolicyWebhook 的准入判断适合本服务在拉取计划前做风险和兼容性阻断。Trivy 的漏洞严重级别、修复状态和扫描时间适合本服务定义风险扫描摘要。Renovate 的 Docker 版本更新和 digest pinning 思路适合本服务记录版本建议、digest 摘要和漂移风险。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Docker Hub repositories](https://docs.docker.com/docker-hub/repos/) | repository、tag、namespace 和访问权限需要和镜像版本、provider 摘要分离。 |
| [OCI Image Specification](https://github.com/opencontainers/image-spec) | manifest、image index、platform、digest 和 layer 信息只能保存摘要，不能回显完整 payload。 |
| [Harbor documentation](https://goharbor.io/docs/) | 项目、机器人账号、漏洞扫描、复制和信任策略需要纳入 provider、凭据引用、风险摘要和审计。 |
| [GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry) | container package 权限和令牌边界说明 registry 凭据不能由前端或本服务明文托管。 |
| [Portainer app templates](https://docs.portainer.io/advanced/app-templates) | 镜像、环境变量、端口和卷可被模板化，但模板不能直接创建真实容器。 |
| [Kubernetes ImagePolicyWebhook](https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/#imagepolicywebhook) | 镜像准入应在执行前判断签名、来源、风险和策略，不依赖前端确认。 |
| [Trivy vulnerability scanning](https://trivy.dev/latest/docs/scanner/vulnerability/) | 扫描摘要需要记录 severity、fixed version、fix availability、扫描状态和过期时间。 |
| [Renovate Docker manager](https://docs.renovatebot.com/docker/) | tag 更新、digest pinning 和版本建议需要作为镜像版本摘要，不自动替换生产运行态。 |

本文档只吸收这些生态的设计思路，不接入它们的 SDK，不保存真实 registry token，不调用真实 Docker、containerd、registry、scanner、`ops-control` 任务或 `external-node-executor`，不执行镜像拉取、镜像删除、容器创建、签名验签、镜像层扫描或节点缓存写入。

### 职责边界

`ops-image-market` 负责运维镜像市场的安全控制面。它保存可信镜像来源摘要、镜像仓库目录、版本和 digest 摘要、平台和架构摘要、兼容性配置、模板摘要、风险扫描摘要、拉取计划、节点缓存只读快照、版本建议、依赖健康摘要、幂等记录和审计日志。

`ops-image-market` 不负责注册、登录、角色能力点主数据、节点注册、节点心跳、容器生命周期、虚拟机生命周期、Minecraft 实例启停、文件上传下载、终端命令、备份恢复、真实镜像拉取、真实镜像删除、真实漏洞扫描、真实签名验签、真实 registry 凭据托管、玩家资源下载、Cloudreve 文件同步、插件安装卸载、外部通知发送或告警规则管理。

第一版固定为安全模拟和计划控制面。拉取计划只能进入 `DRAFT`、`RISK_REVIEW_REQUIRED`、`APPROVED`、`SIMULATED_READY`、`EXECUTION_BLOCKED`、`CANCELED`、`FAILED` 或 `SUCCEEDED_SIMULATED`。响应不得出现真实 `PULLED`、`PUSHED`、`RUNNING_ON_NODE` 或真实节点执行成功语义。后续接入真实 registry、scanner、signature verifier、`ops-control` 任务或 `external-node-executor` 回写，必须重新补充正式契约、测试文档、红灯测试、实现和前序回归。

### 数据归属

`ops-image-market` 拥有以下主数据：ImageRegistryProvider、OpsImage、OpsImageVersion、ImageCompatibilityProfile、ImageTemplate、ImageRiskScanSummary、ImagePullPlan、NodeImageCacheSnapshot、ImageMarketAuditLog、OpsImageMarketSummary 和幂等记录。

`ops-image-market` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和状态快照；可以保存来自 `ops-control` 的节点、资产、容器运行时和 Minecraft 实例只读摘要；可以保存来自 `external-node-executor` 经过 `ops-control` 或测试适配器回写的节点镜像缓存摘要；可以保存来自 `alerting` 可消费的风险事件摘要；可以保存来自 `cross-platform-notification` 的通知意图或投递结果摘要；可以保存来自 `plugin-integration` 的插件运行环境需求摘要。所有跨服务字段只能是安全快照，不得成为来源模块主数据，不得用于绕过来源模块权限，不得反向修改来源模块状态。

`ops-image-market` 不能直接读取其他服务数据库，不能导入前序服务 Java package，不能复用前序服务内存 store，不能修改 `ops-control` 节点、资产、任务或审批，不能直连 `external-node-executor`，不能创建 `alerting` 告警实例，不能发送 `cross-platform-notification` 外部消息，不能安装插件或写入 Minecraft 配置。

### 基础路径、端口和认证

所有接口默认使用 `/api/v1/ops-image-market` 前缀。第四批合并后当前运行入口为 `ops-core-service:8133`。历史原服务端口 `8124` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或测试入口。

健康检查 `GET /api/v1/ops-image-market/health` 不要求认证，只能返回 `service`、`version`、`status` 和 `requestId`，不得返回 provider 数量、registry 地址、镜像 digest、扫描结果、节点摘要、依赖错误详情或任何敏感字段。

后台接口统一使用 `/api/v1/ops-image-market/admin` 前缀，全部要求 `Authorization: Bearer <token>`。后台读取接口要求基础角色为 `HELPER`、`ADMIN` 或 `OWNER`，并具备 `NODE_READ`。后台写接口要求基础角色为 `ADMIN` 或 `OWNER`，并具备 `NODE_WRITE`。涉及启用外部 provider、修改 endpoint 摘要、修改凭据引用摘要、允许未签名镜像、允许 `HIGH` 或 `CRITICAL` 风险镜像、批准高风险拉取计划、创建跨节点拉取计划、取消高风险计划或解除策略阻断时，必须具备 `HIGH_RISK_APPROVE` 或基础角色为 `OWNER`，并要求固定 `confirmText`。`CRITICAL` 风险只允许 `OWNER` 处理；`ADMIN` 即使具备 `HIGH_RISK_APPROVE`，创建或批准 `CRITICAL` 风险计划也必须返回 `42004`。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`beforeState`、`afterState`、`auditResult`、`createdBy`、`updatedBy`、`enabledBy`、`disabledBy`、`archivedBy`、`approvedBy`、`planStatus`、`scanStatus`、`cacheStatus`、`opsControlTaskRef`、`nodeRequestId`、`registryToken`、`registryPassword`、`dockerPassword`、`imageSecret`、`pullSecret`、`rawToken`、`credential`、`secretKey`、`Authorization`、`requestHeaders`、`manifestPayload`、`layerUrl`、`internalUrl`、`internalPath`、`resolvedPath`、`fullException`、`databaseUrl` 等服务端可信字段。可信字段必须递归检查，嵌套在 `metadata`、`endpointSummary`、`credentialRefSummary`、`manifestSummary`、`templateSpec`、`envSchemaSummary`、`paramsSummary` 或任意数组对象中也必须拒绝。出现可信字段时返回 `40001`。

### 本地测试控制头

`ops-image-market` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Ops-Control-Mode`、`X-Test-External-Node-Executor-Mode`、`X-Test-Registry-Mode`、`X-Test-Scanner-Mode`、`X-Test-Alerting-Mode`、`X-Test-Notification-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store`、`X-Test-Fail-Plan` 和 `X-Test-Now` 模拟认证失败、前序依赖不可用、registry 降级、scanner 失败、审计失败、状态写入失败、计划写入失败和时间边界。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发认证失败、依赖失败、registry 失败、scanner 失败、审计失败、存储失败、计划失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 纳入生产化硬化项。

### 前序服务兼容契约

`auth` 是后台接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。auth 不可用返回 `47200`，auth 超时返回 `47201`，字段或枚举不兼容返回 `47202`。

`ops-control` 是节点、资产、容器运行时和后续任务控制面的主数据来源。`ops-image-market` 可以读取节点架构、运行时、实例用途和已有缓存的只读摘要，不能直接修改节点、资产、任务、审批或审计。ops-control 不可用返回 `47210`，超时返回 `47211`，schema 不兼容返回 `47212`。读取类接口可以返回已有脱敏快照并标记 `degraded=true`；创建拉取计划时如果缺少必要节点或运行时摘要，不得伪造兼容成功。

`external-node-executor` 是节点执行边界。第一版 `ops-image-market` 不得直接调用 `external-node-executor`。节点缓存快照只能来自受控测试桩或后续经过正式接口的安全摘要。external-node-executor 摘要不可用返回 `47220`，超时返回 `47221`，schema 不兼容返回 `47222`。任何需要实时节点执行的动作必须返回 `49717` 或进入 `EXECUTION_BLOCKED`，不能假装真实拉取成功。

`alerting` 可以后续消费 registry 失联、高危镜像、扫描过期、digest 漂移和拉取失败摘要。第一版只保存告警来源摘要，不直接创建告警规则、告警实例、静默或通知策略。alerting 不可用返回 `47230`，超时返回 `47231`，schema 不兼容返回 `47232`。

`cross-platform-notification` 可以后续承担高风险计划和镜像风险的外部通知投递。第一版只保存通知意图或结果摘要，不保存 webhook、短信、邮件、机器人或推送凭据。notification 适配不可用返回 `47240`，超时返回 `47241`，schema 不兼容返回 `47242`。

`plugin-integration` 可以提供插件运行环境需求或推荐镜像摘要。`ops-image-market` 不能安装插件、写插件配置、执行 Minecraft 命令或修改插件主数据。plugin-integration 不可用返回 `47250`，超时返回 `47251`，schema 不兼容返回 `47252`。

`resource` 与 `cloudreve-sync` 不参与运维镜像市场主流程。玩家资源下载、整合包、材质包、地图文件和 Cloudreve 分享链接仍归它们维护，不能把容器镜像伪装成玩家下载资源。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `RegistryType` | `DOCKER_HUB`、`OCI_REGISTRY`、`HARBOR`、`GHCR`、`PRIVATE_REGISTRY`、`MIRROR`、`SIMULATED` | 镜像来源类型。第一版只保存摘要和模拟健康。 |
| `ProviderStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`DEGRADED`、`ARCHIVED` | provider 控制面状态。 |
| `ProviderHealthStatus` | `HEALTHY`、`DEGRADED`、`UNAVAILABLE`、`UNKNOWN` | provider 健康摘要。 |
| `ImagePurpose` | `MINECRAFT_SERVER`、`PROXY`、`DATABASE`、`CACHE`、`UTILITY`、`PLUGIN_RUNTIME`、`OPS_TOOLING`、`CUSTOM` | 镜像用途。 |
| `ImageVisibility` | `OPS_ONLY`、`ADMIN_ONLY`、`TEMPLATE_ONLY` | 可见范围。第一版不面向普通玩家。 |
| `ImageStatus` | `DRAFT`、`PUBLISHED`、`DEPRECATED`、`BLOCKED`、`ARCHIVED` | 镜像目录状态。 |
| `ImageVersionStatus` | `DISCOVERED`、`APPROVED`、`DEPRECATED`、`BLOCKED`、`ARCHIVED` | 镜像版本状态。 |
| `RuntimeType` | `DOCKER`、`CONTAINERD`、`LXC`、`PODMAN`、`SIMULATED` | 运行时摘要。 |
| `Architecture` | `AMD64`、`ARM64`、`ARMV7`、`MULTI_ARCH`、`UNKNOWN` | 平台架构摘要。 |
| `TemplateStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`ARCHIVED` | 镜像模板状态。 |
| `ScanStatus` | `NOT_SCANNED`、`SCANNING`、`PASSED`、`WARNINGS`、`FAILED`、`EXPIRED`、`BLOCKED`、`UNAVAILABLE` | 风险扫描状态。 |
| `Severity` | `UNKNOWN`、`LOW`、`MEDIUM`、`HIGH`、`CRITICAL` | 漏洞和风险等级。 |
| `SignatureStatus` | `SIGNED`、`UNSIGNED`、`INVALID`、`UNKNOWN`、`NOT_REQUIRED` | 签名摘要。第一版不做真实验签。 |
| `PullPlanStatus` | `DRAFT`、`RISK_REVIEW_REQUIRED`、`APPROVED`、`SIMULATED_READY`、`EXECUTION_BLOCKED`、`CANCELED`、`FAILED`、`SUCCEEDED_SIMULATED` | 拉取计划状态。第一版不得出现真实拉取成功。 |
| `DependencyStatus` | `AVAILABLE`、`UNAVAILABLE`、`TIMEOUT`、`BAD_SCHEMA`、`STALE`、`SKIPPED` | 依赖摘要状态。 |
| `AuditResult` | `SUCCESS`、`FAILED` | 审计结果。 |

`sourceModule` 使用模块英文名，例如 `ops-control`、`external-node-executor`、`alerting`、`cross-platform-notification`、`plugin-integration` 和 `custom`。浏览器传入 `auth`、`resource`、`cloudreve-sync`、`internal`、`system` 或未列入本契约的来源模块时必须返回 `40001`，不得创建 provider、镜像、模板、扫描、拉取计划或审计记录。

### 通用对象

#### ImageRegistryProvider

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `providerId` | string | 是 | provider ID。 |
| `displayName` | string | 是 | 展示名称，2 到 80 位。 |
| `registryType` | string | 是 | `RegistryType`。 |
| `status` | string | 是 | `ProviderStatus`。 |
| `healthStatus` | string | 是 | `ProviderHealthStatus`。 |
| `endpointSummary` | object | 是 | endpoint 脱敏摘要，只保存协议、host 摘要和路径类型，不返回完整 URL。 |
| `credentialRefSummary` | object 或 null | 是 | 凭据引用摘要，只能保存别名、托管方式和轮换摘要，不返回真实凭据。 |
| `allowedNamespaces` | string[] | 是 | 允许仓库命名空间，最多 50 个。 |
| `allowedSourceModules` | string[] | 是 | 允许来源模块，最多 20 个。 |
| `allowedRiskLevels` | string[] | 是 | 允许风险等级，取公共风险等级。 |
| `syncPolicySummary` | object | 是 | 同步策略摘要，包括同步模式、窗口和最近同步时间。 |
| `rateLimitSummary` | object | 是 | 速率限制摘要，包括窗口、容量和降级原因。 |
| `lastHealthCheckedAt` | string 或 null | 是 | 最近健康刷新时间。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReasons` | string[] | 是 | 降级原因，必须脱敏。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### OpsImage

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `imageId` | string | 是 | 镜像目录 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `repository` | string | 是 | 仓库名，例如 `library/redis`。不得包含 registry 凭据。 |
| `displayName` | string | 是 | 展示名称，2 到 100 位。 |
| `purpose` | string | 是 | `ImagePurpose`。 |
| `visibility` | string | 是 | `ImageVisibility`。 |
| `status` | string | 是 | `ImageStatus`。 |
| `maintainerSummary` | object | 是 | 维护者摘要，不返回邮箱、token 或内部账号。 |
| `sourceRef` | object 或 null | 是 | 来源引用摘要。 |
| `architectureSet` | string[] | 是 | 支持架构集合。 |
| `runtimeHints` | string[] | 是 | 运行时提示摘要。 |
| `latestVersionSummary` | object 或 null | 是 | 最新安全版本摘要。 |
| `riskSummary` | object | 是 | 风险摘要。 |
| `usageSummary` | object | 是 | 关联模板和计划数量摘要。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### OpsImageVersion

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `imageVersionId` | string | 是 | 镜像版本 ID。 |
| `imageId` | string | 是 | 镜像目录 ID。 |
| `tag` | string | 是 | tag 摘要。不得包含凭据或完整 registry URL。 |
| `digestSummary` | object | 是 | digest 摘要，只返回算法、短 hash 和 pinning 状态。 |
| `manifestSummary` | object | 是 | manifest 摘要，只返回 mediaType、platform 数量和 layer 数量，不返回完整 manifest。 |
| `status` | string | 是 | `ImageVersionStatus`。 |
| `os` | string | 是 | 目标 OS 摘要。 |
| `architecture` | string | 是 | `Architecture`。 |
| `sizeSummary` | object | 是 | 大小摘要。 |
| `publishedAt` | string 或 null | 是 | 上游发布时间。 |
| `deprecatedAt` | string 或 null | 是 | 废弃时间。 |
| `signed` | boolean | 是 | 是否有签名摘要。 |
| `signatureSummary` | object | 是 | 签名摘要，不返回证书私有内容。 |
| `scanSummary` | object | 是 | 最近扫描摘要。 |
| `compatibilitySummary` | object | 是 | 兼容性摘要。 |
| `changeSummary` | object | 是 | 版本变更摘要。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### ImageCompatibilityProfile

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `profileId` | string | 是 | 兼容性配置 ID。 |
| `imageId` | string | 是 | 镜像目录 ID。 |
| `runtime` | string | 是 | `RuntimeType`。 |
| `architecture` | string | 是 | `Architecture`。 |
| `minecraftMode` | string 或 null | 是 | `VANILLA`、`FABRIC`、`FORGE`、`PAPER`、`PROXY`、`NONE` 或 `CUSTOM`。 |
| `minimumCpuCores` | number | 是 | 最小 CPU 核心数。 |
| `minimumMemoryMb` | integer | 是 | 最小内存 MB。 |
| `requiredPortsSummary` | object[] | 是 | 端口摘要，不绑定真实宿主端口。 |
| `requiredVolumesSummary` | object[] | 是 | 卷摘要，不返回宿主绝对路径。 |
| `envSchemaSummary` | object | 是 | 环境变量 schema 摘要，不返回 secret 值。 |
| `nodeSelectorSummary` | object | 是 | 节点选择摘要，只保存标签和架构要求。 |
| `status` | string | 是 | `DRAFT`、`ENABLED`、`DISABLED` 或 `ARCHIVED`。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### ImageTemplate

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `templateId` | string | 是 | 模板 ID。 |
| `imageId` | string | 是 | 镜像目录 ID。 |
| `imageVersionId` | string 或 null | 是 | 固定版本 ID。为空时只能作为草案摘要。 |
| `displayName` | string | 是 | 模板名称。 |
| `status` | string | 是 | `TemplateStatus`。 |
| `templateKind` | string | 是 | `CONTAINER`、`MINECRAFT_INSTANCE`、`UTILITY_JOB` 或 `CUSTOM`。 |
| `runtime` | string | 是 | `RuntimeType`。 |
| `portMappingsSummary` | object[] | 是 | 端口映射摘要。 |
| `volumeMountsSummary` | object[] | 是 | 卷挂载摘要，不返回宿主绝对路径。 |
| `envSchemaSummary` | object | 是 | 环境变量摘要，secret 字段只返回键名和是否必填。 |
| `resourceLimitsSummary` | object | 是 | CPU、内存和磁盘摘要。 |
| `compatibilityProfileId` | string | 是 | 兼容性配置 ID。 |
| `riskSummary` | object | 是 | 模板风险摘要。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### ImageRiskScanSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `scanId` | string | 是 | 扫描摘要 ID。 |
| `imageVersionId` | string | 是 | 镜像版本 ID。 |
| `scanner` | string | 是 | scanner 摘要，例如 `TRIVY_SIMULATED`。 |
| `status` | string | 是 | `ScanStatus`。 |
| `severityCounts` | object | 是 | `UNKNOWN`、`LOW`、`MEDIUM`、`HIGH`、`CRITICAL` 数量。 |
| `highestSeverity` | string | 是 | `Severity`。 |
| `fixAvailable` | boolean | 是 | 是否存在可修复版本摘要。 |
| `cveSummary` | object[] | 是 | CVE 摘要，最多返回 20 条，不返回完整扫描 payload。 |
| `licenseSummary` | object | 是 | 许可证摘要。 |
| `signatureStatus` | string | 是 | `SignatureStatus`。 |
| `startedAt` | string 或 null | 是 | 扫描开始时间。 |
| `finishedAt` | string 或 null | 是 | 扫描结束时间。 |
| `expiresAt` | string | 是 | 扫描过期时间。 |
| `degradedReasons` | string[] | 是 | 降级原因，必须脱敏。 |

#### ImagePullPlan

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `planId` | string | 是 | 拉取计划 ID。 |
| `imageVersionId` | string | 是 | 镜像版本 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `templateId` | string 或 null | 是 | 模板 ID。 |
| `targetNodeIds` | string[] | 是 | 目标节点 ID 摘要，最多 20 个。 |
| `runtime` | string | 是 | `RuntimeType`。 |
| `riskLevel` | string | 是 | 公共风险等级。 |
| `status` | string | 是 | `PullPlanStatus`。 |
| `approvalStatus` | string | 是 | `NOT_REQUIRED`、`REQUIRED`、`APPROVED`、`REJECTED` 或 `EXPIRED`。 |
| `compatibilityResult` | object | 是 | 兼容性结果摘要。 |
| `scanResultSummary` | object | 是 | 扫描结果摘要。 |
| `policyDecisionSummary` | object | 是 | 策略准入摘要。 |
| `opsControlTaskRef` | object 或 null | 是 | 后续 ops-control 任务引用摘要。第一版必须为 null。 |
| `simulated` | boolean | 是 | 第一版必须为 true。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `approvedBy` | string 或 null | 是 | 批准者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `finishedAt` | string 或 null | 是 | 终态时间。 |

#### NodeImageCacheSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `snapshotId` | string | 是 | 快照 ID。 |
| `nodeId` | string | 是 | 节点 ID 摘要。 |
| `runtime` | string | 是 | `RuntimeType`。 |
| `imageVersionId` | string 或 null | 是 | 镜像版本 ID。 |
| `repositorySummary` | string | 是 | 仓库摘要，不返回完整内部地址。 |
| `tag` | string | 是 | tag 摘要。 |
| `digestSummary` | object | 是 | digest 摘要。 |
| `sizeSummary` | object | 是 | 大小摘要。 |
| `lastSeenAt` | string | 是 | 最近观察时间。 |
| `source` | string | 是 | `OPS_CONTROL_SNAPSHOT`、`EXTERNAL_NODE_EXECUTOR_SUMMARY`、`TEST_STUB` 或 `SIMULATED`。 |
| `stale` | boolean | 是 | 是否过期。 |
| `degradedReasons` | string[] | 是 | 降级原因。 |

#### ImageMarketAuditLog

审计字段继承公共契约，允许补充 `providerId`、`imageId`、`imageVersionId`、`profileId`、`templateId`、`scanId`、`planId`、`snapshotId`、`stateFrom`、`stateTo`、`dependencyStatus` 和 `idempotencyKey`。审计列表不得提供删除、修改或恢复接口。审计响应不得返回 registry token、完整 endpoint、完整 digest 清单、完整 manifest、layer URL、宿主路径、节点凭据、请求头、异常栈或前序服务私有数据。

#### OpsImageMarketSummary

自检摘要至少包含 `service`、`port`、`storageMode`、`authMode`、`opsControlAdapterMode`、`externalNodeExecutorAdapterMode`、`registryAdapterMode`、`scannerAdapterMode`、`alertingAdapterMode`、`notificationAdapterMode`、`testControlsEnabled`、`providersTotal`、`enabledProvidersTotal`、`imagesTotal`、`versionsTotal`、`templatesTotal`、`pullPlansTotal`、`simulatedReadyPlansTotal`、`blockedPlansTotal`、`cacheSnapshotsTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastScanAt`、`lastPlanAt`、`degradedReasons` 和 `productionGaps`。

### 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `47200` | 502 | auth 认证上下文不可用。 |
| `47201` | 504 | auth 认证上下文调用超时。 |
| `47202` | 502 | auth 认证上下文不兼容。 |
| `47210` | 502 | ops-control 摘要不可用。 |
| `47211` | 504 | ops-control 摘要调用超时。 |
| `47212` | 502 | ops-control 摘要字段不兼容。 |
| `47220` | 502 | external-node-executor 安全摘要不可用。 |
| `47221` | 504 | external-node-executor 安全摘要调用超时。 |
| `47222` | 502 | external-node-executor 安全摘要字段不兼容。 |
| `47230` | 502 | alerting 适配不可用。 |
| `47231` | 504 | alerting 适配调用超时。 |
| `47232` | 502 | alerting 适配字段不兼容。 |
| `47240` | 502 | cross-platform-notification 适配不可用。 |
| `47241` | 504 | cross-platform-notification 适配调用超时。 |
| `47242` | 502 | cross-platform-notification 适配字段不兼容。 |
| `47250` | 502 | plugin-integration 摘要不可用。 |
| `47251` | 504 | plugin-integration 摘要调用超时。 |
| `47252` | 502 | plugin-integration 摘要字段不兼容。 |
| `49700` | 404 | provider 不存在或当前用户不可见。 |
| `49701` | 404 | 镜像目录不存在或当前用户不可见。 |
| `49702` | 404 | 镜像版本不存在或当前用户不可见。 |
| `49703` | 404 | 兼容性配置不存在。 |
| `49704` | 404 | 镜像模板不存在。 |
| `49705` | 404 | 风险扫描摘要不存在。 |
| `49706` | 404 | 拉取计划不存在。 |
| `49707` | 404 | 节点缓存快照不存在。 |
| `49710` | 409 | 状态不允许当前操作。 |
| `49711` | 409 | provider、镜像、模板或计划业务冲突。 |
| `49712` | 409 | 幂等键请求指纹冲突。 |
| `49713` | 400 | endpoint、repository、tag、URL 或路径摘要不安全。 |
| `49714` | 409 | 风险策略阻断。 |
| `49715` | 409 | 扫描过期、失败或不可用。 |
| `49716` | 409 | 节点、运行时或模板兼容性失败。 |
| `49717` | 409 | 第一版真实执行被阻断。 |
| `49718` | 409 | 签名策略不满足。 |
| `49719` | 409 | registry 健康或速率限制阻断。 |
| `55900` | 500 | ops-image-market 内部错误。 |
| `55901` | 500 | ops-image-market 审计写入失败。 |
| `55902` | 500 | ops-image-market 状态写入失败。 |
| `55903` | 500 | ops-image-market 计划写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、能力点不足、高风险确认缺失、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。幂等键冲突使用 `49712`。状态冲突使用 `49710`。真实执行被阻断使用 `49717`。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/ops-image-market/health` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/ops-image-market/admin/ops/summary` | 是 | `NODE_READ` | LOW |
| provider 列表 | GET | `/api/v1/ops-image-market/admin/providers` | 是 | `NODE_READ` | LOW |
| provider 详情 | GET | `/api/v1/ops-image-market/admin/providers/{providerId}` | 是 | `NODE_READ` | LOW |
| 创建 provider | POST | `/api/v1/ops-image-market/admin/providers` | 是 | `NODE_WRITE`、`HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 更新 provider | PATCH | `/api/v1/ops-image-market/admin/providers/{providerId}` | 是 | `NODE_WRITE`，高风险字段需 `HIGH_RISK_APPROVE` 或 `OWNER` | MEDIUM 到 HIGH |
| 启用 provider | PATCH | `/api/v1/ops-image-market/admin/providers/{providerId}/enable` | 是 | `NODE_WRITE`、`HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 禁用 provider | PATCH | `/api/v1/ops-image-market/admin/providers/{providerId}/disable` | 是 | `NODE_WRITE` | MEDIUM |
| 归档 provider | PATCH | `/api/v1/ops-image-market/admin/providers/{providerId}/archive` | 是 | `NODE_WRITE`、`HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 刷新 provider 健康 | POST | `/api/v1/ops-image-market/admin/providers/{providerId}/health-refresh` | 是 | `NODE_WRITE` | MEDIUM |
| 镜像列表 | GET | `/api/v1/ops-image-market/admin/images` | 是 | `NODE_READ` | LOW |
| 镜像详情 | GET | `/api/v1/ops-image-market/admin/images/{imageId}` | 是 | `NODE_READ` | LOW |
| 创建镜像 | POST | `/api/v1/ops-image-market/admin/images` | 是 | `NODE_WRITE` | MEDIUM |
| 更新镜像 | PATCH | `/api/v1/ops-image-market/admin/images/{imageId}` | 是 | `NODE_WRITE` | MEDIUM |
| 发布镜像 | PATCH | `/api/v1/ops-image-market/admin/images/{imageId}/publish` | 是 | `NODE_WRITE` | MEDIUM |
| 阻断镜像 | PATCH | `/api/v1/ops-image-market/admin/images/{imageId}/block` | 是 | `NODE_WRITE`、高风险需 `HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 归档镜像 | PATCH | `/api/v1/ops-image-market/admin/images/{imageId}/archive` | 是 | `NODE_WRITE` | MEDIUM |
| 版本列表 | GET | `/api/v1/ops-image-market/admin/images/{imageId}/versions` | 是 | `NODE_READ` | LOW |
| 版本详情 | GET | `/api/v1/ops-image-market/admin/versions/{imageVersionId}` | 是 | `NODE_READ` | LOW |
| 创建版本 | POST | `/api/v1/ops-image-market/admin/images/{imageId}/versions` | 是 | `NODE_WRITE` | MEDIUM |
| 批准版本 | PATCH | `/api/v1/ops-image-market/admin/versions/{imageVersionId}/approve` | 是 | `NODE_WRITE`，高风险需 `HIGH_RISK_APPROVE` 或 `OWNER` | MEDIUM 到 CRITICAL |
| 废弃版本 | PATCH | `/api/v1/ops-image-market/admin/versions/{imageVersionId}/deprecate` | 是 | `NODE_WRITE` | MEDIUM |
| 阻断版本 | PATCH | `/api/v1/ops-image-market/admin/versions/{imageVersionId}/block` | 是 | `NODE_WRITE` | HIGH |
| 归档版本 | PATCH | `/api/v1/ops-image-market/admin/versions/{imageVersionId}/archive` | 是 | `NODE_WRITE` | MEDIUM |
| 兼容配置列表 | GET | `/api/v1/ops-image-market/admin/compatibility-profiles` | 是 | `NODE_READ` | LOW |
| 兼容配置详情 | GET | `/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}` | 是 | `NODE_READ` | LOW |
| 创建兼容配置 | POST | `/api/v1/ops-image-market/admin/compatibility-profiles` | 是 | `NODE_WRITE` | MEDIUM |
| 更新兼容配置 | PATCH | `/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}` | 是 | `NODE_WRITE` | MEDIUM |
| 启用兼容配置 | PATCH | `/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/enable` | 是 | `NODE_WRITE` | MEDIUM |
| 禁用兼容配置 | PATCH | `/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/disable` | 是 | `NODE_WRITE` | MEDIUM |
| 归档兼容配置 | PATCH | `/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/archive` | 是 | `NODE_WRITE` | MEDIUM |
| 镜像模板列表 | GET | `/api/v1/ops-image-market/admin/templates` | 是 | `NODE_READ` | LOW |
| 镜像模板详情 | GET | `/api/v1/ops-image-market/admin/templates/{templateId}` | 是 | `NODE_READ` | LOW |
| 创建镜像模板 | POST | `/api/v1/ops-image-market/admin/templates` | 是 | `NODE_WRITE` | MEDIUM |
| 更新镜像模板 | PATCH | `/api/v1/ops-image-market/admin/templates/{templateId}` | 是 | `NODE_WRITE` | MEDIUM |
| 启用镜像模板 | PATCH | `/api/v1/ops-image-market/admin/templates/{templateId}/enable` | 是 | `NODE_WRITE` | MEDIUM |
| 禁用镜像模板 | PATCH | `/api/v1/ops-image-market/admin/templates/{templateId}/disable` | 是 | `NODE_WRITE` | MEDIUM |
| 归档镜像模板 | PATCH | `/api/v1/ops-image-market/admin/templates/{templateId}/archive` | 是 | `NODE_WRITE` | MEDIUM |
| 风险扫描列表 | GET | `/api/v1/ops-image-market/admin/scans` | 是 | `NODE_READ` | LOW |
| 风险扫描详情 | GET | `/api/v1/ops-image-market/admin/scans/{scanId}` | 是 | `NODE_READ` | LOW |
| 创建扫描摘要 | POST | `/api/v1/ops-image-market/admin/versions/{imageVersionId}/scans` | 是 | `NODE_WRITE` | MEDIUM |
| 拉取计划列表 | GET | `/api/v1/ops-image-market/admin/pull-plans` | 是 | `NODE_READ` | LOW |
| 拉取计划详情 | GET | `/api/v1/ops-image-market/admin/pull-plans/{planId}` | 是 | `NODE_READ` | LOW |
| 创建拉取计划 | POST | `/api/v1/ops-image-market/admin/pull-plans` | 是 | `NODE_WRITE`，高风险需 `HIGH_RISK_APPROVE` 或 `OWNER` | MEDIUM 到 CRITICAL |
| 批准拉取计划 | PATCH | `/api/v1/ops-image-market/admin/pull-plans/{planId}/approve` | 是 | `NODE_WRITE`、`HIGH_RISK_APPROVE` 或 `OWNER` | HIGH 到 CRITICAL |
| 取消拉取计划 | PATCH | `/api/v1/ops-image-market/admin/pull-plans/{planId}/cancel` | 是 | `NODE_WRITE`，高风险需 `HIGH_RISK_APPROVE` 或 `OWNER` | MEDIUM 到 HIGH |
| 节点缓存列表 | GET | `/api/v1/ops-image-market/admin/cache-snapshots` | 是 | `NODE_READ` | LOW |
| 节点缓存详情 | GET | `/api/v1/ops-image-market/admin/cache-snapshots/{snapshotId}` | 是 | `NODE_READ` | LOW |
| 审计列表 | GET | `/api/v1/ops-image-market/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

### provider 接口

`GET /api/v1/ops-image-market/admin/providers` 支持 `page`、`pageSize`、`keyword`、`registryType`、`status`、`healthStatus`、`namespace`、`riskLevel`、`sourceModule`、`degraded`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`、`lastHealthCheckedAt_desc`。成功响应分页 `items` 为 `ImageRegistryProvider[]`。

`GET /api/v1/ops-image-market/admin/providers/{providerId}` 返回 provider 详情、最近健康刷新摘要、镜像数量摘要、最近扫描摘要、依赖摘要和最近审计摘要。provider 不存在返回 `49700`。响应不得返回完整 endpoint、真实 registry 地址、凭据、token 或完整错误详情。

`POST /api/v1/ops-image-market/admin/providers` 请求字段包括 `displayName`、`registryType`、`endpointSummary`、`credentialRefSummary`、`allowedNamespaces`、`allowedSourceModules`、`allowedRiskLevels`、`syncPolicySummary`、`rateLimitSummary`、`reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `REGISTER_IMAGE_PROVIDER`。创建默认状态为 `DRAFT`，成功响应 HTTP `201`，`data` 为 `ImageRegistryProvider`。endpoint 或 namespace 不安全返回 `49713`。同一未归档 provider 下规范化 endpoint、registryType 和 displayName 冲突返回 `49711`。真实 token、secret、password 或完整 URL 出现在任意层级返回 `40001`。

`PATCH /api/v1/ops-image-market/admin/providers/{providerId}` 可修改创建接口中的业务字段，`reason` 必填。修改 `endpointSummary`、`credentialRefSummary`、`allowedNamespaces` 或 `allowedRiskLevels` 时必须携带 `confirmText=UPDATE_IMAGE_PROVIDER`，并校验高风险能力。`ARCHIVED` provider 不允许修改，返回 `49710`。审计失败返回 `55901` 且状态不变。

`PATCH /api/v1/ops-image-market/admin/providers/{providerId}/enable` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ENABLE_IMAGE_PROVIDER`。`DRAFT`、`DISABLED` 和 `DEGRADED` 可流转为 `ENABLED`。启用前必须校验 endpoint 安全、凭据引用只为摘要、allowed namespaces 非空、allowed source modules 非空、allowed risk levels 非空和健康摘要可用。重复启用保持幂等。

`PATCH /api/v1/ops-image-market/admin/providers/{providerId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 或 `DEGRADED` 可流转为 `DISABLED`。禁用后新镜像发现、新版本批准和新拉取计划不得使用该 provider。重复禁用保持幂等。

`PATCH /api/v1/ops-image-market/admin/providers/{providerId}/archive` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ARCHIVE_IMAGE_PROVIDER`。只有 `DRAFT`、`DISABLED` 或 `DEGRADED` 可归档；`ENABLED` provider 必须先禁用后归档；存在 `PUBLISHED` 镜像、启用模板或非终态拉取计划时返回 `49710`。`ARCHIVED` 为终态。

`POST /api/v1/ops-image-market/admin/providers/{providerId}/health-refresh` 请求字段为 `reason` 和 `idempotencyKey`。第一版只刷新模拟健康摘要，不连真实 registry。测试配置下 `X-Test-Registry-Mode=unavailable` 返回 `47210` 或把 provider 标记为 `DEGRADED`，同一实现版本必须固定并写入测试。

### 镜像和版本接口

`GET /api/v1/ops-image-market/admin/images` 支持 `page`、`pageSize`、`keyword`、`providerId`、`repository`、`purpose`、`visibility`、`status`、`architecture`、`runtime`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`、`riskLevel_desc`。成功响应分页 `items` 为 `OpsImage[]`。

`GET /api/v1/ops-image-market/admin/images/{imageId}` 返回镜像详情、provider 摘要、最新版本摘要、兼容性摘要、模板摘要、拉取计划摘要、缓存摘要和最近审计摘要。镜像不存在返回 `49701`。响应不得返回完整 registry URL、完整 digest 清单、完整 manifest 或 layer URL。

`POST /api/v1/ops-image-market/admin/images` 请求字段包括 `providerId`、`repository`、`displayName`、`purpose`、`visibility`、`maintainerSummary`、`sourceRef`、`architectureSet`、`runtimeHints`、`reason` 和 `idempotencyKey`。provider 不存在返回 `49700`。provider 未启用时允许创建草稿镜像，但不得发布或创建拉取计划。repository 必须匹配 provider 的 allowed namespaces，且不得包含 registry 凭据、协议、用户密码、localhost、内网地址、控制字符、反斜杠或路径穿越。冲突返回 `49711`。

`PATCH /api/v1/ops-image-market/admin/images/{imageId}` 可修改展示名、用途、可见范围、维护者摘要、sourceRef、architectureSet 和 runtimeHints，`reason` 必填。`ARCHIVED` 镜像不允许修改。审计失败返回 `55901` 且状态不变。

`PATCH /api/v1/ops-image-market/admin/images/{imageId}/publish` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT` 或 `DEPRECATED` 可发布为 `PUBLISHED`。发布前必须存在至少一个 `APPROVED` 且扫描未过期的版本，provider 必须 `ENABLED`。不满足返回 `49710`、`49715` 或 `49719`。

`PATCH /api/v1/ops-image-market/admin/images/{imageId}/block` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `BLOCK_OPS_IMAGE`。`DRAFT`、`PUBLISHED` 或 `DEPRECATED` 可阻断为 `BLOCKED`。阻断后新拉取计划必须返回 `49714`。

`PATCH /api/v1/ops-image-market/admin/images/{imageId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。只有 `DRAFT`、`DEPRECATED` 或 `BLOCKED` 可归档；存在启用模板或非终态计划时返回 `49710`。

`GET /api/v1/ops-image-market/admin/images/{imageId}/versions` 支持 `page`、`pageSize`、`tag`、`status`、`architecture`、`signed`、`highestSeverity`、`scanStatus`、`from`、`to` 和 `sort`。`sort` 允许 `publishedAt_desc`、`createdAt_desc`、`tag_asc`、`highestSeverity_desc`。成功响应分页 `items` 为 `OpsImageVersion[]`。

`GET /api/v1/ops-image-market/admin/versions/{imageVersionId}` 返回版本详情、镜像摘要、provider 摘要、扫描摘要、兼容性摘要、模板引用摘要和最近审计摘要。版本不存在返回 `49702`。

`POST /api/v1/ops-image-market/admin/images/{imageId}/versions` 请求字段包括 `tag`、`digestSummary`、`manifestSummary`、`os`、`architecture`、`sizeSummary`、`publishedAt`、`signed`、`signatureSummary`、`changeSummary`、`reason` 和 `idempotencyKey`。创建默认状态为 `DISCOVERED`。tag、digest 和 manifest 摘要必须脱敏，不允许完整 manifest、layer URL、内部 registry 地址或凭据。相同 imageId 下 tag 或 digest 摘要冲突返回 `49711`。

`PATCH /api/v1/ops-image-market/admin/versions/{imageVersionId}/approve` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。只有 `DISCOVERED` 或 `DEPRECATED` 可批准为 `APPROVED`；`APPROVED`、`BLOCKED` 或 `ARCHIVED` 直接批准返回 `49710`，同一幂等键重放除外。普通版本可省略 confirmText；当扫描最高风险为 `HIGH`、签名为 `UNSIGNED` 或 provider 允许高风险时，`confirmText` 必须为 `APPROVE_IMAGE_VERSION_RISK`。当扫描最高风险为 `CRITICAL` 时，仅 `OWNER` 可带同一确认文本批准。批准前必须存在未过期扫描摘要和启用兼容配置；扫描过期返回 `49715`，兼容性失败返回 `49716`，签名策略失败返回 `49718`。

`PATCH /api/v1/ops-image-market/admin/versions/{imageVersionId}/deprecate` 请求字段为 `reason` 和 `idempotencyKey`。`DISCOVERED` 或 `APPROVED` 可变为 `DEPRECATED`。废弃后不得被新模板或新拉取计划引用。

`PATCH /api/v1/ops-image-market/admin/versions/{imageVersionId}/block` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `BLOCK_IMAGE_VERSION`。`DISCOVERED`、`APPROVED` 或 `DEPRECATED` 可变为 `BLOCKED`；`ARCHIVED` 为终态，返回 `49710`。阻断后新拉取计划必须返回 `49714`。

`PATCH /api/v1/ops-image-market/admin/versions/{imageVersionId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。只有 `DISCOVERED`、`DEPRECATED` 或 `BLOCKED` 可归档；`APPROVED` 版本必须先废弃或阻断；存在启用模板或非终态拉取计划引用时返回 `49710`。`ARCHIVED` 为终态，归档后不得批准、废弃、阻断、更新为模板固定版本或创建拉取计划。

### 兼容性和模板接口

`GET /api/v1/ops-image-market/admin/compatibility-profiles` 支持 `page`、`pageSize`、`imageId`、`runtime`、`architecture`、`minecraftMode`、`status`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`minimumMemoryMb_desc`。成功响应分页 `items` 为 `ImageCompatibilityProfile[]`。

`GET /api/v1/ops-image-market/admin/compatibility-profiles/{profileId}` 返回兼容配置详情、镜像摘要、最近计划使用摘要和最近审计摘要。配置不存在返回 `49703`。

`POST /api/v1/ops-image-market/admin/compatibility-profiles` 请求字段包括 `imageId`、`runtime`、`architecture`、`minecraftMode`、`minimumCpuCores`、`minimumMemoryMb`、`requiredPortsSummary`、`requiredVolumesSummary`、`envSchemaSummary`、`nodeSelectorSummary`、`reason` 和 `idempotencyKey`。路径、卷和环境变量摘要必须脱敏。环境变量 secret 字段只能保存键名、类型、是否必填和来源摘要，不能保存值。冲突返回 `49711`。

`PATCH /api/v1/ops-image-market/admin/compatibility-profiles/{profileId}` 可修改创建接口中的业务字段，`reason` 必填。`ARCHIVED` 配置不可修改。修改后引用该 profile 的启用模板必须标记 `degraded=true` 或要求重新启用。

`PATCH /api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/enable` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT` 或 `DISABLED` 可启用为 `ENABLED`；`ARCHIVED` 返回 `49710`。启用前必须校验镜像未归档、runtime 和 architecture 合法、端口和卷摘要安全、env schema 不含 secret 值。重复启用保持幂等。

`PATCH /api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。只有 `ENABLED` 可禁用为 `DISABLED`。禁用后新模板启用和新拉取计划不得使用该配置；重复禁用保持同一目标状态响应，不重复写审计。

`PATCH /api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。只有 `DRAFT` 或 `DISABLED` 可归档；`ENABLED` 必须先禁用；存在启用模板或非终态拉取计划间接引用时返回 `49710`。`ARCHIVED` 为终态，归档后不可修改、启用、禁用、被新模板引用或被新拉取计划使用。

`GET /api/v1/ops-image-market/admin/templates` 支持 `page`、`pageSize`、`keyword`、`imageId`、`imageVersionId`、`templateKind`、`runtime`、`status`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`。成功响应分页 `items` 为 `ImageTemplate[]`。

`GET /api/v1/ops-image-market/admin/templates/{templateId}` 返回模板详情、镜像摘要、版本摘要、兼容配置摘要、最近计划摘要和最近审计摘要。模板不存在返回 `49704`。

`POST /api/v1/ops-image-market/admin/templates` 请求字段包括 `imageId`、`imageVersionId`、`displayName`、`templateKind`、`runtime`、`portMappingsSummary`、`volumeMountsSummary`、`envSchemaSummary`、`resourceLimitsSummary`、`compatibilityProfileId`、`reason` 和 `idempotencyKey`。创建默认状态为 `DRAFT`。模板不得创建 `ops-control` 任务，不得写节点，不得包含宿主绝对路径、完整命令、secret 值、registry 凭据或内部 URL。冲突返回 `49711`。

`PATCH /api/v1/ops-image-market/admin/templates/{templateId}` 可修改模板业务字段，`reason` 必填。`ARCHIVED` 模板不可修改。修改 imageVersionId 时必须校验版本 `APPROVED`、扫描未过期和兼容性通过。

`PATCH /api/v1/ops-image-market/admin/templates/{templateId}/enable` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT` 或 `DISABLED` 可启用为 `ENABLED`；`ARCHIVED` 返回 `49710`。启用前必须校验 provider `ENABLED`、镜像 `PUBLISHED`、版本 `APPROVED`、扫描未过期、兼容配置启用、模板摘要安全。重复启用保持幂等。

`PATCH /api/v1/ops-image-market/admin/templates/{templateId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 可禁用为 `DISABLED`。禁用后新拉取计划不能使用该模板。

`PATCH /api/v1/ops-image-market/admin/templates/{templateId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。只有 `DRAFT` 或 `DISABLED` 可归档；`ENABLED` 必须先禁用；存在非终态拉取计划引用时返回 `49710`。`ARCHIVED` 为终态，归档后不可修改、启用、禁用或创建新拉取计划。

### 风险扫描接口

`GET /api/v1/ops-image-market/admin/scans` 支持 `page`、`pageSize`、`imageVersionId`、`imageId`、`providerId`、`scanner`、`status`、`highestSeverity`、`fixAvailable`、`signatureStatus`、`from`、`to` 和 `sort`。`sort` 允许 `finishedAt_desc`、`startedAt_desc`、`highestSeverity_desc`、`expiresAt_asc`。成功响应分页 `items` 为 `ImageRiskScanSummary[]`。

`GET /api/v1/ops-image-market/admin/scans/{scanId}` 返回扫描详情、版本摘要、镜像摘要、provider 摘要和降级摘要。扫描不存在返回 `49705`。响应不得返回完整漏洞 payload、完整镜像 manifest、layer URL、registry 凭据、scanner 原始输出或内部路径。

`POST /api/v1/ops-image-market/admin/versions/{imageVersionId}/scans` 请求字段包括 `scanner`、`status`、`severityCounts`、`highestSeverity`、`fixAvailable`、`cveSummary`、`licenseSummary`、`signatureStatus`、`startedAt`、`finishedAt`、`expiresAt`、`degradedReasons`、`reason` 和 `idempotencyKey`。第一版只允许创建安全模拟或测试桩扫描摘要，不启动真实 scanner。`X-Test-Scanner-Mode=failed` 时返回 `47220` 或创建 `UNAVAILABLE` 摘要，同一实现版本必须固定并写入测试。`expiresAt` 早于当前时间时扫描为 `EXPIRED`，不得批准版本或创建拉取计划。

### 拉取计划接口

`GET /api/v1/ops-image-market/admin/pull-plans` 支持 `page`、`pageSize`、`imageVersionId`、`imageId`、`providerId`、`templateId`、`nodeId`、`runtime`、`riskLevel`、`status`、`approvalStatus`、`createdBy`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`riskLevel_desc`、`status_asc`。成功响应分页 `items` 为 `ImagePullPlan[]`。

`GET /api/v1/ops-image-market/admin/pull-plans/{planId}` 返回计划详情、镜像版本摘要、provider 摘要、模板摘要、目标节点摘要、兼容性结果、扫描结果、策略决策、依赖摘要和最近审计摘要。计划不存在返回 `49706`。

`POST /api/v1/ops-image-market/admin/pull-plans` 请求字段包括 `imageVersionId`、`templateId`、`targetNodeIds`、`runtime`、`riskLevel`、`allowUnsigned`、`allowHighSeverity`、`reason`、`confirmText` 和 `idempotencyKey`。成功响应 HTTP `201`。第一版只创建模拟计划，不执行拉取，不创建 `ops-control` 任务。跨节点计划、`allowUnsigned=true`、`allowHighSeverity=true`、`riskLevel=HIGH` 或扫描最高风险为 `HIGH` 时，`confirmText` 必须为 `CREATE_IMAGE_PULL_PLAN_RISK` 并要求高风险权限。`riskLevel=CRITICAL` 或扫描最高风险为 `CRITICAL` 时只有 `OWNER` 可创建。provider 未启用返回 `49719`，镜像未发布或版本未批准返回 `49710`，扫描过期返回 `49715`，兼容性失败返回 `49716`，策略阻断返回 `49714`，真实执行请求返回 `49717`。

创建计划时必须按目标节点摘要校验架构、runtime、最小内存、端口需求、卷需求、模板状态和 provider 风险策略。无法从 `ops-control` 获取目标节点摘要时，不得伪造兼容成功。`targetNodeIds` 不允许为空，最多 20 个。重复幂等键同体返回同一计划，不同体返回 `49712`。

`PATCH /api/v1/ops-image-market/admin/pull-plans/{planId}/approve` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `APPROVE_IMAGE_PULL_PLAN`。只有 `DRAFT` 和 `RISK_REVIEW_REQUIRED` 可批准。批准时必须重新校验 provider 仍为 `ENABLED`、镜像仍为 `PUBLISHED`、版本仍为 `APPROVED` 且未废弃/阻断/归档、模板仍为 `ENABLED`、目标节点仍兼容、扫描仍未过期。批准后低中风险计划进入 `SIMULATED_READY`，高风险计划进入 `APPROVED` 或 `SIMULATED_READY`，但不得进入真实执行状态。扫描过期返回 `49715`，provider 禁用返回 `49719`，镜像、版本或模板状态失效返回 `49710`，节点兼容性变化返回 `49716`。

`PATCH /api/v1/ops-image-market/admin/pull-plans/{planId}/cancel` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT`、`RISK_REVIEW_REQUIRED`、`APPROVED`、`SIMULATED_READY` 和 `EXECUTION_BLOCKED` 可取消为 `CANCELED`。取消 `HIGH` 或 `CRITICAL` 计划必须校验高风险权限；`CRITICAL` 仍只允许 `OWNER`。终态计划重复取消按固定幂等语义返回状态冲突或相同结果，同一实现版本必须写入测试。

### 节点缓存和审计接口

`GET /api/v1/ops-image-market/admin/cache-snapshots` 支持 `page`、`pageSize`、`nodeId`、`runtime`、`imageVersionId`、`repository`、`tag`、`stale`、`source`、`from`、`to` 和 `sort`。`sort` 允许 `lastSeenAt_desc`、`lastSeenAt_asc`、`repository_asc`。成功响应分页 `items` 为 `NodeImageCacheSnapshot[]`。节点缓存快照只读，不提供浏览器写接口。

`GET /api/v1/ops-image-market/admin/cache-snapshots/{snapshotId}` 返回快照详情、节点摘要、版本摘要和降级原因。快照不存在返回 `49707`。响应不得返回节点本地镜像层路径、宿主绝对路径、完整 digest 清单、registry 凭据或内部节点地址。

`GET /api/v1/ops-image-market/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`providerId`、`imageId`、`imageVersionId`、`templateId`、`planId`、`result`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`、`riskLevel_desc`。只有 `ADMIN` 和 `OWNER` 可访问。审计列表是只读接口，不提供删除、修改或恢复。审计中的 `requestId` 必须来自当前 HTTP 请求，不能使用固定占位值。

后台写操作必须记录调用者、调用者角色、调用者能力点摘要、来源 IP、`reason`、操作前状态、操作后状态、请求编号、结果和失败原因。审计写入失败时，provider、镜像、版本、兼容配置、模板、扫描摘要和拉取计划不得假装成功，必须返回 `55901` 并保持业务状态不变。

### 状态、幂等和并发

provider 状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DEGRADED` 或 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`DEGRADED` 可恢复为 `ENABLED`、禁用或归档；`ARCHIVED` 为终态。

所有状态写接口必须先按当前状态和目标状态执行统一流转校验，再做依赖校验、审计写入和状态更新。未列入本节的源状态到目标状态转换必须返回 `49710`，不能靠接口实现自行放宽。

镜像目录状态流转为 `DRAFT` 可到 `PUBLISHED`、`BLOCKED` 或 `ARCHIVED`；`PUBLISHED` 可到 `DEPRECATED` 或 `BLOCKED`；`DEPRECATED` 可到 `PUBLISHED`、`BLOCKED` 或 `ARCHIVED`；`BLOCKED` 可到 `DRAFT` 或 `ARCHIVED`；`ARCHIVED` 为终态。

镜像版本状态流转为 `DISCOVERED` 可到 `APPROVED`、`DEPRECATED`、`BLOCKED` 或 `ARCHIVED`；`APPROVED` 可到 `DEPRECATED` 或 `BLOCKED`；`DEPRECATED` 可到 `APPROVED`、`BLOCKED` 或 `ARCHIVED`；`BLOCKED` 可到 `DISCOVERED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

兼容配置状态流转为 `DRAFT` 可到 `ENABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

模板状态流转为 `DRAFT` 可到 `ENABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

拉取计划状态流转为 `DRAFT` 到 `RISK_REVIEW_REQUIRED`、`SIMULATED_READY`、`EXECUTION_BLOCKED`、`FAILED` 或 `CANCELED`；`RISK_REVIEW_REQUIRED` 到 `APPROVED`、`SIMULATED_READY`、`EXECUTION_BLOCKED`、`FAILED` 或 `CANCELED`；`APPROVED` 到 `SIMULATED_READY`、`EXECUTION_BLOCKED`、`FAILED` 或 `CANCELED`；`SIMULATED_READY` 到 `SUCCEEDED_SIMULATED`、`EXECUTION_BLOCKED`、`FAILED` 或 `CANCELED`。`CANCELED`、`FAILED` 和 `SUCCEEDED_SIMULATED` 为终态。第一版不得出现真实拉取成功状态。

所有写接口使用幂等键时，必须使用字段名排序后的稳定 JSON 语义计算请求体指纹，嵌套对象按字段名递归排序，数组保留顺序，不能依赖 Java `Map.toString()` 或浏览器字段顺序。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一响应快照；相同幂等键搭配不同请求体返回 `49712`。幂等键查找、状态校验、业务写入、审计写入、响应快照保存和幂等记录写入必须处于同一临界区内。

并发创建相同 provider、镜像目录、镜像版本、兼容配置、模板、扫描摘要或拉取计划时只能一个成功，其余返回冲突或相同幂等结果。后续数据库实现必须迁移为事务、唯一约束、条件更新或等效机制，不能降低并发口径。

所有支持 `from` 和 `to` 的列表接口必须按该资源主时间字段过滤时间范围。provider、镜像、版本、兼容配置、模板和计划使用 `updatedAt` 或契约指定主时间；扫描使用 `finishedAt`；缓存快照使用 `lastSeenAt`；审计使用 `createdAt`。只传 `from` 时返回主时间大于等于 `from` 的记录，只传 `to` 时返回主时间小于等于 `to` 的记录，同时传入且 `from` 晚于 `to` 时返回 `40001`。`from` 或 `to` 不是 ISO 8601 时间字符串时返回 `40001`。

### 安全、降级和脱敏

endpoint、repository、tag、namespace 和 URL 摘要必须拒绝 `file:`、`data:`、`javascript:`、带用户名密码 URL、localhost、回环 IP、内网 IP、链路本地地址、未解析 host、通配符 `*`、空 host、控制字符、反斜杠、路径穿越和非法 URI。repository 必须是 registry 内 namespace/repository 摘要，不允许浏览器提交完整 registry 登录串。

任何请求体和响应都不得包含 registry token、registry password、Docker password、image secret、pull secret、完整 Authorization 请求头、完整请求 headers、完整 manifest、完整 layer URL、内部 registry 地址、内部 URL、内部路径、节点本地镜像层路径、宿主绝对路径、真实 shell 命令、异常栈、数据库连接串、`.env`、`authorized_keys`、`id_rsa` 或前序服务私有数据。检查必须递归覆盖嵌套对象和数组。

扫描失败、扫描过期、签名不满足、provider 降级、registry 限流、节点摘要不可用或兼容性失败时，读取类接口可以返回已有快照并标记 `degraded=true`、`stale=true` 和 `degradeReasons`。写入类接口不得假装成功。高风险或严重风险计划必须按权限、确认文本、状态和审计规则阻断或进入 `RISK_REVIEW_REQUIRED`，不能靠前端展示来兜底。

第一版不得提供真实删除 provider、镜像、版本、兼容配置、模板、扫描摘要、拉取计划、缓存快照或审计的接口。确需清理历史记录时，必须在后续独立契约中增加归档或保留策略接口，并重新完成文档、测试红灯、实现和回归闭环。

### 验收口径

`ops-image-market` API 文档必须按 `docs/contracts-ops-image-market.md` 独立存在，并由 `.local-docs/tests-ops-core.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、高风险确认缺失、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求、敏感字段脱敏、测试控制头默认关闭和模块验收口径。

`ops-image-market` 完成时必须满足以下条件：当前运行入口为 `ops-core-service:8133`，历史端口 `8124` 只作为 `legacyPort` 返回；健康检查公开且不泄露敏感信息；后台接口按角色、能力点、风险等级和确认文本限制；provider、镜像目录、镜像版本、兼容配置、模板、风险扫描、拉取计划、节点缓存快照、审计、幂等、状态流转、依赖降级、审计失败回滚、敏感字段脱敏、测试控制头默认关闭和自检摘要都有自动化验证；自动化测试必须先红灯；实现后本模块在 `ops-core-service` 中全量测试通过；当前后端运行入口回归测试通过；边界扫描无违规命中；不修改前序服务稳定接口；不直接读取前序服务数据库；不导入前序服务 Java package；不调用真实 `external-node-executor`；不执行真实 Docker、containerd、registry、scanner、镜像拉取、镜像删除或容器创建；不保存真实 registry token、完整 manifest、layer URL、内部地址、宿主路径、节点凭据、完整请求头或前序服务私有数据；不把玩家资源下载、Cloudreve 文件同步、运维任务执行、节点文件管理、终端能力、告警规则、外部通知发送或插件安装塞进本服务。

## 北冥官网 ops-core API 契约

来源：`docs/contracts-ops-core.md`

版本：0.3

### 文档定位

本文档是 `ops-core-service` 运行合并单元的正式 API 契约。`ops-core` 负责承载第四期后台运维控制面和第六期跨平台通知控制面合并后的运行入口、自检摘要、模块装配摘要、生产就绪诊断、真实 HTTP smoke、继承路由漂移防线、测试控制头总开关和网关切换验收口径。

本文档继承 `docs/contracts-common.md`。七个被承载业务模块的业务接口仍分别以 `docs/contracts-ops-control.md`、`docs/contracts-cloudreve-sync.md`、`docs/contracts-backup-recovery.md`、`docs/contracts-alerting.md`、`docs/contracts-plugin-integration.md`、`docs/contracts-ops-image-market.md` 和 `docs/contracts-cross-platform-notification.md` 为准。本文档不得混写七个模块的业务 API 字段、状态机或错误码。

### 职责边界

`ops-core` 承载 `ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration`、`ops-image-market` 和 `cross-platform-notification` 七个后台运维与通知控制面模块。合并后这些模块继续使用原路径前缀，分别是 `/api/v1/ops-control`、`/api/v1/cloudreve-sync`、`/api/v1/backup-recovery`、`/api/v1/alerting`、`/api/v1/plugin-integration`、`/api/v1/ops-image-market` 和 `/api/v1/cross-platform-notification`。

`ops-core` 不新增七个业务模块的业务语义，不把七套 store、状态机、错误码、审计对象或主数据揉成一个大模块，不直接读取前序服务数据库，不绕过正式 API 适配前序服务，也不执行真实宿主机、容器、节点、Cloudreve、registry、scanner、插件命令或真实外部消息发送。

### 基础路径、端口和认证

`ops-core-service` 本地端口固定为 `8133`。`ops-core` 自有接口使用 `/api/v1/ops-core` 前缀。健康检查公开可访问。后台自检、模块装配、生产就绪诊断和 HTTP smoke 接口要求 `Authorization: Bearer <token>`，本地契约实现允许 `owner-token` 和 `admin-token` 访问，`helper-token` 与 `user-token` 返回 `42001`，缺失 token 返回 `41000`，格式错误返回 `41003`。

通过网关注入的可信身份头也可访问后台自有接口。可信上下文必须同时包含合法 `X-Gateway-Internal-Request-Id`、`X-Gateway-Internal-Timestamp`、`X-Gateway-Internal-Signature`、`X-Beiming-Actor-User-Id` 和角色头。角色必须为 `ADMIN` 或 `OWNER`。缺少必要字段、请求编号非法、角色枚举非法、能力点枚举非法、签名错误、签名时间戳过期或签名明文不匹配时返回 `53233`。

浏览器直连时传入的可信身份头不得覆盖真实身份。只有存在 `X-Gateway-Internal-Request-Id` 且整组可信字段和内部签名通过校验时，才按可信上下文授权；否则按 `Authorization` 本地 token 授权。直连请求一旦携带 `X-Gateway-Internal-Request-Id`，不得在签名失败后回退到本地 token。

### 模块装配表

七个继承模块合计 219 个业务 API 路由。`ops-core` 自有接口为 5 个。`ops-core-service` 当前进程应注册 224 个 `/api/v1/**` 方法路由。

| 模块 | 当前服务目录 | 当前端口 | API 数 | 正式契约 |
| --- | --- | ---: | ---: | --- |
| `ops-control` | `backend/ops-core-service` | 8133 | 31 | `docs/contracts-ops-control.md` |
| `cloudreve-sync` | `backend/ops-core-service` | 8133 | 16 | `docs/contracts-cloudreve-sync.md` |
| `backup-recovery` | `backend/ops-core-service` | 8133 | 25 | `docs/contracts-backup-recovery.md` |
| `alerting` | `backend/ops-core-service` | 8133 | 24 | `docs/contracts-alerting.md` |
| `plugin-integration` | `backend/ops-core-service` | 8133 | 38 | `docs/contracts-plugin-integration.md` |
| `ops-image-market` | `backend/ops-core-service` | 8133 | 49 | `docs/contracts-ops-image-market.md` |
| `cross-platform-notification` | `backend/ops-core-service` | 8133 | 36 | `docs/contracts-cross-platform-notification.md` |

### 生产就绪能力状态

生产就绪诊断必须暴露真实数据库持久化、真实跨服务 HTTP adapter、真实审计持久化、真实节点执行、真实 Cloudreve API、真实 registry、真实 scanner、真实插件事件入口、真实通知投递、真实外部消息发送、真实回调签名、生产凭据托管、异步队列和持久化事务仍为 `BLOCKED`。HTTP smoke 使用 `NOT_RUN`、`PASS` 或 `DEGRADED`。可信网关内部签名使用 `PASS` 或 `PARTIAL`。即使 HTTP smoke 和模拟外部投递通过，`readyForProduction` 仍必须为 `false`。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/ops-core/health` | 否 | 无 | LOW |
| 运行摘要 | GET | `/api/v1/ops-core/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 模块装配摘要 | GET | `/api/v1/ops-core/admin/modules` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 生产就绪诊断 | GET | `/api/v1/ops-core/admin/readiness` | 是 | `ADMIN` 或 `OWNER` | LOW |
| HTTP smoke | POST | `/api/v1/ops-core/admin/http-smoke/run` | 是 | `ADMIN` 或 `OWNER` | LOW |

### 健康检查

`GET /api/v1/ops-core/health`

响应 `data` 必须包含 `service=ops-core`、`status=UP`、`version`、`port=8133`、`modulesTotal=7`、`inheritedRoutesTotal=219`、`selfRoutesTotal=5` 和 `routesTotal=224`。该接口不得返回模块内部 provider 数量、节点 endpoint、外部 URL、token、凭据、异常栈、真实宿主路径或依赖错误细节。

### 运行摘要

`GET /api/v1/ops-core/ops/summary`

响应 `data` 必须包含 `service=ops-core`、`port=8133`、`modulesTotal=7`、`modulesMounted=7`、`inheritedRoutesTotal=219`、`selfRoutesTotal=5`、`routesTotal=224`、`testControlsEnabled`、`storageMode`、`authMode`、`dependencyAdapterMode`、`serviceDiscoveryMode`、`registeredUpstreams`、`httpSmokeStatus`、`lastHttpSmokeAt`、`lastHttpSmokeResults`、`trustedGatewaySignatureStatus`、`routeDriftStatus`、`gatewaySwitchStatus`、`moduleRoutes`、`productionGaps`、`recentAuditSummary` 和 `generatedAt`。

### 模块装配摘要

`GET /api/v1/ops-core/admin/modules`

成功响应 HTTP `200`，`data.items` 为模块装配数组。每个元素必须包含 `moduleKey`、`moduleName`、`pathPrefix`、`legacyServiceDirectory`、`legacyPort`、`legacyServiceRetired`、`currentServiceDirectory`、`currentPort`、`contract`、`localTestDocument`、`legacyTestCommand`、`currentTestCommand`、`routesTotal`、`contractRoutesTotal`、`routeDriftStatus`、`enabled`、`mounted`、`businessContractOwnedByModule`、`compatibilityMode` 和 `productionGaps`。

### 生产就绪诊断

`GET /api/v1/ops-core/admin/readiness`

响应 `data` 必须包含 `service=ops-core`、`port=8133`、`readyForProduction=false`、`readinessStatus=NOT_READY`、`routesTotal=224`、`inheritedRoutesTotal=219`、`selfRoutesTotal=5`、`routeDriftStatus`、`legacyServiceRestoreStatus`、`gatewaySwitchStatus`、`testControlHeadersStatus`、`sensitiveFieldScanStatus`、`serviceDiscoveryMode`、`registeredUpstreams`、`httpSmokeStatus`、`lastHttpSmokeAt`、`lastHttpSmokeResults`、`trustedGatewaySignatureStatus`、`checks`、`moduleReadiness`、`productionBlockers` 和 `generatedAt`。

### HTTP smoke

`POST /api/v1/ops-core/admin/http-smoke/run`

该接口只触发本地真实 HTTP smoke，不创建业务数据，不执行真实节点动作，不发送真实外部消息。smoke 通过 `api-gateway-service` 访问当前 `ops-core-service` 承载的关键路径，目标至少包含 `/api/v1/ops-control/overview`、`/api/v1/alerting/health`、`/api/v1/cross-platform-notification/health` 和 `/api/v1/ops-core/health`。

响应 `data` 必须包含 `status`、`targetsTotal`、`passedTargetsTotal`、`failedTargetsTotal`、`targets`、`startedAt`、`finishedAt` 和 `realHttpSmoke=true`。全部目标通过时 `status=PASS`。任一目标失败时 `status=DEGRADED`，失败摘要必须脱敏，不得返回 token、请求头、异常栈、完整内部地址或真实宿主路径。

## 北冥官网 api-gateway API 契约

来源：`docs/contracts-api-gateway.md`

版本：0.4

### 文档定位

本文档是 `api-gateway` 微服务的正式 API 契约。`api-gateway` 是北冥官网后端统一入口，只负责统一路由、请求编号、认证头透传、认证上下文注入、基础 CORS、上游超时、请求边界保护、响应头白名单透传、错误降级、路由表、自检摘要、运行拓扑、上游健康摘要和网关级请求日志摘要。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、运维能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `api-gateway` 的路径、路由表、字段、错误码、降级、审计和验收口径。

第一版设计参考了成熟网关生态中的稳定做法。Spring Cloud Gateway 使用请求属性谓词匹配路由，并通过过滤器处理跨路由逻辑；Kong Gateway 把相关能力拆成可组合插件，例如 Correlation ID 和 Rate Limiting；Nginx 反向代理强调 `proxy_pass`、请求头和请求体透传；Envoy 将上游服务作为 cluster 处理，并把健康检查结果纳入路由判断。第二版继续参考 AWS API Gateway 对请求体大小上限的明确约束、Cloudflare Rules 对边界策略前置的做法，以及 Nginx 对响应头和上游头的显式控制。`api-gateway` 本轮只吸收这些边界思路，不引入动态服务发现、插件市场、真实 WAF、分布式限流、OAuth/OIDC 或 WebSocket 长连接代理。

参考来源为官方文档：Spring Cloud Gateway Request Predicates 与 Gateway Filters、Kong Gateway Correlation ID 与 Rate Limiting 插件、Nginx `ngx_http_proxy_module`、Envoy upstream health checking、AWS API Gateway quotas、Cloudflare Rules。

### 职责边界

`api-gateway` 负责以下能力：

| 能力 | 说明 |
| --- | --- |
| 路由匹配 | 根据固定路径前缀把请求转发到已有微服务端口。 |
| 请求编号 | 接收或生成 `X-Request-Id`，向上游和下游保持一致。 |
| 认证透传与上下文注入 | 原样透传 `Authorization: Bearer <token>`；携带可验证会话时，通过 `auth` 会话校验生成可信身份头、内部时间戳和内部签名。 |
| 请求透传 | 保持 HTTP 方法、路径、查询参数、JSON 请求体和必要请求头。 |
| 请求边界保护 | 校验 `X-Request-Id` 格式，限制 P0 JSON 请求体大小，拒绝明显异常入口请求。 |
| 响应透传 | 上游响应状态码、统一响应体、内容类型和响应头白名单默认原样返回。 |
| CORS | 对 `/api/v1/**` 提供本地前端允许的预检响应。 |
| 超时与降级 | 上游不可连接返回网关错误，上游超时返回网关超时错误，不伪造业务成功。 |
| 路由表 | 暴露只读路由注册表，便于前端和运维控制台确认入口可用性。 |
| 运行拓扑摘要 | 暴露只读运行入口、路由归属和未来单服务合并准备状态。 |
| 上游健康摘要 | 维护手动刷新后的上游健康快照。 |
| 请求日志摘要 | 记录脱敏后的网关访问摘要，用于本地排障和后台观测。 |

`api-gateway` 不负责注册、登录、角色能力点主数据、成员档案、站内通知、内容发布、资源下载、社区工单、活动报名、日历、更新日志、服务器运维、节点执行、备份恢复、告警规则、插件事件、外部通知或镜像市场业务。它不能直接读取任何服务数据库，不能导入任何前序服务 Java package，不能复制业务状态机，不能把多个业务服务聚合成新的业务结果。

上游服务的业务认证、权限、字段校验、状态流转、幂等、审计和失败降级仍由对应微服务负责。网关发现上游返回 4xx 或 5xx 时默认透传，不把业务失败改成网关成功，也不替上游重写业务错误码。

### 基础路径、端口和认证

`api-gateway` 本地端口固定为 `8125`。

网关自有接口使用 `/api/v1/gateway` 前缀。业务转发接口保持上游原路径，例如 `/api/v1/auth/login` 经网关访问时仍是 `/api/v1/auth/login`，不会改写为 `/api/v1/gateway/auth/login`。

公开健康检查无需认证。网关自有后台接口需要 `Authorization: Bearer <token>`。P0 本地实现允许 `owner-token`、`admin-token` 和 `helper-token` 访问只读后台自检、路由和健康接口；请求日志接口只允许 `owner-token` 和 `admin-token` 访问，`helper-token` 与 `user-token` 均返回 `42001`；缺失或格式错误返回公共认证错误码。

除本地固定 token 外，网关自有后台接口也可以通过 `auth` 的 `GET /api/v1/auth/session/verify` 校验真实会话。校验成功后，`user.roles` 中包含 `HELPER`、`ADMIN` 或 `OWNER` 可访问只读后台接口，包含 `ADMIN` 或 `OWNER` 可访问请求日志接口。校验返回认证或权限错误时，网关返回对应错误；`auth` 不可连接、超时或返回 `5xx` 时，网关返回 `46000` 或 `46001`，不得把无法校验的 token 当作已登录用户。

业务转发接口不在网关层做强制角色判断。公开业务接口可以无 `Authorization` 透传；需要登录或后台权限的业务接口由上游服务按自身契约返回 `41000`、`41001`、`42001` 或其他业务错误码。业务转发请求如果携带 `Authorization: Bearer <token>` 且目标路由不是 `auth`，网关会向 `auth` 会话校验接口做一次短路径校验。校验成功时，网关向上游注入可信身份头；校验失败、超时或 `auth` 不可用时，网关不注入可信身份头，但仍透传原始 `Authorization` 给目标上游，由目标上游按自身契约判定请求是否可继续。

网关注入的可信身份头只允许由网关生成，客户端传入同名头必须在转发前剥离。可信身份签名第一版使用 `api-gateway.internal-signing-secret` 配置的共享密钥和 HMAC SHA-256 小写 hex。签名明文必须包含 HTTP 方法、原始路径、请求编号、用户 ID、角色、能力点、时间戳和规范化后的上下文字段。P0.3 可信身份头如下。

| 请求头 | 来源 | 说明 |
| --- | --- | --- |
| `X-Beiming-Actor-User-Id` | `auth.data.user.id` | 当前用户 ID。 |
| `X-Beiming-Actor-Roles` | `auth.data.user.roles` | 逗号分隔角色。 |
| `X-Beiming-Actor-Permissions` | `auth.data.user.permissions` | 逗号分隔能力点，可为空字符串。 |
| `X-Beiming-Actor-Minecraft-Id` | `auth.data.user.minecraftBinding.minecraftId` | 已绑定时注入。 |
| `X-Beiming-Actor-Minecraft-Uuid` | `auth.data.user.minecraftBinding.minecraftUuid` | 已绑定时注入。 |
| `X-Gateway-Internal-Request-Id` | 网关请求编号 | 标记该可信上下文来自当前网关请求。 |
| `X-Gateway-Internal-Timestamp` | 网关生成时间戳 | ISO 8601 时间，用于上游校验签名窗口。 |
| `X-Gateway-Internal-Signature` | 网关内部签名 | HMAC SHA-256 小写 hex，证明可信身份头来自网关。 |

客户端传入的 `X-Gateway-Internal-Signature`、`X-Gateway-Internal-Timestamp`、`X-Gateway-Internal-Request-Id` 和全部 `X-Beiming-Actor-*` 都必须在转发前剥离。没有通过 auth 校验的业务请求不得注入可信身份头、内部时间戳或内部签名，只透传原始 `Authorization` 给上游自行判定。

### 路由注册表

路由表是只读配置。网关不得在运行时通过接口新增、修改或删除路由。新增业务服务时必须先完成该服务契约和测试，再更新网关契约、测试和路由表。

| 路由 ID | 服务键 | 路径前缀 | 上游端口 | 健康探测路径 |
| --- | --- | --- | --- | --- |
| `auth` | `AUTH` | `/api/v1/auth` | `8130` | `/api/v1/auth/session/verify` |
| `profile` | `PROFILE` | `/api/v1/profile` | `8130` | `/api/v1/profile/members` |
| `notification` | `NOTIFICATION` | `/api/v1/notifications` | `8130` | `/api/v1/notifications/me/unread-count` |
| `content` | `CONTENT` | `/api/v1/content` | `8130` | `/api/v1/content/home` |
| `server-status` | `SERVER_STATUS` | `/api/v1/server-status` | `8130` | `/api/v1/server-status/overview` |
| `resource` | `RESOURCE` | `/api/v1/resources` | `8130` | `/api/v1/resources` |
| `admin` | `ADMIN` | `/api/v1/admin` | `8130` | `/api/v1/admin/overview` |
| `onboarding` | `ONBOARDING` | `/api/v1/onboarding` | `8131` | `/api/v1/onboarding/me/progress` |
| `exam` | `EXAM` | `/api/v1/exams` | `8131` | `/api/v1/exams/me/sessions` |
| `whitelist` | `WHITELIST` | `/api/v1/whitelist` | `8131` | `/api/v1/whitelist/me/applications/current` |
| `attendance` | `ATTENDANCE` | `/api/v1/attendance` | `8131` | `/api/v1/attendance/leaderboard` |
| `community` | `COMMUNITY` | `/api/v1/community` | `8132` | `/api/v1/community/boards` |
| `activity` | `ACTIVITY` | `/api/v1/activity` | `8132` | `/api/v1/activity/events` |
| `calendar` | `CALENDAR` | `/api/v1/calendar` | `8132` | `/api/v1/calendar/upcoming` |
| `changelog` | `CHANGELOG` | `/api/v1/changelog` | `8132` | `/api/v1/changelog/versions/latest` |
| `ops-control` | `OPS_CONTROL` | `/api/v1/ops-control` | `8133` | `/api/v1/ops-control/overview` |
| `cloudreve-sync` | `CLOUDREVE_SYNC` | `/api/v1/cloudreve-sync` | `8133` | `/api/v1/cloudreve-sync/health` |
| `backup-recovery` | `BACKUP_RECOVERY` | `/api/v1/backup-recovery` | `8133` | `/api/v1/backup-recovery/health` |
| `alerting` | `ALERTING` | `/api/v1/alerting` | `8133` | `/api/v1/alerting/health` |
| `online-map` | `ONLINE_MAP` | `/api/v1/online-map` | `8134` | `/api/v1/online-map/health` |
| `plugin-integration` | `PLUGIN_INTEGRATION` | `/api/v1/plugin-integration` | `8133` | `/api/v1/plugin-integration/health` |
| `cross-platform-notification` | `CROSS_PLATFORM_NOTIFICATION` | `/api/v1/cross-platform-notification` | `8133` | `/api/v1/cross-platform-notification/health` |
| `ops-image-market` | `OPS_IMAGE_MARKET` | `/api/v1/ops-image-market` | `8133` | `/api/v1/ops-image-market/health` |
| `material` | `MATERIAL` | `/api/v1/materials` | `8134` | `/api/v1/materials/featured` |
| `guide` | `GUIDE` | `/api/v1/guides` | `8134` | `/api/v1/guides/categories` |

本地真实 HTTP 联调允许通过配置项 `api-gateway.upstreams.ops-core-base-url` 临时覆盖 `ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration`、`ops-image-market` 和 `cross-platform-notification` 七个路由的上游基础地址。该配置只改变这七个路由的 `upstreamBaseUrl` 和由 URL 推导出的 `upstreamPort`，不得新增 `OPS_CORE` 业务路由，不得改写任一业务路径前缀，不得影响 `portal-core`、`business-core`、`admission-core`、`engagement-core` 或其他上游。默认值仍为 `http://127.0.0.1:8133`。

路径匹配规则为最长前缀优先。`/api/v1/resources` 和 `/api/v1/resources/**` 都必须命中 `resource`。未知路径返回网关错误，不转发到任何上游。

第八轮单服务合并准备层只允许在网关公开只读运行拓扑。当前官网后端回滚入口是 `api-gateway-service:8125`、`business-core-service:8130`、`admission-core-service:8131`、`engagement-core-service:8132`、`ops-core-service:8133` 和 `portal-core-service:8134`。未来统一后端目标入口为 `unified-backend-service:8135`。外部节点执行器已出仓且未接入，当前网关不得再公开外部节点执行器业务路由。该准备层不得新增业务路由，不得改写 25 条业务路径，不得动态挂载业务模块，不得恢复已退役旧 Maven 入口，不得把外部节点执行器并入统一后端。当前网关保持 `CURRENT_SIX_ROLLBACK_ENTRYPOINTS` 和 `IN_PROCESS_MOUNT_NOT_IMPLEMENTED=NOT_IMPLEMENTED`，但运行拓扑必须识别 `unified-backend` 试点候选。候选入口第一阶段挂载 `api-gateway` 和 `portal-core`，第二阶段扩展挂载 `business-core`，第三阶段扩展挂载 `admission-core`，第四阶段扩展挂载 `engagement-core`，第五阶段扩展挂载 `ops-core`，候选挂载路由为 `auth`、`profile`、`notification`、`content`、`server-status`、`resource`、`admin`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、`ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration`、`cross-platform-notification`、`ops-image-market`、`guide`、`material` 和 `online-map`。

### 网关自有对象

#### GatewayRoute

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `routeId` | string | 是 | 路由 ID。 |
| `serviceKey` | string | 是 | 服务键，大写枚举。 |
| `serviceName` | string | 是 | 服务展示名。 |
| `pathPrefix` | string | 是 | 匹配路径前缀。 |
| `upstreamBaseUrl` | string | 是 | 上游基础地址，本地默认 `http://127.0.0.1:<port>`。 |
| `upstreamPort` | integer | 是 | 上游端口。 |
| `healthCheckPath` | string | 是 | 健康刷新使用的路径。 |
| `timeoutMs` | integer | 是 | 单次转发超时时间。 |
| `enabled` | boolean | 是 | 是否启用，P0 固定为 `true`。 |
| `methods` | string[] | 是 | 允许转发的方法，P0 为 `GET`、`POST`、`PUT`、`PATCH`、`DELETE` 和 `OPTIONS`。 |
| `authDelegated` | boolean | 是 | 是否把认证权限交给上游处理，业务路由固定为 `true`。 |
| `createdAt` | string | 是 | 路由注册时间。 |
| `updatedAt` | string | 是 | 路由更新时间。 |

#### GatewayUpstreamHealth

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `serviceKey` | string | 是 | 服务键。 |
| `routeId` | string | 是 | 路由 ID。 |
| `pathPrefix` | string | 是 | 路径前缀。 |
| `status` | string | 是 | `UNKNOWN`、`UP`、`DEGRADED`、`DOWN` 或 `TIMEOUT`。 |
| `lastHttpStatus` | integer 或 null | 是 | 最近一次健康请求 HTTP 状态。 |
| `lastErrorCode` | integer 或 null | 是 | 最近一次网关错误码。 |
| `lastErrorMessage` | string 或 null | 是 | 脱敏错误摘要。 |
| `lastCheckedAt` | string 或 null | 是 | 最近检查时间。 |
| `durationMs` | integer 或 null | 是 | 最近检查耗时。 |

健康状态判定：健康刷新请求得到任意小于 `500` 的 HTTP 状态，说明上游可达，状态为 `UP`。得到 `500` 到 `599`，状态为 `DEGRADED`。连接失败为 `DOWN`。超过网关超时时间为 `TIMEOUT`。健康刷新不得把业务 `401`、`403` 或 `404` 当作上游不可用。

#### GatewayRequestLog

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `requestId` | string | 是 | 请求编号。 |
| `method` | string | 是 | HTTP 方法。 |
| `path` | string | 是 | 请求路径，不包含 query。 |
| `routeId` | string 或 null | 是 | 命中的路由 ID。 |
| `serviceKey` | string 或 null | 是 | 命中的服务键。 |
| `upstreamStatus` | integer 或 null | 是 | 上游 HTTP 状态。 |
| `gatewayStatus` | integer | 是 | 下游 HTTP 状态。 |
| `result` | string | 是 | `SUCCESS` 或 `FAILED`。 |
| `errorCode` | integer 或 null | 是 | 网关级错误码或上游响应体中的业务错误码。 |
| `durationMs` | integer | 是 | 网关处理耗时。 |
| `clientIp` | string 或 null | 是 | 客户端 IP 摘要。 |
| `actorUserId` | string 或 null | 是 | 已通过 `auth` 会话校验时记录当前用户 ID，否则为 `null`。 |
| `createdAt` | string | 是 | 记录时间。 |

请求日志不得保存请求体、完整 query、完整 token、Cookie、Authorization 原文、外部 webhook、registry 凭据、节点密钥、文件内容、日志正文或异常堆栈。

#### GatewayRuntimeTopology

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `service` | string | 是 | 固定为 `api-gateway`。 |
| `deploymentMode` | string | 是 | 固定为 `CURRENT_SIX_ROLLBACK_ENTRYPOINTS`。 |
| `singleServiceMergeReadiness` | string | 是 | 固定为 `PREPARING`。 |
| `currentEntrypointsTotal` | integer | 是 | 当前官网后端回滚入口数量，固定为 `6`。 |
| `futureMergeCandidateEntrypointsTotal` | integer | 是 | 未来统一后端候选入口数量，固定为 `6`，不包含外部节点执行器。 |
| `businessRoutesTotal` | integer | 是 | 当前业务转发路由数量，固定为 `25`。 |
| `gatewayApiTotal` | integer | 是 | 当前网关自有 API 数量，固定为 `8`。 |
| `currentEntrypoints` | object[] | 是 | 当前运行入口清单。 |
| `futureUnifiedBackend` | object | 是 | 未来统一后端目标摘要。 |
| `mergePreparationChecks` | object[] | 是 | 合并准备守卫检查。 |
| `generatedAt` | string | 是 | 生成时间。 |

`currentEntrypoints` 每项必须包含 `entrypointKey`、`serviceDirectory`、`port`、`role`、`mergeDisposition`、`rollbackEntrypointRole`、`retirementApprovalStatus`、`hostedRouteIds`、`hostedPathPrefixes`、`routesTotal` 和 `keptExternalReason`。`api-gateway` 的 `mergeDisposition` 为 `ROLLBACK_ENTRYPOINT`，`rollbackEntrypointRole` 为 `PROTECTED_ROLLBACK_ENTRYPOINT`，`retirementApprovalStatus` 为 `BLOCKED`，五个 core 运行单元为 `IN_PROCESS_CANDIDATE`。

### 网关错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `46200` | 404 | 网关没有匹配路由。 |
| `46201` | 405 | 网关路由不支持该方法。 |
| `46202` | 400 | 网关请求格式错误。 |
| `46203` | 400 | 网关分页、排序或筛选参数错误。 |
| `46204` | 413 | 网关请求体超过 P0 限制。 |
| `46205` | 400 | 网关请求编号格式非法。 |
| `46210` | 502 | 上游服务不可连接。 |
| `46211` | 504 | 上游服务调用超时。 |
| `46212` | 502 | 上游返回空响应或非 HTTP 响应。 |
| `46213` | 502 | 上游地址配置无效。 |
| `51250` | 500 | api-gateway 内部错误。 |

业务上游返回的错误码由上游契约决定，网关默认原样透传，不映射成以上错误码。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 网关健康检查 | GET | `/api/v1/gateway/health` | 否 | 无 | LOW |
| 网关自检摘要 | GET | `/api/v1/gateway/admin/ops/summary` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 网关运行拓扑 | GET | `/api/v1/gateway/admin/runtime-topology` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 网关路由列表 | GET | `/api/v1/gateway/admin/routes` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 网关路由详情 | GET | `/api/v1/gateway/admin/routes/{routeId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 上游健康列表 | GET | `/api/v1/gateway/admin/upstreams` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 刷新上游健康 | POST | `/api/v1/gateway/admin/upstreams/{serviceKey}/health-refresh` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 网关请求日志 | GET | `/api/v1/gateway/admin/request-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 业务请求转发 | `GET/POST/PUT/PATCH/DELETE/OPTIONS` | `/api/v1/{module}/**` | 由上游决定 | 由上游决定 | 由上游决定 |

### 网关健康检查

`GET /api/v1/gateway/health`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "api-gateway",
    "status": "UP",
    "port": 8125,
    "routesTotal": 26,
    "generatedAt": "2026-05-29T00:00:00Z"
  },
  "requestId": "req_example"
}
```

业务规则：该接口只表示网关进程可用，不表示全部上游可用。上游可用性通过上游健康列表读取。

### 网关自检摘要

`GET /api/v1/gateway/admin/ops/summary`

成功响应 HTTP `200`。

响应字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `service` | string | 是 | 固定为 `api-gateway`。 |
| `port` | integer | 是 | 固定为 `8125`。 |
| `routesTotal` | integer | 是 | 路由总数，P0 为 `26`。 |
| `enabledRoutesTotal` | integer | 是 | 启用路由总数。 |
| `upstreamsUp` | integer | 是 | 最近健康状态为 `UP` 的上游数量。 |
| `upstreamsDegraded` | integer | 是 | 最近健康状态为 `DEGRADED` 的上游数量。 |
| `upstreamsDown` | integer | 是 | 最近健康状态为 `DOWN` 或 `TIMEOUT` 的上游数量。 |
| `requestLogsRetained` | integer | 是 | 当前保留请求日志数量。 |
| `productionGaps` | string[] | 是 | P0 已知生产化差距摘要。 |
| `generatedAt` | string | 是 | 生成时间。 |

降级规则：自检摘要只读取网关内存状态，不主动调用所有上游，避免一次后台刷新拖垮上游。

### 网关运行拓扑

`GET /api/v1/gateway/admin/runtime-topology`

成功响应 HTTP `200`，`data` 为 `GatewayRuntimeTopology`。

业务规则：该接口只读取网关内存中的静态路由表和固定运行入口画像，不主动探测上游，不启动其它服务，不读取文件系统，不导入业务服务代码，不访问数据库。它用于给后续单服务合并提供可测试边界，不能被前端或运维控制台当作当前已经完成单服务合并的信号。缺失认证返回 `41000`，非 Bearer 返回 `41003`，`USER` 返回 `42001`，`HELPER`、`ADMIN` 和 `OWNER` 可读取。响应不得包含 `Authorization`、Cookie、token、secret、节点密钥、内部签名、完整上游地址以外的运行环境路径、异常栈或本地用户目录。

### 网关路由列表

`GET /api/v1/gateway/admin/routes`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`，最小 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配路由 ID、服务键或路径前缀。 |
| `serviceKey` | string | 否 | 服务键。 |
| `enabled` | boolean | 否 | `true` 或 `false`。 |
| `sort` | string | 否 | 允许 `routeId_asc`、`serviceKey_asc`、`upstreamPort_asc`、`updatedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `GatewayRoute[]`。

字段校验失败返回 `46203`。`serviceKey` 不存在返回 `46203`。该接口不得返回 token、Cookie、真实凭据、上游响应体、请求日志正文或异常栈。

### 网关路由详情

`GET /api/v1/gateway/admin/routes/{routeId}`

成功响应 HTTP `200`，`data` 为 `GatewayRoute`。

路由不存在返回 `43000`。`routeId` 只允许小写字母、数字和短横线，格式错误返回 `40001`。

### 上游健康列表

`GET /api/v1/gateway/admin/upstreams`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `status` | string | 否 | `UNKNOWN`、`UP`、`DEGRADED`、`DOWN` 或 `TIMEOUT`。 |
| `serviceKey` | string | 否 | 服务键。 |
| `sort` | string | 否 | 允许 `serviceKey_asc`、`status_asc`、`lastCheckedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `GatewayUpstreamHealth[]`。

业务规则：该接口只读取最近健康快照，不主动探测上游。尚未刷新过的上游状态为 `UNKNOWN`。

### 刷新上游健康

`POST /api/v1/gateway/admin/upstreams/{serviceKey}/health-refresh`

请求体为空。成功响应 HTTP `200`，`data` 为 `GatewayUpstreamHealth`。

业务规则：网关对目标上游执行一次 `GET healthCheckPath` 请求。健康刷新使用与该路由相同的超时。请求中的 `X-Request-Id` 会被透传给上游。请求中的 `Authorization` 可以透传给上游，但健康状态不得依赖具体业务权限通过。

`serviceKey` 不存在返回 `43000`。连接失败返回 HTTP `200` 且 `data.status` 为 `DOWN`，并记录 `lastErrorCode: 46210`。超时返回 HTTP `200` 且 `data.status` 为 `TIMEOUT`，并记录 `lastErrorCode: 46211`。上游返回空响应或非 HTTP 响应时记录 `lastErrorCode: 46212`。上游地址配置无效时记录 `lastErrorCode: 46213`。健康刷新自身不能因为单个上游失败返回 5xx，除非网关内部状态不可用。

### 网关请求日志

`GET /api/v1/gateway/admin/request-logs`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `routeId` | string | 否 | 路由 ID。 |
| `serviceKey` | string | 否 | 服务键。 |
| `result` | string | 否 | `SUCCESS` 或 `FAILED`。 |
| `from` | string | 否 | ISO 8601 时间。 |
| `to` | string | 否 | ISO 8601 时间。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`、`durationMs_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `GatewayRequestLog[]`。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 返回 `42001`。

业务规则：日志保留数量由实现固定，P0 至少保留最近 `200` 条。时间范围格式错误、`from` 晚于 `to`、排序参数非法或枚举非法返回 `46203`。请求日志不得通过网关 API 删除。

### 业务请求转发

`GET/POST/PUT/PATCH/DELETE/OPTIONS /api/v1/{module}/**`

转发规则：

| 规则 | 要求 |
| --- | --- |
| 路径 | 保持原始 path，不改写模块前缀。 |
| Query | 原样保留 query string。 |
| 方法 | 保持原 HTTP 方法。 |
| 请求体 | 对 `POST`、`PUT` 和 `PATCH` 原样透传 body。 |
| 请求体大小 | P0 JSON 请求体最大 `1048576` 字节，超过返回 HTTP `413` 和错误码 `46204`。 |
| Content-Type | 原样透传。 |
| Accept | 原样透传。 |
| Authorization | 原样透传，不写入日志。 |
| X-Request-Id | 缺失时生成，转发给上游，并回传给客户端。只允许 1 到 128 位的字母、数字、下划线、短横线、点和冒号，非法返回 HTTP `400` 和错误码 `46205`。 |
| X-Forwarded-For | 默认使用当前连接远端地址；后续接入可信反向代理后再启用代理链追加。 |
| Hop-by-hop header | 不透传 `Connection`、`Transfer-Encoding`、`Upgrade`、`Keep-Alive`、`TE`、`Trailer` 和 `Proxy-Authorization`。 |
| 可信身份头 | 浏览器传入的 `X-Beiming-Actor-*`、`X-Gateway-Internal-*` 等可信身份头必须丢弃。 |
| 可信身份注入 | `auth` 会话校验成功时，网关注入 `X-Beiming-Actor-*`、`X-Gateway-Internal-Request-Id`、`X-Gateway-Internal-Timestamp` 和 `X-Gateway-Internal-Signature`；校验失败时不注入。 |
| 响应 | 上游 HTTP 状态、响应体、Content-Type 以及响应头白名单默认原样返回。 |
| 响应头白名单 | 允许透传 `Content-Type`、`Cache-Control`、`ETag`、`Location`、`Content-Disposition`、`Last-Modified` 和 `Expires`；其他响应头默认丢弃，避免泄露内部实现或不安全代理头。 |
| 日志 | 只记录脱敏摘要。 |

上游返回 2xx、4xx、5xx 或非标准 HTTP 状态码时，网关默认透传。上游不可连接返回 HTTP `502` 和错误码 `46210`。上游超时返回 HTTP `504` 和错误码 `46211`。上游返回空响应或非 HTTP 响应返回 HTTP `502` 和错误码 `46212`。上游地址配置无效返回 HTTP `502` 和错误码 `46213`。未知路径返回 HTTP `404` 和错误码 `46200`。不支持的方法返回 HTTP `405` 和错误码 `46201`。请求体超过网关 P0 上限返回 HTTP `413` 和错误码 `46204`。请求编号非法返回 HTTP `400` 和错误码 `46205`。

### CORS 规则

本地开发允许以下来源访问 `/api/v1/**`：

| Origin |
| --- |
| `http://localhost:5173` |
| `http://127.0.0.1:5173` |
| `http://localhost:5174` |
| `http://127.0.0.1:5174` |
| `http://localhost:5182` |
| `http://127.0.0.1:5182` |

允许方法为 `GET`、`POST`、`PUT`、`PATCH`、`DELETE` 和 `OPTIONS`。允许请求头包括 `Authorization`、`Content-Type`、`Accept-Language`、`X-Request-Id` 和前端常用安全请求头。响应必须暴露 `X-Request-Id`。CORS 预检不写业务审计，不转发到业务上游。

### 分页、幂等和状态流转

网关自有列表接口统一使用公共分页格式。`page` 从 `1` 开始，`pageSize` 默认 `20`，最大 `100`。

网关自有接口不创建业务资源，不支持客户端幂等键。业务接口的幂等规则完全由上游服务维护，网关只透传 `idempotencyKey` 所在请求体，不读取、不存储、不改写。

网关自有状态只有上游健康快照状态。状态流转由健康刷新结果决定：`UNKNOWN` 可以转为 `UP`、`DEGRADED`、`DOWN` 或 `TIMEOUT`；后续任一状态都可以在下一次刷新后转为其他健康状态。该状态不改变路由启用状态，也不阻止业务转发。

### 审计和日志要求

网关自有后台接口需要记录请求日志摘要。业务转发请求也需要记录请求日志摘要。P0 不写入独立审计存储，只维护内存摘要和请求编号，后续接入正式审计服务时必须先更新契约。

请求日志必须脱敏。以下内容不得存储或返回：请求体、完整 query、完整 `Authorization`、Cookie、密码、邀请码原始码、密码重置令牌、节点密钥、registry 凭据、Cloudreve token、外部 webhook、文件内容、终端命令正文、日志正文、异常堆栈。

网关不得接受浏览器传入的可信身份头作为真实身份。`X-Beiming-Actor-User-Id`、`X-Beiming-Actor-Roles`、`X-Gateway-Internal-Token`、`X-Gateway-Internal-Timestamp`、`X-Gateway-Internal-Signature` 等头如果来自客户端请求，必须在转发前移除。网关向上游注入可信身份时，只能来自 `auth` 会话校验结果。`auth` 校验失败时不得沿用客户端伪造头，不得根据 token 字符串自行推断用户身份。

### 失败降级

网关自身失败时必须返回统一错误响应，不返回 HTML 错误页。

单个上游不可用只影响该上游对应请求和健康摘要，不影响其他路由。上游不可连接返回 `46210`，上游超时返回 `46211`，上游非 HTTP 响应返回 `46212`，上游地址配置无效返回 `46213`。网关不能把上游失败转成空成功响应，不能改写上游业务错误为成功，不能重试非幂等请求。

`GET` 请求可以在未来加入只读缓存或重试策略，但 P0 不做缓存和自动重试，避免返回旧业务状态或重复触发上游副作用。

### 生产化差距

P0 `api-gateway` 是本地契约实现，必须在自检摘要中明确以下生产化差距：尚未接入真实服务发现，尚未接入集中配置，尚未接入分布式限流，认证上下文已支持通过 `auth` 会话校验注入和内部签名，但尚未接入签名密钥集中托管、密钥轮换和缓存，尚未接入持久化审计，尚未代理 WebSocket 和大文件流，统一后端入口尚未接收真实生产流量，网关仍是受保护回滚入口，动态服务发现尚未接入，外部节点执行器已出仓且未连接。

这些差距不得影响 P0 的路径转发、请求编号、认证透传、可信身份头剥离、可验证认证上下文注入、内部签名注入、错误降级、路由表、运行拓扑和测试闭环。

### 验收口径

`api-gateway` API 文档按 `docs/contracts-api-gateway.md` 独立存在，并由 `.local-docs/tests-api-gateway.md` 记录本地测试闭环。

本文档列出的每个网关自有接口都有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、分页排序、状态刷新、失败降级、日志脱敏和验收口径。运行拓扑测试必须确认当前 6 个官网后端回滚入口、未来 6 个统一后端候选、外部节点执行器出仓且未接入、25 条业务转发路由、8 个网关自有 API、5 个 core 运行单元归属、已退役旧入口未恢复、静态服务发现和 in-process 挂载仍未实现，并确认 `unified-backend-service:8135` 候选画像已把 `business-core`、`admission-core`、`engagement-core` 和 `portal-core` 标为试点挂载对象。业务转发测试必须覆盖 25 个已接入路径前缀，确认路由表端口准确、`api-gateway.upstreams.ops-core-base-url` 只覆盖七个 ops-core 承载路由、请求编号透传、请求编号非法拒绝、认证头透传、可信身份头剥离、客户端伪造签名头剥离、`auth` 会话校验成功后的可信身份和内部签名注入、`auth` 校验失败后的不注入降级、查询参数透传、JSON body 透传、请求体大小限制、响应头白名单、上游 2xx 透传、上游 4xx 透传、上游 5xx 透传、未知路径、非法方法、CORS 预检、上游不可用、上游超时和敏感字段不落日志。

开发完成后必须执行 `mvn -f backend/api-gateway-service/pom.xml test`、`mvn -f backend/business-core-service/pom.xml test`、`mvn -f backend/admission-core-service/pom.xml test`、`mvn -f backend/engagement-core-service/pom.xml test`、`mvn -f backend/ops-core-service/pom.xml test`、`mvn -f backend/portal-core-service/pom.xml test`。第一批到第五批旧服务清理后，不得为了网关回归恢复对应旧服务目录、旧 Maven 入口、旧启动类或旧测试命令。第四批和第五批业务路径必须继续由 `ops-core` 和 `portal-core` 当前入口覆盖测试。旧 `backend/online-map-service` 已退役且不得恢复。测试过程必须写入 `.local-docs/tests-api-gateway.md`。

## 北冥官网 unified-backend API 契约

来源：`docs/contracts-unified-backend.md`

版本：0.4

`unified-backend-service` 是统一后端并行候选入口，端口固定为 `8135`。它用于本地验证 `api-gateway`、`business-core`、`admission-core`、`engagement-core`、`ops-core` 与 `portal-core` 在同一 Spring Boot 进程内装配，不替代当前 `api-gateway-service:8125`、`business-core-service:8130`、`admission-core-service:8131`、`engagement-core-service:8132`、`ops-core-service:8133`、`portal-core-service:8134` 或其它生产入口。

候选入口必须在同一进程内挂载 `/api/v1/gateway/**`、`/api/v1/business-core/**`、`/api/v1/admission-core/**`、`/api/v1/engagement-core/**`、`/api/v1/ops-core/**`、`/api/v1/portal-core/**`、`/api/v1/auth/**`、`/api/v1/profile/**`、`/api/v1/notifications/**`、`/api/v1/content/**`、`/api/v1/server-status/**`、`/api/v1/resources/**`、`/api/v1/admin/**`、`/api/v1/onboarding/**`、`/api/v1/exams/**`、`/api/v1/whitelist/**`、`/api/v1/attendance/**`、`/api/v1/community/**`、`/api/v1/activity/**`、`/api/v1/calendar/**`、`/api/v1/changelog/**`、`/api/v1/ops-control/**`、`/api/v1/cloudreve-sync/**`、`/api/v1/backup-recovery/**`、`/api/v1/alerting/**`、`/api/v1/plugin-integration/**`、`/api/v1/cross-platform-notification/**`、`/api/v1/ops-image-market/**`、`/api/v1/guides/**`、`/api/v1/materials/**` 和 `/api/v1/online-map/**`。25 条业务路由必须标记为 `IN_PROCESS`，外部节点执行器不得作为挂载项返回。原路径、认证、响应格式、错误码和测试口径不得改变。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 候选健康检查 | GET | `/api/v1/unified-backend/health` | 否 | 无 | LOW |
| 候选摘要 | GET | `/api/v1/unified-backend/admin/ops/summary` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 候选挂载清单 | GET | `/api/v1/unified-backend/admin/mounts` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 候选 readiness | GET | `/api/v1/unified-backend/admin/readiness` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 候选 HTTP smoke | POST | `/api/v1/unified-backend/admin/http-smoke/run` | 是 | `ADMIN` 或 `OWNER` | LOW |

`GET /api/v1/unified-backend/health` 返回 `service=unified-backend`、`status=UP`、`port=8135`、`deploymentMode=CANDIDATE_PARALLEL_ENTRYPOINT`、`mountedEntrypoints`、`mountedRouteIds` 和 `generatedAt`。该接口只表示候选进程存活，不表示可替换当前网关。

后台摘要、挂载清单和 readiness 必须固定暴露 `currentProductionEntrypointsTotal=6`、`candidateEntrypointsTotal=1`、`mountedEntrypoints=["api-gateway","business-core","admission-core","engagement-core","ops-core","portal-core"]`、`mountedRouteIds=["auth","profile","notification","content","server-status","resource","admin","onboarding","exam","whitelist","attendance","community","activity","calendar","changelog","ops-control","cloudreve-sync","backup-recovery","alerting","plugin-integration","cross-platform-notification","ops-image-market","guide","material","online-map"]`、`inProcessRoutesTotal=25`、`httpFallbackRoutesTotal=0`、`externalRoutesTotal=0`、`externalNodeExecutorOutOfRepository=true`、`externalNodeExecutorConnected=false`、`readyToReplaceGateway=false`、`readyToRetireBusinessCore=false`、`readyToRetireAdmissionCore=false`、`readyToRetireEngagementCore=false`、`readyToRetireOpsCore=false` 和 `readyToRetirePortalCore=false`。

readiness 必须额外暴露 `productionSwitchReadinessStatus=BLOCKED`、`productionSwitchChecks`、`centralConfigPrecheckStatus=BLOCKED`、`centralConfigPrecheckChecks`、`centralConfigGovernancePrecheckStatus=BLOCKED`、`centralConfigGovernancePrecheckChecks`、`centralConfigGovernanceEvidence`、`productionCentralConfigPrecheckStatus=BLOCKED_BY_PRODUCTION_CONFIG_PROVIDER_NOT_CONNECTED`、`productionCentralConfigPrecheckChecks`、`productionCentralConfigEvidence`、`persistentAuditPrecheckStatus=BLOCKED`、`persistentAuditPrecheckChecks`、`persistentAuditGovernancePrecheckStatus=BLOCKED`、`persistentAuditGovernancePrecheckChecks`、`persistentAuditGovernanceEvidence`、`realHttpRehearsalPrecheckStatus=BLOCKED`、`realHttpRehearsalPrecheckChecks`、`routeDriftPrecheckStatus=PASS`、`routeDriftPrecheckChecks`、`rollbackWindowPrecheckStatus=BLOCKED`、`rollbackWindowPrecheckChecks`、`entrypointSwitchPrecheckStatus=BLOCKED`、`entrypointSwitchPrecheckChecks`、`backendSingleServicePrecheckStatus=PASS`、`backendSingleServicePrecheckChecks`、`backendSingleServiceEvidence`、`finalBackendSingleServicePrecheckStatus`、`finalBackendSingleServicePrecheckChecks`、`finalBackendSingleServiceEvidence`、`singleServiceCutoverPrecheckStatus=PASS_READY_FOR_EXTERNAL_CUTOVER`、`singleServiceCutoverPrecheckChecks`、`singleServiceCutoverEvidence`、`entrypointCutoverExecutionPrecheckStatus=BLOCKED`、`entrypointCutoverExecutionPrecheckChecks`、`entrypointCutoverExecutionEvidence`、`productionEntrypointCutoverPrecheckStatus=BLOCKED_BY_MISSING_EXTERNAL_ENTRYPOINT_CONFIG`、`productionEntrypointCutoverPrecheckChecks`、`productionEntrypointCutoverEvidence`、`apiGatewayRetirementPrecheckStatus=BLOCKED_BY_TRAFFIC_NOT_SWITCHED`、`apiGatewayRetirementPrecheckChecks`、`apiGatewayRetirementEvidence`、`coreEntrypointRetirementPrecheckStatus=BLOCKED_BY_PROTECTED_ROLLBACK_ROLE`、`coreEntrypointRetirementPrecheckChecks`、`coreEntrypointRetirementEvidence`、`productionHardeningPrecheckStatus=BLOCKED_BY_EXTERNAL_PRODUCTION_PREREQUISITES`、`productionHardeningPrecheckChecks`、`productionHardeningEvidence` 和 `replacementDecision`。当前只允许把全业务 in-process 覆盖、后端应用入口覆盖、五个 core 自有 API 挂载、25 条业务路由 in-process、当前入口保留、路径响应保持、外部节点执行器出仓边界、旧入口未恢复、候选端口固定、当前入口端口已记录、in-process 路由注册固定、危险测试控制关闭、配置归属已记录、候选配置面已记录、配置域清单、配置漂移扫描、配置回滚来源、配置脱敏守卫、审计归属已记录、审计请求编号保留、审计事件结构固定、审计保留窗口已记录、审计备份导出路径已记录、审计回放范围已记录、审计配置回滚源、审计脱敏守卫、真实 HTTP 目标清单已记录、认证失败路径已纳入、smoke 结果脱敏规则已固定、真实 Web 环境候选进程已启动、全量真实 HTTP 目标已通过、演练结果已记录、当前网关路由和候选挂载清单已记录、路径前缀保持、真实路由差异扫描、认证行为差异扫描、错误码差异扫描、敏感字段差异扫描、旧入口仍保留、回滚目标已记录、旧入口回归、生产源码边界扫描、候选 base URL 已记录、前端本轮未修改、业务路径保持不变、网关回滚目标已定义、网关退役门禁已记录、五个 core 退役前置矩阵已记录、生产化硬化 runbook 口径和 smoke 证据格式已记录标为 `PASS`；集中配置提供方、生产 profile、敏感配置源外置、真实持久化审计落点、审计写入路径、审计回放路径、审计保留任务、演练 runbook、回滚复检、前端入口切换、外部代理切换、外部入口配置存在、生产流量灰度、生产流量入口、流量归零证明、`api-gateway` 真实退役和五个 core 删除确认必须继续标为 `BLOCKED`。

HTTP smoke 至少覆盖 `UNIFIED_HEALTH`、`GATEWAY_HEALTH`、`BUSINESS_CORE_HEALTH`、`ADMISSION_CORE_HEALTH`、`ENGAGEMENT_CORE_HEALTH`、`OPS_CORE_HEALTH`、`PORTAL_CORE_HEALTH`、`AUTH_SESSION_VERIFY`、`PROFILE_MEMBERS`、`NOTIFICATION_UNREAD_COUNT`、`CONTENT_HOME`、`SERVER_STATUS_OVERVIEW`、`RESOURCE_LIST`、`ADMIN_OVERVIEW`、`ONBOARDING_PROGRESS`、`EXAM_SESSIONS`、`WHITELIST_CURRENT_APPLICATION`、`ATTENDANCE_LEADERBOARD`、`COMMUNITY_BOARDS`、`ACTIVITY_EVENTS`、`CALENDAR_UPCOMING`、`CHANGELOG_LATEST_VERSION`、`OPS_CONTROL_OVERVIEW`、`CLOUDREVE_SYNC_HEALTH`、`BACKUP_RECOVERY_HEALTH`、`ALERTING_HEALTH`、`PLUGIN_INTEGRATION_HEALTH`、`CROSS_PLATFORM_NOTIFICATION_HEALTH`、`OPS_IMAGE_MARKET_HEALTH`、`GUIDE_CATEGORIES`、`MATERIAL_FEATURED` 和 `ONLINE_MAP_HEALTH`。25 条业务路径必须通过本地控制器成功，不能调用 `GatewayHttpClient` 的对应代理路径。smoke 失败时接口自身返回统一成功响应，并在 `data.httpSmokeStatus` 中标记 `DEGRADED`。

验收时必须确认 `backend/unified-backend-service` 可独立测试，真实 HTTP 演练、路由漂移扫描、后端单服务准备 evidence、最终单服务后端侧收束 evidence、入口切换执行 evidence、生产入口切换阻塞 evidence、`api-gateway` 退役门禁 evidence、五个 core 退役前置矩阵、生产化硬化前置矩阵和生产集中配置前置矩阵有自动化覆盖，当前 6 个生产后端回滚入口和候选入口回归通过，外部节点执行器不进入候选入口源码扫描和 component scan，已退役旧服务入口没有恢复，生产源码危险删除命令、真实节点执行、终端、RCON、Docker 执行和备份恢复写入扫描无命中。第二十八轮生产集中配置前置准备完成后只能说明候选入口具备集中配置生产接入前置证据；由于仓库内没有真实前端、反向代理或部署入口配置，集中配置 provider、生产 profile、敏感配置外置和持久化审计也未接入，不能说明已经完成生产流量切换、前端或外部代理切换、集中配置接入、真实审计接入、`api-gateway` 退役、五个 core 退役或外部节点执行器合并。

本阶段允许 `unified-backend-service:8135` 以 in-process 方式挂载 `business-core`、`admission-core`、`engagement-core` 和 `ops-core`。该候选挂载不改变 `business-core-service:8130`、`admission-core-service:8131`、`engagement-core-service:8132` 或 `ops-core-service:8133` 的独立入口，不改变 `/api/v1/business-core/**`、`/api/v1/admission-core/**`、`/api/v1/engagement-core/**`、`/api/v1/ops-core/**`、`/api/v1/auth/**`、`/api/v1/profile/**`、`/api/v1/notifications/**`、`/api/v1/content/**`、`/api/v1/server-status/**`、`/api/v1/resources/**`、`/api/v1/admin/**`、`/api/v1/onboarding/**`、`/api/v1/exams/**`、`/api/v1/whitelist/**`、`/api/v1/attendance/**`、`/api/v1/community/**`、`/api/v1/activity/**`、`/api/v1/calendar/**`、`/api/v1/changelog/**`、`/api/v1/ops-control/**`、`/api/v1/cloudreve-sync/**`、`/api/v1/backup-recovery/**`、`/api/v1/alerting/**`、`/api/v1/plugin-integration/**`、`/api/v1/cross-platform-notification/**` 或 `/api/v1/ops-image-market/**` 的路径、认证、响应格式和错误码。

## 北冥官网 portal-core API 契约

来源：`docs/contracts-portal-core.md`

版本：0.1

`portal-core-service` 是第五批玩家门户体验运行合并单元，端口固定为 `8134`，承载 `guide`、`material` 和 `online-map` 三个模块的既有业务路径。它不新增三个业务模块的业务语义，不改变 `/api/v1/guides/**`、`/api/v1/materials/**` 和 `/api/v1/online-map/**` 的路径、权限、状态机、错误码、审计对象或失败降级规则。

`portal-core` 自有接口只用于运行单元健康检查、运行摘要、模块装配摘要、生产就绪诊断和显式 HTTP smoke。被承载业务模块的业务接口仍分别以 `docs/contracts-guide.md`、`docs/contracts-material.md` 和 `docs/contracts-online-map.md` 为准。

第九轮允许 `unified-backend-service:8135` 以 in-process 方式挂载 `portal-core`。该候选挂载不改变 `portal-core-service:8134` 的独立入口，不改变 `/api/v1/portal-core/**`、`/api/v1/guides/**`、`/api/v1/materials/**` 或 `/api/v1/online-map/**` 的路径、认证、响应格式和错误码。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/portal-core/health` | 否 | 无 | LOW |
| 运行摘要 | GET | `/api/v1/portal-core/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 模块装配摘要 | GET | `/api/v1/portal-core/admin/modules` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 生产就绪诊断 | GET | `/api/v1/portal-core/admin/readiness` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 执行 HTTP smoke | POST | `/api/v1/portal-core/admin/http-smoke/run` | 是 | `ADMIN` 或 `OWNER` | LOW |

网关切换后，`material`、`guide` 和 `online-map` 的上游端口均为 `8134`，历史端口 `8126`、`8127` 和 `8121` 只作记录。旧 `backend/online-map-service` 已退役且不得恢复。第六期切换后，`cross-platform-notification` 的上游端口为 `8133`，历史端口 `8123` 只作记录。`external-node-executor`、`api-gateway` 和 `ops-core` 继续保持独立，不并入 `portal-core`。

验收时必须确认 `portal-core-service:8134` 单进程承载三个模块全部既有 API 路径，三个模块原契约仍有效，`portal-core` 自有五个接口全覆盖，`api-gateway-service` 已按契约切换并通过测试，旧 `guide-service` 和 `material-service` Maven 入口已退役且不得恢复，旧 `backend/online-map-service` 已退役且不得恢复，生产 readiness 明确暴露真实持久化、真实对象存储、真实扫描、真实搜索、真实地图 provider HTTP、真实 marker 同步、真实瓦片托管、真实外部通知、静态服务发现和 HTTP smoke 状态等剩余缺口。

## 北冥官网 material API 契约

来源：`docs/contracts-material.md`

版本：0.1

### 文档定位

本文档是 `material` 微服务的正式 API 契约。后续 `content`、`community`、`resource`、`admin`、前端适配和精彩瞬间展示只能通过本文档定义的接口读取或管理玩家投稿素材，不能直接读取或修改 `material` 数据库。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、能力点、审计字段、风险等级、通用状态模型和通用错误码均以公共契约为准。本文档只补充 `material` 的职责边界、上传模型、授权声明、文件安全状态、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

`material` 适配 `auth`、`profile` 和 `notification`，不要求前序服务反向适配 `material`。它通过后端入口传入的认证上下文、`/api/v1/auth/me`、`/api/v1/auth/session/verify` 或测试环境 auth stub 读取当前用户、角色和能力点；通过 profile 正式接口或受控 stub 读取投稿作者公开快照和成员状态；通过 notification 正式接口或受控适配层投递审核结果通知。`material` 不能导入前序服务的实体、Repository、内存存储、测试种子或内部实现。

### 参考生态

本契约参考成熟上传和素材分发生态，但只吸收适合北冥官网当前阶段的边界。Cloudinary Upload API 把服务端签名、unsigned upload preset、资源类型、moderation 和大文件上传拆开，适合借鉴为短期上传票据、上传预设和审核状态。Amazon S3 presigned URL 把上传权限压缩到指定对象、方法、过期时间和校验约束中，适合借鉴为持有即授权的短期上传会话。OWASP File Upload Cheat Sheet 是上传安全底线，要求扩展名白名单、不能信任 `Content-Type`、服务端生成文件名、大小限制、授权上传、隔离存储和必要的安全扫描。Modrinth 和 CurseForge 这类内容平台强调项目、版本、文件哈希、依赖、许可证、审核和可见范围分离，适合借鉴为素材主数据、文件安全摘要和公开展示快照分离。GitHub Releases 的 release asset 设计说明上传资产和发布实体应有独立生命周期，适合确认素材附件不能直接等同素材审核通过。

参考来源包括 [Cloudinary Upload API](https://cloudinary.com/documentation/image_upload_api_reference)、[Cloudinary Upload Presets](https://cloudinary.com/documentation/upload_presets)、[Amazon S3 presigned URL upload](https://docs.aws.amazon.com/AmazonS3/latest/userguide/PresignedUrlUploadObject.html)、[OWASP File Upload Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)、[Modrinth API](https://docs.modrinth.com/api/)、[CurseForge for Studios API](https://docs.curseforge.com/) 和 [GitHub REST API release assets](https://docs.github.com/en/rest/releases/assets)。

### 职责边界

`material` 负责玩家素材投稿、上传会话、上传文件安全摘要、素材授权声明、投稿记录、审核状态、精选状态、公开素材列表、公开素材详情、公开安全文件摘要、当前用户投稿管理、后台审核、素材分类、素材审计、依赖摘要和自检摘要。

`material` 不负责官网首页配置、文章正文、资源下载、Cloudreve 分享链接、资源版本、社区帖子正文、工单附件主数据、活动报名、日历、更新日志、通知主数据、后台聚合入口、服务器文件管理、容器、终端、节点执行、真实文件删除、真实对象存储密钥托管或运维审计。

`content` 后续可以引用精选素材公开快照做精彩瞬间展示，但不能读取 `material` 数据库，也不能修改素材审核状态。`community` 可以引用公开素材快照作为帖子或工单证据，但不能接管素材主数据。`resource` 只管理玩家可见下载资源和 Cloudreve 分享，不接管投稿原始素材。`ops-control` 和 `external-node-executor` 不能复用 `material` 上传权限做服务器文件管理。

### 数据归属

`material` 拥有以下主数据：素材分类、素材投稿、上传会话、素材文件安全摘要、素材授权声明、公开素材快照、素材状态记录、幂等记录、素材审计日志和运行自检摘要。

作者快照字段来自 profile 或可信认证上下文，至少包括 `userId`、`memberId`、`displayName`、`avatarUrl`、`minecraftId`、`memberStatus` 和 `profileSnapshotAt`。快照只用于展示和审计，不是 auth 或 profile 主数据，不能用于账号权限或成员资格最终判断。

文件安全摘要只保存服务端生成文件名、脱敏原始文件名、MIME 摘要、扩展名、大小、校验值、安全状态、公开访问摘要和上传会话关联。不得保存对象存储管理密钥、Cloudreve 管理 token、内部绝对路径、上传票据明文长期副本、真实防病毒扫描原始报告或可直接执行的服务器路径。

### 基础路径与认证

公开接口使用 `/api/v1/materials` 前缀，不要求登录。公开接口只能返回公开可见、已通过或已精选、未下架、未归档、未软删除、处于可见时间范围内且全部公开文件安全状态为 `SAFE` 的素材。公开响应不得返回上传票据、内部路径、后台备注、审核意见、审计字段、通知结果、作者敏感字段、隔离文件、拒绝文件或原始存储 key。

当前用户投稿接口使用 `/api/v1/materials/me` 前缀，全部要求登录。当前用户只能创建自己的上传会话和投稿，只能读取、编辑、提交、撤回或重新提交自己的素材。浏览器请求体中传入 `authorUserId`、`authorSnapshot`、`status`、`reviewerUserId`、`featuredBy`、`auditLogs` 等服务端字段时，必须忽略并以服务端上下文为准，生产实现推荐返回字段校验失败。

后台接口使用 `/api/v1/materials/admin` 前缀，全部要求登录。后台读取允许 `HELPER`、`ADMIN` 或 `OWNER`。审核通过、拒绝和要求修改允许 `HELPER`、`ADMIN` 或 `OWNER`。精选、取消精选、下架、归档、软删除、分类维护和文件安全状态维护要求 `ADMIN` 或 `OWNER`。审计列表和自检摘要只允许 `ADMIN` 或 `OWNER`。

`material` 当前由 `portal-core-service:8134` 承载。历史原服务端口 `8126` 只作为对照记录，不再作为当前网关默认上游。`api-gateway` 必须以兼容方式保留 `/api/v1/materials` 路由，不能改变已有服务路径、认证方式、响应格式或测试。

### auth 兼容契约

当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得创建上传会话、投稿或访问后台接口。

`material` 通过 api-gateway 访问时，优先读取网关注入的可信身份头：`X-Beiming-Actor-User-Id`、`X-Beiming-Actor-Roles`、`X-Beiming-Actor-Permissions`、`X-Beiming-Actor-Minecraft-Id` 和 `X-Gateway-Internal-Request-Id`。P0.1 中只有 `X-Gateway-Internal-Request-Id` 与当前 `X-Request-Id` 一致时，才接受这些 actor 头。客户端直接伪造的 actor 头必须忽略，不得覆盖 `Authorization` 校验结果。生产接入内部签名或 mTLS 前，自检摘要必须暴露 `GATEWAY_INTERNAL_SIGNATURE_NOT_ENABLED` 缺口。

后台写操作中的 `createdBy`、`updatedBy`、`submittedBy`、`reviewedBy`、`featuredBy`、`offlineBy`、`archivedBy` 和 `deletedBy` 均来自服务端认证上下文。auth 上下文不可用返回 `46700`，auth 调用超时返回 `46701`，auth 字段缺失或枚举不兼容返回 `46702`。

### profile 兼容契约

创建投稿和提交审核时，`material` 必须通过 profile 适配层或可信认证上下文读取作者公开快照。允许投稿的成员状态为 `ACTIVE` 和 `INACTIVE`。`SUSPENDED`、`REMOVED`、`ARCHIVED`、无档案、profile 不可用或 profile 字段不兼容时不得提交审核。

profile 成员不存在或不可公开返回 `46710`。profile 调用超时返回 `46711`。profile 字段缺失或枚举不兼容返回 `46712`。已公开素材可以继续返回已保存作者快照，并在后台详情中显示 `profileSnapshotAt`，但不能把旧快照用于新的成员资格判断。

### notification 兼容契约

审核拒绝和要求修改必须通知素材作者。强制通知失败时，素材状态不得变化，返回 `46720` 或 `46721`。审核通过、被精选、取消精选、下架、归档和软删除通知为辅助提醒，通知失败时主流程可以成功，但必须在审计中记录 `notificationStatus=FAILED` 和失败原因。

没有可通知作者时，强制通知场景返回 `46722`。辅助通知场景可跳过通知并写审计摘要。`material` 不保存通知主数据、未读数、模板或外部渠道投递记录。

### 上传和安全模型

P0 上传采用 `LOCAL_STUB` 模式。客户端先创建上传会话，服务端返回短期 `uploadTicket`、允许的 MIME、扩展名、大小上限、文件数量上限、checksum 要求和 stub 上传目标。客户端完成上传后调用 complete，服务端保存文件安全摘要并把文件状态置为 `SAFE` 或按测试头模拟为 `REJECTED`、`QUARANTINED`、`EXPIRED`。

上传会话是持有即授权的短期能力，必须绑定当前用户、用途、素材类型、允许文件数量、单文件大小、总大小、checksum、幂等键和过期时间。会话不得被其他用户完成，不得完成超过限制的文件，不得在过期后完成。重复 complete 同一会话和同一文件摘要应幂等返回同一 asset；相同幂等键搭配不同请求体返回 `43714`。

服务端必须生成文件名，原始文件名只保存脱敏显示名。扩展名白名单和文件签名摘要必须同时校验，不得只信任 `Content-Type`。危险扩展、双扩展、空字节、路径穿越、可执行脚本、压缩炸弹模拟、超大文件、checksum 不匹配和 MIME 伪造都返回 `43712` 或字段校验错误。文件不得通过内部路径直接公开访问，公开接口只能返回 material 控制的 `publicAssetUrl` 和安全摘要。

### 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `MaterialKind` | `IMAGE`、`VIDEO`、`BUILD_SCREENSHOT`、`PROJECT_RECORD`、`EVENT_MEMORY`、`DOCUMENT_ATTACHMENT`、`OTHER` | 素材类型。 |
| `MaterialStatus` | `DRAFT`、`UPLOADING`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`FEATURED`、`OFFLINE`、`ARCHIVED`、`DELETED` | 素材状态。`FEATURED` 是公开强化状态，取消精选后回到 `APPROVED`。 |
| `AssetStatus` | `PENDING_UPLOAD`、`UPLOADED`、`SCANNING`、`SAFE`、`REJECTED`、`QUARANTINED`、`EXPIRED` | 文件安全状态。只有 `SAFE` 文件可公开。 |
| `LicenseType` | `ORIGINAL`、`SERVER_SHARED`、`CC_BY_NC`、`CC_BY_SA`、`AUTHORIZED_REPOST` | 授权声明。 |
| `MaterialVisibility` | `PUBLIC`、`MEMBER_ONLY`、`PRIVATE` | P0 公开接口只返回 `PUBLIC`。 |
| `UploadProvider` | `LOCAL_STUB`、`S3_PRESIGNED`、`CLOUDINARY_UNSIGNED`、`OBJECT_STORAGE` | 上传提供方。P0 只实现 `LOCAL_STUB`。 |
| `MaterialAuditResult` | `SUCCESS`、`FAILED` | 素材审计执行结果。 |

### 通用对象

#### MaterialCategory

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `categoryId` | string | 是 | 分类 ID。 |
| `name` | string | 是 | 分类名称，2 到 40 位，同一未归档分类中唯一。 |
| `slug` | string | 是 | 分类 slug，2 到 80 位，只允许小写字母、数字和短横线。 |
| `description` | string 或 null | 是 | 分类说明，最多 200 位。 |
| `sortOrder` | integer | 是 | 排序值，数字越小越靠前。 |
| `kind` | string | 是 | 适用素材类型。P0.1 创建分类未传时默认为 `IMAGE`。 |
| `enabled` | boolean | 是 | 是否启用。 |
| `archived` | boolean | 是 | 是否归档。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |

#### MaterialAuthorSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | string | 是 | auth 用户 ID。 |
| `memberId` | string 或 null | 是 | profile 成员 ID。 |
| `displayName` | string | 是 | 展示名快照。 |
| `avatarUrl` | string 或 null | 是 | 头像快照。 |
| `minecraftId` | string 或 null | 是 | Minecraft ID 快照。 |
| `memberStatus` | string 或 null | 是 | 成员状态快照。 |
| `profileSnapshotAt` | string | 是 | 快照获取时间。 |

#### MaterialAssetSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `assetId` | string | 是 | 文件摘要 ID。 |
| `materialId` | string 或 null | 是 | 关联素材 ID，未关联投稿时为 `null`。 |
| `uploadSessionId` | string | 是 | 上传会话 ID。 |
| `provider` | string | 是 | `UploadProvider`。 |
| `status` | string | 是 | `AssetStatus`。 |
| `displayName` | string | 是 | 脱敏文件展示名。 |
| `extension` | string | 是 | 小写扩展名。 |
| `mimeType` | string | 是 | 服务端识别 MIME 摘要。 |
| `fileSizeBytes` | integer | 是 | 文件大小。 |
| `checksumSha256` | string | 是 | SHA-256。 |
| `width` | integer 或 null | 是 | 图片或视频宽度。 |
| `height` | integer 或 null | 是 | 图片或视频高度。 |
| `durationSeconds` | integer 或 null | 是 | 视频时长。 |
| `publicAssetUrl` | string 或 null | 是 | 安全公开 URL。未安全通过时为 `null`。 |
| `securityRejectReason` | string 或 null | 是 | 安全拒绝原因摘要，公开接口不得返回。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### MaterialLicenseStatement

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `licenseType` | string | 是 | `LicenseType`。 |
| `authorConfirmed` | boolean | 是 | 投稿者确认有权投稿。 |
| `allowHomepageFeature` | boolean | 是 | 是否允许首页或专题精选展示。 |
| `allowDerivativeUse` | boolean | 是 | 是否允许二次编辑用于官网展示。 |
| `sourceUrl` | string 或 null | 是 | 授权转载来源 URL。`AUTHORIZED_REPOST` 必填。 |
| `creditText` | string 或 null | 是 | 署名文案，最多 120 位。 |

#### PublicMaterialSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `materialId` | string | 是 | 素材 ID。 |
| `slug` | string | 是 | 公开 slug。 |
| `kind` | string | 是 | `MaterialKind`。 |
| `status` | string | 是 | 公开接口只返回 `APPROVED` 或 `FEATURED`。 |
| `title` | string | 是 | 标题，2 到 120 位。 |
| `summary` | string 或 null | 是 | 摘要，最多 300 位。 |
| `coverAsset` | MaterialAssetSummary | 是 | 安全封面文件摘要。公开响应不含安全拒绝原因。 |
| `category` | MaterialCategory 或 null | 是 | 分类公开摘要。 |
| `tags` | string[] | 是 | 标签，最多 20 个。 |
| `author` | MaterialAuthorSnapshot | 是 | 作者公开快照。 |
| `license` | MaterialLicenseStatement | 是 | 授权公开摘要。 |
| `featured` | boolean | 是 | 是否精选。 |
| `publishedAt` | string | 是 | 公开时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

#### PublicMaterialDetail

`PublicMaterialDetail` 在 `PublicMaterialSummary` 基础上补充 `description`、`assets`、`visibleFrom`、`visibleUntil`、`createdAt`。公开详情仍不得返回后台备注、审核意见、上传票据、内部路径、通知结果或审计字段。

#### UploadSessionView

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `uploadSessionId` | string | 是 | 上传会话 ID。 |
| `provider` | string | 是 | P0 固定为 `LOCAL_STUB`。 |
| `purpose` | string | 是 | 固定为 `MATERIAL_SUBMISSION`。 |
| `ownerUserId` | string | 是 | 当前用户 ID。 |
| `allowedExtensions` | string[] | 是 | 允许扩展名。 |
| `allowedMimeTypes` | string[] | 是 | 允许 MIME。 |
| `maxFileSizeBytes` | integer | 是 | 单文件大小上限。 |
| `maxFiles` | integer | 是 | 文件数量上限。 |
| `uploadTicket` | string | 是 | 短期上传票据，只在创建响应返回，后台详情不得返回。 |
| `uploadTarget` | string | 是 | stub 上传目标或未来对象存储目标。 |
| `status` | string | 是 | `PENDING_UPLOAD`、`UPLOADED` 或 `EXPIRED`。 |
| `expiresAt` | string | 是 | 过期时间。 |
| `createdAt` | string | 是 | 创建时间。 |

#### MyMaterialSubmission

当前用户投稿视图包含素材主体、文件摘要、授权声明、作者快照、状态、提交时间、审核意见、后台要求修改公开意见、通知摘要和可执行动作。它不得返回其他用户投稿，也不得返回内部路径、上传票据和审计参数全文。

#### AdminMaterialItem

后台素材视图包含素材主体、全部文件安全摘要、授权声明、作者快照、后台备注、审核意见、通知摘要、状态时间、操作者 ID、审计摘要和生产化提示。后台详情可以返回 `securityRejectReason`，但不得返回真实对象存储密钥、内部绝对路径、上传票据明文或通知正文全文。

#### MaterialAuditLog

审计字段继承公共契约，允许补充 `materialId`、`assetId`、`uploadSessionId`、`idempotencyKey`、`stateFrom`、`stateTo`、`notificationStatus`、`profileSnapshotStatus` 和 `assetStatus`。审计日志不得通过 material API 删除。

### material 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `43700` | 404 | 素材不存在，或公开接口无权访问该素材。 |
| `43701` | 404 | 上传会话不存在或已过期。 |
| `43702` | 404 | 文件摘要不存在。 |
| `43703` | 404 | 分类不存在。 |
| `43710` | 409 | 素材状态不允许当前操作。 |
| `43711` | 409 | slug、分类或投稿指纹冲突。 |
| `43712` | 400 | 上传文件类型、大小、签名、checksum 或文件名不符合规则。 |
| `43713` | 400 | 授权声明缺失或不兼容。 |
| `43714` | 409 | 幂等键请求指纹冲突。 |
| `43715` | 409 | 文件安全状态不允许提交或公开。 |
| `43716` | 409 | 分类仍被未归档素材引用，不能归档。 |
| `46700` | 502 | auth 认证上下文不可用。 |
| `46701` | 504 | auth 认证上下文调用超时。 |
| `46702` | 502 | auth 响应字段或枚举不兼容 material 契约。 |
| `46710` | 502 | profile 作者快照不可用。 |
| `46711` | 504 | profile 作者快照调用超时。 |
| `46712` | 502 | profile 作者快照字段或枚举不兼容 material 契约。 |
| `46720` | 502 | notification 强制投递不可用。 |
| `46721` | 504 | notification 强制投递超时。 |
| `46722` | 502 | notification 强制投递缺少可通知作者。 |
| `46730` | 502 | 上传存储适配器不可用。 |
| `46731` | 504 | 上传存储适配器超时。 |
| `51700` | 500 | material 内部错误。 |
| `51701` | 500 | material 审计写入失败。 |
| `51702` | 500 | 上传记录写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误和通用服务端错误优先使用公共错误码。material 自有幂等冲突使用 `43714`。

### 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 精选素材列表 | GET | `/api/v1/materials/featured` | 否 | 无 | LOW |
| 公开素材列表 | GET | `/api/v1/materials` | 否 | 无 | LOW |
| 公开素材详情 | GET | `/api/v1/materials/{materialId}` | 否 | 无 | LOW |
| 公开 slug 素材详情 | GET | `/api/v1/materials/by-slug/{slug}` | 否 | 无 | LOW |
| 公开分类列表 | GET | `/api/v1/materials/categories` | 否 | 无 | LOW |
| 公开素材文件摘要 | GET | `/api/v1/materials/{materialId}/assets` | 否 | 无 | LOW |
| 创建上传会话 | POST | `/api/v1/materials/me/upload-sessions` | 是 | 当前用户 | LOW |
| 完成上传会话 | PATCH | `/api/v1/materials/me/upload-sessions/{uploadSessionId}/complete` | 是 | 会话所有者 | LOW |
| 创建投稿 | POST | `/api/v1/materials/me/submissions` | 是 | 当前用户 | LOW |
| 我的投稿列表 | GET | `/api/v1/materials/me/submissions` | 是 | 当前用户 | LOW |
| 我的投稿详情 | GET | `/api/v1/materials/me/submissions/{materialId}` | 是 | 当前用户 | LOW |
| 修改我的投稿 | PATCH | `/api/v1/materials/me/submissions/{materialId}` | 是 | 当前用户 | LOW |
| 提交审核 | PATCH | `/api/v1/materials/me/submissions/{materialId}/submit-review` | 是 | 当前用户 | MEDIUM |
| 撤回投稿 | PATCH | `/api/v1/materials/me/submissions/{materialId}/withdraw` | 是 | 当前用户 | LOW |
| 重新提交 | PATCH | `/api/v1/materials/me/submissions/{materialId}/resubmit` | 是 | 当前用户 | MEDIUM |
| 后台素材列表 | GET | `/api/v1/materials/admin/items` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台素材详情 | GET | `/api/v1/materials/admin/items/{materialId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 审核通过 | PATCH | `/api/v1/materials/admin/items/{materialId}/approve` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/materials/admin/items/{materialId}/reject` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | MEDIUM |
| 要求修改 | PATCH | `/api/v1/materials/admin/items/{materialId}/request-changes` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | MEDIUM |
| 设为精选 | PATCH | `/api/v1/materials/admin/items/{materialId}/feature` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 取消精选 | PATCH | `/api/v1/materials/admin/items/{materialId}/unfeature` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 下架素材 | PATCH | `/api/v1/materials/admin/items/{materialId}/offline` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档素材 | PATCH | `/api/v1/materials/admin/items/{materialId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 软删除素材 | PATCH | `/api/v1/materials/admin/items/{materialId}/delete` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台分类列表 | GET | `/api/v1/materials/admin/categories` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建分类 | POST | `/api/v1/materials/admin/categories` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改分类 | PATCH | `/api/v1/materials/admin/categories/{categoryId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档分类 | PATCH | `/api/v1/materials/admin/categories/{categoryId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台文件安全摘要列表 | GET | `/api/v1/materials/admin/assets` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 修改文件安全状态 | PATCH | `/api/v1/materials/admin/assets/{assetId}/security-status` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 素材审计列表 | GET | `/api/v1/materials/admin/items/{materialId}/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| material 自检摘要 | GET | `/api/v1/materials/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

### 公开接口

#### 精选素材列表

`GET /api/v1/materials/featured`

查询参数包括 `limit`、`kind`、`categoryId` 和 `tag`。`limit` 默认 `12`，最大 `50`。成功响应 HTTP `200`，`data.items` 为 `PublicMaterialSummary[]`，只返回 `FEATURED`、`PUBLIC`、文件安全、未下架、未归档、未软删除且处于可见时间范围内的素材。

#### 公开素材列表

`GET /api/v1/materials`

查询参数包括 `page`、`pageSize`、`kind`、`categoryId`、`tag`、`authorUserId`、`keyword` 和 `sort`。`sort` 允许 `publishedAt_desc`、`updatedAt_desc`、`title_asc`、`featured_desc`。成功响应 HTTP `200`，分页 `items` 为 `PublicMaterialSummary[]`。

公开列表必须按过滤后的全集计算 `total`。空页返回空数组，不得回退第一页。`authorUserId` 只按已保存作者快照做公开筛选，不暴露作者敏感字段。公开列表不得返回未审核、需修改、已拒绝、已下架、已归档、已软删除、非公开可见、文件安全未通过、可见时间未开始或可见时间已结束的素材。

#### 公开素材详情

`GET /api/v1/materials/{materialId}` 和 `GET /api/v1/materials/by-slug/{slug}`

成功响应 HTTP `200`，`data` 为 `PublicMaterialDetail`。素材不存在、不可公开或文件安全未通过时返回 `43700`，不得暴露后台状态。

#### 公开分类列表

`GET /api/v1/materials/categories`

查询参数包括 `kind` 和 `keyword`。成功响应 HTTP `200`，`data.items` 为启用且未归档分类，按 `sortOrder` 和 `name` 稳定排序。

#### 公开素材文件摘要

`GET /api/v1/materials/{materialId}/assets`

成功响应 HTTP `200`，`data.items` 为公开安全文件摘要。只返回 `SAFE` 文件，且不得返回 `securityRejectReason`、上传会话票据、内部路径或对象存储 key。素材不可公开返回 `43700`。

### 当前用户投稿接口

#### 创建上传会话

`POST /api/v1/materials/me/upload-sessions`

请求字段包括 `kind`、`expectedFileNames`、`expectedMimeTypes`、`maxFileSizeBytes`、`checksumSha256` 和 `idempotencyKey`。`expectedFileNames` 为 1 到 10 个文件名。单文件大小上限不能超过模块配置上限。成功响应 HTTP `201`，`data` 为 `UploadSessionView`。

创建上传会话必须校验登录、用户状态、扩展名白名单、MIME 白名单、大小上限和 checksum 格式。相同用户、相同 `idempotencyKey`、相同请求体重复提交返回同一会话。相同幂等键搭配不同请求体返回 `43714`。

#### 完成上传会话

`PATCH /api/v1/materials/me/upload-sessions/{uploadSessionId}/complete`

请求字段包括 `files`、`uploadTicket` 和 `idempotencyKey`。每个文件必须包含 `displayName`、`mimeType`、`extension`、`fileSizeBytes`、`checksumSha256`、`signature`、`width`、`height` 和 `durationSeconds`。成功响应 HTTP `200`，`data.items` 为 `MaterialAssetSummary[]`。

会话不存在、过期或不属于当前用户返回 `43701`。文件超限、扩展名非法、MIME 伪造、签名不符、checksum 不符、双扩展、路径穿越、空字节、危险脚本或压缩炸弹模拟返回 `43712`。上传记录写入失败返回 `51702`，不得产生半文件摘要。

#### 创建投稿

`POST /api/v1/materials/me/submissions`

请求字段包括 `kind`、`slug`、`title`、`summary`、`description`、`categoryId`、`tags`、`assetIds`、`coverAssetId`、`visibility`、`license`、`visibleFrom`、`visibleUntil` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `MyMaterialSubmission`，初始状态为 `DRAFT`。

创建投稿必须校验资产属于当前用户且状态为 `SAFE`，授权声明完整，slug 在未软删除素材中唯一，分类存在，公开可见性合法。`AUTHORIZED_REPOST` 必须填写 `sourceUrl`。授权不兼容返回 `43713`。资产不安全返回 `43715`。

#### 我的投稿列表和详情

`GET /api/v1/materials/me/submissions` 支持 `page`、`pageSize`、`status`、`kind`、`keyword` 和 `sort`。`GET /api/v1/materials/me/submissions/{materialId}` 只允许读取当前用户自己的投稿。访问他人投稿返回 `43700`。

#### 修改我的投稿

`PATCH /api/v1/materials/me/submissions/{materialId}`

请求字段同创建投稿，除 `reason` 可选外其余字段按需修改。只有 `DRAFT` 和 `NEEDS_CHANGES` 可修改主体字段。`PENDING_REVIEW` 可先撤回再修改。`APPROVED`、`FEATURED`、`OFFLINE`、`ARCHIVED` 和 `DELETED` 返回 `43710`。

#### 提交审核

`PATCH /api/v1/materials/me/submissions/{materialId}/submit-review`

请求字段包括 `reason` 和可选 `idempotencyKey`。`DRAFT`、`REJECTED` 和 `NEEDS_CHANGES` 可流转为 `PENDING_REVIEW`。提交前必须再次校验 profile 作者快照、授权声明和所有公开文件为 `SAFE`。重复提交 `PENDING_REVIEW` 返回成功，保持幂等，不重复写审计。

#### 撤回投稿

`PATCH /api/v1/materials/me/submissions/{materialId}/withdraw`

请求字段包括 `reason`。只有 `PENDING_REVIEW` 可撤回为 `DRAFT`。重复撤回 `DRAFT` 返回成功，保持幂等。已审核或已公开素材不能由用户撤回，返回 `43710`。

#### 重新提交

`PATCH /api/v1/materials/me/submissions/{materialId}/resubmit`

请求字段包括 `reason`。`REJECTED` 和 `NEEDS_CHANGES` 可重新提交为 `PENDING_REVIEW`。重新提交等价于提交审核，但必须清理旧的公开修改意见快照并写审计。

### 后台素材接口

#### 后台素材列表和详情

`GET /api/v1/materials/admin/items` 支持 `page`、`pageSize`、`status`、`kind`、`visibility`、`categoryId`、`authorUserId`、`assetStatus`、`keyword` 和 `sort`。`sort` 允许 `submittedAt_desc`、`updatedAt_desc`、`publishedAt_desc`、`title_asc`。成功响应分页 `items` 为 `AdminMaterialItem[]`。所有筛选条件必须在分页前生效。

`GET /api/v1/materials/admin/items/{materialId}` 成功响应 `AdminMaterialItem`。素材不存在返回 `43700`。

#### 审核通过

`PATCH /api/v1/materials/admin/items/{materialId}/approve`

请求字段包括 `reviewOpinion`、`reason` 和可选 `idempotencyKey`。`PENDING_REVIEW` 可流转为 `APPROVED`，并写入 `reviewedAt`、`publishedAt` 和审核人。相同操作者、相同 `idempotencyKey` 和相同请求体重复审核返回首次结果，不重复写审计；相同 `idempotencyKey` 搭配不同请求体返回 `43714`。未传 `idempotencyKey` 时，重复审核已 `APPROVED` 返回成功，不重复写审计。所有文件必须为 `SAFE`，否则返回 `43715`。辅助通知失败时主流程成功但审计记录失败摘要。

#### 审核拒绝

`PATCH /api/v1/materials/admin/items/{materialId}/reject`

请求字段包括 `reviewOpinion` 和 `reason`。`PENDING_REVIEW` 可流转为 `REJECTED`。拒绝必须通知作者，强制通知失败时状态不变并返回 `46720` 或 `46721`。重复拒绝保持幂等。

#### 要求修改

`PATCH /api/v1/materials/admin/items/{materialId}/request-changes`

请求字段包括 `reviewOpinion`、`publicComment` 和 `reason`。`PENDING_REVIEW` 可流转为 `NEEDS_CHANGES`。要求修改必须通知作者，强制通知失败时状态不变。

#### 精选、取消精选、下架、归档和软删除

`PATCH /api/v1/materials/admin/items/{materialId}/feature` 允许 `APPROVED` 流转为 `FEATURED`。重复精选保持幂等。只有 `license.allowHomepageFeature=true` 的素材可以精选，否则返回 `43713`。

`PATCH /api/v1/materials/admin/items/{materialId}/unfeature` 允许 `FEATURED` 流转为 `APPROVED`。重复取消精选保持幂等。

`PATCH /api/v1/materials/admin/items/{materialId}/offline` 允许 `APPROVED` 或 `FEATURED` 流转为 `OFFLINE`。重复下架保持幂等。下架后公开接口不可见。

`PATCH /api/v1/materials/admin/items/{materialId}/archive` 允许 `DRAFT`、`REJECTED`、`NEEDS_CHANGES` 和 `OFFLINE` 流转为 `ARCHIVED`。公开中的素材必须先下架再归档。

`PATCH /api/v1/materials/admin/items/{materialId}/delete` 只做软删除，状态为 `DELETED`，写入 `deletedAt`。公开中的素材必须先下架再软删除。P0 不提供真实删除接口。

这些状态接口请求字段均包含必填 `reason` 和可选 `idempotencyKey`。相同操作者、相同接口语义、相同 `idempotencyKey` 和相同请求体重复提交返回首次结果；相同 `idempotencyKey` 搭配不同请求体返回 `43714`。审计失败时不得改变业务状态。

### 后台分类接口

`GET /api/v1/materials/admin/categories` 支持 `includeArchived`、`enabled`、`kind` 和 `keyword`。`POST /api/v1/materials/admin/categories` 创建分类，`PATCH /api/v1/materials/admin/categories/{categoryId}` 修改分类，`PATCH /api/v1/materials/admin/categories/{categoryId}/archive` 归档分类。创建和修改字段包括 `name`、`slug`、`description`、`sortOrder`、`enabled`、`kind`、`reason` 和可选 `idempotencyKey`。分类名称或 slug 冲突返回 `43711`。仍被未归档、未软删除素材引用的分类不能归档，返回 `43716`。

### 后台文件安全接口

`GET /api/v1/materials/admin/assets` 支持 `page`、`pageSize`、`status`、`ownerUserId`、`materialId`、`extension`、`mimeType` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc` 和 `size_desc`。后台可以查看 `securityRejectReason`，但不得返回上传票据或内部绝对路径。

`PATCH /api/v1/materials/admin/assets/{assetId}/security-status` 请求字段包括 `status`、`securityRejectReason`、`reason` 和可选 `idempotencyKey`。允许在 `SCANNING`、`SAFE`、`REJECTED`、`QUARANTINED` 间维护安全状态。相同操作者、相同 `idempotencyKey` 和相同请求体重复提交返回首次结果；相同 `idempotencyKey` 搭配不同请求体返回 `43714`。把已公开素材的唯一公开文件改为非 `SAFE` 时，关联素材必须停止公开展示或返回文件安全冲突，不得继续公开危险文件。

### 审计和自检接口

`GET /api/v1/materials/admin/items/{materialId}/audit-logs` 支持 `page`、`pageSize`、`action`、`actorUserId`、`result`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc` 和 `createdAt_asc`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 material API 删除。

`GET /api/v1/materials/admin/ops/summary` 返回服务运行模式、端口、存储模式、auth/profile/notification/storage 适配模式、素材数量、待审核数量、精选数量、文件数量、安全状态统计、审计数量、幂等记录数量、生产化缺口和最近审计时间。摘要不得返回 token、上传票据、内部路径、后台备注、审核意见全文、通知正文、对象存储密钥或异常堆栈。

### 状态、幂等和并发

素材状态流转为 `DRAFT` 到 `PENDING_REVIEW`，`PENDING_REVIEW` 到 `APPROVED`、`REJECTED` 或 `NEEDS_CHANGES`，`REJECTED` 和 `NEEDS_CHANGES` 可重新提交，`APPROVED` 可设为 `FEATURED` 或下架为 `OFFLINE`，`FEATURED` 可取消精选回到 `APPROVED` 或下架为 `OFFLINE`，`OFFLINE` 可归档或软删除，`DRAFT`、`REJECTED` 和 `NEEDS_CHANGES` 可归档或软删除，`ARCHIVED` 原则上不可恢复，`DELETED` 为软删除终态。

文件安全状态和素材审核状态必须分开。文件未上传、校验中、隔离、拒绝或过期时，素材不能提交审核，也不能公开展示。素材审核通过不代表文件安全通过；文件安全通过也不代表审核通过。

创建上传会话、完成上传、创建投稿、提交审核、后台审核、后台状态操作、创建分类、修改分类和修改安全状态支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果。相同幂等键搭配不同请求体返回 `43714`。请求体指纹必须基于结构化 JSON 规范化结果，嵌套对象按字段名递归排序，数组保留顺序，不能依赖浏览器字段顺序或 Java `Map.toString()`。

P0.1 内存实现必须用本服务内临界区保护创建上传会话、完成上传、创建投稿、修改投稿、提交审核、撤回、重新提交、后台审核、后台状态操作、分类维护和文件安全状态修改。并发创建相同 slug、相同分类 slug、相同上传 complete 文件摘要时只能一个成功或一个幂等成功，其余返回冲突。公开读取允许读到更新前或更新后的完整状态，不能返回半更新对象。后续持久化实现必须把这些保护迁移为数据库事务、唯一约束、条件更新或等效机制。

### 审计要求

必须审计的动作包括创建上传会话、完成上传、创建投稿、修改投稿、提交审核、撤回、重新提交、审核通过、审核拒绝、要求修改、精选、取消精选、下架、归档、软删除、创建分类、修改分类、归档分类、修改文件安全状态、通知失败、profile 快照失败、上传记录失败和审计写入失败。

后台写操作必须记录 `reason`。当前用户提交审核、撤回和重新提交也必须记录原因或系统默认原因。审计字段继承公共契约。审计写入失败时，后台写操作和当前用户关键状态写操作不得假装成功，必须返回 `51701` 或 `51700`，并保持业务数据不变。

公开读取不强制写审计。上传安全拒绝可以写失败审计或安全日志，但不得泄露内部扫描规则。

### 失败降级

公开列表、精选列表、详情、分类和文件摘要在单个素材文件不可公开时应跳过不可公开素材或返回 `43700`，不能返回危险文件。素材主数据存储不可用时不能伪造成功。

auth 认证上下文失败时，当前用户和后台接口不得使用旧用户上下文继续写入。profile 失败时，不得提交新的投稿审核；已公开素材可使用已保存作者快照做公开展示。notification 强制投递失败时，拒绝和要求修改不得改变状态；辅助通知失败时主流程可成功，但必须保留失败摘要和审计记录。上传存储适配器不可用时，创建上传会话或完成上传返回依赖错误，不得生成可用票据或安全文件摘要。

### 验收口径

`material` API 文档按 `docs/contracts-material.md` 独立存在，并由 `.local-docs/tests-material.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`material` 完成时必须满足以下条件：全部接口按本文档实现；公开接口不泄露后台字段、上传票据、内部路径、对象存储密钥、通知结果和审计字段；当前用户只能管理自己的投稿；后台接口按角色限制；上传会话、文件安全摘要、授权声明、profile 作者快照、审核通知、精选展示、状态流转、幂等、审计、自检摘要、端口配置和前序服务适配都有自动化测试；`.local-docs/tests-material.md` 中全部测试用例都有对应自动化验证；未实现时自动化测试必须先失败；实现后 material 在 `portal-core-service:8134` 中全部测试通过；api-gateway `/api/v1/materials` 路由指向 `8134` 并测试通过；旧 `material-service:8126` Maven 入口已退役且不得恢复；没有修改前序服务稳定接口；没有把 `.local-docs/` 提交到仓库；没有把玩家资源下载、Cloudreve 管理、服务器文件管理、容器、终端、节点执行、真实文件删除或真实对象存储密钥塞进 `material`。

## 北冥官网 guide API 契约

来源：`docs/contracts-guide.md`

版本：0.1

### 文档定位

本文档是 `guide` 微服务的正式 API 契约。后续 `content`、`onboarding`、`exam`、`whitelist`、`resource`、`server-status`、`community`、`admin` 和前端指南中心只能通过本文档定义的接口读取或维护指南与知识库主数据，不能直接读取或修改 `guide` 数据库。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、能力点、审计字段、风险等级、通用状态模型和通用错误码均以公共契约为准。本文档只补充 `guide` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了成熟文档和知识库平台的设计，但只吸收适合北冥官网当前阶段的边界。GitBook 的 Space、Collection 和多版本发布说明适合借鉴为分类、文档集和版本化发布。Docusaurus 的 docs versioning 与 sidebar 思路适合借鉴为冻结版本、目录和侧边栏排序。Confluence 的空间、页面树、标签、权限和内容分析适合借鉴为后台维护、可见性和维护反馈。Notion Wiki 的 owner、verified page 和过期验证适合借鉴为负责人、验证时间和过期提醒。Algolia DocSearch 的 facet、版本过滤、命中摘要和 no-result feedback 适合借鉴为本地搜索结果摘要与反馈闭环。Discord Rules Screening 和 Community Onboarding 适合借鉴为规则确认、外部入口准入条件和社区加入说明。本文档不引入外部文档平台、外部搜索服务、跨平台 SSO 或第三方主数据同步。

参考来源包括 [GitBook collections](https://gitbook.com/docs/creating-content/content-structure/collection)、[GitBook multiple versions](https://gitbook.com/docs/help-center/published-documentation/publishing/how-can-i-publish-a-site-with-multiple-versions)、[Docusaurus docs versioning](https://docusaurus.io/docs/versioning)、[Docusaurus sidebar](https://docusaurus.io/docs/sidebar)、[Confluence spaces](https://support.atlassian.com/confluence-cloud/docs/create-a-space/)、[Confluence labels](https://support.atlassian.com/confluence-cloud/docs/use-labels-to-organize-your-content/)、[Notion wikis and verified pages](https://www.notion.com/help/wikis-and-verified-pages)、[Algolia DocSearch facets](https://docsearch.algolia.com/docs/legacy/faceting/)、[Algolia DocSearch insights](https://docsearch.algolia.com/docs/insights/)、[Discord Rules Screening FAQ](https://support.discord.com/hc/en-us/articles/1500000466882-Rules-Screening-FAQ) 和 [Discord Community Onboarding FAQ](https://support.discord.com/hc/en-us/articles/11074987197975-Community-Onboarding-FAQ)。

### 职责边界

`guide` 负责指南分类、指南文章、章节目录、版本记录、规则版本、客户端环境说明、常用指令索引、服务器地址说明、资源说明引用、外部交流入口、搜索摘要、用户反馈、后台审核、发布、下架、归档、软删除、审计和自检摘要。

`guide` 不负责注册、登录、会话、角色权限主数据、成员档案主数据、站内通知主数据、首页配置、公告文章、专题页、真实资源下载、Cloudreve 分享票据、素材投稿、社区讨论、活动报名、考试判分、白名单审核、实时服务器状态采集、真实服务器运维、节点执行、文件管理、容器、终端、备份恢复或外部聊天记录同步。

`content` 继续拥有首页、公告、页面、专题和 SEO。`guide` 不反写 content 首页配置，不把指南复制成 content 文章。`resource` 继续拥有玩家资源和下载入口。`guide` 可以保存资源公开快照引用，但不能生成下载票据，不能保存 Cloudreve 分享密码，不能修改 resource 主数据。`server-status` 继续拥有实时状态和线路。`guide` 可以保存服务器地址说明和线路说明引用，但不能缓存实时在线人数，不能伪造健康状态。`notification` 继续拥有通知主数据。`guide` 只能保存投递结果摘要。`profile` 只提供作者、维护人或负责人公开快照。

外部交流入口第一版归入 `guide`。它只展示 Oopz、QQ群、游戏内聊天等渠道的用途、加入条件、规则和注意事项，不同步外部聊天记录，不做跨平台 SSO，不替代工单、举报、审核和运营记录。

### 数据归属

`guide` 拥有以下主数据：指南分类、指南文章、指南版本、章节目录、命令索引条目、外部渠道入口、规则版本索引、用户反馈、搜索摘要、幂等记录、指南审计日志和运行自检摘要。

`guide` 可以保存当前用户、作者、维护人、资源、线路和通知的安全快照。快照只用于展示、检索、审计和降级，不是来源模块主数据，不能作为权限、成员资格、资源下载或实时状态的最终判断。

浏览器请求体不得覆盖 `createdBy`、`updatedBy`、`submittedBy`、`reviewedBy`、`publishedBy`、`offlineBy`、`archivedBy`、`deletedBy`、`authorSnapshot`、`maintainerSnapshot`、`status`、`publishedAt`、`version`、`auditResult`、`notificationStatus`、`searchWeight` 等服务端可信字段。实现可以忽略这些字段，但生产实现推荐返回字段级校验错误。

### 基础路径与认证

公开接口使用 `/api/v1/guides` 前缀，不要求登录。公开接口只能返回 `PUBLISHED`、`PUBLIC`、未下架、未归档、未软删除、处于可见时间范围内且分类启用的数据。公开响应不得返回后台备注、审核意见、审计字段、通知结果、幂等键、内部引用失败堆栈、作者敏感字段、资源下载密钥或外部渠道管理凭据。

当前用户反馈接口使用 `/api/v1/guides` 下的文章反馈路径，要求登录。反馈只用于知识库维护队列，不替代 `community` 的工单、举报和讨论。

后台接口使用 `/api/v1/guides/admin` 前缀，全部要求登录。后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`。创建、修改、提交审核、审核、发布、下架、归档、软删除、版本恢复、分类维护、渠道维护和反馈处理要求 `ADMIN` 或 `OWNER`，其中审核通过、拒绝和要求修改可以由 `HELPER` 执行，便于协管参与知识库审核。审计列表和自检摘要只允许 `ADMIN` 或 `OWNER`。

`guide` 当前由 `portal-core-service:8134` 承载。历史原服务端口 `8127` 只作为对照记录，不再作为当前网关默认上游。`api-gateway` 必须以兼容方式保留 `/api/v1/guides` 路由，不能改变已有服务路径、认证方式、响应格式或测试。

### 前序服务兼容契约

`guide` 适配 `auth`。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions`、`status` 和可选 Minecraft 绑定。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得提交反馈或访问后台接口。auth 不可用返回 `46940`，auth 超时返回 `46941`，auth 字段或枚举不兼容返回 `46942`。

`guide` 通过 api-gateway 访问时，优先读取网关注入的可信身份头：`X-Beiming-Actor-User-Id`、`X-Beiming-Actor-Roles`、`X-Beiming-Actor-Permissions`、`X-Beiming-Actor-Minecraft-Id`、`X-Beiming-Actor-Minecraft-Uuid` 和 `X-Gateway-Internal-Request-Id`。P0 只有 `X-Gateway-Internal-Request-Id` 与当前 `X-Request-Id` 一致时，才接受这些 actor 头。浏览器伪造 actor 头必须忽略，不得覆盖 Bearer 认证结果。生产接入内部签名或 mTLS 前，自检摘要必须暴露 `GATEWAY_INTERNAL_SIGNATURE_NOT_ENABLED` 缺口。

`guide` 适配 `profile` 读取作者、维护人或负责人公开快照。profile 成员不存在、不可公开或状态不允许作为维护人时返回 `46950`。profile 超时返回 `46951`，字段或枚举不兼容返回 `46952`。profile 不可用时不得创建新的可信维护人快照；已发布指南公开读取可以使用旧快照并在后台详情标记 `snapshotStale=true`。

`guide` 适配 `notification` 投递审核拒绝、要求修改、发布提醒、下架提醒和反馈处理提醒。审核拒绝和要求修改是强制通知，通知失败返回 `46960` 或 `46961`，状态不得变化。发布、下架、归档、软删除和反馈处理为辅助通知，通知失败时主流程可以成功，但必须记录失败摘要。`guide` 不保存通知主数据、未读数、模板或外部渠道投递记录。

`guide` 适配 `resource` 读取玩家资源公开快照，用于客户端整合包、Java 环境、下载加速、地图包、材质包或规则文档说明。resource 不可用返回 `46970`，超时返回 `46971`，字段不兼容返回 `46972`。公开读取可以在资源不可用时返回指南正文并标记 `degraded=true`，不得生成下载票据或返回下载 URL。

`guide` 适配 `server-status` 读取公开线路说明快照，用于服务器地址和线路指南。server-status 不可用返回 `46980`，超时返回 `46981`，字段不兼容返回 `46982`。`guide` 只保存线路说明引用和地址文案，不保存实时在线人数、MOTD、延迟和健康结果。

`guide` 不要求 `content` 反向适配。首页或专题需要展示指南时，由前端或后续 content 兼容变更读取 guide 公开接口。`onboarding` 当前已有规则确认和 `guideRoute` 字段，guide 第一版不修改 onboarding。等 guide 自身闭环完成后，onboarding 引用 guide 规则版本必须作为单独兼容变更处理。

### 枚举

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

### 通用对象

#### GuideCategory

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

#### GuideMaintainerSnapshot

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

#### GuideTocNode

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `nodeId` | string | 是 | 目录节点 ID。 |
| `title` | string | 是 | 节点标题，1 到 80 位。 |
| `anchor` | string | 是 | 页面锚点，只允许小写字母、数字和短横线。 |
| `level` | integer | 是 | 1 到 4。 |
| `sortOrder` | integer | 是 | 同级排序。 |

#### GuideCommandEntry

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

#### GuideReferenceSnapshot

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

#### PublicGuideSummary

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

#### PublicGuideDetail

`PublicGuideDetail` 在 `PublicGuideSummary` 基础上补充 `body`、`toc`、`commandEntries`、`references`、`externalChannelRefs`、`visibleFrom`、`visibleUntil`、`createdAt`。公开详情不得返回后台备注、审核意见、审计字段、通知摘要、内部引用失败堆栈、幂等键或服务端可信字段。

#### AdminGuideArticle

后台指南视图包含公开字段、草稿正文、状态、可见性、目录、指令条目、引用快照、外部入口引用、外部入口 ID 快照、维护人快照、后台备注、审核意见、通知摘要、验证时间、复核时间、版本号、状态时间、操作者 ID、删除时间和生产化提示。后台详情可以返回引用降级摘要，但不得返回 token、分享密码、外部渠道管理凭据、完整请求头或异常堆栈。

#### GuideVersion

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

#### GuideExternalChannel

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

#### GuideFeedback

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

#### GuideSearchResult

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

#### GuideAuditLog

审计字段继承公共契约，允许补充 `guideId`、`categoryId`、`channelId`、`feedbackId`、`version`、`ruleVersion`、`idempotencyKey`、`stateFrom`、`stateTo`、`notificationStatus`、`profileSnapshotStatus`、`resourceReferenceStatus` 和 `serverStatusReferenceStatus`。审计日志不得通过 guide API 删除。

### guide 错误码

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

### 接口总览

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

### 公开接口

#### 指南首页摘要

`GET /api/v1/guides/home`

成功响应 HTTP `200`，`data` 至少包含 `featuredGuides`、`pinnedGuides`、`categories`、`latestUpdatedGuides`、`currentRule`、`externalChannels`、`degraded` 和 `degradeReasons`。

业务规则：只汇总公开可见指南、启用分类和启用外部渠道。单个资源引用或线路引用失败时，可以返回指南主体并标记局部降级，不能整页失败，不能返回后台字段。

#### 公开分类列表

`GET /api/v1/guides/categories`

查询参数包括 `type`、`audience` 和 `keyword`。成功响应 HTTP `200`，`data.items` 为启用且未归档分类，按 `sortOrder`、`name` 稳定排序。非法枚举返回 `40001`。分类公开响应不得返回审计、后台备注或内部引用数量。

#### 公开指南列表

`GET /api/v1/guides/articles`

查询参数包括 `page`、`pageSize`、`type`、`categoryId`、`tag`、`audience`、`keyword`、`pinned` 和 `sort`。`sort` 允许 `publishedAt_desc`、`updatedAt_desc`、`title_asc`、`verifiedAt_desc`、`pinned_desc`。成功响应 HTTP `200`，分页 `items` 为 `PublicGuideSummary[]`。

业务规则：公开列表只返回 `PUBLISHED`、`PUBLIC`、未下架、未归档、未软删除、分类启用且处于可见时间范围内的指南。`AUTHENTICATED`、`MEMBER_ONLY` 和 `ADMIN_ONLY` 不进入公开列表。分页必须按过滤后的全集计算 `total`，空页返回空数组，不回退第一页。

#### 公开指南详情

`GET /api/v1/guides/articles/{guideId}` 和 `GET /api/v1/guides/articles/by-slug/{slug}`

成功响应 HTTP `200`，`data` 为 `PublicGuideDetail`。指南不存在、不可公开、未发布、已下架、已归档、已删除或可见时间不匹配时返回 `43900` 或 `43912`，同一实现版本内必须固定。公开详情不得返回后台字段和服务端可信字段。

#### 指南搜索

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

#### 常用指令索引

`GET /api/v1/guides/commands`

查询参数包括 `page`、`pageSize`、`keyword`、`tag`、`guideId` 和 `sort`。`sort` 允许 `updatedAt_desc`、`command_asc`。成功响应分页 `items` 为 `GuideCommandEntry[]`。

业务规则：只返回来自公开已发布指南的指令条目。下架、归档、删除或非公开指南中的指令不出现。指令响应不得被用于后台权限判定，`permissionHint` 只作为玩家说明。

#### 外部交流入口

`GET /api/v1/guides/external-channels`

查询参数包括 `type`、`audience` 和 `keyword`。成功响应 HTTP `200`，`data.items` 为公开可见、`ENABLED`、未归档的 `GuideExternalChannel[]`，但必须移除 `adminNote`。入口可以返回公开 URL、群号脱敏提示或游戏内频道说明，不得返回管理 token、机器人 token、审核后台链接或外部聊天记录。

#### 当前规则版本

`GET /api/v1/guides/rules/current`

成功响应 HTTP `200`，`data` 为当前公开生效的 `SERVER_RULE` 指南详情和 `ruleVersion`。如果没有已发布规则版本，返回 `43900` 或空降级视图，具体实现固定并写入测试。当前规则只能来自 `PUBLISHED`、`PUBLIC`、未下架、未归档、未删除的指南。

#### 指定规则版本

`GET /api/v1/guides/rules/versions/{ruleVersion}`

成功响应 HTTP `200`，`data` 为指定规则版本对应的公开指南详情。不存在、未发布或不可公开返回 `43900` 或 `43913`。历史规则版本可以公开展示，但必须标记 `current=false`。

#### 提交指南反馈

`POST /api/v1/guides/articles/{guideId}/feedback`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `type` | string | 是 | 任一 `GuideFeedbackType`。 |
| `message` | string 或 null | 否 | 最多 1000 位。`OTHER`、`BROKEN_LINK`、`UNCLEAR_STEP`、`WRONG_COMMAND` 建议必填。 |
| `anchor` | string 或 null | 否 | 页面锚点，最多 120 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `GuideFeedback`。业务规则：必须登录。只能对公开可见指南提交反馈。反馈记录服务端当前指南版本、用户快照和请求编号。相同用户、相同指南、相同幂等键和相同请求体重复提交返回同一反馈；同键不同请求体返回 `43914`。反馈写入失败返回 `51900`，不得伪造成功。

### 后台指南接口

#### 后台指南列表

`GET /api/v1/guides/admin/articles`

查询参数包括 `page`、`pageSize`、`type`、`status`、`visibility`、`categoryId`、`tag`、`maintainerUserId`、`ruleVersion`、`expired`、`keyword` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`publishedAt_desc`、`verifiedAt_desc`、`title_asc`。成功响应分页 `items` 为 `AdminGuideArticle[]`。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。

#### 后台指南详情

`GET /api/v1/guides/admin/articles/{guideId}`

成功响应 HTTP `200`，`data` 为 `AdminGuideArticle`，允许返回版本摘要、反馈摘要和引用降级摘要。指南不存在返回 `43900`。后台详情不得返回 token、Cloudreve 分享密码、外部渠道管理凭据、完整通知正文或异常堆栈。

#### 创建指南

`POST /api/v1/guides/admin/articles`

请求字段包括 `type`、`slug`、`title`、`summary`、`body`、`categoryId`、`tags`、`audience`、`visibility`、`pinned`、`toc`、`commandEntries`、`references`、`externalChannelIds`、`maintainerMemberId`、`ruleVersion`、`visibleFrom`、`visibleUntil`、`verifiedAt`、`expiresAt`、`adminNote`、`reason` 和 `idempotencyKey`。

成功响应 HTTP `201`，`data` 为 `AdminGuideArticle`，默认状态为 `DRAFT`，初始版本为 `1`。`guideId` 由服务端生成且创建后不可变，旧 slug 被修改释放后再次创建同 slug 时必须生成新的 `guideId`，不得覆盖已有指南。slug 在未软删除指南中唯一，冲突返回 `43911`。分类不存在返回 `43901`。`SERVER_RULE` 类型必须提供 `ruleVersion`。非规则类型不得占用同一规则版本。审计失败返回 `51901`，不得创建指南。

#### 修改指南

`PATCH /api/v1/guides/admin/articles/{guideId}`

请求字段同创建指南，除 `reason` 必填外其余字段按需修改。`ARCHIVED` 和 `DELETED` 不允许修改主体字段。修改已发布指南必须保证公开读取不会返回半更新状态。保存成功创建新版本。审计失败时不得改变指南。

#### 提交审核

`PATCH /api/v1/guides/admin/articles/{guideId}/submit-review`

请求字段包括必填 `reason` 和可选 `idempotencyKey`。`DRAFT`、`REJECTED` 和 `NEEDS_CHANGES` 可流转为 `PENDING_REVIEW`。重复提交 `PENDING_REVIEW` 返回成功，保持幂等，不重复写审计。其他状态返回 `43910`。

#### 审核通过

`PATCH /api/v1/guides/admin/articles/{guideId}/approve`

请求字段包括 `reviewOpinion`、`reason` 和可选 `idempotencyKey`。`PENDING_REVIEW` 可流转为 `APPROVED`。重复审核已 `APPROVED` 返回成功。`HELPER` 可执行审核通过，但不能发布。审计失败不得改变状态。

#### 审核拒绝

`PATCH /api/v1/guides/admin/articles/{guideId}/reject`

请求字段包括 `reviewOpinion`、`reason` 和可选 `idempotencyKey`。`PENDING_REVIEW` 可流转为 `REJECTED`。审核拒绝必须通知创建者或维护人；强制通知失败时状态不变并返回 `46960` 或 `46961`。重复拒绝保持幂等。

#### 要求修改

`PATCH /api/v1/guides/admin/articles/{guideId}/request-changes`

请求字段包括 `reviewOpinion`、`publicComment`、`reason` 和可选 `idempotencyKey`。`PENDING_REVIEW` 可流转为 `NEEDS_CHANGES`。要求修改必须通知创建者或维护人；强制通知失败时状态不变。

#### 发布指南

`PATCH /api/v1/guides/admin/articles/{guideId}/publish`

请求字段包括必填 `reason` 和可选 `idempotencyKey`。`APPROVED` 和 `OFFLINE` 可流转为 `PUBLISHED`。发布写入或更新 `publishedAt`、`currentVersion` 和搜索摘要。`SERVER_RULE` 发布时 `ruleVersion` 必须唯一；若设为当前规则，旧当前规则自动变为历史规则但仍保持可读。搜索摘要写入失败返回 `51902`，不得发布。辅助通知失败不阻塞，但必须写审计摘要。

#### 下架、归档和软删除

`PATCH /api/v1/guides/admin/articles/{guideId}/offline` 允许 `PUBLISHED` 流转为 `OFFLINE`。重复下架保持幂等。

`PATCH /api/v1/guides/admin/articles/{guideId}/archive` 允许 `DRAFT`、`REJECTED`、`NEEDS_CHANGES` 和 `OFFLINE` 流转为 `ARCHIVED`。公开中的指南必须先下架再归档。

`PATCH /api/v1/guides/admin/articles/{guideId}/delete` 只做软删除，状态为 `DELETED`，写入 `deletedAt`。公开中的指南必须先下架再软删除。P0 不提供真实删除接口。

这些接口请求字段均包含必填 `reason` 和可选 `idempotencyKey`。审计失败时不得改变业务状态。

### 版本、分类、渠道、反馈、审计和自检

`GET /api/v1/guides/admin/articles/{guideId}/versions` 返回指南版本分页。`GET /api/v1/guides/admin/articles/{guideId}/versions/{version}` 返回指定版本。`PATCH /api/v1/guides/admin/articles/{guideId}/versions/{version}/restore` 用指定历史版本生成新版本；恢复必须原子应用历史快照中的可恢复字段，包括标题、摘要、正文、类型、slug、分类、标签、受众、可见性、置顶、目录、指令条目、外部入口 ID、规则版本、可见时间窗、验证时间、复核时间、后台备注和维护人快照。恢复不得覆盖 `guideId`、当前状态、当前版本号、创建人、发布时间、删除时间、版本摘要、反馈摘要和审计记录；成功后递增当前版本，写入 `RESTORED` 版本并记录 `restoredFromVersion`。`ARCHIVED` 和 `DELETED` 指南不得恢复；历史 slug 被其他未删除指南占用返回 `43911`，历史规则版本被其他指南占用返回 `43913`，历史分类不存在或已归档返回 `43901`，历史外部入口不存在或已归档返回 `43903`。审计失败、幂等冲突或任一校验失败时不得改变业务状态。

后台分类接口支持列表、创建、修改和归档。创建和修改字段包括 `name`、`slug`、`description`、`icon`、`sortOrder`、`enabled`、`reason` 和可选 `idempotencyKey`。仍被未归档指南引用的分类不能归档，返回 `43915`。归档后公开分类列表不再返回。

后台渠道接口支持列表、创建、修改、启用、禁用和归档。创建和修改字段包括 `type`、`name`、`slug`、`purpose`、`joinCondition`、`rules`、`entryUrl`、`entryHint`、`visibility`、`sortOrder`、`adminNote`、`reason` 和可选 `idempotencyKey`。公开入口不得保存管理 token。被未归档指南引用的渠道不能归档，返回 `43915`。禁用后公开入口列表不再返回。

后台反馈列表支持 `page`、`pageSize`、`guideId`、`type`、`status`、`actorUserId`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`、`resolvedAt_desc`。`PATCH /api/v1/guides/admin/feedback/{feedbackId}/resolve` 和 `/ignore` 请求字段包括 `resolutionNote`、`notifyUser`、`reason` 和可选 `idempotencyKey`。重复处理保持幂等。`notifyUser=true` 时通知失败不回滚反馈处理，但必须记录失败摘要。

`GET /api/v1/guides/admin/articles/{guideId}/audit-logs` 支持 `page`、`pageSize`、`action`、`actorUserId`、`result`、`from`、`to` 和 `sort`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 guide API 删除。

`GET /api/v1/guides/admin/ops/summary` 返回服务运行模式、端口、存储模式、auth/profile/notification/resource/server-status 适配模式、指南数量、已发布数量、规则版本数量、外部入口数量、反馈数量、待处理反馈数量、搜索摘要数量、审计数量、幂等记录数量、生产化缺口和最近审计时间。摘要不得返回 token、请求头、外部渠道 secret、Cloudreve 分享密码、后台备注、审核意见全文、通知正文、内部引用堆栈或异常堆栈。

### 状态、幂等和并发

指南状态流转为 `DRAFT` 到 `PENDING_REVIEW`，`PENDING_REVIEW` 到 `APPROVED`、`REJECTED` 或 `NEEDS_CHANGES`，`REJECTED` 和 `NEEDS_CHANGES` 可重新提交，`APPROVED` 可发布为 `PUBLISHED`，`PUBLISHED` 可下架为 `OFFLINE`，`OFFLINE` 可重新发布、归档或软删除，`DRAFT`、`REJECTED` 和 `NEEDS_CHANGES` 可归档或软删除，`ARCHIVED` 原则上不可恢复，`DELETED` 为软删除终态。

创建指南、修改指南、提交审核、审核、发布、下架、归档、软删除、版本恢复、分类维护、渠道维护、提交反馈和处理反馈支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果。相同幂等键搭配不同请求体返回 `43914`。请求体指纹必须基于结构化 JSON 规范化结果，嵌套对象按字段名递归排序，数组保留顺序，不能依赖浏览器字段顺序或 Java `Map.toString()`。

并发创建相同 slug、相同规则版本、相同分类 slug 或相同渠道 slug 时只能一个成功，其余返回冲突。发布规则版本时必须保证同时只有一个当前规则。公开读取允许读到更新前或更新后的完整版本，不能返回半更新对象。

### 审计要求

必须审计的动作包括创建指南、修改指南、提交审核、审核通过、审核拒绝、要求修改、发布、下架、归档、软删除、恢复版本、创建分类、修改分类、归档分类、创建渠道、修改渠道、启用渠道、禁用渠道、归档渠道、提交反馈、解决反馈、忽略反馈、通知失败、引用降级、搜索摘要写入失败和审计写入失败。

后台写操作必须记录 `reason`。审计字段继承公共契约。审计写入失败时，后台写操作和当前用户反馈写操作不得假装成功，必须返回 `51901` 或 `51900`，并保持业务数据不变。

审计响应必须脱敏，不得返回完整请求体、Authorization、Cookie、外部渠道管理 token、Cloudreve 分享密码、资源下载票据、服务器检测目标、异常堆栈或通知正文。

### 失败降级

公开首页、公开指南列表、详情、搜索、指令索引和外部入口读取在单个资源引用、线路引用或维护人快照不可用时可以局部降级，返回指南主体和 `degraded=true`。指南主数据存储不可用时不能伪造成功。

auth 是当前用户反馈和后台接口强依赖。auth 不可用、超时、用户状态不允许或上下文字段不兼容时，不得使用旧用户上下文继续写入。

profile 是创建维护人快照和发布责任人展示的强依赖。profile 失败时不得创建新的可信维护人快照。已发布指南可使用旧快照公开展示。

notification 强制投递失败时，审核拒绝和要求修改不得改变状态。辅助通知失败时主流程可成功，但必须保留失败摘要和审计记录。

resource 和 server-status 引用失败时不得影响指南正文公开读取，但引用摘要必须标记降级。任何情况下不得返回资源下载 URL、Cloudreve 分享密码、服务器检测目标、后台运维入口或实时服务器状态。

### 验收口径

`guide` API 文档按 `docs/contracts-guide.md` 独立存在，并由 `.local-docs/tests-guide.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`guide` 完成时必须满足以下条件：全部接口按本文档实现；公开接口不泄露后台字段、审计字段、通知结果、资源下载密钥、服务器检测目标和外部渠道管理凭据；当前用户反馈只能写入自己的反馈；后台接口按角色限制；指南分类、文章、版本、规则版本、指令索引、外部入口、反馈、搜索摘要、状态流转、幂等、审计、自检摘要、端口配置和前序服务适配都有自动化测试；`.local-docs/tests-guide.md` 中全部测试用例都有对应自动化验证；未实现时自动化测试必须先失败；实现后 guide 在 `portal-core-service:8134` 中全部测试通过；api-gateway `/api/v1/guides` 路由指向 `8134` 并测试通过；旧 `guide-service:8127` Maven 入口已退役且不得恢复；auth、profile、notification、content、server-status、resource、admin 和 material 回归测试通过；没有修改前序服务稳定接口；没有把 `.local-docs/` 提交到仓库；没有把首页配置、公告文章、真实资源下载、素材投稿、社区讨论、考试判分、白名单审核、实时状态采集、服务器文件管理、容器、终端、节点执行、备份恢复或外部聊天同步塞进 `guide`。

