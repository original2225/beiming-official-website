# 北冥官网 whitelist API 契约

版本：0.2

## 文档定位

本文档是 `whitelist` 微服务的正式 API 契约。后续 `attendance`、`community`、`activity`、`calendar`、`changelog`、前端适配、`admin` 聚合、`ops-control` 和 `node-daemon` 只能通过本文档定义的接口读取白名单申请、审核结果、移除记录、审计和考勤初始化交接摘要，不能直接读取或修改 `whitelist` 数据库，也不能把白名单审核逻辑塞进其他服务。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `whitelist` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了现有优秀平台的流程设计。Discord Rules Screening 和 Community Onboarding 强调新成员先完成明确规则门槛，再进入后续流程。Atlassian Jira Workflow 强调状态、流转、条件、校验器和后置动作分离。Moodle Quiz 和 Google Forms Quiz 的考试流程证明考试结果应由考试模块冻结并交接，白名单模块只消费通过结果。Minecraft 和 Spigot 的白名单资料说明白名单最终会影响真实服务器准入，但真实服务器文件、命令、reload 和节点操作不属于本服务 P0 边界。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Discord Rules Screening FAQ](https://support.discord.com/hc/en-us/articles/1500000466882-Rules-Screening-FAQ) | 规则确认是进入社区前的明确门槛，不应只靠前端提示。 |
| [Discord Community Onboarding FAQ](https://support.discord.com/hc/en-us/articles/11074987197975-Community-Onboarding-FAQ) | 入门流程需要分步、可追溯，并根据成员选择进入后续路径。 |
| [Atlassian Jira workflows overview](https://www.atlassian.com/software/jira/guides/workflows/overview) | 审核流程应由状态和单向流转组成，非法跳转必须拒绝。 |
| [Atlassian workflow validators](https://support.atlassian.com/jira-cloud-administration/docs/use-workflow-validators-with-custom-fields/) | 状态推进前必须做字段、权限和条件校验。 |
| [Moodle Quiz activity](https://docs.moodle.org/en/Quiz) | 考试结果和人工阅卷由考试模块负责，白名单只消费冻结结果。 |
| [Google Forms quizzes](https://support.google.com/docs/answer/7032287) | 自动评分和人工审核后的结果发布应可追溯。 |
| [SpigotMC whitelist commands](https://www.spigotmc.org/wiki/spigot-commands-and-permissions/) | 真实 Minecraft 白名单写入是服务器操作，不属于申请审核服务。 |

## 职责边界

`whitelist` 负责考试通过后的白名单申请、申请材料、补充材料、审核分配、审核通过、审核拒绝、要求补充、撤回、移除白名单、允许重新申请、二次入服标记、给 `attendance` 的初始化交接摘要、幂等记录和自身审计。

`whitelist` 不负责注册、登录、邀请码、会话、账号角色能力点、Minecraft 绑定主数据、入服前置流程、题库、试卷、判分、人工阅卷、成员档案主数据、站内通知主数据、考勤积分主数据、社区工单、服务器状态展示、资源下载、后台聚合入口、真实 Minecraft 服务器白名单文件写入、控制台命令、节点守护进程、容器、终端、文件管理、备份恢复或 Cloudreve 管理。

`whitelist` 只能适配前序服务。它通过 `auth` 认证上下文读取当前用户和后台操作者，通过 `exam` 的 whitelist 交接快照创建申请，通过 `profile` 正式接口创建或激活成员档案、移除后更新成员状态，通过 `notification` 投递站内通知，通过保存的只读快照和后续接口给 `attendance` 提供初始化材料。它不能要求前序服务反向写入 whitelist 状态，不能导入前序服务内存存储、实体、Repository、测试种子或内部类。

## 数据归属

`whitelist` 拥有以下主数据：白名单申请、申请材料、补充材料、审核状态、审核意见、审核分配、移除记录、允许重新申请记录、profile 调用摘要、notification 调用摘要、attendance 初始化交接摘要、幂等记录和 whitelist 审计日志。

`whitelist` 可以保存来自 `exam` 的 `sessionId`、`applicationId`、`handoffVersion`、`onboardingHandoffVersion`、`userId`、`minecraftBindingSnapshot`、`reviewDirection`、`attemptType`、`result`、`scoreSummary`、`passedAt` 和 `reviewerSnapshot` 快照。它可以保存来自 `profile` 的成员激活或状态变更结果摘要，可以保存来自 `notification` 的投递结果摘要。快照不是来源模块主数据，不能用于反写来源模块。

## 基础路径与认证

所有接口默认使用 `/api/v1/whitelist` 前缀。第二批合并后当前运行入口由 `admission-core-service` 承载，端口固定为 `8131`。历史原服务端口 `8110` 只作为 `legacyPort` 返回，不作为当前运行入口、网关上游或测试入口。

当前用户接口使用 `/api/v1/whitelist/me` 前缀，全部要求 `Authorization: Bearer <token>`，只能访问当前认证用户自己的白名单申请。浏览器请求体不得传入 `userId`、`roles`、`permissions`、`minecraftBindingSnapshot`、`examResult`、`scoreSummary`、`reviewerId`、`reviewerSnapshot`、`status`、`profileStatus`、`attendanceStatus`、`createdBy`、`updatedBy` 等服务端可信字段。

后台接口使用 `/api/v1/whitelist/admin` 前缀，全部要求登录。后台读取申请列表和详情要求 `HELPER`、`ADMIN` 或 `OWNER`。审核通过、审核拒绝、要求补充、移除、重开、attendance handoff 读取、审计读取和自检摘要要求 `ADMIN` 或 `OWNER`。`HELPER` 可以查看审核队列和详情，可以领取或分配给自己做初审记录，但不能执行最终通过、拒绝、移除或重开。

## 本地测试控制头

`whitelist` 允许在本地自动化测试中使用 `X-Test-Profile-Mode`、`X-Test-Notification-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Fail-After-Profile` 模拟依赖失败、通知失败、审计失败、状态写入失败和 profile 激活后补偿失败。该能力只服务测试闭环，不属于正式业务 API。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、状态失败、通知失败或补偿失败。只有 `whitelist.test-controls.enabled=true` 时，自动化测试才能启用这些请求头。

## 网关可信身份上下文

经 `api-gateway` 访问时，`whitelist` 可以优先读取网关注入的可信身份头。只有 `X-Gateway-Internal-Request-Id` 存在时，才进入可信上下文解析；若该头缺失，即使请求带有 `X-Beiming-Actor-*`，也必须忽略这些头并继续走 `Authorization: Bearer <token>` 兼容路径。可信上下文缺少 `X-Beiming-Actor-User-Id`、角色枚举不兼容或字段无法解析时返回 HTTP `502` 和 `47002`，不得静默降级成匿名用户。

## 前序服务兼容契约

`auth` 是强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions`、`status` 和 `minecraftBinding`。用户状态为 `PENDING_PROFILE` 或 `ACTIVE` 时可创建、补充、撤回和读取自己的申请；`DISABLED`、`BANNED`、`DELETED` 不允许写入。auth 不可用返回 `47000`，auth 超时返回 `47001`，字段或枚举不兼容返回 `47002`。

`exam` 是创建申请的强依赖。创建白名单申请时必须读取 `GET /api/v1/exams/admin/sessions/{sessionId}/whitelist-handoff` 或未来等价服务间适配器。只有交接快照满足 `result=PASSED`、`passedAt` 不为空、`userId` 与当前用户一致、`minecraftBindingSnapshot` 完整、`handoffVersion` 未被当前 whitelist 申请消费过时，才允许创建申请。exam 不可用返回 `47010`，超时返回 `47011`，字段不兼容返回 `47012`，交接状态不允许返回 `44010`。

当用户历史申请已被移除或重开为 `REAPPLYING` 时，新建申请必须消费 `attemptType=RECHECK` 的 exam handoff。再次使用 `FIRST_TIME` handoff 创建二次入服申请必须返回 `44019` 或 `44010`，实现固定为 `44019`，并不得创建申请。

`profile` 是审核通过和移除白名单的强依赖。审核通过时必须通过 `POST /api/v1/profile/admin/members/activate` 或未来等价服务间适配器创建或激活成员档案，并保存调用结果摘要。移除白名单时必须通过 `PATCH /api/v1/profile/admin/members/{memberId}/status` 或未来等价适配器把成员状态流转为 `REMOVED` 或契约允许的目标状态。profile 不可用返回 `47020`，超时返回 `47021`，字段不兼容返回 `47022`，成员激活冲突返回 `44020`。profile 激活失败时申请不得进入 `APPROVED`，必须进入 `APPROVAL_BLOCKED` 或保持可复核状态。

`notification` 用于投递申请提交、要求补充、补充提交、审核通过、审核拒绝、移除和允许重新申请通知。P0 中通知是辅助依赖，失败不得回滚主状态，但必须保存 `notificationStatus=FAILED`、失败原因摘要并写入审计。notification 不可用记录或返回 `47030`，超时记录或返回 `47031`，字段不兼容记录或返回 `47032`。

通知失败摘要必须能区分不可用、超时和字段不兼容三类失败。当前用户接口和后台接口都可以返回 `notificationFailure` 脱敏摘要，便于前端展示和后台排障；摘要不得包含通知正文、完整请求头、认证 token、内部 URL 或异常堆栈。

`attendance` 当前未实现。审核通过后 `whitelist` 不得伪造积分初始化成功，只能生成 `attendanceInitializationStatus=WAITING_MODULE` 的交接摘要。后续 `attendance` 开发时只能通过本文档的 attendance handoff 接口或未来正式服务间接口消费摘要，不能直接读取 whitelist 数据库。

`onboarding` 不是白名单主数据来源。`whitelist` 可以保存 exam 快照中携带的 onboarding `applicationId` 和 `onboardingHandoffVersion`，但不能要求 onboarding 回写白名单状态。

`admin` 当前只是聚合入口。本文档不修改 `admin` 契约。若后续需要 admin 展示白名单待办、指标或审计索引，必须作为 admin 兼容变更单独走契约、测试、红灯、实现和回归闭环。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `WhitelistApplicationStatus` | `DRAFT`、`PENDING_REVIEW`、`UNDER_REVIEW`、`NEEDS_SUPPLEMENT`、`SUPPLEMENT_SUBMITTED`、`APPROVAL_BLOCKED`、`APPROVED`、`REJECTED`、`WITHDRAWN`、`REMOVED`、`REAPPLYING`、`ARCHIVED` | 白名单申请状态。P0 创建后默认进入 `PENDING_REVIEW`，保留 `DRAFT` 兼容前端草稿。 |
| `WhitelistResult` | `PENDING`、`NEEDS_SUPPLEMENT`、`APPROVED`、`REJECTED`、`WITHDRAWN`、`REMOVED` | 对外归一结果。 |
| `ReviewDirection` | `REDSTONE`、`LATE_GAME`、`BUILDING`、`GENERAL` | 审核方向，必须兼容 onboarding 和 exam。 |
| `WhitelistAttemptType` | `FIRST_TIME`、`RECHECK` | 首次入服或二次考核，来自 exam。 |
| `NotificationStatus` | `DELIVERED`、`FAILED`、`SKIPPED` | 最近一次通知摘要。 |
| `ProfileActivationStatus` | `PENDING`、`ACTIVATED`、`FAILED`、`SKIPPED` | profile 激活摘要。 |
| `AttendanceInitializationStatus` | `WAITING_MODULE`、`READY_FOR_CONSUME`、`CONSUMED`、`FAILED` | P0 审核通过后固定为 `WAITING_MODULE` 或 `READY_FOR_CONSUME`，不得返回已初始化积分。 |
| `WhitelistAuditResult` | `SUCCESS`、`FAILED` | whitelist 审计结果。 |

## 通用对象

### WhitelistApplication

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `applicationId` | string | 是 | 白名单申请 ID。 |
| `examSessionId` | string | 是 | 来源考试实例 ID。 |
| `onboardingApplicationId` | string | 是 | 来源 onboarding 流程实例 ID。 |
| `examHandoffVersion` | integer | 是 | 来源 exam 交接版本。 |
| `onboardingHandoffVersion` | integer | 是 | 来源 onboarding 交接版本。 |
| `userId` | string | 是 | auth 用户 ID。当前用户接口中固定为认证用户。 |
| `displayNameSnapshot` | string | 是 | 创建申请时的展示名快照。 |
| `minecraftBindingSnapshot` | object | 是 | Minecraft 绑定快照，兼容 auth 的 `MinecraftBinding`。 |
| `reviewDirection` | string | 是 | 审核方向。 |
| `attemptType` | string | 是 | 首次或二次考核。 |
| `status` | string | 是 | `WhitelistApplicationStatus`。 |
| `result` | string | 是 | `WhitelistResult`。 |
| `materials` | WhitelistMaterial[] | 是 | 申请材料。 |
| `scoreSummary` | object | 是 | exam 成绩摘要。 |
| `examPassedAt` | string | 是 | exam 通过时间。 |
| `reviewerUserId` | string 或 null | 是 | 当前审核人。 |
| `reviewerDisplayNameSnapshot` | string 或 null | 是 | 审核人展示名快照。 |
| `reviewComment` | string 或 null | 是 | 给申请人的审核意见。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注，当前用户接口不得返回。 |
| `supplementRequest` | WhitelistSupplementRequest 或 null | 是 | 最近一次补充要求。 |
| `profileActivation` | WhitelistProfileActivationSummary 或 null | 是 | 成员档案激活摘要。 |
| `attendanceHandoff` | WhitelistAttendanceHandoffSnapshot 或 null | 后台可见 | 给 attendance 的初始化交接摘要。当前用户结果只返回状态摘要。 |
| `notificationStatus` | string 或 null | 是 | 最近一次通知结果。 |
| `notificationFailure` | WhitelistNotificationFailureSummary 或 null | 是 | 最近一次通知失败脱敏摘要。通知成功时为 `null`。 |
| `removedAt` | string 或 null | 是 | 移除时间。 |
| `removedBy` | string 或 null | 是 | 移除操作者。 |
| `removalReason` | string 或 null | 后台可见 | 移除原因。当前用户结果只返回公开说明。 |
| `reapplyRequired` | boolean | 是 | 是否需要重新申请。 |
| `nextExamAttemptType` | string 或 null | 是 | 移除后建议的下一次考试类型。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `submittedAt` | string 或 null | 是 | 提交审核时间。 |
| `reviewedAt` | string 或 null | 是 | 最近审核时间。 |
| `approvedAt` | string 或 null | 是 | 审核通过时间。 |
| `rejectedAt` | string 或 null | 是 | 审核拒绝时间。 |
| `withdrawnAt` | string 或 null | 是 | 撤回时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |

### WhitelistMaterial

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `materialId` | string | 是 | 材料 ID。 |
| `type` | string | 是 | `TEXT`、`LINK`、`IMAGE` 或 `OTHER`。P0 可只接受 `TEXT` 和 `LINK`。 |
| `title` | string | 是 | 2 到 80 位。 |
| `content` | string | 是 | 1 到 2000 位。链接必须为 http、https 或站内路径。 |
| `publicVisibleToApplicant` | boolean | 是 | 是否对申请人可见。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### WhitelistSupplementRequest

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `requestId` | string | 是 | 补充要求 ID。 |
| `publicComment` | string | 是 | 给申请人的补充说明，1 到 1000 位。 |
| `dueAt` | string 或 null | 是 | 补充截止时间，必须晚于当前时间。 |
| `requestedBy` | string | 是 | 操作者用户 ID。 |
| `requestedAt` | string | 是 | 要求补充时间。 |
| `submittedAt` | string 或 null | 是 | 用户提交补充时间。 |
| `materials` | WhitelistMaterial[] | 是 | 本次补充材料。 |

### WhitelistProfileActivationSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | `PENDING`、`ACTIVATED`、`FAILED` 或 `SKIPPED`。 |
| `memberId` | string 或 null | 是 | profile 成员档案 ID。 |
| `profileStatus` | string 或 null | 是 | profile 返回的成员状态。 |
| `calledAt` | string 或 null | 是 | 调用 profile 时间。 |
| `failureCode` | string 或 null | 是 | 失败码摘要。 |
| `failureReason` | string 或 null | 是 | 脱敏失败原因。 |

### WhitelistNotificationFailureSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | 固定为 `FAILED`。 |
| `failureCode` | string | 是 | `47030`、`47031` 或 `47032`。 |
| `failureType` | string | 是 | `UNAVAILABLE`、`TIMEOUT` 或 `BAD_SCHEMA`。 |
| `failureReason` | string | 是 | 脱敏失败摘要，不得包含通知正文、请求头、token 或堆栈。 |
| `failedAt` | string | 是 | 失败发生时间。 |

### WhitelistAttendanceHandoffSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `handoffId` | string | 是 | attendance 交接 ID。 |
| `applicationId` | string | 是 | 白名单申请 ID。 |
| `userId` | string | 是 | 用户 ID。 |
| `memberId` | string | 是 | profile 成员档案 ID。 |
| `minecraftBindingSnapshot` | object | 是 | Minecraft 绑定快照。 |
| `reviewDirection` | string | 是 | 审核方向。 |
| `attemptType` | string | 是 | 首次或二次考核。 |
| `approvedAt` | string | 是 | 白名单通过时间。 |
| `scoreSummary` | object | 是 | exam 成绩摘要。 |
| `initializationStatus` | string | 是 | P0 固定为 `WAITING_MODULE` 或 `READY_FOR_CONSUME`。 |
| `handoffVersion` | integer | 是 | 交接版本，从 `1` 开始。 |
| `generatedAt` | string | 是 | 生成时间。 |
| `consumedAt` | string 或 null | 是 | P0 固定为 `null`。 |

### WhitelistAuditLog

审计字段继承公共契约，允许补充 `applicationId`、`examSessionId`、`stateFrom`、`stateTo`、`reviewDirection`、`attemptType`、`idempotencyKey`、`notificationStatus`、`profileActivationStatus`、`attendanceHandoffStatus` 和 `dependencyStatus`。审计日志不得通过 whitelist API 删除。

## whitelist 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `44000` | 404 | 白名单申请不存在，或当前用户无权访问。 |
| `44001` | 404 | 补充要求不存在。 |
| `44002` | 404 | 白名单审计记录不存在。 |
| `44010` | 409 | exam 交接状态不允许创建白名单申请。 |
| `44011` | 409 | 当前用户与 exam 交接用户不一致。 |
| `44012` | 409 | exam 交接快照已被消费。 |
| `44013` | 409 | 当前用户已有未归档白名单申请。 |
| `44014` | 409 | 当前申请状态不允许该操作。 |
| `44015` | 409 | 申请材料不完整或不允许编辑。 |
| `44016` | 409 | 补充材料不满足要求。 |
| `44017` | 409 | whitelist 幂等键请求指纹冲突。 |
| `44018` | 409 | 当前申请已产生下游结果，不能撤回或重复审核。 |
| `44019` | 409 | 当前申请已移除，必须走重新申请。 |
| `44020` | 409 | profile 激活冲突，白名单不能进入通过终态。 |
| `44021` | 409 | attendance 交接摘要尚未生成。 |
| `44022` | 409 | 当前申请不允许重开或重新申请。 |
| `47000` | 502 | auth 认证上下文不可用。 |
| `47001` | 504 | auth 认证上下文调用超时。 |
| `47002` | 502 | auth 认证上下文字段或枚举不兼容 whitelist 契约。 |
| `47010` | 502 | exam whitelist 交接快照不可用。 |
| `47011` | 504 | exam whitelist 交接快照调用超时。 |
| `47012` | 502 | exam whitelist 交接快照字段不兼容 whitelist 契约。 |
| `47020` | 502 | profile 成员激活或状态变更不可用。 |
| `47021` | 504 | profile 成员激活或状态变更调用超时。 |
| `47022` | 502 | profile 响应字段不兼容 whitelist 契约。 |
| `47030` | 502 | notification 投递不可用。 |
| `47031` | 504 | notification 投递超时。 |
| `47032` | 502 | notification 投递响应不兼容 whitelist 契约。 |
| `52000` | 500 | whitelist 内部错误。 |
| `52001` | 500 | whitelist 审计写入失败。 |
| `52002` | 500 | whitelist 状态写入失败。 |
| `52003` | 500 | whitelist 下游补偿状态写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误和通用服务端错误优先使用公共错误码。whitelist 自有幂等指纹冲突使用 `44017`。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 创建白名单申请 | POST | `/api/v1/whitelist/me/applications` | 是 | 当前用户 | LOW |
| 当前申请 | GET | `/api/v1/whitelist/me/applications/current` | 是 | 当前用户 | LOW |
| 当前用户申请历史 | GET | `/api/v1/whitelist/me/applications` | 是 | 当前用户 | LOW |
| 当前用户申请详情 | GET | `/api/v1/whitelist/me/applications/{applicationId}` | 是 | 当前用户 | LOW |
| 修改申请材料 | PATCH | `/api/v1/whitelist/me/applications/{applicationId}` | 是 | 当前用户 | LOW |
| 提交审核 | POST | `/api/v1/whitelist/me/applications/{applicationId}/submit` | 是 | 当前用户 | LOW |
| 提交补充材料 | PATCH | `/api/v1/whitelist/me/applications/{applicationId}/supplement` | 是 | 当前用户 | LOW |
| 撤回申请 | PATCH | `/api/v1/whitelist/me/applications/{applicationId}/withdraw` | 是 | 当前用户 | MEDIUM |
| 读取审核结果 | GET | `/api/v1/whitelist/me/applications/{applicationId}/result` | 是 | 当前用户 | LOW |
| 后台申请列表 | GET | `/api/v1/whitelist/admin/applications` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台申请详情 | GET | `/api/v1/whitelist/admin/applications/{applicationId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 分配审核人 | PATCH | `/api/v1/whitelist/admin/applications/{applicationId}/assign` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 要求补充 | PATCH | `/api/v1/whitelist/admin/applications/{applicationId}/request-supplement` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核通过 | PATCH | `/api/v1/whitelist/admin/applications/{applicationId}/approve` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/whitelist/admin/applications/{applicationId}/reject` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 移除白名单 | PATCH | `/api/v1/whitelist/admin/applications/{applicationId}/remove` | 是 | `ADMIN` 或 `OWNER` | HIGH |
| 允许重新申请 | POST | `/api/v1/whitelist/admin/applications/{applicationId}/reopen` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| attendance 交接摘要 | GET | `/api/v1/whitelist/admin/applications/{applicationId}/attendance-handoff` | 是 | `ADMIN` 或 `OWNER` | LOW |
| whitelist 审计列表 | GET | `/api/v1/whitelist/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| whitelist 自检摘要 | GET | `/api/v1/whitelist/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 当前用户接口

### 创建白名单申请

`POST /api/v1/whitelist/me/applications`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `examSessionId` | string | 是 | 已通过考试实例 ID。 |
| `materials` | array | 否 | 创建时提交的材料，最多 20 条。P0 可为空。 |
| `publicComment` | string | 否 | 申请说明，最多 1000 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `WhitelistApplication`。如果同一用户同一通过考试已经存在未归档申请，返回 HTTP `200` 和现有申请，除非已有终态要求重开。

业务规则：服务端必须从 exam 读取 whitelist handoff，不能信任浏览器传入的考试结果。只允许 `result=PASSED` 创建申请。交接用户必须等于当前用户。Minecraft 绑定快照必须完整。一个 exam handoff 只能消费一次。创建后默认进入 `PENDING_REVIEW`，`result=PENDING`。创建成功后尝试发送通知，通知失败不回滚。

幂等规则：同一用户、同一 `idempotencyKey`、同一请求体重复提交时返回同一申请。相同幂等键搭配不同请求体返回 `44017`。并发创建同一 exam handoff 只能成功产生一个申请。

审计要求：成功写入 `WHITELIST_APPLICATION_CREATED`。exam 交接失败、用户不匹配、交接已消费和审计失败都必须可追踪。审计失败返回 `52001`，不得创建申请。

### 当前申请

`GET /api/v1/whitelist/me/applications/current`

成功响应 HTTP `200`，`data` 为当前未归档、未撤回且仍可见的 `WhitelistApplication`，没有当前申请时 `data=null`。

业务规则：只返回当前用户自己的申请。`APPROVED`、`REJECTED`、`REMOVED` 可按最新结果作为当前结果返回，`ARCHIVED` 不作为当前申请。当前用户视图不得返回 `internalNote`、完整移除原因、审计参数、profile 失败堆栈或 notification 正文。

### 当前用户申请历史

`GET /api/v1/whitelist/me/applications`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `status` | string | 否 | 任一 `WhitelistApplicationStatus`。 |
| `result` | string | 否 | 任一 `WhitelistResult`。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`updatedAt_desc`、`reviewedAt_desc`、`approvedAt_desc`。默认 `createdAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为当前用户可见的 `WhitelistApplication[]`。

### 当前用户申请详情

`GET /api/v1/whitelist/me/applications/{applicationId}`

成功响应 HTTP `200`，`data` 为当前用户视图的 `WhitelistApplication`。申请不存在、已归档不可见或不属于当前用户返回 `44000`，不得暴露他人申请是否存在。

### 修改申请材料

`PATCH /api/v1/whitelist/me/applications/{applicationId}`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `materials` | array | 是 | 0 到 20 条，整体替换当前可编辑材料。 |
| `publicComment` | string | 否 | 申请说明，最多 1000 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只允许 `DRAFT`、`PENDING_REVIEW`、`NEEDS_SUPPLEMENT` 和 `SUPPLEMENT_SUBMITTED` 的当前用户申请修改材料。`UNDER_REVIEW` 是否允许修改由实现固定，推荐返回 `44014`。`APPROVED`、`REJECTED`、`WITHDRAWN`、`REMOVED`、`ARCHIVED` 不允许修改。浏览器不得传入可信状态字段。

审计要求：成功写入 `WHITELIST_MATERIALS_UPDATED`。

### 提交审核

`POST /api/v1/whitelist/me/applications/{applicationId}/submit`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：`DRAFT` 可提交到 `PENDING_REVIEW`。P0 创建后已经是 `PENDING_REVIEW` 时重复提交返回成功并保持幂等。`NEEDS_SUPPLEMENT` 不允许走 submit，必须走 supplement。终态申请返回 `44014` 或 `44018`。

### 提交补充材料

`PATCH /api/v1/whitelist/me/applications/{applicationId}/supplement`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `materials` | array | 是 | 1 到 20 条补充材料。 |
| `publicComment` | string | 否 | 补充说明，最多 1000 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只有 `NEEDS_SUPPLEMENT` 可提交补充。成功后进入 `SUPPLEMENT_SUBMITTED`，`result=PENDING`。补充截止已过时返回 `44014` 或模块固定错误。通知失败不回滚。

审计要求：成功写入 `WHITELIST_SUPPLEMENT_SUBMITTED`。

### 撤回申请

`PATCH /api/v1/whitelist/me/applications/{applicationId}/withdraw`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位，撤回原因。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只允许撤回 `DRAFT`、`PENDING_REVIEW`、`NEEDS_SUPPLEMENT` 和 `SUPPLEMENT_SUBMITTED`。`UNDER_REVIEW` 可否撤回由实现固定，推荐返回 `44014`。已通过、已拒绝、已移除、已归档或已产生 profile 激活结果的申请不得撤回。

审计要求：成功写入 `WHITELIST_APPLICATION_WITHDRAWN`。

### 读取审核结果

`GET /api/v1/whitelist/me/applications/{applicationId}/result`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "applicationId": "wl_xxx",
    "status": "APPROVED",
    "result": "APPROVED",
    "reviewComment": "审核通过",
    "profileActivationStatus": "ACTIVATED",
    "attendanceInitializationStatus": "WAITING_MODULE",
    "notificationStatus": "DELIVERED",
    "reviewedAt": "2026-05-23T12:00:00Z"
  }
}
```

业务规则：只返回当前用户自己的结果摘要。不得返回内部备注、profile 失败堆栈、审计参数、通知正文、考题答案或后台移除原因全文。

## 后台接口

### 后台申请列表

`GET /api/v1/whitelist/admin/applications`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配申请 ID、用户 ID、展示名、Minecraft ID 或 examSessionId，最多 80 位。 |
| `status` | string | 否 | 任一 `WhitelistApplicationStatus`。 |
| `result` | string | 否 | 任一 `WhitelistResult`。 |
| `reviewDirection` | string | 否 | 任一 `ReviewDirection`。 |
| `attemptType` | string | 否 | `FIRST_TIME` 或 `RECHECK`。 |
| `reviewerUserId` | string | 否 | 审核人用户 ID。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`updatedAt_desc`、`submittedAt_desc`、`reviewedAt_desc`、`approvedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为后台视图 `WhitelistApplication[]`。

### 后台申请详情

`GET /api/v1/whitelist/admin/applications/{applicationId}`

成功响应 HTTP `200`，`data` 为后台视图 `WhitelistApplication`。申请不存在返回 `44000`。响应不得返回 token、完整请求头、Minecraft 验证凭据、考试正确答案、通知正文、profile 内部堆栈、真实服务器命令或节点凭据。

### 分配审核人

`PATCH /api/v1/whitelist/admin/applications/{applicationId}/assign`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reviewerUserId` | string | 否 | 目标审核人。为空时默认当前操作者。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只允许 `PENDING_REVIEW`、`SUPPLEMENT_SUBMITTED` 和 `UNDER_REVIEW` 分配审核人。成功后进入 `UNDER_REVIEW`。`HELPER` 只能把申请分配给自己，不能分配给其他人。`ADMIN` 和 `OWNER` 可分配给任一具备后台读取权限的用户。

审计要求：成功写入 `WHITELIST_REVIEW_ASSIGNED`。

### 要求补充

`PATCH /api/v1/whitelist/admin/applications/{applicationId}/request-supplement`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `publicComment` | string | 是 | 1 到 1000 位，说明需补充内容。 |
| `dueAt` | string | 否 | 必须晚于当前时间且不超过 14 天。 |
| `internalNote` | string | 否 | 最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只允许 `PENDING_REVIEW`、`UNDER_REVIEW` 和 `SUPPLEMENT_SUBMITTED`。成功后进入 `NEEDS_SUPPLEMENT`，`result=NEEDS_SUPPLEMENT`。通知失败不回滚，但必须记录。

`dueAt` 必须晚于服务端当前时间，且不得超过服务端当前时间后 14 天。超过 14 天、格式非法或早于当前时间时返回 `40001`，不得改变申请状态。

### 审核通过

`PATCH /api/v1/whitelist/admin/applications/{applicationId}/approve`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reviewComment` | string | 是 | 1 到 1000 位，给申请人看的通过说明。 |
| `internalNote` | string | 否 | 最多 1000 位。 |
| `profileGroupId` | string | 否 | 传给 profile 的成员组 ID。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只允许 `PENDING_REVIEW`、`UNDER_REVIEW` 和 `SUPPLEMENT_SUBMITTED`。审核通过必须先完成本地校验，再调用 profile 激活成员档案。profile 激活成功后进入 `APPROVED`，`result=APPROVED`，生成 `attendanceHandoff`，`attendanceInitializationStatus=WAITING_MODULE` 或 `READY_FOR_CONSUME`，并尝试发送通知。profile 激活失败时不得进入 `APPROVED`，必须进入 `APPROVAL_BLOCKED`，保存失败摘要并写审计。notification 失败不回滚已通过状态。

profile 不可用、超时或响应字段不兼容时，审核通过不得进入 `APPROVED`，必须返回 `47020`、`47021` 或 `47022`，并保持申请可复核。若 profile 已确认激活成功但 whitelist 本地状态写入失败，必须返回 `52003`，把申请保留为 `APPROVAL_BLOCKED` 或等价可复核状态，保存 `profileActivation.status=ACTIVATED`、失败摘要和审计线索，后续不得对用户伪造成已通过。

审计要求：成功写入 `WHITELIST_APPROVED` 和 `WHITELIST_PROFILE_ACTIVATED`。profile 失败写入 `WHITELIST_PROFILE_ACTIVATION_FAILED`。审计失败时不得调用 profile，不得改变状态。

### 审核拒绝

`PATCH /api/v1/whitelist/admin/applications/{applicationId}/reject`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reviewComment` | string | 是 | 1 到 1000 位，给申请人看的拒绝说明。 |
| `internalNote` | string | 否 | 最多 1000 位。 |
| `allowReapply` | boolean | 否 | 默认 `true`。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只允许 `PENDING_REVIEW`、`UNDER_REVIEW`、`SUPPLEMENT_SUBMITTED` 和 `APPROVAL_BLOCKED`。成功后进入 `REJECTED`，`result=REJECTED`。不得调用 profile 激活，不得生成 attendance handoff。通知失败不回滚。

### 移除白名单

`PATCH /api/v1/whitelist/admin/applications/{applicationId}/remove`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `publicComment` | string | 是 | 1 到 1000 位，给成员看的移除说明。 |
| `reason` | string | 是 | 1 到 200 位，后台移除原因。 |
| `confirmText` | string | 是 | 二次确认文本，P0 固定要求 `REMOVE_WHITELIST`。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：该接口是 `HIGH` 风险，必须要求 `ADMIN` 或 `OWNER`，并校验二次确认。只允许 `APPROVED` 申请移除。成功后状态进入 `REMOVED`，`result=REMOVED`，`reapplyRequired=true`，`nextExamAttemptType=RECHECK`。必须调用 profile 状态接口把成员档案流转为 `REMOVED` 或契约允许目标状态。不得直接执行真实服务器白名单命令，不得写服务器文件。profile 状态变更失败时不得进入 `REMOVED`，必须返回依赖错误或保持原状态。

profile 移除状态接口不可用、超时或响应字段不兼容时，移除操作不得进入 `REMOVED`。若 profile 已确认状态变更成功但 whitelist 本地状态写入失败，必须返回 `52003`，保留可复核摘要，避免官网状态和成员档案状态长期不一致且无法追踪。

审计要求：成功写入 `WHITELIST_REMOVED`，记录为 `HIGH` 风险。重复移除同一申请保持幂等，不重复写审计。

### 允许重新申请

`POST /api/v1/whitelist/admin/applications/{applicationId}/reopen`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `publicComment` | string | 是 | 1 到 1000 位，给用户看的重新申请说明。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为更新后的 `WhitelistApplication`。

业务规则：只允许 `REJECTED`、`WITHDRAWN` 或 `REMOVED` 的历史申请重开为 `REAPPLYING`，并标记下一次考试类型。重开不创建新考试、不创建新白名单申请、不修改 exam 题库、不初始化积分。用户仍必须重新通过 exam 交接创建新申请。通知失败不回滚。

### attendance 交接摘要

`GET /api/v1/whitelist/admin/applications/{applicationId}/attendance-handoff`

成功响应 HTTP `200`，`data` 为 `WhitelistAttendanceHandoffSnapshot`。

业务规则：只有 `APPROVED` 且 profile 激活成功的申请可以读取。未通过、未激活或交接摘要未生成返回 `44021` 或 `44014`。该接口只提供后续 `attendance` 初始化积分所需只读快照，不创建积分流水，不创建榜单，不推进 attendance 状态。读取是低风险，但必须写入可追溯的读取审计或自检计数，具体实现固定。

### whitelist 审计列表

`GET /api/v1/whitelist/admin/audit-logs`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `applicationId` | string | 否 | 白名单申请 ID。 |
| `actorUserId` | string | 否 | 操作者用户 ID。 |
| `action` | string | 否 | 动作，最多 80 位。 |
| `result` | string | 否 | `SUCCESS` 或 `FAILED`。 |
| `from` | string | 否 | ISO 8601 起始时间。 |
| `to` | string | 否 | ISO 8601 结束时间。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`。默认 `createdAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `WhitelistAuditLog[]`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 whitelist API 删除，返回结果必须脱敏。

### whitelist 自检摘要

`GET /api/v1/whitelist/admin/ops/summary`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "whitelist",
    "port": 8131,
    "legacyPort": 8110,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "examMode": "TEST_STUB",
    "profileMode": "TEST_STUB",
    "notificationMode": "TEST_STUB",
    "applicationsTotal": 8,
    "pendingReviewTotal": 2,
    "approvedTotal": 3,
    "rejectedTotal": 1,
    "removedTotal": 1,
    "approvalBlockedTotal": 1,
    "attendanceHandoffsTotal": 3,
    "auditsTotal": 40,
    "idempotencyRecordsTotal": 10,
    "lastAuditAt": "2026-05-23T12:00:00Z",
    "productionGaps": [
      "P0_IN_MEMORY_STORAGE",
      "P0_AUTH_STUB",
      "P0_EXAM_STUB",
      "P0_PROFILE_STUB",
      "P0_NOTIFICATION_STUB",
      "ATTENDANCE_NOT_IMPLEMENTED",
      "REAL_SERVER_WHITELIST_NOT_CONNECTED"
    ]
  }
}
```

业务规则：自检摘要用于后台确认 whitelist 当前运行模式、待审核规模、通过规模、移除规模、profile 阻塞规模、attendance 交接规模和生产化缺口。`port` 固定返回当前运行入口 `8131`，`legacyPort` 固定返回历史原服务端口 `8110`。摘要不得返回 token、请求头、Minecraft 验证凭据、考试答案、profile 内部备注、通知正文、审计参数全文、真实服务器命令、节点凭据或异常堆栈。

## 状态、幂等和并发

创建成功后 P0 默认进入 `PENDING_REVIEW`。`DRAFT` 可提交为 `PENDING_REVIEW`。`PENDING_REVIEW` 可分配为 `UNDER_REVIEW`，可要求补充为 `NEEDS_SUPPLEMENT`，可审核通过为 `APPROVED`，可审核拒绝为 `REJECTED`，可撤回为 `WITHDRAWN`。`UNDER_REVIEW` 可要求补充、通过或拒绝。`NEEDS_SUPPLEMENT` 只可由用户补充为 `SUPPLEMENT_SUBMITTED` 或撤回。`SUPPLEMENT_SUBMITTED` 可重新进入审核、通过、拒绝或再次要求补充。`APPROVAL_BLOCKED` 可重试通过流程或拒绝，不能对用户伪造成已通过。`APPROVED` 可移除为 `REMOVED`。`REJECTED`、`WITHDRAWN` 和 `REMOVED` 可由后台标记为 `REAPPLYING`，但新申请必须重新消费新的 exam 通过交接。`ARCHIVED` 为终态。

状态推进只能由服务端根据 exam handoff、当前申请状态、审核动作、profile 调用结果、二次确认和权限判断决定。非法状态跳跃返回 `44014`。浏览器传入可信字段必须忽略或返回字段校验失败。

创建申请、修改材料、提交审核、提交补充、撤回、分配审核人、要求补充、审核通过、审核拒绝、移除和重开支持 `idempotencyKey`。同一操作者、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `44017`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序。

并发创建同一 exam handoff 只能产生一个申请。并发审核同一申请只能有一个最终结果。并发补充和审核必须以服务端当前状态为准，不得产生补充后又被旧审核覆盖的半状态。读取接口允许读到更新前或更新后的完整状态，不能返回半更新对象。

## 审计要求

必须审计的动作包括创建申请、修改材料、提交审核、提交补充、撤回、分配审核人、要求补充、审核通过、审核拒绝、profile 激活成功、profile 激活失败、notification 投递失败、移除白名单、允许重新申请、attendance handoff 读取、依赖降级导致操作不可继续、审计写入失败和状态写入失败。

后台写操作必须记录 `reason`、操作者、目标对象、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计不得泄露 token、完整请求头、Minecraft 验证凭据、考试正确答案、profile 后台备注全文、通知正文全文、真实服务器命令、节点凭据、内部异常堆栈或前序服务内部路径。

审计写入失败时，创建申请、材料修改、提交补充、撤回、审核、移除和重开不得假装成功，必须返回 `52001` 或 `52000`，并保持业务数据不变。

## 失败降级

auth 是所有接口强依赖。auth 不可用、超时、用户状态不允许或字段不兼容时不得伪造成功。

exam 是创建申请强依赖。交接快照不可用、未通过、已消费、用户不匹配、Minecraft 绑定缺失或字段不兼容时不得创建申请。

profile 是审核通过和移除白名单强依赖。profile 激活失败时申请不得进入 `APPROVED`。profile 移除状态失败时申请不得进入 `REMOVED`。如果 profile 已激活但 whitelist 状态写入失败，必须记录 `52003` 风险并提供可复核状态，不能吞成成功。

notification 默认是辅助依赖。通知失败不得回滚创建、补充、通过、拒绝、移除或重开，但必须记录失败摘要和审计。

通知失败摘要至少要能区分不可用、超时和字段不兼容三类失败。P0 可统一保存为 `notificationStatus=FAILED`，但必须同时保存 `notificationFailure.failureCode`、`notificationFailure.failureType` 和脱敏 `failureReason`，审计中也必须保留同等级别的脱敏失败线索，不得保存通知正文或完整请求头。

attendance 未实现时，审核通过仍可完成 whitelist 和 profile 激活，但只能返回 `attendanceInitializationStatus=WAITING_MODULE` 或 `READY_FOR_CONSUME`。不得返回积分已初始化、不得创建积分流水、不得维护榜单。

真实服务器白名单写入未接入时，审核通过只代表官网业务白名单通过和 profile 激活，不代表已经执行 Minecraft 服务器命令。P0 必须在自检摘要中暴露 `REAL_SERVER_WHITELIST_NOT_CONNECTED`。

## 验收口径

`whitelist` API 文档按 `docs/contracts-whitelist.md` 独立存在，并由 `.local-docs/tests-whitelist.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`whitelist` 完成时必须满足以下条件：全部接口按本文档实现；当前用户接口只能访问自己的申请；后台接口按角色限制；创建申请只通过 exam handoff 和前序适配读取快照，不直接读前序服务实现；审核通过必须通过 profile 正式接口激活成员档案；profile 激活失败不进入通过终态；通知失败按辅助降级记录；attendance 未实现时只生成交接摘要；移除白名单不执行真实服务器命令；当前运行入口为 `admission-core-service:8131`，历史端口只作为 `legacyPort=8110` 返回；默认关闭测试控制头，直连伪造 `X-Beiming-Actor-*` 不能绕过 Bearer，网关注入可信上下文可被识别；`.local-docs/tests-whitelist.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 whitelist 全部测试通过；auth、profile、notification、content、server-status、resource、admin、onboarding 和 exam 前序服务回归测试通过；没有修改前序服务稳定接口；没有把考勤积分、社区工单、活动、日历、更新日志、后台聚合、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复或 Cloudreve 管理能力塞进 whitelist。
