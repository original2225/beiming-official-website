# 北冥官网 admission-core API 契约

版本：0.2

## 文档定位

本文档是 `admission-core` 运行合并单元的正式 API 契约。`admission-core` 用于承载第二批入服流程后端模块，包括 `onboarding`、`exam`、`whitelist` 和 `attendance`。本文档只定义合并后的运行形态、模块装配、自检接口、交接边界、网关切换边界和验收口径，不替代四个业务模块自己的 API 契约。

本文档继承 `docs/contracts-common.md`、`docs/contracts-onboarding.md`、`docs/contracts-exam.md`、`docs/contracts-whitelist.md` 和 `docs/contracts-attendance.md`。四个业务模块的路径、方法、认证、权限、请求字段、响应字段、错误码、分页、幂等、状态流转、降级、审计和验收口径仍以各自契约为准。

`admission-core` 不是新的业务模块，不新增入服引导、考试、白名单或考勤积分的业务语义。它的目标是把强状态流的准入链路收敛为一个 Spring Boot 运行单元，同时保留模块边界、交接快照和原 API 行为。

## 职责边界

`admission-core` 负责以下能力。

| 能力 | 说明 |
| --- | --- |
| 运行合并 | 用一个 Spring Boot 运行单元承载第二批四个入服链路模块。 |
| 模块装配 | 按原模块包名、路由和契约装配 controller、service、adapter、store 和测试替身。 |
| 契约保持 | 保持四个模块既有 API 路径、HTTP 方法、响应结构、错误码、认证、权限、状态流转、幂等和审计行为。 |
| 交接保持 | 保留 onboarding 到 exam、exam 到 whitelist、whitelist 到 attendance 的只读 handoff 语义。 |
| 内部适配 | 把第二批内部跨服务 HTTP 适配收敛为同进程 adapter 或 facade，但不允许跨模块直接读写主数据。 |
| 前序适配 | 通过 `business-core` 或前序模块正式接口适配 auth、profile、notification、content、server-status、resource 和 admin。 |
| 自检摘要 | 暴露 `admission-core` 自身健康检查和后台装配摘要，便于迁移验证。 |
| 网关切换状态 | 为 `api-gateway` 第二批路径上游切换提供稳定目标和完成状态。 |

`admission-core` 不负责吸收网关能力。第四十七轮后，本地开发态 `api-gateway-service` Maven 入口已退役，网关自有 API 和第二批业务路径统一由 `backend:8135` 承接。`admission-core` 不负责第一批基础业务模块，也不负责 `community`、`activity`、`calendar`、`changelog`、`ops-control`、`external-node-executor`、`cloudreve-sync`、`backup-recovery`、`alerting`、`online-map`、插件集成或 P3 扩展。

`admission-core` 不允许把真实服务器白名单命令、Minecraft 控制台、节点守护进程、容器、终端、文件管理、备份恢复或 Cloudreve 管理塞进入服链路。白名单审核通过只代表官网业务状态和成员档案激活交接，不代表真实服务器命令已执行。

`backend:8135` 可以把 `admission-core` 作为 in-process 稳定运行单元挂载，用于验证最终合并成一个后端服务的候选形态。该挂载不得改变 `admission-core-service:8131` 的独立入口，不得改变 `/api/v1/admission-core/**`、`/api/v1/onboarding/**`、`/api/v1/exams/**`、`/api/v1/whitelist/**` 或 `/api/v1/attendance/**` 的认证、响应格式、错误码、请求编号、状态流转、幂等和审计规则，不得把 `admission-core` 路径改写到 `/api/v1/unified-backend/**` 下。

## 运行形态

本地验证运行单元为 `backend/admission-core-service`，本地验证端口为 `8131`。端口 `8108` 到 `8111` 只作为第二批模块历史原服务端口记录，不再作为回归基线。端口 `8130` 继续保留给 `business-core-service`，端口 `8125` 只作为已退役旧 `api-gateway-service` 历史端口记录；当前网关能力由 `backend:8135` 自承载。

Spring Boot 主应用建议放在 `cn.beiming.admission`，组件扫描范围覆盖 `cn.beiming`。第二批模块应保留原包名 `cn.beiming.onboarding`、`cn.beiming.exam`、`cn.beiming.whitelist` 和 `cn.beiming.attendance`。不得为了合并进行无业务收益的大规模包名迁移。

`admission-core` 的自有路径前缀为 `/api/v1/admission-core`。四个业务模块路径保持原样，不加 `/admission-core` 前缀。

## 承载模块

| 模块 | 正式契约 | 路径前缀 | 原服务端口 | 现有代码路由数 | 是否进入 admission-core |
| --- | --- | --- | --- | ---: | --- |
| `onboarding` | `docs/contracts-onboarding.md` | `/api/v1/onboarding` | `8108` | 15 | 是 |
| `exam` | `docs/contracts-exam.md` | `/api/v1/exams` | `8109` | 28 | 是 |
| `whitelist` | `docs/contracts-whitelist.md` | `/api/v1/whitelist` | `8110` | 20 | 是 |
| `attendance` | `docs/contracts-attendance.md` | `/api/v1/attendance` | `8111` | 21 | 是 |

第二批合并后，`admission-core` 需要承载以上 84 个既有业务方法路由。`admission-core` 自身新增 2 个运行单元自检路由。合并验证总方法路由数为 86。

## API 路径清单

四个业务模块的完整接口定义仍在各模块正式契约中维护。本文档只登记它们在 `admission-core` 中的装配范围。

| 路径前缀 | 方法范围 | 业务归属 | 完整接口定义 |
| --- | --- | --- | --- |
| `/api/v1/onboarding/**` | `GET`、`POST`、`PATCH` | 入服引导、规则确认、方向选择、exam 交接快照 | `docs/contracts-onboarding.md` |
| `/api/v1/exams/**` | `GET`、`POST`、`PUT`、`PATCH` | 考试、题库、模板、阅卷、whitelist 交接快照 | `docs/contracts-exam.md` |
| `/api/v1/whitelist/**` | `GET`、`POST`、`PATCH` | 白名单申请、审核、移除、attendance 交接快照 | `docs/contracts-whitelist.md` |
| `/api/v1/attendance/**` | `GET`、`POST`、`PATCH` | 考勤账户、积分流水、贡献、月度扣分、候选、榜单 | `docs/contracts-attendance.md` |
| `/api/v1/admission-core/**` | `GET` | `admission-core` 运行单元自检 | 本文档 |

路径匹配必须保持既有模块前缀。`/api/v1/onboarding/**` 不得被改成 `/api/v1/admission-core/onboarding/**`。`/api/v1/exams/**` 不得误命中 onboarding。`/api/v1/attendance/**` 不得误命中 whitelist。相似路径如 `/api/v1/examiner`、`/api/v1/whitelisted` 和 `/api/v1/attend` 不得误命中第二批业务模块。

## 自有对象

### AdmissionCoreModuleStatus

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `moduleKey` | string | 是 | 模块键，允许 `ONBOARDING`、`EXAM`、`WHITELIST`、`ATTENDANCE`。 |
| `moduleName` | string | 是 | 模块展示名。 |
| `pathPrefix` | string | 是 | 模块路径前缀。 |
| `contract` | string | 是 | 模块正式契约文件路径。 |
| `legacyPort` | integer | 是 | 旧模块端口。 |
| `mounted` | boolean | 是 | 模块是否已装配到 `admission-core`。 |
| `routesTotal` | integer | 是 | 该模块在当前运行单元内登记的路由数量。 |
| `contractRoutesTotal` | integer | 是 | 该模块契约期望路由数量。 |
| `handoffIn` | string[] | 是 | 该模块消费的上游交接摘要。 |
| `handoffOut` | string[] | 是 | 该模块输出的下游交接摘要。 |
| `adapters` | string[] | 是 | 当前模块需要的内部 adapter 或 facade 摘要。 |
| `compatibilityMode` | string | 是 | `LEGACY_BASELINE`、`IN_PROCESS_ADAPTER` 或 `GATEWAY_SWITCH_READY`。 |
| `lastVerifiedAt` | string 或 null | 是 | 最近一次契约测试通过时间。 |
| `status` | string | 是 | `NOT_MOUNTED`、`MOUNTED`、`DEGRADED` 或 `READY`。 |
| `gaps` | string[] | 是 | 当前模块仍未完成的迁移或生产化缺口。 |

### AdmissionCoreOpsSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `service` | string | 是 | 固定为 `admission-core`。 |
| `port` | integer | 是 | 本地验证固定为 `8131`。 |
| `status` | string | 是 | `UP`、`DEGRADED` 或 `DOWN`。 |
| `modulesTotal` | integer | 是 | 固定为 `4`。 |
| `modulesMounted` | integer | 是 | 已装配模块数量。 |
| `routesTotal` | integer | 是 | 当前运行单元登记路由总数。 |
| `admissionRoutesTotal` | integer | 是 | 四个业务模块方法路由总数，完成后为 `84`。 |
| `selfRoutesTotal` | integer | 是 | `admission-core` 自有路由总数，固定为 `2`。 |
| `moduleRoutes` | `AdmissionCoreModuleStatus[]` | 是 | 四个模块装配状态。 |
| `handoffChain` | object[] | 是 | onboarding、exam、whitelist 和 attendance 的交接链摘要。 |
| `businessCoreDependency` | object | 是 | 第一批 `business-core` 前序依赖摘要。 |
| `gatewaySwitchReady` | boolean | 是 | 是否已满足网关切换前置条件。 |
| `gatewaySwitchStatus` | string | 是 | 网关切换状态，允许 `NOT_READY`、`READY` 或 `COMPLETED`。 |
| `legacyBaselines` | object[] | 是 | 当前仍保留的外部基线摘要。第二批旧四服务清理后包含 `business-core-service` 和 `backend`，不再要求运行已退役的 `api-gateway-service` Maven 入口。 |
| `productionGaps` | string[] | 是 | 生产化差距摘要。 |
| `generatedAt` | string | 是 | 摘要生成时间。 |

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| admission-core 健康检查 | GET | `/api/v1/admission-core/health` | 否 | 无 | LOW |
| admission-core 后台装配摘要 | GET | `/api/v1/admission-core/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 四个业务模块接口 | 继承各模块契约 | `/api/v1/onboarding/**`、`/api/v1/exams/**`、`/api/v1/whitelist/**`、`/api/v1/attendance/**` | 继承各模块契约 | 继承各模块契约 | 继承各模块契约 |

## 健康检查

`GET /api/v1/admission-core/health`

该接口无需认证，只表示 `admission-core` 进程和运行单元自检能力可用，不表示四个业务模块全部契约通过。

请求字段：无。

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "admission-core",
    "status": "UP",
    "port": 8131,
    "modulesTotal": 4,
    "modulesMounted": 4,
    "admissionRoutesTotal": 84,
    "selfRoutesTotal": 2,
    "moduleRoutes": [
      {
        "moduleKey": "ONBOARDING",
        "pathPrefix": "/api/v1/onboarding",
        "mounted": true,
        "routesTotal": 15,
        "status": "READY"
      }
    ],
    "generatedAt": "2026-06-02T08:00:00Z"
  },
  "requestId": "req_example"
}
```

响应字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `service` | string | 是 | 固定为 `admission-core`。 |
| `status` | string | 是 | `UP`、`DEGRADED` 或 `DOWN`。 |
| `port` | integer | 是 | 本地验证固定为 `8131`。 |
| `modulesTotal` | integer | 是 | 固定为 `4`。 |
| `modulesMounted` | integer | 是 | 已装配模块数量。 |
| `admissionRoutesTotal` | integer | 是 | 四个业务模块方法路由总数，完成后为 `84`。 |
| `selfRoutesTotal` | integer | 是 | `admission-core` 自有路由数，固定为 `2`。 |
| `moduleRoutes` | object[] | 是 | 低敏模块路由摘要，只返回 `moduleKey`、`pathPrefix`、`mounted`、`routesTotal` 和 `status`。 |
| `generatedAt` | string | 是 | ISO 8601 时间。 |

失败规则：运行单元内部异常返回 HTTP `500` 和错误码 `53130`。模块装配异常导致无法生成健康摘要时返回 HTTP `500` 和错误码 `53131`。该接口不得返回 token、Cookie、真实数据库连接串、异常栈、外部凭据、请求头原文、考试答案、通知正文、Minecraft 验证凭据、真实服务器命令或节点凭据。

分页规则：无分页。

幂等规则：只读接口，重复调用不改变任何业务状态。

审计要求：无后台业务审计要求，但必须保留请求编号，便于运行日志排障。

## 后台装配摘要

`GET /api/v1/admission-core/admin/ops/summary`

该接口需要 `Authorization: Bearer <token>`。只有 `ADMIN` 和 `OWNER` 可访问。未登录返回公共错误码 `41000`，令牌格式错误返回 `41003`，权限不足返回 `42001`。`HELPER` 和 `USER` 均不得读取第二批运行单元装配摘要。

请求字段：无。

成功响应 HTTP `200`，`data` 为 `AdmissionCoreOpsSummary`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "admission-core",
    "port": 8131,
    "status": "UP",
    "modulesTotal": 4,
    "modulesMounted": 4,
    "routesTotal": 86,
    "admissionRoutesTotal": 84,
    "selfRoutesTotal": 2,
    "moduleRoutes": [],
    "handoffChain": [
      {
        "from": "onboarding",
        "to": "exam",
        "handoff": "OnboardingExamHandoffSnapshot",
        "mutable": false
      },
      {
        "from": "exam",
        "to": "whitelist",
        "handoff": "ExamWhitelistHandoffSnapshot",
        "mutable": false
      },
      {
        "from": "whitelist",
        "to": "attendance",
        "handoff": "WhitelistAttendanceHandoffSnapshot",
        "mutable": false
      }
    ],
    "businessCoreDependency": {
      "service": "business-core",
      "port": 8130,
      "status": "REQUIRED_BASELINE"
    },
    "gatewaySwitchReady": true,
    "gatewaySwitchStatus": "COMPLETED",
    "legacyBaselines": [
      {
        "service": "business-core-service",
        "port": 8130,
        "contract": "docs/contracts-business-core.md",
        "testCommand": "mvn -q -f backend/pom.xml test",
        "lastVerifiedAt": null
      }
    ],
    "productionGaps": [
      "real database persistence is still module dependent",
      "real cross-service adapters are still test stubs",
      "real server whitelist operation is not connected"
    ],
    "generatedAt": "2026-06-02T08:00:00Z"
  },
  "requestId": "req_example"
}
```

业务规则：该接口只读取 `admission-core` 内部装配状态和最近测试摘要，不主动执行四个模块的业务写操作，不调用旧服务进行实时健康探测，不把未完成模块伪装成 `READY`。第二批旧四服务清理后，`gatewaySwitchReady` 的判定只依赖四个模块在 `admission-core` 中的继承契约测试、`business-core-service` 基线和 `backend` 基线。只有当统一后端自承载网关契约、测试文档、自动化红灯、实现和相关后端回归均完成后，`gatewaySwitchStatus` 才能为 `COMPLETED`。

失败规则：运行单元内部异常返回 `53130`。模块装配信息缺失返回 `53131`。当前登记路由与本文档或四个模块契约期望不一致时返回 `53132` 或在 `status=DEGRADED` 的成功摘要中列入 `gaps`，由实现按是否影响接口可用性决定。认证上下文解析失败返回原模块契约或公共认证错误，可信网关上下文字段缺失或格式不兼容时返回 `53133`。

分页规则：无分页。

幂等规则：只读接口，重复调用不改变任何业务状态。

状态流转：该接口不改变业务状态。模块装配状态只允许按迁移过程从 `NOT_MOUNTED` 进入 `MOUNTED`，契约测试通过后进入 `READY`，发现路由缺失、adapter 不可用、交接链破坏或继承测试失败时进入 `DEGRADED`。

审计要求：读取后台装配摘要属于低风险后台读取，应保留请求编号、操作者、角色和访问时间的运行日志。不得记录 token 原文。

## 认证上下文

四个业务模块继续兼容 `Authorization: Bearer <token>`。需要登录或后台权限的接口仍按各模块契约解析当前用户、角色、权限、Minecraft 绑定和用户状态。

经 `api-gateway` 访问时，`admission-core` 继续兼容以下可信身份头。

| 请求头 | 说明 |
| --- | --- |
| `X-Beiming-Actor-User-Id` | 当前用户 ID。 |
| `X-Beiming-Actor-Roles` | 逗号分隔角色。 |
| `X-Beiming-Actor-Permissions` | 逗号分隔能力点。 |
| `X-Beiming-Actor-Minecraft-Id` | 已绑定的 Minecraft ID。 |
| `X-Beiming-Actor-Minecraft-Uuid` | 已绑定的 Minecraft UUID。 |
| `X-Gateway-Internal-Request-Id` | 网关注入的内部请求编号。 |

`X-Gateway-Internal-Request-Id` 存在时，各模块按自身契约优先解析可信认证上下文。字段缺失、格式非法、角色或能力点不兼容时，不得静默降级成匿名用户。生产入口必须由 `api-gateway` 或反向代理剥离客户端伪造的同名可信头；直连本地测试必须覆盖伪造头不能绕过权限的场景。

`X-Gateway-Internal-Request-Id` 缺失时，各模块必须忽略所有 `X-Beiming-Actor-*` 头并继续走 Bearer 兼容路径。四个模块的业务接口进入自身处理后，可信上下文解析失败必须返回对应模块契约错误码：`onboarding` 返回 `46802`，`exam` 返回 `46902`，`whitelist` 返回 `47002`，`attendance` 返回 `48002`。`53133` 只用于 `admission-core` 自有接口或装配层在请求进入业务模块前的解析失败。

## 测试控制头

四个业务模块继承各自契约中的本地测试控制头，但生产和默认运行环境必须关闭。默认关闭时，`X-Test-*` 头必须被业务模块忽略，不得触发依赖失败、审计失败、存储失败、通知失败、流水失败或 profile stale。只有对应模块显式启用 `onboarding.test-controls.enabled=true`、`exam.test-controls.enabled=true`、`whitelist.test-controls.enabled=true` 或 `attendance.test-controls.enabled=true` 时，本地自动化测试才可以使用这些头。

`admission-core` 自有健康检查和后台装配摘要不接受业务测试控制头作为真实运行状态输入。测试控制头隔离属于第二批合并后的生产化硬化验收项，必须有默认关闭场景的自动化测试覆盖。

## 内部适配规则

`onboarding` 仍拥有入服流程实例、步骤状态、资料确认、规则确认、方向选择、阻塞状态和 exam 交接快照主数据。它只能输出 `OnboardingExamHandoffSnapshot`，不能判分，不能创建白名单申请，不能初始化考勤积分。

`exam` 仍拥有考试流程、题库、题目版本、试卷模板、答题记录、判分、阅卷、考试结果和 whitelist 交接快照主数据。它只能消费 onboarding 的只读交接快照，不能直接修改 onboarding 状态，不能创建白名单申请。

`whitelist` 仍拥有白名单申请、材料、审核、移除、profile 激活摘要和 attendance 交接快照主数据。它只能消费 exam 的通过结果快照，不能反写 exam 结果，不能创建积分流水，不能执行真实服务器白名单命令。

`attendance` 仍拥有考勤账户、积分余额、积分流水、贡献、月度扣分、榜单和移除候选主数据。它只能消费 whitelist 的 attendance handoff 初始化账户，不能直接修改 whitelist 申请状态，不能调用真实白名单移除或服务器命令。

同 JVM 内部调用可以从 HTTP client 改为 adapter 或 facade，但 adapter 必须保留失败模拟能力，测试必须能覆盖 auth 不可用、profile 不可用、content 不可用、notification 投递失败、onboarding handoff 不可用、exam handoff 不可用、whitelist handoff 不可用、审计失败、状态写入失败和流水写入失败。

`admission-core` 适配第一批模块时，应优先通过 `business-core` 已稳定的正式 API、认证上下文或清晰 adapter 读取，不得直接导入第一批模块的内存 store、Repository、实体或测试种子绕开边界。确需给第一批模块新增能力时，必须按前序模块兼容变更流程先更新对应正式契约、本地测试文档、自动化测试和实现。

## 错误码

四个业务模块接口继续使用各自契约中的错误码，不因进入 `admission-core` 改码。公共错误码继续继承 `docs/contracts-common.md`。

`admission-core` 自有错误码如下。

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `53130` | 500 | admission-core 内部错误。 |
| `53131` | 500 | admission-core 模块装配错误。 |
| `53132` | 500 | admission-core 路由快照与契约不一致。 |
| `53133` | 502 | admission-core 可信认证上下文解析失败。 |
| `53134` | 500 | admission-core 交接链装配错误。 |

以上错误码只用于 `/api/v1/admission-core/**` 自有接口，或用于运行单元装配层在请求到达业务模块前发生的错误。已经进入四个业务模块处理流程的请求，错误码必须按对应模块契约返回。

## 网关策略

当前网关已完成第二批路径切换。第四十七轮后，本地开发态 `api-gateway-service` 独立 Maven 入口已退役；`backend:8135` 自承载网关能力，并保持 `onboarding`、`exam`、`whitelist` 和 `attendance` 到 `admission-core-service:8131` 边界的兼容。旧端口 `8108` 到 `8111` 只作为历史原服务端口记录，不再作为网关上游和测试基线。

| 路由 ID | 路径前缀 | 旧端口 | 目标端口 |
| --- | --- | --- | --- |
| `onboarding` | `/api/v1/onboarding` | `8108` | `8131` |
| `exam` | `/api/v1/exams` | `8109` | `8131` |
| `whitelist` | `/api/v1/whitelist` | `8110` | `8131` |
| `attendance` | `/api/v1/attendance` | `8111` | `8131` |

业务路径仍保持 `/api/v1/onboarding/**`、`/api/v1/exams/**`、`/api/v1/whitelist/**` 和 `/api/v1/attendance/**`，不得改成 `/api/v1/admission-core/<module>/**`。

## 迁移顺序

迁移顺序固定为基线验证、`admission-core` 空壳与自检红灯、`onboarding`、`exam`、`whitelist`、`attendance`、网关切换准备、网关切换实现、全量回归。

`onboarding` 必须先迁入，因为它是入服链路起点，也是 exam 创建考试的前置快照来源。`exam` 必须在 onboarding 之后迁入，因为它只能通过 onboarding handoff 创建考试。`whitelist` 必须在 exam 之后迁入，因为它只能消费 exam 通过结果。`attendance` 必须最后迁入，因为它只能消费 whitelist attendance handoff 初始化账户。

每迁入一个模块，都必须先根据本文档和该模块正式契约生成或补齐 `admission-core` 自动化测试，确认测试因为模块未装配或行为不满足而失败，再迁入实现。第二批旧四服务清理后，迁入和后续回归只运行当前保留的后端运行单元，不恢复旧四服务测试入口。

## 验收口径

`admission-core` 完成的最低标准是，单进程承载第二批四个业务模块的全部既有 API 路径，且响应格式、错误码、认证、权限、请求编号、分页、状态流转、幂等、审计和降级行为与四个模块正式契约一致。

当前统一后端回归命令 `mvn -q -f backend/pom.xml test` 必须覆盖本文档两个自有接口和四个模块继承过来的全部契约测试。第二批旧服务清理后，相关回归基线由统一后端装配的 `admission-core` 和前序 `business-core` 模块承担，不得为了测试恢复 `onboarding-service`、`exam-service`、`whitelist-service`、`attendance-service` 或已退役的 `api-gateway-service` Maven 入口。

`admission-core` 直连合并和第二批网关切换均已完成测试闭环。第二批业务路径经网关访问时仍保持原路径，网关只切换上游端口，不改写业务前缀。

允许 `backend:8135` 以不改变路径、认证、响应格式和错误码的方式挂载 `admission-core` 自有 API 以及 `onboarding`、`exam`、`whitelist`、`attendance` 四个业务模块。该候选挂载不能被描述为已退役 `admission-core-service:8131`，也不能绕过四个模块自己的正式契约、测试文档和自动化测试闭环。

用户确认后，第二批旧服务源码和 Maven 运行入口按明确文件路径逐个清理。旧服务目录不得因本契约自动批量删除；后续如需继续清理残留空目录或其他文件，必须单独确认范围，删除文件只能逐个明确路径处理。

第二批验收还必须满足交接链不被破坏：onboarding 只输出 exam handoff，exam 只消费 onboarding handoff 并输出 whitelist handoff，whitelist 只消费 exam handoff 并输出 attendance handoff，attendance 只消费 whitelist handoff。任何反向写入、直接读库、万能流程表或真实服务器命令都不合格。
