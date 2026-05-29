# 北冥官网 notification API 契约

版本：0.2

## 文档定位

本文档是 `notification` 微服务的正式 API 契约。后续 `content`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、`admin` 和 `ops-control` 只能通过本文档定义的接口投递或读取通知结果，不能在各自模块内自建通知主数据、未读数或模板系统。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `notification` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、降级、审计和验收口径。

`notification` 适配 `auth`，不要求 `auth` 反向适配 `notification`。它通过后端入口传入的认证上下文、`/api/v1/auth/me`、`/api/v1/auth/session/verify` 或测试环境 auth stub 读取当前用户、角色、能力点和目标用户快照。它不能直接读取 auth 数据表，不能修改 auth 用户状态，不能自行实现登录、会话或权限判断。

本轮补强参考成熟通知和网关生态的稳定做法。Firebase Cloud Messaging HTTP v1 把服务端发送请求限定在可信服务端凭据和短期访问令牌链路中；OneSignal 和 Courier 都强调通知创建重试必须使用幂等键，避免网络超时后重复发送；Novu 把工作流触发、订阅者和载荷分开处理，并支持用事务编号去重；OneSignal 的消息 API 还把目标受众、消息内容、调度和响应处理拆成清晰边界。notification 只吸收这些边界思路：服务端认证上下文来自入口层，创建类请求保持幂等，目标收件人先解析再投递，批量投递保持全有或全无，投递结果和用户读取状态分开维护。P0 不引入外部推送服务、真实渠道发送、动态受众规则或跨平台工作流引擎。

## 职责边界

`notification` 负责站内通知、收件人状态、未读数、已读状态、归档状态、通知模板、模板变量、投递记录、失败原因和通知审计。

`notification` 不负责判定考试是否通过，不负责白名单审核，不负责积分计算，不负责处罚逻辑，不负责内容审核，不负责运维高风险审批，不负责修改任何业务结果。业务模块只把已经发生的业务结果传给 `notification`，`notification` 只按契约落通知、维护收件人的读取状态和返回投递结果。

P0 只实现站内通知。邮件、短信、QQ、Oopz 和游戏内消息只保留渠道枚举和后续扩展位置，不进行真实发送。P0 写接口只允许 `IN_APP` 渠道；提交其他渠道返回字段校验错误。

## 数据归属

`notification` 拥有以下主数据：通知主体、收件人状态、模板、模板版本、模板变量定义、投递记录、幂等记录和通知审计日志。

`notification` 可以保存接收人展示名快照，用于通知列表展示。快照来自 auth 目标用户快照、后端入口可信上下文或受信服务端调用方，不来自浏览器可篡改字段。快照不是 auth 或 profile 主数据，不能用于权限判断。

`notification` 不直接依赖 `profile`。需要展示成员名、头像或 Minecraft 身份时，由调用方传入可信服务端快照，或后续通过正式 profile 接口扩展，不能读取 profile 数据表。

## 基础路径与认证

当前用户接口使用 `/api/v1/notifications/me` 前缀。当前用户接口全部要求登录，只能读取和维护当前认证用户自己的收件人状态，不能通过请求参数传入 `userId` 读取别人通知。

后台接口使用 `/api/v1/notifications/admin` 前缀。后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`。后台创建通知、按模板投递、创建模板、修改模板、启用模板和禁用模板要求 `ADMIN` 或 `OWNER`。模板和批量投递写操作必须携带 `reason` 并写审计。

供后续服务调用的投递能力在 P0 以后台受控 HTTP API 表达，即 `POST /api/v1/notifications/admin/messages` 和 `POST /api/v1/notifications/admin/messages/from-template`。后续如果改成服务间内部接口或消息队列，不能破坏本文档定义的请求语义、幂等语义、投递状态和审计要求。

## auth 兼容契约

当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。后台创建通知时，服务端必须对 `recipientUserIds` 逐个解析目标用户快照。目标用户快照至少包含 `id`、`displayName`、`roles`、`permissions` 和 `status`。

目标用户不存在返回 `43315`。目标用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不允许投递，返回 `43315`，不得为该用户创建收件人记录。auth 不可用返回 `46300`，auth 调用超时返回 `46301`，auth 返回字段缺失或枚举不兼容返回 `46302`。

批量投递采用全有或全无语义。只要任一收件人不存在、状态不可投递、auth 不可用、模板渲染失败、审计写入失败或存储写入失败，本次请求不得创建半成品通知、不得更新未读数、不得写入部分收件人成功状态。

浏览器请求体不得覆盖当前登录用户、当前用户角色、当前用户能力点、收件人读取状态、收件人归档状态、可信展示名快照或投递状态。

## 网关可信认证上下文

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

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `NotificationType` | `SYSTEM`、`AUDIT`、`WHITELIST`、`EXAM`、`CONTENT`、`RESOURCE`、`ATTENDANCE`、`COMMUNITY`、`ACTIVITY`、`OPS` | 通知类型，用于列表过滤和前端展示。 |
| `NotificationChannel` | `IN_APP`、`EMAIL`、`SMS`、`QQ`、`OOPZ`、`GAME` | 通知渠道。P0 写接口只允许 `IN_APP`。 |
| `RecipientStatus` | `UNREAD`、`READ`、`ARCHIVED` | 当前用户看到的收件人状态。归档是终态，默认从普通列表隐藏。 |
| `DeliveryStatus` | `PENDING`、`DELIVERED`、`FAILED`、`CANCELED` | 单个收件人的站内投递状态。P0 成功写入收件人记录即为 `DELIVERED`。 |
| `TemplateStatus` | `ENABLED`、`DISABLED` | 模板状态。禁用模板不可用于投递。 |
| `NotificationAuditResult` | `SUCCESS`、`FAILED` | notification 审计执行结果。 |

`sourceModule` 使用模块英文名，例如 `auth`、`profile`、`notification`、`content`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、`admin`、`ops-control`、`node-daemon`。P0 允许 `sourceModule` 为空，但后台创建通知时建议提供来源，便于后续追踪。

## 通用对象

### NotificationRecipientView

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

### AdminNotificationMessage

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

### AdminNotificationRecipient

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

### NotificationTemplate

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

### TemplateVariableDefinition

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `name` | string | 是 | 变量名，1 到 40 位，只允许字母、数字和下划线，必须以字母开头。 |
| `required` | boolean | 是 | 是否必填。 |
| `description` | string 或 null | 是 | 变量说明，最多 120 位。 |
| `example` | string 或 null | 是 | 示例值，最多 120 位。 |

### NotificationAuditLog

审计字段继承公共契约，允许补充 `notificationId`、`templateId`、`recipientUserIds`、`deliveryStatus`、`templateVersion`、`idempotencyKey` 和 `sourceModule`。通知审计日志不得通过 notification API 删除。

## notification 错误码

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

## 接口总览

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

## 当前用户接口

### 当前用户通知列表

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

### 当前用户未读数

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

### 当前用户通知详情

`GET /api/v1/notifications/me/{notificationId}`

成功响应 HTTP `200`，`data` 为 `NotificationRecipientView`。

业务规则：只能读取当前用户自己的收件人记录。通知不存在、已不属于当前用户或收件人记录不存在时返回 `43300`，不得暴露该通知是否属于其他用户。已归档通知允许通过详情接口读取，状态返回 `ARCHIVED`。

### 标记单条已读

`PATCH /api/v1/notifications/me/{notificationId}/read`

请求体为空。成功响应 HTTP `200`，`data` 为更新后的 `NotificationRecipientView`。

业务规则：只允许当前用户标记自己的未归档通知。`UNREAD` 流转为 `READ` 并写入 `readAt`。重复标记已读返回成功，保持 `readAt` 不变，不重复写审计。`ARCHIVED` 通知返回 `43311`。

幂等规则：重复调用同一已读通知保持幂等。

### 全部标记已读

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

### 归档单条通知

`PATCH /api/v1/notifications/me/{notificationId}/archive`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 否 | 当前用户归档原因，最多 200 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `NotificationRecipientView`。

业务规则：只允许当前用户归档自己的通知。归档后状态为 `ARCHIVED`，写入 `archivedAt`，默认不再出现在普通列表，未读数不再统计该通知。重复归档返回成功，保持幂等，不重复写审计。

## 后台通知接口

### 后台通知列表

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

### 后台通知详情

`GET /api/v1/notifications/admin/messages/{notificationId}`

成功响应 HTTP `200`，`data` 为 `AdminNotificationMessage`。通知不存在返回 `43300`。

### 后台创建站内通知

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

### 后台按模板创建通知

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

## 模板接口

### 模板列表

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

### 模板详情

`GET /api/v1/notifications/admin/templates/{templateId}`

成功响应 HTTP `200`，`data` 为 `NotificationTemplate`。模板不存在返回 `43301`。

### 模板预览

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

### 创建模板

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

### 修改模板

`PATCH /api/v1/notifications/admin/templates/{templateId}`

请求字段同创建模板，除 `reason` 必填外其余字段按需修改。`code` 可以修改，但仍必须唯一。

成功响应 HTTP `200`，`data` 为更新后的 `NotificationTemplate`。

业务规则：模板不存在返回 `43301`。模板修改成功后 `version` 加一，已创建通知的模板快照不受影响。字段非法或模板变量不一致返回 `40001` 或 `43313`。编码冲突返回 `43317`。

审计要求：成功写入 `NOTIFICATION_TEMPLATE_UPDATED`，记录变更前后摘要和原因。审计失败时不得改变模板。

### 禁用模板

`PATCH /api/v1/notifications/admin/templates/{templateId}/disable`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，禁用原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `NotificationTemplate`。

业务规则：重复禁用已禁用模板返回成功，保持幂等，不重复写审计。禁用后按模板投递返回 `43312`。

审计要求：首次禁用写入 `NOTIFICATION_TEMPLATE_DISABLED`。

### 启用模板

`PATCH /api/v1/notifications/admin/templates/{templateId}/enable`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，启用原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `NotificationTemplate`。

业务规则：重复启用已启用模板返回成功，保持幂等，不重复写审计。启用前仍需校验模板变量定义和模板内容一致，失败返回 `43313` 或 `43314`。

审计要求：首次启用写入 `NOTIFICATION_TEMPLATE_ENABLED`。

## 审计接口

### 通知审计列表

`GET /api/v1/notifications/admin/messages/{notificationId}/audit-logs`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |

成功响应 HTTP `200`，分页 `items` 为 `NotificationAuditLog[]`。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 可读后台通知列表和详情，但不能读取审计列表。通知不存在返回 `43300`。审计日志不得通过 notification API 删除。

## 运维自检接口

### notification 自检摘要

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
    "lastAuditAt": "2026-05-22T00:00:00Z",
    "warnings": [
      "P0_IN_MEMORY_STORAGE",
      "P0_AUTH_STUB"
    ]
  }
}
```

业务规则：自检摘要用于后台确认 notification 当前运行模式、数据规模、投递状态和生产化缺口。P0 `storageMode` 固定为 `IN_MEMORY`，`authMode` 固定为 `TEST_STUB`，`pendingExternalDeliveries` 固定为 `0`。摘要不得返回 token、请求头、用户敏感字段、通知正文、模板正文或审计原因。数据读取失败返回 `51300`，不得伪造健康。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。`HELPER`、`USER` 返回 `42001`。未登录返回 `41000`。

## 状态、幂等和并发

收件人状态流转为 `UNREAD` 到 `READ`，`UNREAD` 或 `READ` 到 `ARCHIVED`。`ARCHIVED` 为当前用户视角终态，不允许再标记已读。重复已读和重复归档保持幂等。

投递状态 P0 成功写入收件人记录即为 `DELIVERED`。`PENDING`、`FAILED` 和 `CANCELED` 保留给后续异步渠道和失败补偿。P0 任何创建接口必须全有或全无，不允许返回部分成功的 `FAILED` 收件人记录。

创建通知、按模板创建通知和创建模板支持 `idempotencyKey`。同一操作者、同一幂等键、同一请求体重复提交时返回同一个结果。相同幂等键搭配不同请求体返回 `43002`。

并发创建同一幂等键时只能创建一条通知或模板。并发标记已读、全部已读和归档时必须以服务端当前状态为准，不得重复增加未读数、不得把归档通知重新变为已读。

## 审计要求

必须审计的动作包括后台创建站内通知、后台按模板创建通知、创建模板、修改模板、禁用模板、启用模板、当前用户归档通知、批量标记已读、投递失败回滚和审计写入失败补偿记录。

后台写操作必须记录 `reason`。审计字段继承公共契约。审计写入失败时，后台写操作和模板写操作不得假装成功，必须返回 `51301` 或 `51300`，并保持业务数据不变。当前用户已读操作不强制写审计，归档操作建议写低风险审计或用户行为日志。

## 失败降级

当前用户通知列表、详情、未读数、已读和归档都依赖认证上下文。认证失败、会话过期、用户禁用、auth 不可用或 auth 超时时，不得返回旧通知、旧未读数或伪造成功。

后台创建通知依赖目标用户快照、审计和本地投递写入。任一依赖失败时必须全量回滚，不得产生半通知、半收件人或错误未读数。

模板渲染失败时不得创建通知。禁用模板不得用于投递。外部渠道在 P0 不真实发送，提交非 `IN_APP` 渠道时返回字段校验失败，不进入投递流程。

## 验收口径

`notification` API 文档按 `docs/contracts-notification.md` 独立存在，并由 `.local-docs/tests-notification.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级和审计要求。

`notification` 完成时必须满足以下条件：全部接口按本文档实现；当前用户接口只能访问当前用户自己的通知；未读数准确且失败时不伪造 0；后台接口按角色限制；创建通知和模板写操作全有或全无；模板变量校验、模板预览和渲染失败可测试；自检摘要能暴露当前运行模式但不泄露敏感数据；auth 适配不直接读取 auth 实现；受保护接口同时支持网关可信认证上下文和旧 Bearer 兼容路径；审计 actor、权限判断、当前用户隔离、未读数和目标收件人快照均以解析后的当前 actor 或目标用户快照为准；`.local-docs/tests-notification.md` 中全部测试用例都有对应自动化验证；未实现时自动化测试必须先失败；实现后 notification 全部测试通过；api-gateway、auth 和 profile 前序服务回归测试通过；没有修改前序服务稳定接口。
