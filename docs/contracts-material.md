# 北冥官网 material API 契约

版本：0.1

## 文档定位

本文档是 `material` 微服务的正式 API 契约。后续 `content`、`community`、`resource`、`admin`、前端适配和精彩瞬间展示只能通过本文档定义的接口读取或管理玩家投稿素材，不能直接读取或修改 `material` 数据库。

本文档继承 `docs/contracts-common.md`。统一响应格式、统一错误响应、分页格式、认证头、时间格式、基础角色、能力点、审计字段、风险等级、通用状态模型和通用错误码均以公共契约为准。本文档只补充 `material` 的职责边界、上传模型、授权声明、文件安全状态、路径、字段、状态、权限、错误码、幂等、状态流转、失败降级、审计和验收口径。

`material` 适配 `auth`、`profile` 和 `notification`，不要求前序服务反向适配 `material`。它通过后端入口传入的认证上下文、`/api/v1/auth/me`、`/api/v1/auth/session/verify` 或测试环境 auth stub 读取当前用户、角色和能力点；通过 profile 正式接口或受控 stub 读取投稿作者公开快照和成员状态；通过 notification 正式接口或受控适配层投递审核结果通知。`material` 不能导入前序服务的实体、Repository、内存存储、测试种子或内部实现。

## 参考生态

本契约参考成熟上传和素材分发生态，但只吸收适合北冥官网当前阶段的边界。Cloudinary Upload API 把服务端签名、unsigned upload preset、资源类型、moderation 和大文件上传拆开，适合借鉴为短期上传票据、上传预设和审核状态。Amazon S3 presigned URL 把上传权限压缩到指定对象、方法、过期时间和校验约束中，适合借鉴为持有即授权的短期上传会话。OWASP File Upload Cheat Sheet 是上传安全底线，要求扩展名白名单、不能信任 `Content-Type`、服务端生成文件名、大小限制、授权上传、隔离存储和必要的安全扫描。Modrinth 和 CurseForge 这类内容平台强调项目、版本、文件哈希、依赖、许可证、审核和可见范围分离，适合借鉴为素材主数据、文件安全摘要和公开展示快照分离。GitHub Releases 的 release asset 设计说明上传资产和发布实体应有独立生命周期，适合确认素材附件不能直接等同素材审核通过。

参考来源包括 [Cloudinary Upload API](https://cloudinary.com/documentation/image_upload_api_reference)、[Cloudinary Upload Presets](https://cloudinary.com/documentation/upload_presets)、[Amazon S3 presigned URL upload](https://docs.aws.amazon.com/AmazonS3/latest/userguide/PresignedUrlUploadObject.html)、[OWASP File Upload Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)、[Modrinth API](https://docs.modrinth.com/api/)、[CurseForge for Studios API](https://docs.curseforge.com/) 和 [GitHub REST API release assets](https://docs.github.com/en/rest/releases/assets)。

## 职责边界

`material` 负责玩家素材投稿、上传会话、上传文件安全摘要、素材授权声明、投稿记录、审核状态、精选状态、公开素材列表、公开素材详情、公开安全文件摘要、当前用户投稿管理、后台审核、素材分类、素材审计、依赖摘要和自检摘要。

`material` 不负责官网首页配置、文章正文、资源下载、Cloudreve 分享链接、资源版本、社区帖子正文、工单附件主数据、活动报名、日历、更新日志、通知主数据、后台聚合入口、服务器文件管理、容器、终端、节点执行、真实文件删除、真实对象存储密钥托管或运维审计。

`content` 后续可以引用精选素材公开快照做精彩瞬间展示，但不能读取 `material` 数据库，也不能修改素材审核状态。`community` 可以引用公开素材快照作为帖子或工单证据，但不能接管素材主数据。`resource` 只管理玩家可见下载资源和 Cloudreve 分享，不接管投稿原始素材。`ops-control` 和 `external-node-executor` 不能复用 `material` 上传权限做服务器文件管理。

## 数据归属

`material` 拥有以下主数据：素材分类、素材投稿、上传会话、素材文件安全摘要、素材授权声明、公开素材快照、素材状态记录、幂等记录、素材审计日志和运行自检摘要。

作者快照字段来自 profile 或可信认证上下文，至少包括 `userId`、`memberId`、`displayName`、`avatarUrl`、`minecraftId`、`memberStatus` 和 `profileSnapshotAt`。快照只用于展示和审计，不是 auth 或 profile 主数据，不能用于账号权限或成员资格最终判断。

文件安全摘要只保存服务端生成文件名、脱敏原始文件名、MIME 摘要、扩展名、大小、校验值、安全状态、公开访问摘要和上传会话关联。不得保存对象存储管理密钥、Cloudreve 管理 token、内部绝对路径、上传票据明文长期副本、真实防病毒扫描原始报告或可直接执行的服务器路径。

## 基础路径与认证

公开接口使用 `/api/v1/materials` 前缀，不要求登录。公开接口只能返回公开可见、已通过或已精选、未下架、未归档、未软删除、处于可见时间范围内且全部公开文件安全状态为 `SAFE` 的素材。公开响应不得返回上传票据、内部路径、后台备注、审核意见、审计字段、通知结果、作者敏感字段、隔离文件、拒绝文件或原始存储 key。

当前用户投稿接口使用 `/api/v1/materials/me` 前缀，全部要求登录。当前用户只能创建自己的上传会话和投稿，只能读取、编辑、提交、撤回或重新提交自己的素材。浏览器请求体中传入 `authorUserId`、`authorSnapshot`、`status`、`reviewerUserId`、`featuredBy`、`auditLogs` 等服务端字段时，必须忽略并以服务端上下文为准，生产实现推荐返回字段校验失败。

后台接口使用 `/api/v1/materials/admin` 前缀，全部要求登录。后台读取允许 `HELPER`、`ADMIN` 或 `OWNER`。审核通过、拒绝和要求修改允许 `HELPER`、`ADMIN` 或 `OWNER`。精选、取消精选、下架、归档、软删除、分类维护和文件安全状态维护要求 `ADMIN` 或 `OWNER`。审计列表和自检摘要只允许 `ADMIN` 或 `OWNER`。

`material` 当前由 `portal-core-service:8134` 承载。历史原服务端口 `8126` 只作为对照记录，不再作为当前网关默认上游。`api-gateway` 必须以兼容方式保留 `/api/v1/materials` 路由，不能改变已有服务路径、认证方式、响应格式或测试。

## auth 兼容契约

当前请求认证上下文至少包含 `userId`、`displayName`、`roles`、`permissions` 和 `status`。用户状态为 `DISABLED`、`BANNED` 或 `DELETED` 时不得创建上传会话、投稿或访问后台接口。

`material` 通过 api-gateway 访问时，优先读取网关注入的可信身份头：`X-Beiming-Actor-User-Id`、`X-Beiming-Actor-Roles`、`X-Beiming-Actor-Permissions`、`X-Beiming-Actor-Minecraft-Id` 和 `X-Gateway-Internal-Request-Id`。P0.1 中只有 `X-Gateway-Internal-Request-Id` 与当前 `X-Request-Id` 一致时，才接受这些 actor 头。客户端直接伪造的 actor 头必须忽略，不得覆盖 `Authorization` 校验结果。生产接入内部签名或 mTLS 前，自检摘要必须暴露 `GATEWAY_INTERNAL_SIGNATURE_NOT_ENABLED` 缺口。

后台写操作中的 `createdBy`、`updatedBy`、`submittedBy`、`reviewedBy`、`featuredBy`、`offlineBy`、`archivedBy` 和 `deletedBy` 均来自服务端认证上下文。auth 上下文不可用返回 `46700`，auth 调用超时返回 `46701`，auth 字段缺失或枚举不兼容返回 `46702`。

## profile 兼容契约

创建投稿和提交审核时，`material` 必须通过 profile 适配层或可信认证上下文读取作者公开快照。允许投稿的成员状态为 `ACTIVE` 和 `INACTIVE`。`SUSPENDED`、`REMOVED`、`ARCHIVED`、无档案、profile 不可用或 profile 字段不兼容时不得提交审核。

profile 成员不存在或不可公开返回 `46710`。profile 调用超时返回 `46711`。profile 字段缺失或枚举不兼容返回 `46712`。已公开素材可以继续返回已保存作者快照，并在后台详情中显示 `profileSnapshotAt`，但不能把旧快照用于新的成员资格判断。

## notification 兼容契约

审核拒绝和要求修改必须通知素材作者。强制通知失败时，素材状态不得变化，返回 `46720` 或 `46721`。审核通过、被精选、取消精选、下架、归档和软删除通知为辅助提醒，通知失败时主流程可以成功，但必须在审计中记录 `notificationStatus=FAILED` 和失败原因。

没有可通知作者时，强制通知场景返回 `46722`。辅助通知场景可跳过通知并写审计摘要。`material` 不保存通知主数据、未读数、模板或外部渠道投递记录。

## 上传和安全模型

P0 上传采用 `LOCAL_STUB` 模式。客户端先创建上传会话，服务端返回短期 `uploadTicket`、允许的 MIME、扩展名、大小上限、文件数量上限、checksum 要求和 stub 上传目标。客户端完成上传后调用 complete，服务端保存文件安全摘要并把文件状态置为 `SAFE` 或按测试头模拟为 `REJECTED`、`QUARANTINED`、`EXPIRED`。

上传会话是持有即授权的短期能力，必须绑定当前用户、用途、素材类型、允许文件数量、单文件大小、总大小、checksum、幂等键和过期时间。会话不得被其他用户完成，不得完成超过限制的文件，不得在过期后完成。重复 complete 同一会话和同一文件摘要应幂等返回同一 asset；相同幂等键搭配不同请求体返回 `43714`。

服务端必须生成文件名，原始文件名只保存脱敏显示名。扩展名白名单和文件签名摘要必须同时校验，不得只信任 `Content-Type`。危险扩展、双扩展、空字节、路径穿越、可执行脚本、压缩炸弹模拟、超大文件、checksum 不匹配和 MIME 伪造都返回 `43712` 或字段校验错误。文件不得通过内部路径直接公开访问，公开接口只能返回 material 控制的 `publicAssetUrl` 和安全摘要。

## 枚举

| 枚举 | 取值 | 说明 |
| --- | --- | --- |
| `MaterialKind` | `IMAGE`、`VIDEO`、`BUILD_SCREENSHOT`、`PROJECT_RECORD`、`EVENT_MEMORY`、`DOCUMENT_ATTACHMENT`、`OTHER` | 素材类型。 |
| `MaterialStatus` | `DRAFT`、`UPLOADING`、`PENDING_REVIEW`、`APPROVED`、`REJECTED`、`NEEDS_CHANGES`、`FEATURED`、`OFFLINE`、`ARCHIVED`、`DELETED` | 素材状态。`FEATURED` 是公开强化状态，取消精选后回到 `APPROVED`。 |
| `AssetStatus` | `PENDING_UPLOAD`、`UPLOADED`、`SCANNING`、`SAFE`、`REJECTED`、`QUARANTINED`、`EXPIRED` | 文件安全状态。只有 `SAFE` 文件可公开。 |
| `LicenseType` | `ORIGINAL`、`SERVER_SHARED`、`CC_BY_NC`、`CC_BY_SA`、`AUTHORIZED_REPOST` | 授权声明。 |
| `MaterialVisibility` | `PUBLIC`、`MEMBER_ONLY`、`PRIVATE` | P0 公开接口只返回 `PUBLIC`。 |
| `UploadProvider` | `LOCAL_STUB`、`S3_PRESIGNED`、`CLOUDINARY_UNSIGNED`、`OBJECT_STORAGE` | 上传提供方。P0 只实现 `LOCAL_STUB`。 |
| `MaterialAuditResult` | `SUCCESS`、`FAILED` | 素材审计执行结果。 |

## 通用对象

### MaterialCategory

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `categoryId` | string | 是 | 分类 ID。 |
| `name` | string | 是 | 分类名称，2 到 40 位，同一未归档分类中唯一。 |
| `slug` | string | 是 | 分类 slug，2 到 80 位，只允许小写字母、数字和短横线。 |
| `description` | string 或 null | 是 | 分类说明，最多 200 位。 |
| `sortOrder` | integer | 是 | 排序值，数字越小越靠前。 |
| `kind` | string | 是 | 适用素材类型。P0.1 创建分类未传时默认为 `IMAGE`。 |
| `enabled` | boolean | 是 | 是否启用。 |
| `archived` | boolean | 是 | 是否归档。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |
| `archivedAt` | string 或 null | 是 | 归档时间。 |

### MaterialAuthorSnapshot

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | string | 是 | auth 用户 ID。 |
| `memberId` | string 或 null | 是 | profile 成员 ID。 |
| `displayName` | string | 是 | 展示名快照。 |
| `avatarUrl` | string 或 null | 是 | 头像快照。 |
| `minecraftId` | string 或 null | 是 | Minecraft ID 快照。 |
| `memberStatus` | string 或 null | 是 | 成员状态快照。 |
| `profileSnapshotAt` | string | 是 | 快照获取时间。 |

### MaterialAssetSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `assetId` | string | 是 | 文件摘要 ID。 |
| `materialId` | string 或 null | 是 | 关联素材 ID，未关联投稿时为 `null`。 |
| `uploadSessionId` | string | 是 | 上传会话 ID。 |
| `provider` | string | 是 | `UploadProvider`。 |
| `status` | string | 是 | `AssetStatus`。 |
| `displayName` | string | 是 | 脱敏文件展示名。 |
| `extension` | string | 是 | 小写扩展名。 |
| `mimeType` | string | 是 | 服务端识别 MIME 摘要。 |
| `fileSizeBytes` | integer | 是 | 文件大小。 |
| `checksumSha256` | string | 是 | SHA-256。 |
| `width` | integer 或 null | 是 | 图片或视频宽度。 |
| `height` | integer 或 null | 是 | 图片或视频高度。 |
| `durationSeconds` | integer 或 null | 是 | 视频时长。 |
| `publicAssetUrl` | string 或 null | 是 | 安全公开 URL。未安全通过时为 `null`。 |
| `securityRejectReason` | string 或 null | 是 | 安全拒绝原因摘要，公开接口不得返回。 |
| `createdAt` | string | 是 | 创建时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### MaterialLicenseStatement

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `licenseType` | string | 是 | `LicenseType`。 |
| `authorConfirmed` | boolean | 是 | 投稿者确认有权投稿。 |
| `allowHomepageFeature` | boolean | 是 | 是否允许首页或专题精选展示。 |
| `allowDerivativeUse` | boolean | 是 | 是否允许二次编辑用于官网展示。 |
| `sourceUrl` | string 或 null | 是 | 授权转载来源 URL。`AUTHORIZED_REPOST` 必填。 |
| `creditText` | string 或 null | 是 | 署名文案，最多 120 位。 |

### PublicMaterialSummary

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `materialId` | string | 是 | 素材 ID。 |
| `slug` | string | 是 | 公开 slug。 |
| `kind` | string | 是 | `MaterialKind`。 |
| `status` | string | 是 | 公开接口只返回 `APPROVED` 或 `FEATURED`。 |
| `title` | string | 是 | 标题，2 到 120 位。 |
| `summary` | string 或 null | 是 | 摘要，最多 300 位。 |
| `coverAsset` | MaterialAssetSummary | 是 | 安全封面文件摘要。公开响应不含安全拒绝原因。 |
| `category` | MaterialCategory 或 null | 是 | 分类公开摘要。 |
| `tags` | string[] | 是 | 标签，最多 20 个。 |
| `author` | MaterialAuthorSnapshot | 是 | 作者公开快照。 |
| `license` | MaterialLicenseStatement | 是 | 授权公开摘要。 |
| `featured` | boolean | 是 | 是否精选。 |
| `publishedAt` | string | 是 | 公开时间。 |
| `updatedAt` | string | 是 | 更新时间。 |

### PublicMaterialDetail

`PublicMaterialDetail` 在 `PublicMaterialSummary` 基础上补充 `description`、`assets`、`visibleFrom`、`visibleUntil`、`createdAt`。公开详情仍不得返回后台备注、审核意见、上传票据、内部路径、通知结果或审计字段。

### UploadSessionView

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `uploadSessionId` | string | 是 | 上传会话 ID。 |
| `provider` | string | 是 | P0 固定为 `LOCAL_STUB`。 |
| `purpose` | string | 是 | 固定为 `MATERIAL_SUBMISSION`。 |
| `ownerUserId` | string | 是 | 当前用户 ID。 |
| `allowedExtensions` | string[] | 是 | 允许扩展名。 |
| `allowedMimeTypes` | string[] | 是 | 允许 MIME。 |
| `maxFileSizeBytes` | integer | 是 | 单文件大小上限。 |
| `maxFiles` | integer | 是 | 文件数量上限。 |
| `uploadTicket` | string | 是 | 短期上传票据，只在创建响应返回，后台详情不得返回。 |
| `uploadTarget` | string | 是 | stub 上传目标或未来对象存储目标。 |
| `status` | string | 是 | `PENDING_UPLOAD`、`UPLOADED` 或 `EXPIRED`。 |
| `expiresAt` | string | 是 | 过期时间。 |
| `createdAt` | string | 是 | 创建时间。 |

### MyMaterialSubmission

当前用户投稿视图包含素材主体、文件摘要、授权声明、作者快照、状态、提交时间、审核意见、后台要求修改公开意见、通知摘要和可执行动作。它不得返回其他用户投稿，也不得返回内部路径、上传票据和审计参数全文。

### AdminMaterialItem

后台素材视图包含素材主体、全部文件安全摘要、授权声明、作者快照、后台备注、审核意见、通知摘要、状态时间、操作者 ID、审计摘要和生产化提示。后台详情可以返回 `securityRejectReason`，但不得返回真实对象存储密钥、内部绝对路径、上传票据明文或通知正文全文。

### MaterialAuditLog

审计字段继承公共契约，允许补充 `materialId`、`assetId`、`uploadSessionId`、`idempotencyKey`、`stateFrom`、`stateTo`、`notificationStatus`、`profileSnapshotStatus` 和 `assetStatus`。审计日志不得通过 material API 删除。

## material 错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| `43700` | 404 | 素材不存在，或公开接口无权访问该素材。 |
| `43701` | 404 | 上传会话不存在或已过期。 |
| `43702` | 404 | 文件摘要不存在。 |
| `43703` | 404 | 分类不存在。 |
| `43710` | 409 | 素材状态不允许当前操作。 |
| `43711` | 409 | slug、分类或投稿指纹冲突。 |
| `43712` | 400 | 上传文件类型、大小、签名、checksum 或文件名不符合规则。 |
| `43713` | 400 | 授权声明缺失或不兼容。 |
| `43714` | 409 | 幂等键请求指纹冲突。 |
| `43715` | 409 | 文件安全状态不允许提交或公开。 |
| `43716` | 409 | 分类仍被未归档素材引用，不能归档。 |
| `46700` | 502 | auth 认证上下文不可用。 |
| `46701` | 504 | auth 认证上下文调用超时。 |
| `46702` | 502 | auth 响应字段或枚举不兼容 material 契约。 |
| `46710` | 502 | profile 作者快照不可用。 |
| `46711` | 504 | profile 作者快照调用超时。 |
| `46712` | 502 | profile 作者快照字段或枚举不兼容 material 契约。 |
| `46720` | 502 | notification 强制投递不可用。 |
| `46721` | 504 | notification 强制投递超时。 |
| `46722` | 502 | notification 强制投递缺少可通知作者。 |
| `46730` | 502 | 上传存储适配器不可用。 |
| `46731` | 504 | 上传存储适配器超时。 |
| `51700` | 500 | material 内部错误。 |
| `51701` | 500 | material 审计写入失败。 |
| `51702` | 500 | 上传记录写入失败。 |

字段校验、未登录、令牌格式错误、权限不足、分页错误、排序错误和通用服务端错误优先使用公共错误码。material 自有幂等冲突使用 `43714`。

## 接口总览

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

## 公开接口

### 精选素材列表

`GET /api/v1/materials/featured`

查询参数包括 `limit`、`kind`、`categoryId` 和 `tag`。`limit` 默认 `12`，最大 `50`。成功响应 HTTP `200`，`data.items` 为 `PublicMaterialSummary[]`，只返回 `FEATURED`、`PUBLIC`、文件安全、未下架、未归档、未软删除且处于可见时间范围内的素材。

### 公开素材列表

`GET /api/v1/materials`

查询参数包括 `page`、`pageSize`、`kind`、`categoryId`、`tag`、`authorUserId`、`keyword` 和 `sort`。`sort` 允许 `publishedAt_desc`、`updatedAt_desc`、`title_asc`、`featured_desc`。成功响应 HTTP `200`，分页 `items` 为 `PublicMaterialSummary[]`。

公开列表必须按过滤后的全集计算 `total`。空页返回空数组，不得回退第一页。`authorUserId` 只按已保存作者快照做公开筛选，不暴露作者敏感字段。公开列表不得返回未审核、需修改、已拒绝、已下架、已归档、已软删除、非公开可见、文件安全未通过、可见时间未开始或可见时间已结束的素材。

### 公开素材详情

`GET /api/v1/materials/{materialId}` 和 `GET /api/v1/materials/by-slug/{slug}`

成功响应 HTTP `200`，`data` 为 `PublicMaterialDetail`。素材不存在、不可公开或文件安全未通过时返回 `43700`，不得暴露后台状态。

### 公开分类列表

`GET /api/v1/materials/categories`

查询参数包括 `kind` 和 `keyword`。成功响应 HTTP `200`，`data.items` 为启用且未归档分类，按 `sortOrder` 和 `name` 稳定排序。

### 公开素材文件摘要

`GET /api/v1/materials/{materialId}/assets`

成功响应 HTTP `200`，`data.items` 为公开安全文件摘要。只返回 `SAFE` 文件，且不得返回 `securityRejectReason`、上传会话票据、内部路径或对象存储 key。素材不可公开返回 `43700`。

## 当前用户投稿接口

### 创建上传会话

`POST /api/v1/materials/me/upload-sessions`

请求字段包括 `kind`、`expectedFileNames`、`expectedMimeTypes`、`maxFileSizeBytes`、`checksumSha256` 和 `idempotencyKey`。`expectedFileNames` 为 1 到 10 个文件名。单文件大小上限不能超过模块配置上限。成功响应 HTTP `201`，`data` 为 `UploadSessionView`。

创建上传会话必须校验登录、用户状态、扩展名白名单、MIME 白名单、大小上限和 checksum 格式。相同用户、相同 `idempotencyKey`、相同请求体重复提交返回同一会话。相同幂等键搭配不同请求体返回 `43714`。

### 完成上传会话

`PATCH /api/v1/materials/me/upload-sessions/{uploadSessionId}/complete`

请求字段包括 `files`、`uploadTicket` 和 `idempotencyKey`。每个文件必须包含 `displayName`、`mimeType`、`extension`、`fileSizeBytes`、`checksumSha256`、`signature`、`width`、`height` 和 `durationSeconds`。成功响应 HTTP `200`，`data.items` 为 `MaterialAssetSummary[]`。

会话不存在、过期或不属于当前用户返回 `43701`。文件超限、扩展名非法、MIME 伪造、签名不符、checksum 不符、双扩展、路径穿越、空字节、危险脚本或压缩炸弹模拟返回 `43712`。上传记录写入失败返回 `51702`，不得产生半文件摘要。

### 创建投稿

`POST /api/v1/materials/me/submissions`

请求字段包括 `kind`、`slug`、`title`、`summary`、`description`、`categoryId`、`tags`、`assetIds`、`coverAssetId`、`visibility`、`license`、`visibleFrom`、`visibleUntil` 和 `idempotencyKey`。成功响应 HTTP `201`，`data` 为 `MyMaterialSubmission`，初始状态为 `DRAFT`。

创建投稿必须校验资产属于当前用户且状态为 `SAFE`，授权声明完整，slug 在未软删除素材中唯一，分类存在，公开可见性合法。`AUTHORIZED_REPOST` 必须填写 `sourceUrl`。授权不兼容返回 `43713`。资产不安全返回 `43715`。

### 我的投稿列表和详情

`GET /api/v1/materials/me/submissions` 支持 `page`、`pageSize`、`status`、`kind`、`keyword` 和 `sort`。`GET /api/v1/materials/me/submissions/{materialId}` 只允许读取当前用户自己的投稿。访问他人投稿返回 `43700`。

### 修改我的投稿

`PATCH /api/v1/materials/me/submissions/{materialId}`

请求字段同创建投稿，除 `reason` 可选外其余字段按需修改。只有 `DRAFT` 和 `NEEDS_CHANGES` 可修改主体字段。`PENDING_REVIEW` 可先撤回再修改。`APPROVED`、`FEATURED`、`OFFLINE`、`ARCHIVED` 和 `DELETED` 返回 `43710`。

### 提交审核

`PATCH /api/v1/materials/me/submissions/{materialId}/submit-review`

请求字段包括 `reason` 和可选 `idempotencyKey`。`DRAFT`、`REJECTED` 和 `NEEDS_CHANGES` 可流转为 `PENDING_REVIEW`。提交前必须再次校验 profile 作者快照、授权声明和所有公开文件为 `SAFE`。重复提交 `PENDING_REVIEW` 返回成功，保持幂等，不重复写审计。

### 撤回投稿

`PATCH /api/v1/materials/me/submissions/{materialId}/withdraw`

请求字段包括 `reason`。只有 `PENDING_REVIEW` 可撤回为 `DRAFT`。重复撤回 `DRAFT` 返回成功，保持幂等。已审核或已公开素材不能由用户撤回，返回 `43710`。

### 重新提交

`PATCH /api/v1/materials/me/submissions/{materialId}/resubmit`

请求字段包括 `reason`。`REJECTED` 和 `NEEDS_CHANGES` 可重新提交为 `PENDING_REVIEW`。重新提交等价于提交审核，但必须清理旧的公开修改意见快照并写审计。

## 后台素材接口

### 后台素材列表和详情

`GET /api/v1/materials/admin/items` 支持 `page`、`pageSize`、`status`、`kind`、`visibility`、`categoryId`、`authorUserId`、`assetStatus`、`keyword` 和 `sort`。`sort` 允许 `submittedAt_desc`、`updatedAt_desc`、`publishedAt_desc`、`title_asc`。成功响应分页 `items` 为 `AdminMaterialItem[]`。所有筛选条件必须在分页前生效。

`GET /api/v1/materials/admin/items/{materialId}` 成功响应 `AdminMaterialItem`。素材不存在返回 `43700`。

### 审核通过

`PATCH /api/v1/materials/admin/items/{materialId}/approve`

请求字段包括 `reviewOpinion`、`reason` 和可选 `idempotencyKey`。`PENDING_REVIEW` 可流转为 `APPROVED`，并写入 `reviewedAt`、`publishedAt` 和审核人。相同操作者、相同 `idempotencyKey` 和相同请求体重复审核返回首次结果，不重复写审计；相同 `idempotencyKey` 搭配不同请求体返回 `43714`。未传 `idempotencyKey` 时，重复审核已 `APPROVED` 返回成功，不重复写审计。所有文件必须为 `SAFE`，否则返回 `43715`。辅助通知失败时主流程成功但审计记录失败摘要。

### 审核拒绝

`PATCH /api/v1/materials/admin/items/{materialId}/reject`

请求字段包括 `reviewOpinion` 和 `reason`。`PENDING_REVIEW` 可流转为 `REJECTED`。拒绝必须通知作者，强制通知失败时状态不变并返回 `46720` 或 `46721`。重复拒绝保持幂等。

### 要求修改

`PATCH /api/v1/materials/admin/items/{materialId}/request-changes`

请求字段包括 `reviewOpinion`、`publicComment` 和 `reason`。`PENDING_REVIEW` 可流转为 `NEEDS_CHANGES`。要求修改必须通知作者，强制通知失败时状态不变。

### 精选、取消精选、下架、归档和软删除

`PATCH /api/v1/materials/admin/items/{materialId}/feature` 允许 `APPROVED` 流转为 `FEATURED`。重复精选保持幂等。只有 `license.allowHomepageFeature=true` 的素材可以精选，否则返回 `43713`。

`PATCH /api/v1/materials/admin/items/{materialId}/unfeature` 允许 `FEATURED` 流转为 `APPROVED`。重复取消精选保持幂等。

`PATCH /api/v1/materials/admin/items/{materialId}/offline` 允许 `APPROVED` 或 `FEATURED` 流转为 `OFFLINE`。重复下架保持幂等。下架后公开接口不可见。

`PATCH /api/v1/materials/admin/items/{materialId}/archive` 允许 `DRAFT`、`REJECTED`、`NEEDS_CHANGES` 和 `OFFLINE` 流转为 `ARCHIVED`。公开中的素材必须先下架再归档。

`PATCH /api/v1/materials/admin/items/{materialId}/delete` 只做软删除，状态为 `DELETED`，写入 `deletedAt`。公开中的素材必须先下架再软删除。P0 不提供真实删除接口。

这些状态接口请求字段均包含必填 `reason` 和可选 `idempotencyKey`。相同操作者、相同接口语义、相同 `idempotencyKey` 和相同请求体重复提交返回首次结果；相同 `idempotencyKey` 搭配不同请求体返回 `43714`。审计失败时不得改变业务状态。

## 后台分类接口

`GET /api/v1/materials/admin/categories` 支持 `includeArchived`、`enabled`、`kind` 和 `keyword`。`POST /api/v1/materials/admin/categories` 创建分类，`PATCH /api/v1/materials/admin/categories/{categoryId}` 修改分类，`PATCH /api/v1/materials/admin/categories/{categoryId}/archive` 归档分类。创建和修改字段包括 `name`、`slug`、`description`、`sortOrder`、`enabled`、`kind`、`reason` 和可选 `idempotencyKey`。分类名称或 slug 冲突返回 `43711`。仍被未归档、未软删除素材引用的分类不能归档，返回 `43716`。

## 后台文件安全接口

`GET /api/v1/materials/admin/assets` 支持 `page`、`pageSize`、`status`、`ownerUserId`、`materialId`、`extension`、`mimeType` 和 `sort`。`sort` 允许 `createdAt_desc`、`createdAt_asc` 和 `size_desc`。后台可以查看 `securityRejectReason`，但不得返回上传票据或内部绝对路径。

`PATCH /api/v1/materials/admin/assets/{assetId}/security-status` 请求字段包括 `status`、`securityRejectReason`、`reason` 和可选 `idempotencyKey`。允许在 `SCANNING`、`SAFE`、`REJECTED`、`QUARANTINED` 间维护安全状态。相同操作者、相同 `idempotencyKey` 和相同请求体重复提交返回首次结果；相同 `idempotencyKey` 搭配不同请求体返回 `43714`。把已公开素材的唯一公开文件改为非 `SAFE` 时，关联素材必须停止公开展示或返回文件安全冲突，不得继续公开危险文件。

## 审计和自检接口

`GET /api/v1/materials/admin/items/{materialId}/audit-logs` 支持 `page`、`pageSize`、`action`、`actorUserId`、`result`、`from`、`to` 和 `sort`。`sort` 允许 `createdAt_desc` 和 `createdAt_asc`。只有 `ADMIN` 和 `OWNER` 可访问。审计日志不得通过 material API 删除。

`GET /api/v1/materials/admin/ops/summary` 返回服务运行模式、端口、存储模式、auth/profile/notification/storage 适配模式、素材数量、待审核数量、精选数量、文件数量、安全状态统计、审计数量、幂等记录数量、生产化缺口和最近审计时间。摘要不得返回 token、上传票据、内部路径、后台备注、审核意见全文、通知正文、对象存储密钥或异常堆栈。

## 状态、幂等和并发

素材状态流转为 `DRAFT` 到 `PENDING_REVIEW`，`PENDING_REVIEW` 到 `APPROVED`、`REJECTED` 或 `NEEDS_CHANGES`，`REJECTED` 和 `NEEDS_CHANGES` 可重新提交，`APPROVED` 可设为 `FEATURED` 或下架为 `OFFLINE`，`FEATURED` 可取消精选回到 `APPROVED` 或下架为 `OFFLINE`，`OFFLINE` 可归档或软删除，`DRAFT`、`REJECTED` 和 `NEEDS_CHANGES` 可归档或软删除，`ARCHIVED` 原则上不可恢复，`DELETED` 为软删除终态。

文件安全状态和素材审核状态必须分开。文件未上传、校验中、隔离、拒绝或过期时，素材不能提交审核，也不能公开展示。素材审核通过不代表文件安全通过；文件安全通过也不代表审核通过。

创建上传会话、完成上传、创建投稿、提交审核、后台审核、后台状态操作、创建分类、修改分类和修改安全状态支持 `idempotencyKey`。同一操作者、同一接口语义、同一幂等键、同一请求体重复提交时返回同一结果。相同幂等键搭配不同请求体返回 `43714`。请求体指纹必须基于结构化 JSON 规范化结果，嵌套对象按字段名递归排序，数组保留顺序，不能依赖浏览器字段顺序或 Java `Map.toString()`。

P0.1 内存实现必须用本服务内临界区保护创建上传会话、完成上传、创建投稿、修改投稿、提交审核、撤回、重新提交、后台审核、后台状态操作、分类维护和文件安全状态修改。并发创建相同 slug、相同分类 slug、相同上传 complete 文件摘要时只能一个成功或一个幂等成功，其余返回冲突。公开读取允许读到更新前或更新后的完整状态，不能返回半更新对象。后续持久化实现必须把这些保护迁移为数据库事务、唯一约束、条件更新或等效机制。

## 审计要求

必须审计的动作包括创建上传会话、完成上传、创建投稿、修改投稿、提交审核、撤回、重新提交、审核通过、审核拒绝、要求修改、精选、取消精选、下架、归档、软删除、创建分类、修改分类、归档分类、修改文件安全状态、通知失败、profile 快照失败、上传记录失败和审计写入失败。

后台写操作必须记录 `reason`。当前用户提交审核、撤回和重新提交也必须记录原因或系统默认原因。审计字段继承公共契约。审计写入失败时，后台写操作和当前用户关键状态写操作不得假装成功，必须返回 `51701` 或 `51700`，并保持业务数据不变。

公开读取不强制写审计。上传安全拒绝可以写失败审计或安全日志，但不得泄露内部扫描规则。

## 失败降级

公开列表、精选列表、详情、分类和文件摘要在单个素材文件不可公开时应跳过不可公开素材或返回 `43700`，不能返回危险文件。素材主数据存储不可用时不能伪造成功。

auth 认证上下文失败时，当前用户和后台接口不得使用旧用户上下文继续写入。profile 失败时，不得提交新的投稿审核；已公开素材可使用已保存作者快照做公开展示。notification 强制投递失败时，拒绝和要求修改不得改变状态；辅助通知失败时主流程可成功，但必须保留失败摘要和审计记录。上传存储适配器不可用时，创建上传会话或完成上传返回依赖错误，不得生成可用票据或安全文件摘要。

## 验收口径

`material` API 文档按 `docs/contracts-material.md` 独立存在，并由 `.local-docs/tests-material.md` 记录本地测试闭环。本文档列出的每个接口都必须有自动化测试覆盖成功路径、字段校验、认证失败、权限不足、资源不存在、状态冲突、幂等或并发边界、状态流转、失败降级、审计要求和模块验收口径。

`material` 完成时必须满足以下条件：全部接口按本文档实现；公开接口不泄露后台字段、上传票据、内部路径、对象存储密钥、通知结果和审计字段；当前用户只能管理自己的投稿；后台接口按角色限制；上传会话、文件安全摘要、授权声明、profile 作者快照、审核通知、精选展示、状态流转、幂等、审计、自检摘要、端口配置和前序服务适配都有自动化测试；`.local-docs/tests-material.md` 中全部测试用例都有对应自动化验证；未实现时自动化测试必须先失败；实现后 material 在 `portal-core-service:8134` 中全部测试通过；api-gateway `/api/v1/materials` 路由指向 `8134` 并测试通过；旧 `material-service:8126` Maven 入口已退役且不得恢复；没有修改前序服务稳定接口；没有把 `.local-docs/` 提交到仓库；没有把玩家资源下载、Cloudreve 管理、服务器文件管理、容器、终端、节点执行、真实文件删除或真实对象存储密钥塞进 `material`。
