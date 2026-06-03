# 北冥官网后端 API 总览

版本：0.1

## 文档定位

本文档是北冥官网后端 API 的整体索引和汇总说明，用于让维护者快速查看服务边界、端口、契约文件、测试入口和 API 覆盖规模。它不替代任何模块独立契约。正式开发、验收和变更仍以 `docs/contracts-<module>.md` 为准，公共响应、错误码、分页、认证和审计规则仍以 `docs/contracts-common.md` 为准。

面向前端开发的一体化 API 全文查阅文档见 `docs/api-reference.md`。该文档由公共契约和全部模块契约合并生成，方便前端统一检索路径、字段和响应格式。

本汇总覆盖当前仓库 `docs/contracts-*.md` 中除 `contracts-common.md` 之外的 27 个后端或平台模块。第一批、第二批和第三批已完成运行合并并入仓。当前唯一 `METHOD path` 总数为 746。

## 全局接口规则

所有业务成功响应使用统一结构，`code=0` 表示成功，`message` 为成功或错误摘要，业务数据放在 `data` 中。分页响应统一在 `data` 内返回 `items`、`page`、`pageSize` 和 `total`。

受保护接口统一使用 `Authorization: Bearer <token>`。后台、运维和节点相关接口必须继续校验角色、能力点、风险等级、二次确认、幂等键和审计字段。业务服务不能自行实现登录逻辑，不能直接读取其他服务数据表，跨模块读取必须通过正式接口、网关认证上下文或受控适配层。

玩家可见资源、服务器状态展示、后台运维控制和节点真实执行必须保持边界隔离。`resource` 只负责玩家资源分发，`server-status` 只负责玩家可见状态，`ops-control` 负责运维控制面，`node-daemon` 负责节点侧受控执行。

## 服务与契约总表

| 模块 | 服务目录 | 端口 | API 数 | 正式契约 | 本地测试文档 | 自动化测试入口 |
| --- | --- | ---: | ---: | --- | --- | --- |
| `activity` | `backend/engagement-core-service` | 8132 | 41 | `docs/contracts-activity.md` | `.local-docs/tests-activity.md` | `mvn -q -f backend/engagement-core-service/pom.xml test` |
| `admin` | `backend/business-core-service` | 8130 | 10 | `docs/contracts-admin.md` | `.local-docs/tests-admin.md` | `mvn -q -f backend/business-core-service/pom.xml test` |
| `alerting` | `backend/alerting-service` | 8120 | 24 | `docs/contracts-alerting.md` | `.local-docs/tests-alerting.md` | `mvn -q -f backend/alerting-service/pom.xml test` |
| `api-gateway` | `backend/api-gateway-service` | 8125 | 8 | `docs/contracts-api-gateway.md` | `.local-docs/tests-api-gateway.md` | `mvn -q -f backend/api-gateway-service/pom.xml test` |
| `attendance` | `backend/admission-core-service` | 8131 | 22 | `docs/contracts-attendance.md` | `.local-docs/tests-attendance.md` | `mvn -q -f backend/admission-core-service/pom.xml test` |
| `auth` | `backend/business-core-service` | 8130 | 20 | `docs/contracts-auth.md` | `.local-docs/tests-auth.md` | `mvn -q -f backend/business-core-service/pom.xml test` |
| `backup-recovery` | `backend/backup-recovery-service` | 8119 | 25 | `docs/contracts-backup-recovery.md` | `.local-docs/tests-backup-recovery.md` | `mvn -q -f backend/backup-recovery-service/pom.xml test` |
| `calendar` | `backend/engagement-core-service` | 8132 | 22 | `docs/contracts-calendar.md` | `.local-docs/tests-calendar.md` | `mvn -q -f backend/engagement-core-service/pom.xml test` |
| `changelog` | `backend/engagement-core-service` | 8132 | 23 | `docs/contracts-changelog.md` | `.local-docs/tests-changelog.md` | `mvn -q -f backend/engagement-core-service/pom.xml test` |
| `cloudreve-sync` | `backend/cloudreve-sync-service` | 8118 | 16 | `docs/contracts-cloudreve-sync.md` | `.local-docs/tests-cloudreve-sync.md` | `mvn -q -f backend/cloudreve-sync-service/pom.xml test` |
| `community` | `backend/engagement-core-service` | 8132 | 62 | `docs/contracts-community.md` | `.local-docs/tests-community.md` | `mvn -q -f backend/engagement-core-service/pom.xml test` |
| `content` | `backend/business-core-service` | 8130 | 55 | `docs/contracts-content.md` | `.local-docs/tests-content.md` | `mvn -q -f backend/business-core-service/pom.xml test` |
| `cross-platform-notification` | `backend/cross-platform-notification-service` | 8123 | 36 | `docs/contracts-cross-platform-notification.md` | `.local-docs/tests-cross-platform-notification.md` | `mvn -q -f backend/cross-platform-notification-service/pom.xml test` |
| `exam` | `backend/admission-core-service` | 8131 | 29 | `docs/contracts-exam.md` | `.local-docs/tests-exam.md` | `mvn -q -f backend/admission-core-service/pom.xml test` |
| `guide` | `backend/guide-service` | 8127 | 29 | `docs/contracts-guide.md` | `.local-docs/tests-guide.md` | `mvn -q -f backend/guide-service/pom.xml test` |
| `material` | `backend/material-service` | 8126 | 33 | `docs/contracts-material.md` | `.local-docs/tests-material.md` | `mvn -q -f backend/material-service/pom.xml test` |
| `node-daemon` | `backend/node-daemon-service` | 8117 | 17 | `docs/contracts-node-daemon.md` | `.local-docs/tests-node-daemon.md` | `mvn -q -f backend/node-daemon-service/pom.xml test` |
| `notification` | `backend/business-core-service` | 8130 | 19 | `docs/contracts-notification.md` | `.local-docs/tests-notification.md` | `mvn -q -f backend/business-core-service/pom.xml test` |
| `onboarding` | `backend/admission-core-service` | 8131 | 15 | `docs/contracts-onboarding.md` | `.local-docs/tests-onboarding.md` | `mvn -q -f backend/admission-core-service/pom.xml test` |
| `online-map` | `backend/online-map-service` | 8121 | 34 | `docs/contracts-online-map.md` | `.local-docs/tests-online-map.md` | `mvn -q -f backend/online-map-service/pom.xml test` |
| `ops-control` | `backend/ops-control-service` | 8116 | 27 | `docs/contracts-ops-control.md` | `.local-docs/tests-ops-control.md` | `mvn -q -f backend/ops-control-service/pom.xml test` |
| `ops-image-market` | `backend/ops-image-market-service` | 8124 | 48 | `docs/contracts-ops-image-market.md` | `.local-docs/tests-ops-image-market.md` | `mvn -q -f backend/ops-image-market-service/pom.xml test` |
| `plugin-integration` | `backend/plugin-integration-service` | 8122 | 38 | `docs/contracts-plugin-integration.md` | `.local-docs/tests-plugin-integration.md` | `mvn -q -f backend/plugin-integration-service/pom.xml test` |
| `profile` | `backend/business-core-service` | 8130 | 16 | `docs/contracts-profile.md` | `.local-docs/tests-profile.md` | `mvn -q -f backend/business-core-service/pom.xml test` |
| `resource` | `backend/business-core-service` | 8130 | 29 | `docs/contracts-resource.md` | `.local-docs/tests-resource.md` | `mvn -q -f backend/business-core-service/pom.xml test` |
| `server-status` | `backend/business-core-service` | 8130 | 25 | `docs/contracts-server-status.md` | `.local-docs/tests-server-status.md` | `mvn -q -f backend/business-core-service/pom.xml test` |
| `whitelist` | `backend/admission-core-service` | 8131 | 23 | `docs/contracts-whitelist.md` | `.local-docs/tests-whitelist.md` | `mvn -q -f backend/admission-core-service/pom.xml test` |

## 合并后运行入口

当前已入仓的合并运行入口是 `business-core-service`、`admission-core-service` 和 `engagement-core-service`。第三批社区运营路径已经由网关统一切到 `8132`。

| 合并批次 | 运行入口 | 端口 | 承载模块 | 旧端口用途 |
| --- | --- | ---: | --- | --- |
| 第一批基础业务 | `backend/business-core-service` | 8130 | `auth`、`profile`、`notification`、`content`、`server-status`、`resource`、`admin` | `8101` 到 `8107` 为历史原端口，旧服务目录已清理且不得恢复 |
| 第二批入服准入 | `backend/admission-core-service` | 8131 | `onboarding`、`exam`、`whitelist`、`attendance` | `8108` 到 `8111` 为历史原端口，旧服务目录已清理且不得恢复 |
| 第三批社区运营 | `backend/engagement-core-service` | 8132 | `community`、`activity`、`calendar`、`changelog` | `8112` 到 `8115` 为历史原端口，旧服务目录已清理且不得恢复 |

## 依赖顺序和边界

当前后端服务已经按依赖链路沉淀为独立契约和独立测试。前序服务的契约、测试、响应格式、错误码、认证方式和数据归属默认稳定。后序服务只能通过前序服务正式 API、后端入口认证上下文或受控 stub 适配，不能反向要求前序服务改结构，也不能直接读前序服务数据库。

核心身份链路为 `auth`、`profile`、`onboarding`、`exam`、`whitelist`、`attendance` 和 `notification`。官网内容链路为 `content`、`server-status`、`resource`、`guide`、`material` 和 `online-map`。社区运营链路为 `community`、`activity`、`calendar`、`changelog`、`admin` 和 `cross-platform-notification`。运维平台链路为 `api-gateway`、`ops-control`、`node-daemon`、`cloudreve-sync`、`backup-recovery`、`alerting`、`plugin-integration` 和 `ops-image-market`。

## 验收口径

任一模块变更时，必须优先更新该模块自己的 `docs/contracts-<module>.md`，再更新 `.local-docs/tests-<module>.md` 和对应自动化测试。只有模块测试和受影响的上游或下游回归测试全部通过，并留下测试过程记录后，才能认为该模块变更完成。

全量后端验收以合并后的 core 服务、未合并服务和 api-gateway 相关测试全部通过为准。最近一次 `main` 分支全量测试记录见 `.local-docs/backend-main-merge-test-record.md`，该记录不提交到仓库。
