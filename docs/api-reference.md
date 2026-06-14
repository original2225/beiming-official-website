# 北冥官网 API 总文档

版本：1.0

本文档由当前模块化单体代码和 `docs/contracts-*.md` 契约重写生成。当前唯一后端 Maven 入口是 `backend/pom.xml`，本地联调默认入口统一为 `http://127.0.0.1:8135`，业务路径保持 `/api/v1/**` 原样。历史独立服务入口和历史端口不作为当前调用入口。

覆盖模块：api-gateway、business-core、admission-core、engagement-core、ops-core、portal-core、unified-backend、auth、profile、notification、content、server-status、resource、admin、onboarding、exam、whitelist、attendance、community、activity、calendar、changelog、ops-control、cloudreve-sync、backup-recovery、alerting、plugin-integration、cross-platform-notification、ops-image-market、guide、material、online-map。

统一验证命令是 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-common.md`

# 北冥官网公共 API 契约

版本：1.0

## 文档定位

本文档定义当前模块化单体后端所有接口共享的基础契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，本地联调默认入口统一为 `http://127.0.0.1:8135`，业务路径保持 `/api/v1/**` 原样。历史独立服务入口和历史端口只作为追溯字段、回滚引用或脱敏证据样板引用，不作为当前开发、联调、测试或前端调用入口。

本文档适用于官网公开页、用户中心、管理后台、运维控制台、统一后端入口、内置网关兼容面、全部业务模块和后续外部节点执行器控制面。各模块独立契约必须引用本文档，不得另起一套响应格式、认证方式、分页格式、权限模型、审计字段、状态模型或时间格式。

## API 路径

所有 HTTP 接口使用 `/api/v1` 作为版本前缀。模块继续保持原业务路径，例如 `/api/v1/auth`、`/api/v1/profile`、`/api/v1/resources`、`/api/v1/ops-control`、`/api/v1/guides`。统一后端只收敛运行入口，不把业务路径改写到 `/api/v1/unified-backend/<module>` 下。

公开接口可以不带认证。需要登录的接口必须校验 `Authorization: Bearer <token>`。后台接口必须额外校验基础角色和业务权限点。运维接口必须额外校验细粒度能力点、风险等级、二次确认和审批状态。

## 请求头

客户端可携带 `Authorization`、`Content-Type`、`Accept-Language` 和 `X-Request-Id`。没有传入 `X-Request-Id` 时，后端入口生成请求编号，并在响应头和响应体调试字段中保持一致。后台和运维高风险操作还需要携带二次确认凭据、审批编号或操作原因，具体字段由模块契约定义。

## 统一成功响应

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

创建成功使用 HTTP `201`。普通读取和修改成功使用 HTTP `200`。归档、下架、撤回、取消等动作可以返回 `data: null`，也可以返回动作后的资源摘要，具体以模块契约为准。

## 统一错误响应

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

`code` 使用业务错误码。`message` 给前端展示或调试使用。`errors` 用于字段级校验错误。`requestId` 必须和服务端日志中的请求编号一致。

## 错误码分段

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

## 分页格式

分页请求统一使用 `page` 和 `pageSize`。`page` 从 `1` 开始。`pageSize` 默认 `20`，未特殊声明时最大 `100`。分页响应统一放在 `data` 中。

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

## 时间、ID 和枚举

所有时间字段使用 ISO 8601 字符串。业务 ID 对外使用字符串。枚举值使用大写英文和下划线，例如 `OWNER`、`PENDING_REVIEW`、`ARCHIVED`。

## 基础角色与能力点

基础角色固定为 `OWNER`、`ADMIN`、`HELPER`、`USER`。运维能力点包括 `NODE_READ`、`NODE_WRITE`、`CONTAINER_OPERATE`、`VM_OPERATE`、`FILE_MANAGE`、`TERMINAL_ACCESS`、`HIGH_RISK_APPROVE`。基础角色不自动等于全部运维能力。

## 通用状态模型

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

模块可以扩展状态，但必须在模块契约中说明允许流转、通知和审计要求。

## 审计字段

后台关键操作和高风险操作必须写审计。审计字段至少包含 `id`、`requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`sourceIp`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason` 和 `createdAt`。

## 验收口径

任意模块进入实现或变更前，必须先更新对应 `docs/contracts-<module>.md`。当前后端统一验证命令是 `mvn -q -f backend/pom.xml test`。正式接口不得恢复旧独立 Maven 入口，不得把前端调用地址写回历史端口，不得绕过统一响应、统一错误、分页、认证、权限和审计要求。


---

来源：`docs/contracts-overview.md`

# 北冥官网 API 契约总览

版本：1.0

## 文档定位

本文档汇总当前统一后端内全部正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，也是当前唯一后端 Maven 启动入口；本地后端服务端口是 `8135`，本地联调默认入口统一为 `http://127.0.0.1:8135`，业务路径保持 `/api/v1/**` 原样。各模块源码已经由 `backend/src/main/java` 下的统一 Spring Boot 工程编译和运行，不再按旧独立 Maven 入口推进。

## 模块清单

| 模块 | 路径前缀 | 当前运行方式 | 路由数 | 契约文件 |
| --- | --- | --- | --- | --- |
| `api-gateway` | `/api/v1/gateway` | `unified-backend` in `backend:8135` | 9 | `docs/contracts-api-gateway.md` |
| `business-core` | `/api/v1/business-core` | `unified-backend` in `backend:8135` | 3 | `docs/contracts-business-core.md` |
| `admission-core` | `/api/v1/admission-core` | `unified-backend` in `backend:8135` | 2 | `docs/contracts-admission-core.md` |
| `engagement-core` | `/api/v1/engagement-core` | `unified-backend` in `backend:8135` | 3 | `docs/contracts-engagement-core.md` |
| `ops-core` | `/api/v1/ops-core` | `unified-backend` in `backend:8135` | 5 | `docs/contracts-ops-core.md` |
| `portal-core` | `/api/v1/portal-core` | `unified-backend` in `backend:8135` | 5 | `docs/contracts-portal-core.md` |
| `unified-backend` | `/api/v1/unified-backend` | `unified-backend` in `backend:8135` | 5 | `docs/contracts-unified-backend.md` |
| `auth` | `/api/v1/auth` | `business-core` in `backend:8135` | 20 | `docs/contracts-auth.md` |
| `profile` | `/api/v1/profile` | `business-core` in `backend:8135` | 16 | `docs/contracts-profile.md` |
| `notification` | `/api/v1/notifications` | `business-core` in `backend:8135` | 19 | `docs/contracts-notification.md` |
| `content` | `/api/v1/content` | `business-core` in `backend:8135` | 55 | `docs/contracts-content.md` |
| `server-status` | `/api/v1/server-status` | `business-core` in `backend:8135` | 25 | `docs/contracts-server-status.md` |
| `resource` | `/api/v1/resources` | `business-core` in `backend:8135` | 29 | `docs/contracts-resource.md` |
| `admin` | `/api/v1/admin` | `business-core` in `backend:8135` | 10 | `docs/contracts-admin.md` |
| `onboarding` | `/api/v1/onboarding` | `admission-core` in `backend:8135` | 15 | `docs/contracts-onboarding.md` |
| `exam` | `/api/v1/exams` | `admission-core` in `backend:8135` | 28 | `docs/contracts-exam.md` |
| `whitelist` | `/api/v1/whitelist` | `admission-core` in `backend:8135` | 20 | `docs/contracts-whitelist.md` |
| `attendance` | `/api/v1/attendance` | `admission-core` in `backend:8135` | 21 | `docs/contracts-attendance.md` |
| `community` | `/api/v1/community` | `engagement-core` in `backend:8135` | 64 | `docs/contracts-community.md` |
| `activity` | `/api/v1/activity` | `engagement-core` in `backend:8135` | 41 | `docs/contracts-activity.md` |
| `calendar` | `/api/v1/calendar` | `engagement-core` in `backend:8135` | 21 | `docs/contracts-calendar.md` |
| `changelog` | `/api/v1/changelog` | `engagement-core` in `backend:8135` | 23 | `docs/contracts-changelog.md` |
| `ops-control` | `/api/v1/ops-control` | `ops-core` in `backend:8135` | 31 | `docs/contracts-ops-control.md` |
| `cloudreve-sync` | `/api/v1/cloudreve-sync` | `ops-core` in `backend:8135` | 16 | `docs/contracts-cloudreve-sync.md` |
| `backup-recovery` | `/api/v1/backup-recovery` | `ops-core` in `backend:8135` | 25 | `docs/contracts-backup-recovery.md` |
| `alerting` | `/api/v1/alerting` | `ops-core` in `backend:8135` | 24 | `docs/contracts-alerting.md` |
| `plugin-integration` | `/api/v1/plugin-integration` | `ops-core` in `backend:8135` | 38 | `docs/contracts-plugin-integration.md` |
| `cross-platform-notification` | `/api/v1/cross-platform-notification` | `ops-core` in `backend:8135` | 36 | `docs/contracts-cross-platform-notification.md` |
| `ops-image-market` | `/api/v1/ops-image-market` | `ops-core` in `backend:8135` | 49 | `docs/contracts-ops-image-market.md` |
| `guide` | `/api/v1/guides` | `portal-core` in `backend:8135` | 41 | `docs/contracts-guide.md` |
| `material` | `/api/v1/materials` | `portal-core` in `backend:8135` | 33 | `docs/contracts-material.md` |
| `online-map` | `/api/v1/online-map` | `portal-core` in `backend:8135` | 34 | `docs/contracts-online-map.md` |

## 当前调用规则

前端、测试和本地联调只调用 `http://127.0.0.1:8135`。`api-gateway`、五个 core 和业务模块在当前进程内挂载。历史服务名、历史目录和历史端口只允许出现在受控退役、回滚引用、脱敏样板或运行态追溯字段中，不得作为新的 API 调用入口。

## 受控证据字段

统一后端 readiness 仍保留生产入口、旧入口退役、外部入口和审计观测相关门禁字段，前端和运维手册只能把这些字段展示为本地结构化证据或外部证据接收状态，不能展示成真实生产切流完成。

`apiGatewayControlledRetirementStatus` 当前为 `BLOCKED_BY_API_GATEWAY_RETIREMENT_RECEIPT_NOT_PROVIDED`，对应样板为 `docs/unified-backend-api-gateway-retirement-receipt-sample.json`。该字段只说明 `api-gateway-service` 退役收据结构存在，不证明真实生产入口已切换到 `backend:8135`。

`realProductionEntrypointCutoverStatus` 当前为 `BLOCKED_BY_REAL_PRODUCTION_ENTRYPOINT_CUTOVER_EVIDENCE_NOT_PROVIDED`，对应样板为 `docs/unified-backend-real-production-entrypoint-cutover-evidence-sample.json`。即使样板完整，`oldApiGatewayRetirementAllowed=false` 仍必须保持，`api-gateway-service` 仍只能作为历史回滚引用。

`externalEntrypointCutoverEvidenceIntakeStatus` 当前为 `BLOCKED_BY_EXTERNAL_ENTRYPOINT_CUTOVER_EVIDENCE_NOT_PROVIDED`，对应样板为 `docs/unified-backend-external-entrypoint-cutover-evidence-intake-sample.json`。该外部入口与切流证据接收门禁只接收 `frontendEntrypointRef`、`reverseProxyUpstreamRef`、`deploymentEntrypointRef`、`rollbackEntrypointRef`、`canaryWeightRef`、`observabilityRef` 和 `approvalRef` 等脱敏引用，不读取真实域名、token、连接串或 dashboard 地址。

`productionControlledCutoverStatus` 当前为 `BLOCKED_BY_REAL_CUTOVER_RECEIPT_NOT_PROVIDED`。`localApiGatewayEntrypointRetirementStatus` 当前为 `PASS_LOCAL_API_GATEWAY_ENTRYPOINT_RETIRED_UNIFIED_GATEWAY_APIS_PRESERVED`。这些字段不改变 `readyForProduction=false`、`readyToReplaceGateway=false`、`readyToRetireBusinessCore=false`、`readyToRetireAdmissionCore=false`、`readyToRetireEngagementCore=false`、`readyToRetireOpsCore=false`、`readyToRetirePortalCore=false` 和 `oldApiGatewayRetirementAllowed=false` 的保护结论。

前端环境变量继续使用 `VITE_API_BASE_URL` 指向 `http://127.0.0.1:8135`。`api-gateway:8125` 只保留为历史回滚引用，不作为当前接口基址。

## 变更规则

任一接口变更时，先更新对应 `docs/contracts-<module>.md`，再同步更新本文档、`docs/api-reference.md` 和前端 API 手册。实现和回归统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-api-gateway.md`

# 北冥官网 api-gateway API 契约

版本：1.0

## 文档定位

本文档是 `api-gateway` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/apigateway/GatewayModule.java`。`api-gateway` 在运行上由 `unified-backend` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`api-gateway` 负责统一后端内置网关兼容面，保留路由表、健康摘要、请求日志和历史网关行为对照。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 9 个 `api-gateway` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/gateway/health` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | `service`、`status`、`port`、`checkedAt` 等健康摘要字段 | LOW |
| `GET` | `/api/v1/gateway/admin/ops/summary` | HELPER、ADMIN 或 OWNER；API_GATEWAY_READ for GET；API_GATEWAY_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/gateway/admin/runtime-topology` | HELPER、ADMIN 或 OWNER；API_GATEWAY_READ for GET；API_GATEWAY_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/gateway/admin/routes` | HELPER、ADMIN 或 OWNER；API_GATEWAY_READ for GET；API_GATEWAY_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/gateway/admin/routes/{routeId}` | HELPER、ADMIN 或 OWNER；API_GATEWAY_READ for GET；API_GATEWAY_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `routeId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/gateway/admin/upstreams` | HELPER、ADMIN 或 OWNER；API_GATEWAY_READ for GET；API_GATEWAY_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/gateway/admin/upstreams/{serviceKey}/health-refresh` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；API_GATEWAY_READ for GET；API_GATEWAY_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `serviceKey`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/gateway/admin/request-logs` | HELPER、ADMIN 或 OWNER；API_GATEWAY_READ for GET；API_GATEWAY_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `ANY` | `/api/v1/**` | 已登录用户；后台路径另按角色校验；API_GATEWAY_SELF 或模块写权限 | 兼容代理请求，方法和请求体由被命中的业务路径决定 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`api-gateway` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-business-core.md`

# 北冥官网 business-core API 契约

版本：1.0

## 文档定位

本文档是 `business-core` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/core/BusinessCoreModule.java`。`business-core` 在运行上由 `unified-backend` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`business-core` 负责统一后端内的业务核心运行面，承载 auth、profile、notification、content、server-status、resource 和 admin 的健康与汇总。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 3 个 `business-core` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/business-core/health` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | `service`、`status`、`port`、`checkedAt` 等健康摘要字段 | LOW |
| `GET` | `/api/v1/business-core/admin/ops/summary` | HELPER、ADMIN 或 OWNER；BUSINESS_CORE_READ for GET；BUSINESS_CORE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/business-core/admin/production-readiness` | HELPER、ADMIN 或 OWNER；BUSINESS_CORE_READ for GET；BUSINESS_CORE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`business-core` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-admission-core.md`

# 北冥官网 admission-core API 契约

版本：1.0

## 文档定位

本文档是 `admission-core` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/admission/AdmissionCoreModule.java`。`admission-core` 在运行上由 `unified-backend` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`admission-core` 负责统一后端内的入服核心运行面，承载 onboarding、exam、whitelist 和 attendance 的健康与汇总。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 2 个 `admission-core` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/admission-core/health` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | `service`、`status`、`port`、`checkedAt` 等健康摘要字段 | LOW |
| `GET` | `/api/v1/admission-core/admin/ops/summary` | HELPER、ADMIN 或 OWNER；ADMISSION_CORE_READ for GET；ADMISSION_CORE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`admission-core` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-engagement-core.md`

# 北冥官网 engagement-core API 契约

版本：1.0

## 文档定位

本文档是 `engagement-core` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/engagement/EngagementCoreModule.java`。`engagement-core` 在运行上由 `unified-backend` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`engagement-core` 负责统一后端内的互动核心运行面，承载 community、activity、calendar 和 changelog 的健康与汇总。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 3 个 `engagement-core` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/engagement-core/health` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | `service`、`status`、`port`、`checkedAt` 等健康摘要字段 | LOW |
| `GET` | `/api/v1/engagement-core/admin/ops/summary` | HELPER、ADMIN 或 OWNER；ENGAGEMENT_CORE_READ for GET；ENGAGEMENT_CORE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/engagement-core/admin/production-readiness` | HELPER、ADMIN 或 OWNER；ENGAGEMENT_CORE_READ for GET；ENGAGEMENT_CORE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`engagement-core` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-ops-core.md`

# 北冥官网 ops-core API 契约

版本：1.0

## 文档定位

本文档是 `ops-core` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/opscore/OpsCoreModule.java`。`ops-core` 在运行上由 `unified-backend` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`ops-core` 负责统一后端内的运维核心运行面，承载 ops-control、cloudreve-sync、backup-recovery、alerting、plugin-integration、cross-platform-notification 和 ops-image-market 的健康、readiness 与 smoke。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 5 个 `ops-core` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/ops-core/health` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | `service`、`status`、`port`、`checkedAt` 等健康摘要字段 | LOW |
| `GET` | `/api/v1/ops-core/ops/summary` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/ops-core/admin/modules` | HELPER、ADMIN 或 OWNER；OPS_CORE_READ for GET；OPS_CORE_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/ops-core/admin/readiness` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_CORE_READ for GET；OPS_CORE_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | readiness 矩阵、阻断项、证据引用和生产替换保护字段 | LOW |
| `POST` | `/api/v1/ops-core/admin/http-smoke/run` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_CORE_READ for GET；OPS_CORE_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`ops-core` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-portal-core.md`

# 北冥官网 portal-core API 契约

版本：1.0

## 文档定位

本文档是 `portal-core` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/portalcore/PortalCoreModule.java`。`portal-core` 在运行上由 `unified-backend` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`portal-core` 负责统一后端内的门户核心运行面，承载 guide、material 和 online-map 的健康、readiness 与 smoke。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 5 个 `portal-core` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/portal-core/health` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | `service`、`status`、`port`、`checkedAt` 等健康摘要字段 | LOW |
| `GET` | `/api/v1/portal-core/ops/summary` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/portal-core/admin/modules` | HELPER、ADMIN 或 OWNER；PORTAL_CORE_READ for GET；PORTAL_CORE_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/portal-core/admin/readiness` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PORTAL_CORE_READ for GET；PORTAL_CORE_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | readiness 矩阵、阻断项、证据引用和生产替换保护字段 | LOW |
| `POST` | `/api/v1/portal-core/admin/http-smoke/run` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PORTAL_CORE_READ for GET；PORTAL_CORE_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`portal-core` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-unified-backend.md`

# 北冥官网 unified-backend API 契约

版本：1.0

## 文档定位

本文档是 `unified-backend` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/unifiedbackend/UnifiedBackendModule.java`。`unified-backend` 在运行上由 `unified-backend` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`unified-backend` 负责当前唯一后端 Maven 入口，负责统一健康、挂载清单、readiness 和全量 HTTP smoke。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 5 个 `unified-backend` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/unified-backend/health` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | `service`、`status`、`port`、`checkedAt` 等健康摘要字段 | LOW |
| `GET` | `/api/v1/unified-backend/admin/ops/summary` | HELPER、ADMIN 或 OWNER；UNIFIED_BACKEND_READ for GET；UNIFIED_BACKEND_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/unified-backend/admin/mounts` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；UNIFIED_BACKEND_READ for GET；UNIFIED_BACKEND_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一后端 in-process 挂载清单，包含 `routeId`、`serviceKey`、`pathPrefix`、`mountDisposition` | LOW |
| `GET` | `/api/v1/unified-backend/admin/readiness` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；UNIFIED_BACKEND_READ for GET；UNIFIED_BACKEND_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | readiness 矩阵、阻断项、证据引用和生产替换保护字段 | LOW |
| `POST` | `/api/v1/unified-backend/admin/http-smoke/run` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；UNIFIED_BACKEND_READ for GET；UNIFIED_BACKEND_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`unified-backend` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-auth.md`

# 北冥官网 auth API 契约

版本：1.0

## 文档定位

本文档是 `auth` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/auth/AuthModule.java`。`auth` 在运行上由 `business-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`auth` 负责账号、会话、角色、邀请码、密码和 Minecraft 绑定。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 20 个 `auth` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | 已登录用户；后台路径另按角色校验；AUTH_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/auth/login` | 已登录用户；后台路径另按角色校验；AUTH_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/auth/logout` | 已登录用户；后台路径另按角色校验；AUTH_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/auth/me` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；AUTH_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/auth/session/verify` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/auth/me/sessions` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；AUTH_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `DELETE` | `/api/v1/auth/me/sessions/{sessionId}` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；AUTH_SELF | 路径参数 `sessionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `POST` | `/api/v1/auth/me/password` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；AUTH_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/auth/password-reset/request` | 已登录用户；后台路径另按角色校验；AUTH_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/auth/password-reset/confirm` | 已登录用户；后台路径另按角色校验；AUTH_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PUT` | `/api/v1/auth/me/minecraft-binding` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；AUTH_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `DELETE` | `/api/v1/auth/me/minecraft-binding` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；AUTH_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `GET` | `/api/v1/auth/admin/users` | HELPER、ADMIN 或 OWNER；AUTH_READ for GET；AUTH_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/auth/admin/users/{userId}` | HELPER、ADMIN 或 OWNER；AUTH_READ for GET；AUTH_WRITE for mutation | 路径参数 `userId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/auth/admin/users/{userId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；AUTH_READ for GET；AUTH_WRITE for mutation | 路径参数 `userId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PUT` | `/api/v1/auth/admin/users/{userId}/roles` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；AUTH_READ for GET；AUTH_WRITE for mutation | 路径参数 `userId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/auth/admin/invitations` | HELPER、ADMIN 或 OWNER；AUTH_READ for GET；AUTH_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/auth/admin/invitations` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；AUTH_READ for GET；AUTH_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/auth/admin/invitations/{invitationId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；AUTH_READ for GET；AUTH_WRITE for mutation | 路径参数 `invitationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/auth/admin/invitations/{invitationId}/usage-records` | HELPER、ADMIN 或 OWNER；AUTH_READ for GET；AUTH_WRITE for mutation | 路径参数 `invitationId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`auth` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-profile.md`

# 北冥官网 profile API 契约

版本：1.0

## 文档定位

本文档是 `profile` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/profile/ProfileModule.java`。`profile` 在运行上由 `business-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`profile` 负责成员公开档案、当前用户档案、成员组、事迹和作品快照。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 16 个 `profile` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/profile/members` | 公开或按接口内部规则；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/profile/members/{memberId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `memberId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/profile/me` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；PROFILE_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `PATCH` | `/api/v1/profile/me` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；PROFILE_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/profile/admin/members` | HELPER、ADMIN 或 OWNER；PROFILE_READ for GET；PROFILE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/profile/admin/members/{memberId}` | HELPER、ADMIN 或 OWNER；PROFILE_READ for GET；PROFILE_WRITE for mutation | 路径参数 `memberId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/profile/admin/members/activate` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PROFILE_READ for GET；PROFILE_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/profile/admin/members/{memberId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PROFILE_READ for GET；PROFILE_WRITE for mutation | 路径参数 `memberId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/profile/admin/members/{memberId}/status` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PROFILE_READ for GET；PROFILE_WRITE for mutation | 路径参数 `memberId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/profile/admin/groups` | HELPER、ADMIN 或 OWNER；PROFILE_READ for GET；PROFILE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/profile/admin/groups` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PROFILE_READ for GET；PROFILE_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/profile/admin/groups/{groupId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PROFILE_READ for GET；PROFILE_WRITE for mutation | 路径参数 `groupId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/profile/admin/groups/{groupId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PROFILE_READ for GET；PROFILE_WRITE for mutation | 路径参数 `groupId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PUT` | `/api/v1/profile/admin/members/{memberId}/milestones` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PROFILE_READ for GET；PROFILE_WRITE for mutation | 路径参数 `memberId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PUT` | `/api/v1/profile/admin/members/{memberId}/work-snapshots` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PROFILE_READ for GET；PROFILE_WRITE for mutation | 路径参数 `memberId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/profile/admin/members/{memberId}/audit-logs` | HELPER、ADMIN 或 OWNER；PROFILE_READ for GET；PROFILE_WRITE for mutation | 路径参数 `memberId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`profile` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-notification.md`

# 北冥官网 notification API 契约

版本：1.0

## 文档定位

本文档是 `notification` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/notification/NotificationModule.java`。`notification` 在运行上由 `business-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`notification` 负责站内通知、未读数、消息归档和通知模板。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 19 个 `notification` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/notifications/me` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；NOTIFICATION_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/notifications/me/unread-count` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；NOTIFICATION_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/notifications/me/{notificationId}` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；NOTIFICATION_SELF | 路径参数 `notificationId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/notifications/me/{notificationId}/read` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；NOTIFICATION_SELF | 路径参数 `notificationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/notifications/me/read-all` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；NOTIFICATION_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/notifications/me/{notificationId}/archive` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；NOTIFICATION_SELF | 路径参数 `notificationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/notifications/admin/messages` | HELPER、ADMIN 或 OWNER；NOTIFICATION_READ for GET；NOTIFICATION_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/notifications/admin/messages/{notificationId}` | HELPER、ADMIN 或 OWNER；NOTIFICATION_READ for GET；NOTIFICATION_WRITE for mutation | 路径参数 `notificationId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/notifications/admin/messages` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；NOTIFICATION_READ for GET；NOTIFICATION_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/notifications/admin/messages/from-template` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；NOTIFICATION_READ for GET；NOTIFICATION_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/notifications/admin/templates` | HELPER、ADMIN 或 OWNER；NOTIFICATION_READ for GET；NOTIFICATION_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/notifications/admin/templates/{templateId}` | HELPER、ADMIN 或 OWNER；NOTIFICATION_READ for GET；NOTIFICATION_WRITE for mutation | 路径参数 `templateId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/notifications/admin/templates/preview` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；NOTIFICATION_READ for GET；NOTIFICATION_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/notifications/admin/templates` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；NOTIFICATION_READ for GET；NOTIFICATION_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/notifications/admin/templates/{templateId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；NOTIFICATION_READ for GET；NOTIFICATION_WRITE for mutation | 路径参数 `templateId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/notifications/admin/templates/{templateId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；NOTIFICATION_READ for GET；NOTIFICATION_WRITE for mutation | 路径参数 `templateId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/notifications/admin/templates/{templateId}/enable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；NOTIFICATION_READ for GET；NOTIFICATION_WRITE for mutation | 路径参数 `templateId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/notifications/admin/messages/{notificationId}/audit-logs` | HELPER、ADMIN 或 OWNER；NOTIFICATION_READ for GET；NOTIFICATION_WRITE for mutation | 路径参数 `notificationId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/notifications/admin/ops/summary` | HELPER、ADMIN 或 OWNER；NOTIFICATION_READ for GET；NOTIFICATION_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`notification` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-content.md`

# 北冥官网 content API 契约

版本：1.0

## 文档定位

本文档是 `content` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/content/ContentModule.java`。`content` 在运行上由 `business-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`content` 负责官网首页、公告文章、专题、分类、标签、SEO 和内容版本。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 55 个 `content` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/content/home` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/content/items` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/content/items/{contentId}/preview` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `contentId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/content/items/{contentId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `contentId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/content/items/by-slug/{slug}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `slug`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/content/categories` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/content/tags` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/content/topics` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/content/topics/{topicId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `topicId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/content/topics/by-slug/{slug}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `slug`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/content/seo` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/content/seo/sitemap` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/content/admin/items` | HELPER、ADMIN 或 OWNER；CONTENT_READ for GET；CONTENT_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/content/admin/items/{contentId}` | HELPER、ADMIN 或 OWNER；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `contentId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/content/admin/items` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/items/{contentId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `contentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/content/admin/items/{contentId}/preview-token` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `contentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/items/{contentId}/submit-review` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `contentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/items/{contentId}/approve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `contentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/content/admin/items/{contentId}/reject` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `contentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/items/{contentId}/request-changes` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `contentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/items/{contentId}/publish` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `contentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/items/{contentId}/offline` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `contentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/items/{contentId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `contentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/items/{contentId}/delete` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `contentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `GET` | `/api/v1/content/admin/items/{contentId}/versions` | HELPER、ADMIN 或 OWNER；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `contentId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/content/admin/items/{contentId}/versions/{version}` | HELPER、ADMIN 或 OWNER；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `contentId, version`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/content/admin/items/{contentId}/versions/{version}/restore` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `contentId, version`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/content/admin/items/{contentId}/audit-logs` | HELPER、ADMIN 或 OWNER；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `contentId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/content/admin/home` | HELPER、ADMIN 或 OWNER；CONTENT_READ for GET；CONTENT_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `PUT` | `/api/v1/content/admin/home` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/content/admin/home/preview` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/home/publish` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/home/rollback` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/content/admin/categories` | HELPER、ADMIN 或 OWNER；CONTENT_READ for GET；CONTENT_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/content/admin/categories` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/categories/{categoryId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `categoryId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/categories/{categoryId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `categoryId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/content/admin/tags` | HELPER、ADMIN 或 OWNER；CONTENT_READ for GET；CONTENT_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/content/admin/tags` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/tags/{tagId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `tagId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/tags/{tagId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `tagId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/content/admin/topics` | HELPER、ADMIN 或 OWNER；CONTENT_READ for GET；CONTENT_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/content/admin/topics/{topicId}` | HELPER、ADMIN 或 OWNER；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `topicId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/content/admin/topics` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/topics/{topicId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `topicId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/topics/{topicId}/publish` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `topicId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/topics/{topicId}/offline` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `topicId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/topics/{topicId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `topicId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/topics/{topicId}/delete` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `topicId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `GET` | `/api/v1/content/admin/seo` | HELPER、ADMIN 或 OWNER；CONTENT_READ for GET；CONTENT_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/content/admin/seo/{seoId}` | HELPER、ADMIN 或 OWNER；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `seoId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PUT` | `/api/v1/content/admin/seo/by-route` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/content/admin/seo/{seoId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CONTENT_READ for GET；CONTENT_WRITE for mutation | 路径参数 `seoId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/content/admin/ops/summary` | HELPER、ADMIN 或 OWNER；CONTENT_READ for GET；CONTENT_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`content` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-server-status.md`

# 北冥官网 server-status API 契约

版本：1.0

## 文档定位

本文档是 `server-status` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/serverstatus/ServerStatusModule.java`。`server-status` 在运行上由 `business-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`server-status` 负责玩家可见 Minecraft 状态、线路、历史快照和宕机记录。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 25 个 `server-status` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/server-status/overview` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/server-status/instances` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/server-status/instances/{instanceId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `instanceId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/server-status/lines` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/server-status/history/snapshots` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/server-status/outages` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/server-status/admin/sources` | HELPER、ADMIN 或 OWNER；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/server-status/admin/sources` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/server-status/admin/sources/{sourceId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 路径参数 `sourceId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/server-status/admin/sources/{sourceId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 路径参数 `sourceId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/server-status/admin/sources/{sourceId}/enable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 路径参数 `sourceId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/server-status/admin/sources/{sourceId}/refresh` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 路径参数 `sourceId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/server-status/admin/lines` | HELPER、ADMIN 或 OWNER；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/server-status/admin/lines` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/server-status/admin/lines/{lineId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 路径参数 `lineId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/server-status/admin/lines/{lineId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 路径参数 `lineId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/server-status/admin/lines/{lineId}/enable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 路径参数 `lineId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/server-status/admin/outages` | HELPER、ADMIN 或 OWNER；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/server-status/admin/outages` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/server-status/admin/outages/{outageId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 路径参数 `outageId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/server-status/admin/outages/{outageId}/acknowledge` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 路径参数 `outageId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/server-status/admin/outages/{outageId}/resolve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 路径参数 `outageId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/server-status/admin/outages/{outageId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 路径参数 `outageId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/server-status/admin/audit-logs` | HELPER、ADMIN 或 OWNER；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/server-status/admin/ops/summary` | HELPER、ADMIN 或 OWNER；SERVER_STATUS_READ for GET；SERVER_STATUS_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`server-status` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-resource.md`

# 北冥官网 resource API 契约

版本：1.0

## 文档定位

本文档是 `resource` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/resource/ResourceModule.java`。`resource` 在运行上由 `business-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`resource` 负责玩家可见资源、版本、分类、下载票据和 Cloudreve 分享链接。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 29 个 `resource` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/resources` | 公开或按接口内部规则；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/resources/{resourceId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `resourceId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/resources/by-slug/{slug}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `slug`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/resources/categories` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/resources/{resourceId}/versions` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `resourceId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/resources/{resourceId}/versions/{versionId}/download` | 已登录用户；后台路径另按角色校验；RESOURCE_SELF 或模块写权限 | 路径参数 `resourceId, versionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/resources/admin/items` | HELPER、ADMIN 或 OWNER；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/resources/admin/items/{resourceId}` | HELPER、ADMIN 或 OWNER；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/resources/admin/items` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/resources/admin/items/{resourceId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/resources/admin/items/{resourceId}/submit-review` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/resources/admin/items/{resourceId}/approve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/resources/admin/items/{resourceId}/reject` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/resources/admin/items/{resourceId}/request-changes` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/resources/admin/items/{resourceId}/publish` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/resources/admin/items/{resourceId}/offline` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/resources/admin/items/{resourceId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/resources/admin/items/{resourceId}/delete` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `GET` | `/api/v1/resources/admin/items/{resourceId}/versions` | HELPER、ADMIN 或 OWNER；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/resources/admin/items/{resourceId}/versions` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/resources/admin/items/{resourceId}/versions/{versionId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId, versionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/resources/admin/items/{resourceId}/versions/{versionId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId, versionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/resources/admin/items/{resourceId}/versions/{versionId}/enable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId, versionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/resources/admin/categories` | HELPER、ADMIN 或 OWNER；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/resources/admin/categories` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/resources/admin/categories/{categoryId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `categoryId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/resources/admin/categories/{categoryId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `categoryId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/resources/admin/items/{resourceId}/audit-logs` | HELPER、ADMIN 或 OWNER；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 路径参数 `resourceId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/resources/admin/ops/summary` | HELPER、ADMIN 或 OWNER；RESOURCE_READ for GET；RESOURCE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`resource` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-admin.md`

# 北冥官网 admin API 契约

版本：1.0

## 文档定位

本文档是 `admin` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/admin/AdminModule.java`。`admin` 在运行上由 `business-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`admin` 负责后台聚合首页、模块入口、待办、指标、设置和审计索引。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 10 个 `admin` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/admin/overview` | HELPER、ADMIN 或 OWNER；ADMIN_READ for GET；ADMIN_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/admin/modules` | HELPER、ADMIN 或 OWNER；ADMIN_READ for GET；ADMIN_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/admin/modules/{moduleKey}` | HELPER、ADMIN 或 OWNER；ADMIN_READ for GET；ADMIN_WRITE for mutation | 路径参数 `moduleKey`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/admin/todos` | HELPER、ADMIN 或 OWNER；ADMIN_READ for GET；ADMIN_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/admin/todos/{todoId}` | HELPER、ADMIN 或 OWNER；ADMIN_READ for GET；ADMIN_WRITE for mutation | 路径参数 `todoId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/admin/metrics/summary` | HELPER、ADMIN 或 OWNER；ADMIN_READ for GET；ADMIN_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/admin/audit-logs` | HELPER、ADMIN 或 OWNER；ADMIN_READ for GET；ADMIN_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/admin/settings` | HELPER、ADMIN 或 OWNER；ADMIN_READ for GET；ADMIN_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `PATCH` | `/api/v1/admin/settings` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ADMIN_READ for GET；ADMIN_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/admin/ops/summary` | HELPER、ADMIN 或 OWNER；ADMIN_READ for GET；ADMIN_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`admin` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-onboarding.md`

# 北冥官网 onboarding API 契约

版本：1.0

## 文档定位

本文档是 `onboarding` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/onboarding/OnboardingModule.java`。`onboarding` 在运行上由 `admission-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`onboarding` 负责入服流程进度、资料确认、规则确认、方向选择和审核接续。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 15 个 `onboarding` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/onboarding/me/progress` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ONBOARDING_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/onboarding/me/start` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ONBOARDING_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/onboarding/me/profile-confirmation` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ONBOARDING_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/onboarding/me/rules-confirmation` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ONBOARDING_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/onboarding/me/direction` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ONBOARDING_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/onboarding/me/advance` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ONBOARDING_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/onboarding/me/next-action` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ONBOARDING_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/onboarding/admin/applications` | HELPER、ADMIN 或 OWNER；ONBOARDING_READ for GET；ONBOARDING_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/onboarding/admin/applications/{applicationId}` | HELPER、ADMIN 或 OWNER；ONBOARDING_READ for GET；ONBOARDING_WRITE for mutation | 路径参数 `applicationId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/onboarding/admin/applications/{applicationId}/exam-handoff` | HELPER、ADMIN 或 OWNER；ONBOARDING_READ for GET；ONBOARDING_WRITE for mutation | 路径参数 `applicationId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/onboarding/admin/applications/{applicationId}/reset` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONBOARDING_READ for GET；ONBOARDING_WRITE for mutation | 路径参数 `applicationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/onboarding/admin/applications/{applicationId}/block` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONBOARDING_READ for GET；ONBOARDING_WRITE for mutation | 路径参数 `applicationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/onboarding/admin/applications/{applicationId}/unblock` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONBOARDING_READ for GET；ONBOARDING_WRITE for mutation | 路径参数 `applicationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/onboarding/admin/audit-logs` | HELPER、ADMIN 或 OWNER；ONBOARDING_READ for GET；ONBOARDING_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/onboarding/admin/ops/summary` | HELPER、ADMIN 或 OWNER；ONBOARDING_READ for GET；ONBOARDING_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`onboarding` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-exam.md`

# 北冥官网 exam API 契约

版本：1.0

## 文档定位

本文档是 `exam` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/exam/ExamModule.java`。`exam` 在运行上由 `admission-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`exam` 负责考试会话、试卷、答题、提交、人工阅卷、题库和试卷模板。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 28 个 `exam` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/v1/exams/me/sessions` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；EXAM_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/exams/me/sessions/current` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；EXAM_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/exams/me/sessions` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；EXAM_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/exams/me/sessions/{sessionId}/paper` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；EXAM_SELF | 路径参数 `sessionId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PUT` | `/api/v1/exams/me/sessions/{sessionId}/answers` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；EXAM_SELF | 路径参数 `sessionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/exams/me/sessions/{sessionId}/submit` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；EXAM_SELF | 路径参数 `sessionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/exams/me/sessions/{sessionId}/supplement` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；EXAM_SELF | 路径参数 `sessionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/exams/me/sessions/{sessionId}/result` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；EXAM_SELF | 路径参数 `sessionId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/exams/admin/sessions` | HELPER、ADMIN 或 OWNER；EXAM_READ for GET；EXAM_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/exams/admin/sessions/{sessionId}` | HELPER、ADMIN 或 OWNER；EXAM_READ for GET；EXAM_WRITE for mutation | 路径参数 `sessionId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/exams/admin/sessions/{sessionId}/manual-review` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；EXAM_READ for GET；EXAM_WRITE for mutation | 路径参数 `sessionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/exams/admin/sessions/{sessionId}/result-correction` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；EXAM_READ for GET；EXAM_WRITE for mutation | 路径参数 `sessionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/exams/admin/sessions/{sessionId}/request-supplement` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；EXAM_READ for GET；EXAM_WRITE for mutation | 路径参数 `sessionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/exams/admin/sessions/{sessionId}/cancel` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；EXAM_READ for GET；EXAM_WRITE for mutation | 路径参数 `sessionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/exams/admin/sessions/{sessionId}/whitelist-handoff` | HELPER、ADMIN 或 OWNER；EXAM_READ for GET；EXAM_WRITE for mutation | 路径参数 `sessionId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/exams/admin/question-bank/questions` | HELPER、ADMIN 或 OWNER；EXAM_READ for GET；EXAM_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/exams/admin/question-bank/questions/{questionId}/versions` | HELPER、ADMIN 或 OWNER；EXAM_READ for GET；EXAM_WRITE for mutation | 路径参数 `questionId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/exams/admin/question-bank/questions` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；EXAM_READ for GET；EXAM_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/exams/admin/question-bank/questions/{questionId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；EXAM_READ for GET；EXAM_WRITE for mutation | 路径参数 `questionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/exams/admin/question-bank/questions/{questionId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；EXAM_READ for GET；EXAM_WRITE for mutation | 路径参数 `questionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/exams/admin/paper-templates` | HELPER、ADMIN 或 OWNER；EXAM_READ for GET；EXAM_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/exams/admin/paper-templates` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；EXAM_READ for GET；EXAM_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/exams/admin/paper-templates/{templateId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；EXAM_READ for GET；EXAM_WRITE for mutation | 路径参数 `templateId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/exams/admin/paper-templates/{templateId}/publish-preview` | HELPER、ADMIN 或 OWNER；EXAM_READ for GET；EXAM_WRITE for mutation | 路径参数 `templateId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/exams/admin/paper-templates/{templateId}/publish` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；EXAM_READ for GET；EXAM_WRITE for mutation | 路径参数 `templateId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/exams/admin/paper-templates/{templateId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；EXAM_READ for GET；EXAM_WRITE for mutation | 路径参数 `templateId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/exams/admin/audit-logs` | HELPER、ADMIN 或 OWNER；EXAM_READ for GET；EXAM_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/exams/admin/ops/summary` | HELPER、ADMIN 或 OWNER；EXAM_READ for GET；EXAM_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`exam` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-whitelist.md`

# 北冥官网 whitelist API 契约

版本：1.0

## 文档定位

本文档是 `whitelist` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/whitelist/WhitelistModule.java`。`whitelist` 在运行上由 `admission-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`whitelist` 负责白名单申请、补充、撤回、审核、移除、重开和考勤交接。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 20 个 `whitelist` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/v1/whitelist/me/applications` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；WHITELIST_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/whitelist/me/applications/current` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；WHITELIST_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/whitelist/me/applications` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；WHITELIST_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/whitelist/me/applications/{applicationId}` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；WHITELIST_SELF | 路径参数 `applicationId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/whitelist/me/applications/{applicationId}` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；WHITELIST_SELF | 路径参数 `applicationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/whitelist/me/applications/{applicationId}/submit` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；WHITELIST_SELF | 路径参数 `applicationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/whitelist/me/applications/{applicationId}/supplement` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；WHITELIST_SELF | 路径参数 `applicationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/whitelist/me/applications/{applicationId}/withdraw` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；WHITELIST_SELF | 路径参数 `applicationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/whitelist/me/applications/{applicationId}/result` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；WHITELIST_SELF | 路径参数 `applicationId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/whitelist/admin/applications` | HELPER、ADMIN 或 OWNER；WHITELIST_READ for GET；WHITELIST_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/whitelist/admin/applications/{applicationId}` | HELPER、ADMIN 或 OWNER；WHITELIST_READ for GET；WHITELIST_WRITE for mutation | 路径参数 `applicationId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/whitelist/admin/applications/{applicationId}/assign` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；WHITELIST_READ for GET；WHITELIST_WRITE for mutation | 路径参数 `applicationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/whitelist/admin/applications/{applicationId}/request-supplement` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；WHITELIST_READ for GET；WHITELIST_WRITE for mutation | 路径参数 `applicationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/whitelist/admin/applications/{applicationId}/approve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；WHITELIST_READ for GET；WHITELIST_WRITE for mutation | 路径参数 `applicationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/whitelist/admin/applications/{applicationId}/reject` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；WHITELIST_READ for GET；WHITELIST_WRITE for mutation | 路径参数 `applicationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/whitelist/admin/applications/{applicationId}/remove` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；WHITELIST_READ for GET；WHITELIST_WRITE for mutation | 路径参数 `applicationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `POST` | `/api/v1/whitelist/admin/applications/{applicationId}/reopen` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；WHITELIST_READ for GET；WHITELIST_WRITE for mutation | 路径参数 `applicationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/whitelist/admin/applications/{applicationId}/attendance-handoff` | HELPER、ADMIN 或 OWNER；WHITELIST_READ for GET；WHITELIST_WRITE for mutation | 路径参数 `applicationId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/whitelist/admin/audit-logs` | HELPER、ADMIN 或 OWNER；WHITELIST_READ for GET；WHITELIST_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/whitelist/admin/ops/summary` | HELPER、ADMIN 或 OWNER；WHITELIST_READ for GET；WHITELIST_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`whitelist` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-attendance.md`

# 北冥官网 attendance API 契约

版本：1.0

## 文档定位

本文档是 `attendance` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/attendance/AttendanceModule.java`。`attendance` 在运行上由 `admission-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`attendance` 负责考勤账户、积分流水、贡献记录、月度扣分、移除候选和榜单。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 21 个 `attendance` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/attendance/leaderboard` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/attendance/me/account` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ATTENDANCE_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/attendance/me/ledger` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ATTENDANCE_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/attendance/me/contributions` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ATTENDANCE_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/attendance/me/ranking` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ATTENDANCE_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/attendance/admin/accounts` | HELPER、ADMIN 或 OWNER；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/attendance/admin/accounts/{accountId}` | HELPER、ADMIN 或 OWNER；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | 路径参数 `accountId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/attendance/admin/initializations` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/attendance/admin/accounts/{accountId}/adjustments` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | 路径参数 `accountId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/attendance/admin/ledger/{ledgerId}/reverse` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | 路径参数 `ledgerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/attendance/admin/contributions` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/attendance/admin/contributions/{contributionId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | 路径参数 `contributionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/attendance/admin/monthly-runs/preview` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/attendance/admin/monthly-runs` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/attendance/admin/monthly-runs/{runId}` | HELPER、ADMIN 或 OWNER；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | 路径参数 `runId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/attendance/admin/removal-candidates` | HELPER、ADMIN 或 OWNER；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `PATCH` | `/api/v1/attendance/admin/removal-candidates/{candidateId}/confirm` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | 路径参数 `candidateId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/attendance/admin/removal-candidates/{candidateId}/dismiss` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | 路径参数 `candidateId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/attendance/admin/leaderboard/rebuild` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/attendance/admin/audit-logs` | HELPER、ADMIN 或 OWNER；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/attendance/admin/ops/summary` | HELPER、ADMIN 或 OWNER；ATTENDANCE_READ for GET；ATTENDANCE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`attendance` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-community.md`

# 北冥官网 community API 契约

版本：1.0

## 文档定位

本文档是 `community` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/community/CommunityModule.java`。`community` 在运行上由 `engagement-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`community` 负责板块、帖子、评论、点赞、收藏、投票、举报、工单和处罚。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 64 个 `community` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/community/boards` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/community/boards/{boardId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `boardId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/community/posts` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/community/posts/{postId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `postId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/community/posts/{postId}/comments` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `postId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/community/polls/{pollId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `pollId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/community/search` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/community/me/posts` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/me/posts/{postId}` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/me/posts/{postId}/submit` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/me/posts/{postId}/withdraw` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/me/posts/{postId}/comments` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/me/comments/{commentId}` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/me/comments/{commentId}/archive` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/me/posts/{postId}/like` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `DELETE` | `/api/v1/community/me/posts/{postId}/like` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `POST` | `/api/v1/community/me/comments/{commentId}/like` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `DELETE` | `/api/v1/community/me/comments/{commentId}/like` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `POST` | `/api/v1/community/me/posts/{postId}/favorite` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `DELETE` | `/api/v1/community/me/posts/{postId}/favorite` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `POST` | `/api/v1/community/me/polls/{pollId}/votes` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `pollId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/me/posts/{postId}/reports` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/me/comments/{commentId}/reports` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/community/me/reports` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/community/me/tickets` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/community/me/tickets` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/community/me/tickets/{ticketId}` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `ticketId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/community/me/tickets/{ticketId}` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `ticketId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/me/tickets/{ticketId}/close` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `ticketId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/community/admin/boards` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/community/admin/boards` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/boards/{boardId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `boardId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/boards/{boardId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `boardId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/community/admin/posts` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/community/admin/posts/{postId}` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `postId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/community/admin/posts/{postId}/approve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/community/admin/posts/{postId}/reject` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/posts/{postId}/request-changes` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/posts/{postId}/offline` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/posts/{postId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/posts/{postId}/delete` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `GET` | `/api/v1/community/admin/comments` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `PATCH` | `/api/v1/community/admin/comments/{commentId}/approve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/community/admin/comments/{commentId}/reject` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/comments/{commentId}/offline` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/community/admin/reports` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/community/admin/reports/{reportId}` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `reportId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/community/admin/reports/{reportId}/assign` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `reportId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/reports/{reportId}/resolve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `reportId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/reports/{reportId}/dismiss` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `reportId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/community/admin/tickets` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/community/admin/tickets/{ticketId}` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `ticketId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/community/admin/tickets/{ticketId}/assign` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `ticketId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/admin/tickets/{ticketId}/messages` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `ticketId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/tickets/{ticketId}/status` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `ticketId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/admin/penalties` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/penalties/{penaltyId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `penaltyId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/penalties/{penaltyId}/revoke` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `penaltyId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `POST` | `/api/v1/community/admin/polls` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/polls/{pollId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `pollId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/polls/{pollId}/open` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `pollId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/polls/{pollId}/close` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `pollId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/community/admin/audit-logs` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/community/admin/ops/summary` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`community` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-activity.md`

# 北冥官网 activity API 契约

版本：1.0

## 文档定位

本文档是 `activity` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/activity/ActivityModule.java`。`activity` 在运行上由 `engagement-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`activity` 负责活动发布、报名、签到、结果、奖励和贡献候选。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 41 个 `activity` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/activity/events` | 公开或按接口内部规则；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/activity/events/{activityIdOrSlug}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `activityIdOrSlug`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/activity/events/{activityId}/result` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `activityId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/activity/calendar-summary` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/activity/me/registrations` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ACTIVITY_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/activity/me/registrations/{registrationId}` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ACTIVITY_SELF | 路径参数 `registrationId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/activity/me/events/{activityId}/registrations` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ACTIVITY_SELF | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/activity/me/registrations/{registrationId}/cancel` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ACTIVITY_SELF | 路径参数 `registrationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/activity/me/events/{activityId}/check-in` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ACTIVITY_SELF | 路径参数 `activityId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/activity/me/rewards` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；ACTIVITY_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/activity/admin/events` | HELPER、ADMIN 或 OWNER；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/activity/admin/events/{activityId}` | HELPER、ADMIN 或 OWNER；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/activity/admin/events` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/events/{activityId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/activity/admin/events/{activityId}/submit` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/events/{activityId}/approve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/activity/admin/events/{activityId}/reject` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/events/{activityId}/request-changes` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/events/{activityId}/publish` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/events/{activityId}/open-registration` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/events/{activityId}/close-registration` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/events/{activityId}/start` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/events/{activityId}/complete` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/events/{activityId}/offline` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/events/{activityId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/events/{activityId}/delete` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `GET` | `/api/v1/activity/admin/events/{activityId}/registrations` | HELPER、ADMIN 或 OWNER；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/activity/admin/registrations/{registrationId}/confirm` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `registrationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/registrations/{registrationId}/reject` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `registrationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/registrations/{registrationId}/promote` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `registrationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/registrations/{registrationId}/cancel` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `registrationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/registrations/{registrationId}/check-in` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `registrationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/registrations/{registrationId}/no-show` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `registrationId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PUT` | `/api/v1/activity/admin/events/{activityId}/result` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/events/{activityId}/result/publish` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/activity/admin/events/{activityId}/rewards` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/rewards/{rewardId}/issue` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `rewardId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/activity/admin/rewards/{rewardId}/revoke` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `rewardId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `POST` | `/api/v1/activity/admin/events/{activityId}/contribution-candidates` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 路径参数 `activityId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/activity/admin/audit-logs` | HELPER、ADMIN 或 OWNER；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/activity/admin/ops/summary` | HELPER、ADMIN 或 OWNER；ACTIVITY_READ for GET；ACTIVITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`activity` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-calendar.md`

# 北冥官网 calendar API 契约

版本：1.0

## 文档定位

本文档是 `calendar` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/calendar/CalendarModule.java`。`calendar` 在运行上由 `engagement-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`calendar` 负责日程事件、维护窗口、工程节点、活动同步和关注列表。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 21 个 `calendar` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/calendar/events` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/calendar/events/{eventId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `eventId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/calendar/month` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/calendar/upcoming` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/calendar/me/watchlist` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；CALENDAR_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/calendar/me/events/{eventId}/watch` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；CALENDAR_SELF | 路径参数 `eventId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/calendar/me/events/{eventId}/unwatch` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；CALENDAR_SELF | 路径参数 `eventId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/calendar/admin/events` | HELPER、ADMIN 或 OWNER；CALENDAR_READ for GET；CALENDAR_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/calendar/admin/events/{eventId}` | HELPER、ADMIN 或 OWNER；CALENDAR_READ for GET；CALENDAR_WRITE for mutation | 路径参数 `eventId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/calendar/admin/events` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CALENDAR_READ for GET；CALENDAR_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/calendar/admin/events/{eventId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CALENDAR_READ for GET；CALENDAR_WRITE for mutation | 路径参数 `eventId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/calendar/admin/events/{eventId}/submit` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CALENDAR_READ for GET；CALENDAR_WRITE for mutation | 路径参数 `eventId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/calendar/admin/events/{eventId}/approve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CALENDAR_READ for GET；CALENDAR_WRITE for mutation | 路径参数 `eventId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/calendar/admin/events/{eventId}/reject` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CALENDAR_READ for GET；CALENDAR_WRITE for mutation | 路径参数 `eventId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/calendar/admin/events/{eventId}/publish` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CALENDAR_READ for GET；CALENDAR_WRITE for mutation | 路径参数 `eventId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/calendar/admin/events/{eventId}/offline` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CALENDAR_READ for GET；CALENDAR_WRITE for mutation | 路径参数 `eventId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/calendar/admin/events/{eventId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CALENDAR_READ for GET；CALENDAR_WRITE for mutation | 路径参数 `eventId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/calendar/admin/events/{eventId}/delete` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CALENDAR_READ for GET；CALENDAR_WRITE for mutation | 路径参数 `eventId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `POST` | `/api/v1/calendar/admin/sync/activity` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CALENDAR_READ for GET；CALENDAR_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/calendar/admin/audit-logs` | HELPER、ADMIN 或 OWNER；CALENDAR_READ for GET；CALENDAR_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/calendar/admin/ops/summary` | HELPER、ADMIN 或 OWNER；CALENDAR_READ for GET；CALENDAR_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`calendar` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-changelog.md`

# 北冥官网 changelog API 契约

版本：1.0

## 文档定位

本文档是 `changelog` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/changelog/ChangelogModule.java`。`changelog` 在运行上由 `engagement-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`changelog` 负责版本更新、插件变更、规则调整、维护记录和日历同步。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 23 个 `changelog` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/changelog/releases` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/changelog/releases/{releaseIdOrSlug}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `releaseIdOrSlug`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/changelog/versions/latest` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/changelog/tags` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/changelog/changes` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/changelog/me/bookmarks` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；CHANGELOG_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/changelog/me/releases/{releaseId}/bookmark` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；CHANGELOG_SELF | 路径参数 `releaseId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/changelog/me/releases/{releaseId}/unbookmark` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；CHANGELOG_SELF | 路径参数 `releaseId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/changelog/admin/releases` | HELPER、ADMIN 或 OWNER；CHANGELOG_READ for GET；CHANGELOG_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/changelog/admin/releases/{releaseId}` | HELPER、ADMIN 或 OWNER；CHANGELOG_READ for GET；CHANGELOG_WRITE for mutation | 路径参数 `releaseId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/changelog/admin/releases` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CHANGELOG_READ for GET；CHANGELOG_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/changelog/admin/releases/{releaseId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CHANGELOG_READ for GET；CHANGELOG_WRITE for mutation | 路径参数 `releaseId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/changelog/admin/releases/{releaseId}/submit` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CHANGELOG_READ for GET；CHANGELOG_WRITE for mutation | 路径参数 `releaseId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/changelog/admin/releases/{releaseId}/approve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CHANGELOG_READ for GET；CHANGELOG_WRITE for mutation | 路径参数 `releaseId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/changelog/admin/releases/{releaseId}/reject` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CHANGELOG_READ for GET；CHANGELOG_WRITE for mutation | 路径参数 `releaseId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/changelog/admin/releases/{releaseId}/request-changes` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CHANGELOG_READ for GET；CHANGELOG_WRITE for mutation | 路径参数 `releaseId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/changelog/admin/releases/{releaseId}/publish` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CHANGELOG_READ for GET；CHANGELOG_WRITE for mutation | 路径参数 `releaseId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/changelog/admin/releases/{releaseId}/offline` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CHANGELOG_READ for GET；CHANGELOG_WRITE for mutation | 路径参数 `releaseId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/changelog/admin/releases/{releaseId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CHANGELOG_READ for GET；CHANGELOG_WRITE for mutation | 路径参数 `releaseId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/changelog/admin/releases/{releaseId}/delete` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CHANGELOG_READ for GET；CHANGELOG_WRITE for mutation | 路径参数 `releaseId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `POST` | `/api/v1/changelog/admin/releases/{releaseId}/calendar-sync` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CHANGELOG_READ for GET；CHANGELOG_WRITE for mutation | 路径参数 `releaseId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/changelog/admin/audit-logs` | HELPER、ADMIN 或 OWNER；CHANGELOG_READ for GET；CHANGELOG_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/changelog/admin/ops/summary` | HELPER、ADMIN 或 OWNER；CHANGELOG_READ for GET；CHANGELOG_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`changelog` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-ops-control.md`

# 北冥官网 ops-control API 契约

版本：1.0

## 文档定位

本文档是 `ops-control` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/opscontrol/OpsControlModule.java`。`ops-control` 在运行上由 `ops-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`ops-control` 负责运维控制面，负责资产、节点、容器、虚拟机、实例、文件、日志、任务、审批和审计，不执行真实节点操作。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 31 个 `ops-control` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/ops-control/overview` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/ops-control/assets` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/ops-control/assets/{assetId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `assetId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/ops-control/nodes` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/ops-control/nodes/{nodeId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `nodeId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/ops-control/nodes` | 已登录用户；后台路径另按角色校验；OPS_CONTROL_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-control/nodes/{nodeId}/disable` | 已登录用户；后台路径另按角色校验；OPS_CONTROL_SELF 或模块写权限 | 路径参数 `nodeId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-control/nodes/{nodeId}/enable` | 已登录用户；后台路径另按角色校验；OPS_CONTROL_SELF 或模块写权限 | 路径参数 `nodeId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/ops-control/nodes/{nodeId}/heartbeat` | 已登录用户；后台路径另按角色校验；OPS_CONTROL_SELF 或模块写权限 | 路径参数 `nodeId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/ops-control/nodes/{nodeId}/capabilities` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `nodeId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/ops-control/nodes/{nodeId}/metrics/latest` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `nodeId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/ops-control/nodes/{nodeId}/containers` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `nodeId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/ops-control/nodes/{nodeId}/containers/{containerId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `nodeId, containerId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/ops-control/nodes/{nodeId}/vms` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `nodeId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/ops-control/nodes/{nodeId}/vms/{vmId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `nodeId, vmId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/ops-control/nodes/{nodeId}/minecraft-instances` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `nodeId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/ops-control/nodes/{nodeId}/minecraft-instances/{instanceId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `nodeId, instanceId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/ops-control/nodes/{nodeId}/files` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `nodeId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/ops-control/nodes/{nodeId}/files/read` | 已登录用户；后台路径另按角色校验；OPS_CONTROL_SELF 或模块写权限 | 路径参数 `nodeId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/ops-control/nodes/{nodeId}/logs/query` | 已登录用户；后台路径另按角色校验；OPS_CONTROL_SELF 或模块写权限 | 路径参数 `nodeId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | MEDIUM |
| `POST` | `/api/v1/ops-control/tasks` | 已登录用户；后台路径另按角色校验；OPS_CONTROL_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `GET` | `/api/v1/ops-control/tasks` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/ops-control/tasks/{taskId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `taskId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/ops-control/tasks/{taskId}/cancel` | 已登录用户；后台路径另按角色校验；OPS_CONTROL_SELF 或模块写权限 | 路径参数 `taskId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `POST` | `/api/v1/ops-control/tasks/{taskId}/node-result` | 已登录用户；后台路径另按角色校验；OPS_CONTROL_SELF 或模块写权限 | 路径参数 `taskId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `GET` | `/api/v1/ops-control/approvals` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/ops-control/approvals/{approvalId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `approvalId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/ops-control/approvals/{approvalId}/approve` | 已登录用户；后台路径另按角色校验；OPS_CONTROL_SELF 或模块写权限 | 路径参数 `approvalId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/ops-control/approvals/{approvalId}/reject` | 已登录用户；后台路径另按角色校验；OPS_CONTROL_SELF 或模块写权限 | 路径参数 `approvalId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/ops-control/audit-logs` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/ops-control/ops/summary` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`ops-control` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-cloudreve-sync.md`

# 北冥官网 cloudreve-sync API 契约

版本：1.0

## 文档定位

本文档是 `cloudreve-sync` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/cloudrevesync/CloudreveSyncServiceApplication.java`。`cloudreve-sync` 在运行上由 `ops-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`cloudreve-sync` 负责Cloudreve provider、目录文件快照、分享解析、同步任务和同步审计。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 16 个 `cloudreve-sync` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/cloudreve-sync/health` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | `service`、`status`、`port`、`checkedAt` 等健康摘要字段 | LOW |
| `GET` | `/api/v1/cloudreve-sync/ops/summary` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/cloudreve-sync/providers` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/cloudreve-sync/providers/{providerId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `providerId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/cloudreve-sync/providers` | 已登录用户；后台路径另按角色校验；CLOUDREVE_SYNC_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cloudreve-sync/providers/{providerId}` | 已登录用户；后台路径另按角色校验；CLOUDREVE_SYNC_SELF 或模块写权限 | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cloudreve-sync/providers/{providerId}/disable` | 已登录用户；后台路径另按角色校验；CLOUDREVE_SYNC_SELF 或模块写权限 | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cloudreve-sync/providers/{providerId}/enable` | 已登录用户；后台路径另按角色校验；CLOUDREVE_SYNC_SELF 或模块写权限 | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/cloudreve-sync/files` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/cloudreve-sync/shares` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/cloudreve-sync/shares/resolve` | 已登录用户；后台路径另按角色校验；CLOUDREVE_SYNC_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/cloudreve-sync/sync-jobs` | 已登录用户；后台路径另按角色校验；CLOUDREVE_SYNC_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/cloudreve-sync/sync-jobs` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/cloudreve-sync/sync-jobs/{jobId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `jobId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/cloudreve-sync/sync-jobs/{jobId}/cancel` | 已登录用户；后台路径另按角色校验；CLOUDREVE_SYNC_SELF 或模块写权限 | 路径参数 `jobId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/cloudreve-sync/audit-logs` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`cloudreve-sync` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-backup-recovery.md`

# 北冥官网 backup-recovery API 契约

版本：1.0

## 文档定位

本文档是 `backup-recovery` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/backuprecovery/BackupRecoveryServiceApplication.java`。`backup-recovery` 在运行上由 `ops-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`backup-recovery` 负责备份域、策略、任务、备份点、校验、演练、恢复申请和恢复审批。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 25 个 `backup-recovery` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/backup-recovery/health` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | `service`、`status`、`port`、`checkedAt` 等健康摘要字段 | LOW |
| `GET` | `/api/v1/backup-recovery/ops/summary` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/backup-recovery/domains` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/backup-recovery/policies` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/backup-recovery/policies/{policyId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `policyId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/backup-recovery/policies` | 已登录用户；后台路径另按角色校验；BACKUP_RECOVERY_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/backup-recovery/policies/{policyId}` | 已登录用户；后台路径另按角色校验；BACKUP_RECOVERY_SELF 或模块写权限 | 路径参数 `policyId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/backup-recovery/policies/{policyId}/enable` | 已登录用户；后台路径另按角色校验；BACKUP_RECOVERY_SELF 或模块写权限 | 路径参数 `policyId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/backup-recovery/policies/{policyId}/disable` | 已登录用户；后台路径另按角色校验；BACKUP_RECOVERY_SELF 或模块写权限 | 路径参数 `policyId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/backup-recovery/jobs` | 已登录用户；后台路径另按角色校验；BACKUP_RECOVERY_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/backup-recovery/jobs` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/backup-recovery/jobs/{jobId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `jobId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/backup-recovery/jobs/{jobId}/cancel` | 已登录用户；后台路径另按角色校验；BACKUP_RECOVERY_SELF 或模块写权限 | 路径参数 `jobId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/backup-recovery/backup-points` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/backup-recovery/backup-points/{backupPointId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `backupPointId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/backup-recovery/backup-points/{backupPointId}/verify` | 已登录用户；后台路径另按角色校验；BACKUP_RECOVERY_SELF 或模块写权限 | 路径参数 `backupPointId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/backup-recovery/restore-drills` | 已登录用户；后台路径另按角色校验；BACKUP_RECOVERY_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/backup-recovery/restore-drills` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/backup-recovery/restore-drills/{drillId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `drillId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/backup-recovery/restore-requests` | 已登录用户；后台路径另按角色校验；BACKUP_RECOVERY_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/backup-recovery/restore-requests` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/backup-recovery/restore-requests/{restoreRequestId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `restoreRequestId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/backup-recovery/restore-requests/{restoreRequestId}/approve` | 已登录用户；后台路径另按角色校验；BACKUP_RECOVERY_SELF 或模块写权限 | 路径参数 `restoreRequestId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/backup-recovery/restore-requests/{restoreRequestId}/reject` | 已登录用户；后台路径另按角色校验；BACKUP_RECOVERY_SELF 或模块写权限 | 路径参数 `restoreRequestId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/backup-recovery/audit-logs` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`backup-recovery` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-alerting.md`

# 北冥官网 alerting API 契约

版本：1.0

## 文档定位

本文档是 `alerting` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/alerting/AlertingModule.java`。`alerting` 在运行上由 `ops-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`alerting` 负责告警源、规则、告警、静默、路由、测试投递、投递记录和审计。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 24 个 `alerting` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/alerting/health` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | `service`、`status`、`port`、`checkedAt` 等健康摘要字段 | LOW |
| `GET` | `/api/v1/alerting/ops/summary` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/alerting/sources` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/alerting/sources/{sourceId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `sourceId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/alerting/rules` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/alerting/rules/{ruleId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `ruleId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/alerting/rules` | 已登录用户；后台路径另按角色校验；ALERTING_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/alerting/rules/{ruleId}` | 已登录用户；后台路径另按角色校验；ALERTING_SELF 或模块写权限 | 路径参数 `ruleId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/alerting/rules/{ruleId}/enable` | 已登录用户；后台路径另按角色校验；ALERTING_SELF 或模块写权限 | 路径参数 `ruleId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/alerting/rules/{ruleId}/disable` | 已登录用户；后台路径另按角色校验；ALERTING_SELF 或模块写权限 | 路径参数 `ruleId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/alerting/rules/{ruleId}/evaluate` | 已登录用户；后台路径另按角色校验；ALERTING_SELF 或模块写权限 | 路径参数 `ruleId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/alerting/alerts` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/alerting/alerts/{alertId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `alertId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/alerting/alerts/{alertId}/acknowledge` | 已登录用户；后台路径另按角色校验；ALERTING_SELF 或模块写权限 | 路径参数 `alertId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/alerting/alerts/{alertId}/close` | 已登录用户；后台路径另按角色校验；ALERTING_SELF 或模块写权限 | 路径参数 `alertId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/alerting/silences` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/alerting/silences` | 已登录用户；后台路径另按角色校验；ALERTING_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/alerting/silences/{silenceId}/cancel` | 已登录用户；后台路径另按角色校验；ALERTING_SELF 或模块写权限 | 路径参数 `silenceId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/alerting/routes` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/alerting/routes` | 已登录用户；后台路径另按角色校验；ALERTING_SELF 或模块写权限 | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/alerting/routes/{routeId}` | 已登录用户；后台路径另按角色校验；ALERTING_SELF 或模块写权限 | 路径参数 `routeId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/alerting/routes/{routeId}/test` | 已登录用户；后台路径另按角色校验；ALERTING_SELF 或模块写权限 | 路径参数 `routeId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/alerting/deliveries` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/alerting/audit-logs` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`alerting` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-plugin-integration.md`

# 北冥官网 plugin-integration API 契约

版本：1.0

## 文档定位

本文档是 `plugin-integration` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/pluginintegration/PluginIntegrationModule.java`。`plugin-integration` 在运行上由 `ops-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`plugin-integration` 负责插件 provider、实例、能力、事件 schema、事件接收、路由规则、同步任务和对象映射。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 38 个 `plugin-integration` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/plugin-integration/health` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | `service`、`status`、`port`、`checkedAt` 等健康摘要字段 | LOW |
| `GET` | `/api/v1/plugin-integration/admin/ops/summary` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/plugin-integration/admin/providers` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/plugin-integration/admin/providers/{providerId}` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/plugin-integration/admin/providers` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/plugin-integration/admin/providers/{providerId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/plugin-integration/admin/providers/{providerId}/enable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/plugin-integration/admin/providers/{providerId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/plugin-integration/admin/providers/{providerId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/plugin-integration/admin/instances` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/plugin-integration/admin/instances/{instanceId}` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `instanceId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/plugin-integration/admin/capabilities` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/plugin-integration/admin/event-schemas` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/plugin-integration/admin/event-schemas/{schemaId}` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `schemaId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/plugin-integration/admin/event-schemas` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/plugin-integration/admin/event-schemas/{schemaId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `schemaId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/plugin-integration/admin/event-schemas/{schemaId}/enable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `schemaId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/plugin-integration/admin/event-schemas/{schemaId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `schemaId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/plugin-integration/admin/events/ingest` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/plugin-integration/admin/events` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/plugin-integration/admin/events/{eventId}` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `eventId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/plugin-integration/admin/events/{eventId}/replay` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `eventId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/plugin-integration/admin/route-rules` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/plugin-integration/admin/route-rules/{ruleId}` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `ruleId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/plugin-integration/admin/route-rules` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/plugin-integration/admin/route-rules/{ruleId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `ruleId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/plugin-integration/admin/route-rules/{ruleId}/enable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `ruleId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/plugin-integration/admin/route-rules/{ruleId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `ruleId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/plugin-integration/admin/sync-tasks` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/plugin-integration/admin/sync-tasks` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/plugin-integration/admin/sync-tasks/{taskId}` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `taskId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/plugin-integration/admin/sync-tasks/{taskId}/cancel` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `taskId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/plugin-integration/admin/providers/{providerId}/health-snapshots` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/plugin-integration/admin/object-mappings` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/plugin-integration/admin/object-mappings/{mappingId}` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `mappingId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PUT` | `/api/v1/plugin-integration/admin/object-mappings/{mappingId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `mappingId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/plugin-integration/admin/object-mappings/{mappingId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `mappingId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/plugin-integration/admin/audit-logs` | HELPER、ADMIN 或 OWNER；PLUGIN_INTEGRATION_READ for GET；PLUGIN_INTEGRATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`plugin-integration` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-cross-platform-notification.md`

# 北冥官网 cross-platform-notification API 契约

版本：1.0

## 文档定位

本文档是 `cross-platform-notification` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/crossplatformnotification/CrossPlatformNotificationModule.java`。`cross-platform-notification` 在运行上由 `ops-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`cross-platform-notification` 负责外部通知 provider、能力、模板映射、路由、模拟投递、重试、接收人和审计。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 36 个 `cross-platform-notification` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/cross-platform-notification/health` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | `service`、`status`、`port`、`checkedAt` 等健康摘要字段 | LOW |
| `GET` | `/api/v1/cross-platform-notification/admin/ops/summary` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/cross-platform-notification/admin/providers` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/cross-platform-notification/admin/providers/{providerId}` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/cross-platform-notification/admin/providers` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cross-platform-notification/admin/providers/{providerId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cross-platform-notification/admin/providers/{providerId}/enable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cross-platform-notification/admin/providers/{providerId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cross-platform-notification/admin/providers/{providerId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/cross-platform-notification/admin/capabilities` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/cross-platform-notification/admin/capabilities/{capabilityId}` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `capabilityId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/cross-platform-notification/admin/template-mappings` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `mappingId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/cross-platform-notification/admin/template-mappings` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `mappingId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/enable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `mappingId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `mappingId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `mappingId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/cross-platform-notification/admin/routes` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/cross-platform-notification/admin/routes/{routeId}` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `routeId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/cross-platform-notification/admin/routes` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cross-platform-notification/admin/routes/{routeId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `routeId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cross-platform-notification/admin/routes/{routeId}/enable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `routeId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cross-platform-notification/admin/routes/{routeId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `routeId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cross-platform-notification/admin/routes/{routeId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `routeId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/cross-platform-notification/admin/routes/{routeId}/test` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `routeId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/cross-platform-notification/admin/deliveries` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/cross-platform-notification/admin/deliveries` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/cross-platform-notification/admin/deliveries/{deliveryId}` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `deliveryId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/cross-platform-notification/admin/deliveries/{deliveryId}/retry` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `deliveryId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/cross-platform-notification/admin/deliveries/{deliveryId}/cancel` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `deliveryId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/cross-platform-notification/admin/attempts` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/cross-platform-notification/admin/attempts/{attemptId}` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `attemptId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/cross-platform-notification/admin/receivers` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/cross-platform-notification/admin/receivers/{receiverId}` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `receiverId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/cross-platform-notification/admin/audit-logs` | HELPER、ADMIN 或 OWNER；CROSS_PLATFORM_NOTIFICATION_READ for GET；CROSS_PLATFORM_NOTIFICATION_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`cross-platform-notification` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-ops-image-market.md`

# 北冥官网 ops-image-market API 契约

版本：1.0

## 文档定位

本文档是 `ops-image-market` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/opsimagemarket/OpsImageMarketModule.java`。`ops-image-market` 在运行上由 `ops-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`ops-image-market` 负责运维镜像 provider、镜像、版本、兼容配置、模板、安全扫描、拉取计划和缓存快照。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 49 个 `ops-image-market` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/ops-image-market/health` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | `service`、`status`、`port`、`checkedAt` 等健康摘要字段 | LOW |
| `GET` | `/api/v1/ops-image-market/admin/ops/summary` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/ops-image-market/admin/providers` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/ops-image-market/admin/providers/{providerId}` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/ops-image-market/admin/providers` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/providers/{providerId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/providers/{providerId}/enable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/providers/{providerId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/providers/{providerId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/ops-image-market/admin/providers/{providerId}/health-refresh` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/ops-image-market/admin/images` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/ops-image-market/admin/images/{imageId}` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `imageId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/ops-image-market/admin/images` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/images/{imageId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `imageId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/images/{imageId}/publish` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `imageId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/images/{imageId}/block` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `imageId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/images/{imageId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `imageId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/ops-image-market/admin/images/{imageId}/versions` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `imageId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/ops-image-market/admin/versions/{imageVersionId}` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `imageVersionId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/ops-image-market/admin/images/{imageId}/versions` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `imageId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/versions/{imageVersionId}/approve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `imageVersionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/ops-image-market/admin/versions/{imageVersionId}/deprecate` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `imageVersionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/versions/{imageVersionId}/block` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `imageVersionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/versions/{imageVersionId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `imageVersionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/ops-image-market/admin/compatibility-profiles` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `profileId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/ops-image-market/admin/compatibility-profiles` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `profileId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/enable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `profileId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `profileId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `profileId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/ops-image-market/admin/templates` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/ops-image-market/admin/templates/{templateId}` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `templateId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/ops-image-market/admin/templates` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/templates/{templateId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `templateId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/templates/{templateId}/enable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `templateId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/templates/{templateId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `templateId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/templates/{templateId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `templateId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/ops-image-market/admin/scans` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/ops-image-market/admin/scans/{scanId}` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `scanId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/ops-image-market/admin/versions/{imageVersionId}/scans` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `imageVersionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/ops-image-market/admin/pull-plans` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/ops-image-market/admin/pull-plans/{planId}` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `planId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/ops-image-market/admin/pull-plans` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/ops-image-market/admin/pull-plans/{planId}/approve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `planId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/ops-image-market/admin/pull-plans/{planId}/cancel` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `planId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/ops-image-market/admin/cache-snapshots` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/ops-image-market/admin/cache-snapshots/{snapshotId}` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 路径参数 `snapshotId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/ops-image-market/admin/audit-logs` | HELPER、ADMIN 或 OWNER；OPS_IMAGE_MARKET_READ for GET；OPS_IMAGE_MARKET_WRITE 或 HIGH_RISK_APPROVE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`ops-image-market` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-guide.md`

# 北冥官网 guide API 契约

版本：1.0

## 文档定位

本文档是 `guide` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/guide/GuideModule.java`。`guide` 在运行上由 `portal-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`guide` 负责指南首页、分类、文章、搜索、指令、外部交流入口、规则版本和反馈。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 41 个 `guide` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/guides/home` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/guides/categories` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/guides/articles` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/guides/articles/{guideId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `guideId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/guides/articles/by-slug/{slug}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `slug`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/guides/search` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/guides/commands` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/guides/external-channels` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/guides/rules/current` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/guides/rules/versions/{ruleVersion}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `ruleVersion`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/guides/articles/{guideId}/feedback` | 已登录用户；后台路径另按角色校验；GUIDE_SELF 或模块写权限 | 路径参数 `guideId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/guides/admin/articles` | HELPER、ADMIN 或 OWNER；GUIDE_READ for GET；GUIDE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/guides/admin/articles/{guideId}` | HELPER、ADMIN 或 OWNER；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `guideId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/guides/admin/articles` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/guides/admin/articles/{guideId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `guideId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/guides/admin/articles/{guideId}/submit-review` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `guideId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/guides/admin/articles/{guideId}/approve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `guideId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/guides/admin/articles/{guideId}/reject` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `guideId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/guides/admin/articles/{guideId}/request-changes` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `guideId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/guides/admin/articles/{guideId}/publish` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `guideId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/guides/admin/articles/{guideId}/offline` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `guideId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/guides/admin/articles/{guideId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `guideId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/guides/admin/articles/{guideId}/delete` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `guideId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `GET` | `/api/v1/guides/admin/articles/{guideId}/versions` | HELPER、ADMIN 或 OWNER；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `guideId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/guides/admin/articles/{guideId}/versions/{version}` | HELPER、ADMIN 或 OWNER；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `guideId, version`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/guides/admin/articles/{guideId}/versions/{version}/restore` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `guideId, version`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/guides/admin/articles/{guideId}/audit-logs` | HELPER、ADMIN 或 OWNER；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `guideId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/guides/admin/categories` | HELPER、ADMIN 或 OWNER；GUIDE_READ for GET；GUIDE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/guides/admin/categories` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/guides/admin/categories/{categoryId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `categoryId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/guides/admin/categories/{categoryId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `categoryId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/guides/admin/external-channels` | HELPER、ADMIN 或 OWNER；GUIDE_READ for GET；GUIDE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/guides/admin/external-channels` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/guides/admin/external-channels/{channelId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `channelId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/guides/admin/external-channels/{channelId}/enable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `channelId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/guides/admin/external-channels/{channelId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `channelId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/guides/admin/external-channels/{channelId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `channelId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/guides/admin/feedback` | HELPER、ADMIN 或 OWNER；GUIDE_READ for GET；GUIDE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `PATCH` | `/api/v1/guides/admin/feedback/{feedbackId}/resolve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `feedbackId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/guides/admin/feedback/{feedbackId}/ignore` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；GUIDE_READ for GET；GUIDE_WRITE for mutation | 路径参数 `feedbackId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/guides/admin/ops/summary` | HELPER、ADMIN 或 OWNER；GUIDE_READ for GET；GUIDE_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`guide` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-material.md`

# 北冥官网 material API 契约

版本：1.0

## 文档定位

本文档是 `material` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/material/MaterialModule.java`。`material` 在运行上由 `portal-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`material` 负责素材精选、素材列表、投稿、上传会话、审核、分类、资产安全状态和审计。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 33 个 `material` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/materials/featured` | 公开或按接口内部规则；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/materials` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/materials/{materialId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `materialId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/materials/by-slug/{slug}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `slug`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/materials/categories` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/materials/{materialId}/assets` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `materialId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/materials/me/upload-sessions` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；MATERIAL_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/materials/me/upload-sessions/{uploadSessionId}/complete` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；MATERIAL_SELF | 路径参数 `uploadSessionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/materials/me/submissions` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；MATERIAL_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/materials/me/submissions` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；MATERIAL_SELF | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/materials/me/submissions/{materialId}` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；MATERIAL_SELF | 路径参数 `materialId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/materials/me/submissions/{materialId}` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；MATERIAL_SELF | 路径参数 `materialId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/materials/me/submissions/{materialId}/submit-review` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；MATERIAL_SELF | 路径参数 `materialId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/materials/me/submissions/{materialId}/withdraw` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；MATERIAL_SELF | 路径参数 `materialId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/materials/me/submissions/{materialId}/resubmit` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；MATERIAL_SELF | 路径参数 `materialId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/materials/admin/items` | HELPER、ADMIN 或 OWNER；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/materials/admin/items/{materialId}` | HELPER、ADMIN 或 OWNER；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 路径参数 `materialId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/materials/admin/items/{materialId}/approve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 路径参数 `materialId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/materials/admin/items/{materialId}/reject` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 路径参数 `materialId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/materials/admin/items/{materialId}/request-changes` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 路径参数 `materialId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/materials/admin/items/{materialId}/feature` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 路径参数 `materialId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/materials/admin/items/{materialId}/unfeature` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 路径参数 `materialId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/materials/admin/items/{materialId}/offline` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 路径参数 `materialId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/materials/admin/items/{materialId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 路径参数 `materialId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/materials/admin/items/{materialId}/delete` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 路径参数 `materialId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `GET` | `/api/v1/materials/admin/categories` | HELPER、ADMIN 或 OWNER；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/materials/admin/categories` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/materials/admin/categories/{categoryId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 路径参数 `categoryId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/materials/admin/categories/{categoryId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 路径参数 `categoryId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/materials/admin/assets` | HELPER、ADMIN 或 OWNER；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `PATCH` | `/api/v1/materials/admin/assets/{assetId}/security-status` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 路径参数 `assetId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/materials/admin/items/{materialId}/audit-logs` | HELPER、ADMIN 或 OWNER；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 路径参数 `materialId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/materials/admin/ops/summary` | HELPER、ADMIN 或 OWNER；MATERIAL_READ for GET；MATERIAL_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`material` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。


---

来源：`docs/contracts-online-map.md`

# 北冥官网 online-map API 契约

版本：1.0

## 文档定位

本文档是 `online-map` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/onlinemap/OnlineMapModule.java`。`online-map` 在运行上由 `portal-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`online-map` 负责在线地图 provider、世界、图层、marker、区域、嵌入配置和健康快照。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 34 个 `online-map` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/online-map/health` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | `service`、`status`、`port`、`checkedAt` 等健康摘要字段 | LOW |
| `GET` | `/api/v1/online-map/overview` | 公开或按接口内部规则；无；按公开可见范围过滤 | 无请求体 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/online-map/providers` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/online-map/providers/{providerId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `providerId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/online-map/worlds` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/online-map/layers` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/online-map/markers` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/online-map/regions` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/online-map/embed` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/online-map/admin/ops/summary` | HELPER、ADMIN 或 OWNER；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |
| `GET` | `/api/v1/online-map/admin/providers` | HELPER、ADMIN 或 OWNER；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/online-map/admin/providers/{providerId}` | HELPER、ADMIN 或 OWNER；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 路径参数 `providerId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `POST` | `/api/v1/online-map/admin/providers` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/online-map/admin/providers/{providerId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/online-map/admin/providers/{providerId}/enable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/online-map/admin/providers/{providerId}/disable` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/online-map/admin/providers/{providerId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/online-map/admin/providers/{providerId}/health/refresh` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 路径参数 `providerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/online-map/admin/providers/{providerId}/health/snapshots` | HELPER、ADMIN 或 OWNER；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 路径参数 `providerId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/online-map/admin/worlds` | HELPER、ADMIN 或 OWNER；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `PUT` | `/api/v1/online-map/admin/worlds/{worldId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 路径参数 `worldId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/online-map/admin/layers` | HELPER、ADMIN 或 OWNER；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/online-map/admin/layers` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/online-map/admin/layers/{layerId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 路径参数 `layerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/online-map/admin/layers/{layerId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 路径参数 `layerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/online-map/admin/markers` | HELPER、ADMIN 或 OWNER；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/online-map/admin/markers` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/online-map/admin/markers/{markerId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 路径参数 `markerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/online-map/admin/markers/{markerId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 路径参数 `markerId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/online-map/admin/regions` | HELPER、ADMIN 或 OWNER；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/online-map/admin/regions` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/online-map/admin/regions/{regionId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 路径参数 `regionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/online-map/admin/regions/{regionId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 路径参数 `regionId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/online-map/admin/audit-logs` | HELPER、ADMIN 或 OWNER；ONLINE_MAP_READ for GET；ONLINE_MAP_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`online-map` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。
