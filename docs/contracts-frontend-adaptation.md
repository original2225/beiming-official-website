# 北冥官网 frontend-adaptation API 契约

版本：0.1

## 文档定位

本文档是 `frontend-adaptation` 模块的正式契约。它不是新的后端微服务契约，也不新增浏览器可调用的后端业务 API。它定义官网前端应用如何消费现有 `api-gateway` 和各业务服务正式 API，如何组织页面路由、认证状态、权限渲染、接口映射、局部降级、危险操作确认、测试覆盖和验收口径。

本文档继承 `docs/contracts-common.md`、`docs/contracts-api-gateway.md` 和所有现有 `docs/contracts-*.md`。前端适配只能消费这些正式契约，不能写死首页内容、服务器状态、资源列表、用户权限、审核结果、后台待办、运维结果或外部平台状态。发现后端缺少能力时，必须回到对应前序服务按治理闭环处理，不能在前端伪造业务状态。

本轮参考了成熟在线平台和多人社区产品的前端信息架构，但只吸收适合北冥官网的工程口径。GitHub Organizations 和 GitLab Admin Area 的做法用于角色裁剪、审计入口和后台只读聚合。Grafana 的 dashboard、alerting 和 RBAC 思路用于运维摘要、健康降级和权限可见性。Discord Community Onboarding 和 Rules Screening 用于入服流程、规则确认和外部入口展示。Roblox Creator Dashboard 用于创作者式内容、体验状态和运营指标入口。Modrinth 项目、版本和下载结构用于资源列表、版本详情和下载动作拆分。参考资料包括 [GitHub Organizations permissions](https://docs.github.com/en/organizations/managing-peoples-access-to-your-organization-with-roles)、[GitLab Admin Area](https://docs.gitlab.com/administration/admin_area/)、[GitLab Todos API](https://docs.gitlab.com/api/todos/)、[GitLab Audit Events API](https://docs.gitlab.com/api/audit_events/)、[Grafana RBAC](https://grafana.com/docs/grafana/latest/administration/roles-and-permissions/access-control/)、[Grafana Alerting](https://grafana.com/docs/grafana/latest/alerting/)、[Discord Community Onboarding FAQ](https://support.discord.com/hc/en-us/articles/11074987197975-Community-Onboarding-FAQ)、[Discord Rules Screening FAQ](https://support.discord.com/hc/en-us/articles/1500000466882-Rules-Screening-FAQ)、[Roblox Creator Dashboard](https://create.roblox.com/docs/production/creator-dashboard) 和 [Modrinth API](https://docs.modrinth.com/api/)。

## 职责边界

`frontend-adaptation` 负责新建正式前端应用 `frontend/beiming-web`，覆盖官网公开页、登录注册、用户中心、入服流程、考试与白名单、资源下载、指南知识库、社区活动日历、通知中心、管理后台、平台依赖摘要和运维控制台只读与受控操作界面。

`frontend-adaptation` 不拥有业务主数据，不新增后端业务接口，不直接读取数据库，不绕过 `api-gateway` 注入可信身份头，不保存服务端可信字段，不代替后端判断权限、状态流转、考试结果、白名单结果、考勤积分、通知未读数、后台待办、运维任务结果或审计结果。

现有 `frontend/auth-test-console` 继续作为 auth 本地测试台保留，不改造成正式官网。正式官网前端必须位于独立目录，独立维护 API client、路由、权限菜单、降级组件、测试和构建配置。

## 数据归属

前端只拥有浏览器运行态数据：当前路由、表单草稿、筛选条件、分页状态、展开折叠状态、加载状态、局部错误状态、主题偏好、最近请求编号、短期接口缓存和登录令牌本地保存状态。

前端不得把以下字段作为主数据保存或复用：用户角色和能力点最终判断、Minecraft 绑定可信状态、成员资格、首页配置、内容发布状态、资源可见范围、Cloudreve 分享密钥、服务器在线状态、入服流程状态、考试成绩、白名单审核状态、通知投递状态、后台待办状态、审计结果、运维节点 token、文件路径、终端命令、容器或实例真实执行结果。

前端缓存只能作为显示加速。任何写操作成功与否必须以后端统一响应为准。接口返回 `code != 0`、HTTP 非成功状态、超时、解析失败或响应字段不兼容时，不能把本地缓存当作成功结果。

## 应用路径、端口和 API 入口

正式前端目录固定为 `frontend/beiming-web`。开发端口固定为 `5175`。构建产物目录为该前端应用自己的 `dist`，不得提交。

默认 API 入口为 `http://127.0.0.1:8125`，即 `api-gateway`。生产和常规本地联调都必须经网关访问业务接口，保持业务原路径不变，例如 `/api/v1/auth/login`、`/api/v1/admin/overview` 和 `/api/v1/resources`。

本地调试允许通过 `VITE_BEIMING_API_BASE` 指向网关地址。只有在显式设置 `VITE_BEIMING_API_MODE=direct` 时，开发者才可以把只读调试请求指向单个服务固定端口。直连模式不得进入构建默认值，不得改变任何后端端口、路径、认证方式、响应格式或 CORS 契约。

前端不得发送 `X-Beiming-Actor-*`、`X-Gateway-Internal-*` 或其他可信身份头。可信身份只能由 `api-gateway` 根据 auth 会话校验注入。前端可以发送 `Authorization`、`Content-Type`、`Accept-Language` 和 `X-Request-Id`。

## 页面路由契约

| 页面域 | 前端路由 | 认证 | 后端数据源 |
| --- | --- | --- | --- |
| 官网首页 | `/` | 否 | `content` 首页、`server-status` 总览、`resources` 公开列表、`guides` 首页摘要、`activity` 摘要、`changelog` 列表 |
| 内容与公告 | `/content`、`/content/:slug` | 否 | `content` 公开内容列表、详情、专题和分类 |
| 服务器状态 | `/status`、`/status/:instanceId` | 否 | `server-status` 总览、实例、线路、历史快照和宕机记录 |
| 资源中心 | `/resources`、`/resources/:resourceId` | 视资源可见范围 | `resource` 公开资源、分类、版本和下载解析 |
| 指南知识库 | `/guides`、`/guides/:slug`、`/guides/rules/:version` | 公开读取否，反馈是 | `guide` 首页、分类、文章、搜索、规则、指令、外部入口和反馈 |
| 成员展示 | `/members`、`/members/:memberId` | 否 | `profile` 公开成员列表和详情 |
| 社区、活动、日历、更新 | `/community`、`/activity`、`/calendar`、`/changelog` | 公开读取否，参与动作是 | `community`、`activity`、`calendar`、`changelog` 的公开和当前用户接口 |
| 账号 | `/login`、`/register`、`/password-reset` | 否 | `auth` 注册、登录、密码重置 |
| 用户中心 | `/me`、`/me/profile`、`/me/sessions`、`/me/notifications` | 是 | `auth` 当前用户、会话、Minecraft 绑定，`profile` 当前用户档案，`notification` 当前用户通知 |
| 入服流程 | `/me/onboarding`、`/me/exams`、`/me/whitelist` | 是 | `onboarding` 当前用户流程，`exam` 当前用户考试，`whitelist` 当前用户申请 |
| 我的参与 | `/me/activity`、`/me/attendance`、`/me/resources` | 是 | `activity` 报名奖励，`attendance` 账户流水，`resource` 登录可见下载 |
| 管理后台 | `/admin`、`/admin/modules`、`/admin/todos`、`/admin/audit`、`/admin/settings` | `HELPER` 起 | `admin` 总览、模块、待办、指标、审计、设置和自检 |
| 业务管理页 | `/admin/:moduleKey`、`/admin/:moduleKey/*` | 按来源契约 | 来源模块后台接口，只展示当前角色可见能力 |
| 平台依赖 | `/admin/platform/api-gateway` | `HELPER` 起，日志需 `ADMIN` 起 | `api-gateway` 路由、上游健康、请求日志和自检 |
| 运维控制台 | `/ops`、`/ops/nodes`、`/ops/assets`、`/ops/tasks`、`/ops/approvals` | `HELPER` 起并按能力点 | `ops-control` 只读摘要和受控任务，不直连节点、不执行真实命令 |

## 后端 API 全量覆盖契约

前端适配必须维护一个可执行 API registry。registry 的来源是当前仓库所有 `docs/contracts-*.md`，排除 `docs/contracts-common.md` 和本文档本身。每个唯一的 `METHOD path` 都必须注册一次，当前解析到的唯一接口数量为 `746`。后续正式契约新增、删除或改路径时，registry 和测试必须同步变化。

| 模块 | 当前唯一接口数 | 前端适配入口 |
| --- | --- | --- |
| `activity` | 41 | `/activity`、`/me/activity`、`/admin/activity` |
| `admin` | 10 | `/admin` |
| `alerting` | 24 | `/admin/alerting`、`/ops` 摘要 |
| `api-gateway` | 8 | `/admin/platform/api-gateway` |
| `attendance` | 22 | `/me/attendance`、`/admin/attendance` |
| `auth` | 20 | `/login`、`/register`、`/me`、`/admin/auth` |
| `backup-recovery` | 25 | `/admin/backup-recovery`、`/ops` 摘要 |
| `calendar` | 22 | `/calendar`、`/admin/calendar` |
| `changelog` | 23 | `/changelog`、`/admin/changelog` |
| `cloudreve-sync` | 16 | `/admin/cloudreve-sync` |
| `community` | 62 | `/community`、`/admin/community` |
| `content` | 55 | `/`、`/content`、`/admin/content` |
| `cross-platform-notification` | 36 | `/admin/cross-platform-notification` |
| `exam` | 29 | `/me/exams`、`/admin/exam` |
| `guide` | 29 | `/guides`、`/admin/guide` |
| `material` | 33 | `/materials`、`/admin/material` |
| `node-daemon` | 17 | `/ops` 节点摘要，只读展示 |
| `notification` | 19 | `/me/notifications`、`/admin/notification` |
| `onboarding` | 15 | `/me/onboarding`、`/admin/onboarding` |
| `online-map` | 34 | `/map`、`/admin/online-map` |
| `ops-control` | 27 | `/ops` |
| `ops-image-market` | 48 | `/admin/ops-image-market`、`/ops/images` |
| `plugin-integration` | 38 | `/admin/plugin-integration` |
| `profile` | 16 | `/members`、`/me/profile`、`/admin/profile` |
| `resource` | 29 | `/resources`、`/admin/resource` |
| `server-status` | 25 | `/status`、`/admin/server-status` |
| `whitelist` | 23 | `/me/whitelist`、`/admin/whitelist` |

registry 每条记录必须包含 `moduleKey`、`method`、`pathTemplate`、`scope`、`authRequired`、`requiredRoles`、`requiredPermissions`、`riskLevel`、`pageRoute`、`uiSurface`、`degradeMode`、`sensitiveFieldPolicy` 和 `testCaseGroup`。公开接口 `scope=public`，当前用户接口 `scope=me`，后台接口 `scope=admin`，运维接口 `scope=ops`，平台接口 `scope=platform`，节点本地接口 `scope=node`。

registry 不是接口权限来源。它只用于页面映射、菜单裁剪、测试覆盖和开发期 API 浏览。真正权限仍以后端契约和后端响应为准。

## 请求和响应处理

API client 必须统一处理公共响应格式。`code=0` 才能进入成功渲染。字段级错误使用 `errors` 渲染到对应表单字段。`requestId` 必须在错误面板、调试摘要和后台审计入口中可见。

所有写接口必须携带结构化请求体。契约要求 `reason` 的后台写操作，前端必须提供原因输入。契约要求 `idempotencyKey` 的写操作，前端必须生成稳定且单次提交唯一的幂等键。契约要求 `confirmText` 的高风险操作，前端必须要求用户输入精确确认文本，不能只用普通确认框代替。

请求超时、网络失败、网关 `46000`、`46001`、`46200`、`46201`、`46204` 和上游 `5xx` 必须进入降级或错误状态。前端不得把请求失败的写操作显示为成功，不得隐藏失败请求编号。

## 认证和权限渲染

前端启动后如存在本地 token，应先调用 `GET /api/v1/auth/session/verify` 恢复会话。校验失败必须清空本地会话并进入未登录状态。当前用户信息必须来自 `auth`，不能从 token 字符串解析角色或能力点作为可信来源。

后台菜单、按钮和表单按 `roles`、`permissions`、`admin.modules` 和来源契约共同裁剪。隐藏按钮只是体验优化，不是安全边界。用户直接访问无权路由时，页面必须显示权限不足状态，并保留返回入口；不得继续发起高风险写请求。

`USER` 不能看到后台入口。`HELPER` 可以看到只读审核和协助入口，不能看到系统配置写、审计高权限写、运维真实执行入口。`ADMIN` 可以看到普通后台配置和审核入口。`OWNER` 可以看到高影响配置入口和运维审批入口。运维能力点如 `NODE_READ`、`NODE_WRITE`、`CONTAINER_OPERATE`、`VM_OPERATE`、`FILE_MANAGE`、`TERMINAL_ACCESS` 和 `HIGH_RISK_APPROVE` 必须按 `ops-control` 契约独立裁剪。

## 页面降级和空状态

公开首页、状态页、资源页、指南页、活动页和地图页必须支持局部降级。某个接口失败时，只影响对应区块。页面主体不能整页空白。降级区块必须展示来源模块、摘要原因和请求编号。

用户中心和后台页面必须区分空数据、权限不足、认证失败、资源不存在、状态冲突和服务降级。空数据使用正常空状态，不得显示为错误。认证失败引导登录。权限不足显示禁止访问。资源不存在显示不存在或已不可见。状态冲突显示后端返回的状态原因。服务降级显示局部不可用。

运维控制台默认只读。节点离线、node-daemon 未连接或操作处于 `SIMULATED` 时，页面必须显示模拟或离线摘要，不能把模拟结果写成真实执行成功。

## 安全和敏感字段

前端不得在页面、日志、错误面板、localStorage、sessionStorage 或测试快照中保存或展示完整 `Authorization`、邀请码原文、密码、密码重置令牌、Cloudreve 密码、外部 webhook secret、registry token、节点 token、服务器绝对路径、终端命令正文、文件内容、通知正文、后台备注全文、审计参数全文或异常堆栈。

API client、错误渲染、调试面板和 Playwright 测试都必须包含敏感字段扫描。发现敏感种子字段时测试失败。

## 审计触发和高风险操作

前端不写审计主数据。它必须按来源契约提供 `reason`、`idempotencyKey`、`confirmText` 和必要的业务字段，让后端完成审计。后端返回审计失败、配置失败、状态冲突或确认文本错误时，前端必须保持原页面状态并提示失败，不得乐观提交。

高风险操作包括软删除内容或资源、撤销奖励、关闭告警、重放插件事件、启用高风险插件路由、删除文件、终端命令、强制停止实例、恢复备份、修改节点密钥和隐藏关键后台模块。前端必须在 registry 中标注 `riskLevel=HIGH` 或 `CRITICAL`，并用来源契约的确认文本执行二次确认。

## 验收口径

`frontend-adaptation` 完成时必须满足以下条件：本文档存在于 `docs/contracts-frontend-adaptation.md`；本地测试文档存在于 `.local-docs/tests-frontend-adaptation.md` 且被 `.gitignore` 忽略；`frontend/beiming-web` 独立存在；API registry 覆盖当前所有正式契约的唯一 `METHOD path`；公开页、登录注册、会话恢复、权限菜单、用户中心、入服流程、资源下载、指南搜索、通知中心、管理后台、平台依赖和运维控制台都有自动化测试；每个后端 API 都通过 registry 覆盖测试映射到页面和测试组；前端构建通过；端到端测试覆盖桌面和移动端无重叠布局；`api-gateway`、`auth`、`content`、`server-status`、`resource`、`guide`、`admin` 和所有被首轮页面直接调用的后端服务回归测试通过；测试过程记录写入 `.local-docs/tests-frontend-adaptation.md`。

没有完成 API 文档、测试文档、自动化红灯验证、兼容开发、构建测试、端到端测试、后端回归、失败修复、最终全绿和测试过程记录前，不得声称 `frontend-adaptation` 开发完成。
