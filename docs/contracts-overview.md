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
