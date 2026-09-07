# 澄客 CRM 前端

基于 [前端架构设计 v0.3](../architecture/frontend-architecture-v0.3.md) 搭建的 React 管理端 SPA。

全局颜色、字体、间距、Icon 和组件规格以[澄客 CRM Web 设计系统 v1.1](../design-system/chengke-crm/MASTER.md)为唯一来源。

## 技术基线

- React 19 + TypeScript 5（strict）+ Vite 7
- React Router 7 Data Mode
- TanStack Query 5 + Redux Toolkit
- shadcn/ui 源码模式 + Tailwind CSS 4
- React Hook Form + Zod 4
- Axios、TanStack Table、Lucide React
- Vitest、Testing Library、MSW、Playwright

## 本地运行

要求 Node.js 22 和 pnpm 10：

```bash
pnpm install
pnpm dev
```

开发服务器监听 `http://localhost:5173`，并将 `/api` 代理到 `http://localhost:8080`。

## 质量检查

```bash
pnpm typecheck
pnpm lint
pnpm test
pnpm build
pnpm exec playwright install chromium
pnpm test:e2e
```

## 当前实现范围

- 登录页、表单校验和后端认证接口入口
- Access Token 仅存内存，Refresh Token 由 HttpOnly Cookie 管理
- 401 后 single-flight 刷新与授权上下文恢复
- Redux 会话、权限和偏好三类状态域
- TanStack Query 默认缓存与重试策略
- 后端菜单树渲染、routeKey 白名单和路由权限守卫
- 工作台及用户、部门、角色、线索列表、线索池懒加载路由
- 统一设计令牌、基础 Button/Input、错误页、403 和 404

用户管理和线索管理页面暂为架构占位，后续需要分别依据对应 PRD 按 TDD 实现完整交互。

## 模块维护文档

- [前端页面与组件文档规范](./docs/README.md)：文档先行门禁、登记规则、模板和测试要求。
- [组件与页面登记表](./docs/component-page-registry.md)：每个 `.tsx` 文件与对应开发文档的映射。
- [登录与认证模块维护手册](./src/features/auth/README.md)：认证 API、登录流程、会话恢复、组件方法、Redux 状态、测试和当前维护风险。
- [部门管理前端开发文档](./src/features/department-management/README.md)：路由页面、原型组件、方法、模型、测试和接口接入边界。

新增或修改页面和组件时，必须先更新登记表与对应开发文档，再编写失败测试和实现代码。

## 后端菜单 routeKey

前端只接受以下白名单键，未知键不会生成链接：

| routeKey             | 前端路径                  |
| -------------------- | ------------------------- |
| `dashboard`          | `/app/dashboard`          |
| `system.users`       | `/app/system/users`       |
| `system.departments` | `/app/system/departments` |
| `system.roles`       | `/app/system/roles`       |
| `leads`              | `/app/leads`              |
| `lead-pools`         | `/app/lead-pools`         |

菜单是否出现由后端授权上下文决定，前端隐藏不能替代 Spring Security 和数据权限校验。
