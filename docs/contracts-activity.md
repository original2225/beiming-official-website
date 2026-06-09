# 北冥官网 activity API 契约

版本：0.1

## 文档定位

本文档是 `activity` 微服务的正式 API 契约。后续 `calendar`、`changelog`、前端适配、`admin` 聚合、`ops-control` 和 `external-node-executor` 只能通过本文档定义的接口读取活动、报名、参与确认、结果、奖励、贡献候选、审计和自检摘要，不能直接读取或修改 `activity` 数据库，也不能把活动报名、活动结果或活动奖励逻辑塞进其他服务。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `activity` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了成熟活动平台和社区平台的做法。Discord Guild Scheduled Event 的状态和时间模型说明活动必须由服务端维护明确状态机，终态不能随意回滚。Eventbrite 的 ticket class 和 inventory tier 思路说明名额不能只靠前端按钮限制，服务端必须按活动总名额、分组名额和已确认人数判断。Meetup 的 RSVP、候补和签到说明活动开始前、进行中、结束后要分开处理报名、候补、签到和缺席。Luma 的活动、报名审批、候补和 API 说明活动管理应保留审批、邀请、报名和数据同步边界。activity 本轮只吸收这些设计思路，不引入付费票务、外部支付、外部日历主数据或商业活动组织模型。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Discord Guild Scheduled Event](https://docs.discord.com/developers/resources/guild-scheduled-event) | 活动有开始、结束、状态和终态，服务端应限制状态流转。 |
| [Eventbrite Ticket Classes](https://www.eventbrite.com/platform/docs/ticket-classes) | 名额应由服务端库存模型和售出数量共同约束，不能只靠前端展示。 |
| [Meetup attendee and attendance management](https://help.meetup.com/hc/en-us/articles/9389668230541-Manage-attendees-and-track-attendance-for-your-Meetup-event-on-the-web) | 报名名单、候补名单、签到、缺席和活动后状态需要分开记录。 |
| [Meetup GraphQL API guide](https://www.meetup.com/graphql/guide/) | 活动草稿发布、RSVP 和报名问题应作为结构化接口，不写死在页面。 |
| [Luma API](https://help.luma.com/p/luma-api) | 活动 API 应覆盖活动、日历、邀请、报名、候补和统计，但本项目只保留 activity 自身主数据。 |

## 职责边界

`activity` 负责活动草稿、活动审核、活动发布、报名、候补、参与确认、签到、缺席、活动结果、获奖名单、奖励记录、贡献候选、通知投递摘要、活动审计、自检摘要和自身幂等记录。

`activity` 不负责注册、登录、邀请码、会话、账号角色能力点、Minecraft 绑定主数据、成员档案主数据、站内通知主数据、官网公告主发布、资源下载主数据、考勤积分主数据、社区帖子评论、工单举报处罚、日历主数据、更新日志主数据、后台聚合入口、真实服务器运维控制、节点守护进程、容器、终端、文件管理、备份恢复、Cloudreve 管理或外部支付。

`activity` 只能后序适配前序服务。它通过 `auth` 认证上下文读取当前用户、角色、能力点和用户状态；通过 `profile` 的正式接口或未来服务间适配器读取成员展示快照和成员状态；通过 `notification` 投递报名、候补、取消、签到、结果和奖励通知；通过 `attendance` 未来正式贡献入口接收贡献结果；通过 `community`、`content` 和 `resource` 的公开快照关联讨论、说明页和活动资源。`activity` 不能导入前序服务内存存储、实体、Repository、测试种子或内部类，不能要求前序服务为了 activity 反向修改稳定接口。

## 数据归属

`activity` 拥有以下主数据：活动、活动版本记录、报名记录、候补记录、参与确认记录、签到记录、活动结果、获奖名单、奖励记录、奖励发放摘要、活动贡献候选、通知投递摘要、依赖调用摘要、幂等记录、活动审计日志和自检统计。

`activity` 可以保存来自 `auth` 的 `userId`、`displayName`、`roles`、`permissions` 和用户状态快照；可以保存来自 `profile` 的成员展示名、头像、成员组、成员状态和 Minecraft ID 快照；可以保存来自 `notification` 的投递结果摘要；可以保存来自 `attendance` 的贡献接收或拒绝摘要；可以保存来自 `community`、`content` 和 `resource` 的公开对象快照。快照只服务展示、检索和审计，不能成为来源模块主数据，也不能反写来源模块。

## 基础路径与认证

所有接口默认使用 `/api/v1/activity` 前缀。当前运行入口为 `engagement-core-service:8132`。历史原服务端口 `8113` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或回归测试命令。

公开接口允许游客读取已发布、未下架、未归档、未软删除的数据。公开接口不得返回内部备注、报名审核参数、管理员 ID、通知失败详情、完整依赖错误、奖励后台备注、贡献候选内部原因或审计参数。

当前用户接口使用 `/api/v1/activity/me` 前缀，全部要求 `Authorization: Bearer <token>`。当前用户只能读取和维护自己的报名、候补、签到结果、获奖记录和奖励记录。浏览器请求体不得传入 `userId`、`memberId`、`roles`、`permissions`、`participantSnapshot`、`status`、`registrationStatus`、`checkInStatus`、`winnerStatus`、`rewardStatus`、`attendanceContributionStatus`、`notificationStatus`、`reviewerUserId`、`operatorUserId`、`auditResult` 等服务端可信字段。

后台接口使用 `/api/v1/activity/admin` 前缀，全部要求登录。后台读取活动、报名、结果、奖励、审计和自检摘要要求 `HELPER`、`ADMIN` 或 `OWNER`。活动创建、修改、审核、发布、下架、归档、报名管理、签到确认、结果录入、结果发布、奖励创建和奖励发放标记要求 `HELPER`、`ADMIN` 或 `OWNER`，但 `HELPER` 只能处理被授权的初审、报名确认和签到协助。活动软删除、奖励撤销、批量参与确认和贡献候选生成要求 `ADMIN` 或 `OWNER`。真实积分入账在本轮不开放。

## 本地测试控制头

`activity` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Profile-Mode`、`X-Test-Notification-Mode`、`X-Test-Attendance-Mode`、`X-Test-Community-Mode`、`X-Test-Content-Mode`、`X-Test-Resource-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store`、`X-Test-Now` 和 `X-Test-Fail-Registration` 模拟依赖失败、通知失败、写入失败、时间边界和报名并发冲突。该能力只服务本地测试闭环，不属于正式业务 API。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、状态失败、通知失败、报名失败或快照 stale。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 视为已满足的生产化硬化项。

## 前序服务兼容契约

`auth` 是所有登录接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions`、`status` 和可选 `minecraftBinding`。用户状态为 `ACTIVE` 时可报名和签到；`PENDING_PROFILE` 可以查看公开活动但不能报名成员限定活动；`DISABLED`、`BANNED`、`DELETED` 不允许写入活动状态。auth 不可用返回 `49400`，auth 超时返回 `49401`，字段或枚举不兼容返回 `49402`。

`profile` 是成员资格和展示快照的强依赖。成员限定活动、报名、获奖名单和签到确认必须读取成员快照。profile 不可用、超时或字段不兼容时，写入不得伪造成功，分别返回 `49410`、`49411` 或 `49412`。只读公开列表可以使用已有快照降级，但必须返回 `profileSnapshotStale=true` 或依赖摘要，且不得刷新为伪造资料。

`notification` 是辅助依赖。报名确认、报名拒绝、候补转正、取消报名、签到结果、结果发布、获奖和奖励发放可以触发通知。通知失败不得回滚活动主状态，但必须保存 `notificationStatus=FAILED`、失败原因摘要并写入审计。notification 不可用记录或返回 `49420`，超时记录或返回 `49421`，字段不兼容记录或返回 `49422`。通知失败摘要不得包含通知正文、完整请求头、认证 token、内部 URL 或异常堆栈。

`attendance` 当前只作为未来贡献入口。P1 中 `activity` 只能记录 `ActivityContributionCandidate`，不得直接写 attendance 积分余额、流水或榜单。后续需要活动奖励入账时，必须作为 `attendance` 兼容变更先补充契约、测试和回归，再由 `activity` 通过正式接口适配。attendance 不可用不得影响活动结果和奖励主流程，只能记录 `attendanceContributionStatus=SKIPPED` 或 `FAILED`。

`community` 是活动讨论、反馈和投票的可选关联来源。activity 可以保存 community 公开帖子、投票或反馈对象快照，但不能创建社区处罚，不能处理举报工单，不能直接读 community 内存存储或修改社区主状态。community 不可用时，创建关联活动返回 `49430`；读取已保存活动可展示旧快照并标记 stale。

`content` 是活动说明页、公告页和专题页的可选关联来源。activity 不能吞掉 content 的发布审核流程，不能直接创建官网公告。content 不可用时，创建关联活动返回 `49440`；公开读取可以展示已保存快照并标记 stale。

`resource` 是活动资源包、地图、规则文档或报名材料下载的可选关联来源。activity 不能生成 resource 下载票据，不能保存 Cloudreve 管理 token，不能做后台文件管理。resource 不可用时，创建关联活动返回 `49450`；公开读取可以展示已保存快照并标记 stale。

`admin` 当前只是聚合入口。本文档不修改 `admin` 契约。若后续需要 admin 展示 activity 待办、指标或审计索引，必须作为 admin 兼容变更单独走契约、测试、红灯、实现和回归闭环。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `ActivityType` | `BUILD`、`REDSTONE`、`SURVIVAL`、`PVP`、`COMMUNITY`、`MEETING`、`MAINTENANCE_PREP`、`OTHER` | 活动类型。`MAINTENANCE_PREP` 只表示活动准备，不等同运维维护窗口。 |
| `ActivityVisibility` | `PUBLIC`、`MEMBER_ONLY`、`STAFF_ONLY`、`INVITE_ONLY` | 活动可见和报名范围。 |
| `ActivityStatus` | `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`PUBLISHED`、`REGISTRATION_OPEN`、`REGISTRATION_CLOSED`、`RUNNING`、`COMPLETED`、`RESULT_PUBLISHED`、`OFFLINE`、`ARCHIVED`、`DELETED` | 活动主状态。 |
| `RegistrationPolicy` | `OPEN`、`APPROVAL_REQUIRED`、`INVITE_ONLY`、`STAFF_ASSIGNED` | 报名策略。 |
| `ActivityRegistrationStatus` | `SUBMITTED`、`CONFIRMED`、`WAITLISTED`、`REJECTED`、`CANCELED`、`CHECKED_IN`、`NO_SHOW` | 报名和参与状态。 |
| `CheckInMethod` | `MANUAL`、`CODE`、`ADMIN_CONFIRM` | 签到方式。P1 不开放二维码真实校验，只保留 `CODE` 字段和测试。 |
| `ActivityResultStatus` | `DRAFT`、`PUBLISHED`、`ARCHIVED` | 活动结果状态。 |
| `ActivityRewardType` | `POINTS_CANDIDATE`、`TITLE`、`ITEM`、`RESOURCE_ACCESS`、`CUSTOM` | 奖励类型。`POINTS_CANDIDATE` 只生成贡献候选，不直接加分。 |
| `ActivityRewardStatus` | `DRAFT`、`PENDING_ISSUE`、`ISSUED`、`REVOKED`、`FAILED` | 奖励状态。 |
| `ActivityContributionStatus` | `PENDING`、`SKIPPED`、`FAILED`、`ACCEPTED` | 贡献候选状态。P1 默认不进入 `ACCEPTED`。 |
| `ActivityNotificationStatus` | `DELIVERED`、`FAILED`、`SKIPPED` | 最近一次通知摘要。 |
| `ActivityAuditResult` | `SUCCESS`、`FAILED` | activity 审计结果。 |

## 通用对象

### ActivityParticipantSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | string | 是 | auth 用户 ID。 |
| `memberId` | string 或 null | 是 | profile 成员 ID。 |
| `displayNameSnapshot` | string | 是 | 展示名快照。 |
| `avatarUrlSnapshot` | string 或 null | 是 | 头像快照。 |
| `memberGroupSnapshot` | string 或 null | 是 | 成员组快照。 |
| `memberStatusSnapshot` | string 或 null | 是 | 成员状态快照。 |
| `minecraftIdSnapshot` | string 或 null | 是 | Minecraft ID 快照。 |
| `profileSnapshotStale` | boolean | 是 | 是否使用旧快照。 |

### Activity

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `activityId` | string | 是 | 活动 ID。 |
| `slug` | string | 是 | 活动 slug，2 到 80 位，小写字母、数字和短横线。 |
| `title` | string | 是 | 2 到 80 位。 |
| `summary` | string | 是 | 1 到 200 位。 |
| `description` | string | 是 | 1 到 5000 位，公开详情可见。 |
| `type` | string | 是 | `ActivityType`。 |
| `visibility` | string | 是 | `ActivityVisibility`。 |
| `registrationPolicy` | string | 是 | `RegistrationPolicy`。 |
| `status` | string | 是 | `ActivityStatus`。 |
| `startAt` | string | 是 | ISO 8601，活动开始时间。 |
| `endAt` | string | 是 | ISO 8601，必须晚于 `startAt`。 |
| `registrationOpenAt` | string 或 null | 是 | 报名开放时间。为空时随发布后开放。 |
| `registrationCloseAt` | string 或 null | 是 | 报名关闭时间。必须早于或等于 `startAt`。 |
| `capacity` | integer 或 null | 是 | 确认报名名额。为空表示不限制。 |
| `waitlistCapacity` | integer | 是 | 候补名额，`0` 表示不开候补。 |
| `confirmedCount` | integer | 是 | 已确认人数。 |
| `waitlistedCount` | integer | 是 | 候补人数。 |
| `checkedInCount` | integer | 是 | 已签到人数。 |
| `noShowCount` | integer | 是 | 缺席人数。 |
| `locationText` | string 或 null | 是 | 活动地点或线上说明。 |
| `coverImageUrl` | string 或 null | 是 | 活动封面，必须是 http、https 或站内路径。 |
| `tags` | string[] | 是 | 0 到 8 个标签，每个 1 到 24 位。 |
| `discussionSnapshot` | object 或 null | 是 | community 公开讨论快照。 |
| `contentSnapshot` | object 或 null | 是 | content 公开说明页快照。 |
| `resourceSnapshots` | object[] | 是 | resource 公开资源快照。 |
| `reviewComment` | string 或 null | 是 | 给创建者的审核意见。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注，公开和当前用户接口不得返回。 |
| `notificationStatus` | string 或 null | 是 | 最近一次通知状态。 |
| `notificationFailure` | ActivityNotificationFailureSummary 或 null | 后台可见 | 通知失败脱敏摘要。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `submittedAt` | string 或 null | 是 | 提交审核时间。 |
| `reviewedAt` | string 或 null | 是 | 审核时间。 |
| `publishedAt` | string 或 null | 是 | 发布时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `offlineAt` | string 或 null | 是 | 下架时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |
| `deletedAt` | string 或 null | 是 | 软删除时间。 |

### ActivityRegistration

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `registrationId` | string | 是 | 报名 ID。 |
| `activityId` | string | 是 | 活动 ID。 |
| `participant` | ActivityParticipantSnapshot | 是 | 参与者快照。 |
| `status` | string | 是 | `ActivityRegistrationStatus`。 |
| `answers` | object | 是 | 报名问题答案。P1 最多 20 个键值。 |
| `guestCount` | integer | 是 | 随行人数，P1 默认只允许 `0`。 |
| `waitlistRank` | integer 或 null | 是 | 候补排序。 |
| `reviewComment` | string 或 null | 是 | 报名处理意见。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注。 |
| `checkedInAt` | string 或 null | 是 | 签到时间。 |
| `noShowAt` | string 或 null | 是 | 缺席标记时间。 |
| `canceledAt` | string 或 null | 是 | 取消时间。 |
| `notificationStatus` | string 或 null | 是 | 最近一次通知状态。 |
| `notificationFailure` | ActivityNotificationFailureSummary 或 null | 后台可见 | 通知失败脱敏摘要。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ActivityResult

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `resultId` | string | 是 | 结果 ID。 |
| `activityId` | string | 是 | 活动 ID。 |
| `status` | string | 是 | `ActivityResultStatus`。 |
| `title` | string | 是 | 2 到 80 位。 |
| `summary` | string | 是 | 1 到 500 位。 |
| `details` | string | 否 | 最多 5000 位。 |
| `participantTotal` | integer | 是 | 参与总数。 |
| `winnerTotal` | integer | 是 | 获奖人数。 |
| `publishedAt` | string 或 null | 是 | 发布时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ActivityReward

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `rewardId` | string | 是 | 奖励 ID。 |
| `activityId` | string | 是 | 活动 ID。 |
| `registrationId` | string | 是 | 报名 ID。 |
| `recipient` | ActivityParticipantSnapshot | 是 | 获奖人快照。 |
| `type` | string | 是 | `ActivityRewardType`。 |
| `title` | string | 是 | 2 到 80 位。 |
| `description` | string | 否 | 最多 1000 位。 |
| `quantity` | integer | 是 | 数量，1 到 999。 |
| `scoreCandidateDelta` | integer | 是 | 积分候选值，0 到 1000。P1 不直接入账。 |
| `status` | string | 是 | `ActivityRewardStatus`。 |
| `issuedAt` | string 或 null | 是 | 发放标记时间。 |
| `revokedAt` | string 或 null | 是 | 撤销时间。 |
| `notificationStatus` | string 或 null | 是 | 最近一次通知状态。 |
| `notificationFailure` | ActivityNotificationFailureSummary 或 null | 后台可见 | 通知失败脱敏摘要。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ActivityContributionCandidate

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `candidateId` | string | 是 | 贡献候选 ID。 |
| `activityId` | string | 是 | 活动 ID。 |
| `registrationId` | string | 是 | 报名 ID。 |
| `rewardId` | string 或 null | 是 | 来源奖励 ID。 |
| `memberId` | string | 是 | 成员 ID。 |
| `userId` | string | 是 | 用户 ID。 |
| `title` | string | 是 | 候选标题。 |
| `description` | string | 否 | 候选说明。 |
| `scoreDelta` | integer | 是 | 候选积分。 |
| `status` | string | 是 | `ActivityContributionStatus`。 |
| `attendanceResponseSummary` | object 或 null | 是 | future attendance 接收摘要。P1 默认为 `null`。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ActivityNotificationFailureSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | 固定为 `FAILED`。 |
| `failureCode` | string | 是 | `49420`、`49421` 或 `49422`。 |
| `failureType` | string | 是 | `UNAVAILABLE`、`TIMEOUT` 或 `BAD_SCHEMA`。 |
| `failureReason` | string | 是 | 脱敏失败摘要。 |
| `failedAt` | string | 是 | 失败发生时间。 |

### ActivityAuditLog

审计字段继承公共契约，允许补充 `activityId`、`registrationId`、`resultId`、`rewardId`、`candidateId`、`stateFrom`、`stateTo`、`idempotencyKey`、`notificationStatus`、`dependencyStatus`、`profileSnapshotStale` 和 `registrationRank`。审计日志不得通过 activity API 删除。

## activity 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `49400` | 502 | auth 认证上下文不可用。 |
| `49401` | 504 | auth 认证上下文调用超时。 |
| `49402` | 502 | auth 返回字段不兼容 activity 契约。 |
| `49410` | 502 | profile 成员快照不可用。 |
| `49411` | 504 | profile 成员快照调用超时。 |
| `49412` | 502 | profile 返回字段不兼容 activity 契约。 |
| `49420` | 502 | notification 投递不可用。 |
| `49421` | 504 | notification 投递超时。 |
| `49422` | 502 | notification 返回字段不兼容 activity 契约。 |
| `49430` | 502 | community 公开快照不可用。 |
| `49440` | 502 | content 公开快照不可用。 |
| `49450` | 502 | resource 公开快照不可用。 |
| `49600` | 404 | 活动不存在或不可见。 |
| `49601` | 404 | 报名记录不存在或不可见。 |
| `49602` | 404 | 活动结果不存在或不可见。 |
| `49603` | 404 | 奖励记录不存在或不可见。 |
| `49610` | 409 | 活动状态不允许当前操作。 |
| `49611` | 409 | 报名状态不允许当前操作。 |
| `49612` | 409 | 活动未开放报名或报名已截止。 |
| `49613` | 409 | 活动名额已满且候补不可用。 |
| `49614` | 409 | 重复报名或重复签到。 |
| `49615` | 409 | 候补转正条件不满足。 |
| `49616` | 409 | 结果或奖励状态不允许当前操作。 |
| `49617` | 409 | 幂等键冲突。 |
| `49618` | 409 | 活动时间窗口不允许签到。 |
| `49619` | 409 | 活动 slug 已存在。 |
| `49620` | 403 | 活动参与资格不足。 |
| `49621` | 400 | 报名问题答案不满足要求。 |
| `49622` | 409 | 贡献候选已生成或当前不允许生成。 |
| `54600` | 500 | activity 内部错误。 |
| `54601` | 500 | activity 审计写入失败。 |
| `54602` | 500 | activity 状态写入失败。 |
| `54603` | 500 | activity 报名计数写入失败。 |
| `54604` | 500 | activity 奖励或贡献候选写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。幂等冲突可返回公共 `43002` 或 activity 细分 `49617`，同一接口实现必须固定并测试。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开活动列表 | GET | `/api/v1/activity/events` | 否 | 公开 | LOW |
| 公开活动详情 | GET | `/api/v1/activity/events/{activityIdOrSlug}` | 否 | 公开 | LOW |
| 公开活动结果 | GET | `/api/v1/activity/events/{activityId}/result` | 否 | 公开 | LOW |
| 公开活动日历摘要 | GET | `/api/v1/activity/calendar-summary` | 否 | 公开 | LOW |
| 我的报名列表 | GET | `/api/v1/activity/me/registrations` | 是 | 当前用户 | LOW |
| 我的活动详情 | GET | `/api/v1/activity/me/registrations/{registrationId}` | 是 | 当前用户 | LOW |
| 报名活动 | POST | `/api/v1/activity/me/events/{activityId}/registrations` | 是 | 当前用户 | LOW |
| 取消报名 | POST | `/api/v1/activity/me/registrations/{registrationId}/cancel` | 是 | 当前用户 | LOW |
| 我的签到结果 | GET | `/api/v1/activity/me/events/{activityId}/check-in` | 是 | 当前用户 | LOW |
| 我的奖励记录 | GET | `/api/v1/activity/me/rewards` | 是 | 当前用户 | LOW |
| 后台活动列表 | GET | `/api/v1/activity/admin/events` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 后台活动详情 | GET | `/api/v1/activity/admin/events/{activityId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 创建活动草稿 | POST | `/api/v1/activity/admin/events` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 修改活动 | PATCH | `/api/v1/activity/admin/events/{activityId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 提交活动审核 | POST | `/api/v1/activity/admin/events/{activityId}/submit` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核通过活动 | PATCH | `/api/v1/activity/admin/events/{activityId}/approve` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核拒绝活动 | PATCH | `/api/v1/activity/admin/events/{activityId}/reject` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 要求修改活动 | PATCH | `/api/v1/activity/admin/events/{activityId}/request-changes` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 发布活动 | PATCH | `/api/v1/activity/admin/events/{activityId}/publish` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 开放报名 | PATCH | `/api/v1/activity/admin/events/{activityId}/open-registration` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 关闭报名 | PATCH | `/api/v1/activity/admin/events/{activityId}/close-registration` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 标记活动进行中 | PATCH | `/api/v1/activity/admin/events/{activityId}/start` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 标记活动完成 | PATCH | `/api/v1/activity/admin/events/{activityId}/complete` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 下架活动 | PATCH | `/api/v1/activity/admin/events/{activityId}/offline` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 归档活动 | PATCH | `/api/v1/activity/admin/events/{activityId}/archive` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 软删除活动 | PATCH | `/api/v1/activity/admin/events/{activityId}/delete` | 是 | `ADMIN`、`OWNER` | HIGH |
| 报名名单 | GET | `/api/v1/activity/admin/events/{activityId}/registrations` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 确认报名 | PATCH | `/api/v1/activity/admin/registrations/{registrationId}/confirm` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 拒绝报名 | PATCH | `/api/v1/activity/admin/registrations/{registrationId}/reject` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 候补转正 | PATCH | `/api/v1/activity/admin/registrations/{registrationId}/promote` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 管理员取消报名 | PATCH | `/api/v1/activity/admin/registrations/{registrationId}/cancel` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 签到或参与确认 | PATCH | `/api/v1/activity/admin/registrations/{registrationId}/check-in` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 标记缺席 | PATCH | `/api/v1/activity/admin/registrations/{registrationId}/no-show` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 创建或修改结果 | PUT | `/api/v1/activity/admin/events/{activityId}/result` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 发布结果 | PATCH | `/api/v1/activity/admin/events/{activityId}/result/publish` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 创建奖励 | POST | `/api/v1/activity/admin/events/{activityId}/rewards` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 标记奖励已发放 | PATCH | `/api/v1/activity/admin/rewards/{rewardId}/issue` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 撤销奖励 | PATCH | `/api/v1/activity/admin/rewards/{rewardId}/revoke` | 是 | `ADMIN`、`OWNER` | HIGH |
| 生成贡献候选 | POST | `/api/v1/activity/admin/events/{activityId}/contribution-candidates` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 审计列表 | GET | `/api/v1/activity/admin/audit-logs` | 是 | `ADMIN`、`OWNER` | LOW |
| activity 自检摘要 | GET | `/api/v1/activity/admin/ops/summary` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |

## 公开接口

### 公开活动列表

`GET /api/v1/activity/events`

查询参数：`page`、`pageSize`、`keyword`、`type`、`visibility`、`status`、`from`、`to`、`tag` 和 `sort`。`pageSize` 最大 `100`。`sort` 允许 `startAt_asc`、`startAt_desc`、`publishedAt_desc`、`createdAt_desc`。成功响应 HTTP `200`，分页 `items` 为公开视图 `Activity[]`。

业务规则：游客只看到 `PUBLISHED`、`REGISTRATION_OPEN`、`REGISTRATION_CLOSED`、`RUNNING`、`COMPLETED` 和 `RESULT_PUBLISHED` 的公开活动。`MEMBER_ONLY` 活动可展示摘要，但报名资格只在登录报名时判定。列表不得返回内部备注、通知失败详情、审计字段和未发布结果。

### 公开活动详情

`GET /api/v1/activity/events/{activityIdOrSlug}`

成功响应 HTTP `200`，`data` 为公开视图 `Activity`，包含报名名额、候补名额、活动时间、公开说明和公开关联快照。活动不存在、不可见、已下架、已归档或已删除时返回 `49600`。

### 公开活动结果

`GET /api/v1/activity/events/{activityId}/result`

成功响应 HTTP `200`，`data` 包含 `ActivityResult`、公开获奖摘要和公开奖励摘要。只有活动状态为 `RESULT_PUBLISHED` 且结果状态为 `PUBLISHED` 时可见。结果未发布返回 `49602`。

### 公开活动日历摘要

`GET /api/v1/activity/calendar-summary`

查询参数：`from`、`to`、`type`、`visibility`。成功响应 HTTP `200`，`data.items` 为活动时间摘要，字段包含 `activityId`、`slug`、`title`、`type`、`visibility`、`status`、`startAt`、`endAt`、`registrationCloseAt` 和 `summary`。该接口只提供后续 `calendar` 适配的只读摘要，不创建 calendar 主数据。

## 当前用户接口

### 我的报名列表和详情

`GET /api/v1/activity/me/registrations` 支持 `page`、`pageSize`、`status`、`activityStatus`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为当前用户自己的 `ActivityRegistration[]`。

`GET /api/v1/activity/me/registrations/{registrationId}` 只返回当前用户自己的报名详情。不存在或不属于当前用户返回 `49601`。

### 报名活动

`POST /api/v1/activity/me/events/{activityId}/registrations`

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `answers` | object | 否 | 报名问题答案，最多 20 个键值。 |
| `guestCount` | integer | 否 | P1 固定为 `0`。 |
| `note` | string | 否 | 最多 500 位。 |
| `idempotencyKey` | string | 否 | 8 到 80 位。 |

成功响应 HTTP `201`，`data` 为 `ActivityRegistration`。`OPEN` 报名策略在名额未满时进入 `CONFIRMED`；需要审批时进入 `SUBMITTED`；名额已满且候补可用时进入 `WAITLISTED`；名额和候补都满返回 `49613`。重复报名返回 `49614` 或同幂等结果。报名必须按服务端时间判断报名窗口，未开放或已截止返回 `49612`。

### 取消报名

`POST /api/v1/activity/me/registrations/{registrationId}/cancel`

请求字段：`reason` 1 到 200 位，`idempotencyKey` 可选。只允许当前用户取消 `SUBMITTED`、`CONFIRMED` 或 `WAITLISTED` 报名。活动已 `RUNNING`、`COMPLETED` 或 `RESULT_PUBLISHED` 后普通用户不得取消，返回 `49611`。取消成功后释放名额，但不会自动转正候补，候补转正必须由后台明确执行。

### 我的签到结果

`GET /api/v1/activity/me/events/{activityId}/check-in`

成功响应 HTTP `200`。当前用户无报名时返回 `data=null`。已签到返回 `status=CHECKED_IN` 和 `checkedInAt`，缺席返回 `status=NO_SHOW`。

### 我的奖励记录

`GET /api/v1/activity/me/rewards`

查询参数：`page`、`pageSize`、`status`、`activityId` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为当前用户可见的 `ActivityReward[]`。响应不得返回后台备注、贡献候选内部原因或通知失败详情。

## 后台接口

### 活动管理

`GET /api/v1/activity/admin/events` 返回后台活动分页，支持 `page`、`pageSize`、`keyword`、`type`、`visibility`、`status`、`from`、`to`、`createdBy` 和 `sort`。

`GET /api/v1/activity/admin/events/{activityId}` 返回后台活动详情，包含活动、报名统计、结果、奖励摘要、贡献候选摘要、依赖摘要和最近审计。响应不得返回 token、完整请求头、通知正文、前序服务内部路径、异常堆栈、真实服务器命令、节点凭据或 Cloudreve token。

`POST /api/v1/activity/admin/events` 创建草稿。请求字段为 `slug`、`title`、`summary`、`description`、`type`、`visibility`、`registrationPolicy`、`startAt`、`endAt`、`registrationOpenAt`、`registrationCloseAt`、`capacity`、`waitlistCapacity`、`locationText`、`coverImageUrl`、`tags`、`linkedCommunityId`、`linkedContentId`、`linkedResourceIds`、`internalNote`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，状态为 `DRAFT`。slug 冲突返回 `49619`。

`PATCH /api/v1/activity/admin/events/{activityId}` 修改活动。只允许 `DRAFT`、`NEEDS_CHANGES`、`REJECTED` 和未发布的 `APPROVED`。请求字段同创建，按需修改，`reason` 必填。已发布活动如需修改时间、名额或报名策略，P1 必须先下架或归档后新建活动，避免破坏已报名成员预期。

### 活动审核发布状态

`POST /api/v1/activity/admin/events/{activityId}/submit` 使 `DRAFT`、`NEEDS_CHANGES` 或 `REJECTED` 进入 `PENDING_REVIEW`。

`PATCH /api/v1/activity/admin/events/{activityId}/approve` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，使 `PENDING_REVIEW` 或 `NEEDS_CHANGES` 进入 `APPROVED`。

`PATCH /api/v1/activity/admin/events/{activityId}/reject` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，使 `PENDING_REVIEW` 进入 `REJECTED`。

`PATCH /api/v1/activity/admin/events/{activityId}/request-changes` 请求字段同拒绝，使 `PENDING_REVIEW` 进入 `NEEDS_CHANGES`。

`PATCH /api/v1/activity/admin/events/{activityId}/publish` 使 `APPROVED` 进入 `PUBLISHED` 或 `REGISTRATION_OPEN`。如果当前时间在报名窗口内且策略允许报名，推荐直接进入 `REGISTRATION_OPEN`。发布成功可以触发通知，通知失败不回滚主状态。

`PATCH /api/v1/activity/admin/events/{activityId}/open-registration` 使 `PUBLISHED` 或 `REGISTRATION_CLOSED` 进入 `REGISTRATION_OPEN`。

`PATCH /api/v1/activity/admin/events/{activityId}/close-registration` 使 `REGISTRATION_OPEN` 进入 `REGISTRATION_CLOSED`。

`PATCH /api/v1/activity/admin/events/{activityId}/start` 使 `PUBLISHED`、`REGISTRATION_OPEN` 或 `REGISTRATION_CLOSED` 进入 `RUNNING`。

`PATCH /api/v1/activity/admin/events/{activityId}/complete` 使 `RUNNING` 进入 `COMPLETED`。

`PATCH /api/v1/activity/admin/events/{activityId}/offline` 请求字段为 `publicReason`、`reason`、`idempotencyKey`，使公开活动进入 `OFFLINE`。

`PATCH /api/v1/activity/admin/events/{activityId}/archive` 请求字段为 `reason` 和 `idempotencyKey`，使 `COMPLETED`、`RESULT_PUBLISHED` 或 `OFFLINE` 进入 `ARCHIVED`。

`PATCH /api/v1/activity/admin/events/{activityId}/delete` 是 `HIGH` 风险，请求字段为 `reason`、`confirmText` 和 `idempotencyKey`，P1 固定要求 `DELETE_ACTIVITY_EVENT`。成功后状态为 `DELETED`，只做软删除。

### 报名名单和参与确认

`GET /api/v1/activity/admin/events/{activityId}/registrations` 返回报名分页，支持 `status`、`keyword`、`memberGroup`、`page`、`pageSize` 和 `sort`。

`PATCH /api/v1/activity/admin/registrations/{registrationId}/confirm` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，只允许 `SUBMITTED` 或 `WAITLISTED`。确认时必须再次检查名额。名额不足返回 `49613`。

`PATCH /api/v1/activity/admin/registrations/{registrationId}/reject` 请求字段同确认，使 `SUBMITTED` 或 `WAITLISTED` 进入 `REJECTED`。

`PATCH /api/v1/activity/admin/registrations/{registrationId}/promote` 请求字段为 `reviewComment`、`reason`、`idempotencyKey`，只允许 `WAITLISTED` 且名额可用。成功后进入 `CONFIRMED`，保留原候补排序。

`PATCH /api/v1/activity/admin/registrations/{registrationId}/cancel` 请求字段为 `publicReason`、`reason`、`idempotencyKey`，后台可取消 `SUBMITTED`、`CONFIRMED` 或 `WAITLISTED`。

`PATCH /api/v1/activity/admin/registrations/{registrationId}/check-in` 请求字段为 `method`、`checkInCode`、`reason`、`idempotencyKey`，只允许 `CONFIRMED` 报名。签到窗口默认从活动开始前 1 小时到活动结束后 24 小时，超出窗口返回 `49618`。重复签到返回幂等成功，不重复计数。

`PATCH /api/v1/activity/admin/registrations/{registrationId}/no-show` 请求字段为 `publicReason`、`reason`、`idempotencyKey`，只允许 `CONFIRMED` 或按实现固定是否允许 `CHECKED_IN` 修正。默认只允许从 `CONFIRMED` 进入 `NO_SHOW`。

### 活动结果和奖励

`PUT /api/v1/activity/admin/events/{activityId}/result` 创建或修改结果。请求字段为 `title`、`summary`、`details`、`reason` 和 `idempotencyKey`。只允许 `COMPLETED` 或 `RESULT_PUBLISHED` 活动。首次创建结果状态为 `DRAFT`。

`PATCH /api/v1/activity/admin/events/{activityId}/result/publish` 请求字段为 `reason` 和 `idempotencyKey`。成功后结果状态为 `PUBLISHED`，活动状态进入 `RESULT_PUBLISHED`，可触发通知。重复发布返回幂等成功。

`POST /api/v1/activity/admin/events/{activityId}/rewards` 创建奖励。请求字段为 `registrationId`、`type`、`title`、`description`、`quantity`、`scoreCandidateDelta`、`reason` 和 `idempotencyKey`。只允许给 `CHECKED_IN` 或后台明确允许的 `CONFIRMED` 报名创建奖励。成功响应 HTTP `201`，状态为 `PENDING_ISSUE` 或 `DRAFT`，实现必须固定并测试。

`PATCH /api/v1/activity/admin/rewards/{rewardId}/issue` 请求字段为 `publicComment`、`reason`、`idempotencyKey`，使 `PENDING_ISSUE` 进入 `ISSUED`。通知失败不回滚主状态。

`PATCH /api/v1/activity/admin/rewards/{rewardId}/revoke` 是 `HIGH` 风险，请求字段为 `publicReason`、`reason`、`confirmText`、`idempotencyKey`，P1 固定要求 `REVOKE_ACTIVITY_REWARD`。成功后状态为 `REVOKED`。P1 如已生成贡献候选，只把候选标记为 `SKIPPED` 或保留可复核状态，不调用 attendance 撤销。

`POST /api/v1/activity/admin/events/{activityId}/contribution-candidates` 请求字段为 `reason` 和 `idempotencyKey`。只允许 `RESULT_PUBLISHED` 活动。成功响应 HTTP `201`，`data` 包含生成的 `ActivityContributionCandidate[]`。P1 候选状态默认为 `PENDING` 或 `SKIPPED`，不得直接写 attendance 积分。

### 审计和自检

`GET /api/v1/activity/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`activityId`、`result`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为 `ActivityAuditLog[]`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 activity API 删除。

`GET /api/v1/activity/admin/ops/summary` 成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "activity",
    "port": 8132,
    "legacyPort": 8113,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "profileMode": "TEST_STUB",
    "notificationMode": "TEST_STUB",
    "attendanceMode": "SKIPPED",
    "communityMode": "TEST_STUB",
    "contentMode": "TEST_STUB",
    "resourceMode": "TEST_STUB",
    "testControlsEnabled": false,
    "activitiesTotal": 8,
    "publishedActivitiesTotal": 3,
    "openRegistrationsTotal": 20,
    "waitlistedRegistrationsTotal": 2,
    "checkedInRegistrationsTotal": 12,
    "resultsPublishedTotal": 1,
    "rewardsTotal": 6,
    "contributionCandidatesTotal": 6,
    "auditsTotal": 40,
    "idempotencyRecordsTotal": 16,
    "lastAuditAt": "2026-05-24T12:00:00Z",
    "productionGaps": [
      "P1_IN_MEMORY_STORAGE",
      "P1_AUTH_STUB",
      "P1_PROFILE_STUB",
      "P1_NOTIFICATION_STUB",
      "P1_COMMUNITY_STUB",
      "P1_CONTENT_STUB",
      "P1_RESOURCE_STUB",
      "ATTENDANCE_CONTRIBUTION_NOT_CONNECTED",
      "CALENDAR_NOT_CONNECTED",
      "TEST_CONTROLS_DISABLED_OUTSIDE_TEST"
    ]
  }
}
```

## 状态、幂等和并发

活动创建后为 `DRAFT`。`DRAFT` 可提交为 `PENDING_REVIEW`。`PENDING_REVIEW` 可通过为 `APPROVED`、拒绝为 `REJECTED`、要求修改为 `NEEDS_CHANGES`。`NEEDS_CHANGES` 和 `REJECTED` 可修改后再次提交。`APPROVED` 可发布为 `PUBLISHED` 或 `REGISTRATION_OPEN`。`PUBLISHED` 可开放报名，`REGISTRATION_OPEN` 可关闭报名或进入进行中，`REGISTRATION_CLOSED` 可进入进行中，`RUNNING` 可完成，`COMPLETED` 可发布结果。`RESULT_PUBLISHED`、`OFFLINE` 和 `ARCHIVED` 不得直接回到公开报名状态。`DELETED` 为软删除终态。

报名在 `OPEN` 策略下默认进入 `CONFIRMED`，在 `APPROVAL_REQUIRED` 下进入 `SUBMITTED`，在名额满且候补开启时进入 `WAITLISTED`。`SUBMITTED` 可确认或拒绝。`WAITLISTED` 可转正、拒绝或取消。`CONFIRMED` 可签到、缺席或取消。`CHECKED_IN` 和 `NO_SHOW` 是参与事实状态，除非后台修正接口未来补充契约，否则不得回到报名状态。

写接口支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `49617`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序。

后台活动修改、审核、发布、报名管理、签到、结果、奖励、撤销和贡献候选生成均属于写接口，P1 内存实现也必须使用同一套幂等记录机制，不能只校验 `idempotencyKey` 长度。重复提交已经完成的状态流转时，如果操作者、接口语义、幂等键和请求体一致，必须 replay 首次结果；如果请求体不同，必须返回 `49617` 且业务状态不变。

活动创建和活动修改必须使用同一套字段校验规则。`capacity` 必须大于等于 1，`waitlistCapacity` 必须大于等于 0，`registrationOpenAt` 不得晚于 `registrationCloseAt`，`registrationCloseAt` 必须早于活动开始时间。修改活动时不得通过局部字段更新绕过这些规则；校验失败必须返回公共字段校验错误，不能写入半更新活动。

并发报名同一活动同一用户只能产生一条有效报名。并发确认报名必须串行检查活动名额，不能出现 `confirmedCount` 超过 `capacity`。并发取消和候补转正不能让同一候补被转正两次。并发签到同一报名只能增加一次 `checkedInCount`。读取接口允许读到更新前或更新后的完整状态，不能返回半更新对象。

P1 内存实现必须用本服务内的串行临界区保护名额、候补、签到、结果、奖励和贡献候选这些共享状态。后续持久化实现必须把这些保护迁移为数据库事务、唯一约束、条件更新或等效机制，不能降低上述并发口径。

## 审计要求

必须审计的动作包括活动创建、活动修改、提交审核、审核通过、审核拒绝、要求修改、发布、开放报名、关闭报名、开始、完成、下架、归档、软删除、报名创建、报名取消、报名确认、报名拒绝、候补转正、签到、缺席、结果创建或修改、结果发布、奖励创建、奖励发放、奖励撤销、贡献候选生成、通知失败、依赖降级、自检读取、审计写入失败和状态写入失败。

后台写操作必须记录 `reason`、操作者、目标对象、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计不得泄露 token、完整请求头、Minecraft 验证凭据、profile 后台备注全文、通知正文全文、真实服务器命令、节点凭据、Cloudreve token、内部异常堆栈或前序服务内部路径。

审计写入失败时，活动审核、发布、下架、归档、软删除、报名确认、报名拒绝、候补转正、签到、缺席、结果发布、奖励创建、奖励发放、奖励撤销和贡献候选生成不得假装成功，必须返回 `54601` 或 `54600`，并保持业务数据不变。通知失败不回滚主状态，但必须记录失败摘要和审计。

P1 内存实现允许用本地自动化测试控制头模拟审计失败，但失败检查必须发生在业务状态写入前，或者业务写入和审计写入必须处于同一可回滚临界区。审计失败后，不得新增活动、不得改变状态、不得改变报名计数、不得新增奖励或贡献候选，也不得写入幂等成功记录。

## 失败降级

auth 是所有登录接口强依赖。auth 不可用、超时、用户状态不允许或字段不兼容时不得伪造成功。

profile 是报名、签到、获奖和奖励归属的强依赖。写入时 profile 失败不得伪造成员资料。公开读取时 profile 失败可以使用旧快照降级，但必须标记 stale。

notification 是辅助依赖。报名确认、候补转正、取消、签到、结果发布和奖励发放的通知失败不得回滚活动主状态，但必须保存脱敏失败摘要和审计。

attendance 在 P1 不作为活动主流程依赖。活动贡献候选可以记录为 `PENDING`、`SKIPPED` 或 `FAILED`，但不得直接写积分余额、流水或榜单。

community、content 和 resource 是关联快照辅助依赖。创建或修改关联时，公开快照不可用不得保存伪造关联。读取已存在活动时，来源服务失败可以使用已保存快照降级并标记 stale。

状态写入、报名计数、候补排序、签到计数、奖励状态和贡献候选写入必须保持一致。不能出现报名记录写入失败但名额增加，或签到记录失败但签到人数增加。半成功风险必须返回 `54602`、`54603` 或 `54604` 并保持可复核状态。

## 验收口径

`activity` API 文档按 `docs/contracts-activity.md` 独立存在，并由 `.local-docs/tests-activity.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`activity` 完成时必须满足以下条件：全部接口按本文档实现；公开接口只返回公开可见活动、结果和摘要；当前用户接口只能访问自己的报名、签到和奖励；后台接口按角色限制；活动、报名、签到、结果和奖励有服务端状态机；名额、候补和签到由服务端计数和幂等保证；所有后台写操作和高风险操作有审计；通知失败按辅助降级记录；贡献奖励只生成 activity 贡献候选，不直接写 attendance 积分；当前运行端口固定为 `8132`，自检摘要返回 `port=8132` 和 `legacyPort=8113`；`.local-docs/tests-activity.md` 与 `.local-docs/tests-engagement-core.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 activity 在 `engagement-core-service` 中全部测试通过；auth、profile、notification、content、server-status、resource、admin、onboarding、exam、whitelist、attendance 和 community 前序服务回归测试通过；不恢复 `backend/activity-service` 旧入口；没有修改前序服务稳定接口；没有把日历、更新日志、后台聚合、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复或 Cloudreve 管理能力塞进 activity。

生产化硬化验收还必须满足：测试控制头默认关闭，只有本地自动化测试显式启用时才生效；关闭状态下依赖失败模拟头、写入失败模拟头、时间模拟头和通知失败模拟头全部被忽略；自检摘要明确返回当前测试控制头开关状态。
