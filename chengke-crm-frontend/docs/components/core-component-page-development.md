# 核心组件、路由与布局开发文档

| 文档信息 | 内容                                                                       |
| -------- | -------------------------------------------------------------------------- |
| 文档状态 | 当前实现说明                                                               |
| 更新日期 | 2026-08-10                                                                 |
| 适用范围 | `src/main.tsx`、`src/app`、`src/layouts`、`src/components`                 |
| 架构依据 | [前端架构设计 v0.3](../../../architecture/frontend-architecture-v0.3.md)   |
| 设计依据 | [澄客 CRM Web 设计系统 v1.1](../../../design-system/chengke-crm/MASTER.md) |

## 1. 应用入口与 Provider

### `src/main.tsx`

- 职责：查找 `#root`，在 `StrictMode` 中组合 `AppProviders` 和 `RouterProvider`。
- 输入：无 Props；依赖 HTML 根节点和全局样式。
- 状态：无。
- 副作用：创建 React Root；根节点缺失时抛出启动错误。
- 测试：启动冒烟测试应覆盖根节点存在、Provider/Router 可渲染；生产构建必须通过。

### `AppProviders`

- 源文件：`src/app/providers/app-providers.tsx`。
- 职责：提供 Redux Store 和唯一 TanStack QueryClient。
- Props：`children: ReactNode`，必填。
- 状态：Provider 持有全局会话/权限与服务端缓存；组件自身无局部状态。
- 副作用：QueryClient 会在退出登录时由其他模块清空。
- 测试：验证 children 能访问 Redux 和 QueryClient；不得重复创建客户端实例。

## 2. 路由配置与边界

### `router`

- 源文件：`src/app/router/router.tsx`。
- 职责：声明登录、管理端、权限页面和业务路由；业务页面采用路由级懒加载。
- 状态：无运行时局部状态。
- 副作用：导航和动态模块加载。
- 约束：新页面先完成开发文档与登记，再加入路由；未知路径进入 `NotFoundPage`。
- 测试：覆盖根路径重定向、懒加载路由、错误边界和未知路径。

### `SessionBoundary`

- 源文件：`src/app/router/session-boundary.tsx`。
- Props：`children: ReactNode`。
- 职责：进入 `/app` 前恢复会话，只向已认证用户渲染管理端。
- 状态：读取 Redux `auth.status`；`startedRef` 防止 StrictMode 重复恢复。
- 副作用：请求授权上下文、必要时刷新 Token、更新 Redux、未登录时跳转 `/login`。
- 状态：恢复中显示安全会话加载；失败进入匿名状态。
- 测试：已有认证、恢复成功、401 刷新成功、恢复失败、StrictMode 单次启动和回跳地址。

### `PermissionRoute`

- 源文件：`src/app/router/permission-route.tsx`。
- Props：`children` 必填；`required?: readonly string[]`。
- 职责：根据 Redux 权限快照执行页面级体验门禁。
- 副作用：只读 Redux；不能替代后端鉴权。
- 测试：权限满足、缺失、required 为空和权限变化重渲染。

### `ForbiddenPage`

- 源文件：`src/app/router/forbidden-page.tsx`。
- 职责：展示 403 状态并返回后端授权首页。
- 状态：读取 `permission.authorizedHome`。
- 副作用：点击后站内导航。
- 测试：授权首页链接、无敏感错误信息、标题和可访问按钮名称。

### `NotFoundPage`

- 源文件：`src/app/router/not-found-page.tsx`。
- 职责：展示站内 404 并返回工作台。
- 状态/Props：无。
- 副作用：站内导航。
- 测试：404 文案、返回链接和键盘可操作。

### `RouteErrorPage`

- 源文件：`src/app/router/route-error-page.tsx`。
- 职责：隔离路由 loader/渲染错误，隐藏堆栈并提供重新加载。
- 状态：读取 React Router 错误对象并派生 HTTP 状态。
- 副作用：点击重试调用 `window.location.reload()`。
- 测试：RouteErrorResponse、未知异常默认 500、重新加载和不泄露内部错误。

## 3. 管理端布局

### `AdminLayout` 与 `MenuNode`

- 源文件：`src/layouts/admin-layout.tsx`。
- 产品契约：[管理端整体布局 Spec](../layouts/admin-layout.spec.md)。
- 职责：渲染租户、用户、后端菜单树、侧栏折叠、退出入口和业务 `Outlet`。
- Props：无；`MenuNode` 接收 `item`、`collapsed`、`level`。
- 状态：读取 auth、permission、preference Redux 状态。
- 副作用：折叠侧栏写 Redux；退出调用后端，清 Token、Query 缓存和 Redux，并跳转登录页。
- 安全：只允许 routeKey 白名单生成链接，未知键不渲染任意 URL。
- 响应式：布局使用 `100dvh`；侧栏宽 240px/72px；Header 高 68px。
- 测试：菜单白名单、选中态、目录节点、折叠、空菜单、退出成功/失败都完成本地清理。

## 4. 共享组件

### `ModulePlaceholder`

- 源文件：`src/components/shared/module-placeholder.tsx`。
- Props：`title`、`description`，均必填。
- 职责：为尚未按 PRD 实现的路由说明当前架构状态和下一步。
- 状态/副作用：无。
- 测试：标题、描述和三项基础能力渲染；正式页面完成后应删除对应占位使用。

### `PageHeader`

- 源文件：`src/components/shared/page-header.tsx`。
- Props：`title`、`description` 必填；`eyebrow?`、`actions?` 可选。
- 职责：统一页面标题、说明和右侧操作区。
- 状态/副作用：无。
- 无障碍：页面不得因组合该组件产生重复可见 H1；actions 保持自然焦点顺序。
- 测试：可选区域、长文案和操作内容。

### `PermissionGate`

- 源文件：`src/components/shared/permission-gate.tsx`。
- Props：`children`、`granted` 必填；`required?`、`fallback?` 可选。
- 职责：控制局部操作展示，不承担接口鉴权。
- 状态/副作用：无。
- 测试：已有测试覆盖全部权限、权限缺失、fallback 与空 required。

## 5. 基础 UI

### `Button`

- 源文件：`src/components/ui/button.tsx`。
- Props：原生 Button 属性、`variant`、`size`、`asChild`。
- 职责：提供 primary/secondary/ghost/danger 和 default/compact/icon 规格。
- 状态：无；Radix Slot 支持组合为 Link 等语义元素。
- 无障碍：图标按钮由调用方提供 `aria-label`；disabled 不可交互；保留焦点环。
- 测试：各 variant/size、disabled、asChild、点击与键盘。

### `Input`

- 源文件：`src/components/ui/input.tsx`。
- Props：全部原生 input 属性。
- 职责：统一后台输入框尺寸、边框、焦点和禁用状态。
- 状态/副作用：无。
- 无障碍：调用方必须提供关联 Label；错误通过 `aria-invalid` 和描述元素表达。
- 测试：属性透传、受控值、disabled、focus 和 aria 属性。

## 6. 变更记录

| 版本 | 日期       | 说明                                                                           |
| ---- | ---------- | ------------------------------------------------------------------------------ |
| v0.2 | 2026-09-05 | 管理端布局补回产品 Spec，见 `docs/layouts/admin-layout.spec.md` |
| v0.1 | 2026-08-10 | 为应用入口、Provider、路由边界、管理端布局、共享组件和基础 UI 建立当前实现记录 |
