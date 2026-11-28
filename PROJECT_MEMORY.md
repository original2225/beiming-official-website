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

## 2026-11-27｜准入体系重构：考核分服型 → B 方案免审核 → 白名单系统录入 → 持久化治理

### 主题
花蝉定下准入流程四件事：①服型分卷（生电/整合包考核内容不同）；②B 方案（邀请码只用于注册、管理员码免审核、考过直接给白名单）；③邀请码两套不共用、白名单改后台「系统录入」；④治「重启就丢数据」。

### 已完成（四个提交，全部已推送）

| commit | 内容 |
|---|---|
| `65ab320` | 考核按服型分卷，绑定挪到申请邀请码时（前置批次） |
| `a787025` | **B 方案**：`core/InvitationOriginResolver` 契约 + 管理员码免审核 + 考过直发白名单 |
| `ebd1eae` | **持久化治理**：题库/白名单补回读链路与缺失列 |
| `480c2a9` | **白名单后台系统录入**：`POST /whitelist/admin/applications/direct` + `AdminWhitelistDirectPage` |

### 决定与偏好（花蝉定的，带日期）

1. **2026-11-27 服型必须在 onboarding 选方向时定**：考核在申请之前，服型不能拖到申请时才选，否则考生考的是通用卷、申请时才发现该考生电卷，等于要重考。三处 `SERVER_TYPES` 常量（onboarding/exam/whitelist）必须同名同值。
2. **2026-11-27 B 方案**：邀请码只作注册凭证；`ADMIN` 类邀请码 = 免人工审核；考核 `PASSED` 即直接发白名单。
3. **2026-11-27 `邀请码是两套`**：官网注册码与北冥皮肤站码不共用，别想对齐成一套。
4. **2026-11-27 白名单不走邀请码**：改官网后台「系统录入」，管理员直接把玩家录进白名单。注册要码、白名单不要码。
5. **放行条件必须两条同时成立**：`result==PASSED` 且来源是 `ADMIN` 码。PLAYER 码或查不到来源一律留 `PENDING_REVIEW`；管理员码但没考过更早被 `409/44010` 拒——**管理员码不豁免考核**。

### 关键实现

- 跨模块只能走 `cn.beiming.core` 契约（`compatibilityAndBoundaryContract` 扫源码，whitelist 出现 `cn.beiming.auth.` 直接红灯）。新增 `InvitationOriginResolver`。
- `autoApprove` / `directEntry` 都必须调 `activateProfile`，失败降 `APPROVAL_BLOCKED` 交人工；**绝不允许「状态 APPROVED 但档案空/编造」**。
- 系统录入不编造考核数据：`examSessionId=direct-entry`、`examPassedAt=null`；`userId` 可空（纯游戏玩家用 `direct-<序号>` 占位）。

### 踩过的坑

**1. 假持久化第三次复发（最值钱的教训）**
`ExamPersistence` / `WhitelistPersistence` 只有写方法、**一个读方法都没有**。接口 200、库里有行、审计也写了，但重启后内存只剩硬编码 5 道英文种子题，题库/申请/审核状态全"消失"，无异常无告警。前两次是 auth 的 `persistRegister` 不写 + `loadUsers` 不读回。
→ 改法：加 `loadQuestions/loadTemplates/loadApplications`，`seed()` 末尾覆盖式回读；**`idSeq` 要跟着推进**否则新申请 `wl-1001` 撞老单主键。

**2. 表里根本没有这些列**
`exam_questions` 没有 `server_type`、`whitelist_applications` 没有 `server_type`/`qq_group_nickname`，一直只活在 Java 对象里。补 `V19__add_exam_server_type.sql`（`ADD COLUMN IF NOT EXISTS`，NULL = 两服共用）。

**3. `copyQuestion` 漏拷 `serverType`（最阴）**
`updateQuestion` 走 `copyQuestion(old)` 再改字段，它只复刻构造参数那批，`serverType` 是构造后单独赋的 → **任何一次 update（含 DRAFT→ACTIVE 激活）都把服型洗成 null**，生电专属题悄悄变两服通用题。单元测试全绿，只有真跑「建题→激活→查库」才暴露。

**4. `mvn` 绝对不能并行**
一边改代码一边后台跑全量测试，会得到 `703 run / 42 failures / 276 errors` 全片 `Failed to load ApplicationContext`——共享 `target/` 和固定测试端口互撞。这类红灯不是代码问题，别改代码；清进程后单跑即可。

**5. 编辑 `*Module.java`（一文件多类）时 patch 易把两行拼一起**
锚点末尾多带一个换行，替换后 `synchronized ... {` 会和下一行 `UserAccount target = ...` 并成一行。每次 patch 后看 diff 前 3 行。

**6. Surefire 多类用逗号**：`-Dtest=A,B,C`，用 `+` 报 `No matching tests`。

**7. 加接口要同步更新路由快照**
`AdmissionCoreApiContractTest.registersExactlySecondBatchAndSelfApiRoutes` 断言路由条数（86→87）——这是防「悄悄多出接口」的闸门，加完接口必须一起改。

### 测试基线
- 后端 **711/711** BUILD SUCCESS
- 前端 `tsc` 0 错误、`npm test` **33/33**
- 持久化验收用 Testcontainers 真库（`ExamBankReloadIntegrationTest` 直接 `SELECT server_type` 断言列值）——**内存模式下「读得回来」根本无从验证，这正是假持久化能一直绿的原因**

### 待办 / 下一步

1. **容器已重建到 `fd18bdf`**，白名单系统录入两种场景已在真容器实测通过（占位玩家 `wl-1008` / 真实用户 `wl-1006`）。
2. 题库仍是 5 道英文占位题（`"Rules apply before whitelist approval."` 这类），测试硬编码引用其 ID 不能删。**生产环境需要一套真实中文题库**：`docs/rules/exam-question-bank.seed.json` 已有 25 题（4 通用/14 生电/7 整合包），`scripts/seed-exam-bank.py` 可灌，但需 admin token。
3. `WhitelistPage.tsx` 玩家自助申请页与新的「后台系统录入」是两条独立入口，产品上要确认两者都保留还是要收敛。
4. 邀请码 `used_count` 虚高未回退（`OKMSG-CODE` 5/50、`PWTEST-CODE` 2/50，使用记录已删）。

### 重建后实测发现的问题（52000 三连击，已修）

**教训：单元测试全绿不等于功能可用。** 前端 tsc 0 错、`npm test` 33/33、后端全量 711/711 绿，推上去重建容器后调真接口直接 `{"code":52000,"message":"whitelist internal error"}`，连报三次，每次根因都不同，全是 schema 里按「必经考核」写死的约束：

| # | 报错约束 | 原因 | 修法 |
|---|---|---|---|
| 1 | `onboarding_application_id` NOT NULL | 系统录入无前置申请单 | V20 放开非空，NULL = 非 onboarding 来源 |
| 2 | `CHECK (exam_handoff_version > 0)` 与其 onboarding 孪生约束 | 无交接，版本号只能 0 | V21 改成 `>= 0`，0 = 无交接 |
| 3 | `CHECK (status IN ('ACTIVATED','FAILED'))` on `whitelist_profile_activations` | 新状态 `PENDING_USER_BINDING` 不在白名单 | **不写这张从表**——占位玩家没有建档动作就不该有记录 |

**为什么测试全绿还漏**：内存模式只走 Java 对象、不碰真库约束；`createEvidenceTables()` 的 DDL 是测试自己写的，**不含 Flyway 迁移里的约束**。

→ **判别口径：新增数据路径（尤其绕过原流程的）必须在跑 Flyway 真迁移的容器里做一次真写操作。内存绿、证据表绿都不算数。**

**顺带修的真缺陷**：`WhitelistExceptionHandler.handleUnexpected` 只 `return 500` 不打日志，线上 500 是哑巴。已补 `LOG.error` 带 requestId + 堆栈——这次三次根因全靠它才定位。

**业务口径**（花蝉 2026-11-27）：管理员有权录还没注册官网的玩家，`userId` 空时用 `direct-<序号>` 占位；这类人档案状态是 `PENDING_USER_BINDING`（等关联），不是 `APPROVAL_BLOCKED`（建档失败）、更不谎报 `ACTIVATED`。

## 2026-11-28｜attendance 考勤积分：补回读链路（假持久化第三例收口）

### 主题
按第 27 条固定模块顺序，whitelist 之后做 attendance。核实发现它与 exam、whitelist 同病：`AttendancePersistence` 只有 7 个 persist、一个 load 都没有，`AttendanceStore` 15 个内存 Map 是唯一存储；接口 200、库里有行、审计也写，但重启后第一批玩家的考勤全丢，无异常无告警。线上 `attendance_accounts` 当时 0 行，尚未暴雷。

### 已完成
| 项 | 内容 |
|---|---|
| API 文档 | `docs/api-reference.md` attendance 章节补「持久化必须闭环到读」口径：六个 load、覆盖式回读、重建派生索引、推进主键序号、月度周期回读防重复扣 |
| 接口 | `AttendancePersistence` 增 `loadAccounts/loadLedgers/loadContributions/loadMonthlyRuns/loadRemovalCandidates/loadLeaderboardSnapshots`；`NoopAttendancePersistence` 全返空集合 |
| Postgres 实现 | `AttendancePostgresPersistence` 六个 load 用 `queryForList` + 显式列别名映射成 camelCase；jsonb 列 `::text` 读回再用 ObjectMapper 解 |
| 模块回读 | `AttendanceStore.hydrate()` 在 `attendanceStore` Bean 构造后调用，六张表覆盖式读回（库里有行就以库为准整体替换，不合并）；重建 `userId/memberId/applicationId → accountId` 派生索引与贡献来源去重键；`advanceIdSeq()` 把最大数字后缀抬到 `idSeq` 之上防撞主键 |
| 测试 | 新增 `AttendanceReloadIntegrationTest`（Testcontainers 真库）：写账号+流水+贡献+月度结算 → `new AttendanceStore(...) + hydrate()` 重建实例 → 断言账户余额/状态、三个派生索引、流水类型与 delta、贡献字段、月度 run 全部一致；另加覆盖式回读用例（内存幽灵账户被清、idSeq 推进） |

### 踩过的坑
1. **加了回读，旧测试的「删库=清空」假设失效**：`AttendancePostgreSqlPersistenceIntegrationTest` 用 `@DirtiesContext(BEFORE_EACH)` 重建上下文，但同一个 Testcontainers 库数据不随上下文重建清空，新上下文启动即 `hydrate()` 把上一用例的行读进内存；`@BeforeEach` 的 SQL 删库只清了库、没清内存，后续用例跑在幽灵账户上 → `contribution` 报 53000、`candidate confirm` 读到空状态。修法：给 store 加同包可见 `reset()`，在 `@BeforeEach` 删库后同步清内存。**教训：一旦模块有了启动回读，测试的隔离就必须同时管库和内存两层。**
2. 月度扣分会被「本周期有贡献」跳过（`noContributionInCycle`）：贡献写在 2026-05，扣分也放 2026-05 就不扣。测试里扣分改用 2026-06。
3. 测试断言要看内存态本身（`accountsSnapshot()` 等），不能走 HTTP 读——新起实例的 HTTP 读会被内存响应模型冒充成持久化，这正是假持久化能一直绿的原因。

### 测试基线
- 后端 **714/714** BUILD SUCCESS（712 基线 + 新增 AttendanceReloadIntegrationTest 2 条，无掉项）
- attendance 模块单独 20/20 全绿（ApiContract 7 / PostgreSql 6 / ProductionHardening 1 / Reload 2 / RequestDatabaseFlow 4）

### 待办 / 下一步
1. 本轮只做了「写→读」闭环，**未新增数据路径**，因此不需要新迁移；V11 六张表的 NOT NULL/CHECK 未动（本轮无绕过原流程的新写入）。若日后放开 attendance 某条新写入路径，仍须先全库查该表 CHECK/NOT NULL 再上新迁移。
2. `attendance_leaderboard_snapshots` 只留档排障、不参与实时榜单，回读不覆盖内存（内存不存快照），仅推进序号；如需「重启后仍能看历史快照」再单开。
3. 下一模块按顺序是 `community`，动它之前先做同样的「Persistence 有没有读方法 + 模块启动是否调它」四步判定。
