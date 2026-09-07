# 新增用户 API Spec

| 文档信息 | 内容 |
| --- | --- |
| 文档类型 | API Spec（一个 API 一个 Spec） |
| 所属模块 | 系统管理 / 用户管理 |
| API | 新增用户 · POST `/api/v1/system/users` |
| 文档版本 | v0.1 |
| 更新日期 | 2026-09-04 |
| 模块总览 Spec | [UserController.spec.md](./UserController.spec.md) |
| 状态标记 | **已确认** / **设计建议** / **待确认**（同总览） |

---

## 1. 接口概述

- **用途与调用场景**：管理员在新增用户抽屉填写资料、设置初始密码后提交，创建系统账号（PRD §6.2、§8.2）。
- **功能权限**：`system:user:create`；数据权限（目标部门必须在可管理集合内）见总览 §8.2、§5.4。
- **成功状态码**：201。
- **是否修改数据**：是。新增：用户行、部门引用、审计行（总览 §7.3）。
- **引用章节清单**：
  - 总览 §4.2 全局约定、§4.3 关键设计决策
  - 总览 §5.4 授权边界、§5.5 唯一性规则、§5.7 部门引用登记
  - 总览 §6 状态流转（初始状态）
  - 总览 §7.2 `sys_user` / `user_audit_log` / `department_reference`
  - 总览 §9 事务与数据一致性、§10 并发与幂等、§11.2 错误码总表、§13 审计

---

## 2. 请求示例

```json
{
  "avatarUrl": "<avatar-url>",
  "name": "周八",
  "username": "zhouba",
  "mobile": "13800008006",
  "email": "zhouba@example.com",
  "departmentId": "<uuid>",
  "roleId": "<uuid>",
  "status": "active",
  "initialPassword": "<password>",
  "forcePasswordChange": true,
  "remark": "备注文本"
}
```

**不接受的字段（已确认）**：`confirmPassword` 仅前端校验不提交（PRD §8.2）；`tenantId`、操作者字段一律不接受。

---

## 3. 请求参数（Controller 层契约）

| 参数 | 类型 | 来源 | 必填 | 默认值 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- | --- |
| `avatarUrl` | String | Body | 否 | — | ≤500（设计建议） | 头像地址；上传机制 **待确认**（总览 §18 P1-5） |
| `name` | String | Body | 是 | — | 去除首尾空格后 2–30 字符；允许中文、英文、空格和间隔号；不允许纯空格或控制字符（PRD §8.2） | 姓名 |
| `username` | String | Body | 是 | — | 4–32 字符；必须以英文字母开头；仅允许字母、数字、点、下划线、连字符；租户内忽略大小写唯一 | 登录用户名，创建后不可修改（PRD §8.3 已确认） |
| `mobile` | String | Body | 是 | — | 11 位中国大陆手机号；仅数字；租户内唯一；按原始连续数字保存（PRD §8.2） | 手机号 |
| `email` | String | Body | 否 | — | ≤100 字符；填写时执行邮箱格式校验；去除首尾空格并转为小写保存 | 邮箱 |
| `departmentId` | UUID | Body | 是 | — | UUID | 所属部门；业务校验见 §4 |
| `roleId` | UUID | Body | 是 | — | UUID | 角色；v1.0 单角色；业务校验见 §4 |
| `status` | Enum | Body | 否 | `active` | 仅 `active` / `disabled` | 账号状态；允许创建停用账号（PRD §8.2 已确认） |
| `initialPassword` | String | Body | 是 | — | 8–32 字符；至少包含字母、数字、特殊字符中的两类；不允许连续空格（PRD §8.2） | 初始密码 |
| `forcePasswordChange` | Boolean | Body | 否 | `true` | — | 首次登录必须修改密码（PRD §8.2） |
| `remark` | String | Body | 否 | — | ≤200 字符；禁止不可见控制字符 | 备注 |

---

## 4. 参数校验与业务校验

**参数校验**（格式 / 必填，见 §3 校验规则列；密码强度不足返回 400，消息指明规则）

- `name` / `username` / `mobile` / `departmentId` / `roleId` / `initialPassword` 必填。
- `username` 正则：字母开头，仅字母、数字、点、下划线、连字符，4–32 字符。
- `mobile`：11 位数字，符合中国大陆手机号（具体正则 **待确认**）。
- `email` 填写时 ≤100 字符且符合邮箱格式。
- `remark` ≤200 字符。

**业务校验**（见 §5 决策表）

- 部门：存在、`status=active`、在操作者可管理范围内。
- 角色：存在、`status=active`、操作者有权授予（「有权授予」判定机制 **待确认**，总览 §18 P0-8）。
- 用户名：租户内忽略大小写唯一（总览 §5.5）。
- 手机号：租户内唯一（总览 §5.5）。

---

## 5. 业务逻辑（Service 层）

```
请求 + JWT
→ 功能权限校验（create，双层）
→ 参数校验
→ 部门校验（存在、active、可管理）
→ 角色校验（存在、active、可授予）
→ 用户名 / 手机号唯一性校验
→ 密码不可逆化处理（禁止明文存储，具体算法为技术实现；总览 §13）
→ 事务内：写用户 + 登记部门引用（总览 §5.7） + 写审计（总览 §13）
→ 提交后：授权版本 +1 + 失效部门树相关缓存（总览 §9.1）
→ 返回 201（响应与提示不含初始密码，PRD §8.2 已确认）
```

**新增决策表（本接口特有）**

| 部门存在 | 部门状态 | 部门可管理 | 角色存在 | 角色状态 | 用户名唯一 | 手机号唯一 | 允许 | 系统行为 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 否 | — | — | — | — | — | — | 否 | 404 部门不存在 |
| 是 | 非 `active` | — | — | — | — | — | 否 | 422 部门已停用 |
| 是 | `active` | 否 | — | — | — | — | 否 | 404 部门不存在（不暴露越权，总览 §5.2） |
| 是 | `active` | 是 | 否 | — | — | — | 否 | 404 角色不存在 |
| 是 | `active` | 是 | 是 | 非 `active` | — | — | 否 | 422 角色已停用 |
| 是 | `active` | 是 | 是 | `active` | 否 | — | 否 | 409 用户名重复 |
| 是 | `active` | 是 | 是 | `active` | 是 | 否 | 否 | 409 手机号重复 |
| 是 | `active` | 是 | 是 | `active` | 是 | 是 | 是 | 创建用户 + 引用登记 + 审计 |

> 角色「操作者有权授予」校验位 **待确认**（总览 §18 P0-8），落地后在本表补充一列。

创建默认值：`status=active`、`forcePasswordChange=true`（已确认）；新用户初始 `version=1`。

---

## 6. 数据操作与 SQL（DAO 层）

| 操作 | 操作目的（服务的业务规则） | 数据来源 | 数据变化 |
| --- | --- | --- | --- |
| SELECT 部门 | 部门存在、可用、可管理校验 | `departmentId` + 租户 + 操作者范围 | 无 |
| SELECT 角色 | 角色存在、可用校验（数据源 **待确认**，总览 §18 P0-2） | `roleId` + 租户 | 无 |
| SELECT 用户名 / 手机号唯一性 | 总览 §5.5 唯一性规则 | 请求参数 + 租户 | 无 |
| INSERT 用户 | 创建账号 | 请求体 + 操作者 | 新增 `sys_user` 行 |
| 登记部门引用（调用既有函数） | 总览 §5.7：存在用户的部门不可被删除 | 新用户与部门 | 新增 `department_reference` 行 |
| INSERT 审计行 | 总览 §13：新增操作必须审计 | 变更快照 + 操作者 | 新增 `user_audit_log` 行（`action=create`） |

```sql
-- 唯一性校验（用户名忽略大小写）
SELECT 1 FROM chengke_crm.sys_user
WHERE tenant_id = :tenantId AND lower(username) = lower(:username);

SELECT 1 FROM chengke_crm.sys_user
WHERE tenant_id = :tenantId AND mobile = :mobile;
```

```sql
-- 新增用户（字段以总览 §7.2 为准；密码列写入不可逆化后的凭据）
INSERT INTO chengke_crm.sys_user (
    tenant_id, name, username, /* 密码凭据 */, avatar_url, mobile, email,
    department_id, role_id, status, force_password_change, remark,
    version, created_by, updated_by
) VALUES (
    :tenantId, :name, :username, :credential, :avatarUrl, :mobile, :email,
    :departmentId, :roleId, :status, :forcePasswordChange, :remark,
    1, :actorId, :actorId
);
```

```sql
-- 部门引用登记（总览 §5.7，调用既有函数）
SELECT chengke_crm.register_department_reference(
    :tenantId, :departmentId, 'user', :userId, true, '{}'::jsonb);
```

---

## 7. 响应结构

**`UserCreateResultVO`**（设计建议）

| 字段 | 类型 | 一定存在 | 空值规则 | 业务含义 | 来源 |
| --- | --- | --- | --- | --- | --- |
| `userId` | UUID | 是 | — | 新用户 ID | 系统生成 |
| `status` | Enum | 是 | — | 创建后状态 | 请求 + 默认值 |
| `updatedAt` | OffsetDateTime | 是 | — | 创建时间 | 系统 |

成功提示不得包含初始密码（已确认，PRD §8.2）。

---

## 8. 响应示例

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "userId": "<uuid>",
    "status": "active",
    "updatedAt": "<offset-datetime>"
  },
  "requestId": "<request-id>",
  "timestamp": "<offset-datetime>"
}
```

---

## 9. 成功 / 失败行为

**成功**：全部校验通过 → 事务写入用户 + 部门引用 + 审计 → 提交后授权版本 +1、缓存失效 → 返回 201。

**失败**（任一校验失败均不产生任何数据；数据库写失败整笔回滚）：

| 场景 | 错误码（总览 §11.2） | HTTP | 业务是否终止 |
| --- | --- | --- | --- |
| 参数缺失 / 格式错误 / 密码强度不足 | `USER_REQUEST_INVALID` | 400 | 是 |
| 无 `system:user:create` | `USER_OPERATION_FORBIDDEN` | 403 | 是 |
| 部门不存在 / 不可管理 | `USER_DEPARTMENT_UNAVAILABLE` | 404 | 是 |
| 部门已停用 | `USER_DEPARTMENT_UNAVAILABLE` | 422 | 是 |
| 角色不存在 / 不可授予 | `USER_ROLE_UNAVAILABLE` | 404 | 是 |
| 角色已停用 | `USER_ROLE_UNAVAILABLE` | 422 | 是 |
| 用户名重复 | `USER_USERNAME_DUPLICATE` | 409 | 是 |
| 手机号重复 | `USER_MOBILE_DUPLICATE` | 409 | 是 |
| 数据库写失败 | `USER_DATABASE_OPERATION_FAILED` | 500 | 是（整笔回滚） |

---

## 10. 事务与并发要点

- **原子组**（总览 §7.3、§9）：用户行 + 部门引用登记 + 审计行必须同时一致；任一步失败不得出现「用户已建但引用未登记」或「用户已建但无审计」。
- **提交后**（总览 §9.1）：授权版本 +1；失效部门树相关缓存。
- **并发**（总览 §10.1）：并发创建相同用户名 / 手机号时，由数据库唯一约束兜底，仅一个成功，另一个返回重复错误；相同请求重复提交同理由唯一性拦截，不引入幂等令牌（总览 §10.2）。

---

## 11. 验收标准（本 API）

- [ ] POST `/api/v1/system/users` 可正常调用，成功返回 201。
- [ ] 必填字段为空 / 格式错误 / 密码强度不足时 400，且不产生数据。
- [ ] 用户名不符合格式或已存在（忽略大小写）时拒绝。
- [ ] 手机号格式错误或已存在时拒绝。
- [ ] 部门或角色已停用、不存在或越权时拒绝（§5 决策表全覆盖）。
- [ ] 创建成功后：用户行、部门引用、审计行同时存在；审计不含密码。
- [ ] 可创建 `status=disabled` 的停用账号；`forcePasswordChange` 默认 `true`。
- [ ] 响应与成功提示不含初始密码。
- [ ] 邮箱以小写保存；用户名创建后不可修改（本接口不产生后续修改路径）。
- [ ] 提交后授权版本 +1。

---

## 12. 本接口相关待确认问题

无仅影响本接口的待确认问题；相关跨 API 问题见总览 §18（P0-2 角色数据、P0-8 可授予判定、P1-5 头像上传、P0-9 表名）。
