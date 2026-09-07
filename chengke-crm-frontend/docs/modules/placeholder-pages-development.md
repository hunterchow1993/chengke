# 当前占位业务页面开发文档

本文记录尚未进入完整业务实现的路由页面。进入开发前，必须建立模块独立开发文档并把登记表指向新文档。

## `DashboardPage`

- 源文件：`src/features/dashboard/pages/dashboard-page.tsx`。
- 路由：`/app/dashboard`。
- 职责：展示当前用户、租户和基础设施就绪状态，不伪造业务统计。
- 状态：读取 Redux 用户和租户；无接口请求。
- 副作用：无。
- 测试：用户/租户文案、缺省值、无伪造统计；接入统计接口时改用 TanStack Query 并补加载/错误/空状态测试。

## `LeadList Component`

- 源文件：`src/features/lead-list/pages/lead-list-page.tsx`。
- 路由：`/app/leads`；权限 `lead:view`。
- 职责：当前只组合 `PermissionRoute` 与 `ModulePlaceholder`。
- 状态/副作用：无业务状态和请求。
- 测试：权限满足/拒绝和占位文案；正式实现前建立线索列表模块文档。

## `LeadPool Component`

- 源文件：`src/features/lead-pool/pages/lead-pool-page.tsx`。
- 路由：`/app/lead-pools`；权限 `lead:pool:view`。
- 职责：当前只展示线索池架构占位。
- 测试：权限与占位文案；正式实现需覆盖领取、分配、回收和成员权限。

## `UsersPage` 与用户路由 `Component`

- 源文件：`src/features/user-management/pages/users-page.tsx`。
- 路由：`/app/system/users`；权限 `system:user:view`。
- 职责：`UsersPage` 展示占位；`Component` 提供路由权限门禁。
- 测试：权限、标题和描述；正式实现前切换到用户管理独立文档。

## 角色管理路由 `Component`

- 源文件：`src/features/user-management/pages/roles-page.tsx`。
- 路由：`/app/system/roles`；权限 `system:role:view`。
- 职责：当前展示角色管理占位。
- 测试：权限和占位文案；正式实现需覆盖功能权限、数据范围和敏感字段授权。

## 旧部门占位路由 `Component`

- 源文件：`src/features/user-management/pages/departments-page.tsx`。
- 当前状态：路由已切换到 `features/department-management`，此文件未被使用，保留仅用于识别待清理代码。
- 职责：旧的部门占位页面，无业务实现。
- 变更规则：确认无外部引用后单独删除，并同步从登记表和本文移除；不得重新接回路由。

## 变更记录

| 版本 | 日期       | 说明                                                     |
| ---- | ---------- | -------------------------------------------------------- |
| v0.1 | 2026-08-10 | 登记当前工作台、线索、线索池、用户、角色和旧部门占位页面 |
