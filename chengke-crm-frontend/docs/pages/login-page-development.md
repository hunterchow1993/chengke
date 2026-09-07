# `LoginPage` 开发文档

| 文档信息 | 内容                                                                                                                      |
| -------- | ------------------------------------------------------------------------------------------------------------------------- |
| 源文件   | `src/features/auth/pages/login-page.tsx`                                                                                  |
| 类型     | 路由页面                                                                                                                  |
| 状态     | 当前实现基线；目标设计待实现                                                                                              |
| 文档版本 | v0.1                                                                                                                      |
| 更新日期 | 2026-08-12                                                                                                                |
| 主要读者 | 前端开发、后端联调开发、测试人员                                                                                          |
| 需求文档 | [`modules/login/docs/PRD-login-v0.3.md`](../../../modules/login/docs/PRD-login-v0.3.md)                                   |
| 页面设计 | [`modules/login/docs/UI-design-login-responsive-v0.1.md`](../../../modules/login/docs/UI-design-login-responsive-v0.1.md) |
| 设计系统 | [`design-system/chengke-crm/MASTER.md`](../../../design-system/chengke-crm/MASTER.md)                                     |
| 模块手册 | [`src/features/auth/README.md`](../../src/features/auth/README.md)                                                        |

> 本文同时记录代码事实和目标设计。标注为“当前实现”的内容可作为当前测试依据；标注为“目标设计”或“待实现”的内容不能视为已经交付。

## 1. 职责与非职责

### 1.1 当前职责

`LoginPage` 是 `/login` 的路由页面，负责：

- 组合左侧品牌展示区、右侧登录操作区和 `LoginForm`。
- 从 Redux 读取会话状态 `auth.status` 和授权首页 `permission.authorizedHome`。
- 维护登录请求的提交状态和页面级错误信息。
- 编排登录、Access Token 内存写入、授权上下文加载、Redux 更新和登录后跳转。
- 已认证用户重新访问 `/login` 时，使用 `replace` 跳转至当前授权首页。
- 通过 `Component = LoginPage` 提供 React Router 懒加载模块约定的导出。

### 1.2 非职责

`LoginPage` 不负责：

- 账号和密码的字段级校验、密码显隐及表单错误渲染；这些属于 `LoginForm`。
- 根据角色名称计算菜单、页面权限、按钮权限或数据范围；授权上下文由后端生成。
- 普通业务请求的 401 自动刷新、会话 single-flight 恢复和退出清理；这些属于认证服务、会话边界和管理端布局。
- 后端账号状态、密码、角色、租户、风控和接口权限的最终校验。
- 在 Web Storage 中持久化 Access Token、Refresh Token 或密码。

## 2. Props、回调、children 与路由出口

`LoginPage` 不接收 Props、回调或 children。

| 导出        | 类型               | 含义                                                        |
| ----------- | ------------------ | ----------------------------------------------------------- |
| `LoginPage` | React 函数组件     | 登录页面实现，可供测试或模块内部直接引用                    |
| `Component` | `typeof LoginPage` | React Router `lazy` 路由所需出口；当前为 `LoginPage` 的别名 |

路由在 `src/app/router/router.tsx` 中注册为 `/login`。根路径 `/` 使用 `replace` 重定向到 `/login`。

## 3. 状态与数据流

### 3.1 当前状态

| 状态             | 来源                              | 初始值             | 用途                                          |
| ---------------- | --------------------------------- | ------------------ | --------------------------------------------- |
| `status`         | Redux `auth.status`               | 由 auth slice 决定 | 判断是否已认证；已认证时离开登录页            |
| `authorizedHome` | Redux `permission.authorizedHome` | `/app/dashboard`   | 已认证用户访问登录页时的跳转目标              |
| `isSubmitting`   | 页面本地 state                    | `false`            | 锁定重复提交，并传给 `LoginForm` 展示加载状态 |
| `errorMessage`   | 页面本地 state                    | `null`             | 向 `LoginForm` 传递登录或响应解析错误         |

页面不把账号、密码或 Token 写入 Redux。表单值由 `LoginForm` 的 React Hook Form 管理；短期 Access Token 只写入模块级内存存储。

### 3.2 当前登录数据流

1. `LoginForm` 完成 Zod 校验和字段规范化后调用 `onSubmit(values)`。
2. `handleSubmit` 设置 `isSubmitting=true`，并清除上一次页面级错误。
3. 调用 `POST /api/v1/auth/login` 获取短期 Access Token。
4. 使用 `setAccessToken` 把 Token 写入当前 JavaScript 进程内存和本标签页 sessionStorage。
5. 调用 `GET /api/v1/auth/context` 获取后端权威授权上下文。
6. 使用 `applyAuthContext` 同步更新 auth 和 permission 两个 Redux slice。
7. 使用 `replace` 跳转 `context.authorizedHome`。
8. 失败时设置页面级错误；无论成功失败，最终恢复 `isSubmitting=false`。

菜单与权限加载规则：前端只消费后端返回的 `menus`、`permissionCodes`、`dataScopes` 和 `sensitiveFieldPermissions`，不根据角色名称或用户类型扩展授权结果。

## 4. 副作用与安全边界

| 副作用         | 当前行为                                              | 边界与要求                                                                   |
| -------------- | ----------------------------------------------------- | ---------------------------------------------------------------------------- |
| 登录接口       | 调用 `login(values)`                                  | 密码只用于本次请求，不得进入 URL、日志、埋点或持久化存储                     |
| Token 写入     | `setAccessToken(token)`                               | Access Token 写入内存和本标签页 Session Storage；不写入 Local Storage 或普通 Cookie |
| 授权上下文接口 | 调用 `fetchAuthContext()`                             | 后端是用户、租户、菜单、权限和数据范围的权威来源                             |
| Redux 更新     | `applyAuthContext(dispatch, context)`                 | auth 与 permission 必须作为同一次登录流程同步更新                            |
| 路由跳转       | `navigate(context.authorizedHome, { replace: true })` | 目标必须通过授权上下文 schema 的站内路径校验；目标授权仍由页面和后端接口校验 |

Access Token 由 `setAccessToken` 写入内存和本标签页 sessionStorage。页面 JavaScript 不使用 Cookie 投递凭证。前端菜单隐藏、登录重定向和页面门禁只改善用户体验，不能替代后端会话、租户、功能权限和数据权限校验。

## 5. 页面状态与交互

### 5.1 当前实现

| 场景                 | 当前表现                                             |
| -------------------- | ---------------------------------------------------- |
| 默认                 | 显示品牌区、帮助入口、登录标题、说明、表单和安全提示 |
| 提交中               | `LoginForm` 登录按钮禁用，显示旋转图标和“正在登录…”  |
| API 业务错误         | `ApiError.message` 传入表单顶部 `role="alert"` 区域  |
| 非预期或 schema 错误 | 显示“登录响应格式异常，请联系管理员”                 |
| 已认证访问           | 立即 `replace` 到 Redux 中的 `authorizedHome`        |
| 联系管理员           | 按钮可见，但没有事件、链接或其他副作用               |
| 隐私与安全           | 按钮可见，但没有事件、链接或其他副作用               |

页面当前没有独立空状态和无权限状态。账号能够通过认证但授权菜单为空时的专用受限状态尚未实现。

### 5.2 目标设计

- 登录页只保留一个主要操作“登录”，不提供注册、社交登录或公开营销入口。
- 登录成功后支持首次改密分支、保持登录策略、风险验证码和稳定错误码处理。
- 因会话失效返回登录页时显示明确原因，并在重新认证后安全恢复原目标路由。
- 账号或密码错误时使用统一文案，避免泄露账号是否存在。
- 网络异常、账号停用、无有效角色、验证码、限流和系统不可用应有可测试的独立行为。
- 联系管理员和隐私入口只有在目标地址或交互明确后才能视为可用功能。

## 6. 当前实现与目标设计差异

| 项目           | 当前实现                                        | 目标设计 / 处理要求                                                      |
| -------------- | ----------------------------------------------- | ------------------------------------------------------------------------ |
| 保持登录       | 没有 `rememberMe`                               | 由后端签发最长 7 天、可撤销的 Refresh 凭证；前端不得保存密码或长期 Token |
| 风控验证码     | 没有相关字段或分支                              | 后端要求时动态展示验证码，阈值不得写死在前端                             |
| 首次改密       | 登录响应只有 Access Token                       | 支持 `forcePasswordChange` 并阻止直接进入业务系统                        |
| 错误语义       | 展示 `ApiError.message`                         | 应优先依据稳定错误码决定行为，中文消息只负责展示                         |
| context 失败   | Token 已写入内存，失败分支不清理                | 应清除 Token，并按错误类型安全结束或撤销会话                             |
| 原目标路由     | 未读取 `location.state.from`                    | 校验站内路径和授权范围后优先恢复，否则进入授权首页                       |
| 无菜单用户     | 没有登录后的专用受限页面                        | 展示账号已认证但未获系统访问权限的明确状态                               |
| 布局           | 固定双栏；窄屏没有目标断点实现                  | 按响应式设计切换为浅色单列布局并支持自然滚动                             |
| 色彩           | 品牌区使用硬编码渐变、青色和透明效果            | 统一使用全局语义 Token，不建立登录页第二套色值                           |
| 字体           | 页面标题和品牌文字显式使用 `font-['Songti_SC']` | 字体属于全局 CSS 责任；页面正文、标题和表单控件统一继承设计系统字体栈    |
| 动效           | 使用全局 `animate-rise-in`、`animate-drift`     | 使用全局动效约束并支持 `prefers-reduced-motion`                          |
| 插图           | 跨目录引用 `modules/login/assets`               | 迁入前端 feature 资产或 `public`，避免构建边界外耦合                     |
| 帮助与合规入口 | 按钮没有行为                                    | 明确管理员联系配置和隐私页面后再接入                                     |

## 7. 全局 CSS 与字体约束

字体一致性是全局样式问题，不由 `LoginPage` 使用局部字体类修补。

当前 `src/app/styles/index.css` 在 `:root` 声明：

```css
font-family: Inter, 'PingFang SC', 'Microsoft YaHei', 'Noto Sans CJK SC', Arial, sans-serif;
```

并通过 `button, input { font: inherit; }` 让登录表单控件继承全局字体。后续实现必须遵循以下约束：

- `html`、`body`、React 根节点、标题、正文、按钮和输入控件使用同一全局字体策略。
- 登录页不得通过 `font-['Songti_SC']` 或局部 `font-family` 建立第二套中文字体语言；若品牌确需例外，必须先更新设计系统并说明字体加载、回退和适用范围。
- `input`、`button`、`select`、`textarea` 应统一继承全局字体，避免浏览器默认控件字体与正文不同。
- 浏览器自动填充、密码管理器填充、加载态和错误态不得改变字体、字号或控件尺寸。
- 字体修正应在全局 CSS 或共享基础组件完成；不要只在登录页覆盖，否则其他页面会继续不一致。
- 当前页面的 `Songti_SC` 覆盖属于已知差异，本轮只记录，不修改实现。

## 8. 无障碍与响应式

### 8.1 当前实现

- 使用 `<main>`、品牌 `<section>` 和登录 `<section>` 组织页面。
- 插图提供描述性 `alt`；纯装饰性背景元素不进入交互顺序。
- 表单标签、字段错误、密码显隐和错误播报由 `LoginForm` 负责。
- 页面使用 `min-h-screen` 和固定双栏网格；尚未实现 PRD 的平板、手机、横屏和软键盘规则。
- 全局 CSS 已对 `prefers-reduced-motion` 降低动画时长和次数。

### 8.2 目标验收

- 页面容器使用 `min-height: 100dvh`，短视口顶部对齐并允许自然滚动。
- 768px 以下取消左右分屏；手机宽度不横向溢出。
- 支持 320、360、375、414、768、1024、1280 和 1440px 关键视口。
- 顶部和底部内边距兼容安全区域；软键盘弹出后表单和登录按钮仍可访问。
- 键盘焦点顺序与视觉顺序一致；不可用的展示按钮在接入行为前应禁用、隐藏或清晰说明。
- 动画关闭时不影响内容出现、阅读顺序或提交功能。
- 字体在响应式断点、浏览器缩放、自动填充和系统字体回退时保持一致。

## 9. 测试基线与待补场景

### 9.1 当前覆盖

| 测试                          | 已覆盖行为                                                      |
| ----------------------------- | --------------------------------------------------------------- |
| `e2e/login.spec.ts`           | `/` 跳转 `/login`、页面标题可见、空表单客户端校验、控制台无错误 |
| `login-form.test.tsx`         | 由 `LoginForm` 覆盖字段提示和规范化提交                         |
| `login.schema.test.ts`        | 由 schema 覆盖账号 trim、空账号和短密码                         |
| `auth-context.schema.test.ts` | 授权上下文解析和外部授权首页拒绝                                |

### 9.2 待补页面测试

- [ ] 默认渲染品牌区、登录区和 `LoginForm`。
- [ ] 已认证状态访问 `/login` 时跳转 `authorizedHome`，并使用 `replace`。
- [ ] 成功登录依次调用 login、写入 Token、请求 context、更新 Redux、跳转授权首页。
- [ ] 登录接口失败时显示业务错误，不请求授权上下文。
- [ ] 登录响应格式错误时显示安全的通用错误。
- [ ] 授权上下文请求失败时展示错误；未来实现应验证内存 Token 已清理。
- [ ] 提交期间不产生重复登录请求。
- [ ] 后端返回不安全 `authorizedHome` 时 schema 拒绝且不跳转。
- [ ] 会话失效返回登录页和原目标路由恢复。
- [ ] 联系管理员、隐私与安全入口在未实现阶段没有误导性的可用行为。
- [ ] 关键视口、移动软键盘、横屏、安全区域和无横向溢出。
- [ ] `prefers-reduced-motion` 下页面内容完整可用。
- [ ] 页面标题、正文和表单控件使用统一全局字体，不出现局部 `Songti_SC` 覆盖。

接口场景优先使用测试文件内的局部 MSW handler，避免把页面特定响应隐藏在全局 fixture 中。

## 10. 依赖与已知限制

### 10.1 直接依赖

- Redux hooks 和 `applyAuthContext`。
- React Router 的 `Navigate`、`useNavigate` 和 lazy `Component` 约定。
- auth API 的 `login`、`fetchAuthContext`。
- 内存 Token 存储 `setAccessToken`。
- `LoginForm`、Lucide 图标和登录插图资产。
- 全局 CSS、Button/Input 基础组件及设计系统 Token。

### 10.2 已知限制

- 后端登录和 `/auth/context` 完整授权实现不在当前前端源码中，端到端联调仍依赖后端能力。
- 登录成功但 context 失败时可能残留 Access Token。
- 未实现普通业务请求 401 自动刷新、首次改密、保持登录、验证码和原目标恢复。
- 三个帮助或合规按钮尚无行为。
- 固定双栏、局部宋体、硬编码色值和跨目录插图与目标设计不一致。
- 当前页面级成功和失败流程缺少组件测试与 MSW 测试。

## 11. 修改完成检查表

- [ ] 页面职责、路由出口、状态和副作用仍与源码一致。
- [ ] 登录接口与授权上下文接口的调用顺序已验证。
- [ ] Access Token 仍只保存在内存，Refresh Token 仍由 HttpOnly Cookie 管理。
- [ ] auth 与 permission Redux 状态同步更新。
- [ ] 授权首页经过站内路径校验，前端跳转不代替后端鉴权。
- [ ] 登录页只编排请求，字段校验仍由 `LoginForm`/schema 负责。
- [ ] 全局字体栈覆盖正文、标题和原生表单控件，没有页面级字体分叉。
- [ ] 加载、错误、重复提交和已认证跳转测试已更新。
- [ ] 关键响应式视口、键盘、焦点、减少动效和软键盘已检查。
- [ ] 当前实现与目标设计的差异没有被误写成已交付能力。

## 12. 变更记录

| 版本 | 日期       | 说明                                                                                                       |
| ---- | ---------- | ---------------------------------------------------------------------------------------------------------- |
| v0.1 | 2026-08-12 | 根据当前代码、登录 PRD、响应式设计和全局设计系统建立 `LoginPage` 独立开发基线；补充全局 CSS 字体一致性约束 |
