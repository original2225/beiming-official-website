# 北冥官网公共 API 契约

版本：1.0

## 文档定位

本文档定义当前模块化单体后端所有接口共享的基础契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，本地联调默认入口统一为 `http://127.0.0.1:8135`，业务路径保持 `/api/v1/**` 原样。历史独立服务入口和历史端口只作为追溯字段、回滚引用或仓库外证据引用，不作为当前开发、联调、测试或前端调用入口。

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

## 受控生产入口证据

统一后端 readiness 可以暴露受控生产入口和旧入口退役字段。`apiGatewayControlledRetirementStatus=BLOCKED_BY_API_GATEWAY_RETIREMENT_RECEIPT_NOT_PROVIDED`、`apiGatewayExternalRetirementEvidenceStatus=BLOCKED_BY_EXTERNAL_API_GATEWAY_RETIREMENT_EVIDENCE_NOT_PROVIDED`、`realProductionEntrypointCutoverStatus=BLOCKED_BY_REAL_PRODUCTION_ENTRYPOINT_CUTOVER_EVIDENCE_NOT_PROVIDED` 和 `externalEntrypointCutoverEvidenceIntakeStatus=BLOCKED_BY_EXTERNAL_ENTRYPOINT_CUTOVER_EVIDENCE_NOT_PROVIDED` 只能说明仓库外证据未提供，不能说明真实生产切流完成、旧网关新流量归零或删除审批完成。运行态只返回 `EXTERNAL_EVIDENCE_REF:API_GATEWAY_RETIREMENT_RECEIPT`、`EXTERNAL_EVIDENCE_REF:API_GATEWAY_EXTERNAL_RETIREMENT`、`EXTERNAL_EVIDENCE_REF:REAL_PRODUCTION_ENTRYPOINT_CUTOVER` 和 `EXTERNAL_EVIDENCE_REF:EXTERNAL_ENTRYPOINT_CUTOVER_INTAKE` 这类脱敏引用，`api-gateway-service` 只作为历史回滚引用，当前入口保持 `backend:8135`，`docs/` 不再保留 JSON 或 JSONL 样例文件。没有仓库外证据时，`readyForProduction=false`、`readyToReplaceGateway=false`、`oldApiGatewayRetirementAllowed=false`、`readyToRetireBusinessCore=false`、`readyToRetireAdmissionCore=false`、`readyToRetireEngagementCore=false`、`readyToRetireOpsCore=false` 和 `readyToRetirePortalCore=false` 必须保持。

## 验收口径

任意模块进入实现或变更前，必须先更新对应 `docs/contracts-<module>.md`。当前后端统一验证命令是 `mvn -q -f backend/pom.xml test`。正式接口不得恢复旧独立 Maven 入口，不得把前端调用地址写回历史端口，不得绕过统一响应、统一错误、分页、认证、权限和审计要求。
