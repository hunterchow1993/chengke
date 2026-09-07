# 组件与页面登记表

每个非测试 `.tsx` 源文件都必须登记。开发文档可以覆盖多个相关组件，但必须为每个导出组件或页面提供独立记录。

| 源文件                                                                              | 类型           | 对应开发文档                                                            | 当前状态   |
| ----------------------------------------------------------------------------------- | -------------- | ----------------------------------------------------------------------- | ---------- |
| `src/main.tsx`                                                                      | 应用入口       | [核心组件、路由与布局](./components/core-component-page-development.md) | 当前实现   |
| `src/app/providers/app-providers.tsx`                                               | Provider       | [核心组件、路由与布局](./components/core-component-page-development.md) | 当前实现   |
| `src/app/router/forbidden-page.tsx`                                                 | 状态页面       | [核心组件、路由与布局](./components/core-component-page-development.md) | 当前实现   |
| `src/app/router/not-found-page.tsx`                                                 | 状态页面       | [核心组件、路由与布局](./components/core-component-page-development.md) | 当前实现   |
| `src/app/router/permission-route.tsx`                                               | 路由边界       | [核心组件、路由与布局](./components/core-component-page-development.md) | 当前实现   |
| `src/app/router/route-error-page.tsx`                                               | 错误页面       | [核心组件、路由与布局](./components/core-component-page-development.md) | 当前实现   |
| `src/app/router/router.tsx`                                                         | 路由配置       | [核心组件、路由与布局](./components/core-component-page-development.md) | 当前实现   |
| `src/app/router/session-boundary.tsx`                                               | 会话边界       | [核心组件、路由与布局](./components/core-component-page-development.md) | 当前实现   |
| `src/layouts/admin-layout.tsx`                                                      | 管理端布局     | [管理端整体布局 Spec](./layouts/admin-layout.spec.md)；[核心组件、路由与布局](./components/core-component-page-development.md) | 当前实现   |
| `src/components/shared/module-placeholder.tsx`                                      | 共享组件       | [核心组件、路由与布局](./components/core-component-page-development.md) | 当前实现   |
| `src/components/shared/page-header.tsx`                                             | 共享组件       | [核心组件、路由与布局](./components/core-component-page-development.md) | 当前实现   |
| `src/components/shared/permission-gate.tsx`                                         | 权限组件       | [核心组件、路由与布局](./components/core-component-page-development.md) | 当前实现   |
| `src/components/ui/button.tsx`                                                      | 基础 UI        | [核心组件、路由与布局](./components/core-component-page-development.md) | 当前实现   |
| `src/components/ui/input.tsx`                                                       | 基础 UI        | [核心组件、路由与布局](./components/core-component-page-development.md) | 当前实现   |
| `src/features/auth/components/login-form.tsx`                                       | 业务组件       | [`LoginForm` 开发文档](./pages/login-form-development.md)               | 当前实现   |
| `src/features/auth/pages/login-page.tsx`                                            | 路由页面       | [`LoginPage` 开发文档](./pages/login-page-development.md)               | 当前实现   |
| `src/features/dashboard/pages/dashboard-page.tsx`                                   | 路由页面       | [占位业务页面](./modules/placeholder-pages-development.md)              | 架构占位   |
| `src/features/department-management/components/department-management-prototype.tsx` | 业务组件       | [部门管理前端开发文档](../src/features/department-management/README.md) | 可交互原型 |
| `src/features/department-management/pages/departments-page.tsx`                     | 路由页面       | [部门管理前端开发文档](../src/features/department-management/README.md) | 可交互原型 |
| `src/features/lead-list/pages/lead-list-page.tsx`                                   | 路由页面       | [占位业务页面](./modules/placeholder-pages-development.md)              | 架构占位   |
| `src/features/lead-pool/pages/lead-pool-page.tsx`                                   | 路由页面       | [占位业务页面](./modules/placeholder-pages-development.md)              | 架构占位   |
| `src/features/user-management/pages/departments-page.tsx`                           | 未使用旧占位页 | [占位业务页面](./modules/placeholder-pages-development.md)              | 待清理     |
| `src/features/user-management/pages/roles-page.tsx`                                 | 路由页面       | [占位业务页面](./modules/placeholder-pages-development.md)              | 架构占位   |
| `src/features/user-management/pages/users-page.tsx`                                 | 路由页面       | [占位业务页面](./modules/placeholder-pages-development.md)              | 架构占位   |

测试文件不单独登记：

- `*.test.tsx` 的用例必须记录在对应组件开发文档。
- Playwright 场景记录在对应路由页面开发文档。
- Storybook 或视觉回归文件未来启用后同样归属于对应组件记录。
