# 北冥官网 attendance API 契约

版本：0.1

## 文档定位

本文档是 `attendance` 微服务的正式 API 契约。后续 `community`、`activity`、`calendar`、`changelog`、前端适配、`admin` 聚合、`ops-control` 和 `node-daemon` 只能通过本文档定义的接口读取考勤账户、积分流水、贡献记录、月度扣分、榜单、白名单移除候选、审计和自检摘要，不能直接读取或修改 `attendance` 数据库，也不能把考勤积分逻辑塞进其他服务。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `attendance` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了现有优秀平台的成熟做法。Stack Overflow 的声望、徽章和权限说明证明积分必须来自可解释的行为，并且高积分只解锁更高信任能力，不应变成人工随意奖惩。GitHub 贡献图说明公开活跃展示需要明确什么行为计入贡献，且展示口径和真实数据来源要分开。Atlassian Jira workflow 的条件、校验器和后置动作说明状态推进前要先校验，通知、审计等后置动作不能反过来绕开状态规则。Discord Rules Screening 说明准入流程中的门槛应由服务端记录，不只靠前端提示。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Stack Overflow reputation](https://stackoverflow.com/help/whats-reputation) | 积分代表社区信任，增减都要有可解释原因，并影响后续能力。 |
| [Stack Overflow privileges](https://stackoverflow.com/help/privileges) | 权限或能力应随可信度分层解锁，后台写操作不能只靠展示积分判断。 |
| [Stack Overflow badges](https://stackoverflow.com/help/what-are-badges) | 激励项应基于可量化行为，不应由人工随意发放。 |
| [GitHub profile contributions reference](https://docs.github.com/en/account-and-profile/reference/profile-contributions-reference) | 活跃展示必须定义计入口径、可见性和时间口径。 |
| [Atlassian Jira workflow validators](https://support.atlassian.com/jira-cloud-administration/docs/use-workflow-validators-with-custom-fields/) | 状态流转前必须做字段、权限和条件校验，失败时不得执行后置动作。 |
| [Discord Rules Screening FAQ](https://support.discord.com/hc/en-us/articles/1500000466882-Rules-Screening-FAQ) | 入门门槛需要被服务端记录和追踪，不能只依赖前端展示。 |

## 职责边界

`attendance` 负责成员考勤账户、积分余额、积分流水、贡献记录、月度扣分任务、榜单、白名单移除候选、初始化交接消费、幂等记录和自身审计。

`attendance` 不负责注册、登录、邀请码、会话、账号角色能力点、Minecraft 绑定主数据、成员档案主数据、考试判分、白名单申请审核、真实服务器在线采集、真实 Minecraft 白名单移除命令、社区帖子、活动报名、内容审核、资源下载、后台聚合入口、服务器运维控制、节点守护进程、容器、终端、文件管理、备份恢复或 Cloudreve 管理。

`attendance` 只能适配前序服务。它通过 `auth` 认证上下文读取当前用户和后台操作者，通过 `whitelist` 的 attendance handoff 初始化考勤账户，通过 `profile` 正式接口或未来服务间适配器校验成员状态并保存展示快照，通过 `notification` 投递积分变化、扣分预警和候选提醒。它不能要求前序服务反向写入 attendance 状态，不能导入前序服务内存存储、实体、Repository、测试种子或内部类。

## 数据归属

`attendance` 拥有以下主数据：成员考勤账户、积分余额、积分流水、贡献记录、月度周期、扣分运行记录、榜单快照、白名单移除候选、初始化交接消费记录、幂等记录、依赖调用摘要和 attendance 审计日志。

`attendance` 可以保存来自 `whitelist` 的 `handoffId`、`applicationId`、`userId`、`memberId`、`minecraftBindingSnapshot`、`reviewDirection`、`attemptType`、`approvedAt`、`scoreSummary`、`handoffVersion` 和 `generatedAt` 快照。它可以保存来自 `profile` 的成员展示名、头像、成员组和成员状态快照，可以保存来自 `notification` 的投递结果摘要。快照不是来源模块主数据，不能用于反写来源模块。

## 基础路径与认证

所有接口默认使用 `/api/v1/attendance` 前缀。P0 端口固定为 `8111`，自检摘要必须返回该端口。

公开接口只包括公开榜单，路径为 `/api/v1/attendance/leaderboard`，允许游客访问，但不得返回内部备注、扣分原因全文、管理员 ID、审计参数、通知失败详情或白名单移除候选详情。

当前用户接口使用 `/api/v1/attendance/me` 前缀，全部要求 `Authorization: Bearer <token>`，只能访问当前认证用户自己的考勤账户、流水、贡献记录和榜单位置。浏览器请求体不得传入 `userId`、`memberId`、`roles`、`permissions`、`score`、`balanceAfter`、`status`、`operatorUserId`、`sourceModule`、`sourceId`、`notificationStatus`、`profileSnapshot` 等服务端可信字段。

后台接口使用 `/api/v1/attendance/admin` 前缀，全部要求登录。后台读取账户、贡献、榜单、候选和自检摘要要求 `HELPER`、`ADMIN` 或 `OWNER`。初始化账户、积分调整、流水撤销、贡献记录创建或修正、月度扣分、候选确认或驳回、榜单重算和审计读取要求 `ADMIN` 或 `OWNER`。`HELPER` 可以读取低风险后台汇总，不能写积分，不能运行扣分任务，不能确认白名单移除候选。

## 本地测试控制头

attendance 允许在本地自动化测试中使用 `X-Test-Whitelist-Mode`、`X-Test-Profile-Mode`、`X-Test-Notification-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Fail-Ledger` 模拟依赖失败、通知失败和写入失败。该能力只服务测试闭环，不属于正式业务 API。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、状态失败、流水失败、通知失败或 profile stale。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 视为已满足的生产化硬化项。

## 前序服务兼容契约

`auth` 是强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `ACTIVE` 时可读取自己的考勤数据；`DISABLED`、`BANNED`、`DELETED` 不允许读取或写入。auth 不可用返回 `48000`，auth 超时返回 `48001`，字段或枚举不兼容返回 `48002`。

`whitelist` 是初始化强依赖。初始化考勤账户时必须读取 `GET /api/v1/whitelist/admin/applications/{applicationId}/attendance-handoff` 或未来等价服务间适配器。只有交接快照满足 `initializationStatus=WAITING_MODULE` 或 `READY_FOR_CONSUME`、`memberId` 和 `userId` 完整、`handoffVersion` 未被当前 attendance 消费过、申请仍允许初始化时，才允许创建考勤账户。同一 handoff 重放必须幂等返回同一个初始化结果，不能重复加初始积分。whitelist 不可用返回 `48010`，超时返回 `48011`，字段不兼容返回 `48012`，交接状态不允许返回 `45010`，交接已被其他初始化消费返回 `45011`。

`profile` 是初始化和后台详情的强依赖。初始化时必须校验 `memberId` 对应成员仍可激活考勤，推荐状态为 `ACTIVE`。profile 不可用、超时或字段不兼容时，初始化不得伪造成功，分别返回 `48020`、`48021` 或 `48022`。榜单和只读详情可以在 profile 不可用时降级使用已保存成员快照，但必须返回 `profileSnapshotStale=true` 或依赖摘要，且不得刷新为伪造资料。

`notification` 是辅助依赖。初始化成功、管理员调整、流水撤销、月度扣分、候选生成、候选确认和候选驳回可以触发通知。通知失败不得回滚积分主状态，但必须保存 `notificationStatus=FAILED`、失败原因摘要并写入审计。notification 不可用记录或返回 `48030`，超时记录或返回 `48031`，字段不兼容记录或返回 `48032`。通知失败摘要不得包含通知正文、完整请求头、认证 token、内部 URL 或异常堆栈。

`admin` 当前只是聚合入口。本文档不修改 `admin` 契约。若后续需要 admin 展示 attendance 待办、指标或审计索引，必须作为 admin 兼容变更单独走契约、测试、红灯、实现和回归闭环。

`server-status` 当前不作为 attendance 强依赖。真实在线时长未来可以通过独立事件或正式状态采集适配进入，但 P0 不得直接读取 server-status 历史快照扣分，避免把展示状态当作考勤事实。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `AttendanceAccountStatus` | `PENDING_INITIALIZATION`、`ACTIVE`、`FROZEN`、`REMOVAL_CANDIDATE`、`REMOVED`、`ARCHIVED` | 考勤账户状态。初始化成功后默认 `ACTIVE`。 |
| `AttendanceLedgerType` | `INITIAL_GRANT`、`ADMIN_ADJUSTMENT`、`ACTIVITY_REWARD`、`CONTRIBUTION_REWARD`、`MONTHLY_DEDUCTION`、`REVERSAL` | 积分流水类型。P0 活动和贡献奖励可由后台受控写入。 |
| `AttendanceLedgerStatus` | `POSTED`、`REVERSED` | 流水状态。撤销不会删除原流水，只生成反向流水。 |
| `ContributionType` | `ONLINE_ACTIVE`、`PROJECT_BUILD`、`EVENT_PARTICIPATION`、`WORK_SUBMISSION`、`HELPER_SUPPORT`、`MANUAL` | 贡献记录类型。P0 通过后台受控写入，未来由对应模块适配。 |
| `MonthlyRunStatus` | `PENDING`、`RUNNING`、`COMPLETED`、`FAILED`、`PARTIAL_FAILED` | 月度扣分运行状态。 |
| `RemovalCandidateStatus` | `OPEN`、`CONFIRMED`、`DISMISSED`、`EXPIRED` | 白名单移除候选状态。候选只代表考勤建议，不直接执行 whitelist 移除。 |
| `AttendanceNotificationStatus` | `DELIVERED`、`FAILED`、`SKIPPED` | 最近一次通知摘要。 |
| `AttendanceAuditResult` | `SUCCESS`、`FAILED` | attendance 审计结果。 |

## 通用对象

### AttendanceAccount

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `accountId` | string | 是 | 考勤账户 ID。 |
| `userId` | string | 是 | auth 用户 ID。 |
| `memberId` | string | 是 | profile 成员 ID。 |
| `displayNameSnapshot` | string | 是 | 成员展示名快照。 |
| `avatarUrlSnapshot` | string 或 null | 是 | 头像快照。 |
| `memberGroupSnapshot` | string 或 null | 是 | 成员组快照。 |
| `memberStatusSnapshot` | string | 是 | 成员状态快照。 |
| `minecraftBindingSnapshot` | object | 是 | Minecraft 绑定快照。 |
| `status` | string | 是 | `AttendanceAccountStatus`。 |
| `scoreBalance` | integer | 是 | 当前积分余额，最低为 `0`。 |
| `initialScore` | integer | 是 | 初始化积分，P0 默认 `100`。 |
| `totalEarned` | integer | 是 | 历史累计正向积分。 |
| `totalDeducted` | integer | 是 | 历史累计扣分绝对值。 |
| `lastPositiveActivityAt` | string 或 null | 是 | 最近一次正向贡献或活跃时间。 |
| `lastDeductedAt` | string 或 null | 是 | 最近一次扣分时间。 |
| `lastLedgerId` | string 或 null | 是 | 最近流水 ID。 |
| `whitelistApplicationId` | string | 是 | 来源白名单申请 ID。 |
| `whitelistHandoffId` | string | 是 | 来源 handoff ID。 |
| `whitelistHandoffVersion` | integer | 是 | 来源 handoff 版本。 |
| `reviewDirection` | string | 是 | 审核方向。 |
| `attemptType` | string | 是 | 首次入服或二次考核。 |
| `notificationStatus` | string 或 null | 是 | 最近一次通知状态。 |
| `notificationFailure` | AttendanceNotificationFailureSummary 或 null | 是 | 通知失败脱敏摘要。 |
| `profileSnapshotStale` | boolean | 是 | profile 降级时是否使用旧快照。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |

### AttendanceLedgerEntry

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ledgerId` | string | 是 | 流水 ID。 |
| `accountId` | string | 是 | 考勤账户 ID。 |
| `memberId` | string | 是 | 成员 ID。 |
| `userId` | string | 是 | 用户 ID。 |
| `type` | string | 是 | `AttendanceLedgerType`。 |
| `status` | string | 是 | `AttendanceLedgerStatus`。 |
| `delta` | integer | 是 | 积分变化值，正数加分，负数扣分。 |
| `balanceBefore` | integer | 是 | 变更前余额。 |
| `balanceAfter` | integer | 是 | 变更后余额。 |
| `sourceModule` | string | 是 | 来源模块。P0 允许 `attendance`、`whitelist`、`manual`。 |
| `sourceId` | string | 是 | 来源对象 ID。 |
| `cycleKey` | string 或 null | 是 | 月度周期，例如 `2026-05`。 |
| `reason` | string | 是 | 原因，后台可见，1 到 500 位。 |
| `publicReason` | string | 是 | 对成员可见原因，1 到 200 位。 |
| `operatorUserId` | string 或 null | 是 | 操作者用户 ID。系统任务可为 `null`。 |
| `idempotencyKey` | string 或 null | 是 | 幂等键。 |
| `reversalOfLedgerId` | string 或 null | 是 | 撤销来源流水 ID。 |
| `reversedByLedgerId` | string 或 null | 是 | 被哪条反向流水撤销。 |
| `notificationStatus` | string 或 null | 是 | 通知状态。 |
| `notificationFailure` | AttendanceNotificationFailureSummary 或 null | 是 | 通知失败摘要。 |
| `createdAt` | string | 是 | 创建时间。 |
| `reversedAt` | string 或 null | 是 | 原流水被撤销时间。 |

### ContributionRecord

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `contributionId` | string | 是 | 贡献记录 ID。 |
| `accountId` | string | 是 | 考勤账户 ID。 |
| `memberId` | string | 是 | 成员 ID。 |
| `userId` | string | 是 | 用户 ID。 |
| `type` | string | 是 | `ContributionType`。 |
| `sourceModule` | string | 是 | 来源模块。P0 后台写入时为 `attendance` 或 `manual`。 |
| `sourceId` | string | 是 | 来源对象 ID。 |
| `title` | string | 是 | 2 到 80 位。 |
| `description` | string | 否 | 最多 1000 位。 |
| `occurredAt` | string | 是 | 贡献发生时间。 |
| `scoreDelta` | integer | 是 | 该贡献对应积分变化，可为 `0`。 |
| `ledgerId` | string 或 null | 是 | 关联流水。 |
| `operatorUserId` | string | 是 | 录入或修正操作者。 |
| `correctionOfContributionId` | string 或 null | 是 | 修正来源贡献 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### MonthlyDeductionRun

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `runId` | string | 是 | 扣分运行 ID。 |
| `cycleKey` | string | 是 | 月度周期，格式 `YYYY-MM`。 |
| `status` | string | 是 | `MonthlyRunStatus`。 |
| `dryRun` | boolean | 是 | 是否只预检。 |
| `reason` | string | 是 | 后台原因。 |
| `deductionScore` | integer | 是 | 本周期扣分，P0 默认 `20`。 |
| `eligibleAccounts` | integer | 是 | 纳入检查账户数。 |
| `deductedAccounts` | integer | 是 | 实际扣分账户数。 |
| `skippedAccounts` | integer | 是 | 因有活跃或状态不符跳过账户数。 |
| `candidateCreated` | integer | 是 | 本次生成候选数。 |
| `previewItems` | MonthlyDeductionPreviewItem[] | 是 | 预览或执行结果摘要。 |
| `idempotencyKey` | string 或 null | 是 | 幂等键。 |
| `startedAt` | string 或 null | 是 | 开始时间。 |
| `completedAt` | string 或 null | 是 | 完成时间。 |
| `failureReason` | string 或 null | 是 | 脱敏失败摘要。 |
| `createdBy` | string | 是 | 操作者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |

### RemovalCandidate

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `candidateId` | string | 是 | 候选 ID。 |
| `accountId` | string | 是 | 考勤账户 ID。 |
| `memberId` | string | 是 | 成员 ID。 |
| `userId` | string | 是 | 用户 ID。 |
| `displayNameSnapshot` | string | 是 | 成员展示名快照。 |
| `scoreBalance` | integer | 是 | 生成候选时余额。 |
| `cycleKey` | string | 是 | 触发周期。 |
| `status` | string | 是 | `RemovalCandidateStatus`。 |
| `reason` | string | 是 | 后台原因。 |
| `publicReason` | string | 是 | 对成员可见说明。 |
| `recommendedAction` | string | 是 | P0 固定为 `WHITELIST_REVIEW_REQUIRED`。 |
| `confirmedBy` | string 或 null | 是 | 确认操作者。 |
| `confirmedAt` | string 或 null | 是 | 确认时间。 |
| `dismissedBy` | string 或 null | 是 | 驳回操作者。 |
| `dismissedAt` | string 或 null | 是 | 驳回时间。 |
| `dismissReason` | string 或 null | 是 | 驳回原因。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### LeaderboardEntry

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `rank` | integer | 是 | 当前排名，从 `1` 开始。 |
| `accountId` | string | 是 | 考勤账户 ID。 |
| `memberId` | string | 是 | 成员 ID。 |
| `displayNameSnapshot` | string | 是 | 展示名快照。 |
| `avatarUrlSnapshot` | string 或 null | 是 | 头像快照。 |
| `memberGroupSnapshot` | string 或 null | 是 | 成员组快照。 |
| `scoreBalance` | integer | 是 | 当前余额。 |
| `totalEarned` | integer | 是 | 累计正向积分。 |
| `lastPositiveActivityAt` | string 或 null | 是 | 最近正向活跃。 |
| `profileSnapshotStale` | boolean | 是 | 是否使用旧快照。 |
| `generatedAt` | string | 是 | 榜单生成时间。 |

### AttendanceNotificationFailureSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | 固定为 `FAILED`。 |
| `failureCode` | string | 是 | `48030`、`48031` 或 `48032`。 |
| `failureType` | string | 是 | `UNAVAILABLE`、`TIMEOUT` 或 `BAD_SCHEMA`。 |
| `failureReason` | string | 是 | 脱敏失败摘要。 |
| `failedAt` | string | 是 | 失败发生时间。 |

### AttendanceAuditLog

审计字段继承公共契约，允许补充 `accountId`、`memberId`、`userId`、`ledgerId`、`contributionId`、`runId`、`candidateId`、`cycleKey`、`stateFrom`、`stateTo`、`delta`、`balanceBefore`、`balanceAfter`、`idempotencyKey`、`notificationStatus`、`dependencyStatus` 和 `profileSnapshotStale`。审计日志不得通过 attendance API 删除。

## 积分规则

P0 默认审核通过初始化为 `100` 分。月度无上线活跃、无工程贡献、无活动参与、无作品投稿、无协助管理记录时扣 `20` 分。积分最低为 `0`。积分小于等于 `0` 后，账户进入 `REMOVAL_CANDIDATE`，生成白名单移除候选和通知摘要。

所有积分变化必须有流水。任何直接改余额但没有流水的实现都不合格。管理员调整必须要求 `reason` 和 `publicReason`。撤销流水必须生成反向流水，不能直接删除原流水。月度扣分必须按 `cycleKey` 幂等，不能重复扣同一个账户同一个周期。

P0 的正向贡献可以由后台受控写入。未来接入 `activity`、`community`、`content`、`server-status` 或真实在线事件时，必须作为兼容变更补充契约、测试和适配器，不能绕过流水。

## attendance 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `45000` | 404 | 考勤账户不存在，或当前用户无权访问。 |
| `45001` | 404 | 积分流水不存在。 |
| `45002` | 404 | 贡献记录不存在。 |
| `45003` | 404 | 月度扣分运行不存在。 |
| `45004` | 404 | 白名单移除候选不存在。 |
| `45010` | 409 | whitelist 交接状态不允许初始化。 |
| `45011` | 409 | whitelist 交接快照已被消费。 |
| `45012` | 409 | 当前成员已存在考勤账户。 |
| `45013` | 409 | 当前账户状态不允许该操作。 |
| `45014` | 409 | 积分调整会造成非法余额。 |
| `45015` | 409 | 积分流水状态不允许撤销。 |
| `45016` | 409 | 月度周期已执行，不能重复扣分。 |
| `45017` | 409 | attendance 幂等键请求指纹冲突。 |
| `45018` | 409 | 候选状态不允许确认或驳回。 |
| `45019` | 409 | 贡献记录来源冲突或重复。 |
| `48000` | 502 | auth 认证上下文不可用。 |
| `48001` | 504 | auth 认证上下文调用超时。 |
| `48002` | 502 | auth 认证上下文字段或枚举不兼容 attendance 契约。 |
| `48010` | 502 | whitelist attendance 交接快照不可用。 |
| `48011` | 504 | whitelist attendance 交接快照调用超时。 |
| `48012` | 502 | whitelist attendance 交接快照字段不兼容 attendance 契约。 |
| `48020` | 502 | profile 成员校验或快照刷新不可用。 |
| `48021` | 504 | profile 成员校验或快照刷新超时。 |
| `48022` | 502 | profile 响应字段不兼容 attendance 契约。 |
| `48030` | 502 | notification 投递不可用。 |
| `48031` | 504 | notification 投递超时。 |
| `48032` | 502 | notification 投递响应不兼容 attendance 契约。 |
| `53000` | 500 | attendance 内部错误。 |
| `53001` | 500 | attendance 审计写入失败。 |
| `53002` | 500 | attendance 状态写入失败。 |
| `53003` | 500 | attendance 流水和余额写入不一致。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误和通用服务端错误优先使用公共错误码。attendance 自有幂等指纹冲突使用 `45017`。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开榜单 | GET | `/api/v1/attendance/leaderboard` | 否 | 游客 | LOW |
| 我的考勤账户 | GET | `/api/v1/attendance/me/account` | 是 | 当前用户 | LOW |
| 我的积分流水 | GET | `/api/v1/attendance/me/ledger` | 是 | 当前用户 | LOW |
| 我的贡献记录 | GET | `/api/v1/attendance/me/contributions` | 是 | 当前用户 | LOW |
| 我的榜单位置 | GET | `/api/v1/attendance/me/ranking` | 是 | 当前用户 | LOW |
| 后台账户列表 | GET | `/api/v1/attendance/admin/accounts` | 是 | HELPER、ADMIN、OWNER | LOW |
| 后台账户详情 | GET | `/api/v1/attendance/admin/accounts/{accountId}` | 是 | HELPER、ADMIN、OWNER | LOW |
| 消费 whitelist 初始化交接 | POST | `/api/v1/attendance/admin/initializations` | 是 | ADMIN、OWNER | MEDIUM |
| 管理员积分调整 | POST | `/api/v1/attendance/admin/accounts/{accountId}/adjustments` | 是 | ADMIN、OWNER | MEDIUM |
| 撤销积分流水 | POST | `/api/v1/attendance/admin/ledger/{ledgerId}/reverse` | 是 | ADMIN、OWNER | MEDIUM |
| 创建贡献记录 | POST | `/api/v1/attendance/admin/contributions` | 是 | ADMIN、OWNER | MEDIUM |
| 修正贡献记录 | PATCH | `/api/v1/attendance/admin/contributions/{contributionId}` | 是 | ADMIN、OWNER | MEDIUM |
| 月度扣分预检 | POST | `/api/v1/attendance/admin/monthly-runs/preview` | 是 | ADMIN、OWNER | MEDIUM |
| 执行月度扣分 | POST | `/api/v1/attendance/admin/monthly-runs` | 是 | ADMIN、OWNER | HIGH |
| 月度扣分运行详情 | GET | `/api/v1/attendance/admin/monthly-runs/{runId}` | 是 | ADMIN、OWNER | LOW |
| 白名单移除候选列表 | GET | `/api/v1/attendance/admin/removal-candidates` | 是 | HELPER、ADMIN、OWNER | LOW |
| 确认移除候选 | PATCH | `/api/v1/attendance/admin/removal-candidates/{candidateId}/confirm` | 是 | ADMIN、OWNER | HIGH |
| 驳回移除候选 | PATCH | `/api/v1/attendance/admin/removal-candidates/{candidateId}/dismiss` | 是 | ADMIN、OWNER | MEDIUM |
| 榜单重算 | POST | `/api/v1/attendance/admin/leaderboard/rebuild` | 是 | ADMIN、OWNER | MEDIUM |
| attendance 审计列表 | GET | `/api/v1/attendance/admin/audit-logs` | 是 | ADMIN、OWNER | LOW |
| attendance 自检摘要 | GET | `/api/v1/attendance/admin/ops/summary` | 是 | HELPER、ADMIN、OWNER | LOW |

## 公开和当前用户接口

### 公开榜单

`GET /api/v1/attendance/leaderboard`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `cycleKey` | string | 否 | 格式 `YYYY-MM`，为空时返回当前榜单。 |
| `memberGroup` | string | 否 | 成员组快照，最多 80 位。 |
| `sort` | string | 否 | 允许 `score_desc`、`earned_desc`、`lastActivity_desc`。默认 `score_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `LeaderboardEntry[]`。

业务规则：只展示 `ACTIVE` 和 `REMOVAL_CANDIDATE` 账户的公开字段。不得返回扣分原因全文、内部备注、审计参数、通知失败详情、管理员 ID 或候选确认状态。profile 不可用时可使用已保存快照降级，并标记 `profileSnapshotStale=true`。

### 我的考勤账户

`GET /api/v1/attendance/me/account`

成功响应 HTTP `200`，`data` 为当前用户 `AttendanceAccount`。当前用户尚未初始化时返回 `data=null`，不得自动创建账户。

业务规则：只能读取认证用户自己的账户。响应不得包含后台内部原因、候选确认备注、通知正文、完整 profile 失败详情或审计参数。

### 我的积分流水

`GET /api/v1/attendance/me/ledger`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `type` | string | 否 | 任一 `AttendanceLedgerType`。 |
| `cycleKey` | string | 否 | 格式 `YYYY-MM`。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`。 |

成功响应 HTTP `200`，分页 `items` 为当前用户可见的 `AttendanceLedgerEntry[]`。

业务规则：只返回当前用户流水。成员视图使用 `publicReason`，不得返回后台 `reason` 全文、操作者内部备注或通知失败详情。

### 我的贡献记录

`GET /api/v1/attendance/me/contributions`

查询参数同积分流水，额外支持 `type` 为 `ContributionType`。

成功响应 HTTP `200`，分页 `items` 为当前用户可见的 `ContributionRecord[]`。

业务规则：只返回当前用户贡献。后台修正历史可以显示为被修正摘要，但不得泄露内部备注。

### 我的榜单位置

`GET /api/v1/attendance/me/ranking`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "rank": 3,
    "totalRanked": 28,
    "entry": {
      "rank": 3,
      "accountId": "att_acc_001",
      "memberId": "mem_001",
      "displayNameSnapshot": "BeiMingPlayer",
      "avatarUrlSnapshot": null,
      "memberGroupSnapshot": "default",
      "scoreBalance": 120,
      "totalEarned": 140,
      "lastPositiveActivityAt": "2026-05-23T08:00:00Z",
      "profileSnapshotStale": false,
      "generatedAt": "2026-05-23T08:30:00Z"
    }
  }
}
```

业务规则：当前用户没有考勤账户时返回 `data=null`。

## 后台接口

### 后台账户列表

`GET /api/v1/attendance/admin/accounts`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配账户 ID、用户 ID、成员 ID、展示名或 Minecraft ID，最多 80 位。 |
| `status` | string | 否 | 任一 `AttendanceAccountStatus`。 |
| `reviewDirection` | string | 否 | 任一审核方向。 |
| `attemptType` | string | 否 | `FIRST_TIME` 或 `RECHECK`。 |
| `minScore` | integer | 否 | 最低余额，不能小于 `0`。 |
| `maxScore` | integer | 否 | 最高余额，不能小于 `minScore`。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`updatedAt_desc`、`score_desc`、`score_asc`、`lastActivity_desc`。 |

成功响应 HTTP `200`，分页 `items` 为后台视图 `AttendanceAccount[]`。

### 后台账户详情

`GET /api/v1/attendance/admin/accounts/{accountId}`

成功响应 HTTP `200`，`data` 包含 `AttendanceAccount`、最近流水、最近贡献、打开的候选和依赖摘要。账户不存在返回 `45000`。响应不得返回 token、完整请求头、通知正文、profile 内部堆栈、真实服务器命令或节点凭据。

### 消费 whitelist 初始化交接

`POST /api/v1/attendance/admin/initializations`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `applicationId` | string | 条件必填 | 白名单申请 ID。`applicationId` 和 `handoffId` 至少传一个。 |
| `handoffId` | string | 条件必填 | whitelist handoff ID，用于幂等校验。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `201`，`data` 为 `WhitelistAttendanceInitialization`，包含创建的 `AttendanceAccount` 和 `INITIAL_GRANT` 流水。重复消费同一 handoff 成功响应 HTTP `200`，返回同一初始化结果。

业务规则：初始化接口不能接受前端传入 `memberId`、`userId`、`scoreSummary`、`approvedAt` 等可信字段作为最终依据。必须从 whitelist handoff 读取，并用 profile 校验成员状态。初始化成功后创建账户、写入初始积分流水、记录 handoff 消费、尝试通知。审计失败时不得创建账户。通知失败不回滚主状态，但必须记录摘要。

### 管理员积分调整

`POST /api/v1/attendance/admin/accounts/{accountId}/adjustments`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `delta` | integer | 是 | 不能为 `0`，范围 `-1000` 到 `1000`。 |
| `publicReason` | string | 是 | 1 到 200 位，成员可见。 |
| `reason` | string | 是 | 1 到 500 位，后台原因。 |
| `sourceId` | string | 否 | 外部或手工来源 ID，默认生成。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为新 `AttendanceLedgerEntry` 和更新后的 `AttendanceAccount`。

业务规则：只允许 `ACTIVE`、`FROZEN` 和 `REMOVAL_CANDIDATE` 账户调整。扣分后最低余额为 `0`。余额变为 `0` 时必须生成或保持打开的移除候选。审计失败、流水写入失败或余额写入失败不得假装成功。

### 撤销积分流水

`POST /api/v1/attendance/admin/ledger/{ledgerId}/reverse`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `publicReason` | string | 是 | 1 到 200 位。 |
| `reason` | string | 是 | 1 到 500 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为反向 `REVERSAL` 流水和更新后的 `AttendanceAccount`。

业务规则：只允许撤销 `POSTED` 且未被撤销的流水。`INITIAL_GRANT` 是否允许撤销由实现固定，P0 推荐禁止并返回 `45015`。撤销不会删除原流水，原流水标记 `REVERSED` 并关联反向流水。

### 创建贡献记录

`POST /api/v1/attendance/admin/contributions`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `accountId` | string | 是 | 考勤账户 ID。 |
| `type` | string | 是 | 任一 `ContributionType`。 |
| `sourceModule` | string | 否 | P0 允许 `attendance`、`manual`。 |
| `sourceId` | string | 否 | 来源 ID，缺省生成。 |
| `title` | string | 是 | 2 到 80 位。 |
| `description` | string | 否 | 最多 1000 位。 |
| `occurredAt` | string | 是 | ISO 8601，不能晚于当前时间后 5 分钟。 |
| `scoreDelta` | integer | 是 | 范围 `0` 到 `1000`。为 `0` 时只记录贡献，不加分。 |
| `publicReason` | string | 否 | 加分时必填，1 到 200 位。 |
| `reason` | string | 是 | 1 到 500 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `201`，`data` 为 `ContributionRecord`，加分时同时返回关联流水。

业务规则：同一 `sourceModule`、`sourceId`、`accountId` 不能重复创建正向贡献，重复请求用幂等返回同一结果。未来其他模块接入前不得接受任意来源绕过后台权限。

### 修正贡献记录

`PATCH /api/v1/attendance/admin/contributions/{contributionId}`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `title` | string | 否 | 2 到 80 位。 |
| `description` | string | 否 | 最多 1000 位。 |
| `occurredAt` | string | 否 | ISO 8601。 |
| `publicReason` | string | 是 | 1 到 200 位。 |
| `reason` | string | 是 | 1 到 500 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为修正后的 `ContributionRecord`。修正不直接改历史积分，如需改分必须通过流水撤销和新调整完成。

### 月度扣分预检

`POST /api/v1/attendance/admin/monthly-runs/preview`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `cycleKey` | string | 是 | 格式 `YYYY-MM`。 |
| `deductionScore` | integer | 否 | 默认 `20`，范围 `1` 到 `100`。 |
| `reason` | string | 是 | 1 到 500 位。 |

成功响应 HTTP `200`，`data` 为 `MonthlyDeductionRun`，`dryRun=true`。预检只返回影响范围，不写积分流水，不改变账户状态，不生成候选。

### 执行月度扣分

`POST /api/v1/attendance/admin/monthly-runs`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `cycleKey` | string | 是 | 格式 `YYYY-MM`。 |
| `deductionScore` | integer | 否 | 默认 `20`，范围 `1` 到 `100`。 |
| `reason` | string | 是 | 1 到 500 位。 |
| `confirmText` | string | 是 | 二次确认文本，P0 固定要求 `RUN_MONTHLY_DEDUCTION`。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `201`，`data` 为 `MonthlyDeductionRun`。重复执行同一 `cycleKey` 返回 `45016`，同一幂等键同一请求体重复提交返回同一运行结果。

业务规则：该接口是 `HIGH` 风险。只扣 `ACTIVE` 或 `REMOVAL_CANDIDATE` 账户；当周期内无正向贡献、活动、作品或协助记录时扣分。扣分后余额小于等于 `0` 时生成或保持打开的移除候选。候选只代表建议，不调用 whitelist 移除接口，不执行服务器命令。

### 月度扣分运行详情

`GET /api/v1/attendance/admin/monthly-runs/{runId}`

成功响应 HTTP `200`，`data` 为 `MonthlyDeductionRun`。不存在返回 `45003`。

### 白名单移除候选列表

`GET /api/v1/attendance/admin/removal-candidates`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `status` | string | 否 | 任一 `RemovalCandidateStatus`。 |
| `cycleKey` | string | 否 | 格式 `YYYY-MM`。 |
| `keyword` | string | 否 | 匹配候选 ID、账户 ID、成员 ID、展示名。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`updatedAt_desc`、`score_asc`。 |

成功响应 HTTP `200`，分页 `items` 为 `RemovalCandidate[]`。

### 确认移除候选

`PATCH /api/v1/attendance/admin/removal-candidates/{candidateId}/confirm`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `publicReason` | string | 是 | 1 到 200 位。 |
| `reason` | string | 是 | 1 到 500 位。 |
| `confirmText` | string | 是 | 二次确认文本，P0 固定要求 `CONFIRM_REMOVAL_CANDIDATE`。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `RemovalCandidate` 和 `AttendanceAccount`。

业务规则：该接口是 `HIGH` 风险。确认后候选进入 `CONFIRMED`，账户保持或进入 `REMOVAL_CANDIDATE`，`recommendedAction=WHITELIST_REVIEW_REQUIRED`。P0 不调用 whitelist 移除接口，不执行服务器命令。

### 驳回移除候选

`PATCH /api/v1/attendance/admin/removal-candidates/{candidateId}/dismiss`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `publicReason` | string | 是 | 1 到 200 位。 |
| `reason` | string | 是 | 1 到 500 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `RemovalCandidate` 和 `AttendanceAccount`。

业务规则：只允许 `OPEN` 候选驳回。驳回后候选进入 `DISMISSED`。若账户余额仍为 `0`，账户可以保持 `REMOVAL_CANDIDATE`，但不能自动移除白名单。

### 榜单重算

`POST /api/v1/attendance/admin/leaderboard/rebuild`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `cycleKey` | string | 否 | 格式 `YYYY-MM`，为空表示当前榜单。 |
| `reason` | string | 是 | 1 到 500 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 包含 `rebuiltAt`、`entriesTotal` 和前 `20` 条 `LeaderboardEntry`。

### attendance 审计列表

`GET /api/v1/attendance/admin/audit-logs`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `accountId` | string | 否 | 考勤账户 ID。 |
| `memberId` | string | 否 | 成员 ID。 |
| `actorUserId` | string | 否 | 操作者用户 ID。 |
| `action` | string | 否 | 动作，最多 80 位。 |
| `result` | string | 否 | `SUCCESS` 或 `FAILED`。 |
| `from` | string | 否 | ISO 8601 起始时间。 |
| `to` | string | 否 | ISO 8601 结束时间。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`。默认 `createdAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `AttendanceAuditLog[]`。审计日志不得通过 attendance API 删除，返回结果必须脱敏。

### attendance 自检摘要

`GET /api/v1/attendance/admin/ops/summary`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "attendance",
    "port": 8111,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "whitelistMode": "TEST_STUB",
    "profileMode": "TEST_STUB",
    "notificationMode": "TEST_STUB",
    "testControlsEnabled": false,
    "accountsTotal": 10,
    "activeAccountsTotal": 8,
    "removalCandidatesOpenTotal": 1,
    "monthlyRunsTotal": 2,
    "ledgerEntriesTotal": 30,
    "contributionsTotal": 12,
    "auditsTotal": 80,
    "idempotencyRecordsTotal": 18,
    "lastMonthlyRunAt": "2026-05-23T12:00:00Z",
    "lastAuditAt": "2026-05-23T12:05:00Z",
    "productionGaps": [
      "P0_IN_MEMORY_STORAGE",
      "P0_AUTH_STUB",
      "P0_WHITELIST_STUB",
      "P0_PROFILE_STUB",
      "P0_NOTIFICATION_STUB",
      "TEST_CONTROLS_DISABLED_OUTSIDE_TEST",
      "REAL_ACTIVITY_EVENTS_NOT_CONNECTED",
      "REAL_ONLINE_TIME_NOT_CONNECTED",
      "WHITELIST_REMOVAL_NOT_CONNECTED"
    ]
  }
}
```

业务规则：自检摘要用于后台确认 attendance 当前运行模式、账户规模、扣分任务状态、移除候选数量、测试控制头开关和生产化缺口。摘要不得返回 token、请求头、通知正文、审计参数全文、真实服务器命令、节点凭据或异常堆栈。

## 状态、幂等和并发

考勤账户初始化成功后进入 `ACTIVE`。管理员可以把账户冻结为 `FROZEN` 或未来兼容变更归档，但 P0 不提供冻结写接口。扣分后余额为 `0` 时进入 `REMOVAL_CANDIDATE`。确认候选不直接进入 `REMOVED`，只有未来 whitelist 正式移除回传或兼容变更才允许进入 `REMOVED`。`ARCHIVED` 为终态。

状态推进只能由服务端根据 whitelist handoff、profile 校验、积分流水、月度扣分、候选动作、权限和二次确认判断。非法状态跳跃返回 `45013`。浏览器传入可信字段必须忽略或返回字段校验失败。

初始化、积分调整、流水撤销、贡献创建、贡献修正、月度扣分执行、候选确认、候选驳回和榜单重算支持 `idempotencyKey`。同一操作者、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `45017`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序。

并发初始化同一 handoff 只能产生一个账户和一条初始流水。并发调整同一账户必须串行化余额计算，不能出现流水余额断裂。并发月度扣分同一 `cycleKey` 只能有一个成功运行。读取接口允许读到更新前或更新后的完整状态，不能返回半更新对象。

## 审计要求

必须审计的动作包括初始化考勤账户、初始化 handoff 重放、初始化失败、管理员积分调整、流水撤销、贡献记录创建、贡献记录修正、月度扣分预检、月度扣分执行、扣分任务部分失败、候选生成、候选确认、候选驳回、榜单重算、通知失败、依赖降级、自检读取、审计写入失败和状态写入失败。

后台写操作必须记录 `reason`、操作者、目标对象、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计不得泄露 token、完整请求头、Minecraft 验证凭据、profile 后台备注全文、通知正文全文、真实服务器命令、节点凭据、内部异常堆栈或前序服务内部路径。

审计写入失败时，初始化、积分调整、流水撤销、贡献创建、贡献修正、月度扣分、候选确认、候选驳回和榜单重算不得假装成功，必须返回 `53001` 或 `53000`，并保持业务数据不变。

## 失败降级

auth 是所有登录接口强依赖。auth 不可用、超时、用户状态不允许或字段不兼容时不得伪造成功。

whitelist 是初始化强依赖。交接快照不可用、未通过、已消费、字段不兼容、用户或成员缺失时不得创建考勤账户。

profile 是初始化强依赖，也是只读展示的辅助刷新依赖。初始化时 profile 失败不得创建账户。榜单和详情读取时 profile 刷新失败可以使用已有快照降级，但必须标记 `profileSnapshotStale=true` 并写入依赖降级审计或计数。

notification 是辅助依赖。通知失败不得回滚初始化、积分调整、流水撤销、月度扣分、候选确认或候选驳回，但必须记录失败摘要和审计。

流水写入和余额写入必须保持一致。任何半成功风险都必须返回 `53003` 或进入可复核失败状态，不能出现余额变化但没有流水，或流水成功但余额未更新却返回成功。

## 验收口径

`attendance` API 文档按 `docs/contracts-attendance.md` 独立存在，并由 `.local-docs/tests-attendance.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`attendance` 完成时必须满足以下条件：全部接口按本文档实现；当前用户接口只能访问自己的账户、流水、贡献和排名；后台接口按角色限制；初始化只通过 whitelist handoff 和 profile 正式适配读取快照，不直接读前序服务实现；所有积分变化都有流水；月度扣分按 `cycleKey` 幂等；移除候选只生成建议，不执行真实 whitelist 移除或服务器命令；通知失败按辅助降级记录；端口固定为 `8111`；`.local-docs/tests-attendance.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 attendance 全部测试通过；auth、profile、notification、content、server-status、resource、admin、onboarding、exam 和 whitelist 前序服务回归测试通过；没有修改前序服务稳定接口；没有把社区、活动、日历、更新日志、后台聚合、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复或 Cloudreve 管理能力塞进 attendance。

生产化硬化验收还必须满足：测试控制头默认关闭，只有本地自动化测试显式启用时才生效；关闭状态下依赖失败模拟头、写入失败模拟头和通知失败模拟头全部被忽略；自检摘要明确返回当前测试控制头开关状态。
