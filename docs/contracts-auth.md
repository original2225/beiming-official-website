# 北冥官网 P0 账号权限与邀请码契约

版本：0.1

## 文档定位

本文档定义 P0 阶段 `auth` 模块的职责边界、数据归属、接口路径、权限规则、状态流转、错误码、审计要求和验收口径。本文档依赖 `docs/contracts-common.md`，所有响应格式、分页格式、认证方式、通用错误码、基础角色、能力点、审计字段和时间格式以公共契约为准。

`auth` 是系统认证与权限边界。其他模块不能自行实现登录逻辑、会话校验、角色判断或邀请码校验。其他模块需要当前用户、角色、能力点或 Minecraft 身份绑定状态时，必须通过 `auth` 暴露的接口或后端入口注入的认证上下文获取。

## 模块职责

`auth` 负责注册、登录、退出、当前用户、会话校验、密码重置、用户状态、基础角色、细粒度能力点、邀请码和 Minecraft 身份绑定。

`auth` 不负责成员公开档案、入服流程、考试、白名单审核、考勤积分、内容审核和运维操作执行。注册用户通过白名单审核后，由 `profile` 创建或激活成员档案，由 `attendance` 初始化积分。`auth` 可以保存 Minecraft 身份绑定字段，但成员展示主数据归 `profile`。

## 数据归属

`auth` 拥有用户、凭证、会话、角色、权限、邀请码、邀请码使用记录、密码重置令牌和 Minecraft 身份绑定记录。

用户快照字段可以被其他模块保存，例如昵称、头像和 Minecraft ID。快照字段只用于展示，不允许作为权限判断或主数据来源。

## 用户状态

用户状态使用以下枚举。

| 状态 | 含义 |
| --- | --- |
| ACTIVE | 正常 |
| PENDING_PROFILE | 已注册但资料未完善 |
| DISABLED | 已禁用 |
| BANNED | 已封禁 |
| DELETED | 已软删除 |

`DISABLED` 和 `BANNED` 用户不能登录。`DELETED` 用户不能登录，也不能被普通后台列表默认展示。恢复用户必须由具备用户管理权限的管理员执行，并写入审计。

## 邀请码类型和状态

邀请码类型使用 `PLAYER` 和 `ADMIN`。`PLAYER` 用于玩家注册，默认绑定 `USER` 角色。`ADMIN` 用于管理员或协管注册，只能由 `OWNER` 创建，并且必须显式指定绑定角色和能力点。

邀请码状态使用以下枚举。

| 状态 | 含义 |
| --- | --- |
| ACTIVE | 可使用 |
| DISABLED | 已禁用 |
| EXPIRED | 已过期 |
| EXHAUSTED | 使用次数已耗尽 |

邀请码必须记录创建人、用途说明、绑定角色、绑定能力点、最大使用次数、已使用次数、有效期、禁用状态和使用记录。邀请码不能明文存储完整原始码，数据库中只保存哈希摘要、显示前缀和必要的检索字段。

## 角色和权限规则

注册后的默认角色由邀请码决定。普通玩家邀请码只能授予 `USER`。管理员邀请码可以授予 `HELPER` 或 `ADMIN`。`OWNER` 账号不能通过普通注册接口创建，必须通过初始化流程或受控后台流程创建。

`ADMIN` 不能创建、修改或禁用 `OWNER`。`ADMIN` 不能创建管理员邀请码。`HELPER` 不能访问用户角色修改接口。普通 `USER` 不能访问后台接口。

细粒度能力点用于运维和高风险后台能力。能力点可以授予 `OWNER`、`ADMIN` 或特定 `HELPER`，但授予、移除和变更必须写入审计。

## 会话和认证

客户端使用 `Authorization: Bearer <token>` 调用需要登录的接口。令牌实现可以是签名令牌或不透明会话令牌，但对外契约只承诺 Bearer 认证方式。

登录成功后返回访问令牌、过期时间和当前用户摘要。退出登录必须使当前会话失效。用户禁用、封禁、密码重置成功或角色权限被降低时，服务端必须能让相关会话失效或在下一次会话校验时拒绝访问。

## 接口总览

所有接口默认前缀为 `/api/v1/auth`。

| 方法 | 路径 | 认证 | 权限 | 风险 | 用途 |
| --- | --- | --- | --- | --- | --- |
| POST | `/register` | 否 | 无 | LOW | 邀请码注册 |
| POST | `/login` | 否 | 无 | LOW | 账号登录 |
| POST | `/logout` | 是 | 登录用户 | LOW | 退出当前会话 |
| GET | `/me` | 是 | 登录用户 | LOW | 获取当前用户 |
| POST | `/session/verify` | 是 | 登录用户 | LOW | 校验当前会话 |
| POST | `/password-reset/request` | 否 | 无 | MEDIUM | 申请密码重置 |
| POST | `/password-reset/confirm` | 否 | 无 | MEDIUM | 确认密码重置 |
| GET | `/users` | 是 | ADMIN 或 OWNER | MEDIUM | 用户列表 |
| GET | `/users/{userId}` | 是 | ADMIN 或 OWNER，或本人 | LOW | 用户详情 |
| PATCH | `/users/{userId}` | 是 | ADMIN 或 OWNER，或本人受限修改 | MEDIUM | 修改用户资料或状态 |
| PATCH | `/users/{userId}/roles` | 是 | OWNER | HIGH | 修改角色和能力点 |
| GET | `/invites` | 是 | ADMIN 或 OWNER | MEDIUM | 邀请码列表 |
| POST | `/invites` | 是 | ADMIN 或 OWNER，管理员邀请码仅 OWNER | HIGH | 创建邀请码 |
| PATCH | `/invites/{inviteId}` | 是 | ADMIN 或 OWNER，管理员邀请码仅 OWNER | HIGH | 禁用或调整邀请码 |
| GET | `/invites/{inviteId}/uses` | 是 | ADMIN 或 OWNER | MEDIUM | 邀请码使用记录 |
| PUT | `/minecraft-binding` | 是 | 登录用户 | MEDIUM | 绑定或更新 Minecraft 身份 |
| DELETE | `/minecraft-binding` | 是 | 登录用户或 ADMIN | HIGH | 解绑 Minecraft 身份 |

## 注册

`POST /api/v1/auth/register`

请求体如下。

```json
{
  "inviteCode": "BM-PLAYER-EXAMPLE",
  "username": "player_name",
  "password": "example-password",
  "displayName": "北冥玩家",
  "email": "player@example.com"
}
```

成功响应如下。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "access-token",
    "expiresAt": "2026-05-21T08:00:00Z",
    "user": {
      "id": "user_001",
      "username": "player_name",
      "displayName": "北冥玩家",
      "role": "USER",
      "permissions": [],
      "status": "PENDING_PROFILE",
      "minecraftId": null
    }
  }
}
```

注册必须校验邀请码状态、有效期、剩余次数、绑定角色和禁用状态。注册成功后必须写入邀请码使用记录，并创建登录会话。用户名、邮箱和邀请码使用需要具备幂等和并发保护，不能出现超额使用邀请码。

## 登录

`POST /api/v1/auth/login`

请求体如下。

```json
{
  "username": "player_name",
  "password": "example-password"
}
```

成功响应如下。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "access-token",
    "expiresAt": "2026-05-21T08:00:00Z",
    "user": {
      "id": "user_001",
      "username": "player_name",
      "displayName": "北冥玩家",
      "role": "USER",
      "permissions": [],
      "status": "ACTIVE",
      "minecraftId": "MinecraftName"
    }
  }
}
```

登录必须校验密码哈希、用户状态和频率限制。登录失败不能暴露用户名是否存在。连续失败达到限制后返回通用限流错误或认证错误。

## 当前用户和会话校验

`GET /api/v1/auth/me` 返回当前用户摘要。`POST /api/v1/auth/session/verify` 用于后端入口、前端启动和跨模块调用校验登录状态。

当前用户摘要必须包含 `id`、`username`、`displayName`、`role`、`permissions`、`status`、`minecraftId`、`minecraftUuid` 和 `avatarUrl`。权限判断以后端解析结果为准，前端不能自行根据本地缓存放行后台功能。

## 用户列表

`GET /api/v1/auth/users`

查询参数支持 `page`、`pageSize`、`keyword`、`role`、`status` 和 `createdFrom`、`createdTo`。默认不返回 `DELETED` 用户。

响应使用公共分页格式，`items` 中的用户对象不得包含密码哈希、密码重置令牌、邀请码原始码、会话令牌或敏感凭证。

## 用户修改

`PATCH /api/v1/auth/users/{userId}`

本人只能修改 `displayName`、`email`、`avatarUrl` 等个人资料字段。`ADMIN` 可以修改普通用户和协管的状态与基础资料，但不能修改 `OWNER`，不能提升他人为 `OWNER`，不能授予运维能力点。`OWNER` 可以修改角色和能力点，但必须通过 `/users/{userId}/roles` 接口执行。

用户状态修改、封禁、禁用、恢复、角色修改和能力点修改必须写入审计。

## 角色和能力点修改

`PATCH /api/v1/auth/users/{userId}/roles`

请求体如下。

```json
{
  "role": "ADMIN",
  "permissions": [
    "NODE_READ",
    "HIGH_RISK_APPROVE"
  ],
  "reason": "负责 P0 运维控制台联调"
}
```

该接口风险等级为 `HIGH`，必须由 `OWNER` 调用，并写入审计。不能通过该接口修改自己的 `OWNER` 身份为更低角色。防止唯一 `OWNER` 被降级或禁用的规则必须在服务端实现。

## 邀请码创建

`POST /api/v1/auth/invites`

请求体如下。

```json
{
  "type": "PLAYER",
  "role": "USER",
  "permissions": [],
  "maxUses": 10,
  "expiresAt": "2026-06-21T00:00:00Z",
  "note": "P0 内测玩家注册"
}
```

成功响应如下。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "invite_001",
    "code": "BM-PLAYER-EXAMPLE",
    "type": "PLAYER",
    "role": "USER",
    "permissions": [],
    "maxUses": 10,
    "usedCount": 0,
    "status": "ACTIVE",
    "expiresAt": "2026-06-21T00:00:00Z"
  }
}
```

原始邀请码只在创建成功响应中返回一次。后续列表和详情只能返回显示前缀、类型、状态、使用次数和过期时间。管理员邀请码只能由 `OWNER` 创建。

## Minecraft 身份绑定

`PUT /api/v1/auth/minecraft-binding`

请求体如下。

```json
{
  "minecraftId": "MinecraftName",
  "minecraftUuid": "00000000-0000-0000-0000-000000000000"
}
```

绑定成功后，`auth` 保存 Minecraft 身份绑定状态。后续白名单、成员档案和入服流程可以读取该状态。Minecraft 身份冲突时返回状态冲突错误。解绑操作风险等级为 `HIGH`，需要写入审计。

## 密码重置

`POST /api/v1/auth/password-reset/request` 用于申请重置。接口不能暴露账号是否存在。`POST /api/v1/auth/password-reset/confirm` 用于提交重置令牌和新密码。

密码重置成功后，必须使该用户现有会话失效，并写入安全审计。重置令牌必须有有效期、使用次数限制和哈希存储。

## 模块错误码

`auth` 模块错误码使用 `41000-41999`、`42000-42999` 和 `44000-44999` 中的认证、权限和风控分段。

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| 41100 | 400 | 用户名或密码格式错误 |
| 41101 | 401 | 用户名或密码错误 |
| 41102 | 403 | 用户已禁用 |
| 41103 | 403 | 用户已封禁 |
| 41104 | 409 | 用户名已存在 |
| 41105 | 409 | 邮箱已存在 |
| 41200 | 400 | 邀请码格式错误 |
| 41201 | 404 | 邀请码不存在 |
| 41202 | 409 | 邀请码不可用 |
| 41203 | 409 | 邀请码已过期 |
| 41204 | 409 | 邀请码次数已用完 |
| 41205 | 403 | 管理员邀请码权限不足 |
| 41300 | 409 | Minecraft 身份已被绑定 |
| 41301 | 400 | Minecraft 身份格式错误 |
| 41400 | 400 | 密码重置令牌无效 |
| 41401 | 400 | 密码重置令牌已过期 |
| 42100 | 403 | 不能修改 OWNER 用户 |
| 42101 | 403 | 不能移除唯一 OWNER |
| 44100 | 429 | 登录尝试过于频繁 |
| 44101 | 429 | 注册尝试过于频繁 |
| 44102 | 429 | 密码重置尝试过于频繁 |

## 审计要求

注册、登录成功、退出、登录失败达到风控阈值、密码重置、用户禁用、用户封禁、用户恢复、角色修改、能力点修改、邀请码创建、邀请码禁用、管理员邀请码使用、Minecraft 身份绑定和解绑都必须留下审计或安全日志。

审计记录必须包含公共契约要求的字段。注册和登录类安全日志可以不记录完整请求体，但必须记录请求编号、来源 IP、用户标识摘要、结果和失败原因。

## 限流要求

注册、登录、密码重置、邀请码校验和敏感后台接口必须限流。P0 代码实现前需要在工程配置中明确默认限流值。正式接口契约只要求这些接口具备服务端限流能力，不能只依赖前端按钮禁用。

## 失败降级

`auth` 不提供假成功降级。认证、权限、注册、登录、密码重置、用户修改、角色修改和邀请码使用失败时，必须返回明确错误码。依赖 `auth` 的其他模块在认证不可用时，应拒绝需要登录或后台权限的操作。

公开页面如果无法获取当前用户，可以按游客状态展示。后台页面如果无法校验当前用户，必须回到登录或无权限状态。

## 验收口径

P0 账号权限模块进入实现前，必须确认本文档中的接口路径、请求字段、响应字段、角色规则、邀请码规则、用户状态、Minecraft 绑定、错误码、限流要求和审计要求已经被代码任务引用。

P0 账号权限模块完成时，注册、登录、退出、当前用户、会话校验、用户列表、用户修改、角色能力点修改、邀请码创建、邀请码禁用、邀请码使用记录、密码重置和 Minecraft 身份绑定都必须有可执行验证。没有自动化测试时，必须在 `.local-docs/` 留下本地验证记录，但不得提交该目录。
