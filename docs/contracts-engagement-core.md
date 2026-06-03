# 北冥官网 engagement-core API 契约

版本：0.1

## 文档定位

本文档是 `engagement-core` 运行合并单元的正式 API 契约。`engagement-core` 用于承载第三批社区运营后端模块，包括 `community`、`activity`、`calendar` 和 `changelog`。本文档只定义合并后的运行形态、模块装配、自检接口、内部适配边界、网关切换边界和验收口径，不替代四个业务模块自己的 API 契约。

本文档继承 `docs/contracts-common.md`、`docs/contracts-community.md`、`docs/contracts-activity.md`、`docs/contracts-calendar.md` 和 `docs/contracts-changelog.md`。四个业务模块的路径、方法、认证、权限、请求字段、响应字段、错误码、分页、幂等、状态流转、降级、审计和验收口径仍以各自契约为准。

`engagement-core` 不是新的业务模块，不新增社区、活动、日程或更新日志的业务语义。它的目标是把社区运营域收敛为一个 Spring Boot 运行单元，同时保留模块边界、数据主权、适配方向和原 API 行为。

## 职责边界

`engagement-core` 负责以下能力。

| 能力 | 说明 |
| --- | --- |
| 运行合并 | 用一个 Spring Boot 运行单元承载第三批四个社区运营模块。 |
| 模块装配 | 按原模块包名、路由和契约装配 controller、service、adapter、store 和测试替身。 |
| 契约保持 | 保持四个模块既有 API 路径、HTTP 方法、响应结构、错误码、认证、权限、状态流转、幂等和审计行为。 |
| 内部适配 | 把第三批内部跨服务 HTTP 适配收敛为同进程 adapter 或 facade，但不允许跨模块直接读写主数据。 |
| 前序适配 | 通过 `business-core`、`admission-core` 或前序模块正式接口适配 auth、profile、notification、content、server-status、resource、admin、whitelist 和 attendance。 |
| 自检摘要 | 暴露 `engagement-core` 自身健康检查和后台装配摘要，便于迁移验证。 |
| 网关切换状态 | 为 `api-gateway` 第三批路径上游切换提供稳定目标和完成状态。 |

`engagement-core` 不负责吸收 `api-gateway`。当前阶段仍保留 `api-gateway-service` 作为统一入口。`engagement-core` 不负责第一批基础业务模块、第二批入服准入模块、`ops-control`、`node-daemon`、`cloudreve-sync`、`backup-recovery`、`alerting`、`online-map`、插件集成、跨平台通知、素材、指南或 P3 扩展。

`engagement-core` 不允许把真实服务器维护、白名单命令、积分直写、Minecraft 控制台、节点守护进程、容器、终端、文件管理、备份恢复、Cloudreve 管理或公告主发布塞进社区运营域。活动奖励只能保留贡献候选，社区处罚只影响社区写权限，维护窗口只作为日程元数据，更新日志只保存发布说明和来源快照。

## 运行形态

本地验证运行单元为 `backend/engagement-core-service`，本地验证端口为 `8132`。端口 `8112` 到 `8115` 只保留为第三批模块历史原服务端口记录，不再作为旧服务回归基线。端口 `8130` 继续保留给已完成第一批合并的 `business-core-service`，端口 `8131` 继续保留给已完成第二批合并的稳定前序运行单元 `admission-core-service`，端口 `8125` 继续保留给 `api-gateway-service`。

Spring Boot 主应用建议放在 `cn.beiming.engagement`，组件扫描范围覆盖 `cn.beiming`。第三批模块应保留原包名 `cn.beiming.community`、`cn.beiming.activity`、`cn.beiming.calendar` 和 `cn.beiming.changelog`。不得为了合并进行无业务收益的大规模包名迁移。

`engagement-core` 的自有路径前缀为 `/api/v1/engagement-core`。四个业务模块路径保持原样，不加 `/engagement-core` 前缀。

## 承载模块

| 模块 | 正式契约 | 路径前缀 | 原服务端口 | 现有代码路由数 | 是否进入 engagement-core |
| --- | --- | --- | --- | ---: | --- |
| `community` | `docs/contracts-community.md` | `/api/v1/community` | `8112` | 64 | 是 |
| `activity` | `docs/contracts-activity.md` | `/api/v1/activity` | `8113` | 41 | 是 |
| `calendar` | `docs/contracts-calendar.md` | `/api/v1/calendar` | `8114` | 21 | 是 |
| `changelog` | `docs/contracts-changelog.md` | `/api/v1/changelog` | `8115` | 23 | 是 |

第三批合并后，`engagement-core` 需要承载以上 149 个既有业务方法路由。`engagement-core` 自身新增 2 个运行单元自检路由。合并验证总方法路由数为 151。四个业务模块自检摘要必须返回当前运行端口 `port=8132`，并用 `legacyPort` 记录各自历史端口。

## API 路径清单

四个业务模块的完整接口定义仍在各模块正式契约中维护。本文档只登记它们在 `engagement-core` 中的装配范围。

| 路径前缀 | 方法范围 | 业务归属 | 完整接口定义 |
| --- | --- | --- | --- |
| `/api/v1/community/**` | `GET`、`POST`、`PATCH`、`DELETE` | 论坛板块、帖子、评论、互动、投票、举报、工单、处罚、社区审计 | `docs/contracts-community.md` |
| `/api/v1/activity/**` | `GET`、`POST`、`PUT`、`PATCH` | 活动、报名、候补、签到、结果、奖励、贡献候选、活动审计 | `docs/contracts-activity.md` |
| `/api/v1/calendar/**` | `GET`、`POST`、`PATCH` | 日程事件、月视图、即将开始、关注、activity 摘要同步、日程审计 | `docs/contracts-calendar.md` |
| `/api/v1/changelog/**` | `GET`、`POST`、`PATCH` | 更新日志、版本发布、变更项、收藏、日历同步摘要、更新日志审计 | `docs/contracts-changelog.md` |
| `/api/v1/engagement-core/**` | `GET` | `engagement-core` 运行单元自检 | 本文档 |

路径匹配必须保持既有模块前缀。`/api/v1/community/**` 不得改成 `/api/v1/engagement-core/community/**`。`/api/v1/activity/**` 不得误命中 community。`/api/v1/calendar/**` 不得误命中 changelog。相似路径如 `/api/v1/community-core`、`/api/v1/activity-log`、`/api/v1/calendarize` 和 `/api/v1/changelogger` 不得误命中第三批业务模块。

## 自有对象

### EngagementCoreModuleStatus

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `moduleKey` | string | 是 | 模块键，允许 `COMMUNITY`、`ACTIVITY`、`CALENDAR`、`CHANGELOG`。 |
| `moduleName` | string | 是 | 模块展示名。 |
| `pathPrefix` | string | 是 | 模块路径前缀。 |
| `contract` | string | 是 | 模块正式契约文件路径。 |
| `port` | integer | 是 | 当前运行端口，第三批合并后固定为 `8132`。 |
| `legacyPort` | integer | 是 | 历史原服务端口，只用于追溯，不作为运行入口、网关上游或测试命令。 |
| `mounted` | boolean | 是 | 模块是否已装配到 `engagement-core`。 |
| `routesTotal` | integer | 是 | 该模块在当前运行单元内登记的路由数量。 |
| `contractRoutesTotal` | integer | 是 | 该模块契约期望路由数量。 |
| `adapters` | string[] | 是 | 当前模块需要的内部 adapter 或 facade 摘要。 |
| `upstreamDependencies` | string[] | 是 | 该模块依赖的前序模块或运行单元摘要。 |
| `compatibilityMode` | string | 是 | `LEGACY_BASELINE`、`IN_PROCESS_ADAPTER` 或 `GATEWAY_SWITCH_READY`。 |
| `lastVerifiedAt` | string 或 null | 是 | 最近一次契约测试通过时间。 |
| `status` | string | 是 | `NOT_MOUNTED`、`MOUNTED`、`DEGRADED` 或 `READY`。 |
| `gaps` | string[] | 是 | 当前模块仍未完成的迁移或生产化缺口。 |

### EngagementCoreOpsSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `service` | string | 是 | 固定为 `engagement-core`。 |
| `port` | integer | 是 | 本地验证固定为 `8132`。 |
| `status` | string | 是 | `UP`、`DEGRADED` 或 `DOWN`。 |
| `modulesTotal` | integer | 是 | 固定为 `4`。 |
| `modulesMounted` | integer | 是 | 已装配模块数量。 |
| `routesTotal` | integer | 是 | 当前运行单元登记路由总数。 |
| `engagementRoutesTotal` | integer | 是 | 四个业务模块方法路由总数，完成后为 `149`。 |
| `selfRoutesTotal` | integer | 是 | `engagement-core` 自有路由总数，固定为 `2`。 |
| `moduleRoutes` | `EngagementCoreModuleStatus[]` | 是 | 四个模块装配状态。 |
| `adapterChain` | object[] | 是 | community、activity、calendar 和 changelog 的只读适配链摘要。 |
| `businessCoreDependency` | object | 是 | 第一批 `business-core` 前序依赖摘要。 |
| `admissionCoreDependency` | object | 是 | 第二批 `admission-core` 稳定前序运行单元摘要。 |
| `gatewaySwitchReady` | boolean | 是 | 是否已满足网关切换前置条件。 |
| `gatewaySwitchStatus` | string | 是 | 网关切换状态，允许 `NOT_READY`、`READY` 或 `COMPLETED`。 |
| `legacyBaselines` | object[] | 是 | 当前仍保留的外部基线摘要。第三批旧四服务清理后只包含 `business-core-service`、`admission-core-service` 和 `api-gateway-service`。 |
| `retiredLegacyServices` | object[] | 是 | 已由 `engagement-core` 替代并清理 Maven 入口的第三批旧服务摘要。 |
| `productionGaps` | string[] | 是 | 生产化差距摘要。 |
| `generatedAt` | string | 是 | 摘要生成时间。 |

## 接口总览

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| engagement-core 健康检查 | GET | `/api/v1/engagement-core/health` | 否 | 无 | LOW |
| engagement-core 后台装配摘要 | GET | `/api/v1/engagement-core/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 四个业务模块接口 | 继承各模块契约 | `/api/v1/community/**`、`/api/v1/activity/**`、`/api/v1/calendar/**`、`/api/v1/changelog/**` | 继承各模块契约 | 继承各模块契约 | 继承各模块契约 |

## 健康检查

`GET /api/v1/engagement-core/health`

该接口无需认证，只表示 `engagement-core` 进程和运行单元自检能力可用，不表示四个业务模块全部契约通过。

请求字段：无。

成功响应 HTTP `200`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "engagement-core",
    "status": "UP",
    "port": 8132,
    "modulesTotal": 4,
    "modulesMounted": 4,
    "engagementRoutesTotal": 149,
    "selfRoutesTotal": 2,
    "moduleRoutes": [
      {
        "moduleKey": "COMMUNITY",
        "pathPrefix": "/api/v1/community",
        "mounted": true,
        "routesTotal": 64,
        "status": "READY"
      }
    ],
    "generatedAt": "2026-06-02T08:26:06Z"
  },
  "requestId": "req_example"
}
```

响应字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `service` | string | 是 | 固定为 `engagement-core`。 |
| `status` | string | 是 | `UP`、`DEGRADED` 或 `DOWN`。 |
| `port` | integer | 是 | 本地验证固定为 `8132`。 |
| `modulesTotal` | integer | 是 | 固定为 `4`。 |
| `modulesMounted` | integer | 是 | 已装配模块数量。 |
| `engagementRoutesTotal` | integer | 是 | 四个业务模块方法路由总数，完成后为 `149`。 |
| `selfRoutesTotal` | integer | 是 | `engagement-core` 自有路由数，固定为 `2`。 |
| `moduleRoutes` | object[] | 是 | 低敏模块路由摘要，只返回 `moduleKey`、`pathPrefix`、`mounted`、`routesTotal` 和 `status`。 |
| `generatedAt` | string | 是 | ISO 8601 时间。 |

失败规则：运行单元内部异常返回 HTTP `500` 和错误码 `53230`。模块装配异常导致无法生成健康摘要时返回 HTTP `500` 和错误码 `53231`。该接口不得返回 token、Cookie、真实数据库连接串、异常栈、外部凭据、请求头原文、举报证据详情、工单内部备注、通知正文、真实服务器命令、节点凭据或 Cloudreve token。

分页规则：无分页。

幂等规则：只读接口，重复调用不改变任何业务状态。

审计要求：无后台业务审计要求，但必须保留请求编号，便于运行日志排障。

## 后台装配摘要

`GET /api/v1/engagement-core/admin/ops/summary`

该接口需要 `Authorization: Bearer <token>`。只有 `ADMIN` 和 `OWNER` 可访问。未登录返回公共错误码 `41000`，令牌格式错误返回 `41003`，权限不足返回 `42001`。`HELPER` 和 `USER` 均不得读取第三批运行单元装配摘要。

请求字段：无。

成功响应 HTTP `200`，`data` 为 `EngagementCoreOpsSummary`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "engagement-core",
    "port": 8132,
    "status": "UP",
    "modulesTotal": 4,
    "modulesMounted": 4,
    "routesTotal": 151,
    "engagementRoutesTotal": 149,
    "selfRoutesTotal": 2,
    "moduleRoutes": [],
    "adapterChain": [
      {
        "from": "activity",
        "to": "community",
        "adapter": "CommunityPublicSnapshotAdapter",
        "mutable": false
      },
      {
        "from": "calendar",
        "to": "activity",
        "adapter": "ActivityCalendarSummaryAdapter",
        "mutable": false
      },
      {
        "from": "changelog",
        "to": "calendar",
        "adapter": "CalendarReleaseSummaryAdapter",
        "mutable": false
      }
    ],
    "businessCoreDependency": {
      "service": "business-core",
      "port": 8130,
      "status": "REQUIRED_BASELINE"
    },
    "admissionCoreDependency": {
      "service": "admission-core",
      "port": 8131,
      "status": "STABLE_BASELINE"
    },
    "gatewaySwitchReady": true,
    "gatewaySwitchStatus": "COMPLETED",
    "legacyBaselines": [
      {
        "service": "business-core-service",
        "port": 8130,
        "contract": "docs/contracts-business-core.md",
        "testCommand": "mvn -f backend/business-core-service/pom.xml test",
        "lastVerifiedAt": "2026-06-03T00:00:00+08:00"
      },
      {
        "service": "admission-core-service",
        "port": 8131,
        "contract": "docs/contracts-admission-core.md",
        "testCommand": "mvn -f backend/admission-core-service/pom.xml test",
        "lastVerifiedAt": "2026-06-03T00:00:00+08:00"
      },
      {
        "service": "api-gateway-service",
        "port": 8125,
        "contract": "docs/contracts-api-gateway.md",
        "testCommand": "mvn -f backend/api-gateway-service/pom.xml test",
        "lastVerifiedAt": "2026-06-03T00:00:00+08:00"
      }
    ],
    "retiredLegacyServices": [
      {
        "service": "community-service",
        "port": 8112,
        "directory": "backend/community-service",
        "contract": "docs/contracts-community.md",
        "status": "RETIRED",
        "testCommand": null,
        "retiredAt": "2026-06-03T00:00:00+08:00"
      }
    ],
    "productionGaps": [
      "complete inherited contract tests are not all mounted in engagement-core",
      "real auth and gateway trusted context adapters are not connected",
      "real database persistence is still module dependent",
      "persistent audit storage is not connected",
      "real cross-service adapters are still represented by local test stubs",
      "real notification delivery is not connected",
      "live gateway-to-engagement-core HTTP smoke is not verified"
    ],
    "generatedAt": "2026-06-02T08:26:06Z"
  },
  "requestId": "req_example"
}
```

业务规则：该接口只读取 `engagement-core` 内部装配状态和最近测试摘要，不主动执行四个模块的业务写操作，不调用旧服务进行实时健康探测，不把未完成模块伪装成 `READY`。第三批旧四服务清理后，`gatewaySwitchReady` 的判定只依赖四个模块在 `engagement-core` 中的继承契约测试、`business-core-service` 基线、`admission-core-service` 基线和 `api-gateway-service` 基线。只有当 `api-gateway` 契约、测试文档、自动化红灯、网关实现和全量后端回归均完成后，`gatewaySwitchStatus` 才能为 `COMPLETED`。四个业务模块自己的后台自检摘要必须同步合并后入口，返回 `port=8132` 和历史 `legacyPort`，不得继续把 `8112` 到 `8115` 暴露成当前运行端口。

失败规则：运行单元内部异常返回 `53230`。模块装配信息缺失返回 `53231`。当前登记路由与本文档或四个模块契约期望不一致时返回 `53232` 或在 `status=DEGRADED` 的成功摘要中列入 `gaps`，由实现按是否影响接口可用性决定。认证上下文解析失败返回原模块契约或公共认证错误，可信网关上下文字段缺失或格式不兼容时返回 `53233`。内部 adapter 链装配错误返回 `53234`。

分页规则：无分页。

幂等规则：只读接口，重复调用不改变任何业务状态。

状态流转：该接口不改变业务状态。模块装配状态只允许按迁移过程从 `NOT_MOUNTED` 进入 `MOUNTED`，契约测试通过后进入 `READY`，发现路由缺失、adapter 不可用、模块边界破坏或继承测试失败时进入 `DEGRADED`。

审计要求：读取后台装配摘要属于低风险后台读取，应保留请求编号、操作者、角色和访问时间的运行日志。不得记录 token 原文、举报证据、工单内部备注、通知正文、前序服务内部路径或真实运维参数。

## 认证上下文

四个业务模块继续兼容 `Authorization: Bearer <token>`。需要登录或后台权限的接口仍按各模块契约解析当前用户、角色、权限、Minecraft 绑定和用户状态。

经 `api-gateway` 访问时，`engagement-core` 继续兼容以下可信身份头。

| 请求头 | 说明 |
| --- | --- |
| `X-Beiming-Actor-User-Id` | 当前用户 ID。 |
| `X-Beiming-Actor-Roles` | 逗号分隔角色。 |
| `X-Beiming-Actor-Permissions` | 逗号分隔能力点。 |
| `X-Beiming-Actor-Minecraft-Id` | 已绑定的 Minecraft ID。 |
| `X-Beiming-Actor-Minecraft-Uuid` | 已绑定的 Minecraft UUID。 |
| `X-Gateway-Internal-Request-Id` | 网关注入的内部请求编号。 |

`X-Gateway-Internal-Request-Id` 存在时，各模块按自身契约优先解析可信认证上下文。字段缺失、格式非法、角色或能力点不兼容时，不得静默降级成匿名用户。生产入口必须由 `api-gateway` 或反向代理剥离客户端伪造的同名可信头；直连本地测试必须覆盖伪造头不能绕过权限的场景。

## 内部适配规则

`community` 仍拥有社区板块、帖子、评论、互动、投票、举报、工单、处罚、审计和社区自检主数据。它不能创建活动、写日历主数据、写更新日志、修改白名单状态、修改 attendance 积分或执行服务器命令。

`activity` 仍拥有活动、报名、候补、签到、结果、奖励、贡献候选、活动审计和自检主数据。它可以消费 community 公开讨论快照，但不能直接读取 community 内部 store，不能创建社区处罚，不能处理举报工单，不能写 attendance 积分流水。

`calendar` 仍拥有日程事件、关注、提醒摘要、来源同步快照、日程审计和自检主数据。它只能通过 `GET /api/v1/activity/calendar-summary` 或同语义 adapter 导入活动时间摘要，不能读取 activity 内部对象，不能修改活动报名、结果和奖励。维护窗口和服务器日程只是日程元数据，不触发真实运维。

`changelog` 仍拥有发布记录、变更分组、变更项、影响范围、关联资源快照、关联服务器状态快照、关联内容快照、关联日程摘要、通知摘要、更新日志审计和自检主数据。它可以消费 resource、server-status、content、calendar 和 notification 的正式摘要，但不能创建公告主数据，不能生成下载票据，不能刷新服务器状态，不能写 calendar 主数据，不能执行真实维护命令。

同 JVM 内部调用可以从 HTTP client 改为 adapter 或 facade，但 adapter 必须保留失败模拟能力，测试必须能覆盖 profile 失败、content 失败、resource 失败、notification 失败、community 快照失败、activity 摘要失败、calendar 同步失败、changelog 来源占位、审计失败、状态写入失败、互动或关注并发失败。

`engagement-core` 适配第一批模块时，应优先通过 `business-core` 已稳定的正式 API、认证上下文或清晰 adapter 读取，不得直接导入第一批模块的内存 store、Repository、实体或测试种子绕开边界。需要 attendance、whitelist 或入服链路信息时，应通过已完成第二批合并的 `admission-core-service` 稳定边界读取，不得反向修改准入链路状态。确需给前序模块新增能力时，必须按前序服务兼容变更流程先更新对应正式契约、本地测试文档、自动化测试和实现。

## 错误码

四个业务模块接口继续使用各自契约中的错误码，不因进入 `engagement-core` 改码。公共错误码继续继承 `docs/contracts-common.md`。

`engagement-core` 自有错误码如下。

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `53230` | 500 | engagement-core 内部错误。 |
| `53231` | 500 | engagement-core 模块装配错误。 |
| `53232` | 500 | engagement-core 路由快照与契约不一致。 |
| `53233` | 502 | engagement-core 可信认证上下文解析失败。 |
| `53234` | 500 | engagement-core 内部 adapter 链装配错误。 |

以上错误码只用于 `/api/v1/engagement-core/**` 自有接口，或用于运行单元装配层在请求到达业务模块前发生的错误。已经进入四个业务模块处理流程的请求，错误码必须按对应模块契约返回。

## 网关策略

当前网关已完成第三批路径切换。`api-gateway-service` 把 `community`、`activity`、`calendar` 和 `changelog` 路由统一指向 `engagement-core-service` 的 `8132`。旧端口 `8112` 到 `8115` 只作为历史原服务端口记录，不再作为第三批网关业务路由上游或回归测试入口。

第三批路径上游切换如下。

| 路由 ID | 路径前缀 | 旧端口 | 目标端口 |
| --- | --- | ---: | ---: |
| `community` | `/api/v1/community` | `8112` | `8132` |
| `activity` | `/api/v1/activity` | `8113` | `8132` |
| `calendar` | `/api/v1/calendar` | `8114` | `8132` |
| `changelog` | `/api/v1/changelog` | `8115` | `8132` |

网关切换后，业务路径仍保持 `/api/v1/community/**`、`/api/v1/activity/**`、`/api/v1/calendar/**` 和 `/api/v1/changelog/**`，不得改成 `/api/v1/engagement-core/<module>/**`。

## 后续完善顺序

第三批合并后的后续完善按以下优先级进入闭环。每一项都必须单独完成 API 契约、测试文档、红灯、实现、复测和记录。合并后自检端口口径已经纳入当前小步；完整继承契约测试仍是后续最高优先级，不能因为自检测试通过就视为业务契约完成。

| 顺序 | 完善项 | 契约要求 | 自动化测试要求 |
| --- | --- | --- | --- |
| 1 | 完整继承契约测试 | 把 community、activity、calendar 和 changelog 的全部 API 行为纳入 `engagement-core-service` 测试，不只保留代表路由。 | 新增模块级 contract cases，覆盖 149 个业务路由的成功、字段校验、认证、权限、资源不存在、状态冲突、幂等并发、降级、审计和生产硬化。 |
| 2 | 合并后自检端口口径 | 四个业务模块自检摘要返回 `port=8132` 和 `legacyPort`，旧端口只用于历史追溯。 | 已新增红灯测试先断言旧端口失败，再实现并复测。 |
| 3 | 真实认证与可信网关上下文 | 固定 token 只能用于本地测试 profile；生产路径必须消费 `auth` 或网关注入的可信身份上下文。 | 覆盖直连伪造 `X-Beiming-Actor-*` 不能绕过权限、网关注入上下文可用、字段缺失失败。 |
| 4 | 持久化与审计持久化 | 社区、活动、日程、更新日志、互动、报名、收藏、工单、处罚、幂等和审计迁入数据库事务或等效持久层。 | 覆盖重启后数据仍在、审计失败回滚、唯一约束、并发幂等和分页过滤。 |
| 5 | 真实跨服务 adapter | profile、content、resource、server-status、notification、attendance 和内部 calendar/changelog 适配通过正式接口或受控 adapter。 | 覆盖不可用、超时、字段不兼容、旧快照降级、通知失败不伪造成功。 |
| 6 | 真实 HTTP 联调 | `api-gateway-service` 到 `engagement-core-service:8132` 做真实进程 smoke，验证路径、认证、请求编号和错误透传。 | 启动当前入口后执行真实 HTTP smoke，记录到 `.local-docs/tests-engagement-core.md` 和 `.local-docs/tests-api-gateway.md`。 |

## 迁移顺序

迁移顺序固定为基线验证、`engagement-core` 空壳与自检红灯、`community`、`activity`、`calendar`、`changelog`、网关切换准备、网关切换实现、全量回归。

`community` 必须先迁入，因为它是社区互动和治理底座，也是 activity 可选讨论快照来源。`activity` 必须在 community 之后迁入，因为它只能消费 community 公开快照。`calendar` 必须在 activity 之后迁入，因为它只能消费 activity 日历摘要。`changelog` 必须在 calendar 之后迁入，因为它保存 calendar 同步摘要和发布日程引用。

第三批旧四服务清理后，后续补强必须先根据本文档和该模块正式契约生成或补齐 `engagement-core` 自动化测试，确认测试因为行为不满足而失败，再修改当前实现。实现后必须运行 `engagement-core-service` 对应测试和当前前序依赖测试，不得恢复或执行旧四服务测试。

## 验收口径

`engagement-core` 完成的最低标准是，单进程承载第三批四个业务模块的全部既有 API 路径，且响应格式、错误码、认证、权限、请求编号、分页、状态流转、幂等、审计和降级行为与四个模块正式契约一致。

`mvn -f backend/engagement-core-service/pom.xml test` 必须覆盖本文档两个自有接口和四个模块继承过来的全部契约测试。第三批旧服务清理后，相关回归基线为 `engagement-core-service`、`business-core-service`、`admission-core-service` 和 `api-gateway-service`，不得为了测试恢复 `community-service`、`activity-service`、`calendar-service` 或 `changelog-service`。

当前阶段不得把 `engagement-core-service` 的 8 个自检与代表路由测试视为第三批业务完成。只有 149 个业务方法路由的继承契约测试和 2 个自有接口测试都进入自动化验证，并且所有当前回归入口全绿，第三批合并后完善闭环才算完成。

`engagement-core` 直连合并和第三批网关切换均已完成测试闭环。第三批业务路径经网关访问时仍保持原路径，网关只切换上游端口，不改写业务前缀。

用户确认后，第三批旧服务源码和 Maven 运行入口按明确文件路径逐个清理。旧服务目录不得因本契约自动批量删除；后续如需继续清理残留空目录或其他文件，必须单独确认范围，删除文件只能逐个明确路径处理。第一批旧服务目录 `backend/auth-service`、`backend/profile-service`、`backend/notification-service`、`backend/content-service`、`backend/server-status-service`、`backend/resource-service`、`backend/admin-service`，第二批旧服务目录 `backend/onboarding-service`、`backend/exam-service`、`backend/whitelist-service`、`backend/attendance-service`，以及第三批旧服务目录 `backend/community-service`、`backend/activity-service`、`backend/calendar-service`、`backend/changelog-service` 已经完成合并清理，不得通过 Git 恢复、复制目录、重建 Maven 入口、重建启动类或重写旧测试命令。

第三批验收还必须满足社区运营边界不被破坏：community 不吞 activity、calendar 或 changelog 主数据；activity 不改 community 状态、不写 attendance 积分；calendar 不改 activity 状态、不触发真实运维；changelog 不写 calendar 主数据、不创建公告、不生成资源下载票据、不执行真实维护命令。
