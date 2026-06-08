# 北冥官网前端接口交付文档

版本：0.1

## 给前端工程师先看

这份文档是前端开发入口文档。开发页面、封装 API client、做权限路由、做错误态和降级态时，先看这里。字段明细、状态流转、幂等、审计和模块验收仍以 `docs/api-reference.md` 与各模块 `docs/contracts-<module>.md` 为准。

本手册按各模块 `接口总览` 抽取前端可查接口条目。网关业务转发 `GET/POST/PUT/PATCH/DELETE/OPTIONS /api/v1/{module}/**` 是通配规则，单独写在网关说明里，不重复进普通接口表。

## 请求入口

本地联调默认走网关：`http://127.0.0.1:8125`。第十七轮开始，统一后端候选入口提供入口切换适配证据，后续前端或代理联调可以把 API base URL 覆盖为 `http://127.0.0.1:8135`。所有业务路径保持 `/api/v1/...` 原样，例如登录请求是 `POST /api/v1/auth/login`，不是 `/api/v1/gateway/auth/login`，也不是 `/api/v1/unified-backend/auth/login`。

单服务直连只用于排查问题。服务端口见本文后面的模块表。前端代码不要把单服务端口写死进业务页面，统一通过 API client 的 baseURL 管理。

## 统一请求规则

普通 JSON 请求使用 `Content-Type: application/json`。登录后接口统一携带 `Authorization: Bearer <token>`。可选传 `X-Request-Id`，不传时后端会生成。高风险后台或运维操作按接口契约额外传二次确认凭据、审批编号、`reason` 和 `idempotencyKey`。

## 统一响应

成功响应固定看 `code`，`code=0` 才表示业务成功。HTTP 200 但 `code` 非 0 时仍按业务失败处理。

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

分页响应统一在 `data` 内返回 `items`、`page`、`pageSize` 和 `total`。

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

错误响应统一包含 `code`、`message`、`data: null` 和 `requestId`，字段校验错误可能带 `errors`。前端展示用户可理解的提示时优先映射 `code`，排障时带上 `requestId`。

## 通用错误处理

| 错误码 | 前端处理建议 |
| --- | --- |
| `40001` | 表单字段错误，展示字段级提示。 |
| `41000`、`41001`、`41002`、`41003` | 登录失效或未登录，清理本地会话并引导登录。 |
| `42000`、`42001`、`42002` | 权限不足，隐藏不可用入口并展示无权限状态。 |
| `42003`、`42004` | 高风险操作缺少二次确认或审批，引导完成确认流程。 |
| `43000` | 资源不存在，详情页展示不存在或已下架。 |
| `43001`、`43002` | 状态冲突或幂等冲突，刷新数据后提示用户重试。 |
| `44000` | 请求过快，按钮进入短暂冷却。 |
| `46000`、`46001` | 外部依赖不可用或超时，公开页做局部降级，后台页展示服务异常。 |
| `50000` 和 `51000-59999` | 服务端错误，保留 `requestId` 方便后端查日志。 |

## 权限和角色

基础角色为 `OWNER`、`ADMIN`、`HELPER`、`USER`。普通用户不能进后台。`HELPER` 只能处理被授权的审核和协助任务。`ADMIN` 管理业务后台。`OWNER` 管理最高权限、系统配置和高风险授权。运维能力不等同于后台登录，涉及节点、容器、文件、终端和高风险审批时还要检查 `NODE_READ`、`NODE_WRITE`、`CONTAINER_OPERATE`、`VM_OPERATE`、`FILE_MANAGE`、`TERMINAL_ACCESS`、`HIGH_RISK_APPROVE`。

## 页面接入地图

### 账号与用户中心

`auth`、`profile`、`notification`

### 官网公开展示

`content`、`server-status`、`resource`、`guide`、`material`、`online-map`

### 入服流程

`onboarding`、`exam`、`whitelist`、`attendance`

### 社区运营

`community`、`activity`、`calendar`、`changelog`

### 后台管理

`admin`、`content`、`guide`、`material`、`resource`、`community`、`activity`、`calendar`、`changelog`、`notification`、`cross-platform-notification`

### 运维控制台

`api-gateway`、`ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration`、`ops-image-market`、`server-status`

## 前端禁止写死的内容

首页内容、公告、服务器状态、资源下载链接、用户权限、审核状态、白名单结果、考勤积分、后台待办、运维操作结果都必须来自接口。公开页接口失败时做局部降级，不能整页空白。后台和运维写操作失败时不能在前端假装成功。

## 模块速查

| 模块 | 端口 | 前端用途 | 契约 |
| --- | ---: | --- | --- |
| `auth` | 8130 | 登录、注册、当前用户、会话、密码、邀请码、角色和 Minecraft 账号绑定。 | `docs/contracts-auth.md` |
| `profile` | 8130 | 成员公开档案、成员详情、当前用户档案和后台成员维护。 | `docs/contracts-profile.md` |
| `notification` | 8130 | 站内通知、未读数、已读、归档和通知模板后台维护。 | `docs/contracts-notification.md` |
| `content` | 8130 | 官网首页、公告文章、专题、SEO、分类标签和首页配置后台。 | `docs/contracts-content.md` |
| `server-status` | 8130 | 玩家可见服务器状态、线路、历史快照和后台线路配置。 | `docs/contracts-server-status.md` |
| `resource` | 8130 | 公开资源、资源版本、下载票据、Cloudreve 分享和资源后台管理。 | `docs/contracts-resource.md` |
| `admin` | 8130 | 后台首页、待办、配置、审计、数据看板和系统摘要。 | `docs/contracts-admin.md` |
| `onboarding` | 8131 | 入服流程进度、步骤完成、规则确认和后台流程配置。 | `docs/contracts-onboarding.md` |
| `exam` | 8131 | 考试方向、题库、试卷、答题、阅卷和考试后台管理。 | `docs/contracts-exam.md` |
| `whitelist` | 8131 | 白名单申请、补充、撤回、审核、移除和重新申请。 | `docs/contracts-whitelist.md` |
| `attendance` | 8131 | 考勤积分、积分流水、榜单、月度任务和后台调整。 | `docs/contracts-attendance.md` |
| `community` | 8132 | 板块、帖子、评论、点赞、收藏、投票、举报、工单和处罚。 | `docs/contracts-community.md` |
| `activity` | 8132 | 活动列表、报名、签到、结果、奖励和活动后台管理。 | `docs/contracts-activity.md` |
| `calendar` | 8132 | 公开日程、维护窗口、工程节点、提醒和日历后台维护。 | `docs/contracts-calendar.md` |
| `changelog` | 8132 | 版本更新、维护日志、插件变更、规则调整和后台发布。 | `docs/contracts-changelog.md` |
| `ops-control` | 8133 | 运维控制台的节点、资产、容器、实例、文件、日志、终端和审批控制面，由 `ops-core-service` 承载。 | `docs/contracts-ops-control.md` |
| `cloudreve-sync` | 8133 | Cloudreve provider、目录同步、文件快照、分享解析和同步审计，由 `ops-core-service` 承载。 | `docs/contracts-cloudreve-sync.md` |
| `backup-recovery` | 8133 | 备份域、策略、任务、备份点、校验、演练、恢复申请和审批摘要，由 `ops-core-service` 承载。 | `docs/contracts-backup-recovery.md` |
| `alerting` | 8133 | 告警规则、事件、静默、订阅、通知演练和告警后台管理，由 `ops-core-service` 承载。 | `docs/contracts-alerting.md` |
| `online-map` | 8134 | 在线地图 provider、世界、图层、marker、区域、公开入口和后台维护，由 `portal-core-service` 承载。 | `docs/contracts-online-map.md` |
| `plugin-integration` | 8133 | 插件源、实例、事件、命令、同步任务和插件联动后台，由 `ops-core-service` 承载。 | `docs/contracts-plugin-integration.md` |
| `cross-platform-notification` | 8133 | 跨平台通知渠道、模板、投递任务、回执和后台演练，由 `ops-core-service` 承载。 | `docs/contracts-cross-platform-notification.md` |
| `ops-image-market` | 8133 | 运维镜像、仓库、版本、拉取任务、漏洞摘要和镜像后台管理，由 `ops-core-service` 承载。 | `docs/contracts-ops-image-market.md` |
| `api-gateway` | 8125 | 统一入口、路由表、上游健康、请求日志和业务请求转发。 | `docs/contracts-api-gateway.md` |
| `material` | 8134 | 素材投稿、素材展示、精选、审核、授权、文件摘要和素材后台管理，由 `portal-core-service` 承载。 | `docs/contracts-material.md` |
| `guide` | 8134 | 指南、规则、指令、外部交流入口、反馈和指南后台维护，由 `portal-core-service` 承载。 | `docs/contracts-guide.md` |

## 完整接口清单

表里的认证为“否”表示公开接口，“是”表示需要 `Authorization: Bearer <token>`。字段细节、请求体、响应体和状态流转请跳到来源契约。

### auth

用途：登录、注册、当前用户、会话、密码、邀请码、角色和 Minecraft 账号绑定。

端口：`8101`。来源：`docs/contracts-auth.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 注册 | POST | `/api/v1/auth/register` | 否 | 无 | LOW |
| 登录 | POST | `/api/v1/auth/login` | 否 | 无 | LOW |
| 退出登录 | POST | `/api/v1/auth/logout` | 是 | 当前用户 | LOW |
| 当前用户 | GET | `/api/v1/auth/me` | 是 | 当前用户 | LOW |
| 会话校验 | GET | `/api/v1/auth/session/verify` | 是 | 当前用户 | LOW |
| 当前用户会话列表 | GET | `/api/v1/auth/me/sessions` | 是 | 当前用户 | LOW |
| 吊销当前用户指定会话 | DELETE | `/api/v1/auth/me/sessions/{sessionId}` | 是 | 当前用户 | MEDIUM |
| 修改当前用户密码 | POST | `/api/v1/auth/me/password` | 是 | 当前用户 | MEDIUM |
| 申请密码重置 | POST | `/api/v1/auth/password-reset/request` | 否 | 无 | LOW |
| 确认密码重置 | POST | `/api/v1/auth/password-reset/confirm` | 否 | 无 | MEDIUM |
| 绑定 Minecraft 身份 | PUT | `/api/v1/auth/me/minecraft-binding` | 是 | 当前用户 | MEDIUM |
| 解绑 Minecraft 身份 | DELETE | `/api/v1/auth/me/minecraft-binding` | 是 | 当前用户 | MEDIUM |
| 用户列表 | GET | `/api/v1/auth/admin/users` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 用户详情 | GET | `/api/v1/auth/admin/users/{userId}` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 修改用户基础信息和状态 | PATCH | `/api/v1/auth/admin/users/{userId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改角色和能力点 | PUT | `/api/v1/auth/admin/users/{userId}/roles` | 是 | `OWNER` | MEDIUM |
| 邀请码列表 | GET | `/api/v1/auth/admin/invitations` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 创建邀请码 | POST | `/api/v1/auth/admin/invitations` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 禁用邀请码 | PATCH | `/api/v1/auth/admin/invitations/{invitationId}/disable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 邀请码使用记录 | GET | `/api/v1/auth/admin/invitations/{invitationId}/usage-records` | 是 | `ADMIN` 或 `OWNER` | LOW |

### profile

用途：成员公开档案、成员详情、当前用户档案和后台成员维护。

端口：`8102`。来源：`docs/contracts-profile.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开成员列表 | GET | `/api/v1/profile/members` | 否 | 无 | LOW |
| 公开成员详情 | GET | `/api/v1/profile/members/{memberId}` | 否 | 无 | LOW |
| 当前用户成员档案 | GET | `/api/v1/profile/me` | 是 | 当前用户 | LOW |
| 当前用户维护公开资料 | PATCH | `/api/v1/profile/me` | 是 | 当前用户 | MEDIUM |
| 后台成员列表 | GET | `/api/v1/profile/admin/members` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台成员详情 | GET | `/api/v1/profile/admin/members/{memberId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建或激活成员档案 | POST | `/api/v1/profile/admin/members/activate` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台修改成员档案 | PATCH | `/api/v1/profile/admin/members/{memberId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改成员状态 | PATCH | `/api/v1/profile/admin/members/{memberId}/status` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 成员组列表 | GET | `/api/v1/profile/admin/groups` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建成员组 | POST | `/api/v1/profile/admin/groups` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改成员组 | PATCH | `/api/v1/profile/admin/groups/{groupId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档成员组 | PATCH | `/api/v1/profile/admin/groups/{groupId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 维护成员事迹 | PUT | `/api/v1/profile/admin/members/{memberId}/milestones` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 维护代表作品快照 | PUT | `/api/v1/profile/admin/members/{memberId}/work-snapshots` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 成员审计列表 | GET | `/api/v1/profile/admin/members/{memberId}/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

### notification

用途：站内通知、未读数、已读、归档和通知模板后台维护。

端口：`8103`。来源：`docs/contracts-notification.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 当前用户通知列表 | GET | `/api/v1/notifications/me` | 是 | 当前用户 | LOW |
| 当前用户未读数 | GET | `/api/v1/notifications/me/unread-count` | 是 | 当前用户 | LOW |
| 当前用户通知详情 | GET | `/api/v1/notifications/me/{notificationId}` | 是 | 当前用户 | LOW |
| 标记单条已读 | PATCH | `/api/v1/notifications/me/{notificationId}/read` | 是 | 当前用户 | LOW |
| 全部标记已读 | PATCH | `/api/v1/notifications/me/read-all` | 是 | 当前用户 | LOW |
| 归档单条通知 | PATCH | `/api/v1/notifications/me/{notificationId}/archive` | 是 | 当前用户 | LOW |
| 后台通知列表 | GET | `/api/v1/notifications/admin/messages` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台通知详情 | GET | `/api/v1/notifications/admin/messages/{notificationId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台创建站内通知 | POST | `/api/v1/notifications/admin/messages` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台按模板创建通知 | POST | `/api/v1/notifications/admin/messages/from-template` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 模板列表 | GET | `/api/v1/notifications/admin/templates` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 模板详情 | GET | `/api/v1/notifications/admin/templates/{templateId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 模板预览 | POST | `/api/v1/notifications/admin/templates/preview` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建模板 | POST | `/api/v1/notifications/admin/templates` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改模板 | PATCH | `/api/v1/notifications/admin/templates/{templateId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 禁用模板 | PATCH | `/api/v1/notifications/admin/templates/{templateId}/disable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 启用模板 | PATCH | `/api/v1/notifications/admin/templates/{templateId}/enable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 通知审计列表 | GET | `/api/v1/notifications/admin/messages/{notificationId}/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| notification 自检摘要 | GET | `/api/v1/notifications/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

### content

用途：官网首页、公告文章、专题、SEO、分类标签和首页配置后台。

端口：`8104`。来源：`docs/contracts-content.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开首页内容 | GET | `/api/v1/content/home` | 否 | 无 | LOW |
| 公开内容列表 | GET | `/api/v1/content/items` | 否 | 无 | LOW |
| 公开内容详情 | GET | `/api/v1/content/items/{contentId}` | 否 | 无 | LOW |
| 公开 slug 内容详情 | GET | `/api/v1/content/items/by-slug/{slug}` | 否 | 无 | LOW |
| 内容令牌预览 | GET | `/api/v1/content/items/{contentId}/preview` | 否 | 预览令牌 | LOW |
| 公开分类列表 | GET | `/api/v1/content/categories` | 否 | 无 | LOW |
| 公开标签列表 | GET | `/api/v1/content/tags` | 否 | 无 | LOW |
| 公开专题列表 | GET | `/api/v1/content/topics` | 否 | 无 | LOW |
| 公开专题详情 | GET | `/api/v1/content/topics/{topicId}` | 否 | 无 | LOW |
| 公开 slug 专题详情 | GET | `/api/v1/content/topics/by-slug/{slug}` | 否 | 无 | LOW |
| 公开 SEO 配置 | GET | `/api/v1/content/seo` | 否 | 无 | LOW |
| 公开站点地图 | GET | `/api/v1/content/seo/sitemap` | 否 | 无 | LOW |
| 后台内容列表 | GET | `/api/v1/content/admin/items` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台内容详情 | GET | `/api/v1/content/admin/items/{contentId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建内容 | POST | `/api/v1/content/admin/items` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改内容 | PATCH | `/api/v1/content/admin/items/{contentId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 创建内容预览令牌 | POST | `/api/v1/content/admin/items/{contentId}/preview-token` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 提交审核 | PATCH | `/api/v1/content/admin/items/{contentId}/submit-review` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核通过 | PATCH | `/api/v1/content/admin/items/{contentId}/approve` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/content/admin/items/{contentId}/reject` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 要求修改 | PATCH | `/api/v1/content/admin/items/{contentId}/request-changes` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 发布内容 | PATCH | `/api/v1/content/admin/items/{contentId}/publish` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 下架内容 | PATCH | `/api/v1/content/admin/items/{contentId}/offline` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档内容 | PATCH | `/api/v1/content/admin/items/{contentId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 软删除内容 | PATCH | `/api/v1/content/admin/items/{contentId}/delete` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 内容版本列表 | GET | `/api/v1/content/admin/items/{contentId}/versions` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 内容版本详情 | GET | `/api/v1/content/admin/items/{contentId}/versions/{version}` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 恢复内容版本 | PATCH | `/api/v1/content/admin/items/{contentId}/versions/{version}/restore` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 内容审计列表 | GET | `/api/v1/content/admin/items/{contentId}/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 后台首页配置详情 | GET | `/api/v1/content/admin/home` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 保存首页草稿配置 | PUT | `/api/v1/content/admin/home` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 首页配置预览 | POST | `/api/v1/content/admin/home/preview` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 发布首页配置 | PATCH | `/api/v1/content/admin/home/publish` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 回滚首页配置 | PATCH | `/api/v1/content/admin/home/rollback` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台分类列表 | GET | `/api/v1/content/admin/categories` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建分类 | POST | `/api/v1/content/admin/categories` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改分类 | PATCH | `/api/v1/content/admin/categories/{categoryId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档分类 | PATCH | `/api/v1/content/admin/categories/{categoryId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台标签列表 | GET | `/api/v1/content/admin/tags` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建标签 | POST | `/api/v1/content/admin/tags` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改标签 | PATCH | `/api/v1/content/admin/tags/{tagId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档标签 | PATCH | `/api/v1/content/admin/tags/{tagId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台专题列表 | GET | `/api/v1/content/admin/topics` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台专题详情 | GET | `/api/v1/content/admin/topics/{topicId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建专题 | POST | `/api/v1/content/admin/topics` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改专题 | PATCH | `/api/v1/content/admin/topics/{topicId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 发布专题 | PATCH | `/api/v1/content/admin/topics/{topicId}/publish` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 下架专题 | PATCH | `/api/v1/content/admin/topics/{topicId}/offline` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档专题 | PATCH | `/api/v1/content/admin/topics/{topicId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 软删除专题 | PATCH | `/api/v1/content/admin/topics/{topicId}/delete` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台 SEO 列表 | GET | `/api/v1/content/admin/seo` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台 SEO 详情 | GET | `/api/v1/content/admin/seo/{seoId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 保存路由 SEO | PUT | `/api/v1/content/admin/seo/by-route` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 禁用路由 SEO | PATCH | `/api/v1/content/admin/seo/{seoId}/disable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| content 自检摘要 | GET | `/api/v1/content/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

### server-status

用途：玩家可见服务器状态、线路、历史快照和后台线路配置。

端口：`8105`。来源：`docs/contracts-server-status.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 状态总览 | GET | `/api/v1/server-status/overview` | 否 | 无 | LOW |
| 公开实例列表 | GET | `/api/v1/server-status/instances` | 否 | 无 | LOW |
| 公开实例详情 | GET | `/api/v1/server-status/instances/{instanceId}` | 否 | 无 | LOW |
| 公开线路列表 | GET | `/api/v1/server-status/lines` | 否 | 无 | LOW |
| 历史快照 | GET | `/api/v1/server-status/history/snapshots` | 否 | 无 | LOW |
| 公开宕机记录 | GET | `/api/v1/server-status/outages` | 否 | 无 | LOW |
| 状态源列表 | GET | `/api/v1/server-status/admin/sources` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建状态源 | POST | `/api/v1/server-status/admin/sources` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改状态源 | PATCH | `/api/v1/server-status/admin/sources/{sourceId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 禁用状态源 | PATCH | `/api/v1/server-status/admin/sources/{sourceId}/disable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 启用状态源 | PATCH | `/api/v1/server-status/admin/sources/{sourceId}/enable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 手动刷新 | POST | `/api/v1/server-status/admin/sources/{sourceId}/refresh` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台线路列表 | GET | `/api/v1/server-status/admin/lines` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建线路 | POST | `/api/v1/server-status/admin/lines` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改线路 | PATCH | `/api/v1/server-status/admin/lines/{lineId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 禁用线路 | PATCH | `/api/v1/server-status/admin/lines/{lineId}/disable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 启用线路 | PATCH | `/api/v1/server-status/admin/lines/{lineId}/enable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台宕机列表 | GET | `/api/v1/server-status/admin/outages` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建宕机记录 | POST | `/api/v1/server-status/admin/outages` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改宕机记录 | PATCH | `/api/v1/server-status/admin/outages/{outageId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 确认宕机记录 | PATCH | `/api/v1/server-status/admin/outages/{outageId}/acknowledge` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 解决宕机记录 | PATCH | `/api/v1/server-status/admin/outages/{outageId}/resolve` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档宕机记录 | PATCH | `/api/v1/server-status/admin/outages/{outageId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审计列表 | GET | `/api/v1/server-status/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 自检摘要 | GET | `/api/v1/server-status/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

### resource

用途：公开资源、资源版本、下载票据、Cloudreve 分享和资源后台管理。

端口：`8106`。来源：`docs/contracts-resource.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开资源列表 | GET | `/api/v1/resources` | 否 | 无 | LOW |
| 公开资源详情 | GET | `/api/v1/resources/{resourceId}` | 否 | 无 | LOW |
| 公开 slug 资源详情 | GET | `/api/v1/resources/by-slug/{slug}` | 否 | 无 | LOW |
| 公开分类列表 | GET | `/api/v1/resources/categories` | 否 | 无 | LOW |
| 公开资源版本列表 | GET | `/api/v1/resources/{resourceId}/versions` | 否 | 无 | LOW |
| 下载入口解析 | POST | `/api/v1/resources/{resourceId}/versions/{versionId}/download` | 视可见范围 | 当前用户或公开 | LOW |
| 后台资源列表 | GET | `/api/v1/resources/admin/items` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台资源详情 | GET | `/api/v1/resources/admin/items/{resourceId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建资源 | POST | `/api/v1/resources/admin/items` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改资源 | PATCH | `/api/v1/resources/admin/items/{resourceId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 提交审核 | PATCH | `/api/v1/resources/admin/items/{resourceId}/submit-review` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核通过 | PATCH | `/api/v1/resources/admin/items/{resourceId}/approve` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/resources/admin/items/{resourceId}/reject` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 要求修改 | PATCH | `/api/v1/resources/admin/items/{resourceId}/request-changes` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 发布资源 | PATCH | `/api/v1/resources/admin/items/{resourceId}/publish` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 下架资源 | PATCH | `/api/v1/resources/admin/items/{resourceId}/offline` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档资源 | PATCH | `/api/v1/resources/admin/items/{resourceId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 软删除资源 | PATCH | `/api/v1/resources/admin/items/{resourceId}/delete` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台版本列表 | GET | `/api/v1/resources/admin/items/{resourceId}/versions` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建版本 | POST | `/api/v1/resources/admin/items/{resourceId}/versions` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改版本 | PATCH | `/api/v1/resources/admin/items/{resourceId}/versions/{versionId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 禁用版本 | PATCH | `/api/v1/resources/admin/items/{resourceId}/versions/{versionId}/disable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 启用版本 | PATCH | `/api/v1/resources/admin/items/{resourceId}/versions/{versionId}/enable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台分类列表 | GET | `/api/v1/resources/admin/categories` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建分类 | POST | `/api/v1/resources/admin/categories` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改分类 | PATCH | `/api/v1/resources/admin/categories/{categoryId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档分类 | PATCH | `/api/v1/resources/admin/categories/{categoryId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 资源审计列表 | GET | `/api/v1/resources/admin/items/{resourceId}/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| resource 自检摘要 | GET | `/api/v1/resources/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

### admin

用途：后台首页、待办、配置、审计、数据看板和系统摘要。

端口：`8107`。来源：`docs/contracts-admin.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 管理后台总览 | GET | `/api/v1/admin/overview` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 模块注册表 | GET | `/api/v1/admin/modules` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 模块详情 | GET | `/api/v1/admin/modules/{moduleKey}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 待办聚合列表 | GET | `/api/v1/admin/todos` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 待办详情 | GET | `/api/v1/admin/todos/{todoId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 看板指标摘要 | GET | `/api/v1/admin/metrics/summary` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 审计索引列表 | GET | `/api/v1/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 系统配置快照 | GET | `/api/v1/admin/settings` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 更新系统配置 | PATCH | `/api/v1/admin/settings` | 是 | `ADMIN` 或 `OWNER`，高影响配置只允许 `OWNER` | MEDIUM |
| admin 自检摘要 | GET | `/api/v1/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

### onboarding

用途：入服流程进度、步骤完成、规则确认和后台流程配置。

端口：`8131`，由 `admission-core-service` 承载。来源：`docs/contracts-onboarding.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 当前用户进度 | GET | `/api/v1/onboarding/me/progress` | 是 | 当前用户 | LOW |
| 创建或恢复流程 | POST | `/api/v1/onboarding/me/start` | 是 | 当前用户 | LOW |
| 确认账号和 Minecraft 资料 | PATCH | `/api/v1/onboarding/me/profile-confirmation` | 是 | 当前用户 | LOW |
| 确认阅读规则 | PATCH | `/api/v1/onboarding/me/rules-confirmation` | 是 | 当前用户 | LOW |
| 选择审核方向 | PATCH | `/api/v1/onboarding/me/direction` | 是 | 当前用户 | LOW |
| 推进下一步 | POST | `/api/v1/onboarding/me/advance` | 是 | 当前用户 | LOW |
| 当前用户下一步入口 | GET | `/api/v1/onboarding/me/next-action` | 是 | 当前用户 | LOW |
| 后台流程列表 | GET | `/api/v1/onboarding/admin/applications` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台流程详情 | GET | `/api/v1/onboarding/admin/applications/{applicationId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 考试交接快照 | GET | `/api/v1/onboarding/admin/applications/{applicationId}/exam-handoff` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 后台重置流程 | PATCH | `/api/v1/onboarding/admin/applications/{applicationId}/reset` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台阻塞流程 | PATCH | `/api/v1/onboarding/admin/applications/{applicationId}/block` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台解除阻塞 | PATCH | `/api/v1/onboarding/admin/applications/{applicationId}/unblock` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| onboarding 审计列表 | GET | `/api/v1/onboarding/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| onboarding 自检摘要 | GET | `/api/v1/onboarding/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

### exam

用途：考试方向、题库、试卷、答题、阅卷和考试后台管理。

端口：`8131`，由 `admission-core-service` 承载。来源：`docs/contracts-exam.md`。详情合并稿：`docs/api-reference.md`。

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
| 结果修正 | PATCH | `/api/v1/exams/admin/sessions/{sessionId}/result-correction` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 要求补充 | PATCH | `/api/v1/exams/admin/sessions/{sessionId}/request-supplement` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 取消考试 | PATCH | `/api/v1/exams/admin/sessions/{sessionId}/cancel` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| whitelist 交接快照 | GET | `/api/v1/exams/admin/sessions/{sessionId}/whitelist-handoff` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 后台题目列表 | GET | `/api/v1/exams/admin/question-bank/questions` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 题目版本历史 | GET | `/api/v1/exams/admin/question-bank/questions/{questionId}/versions` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建题目 | POST | `/api/v1/exams/admin/question-bank/questions` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改题目 | PATCH | `/api/v1/exams/admin/question-bank/questions/{questionId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档题目 | PATCH | `/api/v1/exams/admin/question-bank/questions/{questionId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 模板列表 | GET | `/api/v1/exams/admin/paper-templates` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建模板 | POST | `/api/v1/exams/admin/paper-templates` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改模板 | PATCH | `/api/v1/exams/admin/paper-templates/{templateId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 模板发布预检 | GET | `/api/v1/exams/admin/paper-templates/{templateId}/publish-preview` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 发布模板 | PATCH | `/api/v1/exams/admin/paper-templates/{templateId}/publish` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档模板 | PATCH | `/api/v1/exams/admin/paper-templates/{templateId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| exam 审计列表 | GET | `/api/v1/exams/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| exam 自检摘要 | GET | `/api/v1/exams/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

### whitelist

用途：白名单申请、补充、撤回、审核、移除和重新申请。

端口：`8131`，由 `admission-core-service` 承载。来源：`docs/contracts-whitelist.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 创建白名单申请 | POST | `/api/v1/whitelist/me/applications` | 是 | 当前用户 | LOW |
| 当前申请 | GET | `/api/v1/whitelist/me/applications/current` | 是 | 当前用户 | LOW |
| 当前用户申请历史 | GET | `/api/v1/whitelist/me/applications` | 是 | 当前用户 | LOW |
| 当前用户申请详情 | GET | `/api/v1/whitelist/me/applications/{applicationId}` | 是 | 当前用户 | LOW |
| 修改申请材料 | PATCH | `/api/v1/whitelist/me/applications/{applicationId}` | 是 | 当前用户 | LOW |
| 提交审核 | POST | `/api/v1/whitelist/me/applications/{applicationId}/submit` | 是 | 当前用户 | LOW |
| 提交补充材料 | PATCH | `/api/v1/whitelist/me/applications/{applicationId}/supplement` | 是 | 当前用户 | LOW |
| 撤回申请 | PATCH | `/api/v1/whitelist/me/applications/{applicationId}/withdraw` | 是 | 当前用户 | MEDIUM |
| 读取审核结果 | GET | `/api/v1/whitelist/me/applications/{applicationId}/result` | 是 | 当前用户 | LOW |
| 后台申请列表 | GET | `/api/v1/whitelist/admin/applications` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台申请详情 | GET | `/api/v1/whitelist/admin/applications/{applicationId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 分配审核人 | PATCH | `/api/v1/whitelist/admin/applications/{applicationId}/assign` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 要求补充 | PATCH | `/api/v1/whitelist/admin/applications/{applicationId}/request-supplement` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核通过 | PATCH | `/api/v1/whitelist/admin/applications/{applicationId}/approve` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/whitelist/admin/applications/{applicationId}/reject` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 移除白名单 | PATCH | `/api/v1/whitelist/admin/applications/{applicationId}/remove` | 是 | `ADMIN` 或 `OWNER` | HIGH |
| 允许重新申请 | POST | `/api/v1/whitelist/admin/applications/{applicationId}/reopen` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| attendance 交接摘要 | GET | `/api/v1/whitelist/admin/applications/{applicationId}/attendance-handoff` | 是 | `ADMIN` 或 `OWNER` | LOW |
| whitelist 审计列表 | GET | `/api/v1/whitelist/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| whitelist 自检摘要 | GET | `/api/v1/whitelist/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

### attendance

用途：考勤积分、积分流水、榜单、月度任务和后台调整。

端口：`8131`，由 `admission-core-service` 承载。来源：`docs/contracts-attendance.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开榜单 | GET | `/api/v1/attendance/leaderboard` | 否 | 游客 | LOW |
| 我的考勤账户 | GET | `/api/v1/attendance/me/account` | 是 | 当前用户 | LOW |
| 我的积分流水 | GET | `/api/v1/attendance/me/ledger` | 是 | 当前用户 | LOW |
| 我的贡献记录 | GET | `/api/v1/attendance/me/contributions` | 是 | 当前用户 | LOW |
| 我的榜单位置 | GET | `/api/v1/attendance/me/ranking` | 是 | 当前用户 | LOW |
| 后台账户列表 | GET | `/api/v1/attendance/admin/accounts` | 是 | HELPER、ADMIN、OWNER | LOW |
| 后台账户详情 | GET | `/api/v1/attendance/admin/accounts/{accountId}` | 是 | HELPER、ADMIN、OWNER | LOW |
| 消费 whitelist 初始化交接 | POST | `/api/v1/attendance/admin/initializations` | 是 | ADMIN、OWNER | MEDIUM |
| 管理员积分调整 | POST | `/api/v1/attendance/admin/accounts/{accountId}/adjustments` | 是 | ADMIN、OWNER | MEDIUM |
| 撤销积分流水 | POST | `/api/v1/attendance/admin/ledger/{ledgerId}/reverse` | 是 | ADMIN、OWNER | MEDIUM |
| 创建贡献记录 | POST | `/api/v1/attendance/admin/contributions` | 是 | ADMIN、OWNER | MEDIUM |
| 修正贡献记录 | PATCH | `/api/v1/attendance/admin/contributions/{contributionId}` | 是 | ADMIN、OWNER | MEDIUM |
| 月度扣分预检 | POST | `/api/v1/attendance/admin/monthly-runs/preview` | 是 | ADMIN、OWNER | MEDIUM |
| 执行月度扣分 | POST | `/api/v1/attendance/admin/monthly-runs` | 是 | ADMIN、OWNER | HIGH |
| 月度扣分运行详情 | GET | `/api/v1/attendance/admin/monthly-runs/{runId}` | 是 | ADMIN、OWNER | LOW |
| 白名单移除候选列表 | GET | `/api/v1/attendance/admin/removal-candidates` | 是 | HELPER、ADMIN、OWNER | LOW |
| 确认移除候选 | PATCH | `/api/v1/attendance/admin/removal-candidates/{candidateId}/confirm` | 是 | ADMIN、OWNER | HIGH |
| 驳回移除候选 | PATCH | `/api/v1/attendance/admin/removal-candidates/{candidateId}/dismiss` | 是 | ADMIN、OWNER | MEDIUM |
| 榜单重算 | POST | `/api/v1/attendance/admin/leaderboard/rebuild` | 是 | ADMIN、OWNER | MEDIUM |
| attendance 审计列表 | GET | `/api/v1/attendance/admin/audit-logs` | 是 | ADMIN、OWNER | LOW |
| attendance 自检摘要 | GET | `/api/v1/attendance/admin/ops/summary` | 是 | HELPER、ADMIN、OWNER | LOW |

### community

用途：板块、帖子、评论、点赞、收藏、投票、举报、工单和处罚。

端口：`8132`，由 `engagement-core-service` 承载。来源：`docs/contracts-community.md`。详情合并稿：`docs/api-reference.md`。

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

### activity

用途：活动列表、报名、签到、结果、奖励和活动后台管理。

端口：`8132`，由 `engagement-core-service` 承载。来源：`docs/contracts-activity.md`。详情合并稿：`docs/api-reference.md`。

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

### calendar

用途：公开日程、维护窗口、工程节点、提醒和日历后台维护。

端口：`8132`，由 `engagement-core-service` 承载。来源：`docs/contracts-calendar.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开事件列表 | GET | `/api/v1/calendar/events` | 否 | 无 | LOW |
| 公开事件详情 | GET | `/api/v1/calendar/events/{eventId}` | 否 | 无 | LOW |
| 月视图 | GET | `/api/v1/calendar/month` | 否 | 无 | LOW |
| 即将开始 | GET | `/api/v1/calendar/upcoming` | 否 | 无 | LOW |
| 我的关注列表 | GET | `/api/v1/calendar/me/watchlist` | 是 | 当前用户 | LOW |
| 关注事件 | POST | `/api/v1/calendar/me/events/{eventId}/watch` | 是 | 当前用户 | LOW |
| 取消关注事件 | POST | `/api/v1/calendar/me/events/{eventId}/unwatch` | 是 | 当前用户 | LOW |
| 后台事件列表 | GET | `/api/v1/calendar/admin/events` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 后台事件详情 | GET | `/api/v1/calendar/admin/events/{eventId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 创建事件 | POST | `/api/v1/calendar/admin/events` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 修改事件 | PATCH | `/api/v1/calendar/admin/events/{eventId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 提交审核 | POST | `/api/v1/calendar/admin/events/{eventId}/submit` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核通过 | PATCH | `/api/v1/calendar/admin/events/{eventId}/approve` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/calendar/admin/events/{eventId}/reject` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 发布事件 | PATCH | `/api/v1/calendar/admin/events/{eventId}/publish` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 下架事件 | PATCH | `/api/v1/calendar/admin/events/{eventId}/offline` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 归档事件 | PATCH | `/api/v1/calendar/admin/events/{eventId}/archive` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 软删除事件 | PATCH | `/api/v1/calendar/admin/events/{eventId}/delete` | 是 | `ADMIN`、`OWNER` | HIGH |
| 同步 activity 摘要 | POST | `/api/v1/calendar/admin/sync/activity` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 审计列表 | GET | `/api/v1/calendar/admin/audit-logs` | 是 | `ADMIN`、`OWNER` | LOW |
| calendar 自检摘要 | GET | `/api/v1/calendar/admin/ops/summary` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |

### changelog

用途：版本更新、维护日志、插件变更、规则调整和后台发布。

端口：`8132`，由 `engagement-core-service` 承载。来源：`docs/contracts-changelog.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 公开发布列表 | GET | `/api/v1/changelog/releases` | 否 | 无 | LOW |
| 公开发布详情 | GET | `/api/v1/changelog/releases/{releaseIdOrSlug}` | 否 | 无 | LOW |
| 最新版本 | GET | `/api/v1/changelog/versions/latest` | 否 | 无 | LOW |
| 标签和筛选项 | GET | `/api/v1/changelog/tags` | 否 | 无 | LOW |
| 公开变更项搜索 | GET | `/api/v1/changelog/changes` | 否 | 无 | LOW |
| 我的收藏列表 | GET | `/api/v1/changelog/me/bookmarks` | 是 | 当前用户 | LOW |
| 收藏发布记录 | POST | `/api/v1/changelog/me/releases/{releaseId}/bookmark` | 是 | 当前用户 | LOW |
| 取消收藏发布记录 | POST | `/api/v1/changelog/me/releases/{releaseId}/unbookmark` | 是 | 当前用户 | LOW |
| 后台发布列表 | GET | `/api/v1/changelog/admin/releases` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 后台发布详情 | GET | `/api/v1/changelog/admin/releases/{releaseId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |
| 创建发布草稿 | POST | `/api/v1/changelog/admin/releases` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 修改发布记录 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 提交审核 | POST | `/api/v1/changelog/admin/releases/{releaseId}/submit` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核通过 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/approve` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/reject` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 要求修改 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/request-changes` | 是 | `HELPER`、`ADMIN`、`OWNER` | MEDIUM |
| 发布 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/publish` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 下架 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/offline` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 归档 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/archive` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 软删除 | PATCH | `/api/v1/changelog/admin/releases/{releaseId}/delete` | 是 | `ADMIN`、`OWNER` | HIGH |
| 日历同步摘要 | POST | `/api/v1/changelog/admin/releases/{releaseId}/calendar-sync` | 是 | `ADMIN`、`OWNER` | MEDIUM |
| 审计列表 | GET | `/api/v1/changelog/admin/audit-logs` | 是 | `ADMIN`、`OWNER` | LOW |
| changelog 自检摘要 | GET | `/api/v1/changelog/admin/ops/summary` | 是 | `HELPER`、`ADMIN`、`OWNER` | LOW |

### ops-control

用途：运维控制台的节点、资产、容器、实例、文件、日志、终端和审批控制面。

当前入口端口：`8133`。历史原端口：`8116`。来源：`docs/contracts-ops-control.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 运维总览 | GET | `/api/v1/ops-control/overview` | 是 | `NODE_READ` | LOW |
| 资产列表 | GET | `/api/v1/ops-control/assets` | 是 | `NODE_READ` | LOW |
| 资产详情 | GET | `/api/v1/ops-control/assets/{assetId}` | 是 | `NODE_READ` | LOW |
| 节点列表 | GET | `/api/v1/ops-control/nodes` | 是 | `NODE_READ` | LOW |
| 节点详情 | GET | `/api/v1/ops-control/nodes/{nodeId}` | 是 | `NODE_READ` | LOW |
| 注册节点 | POST | `/api/v1/ops-control/nodes` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 禁用节点 | PATCH | `/api/v1/ops-control/nodes/{nodeId}/disable` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | HIGH |
| 启用节点 | PATCH | `/api/v1/ops-control/nodes/{nodeId}/enable` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 节点心跳摘要 | POST | `/api/v1/ops-control/nodes/{nodeId}/heartbeat` | 是 | 节点认证或 `NODE_WRITE` | MEDIUM |
| 节点能力 | GET | `/api/v1/ops-control/nodes/{nodeId}/capabilities` | 是 | `NODE_READ` | LOW |
| 节点指标 | GET | `/api/v1/ops-control/nodes/{nodeId}/metrics/latest` | 是 | `NODE_READ` | LOW |
| 容器列表 | GET | `/api/v1/ops-control/nodes/{nodeId}/containers` | 是 | `NODE_READ` | LOW |
| 容器详情 | GET | `/api/v1/ops-control/nodes/{nodeId}/containers/{containerId}` | 是 | `NODE_READ` | LOW |
| 虚拟机列表 | GET | `/api/v1/ops-control/nodes/{nodeId}/vms` | 是 | `NODE_READ` | LOW |
| 虚拟机详情 | GET | `/api/v1/ops-control/nodes/{nodeId}/vms/{vmId}` | 是 | `NODE_READ` | LOW |
| Minecraft 实例列表 | GET | `/api/v1/ops-control/nodes/{nodeId}/minecraft-instances` | 是 | `NODE_READ` | LOW |
| Minecraft 实例详情 | GET | `/api/v1/ops-control/nodes/{nodeId}/minecraft-instances/{instanceId}` | 是 | `NODE_READ` | LOW |
| 授权目录文件列表 | GET | `/api/v1/ops-control/nodes/{nodeId}/files` | 是 | `FILE_MANAGE` | LOW |
| 文本文件读取请求 | POST | `/api/v1/ops-control/nodes/{nodeId}/files/read` | 是 | `FILE_MANAGE` | MEDIUM |
| 日志摘要请求 | POST | `/api/v1/ops-control/nodes/{nodeId}/logs/query` | 是 | `NODE_READ` | MEDIUM |
| 创建受控任务 | POST | `/api/v1/ops-control/tasks` | 是 | 按任务类型校验能力点 | MEDIUM 到 CRITICAL |
| 任务列表 | GET | `/api/v1/ops-control/tasks` | 是 | `NODE_READ` | LOW |
| 任务详情 | GET | `/api/v1/ops-control/tasks/{taskId}` | 是 | `NODE_READ` | LOW |
| 取消任务 | PATCH | `/api/v1/ops-control/tasks/{taskId}/cancel` | 是 | 创建者、`ADMIN` 或 `OWNER` | MEDIUM |
| 节点任务回写 | POST | `/api/v1/ops-control/tasks/{taskId}/node-result` | 是 | 节点认证或 `NODE_WRITE` | MEDIUM |
| 审批列表 | GET | `/api/v1/ops-control/approvals` | 是 | `HIGH_RISK_APPROVE` 或 `OWNER` | LOW |
| 审批详情 | GET | `/api/v1/ops-control/approvals/{approvalId}` | 是 | `HIGH_RISK_APPROVE` 或 `OWNER` | LOW |
| 审批通过 | PATCH | `/api/v1/ops-control/approvals/{approvalId}/approve` | 是 | `HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 审批拒绝 | PATCH | `/api/v1/ops-control/approvals/{approvalId}/reject` | 是 | `HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 审计列表 | GET | `/api/v1/ops-control/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 自检摘要 | GET | `/api/v1/ops-control/ops/summary` | 是 | `NODE_READ` | LOW |

### cloudreve-sync

用途：Cloudreve provider、目录同步、文件快照、分享解析和同步审计。

当前入口端口：`8133`。历史原端口：`8118`。来源：`docs/contracts-cloudreve-sync.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/cloudreve-sync/health` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/cloudreve-sync/ops/summary` | 是 | `NODE_READ` 或 `FILE_MANAGE` | LOW |
| provider 列表 | GET | `/api/v1/cloudreve-sync/providers` | 是 | `NODE_READ` 或 `FILE_MANAGE` | LOW |
| provider 详情 | GET | `/api/v1/cloudreve-sync/providers/{providerId}` | 是 | `NODE_READ` 或 `FILE_MANAGE` | LOW |
| 创建 provider | POST | `/api/v1/cloudreve-sync/providers` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 更新 provider | PATCH | `/api/v1/cloudreve-sync/providers/{providerId}` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 禁用 provider | PATCH | `/api/v1/cloudreve-sync/providers/{providerId}/disable` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 启用 provider | PATCH | `/api/v1/cloudreve-sync/providers/{providerId}/enable` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 文件快照列表 | GET | `/api/v1/cloudreve-sync/files` | 是 | `FILE_MANAGE` 或 `NODE_READ` | LOW |
| 分享快照列表 | GET | `/api/v1/cloudreve-sync/shares` | 是 | `FILE_MANAGE` 或 `NODE_READ` | LOW |
| 分享解析 | POST | `/api/v1/cloudreve-sync/shares/resolve` | 是 | `FILE_MANAGE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 创建同步任务 | POST | `/api/v1/cloudreve-sync/sync-jobs` | 是 | `FILE_MANAGE` 或 `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 同步任务列表 | GET | `/api/v1/cloudreve-sync/sync-jobs` | 是 | `FILE_MANAGE` 或 `NODE_READ` | LOW |
| 同步任务详情 | GET | `/api/v1/cloudreve-sync/sync-jobs/{jobId}` | 是 | `FILE_MANAGE` 或 `NODE_READ` | LOW |
| 取消同步任务 | PATCH | `/api/v1/cloudreve-sync/sync-jobs/{jobId}/cancel` | 是 | 任务创建者、`ADMIN` 或 `OWNER` | MEDIUM |
| 审计列表 | GET | `/api/v1/cloudreve-sync/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

### backup-recovery

用途：备份域、策略、任务、备份点、校验、演练、恢复申请和审批摘要。

当前入口端口：`8133`。历史原端口：`8119`。来源：`docs/contracts-backup-recovery.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/backup-recovery/health` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/backup-recovery/ops/summary` | 是 | `NODE_READ` 或 `HIGH_RISK_APPROVE` | LOW |
| 备份域列表 | GET | `/api/v1/backup-recovery/domains` | 是 | `NODE_READ` | LOW |
| 策略列表 | GET | `/api/v1/backup-recovery/policies` | 是 | `NODE_READ` | LOW |
| 策略详情 | GET | `/api/v1/backup-recovery/policies/{policyId}` | 是 | `NODE_READ` | LOW |
| 创建策略 | POST | `/api/v1/backup-recovery/policies` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 更新策略 | PATCH | `/api/v1/backup-recovery/policies/{policyId}` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 启用策略 | PATCH | `/api/v1/backup-recovery/policies/{policyId}/enable` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 禁用策略 | PATCH | `/api/v1/backup-recovery/policies/{policyId}/disable` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | MEDIUM |
| 创建备份任务 | POST | `/api/v1/backup-recovery/jobs` | 是 | `NODE_WRITE`，`ADMIN` 或 `OWNER` | HIGH |
| 任务列表 | GET | `/api/v1/backup-recovery/jobs` | 是 | `NODE_READ` | LOW |
| 任务详情 | GET | `/api/v1/backup-recovery/jobs/{jobId}` | 是 | `NODE_READ` | LOW |
| 取消任务 | PATCH | `/api/v1/backup-recovery/jobs/{jobId}/cancel` | 是 | 创建者、`ADMIN` 或 `OWNER` | MEDIUM |
| 备份点列表 | GET | `/api/v1/backup-recovery/backup-points` | 是 | `NODE_READ` | LOW |
| 备份点详情 | GET | `/api/v1/backup-recovery/backup-points/{backupPointId}` | 是 | `NODE_READ` | LOW |
| 校验备份点 | POST | `/api/v1/backup-recovery/backup-points/{backupPointId}/verify` | 是 | `HIGH_RISK_APPROVE`，`ADMIN` 或 `OWNER` | HIGH |
| 创建恢复演练 | POST | `/api/v1/backup-recovery/restore-drills` | 是 | `HIGH_RISK_APPROVE`，`ADMIN` 或 `OWNER` | HIGH |
| 恢复演练列表 | GET | `/api/v1/backup-recovery/restore-drills` | 是 | `NODE_READ` | LOW |
| 恢复演练详情 | GET | `/api/v1/backup-recovery/restore-drills/{drillId}` | 是 | `NODE_READ` | LOW |
| 创建恢复申请 | POST | `/api/v1/backup-recovery/restore-requests` | 是 | `HIGH_RISK_APPROVE`，`ADMIN` 或 `OWNER` | CRITICAL |
| 恢复申请列表 | GET | `/api/v1/backup-recovery/restore-requests` | 是 | `NODE_READ` 或 `HIGH_RISK_APPROVE` | LOW |
| 恢复申请详情 | GET | `/api/v1/backup-recovery/restore-requests/{restoreRequestId}` | 是 | `NODE_READ` 或 `HIGH_RISK_APPROVE` | LOW |
| 审批恢复申请 | PATCH | `/api/v1/backup-recovery/restore-requests/{restoreRequestId}/approve` | 是 | `HIGH_RISK_APPROVE` 或 `OWNER` | CRITICAL |
| 拒绝恢复申请 | PATCH | `/api/v1/backup-recovery/restore-requests/{restoreRequestId}/reject` | 是 | `HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 审计列表 | GET | `/api/v1/backup-recovery/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

### alerting

用途：告警规则、事件、静默、订阅、通知演练和告警后台管理。

当前入口端口：`8133`。历史原端口：`8120`。来源：`docs/contracts-alerting.md`。详情合并稿：`docs/api-reference.md`。

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

### online-map

用途：在线地图 provider、世界、图层、marker、区域、公开入口和后台维护。

端口：`8134`，由 `portal-core-service` 承载。历史原服务端口 `8121` 只作为原端口记录，旧入口已退役。来源：`docs/contracts-online-map.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/online-map/health` | 否 | 无 | LOW |
| 公开地图总览 | GET | `/api/v1/online-map/overview` | 否 | 无 | LOW |
| 公开 provider 列表 | GET | `/api/v1/online-map/providers` | 否 | 无 | LOW |
| 公开 provider 详情 | GET | `/api/v1/online-map/providers/{providerId}` | 否 | 无 | LOW |
| 公开世界列表 | GET | `/api/v1/online-map/worlds` | 否 | 无 | LOW |
| 公开图层列表 | GET | `/api/v1/online-map/layers` | 否 | 无 | LOW |
| 公开 marker 列表 | GET | `/api/v1/online-map/markers` | 否 | 无 | LOW |
| 公开区域列表 | GET | `/api/v1/online-map/regions` | 否 | 无 | LOW |
| 公开嵌入配置 | GET | `/api/v1/online-map/embed` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/online-map/admin/ops/summary` | 是 | `NODE_READ` | LOW |
| 后台 provider 列表 | GET | `/api/v1/online-map/admin/providers` | 是 | `NODE_READ` | LOW |
| 后台 provider 详情 | GET | `/api/v1/online-map/admin/providers/{providerId}` | 是 | `NODE_READ` | LOW |
| 创建 provider | POST | `/api/v1/online-map/admin/providers` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 修改 provider | PATCH | `/api/v1/online-map/admin/providers/{providerId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 启用 provider | PATCH | `/api/v1/online-map/admin/providers/{providerId}/enable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | HIGH |
| 禁用 provider | PATCH | `/api/v1/online-map/admin/providers/{providerId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档 provider | PATCH | `/api/v1/online-map/admin/providers/{providerId}/archive` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | HIGH |
| 刷新 provider 健康 | POST | `/api/v1/online-map/admin/providers/{providerId}/health/refresh` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 健康快照列表 | GET | `/api/v1/online-map/admin/providers/{providerId}/health/snapshots` | 是 | `NODE_READ` | LOW |
| 后台世界列表 | GET | `/api/v1/online-map/admin/worlds` | 是 | `NODE_READ` | LOW |
| 保存世界快照 | PUT | `/api/v1/online-map/admin/worlds/{worldId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 后台图层列表 | GET | `/api/v1/online-map/admin/layers` | 是 | `NODE_READ` | LOW |
| 创建图层 | POST | `/api/v1/online-map/admin/layers` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 修改图层 | PATCH | `/api/v1/online-map/admin/layers/{layerId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档图层 | PATCH | `/api/v1/online-map/admin/layers/{layerId}/archive` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 后台 marker 列表 | GET | `/api/v1/online-map/admin/markers` | 是 | `NODE_READ` | LOW |
| 创建 marker | POST | `/api/v1/online-map/admin/markers` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 修改 marker | PATCH | `/api/v1/online-map/admin/markers/{markerId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档 marker | PATCH | `/api/v1/online-map/admin/markers/{markerId}/archive` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 后台区域列表 | GET | `/api/v1/online-map/admin/regions` | 是 | `NODE_READ` | LOW |
| 创建区域 | POST | `/api/v1/online-map/admin/regions` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 修改区域 | PATCH | `/api/v1/online-map/admin/regions/{regionId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档区域 | PATCH | `/api/v1/online-map/admin/regions/{regionId}/archive` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 审计列表 | GET | `/api/v1/online-map/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

### plugin-integration

用途：插件源、实例、事件、命令、同步任务和插件联动后台。

当前入口端口：`8133`。历史原端口：`8122`。来源：`docs/contracts-plugin-integration.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/plugin-integration/health` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/plugin-integration/admin/ops/summary` | 是 | `NODE_READ` | LOW |
| provider 列表 | GET | `/api/v1/plugin-integration/admin/providers` | 是 | `NODE_READ` | LOW |
| provider 详情 | GET | `/api/v1/plugin-integration/admin/providers/{providerId}` | 是 | `NODE_READ` | LOW |
| 创建 provider | POST | `/api/v1/plugin-integration/admin/providers` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 修改 provider | PATCH | `/api/v1/plugin-integration/admin/providers/{providerId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 启用 provider | PATCH | `/api/v1/plugin-integration/admin/providers/{providerId}/enable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | HIGH |
| 禁用 provider | PATCH | `/api/v1/plugin-integration/admin/providers/{providerId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 归档 provider | PATCH | `/api/v1/plugin-integration/admin/providers/{providerId}/archive` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | HIGH |
| 插件实例列表 | GET | `/api/v1/plugin-integration/admin/instances` | 是 | `NODE_READ` | LOW |
| 插件实例详情 | GET | `/api/v1/plugin-integration/admin/instances/{instanceId}` | 是 | `NODE_READ` | LOW |
| 插件能力列表 | GET | `/api/v1/plugin-integration/admin/capabilities` | 是 | `NODE_READ` | LOW |
| schema 列表 | GET | `/api/v1/plugin-integration/admin/event-schemas` | 是 | `NODE_READ` | LOW |
| schema 详情 | GET | `/api/v1/plugin-integration/admin/event-schemas/{schemaId}` | 是 | `NODE_READ` | LOW |
| 创建 schema | POST | `/api/v1/plugin-integration/admin/event-schemas` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 修改 schema | PATCH | `/api/v1/plugin-integration/admin/event-schemas/{schemaId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 启用 schema | PATCH | `/api/v1/plugin-integration/admin/event-schemas/{schemaId}/enable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 禁用 schema | PATCH | `/api/v1/plugin-integration/admin/event-schemas/{schemaId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 事件接收 | POST | `/api/v1/plugin-integration/admin/events/ingest` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 事件列表 | GET | `/api/v1/plugin-integration/admin/events` | 是 | `NODE_READ` | LOW |
| 事件详情 | GET | `/api/v1/plugin-integration/admin/events/{eventId}` | 是 | `NODE_READ` | LOW |
| 事件重放 | POST | `/api/v1/plugin-integration/admin/events/{eventId}/replay` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE`，高风险时 `HIGH_RISK_APPROVE` | HIGH |
| 路由规则列表 | GET | `/api/v1/plugin-integration/admin/route-rules` | 是 | `NODE_READ` | LOW |
| 路由规则详情 | GET | `/api/v1/plugin-integration/admin/route-rules/{ruleId}` | 是 | `NODE_READ` | LOW |
| 创建路由规则 | POST | `/api/v1/plugin-integration/admin/route-rules` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 修改路由规则 | PATCH | `/api/v1/plugin-integration/admin/route-rules/{ruleId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 启用路由规则 | PATCH | `/api/v1/plugin-integration/admin/route-rules/{ruleId}/enable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 禁用路由规则 | PATCH | `/api/v1/plugin-integration/admin/route-rules/{ruleId}/disable` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 创建同步任务 | POST | `/api/v1/plugin-integration/admin/sync-tasks` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 同步任务列表 | GET | `/api/v1/plugin-integration/admin/sync-tasks` | 是 | `NODE_READ` | LOW |
| 同步任务详情 | GET | `/api/v1/plugin-integration/admin/sync-tasks/{taskId}` | 是 | `NODE_READ` | LOW |
| 取消同步任务 | PATCH | `/api/v1/plugin-integration/admin/sync-tasks/{taskId}/cancel` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 健康快照列表 | GET | `/api/v1/plugin-integration/admin/providers/{providerId}/health-snapshots` | 是 | `NODE_READ` | LOW |
| 对象映射列表 | GET | `/api/v1/plugin-integration/admin/object-mappings` | 是 | `NODE_READ` | LOW |
| 对象映射详情 | GET | `/api/v1/plugin-integration/admin/object-mappings/{mappingId}` | 是 | `NODE_READ` | LOW |
| 创建或更新对象映射 | PUT | `/api/v1/plugin-integration/admin/object-mappings/{mappingId}` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM 或 HIGH |
| 归档对象映射 | PATCH | `/api/v1/plugin-integration/admin/object-mappings/{mappingId}/archive` | 是 | `ADMIN` 或 `OWNER`，`NODE_WRITE` | MEDIUM |
| 审计列表 | GET | `/api/v1/plugin-integration/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

### cross-platform-notification

用途：跨平台通知渠道、模板、投递任务、回执和后台演练。

端口：`8133`。当前运行入口：`ops-core-service`。历史独立端口：`8123`。来源：`docs/contracts-cross-platform-notification.md`。详情合并稿：`docs/api-reference.md`。

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

### ops-image-market

用途：运维镜像、仓库、版本、拉取任务、漏洞摘要和镜像后台管理。

当前入口端口：`8133`。历史原端口：`8124`。来源：`docs/contracts-ops-image-market.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 健康检查 | GET | `/api/v1/ops-image-market/health` | 否 | 无 | LOW |
| 自检摘要 | GET | `/api/v1/ops-image-market/admin/ops/summary` | 是 | `NODE_READ` | LOW |
| provider 列表 | GET | `/api/v1/ops-image-market/admin/providers` | 是 | `NODE_READ` | LOW |
| provider 详情 | GET | `/api/v1/ops-image-market/admin/providers/{providerId}` | 是 | `NODE_READ` | LOW |
| 创建 provider | POST | `/api/v1/ops-image-market/admin/providers` | 是 | `NODE_WRITE`、`HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 更新 provider | PATCH | `/api/v1/ops-image-market/admin/providers/{providerId}` | 是 | `NODE_WRITE`，高风险字段需 `HIGH_RISK_APPROVE` 或 `OWNER` | MEDIUM 到 HIGH |
| 启用 provider | PATCH | `/api/v1/ops-image-market/admin/providers/{providerId}/enable` | 是 | `NODE_WRITE`、`HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 禁用 provider | PATCH | `/api/v1/ops-image-market/admin/providers/{providerId}/disable` | 是 | `NODE_WRITE` | MEDIUM |
| 归档 provider | PATCH | `/api/v1/ops-image-market/admin/providers/{providerId}/archive` | 是 | `NODE_WRITE`、`HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 刷新 provider 健康 | POST | `/api/v1/ops-image-market/admin/providers/{providerId}/health-refresh` | 是 | `NODE_WRITE` | MEDIUM |
| 镜像列表 | GET | `/api/v1/ops-image-market/admin/images` | 是 | `NODE_READ` | LOW |
| 镜像详情 | GET | `/api/v1/ops-image-market/admin/images/{imageId}` | 是 | `NODE_READ` | LOW |
| 创建镜像 | POST | `/api/v1/ops-image-market/admin/images` | 是 | `NODE_WRITE` | MEDIUM |
| 更新镜像 | PATCH | `/api/v1/ops-image-market/admin/images/{imageId}` | 是 | `NODE_WRITE` | MEDIUM |
| 发布镜像 | PATCH | `/api/v1/ops-image-market/admin/images/{imageId}/publish` | 是 | `NODE_WRITE` | MEDIUM |
| 阻断镜像 | PATCH | `/api/v1/ops-image-market/admin/images/{imageId}/block` | 是 | `NODE_WRITE`、高风险需 `HIGH_RISK_APPROVE` 或 `OWNER` | HIGH |
| 归档镜像 | PATCH | `/api/v1/ops-image-market/admin/images/{imageId}/archive` | 是 | `NODE_WRITE` | MEDIUM |
| 版本列表 | GET | `/api/v1/ops-image-market/admin/images/{imageId}/versions` | 是 | `NODE_READ` | LOW |
| 版本详情 | GET | `/api/v1/ops-image-market/admin/versions/{imageVersionId}` | 是 | `NODE_READ` | LOW |
| 创建版本 | POST | `/api/v1/ops-image-market/admin/images/{imageId}/versions` | 是 | `NODE_WRITE` | MEDIUM |
| 批准版本 | PATCH | `/api/v1/ops-image-market/admin/versions/{imageVersionId}/approve` | 是 | `NODE_WRITE`，高风险需 `HIGH_RISK_APPROVE` 或 `OWNER` | MEDIUM 到 CRITICAL |
| 废弃版本 | PATCH | `/api/v1/ops-image-market/admin/versions/{imageVersionId}/deprecate` | 是 | `NODE_WRITE` | MEDIUM |
| 阻断版本 | PATCH | `/api/v1/ops-image-market/admin/versions/{imageVersionId}/block` | 是 | `NODE_WRITE` | HIGH |
| 归档版本 | PATCH | `/api/v1/ops-image-market/admin/versions/{imageVersionId}/archive` | 是 | `NODE_WRITE` | MEDIUM |
| 兼容配置列表 | GET | `/api/v1/ops-image-market/admin/compatibility-profiles` | 是 | `NODE_READ` | LOW |
| 兼容配置详情 | GET | `/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}` | 是 | `NODE_READ` | LOW |
| 创建兼容配置 | POST | `/api/v1/ops-image-market/admin/compatibility-profiles` | 是 | `NODE_WRITE` | MEDIUM |
| 更新兼容配置 | PATCH | `/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}` | 是 | `NODE_WRITE` | MEDIUM |
| 启用兼容配置 | PATCH | `/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/enable` | 是 | `NODE_WRITE` | MEDIUM |
| 禁用兼容配置 | PATCH | `/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/disable` | 是 | `NODE_WRITE` | MEDIUM |
| 归档兼容配置 | PATCH | `/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/archive` | 是 | `NODE_WRITE` | MEDIUM |
| 镜像模板列表 | GET | `/api/v1/ops-image-market/admin/templates` | 是 | `NODE_READ` | LOW |
| 镜像模板详情 | GET | `/api/v1/ops-image-market/admin/templates/{templateId}` | 是 | `NODE_READ` | LOW |
| 创建镜像模板 | POST | `/api/v1/ops-image-market/admin/templates` | 是 | `NODE_WRITE` | MEDIUM |
| 更新镜像模板 | PATCH | `/api/v1/ops-image-market/admin/templates/{templateId}` | 是 | `NODE_WRITE` | MEDIUM |
| 启用镜像模板 | PATCH | `/api/v1/ops-image-market/admin/templates/{templateId}/enable` | 是 | `NODE_WRITE` | MEDIUM |
| 禁用镜像模板 | PATCH | `/api/v1/ops-image-market/admin/templates/{templateId}/disable` | 是 | `NODE_WRITE` | MEDIUM |
| 归档镜像模板 | PATCH | `/api/v1/ops-image-market/admin/templates/{templateId}/archive` | 是 | `NODE_WRITE` | MEDIUM |
| 风险扫描列表 | GET | `/api/v1/ops-image-market/admin/scans` | 是 | `NODE_READ` | LOW |
| 风险扫描详情 | GET | `/api/v1/ops-image-market/admin/scans/{scanId}` | 是 | `NODE_READ` | LOW |
| 创建扫描摘要 | POST | `/api/v1/ops-image-market/admin/versions/{imageVersionId}/scans` | 是 | `NODE_WRITE` | MEDIUM |
| 拉取计划列表 | GET | `/api/v1/ops-image-market/admin/pull-plans` | 是 | `NODE_READ` | LOW |
| 拉取计划详情 | GET | `/api/v1/ops-image-market/admin/pull-plans/{planId}` | 是 | `NODE_READ` | LOW |
| 创建拉取计划 | POST | `/api/v1/ops-image-market/admin/pull-plans` | 是 | `NODE_WRITE`，高风险需 `HIGH_RISK_APPROVE` 或 `OWNER` | MEDIUM 到 CRITICAL |
| 批准拉取计划 | PATCH | `/api/v1/ops-image-market/admin/pull-plans/{planId}/approve` | 是 | `NODE_WRITE`、`HIGH_RISK_APPROVE` 或 `OWNER` | HIGH 到 CRITICAL |
| 取消拉取计划 | PATCH | `/api/v1/ops-image-market/admin/pull-plans/{planId}/cancel` | 是 | `NODE_WRITE`，高风险需 `HIGH_RISK_APPROVE` 或 `OWNER` | MEDIUM 到 HIGH |
| 节点缓存列表 | GET | `/api/v1/ops-image-market/admin/cache-snapshots` | 是 | `NODE_READ` | LOW |
| 节点缓存详情 | GET | `/api/v1/ops-image-market/admin/cache-snapshots/{snapshotId}` | 是 | `NODE_READ` | LOW |
| 审计列表 | GET | `/api/v1/ops-image-market/admin/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

### api-gateway

用途：统一入口、路由表、上游健康、请求日志和业务请求转发。

端口：`8125`。来源：`docs/contracts-api-gateway.md`。详情合并稿：`docs/api-reference.md`。

网关业务转发规则：`GET/POST/PUT/PATCH/DELETE/OPTIONS /api/v1/{module}/**`。认证、权限和风险由上游模块决定。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 网关健康检查 | GET | `/api/v1/gateway/health` | 否 | 无 | LOW |
| 网关自检摘要 | GET | `/api/v1/gateway/admin/ops/summary` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 网关路由列表 | GET | `/api/v1/gateway/admin/routes` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 网关路由详情 | GET | `/api/v1/gateway/admin/routes/{routeId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 上游健康列表 | GET | `/api/v1/gateway/admin/upstreams` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 刷新上游健康 | POST | `/api/v1/gateway/admin/upstreams/{serviceKey}/health-refresh` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 网关请求日志 | GET | `/api/v1/gateway/admin/request-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |

### material

用途：素材投稿、素材展示、精选、审核、授权、文件摘要和素材后台管理。

端口：`8134`，由 `portal-core-service` 承载。来源：`docs/contracts-material.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 精选素材列表 | GET | `/api/v1/materials/featured` | 否 | 无 | LOW |
| 公开素材列表 | GET | `/api/v1/materials` | 否 | 无 | LOW |
| 公开素材详情 | GET | `/api/v1/materials/{materialId}` | 否 | 无 | LOW |
| 公开 slug 素材详情 | GET | `/api/v1/materials/by-slug/{slug}` | 否 | 无 | LOW |
| 公开分类列表 | GET | `/api/v1/materials/categories` | 否 | 无 | LOW |
| 公开素材文件摘要 | GET | `/api/v1/materials/{materialId}/assets` | 否 | 无 | LOW |
| 创建上传会话 | POST | `/api/v1/materials/me/upload-sessions` | 是 | 当前用户 | LOW |
| 完成上传会话 | PATCH | `/api/v1/materials/me/upload-sessions/{uploadSessionId}/complete` | 是 | 会话所有者 | LOW |
| 创建投稿 | POST | `/api/v1/materials/me/submissions` | 是 | 当前用户 | LOW |
| 我的投稿列表 | GET | `/api/v1/materials/me/submissions` | 是 | 当前用户 | LOW |
| 我的投稿详情 | GET | `/api/v1/materials/me/submissions/{materialId}` | 是 | 当前用户 | LOW |
| 修改我的投稿 | PATCH | `/api/v1/materials/me/submissions/{materialId}` | 是 | 当前用户 | LOW |
| 提交审核 | PATCH | `/api/v1/materials/me/submissions/{materialId}/submit-review` | 是 | 当前用户 | MEDIUM |
| 撤回投稿 | PATCH | `/api/v1/materials/me/submissions/{materialId}/withdraw` | 是 | 当前用户 | LOW |
| 重新提交 | PATCH | `/api/v1/materials/me/submissions/{materialId}/resubmit` | 是 | 当前用户 | MEDIUM |
| 后台素材列表 | GET | `/api/v1/materials/admin/items` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台素材详情 | GET | `/api/v1/materials/admin/items/{materialId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 审核通过 | PATCH | `/api/v1/materials/admin/items/{materialId}/approve` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/materials/admin/items/{materialId}/reject` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | MEDIUM |
| 要求修改 | PATCH | `/api/v1/materials/admin/items/{materialId}/request-changes` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | MEDIUM |
| 设为精选 | PATCH | `/api/v1/materials/admin/items/{materialId}/feature` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 取消精选 | PATCH | `/api/v1/materials/admin/items/{materialId}/unfeature` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 下架素材 | PATCH | `/api/v1/materials/admin/items/{materialId}/offline` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档素材 | PATCH | `/api/v1/materials/admin/items/{materialId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 软删除素材 | PATCH | `/api/v1/materials/admin/items/{materialId}/delete` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台分类列表 | GET | `/api/v1/materials/admin/categories` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建分类 | POST | `/api/v1/materials/admin/categories` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改分类 | PATCH | `/api/v1/materials/admin/categories/{categoryId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档分类 | PATCH | `/api/v1/materials/admin/categories/{categoryId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台文件安全摘要列表 | GET | `/api/v1/materials/admin/assets` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 修改文件安全状态 | PATCH | `/api/v1/materials/admin/assets/{assetId}/security-status` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 素材审计列表 | GET | `/api/v1/materials/admin/items/{materialId}/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| material 自检摘要 | GET | `/api/v1/materials/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

### guide

用途：指南、规则、指令、外部交流入口、反馈和指南后台维护。

端口：`8134`，由 `portal-core-service` 承载。来源：`docs/contracts-guide.md`。详情合并稿：`docs/api-reference.md`。

| 接口 | 方法 | 路径 | 认证 | 权限 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 指南首页摘要 | GET | `/api/v1/guides/home` | 否 | 无 | LOW |
| 公开分类列表 | GET | `/api/v1/guides/categories` | 否 | 无 | LOW |
| 公开指南列表 | GET | `/api/v1/guides/articles` | 否 | 无 | LOW |
| 公开指南详情 | GET | `/api/v1/guides/articles/{guideId}` | 否 | 无 | LOW |
| 公开 slug 指南详情 | GET | `/api/v1/guides/articles/by-slug/{slug}` | 否 | 无 | LOW |
| 指南搜索 | GET | `/api/v1/guides/search` | 否 | 无 | LOW |
| 常用指令索引 | GET | `/api/v1/guides/commands` | 否 | 无 | LOW |
| 外部交流入口 | GET | `/api/v1/guides/external-channels` | 否 | 无 | LOW |
| 当前规则版本 | GET | `/api/v1/guides/rules/current` | 否 | 无 | LOW |
| 指定规则版本 | GET | `/api/v1/guides/rules/versions/{ruleVersion}` | 否 | 无 | LOW |
| 提交指南反馈 | POST | `/api/v1/guides/articles/{guideId}/feedback` | 是 | 当前用户 | LOW |
| 后台指南列表 | GET | `/api/v1/guides/admin/articles` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 后台指南详情 | GET | `/api/v1/guides/admin/articles/{guideId}` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建指南 | POST | `/api/v1/guides/admin/articles` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改指南 | PATCH | `/api/v1/guides/admin/articles/{guideId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 提交审核 | PATCH | `/api/v1/guides/admin/articles/{guideId}/submit-review` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 审核通过 | PATCH | `/api/v1/guides/admin/articles/{guideId}/approve` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | MEDIUM |
| 审核拒绝 | PATCH | `/api/v1/guides/admin/articles/{guideId}/reject` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | MEDIUM |
| 要求修改 | PATCH | `/api/v1/guides/admin/articles/{guideId}/request-changes` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | MEDIUM |
| 发布指南 | PATCH | `/api/v1/guides/admin/articles/{guideId}/publish` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 下架指南 | PATCH | `/api/v1/guides/admin/articles/{guideId}/offline` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档指南 | PATCH | `/api/v1/guides/admin/articles/{guideId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 软删除指南 | PATCH | `/api/v1/guides/admin/articles/{guideId}/delete` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 指南版本列表 | GET | `/api/v1/guides/admin/articles/{guideId}/versions` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 指南版本详情 | GET | `/api/v1/guides/admin/articles/{guideId}/versions/{version}` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 恢复指南版本 | PATCH | `/api/v1/guides/admin/articles/{guideId}/versions/{version}/restore` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 指南审计列表 | GET | `/api/v1/guides/admin/articles/{guideId}/audit-logs` | 是 | `ADMIN` 或 `OWNER` | LOW |
| 后台分类列表 | GET | `/api/v1/guides/admin/categories` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建分类 | POST | `/api/v1/guides/admin/categories` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改分类 | PATCH | `/api/v1/guides/admin/categories/{categoryId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档分类 | PATCH | `/api/v1/guides/admin/categories/{categoryId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台渠道列表 | GET | `/api/v1/guides/admin/external-channels` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 创建渠道 | POST | `/api/v1/guides/admin/external-channels` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 修改渠道 | PATCH | `/api/v1/guides/admin/external-channels/{channelId}` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 启用渠道 | PATCH | `/api/v1/guides/admin/external-channels/{channelId}/enable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 禁用渠道 | PATCH | `/api/v1/guides/admin/external-channels/{channelId}/disable` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 归档渠道 | PATCH | `/api/v1/guides/admin/external-channels/{channelId}/archive` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 后台反馈列表 | GET | `/api/v1/guides/admin/feedback` | 是 | `HELPER`、`ADMIN` 或 `OWNER` | LOW |
| 解决反馈 | PATCH | `/api/v1/guides/admin/feedback/{feedbackId}/resolve` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| 忽略反馈 | PATCH | `/api/v1/guides/admin/feedback/{feedbackId}/ignore` | 是 | `ADMIN` 或 `OWNER` | MEDIUM |
| guide 自检摘要 | GET | `/api/v1/guides/admin/ops/summary` | 是 | `ADMIN` 或 `OWNER` | LOW |

## 交接口径

前端开发以本文档确定调用入口和页面所需接口，以 `docs/api-reference.md` 查看完整字段，以模块 `docs/contracts-<module>.md` 核对状态流转、错误码、幂等和审计。发现接口缺口时，不要先在前端兜业务，应回到对应模块契约补齐后再开发。
