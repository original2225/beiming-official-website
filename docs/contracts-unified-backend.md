# 北冥官网 unified-backend API 契约

版本：1.3

## 文档定位

本文档是 `unified-backend-service` 统一后端候选入口的正式 API 契约。它只描述统一后端候选入口自有 API、in-process 挂载规则、readiness、HTTP smoke、边界和验收口径，不复制 `auth`、`profile`、`notification`、`content`、`server-status`、`resource`、`admin`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、`ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration`、`cross-platform-notification`、`ops-image-market`、`guide`、`material`、`online-map`、`business-core`、`admission-core`、`engagement-core`、`ops-core`、`portal-core` 或 `api-gateway` 的业务契约。

本文档继承 `docs/contracts-common.md`。统一响应格式、认证头、请求编号、错误结构、分页、角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。被挂载模块的原路径、响应格式、错误码、认证方式和测试口径仍以各自正式契约为准。

## 职责边界

`unified-backend-service` 是统一后端并行候选入口，端口固定为 `8135`，部署模式为 `CANDIDATE_PARALLEL_ENTRYPOINT`。它不替代当前生产入口，当前用于本地验证 `api-gateway`、`business-core`、`admission-core`、`engagement-core`、`ops-core` 与 `portal-core` 能否在同一 Spring Boot 进程内装配。

候选入口必须在同一进程内挂载 `api-gateway` 自有 API、`business-core` 自有 API、`admission-core` 自有 API、`engagement-core` 自有 API、`ops-core` 自有 API、`portal-core` 自有 API、第一批业务路由 `auth`、`profile`、`notification`、`content`、`server-status`、`resource`、`admin`，第二批入服准入路由 `onboarding`、`exam`、`whitelist`、`attendance`，第三批社区运营路由 `community`、`activity`、`calendar`、`changelog`，第四批和第六期运维通知路由 `ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration`、`cross-platform-notification`、`ops-image-market`，以及第五批门户体验路由 `guide`、`material` 和 `online-map`。通过候选入口访问 `/api/v1/gateway/**`、`/api/v1/business-core/**`、`/api/v1/admission-core/**`、`/api/v1/engagement-core/**`、`/api/v1/ops-core/**`、`/api/v1/portal-core/**`、`/api/v1/auth/**`、`/api/v1/profile/**`、`/api/v1/notifications/**`、`/api/v1/content/**`、`/api/v1/server-status/**`、`/api/v1/resources/**`、`/api/v1/admin/**`、`/api/v1/onboarding/**`、`/api/v1/exams/**`、`/api/v1/whitelist/**`、`/api/v1/attendance/**`、`/api/v1/community/**`、`/api/v1/activity/**`、`/api/v1/calendar/**`、`/api/v1/changelog/**`、`/api/v1/ops-control/**`、`/api/v1/cloudreve-sync/**`、`/api/v1/backup-recovery/**`、`/api/v1/alerting/**`、`/api/v1/plugin-integration/**`、`/api/v1/cross-platform-notification/**`、`/api/v1/ops-image-market/**`、`/api/v1/guides/**`、`/api/v1/materials/**` 和 `/api/v1/online-map/**` 时，路径前缀、认证方式、响应格式、错误码、请求编号、审计摘要和测试口径必须保持原样。

本阶段不退役 `api-gateway-service:8125`，不退役 `business-core-service:8130`，不退役 `admission-core-service:8131`，不退役 `engagement-core-service:8132`，不退役 `ops-core-service:8133`，不退役 `portal-core-service:8134`，不修改 26 条网关业务路由的现有上游配置，不修改前端，不恢复任何旧服务目录，不把 `node-daemon` 放进候选入口进程，不把业务路径改写成 `/api/v1/unified-backend/auth`、`/api/v1/unified-backend/onboarding`、`/api/v1/unified-backend/community`、`/api/v1/unified-backend/ops-control` 或 `/api/v1/unified-backend/guides` 之类新前缀。

`ops-core` 自有接口和七个继承业务路由在本阶段必须标记为 `IN_PROCESS`。`node-daemon` 必须标记为 `KEEP_EXTERNAL`，不得进入候选入口依赖、源码扫描、编译 source root 或 component scan。

## 基础路径、端口和认证

`unified-backend-service` 本地端口固定为 `8135`。自有接口使用 `/api/v1/unified-backend` 前缀。

健康检查无需认证。后台自有接口要求 `Authorization: Bearer <token>`。本地契约允许 `helper-token`、`admin-token` 和 `owner-token` 读取摘要、挂载清单和 readiness；HTTP smoke 只允许 `admin-token` 和 `owner-token`。`user-token` 返回 `42001`，缺失 token 返回 `41000`，非 Bearer 返回 `41003`。

候选入口接受现有网关可信上下文时，仍沿用 `X-Gateway-Internal-Request-Id`、`X-Gateway-Internal-Timestamp`、`X-Gateway-Internal-Signature` 和 `X-Beiming-Actor-*` 的现有口径。浏览器伪造可信头不得覆盖真实身份。

## 运行画像字段

统一后端候选摘要必须固定暴露以下字段：`service=unified-backend`、`deploymentMode=CANDIDATE_PARALLEL_ENTRYPOINT`、`candidatePort=8135`、`currentProductionEntrypointsTotal=7`、`candidateEntrypointsTotal=1`、`mountedEntrypoints=["api-gateway","business-core","admission-core","engagement-core","ops-core","portal-core"]`、`mountedRouteIds=["auth","profile","notification","content","server-status","resource","admin","onboarding","exam","whitelist","attendance","community","activity","calendar","changelog","ops-control","cloudreve-sync","backup-recovery","alerting","plugin-integration","cross-platform-notification","ops-image-market","guide","material","online-map"]`、`inProcessRoutesTotal=25`、`httpFallbackRoutesTotal=0`、`externalRoutesTotal=1`、`nodeDaemonDisposition=KEEP_EXTERNAL`、`readyToReplaceGateway=false`、`readyToRetireBusinessCore=false`、`readyToRetireAdmissionCore=false`、`readyToRetireEngagementCore=false`、`readyToRetireOpsCore=false` 和 `readyToRetirePortalCore=false`。

挂载状态枚举固定为 `IN_PROCESS`、`HTTP_UPSTREAM_FALLBACK` 和 `KEEP_EXTERNAL`。`IN_PROCESS` 表示请求在候选入口内由本地控制器处理，不调用 `GatewayHttpClient` 转发到当前 core 上游。`HTTP_UPSTREAM_FALLBACK` 表示仍沿用当前网关 HTTP 代理模式，本阶段除显式降级演练外不得出现在挂载清单里。`KEEP_EXTERNAL` 表示长期外部执行边界。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 候选健康检查 | GET | `/api/v1/unified-backend/health` | 否 | 无 | LOW |
| 候选摘要 | GET | `/api/v1/unified-backend/admin/ops/summary` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 候选挂载清单 | GET | `/api/v1/unified-backend/admin/mounts` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 候选 readiness | GET | `/api/v1/unified-backend/admin/readiness` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 候选 HTTP smoke | POST | `/api/v1/unified-backend/admin/http-smoke/run` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 候选健康检查

`GET /api/v1/unified-backend/health`

成功响应 HTTP `200`。`data` 必须包含 `service=unified-backend`、`status=UP`、`port=8135`、`deploymentMode=CANDIDATE_PARALLEL_ENTRYPOINT`、`mountedEntrypoints`、`mountedRouteIds` 和 `generatedAt`。该接口只表示候选进程存活，不探测上游，不代表可替换当前网关。

## 候选摘要

`GET /api/v1/unified-backend/admin/ops/summary`

成功响应 HTTP `200`。`data` 必须包含运行画像字段、`gatewayApiMounted=true`、`businessCoreMounted=true`、`admissionCoreMounted=true`、`engagementCoreMounted=true`、`opsCoreMounted=true`、`portalCoreMounted=true`、`productionEntrypointsPreserved=true`、`legacyEntrypointsRestored=false`、`productionGaps` 和 `generatedAt`。`productionGaps` 必须至少包含尚未替换当前网关、尚未退役 business-core、尚未退役 admission-core、尚未退役 engagement-core、尚未退役 ops-core、尚未退役 portal-core、动态服务发现未接入、集中配置未接入、持久化审计未接入、真实生产流量演练未完成和 node-daemon 保持外部边界。

## 候选挂载清单

`GET /api/v1/unified-backend/admin/mounts`

成功响应 HTTP `200`。`data.items` 必须列出 26 条网关业务路由以及 `api-gateway`、`business-core`、`admission-core`、`engagement-core`、`ops-core`、`portal-core` 和 `unified-backend` 自有入口。除 `node-daemon` 外的 25 条业务路由和六个当前 core 自有入口的 `mountDisposition` 必须为 `IN_PROCESS`，`node-daemon` 必须为 `KEEP_EXTERNAL`。挂载清单不得出现 `HTTP_UPSTREAM_FALLBACK`，除非后续契约明确进入降级回退演练。

每项至少包含 `routeId`、`serviceKey`、`pathPrefix`、`sourceEntrypoint`、`candidateEntrypoint`、`mountDisposition`、`currentPort`、`candidatePort`、`preservesPathPrefix`、`preservesAuth`、`preservesResponseEnvelope` 和 `boundaryReason`。挂载清单不得返回 token、Cookie、内部签名、节点密钥、异常栈或本地用户目录。

## 候选 readiness

`GET /api/v1/unified-backend/admin/readiness`

成功响应 HTTP `200`。`data` 必须包含 `readyForProduction=false`、`readyToReplaceGateway=false`、`readyToRetireBusinessCore=false`、`readyToRetireAdmissionCore=false`、`readyToRetireEngagementCore=false`、`readyToRetireOpsCore=false`、`readyToRetirePortalCore=false`、`currentProductionEntrypointsTotal=7`、`candidateEntrypointsTotal=1`、`checks`、`lastHttpSmokeStatus`、`productionBlockers`、`productionSwitchReadinessStatus`、`productionSwitchChecks`、`centralConfigPrecheckStatus`、`centralConfigPrecheckChecks`、`persistentAuditPrecheckStatus`、`persistentAuditPrecheckChecks`、`realHttpRehearsalPrecheckStatus`、`realHttpRehearsalPrecheckChecks`、`routeDriftPrecheckStatus`、`routeDriftPrecheckChecks`、`rollbackWindowPrecheckStatus`、`rollbackWindowPrecheckChecks`、`rollbackWindowEvidence`、`entrypointSwitchPrecheckStatus`、`entrypointSwitchPrecheckChecks`、`entrypointSwitchEvidence`、`productionTrafficCanaryEvidence`、`replacementDecision` 和 `generatedAt`。

本阶段即便所有测试通过，readiness 也不能声明可替换当前入口。`checks` 必须把 `api-gateway` 自有 API 挂载、`business-core` 自有 API 挂载、`admission-core` 自有 API 挂载、`engagement-core` 自有 API 挂载、`ops-core` 自有 API 挂载、`portal-core` 自有 API 挂载、第一批七个业务路由 in-process、第二批四个入服准入路由 in-process、第三批四个社区运营路由 in-process、第四批和第六期七个运维通知路由 in-process、第五批三个门户体验路由 in-process、旧入口保留、`node-daemon` 外部边界、路径前缀保留和响应格式保留列为通过或待验证项；动态服务发现未接入、集中配置未接入、生产审计未接入和真实生产流量演练未完成必须保留为阻塞。

为后续最终合并成一个后端应用服务做准备，readiness 必须额外暴露生产切换检查矩阵。`productionSwitchReadinessStatus` 在本阶段固定为 `BLOCKED`。`productionSwitchChecks` 每项必须包含 `check`、`status`、`detail` 和 `requiredForReplacement`。其中 `ALL_CURRENT_BUSINESS_ROUTES_IN_PROCESS`、`CURRENT_ENTRYPOINTS_PRESERVED`、`ROUTE_PREFIX_AND_RESPONSE_PRESERVED`、`NODE_DAEMON_EXTERNAL_BOUNDARY` 和 `LEGACY_ENTRYPOINTS_NOT_RESTORED` 必须为 `PASS`；`CENTRAL_CONFIG_READY`、`PERSISTENT_AUDIT_READY`、`REAL_HTTP_SMOKE_REHEARSAL_READY`、`FRONTEND_ENTRYPOINT_SWITCH_READY`、`ROLLBACK_WINDOW_READY` 和 `PRODUCTION_TRAFFIC_ENTRYPOINT_READY` 必须为 `BLOCKED`。`replacementDecision` 必须包含 `canReplaceGateway=false`、`canRetireIndependentCoreEntrypoints=false`、`canRetireApiGateway=false`、`nodeDaemonDisposition=KEEP_EXTERNAL`、`candidateCoverageStatus=PASS` 和 `reason`。该矩阵只用于生产切换准备，不得把 `node-daemon` 外部边界误报为可合并项，也不得把当前候选入口描述为生产替换完成。

为避免后续切换单服务时配置漂移，readiness 必须额外暴露集中配置预检摘要。`centralConfigPrecheckStatus` 在本阶段固定为 `BLOCKED`。`centralConfigPrecheckChecks` 每项必须包含 `check`、`status`、`detail` 和 `requiredForReplacement`。其中 `CANDIDATE_PORT_FIXED`、`CURRENT_ENTRYPOINT_PORTS_DOCUMENTED`、`IN_PROCESS_ROUTE_REGISTRY_FIXED`、`NODE_DAEMON_EXTERNAL_PORT_DOCUMENTED` 和 `DANGEROUS_TEST_CONTROLS_DISABLED` 必须为 `PASS`；`CENTRAL_CONFIG_PROVIDER_CONNECTED`、`PRODUCTION_PROFILE_BOUND`、`SENSITIVE_CONFIG_SOURCE_EXTERNALIZED`、`CONFIG_DRIFT_SCAN_AUTOMATED` 和 `CONFIG_ROLLBACK_SOURCE_DEFINED` 必须为 `BLOCKED`。集中配置预检只判断候选入口是否具备进入生产配置治理的基础，不得读取或返回真实密钥、token、Cookie、内部签名明文、本地用户目录或完整环境变量。

为避免后续切换单服务时审计链路漂移，readiness 必须额外暴露持久化审计预检摘要。`persistentAuditPrecheckStatus` 在本阶段固定为 `BLOCKED`。`persistentAuditPrecheckChecks` 每项必须包含 `check`、`status`、`detail` 和 `requiredForReplacement`。其中 `AUDIT_SINK_FIXED`、`AUDIT_REQUEST_ID_PRESERVED`、`AUDIT_EVENT_SCHEMA_FIXED`、`AUDIT_RETENTION_WINDOW_DOCUMENTED` 和 `AUDIT_BACKUP_EXPORT_PATH_DOCUMENTED` 必须为 `PASS`；`PERSISTENT_AUDIT_SINK_CONNECTED`、`AUDIT_WRITE_PATH_CONNECTED`、`AUDIT_REPLAY_PATH_CONNECTED`、`AUDIT_RETENTION_JOB_CONNECTED` 和 `AUDIT_CONFIG_ROLLBACK_SOURCE_DEFINED` 必须为 `BLOCKED`。持久化审计预检只判断候选入口是否具备稳定审计治理的前置条件，不得把模拟日志、内存队列、请求摘要或测试记录误报为真实持久化审计。

为进入生产替换预演阶段，readiness 必须额外暴露真实 HTTP 演练预检摘要。第十二轮开始，候选入口必须有基于真实 Web 环境和 HTTP client 的自动化演练证据，不能只依赖 MockMvc、路由注册表或静态 smoke 目标。`realHttpRehearsalPrecheckStatus` 在回滚 runbook 和回滚复检完成前仍为 `BLOCKED`。`realHttpRehearsalPrecheckChecks` 每项必须包含 `check`、`status`、`detail` 和 `requiredForReplacement`。其中 `CANDIDATE_HTTP_PORT_FIXED`、`REAL_HTTP_TARGETS_DOCUMENTED`、`AUTH_FAILURE_PATH_INCLUDED`、`NODE_DAEMON_EXCLUDED_FROM_REHEARSAL`、`SMOKE_RESULT_REDACTION_FIXED`、`CANDIDATE_PROCESS_STARTED_FOR_REHEARSAL`、`ALL_REAL_HTTP_TARGETS_PASSED` 和 `REHEARSAL_RESULT_RECORDED` 必须为 `PASS`；`REHEARSAL_RUNBOOK_DEFINED` 和 `REHEARSAL_ROLLBACK_RECHECKED` 必须为 `BLOCKED`。真实 HTTP 演练必须覆盖候选健康、六个入口健康、除 `node-daemon` 外的 25 条业务路径和至少一个认证失败路径，并且结果不得泄露 Authorization、Cookie、凭据、异常栈、本地用户目录或完整环境变量。即便真实 HTTP 演练通过，`REAL_HTTP_SMOKE_REHEARSAL_READY` 仍必须保持 `BLOCKED`，直到回滚 runbook 和回滚复检也完成。

为避免生产入口切换时出现路由漂移，readiness 必须额外暴露路由漂移预检摘要。第十二轮开始，`routeDriftPrecheckStatus` 必须为 `PASS`，表示候选入口已经用自动化测试对比 `api-gateway` 当前 26 条业务路由和 `unified-backend` 挂载清单。`routeDriftPrecheckChecks` 每项必须包含 `check`、`status`、`detail` 和 `requiredForReplacement`。其中 `CURRENT_GATEWAY_ROUTES_DOCUMENTED`、`UNIFIED_MOUNT_ROUTES_DOCUMENTED`、`ROUTE_PREFIX_PRESERVED`、`NODE_DAEMON_ROUTE_KEPT_EXTERNAL`、`NO_HTTP_UPSTREAM_FALLBACK_IN_CANDIDATE`、`REAL_GATEWAY_TO_UNIFIED_DIFF_SCAN_AUTOMATED`、`AUTH_BEHAVIOR_DIFF_SCAN_AUTOMATED`、`ERROR_CODE_DIFF_SCAN_AUTOMATED`、`SENSITIVE_FIELD_DIFF_SCAN_AUTOMATED` 和 `DRIFT_SCAN_RESULT_RECORDED` 必须为 `PASS`。路由漂移预检只判断切流前的对比门槛，不新增业务路径，不改写任何原路径前缀，不把 `node-daemon` 纳入 in-process 候选。

为保留旧入口回退能力，readiness 必须额外暴露回滚窗口预检摘要。第十三轮开始，候选入口必须把回滚窗口时长、回滚触发条件、回滚复检自动化和回滚记录证据纳入只读 readiness 证据，但不得把旧入口退役审批误报为完成。`rollbackWindowPrecheckStatus` 在旧入口退役审批完成前仍为 `BLOCKED`。`rollbackWindowPrecheckChecks` 每项必须包含 `check`、`status`、`detail` 和 `requiredForReplacement`。其中 `CURRENT_ENTRYPOINTS_STILL_PRESENT`、`CURRENT_ENTRYPOINT_TESTS_STILL_REQUIRED`、`API_GATEWAY_ROLLBACK_TARGET_DOCUMENTED`、`CORE_ENTRYPOINTS_ROLLBACK_TARGETS_DOCUMENTED`、`NODE_DAEMON_UNAFFECTED_BY_CANDIDATE`、`ROLLBACK_WINDOW_DURATION_DEFINED`、`ROLLBACK_TRIGGER_CRITERIA_DEFINED`、`ROLLBACK_RECHECK_AUTOMATED` 和 `ROLLBACK_RECORDING_COMPLETED` 必须为 `PASS`；`OLD_ENTRYPOINT_RETIREMENT_APPROVAL_READY` 必须为 `BLOCKED`。`rollbackWindowEvidence` 必须包含 `windowDuration`、`triggerCriteria`、`recheckAutomation`、`rollbackTargets`、`recordingStatus` 和 `retirementApprovalStatus`。`windowDuration.status` 必须为 `DEFINED`，`windowDuration.minimumHours` 必须为 `24`；`triggerCriteria.items` 必须至少覆盖真实 HTTP 演练失败、路由漂移、认证错误码漂移、当前入口回归失败、生产源码边界扫描命中和 `node-daemon` 边界异常；`recheckAutomation.commands` 必须记录候选入口测试、当前八个 Maven 入口回归、`git diff --check` 和生产源码边界扫描；`rollbackTargets` 必须记录 `api-gateway:8125`、五个 core 入口 `8130` 到 `8134`、`unified-backend:8135` 候选入口和 `node-daemon:8117` 外部边界；`recordingStatus` 必须为 `COMPLETED`；`retirementApprovalStatus` 必须为 `BLOCKED`。回滚窗口预检不得把当前 7 个生产入口描述为可删除，`ROLLBACK_WINDOW_READY` 必须继续保持 `BLOCKED`。

为下一轮入口切换做准备，readiness 必须额外暴露入口切换开关画像。第十四轮开始，候选入口必须把入口切换演练目标、业务路径保持、测试自动化和审计记录就绪状态纳入只读 readiness evidence，但不得真实切换前端、外部代理或生产流量。第十五轮开始，候选入口必须把生产灰度流量计划纳入只读 readiness evidence，做法参考金丝雀发布的分阶权重、暂停门禁和回滚保留原则，但不得让候选入口接收真实生产流量。`entrypointSwitchPrecheckStatus` 在本阶段固定为 `BLOCKED`。`entrypointSwitchPrecheckChecks` 每项必须包含 `check`、`status`、`detail` 和 `requiredForReplacement`。其中 `BUSINESS_PATHS_REMAIN_UNCHANGED`、`CANDIDATE_BASE_URL_DOCUMENTED`、`FRONTEND_NOT_MODIFIED_IN_THIS_ROUND`、`PROXY_SWITCH_SCOPE_DOCUMENTED`、`SWITCH_REQUIRES_ROLLBACK_WINDOW`、`ENTRYPOINT_SWITCH_TESTS_AUTOMATED`、`SWITCH_AUDIT_RECORDING_READY` 和 `PRODUCTION_TRAFFIC_CANARY_DEFINED` 必须为 `PASS`；`FRONTEND_ENTRYPOINT_SWITCH_IMPLEMENTED` 和 `EXTERNAL_PROXY_SWITCH_IMPLEMENTED` 必须为 `BLOCKED`。`entrypointSwitchEvidence` 必须包含 `candidateBaseUrl=http://127.0.0.1:8135`、`currentGatewayBaseUrl=http://127.0.0.1:8125`、`businessPathsRemainUnchanged=true`、`switchMode=ENTRYPOINT_TARGET_ONLY`、`forbiddenPathPrefix=/api/v1/unified-backend/<module>`、`rollbackTarget=api-gateway:8125`、`rehearsalStatus=PASS` 和 `auditRecordingStatus=READY_FOR_REHEARSAL`。`productionTrafficCanaryEvidence` 必须包含 `strategy=CANARY_WITH_PAUSE_AND_ROLLBACK`、`plannedWeights=[0,5,25,50,100]`、`initialWeightPercent=0`、`currentProductionTrafficPercent=0`、`candidateProductionTrafficPercent=0`、`manualPromotionRequired=true`、`rollbackTarget=api-gateway:8125`、`rollbackWindowMinimumHours=24`、`trafficSwitchApplied=false`、`status=PLAN_DEFINED_NOT_APPLIED` 和 `gates`。`gates` 至少包含真实 HTTP 演练通过、路由漂移扫描通过、回滚窗口证据完成、当前入口全量回归通过、生产源码边界扫描无命中、前端入口开关就绪和外部代理开关就绪。入口切换开关画像只记录未来从 `8125` 切到 `8135` 的入口目标，不允许把业务路径改成 `/api/v1/unified-backend/<module>`，也不得返回真实外部代理配置、前端构建变量、token、Cookie、内部签名、本地用户目录或完整环境变量。`FRONTEND_ENTRYPOINT_SWITCH_READY` 和 `PRODUCTION_TRAFFIC_ENTRYPOINT_READY` 必须继续保持 `BLOCKED`。

## 候选 HTTP smoke

`POST /api/v1/unified-backend/admin/http-smoke/run`

成功响应 HTTP `200`。接口自身只在认证、权限、配置非法或内部状态不可用时返回错误。目标失败时返回统一成功响应，并在 `data.httpSmokeStatus` 中标记 `DEGRADED`。

smoke 目标至少包含 `UNIFIED_HEALTH`、`GATEWAY_HEALTH`、`BUSINESS_CORE_HEALTH`、`ADMISSION_CORE_HEALTH`、`ENGAGEMENT_CORE_HEALTH`、`OPS_CORE_HEALTH`、`PORTAL_CORE_HEALTH`、`AUTH_SESSION_VERIFY`、`PROFILE_MEMBERS`、`NOTIFICATION_UNREAD_COUNT`、`CONTENT_HOME`、`SERVER_STATUS_OVERVIEW`、`RESOURCE_LIST`、`ADMIN_OVERVIEW`、`ONBOARDING_PROGRESS`、`EXAM_SESSIONS`、`WHITELIST_CURRENT_APPLICATION`、`ATTENDANCE_LEADERBOARD`、`COMMUNITY_BOARDS`、`ACTIVITY_EVENTS`、`CALENDAR_UPCOMING`、`CHANGELOG_LATEST_VERSION`、`OPS_CONTROL_OVERVIEW`、`CLOUDREVE_SYNC_HEALTH`、`BACKUP_RECOVERY_HEALTH`、`ALERTING_HEALTH`、`PLUGIN_INTEGRATION_HEALTH`、`CROSS_PLATFORM_NOTIFICATION_HEALTH`、`OPS_IMAGE_MARKET_HEALTH`、`GUIDE_CATEGORIES`、`MATERIAL_FEATURED` 和 `ONLINE_MAP_HEALTH`。每个结果必须包含 `targetKey`、`serviceKey`、`method`、`path`、`mountDisposition`、`status`、`httpStatus`、`businessCode`、`durationMs`、`checkedAt` 和 `failureReason`。

响应不得返回完整请求头、Authorization、Cookie、token、异常栈、内部绝对路径、完整上游地址以外的本地环境路径或外部凭据。除 `node-daemon` 外的 25 条业务路由必须通过本地控制器成功，不能调用 `GatewayHttpClient` 的 `AUTH`、`PROFILE`、`NOTIFICATION`、`CONTENT`、`SERVER_STATUS`、`RESOURCE`、`ADMIN`、`ONBOARDING`、`EXAM`、`WHITELIST`、`ATTENDANCE`、`COMMUNITY`、`ACTIVITY`、`CALENDAR`、`CHANGELOG`、`OPS_CONTROL`、`CLOUDREVE_SYNC`、`BACKUP_RECOVERY`、`ALERTING`、`PLUGIN_INTEGRATION`、`CROSS_PLATFORM_NOTIFICATION`、`OPS_IMAGE_MARKET`、`GUIDE`、`MATERIAL` 或 `ONLINE_MAP` 代理路径。

## 验收口径

`unified-backend` API 文档按 `docs/contracts-unified-backend.md` 独立存在，并由 `.local-docs/tests-unified-backend.md` 记录本地测试闭环。

完成时必须满足以下条件：`backend/unified-backend-service` 可独立运行测试；端口固定为 `8135`；五个自有 API 全覆盖认证、权限、成功路径、脱敏、挂载清单、readiness、smoke 降级、真实 HTTP 演练和路由漂移扫描；除 `node-daemon` 外的 25 条业务路由在候选入口内不经 `GatewayHttpClient` 代理即可成功响应；`/api/v1/gateway/**`、`/api/v1/business-core/**`、`/api/v1/admission-core/**`、`/api/v1/engagement-core/**`、`/api/v1/ops-core/**`、`/api/v1/portal-core/**`、`/api/v1/auth/**`、`/api/v1/profile/**`、`/api/v1/notifications/**`、`/api/v1/content/**`、`/api/v1/server-status/**`、`/api/v1/resources/**`、`/api/v1/admin/**`、`/api/v1/onboarding/**`、`/api/v1/exams/**`、`/api/v1/whitelist/**`、`/api/v1/attendance/**`、`/api/v1/community/**`、`/api/v1/activity/**`、`/api/v1/calendar/**`、`/api/v1/changelog/**`、`/api/v1/ops-control/**`、`/api/v1/cloudreve-sync/**`、`/api/v1/backup-recovery/**`、`/api/v1/alerting/**`、`/api/v1/plugin-integration/**`、`/api/v1/cross-platform-notification/**`、`/api/v1/ops-image-market/**`、`/api/v1/guides/**`、`/api/v1/materials/**` 和 `/api/v1/online-map/**` 不改路径、不改响应格式；当前 `api-gateway-service:8125`、`business-core-service:8130`、`admission-core-service:8131`、`engagement-core-service:8132`、`ops-core-service:8133` 和 `portal-core-service:8134` 测试继续通过；当前 7 个生产后端 Maven 入口和候选入口回归通过；`node-daemon` 不进入候选入口源码扫描和 component scan；已退役旧服务目录、旧 Maven 入口、旧启动类和旧测试命令没有恢复；生产源码危险删除命令、真实节点执行、终端、RCON、Docker 执行和备份恢复写入扫描无命中。
