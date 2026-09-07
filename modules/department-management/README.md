# 部门管理模块

## 当前版本

- PRD：[部门管理模块 PRD v0.1](./docs/PRD-department-management-v0.1.md)
- PostgreSQL 脚本：[部门管理 PostgreSQL v0.1](./sql/department-management-pgsql-v0.1.sql)
- PostgreSQL 验收：[数据库行为测试 v0.1](./sql/tests/department-management-pgsql-v0.1.test.sql)
- UI/UX 设计：[部门管理 UI/UX 设计 v0.1](./docs/UI-design-department-management-v0.1.md)
- 原型：[总览](./prototypes/department-management-overview-v1.png) / [新增部门](./prototypes/department-create-drawer-v1.png) / [移动影响预览](./prototypes/department-move-impact-v1.png)
- 全局规范：[澄客 CRM Web 设计系统 v1.1](../../design-system/chengke-crm/MASTER.md)

部门管理负责维护租户内的集团与部门层级、部门负责人、显示顺序和部门状态，并为用户管理、角色数据范围和 CRM 业务数据归属提供统一组织基础数据。

用户账号资料和调岗由用户管理维护；角色对组织范围的授权由角色管理维护；部门管理不得直接修改角色权限或业务数据负责人。
