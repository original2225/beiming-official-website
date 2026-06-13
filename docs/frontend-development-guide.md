# 北冥官网前端开发说明

版本：0.1

## 文档定位

本文档是北冥官网前端开发的正式说明，用于开始 React 前端工程前统一技术栈、目录边界、接口入口、状态管理、响应式布局、公用组件、类型、常量和功能范围。

前端实现必须以 `docs/frontend-api-handbook.md`、`docs/api-reference.md` 和各模块 `docs/contracts-<module>.md` 为准。本文档不替代后端 API 契约，不重新定义接口字段，不允许前端把后端状态机写成自己的业务规则。

## 当前前置条件

仓库当前没有前端工程目录。正式开发前应在仓库根目录创建独立 `frontend/`，并保持后端服务目录不变。

前端只能通过后端正式接口读取业务数据。首页内容、服务器状态、资源下载、用户权限、白名单状态、考勤积分、后台待办、运维操作结果都不能写死在页面里。接口失败时按契约做局部降级，不能让整个页面空白。

## 技术栈

前端框架使用 React 和 TypeScript。构建工具建议使用 Vite。样式使用 Tailwind CSS。路由使用 React Router。接口请求统一封装在 API client 中。状态管理推荐 Zustand。

Zustand 更适合本项目第一版，因为登录态、当前用户、权限、通知未读数、布局状态和后台筛选条件都属于轻量客户端状态。服务端列表数据、详情数据和分页结果不建议长期塞进全局 store，应由页面 hooks 调接口并按页面生命周期管理。

如果后续列表缓存、后台表格刷新、乐观更新和请求去重变多，可以再引入 TanStack Query。第一版不强行引入，避免前端工程还没成型就堆依赖。

## 后端入口和端口

本地联调当前仍可走网关回滚入口，地址为 `http://127.0.0.1:8125`。第四十一轮生产审计和观测 smoke 演练下，后端单服务目标入口仍为 `http://127.0.0.1:8135`；`productionAuditObservabilitySmokeStatus=PASS_PRODUCTION_AUDIT_OBSERVABILITY_SMOKE_REHEARSAL_NOT_PRODUCTION` 只表示审计 sink 绑定、审计事件 schema、写入 smoke 引用、HTTP smoke、错误率、延迟、业务码、trace、dashboard、alert 和 rollback 引用已经被本地样板记录和测试守卫，不表示真实 audit sink、真实审计写入、真实观测平台、真实 dashboard、真实告警或真实 trace 管道已经接入。`productionRuntimeConfigShellStatus=PASS_PRODUCTION_RUNTIME_CONFIG_SHELL_REHEARSAL_NOT_PRODUCTION` 只表示生产 profile、集中配置 provider、敏感配置外置、部署入口和回滚配置这些接入槽位已经被本地样板记录和测试守卫，不表示真实生产 profile 已绑定、真实集中配置 provider 已连接或真实部署入口已应用。`productionExternalValueIntakeRehearsalStatus=PASS_EXTERNAL_VALUE_INTAKE_REHEARSAL_NOT_PRODUCTION` 只表示外部值保管类型、值组、值引用、注入目标、验证引用、回滚引用、审批引用和脱敏规则已经被本地样板记录和测试守卫，不表示真实外部值已经提供或应用。`productionCutoverEvidenceConsistencyAuditStatus=PASS_LOCAL_CUTOVER_EVIDENCE_CONSISTENCY_AUDIT_NOT_PRODUCTION` 只表示仓库内切流样板、集中配置样板、audit sink 样板、runbook、审批包和外部参数 manifest 的字段、命令、阻塞项和脱敏口径一致，不表示真实生产参数已经提供或生产流量已经切换。`productionCutoverExternalParameterManifestStatus=PASS_REDACTED_EXTERNAL_PARAMETER_MANIFEST_REHEARSAL_NOT_PRODUCTION` 只表示前端入口、代理 upstream、部署入口、集中配置、生产 profile、敏感配置外置、audit sink、观测字段、回滚授权和退役审批这些外部参数槽位已经被统一记录和脱敏校验，不表示真实外部参数已经提供。`productionCutoverApprovalPackageStatus=PASS_LOCAL_APPROVAL_PACKAGE_REHEARSAL_NOT_PRODUCTION` 只表示候选入口覆盖、本地外部入口演练、本地 file provider、本地 audit sink adapter、生产切换 runbook、外部参数清单、审批角色、go/no-go 矩阵、观测字段、回滚授权和退役门禁已经被统一记录和测试守卫，不表示真实前端、反向代理、部署入口或生产流量已经切到候选入口。`productionCentralConfigProviderStatus=PASS_LOCAL_FILE_PROVIDER_REHEARSAL_NOT_PRODUCTION` 只表示本地非密钥 file provider 样板已经完成加载、解析、校验、脱敏和映射验证，不表示真实 Nacos、Spring Cloud Config、Consul、Kubernetes ConfigMap、部署平台配置或生产 profile 已接入。`auditSinkAdapterRehearsalStatus=PASS_LOCAL_AUDIT_SINK_REHEARSAL_NOT_PRODUCTION` 只表示本地非生产 JSONL audit sink adapter 完成事件构造、写入 smoke、只读回放、导出摘要、保留策略记录和脱敏检查，不表示真实数据库、对象存储、日志平台、审计平台、SIEM 或生产 audit sink 已接入。只有存在可提交的前端、反向代理或部署入口配置时，才允许把 `VITE_API_BASE_URL` 或等价外部入口目标覆盖为 `http://127.0.0.1:8135`。业务路径保持原样，例如登录为 `POST /api/v1/auth/login`，不是 `/api/v1/gateway/auth/login`，也不是 `/api/v1/unified-backend/auth/login`。本仓库没有真实前端或外部代理配置，所以这里不声称生产流量已经切换。

第四十二轮受控生产入口切流收据门禁只新增后端 readiness 证据。`productionControlledCutoverStatus=BLOCKED_BY_REAL_CUTOVER_RECEIPT_NOT_PROVIDED` 时，前端默认仍使用网关回滚入口，不自动切 `VITE_API_BASE_URL`，也不改任何业务路径。这个状态可以在运维页展示为外部切流收据未提供，不能展示成生产切流完成。

第四十三轮 `api-gateway-service` 受控退役预检同样只新增后端 readiness 证据。`apiGatewayControlledRetirementStatus=BLOCKED_BY_API_GATEWAY_RETIREMENT_RECEIPT_NOT_PROVIDED` 时，运维页只能展示 `docs/unified-backend-api-gateway-retirement-receipt-sample.json` 对应的本地退役门禁、删除清单和回滚引用尚未获得真实生产收据，不得展示成 `api-gateway-service` 已退役。即使本地联调把 `VITE_API_BASE_URL` 指向 `http://127.0.0.1:8135`，也只是在访问 `unified-backend-service:8135` 候选入口，业务路径仍是 `POST /api/v1/auth/login` 这类 `/api/v1/**` 原路径，不改成统一后端专用路径。五个 core 仍保持 `readyToRetireBusinessCore=false`、`readyToRetireAdmissionCore=false`、`readyToRetireEngagementCore=false`、`readyToRetireOpsCore=false` 和 `readyToRetirePortalCore=false`，前端不得提供批量退役或删除提示。

单服务直连只用于排障，不应写在业务页面里。端口统一放在前端常量文件中，便于联调和排障页读取。

| 模块 | 端口 | 用途 |
| --- | ---: | --- |
| auth | 8130 | 登录、注册、当前用户、会话、密码、邀请码、角色、Minecraft 绑定 |
| profile | 8130 | 成员公开档案、当前用户档案、后台成员维护 |
| notification | 8130 | 站内通知、未读数、已读、归档、模板维护 |
| content | 8130 | 首页内容、公告文章、专题、SEO、分类标签 |
| server-status | 8130 | 玩家可见服务器状态、线路、历史快照 |
| resource | 8130 | 资源展示、资源版本、下载票据、Cloudreve 分享 |
| admin | 8130 | 后台首页、待办、配置、审计、数据看板 |
| onboarding | 8131 | 入服进度、步骤完成、规则确认 |
| exam | 8131 | 考试方向、题库、试卷、答题、阅卷 |
| whitelist | 8131 | 白名单申请、补充、撤回、审核、移除 |
| attendance | 8131 | 考勤积分、积分流水、榜单、月度任务 |
| community | 8132 | 板块、帖子、评论、点赞、收藏、投票、举报、工单、处罚 |
| activity | 8132 | 活动列表、报名、签到、结果、奖励 |
| calendar | 8132 | 日程、维护窗口、工程节点、提醒 |
| changelog | 8132 | 更新日志、维护日志、插件变更、规则调整 |
| ops-control | 8133 | 运维控制台控制面，由 `ops-core-service` 承载 |
| cloudreve-sync | 8133 | Cloudreve provider、目录同步、文件快照、分享解析，由 `ops-core-service` 承载 |
| backup-recovery | 8133 | 备份域、策略、任务、备份点、恢复申请，由 `ops-core-service` 承载 |
| alerting | 8133 | 告警规则、事件、静默、订阅，由 `ops-core-service` 承载 |
| online-map | 8134 | 在线地图 provider、世界、图层、marker、区域，由 `portal-core-service` 承载 |
| plugin-integration | 8133 | 插件源、实例、事件、命令、同步任务，由 `ops-core-service` 承载 |
| cross-platform-notification | 8133 | 跨平台通知渠道、模板、投递任务，由 `ops-core-service` 承载 |
| ops-image-market | 8133 | 运维镜像、仓库、版本、拉取任务，由 `ops-core-service` 承载 |
| api-gateway | 8125 | 回滚入口、路由表、上游健康、请求日志 |
| portal-core | 8134 | 玩家门户体验运行入口，承载 guide、material 和 online-map |
| material | 8134 | 素材投稿、素材展示、精选、审核、授权，由 `portal-core-service` 承载 |
| guide | 8134 | 指南、规则、指令、外部交流入口、反馈，由 `portal-core-service` 承载 |

## 前端功能范围

第一版前端应按页面域拆分，不按后端微服务硬塞菜单。推荐页面域为官网公开页、账号与用户中心、入服流程、社区与活动、后台管理、运维控制台。

官网公开页接入 `content`、`server-status`、`resource`、`guide`、`material`、`profile`、`activity`、`calendar`、`changelog` 和 `online-map`。重点页面包括首页、公告列表和详情、指南中心、资源中心、服务器状态、成员展示、素材精选、活动日历、更新日志和在线地图入口。

账号与用户中心接入 `auth`、`profile`、`notification`、`onboarding`、`whitelist`、`exam` 和 `attendance`。重点页面包括登录、注册、当前用户、账号安全、Minecraft 绑定、个人档案、通知中心、入服进度、考试记录、白名单申请和积分记录。

社区与活动接入 `community`、`activity`、`calendar` 和 `changelog`。重点页面包括板块列表、帖子列表、帖子详情、评论、举报入口、工单入口、活动列表、活动详情、报名状态和维护日程。

后台管理接入 `admin` 以及各业务模块的后台接口。重点页面包括后台概览、待办队列、用户管理、邀请码管理、内容管理、指南管理、素材审核、资源管理、活动管理、通知模板、社区审核、工单举报、白名单审核、考勤管理、日历维护、更新日志和审计记录。

运维控制台接入 `api-gateway`、`ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration`、`ops-image-market` 和 `server-status`。外部节点执行器未接入时，通过 `ops-control` 摘要展示 `EXTERNAL_EXECUTOR_NOT_CONNECTED`，前端不得直连执行器接口。重点页面包括网关健康、上游状态、节点列表、资产清单、容器、Minecraft 实例、文件、日志、终端入口、备份恢复、告警、插件联动和镜像市场。高风险操作必须显示二次确认和原因输入，失败时不能展示成功状态。

## 推荐目录结构

前端工程建议使用以下结构。

```text
frontend/
  src/
    app/
      router.tsx
      App.tsx
      providers.tsx
    api/
      client.ts
      endpoints.ts
      modules/
        auth.ts
        profile.ts
        notification.ts
        content.ts
        serverStatus.ts
        resource.ts
        admin.ts
        onboarding.ts
        exam.ts
        whitelist.ts
        attendance.ts
        community.ts
        activity.ts
        calendar.ts
        changelog.ts
        opsControl.ts
        cloudreveSync.ts
        backupRecovery.ts
        alerting.ts
        onlineMap.ts
        pluginIntegration.ts
        crossPlatformNotification.ts
        opsImageMarket.ts
        apiGateway.ts
        material.ts
        guide.ts
    components/
      common/
      layout/
      feedback/
      forms/
      data-display/
    constants/
      app.ts
      api.ts
      permissions.ts
      routes.ts
      status.ts
    hooks/
      useAuth.ts
      useCurrentUser.ts
      usePermissions.ts
      usePagination.ts
      useResponsive.ts
      useRequest.ts
    pages/
      public/
      account/
      onboarding/
      community/
      admin/
      ops/
    store/
      authStore.ts
      layoutStore.ts
      notificationStore.ts
    types/
      api.ts
      common.ts
      domain.ts
    utils/
      error.ts
      format.ts
      requestId.ts
```

`types/` 统一管理跨页面复用类型。`api/` 统一管理请求函数，不允许页面里直接拼 `fetch`。`constants/` 统一管理 API base URL、服务端口、路由、角色、能力点、状态枚举和错误码映射。`hooks/` 只放可复用逻辑，不放单页面一次性逻辑。`components/` 只放展示和交互组件，不直接写业务接口。

## 类型管理

`src/types/api.ts` 放统一响应、分页、错误结构和请求选项类型。`src/types/common.ts` 放角色、能力点、风险等级、通用状态、审计摘要和时间字段。`src/types/domain.ts` 放跨页面会复用的业务摘要类型，例如当前用户、成员摘要、通知摘要、内容摘要、资源摘要、服务健康摘要。

模块内部只在 API 文件附近定义本模块私有请求和响应类型。若同一个类型被两个以上页面使用，再移动到 `types/`。不要把 700 多个接口的所有类型先一次性塞进一个超大文件。

统一响应类型建议固定为以下形态。

```ts
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  requestId?: string;
  errors?: FieldError[];
}

export interface PageResult<T> {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
}

export interface FieldError {
  field: string;
  reason: string;
}
```

## API 管理

`src/api/client.ts` 负责 baseURL、JSON 请求、认证头、请求编号、统一响应解析和错误抛出。`src/api/endpoints.ts` 负责集中保存接口路径生成函数。`src/api/modules/*.ts` 按模块导出业务请求函数。

请求成功只以 `code === 0` 为准。HTTP 200 但 `code !== 0` 必须按业务失败处理。认证失败错误码 `41000` 到 `41003` 需要清理本地登录态。权限不足错误码 `42000` 到 `42002` 展示无权限状态。外部依赖错误 `46000` 和超时 `46001` 在公开页面做局部降级。

## 常量管理

`src/constants/api.ts` 保存默认网关地址、模块端口、接口前缀和请求超时。`src/constants/permissions.ts` 保存基础角色和运维能力点。`src/constants/status.ts` 保存通用状态、风险等级和前端展示文案。`src/constants/routes.ts` 保存前端路由路径和权限要求。

环境变量建议使用 `VITE_API_BASE_URL` 覆盖默认网关地址。没有配置时默认使用 `http://127.0.0.1:8125`。需要演练统一后端候选入口时，将该变量设置为 `http://127.0.0.1:8135`，并保持所有 `/api/v1/**` 业务路径不变。

## 状态管理

Zustand store 只保存客户端长期状态。`authStore` 保存 accessToken、当前用户摘要、登录状态和登出动作。`layoutStore` 保存侧边栏、主题偏好、移动端菜单状态。`notificationStore` 保存未读数和最近一次刷新时间。

业务列表、详情、搜索条件和表单草稿优先放页面局部状态或自定义 hook。后台表格筛选条件如果需要跨页面保留，可以单独拆 store，但不能把后端主数据复制成前端主数据。

## 响应式布局

所有页面必须使用响应式布局。公开页至少覆盖手机、平板、桌面和宽屏。后台和运维控制台在手机端可以降级为可读可操作的单列结构，但不能出现文字溢出、按钮重叠、表格撑破页面。

Tailwind 断点按默认体系使用，优先使用 `sm`、`md`、`lg`、`xl`。页面主体使用弹性容器、网格和最大宽度约束。固定格式组件，例如状态卡、工具栏、表格操作区、统计面板和控制台按钮组，需要设置稳定尺寸和换行规则。

后台表格在窄屏应切换为卡片列表或横向滚动容器。高风险操作确认弹窗在移动端必须完整可读，确认按钮不能被遮挡。

## 公用组件

第一版至少提取以下公用组件：页面布局、公开页导航、后台侧边栏、权限保护路由、加载状态、错误状态、空状态、分页器、状态徽标、风险徽标、确认弹窗、表单字段、搜索筛选栏、数据表格、详情面板、审计摘要、时间显示、请求失败提示。

组件只接收明确 props，不直接读取全局 store，除非它本身就是登录入口、权限路由或应用布局。组件不直接调用 API，页面或 hook 负责数据获取。

## 可复用 hooks

`useAuth` 负责登录态和登出。`useCurrentUser` 负责读取当前用户。`usePermissions` 负责角色和能力点判断。`useRequest` 负责页面级请求状态。`usePagination` 负责分页参数。`useResponsive` 负责断点判断。`useDebouncedValue` 可用于搜索输入。`useConfirmAction` 可用于后台和运维写操作确认。

高风险操作必须通过统一 hook 或组件收集 `reason`、确认文本和幂等键。不要在每个页面各写一套确认逻辑。

## 路由和权限

公开页面无需登录。用户中心需要登录。后台入口至少要求 `HELPER`、`ADMIN` 或 `OWNER`。普通后台写操作通常要求 `ADMIN` 或 `OWNER`。运维控制台除基础角色外，还需要细粒度能力点，例如 `NODE_READ`、`NODE_WRITE`、`CONTAINER_OPERATE`、`FILE_MANAGE`、`TERMINAL_ACCESS`、`HIGH_RISK_APPROVE`。

前端权限只用于隐藏入口和改善体验。最终权限以服务端返回为准。服务端返回无权限时，前端展示无权限状态，不重试，不伪造成功。

## 错误和降级

公开页读取失败时只降级相关区块。例如服务器状态失败时，只显示服务器状态暂不可用，首页其他内容继续展示。资源接口失败时，资源区显示暂不可用，不影响公告和指南。后台写操作失败时必须保留失败提示和 `requestId`。

表单字段错误使用 `errors` 做字段级提示。资源不存在使用不存在或已下架页面。请求过快时按钮短暂冷却。高风险确认缺失时停留在确认流程。

## 测试要求

前端开发也要遵守项目闭环。开始编码前应补充 `.local-docs/tests-frontend.md`，记录页面范围、接口依赖、响应式断点、权限路由、错误降级和自动化测试命令。该文件不得提交。

自动化测试建议覆盖 API client、权限判断、状态 store、核心 hooks、路由守卫、公开页降级和后台高风险确认。页面级验证应覆盖移动端和桌面端。完成后记录测试时间、命令、结果、失败原因、修复动作和复测结果。

## 开发顺序

前端建议按工程骨架、API client 和类型、认证登录、公开首页、公开内容页、用户中心、入服流程、社区活动、后台骨架、业务后台、运维控制台的顺序推进。

当前后端契约已经覆盖 27 个模块和 746 个唯一接口，`guide`、`material` 和 `online-map` 当前都由 `portal-core-service:8134` 承载。前端第一轮不应试图一次实现全部后台和运维页面。更稳的做法是先完成工程骨架、统一 API 层、统一类型、统一错误处理、权限路由和公开页核心链路，再分域推进。

## 验收口径

前端工程完成一个阶段时，必须满足 TypeScript 无类型错误、构建通过、核心自动化测试通过、公开页接口失败可局部降级、登录态和权限路由按契约工作、移动端和桌面端布局可用、API base URL 和模块端口集中管理、页面中没有硬编码业务数据和后端单服务端口。

任何接口缺口都应回到对应 `docs/contracts-<module>.md` 补齐契约，再更新本地测试文档和自动化测试。前端不能先吞业务。
