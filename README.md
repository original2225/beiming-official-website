# Beiming Official Website

中文名：北冥官网

这是北冥官网的正式项目仓库。后端已经完成本地物理单体收束，当前以 `backend` 根目录下的单 Spring Boot 工程作为统一后端开发入口。

## 当前状态

当前唯一后端 Maven 入口是 `backend/pom.xml`。当前本地后端服务端口是 `8135`，本地联调默认入口统一为 `http://127.0.0.1:8135`。业务路径保持 `/api/v1/**` 原样。

五个 core 模块源码已经物理位于 `backend/src/main/java`，由 `backend/pom.xml` 统一编译和测试，不再通过 build-helper 装配旧 core 源码目录。`api-gateway:8125` 和 `8130` 到 `8134` 的五个 core 端口只作为历史入口、模块来源、回滚引用或外部证据样板引用保留，不是当前本地 Maven 启动入口。

真实生产入口切流、真实集中配置、真实持久化审计、真实观测、回滚窗口和审批证据仍未完成。当前仓库内的 readiness 和样板只能证明本地门禁与脱敏证据结构存在，不能证明生产流量已经切到 `8135`。

## 本地验证

后端全量测试命令：

```powershell
mvn -q -f backend/pom.xml test
```

## 文档

- `docs/requirements.md`：需求文档
- `docs/system-design.md`：系统设计文档
- `docs/development-governance.md`：开发治理文档
- `docs/contracts-common.md`：P0 公共契约
- `docs/contracts-<module>.md`：各模块独立 API 契约
- `AGENTS.md`：开发协作规则

正式需求、系统设计、开发治理、接口契约和模块验收标准都保存在 `docs/` 并随仓库提交。测试文档、测试记录、本地阶段资料、临时分析和本地模块开发指导文档只保存在 `.local-docs/`，不上传仓库。

## 开发原则

先定边界，再写代码。一次只做一个模块。新增模块必须兼容已有模块，不允许顺手修改无关代码。涉及生产切流、旧入口退役、集中配置、持久化审计或观测接入时，必须先补正式契约、本地测试记录和自动化守卫。
