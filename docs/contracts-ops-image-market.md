# 北冥官网 ops-image-market API 契约

版本：0.1

## 文档定位

本文档是 `ops-image-market` 微服务的正式 API 契约。`ops-image-market` 负责运维镜像市场控制面，包括 registry provider 摘要、镜像目录、镜像版本、兼容性配置、部署模板摘要、风险扫描摘要、拉取计划、节点镜像缓存快照、依赖健康摘要、幂等记录、审计日志和自检摘要。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、请求编号、时间格式、基础角色、运维能力点、审计字段、风险等级和通用错误码均以公共契约为准。本文档只补充 `ops-image-market` 的职责边界、数据归属、前序服务兼容、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

本文档参考 Docker Hub、OCI Image Specification、Harbor、GitHub Container Registry、Portainer App Templates、Kubernetes ImagePolicyWebhook、Trivy 和 Renovate 的公开设计。Docker Hub 和 GitHub Container Registry 的 repository、tag、package 权限和访问控制适合本服务拆分 provider、repository 和可见范围。OCI Image Specification 的 manifest、image index、platform 和 digest 适合本服务保存脱敏镜像版本摘要。Harbor 的项目、机器人账号、扫描、复制策略和信任模型适合本服务建立 provider、凭据引用、扫描摘要和高风险启用规则。Portainer App Templates 的 image、env、ports、volumes 和 stack 模板思路适合本服务建立部署模板摘要。Kubernetes ImagePolicyWebhook 的准入判断适合本服务在拉取计划前做风险和兼容性阻断。Trivy 的漏洞严重级别、修复状态和扫描时间适合本服务定义风险扫描摘要。Renovate 的 Docker 版本更新和 digest pinning 思路适合本服务记录版本建议、digest 摘要和漂移风险。

参考资料：

| 来源 | 用到的判断 |
| --- | --- |
| [Docker Hub repositories](https://docs.docker.com/docker-hub/repos/) | repository、tag、namespace 和访问权限需要和镜像版本、provider 摘要分离。 |
| [OCI Image Specification](https://github.com/opencontainers/image-spec) | manifest、image index、platform、digest 和 layer 信息只能保存摘要，不能回显完整 payload。 |
| [Harbor documentation](https://goharbor.io/docs/) | 项目、机器人账号、漏洞扫描、复制和信任策略需要纳入 provider、凭据引用、风险摘要和审计。 |
| [GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry) | container package 权限和令牌边界说明 registry 凭据不能由前端或本服务明文托管。 |
| [Portainer app templates](https://docs.portainer.io/advanced/app-templates) | 镜像、环境变量、端口和卷可被模板化，但模板不能直接创建真实容器。 |
| [Kubernetes ImagePolicyWebhook](https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/#imagepolicywebhook) | 镜像准入应在执行前判断签名、来源、风险和策略，不依赖前端确认。 |
| [Trivy vulnerability scanning](https://trivy.dev/latest/docs/scanner/vulnerability/) | 扫描摘要需要记录 severity、fixed version、fix availability、扫描状态和过期时间。 |
| [Renovate Docker manager](https://docs.renovatebot.com/docker/) | tag 更新、digest pinning 和版本建议需要作为镜像版本摘要，不自动替换生产运行态。 |

本文档只吸收这些生态的设计思路，不接入它们的 SDK，不保存真实 registry token，不调用真实 Docker、containerd、registry、scanner、`ops-control` 任务或 `node-daemon`，不执行镜像拉取、镜像删除、容器创建、签名验签、镜像层扫描或节点缓存写入。

## 职责边界

`ops-image-market` 负责运维镜像市场的安全控制面。它保存可信镜像来源摘要、镜像仓库目录、版本和 digest 摘要、平台和架构摘要、兼容性配置、模板摘要、风险扫描摘要、拉取计划、节点缓存只读快照、版本建议、依赖健康摘要、幂等记录和审计日志。

`ops-image-market` 不负责注册、登录、角色能力点主数据、节点注册、节点心跳、容器生命周期、虚拟机生命周期、Minecraft 实例启停、文件上传下载、终端命令、备份恢复、真实镜像拉取、真实镜像删除、真实漏洞扫描、真实签名验签、真实 registry 凭据托管、玩家资源下载、Cloudreve 文件同步、插件安装卸载、外部通知发送或告警规则管理。

第一版固定为安全模拟和计划控制面。拉取计划只能进入 `DRAFT`、`RISK_REVIEW_REQUIRED`、`APPROVED`、`SIMULATED_READY`、`EXECUTION_BLOCKED`、`CANCELED`、`FAILED` 或 `SUCCEEDED_SIMULATED`。响应不得出现真实 `PULLED`、`PUSHED`、`RUNNING_ON_NODE` 或真实节点执行成功语义。后续接入真实 registry、scanner、signature verifier、`ops-control` 任务或 `node-daemon` 回写，必须重新补充正式契约、测试文档、红灯测试、实现和前序回归。

## 数据归属

`ops-image-market` 拥有以下主数据：ImageRegistryProvider、OpsImage、OpsImageVersion、ImageCompatibilityProfile、ImageTemplate、ImageRiskScanSummary、ImagePullPlan、NodeImageCacheSnapshot、ImageMarketAuditLog、OpsImageMarketSummary 和幂等记录。

`ops-image-market` 可以保存来自 `auth` 的操作者用户 ID、展示名、角色、能力点和状态快照；可以保存来自 `ops-control` 的节点、资产、容器运行时和 Minecraft 实例只读摘要；可以保存来自 `node-daemon` 经过 `ops-control` 或测试适配器回写的节点镜像缓存摘要；可以保存来自 `alerting` 可消费的风险事件摘要；可以保存来自 `cross-platform-notification` 的通知意图或投递结果摘要；可以保存来自 `plugin-integration` 的插件运行环境需求摘要。所有跨服务字段只能是安全快照，不得成为来源模块主数据，不得用于绕过来源模块权限，不得反向修改来源模块状态。

`ops-image-market` 不能直接读取其他服务数据库，不能导入前序服务 Java package，不能复用前序服务内存 store，不能修改 `ops-control` 节点、资产、任务或审批，不能直连 `node-daemon`，不能创建 `alerting` 告警实例，不能发送 `cross-platform-notification` 外部消息，不能安装插件或写入 Minecraft 配置。

## 基础路径、端口和认证

所有接口默认使用 `/api/v1/ops-image-market` 前缀。第四批合并后当前运行入口为 `ops-core-service:8133`。历史原服务端口 `8124` 只作为 `legacyPort` 返回，不再作为独立服务入口、网关上游或测试入口。

健康检查 `GET /api/v1/ops-image-market/health` 不要求认证，只能返回 `service`、`version`、`status` 和 `requestId`，不得返回 provider 数量、registry 地址、镜像 digest、扫描结果、节点摘要、依赖错误详情或任何敏感字段。

后台接口统一使用 `/api/v1/ops-image-market/admin` 前缀，全部要求 `Authorization: Bearer <token>`。后台读取接口要求基础角色为 `HELPER`、`ADMIN` 或 `OWNER`，并具备 `NODE_READ`。后台写接口要求基础角色为 `ADMIN` 或 `OWNER`，并具备 `NODE_WRITE`。涉及启用外部 provider、修改 endpoint 摘要、修改凭据引用摘要、允许未签名镜像、允许 `HIGH` 或 `CRITICAL` 风险镜像、批准高风险拉取计划、创建跨节点拉取计划、取消高风险计划或解除策略阻断时，必须具备 `HIGH_RISK_APPROVE` 或基础角色为 `OWNER`，并要求固定 `confirmText`。`CRITICAL` 风险只允许 `OWNER` 处理；`ADMIN` 即使具备 `HIGH_RISK_APPROVE`，创建或批准 `CRITICAL` 风险计划也必须返回 `42004`。

浏览器请求体不得传入并覆盖 `actorUserId`、`actorRole`、`actorPermissions`、`beforeState`、`afterState`、`auditResult`、`createdBy`、`updatedBy`、`enabledBy`、`disabledBy`、`archivedBy`、`approvedBy`、`planStatus`、`scanStatus`、`cacheStatus`、`opsControlTaskRef`、`nodeRequestId`、`registryToken`、`registryPassword`、`dockerPassword`、`imageSecret`、`pullSecret`、`rawToken`、`credential`、`secretKey`、`Authorization`、`requestHeaders`、`manifestPayload`、`layerUrl`、`internalUrl`、`internalPath`、`resolvedPath`、`fullException`、`databaseUrl` 等服务端可信字段。可信字段必须递归检查，嵌套在 `metadata`、`endpointSummary`、`credentialRefSummary`、`manifestSummary`、`templateSpec`、`envSchemaSummary`、`paramsSummary` 或任意数组对象中也必须拒绝。出现可信字段时返回 `40001`。

## 本地测试控制头

`ops-image-market` 允许在本地自动化测试中使用 `X-Test-Auth-Mode`、`X-Test-Ops-Control-Mode`、`X-Test-Node-Daemon-Mode`、`X-Test-Registry-Mode`、`X-Test-Scanner-Mode`、`X-Test-Alerting-Mode`、`X-Test-Notification-Mode`、`X-Test-Fail-Audit`、`X-Test-Fail-Store`、`X-Test-Fail-Plan` 和 `X-Test-Now` 模拟认证失败、前序依赖不可用、registry 降级、scanner 失败、审计失败、状态写入失败、计划写入失败和时间边界。

生产和默认运行环境必须关闭测试控制头。关闭后这些请求头必须被忽略，不能触发认证失败、依赖失败、registry 失败、scanner 失败、审计失败、存储失败、计划失败或时间模拟。自检摘要必须返回 `testControlsEnabled`，并在测试控制关闭时把 `TEST_CONTROLS_DISABLED_OUTSIDE_TEST` 纳入生产化硬化项。

## 前序服务兼容契约

`auth` 是后台接口强依赖。当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得访问后台接口。auth 不可用返回 `47200`，auth 超时返回 `47201`，字段或枚举不兼容返回 `47202`。

`ops-control` 是节点、资产、容器运行时和后续任务控制面的主数据来源。`ops-image-market` 可以读取节点架构、运行时、实例用途和已有缓存的只读摘要，不能直接修改节点、资产、任务、审批或审计。ops-control 不可用返回 `47210`，超时返回 `47211`，schema 不兼容返回 `47212`。读取类接口可以返回已有脱敏快照并标记 `degraded=true`；创建拉取计划时如果缺少必要节点或运行时摘要，不得伪造兼容成功。

`node-daemon` 是节点执行边界。第一版 `ops-image-market` 不得直接调用 `node-daemon`。节点缓存快照只能来自受控测试桩或后续经过正式接口的安全摘要。node-daemon 摘要不可用返回 `47220`，超时返回 `47221`，schema 不兼容返回 `47222`。任何需要实时节点执行的动作必须返回 `49717` 或进入 `EXECUTION_BLOCKED`，不能假装真实拉取成功。

`alerting` 可以后续消费 registry 失联、高危镜像、扫描过期、digest 漂移和拉取失败摘要。第一版只保存告警来源摘要，不直接创建告警规则、告警实例、静默或通知策略。alerting 不可用返回 `47230`，超时返回 `47231`，schema 不兼容返回 `47232`。

`cross-platform-notification` 可以后续承担高风险计划和镜像风险的外部通知投递。第一版只保存通知意图或结果摘要，不保存 webhook、短信、邮件、机器人或推送凭据。notification 适配不可用返回 `47240`，超时返回 `47241`，schema 不兼容返回 `47242`。

`plugin-integration` 可以提供插件运行环境需求或推荐镜像摘要。`ops-image-market` 不能安装插件、写插件配置、执行 Minecraft 命令或修改插件主数据。plugin-integration 不可用返回 `47250`，超时返回 `47251`，schema 不兼容返回 `47252`。

`resource` 与 `cloudreve-sync` 不参与运维镜像市场主流程。玩家资源下载、整合包、材质包、地图文件和 Cloudreve 分享链接仍归它们维护，不能把容器镜像伪装成玩家下载资源。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `RegistryType` | `DOCKER_HUB`、`OCI_REGISTRY`、`HARBOR`、`GHCR`、`PRIVATE_REGISTRY`、`MIRROR`、`SIMULATED` | 镜像来源类型。第一版只保存摘要和模拟健康。 |
| `ProviderStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`DEGRADED`、`ARCHIVED` | provider 控制面状态。 |
| `ProviderHealthStatus` | `HEALTHY`、`DEGRADED`、`UNAVAILABLE`、`UNKNOWN` | provider 健康摘要。 |
| `ImagePurpose` | `MINECRAFT_SERVER`、`PROXY`、`DATABASE`、`CACHE`、`UTILITY`、`PLUGIN_RUNTIME`、`OPS_TOOLING`、`CUSTOM` | 镜像用途。 |
| `ImageVisibility` | `OPS_ONLY`、`ADMIN_ONLY`、`TEMPLATE_ONLY` | 可见范围。第一版不面向普通玩家。 |
| `ImageStatus` | `DRAFT`、`PUBLISHED`、`DEPRECATED`、`BLOCKED`、`ARCHIVED` | 镜像目录状态。 |
| `ImageVersionStatus` | `DISCOVERED`、`APPROVED`、`DEPRECATED`、`BLOCKED`、`ARCHIVED` | 镜像版本状态。 |
| `RuntimeType` | `DOCKER`、`CONTAINERD`、`LXC`、`PODMAN`、`SIMULATED` | 运行时摘要。 |
| `Architecture` | `AMD64`、`ARM64`、`ARMV7`、`MULTI_ARCH`、`UNKNOWN` | 平台架构摘要。 |
| `TemplateStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`ARCHIVED` | 镜像模板状态。 |
| `ScanStatus` | `NOT_SCANNED`、`SCANNING`、`PASSED`、`WARNINGS`、`FAILED`、`EXPIRED`、`BLOCKED`、`UNAVAILABLE` | 风险扫描状态。 |
| `Severity` | `UNKNOWN`、`LOW`、`MEDIUM`、`HIGH`、`CRITICAL` | 漏洞和风险等级。 |
| `SignatureStatus` | `SIGNED`、`UNSIGNED`、`INVALID`、`UNKNOWN`、`NOT_REQUIRED` | 签名摘要。第一版不做真实验签。 |
| `PullPlanStatus` | `DRAFT`、`RISK_REVIEW_REQUIRED`、`APPROVED`、`SIMULATED_READY`、`EXECUTION_BLOCKED`、`CANCELED`、`FAILED`、`SUCCEEDED_SIMULATED` | 拉取计划状态。第一版不得出现真实拉取成功。 |
| `DependencyStatus` | `AVAILABLE`、`UNAVAILABLE`、`TIMEOUT`、`BAD_SCHEMA`、`STALE`、`SKIPPED` | 依赖摘要状态。 |
| `AuditResult` | `SUCCESS`、`FAILED` | 审计结果。 |

`sourceModule` 使用模块英文名，例如 `ops-control`、`node-daemon`、`alerting`、`cross-platform-notification`、`plugin-integration` 和 `custom`。浏览器传入 `auth`、`resource`、`cloudreve-sync`、`internal`、`system` 或未列入本契约的来源模块时必须返回 `40001`，不得创建 provider、镜像、模板、扫描、拉取计划或审计记录。

## 通用对象

### ImageRegistryProvider

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `providerId` | string | 是 | provider ID。 |
| `displayName` | string | 是 | 展示名称，2 到 80 位。 |
| `registryType` | string | 是 | `RegistryType`。 |
| `status` | string | 是 | `ProviderStatus`。 |
| `healthStatus` | string | 是 | `ProviderHealthStatus`。 |
| `endpointSummary` | object | 是 | endpoint 脱敏摘要，只保存协议、host 摘要和路径类型，不返回完整 URL。 |
| `credentialRefSummary` | object 或 null | 是 | 凭据引用摘要，只能保存别名、托管方式和轮换摘要，不返回真实凭据。 |
| `allowedNamespaces` | string[] | 是 | 允许仓库命名空间，最多 50 个。 |
| `allowedSourceModules` | string[] | 是 | 允许来源模块，最多 20 个。 |
| `allowedRiskLevels` | string[] | 是 | 允许风险等级，取公共风险等级。 |
| `syncPolicySummary` | object | 是 | 同步策略摘要，包括同步模式、窗口和最近同步时间。 |
| `rateLimitSummary` | object | 是 | 速率限制摘要，包括窗口、容量和降级原因。 |
| `lastHealthCheckedAt` | string 或 null | 是 | 最近健康刷新时间。 |
| `degraded` | boolean | 是 | 是否降级。 |
| `degradeReasons` | string[] | 是 | 降级原因，必须脱敏。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### OpsImage

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `imageId` | string | 是 | 镜像目录 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `repository` | string | 是 | 仓库名，例如 `library/redis`。不得包含 registry 凭据。 |
| `displayName` | string | 是 | 展示名称，2 到 100 位。 |
| `purpose` | string | 是 | `ImagePurpose`。 |
| `visibility` | string | 是 | `ImageVisibility`。 |
| `status` | string | 是 | `ImageStatus`。 |
| `maintainerSummary` | object | 是 | 维护者摘要，不返回邮箱、token 或内部账号。 |
| `sourceRef` | object 或 null | 是 | 来源引用摘要。 |
| `architectureSet` | string[] | 是 | 支持架构集合。 |
| `runtimeHints` | string[] | 是 | 运行时提示摘要。 |
| `latestVersionSummary` | object 或 null | 是 | 最新安全版本摘要。 |
| `riskSummary` | object | 是 | 风险摘要。 |
| `usageSummary` | object | 是 | 关联模板和计划数量摘要。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### OpsImageVersion

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `imageVersionId` | string | 是 | 镜像版本 ID。 |
| `imageId` | string | 是 | 镜像目录 ID。 |
| `tag` | string | 是 | tag 摘要。不得包含凭据或完整 registry URL。 |
| `digestSummary` | object | 是 | digest 摘要，只返回算法、短 hash 和 pinning 状态。 |
| `manifestSummary` | object | 是 | manifest 摘要，只返回 mediaType、platform 数量和 layer 数量，不返回完整 manifest。 |
| `status` | string | 是 | `ImageVersionStatus`。 |
| `os` | string | 是 | 目标 OS 摘要。 |
| `architecture` | string | 是 | `Architecture`。 |
| `sizeSummary` | object | 是 | 大小摘要。 |
| `publishedAt` | string 或 null | 是 | 上游发布时间。 |
| `deprecatedAt` | string 或 null | 是 | 废弃时间。 |
| `signed` | boolean | 是 | 是否有签名摘要。 |
| `signatureSummary` | object | 是 | 签名摘要，不返回证书私有内容。 |
| `scanSummary` | object | 是 | 最近扫描摘要。 |
| `compatibilitySummary` | object | 是 | 兼容性摘要。 |
| `changeSummary` | object | 是 | 版本变更摘要。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ImageCompatibilityProfile

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `profileId` | string | 是 | 兼容性配置 ID。 |
| `imageId` | string | 是 | 镜像目录 ID。 |
| `runtime` | string | 是 | `RuntimeType`。 |
| `architecture` | string | 是 | `Architecture`。 |
| `minecraftMode` | string 或 null | 是 | `VANILLA`、`FABRIC`、`FORGE`、`PAPER`、`PROXY`、`NONE` 或 `CUSTOM`。 |
| `minimumCpuCores` | number | 是 | 最小 CPU 核心数。 |
| `minimumMemoryMb` | integer | 是 | 最小内存 MB。 |
| `requiredPortsSummary` | object[] | 是 | 端口摘要，不绑定真实宿主端口。 |
| `requiredVolumesSummary` | object[] | 是 | 卷摘要，不返回宿主绝对路径。 |
| `envSchemaSummary` | object | 是 | 环境变量 schema 摘要，不返回 secret 值。 |
| `nodeSelectorSummary` | object | 是 | 节点选择摘要，只保存标签和架构要求。 |
| `status` | string | 是 | `DRAFT`、`ENABLED`、`DISABLED` 或 `ARCHIVED`。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ImageTemplate

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `templateId` | string | 是 | 模板 ID。 |
| `imageId` | string | 是 | 镜像目录 ID。 |
| `imageVersionId` | string 或 null | 是 | 固定版本 ID。为空时只能作为草案摘要。 |
| `displayName` | string | 是 | 模板名称。 |
| `status` | string | 是 | `TemplateStatus`。 |
| `templateKind` | string | 是 | `CONTAINER`、`MINECRAFT_INSTANCE`、`UTILITY_JOB` 或 `CUSTOM`。 |
| `runtime` | string | 是 | `RuntimeType`。 |
| `portMappingsSummary` | object[] | 是 | 端口映射摘要。 |
| `volumeMountsSummary` | object[] | 是 | 卷挂载摘要，不返回宿主绝对路径。 |
| `envSchemaSummary` | object | 是 | 环境变量摘要，secret 字段只返回键名和是否必填。 |
| `resourceLimitsSummary` | object | 是 | CPU、内存和磁盘摘要。 |
| `compatibilityProfileId` | string | 是 | 兼容性配置 ID。 |
| `riskSummary` | object | 是 | 模板风险摘要。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `updatedBy` | string | 是 | 最近修改者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### ImageRiskScanSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `scanId` | string | 是 | 扫描摘要 ID。 |
| `imageVersionId` | string | 是 | 镜像版本 ID。 |
| `scanner` | string | 是 | scanner 摘要，例如 `TRIVY_SIMULATED`。 |
| `status` | string | 是 | `ScanStatus`。 |
| `severityCounts` | object | 是 | `UNKNOWN`、`LOW`、`MEDIUM`、`HIGH`、`CRITICAL` 数量。 |
| `highestSeverity` | string | 是 | `Severity`。 |
| `fixAvailable` | boolean | 是 | 是否存在可修复版本摘要。 |
| `cveSummary` | object[] | 是 | CVE 摘要，最多返回 20 条，不返回完整扫描 payload。 |
| `licenseSummary` | object | 是 | 许可证摘要。 |
| `signatureStatus` | string | 是 | `SignatureStatus`。 |
| `startedAt` | string 或 null | 是 | 扫描开始时间。 |
| `finishedAt` | string 或 null | 是 | 扫描结束时间。 |
| `expiresAt` | string | 是 | 扫描过期时间。 |
| `degradedReasons` | string[] | 是 | 降级原因，必须脱敏。 |

### ImagePullPlan

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `planId` | string | 是 | 拉取计划 ID。 |
| `imageVersionId` | string | 是 | 镜像版本 ID。 |
| `providerId` | string | 是 | provider ID。 |
| `templateId` | string 或 null | 是 | 模板 ID。 |
| `targetNodeIds` | string[] | 是 | 目标节点 ID 摘要，最多 20 个。 |
| `runtime` | string | 是 | `RuntimeType`。 |
| `riskLevel` | string | 是 | 公共风险等级。 |
| `status` | string | 是 | `PullPlanStatus`。 |
| `approvalStatus` | string | 是 | `NOT_REQUIRED`、`REQUIRED`、`APPROVED`、`REJECTED` 或 `EXPIRED`。 |
| `compatibilityResult` | object | 是 | 兼容性结果摘要。 |
| `scanResultSummary` | object | 是 | 扫描结果摘要。 |
| `policyDecisionSummary` | object | 是 | 策略准入摘要。 |
| `opsControlTaskRef` | object 或 null | 是 | 后续 ops-control 任务引用摘要。第一版必须为 null。 |
| `simulated` | boolean | 是 | 第一版必须为 true。 |
| `createdBy` | string | 是 | 创建者用户 ID。 |
| `approvedBy` | string 或 null | 是 | 批准者用户 ID。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `finishedAt` | string 或 null | 是 | 终态时间。 |

### NodeImageCacheSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `snapshotId` | string | 是 | 快照 ID。 |
| `nodeId` | string | 是 | 节点 ID 摘要。 |
| `runtime` | string | 是 | `RuntimeType`。 |
| `imageVersionId` | string 或 null | 是 | 镜像版本 ID。 |
| `repositorySummary` | string | 是 | 仓库摘要，不返回完整内部地址。 |
| `tag` | string | 是 | tag 摘要。 |
| `digestSummary` | object | 是 | digest 摘要。 |
| `sizeSummary` | object | 是 | 大小摘要。 |
| `lastSeenAt` | string | 是 | 最近观察时间。 |
| `source` | string | 是 | `OPS_CONTROL_SNAPSHOT`、`NODE_DAEMON_SUMMARY`、`TEST_STUB` 或 `SIMULATED`。 |
| `stale` | boolean | 是 | 是否过期。 |
| `degradedReasons` | string[] | 是 | 降级原因。 |

### ImageMarketAuditLog

审计字段继承公共契约，允许补充 `providerId`、`imageId`、`imageVersionId`、`profileId`、`templateId`、`scanId`、`planId`、`snapshotId`、`stateFrom`、`stateTo`、`dependencyStatus` 和 `idempotencyKey`。审计列表不得提供删除、修改或恢复接口。审计响应不得返回 registry token、完整 endpoint、完整 digest 清单、完整 manifest、layer URL、宿主路径、节点凭据、请求头、异常栈或前序服务私有数据。

### OpsImageMarketSummary

自检摘要至少包含 `service`、`port`、`storageMode`、`authMode`、`opsControlAdapterMode`、`nodeDaemonAdapterMode`、`registryAdapterMode`、`scannerAdapterMode`、`alertingAdapterMode`、`notificationAdapterMode`、`testControlsEnabled`、`providersTotal`、`enabledProvidersTotal`、`imagesTotal`、`versionsTotal`、`templatesTotal`、`pullPlansTotal`、`simulatedReadyPlansTotal`、`blockedPlansTotal`、`cacheSnapshotsTotal`、`auditsTotal`、`idempotencyRecordsTotal`、`lastScanAt`、`lastPlanAt`、`degradedReasons` 和 `productionGaps`。

## 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `47200` | 502 | auth 认证上下文不可用。 |
| `47201` | 504 | auth 认证上下文调用超时。 |
| `47202` | 502 | auth 认证上下文不兼容。 |
| `47210` | 502 | ops-control 摘要不可用。 |
| `47211` | 504 | ops-control 摘要调用超时。 |
| `47212` | 502 | ops-control 摘要字段不兼容。 |
| `47220` | 502 | node-daemon 安全摘要不可用。 |
| `47221` | 504 | node-daemon 安全摘要调用超时。 |
| `47222` | 502 | node-daemon 安全摘要字段不兼容。 |
| `47230` | 502 | alerting 适配不可用。 |
| `47231` | 504 | alerting 适配调用超时。 |
| `47232` | 502 | alerting 适配字段不兼容。 |
| `47240` | 502 | cross-platform-notification 适配不可用。 |
| `47241` | 504 | cross-platform-notification 适配调用超时。 |
| `47242` | 502 | cross-platform-notification 适配字段不兼容。 |
| `47250` | 502 | plugin-integration 摘要不可用。 |
| `47251` | 504 | plugin-integration 摘要调用超时。 |
| `47252` | 502 | plugin-integration 摘要字段不兼容。 |
| `49700` | 404 | provider 不存在或当前用户不可见。 |
| `49701` | 404 | 镜像目录不存在或当前用户不可见。 |
| `49702` | 404 | 镜像版本不存在或当前用户不可见。 |
| `49703` | 404 | 兼容性配置不存在。 |
| `49704` | 404 | 镜像模板不存在。 |
| `49705` | 404 | 风险扫描摘要不存在。 |
| `49706` | 404 | 拉取计划不存在。 |
| `49707` | 404 | 节点缓存快照不存在。 |
| `49710` | 409 | 状态不允许当前操作。 |
| `49711` | 409 | provider、镜像、模板或计划业务冲突。 |
| `49712` | 409 | 幂等键请求指纹冲突。 |
| `49713` | 400 | endpoint、repository、tag、URL 或路径摘要不安全。 |
| `49714` | 409 | 风险策略阻断。 |
| `49715` | 409 | 扫描过期、失败或不可用。 |
| `49716` | 409 | 节点、运行时或模板兼容性失败。 |
| `49717` | 409 | 第一版真实执行被阻断。 |
| `49718` | 409 | 签名策略不满足。 |
| `49719` | 409 | registry 健康或速率限制阻断。 |
| `55900` | 500 | ops-image-market 内部错误。 |
| `55901` | 500 | ops-image-market 审计写入失败。 |
| `55902` | 500 | ops-image-market 状态写入失败。 |
| `55903` | 500 | ops-image-market 计划写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、能力点不足、高风险确认缺失、分页错误、排序错误、限流和通用服务端错误优先使用公共错误码。幂等键冲突使用 `49712`。状态冲突使用 `49710`。真实执行被阻断使用 `49717`。

## 接口总览

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

## provider 接口

`GET /api/v1/ops-image-market/admin/providers` 支持 `page`、`pageSize`、`keyword`、`registryType`、`status`、`healthStatus`、`namespace`、`riskLevel`、`sourceModule`、`degraded`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`、`lastHealthCheckedAt_desc`。成功响应分页 `items` 为 `ImageRegistryProvider[]`。

`GET /api/v1/ops-image-market/admin/providers/{providerId}` 返回 provider 详情、最近健康刷新摘要、镜像数量摘要、最近扫描摘要、依赖摘要和最近审计摘要。provider 不存在返回 `49700`。响应不得返回完整 endpoint、真实 registry 地址、凭据、token 或完整错误详情。

`POST /api/v1/ops-image-market/admin/providers` 请求字段包括 `displayName`、`registryType`、`endpointSummary`、`credentialRefSummary`、`allowedNamespaces`、`allowedSourceModules`、`allowedRiskLevels`、`syncPolicySummary`、`rateLimitSummary`、`reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `REGISTER_IMAGE_PROVIDER`。创建默认状态为 `DRAFT`，成功响应 HTTP `201`，`data` 为 `ImageRegistryProvider`。endpoint 或 namespace 不安全返回 `49713`。同一未归档 provider 下规范化 endpoint、registryType 和 displayName 冲突返回 `49711`。真实 token、secret、password 或完整 URL 出现在任意层级返回 `40001`。

`PATCH /api/v1/ops-image-market/admin/providers/{providerId}` 可修改创建接口中的业务字段，`reason` 必填。修改 `endpointSummary`、`credentialRefSummary`、`allowedNamespaces` 或 `allowedRiskLevels` 时必须携带 `confirmText=UPDATE_IMAGE_PROVIDER`，并校验高风险能力。`ARCHIVED` provider 不允许修改，返回 `49710`。审计失败返回 `55901` 且状态不变。

`PATCH /api/v1/ops-image-market/admin/providers/{providerId}/enable` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ENABLE_IMAGE_PROVIDER`。`DRAFT`、`DISABLED` 和 `DEGRADED` 可流转为 `ENABLED`。启用前必须校验 endpoint 安全、凭据引用只为摘要、allowed namespaces 非空、allowed source modules 非空、allowed risk levels 非空和健康摘要可用。重复启用保持幂等。

`PATCH /api/v1/ops-image-market/admin/providers/{providerId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 或 `DEGRADED` 可流转为 `DISABLED`。禁用后新镜像发现、新版本批准和新拉取计划不得使用该 provider。重复禁用保持幂等。

`PATCH /api/v1/ops-image-market/admin/providers/{providerId}/archive` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `ARCHIVE_IMAGE_PROVIDER`。只有 `DRAFT`、`DISABLED` 或 `DEGRADED` 可归档；`ENABLED` provider 必须先禁用后归档；存在 `PUBLISHED` 镜像、启用模板或非终态拉取计划时返回 `49710`。`ARCHIVED` 为终态。

`POST /api/v1/ops-image-market/admin/providers/{providerId}/health-refresh` 请求字段为 `reason` 和 `idempotencyKey`。第一版只刷新模拟健康摘要，不连真实 registry。测试配置下 `X-Test-Registry-Mode=unavailable` 返回 `47210` 或把 provider 标记为 `DEGRADED`，同一实现版本必须固定并写入测试。

## 镜像和版本接口

`GET /api/v1/ops-image-market/admin/images` 支持 `page`、`pageSize`、`keyword`、`providerId`、`repository`、`purpose`、`visibility`、`status`、`architecture`、`runtime`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`、`riskLevel_desc`。成功响应分页 `items` 为 `OpsImage[]`。

`GET /api/v1/ops-image-market/admin/images/{imageId}` 返回镜像详情、provider 摘要、最新版本摘要、兼容性摘要、模板摘要、拉取计划摘要、缓存摘要和最近审计摘要。镜像不存在返回 `49701`。响应不得返回完整 registry URL、完整 digest 清单、完整 manifest 或 layer URL。

`POST /api/v1/ops-image-market/admin/images` 请求字段包括 `providerId`、`repository`、`displayName`、`purpose`、`visibility`、`maintainerSummary`、`sourceRef`、`architectureSet`、`runtimeHints`、`reason` 和 `idempotencyKey`。provider 不存在返回 `49700`。provider 未启用时允许创建草稿镜像，但不得发布或创建拉取计划。repository 必须匹配 provider 的 allowed namespaces，且不得包含 registry 凭据、协议、用户密码、localhost、内网地址、控制字符、反斜杠或路径穿越。冲突返回 `49711`。

`PATCH /api/v1/ops-image-market/admin/images/{imageId}` 可修改展示名、用途、可见范围、维护者摘要、sourceRef、architectureSet 和 runtimeHints，`reason` 必填。`ARCHIVED` 镜像不允许修改。审计失败返回 `55901` 且状态不变。

`PATCH /api/v1/ops-image-market/admin/images/{imageId}/publish` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT` 或 `DEPRECATED` 可发布为 `PUBLISHED`。发布前必须存在至少一个 `APPROVED` 且扫描未过期的版本，provider 必须 `ENABLED`。不满足返回 `49710`、`49715` 或 `49719`。

`PATCH /api/v1/ops-image-market/admin/images/{imageId}/block` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `BLOCK_OPS_IMAGE`。`DRAFT`、`PUBLISHED` 或 `DEPRECATED` 可阻断为 `BLOCKED`。阻断后新拉取计划必须返回 `49714`。

`PATCH /api/v1/ops-image-market/admin/images/{imageId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。只有 `DRAFT`、`DEPRECATED` 或 `BLOCKED` 可归档；存在启用模板或非终态计划时返回 `49710`。

`GET /api/v1/ops-image-market/admin/images/{imageId}/versions` 支持 `page`、`pageSize`、`tag`、`status`、`architecture`、`signed`、`highestSeverity`、`scanStatus`、`from`、`to` 和 `sort`。`sort` 允许 `publishedAt_desc`、`createdAt_desc`、`tag_asc`、`highestSeverity_desc`。成功响应分页 `items` 为 `OpsImageVersion[]`。

`GET /api/v1/ops-image-market/admin/versions/{imageVersionId}` 返回版本详情、镜像摘要、provider 摘要、扫描摘要、兼容性摘要、模板引用摘要和最近审计摘要。版本不存在返回 `49702`。

`POST /api/v1/ops-image-market/admin/images/{imageId}/versions` 请求字段包括 `tag`、`digestSummary`、`manifestSummary`、`os`、`architecture`、`sizeSummary`、`publishedAt`、`signed`、`signatureSummary`、`changeSummary`、`reason` 和 `idempotencyKey`。创建默认状态为 `DISCOVERED`。tag、digest 和 manifest 摘要必须脱敏，不允许完整 manifest、layer URL、内部 registry 地址或凭据。相同 imageId 下 tag 或 digest 摘要冲突返回 `49711`。

`PATCH /api/v1/ops-image-market/admin/versions/{imageVersionId}/approve` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。只有 `DISCOVERED` 或 `DEPRECATED` 可批准为 `APPROVED`；`APPROVED`、`BLOCKED` 或 `ARCHIVED` 直接批准返回 `49710`，同一幂等键重放除外。普通版本可省略 confirmText；当扫描最高风险为 `HIGH`、签名为 `UNSIGNED` 或 provider 允许高风险时，`confirmText` 必须为 `APPROVE_IMAGE_VERSION_RISK`。当扫描最高风险为 `CRITICAL` 时，仅 `OWNER` 可带同一确认文本批准。批准前必须存在未过期扫描摘要和启用兼容配置；扫描过期返回 `49715`，兼容性失败返回 `49716`，签名策略失败返回 `49718`。

`PATCH /api/v1/ops-image-market/admin/versions/{imageVersionId}/deprecate` 请求字段为 `reason` 和 `idempotencyKey`。`DISCOVERED` 或 `APPROVED` 可变为 `DEPRECATED`。废弃后不得被新模板或新拉取计划引用。

`PATCH /api/v1/ops-image-market/admin/versions/{imageVersionId}/block` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `BLOCK_IMAGE_VERSION`。`DISCOVERED`、`APPROVED` 或 `DEPRECATED` 可变为 `BLOCKED`；`ARCHIVED` 为终态，返回 `49710`。阻断后新拉取计划必须返回 `49714`。

`PATCH /api/v1/ops-image-market/admin/versions/{imageVersionId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。只有 `DISCOVERED`、`DEPRECATED` 或 `BLOCKED` 可归档；`APPROVED` 版本必须先废弃或阻断；存在启用模板或非终态拉取计划引用时返回 `49710`。`ARCHIVED` 为终态，归档后不得批准、废弃、阻断、更新为模板固定版本或创建拉取计划。

## 兼容性和模板接口

`GET /api/v1/ops-image-market/admin/compatibility-profiles` 支持 `page`、`pageSize`、`imageId`、`runtime`、`architecture`、`minecraftMode`、`status`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`minimumMemoryMb_desc`。成功响应分页 `items` 为 `ImageCompatibilityProfile[]`。

`GET /api/v1/ops-image-market/admin/compatibility-profiles/{profileId}` 返回兼容配置详情、镜像摘要、最近计划使用摘要和最近审计摘要。配置不存在返回 `49703`。

`POST /api/v1/ops-image-market/admin/compatibility-profiles` 请求字段包括 `imageId`、`runtime`、`architecture`、`minecraftMode`、`minimumCpuCores`、`minimumMemoryMb`、`requiredPortsSummary`、`requiredVolumesSummary`、`envSchemaSummary`、`nodeSelectorSummary`、`reason` 和 `idempotencyKey`。路径、卷和环境变量摘要必须脱敏。环境变量 secret 字段只能保存键名、类型、是否必填和来源摘要，不能保存值。冲突返回 `49711`。

`PATCH /api/v1/ops-image-market/admin/compatibility-profiles/{profileId}` 可修改创建接口中的业务字段，`reason` 必填。`ARCHIVED` 配置不可修改。修改后引用该 profile 的启用模板必须标记 `degraded=true` 或要求重新启用。

`PATCH /api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/enable` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT` 或 `DISABLED` 可启用为 `ENABLED`；`ARCHIVED` 返回 `49710`。启用前必须校验镜像未归档、runtime 和 architecture 合法、端口和卷摘要安全、env schema 不含 secret 值。重复启用保持幂等。

`PATCH /api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。只有 `ENABLED` 可禁用为 `DISABLED`。禁用后新模板启用和新拉取计划不得使用该配置；重复禁用保持同一目标状态响应，不重复写审计。

`PATCH /api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。只有 `DRAFT` 或 `DISABLED` 可归档；`ENABLED` 必须先禁用；存在启用模板或非终态拉取计划间接引用时返回 `49710`。`ARCHIVED` 为终态，归档后不可修改、启用、禁用、被新模板引用或被新拉取计划使用。

`GET /api/v1/ops-image-market/admin/templates` 支持 `page`、`pageSize`、`keyword`、`imageId`、`imageVersionId`、`templateKind`、`runtime`、`status`、`from`、`to` 和 `sort`。`sort` 允许 `updatedAt_desc`、`createdAt_desc`、`displayName_asc`。成功响应分页 `items` 为 `ImageTemplate[]`。

`GET /api/v1/ops-image-market/admin/templates/{templateId}` 返回模板详情、镜像摘要、版本摘要、兼容配置摘要、最近计划摘要和最近审计摘要。模板不存在返回 `49704`。

`POST /api/v1/ops-image-market/admin/templates` 请求字段包括 `imageId`、`imageVersionId`、`displayName`、`templateKind`、`runtime`、`portMappingsSummary`、`volumeMountsSummary`、`envSchemaSummary`、`resourceLimitsSummary`、`compatibilityProfileId`、`reason` 和 `idempotencyKey`。创建默认状态为 `DRAFT`。模板不得创建 `ops-control` 任务，不得写节点，不得包含宿主绝对路径、完整命令、secret 值、registry 凭据或内部 URL。冲突返回 `49711`。

`PATCH /api/v1/ops-image-market/admin/templates/{templateId}` 可修改模板业务字段，`reason` 必填。`ARCHIVED` 模板不可修改。修改 imageVersionId 时必须校验版本 `APPROVED`、扫描未过期和兼容性通过。

`PATCH /api/v1/ops-image-market/admin/templates/{templateId}/enable` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT` 或 `DISABLED` 可启用为 `ENABLED`；`ARCHIVED` 返回 `49710`。启用前必须校验 provider `ENABLED`、镜像 `PUBLISHED`、版本 `APPROVED`、扫描未过期、兼容配置启用、模板摘要安全。重复启用保持幂等。

`PATCH /api/v1/ops-image-market/admin/templates/{templateId}/disable` 请求字段为 `reason` 和 `idempotencyKey`。`ENABLED` 可禁用为 `DISABLED`。禁用后新拉取计划不能使用该模板。

`PATCH /api/v1/ops-image-market/admin/templates/{templateId}/archive` 请求字段为 `reason` 和 `idempotencyKey`。只有 `DRAFT` 或 `DISABLED` 可归档；`ENABLED` 必须先禁用；存在非终态拉取计划引用时返回 `49710`。`ARCHIVED` 为终态，归档后不可修改、启用、禁用或创建新拉取计划。

## 风险扫描接口

`GET /api/v1/ops-image-market/admin/scans` 支持 `page`、`pageSize`、`imageVersionId`、`imageId`、`providerId`、`scanner`、`status`、`highestSeverity`、`fixAvailable`、`signatureStatus`、`from`、`to` 和 `sort`。`sort` 允许 `finishedAt_desc`、`startedAt_desc`、`highestSeverity_desc`、`expiresAt_asc`。成功响应分页 `items` 为 `ImageRiskScanSummary[]`。

`GET /api/v1/ops-image-market/admin/scans/{scanId}` 返回扫描详情、版本摘要、镜像摘要、provider 摘要和降级摘要。扫描不存在返回 `49705`。响应不得返回完整漏洞 payload、完整镜像 manifest、layer URL、registry 凭据、scanner 原始输出或内部路径。

`POST /api/v1/ops-image-market/admin/versions/{imageVersionId}/scans` 请求字段包括 `scanner`、`status`、`severityCounts`、`highestSeverity`、`fixAvailable`、`cveSummary`、`licenseSummary`、`signatureStatus`、`startedAt`、`finishedAt`、`expiresAt`、`degradedReasons`、`reason` 和 `idempotencyKey`。第一版只允许创建安全模拟或测试桩扫描摘要，不启动真实 scanner。`X-Test-Scanner-Mode=failed` 时返回 `47220` 或创建 `UNAVAILABLE` 摘要，同一实现版本必须固定并写入测试。`expiresAt` 早于当前时间时扫描为 `EXPIRED`，不得批准版本或创建拉取计划。

## 拉取计划接口

`GET /api/v1/ops-image-market/admin/pull-plans` 支持 `page`、`pageSize`、`imageVersionId`、`imageId`、`providerId`、`templateId`、`nodeId`、`runtime`、`riskLevel`、`status`、`approvalStatus`、`createdBy`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`updatedAt_desc`、`riskLevel_desc`、`status_asc`。成功响应分页 `items` 为 `ImagePullPlan[]`。

`GET /api/v1/ops-image-market/admin/pull-plans/{planId}` 返回计划详情、镜像版本摘要、provider 摘要、模板摘要、目标节点摘要、兼容性结果、扫描结果、策略决策、依赖摘要和最近审计摘要。计划不存在返回 `49706`。

`POST /api/v1/ops-image-market/admin/pull-plans` 请求字段包括 `imageVersionId`、`templateId`、`targetNodeIds`、`runtime`、`riskLevel`、`allowUnsigned`、`allowHighSeverity`、`reason`、`confirmText` 和 `idempotencyKey`。成功响应 HTTP `201`。第一版只创建模拟计划，不执行拉取，不创建 `ops-control` 任务。跨节点计划、`allowUnsigned=true`、`allowHighSeverity=true`、`riskLevel=HIGH` 或扫描最高风险为 `HIGH` 时，`confirmText` 必须为 `CREATE_IMAGE_PULL_PLAN_RISK` 并要求高风险权限。`riskLevel=CRITICAL` 或扫描最高风险为 `CRITICAL` 时只有 `OWNER` 可创建。provider 未启用返回 `49719`，镜像未发布或版本未批准返回 `49710`，扫描过期返回 `49715`，兼容性失败返回 `49716`，策略阻断返回 `49714`，真实执行请求返回 `49717`。

创建计划时必须按目标节点摘要校验架构、runtime、最小内存、端口需求、卷需求、模板状态和 provider 风险策略。无法从 `ops-control` 获取目标节点摘要时，不得伪造兼容成功。`targetNodeIds` 不允许为空，最多 20 个。重复幂等键同体返回同一计划，不同体返回 `49712`。

`PATCH /api/v1/ops-image-market/admin/pull-plans/{planId}/approve` 请求字段为 `reason`、`confirmText` 和 `idempotencyKey`。`confirmText` 必须为 `APPROVE_IMAGE_PULL_PLAN`。只有 `DRAFT` 和 `RISK_REVIEW_REQUIRED` 可批准。批准时必须重新校验 provider 仍为 `ENABLED`、镜像仍为 `PUBLISHED`、版本仍为 `APPROVED` 且未废弃/阻断/归档、模板仍为 `ENABLED`、目标节点仍兼容、扫描仍未过期。批准后低中风险计划进入 `SIMULATED_READY`，高风险计划进入 `APPROVED` 或 `SIMULATED_READY`，但不得进入真实执行状态。扫描过期返回 `49715`，provider 禁用返回 `49719`，镜像、版本或模板状态失效返回 `49710`，节点兼容性变化返回 `49716`。

`PATCH /api/v1/ops-image-market/admin/pull-plans/{planId}/cancel` 请求字段为 `reason` 和 `idempotencyKey`。`DRAFT`、`RISK_REVIEW_REQUIRED`、`APPROVED`、`SIMULATED_READY` 和 `EXECUTION_BLOCKED` 可取消为 `CANCELED`。取消 `HIGH` 或 `CRITICAL` 计划必须校验高风险权限；`CRITICAL` 仍只允许 `OWNER`。终态计划重复取消按固定幂等语义返回状态冲突或相同结果，同一实现版本必须写入测试。

## 节点缓存和审计接口

`GET /api/v1/ops-image-market/admin/cache-snapshots` 支持 `page`、`pageSize`、`nodeId`、`runtime`、`imageVersionId`、`repository`、`tag`、`stale`、`source`、`from`、`to` 和 `sort`。`sort` 允许 `lastSeenAt_desc`、`lastSeenAt_asc`、`repository_asc`。成功响应分页 `items` 为 `NodeImageCacheSnapshot[]`。节点缓存快照只读，不提供浏览器写接口。

`GET /api/v1/ops-image-market/admin/cache-snapshots/{snapshotId}` 返回快照详情、节点摘要、版本摘要和降级原因。快照不存在返回 `49707`。响应不得返回节点本地镜像层路径、宿主绝对路径、完整 digest 清单、registry 凭据或内部节点地址。

`GET /api/v1/ops-image-market/admin/audit-logs` 支持 `page`、`pageSize`、`actorUserId`、`action`、`targetType`、`targetId`、`providerId`、`imageId`、`imageVersionId`、`templateId`、`planId`、`result`、`riskLevel`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc`、`riskLevel_desc`。只有 `ADMIN` 和 `OWNER` 可访问。审计列表是只读接口，不提供删除、修改或恢复。审计中的 `requestId` 必须来自当前 HTTP 请求，不能使用固定占位值。

后台写操作必须记录调用者、调用者角色、调用者能力点摘要、来源 IP、`reason`、操作前状态、操作后状态、请求编号、结果和失败原因。审计写入失败时，provider、镜像、版本、兼容配置、模板、扫描摘要和拉取计划不得假装成功，必须返回 `55901` 并保持业务状态不变。

## 状态、幂等和并发

provider 状态流转为 `DRAFT` 可到 `ENABLED`、`DISABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DEGRADED` 或 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`DEGRADED` 可恢复为 `ENABLED`、禁用或归档；`ARCHIVED` 为终态。

所有状态写接口必须先按当前状态和目标状态执行统一流转校验，再做依赖校验、审计写入和状态更新。未列入本节的源状态到目标状态转换必须返回 `49710`，不能靠接口实现自行放宽。

镜像目录状态流转为 `DRAFT` 可到 `PUBLISHED`、`BLOCKED` 或 `ARCHIVED`；`PUBLISHED` 可到 `DEPRECATED` 或 `BLOCKED`；`DEPRECATED` 可到 `PUBLISHED`、`BLOCKED` 或 `ARCHIVED`；`BLOCKED` 可到 `DRAFT` 或 `ARCHIVED`；`ARCHIVED` 为终态。

镜像版本状态流转为 `DISCOVERED` 可到 `APPROVED`、`DEPRECATED`、`BLOCKED` 或 `ARCHIVED`；`APPROVED` 可到 `DEPRECATED` 或 `BLOCKED`；`DEPRECATED` 可到 `APPROVED`、`BLOCKED` 或 `ARCHIVED`；`BLOCKED` 可到 `DISCOVERED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

兼容配置状态流转为 `DRAFT` 可到 `ENABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

模板状态流转为 `DRAFT` 可到 `ENABLED` 或 `ARCHIVED`；`ENABLED` 可到 `DISABLED`；`DISABLED` 可到 `ENABLED` 或 `ARCHIVED`；`ARCHIVED` 为终态。

拉取计划状态流转为 `DRAFT` 到 `RISK_REVIEW_REQUIRED`、`SIMULATED_READY`、`EXECUTION_BLOCKED`、`FAILED` 或 `CANCELED`；`RISK_REVIEW_REQUIRED` 到 `APPROVED`、`SIMULATED_READY`、`EXECUTION_BLOCKED`、`FAILED` 或 `CANCELED`；`APPROVED` 到 `SIMULATED_READY`、`EXECUTION_BLOCKED`、`FAILED` 或 `CANCELED`；`SIMULATED_READY` 到 `SUCCEEDED_SIMULATED`、`EXECUTION_BLOCKED`、`FAILED` 或 `CANCELED`。`CANCELED`、`FAILED` 和 `SUCCEEDED_SIMULATED` 为终态。第一版不得出现真实拉取成功状态。

所有写接口使用幂等键时，必须使用字段名排序后的稳定 JSON 语义计算请求体指纹，嵌套对象按字段名递归排序，数组保留顺序，不能依赖 Java `Map.toString()` 或浏览器字段顺序。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一响应快照；相同幂等键搭配不同请求体返回 `49712`。幂等键查找、状态校验、业务写入、审计写入、响应快照保存和幂等记录写入必须处于同一临界区内。

并发创建相同 provider、镜像目录、镜像版本、兼容配置、模板、扫描摘要或拉取计划时只能一个成功，其余返回冲突或相同幂等结果。后续数据库实现必须迁移为事务、唯一约束、条件更新或等效机制，不能降低并发口径。

所有支持 `from` 和 `to` 的列表接口必须按该资源主时间字段过滤时间范围。provider、镜像、版本、兼容配置、模板和计划使用 `updatedAt` 或契约指定主时间；扫描使用 `finishedAt`；缓存快照使用 `lastSeenAt`；审计使用 `createdAt`。只传 `from` 时返回主时间大于等于 `from` 的记录，只传 `to` 时返回主时间小于等于 `to` 的记录，同时传入且 `from` 晚于 `to` 时返回 `40001`。`from` 或 `to` 不是 ISO 8601 时间字符串时返回 `40001`。

## 安全、降级和脱敏

endpoint、repository、tag、namespace 和 URL 摘要必须拒绝 `file:`、`data:`、`javascript:`、带用户名密码 URL、localhost、回环 IP、内网 IP、链路本地地址、未解析 host、通配符 `*`、空 host、控制字符、反斜杠、路径穿越和非法 URI。repository 必须是 registry 内 namespace/repository 摘要，不允许浏览器提交完整 registry 登录串。

任何请求体和响应都不得包含 registry token、registry password、Docker password、image secret、pull secret、完整 Authorization 请求头、完整请求 headers、完整 manifest、完整 layer URL、内部 registry 地址、内部 URL、内部路径、节点本地镜像层路径、宿主绝对路径、真实 shell 命令、异常栈、数据库连接串、`.env`、`authorized_keys`、`id_rsa` 或前序服务私有数据。检查必须递归覆盖嵌套对象和数组。

扫描失败、扫描过期、签名不满足、provider 降级、registry 限流、节点摘要不可用或兼容性失败时，读取类接口可以返回已有快照并标记 `degraded=true`、`stale=true` 和 `degradeReasons`。写入类接口不得假装成功。高风险或严重风险计划必须按权限、确认文本、状态和审计规则阻断或进入 `RISK_REVIEW_REQUIRED`，不能靠前端展示来兜底。

第一版不得提供真实删除 provider、镜像、版本、兼容配置、模板、扫描摘要、拉取计划、缓存快照或审计的接口。确需清理历史记录时，必须在后续独立契约中增加归档或保留策略接口，并重新完成文档、测试红灯、实现和回归闭环。

## 验收口径

`ops-image-market` API 文档必须按 `docs/contracts-ops-image-market.md` 独立存在，并由 `.local-docs/tests-ops-core.md` 记录合并后的本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、能力点不足、高风险确认缺失、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求、敏感字段脱敏、测试控制头默认关闭和模块验收口径。

`ops-image-market` 完成时必须满足以下条件：当前运行入口为 `ops-core-service:8133`，历史端口 `8124` 只作为 `legacyPort` 返回；健康检查公开且不泄露敏感信息；后台接口按角色、能力点、风险等级和确认文本限制；provider、镜像目录、镜像版本、兼容配置、模板、风险扫描、拉取计划、节点缓存快照、审计、幂等、状态流转、依赖降级、审计失败回滚、敏感字段脱敏、测试控制头默认关闭和自检摘要都有自动化验证；自动化测试必须先红灯；实现后本模块在 `ops-core-service` 中全量测试通过；当前后端运行入口回归测试通过；边界扫描无违规命中；不修改前序服务稳定接口；不直接读取前序服务数据库；不导入前序服务 Java package；不调用真实 `node-daemon`；不执行真实 Docker、containerd、registry、scanner、镜像拉取、镜像删除或容器创建；不保存真实 registry token、完整 manifest、layer URL、内部地址、宿主路径、节点凭据、完整请求头或前序服务私有数据；不把玩家资源下载、Cloudreve 文件同步、运维任务执行、节点文件管理、终端能力、告警规则、外部通知发送或插件安装塞进本服务。
