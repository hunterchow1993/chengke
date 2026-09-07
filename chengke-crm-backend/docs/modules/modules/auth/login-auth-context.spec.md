# 获取授权上下文 API Spec

| 文档信息 | 内容 |
| --- | --- |
| 文档类型 | API Spec（一个 API 一个 Spec） |
| 所属模块 | 登录与会话认证 |
| API | 获取授权上下文 · GET `/api/v1/auth/context`（URL 为设计建议，总览 §4.1、§18 P0-1） |
| 文档版本 | v0.2 |
| 更新日期 | 2026-09-05 |
| 模块总览 Spec | [LoginController.spec.md](./LoginController.spec.md) |
| 状态标记 | **已确认** / **设计建议** / **待确认**（同总览） |

---

## 1. 接口概述

- **用途与调用场景**：在登录成功且无需首次改密后、首次改密完成后、浏览器刷新或恢复仍有效的会话时、后端提示权限已变更后，获取最新菜单树、功能权限码、数据范围摘要、默认首页与权限版本（登录 PRD §12.3、§12.4）。
- **功能权限 / 数据权限**：无单独 `system:*` 才能调用本接口；需要**有效会话**。返回的菜单与权限码本身即授权结果。强制改密未完成时是否允许调用 **待确认**（总览 §4.3、§18 P1-1）。
- **成功状态码**：**待确认**；设计建议 HTTP 200。空菜单是业务结果（`NO_AVAILABLE_MENU`），不一定等于 HTTP 失败（映射 **待确认**）。
- **是否修改数据**：**不改变**用户账号、角色、会话有效性。可写授权异常日志（如 `NO_AVAILABLE_MENU`，登录 PRD §12.10）。
- **引用章节清单**：
  - 总览 §4.2 授权上下文字段、MenuNode
  - 总览 §5.6 授权计算顺序与空菜单、权限变更
  - 总览 §6.1 用户状态、§8 权限
  - 总览 §11.2 `AUTH_CONTEXT_LOAD_FAILED` / `NO_AVAILABLE_MENU` / `SESSION_REVOKED`
  - 总览 §18 P0-6、P0-8、P0-11

若评审决定登录响应合并授权上下文（总览 §18 P0-11），必须先改总览 §4.1 再改/删除本文件。

---

## 2. 请求示例

PRD 未定义 Query。会话凭证携带方式：请求头 `Authorization: Bearer <accessToken>`（总览 §4.3，已确认）。

```
GET /api/v1/auth/context
```

不接受用 Query 覆盖用户 ID 或租户。

---

## 3. 请求参数（Controller 层契约）

| 参数 | 类型 | 来源 | 必填 | 默认值 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- | --- |
| 当前用户 | UUID | 当前登录会话（JWT `sub` 或等价） | 是 | — | 合法 UUID | 授权主体 |
| 租户 | UUID | 当前登录会话（`tenant_id`） | 是 | — | 合法 UUID | 隔离 |
| （无 Body） | — | — | — | — | — | GET |

---

## 4. 参数校验与业务校验

**参数校验**：无已确认 Query 业务字段。

**业务校验**

- 会话有效；否则 `AUTHENTICATION_REQUIRED` 或 `SESSION_REVOKED`。
- 用户状态为 `active`（登录 PRD §12.2 步骤 2）；停用应已撤销会话，若仍读到 `disabled` → 不得返回业务菜单。
- 必须存在当前有效角色（步骤 3）；否则不能按成功授权上下文进入系统。
- 计算过程失败 → `AUTH_CONTEXT_LOAD_FAILED`，不得降级为「全量菜单」（登录 PRD §12.6、§14.5）。

---

## 5. 业务逻辑（Service 层）

计算顺序必须与总览 §5.6（登录 PRD §12.2）一致，此处只引用不改写：

获取当前会话用户 → 校验 `active` → 获取有效角色 → 功能权限编码 → 筛选菜单与路由 → 删除无子菜单的空目录 → 计算 `dataScope` 摘要 → 计算 `homeRoute` → 生成 `permissionVersion` → 返回完整授权上下文。

超级管理员同样走该流程（总览 §5.6）。

| 条件 | 是否返回完整 menus | 系统行为 |
| --- | --- | --- |
| 会话无效 | 否 | `SESSION_REVOKED` / 401 |
| 用户非 active 或角色无效 | 否 | 不得当作已授权；与登录侧 `ACCOUNT_DISABLED` / `ROLE_UNAVAILABLE` 的编码是否复用 **待确认** |
| 计算成功且 menus 非空 | 是 | 返回 §7 字段 |
| 计算成功且 menus 为空（删除空目录后） | 否（空树） | 记录 `NO_AVAILABLE_MENU`；不进入空白后台；不补默认菜单 |
| 菜单或权限数据源失败 | 否 | `AUTH_CONTEXT_LOAD_FAILED`；允许前端重试 |

`homeRoute` 为空时前端使用第一个可访问 `page`（登录 PRD §12.9）；后端仍返回可空 `homeRoute`。`homeRoute` 的配置来源 **待确认**（总览 §18，登录 PRD §15-13）。

`dataScope` 仅供界面说明；实际列表过滤由各业务 API 执行（登录 PRD §12.8）。对象内部字段 **待确认**（总览 §18 P0-8）。

`role.name` 仅展示，前端不得用角色名推断菜单（登录 PRD §12.4）。

返回的 `permissionCodes` 须与后续写入 JWT `scope` 的权限计算同源（总览 §9）。本接口是否同时刷新 Token **待确认**。

---

## 6. 数据操作与 SQL（DAO 层）

| 操作 | 目的 | 数据来源 | 数据变化 |
| --- | --- | --- | --- |
| SELECT 用户 | 步骤 1–2：身份与 `active` | 会话 userId + tenant | 无 |
| 读取角色、权限、菜单 | 步骤 3–6 | `role_id`、权限配置 | 角色 / 菜单表 **待确认**（总览 §18 P0-6），**无 JOIN SQL** |
| 读取 permissionVersion | 步骤 9 | Redis 租户授权版本 `chengke:authorization:version:{tenantId}` | 本 GET 只读，不写版本 |
| 可选 INSERT 授权异常日志 | 空菜单 / 加载失败 | 用户、编码 | 仅日志；账号不变 |

```sql
SELECT id, tenant_id, name, avatar_url, status, role_id
  FROM sys_user
 WHERE id = :userId
   AND tenant_id = :tenantId;
```

不返回、不查询密码凭据列。

---

## 7. 响应结构

字段以总览 §4.2（登录 PRD §12.4 / §12.5）为唯一事实来源，下表与总览逐字一致，不增字段。

| 字段 | 类型 | 是否必定存在 | 空值规则 | 业务含义 | 来源 |
| --- | --- | --- | --- | --- | --- |
| `user` | object | 是（成功计算时） | — | 当前用户基础信息，不含密码 | 组装 |
| `user.id` | string | 是 | — | 当前用户 ID | `sys_user.id` |
| `user.name` | string | 是 | — | 当前用户姓名 | `sys_user.name` |
| `user.avatarUrl` | string nullable | 是 | 可空 | 头像 | `sys_user.avatar_url` |
| `role` | object | 是（有有效角色时） | 无角色时整体是否省略 **待确认** | 角色摘要 | 角色模块 |
| `role.id` | string | 是 | — | 角色 ID | 角色 |
| `role.name` | string | 是 | — | 仅展示 | 角色 |
| `menus` | MenuNode[] | 是 | 可为空数组 | 已排序左侧菜单树 | 后端筛选 |
| `permissionCodes` | string[] | 是 | 可为空数组 | 页面和按钮功能权限编码 | 角色权限 |
| `dataScope` | object | 是 | **内部字段待确认** | 数据权限摘要，仅界面说明 | 角色数据范围 |
| `homeRoute` | string nullable | 是 | 可空 | 默认首页路由 | 后端计算 |
| `permissionVersion` | string | 是 | 非空（格式待确认） | 判断前端缓存是否过期 | 后端生成 |

**MenuNode**（每节点，总览 §4.2）：`id`，`parentId`（根为空），`type`（`directory`/`page`），`name`，`routeName`（目录可空），`path`（目录可空），`iconKey`（可空），`permissionCode`（可空），`order`，`children`。

禁止：远程组件代码、脚本地址、任意图标 URL、密码、完整会话凭证。

`user` 不包含手机号等用户管理敏感字段（PRD 本接口只列了 id/name/avatarUrl）。

---

## 8. 响应示例

只展示已确认字段。`dataScope` 内部不编造键。`menus` 用一个 `page` 节点示意结构。

```json
{
  "user": {
    "id": "<uuid>",
    "name": "周八",
    "avatarUrl": null
  },
  "role": {
    "id": "<uuid>",
    "name": "<role-display-name>"
  },
  "menus": [
    {
      "id": "<menu-id>",
      "parentId": null,
      "type": "page",
      "name": "系统管理",
      "routeName": "<route-name>",
      "path": "<path>",
      "iconKey": "<icon-key>",
      "permissionCode": "system:user:view",
      "order": 1,
      "children": []
    }
  ],
  "permissionCodes": ["system:user:view"],
  "dataScope": {},
  "homeRoute": null,
  "permissionVersion": "<version>"
}
```

`dataScope: {}` 仅表示「对象存在但键未确认」，**不是**已确认的空对象业务含义；实现不得把空对象当成「无范围」的正式语义，除非评审确认。

空菜单成功计算示例（`menus` 为空数组；HTTP 与 `NO_AVAILABLE_MENU` 如何组合 **待确认**）：

```json
{
  "user": {
    "id": "<uuid>",
    "name": "周八",
    "avatarUrl": null
  },
  "role": {
    "id": "<uuid>",
    "name": "<role-display-name>"
  },
  "menus": [],
  "permissionCodes": [],
  "dataScope": {},
  "homeRoute": null,
  "permissionVersion": "<version>"
}
```

---

## 9. 成功 / 失败行为

**成功**：按 §5 算出上下文 → **不改变**账号与会话 → 返回 §7。空菜单时前端进「暂无可用功能」，后端已记 `NO_AVAILABLE_MENU`。

**失败**

| 场景 | 数据是否变化 | 错误码 | 业务是否终止 |
| --- | --- | --- | --- |
| 无会话 / 已撤销 | 否 | `AUTHENTICATION_REQUIRED` / `SESSION_REVOKED` | 是 |
| 加载失败 | 可记失败日志 | `AUTH_CONTEXT_LOAD_FAILED` | 是；允许重试；禁止全量菜单降级 |
| 系统异常 | 否 | `SYSTEM_UNAVAILABLE` | 是 |

`ROUTE_FORBIDDEN` / `ACTION_FORBIDDEN` / `PERMISSION_VERSION_CHANGED` 主要由后续业务访问触发（总览 §4.2），**不是**本 GET 的返回。本 GET 在会话有效时按当前权限计算快照；版本已变也要成功返回新 `permissionVersion` 与新 `menus`，以便前端消化 409。

---

## 10. 事务与并发要点

- 只读计算；无用户行事务。日志失败不得改写授权结果口径（是否因日志失败而改返回 **待确认**，默认日志失败不应变成全量菜单）。
- 并发两次 GET 各自按当前权限计算；权限刚收回时，下一次业务请求须 409，本 GET 须能读到新版本（总览 §5.6）。
- 可安全重试（总览 §10.2）。

---

## 11. 验收标准（本 API）

- [ ] 有效会话可获取上下文；无效会话被拒绝。
- [ ] 返回字段不超过 §7；无密码；无远程脚本 / 任意 URL 图标。
- [ ] `menus` 已去掉空目录；顺序由后端决定。
- [ ] 超级管理员也走同一计算，无「全部菜单」捷径。
- [ ] 空菜单不补默认节点，并有 `NO_AVAILABLE_MENU` 日志。
- [ ] 加载失败可重试且不降级全量菜单。
- [ ] `permissionCodes` 与后端业务接口校验使用同一套编码。
- [ ] **不改变**用户与会话数据。
- [ ] 权限版本已变时本 GET 仍返回 200 新快照，不返回 409 `PERMISSION_VERSION_CHANGED`。

---

## 12. 本接口相关待确认问题

| 优先级 | 问题 | 为什么需要确认 | 影响 |
| --- | --- | --- | --- |
| P0 | `dataScope` 的键；空菜单时 HTTP vs `NO_AVAILABLE_MENU` | 前端无权限页与重试 | §7、§9 |
| P1 | 强制改密未完成是否 拒绝本接口 | 闸门 | §1、§4 |
| P1 | 无有效角色时的错误码是否复用 `ROLE_UNAVAILABLE` | 与登录枚举一致性 | §9 |
| P2 | `permissionVersion` 字符串格式 | 缓存比较 | §7 |

---

## 变更记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| v0.2 | 2026-09-05 | 确认本 GET 只读 Redis 租户授权版本；版本已变时仍返回新快照，不返回 409 |
| v0.1 | 2026-09-05 | 首版 |
