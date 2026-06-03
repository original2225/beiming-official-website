# 北冥官网 onboarding API 契约

版本：0.2

## 文档定位

本文档是 `onboarding` 微服务的正式 API 契约。后续 `exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、前端适配和 `ops-control` 只能通过本文档定义的接口读取或维护入服引导状态，不能直接读取或修改 `onboarding` 数据库，也不能把考试、白名单、成员激活或考勤积分逻辑塞进 `onboarding`。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `onboarding` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、降级、审计和验收口径。

本文档参考了成熟平台的 onboarding 和流程状态设计。Stripe Connect onboarding 强调由服务端读取当前要求、生成下一步组件、处理状态和校验，避免前端自行判定完成状态。Discord Community Onboarding 和 Rules Screening 强调新成员先完成必要规则确认，再做个性化方向选择，并避免一次塞入太多问题。Jira Workflow 强调状态、单向流转、条件、校验器和后置动作的分离。本文档只吸收这些适合北冥入服流程的做法，不引入支付、Discord 角色分配或 Jira 工单模型。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Stripe Embedded onboarding](https://docs.stripe.com/connect/embedded-onboarding?locale=en-GB) | onboarding 应由服务端根据要求生成下一步和校验状态，前端只渲染入口。 |
| [Stripe onboarding configuration](https://docs.stripe.com/connect/onboarding?locale=en-GB) | onboarding 可以分为托管、嵌入和自定义 API，北冥选择自有 API，但仍保留服务端状态判定。 |
| [Discord Community Onboarding FAQ](https://support.discord.com/hc/en-us/articles/11074987197975-Community-Onboarding-FAQ) | 新成员流程应包含默认入口、简单问题、必要项和完成后的下一步。 |
| [Discord Rules Screening FAQ](https://support.discord.com/hc/en-us/articles/1500000466882-Rules-Screening-FAQ) | 规则确认应作为明确门槛，确认前不能进入后续参与步骤。 |
| [Atlassian Jira workflows overview](https://www.atlassian.com/software/jira/guides/workflows/overview) | 流程由状态和单向流转组成，非法跳转必须被拒绝。 |
| [Atlassian workflow validators](https://support.atlassian.com/jira-cloud-administration/docs/use-workflow-validators-with-custom-fields/) | 状态推进前必须先做输入和条件校验，校验失败不得执行后置动作。 |

## 职责边界

`onboarding` 负责新玩家从注册后到进入考试前的入服引导状态。它保存流程实例、步骤完成状态、资料确认、规则确认、审核方向选择、流程阻塞原因、下一步入口、后续模块占位、幂等记录和自身审计。

`onboarding` 不负责注册、登录、邀请码、会话、账号角色、Minecraft 绑定验证、成员档案主数据、考试题库、试卷、判分、人工阅卷、白名单申请、白名单审核、成员激活、考勤积分、通知主数据、内容主数据、资源下载、服务器状态采集或真实运维操作。

`onboarding` 可以读取前序模块摘要，但不能要求前序模块反向适配。它只能适配 `auth`、`profile`、`notification` 和 `content` 的正式契约或后端入口传入的可信上下文。`server-status`、`resource` 和 `admin` 不是 onboarding 状态判定依赖。

## 数据归属

`onboarding` 拥有以下主数据：入服流程实例、步骤状态、资料确认记录、规则版本确认记录、审核方向选择、流程阻塞记录、后续模块移交摘要、幂等记录、通知调用摘要和 onboarding 审计日志。

`onboarding` 可以保存当前用户快照、Minecraft 绑定快照、成员档案摘要、规则内容摘要和通知投递结果摘要。快照不是来源模块主数据，不能用于替代来源模块权限判断，也不能反写 `auth`、`profile`、`notification` 或 `content`。

## 基础路径与认证

所有接口默认使用 `/api/v1/onboarding` 前缀。第二批合并后当前运行入口由 `admission-core-service` 承载，端口固定为 `8131`。历史原服务端口 `8108` 只作为 `legacyPort` 返回，不作为当前运行入口、网关上游或测试入口。

当前用户接口使用 `/api/v1/onboarding/me` 前缀，全部要求 `Authorization: Bearer <token>`，只能读取或维护当前认证用户自己的入服流程。浏览器请求体不得传入 `userId`、`roles`、`permissions`、`minecraftId`、`minecraftUuid`、`status`、`currentStep`、`createdBy`、`updatedBy`、`blockedBy` 等服务端可信字段。

后台接口使用 `/api/v1/onboarding/admin` 前缀，全部要求登录。后台读取接口要求 `HELPER`、`ADMIN` 或 `OWNER`。后台写操作要求 `ADMIN` 或 `OWNER`，必须携带 `reason` 并写入审计。`HELPER` 可以查看流程和审计摘要，但不能重置、阻塞或解除阻塞。

## 本地测试控制头

`onboarding` 允许在本地自动化测试中使用 `X-Test-Dependency-Mode`、`X-Test-Notification-Mode`、`X-Test-Fail-Audit` 和 `X-Test-Fail-Store` 模拟依赖失败、通知失败、审计失败和状态写入失败。该能力只服务测试闭环，不属于正式业务 API。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、状态失败或通知失败。只有 `onboarding.test-controls.enabled=true` 时，自动化测试才能启用这些请求头。

## 网关可信身份上下文

经 `api-gateway` 访问时，`onboarding` 可以优先读取网关注入的可信身份头。只有 `X-Gateway-Internal-Request-Id` 存在时，才进入可信上下文解析；若该头缺失，即使请求带有 `X-Beiming-Actor-*`，也必须忽略这些头并继续走 `Authorization: Bearer <token>` 兼容路径。可信上下文缺少 `X-Beiming-Actor-User-Id`、角色枚举不兼容或字段无法解析时返回 HTTP `502` 和 `46802`，不得静默降级成匿名用户。

## 前序服务兼容契约

`onboarding` 适配 `auth`。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions`、`status` 和 `minecraftBinding`。`minecraftBinding` 结构必须兼容 `docs/contracts-auth.md`。用户状态为 `PENDING_PROFILE` 或 `ACTIVE` 时允许启动或读取流程；`DISABLED`、`BANNED`、`DELETED` 不允许启动、确认或推进流程。auth 不可用返回 `46800`，auth 超时返回 `46801`，auth 字段或枚举不兼容返回 `46802`。

`onboarding` 可以适配 `profile` 读取当前用户成员档案摘要。用户已有 `ACTIVE` 或 `INACTIVE` 成员档案时，默认不允许开启新玩家流程，返回 `43812`，除非后续 `exam` 和 `whitelist` 契约明确引入二次入服流程。profile 不可用时，读取进度可以返回降级摘要；涉及资料确认、成员状态判定和推进的写操作必须返回 `46810`、`46811` 或 `46812`，不得伪造成员状态。

`onboarding` 可以适配 `content` 获取当前生效规则摘要。规则摘要至少包含 `ruleContentId`、`ruleVersion`、`title`、`updatedAt` 和 `guideRoute`。content 不可用时，读取进度可以使用已缓存规则摘要并标记降级；没有缓存时，规则确认和推进必须返回 `46820` 或 `46821`。content 字段不兼容返回 `46822`。

`onboarding` 可以适配 `notification` 在关键状态变化后投递站内通知。通知是辅助动作，除后台重置、阻塞和解除阻塞明确要求通知目标用户外，通知失败不得回滚用户主流程，但必须记录 `notificationStatus=FAILED` 和失败原因摘要。通知强制投递不可用返回 `46830`，通知超时返回 `46831`，字段不兼容返回 `46832`。

后续 `exam` 和 `whitelist` 未实现时，`onboarding` 仍可把流程推进到 `READY_FOR_EXAM`，并在下一步入口中返回 `targetModuleStatus=NOT_IMPLEMENTED` 或 `WAITING_MODULE`。它不能伪造考试已开始、考试已通过、白名单已申请、白名单已通过、成员已激活或积分已初始化。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `OnboardingStatus` | `NOT_STARTED`、`IN_PROGRESS`、`BLOCKED`、`READY_FOR_EXAM`、`WAITING_EXAM`、`READY_FOR_WHITELIST`、`WAITING_WHITELIST`、`COMPLETED`、`CANCELLED` | 入服流程状态。P0 实现到 `READY_FOR_EXAM`，后续状态作为兼容占位，不得伪造下游结果。 |
| `OnboardingStepKey` | `ACCOUNT_READY`、`MINECRAFT_BOUND`、`PROFILE_CONFIRMED`、`RULES_CONFIRMED`、`DIRECTION_SELECTED`、`EXAM_READY`、`WHITELIST_READY` | 入服步骤。 |
| `OnboardingStepStatus` | `LOCKED`、`AVAILABLE`、`COMPLETED`、`BLOCKED`、`WAITING_MODULE` | 单步状态。 |
| `ReviewDirection` | `REDSTONE`、`LATE_GAME`、`BUILDING`、`GENERAL` | 审核方向，对应红石、后期、建筑和通用。 |
| `TargetModuleStatus` | `AVAILABLE`、`NOT_IMPLEMENTED`、`DEGRADED`、`UNAVAILABLE`、`WAITING_MODULE` | 下一步目标模块状态。 |
| `OnboardingAuditResult` | `SUCCESS`、`FAILED` | onboarding 审计结果。 |

## 通用对象

### OnboardingApplication

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `applicationId` | string | 是 | 入服流程实例 ID。 |
| `userId` | string | 是 | auth 用户 ID。当前用户接口中固定为认证用户。 |
| `displayNameSnapshot` | string | 是 | auth 展示名快照。 |
| `authStatusSnapshot` | string | 是 | 最近一次读取 auth 时的用户状态。 |
| `minecraftBindingSnapshot` | object 或 null | 是 | Minecraft 绑定快照，字段兼容 auth 的 `MinecraftBinding`。 |
| `profileSummary` | OnboardingProfileSummary 或 null | 是 | profile 摘要。 |
| `status` | string | 是 | `OnboardingStatus`。 |
| `currentStep` | string | 是 | 当前建议处理步骤。 |
| `steps` | OnboardingStep[] | 是 | 步骤状态列表。 |
| `reviewDirection` | string 或 null | 是 | 审核方向。 |
| `ruleConfirmation` | RuleConfirmation 或 null | 是 | 规则确认记录。 |
| `profileConfirmation` | ProfileConfirmation 或 null | 是 | 资料确认记录。 |
| `nextAction` | OnboardingNextAction | 是 | 下一步入口。 |
| `blockedReason` | string 或 null | 是 | 阻塞原因摘要。 |
| `blockedBy` | string 或 null | 是 | 阻塞操作者用户 ID。 |
| `blockedAt` | string 或 null | 是 | 阻塞时间。 |
| `notificationStatus` | string 或 null | 是 | 最近一次通知摘要，允许 `DELIVERED`、`FAILED`、`SKIPPED`。 |
| `degraded` | boolean | 是 | 是否存在局部降级。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `completedAt` | string 或 null | 是 | 完成时间。P0 不会因下游未实现而写入。 |
| `cancelledAt` | string 或 null | 是 | 取消时间。 |

### OnboardingStep

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `key` | string | 是 | `OnboardingStepKey`。 |
| `status` | string | 是 | `OnboardingStepStatus`。 |
| `title` | string | 是 | 前端展示标题。 |
| `required` | boolean | 是 | 是否必做。 |
| `completedAt` | string 或 null | 是 | 完成时间。 |
| `blockReason` | string 或 null | 是 | 该步骤阻塞原因。 |
| `targetRoute` | string 或 null | 是 | 前端建议入口。 |
| `targetApi` | string 或 null | 是 | 对应 API 或后续模块接口。未实现模块可为 `null`。 |

### ProfileConfirmation

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `confirmed` | boolean | 是 | 是否已确认资料。 |
| `displayNameSnapshot` | string | 是 | 确认时的展示名快照。 |
| `minecraftIdSnapshot` | string 或 null | 是 | 确认时的 Minecraft ID。 |
| `minecraftUuidSnapshot` | string 或 null | 是 | 确认时的 Minecraft UUID。 |
| `confirmedAt` | string | 是 | 确认时间。 |

### RuleConfirmation

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `confirmed` | boolean | 是 | 是否已确认规则。 |
| `ruleContentId` | string | 是 | content 中规则或指南内容 ID。 |
| `ruleVersion` | string | 是 | 规则版本。 |
| `ruleTitle` | string | 是 | 规则标题。 |
| `guideRoute` | string | 是 | 前端规则页面路由。 |
| `confirmedAt` | string | 是 | 确认时间。 |

### OnboardingProfileSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `memberId` | string | 是 | profile 成员档案 ID。 |
| `status` | string | 是 | profile 成员状态。 |
| `displayName` | string | 是 | 成员展示名快照。 |
| `minecraftId` | string 或 null | 是 | Minecraft 展示 ID。 |
| `snapshotAt` | string | 是 | 快照时间。 |

### OnboardingNextAction

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `step` | string | 是 | 建议处理步骤。 |
| `label` | string | 是 | 前端展示文案。 |
| `targetRoute` | string 或 null | 是 | 前端路由。 |
| `targetApi` | string 或 null | 是 | 当前模块或后续模块 API。 |
| `targetModule` | string | 是 | `ONBOARDING`、`EXAM` 或 `WHITELIST`。 |
| `targetModuleStatus` | string | 是 | `TargetModuleStatus`。 |
| `enabled` | boolean | 是 | 当前是否可点击进入。 |
| `disabledReason` | string 或 null | 是 | 不可用原因。 |

### OnboardingExamHandoffSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `applicationId` | string | 是 | 入服流程实例 ID。 |
| `userId` | string | 是 | auth 用户 ID。 |
| `displayNameSnapshot` | string | 是 | auth 展示名快照。 |
| `minecraftBindingSnapshot` | object | 是 | Minecraft 绑定快照，字段兼容 auth 的 `MinecraftBinding`。 |
| `profileConfirmation` | ProfileConfirmation | 是 | 资料确认记录。 |
| `ruleConfirmation` | RuleConfirmation | 是 | 规则确认记录。 |
| `reviewDirection` | string | 是 | 审核方向。 |
| `status` | string | 是 | 当前必须为 `READY_FOR_EXAM`。 |
| `readyForExam` | boolean | 是 | 是否满足 exam 可读取前置条件。 |
| `handoffAllowed` | boolean | 是 | 是否允许后续 exam 读取并创建考试流程。P0 在 exam 未实现时仍可为 `true`，但不代表考试已开始。 |
| `targetModule` | string | 是 | 固定为 `EXAM`。 |
| `targetModuleStatus` | string | 是 | P0 固定为 `NOT_IMPLEMENTED`，后续 exam 闭环后可变更为 `AVAILABLE` 或 `WAITING_MODULE`。 |
| `blocked` | boolean | 是 | 当前流程是否被后台阻塞。 |
| `blockedReason` | string 或 null | 是 | 阻塞摘要。 |
| `handoffVersion` | integer | 是 | 交接快照版本，从 `1` 开始，后续字段兼容扩展时递增。 |
| `generatedAt` | string | 是 | 生成快照时间。 |

### OnboardingAuditLog

审计字段继承公共契约，允许补充 `applicationId`、`stateFrom`、`stateTo`、`stepKey`、`reviewDirection`、`ruleVersion`、`idempotencyKey`、`notificationStatus` 和 `dependencyStatus`。审计日志不得通过 onboarding API 删除。

## onboarding 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `43800` | 404 | 入服流程不存在，或当前用户无权访问该流程。 |
| `43801` | 404 | 规则摘要不存在或不可确认。 |
| `43802` | 404 | 审计记录不存在。 |
| `43810` | 409 | 当前流程状态不允许该操作。 |
| `43811` | 409 | 步骤前置条件未满足。 |
| `43812` | 409 | 当前用户已有成员档案，不允许开启新玩家流程。 |
| `43813` | 409 | Minecraft 身份未绑定或绑定快照不完整。 |
| `43814` | 409 | 规则版本已过期，需要重新读取并确认。 |
| `43815` | 409 | 审核方向不允许修改。 |
| `43816` | 409 | 流程已被后台阻塞。 |
| `43817` | 409 | onboarding 幂等键请求指纹冲突。 |
| `43818` | 409 | 后续模块未实现，不能继续推进到下游已完成状态。 |
| `46800` | 502 | auth 认证上下文不可用。 |
| `46801` | 504 | auth 认证上下文调用超时。 |
| `46802` | 502 | auth 认证上下文字段或枚举不兼容 onboarding 契约。 |
| `46810` | 502 | profile 摘要不可用。 |
| `46811` | 504 | profile 摘要调用超时。 |
| `46812` | 502 | profile 摘要字段或枚举不兼容 onboarding 契约。 |
| `46820` | 502 | content 规则摘要不可用。 |
| `46821` | 504 | content 规则摘要调用超时。 |
| `46822` | 502 | content 规则摘要字段不兼容 onboarding 契约。 |
| `46830` | 502 | notification 强制投递不可用。 |
| `46831` | 504 | notification 强制投递超时。 |
| `46832` | 502 | notification 投递响应不兼容 onboarding 契约。 |
| `51800` | 500 | onboarding 内部错误。 |
| `51801` | 500 | onboarding 审计写入失败。 |
| `51802` | 500 | onboarding 状态写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误、通用幂等键冲突和通用服务端错误优先使用公共错误码。onboarding 自有幂等指纹冲突使用 `43817`。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 当前用户进度 | GET | `/api/v1/onboarding/me/progress` | 是 | 当前用户 | LOW |
| 创建或恢复流程 | POST | `/api/v1/onboarding/me/start` | 是 | 当前用户 | LOW |
| 确认账号和 Minecraft 资料 | PATCH | `/api/v1/onboarding/me/profile-confirmation` | 是 | 当前用户 | LOW |
| 确认阅读规则 | PATCH | `/api/v1/onboarding/me/rules-confirmation` | 是 | 当前用户 | LOW |
| 选择审核方向 | PATCH | `/api/v1/onboarding/me/direction` | 是 | 当前用户 | LOW |
| 推进下一步 | POST | `/api/v1/onboarding/me/advance` | 是 | 当前用户 | LOW |
| 当前用户下一步入口 | GET | `/api/v1/onboarding/me/next-action` | 是 | 当前用户 | LOW |
| 后台流程列表 | GET | `/api/v1/onboarding/admin/applications` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台流程详情 | GET | `/api/v1/onboarding/admin/applications/{applicationId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 考试交接快照 | GET | `/api/v1/onboarding/admin/applications/{applicationId}/exam-handoff` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 后台重置流程 | PATCH | `/api/v1/onboarding/admin/applications/{applicationId}/reset` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台阻塞流程 | PATCH | `/api/v1/onboarding/admin/applications/{applicationId}/block` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台解除阻塞 | PATCH | `/api/v1/onboarding/admin/applications/{applicationId}/unblock` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| onboarding 审计列表 | GET | `/api/v1/onboarding/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| onboarding 自检摘要 | GET | `/api/v1/onboarding/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 当前用户接口

### 当前用户进度

`GET /api/v1/onboarding/me/progress`

成功响应 HTTP `200`，`data` 为 `OnboardingApplication`。当前用户没有流程时返回一个只读进度视图，`applicationId=null`、`status=NOT_STARTED`、`currentStep=ACCOUNT_READY`，不得创建流程。

业务规则：必须从 auth 获取当前用户和 Minecraft 绑定状态。profile 不可用时允许返回进度并标记 `degraded=true`，但 `PROFILE_CONFIRMED` 不得被伪造成完成。content 规则摘要不可用且无缓存时，规则步骤为 `BLOCKED`。已有成员档案且状态为 `ACTIVE` 或 `INACTIVE` 时，下一步返回已是成员的说明，不自动创建新流程。

### 创建或恢复流程

`POST /api/v1/onboarding/me/start`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `OnboardingApplication`。若流程已存在且未取消，返回 HTTP `200` 和现有流程。

业务规则：未绑定 Minecraft 身份时允许创建流程，但 `MINECRAFT_BOUND` 步骤为 `BLOCKED`，后续确认和推进不得跳过。已有 `ACTIVE` 或 `INACTIVE` 成员档案返回 `43812`。用户状态不允许启动时返回认证或状态错误。创建流程必须写入 `ONBOARDING_STARTED` 审计，审计失败返回 `51801`，不得创建流程。

幂等规则：同一用户、同一 `idempotencyKey`、同一请求体重复提交时返回同一流程。相同幂等键搭配不同请求体返回 `43817`。

### 确认账号和 Minecraft 资料

`PATCH /api/v1/onboarding/me/profile-confirmation`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `confirmed` | boolean | 是 | 必须为 `true`。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为 `OnboardingApplication`。

业务规则：流程必须已创建且未被阻塞。auth 必须存在完整 Minecraft 绑定，否则返回 `43813`。profile 不可用、已有正式成员档案或字段不兼容时不得确认。确认只保存服务端读取到的展示名和 Minecraft 快照，浏览器传入同名字段必须忽略或返回字段校验失败。重复确认同一快照返回成功并保持幂等。绑定快照发生变化时，必须重新确认并记录新快照。

### 确认阅读规则

`PATCH /api/v1/onboarding/me/rules-confirmation`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `confirmed` | boolean | 是 | 必须为 `true`。 |
| `ruleContentId` | string | 是 | 前端展示的规则内容 ID，必须等于服务端当前规则摘要。 |
| `ruleVersion` | string | 是 | 前端确认的规则版本，必须等于服务端当前规则版本或服务端允许的缓存版本。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为 `OnboardingApplication`。

业务规则：流程必须已创建且未被阻塞。content 当前规则不可用且没有可用缓存时返回 `46820` 或 `46821`。提交的规则版本不是当前版本或允许缓存版本时返回 `43814`，不得写确认。规则确认只表示用户读过规则，不代表考试或白名单通过。

### 选择审核方向

`PATCH /api/v1/onboarding/me/direction`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reviewDirection` | string | 是 | `REDSTONE`、`LATE_GAME`、`BUILDING`、`GENERAL`。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为 `OnboardingApplication`。

业务规则：流程必须已创建且未被阻塞。资料确认和规则确认必须已完成，否则返回 `43811`。方向首次选择后允许在进入 `READY_FOR_EXAM` 前修改；一旦状态达到 `READY_FOR_EXAM`、`WAITING_EXAM` 或更后状态，修改方向返回 `43815`。方向只作为后续 exam 的输入摘要，onboarding 不生成试卷。

### 推进下一步

`POST /api/v1/onboarding/me/advance`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为 `OnboardingApplication`。

业务规则：推进由服务端根据 auth、profile、content、资料确认、规则确认和方向选择计算。流程未创建返回 `43800`。被阻塞返回 `43816`。资料、规则或方向缺失返回 `43811`。全部前置步骤完成后，P0 将状态推进为 `READY_FOR_EXAM`，`EXAM_READY` 步骤为 `WAITING_MODULE`，下一步入口指向 exam 占位并返回 `targetModuleStatus=NOT_IMPLEMENTED`。不得推进到 `WAITING_EXAM`、`READY_FOR_WHITELIST`、`WAITING_WHITELIST` 或 `COMPLETED`，除非后续模块正式契约和实现已经闭环。

### 当前用户下一步入口

`GET /api/v1/onboarding/me/next-action`

成功响应 HTTP `200`，`data` 为 `OnboardingNextAction`。

业务规则：下一步入口只由服务端状态计算。未开始时指向 `/onboarding/start`。资料未确认时指向资料确认。规则未确认时指向规则页。方向未选时指向方向选择。准备考试时指向 `/exam/start`，但在 exam 未实现时 `enabled=false`、`targetModuleStatus=NOT_IMPLEMENTED`。被阻塞时 `enabled=false` 并返回阻塞摘要。

## 后台接口

### 后台流程列表

`GET /api/v1/onboarding/admin/applications`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配用户 ID、展示名、Minecraft ID 或流程 ID，最多 80 位。 |
| `status` | string | 否 | 任一 `OnboardingStatus`。 |
| `reviewDirection` | string | 否 | 任一 `ReviewDirection`。 |
| `blocked` | boolean | 否 | 是否只看阻塞流程。 |
| `sort` | string | 否 | 允许 `updatedAt_desc`、`createdAt_desc`、`status_asc`、`displayName_asc`。默认 `updatedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `OnboardingApplication[]`。

权限规则：`HELPER`、`ADMIN` 和 `OWNER` 可访问。`USER` 返回 `42001`。未登录返回 `41000`。

### 后台流程详情

`GET /api/v1/onboarding/admin/applications/{applicationId}`

成功响应 HTTP `200`，`data` 为 `OnboardingApplication`。流程不存在返回 `43800`。后台详情不得返回访问令牌、完整请求头、Minecraft 验证凭据、通知正文、content 正文、后台审计参数全文或异常堆栈。

### 考试交接快照

`GET /api/v1/onboarding/admin/applications/{applicationId}/exam-handoff`

成功响应 HTTP `200`，`data` 为 `OnboardingExamHandoffSnapshot`。

权限规则：只有 `ADMIN` 和 `OWNER` 可读取。`HELPER` 返回 `42001`，未登录返回 `41000`。

业务规则：该接口只提供后续 `exam` 创建考试流程所需的只读快照，不创建试卷，不推进 onboarding 状态，不写考试记录，不写白名单申请。流程不存在返回 `43800`。流程被后台阻塞返回 `43816`。流程未达到 `READY_FOR_EXAM`、资料确认缺失、规则确认缺失、审核方向缺失、Minecraft 绑定快照不完整或规则版本已过期时返回 `43811` 或 `43814`，不得返回可用交接快照。P0 因 exam 未实现时，`targetModuleStatus` 固定为 `NOT_IMPLEMENTED`，但 `handoffAllowed` 可以为 `true`，表示 onboarding 自身前置条件已经满足。

降级规则：auth 是强依赖，认证上下文不可用、超时或字段不兼容时返回 `46800`、`46801` 或 `46802`。content 当前规则不可用时，如果无法确认已保存规则版本仍有效，返回 `46820` 或 `46821`，不得生成交接快照。profile 不可用时，若无法确认当前用户没有 `ACTIVE` 或 `INACTIVE` 成员档案，返回 `46810` 或 `46811`。

审计要求：交接快照读取是低风险读取，不强制写审计，不得增加 `auditsTotal`。后续 exam 创建考试流程时必须在 exam 自己的契约和审计中记录来源 `applicationId` 和 `handoffVersion`。

### 后台重置流程

`PATCH /api/v1/onboarding/admin/applications/{applicationId}/reset`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `resetToStep` | string | 否 | 允许 `ACCOUNT_READY`、`PROFILE_CONFIRMED`、`RULES_CONFIRMED`、`DIRECTION_SELECTED`，默认 `ACCOUNT_READY`。 |
| `notifyUser` | boolean | 否 | 默认 `true`。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为重置后的 `OnboardingApplication`。

业务规则：`COMPLETED` 流程 P0 不允许重置，返回 `43810`。重置会清除目标步骤之后的完成记录，并把状态改为 `IN_PROGRESS`。若 `notifyUser=true`，通知失败返回 `46830` 或 `46831`，状态不得变化。审计失败返回 `51801`，状态不得变化。

### 后台阻塞流程

`PATCH /api/v1/onboarding/admin/applications/{applicationId}/block`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `blockReason` | string | 是 | 1 到 500 位，阻塞原因。 |
| `notifyUser` | boolean | 否 | 默认 `true`。 |
| `reason` | string | 是 | 1 到 200 位，后台操作原因。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为阻塞后的 `OnboardingApplication`。

业务规则：重复阻塞同一流程且阻塞原因相同返回成功，保持幂等，不重复写审计。已取消或已完成流程不能阻塞，返回 `43810`。阻塞后当前用户写接口返回 `43816`，读取接口仍可返回当前状态。

### 后台解除阻塞

`PATCH /api/v1/onboarding/admin/applications/{applicationId}/unblock`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `notifyUser` | boolean | 否 | 默认 `true`。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为解除阻塞后的 `OnboardingApplication`。

业务规则：只有 `BLOCKED` 状态可解除阻塞。重复解除未阻塞流程返回成功，但不得重复写审计。解除后状态回到阻塞前状态，若阻塞前状态不可判断则回到 `IN_PROGRESS` 并重新计算当前步骤。

### onboarding 审计列表

`GET /api/v1/onboarding/admin/audit-logs`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `applicationId` | string | 否 | 流程 ID。 |
| `actorUserId` | string | 否 | 操作者用户 ID。 |
| `action` | string | 否 | 动作，最多 80 位。 |
| `result` | string | 否 | `SUCCESS` 或 `FAILED`。 |
| `from` | string | 否 | ISO 8601 起始时间。 |
| `to` | string | 否 | ISO 8601 结束时间。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`。默认 `createdAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `OnboardingAuditLog[]`。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 返回 `42001`。审计日志不得通过 onboarding API 删除。返回结果必须脱敏。

### onboarding 自检摘要

`GET /api/v1/onboarding/admin/ops/summary`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "onboarding",
    "port": 8131,
    "legacyPort": 8108,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "profileMode": "TEST_STUB",
    "contentMode": "TEST_STUB",
    "notificationMode": "TEST_STUB",
    "applicationsTotal": 8,
    "blockedTotal": 1,
    "readyForExamTotal": 2,
    "stateMachineMode": "EXPLICIT_P0",
    "handoffSnapshotsTotal": 0,
    "auditsTotal": 20,
    "idempotencyRecordsTotal": 5,
    "lastAuditAt": "2026-05-22T12:00:00Z",
    "productionGaps": [
      "P0_IN_MEMORY_STORAGE",
      "P0_AUTH_STUB",
      "P0_PROFILE_STUB",
      "P0_CONTENT_STUB",
      "P0_NOTIFICATION_STUB",
      "EXAM_NOT_IMPLEMENTED",
      "WHITELIST_NOT_IMPLEMENTED"
    ]
  }
}
```

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。`port` 固定返回当前运行入口 `8131`，`legacyPort` 固定返回历史原服务端口 `8108`。`stateMachineMode` 固定为 `EXPLICIT_P0`，表示流程推进集中按契约状态机判定。`handoffSnapshotsTotal` 统计本进程生成过的 exam 交接快照次数。自检摘要不得返回 token、请求头、Minecraft 验证凭据、通知正文、content 正文、后台备注全文、审计参数全文或异常堆栈。

## 状态、幂等和并发

新流程初始状态为 `IN_PROGRESS`。未创建流程时只读视图为 `NOT_STARTED`。`IN_PROGRESS` 在后台阻塞后进入 `BLOCKED`，解除阻塞后回到阻塞前状态或重新计算为 `IN_PROGRESS`。当前用户完成资料确认、规则确认和方向选择后，通过推进接口进入 `READY_FOR_EXAM`。`READY_FOR_EXAM` 之后的 `WAITING_EXAM`、`READY_FOR_WHITELIST`、`WAITING_WHITELIST`、`COMPLETED` 和 `CANCELLED` 只作为后续兼容占位，P0 不主动写入。

步骤推进顺序固定为 `ACCOUNT_READY`、`MINECRAFT_BOUND`、`PROFILE_CONFIRMED`、`RULES_CONFIRMED`、`DIRECTION_SELECTED`、`EXAM_READY`、`WHITELIST_READY`。服务端可以根据 auth Minecraft 绑定自动完成 `MINECRAFT_BOUND`，但不能自动完成资料确认、规则确认或方向选择。非法跳跃返回 `43811` 或 `43810`。

创建或恢复流程、资料确认、规则确认、方向选择、推进下一步、后台重置、阻塞和解除阻塞支持 `idempotencyKey`。同一操作者、同一幂等键、同一请求体重复提交时返回同一结果。相同幂等键搭配不同请求体返回 `43817`。幂等请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象都按字段名递归排序，不能受浏览器字段顺序影响。

并发启动同一用户流程只能产生一个未取消流程。并发确认、方向选择和推进必须以服务端当前状态为准，不得产生半完成步骤。公开读取允许读到更新前或更新后的完整状态，不能返回半更新对象。

## 审计要求

必须审计的动作包括启动流程、确认资料、确认规则、选择方向、推进到考试准备、后台重置、后台阻塞、后台解除阻塞、通知强制投递失败、审计写入失败和依赖降级导致状态不可推进。

当前用户关键动作写低风险审计。后台写操作必须记录 `reason`、操作者、目标流程、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计写入失败时，状态写操作不得假装成功，必须返回 `51801` 或 `51800`，并保持业务数据不变。

## 失败降级

auth 是强依赖。auth 不可用、超时、用户状态不允许或上下文字段不兼容时，当前用户接口和后台接口不得伪造成功。

profile 是资料确认和成员状态判定强依赖。读取进度可局部降级，写操作不得在 profile 不可用时确认资料或推进。已有缓存成员摘要只用于展示，不能作为准入判断。

content 是规则确认强依赖。读取进度可返回缓存规则摘要并标记降级；没有可用规则摘要时不得确认规则。规则版本过期必须要求用户重新确认。

notification 对当前用户普通流程是辅助依赖。通知失败时主状态可以成功，但必须写失败摘要。后台重置、阻塞、解除阻塞若 `notifyUser=true`，通知是强制依赖，失败时状态不得变化。

exam、whitelist 和 attendance 未实现时，onboarding 只返回下一步占位，不得整体失败，也不得伪造下游结果。

## 验收口径

`onboarding` API 文档按 `docs/contracts-onboarding.md` 独立存在，并由 `.local-docs/tests-onboarding.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`onboarding` 完成时必须满足以下条件：全部接口按本文档实现；当前用户接口只能访问当前用户自己的流程；后台接口按角色限制；服务端决定状态和下一步，不信任浏览器传入的可信字段；auth、profile、content 和 notification 适配不直接读取前序服务数据库或内部类；规则确认、资料确认、方向选择、推进、阻塞、重置、幂等、审计、自检摘要、requestId 和端口配置都有自动化测试；当前运行入口为 `admission-core-service:8131`，历史端口只作为 `legacyPort=8108` 返回；默认关闭测试控制头，直连伪造 `X-Beiming-Actor-*` 不能绕过 Bearer，网关注入可信上下文可被识别；`.local-docs/tests-onboarding.md` 中全部测试用例都有对应自动化验证；未实现时自动化测试必须先失败；实现后 onboarding 全部测试通过；auth、profile、notification、content、server-status、resource 和 admin 前序服务回归测试通过；没有修改前序服务稳定接口；没有把考试判分、白名单审核、成员激活、考勤积分、社区、资源下载、服务器状态采集、后台聚合、真实运维、节点、容器、终端、文件管理、备份恢复或 Cloudreve 管理能力塞进 onboarding。
