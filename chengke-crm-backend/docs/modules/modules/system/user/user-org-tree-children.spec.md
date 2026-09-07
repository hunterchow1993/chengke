# 懒加载组织树子节点 API Spec

| 文档信息 | 内容 |
| --- | --- |
| 文档类型 | API Spec（一个 API 一个 Spec） |
| 所属模块 | 系统管理 / 用户管理 |
| API | 懒加载组织树子节点 · GET `/api/v1/system/users/organization-tree/{nodeId}/children` |
| 文档版本 | v0.1 |
| 更新日期 | 2026-09-04 |
| 模块总览 Spec | [UserController.spec.md](./UserController.spec.md) |
| 状态标记 | **已确认** / **设计建议** / **待确认**（同总览） |

---

## 1. 接口概述

- **用途与调用场景**：组织树按节点懒加载子节点，已加载节点在当前会话内缓存（前端缓存，TREE-05）；仅在对应节点下方显示局部加载反馈（PRD §9.1）。
- **功能权限**：`system:user:view`；数据权限见总览 §8.2、§5.1。
- **成功状态码**：200。
- **是否修改数据**：否，**不改变任何数据**。
- **引用章节清单**：
  - 总览 §4.2 全局约定、§5.1 可见范围（读）、§5.2 越权保护
  - 总览 §7.2 `department` / `department_closure` / `sys_user` 表、§11.2 错误码总表
  - [user-org-tree.spec.md](./user-org-tree.spec.md) §7（`UserOrgNodeVO` 节点结构）

---

## 2. 请求示例

```
GET /api/v1/system/users/organization-tree/<uuid>/children
```

---

## 3. 请求参数（Controller 层契约）

| 参数 | 类型 | 来源 | 必填 | 默认值 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- | --- |
| `nodeId` | UUID | Path | 是 | — | UUID 格式 | 父节点 ID |

---

## 4. 参数校验与业务校验

**参数校验**

- `nodeId` 必填，合法 UUID。

**业务校验**

- `nodeId` 节点必须存在且在操作者可见集合内（总览 §5.1）；否则 404，不暴露存在性（总览 §5.2）。
- 只返回可见子节点：子节点可见 ⇔ 子节点在可管理集合内，或子节点是某个可管理节点的祖先（可见 = 可管理 ∪ 祖先，总览 §5.1）。

---

## 5. 业务逻辑（Service 层）

```
请求 + JWT
→ 功能权限校验（view，双层）
→ 校验 nodeId 存在且在可见集合内（否则 404）
→ 读取该节点的可见直接子节点
→ 聚合每个子节点的权限范围内用户数
→ 返回 200 + 子节点列表（结构与组织树节点一致）
```

单个树节点的子节点加载失败时，前端仅在该节点下方提供重试入口（PRD §9.3）；后端按常规错误返回。

---

## 6. 数据操作与 SQL（DAO 层）

| 操作 | 操作目的（服务的业务规则） | 数据来源 | 数据变化 |
| --- | --- | --- | --- |
| SELECT 父节点（可见性校验） | 总览 §5.1 / §5.2 | `nodeId` + 租户 + 操作者范围 | 无 |
| SELECT 直接子节点 | 懒加载（TREE-05） | `nodeId` + 租户 | 无 |
| SELECT 各子节点子树用户数 | 人数只统计有权查看的用户（TREE-08） | 子节点集合 + 可管理范围 | 无 |

```sql
-- 直接子节点
SELECT d.id, d.parent_id, d.name, d.node_type, d.status, d.depth
FROM chengke_crm.department d
WHERE d.tenant_id = :tenantId
  AND d.parent_id = :nodeId
  AND d.deleted_at IS NULL;
```

子树用户数聚合与 [user-org-tree.spec.md](./user-org-tree.spec.md) §6 相同（`ancestor_id IN (:子节点集合)`）。

---

## 7. 响应结构

`data` 为 `UserOrgNodeVO[]`（节点结构定义见 [user-org-tree.spec.md](./user-org-tree.spec.md) §7，含 `userCount` 与 `manageable`）。

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
      "name": "上海销售一部",
      "nodeType": "department",
      "status": "active",
      "depth": 2,
      "hasChildren": false,
      "userCount": 12,
      "manageable": true
    }
  ],
  "requestId": "<request-id>",
  "timestamp": "<offset-datetime>"
}
```

---

## 9. 成功 / 失败行为

**成功**：权限与可见性校验通过 → 返回可见子节点（含人数）；不改变任何数据。

**失败**（数据均不变化）：

| 场景 | 错误码（总览 §11.2） | HTTP | 业务是否终止 |
| --- | --- | --- | --- |
| `nodeId` 格式错误 | `USER_REQUEST_INVALID` | 400 | 是 |
| 无 `system:user:view` | `USER_OPERATION_FORBIDDEN` | 403 | 是 |
| 节点不存在或不可见 | `USER_NOT_FOUND` | 404 | 是 |
| 数据库错误 | `USER_DATABASE_OPERATION_FAILED` | 500 | 是 |

---

## 10. 事务与并发要点

只读接口，无事务、无数据变化、无并发一致性要求。

---

## 11. 验收标准（本 API）

- [ ] GET `/api/v1/system/users/organization-tree/{nodeId}/children` 可正常调用，成功返回 200。
- [ ] 只返回可见子节点；不可见子节点不出现在结果中。
- [ ] 每个子节点 `userCount` 只统计权限范围内用户。
- [ ] 父节点不存在或不可见返回 404。
- [ ] 不改变任何数据。

---

## 12. 本接口相关待确认问题

无。
