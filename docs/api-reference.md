# 北冥官网 API 总文档

版本：1.1

本文档是当前仓库唯一保留的总 API 文档。当前后端是模块化单体，唯一后端 Maven 入口是 `backend/pom.xml`，本地联调默认入口是 `http://127.0.0.1:8135`，所有业务路径保持 `/api/v1/**` 原样。历史独立服务入口、历史端口和旧微服务目录不作为当前调用入口。

## 公共约定

所有 HTTP 接口使用 `/api/v1` 作为版本前缀。公开接口可以不带认证。需要登录的接口使用 `Authorization: Bearer <token>`。后台接口额外校验基础角色和业务权限点。运维接口额外校验细粒度能力点、风险等级、二次确认和审批状态。

成功响应统一为：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

错误响应统一为：

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
  "requestId": "req_202606150001"
}
```

分页请求统一使用 `page` 和 `pageSize`。`page` 从 `1` 开始，`pageSize` 默认 `20`，未特殊声明时最大 `100`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。

基础角色固定为 `OWNER`、`ADMIN`、`HELPER`、`USER`。运维能力点包括 `NODE_READ`、`NODE_WRITE`、`CONTAINER_OPERATE`、`VM_OPERATE`、`FILE_MANAGE`、`TERMINAL_ACCESS`、`HIGH_RISK_APPROVE`。基础角色不自动等于全部运维能力。

通用错误码范围：`40000-40999` 为请求错误，`41000-41999` 为认证与会话错误，`42000-42999` 为权限与风险控制错误，`43000-43999` 为资源不存在、状态冲突和幂等冲突，`44000-44999` 为限流和风控错误，`45000-45999` 为上传、文件和资源分发错误，`46000-46999` 为跨模块调用和外部依赖错误，`50000-59999` 为服务端错误。

PostgreSQL 是正式持久化数据库。所有涉及新增、更新、状态流转、审计、幂等或请求日志的自动化测试，必须使用 `SpringBootTest` 的 `RANDOM_PORT` 通过真实 HTTP 请求进入后端，不允许用 MockMvc 替代真实链路。每个成功写接口必须覆盖请求接收、后端处理请求、数据库写入、后端处理数据和响应返回，随后使用独立 SQL 查询 PostgreSQL 验证模块业务表、`app_audit_logs`、`app_idempotency_records` 和 `app_request_logs`，并在测试日志输出 `SQL evidence`。H2 只允许用于轻量单测，不作为正式数据库验收证据。

公共请求日志表记录 `requestId`、HTTP 方法、路径、操作者、来源 IP、响应码、结果和失败原因。公共审计表记录操作者、角色、权限点、来源 IP、请求编号、目标对象、操作、风险等级、原因、参数摘要、前后状态、结果和失败原因。公共幂等表记录操作者、作用域、幂等键、请求指纹、响应码和响应体。写接口中业务表写入、审计写入、幂等记录和请求日志必须同事务提交；审计或幂等写入失败必须阻断对应写操作。幂等键同一操作者、同一作用域、同一请求指纹返回原结果；同键不同指纹返回 `409/43002`，模块有更细错误码时以模块章节为准。

## 模块清单

| 模块 | 路径前缀 | 当前承载 | 路由数 |
| --- | --- | --- | ---: |
| `api-gateway` | `/api/v1/gateway` | `backend:8135` | 9 |
| `business-core` | `/api/v1/business-core` | `backend:8135` | 3 |
| `admission-core` | `/api/v1/admission-core` | `backend:8135` | 2 |
| `engagement-core` | `/api/v1/engagement-core` | `backend:8135` | 3 |
| `ops-core` | `/api/v1/ops-core` | `backend:8135` | 5 |
| `portal-core` | `/api/v1/portal-core` | `backend:8135` | 5 |
| `unified-backend` | `/api/v1/unified-backend` | `backend:8135` | 5 |
| `auth` | `/api/v1/auth` | `business-core` | 20 |
| `profile` | `/api/v1/profile` | `business-core` | 16 |
| `notification` | `/api/v1/notifications` | `business-core` | 19 |
| `content` | `/api/v1/content` | `business-core` | 55 |
| `server-status` | `/api/v1/server-status` | `business-core` | 25 |
| `resource` | `/api/v1/resources` | `business-core` | 29 |
| `admin` | `/api/v1/admin` | `business-core` | 10 |
| `onboarding` | `/api/v1/onboarding` | `admission-core` | 15 |
| `exam` | `/api/v1/exams` | `admission-core` | 28 |
| `whitelist` | `/api/v1/whitelist` | `admission-core` | 20 |
| `attendance` | `/api/v1/attendance` | `admission-core` | 21 |
| `community` | `/api/v1/community` | `engagement-core` | 64 |
| `activity` | `/api/v1/activity` | `engagement-core` | 41 |
| `calendar` | `/api/v1/calendar` | `engagement-core` | 21 |
| `changelog` | `/api/v1/changelog` | `engagement-core` | 23 |
| `ops-control` | `/api/v1/ops-control` | `ops-core` | 31 |
| `cloudreve-sync` | `/api/v1/cloudreve-sync` | `ops-core` | 16 |
| `backup-recovery` | `/api/v1/backup-recovery` | `ops-core` | 25 |
| `alerting` | `/api/v1/alerting` | `ops-core` | 24 |
| `plugin-integration` | `/api/v1/plugin-integration` | `ops-core` | 38 |
| `cross-platform-notification` | `/api/v1/cross-platform-notification` | `ops-core` | 36 |
| `ops-image-market` | `/api/v1/ops-image-market` | `ops-core` | 49 |
| `guide` | `/api/v1/guides` | `portal-core` | 41 |
| `material` | `/api/v1/materials` | `portal-core` | 33 |
| `online-map` | `/api/v1/online-map` | `portal-core` | 34 |

## 业务边界

`auth` 负责登录、注册、当前用户、会话、邀请码、角色权限和 Minecraft 绑定。其他模块不得自行实现登录逻辑。

登录入口为 `POST /api/v1/auth/login`，前端联调时仍通过统一后端基地址访问。

`auth` PostgreSQL 持久化验收覆盖用户、会话、角色、权限、邀请码、邀请码使用记录、Minecraft 绑定、密码重置凭证、审计、幂等和请求日志。注册、登录、退出、用户修改、角色权限修改、邀请码创建、邀请码禁用、会话撤销、密码修改、密码重置确认、Minecraft 绑定和解绑等写接口成功后，测试必须通过独立 SQL 查询验证对应 `auth_*` 业务表以及 `app_audit_logs`、`app_idempotency_records` 和 `app_request_logs`。密码不得以明文入库，邀请码原始码不得以可复用明文入库，session token 不得以可直接使用的明文入库。`auth` 对外字段、路径、响应格式、认证方式、错误码和权限语义不得因为 PostgreSQL 落地而改变。

`profile` 负责成员公开档案、成员组、成员状态、Minecraft 身份和公开展示字段。

`notification` 负责站内通知、未读数、已读状态、通知归档和通知模板。

`content` 负责公告、文章、页面内容、摄影作品、成员作品、服务器进度、专题和 SEO 配置。

`server-status` 只负责玩家可见的 Minecraft 服务器状态、线路、在线人数、延迟和历史快照，不执行启停或终端命令。

`resource` 负责玩家资源下载、资源分类、版本、Cloudreve 分享链接、下载权限和资源状态。它不负责后台服务器文件管理。

`admin` 负责后台聚合入口、待办队列、运营配置、数据看板和审计索引，不吞掉业务模块职责。

`onboarding` 负责新玩家入服流程状态。`exam` 负责题库、试卷、答题、自动判分和人工阅卷。`whitelist` 负责白名单申请、补充、撤回、审核和移除。`attendance` 负责考勤积分、积分流水、榜单和月度任务。

`community` 负责板块、帖子、评论、点赞、收藏、投票、举报、工单和处罚。`activity` 负责活动发布、报名、签到、结果和奖励。`calendar` 负责日程、维护窗口和工程节点。`changelog` 负责版本更新、插件变更、规则调整和维护记录。

`ops-control` 负责服务器与资源运维控制面，包括节点、资产、容器、虚拟机、Minecraft 实例、文件、日志、终端、监控、备份和高风险审批。真实服务器操作必须交给外部节点执行器。

`cloudreve-sync` 负责 Cloudreve provider、目录同步、文件快照、分享解析和失效降级。`backup-recovery` 负责备份域、策略、任务、备份点、校验、恢复演练和恢复申请。`alerting` 负责告警规则、事件、静默和订阅。`plugin-integration` 负责插件源、实例、事件、命令和同步任务。`cross-platform-notification` 负责外部渠道控制面和模拟投递。`ops-image-market` 负责镜像仓库、镜像版本和拉取任务。

`guide` 负责指南、规则、指令和外部交流入口。`material` 负责素材投稿、展示、精选、审核和授权。`online-map` 负责在线地图 provider、世界、图层、marker、区域、嵌入配置和健康快照。


## profile 模块接口

`profile` 由 `business-core` 承载，路径前缀为 `/api/v1/profile`，负责成员公开档案、成员组、成员状态、Minecraft 身份快照、成员里程碑和作品快照。公开读取接口不要求登录；当前用户接口要求 `Authorization: Bearer <token>` 或可信网关身份头；后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`；后台写接口要求 `ADMIN` 或 `OWNER`；审计读取接口要求 `ADMIN` 或 `OWNER`。`profile` 只能使用认证上下文、可信网关头和 auth 用户快照字段，不得直接读取 auth 数据库表。

成员状态包括 `PENDING_ACTIVATION`、`ACTIVE`、`INACTIVE`、`SUSPENDED`、`REMOVED` 和 `ARCHIVED`。公开列表和公开详情只展示 `visibility=PUBLIC` 且状态为 `ACTIVE`、`INACTIVE` 或 `SUSPENDED` 的成员。可见性包括 `PUBLIC` 和 `PRIVATE`。状态流转规则为：`PENDING_ACTIVATION` 可转 `ACTIVE`、`REMOVED`、`ARCHIVED`；`ACTIVE` 可转 `INACTIVE`、`SUSPENDED`、`REMOVED`、`ARCHIVED`；`INACTIVE` 可转 `ACTIVE`、`SUSPENDED`、`REMOVED`、`ARCHIVED`；`SUSPENDED` 可转 `ACTIVE`、`INACTIVE`、`REMOVED`、`ARCHIVED`；`REMOVED` 只能转 `ARCHIVED`；`ARCHIVED` 为终态。

公开成员接口包括 `GET /members` 和 `GET /members/{memberId}`。`GET /members` 支持 `page`、`pageSize`、`keyword`、`groupId`、`status` 和 `sort`，`sort` 可为 `joinedAt_desc`、`joinedAt_asc`、`updatedAt_desc` 和 `displayName_asc`，响应字段包括 `memberId`、`displayName`、`avatarUrl`、`minecraftId`、`minecraftUuid`、`skinUrl`、`group`、`status`、`joinedAt`、`bio` 摘要、`featuredWorkCount`、`milestoneCount` 和 `updatedAt`。`GET /members/{memberId}` 额外返回完整 `bio`、公开 `milestones`、公开 `workSnapshots`、`activitySummary`、`contributionSummary` 和 `createdAt`。成员不存在返回 `404/43200`，非公开档案返回 `409/43213`。

当前用户接口包括 `GET /me` 和 `PATCH /me`。`GET /me` 返回当前登录用户的成员档案，不返回 `adminNote`。`PATCH /me` 允许修改 `avatarUrl`、`skinUrl`、`bio` 和 `visibility`，必须带 `reason`，成功返回更新后的当前用户档案；禁止修改 `status`、`groupId`、`joinedAt`、`adminNote`、`userId`、`minecraftId`、`minecraftUuid` 和 `displayNameSnapshot`。已移除或已归档成员自助修改返回 `409/43212`。成功写入必须记录 `PROFILE_SELF_UPDATED` 审计。

后台成员接口包括 `GET /admin/members`、`GET /admin/members/{memberId}`、`POST /admin/members/activate`、`PATCH /admin/members/{memberId}`、`PATCH /admin/members/{memberId}/status`、`PUT /admin/members/{memberId}/milestones`、`PUT /admin/members/{memberId}/work-snapshots` 和 `GET /admin/members/{memberId}/audit-logs`。后台成员列表支持 `page`、`pageSize`、`keyword`、`groupId`、`status`、`visibility` 和 `sort`，`sort` 可为 `createdAt_desc`、`updatedAt_desc`、`joinedAt_desc` 和 `displayName_asc`。后台成员详情返回 `memberId`、`userId`、`displayNameSnapshot`、`authUserStatusSnapshot`、`authRolesSnapshot`、头像、Minecraft 快照、成员组、状态、可见性、加入时间、公开简介、`adminNote`、里程碑、作品快照和时间字段。

`POST /admin/members/activate` 请求字段包括 `userId`、`groupId`、`avatarUrl`、`skinUrl`、`visibility`、`joinedAt`、`bio`、`reason` 和 `idempotencyKey`，成功返回 `201` 和后台成员详情，并记录 `PROFILE_MEMBER_ACTIVATED`。用户已有成员档案返回 `409/43210`，成员组不存在返回 `404/43201`，Minecraft 身份冲突返回 `409/43211`，auth 用户状态不允许激活返回 `409/43215`。`PATCH /admin/members/{memberId}` 允许修改 `displayNameSnapshot`、`avatarUrl`、`minecraftId`、`minecraftUuid`、`skinUrl`、`groupId`、`joinedAt`、`bio`、`visibility` 和 `adminNote`，必须带 `reason`，成功记录 `PROFILE_MEMBER_UPDATED`。`PATCH /admin/members/{memberId}/status` 请求字段为 `status` 和 `reason`，成功记录 `PROFILE_MEMBER_STATUS_CHANGED`。

`PUT /admin/members/{memberId}/milestones` 请求字段为 `items` 和 `reason`，`items` 最多 50 条，字段包括 `id`、`type`、`title`、`description`、`happenedAt`、`publicVisible` 和 `sortOrder`，成功替换该成员全部里程碑并记录 `PROFILE_MEMBER_MILESTONES_REPLACED`。里程碑类型包括 `JOINED`、`PROJECT`、`EVENT`、`AWARD`、`MANAGEMENT` 和 `OTHER`。`PUT /admin/members/{memberId}/work-snapshots` 请求字段为 `items` 和 `reason`，`items` 最多 30 条，字段包括 `id`、`type`、`title`、`summary`、`coverUrl`、`sourceModule`、`sourceId`、`publicVisible` 和 `sortOrder`，成功替换该成员全部作品快照并记录 `PROFILE_MEMBER_WORKS_REPLACED`。作品类型包括 `BUILD`、`REDSTONE`、`FARM`、`ARTICLE`、`IMAGE`、`VIDEO` 和 `OTHER`。

成员组接口包括 `GET /admin/groups`、`POST /admin/groups`、`PATCH /admin/groups/{groupId}` 和 `PATCH /admin/groups/{groupId}/archive`。`GET /admin/groups` 支持 `includeArchived`。成员组字段包括 `id`、`name`、`description`、`color`、`sortOrder`、`archived`、`createdAt`、`updatedAt` 和 `archivedAt`。`POST /admin/groups` 请求字段包括 `name`、`description`、`color`、`sortOrder`、`reason` 和 `idempotencyKey`，成功返回 `201` 并记录 `PROFILE_GROUP_CREATED`。`PATCH /admin/groups/{groupId}` 允许修改 `name`、`description`、`color` 和 `sortOrder`，必须带 `reason`，成功记录 `PROFILE_GROUP_UPDATED`。`PATCH /admin/groups/{groupId}/archive` 必须带 `reason`，成功记录 `PROFILE_GROUP_ARCHIVED`；仍被非归档成员使用的成员组不能归档，返回 `409/43214`。

写接口幂等使用请求体内 `idempotencyKey`。同一操作者、同一作用域、同一幂等键和同一请求指纹必须返回原响应；同键不同指纹返回 `409/43002`。当前必须覆盖成员组创建的幂等重放和冲突；成员激活、成员修改、状态修改、成员组修改、成员组归档、里程碑替换和作品快照替换按同一规则落库。请求字段顺序不同但语义相同时应视为同一指纹。

审计接口为 `GET /admin/members/{memberId}/audit-logs`，支持 `page` 和 `pageSize`。审计字段至少包括 `id`、`requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`sourceIp`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason` 和 `createdAt`。审计记录不得泄露 `Authorization`、Cookie、原始 token 或敏感请求体。审计写入失败必须阻断对应写操作并返回 `500/51201`。

错误码保持现有语义：字段校验失败返回 `400/40001`，分页非法返回 `400/40002`，未登录返回 `401/41000`，会话无效返回 `401/41001`，token 格式错误返回 `401/41003`，角色不足返回 `403/42001`，普通冲突返回 `409/43001`，幂等冲突返回 `409/43002`，成员不存在返回 `404/43200`，成员组不存在返回 `404/43201`，Minecraft 身份冲突返回 `409/43211`，终态成员状态冲突返回 `409/43212`，档案不可公开展示返回 `409/43213`，成员组使用中返回 `409/43214`，auth 用户状态不允许激活返回 `409/43215`，认证依赖不可用、超时或结构不兼容分别返回 `502/46200`、`504/46201` 和 `502/46202`，模块内部错误返回 `500/51200`。

PostgreSQL 是 `profile` 正式持久化验收依据。成功写接口必须经 `SpringBootTest` 的 `RANDOM_PORT` 通过真实 HTTP 请求进入后端，覆盖请求接收、认证上下文解析、业务校验、内存响应模型更新、PostgreSQL 写入和响应返回。每个成功写接口必须用独立 SQL 查询验证 `profile_members`、`profile_member_groups`、`profile_member_milestones` 或 `profile_member_work_snapshots` 对应业务表，以及 `app_audit_logs` 和 `app_request_logs`；带 `idempotencyKey` 的接口还必须验证 `app_idempotency_records`。业务表写入、审计、幂等记录和请求日志必须在同一事务内提交，并在测试日志输出 `SQL evidence`。

## notification 模块接口

`notification` 由 `business-core` 承载，路径前缀为 `/api/v1/notifications`，负责站内通知、未读数、已读状态、归档、通知模板、通知审计和运维摘要。当前用户接口要求 `Authorization: Bearer <token>` 或可信网关身份头；后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`；后台写接口要求 `ADMIN` 或 `OWNER`；审计读取和运维摘要要求 `ADMIN` 或 `OWNER`。`notification` 只能使用认证上下文、可信网关头和 auth 目标用户快照，不得直接读取 auth、profile 或其他模块数据库。

状态值固定为 Recipient status `UNREAD`、`READ`、`ARCHIVED`，Delivery status `DELIVERED`、`FAILED`，Template status `ENABLED`、`DISABLED`，Channel `IN_APP`，Risk level `LOW`、`MEDIUM`、`HIGH`。旧内存态兼容筛选可继续识别历史 `PENDING`、`CANCELED` 和 `CRITICAL`，但 PostgreSQL 正式验收只接受前述固定值。通知类型包括 `SYSTEM`、`AUDIT`、`WHITELIST`、`EXAM`、`CONTENT`、`RESOURCE`、`ATTENDANCE`、`COMMUNITY`、`ACTIVITY` 和 `OPS`。

当前用户接口包括 `GET /me`、`GET /me/unread-count`、`GET /me/{notificationId}`、`PATCH /me/{notificationId}/read`、`PATCH /me/read-all` 和 `PATCH /me/{notificationId}/archive`。`GET /me` 支持 `page`、`pageSize`、`status`、`type`、`sourceModule`、`includeExpired` 和 `sort`，`sort` 可为 `createdAt_desc`、`createdAt_asc` 和 `readAt_desc`，默认不返回已归档通知和已过期通知。当前用户列表和详情字段包括 `notificationId`、`recipientUserId`、`recipientDisplayNameSnapshot`、`title`、`body`、`type`、`sourceModule`、`sourceId`、`riskLevel`、`actionUrl`、`status`、`deliveryStatus`、`failureReason`、`createdBy`、`createdAt`、`readAt`、`archivedAt` 和 `expiresAt`。`GET /me/unread-count` 返回 `unreadCount`。`PATCH /me/{notificationId}/read` 将本人收件人状态从 `UNREAD` 转为 `READ` 并写入 `readAt`；重复读取保持幂等返回当前 `READ` 状态；已归档通知读取返回 `409/43311`。`PATCH /me/read-all` 请求字段可带 `type` 和 `sourceModule`，批量把本人未过期、未读、未归档通知转为 `READ`，返回 `updatedCount`。`PATCH /me/{notificationId}/archive` 请求字段可带 `reason`，将本人收件人状态转为 `ARCHIVED` 并写入 `archivedAt`，重复归档返回当前归档状态。

后台消息接口包括 `GET /admin/messages`、`GET /admin/messages/{notificationId}`、`POST /admin/messages`、`POST /admin/messages/from-template` 和 `GET /admin/messages/{notificationId}/audit-logs`。`GET /admin/messages` 支持 `page`、`pageSize`、`keyword`、`type`、`sourceModule`、`recipientUserId`、`deliveryStatus`、`createdBy` 和 `sort`，`sort` 可为 `createdAt_desc`、`createdAt_asc` 和 `recipientTotal_desc`。后台消息字段包括当前用户消息字段、`channels`、`templateId`、`templateCode`、`templateVersion`、`variables`、`recipientTotal`、`deliveredTotal`、`failedTotal`、`recipients` 和 `expiresAt`，`recipients` 内字段包括 `recipientUserId`、`recipientDisplayNameSnapshot`、`status`、`deliveryStatus`、`failureReason`、`readAt`、`archivedAt` 和 `deliveredAt`。`POST /admin/messages` 请求字段包括 `recipientUserIds`、`title`、`body`、`type`、`channels`、`sourceModule`、`sourceId`、`riskLevel`、`actionUrl`、`expiresAt`、`reason` 和 `idempotencyKey`，成功返回 `201` 和后台消息详情，并记录 `NOTIFICATION_MESSAGE_CREATED` 审计。`POST /admin/messages/from-template` 请求字段包括 `templateCode`、`recipientUserIds`、`variables`、`channels`、`sourceModule`、`sourceId`、`riskLevel`、`actionUrl`、`expiresAt`、`reason` 和 `idempotencyKey`，必须基于启用模板渲染标题和正文，成功返回 `201` 和后台消息详情，并记录同一消息创建审计。

模板接口包括 `GET /admin/templates`、`GET /admin/templates/{templateId}`、`POST /admin/templates/preview`、`POST /admin/templates`、`PATCH /admin/templates/{templateId}`、`PATCH /admin/templates/{templateId}/disable` 和 `PATCH /admin/templates/{templateId}/enable`。模板列表支持 `page`、`pageSize`、`keyword`、`status`、`type` 和 `sort`，`sort` 可为 `updatedAt_desc`、`createdAt_desc` 和 `code_asc`。模板字段包括 `templateId`、`code`、`name`、`titleTemplate`、`bodyTemplate`、`variableDefinitions`、`type`、`channels`、`status`、`version`、`createdBy`、`createdAt`、`updatedBy`、`updatedAt` 和 `disabledAt`。`variableDefinitions` 字段包括 `name`、`required`、`description` 和 `example`。`POST /admin/templates/preview` 请求字段为 `templateCode` 和 `variables`，仅返回渲染预览，不创建消息。`POST /admin/templates` 请求字段包括 `code`、`name`、`titleTemplate`、`bodyTemplate`、`variableDefinitions`、`type`、`channels`、`reason` 和 `idempotencyKey`，成功返回 `201` 并记录 `NOTIFICATION_TEMPLATE_CREATED`。`PATCH /admin/templates/{templateId}` 可修改 `code`、`name`、`titleTemplate`、`bodyTemplate`、`variableDefinitions`、`type` 和 `channels`，必须带 `reason`，成功后 `version` 加一并记录 `NOTIFICATION_TEMPLATE_UPDATED`。禁用和启用接口必须带 `reason`，分别记录 `NOTIFICATION_TEMPLATE_DISABLED` 和 `NOTIFICATION_TEMPLATE_ENABLED`。

通知状态流转规则为：收件人 `UNREAD` 可转 `READ` 或 `ARCHIVED`；`READ` 可转 `ARCHIVED`；`ARCHIVED` 为终态，重复归档只返回当前状态。模板 `ENABLED` 可转 `DISABLED`，`DISABLED` 可转 `ENABLED`，启用前必须重新校验模板变量定义和渲染占位符。消息创建时每个收件人默认 `UNREAD`，站内投递成功为 `DELIVERED`，目标用户不存在、不可用或投递写入失败时不得伪造成功。

写接口幂等使用请求体内 `idempotencyKey`。`POST /admin/messages` 使用作用域 `notification.message.create`，`POST /admin/messages/from-template` 使用作用域 `notification.message.from-template`，`POST /admin/templates` 使用作用域 `notification.template.create`。同一操作者、同一作用域、同一幂等键和同一请求指纹必须返回原响应；同键不同指纹返回 `409/43002`；幂等记录过期时间为 24 小时。请求字段顺序不同但语义相同时应视为同一指纹。消息创建和模板创建必须在业务表、审计、幂等和请求日志同事务成功后才返回成功。

审计接口 `GET /admin/messages/{notificationId}/audit-logs` 支持 `page` 和 `pageSize`。审计字段至少包括 `id`、`requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`sourceIp`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason` 和 `createdAt`。审计记录不得泄露 `Authorization`、Cookie、原始 token、`idempotencyKey` 或通知正文敏感内容。审计写入失败必须阻断对应写操作并返回 `500/51301`。请求日志写入失败不得假装成功。

运维摘要接口为 `GET /admin/ops/summary`，返回 `service`、`storageMode`、`authMode`、`messagesTotal`、`templatesTotal`、`auditsTotal`、`recipientsTotal`、`unreadTotal`、`archivedTotal`、`deliveredTotal`、`failedTotal`、`pendingExternalDeliveries`、`idempotencyRecordsTotal`、`idempotencyRetentionHours`、`auditCompletenessMode`、`lastAuditAt` 和 `warnings`。摘要不得暴露 token、通知正文、模板正文、幂等键或真实外部凭据。

错误码保持现有语义：字段校验失败返回 `400/40001`，分页非法返回 `400/40002`，排序非法返回 `400/40003`，未登录返回 `401/41000`，会话无效返回 `401/41001`，token 格式错误返回 `401/41003`，角色不足返回 `403/42001`，普通冲突返回 `409/43001`，幂等冲突返回 `409/43002`，消息或通知不存在返回 `404/43300`，模板不存在返回 `404/43301`，收件人不存在返回 `404/43310`，通知过期或归档读取冲突返回 `409/43311`，模板禁用返回 `409/43312`，模板变量非法返回 `400/43313`，模板渲染失败返回 `400/43314`，收件用户不存在或不可用返回 `404/43315`，收件人集合非法返回 `400/43316`，模板代码冲突返回 `409/43317`，认证依赖不可用、超时或结构不兼容分别返回 `502/46300`、`504/46301` 和 `502/46302`，模块内部错误返回 `500/51300`，审计失败返回 `500/51301`，投递写入失败返回 `500/51302`。

失败降级规则为：认证上下文不可用、超时或结构不兼容时按认证依赖错误返回，不允许使用伪造用户继续写入；目标用户快照不可用时消息创建失败，不写消息、不写收件人、不写幂等成功记录；模板预览失败不创建消息；外部渠道当前不存在真实发送，`IN_APP` 之外的渠道必须被拒绝；读取接口只能返回当前用户可见数据，不能通过请求参数越权读取其他用户收件箱。

PostgreSQL 是 `notification` 正式持久化验收依据。新增、更新、状态流转、审计、幂等或请求日志相关自动化测试必须使用 `SpringBootTest` 的 `RANDOM_PORT` 和 Testcontainers PostgreSQL，通过真实 HTTP 请求进入后端，不允许用 MockMvc 替代真实链路。成功写接口必须覆盖 `POST /admin/messages`、`POST /admin/messages` 幂等重放、`POST /admin/messages` 同键不同指纹冲突、`POST /admin/messages/from-template`、`PATCH /me/{notificationId}/read`、`PATCH /me/read-all`、`PATCH /me/{notificationId}/archive`、`POST /admin/templates`、`POST /admin/templates` 幂等重放、`POST /admin/templates` 同键不同指纹冲突、`PATCH /admin/templates/{templateId}`、`PATCH /admin/templates/{templateId}/disable` 和 `PATCH /admin/templates/{templateId}/enable`。每个成功写接口后必须使用独立 SQL 查询验证 `notification_messages`、`notification_recipients`、`notification_templates`、`app_audit_logs` 和 `app_request_logs`；带 `idempotencyKey` 的接口还必须验证 `app_idempotency_records`；测试日志必须输出 `SQL evidence`。`notification_messages`、`notification_recipients`、`notification_templates`、`app_audit_logs`、`app_idempotency_records` 和 `app_request_logs` 必须在同一事务内提交，审计失败、幂等写入失败或请求日志写入失败都必须回滚业务写入。

## ops-image-market 模块接口

`ops-image-market` 由 `ops-core` 承载，路径前缀为 `/api/v1/ops-image-market`，负责镜像 provider、镜像目录、镜像版本、安全扫描摘要、兼容配置、部署模板、拉取计划、节点缓存快照和审计。除 `GET /health` 外均要求登录；读取接口要求 `HELPER`、`ADMIN` 或 `OWNER` 且具备 `NODE_READ`；写接口要求 `ADMIN` 或 `OWNER` 且具备 `NODE_WRITE`；provider 注册、provider 高风险更新、provider 启用、provider 归档、镜像或版本阻断、高风险拉取计划创建和拉取计划审批还要求 `HIGH_RISK_APPROVE` 或 `OWNER`。所有写接口支持 `idempotencyKey`，同一操作者、同一作用域、同一请求指纹返回原结果；同键不同指纹返回 `409/49712`。

健康和摘要接口包括 `GET /health` 和 `GET /admin/ops/summary`。摘要返回 provider、镜像、版本、模板、拉取计划、缓存快照、审计、幂等、测试控制、依赖适配模式和生产缺口。认证依赖不可用、超时或结构不兼容时分别返回 `502/47200`、`504/47201` 和 `502/47202`；测试控制头在本地测试控制关闭时必须被忽略。

provider 接口包括 `GET /admin/providers`、`GET /admin/providers/{providerId}`、`POST /admin/providers`、`PATCH /admin/providers/{providerId}`、`PATCH /admin/providers/{providerId}/enable`、`PATCH /admin/providers/{providerId}/disable`、`PATCH /admin/providers/{providerId}/archive` 和 `POST /admin/providers/{providerId}/health-refresh`。provider 字段包括 `providerId`、`displayName`、`registryType`、`status`、`healthStatus`、`endpointSummary`、`credentialRefSummary`、`allowedNamespaces`、`allowedSourceModules`、`allowedRiskLevels`、`syncPolicySummary`、`rateLimitSummary`、`lastHealthCheckedAt`、`degraded`、`degradeReasons`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。provider 状态包括 `DRAFT`、`ENABLED`、`DISABLED` 和 `ARCHIVED`。provider 不存在返回 `404/49700`，状态冲突返回 `409/49710`，名称或镜像冲突返回 `409/49711`，端点、仓库或标签非法返回 `400/49713`，provider 不可用返回 `409/49719`。

镜像、版本、安全扫描和兼容配置接口包括 `GET /admin/images`、`GET /admin/images/{imageId}`、`POST /admin/images`、`PATCH /admin/images/{imageId}`、`PATCH /admin/images/{imageId}/publish`、`PATCH /admin/images/{imageId}/block`、`PATCH /admin/images/{imageId}/archive`、`GET /admin/images/{imageId}/versions`、`GET /admin/versions/{imageVersionId}`、`POST /admin/images/{imageId}/versions`、`PATCH /admin/versions/{imageVersionId}/approve`、`PATCH /admin/versions/{imageVersionId}/deprecate`、`PATCH /admin/versions/{imageVersionId}/block`、`PATCH /admin/versions/{imageVersionId}/archive`、`GET /admin/scans`、`GET /admin/scans/{scanId}`、`POST /admin/versions/{imageVersionId}/scans`、`GET /admin/compatibility-profiles`、`GET /admin/compatibility-profiles/{profileId}`、`POST /admin/compatibility-profiles`、`PATCH /admin/compatibility-profiles/{profileId}`、`PATCH /admin/compatibility-profiles/{profileId}/enable`、`PATCH /admin/compatibility-profiles/{profileId}/disable` 和 `PATCH /admin/compatibility-profiles/{profileId}/archive`。镜像状态包括 `DRAFT`、`PUBLISHED`、`DEPRECATED`、`BLOCKED` 和 `ARCHIVED`；版本状态包括 `DISCOVERED`、`APPROVED`、`DEPRECATED`、`BLOCKED` 和 `ARCHIVED`；兼容配置状态包括 `DRAFT`、`ENABLED`、`DISABLED` 和 `ARCHIVED`。镜像不存在返回 `404/49701`，版本不存在返回 `404/49702`，兼容配置不存在返回 `404/49703`，扫描不存在返回 `404/49705`，扫描不可用或过期返回 `409/49715`，兼容性失败返回 `409/49716`，真实执行被阻断返回 `409/49717`，签名阻断返回 `409/49718`，扫描依赖不可用返回 `502/47220`。

模板、拉取计划和缓存快照接口包括 `GET /admin/templates`、`GET /admin/templates/{templateId}`、`POST /admin/templates`、`PATCH /admin/templates/{templateId}`、`PATCH /admin/templates/{templateId}/enable`、`PATCH /admin/templates/{templateId}/disable`、`PATCH /admin/templates/{templateId}/archive`、`GET /admin/pull-plans`、`GET /admin/pull-plans/{planId}`、`POST /admin/pull-plans`、`PATCH /admin/pull-plans/{planId}/approve`、`PATCH /admin/pull-plans/{planId}/cancel`、`GET /admin/cache-snapshots` 和 `GET /admin/cache-snapshots/{snapshotId}`。模板状态包括 `DRAFT`、`ENABLED`、`DISABLED` 和 `ARCHIVED`；拉取计划状态包括 `RISK_REVIEW_REQUIRED`、`SIMULATED_READY`、`EXECUTION_BLOCKED`、`CANCELED`、`FAILED` 和 `SUCCEEDED_SIMULATED`。模板不存在返回 `404/49704`，拉取计划不存在返回 `404/49706`，缓存快照不存在返回 `404/49707`，ops-control 依赖不可用返回 `502/47210`，拉取计划写入失败返回 `500/55903`。

审计接口为 `GET /admin/audit-logs`。审计筛选支持 `actorUserId`、`action`、`targetType`、`targetId`、`providerId`、`imageId`、`imageVersionId`、`templateId`、`planId`、`result`、`riskLevel`、`from`、`to` 和 `sort`。审计字段至少包括 `requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`sourceIp`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason`、`createdAt` 和目标对象引用字段。审计写入失败必须阻断对应写操作并返回 `500/55901`。涉及数据库新增或更新的自动化测试必须通过真实 HTTP 请求进入后端，覆盖请求接收、后端处理、数据库写入、后端处理结果和响应返回；完成后必须使用独立 SQL 查询验证 provider、镜像、版本、兼容配置、模板、扫描、拉取计划、审计和请求日志证据，并在测试日志输出 `SQL evidence`。

## cross-platform-notification 模块接口

`cross-platform-notification` 由 `ops-core` 承载，路径前缀为 `/api/v1/cross-platform-notification`，负责外部渠道 provider、能力点、模板映射、路由、模拟投递、投递尝试、收件人快照、审计和运维摘要。除 `GET /health` 外均要求登录；读取接口要求 `NODE_READ` 或 `HIGH_RISK_APPROVE`；provider 写入要求 `NODE_WRITE` 且操作者为 `ADMIN` 或 `OWNER`；模板映射写入要求 `NODE_WRITE` 且操作者为 `ADMIN` 或 `OWNER`；路由创建、测试和高风险变更要求 `HIGH_RISK_APPROVE` 且操作者为 `ADMIN` 或 `OWNER`；模拟投递和重试要求 `NODE_WRITE` 且操作者为 `ADMIN` 或 `OWNER`。所有写接口必须带 `idempotencyKey`，同一操作者、同一作用域、同一请求指纹返回原结果；同键不同指纹返回 `409/49962`。

健康和摘要接口包括 `GET /health` 和 `GET /admin/ops/summary`。摘要返回 provider、能力点、模板映射、路由、投递、尝试、收件人、审计、幂等、降级状态和生产缺口。认证依赖不可用、超时或结构不兼容时分别返回 `502/47150`、`504/47151` 和 `502/47152`；测试控制头在本地测试控制关闭时必须被忽略。

provider 接口包括 `GET /admin/providers`、`GET /admin/providers/{providerId}`、`POST /admin/providers`、`PATCH /admin/providers/{providerId}`、`PATCH /admin/providers/{providerId}/enable`、`PATCH /admin/providers/{providerId}/disable` 和 `PATCH /admin/providers/{providerId}/archive`。provider 字段包括 `providerId`、`channel`、`displayName`、`status`、`endpointSummary`、`credentialRefSummary`、`receiverPolicy`、`allowedSourceModules`、`allowedRiskLevels`、`rateLimitSummary`、`healthStatus`、`lastTestAt`、`lastDeliveryAt`、`degraded`、`degradeReasons`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。provider 不存在返回 `404/49950`，provider 冲突返回 `409/49961`，状态冲突返回 `409/49960`，安全端点非法返回 `400/49963`，接收方非法返回 `400/49964`，变量非法返回 `400/49965`，能力不匹配返回 `400/49966`。

能力点接口包括 `GET /admin/capabilities` 和 `GET /admin/capabilities/{capabilityId}`。能力点字段包括 `capabilityId`、`providerId`、`channel`、`supportsMarkdown`、`supportsRichBlocks`、`supportsImages`、`supportsThreads`、`supportsMentions`、`supportsDeliveryCallback`、`maxTitleLength`、`maxBodyLength`、`maxReceiversPerRequest`、`rateLimitSummary` 和 `updatedAt`。能力点不存在返回 `404/49956`。

模板映射接口包括 `GET /admin/template-mappings`、`GET /admin/template-mappings/{mappingId}`、`POST /admin/template-mappings`、`PATCH /admin/template-mappings/{mappingId}`、`PATCH /admin/template-mappings/{mappingId}/enable`、`PATCH /admin/template-mappings/{mappingId}/disable` 和 `PATCH /admin/template-mappings/{mappingId}/archive`。模板映射字段包括 `mappingId`、`sourceModule`、`sourceTemplateRef`、`providerId`、`channel`、`externalTemplateKey`、`allowedVariables`、`renderMode`、`fallbackTitleTemplate`、`fallbackBodyTemplate`、`status`、`version`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。模板映射状态包括 `DRAFT`、`ENABLED`、`DISABLED` 和 `ARCHIVED`。模板映射冲突返回 `409/49961`，状态冲突返回 `409/49960`。

路由和投递接口包括 `GET /admin/routes`、`GET /admin/routes/{routeId}`、`POST /admin/routes`、`PATCH /admin/routes/{routeId}`、`PATCH /admin/routes/{routeId}/enable`、`PATCH /admin/routes/{routeId}/disable`、`PATCH /admin/routes/{routeId}/archive`、`POST /admin/routes/{routeId}/test`、`POST /admin/deliveries`、`GET /admin/deliveries`、`GET /admin/deliveries/{deliveryId}`、`PATCH /admin/deliveries/{deliveryId}/retry`、`PATCH /admin/deliveries/{deliveryId}/cancel`、`GET /admin/attempts`、`GET /admin/attempts/{attemptId}`、`GET /admin/receivers` 和 `GET /admin/receivers/{receiverId}`。路由字段包括 `routeId`、`displayName`、`sourceModule`、`eventType`、`riskLevel`、`matchers`、`providerId`、`templateMappingId`、`receiverSummary`、`groupingPolicy`、`retryPolicySummary`、`status`、`lastTestDeliveryId`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。投递字段包括 `deliveryId`、`sourceModule`、`sourceId`、`eventType`、`riskLevel`、`routeId`、`providerId`、`channel`、`templateMappingId`、`receiverSummary`、`payloadSummary`、`status`、`attempts`、`lastAttemptAt`、`nextRetryAt`、`expiresAt`、`failureCode`、`failureSummary`、`createdBy`、`updatedBy`、`createdAt`、`updatedAt` 和 `receiverId`。尝试字段包括 `attemptId`、`deliveryId`、`providerId`、`channel`、`attemptNo`、`status`、`requestSummary`、`responseSummary`、`failureCode`、`failureSummary`、`startedAt`、`finishedAt` 和 `simulated`。收件人字段包括 `receiverId`、`providerId`、`channel`、`receiverType`、`sourceModule`、`displayName`、`targetRefSummary`、`verified`、`degraded`、`degradeReasons`、`lastUsedAt` 和 `createdAt`。路由不存在返回 `404/49952`，投递不存在返回 `404/49953`，尝试不存在返回 `404/49954`，收件人不存在返回 `404/49955`，路由冲突返回 `409/49961`，路由状态冲突返回 `409/49960`，真实外部投递被阻断返回 `409/49967`，重试窗口过期返回 `409/49968`。

审计接口为 `GET /admin/audit-logs`。审计筛选支持 `actorUserId`、`action`、`targetType`、`targetId`、`providerId`、`mappingId`、`routeId`、`deliveryId`、`attemptId`、`receiverId`、`sourceModule`、`result`、`riskLevel`、`from`、`to` 和 `sort`。审计字段至少包括 `requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason`、`providerId`、`mappingId`、`routeId`、`deliveryId`、`attemptId`、`receiverId`、`sourceModule`、`sourceIp`、`createdAt` 和 `requestIdRef`。审计写入失败必须阻断对应写操作并返回 `500/55801` 或 `500/55803`。涉及数据库新增或更新的自动化测试必须通过真实 HTTP 请求进入后端，完成后使用独立 SQL 查询验证 provider、模板映射、路由、投递、尝试、收件人、审计和请求日志证据，并在测试日志输出 `SQL evidence`。

## changelog 模块接口

`changelog` 由 `engagement-core` 承载，路径前缀为 `/api/v1/changelog`，拥有版本更新、插件变更、规则调整、资源包更新、地图更新和维护记录。公开读取接口不要求认证；`/me/**` 要求登录用户；`/admin/**` 要求 `HELPER`、`ADMIN` 或 `OWNER`，其中发布、下架、归档、删除和日历同步要求 `ADMIN` 或 `OWNER`。所有写接口必须带 `idempotencyKey`，并写入审计。幂等键同一操作者、同一作用域、同一请求指纹时返回原结果；同键不同指纹返回 `409/49312`。

公开接口包括 `GET /releases`、`GET /releases/{releaseIdOrSlug}`、`GET /versions/latest`、`GET /tags` 和 `GET /changes`。列表接口使用统一分页字段，支持 `keyword`、`type`、`visibility`、`impactLevel`、`minecraftVersion`、`tag`、`from`、`to` 和 `sort` 等过滤。公开详情只返回已发布且公开的版本，不返回内部备注、审核意见、提交时间、审核时间、下架时间和归档时间。安全版本的非公开变更项必须脱敏展示。

用户收藏接口包括 `GET /me/bookmarks`、`POST /me/releases/{releaseId}/bookmark` 和 `POST /me/releases/{releaseId}/unbookmark`。收藏只能作用于公开可见版本，返回 `bookmark` 和当前用户视角的 `release` 摘要。重复收藏保持 `ACTIVE`，取消收藏返回 `CANCELED`，并同步版本 `bookmarkCount`。收藏写入失败返回 `500/54903`，不得留下不一致的收藏计数。

后台版本接口包括 `GET /admin/releases`、`GET /admin/releases/{releaseId}`、`POST /admin/releases`、`PATCH /admin/releases/{releaseId}`、`POST /admin/releases/{releaseId}/submit`、`PATCH /admin/releases/{releaseId}/approve`、`PATCH /admin/releases/{releaseId}/reject`、`PATCH /admin/releases/{releaseId}/request-changes`、`PATCH /admin/releases/{releaseId}/publish`、`PATCH /admin/releases/{releaseId}/offline`、`PATCH /admin/releases/{releaseId}/archive` 和 `PATCH /admin/releases/{releaseId}/delete`。版本请求字段包括 `slug`、`versionName`、`title`、`summary`、`body`、`type`、`visibility`、`impactLevel`、`releasedAt`、`effectiveAt`、`minecraftVersion`、`pluginVersions`、`resourcePackVersions`、`mapVersion`、`groups`、`compatibilityNotes`、`knownIssues`、`rollbackNotes`、`securityPublicSummary`、`internalNote`、`relatedResourceIds`、`relatedServerInstanceIds`、`relatedContentId`、`reason` 和 `idempotencyKey`。响应字段包括版本基础字段、分组和变更项、关联资源快照、关联服务器快照、关联内容快照、日历引用、通知摘要、收藏计数、审核状态字段、创建更新时间和后台可见内部字段。状态流转为 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`PUBLISHED`、`OFFLINE`、`ARCHIVED`、`DELETED`，并允许 `REJECTED` 和 `NEEDS_CHANGES` 回到提交审核。删除必须带 `confirmText=DELETE_CHANGELOG_RELEASE`。

日历同步接口为 `POST /admin/releases/{releaseId}/calendar-sync`，请求字段包括 `mode`、`reason` 和 `idempotencyKey`。`mode=UPSERT_SNAPSHOT` 时返回 `syncStatus=SYNCED`，并写入 `calendarEvent.eventId`、`calendarEvent.syncStatus` 和 `lastSyncedAt`；其他模式返回 `SKIPPED`。日历依赖不可用、超时或结构不兼容时分别返回 `502/49140`、`504/49141` 和 `502/49142`。通知依赖失败不阻断发布或下架，但必须在 `notificationSummary.failure` 中返回降级摘要。

后台审计和运维接口包括 `GET /admin/audit-logs` 和 `GET /admin/ops/summary`。审计筛选支持 `actorUserId`、`action`、`targetType`、`targetId`、`releaseId`、`result`、`from`、`to` 和 `sort`。审计字段至少包括 `requestId`、`action`、`targetType`、`releaseId`、`targetId`、`actorUserId`、`actorRole`、`result`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`failureReason` 和 `createdAt`。验收时，涉及数据库新增或更新的自动化测试必须通过真实 HTTP 请求进入后端，完成后使用独立 SQL 查询验证版本、收藏、日历同步、审计和请求日志证据，并在测试日志输出 `SQL evidence`。

## ops-control 模块接口

`ops-control` 由 `ops-core` 承载，路径前缀为 `/api/v1/ops-control`，负责节点、资产、容器、虚拟机、Minecraft 实例、授权文件、日志查询、受控任务、审批和审计。所有接口都要求登录，读接口要求 `NODE_READ`，节点注册、启停和心跳要求 `NODE_WRITE`，容器和 Minecraft 操作要求 `CONTAINER_OPERATE`，虚拟机操作要求 `VM_OPERATE`，文件操作要求 `FILE_MANAGE`，终端命令和 Minecraft 命令要求 `TERMINAL_ACCESS`，高风险审批要求 `HIGH_RISK_APPROVE`。写接口使用 `idempotencyKey`，同一操作者、作用域和请求指纹返回原结果，同键不同指纹返回 `409/49412`。

资产和节点读取接口包括 `GET /overview`、`GET /assets`、`GET /assets/{assetId}`、`GET /nodes`、`GET /nodes/{nodeId}`、`GET /nodes/{nodeId}/capabilities`、`GET /nodes/{nodeId}/metrics/latest`、`GET /nodes/{nodeId}/containers`、`GET /nodes/{nodeId}/containers/{containerId}`、`GET /nodes/{nodeId}/vms`、`GET /nodes/{nodeId}/vms/{vmId}`、`GET /nodes/{nodeId}/minecraft-instances` 和 `GET /nodes/{nodeId}/minecraft-instances/{instanceId}`。列表接口使用统一分页和排序。节点详情包含最新指标和降级状态，运行时清单只展示脱敏快照，不泄露真实系统路径、token、命令行密钥或节点凭据。

节点写接口包括 `POST /nodes`、`PATCH /nodes/{nodeId}/disable`、`PATCH /nodes/{nodeId}/enable` 和 `POST /nodes/{nodeId}/heartbeat`。注册节点返回 `node` 和脱敏注册 token；禁用必须带 `confirmText=DISABLE_OPS_NODE`；心跳可更新节点状态、版本、能力点、指标、容器、虚拟机、Minecraft 实例和授权文件快照。审计失败时必须回滚节点创建或状态变更。节点不存在返回 `404/49401`，节点冲突返回 `409/49411`，确认文本不匹配返回 `409/49413`。

文件和日志接口包括 `GET /nodes/{nodeId}/files`、`POST /nodes/{nodeId}/files/read` 和 `POST /nodes/{nodeId}/logs/query`。文件路径必须以授权根内的 `/` 开头，禁止路径穿越、反斜杠和控制字符，非法路径返回 `409/49414`。文件读取和日志查询只返回脱敏摘要，并写入审计，不返回真实文件内容、真实日志密钥、真实系统路径或真实节点执行输出。

任务接口包括 `POST /tasks`、`GET /tasks`、`GET /tasks/{taskId}`、`PATCH /tasks/{taskId}/cancel` 和 `POST /tasks/{taskId}/node-result`。任务字段包括 `taskType`、`nodeId`、`targetType`、`targetId`、`params`、`reason`、`confirmText` 和 `idempotencyKey`。任务状态包括 `QUEUED`、`DISPATCHED`、`RUNNING`、`SUCCEEDED`、`FAILED`、`TIMEOUT`、`CANCELED` 和 `PENDING_APPROVAL`。高风险任务必须确认，更高风险任务生成审批。节点离线返回 `409/49415`，任务状态冲突返回 `409/49410`，目标不存在返回 `404/49400`。节点结果只能由具备 `NODE_WRITE` 的节点写入方回写，并必须保留 `nodeRequestId`、结果摘要、失败原因和审计记录。

审批、审计和运维摘要接口包括 `GET /approvals`、`GET /approvals/{approvalId}`、`PATCH /approvals/{approvalId}/approve`、`PATCH /approvals/{approvalId}/reject`、`GET /audit-logs` 和 `GET /ops/summary`。审批字段包括 `approvalId`、`taskId`、`status`、`riskLevel`、`requestedBy`、`approvedBy`、`reviewComment`、`createdAt`、`reviewedAt` 和 `expiresAt`。关键审计字段包括 `requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason` 和 `createdAt`。涉及数据库新增或更新的自动化测试必须通过真实 HTTP 请求进入后端，完成后使用独立 SQL 查询验证节点、运行时快照、任务、审批、审计和请求日志证据，并在测试日志输出 `SQL evidence`。

## cloudreve-sync 模块接口

`cloudreve-sync` 由 `ops-core` 承载，路径前缀为 `/api/v1/cloudreve-sync`，负责 Cloudreve provider 配置摘要、文件快照、分享快照、分享解析、同步任务、审计和运维摘要。所有非健康检查接口都要求登录，读取接口要求 `NODE_READ` 或 `FILE_MANAGE`，provider 写入要求 `NODE_WRITE` 且操作者为 `ADMIN` 或 `OWNER`，分享解析要求 `FILE_MANAGE` 且操作者为 `ADMIN` 或 `OWNER`，同步任务创建和取消要求 `FILE_MANAGE` 或 `NODE_WRITE`。所有写接口必须带 `idempotencyKey`，同一操作者、同一作用域、同一请求指纹返回原结果；同键不同指纹返回 `409/49712`。

健康和摘要接口包括 `GET /health` 和 `GET /ops/summary`。摘要返回 provider、文件、分享、任务、审计、幂等、配额、成本估算、降级状态和生产缺口。依赖认证不可用、超时或结构不兼容时分别返回 `502/46710`、`504/46711` 和 `502/46712`；本地测试控制关闭时，所有测试控制头必须被忽略。

provider 接口包括 `GET /providers`、`GET /providers/{providerId}`、`POST /providers`、`PATCH /providers/{providerId}`、`PATCH /providers/{providerId}/disable` 和 `PATCH /providers/{providerId}/enable`。provider 字段包括 `providerId`、`displayName`、`baseUrlSummary`、`authMode`、`status`、`capabilities`、`quotaTotalBytes`、`quotaUsedBytes`、`quotaUsagePercent`、`quotaWarningThresholdPercent`、`quotaStatus`、`estimatedMonthlyCostCents`、`pricingPlanSummary`、`opsAssetRef`、`lastHealthStatus`、`lastCheckedAt`、`lastSyncJobId`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。凭据只允许写入，不允许在响应、审计和日志中返回明文。provider 不存在返回 `404/49700`，重复 provider 或禁用 provider 执行同步返回 `409/49710`，Cloudreve 未授权返回 `502/46703`，ops-control 资产依赖不可用返回 `502/46730`。

文件和分享接口包括 `GET /files`、`GET /shares` 和 `POST /shares/resolve`。文件路径必须以 `/` 开头，禁止路径穿越、反斜杠和控制字符，非法路径返回 `400/49714`。分享解析可以通过 `fileId`、`path` 或 `shareUrl` 定位文件，成功后返回 provider、file、share、资源引用、可下载状态和降级摘要。Cloudreve 不可用且允许使用可用旧快照时返回 `stale=true` 和降级原因；没有可用旧快照时返回 `409/49713`。Cloudreve 超时、结构不兼容和未授权分别返回 `504/46701`、`502/46702` 和 `502/46703`。资源依赖不可用、超时和结构不兼容分别返回 `502/46720`、`504/46721` 和 `502/46722`。

同步任务接口包括 `POST /sync-jobs`、`GET /sync-jobs`、`GET /sync-jobs/{jobId}` 和 `PATCH /sync-jobs/{jobId}/cancel`。任务字段包括 `jobId`、`jobType`、`status`、`trigger`、`providerId`、`target`、`idempotencyKey`、`steps`、`resultSummary`、`failureReason`、`createdBy`、`createdAt`、`startedAt`、`finishedAt` 和 `updatedAt`。任务类型包括 `PROVIDER_HEALTH_CHECK`、`DIRECTORY_SYNC`、`SHARE_REFRESH` 和 `RESOURCE_LINK_VERIFY`。状态包括 `PENDING`、`RUNNING`、`SUCCEEDED`、`FAILED` 和 `CANCELLED`。只有 `PENDING` 和 `RUNNING` 可以取消，终态取消返回 `409/49711`。写入同步状态失败返回 `500/55302`。

审计接口为 `GET /audit-logs`。审计筛选支持 `actorUserId`、`providerId`、`fileId`、`shareSnapshotId`、`jobId`、`action`、`result`、`from`、`to` 和 `sort`。审计字段至少包括 `requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason`、`providerId`、`fileId`、`shareSnapshotId`、`jobId`、`dependencyStatus` 和 `createdAt`。审计写入失败必须阻断对应写操作并返回 `500/55301`。涉及数据库新增或更新的自动化测试必须通过真实 HTTP 请求进入后端，完成后使用独立 SQL 查询验证 provider、share、sync job、文件快照、审计和请求日志证据，并在测试日志输出 `SQL evidence`。

## backup-recovery 模块接口

`backup-recovery` 由 `ops-core` 承载，路径前缀为 `/api/v1/backup-recovery`，负责备份域、备份策略、备份任务、备份点、校验、恢复演练、恢复申请、审批摘要和审计。所有非健康检查接口都要求登录，读取接口要求 `NODE_READ` 或 `HIGH_RISK_APPROVE`，策略和任务写入要求 `NODE_WRITE` 且操作者为 `ADMIN` 或 `OWNER`，备份点校验、恢复演练、恢复申请和审批要求 `HIGH_RISK_APPROVE` 且操作者为 `ADMIN` 或 `OWNER`。所有写接口必须带 `idempotencyKey`，同一操作者、同一作用域、同一请求指纹返回原结果；同键不同指纹返回 `409/49812`。

健康、摘要和备份域接口包括 `GET /health`、`GET /ops/summary` 和 `GET /domains`。摘要返回域、策略、任务、备份点、已校验备份点、恢复演练、恢复申请、审计、幂等、最近成功和失败时间、降级状态和生产缺口。认证依赖不可用、超时或结构不兼容时分别返回 `502/46810`、`504/46811` 和 `502/46812`。本地测试控制关闭时，所有测试控制头必须被忽略。

策略接口包括 `GET /policies`、`GET /policies/{policyId}`、`POST /policies`、`PATCH /policies/{policyId}`、`PATCH /policies/{policyId}/enable` 和 `PATCH /policies/{policyId}/disable`。策略字段包括 `policyId`、`displayName`、`domains`、`scheduleSummary`、`retentionDays`、`minimumCopies`、`storageRef`、`encryptionMode`、`status`、`lastRunStatus`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。状态包括 `DRAFT`、`ENABLED`、`DISABLED` 和 `ARCHIVED`。策略不存在返回 `404/49801`，状态冲突返回 `409/49810`，名称冲突返回 `409/49811`。

备份任务和备份点接口包括 `POST /jobs`、`GET /jobs`、`GET /jobs/{jobId}`、`PATCH /jobs/{jobId}/cancel`、`GET /backup-points`、`GET /backup-points/{backupPointId}` 和 `POST /backup-points/{backupPointId}/verify`。任务字段包括 `jobId`、`policyId`、`trigger`、`status`、`domains`、`startedAt`、`finishedAt`、`resultSummary`、`failureReason`、`idempotencyKey`、`createdBy`、`createdAt` 和 `updatedAt`。成功任务必须生成 `backupPointId`，同步策略 `lastRunStatus`，并更新域最近备份点。任务状态包括 `PENDING`、`RUNNING`、`PENDING_APPROVAL`、`SUCCEEDED`、`FAILED`、`TIMEOUT` 和 `CANCELLED`。只有待执行或运行中任务可以取消，终态取消返回 `409/49810`。备份点字段包括 `backupPointId`、`policyId`、`jobId`、`domains`、`storageRef`、`checksumSummary`、`sizeBytes`、`encrypted`、`verified`、`verifiedAt`、`expiresAt`、`status` 和 `createdAt`。备份点不存在返回 `404/49803`，不可用备份点校验或恢复返回 `409/49813`。

恢复演练和恢复申请接口包括 `POST /restore-drills`、`GET /restore-drills`、`GET /restore-drills/{drillId}`、`POST /restore-requests`、`GET /restore-requests`、`GET /restore-requests/{restoreRequestId}`、`PATCH /restore-requests/{restoreRequestId}/approve` 和 `PATCH /restore-requests/{restoreRequestId}/reject`。恢复演练字段包括 `drillId`、`backupPointId`、`domains`、`status`、`validationSummary`、`startedAt`、`finishedAt`、`failureReason`、`createdBy` 和 `createdAt`。恢复申请字段包括 `restoreRequestId`、`backupPointId`、`domains`、`restoreMode`、`riskLevel`、`status`、`approvalSummary`、`requestedBy`、`approvedBy`、`reason`、`createdAt` 和 `updatedAt`。`SANDBOX_RESTORE` 必须先有通过的演练，否则返回 `409/49814`。申请确认文本必须为 `REQUEST_RESTORE_REVIEW`，审批确认文本必须为 `APPROVE_SIMULATED_RESTORE`。恢复审批禁止自审，状态冲突返回 `409/49810`。仓库内只允许模拟恢复，不允许生产写入。

审计接口为 `GET /audit-logs`。审计筛选支持 `actorUserId`、`policyId`、`jobId`、`backupPointId`、`drillId`、`restoreRequestId`、`action`、`result`、`riskLevel`、`from`、`to` 和 `sort`。审计字段至少包括 `requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason`、`policyId`、`jobId`、`backupPointId`、`drillId`、`restoreRequestId`、`dependencyStatus` 和 `createdAt`。审计写入失败必须阻断对应写操作并返回 `500/55401`。涉及数据库新增或更新的自动化测试必须通过真实 HTTP 请求进入后端，完成后使用独立 SQL 查询验证策略、任务、备份点、校验记录、恢复演练、恢复申请、审批摘要、审计和请求日志证据，并在测试日志输出 `SQL evidence`。

## alerting 模块接口

`alerting` 由 `ops-core` 承载，路径前缀为 `/api/v1/alerting`，负责告警来源、规则、评估、告警实例、静默、路由、投递、审计和运维摘要。除 `GET /health` 外均要求登录；读取接口要求 `NODE_READ` 或 `HIGH_RISK_APPROVE`；规则写入要求 `NODE_WRITE` 且操作者为 `ADMIN` 或 `OWNER`；规则评估要求 `NODE_READ` 且操作者为 `ADMIN` 或 `OWNER`；告警确认、关闭、静默创建和取消要求 `ADMIN` 或 `OWNER`，其中 `BLOCKER` 级告警关闭还要求 `HIGH_RISK_APPROVE` 或 `OWNER`；路由创建、修改和测试要求 `HIGH_RISK_APPROVE` 且操作者为 `ADMIN` 或 `OWNER`。所有写接口支持 `idempotencyKey`，同一操作者、同一作用域、同一请求指纹返回原结果；同键不同指纹返回 `409/49912`。

健康、摘要和来源接口包括 `GET /health`、`GET /ops/summary`、`GET /sources` 和 `GET /sources/{sourceId}`。摘要返回来源、规则、启用规则、告警、活跃静默、路由、投递、审计、幂等、降级状态和生产缺口。来源列表支持 `keyword`、`sourceService`、`sourceType`、`healthStatus`、`enabled`、`page`、`pageSize` 和 `sort`。认证依赖不可用、超时或结构不兼容时分别返回 `502/46920`、`504/46921` 和 `502/46922`；测试控制头在本地测试控制关闭时必须被忽略。

规则接口包括 `GET /rules`、`GET /rules/{ruleId}`、`POST /rules`、`PATCH /rules/{ruleId}`、`PATCH /rules/{ruleId}/enable`、`PATCH /rules/{ruleId}/disable` 和 `POST /rules/{ruleId}/evaluate`。规则字段包括 `ruleId`、`displayName`、`sourceService`、`sourceType`、`severity`、`labels`、`conditionType`、`conditionSummary`、`evaluationWindowSeconds`、`forDurationSeconds`、`dedupeKeyTemplate`、`routeId`、`runbookUrl`、`status`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。规则状态包括 `DRAFT`、`ENABLED`、`DISABLED` 和 `ARCHIVED`。评估请求字段包括 `sourceSnapshot`、`dryRun`、`reason` 和 `idempotencyKey`；启用规则评估命中时必须写入 evaluation，非 dry run 且无可复用未关闭告警时写入 alert，并根据路由写入 delivery 或记录未投递原因。规则不存在返回 `404/49901`，路由不存在返回 `404/49904`，规则冲突或状态冲突返回 `409/49910`，条件非法返回 `400/49911`。来源依赖不可用、超时或结构不兼容时分别返回 `502/46910`、`504/46911` 和 `502/46912`。

告警接口包括 `GET /alerts`、`GET /alerts/{alertId}`、`PATCH /alerts/{alertId}/acknowledge` 和 `PATCH /alerts/{alertId}/close`。告警字段包括 `alertId`、`ruleId`、`sourceService`、`sourceRef`、`severity`、`status`、`labels`、`fingerprint`、`groupKey`、`firstFiredAt`、`lastFiredAt`、`acknowledgedBy`、`acknowledgedAt`、`closedBy`、`closedAt`、`summary`、`runbookUrl`、`notificationSummary`、`suppressionSummary` 和 `resolutionSummary`。状态包括 `FIRING`、`SUPPRESSED`、`ACKNOWLEDGED` 和 `CLOSED`。确认已关闭告警返回 `409/49910`；关闭告警必须带 `confirmText=CLOSE_ALERT`，未确认返回 `403/42003`；告警不存在返回 `404/49902`。

静默接口包括 `GET /silences`、`POST /silences` 和 `PATCH /silences/{silenceId}/cancel`。静默字段包括 `silenceId`、`matchers`、`startsAt`、`endsAt`、`reason`、`status`、`createdBy`、`cancelledBy`、`createdAt` 和 `cancelledAt`。状态包括 `ACTIVE`、`EXPIRED` 和 `CANCELLED`。`matchers` 至少包含 `sourceService`、`severity`、`groupKey` 或 `labels` 之一；`endsAt` 必须晚于 `startsAt`。时间非法返回 `400/49913`，匹配器非法返回 `400/49914`，静默不存在返回 `404/49903`，终态静默取消返回 `409/49910`。

路由和投递接口包括 `GET /routes`、`POST /routes`、`PATCH /routes/{routeId}`、`POST /routes/{routeId}/test` 和 `GET /deliveries`。路由字段包括 `routeId`、`displayName`、`matchers`、`groupBy`、`groupWaitSeconds`、`groupIntervalSeconds`、`repeatIntervalSeconds`、`notificationTemplateRef`、`receiverSummary`、`status`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。投递字段包括 `deliveryId`、`alertId`、`routeId`、`notificationRef`、`deliveryMode`、`externalModule`、`externalDeliveryId`、`externalDeliveryStatus`、`externalAttemptStatus`、`realExternalSend`、`status`、`attempts`、`lastAttemptAt`、`failureCode`、`failureSummary`、`nextRetryAt` 和 `createdAt`。投递状态包括 `SENT`、`FAILED`、`RETRYING` 和 `SUPPRESSED`。通知依赖不可用、超时或结构不兼容时分别返回 `502/46900`、`504/46901` 和 `502/46902`；路由不存在返回 `404/49904`；路由字段非法返回 `400/40001`。

审计接口为 `GET /audit-logs`。审计筛选支持 `actorUserId`、`ruleId`、`alertId`、`silenceId`、`routeId`、`action`、`result`、`riskLevel`、`from`、`to` 和 `sort`。审计字段至少包括 `requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason`、`ruleId`、`alertId`、`silenceId`、`routeId`、`deliveryId`、`dependencyStatus` 和 `createdAt`。审计写入失败必须阻断对应写操作并返回 `500/55501`。涉及数据库新增或更新的自动化测试必须通过真实 HTTP 请求进入后端，完成后使用独立 SQL 查询验证规则、评估、告警、静默、路由、投递、审计和请求日志证据，并在测试日志输出 `SQL evidence`。

## plugin-integration 模块接口

`plugin-integration` 由 `ops-core` 承载，路径前缀为 `/api/v1/plugin-integration`，负责插件 provider、插件实例、能力点、事件 schema、事件接收、事件重放、路由规则、同步任务、对象映射、健康快照和审计。除 `GET /health` 外均要求登录；读取接口要求 `NODE_READ`；写接口要求 `NODE_WRITE` 且操作者为 `ADMIN` 或 `OWNER`；审计读取要求 `ADMIN` 或 `OWNER`。provider 端点注册、公开可见、启用、归档、高风险路由、事件重放、高风险同步任务和公开在线地图对象映射必须带对应 `confirmText`。所有写接口支持 `idempotencyKey`，同一操作者、同一作用域、同一请求指纹返回原结果；同键不同指纹返回 `409/49812`。

健康、摘要、实例和能力接口包括 `GET /health`、`GET /admin/ops/summary`、`GET /admin/instances`、`GET /admin/instances/{instanceId}` 和 `GET /admin/capabilities`。摘要返回 provider、实例、schema、事件、路由、同步任务、对象映射、审计、幂等、降级状态和生产缺口。认证依赖不可用、超时或结构不兼容时分别返回 `502/47050`、`504/47051` 和 `502/47052`；本地测试控制关闭时，测试控制头必须被忽略。实例不存在返回 `404/49801`。

provider 接口包括 `GET /admin/providers`、`GET /admin/providers/{providerId}`、`POST /admin/providers`、`PATCH /admin/providers/{providerId}`、`PATCH /admin/providers/{providerId}/enable`、`PATCH /admin/providers/{providerId}/disable`、`PATCH /admin/providers/{providerId}/archive` 和 `GET /admin/providers/{providerId}/health-snapshots`。provider 字段包括 `providerId`、`providerType`、`displayName`、`pluginName`、`pluginVersion`、`serverKind`、`instanceRef`、`nodeRef`、`status`、`publicVisible`、`eventEndpointSummary`、`allowedEventTypes`、`allowedOrigins`、`healthStatus`、`lastEventAt`、`lastSyncAt`、`degradeReasons`、`adminNote`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。provider 状态包括 `DRAFT`、`ENABLED`、`DISABLED` 和 `ARCHIVED`。provider 不存在返回 `404/49800`，状态冲突返回 `409/49810`，名称冲突返回 `409/49811`，端点或来源非法返回 `400/49813`。ops-control 依赖不可用返回 `502/47060`。

事件 schema 接口包括 `GET /admin/event-schemas`、`GET /admin/event-schemas/{schemaId}`、`POST /admin/event-schemas`、`PATCH /admin/event-schemas/{schemaId}`、`PATCH /admin/event-schemas/{schemaId}/enable` 和 `PATCH /admin/event-schemas/{schemaId}/disable`。schema 字段包括 `schemaId`、`providerId`、`eventType`、`sourcePlugin`、`version`、`status`、`requiredFields`、`optionalFields`、`sensitiveFields`、`routingHints`、`samplePayloadSummary`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。schema 状态包括 `DRAFT`、`ENABLED`、`DISABLED` 和 `ARCHIVED`。schema 不存在返回 `404/49802`，schema 冲突返回 `409/49811`，schema 中不得包含敏感明文字段。

事件接口包括 `POST /admin/events/ingest`、`GET /admin/events`、`GET /admin/events/{eventId}` 和 `POST /admin/events/{eventId}/replay`。事件字段包括 `eventId`、`providerId`、`eventType`、`schemaId`、`sourcePlugin`、`sourceInstanceId`、`dedupeKey`、`payloadSummary`、`rawPayloadStored`、`validationStatus`、`routeStatus`、`syncStatus`、`notificationStatus`、`receivedAt`、`processedAt`、`failureReason` 和 `requestId`。事件接收必须校验 provider 启用状态、允许事件类型、允许来源、启用 schema、必填字段和敏感字段脱敏；原始 payload 不得存储。事件不存在返回 `404/49803`，事件来源不允许返回 `403/49815`，payload 校验失败返回 `400/49814`。通知依赖失败不阻断事件写入，但必须在 `notificationStatus` 中返回失败或降级摘要。

路由规则接口包括 `GET /admin/route-rules`、`GET /admin/route-rules/{ruleId}`、`POST /admin/route-rules`、`PATCH /admin/route-rules/{ruleId}`、`PATCH /admin/route-rules/{ruleId}/enable` 和 `PATCH /admin/route-rules/{ruleId}/disable`。路由字段包括 `ruleId`、`displayName`、`eventType`、`matchers`、`targetModule`、`targetAction`、`enabled`、`riskLevel`、`rateLimitSummary`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。目标模块不得指向 `OPS_CONTROL`，否则返回 `409/49817`。路由不存在返回 `404/49804`，路由冲突返回 `409/49811`，高风险路由缺确认返回 `403/42003`。

同步任务和对象映射接口包括 `POST /admin/sync-tasks`、`GET /admin/sync-tasks`、`GET /admin/sync-tasks/{taskId}`、`PATCH /admin/sync-tasks/{taskId}/cancel`、`GET /admin/object-mappings`、`GET /admin/object-mappings/{mappingId}`、`PUT /admin/object-mappings/{mappingId}` 和 `PATCH /admin/object-mappings/{mappingId}/archive`。同步任务字段包括 `taskId`、`providerId`、`eventId`、`targetModule`、`targetAction`、`status`、`riskLevel`、`paramsSummary`、`resultSummary`、`failureReason`、`idempotencyKey`、`createdBy`、`createdAt`、`updatedAt` 和 `expiresAt`；状态包括 `QUEUED`、`SIMULATED_BLOCKED`、`SUCCEEDED`、`FAILED`、`CANCELED` 和 `TIMEOUT`。对象映射字段包括 `mappingId`、`providerId`、`sourcePlugin`、`sourceObjectType`、`sourceObjectKey`、`targetModule`、`targetObjectType`、`targetObjectId`、`status`、`visibility`、`lastSyncedAt`、`syncHash`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`；状态包括 `ACTIVE` 和 `ARCHIVED`。任务不存在返回 `404/49805`，对象映射不存在返回 `404/49806`，任务状态冲突返回 `409/49810`，对象映射冲突返回 `409/49811`，online-map 依赖不可用返回 `502/47080`。

审计接口为 `GET /admin/audit-logs`。审计筛选支持 `actorUserId`、`action`、`targetType`、`targetId`、`providerId`、`eventId`、`schemaId`、`ruleId`、`taskId`、`mappingId`、`result`、`riskLevel` 和 `sort`。审计字段至少包括 `requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason`、`providerId`、`eventId`、`schemaId`、`ruleId`、`taskId`、`mappingId`、`dependencyStatus`、`notificationStatus` 和 `createdAt`。审计写入失败必须阻断对应写操作并返回 `500/55701`。涉及数据库新增或更新的自动化测试必须通过真实 HTTP 请求进入后端，完成后使用独立 SQL 查询验证 provider、schema、event、route rule、sync task、object mapping、审计和请求日志证据，并在测试日志输出 `SQL evidence`。

## 受控生产入口字段

统一后端 readiness 可以暴露生产入口、旧入口退役、外部入口和审计观测相关状态字段。当前这些字段只代表仓库内运行态和外部证据接收状态，不代表真实生产切流完成。

`apiGatewayControlledRetirementStatus=BLOCKED_BY_API_GATEWAY_RETIREMENT_RECEIPT_NOT_PROVIDED` 表示旧入口退役收据未提供，对应仓库外证据引用 `EXTERNAL_EVIDENCE_REF:API_GATEWAY_RETIREMENT_RECEIPT`。

`apiGatewayExternalRetirementEvidenceStatus=BLOCKED_BY_EXTERNAL_API_GATEWAY_RETIREMENT_EVIDENCE_NOT_PROVIDED` 表示旧网关外部退役证据未提供，对应仓库外证据引用 `EXTERNAL_EVIDENCE_REF:API_GATEWAY_EXTERNAL_RETIREMENT`。

`realProductionEntrypointCutoverStatus=BLOCKED_BY_REAL_PRODUCTION_ENTRYPOINT_CUTOVER_EVIDENCE_NOT_PROVIDED` 表示真实生产入口切流证据未提供，对应仓库外证据引用 `EXTERNAL_EVIDENCE_REF:REAL_PRODUCTION_ENTRYPOINT_CUTOVER`。

`externalEntrypointCutoverEvidenceIntakeStatus=BLOCKED_BY_EXTERNAL_ENTRYPOINT_CUTOVER_EVIDENCE_NOT_PROVIDED` 表示外部入口与切流证据未提供，对应仓库外证据引用 `EXTERNAL_EVIDENCE_REF:EXTERNAL_ENTRYPOINT_CUTOVER_INTAKE`。

`localApiGatewayEntrypointRetirementStatus=PASS_LOCAL_API_GATEWAY_ENTRYPOINT_RETIRED_UNIFIED_GATEWAY_APIS_PRESERVED` 表示本地旧网关入口已经退役，统一后端 API 仍保持可用。

`productionAuditSinkPrecheckStatus=BLOCKED_BY_PERSISTENT_AUDIT_SINK_NOT_CONFIGURED` 表示生产持久化审计 sink 未配置。

外部入口与切流证据接收门禁使用 `frontendEntrypointRef`、`reverseProxyUpstreamRef`、`deploymentEntrypointRef`、`rollbackEntrypointRef`、`canaryWeightRef`、`observabilityRef` 和 `approvalRef` 记录仓库外证据引用。前端联调入口通过 `VITE_API_BASE_URL` 指向 `http://127.0.0.1:8135`，不得展示成真实生产切流完成。

历史网关行为对照只用于回滚追溯。旧网关引用为 `api-gateway:8125`，当前调用入口保持 `http://127.0.0.1:8135`，历史回滚引用不得作为默认前端入口。

没有仓库外证据时，`readyForProduction=false`、`readyToReplaceGateway=false`、`oldApiGatewayRetirementAllowed=false`、`readyToRetireBusinessCore=false`、`readyToRetireAdmissionCore=false`、`readyToRetireEngagementCore=false`、`readyToRetireOpsCore=false` 和 `readyToRetirePortalCore=false` 必须保持。`api-gateway-service` 只作为历史回滚引用，当前入口保持 `backend:8135`。

## 验收

当前后端统一验证命令是 `mvn -q -f backend/pom.xml test`。接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段以本文档、`docs/system-design.md` 和当前代码为准。`docs/` 当前只保留本文档和模块设计文档，不再保留独立模块契约文件、本地测试文档、阶段治理文档或旧微服务样例文件。
