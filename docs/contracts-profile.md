# 北冥官网 profile API 契约

版本：0.1

## 文档定位

本文档是 `profile` 微服务的正式 API 契约。后续 `notification`、`content`、`admin`、`onboarding`、`exam`、`whitelist`、`attendance`、`community` 等服务只能通过本文档定义的接口适配成员档案，不能直接读取或修改 `profile` 数据库。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、运维能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `profile` 模块自己的路径、字段、状态、权限、错误码、审计和验收口径。

`profile` 适配 `auth`，不要求 `auth` 反向适配 `profile`。`profile` 只能通过后端入口提供的认证上下文、`/api/v1/auth/me`、`/api/v1/auth/session/verify` 或测试环境 auth stub 读取当前用户摘要，不能直接读取 auth 数据表，不能修改 auth 用户状态。

## 职责边界

`profile` 负责成员公开档案、公开展示字段、Minecraft 展示信息快照、成员组、成员状态、加入时间、个人简介、成员事迹、代表作品快照、活动和贡献展示入口。

`profile` 不负责注册、登录、邀请码、会话、账号角色能力点、账号级 Minecraft 绑定验证、考试判分、白名单审核、通知投递、考勤积分计算、资源上传、社区内容审核或运维操作。

`auth` 拥有用户、会话、角色、能力点和账号级 Minecraft 绑定。`profile` 只保存创建或更新档案时取得的账号展示快照和 Minecraft 展示快照。账号绑定变化不会由 `auth` 直接写入 `profile`，后续如需同步必须通过正式事件或本文档新增接口。

## 基础路径与认证

所有接口默认使用 `/api/v1/profile` 前缀。

公开读取接口无需 `Authorization`，但只能返回公开字段，并且必须遵守统一响应、请求编号、分页和错误码。

当前用户接口必须使用已认证上下文，只能读取或维护当前认证用户自己的成员档案。认证上下文优先来自后端入口注入的网关可信身份头；没有完整网关可信上下文时，保留 `Authorization: Bearer <token>` 本地兼容路径。

后台接口统一使用 `/api/v1/profile/admin` 前缀。后台读取至少要求 `HELPER`、`ADMIN` 或 `OWNER`。后台写操作至少要求 `ADMIN` 或 `OWNER`。成员移除、归档、恢复公开、恢复状态等会影响成员资格或公开展示的操作必须携带 `reason` 并写入审计。

## 请求编号和输入边界

profile 必须接收或生成 `X-Request-Id`。客户端或网关传入的请求编号只允许 1 到 128 位字母、数字、下划线、短横线、点和冒号。缺失或空白时由 profile 生成请求编号，并在响应头和错误响应体 `requestId` 中保持一致。

当直连 profile 且 `X-Request-Id` 格式非法时，profile 返回 HTTP `400`、错误码 `40001`，`errors.field` 为 `X-Request-Id`，响应头和响应体使用服务端兜底请求编号，不得把非法请求编号写入审计。经 `api-gateway` 访问时，非法请求编号应由网关先按 `docs/contracts-api-gateway.md` 返回 `46205`，正常情况下不应到达 profile。

所有请求体中的时间字段必须是 ISO 8601 字符串。当前契约涉及 `joinedAt` 和 `happenedAt`。时间字段缺失时按各接口默认规则处理；时间字段存在但格式非法时必须返回 HTTP `400`、错误码 `40001`，不得落入 `51200` 内部错误。

所有列表接口的枚举筛选、排序参数和长度受限查询参数必须严格校验。`keyword` 超过 50 位、`sort` 不在接口列出的允许值内、`status` 或 `visibility` 不在允许枚举内时，profile 必须返回 HTTP `400`、错误码 `40001` 或分页类公共错误码，不得静默回退默认排序或忽略非法筛选。

## auth 兼容契约

profile 必须通过 `ProfileAuthContextProvider`、`AuthContextProvider` 或等价适配层读取 auth 信息。生产环境优先消费后端入口传入的已校验认证上下文，也可以调用 auth 正式 API。测试环境使用 auth stub。任何实现都不能导入 auth 的内存存储类、数据表实体、Repository 或测试种子实现。

当前请求认证上下文至少包含以下字段：`userId`、`displayName`、`roles`、`permissions`、`status`、`minecraftBinding`。`minecraftBinding` 字段结构必须兼容 `docs/contracts-auth.md` 中的 `MinecraftBinding`。

后台激活目标用户时，profile 必须通过 auth 适配层读取目标用户快照。目标用户快照至少包含 `id`、`displayName`、`roles`、`permissions`、`status` 和 `minecraftBinding`。客户端请求体不得传入并覆盖 `displayNameSnapshot`、`authUserStatusSnapshot`、`authRolesSnapshot` 或 `minecraftBinding` 作为可信来源；这些字段只能来自 auth 适配层返回值。若后续网关已经传入可信目标用户快照，profile 仍必须校验来源为服务端上下文，而不是浏览器请求体。

auth 用户状态为 `PENDING_PROFILE` 或 `ACTIVE` 时允许创建或激活成员档案。`DISABLED`、`BANNED`、`DELETED` 不允许激活。目标用户不存在返回 `43204`。目标用户状态不允许激活返回 `43215`。auth 不可用返回 `46200`，auth 调用超时返回 `46201`，auth 返回字段缺失或枚举不兼容返回 `46202`。

profile 不能修改 auth 用户状态、角色、权限或 Minecraft 绑定。profile 写接口完成后，auth 用户快照只允许作为 profile 本地快照保存，不得反写 auth。

## 网关可信认证上下文

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

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `MemberStatus` | `PENDING_ACTIVATION`、`ACTIVE`、`INACTIVE`、`SUSPENDED`、`REMOVED`、`ARCHIVED` | 成员档案状态。`PENDING_ACTIVATION` 表示账号存在但档案未正式激活，`ACTIVE` 表示正式成员，`REMOVED` 表示已离开或被移除白名单后的历史成员，`ARCHIVED` 表示档案归档。 |
| `ProfileVisibility` | `PUBLIC`、`PRIVATE` | 公开可见性。`PRIVATE` 不进入公开列表，公开详情返回不存在。 |
| `MilestoneType` | `JOINED`、`PROJECT`、`EVENT`、`AWARD`、`MANAGEMENT`、`OTHER` | 成员事迹类型。 |
| `WorkSnapshotType` | `BUILD`、`REDSTONE`、`FARM`、`ARTICLE`、`IMAGE`、`VIDEO`、`OTHER` | 代表作品快照类型。 |
| `ProfileAuditResult` | `SUCCESS`、`FAILED` | profile 审计结果。 |

## 通用对象

### MemberGroup

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

### PublicMemberSummary

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

### PublicMemberDetail

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

### AdminMemberProfile

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

### MemberMilestone

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

### MemberWorkSnapshot

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

## profile 错误码

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

## 接口总览

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

## 公开成员接口

### 公开成员列表

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

### 公开成员详情

`GET /api/v1/profile/members/{memberId}`

成功响应 HTTP `200`，`data` 为 `PublicMemberDetail`。

业务规则：只有公开可见且状态允许公开展示的成员档案可以访问。成员不存在返回 `43200`。成员存在但不可公开访问时返回 `43213`，实现也可以出于防枚举考虑返回 `43200`，但同一版本内必须保持一致并写入测试。

降级规则：公开详情不得因活动或贡献入口暂不可用而整体失败。`activitySummary` 和 `contributionSummary` 在依赖不可用时返回 `null`。

## 当前用户接口

### 当前用户成员档案

`GET /api/v1/profile/me`

成功响应 HTTP `200`，`data` 为 `AdminMemberProfile` 去除 `adminNote` 和审计字段后的当前用户视图。

业务规则：必须要求登录。只返回当前认证用户 `userId` 对应的成员档案。当前用户没有成员档案时返回 `43200`，不得伪造空档案成功。auth 上下文不可用返回 `46200` 或 `46201`。

### 当前用户维护公开资料

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

## 后台成员接口

### 后台成员列表

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

### 后台成员详情

`GET /api/v1/profile/admin/members/{memberId}`

成功响应 HTTP `200`，`data` 为 `AdminMemberProfile`。

资源不存在返回 `43200`。权限不足返回公共权限错误码。

### 创建或激活成员档案

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

### 后台修改成员档案

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

### 修改成员状态

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

## 后台成员组接口

### 成员组列表

`GET /api/v1/profile/admin/groups`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `includeArchived` | boolean | 否 | 默认 `false`。 |

成功响应 HTTP `200`，`data.items` 为 `MemberGroup[]`，按 `sortOrder` 升序排序。

### 创建成员组

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

### 修改成员组

`PATCH /api/v1/profile/admin/groups/{groupId}`

请求字段同创建成员组，`reason` 必填，其余字段按需修改。

成功响应 HTTP `200`，`data` 为 `MemberGroup`。成员组不存在返回 `43201`。名称冲突返回 `43001`。

审计要求：成功写入 `PROFILE_GROUP_UPDATED`。

### 归档成员组

`PATCH /api/v1/profile/admin/groups/{groupId}/archive`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，归档原因。 |

成功响应 HTTP `200`，`data` 为归档后的 `MemberGroup`。

业务规则：成员组不存在返回 `43201`。仍被未归档成员使用时返回 `43214`。重复归档同一成员组返回成功，保持幂等，不重复写审计。

审计要求：首次归档写入 `PROFILE_GROUP_ARCHIVED`。

## 后台事迹与作品接口

### 维护成员事迹

`PUT /api/v1/profile/admin/members/{memberId}/milestones`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `items` | array | 是 | 最多 50 条。每条包含 `id`、`type`、`title`、`description`、`happenedAt`、`publicVisible`、`sortOrder`。新建项 `id` 可为空。 |
| `reason` | string | 是 | 1 到 200 位，维护原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `AdminMemberProfile`。

业务规则：该接口采用整体替换语义。请求中的 `items` 即为成员事迹的完整新集合。不存在于请求中的旧事迹视为移除，但必须通过审计保留操作记录。成员不存在返回 `43200`。字段非法返回 `40001`。同一请求内 `sortOrder` 可以重复，但返回时必须按 `sortOrder`、`happenedAt` 稳定排序。

审计要求：成功写入 `PROFILE_MEMBER_MILESTONES_REPLACED`，记录新增、修改、移除数量和原因。

### 维护代表作品快照

`PUT /api/v1/profile/admin/members/{memberId}/work-snapshots`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `items` | array | 是 | 最多 30 条。每条包含 `id`、`type`、`title`、`summary`、`coverUrl`、`sourceModule`、`sourceId`、`publicVisible`、`sortOrder`。新建项 `id` 可为空。 |
| `reason` | string | 是 | 1 到 200 位，维护原因。 |

成功响应 HTTP `200`，`data` 为更新后的 `AdminMemberProfile`。

业务规则：该接口采用整体替换语义。P0 可手工维护快照，后续 `content`、`activity` 等模块接入时只能通过正式接口或事件维护快照，不能直接写 profile 数据库。成员不存在返回 `43200`。字段非法返回 `40001`。

审计要求：成功写入 `PROFILE_MEMBER_WORKS_REPLACED`，记录新增、修改、移除数量和原因。

## 审计接口

### 成员审计列表

`GET /api/v1/profile/admin/members/{memberId}/audit-logs`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |

成功响应 HTTP `200`，分页 `items` 使用公共审计字段，允许补充 `action`、`beforeState`、`afterState`、`reason`、`result`。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 可读成员详情，但不能读取审计列表。

业务规则：成员不存在返回 `43200`。审计日志不得通过 profile API 删除。

## 状态、幂等和并发

创建成员档案和创建成员组支持 `idempotencyKey`。同一操作者、同一幂等键、同一请求体重复提交时返回同一个结果。相同幂等键搭配不同请求体返回 `43002`。

幂等签名必须基于规范化后的请求语义生成，而不是基于 JSON 字段原始顺序。对象字段顺序不同但字段和值完全一致时，必须视为同一请求体；数组顺序仍然保留业务含义，不得排序。字段值不同、数组顺序不同或缺失字段不同，均视为不同请求体。

后台更新接口必须以服务端当前状态为准。状态流转和归档操作失败时不能写入部分变更。实现可使用版本号、更新时间或事务锁保证并发下同一 `userId`、Minecraft ID、Minecraft UUID、成员组名称不会产生重复主数据。

公开读取接口允许读到更新前或更新后的完整状态，但不能返回半更新对象。

## 审计要求

必须审计的动作包括当前用户维护公开资料、创建或激活成员档案、后台修改成员档案、修改成员状态、创建成员组、修改成员组、归档成员组、维护成员事迹和维护代表作品快照。

后台写操作必须记录 `reason`。审计字段继承公共契约。审计写入失败时，后台写操作和当前用户资料修改不得假装成功，必须返回 `51201` 或 `51200`，并保持业务数据不变。

profile 审计返回必须至少包含 `id`、`requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`sourceIp`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason` 和 `createdAt`。`paramsSummary` 只能保存脱敏摘要，不得保存完整请求体、`Authorization`、Cookie、邀请码、密码、Minecraft 验证凭据或其他秘密。

公开读取、当前用户读取、后台低风险读取不强制写审计。

## 失败降级

公开成员列表和公开成员详情失败时，前端按成员展示区局部降级。profile 不得返回伪造成功数据。

当前用户接口依赖认证上下文。认证失败、会话过期、用户禁用或 auth 调用失败时不得返回旧档案当作成功。

活动、贡献、作品来源模块不可用时，profile 公开详情可以返回已有快照和 `null` 摘要，不得因为后序模块未实现而阻塞 profile P0 闭环。

## 验收口径

`profile` API 文档按 `docs/contracts-profile.md` 独立存在，并由 `.local-docs/tests-profile.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级和审计要求。

`profile` 完成时必须满足以下条件：全部接口按本文档实现；公开接口不泄露后台字段和 auth 安全字段；当前用户接口只能访问当前用户自己的档案；后台接口按角色限制；创建或激活成员档案不直接读取 auth 数据库；受保护接口同时支持网关可信认证上下文和旧 Bearer 兼容路径；状态流转、成员组、事迹、作品快照、审计和网关认证上下文消费全部有自动化测试；`.local-docs/tests-profile.md` 中记录的全部测试用例最终通过；未实现时自动化测试必须先失败，不能跳过红灯验证。
