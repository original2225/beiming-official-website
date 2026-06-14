# 北冥官网 community API 契约

版本：1.0

## 文档定位

本文档是 `community` 模块在当前模块化单体架构下的正式 API 契约。当前仓库唯一后端 Maven 入口是 `backend/pom.xml`，本地后端服务端口是 `8135`，所有接口通过 `http://127.0.0.1:8135` 访问，业务路径保持 `/api/v1/**` 原样。

当前实现来源为 `backend/src/main/java/cn/beiming/community/CommunityModule.java`。`community` 在运行上由 `engagement-core` 装配进统一后端进程，模块数据归属、路径前缀、错误码、认证方式、审计规则和状态流转仍按本文档独立维护。历史独立服务入口和历史端口只允许作为追溯字段或脱敏证据引用，不作为当前开发、联调、测试或前端调用入口。

## 模块边界

`community` 负责板块、帖子、评论、点赞、收藏、投票、举报、工单和处罚。它不直接读写其他模块主数据。跨模块协作通过统一后端内的正式接口或认证上下文完成，可以保存高频展示快照，但快照不是主数据。

## 基础契约

本模块遵守 `docs/contracts-common.md`。成功响应统一为 `code=0`、`message=success`、`data=<payload>`。分页响应统一放在 `data.items`、`data.page`、`data.pageSize` 和 `data.total`。认证请求统一使用 `Authorization: Bearer <token>`。后台接口必须校验 `OWNER`、`ADMIN`、`HELPER` 或模块能力点。运维类接口还必须校验细粒度能力点、二次确认和高风险审批。

## 接口清单

当前代码暴露 64 个 `community` 路由。下表根据当前控制器注解生成。

| 方法 | 路径 | 认证与权限 | 请求字段 | 响应字段 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/v1/community/boards` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/community/boards/{boardId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `boardId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/community/posts` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/community/posts/{postId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `postId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/community/posts/{postId}/comments` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `postId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/community/polls/{pollId}` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 路径参数 `pollId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `GET` | `/api/v1/community/search` | 公开或登录态按资源可见范围过滤；无；按公开可见范围过滤 | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/community/me/posts` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/me/posts/{postId}` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/me/posts/{postId}/submit` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/me/posts/{postId}/withdraw` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/me/posts/{postId}/comments` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/me/comments/{commentId}` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/me/comments/{commentId}/archive` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/me/posts/{postId}/like` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `DELETE` | `/api/v1/community/me/posts/{postId}/like` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `POST` | `/api/v1/community/me/comments/{commentId}/like` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `DELETE` | `/api/v1/community/me/comments/{commentId}/like` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `POST` | `/api/v1/community/me/posts/{postId}/favorite` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `DELETE` | `/api/v1/community/me/posts/{postId}/favorite` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `POST` | `/api/v1/community/me/polls/{pollId}/votes` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `pollId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/me/posts/{postId}/reports` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/me/comments/{commentId}/reports` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/community/me/reports` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/community/me/tickets` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/community/me/tickets` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 无请求体 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/community/me/tickets/{ticketId}` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `ticketId`；无请求体 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/community/me/tickets/{ticketId}` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `ticketId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/me/tickets/{ticketId}/close` | USER、HELPER、ADMIN 或 OWNER，且只能访问本人数据；COMMUNITY_SELF | 路径参数 `ticketId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/community/admin/boards` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `POST` | `/api/v1/community/admin/boards` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/boards/{boardId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `boardId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/boards/{boardId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `boardId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/community/admin/posts` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/community/admin/posts/{postId}` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `postId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/community/admin/posts/{postId}/approve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/community/admin/posts/{postId}/reject` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/posts/{postId}/request-changes` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/posts/{postId}/offline` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/posts/{postId}/archive` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/posts/{postId}/delete` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `postId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `GET` | `/api/v1/community/admin/comments` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `PATCH` | `/api/v1/community/admin/comments/{commentId}/approve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `PATCH` | `/api/v1/community/admin/comments/{commentId}/reject` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/comments/{commentId}/offline` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `commentId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/community/admin/reports` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/community/admin/reports/{reportId}` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `reportId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/community/admin/reports/{reportId}/assign` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `reportId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/reports/{reportId}/resolve` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `reportId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/reports/{reportId}/dismiss` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `reportId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/community/admin/tickets` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的分页列表或摘要对象 | LOW |
| `GET` | `/api/v1/community/admin/tickets/{ticketId}` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `ticketId`；可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | LOW |
| `PATCH` | `/api/v1/community/admin/tickets/{ticketId}/assign` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `ticketId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/admin/tickets/{ticketId}/messages` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `ticketId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/tickets/{ticketId}/status` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `ticketId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `POST` | `/api/v1/community/admin/penalties` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/penalties/{penaltyId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `penaltyId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/penalties/{penaltyId}/revoke` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `penaltyId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | HIGH |
| `POST` | `/api/v1/community/admin/polls` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/polls/{pollId}` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `pollId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/polls/{pollId}/open` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `pollId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `PATCH` | `/api/v1/community/admin/polls/{pollId}/close` | HELPER 只读；ADMIN/OWNER 写操作；高风险按能力点；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 路径参数 `pollId`；JSON 请求体；字段按当前控制器校验，必须包含业务动作需要的主体字段、状态原因、二次确认或幂等键 | 统一响应中的业务对象；写操作返回更新后的资源摘要、状态流转结果或任务摘要 | MEDIUM |
| `GET` | `/api/v1/community/admin/audit-logs` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 分页审计或日志摘要对象，包含操作者、目标、动作、结果、失败原因和时间 | LOW |
| `GET` | `/api/v1/community/admin/ops/summary` | HELPER、ADMIN 或 OWNER；COMMUNITY_READ for GET；COMMUNITY_WRITE for mutation | 可选查询 `page`、`pageSize`、`keyword`、`status`、`sort`，具体过滤字段以控制器校验为准 | 模块运行摘要、计数、状态、降级原因和审计摘要 | LOW |

## 状态流转

模块内资源默认遵守 `DRAFT`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`OFFLINE`、`ARCHIVED`、`DELETED` 的通用状态模型。没有状态字段的健康、自检、只读摘要接口不产生业务状态流转。写接口必须由服务端判定允许流转，前端不得只靠本地状态决定审核、发布、删除、恢复、审批或任务完成。

## 幂等与并发

创建、提交、审核、发布、撤回、取消、归档、删除、任务触发、投递、同步、审批和重试类接口必须支持服务端幂等保护。同一资源在同一状态下重复执行同一动作时，应返回已有结果或明确的状态冲突错误，不得重复生成主数据、通知、积分、任务或审计。并发更新以服务端当前状态为准，冲突返回 `43001` 或模块内更具体错误码。

## 错误码

通用错误码沿用 `docs/contracts-common.md`。本模块字段校验失败返回 `40001`，未登录返回 `41000`，令牌格式错误返回 `41003`，角色或能力点不足返回 `42001` 或 `42002`，高风险操作缺少确认或审批返回 `42003` 或 `42004`，资源不存在返回 `43000`，状态冲突返回 `43001`，幂等冲突返回 `43002`，外部依赖不可用返回 `46000`，跨模块调用超时返回 `46001`。模块内部错误使用 `51xxx` 到 `59xxx`，不得复用其他模块的专用语义。

## 失败降级

公开读取接口失败时，前端按模块契约做局部降级，不能整页空白。登录、审核、积分、白名单、通知、运维任务、备份恢复、告警投递和高风险审批不能伪造成成功。外部节点执行器、Cloudreve、外部通知渠道、镜像仓库、观测平台或其他外部依赖未接入时，接口必须返回明确降级状态、失败原因或阻断项。

## 审计要求

所有后台写操作、高风险操作、状态流转、审批、投递、同步、任务触发、资源上下架、删除、归档、恢复、角色权限相关操作都必须写审计。审计至少记录 `requestId`、操作者、角色、目标类型、目标 ID、动作、风险等级、原因、参数摘要、操作前状态、操作后状态、结果、失败原因和时间。只读公开接口不强制写审计。

## 验收口径

`community` 完成时必须满足本文档全部路由都由 `backend/pom.xml` 统一编译和测试；当前调用入口只使用 `http://127.0.0.1:8135`；接口路径、响应格式、认证方式、错误码、分页、幂等、状态流转、失败降级和审计字段与本文档一致；不得恢复旧独立 Maven 入口；不得把其他模块主数据或真实节点执行逻辑塞进本模块。自动化验证统一运行 `mvn -q -f backend/pom.xml test`。
