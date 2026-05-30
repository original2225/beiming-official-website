# 北冥官网 api-gateway API 契约

版本：0.2

## 文档定位

本文档是 `api-gateway` 微服务的正式 API 契约。`api-gateway` 是北冥官网后端统一入口，只负责统一路由、请求编号、认证头透传、认证上下文注入、基础 CORS、上游超时、请求边界保护、响应头白名单透传、错误降级、路由表、自检摘要、上游健康摘要和网关级请求日志摘要。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、运维能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `api-gateway` 的路径、路由表、字段、错误码、降级、审计和验收口径。

第一版设计参考了成熟网关生态中的稳定做法。Spring Cloud Gateway 使用请求属性谓词匹配路由，并通过过滤器处理跨路由逻辑；Kong Gateway 把相关能力拆成可组合插件，例如 Correlation ID 和 Rate Limiting；Nginx 反向代理强调 `proxy_pass`、请求头和请求体透传；Envoy 将上游服务作为 cluster 处理，并把健康检查结果纳入路由判断。第二版继续参考 AWS API Gateway 对请求体大小上限的明确约束、Cloudflare Rules 对边界策略前置的做法，以及 Nginx 对响应头和上游头的显式控制。`api-gateway` 本轮只吸收这些边界思路，不引入动态服务发现、插件市场、真实 WAF、分布式限流、OAuth/OIDC 或 WebSocket 长连接代理。

参考来源为官方文档：Spring Cloud Gateway Request Predicates 与 Gateway Filters、Kong Gateway Correlation ID 与 Rate Limiting 插件、Nginx `ngx_http_proxy_module`、Envoy upstream health checking、AWS API Gateway quotas、Cloudflare Rules。

## 职责边界

`api-gateway` 负责以下能力：

| 能力 | 说明 |
| --- | --- |
| 路由匹配 | 根据固定路径前缀把请求转发到已有微服务端口。 |
| 请求编号 | 接收或生成 `X-Request-Id`，向上游和下游保持一致。 |
| 认证透传与上下文注入 | 原样透传 `Authorization: Bearer <token>`；携带可验证会话时，通过 `auth` 会话校验生成可信身份头。 |
| 请求透传 | 保持 HTTP 方法、路径、查询参数、JSON 请求体和必要请求头。 |
| 请求边界保护 | 校验 `X-Request-Id` 格式，限制 P0 JSON 请求体大小，拒绝明显异常入口请求。 |
| 响应透传 | 上游响应状态码、统一响应体、内容类型和响应头白名单默认原样返回。 |
| CORS | 对 `/api/v1/**` 提供本地前端允许的预检响应。 |
| 超时与降级 | 上游不可连接返回网关错误，上游超时返回网关超时错误，不伪造业务成功。 |
| 路由表 | 暴露只读路由注册表，便于前端和运维控制台确认入口可用性。 |
| 上游健康摘要 | 维护手动刷新后的上游健康快照。 |
| 请求日志摘要 | 记录脱敏后的网关访问摘要，用于本地排障和后台观测。 |

`api-gateway` 不负责注册、登录、角色能力点主数据、成员档案、站内通知、内容发布、资源下载、社区工单、活动报名、日历、更新日志、服务器运维、节点执行、备份恢复、告警规则、插件事件、外部通知或镜像市场业务。它不能直接读取任何服务数据库，不能导入任何前序服务 Java package，不能复制业务状态机，不能把多个业务服务聚合成新的业务结果。

上游服务的业务认证、权限、字段校验、状态流转、幂等、审计和失败降级仍由对应微服务负责。网关发现上游返回 4xx 或 5xx 时默认透传，不把业务失败改成网关成功，也不替上游重写业务错误码。

## 基础路径、端口和认证

`api-gateway` 本地端口固定为 `8125`。

网关自有接口使用 `/api/v1/gateway` 前缀。业务转发接口保持上游原路径，例如 `/api/v1/auth/login` 经网关访问时仍是 `/api/v1/auth/login`，不会改写为 `/api/v1/gateway/auth/login`。

公开健康检查无需认证。网关自有后台接口需要 `Authorization: Bearer <token>`。P0 本地实现允许 `owner-token`、`admin-token` 和 `helper-token` 访问只读后台自检、路由和健康接口；请求日志接口只允许 `owner-token` 和 `admin-token` 访问，`helper-token` 与 `user-token` 均返回 `42001`；缺失或格式错误返回公共认证错误码。

除本地固定 token 外，网关自有后台接口也可以通过 `auth` 的 `GET /api/v1/auth/session/verify` 校验真实会话。校验成功后，`user.roles` 中包含 `HELPER`、`ADMIN` 或 `OWNER` 可访问只读后台接口，包含 `ADMIN` 或 `OWNER` 可访问请求日志接口。校验返回认证或权限错误时，网关返回对应错误；`auth` 不可连接、超时或返回 `5xx` 时，网关返回 `46000` 或 `46001`，不得把无法校验的 token 当作已登录用户。

业务转发接口不在网关层做强制角色判断。公开业务接口可以无 `Authorization` 透传；需要登录或后台权限的业务接口由上游服务按自身契约返回 `41000`、`41001`、`42001` 或其他业务错误码。业务转发请求如果携带 `Authorization: Bearer <token>` 且目标路由不是 `auth`，网关会向 `auth` 会话校验接口做一次短路径校验。校验成功时，网关向上游注入可信身份头；校验失败、超时或 `auth` 不可用时，网关不注入可信身份头，但仍透传原始 `Authorization` 给目标上游，由目标上游按自身契约判定请求是否可继续。

网关注入的可信身份头只允许由网关生成，客户端传入同名头必须在转发前剥离。P0 可信身份头如下。

| 请求头 | 来源 | 说明 |
| --- | --- | --- |
| `X-Beiming-Actor-User-Id` | `auth.data.user.id` | 当前用户 ID。 |
| `X-Beiming-Actor-Roles` | `auth.data.user.roles` | 逗号分隔角色。 |
| `X-Beiming-Actor-Permissions` | `auth.data.user.permissions` | 逗号分隔能力点，可为空字符串。 |
| `X-Beiming-Actor-Minecraft-Id` | `auth.data.user.minecraftBinding.minecraftId` | 已绑定时注入。 |
| `X-Beiming-Actor-Minecraft-Uuid` | `auth.data.user.minecraftBinding.minecraftUuid` | 已绑定时注入。 |
| `X-Gateway-Internal-Request-Id` | 网关请求编号 | 标记该可信上下文来自当前网关请求。 |

## 路由注册表

路由表是只读配置。网关不得在运行时通过接口新增、修改或删除路由。新增业务服务时必须先完成该服务契约和测试，再更新网关契约、测试和路由表。

| 路由 ID | 服务键 | 路径前缀 | 上游端口 | 健康探测路径 |
| --- | --- | --- | --- | --- |
| `auth` | `AUTH` | `/api/v1/auth` | `8101` | `/api/v1/auth/session/verify` |
| `profile` | `PROFILE` | `/api/v1/profile` | `8102` | `/api/v1/profile/members` |
| `notification` | `NOTIFICATION` | `/api/v1/notifications` | `8103` | `/api/v1/notifications/me/unread-count` |
| `content` | `CONTENT` | `/api/v1/content` | `8104` | `/api/v1/content/homepage` |
| `server-status` | `SERVER_STATUS` | `/api/v1/server-status` | `8105` | `/api/v1/server-status/overview` |
| `resource` | `RESOURCE` | `/api/v1/resources` | `8106` | `/api/v1/resources` |
| `admin` | `ADMIN` | `/api/v1/admin` | `8107` | `/api/v1/admin/overview` |
| `onboarding` | `ONBOARDING` | `/api/v1/onboarding` | `8108` | `/api/v1/onboarding/me/progress` |
| `exam` | `EXAM` | `/api/v1/exams` | `8109` | `/api/v1/exams/me/sessions` |
| `whitelist` | `WHITELIST` | `/api/v1/whitelist` | `8110` | `/api/v1/whitelist/me/applications/current` |
| `attendance` | `ATTENDANCE` | `/api/v1/attendance` | `8111` | `/api/v1/attendance/me/summary` |
| `community` | `COMMUNITY` | `/api/v1/community` | `8112` | `/api/v1/community/boards` |
| `activity` | `ACTIVITY` | `/api/v1/activity` | `8113` | `/api/v1/activity/events` |
| `calendar` | `CALENDAR` | `/api/v1/calendar` | `8114` | `/api/v1/calendar/upcoming` |
| `changelog` | `CHANGELOG` | `/api/v1/changelog` | `8115` | `/api/v1/changelog/versions/latest` |
| `ops-control` | `OPS_CONTROL` | `/api/v1/ops-control` | `8116` | `/api/v1/ops-control/overview` |
| `node-daemon` | `NODE_DAEMON` | `/api/v1/node-daemon` | `8117` | `/api/v1/node-daemon/health` |
| `cloudreve-sync` | `CLOUDREVE_SYNC` | `/api/v1/cloudreve-sync` | `8118` | `/api/v1/cloudreve-sync/health` |
| `backup-recovery` | `BACKUP_RECOVERY` | `/api/v1/backup-recovery` | `8119` | `/api/v1/backup-recovery/health` |
| `alerting` | `ALERTING` | `/api/v1/alerting` | `8120` | `/api/v1/alerting/health` |
| `online-map` | `ONLINE_MAP` | `/api/v1/online-map` | `8121` | `/api/v1/online-map/health` |
| `plugin-integration` | `PLUGIN_INTEGRATION` | `/api/v1/plugin-integration` | `8122` | `/api/v1/plugin-integration/health` |
| `cross-platform-notification` | `CROSS_PLATFORM_NOTIFICATION` | `/api/v1/cross-platform-notification` | `8123` | `/api/v1/cross-platform-notification/health` |
| `ops-image-market` | `OPS_IMAGE_MARKET` | `/api/v1/ops-image-market` | `8124` | `/api/v1/ops-image-market/health` |
| `material` | `MATERIAL` | `/api/v1/materials` | `8126` | `/api/v1/materials/featured` |

路径匹配规则为最长前缀优先。`/api/v1/resources` 和 `/api/v1/resources/**` 都必须命中 `resource`。未知路径返回网关错误，不转发到任何上游。

## 网关自有对象

### GatewayRoute

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `routeId` | string | 是 | 路由 ID。 |
| `serviceKey` | string | 是 | 服务键，大写枚举。 |
| `serviceName` | string | 是 | 服务展示名。 |
| `pathPrefix` | string | 是 | 匹配路径前缀。 |
| `upstreamBaseUrl` | string | 是 | 上游基础地址，本地默认 `http://127.0.0.1:<port>`。 |
| `upstreamPort` | integer | 是 | 上游端口。 |
| `healthCheckPath` | string | 是 | 健康刷新使用的路径。 |
| `timeoutMs` | integer | 是 | 单次转发超时时间。 |
| `enabled` | boolean | 是 | 是否启用，P0 固定为 `true`。 |
| `methods` | string[] | 是 | 允许转发的方法，P0 为 `GET`、`POST`、`PUT`、`PATCH`、`DELETE` 和 `OPTIONS`。 |
| `authDelegated` | boolean | 是 | 是否把认证权限交给上游处理，业务路由固定为 `true`。 |
| `createdAt` | string | 是 | 路由注册时间。 |
| `updatedAt` | string | 是 | 路由更新时间。 |

### GatewayUpstreamHealth

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `serviceKey` | string | 是 | 服务键。 |
| `routeId` | string | 是 | 路由 ID。 |
| `pathPrefix` | string | 是 | 路径前缀。 |
| `status` | string | 是 | `UNKNOWN`、`UP`、`DEGRADED`、`DOWN` 或 `TIMEOUT`。 |
| `lastHttpStatus` | integer 或 null | 是 | 最近一次健康请求 HTTP 状态。 |
| `lastErrorCode` | integer 或 null | 是 | 最近一次网关错误码。 |
| `lastErrorMessage` | string 或 null | 是 | 脱敏错误摘要。 |
| `lastCheckedAt` | string 或 null | 是 | 最近检查时间。 |
| `durationMs` | integer 或 null | 是 | 最近检查耗时。 |

健康状态判定：健康刷新请求得到任意小于 `500` 的 HTTP 状态，说明上游可达，状态为 `UP`。得到 `500` 到 `599`，状态为 `DEGRADED`。连接失败为 `DOWN`。超过网关超时时间为 `TIMEOUT`。健康刷新不得把业务 `401`、`403` 或 `404` 当作上游不可用。

### GatewayRequestLog

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `requestId` | string | 是 | 请求编号。 |
| `method` | string | 是 | HTTP 方法。 |
| `path` | string | 是 | 请求路径，不包含 query。 |
| `routeId` | string 或 null | 是 | 命中的路由 ID。 |
| `serviceKey` | string 或 null | 是 | 命中的服务键。 |
| `upstreamStatus` | integer 或 null | 是 | 上游 HTTP 状态。 |
| `gatewayStatus` | integer | 是 | 下游 HTTP 状态。 |
| `result` | string | 是 | `SUCCESS` 或 `FAILED`。 |
| `errorCode` | integer 或 null | 是 | 网关级错误码或上游响应体中的业务错误码。 |
| `durationMs` | integer | 是 | 网关处理耗时。 |
| `clientIp` | string 或 null | 是 | 客户端 IP 摘要。 |
| `actorUserId` | string 或 null | 是 | 已通过 `auth` 会话校验时记录当前用户 ID，否则为 `null`。 |
| `createdAt` | string | 是 | 记录时间。 |

请求日志不得保存请求体、完整 query、完整 token、Cookie、Authorization 原文、外部 webhook、registry 凭据、节点密钥、文件内容、日志正文或异常堆栈。

## 网关错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `46200` | 404 | 网关没有匹配路由。 |
| `46201` | 405 | 网关路由不支持该方法。 |
| `46202` | 400 | 网关请求格式错误。 |
| `46203` | 400 | 网关分页、排序或筛选参数错误。 |
| `46204` | 413 | 网关请求体超过 P0 限制。 |
| `46205` | 400 | 网关请求编号格式非法。 |
| `46210` | 502 | 上游服务不可连接。 |
| `46211` | 504 | 上游服务调用超时。 |
| `46212` | 502 | 上游返回空响应或非 HTTP 响应。 |
| `46213` | 502 | 上游地址配置无效。 |
| `51250` | 500 | api-gateway 内部错误。 |

业务上游返回的错误码由上游契约决定，网关默认原样透传，不映射成以上错误码。

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 网关健康检查 | GET | `/api/v1/gateway/health` | 否 | 无 | LOW |
| 网关自检摘要 | GET | `/api/v1/gateway/admin/ops/summary` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 网关路由列表 | GET | `/api/v1/gateway/admin/routes` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 网关路由详情 | GET | `/api/v1/gateway/admin/routes/{routeId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 上游健康列表 | GET | `/api/v1/gateway/admin/upstreams` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 刷新上游健康 | POST | `/api/v1/gateway/admin/upstreams/{serviceKey}/health-refresh` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 网关请求日志 | GET | `/api/v1/gateway/admin/request-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 业务请求转发 | `GET/POST/PUT/PATCH/DELETE/OPTIONS` | `/api/v1/{module}/**` | 由上游决定 | 由上游决定 | 由上游决定 |

## 网关健康检查

`GET /api/v1/gateway/health`

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "api-gateway",
    "status": "UP",
    "port": 8125,
    "routesTotal": 25,
    "generatedAt": "2026-05-29T00:00:00Z"
  },
  "requestId": "req_example"
}
```

业务规则：该接口只表示网关进程可用，不表示全部上游可用。上游可用性通过上游健康列表读取。

## 网关自检摘要

`GET /api/v1/gateway/admin/ops/summary`

成功响应 HTTP `200`。

响应字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `service` | string | 是 | 固定为 `api-gateway`。 |
| `port` | integer | 是 | 固定为 `8125`。 |
| `routesTotal` | integer | 是 | 路由总数，P0 为 `25`。 |
| `enabledRoutesTotal` | integer | 是 | 启用路由总数。 |
| `upstreamsUp` | integer | 是 | 最近健康状态为 `UP` 的上游数量。 |
| `upstreamsDegraded` | integer | 是 | 最近健康状态为 `DEGRADED` 的上游数量。 |
| `upstreamsDown` | integer | 是 | 最近健康状态为 `DOWN` 或 `TIMEOUT` 的上游数量。 |
| `requestLogsRetained` | integer | 是 | 当前保留请求日志数量。 |
| `productionGaps` | string[] | 是 | P0 已知生产化差距摘要。 |
| `generatedAt` | string | 是 | 生成时间。 |

降级规则：自检摘要只读取网关内存状态，不主动调用所有上游，避免一次后台刷新拖垮上游。

## 网关路由列表

`GET /api/v1/gateway/admin/routes`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`，最小 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `keyword` | string | 否 | 匹配路由 ID、服务键或路径前缀。 |
| `serviceKey` | string | 否 | 服务键。 |
| `enabled` | boolean | 否 | `true` 或 `false`。 |
| `sort` | string | 否 | 允许 `routeId_asc`、`serviceKey_asc`、`upstreamPort_asc`、`updatedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `GatewayRoute[]`。

字段校验失败返回 `46203`。`serviceKey` 不存在返回 `46203`。该接口不得返回 token、Cookie、真实凭据、上游响应体、请求日志正文或异常栈。

## 网关路由详情

`GET /api/v1/gateway/admin/routes/{routeId}`

成功响应 HTTP `200`，`data` 为 `GatewayRoute`。

路由不存在返回 `43000`。`routeId` 只允许小写字母、数字和短横线，格式错误返回 `40001`。

## 上游健康列表

`GET /api/v1/gateway/admin/upstreams`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `status` | string | 否 | `UNKNOWN`、`UP`、`DEGRADED`、`DOWN` 或 `TIMEOUT`。 |
| `serviceKey` | string | 否 | 服务键。 |
| `sort` | string | 否 | 允许 `serviceKey_asc`、`status_asc`、`lastCheckedAt_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `GatewayUpstreamHealth[]`。

业务规则：该接口只读取最近健康快照，不主动探测上游。尚未刷新过的上游状态为 `UNKNOWN`。

## 刷新上游健康

`POST /api/v1/gateway/admin/upstreams/{serviceKey}/health-refresh`

请求体为空。成功响应 HTTP `200`，`data` 为 `GatewayUpstreamHealth`。

业务规则：网关对目标上游执行一次 `GET healthCheckPath` 请求。健康刷新使用与该路由相同的超时。请求中的 `X-Request-Id` 会被透传给上游。请求中的 `Authorization` 可以透传给上游，但健康状态不得依赖具体业务权限通过。

`serviceKey` 不存在返回 `43000`。连接失败返回 HTTP `200` 且 `data.status` 为 `DOWN`，并记录 `lastErrorCode: 46210`。超时返回 HTTP `200` 且 `data.status` 为 `TIMEOUT`，并记录 `lastErrorCode: 46211`。上游返回空响应或非 HTTP 响应时记录 `lastErrorCode: 46212`。上游地址配置无效时记录 `lastErrorCode: 46213`。健康刷新自身不能因为单个上游失败返回 5xx，除非网关内部状态不可用。

## 网关请求日志

`GET /api/v1/gateway/admin/request-logs`

查询参数：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | 否 | 默认 `1`。 |
| `pageSize` | integer | 否 | 默认 `20`，最大 `100`。 |
| `routeId` | string | 否 | 路由 ID。 |
| `serviceKey` | string | 否 | 服务键。 |
| `result` | string | 否 | `SUCCESS` 或 `FAILED`。 |
| `from` | string | 否 | ISO 8601 时间。 |
| `to` | string | 否 | ISO 8601 时间。 |
| `sort` | string | 否 | 允许 `createdAt_desc`、`createdAt_asc`、`durationMs_desc`。 |

成功响应 HTTP `200`，分页 `items` 为 `GatewayRequestLog[]`。

权限规则：只有 `ADMIN` 和 `OWNER` 可访问。`HELPER` 返回 `42001`。

业务规则：日志保留数量由实现固定，P0 至少保留最近 `200` 条。时间范围格式错误、`from` 晚于 `to`、排序参数非法或枚举非法返回 `46203`。请求日志不得通过网关 API 删除。

## 业务请求转发

`GET/POST/PUT/PATCH/DELETE/OPTIONS /api/v1/{module}/**`

转发规则：

| 规则 | 要求 |
| --- | --- |
| 路径 | 保持原始 path，不改写模块前缀。 |
| Query | 原样保留 query string。 |
| 方法 | 保持原 HTTP 方法。 |
| 请求体 | 对 `POST`、`PUT` 和 `PATCH` 原样透传 body。 |
| 请求体大小 | P0 JSON 请求体最大 `1048576` 字节，超过返回 HTTP `413` 和错误码 `46204`。 |
| Content-Type | 原样透传。 |
| Accept | 原样透传。 |
| Authorization | 原样透传，不写入日志。 |
| X-Request-Id | 缺失时生成，转发给上游，并回传给客户端。只允许 1 到 128 位的字母、数字、下划线、短横线、点和冒号，非法返回 HTTP `400` 和错误码 `46205`。 |
| X-Forwarded-For | 默认使用当前连接远端地址；后续接入可信反向代理后再启用代理链追加。 |
| Hop-by-hop header | 不透传 `Connection`、`Transfer-Encoding`、`Upgrade`、`Keep-Alive`、`TE`、`Trailer` 和 `Proxy-Authorization`。 |
| 可信身份头 | 浏览器传入的 `X-Beiming-Actor-*`、`X-Gateway-Internal-*` 等可信身份头必须丢弃。 |
| 可信身份注入 | `auth` 会话校验成功时，网关注入 `X-Beiming-Actor-*` 和 `X-Gateway-Internal-Request-Id`；校验失败时不注入。 |
| 响应 | 上游 HTTP 状态、响应体、Content-Type 以及响应头白名单默认原样返回。 |
| 响应头白名单 | 允许透传 `Content-Type`、`Cache-Control`、`ETag`、`Location`、`Content-Disposition`、`Last-Modified` 和 `Expires`；其他响应头默认丢弃，避免泄露内部实现或不安全代理头。 |
| 日志 | 只记录脱敏摘要。 |

上游返回 2xx、4xx、5xx 或非标准 HTTP 状态码时，网关默认透传。上游不可连接返回 HTTP `502` 和错误码 `46210`。上游超时返回 HTTP `504` 和错误码 `46211`。上游返回空响应或非 HTTP 响应返回 HTTP `502` 和错误码 `46212`。上游地址配置无效返回 HTTP `502` 和错误码 `46213`。未知路径返回 HTTP `404` 和错误码 `46200`。不支持的方法返回 HTTP `405` 和错误码 `46201`。请求体超过网关 P0 上限返回 HTTP `413` 和错误码 `46204`。请求编号非法返回 HTTP `400` 和错误码 `46205`。

## CORS 规则

本地开发允许以下来源访问 `/api/v1/**`：

| Origin |
| --- |
| `http://localhost:5173` |
| `http://127.0.0.1:5173` |
| `http://localhost:5174` |
| `http://127.0.0.1:5174` |
| `http://localhost:5182` |
| `http://127.0.0.1:5182` |

允许方法为 `GET`、`POST`、`PUT`、`PATCH`、`DELETE` 和 `OPTIONS`。允许请求头包括 `Authorization`、`Content-Type`、`Accept-Language`、`X-Request-Id` 和前端常用安全请求头。响应必须暴露 `X-Request-Id`。CORS 预检不写业务审计，不转发到业务上游。

## 分页、幂等和状态流转

网关自有列表接口统一使用公共分页格式。`page` 从 `1` 开始，`pageSize` 默认 `20`，最大 `100`。

网关自有接口不创建业务资源，不支持客户端幂等键。业务接口的幂等规则完全由上游服务维护，网关只透传 `idempotencyKey` 所在请求体，不读取、不存储、不改写。

网关自有状态只有上游健康快照状态。状态流转由健康刷新结果决定：`UNKNOWN` 可以转为 `UP`、`DEGRADED`、`DOWN` 或 `TIMEOUT`；后续任一状态都可以在下一次刷新后转为其他健康状态。该状态不改变路由启用状态，也不阻止业务转发。

## 审计和日志要求

网关自有后台接口需要记录请求日志摘要。业务转发请求也需要记录请求日志摘要。P0 不写入独立审计存储，只维护内存摘要和请求编号，后续接入正式审计服务时必须先更新契约。

请求日志必须脱敏。以下内容不得存储或返回：请求体、完整 query、完整 `Authorization`、Cookie、密码、邀请码原始码、密码重置令牌、节点密钥、registry 凭据、Cloudreve token、外部 webhook、文件内容、终端命令正文、日志正文、异常堆栈。

网关不得接受浏览器传入的可信身份头作为真实身份。`X-Beiming-Actor-User-Id`、`X-Beiming-Actor-Roles`、`X-Gateway-Internal-Token` 等头如果来自客户端请求，必须在转发前移除。网关向上游注入可信身份时，只能来自 `auth` 会话校验结果。`auth` 校验失败时不得沿用客户端伪造头，不得根据 token 字符串自行推断用户身份。

## 失败降级

网关自身失败时必须返回统一错误响应，不返回 HTML 错误页。

单个上游不可用只影响该上游对应请求和健康摘要，不影响其他路由。上游不可连接返回 `46210`，上游超时返回 `46211`，上游非 HTTP 响应返回 `46212`，上游地址配置无效返回 `46213`。网关不能把上游失败转成空成功响应，不能改写上游业务错误为成功，不能重试非幂等请求。

`GET` 请求可以在未来加入只读缓存或重试策略，但 P0 不做缓存和自动重试，避免返回旧业务状态或重复触发上游副作用。

## 生产化差距

P0 `api-gateway` 是本地契约实现，必须在自检摘要中明确以下生产化差距：尚未接入真实服务发现，尚未接入集中配置，尚未接入分布式限流，认证上下文已支持通过 `auth` 会话校验注入但尚未接入内部签名和缓存，尚未接入持久化审计，尚未代理 WebSocket 和大文件流。

这些差距不得影响 P0 的路径转发、请求编号、认证透传、可信身份头剥离、可验证认证上下文注入、错误降级、路由表和测试闭环。

## 验收口径

`api-gateway` API 文档按 `docs/contracts-api-gateway.md` 独立存在，并由 `.local-docs/tests-api-gateway.md` 记录本地测试闭环。

本文档列出的每个网关自有接口都有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、分页排序、状态刷新、失败降级、日志脱敏和验收口径。业务转发测试必须覆盖 25 个已有微服务的路径前缀，确认路由表端口准确、请求编号透传、请求编号非法拒绝、认证头透传、可信身份头剥离、`auth` 会话校验成功后的可信身份注入、`auth` 校验失败后的不注入降级、查询参数透传、JSON body 透传、请求体大小限制、响应头白名单、上游 2xx 透传、上游 4xx 透传、上游 5xx 透传、未知路径、非法方法、CORS 预检、上游不可用、上游超时和敏感字段不落日志。

开发完成后必须执行 `mvn -f backend/api-gateway-service/pom.xml test`，并执行已有 24 个稳定后端微服务和 `material` 的相关回归测试，确认网关新增没有修改前序服务结构、接口、端口、响应格式、认证方式、错误码、状态机、测试或构建脚本。测试过程必须写入 `.local-docs/tests-api-gateway.md`。
