# 北冥官网 community API 契约

版本：0.2

## 版本记录

`0.2` 补充 P1 硬化验收：公开浏览计数必须按服务端访问指纹去重；投票开放时间、关闭时间和可投资格必须生效；举报证据链接必须校验协议，举报处理必须保存关联处罚；工单必须保存并返回站内安全附件摘要和关联对象摘要；高风险处罚解除必须纳入审计失败回滚；工单后台状态推进必须遵守固定状态机。

## 文档定位

本文档是 `community` 微服务的正式 API 契约。后续 `activity`、`calendar`、`changelog`、前端适配、`admin` 聚合、`ops-control` 和 `external-node-executor` 只能通过本文档定义的接口读取论坛、帖子、评论、互动、投票、举报、工单、处罚、审计和自检摘要，不能直接读取或修改 `community` 数据库，也不能把社区治理逻辑塞进其他服务。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `community` 的职责边界、数据归属、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考了现有高活跃社区的成熟做法。Reddit 的社区、版主管理、举报和 Mod Queue 说明举报处理要有独立队列和处理状态，不能只把举报挂在帖子上。Discourse 的分类、标签、信任等级和 flag 机制说明论坛需要可治理的版块、标签、用户可信度和可追溯审核。GitHub Discussions 的分类、问答标记、评论和投票说明讨论型社区可以同时承载建议、问答、公告讨论和轻量投票。Stack Overflow 的声望、权限和 flag 说明互动权重、举报和处罚要基于服务端可信状态，不能靠前端显示决定。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Reddit Help: Mod Queue](https://support.reddithelp.com/hc/en-us/articles/15484440494356-Moderation-Queue) | 举报和审核应进入独立处理队列，有明确处理结果和处理人。 |
| [Discourse Meta: Trust Levels](https://meta.discourse.org/t/understanding-discourse-trust-levels/90752) | 社区写权限、互动频率和治理能力要随可信度分层，不能只看登录态。 |
| [Discourse Meta: Flagging](https://meta.discourse.org/t/discourse-moderation-guide/63116) | 用户举报、版主审核和处罚要保留原因、证据和状态。 |
| [GitHub Docs: About Discussions](https://docs.github.com/en/discussions/collaborating-with-your-community-using-discussions/about-discussions) | 讨论区可按分类组织，支持问答、公告讨论、评论和投票反馈。 |
| [Stack Overflow Help: Reputation](https://stackoverflow.com/help/whats-reputation) | 互动权重和可信能力应来自可解释行为，不能由人工随意变更。 |
| [Stack Overflow Help: Privileges](https://stackoverflow.com/help/privileges) | 社区治理能力需要按权限分层解锁，后台处罚仍必须走角色和审计。 |

## 职责边界

`community` 负责论坛板块、帖子、评论、点赞、收藏、轻量投票、举报、工单、处罚、社区审计、社区自检和自身幂等记录。

`community` 不负责注册、登录、邀请码、会话、账号角色能力点、Minecraft 绑定主数据、成员档案主数据、站内通知主数据、官网公告主发布、资源下载主数据、考勤积分主数据、白名单审核主流程、考试判分、服务器状态采集、后台聚合入口、真实服务器运维控制、节点守护进程、容器、终端、文件管理、备份恢复或 Cloudreve 管理。

`community` 只能后序适配前序服务。它通过 `auth` 认证上下文读取当前用户、角色、能力点和用户状态；通过 `profile` 的正式接口或未来服务间适配器读取成员展示快照；通过 `notification` 投递帖子审核、举报处理、工单回复和处罚通知；通过 `content` 与 `resource` 的公开快照关联公告、专题或资源讨论；通过 `attendance` 未来正式贡献入口或兼容变更产生社区贡献候选。`community` 不能导入前序服务内存存储、实体、Repository、测试种子或内部类，不能要求前序服务为了社区反向改稳定接口。

## 数据归属

`community` 拥有以下主数据：社区板块、板块权限规则、帖子、帖子版本、评论、评论版本、帖子互动、评论互动、收藏、轻量投票、投票选项、投票记录、举报、举报证据、工单、工单消息、处罚记录、处罚解除记录、幂等记录、依赖调用摘要、社区审计日志和自检统计。

`community` 可以保存来自 `auth` 的 `userId`、`displayName`、`roles`、`permissions` 和用户状态快照；可以保存来自 `profile` 的成员展示名、头像、成员组、成员状态和 Minecraft ID 快照；可以保存来自 `content` 的内容 ID、标题、slug 和公开状态快照；可以保存来自 `resource` 的资源 ID、标题、slug、版本和公开状态快照；可以保存来自 `notification` 的投递结果摘要；可以保存 future `attendance` 的贡献接收摘要。快照只服务展示、检索和审计，不能成为来源模块主数据，也不能反写来源模块。

## 基础路径与认证

所有接口默认使用 `/api/v1/community` 前缀。当前运行入口为 `engagement-core-service:8132`。历史原服务端口 `8112` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或回归测试命令。

公开接口允许游客读取已公开、已通过、未下架、未软删除的数据。公开接口不得返回内部备注、举报详情、处罚证据、工单内容、审核参数、管理员 ID、通知失败详情、完整依赖错误或审计参数。

当前用户接口使用 `/api/v1/community/me` 前缀，全部要求 `Authorization: Bearer <token>`。当前用户只能创建和维护自己的帖子草稿、需修改帖子、评论、互动、收藏、投票、举报和工单。浏览器请求体不得传入 `userId`、`memberId`、`roles`、`permissions`、`authorSnapshot`、`moderatorUserId`、`status`、`reviewStatus`、`voteCount`、`likeCount`、`favoriteCount`、`reportStatus`、`ticketAssigneeId`、`penaltyStatus`、`notificationStatus`、`sourceModule`、`auditResult` 等服务端可信字段。

后台接口使用 `/api/v1/community/admin` 前缀，全部要求登录。后台读取板块、帖子、评论、举报、工单、处罚、审计和自检摘要要求 `HELPER`、`ADMIN` 或 `OWNER`。板块写入、帖子审核、评论审核、举报处理、工单分配和回复要求 `HELPER`、`ADMIN` 或 `OWNER`，但 `HELPER` 只能处理被授权的初审、回复和协助事项。处罚创建、处罚修正、处罚解除、投票管理、强制下架、归档、软删除和系统配置要求 `ADMIN` 或 `OWNER`。高风险处罚、批量状态变更和跨模块贡献接入在 P1 不开放。

## 本地测试控制头

`community` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Profile-Mode`、`X-Test-Notification-Mode`、`X-Test-Content-Mode`、`X-Test-Resource-Mode`、`X-Test-Attendance-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Fail-Reaction` 模拟依赖失败、通知失败、写入失败和互动并发冲突。该能力只服务本地测试闭环，不属于正式业务 API。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发依赖失败、审计失败、状态失败、互动失败、通知失败或快照 stale。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 视为已满足的生产化硬化项。

## 前序服务兼容契约

`auth` 是所有登录接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions`、`status` 和可选 `minecraftBinding`。用户状态为 `ACTIVE` 时可参与社区；`PENDING_PROFILE` 可以创建工单和查看自己的举报进度，但不能发布公开帖子；`DISABLED`、`BANNED`、`DELETED` 不允许写入。auth 不可用返回 `49200`，auth 超时返回 `49201`，字段或枚举不兼容返回 `49202`。

`profile` 是作者展示快照的强依赖。发帖、评论、工单和后台处罚创建时必须读取成员展示快照。profile 不可用、超时或字段不兼容时，发帖和评论不得伪造成功，分别返回 `49210`、`49211` 或 `49212`。只读公开列表可以使用已有快照降级，但必须返回 `profileSnapshotStale=true` 或依赖摘要，且不得刷新为伪造资料。

`notification` 是辅助依赖。帖子审核结果、评论审核结果、举报处理、工单回复、工单关闭、处罚生效和处罚解除可以触发通知。通知失败不得回滚社区主状态，但必须保存 `notificationStatus=FAILED`、失败原因摘要并写入审计。notification 不可用记录或返回 `49220`，超时记录或返回 `49221`，字段不兼容记录或返回 `49222`。通知失败摘要不得包含通知正文、完整请求头、认证 token、内部 URL 或异常堆栈。

`content` 是关联公告、专题和内容讨论的辅助依赖。帖子引用 `content` 对象时只能保存公开快照，不能读取 content 内部存储。content 不可用时，发帖引用校验返回 `49230`；公开读取可以展示已保存快照并标记 `linkedContentSnapshotStale=true`。

`resource` 是关联资源讨论的辅助依赖。帖子引用 `resource` 对象时只能保存公开快照，不能读取 resource 内部存储，不能生成下载票据。resource 不可用时，发帖引用校验返回 `49240`；公开读取可以展示已保存快照并标记 `linkedResourceSnapshotStale=true`。

`attendance` 当前只作为未来贡献入口。P1 中 `community` 只能记录 `communityContributionCandidate`，不得直接写 attendance 积分余额、流水或榜单。后续需要贡献积分时，必须作为 `attendance` 兼容变更先补充契约、测试和回归，再由 `community` 通过正式接口适配。attendance 不可用不得影响帖子、评论、举报和工单主流程，只能记录 `attendanceSyncStatus=SKIPPED` 或 `FAILED`。

`admin` 当前只是聚合入口。本文档不修改 `admin` 契约。若后续需要 admin 展示 community 待办、指标或审计索引，必须作为 admin 兼容变更单独走契约、测试、红灯、实现和回归闭环。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `CommunityBoardStatus` | `DRAFT`、`ACTIVE`、`LOCKED`、`ARCHIVED` | 板块状态。`ACTIVE` 可公开读取和发帖，`LOCKED` 只读，`ARCHIVED` 不接受新内容。 |
| `CommunityBoardVisibility` | `PUBLIC`、`MEMBER_ONLY`、`STAFF_ONLY` | 板块可见范围。 |
| `CommunityPostType` | `DISCUSSION`、`QUESTION`、`GUIDE`、`SHOWCASE`、`SUGGESTION`、`ANNOUNCEMENT_DISCUSSION`、`RESOURCE_DISCUSSION` | 帖子类型。公告和资源讨论只保存关联快照，不吞 content 或 resource 主数据。 |
| `CommunityPostStatus` | `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`NEEDS_CHANGES`、`REJECTED`、`LOCKED`、`OFFLINE`、`ARCHIVED`、`DELETED` | 帖子状态。公开列表只展示 `APPROVED` 和未下架内容。 |
| `CommunityCommentStatus` | `PENDING_REVIEW`、`APPROVED`、`NEEDS_CHANGES`、`REJECTED`、`OFFLINE`、`ARCHIVED`、`DELETED` | 评论状态。 |
| `CommunityReactionTargetType` | `POST`、`COMMENT` | 互动目标。 |
| `CommunityVoteStatus` | `DRAFT`、`OPEN`、`CLOSED`、`ARCHIVED` | 轻量投票状态。 |
| `CommunityReportTargetType` | `POST`、`COMMENT`、`USER`、`RESOURCE_SNAPSHOT`、`GAME_BEHAVIOR` | 举报目标。 |
| `CommunityReportReason` | `SPAM`、`HARASSMENT`、`INAPPROPRIATE`、`COPYRIGHT`、`IMPERSONATION`、`GAME_VIOLATION`、`OTHER` | 举报原因。 |
| `CommunityReportStatus` | `OPEN`、`UNDER_REVIEW`、`RESOLVED`、`DISMISSED`、`ESCALATED`、`ARCHIVED` | 举报处理状态。 |
| `CommunityTicketType` | `BAN_APPEAL`、`WHITELIST_ISSUE`、`ACCOUNT_ISSUE`、`RESOURCE_ISSUE`、`BUG_REPORT`、`CONTENT_DISPUTE`、`OTHER` | 工单类型。 |
| `CommunityTicketStatus` | `OPEN`、`WAITING_STAFF`、`WAITING_USER`、`RESOLVED`、`CLOSED`、`ARCHIVED` | 工单状态。 |
| `CommunityTicketMessageType` | `USER_REPLY`、`STAFF_REPLY`、`INTERNAL_NOTE`、`SYSTEM_EVENT` | 工单消息类型。 |
| `CommunityPenaltyType` | `WARNING`、`MUTE`、`BAN`、`WHITELIST_REVIEW_REQUIRED`、`POST_RESTRICTED`、`SUBMISSION_RESTRICTED` | 处罚类型。`WHITELIST_REVIEW_REQUIRED` 只生成社区治理建议，不执行白名单移除。 |
| `CommunityPenaltyStatus` | `ACTIVE`、`EXPIRED`、`REVOKED`、`ARCHIVED` | 处罚状态。 |
| `CommunityAuditResult` | `SUCCESS`、`FAILED` | community 审计结果。 |
| `CommunityNotificationStatus` | `DELIVERED`、`FAILED`、`SKIPPED` | 最近一次通知摘要。 |

## 通用对象

### CommunityAuthorSnapshot

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

### CommunityBoard

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `boardId` | string | 是 | 板块 ID。 |
| `slug` | string | 是 | 板块 slug，2 到 60 位，小写字母、数字和短横线。 |
| `name` | string | 是 | 2 到 40 位。 |
| `description` | string | 是 | 1 到 300 位。 |
| `visibility` | string | 是 | `CommunityBoardVisibility`。 |
| `status` | string | 是 | `CommunityBoardStatus`。 |
| `allowedPostTypes` | string[] | 是 | 允许的帖子类型。 |
| `tags` | string[] | 是 | 默认标签。 |
| `sortOrder` | integer | 是 | 后台排序。 |
| `postCount` | integer | 是 | 已通过帖子数量。 |
| `lastPostAt` | string 或 null | 是 | 最近帖子时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |

### CommunityPost

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `postId` | string | 是 | 帖子 ID。 |
| `boardId` | string | 是 | 所属板块。 |
| `type` | string | 是 | `CommunityPostType`。 |
| `title` | string | 是 | 2 到 80 位。 |
| `summary` | string | 否 | 最多 200 位。 |
| `body` | string | 是 | Markdown 或纯文本，1 到 20000 位。公开摘要不得返回未审核草稿。 |
| `tags` | string[] | 是 | 0 到 8 个标签，每个 1 到 24 位。 |
| `status` | string | 是 | `CommunityPostStatus`。 |
| `author` | CommunityAuthorSnapshot | 是 | 作者快照。 |
| `linkedContentSnapshot` | object 或 null | 是 | 关联 content 的公开快照。 |
| `linkedResourceSnapshot` | object 或 null | 是 | 关联 resource 的公开快照。 |
| `pollId` | string 或 null | 是 | 关联轻量投票。 |
| `commentCount` | integer | 是 | 已通过评论数。 |
| `likeCount` | integer | 是 | 点赞数。 |
| `favoriteCount` | integer | 是 | 收藏数。 |
| `viewCount` | integer | 是 | 服务端统计浏览数。公开详情读取可以增加该值，但必须按服务端访问指纹去重，不能让同一访客在短时间内无限累加。 |
| `acceptedCommentId` | string 或 null | 是 | 问答帖的采纳评论。 |
| `lastCommentAt` | string 或 null | 是 | 最近评论时间。 |
| `submittedAt` | string 或 null | 是 | 提交审核时间。 |
| `reviewedAt` | string 或 null | 是 | 审核时间。 |
| `reviewerUserId` | string 或 null | 后台可见 | 审核人。公开接口不得返回。 |
| `reviewComment` | string 或 null | 是 | 给作者的审核意见。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注。当前用户和公开接口不得返回。 |
| `notificationStatus` | string 或 null | 是 | 最近通知状态。 |
| `notificationFailure` | CommunityNotificationFailureSummary 或 null | 后台可见 | 通知失败脱敏摘要。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `offlineAt` | string 或 null | 是 | 下架时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |
| `deletedAt` | string 或 null | 是 | 软删除时间。 |

### CommunityComment

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `commentId` | string | 是 | 评论 ID。 |
| `postId` | string | 是 | 所属帖子。 |
| `parentCommentId` | string 或 null | 是 | 父评论。P1 最多允许二级回复。 |
| `body` | string | 是 | 1 到 10000 位。 |
| `status` | string | 是 | `CommunityCommentStatus`。 |
| `author` | CommunityAuthorSnapshot | 是 | 作者快照。 |
| `likeCount` | integer | 是 | 点赞数。 |
| `isAcceptedAnswer` | boolean | 是 | 是否被采纳。 |
| `submittedAt` | string | 是 | 提交时间。 |
| `reviewedAt` | string 或 null | 是 | 审核时间。 |
| `reviewComment` | string 或 null | 是 | 给作者的审核意见。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `deletedAt` | string 或 null | 是 | 软删除时间。 |

### CommunityPoll

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `pollId` | string | 是 | 投票 ID。 |
| `postId` | string 或 null | 是 | 关联帖子。 |
| `title` | string | 是 | 2 到 80 位。 |
| `description` | string | 否 | 最多 500 位。 |
| `status` | string | 是 | `CommunityVoteStatus`。 |
| `options` | CommunityPollOption[] | 是 | 2 到 10 个选项。 |
| `multipleChoice` | boolean | 是 | 是否多选。 |
| `minChoices` | integer | 是 | 最少选择数。 |
| `maxChoices` | integer | 是 | 最多选择数。 |
| `eligibleVisibility` | string | 是 | `PUBLIC`、`MEMBER_ONLY` 或 `STAFF_ONLY`。 |
| `anonymousResult` | boolean | 是 | 是否隐藏投票人。 |
| `voteCount` | integer | 是 | 投票人数。 |
| `opensAt` | string 或 null | 是 | 开放时间。非空时，早于该时间不得投票。 |
| `closesAt` | string 或 null | 是 | 关闭时间。非空时，达到或晚于该时间不得投票。`closesAt` 必须晚于 `opensAt`。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### CommunityPollOption

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `optionId` | string | 是 | 选项 ID。 |
| `label` | string | 是 | 1 到 80 位。 |
| `description` | string | 否 | 最多 300 位。 |
| `voteCount` | integer | 后台或公开结果可见 | 投票数。 |

### CommunityReport

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `reportId` | string | 是 | 举报 ID。 |
| `targetType` | string | 是 | `CommunityReportTargetType`。 |
| `targetId` | string | 是 | 目标 ID。 |
| `reasonType` | string | 是 | `CommunityReportReason`。 |
| `description` | string | 是 | 5 到 2000 位。 |
| `evidenceLinks` | string[] | 是 | 0 到 10 个 http、https 或站内链接。站内链接必须以 `/` 开头，禁止 `javascript:`、`file:`、`ftp:` 和空白链接。 |
| `status` | string | 是 | `CommunityReportStatus`。 |
| `reporter` | CommunityAuthorSnapshot | 是 | 举报人快照。 |
| `assigneeUserId` | string 或 null | 后台可见 | 处理人。 |
| `resolution` | string 或 null | 是 | 处理结果摘要。 |
| `internalNote` | string 或 null | 后台可见 | 内部备注。 |
| `linkedPenaltyId` | string 或 null | 后台可见 | 关联处罚。 |
| `notificationStatus` | string 或 null | 是 | 最近通知状态。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `resolvedAt` | string 或 null | 是 | 处理时间。 |

### CommunityTicket

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ticketId` | string | 是 | 工单 ID。 |
| `type` | string | 是 | `CommunityTicketType`。 |
| `title` | string | 是 | 2 到 80 位。 |
| `status` | string | 是 | `CommunityTicketStatus`。 |
| `priority` | string | 是 | `LOW`、`NORMAL`、`HIGH` 或 `URGENT`。 |
| `creator` | CommunityAuthorSnapshot | 是 | 创建人快照。 |
| `assigneeUserId` | string 或 null | 后台可见 | 处理人。 |
| `relatedObject` | object 或 null | 是 | 关联帖子、举报、资源、白名单申请或账号问题摘要。P1 只保存摘要字段，禁止保存 token、完整请求头、内部 URL、服务器命令和前序服务内部路径。 |
| `messages` | CommunityTicketMessage[] | 详情可见 | 工单消息。列表只返回最近摘要。 |
| `lastReplyAt` | string 或 null | 是 | 最近回复时间。 |
| `resolvedAt` | string 或 null | 是 | 解决时间。 |
| `closedAt` | string 或 null | 是 | 关闭时间。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### CommunityTicketMessage

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `messageId` | string | 是 | 消息 ID。 |
| `ticketId` | string | 是 | 工单 ID。 |
| `messageType` | string | 是 | `CommunityTicketMessageType`。 |
| `body` | string | 是 | 1 到 10000 位。 |
| `author` | CommunityAuthorSnapshot 或 null | 是 | 系统事件可为 `null`。 |
| `attachments` | object[] | 是 | P1 只允许站内安全附件摘要，不上传原文件。每个附件必须包含 `attachmentId`、`name` 和以 `/` 开头的站内 `url`，最多 5 个。 |
| `createdAt` | string | 是 | 创建时间。 |

### CommunityPenalty

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `penaltyId` | string | 是 | 处罚 ID。 |
| `targetUserId` | string | 是 | 被处罚用户。 |
| `targetMemberId` | string 或 null | 是 | 被处罚成员。 |
| `type` | string | 是 | `CommunityPenaltyType`。 |
| `status` | string | 是 | `CommunityPenaltyStatus`。 |
| `reason` | string | 是 | 后台原因，1 到 500 位。 |
| `publicReason` | string | 是 | 对用户可见原因，1 到 200 位。 |
| `evidenceReportId` | string 或 null | 是 | 来源举报。 |
| `relatedPostId` | string 或 null | 是 | 关联帖子。 |
| `relatedCommentId` | string 或 null | 是 | 关联评论。 |
| `startsAt` | string | 是 | 生效时间。 |
| `expiresAt` | string 或 null | 是 | 过期时间。警告可为空。 |
| `createdBy` | string | 是 | 创建操作者。 |
| `revokedBy` | string 或 null | 是 | 解除操作者。 |
| `revokedAt` | string 或 null | 是 | 解除时间。 |
| `revokeReason` | string 或 null | 是 | 解除原因。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### CommunityNotificationFailureSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | 固定为 `FAILED`。 |
| `failureCode` | string | 是 | `49220`、`49221` 或 `49222`。 |
| `failureType` | string | 是 | `UNAVAILABLE`、`TIMEOUT` 或 `BAD_SCHEMA`。 |
| `failureReason` | string | 是 | 脱敏失败摘要。 |
| `failedAt` | string | 是 | 失败发生时间。 |

### CommunityAuditLog

审计字段继承公共契约，允许补充 `boardId`、`postId`、`commentId`、`pollId`、`reportId`、`ticketId`、`ticketMessageId`、`penaltyId`、`targetUserId`、`stateFrom`、`stateTo`、`idempotencyKey`、`notificationStatus`、`dependencyStatus`、`profileSnapshotStale`、`linkedContentSnapshotStale` 和 `linkedResourceSnapshotStale`。审计日志不得通过 community API 删除。

## community 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `49000` | 404 | 板块不存在，或当前用户无权访问。 |
| `49001` | 404 | 帖子不存在，或当前用户无权访问。 |
| `49002` | 404 | 评论不存在，或当前用户无权访问。 |
| `49003` | 404 | 投票不存在，或当前用户无权访问。 |
| `49004` | 404 | 举报不存在，或当前用户无权访问。 |
| `49005` | 404 | 工单不存在，或当前用户无权访问。 |
| `49006` | 404 | 处罚记录不存在。 |
| `49010` | 409 | 板块状态不允许该操作。 |
| `49011` | 409 | 帖子状态不允许该操作。 |
| `49012` | 409 | 评论状态不允许该操作。 |
| `49013` | 409 | 投票状态不允许该操作。 |
| `49014` | 409 | 举报状态不允许该操作。 |
| `49015` | 409 | 工单状态不允许该操作。 |
| `49016` | 409 | 处罚状态不允许该操作。 |
| `49017` | 409 | community 幂等键请求指纹冲突。 |
| `49018` | 409 | 重复互动或互动状态冲突。 |
| `49019` | 409 | 重复收藏或收藏状态冲突。 |
| `49020` | 409 | 投票资格不足或选项数量不符合规则。 |
| `49021` | 409 | 重复举报或举报目标状态不允许。 |
| `49022` | 409 | 当前用户处于社区处罚限制期。 |
| `49023` | 409 | 评论层级超过限制。 |
| `49024` | 409 | 关联内容或资源状态不允许公开讨论。 |
| `49200` | 502 | auth 认证上下文不可用。 |
| `49201` | 504 | auth 认证上下文调用超时。 |
| `49202` | 502 | auth 响应字段或枚举不兼容 community 契约。 |
| `49210` | 502 | profile 作者快照不可用。 |
| `49211` | 504 | profile 作者快照调用超时。 |
| `49212` | 502 | profile 响应字段不兼容 community 契约。 |
| `49220` | 502 | notification 投递不可用。 |
| `49221` | 504 | notification 投递超时。 |
| `49222` | 502 | notification 响应字段不兼容 community 契约。 |
| `49230` | 502 | content 公开快照不可用。 |
| `49231` | 504 | content 公开快照调用超时。 |
| `49232` | 502 | content 响应字段不兼容 community 契约。 |
| `49240` | 502 | resource 公开快照不可用。 |
| `49241` | 504 | resource 公开快照调用超时。 |
| `49242` | 502 | resource 响应字段不兼容 community 契约。 |
| `49250` | 502 | attendance 贡献入口不可用。P1 不阻断社区主流程。 |
| `54000` | 500 | community 内部错误。 |
| `54001` | 500 | community 审计写入失败。 |
| `54002` | 500 | community 状态写入失败。 |
| `54003` | 500 | community 互动写入失败。 |
| `54004` | 500 | community 工单消息写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误、通用依赖错误和通用服务端错误优先使用公共错误码。community 自有幂等指纹冲突使用 `49017`。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开板块列表 | GET | `/api/v1/community/boards` | 否 | 游客可读公开板块 | LOW |
| 公开板块详情 | GET | `/api/v1/community/boards/{boardId}` | 否 | 游客可读公开板块 | LOW |
| 公开帖子列表 | GET | `/api/v1/community/posts` | 否 | 游客可读公开帖子 | LOW |
| 公开帖子详情 | GET | `/api/v1/community/posts/{postId}` | 否 | 游客可读公开帖子 | LOW |
| 公开评论列表 | GET | `/api/v1/community/posts/{postId}/comments` | 否 | 游客可读公开评论 | LOW |
| 公开投票详情 | GET | `/api/v1/community/polls/{pollId}` | 否 | 游客可读公开投票 | LOW |
| 公开社区搜索 | GET | `/api/v1/community/search` | 否 | 游客可搜公开内容 | LOW |
| 创建帖子 | POST | `/api/v1/community/me/posts` | 是 | 当前用户 | LOW |
| 修改自己的帖子 | PATCH | `/api/v1/community/me/posts/{postId}` | 是 | 当前用户本人 | LOW |
| 提交帖子审核 | POST | `/api/v1/community/me/posts/{postId}/submit` | 是 | 当前用户本人 | LOW |
| 撤回自己的帖子 | PATCH | `/api/v1/community/me/posts/{postId}/withdraw` | 是 | 当前用户本人 | LOW |
| 创建评论 | POST | `/api/v1/community/me/posts/{postId}/comments` | 是 | 当前用户 | LOW |
| 修改自己的评论 | PATCH | `/api/v1/community/me/comments/{commentId}` | 是 | 当前用户本人 | LOW |
| 归档自己的评论 | PATCH | `/api/v1/community/me/comments/{commentId}/archive` | 是 | 当前用户本人 | LOW |
| 点赞帖子 | POST | `/api/v1/community/me/posts/{postId}/like` | 是 | 当前用户 | LOW |
| 取消点赞帖子 | DELETE | `/api/v1/community/me/posts/{postId}/like` | 是 | 当前用户 | LOW |
| 点赞评论 | POST | `/api/v1/community/me/comments/{commentId}/like` | 是 | 当前用户 | LOW |
| 取消点赞评论 | DELETE | `/api/v1/community/me/comments/{commentId}/like` | 是 | 当前用户 | LOW |
| 收藏帖子 | POST | `/api/v1/community/me/posts/{postId}/favorite` | 是 | 当前用户 | LOW |
| 取消收藏帖子 | DELETE | `/api/v1/community/me/posts/{postId}/favorite` | 是 | 当前用户 | LOW |
| 投票 | POST | `/api/v1/community/me/polls/{pollId}/votes` | 是 | 当前用户 | LOW |
| 举报帖子 | POST | `/api/v1/community/me/posts/{postId}/reports` | 是 | 当前用户 | LOW |
| 举报评论 | POST | `/api/v1/community/me/comments/{commentId}/reports` | 是 | 当前用户 | LOW |
| 我的举报进度 | GET | `/api/v1/community/me/reports` | 是 | 当前用户 | LOW |
| 创建工单 | POST | `/api/v1/community/me/tickets` | 是 | 当前用户 | LOW |
| 我的工单列表 | GET | `/api/v1/community/me/tickets` | 是 | 当前用户 | LOW |
| 我的工单详情 | GET | `/api/v1/community/me/tickets/{ticketId}` | 是 | 当前用户本人 | LOW |
| 补充工单 | PATCH | `/api/v1/community/me/tickets/{ticketId}` | 是 | 当前用户本人 | LOW |
| 关闭自己的工单 | POST | `/api/v1/community/me/tickets/{ticketId}/close` | 是 | 当前用户本人 | LOW |
| 后台板块列表 | GET | `/api/v1/community/admin/boards` | 是 | HELPER+ 读 | LOW |
| 创建板块 | POST | `/api/v1/community/admin/boards` | 是 | ADMIN+ | MEDIUM |
| 修改板块 | PATCH | `/api/v1/community/admin/boards/{boardId}` | 是 | ADMIN+ | MEDIUM |
| 归档板块 | PATCH | `/api/v1/community/admin/boards/{boardId}/archive` | 是 | ADMIN+ | MEDIUM |
| 后台帖子列表 | GET | `/api/v1/community/admin/posts` | 是 | HELPER+ | LOW |
| 后台帖子详情 | GET | `/api/v1/community/admin/posts/{postId}` | 是 | HELPER+ | LOW |
| 审核通过帖子 | PATCH | `/api/v1/community/admin/posts/{postId}/approve` | 是 | HELPER+ | MEDIUM |
| 审核拒绝帖子 | PATCH | `/api/v1/community/admin/posts/{postId}/reject` | 是 | HELPER+ | MEDIUM |
| 要求修改帖子 | PATCH | `/api/v1/community/admin/posts/{postId}/request-changes` | 是 | HELPER+ | MEDIUM |
| 下架帖子 | PATCH | `/api/v1/community/admin/posts/{postId}/offline` | 是 | ADMIN+ | MEDIUM |
| 归档帖子 | PATCH | `/api/v1/community/admin/posts/{postId}/archive` | 是 | ADMIN+ | MEDIUM |
| 软删除帖子 | PATCH | `/api/v1/community/admin/posts/{postId}/delete` | 是 | ADMIN+ | HIGH |
| 后台评论列表 | GET | `/api/v1/community/admin/comments` | 是 | HELPER+ | LOW |
| 审核通过评论 | PATCH | `/api/v1/community/admin/comments/{commentId}/approve` | 是 | HELPER+ | MEDIUM |
| 审核拒绝评论 | PATCH | `/api/v1/community/admin/comments/{commentId}/reject` | 是 | HELPER+ | MEDIUM |
| 下架评论 | PATCH | `/api/v1/community/admin/comments/{commentId}/offline` | 是 | ADMIN+ | MEDIUM |
| 后台举报列表 | GET | `/api/v1/community/admin/reports` | 是 | HELPER+ | LOW |
| 后台举报详情 | GET | `/api/v1/community/admin/reports/{reportId}` | 是 | HELPER+ | LOW |
| 分配举报 | PATCH | `/api/v1/community/admin/reports/{reportId}/assign` | 是 | HELPER+ | MEDIUM |
| 处理举报 | PATCH | `/api/v1/community/admin/reports/{reportId}/resolve` | 是 | HELPER+ | MEDIUM |
| 驳回举报 | PATCH | `/api/v1/community/admin/reports/{reportId}/dismiss` | 是 | HELPER+ | MEDIUM |
| 后台工单列表 | GET | `/api/v1/community/admin/tickets` | 是 | HELPER+ | LOW |
| 后台工单详情 | GET | `/api/v1/community/admin/tickets/{ticketId}` | 是 | HELPER+ | LOW |
| 分配工单 | PATCH | `/api/v1/community/admin/tickets/{ticketId}/assign` | 是 | HELPER+ | MEDIUM |
| 回复工单 | POST | `/api/v1/community/admin/tickets/{ticketId}/messages` | 是 | HELPER+ | MEDIUM |
| 推进工单状态 | PATCH | `/api/v1/community/admin/tickets/{ticketId}/status` | 是 | HELPER+ | MEDIUM |
| 创建处罚 | POST | `/api/v1/community/admin/penalties` | 是 | ADMIN+ | HIGH |
| 修正处罚 | PATCH | `/api/v1/community/admin/penalties/{penaltyId}` | 是 | ADMIN+ | HIGH |
| 解除处罚 | PATCH | `/api/v1/community/admin/penalties/{penaltyId}/revoke` | 是 | ADMIN+ | HIGH |
| 创建投票 | POST | `/api/v1/community/admin/polls` | 是 | ADMIN+ | MEDIUM |
| 修改投票 | PATCH | `/api/v1/community/admin/polls/{pollId}` | 是 | ADMIN+ | MEDIUM |
| 开放投票 | PATCH | `/api/v1/community/admin/polls/{pollId}/open` | 是 | ADMIN+ | MEDIUM |
| 关闭投票 | PATCH | `/api/v1/community/admin/polls/{pollId}/close` | 是 | ADMIN+ | MEDIUM |
| community 审计列表 | GET | `/api/v1/community/admin/audit-logs` | 是 | ADMIN+ | LOW |
| community 自检摘要 | GET | `/api/v1/community/admin/ops/summary` | 是 | HELPER+ | LOW |

## 公开接口

### 公开板块列表

`GET /api/v1/community/boards`

查询参数：`visibility`、`keyword`、`page`、`pageSize` 和 `sort`。`pageSize` 最大 `100`。`sort` 允许 `sortOrder_asc`、`lastPostAt_desc`、`postCount_desc` 和 `createdAt_desc`。

成功响应 HTTP `200`，分页 `items` 为 `CommunityBoard[]`。只返回 `ACTIVE` 且当前访问者可见的板块。

### 公开板块详情

`GET /api/v1/community/boards/{boardId}`

成功响应 HTTP `200`，`data` 为 `CommunityBoard`，并包含最近已通过帖子摘要。不存在或不可见返回 `49000`。

### 公开帖子列表

`GET /api/v1/community/posts`

查询参数：`boardId`、`type`、`tag`、`keyword`、`authorUserId`、`linkedContentId`、`linkedResourceId`、`page`、`pageSize` 和 `sort`。`sort` 允许 `lastCommentAt_desc`、`createdAt_desc`、`likeCount_desc`、`viewCount_desc`。

成功响应 HTTP `200`，分页 `items` 为公开视图 `CommunityPost[]`。只返回 `APPROVED`、未下架、未归档、未软删除帖子。响应必须脱敏，不返回 `internalNote`、`reviewerUserId`、举报和处罚字段。

### 公开帖子详情

`GET /api/v1/community/posts/{postId}`

成功响应 HTTP `200`，`data` 为公开视图 `CommunityPost`。读取可增加 `viewCount`，但该计数必须服务端限流和去重，不能由前端直接传入。帖子不存在或不可见返回 `49001`。

### 公开评论列表

`GET /api/v1/community/posts/{postId}/comments`

查询参数：`page`、`pageSize`、`parentCommentId` 和 `sort`。`sort` 允许 `createdAt_asc`、`createdAt_desc`、`likeCount_desc`。

成功响应 HTTP `200`，分页 `items` 为公开视图 `CommunityComment[]`。只返回 `APPROVED` 评论。帖子不存在或不可见返回 `49001`。

### 公开投票详情

`GET /api/v1/community/polls/{pollId}`

成功响应 HTTP `200`，`data` 为 `CommunityPoll`。`OPEN` 投票按 `anonymousResult` 和展示策略返回结果摘要；未到公开时间或不可见返回 `49003`。

### 公开社区搜索

`GET /api/v1/community/search`

查询参数：`keyword` 必填 1 到 80 位，`scope` 允许 `ALL`、`POST`、`COMMENT`、`BOARD`，`page`、`pageSize` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为公开搜索摘要，不返回草稿、待审核、工单、举报、处罚和内部备注。

## 当前用户接口

### 创建帖子

`POST /api/v1/community/me/posts`

请求字段：`boardId`、`type`、`title`、`summary`、`body`、`tags`、`linkedContentId`、`linkedResourceId`、`pollDraft` 和 `idempotencyKey`。`title` 2 到 80 位，`body` 1 到 20000 位，`tags` 0 到 8 个。成功响应 HTTP `201`，`data` 为 `CommunityPost`，默认状态为 `DRAFT`。重复同一幂等键和同一请求体返回 HTTP `200`。

业务规则：必须校验板块可见、板块允许该帖子类型、当前用户未被处罚限制、profile 快照可用。引用 content 或 resource 时只能保存公开快照。浏览器传入服务端可信字段必须忽略或返回字段校验失败。

### 修改自己的帖子

`PATCH /api/v1/community/me/posts/{postId}`

请求字段同创建帖子，可部分更新，必须包含 `idempotencyKey` 时按幂等处理。只允许作者修改 `DRAFT` 或 `NEEDS_CHANGES` 帖子。成功响应 HTTP `200`，`data` 为更新后的 `CommunityPost`。状态不允许返回 `49011`。

### 提交帖子审核

`POST /api/v1/community/me/posts/{postId}/submit`

请求字段：`idempotencyKey` 可选。`DRAFT` 或 `NEEDS_CHANGES` 可提交为 `PENDING_REVIEW`。已是 `PENDING_REVIEW` 时同请求幂等成功。成功响应 HTTP `200`。

### 撤回自己的帖子

`PATCH /api/v1/community/me/posts/{postId}/withdraw`

请求字段：`reason` 1 到 200 位，`idempotencyKey` 可选。只允许作者撤回 `DRAFT`、`PENDING_REVIEW` 或 `NEEDS_CHANGES` 帖子，成功后回到 `DRAFT` 或进入 `ARCHIVED`，实现必须固定一种策略并测试。已公开、已下架或终态帖子返回 `49011`。

### 创建评论

`POST /api/v1/community/me/posts/{postId}/comments`

请求字段：`body`、`parentCommentId`、`idempotencyKey`。成功响应 HTTP `201`，`data` 为 `CommunityComment`。普通用户评论默认 `PENDING_REVIEW`，管理员或可信成员是否可直接 `APPROVED` 由实现固定并测试。锁定帖子、下架帖子、超过二级回复、被处罚限制时返回对应冲突错误。

### 修改自己的评论

`PATCH /api/v1/community/me/comments/{commentId}`

请求字段：`body`、`idempotencyKey`。只允许作者修改 `PENDING_REVIEW`、`NEEDS_CHANGES` 或实现固定的可编辑 `APPROVED` 评论。成功后需要保留评论版本，且重新进入审核策略必须固定。

### 归档自己的评论

`PATCH /api/v1/community/me/comments/{commentId}/archive`

请求字段：`reason` 和 `idempotencyKey`。只允许作者归档自己的非终态评论。成功响应 HTTP `200`，状态为 `ARCHIVED` 或 `DELETED`，实现必须固定一种软删除策略。

### 帖子和评论互动

`POST /api/v1/community/me/posts/{postId}/like`、`DELETE /api/v1/community/me/posts/{postId}/like`、`POST /api/v1/community/me/comments/{commentId}/like`、`DELETE /api/v1/community/me/comments/{commentId}/like`

请求字段：`idempotencyKey` 可选。点赞已点赞目标返回幂等成功，不得重复增加计数。取消未点赞目标返回幂等成功。目标不存在或不可见返回 `49001` 或 `49002`。并发点赞必须保证每个用户每个目标最多一条有效互动。

### 收藏帖子

`POST /api/v1/community/me/posts/{postId}/favorite`、`DELETE /api/v1/community/me/posts/{postId}/favorite`

请求字段：`idempotencyKey` 可选。收藏和取消收藏必须幂等。只能收藏可见帖子，不得收藏草稿、待审核、下架或软删除帖子。

### 投票

`POST /api/v1/community/me/polls/{pollId}/votes`

请求字段：`optionIds` 必填，1 到 10 个，`idempotencyKey` 可选。成功响应 HTTP `200`，`data` 包含投票记录摘要和更新后的投票结果。只允许 `OPEN` 投票；资格不足、重复投票、选项数量不满足规则返回 `49020`。投票记录必须服务端按用户去重。是否允许改票由实现固定，推荐 P1 禁止改票。

`eligibleVisibility` 必须在投票时生效。`PUBLIC` 允许所有可写入社区的登录用户投票，`MEMBER_ONLY` 允许普通成员和工作人员投票，`STAFF_ONLY` 只允许 `HELPER`、`ADMIN` 或 `OWNER` 投票。`opensAt` 和 `closesAt` 必须按服务端时间判断，未开放或已关闭返回 `49020`，权限不足返回 `42001`。

### 举报帖子和评论

`POST /api/v1/community/me/posts/{postId}/reports`、`POST /api/v1/community/me/comments/{commentId}/reports`

请求字段：`reasonType`、`description`、`evidenceLinks`、`idempotencyKey`。成功响应 HTTP `201`，`data` 为 `CommunityReport` 当前用户视图。重复举报同一目标和同一原因在未处理前返回 `49021` 或幂等结果。举报不得向被举报用户泄露举报人。

`evidenceLinks` 必须逐条校验协议，只允许 `http://`、`https://` 或以 `/` 开头的站内链接。非法链接返回 `40001`，不得创建举报。

### 我的举报进度

`GET /api/v1/community/me/reports`

查询参数：`status`、`targetType`、`page`、`pageSize` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为当前用户自己的举报摘要。响应不得返回处理人内部备注、处罚证据和其他举报人信息。

### 创建工单

`POST /api/v1/community/me/tickets`

请求字段：`type`、`title`、`body`、`relatedObject`、`attachments`、`idempotencyKey`。成功响应 HTTP `201`，`data` 为 `CommunityTicket` 当前用户视图，状态为 `OPEN` 或 `WAITING_STAFF`。账号状态为 `PENDING_PROFILE` 的用户可以创建账号、白名单和资源问题工单。

`relatedObject` 和 `attachments` 必须保存到工单详情。公开给创建人的视图和后台视图都可以返回安全摘要，但不得返回内部备注、token、请求头、异常堆栈、内部 URL 或服务器命令。

### 我的工单列表和详情

`GET /api/v1/community/me/tickets`、`GET /api/v1/community/me/tickets/{ticketId}`

列表查询支持 `status`、`type`、`page`、`pageSize` 和 `sort`。详情只返回当前用户自己的工单，消息中不得返回 `INTERNAL_NOTE`。

### 补充和关闭自己的工单

`PATCH /api/v1/community/me/tickets/{ticketId}` 用于补充回复，请求字段为 `body`、`attachments`、`idempotencyKey`，只允许 `OPEN`、`WAITING_USER` 或 `WAITING_STAFF`。成功后状态进入 `WAITING_STAFF`。

`POST /api/v1/community/me/tickets/{ticketId}/close` 请求字段为 `reason` 和 `idempotencyKey`，只允许创建人关闭自己的非终态工单。成功后状态为 `CLOSED`。

## 后台接口

后台列表接口默认支持 `page`、`pageSize`、`keyword`、状态筛选和稳定排序，`pageSize` 最大 `100`。后台详情可返回内部备注、依赖摘要、通知失败摘要和审计关联，但仍不得返回 token、完整请求头、异常堆栈、通知正文、前序服务内部路径、真实服务器命令、节点凭据或 Cloudreve token。

### 板块管理

`GET /api/v1/community/admin/boards` 返回全部板块分页。`POST /api/v1/community/admin/boards` 创建板块，请求字段为 `slug`、`name`、`description`、`visibility`、`status`、`allowedPostTypes`、`tags`、`sortOrder`、`reason` 和 `idempotencyKey`。`PATCH /api/v1/community/admin/boards/{boardId}` 修改板块。`PATCH /api/v1/community/admin/boards/{boardId}/archive` 归档板块，请求字段为 `reason` 和 `idempotencyKey`。归档板块后不得接收新帖子。

### 帖子审核和治理

`GET /api/v1/community/admin/posts` 和 `GET /api/v1/community/admin/posts/{postId}` 用于后台帖子列表和详情。

`PATCH /api/v1/community/admin/posts/{postId}/approve` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，只允许 `PENDING_REVIEW` 或 `NEEDS_CHANGES` 帖子通过，成功后状态为 `APPROVED`，可触发通知。

`PATCH /api/v1/community/admin/posts/{postId}/reject` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，成功后状态为 `REJECTED`。

`PATCH /api/v1/community/admin/posts/{postId}/request-changes` 请求字段为 `reviewComment`、`internalNote`、`reason`、`idempotencyKey`，成功后状态为 `NEEDS_CHANGES`。

`PATCH /api/v1/community/admin/posts/{postId}/offline` 请求字段为 `publicReason`、`reason`、`idempotencyKey`，只允许 `APPROVED` 或 `LOCKED` 帖子，成功后状态为 `OFFLINE`。

`PATCH /api/v1/community/admin/posts/{postId}/archive` 请求字段为 `reason` 和 `idempotencyKey`，成功后状态为 `ARCHIVED`。

`PATCH /api/v1/community/admin/posts/{postId}/delete` 是 `HIGH` 风险，请求字段为 `reason`、`confirmText` 和 `idempotencyKey`，P1 固定要求 `DELETE_COMMUNITY_POST`。成功后状态为 `DELETED`，只做软删除。

### 评论审核和治理

`GET /api/v1/community/admin/comments` 返回后台评论分页，可按 `postId`、`authorUserId`、`status`、`keyword` 筛选。`PATCH /api/v1/community/admin/comments/{commentId}/approve`、`reject` 和 `offline` 分别用于通过、拒绝和下架评论，请求字段包含 `reviewComment` 或 `publicReason`、`internalNote`、`reason` 和 `idempotencyKey`。非法状态返回 `49012`。

### 举报处理

`GET /api/v1/community/admin/reports` 和 `GET /api/v1/community/admin/reports/{reportId}` 用于举报队列。

`PATCH /api/v1/community/admin/reports/{reportId}/assign` 请求字段为 `assigneeUserId`、`reason`、`idempotencyKey`。`HELPER` 只能分配给自己，`ADMIN` 和 `OWNER` 可分配给任一具备后台权限的用户。

`PATCH /api/v1/community/admin/reports/{reportId}/resolve` 请求字段为 `resolution`、`internalNote`、`linkedPenaltyId`、`reason`、`idempotencyKey`。成功后状态为 `RESOLVED`，可以关联已创建处罚。

如果传入 `linkedPenaltyId`，服务端必须确认处罚存在，并把该 ID 保存到举报后台视图。该字段只在后台可见，不得出现在举报人视图或公开接口中。

`PATCH /api/v1/community/admin/reports/{reportId}/dismiss` 请求字段为 `resolution`、`internalNote`、`reason`、`idempotencyKey`。成功后状态为 `DISMISSED`。

### 工单处理

`GET /api/v1/community/admin/tickets` 和 `GET /api/v1/community/admin/tickets/{ticketId}` 用于工单队列。

`PATCH /api/v1/community/admin/tickets/{ticketId}/assign` 请求字段为 `assigneeUserId`、`reason`、`idempotencyKey`。

`POST /api/v1/community/admin/tickets/{ticketId}/messages` 请求字段为 `messageType`、`body`、`attachments`、`reason`、`idempotencyKey`。`HELPER` 可以写 `STAFF_REPLY`，只有 `ADMIN` 和 `OWNER` 可写 `INTERNAL_NOTE`。

`PATCH /api/v1/community/admin/tickets/{ticketId}/status` 请求字段为 `status`、`publicComment`、`reason`、`idempotencyKey`。只允许在 `OPEN`、`WAITING_STAFF`、`WAITING_USER`、`RESOLVED`、`CLOSED` 之间按服务端状态机推进。非法跳转返回 `49015`。

后台工单状态推进使用固定状态机。`OPEN`、`WAITING_STAFF` 和 `WAITING_USER` 可以进入 `WAITING_STAFF`、`WAITING_USER`、`RESOLVED` 或 `CLOSED`；`RESOLVED` 只能进入 `CLOSED`；`CLOSED` 和 `ARCHIVED` 是终态，不得再改为非终态；`ARCHIVED` 只允许 `ADMIN` 或 `OWNER` 从 `CLOSED` 推进。

### 处罚管理

`POST /api/v1/community/admin/penalties` 是 `HIGH` 风险，请求字段为 `targetUserId`、`type`、`publicReason`、`reason`、`evidenceReportId`、`relatedPostId`、`relatedCommentId`、`startsAt`、`expiresAt`、`confirmText`、`idempotencyKey`，P1 固定要求 `CREATE_COMMUNITY_PENALTY`。成功响应 HTTP `201`，`data` 为 `CommunityPenalty`。处罚只影响 community 写权限和社区治理状态，不执行真实服务器命令，不移除白名单，不改 attendance 积分。

`PATCH /api/v1/community/admin/penalties/{penaltyId}` 是 `HIGH` 风险，用于修正公开原因、后台原因、过期时间和关联证据。请求必须包含 `reason` 和 `idempotencyKey`。

`PATCH /api/v1/community/admin/penalties/{penaltyId}/revoke` 是 `HIGH` 风险，请求字段为 `publicReason`、`reason`、`confirmText`、`idempotencyKey`，P1 固定要求 `REVOKE_COMMUNITY_PENALTY`。成功后状态为 `REVOKED`。

### 投票管理

`POST /api/v1/community/admin/polls` 创建轻量投票，请求字段为 `title`、`description`、`postId`、`options`、`multipleChoice`、`minChoices`、`maxChoices`、`eligibleVisibility`、`anonymousResult`、`opensAt`、`closesAt`、`reason` 和 `idempotencyKey`，成功响应 HTTP `201`，默认 `DRAFT`。

`PATCH /api/v1/community/admin/polls/{pollId}` 只允许修改 `DRAFT` 投票。`PATCH /api/v1/community/admin/polls/{pollId}/open` 使投票进入 `OPEN`。`PATCH /api/v1/community/admin/polls/{pollId}/close` 使投票进入 `CLOSED`。关闭后不得再投票。

### 审计和自检

`GET /api/v1/community/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`result`、`from`、`to` 和 `sort`。成功响应 HTTP `200`，分页 `items` 为 `CommunityAuditLog[]`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 community API 删除。

`GET /api/v1/community/admin/ops/summary` 成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "community",
    "port": 8132,
    "legacyPort": 8112,
    "storageMode": "IN_MEMORY",
    "authMode": "TEST_STUB",
    "profileMode": "TEST_STUB",
    "notificationMode": "TEST_STUB",
    "contentMode": "TEST_STUB",
    "resourceMode": "TEST_STUB",
    "attendanceMode": "SKIPPED",
    "testControlsEnabled": false,
    "boardsTotal": 4,
    "postsTotal": 20,
    "pendingReviewPostsTotal": 3,
    "commentsTotal": 60,
    "openReportsTotal": 2,
    "openTicketsTotal": 4,
    "activePenaltiesTotal": 1,
    "pollsOpenTotal": 1,
    "auditsTotal": 100,
    "idempotencyRecordsTotal": 30,
    "lastAuditAt": "2026-05-24T12:00:00Z",
    "productionGaps": [
      "P1_IN_MEMORY_STORAGE",
      "P1_AUTH_STUB",
      "P1_PROFILE_STUB",
      "P1_NOTIFICATION_STUB",
      "P1_CONTENT_STUB",
      "P1_RESOURCE_STUB",
      "ATTENDANCE_CONTRIBUTION_NOT_CONNECTED",
      "TEST_CONTROLS_DISABLED_OUTSIDE_TEST"
    ]
  }
}
```

## 状态、幂等和并发

板块创建后可为 `DRAFT` 或 `ACTIVE`。`ACTIVE` 可锁定为 `LOCKED`，可归档为 `ARCHIVED`。`ARCHIVED` 为终态，不接受新帖子。

帖子创建后默认 `DRAFT`。`DRAFT` 可提交为 `PENDING_REVIEW`。`PENDING_REVIEW` 可通过为 `APPROVED`、拒绝为 `REJECTED`、要求修改为 `NEEDS_CHANGES`，也可由作者撤回。`NEEDS_CHANGES` 可由作者修改后再次提交。`APPROVED` 可锁定、下架、归档或软删除。`REJECTED`、`OFFLINE`、`ARCHIVED` 和 `DELETED` 不得直接回到公开状态，除非未来兼容变更补充恢复接口。

评论创建后默认 `PENDING_REVIEW` 或按可信策略直接 `APPROVED`，实现必须固定并测试。`PENDING_REVIEW` 可通过、拒绝或要求修改。`APPROVED` 可下架、归档或软删除。评论不得在帖子已锁定、下架、归档或删除后继续创建。

举报创建后为 `OPEN`，可分配为 `UNDER_REVIEW`，可处理为 `RESOLVED`、驳回为 `DISMISSED`、升级为 `ESCALATED`，最终可归档。举报处理不得自动创建处罚，必须通过处罚接口明确创建。

工单创建后为 `OPEN` 或 `WAITING_STAFF`。用户补充后进入 `WAITING_STAFF`，工作人员回复后进入 `WAITING_USER` 或 `RESOLVED`，用户或后台可关闭为 `CLOSED`。`ARCHIVED` 为终态。

处罚创建后为 `ACTIVE`。到期后可进入 `EXPIRED`，后台解除后进入 `REVOKED`，归档后进入 `ARCHIVED`。处罚只影响 community 写权限，不调用 whitelist、attendance、真实服务器或 ops-control。

写接口支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果；相同幂等键搭配不同请求体返回 `49017`。请求体指纹必须基于结构化 JSON 规范化结果，所有嵌套对象按字段名递归排序。

并发发帖同一幂等键只能产生一个帖子。并发点赞、取消点赞、收藏和取消收藏必须按用户和目标去重。并发审核同一帖子或评论只能有一个最终状态。并发处理同一举报、工单或处罚只能有一个成功状态推进。读取接口允许读到更新前或更新后的完整状态，不能返回半更新对象。

## 审计要求

必须审计的动作包括板块创建、板块修改、板块归档、帖子创建、帖子修改、帖子提交审核、帖子撤回、帖子审核通过、帖子审核拒绝、帖子要求修改、帖子下架、帖子归档、帖子软删除、评论创建、评论修改、评论审核、评论下架、点赞、取消点赞、收藏、取消收藏、投票、举报创建、举报分配、举报处理、举报驳回、工单创建、工单补充、工单分配、工单回复、工单状态推进、工单关闭、处罚创建、处罚修正、处罚解除、投票创建、投票开放、投票关闭、通知失败、依赖降级、自检读取、审计写入失败和状态写入失败。

后台写操作必须记录 `reason`、操作者、目标对象、操作前状态、操作后状态、请求编号、参数摘要和结果。审计字段继承公共契约。审计不得泄露 token、完整请求头、Minecraft 验证凭据、profile 后台备注全文、通知正文全文、举报人不该公开的信息、处罚证据原文、真实服务器命令、节点凭据、内部异常堆栈或前序服务内部路径。

审计写入失败时，板块写入、帖子审核、评论审核、举报处理、工单回复、处罚创建、处罚修正、处罚解除、投票管理和高风险软删除不得假装成功，必须返回 `54001` 或 `54000`，并保持业务数据不变。通知失败不回滚主状态，但必须记录失败摘要和审计。

## 失败降级

auth 是所有登录接口强依赖。auth 不可用、超时、用户状态不允许或字段不兼容时不得伪造成功。

profile 是创建帖子、评论、工单、举报和处罚目标快照的强依赖。写入时 profile 失败不得伪造作者或目标资料。公开读取时 profile 失败可以使用旧快照降级，但必须标记 stale。

notification 是辅助依赖。帖子审核、评论审核、举报处理、工单回复、处罚生效和处罚解除的通知失败不得回滚社区主状态，但必须保存脱敏失败摘要和审计。

content 和 resource 是关联讨论的辅助依赖。创建关联讨论时，公开快照不可用不得创建关联帖子。读取已存在帖子时，来源服务失败可以使用已保存快照降级并标记 stale。

attendance 在 P1 不作为社区主流程依赖。社区贡献候选可以记录为 `SKIPPED` 或 `FAILED`，但不得直接写积分余额、流水或榜单。

任何状态写入和互动计数必须保持一致。不能出现点赞记录写入失败但计数增加，或计数更新成功但返回错误后无法追踪的半状态。半成功风险必须返回 `54002` 或 `54003` 并保持可复核状态。

## 验收口径

`community` API 文档按 `docs/contracts-community.md` 独立存在，并由 `.local-docs/tests-community.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`community` 完成时必须满足以下条件：全部接口按本文档实现；公开接口只返回公开可见数据；当前用户接口只能访问自己的帖子草稿、评论、互动、收藏、投票、举报和工单；后台接口按角色限制；帖子、评论、举报、工单、处罚和投票有服务端状态机；举报和工单不是前端假状态；处罚只影响 community 写权限，不执行白名单移除、服务器命令或 attendance 积分修改；所有后台写操作和高风险操作有审计；通知失败按辅助降级记录；当前运行端口固定为 `8132`，自检摘要返回 `port=8132` 和 `legacyPort=8112`；`.local-docs/tests-community.md` 与 `.local-docs/tests-engagement-core.md` 中全部测试用例都有对应自动化验证；自动化测试必须先红灯；实现后 community 在 `engagement-core-service` 中全部测试通过；auth、profile、notification、content、server-status、resource、admin、onboarding、exam、whitelist 和 attendance 前序服务回归测试通过；不恢复 `backend/community-service` 旧入口；没有修改前序服务稳定接口；没有把活动、日历、更新日志、后台聚合、真实服务器操作、文件管理、容器、终端、日志流、节点注册、备份恢复或 Cloudreve 管理能力塞进 community。

生产化硬化验收还必须满足：测试控制头默认关闭，只有本地自动化测试显式启用时才生效；关闭状态下依赖失败模拟头、写入失败模拟头和通知失败模拟头全部被忽略；自检摘要明确返回当前测试控制头开关状态。
