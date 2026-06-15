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
