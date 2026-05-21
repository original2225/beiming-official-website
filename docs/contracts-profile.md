# 北冥官网 P0 成员档案契约

版本：0.1

## 文档定位

本文档定义 P0 阶段 `profile` 模块的职责边界、数据归属、接口路径、权限规则、状态流转、错误码、审计要求和验收口径。本文档依赖 `docs/contracts-common.md` 和 `docs/contracts-auth.md`，统一响应、分页、认证头、基础角色、能力点、时间格式和审计字段以公共契约为准。

`profile` 是成员公开档案的主数据服务。它只负责成员档案、成员组、成员状态、公开展示字段、成员事迹、代表作品、活动快照和贡献快照，不负责登录、会话、邀请码、白名单审核、考试判分、考勤积分和通知投递。

## 模块职责

`profile` 负责公开成员列表、公开成员详情、当前登录用户自己的成员档案、管理员维护成员档案、成员状态流转、成员组维护、成员档案公开开关、成员事迹和代表作品快照维护。

`profile` 不直接读取 `auth` 数据库。调用方必须带上 `Authorization: Bearer <token>`，后端入口或本服务的认证适配层负责向 `auth` 校验会话，并形成可信认证上下文。`profile` 只信任服务端认证结果，不信任前端提交的用户身份、角色或 Minecraft 身份。

## 数据归属

`profile` 拥有成员档案、成员组、成员公开字段、成员事迹、成员代表作品、成员活动快照和成员贡献快照。

`auth` 仍然拥有账号、角色、权限、会话和 Minecraft 身份绑定。`profile` 可以保存 `authUserId`、`usernameSnapshot`、`displayNameSnapshot`、`minecraftIdSnapshot`、`minecraftUuidSnapshot` 和 `avatarUrlSnapshot`，这些字段只用于展示和历史留痕，不能作为登录或权限判断来源。

## 成员状态

成员档案状态使用以下枚举。

| 状态 | 含义 |
| --- | --- |
| PENDING_ACTIVATION | 已创建但未正式激活 |
| ACTIVE | 正常成员 |
| INACTIVE | 暂不活跃 |
| REMOVED | 已移出成员 |
| BANNED | 已封禁 |
| ARCHIVED | 已归档 |

`ACTIVE` 且 `publicVisible` 为 `true` 的档案可以出现在公开成员列表。`PENDING_ACTIVATION`、`REMOVED`、`BANNED` 和 `ARCHIVED` 默认不公开展示。管理员可以查看全部状态。

## 成员组

成员组是 `profile` 的主数据，用于公开展示和后台管理，不等同于 `auth` 角色。默认成员组建议包括普通成员、建筑、红石、后期、管理组和访客转正成员。成员组的正式名称由后台维护，接口只承诺字符串 ID 和展示名。

## 成员档案对象

成员档案对外字段如下。

```json
{
  "id": "profile_001",
  "authUserId": "user_001",
  "usernameSnapshot": "player_name",
  "displayName": "北冥玩家",
  "minecraftId": "MinecraftName",
  "minecraftUuid": "00000000-0000-0000-0000-000000000000",
  "avatarUrl": "https://example.com/avatar.png",
  "skinUrl": "https://example.com/skin.png",
  "memberGroupId": "group_builder",
  "memberGroupName": "建筑组",
  "status": "ACTIVE",
  "publicVisible": true,
  "joinedAt": "2026-05-21T08:00:00Z",
  "bio": "喜欢大型建筑工程",
  "achievements": [
    {
      "id": "achievement_001",
      "title": "主城一期建设",
      "description": "参与主城地标建设",
      "occurredAt": "2026-05-21T08:00:00Z"
    }
  ],
  "works": [
    {
      "id": "work_001",
      "title": "北冥主城钟楼",
      "description": "主城公共建筑",
      "coverUrl": "https://example.com/work.png",
      "linkUrl": "https://example.com/detail",
      "sortOrder": 1
    }
  ],
  "activitySummary": "最近参与主城建设",
  "contributionSummary": "累计贡献 20 分",
  "createdAt": "2026-05-21T08:00:00Z",
  "updatedAt": "2026-05-21T08:00:00Z"
}
```

公开接口不得返回后台备注、审计摘要和非公开成员。后台接口可以返回完整维护字段，但不得返回任何 `auth` 密码、会话、邀请码或权限敏感数据。

## 接口总览

所有接口默认前缀为 `/api/v1/profile`。

| 方法 | 路径 | 认证 | 权限 | 风险 | 用途 |
| --- | --- | --- | --- | --- | --- |
| GET | `/members` | 否 | 无 | LOW | 公开成员列表 |
| GET | `/members/{profileId}` | 否 | 无 | LOW | 公开成员详情 |
| GET | `/me` | 是 | 登录用户 | LOW | 获取自己的成员档案 |
| PATCH | `/me` | 是 | 登录用户 | MEDIUM | 修改自己的公开资料字段 |
| GET | `/admin/members` | 是 | ADMIN 或 OWNER | MEDIUM | 后台成员列表 |
| POST | `/admin/members` | 是 | ADMIN 或 OWNER | MEDIUM | 创建或激活成员档案 |
| GET | `/admin/members/{profileId}` | 是 | ADMIN 或 OWNER | LOW | 后台成员详情 |
| PATCH | `/admin/members/{profileId}` | 是 | ADMIN 或 OWNER | MEDIUM | 修改成员档案 |
| PATCH | `/admin/members/{profileId}/status` | 是 | ADMIN 或 OWNER | HIGH | 修改成员状态 |
| POST | `/admin/groups` | 是 | ADMIN 或 OWNER | MEDIUM | 创建成员组 |
| GET | `/admin/groups` | 是 | ADMIN 或 OWNER | LOW | 成员组列表 |
| PATCH | `/admin/groups/{groupId}` | 是 | ADMIN 或 OWNER | MEDIUM | 修改成员组 |
| POST | `/internal/members/activate` | 是 | 服务间调用 | MEDIUM | 白名单通过后激活档案 |

## 公开成员列表

`GET /api/v1/profile/members`

查询参数支持 `page`、`pageSize`、`keyword`、`memberGroupId` 和 `status`。公开接口只允许筛选 `ACTIVE` 或不传 `status`，不允许通过公开接口查看非公开成员。默认按 `joinedAt` 倒序，`pageSize` 最大为 `100`。

成功响应使用公共分页格式，`items` 中只返回公开展示字段。

## 公开成员详情

`GET /api/v1/profile/members/{profileId}`

只有 `ACTIVE` 且 `publicVisible` 为 `true` 的成员档案可以被公开读取。不存在、非公开或状态不可公开时统一返回 `43000`，避免暴露后台状态。

## 当前用户成员档案

`GET /api/v1/profile/me`

该接口需要登录。服务端通过认证上下文中的 `authUserId` 查找成员档案。用户尚未拥有成员档案时返回 `43000`。响应可以包含自己的完整公开资料字段，但不能包含后台备注和审计数据。

## 当前用户修改资料

`PATCH /api/v1/profile/me`

本人只能修改 `displayName`、`avatarUrl`、`skinUrl`、`bio`、`publicVisible`、`achievements` 和 `works`。本人不能修改 `authUserId`、`minecraftId`、`minecraftUuid`、`memberGroupId`、`status`、`joinedAt`、活动快照和贡献快照。

请求体示例。

```json
{
  "displayName": "北冥玩家",
  "avatarUrl": "https://example.com/avatar.png",
  "skinUrl": "https://example.com/skin.png",
  "bio": "喜欢大型建筑工程",
  "publicVisible": true,
  "achievements": [
    {
      "title": "主城一期建设",
      "description": "参与主城地标建设",
      "occurredAt": "2026-05-21T08:00:00Z"
    }
  ],
  "works": [
    {
      "title": "北冥主城钟楼",
      "description": "主城公共建筑",
      "coverUrl": "https://example.com/work.png",
      "linkUrl": "https://example.com/detail",
      "sortOrder": 1
    }
  ]
}
```

字段校验失败返回 `40001`。成员档案不存在返回 `43000`。更新成功后写入中风险审计。

## 后台成员列表

`GET /api/v1/profile/admin/members`

查询参数支持 `page`、`pageSize`、`keyword`、`memberGroupId`、`status`、`publicVisible`、`joinedFrom` 和 `joinedTo`。该接口只允许 `ADMIN` 或 `OWNER` 访问，普通用户返回 `42001`，未登录返回 `41000`。

## 创建或激活成员档案

`POST /api/v1/profile/admin/members`

请求体如下。

```json
{
  "authUserId": "user_001",
  "usernameSnapshot": "player_name",
  "displayName": "北冥玩家",
  "minecraftId": "MinecraftName",
  "minecraftUuid": "00000000-0000-0000-0000-000000000000",
  "avatarUrl": "https://example.com/avatar.png",
  "memberGroupId": "group_builder",
  "status": "ACTIVE",
  "publicVisible": true,
  "joinedAt": "2026-05-21T08:00:00Z"
}
```

`authUserId` 在 `profile` 内必须唯一。重复创建返回 `43100`。`minecraftId` 和 `minecraftUuid` 在 `profile` 内必须唯一，冲突返回 `43101`。创建成功返回 HTTP `201`，并写入审计。

## 后台成员详情

`GET /api/v1/profile/admin/members/{profileId}`

后台详情返回完整维护字段，包括活动快照和贡献快照。成员不存在返回 `43000`。

## 后台修改成员档案

`PATCH /api/v1/profile/admin/members/{profileId}`

管理员可以修改 `displayName`、`avatarUrl`、`skinUrl`、`memberGroupId`、`publicVisible`、`joinedAt`、`bio`、`achievements`、`works`、`activitySummary` 和 `contributionSummary`。管理员不能通过该接口修改 `authUserId`。状态修改必须走 `/admin/members/{profileId}/status`。

修改成功后写入审计。成员组不存在返回 `43102`。Minecraft 身份冲突返回 `43101`。

## 修改成员状态

`PATCH /api/v1/profile/admin/members/{profileId}/status`

请求体如下。

```json
{
  "status": "INACTIVE",
  "reason": "连续一个月未参与服务器活动"
}
```

允许状态流转为 `PENDING_ACTIVATION` 到 `ACTIVE`，`ACTIVE` 到 `INACTIVE`、`REMOVED`、`BANNED` 或 `ARCHIVED`，`INACTIVE` 到 `ACTIVE`、`REMOVED` 或 `ARCHIVED`，`REMOVED` 到 `ARCHIVED`，`BANNED` 到 `ARCHIVED`。不允许从 `ARCHIVED` 恢复到活跃状态，违反流转返回 `43103`。该接口风险等级为 `HIGH`，必须写入包含原因的审计。

## 成员组接口

`POST /api/v1/profile/admin/groups` 创建成员组。请求体包含 `name`、`description`、`sortOrder` 和 `enabled`。组名不能为空，组名重复返回 `43104`。创建成功返回 HTTP `201`。

`GET /api/v1/profile/admin/groups` 返回全部成员组，默认按 `sortOrder` 升序。

`PATCH /api/v1/profile/admin/groups/{groupId}` 修改成员组。已经被成员使用的组不能禁用，冲突返回 `43105`。

## 服务间激活接口

`POST /api/v1/profile/internal/members/activate`

该接口供 `whitelist` 审核通过后调用。请求必须来自服务间认证上下文，P0 阶段可以由后端入口或受控内部 token 转发。请求体和后台创建成员档案一致，但 `status` 固定为 `ACTIVE`。重复调用同一个 `authUserId` 时应幂等返回已有档案，并更新快照字段。该接口必须写入审计，不能由前端直接调用。

## 模块错误码

`profile` 模块错误码使用 `43100-43199` 的资源状态冲突分段和 `53100-53199` 的服务端错误分段。

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| 43100 | 409 | 成员档案已存在 |
| 43101 | 409 | Minecraft 身份已被成员档案使用 |
| 43102 | 404 | 成员组不存在 |
| 43103 | 409 | 成员状态流转不允许 |
| 43104 | 409 | 成员组名称已存在 |
| 43105 | 409 | 成员组正在使用，不能禁用 |
| 53100 | 500 | 成员档案服务内部错误 |

通用未登录、会话无效、权限不足、字段校验失败、分页错误和资源不存在使用公共错误码。

## 审计要求

创建成员档案、后台修改成员档案、本人修改公开资料、状态修改、成员组创建、成员组修改和服务间激活都必须写入审计。审计字段至少包含请求编号、操作者、角色、来源 IP、目标类型、目标 ID、操作类型、风险等级、操作前状态、操作后状态、执行结果和失败原因。状态修改必须记录 `reason`。

## 失败降级

公开成员列表和详情失败时，前端可以局部展示成员暂不可用。后台成员维护、本人资料修改、状态流转和服务间激活不能假成功。`profile` 依赖认证上下文失败时必须返回认证或权限错误。

## 验收口径

P0 成员档案模块完成时，必须有自动化测试覆盖公开成员列表、公开成员详情、非公开成员不可见、当前用户读取自己的档案、当前用户只能修改允许字段、管理员创建成员档案、重复 `authUserId` 冲突、Minecraft 身份冲突、后台成员列表、后台修改成员档案、状态合法流转、状态非法流转、成员组创建、成员组重复、成员组正在使用不能禁用、未登录、普通用户访问后台被拒绝、资源不存在和统一响应格式。
