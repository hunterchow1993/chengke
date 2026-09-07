# 查询用户详情 API Spec

| 文档信息 | 内容 |
| --- | --- |
| 文档类型 | API Spec（一个 API 一个 Spec） |
| 所属模块 | 系统管理 / 用户管理 |
| API | 查询用户详情 · GET `/api/v1/system/users/{id}` |
| 文档版本 | v0.1 |
| 更新日期 | 2026-09-04 |
| 模块总览 Spec | [UserController.spec.md](./UserController.spec.md) |
| 状态标记 | **已确认** / **设计建议** / **待确认**（同总览） |

---

## 1. 接口概述

- **用途与调用场景**：管理员点击列表行「编辑」打开编辑抽屉时，获取回填表单所需的用户资料（PRD §6.3）。
- **功能权限**：`system:user:view`；数据权限见总览 §8.2。
- **成功状态码**：200。
- **是否修改数据**：否，**不改变任何数据**。
- **引用章节清单**：
  - 总览 §4.2 全局约定（`ApiResponse`、共享枚举）
  - 总览 §5.1 可见范围（读）、§5.2 越权与存在性保护、§5.6 敏感字段脱敏
  - 总览 §7.2 `sys_user` 表
  - 总览 §8 权限、§11.2 错误码总表

---

## 2. 请求示例

```
GET /api/v1/system/users/<uuid>
```

---

## 3. 请求参数（Controller 层契约）

| 参数 | 类型 | 来源 | 必填 | 默认值 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- | --- |
| `id` | UUID | Path | 是 | — | UUID 格式 | 目标用户 ID |

---

## 4. 参数校验与业务校验

**参数校验**

- `id` 必填，必须为合法 UUID。

**业务校验**

- 目标用户必须存在且所属部门在操作者可管理集合内（总览 §5.1）；否则 404，不暴露存在性（总览 §5.2）。

---

## 5. 业务逻辑（Service 层）

```
请求 + JWT
→ 功能权限校验（view，双层）
→ 解析操作者与数据范围
→ 查询目标用户（租户 + id）
→ 范围校验：用户所属部门 ∈ 可管理集合；不满足 → 404（总览 §5.2）
→ 关联部门名称、角色名称
→ 按敏感字段权限处理手机号与邮箱（总览 §5.6）
→ 返回 200 + 用户详情
```

**不返回的字段**：`initialPassword` / 任何密码信息（密码不得回显，PRD §11）；`forcePasswordChange`（编辑表单不展示该字段，PRD §8.3；是否需要返回 **待确认**，见 §12）。

---

## 6. 数据操作与 SQL（DAO 层）

| 操作 | 操作目的（服务的业务规则） | 数据来源 | 数据变化 |
| --- | --- | --- | --- |
| SELECT 单用户（关联部门名、角色名） | 编辑回填；仅范围内可读（总览 §5.1） | `id` 参数 + 租户 | 无 |

> 角色名称的关联依赖角色表，角色表落地前来源 **待确认**（总览 §18 P0-2）。

```sql
SELECT u.id, u.name, u.username, u.avatar_url, u.mobile, u.email,
       u.department_id, d.name AS department_name,
       u.role_id, /* 角色名称：角色表落地后关联，待确认 */
       u.status, u.remark, u.version, u.updated_at
FROM chengke_crm.sys_user u
JOIN chengke_crm.department d
  ON d.tenant_id = u.tenant_id AND d.id = u.department_id AND d.deleted_at IS NULL
WHERE u.tenant_id = :tenantId
  AND u.id = :id;
```

---

## 7. 响应结构

**`UserDetailVO`**（在列表项字段基础上增加 `email` / `remark` / `version`）

| 字段 | 类型 | 一定存在 | 空值规则 | 业务含义 | 来源 |
| --- | --- | --- | --- | --- | --- |
| `id` | UUID | 是 | — | 用户 ID | DB |
| `name` | String | 是 | — | 姓名 | DB |
| `username` | String | 是 | — | 登录用户名（编辑表单只读展示，PRD §8.3） | DB |
| `avatarUrl` | String | 否 | 未上传头像时为 null | 头像地址 | DB |
| `mobile` | String | 是 | — | 手机号；无 `system:user:mobile:full` 权限时脱敏（总览 §5.6） | DB + 权限处理 |
| `email` | String | 否 | 未填写为 null；无 `system:user:email:full` 权限时返回脱敏值（脱敏格式 **待确认**，总览 §18 P1-1） | 邮箱 | DB + 权限处理 |
| `departmentId` | UUID | 是 | — | 所属部门 ID | DB |
| `departmentName` | String | 是 | — | 所属部门名称 | DB 关联 |
| `roleId` | UUID | 是 | — | 角色 ID（v1.0 单角色） | DB |
| `roleName` | String | 是 | — | 角色名称 | DB 关联 |
| `status` | Enum | 是 | — | `active` / `disabled` | DB |
| `remark` | String | 否 | 未填写为 null | 备注 | DB |
| `version` | Integer | 是 | — | 乐观锁版本，编辑提交时回传（总览 §4.3 决策） | DB |
| `updatedAt` | OffsetDateTime | 是 | — | 最近更新时间 | DB |

---

## 8. 响应示例

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "id": "<uuid>",
    "name": "周八",
    "username": "zhouba",
    "avatarUrl": null,
    "mobile": "138****8006",
    "email": "zh***@example.com",
    "departmentId": "<uuid>",
    "departmentName": "上海销售一部",
    "roleId": "<uuid>",
    "roleName": "华东销售经理",
    "status": "active",
    "remark": "备注文本",
    "version": 2,
    "updatedAt": "2026-09-01T10:30:00+08:00"
  },
  "requestId": "<request-id>",
  "timestamp": "<offset-datetime>"
}
```

> 示例中邮箱脱敏格式仅为占位示意，实际格式 **待确认**（总览 §18 P1-1）。

---

## 9. 成功 / 失败行为

**成功**：请求通过权限与校验 → 查询并关联 → 脱敏处理 → 返回 200 + 详情；不改变任何数据。

**失败**（数据均不变化）：

| 场景 | 错误码（总览 §11.2） | HTTP | 业务是否终止 |
| --- | --- | --- | --- |
| `id` 格式错误 | `USER_REQUEST_INVALID` | 400 | 是 |
| 无 `system:user:view` | `USER_OPERATION_FORBIDDEN` | 403 | 是 |
| 用户不存在或所属部门不在可管理范围 | `USER_NOT_FOUND` | 404 | 是 |
| 数据库错误 | `USER_DATABASE_OPERATION_FAILED` | 500 | 是 |

> 错误语义口径差异（PRD「返回无权限」 vs 项目 404 约定）见总览 §18 P0-5。

---

## 10. 事务与并发要点

读接口，无事务、无数据变化、无并发一致性要求。

---

## 11. 验收标准（本 API）

- [ ] GET `/api/v1/system/users/{id}` 可正常调用，成功返回 200。
- [ ] 返回字段与 §7 一致；不包含任何密码字段与 `forcePasswordChange`。
- [ ] 用户名、部门、角色、状态、备注、版本正确回填。
- [ ] 无 `system:user:mobile:full` 权限时 `mobile` 返回脱敏值；无 `system:user:email:full` 权限时 `email` 返回脱敏值。
- [ ] 用户不存在或范围外返回 404，不暴露存在性。
- [ ] 不改变任何数据。

---

## 12. 本接口相关待确认问题

| 优先级 | 问题 | 为什么需要确认 | 影响 |
| --- | --- | --- | --- |
| P2 | 详情是否需要返回 `forcePasswordChange`？（当前按 PRD §8.3「编辑表单不展示」决定不返回） | 若后续有其他页面消费该字段需调整 | §7 响应结构 |
