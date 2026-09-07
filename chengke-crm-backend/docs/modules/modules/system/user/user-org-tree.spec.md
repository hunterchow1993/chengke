# 查询用户管理组织树 API Spec

| 文档信息 | 内容 |
| --- | --- |
| 文档类型 | API Spec（一个 API 一个 Spec） |
| 所属模块 | 系统管理 / 用户管理 |
| API | 查询用户管理组织树 · GET `/api/v1/system/users/organization-tree` |
| 文档版本 | v0.1 |
| 更新日期 | 2026-09-04 |
| 模块总览 Spec | [UserController.spec.md](./UserController.spec.md) |
| 状态标记 | **已确认** / **设计建议** / **待确认**（同总览） |

---

## 1. 接口概述

- **用途与调用场景**：进入用户管理页时加载左侧组织树：仅展示操作者有权查看的组织节点、祖先路径和权限范围内人数（PRD §4.9、§7.2、§12.1）。默认加载根节点、首层节点与操作者祖先路径，其余节点由 [user-org-tree-children.spec.md](./user-org-tree-children.spec.md) 懒加载（TREE-05）。
- **功能权限**：`system:user:view`；数据权限见总览 §8.2、§5.1。
- **成功状态码**：200。
- **是否修改数据**：否，**不改变任何数据**。
- **引用章节清单**：
  - 总览 §4.2 全局约定、§4.3 关键设计决策（组织树接口归属）
  - 总览 §5.1 可见范围（读）、§5.2 越权保护、§8.2 数据权限
  - 总览 §7.2 `department` / `department_closure` / `sys_user` 表、§11.2 错误码总表

---

## 2. 请求示例

```
GET /api/v1/system/users/organization-tree
```

无请求参数。

---

## 3. 请求参数（Controller 层契约）

无请求参数。默认返回内容（设计建议，对应 PRD §12.1 与 TREE-04）：

- 根节点（集团）与首层节点；
- 当前操作者所属节点的完整祖先路径；
- 每个可见节点的权限范围内用户数；
- 默认选中节点 = 操作者所属部门（见 §5）。

---

## 4. 参数校验与业务校验

**参数校验**：无参数。

**业务校验**

- 操作者无任何可见组织节点时：返回空节点列表（前端显示「暂无可查看的组织」，右侧不发起用户列表请求，PRD §9.2）。

---

## 5. 业务逻辑（Service 层）

```
请求 + JWT
→ 功能权限校验（view，双层）
→ 计算可见节点集合 = 可管理部门 ∪ 其祖先（总览 §5.1）
→ 读取默认加载范围的节点（根 + 首层 + 操作者祖先路径）
→ 聚合各节点权限范围内用户数（闭包 + 可管理范围交集）
→ 计算默认选中节点（操作者所属部门，须在可见范围内）
→ 返回 200 + 扁平节点列表 + 默认选中标记
```

要点：

- `userCount` = 该节点及全部后代中、所属部门在操作者**可管理范围**内的用户数（已确认口径：只统计有权查看的用户，不能泄露无权限部门人数，TREE-08 / PRD §7.2.1）。
- `manageable` 标记：可见但不可管理的祖先节点为 `false`（用于前端交互；可见性规则总览 §5.1）。
- 节点停用 / 删除 / 移出权限范围后的前端回退（TREE-09）由前端依据本接口数据自行处理，后端保证不返回越权节点。

---

## 6. 数据操作与 SQL（DAO 层）

| 操作 | 操作目的（服务的业务规则） | 数据来源 | 数据变化 |
| --- | --- | --- | --- |
| SELECT 操作者所属部门 | 默认选中节点与祖先路径展开（TREE-04、PRD §6.1） | 操作者 ID（`actorId`） | 无 |
| SELECT 可见节点（可管理 ∪ 祖先，含根与首层） | 总览 §5.1 可见范围；懒加载默认集（PRD §12.1） | 操作者上下文 + 租户 | 无 |
| SELECT 各可见节点的子树用户数 | 人数只统计有权查看的用户（TREE-08） | 可见节点集合 + 可管理范围 | 无 |

```sql
-- 操作者所属部门（默认选中与祖先路径）
SELECT department_id FROM chengke_crm.sys_user
WHERE tenant_id = :tenantId AND id = :actorId;
```

```sql
-- 子树用户数（权限范围内）：对每个可见节点统计
SELECT c.ancestor_id AS node_id, count(u.id) AS user_count
FROM chengke_crm.department_closure c
JOIN chengke_crm.sys_user u
  ON u.tenant_id = c.tenant_id AND u.department_id = c.descendant_id
WHERE c.tenant_id = :tenantId
  AND c.ancestor_id IN (:visibleNodeIds)
  AND u.department_id IN (:manageableDepartmentIds)   -- 全量范围操作者不加此条件
GROUP BY c.ancestor_id;
```

---

## 7. 响应结构

**`UserOrgTreeVO`**（设计建议）

| 字段 | 类型 | 一定存在 | 空值规则 | 业务含义 | 来源 |
| --- | --- | --- | --- | --- | --- |
| `nodes` | `UserOrgNodeVO[]` | 是 | 可为空数组（无可见节点） | 默认可见节点扁平列表（对齐部门树接口的扁平返回约定） | 计算 |
| `defaultSelectedId` | UUID | 否 | 操作者所属部门不在可见范围时为 null（回退规则 **待确认**，§12） | 默认选中节点 | 计算 |

**`UserOrgNodeVO`**（人数要求已确认，PRD §7.2.1 / TREE-08；结构为设计建议）

| 字段 | 类型 | 一定存在 | 空值规则 | 业务含义 | 来源 |
| --- | --- | --- | --- | --- | --- |
| `id` | UUID | 是 | — | 节点 ID | DB |
| `parentId` | UUID | 否 | 集团根为 null | 父节点 | DB |
| `name` | String | 是 | — | 节点名称 | DB |
| `nodeType` | Enum | 是 | — | `group` / `department`（沿用部门模型，总览 §4.2） | DB |
| `status` | Enum | 是 | — | `active` / `disabled` | DB |
| `depth` | Integer | 是 | — | 层级（根=0） | DB |
| `hasChildren` | Boolean | 是 | — | 是否有直接子节点（TREE-02：无子节点时前端保留对齐空间不展示展开图标） | 计算 |
| `userCount` | Integer | 是 | — | 该节点及后代中操作者权限范围内的用户总数 | DB 聚合 |
| `manageable` | Boolean | 是 | — | 是否在操作者可管理范围内（可见但不可管理的祖先为 `false`） | 计算 |

---

## 8. 响应示例

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "nodes": [
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
        "name": "华东事业部",
        "nodeType": "department",
        "status": "active",
        "depth": 1,
        "hasChildren": true,
        "userCount": 56,
        "manageable": true
      }
    ],
    "defaultSelectedId": "<uuid>"
  },
  "requestId": "<request-id>",
  "timestamp": "<offset-datetime>"
}
```

---

## 9. 成功 / 失败行为

**成功**：权限校验通过 → 计算可见节点与人数 → 返回 200；不改变任何数据。

**失败**（数据均不变化）：

| 场景 | 错误码（总览 §11.2） | HTTP | 业务是否终止 |
| --- | --- | --- | --- |
| 无 `system:user:view` | `USER_OPERATION_FORBIDDEN` | 403 | 是 |
| 数据库错误 | `USER_DATABASE_OPERATION_FAILED` | 500 | 是 |

---

## 10. 事务与并发要点

只读接口，无事务、无数据变化、无并发一致性要求。

---

## 11. 验收标准（本 API）

- [ ] GET `/api/v1/system/users/organization-tree` 可正常调用，成功返回 200。
- [ ] 仅返回可见节点（可管理 ∪ 祖先），不返回越权节点。
- [ ] `userCount` 只统计操作者权限范围内的用户（无权限部门人数不泄露）。
- [ ] 默认返回包含根节点、首层节点与操作者祖先路径。
- [ ] `defaultSelectedId` 为操作者所属部门（在可见范围内）。
- [ ] 无可见节点时返回空 `nodes`，不报错。
- [ ] 不改变任何数据。

---

## 12. 本接口相关待确认问题

| 优先级 | 问题 | 为什么需要确认 | 影响 |
| --- | --- | --- | --- |
| P1 | 默认选中节点（操作者所属部门）不在可见范围时的回退策略？（跨部门授权场景可能出现） | 影响 `defaultSelectedId` 返回值 | §5、§7 |
