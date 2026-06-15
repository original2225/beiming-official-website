# 北冥官网系统设计文档

版本：0.4

## 总体方向

北冥官网当前以后端模块化单体和前后端分离为主线继续演进。当前唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，本地联调默认入口统一为 `http://127.0.0.1:8135`，业务路径保持 `/api/v1/**` 原样。

系统采用前后端分离设计。前端负责官网展示、用户中心、管理后台和运维控制台界面。后端按业务模块拆分边界，在同一个 Spring Boot 工程内统一编译、测试和运行。历史独立入口和端口只作为模块来源或回滚追溯引用保留，不再作为当前仓库的开发架构。

服务器运维能力采用控制面和节点守护进程分离。官网后台只发起授权后的管理请求，真实服务器上的容器、虚拟机、文件、日志和 Minecraft 实例操作由节点守护进程执行。

## 开发原则

认证模块必须独立成边界。统一后端入口负责统一路由、基础鉴权、请求日志和认证转发。资源模块必须和后台运维控制分开。外部节点执行器负责真实服务器上的系统资源、进程、容器、文件和日志操作，当前不属于本仓库源码。

账号模块必须覆盖注册、登录、当前用户、退出、会话校验、用户列表和用户修改。认证方案在实现前单独确认，最终必须兼容 `OWNER`、`ADMIN`、`HELPER`、`USER` 和细粒度能力点。

所有正式接口路径必须在模块实现前写入接口契约。未写入契约的路径、角色名、响应格式、数据库结构和服务依赖不得进入实现。

## 架构分层

```text
用户浏览器
  |
前端应用
  |-- 官网公开页
  |-- 用户中心
  |-- 管理后台
  |-- 运维控制台
  |
API 网关或后端入口
  |
业务模块层
  |-- auth            账号、登录、权限、邀请码
  |-- profile         成员档案、Minecraft 身份
  |-- content         公告、页面、作品、专题
  |-- community       帖子、评论、举报、工单
  |-- notification    站内通知
  |-- onboarding      入服引导
  |-- exam            入服考试
  |-- whitelist       白名单申请与审核
  |-- attendance      考勤积分与榜单
  |-- resource        玩家资源下载、Cloudreve 链接
  |-- server-status   玩家可见服务器状态和线路
  |-- activity        活动、报名、结果
  |-- calendar        日程、维护、工程节点
  |-- changelog       版本更新与维护记录
  |-- portal-core     玩家门户体验合并运行单元，承载 guide、material 和 online-map
  |-- online-map      在线地图接入控制面和公开展示快照，由 portal-core 承载
  |-- admin           后台聚合、配置、审计
  |-- ops-core        运维控制面合并运行单元，承载 ops-control、cloudreve-sync、backup-recovery、alerting、plugin-integration 和 ops-image-market
  |-- unified-backend 统一后端入口，当前由 backend/pom.xml 承载
  |-- ops-control     服务器与资源运维控制面
  |-- cloudreve-sync  Cloudreve API 深度接入与同步快照
  |-- backup-recovery 备份策略、备份点、校验、恢复演练和恢复申请
  |
基础设施层
  |-- 数据库
  |-- 缓存
  |-- 对象存储或本地上传目录
  |-- Cloudreve
  |-- Minecraft 服务器
  |-- 节点守护进程
  |-- 容器运行时
  |-- 虚拟化平台
```

## 模块边界

`auth` 负责身份认证、会话、角色、权限、邀请码和 Minecraft 身份绑定。其他模块不能自行实现登录逻辑。

`profile` 负责成员公开档案、成员组、成员状态、成员事迹、成员作品关联和公开展示字段。

`content` 负责官网公告、页面内容、摄影作品、成员作品、服务器进度、里程碑、专题和 SEO 配置。

`community` 负责论坛、帖子、评论、点赞、收藏、投票、举报、处罚和工单。后续可以按规模拆成 forum、ticket、report、vote。

`notification` 负责站内通知、通知模板、未读数、已读状态和通知归档。

`onboarding` 负责新玩家入服进度。它只保存流程状态，不直接判定考试分数和白名单结果。

`exam` 负责审核方向、题库、试卷、答题记录、自动判分、人工阅卷和二次考核规则。

`whitelist` 负责白名单申请、审核、通过、拒绝、补充、移除和重新申请。

`attendance` 负责考勤积分、贡献记录、积分流水、月度扣分、活跃度榜单和白名单移除触发。

`resource` 负责玩家可见资源下载、资源分类、版本、Cloudreve 分享链接、下载权限和资源状态。

`server-status` 负责玩家可见的 Minecraft 服务器状态、线路、延迟、在线人数、开服时长和历史快照。

`activity` 负责活动发布、报名、结果、奖励和活动贡献记录。

`calendar` 负责活动、维护、工程节点、投票截止、版本更新等时间线。

`changelog` 负责服务器版本、插件变更、规则调整、资源包更新、地图更新和维护记录。

`online-map` 负责在线地图接入控制面和公开展示快照。它管理地图 provider、公开地图入口、世界列表、图层、marker、区域、嵌入配置、健康快照和审计摘要，不负责真实渲染、真实瓦片代理或节点命令执行。

`portal-core` 是第五批运行合并单元，承载 `guide`、`material` 和 `online-map` 的现有业务路径。它只收敛运行入口，不改变三个模块的数据归属、正式契约、路径前缀、权限、状态机、错误码、审计对象或失败降级规则。`online-map` 仍只负责地图控制面和公开展示快照，不执行真实地图渲染、真实瓦片代理、真实世界目录读取或节点命令。`cross-platform-notification`、外部节点执行器、`api-gateway` 和 `ops-core` 不并入 `portal-core`。

`admin` 负责后台聚合入口、审核待办、运营配置、数据看板和操作日志。它不直接吞掉业务模块职责。

`ops-control` 负责后台服务器与资源运维控制面，包括节点、容器、虚拟机、Minecraft 实例、文件、日志、终端、监控、备份和高风险操作审批。

`cloudreve-sync` 负责 Cloudreve API 深度接入、provider 配置摘要、目录同步任务、文件元数据快照、分享链接解析、失效降级和同步审计。它不拥有玩家资源主数据，不执行后台服务器文件操作，只向后续 resource 兼容适配提供安全快照。

`backup-recovery` 负责备份域、备份策略、备份任务、备份点索引、备份校验、恢复演练、恢复申请、审批摘要、保留策略、依赖健康摘要和风险审计。它不执行真实数据库导出、真实文件复制、真实恢复写入或节点执行器调用；真实执行必须通过后续 `ops-control` 审批和独立外部节点执行器授权任务闭环。

`ops-core` 是第四批和第六期运行合并单元，承载 `ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration`、`ops-image-market` 和 `cross-platform-notification` 的现有业务路径。它只收敛运行入口，不改变七个模块的数据归属、正式契约、路径前缀、权限、状态机、错误码、审计对象或失败降级规则。`cross-platform-notification` 仍只负责外部渠道控制面和模拟投递，不执行真实外部消息发送、真实回调签名或生产凭据托管。

当前统一后端入口由 `backend:8135` 承载。它在同一 Spring Boot 工程内挂载 `api-gateway`、`business-core`、`admission-core`、`engagement-core`、`ops-core`、`portal-core` 和全部业务模块，当前必须保留 `/api/v1/gateway/**`、`/api/v1/business-core/**`、`/api/v1/admission-core/**`、`/api/v1/engagement-core/**`、`/api/v1/ops-core/**`、`/api/v1/portal-core/**`、`/api/v1/auth/**`、`/api/v1/profile/**`、`/api/v1/notifications/**`、`/api/v1/content/**`、`/api/v1/server-status/**`、`/api/v1/resources/**`、`/api/v1/admin/**`、`/api/v1/onboarding/**`、`/api/v1/exams/**`、`/api/v1/whitelist/**`、`/api/v1/attendance/**`、`/api/v1/community/**`、`/api/v1/activity/**`、`/api/v1/calendar/**`、`/api/v1/changelog/**`、`/api/v1/ops-control/**`、`/api/v1/cloudreve-sync/**`、`/api/v1/backup-recovery/**`、`/api/v1/alerting/**`、`/api/v1/plugin-integration/**`、`/api/v1/cross-platform-notification/**`、`/api/v1/ops-image-market/**`、`/api/v1/guides/**`、`/api/v1/materials/**` 和 `/api/v1/online-map/**` 原路径、原响应格式、原认证方式和原错误码。五个 core 模块源码已经物理位于 `backend/src/main/java`，由 `backend/pom.xml` 统一编译和测试，不再通过 build-helper 装配旧 core 源码目录。真实生产入口切流、真实集中配置 provider、持久化审计 sink、真实观测 smoke、回滚窗口完成和审批证据仍在仓库外阻塞。

统一后端 readiness 可以暴露生产入口、旧入口退役、外部入口和审计观测相关状态字段，但这些字段只代表仓库内运行态和外部证据接收状态。`apiGatewayControlledRetirementStatus=BLOCKED_BY_API_GATEWAY_RETIREMENT_RECEIPT_NOT_PROVIDED`、`apiGatewayExternalRetirementEvidenceStatus=BLOCKED_BY_EXTERNAL_API_GATEWAY_RETIREMENT_EVIDENCE_NOT_PROVIDED`、`realProductionEntrypointCutoverStatus=BLOCKED_BY_REAL_PRODUCTION_ENTRYPOINT_CUTOVER_EVIDENCE_NOT_PROVIDED` 和 `externalEntrypointCutoverEvidenceIntakeStatus=BLOCKED_BY_EXTERNAL_ENTRYPOINT_CUTOVER_EVIDENCE_NOT_PROVIDED` 必须继续引用 `EXTERNAL_EVIDENCE_REF:API_GATEWAY_RETIREMENT_RECEIPT`、`EXTERNAL_EVIDENCE_REF:API_GATEWAY_EXTERNAL_RETIREMENT`、`EXTERNAL_EVIDENCE_REF:REAL_PRODUCTION_ENTRYPOINT_CUTOVER` 和 `EXTERNAL_EVIDENCE_REF:EXTERNAL_ENTRYPOINT_CUTOVER_INTAKE`。真实切流、真实观测、真实审批和生产退役证据不写入仓库，`api-gateway-service` 只作为历史回滚引用，当前入口保持 `backend:8135`。没有仓库外证据时，`readyForProduction=false`、`readyToReplaceGateway=false`、`oldApiGatewayRetirementAllowed=false`、`readyToRetireBusinessCore=false`、`readyToRetireAdmissionCore=false`、`readyToRetireEngagementCore=false`、`readyToRetireOpsCore=false` 和 `readyToRetirePortalCore=false` 必须保持。

## 公共基础契约

所有模块共享统一响应格式、错误码、分页格式、认证方式、审计字段和时间字段。

内容类、审核类和资源类数据默认支持软删除、归档、状态流转和操作记录。常用状态包括草稿、待审核、已通过、已拒绝、需修改、已下架和已归档。

每个模块实现前必须补齐接口契约。接口契约至少包括路径、方法、认证要求、权限点、请求字段、响应字段、错误码、分页规则、幂等规则和失败降级行为。

跨模块调用必须明确超时、重试、兜底和审计策略。涉及通知、审核、积分、白名单和运维操作的调用，不能只依赖前端状态判断。

## 运维控制面设计

后台资源与服务器管理能力单独归入 `ops-control`，不要和玩家可见的 `resource`、`server-status` 混在一起。

`ops-control` 管理资源资产清单。资产类型包括物理服务器、云服务器、LXC 容器、Docker 容器、虚拟机、Minecraft 实例、Cloudreve 服务、反向代理、数据库、缓存、数据盘、备份盘、域名和线路。

每台被管理节点后续由独立外部节点执行器接入。节点执行器启动后向控制面注册，随后定时上报心跳、版本、能力、系统指标、容器运行时状态、Minecraft 实例状态和最近异常事件。该执行器不属于当前官网仓库，不参与 unified-backend 合并。

控制面不直接持有服务器系统密码。节点认证使用独立 token 或证书。节点 token 泄露、轮换、禁用和重新注册都必须有后台操作记录。

控制面与节点之间的调用必须有请求编号、超时、重试限制、结果回写和审计记录。节点离线时，控制面只能展示最后状态，不能假装操作成功。

## 节点守护进程能力

节点守护进程负责执行真实服务器上的受控操作。第一阶段建议优先支持健康检查、指标采集、容器列表、容器日志、Minecraft 实例列表、实例日志、实例启动停止、授权目录文件浏览和文本配置读取。

第二阶段扩展容器创建、镜像搜索与拉取、文件上传下载、WebSocket 日志流、终端命令、备份任务、快照任务和虚拟机控制。

节点守护进程必须限制操作根目录。所有文件路径需要规范化校验，禁止路径穿越。终端命令、删除、强制停止、恢复备份等操作必须由控制面传入已审计的授权请求。

## 容器、虚拟机和 Minecraft 实例

容器管理通过适配层对接 Docker、containerd 或 LXC。控制面统一展示容器名称、镜像、状态、端口、卷、环境变量、CPU、内存、网络、日志和生命周期操作。

虚拟机管理通过适配层对接实际平台。平台未确认前，系统只定义统一模型和接口，不把 Proxmox、LXD 或云厂商 API 写死进业务层。

Minecraft 实例管理通过适配层对接 MCSManager 或自研节点守护进程。实例模型需要包含实例名称、所在节点、目录、版本、启动命令、在线人数、运行状态、日志、控制台命令和最近启动停止记录。

## Cloudreve 与资源体系

玩家资源下载属于 `resource`。它面向官网展示，核心是资源分类、版本、下载链接、可见范围和下载权限。

Cloudreve 第一阶段可以作为外部分享链接存在。后续接入 API 时，通过资源适配层同步目录、文件、分享链接、过期时间、权限和下载统计。

后台文件管理属于 `ops-control`。它面向服务器运维，核心是授权目录下的文件浏览、上传、下载、编辑、重命名、移动和删除。两者不能共用同一套权限。

## 数据归属

每个模块拥有自己的数据。跨模块读取必须通过接口，不直接读库。高频展示可以保存快照字段，例如昵称、头像、Minecraft ID、资源名称和实例名称。

`auth` 拥有用户、会话、角色、权限和邀请码。

`profile` 拥有成员档案、成员组、成员状态和公开资料。

`resource` 拥有玩家资源、资源版本、下载规则和 Cloudreve 分享元数据。

`server-status` 拥有玩家可见线路、状态快照和历史在线数据。

`activity` 拥有活动、报名、结果、奖励和活动参与记录。

`calendar` 拥有日程事件、维护窗口、工程节点和投票截止时间。

`changelog` 拥有版本更新、插件变更、规则调整、资源包更新和维护记录。

`ops-control` 拥有节点、资产、容器、虚拟机、实例、文件操作记录、命令记录、指标快照、备份记录和高风险审批。

`admin` 拥有后台聚合配置、系统配置和审计索引，但不复制所有业务主数据。

`backup-recovery` 拥有备份域、备份策略、备份任务摘要、备份点索引、备份校验记录、恢复演练记录、恢复申请、审批摘要、恢复审计和幂等记录。

## 统一接口规范

普通成功响应使用统一结构。

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

分页响应统一放在 `data` 中。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [],
    "page": 1,
    "pageSize": 20,
    "total": 0
  }
}
```

认证请求统一使用 `Authorization: Bearer <token>`。后台和运维接口必须额外校验角色、能力点和操作风险等级。

## 核心链路

邀请码注册链路是前端提交注册信息，经后端入口进入 `auth`，校验邀请码、创建用户、创建会话，再返回登录状态。

入服链路是用户注册后进入 `onboarding`，绑定 Minecraft 身份，经 `exam` 完成考试，再由 `whitelist` 审核，审核通过后通知 `profile` 和 `attendance` 初始化成员档案与积分。

资源下载链路是前端请求 `resource`，服务返回可见资源、版本、说明和 Cloudreve 分享链接。Cloudreve 不可用时，官网应展示资源暂不可用，而不是影响登录和首页。

服务器状态链路是 `server-status` 周期性获取 Minecraft 状态和线路状态，前端读取缓存结果。它不执行启停和终端命令。

活动与日历链路是管理员在后台发布活动、维护窗口或工程节点，`activity`、`calendar` 和 `notification` 协作完成展示、提醒、报名和结果归档。

后台运维链路是管理员在运维控制台发起操作，`ops-control` 校验登录、角色、能力点、风险等级和二次确认，写入操作记录，再调用对应节点守护进程，节点执行后回写结果和日志摘要。

## 审计与风险控制

所有后台关键操作都要记录审计。审计字段至少包括操作者、角色、来源 IP、请求编号、目标类型、目标 ID、操作类型、操作原因、参数摘要、操作前状态、操作后状态、执行结果、失败原因和时间。

高风险操作必须二次确认。更高风险操作需要超级管理员审批。高风险操作包括删除容器、删除文件、恢复备份、删除备份、强制停止实例、执行终端命令、修改节点密钥、修改反向代理、修改数据盘挂载和批量变更权限。

审计日志不得允许普通管理员直接删除。确需归档时，应走备份和归档流程。

## 部署原则

官网前端、后端模块化单体、数据库、缓存、Cloudreve、Minecraft 服务器和外部节点执行器应分开部署。开发阶段以 `backend/pom.xml` 这一个 Spring Boot 工程模拟和验证后端模块边界，但接口、权限和数据归属不能混乱。本地开发态 `api-gateway-service:8125` Maven 入口已退役，五个 core 独立 Maven 入口也已退役，网关能力和五个 core 模块由 `backend:8135` 自承载。外部节点执行器已出仓且未接入。

节点守护进程部署在被管理服务器上。它只开放必要管理端口，优先由控制面主动连接或通过受控通道通信，不暴露无鉴权的系统操作接口。

统一后端入口负责路由、跨域、基础鉴权、可信身份签名、限流和请求日志。业务模块负责自己的业务规则。网关能力不直接访问业务数据库。

本地开发端口必须固定，避免 IDEA、命令行、前端代理和后续网关联调互相抢占默认端口。当前唯一后端 Maven 入口是 `backend/pom.xml`，当前本地后端服务端口是 `8135`，本地联调默认入口统一为 `http://127.0.0.1:8135`，业务路径保持 `/api/v1/**` 原样。`auth`、`profile`、`notification`、`content`、`server-status`、`resource` 和 `admin` 由统一后端装配的 `business-core` 模块承载；`onboarding`、`exam`、`whitelist` 和 `attendance` 由统一后端装配的 `admission-core` 模块承载；`community`、`activity`、`calendar` 和 `changelog` 由统一后端装配的 `engagement-core` 模块承载；`ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration`、`ops-image-market` 和 `cross-platform-notification` 由统一后端装配的 `ops-core` 模块承载；`guide`、`material` 和 `online-map` 由统一后端装配的 `portal-core` 模块承载。五个 core 模块源码已经物理位于 `backend/src/main/java`，由 `backend/pom.xml` 统一编译和测试，不再通过 build-helper 装配旧 core 源码目录。端口 `8130` 到 `8134` 只保留为五个 core 历史独立入口和模块来源记录，不再作为当前本地 Maven 启动入口。历史端口 `8101` 到 `8116`、`8118` 到 `8124`、`8126` 和 `8127` 只保留为模块原端口记录，不作为当前网关上游。外部节点执行器端口不在本仓库分配。旧 `backend/online-map-service` 已退役且不得恢复；第六期完成后旧 `backend/cross-platform-notification-service` 不得恢复。新增或调整端口时，必须同步更新正式文档和对应自动化测试。

## 技术选型原则

前端建议继续使用 React 和 TypeScript。管理后台可使用适合表格、表单、审核流和运维控制台的组件库。

后端语言和框架需要在开始实现 P0 前最终确认。账号、官网、社区等业务模块适合 Java 或 Node 系后端。节点守护进程适合 Go，因为它需要管理系统资源、进程、容器、文件和日志流。未确认的技术栈、数据库、网关、会话方式和服务拆分方式不能写死到实现里。

数据库建议优先使用关系型数据库保存业务主数据。缓存用于会话、状态快照、限流和短期热点数据。文件上传和素材存储可以先用本地目录或对象存储抽象，资源分发可以先接 Cloudreve 分享链接。

## 开发顺序

第一步完成正式需求、系统设计和项目规则。

第二步完成基础工程结构、统一响应、认证权限、成员档案、内容展示、资源展示和管理后台骨架。

第三步完成入服引导、考试、白名单、通知、工单举报和素材审核。

第四步完成考勤积分、榜单、社区、活动、日历和版本更新日志。

第五步完成服务器与资源运维控制面、节点守护进程、容器管理、Minecraft 实例管理、文件管理、日志流和审计闭环。

第六步扩展虚拟机管理、Cloudreve API、备份恢复、告警、在线地图、插件联动和跨平台通知。
