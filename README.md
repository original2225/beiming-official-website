# Beiming Official Website

北冥官网是北冥项目的正式线上门户，面向访客、玩家、成员、管理员和运维人员提供统一的官网、用户中心、管理后台和运维控制台能力。

本仓库承载官网前端、后端接口、正式项目文档和自动化验证。系统范围覆盖品牌展示、入服流程、账号权限、成员档案、公告内容、资源分发、社区互动、通知消息、白名单审核、考勤积分、服务器状态、后台运营和服务器资源运维控制。

## 架构概览

后端采用模块化单体架构。统一后端工程位于 `backend/`，Maven 入口为 `backend/pom.xml`，本地服务端口为 `8135`，本地 API 基地址为 `http://127.0.0.1:8135`。业务接口统一使用 `/api/v1/**` 路径前缀。

后端模块按业务边界组织，包括 `auth`、`profile`、`notification`、`content`、`server-status`、`resource`、`admin`、`onboarding`、`exam`、`whitelist`、`attendance`、`community`、`activity`、`calendar`、`changelog`、`ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration`、`cross-platform-notification`、`ops-image-market`、`guide`、`material` 和 `online-map`。

前端负责官网公开页、用户中心、管理后台和运维控制台界面。后端负责统一接口、认证授权、业务规则、审计记录和模块边界。服务器节点上的容器、虚拟机、文件、日志和 Minecraft 实例操作由独立节点执行器承载，不并入官网仓库。

## 本地运行

后端全量测试命令如下。

```powershell
mvn -q -f backend/pom.xml test
```

后端本地启动使用同一个 Maven 工程。

```powershell
mvn -f backend/pom.xml spring-boot:run
```

前端开发和接口联调以正式契约为准。需要调整 API 基地址时，应通过前端环境配置指向 `http://127.0.0.1:8135`，不能在页面代码中写死业务结果或权限状态。

## 文档体系

正式项目文档保存在 `docs/`，并随仓库提交。`docs/requirements.md` 是需求文档，`docs/system-design.md` 是系统设计文档，`docs/development-governance.md` 是开发治理文档，`docs/contracts-common.md` 是公共接口契约，`docs/contracts-<module>.md` 是各模块独立 API 契约，`docs/api-reference.md` 是接口总览。

本地测试记录、阶段手册、临时分析和个人交接资料保存在 `.local-docs/`，不提交到仓库。可复用的规则、接口和验收要求需要沉淀到 `docs/`。

## 开发约定

开发以正式文档和 API 契约为准。每个模块先补齐契约，再补齐本地测试文档和自动化测试，随后实现代码并执行相关测试。

模块之间通过明确接口协作。业务模块不能直接读取其他模块的数据表，前端不能绕过后端契约吞业务逻辑，后台运维能力不能混入玩家资源下载或服务器状态展示模块。

提交内容不得包含真实密码、真实 token、数据库连接串、本地环境文件、构建产物、运行日志或 `.local-docs/` 内容。
