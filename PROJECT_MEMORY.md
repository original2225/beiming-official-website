# 北冥官网 · 项目记忆


## 2026-11-25｜GitHub 授权与推送
- 主题：预览面板加载 `https://github.com/login/device` 报 `加载失败 (-100): ERR_CONNECTION_CLOSED`；确认该地址是 GitHub 外部登录页，不是本地服务，无需重启本地服务器。
- 知识与发现：预览面板访问外网失败时应在系统浏览器打开；已生成设备码 `DEB6-D5C4` 并等待授权。`beiming-drive` 远端可免密拉取，说明 Windows 凭据管理器已保存 GitHub 凭据；若仓库存在可直接推送。
- 决定与偏好：优先走系统浏览器设备授权；同时提出更快的方案：新建空仓库 `beiming-ui`（不勾 README）后直接推送，跳过验证码流程。
- 已完成：已在系统浏览器打开 GitHub 登录页并生成设备码 `DEB6-D5C4`。
## 2026-11-26｜登录注册验收（花蝉逐项验收）

### 主题
花蝉验收登录注册流程，指出「注册失败只弹一句『注册失败，请稍后重试』，没有给明失败原因」。

### 知识与发现

**1. 错误提示三层根因**

- `fetch` 被 CORS 拦 / 网络断 / 后端没起时只抛 `TypeError`，拿不到状态码和响应体；原 `client.ts` 原样上抛，页面 `e instanceof ApiError` 为 false → 落到泛化兜底文案。
- 后端 message 是英文（`invitation exhausted`、`invalid request`），即便走到 ApiError 分支用户也读不懂。
- 非 2xx 若不读响应体，就会丢掉后端返回的具体业务原因。

**2. 他截图那次失败的真正原因**

表单填 `PLAYER-CODE-1`（used 10/10 已用尽）→ 后端实际返回 `43114 invitation exhausted`。账号密码都没问题，是邀请码没了。

**3. curl 验不出 CORS 问题**

curl 不管 CORS，打接口永远正常。前三轮就是因为只用 curl 验证，才误判「注册是好的」。**必须用真实浏览器验。**

**4. 前端组件接口易错点**

- `DataTable` 列定义字段是 `header`（不是 `title`），行数据 prop 是 `data`（不是 `rows`）。
- `StatusBadge` 只接受 `{ status }` 且只认 6 种固定值，业务自定义状态（EXHAUSTED/EXPIRED）要自己画。

**5. 邀请码存储**

内存里按 `code_value` 明文查找（`WHERE code_value IS NOT NULL`），`code_hash` 不参与查询。手插测试数据只要填 `code_value` 即可使用。

### 决定与偏好（花蝉定的）

1. **注册限流改为按 IP + 10 分钟窗口**，每 IP 5 次。原先是写死 `"global"` 的全局计数器、无时间窗口，任何人试错 5 次全站就没人能注册，只能重启后端恢复（当天卡住测试 3 次）。
2. **邀请码用尽时提示「请向管理员索取新的邀请码」**，并把邀请码管理页做出来。
3. **密码改边输边提示**，不合格时输入框变红并写明原因。

### 已完成

- `client.ts` 兜住 `TypeError`/超时/响应体异常，统一转成带原因的 `ApiError`。
- 新增 `api/errorMessages.ts` 的 `describeApiError`：30+ 业务码翻中文；未收录的显示「后端原文（错误码 N）」，宁可显示英文也不说废话。
- 注册/登录弹窗接入。
- `AuthModule` 注册限流改按 IP 分桶 + 10 分钟窗口（`AttemptWindow` record + `nextAttempt` + `clientIp`）。
- 注册密码框边输边提示（还差几位 / 缺哪类字符 / 密码可用）。
- 新增 `AdminInvitationsPage` 邀请码管理页：列表、状态筛选、新建、禁用；注册路由 `/admin/invitations` 与侧边栏入口。
- 测试：后端 65/65、前端 33/33、tsc 0 错误。

### 待办 / 下一步

- 邀请码管理页的「新建」流程未在浏览器中实测（接口已验证可用）。
- 注册限流的 IP 取自 `X-Forwarded-For` 首个地址，回退 `remoteAddr`；公网部署时需确认前置代理会正确透传。
- 临时测试账号 `adminview01` 与测试邀请码 `ADMINVIEW-CODE` 仍留在库中，可清理。
