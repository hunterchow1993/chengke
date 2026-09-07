# 搜索组织节点 API Spec

| 文档信息 | 内容 |
| --- | --- |
| 文档类型 | API Spec（一个 API 一个 Spec） |
| 所属模块 | 系统管理 / 用户管理 |
| API | 搜索组织节点 · GET `/api/v1/system/users/organization-tree/search` |
| 文档版本 | v0.1 |
| 更新日期 | 2026-09-04 |
| 模块总览 Spec | [UserController.spec.md](./UserController.spec.md) |
| 状态标记 | **已确认** / **设计建议** / **待确认**（同总览） |

---

## 1. 接口概述

- **用途与调用场景**：组织树搜索部门：展示匹配节点及其**完整祖先路径**，并高亮匹配文字；清空搜索后前端恢复搜索前的展开和选中状态（TREE-06、TREE-07）。使用服务端检索，不能要求前端先加载整棵组织树（已确认，PRD §12.1）。
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
GET /api/v1/system/users/organization-tree/search?keyword=上海
```

---

## 3. 请求参数（Controller 层契约）

| 参数 | 类型 | 来源 | 必填 | 默认值 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- | --- |
| `keyword` | String | Query | 是 | — | 非空，≤50 字符，首尾空格去除（去除后为空视为参数错误） | 节点名称关键词 |

---

## 4. 参数校验与业务校验

**参数校验**

- `keyword` 必填、非空、≤50 字符；仅输入空格时按空值处理并拒绝（参数异常）。

**业务校验**

- 匹配与返回均限定在操作者可见节点集合内（总览 §5.1）；不得返回越权节点，不得泄露无权限节点人数（人数仅对匹配节点按可管理范围统计，口径同组织树）。

---

## 5. 业务逻辑（Service 层）

```
请求 + JWT
→ 功能权限校验（view，双层）
→ 参数校验
→ 在可见节点集合内按名称模糊检索匹配节点
→ 为每个匹配节点展开完整祖先路径（闭包）
→ 聚合匹配节点的权限范围内用户数
→ 返回 200 + 匹配节点及其祖先节点（去重扁平列表）
```

返回形态说明：扁平节点列表同时包含匹配节点与其全部祖先节点（去重），前端据此渲染祖先路径并高亮匹配文字（设计建议）。

---

## 6. 数据操作与 SQL（DAO 层）

| 操作 | 操作目的（服务的业务规则） | 数据来源 | 数据变化 |
| --- | --- | --- | --- |
| SELECT 可见节点内名称匹配 | 服务端检索（PRD §12.1） | `keyword` + 可见节点集合 | 无 |
| SELECT 匹配节点的祖先路径 | 展示完整祖先路径（TREE-06） | 闭包表 | 无 |
| SELECT 匹配节点子树用户数 | 人数口径与组织树一致（TREE-08） | 匹配节点集合 + 可管理范围 | 无 |

```sql
-- 可见节点内名称匹配
SELECT d.id, d.parent_id, d.name, d.node_type, d.status, d.depth
FROM chengke_crm.department d
WHERE d.tenant_id = :tenantId
  AND d.deleted_at IS NULL
  AND d.id IN (:visibleNodeIds)
  AND d.name ILIKE '%' || :keyword || '%';
```

```sql
-- 匹配节点的祖先路径（含自身）
SELECT a.id, a.name, a.depth
FROM chengke_crm.department_closure c
JOIN chengke_crm.department a ON a.tenant_id = c.tenant_id AND a.id = c.ancestor_id
WHERE c.tenant_id = :tenantId AND c.descendant_id IN (:matchedNodeIds);
```

---

## 7. 响应结构

`data` 为 `UserOrgNodeVO[]`（匹配节点 + 全部祖先节点，去重；节点结构定义见 [user-org-tree.spec.md](./user-org-tree.spec.md) §7）。

---

## 8. 响应示例

```json
{
  "code": "OK",
  "message": "success",
  "data": [
    {
      "id": "<uuid>",
      "parentId": null,
      "name": "澄客集团",
      "nodeType": "group",
      "status": "active",
      "depth": 0,
      "hasChildren": true,
      "userCount": 128,
      "manageable": false
    },
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

**成功**：权限与参数校验通过 → 可见范围内检索 → 返回匹配节点与祖先路径；不改变任何数据。无匹配时返回空数组（前端展示无结果状态）。

**失败**（数据均不变化）：

| 场景 | 错误码（总览 §11.2） | HTTP | 业务是否终止 |
| --- | --- | --- | --- |
| `keyword` 缺失 / 超长 / 纯空格 | `USER_REQUEST_INVALID` | 400 | 是 |
| 无 `system:user:view` | `USER_OPERATION_FORBIDDEN` | 403 | 是 |
| 数据库错误 | `USER_DATABASE_OPERATION_FAILED` | 500 | 是 |

---

## 10. 事务与并发要点

只读接口，无事务、无数据变化、无并发一致性要求。

---

## 11. 验收标准（本 API）

- [ ] GET `/api/v1/system/users/organization-tree/search` 可正常调用，成功返回 200。
- [ ] 返回匹配节点及其完整祖先路径；不返回越权节点。
- [ ] 人数口径与组织树一致（只统计权限范围内用户）。
- [ ] 无匹配时返回空数组，不报错。
- [ ] 不改变任何数据。

---

## 12. 本接口相关待确认问题

| 优先级 | 问题 | 为什么需要确认 | 影响 |
| --- | --- | --- | --- |
| P2 | 搜索是否需要结果上限或分页？（大型组织下匹配节点可能很多） | 影响响应规模与性能 | §5、§7 |
