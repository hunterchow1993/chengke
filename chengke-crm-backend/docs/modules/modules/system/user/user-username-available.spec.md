# 校验用户名可用性 API Spec

| 文档信息 | 内容 |
| --- | --- |
| 文档类型 | API Spec（一个 API 一个 Spec） |
| 所属模块 | 系统管理 / 用户管理 |
| API | 校验用户名可用性 · GET `/api/v1/system/users/username-available` |
| 文档版本 | v0.1 |
| 更新日期 | 2026-09-04 |
| 模块总览 Spec | [UserController.spec.md](./UserController.spec.md) |
| 状态标记 | **已确认** / **设计建议** / **待确认**（同总览） |

---

## 1. 接口概述

- **用途与调用场景**：新增用户表单中用户名字段失焦时，前端先校验格式，格式通过后调用本接口做唯一性异步校验；输入未变化时不重复请求（PRD §8.5）。
- **功能权限**：`system:user:create` 或 `system:user:update`（总览 §8.1）。
- **成功状态码**：200。
- **是否修改数据**：否，**不改变任何数据**。
- **引用章节清单**：
  - 总览 §4.2 全局约定、§5.5 唯一性规则、§8 权限、§11.2 错误码总表

---

## 2. 请求示例

```
GET /api/v1/system/users/username-available?username=zhouba
```

---

## 3. 请求参数（Controller 层契约）

| 参数 | 类型 | 来源 | 必填 | 默认值 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- | --- |
| `username` | String | Query | 是 | — | 非空，≤32 字符 | 待校验用户名 |
| `excludeUserId` | UUID | Query | 否 | — | UUID 格式 | 排除的用户（设计建议；用户名创建后不可修改，编辑场景实际不使用，总览 §5.5） |

---

## 4. 参数校验与业务校验

**参数校验**

- `username` 必填、非空、≤32 字符。
- `excludeUserId` 若提供必须为合法 UUID。

**业务校验**

- 无（唯一性查询本身即业务结果）。

---

## 5. 业务逻辑（Service 层）

```
请求 + JWT
→ 功能权限校验（create 或 update）
→ 参数校验
→ 租户内唯一性查询（用户名忽略大小写，总览 §5.5）；
   提供 excludeUserId 时排除该用户
→ 返回 200 + available
```

**失败语义（已确认，PRD §8.5）**：查询失败或超时时**不得**默认视为「可用」；返回错误，由前端提示重试，表单保留当前输入。

---

## 6. 数据操作与 SQL（DAO 层）

| 操作 | 操作目的（服务的业务规则） | 数据来源 | 数据变化 |
| --- | --- | --- | --- |
| SELECT 唯一性探测 | 总览 §5.5：用户名租户内忽略大小写唯一 | `username` 参数 + 租户 | 无 |

```sql
SELECT 1 FROM chengke_crm.sys_user
WHERE tenant_id = :tenantId AND lower(username) = lower(:username)
  AND (:excludeUserId IS NULL OR id <> :excludeUserId);
```

---

## 7. 响应结构

**`AvailabilityCheckVO`**（设计建议）

| 字段 | 类型 | 一定存在 | 空值规则 | 业务含义 | 来源 |
| --- | --- | --- | --- | --- | --- |
| `available` | Boolean | 是 | — | 是否可用（不存在冲突） | 计算 |

---

## 8. 响应示例

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "available": true
  },
  "requestId": "<request-id>",
  "timestamp": "<offset-datetime>"
}
```

---

## 9. 成功 / 失败行为

**成功**：权限与参数校验通过 → 唯一性查询 → 返回 200 + `available`；不改变任何数据。

**失败**（数据均不变化）：

| 场景 | 错误码（总览 §11.2） | HTTP | 业务是否终止 |
| --- | --- | --- | --- |
| `username` 缺失 / 超长 | `USER_REQUEST_INVALID` | 400 | 是 |
| 无 `system:user:create` 且无 `system:user:update` | `USER_OPERATION_FORBIDDEN` | 403 | 是 |
| 数据库查询失败（不得视为可用，PRD §8.5） | `USER_DATABASE_OPERATION_FAILED` | 500 | 是 |

---

## 10. 事务与并发要点

只读接口，无事务、无并发一致性要求；可安全重试（总览 §10.2）。

---

## 11. 验收标准（本 API）

- [ ] GET `/api/v1/system/users/username-available` 可正常调用，成功返回 200。
- [ ] 用户名已存在（任意大小写形式）时 `available=false`。
- [ ] 用户名不存在时 `available=true`。
- [ ] 查询失败不返回 `available=true`，而是返回错误。
- [ ] 不改变任何数据。

---

## 12. 本接口相关待确认问题

无。
