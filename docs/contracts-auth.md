# 北冥官网 auth API 契约

版本：0.1

## 文档定位

本文档是 `auth` 微服务的正式 API 契约。后续 `profile`、`notification`、`content`、`admin`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`ops-control` 等服务只能通过本文档定义的接口或后端入口提供的认证上下文适配 `auth`，不能直接读取 `auth` 数据库，也不能自行实现登录、会话或权限判断。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、运维能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `auth` 模块自己的路径、字段、状态、权限、错误码、审计和验收口径。

## 职责边界

`auth` 负责账号、凭证、登录会话、当前用户、角色、能力点、邀请码、密码重置、账号级 Minecraft 绑定和认证上下文输出。

`auth` 不负责成员公开档案、入服进度、考试、白名单审核、考勤积分、内容审核、通知投递、后台待办聚合、服务器状态展示或真实运维操作。账号级 Minecraft 绑定只表示某个账号绑定了 Minecraft 身份，成员主数据仍归后续 `profile`。

## 数据归属

`auth` 拥有以下主数据：用户、登录凭证、会话、角色能力点、邀请码、邀请码使用记录、密码重置令牌、Minecraft 绑定、安全日志和审计日志。

邀请码原始码不得完整明文长期存储。创建邀请码成功时可以返回原始码一次，之后只能返回前缀、类型、状态、使用次数、使用上限和过期时间。

密码不得明文存储。实现必须使用成熟密码哈希算法，例如 bcrypt 或 Argon2。退出登录、用户禁用、用户封禁、密码重置和角色降权后，相关会话必须能失效。

## 基础路径与认证

所有接口默认使用 `/api/v1/auth` 前缀。公开接口无需 `Authorization`，但仍需要统一响应、请求编号、字段校验和限流。登录后接口必须使用 `Authorization: Bearer <token>`。后台接口必须额外校验基础角色和能力点。

`auth` 返回的访问令牌可以是服务端会话令牌或签名令牌。无论采用哪种实现，服务端都必须能主动吊销会话。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `UserStatus` | `PENDING_PROFILE`、`ACTIVE`、`DISABLED`、`BANNED`、`DELETED` | 注册后默认 `PENDING_PROFILE`，完成账号基础资料后可进入 `ACTIVE`。`DELETED` 为软删除状态。 |
| `Role` | `OWNER`、`ADMIN`、`HELPER`、`USER` | 继承公共契约基础角色。 |
| `Permission` | `NODE_READ`、`NODE_WRITE`、`CONTAINER_OPERATE`、`VM_OPERATE`、`FILE_MANAGE`、`TERMINAL_ACCESS`、`HIGH_RISK_APPROVE` | 继承公共契约运维能力点，可继续用于后续 `ops-control`。 |
| `InvitationType` | `PLAYER`、`ADMIN` | 玩家邀请码默认绑定 `USER`。管理员邀请码可绑定 `ADMIN` 或 `HELPER`，只能由 `OWNER` 创建。 |
| `InvitationStatus` | `ACTIVE`、`DISABLED`、`EXPIRED`、`EXHAUSTED` | 根据禁用、过期时间和使用次数计算。 |
| `AuditResult` | `SUCCESS`、`FAILED` | 审计执行结果。 |

## 通用对象

### UserSummary

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

### SessionPayload

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `accessToken` | string | 是 | 访问令牌。 |
| `tokenType` | string | 是 | 固定为 `Bearer`。 |
| `expiresAt` | string | 是 | 会话过期时间。 |
| `user` | UserSummary | 是 | 当前用户摘要。 |

### MinecraftBinding

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `minecraftId` | string | 是 | Minecraft Java 版名称。 |
| `minecraftUuid` | string | 是 | Minecraft UUID，使用无连字符小写格式。 |
| `verifiedAt` | string | 是 | 绑定验证通过时间。 |
| `source` | string | 是 | 绑定来源，P0 固定为 `MANUAL_VERIFICATION`。 |

### InvitationSummary

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

## auth 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `41100` | 401 | 用户名或密码错误。 |
| `41101` | 401 | 用户已禁用，不能登录。 |
| `41102` | 401 | 用户已封禁，不能登录。 |
| `41103` | 401 | 会话已被主动吊销。 |
| `41104` | 401 | 密码重置令牌无效或过期。 |
| `41105` | 401 | 当前密码错误。 |
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

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 注册 | POST | `/api/v1/auth/register` | 否 | 无 | LOW |
| 登录 | POST | `/api/v1/auth/login` | 否 | 无 | LOW |
| 退出登录 | POST | `/api/v1/auth/logout` | 是 | 当前用户 | LOW |
| 当前用户 | GET | `/api/v1/auth/me` | 是 | 当前用户 | LOW |
| 会话校验 | GET | `/api/v1/auth/session/verify` | 是 | 当前用户 | LOW |
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

## 公开账号接口

### 注册

`POST /api/v1/auth/register`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `invitationCode` | string | 是 | 8 到 64 位，只允许字母、数字、短横线和下划线。 |
| `username` | string | 是 | 3 到 32 位，只允许字母、数字、下划线，大小写不敏感唯一。 |
| `password` | string | 是 | 10 到 128 位，必须包含字母和数字。 |
| `displayName` | string | 是 | 2 到 24 位，站内唯一。 |
| `idempotencyKey` | string | 否 | 同一客户端重试注册时使用，24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `SessionPayload`。

业务规则：注册必须在同一事务内完成邀请码校验、用户创建、角色能力点授予、邀请码使用记录写入、使用次数递增和会话创建。邀请码并发使用时必须只允许剩余次数内的请求成功。`PLAYER` 邀请码只能创建普通用户。`ADMIN` 邀请码不能通过普通管理员创建，使用后可授予 `ADMIN` 或 `HELPER`。`OWNER` 不能通过注册接口创建。

幂等规则：同一 `idempotencyKey`、同一邀请码、同一用户名和同一请求体重复提交时返回同一个注册结果。相同 `idempotencyKey` 搭配不同请求体返回 `43002`。

降级规则：注册失败不得创建半成品用户，不得消耗邀请码次数。登录凭证创建失败时整笔事务回滚。

审计要求：成功注册写入 `AUTH_REGISTER_SUCCESS`。邀请码异常、字段校验失败不强制审计。管理员邀请码使用成功必须额外写入 `AUTH_ADMIN_INVITATION_USED`。

### 登录

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

### 退出登录

`POST /api/v1/auth/logout`

请求体为空。成功响应 HTTP `200`，`data: null`。

业务规则：只吊销当前访问令牌对应会话。重复退出同一会话返回成功，保持幂等。

审计要求：首次成功吊销写入 `AUTH_LOGOUT_SUCCESS`。重复退出不重复写审计。

### 当前用户

`GET /api/v1/auth/me`

成功响应 HTTP `200`，`data` 为 `UserSummary`。

业务规则：会话无效、过期、被吊销或用户状态变为 `DISABLED`、`BANNED`、`DELETED` 时不得返回用户摘要，分别返回公共认证错误码或 auth 状态错误码。

降级规则：这是认证基础接口，失败时不得伪造游客态成功响应。

### 会话校验

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

## 密码重置接口

### 申请密码重置

`POST /api/v1/auth/password-reset/request`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `username` | string | 是 | 用户名。 |

成功响应 HTTP `200`，`data` 固定为 `null`。

业务规则：无论用户名是否存在，都返回成功，避免枚举账号。存在且状态允许重置的用户创建一次性重置令牌。P0 可以只落库令牌和审计，不要求接入邮件或短信。

限流规则：同一 IP、同一用户名维度需要限流。触发限流返回 `44102`。

审计要求：存在用户时写入 `AUTH_PASSWORD_RESET_REQUESTED`。

### 确认密码重置

`POST /api/v1/auth/password-reset/confirm`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `resetToken` | string | 是 | 一次性重置令牌。 |
| `newPassword` | string | 是 | 10 到 128 位，必须包含字母和数字。 |

成功响应 HTTP `200`，`data: null`。

业务规则：令牌不存在、过期或已使用返回 `41104`。成功后更新密码哈希，标记令牌已使用，并吊销该用户全部现有会话。新密码不能和当前密码相同，相同时返回 `43001`。

审计要求：成功写入 `AUTH_PASSWORD_RESET_CONFIRMED`，失败写入 `AUTH_PASSWORD_RESET_FAILED`。

## Minecraft 绑定接口

### 绑定 Minecraft 身份

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

### 解绑 Minecraft 身份

`DELETE /api/v1/auth/me/minecraft-binding`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，说明解绑原因。 |

成功响应 HTTP `200`，`data: null`。

业务规则：没有绑定返回 `43102`。解绑只清除账号级 Minecraft 绑定，不修改后续 `profile` 成员主数据。若后续服务需要同步，应通过正式事件或接口适配，不能由 `auth` 直接写其他服务数据库。

审计要求：成功写入 `AUTH_MINECRAFT_UNBOUND`。

## 后台用户接口

### 用户列表

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

### 用户详情

`GET /api/v1/auth/admin/users/{userId}`

成功响应 HTTP `200`，`data` 为 `UserSummary`，可额外包含 `securitySummary`。

资源不存在返回 `43100`。

### 修改用户基础信息和状态

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

### 修改角色和能力点

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

## 后台邀请码接口

### 邀请码列表

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

### 创建邀请码

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

### 禁用邀请码

`PATCH /api/v1/auth/admin/invitations/{invitationId}/disable`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，禁用原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `InvitationSummary`。

业务规则：已禁用的邀请码重复禁用返回成功，保持幂等。已过期或已用完的邀请码允许禁用，但状态展示按禁用优先。

权限规则：`ADMIN` 只能禁用自己创建的 `PLAYER` 邀请码。`OWNER` 可禁用所有邀请码。

审计要求：首次禁用写入 `AUTH_INVITATION_DISABLED`。重复禁用不重复写审计。

### 邀请码使用记录

`GET /api/v1/auth/admin/invitations/{invitationId}/usage-records`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |

成功响应 HTTP `200`，分页 `items` 包含 `id`、`invitationId`、`usedByUserId`、`usedByUsername`、`usedAt`、`sourceIp`、`requestId`。

权限规则：`ADMIN` 只能查看自己创建的 `PLAYER` 邀请码使用记录。`OWNER` 可查看全部使用记录。

## 状态流转

用户状态允许流转如下：`PENDING_PROFILE` 可流转为 `ACTIVE`、`DISABLED`、`BANNED`、`DELETED`；`ACTIVE` 可流转为 `DISABLED`、`BANNED`、`DELETED`；`DISABLED` 可恢复为 `ACTIVE`；`BANNED` 可恢复为 `ACTIVE`；`DELETED` 不可恢复。唯一 `OWNER` 不允许进入 `DISABLED`、`BANNED` 或 `DELETED`。

邀请码状态由字段计算。未禁用、未过期、使用次数未满时为 `ACTIVE`。手动禁用后为 `DISABLED`。超过 `expiresAt` 为 `EXPIRED`。`usedCount >= maxUses` 为 `EXHAUSTED`。状态冲突时展示优先级为 `DISABLED`、`EXPIRED`、`EXHAUSTED`、`ACTIVE`。

会话状态至少包含可用、过期和吊销。密码重置、用户禁用、用户封禁、用户软删除、角色能力点降权都必须吊销相关会话。

## 限流要求

注册、登录、密码重置申请、邀请码校验和敏感后台写操作必须限流。限流维度至少包含来源 IP，登录和密码重置还必须包含用户名维度。限流命中时返回对应 `44xxx` 错误码。限流不能消耗邀请码次数，不能创建用户，不能修改密码。

## 审计要求

必须审计的动作包括注册成功、管理员邀请码使用、登录成功、登录失败触发风控、退出、密码重置申请、密码重置确认、用户状态修改、角色能力点修改、邀请码创建、邀请码禁用、Minecraft 绑定和解绑。

审计字段继承公共契约。后台写操作必须记录 `reason`。审计失败时，后台写操作不得假装成功；登录成功、退出和普通注册可先完成主流程，但必须至少写安全日志，后续实现如果支持审计重试，需要记录补偿状态。

## 验收口径

`auth` API 文档存在且被测试文档完整引用。本文档列出的每个接口都有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级和审计要求。

注册、登录、当前用户、会话校验和退出形成最小账号闭环。后台用户管理、角色能力点、邀请码管理、密码重置和 Minecraft 绑定均按本文档实现。测试文档中的全部用例最终必须全部通过，且不能为了通过测试降低本文档、公共契约、需求文档或系统设计的要求。
