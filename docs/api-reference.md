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

## content 模块接口

`content` 由 `business-core` 承载，路径前缀为 `/api/v1/content`，负责官网首页配置、公告、文章、页面内容、摄影作品、成员作品、服务器进度、专题、分类、标签、预览、版本历史和 SEO 配置。公开读取接口不要求登录；后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`；后台写接口要求 `ADMIN` 或 `OWNER`；审计读取和运维摘要要求 `ADMIN` 或 `OWNER`。`content` 只能使用认证上下文、可信网关头、profile 成员快照 provider 和 notification 正式客户端，不得直接读取 auth、profile、notification 或其他模块数据库。

内容类型包括 `ARTICLE`、`ANNOUNCEMENT`、`PAGE`、`PHOTO`、`MEMBER_WORK`、`SERVER_PROGRESS` 和 `MOMENT`。内容状态包括 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED` 和 `DELETED`。内容可见性包括 `PUBLIC`、`MEMBER_ONLY` 和 `PRIVATE`。专题状态包括 `DRAFT`、`APPROVED`、`OFFLINE`、`ARCHIVED` 和 `DELETED`，专题可见性包括 `PUBLIC` 和 `PRIVATE`。SEO robots 包括 `INDEX_FOLLOW`、`NOINDEX_FOLLOW` 和 `NOINDEX_NOFOLLOW`。首页区块类型包括 `HERO`、`FEATURED_ARTICLES`、`ANNOUNCEMENTS`、`MEMBER_WORKS`、`MOMENTS`、`MILESTONES`、`TOPICS`、`SERVER_ENTRY`、`RESOURCE_ENTRY` 和 `CUSTOM_LINKS`。

内容状态流转规则为：`DRAFT` 可转 `PENDING_REVIEW`、`ARCHIVED` 或 `DELETED`；`REJECTED` 可转 `PENDING_REVIEW`、`ARCHIVED` 或 `DELETED`；`NEEDS_CHANGES` 可转 `PENDING_REVIEW`、`ARCHIVED` 或 `DELETED`；`PENDING_REVIEW` 可转 `APPROVED`、`REJECTED` 或 `NEEDS_CHANGES`；`APPROVED` 可发布，已发布内容可转 `OFFLINE`，未发布已批准内容可以重复批准或发布；`OFFLINE` 可重新发布、归档或删除；`ARCHIVED` 和 `DELETED` 为终态，禁止普通修改、预览 token 创建、提交审核、发布、下线和版本恢复。专题状态流转规则为：`DRAFT` 可发布、归档或删除；`APPROVED` 可下线；`OFFLINE` 可发布、归档或删除；`ARCHIVED` 和 `DELETED` 为终态。内容版本恢复会把目标版本快照恢复为新的 `DRAFT`，并清空审核、发布和通知状态。

公开接口包括 `GET /home`、`GET /items`、`GET /items/{contentId}`、`GET /items/by-slug/{slug}`、`GET /items/{contentId}/preview`、`GET /categories`、`GET /tags`、`GET /topics`、`GET /topics/{topicId}`、`GET /topics/by-slug/{slug}`、`GET /seo` 和 `GET /seo/sitemap`。`GET /items` 支持 `page`、`pageSize`、`type`、`categoryId`、`tag`、`keyword` 和 `sort`，`sort` 可为 `publishedAt_desc`、`publishedAt_asc`、`updatedAt_desc`、`createdAt_desc` 和 `title_asc`，只返回状态为已发布 `APPROVED` 且可见性为 `PUBLIC` 的未过期内容，不返回 `body`、`adminNote`、`reviewOpinion`、`notificationStatus` 或 `idempotencyKey`。公开详情返回 `contentId`、`type`、`status`、`visibility`、`slug`、`title`、`summary`、`body`、`coverUrl`、`categoryId`、`tagIds`、作者快照、成员快照、SEO 摘要、发布时间、可见时间和更新时间；非公开、未发布、下线、归档、删除、未到可见时间或已过可见时间返回 `409/43412`。预览接口必须带有效 token，token 不存在、过期或指向终态内容返回 `404/43419`。

公开首页 `GET /home` 返回已发布首页配置的 `homeConfigId`、`version`、`sections`、`degraded`、`degradeReasons`、`publishedAt` 和 `seo`。引用的内容或专题不可公开展示时，该区块局部降级并在区块和首页上返回降级原因，不得把草稿、下线、归档、删除、私有内容或后台字段泄露到公开响应。没有已发布首页时返回降级空配置，不影响其他公开读取接口。`GET /categories` 和 `GET /tags` 只返回未归档分类和标签。`GET /topics` 支持 `page`、`pageSize`、`keyword`、`visibility` 和 `sort`，`sort` 可为 `publishedAt_desc`、`updatedAt_desc`、`createdAt_desc` 和 `title_asc`，公开接口只返回 `APPROVED` 且 `PUBLIC` 的专题。`GET /seo` 按 route 返回启用 SEO，未配置或已禁用时返回默认 SEO。`GET /seo/sitemap` 支持 `type=HOME|CONTENT|TOPIC`，只输出公开可访问资源。

后台内容接口包括 `GET /admin/items`、`GET /admin/items/{contentId}`、`POST /admin/items`、`PATCH /admin/items/{contentId}`、`POST /admin/items/{contentId}/preview-token`、`PATCH /admin/items/{contentId}/submit-review`、`PATCH /admin/items/{contentId}/approve`、`PATCH /admin/items/{contentId}/reject`、`PATCH /admin/items/{contentId}/request-changes`、`PATCH /admin/items/{contentId}/publish`、`PATCH /admin/items/{contentId}/offline`、`PATCH /admin/items/{contentId}/archive`、`PATCH /admin/items/{contentId}/delete`、`GET /admin/items/{contentId}/versions`、`GET /admin/items/{contentId}/versions/{version}`、`PATCH /admin/items/{contentId}/versions/{version}/restore` 和 `GET /admin/items/{contentId}/audit-logs`。后台列表支持 `page`、`pageSize`、`type`、`status`、`visibility`、`categoryId`、`tagId`、`createdBy`、`keyword` 和 `sort`，`sort` 可为 `createdAt_desc`、`updatedAt_desc`、`publishedAt_desc` 和 `title_asc`。后台内容字段包括公开字段以及 `adminNote`、`reviewOpinion`、`notificationStatus`、`submittedAt`、`reviewedAt`、`deletedAt`、`createdBy` 和 `updatedBy`。

`POST /admin/items` 请求字段包括 `type`、`slug`、`title`、`summary`、`body`、`coverUrl`、`categoryId`、`tagIds`、`visibility`、`authorUserId`、`memberId`、`seo`、`adminNote`、`visibleFrom`、`visibleUntil`、`reason` 和 `idempotencyKey`，成功返回 `201` 和后台内容详情，初始状态为 `DRAFT`，写入 `CONTENT_ITEM_CREATED` 审计并生成版本 `1`。`PATCH /admin/items/{contentId}` 允许修改 `slug`、`title`、`summary`、`body`、`coverUrl`、`categoryId`、`tagIds`、`visibility`、`memberId`、`seo`、`adminNote`、`visibleFrom` 和 `visibleUntil`，必须带 `reason`，成功写入 `CONTENT_ITEM_UPDATED` 并生成新版本。slug 冲突返回 `409/43411`，分类不存在返回 `404/43401`，标签不存在返回 `404/43405`，终态内容修改返回 `409/43410`。

预览、审核和发布接口都必须带 `reason`。`POST /admin/items/{contentId}/preview-token` 请求字段包括 `expiresInMinutes` 和 `reason`，过期分钟数范围为 `5-1440`，成功返回 `token`、`previewUrl`、`expiresAt` 和 `createdAt`，并记录 `CONTENT_ITEM_PREVIEW_TOKEN_CREATED`。`PATCH /submit-review` 将 `DRAFT`、`REJECTED` 或 `NEEDS_CHANGES` 转为 `PENDING_REVIEW`，重复提交保持幂等返回当前状态。`PATCH /approve`、`PATCH /reject` 和 `PATCH /request-changes` 只能处理 `PENDING_REVIEW`，请求字段包括 `reviewOpinion` 和 `reason`，成功分别写入 `CONTENT_ITEM_APPROVED`、`CONTENT_ITEM_REJECTED` 和 `CONTENT_ITEM_CHANGES_REQUESTED`，其中通知依赖为必需依赖，失败分别返回 `502/46410` 或 `504/46411` 且不得完成状态流转。`PATCH /publish` 只能发布 `APPROVED` 或 `OFFLINE` 内容，可带 `visibleFrom` 和 `visibleUntil`，成功写入 `CONTENT_ITEM_PUBLISHED` 并生成版本；通知依赖为可选依赖，失败只写入 `notificationStatus` 降级摘要，不阻断发布。`PATCH /offline`、`PATCH /archive` 和 `PATCH /delete` 分别写入 `CONTENT_ITEM_OFFLINE`、`CONTENT_ITEM_ARCHIVED` 和 `CONTENT_ITEM_DELETED`；已发布内容必须先下线再归档或删除。

版本接口中 `GET /admin/items/{contentId}/versions` 支持 `page`、`pageSize` 和 `sort`，`sort` 可为 `version_desc` 和 `version_asc`。版本字段包括 `contentId`、`version`、`sourceAction`、`snapshot`、`createdBy`、`createdAt`、`reason` 和 `restoredFromVersion`。版本不存在返回 `404/43417`，终态内容恢复返回 `409/43418`，恢复后必须生成新的版本记录并写入 `CONTENT_ITEM_VERSION_RESTORED` 审计。

后台首页接口包括 `GET /admin/home`、`PUT /admin/home`、`POST /admin/home/preview`、`PATCH /admin/home/publish` 和 `PATCH /admin/home/rollback`。`PUT /admin/home` 请求字段包括 `sections`、`seo`、`reason` 和 `idempotencyKey`，`sections` 最多 20 个，成功保存草稿并写入 `CONTENT_HOME_DRAFT_SAVED`。`POST /admin/home/preview` 只返回预览结果，不写业务表、不写审计、不改变公开首页。`PATCH /admin/home/publish` 发布当前草稿并写入 `CONTENT_HOME_PUBLISHED`；无草稿返回 `404/43404`；重复发布相同草稿保持幂等返回当前发布版本。`PATCH /admin/home/rollback` 请求字段为 `version` 和 `reason`，成功回滚已发布首页并写入 `CONTENT_HOME_ROLLED_BACK`，目标版本不存在返回 `404/43404`。

分类和标签接口包括 `GET /admin/categories`、`POST /admin/categories`、`PATCH /admin/categories/{categoryId}`、`PATCH /admin/categories/{categoryId}/archive`、`GET /admin/tags`、`POST /admin/tags`、`PATCH /admin/tags/{tagId}` 和 `PATCH /admin/tags/{tagId}/archive`。列表支持 `includeArchived`。分类字段包括 `categoryId`、`name`、`slug`、`description`、`sortOrder`、`archived`、`createdAt` 和 `updatedAt`；标签字段包括 `tagId`、`name`、`slug`、`archived`、`createdAt` 和 `updatedAt`。创建接口请求字段包括名称、slug、可选描述或排序、`reason` 和 `idempotencyKey`，成功返回 `201`。名称或 slug 冲突返回 `409/43001`，仍被非归档非删除内容使用时归档返回 `409/43415`。

专题接口包括 `GET /admin/topics`、`GET /admin/topics/{topicId}`、`POST /admin/topics`、`PATCH /admin/topics/{topicId}`、`PATCH /admin/topics/{topicId}/publish`、`PATCH /admin/topics/{topicId}/offline`、`PATCH /admin/topics/{topicId}/archive` 和 `PATCH /admin/topics/{topicId}/delete`。列表支持 `page`、`pageSize`、`status`、`visibility`、`keyword` 和 `sort`，`sort` 可为 `updatedAt_desc`、`createdAt_desc`、`publishedAt_desc` 和 `title_asc`。专题字段包括 `topicId`、`slug`、`title`、`summary`、`coverUrl`、`status`、`visibility`、`contentIds`、`items`、`seo`、`publishedAt`、`createdAt`、`updatedAt` 和 `deletedAt`。创建和修改请求字段包括 `slug`、`title`、`summary`、`coverUrl`、`visibility`、`contentIds`、`seo`、`reason` 和 `idempotencyKey`；`contentIds` 最多 50 个，可保存缺失引用但公开渲染必须局部降级。slug 冲突返回 `409/43411`，终态或非法流转返回 `409/43414`。

SEO 后台接口包括 `GET /admin/seo`、`GET /admin/seo/{seoId}`、`PUT /admin/seo/by-route` 和 `PATCH /admin/seo/{seoId}/disable`。列表支持 `page`、`pageSize`、`route`、`keyword` 和 `sort`，`sort` 可为 `updatedAt_desc` 和 `route_asc`。SEO 字段包括 `seoId`、`route`、`title`、`description`、`keywords`、`coverUrl`、`robots`、`canonicalUrl`、`enabled` 和 `updatedAt`。`PUT /admin/seo/by-route` 请求字段包括 `route`、`title`、`description`、`keywords`、`coverUrl`、`robots`、`canonicalUrl`、`reason` 和 `idempotencyKey`，按 route 创建或更新，成功分别写入 `CONTENT_SEO_CREATED` 或 `CONTENT_SEO_UPDATED`。SEO 不存在返回 `404/43403`，字段非法返回 `400/40001`，幂等冲突返回 `409/43002`。

审计接口为 `GET /admin/items/{contentId}/audit-logs`，支持 `page` 和 `pageSize`。审计字段至少包括 `id`、`requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`sourceIp`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason` 和 `createdAt`。审计记录不得泄露 `Authorization`、Cookie、token、预览 token 原文、`idempotencyKey`、完整正文中的敏感片段或后台私有备注。审计写入失败必须阻断对应写操作并返回 `500/51401`。`GET /admin/ops/summary` 返回 `service`、`storageMode`、`authMode`、内容、分类、标签、专题、SEO、首页配置、版本、预览 token、审计、幂等、降级状态和生产缺口摘要，不得包含正文、token 或幂等键。

写接口幂等使用请求体内 `idempotencyKey`。同一操作者、同一作用域、同一幂等键和同一请求指纹必须返回原响应；同键不同指纹返回 `409/43002`。当前必须覆盖内容创建、首页草稿保存、分类创建、标签创建、专题创建和 SEO by-route upsert 的幂等重放和冲突；其他带 `idempotencyKey` 的写接口按同一规则落库。请求字段顺序不同但语义相同时应视为同一指纹。

错误码保持现有语义：字段校验失败返回 `400/40001`，分页非法返回 `400/40002`，排序非法返回 `400/40003`，未登录返回 `401/41000`，会话无效返回 `401/41001`，token 格式错误返回 `401/41003`，角色不足返回 `403/42001`，普通冲突返回 `409/43001`，幂等冲突返回 `409/43002`，内容不存在返回 `404/43400`，分类不存在返回 `404/43401`，专题不存在返回 `404/43402`，SEO 不存在返回 `404/43403`，首页配置不存在返回 `404/43404`，标签不存在返回 `404/43405`，内容状态冲突返回 `409/43410`，slug 冲突返回 `409/43411`，内容不可公开展示返回 `409/43412`，专题状态冲突返回 `409/43414`，分类或标签使用中返回 `409/43415`，版本不存在返回 `404/43417`，版本恢复状态冲突返回 `409/43418`，预览 token 不存在或过期返回 `404/43419`，profile 依赖不可用、超时或结构不兼容分别返回 `502/46400`、`504/46401` 和 `502/46402`，notification 必需投递不可用或超时分别返回 `502/46410` 和 `504/46411`，auth 依赖不可用、超时或结构不兼容分别返回 `502/46420`、`504/46421` 和 `502/46422`，模块内部错误返回 `500/51400`，审计写入失败返回 `500/51401`。

PostgreSQL 是 `content` 正式持久化验收依据。成功写接口必须经 `SpringBootTest` 的 `RANDOM_PORT` 通过真实 HTTP 请求进入后端，覆盖请求接收、认证上下文解析、业务校验、内存响应模型更新、PostgreSQL 写入和响应返回。每个成功写接口必须用独立 SQL 查询验证 `content_items`、`content_item_versions`、`content_categories`、`content_tags`、`content_item_tags`、`content_topics`、`content_topic_items`、`content_home_configs`、`content_home_versions`、`content_preview_tokens` 或 `content_seo_configs` 对应业务表，以及 `app_audit_logs` 和 `app_request_logs`；带 `idempotencyKey` 的接口还必须验证 `app_idempotency_records`。业务表写入、审计、幂等记录和请求日志必须在同一事务内提交，并在测试日志输出 `SQL evidence`。H2 只保留为旧 request database flow evidence，不作为 content 正式持久化验收依据。

## server-status 模块接口

`server-status` 由 `business-core` 承载，路径前缀为 `/api/v1/server-status`，负责玩家可见的 Minecraft 服务器状态、线路、在线人数、延迟、历史快照和故障展示。公开读取接口不要求登录；后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`；后台写接口、来源刷新、审计读取和运维摘要要求 `ADMIN` 或 `OWNER`。`server-status` 只能使用认证上下文和采集器摘要，不得直接读取 auth、profile、resource、ops-control 或节点守护进程数据库，不得执行服务器启停、容器操作、文件操作、终端命令或真实节点控制。

来源配置状态包括 `ENABLED`、`DISABLED` 和 `ARCHIVED`。来源类型包括 `MINECRAFT_PING`、`HTTP_HEALTH`、`MANUAL` 和 `STUB`。实例类型包括 `SURVIVAL`、`CREATIVE`、`TEST`、`LOBBY` 和 `OTHER`。实例运行状态包括 `ONLINE`、`DEGRADED`、`OFFLINE` 和 `UNKNOWN`。线路配置状态包括 `ENABLED`、`DISABLED` 和 `ARCHIVED`，线路运行状态包括 `AVAILABLE`、`DEGRADED`、`UNAVAILABLE` 和 `UNKNOWN`。快照来源包括 `SCHEDULED`、`MANUAL_REFRESH`、`SEED` 和 `DEGRADED_FALLBACK`。故障状态包括 `OPEN`、`ACKNOWLEDGED`、`RESOLVED` 和 `ARCHIVED`，故障等级包括 `LOW`、`MEDIUM`、`HIGH` 和 `CRITICAL`。

来源状态流转规则为：`ENABLED` 可转 `DISABLED` 或 `ARCHIVED`；`DISABLED` 可转 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态，禁止修改、启用、禁用和刷新。线路状态流转规则与来源一致。故障状态流转规则为：`OPEN` 可转 `ACKNOWLEDGED` 或 `RESOLVED`；`ACKNOWLEDGED` 可转 `RESOLVED`；`RESOLVED` 可转 `ARCHIVED`；`ARCHIVED` 为终态。重复禁用、重复启用、重复确认、重复解决在状态已满足时保持幂等返回当前状态，不重复写入同一动作审计。

公开接口包括 `GET /overview`、`GET /instances`、`GET /instances/{instanceId}`、`GET /lines`、`GET /history/snapshots` 和 `GET /outages`。`GET /overview` 返回 `overallStatus`、`primaryInstance`、`primaryLine`、`onlinePlayers`、`maxPlayers`、`version`、`motd`、`latencyMs`、`uptimeSeconds`、`peakOnlinePlayers`、`lastSuccessfulSnapshotAt`、`lastCheckedAt`、公开 `instances`、公开 `lines`、公开 `openOutages`、`degraded` 和 `degradeReasons`。公开响应不得包含 `target`、`checkTarget`、`adminNote`、`internalReason`、`idempotencyKey`、节点凭据或运维命令字段。

`GET /instances` 支持 `page`、`pageSize`、`kind`、`status` 和 `sort`，`sort` 可为 `sortOrder_asc`、`name_asc` 和 `onlinePlayers_desc`，只返回 `publicVisible=true` 且来源配置状态为 `ENABLED` 的实例。实例列表字段包括 `instanceId`、`name`、`kind`、`status`、`version`、`motd`、`onlinePlayers`、`maxPlayers`、`latencyMs`、`startedAt`、`lastSuccessfulSnapshotAt` 和 `sortOrder`。`GET /instances/{instanceId}` 返回同一公开实例详情；实例不存在、隐藏或禁用返回 `404/43500`。

`GET /lines` 支持 `page`、`pageSize`、`status` 和 `sort`，`sort` 可为 `sortOrder_asc`、`latencyMs_asc` 和 `name_asc`，只返回 `publicVisible=true` 且配置状态为 `ENABLED` 的线路。线路字段包括 `lineId`、`name`、`entryAddress`、`description`、`status`、`latencyMs`、`packetLossPercent`、`lastCheckedAt` 和 `sortOrder`。线路不存在或不可公开时返回 `404/43501`。

`GET /history/snapshots` 支持 `page`、`pageSize`、`instanceId`、`lineId`、`status`、`from`、`to` 和 `sort`，`sort` 可为 `checkedAt_desc`、`checkedAt_asc` 和 `onlinePlayers_desc`。快照字段包括 `snapshotId`、`sourceId`、`instanceId`、`lineId`、`source`、`status`、`lineStatus`、`version`、`motd`、`onlinePlayers`、`maxPlayers`、`latencyMs`、`lineLatencyMs`、`checkedAt` 和 `degraded`。`from` 晚于 `to` 返回 `400/40001`，实例或线路不存在返回对应 `404/43500` 或 `404/43501`。

`GET /outages` 支持 `page`、`pageSize`、`status`、`severity` 和 `sort`，`sort` 可为 `startedAt_desc` 和 `updatedAt_desc`。公开故障只返回 `publicVisible=true` 且状态不是 `ARCHIVED` 的记录，字段包括 `outageId`、`title`、`publicMessage`、`status`、`severity`、`startedAt`、`resolvedAt` 和 `updatedAt`。公开接口不接受 `ARCHIVED` 状态筛选，非法状态返回 `400/40001`。

后台来源接口包括 `GET /admin/sources`、`POST /admin/sources`、`PATCH /admin/sources/{sourceId}`、`PATCH /admin/sources/{sourceId}/disable`、`PATCH /admin/sources/{sourceId}/enable` 和 `POST /admin/sources/{sourceId}/refresh`。`GET /admin/sources` 支持 `page`、`pageSize`、`keyword`、`sourceType`、`configStatus`、`instanceKind`、`publicVisible` 和 `sort`，`sort` 可为 `createdAt_desc`、`updatedAt_desc`、`sortOrder_asc` 和 `displayName_asc`。来源字段包括 `sourceId`、`instanceId`、`displayName`、`instanceName`、`instanceKind`、`sourceType`、`configStatus`、`publicVisible`、`primary`、`target`、`timeoutMs`、`sortOrder`、`startedAt`、`adminNote`、`lastSnapshotAt`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。

`POST /admin/sources` 请求字段包括 `instanceName`、`displayName`、`instanceKind`、`sourceType`、`target`、`publicVisible`、`primary`、`timeoutMs`、`sortOrder`、`startedAt`、`adminNote`、`reason` 和 `idempotencyKey`，成功返回 `201` 和来源详情，默认 `configStatus=ENABLED`，记录 `SERVER_STATUS_SOURCE_CREATED`。同一个未归档来源的 `target` 或 `instanceName` 冲突返回 `409/43511`。`PATCH /admin/sources/{sourceId}` 允许修改 `displayName`、`instanceName`、`instanceKind`、`sourceType`、`target`、`publicVisible`、`primary`、`timeoutMs`、`sortOrder`、`startedAt` 和 `adminNote`，必须带 `reason`，成功记录 `SERVER_STATUS_SOURCE_UPDATED`。来源不存在返回 `404/43502`，终态或非法流转返回 `409/43510`。

`PATCH /admin/sources/{sourceId}/disable` 和 `PATCH /admin/sources/{sourceId}/enable` 必须带 `reason`，成功分别记录 `SERVER_STATUS_SOURCE_DISABLED` 和 `SERVER_STATUS_SOURCE_ENABLED`。`POST /admin/sources/{sourceId}/refresh` 必须带 `reason`，可带 `idempotencyKey`，只允许刷新 `ENABLED` 来源；同一来源同一时刻只允许一个刷新请求，未带幂等键的手动刷新在 10 分钟冷却期内返回 `409/43512`。刷新成功写入 `server_status_snapshots`，返回快照字段并记录 `SERVER_STATUS_SOURCE_REFRESHED`。采集器不可用、超时和结构不兼容分别返回 `502/46510`、`504/46511` 和 `502/46512`，快照写入失败返回 `500/51502`。

后台线路接口包括 `GET /admin/lines`、`POST /admin/lines`、`PATCH /admin/lines/{lineId}`、`PATCH /admin/lines/{lineId}/disable` 和 `PATCH /admin/lines/{lineId}/enable`。`GET /admin/lines` 支持 `page`、`pageSize`、`keyword`、`configStatus`、`currentStatus`、`publicVisible` 和 `sort`，`sort` 可为 `createdAt_desc`、`updatedAt_desc`、`sortOrder_asc` 和 `name_asc`。后台线路字段包括公开线路字段以及 `checkTarget`、`configStatus`、`currentStatus`、`publicVisible`、`primary`、`adminNote`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。

`POST /admin/lines` 请求字段包括 `name`、`entryAddress`、`checkTarget`、`description`、`publicVisible`、`primary`、`sortOrder`、`adminNote`、`reason` 和 `idempotencyKey`，成功返回 `201`，默认 `configStatus=ENABLED`、`currentStatus=UNKNOWN`，记录 `SERVER_STATUS_LINE_CREATED`。未归档线路 `entryAddress` 冲突返回 `409/43511`。`PATCH /admin/lines/{lineId}` 允许修改 `name`、`entryAddress`、`checkTarget`、`description`、`publicVisible`、`primary`、`sortOrder` 和 `adminNote`，必须带 `reason`，成功记录 `SERVER_STATUS_LINE_UPDATED`。线路不存在返回 `404/43501`，终态或非法流转返回 `409/43510`。禁用和启用接口成功分别记录 `SERVER_STATUS_LINE_DISABLED` 和 `SERVER_STATUS_LINE_ENABLED`。

后台故障接口包括 `GET /admin/outages`、`POST /admin/outages`、`PATCH /admin/outages/{outageId}`、`PATCH /admin/outages/{outageId}/acknowledge`、`PATCH /admin/outages/{outageId}/resolve` 和 `PATCH /admin/outages/{outageId}/archive`。`GET /admin/outages` 支持 `page`、`pageSize`、`keyword`、`status`、`severity`、`instanceId`、`lineId` 和 `sort`，`sort` 可为 `startedAt_desc`、`updatedAt_desc` 和 `resolvedAt_desc`。后台故障字段包括公开故障字段以及 `instanceId`、`lineId`、`internalReason`、`adminNote`、`createdBy`、`updatedBy`、`acknowledgedBy`、`resolvedBy`、`archivedBy`、`acknowledgedAt`、`archivedAt` 和 `createdAt`。

`POST /admin/outages` 请求字段包括 `title`、`publicMessage`、`severity`、`instanceId`、`lineId`、`startedAt`、`internalReason`、`adminNote`、`publicVisible`、`reason` 和 `idempotencyKey`，成功返回 `201`，初始状态为 `OPEN`，记录 `SERVER_STATUS_OUTAGE_CREATED`。实例不存在返回 `404/43500`，线路不存在返回 `404/43501`。`PATCH /admin/outages/{outageId}` 允许修改 `publicMessage`、`internalReason`、`adminNote`、`instanceId`、`lineId` 和 `publicVisible`，必须带 `reason`，成功记录 `SERVER_STATUS_OUTAGE_UPDATED`。确认、解决和归档接口必须带 `reason`；解决接口可带 `resolvedAt` 和 `publicMessage`，`resolvedAt` 不能早于 `startedAt`；成功分别记录 `SERVER_STATUS_OUTAGE_ACKNOWLEDGED`、`SERVER_STATUS_OUTAGE_RESOLVED` 和 `SERVER_STATUS_OUTAGE_ARCHIVED`。故障不存在返回 `404/43504`，非法状态流转返回 `409/43510`。

审计接口为 `GET /admin/audit-logs`，支持 `page`、`pageSize`、`targetType`、`targetId`、`action`、`actorUserId`、`result`、`from`、`to` 和 `sort`，`sort` 可为 `createdAt_desc` 和 `createdAt_asc`。审计字段至少包括 `id`、`requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`sourceIp`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason` 和 `createdAt`。审计记录不得泄露 `Authorization`、Cookie、token、`idempotencyKey`、来源目标中的凭据、后台备注中的敏感片段或节点运维参数。审计写入失败必须阻断对应写操作并返回 `500/51501`。

`GET /admin/ops/summary` 返回 `service`、`storageMode`、`collectorMode`、`authMode`、`sourcesTotal`、`instancesTotal`、`linesTotal`、`snapshotsTotal`、`outagesTotal`、`auditsTotal`、`lastSnapshotAt`、`lastAuditAt`、`warnings`、`postgresTablesReady` 和 `persistenceGaps`。该接口不得包含 `target`、`checkTarget`、`adminNote`、`internalReason`、token、节点凭据或真实服务器操作入口。当前 PostgreSQL 前置闭环完成后 `storageMode` 可返回 `POSTGRESQL_WITH_IN_MEMORY_RESPONSE_MODEL`，旧内存契约测试仍可在无 JDBC 环境下返回 `IN_MEMORY`。

写接口幂等使用请求体内 `idempotencyKey`。同一操作者、同一作用域、同一幂等键和同一请求指纹必须返回原结果；同键不同指纹返回 `409/43002`。当前必须覆盖来源创建和来源刷新的幂等重放与冲突，线路创建和故障创建按同一规则落库。请求字段顺序不同但语义相同时应视为同一指纹。幂等记录作用域固定为 `server-status.source.create`、`server-status.source.refresh`、`server-status.line.create` 和 `server-status.outage.create`。

错误码保持现有语义：字段校验失败返回 `400/40001`，分页非法返回 `400/40002`，排序非法返回 `400/40003`，未登录返回 `401/41000`，会话无效返回 `401/41001`，token 格式错误返回 `401/41003`，角色不足返回 `403/42001`，普通冲突返回 `409/43001`，幂等冲突返回 `409/43002`，实例不存在或不可公开返回 `404/43500`，线路不存在或不可公开返回 `404/43501`，来源不存在返回 `404/43502`，快照不存在返回 `404/43503`，故障不存在返回 `404/43504`，终态或状态流转冲突返回 `409/43510`，来源、线路或唯一字段冲突返回 `409/43511`，刷新并发或冷却冲突返回 `409/43512`，auth 依赖不可用、超时或结构不兼容分别返回 `502/46500`、`504/46501` 和 `502/46502`，采集器不可用、超时或结构不兼容分别返回 `502/46510`、`504/46511` 和 `502/46512`，模块内部错误返回 `500/51500`，审计写入失败返回 `500/51501`，快照写入失败返回 `500/51502`。

PostgreSQL 是 `server-status` 正式持久化验收依据。成功写接口必须经 `SpringBootTest` 的 `RANDOM_PORT` 通过真实 HTTP 请求进入后端，覆盖请求接收、认证上下文解析、业务校验、内存响应模型更新、PostgreSQL 写入和响应返回。每个成功写接口必须用独立 SQL 查询验证 `server_status_sources`、`server_status_lines`、`server_status_snapshots`、`server_status_outages` 或 `server_status_refresh_records` 对应业务表，以及 `app_audit_logs` 和 `app_request_logs`；带 `idempotencyKey` 的接口还必须验证 `app_idempotency_records`。业务表写入、审计、幂等记录和请求日志必须在同一事务内提交，并在测试日志输出 `SQL evidence`。H2 只保留为旧 request database flow evidence，不作为 `server-status` 正式持久化验收依据。

## resource 模块接口

`resource` 由 `business-core` 承载，路径前缀为 `/api/v1/resources`，负责玩家资源下载、资源分类、版本、Cloudreve 分享链接、下载权限、资源状态、下载记录和资源审计。公开读取接口不要求登录；需要下载 `AUTHENTICATED`、`MEMBER_ONLY` 或 `ADMIN_ONLY` 可见资源时分别要求登录、成员档案状态为 `ACTIVE` 或 `INACTIVE`、以及 `ADMIN` 或 `OWNER`；后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`；后台写接口、审计读取和运维摘要要求 `ADMIN` 或 `OWNER`。`resource` 只能使用认证上下文、profile 成员快照、notification 投递摘要和 Cloudreve 分享快照，不得直接读取 auth、profile、notification、content、server-status、ops-control 或节点守护进程数据库，不得执行后台服务器文件管理、终端命令、容器操作或真实节点控制。

资源类型包括 `CLIENT_PACK`、`RESOURCE_PACK`、`SHADER_PACK`、`MAP_FILE`、`RULE_DOCUMENT`、`ACTIVITY_RESOURCE`、`GUIDE_ATTACHMENT` 和 `OTHER`。资源可见性包括 `PUBLIC`、`AUTHENTICATED`、`MEMBER_ONLY` 和 `ADMIN_ONLY`。资源状态包括 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`PUBLISHED`、`OFFLINE`、`ARCHIVED` 和 `DELETED`。版本状态包括 `ENABLED`、`DISABLED` 和 `ARCHIVED`。下载入口状态包括 `ACTIVE`、`DISABLED`、`EXPIRED` 和 `UNAVAILABLE`。下载 provider 包括 `CLOUDREVE_SHARE` 和 `EXTERNAL_URL`。Cloudreve 降级模式包括正常、可用旧快照、不可用和超时；可用旧快照时响应返回 `degraded=true`、`stale=true` 和 `degradeReasons`，不可用或超时分别返回依赖错误。

资源状态流转规则为：`DRAFT`、`REJECTED` 和 `NEEDS_CHANGES` 可提交为 `PENDING_REVIEW`；`PENDING_REVIEW` 可转 `APPROVED`、`REJECTED` 或 `NEEDS_CHANGES`；`APPROVED` 和 `OFFLINE` 可发布为 `PUBLISHED`；`PUBLISHED` 可转 `OFFLINE`；`DRAFT`、`REJECTED`、`NEEDS_CHANGES` 和 `OFFLINE` 可转 `ARCHIVED` 或 `DELETED`；`ARCHIVED` 和 `DELETED` 为终态。重复提交审核、重复审核同一结果、重复发布、重复下线、重复归档或重复删除在状态已满足时返回当前状态，不得破坏已有响应格式。发布必须存在至少一个 `ENABLED` 且下载入口 `ACTIVE` 的版本，否则返回状态冲突。

公开接口包括 `GET /`、`GET /{resourceId}`、`GET /by-slug/{slug}`、`GET /categories`、`GET /{resourceId}/versions` 和 `POST /{resourceId}/versions/{versionId}/download`。`GET /` 支持 `page`、`pageSize`、`keyword`、`type`、`categoryId`、`visibility`、`tag`、`minecraftVersion` 和 `sort`，`sort` 可为 `publishedAt_desc`、`updatedAt_desc`、`title_asc` 和 `downloadUpdatedAt_desc`，只返回 `PUBLISHED`、未超出可见时间窗口且非 `ADMIN_ONLY` 的资源。公开资源列表字段包括 `resourceId`、`slug`、`type`、`title`、`summary`、`coverUrl`、`visibility`、`category`、`tags`、`latestVersion`、`downloadAvailable`、`degraded`、`degradeReasons`、`publishedAt` 和 `updatedAt`。公开详情额外返回 `description`、`visibleFrom`、`visibleUntil`、`maintainerSnapshot`、公开版本列表和 `createdAt`。资源不存在、草稿、下线、归档、删除、隐藏或后台专用时返回 `404/43600`。公开响应不得包含 `adminNote`、`reviewOpinion`、`idempotencyKey`、`downloadUrl`、`sharePassword`、`internalPath`、节点凭据或运维命令字段。

`GET /categories` 支持 `type`，只返回启用且未归档分类。分类字段包括 `categoryId`、`name`、`slug`、`description`、`icon`、`sortOrder`、`enabled`、`archived`、`createdAt`、`updatedAt` 和 `archivedAt`。`GET /{resourceId}/versions` 只返回公开资源下 `ENABLED`、未归档且存在下载入口的版本，字段包括 `versionId`、`versionName`、`title`、`changelog`、`minecraftVersions`、`loader`、`fileSizeBytes`、`checksumSha256`、`releasedAt`、`downloadEntryId`、`downloadAvailable` 和 `createdAt`。版本不存在返回 `404/43602`，下载入口不存在返回 `404/43603`。

`POST /{resourceId}/versions/{versionId}/download` 请求字段包括 `downloadEntryId`、`clientLabel` 和 `idempotencyKey`。成功返回下载票据字段 `ticketId`、`resourceId`、`versionId`、`downloadEntryId`、`provider`、`downloadUrl`、`expiresAt`、`degraded`、`stale`、`degradeReasons`、`maskedPasswordRequired` 和 `createdAt`，并写入 `RESOURCE_DOWNLOADED` 审计和下载记录。公开资源可匿名下载；`AUTHENTICATED` 资源要求登录；`MEMBER_ONLY` 资源要求 profile 成员状态为 `ACTIVE` 或 `INACTIVE`；`ADMIN_ONLY` 资源要求 `ADMIN` 或 `OWNER`。版本非 `ENABLED` 返回 `409/43610`，下载入口非 `ACTIVE` 返回 `409/43613`，Cloudreve 不可用或超时分别返回 `502/46630` 和 `504/46631`。下载票据不得返回分享密码、内部路径、原始 token 或 Cloudreve 管理凭据。

后台资源接口包括 `GET /admin/items`、`GET /admin/items/{resourceId}`、`POST /admin/items`、`PATCH /admin/items/{resourceId}`、`PATCH /admin/items/{resourceId}/submit-review`、`PATCH /admin/items/{resourceId}/approve`、`PATCH /admin/items/{resourceId}/reject`、`PATCH /admin/items/{resourceId}/request-changes`、`PATCH /admin/items/{resourceId}/publish`、`PATCH /admin/items/{resourceId}/offline`、`PATCH /admin/items/{resourceId}/archive` 和 `PATCH /admin/items/{resourceId}/delete`。`GET /admin/items` 支持 `page`、`pageSize`、`keyword`、`status`、`type`、`visibility`、`categoryId`、`tag`、`maintainerMemberId` 和 `sort`，`sort` 可为 `createdAt_desc`、`updatedAt_desc`、`publishedAt_desc` 和 `title_asc`。后台资源详情字段包括公开字段以及 `status`、完整 `description`、`adminNote`、`reviewOpinion`、`notificationStatus`、`submittedAt`、`reviewedAt`、`visibleFrom`、`visibleUntil`、`createdBy`、`updatedBy`、`createdAt`、`updatedAt`、`deletedAt` 和后台版本列表。

`POST /admin/items` 请求字段包括 `type`、`visibility`、`slug`、`title`、`summary`、`description`、`coverUrl`、`categoryId`、`tags`、`maintainerMemberId`、`visibleFrom`、`visibleUntil`、`adminNote`、`reason` 和 `idempotencyKey`，成功返回 `201` 和后台资源详情，初始状态为 `DRAFT`，记录 `RESOURCE_CREATED`。`PATCH /admin/items/{resourceId}` 允许修改 `type`、`visibility`、`slug`、`title`、`summary`、`description`、`coverUrl`、`categoryId`、`tags`、`maintainerMemberId`、`visibleFrom`、`visibleUntil` 和 `adminNote`，必须带 `reason`，成功记录 `RESOURCE_UPDATED`。资源 slug 冲突返回 `409/43611`，分类不存在返回 `404/43601`，profile 依赖不可用、超时或结构不兼容分别返回 `502/46610`、`504/46611` 和 `502/46612`，终态资源修改返回 `409/43610`。

审核和发布接口都必须带 `reason`。`PATCH /submit-review` 写入 `RESOURCE_SUBMIT_REVIEW`；`PATCH /approve`、`PATCH /reject` 和 `PATCH /request-changes` 请求字段包括 `reviewOpinion` 和 `reason`，成功分别写入 `RESOURCE_APPROVE`、`RESOURCE_REJECT` 和 `RESOURCE_REQUEST_CHANGES`。拒绝和要求修改依赖 notification 必需投递，notification 不可用或超时分别返回 `502/46620` 和 `504/46621`，不得完成状态流转。`PATCH /publish` 成功写入 `RESOURCE_PUBLISH`，并设置 `publishedAt`；通知失败只写入 `notificationStatus=FAILED` 降级摘要，不阻断发布。`PATCH /offline`、`PATCH /archive` 和 `PATCH /delete` 分别写入 `RESOURCE_OFFLINE`、`RESOURCE_ARCHIVE` 和 `RESOURCE_DELETE`。资源不存在返回 `404/43600`，非法状态流转返回 `409/43610`，发布条件不足返回 `409/43614`。

后台版本接口包括 `GET /admin/items/{resourceId}/versions`、`POST /admin/items/{resourceId}/versions`、`PATCH /admin/items/{resourceId}/versions/{versionId}`、`PATCH /admin/items/{resourceId}/versions/{versionId}/disable` 和 `PATCH /admin/items/{resourceId}/versions/{versionId}/enable`。后台版本字段包括 `versionId`、`resourceId`、`status`、`versionName`、`title`、`changelog`、`minecraftVersions`、`loader`、`fileSizeBytes`、`checksumSha256`、`releasedAt`、`downloadEntry`、`createdBy`、`updatedBy`、`createdAt` 和 `updatedAt`。下载入口字段包括 `downloadEntryId`、`provider`、`status`、`displayName`、`shareSnapshot`、`lastCheckedAt`、`expiresAt` 和后台 `adminNote`；后台响应不得包含分享密码、内部路径或 Cloudreve 管理 token。

`POST /admin/items/{resourceId}/versions` 请求字段包括 `versionName`、`title`、`changelog`、`minecraftVersions`、`loader`、`fileSizeBytes`、`checksumSha256`、`downloadEntry`、`releasedAt`、`reason` 和 `idempotencyKey`，成功返回 `201`，默认版本状态为 `ENABLED`，记录 `RESOURCE_VERSION_CREATED`。`downloadEntry` 字段包括 `provider`、`displayName`、`shareUrl` 或 `url`、`status`、`expiresAt` 和 `adminNote`。`PATCH /admin/items/{resourceId}/versions/{versionId}` 允许修改版本基础字段和下载入口，必须带 `reason`，成功记录 `RESOURCE_VERSION_UPDATED`。禁用和启用接口必须带 `reason`，成功分别记录 `RESOURCE_VERSION_DISABLED` 和 `RESOURCE_VERSION_ENABLED`；启用时必须存在 `ACTIVE` 下载入口。版本名冲突返回 `409/43611`，版本状态冲突返回 `409/43610`，下载入口不可用返回 `409/43613`。

后台分类接口包括 `GET /admin/categories`、`POST /admin/categories`、`PATCH /admin/categories/{categoryId}` 和 `PATCH /admin/categories/{categoryId}/archive`。`GET /admin/categories` 支持 `includeArchived`、`enabled` 和 `keyword`。`POST /admin/categories` 请求字段包括 `name`、`slug`、`description`、`icon`、`sortOrder`、`enabled`、`reason` 和 `idempotencyKey`，成功返回 `201` 并记录 `RESOURCE_CATEGORY_CREATED`。`PATCH /admin/categories/{categoryId}` 允许修改 `name`、`slug`、`description`、`icon`、`sortOrder` 和 `enabled`，必须带 `reason`，成功记录 `RESOURCE_CATEGORY_UPDATED`。`PATCH /admin/categories/{categoryId}/archive` 必须带 `reason`，成功记录 `RESOURCE_CATEGORY_ARCHIVED`；仍被非归档非删除资源使用的分类不能归档，返回 `409/43615`。分类不存在返回 `404/43601`，名称或 slug 冲突返回 `409/43611`。

审计接口为 `GET /admin/items/{resourceId}/audit-logs`，支持 `page`、`pageSize`、`action`、`from`、`to` 和 `sort`，`sort` 可为 `createdAt_desc` 和 `createdAt_asc`。审计字段至少包括 `id`、`requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`sourceIp`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason` 和 `createdAt`。审计记录不得泄露 `Authorization`、Cookie、token、`idempotencyKey`、下载票据、完整下载地址中的敏感参数、分享密码、Cloudreve 管理凭据、后台私有备注中的敏感片段、内部路径或节点运维参数。审计写入失败必须阻断对应写操作并返回 `500/51601`。

`GET /admin/ops/summary` 返回 `service`、`storageMode`、`authMode`、`profileMode`、`notificationMode`、`cloudreveMode`、`resourcesTotal`、`publishedResourcesTotal`、`versionsTotal`、`categoriesTotal`、`downloadEntriesTotal`、`downloadRecordsTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastAuditAt`、`lastDownloadAt`、`warnings`、`postgresTablesReady` 和 `productionGaps`。该接口不得包含下载 URL、分享密码、token、内部路径、后台备注、节点凭据、真实文件管理入口或运维命令。当前 PostgreSQL 前置闭环完成后 `storageMode` 可返回 `POSTGRESQL_WITH_IN_MEMORY_RESPONSE_MODEL`，旧内存契约测试仍可在无 JDBC 环境下返回 `IN_MEMORY`。

写接口幂等使用请求体内 `idempotencyKey`。同一操作者、同一作用域、同一幂等键和同一请求指纹必须返回原结果；同键不同指纹返回 `409/43612`。当前必须覆盖资源创建、版本创建、分类创建和下载票据创建的幂等重放与冲突；资源修改、状态流转、版本修改、版本启用禁用、分类修改和分类归档按同一规则落库。请求字段顺序不同但语义相同时应视为同一指纹。幂等记录作用域固定为 `resource.item.create`、`resource.version.create`、`resource.category.create` 和 `resource.download.create`。

错误码保持现有语义：字段校验失败返回 `400/40001`，分页非法返回 `400/40002`，排序非法返回 `400/40003`，未登录返回 `401/41000`，会话无效返回 `401/41001`，token 格式错误返回 `401/41003`，角色不足返回 `403/42001`，普通冲突返回 `409/43001`，通用幂等冲突保留 `409/43002`，resource 当前幂等冲突返回 `409/43612`，资源不存在或不可公开返回 `404/43600`，分类不存在返回 `404/43601`，版本不存在返回 `404/43602`，下载入口不存在返回 `404/43603`，资源或版本状态冲突返回 `409/43610`，slug、分类或版本名冲突返回 `409/43611`，下载入口不可用返回 `409/43613`，发布条件不足返回 `409/43614`，分类使用中返回 `409/43615`，auth 依赖不可用、超时或结构不兼容分别返回 `502/46600`、`504/46601` 和 `502/46602`，profile 依赖不可用、超时或结构不兼容分别返回 `502/46610`、`504/46611` 和 `502/46612`，notification 必需投递不可用或超时分别返回 `502/46620` 和 `504/46621`，Cloudreve 不可用或超时分别返回 `502/46630` 和 `504/46631`，模块内部错误返回 `500/51600`，审计写入失败返回 `500/51601`，下载记录写入失败返回 `500/51602`。

PostgreSQL 是 `resource` 正式持久化验收依据。成功写接口必须经 `SpringBootTest` 的 `RANDOM_PORT` 通过真实 HTTP 请求进入后端，覆盖请求接收、认证上下文解析、业务校验、内存响应模型更新、PostgreSQL 写入和响应返回。每个成功写接口必须用独立 SQL 查询验证 `resource_items`、`resource_versions`、`resource_categories`、`resource_download_entries` 或 `resource_download_records` 对应业务表，以及 `app_audit_logs` 和 `app_request_logs`；带 `idempotencyKey` 的接口还必须验证 `app_idempotency_records`。业务表写入、审计、幂等记录和请求日志必须在同一事务内提交，审计、幂等或请求日志写入失败必须阻断对应写操作，并在测试日志输出 `SQL evidence`。H2 只保留为旧 request database flow evidence，不作为 `resource` 正式持久化验收依据。

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
