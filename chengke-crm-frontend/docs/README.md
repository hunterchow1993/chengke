# 澄客 CRM 前端开发文档规范

本目录维护页面与组件开发契约。每个页面和组件都必须在[组件与页面登记表](./component-page-registry.md)中登记，并映射到包含专属记录的开发文档。

## 文档先行门禁

新增或修改 `.tsx` 页面、组件、布局、路由边界或 Provider 时，必须按以下顺序执行：

1. 在登记表找到源文件对应的开发文档；新文件先登记并创建文档记录。
2. 先更新文档中的职责、Props、Emits/回调、Slots/children、状态、副作用、接口、权限、交互、无障碍、响应式和测试用例。
3. 在文档变更记录中说明修改原因、行为变化和兼容性。
4. 先新增或修改 Vitest/Testing Library/MSW/Playwright 测试，并确认旧实现按预期失败。
5. 修改组件或页面实现，使测试通过。
6. 运行格式、Lint、类型检查、单元/组件测试、生产构建；关键链路还要运行 Playwright 和浏览器视觉检查。
7. 回读实现和文档，解决全部命名、Props、状态、副作用、接口与测试差异后再提交。

禁止先改实现、后补文档。禁止新建未登记的 `.tsx` 文件。测试文件自身不单独登记，但必须写入被测页面或组件的开发文档。

## 文档组织方式

每个源文件必须有一条登记记录，但不强制每个组件单独创建 Markdown 文件：

- feature 页面和私有组件优先记录在 feature 的 `README.md`。
- 通用 UI、共享组件、布局和路由边界记录在 `docs/components/`。
- 简单占位页面可以集中记录，但每个页面必须有独立小节。
- 一个 `.tsx` 文件导出多个组件时，文档必须逐个说明，不能只记录文件名。

新文档从[组件与页面开发文档模板](./templates/component-page-development-template.md)开始。

## 每条组件记录的必填内容

- 名称、源文件、类型、当前状态和业务职责。
- Props、回调、children/slots 与默认值。
- 内部状态、派生状态和状态归属理由。
- 接口请求、路由、缓存、Redux、Token、浏览器 API 等副作用。
- 权限与租户边界，明确前端控制不能替代后端鉴权。
- 加载、空、错误、无权限、禁用和成功反馈。
- 键盘、焦点、ARIA、对比度和减少动态效果。
- 响应式行为和关键断点。
- 单元、组件、MSW 和 E2E 测试用例。
- 依赖关系、已知限制和变更记录。

## 当前文档

- [组件与页面登记表](./component-page-registry.md)
- [管理端整体布局 Spec](./layouts/admin-layout.spec.md)
- [核心组件、路由与布局开发文档](./components/core-component-page-development.md)
- [占位业务页面开发文档](./modules/placeholder-pages-development.md)
- [`LoginPage` 开发文档](./pages/login-page-development.md)
- [`LoginForm` 开发文档](./pages/login-form-development.md)
- [登录与认证模块维护手册](../src/features/auth/README.md)
- [部门管理前端开发文档](../src/features/department-management/README.md)
