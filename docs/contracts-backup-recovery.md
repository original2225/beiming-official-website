# 北冥官网 backup-recovery API 契约

版本：0.1

## 文档定位

本文档是 `backup-recovery` 微服务的正式 API 契约。`backup-recovery` 负责备份域、备份策略、备份任务、备份点索引、备份校验、恢复演练、恢复申请、审批摘要、保留策略、加密摘要、依赖健康摘要、风险审计和自检摘要。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `backup-recovery` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、任务流转、恢复流转、失败降级、审计和验收口径。

`backup-recovery` 不是 `ops-control` 的任务子页面，也不是 `node-daemon` 的执行器。第一版只做安全模拟和控制面快照，不执行真实数据库导出、真实文件复制、真实 Cloudreve 操作、真实备份删除、真实恢复写入、真实 shell 命令或真实节点调用。真实执行只能在后续独立闭环中，通过 `ops-control` 审批、`node-daemon` 授权任务、路径限制、完整性校验和回滚审计再打开。

本文档参考 GitHub Enterprise Server Backup Utilities、GitLab 备份恢复、AWS Backup、AWS Backup Restore Testing 和 Velero 的公开设计。GitHub Enterprise Server 强调独立备份主机、异地存放和版本兼容；GitLab 强调关键数据、灾备、回滚、迁移和测试环境；AWS Backup 把备份计划、保留生命周期、恢复点和恢复测试分开；Velero 把备份、计划、恢复对象、恢复顺序和对象存储数据分开。本项目只吸收策略、恢复点、恢复演练、异地冗余、状态机、审计和非破坏性恢复这些思路，不接入这些平台的主数据。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [GitHub Enterprise Server Backup Utilities](https://docs.github.com/en/enterprise-server%403.17/admin/backing-up-and-restoring-your-instance/configuring-backups-on-your-instance?learn=increase_fault_tolerance&learnProduct=admin) | 备份主机应和主实例分离，异地存放，工具版本要和实例版本保持兼容。 |
| [GitLab Backup and Restore](https://docs.gitlab.com/administration/backup_restore/) | 备份恢复服务要覆盖数据保护、灾难恢复、历史快照、合规、迁移和测试开发用途。 |
| [AWS Backup Plans](https://docs.aws.amazon.com/aws-backup/latest/devguide/about-backup-plans.html) | 备份计划定义资源、窗口、保留生命周期和恢复点管理，适合本服务拆分策略和任务。 |
| [AWS Backup Restore Testing](https://docs.aws.amazon.com/aws-backup/latest/devguide/restore-testing.html) | 恢复测试应和真实恢复分离，测试资源要带标记、可清理、可审计，并暴露验证结果。 |
| [Velero Backup Reference](https://velero.io/docs/v1.18/backup-reference/) | 备份支持排除项、计划触发、手动触发、分页和删除语义区分。 |
| [Velero Restore Reference](https://velero.io/docs/v1.18/restore-reference/) | 恢复是独立对象，创建后由控制器校验、读取备份元数据、排序并执行恢复流程。 |

## 职责边界

`backup-recovery` 负责备份域注册视图、策略管理、策略启停、按策略创建备份任务、任务列表与详情、任务取消、备份点索引、备份点详情、备份点校验、恢复演练、恢复申请、恢复审批摘要、恢复拒绝、审计列表和服务自检摘要。

`backup-recovery` 不负责用户登录、角色能力点主数据、业务模块主数据、真实数据库导出、真实文件复制、真实对象存储写入、真实 Cloudreve 管理、真实服务器文件操作、真实 shell 命令、真实容器或虚拟机控制、真实备份删除、真实恢复执行、节点守护进程执行逻辑、玩家资源下载或通知主数据。

第一版使用内存存储和受控 fake adapter。它的目标是把备份恢复 API、权限、状态机、审批、审计、脱敏和失败降级先定稳。所有恢复申请审批通过后只能进入 `COMPLETED_SIMULATED` 或 `EXECUTION_BLOCKED`，不得写入业务服务。

## 数据归属

`backup-recovery` 拥有以下主数据：BackupDomain、BackupPolicy、BackupJob、BackupPoint、BackupVerification、RestoreDrill、RestoreRequest、BackupRecoveryAuditLog、BackupRecoveryOpsSummary 和幂等记录。

`backup-recovery` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和状态快照；可以保存来自 `ops-control` 的节点、资产、备份盘和受控任务引用摘要；可以保存来自 `admin` 的模块健康聚合引用；可以保存来自 `notification` 的投递降级摘要。所有跨服务字段只能来自正式接口、后端入口可信上下文或契约允许的本地测试 stub，不能直接读取前序服务数据库、内存 store、测试种子或私有类。

备份点中的 `storageRef` 只能返回安全摘要，例如存储别名、区域摘要、保留分层和加密模式。不得返回真实绝对路径、数据库连接串、对象存储密钥、加密密钥、节点 token、Cloudreve 管理凭据或完整上游响应。

## 基础路径、端口和认证

所有接口默认使用 `/api/v1/backup-recovery` 前缀。第四批合并后当前运行入口为 `ops-core-service:8133`。历史原服务端口 `8119` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或测试入口。

健康检查 `GET /api/v1/backup-recovery/health` 不要求认证，但只能返回存活、版本、服务名和请求编号，不返回策略数量、备份点数量、存储摘要、节点摘要、内部路径或依赖错误细节。

除健康检查外，全部接口要求 `Authorization: Bearer <token>`。读取类接口要求后台角色 `HELPER`、`ADMIN` 或 `OWNER`，并具备 `NODE_READ` 或 `HIGH_RISK_APPROVE`。创建、更新、启停策略和创建备份任务要求 `ADMIN` 或 `OWNER`，并具备 `NODE_WRITE`。校验备份点、创建恢复演练、创建恢复申请、审批和拒绝恢复申请要求 `ADMIN` 或 `OWNER`，并具备 `HIGH_RISK_APPROVE`，其中恢复申请审批为 `CRITICAL` 风险。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`beforeState`、`afterState`、`auditResult`、`internalPath`、`resolvedPath`、`rawToken`、`credential`、`secretKey`、`backupEncryptionKey`、`nodeToken`、`taskStatus`、`createdBy`、`updatedBy`、`verifiedBy`、`approvedBy`、`finishedAt` 等服务端可信字段。出现可信字段时返回 `40001`。

## 本地测试控制头

`backup-recovery` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Ops-Control-Mode`、`X-Test-Notification-Mode`、`X-Test-Backup-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Now` 模拟认证失败、运维控制面不可用、通知不可用、备份 adapter 失败、超时、待审批、审计失败、状态写入失败和时间边界。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发认证失败、ops-control 失败、notification 失败、备份任务失败、审计失败、存储失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 纳入生产化硬化项。

## 前序服务兼容契约

`auth` 是强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。auth 不可用返回 `46810`，auth 超时返回 `46811`，auth 字段或枚举不兼容返回 `46812`。

`admin` 是后台聚合入口。`backup-recovery` 可以向 admin 暴露模块健康、待办摘要、恢复申请待审批数量和审计摘要，但不能让 admin 修改备份主状态。admin 不可用时，自检摘要可以返回降级摘要，业务写操作不得伪造 admin 已同步成功。

`admin` 目前的稳定契约尚未声明 `BACKUP_RECOVERY` 模块入口。本轮不得直接修改 admin 稳定接口或让 admin 写入备份恢复主状态。backup-recovery 完成本轮闭环后，如果需要后台聚合入口，必须作为 admin 的兼容增强单独走文档、测试红灯、实现和回归流程，且只能增加只读入口、待办摘要和审计索引摘要。

`ops-control` 是运维控制面。`backup-recovery` 可以读取节点、资产、备份盘和任务摘要快照，也可以保存 `opsControlTaskRef` 摘要。第一版不得直接调用 `node-daemon`，不得通过 `ops-control` 真实执行 `BACKUP_RESTORE`。ops-control 不可用返回 `46820`，超时返回 `46821`，字段不兼容返回 `46822`。

`node-daemon` 只接受 `ops-control` 已授权任务。`backup-recovery` 第一版不得直接调用 `node-daemon`。

`notification` 是辅助依赖。备份失败、恢复演练失败、恢复申请待审批和高风险完成可以形成通知提示。通知失败只记录降级摘要，不能改变备份任务、备份点或恢复申请主状态。notification 不可用返回降级摘要或 `46830`，不得导致已完成的备份任务被改成失败。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `BackupDomainType` | `DATABASE_AUTH`、`DATABASE_PROFILE`、`UPLOAD_CONTENT`、`RESOURCE_METADATA`、`INVITATION_DATA`、`WHITELIST_AUDIT`、`ATTENDANCE_LEDGER`、`PUNISHMENT_RECORD`、`REVIEW_RECORD`、`OPS_CONTROL_CONFIG`、`OPS_AUDIT_INDEX`、`CLOUDREVE_SNAPSHOT` | 备份域。第一版只做域摘要，不读取真实数据。 |
| `BackupDomainCriticality` | `LOW`、`MEDIUM`、`HIGH`、`CRITICAL` | 备份域重要性。 |
| `BackupPolicyStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`ARCHIVED` | 策略状态。 |
| `BackupTrigger` | `ADMIN_MANUAL`、`SCHEDULED`、`PRE_CHANGE_SAFETY_POINT`、`TEST_CONTROL` | 任务触发来源。 |
| `BackupJobStatus` | `PENDING`、`RUNNING`、`SUCCEEDED`、`FAILED`、`CANCELLED`、`TIMEOUT`、`PENDING_APPROVAL` | 备份任务状态。 |
| `BackupPointStatus` | `AVAILABLE`、`VERIFYING`、`VERIFIED`、`CORRUPTED`、`EXPIRED`、`DELETED_LOGICAL`、`INACCESSIBLE` | 备份点状态。 |
| `BackupVerificationStatus` | `PENDING`、`RUNNING`、`PASSED`、`FAILED`、`TIMEOUT` | 校验状态。 |
| `RestoreDrillStatus` | `PENDING`、`RUNNING`、`PASSED`、`FAILED`、`TIMEOUT`、`CANCELLED` | 恢复演练状态。 |
| `RestoreMode` | `DRY_RUN`、`SANDBOX_RESTORE`、`FULL_RESTORE_BLOCKED` | 第一版只允许 `DRY_RUN` 和 `SANDBOX_RESTORE`，`FULL_RESTORE_BLOCKED` 用于表达真实恢复被阻断。 |
| `RestoreRequestStatus` | `DRAFT`、`PENDING_APPROVAL`、`APPROVED`、`REJECTED`、`DRILL_REQUIRED`、`EXECUTION_BLOCKED`、`COMPLETED_SIMULATED`、`CANCELLED` | 恢复申请状态。 |
| `BackupRecoveryAuditResult` | `SUCCESS`、`FAILED` | 审计结果。 |
| `BackupDependencyStatus` | `AVAILABLE`、`UNAVAILABLE`、`TIMEOUT`、`BAD_SCHEMA`、`DISABLED` | 依赖摘要。 |

## 通用对象

### BackupDomain

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `domainKey` | string | 是 | 备份域键。 |
| `displayName` | string | 是 | 展示名。 |
| `sourceService` | string | 是 | 来源服务，例如 `auth`、`resource`、`ops-control`。 |
| `domainType` | string | 是 | `BackupDomainType`。 |
| `criticality` | string | 是 | 重要性。 |
| `enabled` | boolean | 是 | 是否可被策略选择。 |
| `dependencySummary` | object | 是 | 依赖摘要，只返回状态和安全说明。 |
| `lastBackupPointId` | string 或 null | 是 | 最近备份点。 |
| `lastVerifiedAt` | string 或 null | 是 | 最近校验时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### BackupPolicy

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `policyId` | string | 是 | 策略 ID。 |
| `displayName` | string | 是 | 展示名，2 到 80 位。 |
| `domains` | string[] | 是 | 备份域键，至少一个。 |
| `scheduleSummary` | object | 是 | 计划摘要，包含 `mode`、`cron`、`timezone` 和 `windowMinutes`。 |
| `retentionDays` | integer | 是 | 保留天数，1 到 3650。 |
| `minimumCopies` | integer | 是 | 最少保留份数，1 到 30。 |
| `storageRef` | object | 是 | 存储安全摘要。 |
| `encryptionMode` | string | 是 | `NONE`、`MANAGED_KEY` 或 `EXTERNAL_KMS_SUMMARY`。不得返回密钥。 |
| `status` | string | 是 | `BackupPolicyStatus`。 |
| `lastRunStatus` | string 或 null | 是 | 最近任务状态。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### BackupJob

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `jobId` | string | 是 | 备份任务 ID。 |
| `policyId` | string | 是 | 关联策略。 |
| `trigger` | string | 是 | `BackupTrigger`。 |
| `status` | string | 是 | `BackupJobStatus`。 |
| `domains` | string[] | 是 | 本次任务覆盖域。 |
| `startedAt` | string 或 null | 是 | 开始时间。 |
| `finishedAt` | string 或 null | 是 | 完成时间。 |
| `resultSummary` | object 或 null | 是 | 结果摘要，包含备份点和大小估算。 |
| `failureReason` | string 或 null | 是 | 脱敏失败原因。 |
| `idempotencyKey` | string 或 null | 是 | 幂等键。 |
| `opsControlTaskRef` | object 或 null | 是 | ops-control 任务摘要，不代表真实执行。 |
| `createdBy` | string | 是 | 创建者。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### BackupPoint

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `backupPointId` | string | 是 | 备份点 ID。 |
| `policyId` | string | 是 | 策略 ID。 |
| `jobId` | string | 是 | 来源任务 ID。 |
| `domains` | string[] | 是 | 覆盖域。 |
| `storageRef` | object | 是 | 存储安全摘要，不返回真实路径或密钥。 |
| `checksumSummary` | object | 是 | 校验摘要，包含算法和摘要前缀。 |
| `sizeBytes` | integer | 是 | 估算大小。 |
| `encrypted` | boolean | 是 | 是否加密。 |
| `verified` | boolean | 是 | 是否校验通过。 |
| `verifiedAt` | string 或 null | 是 | 校验时间。 |
| `expiresAt` | string | 是 | 过期时间。 |
| `status` | string | 是 | `BackupPointStatus`。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |
| `createdAt` | string | 是 | 创建时间。 |

### BackupVerification

字段为 `verificationId`、`backupPointId`、`status`、`validationSummary`、`failureReason`、`startedAt`、`finishedAt`、`createdBy` 和 `createdAt`。校验只读备份点摘要和校验摘要，不执行恢复写入。

### RestoreDrill

字段为 `drillId`、`backupPointId`、`domains`、`status`、`validationSummary`、`startedAt`、`finishedAt`、`failureReason`、`createdBy` 和 `createdAt`。第一版恢复演练只在 fake sandbox 中模拟校验，不写业务服务。

### RestoreRequest

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `restoreRequestId` | string | 是 | 恢复申请 ID。 |
| `backupPointId` | string | 是 | 目标备份点。 |
| `domains` | string[] | 是 | 恢复域。 |
| `restoreMode` | string | 是 | `RestoreMode`。 |
| `riskLevel` | string | 是 | `HIGH` 或 `CRITICAL`。 |
| `status` | string | 是 | `RestoreRequestStatus`。 |
| `approvalSummary` | object 或 null | 是 | 审批摘要。 |
| `requestedBy` | string | 是 | 申请人。 |
| `approvedBy` | string 或 null | 是 | 审批人。 |
| `reason` | string | 是 | 申请原因。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### BackupRecoveryAuditLog

审计字段继承公共契约，允许补充 `policyId`、`jobId`、`backupPointId`、`verificationId`、`drillId`、`restoreRequestId`、`dependencyStatus`、`stateFrom`、`stateTo`、`idempotencyKey` 和 `notificationHint`。审计列表不得提供删除接口。审计响应不得返回真实路径、数据库连接串、对象存储凭据、加密密钥、节点 token、完整请求头、备份内容、异常堆栈或恢复参数全文。

### BackupRecoveryOpsSummary

字段至少包含 `service`、`port`、`storageMode`、`authMode`、`opsControlAdapterMode`、`notificationAdapterMode`、`backupAdapterMode`、`testControlsEnabled`、`domainsTotal`、`policiesTotal`、`enabledPoliciesTotal`、`jobsTotal`、`backupPointsTotal`、`verifiedBackupPointsTotal`、`restoreDrillsTotal`、`restoreRequestsTotal`、`pendingRestoreRequestsTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastSuccessfulBackupAt`、`lastFailedBackupAt`、`degraded`、`degradeReasons` 和 `productionGaps`。

## 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `46810` | 502 | auth 认证上下文不可用。 |
| `46811` | 504 | auth 认证上下文调用超时。 |
| `46812` | 502 | auth 认证上下文字段不兼容。 |
| `46820` | 502 | ops-control 资产或任务摘要不可用。 |
| `46821` | 504 | ops-control 调用超时。 |
| `46822` | 502 | ops-control 响应字段不兼容。 |
| `46830` | 502 | notification 投递摘要不可用。 |
| `46840` | 502 | 备份 adapter 不可用。 |
| `46841` | 504 | 备份 adapter 超时。 |
| `49800` | 404 | 备份域、策略、任务、备份点、校验、演练、恢复申请或审计不存在。 |
| `49801` | 404 | 备份策略不存在。 |
| `49802` | 404 | 备份任务不存在。 |
| `49803` | 404 | 备份点不存在。 |
| `49804` | 404 | 恢复申请不存在。 |
| `49810` | 409 | 策略、任务、备份点或恢复申请状态不允许当前操作。 |
| `49811` | 409 | 备份策略名称或域组合冲突。 |
| `49812` | 409 | 幂等键请求指纹冲突。 |
| `49813` | 409 | 备份点校验失败或不可用于恢复。 |
| `49814` | 409 | 恢复申请缺少通过的恢复演练。 |
| `55400` | 500 | backup-recovery 内部错误。 |
| `55401` | 500 | backup-recovery 审计写入失败。 |
| `55402` | 500 | backup-recovery 状态写入失败。 |

字段校验、未登录、令牌格式错误、角色权限不足、能力点不足、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/backup-recovery/health` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/backup-recovery/ops/summary` | 是 | `NODE_READ` 或 `HIGH_RISK_APPROVE` | LOW |
| 备份域列表 | GET | `/api/v1/backup-recovery/domains` | 是 | `NODE_READ` | LOW |
| 策略列表 | GET | `/api/v1/backup-recovery/policies` | 是 | `NODE_READ` | LOW |
| 策略详情 | GET | `/api/v1/backup-recovery/policies/{policyId}` | 是 | `NODE_READ` | LOW |
| 创建策略 | POST | `/api/v1/backup-recovery/policies` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 更新策略 | PATCH | `/api/v1/backup-recovery/policies/{policyId}` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 启用策略 | PATCH | `/api/v1/backup-recovery/policies/{policyId}/enable` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 禁用策略 | PATCH | `/api/v1/backup-recovery/policies/{policyId}/disable` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 创建备份任务 | POST | `/api/v1/backup-recovery/jobs` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | HIGH |
| 任务列表 | GET | `/api/v1/backup-recovery/jobs` | 是 | `NODE_READ` | LOW |
| 任务详情 | GET | `/api/v1/backup-recovery/jobs/{jobId}` | 是 | `NODE_READ` | LOW |
| 取消任务 | PATCH | `/api/v1/backup-recovery/jobs/{jobId}/cancel` | 是 | 创建者、`ADMIN` 或 `OWNER` | MEDIUM |
| 备份点列表 | GET | `/api/v1/backup-recovery/backup-points` | 是 | `NODE_READ` | LOW |
| 备份点详情 | GET | `/api/v1/backup-recovery/backup-points/{backupPointId}` | 是 | `NODE_READ` | LOW |
| 校验备份点 | POST | `/api/v1/backup-recovery/backup-points/{backupPointId}/verify` | 是 | `HIGH_RISK_APPROVE`，`ADMIN` 或 `OWNER` | HIGH |
| 创建恢复演练 | POST | `/api/v1/backup-recovery/restore-drills` | 是 | `HIGH_RISK_APPROVE`，`ADMIN` 或 `OWNER` | HIGH |
| 恢复演练列表 | GET | `/api/v1/backup-recovery/restore-drills` | 是 | `NODE_READ` | LOW |
| 恢复演练详情 | GET | `/api/v1/backup-recovery/restore-drills/{drillId}` | 是 | `NODE_READ` | LOW |
| 创建恢复申请 | POST | `/api/v1/backup-recovery/restore-requests` | 是 | `HIGH_RISK_APPROVE`，`ADMIN` 或 `OWNER` | CRITICAL |
| 恢复申请列表 | GET | `/api/v1/backup-recovery/restore-requests` | 是 | `NODE_READ` 或 `HIGH_RISK_APPROVE` | LOW |
| 恢复申请详情 | GET | `/api/v1/backup-recovery/restore-requests/{restoreRequestId}` | 是 | `NODE_READ` 或 `HIGH_RISK_APPROVE` | LOW |
| 审批恢复申请 | PATCH | `/api/v1/backup-recovery/restore-requests/{restoreRequestId}/approve` | 是 | `HIGH_RISK_APPROVE` 或 `OWNER` | CRITICAL |
| 拒绝恢复申请 | PATCH | `/api/v1/backup-recovery/restore-requests/{restoreRequestId}/reject` | 是 | `HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 审计列表 | GET | `/api/v1/backup-recovery/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 健康、自检和备份域接口

`GET /api/v1/backup-recovery/health` 成功返回 `service=backup-recovery`、`status`、`version` 和 `requestId`。进程存活但依赖不可用时仍可返回 HTTP `200`，并用 `status=DEGRADED` 标记。该接口不得泄露策略、备份点、存储引用、节点、内部路径或依赖错误细节。

`GET /api/v1/backup-recovery/ops/summary` 成功返回 `BackupRecoveryOpsSummary`。合并后必须返回 `port=8133`、`legacyPort=8119`、`storageMode=IN_MEMORY`、`backupAdapterMode=SIMULATED`、`opsControlAdapterMode=TEST_STUB`、`notificationAdapterMode=TEST_STUB` 和生产化缺口。读取失败返回 `55400`，不得伪造健康。

自检摘要必须暴露正式系统设计同步状态。`productionGaps` 在第一版至少包含真实持久化未接入、真实备份介质未接入、真实跨服务 HTTP 未接入、真实恢复执行被阻断、admin 只读入口未适配和 node-daemon 直连禁止等项。该摘要用于提醒后续闭环，不允许前端把这些缺口当作可执行能力。

`GET /api/v1/backup-recovery/domains` 支持 `page`、`pageSize`、`keyword`、`sourceService`、`criticality`、`enabled` 和 `sort`。`sort` 允许 `updatedAt_desc`、`displayName_asc` 和 `criticality_desc`。成功响应分页 `items` 为 `BackupDomain[]`。备份域只表达可备份范围，不读取真实数据。

## 策略接口

`GET /api/v1/backup-recovery/policies` 支持 `page`、`pageSize`、`keyword`、`status`、`domain` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`。成功响应分页 `items` 为 `BackupPolicy[]`。

`GET /api/v1/backup-recovery/policies/{policyId}` 返回策略详情、最近任务和最近备份点摘要。策略不存在返回 `49801`。

`POST /api/v1/backup-recovery/policies` 请求字段为 `displayName`、`domains`、`scheduleSummary`、`retentionDays`、`minimumCopies`、`storageRef`、`encryptionMode`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `BackupPolicy`。策略名称冲突或同域同存储策略冲突返回 `49811`。同一操作者、同一幂等键、同一请求体重复提交返回同一策略，相同键不同体返回 `49812`。审计失败返回 `55401`，不得创建策略。

`PATCH /api/v1/backup-recovery/policies/{policyId}` 可修改 `displayName`、`domains`、`scheduleSummary`、`retentionDays`、`minimumCopies`、`storageRef`、`encryptionMode`、`reason` 和 `idempotencyKey`。`ARCHIVED` 策略不可修改。更新后必须写审计。

`PATCH /api/v1/backup-recovery/policies/{policyId}/enable` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT` 和 `DISABLED` 可启用为 `ENABLED`。`ARCHIVED` 返回 `49810`。重复启用保持幂等。

`PATCH /api/v1/backup-recovery/policies/{policyId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 可禁用为 `DISABLED`。禁用后不影响历史备份点读取。重复禁用保持幂等。

## 任务和备份点接口

`POST /api/v1/backup-recovery/jobs` 请求字段为 `policyId`、`trigger`、`domains`、`reason`、`idempotencyKey` 和可选 `opsControlTaskRef`。策略必须存在且为 `ENABLED`，否则返回 `49810`。第一版 fake adapter 可以同步完成为 `SUCCEEDED`，也可以在本地测试控制下返回 `FAILED`、`TIMEOUT` 或 `PENDING_APPROVAL`。任务成功时生成 `BackupPoint`。任务失败不得生成可用备份点。

`GET /api/v1/backup-recovery/jobs` 支持 `page`、`pageSize`、`policyId`、`status`、`trigger`、`createdBy`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`finishedAt_desc`。时间范围必须实际过滤任务 `createdAt`。

`GET /api/v1/backup-recovery/jobs/{jobId}` 返回任务详情。任务不存在返回 `49802`。

`PATCH /api/v1/backup-recovery/jobs/{jobId}/cancel` 请求字段为 `reason` 和 `idempotencyKey`。只有 `PENDING`、`RUNNING` 和 `PENDING_APPROVAL` 可取消。`SUCCEEDED`、`FAILED`、`CANCELLED` 和 `TIMEOUT` 为终态，取消返回 `49810`。

`GET /api/v1/backup-recovery/backup-points` 支持 `page`、`pageSize`、`policyId`、`jobId`、`domain`、`status`、`verified`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`expiresAt_asc`、`sizeBytes_desc`。时间范围必须实际过滤备份点 `createdAt`。

`GET /api/v1/backup-recovery/backup-points/{backupPointId}` 返回备份点详情。备份点不存在返回 `49803`。响应不得返回真实路径、密钥、连接串、完整对象存储地址或备份内容。

`POST /api/v1/backup-recovery/backup-points/{backupPointId}/verify` 请求字段为 `validationLevel`、`reason` 和 `idempotencyKey`。`validationLevel` 允许 `METADATA_ONLY`、`CHECKSUM` 和 `SANDBOX_READ`。`AVAILABLE` 或 `VERIFIED` 备份点可校验。`CORRUPTED`、`EXPIRED`、`DELETED_LOGICAL` 和 `INACCESSIBLE` 返回 `49813`。校验通过后备份点状态为 `VERIFIED`，失败时为 `CORRUPTED` 或保持原状态并记录失败摘要，同一实现版本内必须固定。

## 恢复演练和恢复申请接口

`POST /api/v1/backup-recovery/restore-drills` 请求字段为 `backupPointId`、`domains`、`validationPlan`、`reason` 和 `idempotencyKey`。备份点必须 `VERIFIED` 或 `AVAILABLE` 且未过期。第一版只模拟沙箱校验，不恢复真实业务数据。成功响应 HTTP `201`，`data` 为 `RestoreDrill`。

`GET /api/v1/backup-recovery/restore-drills` 支持 `page`、`pageSize`、`backupPointId`、`status`、`createdBy`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`finishedAt_desc`。

`GET /api/v1/backup-recovery/restore-drills/{drillId}` 返回演练详情。演练不存在返回 `49800`。

`POST /api/v1/backup-recovery/restore-requests` 请求字段为 `backupPointId`、`domains`、`restoreMode`、`drillId`、`impactSummary`、`confirmText`、`reason` 和 `idempotencyKey`。`confirmText` 必须为 `REQUEST_RESTORE_REVIEW`。`SANDBOX_RESTORE` 要求目标备份点有通过的恢复演练，缺少时返回 `49814`。第一版 `FULL_RESTORE_BLOCKED` 只能创建为 `EXECUTION_BLOCKED` 或字段校验失败，不得真实恢复。

恢复申请的 `domains` 必须是目标备份点 `domains` 的子集，不能申请恢复备份点不包含的数据域。`impactSummary.writesProduction` 在第一版必须为 `false`；出现 `true` 时返回 `40001` 或创建为 `EXECUTION_BLOCKED`，同一实现版本内必须固定并写入测试。申请请求、审批请求和响应都不得包含真实恢复目标路径、数据库连接、对象存储路径、节点地址或 shell 命令。

`GET /api/v1/backup-recovery/restore-requests` 支持 `page`、`pageSize`、`backupPointId`、`status`、`requestedBy`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`riskLevel_desc`。

`GET /api/v1/backup-recovery/restore-requests/{restoreRequestId}` 返回申请详情。申请不存在返回 `49804`。

`PATCH /api/v1/backup-recovery/restore-requests/{restoreRequestId}/approve` 请求字段为 `reviewComment`、`confirmText`、`reason` 和 `idempotencyKey`。`confirmText` 必须为 `APPROVE_SIMULATED_RESTORE`。只有 `PENDING_APPROVAL` 可审批。审批人不能审批自己创建的 `CRITICAL` 申请，返回 `49810`。审批通过后第一版只进入 `COMPLETED_SIMULATED` 或 `EXECUTION_BLOCKED`，并写入审批摘要。

审批通过不得创建 `ops-control` 的真实 `BACKUP_RESTORE` 任务，不得调用 `node-daemon`，不得修改任何业务模块数据。响应中的 `approvalSummary` 必须明确 `executionMode=SIMULATED_ONLY` 或 `executionMode=BLOCKED_BY_CONTRACT`，方便前端和审计区分审批完成与真实恢复完成。

`PATCH /api/v1/backup-recovery/restore-requests/{restoreRequestId}/reject` 请求字段为 `reviewComment`、`reason` 和 `idempotencyKey`。只有 `PENDING_APPROVAL` 可拒绝。拒绝后状态为 `REJECTED`。

## 审计接口

`GET /api/v1/backup-recovery/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`policyId`、`jobId`、`backupPointId`、`drillId`、`restoreRequestId`、`action`、`result`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`、`riskLevel_desc`。只有 `ADMIN` 和 `OWNER` 可访问。

审计中的 `requestId` 必须来自当前 HTTP 请求，不能使用固定占位值。后台写操作必须记录调用者、`reason`、操作前状态、操作后状态、请求编号、结果和失败原因。审计写入失败时，策略创建修改启停、任务创建取消、备份点校验、恢复演练、恢复申请创建、审批和拒绝不得假装成功，必须返回 `55401` 并保持业务状态不变。

## 状态、幂等和并发

策略状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

任务状态流转为 `PENDING` 到 `RUNNING`、`CANCELLED`、`FAILED` 或 `PENDING_APPROVAL`；`RUNNING` 到 `SUCCEEDED`、`FAILED`、`CANCELLED` 或 `TIMEOUT`；`PENDING_APPROVAL` 到 `RUNNING`、`FAILED` 或 `CANCELLED`；`SUCCEEDED`、`FAILED`、`CANCELLED` 和 `TIMEOUT` 为终态。

备份点状态流转为 `AVAILABLE` 到 `VERIFYING`、`VERIFIED`、`CORRUPTED`、`EXPIRED`、`INACCESSIBLE` 或 `DELETED_LOGICAL`；`VERIFIED` 可因后续校验失败进入 `CORRUPTED`；`EXPIRED` 和 `DELETED_LOGICAL` 第一版只做逻辑状态，不删除真实数据。

恢复申请状态流转为 `PENDING_APPROVAL` 到 `APPROVED`、`REJECTED`、`DRILL_REQUIRED`、`EXECUTION_BLOCKED` 或 `COMPLETED_SIMULATED`。第一版不得进入真实执行成功状态。

写接口使用幂等键时，必须使用字段名排序后的稳定 JSON 语义计算请求体指纹，嵌套对象按字段名递归排序，数组保留顺序，不能依赖 Java `Map.toString()` 或浏览器字段顺序。所有写接口必须用本服务内串行临界区保护状态推进、幂等记录、审计和响应快照。后续数据库实现必须使用事务、唯一约束、条件更新或等效机制，不能降低并发口径。

## 安全、降级和脱敏

任何响应不得包含真实文件路径、数据库连接串、对象存储凭据、加密密钥、节点 token、Cloudreve 管理 token、完整 Authorization 请求头、备份内容、恢复参数全文、异常堆栈、`.env`、`authorized_keys`、`id_rsa`、服务器密码或 shell 命令。

外部依赖不可用时，读取类接口可以返回已有快照并标记 `degraded=true` 和 `degradeReasons`。写入类接口不得假装成功。备份 adapter 失败时，任务必须明确为 `FAILED` 或接口返回 `46840`、`46841`。notification 失败只影响通知提示，不改变备份任务和恢复申请主状态。

第一版不得提供真实删除备份点接口。确需清理过期备份点时，只能在后续独立契约中增加逻辑删除或保留策略执行接口，并重新完成文档、测试红灯、实现和回归闭环。

## 验收口径

`backup-recovery` API 文档必须按 `docs/contracts-backup-recovery.md` 独立存在，并由 `.local-docs/tests-ops-core.md` 记录合并后的本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求、敏感字段脱敏、测试控制头默认关闭和模块验收口径。

`backup-recovery` 完成时必须满足以下条件：当前运行入口为 `ops-core-service:8133`，历史端口 `8119` 只作为 `legacyPort` 返回；健康检查不泄露敏感信息；除健康检查外全部接口要求后台认证；备份域、策略、任务、备份点、校验、恢复演练、恢复申请、审批、审计、幂等、状态流转、依赖失败降级、审计失败回滚、敏感字段脱敏、测试控制头默认关闭和自检摘要都有自动化验证；不修改前序服务稳定接口；不直接调用 `node-daemon`；不执行真实恢复；不把备份恢复能力塞回 `ops-control`、`admin`、`resource` 或 `cloudreve-sync`；自动化测试必须先红灯；实现后 `backup-recovery` 在 `ops-core-service` 中全部测试通过；当前后端运行入口回归测试通过；边界扫描无违规命中；测试过程记录完整。
