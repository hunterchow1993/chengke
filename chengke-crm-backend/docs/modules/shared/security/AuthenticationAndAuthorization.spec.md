# 认证与授权（JWT Resource Server）业务规格说明

| 文档信息 | 内容 |
| --- | --- |
| 文档类型 | 业务 Spec（基于代码考古生成） |
| 文档版本 | v1.4 |
| 更新日期 | 2026-09-05 |
| 对应模块 | `shared/security` |
| 状态说明 | **已确认业务规则** = 代码行为明确且一致；**当前代码行为** = 已实现但可能与产品/架构文档不一致；**待确认** = 无法从代码推断；**疑似 Bug** = 代码矛盾或未完成的实现 |

---

## 功能名称

**认证与授权（JWT Resource Server + 操作者上下文）**

---

## 1. 功能目标

为澄客 CRM 后端 API 提供统一的访问控制基础能力：

- **认证**：除公开路径外，所有 HTTP 请求必须携带可验证的 Bearer JWT；未通过认证的请求不得进入业务处理。
- **功能权限**：从 JWT 解析功能权限码，供各业务 API 判断操作者是否具备 view/create/update 等能力。
- **数据权限（组织范围）**：从 JWT 解析租户 ID、操作者 ID、可管理部门范围，供业务模块限定读写边界。
- **请求追踪**：为每个已认证请求提供 `requestId`，写入统一 API 响应。
- **fail-closed**：认证信息缺失、无效或不完整时，拒绝访问，不降级为匿名或默认租户。

本模块**不负责**：用户登录、Token 签发、Token 刷新、权限配置管理、组织树计算。用户登录、会话、验证码、首次改密、授权上下文计算与 Token 签发的契约见 [登录与会话认证模块总览 Spec](../../modules/auth/LoginController.spec.md)。访问令牌投递已确认为 `Authorization: Bearer`（登录总览 §4.3）。7 天保持登录如何换票仍见登录总览 §18 P0-3。

---

## 2. API

本模块**不对外暴露独立 REST 业务接口**。其「API 契约」包含两部分：**HTTP 访问边界** 与 **JWT Token 声明契约**。

---

### 2.1 HTTP 访问边界（全局）

适用于所有进入后端的 HTTP 请求。

| 项 | 内容 |
| --- | --- |
| 名称 | 全局 JWT 认证边界 |
| 作用范围 | 除白名单外的全部路径 |
| 调用时机 | 每个 HTTP 请求到达业务 Controller 之前 |

**白名单路径（无需 JWT、无需表单登录）**

| 路径模式 | 说明 |
| --- | --- |
| `/v3/api-docs` | OpenAPI JSON（精确路径；`/**` 不匹配自身） |
| `/v3/api-docs/**` | OpenAPI 分组文档与 YAML |
| `/swagger-ui.html` | Swagger UI 入口（通常 302 到 index） |
| `/swagger-ui/**` | Swagger UI 静态资源 |
| `/actuator/health` | 健康检查 |

登录模块的公开接口（设计建议：`POST /api/v1/auth/login`、`GET /api/v1/auth/captcha`）**当前不在白名单中**。路径与范围以 [登录模块总览 Spec §4.1](../../modules/auth/LoginController.spec.md) 评审结果为准；签发落地时必须把已确认的公开路径加入本表，否则无法匿名登录。

**过滤链加载（已确认）**

- 应用启动时**始终**注册一条 `SecurityFilterChain`，不依赖 `JwtDecoder` 是否存在。
- 关闭默认表单登录与 HTTP Basic，禁止把未认证请求重定向到 `/login`。
- 存在 `JwtDecoder` 时，非白名单路径按 OAuth2 Resource Server 校验 Bearer JWT。
- 不存在 `JwtDecoder` 时（本地未配置 issuer / JWK），白名单仍可匿名访问；非白名单返回 **401**，不回退到可登录的默认用户。

**非白名单路径**

| 项 | 规则 |
| --- | --- |
| 请求头 | 须携带 `Authorization: Bearer <JWT>` |
| 认证失败 | 请求被拒绝，不进入业务 Controller；HTTP **401**，不是 302 `/login` |
| CSRF | 不适用（无状态 API） |

**待确认**

- 生产环境是否必须配置 `JwtDecoder`（issuer-uri / jwk-set-uri）。未配置时受保护 API 全部 401，应用仍可启动并开放文档白名单。
- 认证失败时 HTTP 响应体是否统一为 `ApiResponse` 格式（当前全局异常处理器未覆盖 Spring Security 认证异常）。

---

### 2.2 JWT Token 声明契约（输入）

由外部认证服务签发、本模块消费。业务 API **不得**从请求参数或 Header 中读取租户/操作者身份覆盖 JWT。

| Claim / 字段 | 必填 | 校验规则 | 业务含义 |
| --- | --- | --- | --- |
| `sub` | 是 | 合法 UUID 字符串 | 当前操作者用户 ID |
| `tenant_id` | 是 | 合法 UUID 字符串 | 当前租户 ID |
| `scope` | 待确认 | 功能权限码列表 | 经转换后成为操作者功能权限集合 |
| `manageable_department_ids` | 否 | UUID 数组；缺失或非数组 → 视为空集合 | 操作者可管理的部门 ID 列表 |
| `permission_version` | 是（新签发的 Access Token） | 非空字符串，与 Redis 租户授权版本一致 | 判断 JWT 权限快照是否过期 |

**功能权限码写入方式（待确认）**

- **当前代码行为**：JWT 的 `scope` Claim 经转换后写入 Security Authorities，Authority 前缀为空（权限码形如 `system:department:view`，不带 `SCOPE_` 前缀）。
- 若 Authority 仍带 `SCOPE_` 前缀，解析时会剥前缀后写入权限集合（兼容处理）。

**特殊权限码（已确认）**

| 权限码 | 含义 |
| --- | --- |
| `system:department:manage:all` | 操作者拥有租户内**全量**部门管理数据范围 |

**Token 标准字段（待确认）**

架构文档要求校验 `iss`、`aud`、`exp` 等；具体校验规则由 JWT 解码器配置决定，本模块代码未单独定义。

---

### 2.3 操作者上下文（内部输出契约）

业务模块通过「获取当前操作者」能力取得以下结构（非 HTTP 响应体）：

| 字段 | 来源 | 业务含义 |
| --- | --- | --- |
| `tenantId` | `tenant_id` Claim | 本次请求所属租户 |
| `actorId` | `sub` | 本次请求操作者 |
| `permissions` | JWT scope → Authorities | 功能权限码集合 |
| `manageableDepartmentIds` | `manageable_department_ids` Claim | 可管理部门 ID 集合 |
| `manageAllDepartments` | `permissions` 含 `system:department:manage:all` | 是否全量部门管理范围 |
| `requestId` | 请求 MDC；缺失则生成随机 ID | 请求追踪标识 |

**约束（已确认）**

- 不读取客户端自定义租户 Header、操作者 Header 或请求体中的 tenantId/actorId。
- 每次业务请求在处理时即时解析，不跨请求缓存操作者上下文。

---

### 2.4 业务 API 的功能权限绑定（消费方，已确认）

以下权限码由部门管理等业务 API 使用；本模块负责提供解析与校验基础，权限码清单随业务模块扩展。

| 权限码 | 典型用途 |
| --- | --- |
| `system:department:view` | 部门读接口 |
| `system:department:create` | 新增部门 |
| `system:department:update` | 编辑部门 |
| `system:department:move` | 移动部门 |
| `system:department:status` | 启停部门 |
| `system:department:delete` | 软删除部门 |
| `system:department:member:view` | 查看直属成员计数 |
| `system:department:manage:all` | 租户全量部门数据范围 |

---

## 3. 业务规则

描述格式：**触发条件 → 业务判断 → 系统行为**

---

### 3.1 全局 HTTP 认证

**触发条件** → 请求路径不在白名单内

**业务判断**
- 是否携带 Bearer JWT
- JWT 能否通过解码器验证（签名、有效期等）

**系统行为**
- 路径在白名单 → 直接放行，不校验 JWT，不重定向登录页
- 验证通过 → 请求继续，JWT 写入安全上下文
- 验证失败或缺少 Token → 请求被拒绝，HTTP 401，不执行业务逻辑，不重定向 `/login`

**数据变化** → 无

---

### 3.2 解析当前操作者

**触发条件** → 业务 API 需要租户/操作者/权限/数据范围信息

**业务判断 — 决策表**

| 条件 | 结果 |
| --- | --- |
| 安全上下文无认证对象，或未认证，或 Principal 不是 JWT | 拒绝：`AUTHENTICATION_REQUIRED`，401，「请先登录」 |
| `tenant_id` 缺失或非法 UUID | 拒绝：`AUTHENTICATION_INVALID`，401，「认证信息不完整」 |
| `sub` 缺失或非法 UUID | 同上 |
| `manageable_department_ids` 含非法 UUID | 同上 |
| 全部通过 | 返回操作者上下文 |

**系统行为**
- 从 JWT 组装操作者上下文（见 §2.3）
- `manageable_department_ids` 缺失 → 可管理部门集合为空
- MDC 无 `requestId` → 生成随机 UUID 作为 requestId

**数据变化** → 无

---

### 3.3 功能权限判断

**触发条件** → 业务 API 校验操作者是否具备某功能权限码

**业务判断** → 操作者 `permissions` 是否包含目标权限码

**系统行为**
- 包含 → 允许继续（仍可能有数据范围限制）
- 不包含 → 由业务模块返回 forbidden（部门模块为 403 `DEPARTMENT_OPERATION_FORBIDDEN`）

**说明（已确认）**：业务 API 通常在 HTTP 层与方法/security 表达式层各校验一次功能权限（双层校验）。

---

### 3.4 组织数据权限判断（写）

**触发条件** → 业务写操作需确认目标部门是否在操作者管理范围内

**业务判断 — 决策表**

| manageAllDepartments | 目标部门 ID ∈ manageableDepartmentIds | 结果 |
| --- | --- | --- |
| true | — | 允许管理 |
| false | true | 允许管理 |
| false | false | 不允许管理 |

**系统行为（部门模块消费时的已确认行为）**
- 不允许管理 → 返回 404「部门不存在」（不暴露越权信息）
- 允许管理 → 继续执行业务写逻辑

---

### 3.5 组织数据权限判断（读）

**触发条件** → 业务读操作需确定可见部门集合

**业务判断**
- 全量管理员 → 租户内全部未删除部门可见
- 范围管理员 → 可见集合 = 可管理部门 + 其全部祖先（祖先为只读定位用途）

**系统行为**
- 目标不在可见集合 → 404「部门不存在」
- 在可见集合 → 返回数据；祖先节点标记为只读

**说明**：读可见性规则由业务模块（如部门查询）实现，本模块仅提供 `canManage` 与权限集合。

---

### 3.6 全量管理员创建集团根（消费方规则，已确认）

**触发条件** → 操作者创建无 parentId 的部门（集团根）

**业务判断** → `manageAllDepartments` 必须为 true

**系统行为**
- false → 403「仅租户级管理员可创建集团根节点」
- true → 允许（还须具备 create 功能权限）

---

## 4. 数据模型

### 4.1 操作者上下文（核心领域对象）

等价于代码中的 `CurrentActor`，表示**单次已认证请求**的操作者快照。

| 字段 | 业务含义 | 关系 |
| --- | --- | --- |
| `tenantId` | 租户隔离键 | 所有业务数据查询须限定此租户 |
| `actorId` | 操作者 | 审计、负责人引用 |
| `permissions` | 功能权限集合 | 与 `@PreAuthorize`/业务校验表达式一致 |
| `manageableDepartmentIds` | 可管理部门 ID | 写操作数据范围 |
| `manageAllDepartments` | 全量部门范围标志 | true 时忽略 manageableDepartmentIds 的写范围限制 |
| `requestId` | 请求追踪 ID | 写入 `ApiResponse.requestId` |

**字段关系**
- `manageAllDepartments=true` 当且仅当 `permissions` 含 `system:department:manage:all`（**当前代码行为**）
- 功能权限与组织数据范围相互独立：可有 view 权限但无可管理范围

### 4.2 JWT Token（输入）

见 §2.2。非持久化实体，仅请求生命周期内有效。

### 4.3 数据库表

**本模块不涉及数据库表。** 租户、用户、角色、组织范围的数据来源为 JWT Claims 及外部认证/授权服务（**待确认**持久化归属）。

### 4.4 HTTP 响应中的 requestId

所有经 `ApiResponse` 封装的成功/业务错误响应均携带 `requestId`，取值来自操作者上下文或 MDC。

---

## 5. 状态流转

本模块无业务 status 字段。认证状态为二元：

| 当前状态 | 操作/条件 | 新状态 | 是否允许 |
| --- | --- | --- | --- |
| 未认证 | 提供有效 JWT | 已认证 | ✓ |
| 未认证 | 无 Token / Token 无效 | 未认证（请求被拒） | ✓ |
| 已认证 | 调用解析操作者，Claim 完整 | 上下文就绪 | ✓ |
| 已认证 | Claim 缺失或格式错误 | — | ✗ → 401 |
| 已认证 | 功能权限不足 | — | ✗ → 403（业务层） |
| 已认证 | 数据范围外（写/读管理范围内资源） | — | ✗ → 404（业务层，部门模块） |

**不允许**
- 认证失败后以匿名身份访问受保护 API
- 用请求参数覆盖 JWT 中的 tenantId / actorId
- Token 过期或撤销后仍访问受保护 API（**待确认**撤销机制）

---

## 6. 权限

### 6.1 谁可以调用受保护 API

| 条件 | 是否可调用 |
| --- | --- |
| 无 JWT / JWT 无效 | ✗ |
| JWT 有效但缺少 tenant_id 或 sub | ✗ |
| JWT 有效且 Claim 完整 | ✓（进入业务层，仍受功能权限与数据范围约束） |
| 访问白名单路径 | ✓（无需 JWT） |

### 6.2 功能权限 vs 数据权限

| 类型 | 载体 | 校验时机 |
| --- | --- | --- |
| 功能权限 | JWT scope → permissions | HTTP 方法 security 表达式 + 业务层二次校验 |
| 组织数据权限（写） | manageable_department_ids + manage:all | 业务层 canManage |
| 组织数据权限（读） | 同上 + 祖先展开 | 业务层 visibleIds（部门模块） |

### 6.3 不同操作者类型的行为差异

| 操作者类型 | 功能权限 | 数据范围 | 读 | 写 | 创建 group 根 |
| --- | --- | --- | --- | --- | --- |
| 租户全量管理员 | 含 manage:all + 各写权限 | 全部部门 | 全部 | 全部 | ✓（+ create） |
| 范围管理员 | 含对应写/读权限 | manageable_department_ids | 范围+祖先 | 仅范围内 | ✗ |
| 只读用户 | 仅 view（+ 可选 member:view） | 有限或空 | 范围+祖先 | ✗ | ✗ |
| 无 Token | — | — | ✗ | ✗ | ✗ |

### 6.4 范围外资源的错误语义（已确认，由业务模块执行）

| 场景 | HTTP | 错误码 | 消息 |
| --- | --- | --- | --- |
| 无功能权限 | 403 | `DEPARTMENT_OPERATION_FORBIDDEN` 等 | 无操作权限 |
| 无数据范围（故意隐藏） | 404 | `DEPARTMENT_NOT_FOUND` 等 | 资源不存在 |
| HTTP 层 @PreAuthorize 失败 | 403 | **待确认** | **待确认** |

---

## 7. 事务与数据一致性

### 7.1 事务

本模块无数据库写入，**不涉及业务事务**。

### 7.2 必须保证的一致性

| 关系 | 必须保证 |
| --- | --- |
| JWT `tenant_id` ↔ 业务查询租户条件 | 业务模块只能访问 token 所属租户数据 |
| JWT scope ↔ 功能权限校验 | 权限码字符串须与业务 API 要求完全一致 |
| JWT `manageable_department_ids` ↔ 写操作目标 | 非全量管理员只能写范围内的部门 |
| 单次请求内操作者上下文 | 同一请求内 tenantId/actorId/permissions 须来自同一 JWT 快照 |

### 7.3 组织 / 权限变更与 Token 一致性（已确认目标；当前代码未对齐）

部门 move/status/delete、用户角色 / 状态 / 部门变化、以及角色权限定义（功能权限 / 数据范围 / 敏感字段）提交后，Redis 租户授权版本递增。

**已确认规则（2026-09-05）**

- 签发 Access Token 时写入 `permission_version`（值 = 当时 Redis 租户授权版本）。
- 进入业务前比对 JWT `permission_version` 与 Redis 当前版本。不一致或 JWT 缺该 Claim → 不进入业务 Controller，HTTP **409**，编码 `PERMISSION_VERSION_CHANGED`。
- 比对豁免：`GET /api/v1/auth/context`、`POST /api/v1/auth/logout`、以及已提供的 `POST /api/v1/auth/refresh`，以便换新快照和新 Token。
- 最大延迟：该用户下一次业务请求。不做推送。

**当前代码行为**：本模块仍不比对授权版本；JWT 内 `scope` / `manageable_department_ids` 在 Token 过期前可能滞后。实现须按上表补齐，不得把现状写成已交付。

### 7.4 并发

- 同一用户并发请求各自独立解析 JWT，无跨请求锁。
- 并发控制（如部门 version 乐观锁）属于业务模块，不在本模块范围。

### 7.5 失败回滚

本模块无持久化副作用，无回滚需求。JWT 解析失败直接拒绝请求，不产生部分状态。

---

## 8. 异常

| 场景 | 系统行为 | 错误码 / 状态 | HTTP |
| --- | --- | --- | --- |
| 无 Bearer Token | 过滤链拒绝，不进入业务，不重定向 `/login` | HTTP 401（`HttpStatusEntryPoint`；响应体格式仍待确认） | 401 |
| JWT 签名/过期无效 | 过滤链拒绝 | **待确认** | 401 |
| 已进业务层但 Security 上下文无 JWT | 抛业务异常 | `AUTHENTICATION_REQUIRED` | 401 |
| tenant_id / sub / manageable_department_ids 格式错误 | 抛业务异常 | `AUTHENTICATION_INVALID` | 401 |
| @PreAuthorize 功能权限不足 | Method Security 拒绝 | **待确认** | 403 |
| JWT `permission_version` 与 Redis 租户授权版本不一致 | 不进入业务 | `PERMISSION_VERSION_CHANGED` | **409** |
| 业务层功能权限不足 | ApiResponse 错误 | `DEPARTMENT_OPERATION_FORBIDDEN` 等 | 403 |
| 业务层数据范围外 | ApiResponse 错误 | `DEPARTMENT_NOT_FOUND` 等 | 404 |

**已确认**：`AUTHENTICATION_*` 异常由全局处理器转为 `ApiResponse` 格式；过滤链/`@PreAuthorize` 失败**可能**不是同一响应格式（**疑似 Bug / 待确认**）。

**客户端消息（已确认）**

| 错误码 | message |
| --- | --- |
| `AUTHENTICATION_REQUIRED` | 请先登录 |
| `AUTHENTICATION_INVALID` | 认证信息不完整 |

---

## 9. 外部依赖

| 依赖 | 用途 | 失败时应如何处理 |
| --- | --- | --- |
| **JWT 解码器（JwtDecoder）** | 验证 Bearer Token | 无法验证 → 拒绝请求；**待确认**降级策略 |
| **JWT 签发服务（登录与会话认证模块）** | 签发含 tenant_id、sub、scope、manageable_department_ids 的 Token | 本模块不签发；签发不可用则用户无法获取有效 Token。契约见 [登录模块总览 Spec](../../modules/auth/LoginController.spec.md) |
| **SLF4J MDC** | 可选 requestId | 缺失时不失败，生成随机 requestId |
| **Spring Security OAuth2 Resource Server** | JWT 过滤链、Authority 转换 | 容器启动失败则应用不可用 |
| **Redis / MQ / 第三方 API** | — | 本模块不依赖 |

---

## 10. 验收标准

### 10.1 正常场景

- [ ] 携带有效 JWT 访问 `/api/v1/system/departments/tree` 且含 view 权限 → 200
- [ ] 无 JWT、无 JwtDecoder 访问 `/v3/api-docs` → 200，不重定向 `/login`
- [ ] 无 JWT、无 JwtDecoder 访问 `/swagger-ui/index.html` → 200
- [ ] 无 JWT 访问 `/actuator/health` → 200
- [ ] 无 JWT 访问受保护 API（如 `/api/v1/system/users`）→ 401，不是 302 `/login`
- [ ] JWT 含 tenant_id、sub、scope、manageable_department_ids → 业务 API 正确限定租户与范围
- [ ] JWT scope 含 `system:department:manage:all` → manageAllDepartments=true，可管理任意部门
- [ ] 成功响应 ApiResponse 含 requestId

### 10.2 参数异常（Token / Claim）

- [ ] JWT 缺少 tenant_id → 401 `AUTHENTICATION_INVALID`
- [ ] JWT sub 非 UUID → 401 `AUTHENTICATION_INVALID`
- [ ] manageable_department_ids 含非法 UUID → 401 `AUTHENTICATION_INVALID`
- [ ] manageable_department_ids 缺失 → 可管理部门为空集，不报错

### 10.3 权限异常

- [ ] 无 JWT 访问受保护 API → 401
- [ ] 有 JWT 但无 view 权限访问部门树 → 403
- [ ] 有 view 但无 write 权限访问写接口 → 403
- [ ] 非全量管理员创建 group 根 → 403
- [ ] 范围外部门写操作 → 404（非 403）

### 10.4 业务异常（认证上下文）

- [ ] 业务层调用解析操作者时 Security 上下文无 JWT → 401 `AUTHENTICATION_REQUIRED`

### 10.5 状态异常

- [ ] 过期 JWT → 401，不可访问受保护 API
- [ ] 认证失败后不得以匿名身份读取业务数据

### 10.6 数据一致性

- [ ] 业务查询使用的 tenantId 与 JWT tenant_id 一致
- [ ] 请求体传入的 tenantId（若存在）不被采用（**待确认**是否有接口误收该字段）
- [ ] 全量管理员 JWT 中 manageable_department_ids 为空仍可管理全部部门

### 10.7 并发场景

- [ ] 同一 Token 并发 10 次合法请求均独立解析，互不干扰
- [ ] 组织变更后旧 Token 内部门范围滞后行为符合产品决策（**待确认**）

### 10.8 响应格式一致性（待确认 / 疑似 Bug）

- [ ] 过滤链 401 与 BusinessException 401 响应格式是否统一
- [ ] @PreAuthorize 403 与 BusinessException 403 响应格式是否统一

---

## 11. 待确认问题

| # | 问题 | 当前代码行为 | 分类 |
| --- | --- | --- | --- |
| 1 | 生产环境是否必须配置 JwtDecoder | 过滤链始终启用；无解码器时白名单可访问，其余 401 | 待确认（生产配置） |
| 2 | JWT 权限写入 scope 的格式（空格分隔 / 数组） | 转换器默认读 scope Claim | 待确认 |
| 3 | 过滤链与 @PreAuthorize 失败的响应格式 | 无自定义 Handler | 待确认 / 疑似 Bug |
| 4 | 登录/签发/刷新 Token 的 API 位置 | 本仓库无认证 Controller | 待确认 |
| 5 | 组织 / 权限变更后 Token 内范围刷新 | 目标：比对 `permission_version`，409；当前代码未比对 | 已确认目标；实现缺口 |
| 6 | manageable_department_ids 为空且无 manage:all 时，仅有 view 能否看到数据 | 可见集合可能为空 | 待确认 |
| 7 | MDC requestId 由哪个组件写入 | 本模块只读 MDC | 待确认 |
| 8 | JWT 是否应含 manageable_department_ids | 代码读取；架构文档称 Token 不含组织树 | 待确认 / 设计分歧 |
| 9 | iss/aud/exp/jti 等标准 Claim 校验规则 | 由 JwtDecoder 配置决定，本模块未定义 | 待确认 |
| 10 | Token 撤销（jti/sid 黑名单）是否启用 | 本模块未实现 | 待确认 |

---

## 附录 A：与架构文档的差异

| 项 | 架构文档 (backend-architecture-v0.2) | 当前实现 | 标记 |
| --- | --- | --- | --- |
| Token 内容 | 不含完整权限/组织树 | 读取 scope + manageable_department_ids | 待确认 / 设计分歧 |
| Refresh Token | Redis 存储 | 本模块不涉及；访问令牌投递已确认为 Bearer（登录总览 §4.3）；刷新 API 与存储介质见登录总览 §18 P0-3 | — |
| 算法 | RS256 | 由 JwtDecoder 配置决定 | 待确认 |

---

## 附录 B：当前代码行为摘要（非目标规则）

以下为实现现状记录，**不应**默认为最终产品规则，除非 §11 待确认项闭合后升格为已确认规则：

1. `SecurityFilterChain` 始终注册；仅 OAuth2 Resource Server / `JwtAuthenticationConverter` 在存在 `JwtDecoder` 时启用。默认表单登录关闭。
2. CSRF 全局关闭。
3. scope Authority 前缀为空；兼容剥 `SCOPE_` 前缀。
4. 双层功能权限校验（HTTP 表达式 + 业务层 hasPermission）。
5. 数据范围外故意返回 404（由业务模块执行）。
6. 认证相关 BusinessException 走 ApiResponse；Security 框架异常可能不走同一格式。

---

## 变更记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| v1.4 | 2026-09-07 | 将 CSRF、HTTP Basic 和表单登录的禁用配置改为显式 lambda，消除 Spring Security `@NonNull` 方法引用警告；安全策略不变 |
| v1.4 | 2026-09-07 | 为 `AuthSessionFilter` 重写的 `OncePerRequestFilter#doFilterInternal` 参数补充 Spring `@NonNull` 注解；不改变认证过滤行为 |
| v1.4 | 2026-09-07 | 修复 Access Token 可管理部门 UUID 转字符串的方法引用 null 类型安全警告；保持 `manageable_department_ids` Claim 内容不变 |
| v1.4 | 2026-09-05 | 登录总览确认访问令牌投递为 `Authorization: Bearer`，本模块资源服务器与此对齐 |
| v1.2 | 2026-09-05 | 文档交叉引用登录与会话认证 Spec：明确本模块仍不签发 Token；公开登录路径尚未进入当前白名单。不改变资源服务器代码行为。 |
| v1.1 | 2026-09-05 | 本地无 JwtDecoder 时文档白名单仍可匿名访问；关闭默认 `/login` 表单，避免 Swagger 被拦。兼容：白名单新增精确路径 `/v3/api-docs`、`/swagger-ui.html`；受保护 API 未认证从 302 变为 401。无需数据库迁移。测试：`PublicApiDocumentationSecurityTest` |
| v1.0 | 2026-09-03 | 基于 shared/security 模块代码考古生成首版业务 Spec |
