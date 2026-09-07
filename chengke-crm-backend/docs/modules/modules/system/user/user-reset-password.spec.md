# 重置用户密码 API Spec

| 文档信息 | 内容 |
| --- | --- |
| 文档类型 | API Spec（一个 API 一个 Spec） |
| 所属模块 | 系统管理 / 用户管理 |
| API | 重置用户密码 · POST `/api/v1/system/users/{id}/reset-password` |
| 文档版本 | v0.1 |
| 更新日期 | 2026-09-04 |
| 模块总览 Spec | [UserController.spec.md](./UserController.spec.md) |
| 状态标记 | **已确认** / **设计建议** / **待确认**（同总览） |

---

## 1. 接口概述

- **用途与调用场景**：管理员在行内更多菜单选择「重置密码」，使用独立对话框设置新密码（不进入编辑资料表单），纳入 v1.0（PRD §7.6、§8.3）。
- **功能权限**：`system:user:reset-password`（总览 §8.1）；数据权限见总览 §8.2。
- **成功状态码**：200。
- **是否修改数据**：是。修改：用户行密码凭据与 `forcePasswordChange`；新增：审计行（总览 §7.3）。
- **引用章节清单**：
  - 总览 §4.2 全局约定、§5.2 越权保护
  - 总览 §9 事务、§10 并发、§11.2 错误码总表、§13 审计（密码安全要求）

---

## 2. 请求示例

```json
{
  "newPassword": "<password>",
  "forcePasswordChange": true
}
```

---

## 3. 请求参数（Controller 层契约）

| 参数 | 类型 | 来源 | 必填 | 默认值 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- | --- |
| `id` | UUID | Path | 是 | — | UUID 格式 | 目标用户 ID |
| `newPassword` | String | Body | 是 | — | 同初始密码规则：8–32 字符；至少包含字母、数字、特殊字符中的两类；不允许连续空格（设计建议沿用新增规则，PRD §8.2） | 新密码 |
| `forcePasswordChange` | Boolean | Body | 否 | `true`（设计建议，与新增默认一致） | — | 是否要求下次登录修改密码（重置密码时可设置，已确认，PRD §8.3） |

---

## 4. 参数校验与业务校验

**参数校验**

- `id` 必填，合法 UUID。
- `newPassword` 必填，满足密码强度规则；不满足返回 400，消息指明规则（总览 §11.1）。

**业务校验**

- 目标用户存在且在操作者数据范围内（总览 §5.1 / §5.2）。
- 操作者能否重置本人密码 **待确认**（§12）。

---

## 5. 业务逻辑（Service 层）

```
请求 + JWT
→ 功能权限校验（reset-password，双层）
→ 数据范围校验（范围外 → 404，不暴露存在性）
→ 密码规则校验
→ 密码不可逆化处理（禁止明文存储，具体算法为技术实现；总览 §13）
→ 事务内：更新密码凭据与 forcePasswordChange + 写审计
   （action=reset_password；审计不含密码，总览 §13）
→ 提交后：授权版本 +1（登录前置条件变化，总览 §9.1）
→ 返回 200（响应不含密码，总览 §13）
```

密码变更只能通过本接口完成；编辑接口不接受密码字段（已确认，PRD §8.3）。

---

## 6. 数据操作与 SQL（DAO 层）

| 操作 | 操作目的（服务的业务规则） | 数据来源 | 数据变化 |
| --- | --- | --- | --- |
| SELECT 目标用户 | 范围校验（总览 §5.2） | `id` + 租户 | 无 |
| UPDATE 密码凭据与改密开关 | 重置密码；登录模块消费 `forcePasswordChange`（登录 PRD 已确认） | 请求体 | 修改 `sys_user` 行（`版本+1`） |
| INSERT 审计行 | 总览 §13（重置密码必须审计） | 操作者 + 时间 + 结果 | 新增 `user_audit_log` 行；**快照不得包含密码** |

```sql
UPDATE chengke_crm.sys_user
SET /* 密码凭据 */ = :newCredential,
    force_password_change = :forcePasswordChange,
    version = version + 1, updated_by = :actorId
WHERE tenant_id = :tenantId AND id = :id;
```

---

## 7. 响应结构

**`ResetPasswordResultVO`**（设计建议）

| 字段 | 类型 | 一定存在 | 空值规则 | 业务含义 | 来源 |
| --- | --- | --- | --- | --- | --- |
| `userId` | UUID | 是 | — | 用户 ID | Path |
| `forcePasswordChange` | Boolean | 是 | — | 设置后的改密开关 | 请求 + 默认值 |
| `updatedAt` | OffsetDateTime | 是 | — | 操作时间 | 系统 |

响应与日志中不得出现新密码（已确认，PRD §11）。

---

## 8. 响应示例

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "userId": "<uuid>",
    "forcePasswordChange": true,
    "updatedAt": "<offset-datetime>"
  },
  "requestId": "<request-id>",
  "timestamp": "<offset-datetime>"
}
```

---

## 9. 成功 / 失败行为

**成功**：校验通过 → 事务更新凭据与开关 + 审计 → 提交后授权版本 +1 → 返回 200（不含密码）。

**失败**（数据均不变化）：

| 场景 | 错误码（总览 §11.2） | HTTP | 业务是否终止 |
| --- | --- | --- | --- |
| `id` 格式错误 / 密码强度不足 | `USER_REQUEST_INVALID` | 400 | 是 |
| 无 `system:user:reset-password` | `USER_OPERATION_FORBIDDEN` | 403 | 是 |
| 用户不存在或范围外 | `USER_NOT_FOUND` | 404 | 是 |
| 数据库写失败 | `USER_DATABASE_OPERATION_FAILED` | 500 | 是（回滚） |

---

## 10. 事务与并发要点

- **原子组**（总览 §7.3、§9）：密码凭据 + `forcePasswordChange` + 审计行必须同时一致；不得出现凭据已改但开关未更新。
- **提交后**（总览 §9.1）：授权版本 +1。
- **并发**：重复重置无数据损坏风险，每次生成新凭据与审计（总览 §10.2）；是否限制重置频率 **待确认**（§12）。

---

## 11. 验收标准（本 API）

- [ ] POST `/api/v1/system/users/{id}/reset-password` 可正常调用，成功返回 200。
- [ ] 密码强度不足返回 400，消息指明规则。
- [ ] 重置成功后：新凭据生效、`forcePasswordChange` 按请求 / 默认值落库、审计 `action=reset_password`。
- [ ] 响应、审计、日志中均不含新密码。
- [ ] 范围外用户返回 404，不暴露存在性。
- [ ] 重置成功后授权版本 +1。

---

## 12. 本接口相关待确认问题

| 优先级 | 问题 | 为什么需要确认 | 影响 |
| --- | --- | --- | --- |
| P1 | 操作者能否通过本接口重置自己的密码？（PRD 未禁止也未允许；本人资料自助维护范围未定） | 影响本接口的目标校验 | §4 业务校验、§9 |
| P2 | 是否限制重置频率 / 重复重置行为？ | 总览 §10.2 | §10 |
