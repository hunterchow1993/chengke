# 启用或停用单个用户 API Spec

| 文档信息 | 内容 |
| --- | --- |
| 文档类型 | API Spec（一个 API 一个 Spec） |
| 所属模块 | 系统管理 / 用户管理 |
| API | 启用或停用单个用户 · PATCH `/api/v1/system/users/{id}/status` |
| 文档版本 | v0.1 |
| 更新日期 | 2026-09-04 |
| 模块总览 Spec | [UserController.spec.md](./UserController.spec.md) |
| 状态标记 | **已确认** / **设计建议** / **待确认**（同总览） |

---

## 1. 接口概述

- **用途与调用场景**：管理员在行内更多菜单对单个用户执行「停用账号」（仅正常用户展示）或「启用账号」（仅已停用用户展示），前端二次确认后调用（PRD §6.4、§6.5、§7.6）。
- **功能权限**：`system:user:disable`（启用与停用共用，总览 §8.1，已确认）；数据权限见总览 §8.2。
- **成功状态码**：200。
- **是否修改数据**：是。修改：用户行 `status`；新增：审计行（总览 §7.3）。
- **引用章节清单**：
  - 总览 §4.2 全局约定、§5.2 越权保护、§5.3 保护账号规则与决策表
  - 总览 §6 状态流转
  - 总览 §9 事务、§10 并发、§11.2 错误码总表、§13 审计

---

## 2. 请求示例

```json
{
  "targetStatus": "disabled"
}
```

---

## 3. 请求参数（Controller 层契约）

| 参数 | 类型 | 来源 | 必填 | 默认值 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- | --- |
| `id` | UUID | Path | 是 | — | UUID 格式 | 目标用户 ID |
| `targetStatus` | Enum | Body | 是 | — | 仅 `active` / `disabled` | 目标状态 |

本接口**不要求** `version`（状态为窄字段更新，保护规则以提交时最新数据判断；设计建议）。

---

## 4. 参数校验与业务校验

**参数校验**

- `id` 必填，合法 UUID。
- `targetStatus` 必填，仅 `active` / `disabled`。

**业务校验**

- 目标用户存在且在操作者数据范围内（总览 §5.1 / §5.2）。
- 保护账号决策表（总览 §5.3）：本人不可停用；唯一超级管理员不可停用。
- 目标状态与当前状态相同（重复同态变更）的处理 **待确认**（§12）。

---

## 5. 业务逻辑（Service 层）

```
请求 + JWT
→ 功能权限校验（disable，双层）
→ 数据范围校验（范围外 → 404，不暴露存在性）
→ 读取最新状态（保护规则必须基于提交时最新数据判断，总览 §10.1）
→ 保护账号决策表校验（总览 §5.3）
→ 事务内：更新 status（version+1）+ 写审计（action=enable / disable）
→ 提交后：授权版本 +1；失效相关缓存（总览 §9.1）
→ 返回 200
```

**状态变更决策表（本接口特有）**

| 当前状态 | 目标状态 | 保护账号决策表（总览 §5.3） | 允许 | 系统行为 |
| --- | --- | --- | --- | --- |
| `active` | `disabled` | 通过 | 是 | 停用 + 审计（`action=disable`） |
| `active` | `disabled` | 本人或唯一超级管理员 | 否 | 422，专用消息 |
| `disabled` | `active` | —（启用无保护限制） | 是 | 启用 + 审计（`action=enable`），二次确认由前端完成（PRD §6.5） |
| 相同 | 相同 | — | **待确认**（§12） | — |

停用成功后：该用户不可再登录（登录模块消费 `status`，登录 PRD 已确认）；历史业务数据继续保留（已确认，PRD §8.4.1）。停用后已有登录会话是否立即失效 **待确认**（总览 §18 P1-2）。

---

## 6. 数据操作与 SQL（DAO 层）

| 操作 | 操作目的（服务的业务规则） | 数据来源 | 数据变化 |
| --- | --- | --- | --- |
| SELECT 目标用户最新状态与身份 | 范围校验；保护规则基于提交时最新数据（总览 §10.1）；唯一超级管理员判定 | `id` + 租户 | 无 |
| SELECT 超级管理员正常状态用户计数 | 「唯一超级管理员」判定（总览 §5.3；计数实现依赖角色模块，总览 §18 P0-2） | 租户 | 无 |
| UPDATE `status` | 状态流转（总览 §6） | `targetStatus` | 修改 `sys_user` 行 `status`、`version+1` |
| INSERT 审计行 | 总览 §13（启用、停用必须审计） | 变更快照 + 操作者 | 新增 `user_audit_log` 行 |

```sql
UPDATE chengke_crm.sys_user
SET status = :targetStatus, version = version + 1, updated_by = :actorId
WHERE tenant_id = :tenantId AND id = :id;
```

---

## 7. 响应结构

**`UserStatusResultVO`**（设计建议）

| 字段 | 类型 | 一定存在 | 空值规则 | 业务含义 | 来源 |
| --- | --- | --- | --- | --- | --- |
| `userId` | UUID | 是 | — | 用户 ID | Path |
| `status` | Enum | 是 | — | 变更后状态 | 请求 |
| `version` | Integer | 是 | — | 变更后版本 | 系统递增 |
| `updatedAt` | OffsetDateTime | 是 | — | 变更时间 | 系统 |

---

## 8. 响应示例

**成功**

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "userId": "<uuid>",
    "status": "disabled",
    "version": 4,
    "updatedAt": "<offset-datetime>"
  },
  "requestId": "<request-id>",
  "timestamp": "<offset-datetime>"
}
```

**失败（停用本人）**

```json
{
  "code": "USER_SELF_DISABLE_FORBIDDEN",
  "message": "不能停用自己",
  "data": null,
  "requestId": "<request-id>",
  "timestamp": "<offset-datetime>"
}
```

---

## 9. 成功 / 失败行为

**成功**：校验通过 → 事务更新状态 + 审计 → 提交后授权版本 +1、缓存失效 → 返回 200。

**失败**（数据均不变化）：

| 场景 | 错误码（总览 §11.2） | HTTP | 业务是否终止 |
| --- | --- | --- | --- |
| 参数缺失 / `targetStatus` 非法 | `USER_REQUEST_INVALID` | 400 | 是 |
| 无 `system:user:disable` | `USER_OPERATION_FORBIDDEN` | 403 | 是 |
| 用户不存在或范围外 | `USER_NOT_FOUND` | 404 | 是 |
| 停用本人 | `USER_SELF_DISABLE_FORBIDDEN` | 422 | 是 |
| 停用唯一超级管理员 | `USER_LAST_SUPER_ADMIN_PROTECTED` | 422 | 是 |
| 数据库写失败 | `USER_DATABASE_OPERATION_FAILED` | 500 | 是（回滚） |

---

## 10. 事务与并发要点

- **原子组**（总览 §7.3、§9）：`status` 更新 + 审计行必须同时一致。
- **提交后**（总览 §9.1）：授权版本 +1；失效相关缓存。
- **并发**（总览 §10.1）：
  - 停用与启用并发作用于同一用户：最终状态为后提交者的合法变更。
  - 保护判定与更新必须对同一行串行化：保护账号在任何交错下都不得被停用；「唯一超级管理员」判定必须以提交时最新计数为准，不得出现零个正常超管。

---

## 11. 验收标准（本 API）

- [ ] PATCH `/api/v1/system/users/{id}/status` 可正常调用，成功返回 200。
- [ ] 停用：`active` → `disabled`，审计 `action=disable`。
- [ ] 启用：`disabled` → `active`，审计 `action=enable`。
- [ ] 停用本人、停用唯一超级管理员被 422 拒绝（消息正确）。
- [ ] 范围外用户返回 404，不暴露存在性。
- [ ] 状态变更后授权版本 +1。
- [ ] 并发停用超管场景下不出现零个正常超管。

---

## 12. 本接口相关待确认问题

| 优先级 | 问题 | 为什么需要确认 | 影响 |
| --- | --- | --- | --- |
| P1 | 重复同态变更（`active`→`active` 或 `disabled`→`disabled`）是幂等成功还是拒绝？（部门模块为幂等成功；用户场景未定义，总览 §6 / §10.2） | 影响重复请求行为与前端反馈 | §5 决策表、§9 失败行为 |
