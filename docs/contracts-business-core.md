# 北冥官网 business-core API 契约

版本：0.1

## 文档定位

本文档是 `business-core` 运行合并单元的正式 API 契约。`business-core` 用于承载第一批已完成闭环的业务模块，包括 `auth`、`profile`、`notification`、`content`、`server-status`、`resource` 和 `admin`。本文档只定义合并后的运行形态、模块装配、自检接口、生产就绪摘要、网关切换边界和验收口径，不替代七个业务模块自己的 API 契约。

本文档继承 `docs/contracts-common.md`、`docs/contracts-auth.md`、`docs/contracts-profile.md`、`docs/contracts-notification.md`、`docs/contracts-content.md`、`docs/contracts-server-status.md`、`docs/contracts-resource.md` 和 `docs/contracts-admin.md`。七个业务模块的路径、方法、认证、权限、请求字段、响应字段、错误码、分页、幂等、状态流转、降级、审计和验收口径仍以各自契约为准。

`business-core` 不是新的业务模块，不新增用户、成员、通知、内容、状态、资源或后台聚合的业务语义。它的目标是减少第一批业务后端的运行进程数量，同时保留模块边界和现有 API 行为。

## 职责边界

`business-core` 负责以下能力。

| 能力 | 说明 |
| --- | --- |
| 运行合并 | 用一个 Spring Boot 运行单元承载第一批七个业务模块。 |
| 模块装配 | 按原模块包名、路由和契约装配 controller、service、adapter、store 和测试替身。 |
| 契约保持 | 保持七个模块既有 API 路径、HTTP 方法、响应结构、错误码、认证、权限、状态流转、幂等和审计行为。 |
| 内部适配 | 把合并前跨服务 HTTP 适配收敛为同进程 adapter 或 facade，但不允许跨模块直接读写主数据。 |
| 自检摘要 | 暴露 `business-core` 自身健康检查、后台装配摘要和生产就绪摘要，便于迁移验证和生产化排障。 |
| 网关切换状态 | 为 `api-gateway` 第一批路径上游切换提供稳定目标，并在切换完成后暴露完成状态。 |

`business-core` 不负责吸收网关能力。第四十七轮后，本地开发态 `api-gateway-service` Maven 入口已退役，网关自有 API 和第一批业务路径统一由 `unified-backend-service:8135` 承接。`business-core` 不负责后续模块，如 `onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、`ops-control`、`external-node-executor` 和 P3 扩展。

`business-core` 不允许把后续模块逻辑塞进第一批模块，不允许让前端直连新增路径吞掉业务，也不允许为了合并修改旧模块的稳定契约。

`unified-backend-service:8135` 可以把 `business-core` 作为 in-process 稳定运行单元挂载，用于验证最终合并成一个后端服务的候选形态。该挂载不得改变 `business-core-service:8130` 的独立入口，不得改变 `/api/v1/business-core/**` 或第一批七个业务路径的认证、响应格式、错误码、请求编号、状态流转、幂等和审计规则，不得把 `business-core` 路径改写到 `/api/v1/unified-backend/**` 下。

## 运行形态

本地验证运行单元为 `backend/business-core-service`，本地验证端口为 `8130`。端口 `8101` 到 `8107` 只作为第一批模块历史原服务端口登记，当前仓库不再保留旧七个微服务源码和 Maven 运行入口。端口 `8125` 只作为已退役旧 `api-gateway-service` 历史端口记录；当前网关能力由 `unified-backend-service:8135` 自承载。

Spring Boot 主应用建议放在 `cn.beiming.core`，组件扫描范围覆盖 `cn.beiming`。第一批模块应保留原包名 `cn.beiming.auth`、`cn.beiming.profile`、`cn.beiming.notification`、`cn.beiming.content`、`cn.beiming.serverstatus`、`cn.beiming.resource` 和 `cn.beiming.admin`。不得为了合并进行无业务收益的大规模包名迁移。

`business-core` 的自有路径前缀为 `/api/v1/business-core`。七个业务模块路径保持原样，不加 `/business-core` 前缀。

## 承载模块

| 模块 | 正式契约 | 路径前缀 | 原服务端口 | 现有代码路由数 | 是否进入 business-core |
| --- | --- | --- | --- | ---: | --- |
| `auth` | `docs/contracts-auth.md` | `/api/v1/auth` | `8101` | 20 | 是 |
| `profile` | `docs/contracts-profile.md` | `/api/v1/profile` | `8102` | 16 | 是 |
| `notification` | `docs/contracts-notification.md` | `/api/v1/notifications` | `8103` | 19 | 是 |
| `content` | `docs/contracts-content.md` | `/api/v1/content` | `8104` | 55 | 是 |
| `server-status` | `docs/contracts-server-status.md` | `/api/v1/server-status` | `8105` | 25 | 是 |
| `resource` | `docs/contracts-resource.md` | `/api/v1/resources` | `8106` | 29 | 是 |
| `admin` | `docs/contracts-admin.md` | `/api/v1/admin` | `8107` | 10 | 是 |

第一批合并后，`business-core` 需要承载以上 174 个既有业务方法路由。`business-core` 自身新增 3 个运行单元自检和生产就绪路由。合并验证总方法路由数为 177。

## API 路径清单

七个业务模块的完整接口定义仍在各模块正式契约中维护。本文档只登记它们在 `business-core` 中的装配范围。

| 路径前缀 | 方法范围 | 业务归属 | 完整接口定义 |
| --- | --- | --- | --- |
| `/api/v1/auth/**` | `GET`、`POST`、`PUT`、`PATCH`、`DELETE` | 账号、会话、角色、权限、邀请码、密码、Minecraft 账号级绑定 | `docs/contracts-auth.md` |
| `/api/v1/profile/**` | `GET`、`POST`、`PUT`、`PATCH` | 成员档案、公开成员、成员组、成员事迹、作品快照 | `docs/contracts-profile.md` |
| `/api/v1/notifications/**` | `GET`、`POST`、`PATCH` | 站内通知、模板、收件人状态、通知审计 | `docs/contracts-notification.md` |
| `/api/v1/content/**` | `GET`、`POST`、`PUT`、`PATCH` | 首页配置、内容、专题、分类、标签、SEO、预览、内容审计 | `docs/contracts-content.md` |
| `/api/v1/server-status/**` | `GET`、`POST`、`PATCH` | 玩家可见状态、线路、历史快照、状态源、宕机记录 | `docs/contracts-server-status.md` |
| `/api/v1/resources/**` | `GET`、`POST`、`PATCH` | 玩家资源、版本、分类、下载、Cloudreve 分享链接、资源审计 | `docs/contracts-resource.md` |
| `/api/v1/admin/**` | `GET`、`PATCH` | 后台总览、模块注册表、待办、指标、审计索引、系统配置 | `docs/contracts-admin.md` |
| `/api/v1/business-core/**` | `GET` | `business-core` 运行单元自检和生产就绪摘要 | 本文档 |

路径匹配必须保持既有模块前缀，不得把 `/api/v1/resources/**` 误匹配到其他模块，不得把 `/api/v1/resourceful` 误命中 `resource`，不得把 `/api/v1/admin/**` 路径交给其他模块处理。

## 自有对象

### BusinessCoreModuleStatus

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `moduleKey` | string | 是 | 模块键，允许 `AUTH`、`PROFILE`、`NOTIFICATION`、`CONTENT`、`SERVER_STATUS`、`RESOURCE`、`ADMIN`。 |
| `moduleName` | string | 是 | 模块展示名。 |
| `pathPrefix` | string | 是 | 模块路径前缀。 |
| `contract` | string | 是 | 模块正式契约文件路径。 |
| `legacyPort` | integer | 是 | 旧微服务端口。 |
| `mounted` | boolean | 是 | 模块是否已装配到 `business-core`。 |
| `routesTotal` | integer | 是 | 该模块在当前运行单元内登记的路由数量。 |
| `contractRoutesTotal` | integer | 是 | 该模块契约期望路由数量。 |
| `adapters` | string[] | 是 | 当前模块需要的内部 adapter 或 facade 摘要。 |
| `compatibilityMode` | string | 是 | `LEGACY_BASELINE`、`IN_PROCESS_ADAPTER` 或 `GATEWAY_SWITCH_READY`。 |
| `lastVerifiedAt` | string 或 null | 是 | 最近一次契约测试通过时间。 |
| `status` | string | 是 | `NOT_MOUNTED`、`MOUNTED`、`DEGRADED` 或 `READY`。 |
| `gaps` | string[] | 是 | 当前模块仍未完成的迁移或生产化缺口。 |

### BusinessCoreOpsSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `service` | string | 是 | 固定为 `business-core`。 |
| `port` | integer | 是 | 本地验证固定为 `8130`。 |
| `status` | string | 是 | `UP`、`DEGRADED` 或 `DOWN`。 |
| `modulesTotal` | integer | 是 | 固定为 `7`。 |
| `modulesMounted` | integer | 是 | 已装配模块数量。 |
| `routesTotal` | integer | 是 | 当前运行单元登记路由总数。 |
| `businessRoutesTotal` | integer | 是 | 七个业务模块方法路由总数，完成后为 `174`。 |
| `selfRoutesTotal` | integer | 是 | `business-core` 自有路由总数，固定为 `3`。 |
| `moduleRoutes` | `BusinessCoreModuleStatus[]` | 是 | 七个模块装配状态。 |
| `gatewaySwitchReady` | boolean | 是 | 是否已满足网关切换前置条件。网关切换完成后仍为 `true`。 |
| `gatewaySwitchStatus` | string | 是 | 网关切换状态，允许 `NOT_READY`、`READY` 或 `COMPLETED`。 |
| `legacyBaselines` | object[] | 是 | 当前仍保留的外部基线摘要。第四十七轮后指向 `unified-backend-service`，不再要求运行已退役的 `api-gateway-service` Maven 入口。 |
| `retiredLegacyServices` | string[] | 是 | 已由 `business-core` 替代并清理源码的第一批旧服务。 |
| `productionGaps` | string[] | 是 | 生产化差距摘要。 |
| `generatedAt` | string | 是 | 摘要生成时间。 |

### BusinessCoreProductionReadiness

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `service` | string | 是 | 固定为 `business-core`。 |
| `port` | integer | 是 | 本地验证固定为 `8130`。 |
| `productionReady` | boolean | 是 | 是否达到生产就绪。只有所有阻塞项完成并有真实联调记录时才能为 `true`。 |
| `readinessStatus` | string | 是 | `NOT_READY`、`PARTIAL` 或 `READY_FOR_PRODUCTION`。当前未完成真实持久化、真实认证边界和真实 HTTP 联调时必须为 `NOT_READY`。 |
| `routeSummary` | object | 是 | 路由摘要，包含 `businessRoutesTotal`、`selfRoutesTotal` 和 `routesTotal`。 |
| `blockingGaps` | object[] | 是 | 阻塞生产化的缺口清单。每项包含 `gapKey`、`category`、`severity`、`ownerModule`、`currentMode`、`requiredMode`、`nextAction` 和 `verification`。 |
| `gapsTotal` | integer | 是 | 阻塞缺口总数。 |
| `criticalGapsTotal` | integer | 是 | 严重缺口数量。 |
| `highGapsTotal` | integer | 是 | 高风险缺口数量。 |
| `integrationChecks` | object[] | 是 | 生产联调检查项。每项包含 `checkKey`、`status`、`evidence` 和 `requiredBeforeProduction`。 |
| `testScope` | object | 是 | 当前测试覆盖摘要，必须区分 MockMvc、本地 Maven、旧服务退役检查、网关路由切换和真实 HTTP 联调。 |
| `testControls` | object | 是 | 测试控制头生产隔离摘要，包含 `productionGuardRequired`、`productionGuardStatus`、`knownControlHeaders` 和 `risk`。 |
| `sourceDrift` | object | 是 | 第一批旧服务源码退役后的漂移风险摘要。 |
| `nextDevelopmentOrder` | string[] | 是 | 后续生产化建议顺序。 |
| `legacyBaselinesKept` | boolean | 是 | 旧七服务源码基线是否仍保留作回归基线。旧源码清理后必须为 `false`。 |
| `generatedAt` | string | 是 | ISO 8601 时间。 |

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| business-core 健康检查 | GET | `/api/v1/business-core/health` | 否 | 无 | LOW |
| business-core 后台装配摘要 | GET | `/api/v1/business-core/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |
| business-core 生产就绪摘要 | GET | `/api/v1/business-core/admin/production-readiness` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 七个业务模块接口 | 继承各模块契约 | `/api/v1/auth/**`、`/api/v1/profile/**`、`/api/v1/notifications/**`、`/api/v1/content/**`、`/api/v1/server-status/**`、`/api/v1/resources/**`、`/api/v1/admin/**` | 继承各模块契约 | 继承各模块契约 | 继承各模块契约 |

## 健康检查

`GET /api/v1/business-core/health`

该接口无需认证，只表示 `business-core` 进程和运行单元自检能力可用，不表示七个业务模块全部契约通过。

请求字段：无。

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "business-core",
    "status": "UP",
    "port": 8130,
    "modulesTotal": 7,
    "modulesMounted": 7,
    "businessRoutesTotal": 174,
    "selfRoutesTotal": 3,
    "moduleRoutes": [
      {
        "moduleKey": "AUTH",
        "pathPrefix": "/api/v1/auth",
        "mounted": true,
        "routesTotal": 20,
        "status": "READY"
      }
    ],
    "generatedAt": "2026-06-02T05:26:20Z"
  },
  "requestId": "req_example"
}
```

响应字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `service` | string | 是 | 固定为 `business-core`。 |
| `status` | string | 是 | `UP`、`DEGRADED` 或 `DOWN`。 |
| `port` | integer | 是 | 本地验证固定为 `8130`。 |
| `modulesTotal` | integer | 是 | 固定为 `7`。 |
| `modulesMounted` | integer | 是 | 已装配模块数量。 |
| `businessRoutesTotal` | integer | 是 | 七个业务模块方法路由总数，完成后为 `174`。 |
| `selfRoutesTotal` | integer | 是 | `business-core` 自有路由数，固定为 `3`。 |
| `moduleRoutes` | object[] | 是 | 低敏模块路由摘要，只返回 `moduleKey`、`pathPrefix`、`mounted`、`routesTotal` 和 `status`。 |
| `generatedAt` | string | 是 | ISO 8601 时间。 |

失败规则：运行单元内部异常返回 HTTP `500` 和错误码 `51730`。模块装配异常导致无法生成健康摘要时返回 HTTP `500` 和错误码 `51731`。该接口不得返回 token、Cookie、真实数据库连接串、异常栈、外部凭据或请求头原文。

分页规则：无分页。

幂等规则：只读接口，重复调用不改变任何业务状态。

审计要求：无后台业务审计要求，但必须保留请求编号，便于运行日志排障。

## 后台装配摘要

`GET /api/v1/business-core/admin/ops/summary`

该接口需要 `Authorization: Bearer <token>`。只有 `ADMIN` 和 `OWNER` 可访问。未登录返回公共错误码 `41000`，令牌格式错误返回 `41003`，权限不足返回 `42001`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时，按认证上下文返回对应模块契约错误或公共认证错误。

请求字段：无。

成功响应 HTTP `200`，`data` 为 `BusinessCoreOpsSummary`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "business-core",
    "port": 8130,
    "status": "UP",
    "modulesTotal": 7,
    "modulesMounted": 7,
    "routesTotal": 177,
    "businessRoutesTotal": 174,
    "selfRoutesTotal": 3,
    "moduleRoutes": [],
    "gatewaySwitchReady": true,
    "gatewaySwitchStatus": "COMPLETED",
    "legacyBaselines": [
      {
        "service": "unified-backend-service",
        "port": 8135,
        "contract": "docs/contracts-unified-backend.md",
        "testCommand": "mvn -f backend/unified-backend-service/pom.xml test",
        "lastVerifiedAt": "2026-06-02T15:34:38+08:00"
      }
    ],
    "retiredLegacyServices": [
      "auth-service",
      "profile-service",
      "notification-service",
      "content-service",
      "server-status-service",
      "resource-service",
      "admin-service"
    ],
    "productionGaps": [
      "real database persistence is still module dependent"
    ],
    "generatedAt": "2026-06-02T05:26:20Z"
  },
  "requestId": "req_example"
}
```

业务规则：该接口只读取 `business-core` 内部装配状态和最近测试摘要，不主动执行七个模块的业务写操作，不调用旧服务进行实时健康探测，不把未完成模块伪装成 `READY`。只有当七个模块全部装配、七个模块在 `business-core` 中的继承契约测试通过、第一批旧服务源码已按确认范围清理、`api-gateway` 基线通过时，`gatewaySwitchReady` 才能为 `true`。只有当 `api-gateway` 契约、测试文档、自动化红灯、网关实现和全量后端回归均完成后，`gatewaySwitchStatus` 才能为 `COMPLETED`。

失败规则：运行单元内部异常返回 `51730`。模块装配信息缺失返回 `51731`。当前登记路由与本文档或七个模块契约期望不一致时返回 `51732` 或在 `status=DEGRADED` 的成功摘要中列入 `gaps`，由实现按是否影响接口可用性决定。认证上下文解析失败返回原模块契约或公共认证错误，可信网关上下文字段缺失或格式不兼容时返回 `51733`。

分页规则：无分页。

幂等规则：只读接口，重复调用不改变任何业务状态。

状态流转：该接口不改变业务状态。模块装配状态只允许按迁移过程从 `NOT_MOUNTED` 进入 `MOUNTED`，契约测试通过后进入 `READY`，发现路由缺失、adapter 不可用或继承测试失败时进入 `DEGRADED`。

审计要求：读取后台装配摘要属于低风险后台读取，应保留请求编号、操作者、角色和访问时间的运行日志。不得记录 token 原文。

## 生产就绪摘要

`GET /api/v1/business-core/admin/production-readiness`

该接口需要 `Authorization: Bearer <token>`。只有 `ADMIN` 和 `OWNER` 可访问。未登录返回公共错误码 `41000`，令牌格式错误返回 `41003`，权限不足返回 `42001`。`HELPER` 和 `USER` 均不得读取生产就绪摘要。

请求字段：无。

成功响应 HTTP `200`，`data` 为 `BusinessCoreProductionReadiness`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "business-core",
    "port": 8130,
    "productionReady": false,
    "readinessStatus": "NOT_READY",
    "routeSummary": {
      "businessRoutesTotal": 174,
      "selfRoutesTotal": 3,
      "routesTotal": 177
    },
    "blockingGaps": [
      {
        "gapKey": "LIVE_GATEWAY_HTTP_SMOKE_NOT_VERIFIED",
        "category": "INTEGRATION",
        "severity": "HIGH",
        "ownerModule": "BUSINESS_CORE",
        "currentMode": "MOCKMVC_AND_MAVEN_CONTRACTS",
        "requiredMode": "REAL_HTTP_GATEWAY_TO_BUSINESS_CORE",
        "nextAction": "启动 unified-backend-service 与 business-core-service，执行经统一后端访问第一批路径的真实 HTTP 冒烟测试。",
        "verification": "记录命令、端口、请求路径、响应码、请求编号和失败降级结果到 .local-docs/tests-business-core.md。"
      }
    ],
    "gapsTotal": 4,
    "criticalGapsTotal": 0,
    "highGapsTotal": 4,
    "integrationChecks": [],
    "testScope": {},
    "testControls": {
      "productionGuardRequired": true,
      "productionGuardStatus": "ENFORCED_OUTSIDE_TEST_MODE",
      "knownControlHeaders": [
        "X-Test-Fail-Audit",
        "X-Test-Notification-Mode"
      ],
      "risk": "TEST_CONTROLS_ARE_REJECTED_WHEN_PRODUCTION_GUARD_IS_DISABLED"
    },
    "sourceDrift": {
      "risk": "LEGACY_SOURCE_RETIRED",
      "retiredLegacyServices": [
        "auth-service",
        "profile-service",
        "notification-service",
        "content-service",
        "server-status-service",
        "resource-service",
        "admin-service"
      ],
      "activeRuntime": "business-core-service",
      "guardRequired": false,
      "policy": "BUSINESS_CORE_OWNS_FIRST_BATCH_RUNTIME"
    },
    "nextDevelopmentOrder": [
      "LIVE_GATEWAY_HTTP_SMOKE",
      "PRODUCTION_AUTH_CONTEXT",
      "PERSISTENCE_AND_AUDIT"
    ],
    "legacyBaselinesKept": false,
    "generatedAt": "2026-06-03T00:00:00Z"
  },
  "requestId": "req_example"
}
```

业务规则：生产就绪摘要必须诚实暴露 `business-core` 尚未完成的生产化阻塞项，不能因为合并测试全绿就返回 `productionReady=true`。当前至少要列出真实网关到 `business-core` HTTP 联调未验证、真实数据库持久化未接入、真实审计持久化未接入、生产认证上下文或网关内部签名未接入。该接口只读，不执行联调，不访问旧服务端口，不连接数据库，不调用真实外部依赖。第一批旧服务源码清理后，`SOURCE_DRIFT_GUARD` 集成检查必须返回 `PASS`，且该项不得继续作为阻塞缺口。测试控制头生产隔离完成后，`TEST_CONTROL_GUARD` 集成检查必须返回 `PASS`，且该项不得继续作为阻塞缺口。

`integrationChecks` 至少包含以下检查项：`LIVE_GATEWAY_HTTP_SMOKE`、`PERSISTENT_DATABASE`、`PERSISTENT_AUDIT`、`PRODUCTION_AUTH_CONTEXT`、`GATEWAY_INTERNAL_SIGNATURE`、`TEST_CONTROL_GUARD`、`SOURCE_DRIFT_GUARD`。状态允许 `PASS`、`PARTIAL`、`NOT_VERIFIED`、`NOT_CONNECTED` 或 `REQUIRED`。

`testScope` 必须明确 `mockMvcContractTests`、`legacyBaselineTests`、`apiGatewayRouteSwitchTests` 和 `liveHttpSmokeTests` 的覆盖状态。当前真实 HTTP 联调未执行时，`liveHttpSmokeTests.status` 必须为 `NOT_VERIFIED`。

失败规则：运行单元内部异常返回 `51730`。模块装配信息缺失返回 `51731`。生产就绪摘要生成时发现路由总数与契约不一致，返回 `51732` 或在成功摘要中列入对应阻塞项。认证上下文解析失败返回原模块契约或公共认证错误，可信网关上下文字段缺失或格式不兼容时返回 `51733`。

分页规则：无分页。

幂等规则：只读接口，重复调用不改变任何业务状态。

审计要求：读取生产就绪摘要属于低风险后台读取，应保留请求编号、操作者、角色和访问时间的运行日志。响应不得包含 token、Cookie、完整请求头、数据库连接串、异常栈、外部凭据、真实分享密码、节点密钥或本地绝对运行路径。

## 认证上下文

七个业务模块继续兼容 `Authorization: Bearer <token>`。需要登录或后台权限的接口仍按各模块契约解析当前用户、角色、权限、Minecraft 绑定和用户状态。

经 `api-gateway` 访问时，`business-core` 继续兼容以下可信身份头。

| 请求头 | 说明 |
| --- | --- |
| `X-Beiming-Actor-User-Id` | 当前用户 ID。 |
| `X-Beiming-Actor-Roles` | 逗号分隔角色。 |
| `X-Beiming-Actor-Permissions` | 逗号分隔能力点。 |
| `X-Beiming-Actor-Minecraft-Id` | 已绑定的 Minecraft ID。 |
| `X-Beiming-Actor-Minecraft-Uuid` | 已绑定的 Minecraft UUID。 |
| `X-Gateway-Internal-Request-Id` | 网关注入的内部请求编号。 |

`X-Gateway-Internal-Request-Id` 存在时，各模块按自身契约优先解析可信认证上下文。字段缺失、格式非法、角色或能力点不兼容时，不得静默降级成匿名用户。生产入口必须由 `api-gateway` 或反向代理剥离客户端伪造的同名可信头；直连本地测试必须覆盖伪造头不能绕过权限的场景。

## 测试控制头生产隔离

`business-core` 允许本地自动化测试通过 `X-Test-*` 请求头模拟审计失败、存储失败、通知失败、模块降级和平台降级。该能力只属于测试环境，不属于正式业务 API。

运行单元必须提供中央开关 `beiming.business-core.test-control-headers.enabled`。该开关为 `true` 时，`X-Test-*` 请求头按各模块测试契约继续生效。该开关为 `false` 时，所有 `/api/v1/**` 请求只要携带任意 `X-Test-*` 请求头，就必须在 `business-core` 装配层返回 HTTP `400` 和错误码 `51735`。拒绝响应必须使用统一响应格式，保留 `requestId`，响应头必须带同一个 `X-Request-Id`。

生产模式拒绝测试控制头时，不得继续调用业务 controller，不得创建、修改或删除业务数据，不得写入由该请求触发的业务审计，不得触发模块降级、通知失败、存储失败或审计失败测试钩子。拒绝响应不得暴露被拒绝请求头的值、Authorization、Cookie、数据库连接串、异常栈、本地绝对路径、外部凭据、真实分享密码或节点密钥。拒绝响应体中的 `requestId` 必须保持合法 JSON 字符串，不能因请求编号包含引号、反斜杠或控制字符导致响应不可解析。

验收口径为生产模式下携带 `X-Test-Fail-Audit`、`X-Test-Notification-Mode`、`X-Test-Fail-Store`、`X-Test-Fail-Download-Record`、`X-Test-Module-Mode` 或 `X-Test-Platform-Mode` 调用任意第一批业务路径和 `business-core` 自有路径，均返回 `51735`。本地测试模式下，既有继承测试仍能使用这些请求头完成失败降级和回滚验证。

## 内部适配规则

`auth` 仍拥有账号、会话、角色、能力点、邀请码和 Minecraft 账号级绑定主数据。

`profile` 仍拥有成员档案、公开成员字段、成员组、事迹和作品快照主数据。它只能通过认证上下文或 auth adapter 读取账号快照，不能直接写 auth 用户状态。

`notification` 仍拥有通知、模板、收件人状态、未读数、归档状态和通知审计主数据。

`content` 仍拥有首页配置、内容、专题、分类、标签、SEO、预览令牌和内容审计主数据。强制通知失败回滚、辅助通知失败不阻塞的规则必须保留。

`server-status` 仍只负责玩家可见状态、线路、历史快照、MOTD、在线人数、延迟和状态降级，不能承接真实服务器运维操作。

`resource` 仍只负责玩家可见资源、版本、分类、下载权限和 Cloudreve 分享链接，不能承接运维文件管理能力。

`admin` 仍是后台聚合入口，只能通过模块 adapter 汇总状态、待办、指标和审计摘要，不能替业务模块处理审核、资源状态、内容状态或通知投递。

同 JVM 内部调用可以从 HTTP client 改为 adapter 或 facade，但 adapter 必须保留失败模拟能力，测试必须能覆盖 auth 不可用、profile 快照失败、notification 投递失败、状态采集失败和资源外部依赖失败。

## 错误码

七个业务模块接口继续使用各自契约中的错误码，不因进入 `business-core` 改码。公共错误码继续继承 `docs/contracts-common.md`。

`business-core` 自有错误码如下。

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `51730` | 500 | business-core 内部错误。 |
| `51731` | 500 | business-core 模块装配错误。 |
| `51732` | 500 | business-core 路由快照与契约不一致。 |
| `51733` | 502 | business-core 可信认证上下文解析失败。 |
| `51734` | 500 | business-core 生产就绪摘要生成失败。 |
| `51735` | 400 | business-core 生产模式拒绝测试控制头。 |

以上错误码只用于 `/api/v1/business-core/**` 自有接口，或用于运行单元装配层在请求到达业务模块前发生的错误。已经进入七个业务模块处理流程的请求，错误码必须按对应模块契约返回。

## 网关策略

第一批网关切换已完成。第四十七轮后，本地开发态 `api-gateway-service` 独立 Maven 入口已退役；`unified-backend-service:8135` 自承载网关能力，并保持 `auth`、`profile`、`notification`、`content`、`server-status`、`resource` 和 `admin` 业务路径对 `business-core-service` 约定边界的兼容。前端仍通过原 API 路径访问，不新增前端直连约定。

后续若再次调整网关路由，必须先更新 `docs/contracts-api-gateway.md` 和 `.local-docs/tests-api-gateway.md`，确认测试红灯后再修改网关实现。

网关切换后，业务路径仍保持 `/api/v1/auth/**`、`/api/v1/profile/**`、`/api/v1/notifications/**`、`/api/v1/content/**`、`/api/v1/server-status/**`、`/api/v1/resources/**` 和 `/api/v1/admin/**`，不得改成 `/api/v1/business-core/<module>/**`。

## 迁移顺序

迁移顺序固定为基线验证、`business-core` 空壳与自检红灯、`auth`、`profile`、`notification`、`content`、`server-status`、`resource`、`admin`、网关切换准备、网关切换实现、全量回归。

`auth` 必须先迁入，因为其他受保护接口都依赖认证上下文。`admin` 必须最后迁入，因为它依赖前面模块的可见状态、待办、指标和审计摘要。

每迁入一个模块，都必须先根据本文档和该模块正式契约生成或补齐 `business-core` 自动化测试，确认测试因为模块未装配或行为不满足而失败，再迁入实现。迁入后必须运行 `business-core` 对应测试和该模块前序依赖测试。

## 验收口径

`business-core` 完成的最低标准是，单进程承载第一批七个业务模块的全部既有 API 路径，且响应格式、错误码、认证、权限、请求编号、分页、状态流转、幂等、审计和降级行为与七个模块正式契约一致。

`mvn -f backend/business-core-service/pom.xml test` 必须覆盖本文档三个自有接口和七个模块继承过来的全部契约测试。第一批旧服务源码清理后，`BusinessCoreLegacyBaselineTest` 必须确认旧服务运行文件不存在，且七个模块正式契约和 `business-core-service` 运行入口仍存在。

`business-core` 直连合并和第一批网关切换均已完成测试闭环。用户确认后，第一批旧服务源码和 Maven 运行入口已按明确文件路径逐个清理，后续运行以 `business-core-service` 为准。

旧服务目录不得因本契约自动批量删除。需要继续清理残留空目录或其他批次旧服务时，必须单独确认范围；删除文件只能逐个明确路径处理。
