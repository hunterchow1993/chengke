# 部门管理前端开发文档

| 文档信息 | 内容                                                                                                                        |
| -------- | --------------------------------------------------------------------------------------------------------------------------- |
| 模块路径 | `src/features/department-management`                                                                                        |
| 页面路由 | `/app/system/departments`                                                                                                   |
| 文档状态 | 可交互原型当前实现                                                                                                          |
| 更新日期 | 2026-08-10                                                                                                                  |
| 产品需求 | [部门管理 PRD v0.1](../../../../modules/department-management/docs/PRD-department-management-v0.1.md)                       |
| UI 设计  | [部门管理 UI/UX 设计 v0.1](../../../../modules/department-management/docs/UI-design-department-management-v0.1.md)          |
| 后端契约 | [部门管理后端开发文档 v0.1](../../../../chengke-crm-backend/docs/modules/department-management-backend-development-v0.1.md) |

> 当前使用本地模拟组织数据，用于产品和交互评审。尚未接入真实部门 API、TanStack Query、React Hook Form 或 OpenAPI 生成类型。

## 1. 模块职责

负责组织树浏览和搜索、部门详情、新增部门表单、移动影响预览及操作保护说明。前端节点能力只能改善体验，后端仍负责最终权限、租户、版本和引用校验。

## 2. 目录

```text
department-management/
├── components/
│   ├── department-management-prototype.tsx
│   └── department-management-prototype.test.tsx
├── model/
│   └── department-prototype-data.ts
├── pages/
│   └── departments-page.tsx
└── README.md
```

## 3. 路由页面 `Component`

- 源文件：`pages/departments-page.tsx`。
- Props/状态：无。
- 职责：声明 `system:department:view` 权限门禁，并承载 `DepartmentManagementPrototype`。
- 副作用：只读取权限快照；自身不请求接口。
- 测试：无权限显示统一 403；有权限渲染部门页面；路由懒加载成功。

## 4. `DepartmentManagementPrototype`

- 源文件：`components/department-management-prototype.tsx`。
- Props：无。
- 职责：组合组织树、搜索、详情、新增抽屉、更多菜单、移动对话框和成功反馈。
- 本地状态：`tree`、`selectedId`、`expandedIds`、`keyword`、`createOpen`、`moveOpen`、`menuOpen`、`notice`。
- 派生状态：当前部门、搜索结果、后代数和总节点数。
- 副作用：仅修改页面本地状态；新增节点使用 `Date.now()` 生成原型 ID；不请求接口、不写缓存和 Redux。
- 响应式：≥1280px 左树 320px + 右详情；更窄时上下排列；抽屉最大 560px。
- 无障碍：组织树使用 tree/treeitem 语义；图标按钮有可读名称；成功提示使用 `aria-live`；表单错误关联输入框。

### 4.1 内部组件

| 组件                   | Props                                                    | 状态与职责                        | 副作用                       |
| ---------------------- | -------------------------------------------------------- | --------------------------------- | ---------------------------- |
| `StatusBadge`          | `status`                                                 | 以文字、图标和语义色显示正常/停用 | 无                           |
| `DepartmentTreeNode`   | node、level、expandedIds、selectedId、onSelect、onToggle | 递归渲染树节点，分离展开和选择    | 调用父级回调                 |
| `SummaryMetric`        | icon、label、value                                       | 展示详情顶部统计                  | 无                           |
| `DetailItem`           | label、children                                          | 统一详情字段布局                  | 无                           |
| `DepartmentDrawer`     | parent、onCreate、onClose                                | 管理新增表单值和字段错误          | 提交有效名称/编码给父级      |
| `MoveDepartmentDialog` | department、onClose                                      | 展示目标父节点和影响摘要          | 当前确认只关闭原型，不写数据 |

### 4.2 主要方法

| 方法                            | 功能                    | 副作用                            |
| ------------------------------- | ----------------------- | --------------------------------- |
| `toggleExpanded`                | 切换节点展开集合        | 更新本地 `expandedIds`            |
| `selectDepartment`              | 选中节点并清空搜索/菜单 | 更新本地详情上下文                |
| `createPrototypeDepartment`     | 创建本地演示子节点      | 更新 tree、展开态、抽屉和成功提示 |
| `DepartmentDrawer.handleSubmit` | 校验名称、编码并提交    | 更新字段错误或调用 `onCreate`     |

## 5. 原型数据模型

`model/department-prototype-data.ts` 定义 `DepartmentNode`、状态、负责人和搜索结果，并提供：

- `findDepartmentById`
- `countDescendants`
- `getDepartmentPath`
- `searchDepartments`
- `appendDepartmentChild`

接入后端后，原型模型必须替换为 OpenAPI DTO 适配后的页面模型，不能把 API DTO 直接扩展为 UI 状态对象。

## 6. 当前测试

`department-management-prototype.test.tsx` 已覆盖：

- 默认部门、组织影响和完整路径。
- 选择树节点后更新详情。
- 搜索保留祖先上下文。
- 新增抽屉名称校验。
- 新增本地节点与总节点数更新。
- 移动对话框的角色和节点影响摘要。

接入真实接口前还必须新增：

- MSW：树、详情、搜索、负责人候选、创建、编辑、预览、移动、启停和删除。
- TanStack Query：query key、取消旧搜索、精确失效、409 冲突与 422 阻塞。
- 权限：节点 capabilities 和接口 403/404。
- 无障碍：键盘树导航、抽屉/对话框焦点、Escape 和焦点返回。
- Playwright：搜索、新增、移动预览、并发冲突和删除阻塞主链路。

## 7. 接口接入边界

后续依据后端 OpenAPI 建立 `api/`、`queries/`、`schemas/` 和 `types/`：

- 服务端数据只进入 TanStack Query，不复制到 Redux。
- 搜索关键词进入 URL 前必须确认不含敏感数据；当前部门选择可进入 URL。
- 写操作不自动重试；成功后精确失效父节点、详情、搜索和可选树 query。
- 409 版本冲突要求重新加载；422 显示后端稳定阻塞原因并保留表单输入。
- 预览令牌仅随确认请求发送，不写持久化存储。

## 8. 已知限制

- 新增、移动、编辑、启停和删除尚未调用后端。
- 编辑按钮和启停菜单目前仅展示交互入口。
- 树键盘方向键、焦点陷阱和 Escape 关闭尚待正式组件接入。
- 页面仍使用原型示例用户和组织数据。

## 9. 变更记录

| 版本 | 日期       | 说明                                                               |
| ---- | ---------- | ------------------------------------------------------------------ |
| v0.1 | 2026-08-10 | 建立路由页面、主原型、内部组件、方法、模型、测试和后端接入边界文档 |
