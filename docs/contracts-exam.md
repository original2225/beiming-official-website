# 北冥官网 exam API 契约

版本：0.1

## 文档定位

本文档是 `exam` 微服务的正式 API 契约。后续 `whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、前端适配和 `admin` 聚合只能通过本文档定义的接口读取考试结果、考试待办和考试审计摘要，不能直接读取或修改 `exam` 数据库，也不能把白名单审核、成员激活、考勤积分或社区工单逻辑塞进 `exam`。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `exam` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了现有 Minecraft 服务器白名单审核和成熟在线考试平台的通用做法。Minecraft 服务器通过白名单控制入服资格，`enforce-whitelist` 会让不在白名单中的在线玩家在白名单重载后被踢出；北冥的考试只负责准入评估，不直接操作服务器白名单。Moodle Quiz 把题库和测验分开，支持时间限制、尝试次数、提交后反馈、随机题和人工评分题；Google Forms Quiz 支持自动评分，也支持人工审核后再发布成绩。北冥吸收这些适合入服审核的边界：题库与试卷分离、题库版本冻结、客观题自动判分、简答题人工阅卷、考试结果延迟发布、重考冷却和审计追踪。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Minecraft Wiki server.properties](https://minecraft.fandom.com/wiki/Server.properties) | 白名单是服务器准入控制，不应由考试服务直接操作。 |
| [Moodle Quiz activity](https://docs.moodle.org/en/Quiz) | 题库与测验分离，题目可复用，测验有独立设置。 |
| [Moodle Quiz settings](https://docs.moodle.org/en/Quiz_settings) | 测验可配置时间限制、尝试间隔和提交策略。 |
| [Moodle Using Quiz](https://docs.moodle.org/en/Using_Quiz) | 考生可保存尝试并提交，作文题需要人工评分。 |
| [Google Forms quizzes](https://support.google.com/docs/answer/7032287) | 成绩可自动发布，也可人工审核后发布。 |

## 职责边界

`exam` 负责入服考试方向、题库、题目版本、试卷模板、试卷实例、答题草稿、提交记录、客观题自动判分、简答题人工阅卷、考试结果、重考策略、二次考核标记、给 `whitelist` 的只读考试结果交接摘要、幂等记录和自身审计。

`exam` 不负责注册、登录、邀请码、会话、账号角色能力点、Minecraft 绑定主数据、入服前置流程主数据、成员档案主数据、站内通知主数据、官网内容主数据、白名单申请、白名单审核、成员激活、考勤积分初始化、社区工单、服务器状态展示、资源下载、Cloudreve 分享、后台聚合入口或任何真实服务器运维操作。

`exam` 只能适配前序服务。它通过 `auth` 认证上下文读取当前用户和后台操作者，通过 `onboarding` 的考试交接快照创建考试，通过 `profile` 摘要判断已有成员与二次考核，通过 `content` 摘要读取考试说明版本，通过 `notification` 投递考试状态通知。它不能要求前序服务反向写入 exam 状态，不能导入前序服务的内存存储、实体、Repository、测试种子或内部类。

## 数据归属

`exam` 拥有以下主数据：考试流程、题库题目、题目版本、试卷模板、试卷实例、答题记录、自动判分结果、人工阅卷记录、考试结果、重考策略、给 whitelist 的只读交接快照、幂等记录、通知调用摘要和 exam 审计日志。

`exam` 可以保存来自 `onboarding` 的 `applicationId`、`handoffVersion`、`userId`、`displayNameSnapshot`、`minecraftBindingSnapshot`、`profileConfirmation`、`ruleConfirmation` 和 `reviewDirection` 快照。它可以保存来自 `profile` 的成员状态摘要、来自 `content` 的考试说明摘要、来自 `notification` 的投递结果摘要。快照不是来源模块主数据，不能用于反写来源模块。

## 基础路径与认证

所有接口默认使用 `/api/v1/exams` 前缀。P0 端口固定为 `8109`，自检摘要必须返回该端口。

当前用户接口使用 `/api/v1/exams/me` 前缀，全部要求 `Authorization: Bearer <token>`，只能访问当前认证用户自己的考试。浏览器请求体不得传入 `userId`、`roles`、`permissions`、`minecraftBindingSnapshot`、`score`、`passed`、`reviewerId`、`status`、`createdBy`、`updatedBy` 等服务端可信字段。

后台接口使用 `/api/v1/exams/admin` 前缀，全部要求登录。后台读取考试、题库、模板、审计和自检摘要要求 `HELPER`、`ADMIN` 或 `OWNER`。题库维护、模板维护、人工阅卷、要求补充、取消考试、结果修正和策略更新要求 `ADMIN` 或 `OWNER`。`HELPER` 可读取待阅卷和考试详情，但不能写题库、模板或最终结果。

## 前序服务兼容契约

`auth` 是强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions`、`status` 和 `minecraftBinding`。用户状态为 `PENDING_PROFILE` 或 `ACTIVE` 时可参加入服考试；`DISABLED`、`BANNED`、`DELETED` 不允许创建、答题或提交。auth 不可用返回 `46900`，auth 超时返回 `46901`，字段或枚举不兼容返回 `46902`。

`onboarding` 是创建考试的强依赖。创建考试时必须读取 `GET /api/v1/onboarding/admin/applications/{applicationId}/exam-handoff` 或未来等价服务间适配器。只有交接快照满足 `readyForExam=true`、`handoffAllowed=true`、`status=READY_FOR_EXAM`、`targetModule=EXAM`，且 `reviewDirection` 为 `REDSTONE`、`LATE_GAME`、`BUILDING` 或 `GENERAL` 时，才允许创建考试。onboarding 不可用返回 `46910`，超时返回 `46911`，字段不兼容返回 `46912`，交接状态不满足返回 `43910`。

`profile` 是创建考试时的成员状态判定依赖。已有 `ACTIVE` 或 `INACTIVE` 成员档案的用户不允许创建新玩家考试，返回 `43911`。`REMOVED` 成员允许创建二次考核，`attemptType=RECHECK`，`difficulty=RECHECK`，试卷必须使用二次考核模板。profile 不可用返回 `46920`，超时返回 `46921`，字段不兼容返回 `46922`。读取已有考试、试卷、结果时可以使用 exam 已保存快照降级返回。

`content` 用于读取考试说明和规则说明摘要。创建考试时如模板要求绑定说明版本，content 是强依赖；已生成的试卷读取、保存草稿和提交不因 content 当前不可用而失败。content 不可用返回 `46930`，超时返回 `46931`，字段不兼容返回 `46932`，规则说明版本不匹配返回 `43912`。

`notification` 用于投递考试状态通知。考试创建、提交、自动失败、进入人工阅卷、人工阅卷通过、人工阅卷失败、要求补充、补充提交、考试过期和后台取消都应尝试通知。通知是辅助依赖，除本文档单独说明的强制通知场景外，通知失败不得回滚考试主状态，但必须记录 `notificationStatus=FAILED`、失败原因摘要和审计。notification 不可用返回或记录 `46940`，超时返回或记录 `46941`，字段不兼容返回或记录 `46942`。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `ReviewDirection` | `REDSTONE`、`LATE_GAME`、`BUILDING`、`GENERAL` | 考试方向，必须兼容 onboarding。 |
| `QuestionType` | `SINGLE_CHOICE`、`MULTIPLE_CHOICE`、`TRUE_FALSE`、`SHORT_TEXT` | P0 题型。 |
| `QuestionStatus` | `DRAFT`、`ACTIVE`、`ARCHIVED` | 题目状态。已进入试卷的题目版本仍可追溯。 |
| `PaperTemplateStatus` | `DRAFT`、`PUBLISHED`、`ARCHIVED` | 试卷模板状态。只有已发布模板可生成考试。 |
| `AttemptType` | `FIRST_TIME`、`RECHECK` | 首次入服考试或二次考核。 |
| `ExamDifficulty` | `NORMAL`、`RECHECK` | 二次考核必须使用 `RECHECK`。 |
| `ExamSessionStatus` | `CREATED`、`IN_PROGRESS`、`SUBMITTED`、`AUTO_PASSED`、`AUTO_FAILED`、`PENDING_MANUAL_REVIEW`、`NEEDS_SUPPLEMENT`、`SUPPLEMENT_SUBMITTED`、`MANUAL_PASSED`、`MANUAL_FAILED`、`EXPIRED`、`CANCELLED` | 考试内部状态。 |
| `ExamResult` | `PENDING`、`PASSED`、`FAILED`、`NEEDS_SUPPLEMENT`、`EXPIRED`、`CANCELLED` | 对外归一结果。 |
| `NotificationStatus` | `DELIVERED`、`FAILED`、`SKIPPED` | 最近一次通知摘要。 |
| `ExamAuditResult` | `SUCCESS`、`FAILED` | exam 审计结果。 |

## 通用对象

### ExamSession

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sessionId` | string | 是 | 考试实例 ID。 |
| `applicationId` | string | 是 | onboarding 流程实例 ID。 |
| `handoffVersion` | integer | 是 | onboarding 交接快照版本。 |
| `userId` | string | 是 | auth 用户 ID。当前用户接口固定为认证用户。 |
| `displayNameSnapshot` | string | 是 | 创建考试时的展示名快照。 |
| `minecraftBindingSnapshot` | object | 是 | Minecraft 绑定快照，兼容 auth 的 `MinecraftBinding`。 |
| `reviewDirection` | string | 是 | `ReviewDirection`。 |
| `attemptType` | string | 是 | `FIRST_TIME` 或 `RECHECK`。 |
| `difficulty` | string | 是 | `NORMAL` 或 `RECHECK`。 |
| `status` | string | 是 | `ExamSessionStatus`。 |
| `result` | string | 是 | `ExamResult`。 |
| `templateId` | string | 是 | 使用的试卷模板 ID。 |
| `templateVersion` | integer | 是 | 使用的模板版本。 |
| `paperId` | string | 是 | 试卷实例 ID。 |
| `scoreSummary` | ExamScoreSummary 或 null | 是 | 成绩摘要。未提交时为 `null`。 |
| `manualReview` | ExamManualReview 或 null | 是 | 最近一次人工阅卷记录。 |
| `supplementRequest` | object 或 null | 是 | 补充要求摘要。 |
| `notificationStatus` | string 或 null | 是 | 最近一次通知结果。 |
| `degraded` | boolean | 是 | 是否存在局部降级。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |
| `startedAt` | string | 是 | 考试创建时间。 |
| `lastSavedAt` | string 或 null | 是 | 最近保存草稿时间。 |
| `submittedAt` | string 或 null | 是 | 首次提交时间。 |
| `reviewedAt` | string 或 null | 是 | 最近人工阅卷时间。 |
| `expiresAt` | string | 是 | 考试截止时间。 |
| `passedAt` | string 或 null | 是 | 通过时间。 |
| `cancelledAt` | string 或 null | 是 | 取消时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ExamQuestion

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `questionId` | string | 是 | 题目 ID。 |
| `version` | integer | 是 | 题目版本，从 `1` 开始。 |
| `type` | string | 是 | `QuestionType`。 |
| `reviewDirection` | string | 是 | 适用方向。 |
| `difficulty` | string | 是 | `ExamDifficulty`。 |
| `stem` | string | 是 | 题干，最多 2000 位。 |
| `options` | ExamQuestionOption[] | 是 | 客观题选项，简答题为空数组。 |
| `correctOptionIds` | string[] | 后台必返 | 正确选项 ID。考生视图不得返回。 |
| `referenceAnswer` | string 或 null | 后台必返 | 简答参考答案。考生视图不得返回。 |
| `score` | integer | 是 | 分值，1 到 100。 |
| `tags` | string[] | 是 | 标签，最多 10 个。 |
| `status` | string | 是 | `QuestionStatus`。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |

### ExamQuestionOption

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `optionId` | string | 是 | 选项 ID。 |
| `label` | string | 是 | 展示标签，例如 `A`。 |
| `text` | string | 是 | 选项文本，最多 500 位。 |

### ExamPaper

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `paperId` | string | 是 | 试卷实例 ID。 |
| `sessionId` | string | 是 | 考试实例 ID。 |
| `templateId` | string | 是 | 模板 ID。 |
| `templateVersion` | integer | 是 | 模板版本。 |
| `reviewDirection` | string | 是 | 考试方向。 |
| `attemptType` | string | 是 | 考试类型。 |
| `timeLimitMinutes` | integer | 是 | 考试时长。 |
| `questions` | ExamQuestion[] | 是 | 考生视图不含正确答案和参考答案。 |
| `totalScore` | integer | 是 | 总分。 |
| `objectiveTotalScore` | integer | 是 | 客观题总分。 |
| `manualTotalScore` | integer | 是 | 人工阅卷题总分。 |
| `generatedAt` | string | 是 | 生成时间。 |

### ExamAnswerItem

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `questionId` | string | 是 | 题目 ID。 |
| `selectedOptionIds` | string[] | 视题型 | 客观题选项。单选和判断必须为 1 个，多选至少 1 个。 |
| `textAnswer` | string 或 null | 视题型 | 简答题答案，最多 2000 位。 |

### ExamAnswerSheet

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sessionId` | string | 是 | 考试实例 ID。 |
| `answers` | ExamAnswerItem[] | 是 | 答案列表。 |
| `draft` | boolean | 是 | 是否为草稿。 |
| `savedAt` | string | 是 | 保存时间。 |
| `submittedAt` | string 或 null | 是 | 提交时间。 |

### ExamScoreSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `objectiveScore` | integer | 是 | 客观题得分。 |
| `manualScore` | integer 或 null | 是 | 人工题得分。未阅卷为 `null`。 |
| `totalScore` | integer 或 null | 是 | 总分。未阅卷为 `null`。 |
| `objectivePassed` | boolean | 是 | 客观题是否达到客观题最低线。 |
| `finalPassed` | boolean 或 null | 是 | 最终是否通过。未完成阅卷为 `null`。 |
| `passScore` | integer | 是 | 最终通过线。 |
| `objectivePassScore` | integer | 是 | 客观题最低线。 |
| `manualRequired` | boolean | 是 | 是否需要人工阅卷。 |
| `scoredAt` | string | 是 | 最近判分时间。 |

### ExamManualReview

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `reviewId` | string | 是 | 阅卷记录 ID。 |
| `reviewerUserId` | string | 是 | 阅卷人用户 ID。 |
| `reviewerDisplayNameSnapshot` | string | 是 | 阅卷人展示名快照。 |
| `manualScores` | object[] | 是 | 每道人工题得分和评语。 |
| `publicComment` | string | 是 | 给考生看的评语，最多 1000 位。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注。考生视图不得返回。 |
| `result` | string | 是 | `PASSED`、`FAILED` 或 `NEEDS_SUPPLEMENT`。 |
| `reviewedAt` | string | 是 | 阅卷时间。 |

### PaperTemplate

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `templateId` | string | 是 | 模板 ID。 |
| `version` | integer | 是 | 模板版本。 |
| `name` | string | 是 | 模板名称，最多 80 位。 |
| `reviewDirection` | string | 是 | 适用方向。 |
| `difficulty` | string | 是 | `NORMAL` 或 `RECHECK`。 |
| `status` | string | 是 | `PaperTemplateStatus`。 |
| `timeLimitMinutes` | integer | 是 | 15 到 180。 |
| `passScore` | integer | 是 | 最终通过线。 |
| `objectivePassScore` | integer | 是 | 客观题最低线。 |
| `questionRules` | object[] | 是 | 按题型、标签、数量和分值配置的抽题规则。 |
| `contentRuleVersion` | string 或 null | 是 | 绑定的考试说明版本。 |
| `retakeCooldownHours` | integer | 是 | 重考冷却小时数。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `publishedAt` | string 或 null | 是 | 发布时间。 |

### ExamWhitelistHandoffSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sessionId` | string | 是 | 考试实例 ID。 |
| `applicationId` | string | 是 | onboarding 流程实例 ID。 |
| `handoffVersion` | integer | 是 | exam 交接快照版本，从 `1` 开始。 |
| `onboardingHandoffVersion` | integer | 是 | 来源 onboarding 交接版本。 |
| `userId` | string | 是 | 用户 ID。 |
| `minecraftBindingSnapshot` | object | 是 | Minecraft 绑定快照。 |
| `reviewDirection` | string | 是 | 审核方向。 |
| `attemptType` | string | 是 | 首次或二次考核。 |
| `result` | string | 是 | 必须为 `PASSED` 才允许 whitelist 创建申请。 |
| `scoreSummary` | ExamScoreSummary | 是 | 成绩摘要。 |
| `passedAt` | string | 是 | 通过时间。 |
| `reviewerSnapshot` | object 或 null | 是 | 人工阅卷人快照。客观题自动通过时为 `null`。 |
| `generatedAt` | string | 是 | 生成时间。 |

### ExamAuditLog

审计字段继承公共契约，允许补充 `sessionId`、`questionId`、`templateId`、`applicationId`、`handoffVersion`、`stateFrom`、`stateTo`、`reviewDirection`、`attemptType`、`idempotencyKey`、`notificationStatus`、`dependencyStatus` 和 `scoreSummary`。审计日志不得通过 exam API 删除。

## exam 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `43900` | 404 | 考试实例不存在，或当前用户无权访问。 |
| `43901` | 404 | 试卷不存在。 |
| `43902` | 404 | 题目不存在。 |
| `43903` | 404 | 试卷模板不存在。 |
| `43904` | 404 | 审计记录不存在。 |
| `43910` | 409 | onboarding 交接状态不允许创建考试。 |
| `43911` | 409 | 当前用户已有正式成员档案，不允许创建新玩家考试。 |
| `43912` | 409 | 考试说明或规则版本不匹配。 |
| `43913` | 409 | 当前考试状态不允许该操作。 |
| `43914` | 409 | 没有可用已发布试卷模板。 |
| `43915` | 409 | 题库题目不足，无法生成试卷。 |
| `43916` | 409 | 答案缺失、题型不匹配或选项非法。 |
| `43917` | 409 | 考试已过期。 |
| `43918` | 409 | 考试提交后不可修改答案。 |
| `43919` | 409 | exam 幂等键请求指纹冲突。 |
| `43920` | 409 | 重考冷却未结束。 |
| `43921` | 409 | 题目已被发布模板或历史试卷引用，不能破坏历史版本。 |
| `43922` | 409 | 模板状态不允许该操作。 |
| `43923` | 409 | 人工阅卷分数超出题目分值。 |
| `43924` | 409 | 只有通过结果可生成 whitelist 交接快照。 |
| `46900` | 502 | auth 认证上下文不可用。 |
| `46901` | 504 | auth 认证上下文调用超时。 |
| `46902` | 502 | auth 认证上下文字段或枚举不兼容 exam 契约。 |
| `46910` | 502 | onboarding 交接快照不可用。 |
| `46911` | 504 | onboarding 交接快照调用超时。 |
| `46912` | 502 | onboarding 交接快照字段不兼容 exam 契约。 |
| `46920` | 502 | profile 摘要不可用。 |
| `46921` | 504 | profile 摘要调用超时。 |
| `46922` | 502 | profile 摘要字段或枚举不兼容 exam 契约。 |
| `46930` | 502 | content 考试说明不可用。 |
| `46931` | 504 | content 考试说明调用超时。 |
| `46932` | 502 | content 考试说明字段不兼容 exam 契约。 |
| `46940` | 502 | notification 强制投递不可用。 |
| `46941` | 504 | notification 强制投递超时。 |
| `46942` | 502 | notification 投递响应不兼容 exam 契约。 |
| `51900` | 500 | exam 内部错误。 |
| `51901` | 500 | exam 审计写入失败。 |
| `51902` | 500 | exam 状态写入失败。 |
| `51903` | 500 | exam 判分失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误和通用服务端错误优先使用公共错误码。exam 自有幂等指纹冲突使用 `43919`。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 创建或恢复当前考试 | POST | `/api/v1/exams/me/sessions` | 是 | 当前用户 | LOW |
| 当前考试 | GET | `/api/v1/exams/me/sessions/current` | 是 | 当前用户 | LOW |
| 当前用户考试历史 | GET | `/api/v1/exams/me/sessions` | 是 | 当前用户 | LOW |
| 读取试卷 | GET | `/api/v1/exams/me/sessions/{sessionId}/paper` | 是 | 当前用户 | LOW |
| 保存答案草稿 | PUT | `/api/v1/exams/me/sessions/{sessionId}/answers` | 是 | 当前用户 | LOW |
| 提交考试 | POST | `/api/v1/exams/me/sessions/{sessionId}/submit` | 是 | 当前用户 | MEDIUM |
| 补充答案 | PATCH | `/api/v1/exams/me/sessions/{sessionId}/supplement` | 是 | 当前用户 | MEDIUM |
| 读取结果 | GET | `/api/v1/exams/me/sessions/{sessionId}/result` | 是 | 当前用户 | LOW |
| 后台考试列表 | GET | `/api/v1/exams/admin/sessions` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台考试详情 | GET | `/api/v1/exams/admin/sessions/{sessionId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 人工阅卷 | PATCH | `/api/v1/exams/admin/sessions/{sessionId}/manual-review` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 要求补充 | PATCH | `/api/v1/exams/admin/sessions/{sessionId}/request-supplement` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 取消考试 | PATCH | `/api/v1/exams/admin/sessions/{sessionId}/cancel` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| whitelist 交接快照 | GET | `/api/v1/exams/admin/sessions/{sessionId}/whitelist-handoff` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 后台题目列表 | GET | `/api/v1/exams/admin/question-bank/questions` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建题目 | POST | `/api/v1/exams/admin/question-bank/questions` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改题目 | PATCH | `/api/v1/exams/admin/question-bank/questions/{questionId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档题目 | PATCH | `/api/v1/exams/admin/question-bank/questions/{questionId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 模板列表 | GET | `/api/v1/exams/admin/paper-templates` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建模板 | POST | `/api/v1/exams/admin/paper-templates` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改模板 | PATCH | `/api/v1/exams/admin/paper-templates/{templateId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 发布模板 | PATCH | `/api/v1/exams/admin/paper-templates/{templateId}/publish` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档模板 | PATCH | `/api/v1/exams/admin/paper-templates/{templateId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| exam 审计列表 | GET | `/api/v1/exams/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| exam 自检摘要 | GET | `/api/v1/exams/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 当前用户接口

### 创建或恢复当前考试

`POST /api/v1/exams/me/sessions`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `applicationId` | string | 是 | onboarding 流程实例 ID。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `ExamSession`。若当前用户已有同一 `applicationId` 下未结束考试，返回 HTTP `200` 和现有考试。若最近一次考试失败但重考冷却未结束，返回 `43920`。

业务规则：服务端必须读取 onboarding 交接快照和 profile 成员状态，选择与 `reviewDirection`、`attemptType`、`difficulty` 匹配的已发布模板，生成确定性试卷实例，并冻结题目版本、模板版本、考试说明版本和交接快照。题库不足、模板缺失或前序依赖失败不得创建半成品考试。创建成功后状态为 `IN_PROGRESS`。创建必须写入 `EXAM_SESSION_CREATED` 审计，审计失败返回 `51901`，不得创建考试。

幂等规则：同一用户、同一 `idempotencyKey`、同一请求体重复提交时返回同一考试。相同幂等键搭配不同请求体返回 `43919`。请求体指纹必须基于结构化 JSON 规范化结果。

### 当前考试

`GET /api/v1/exams/me/sessions/current`

成功响应 HTTP `200`，`data` 为 `ExamSession` 或 `null`。只返回当前用户最近一个未结束考试。已通过、已失败、已取消或已过期的考试不作为当前考试返回。

业务规则：profile、content 或 notification 当前不可用时，可返回 exam 已保存快照并标记 `degraded=true`。auth 不可用不得返回成功。读取不会创建考试，不写审计。

### 当前用户考试历史

`GET /api/v1/exams/me/sessions`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `status` | string | 否 | 任一 `ExamSessionStatus`。 |
| `result` | string | 否 | 任一 `ExamResult`。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`、`submittedAt_desc`、`updatedAt_desc`。默认 `createdAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `ExamSession[]`。只能返回当前用户自己的考试，不返回正确答案、参考答案、内部备注或审计参数。

### 读取试卷

`GET /api/v1/exams/me/sessions/{sessionId}/paper`

成功响应 HTTP `200`，`data` 为 `ExamPaper` 的考生视图。考生视图永远不得返回 `correctOptionIds`、`referenceAnswer`、判分规则内部细节、管理员备注、审计参数或其他用户信息。

业务规则：考试必须属于当前用户，状态必须为 `IN_PROGRESS` 或 `NEEDS_SUPPLEMENT`。考试过期时返回 `43917`，并把状态推进为 `EXPIRED`，写入审计和通知失败摘要。已提交但待人工阅卷、已补充提交等待复审、已最终通过、已最终失败或已取消的考试不能重新读取可编辑试卷，返回 `43913`。

### 保存答案草稿

`PUT /api/v1/exams/me/sessions/{sessionId}/answers`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `answers` | ExamAnswerItem[] | 是 | 只能包含试卷中的题目。未答题可传空数组。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为 `ExamAnswerSheet`。

业务规则：只允许 `IN_PROGRESS` 状态保存草稿。`NEEDS_SUPPLEMENT` 必须使用补充接口。提交后、过期后、取消后不可保存，分别返回 `43918`、`43917` 或 `43913`。草稿答案必须校验题型、选项、简答长度和重复题目。保存草稿不判分，不通知，不泄露正确答案。

### 提交考试

`POST /api/v1/exams/me/sessions/{sessionId}/submit`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `answers` | ExamAnswerItem[] | 是 | 必须覆盖全部必答题。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为 `ExamSession`。

业务规则：提交只允许 `IN_PROGRESS` 状态。提交时必须重新校验考试未过期和答案完整性。单选和判断完全匹配得分，否则 0 分。多选必须完全匹配正确选项才得分，漏选或错选均 0 分。客观题达到最终通过线且无简答题时进入 `AUTO_PASSED`，结果为 `PASSED`。客观题低于客观题最低线时进入 `AUTO_FAILED`，结果为 `FAILED`。存在简答题且客观题达到最低线时进入 `PENDING_MANUAL_REVIEW`，结果为 `PENDING`。提交成功写入审计，通知失败不回滚主状态。

### 补充答案

`PATCH /api/v1/exams/me/sessions/{sessionId}/supplement`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `answers` | ExamAnswerItem[] | 是 | 只允许补充被要求补充的简答题。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一用户 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为 `ExamSession`。

业务规则：只允许 `NEEDS_SUPPLEMENT` 状态。补充不得修改客观题，不得修改未被要求补充的题目。提交后状态为 `SUPPLEMENT_SUBMITTED`，结果为 `PENDING`，等待后台重新人工阅卷。补充截止时间沿用补充要求中的 `supplementDueAt`，超时返回 `43917` 并进入 `EXPIRED`。

### 读取结果

`GET /api/v1/exams/me/sessions/{sessionId}/result`

成功响应 HTTP `200`，`data` 为 `ExamSession` 的结果视图。

业务规则：未提交考试返回 `43913`。`PENDING_MANUAL_REVIEW` 和 `SUPPLEMENT_SUBMITTED` 返回 `result=PENDING`，不得提前泄露人工阅卷内部备注。`NEEDS_SUPPLEMENT` 返回公开补充要求。最终失败可返回公开评语和重考冷却时间。最终通过可返回 `passedAt`，但不得返回 whitelist 申请已创建。

## 后台考试接口

### 后台考试列表

`GET /api/v1/exams/admin/sessions`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配考试 ID、流程 ID、用户 ID、展示名或 Minecraft ID，最多 80 位。 |
| `status` | string | 否 | 任一 `ExamSessionStatus`。 |
| `result` | string | 否 | 任一 `ExamResult`。 |
| `reviewDirection` | string | 否 | 任一 `ReviewDirection`。 |
| `attemptType` | string | 否 | `FIRST_TIME` 或 `RECHECK`。 |
| `needsManualReview` | boolean | 否 | 是否只看待阅卷。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`submittedAt_desc`、`updatedAt_desc`、`status_asc`、`score_desc`。默认 `updatedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `ExamSession[]`。列表不得返回正确答案、参考答案、完整答卷正文、内部备注或审计参数全文。

### 后台考试详情

`GET /api/v1/exams/admin/sessions/{sessionId}`

成功响应 HTTP `200`，`data` 包含 `ExamSession`、管理员视图试卷、答卷、判分明细和人工阅卷记录。`HELPER` 可读详情但看不到 `internalNote`。`ADMIN` 和 `OWNER` 可见内部备注。考试不存在返回 `43900`。

### 人工阅卷

`PATCH /api/v1/exams/admin/sessions/{sessionId}/manual-review`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `manualScores` | object[] | 是 | 每道简答题得分，不能超过题目分值。 |
| `result` | string | 是 | `PASSED` 或 `FAILED`。要求补充必须调用 request-supplement。 |
| `publicComment` | string | 是 | 1 到 1000 位。 |
| `internalNote` | string | 否 | 最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为更新后的 `ExamSession`。

业务规则：只允许 `PENDING_MANUAL_REVIEW` 或 `SUPPLEMENT_SUBMITTED`。阅卷人不能修改客观题原始答案和客观题得分。最终总分达到通过线且 `result=PASSED` 时状态为 `MANUAL_PASSED`，结果为 `PASSED`；否则状态为 `MANUAL_FAILED`，结果为 `FAILED`。审计失败或状态写入失败时不得改变结果。通知失败不回滚，但必须记录。

### 要求补充

`PATCH /api/v1/exams/admin/sessions/{sessionId}/request-supplement`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `questionIds` | string[] | 是 | 只允许简答题。 |
| `publicComment` | string | 是 | 1 到 1000 位，说明要补充什么。 |
| `supplementDueAt` | string | 是 | ISO 8601，必须晚于当前时间且不超过 14 天。 |
| `internalNote` | string | 否 | 最多 1000 位。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为更新后的 `ExamSession`。只允许 `PENDING_MANUAL_REVIEW` 或 `SUPPLEMENT_SUBMITTED`。成功后状态为 `NEEDS_SUPPLEMENT`，结果为 `NEEDS_SUPPLEMENT`。

### 取消考试

`PATCH /api/v1/exams/admin/sessions/{sessionId}/cancel`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `notifyUser` | boolean | 否 | 默认 `true`。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `200`，`data` 为更新后的 `ExamSession`。只允许取消 `IN_PROGRESS`、`PENDING_MANUAL_REVIEW`、`NEEDS_SUPPLEMENT` 或 `SUPPLEMENT_SUBMITTED` 状态的考试。已通过、已失败、已过期、已取消或已生成 whitelist 交接快照的考试不能取消，返回 `43913`。若 `notifyUser=true`，通知失败是辅助失败，不回滚取消，但必须记录失败摘要。

### whitelist 交接快照

`GET /api/v1/exams/admin/sessions/{sessionId}/whitelist-handoff`

成功响应 HTTP `200`，`data` 为 `ExamWhitelistHandoffSnapshot`。

业务规则：只有 `AUTO_PASSED` 或 `MANUAL_PASSED` 可生成快照，其他状态返回 `43924`。该接口只提供后续 `whitelist` 创建申请所需的只读快照，不创建白名单申请，不推进 onboarding 状态，不创建成员档案，不初始化考勤积分。读取是低风险读取，不强制写审计，但自检中的 `whitelistHandoffSnapshotsTotal` 必须递增。

## 后台题库接口

### 后台题目列表

`GET /api/v1/exams/admin/question-bank/questions`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配题干或标签，最多 80 位。 |
| `type` | string | 否 | 任一 `QuestionType`。 |
| `reviewDirection` | string | 否 | 任一 `ReviewDirection`。 |
| `difficulty` | string | 否 | 任一 `ExamDifficulty`。 |
| `status` | string | 否 | 任一 `QuestionStatus`。 |
| `tag` | string | 否 | 标签。 |
| `sort` | string | 否 | 允许 `updatedAt_desc`、`createdAt_desc`、`score_desc`、`type_asc`。默认 `updatedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `ExamQuestion[]`。`HELPER` 可读题目和答案，用于阅卷准备，但不能修改题库。

### 创建题目

`POST /api/v1/exams/admin/question-bank/questions`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `type` | string | 是 | 任一 `QuestionType`。 |
| `reviewDirection` | string | 是 | 任一 `ReviewDirection`。 |
| `difficulty` | string | 是 | 任一 `ExamDifficulty`。 |
| `stem` | string | 是 | 1 到 2000 位。 |
| `options` | ExamQuestionOption[] | 视题型 | 客观题 2 到 6 个选项，判断题固定 2 个选项，简答题为空数组。 |
| `correctOptionIds` | string[] | 视题型 | 客观题必填。 |
| `referenceAnswer` | string | 视题型 | 简答题必填，最多 2000 位。 |
| `score` | integer | 是 | 1 到 100。 |
| `tags` | string[] | 否 | 最多 10 个。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位，同一操作者 24 小时内有效。 |

成功响应 HTTP `201`，`data` 为 `ExamQuestion`。创建后状态为 `DRAFT`。题目创建写审计。

### 修改题目

`PATCH /api/v1/exams/admin/question-bank/questions/{questionId}`

请求字段同创建题目，均可选，但 `reason` 必填。成功响应 HTTP `200`。

业务规则：修改题干、选项、答案、参考答案、分值、方向、难度或题型时必须创建新版本。已被历史试卷引用的旧版本不可被覆盖。题目处于 `ARCHIVED` 时不可修改，返回 `43913`。只修改标签或状态说明也要写审计。

### 归档题目

`PATCH /api/v1/exams/admin/question-bank/questions/{questionId}/archive`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为归档后的 `ExamQuestion`。归档不会删除历史版本。已发布模板仍可使用冻结版本生成历史追溯，但新发布模板不得引用已归档题目。

## 后台模板接口

### 模板列表

`GET /api/v1/exams/admin/paper-templates`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `reviewDirection` | string | 否 | 任一 `ReviewDirection`。 |
| `difficulty` | string | 否 | 任一 `ExamDifficulty`。 |
| `status` | string | 否 | 任一 `PaperTemplateStatus`。 |
| `sort` | string | 否 | 允许 `updatedAt_desc`、`publishedAt_desc`、`name_asc`。默认 `updatedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `PaperTemplate[]`。

### 创建模板

`POST /api/v1/exams/admin/paper-templates`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `name` | string | 是 | 1 到 80 位。 |
| `reviewDirection` | string | 是 | 任一 `ReviewDirection`。 |
| `difficulty` | string | 是 | 任一 `ExamDifficulty`。 |
| `timeLimitMinutes` | integer | 是 | 15 到 180。 |
| `passScore` | integer | 是 | 1 到总分。 |
| `objectivePassScore` | integer | 是 | 0 到客观题总分。 |
| `questionRules` | object[] | 是 | 每条包含 `type`、`count`、`scoreEach`、`tags`。 |
| `contentRuleVersion` | string 或 null | 否 | 考试说明版本。 |
| `retakeCooldownHours` | integer | 是 | 0 到 720。 |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `201`，`data` 为 `PaperTemplate`，状态为 `DRAFT`。模板总分由规则计算。创建不得要求题库足量，发布时必须校验题库足量。

### 修改模板

`PATCH /api/v1/exams/admin/paper-templates/{templateId}`

请求字段同创建模板，均可选，但 `reason` 必填。成功响应 HTTP `200`。修改已发布模板时必须创建新版本并回到 `DRAFT`，旧版本仍供历史试卷追溯。

### 发布模板

`PATCH /api/v1/exams/admin/paper-templates/{templateId}/publish`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为 `PaperTemplate`。发布前必须校验每条抽题规则都有足够 `ACTIVE` 题目，`contentRuleVersion` 若不为空必须能从 content 读取且仍有效。每个 `reviewDirection+difficulty` 同一时间至少允许一个已发布模板；如存在多个，创建考试选择最新 `publishedAt`。

### 归档模板

`PATCH /api/v1/exams/admin/paper-templates/{templateId}/archive`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `reason` | string | 是 | 1 到 200 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `200`，`data` 为归档后的 `PaperTemplate`。归档不影响已生成试卷。若归档导致某个方向和难度没有可用模板，后续创建考试返回 `43914`。

## 审计与自检接口

### exam 审计列表

`GET /api/v1/exams/admin/audit-logs`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `sessionId` | string | 否 | 考试 ID。 |
| `actorUserId` | string | 否 | 操作者用户 ID。 |
| `action` | string | 否 | 动作，最多 80 位。 |
| `result` | string | 否 | `SUCCESS` 或 `FAILED`。 |
| `from` | string | 否 | ISO 8601 起始时间。 |
| `to` | string | 否 | ISO 8601 结束时间。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`。默认 `createdAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `ExamAuditLog[]`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 exam API 删除，返回结果必须脱敏。

### exam 自检摘要

`GET /api/v1/exams/admin/ops/summary`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "exam",
    "port": 8109,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "onboardingMode": "TEST_STUB",
    "profileMode": "TEST_STUB",
    "contentMode": "TEST_STUB",
    "notificationMode": "TEST_STUB",
    "sessionsTotal": 8,
    "pendingManualReviewTotal": 2,
    "passedTotal": 3,
    "failedTotal": 2,
    "questionsTotal": 32,
    "publishedTemplatesTotal": 8,
    "whitelistHandoffSnapshotsTotal": 1,
    "auditsTotal": 40,
    "idempotencyRecordsTotal": 10,
    "lastAuditAt": "2026-05-23T12:00:00Z",
    "productionGaps": [
      "P0_IN_MEMORY_STORAGE",
      "P0_AUTH_STUB",
      "P0_ONBOARDING_STUB",
      "P0_PROFILE_STUB",
      "P0_CONTENT_STUB",
      "P0_NOTIFICATION_STUB",
      "WHITELIST_NOT_IMPLEMENTED"
    ]
  }
}
```

业务规则：自检摘要用于后台确认 exam 当前运行模式、题库规模、待阅卷规模、交接快照次数和生产化缺口。摘要不得返回 token、请求头、正确答案、参考答案、完整答卷、内部备注、审计参数全文、通知正文、content 正文、Minecraft 验证凭据或异常堆栈。

## 状态、判分、幂等和并发

考试创建成功后进入 `IN_PROGRESS`。`IN_PROGRESS` 可保存草稿或提交。提交后按判分结果进入 `AUTO_PASSED`、`AUTO_FAILED` 或 `PENDING_MANUAL_REVIEW`。`PENDING_MANUAL_REVIEW` 可进入 `MANUAL_PASSED`、`MANUAL_FAILED` 或 `NEEDS_SUPPLEMENT`。`NEEDS_SUPPLEMENT` 可由用户补充为 `SUPPLEMENT_SUBMITTED`，再由管理员进入最终通过、失败或再次要求补充。任何未最终结束的考试到期后进入 `EXPIRED`。后台可把尚未最终出结果且未生成 whitelist 交接快照的考试取消为 `CANCELLED`。

状态推进只能由服务端根据交接快照、模板、题库、时间、答案、自动判分、人工阅卷和后台动作决定。非法状态跳跃返回 `43913`。浏览器传入可信字段必须忽略或返回字段校验失败。

客观题判分固定为完全匹配。单选、判断只有选中唯一正确选项才得分。多选必须选项集合完全等于正确集合才得分，漏选、错选、多选均 0 分。简答题不自动通过，只由人工阅卷给分。

创建考试、保存草稿、提交考试、补充答案、人工阅卷、要求补充、取消考试、创建题目、修改题目、归档题目、创建模板、修改模板、发布模板和归档模板支持 `idempotencyKey`。同一操作者、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `43919`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序。

并发创建同一用户同一 `applicationId` 下只能产生一个未结束考试。并发提交同一考试只能有一个成功判分结果。并发人工阅卷必须以服务端当前状态为准，不能产生两个最终结果。读取接口允许读到更新前或更新后的完整状态，不能返回半更新对象。

## 审计要求

必须审计的动作包括创建考试、过期推进、保存草稿失败、提交考试、自动判分通过、自动判分失败、进入人工阅卷、人工阅卷通过、人工阅卷失败、要求补充、补充提交、后台取消、题目创建、题目修改、题目归档、模板创建、模板修改、模板发布、模板归档、依赖降级导致操作不可继续、通知失败和审计写入失败。

后台写操作必须记录 `reason`、操作者、目标对象、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计不得泄露 token、完整请求头、正确答案给考生、参考答案给考生、内部备注给 `HELPER` 或考生、完整通知正文、content 正文、异常堆栈或前序服务内部路径。

审计写入失败时，创建考试、提交考试、人工阅卷、要求补充、取消、题库维护和模板维护不得假装成功，必须返回 `51901` 或 `51900`，并保持业务数据不变。

## 失败降级

auth 是所有接口强依赖。auth 不可用、超时、用户状态不允许或字段不兼容时不得伪造成功。

onboarding 是创建考试强依赖。交接快照不可用、阻塞、过期、规则版本不匹配、Minecraft 绑定不完整或字段不兼容时，不得创建考试。

profile 是创建考试强依赖。读取当前考试、历史和结果时可使用已保存快照降级；创建考试时不能伪造成员状态。

content 对已冻结试卷不是强依赖。读取试卷、保存草稿、提交和阅卷不得因为 content 当前不可用而失败。创建考试或发布模板若绑定 content 说明版本，content 不可用或版本不匹配必须失败。

notification 默认是辅助依赖。通知失败不得回滚考试主状态，但必须记录失败摘要和审计。若未来引入强制通知动作，必须在本文档中新增说明和测试。

题库为空、模板缺失、试卷已过期、重复提交、答案缺失、题型不匹配、非法选项、简答超长、人工阅卷状态冲突、幂等键指纹冲突和审计失败都必须返回稳定错误码，不能吞成成功。

## 验收口径

`exam` API 文档按 `docs/contracts-exam.md` 独立存在，并由 `.local-docs/tests-exam.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`exam` 完成时必须满足以下条件：全部接口按本文档实现；当前用户接口只能访问自己的考试；后台接口按角色限制；考生视图不泄露正确答案、参考答案和内部备注；题库、模板、题目版本、试卷实例和答案快照可追溯；自动判分规则可测试；简答题人工阅卷和补充闭环可测试；创建考试只通过 onboarding handoff 和前序适配读取快照，不直接读前序服务实现；通过结果只暴露 whitelist 只读交接快照，不创建白名单申请；通知失败按辅助降级记录；端口固定为 `8109`；`.local-docs/tests-exam.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 exam 全部测试通过；auth、profile、notification、content、server-status、resource、admin 和 onboarding 前序服务回归测试通过；没有修改前序服务稳定接口；没有把白名单审核、成员激活、考勤积分、社区工单、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复或 Cloudreve 管理能力塞进 exam。
