# 澄客 CRM Web 设计系统

| 文档信息 | 内容 |
| --- | --- |
| 设计系统版本 | v1.1 |
| 文档状态 | 统一基线 |
| 更新日期 | 2026-08-07 |
| 适用范围 | 澄客 CRM Web 管理端全部模块 |
| 技术映射 | React、shadcn/ui、Tailwind CSS 4、Lucide React |
| 当前视觉基准 | [用户管理组织树 v9](../../modules/user-management/prototypes/user-management-org-tree-v9.png) |

## 1. 文档职责

本文档是澄客 CRM 视觉与交互规范的唯一来源，统一管理颜色、字体、间距、圆角、阴影、图标、组件状态、导航框架、响应式和无障碍规则。

模块 PRD 只描述业务需求；页面设计文档只描述该页面独有的布局、信息层级和交互差异。页面不得重新定义本文件已有的颜色、图标、控件尺寸和通用状态。确需新增全局 Token 时，先修改本文档，再在页面文档中引用。

优先级如下：

1. 可访问性和业务安全规则。
2. 本 `MASTER` 设计系统。
3. 页面级设计说明。
4. 原型图中的视觉表现。

原型与本文档不一致时，以本文档为准；页面确需例外时必须记录原因、范围和回收条件。

## 2. 设计原则

- 专业克制：使用低饱和灰岩蓝，不使用高饱和科技蓝和装饰性渐变。
- 数据优先：装饰不能挤占筛选、表格、树和业务状态的空间。
- 层级明确：深色侧栏、浅色 Header、浅灰工作区、白色数据面形成四级结构。
- 语义稳定：品牌蓝只表达交互，绿、黄、红分别表达成功、警告和错误。
- 密度一致：后台默认使用中高密度组件，并提供紧凑表格规格。
- Token 驱动：组件不得散落硬编码色值、阴影、圆角和动画时长。
- 可访问：颜色不是唯一提示，键盘焦点、文字标签和可访问名称不可缺失。

设计参数：`DESIGN_VARIANCE=3/10`、`MOTION_INTENSITY=2/10`、`VISUAL_DENSITY=9/10`。

## 3. 颜色系统

### 3.1 品牌与应用框架

| Token | 色值 | 用途 |
| --- | --- | --- |
| `--sidebar` | `#263B52` | 左侧全局菜单与 Logo 区 |
| `--sidebar-active` | `#36526D` | 当前菜单背景 |
| `--sidebar-foreground` | `#F4F7FA` | 侧栏主要文字和图标 |
| `--sidebar-muted` | `#C7D3DE` | 侧栏次级文字和默认图标 |
| `--sidebar-accent` | `#8FB0CC` | 当前菜单强调条和侧栏焦点 |
| `--header` | `#F7F9FB` | 顶部 Header |
| `--primary` | `#4E6F91` | 主按钮、链接、分页和选中强调 |
| `--primary-hover` | `#3F5F7F` | 悬停状态 |
| `--primary-active` | `#314A63` | 按下状态 |
| `--primary-subtle` | `#EAF0F5` | 浅选中面和轻量强调 |
| `--primary-subtle-hover` | `#F2F5F7` | 行、节点和轻按钮悬停 |
| `--focus-ring` | `#8FB0CC` | 键盘焦点环 |

### 3.2 表面、文字与边框

| Token | 色值 | 用途 |
| --- | --- | --- |
| `--background` | `#F5F7F9` | 应用工作区背景 |
| `--surface` | `#FFFFFF` | 卡片、表格、弹窗和抽屉 |
| `--surface-muted` | `#F7F9FA` | 表头和分组背景 |
| `--foreground` | `#1F2D3D` | 标题、标签和关键数据 |
| `--muted-foreground` | `#697786` | 说明、占位、用户名和时间 |
| `--border` | `#DDE3E8` | 输入框、卡片和区域分隔 |
| `--border-strong` | `#C8D2DC` | 高对比边界和控件悬停 |
| `--disabled-bg` | `#EEF1F4` | 禁用控件背景 |
| `--disabled-fg` | `#8A949E` | 禁用文字和图标 |
| `--overlay` | `rgba(15, 23, 42, 0.48)` | Dialog、Drawer 遮罩 |

### 3.3 业务语义色

| 语义 | 强色 | 浅背景 | 使用规则 |
| --- | --- | --- | --- |
| 成功/正常 | `#3F6F58` | `#E8F2EC` | 正常、完成、启用 |
| 警告/待处理 | `#916A2E` | `#F8F0DF` | 待跟进、即将过期 |
| 错误/危险 | `#B14F4F` | `#F8E9E9` | 删除、失败、校验错误 |
| 信息 | `#4E6F91` | `#EAF0F5` | 普通提示和处理中 |

- 状态必须同时显示文字或图标，不能只依赖颜色。
- 危险色只用于真实危险操作和错误，不用于普通取消按钮。
- 绿色只承担业务状态，不作为品牌按钮或普通链接颜色。
- 禁止在业务组件中直接使用 `blue-500`、`green-500` 等 Tailwind 原始色阶。

## 4. 字体与数字

### 4.1 字体栈

```css
font-family: Inter, "PingFang SC", "Microsoft YaHei", "Noto Sans CJK SC", Arial, sans-serif;
```

优先使用系统中文字体，避免为后台页面加载装饰字体。编号、手机号、金额、数量和时间统一启用 `font-variant-numeric: tabular-nums`。

### 4.2 字号层级

| Token | 字号 / 行高 | 字重 | 用途 |
| --- | --- | --- | --- |
| `text-xs` | 12 / 18px | 400–500 | 标签辅助文字、表格次信息 |
| `text-sm` | 14 / 22px | 400–500 | 正文、输入、表格内容 |
| `text-base` | 16 / 24px | 400–600 | 重要正文、弹窗标题下说明 |
| `text-lg` | 18 / 28px | 600 | 区域标题 |
| `text-xl` | 20 / 30px | 600 | 页面标题，仅无面包屑定位时使用 |
| `text-2xl` | 24 / 34px | 600–700 | 登录页等独立场景主标题 |

- 表格正文不得小于 14px，辅助信息不得小于 12px。
- 不通过字重和字号同时大幅放大层级，后台页面避免营销式大标题。
- 已有面包屑能够明确定位时，不重复显示页面主标题和说明区。

## 5. 间距、尺寸与布局

### 5.1 4px 间距体系

统一使用 `4、8、12、16、20、24、32、40、48px`。禁止出现没有设计理由的 13px、17px、26px 等孤立间距。

| 场景 | 规格 |
| --- | --- |
| 图标与文字 | 8px |
| 标签与控件 | 8px |
| 同组控件 | 12–16px |
| 工具栏内边距 | 16–20px |
| 页面边距 | 20–24px |
| 页面区块间距 | 24–32px |

### 5.2 应用框架

| 项目 | 默认规格 |
| --- | --- |
| 展开侧栏 | 240px |
| 收起侧栏 | 72px |
| Header | 68px |
| 一级菜单 | 44px |
| 二级菜单 | 40px |
| 页面水平边距 | 24px |
| Header 到首个工作面 | 20–24px |

- 侧栏是唯一大面积深色应用框架。
- Header 使用浅色表面和 1px 底边，不能与侧栏使用相同深色背景。
- Header 内的面包屑是二级及更深页面的主要定位信息。
- 固定侧栏与 Header 不能遮挡滚动内容。
- 侧栏底部的“收起菜单”是低频导航工具，直接放在侧栏背景中，不使用顶部分隔线、独立 Footer 表面或悬浮卡片。
- “收起菜单”使用 Ghost 菜单行：图标与文字左对齐，保留 16px 侧栏内边距，默认使用 `--sidebar-muted`，Hover 时仅增加轻量背景反馈。

## 6. 圆角、边框与阴影

| Token | 规格 | 用途 |
| --- | --- | --- |
| `--radius-sm` | 4px | Badge、Tag、Checkbox |
| `--radius-md` | 6px | Button、Input、Select |
| `--radius-lg` | 8px | Card、Table 容器 |
| `--radius-xl` | 10px | Dialog、Drawer |
| `--shadow-surface` | `0 4px 18px rgba(38,59,82,.07)` | 页面主工作面 |
| `--shadow-overlay` | `0 12px 32px rgba(15,23,42,.16)` | Dialog、Popover |

- 普通表格行、筛选项和菜单项不使用独立阴影。
- 同级组件必须使用同一圆角等级。
- 不使用药丸形大按钮；Badge 可根据状态内容使用小圆角。

## 7. Icon 系统

### 7.1 唯一图标库

- 全项目只使用 `lucide-react`。
- 使用 Outline 风格，默认 `strokeWidth={1.75}`，保持圆角端点和连接。
- 禁止混用 Phosphor、Heroicons、Ant Design Icons、Font Awesome 和 Emoji。
- 品牌 Logo、用户头像和业务图片不属于通用 Icon，可使用审核后的独立资产。
- 后端只返回稳定 `iconKey`，前端通过本地白名单映射 Lucide 组件；禁止加载任意远程图标 URL。

### 7.2 尺寸 Token

| Token | 视觉尺寸 | 使用位置 |
| --- | --- | --- |
| `icon-xs` | 14px | Badge、输入框辅助状态 |
| `icon-sm` | 16px | Button、Table 行操作 |
| `icon-md` | 18px | Input 前后缀、工具栏 |
| `icon-lg` | 20px | 侧栏、Header、空状态辅助图标 |
| `icon-xl` | 24px | 空状态主图标、模块入口 |

- Desktop IconButton 点击热区最小 36×36px；触屏场景最小 44×44px。
- 同一导航层级不得混用填充和线性图标。
- 图标与相邻文字保持 8px 间距，并按视觉中心或文字基线对齐。
- Icon-only 控件必须提供 Tooltip 和 `aria-label`。

### 7.3 全局 `iconKey` 白名单

| `iconKey` | Lucide 组件 | 语义 |
| --- | --- | --- |
| `home` | `House` | 首页 |
| `leads` | `ContactRound` | 线索管理 |
| `opportunities` | `BriefcaseBusiness` | 商机管理 |
| `customers` | `UsersRound` | 客户管理 |
| `resources` | `Database` | 资源管理 |
| `system` | `Settings` | 系统管理 |
| `departments` | `Building2` | 部门管理 |
| `roles` | `ShieldCheck` | 角色管理 |
| `users` | `UserRound` | 用户管理 |
| `logs` | `FileText` | 系统日志 |

通用操作固定使用：`Search`、`Bell`、`ChevronDown`、`ChevronRight`、`PanelLeftClose`、`Plus`、`SquarePen`、`Trash2`、`RefreshCw`、`Ellipsis`、`SlidersHorizontal`、`X`。不得为同一操作在不同页面选择不同图标。

## 8. 通用组件规格

### 8.1 Button

| 类型 | 高度 | 使用场景 |
| --- | --- | --- |
| `sm` | 32px | 表格内紧凑操作 |
| `default` | 36px | 筛选和工具栏 |
| `lg` | 40px | 表单主要操作 |
| `auth` | 44px | 登录和身份验证 |

- Primary：品牌主色实心，每个工具栏原则上只有一个主按钮。
- Secondary：白底中性描边，用于重置、取消和次级操作。
- Ghost：无底色，用于低频图标操作。
- Destructive：仅用于确认后的危险操作。
- Loading、disabled、hover、focus、active 状态不得造成尺寸或布局跳动。

### 8.2 Form Controls

- 筛选控件高度 36–40px，编辑表单控件统一 40px，登录控件 44px。
- Label 常驻显示，不使用 Placeholder 代替字段名称。
- Label 到控件 8px，字段间垂直间距 20px。
- 错误信息紧邻字段，使用错误色、错误图标和明确文字。
- Focus 使用 2px `--focus-ring`，不得移除可见焦点。
- Disabled 必须同时降低文字、背景和交互能力。

### 8.3 Table、Tree 与 Pagination

- 表头高度 48px，背景使用 `--surface-muted`，字重 600。
- 默认业务列表行高 60px；信息较复杂时可使用 64px，紧凑模式 52px。
- 单元格水平内边距 16px；手机号、金额和时间使用等宽数字。
- 行悬停使用 `--primary-subtle-hover`，勾选行使用 `--primary-subtle`。
- Tree 节点最小高度 40px，整行可点击；展开箭头拥有独立点击热区。
- 分页当前项使用品牌主色；不可用翻页按钮必须显示禁用态。
- 列表加载使用结构一致的 Skeleton，不使用遮挡整页的大型 Spinner。

### 8.4 Dialog、Drawer、Dropdown 与 Toast

- 简单确认和少字段任务使用 Dialog；复杂新增、编辑和详情使用 Drawer。
- Dialog 默认宽度 480–640px；Drawer 默认 560–720px，按字段复杂度选择。
- Dropdown 只承载低频操作；高频操作必须直接可见。
- Toast 用于操作结果，不替代字段错误和需要用户决策的提示。
- 危险确认必须写明对象、影响和不可逆后果。

## 9. 导航与页面标题

- 侧栏展示后端授权上下文返回的菜单，前端只映射白名单路由和 `iconKey`。
- 当前菜单同时使用背景、文字和左侧强调条，不能只改图标颜色。
- Header 左侧使用面包屑；右侧固定为全局搜索、通知和账号区。
- 二级及更深页面默认不重复显示与面包屑同名的 H1。
- 只有独立登录页、工作台或确需解释任务的页面才展示页面主标题。
- 页面需要 H1 语义但视觉不展示时，应使用可访问的 visually-hidden 标题。

## 10. 交互与动效

| Token | 时长 | 使用位置 |
| --- | --- | --- |
| `--motion-fast` | 120ms | Hover、pressed、颜色变化 |
| `--motion-base` | 180ms | Dropdown、Tooltip、菜单状态 |
| `--motion-slow` | 240ms | Drawer、Dialog、侧栏收起 |

- 使用 `ease-out` 进入、`ease-in` 退出。
- 只动画 `opacity` 和 `transform`，避免动画宽高造成布局抖动；侧栏宽度变化除外，但需固定内容锚点。
- 支持 `prefers-reduced-motion`，减少或关闭非必要动画。
- 请求超过 300ms 才显示加载状态，避免闪烁；重复提交期间禁用操作。

## 11. 响应式

| 宽度 | 规则 |
| --- | --- |
| ≥1440px | 侧栏 240px，完整表格与筛选区 |
| 1024–1439px | 侧栏可收至 72px，低优先级列可隐藏 |
| 768–1023px | 侧栏默认收起，复杂 Tree 使用 Drawer，表格可横向滚动 |
| <768px | 非一期核心场景；菜单和复杂筛选使用 Drawer |

禁止固定像素容器导致页面横向溢出；表格横向滚动必须局限在表格容器内。

## 12. 无障碍与内容规范

- 正文对比度至少 4.5:1，大型图标和边界至少 3:1。
- 所有交互都可通过键盘完成，焦点顺序与视觉顺序一致。
- Icon-only 控件提供 `aria-label` 和 Tooltip。
- 表单字段具有可关联 Label、帮助文字和错误信息。
- Loading、empty、error、forbidden 状态必须有清晰文字。
- 删除、停用、转移等操作不能仅使用颜色或图标表达风险。
- 手机号、邮箱等敏感字段按后端权限脱敏；Tooltip、URL、日志和埋点不得泄露完整值。
- 中文文案使用全角标点；按钮使用动词加对象，例如“新增用户”“转移线索”。

## 13. 开发映射

- shadcn/ui 基础组件统一放在 `src/components/ui`。
- 通用复合组件放在 `src/components/shared`。
- 设计 Token 在 `src/app/styles` 中通过 CSS 变量维护，并映射给 Tailwind CSS 4。
- 页面组件只引用语义类名，例如 `bg-primary`、`text-muted-foreground`，不写十六进制颜色。
- Lucide 图标通过统一 `AppIcon` 或注册表出口使用，集中控制尺寸、描边和 `iconKey`。
- 组件状态使用 `class-variance-authority` 维护，禁止运行时拼接不可静态分析的 Tailwind 类名。

## 14. 页面文档与变更治理

页面设计文档只允许包含：

1. 页面目标和用户任务。
2. 页面独有的信息架构和布局。
3. 业务组件组合、字段和交互流程。
4. 对 `MASTER` 的明确例外。
5. 对应原型与验收标准。

新增颜色、图标尺寸、圆角、阴影或通用组件状态时，必须先更新 `MASTER`。设计系统只保留最新版本，变更记录写在本文档末尾，不复制多个历史文档。

## 15. 发布前检查

- [ ] 页面没有硬编码品牌色、圆角、阴影或动画时长。
- [ ] 图标全部来自 Lucide，语义、尺寸和描边一致。
- [ ] Icon-only 控件具有 Tooltip、`aria-label` 和足够热区。
- [ ] Header、侧栏、工作区和数据面层级符合规范。
- [ ] 表格、Tree、表单和按钮使用统一尺寸。
- [ ] Hover、focus、active、disabled、loading 状态完整。
- [ ] 成功、警告、错误状态不只依赖颜色。
- [ ] 文字和图标达到 WCAG AA 对比度。
- [ ] 1024px、1440px 和更宽桌面视口完成检查。
- [ ] 品牌文字“澄客 CRM”及业务数据逐字校对。

## 16. 变更记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| v1.1 | 2026-08-07 | 参考 Vben 简化侧栏底部收起入口，移除顶部横线和独立底栏感 |
| v1.0 | 2026-08-07 | 基于灰岩蓝方案建立首版全局 Token、Icon、组件与页面治理规范 |
