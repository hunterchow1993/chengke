# 登录 API Spec

| 文档信息 | 内容 |
| --- | --- |
| 文档类型 | API Spec（一个 API 一个 Spec） |
| 所属模块 | 登录与会话认证 |
| API | 登录 · POST `/api/v1/auth/login`（URL 为设计建议，总览 §4.1、§18 P0-1） |
| 文档版本 | v0.1 |
| 更新日期 | 2026-09-05 |
| 模块总览 Spec | [LoginController.spec.md](./LoginController.spec.md) |
| 状态标记 | **已确认** / **设计建议** / **待确认**（同总览） |

---

## 1. 接口概述

- **用途与调用场景**：登录页提交账号、密码、是否保持登录，以及（若风控要求）验证码，完成认证并创建会话（登录 PRD §8.1、§11.1）。
- **功能权限 / 数据权限**：无业务权限码；**公开接口**（总览 §8.1）。须进入认证白名单（总览 §4.3、§18 P0-1）。
- **成功状态码**：**待确认**；设计建议 HTTP 200（总览 §4.3）。
- **是否修改数据**：是。成功时新增会话（及可选刷新凭证）、写成功审计、清除该账号连续失败计数；失败时写失败审计，并可能累加失败计数 / 进入需验证码或锁定状态（总览 §7.3）。
- **引用章节清单**：
  - 总览 §4.2 全局约定（登录结果字段、错误枚举、账号 / 密码 / 验证码格式）
  - 总览 §4.3 关键设计决策（HTTP 映射、凭证投递）
  - 总览 §5.1 账号识别、§5.2 防枚举、§5.3 风控、§5.4 会话创建、§5.5 改密闸门、§5.7 凭证与日志安全
  - 总览 §6.1–§6.3 账号 / 改密 / 会话状态
  - 总览 §7 `sys_user` 只读消费；会话存储待确认
  - 总览 §8 权限、§9 事务、§10 并发、§11.2 错误码总表、§13 审计

---

## 2. 请求示例

仅包含登录 PRD §11.1 已确认字段。未触发验证码时不传验证码字段。

无需验证码：

```json
{
  "account": "zhouba",
  "password": "<user-input>",
  "rememberMe": false
}
```

需要验证码时（`captchaCode` / `captchaToken` 条件必填，登录 PRD §11.1）：

```json
{
  "account": "13800008006",
  "password": "<user-input>",
  "rememberMe": true,
  "captchaCode": "ab12",
  "captchaToken": "<server-issued-token>"
}
```

**不接受**：`tenantId`、操作者字段、明文要持久化的密码副本。`confirmPassword` 不属于本接口。

---

## 3. 请求参数（Controller 层契约）

| 参数 | 类型 | 来源 | 必填 | 默认值 | 校验规则 | 业务含义 |
| --- | --- | --- | --- | --- | --- | --- |
| `account` | string | Body | 是 | — | 去除首尾空格后非空；最大 32 字符；不自动改变字母大小写（登录 PRD §6.1） | 用户名或 11 位中国大陆手机号 |
| `password` | string | Body | 是 | — | 8–32 字符；不 trim 内部空格；不自动格式化（登录 PRD §6.1） | 仅用于本次认证 |
| `rememberMe` | boolean | Body | 是（登录 PRD §11.1） | UI 默认未选中（登录 PRD §6.1） | 布尔 | 是否申请最长 1 天保持登录 |
| `captchaCode` | string | Body | 条件必填 | — | 4–6 位；不区分大小写（登录 PRD §6.2） | 图形验证码 |
| `captchaToken` | string | Body | 条件必填 | — | 服务端签发的一次性凭证 | 与验证码绑定 |
| 租户 | UUID | 系统上下文 | 是（业务上） | — | 解析方式 **待确认**（总览 §18 P0-5） | 限定 `sys_user` 查询 |
| 客户端 IP | string | 系统上下文 | 是（审计 / 限流） | — | — | 登录 PRD §9.4、§9.1 |
| 设备或浏览器摘要 | string | 系统上下文 | 是（审计） | — | 采集字段 **待确认** | 登录 PRD §9.4 |

`rememberMe` 缺失时按 `false` 还是参数错误：**待确认**（总览未升为跨 API 项；本接口：API 表为必填，UI 可缺省）。

---

## 4. 参数校验与业务校验

**参数校验**

- `account`、`password` 必须存在；`account` 去首尾空格后非空且 ≤32；`password` 长度 8–32。
- `rememberMe` 必须为布尔（若确认为必填）。
- 当本次请求**已经被判定需要验证码**时，`captchaCode` 与 `captchaToken` 必须存在；`captchaCode` 为 4–6 位。
- 不需要验证码时提交验证码：**待确认**（忽略或拒绝）。

**业务校验**（顺序见 §5，不得用密码验证前的结果区分「账号不存在」）

- 验证码要求、验证码正确性、临时锁定、IP 限流。
- 租户内账号可识别且密码匹配。
- 账号 `status` 为 `active`（仅在密码通过之后使用 `ACCOUNT_DISABLED`）。
- 用户当前角色有效（登录 PRD §8.1 步骤 5）。

---

## 5. 业务逻辑（Service 层）

输入 → 条件 → 业务判断 → 数据变化 → 输出。

```text
提交登录
↓
参数格式是否合法？否 → 拒绝，不创建会话（HTTP / 码见总览 §4.3、§11.1）
↓
IP 是否超过频率限制？是 → RATE_LIMITED，不创建会话（阈值待确认，总览 §5.3、§18 P0-4）
↓
解析租户（待确认，总览 §18 P0-5）
↓
按总览 §5.1 识别账号（用户名或手机号）
↓
是否处于临时锁定？→ 见下方决策表与总览 §18 P0-9
↓
本次是否必须验证码？
  必须且未提供 → CAPTCHA_REQUIRED，requiresCaptcha=true
  必须且 token/码无效或过期 → CAPTCHA_INVALID（须刷新验证码），写失败审计
  必须且通过 → 验证码一次性作废，继续
↓
密码是否匹配？（账号不存在视为不匹配）
  否 → INVALID_CREDENTIALS；累加失败计数（计数主体待确认）；更新 requiresCaptcha / 可能锁定；写失败审计；不创建会话
↓
账号是否 disabled？是 → ACCOUNT_DISABLED，不创建会话，写失败审计
↓
角色是否有效？否 → ROLE_UNAVAILABLE，不创建会话，写失败审计
↓
创建会话；rememberMe=true 时签发最长 1 天可撤销刷新凭证（总览 §5.4）
↓
清除当前账号连续失败计数（登录 PRD §9.1，已确认）
↓
写成功审计（总览 §13）
↓
返回成功结果：success=true，errorCode=null（空值规则待确认），requiresCaptcha=false，forcePasswordChange=用户当前值，authContextRequired=true
```

**登录业务失败决策表**

| 密码已通过 | 账号状态 | 角色有效 | 需验证码且未通过 | 锁定中 | errorCode | 创建会话 |
| --- | --- | --- | --- | --- | --- | --- |
| 否 | — | — | 否 | **待确认是否先返回锁定** | `INVALID_CREDENTIALS` | 否 |
| — | — | — | 是（未提供） | — | `CAPTCHA_REQUIRED` | 否 |
| — | — | — | 是（码无效） | — | `CAPTCHA_INVALID` | 否 |
| 是 | `disabled` | — | 否 | 否 | `ACCOUNT_DISABLED` | 否 |
| 是 | `active` | 否 | 否 | 否 | `ROLE_UNAVAILABLE` | 否 |
| 是 | `active` | 是 | 否 | 否 | （成功，无 errorCode） | 是 |

建议基线的「5 次验证码 / 10 次锁定」数值本身 **待确认**（总览 §5.3），本表不把具体次数写成已确认规则。

**保持登录**

| `rememberMe` | 会话 |
| --- | --- |
| `false` | 常规会话；关闭浏览器后时长待确认（总览 §18 P0-10） |
| `true` | 最长 1 天可撤销刷新凭证（登录 PRD §7.4） |

凭证放入 Cookie 还是别处：总览 §18 P0-2 投递方式已确认——访问令牌只出现在 JSON `data.accessToken`，后续请求使用 `Authorization: Bearer`。本接口不得在 JSON 中返回密码、哈希或完整刷新凭证（总览 §5.7）。

系统异常 → `SYSTEM_UNAVAILABLE`，不创建会话。

---

## 6. 数据操作与 SQL（DAO 层）

SQL 只表达访问意图；表 / 列以已确认结构为限，不虚构会话表 SQL。

| 操作 | 目的 | 数据来源 | 数据变化 |
| --- | --- | --- | --- |
| 按租户 + 用户名或手机号读取用户 | 账号识别（总览 §5.1） | `account`、租户 | 无 |
| 读取密码凭据并做哈希比对 | 认证 | 用户行 + 请求 `password` | 无（内存比对） |
| 读取 `status`、`force_password_change`、`role_id` | 状态与改密闸门、角色校验入口 | 用户行 | 无 |
| 读取角色是否有效 | 登录 PRD §8.1 步骤 5 | `role_id` | 角色表 **待确认**（总览 §18 P0-6），无表则无法编造 JOIN |
| 读写失败计数 / 锁定 | 风控（总览 §5.3） | 账号 / IP（主体待确认） | 失败则累加；成功则清除该账号失败计数 |
| 校验并作废验证码挑战 | 一次性、60 秒 | `captchaToken`、`captchaCode` | 挑战转为已使用或已失效；存储 **待确认** |
| 写入会话 / 刷新凭证 | 登录成功 | 用户 ID、租户、`rememberMe` | 新增有效会话；存储 **待确认** |
| 写入登录审计 | 登录 PRD §9.4 | 结果、IP、UA 摘要、用户 ID（无法识别则不记） | 新增审计；禁止写密码 / 验证码原文 / 完整凭证 |

**已确认表上的查询语义示例**（`sys_user` 列引用用户管理总览 §7.2）：

```sql
-- 用户名：租户内忽略大小写匹配
SELECT id, tenant_id, username, mobile, /* 密码凭据列 */, status,
       force_password_change, role_id, name, avatar_url
  FROM sys_user
 WHERE tenant_id = :tenantId
   AND lower(username) = lower(:account);

-- 11 位数字按手机号匹配
SELECT id, tenant_id, username, mobile, /* 密码凭据列 */, status,
       force_password_change, role_id, name, avatar_url
  FROM sys_user
 WHERE tenant_id = :tenantId
   AND mobile = :account;
```

两条查找均未命中时**不得**对客户端区分，统一走 `INVALID_CREDENTIALS`。密码比对不在 SQL 中明文比较。会话 INSERT **待确认**。

---

## 7. 响应结构

业务 `data` 仅含登录 PRD §11.2 已确认字段。Token 不在该表中（PRD 未列入登录结果字段）。

| 字段 | 类型 | 是否必定存在 | 空值规则 | 业务含义 | 来源 |
| --- | --- | --- | --- | --- | --- |
| `success` | boolean | 是 | — | 是否认证成功 | 业务判断 |
| `errorCode` | enum nullable | 失败时应有 | 成功时 nullable；成功是否省略 **待确认** | 稳定失败编码 | §11.2 登录枚举 |
| `requiresCaptcha` | boolean | 是 | — | 下次提交是否需要验证码 | 失败计数 / 风控状态 |
| `forcePasswordChange` | boolean | **待确认** 失败时是否仍返回 | 成功时为用户当前开关 | 是否必须先改密 | `sys_user.force_password_change` |
| `authContextRequired` | boolean | 登录成功后固定 `true` | 失败时是否返回 **待确认** | 是否需要拉授权上下文 | 常量规则（登录 PRD §11.2） |

外层 `ApiResponse` 见总览 §4.2。`message` 不得含内部异常；前端以 `errorCode` 映射文案（登录 PRD §11.2）。

页面提示对照（供前端，不作为必须返回的中文字段）：登录 PRD §8.3。

---

## 8. 响应示例

成功（无需改密）：

```json
{
  "success": true,
  "errorCode": null,
  "requiresCaptcha": false,
  "forcePasswordChange": false,
  "authContextRequired": true
}
```

成功（必须先改密）：

```json
{
  "success": true,
  "errorCode": null,
  "requiresCaptcha": false,
  "forcePasswordChange": true,
  "authContextRequired": true
}
```

失败（账号或密码错误，且下次需要验证码）：

```json
{
  "success": false,
  "errorCode": "INVALID_CREDENTIALS",
  "requiresCaptcha": true
}
```

上例未写出的 `forcePasswordChange` / `authContextRequired` 在失败场景是否出现：**待确认**，故不补全。

---

## 9. 成功 / 失败行为

**成功**

请求成功 → 会话（及可选刷新凭证）已创建 → 失败计数已清除 → 审计成功 → 返回 §7；`forcePasswordChange=true` 时仍算认证成功，但不得进入业务系统（总览 §5.5）。

**失败**

| 场景 | 数据是否变化 | 错误码 | 业务是否终止 |
| --- | --- | --- | --- |
| 参数非法 | 否（或仅审计 **待确认**） | 参数校验路径；本表无新码（总览 §11.2） | 是 |
| 账号或密码错误 | 失败计数可能增加；失败审计 | `INVALID_CREDENTIALS` | 是 |
| 需验证码未提供 | 失败审计（是否计数 **待确认**） | `CAPTCHA_REQUIRED` | 是 |
| 验证码错误 / 失效 | 失败审计；验证码作废 | `CAPTCHA_INVALID` | 是 |
| 账号停用 | 失败审计 | `ACCOUNT_DISABLED` | 是 |
| 无有效角色 | 失败审计 | `ROLE_UNAVAILABLE` | 是 |
| 临时锁定 | 失败审计 | `ACCOUNT_TEMP_LOCKED` | 是 |
| 过于频繁 | 失败审计 | `RATE_LIMITED` | 是 |
| 系统异常 | 不得留下可用会话 | `SYSTEM_UNAVAILABLE` | 是 |

失败时**不创建会话**。当前请求终止。

---

## 10. 事务与并发要点

- **原子组**（总览 §7.3、§9）：成功路径上「可用会话 + 成功审计 + 清除失败计数」必须一致；失败路径上不得产生可用会话。
- **并发**（总览 §10）：成功必须清计数；同时登录设备数未确认前不得把「踢掉其他设备」或「无限设备」当作已确认行为。
- 凭证投递已确认（总览 §4.3）：本接口只在 JSON 返回 `accessToken`；刷新消费见总览 §18 P0-3，本接口不单独发明刷新字段。

---

## 11. 验收标准（本 API）

- [ ] POST `/api/v1/auth/login`（或评审后的 URL）可调用。
- [ ] 请求字段与 §3 / §4 一致；密码不出现在日志与响应中。
- [ ] 用户名与手机号均可在凭据正确、账号正常、角色有效时 `success=true`。
- [ ] 登录成功 `authContextRequired` 为 `true`；`forcePasswordChange` 与用户数据一致。
- [ ] `rememberMe=true` 签发最长 1 天可撤销刷新凭证；`false` 为常规会话。
- [ ] `INVALID_CREDENTIALS` 不区分账号是否存在。
- [ ] `ACCOUNT_DISABLED` / `ROLE_UNAVAILABLE` 仅出现在密码通过之后，且不创建会话。
- [ ] `requiresCaptcha` 与服务端风控状态一致，阈值来自后端配置而非前端写死。
- [ ] 成功清除该账号连续失败计数。
- [ ] 审计含结果分类、IP、设备摘要；无密码 / 验证码 / 完整凭证。

---

## 12. 本接口相关待确认问题

| 优先级 | 问题 | 为什么需要确认 | 影响 |
| --- | --- | --- | --- |
| P1 | `rememberMe` 缺失是 400 还是默认 `false`？ | §11.1 必填 vs §6.1 可选 | §3 |
| P1 | 失败响应是否仍返回 `forcePasswordChange`、`authContextRequired`？成功时 `errorCode` 是 `null` 还是省略？ | PRD 表未写必定存在规则 | §7 / §8 |
| P1 | 不需要验证码时多余提交验证码如何处理？验证码失败是否计入连续失败？ | 影响计数与锁定 | §5 |
| P2 | 设备 / 浏览器摘要取哪些 Header？ | 审计字段 | §6 |
