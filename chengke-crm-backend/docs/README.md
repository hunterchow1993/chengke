# 澄客 CRM 后端开发文档索引

本文目录保存后端实现前的开发契约。业务代码、数据库迁移、API 契约或测试发生变化时，必须先更新对应文档，再进入实现。

## 文档目录

| 模块 | 开发文档 | 状态 |
| --- | --- | --- |
| 部门管理 | [DepartmentController 业务 Spec](./modules/modules/system/department/DepartmentController.spec.md) | 已实现 |
| 认证与授权 | [AuthenticationAndAuthorization 业务 Spec](./modules/shared/security/AuthenticationAndAuthorization.spec.md) | 已实现（资源服务器；不负责登录与 Token 签发） |
| 登录与会话认证 | [登录模块总览 Spec](./modules/modules/auth/LoginController.spec.md) + 5 份 API Spec（一个 API 一个 Spec，见总览 §4.1 索引表） | 未实现（Spec 评审中，基于登录模块 PRD v0.3） |
| 用户管理 | [用户管理模块总览 Spec](./modules/modules/system/user/UserController.spec.md) + 14 份 API Spec（一个 API 一个 Spec，见总览 §4.1 索引表） | 未实现（Spec 评审中，基于用户管理模块 PRD v0.6） |

## 文档先行变更门禁

任何后端功能新增或修改必须按以下顺序执行：

1. 更新模块开发文档的需求来源、接口、方法、DTO/VO、错误码、数据库影响和测试矩阵。
2. 在文档变更记录中写明变更原因、兼容性和迁移要求。
3. 根据文档先新增或修改自动化测试，并确认测试在旧实现上按预期失败。
4. 修改生产代码和 Flyway 迁移，使测试通过。
5. 运行单元测试、Mapper/Testcontainers 测试、Controller 测试、集成测试、架构测试和 OpenAPI 契约检查。
6. 回看文档与最终实现，修正类名、方法签名、字段、错误码或行为差异后再提交。

禁止以下做法：

- 先修改 Controller、Service、Mapper、DTO、VO 或数据库，再事后补文档。
- 只修改 Swagger 注解而不更新模块开发文档。
- 修改已在共享环境执行的 Flyway 脚本；必须新增前向修复迁移。
- 删除或放宽测试来迁就未经评审的行为变化。
- 让 Request DTO 直接进入 Mapper，或把数据库 DO 直接返回给前端。

## 模块文档最低内容

每个业务模块的后端开发文档至少包含：

- 需求来源、范围、关键决策和待确认项。
- 包结构与依赖边界。
- Controller 和 Application Service 方法契约。
- Request DTO、Command/Query、Application Result、Response VO、DO 和枚举定义。
- 数据表、Flyway、Mapper SQL 或存储函数映射。
- 权限、租户、事务、并发、缓存和审计规则。
- 稳定错误码及 HTTP 状态映射。
- 可执行测试矩阵和 Definition of Done。
- 文档版本及变更记录。

## 注释要求

- 新增或修改的公开类、组件、函数和方法必须有说明职责的注释。
- Java 公共类和方法使用 Javadoc，写清参数、返回值、异常和副作用。
- Controller 方法说明权限与 HTTP 行为；Application 方法说明事务、缓存和审计副作用。
- 少于 5 行且逻辑完全直观的私有方法可不写 Javadoc，但提交说明中必须能解释为何跳过。
