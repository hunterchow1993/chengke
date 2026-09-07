# 部门管理 Backend Spec

| 文档信息 | 内容 |
| --- | --- |
| 文档类型 | Backend Spec（基于代码考古生成） |
| 文档版本 | v2.0 |
| 更新日期 | 2026-09-03 |
| 对应模块 | 部门管理 |
| API 基础路径 | `/api/v1/system/departments` |
|  legend | **已确认** = 代码与数据库行为一致；**当前代码行为** = 已实现待产品确认；**待确认** = 无法从代码/需求推断；**疑似 Bug** = 代码矛盾 |

---

## 1. 功能名称

**部门管理（Department Management）**

---

## 2. 功能目标

### 2.1 业务问题

租户需要在系统内维护「集团 → 部门」组织树，并支撑：

- 按层级浏览、搜索、查看部门详情。
- 在权限与数据范围内新增、编辑、移动、启用/停用、软删除部门。
- 结构性变更（移动、启停、删除）前预览影响范围。
- 为前端展示当前操作者对每个节点的可操作能力。
- 组织变更后，相关缓存与授权版本失效，避免组织视图与权限滞后。

### 2.2 使用方

- 持有部门管理功能权限的租户内管理员（具体角色名称 **待确认**）。
- 前端部门管理页面（**待确认**具体页面清单）。

### 2.3 期望结果

- 组织树数据与租户隔离、层级深度、启停、删除保护规则一致。
- 越权访问不暴露资源存在性（范围外返回「不存在」）。
- 并发编辑通过版本号冲突检测避免覆盖。

---

## 3. API 总览

| # | API 名称 | Method | URL | 成功 HTTP |
| --- | --- | --- | --- | --- |
| 1 | 查询部门树 | GET | `/api/v1/system/departments/tree` | 200 |
| 2 | 查询直接下级 | GET | `/api/v1/system/departments/{id}/children` | 200 |
| 3 | 搜索部门 | GET | `/api/v1/system/departments/search` | 200 |
| 4 | 查询部门详情 | GET | `/api/v1/system/departments/{id}` | 200 |
| 5 | 查询可移动父节点 | GET | `/api/v1/system/departments/{id}/movable-parents` | 200 |
| 6 | 查询负责人候选 | GET | `/api/v1/system/departments/leader-candidates` | 200 |
| 7 | 新增部门 | POST | `/api/v1/system/departments` | **201** |
| 8 | 编辑部门 | PUT | `/api/v1/system/departments/{id}` | 200 |
| 9 | 预览移动影响 | POST | `/api/v1/system/departments/{id}/move-preview` | 200 |
| 10 | 移动部门 | POST | `/api/v1/system/departments/{id}/move` | 200 |
| 11 | 预览启停影响 | POST | `/api/v1/system/departments/{id}/status-preview` | 200 |
| 12 | 启用或停用部门 | PATCH | `/api/v1/system/departments/{id}/status` | 200 |
| 13 | 软删除部门 | DELETE | `/api/v1/system/departments/{id}` | 200 |

**全局约定（已确认）**

- 认证：Bearer JWT；租户与操作者从认证上下文解析，请求不接受 `tenantId`/操作者字段。
- 响应外壳：`ApiResponse<T>`（见 §6）。
- 树/下级接口返回**扁平节点列表**，非嵌套 JSON 树（**当前代码行为**）。

---

## 4. API 请求参数

### 4.1 查询部门树

| 参数 | 类型 | 必填 | 来源 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- |
| `rootId` | UUID | 否 | Query | UUID 格式 | 局部根；传则仅返回该节点及直接下级 |
| `selectedId` | UUID | 否 | Query | UUID 格式 | **待确认**；当前接收但未参与过滤 |

**功能权限**：`system:department:view`

---

### 4.2 查询直接下级

| 参数 | 类型 | 必填 | 来源 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | UUID | 是 | Path | UUID | 父部门 ID |
| `includeDisabled` | Boolean | 否 | Query | 默认 `false` | 是否包含停用节点 |

**功能权限**：`system:department:view`

---

### 4.3 搜索部门

| 参数 | 类型 | 必填 | 来源 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- |
| `keyword` | String | 是 | Query | 非空，≤50 | 名称或编码关键词 |
| `page` | Integer | 否 | Query | ≥1，默认 1 | 页码 |
| `pageSize` | Integer | 否 | Query | 1–100，默认 20 | 每页条数 |

**功能权限**：`system:department:view`

---

### 4.4 查询部门详情

| 参数 | 类型 | 必填 | 来源 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | UUID | 是 | Path | UUID | 部门 ID |

**功能权限**：`system:department:view`

---

### 4.5 查询可移动父节点

| 参数 | 类型 | 必填 | 来源 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | UUID | 是 | Path | UUID | 待移动部门 ID |
| `keyword` | String | 否 | Query | ≤50 | 名称/编码过滤 |
| `limit` | Integer | 否 | Query | 1–100，默认 50 | 候选数量上限 |

**功能权限**：`system:department:move`

---

### 4.6 查询负责人候选

| 参数 | 类型 | 必填 | 来源 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- |
| `keyword` | String | 否 | Query | ≤50 | 用户名/显示名过滤 |
| `page` | Integer | 否 | Query | ≥1，默认 1 | 页码 |
| `pageSize` | Integer | 否 | Query | 1–100，默认 20 | 每页条数 |

**功能权限**：`system:department:create` 或 `system:department:update`

---

### 4.7 新增部门

| 参数 | 类型 | 必填 | 来源 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- |
| `parentId` | UUID | 否 | Body | UUID；建根时为 null | 上级部门 |
| `name` | String | 是 | Body | 2–50 字符 | 部门名称 |
| `code` | String | 是 | Body | `^[a-z][a-z0-9_]{1,49}$` | 部门编码 |
| `leaderUserId` | UUID | 否 | Body | UUID | 负责人 |
| `sortOrder` | Integer | 否 | Body | 0–9999，默认 100 | 同级排序 |
| `status` | Enum | 否 | Body | `active`/`disabled`，默认 `active` | 初始状态 |
| `remark` | String | 否 | Body | ≤200 | 备注 |

**功能权限**：`system:department:create`

---

### 4.8 编辑部门

| 参数 | 类型 | 必填 | 来源 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | UUID | 是 | Path | UUID | 部门 ID |
| `name` | String | 是 | Body | 2–50 | 名称 |
| `leaderUserId` | UUID | 否 | Body | UUID | 负责人 |
| `sortOrder` | Integer | 是 | Body | 0–9999 | 排序 |
| `remark` | String | 否 | Body | ≤200 | 备注 |
| `version` | Integer | 是 | Body | 正整数 | 乐观锁版本 |
| `reason` | String | 否 | Body | ≤500 | 变更原因 |

**不可通过本接口修改（已确认）**：`code`、`parentId`、`status`、`nodeType`

**功能权限**：`system:department:update`

---

### 4.9 预览移动影响

| 参数 | 类型 | 必填 | 来源 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | UUID | 是 | Path | UUID | 待移动部门 |
| `newParentId` | UUID | 是 | Body | UUID | 目标上级 |
| `version` | Integer | 是 | Body | 正整数 | 当前版本 |

**功能权限**：`system:department:move`

---

### 4.10 移动部门

| 参数 | 类型 | 必填 | 来源 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | UUID | 是 | Path | UUID | 待移动部门 |
| `newParentId` | UUID | 是 | Body | UUID | 目标上级 |
| `version` | Integer | 是 | Body | 正整数 | 当前版本 |
| `previewToken` | String | 是 | Body | 非空 | 预览令牌 |
| `reason` | String | 否 | Body | ≤500 | 原因 |

**功能权限**：`system:department:move`

---

### 4.11 预览启停影响

| 参数 | 类型 | 必填 | 来源 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | UUID | 是 | Path | UUID | 目标部门 |
| `targetStatus` | Enum | 是 | Body | `active`/`disabled` | 目标状态 |
| `cascade` | Boolean | 是 | Body | — | 是否级联后代 |
| `version` | Integer | 是 | Body | 正整数 | 当前版本 |

**功能权限**：`system:department:status`

---

### 4.12 启用或停用部门

| 参数 | 类型 | 必填 | 来源 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | UUID | 是 | Path | UUID | 目标部门 |
| `targetStatus` | Enum | 是 | Body | `active`/`disabled` | 目标状态 |
| `cascade` | Boolean | 是 | Body | — | 是否级联 |
| `version` | Integer | 是 | Body | 正整数 | 当前版本 |
| `previewToken` | String | 是 | Body | 非空 | 预览令牌 |
| `reason` | String | 否 | Body | ≤500 | 原因 |

**功能权限**：`system:department:status`

---

### 4.13 软删除部门

| 参数 | 类型 | 必填 | 来源 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | UUID | 是 | Path | UUID | 目标部门 |
| `version` | Integer | 是 | Query | 正整数 | 当前版本 |
| `previewToken` | String | 否 | Query | — | **疑似 Bug**：参数存在但未校验 |
| `reason` | String | 否 | Query | ≤500 | 原因 |

**功能权限**：`system:department:delete`

---

## 5. API 请求示例

> 仅展示已确认字段；UUID 用占位符表示，不虚构具体 ID。

### 5.1 新增部门

```json
{
  "parentId": "<uuid>",
  "name": "华东销售部",
  "code": "east_sales",
  "leaderUserId": "<uuid>",
  "sortOrder": 100,
  "status": "active",
  "remark": "备注文本"
}
```

创建集团根时 `parentId` 为 `null`（**当前代码行为**）。

### 5.2 编辑部门

```json
{
  "name": "华东销售部",
  "leaderUserId": "<uuid>",
  "sortOrder": 100,
  "remark": "备注",
  "version": 1,
  "reason": "调整负责人"
}
```

### 5.3 移动部门（执行）

```json
{
  "newParentId": "<uuid>",
  "version": 2,
  "previewToken": "<preview-token>",
  "reason": "组织调整"
}
```

### 5.4 启停部门（执行）

```json
{
  "targetStatus": "disabled",
  "cascade": true,
  "version": 2,
  "previewToken": "<preview-token>",
  "reason": "部门撤销"
}
```

---

## 6. API 响应结构

### 6.1 统一外壳 `ApiResponse<T>`

| 字段 | 类型 | 一定存在 | 业务含义 | 来源 |
| --- | --- | --- | --- | --- |
| `code` | String | 是 | 业务码；成功为 `OK` | 系统 |
| `message` | String | 是 | 展示消息 | 系统 |
| `data` | T | 成功时 | 业务数据 | 业务 |
| `requestId` | String | 是 | 请求追踪 ID | 认证上下文/MDC |
| `timestamp` | OffsetDateTime | 是 | 响应时间 | 系统 |

### 6.2 树节点 `DepartmentTreeNodeVO`

| 字段 | 类型 | 一定存在 | 业务含义 | 来源 |
| --- | --- | --- | --- | --- |
| `id` | UUID | 是 | 部门 ID | DB |
| `parentId` | UUID | 否 | 上级 ID；根为 null | DB |
| `name` | String | 是 | 名称 | DB |
| `code` | String | 是 | 编码 | DB |
| `nodeType` | Enum | 是 | `group`/`department` | DB |
| `status` | Enum | 是 | `active`/`disabled` | DB |
| `depth` | int | 是 | 层级（根=0） | DB |
| `hasChildren` | boolean | 是 | 是否有直接下级 | 计算 |
| `directMemberCount` | Integer | 否 | 直属成员数；无 member:view 权限时为 null | DB 聚合 |
| `readOnlyAncestor` | boolean | 是 | 可见但不可管理的祖先 | 计算 |
| `capabilities` | Object | 是 | 操作能力（见下） | 计算 |

**capabilities 字段**

| 字段 | 类型 | 业务含义 |
| --- | --- | --- |
| `canView` | boolean | 固定 true（**当前代码行为**） |
| `canCreateChild` | boolean | 可管理 + active + 有 create 权限 |
| `canEdit` | boolean | 可管理 + 有 update 权限 |
| `canMove` | boolean | 可管理 + 非 group + 有 move 权限 |
| `canChangeStatus` | boolean | 可管理 + 非 group + 有 status 权限 |
| `canDelete` | boolean | 可管理 + 非 group + 有 delete 权限 |
| `canViewMembers` | boolean | 有 member:view 权限 |
| `disabledReasons` | Map | 禁用原因，如 `scope`/`root` |

### 6.3 详情 `DepartmentDetailVO`

在树节点字段基础上增加：

| 字段 | 类型 | 一定存在 | 业务含义 | 来源 |
| --- | --- | --- | --- | --- |
| `path` | Array | 是 | 根到当前节点路径 | DB 闭包 |
| `leader` | Object | 否 | 负责人；**当前代码行为**仅 userId 有值 | DB |
| `sortOrder` | int | 是 | 排序 | DB |
| `remark` | String | 否 | 备注 | DB |
| `version` | int | 是 | 乐观锁版本 | DB |
| `directChildCount` | Integer | 否 | **当前代码行为**固定 null | — |
| `descendantCount` | int | 是 | 全部后代数 | DB 聚合 |
| `roleReferenceCount` | int | 是 | 角色引用数 | DB 聚合 |
| `businessReferenceCount` | int | 是 | 受保护业务引用数 | DB 聚合 |
| `hasHiddenReferences` | boolean | 是 | **当前代码行为**固定 false | — |
| `deleteBlockers` | Array | 是 | 删除阻塞项 | 计算 |
| `createdAt` | OffsetDateTime | 是 | 创建时间 | DB |
| `updatedAt` | OffsetDateTime | 是 | 更新时间 | DB |
| `updatedBy` | Object | 是 | 最近更新者；**当前代码行为** name 为 null | DB |
| `capabilities` | Object | 是 | 同树节点 | 计算 |

### 6.4 写操作 `DepartmentOperationVO`

| 字段 | 类型 | 业务含义 |
| --- | --- | --- |
| `departmentId` | UUID | 目标部门 |
| `version` | int | 操作后版本 |
| `status` | Enum | 操作后状态 |
| `updatedAt` | OffsetDateTime | 操作时间 |
| `affectedDepartmentCount` | int | 受影响节点数 |
| `message` | String | 结果描述 |

### 6.5 影响预览 `DepartmentImpactPreviewVO`

| 字段 | 类型 | 业务含义 |
| --- | --- | --- |
| `previewToken` | String | 一次性预览令牌 |
| `expiresAt` | OffsetDateTime | 失效时间（签发后 5 分钟） |
| `currentPath` | Array | 当前路径 |
| `targetPath` | Array | 目标路径（启停时为空列表） |
| `affectedDepartmentCount` | int | 受影响部门数（含自身） |
| `affectedDirectUserCount` | Integer | 直属成员数 |
| `affectedRoleCount` | Integer | 角色引用数 |
| `affectedBusinessReferenceCount` | Integer | 业务引用数 |
| `hasHiddenReferences` | boolean | 是否存在不可见引用 |
| `riskLevel` | String | `low`/`medium`/`high` |
| `requiresConfirmation` | boolean | 固定 true |
| `messages` | Array | 风险提示文案 |

**riskLevel 规则（已确认）**

| 条件 | riskLevel |
| --- | --- |
| 影响部门数 > 100 或 存在业务引用 | `high` |
| 影响部门数 > 10 或 存在角色引用 | `medium` |
| 其他 | `low` |

### 6.6 分页 `PageVO<T>`

| 字段 | 类型 | 业务含义 |
| --- | --- | --- |
| `items` | Array | 当前页数据 |
| `page` | int | 页码 |
| `pageSize` | int | 每页大小 |
| `total` | long | 总条数 |

---

## 7. API 响应示例

### 7.1 成功（查询树，结构示意）

```json
{
  "code": "OK",
  "message": "success",
  "data": [
    {
      "id": "<uuid>",
      "parentId": null,
      "name": "集团名称",
      "code": "group_code",
      "nodeType": "group",
      "status": "active",
      "depth": 0,
      "hasChildren": true,
      "directMemberCount": null,
      "readOnlyAncestor": true,
      "capabilities": {
        "canView": true,
        "canCreateChild": false,
        "canEdit": false,
        "canMove": false,
        "canChangeStatus": false,
        "canDelete": false,
        "canViewMembers": false,
        "disabledReasons": {
          "scope": "节点仅用于路径定位",
          "root": "集团根节点受保护"
        }
      }
    }
  ],
  "requestId": "<request-id>",
  "timestamp": "<offset-datetime>"
}
```

### 7.2 成功（新增，HTTP 201）

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "departmentId": "<uuid>",
    "version": 1,
    "status": "active",
    "updatedAt": "<offset-datetime>",
    "affectedDepartmentCount": 1,
    "message": "部门创建成功"
  },
  "requestId": "<request-id>",
  "timestamp": "<offset-datetime>"
}
```

### 7.3 失败（版本冲突）

```json
{
  "code": "DEPARTMENT_VERSION_CONFLICT",
  "message": "部门已被其他管理员修改",
  "data": null,
  "requestId": "<request-id>",
  "timestamp": "<offset-datetime>"
}
```

---

## 8. API 成功 / 失败行为

### 8.1 读接口（以查询树为例）

```
请求 + JWT
→ 功能权限校验（view）
→ 解析操作者与可见范围
→ 读取租户全部未删除部门
→ 按可见范围与 rootId 过滤
→ 组装节点与 capabilities
→ 返回 200 + 扁平列表
```

**失败**：无 view 权限 → 403；参数校验失败 → 400。

### 8.2 移动部门（执行）

```
请求 + JWT
→ 功能权限校验（move）
→ 数据范围校验（源、目标可管理）
→ 版本一致校验
→ 移动规则校验（无环、深度、父 active）
→ 消费 previewToken（一次性、5 分钟内、参数一致）
→ 原子更新子树 parent/path/closure + 审计
→ 发布变更事件
→ 事务提交后清缓存 + 授权版本 +1
→ 返回 200 + 新版本与影响数
```

**失败**：任一步不满足 → 对应错误码；**数据库写失败 → 整笔回滚，previewToken 已消费需重新预览（当前代码行为）**。

### 8.3 软删除

```
请求 + JWT
→ 功能权限 + 数据范围 + 版本
→ 删除阻塞校验（后代/成员/角色/业务引用/group 根）
→ 软删除（deleted_at + status=disabled）+ 审计
→ 提交后清缓存 + 授权版本 +1
→ 返回 200
```

**失败**：存在阻塞 → 422；**previewToken 未校验（疑似 Bug）**。

---

## 9. 业务规则

格式：**触发条件 → 业务判断 → 系统行为**

### 9.1 可见范围（读）

**触发** → 持有 view 权限

**判断**
- 全量部门管理员 → 全部未删除部门可见
- 否则 → 可见 = 可管理部门 + 其全部祖先

**行为** → 返回可见节点；范围外 → 404「部门不存在」

### 9.2 新增部门

**触发** → 持有 create 权限

**判断** → 见 §10 决策表

**行为** → 写 department + closure + audit；返回新 ID、version=1

### 9.3 编辑

**触发** → 持有 update；目标可管理；非 group

**判断** → version 一致

**行为** → 更新 name/leader/sortOrder/remark；version+1

### 9.4 移动

**触发** → 持有 move；预览后执行

**判断** → 无环、深度≤10、目标 active、非 group、token 有效

**行为** → 原子移动整棵子树；授权版本 +1

### 9.5 启停

**触发** → 持有 status；预览后执行

**判断** → 见 §16 状态表

**行为** → 更新 status；cascade 时同步后代；授权版本 +1

### 9.6 软删除

**触发** → 持有 delete；目标可管理；无阻塞

**行为** → deleted_at 写入、status=disabled；闭包保留；授权版本 +1

---

## 10. 条件与决策表

### 10.1 新增部门

| parentId | 全量部门管理范围 | 父状态 | 父.depth+1 | 允许 | 系统行为 |
| --- | --- | --- | --- | --- | --- |
| null | 否 | — | — | 否 | 403 |
| null | 是 | — | — | 是 | 创建 group 根 |
| 有值 | 不可管理父 | — | — | 否 | 404 |
| 有值 | 可管理 | 非 active | — | 否 | 422 上级停用 |
| 有值 | 可管理 | active | >10 | 否 | 422 层级超限 |
| 有值 | 可管理 | active | ≤10 | 是 | 创建 department |

### 10.2 移动目标父节点候选

| 条件 | 允许为候选 |
| --- | --- |
| 操作者可管理 | 是 |
| status=active | 是 |
| 非待移动节点自身 | 是 |
| 非待移动节点后代 | 是 |
| 移动后深度≤10 | 是 |
| 关键词匹配（若提供） | 是 |

### 10.3 删除阻塞

| 条件 | 阻塞删除 |
| --- | --- |
| nodeType=group | 是 |
| 存在任意后代部门 | 是 |
| 存在 user 引用 | 是 |
| 存在 role 引用 | 是 |
| 存在受保护业务引用 | 是 |
| 以上均无 | 否，允许软删除 |

### 10.4 预览令牌消费（移动/启停）

| 条件 | 允许执行 |
| --- | --- |
| token 存在且未消费 | 是 |
| 未过期（5 分钟） | 是 |
| 租户/操作者/部门/目标/version/cascade 与预览一致 | 是 |
| 任一不满足 | 否 → 409 |

---

## 11. 数据模型

### 11.1 业务对象

| 对象 | 说明 | 关键字段 | 关系 |
| --- | --- | --- | --- |
| 部门 | 组织树节点 | id, name, code, nodeType, status, depth, version | 父子自关联 |
| 部门闭包 | 祖先-后代关系 | ancestor_id, descendant_id, distance | 多对多 via 部门 |
| 部门引用 | 跨模块引用 | reference_type, reference_key, protected | 多对一 部门 |
| 部门审计 | 变更记录 | action, before/after | 多对一 部门 |
| 预览令牌 | 移动/启停前置确认 | token, 绑定参数, 过期时间 | 无 DB 表（Redis） |

---

## 12. 数据库表与字段

Schema：`chengke_crm`

### 12.1 `department`

| 字段 | 类型 | 可空 | 默认 | 枚举/约束 | 业务含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | uuid | 否 | gen_random_uuid | PK | 部门 ID |
| `tenant_id` | uuid | 否 | — | | 租户 |
| `parent_id` | uuid | 是 | — | group 为 null | 上级 |
| `name` | varchar(50) | 否 | — | 2–50，同级唯一 | 名称 |
| `code` | varchar(50) | 否 | — | 租户唯一，正则 | 编码 |
| `node_type` | varchar(20) | 否 | — | group/department | 节点类型 |
| `leader_user_id` | uuid | 是 | — | | 负责人 |
| `sort_order` | int | 否 | 100 | 0–9999 | 排序 |
| `status` | varchar(20) | 否 | active | active/disabled | 状态 |
| `remark` | varchar(200) | 是 | — | | 备注 |
| `path_ids` | uuid[] | 否 | — | 长度=depth+1 | 物化路径 |
| `depth` | int | 否 | — | 0–10 | 层级 |
| `version` | int | 否 | 1 | >0 | 乐观锁 |
| `deleted_at` | timestamptz | 是 | — | | 软删时间 |
| `deleted_by` | uuid | 是 | — | | 软删操作者 |

### 12.2 `department_closure`

| 字段 | 类型 | 业务含义 |
| --- | --- | --- |
| `tenant_id`, `ancestor_id`, `descendant_id` | uuid | 闭包主键 |
| `distance` | int | 0=自身，>0=后代 |

### 12.3 `department_reference`

| 字段 | 类型 | 业务含义 |
| --- | --- | --- |
| `reference_type` | varchar | user/role/业务类型 |
| `reference_key` | varchar | 外部实体键 |
| `protected` | boolean | true 时阻止删除 |
| `metadata` | jsonb | 扩展信息 |

### 12.4 `department_audit_log`

| 字段 | 类型 | 业务含义 |
| --- | --- | --- |
| `action` | varchar | create/update/move/enable/disable/soft_delete |
| `before_data`, `after_data` | jsonb | 变更快照 |
| `affected_department_count` | int | 影响节点数 |

---

## 13. 数据库操作

| 操作 | 目的 | 触发 API | 数据变化 |
| --- | --- | --- | --- |
| SELECT 全部/单条部门 | 树、搜索、详情 | 读接口 | 无 |
| SELECT 路径 | 展示面包屑 | 详情、搜索、预览 | 无 |
| SELECT 影响计数 | 预览、删除阻塞 | 详情、预览、删除 | 无 |
| SELECT 负责人候选 | 选负责人 | leader-candidates | 无 |
| INSERT 部门+闭包 | 新增 | POST 创建 | department, closure, audit |
| UPDATE 基本信息 | 编辑 | PUT | department, audit |
| UPDATE 子树+闭包 | 移动 | POST move | 多行 department, closure, audit |
| UPDATE status | 启停 | PATCH status | 多行 department, audit |
| UPDATE 软删标记 | 删除 | DELETE | department, audit |

写操作通过 PostgreSQL 函数原子执行（**技术约束：当前实现**）。

---

## 14. SQL / SQL 语义示例

> 表达数据访问意图，非强制实现方式。

### 14.1 查询租户全部未删除部门

```sql
SELECT id, tenant_id, parent_id, name, code, node_type, leader_user_id,
       sort_order, status, remark, depth, version, updated_by, created_at, updated_at
FROM chengke_crm.department
WHERE tenant_id = :tenantId AND deleted_at IS NULL
ORDER BY path_ids, sort_order, created_at, id;
```

### 14.2 查询路径

```sql
SELECT a.id, a.name, a.depth
FROM chengke_crm.department_closure c
JOIN chengke_crm.department a ON a.tenant_id = c.tenant_id AND a.id = c.ancestor_id
WHERE c.tenant_id = :tenantId AND c.descendant_id = :departmentId AND a.deleted_at IS NULL
ORDER BY c.distance DESC;
```

### 14.3 影响计数

```sql
-- 后代数
SELECT count(*) FROM chengke_crm.department_closure c
JOIN chengke_crm.department d ON d.tenant_id = c.tenant_id AND d.id = c.descendant_id
WHERE c.tenant_id = :tenantId AND c.ancestor_id = :departmentId
  AND c.distance > 0 AND d.deleted_at IS NULL;

-- 引用数（同 department_reference 表按 type 分类统计）
```

### 14.4 写操作（语义）

```sql
SELECT chengke_crm.create_department(...);
SELECT chengke_crm.update_department(...);
SELECT chengke_crm.move_department(...);
SELECT chengke_crm.set_department_status(...);
SELECT chengke_crm.soft_delete_department(...);
```

---

## 15. SQL 与业务规则关系

| 业务条件 | 数据访问 |
| --- | --- |
| 用户打开部门树 | 查租户全部未删除部门 → 内存按可见范围过滤 |
| 用户搜索关键词 | 同上 → 内存匹配 name/code → 内存分页 |
| 用户查看详情 | 查单条 + 路径 + 引用计数 |
| 用户新增子部门 | create_department；父须 active，depth 合法 |
| 用户移动部门 | move_department；重建子树 path 与 closure |
| 用户停用且 cascade | set_department_status 更新自身及后代 |
| 用户删除叶子部门 | soft_delete_department；须无阻塞引用 |

---

## 16. 状态流转

### 16.1 `status`（active / disabled）

| 当前状态 | 操作/条件 | 新状态 | 允许 | 业务说明 |
| --- | --- | --- | --- | --- |
| active | 停用，无 active 后代 | disabled | 是 | 仅自身 |
| active | 停用，有 active 后代，cascade=true | disabled | 是 | 含后代 |
| active | 停用，有 active 后代，cascade=false | — | 否 | 须级联或先处理下级 |
| disabled | 启用，全部祖先 active | active | 是 | 仅自身 |
| disabled | 启用，存在 disabled 祖先 | — | 否 | 上级未启用 |
| 相同 | 重复启停 | 不变 | 是 | affected=0 |
| group | 任意启停 | — | 否 | 根保护 |
| 任意 | 软删除 | disabled + deleted_at | 是* | *须满足删除条件 |

**不允许**：软删除后通过本模块恢复（**待确认**是否存在恢复流程）。

---

## 17. 权限

### 17.1 功能权限码（已确认）

| 权限码 | 操作 |
| --- | --- |
| `system:department:view` | 读 |
| `system:department:create` | 新增 |
| `system:department:update` | 编辑 |
| `system:department:move` | 移动 |
| `system:department:status` | 启停 |
| `system:department:delete` | 软删 |
| `system:department:member:view` | 看成员数 |
| `system:department:manage:all` | 全量组织数据范围 |

### 17.2 数据权限

| 操作者 | 读 | 写 | 建 group 根 |
| --- | --- | --- | --- |
| 全量范围 | 全部 | 全部 | 可以 |
| 范围管理员 | 范围+祖先（祖先只读） | 仅范围内 | 不可以 |
| 范围外 | 404 | 404 | 不可以 |

### 17.3 双层校验（已确认）

HTTP `@PreAuthorize` + Application 层 `hasPermission` 二次校验。

---

## 18. 事务与数据一致性

### 18.1 必须原子性的操作

| 操作 | 必须同时一致 |
| --- | --- |
| 新增 | department + closure + audit |
| 编辑 | department 字段 + version + audit |
| 移动 | 子树全部 department + closure + audit |
| 级联启停 | 全部受影响 department.status + audit |
| 软删除 | deleted 标记 + status + version + audit |

任一步失败 → 全部回滚，不得出现闭包与 department 不一致。

### 18.2 提交后一致性（非同一 DB 事务）

| 触发 | 必须发生 |
| --- | --- |
| 任意写成功 | 清空部门相关缓存 |
| move/status/delete 成功 | 租户授权版本 +1 |

---

## 19. 并发控制

| 机制 | 必须保证 |
| --- | --- |
| 乐观锁 `version` | 并发写时后提交者收到冲突，不覆盖先提交数据 |
| 预览令牌一次性消费 | 同一 token 不可重复执行 |
| DB 行锁（写函数内 FOR UPDATE） | 同一部门并发写串行校验 version |

**待确认**：previewToken 已消费但 DB 写失败时，用户须重新预览（当前无补偿）。

---

## 20. 异常处理

| 异常场景 | 系统行为 | 错误码 | HTTP | 分类 |
| --- | --- | --- | --- | --- |
| 参数校验失败 | 拒绝，返回字段错误 | `DEPARTMENT_REQUEST_INVALID` | 400 | 参数异常 |
| 无功能权限 | 拒绝 | `DEPARTMENT_OPERATION_FORBIDDEN` | 403 | 权限异常 |
| @PreAuthorize 失败 | 拒绝 | **待确认** | 403 | 权限异常 |
| 范围外/不存在 | 404，不暴露越权 | `DEPARTMENT_NOT_FOUND` | 404 | 数据异常 |
| version 不一致 | 拒绝 | `DEPARTMENT_VERSION_CONFLICT` | 409 | 数据异常 |
| 预览 token 无效 | 拒绝 | `DEPARTMENT_PREVIEW_EXPIRED` / `MISMATCH` | 409 | 业务异常 |
| 名称/编码重复 | 拒绝 | `DEPARTMENT_SIBLING_NAME_DUPLICATE` / `CODE_DUPLICATE` | 409 | 数据异常 |
| 树规则违反 | 拒绝 | 见 CK001–CK010 映射 | 422 | 业务/状态异常 |
| 领域规则（统一文案） | 拒绝 | 同上 | 422 | 业务异常 |
| 未映射 DB 错误 | 拒绝 | `DEPARTMENT_DATABASE_OPERATION_FAILED` | 500 | 系统异常 |

**领域异常 HTTP 422 统一 message**：「部门操作不符合业务规则」。

---

## 21. 外部依赖

| 依赖 | 用途 | 调用时机 | 失败行为 | 影响事务 |
| --- | --- | --- | --- | --- |
| PostgreSQL | 主数据与存储函数 | 全部读写 | 500/映射业务码 | 是，回滚 |
| Redis | 预览令牌；授权版本 | 预览/执行/提交后 | **待确认** | 预览：是；版本：提交后 |
| JWT 认证 | 租户/操作者/权限 | 每个请求 | 401 | 否 |
| Spring Cache | 读缓存 | 读（**待确认**是否已用于读路径） | 提交后须 clear | 提交后 |
| 用户目录 | 负责人候选 | leader-candidates | **待确认** | 否 |

无 MQ、无第三方 API、无文件服务。

---

## 22. 接口 / 数据库 / 外部系统调用链

### 22.1 移动部门（完整链）

```
PATCH/POST move
→ JWT 认证
→ @PreAuthorize(move)
→ 解析 CurrentActor
→ 读源/目标部门（SELECT）
→ 版本与规则校验
→ 消费 Redis previewToken
→ move_department() 原子写
→ 发布 DepartmentChangedEvent
→ COMMIT
→ AFTER_COMMIT: clear cache + Redis auth version INCR
→ 200 ApiResponse
```

### 22.2 查询树

```
GET tree
→ JWT + view 权限
→ SELECT 全部部门
→ 内存 visibleIds 过滤
→ 内存 rootId 过滤
→ 200 扁平列表
```

---

## 23. 数据一致性规则

| 规则 | 说明 |
| --- | --- |
| parent ↔ path_ids ↔ depth ↔ closure | 移动后必须一致 |
| version | 写前客户端 version 须等于 DB |
| 同级名称唯一 | 未删除同级节点内 |
| 租户编码唯一 | 未删除节点 |
| 每租户唯一 group 根 | 未删除 |
| 软删除后 | 常规查询不可见；closure 保留 |
| 缓存 | 写成功后必须失效；**不允许**长期脏读 |
| 授权版本 | move/status/delete 后必须递增 |

---

## 24. 缓存规则

| 缓存名 | 何时清除 | 何时读取 |
| --- | --- | --- |
| `department-tree` | 任意部门写提交后 | **待确认** |
| `department-detail` | 同上 | **待确认** |
| `department-leader-candidates` | 同上 | **待确认** |
| `role-department-options` | 同上 | **待确认** |

清除方式：整库 clear（**当前代码行为**）。CacheManager 缺失时抛异常，不静默跳过。

---

## 25. 验收标准

### API

- [ ] 13 个接口 Method/URL 与 Spec 一致
- [ ] 参数必填与校验规则一致
- [ ] 成功/失败响应为 ApiResponse 结构
- [ ] 创建返回 201，其余写操作 200

### 业务规则

- [ ] 可见范围：范围+祖先；范围外 404
- [ ] 新增/移动/启停/删除决策表场景全覆盖
- [ ] group 根不可移动/启停/删
- [ ] 移动/启停须先预览再执行

### 数据库

- [ ] 写操作原子；失败无中间态
- [ ] 软删除后 selectAll 不可见
- [ ] 审计 action 与操作一致

### 权限

- [ ] 无权限 403；范围外 404
- [ ] 非全量管理员不可建 group 根

### 事务与一致性

- [ ] version 冲突 409
- [ ] move/status/delete 后授权版本 +1
- [ ] 写后四类缓存清空

### 异常

- [ ] 400/403/404/409/422/500 场景与 §20 一致

### 并发

- [ ] 双写 version 冲突
- [ ] previewToken 不可重复使用

---

## 26. 不包含范围

- 用户入离职、调岗、账号状态
- 角色权限配置界面与 API
- 业务数据批量改归属部门
- 第三方通讯录同步
- 组织版本回滚、拖拽移动 UI
- 软删除恢复 API
- `register_department_reference` / `unregister_department_reference` 的 HTTP 暴露（仅 DB 函数存在）
- 登录/Token 签发

---

## 27. 当前实现中的问题

| 类型 | 当前情况 | 是否符合正式业务规则 | 状态 |
| --- | --- | --- | --- |
| 当前代码行为 | `selectedId` 请求参数未参与查询 | 待确认 | 待确认 |
| 疑似 Bug | DELETE 接口有 `previewToken` 但未校验 | 与移动/启停不一致 | 疑似 Bug |
| 当前代码行为 | 详情 `leader.displayName`、`updatedBy.name` 等为 null | 待确认 | 待确认 |
| 当前代码行为 | `hasHiddenReferences` 固定 false | 待确认 | 待确认 |
| 当前代码行为 | `directChildCount` 固定 null | 待确认 | 待确认 |
| 当前代码行为 | 树接口返回扁平列表 | 待确认是否为产品预期 | 待确认 |
| 疑似 Bug / 待确认 | @PreAuthorize 失败响应格式可能非 ApiResponse | 待确认 | 待确认 |
| 当前代码行为 | token 消费后 DB 失败须重新预览 | 待确认产品是否接受 | 待确认 |

---

## 28. 待确认问题

### P0

| 问题 | 原因 | 影响 |
| --- | --- | --- |
| 删除是否必须 previewToken | 参数存在但未实现 | API 契约、前端流程 |
| `selectedId` 业务用途 | 代码未使用 | 树定位 API |
| 软删除后是否可恢复 | 无恢复 API | 数据生命周期 |
| 组织变更与 JWT 部门范围同步 | 不读授权版本 | 数据权限准确性 |

### P1

| 问题 | 原因 | 影响 |
| --- | --- | --- |
| 详情/负责人/操作者名称填充 | VO 字段为 null | 前端展示 |
| `hasHiddenReferences` 是否应实现 | SQL 硬编码 false | 预览风险准确性 |
| 读路径是否已接 Spring Cache | 仅见 clear 逻辑 | 性能与一致性 |
| Security 403 响应格式 | 无统一 Handler | 前端错误处理 |

### P2

| 问题 | 原因 | 影响 |
| --- | --- | --- |
| 搜索/负责候选内存分页 | 先全量再切片 | 大数据量性能 |
| 移动到新父相同 | DB 直接返回原 version | 是否需提示「无变更」 |

---

## 29. 变更记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| v1.0 | 2026-09-02 | 首版业务 Spec |
| v2.0 | 2026-09-03 | 按 Backend Spec 全量模板重构：补 JSON/SQL 示例、调用链、缓存、实现问题与 P0/P1/P2 待确认 |

---

## 30. 最终检查清单

- [x] 13 个 API 全覆盖
- [x] 请求/响应结构与示例仅含已确认字段
- [x] 业务规则用「条件→判断→行为」
- [x] 决策表覆盖新增/移动/删除/令牌
- [x] 数据库表、字段、操作、SQL 语义
- [x] 状态流转表
- [x] 权限与数据范围
- [x] 事务、并发、缓存、一致性
- [x] 异常分类
- [x] 不包含范围
- [x] 实现问题与待确认分离
- [x] 未将疑似 Bug 写成正确规则
