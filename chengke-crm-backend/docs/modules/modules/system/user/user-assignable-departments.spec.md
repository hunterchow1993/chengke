# 查询可分配部门选项 API Spec

| 文档信息 | 内容 |
| --- | --- |
| 文档类型 | API Spec（一个 API 一个 Spec） |
| 所属模块 | 系统管理 / 用户管理 |
| API | 查询可分配部门选项 · GET `/api/v1/system/users/assignable-departments` |
| 文档版本 | v0.1 |
| 更新日期 | 2026-09-04 |
| 模块总览 Spec | [UserController.spec.md](./UserController.spec.md) |
| 状态标记 | **已确认** / **设计建议** / **待确认**（同总览） |

---

## 1. 接口概述

- **用途与调用场景**：新增 / 编辑用户表单的部门选择器（可搜索树形单选器）加载选项：仅展示管理员有权分配且状态可用的部门（已确认，PRD §4.9、§8.4.2）；已停用部门不可选（已确认，PRD §8.2）。
- **功能权限**：`system:user:create` 或 `system:user:update`（总览 §8.1）。
- **成功状态码**：200。
- **是否修改数据**：否，**不改变任何数据**。
- **引用章节清单**：
  - 总览 §4.2 全局约定、§5.4 授权边界（不能把用户移到自己无权管理的部门）
  - 总览 §8.2 数据权限、§11.2 错误码总表
  - 总览 §7.2 `department` 表

---

## 2. 请求示例

```
GET /api/v1/system/users/assignable-departments
```

无请求参数。

---

## 3. 请求参数（Controller 层契约）

无请求参数。返回操作者可管理且 `status=active` 的部门（供前端构建可搜索树形选择器）。

---

## 4. 参数校验与业务校验

**参数校验**：无参数。

**业务校验**

- 候选部门必须同时满足：操作者可管理（数据范围，总览 §8.2）且 `status=active`。
- 可见但不可管理的祖先节点**不在**可分配集合内（授权边界：不能把用户移到自己无权管理的部门，总览 §5.4）。
- 「仅末级或允许挂载用户的部门」是否额外限制 **待确认**（总览 §18 P0-7）。

---

## 5. 业务逻辑（Service 层）

```
请求 + JWT
→ 功能权限校验（create 或 update）
→ 计算操作者可管理部门集合（全量范围 → 全部未删除部门）
→ 过滤 status=active
→ 返回 200 + 扁平节点列表（含 parentId，前端构建树）
```

提交表单时只传部门 ID（已确认，PRD §8.4.2）；选项接口同时返回稳定 ID 与名称，禁止前端写死部门数据。

---

## 6. 数据操作与 SQL（DAO 层）

| 操作 | 操作目的（服务的业务规则） | 数据来源 | 数据变化 |
| --- | --- | --- | --- |
| SELECT 可管理且 `active` 的部门 | 仅展示有权分配且状态可用的部门（PRD §8.4.2）；授权边界（总览 §5.4） | 操作者上下文 + 租户 | 无 |

```sql
SELECT d.id, d.parent_id, d.name, d.node_type
FROM chengke_crm.department d
WHERE d.tenant_id = :tenantId
  AND d.deleted_at IS NULL
  AND d.status = 'active'
  AND d.id IN (:manageableDepartmentIds);   -- 全量范围操作者不加此条件
```

---

## 7. 响应结构

**`AssignableDepartmentVO`**（设计建议）

| 字段 | 类型 | 一定存在 | 空值规则 | 业务含义 | 来源 |
| --- | --- | --- | --- | --- | --- |
| `id` | UUID | 是 | — | 部门 ID | DB |
| `parentId` | UUID | 否 | 集团根为 null | 父节点，供前端构建树形选择器 | DB |
| `name` | String | 是 | — | 部门名称 | DB |
| `nodeType` | Enum | 是 | — | `group` / `department` | DB |

---

## 8. 响应示例

```json
{
  "code": "OK",
  "message": "success",
  "data": [
    {
      "id": "<uuid>",
      "parentId": "<uuid>",
      "name": "华东事业部",
      "nodeType": "department"
    },
    {
      "id": "<uuid>",
      "parentId": "<uuid>",
      "name": "上海销售一部",
      "nodeType": "department"
    }
  ],
  "requestId": "<request-id>",
  "timestamp": "<offset-datetime>"
}
```

---

## 9. 成功 / 失败行为

**成功**：权限校验通过 → 返回可分配部门集合；不改变任何数据。无可分配部门时返回空数组（前端表单无法提交部门字段，由新增 / 编辑接口的必填校验兜底）。

**失败**（数据均不变化）：

| 场景 | 错误码（总览 §11.2） | HTTP | 业务是否终止 |
| --- | --- | --- | --- |
| 无 `system:user:create` 且无 `system:user:update` | `USER_OPERATION_FORBIDDEN` | 403 | 是 |
| 数据库错误 | `USER_DATABASE_OPERATION_FAILED` | 500 | 是 |

---

## 10. 事务与并发要点

只读接口，无事务、无数据变化、无并发一致性要求。

---

## 11. 验收标准（本 API）

- [ ] GET `/api/v1/system/users/assignable-departments` 可正常调用，成功返回 200。
- [ ] 只返回操作者可管理且 `active` 的部门；可见但不可管理的祖先不在结果中。
- [ ] 停用部门不在结果中。
- [ ] 全量范围操作者返回租户内全部 `active` 部门。
- [ ] 不改变任何数据。

---

## 12. 本接口相关待确认问题

无仅影响本接口的待确认问题；相关跨 API 问题见总览 §18（P0-7 「末级或允许挂载用户的部门」判定规则）。
