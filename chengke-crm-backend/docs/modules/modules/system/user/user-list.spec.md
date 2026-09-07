# 分页查询用户 API Spec

| 文档信息 | 内容 |
| --- | --- |
| 文档类型 | API Spec（一个 API 一个 Spec） |
| 所属模块 | 系统管理 / 用户管理 |
| API | 分页查询用户 · GET `/api/v1/system/users` |
| 文档版本 | v0.1 |
| 更新日期 | 2026-09-04 |
| 模块总览 Spec | [UserController.spec.md](./UserController.spec.md) |
| 状态标记 | **已确认** / **设计建议** / **待确认**（同总览） |

---

## 1. 接口概述

- **用途与调用场景**：管理员进入用户管理页、切换组织节点 / 数据范围、输入筛选条件查询时，在组织与数据范围内分页检索用户（PRD §6.1）。
- **功能权限**：`system:user:view`；数据权限见总览 §8.2。
- **成功状态码**：200。
- **是否修改数据**：否，**不改变任何数据**。
- **引用章节清单**（阅读本 Spec 前建议先读）：
  - 总览 §4.2 全局约定（`ApiResponse`、`PageVO`、状态枚举、「全部」不传 `status`）
  - 总览 §5.1 可见范围（读）、§5.2 越权与存在性保护、§5.6 敏感字段脱敏
  - 总览 §7.2 `sys_user` 表、`department` / `department_closure` 依赖表
  - 总览 §8 权限、§11.2 错误码总表

---

## 2. 请求示例

```
GET /api/v1/system/users?departmentId=<uuid>&includeDescendants=true&keyword=张&status=active&page=1&pageSize=10
```

「全部」状态不传 `status`（总览 §4.2 共享枚举，已确认）。

---

## 3. 请求参数（Controller 层契约）

| 参数 | 类型 | 来源 | 必填 | 默认值 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- | --- |
| `departmentId` | UUID | Query | 是 | — | UUID 格式 | 当前选中的组织节点（页面始终有选中节点；无可见节点时前端不发起请求，PRD §9.2） |
| `includeDescendants` | Boolean | Query | 否 | `false` | — | 数据范围切换：`false`=仅当前部门，`true`=当前节点及可见后代节点（PRD §7.2.3） |
| `keyword` | String | Query | 否 | — | ≤50 字符，首尾空格去除 | 姓名、用户名模糊查询（PRD FILTER-01）；手机号是否参与匹配 **待确认**（§12） |
| `status` | Enum | Query | 否 | — | 仅 `active` / `disabled`；传其他值（含 `all`）拒绝 | 账号状态筛选；「全部」= 不传本参数 |
| `page` | Integer | Query | 否 | 1 | ≥1 | 页码 |
| `pageSize` | Integer | Query | 否 | 10 | 1–100 | 每页条数（默认 10 已确认，PRD PAGE-02；上限 100 为设计建议） |

---

## 4. 参数校验与业务校验

**参数校验**

- `departmentId` 必填，必须为合法 UUID。
- `status` 只接受 `active` / `disabled`，**禁止**传 `all`（PRD §8.4.1 已确认）。
- `keyword` ≤50 字符，首尾空格去除后为空按空处理。
- `page` ≥1；`pageSize` 1–100。

**业务校验**

- `departmentId` 对应节点必须存在且在操作者可见集合内（可见 = 可管理 + 祖先，总览 §5.1）；否则 404，不暴露存在性（总览 §5.2）。

---

## 5. 业务逻辑（Service 层）

```
请求 + JWT
→ 功能权限校验（view，双层，总览 §8.3）
→ 解析操作者与数据范围
→ 校验 departmentId 节点存在且在可见集合内
→ 计算有效部门集合 = 请求范围（当前节点 [+ 后代]）∩ 可管理部门
→ 按关键词、状态、租户条件分页查询（范围进入查询条件，不做全量内存过滤）
→ 关联部门名称、角色名称
→ 按敏感字段权限处理手机号（邮箱不在列表返回）（总览 §5.6）
→ 返回 200 + PageVO
```

**范围组合决策表（本接口特有）**

| `departmentId` 归属 | `includeDescendants` | 有效查询范围 |
| --- | --- | --- |
| 可管理部门 | `false` | 仅该部门直属用户 |
| 可管理部门 | `true` | 该部门及后代 ∩ 操作者可管理部门 的直属用户 |
| 可见祖先（可管理集合之外） | `false` | 空（祖先直属用户不在可管理范围）→ 返回空列表与 0 总数 |
| 可见祖先 | `true` | 后代 ∩ 可管理部门 的直属用户 |
| 不存在 / 不可见 | — | 404 部门不存在 |

> 「点击只读祖先节点返回空或部分结果」为从「仅返回数据范围内用户」（PRD §4.9）推导的默认口径，是否允许该交互见 §12 与总览 §18 P1-8。

排序固定按 `updatedAt` 倒序（已确认，PRD §7.5）；是否提供排序参数 **待确认**（§12）。

---

## 6. 数据操作与 SQL（DAO 层）

| 操作 | 操作目的（服务的业务规则） | 数据来源 | 数据变化 |
| --- | --- | --- | --- |
| SELECT 可见 / 可管理部门集合（闭包后代展开） | 总览 §5.1 可见范围；「仅当前部门 / 包含下级」切换（PRD §7.2.3） | 操作者上下文 + `departmentId` 参数 | 无 |
| SELECT 分页用户（关联部门名、角色名） | 仅返回范围内用户；关键词 / 状态筛选 | 有效部门集合 + 查询参数 + 租户 | 无 |
| SELECT 总数（与列表同条件） | 总数只统计范围内结果，不泄露全集团数量（PRD §4.9） | 同上 | 无 |

> 角色名称的关联依赖角色表，角色表落地前该列来源 **待确认**（总览 §18 P0-2）。

> SQL 示例仅表达数据访问意图，不代表最终实现；表名为设计建议（总览 §7.2）。

```sql
-- 有效部门集合（当前节点 [+ 后代] ∩ 可管理范围）
SELECT c.descendant_id
FROM chengke_crm.department_closure c
WHERE c.tenant_id = :tenantId
  AND c.ancestor_id = :departmentId
  AND (c.distance = 0 OR :includeDescendants = true)
  AND c.descendant_id IN (:manageableDepartmentIds);  -- 全量范围操作者不加此条件
```

```sql
SELECT u.id, u.name, u.username, u.avatar_url, u.mobile, u.email,
       u.department_id, d.name AS department_name,
       u.role_id, /* 角色名称：角色表落地后关联，待确认 */
       u.status, u.updated_at
FROM chengke_crm.sys_user u
JOIN chengke_crm.department d
  ON d.tenant_id = u.tenant_id AND d.id = u.department_id AND d.deleted_at IS NULL
WHERE u.tenant_id = :tenantId
  AND u.department_id IN (:effectiveDepartmentIds)
  AND (:keyword IS NULL
       OR u.name ILIKE '%' || :keyword || '%'
       OR u.username ILIKE '%' || :keyword || '%')   -- 手机号是否参与：待确认（§12）
  AND (:status IS NULL OR u.status = :status)
ORDER BY u.updated_at DESC
LIMIT :pageSize OFFSET :offset;
```

```sql
-- 总数（与列表完全相同的范围条件）
SELECT count(*)
FROM chengke_crm.sys_user u
WHERE u.tenant_id = :tenantId
  AND u.department_id IN (:effectiveDepartmentIds)
  AND /* 与列表相同的 keyword / status 条件 */;
```

---

## 7. 响应结构

`data` 为 `PageVO<UserListItemVO>`（外壳见总览 §4.2）。

**`UserListItemVO`**

| 字段 | 类型 | 一定存在 | 空值规则 | 业务含义 | 来源 |
| --- | --- | --- | --- | --- | --- |
| `id` | UUID | 是 | — | 用户 ID | DB |
| `name` | String | 是 | — | 姓名 | DB |
| `username` | String | 是 | — | 登录用户名 | DB |
| `avatarUrl` | String | 否 | 未上传头像时为 null；前端以姓名首字占位（PRD §7.5） | 头像地址 | DB |
| `mobile` | String | 是 | — | 手机号；无 `system:user:mobile:full` 权限时返回脱敏值 `138****8006` 格式（总览 §5.6） | DB + 敏感字段权限计算 |
| `departmentId` | UUID | 是 | — | 所属部门 ID | DB |
| `departmentName` | String | 是 | — | 所属部门名称（用户存在则部门引用受保护，部门不会被删除，故必定可解析） | DB 关联 |
| `roleId` | UUID | 是 | — | 角色 ID（v1.0 单角色） | DB |
| `roleName` | String | 是 | — | 角色名称，仅用于展示（PRD §6 角色管理边界） | DB 关联 |
| `status` | Enum | 是 | — | `active` / `disabled` | DB |
| `updatedAt` | OffsetDateTime | 是 | — | 最近更新时间；前端按 `YYYY-MM-DD HH:mm:ss` 展示（PRD §7.5） | DB |

`PageVO.total` 仅统计当前数据权限范围内的结果（已确认，总览 §4.2）。

---

## 8. 响应示例

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "items": [
      {
        "id": "<uuid>",
        "name": "周八",
        "username": "zhouba",
        "avatarUrl": null,
        "mobile": "138****8006",
        "departmentId": "<uuid>",
        "departmentName": "上海销售一部",
        "roleId": "<uuid>",
        "roleName": "华东销售经理",
        "status": "active",
        "updatedAt": "2026-09-01T10:30:00+08:00"
      }
    ],
    "page": 1,
    "pageSize": 10,
    "total": 56
  },
  "requestId": "<request-id>",
  "timestamp": "<offset-datetime>"
}
```

---

## 9. 成功 / 失败行为

**成功**：请求通过权限与校验 → 按有效部门集合分页查询 → 脱敏处理 → 返回 200 + `PageVO`；不改变任何数据。

**失败**（数据均不变化）：

| 场景 | 错误码（总览 §11.2） | HTTP | 业务是否终止 |
| --- | --- | --- | --- |
| 参数缺失 / 格式错误 / `status` 传 `all` 等 | `USER_REQUEST_INVALID` | 400 | 是 |
| 无 `system:user:view` | `USER_OPERATION_FORBIDDEN` | 403 | 是 |
| `departmentId` 不存在或不可见 | `USER_NOT_FOUND`（部门不存在语义；不暴露存在性，总览 §5.2） | 404 | 是 |
| 数据库错误 | `USER_DATABASE_OPERATION_FAILED` | 500 | 是 |

---

## 10. 事务与并发要点

读接口，无事务、无数据变化、无并发一致性要求。

---

## 11. 验收标准（本 API）

- [ ] GET `/api/v1/system/users` 可正常调用，成功返回 200。
- [ ] 必填 / 选填参数与 §3 一致；`status` 传 `all` 被 400 拒绝。
- [ ] 仅返回操作者数据范围内的用户；`total` 只统计范围内结果。
- [ ] 「仅当前部门」不包含下级；「包含下级部门」只包含可见后代节点（§5 决策表全覆盖）。
- [ ] 关键词按姓名、用户名模糊匹配；状态筛选单独或组合生效；均空时返回范围内全部。
- [ ] 排序按 `updatedAt` 倒序。
- [ ] 无 `system:user:mobile:full` 权限时 `mobile` 返回脱敏值。
- [ ] 范围外 / 不存在的 `departmentId` 返回 404，不暴露存在性。
- [ ] 不改变任何数据。

---

## 12. 本接口相关待确认问题

| 优先级 | 问题 | 为什么需要确认 | 影响 |
| --- | --- | --- | --- |
| P0 | 关键词是否匹配手机号？精确还是模糊？（PRD FILTER-01 明确关键词匹配姓名与用户名，但占位提示含手机号且「手机号查询规则待确认」） | 影响查询条件与前端占位语义 | §5 业务逻辑、§6 SQL |
| P1 | 是否允许切换排序字段？（PRD §7.5 标记待确认；当前固定 `updatedAt` 倒序） | 影响是否新增排序参数 | §3 参数、§6 SQL |
| P1 | 点击只读祖先节点的列表口径（§5 决策表第 3、4 行）是否按当前设计交付？ | 总览 §18 P1-8 的产品交互部分 | §5 决策表 |
| P2 | 关键词 `ILIKE` 模糊查询在大数据量下的性能方案？ | 无法利用普通索引 | §6 查询性能 |
