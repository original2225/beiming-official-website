# 北冥官网 cross-platform-notification API 契约

版本：0.4

## 文档定位

本文档是 `cross-platform-notification` 模块的正式 API 契约。第六期运行合并后，该模块由 `ops-core-service:8133` 承载，但仍保持独立模块契约身份。`cross-platform-notification` 负责跨平台外部通知控制面，包括外部渠道 provider 摘要、渠道能力、模板映射、路由策略、投递请求、投递尝试、receiver 摘要、重试摘要、失败降级、幂等记录、审计日志和自检摘要。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、运维能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `cross-platform-notification` 的职责边界、数据归属、前序模块兼容、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考 Slack Incoming Webhooks、Discord Webhooks、Telegram Bot API、Twilio Messaging、Firebase Cloud Messaging、OneSignal、Novu 和 Grafana Alerting 的公开设计。Slack Incoming Webhooks 明确 webhook URL 本身包含 secret，适合约束本服务不回显完整 webhook。Discord Webhooks 说明 webhook token、内容长度、allowed mentions 和服务端确认差异，适合本服务建立渠道能力和内容降级模型。Telegram Bot API 说明 bot token、HTTPS 请求、webhook secret token 和更新重试，适合本服务区分发送 token、回调签名和失败摘要。Twilio Messaging 的状态回调说明外部投递会经历 queued、sent、delivered、failed 等状态，适合本服务保留投递尝试和后续回调适配位置。FCM 的跨平台消息、topic、device token 和平台差异化配置，适合本服务抽象 `PUSH` 渠道能力和 receiver 摘要。OneSignal 的 Push、In-App、Email、SMS 和 Live Activities 多渠道模型，适合本服务拆分渠道、订阅和模板。Novu 的 workflow、channel steps、delay、digest 和 subscriber preference 说明通知编排应独立于业务主数据。Grafana Alerting 的 contact point、notification policy、grouping、silence 和 template 说明路由策略、分组抑制和接收方配置要和告警规则分离。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Slack Incoming Webhooks](https://docs.slack.dev/messaging/sending-messages-using-incoming-webhooks/) | webhook URL 是 secret，响应和审计不得回显完整 URL 或 token。 |
| [Discord Webhook Resource](https://discord.com/developers/docs/resources/webhook) | webhook token、内容长度、allowed mentions、wait 确认和执行响应差异需要抽象为渠道能力。 |
| [Telegram Bot API](https://core.telegram.org/bots/api) | bot token、HTTPS Bot API、webhook secret token 和更新重试都必须归入凭据和回调边界。 |
| [Twilio Messaging Webhooks](https://www.twilio.com/docs/usage/webhooks/messaging-webhooks) | 外部消息投递状态应支持状态回调、失败码和生命周期摘要。 |
| [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging) | 跨平台推送需要区分 topic、device token、平台配置和 payload 限制。 |
| [OneSignal Channel Setup](https://documentation.onesignal.com/docs/channel-setup) | Push、In-App、Email、SMS 等渠道有不同配置、receiver 和发送约束。 |
| [Novu Workflows](https://docs.novu.co/platform/concepts/workflows) | workflow、channel steps、delay、digest 和 preference 需要和业务事件解耦。 |
| [Grafana Alerting Notifications](https://grafana.com/docs/grafana/latest/alerting/fundamentals/notifications/) | contact point、notification policy、grouping、silence 和 template 应独立建模。 |

本文档只吸收这些生态的设计思路，不接入它们的 SDK，不保存真实外部平台 token，不发送真实外部消息，不开放真实 webhook 回调入口。

## 职责边界

`cross-platform-notification` 负责外部通知渠道控制面。它保存外部渠道 provider 的脱敏配置摘要、渠道能力、模板映射、路由策略、投递请求摘要、投递尝试记录、receiver 摘要、重试摘要、依赖摘要、幂等记录、审计日志和自检摘要。

`cross-platform-notification` 不负责注册、登录、会话、用户角色、站内通知主数据、站内未读数、站内已读归档、告警规则、告警实例、插件事件、社区工单、活动报名、日历事件、白名单审核、考勤积分、运维任务、节点执行、Minecraft 命令、真实 SMTP、真实短信网关、真实 QQ/Oopz/Discord/Slack/Telegram/企业微信机器人托管、真实推送 token 管理或外部平台账号绑定主数据。

第一版固定为安全控制面和投递模拟。它可以创建 `SIMULATED_SENT`、`SIMULATED_FAILED`、`BLOCKED`、`RETRY_SCHEDULED`、`CANCELED` 和 `EXPIRED` 状态，不能返回真实 `SENT`，不能对外发邮件、短信、聊天消息、机器人消息、Webhook、移动推送或游戏内消息。后续开启真实发送必须重新补充正式契约、测试文档、红灯测试、生产凭据托管、回调签名、速率限制、死信队列、隐私脱敏和回归记录。

## 数据归属

`cross-platform-notification` 拥有以下主数据：ExternalChannelProvider、ExternalChannelCapability、ExternalTemplateMapping、ExternalRoutePolicy、ExternalDeliveryRequest、ExternalDeliveryAttempt、ExternalReceiverSummary、ExternalNotificationAuditLog、CrossPlatformNotificationOpsSummary 和幂等记录。

`cross-platform-notification` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和状态快照；可以保存来自 `notification` 的站内通知引用、模板引用和投递偏好安全摘要；可以保存来自 `alerting` 的告警投递请求摘要；可以保存来自 `plugin-integration` 的插件事件和插件健康异常摘要；可以保存来自 `ops-control` 的高风险审批、节点异常和任务失败摘要；可以保存来自 `community`、`activity`、`calendar`、`changelog`、`whitelist`、`attendance`、`resource` 和 `server-status` 的业务来源摘要。

所有跨模块字段只能是安全快照，不得成为来源模块主数据，不得用于绕过来源模块权限，不得反向修改来源模块状态。`cross-platform-notification` 不能直接读取其他服务数据库，不能导入前序模块 Java package，不能复用前序模块内存 store，不能修改 `notification` 未读数，不能关闭 `alerting` 告警，不能重放 `plugin-integration` 事件，不能创建 `ops-control` 任务。

## 基础路径、端口和认证

所有接口默认使用 `/api/v1/cross-platform-notification` 前缀。第六期运行合并后当前运行入口为 `ops-core-service:8133`，自检摘要必须返回 `port=8133` 和 `legacyPort=8123`。历史独立端口 `8123` 只作为追溯字段，不再作为当前网关上游、当前 Maven 测试入口或独立部署入口。

健康检查 `GET /api/v1/cross-platform-notification/health` 不要求认证，只能返回 `service`、`version`、`status` 和 `requestId`，不得返回 provider 数量、receiver、endpoint、外部平台错误详情、依赖明细或任何敏感字段。

后台接口统一使用 `/api/v1/cross-platform-notification/admin` 前缀，全部要求 `Authorization: Bearer <token>`。后台读取接口要求基础角色为 `HELPER`、`ADMIN` 或 `OWNER`，并具备 `NODE_READ` 或等效后台读取能力。后台写接口要求 `ADMIN` 或 `OWNER`，并具备 `NODE_WRITE`。涉及外部 endpoint、receiver、provider 启用、路由启用、测试路由、投递请求、批量重试、取消高风险投递或真实发送开关的接口要求 `HIGH_RISK_APPROVE` 或 `OWNER`，并要求固定 `confirmText`。`ADMIN` 具备 `NODE_WRITE` 但缺少 `HIGH_RISK_APPROVE` 时，高风险写接口必须返回 `42002`；`OWNER` 可绕过该能力点但仍必须满足确认文本、状态流转和审计规则。

第一版支持公共风险等级 `LOW`、`MEDIUM`、`HIGH` 和 `CRITICAL`。`HIGH` 写操作要求 `HIGH_RISK_APPROVE` 或 `OWNER`。`CRITICAL` 只允许 `OWNER` 执行；非 `OWNER` 账号即使具备 `HIGH_RISK_APPROVE`，在创建或更新 provider 允许风险等级、创建或更新路由、创建投递时传入 `CRITICAL` 也必须返回 `42004`。后续如果接入独立审批记录，必须先补充本契约、测试文档、红灯测试和回归记录。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`beforeState`、`afterState`、`auditResult`、`createdBy`、`updatedBy`、`enabledBy`、`disabledBy`、`archivedBy`、`rawPayload`、`rawToken`、`webhookSecret`、`discordToken`、`qqToken`、`oopzToken`、`smtpPassword`、`smsToken`、`botToken`、`rconPassword`、`credential`、`secretKey`、`Authorization`、`requestHeaders`、`internalUrl`、`internalPath`、`resolvedPath`、`fullException`、`databaseUrl`、`deliveryStatus`、`attemptStatus`、`externalMessageId` 和 `providerRawResponse` 等服务端可信字段。可信字段必须递归检查，嵌套在 `payloadSummary`、`receiverSummary`、`endpointSummary`、`metadata`、`matchers`、`requestSummary`、`responseSummary` 或任意数组对象中也必须拒绝。出现可信字段时返回 `40001`。

## 本地测试控制头

`cross-platform-notification` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Notification-Mode`、`X-Test-Alerting-Mode`、`X-Test-Plugin-Integration-Mode`、`X-Test-Source-Mode`、`X-Test-Provider-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store`、`X-Test-Fail-Delivery` 和 `X-Test-Now` 模拟认证失败、依赖不可用、依赖超时、schema 不兼容、provider 降级、模拟投递失败、审计失败、状态写入失败、投递记录写入失败和时间边界。第六期合并后，该开关由 `ops-core.test-controls.enabled` 统一控制，并通过 `cross-platform-notification.test-controls.enabled=${ops-core.test-controls.enabled:false}` 继承。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发认证失败、依赖失败、provider 失败、审计失败、存储失败、投递失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 纳入生产化硬化项。

## 前序模块兼容契约

`auth` 是后台接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。auth 不可用返回 `47150`，auth 超时返回 `47151`，auth 字段或枚举不兼容返回 `47152`。

`notification` 是站内通知和站内模板主数据来源。`cross-platform-notification` 可以读取站内通知引用、模板引用、模板变量白名单和通知偏好安全摘要，不能修改 notification 的站内通知、未读数、已读归档或模板主数据。notification 不可用返回 `47120`，超时返回 `47121`，schema 不兼容返回 `47122`。notification 不可用时，读取类接口可以返回已有脱敏快照并标记 `degraded=true`；创建关联站内通知的投递请求不得伪造关联成功。

`alerting` 是告警来源方。`cross-platform-notification` 可以接收或模拟来自 alerting 的外部投递请求摘要，不能修改告警规则、告警实例、静默、确认或关闭状态。alerting 不可用返回 `47130`，超时返回 `47131`，schema 不兼容返回 `47132`。外部投递失败不能自动关闭告警，也不能把告警投递摘要伪造成成功。

来自 `alerting` 的内部适配请求必须使用 `sourceModule=alerting`。请求字段只允许安全摘要，至少包括 `sourceId`、`eventType=alert.firing`、`riskLevel`、`routeId` 或 `providerId` 与 `templateMappingId`、`receiverSummary`、`payloadSummary`、`expiresAt`、`reason` 和 `idempotencyKey`。`payloadSummary` 只能包含模板允许变量的摘要值，不能包含完整日志、完整告警正文、完整请求头、内部路径、token、外部渠道凭据或异常堆栈。`riskLevel` 映射规则为 `INFO -> LOW`、`WARNING -> MEDIUM`、`CRITICAL -> HIGH`、`BLOCKER -> CRITICAL`。相同 `alertId + routeId + fingerprint + idempotencyKey` 只能创建一条 delivery 和一条 attempt；同一幂等键不同请求体返回 `49962`。审计必须记录 `sourceModule=alerting`、`sourceId`、`routeId`、`deliveryId`、`attemptId`、风险等级和脱敏参数摘要。

`plugin-integration` 是插件事件来源方。`cross-platform-notification` 可以保存插件事件通知摘要和模拟投递结果，不能修改插件 provider、事件、路由规则、同步任务或对象映射。plugin-integration 不可用返回 `47140`，超时返回 `47141`，schema 不兼容返回 `47142`。

其他业务来源模块包括 `ops-control`、`external-node-executor`、`community`、`activity`、`calendar`、`changelog`、`whitelist`、`attendance`、`resource` 和 `server-status`。本服务只能保存来源模块传入或正式 API 返回的安全摘要。来源模块不可用返回 `47160`，超时返回 `47161`，schema 不兼容返回 `47162`。`external-node-executor` 只能作为来源摘要出现，本服务不得直连节点，不得执行命令。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `ExternalChannel` | `EMAIL`、`SMS`、`QQ`、`OOPZ`、`DISCORD`、`SLACK`、`TELEGRAM`、`WECHAT_WORK`、`GAME`、`PUSH`、`WEBHOOK`、`CUSTOM` | 外部渠道类型。第一版只模拟投递。 |
| `ExternalProviderStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`DEGRADED`、`ARCHIVED` | provider 控制面状态。 |
| `ExternalProviderHealthStatus` | `HEALTHY`、`DEGRADED`、`UNAVAILABLE`、`UNKNOWN` | provider 健康摘要。 |
| `ExternalTemplateMappingStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`ARCHIVED` | 模板映射状态。 |
| `ExternalRenderMode` | `PLAIN_TEXT`、`MARKDOWN`、`RICH_BLOCK`、`PLATFORM_TEMPLATE` | 渲染模式。第一版只保存摘要和模拟渲染。 |
| `ExternalRoutePolicyStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`ARCHIVED` | 路由策略状态。 |
| `ExternalDeliveryStatus` | `QUEUED`、`SIMULATED_SENT`、`SIMULATED_FAILED`、`RETRY_SCHEDULED`、`CANCELED`、`EXPIRED`、`BLOCKED` | 投递请求状态。第一版不得出现真实 `SENT`。 |
| `ExternalAttemptStatus` | `SIMULATED_SUCCESS`、`SIMULATED_FAILURE`、`BLOCKED_BY_POLICY`、`RATE_LIMITED`、`DEPENDENCY_FAILED` | 投递尝试状态。 |
| `ExternalReceiverType` | `USER`、`ROLE`、`GROUP`、`CHANNEL`、`TOPIC`、`WEBHOOK_ENDPOINT`、`DEVICE_TOKEN`、`EMAIL_ADDRESS`、`PHONE_NUMBER`、`GAME_TARGET`、`CUSTOM` | receiver 摘要类型。 |
| `ExternalDependencyStatus` | `AVAILABLE`、`UNAVAILABLE`、`TIMEOUT`、`BAD_SCHEMA`、`STALE`、`SKIPPED` | 依赖摘要状态。 |
| `ExternalNotificationAuditResult` | `SUCCESS`、`FAILED` | 审计结果。 |

`sourceModule` 使用模块英文名，例如 `notification`、`alerting`、`plugin-integration`、`ops-control`、`community`、`activity`、`calendar`、`changelog`、`whitelist`、`attendance`、`resource`、`server-status` 和 `custom`。第一版不允许浏览器伪装为 `auth`、`external-node-executor` 或内部系统用户。浏览器传入未列入本契约的来源模块、`auth`、`external-node-executor` 或以 `internal`、`system` 开头的来源模块时必须返回 `40001`，不得创建投递、路由、模板映射或审计记录。`sourceModule=alerting` 可以由后台接口或同进程受控适配器创建，但必须继续执行 provider、模板、路由、receiver、payload 白名单、幂等和审计校验。

## 通用对象

### ExternalChannelProvider

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `providerId` | string | 是 | provider ID。 |
| `channel` | string | 是 | `ExternalChannel`。 |
| `displayName` | string | 是 | 展示名称，2 到 80 位。 |
| `status` | string | 是 | `ExternalProviderStatus`。 |
| `endpointSummary` | object 或 null | 是 | endpoint 脱敏摘要，只保存域名、路径类型和协议摘要，不返回完整 URL。 |
| `credentialRefSummary` | object 或 null | 是 | 凭据引用摘要，只能是凭据别名、托管方式和轮换时间，不返回真实凭据。 |
| `receiverPolicy` | object | 是 | receiver 允许类型、最大数量、隐私脱敏和来源限制。 |
| `allowedSourceModules` | string[] | 是 | 允许来源模块，最多 30 个。 |
| `allowedRiskLevels` | string[] | 是 | 允许风险等级，取公共风险等级。 |
| `rateLimitSummary` | object | 是 | 速率限制摘要，包含窗口、容量和当前降级原因，不返回平台 secret。 |
| `healthStatus` | string | 是 | `ExternalProviderHealthStatus`。 |
| `lastTestAt` | string 或 null | 是 | 最近测试时间。 |
| `lastDeliveryAt` | string 或 null | 是 | 最近模拟投递时间。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReasons` | string[] | 是 | 降级原因，必须脱敏。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ExternalChannelCapability

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `capabilityId` | string | 是 | 能力 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `channel` | string | 是 | `ExternalChannel`。 |
| `supportsMarkdown` | boolean | 是 | 是否支持 Markdown 或等价文本格式。 |
| `supportsRichBlocks` | boolean | 是 | 是否支持 rich block、embed、button 或卡片摘要。 |
| `supportsImages` | boolean | 是 | 是否支持图片摘要。第一版不上传真实图片。 |
| `supportsThreads` | boolean | 是 | 是否支持 thread/topic 回复。 |
| `supportsMentions` | boolean | 是 | 是否支持 @ 提及。第一版必须默认禁用危险提及。 |
| `supportsDeliveryCallback` | boolean | 是 | 是否支持后续状态回调。第一版只保存能力摘要，不开放真实回调。 |
| `maxTitleLength` | integer | 是 | 标题最大长度。 |
| `maxBodyLength` | integer | 是 | 正文最大长度。 |
| `maxReceiversPerRequest` | integer | 是 | 单次 receiver 最大数量。 |
| `rateLimitSummary` | object | 是 | 速率限制摘要。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ExternalTemplateMapping

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `mappingId` | string | 是 | 模板映射 ID。 |
| `sourceModule` | string | 是 | 来源模块。 |
| `sourceTemplateRef` | object | 是 | 来源模板引用摘要，例如 notification 模板 code 或 alerting 模板名。 |
| `providerId` | string | 是 | provider ID。 |
| `channel` | string | 是 | `ExternalChannel`。 |
| `externalTemplateKey` | string 或 null | 是 | 外部平台模板键摘要，最多 120 位。第一版不调用真实平台模板。 |
| `allowedVariables` | string[] | 是 | 允许变量名，最多 50 个。 |
| `renderMode` | string | 是 | `ExternalRenderMode`。 |
| `fallbackTitleTemplate` | string | 是 | 脱敏标题模板，2 到 120 位。 |
| `fallbackBodyTemplate` | string | 是 | 脱敏正文模板，1 到 3000 位。 |
| `status` | string | 是 | `ExternalTemplateMappingStatus`。 |
| `version` | integer | 是 | 版本，从 `1` 开始。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ExternalRoutePolicy

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `routeId` | string | 是 | 路由策略 ID。 |
| `displayName` | string | 是 | 路由名称，2 到 80 位。 |
| `sourceModule` | string | 是 | 来源模块。 |
| `eventType` | string | 是 | 事件类型，3 到 120 位。 |
| `riskLevel` | string | 是 | 公共风险等级。 |
| `matchers` | object | 是 | 匹配器，第一版支持 `sourceModule`、`eventType`、`riskLevel`、`labels` 和 `receiverType` 精确匹配。 |
| `providerId` | string | 是 | provider ID。 |
| `templateMappingId` | string | 是 | 模板映射 ID。 |
| `receiverSummary` | object | 是 | 默认 receiver 摘要，不得包含完整账号、手机号、邮箱、token 或 webhook。 |
| `groupingPolicy` | object | 是 | 分组摘要，包括 `groupBy`、`groupWaitSeconds`、`groupIntervalSeconds`。 |
| `retryPolicySummary` | object | 是 | 重试摘要，包括最大尝试数、退避窗口和过期窗口。 |
| `status` | string | 是 | `ExternalRoutePolicyStatus`。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ExternalDeliveryRequest

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `deliveryId` | string | 是 | 投递请求 ID。 |
| `sourceModule` | string | 是 | 来源模块。 |
| `sourceId` | string 或 null | 是 | 来源业务 ID。 |
| `eventType` | string | 是 | 事件类型。 |
| `riskLevel` | string | 是 | 公共风险等级。 |
| `routeId` | string 或 null | 是 | 命中的路由策略 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `channel` | string | 是 | `ExternalChannel`。 |
| `templateMappingId` | string 或 null | 是 | 模板映射 ID。 |
| `receiverSummary` | object | 是 | receiver 脱敏摘要。 |
| `payloadSummary` | object | 是 | 载荷摘要，只保存变量白名单、字段名、字段数量、字符串长度、值类型和短 hash，不保存完整通知正文或完整变量值。 |
| `status` | string | 是 | `ExternalDeliveryStatus`。 |
| `attempts` | integer | 是 | 尝试次数。 |
| `lastAttemptAt` | string 或 null | 是 | 最近尝试时间。 |
| `nextRetryAt` | string 或 null | 是 | 下次重试时间。 |
| `expiresAt` | string 或 null | 是 | 投递过期时间。 |
| `failureCode` | string 或 null | 是 | 失败码摘要。 |
| `failureSummary` | string 或 null | 是 | 失败摘要，必须脱敏。 |
| `idempotencyKey` | string 或 null | 是 | 幂等键摘要，可脱敏显示。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ExternalDeliveryAttempt

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `attemptId` | string | 是 | 投递尝试 ID。 |
| `deliveryId` | string | 是 | 投递请求 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `channel` | string | 是 | `ExternalChannel`。 |
| `attemptNo` | integer | 是 | 尝试序号，从 `1` 开始。 |
| `status` | string | 是 | `ExternalAttemptStatus`。 |
| `requestSummary` | object | 是 | 请求摘要，不返回完整正文、headers、URL 或 secret。 |
| `responseSummary` | object | 是 | 响应摘要，不返回完整平台响应或 token。 |
| `failureCode` | string 或 null | 是 | 失败码摘要。 |
| `failureSummary` | string 或 null | 是 | 失败摘要。 |
| `startedAt` | string | 是 | 开始时间。 |
| `finishedAt` | string 或 null | 是 | 完成时间。 |
| `simulated` | boolean | 是 | 第一版固定为 `true`。 |

### ExternalReceiverSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `receiverId` | string | 是 | receiver 摘要 ID。 |
| `receiverType` | string | 是 | `ExternalReceiverType`。 |
| `channel` | string | 是 | `ExternalChannel`。 |
| `displayName` | string | 是 | 脱敏展示名，2 到 80 位。 |
| `providerId` | string 或 null | 是 | 关联 provider。 |
| `sourceModule` | string 或 null | 是 | 来源模块。 |
| `targetRefSummary` | object | 是 | 目标引用摘要，不返回完整邮箱、手机号、账号、webhook、token 或设备 token。 |
| `verified` | boolean | 是 | 是否已由来源模块或 provider 摘要确认。 |
| `lastUsedAt` | string 或 null | 是 | 最近使用时间。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReasons` | string[] | 是 | 降级原因。 |

### ExternalNotificationAuditLog

审计字段继承公共契约，允许补充 `providerId`、`capabilityId`、`mappingId`、`routeId`、`deliveryId`、`attemptId`、`receiverId`、`sourceModule`、`dependencyStatus`、`stateFrom`、`stateTo`、`idempotencyKey`、`confirmTextMatched` 和 `failureReason`。审计列表只读，不提供删除接口。审计响应不得返回 token、secret、完整 webhook、完整请求头、完整通知正文、内部 URL、内部路径、完整异常栈、数据库连接串或前序模块私有数据。

### CrossPlatformNotificationOpsSummary

字段至少包含 `service`、`port`、`storageMode`、`authMode`、`providerAdapterMode`、`notificationAdapterMode`、`testControlsEnabled`、`providersTotal`、`enabledProvidersTotal`、`templateMappingsTotal`、`enabledTemplateMappingsTotal`、`routesTotal`、`enabledRoutesTotal`、`deliveriesTotal`、`simulatedSentTotal`、`simulatedFailedTotal`、`retryScheduledTotal`、`attemptsTotal`、`receiversTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastDeliveryAt`、`lastFailureAt`、`degraded`、`degradeReasons` 和 `productionGaps`。

## 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `47120` | 502 | notification 不可用。 |
| `47121` | 504 | notification 调用超时。 |
| `47122` | 502 | notification 响应不兼容。 |
| `47130` | 502 | alerting 不可用。 |
| `47131` | 504 | alerting 调用超时。 |
| `47132` | 502 | alerting 响应不兼容。 |
| `47140` | 502 | plugin-integration 不可用。 |
| `47141` | 504 | plugin-integration 调用超时。 |
| `47142` | 502 | plugin-integration 响应不兼容。 |
| `47150` | 502 | auth 认证上下文不可用。 |
| `47151` | 504 | auth 认证上下文调用超时。 |
| `47152` | 502 | auth 认证上下文字段不兼容。 |
| `47160` | 502 | 来源模块不可用。 |
| `47161` | 504 | 来源模块调用超时。 |
| `47162` | 502 | 来源模块摘要不兼容。 |
| `49950` | 404 | provider 不存在。 |
| `49951` | 404 | 模板映射不存在。 |
| `49952` | 404 | 路由策略不存在。 |
| `49953` | 404 | 投递请求不存在。 |
| `49954` | 404 | 投递尝试不存在。 |
| `49955` | 404 | receiver 摘要不存在。 |
| `49956` | 404 | 渠道能力不存在。 |
| `49960` | 409 | 状态不允许当前操作。 |
| `49961` | 409 | provider、模板映射、路由或 receiver 冲突。 |
| `49962` | 409 | 幂等键请求指纹冲突。 |
| `49963` | 400 | endpoint 或 receiver 不安全。 |
| `49964` | 400 | receiver 摘要不合法。 |
| `49965` | 400 | 模板变量不允许。 |
| `49966` | 400 | 渠道或能力不支持。 |
| `49967` | 409 | 真实发送被第一版策略阻断。 |
| `49968` | 409 | 投递重试窗口已过。 |
| `49969` | 409 | 速率限制策略冲突。 |
| `55800` | 500 | cross-platform-notification 内部错误。 |
| `55801` | 500 | cross-platform-notification 审计写入失败。 |
| `55802` | 500 | cross-platform-notification 状态写入失败。 |
| `55803` | 500 | cross-platform-notification 投递记录写入失败。 |
| `55804` | 500 | cross-platform-notification 依赖摘要写入失败。 |

字段校验、未登录、令牌格式错误、角色权限不足、能力点不足、高风险确认缺失、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/cross-platform-notification/health` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/cross-platform-notification/admin/ops/summary` | 是 | `NODE_READ` | LOW |
| provider 列表 | GET | `/api/v1/cross-platform-notification/admin/providers` | 是 | `NODE_READ` | LOW |
| provider 详情 | GET | `/api/v1/cross-platform-notification/admin/providers/{providerId}` | 是 | `NODE_READ` | LOW |
| 创建 provider | POST | `/api/v1/cross-platform-notification/admin/providers` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | HIGH |
| 更新 provider | PATCH | `/api/v1/cross-platform-notification/admin/providers/{providerId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | HIGH |
| 启用 provider | PATCH | `/api/v1/cross-platform-notification/admin/providers/{providerId}/enable` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 禁用 provider | PATCH | `/api/v1/cross-platform-notification/admin/providers/{providerId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档 provider | PATCH | `/api/v1/cross-platform-notification/admin/providers/{providerId}/archive` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 渠道能力列表 | GET | `/api/v1/cross-platform-notification/admin/capabilities` | 是 | `NODE_READ` | LOW |
| 渠道能力详情 | GET | `/api/v1/cross-platform-notification/admin/capabilities/{capabilityId}` | 是 | `NODE_READ` | LOW |
| 模板映射列表 | GET | `/api/v1/cross-platform-notification/admin/template-mappings` | 是 | `NODE_READ` | LOW |
| 模板映射详情 | GET | `/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}` | 是 | `NODE_READ` | LOW |
| 创建模板映射 | POST | `/api/v1/cross-platform-notification/admin/template-mappings` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 更新模板映射 | PATCH | `/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 启用模板映射 | PATCH | `/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/enable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 禁用模板映射 | PATCH | `/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档模板映射 | PATCH | `/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/archive` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 路由策略列表 | GET | `/api/v1/cross-platform-notification/admin/routes` | 是 | `NODE_READ` | LOW |
| 路由策略详情 | GET | `/api/v1/cross-platform-notification/admin/routes/{routeId}` | 是 | `NODE_READ` | LOW |
| 创建路由策略 | POST | `/api/v1/cross-platform-notification/admin/routes` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 更新路由策略 | PATCH | `/api/v1/cross-platform-notification/admin/routes/{routeId}` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 启用路由策略 | PATCH | `/api/v1/cross-platform-notification/admin/routes/{routeId}/enable` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 禁用路由策略 | PATCH | `/api/v1/cross-platform-notification/admin/routes/{routeId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档路由策略 | PATCH | `/api/v1/cross-platform-notification/admin/routes/{routeId}/archive` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 测试路由策略 | POST | `/api/v1/cross-platform-notification/admin/routes/{routeId}/test` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 创建投递请求 | POST | `/api/v1/cross-platform-notification/admin/deliveries` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 投递请求列表 | GET | `/api/v1/cross-platform-notification/admin/deliveries` | 是 | `NODE_READ` | LOW |
| 投递请求详情 | GET | `/api/v1/cross-platform-notification/admin/deliveries/{deliveryId}` | 是 | `NODE_READ` | LOW |
| 重试投递 | PATCH | `/api/v1/cross-platform-notification/admin/deliveries/{deliveryId}/retry` | 是 | `ADMIN` 或 `OWNER`，`HIGH_RISK_APPROVE` | HIGH |
| 取消投递 | PATCH | `/api/v1/cross-platform-notification/admin/deliveries/{deliveryId}/cancel` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE`；取消 `HIGH` 或 `CRITICAL` 投递时还要求 `HIGH_RISK_APPROVE` 或 `OWNER` | 按投递风险 |
| 投递尝试列表 | GET | `/api/v1/cross-platform-notification/admin/attempts` | 是 | `NODE_READ` | LOW |
| 投递尝试详情 | GET | `/api/v1/cross-platform-notification/admin/attempts/{attemptId}` | 是 | `NODE_READ` | LOW |
| receiver 摘要列表 | GET | `/api/v1/cross-platform-notification/admin/receivers` | 是 | `NODE_READ` | LOW |
| receiver 摘要详情 | GET | `/api/v1/cross-platform-notification/admin/receivers/{receiverId}` | 是 | `NODE_READ` | LOW |
| 审计列表 | GET | `/api/v1/cross-platform-notification/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 健康和自检接口

`GET /api/v1/cross-platform-notification/health` 成功返回 HTTP `200`，`data` 至少包含 `service=cross-platform-notification`、`status`、`version` 和 `requestId`。进程存活但依赖不可用时可以返回 `status=DEGRADED`，但不得返回 provider、receiver、外部 endpoint、依赖详细错误、投递数量或敏感字段。

`GET /api/v1/cross-platform-notification/admin/ops/summary` 成功返回 `CrossPlatformNotificationOpsSummary`。第六期合并后必须返回 `port=8133`、`legacyPort=8123`、`storageMode=IN_MEMORY`、`providerAdapterMode=SIMULATION_ONLY`、`notificationAdapterMode=TEST_STUB`、`testControlsEnabled` 和生产化缺口。读取失败返回 `55800`，不得伪造健康。

## Provider 接口

`GET /api/v1/cross-platform-notification/admin/providers` 支持 `page`、`pageSize`、`keyword`、`channel`、`status`、`healthStatus`、`sourceModule`、`degraded`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`、`lastTestAt_desc` 和 `lastDeliveryAt_desc`。成功响应分页 `items` 为 `ExternalChannelProvider[]`。

`GET /api/v1/cross-platform-notification/admin/providers/{providerId}` 返回 provider 详情、能力摘要、最近投递摘要、最近失败摘要、依赖摘要和最近审计摘要。provider 不存在返回 `49950`。响应不得返回完整 endpoint、token、secret、真实外部账号、请求 headers 或内部 URL。

`POST /api/v1/cross-platform-notification/admin/providers` 请求字段包括 `channel`、`displayName`、`endpointSummary`、`credentialRefSummary`、`receiverPolicy`、`allowedSourceModules`、`allowedRiskLevels`、`rateLimitSummary`、`reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `REGISTER_EXTERNAL_PROVIDER`。创建默认状态为 `DRAFT`，成功响应 HTTP `201`，`data` 为 `ExternalChannelProvider`。endpoint 或 receiver 不安全返回 `49963`。同一未归档 provider 下 `channel + displayName`、规范化 `endpointSummary` 或凭据引用冲突返回 `49961`。真实 token、secret 或完整 webhook 字段出现在任意层级返回 `40001`。

`PATCH /api/v1/cross-platform-notification/admin/providers/{providerId}` 可修改创建接口中的业务字段，`reason` 必填。修改 `endpointSummary`、`credentialRefSummary`、`receiverPolicy`、`allowedSourceModules` 或 `allowedRiskLevels` 时必须携带 `confirmText=UPDATE_EXTERNAL_PROVIDER`。`ARCHIVED` provider 不允许修改，返回 `49960`。审计失败返回 `55801` 且状态不变。

`PATCH /api/v1/cross-platform-notification/admin/providers/{providerId}/enable` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ENABLE_EXTERNAL_PROVIDER`。`DRAFT`、`DISABLED` 和 `DEGRADED` 可流转为 `ENABLED`。启用前必须校验 endpoint 摘要安全、凭据引用只为摘要、receiver policy 合法、allowed source modules 非空、allowed risk levels 非空、能力摘要存在。重复启用保持幂等。

`PATCH /api/v1/cross-platform-notification/admin/providers/{providerId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 或 `DEGRADED` 可流转为 `DISABLED`。禁用后不再被路由策略或新投递请求使用，历史投递不删除。重复禁用保持幂等。

`PATCH /api/v1/cross-platform-notification/admin/providers/{providerId}/archive` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ARCHIVE_EXTERNAL_PROVIDER`。只有 `DRAFT`、`DISABLED` 或 `DEGRADED` 可归档；`ENABLED` provider 必须先禁用后归档；存在启用模板映射、启用路由策略、未终态投递时返回 `49960`。`ARCHIVED` 为终态。

## 渠道能力接口

`GET /api/v1/cross-platform-notification/admin/capabilities` 支持 `page`、`pageSize`、`providerId`、`channel`、`supportsMarkdown`、`supportsRichBlocks`、`supportsDeliveryCallback`、`keyword` 和 `sort`。`sort` 允许 `updatedAt_desc`、`channel_asc`、`maxBodyLength_desc`。成功响应分页 `items` 为 `ExternalChannelCapability[]`。能力由 provider 初始化、测试适配器或后续真实适配器维护，第一版不提供浏览器写接口。

`GET /api/v1/cross-platform-notification/admin/capabilities/{capabilityId}` 返回能力详情、provider 摘要和最近降级原因。能力不存在返回 `49956`。响应不得返回外部平台私有能力 payload。

## 模板映射接口

`GET /api/v1/cross-platform-notification/admin/template-mappings` 支持 `page`、`pageSize`、`keyword`、`sourceModule`、`providerId`、`channel`、`status`、`renderMode` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`sourceModule_asc`、`version_desc`。成功响应分页 `items` 为 `ExternalTemplateMapping[]`。

`GET /api/v1/cross-platform-notification/admin/template-mappings/{mappingId}` 返回模板映射详情、provider 摘要、来源模板摘要、最近投递摘要和最近审计摘要。模板映射不存在返回 `49951`。

`POST /api/v1/cross-platform-notification/admin/template-mappings` 请求字段包括 `sourceModule`、`sourceTemplateRef`、`providerId`、`externalTemplateKey`、`allowedVariables`、`renderMode`、`fallbackTitleTemplate`、`fallbackBodyTemplate`、`reason` 和 `idempotencyKey`。创建默认状态为 `DRAFT`，成功响应 HTTP `201`。provider 不存在返回 `49950`。provider 与 channel 不匹配返回 `49966`。模板字段引用未列入 `allowedVariables` 的变量返回 `49965`。同一来源模板、provider 和 render mode 的未归档映射冲突返回 `49961`。

`PATCH /api/v1/cross-platform-notification/admin/template-mappings/{mappingId}` 可修改创建接口中的业务字段，`reason` 必填。修改成功后 `version` 加一，已有投递的模板快照不受影响。`ARCHIVED` 映射不可修改。审计失败返回 `55801` 且状态不变。

`PATCH /api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/enable` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT` 或 `DISABLED` 可启用为 `ENABLED`。启用前必须校验 provider 为 `ENABLED`、变量定义和模板内容一致、channel 能力支持 render mode。重复启用保持幂等。

`PATCH /api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 可禁用为 `DISABLED`。禁用后新投递不再使用该映射。重复禁用保持幂等。

`PATCH /api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。只有 `DRAFT` 或 `DISABLED` 可归档；`ENABLED` 映射必须先禁用后归档；仍被启用路由策略引用时返回 `49960`。`ARCHIVED` 为终态。

## 路由策略接口

`GET /api/v1/cross-platform-notification/admin/routes` 支持 `page`、`pageSize`、`keyword`、`sourceModule`、`eventType`、`riskLevel`、`providerId`、`templateMappingId`、`status`、`receiverType` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`、`riskLevel_desc`。成功响应分页 `items` 为 `ExternalRoutePolicy[]`。

`GET /api/v1/cross-platform-notification/admin/routes/{routeId}` 返回路由策略详情、provider 摘要、模板映射摘要、最近投递摘要、最近测试摘要和最近审计摘要。路由不存在返回 `49952`。

`POST /api/v1/cross-platform-notification/admin/routes` 请求字段包括 `displayName`、`sourceModule`、`eventType`、`riskLevel`、`matchers`、`providerId`、`templateMappingId`、`receiverSummary`、`groupingPolicy`、`retryPolicySummary`、`reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `CONFIGURE_EXTERNAL_ROUTE`。创建默认状态为 `DRAFT`，成功响应 HTTP `201`。provider 不存在返回 `49950`，模板映射不存在返回 `49951`，receiver 不合法返回 `49964`，receiver 或 endpoint 不安全返回 `49963`，路由冲突返回 `49961`。

`PATCH /api/v1/cross-platform-notification/admin/routes/{routeId}` 可修改创建接口中的业务字段，`reason` 必填。修改 provider、模板映射、receiver、matchers、riskLevel、retry policy 或 grouping policy 时必须携带 `confirmText=UPDATE_EXTERNAL_ROUTE`。`ARCHIVED` 路由不可修改。审计失败返回 `55801` 且状态不变。

`PATCH /api/v1/cross-platform-notification/admin/routes/{routeId}/enable` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ENABLE_EXTERNAL_ROUTE`。`DRAFT` 或 `DISABLED` 可启用为 `ENABLED`。启用前必须校验 provider 为 `ENABLED`、模板映射为 `ENABLED`、receiver 安全、retry policy 合法、risk level 被 provider 允许。重复启用保持幂等。

`PATCH /api/v1/cross-platform-notification/admin/routes/{routeId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 可禁用为 `DISABLED`。禁用不会删除历史投递。重复禁用保持幂等。

`PATCH /api/v1/cross-platform-notification/admin/routes/{routeId}/archive` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ARCHIVE_EXTERNAL_ROUTE`。只有 `DRAFT` 或 `DISABLED` 可归档；`ENABLED` 路由必须先禁用后归档；存在未终态投递时返回 `49960`。

`POST /api/v1/cross-platform-notification/admin/routes/{routeId}/test` 请求字段包括 `samplePayloadSummary`、`sampleReceiverSummary`、`dryRun`、`reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `TEST_EXTERNAL_ROUTE`。成功响应 `ExternalDeliveryRequest` 和最近 `ExternalDeliveryAttempt` 摘要。第一版只生成模拟投递，不发送真实外部消息。provider 降级或测试控制头模拟失败时生成 `SIMULATED_FAILED` 或返回依赖错误，同一实现版本必须固定并写入测试。

## 投递接口

`POST /api/v1/cross-platform-notification/admin/deliveries` 请求字段包括 `sourceModule`、`sourceId`、`eventType`、`riskLevel`、`routeId`、`providerId`、`templateMappingId`、`receiverSummary`、`payloadSummary`、`expiresAt`、`reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `CREATE_EXTERNAL_DELIVERY`。成功响应 HTTP `201`，`data` 为 `ExternalDeliveryRequest`。第一版必须创建模拟 attempt，结果只能为 `SIMULATED_SENT`、`SIMULATED_FAILED`、`BLOCKED` 或 `RETRY_SCHEDULED`，不得返回真实 `SENT`。路由启用时优先使用路由的 provider、模板映射、receiver 和 retry policy；显式 provider 或模板与路由冲突返回 `49961`。未传入 `routeId` 时，投递必须使用请求中的 `sourceModule`、`sourceId`、`eventType`、`riskLevel`、`receiverSummary` 和 `expiresAt` 生成投递快照，不能降级为固定 `custom`、`manual.external` 或 `MEDIUM`。未传入 `routeId` 时必须校验 provider 已启用、模板映射已启用、provider 允许该 `sourceModule` 和 `riskLevel`、receiver 类型在 provider 的 `receiverPolicy.allowedReceiverTypes` 内，且 payload 字段只包含模板映射允许变量；不满足时返回 `40001`、`49960`、`49964`、`49965` 或 `49966`。`sourceModule=alerting` 的请求必须额外校验 `eventType=alert.firing`、`sourceId` 为告警 ID 摘要、`payloadSummary` 不含完整日志或原始告警正文，并把返回 attempt 摘要交给 alerting 保存为 `externalAttemptStatus`。

`GET /api/v1/cross-platform-notification/admin/deliveries` 支持 `page`、`pageSize`、`sourceModule`、`sourceId`、`eventType`、`riskLevel`、`routeId`、`providerId`、`channel`、`status`、`receiverType`、`from`、`to`、`keyword` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`lastAttemptAt_desc`、`riskLevel_desc`、`status_asc`。成功响应分页 `items` 为 `ExternalDeliveryRequest[]`。

`GET /api/v1/cross-platform-notification/admin/deliveries/{deliveryId}` 返回投递详情、attempt 摘要、route 摘要、provider 摘要、receiver 摘要、依赖摘要和审计摘要。投递不存在返回 `49953`。

`PATCH /api/v1/cross-platform-notification/admin/deliveries/{deliveryId}/retry` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `RETRY_EXTERNAL_DELIVERY`。只有 `SIMULATED_FAILED`、`RETRY_SCHEDULED` 和可重试的 `BLOCKED` 可重试；`SIMULATED_SENT`、`CANCELED`、`EXPIRED` 不可重试。超过 retry window 返回 `49968`。重试仍只生成模拟 attempt。重复同幂等键返回同一结果。

`PATCH /api/v1/cross-platform-notification/admin/deliveries/{deliveryId}/cancel` 请求字段为 `reason` 和 `idempotencyKey`。只有 `QUEUED`、`RETRY_SCHEDULED` 和未过期的 `BLOCKED` 可取消为 `CANCELED`。取消 `HIGH` 或 `CRITICAL` 投递必须校验 `HIGH_RISK_APPROVE` 或 `OWNER`，缺少时返回 `42002`。审计风险等级必须使用被取消投递自身的 `riskLevel`，不能固定为 `MEDIUM`。`SIMULATED_SENT`、`SIMULATED_FAILED`、`EXPIRED` 和已 `CANCELED` 为终态或按固定幂等语义返回。取消不删除 attempt。

## 投递尝试和 receiver 接口

`GET /api/v1/cross-platform-notification/admin/attempts` 支持 `page`、`pageSize`、`deliveryId`、`providerId`、`channel`、`status`、`from`、`to` 和 `sort`。`sort` 允许 `startedAt_desc`、`finishedAt_desc`、`attemptNo_asc`、`status_asc`。成功响应分页 `items` 为 `ExternalDeliveryAttempt[]`。

`GET /api/v1/cross-platform-notification/admin/attempts/{attemptId}` 返回 attempt 详情、投递摘要、provider 摘要和脱敏 request/response 摘要。attempt 不存在返回 `49954`。响应不得返回完整外部请求、完整响应、headers、token、secret、完整正文或内部 URL。

`GET /api/v1/cross-platform-notification/admin/receivers` 支持 `page`、`pageSize`、`providerId`、`channel`、`receiverType`、`sourceModule`、`verified`、`degraded`、`keyword` 和 `sort`。`sort` 允许 `lastUsedAt_desc`、`displayName_asc`、`channel_asc`。成功响应分页 `items` 为 `ExternalReceiverSummary[]`。receiver 摘要由 route、delivery 或来源模块快照派生，第一版不提供浏览器直接创建真实 receiver 的接口。

`GET /api/v1/cross-platform-notification/admin/receivers/{receiverId}` 返回 receiver 详情、最近投递摘要和降级原因。receiver 不存在返回 `49955`。响应必须脱敏邮箱、手机号、外部账号、设备 token、webhook URL 和游戏目标。

## 审计接口

`GET /api/v1/cross-platform-notification/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`providerId`、`mappingId`、`routeId`、`deliveryId`、`attemptId`、`receiverId`、`sourceModule`、`result`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`、`riskLevel_desc`。只有 `ADMIN` 和 `OWNER` 可访问。审计列表只读，不提供删除、修改或恢复接口。

后台写操作必须记录调用者、调用者角色、调用者能力点摘要、来源 IP、`reason`、操作前状态、操作后状态、请求编号、结果和失败原因。审计响应字段必须至少包含公共契约要求的 `id`、`requestId`、`actorUserId`、`actorRole`、`actorPermissions`、`sourceIp`、`targetType`、`targetId`、`action`、`riskLevel`、`reason`、`paramsSummary`、`beforeState`、`afterState`、`result`、`failureReason` 和 `createdAt`，并可补充本模块的 provider、route、delivery、attempt 和 receiver 摘要 ID。审计写入失败时，provider、模板映射、路由策略、投递请求、重试、取消和测试路由不得假装成功，必须返回 `55801` 并保持业务状态不变。投递模拟失败可以保存失败 attempt 和失败审计，但不得返回真实发送成功。

## 状态、幂等和并发

provider 状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DEGRADED` 或 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`DEGRADED` 可恢复为 `ENABLED`、禁用或归档；`ARCHIVED` 为终态。

模板映射状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

路由策略状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

投递请求状态流转为 `QUEUED` 到 `SIMULATED_SENT`、`SIMULATED_FAILED`、`RETRY_SCHEDULED`、`BLOCKED`、`CANCELED` 或 `EXPIRED`；`SIMULATED_FAILED` 和 `RETRY_SCHEDULED` 可重试；`QUEUED`、`RETRY_SCHEDULED` 和未过期 `BLOCKED` 可取消；`SIMULATED_SENT`、`CANCELED`、`EXPIRED` 为终态。第一版不得出现真实 `SENT`、`DELIVERED` 或 `READ`。

写接口使用幂等键时，必须使用字段名排序后的稳定 JSON 语义计算请求体指纹，嵌套对象按字段名递归排序，数组保留顺序，不能依赖 Java `Map.toString()` 或浏览器字段顺序。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一响应快照；相同幂等键搭配不同请求体返回 `49962`。幂等键查找、状态校验、业务写入、审计写入、响应快照保存和幂等记录写入必须处于同一临界区内。

并发创建相同 provider、模板映射、路由策略、delivery 或 receiver 摘要时只能一个成功，其余返回冲突或相同幂等结果。后续数据库实现必须迁移为事务、唯一约束、条件更新或等效机制，不能降低并发口径。

所有支持 `from` 和 `to` 的列表接口必须按该资源主时间字段过滤时间范围。provider 使用 `updatedAt`，投递使用 `createdAt`，attempt 使用 `startedAt`，审计使用 `createdAt`。只传 `from` 时返回主时间大于等于 `from` 的记录，只传 `to` 时返回主时间小于等于 `to` 的记录，同时传入且 `from` 晚于 `to` 时返回 `40001`。`from` 或 `to` 不是 ISO 8601 时间字符串时返回 `40001`。

## 安全、降级和脱敏

任何请求体和响应都不得包含外部平台 token、webhook secret、完整 webhook URL、Discord token、Slack webhook URL、Telegram bot token、QQ token、Oopz token、企业微信 key、SMTP 密码、短信 token、推送 server key、设备 token、RCON 密码、完整 Authorization 请求头、完整请求 headers、完整通知正文、完整 raw payload、内部 URL、内部路径、节点地址、服务器密码、shell 命令、异常栈、数据库连接串、`.env`、`authorized_keys`、`id_rsa` 或前序模块私有数据。检查必须递归覆盖嵌套对象和数组。

endpoint 和 receiver URL 必须拒绝 `file:`、`data:`、`javascript:`、带用户名密码 URL、localhost、回环 IP、内网 IP、链路本地地址、未解析 host、通配符 `*`、空 host、控制字符和非法 URI。站内受控路径必须以 `/` 开头，不能以 `//` 开头，不能包含反斜杠或控制字符。

receiver 摘要必须按类型脱敏。邮箱最多显示域名和首尾字符，手机号最多显示国家码和后四位，外部用户 ID 只显示短 hash，webhook endpoint 只显示 provider 和域名摘要，设备 token 不得返回原值，游戏目标不得返回 RCON、命令或服务器内部地址。

外部依赖不可用时，读取类接口可以返回已有快照并标记 `degraded=true`、`stale=true` 和 `degradeReasons`。写入类接口不得假装成功。provider 降级、速率限制或模拟发送失败必须写入投递失败摘要，不能返回真实外部投递成功。真实发送被第一版阻断时返回 `49967` 或创建 `BLOCKED` 投递摘要，同一实现版本必须固定并写入测试。

第一版不得提供真实删除 provider、模板映射、路由策略、投递、attempt、receiver 或审计的接口。确需清理历史记录时，必须在后续独立契约中增加归档或保留策略接口，并重新完成文档、测试红灯、实现和回归闭环。

## 验收口径

`cross-platform-notification` API 文档必须按 `docs/contracts-cross-platform-notification.md` 独立存在，并由 `.local-docs/tests-cross-platform-notification.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、高风险确认缺失、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求、敏感字段脱敏、测试控制头默认关闭和模块验收口径。

`cross-platform-notification` 完成时必须满足以下条件：当前运行入口为 `ops-core-service:8133`，历史端口 `8123` 只作为 `legacyPort` 返回；健康检查公开且不泄露敏感信息；后台接口按角色和能力点限制；provider、渠道能力、模板映射、路由策略、投递请求、投递尝试、receiver 摘要、审计、幂等、状态流转、依赖降级、审计失败回滚、敏感字段脱敏、测试控制头默认关闭和自检摘要都有自动化验证；自动化测试必须先红灯；实现后在 `ops-core-service` 中全量测试通过；当前后端运行入口回归通过；边界扫描无违规命中；不修改前序模块稳定接口；不直接读取前序模块数据库；不导入前序模块 Java package；不调用真实 `external-node-executor`；不执行真实外部通知发送；不保存真实外部 token、完整 webhook、SMTP 密码、短信 token、机器人 token、设备 token、RCON 密码或完整请求头；不把站内通知主数据、告警规则、插件事件、社区工单、活动、日历、白名单、考勤、资源下载、运维任务、节点文件管理或终端能力塞进本服务；不得恢复 `backend/cross-platform-notification-service` 独立 Maven 入口。
