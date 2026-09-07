# 澄客 CRM

项目资料按业务模块归档。每个模块独立保存 PRD、原型图和相关视觉资产，新增资料时不要放回项目级 `docs`、`prototypes` 或 `assets` 目录。

产品中文品牌为“澄客”，完整产品名称为“澄客 CRM”，项目英文目录统一使用 `chengke-crm`。

架构资料统一存放在 [architecture](./architecture/README.md) 目录，当前文档为 [前端架构设计 v0.3](./architecture/frontend-architecture-v0.3.md) 和 [后端架构设计 v0.2](./architecture/backend-architecture-v0.2.md)。跨模块颜色、字体、Icon 与组件标准统一查阅[澄客 CRM Web 设计系统 v1.1](./design-system/chengke-crm/MASTER.md)。

## 工程入口

- [前端工程](./chengke-crm-frontend/README.md)：React 管理端 SPA
- `chengke-crm-backend/`：Spring Boot REST API

## 模块索引

| 模块 | 当前 PRD | 当前原型 | 模块入口 |
| --- | --- | --- | --- |
| 登录与认证 | [PRD v0.3](./modules/login/docs/PRD-login-v0.3.md) | [桌面 v4](./modules/login/prototypes/login-desktop-v4.png) / [移动 v1](./modules/login/prototypes/login-mobile-v1.png) | [登录模块](./modules/login/README.md) |
| 部门管理 | [PRD v0.1](./modules/department-management/docs/PRD-department-management-v0.1.md) | [交互原型 v0.1](./modules/department-management/docs/UI-design-department-management-v0.1.md) | [部门管理模块](./modules/department-management/README.md) |
| 用户管理 | [PRD v0.6](./modules/user-management/docs/PRD-user-management-v0.6.md) | [组织树用户管理原型 v9](./modules/user-management/prototypes/user-management-org-tree-v9.png) | [用户管理模块](./modules/user-management/README.md) |
| 角色管理 | [PRD v0.3](./modules/role-management/docs/PRD-role-management-v0.3.md) | [角色管理列表 v1](./modules/role-management/prototypes/role-management-list-v1.png) | [角色管理模块](./modules/role-management/README.md) |
| 线索管理 | [线索列表 PRD v0.3](./modules/lead-management/docs/PRD-lead-list-v0.3.md) / [线索池 PRD v0.3](./modules/lead-management/docs/PRD-lead-pool-v0.3.md) | 待设计 | [线索管理模块](./modules/lead-management/README.md) |

## 目录约定

```text
modules/
  <module-name>/
    docs/        当前 PRD 和规则说明
    prototypes/  原型图和历史原型
    assets/      前端交付使用的模块视觉素材，需要时创建
```

模块目录使用英文短横线命名，例如 `user-management`。文档和原型文件名需要包含模块名及版本号。

跨模块的前端、后端、接口、数据库和部署架构统一存放在项目级 `architecture` 目录，不放入单一业务模块。
# chengke
