# 北冥官网 unified-backend API 契约

版本：0.5

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

成功响应 HTTP `200`。`data` 必须包含 `readyForProduction=false`、`readyToReplaceGateway=false`、`readyToRetireBusinessCore=false`、`readyToRetireAdmissionCore=false`、`readyToRetireEngagementCore=false`、`readyToRetireOpsCore=false`、`readyToRetirePortalCore=false`、`currentProductionEntrypointsTotal=7`、`candidateEntrypointsTotal=1`、`checks`、`lastHttpSmokeStatus`、`productionBlockers` 和 `generatedAt`。

本阶段即便所有测试通过，readiness 也不能声明可替换当前入口。`checks` 必须把 `api-gateway` 自有 API 挂载、`business-core` 自有 API 挂载、`admission-core` 自有 API 挂载、`engagement-core` 自有 API 挂载、`ops-core` 自有 API 挂载、`portal-core` 自有 API 挂载、第一批七个业务路由 in-process、第二批四个入服准入路由 in-process、第三批四个社区运营路由 in-process、第四批和第六期七个运维通知路由 in-process、第五批三个门户体验路由 in-process、旧入口保留、`node-daemon` 外部边界、路径前缀保留和响应格式保留列为通过或待验证项；动态服务发现未接入、集中配置未接入、生产审计未接入和真实生产流量演练未完成必须保留为阻塞。

## 候选 HTTP smoke

`POST /api/v1/unified-backend/admin/http-smoke/run`

成功响应 HTTP `200`。接口自身只在认证、权限、配置非法或内部状态不可用时返回错误。目标失败时返回统一成功响应，并在 `data.httpSmokeStatus` 中标记 `DEGRADED`。

smoke 目标至少包含 `UNIFIED_HEALTH`、`GATEWAY_HEALTH`、`BUSINESS_CORE_HEALTH`、`ADMISSION_CORE_HEALTH`、`ENGAGEMENT_CORE_HEALTH`、`OPS_CORE_HEALTH`、`PORTAL_CORE_HEALTH`、`AUTH_SESSION_VERIFY`、`PROFILE_MEMBERS`、`NOTIFICATION_UNREAD_COUNT`、`CONTENT_HOME`、`SERVER_STATUS_OVERVIEW`、`RESOURCE_LIST`、`ADMIN_OVERVIEW`、`ONBOARDING_PROGRESS`、`EXAM_SESSIONS`、`WHITELIST_CURRENT_APPLICATION`、`ATTENDANCE_LEADERBOARD`、`COMMUNITY_BOARDS`、`ACTIVITY_EVENTS`、`CALENDAR_UPCOMING`、`CHANGELOG_LATEST_VERSION`、`OPS_CONTROL_OVERVIEW`、`CLOUDREVE_SYNC_HEALTH`、`BACKUP_RECOVERY_HEALTH`、`ALERTING_HEALTH`、`PLUGIN_INTEGRATION_HEALTH`、`CROSS_PLATFORM_NOTIFICATION_HEALTH`、`OPS_IMAGE_MARKET_HEALTH`、`GUIDE_CATEGORIES`、`MATERIAL_FEATURED` 和 `ONLINE_MAP_HEALTH`。每个结果必须包含 `targetKey`、`serviceKey`、`method`、`path`、`mountDisposition`、`status`、`httpStatus`、`businessCode`、`durationMs`、`checkedAt` 和 `failureReason`。

响应不得返回完整请求头、Authorization、Cookie、token、异常栈、内部绝对路径、完整上游地址以外的本地环境路径或外部凭据。除 `node-daemon` 外的 25 条业务路由必须通过本地控制器成功，不能调用 `GatewayHttpClient` 的 `AUTH`、`PROFILE`、`NOTIFICATION`、`CONTENT`、`SERVER_STATUS`、`RESOURCE`、`ADMIN`、`ONBOARDING`、`EXAM`、`WHITELIST`、`ATTENDANCE`、`COMMUNITY`、`ACTIVITY`、`CALENDAR`、`CHANGELOG`、`OPS_CONTROL`、`CLOUDREVE_SYNC`、`BACKUP_RECOVERY`、`ALERTING`、`PLUGIN_INTEGRATION`、`CROSS_PLATFORM_NOTIFICATION`、`OPS_IMAGE_MARKET`、`GUIDE`、`MATERIAL` 或 `ONLINE_MAP` 代理路径。

## 验收口径

`unified-backend` API 文档按 `docs/contracts-unified-backend.md` 独立存在，并由 `.local-docs/tests-unified-backend.md` 记录本地测试闭环。

完成时必须满足以下条件：`backend/unified-backend-service` 可独立运行测试；端口固定为 `8135`；五个自有 API 全覆盖认证、权限、成功路径、脱敏、挂载清单、readiness 和 smoke 降级；除 `node-daemon` 外的 25 条业务路由在候选入口内不经 `GatewayHttpClient` 代理即可成功响应；`/api/v1/gateway/**`、`/api/v1/business-core/**`、`/api/v1/admission-core/**`、`/api/v1/engagement-core/**`、`/api/v1/ops-core/**`、`/api/v1/portal-core/**`、`/api/v1/auth/**`、`/api/v1/profile/**`、`/api/v1/notifications/**`、`/api/v1/content/**`、`/api/v1/server-status/**`、`/api/v1/resources/**`、`/api/v1/admin/**`、`/api/v1/onboarding/**`、`/api/v1/exams/**`、`/api/v1/whitelist/**`、`/api/v1/attendance/**`、`/api/v1/community/**`、`/api/v1/activity/**`、`/api/v1/calendar/**`、`/api/v1/changelog/**`、`/api/v1/ops-control/**`、`/api/v1/cloudreve-sync/**`、`/api/v1/backup-recovery/**`、`/api/v1/alerting/**`、`/api/v1/plugin-integration/**`、`/api/v1/cross-platform-notification/**`、`/api/v1/ops-image-market/**`、`/api/v1/guides/**`、`/api/v1/materials/**` 和 `/api/v1/online-map/**` 不改路径、不改响应格式；当前 `api-gateway-service:8125`、`business-core-service:8130`、`admission-core-service:8131`、`engagement-core-service:8132`、`ops-core-service:8133` 和 `portal-core-service:8134` 测试继续通过；当前 7 个生产后端 Maven 入口和候选入口回归通过；`node-daemon` 不进入候选入口源码扫描和 component scan；已退役旧服务目录、旧 Maven 入口、旧启动类和旧测试命令没有恢复；生产源码危险删除命令、真实节点执行、终端、RCON、Docker 执行和备份恢复写入扫描无命中。
