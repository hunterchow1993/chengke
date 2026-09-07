# 澄客 CRM 前端架构设计

| 文档信息 | 内容 |
| --- | --- |
| 文档版本 | v0.3 |
| 文档状态 | 架构评审稿 |
| 更新日期 | 2026-08-04 |
| 适用范围 | 澄客 CRM Web 管理端 |
| 主要读者 | 前端、后端、测试、技术负责人 |
| 前端形态 | React 单页应用（SPA） |
| 后端依赖 | Spring Boot REST API |
| 后端架构 | [后端架构设计 v0.2](./backend-architecture-v0.2.md) |
| 设计系统 | [澄客 CRM Web 设计系统 v1.1](../design-system/chengke-crm/MASTER.md) |

## 1. 架构结论

澄客 CRM 前端采用“按业务特性分层的模块化单体 SPA”，不在第一期引入微前端。推荐基线如下：

| 分类 | 选型 | 用途 |
| --- | --- | --- |
| 语言 | TypeScript，开启 `strict` | 静态类型和重构保障 |
| UI 框架 | React 19 | 页面和组件运行时 |
| 构建工具 | Vite | 本地开发、构建和按需分包 |
| UI 基础 | shadcn/ui | 可控源码组件和无障碍基础 |
| 样式 | Tailwind CSS 4 | 原子样式和设计令牌 |
| 路由 | React Router 7 Data Mode | 路由、懒加载、守卫和错误边界 |
| 服务端状态 | TanStack Query 5 | 请求缓存、失效、重试和并发请求 |
| 客户端全局状态 | Redux Toolkit + React Redux | 会话快照、授权上下文和跨页面 UI 状态 |
| 表单 | React Hook Form | 高性能表单状态和字段控制 |
| 校验 | Zod 4 | 表单、路由参数和关键接口边界校验 |
| HTTP | Axios 单实例封装 | 请求头、CSRF、错误和取消处理 |
| 数据表格 | TanStack Table | CRM 复杂表格、列定义和选择状态 |
| 图标 | Lucide React | 与 shadcn/ui 一致的线性图标体系 |
| 单元/组件测试 | Vitest + Testing Library + MSW | 逻辑、组件和接口模拟 |
| 端到端测试 | Playwright | 登录、权限和核心业务链路 |
| 工程质量 | ESLint + Prettier + lint-staged | 静态检查和提交前校验 |

名称修正：原需求中的 `tank-query` 应为 `TanStack Query`；Redux 不重复安装和直接使用原始 API，统一通过 `@reduxjs/toolkit` 与 `react-redux` 接入。

## 2. 关键设计原则

1. 业务模块优先：登录、用户管理、线索列表、线索池分别形成独立 feature。
2. 状态归属唯一：同一份远程数据不能同时存入 TanStack Query 与 Redux。
3. 权限后端权威：菜单和按钮由后端授权上下文返回，所有接口仍由后端最终鉴权。
4. 接口契约优先：OpenAPI 是前后端字段、枚举和错误码的单一契约来源。
5. 组件源码可控：shadcn/ui 组件进入仓库后视为项目源码，二次封装遵循统一设计令牌。
6. 运行时边界校验：TypeScript 只保证编译期，外部输入仍使用 Zod 校验。
7. 默认可测试：业务逻辑与 UI 分离，请求通过 MSW 模拟，关键权限链路用 E2E 覆盖。
8. 可观测且可审计：请求携带追踪 ID，错误信息可定位但不得泄露联系方式等敏感数据。

## 3. 状态管理边界

### 3.1 状态分类

| 状态类型 | 存放位置 | 示例 |
| --- | --- | --- |
| 服务端业务数据 | TanStack Query | 用户列表、线索详情、字典、分页结果 |
| 会话与授权快照 | Redux Toolkit | 当前用户、菜单树、权限码、租户摘要 |
| 跨页面 UI 状态 | Redux Toolkit | 侧栏收起、主题、密度偏好 |
| 可分享查询状态 | URL Search Params | 页码、排序、工作视图、非敏感筛选 |
| 敏感查询状态 | 页面内存 | 手机号、邮箱等筛选值 |
| 表单状态 | React Hook Form | 新建用户、编辑线索、添加跟进 |
| 局部短暂状态 | React state | 弹窗开关、悬停、当前页签 |

### 3.2 TanStack Query 规范

- 所有业务请求通过 feature 内的 query/mutation hooks 使用。
- query key 使用集中式工厂，包含模块、资源、ID 和规范化筛选条件。
- 列表、详情和统计使用不同 key，不通过手写字符串随意拼接。
- 写操作成功后优先精确更新或失效相关 key，不执行全局缓存清空。
- 分页切换保留上一页数据，避免表格闪烁；权限变化时清空全部业务缓存。
- 默认重试只用于幂等查询；`401`、`403`、业务校验错误和写操作不自动重试。
- 列表建议 `staleTime` 30 秒，字典 30 分钟，授权上下文不进入 Query 缓存。

### 3.3 Redux Toolkit 规范

Redux 只保留少量真正跨页面且需同步读取的状态：

```text
store
├── auth        当前会话、租户和用户摘要
├── permission  菜单树、功能权限码、数据范围摘要
└── preference  侧栏、主题和页面密度偏好
```

- 不把用户列表、线索列表、请求加载状态或表单输入放入 Redux。
- 不同时使用 RTK Query 和 TanStack Query；本项目选择 TanStack Query 作为唯一服务端缓存。
- slice 只能暴露业务动作和选择器，组件不得依赖内部 state 结构。
- 开发环境启用 Redux DevTools，生产环境不记录敏感 payload。

## 4. 路由与权限架构

### 4.1 路由模式

采用 React Router Data Mode 和 `createBrowserRouter`，保留 Vite SPA 的部署控制能力。业务页面使用路由级懒加载，每个顶级路由配置错误边界。

建议路由树：

```text
/
├── /login
└── /app
    ├── /dashboard
    ├── /system/users
    ├── /system/departments
    ├── /system/roles
    ├── /leads
    └── /lead-pools
```

- loader 只处理启动依赖、路由参数校验和必要预取。
- 业务数据仍通过 TanStack Query 管理；loader 可调用 `ensureQueryData`，不得另建一套请求缓存。
- 页面标题、面包屑和所需权限写在路由元数据中。
- 未匹配页面进入 404；路由渲染错误进入模块级错误页。

### 4.2 启动与登录流程

```text
加载应用
  ↓
GET /api/v1/auth/context
  ├── 401 -> 清空缓存 -> /login
  └── 200 -> 初始化 auth + permission
                 ↓
          按后端菜单渲染侧栏
                 ↓
          校验目标路由 permission
            ├── 允许 -> 渲染页面
            └── 拒绝 -> 后端指定首页或 403
```

后端授权上下文至少返回：

- 当前用户与租户摘要
- 菜单树和菜单路由键
- 功能权限码集合
- 数据权限摘要
- 敏感字段权限集合
- 登录过期时间和后端指定首页

前端路由守卫只改善体验，接口必须由 Spring Security 和业务服务再次校验。用户权限变化后，后端可通过响应码要求前端重新加载授权上下文。

## 5. 目录结构

```text
src/
├── app/
│   ├── providers/          React、Query、Redux、Router 提供器
│   ├── router/             路由定义、守卫和错误边界
│   ├── store/              Redux store 与根选择器
│   ├── styles/             Tailwind 入口和设计令牌
│   └── config/             运行环境和功能开关
├── components/
│   ├── ui/                 shadcn/ui 源码组件
│   └── shared/             业务无关的复合组件
├── features/
│   ├── auth/
│   ├── user-management/
│   ├── lead-list/
│   └── lead-pool/
├── layouts/                登录布局、管理端布局
├── lib/
│   ├── http/               Axios 实例、拦截器和错误映射
│   ├── query/              QueryClient 和 key 工厂基础能力
│   ├── validation/         通用 Zod schema
│   └── utils/              无业务依赖工具
├── generated/              OpenAPI 生成类型，不手工修改
├── test/                   MSW handlers、fixtures 和测试工具
├── main.tsx
└── vite-env.d.ts
```

每个 feature 内部结构：

```text
features/lead-list/
├── api/                    请求函数、query keys、hooks
├── components/             模块私有组件
├── pages/                  路由页面
├── schemas/                表单与参数 Zod schema
├── types/                  模块领域类型
├── utils/                  模块纯函数
└── index.ts                对外公开入口
```

约束：

- feature 之间不能导入对方内部路径，只能使用公开入口。
- `components/ui` 不包含业务字段、权限判断或接口请求。
- 页面负责组合，不承载大段可复用业务逻辑。
- 公共组件只有被至少两个模块稳定复用后才上移，避免提前抽象。

## 6. 组件与样式体系

### 6.1 shadcn/ui 使用方式

- shadcn/ui 是源码组件集合，不是不可修改的黑盒依赖。
- 原始基础组件放在 `components/ui`，业务组合组件放在 `components/shared` 或 feature 中。
- 修改基础组件时保留键盘操作、焦点管理和 ARIA 行为。
- Drawer、Dialog、DropdownMenu、Table、Form 等统一从项目封装入口引用。
- 图标统一使用 Lucide React，不混用多套图标库或 Emoji 业务图标。

### 6.2 Tailwind 与设计令牌

Tailwind CSS 4 通过 Vite 插件接入。颜色、字体、间距、圆角、阴影、Icon 和通用组件规格统一以[澄客 CRM Web 设计系统](../design-system/chengke-crm/MASTER.md)为唯一来源，业务组件和页面文档不得建立第二套视觉 Token。

- 禁止通过运行时字符串拼接 Tailwind class，动态样式使用枚举映射。
- 组件 class 合并统一使用项目 `cn` 工具。
- 响应式基准以 1440px 后台为主，最低支持 1280px。
- 状态必须同时有文字或图标，不能只依赖颜色。
- 图标统一通过 Lucide 注册表出口使用，后端 `iconKey` 只能映射本地白名单。

## 7. 表单与 Zod 校验

React Hook Form 管理交互状态，Zod 管理结构和业务前置校验。

建议分层：

```text
后端 DTO 类型 -> API response schema（关键边界）
表单 schema    -> 输入与条件必填规则
transform      -> Form Values 转 Request DTO
```

- `tsconfig` 必须开启 `strict`。
- 表单 schema 不直接等同后端 DTO，日期、空字符串和级联选择需显式转换。
- 枚举值来自 OpenAPI 或后端字典，展示名称与提交值分离。
- 后端校验错误映射到具体字段；无法映射的错误显示在表单顶部。
- Zod 仅提供用户即时反馈，后端 Bean Validation 仍为最终约束。
- 离开有未保存修改的抽屉或页面时需要二次确认。

## 8. HTTP 与接口契约

### 8.1 请求约定

- API 前缀统一为 `/api/v1`。
- Axios 只创建一个基础实例，默认发送 `credentials`/Cookie 和 CSRF 请求头。
- 请求超时建议 15 秒；导入、导出和上传使用独立超时策略。
- 每个请求携带前端生成或后端下发的 `X-Request-Id`，便于日志关联。
- 取消筛选请求时使用 `AbortSignal`，避免旧响应覆盖新条件。
- 任何日志、URL 和埋点不得记录完整手机号、微信、QQ、邮箱或跟进内容。

### 8.2 响应约定

建议成功响应：

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "requestId": "01J...",
  "timestamp": "2026-08-04T10:00:00+08:00"
}
```

建议分页响应的 `data`：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

建议错误响应：

```json
{
  "code": "LEAD_VERSION_CONFLICT",
  "message": "数据已被其他用户更新",
  "fieldErrors": [],
  "requestId": "01J...",
  "timestamp": "2026-08-04T10:00:00+08:00"
}
```

- HTTP 状态表达协议结果，业务 `code` 表达稳定业务原因。
- 前端不得依赖中文 `message` 判断逻辑。
- 日期时间统一使用带时区 ISO 8601 字符串；数据库按 UTC 存储，展示按用户时区。
- 金额通过字符串或最小货币单位传输，禁止使用浮点数处理财务值。

### 8.3 OpenAPI

- 后端使用 springdoc-openapi 生成契约。
- CI 导出并校验 OpenAPI 文档，前端生成 `generated` 类型或 API client。
- 生成目录不可手改；业务层通过适配函数把 DTO 转为页面模型。
- 字段删除、改名、枚举收窄和必填变化视为破坏性变更。
- PR 合并前运行接口契约差异检查。

## 9. 前端认证与会话处理

具体 JWT 库、签名、Refresh Token、Redis 撤销和后端安全规则见 [后端架构设计 v0.2](./backend-architecture-v0.2.md)。本节只规定浏览器端行为。

### 9.1 Token 存储

- Access Token 保存在 JavaScript 内存，并写入当前标签页 `sessionStorage`，以便硬刷新后继续以 `Authorization: Bearer` 访问。不写入 localStorage、URL 或 IndexedDB。
- 本期不使用 Cookie 投递访问令牌或刷新凭证；换票时携带当前访问令牌。
- 菜单、权限码和数据权限通过 `/api/v1/auth/context` 获取，不能通过解码 JWT 自行推断。
- 退出登录时清空 TanStack Query、Redux、本标签页访问令牌和页面内敏感状态。

### 9.2 刷新流程

```text
API 返回 401 TOKEN_EXPIRED
        ↓
进入全局 single-flight 刷新锁
        ↓
POST /api/v1/auth/refresh（携带 HttpOnly Refresh Cookie + CSRF）
  ├── 成功 -> 更新内存 Access Token -> 原请求最多重放一次
  └── 失败 -> 清空 Query/Redux -> 跳转登录页
```

- 同一时刻只允许一个刷新请求，其余失败请求等待同一个 Promise，避免刷新风暴。
- 登录、刷新、退出和修改密码接口需要速率限制及安全审计。
- 写请求只有在请求体可安全重放时才自动重试；文件上传和非幂等业务动作不自动重放。

### 9.3 CSRF 与 Cookie

无论采用 Session Cookie 还是 Refresh Cookie，Cookie 参与认证的接口都必须评估 CSRF：

| 项目 | 规则 |
| --- | --- |
| Cookie | `HttpOnly`、`Secure`，限制 Path 和 Domain |
| SameSite | 同域优先 `Lax`，跨站必须单独安全评审 |
| CSRF | 后端生成 Token，前端通过自定义请求头回传 |
| CORS | 明确白名单，不允许携带凭证时使用 `*` |
| 退出登录 | 撤销 Session 或 Token Family，前端清空 Query 与 Redux |
| 会话过期 | 统一处理 401，保存安全的站内回跳地址 |
| 权限不足 | 403，不自动跳登录；展示无权限或返回授权首页 |

### 9.4 多标签页和路由

- 使用 BroadcastChannel 同步退出和刷新失败，不广播 Token 明文。
- 401 刷新失败后保存经过白名单校验的站内回跳地址并进入登录页。
- 403 不触发刷新或跳转登录，应显示无权限或返回后端指定首页。
- 用户权限变化后重新拉取授权上下文，并清除已经无权访问的业务缓存。

## 10. 错误处理与用户反馈

错误分四层处理：

| 层级 | 处理方式 |
| --- | --- |
| 网络层 | 超时、断网、网关错误，显示可重试反馈 |
| 协议层 | 401、403、404、409、422、429、5xx 统一映射 |
| 业务层 | 根据稳定错误码展示可操作提示 |
| 渲染层 | 路由 Error Boundary 隔离模块错误 |

- 401 清理状态并跳转登录；403 不循环重试。
- 409 用于版本冲突或领取冲突，刷新对应 Query 而非整页刷新。
- 表单失败保留用户输入。
- 批量操作支持部分成功，并返回成功数、失败数和脱敏失败清单。
- Toast 只用于短反馈；需要用户决策或包含失败明细时使用 Dialog/页面提示。

## 11. 性能策略

- 路由级动态导入，登录页与管理端业务包分离。
- 大型表格优先服务端分页；超过实际性能阈值后再引入虚拟列表。
- 字典和低频配置使用较长 `staleTime`，高频列表保持短缓存。
- 搜索输入防抖 300ms；手机号等精确筛选由用户主动提交。
- 详情基础信息与时间线并行请求。
- Vite 构建输出进行 chunk 大小检查，不手工做无依据的过度拆包。
- 图片和插图使用 WebP/AVIF 候选格式并设置明确尺寸，避免布局偏移。

性能目标：常规网络下核心列表首屏 2 秒内可用；路由切换有即时反馈；领取等高频动作 1 秒内获得明确成功或冲突结果。

## 12. 测试策略

### 12.1 测试分层

| 层级 | 工具 | 覆盖重点 |
| --- | --- | --- |
| 单元测试 | Vitest | schema、转换、权限判断、query key |
| 组件测试 | Testing Library | 表单、表格、抽屉和无障碍行为 |
| 接口模拟 | MSW | 成功、空数据、权限、冲突和失败 |
| 端到端 | Playwright | 登录、菜单权限、用户管理、线索领取与转化 |

### 12.2 必测链路

- 登录成功、失败、会话过期和退出。
- 后端菜单变化后前端不展示无权限入口。
- 修改 URL 不能访问无权限页面或数据。
- 用户列表的组织数据范围。
- 线索池多人领取冲突。
- 表单条件必填、重复校验、版本冲突和输入保留。
- 敏感字段脱敏及手机号不进入 URL。
- 批量操作部分成功。

## 13. 工程质量与环境

### 13.1 推荐工具

- 包管理器：pnpm，提交唯一 lockfile。
- Node.js：使用当前维护中的 LTS 版本，由 `.nvmrc` 或 Volta 固定。
- 环境变量：只允许 `VITE_` 前缀的公开配置，密钥不得进入前端环境变量。
- Git hooks：lint-staged 只检查本次修改文件；完整测试由 CI 执行。
- 提交检查：类型检查、ESLint、单测、构建和 OpenAPI 差异检查。

### 13.2 环境划分

| 环境 | 用途 |
| --- | --- |
| local | 本地开发，可使用 MSW 或开发后端 |
| test | 集成测试和产品验收 |
| staging | 接近生产的发布验证 |
| production | 正式环境 |

前端构建产物应保持环境无关；优先由部署层注入公开运行配置，避免为每个环境重新构建不同代码。

## 14. 前后端协作边界

- 后端通过 OpenAPI 提供 DTO、枚举、分页和错误码契约。
- 前端只依赖生成类型和公开 API，不依赖后端 DO、表结构或内部包名。
- 菜单、功能权限、数据范围和敏感字段权限由后端授权上下文返回。
- 前端隐藏操作不替代后端鉴权；后端错误必须使用稳定 code。
- 日期、金额、长整型 ID 和空值语义在 OpenAPI 中明确。
- 后端架构、数据库、缓存、JWT 签名和部署规范统一维护在 [后端架构设计 v0.2](./backend-architecture-v0.2.md)。

## 15. 版本与兼容策略

- 脚手架建立时锁定确切版本并提交 lockfile/BOM，本文只规定大版本和兼容边界。
- 前端依赖按月评估、小版本自动化测试、大版本单独 RFC。
- OpenAPI 破坏性变更必须提供版本迁移方案。

建议首期组合：React 19、TypeScript 5.5+、Vite、React Router 7、TanStack Query 5、Zod 4、Tailwind CSS 4。

## 16. 架构验收清单

- [ ] TypeScript 开启 `strict`，构建无类型错误。
- [ ] TanStack Query 与 Redux 状态边界符合本文，不引入 RTK Query 双缓存。
- [ ] 路由级懒加载、错误边界和权限元数据可用。
- [ ] 菜单、按钮权限和授权首页来自后端授权上下文。
- [ ] shadcn/ui、Tailwind 令牌和 Lucide 图标使用统一入口。
- [ ] 表单使用 React Hook Form + Zod，并正确展示后端字段错误。
- [ ] OpenAPI 能生成前端类型，CI 能发现破坏性变更。
- [ ] Cookie、CSRF、401、403、409 和退出登录行为有集成测试。
- [ ] Access Token 仅在内存保存，Refresh Token 对前端不可读。
- [ ] 刷新请求使用 single-flight，失败后安全退出且不重复非幂等请求。
- [ ] 敏感字段不进入 URL、localStorage、日志和埋点。
- [ ] 核心业务链路具备组件测试和 Playwright E2E。
- [ ] 生产构建、健康检查、requestId 和错误追踪可验证。

## 17. 待确认项

1. 前后端是否同域部署，以确定 Cookie、CORS 和 CSRF 的浏览器配置。
2. 是否需要文件上传直传对象存储，以确定上传组件和进度反馈方案。
3. 是否在第一期支持主题切换和页面密度偏好。
4. OpenAPI 使用只生成类型还是同时生成请求函数。

## 18. 官方参考

- [React 19](https://react.dev/blog/2024/12/05/react-19)
- [React Router 模式选择](https://reactrouter.com/start/modes)
- [TanStack Query React v5](https://tanstack.com/query/v5/docs/framework/react)
- [Redux Toolkit 使用指南](https://redux-toolkit.js.org/usage/usage-guide/)
- [shadcn/ui Vite 安装](https://ui.shadcn.com/docs/installation/vite)
- [shadcn/ui React Hook Form](https://ui.shadcn.com/docs/forms/react-hook-form)
- [Tailwind CSS Vite 安装](https://tailwindcss.com/docs/installation/using-vite)
- [Zod 文档](https://zod.dev/)

## 19. 变更记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| v0.3 | 2026-08-04 | 将后端技术选型、JWT 签发、数据库和部署内容迁移至独立后端架构文档，保留前端认证消费与协作边界 |
| v0.2 | 2026-08-04 | 增加 JWT 库选型、RS256、Token Claims、Refresh Token Rotation、Redis 撤销、前端刷新和 CSRF 规范 |
| v0.1 | 2026-08-04 | 建立前端技术栈、目录、状态、路由权限、接口契约、认证、测试和后端依赖基线 |
