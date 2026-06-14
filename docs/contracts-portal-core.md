# 北冥官网 portal-core API 契约

版本：0.2

## 文档定位

本文档是 `portal-core-service` 运行合并单元的正式 API 契约。`portal-core` 只负责承载第五期后续玩家门户体验合并后的运行入口、自检摘要、模块装配摘要、生产就绪诊断、服务发现快照、跨进程 HTTP smoke、继承路由漂移防线、测试控制头总开关和网关切换验收口径。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。被承载业务模块的业务接口仍分别以 `docs/contracts-guide.md`、`docs/contracts-material.md` 和 `docs/contracts-online-map.md` 为准。本文档不得混写三个模块的业务 API 字段、状态机或错误码。

## 职责边界

`portal-core` 承载 `guide`、`material` 和 `online-map` 三个玩家门户体验模块。合并后三个模块继续使用原路径前缀，分别是 `/api/v1/guides`、`/api/v1/materials` 和 `/api/v1/online-map`。

`portal-core` 不新增三个业务模块的业务语义，不把指南文章、外部交流入口、素材投稿、上传会话、文件安全摘要、地图 provider、世界、图层、marker、区域、嵌入配置、状态机、错误码、审计对象或主数据揉成一个大模块，不直接读取前序模块数据库，不绕过正式 API 适配前序模块，也不执行真实对象存储、Cloudreve、服务器文件、节点、地图渲染、地图瓦片代理、真实世界目录读取或外部通知发送能力。

`api-gateway-service` 仍保持独立。`external-node-executor-service` 和 `ops-core-service` 仍保持独立。`cross-platform-notification` 已由 `ops-core-service:8133` 承载，不再保留独立 Maven 运行入口，也不并入 `portal-core`。旧 `backend/online-map-service` 和旧 `backend/cross-platform-notification-service` Maven 入口已退役且不得恢复，不作为当前网关上游。`portal-core` 可以展示真实能力未接入的缺口，也可以通过显式 smoke 入口验证网关到 `guide`、`material` 和 `online-map` 原路径的真实 HTTP 可达性，但不能把内存、stub、fake、静态服务发现或单次 smoke 成功伪造成完整生产完成。

第九轮允许 `portal-core` 被 `backend:8135` 以 in-process 方式挂载。该挂载只能复用当前 `portal-core` 自有控制器和 `guide`、`material`、`online-map` 三个业务控制器，不改变独立入口 `portal-core-service:8134` 的端口、路径、认证、响应格式、错误码、测试入口或生产缺口声明。统一后端入口通过 `/api/v1/portal-core/**`、`/api/v1/guides/**`、`/api/v1/materials/**` 和 `/api/v1/online-map/**` 访问时，行为必须与当前入口兼容。

## 基础路径、端口和认证

`portal-core-service` 本地端口固定为 `8134`。

`portal-core` 自有接口使用 `/api/v1/portal-core` 前缀。健康检查公开可访问。后台自检、模块装配和生产就绪诊断接口要求 `Authorization: Bearer <token>`，本地契约实现允许 `owner-token` 和 `admin-token` 访问，`helper-token` 与 `user-token` 返回 `42001`，缺失 token 返回 `41000`，格式错误返回 `41003`。

通过网关注入的可信身份头也可访问后台自有接口。可信上下文必须同时包含合法 `X-Gateway-Internal-Request-Id`、`X-Beiming-Actor-User-Id` 和角色头。角色必须为 `ADMIN` 或 `OWNER`。缺少必要字段、请求编号非法、角色枚举非法或能力点枚举非法时返回 `53233`。

浏览器直连时传入的可信身份头不得覆盖真实身份。只有存在 `X-Gateway-Internal-Request-Id` 且整组可信字段通过校验时，才按可信上下文授权；否则按 `Authorization` 本地 token 授权。

## 测试控制头总开关

`portal-core` 统一持有测试控制头总开关。默认环境和生产环境下，所有 `X-Test-*` 头都必须被忽略。只有显式配置 `portal-core.test-controls.enabled=true` 时，继承模块才允许读取本地自动化测试所需的 `X-Test-*` 头。

自有摘要必须返回 `testControlsEnabled`。默认值为 `false`。生产 readiness 必须明确暴露测试控制头总开关状态和默认关闭口径。

## 服务发现和 HTTP smoke

本轮生产化增强参考 Kubernetes readiness/liveness probes、Spring Boot Actuator readiness/liveness probes 和 Consul service health checks 的边界做法：存活检查只证明进程活着，生产 readiness 必须单独暴露外部依赖状态，HTTP smoke 必须有可追溯结果，单次失败不能被伪造成业务成功。参考来源为 Kubernetes 官方探针文档、Spring Boot Actuator Kubernetes probes 官方文档和 Consul service checks 官方文档。

`portal-core` 第一版服务发现模式为 `STATIC_LOCAL_REGISTRY`。该注册表只保存本地固定上游和 smoke 目标，不接入 Eureka、Consul、Kubernetes API、Nacos、DNS SRV 或集中配置中心。注册表至少包含 `API_GATEWAY`、`PORTAL_CORE`、`GUIDE`、`MATERIAL` 和 `ONLINE_MAP` 五个条目，并暴露 `serviceKey`、`serviceName`、`baseUrl`、`port`、`pathPrefix`、`healthPath`、`discoverySource`、`enabled` 和 `lastObservedStatus`。

跨进程 HTTP smoke 只通过显式后台接口触发，默认不由 `GET /api/v1/portal-core/health` 或 `GET /api/v1/portal-core/admin/readiness` 自动触发，避免一次读取拖慢或污染生产自检。默认目标为通过网关访问 `/api/v1/guides/categories`、`/api/v1/materials/featured` 和 `/api/v1/online-map/health`，基础地址默认 `http://127.0.0.1:8125`。本地自动化测试可以通过配置覆盖 smoke 目标到临时 HTTP 服务，以验证真实 TCP/HTTP 调用链，不依赖真实网关进程常驻。

真实网关联调验收必须启动真实 `portal-core-service` HTTP 服务和真实 `api-gateway-service` HTTP 服务，并把 `portal-core.http-smoke.gateway-base-url` 指向本次启动的网关地址。网关在本地联调中允许通过 `api-gateway.upstreams.portal-core-base-url` 把 `guide`、`material` 和 `online-map` 三个原业务路由临时指向本次启动的 `portal-core` 地址，从而避免固定端口占用导致测试不稳定。联调必须通过 `POST /api/v1/portal-core/admin/http-smoke/run` 触发真实 HTTP 调用，确认请求路径仍为 `/api/v1/guides/categories`、`/api/v1/materials/featured` 和 `/api/v1/online-map/health`，请求编号透传，三个目标均返回统一成功响应后 `httpSmokeStatus=PASS`。该联调只证明网关到 `portal-core` 的原路径转发和 smoke 入口可用，不能替代真实持久化、审计持久化、对象存储、文件安全扫描、全文搜索、地图 provider HTTP、marker 同步、外部通知投递、动态服务发现或集中配置。

smoke 状态允许 `NOT_RUN`、`PASS`、`DEGRADED` 和 `DISABLED`。未执行时为 `NOT_RUN`，全部目标返回 HTTP 小于 `500` 且统一响应体 `code=0` 时为 `PASS`，任一目标连接失败、超时、返回 HTTP `5xx`、非 JSON 或业务 `code` 非 `0` 时为 `DEGRADED`。`portal-core` 不因为 smoke 失败返回 5xx，失败结果进入响应体和 readiness 诊断。结果只保存在当前进程内存中，不代表审计持久化已经完成。

## 运行治理画像

本轮继续优化参考 Google SRE 的 SLI/SLO 管理口径和 Uber DOMA 的领域运行边界思想。`portal-core` 不再只给出零散检查项，还必须在 summary 和 readiness 中暴露统一的运行治理画像 `operationalProfile`。该画像用于把存活、就绪、发布门禁、SLO 目标、流量资格和领域边界放在同一个可测试结构里，方便后续接入部署系统、发布检查和告警系统。

`operationalProfile.profileVersion` 固定为 `portal-core-operational-profile-v1`。`domainBoundary` 固定为 `PORTAL_EXPERIENCE_CORE`。`referenceModel` 必须包含 `KUBERNETES_PROBES`、`SPRING_BOOT_AVAILABILITY`、`GOOGLE_SRE_SLO` 和 `UBER_DOMA`。`livenessStatus` 在进程可响应时为 `LIVE`。`readinessGateStatus` 和 `releaseGateStatus` 在真实生产缺口仍存在时必须为 `NOT_READY`，不能因为 HTTP smoke 一次通过而变成 `PASS`。`trafficEligibility` 在真实持久化、审计持久化、动态发现和外部依赖未接入前必须为 `INTERNAL_AND_TEST_ONLY`。

`operationalProfile.probeRecommendations` 必须明确 `livenessPath=/api/v1/portal-core/health`、`readinessPath=/api/v1/portal-core/admin/readiness`、`startupPath=/api/v1/portal-core/health`，并声明 `externalDependenciesInLiveness=false`。`operationalProfile.sloTargets` 必须至少包含路由漂移为 0、测试控制头默认关闭、HTTP smoke 目标全部成功和生产缺口为 0 四类目标。`operationalProfile.releaseGates` 必须至少包含继承路由漂移、网关路由切换、测试控制头默认关闭、HTTP smoke、真实持久化、真实审计持久化、真实外部依赖和动态服务发现。未完成的生产能力必须以 `BLOCKED`、`PARTIAL`、`NOT_RUN` 或 `DEGRADED` 体现，不能隐藏。

## 模块装配表

| 模块 | 历史来源目录 | 历史端口 | 当前模块源码归属 | 当前后端入口 | API 数 | 正式契约 | 历史测试入口状态 | 当前测试入口 |
| --- | --- | ---: | --- | ---: | ---: | --- | --- | --- |
| `guide` | `backend/guide-service` | 8127 | `backend/src/main/java` | `backend:8135` | 41 | `docs/contracts-guide.md` | 已退役，不得恢复旧 Maven 入口 | `mvn -q -f backend/pom.xml test` |
| `material` | `backend/material-service` | 8126 | `backend/src/main/java` | `backend:8135` | 33 | `docs/contracts-material.md` | 已退役，不得恢复旧 Maven 入口 | `mvn -q -f backend/pom.xml test` |
| `online-map` | `backend/online-map-service` | 8121 | `backend/src/main/java` | `backend:8135` | 34 | `docs/contracts-online-map.md` | 已退役，不得恢复旧 Maven 入口 | `mvn -q -f backend/pom.xml test` |

三个继承模块合计 108 个业务 API 路由。`portal-core` 自有接口为 5 个。当前统一后端 `backend:8135` 应注册 113 个 `/api/v1/**` 方法路由。

## 生产就绪能力状态

生产就绪诊断必须逐项暴露以下能力状态。

| 能力 | 第一版状态 | 说明 |
| --- | --- | --- |
| 真实数据库持久化 | `BLOCKED` | 三个继承模块仍以本地内存或契约 stub 为主。 |
| 真实跨服务 HTTP adapter | `BLOCKED` | auth、profile、notification、resource 和 server-status 依赖仍以本地安全快照或测试适配器表示。 |
| 真实审计持久化 | `BLOCKED` | 审计闭环已覆盖回滚和脱敏，但未接持久化审计库。 |
| 真实对象存储 | `BLOCKED` | `material` 上传第一版仍为 `LOCAL_STUB`，不保存真实对象存储密钥。 |
| 真实文件安全扫描 | `BLOCKED` | 素材文件安全状态只保存安全摘要和测试模拟，不运行真实 scanner。 |
| 真实全文搜索 | `BLOCKED` | `guide` 搜索第一版仍为本地摘要搜索，不接外部搜索服务。 |
| 真实外部通知投递 | `BLOCKED` | `guide` 和 `material` 只保留通知适配摘要，不执行真实外部发送。 |
| 真实地图 provider HTTP | `BLOCKED` | `online-map` 只保存安全快照，不执行真实 provider 探测。 |
| 真实 marker 同步 | `BLOCKED` | `online-map` 只维护本地契约数据，不同步真实地图插件 marker。 |
| 真实瓦片托管 | `BLOCKED` | `online-map` 不代理真实瓦片，也不读取真实世界目录。 |
| 服务发现快照 | `PARTIAL` | 已提供静态本地注册表，不接入动态服务发现或集中配置。 |
| 真实 HTTP smoke | `NOT_RUN` | 已提供显式 smoke 入口；未执行时为 `NOT_RUN`，执行失败为 `DEGRADED`，执行成功为 `PASS`。 |
| 测试控制头默认关闭 | `PASS` | 默认和生产环境必须忽略所有 `X-Test-*` 头。 |
| 继承路由漂移防线 | `PASS` | 自动比对三个继承模块契约表和当前 Controller 路由。 |
| 敏感字段扫描 | `PASS` | 自动扫描请求、响应和生产源码中的 token、secret、真实路径、上传票据长期副本、真实命令和禁止删除命令。 |
| 网关路由切换 | `PASS` | 网关三个原路径前缀仍保持不改写，并统一指向 `8134`。 |

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/portal-core/health` | 否 | 无 | LOW |
| 运行摘要 | GET | `/api/v1/portal-core/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 模块装配摘要 | GET | `/api/v1/portal-core/admin/modules` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 生产就绪诊断 | GET | `/api/v1/portal-core/admin/readiness` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 执行 HTTP smoke | POST | `/api/v1/portal-core/admin/http-smoke/run` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 健康检查

`GET /api/v1/portal-core/health`

成功响应 HTTP `200`。

响应 `data` 必须包含 `service=portal-core`、`status=UP`、`version`、`port=8134`、`modulesTotal=3`、`inheritedRoutesTotal=108`、`selfRoutesTotal=5`、`routesTotal=113`、`livenessStatus=LIVE`、`readinessProbePath=/api/v1/portal-core/admin/readiness` 和 `startupProbePath=/api/v1/portal-core/health`。该接口不得返回上传票据、内部路径、后台备注、审核意见、通知摘要、外部 URL 管理凭据、地图 provider 内部地址、异常栈或依赖错误细节。

## 运行摘要

`GET /api/v1/portal-core/ops/summary`

成功响应 HTTP `200`。

响应 `data` 必须包含 `service=portal-core`、`port=8134`、`modulesTotal=3`、`modulesMounted=3`、`inheritedRoutesTotal=108`、`selfRoutesTotal=5`、`routesTotal=113`、`testControlsEnabled`、`storageMode`、`authMode`、`dependencyAdapterMode`、`serviceDiscoveryMode`、`registeredUpstreams`、`httpSmokeStatus`、`httpSmokeTargets`、`lastHttpSmokeAt`、`lastHttpSmokeResults`、`operationalProfile`、`routeDriftStatus`、`gatewaySwitchStatus`、`moduleRoutes`、`productionGaps`、`recentAuditSummary` 和 `generatedAt`。

`storageMode` 第一版固定说明为 `IN_MEMORY_CONTRACT_STUBS`。`dependencyAdapterMode` 第一版固定说明为 `SAFE_SNAPSHOT_AND_TEST_ADAPTERS`。`routeDriftStatus` 必须为 `NO_DRIFT` 才能进入完成验收。`gatewaySwitchStatus` 必须为 `COMPLETED` 才能进入完成验收。

## 模块装配摘要

`GET /api/v1/portal-core/admin/modules`

成功响应 HTTP `200`，`data.items` 为模块装配数组。每个元素必须包含 `moduleKey`、`moduleName`、`pathPrefix`、`legacyServiceDirectory`、`legacyPort`、`currentServiceDirectory`、`currentPort`、`contract`、`localTestDocument`、`legacyTestCommand`、`currentTestCommand`、`routesTotal`、`contractRoutesTotal`、`routeDriftStatus`、`enabled`、`mounted`、`businessContractOwnedByModule`、`compatibilityMode` 和 `productionGaps`。

装配摘要中的三个业务模块路径必须保持原样，不得出现 `/api/v1/portal-core/guides`、`/api/v1/portal-core/materials` 或 `/api/v1/portal-core/online-map`。

## 生产就绪诊断

`GET /api/v1/portal-core/admin/readiness`

成功响应 HTTP `200`。

响应 `data` 必须包含 `service=portal-core`、`port=8134`、`readyForProduction=false`、`readinessStatus=NOT_READY`、`routesTotal=113`、`inheritedRoutesTotal=108`、`selfRoutesTotal=5`、`routeDriftStatus`、`legacyServiceRestoreStatus`、`gatewaySwitchStatus`、`testControlHeadersStatus`、`sensitiveFieldScanStatus`、`serviceDiscoveryMode`、`registeredUpstreams`、`httpSmokeStatus`、`httpSmokeTargets`、`lastHttpSmokeAt`、`lastHttpSmokeResults`、`operationalProfile`、`checks`、`moduleReadiness`、`productionBlockers` 和 `generatedAt`。

`checks` 必须至少包含真实持久化、真实跨服务 HTTP、真实审计持久化、真实对象存储、真实文件安全扫描、真实全文搜索、真实外部通知投递、真实地图 provider HTTP、真实 marker 同步、真实瓦片托管、服务发现快照、真实 HTTP smoke、测试控制头默认关闭、继承路由漂移防线、敏感字段扫描和网关路由切换。未完成真实生产能力必须以 `BLOCKED`、`PARTIAL`、`NOT_RUN` 或 `NOT_CONNECTED` 暴露，不能返回 `PASS`。真实 HTTP smoke 只有在本进程内最近一次显式执行全部目标成功后才允许返回 `PASS`。

## 执行 HTTP smoke

`POST /api/v1/portal-core/admin/http-smoke/run`

请求体为空。成功响应 HTTP `200`，无论目标成功或失败，接口自身都返回统一成功响应；目标失败通过 `data.httpSmokeStatus=DEGRADED`、`data.results[*].status`、`data.results[*].failureReason` 和 readiness 检查项暴露。接口自身只有在认证、权限、配置非法或内部状态不可用时返回错误。

响应 `data` 必须包含 `service=portal-core`、`serviceDiscoveryMode`、`registeredUpstreams`、`httpSmokeStatus`、`startedAt`、`finishedAt`、`targets` 和 `results`。每个 target 必须包含 `targetKey`、`serviceKey`、`method`、`url`、`expectedStatusMax`、`expectedBusinessCode` 和 `timeoutMs`。每个 result 必须包含 `targetKey`、`serviceKey`、`method`、`url`、`status`、`httpStatus`、`businessCode`、`durationMs`、`checkedAt` 和 `failureReason`。响应不得返回完整请求头、认证 token、Cookie、异常栈、内部绝对路径或外部凭据。

服务发现注册表为空、目标 URL 非 HTTP(S)、目标数量为 0 或 timeout 非法时返回 `50000`，不得执行半套 smoke。连接失败、超时、HTTP `5xx`、业务码非 `0` 和响应体不可解析时只标记目标 `FAILED`，不伪造成功。

## 路由漂移、脱敏和禁止能力

`portal-core` 必须有自动化测试比对三个继承模块正式契约表中的 `METHOD path` 与当前 Spring Controller 注册路由。任何新增、缺失、方法漂移或路径前缀改写都必须导致测试失败。

`portal-core` 必须有敏感字段扫描测试，覆盖请求体、响应体和生产源码。以下内容不得出现在浏览器响应、审计摘要或生产源码的可执行能力中：token、secret、credential、Authorization、uploadTicket 长期副本、internalPath、objectStorageKey、storageSecret、sharePassword、Cloudreve token、webhook secret、SMTP 密码、bot token、真实宿主路径、真实世界目录、`ProcessBuilder`、`Runtime.getRuntime`、真实 Docker client、containerd 直连、kubectl、helm、真实对象存储删除、真实 Cloudreve 删除、真实文件删除、真实节点命令、`del /s`、`rd /s`、`rmdir /s`、`Remove-Item -Recurse` 和 `rm -rf`。

测试中为了断言禁止字符串可以出现字符串字面量，但不得形成真实执行能力。生产源码不得出现真实执行类或命令调用。

## 网关切换口径

`api-gateway-service` 必须保留原路由 ID、服务键、路径前缀、请求路径、认证透传、可信身份头剥离与注入、请求编号和响应透传规则。只允许把以下三个路由的上游端口切到 `8134`。

| 路由 ID | 服务键 | 路径前缀 | 当前上游端口 | 历史端口 |
| --- | --- | --- | ---: | ---: |
| `material` | `MATERIAL` | `/api/v1/materials` | 8134 | 8126 |
| `guide` | `GUIDE` | `/api/v1/guides` | 8134 | 8127 |
| `online-map` | `ONLINE_MAP` | `/api/v1/online-map` | 8134 | 8121 |

## 验收口径

`portal-core` API 文档按 `docs/contracts-portal-core.md` 独立存在，并由 `.local-docs/tests-portal-core.md` 记录本地测试闭环。

完成时必须满足以下条件：`portal-core-service:8134` 单进程承载三个玩家门户体验模块的全部既有 API 路径；三个模块原契约仍有效；`portal-core` 自有五个接口全覆盖；服务发现静态注册表、HTTP smoke 结果字段和运行治理画像全覆盖；真实 `api-gateway-service` 到真实 `portal-core-service` 的本地 HTTP 联调用例通过；`.local-docs/tests-portal-core.md` 中的完备用例都有自动化验证；自动化测试先红灯；实现后 `mvn -q -f backend/pom.xml test` 通过；旧 `guide-service`、`material-service` 和 `online-map-service` Maven 入口已退役且不得恢复；`api-gateway-service` 已按契约切换并通过测试；`business-core-service`、`admission-core-service`、`engagement-core-service` 和 `ops-core-service` 回归通过；前三期旧服务目录没有恢复；`cross-platform-notification`、`external-node-executor` 和 `api-gateway` 仍保持独立；允许 `backend:8135` 以不改变路径和响应格式的方式挂载 `portal-core`；生产 readiness 明确暴露剩余生产缺口，运行治理画像明确声明当前只具备内部和测试流量资格，且不得把静态服务发现、可配置本地上游、运行画像、统一后端挂载或单次 smoke 成功当作真实持久化、审计持久化、对象存储、文件扫描、全文搜索、地图 provider HTTP、marker 同步、瓦片托管、外部通知投递、动态服务发现或集中配置完成；测试过程完整写入 `.local-docs/tests-portal-core.md`、`.local-docs/tests-online-map.md`、`.local-docs/tests-api-gateway.md` 和 `.local-docs/tests-unified-backend.md`。
