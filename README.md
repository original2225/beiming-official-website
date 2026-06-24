# Beiming Official Website

北冥官网是北冥项目的正式线上门户，面向访客、玩家、成员、管理员和运维人员提供统一的官网、用户中心、管理后台和运维控制台能力。

本仓库承载官网前端、后端接口、正式项目文档和自动化验证。系统范围覆盖品牌展示、入服流程、账号权限、成员档案、公告内容、资源分发、社区互动、通知消息、白名单审核、考勤积分、服务器状态、后台运营和服务器资源运维控制。

## 架构概览

后端采用模块化单体架构。统一后端工程位于 `backend/`，Maven 入口为 `backend/pom.xml`，本地服务端口为 `8135`，本地 API 基地址为 `http://127.0.0.1:8135`。业务接口统一使用 `/api/v1/**` 路径前缀。

后端模块按业务边界组织，包括 `auth`、`profile`、`notification`、`content`、`server-status`、`resource`、`admin`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、`ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration`、`cross-platform-notification`、`ops-image-market`、`guide`、`material` 和 `online-map`。

当前后端代码已物理收敛到 `backend/src/main/java`，统一后端挂载 `api-gateway`、五个 core 运行单元和全部业务模块。当前代码中可检索到 32 个控制器入口、765 个映射方法，数据库迁移已提交到 `backend/src/main/resources/db/migration`，迁移范围覆盖 PostgreSQL 公共基础表和 `auth` 到 `changelog` 的业务表。

前端位于 `frontend/`，使用 React、TypeScript、Vite、React Router 和 Zustand，已经包含官网公开页、用户中心、社区页、管理后台基础页面、公共 API client、模块 API 封装和 Vitest 测试。前端默认通过 `VITE_API_BASE_URL` 指向统一后端，本地默认值是 `http://127.0.0.1:8135`。

后端负责统一接口、认证授权、业务规则、审计记录和模块边界。服务器节点上的容器、虚拟机、文件、日志和 Minecraft 实例操作由独立节点执行器承载，不并入官网仓库。

## 本地运行

后端全量测试命令如下。

```powershell
mvn -q -f backend/pom.xml test
```

后端本地启动使用同一个 Maven 工程。

```powershell
mvn -f backend/pom.xml spring-boot:run
```

后端也可以用 Docker Desktop 接入的 WSL 环境启动。先复制本地环境样例并只在本机填写密码。

```bash
cp compose.local.env.example compose.local.env
mvn -q -f backend/pom.xml -DskipTests package
docker compose up --build
```

启动后统一后端入口仍是 `http://127.0.0.1:8135`，健康检查路径为 `http://127.0.0.1:8135/api/v1/unified-backend/health`。当前 Compose 编排 PostgreSQL、统一后端和前端静态服务，前端入口是 `http://127.0.0.1:5173`。Compose 不恢复旧网关或历史 core 入口。迁到 Arch Linux 容器时沿用同一镜像入口和环境变量，把 `compose.local.env` 换成服务器侧的外部密钥配置即可，真实密码不写进仓库。

前端开发和接口联调以正式契约为准。需要调整 API 基地址时，应通过前端环境配置指向 `http://127.0.0.1:8135`，不能在页面代码中写死业务结果或权限状态。

前端本地验证命令如下。

```powershell
cd frontend
npm test
npm run build
```

## 文档体系

正式项目文档保存在 `docs/`，并随仓库提交。当前只保留 `docs/api-reference.md` 和 `docs/system-design.md`。`docs/api-reference.md` 是当前统一后端的总 API 文档，`docs/system-design.md` 是模块化单体的模块设计文档。

本地测试记录、阶段手册、临时分析和个人交接资料保存在 `.local-docs/`，不提交到仓库。可复用的规则、接口和验收要求需要沉淀到 `docs/`。

## 开发约定

开发以正式文档和 API 契约为准。每个模块先补齐契约，再补齐本地测试文档和自动化测试，随后实现代码并执行相关测试。

模块之间通过明确接口协作。业务模块不能直接读取其他模块的数据表，前端不能绕过后端契约吞业务逻辑，后台运维能力不能混入玩家资源下载或服务器状态展示模块。

提交内容不得包含真实密码、真实 token、数据库连接串、本地环境文件、构建产物、运行日志或 `.local-docs/` 内容。
