# 澄客 CRM 后端架构设计

| 文档信息 | 内容 |
| --- | --- |
| 文档版本 | v0.2 |
| 文档状态 | 架构评审稿 |
| 更新日期 | 2026-08-10 |
| 适用范围 | 澄客 CRM 后端服务 |
| 主要读者 | 后端、前端、测试、运维、技术负责人 |
| 架构形态 | 模块化单体 REST API |
| 关联文档 | [前端架构设计 v0.3](./frontend-architecture-v0.3.md) |

## 1. 总体结论

第一期采用模块化单体，不拆微服务。单个 Spring Boot 应用按认证、系统管理、线索、商机、客户、资源等业务模块隔离，在同一进程和数据库内保持清晰依赖边界。

推荐生产基线：

| 分类 | 选型 | 说明 |
| --- | --- | --- |
| JDK | Java 25 LTS | 当前 LTS 运行时，保持与 Spring Boot 3.5.x 的兼容性 |
| 核心框架 | Spring Boot 3.5.x | Spring Framework 6.2，生态兼容性优先 |
| Web | Spring MVC | REST API 和企业后台场景 |
| 安全 | Spring Security | 认证、方法权限和安全过滤链 |
| JWT | OAuth2 Resource Server + Nimbus JOSE JWT | Spring Security 原生 JWT 方案 |
| 数据访问 | MyBatis Spring Boot Starter 3.0.x | SQL 显式可控，匹配 Boot 3.5 |
| 数据库 | PostgreSQL 18 | 业务事实数据源；生产始终使用当前受支持的小版本 |
| 连接池 | HikariCP | Spring Boot 默认连接池 |
| 缓存与令牌 | Redis 7.4+ + Spring Data Redis + Lettuce | Refresh Token、缓存、限流和临时状态；上线时锁定受支持稳定小版本 |
| 数据库迁移 | Flyway | 版本化管理数据库结构 |
| JSON | Fastjson2（待最终确认） | 若启用，使用 Spring 6 扩展并做契约测试 |
| 参数校验 | Spring Validation | 后端最终输入校验 |
| API 文档 | springdoc-openapi 2.8.x | 与 Spring Boot 3.5 对应 |
| 对象映射 | MapStruct | DTO、Command、DO 和 VO 显式转换 |
| 代码生成 | Lombok | 减少样板代码，限制使用范围 |
| 定时任务 | Spring Scheduling + ShedLock | 线索自动回收等多实例任务 |
| 可观测性 | Actuator + Micrometer + OpenTelemetry | 健康、指标和追踪 |
| 测试 | JUnit 5 + Spring Boot Test + Testcontainers | 单元、集成和真实依赖测试 |
| 架构测试 | ArchUnit | 自动约束模块和分层依赖 |
| 构建 | Maven Wrapper | 固定构建环境 |

## 2. 为什么暂不使用微服务

当前产品仍处于业务模型成型阶段，登录、权限、组织、线索、商机和客户之间存在较强事务与数据关系。第一期拆微服务会增加分布式事务、接口版本、部署、链路追踪和故障排查成本。

模块化单体必须满足：

- 代码按业务模块隔离，而不是只按 Controller、Service、Mapper 横向堆放。
- 模块只通过公开 application service 或 domain event 协作。
- 禁止跨模块直接调用 Mapper 或访问内部表模型。
- 通过 ArchUnit 固化依赖方向。
- 当某模块具备独立团队、独立扩缩容或独立发布需求时，再评估拆分服务。

## 3. 工程与包结构

建议第一期使用单 Maven 模块和业务包隔离，避免过早建立大量 Maven 子模块。

```text
chengke-crm-server/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/chengke/crm/
    │   │   ├── ChengkeCrmApplication.java
    │   │   ├── bootstrap/
    │   │   ├── shared/
    │   │   │   ├── api/
    │   │   │   ├── audit/
    │   │   │   ├── error/
    │   │   │   ├── security/
    │   │   │   ├── tenant/
    │   │   │   └── observability/
    │   │   └── modules/
    │   │       ├── auth/
    │   │       ├── system/
    │   │       └── lead/
    │   │           ├── controller/
    │   │           ├── application/
    │   │           ├── domain/
    │   │           └── infrastructure/
    │   └── resources/
    │       ├── db/migration/
    │       ├── mapper/
    │       ├── application.yml
    │       └── application-local.yml
    └── test/
```

### 3.1 分层职责

| 层 | 职责 | 禁止事项 |
| --- | --- | --- |
| Controller | HTTP 协议、参数校验、DTO 转换 | 不写业务规则，不直接调用 Mapper |
| Application | 用例编排、权限入口、事务边界 | 不拼接 HTTP 响应，不依赖前端模型 |
| Domain | 状态机、业务规则、领域动作 | 不依赖 Spring MVC、MyBatis 实现 |
| Infrastructure | MyBatis、Redis、文件和外部系统 | 不决定业务权限和状态流转 |

依赖方向：`controller -> application -> domain`，`infrastructure` 实现 domain/application 所需端口。第一期不强制完整 DDD，但必须保留业务规则与数据访问分离。

## 4. Maven 依赖分组

### 4.1 第一期开启

| 依赖 | 用途 |
| --- | --- |
| `spring-boot-starter-web` | Spring MVC REST API |
| `spring-boot-starter-validation` | Bean Validation |
| `spring-boot-starter-security` | 认证与授权 |
| `spring-boot-starter-oauth2-resource-server` | Bearer JWT 校验 |
| `spring-boot-starter-data-redis` | Redis 和 Lettuce |
| `spring-boot-starter-cache` | Spring Cache 抽象 |
| `spring-boot-starter-actuator` | 健康检查和指标 |
| `mybatis-spring-boot-starter` | MyBatis 集成 |
| `postgresql` | PostgreSQL JDBC 驱动 |
| `flyway-core`、`flyway-database-postgresql` | 数据库迁移 |
| `springdoc-openapi-starter-webmvc-ui` | OpenAPI 文档 |
| `mapstruct`、`mapstruct-processor` | 类型映射 |
| `lombok` | 编译期样板代码生成 |
| `shedlock-spring` + JDBC Provider | 多实例定时任务互斥 |

### 4.2 测试依赖

- `spring-boot-starter-test`
- `spring-security-test`
- Testcontainers JUnit Jupiter、PostgreSQL 模块
- Testcontainers Redis 通过 GenericContainer 或团队统一封装
- ArchUnit JUnit 5
- Awaitility，用于异步任务和最终一致性测试

### 4.3 有明确需求后再引入

| 技术 | 引入条件 |
| --- | --- |
| 对象存储 SDK / MinIO | 跟进附件正式进入开发 |
| Bucket4j | 登录、刷新、验证码等接口需要细粒度限流 |
| Redisson | 出现 Spring Data Redis 无法满足的可靠分布式协调场景 |
| EasyExcel | 导入导出进入版本范围 |
| 消息队列 | 有可容忍最终一致性的跨系统异步任务 |
| Elasticsearch/OpenSearch | PostgreSQL 索引、全文检索与 `pg_trgm` 无法满足经过测量的搜索需求 |
| Quartz | 需要动态配置、复杂日历或可暂停恢复的任务 |

不在第一期预装 MyBatis-Plus、Druid、Kafka、RabbitMQ、Elasticsearch、工作流引擎和微服务全家桶。

## 5. API 设计规范

### 5.1 基础约定

- API 前缀：`/api/v1`。
- 资源路径使用复数名词，例如 `/leads`、`/lead-pools`。
- GET 查询不得产生业务副作用。
- POST 创建或动作、PUT 完整替换、PATCH 局部更新、DELETE 删除或停用。
- 领取、转化等领域动作使用语义化动作端点，例如 `POST /lead-pools/{poolId}/leads/{leadId}:claim`。
- 日期时间使用带时区 ISO 8601；服务端和数据库以 UTC 存储。
- 金额使用 `BigDecimal`，接口使用字符串或明确精度格式。
- 数据库 `BIGINT` ID 对前端优先序列化为字符串，避免 JavaScript 精度丢失。

### 5.2 响应与错误

成功响应统一包含 `code`、`message`、`data`、`requestId`、`timestamp`。分页 `data` 包含 `items`、`page`、`pageSize`、`total`。

错误响应使用 HTTP 状态 + 稳定业务码：

| HTTP | 场景 |
| --- | --- |
| 400 | 请求格式错误 |
| 401 | 未认证、Token 过期或无效 |
| 403 | 已认证但无权限 |
| 404 | 资源不存在或需要隐藏资源存在性 |
| 409 | 乐观锁、重复提交或领取冲突 |
| 422 | 业务字段或状态校验失败 |
| 429 | 触发限流 |
| 500 | 未预期服务端错误 |

前端只能根据稳定 `code` 判断业务逻辑，不依赖中文 message。

### 5.3 OpenAPI、springdoc 与 Swagger UI

三者职责必须明确区分：

| 名称 | 职责 |
| --- | --- |
| OpenAPI | 机器可读的接口描述标准，定义路径、参数、请求、响应、枚举和认证方式 |
| springdoc-openapi | 扫描 Spring MVC Controller、DTO 和注解，生成 OpenAPI JSON/YAML |
| Swagger UI | 读取 OpenAPI 文档，为前端和测试提供可视化浏览与在线调试页面 |

项目只需要引入 `springdoc-openapi-starter-webmvc-ui`，其中已经包含 Swagger UI。禁止再引入 Springfox 或另一套 Swagger 生成依赖。

默认访问地址：

| 地址 | 用途 | 使用者 |
| --- | --- | --- |
| `/v3/api-docs` | OpenAPI JSON | CI、代码生成和契约检查 |
| `/v3/api-docs.yaml` | OpenAPI YAML | 归档、评审和外部工具 |
| `/swagger-ui.html` | Swagger UI 入口 | 前端、测试和后端联调 |
| `/swagger-ui/index.html` | Swagger UI 实际页面 | 浏览器访问 |

### 5.4 文档内容要求

- Controller DTO、分页结构、枚举、错误响应和认证要求必须进入契约。
- 每个接口写清用途、所需权限、重要业务前置条件和可能返回的稳定错误码。
- Request/Response 字段需要中文说明、必填性、长度、格式和示例。
- 不在示例中使用真实姓名、手机号、邮箱、Token 或生产数据。
- 全局配置 Bearer JWT Security Scheme，使 Swagger UI 显示 `Authorize`，仅用于受控环境联调。
- 通用响应、分页、错误和枚举尽量复用 Schema，避免每个 Controller 重复定义。
- `@Operation`、`@Parameter`、`@Schema` 用于补充框架无法准确推断的语义，不为显而易见内容堆砌注解。

### 5.5 环境开放策略

| 环境 | OpenAPI JSON/YAML | Swagger UI | 规则 |
| --- | --- | --- | --- |
| local | 开启 | 开启 | 供开发调试 |
| test | 开启 | 开启 | 供前后端联调和测试 |
| staging | 开启 | 受限开启 | 仅公司网络、VPN 或认证用户访问 |
| production | 默认关闭外部访问 | 关闭 | CI 保存契约制品，需要时通过内部受控渠道查看 |

- Swagger UI 不能因为便于联调而绕过业务鉴权。
- staging 即使开放 UI，也不能在页面中固化账号、密码、Access Token 或 Refresh Token。
- OpenAPI 与 Swagger UI 端点的安全规则必须进入 Spring Security 配置和自动化测试。

### 5.6 前后端协作流程

```text
后端 Controller / DTO / Schema
              ↓
      springdoc-openapi 生成
              ↓
     /v3/api-docs（OpenAPI）
        ├── Swagger UI：人工查看与在线调试
        ├── 前端类型/API Client 生成
        └── CI 契约差异与破坏性变更检查
```

- 后端提交接口变更时同步更新注解和 Schema。
- CI 导出 OpenAPI，并与主分支进行破坏性变更检查。
- 前端生成类型的输入只能来自评审通过的 OpenAPI，不从 Swagger 页面手工复制类型。
- Swagger UI 适合查看和调试，但 PRD 仍负责业务流程，OpenAPI 仍是接口字段契约。

## 6. 参数校验与对象模型

建议对象类型：

| 类型 | 用途 |
| --- | --- |
| Request DTO | Controller 接收和 Bean Validation |
| Command / Query | Application 用例输入 |
| Domain Model | 业务状态与规则 |
| DO | MyBatis 数据库映射 |
| Response VO | API 输出 |

- Request DTO 不直接传给 Mapper。
- DO 不直接返回前端。
- 使用 MapStruct 处理结构映射，复杂业务转换写显式方法。
- 分组校验只用于少量确有差异的创建/编辑场景，避免复杂校验组继承。
- 数据库约束与后端校验同时存在；后端友好提示不能替代唯一键和非空约束。

## 7. JWT 认证架构

### 7.1 技术选择

统一使用 Spring Security OAuth2 Resource Server：

| 能力 | 组件 |
| --- | --- |
| JWT 签发 | `JwtEncoder` / `NimbusJwtEncoder` |
| JWT 验证 | `JwtDecoder` / `NimbusJwtDecoder` |
| Bearer 过滤链 | Spring Security Resource Server |
| 算法 | RS256 |
| Refresh Token | 不透明随机 Token，哈希存 Redis |

不额外引入 JJWT 或 Auth0 `java-jwt`，不手写一套绕开 SecurityFilterChain 的 JWT Filter。

### 7.2 Access Token

- 有效期 10-15 分钟。
- 必须校验 `iss`、`sub`、`aud`、`iat`、`nbf`、`exp`、`jti`。
- 自定义声明仅保留 `tenant_id`、`sid` 和必要粗粒度 `scope`。
- 不放菜单树、完整权限、组织树、联系方式和敏感业务数据。
- 使用 `kid` 支持密钥轮换，私钥存 KMS、Vault 或部署 Secret。
- 允许时钟偏差不超过 60 秒，服务器必须同步时间。

### 7.3 Refresh Token

- 使用至少 256 bit 安全随机数，不使用 JWT。
- 仅通过 `HttpOnly`、`Secure` Cookie 传输。
- Redis 保存哈希、Token Family、用户、租户、设备摘要、创建和过期时间。
- 每次刷新执行 Rotation，旧 Token 立即失效。
- 发现旧 Token 复用时撤销整个 Family。
- 建议绝对有效期 7 天；最长 30 天需额外风险评审。
- 退出、禁用账号、修改密码或高风险权限变更时撤销相关 Family。

### 7.4 密钥和撤销

- RS256 私钥仅认证模块可访问，业务资源服务只持有公钥。
- JWT Header 固定允许算法，拒绝 `none` 和非配置算法。
- 常规撤销依赖短 Access Token；高风险场景把 `jti` 或 `sid` 加入 Redis 撤销集合。
- 撤销记录 TTL 不短于 Access Token 剩余有效期。
- 密钥建议 90-180 天轮换，新旧公钥在过渡期并行验证。

## 8. 权限、菜单与数据范围

### 8.1 后端权威模型

后端登录上下文统一返回：用户摘要、租户、菜单树、功能权限码、数据范围、敏感字段权限、登录过期时间和授权首页。

- 菜单由后端组装，前端不根据角色名自行推导。
- `@PreAuthorize` 或等价方法权限负责功能级鉴权。
- 数据范围必须进入查询条件，不能先查询全部再在 Java 内存过滤。
- 敏感字段由后端决定返回明文或脱敏值，不能只依靠前端遮挡。
- 修改 URL、请求体中的用户 ID、部门 ID 或池 ID 不能绕过鉴权。

### 8.2 RBAC 与数据权限分离

| 权限类型 | 示例 |
| --- | --- |
| 功能权限 | `lead:view`、`lead:pool:claim` |
| 组织数据范围 | `self`、`department`、`department_and_descendants`、`selected_departments`、`all` |
| 资源成员权限 | 线索池 viewer/member/manager |
| 字段权限 | 完整手机号、完整联系方式 |

四类权限必须同时满足。集团节点员工不会自动获得下级部门数据。

### 8.3 多租户

- 所有业务表包含 `tenant_id`，唯一索引通常以 `tenant_id` 开头。
- 租户 ID 从认证上下文注入，禁止信任前端传入值。
- MyBatis 查询、更新和删除都必须包含租户条件。
- 超级管理员跨租户能力使用独立受控入口，不在普通查询中省略租户条件。
- 集成测试必须覆盖跨租户 ID 猜测和批量操作越权。

## 9. MyBatis 数据访问规范

- Mapper 接口和 XML 按业务模块归档。
- 简单单表语句可用注解；复杂查询、数据权限和动态条件使用 XML。
- 所有参数使用 `#{}` 绑定，禁止把用户输入放入 `${}`。
- 动态排序只接受后端白名单映射后的列名和方向。
- 分页使用明确的 `LIMIT/OFFSET` 或经过评审的分页组件，禁止加载全量后内存分页。
- 查询只选择需要字段，不使用无边界 `SELECT *`。
- 批量写入设置合理批次，单次默认不超过 500 条。
- SQL 超过性能阈值必须查看执行计划和真实数据量，不以开发数据判断。

### 9.1 数据范围实现

数据权限优先由 application 层形成明确的 `DataScope` 查询参数，再由 Mapper 生成 SQL。若使用 MyBatis Interceptor 自动注入租户或数据权限：

- 只解析受控 SQL，不对未知语句静默放行。
- 对子查询、联合查询、别名和批量语句有完整测试。
- 管理员绕过必须是显式、可审计的代码路径。
- 解析失败采用 fail closed，不能省略权限条件继续执行。

## 10. PostgreSQL 设计规范

### 10.1 基线

- PostgreSQL 18，使用 UTF-8 编码；生产始终升级到当前受支持的小版本。
- 业务对象默认放在应用专属 Schema；第一期可使用 `public`，但必须撤销无关角色的 `CREATE` 权限。
- 业务时间使用 `TIMESTAMPTZ(3)`，数据库会话和应用统一使用 UTC；展示时由应用转换用户时区。
- 主键使用 `BIGINT GENERATED BY DEFAULT AS IDENTITY`，或经过评审的 UUIDv7/有序分布式 ID。
- 金额使用 `NUMERIC`，禁止使用浮点类型存储金额。
- 逻辑删除只用于确需恢复或审计的实体，不能成为所有表默认模板。

### 10.2 表公共字段

建议业务表包含：`id`、`tenant_id`、`created_by`、`created_at`、`updated_by`、`updated_at`、`version`。需要软删除时增加 `deleted_at` 和 `deleted_by`。

### 10.3 索引规则

- 索引围绕真实查询、排序和租户条件设计。
- 联合索引通常以 `tenant_id` 开头，再放高选择性筛选和排序字段。
- 唯一约束在数据库层实现，例如租户内用户名、角色编码和业务编号。
- 避免在低区分度状态列上创建孤立索引。
- 模糊搜索优先评估 `pg_trgm` 与 GIN/GiST 索引；跨字段全文检索超出 PostgreSQL 能力后再评估搜索引擎。

### 10.4 Flyway

- 迁移脚本放在 `db/migration`，使用 `V<版本>__<说明>.sql`。
- 已进入共享环境的脚本不可修改，只能新增修复迁移。
- CI 执行 `validate` 和从空库完整迁移测试。
- 大表 DDL、索引创建和数据回填必须单独评审锁表风险。
- 生产迁移前备份并验证恢复流程；应用启动自动迁移还是发布任务迁移由部署方案确认。

## 11. Redis 设计规范

### 11.1 使用范围

| 场景 | 是否使用 Redis |
| --- | --- |
| Refresh Token 与撤销 | 是 |
| 授权上下文短缓存 | 是 |
| 字典和低频配置缓存 | 是 |
| 登录/刷新限流 | 是 |
| 线索领取最终一致性 | 否，以 PostgreSQL 原子更新为准 |
| 业务事实数据 | 否，PostgreSQL 为准 |

### 11.2 Key 和 TTL

Key 格式：`chengke:{env}:{tenant}:{domain}:{purpose}:{id}`。

- 所有缓存必须设置 TTL，除非经过明确评审。
- value 使用带版本的 DTO，不直接序列化数据库实体。
- 删除或更新数据时精确失效相关 key，不使用生产 `KEYS` 扫描。
- 热点 key、超大 value 和批量删除需监控。
- 缓存穿透使用空值短缓存或请求合并；布隆过滤器只在有测量依据时引入。

### 11.3 安全与持久化

- Redis 只部署在可信网络，不暴露公网。
- 使用 ACL 命名用户和最小命令权限，生产启用 TLS。
- 禁止业务账号执行 `FLUSHALL`、`CONFIG` 等管理命令。
- Refresh Token 等安全状态需要持久化时启用 AOF everysec，并结合 RDB 备份。
- 应用必须处理 Redis 超时；鉴权刷新场景 fail closed，普通缓存场景可回源 PostgreSQL。

## 12. 事务、并发与幂等

### 12.1 事务

- `@Transactional` 放在 application service 公共方法。
- 默认事务只覆盖一个业务用例，不把外部 HTTP、文件上传或长耗时计算放在数据库事务中。
- 只读查询标记 `readOnly = true`，但不能把它当作安全保证。
- 明确异常回滚规则，禁止捕获异常后返回成功导致事务提交。

### 12.2 乐观锁

编辑请求携带 `version`，更新 SQL 包含 `WHERE id = ? AND version = ?`，成功后版本递增。影响行数为 0 时返回稳定的 `*_VERSION_CONFLICT`。

### 12.3 原子状态流转

线索领取不依赖 Redis 分布式锁，使用 PostgreSQL 条件更新：只有当前仍在池、无负责人、状态可领取且版本匹配时才执行更新，并可通过 `RETURNING` 取得成功记录。并发领取最多一人影响一行，其余返回 `LEAD_ALREADY_CLAIMED`。

### 12.4 幂等

- 登录刷新使用 Refresh Token Rotation 保证一次性。
- 转商机通过线索唯一约束和事务保证只成功一次。
- 支付类能力未来使用业务幂等键和数据库唯一约束。
- 通用 `Idempotency-Key` 只用于明确需要安全重放的 POST，不对所有请求强制套用。
- 批量操作逐条鉴权，允许部分成功并返回稳定明细。

## 13. 定时任务

线索自动回收、过期提醒和临时数据清理由 Spring Scheduling 执行，ShedLock 保证多实例同一任务在一个时间窗口只有一个执行者。

- 锁优先存 PostgreSQL，避免 Redis 故障同时影响 Token 和任务协调。
- 任务必须可重入、幂等、可分批和可续跑。
- 单批处理数量有上限，记录游标或最后处理 ID。
- 每次执行记录开始、结束、处理数、失败数和 request/job ID。
- 单条失败不得导致整批永久停止；失败进入重试或人工处理清单。
- 动态任务、复杂日历和暂停恢复需求出现前不引入 Quartz。

## 14. JSON 与 Fastjson2

### 14.1 建议

Spring Boot 默认 Jackson 的兼容性最好。如果没有经过验证的性能或既有代码要求，建议保留 Jackson 作为 HTTP 消息转换器。

若团队确定使用 Fastjson2：

- 使用 `com.alibaba.fastjson2` 新包和 `fastjson2-extension-spring6`。
- AutoType 保持关闭，启用 SafeMode，不开启宽松智能匹配处理不可信输入。
- 全项目只使用一种 HTTP JSON 注解，不混用 Jackson 与 Fastjson2 注解。
- 对日期、枚举、`Long`、`BigDecimal`、空值、泛型响应和异常响应做契约测试。
- 验证 Spring Security、springdoc-openapi、Redis Serializer 和第三方 Starter 兼容性。
- 安全公告进入依赖升级流程，Fastjson2 版本通过 Maven 属性集中锁定。

Fastjson2 可用于明确的内部高性能场景，不代表必须替换 Spring MVC 全局序列化器。

## 15. Lombok 与 MapStruct

### 15.1 Lombok

- DTO 可使用 `@Getter`、`@Setter`、`@Builder`。
- 数据库实体避免无差别使用 `@Data`。
- 密码、Token、手机号、微信和邮件不得进入自动 `toString`。
- 领域对象优先显式构造和业务方法，避免所有字段公开 setter。
- 构造器注入优先，可使用 `@RequiredArgsConstructor`。

### 15.2 MapStruct

- Mapper 仅负责结构转换，不访问数据库和远程服务。
- 敏感字段是否脱敏由专门策略决定，不靠自动字段拷贝。
- 枚举转换失败必须显式处理，不能静默变成 null。
- 时间、金额和 ID 类型转换集中配置。

## 16. 审计与安全

### 16.1 审计

新建、编辑、授权、禁用用户、领取、分配、退回、收回、转池、跟进、转商机、判定无效和恢复必须记录审计。

审计记录：租户、操作人、目标资源、动作、前后关键状态、原因、时间、结果和 requestId。不得记录密码、Token、完整联系方式或跟进内容原文。

### 16.2 安全基线

- 密码使用 `DelegatingPasswordEncoder`，基线 BCrypt；成本参数通过部署环境压测确定。
- 登录、刷新、忘记密码和验证码接口限流。
- CORS 使用明确 Origin 白名单，携带凭证时禁止 `*`。
- Cookie 设置 `HttpOnly`、`Secure`、合理 SameSite、Path 和 Domain。
- 文件上传校验大小、扩展名、MIME、真实文件类型和访问权限，存储在 Web Root 之外。
- 日志、异常、OpenAPI 示例和监控标签不得包含敏感字段。
- 依赖漏洞扫描和镜像扫描进入 CI/CD。

## 17. 异常处理

使用统一 `@RestControllerAdvice` 将异常映射为稳定错误响应：

| 异常类型 | 处理 |
| --- | --- |
| 参数绑定/校验 | 400/422 + fieldErrors |
| 未认证 | 401，由 Security AuthenticationEntryPoint 统一输出 |
| 无权限 | 403，由 AccessDeniedHandler 统一输出 |
| 资源不存在 | 404 |
| 版本/并发冲突 | 409 |
| 业务规则失败 | 422 + 稳定业务码 |
| 未知异常 | 500，外部返回通用信息，内部记录 requestId |

禁止把堆栈、SQL、表名、服务器路径和内部类名返回前端。

## 18. 可观测性

### 18.1 日志

- 使用 SLF4J + Logback，生产输出结构化 JSON。
- MDC 至少包含 `requestId`、`traceId`、`tenantId`、`userId`。
- HTTP 访问日志不记录请求体中的敏感字段。
- SQL 参数日志仅限 local/test，生产关闭完整参数输出。

### 18.2 指标

Actuator + Micrometer 输出 JVM、HTTP、连接池、MyBatis 查询、Redis、登录失败、领取冲突、定时任务和业务动作指标。

- 指标标签保持低基数，不使用 userId、leadId、手机号作指标标签。
- 健康端点区分 liveness 和 readiness。
- Actuator 只暴露必要端点并单独保护。

### 18.3 追踪

使用 Micrometer Tracing 和 OTLP 输出到 OpenTelemetry 后端。异步任务和线程池通过 TaskDecorator 传播上下文。业务代码优先使用 Micrometer Observation API，不直接绑定具体追踪厂商。

## 19. 测试策略

| 层级 | 目标 |
| --- | --- |
| 单元测试 | 领域状态机、权限组合、校验和纯函数 |
| Mapper 测试 | 使用 Testcontainers PostgreSQL 验证真实 SQL、索引和租户条件 |
| Redis 测试 | Token Rotation、撤销、TTL 和缓存失效 |
| Controller 测试 | MockMvc 验证协议、错误响应和 Spring Security |
| 集成测试 | SpringBootTest + Testcontainers 验证完整用例和事务 |
| 架构测试 | ArchUnit 验证分层和模块依赖 |
| 契约测试 | OpenAPI、JSON、日期、Long、枚举和错误码 |

必须覆盖：

- 跨租户和跨部门越权。
- 禁用账号、撤销权限和 Token 复用。
- 两人同时领取同一线索。
- 乐观锁编辑冲突。
- 转商机重复提交。
- 批量操作部分成功。
- Redis 不可用时刷新失败关闭、普通缓存回源。
- Flyway 从空库迁移和历史库升级。

## 20. 配置、密钥与环境

| 环境 | 用途 |
| --- | --- |
| local | 本地开发，可用 Docker Compose 启动 PostgreSQL/Redis |
| test | 自动化集成和产品测试 |
| staging | 生产同构验证 |
| production | 正式环境 |

- 本地 `local` Profile 默认连接 `jdbc:postgresql://localhost:5432/chengke_crm`，账号为 `chengke_app`。
- 本地密码通过 `${CHENGKE_DB_PASSWORD:Root123}` 提供默认值，仅用于个人开发；共享、测试和生产环境必须通过 Secret 或环境变量覆盖。
- 本地启动时使用 `--spring.profiles.active=local` 或设置 `SPRING_PROFILES_ACTIVE=local`。
- `application.yml` 只放非敏感默认值，环境差异通过环境变量或外部配置注入。
- 数据库密码、Redis 凭证、JWT 私钥和对象存储密钥进入 Secret 管理系统。
- 不在仓库提交 `.env`、私钥、生产连接串和真实账号。
- 配置属性使用 `@ConfigurationProperties` 和启动时校验，避免散落 `@Value`。
- 功能开关有默认值、所有者和清理日期，不能永久堆积。

## 21. 构建、部署与发布

- 使用 Maven Wrapper 和 Spring Boot BOM 管理依赖版本。
- CI 顺序：格式/静态检查、编译、单测、架构测试、集成测试、OpenAPI 差异、Flyway 验证、构建、依赖扫描。
- 构建 OCI 镜像，使用非 root 用户运行，镜像内不包含密钥。
- 容器设置 JVM 内存和优雅停机，readiness 通过后才接收流量。
- 数据库迁移在应用流量切换前执行；破坏性字段变更使用 expand/migrate/contract 三阶段。
- 发布失败可回滚应用版本，但数据库回滚依赖前向修复迁移和备份恢复方案。

## 22. 性能与容量基线

- 所有列表服务端分页，默认 20，最大 100。
- 数据库连接池大小根据实例数、数据库上限和压测确定，不照搬固定大值。
- 常规 API P95 目标小于 500ms；登录和复杂列表另设目标。
- 慢 SQL 阈值建议从 500ms 起步并按环境调整。
- Redis 命令超时和数据库查询超时必须配置，禁止无限等待。
- 压测至少覆盖用户列表、线索列表、线索领取、跟进新增和 Token 刷新。

## 23. 架构验收清单

- [ ] Java 25、Spring Boot 3.5.x、MyBatis Starter 3.0.x 兼容版本已锁定。
- [ ] 模块和分层依赖通过 ArchUnit 自动验证。
- [ ] Controller 不直接调用 Mapper，DO 不作为 API 响应。
- [ ] JWT 使用 Spring Security Nimbus、RS256 和固定算法校验。
- [ ] Refresh Token 哈希存 Redis并启用 Rotation 与复用检测。
- [ ] 菜单、功能权限、数据范围、池成员和字段权限均由后端校验。
- [ ] 所有业务 SQL 包含租户和数据范围条件。
- [ ] PostgreSQL 具有必要唯一约束、版本字段和核心查询索引。
- [ ] Flyway 能从空库完整迁移，已发布脚本不可修改。
- [ ] 线索领取、转商机和批量动作具备并发与幂等测试。
- [ ] Redis 不作为业务事实源，key、TTL、ACL 和故障策略明确。
- [ ] 定时任务使用 ShedLock 且任务本身幂等。
- [ ] OpenAPI、稳定错误码和前端类型生成可在 CI 验证。
- [ ] local/test 可访问 Swagger UI，staging 受限，production 默认关闭。
- [ ] Swagger UI 能使用 Bearer JWT 联调，但不能绕过接口权限和数据权限。
- [ ] 项目只使用 springdoc-openapi，不引入 Springfox 等重复接口文档方案。
- [ ] 日志、指标、追踪、审计和健康检查可用且不泄露敏感数据。
- [ ] Testcontainers 覆盖 PostgreSQL、Redis 和权限关键链路。

## 24. 待确认项

1. 是否确认使用 JWT，而不是同域 Web 场景更简单的 Redis Session。
2. JWT 由本系统签发，还是接入企业 OIDC/统一身份平台。
3. 是否强制 Fastjson2 替换 Jackson；建议保留 Jackson，除非验证出明确需求。
4. 是否从第一期启用多租户；当前权限模型建议保留 `tenant_id`。
5. 前后端是否同域部署，以确定 Cookie、CORS 和 CSRF 配置。
6. 附件使用哪种对象存储、病毒扫描和保存周期。
7. 审计日志保存周期及是否进入独立存储。
8. 生产采用容器平台还是传统服务器，以及监控平台选型。
9. 数据库迁移由应用启动执行还是独立发布任务执行。

## 25. 官方参考

- [Spring Boot 3.5 系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [Spring Security Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [MyBatis Spring Boot Starter 兼容矩阵](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
- [PostgreSQL 版本支持策略](https://www.postgresql.org/support/versioning/)
- [PostgreSQL 18 文档](https://www.postgresql.org/docs/18/)
- [Redis 安全](https://redis.io/docs/latest/operate/oss_and_stack/management/security/)
- [Redis 持久化](https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/)
- [Flyway Migrations](https://documentation.red-gate.com/fd/migrations-271585107.html)
- [springdoc-openapi](https://springdoc.org/)
- [Spring Boot Observability](https://docs.spring.io/spring-boot/reference/actuator/observability.html)
- [Fastjson2](https://github.com/alibaba/fastjson2)

## 26. 变更记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| v0.2 | 2026-08-10 | 明确 OpenAPI、springdoc-openapi 与 Swagger UI 的关系；将业务数据库统一为 PostgreSQL 18，同步调整 JDBC、Flyway、Testcontainers、SQL 与运维规范 |
| v0.1 | 2026-08-04 | 建立后端技术基线、模块分层、JWT、权限、MyBatis、关系型数据库、Redis、事务、任务、测试和部署规范 |
