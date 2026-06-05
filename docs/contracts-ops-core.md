# 北冥官网 ops-core API 契约

版本：0.3

## 文档定位

本文档是 `ops-core-service` 运行合并单元的正式 API 契约。`ops-core` 负责承载第四期后台运维控制面和第六期跨平台通知控制面合并后的运行入口、自检摘要、模块装配摘要、生产就绪诊断、继承路由漂移防线、测试控制头总开关和网关切换验收口径。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、运维能力点、审计字段、风险等级和通用错误码均以公共契约为准。七个被承载业务模块的业务接口仍分别以 `docs/contracts-ops-control.md`、`docs/contracts-cloudreve-sync.md`、`docs/contracts-backup-recovery.md`、`docs/contracts-alerting.md`、`docs/contracts-plugin-integration.md`、`docs/contracts-ops-image-market.md` 和 `docs/contracts-cross-platform-notification.md` 为准。本文档不得混写七个模块的业务 API 字段、状态机或错误码。

## 职责边界

`ops-core` 承载 `ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration`、`ops-image-market` 和 `cross-platform-notification` 七个后台运维与通知控制面模块。合并后这些模块继续使用原路径前缀，分别是 `/api/v1/ops-control`、`/api/v1/cloudreve-sync`、`/api/v1/backup-recovery`、`/api/v1/alerting`、`/api/v1/plugin-integration`、`/api/v1/ops-image-market` 和 `/api/v1/cross-platform-notification`。

`ops-core` 不新增七个业务模块的业务语义，不把七套 store、状态机、错误码、审计对象或主数据揉成一个大模块，不直接读取前序服务数据库，不绕过正式 API 适配前序服务，也不执行真实宿主机、容器、节点、Cloudreve、registry、scanner、插件命令或真实外部消息发送。

`api-gateway-service` 仍保持独立。`node-daemon-service` 仍保持独立。第六期完成后 `cross-platform-notification` 只保留独立模块契约身份，不再保留独立 Maven 运行入口。`ops-core` 可以展示真实能力未接入的缺口，但不能把模拟、stub、fake 或 blocked 状态伪造成生产完成。

## 基础路径、端口和认证

`ops-core-service` 本地端口固定为 `8133`。

`ops-core` 自有接口使用 `/api/v1/ops-core` 前缀。健康检查公开可访问。后台自检、模块装配、生产就绪诊断和 HTTP smoke 接口要求 `Authorization: Bearer <token>`，本地契约实现允许 `owner-token` 和 `admin-token` 访问，`helper-token` 与 `user-token` 返回 `42001`，缺失 token 返回 `41000`，格式错误返回 `41003`。

通过网关注入的可信身份头也可访问后台自有接口。可信上下文必须同时包含合法 `X-Gateway-Internal-Request-Id`、`X-Gateway-Internal-Timestamp`、`X-Gateway-Internal-Signature`、`X-Beiming-Actor-User-Id` 和角色头。角色必须为 `ADMIN` 或 `OWNER`。缺少必要字段、请求编号非法、角色枚举非法、能力点枚举非法、签名错误、签名时间戳过期或签名明文不匹配时返回 `53233`。

浏览器直连时传入的可信身份头不得覆盖真实身份。只有存在 `X-Gateway-Internal-Request-Id` 且整组可信字段和内部签名通过校验时，才按可信上下文授权；否则按 `Authorization` 本地 token 授权。直连请求一旦携带 `X-Gateway-Internal-Request-Id`，不得在签名失败后回退到本地 token。

## 测试控制头总开关

`ops-core` 统一持有测试控制头总开关。默认环境和生产环境下，所有 `X-Test-*` 头都必须被忽略。只有显式配置 `ops-core.test-controls.enabled=true` 时，继承模块才允许读取本地自动化测试所需的 `X-Test-*` 头。

自有摘要必须返回 `testControlsEnabled`。默认值为 `false`。生产 readiness 必须明确暴露测试控制头总开关状态和默认关闭口径。

## 模块装配表

| 模块 | 原服务目录 | 原端口 | 当前服务目录 | 当前端口 | API 数 | 正式契约 | 旧测试入口状态 | 当前测试入口 |
| --- | --- | ---: | --- | ---: | ---: | --- | --- | --- |
| `ops-control` | `backend/ops-control-service` | 8116 | `backend/ops-core-service` | 8133 | 31 | `docs/contracts-ops-control.md` | 已退役，`legacyTestCommand=null` | `mvn -q -f backend/ops-core-service/pom.xml test` |
| `cloudreve-sync` | `backend/cloudreve-sync-service` | 8118 | `backend/ops-core-service` | 8133 | 16 | `docs/contracts-cloudreve-sync.md` | 已退役，`legacyTestCommand=null` | `mvn -q -f backend/ops-core-service/pom.xml test` |
| `backup-recovery` | `backend/backup-recovery-service` | 8119 | `backend/ops-core-service` | 8133 | 25 | `docs/contracts-backup-recovery.md` | 已退役，`legacyTestCommand=null` | `mvn -q -f backend/ops-core-service/pom.xml test` |
| `alerting` | `backend/alerting-service` | 8120 | `backend/ops-core-service` | 8133 | 24 | `docs/contracts-alerting.md` | 已退役，`legacyTestCommand=null` | `mvn -q -f backend/ops-core-service/pom.xml test` |
| `plugin-integration` | `backend/plugin-integration-service` | 8122 | `backend/ops-core-service` | 8133 | 38 | `docs/contracts-plugin-integration.md` | 已退役，`legacyTestCommand=null` | `mvn -q -f backend/ops-core-service/pom.xml test` |
| `ops-image-market` | `backend/ops-image-market-service` | 8124 | `backend/ops-core-service` | 8133 | 49 | `docs/contracts-ops-image-market.md` | 已退役，`legacyTestCommand=null` | `mvn -q -f backend/ops-core-service/pom.xml test` |
| `cross-platform-notification` | `backend/cross-platform-notification-service` | 8123 | `backend/ops-core-service` | 8133 | 36 | `docs/contracts-cross-platform-notification.md` | 已退役，`legacyTestCommand=null` | `mvn -q -f backend/ops-core-service/pom.xml test` |

七个继承模块合计 219 个业务 API 路由。`ops-core` 自有接口为 5 个。`ops-core-service` 当前进程应注册 224 个 `/api/v1/**` 方法路由。

## 生产就绪能力状态

生产就绪诊断必须逐项暴露以下能力状态。

| 能力 | 第一版状态 | 说明 |
| --- | --- | --- |
| 真实数据库持久化 | `BLOCKED` | 七个继承模块仍以本地内存或契约 stub 为主。 |
| 真实跨服务 HTTP adapter | `BLOCKED` | 前序服务依赖仍以本地安全快照或测试适配器表示。 |
| 真实审计持久化 | `BLOCKED` | 审计闭环已覆盖回滚和脱敏，但未接持久化审计库。 |
| 真实节点执行 | `BLOCKED` | 控制面不得直连宿主机命令，真实执行仍由独立 `node-daemon` 后续闭环。 |
| 真实 Cloudreve API | `BLOCKED` | `cloudreve-sync` 只保存 provider、文件、分享和同步摘要，不保存真实 token。 |
| 真实 registry | `BLOCKED` | `ops-image-market` 不保存 registry 凭据，不执行真实镜像拉取。 |
| 真实 scanner | `BLOCKED` | 风险扫描只保存安全摘要，不运行真实 scanner。 |
| 真实插件事件入口 | `BLOCKED` | `plugin-integration` 不开放无鉴权公网 webhook，不保存 raw payload。 |
| 真实通知投递 | `BLOCKED` | `alerting` 仅保留站内通知或脱敏投递摘要，不执行真实外部发送。 |
| 真实外部消息发送 | `BLOCKED` | `cross-platform-notification` 只保留模拟投递，不发送真实邮件、短信、聊天消息、Webhook、移动推送或游戏内消息。 |
| 真实回调签名 | `BLOCKED` | 外部平台回调签名、webhook secret token 和状态回执仍未接入。 |
| 生产凭据托管 | `BLOCKED` | 外部渠道 token、SMTP 密码、短信 token、机器人 token 和推送凭据只允许脱敏引用。 |
| 异步队列 | `BLOCKED` | 跨平台投递仍为本地契约 stub，未接入异步队列、死信队列和重试窗口。 |
| 持久化事务 | `BLOCKED` | provider、模板映射、路由、投递、attempt、receiver、audit 和幂等记录未接入数据库事务。 |
| 测试控制头默认关闭 | `PASS` | 默认和生产环境必须忽略所有 `X-Test-*` 头。 |
| HTTP smoke | `NOT_RUN`、`PASS` 或 `DEGRADED` | 通过独立 `api-gateway-service` 访问当前 `ops-core-service` 的关键路径，未执行时为 `NOT_RUN`。 |
| 可信网关内部签名 | `PASS` 或 `PARTIAL` | `ops-core` 管理接口必须拒绝未签名或签名错误的可信身份头；本地 Bearer 兼容路径仍只用于契约测试。 |
| 继承路由漂移防线 | `PASS` | 自动比对七个继承模块契约表和当前 Controller 路由。 |
| 敏感字段扫描 | `PASS` | 自动扫描请求、响应和生产源码中的 token、secret、真实路径、真实命令和禁止删除命令。 |
| 网关路由切换 | `PASS` | 网关七个原路径前缀仍保持不改写，并统一指向 `8133`。 |

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/ops-core/health` | 否 | 无 | LOW |
| 运行摘要 | GET | `/api/v1/ops-core/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 模块装配摘要 | GET | `/api/v1/ops-core/admin/modules` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 生产就绪诊断 | GET | `/api/v1/ops-core/admin/readiness` | 是 | `ADMIN` 或 `OWNER` | LOW |
| HTTP smoke | POST | `/api/v1/ops-core/admin/http-smoke/run` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 健康检查

`GET /api/v1/ops-core/health`

成功响应 HTTP `200`。

响应 `data` 必须包含 `service=ops-core`、`status=UP`、`version`、`port=8133`、`modulesTotal=7`、`inheritedRoutesTotal=219`、`selfRoutesTotal=5` 和 `routesTotal=224`。该接口不得返回模块内部 provider 数量、节点 endpoint、外部 URL、token、凭据、异常栈、真实宿主路径或依赖错误细节。

## 运行摘要

`GET /api/v1/ops-core/ops/summary`

成功响应 HTTP `200`。

响应 `data` 必须包含 `service=ops-core`、`port=8133`、`modulesTotal=7`、`modulesMounted=7`、`inheritedRoutesTotal=219`、`selfRoutesTotal=5`、`routesTotal=224`、`testControlsEnabled`、`storageMode`、`authMode`、`dependencyAdapterMode`、`serviceDiscoveryMode`、`registeredUpstreams`、`httpSmokeStatus`、`lastHttpSmokeAt`、`lastHttpSmokeResults`、`trustedGatewaySignatureStatus`、`routeDriftStatus`、`gatewaySwitchStatus`、`moduleRoutes`、`productionGaps`、`recentAuditSummary` 和 `generatedAt`。

`storageMode` 第一版固定说明为 `IN_MEMORY_CONTRACT_STUBS`。`dependencyAdapterMode` 第一版固定说明为 `SAFE_SNAPSHOT_AND_TEST_ADAPTERS`。`routeDriftStatus` 必须为 `NO_DRIFT` 才能进入完成验收。`gatewaySwitchStatus` 必须为 `COMPLETED` 才能进入完成验收。

## 模块装配摘要

`GET /api/v1/ops-core/admin/modules`

成功响应 HTTP `200`，`data.items` 为模块装配数组。每个元素必须包含 `moduleKey`、`moduleName`、`pathPrefix`、`legacyServiceDirectory`、`legacyPort`、`legacyServiceRetired`、`currentServiceDirectory`、`currentPort`、`contract`、`localTestDocument`、`legacyTestCommand`、`currentTestCommand`、`routesTotal`、`contractRoutesTotal`、`routeDriftStatus`、`enabled`、`mounted`、`businessContractOwnedByModule`、`compatibilityMode` 和 `productionGaps`。

第四批和第六期旧服务 Maven 入口退役后，`legacyServiceRetired` 必须为 `true`，`legacyTestCommand` 必须为 `null`。历史目录字段只用于追溯，不表示当前仓库仍保留或允许恢复旧服务入口。

装配摘要中的七个业务模块路径必须保持原样，不得出现 `/api/v1/ops-core/ops-control`、`/api/v1/ops-core/cloudreve-sync`、`/api/v1/ops-core/backup-recovery`、`/api/v1/ops-core/alerting`、`/api/v1/ops-core/plugin-integration`、`/api/v1/ops-core/ops-image-market` 或 `/api/v1/ops-core/cross-platform-notification`。

## 生产就绪诊断

`GET /api/v1/ops-core/admin/readiness`

成功响应 HTTP `200`。

响应 `data` 必须包含 `service=ops-core`、`port=8133`、`readyForProduction=false`、`readinessStatus=NOT_READY`、`routesTotal=224`、`inheritedRoutesTotal=219`、`selfRoutesTotal=5`、`routeDriftStatus`、`legacyServiceRestoreStatus`、`gatewaySwitchStatus`、`testControlHeadersStatus`、`sensitiveFieldScanStatus`、`serviceDiscoveryMode`、`registeredUpstreams`、`httpSmokeStatus`、`lastHttpSmokeAt`、`lastHttpSmokeResults`、`trustedGatewaySignatureStatus`、`checks`、`moduleReadiness`、`productionBlockers` 和 `generatedAt`。

`checks` 必须至少包含真实持久化、真实跨服务 HTTP、真实审计持久化、真实节点执行、真实 Cloudreve API、真实 registry、真实 scanner、真实插件事件入口、真实通知投递、真实外部消息发送、真实回调签名、生产凭据托管、异步队列、持久化事务、测试控制头默认关闭、HTTP smoke、可信网关内部签名、继承路由漂移防线、敏感字段扫描和网关路由切换。未完成真实生产能力必须以 `BLOCKED` 或 `NOT_CONNECTED` 暴露，不能返回 `PASS`。

## HTTP smoke

`POST /api/v1/ops-core/admin/http-smoke/run`

成功响应 HTTP `200`。该接口只触发本地真实 HTTP smoke，不创建业务数据，不执行真实节点动作，不发送真实外部消息。smoke 通过 `api-gateway-service` 访问当前 `ops-core-service` 承载的关键路径，目标至少包含 `/api/v1/ops-control/overview`、`/api/v1/alerting/health`、`/api/v1/cross-platform-notification/health` 和 `/api/v1/ops-core/health`。

响应 `data` 必须包含 `status`、`targetsTotal`、`passedTargetsTotal`、`failedTargetsTotal`、`targets`、`startedAt`、`finishedAt` 和 `realHttpSmoke=true`。全部目标通过时 `status=PASS`。任一目标失败时 `status=DEGRADED`，失败摘要必须脱敏，不得返回 token、请求头、异常栈、完整内部地址或真实宿主路径。执行后 readiness 的 `httpSmokeStatus`、`lastHttpSmokeAt` 和 `lastHttpSmokeResults` 必须同步更新，但 `readyForProduction` 仍为 `false`。

## 路由漂移、脱敏和禁止能力

`ops-core` 必须有自动化测试比对七个继承模块正式契约表中的 `METHOD path` 与当前 Spring Controller 注册路由。任何新增、缺失、方法漂移或路径前缀改写都必须导致测试失败。

`ops-core` 必须有敏感字段扫描测试，覆盖请求体、响应体和生产源码。以下内容不得出现在浏览器响应、审计摘要或生产源码的可执行能力中：token、secret、credential、Authorization、internalPath、worldDirectory、manifestPayload、rawPayload、registryPassword、nodeToken、Cloudreve token、webhook secret、SMTP 密码、shell 命令、真实宿主路径、`ProcessBuilder`、`Runtime.getRuntime`、真实 Docker client、containerd 直连、kubectl、helm、真实 registry pull、真实 scanner、真实 Cloudreve 删除、真实插件命令、RCON、终端命令、真实文件删除、真实恢复写入、`del /s`、`rd /s`、`rmdir /s`、`Remove-Item -Recurse` 和 `rm -rf`。

测试中为了断言禁止字符串可以出现字符串字面量，但不得形成真实执行能力。生产源码不得出现真实执行类或命令调用。

## 网关切换口径

`api-gateway-service` 必须保留原路由 ID、服务键、路径前缀、请求路径、认证透传、可信身份头剥离与注入、请求编号和响应透传规则。只允许把以下七个路由的上游端口切到 `8133`。

| 路由 ID | 服务键 | 路径前缀 | 当前上游端口 | 历史端口 |
| --- | --- | --- | ---: | ---: |
| `ops-control` | `OPS_CONTROL` | `/api/v1/ops-control` | 8133 | 8116 |
| `cloudreve-sync` | `CLOUDREVE_SYNC` | `/api/v1/cloudreve-sync` | 8133 | 8118 |
| `backup-recovery` | `BACKUP_RECOVERY` | `/api/v1/backup-recovery` | 8133 | 8119 |
| `alerting` | `ALERTING` | `/api/v1/alerting` | 8133 | 8120 |
| `plugin-integration` | `PLUGIN_INTEGRATION` | `/api/v1/plugin-integration` | 8133 | 8122 |
| `ops-image-market` | `OPS_IMAGE_MARKET` | `/api/v1/ops-image-market` | 8133 | 8124 |
| `cross-platform-notification` | `CROSS_PLATFORM_NOTIFICATION` | `/api/v1/cross-platform-notification` | 8133 | 8123 |

## 验收口径

`ops-core` API 文档按 `docs/contracts-ops-core.md` 独立存在，并由 `.local-docs/tests-ops-core.md` 记录本地测试闭环。

完成时必须满足以下条件：`ops-core-service:8133` 单进程承载七个控制面模块的全部既有 API 路径；七个模块原契约仍有效；`ops-core` 自有四个接口全覆盖；`.local-docs/tests-ops-core.md` 中的完备用例都有自动化验证；自动化测试先红灯；实现后 `mvn -q -f backend/ops-core-service/pom.xml test` 通过；第四批和第六期旧服务 Maven 入口已退役且不得恢复；`api-gateway-service` 已按契约切换并通过测试；`business-core-service`、`admission-core-service`、`engagement-core-service`、`portal-core-service` 和 `node-daemon-service` 回归通过；旧服务目录没有恢复；`node-daemon` 和 `api-gateway` 仍保持独立；生产 readiness 明确暴露剩余生产缺口；测试过程完整写入 `.local-docs/tests-ops-core.md`、`.local-docs/tests-api-gateway.md` 和 `.local-docs/tests-business-core.md`。
