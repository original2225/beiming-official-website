# 北冥官网后端 API 总览

版本：0.1

## 文档定位

本文档是北冥官网后端 API 的整体索引和汇总说明，用于让维护者快速查看服务边界、端口、契约文件、测试入口和 API 覆盖规模。它不替代任何模块独立契约。正式开发、验收和变更仍以 `docs/contracts-<module>.md` 为准，公共响应、错误码、分页、认证和审计规则仍以 `docs/contracts-common.md` 为准。

面向前端开发的一体化 API 全文查阅文档见 `docs/api-reference.md`。该文档由公共契约和全部模块契约合并生成，方便前端统一检索路径、字段和响应格式。

本汇总覆盖当前仓库 `docs/contracts-*.md` 中除 `contracts-common.md` 和本文档之外的 32 个正式契约。第一批、第二批、第三批、第四批、第五批和第六期已完成运行合并并入仓。外部节点执行器已出仓，当前不计入官网后端模块、路由、入口、正式契约或验收命令。`unified-backend-service:8135` 是官网后端单服务目标入口；本地开发态 `api-gateway-service` Maven 入口已退役，本轮五个 core 独立 Maven 入口也已退役。当前候选挂载对象为 `api-gateway`、`business-core`、`admission-core`、`engagement-core`、`ops-core` 和 `portal-core`。当前契约表中的 `METHOD path` 记录总数为 765，其中第三批四个业务模块在 `engagement-core-service:8132` 中承载 149 个业务路由，第四批六个后台运维控制面模块和第六期跨平台通知控制面在 `ops-core-service:8133` 中承载 219 个业务路由，第五批后三个玩家门户体验模块在 `portal-core-service:8134` 中承载 108 个业务路由，`engagement-core` 自身提供 3 个运行单元自检和诊断路由，`ops-core` 自身提供 5 个运行单元自检、诊断和 HTTP smoke 路由，`portal-core` 自身提供 5 个运行单元自检、诊断和 HTTP smoke 路由，`api-gateway` 自身提供 8 个网关健康、路由、上游、日志和运行拓扑路由，`unified-backend` 自身提供 5 个候选入口自检、挂载和 HTTP smoke 路由，并在 readiness 中暴露真实 HTTP 演练、路由漂移、回滚窗口、入口切换开关、外部入口配置样板、外部入口切流本地演练、生产切换 runbook、生产切流审批包、生产切流外部参数 manifest、生产切流证据一致性审计、生产外部值接收演练、生产运行壳和配置接入演练、生产审计和观测 smoke 演练、本地配置 provider、本地 audit sink adapter、后端单服务准备、最终单服务后端侧收束、单服务切换、入口切换适配、入口切换执行、生产入口切换阻塞证据、外部入口切换适配与观测门禁、`api-gateway` 退役门禁、五个 core 退役前置矩阵、生产化硬化前置矩阵、旧入口退役审批、集中配置治理、生产集中配置前置、持久化审计治理预检和生产 audit sink 接入前置门禁。当前本地 Maven 入口已收束为 1 个，仓库内没有真实前端、反向代理或部署入口配置可更新，真实生产 profile、真实集中配置、敏感配置外置、真实部署入口、真实回滚配置、真实持久化审计和真实观测平台也未接入，所以 `productionAuditObservabilitySmokeStatus=PASS_PRODUCTION_AUDIT_OBSERVABILITY_SMOKE_REHEARSAL_NOT_PRODUCTION`、`productionRuntimeConfigShellStatus=PASS_PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_NOT_PRODUCTION`、`productionExternalValueIntakeRehearsalStatus=PASS_EXTERNAL_VALUE_INTAKE_REHEARSAL_NOT_PRODUCTION`、`productionCutoverEvidenceConsistencyAuditStatus=PASS_LOCAL_CUTOVER_EVIDENCE_CONSISTENCY_AUDIT_NOT_PRODUCTION`、`productionCutoverExternalParameterManifestStatus=PASS_REDACTED_EXTERNAL_PARAMETER_MANIFEST_REHEARSAL_NOT_PRODUCTION`、`productionCutoverApprovalPackageStatus=PASS_LOCAL_APPROVAL_PACKAGE_REHEARSAL_NOT_PRODUCTION`、`productionCutoverRunbookStatus=PASS_LOCAL_CUTOVER_RUNBOOK_REHEARSAL_NOT_PRODUCTION`、`externalEntrypointLocalCutoverRehearsalStatus=PASS_LOCAL_REHEARSAL_NOT_PRODUCTION`、`productionCentralConfigProviderStatus=PASS_LOCAL_FILE_PROVIDER_REHEARSAL_NOT_PRODUCTION`、`auditSinkAdapterRehearsalStatus=PASS_LOCAL_AUDIT_SINK_REHEARSAL_NOT_PRODUCTION`、`externalEntrypointConfigSamplePrecheckStatus=BLOCKED_BY_CUTOVER_SAMPLE_NOT_APPLIED`、`productionEntrypointCutoverPrecheckStatus=BLOCKED_BY_MISSING_EXTERNAL_ENTRYPOINT_CONFIG`，`externalEntrypointCutoverPrecheckStatus=BLOCKED_BY_EXTERNAL_ENTRYPOINT_CONFIG_NOT_PROVIDED`，`apiGatewayRetirementPrecheckStatus=BLOCKED_BY_TRAFFIC_NOT_SWITCHED`，`coreEntrypointRetirementPrecheckStatus=PASS_LOCAL_CORE_MAVEN_ENTRYPOINTS_RETIRED_UNIFIED_MODULES_PRESERVED`，`productionHardeningPrecheckStatus=BLOCKED_BY_EXTERNAL_PRODUCTION_PREREQUISITES`，`productionCentralConfigPrecheckStatus=BLOCKED_BY_PRODUCTION_CONFIG_PROVIDER_NOT_CONNECTED`，`productionAuditSinkPrecheckStatus=BLOCKED_BY_PERSISTENT_AUDIT_SINK_NOT_CONFIGURED`，生产流量未切到 `8135`，真实审计 sink 未连接，真实 dashboard、alert 和 trace 管道未连接，生产态旧网关退役仍被阻塞。本地开发态旧网关 Maven 入口已退役，五个 core 独立 Maven 入口也已本地退役；`readyToRetireBusinessCore=false`、`readyToRetireAdmissionCore=false`、`readyToRetireEngagementCore=false`、`readyToRetireOpsCore=false` 和 `readyToRetirePortalCore=false` 仍表示真实生产退役未闭环。

第四十二轮新增受控生产入口切流收据门禁。`unified-backend` readiness 需要暴露 `productionControlledCutoverStatus`、`productionControlledCutoverChecks` 和 `productionControlledCutoverEvidence`，默认 `productionControlledCutoverStatus=BLOCKED_BY_REAL_CUTOVER_RECEIPT_NOT_PROVIDED`。该状态只表示仓库内已经具备脱敏切流收据样板、结构校验、旧入口保护和测试守卫，不表示真实生产入口已经切到 `8135`，不表示生产流量已观测在候选入口，不表示真实 audit write smoke、dashboard、alert、trace 或回滚窗口已经完成，也不允许把真实生产退役状态改成通过。

第四十三轮新增 `api-gateway-service:8125` 受控退役预检门禁。`unified-backend-service:8135` readiness 需要暴露 `apiGatewayControlledRetirementStatus`、`apiGatewayControlledRetirementChecks` 和 `apiGatewayControlledRetirementEvidence`，默认 `apiGatewayControlledRetirementStatus=BLOCKED_BY_API_GATEWAY_RETIREMENT_RECEIPT_NOT_PROVIDED`。正式脱敏样板为 `docs/unified-backend-api-gateway-retirement-receipt-sample.json`。该状态只表示仓库内已经具备 `api-gateway-service` 退役收据、删除清单、网关自有 API parity、core 保护和 unified 自承载网关源码的本地校验能力，不表示真实生产流量已经切到 `unified-backend-service:8135`，不表示 `api-gateway:8125` 新流量归零，不表示回滚窗口完成，不表示用户已批准删除清单。

第四十四轮新增 `api-gateway-service:8125` 外部退役证据接收与删除审批门禁。`unified-backend-service:8135` readiness 需要暴露 `apiGatewayExternalRetirementEvidenceStatus`、`apiGatewayExternalRetirementEvidenceChecks` 和 `apiGatewayExternalRetirementEvidence`，默认 `apiGatewayExternalRetirementEvidenceStatus=BLOCKED_BY_EXTERNAL_API_GATEWAY_RETIREMENT_EVIDENCE_NOT_PROVIDED`。正式脱敏样板为 `docs/unified-backend-api-gateway-external-retirement-evidence-sample.json`。该状态只表示仓库内已经具备外部退役证据结构、真实切流引用槽位、真实流量归零引用槽位、真实 audit write smoke 引用槽位、dashboard、alert、trace、回滚窗口和删除清单审批门禁，不表示真实生产入口已经切到 `unified-backend-service:8135`。

第四十六轮新增真实生产入口切流证据闭环门禁。`unified-backend-service:8135` readiness 需要暴露 `realProductionEntrypointCutoverStatus`、`realProductionEntrypointCutoverChecks` 和 `realProductionEntrypointCutoverEvidence`，默认 `realProductionEntrypointCutoverStatus=BLOCKED_BY_REAL_PRODUCTION_ENTRYPOINT_CUTOVER_EVIDENCE_NOT_PROVIDED`。正式脱敏样板为 `docs/unified-backend-real-production-entrypoint-cutover-evidence-sample.json`。该状态只表示仓库内已经具备真实生产入口切流证据结构、切流窗口引用槽位、生产流量观测引用槽位、旧网关新流量归零引用槽位、真实 audit write smoke 引用槽位、dashboard、alert、trace、回滚和审批门禁，不表示真实生产入口已经切到 `unified-backend-service:8135`。即使切流证据样板完整，`oldApiGatewayRetirementAllowed` 仍必须保持 `false`。


第四十七轮完成本地开发态 `api-gateway-service` 入口退役门禁。`unified-backend-service:8135` readiness 暴露 `localApiGatewayEntrypointRetirementStatus`、`localApiGatewayEntrypointRetirementChecks` 和 `localApiGatewayEntrypointRetirementEvidence`，当前 `localApiGatewayEntrypointRetirementStatus=PASS_LOCAL_API_GATEWAY_ENTRYPOINT_RETIRED_UNIFIED_GATEWAY_APIS_PRESERVED`。该门禁只说明旧网关 Maven 入口已退役，网关能力由 `unified-backend-service:8135` 自承载。

本轮按用户确认完成五个 core 独立 Maven 入口本地退役。仓库内后端 Maven 启动入口收束为 `backend/pom.xml` 一个；`business-core`、`admission-core`、`engagement-core`、`ops-core` 和 `portal-core` 的业务源码仍保留在各自目录，由 unified 的 build-helper 装配。不得恢复五个 core 的独立 `pom.xml`、`*ServiceApplication.java` 或 `application.yml`，不得删除五个 core 目录和模块业务代码，业务路径、认证、响应格式、错误码和审计口径继续按原契约执行。

## 全局接口规则

所有业务成功响应使用统一结构，`code=0` 表示成功，`message` 为成功或错误摘要，业务数据放在 `data` 中。分页响应统一在 `data` 内返回 `items`、`page`、`pageSize` 和 `total`。

受保护接口统一使用 `Authorization: Bearer <token>`。后台、运维和节点相关接口必须继续校验角色、能力点、风险等级、二次确认、幂等键和审计字段。业务服务不能自行实现登录逻辑，不能直接读取其他服务数据表，跨模块读取必须通过正式接口、网关认证上下文或受控适配层。

玩家可见资源、服务器状态展示、后台运维控制和节点真实执行必须保持边界隔离。`resource` 只负责玩家资源分发，`server-status` 只负责玩家可见状态，`ops-control` 负责运维控制面，真实节点执行由出仓后的外部节点执行器独立闭环。

## 服务与契约总表

| 模块 | 服务目录 | 端口 | API 数 | 正式契约 | 本地测试文档 | 自动化测试入口 |
| --- | --- | ---: | ---: | --- | --- | --- |
| `activity` | `backend/engagement-core-service` | 8132 | 41 | `docs/contracts-activity.md` | `.local-docs/tests-activity.md` | `mvn -q -f backend/pom.xml test` |
| `admin` | `backend/business-core-service` | 8130 | 10 | `docs/contracts-admin.md` | `.local-docs/tests-admin.md` | `mvn -q -f backend/pom.xml test` |
| `admission-core` | `backend/admission-core-service` | 8131 | 2 | `docs/contracts-admission-core.md` | `.local-docs/tests-admission-core.md` | `mvn -q -f backend/pom.xml test` |
| `alerting` | `backend/ops-core-service` | 8133 | 24 | `docs/contracts-alerting.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/pom.xml test` |
| `api-gateway` | `backend` | 8135 | 8 | `docs/contracts-api-gateway.md` | `.local-docs/tests-unified-backend.md` | `mvn -q -f backend/pom.xml test` |
| `attendance` | `backend/admission-core-service` | 8131 | 21 | `docs/contracts-attendance.md` | `.local-docs/tests-attendance.md` | `mvn -q -f backend/pom.xml test` |
| `auth` | `backend/business-core-service` | 8130 | 20 | `docs/contracts-auth.md` | `.local-docs/tests-auth.md` | `mvn -q -f backend/pom.xml test` |
| `backup-recovery` | `backend/ops-core-service` | 8133 | 25 | `docs/contracts-backup-recovery.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/pom.xml test` |
| `business-core` | `backend/business-core-service` | 8130 | 3 | `docs/contracts-business-core.md` | `.local-docs/tests-business-core.md` | `mvn -q -f backend/pom.xml test` |
| `calendar` | `backend/engagement-core-service` | 8132 | 21 | `docs/contracts-calendar.md` | `.local-docs/tests-calendar.md` | `mvn -q -f backend/pom.xml test` |
| `changelog` | `backend/engagement-core-service` | 8132 | 23 | `docs/contracts-changelog.md` | `.local-docs/tests-changelog.md` | `mvn -q -f backend/pom.xml test` |
| `cloudreve-sync` | `backend/ops-core-service` | 8133 | 16 | `docs/contracts-cloudreve-sync.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/pom.xml test` |
| `community` | `backend/engagement-core-service` | 8132 | 64 | `docs/contracts-community.md` | `.local-docs/tests-community.md` | `mvn -q -f backend/pom.xml test` |
| `content` | `backend/business-core-service` | 8130 | 55 | `docs/contracts-content.md` | `.local-docs/tests-content.md` | `mvn -q -f backend/pom.xml test` |
| `cross-platform-notification` | `backend/ops-core-service` | 8133 | 36 | `docs/contracts-cross-platform-notification.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/pom.xml test` |
| `engagement-core` | `backend/engagement-core-service` | 8132 | 3 | `docs/contracts-engagement-core.md` | `.local-docs/tests-engagement-core.md` | `mvn -q -f backend/pom.xml test` |
| `exam` | `backend/admission-core-service` | 8131 | 28 | `docs/contracts-exam.md` | `.local-docs/tests-exam.md` | `mvn -q -f backend/pom.xml test` |
| `guide` | `backend/portal-core-service` | 8134 | 41 | `docs/contracts-guide.md` | `.local-docs/tests-guide.md` | `mvn -q -f backend/pom.xml test` |
| `material` | `backend/portal-core-service` | 8134 | 33 | `docs/contracts-material.md` | `.local-docs/tests-material.md` | `mvn -q -f backend/pom.xml test` |
| `notification` | `backend/business-core-service` | 8130 | 19 | `docs/contracts-notification.md` | `.local-docs/tests-notification.md` | `mvn -q -f backend/pom.xml test` |
| `onboarding` | `backend/admission-core-service` | 8131 | 15 | `docs/contracts-onboarding.md` | `.local-docs/tests-onboarding.md` | `mvn -q -f backend/pom.xml test` |
| `online-map` | `backend/portal-core-service` | 8134 | 34 | `docs/contracts-online-map.md` | `.local-docs/tests-online-map.md` | `mvn -q -f backend/pom.xml test` |
| `ops-control` | `backend/ops-core-service` | 8133 | 31 | `docs/contracts-ops-control.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/pom.xml test` |
| `ops-core` | `backend/ops-core-service` | 8133 | 5 | `docs/contracts-ops-core.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/pom.xml test` |
| `ops-image-market` | `backend/ops-core-service` | 8133 | 49 | `docs/contracts-ops-image-market.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/pom.xml test` |
| `plugin-integration` | `backend/ops-core-service` | 8133 | 38 | `docs/contracts-plugin-integration.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/pom.xml test` |
| `portal-core` | `backend/portal-core-service` | 8134 | 5 | `docs/contracts-portal-core.md` | `.local-docs/tests-portal-core.md` | `mvn -q -f backend/pom.xml test` |
| `profile` | `backend/business-core-service` | 8130 | 16 | `docs/contracts-profile.md` | `.local-docs/tests-profile.md` | `mvn -q -f backend/pom.xml test` |
| `resource` | `backend/business-core-service` | 8130 | 29 | `docs/contracts-resource.md` | `.local-docs/tests-resource.md` | `mvn -q -f backend/pom.xml test` |
| `server-status` | `backend/business-core-service` | 8130 | 25 | `docs/contracts-server-status.md` | `.local-docs/tests-server-status.md` | `mvn -q -f backend/pom.xml test` |
| `unified-backend` | `backend` | 8135 | 5 | `docs/contracts-unified-backend.md` | `.local-docs/tests-unified-backend.md` | `mvn -q -f backend/pom.xml test` |
| `whitelist` | `backend/admission-core-service` | 8131 | 20 | `docs/contracts-whitelist.md` | `.local-docs/tests-whitelist.md` | `mvn -q -f backend/pom.xml test` |

## 合并后运行入口

当前已入仓的合并运行入口是 `business-core-service`、`admission-core-service`、`engagement-core-service`、`ops-core-service` 和 `portal-core-service`。第三批社区运营路径已经由网关统一切到 `8132`。第四批后台运维控制面路径已经由网关统一切到 `8133`。第五批玩家门户体验路径已经由网关统一切到 `8134`。旧 `backend/online-map-service` 已退役且不得恢复，不再作为当前网关上游。`engagement-core-service` 的后台自检摘要入口已经支持网关注入的可信认证上下文；149 个业务方法路由完整行为契约已经迁入当前入口，真实业务认证、真实持久化、真实跨服务 adapter、真实通知交付和真实 HTTP smoke 仍需后续独立闭环。

`api-gateway` 的 8 个自有接口新增只读运行拓扑，不新增业务语义。当前官网后端保持 1 个 Maven 入口，`api-gateway` 行为和五个 core 模块能力由 `unified-backend-service:8135` 自承载。外部节点执行器已出仓且未连接，只能作为生产缺口摘要展示。该拓扑只用于单服务合并准备和测试守卫，不能被描述为真实生产切流完成。

`unified-backend` 是并行候选入口，端口固定为 `8135`。它在同一进程内挂载 `api-gateway` 自有 API、`business-core` 自有 API、`admission-core` 自有 API、`engagement-core` 自有 API、`ops-core` 自有 API、`portal-core` 自有 API、第一批七个基础业务路由、第二批四个入服准入路由、第三批四个社区运营路由、第四批和第六期七个运维通知路由、`guide`、`material` 和 `online-map`，只用于验证稳定运行单元逐步 in-process 装配。当前本地 Maven 入口只有 `unified-backend-service` 一个；`unified-backend-service:8135` 已承接本地网关能力和五个 core 模块能力，仍不代表真实生产入口已经切流。第三十九轮进一步补齐生产外部值接收演练 evidence，明确本仓库只记录真实切流前的外部保管类型、值组、值引用、注入目标、验证引用、回滚引用、审批引用和脱敏规则；旧网关和五个 core 独立 Maven 入口不再作为当前回归命令，前端或代理真实入口切换、生产流量入口、流量归零证明、回滚窗口完成、旧入口退役审批、真实集中配置提供方、生产 profile、敏感配置外置、真实 audit sink 配置、真实写入 smoke、真实回放、真实保留任务和真实导出路径仍为阻塞项。

`engagement-core` 的 3 个自有接口只用于运行单元自检、后台装配摘要和生产就绪诊断。它不新增 community、activity、calendar 或 changelog 的业务语义。第三批 149 个业务路由签名和四个模块完整行为契约必须在 `engagement-core-service` 中自动化验证；生产就绪诊断仍必须公开真实持久化、审计持久化、真实跨服务 adapter、真实通知交付和真实 HTTP smoke 缺口。

`ops-core` 的 5 个自有接口只用于运行单元健康检查、运行摘要、模块装配摘要、生产就绪诊断和显式 HTTP smoke。它不新增 ops-control、cloudreve-sync、backup-recovery、alerting、plugin-integration、ops-image-market 或 cross-platform-notification 的业务语义。第四批和第六期合计 219 个业务路由签名和七个模块完整行为契约必须在 `ops-core-service` 中自动化验证；生产就绪诊断仍必须公开真实持久化、审计持久化、真实跨服务 HTTP、真实节点执行、真实 Cloudreve API、真实 registry、真实 scanner、真实插件事件入口、真实通知投递、真实外部发送、真实回调签名、生产凭据托管、异步队列、持久化事务、HTTP smoke 状态和可信网关内部签名状态。

`portal-core` 的 5 个自有接口只用于运行单元健康检查、运行摘要、模块装配摘要、生产就绪诊断和显式 HTTP smoke。它不新增 guide、material 或 online-map 的业务语义。第五批后三个玩家门户体验模块的 108 个业务路由签名和完整行为契约必须在 `portal-core-service` 中自动化验证；生产就绪诊断必须公开真实持久化、真实跨服务 HTTP、真实审计持久化、真实对象存储、真实文件安全扫描、真实全文搜索、真实地图 provider HTTP、真实 marker 同步、真实瓦片托管、真实外部通知投递、静态服务发现和 HTTP smoke 状态。

| 合并批次 | 运行入口 | 端口 | 承载模块 | 旧端口用途 |
| --- | --- | ---: | --- | --- |
| 第一批基础业务 | `backend/business-core-service` | 8130 | `auth`、`profile`、`notification`、`content`、`server-status`、`resource`、`admin` | `8101` 到 `8107` 为历史原端口，旧服务目录已清理且不得恢复 |
| 第二批入服准入 | `backend/admission-core-service` | 8131 | `onboarding`、`exam`、`whitelist`、`attendance` | `8108` 到 `8111` 为历史原端口，旧服务目录已清理且不得恢复 |
| 第三批社区运营 | `backend/engagement-core-service` | 8132 | `community`、`activity`、`calendar`、`changelog` | `8112` 到 `8115` 为历史原端口，旧服务目录已清理且不得恢复 |
| 第四批运维控制面 | `backend/ops-core-service` | 8133 | `ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration`、`ops-image-market` | `8116`、`8118`、`8119`、`8120`、`8122` 和 `8124` 为历史原端口，旧服务目录已退役且不得恢复 |
| 第五批玩家门户体验 | `backend/portal-core-service` | 8134 | `guide`、`material`、`online-map` | `8127`、`8126` 和 `8121` 为历史原端口；`guide`、`material` 和 `online-map` 旧服务目录已退役且不得恢复 |
| 第六期跨平台通知控制面 | `backend/ops-core-service` | 8133 | `cross-platform-notification` | `8123` 为历史原端口，旧服务目录退役后不得恢复 |
| 统一后端候选入口 | `backend` | 8135 | 候选挂载 `api-gateway` 自有 API、`business-core` 自有 API、`admission-core` 自有 API、`engagement-core` 自有 API、`ops-core` 自有 API、`portal-core` 自有 API、`auth`、`profile`、`notification`、`content`、`server-status`、`resource`、`admin`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、`ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration`、`cross-platform-notification`、`ops-image-market`、`guide`、`material`、`online-map` | 当前唯一后端 Maven 启动入口，外部节点执行器不在仓库内挂载 |

## 依赖顺序和边界

当前后端服务已经按依赖链路沉淀为独立契约和独立测试。前序服务的契约、测试、响应格式、错误码、认证方式和数据归属默认稳定。后序服务只能通过前序服务正式 API、后端入口认证上下文或受控 stub 适配，不能反向要求前序服务改结构，也不能直接读前序服务数据库。

核心身份链路为 `auth`、`profile`、`onboarding`、`exam`、`whitelist`、`attendance` 和 `notification`。官网内容链路为 `content`、`server-status`、`resource`、`portal-core`、`guide`、`material` 和 `online-map`。社区运营链路为 `community`、`activity`、`calendar`、`changelog`、`admin` 和 `cross-platform-notification`。运维平台链路为 `api-gateway`、`ops-core`、`ops-control`、外部节点执行器、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration` 和 `ops-image-market`。

## 验收口径

任一模块变更时，必须优先更新该模块自己的 `docs/contracts-<module>.md`，再更新 `.local-docs/tests-<module>.md` 和对应自动化测试。只有模块测试和受影响的上游或下游回归测试全部通过，并留下测试过程记录后，才能认为该模块变更完成。

全量后端验收以合并后的 core 服务、未合并服务和 api-gateway 相关测试全部通过为准。本地全量测试过程记录见 `.local-docs/tests-overview.md`，该记录不提交到仓库。
