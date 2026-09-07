# 编辑用户 API Spec

| 文档信息 | 内容 |
| --- | --- |
| 文档类型 | API Spec（一个 API 一个 Spec） |
| 所属模块 | 系统管理 / 用户管理 |
| API | 编辑用户 · PUT `/api/v1/system/users/{id}` |
| 文档版本 | v0.1 |
| 更新日期 | 2026-09-04 |
| 模块总览 Spec | [UserController.spec.md](./UserController.spec.md) |
| 状态标记 | **已确认** / **设计建议** / **待确认**（同总览） |

---

## 1. 接口概述

- **用途与调用场景**：管理员在编辑抽屉修改用户资料、部门、角色或状态后保存（PRD §6.3、§8.3）。接口形态为 PUT + 完整字段 + `version`（PRD §8.6 授权接口设计决定；总览 §4.3 决策）。
- **功能权限**：`system:user:update`；数据权限见总览 §8.2、§5.4。
- **成功状态码**：200。
- **是否修改数据**：是。修改：用户行；换部门时新增新部门引用、删除旧部门引用；新增审计行（总览 §7.3）。
- **引用章节清单**：
  - 总览 §4.2 全局约定、§4.3 关键设计决策（PUT + version）
  - 总览 §5.2 越权保护、§5.3 保护账号规则与决策表、§5.4 授权边界、§5.5 唯一性、§5.7 部门引用登记
  - 总览 §6 状态流转
  - 总览 §7.2 表结构、§9 事务、§10 并发、§11.2 错误码总表、§13 审计

---

## 2. 请求示例

```json
{
  "avatarUrl": "<avatar-url>",
  "name": "周八",
  "mobile": "13800008006",
  "email": "zhouba@example.com",
  "departmentId": "<uuid>",
  "roleId": "<uuid>",
  "status": "active",
  "remark": "备注文本",
  "version": 2
}
```

---

## 3. 请求参数（Controller 层契约）

| 参数 | 类型 | 来源 | 必填 | 默认值 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- | --- |
| `id` | UUID | Path | 是 | — | UUID 格式 | 目标用户 ID |
| `avatarUrl` | String | Body | 否 | — | ≤500（设计建议） | 头像 |
| `name` | String | Body | 是 | — | 同新增（2–30 字符等） | 姓名 |
| `mobile` | String | Body | 是 | — | 同新增；唯一性校验排除当前用户（PRD §8.3） | 手机号 |
| `email` | String | Body | 否 | — | 同新增 | 邮箱 |
| `departmentId` | UUID | Body | 是 | — | UUID | 所属部门；切换部门不自动改变角色（PRD §8.5 已确认） |
| `roleId` | UUID | Body | 是 | — | UUID | 角色 |
| `status` | Enum | Body | 是 | — | 仅 `active` / `disabled` | 账号状态；保护规则见 §5 |
| `remark` | String | Body | 否 | — | ≤200 字符 | 备注 |
| `version` | Integer | Body | 是 | — | 正整数 | 乐观锁版本（总览 §4.3 决策） |

**不可通过本接口修改（已确认）**：`username`（创建后不可修改，编辑表单只读，PRD §8.3）；任何密码字段（密码变更只能走 [user-reset-password.spec.md](./user-reset-password.spec.md)，PRD §8.3）；`forcePasswordChange`（仅在创建或重置密码时设置，PRD §8.3）。

---

## 4. 参数校验与业务校验

**参数校验**

- `id`、`version` 必填；`name` / `mobile` / `departmentId` / `roleId` / `status` 必填；字段格式同新增（见 [user-create.spec.md](./user-create.spec.md) §4）。

**业务校验**

- 目标用户存在且在操作者数据范围内（加载与保存时均校验，PRD §4.9）。
- `version` 与数据库一致（并发冲突检测）。
- 手机号唯一性排除本人（总览 §5.5）。
- 部门 / 角色校验同新增（存在、可用、可管理 / 可授予）。
- 保护规则（总览 §5.3）：本人不可改为 `disabled`；唯一超级管理员不可改为 `disabled`；本人角色不可修改。

---

## 5. 业务逻辑（Service 层）

```
请求 + JWT
→ 功能权限校验（update，双层）
→ 数据范围校验（目标用户在操作者范围内，不在 → 404，
   消息「该用户已不在你的管理范围内」，PRD §4.9）
→ 读取当前用户（含 version）
→ 版本一致校验（不一致 → 409，提示刷新后重试，PRD §6.3）
→ 参数校验 + 部门 / 角色校验 + 手机号唯一性（排除本人）
→ 保护规则校验（本人停用、唯一超级管理员停用、本人角色，总览 §5.3）
→ 事务内：更新用户（version+1）+（换部门时）引用迁移（总览 §5.7）+ 写审计
→ 提交后：部门 / 角色 / 状态任一变化 → 授权版本 +1；失效相关缓存（总览 §9.1）
→ 返回 200 + 新版本
```

**编辑保护规则决策表（本接口特有，基于总览 §5.3）**

| 变更内容 | 目标是本人 | 目标是唯一超级管理员 | 允许 |
| --- | --- | --- | --- |
| 修改姓名 / 手机号 / 邮箱 / 备注 / 头像 | — | — | 是（在范围内） |
| 修改部门 | — | — | 是（目标部门可管理且 `active`） |
| 修改角色 | 是 | — | 否（本人角色不可自改，总览 §5.3；→ 422） |
| 修改角色 | 否 | — | 是（角色 `active` 且可授予；「可授予」判定待确认，总览 §18 P0-8） |
| 状态改为 `disabled` | 是 | — | 否（→ 422，消息「不能停用自己」） |
| 状态改为 `disabled` | 否 | 是 | 否（→ 422，消息「唯一超级管理员受保护」） |
| 状态改为 `active` | — | — | 是 |

当前角色已停用时：编辑表单仍展示原值并要求改选有效角色后才能保存（已确认，PRD §8.3）→ 提交停用角色值按「角色已停用」拒绝（422）。

---

## 6. 数据操作与 SQL（DAO 层）

| 操作 | 操作目的（服务的业务规则） | 数据来源 | 数据变化 |
| --- | --- | --- | --- |
| SELECT 当前用户（含 version、部门、角色） | 范围校验、版本校验、保护规则判断必须基于提交时最新数据（总览 §10.1） | `id` + 租户 | 无 |
| SELECT 部门 / 角色 | 可用性 / 可管理 / 可授予校验 | 请求体 + 操作者范围 | 无 |
| SELECT 手机号唯一性（排除本人） | 总览 §5.5 | 请求体 + 租户 + `id` | 无 |
| UPDATE 用户（`version` 条件） | 并发不覆盖（总览 §10.1） | 请求体 + `version` | 修改 `sys_user` 行，`version+1` |
| 注销旧部门引用 + 登记新部门引用（仅换部门时） | 总览 §5.7 | 新旧部门 + 用户 | 删除旧 `department_reference` 行；新增新引用行 |
| INSERT 审计行 | 总览 §13 | 变更前后快照 + 操作者 | 新增 `user_audit_log` 行（`action=update`，不含密码） |

```sql
-- 版本冲突检测更新（影响行数为 0 → 409）
UPDATE chengke_crm.sys_user
SET name = :name, mobile = :mobile, email = :email,
    department_id = :departmentId, role_id = :roleId,
    status = :status, remark = :remark,
    version = version + 1, updated_by = :actorId
WHERE tenant_id = :tenantId AND id = :id AND version = :version;
```

```sql
-- 换部门时的引用迁移（总览 §5.7）
SELECT chengke_crm.unregister_department_reference(
    :tenantId, :oldDepartmentId, 'user', :userId);
SELECT chengke_crm.register_department_reference(
    :tenantId, :newDepartmentId, 'user', :userId, true, '{}'::jsonb);
```

```sql
-- 手机号唯一性（排除本人）
SELECT 1 FROM chengke_crm.sys_user
WHERE tenant_id = :tenantId AND mobile = :mobile AND id <> :id;
```

---

## 7. 响应结构

**`UserUpdateResultVO`**（设计建议）

| 字段 | 类型 | 一定存在 | 空值规则 | 业务含义 | 来源 |
| --- | --- | --- | --- | --- | --- |
| `userId` | UUID | 是 | — | 用户 ID | Path |
| `version` | Integer | 是 | — | 更新后版本 | 系统递增 |
| `status` | Enum | 是 | — | 更新后状态 | 请求 |
| `updatedAt` | OffsetDateTime | 是 | — | 更新时间 | 系统 |

---

## 8. 响应示例

**成功**

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "userId": "<uuid>",
    "version": 3,
    "status": "active",
    "updatedAt": "<offset-datetime>"
  },
  "requestId": "<request-id>",
  "timestamp": "<offset-datetime>"
}
```

**失败（版本冲突）**

```json
{
  "code": "USER_VERSION_CONFLICT",
  "message": "用户信息已被其他管理员修改",
  "data": null,
  "requestId": "<request-id>",
  "timestamp": "<offset-datetime>"
}
```

---

## 9. 成功 / 失败行为

**成功**：全部校验通过 → 事务更新用户（+ 引用迁移）+ 审计 → 提交后按 §5 规则递增授权版本 → 返回 200 + 新版本。

**失败**（数据均不变化）：

| 场景 | 错误码（总览 §11.2） | HTTP | 业务是否终止 |
| --- | --- | --- | --- |
| 参数缺失 / 格式错误 | `USER_REQUEST_INVALID` | 400 | 是 |
| 无 `system:user:update` | `USER_OPERATION_FORBIDDEN` | 403 | 是 |
| 用户不存在或不在数据范围 | `USER_NOT_FOUND`（保存时已移出范围：消息「该用户已不在你的管理范围内」，PRD §4.9） | 404 | 是 |
| 版本冲突 | `USER_VERSION_CONFLICT` | 409 | 是（提示刷新后重试） |
| 部门不存在 / 不可管理 | `USER_DEPARTMENT_UNAVAILABLE` | 404 | 是 |
| 部门已停用 | `USER_DEPARTMENT_UNAVAILABLE` | 422 | 是 |
| 角色不存在 / 不可授予 | `USER_ROLE_UNAVAILABLE` | 404 | 是 |
| 角色已停用 | `USER_ROLE_UNAVAILABLE` | 422 | 是 |
| 手机号重复 | `USER_MOBILE_DUPLICATE` | 409 | 是 |
| 停用本人 | `USER_SELF_DISABLE_FORBIDDEN` | 422 | 是 |
| 停用唯一超级管理员 | `USER_LAST_SUPER_ADMIN_PROTECTED` | 422 | 是 |
| 修改本人角色 | `USER_SELF_ROLE_CHANGE_FORBIDDEN` | 422 | 是 |
| 数据库写失败 | `USER_DATABASE_OPERATION_FAILED` | 500 | 是（整笔回滚） |

---

## 10. 事务与并发要点

- **原子组**（总览 §7.3、§9）：用户行更新 +（换部门时）旧引用注销 + 新引用登记 + 审计行必须同时一致；不得出现引用仍指向旧部门的中间态。
- **提交后**（总览 §9.1）：部门 / 角色 / 状态任一变化 → 授权版本 +1；失效相关缓存。
- **并发**（总览 §10.1）：
  - 两个管理员并发编辑同一用户：后提交者收到 409，先提交数据保留。
  - 编辑与状态变更接口并发（不同字段）：均为行内更新，以提交顺序为准。
  - 打开抽屉期间目标被调离授权范围：保存时重新范围校验并拒绝（PRD §4.9）。

---

## 11. 验收标准（本 API）

- [ ] PUT `/api/v1/system/users/{id}` 可正常调用，成功返回 200。
- [ ] 用户名只读：请求不含用户名修改路径，数据库用户名不变。
- [ ] 请求不接受密码字段与 `forcePasswordChange` 修改。
- [ ] 版本不一致返回 409，数据不变。
- [ ] 手机号改为已存在号码（他人）返回 409；唯一性排除本人。
- [ ] 停用本人 / 停用唯一超级管理员 / 修改本人角色均被 422 拒绝。
- [ ] 换部门后：旧部门引用注销、新部门引用登记；审计记录 `action=update` 且不含密码。
- [ ] 保存时目标已移出范围 → 404，消息「该用户已不在你的管理范围内」。
- [ ] 部门 / 角色 / 状态变化后授权版本 +1。

---

## 12. 本接口相关待确认问题

无仅影响本接口的待确认问题；相关跨 API 问题见总览 §18（P0-2 角色数据、P0-7 可挂载用户部门、P0-8 可授予判定、P1-5 头像）。
