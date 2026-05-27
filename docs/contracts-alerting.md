# 北冥官网 alerting API 契约

版本：0.2

## 文档定位

本文档是 `alerting` 微服务的正式 API 契约。`alerting` 负责告警源摘要、告警规则、规则评估、告警实例、去重分组、静默、通知路由、投递摘要、确认关闭、审计列表和自检摘要。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `alerting` 的职责边界、数据归属、前序服务兼容、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考 Prometheus Alertmanager、Grafana Alerting、PagerDuty、Opsgenie、Amazon CloudWatch 和 Datadog Monitor 的公开设计。Alertmanager 的 grouping、deduplication、routing、silencing 和 inhibition 适合告警风暴控制；Grafana 的 contact point、notification policy、silence 和 mute timing 适合把告警规则与通知渠道拆开；PagerDuty 的事件编排、`dedup_key` 和通用事件字段适合把来源事件整理成可处理事件；Opsgenie 的 `alias` 去重和 acknowledge、close、snooze 生命周期适合告警处理闭环；CloudWatch 的复合告警和抑制等待窗口适合维护窗口和上游故障抑制；Datadog 的 monitor、renotify 和 escalation message 适合重复提醒和升级摘要。本文档只吸收规则、事件、抑制、路由、去重键和闭环模型，不接入这些平台的主数据，也不在第一版发送真实外部通知。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Prometheus Alertmanager](https://prometheus.io/docs/alerting/latest/alertmanager/) | 告警服务负责去重、分组、路由、静默和抑制，不直接替代指标采集。 |
| [Grafana Alerting notifications](https://grafana.com/docs/grafana/latest/alerting/fundamentals/notifications/) | 通知目的地和通知策略独立于规则，避免规则里硬编码渠道。 |
| [PagerDuty Event Orchestration](https://support.pagerduty.com/main/docs/event-orchestration) | 来源事件先经过编排和路由，再进入处理闭环。 |
| [Opsgenie Alert API](https://docs.opsgenie.com/docs/alert-api) | 告警别名可作为客户端定义的去重键，确认、关闭和静默类操作需要保留处理记录。 |
| [Amazon CloudWatch alarm suppression](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/alarm-suppression.html) | 复合告警、等待期和扩展期可减少维护窗口和上游故障产生的噪音。 |
| [Datadog monitor notifications](https://docs.datadoghq.com/monitors/notify/) | 重复提醒、升级消息和通知变量应是告警路由策略的一部分。 |

## 职责边界

`alerting` 负责统一告警控制面。它保存来源服务健康摘要、规则定义、规则评估摘要、告警实例、去重指纹、分组键、静默规则、抑制摘要、通知路由、投递摘要、确认关闭记录、幂等记录和告警审计。

`alerting` 不负责用户登录、角色能力点主数据、站内通知主数据、真实短信、真实邮件、真实 Webhook、真实指标采集、真实节点操作、真实备份恢复、真实 Cloudreve 调用、真实容器或虚拟机控制、玩家资源下载、社区工单主数据或后台聚合主数据。

第一版固定为内存存储和受控测试适配器。它可以使用前序服务健康、指标、任务失败和风险摘要的快照，不能直接导入前序服务 Java 类、内存 store、测试种子或私有数据结构。跨服务字段只能来自正式 API、后端入口可信认证上下文或契约允许的本地测试 stub。

## 数据归属

`alerting` 拥有以下主数据：AlertSource、AlertRule、AlertEvaluation、AlertInstance、AlertSilence、AlertRoute、AlertDelivery、AlertingAuditLog、AlertingOpsSummary 和幂等记录。

`alerting` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和状态快照；可以保存来自 `ops-control`、`node-daemon`、`server-status`、`cloudreve-sync` 和 `backup-recovery` 的健康或异常摘要；可以保存来自 `notification` 的投递引用摘要。所有保存内容都只能是安全摘要，不得保存访问 token、节点密钥、Cloudreve 管理凭据、内部绝对路径、完整通知正文、完整请求头或异常堆栈。

## 基础路径、端口和认证

所有接口默认使用 `/api/v1/alerting` 前缀。第一版本地端口固定为 `8120`，自检摘要必须返回该端口。

健康检查 `GET /api/v1/alerting/health` 不要求认证，但只能返回存活、版本、服务名、状态和请求编号，不返回告警数量、路由详情、来源摘要、依赖错误细节或任何敏感字段。

除健康检查外，全部接口要求 `Authorization: Bearer <token>`。读取类接口要求后台角色 `HELPER`、`ADMIN` 或 `OWNER`，并具备 `NODE_READ`、`HIGH_RISK_APPROVE` 或等效后台读取能力。规则、静默、通知路由、确认和关闭写接口要求 `ADMIN` 或 `OWNER`。高风险升级策略、强制关闭严重告警和路由测试要求 `HIGH_RISK_APPROVE` 或 `OWNER`。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`beforeState`、`afterState`、`auditResult`、`internalPath`、`resolvedPath`、`rawToken`、`credential`、`secretKey`、`nodeToken`、`notificationToken`、`webhookSecret`、`smtpPassword`、`smsToken`、`deliveryStatus`、`createdBy`、`updatedBy`、`acknowledgedBy`、`closedBy` 和 `suppressedBy` 等服务端可信字段。可信字段必须递归检查，嵌套在 `sourceSnapshot`、`labels`、`conditionSummary`、`matchers`、`notificationTemplateRef`、`receiverSummary` 或任意数组对象中也必须拒绝。出现可信字段时返回 `40001`。

## 本地测试控制头

`alerting` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Source-Mode`、`X-Test-Notification-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store` 和 `X-Test-Now` 模拟认证失败、来源服务不可用、来源超时、来源坏 schema、通知不可用、通知超时、审计失败、状态写入失败和时间边界。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发认证失败、来源失败、通知失败、审计失败、存储失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 纳入生产化硬化项。

## 前序服务兼容契约

`auth` 是强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。auth 不可用返回 `46920`，auth 超时返回 `46921`，auth 字段或枚举不兼容返回 `46922`。

`notification` 是投递依赖。`alerting` 只生成告警投递请求和投递摘要，不保存 notification 通知正文主数据，不绕过 notification 自建渠道。notification 不可用返回 `46900`，notification 超时返回 `46901`，notification 字段不兼容返回 `46902`。通知失败只影响投递摘要，不得自动关闭告警实例。

`admin` 是后台聚合入口。`alerting` 可以向 admin 暴露模块健康、待处理告警数量、严重级别摘要和审计摘要，不能让 admin 修改告警规则或告警状态。admin 尚未声明 `ALERTING` 入口时，本轮不得修改 admin 稳定接口。

`ops-control` 和 `node-daemon` 是运维状态来源。`alerting` 可以消费节点离线、指标超阈值、任务失败、审批超时和心跳异常摘要，不能创建节点任务，不能执行终端、文件、容器、虚拟机或实例操作。

`server-status` 是玩家可见状态来源。`alerting` 可以把公开服务状态异常作为来源快照，但不能替代 server-status 展示接口，不能执行线路或实例操作。

`cloudreve-sync` 可以提供 provider 不可用、配额 WARNING、配额 EXCEEDED、同步任务失败和分享失效摘要。`alerting` 不能保存 Cloudreve token、分享密码、私有直链或真实文件路径。

`backup-recovery` 可以提供备份失败、备份点校验失败、恢复演练失败、恢复申请待审批和生产恢复阻断摘要。`alerting` 不能触发真实恢复，也不能绕过 backup-recovery 的恢复审批。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `AlertSourceService` | `OPS_CONTROL`、`NODE_DAEMON`、`SERVER_STATUS`、`CLOUDREVE_SYNC`、`BACKUP_RECOVERY`、`NOTIFICATION`、`ADMIN` | 第一版可登记的告警来源服务。 |
| `AlertSourceType` | `HEALTH`、`METRIC`、`TASK`、`QUOTA`、`SCHEDULE`、`AUDIT`、`DEPENDENCY` | 来源类型。 |
| `AlertSeverity` | `INFO`、`WARNING`、`CRITICAL`、`BLOCKER` | 告警严重级别。 |
| `AlertRuleStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`ARCHIVED` | 规则状态。 |
| `AlertConditionType` | `THRESHOLD`、`MISSING_HEARTBEAT`、`TASK_FAILED`、`QUOTA_EXCEEDED`、`DEPENDENCY_DOWN`、`RESTORE_PENDING`、`MANUAL_EVENT` | 第一版规则条件类型。 |
| `AlertEvaluationStatus` | `MATCHED`、`NOT_MATCHED`、`SUPPRESSED`、`SOURCE_UNAVAILABLE`、`FAILED` | 手动评估结果。 |
| `AlertInstanceStatus` | `FIRING`、`ACKNOWLEDGED`、`SUPPRESSED`、`RESOLVED`、`CLOSED` | 告警实例状态。 |
| `AlertSilenceStatus` | `ACTIVE`、`EXPIRED`、`CANCELLED` | 静默状态。 |
| `AlertRouteStatus` | `ENABLED`、`DISABLED` | 通知路由状态。 |
| `AlertDeliveryStatus` | `PENDING`、`SENT`、`FAILED`、`RETRYING`、`SUPPRESSED` | 投递摘要状态。 |
| `AlertDependencyStatus` | `AVAILABLE`、`UNAVAILABLE`、`TIMEOUT`、`BAD_SCHEMA`、`DISABLED` | 依赖摘要状态。 |
| `AlertingAuditResult` | `SUCCESS`、`FAILED` | 审计结果。 |

## 通用对象

### AlertSource

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sourceId` | string | 是 | 告警来源 ID。 |
| `sourceService` | string | 是 | `AlertSourceService`。 |
| `sourceType` | string | 是 | `AlertSourceType`。 |
| `displayName` | string | 是 | 展示名，2 到 80 位。 |
| `enabled` | boolean | 是 | 是否启用。 |
| `healthStatus` | string | 是 | `AlertDependencyStatus` 或来源状态摘要。 |
| `lastEventAt` | string 或 null | 是 | 最近事件时间。 |
| `lastSnapshotAt` | string 或 null | 是 | 最近来源快照时间。 |
| `capabilities` | string[] | 是 | 来源能力摘要。 |
| `labels` | object | 是 | 标签，最多 20 个键值。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReasons` | string[] | 是 | 降级原因，必须脱敏。 |

### AlertRule

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ruleId` | string | 是 | 规则 ID。 |
| `displayName` | string | 是 | 规则名称，2 到 80 位。 |
| `sourceService` | string | 是 | 来源服务。 |
| `sourceType` | string | 是 | 来源类型。 |
| `severity` | string | 是 | `AlertSeverity`。 |
| `labels` | object | 是 | 标签，最多 20 个键值。 |
| `conditionType` | string | 是 | `AlertConditionType`。 |
| `conditionSummary` | object | 是 | 条件摘要，不保存原始查询密钥。 |
| `evaluationWindowSeconds` | integer | 是 | 评估窗口，60 到 86400。 |
| `forDurationSeconds` | integer | 是 | 持续触发时间，0 到 86400。 |
| `dedupeKeyTemplate` | string | 是 | 去重键模板，最多 200 位。第一版支持 `{{sourceService}}`、`{{sourceType}}`、`{{severity}}`、`{{sourceRef}}`、`{{nodeId}}`、`{{groupKey}}`、`{{labels.<key>}}` 和 `{{snapshot.<key>}}` 占位符。无法解析的占位符按空字符串处理，生成后的空白字符归一为 `_`，连续空值不得导致指纹为空。 |
| `routeId` | string 或 null | 是 | 默认通知路由。 |
| `runbookUrl` | string 或 null | 是 | 处理说明链接，只允许 http、https 或站内路径。 |
| `status` | string | 是 | `AlertRuleStatus`。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### AlertEvaluation

字段为 `evaluationId`、`ruleId`、`status`、`matchedSourceId`、`createdAlertId`、`dedupeHit`、`suppressed`、`dependencyStatus`、`resultSummary`、`failureReason`、`evaluatedBy` 和 `evaluatedAt`。手动评估只读取来源快照，不主动采集真实指标。命中时必须按 `dedupeKeyTemplate` 生成稳定指纹；未静默且规则绑定启用路由、路由匹配成功时，必须生成 `SENT` 投递摘要；未静默但路由缺失、禁用或不匹配时，不得伪造投递成功，告警的 `notificationSummary.status` 应返回 `PENDING` 并带安全原因摘要。

### AlertInstance

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `alertId` | string | 是 | 告警实例 ID。 |
| `ruleId` | string | 是 | 来源规则 ID。 |
| `sourceService` | string | 是 | 来源服务。 |
| `sourceRef` | object | 是 | 来源对象安全摘要。 |
| `severity` | string | 是 | 告警级别。 |
| `status` | string | 是 | `AlertInstanceStatus`。 |
| `labels` | object | 是 | 标签快照。 |
| `fingerprint` | string | 是 | 去重指纹摘要。 |
| `groupKey` | string | 是 | 分组键。 |
| `firstFiredAt` | string | 是 | 首次触发时间。 |
| `lastFiredAt` | string | 是 | 最近触发时间。 |
| `acknowledgedBy` | string 或 null | 是 | 确认人。 |
| `acknowledgedAt` | string 或 null | 是 | 确认时间。 |
| `closedBy` | string 或 null | 是 | 关闭人。 |
| `closedAt` | string 或 null | 是 | 关闭时间。 |
| `summary` | string | 是 | 告警摘要，最多 300 位。 |
| `runbookUrl` | string 或 null | 是 | 处理说明链接。 |
| `notificationSummary` | object | 是 | 投递摘要。 |
| `suppressionSummary` | object | 是 | 静默或抑制摘要。 |

### AlertSilence

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `silenceId` | string | 是 | 静默 ID。 |
| `matchers` | object | 是 | 标签、来源、级别匹配器。第一版支持 `sourceService`、`severity`、`groupKey` 和 `labels` 精确匹配。 |
| `startsAt` | string | 是 | 开始时间。 |
| `endsAt` | string | 是 | 结束时间，必须晚于开始时间。 |
| `reason` | string | 是 | 静默原因，1 到 200 位。 |
| `status` | string | 是 | `AlertSilenceStatus`。 |
| `createdBy` | string | 是 | 创建者。 |
| `cancelledBy` | string 或 null | 是 | 取消者。 |
| `createdAt` | string | 是 | 创建时间。 |
| `cancelledAt` | string 或 null | 是 | 取消时间。 |

### AlertRoute

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `routeId` | string | 是 | 路由 ID。 |
| `displayName` | string | 是 | 路由名称。 |
| `matchers` | object | 是 | 匹配器。第一版支持 `sourceService`、`severity`、`groupKey` 和 `labels` 精确匹配，路由必须启用且全部 matcher 命中后才允许生成正常投递摘要。 |
| `groupBy` | string[] | 是 | 分组字段。 |
| `groupWaitSeconds` | integer | 是 | 首次分组等待，0 到 3600。 |
| `groupIntervalSeconds` | integer | 是 | 分组重复间隔，60 到 86400。 |
| `repeatIntervalSeconds` | integer | 是 | 重复提醒间隔，300 到 604800。 |
| `notificationTemplateRef` | object | 是 | notification 模板引用摘要。 |
| `receiverSummary` | object | 是 | 接收方摘要，不返回外部渠道 secret。 |
| `status` | string | 是 | `AlertRouteStatus`。 |
| `createdBy` | string | 是 | 创建者。 |
| `updatedBy` | string | 是 | 最近修改者。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### AlertDelivery

字段为 `deliveryId`、`alertId`、`routeId`、`notificationRef`、`status`、`attempts`、`lastAttemptAt`、`failureCode`、`failureSummary`、`nextRetryAt` 和 `createdAt`。不得保存真实外部 webhook secret、邮件密码、短信 token 或完整通知正文。

### AlertingAuditLog

审计字段继承公共契约，允许补充 `ruleId`、`alertId`、`silenceId`、`routeId`、`deliveryId`、`dependencyStatus`、`stateFrom`、`stateTo`、`idempotencyKey` 和 `notificationHint`。写接口传入的 `reason` 必须进入审计响应；`paramsSummary` 只能返回脱敏后的字段名、幂等键是否存在和安全摘要，不能回显完整请求体。审计列表不得提供删除接口。审计响应不得返回 token、密钥、外部渠道 secret、完整请求头、完整通知正文、内部路径、完整来源 payload 或异常堆栈。

### AlertingOpsSummary

字段至少包含 `service`、`port`、`storageMode`、`authMode`、`sourceAdapterMode`、`notificationAdapterMode`、`testControlsEnabled`、`sourcesTotal`、`rulesTotal`、`enabledRulesTotal`、`alertsTotal`、`firingAlertsTotal`、`acknowledgedAlertsTotal`、`silencesTotal`、`activeSilencesTotal`、`routesTotal`、`deliveriesTotal`、`failedDeliveriesTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastAlertAt`、`lastDeliveryFailureAt`、`degraded`、`degradeReasons` 和 `productionGaps`。

## 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `46900` | 502 | notification 不可用。 |
| `46901` | 504 | notification 调用超时。 |
| `46902` | 502 | notification 响应不兼容。 |
| `46910` | 502 | 来源服务不可用。 |
| `46911` | 504 | 来源服务调用超时。 |
| `46912` | 502 | 来源服务快照不兼容。 |
| `46920` | 502 | auth 认证上下文不可用。 |
| `46921` | 504 | auth 认证上下文调用超时。 |
| `46922` | 502 | auth 认证上下文字段不兼容。 |
| `49900` | 404 | 告警源不存在。 |
| `49901` | 404 | 告警规则不存在。 |
| `49902` | 404 | 告警实例不存在。 |
| `49903` | 404 | 静默不存在。 |
| `49904` | 404 | 通知路由不存在。 |
| `49905` | 404 | 投递摘要不存在。 |
| `49910` | 409 | 状态不允许当前操作。 |
| `49911` | 400 | 告警规则条件不合法。 |
| `49912` | 409 | 幂等键请求指纹冲突。 |
| `49913` | 400 | 静默时间范围不合法。 |
| `49914` | 400 | 标签匹配器不合法。 |
| `49915` | 409 | 告警已被静默或抑制，不能重复投递。 |
| `55500` | 500 | alerting 内部错误。 |
| `55501` | 500 | alerting 审计写入失败。 |
| `55502` | 500 | alerting 状态写入失败。 |
| `55503` | 500 | alerting 投递状态写入失败。 |

字段校验、未登录、令牌格式错误、角色权限不足、能力点不足、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/alerting/health` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/alerting/ops/summary` | 是 | `NODE_READ` 或 `HIGH_RISK_APPROVE` | LOW |
| 告警源列表 | GET | `/api/v1/alerting/sources` | 是 | `NODE_READ` | LOW |
| 告警源详情 | GET | `/api/v1/alerting/sources/{sourceId}` | 是 | `NODE_READ` | LOW |
| 规则列表 | GET | `/api/v1/alerting/rules` | 是 | `NODE_READ` | LOW |
| 规则详情 | GET | `/api/v1/alerting/rules/{ruleId}` | 是 | `NODE_READ` | LOW |
| 创建规则 | POST | `/api/v1/alerting/rules` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 更新规则 | PATCH | `/api/v1/alerting/rules/{ruleId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 启用规则 | PATCH | `/api/v1/alerting/rules/{ruleId}/enable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 禁用规则 | PATCH | `/api/v1/alerting/rules/{ruleId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 手动评估规则 | POST | `/api/v1/alerting/rules/{ruleId}/evaluate` | 是 | `ADMIN` 或 `OWNER`，`NODE_READ` | MEDIUM |
| 告警实例列表 | GET | `/api/v1/alerting/alerts` | 是 | `NODE_READ` | LOW |
| 告警实例详情 | GET | `/api/v1/alerting/alerts/{alertId}` | 是 | `NODE_READ` | LOW |
| 确认告警 | PATCH | `/api/v1/alerting/alerts/{alertId}/acknowledge` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 关闭告警 | PATCH | `/api/v1/alerting/alerts/{alertId}/close` | 是 | `ADMIN` 或 `OWNER`，严重告警要求 `HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 静默列表 | GET | `/api/v1/alerting/silences` | 是 | `NODE_READ` | LOW |
| 创建静默 | POST | `/api/v1/alerting/silences` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 取消静默 | PATCH | `/api/v1/alerting/silences/{silenceId}/cancel` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 通知路由列表 | GET | `/api/v1/alerting/routes` | 是 | `NODE_READ` | LOW |
| 创建通知路由 | POST | `/api/v1/alerting/routes` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 更新通知路由 | PATCH | `/api/v1/alerting/routes/{routeId}` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 测试通知路由 | POST | `/api/v1/alerting/routes/{routeId}/test` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 投递记录列表 | GET | `/api/v1/alerting/deliveries` | 是 | `NODE_READ` | LOW |
| 审计列表 | GET | `/api/v1/alerting/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 健康、自检和来源接口

`GET /api/v1/alerting/health` 成功返回 `service=alerting`、`status`、`version` 和 `requestId`。进程存活但依赖不可用时仍可返回 HTTP `200`，并用 `status=DEGRADED` 标记。该接口不得泄露来源详情、规则数量、告警数量、通知路由、token 或依赖错误细节。

`GET /api/v1/alerting/ops/summary` 成功返回 `AlertingOpsSummary`。第一版必须返回 `port=8120`、`storageMode=IN_MEMORY`、`sourceAdapterMode=TEST_STUB`、`notificationAdapterMode=TEST_STUB` 和生产化缺口。读取失败返回 `55500`，不得伪造健康。

`GET /api/v1/alerting/sources` 支持 `page`、`pageSize`、`keyword`、`sourceService`、`sourceType`、`healthStatus`、`enabled` 和 `sort`。`sort` 允许 `lastEventAt_desc`、`lastSnapshotAt_desc`、`displayName_asc`。成功响应分页 `items` 为 `AlertSource[]`。

`GET /api/v1/alerting/sources/{sourceId}` 返回来源详情、最近来源快照摘要和降级原因。来源不存在返回 `49900`。响应不得返回来源服务私有数据、内部路径、token 或完整 payload。

## 规则接口

`GET /api/v1/alerting/rules` 支持 `page`、`pageSize`、`keyword`、`sourceService`、`sourceType`、`severity`、`status`、`routeId`、`labelKey`、`labelValue` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`、`severity_desc`。成功响应分页 `items` 为 `AlertRule[]`。

`GET /api/v1/alerting/rules/{ruleId}` 返回规则详情、最近评估摘要、最近告警实例摘要和路由摘要。规则不存在返回 `49901`。

`POST /api/v1/alerting/rules` 请求字段为 `displayName`、`sourceService`、`sourceType`、`severity`、`labels`、`conditionType`、`conditionSummary`、`evaluationWindowSeconds`、`forDurationSeconds`、`dedupeKeyTemplate`、`routeId`、`runbookUrl`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `AlertRule`。规则名称在同一来源服务内不能重复。条件摘要必须匹配条件类型，非法返回 `49911`。路由不存在返回 `49904`。

`PATCH /api/v1/alerting/rules/{ruleId}` 可修改 `displayName`、`sourceService`、`sourceType`、`severity`、`labels`、`conditionType`、`conditionSummary`、`evaluationWindowSeconds`、`forDurationSeconds`、`dedupeKeyTemplate`、`routeId`、`runbookUrl`、`reason` 和 `idempotencyKey`。`ARCHIVED` 规则不可修改。

`PATCH /api/v1/alerting/rules/{ruleId}/enable` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT` 和 `DISABLED` 可启用为 `ENABLED`。重复启用保持幂等。`ARCHIVED` 返回 `49910`。

`PATCH /api/v1/alerting/rules/{ruleId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 可禁用为 `DISABLED`。重复禁用保持幂等。禁用规则不删除已有告警实例。

`POST /api/v1/alerting/rules/{ruleId}/evaluate` 请求字段为 `sourceSnapshot`、`dryRun`、`reason` 和 `idempotencyKey`。`sourceSnapshot` 只能是测试或服务端适配器提供的安全摘要，不能包含 token、内部路径、完整日志或任何嵌套可信字段。成功返回 `AlertEvaluation`。规则未启用返回 `49910`。来源不可用返回 `46910`，来源超时返回 `46911`，来源 schema 不兼容返回 `46912`。命中时按 `dedupeKeyTemplate` 生成指纹，重复指纹更新已有 `AlertInstance.lastFiredAt`，不新建告警实例。未静默告警必须先按规则 `routeId` 找到启用路由，再按 route matcher 匹配来源、级别、分组和标签；匹配成功生成 `SENT` 投递摘要，匹配失败保留 `PENDING` 摘要并记录不投递原因。

## 告警实例接口

`GET /api/v1/alerting/alerts` 支持 `page`、`pageSize`、`ruleId`、`sourceService`、`severity`、`status`、`groupKey`、`labelKey`、`labelValue`、`keyword`、`from`、`to` 和 `sort`。`sort` 允许 `lastFiredAt_desc`、`firstFiredAt_desc`、`severity_desc`、`status_asc`。时间范围按 `firstFiredAt` 过滤，反向范围返回 `40001`。

`GET /api/v1/alerting/alerts/{alertId}` 返回告警详情、最近投递摘要和关联静默摘要。告警不存在返回 `49902`。

`PATCH /api/v1/alerting/alerts/{alertId}/acknowledge` 请求字段为 `reason` 和 `idempotencyKey`。`FIRING` 和 `SUPPRESSED` 可确认为 `ACKNOWLEDGED`。重复确认已确认告警保持幂等。`CLOSED` 返回 `49910`。确认不会关闭告警，也不会取消后续重复提醒，只改变处理状态。

`PATCH /api/v1/alerting/alerts/{alertId}/close` 请求字段为 `resolutionSummary`、`reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `CLOSE_ALERT`。`FIRING`、`ACKNOWLEDGED`、`SUPPRESSED` 和 `RESOLVED` 可关闭为 `CLOSED`。`BLOCKER` 严重级别关闭要求 `HIGH_RISK_APPROVE` 或 `OWNER`。重复关闭返回成功且保持原关闭时间。

## 静默接口

`GET /api/v1/alerting/silences` 支持 `page`、`pageSize`、`status`、`sourceService`、`severity`、`labelKey`、`labelValue`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`startsAt_asc`、`endsAt_asc`。读取和匹配静默前必须懒更新过期状态，`endsAt` 早于当前时间且仍为 `ACTIVE` 的静默必须转为 `EXPIRED`。

`POST /api/v1/alerting/silences` 请求字段为 `matchers`、`startsAt`、`endsAt`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `AlertSilence`。`endsAt` 必须晚于 `startsAt`，否则返回 `49913`。匹配器必须至少包含来源、级别、标签或 groupKey 中的一类，非法返回 `49914`。静默只暂停通知，不删除告警实例，不停止规则评估。

`PATCH /api/v1/alerting/silences/{silenceId}/cancel` 请求字段为 `reason` 和 `idempotencyKey`。只有 `ACTIVE` 可取消。`EXPIRED` 和 `CANCELLED` 返回 `49910` 或按已取消幂等返回同一结果，同一实现版本内必须固定并写入测试。

## 通知路由和投递接口

`GET /api/v1/alerting/routes` 支持 `page`、`pageSize`、`keyword`、`status`、`severity`、`sourceService` 和 `sort`。`sort` 允许 `updatedAt_desc`、`displayName_asc`。

`POST /api/v1/alerting/routes` 请求字段为 `displayName`、`matchers`、`groupBy`、`groupWaitSeconds`、`groupIntervalSeconds`、`repeatIntervalSeconds`、`notificationTemplateRef`、`receiverSummary`、`enabled`、`reason` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `AlertRoute`。接收方摘要只能保存站内通知或外部渠道的脱敏描述，不得保存 webhook secret、邮件密码或短信 token。

`PATCH /api/v1/alerting/routes/{routeId}` 可修改创建接口中的字段，`reason` 和 `idempotencyKey` 必填。路由不存在返回 `49904`。审计失败时不得改变路由。

`POST /api/v1/alerting/routes/{routeId}/test` 请求字段为 `sampleAlert`、`reason` 和 `idempotencyKey`。成功返回 `AlertDelivery`。第一版只调用 notification 测试适配器或生成投递摘要，不发送真实外部渠道。notification 不可用返回 `46900` 或创建 `FAILED` 投递摘要，同一实现版本内必须固定并写入测试。

`GET /api/v1/alerting/deliveries` 支持 `page`、`pageSize`、`alertId`、`routeId`、`status`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`lastAttemptAt_desc`、`status_asc`。成功响应分页 `items` 为 `AlertDelivery[]`。

## 审计接口

`GET /api/v1/alerting/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`ruleId`、`alertId`、`silenceId`、`routeId`、`action`、`result`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`、`riskLevel_desc`。只有 `ADMIN` 和 `OWNER` 可访问。审计列表只读，不提供删除、修改或恢复接口。

审计中的 `requestId` 必须来自当前 HTTP 请求，不能使用固定占位值。后台写操作必须记录调用者、`reason`、操作前状态、操作后状态、请求编号、结果和失败原因。审计写入失败时，规则创建修改启停、规则评估生成告警、确认关闭、静默创建取消、路由创建修改和路由测试不得假装成功，必须返回 `55501` 并保持业务状态不变。

## 状态、幂等和并发

规则状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

告警实例状态流转为 `FIRING` 到 `ACKNOWLEDGED`、`SUPPRESSED`、`RESOLVED` 或 `CLOSED`；`ACKNOWLEDGED` 到 `RESOLVED` 或 `CLOSED`；`SUPPRESSED` 在静默结束后可回到 `FIRING` 或 `RESOLVED`；`CLOSED` 为人工终态。关闭不会修改来源服务状态。

静默状态流转为 `ACTIVE` 到 `EXPIRED` 或 `CANCELLED`。过期和取消都不删除历史记录。路由状态为 `ENABLED` 或 `DISABLED`，禁用路由不删除历史投递。

写接口使用幂等键时，必须使用字段名排序后的稳定 JSON 语义计算请求体指纹，嵌套对象按字段名递归排序，数组保留顺序，不能依赖 Java `Map.toString()` 或浏览器字段顺序。幂等键的查找、冲突判断、业务状态推进、审计写入、响应快照保存和幂等记录写入必须处于同一临界区内；并发相同幂等键同请求体只能执行一次业务动作，并返回同一响应快照；并发相同幂等键不同请求体必须返回 `49912`。后续数据库实现必须使用事务、唯一约束、条件更新或等效机制，不能降低并发口径。

## 安全、降级和脱敏

任何请求体都不得包含访问 token、节点密钥、Cloudreve 管理 token、分享密码、外部 webhook secret、SMTP 密码、短信 token、完整 Authorization 请求头、完整通知正文、内部绝对路径、完整来源 payload、异常堆栈、数据库连接串、`.env`、`authorized_keys`、`id_rsa`、服务器密码或 shell 命令。任何响应也不得包含这些字段或值。检查必须递归覆盖嵌套对象和数组。

外部依赖不可用时，读取类接口可以返回已有快照并标记 `degraded=true` 和 `degradeReasons`。写入类接口不得假装成功。通知投递失败不得关闭告警，也不得把告警主状态改成已处理。来源服务不可用时，规则评估必须返回明确依赖错误或降级评估摘要。

第一版不得提供真实删除规则、告警、静默、路由或投递记录的接口。确需清理历史记录时，必须在后续独立契约中增加归档接口，并重新完成文档、测试红灯、实现和回归闭环。

## 验收口径

`alerting` API 文档必须按 `docs/contracts-alerting.md` 独立存在，并由 `.local-docs/tests-alerting.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求、敏感字段脱敏、测试控制头默认关闭和模块验收口径。

`alerting` 完成时必须满足以下条件：端口固定为 `8120`；健康检查不泄露敏感信息；除健康检查外全部接口要求后台认证；告警源、规则、评估、实例、确认关闭、静默、通知路由、投递摘要、审计、幂等、状态流转、去重分组、通知失败降级、审计失败回滚、敏感字段脱敏、测试控制头默认关闭和自检摘要都有自动化验证；不修改前序服务稳定接口；不直接调用 `node-daemon`；不执行真实外部通知发送；不把告警规则塞进 `notification`、`admin`、`ops-control`、`server-status`、`cloudreve-sync` 或 `backup-recovery`；自动化测试必须先红灯；实现后 `alerting` 全量测试通过；前序 19 个稳定服务回归通过；边界扫描无违规命中；测试过程记录完整。
