# 北冥官网后端 API 总览

版本：0.1

## 文档定位

本文档是北冥官网后端 API 的整体索引和汇总说明，用于让维护者快速查看服务边界、端口、契约文件、测试入口和 API 覆盖规模。它不替代任何模块独立契约。正式开发、验收和变更仍以 `docs/contracts-<module>.md` 为准，公共响应、错误码、分页、认证和审计规则仍以 `docs/contracts-common.md` 为准。

面向前端开发的一体化 API 全文查阅文档见 `docs/api-reference.md`。该文档由公共契约和全部模块契约合并生成，方便前端统一检索路径、字段和响应格式。

本汇总覆盖当前仓库 `docs/contracts-*.md` 中除 `contracts-common.md` 和本文档之外的 28 个业务或平台模块，以及 `business-core`、`admission-core`、`engagement-core`、`ops-core`、`portal-core` 和 `unified-backend` 六个运行入口或候选入口契约。第一批、第二批、第三批、第四批、第五批和第六期已完成运行合并并入仓。第九轮新增 `unified-backend-service:8135` 只作为并行候选入口，不计入当前生产入口退役结果。当前契约表中的 `METHOD path` 记录总数为 780，其中第三批四个业务模块在 `engagement-core-service:8132` 中承载 149 个业务路由，第四批六个后台运维控制面模块和第六期跨平台通知控制面在 `ops-core-service:8133` 中承载 219 个业务路由，第五批后三个玩家门户体验模块在 `portal-core-service:8134` 中承载 108 个业务路由，`engagement-core` 自身提供 3 个运行单元自检和诊断路由，`ops-core` 自身提供 5 个运行单元自检、诊断和 HTTP smoke 路由，`portal-core` 自身提供 5 个运行单元自检、诊断和 HTTP smoke 路由，`api-gateway` 自身提供 8 个网关健康、路由、上游、日志和运行拓扑路由，`unified-backend` 自身提供 5 个候选入口自检、挂载和 HTTP smoke 路由。

## 全局接口规则

所有业务成功响应使用统一结构，`code=0` 表示成功，`message` 为成功或错误摘要，业务数据放在 `data` 中。分页响应统一在 `data` 内返回 `items`、`page`、`pageSize` 和 `total`。

受保护接口统一使用 `Authorization: Bearer <token>`。后台、运维和节点相关接口必须继续校验角色、能力点、风险等级、二次确认、幂等键和审计字段。业务服务不能自行实现登录逻辑，不能直接读取其他服务数据表，跨模块读取必须通过正式接口、网关认证上下文或受控适配层。

玩家可见资源、服务器状态展示、后台运维控制和节点真实执行必须保持边界隔离。`resource` 只负责玩家资源分发，`server-status` 只负责玩家可见状态，`ops-control` 负责运维控制面，`node-daemon` 负责节点侧受控执行。

## 服务与契约总表

| 模块 | 服务目录 | 端口 | API 数 | 正式契约 | 本地测试文档 | 自动化测试入口 |
| --- | --- | ---: | ---: | --- | --- | --- |
| `activity` | `backend/engagement-core-service` | 8132 | 41 | `docs/contracts-activity.md` | `.local-docs/tests-activity.md` | `mvn -q -f backend/engagement-core-service/pom.xml test` |
| `admin` | `backend/business-core-service` | 8130 | 10 | `docs/contracts-admin.md` | `.local-docs/tests-admin.md` | `mvn -q -f backend/business-core-service/pom.xml test` |
| `admission-core` | `backend/admission-core-service` | 8131 | 2 | `docs/contracts-admission-core.md` | `.local-docs/tests-admission-core.md` | `mvn -q -f backend/admission-core-service/pom.xml test` |
| `alerting` | `backend/ops-core-service` | 8133 | 24 | `docs/contracts-alerting.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/ops-core-service/pom.xml test` |
| `api-gateway` | `backend/api-gateway-service` | 8125 | 8 | `docs/contracts-api-gateway.md` | `.local-docs/tests-api-gateway.md` | `mvn -q -f backend/api-gateway-service/pom.xml test` |
| `attendance` | `backend/admission-core-service` | 8131 | 21 | `docs/contracts-attendance.md` | `.local-docs/tests-attendance.md` | `mvn -q -f backend/admission-core-service/pom.xml test` |
| `auth` | `backend/business-core-service` | 8130 | 20 | `docs/contracts-auth.md` | `.local-docs/tests-auth.md` | `mvn -q -f backend/business-core-service/pom.xml test` |
| `backup-recovery` | `backend/ops-core-service` | 8133 | 25 | `docs/contracts-backup-recovery.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/ops-core-service/pom.xml test` |
| `business-core` | `backend/business-core-service` | 8130 | 3 | `docs/contracts-business-core.md` | `.local-docs/tests-business-core.md` | `mvn -q -f backend/business-core-service/pom.xml test` |
| `calendar` | `backend/engagement-core-service` | 8132 | 21 | `docs/contracts-calendar.md` | `.local-docs/tests-calendar.md` | `mvn -q -f backend/engagement-core-service/pom.xml test` |
| `changelog` | `backend/engagement-core-service` | 8132 | 23 | `docs/contracts-changelog.md` | `.local-docs/tests-changelog.md` | `mvn -q -f backend/engagement-core-service/pom.xml test` |
| `cloudreve-sync` | `backend/ops-core-service` | 8133 | 16 | `docs/contracts-cloudreve-sync.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/ops-core-service/pom.xml test` |
| `community` | `backend/engagement-core-service` | 8132 | 64 | `docs/contracts-community.md` | `.local-docs/tests-community.md` | `mvn -q -f backend/engagement-core-service/pom.xml test` |
| `content` | `backend/business-core-service` | 8130 | 55 | `docs/contracts-content.md` | `.local-docs/tests-content.md` | `mvn -q -f backend/business-core-service/pom.xml test` |
| `cross-platform-notification` | `backend/ops-core-service` | 8133 | 36 | `docs/contracts-cross-platform-notification.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/ops-core-service/pom.xml test` |
| `engagement-core` | `backend/engagement-core-service` | 8132 | 3 | `docs/contracts-engagement-core.md` | `.local-docs/tests-engagement-core.md` | `mvn -q -f backend/engagement-core-service/pom.xml test` |
| `exam` | `backend/admission-core-service` | 8131 | 28 | `docs/contracts-exam.md` | `.local-docs/tests-exam.md` | `mvn -q -f backend/admission-core-service/pom.xml test` |
| `guide` | `backend/portal-core-service` | 8134 | 41 | `docs/contracts-guide.md` | `.local-docs/tests-guide.md` | `mvn -q -f backend/portal-core-service/pom.xml test` |
| `material` | `backend/portal-core-service` | 8134 | 33 | `docs/contracts-material.md` | `.local-docs/tests-material.md` | `mvn -q -f backend/portal-core-service/pom.xml test` |
| `node-daemon` | `backend/node-daemon-service` | 8117 | 15 | `docs/contracts-node-daemon.md` | `.local-docs/tests-node-daemon.md` | `mvn -q -f backend/node-daemon-service/pom.xml test` |
| `notification` | `backend/business-core-service` | 8130 | 19 | `docs/contracts-notification.md` | `.local-docs/tests-notification.md` | `mvn -q -f backend/business-core-service/pom.xml test` |
| `onboarding` | `backend/admission-core-service` | 8131 | 15 | `docs/contracts-onboarding.md` | `.local-docs/tests-onboarding.md` | `mvn -q -f backend/admission-core-service/pom.xml test` |
| `online-map` | `backend/portal-core-service` | 8134 | 34 | `docs/contracts-online-map.md` | `.local-docs/tests-online-map.md` | `mvn -q -f backend/portal-core-service/pom.xml test` |
| `ops-control` | `backend/ops-core-service` | 8133 | 31 | `docs/contracts-ops-control.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/ops-core-service/pom.xml test` |
| `ops-core` | `backend/ops-core-service` | 8133 | 5 | `docs/contracts-ops-core.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/ops-core-service/pom.xml test` |
| `ops-image-market` | `backend/ops-core-service` | 8133 | 49 | `docs/contracts-ops-image-market.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/ops-core-service/pom.xml test` |
| `plugin-integration` | `backend/ops-core-service` | 8133 | 38 | `docs/contracts-plugin-integration.md` | `.local-docs/tests-ops-core.md` | `mvn -q -f backend/ops-core-service/pom.xml test` |
| `portal-core` | `backend/portal-core-service` | 8134 | 5 | `docs/contracts-portal-core.md` | `.local-docs/tests-portal-core.md` | `mvn -q -f backend/portal-core-service/pom.xml test` |
| `profile` | `backend/business-core-service` | 8130 | 16 | `docs/contracts-profile.md` | `.local-docs/tests-profile.md` | `mvn -q -f backend/business-core-service/pom.xml test` |
| `resource` | `backend/business-core-service` | 8130 | 29 | `docs/contracts-resource.md` | `.local-docs/tests-resource.md` | `mvn -q -f backend/business-core-service/pom.xml test` |
| `server-status` | `backend/business-core-service` | 8130 | 25 | `docs/contracts-server-status.md` | `.local-docs/tests-server-status.md` | `mvn -q -f backend/business-core-service/pom.xml test` |
| `unified-backend` | `backend/unified-backend-service` | 8135 | 5 | `docs/contracts-unified-backend.md` | `.local-docs/tests-unified-backend.md` | `mvn -q -f backend/unified-backend-service/pom.xml test` |
| `whitelist` | `backend/admission-core-service` | 8131 | 20 | `docs/contracts-whitelist.md` | `.local-docs/tests-whitelist.md` | `mvn -q -f backend/admission-core-service/pom.xml test` |

## 合并后运行入口

当前已入仓的合并运行入口是 `business-core-service`、`admission-core-service`、`engagement-core-service`、`ops-core-service` 和 `portal-core-service`。第三批社区运营路径已经由网关统一切到 `8132`。第四批后台运维控制面路径已经由网关统一切到 `8133`。第五批玩家门户体验路径已经由网关统一切到 `8134`。旧 `backend/online-map-service` 已退役且不得恢复，不再作为当前网关上游。`engagement-core-service` 的后台自检摘要入口已经支持网关注入的可信认证上下文；149 个业务方法路由完整行为契约已经迁入当前入口，真实业务认证、真实持久化、真实跨服务 adapter、真实通知交付和真实 HTTP smoke 仍需后续独立闭环。

`api-gateway` 的 8 个自有接口新增只读运行拓扑，不新增业务语义。当前后端仍保持 7 个 Maven 运行入口，未来统一后端候选为 `api-gateway` 与五个 core 运行单元，`node-daemon` 继续作为外部节点执行边界。该拓扑只用于单服务合并准备和测试守卫，不能被描述为已经完成单服务合并、动态服务发现或 in-process 挂载。

`unified-backend` 是第九轮并行候选入口，端口固定为 `8135`。它在同一进程内挂载 `api-gateway` 自有 API、`portal-core` 自有 API、`guide`、`material` 和 `online-map`，只用于验证 `portal-core` 试点 in-process 装配。当前生产入口仍是 7 个，候选入口额外增加 1 个，不替代 `api-gateway-service:8125`、`portal-core-service:8134` 或 `node-daemon-service:8117`。

`engagement-core` 的 3 个自有接口只用于运行单元自检、后台装配摘要和生产就绪诊断。它不新增 community、activity、calendar 或 changelog 的业务语义。第三批 149 个业务路由签名和四个模块完整行为契约必须在 `engagement-core-service` 中自动化验证；生产就绪诊断仍必须公开真实持久化、审计持久化、真实跨服务 adapter、真实通知交付和真实 HTTP smoke 缺口。

`ops-core` 的 5 个自有接口只用于运行单元健康检查、运行摘要、模块装配摘要、生产就绪诊断和显式 HTTP smoke。它不新增 ops-control、cloudreve-sync、backup-recovery、alerting、plugin-integration、ops-image-market 或 cross-platform-notification 的业务语义。第四批和第六期合计 219 个业务路由签名和七个模块完整行为契约必须在 `ops-core-service` 中自动化验证；生产就绪诊断仍必须公开真实持久化、审计持久化、真实跨服务 HTTP、真实节点执行、真实 Cloudreve API、真实 registry、真实 scanner、真实插件事件入口、真实通知投递、真实外部发送、真实回调签名、生产凭据托管、异步队列、持久化事务、HTTP smoke 状态和可信网关内部签名状态。

`portal-core` 的 5 个自有接口只用于运行单元健康检查、运行摘要、模块装配摘要、生产就绪诊断和显式 HTTP smoke。它不新增 guide、material 或 online-map 的业务语义。第五批后三个玩家门户体验模块的 108 个业务路由签名和完整行为契约必须在 `portal-core-service` 中自动化验证；生产就绪诊断必须公开真实持久化、真实跨服务 HTTP、真实审计持久化、真实对象存储、真实文件安全扫描、真实全文搜索、真实地图 provider HTTP、真实 marker 同步、真实瓦片托管、真实外部通知投递、静态服务发现和 HTTP smoke 状态。

| 合并批次 | 运行入口 | 端口 | 承载模块 | 旧端口用途 |
| --- | --- | ---: | --- | --- |
| 第一批基础业务 | `backend/business-core-service` | 8130 | `auth`、`profile`、`notification`、`content`、`server-status`、`resource`、`admin` | `8101` 到 `8107` 为历史原端口，旧服务目录已清理且不得恢复 |
| 第二批入服准入 | `backend/admission-core-service` | 8131 | `onboarding`、`exam`、`whitelist`、`attendance` | `8108` 到 `8111` 为历史原端口，旧服务目录已清理且不得恢复 |
| 第三批社区运营 | `backend/engagement-core-service` | 8132 | `community`、`activity`、`calendar`、`changelog` | `8112` 到 `8115` 为历史原端口，旧服务目录已清理且不得恢复 |
| 第四批运维控制面 | `backend/ops-core-service` | 8133 | `ops-control`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration`、`ops-image-market` | `8116`、`8118`、`8119`、`8120`、`8122` 和 `8124` 为历史原端口，旧服务目录已退役且不得恢复 |
| 第五批玩家门户体验 | `backend/portal-core-service` | 8134 | `guide`、`material`、`online-map` | `8127`、`8126` 和 `8121` 为历史原端口；`guide`、`material` 和 `online-map` 旧服务目录已退役且不得恢复 |
| 第六期跨平台通知控制面 | `backend/ops-core-service` | 8133 | `cross-platform-notification` | `8123` 为历史原端口，旧服务目录退役后不得恢复 |
| 第九轮统一后端候选入口 | `backend/unified-backend-service` | 8135 | 候选挂载 `api-gateway` 自有 API、`portal-core` 自有 API、`guide`、`material`、`online-map` | 并行候选入口，不退役当前 7 个生产入口，`node-daemon` 保持外部 |

## 依赖顺序和边界

当前后端服务已经按依赖链路沉淀为独立契约和独立测试。前序服务的契约、测试、响应格式、错误码、认证方式和数据归属默认稳定。后序服务只能通过前序服务正式 API、后端入口认证上下文或受控 stub 适配，不能反向要求前序服务改结构，也不能直接读前序服务数据库。

核心身份链路为 `auth`、`profile`、`onboarding`、`exam`、`whitelist`、`attendance` 和 `notification`。官网内容链路为 `content`、`server-status`、`resource`、`portal-core`、`guide`、`material` 和 `online-map`。社区运营链路为 `community`、`activity`、`calendar`、`changelog`、`admin` 和 `cross-platform-notification`。运维平台链路为 `api-gateway`、`ops-core`、`ops-control`、`node-daemon`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration` 和 `ops-image-market`。

## 验收口径

任一模块变更时，必须优先更新该模块自己的 `docs/contracts-<module>.md`，再更新 `.local-docs/tests-<module>.md` 和对应自动化测试。只有模块测试和受影响的上游或下游回归测试全部通过，并留下测试过程记录后，才能认为该模块变更完成。

全量后端验收以合并后的 core 服务、未合并服务和 api-gateway 相关测试全部通过为准。本地全量测试过程记录见 `.local-docs/tests-overview.md`，该记录不提交到仓库。
