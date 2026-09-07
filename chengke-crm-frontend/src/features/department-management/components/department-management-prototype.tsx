import {
  Building2,
  Check,
  ChevronDown,
  ChevronRight,
  CircleAlert,
  Clock3,
  Database,
  Ellipsis,
  Info,
  Layers3,
  Move,
  Network,
  PanelLeftClose,
  Plus,
  Power,
  RefreshCw,
  Search,
  ShieldCheck,
  SquarePen,
  Trash2,
  UserRound,
  UsersRound,
  X,
} from 'lucide-react';
import { type FormEvent, useMemo, useState } from 'react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils/cn';

import {
  appendDepartmentChild,
  countDescendants,
  DEFAULT_DEPARTMENT_ID,
  departmentPrototypeTree,
  findDepartmentById,
  getDepartmentPath,
  searchDepartments,
  type DepartmentNode,
} from '../model/department-prototype-data';

const ICON_STROKE_WIDTH = 1.75;

interface StatusBadgeProps {
  status: DepartmentNode['status'];
}

/**
 * 使用文字与语义色展示部门状态，避免仅依赖颜色传达信息。
 * `status` 为 active 或 disabled；组件无状态和副作用。
 */
function StatusBadge({ status }: StatusBadgeProps) {
  const isActive = status === 'active';

  return (
    <span
      className={cn(
        'inline-flex h-6 items-center gap-1.5 rounded px-2 text-xs font-semibold',
        isActive
          ? 'bg-[color:var(--success-soft)] text-[color:var(--success)]'
          : 'bg-[color:var(--surface-muted)] text-[color:var(--text-muted)]',
      )}
    >
      {isActive ? (
        <Check className="size-3.5" strokeWidth={ICON_STROKE_WIDTH} />
      ) : (
        <CircleAlert className="size-3.5" strokeWidth={ICON_STROKE_WIDTH} />
      )}
      {isActive ? '正常' : '已停用'}
    </span>
  );
}

interface DepartmentTreeNodeProps {
  expandedIds: ReadonlySet<string>;
  level?: number;
  node: DepartmentNode;
  onSelect: (departmentId: string) => void;
  onToggle: (departmentId: string) => void;
  selectedId: string;
}

/**
 * 递归渲染组织树节点，提供独立展开按钮、整行选择和状态文本。
 * `expandedIds` 控制展开状态，`selectedId` 标记当前详情节点。
 * 点击只修改父组件本地原型状态，不请求接口或写入缓存。
 */
function DepartmentTreeNode({
  expandedIds,
  level = 0,
  node,
  onSelect,
  onToggle,
  selectedId,
}: DepartmentTreeNodeProps) {
  const hasChildren = node.children.length > 0;
  const expanded = expandedIds.has(node.id);
  const selected = selectedId === node.id;

  return (
    <li role="none">
      <div
        className={cn(
          'group relative flex min-h-10 items-center rounded-md pr-2 transition-colors duration-150',
          selected
            ? 'bg-[color:var(--brand-wash)] text-[color:var(--brand-strong)]'
            : 'text-[color:var(--text)] hover:bg-[color:var(--surface-muted)]',
        )}
        style={{ paddingLeft: `${8 + level * 20}px` }}
      >
        {hasChildren ? (
          <button
            aria-label={expanded ? `收起${node.name}` : `展开${node.name}`}
            className="grid size-8 shrink-0 place-items-center rounded text-[color:var(--text-muted)] outline-none hover:bg-white/80 focus-visible:ring-2 focus-visible:ring-[color:var(--brand-border)]"
            onClick={() => onToggle(node.id)}
            type="button"
          >
            {expanded ? (
              <ChevronDown className="size-4" strokeWidth={ICON_STROKE_WIDTH} />
            ) : (
              <ChevronRight className="size-4" strokeWidth={ICON_STROKE_WIDTH} />
            )}
          </button>
        ) : (
          <span className="size-8 shrink-0" />
        )}

        <button
          aria-label={`${node.name}，${node.directMembers} 位直属成员${node.status === 'disabled' ? '，已停用' : ''}`}
          aria-selected={selected}
          className="flex min-w-0 flex-1 items-center gap-2 py-2 text-left outline-none focus-visible:ring-2 focus-visible:ring-[color:var(--brand-border)]"
          onClick={() => onSelect(node.id)}
          role="treeitem"
          type="button"
        >
          <Building2
            className={cn(
              'size-4 shrink-0',
              node.status === 'disabled'
                ? 'text-[color:var(--text-subtle)]'
                : 'text-[color:var(--brand)]',
            )}
            strokeWidth={ICON_STROKE_WIDTH}
          />
          <span
            className={cn(
              'truncate text-sm font-medium',
              node.status === 'disabled' && 'text-[color:var(--text-muted)]',
            )}
          >
            {node.name}
          </span>
          {node.status === 'disabled' ? (
            <span className="shrink-0 text-[11px] text-[color:var(--text-subtle)]">停用</span>
          ) : null}
          <span className="ml-auto min-w-7 text-right text-xs tabular-nums text-[color:var(--text-subtle)]">
            {node.directMembers}
          </span>
        </button>
      </div>

      {hasChildren && expanded ? (
        <ul aria-label={`${node.name}下级部门`} role="group">
          {node.children.map((child) => (
            <DepartmentTreeNode
              expandedIds={expandedIds}
              key={child.id}
              level={level + 1}
              node={child}
              onSelect={onSelect}
              onToggle={onToggle}
              selectedId={selectedId}
            />
          ))}
        </ul>
      ) : null}
    </li>
  );
}

interface SummaryMetricProps {
  icon: typeof UsersRound;
  label: string;
  value: string;
}

/**
 * 展示部门详情顶部的单项关键统计。
 * `icon`、`label` 和 `value` 分别控制图标、统计含义和数值；组件无副作用。
 */
function SummaryMetric({ icon: Icon, label, value }: SummaryMetricProps) {
  return (
    <div className="flex min-w-0 items-center gap-3 px-5 py-4">
      <span className="grid size-9 shrink-0 place-items-center rounded-md bg-[color:var(--brand-wash)] text-[color:var(--brand)]">
        <Icon className="size-[18px]" strokeWidth={ICON_STROKE_WIDTH} />
      </span>
      <div className="min-w-0">
        <p className="text-xs text-[color:var(--text-muted)]">{label}</p>
        <p className="mt-0.5 truncate text-lg font-bold tabular-nums text-[color:var(--text)]">
          {value}
        </p>
      </div>
    </div>
  );
}

interface DetailItemProps {
  children: React.ReactNode;
  label: string;
}

/**
 * 以稳定的标签和值布局展示部门基本字段。
 * `label` 为字段名称，`children` 为可组合的字段内容；组件无状态和副作用。
 */
function DetailItem({ children, label }: DetailItemProps) {
  return (
    <div className="grid grid-cols-[112px_minmax(0,1fr)] items-start gap-4 py-3">
      <dt className="text-sm text-[color:var(--text-muted)]">{label}</dt>
      <dd className="min-w-0 text-sm font-medium text-[color:var(--text)]">{children}</dd>
    </div>
  );
}

interface DepartmentDrawerProps {
  onClose: () => void;
  onCreate: (name: string, code: string) => void;
  parent: DepartmentNode;
}

/**
 * 提供新增下级部门的高保真表单抽屉。
 * `parent` 决定只读上级部门，`onCreate` 提交演示数据，`onClose` 关闭抽屉。
 * 组件只写入页面本地原型状态，不调用接口或修改全局权限数据。
 */
function DepartmentDrawer({ onClose, onCreate, parent }: DepartmentDrawerProps) {
  const [name, setName] = useState('');
  const [code, setCode] = useState('');
  const [leader, setLeader] = useState('');
  const [sortOrder, setSortOrder] = useState('100');
  const [remark, setRemark] = useState('');
  const [nameError, setNameError] = useState('');
  const [codeError, setCodeError] = useState('');

  /**
   * 校验部门名称和编码，并把有效演示数据提交给父组件。
   * @param event 表单提交事件。
   * @returns 无返回值；校验失败时更新字段错误，成功时更新页面本地组织树。
   */
  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault();
    const normalizedName = name.trim();
    const normalizedCode = code.trim();
    const nextNameError =
      normalizedName.length >= 2 && normalizedName.length <= 50
        ? ''
        : '请输入 2-50 个字符的部门名称';
    const nextCodeError = /^[a-z][a-z0-9_]{1,49}$/.test(normalizedCode)
      ? ''
      : '请输入小写字母开头的部门编码';

    setNameError(nextNameError);
    setCodeError(nextCodeError);
    if (nextNameError || nextCodeError) return;

    onCreate(normalizedName, normalizedCode);
  }

  return (
    <div
      className="fixed inset-0 z-50 flex justify-end bg-[color:var(--overlay)]"
      role="presentation"
    >
      <section
        aria-labelledby="create-department-title"
        aria-modal="true"
        className="flex h-full w-[560px] max-w-full flex-col bg-white shadow-[var(--shadow-overlay)]"
        role="dialog"
      >
        <header className="flex h-[68px] shrink-0 items-center justify-between border-b border-[color:var(--border-soft)] px-6">
          <div>
            <h2 className="text-lg font-bold" id="create-department-title">
              新增部门
            </h2>
            <p className="mt-0.5 text-xs text-[color:var(--text-muted)]">
              在当前组织节点下创建新的部门
            </p>
          </div>
          <Button aria-label="关闭新增部门" onClick={onClose} size="icon" variant="ghost">
            <X className="size-[18px]" strokeWidth={ICON_STROKE_WIDTH} />
          </Button>
        </header>

        <form className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit}>
          <div className="flex-1 space-y-5 overflow-y-auto px-6 py-6">
            <div className="rounded-lg border border-[color:var(--border-soft)] bg-[color:var(--surface-muted)] px-4 py-3">
              <p className="text-xs font-medium text-[color:var(--text-muted)]">上级部门</p>
              <div className="mt-1.5 flex items-center gap-2 text-sm font-semibold">
                <Building2
                  className="size-4 text-[color:var(--brand)]"
                  strokeWidth={ICON_STROKE_WIDTH}
                />
                {parent.name}
              </div>
            </div>

            <label className="block" htmlFor="department-name">
              <span className="text-sm font-semibold">
                部门名称{' '}
                <span aria-hidden="true" className="text-[color:var(--danger)]">
                  *
                </span>
              </span>
              <Input
                aria-label="部门名称"
                aria-describedby={nameError ? 'department-name-error' : undefined}
                aria-invalid={Boolean(nameError)}
                className="mt-2"
                id="department-name"
                onBlur={() => {
                  if (name.trim().length > 0 && name.trim().length < 2) {
                    setNameError('请输入 2-50 个字符的部门名称');
                  }
                }}
                onChange={(event) => {
                  setName(event.target.value);
                  if (nameError) setNameError('');
                }}
                placeholder="例如：宁波销售部"
                value={name}
              />
              {nameError ? (
                <span
                  className="mt-1.5 flex items-center gap-1.5 text-xs text-[color:var(--danger)]"
                  id="department-name-error"
                >
                  <CircleAlert className="size-3.5" strokeWidth={ICON_STROKE_WIDTH} />
                  {nameError}
                </span>
              ) : null}
            </label>

            <label className="block" htmlFor="department-code">
              <span className="text-sm font-semibold">
                部门编码{' '}
                <span aria-hidden="true" className="text-[color:var(--danger)]">
                  *
                </span>
              </span>
              <Input
                aria-label="部门编码"
                aria-describedby="department-code-help"
                aria-invalid={Boolean(codeError)}
                className="mt-2 font-mono"
                id="department-code"
                onChange={(event) => {
                  setCode(event.target.value);
                  if (codeError) setCodeError('');
                }}
                placeholder="例如：ningbo_sales"
                value={code}
              />
              {codeError ? (
                <span className="mt-1.5 flex items-center gap-1.5 text-xs text-[color:var(--danger)]">
                  <CircleAlert className="size-3.5" strokeWidth={ICON_STROKE_WIDTH} />
                  {codeError}
                </span>
              ) : (
                <span
                  className="mt-1.5 block text-xs text-[color:var(--text-muted)]"
                  id="department-code-help"
                >
                  创建后不可修改，仅支持小写字母、数字和下划线
                </span>
              )}
            </label>

            <label className="block" htmlFor="department-leader">
              <span className="text-sm font-semibold">部门负责人</span>
              <select
                className="mt-2 h-10 w-full rounded-md border border-[color:var(--border)] bg-white px-3 text-sm outline-none focus:border-[color:var(--brand)] focus:ring-3 focus:ring-[color:var(--brand-soft)]"
                id="department-leader"
                onChange={(event) => setLeader(event.target.value)}
                value={leader}
              >
                <option value="">暂不设置</option>
                <option value="jiang-yue">蒋玥 / jiangyue</option>
                <option value="fang-zhi">方知 / fangzhi</option>
              </select>
              <span className="mt-1.5 block text-xs text-[color:var(--text-muted)]">
                负责人身份不会自动授予部门或业务权限
              </span>
            </label>

            <label className="block" htmlFor="department-sort">
              <span className="text-sm font-semibold">显示顺序</span>
              <Input
                className="mt-2"
                id="department-sort"
                inputMode="numeric"
                max="9999"
                min="0"
                onChange={(event) => setSortOrder(event.target.value)}
                type="number"
                value={sortOrder}
              />
              <span className="mt-1.5 block text-xs text-[color:var(--text-muted)]">
                数值越小，同级部门排序越靠前
              </span>
            </label>

            <label className="block" htmlFor="department-remark">
              <span className="text-sm font-semibold">备注</span>
              <textarea
                className="mt-2 min-h-24 w-full resize-none rounded-md border border-[color:var(--border)] bg-white px-3 py-2 text-sm leading-6 outline-none focus:border-[color:var(--brand)] focus:ring-3 focus:ring-[color:var(--brand-soft)]"
                id="department-remark"
                maxLength={200}
                onChange={(event) => setRemark(event.target.value)}
                placeholder="补充部门职责或管理说明"
                value={remark}
              />
              <span className="mt-1 block text-right text-xs tabular-nums text-[color:var(--text-subtle)]">
                {remark.length}/200
              </span>
            </label>
          </div>

          <footer className="flex h-[68px] shrink-0 items-center justify-end gap-3 border-t border-[color:var(--border-soft)] px-6">
            <Button onClick={onClose} type="button" variant="secondary">
              取消
            </Button>
            <Button type="submit">保存部门</Button>
          </footer>
        </form>
      </section>
    </div>
  );
}

interface MoveDepartmentDialogProps {
  department: DepartmentNode;
  onClose: () => void;
}

/**
 * 展示移动部门前的目标选择与服务端式影响摘要。
 * `department` 提供当前节点统计，`onClose` 关闭容器。
 * 组件不执行真实移动，仅用于原型评审高风险确认流程。
 */
function MoveDepartmentDialog({ department, onClose }: MoveDepartmentDialogProps) {
  const affectedCount = countDescendants(department) + 1;

  return (
    <div
      className="fixed inset-0 z-50 grid place-items-center bg-[color:var(--overlay)] px-6"
      role="presentation"
    >
      <section
        aria-labelledby="move-department-title"
        aria-modal="true"
        className="w-full max-w-[560px] rounded-xl bg-white shadow-[var(--shadow-overlay)]"
        role="dialog"
      >
        <header className="flex items-start justify-between border-b border-[color:var(--border-soft)] px-6 py-5">
          <div>
            <h2 className="text-lg font-bold" id="move-department-title">
              移动部门
            </h2>
            <p className="mt-1 text-sm text-[color:var(--text-muted)]">
              移动 {department.name} 及其全部下级节点
            </p>
          </div>
          <Button aria-label="关闭移动部门" onClick={onClose} size="icon" variant="ghost">
            <X className="size-[18px]" strokeWidth={ICON_STROKE_WIDTH} />
          </Button>
        </header>

        <div className="space-y-5 px-6 py-5">
          <label className="block" htmlFor="move-target">
            <span className="text-sm font-semibold">目标上级部门</span>
            <select
              className="mt-2 h-10 w-full rounded-md border border-[color:var(--border)] bg-white px-3 text-sm outline-none focus:border-[color:var(--brand)] focus:ring-3 focus:ring-[color:var(--brand-soft)]"
              defaultValue="south-division"
              id="move-target"
            >
              <option value="south-division">澄客集团 / 华南事业部</option>
              <option value="group-hr">澄客集团 / 集团人力资源部</option>
              <option value="group-finance">澄客集团 / 集团财务部</option>
            </select>
          </label>

          <div className="rounded-lg border border-[color:var(--brand-border)] bg-[color:var(--brand-wash)] p-4">
            <div className="flex items-start gap-3">
              <Info
                className="mt-0.5 size-[18px] shrink-0 text-[color:var(--brand)]"
                strokeWidth={ICON_STROKE_WIDTH}
              />
              <div>
                <h3 className="text-sm font-bold">移动影响预览</h3>
                <p className="mt-1 text-sm text-[color:var(--text-muted)]">
                  共影响 {affectedCount} 个组织节点
                </p>
              </div>
            </div>
            <div className="mt-4 grid gap-2 text-sm">
              <p className="flex items-center gap-2">
                <ShieldCheck
                  className="size-4 text-[color:var(--brand)]"
                  strokeWidth={ICON_STROKE_WIDTH}
                />
                {department.roleCount} 个角色的数据范围将重新计算
              </p>
              <p className="flex items-center gap-2">
                <UsersRound
                  className="size-4 text-[color:var(--brand)]"
                  strokeWidth={ICON_STROKE_WIDTH}
                />
                {department.directMembers} 位直属成员的组织路径将更新
              </p>
              <p className="flex items-center gap-2">
                <RefreshCw
                  className="size-4 text-[color:var(--brand)]"
                  strokeWidth={ICON_STROKE_WIDTH}
                />
                相关权限缓存将在确认后失效
              </p>
            </div>
          </div>
        </div>

        <footer className="flex items-center justify-end gap-3 border-t border-[color:var(--border-soft)] px-6 py-4">
          <Button onClick={onClose} variant="secondary">
            取消
          </Button>
          <Button onClick={onClose}>确认移动</Button>
        </footer>
      </section>
    </div>
  );
}

/**
 * 渲染部门管理的高保真可交互原型，覆盖组织树、搜索、详情、新增和移动影响预览。
 * 组件内部使用 PRD 示例数据和本地状态，不请求接口、不写缓存、不修改全局授权。
 * 该原型用于产品与视觉评审，后续接入接口时应替换为 TanStack Query 服务端状态。
 */
export function DepartmentManagementPrototype() {
  const [tree, setTree] = useState(departmentPrototypeTree);
  const [selectedId, setSelectedId] = useState(DEFAULT_DEPARTMENT_ID);
  const [expandedIds, setExpandedIds] = useState<Set<string>>(
    new Set(['chengke-group', 'east-division', 'south-division']),
  );
  const [keyword, setKeyword] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [moveOpen, setMoveOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [notice, setNotice] = useState('');
  const selectedDepartment = findDepartmentById(tree, selectedId) ?? tree[0]!;
  const searchResults = useMemo(() => searchDepartments(tree, keyword), [keyword, tree]);
  const descendantCount = countDescendants(selectedDepartment);
  const totalNodeCount = tree.reduce((total, node) => total + 1 + countDescendants(node), 0);

  /**
   * 切换一个部门节点的展开状态。
   * @param departmentId 需要展开或收起的部门 ID。
   * @returns 无返回值；只更新当前页面的树展开集合。
   */
  function toggleExpanded(departmentId: string): void {
    setExpandedIds((current) => {
      const next = new Set(current);
      if (next.has(departmentId)) next.delete(departmentId);
      else next.add(departmentId);
      return next;
    });
  }

  /**
   * 选中部门并清理临时菜单或搜索状态。
   * @param departmentId 目标部门 ID。
   * @returns 无返回值；更新详情上下文和树展开状态。
   */
  function selectDepartment(departmentId: string): void {
    setSelectedId(departmentId);
    setMenuOpen(false);
    setKeyword('');
  }

  /**
   * 向当前部门添加本地演示子节点，并选中新节点反馈成功状态。
   * @param name 已校验的部门名称。
   * @param code 已校验的部门编码。
   * @returns 无返回值；只修改当前原型组织树，不调用后端接口。
   */
  function createPrototypeDepartment(name: string, code: string): void {
    const newDepartment: DepartmentNode = {
      id: `prototype-${Date.now()}`,
      parentId: selectedDepartment.id,
      parentName: selectedDepartment.name,
      name,
      code,
      nodeType: 'department',
      status: 'active',
      leader: null,
      sortOrder: 100,
      directMembers: 0,
      roleCount: 0,
      businessReferenceCount: 0,
      remark: '原型演示部门，尚未提交后端。',
      updatedAt: '刚刚',
      updatedBy: '当前管理员',
      children: [],
    };

    setTree((current) => appendDepartmentChild(current, selectedDepartment.id, newDepartment));
    setExpandedIds((current) => new Set([...current, selectedDepartment.id]));
    setCreateOpen(false);
    setNotice(`${name}已加入当前原型`);
  }

  return (
    <div className="min-h-[calc(100dvh-68px)] bg-[color:var(--page)] p-6">
      <h1 className="sr-only">部门管理</h1>

      <div className="mb-4 flex min-h-9 items-center justify-between gap-4">
        <nav
          aria-label="面包屑"
          className="flex items-center gap-2 text-sm text-[color:var(--text-muted)]"
        >
          <span>系统管理</span>
          <ChevronRight
            className="size-4 text-[color:var(--text-subtle)]"
            strokeWidth={ICON_STROKE_WIDTH}
          />
          <span className="font-semibold text-[color:var(--text)]">部门管理</span>
        </nav>
        <div className="flex items-center gap-2 text-xs text-[color:var(--text-muted)]">
          <Clock3 className="size-4" strokeWidth={ICON_STROKE_WIDTH} />
          组织数据更新于 14:36
        </div>
      </div>

      {notice ? (
        <div
          aria-live="polite"
          className="mb-4 flex items-center justify-between rounded-lg border border-[color:var(--success)]/20 bg-[color:var(--success-soft)] px-4 py-3 text-sm text-[color:var(--success)]"
          role="status"
        >
          <span className="flex items-center gap-2 font-medium">
            <Check className="size-4" strokeWidth={ICON_STROKE_WIDTH} />
            {notice}
          </span>
          <button
            aria-label="关闭成功提示"
            className="grid size-8 place-items-center rounded hover:bg-white/50"
            onClick={() => setNotice('')}
            type="button"
          >
            <X className="size-4" strokeWidth={ICON_STROKE_WIDTH} />
          </button>
        </div>
      ) : null}

      <div className="grid min-h-[720px] grid-cols-[320px_minmax(0,1fr)] gap-4 max-[1279px]:grid-cols-1">
        <aside className="flex min-h-0 flex-col rounded-lg border border-[color:var(--border)] bg-white shadow-[var(--shadow-surface)]">
          <header className="flex h-14 shrink-0 items-center justify-between border-b border-[color:var(--border-soft)] px-4">
            <div className="flex items-center gap-2.5">
              <span className="grid size-8 place-items-center rounded-md bg-[color:var(--brand-wash)] text-[color:var(--brand)]">
                <Network className="size-[18px]" strokeWidth={ICON_STROKE_WIDTH} />
              </span>
              <div>
                <h2 className="text-sm font-bold">组织架构</h2>
                <p className="text-[11px] text-[color:var(--text-subtle)]">集团与部门层级</p>
              </div>
            </div>
            <div className="flex items-center gap-1">
              <Button aria-label="刷新组织树" size="icon" title="刷新组织树" variant="ghost">
                <RefreshCw className="size-4" strokeWidth={ICON_STROKE_WIDTH} />
              </Button>
              <Button aria-label="收起组织树" size="icon" title="收起组织树" variant="ghost">
                <PanelLeftClose className="size-4" strokeWidth={ICON_STROKE_WIDTH} />
              </Button>
            </div>
          </header>

          <div className="border-b border-[color:var(--border-soft)] p-3">
            <div className="relative">
              <Search
                className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-[color:var(--text-subtle)]"
                strokeWidth={ICON_STROKE_WIDTH}
              />
              <Input
                aria-label="搜索部门"
                className="h-9 pl-9 pr-8"
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="搜索部门名称或编码"
                role="searchbox"
                value={keyword}
              />
              {keyword ? (
                <button
                  aria-label="清空部门搜索"
                  className="absolute right-1.5 top-1/2 grid size-7 -translate-y-1/2 place-items-center rounded text-[color:var(--text-muted)] hover:bg-[color:var(--surface-muted)]"
                  onClick={() => setKeyword('')}
                  type="button"
                >
                  <X className="size-3.5" strokeWidth={ICON_STROKE_WIDTH} />
                </button>
              ) : null}
            </div>
          </div>

          <div className="min-h-0 flex-1 overflow-y-auto p-2">
            {keyword ? (
              <section aria-label="部门搜索结果" className="space-y-1" role="region">
                <p className="px-2 py-2 text-xs text-[color:var(--text-muted)]">
                  找到 {searchResults.length} 个部门
                </p>
                {searchResults.length ? (
                  searchResults.map((result) => (
                    <button
                      className="w-full rounded-md px-3 py-2.5 text-left outline-none hover:bg-[color:var(--surface-muted)] focus-visible:ring-2 focus-visible:ring-[color:var(--brand-border)]"
                      key={result.node.id}
                      onClick={() => selectDepartment(result.node.id)}
                      type="button"
                    >
                      <span className="flex items-center gap-2 text-sm font-semibold">
                        <Building2
                          className="size-4 text-[color:var(--brand)]"
                          strokeWidth={ICON_STROKE_WIDTH}
                        />
                        {result.node.name}
                      </span>
                      <span className="mt-1 block truncate pl-6 text-xs text-[color:var(--text-subtle)]">
                        {result.ancestorPath}
                      </span>
                    </button>
                  ))
                ) : (
                  <div className="px-4 py-10 text-center">
                    <Search
                      className="mx-auto size-6 text-[color:var(--text-subtle)]"
                      strokeWidth={ICON_STROKE_WIDTH}
                    />
                    <p className="mt-3 text-sm font-semibold">未找到相关部门</p>
                    <button
                      className="mt-2 text-sm font-medium text-[color:var(--brand)] hover:underline"
                      onClick={() => setKeyword('')}
                      type="button"
                    >
                      清空搜索
                    </button>
                  </div>
                )}
              </section>
            ) : (
              <ul aria-label="部门组织树" role="tree">
                {tree.map((node) => (
                  <DepartmentTreeNode
                    expandedIds={expandedIds}
                    key={node.id}
                    node={node}
                    onSelect={selectDepartment}
                    onToggle={toggleExpanded}
                    selectedId={selectedId}
                  />
                ))}
              </ul>
            )}
          </div>

          <footer className="flex h-11 shrink-0 items-center justify-between border-t border-[color:var(--border-soft)] px-4 text-xs text-[color:var(--text-muted)]">
            <span>数字表示直属成员</span>
            <span className="tabular-nums">当前可见 {totalNodeCount} 个节点</span>
          </footer>
        </aside>

        <main className="min-w-0 rounded-lg border border-[color:var(--border)] bg-white shadow-[var(--shadow-surface)]">
          <header className="flex min-h-[92px] items-center justify-between gap-6 border-b border-[color:var(--border-soft)] px-6 py-4">
            <div className="min-w-0">
              <div className="flex items-center gap-3">
                <span className="grid size-10 shrink-0 place-items-center rounded-lg bg-[color:var(--brand-wash)] text-[color:var(--brand)]">
                  <Building2 className="size-5" strokeWidth={ICON_STROKE_WIDTH} />
                </span>
                <div className="min-w-0">
                  <div className="flex items-center gap-2.5">
                    <h2 className="truncate text-lg font-bold">{selectedDepartment.name}</h2>
                    <StatusBadge status={selectedDepartment.status} />
                  </div>
                  <p className="mt-1 truncate text-xs text-[color:var(--text-muted)]">
                    {getDepartmentPath(tree, selectedDepartment.id)}
                  </p>
                </div>
              </div>
            </div>
            <div className="relative flex shrink-0 items-center gap-2">
              <Button onClick={() => setCreateOpen(true)}>
                <Plus className="size-4" strokeWidth={ICON_STROKE_WIDTH} />
                新增下级部门
              </Button>
              <Button variant="secondary">
                <SquarePen className="size-4" strokeWidth={ICON_STROKE_WIDTH} />
                编辑
              </Button>
              <Button
                aria-expanded={menuOpen}
                aria-haspopup="menu"
                aria-label="更多部门操作"
                onClick={() => setMenuOpen((current) => !current)}
                size="icon"
                variant="secondary"
              >
                <Ellipsis className="size-[18px]" strokeWidth={ICON_STROKE_WIDTH} />
              </Button>
              {menuOpen ? (
                <div
                  className="absolute right-0 top-12 z-10 w-44 rounded-lg border border-[color:var(--border)] bg-white p-1.5 shadow-[var(--shadow-overlay)]"
                  role="menu"
                >
                  <button
                    className="flex h-9 w-full items-center gap-2 rounded-md px-3 text-sm hover:bg-[color:var(--surface-muted)]"
                    onClick={() => {
                      setMoveOpen(true);
                      setMenuOpen(false);
                    }}
                    role="menuitem"
                    type="button"
                  >
                    <Move className="size-4" strokeWidth={ICON_STROKE_WIDTH} />
                    移动部门
                  </button>
                  <button
                    className="flex h-9 w-full items-center gap-2 rounded-md px-3 text-sm hover:bg-[color:var(--surface-muted)]"
                    role="menuitem"
                    type="button"
                  >
                    <Power className="size-4" strokeWidth={ICON_STROKE_WIDTH} />
                    {selectedDepartment.status === 'active' ? '停用部门' : '启用部门'}
                  </button>
                  <button
                    className="flex h-9 w-full cursor-not-allowed items-center gap-2 rounded-md px-3 text-sm text-[color:var(--text-subtle)]"
                    disabled
                    role="menuitem"
                    type="button"
                  >
                    <Trash2 className="size-4" strokeWidth={ICON_STROKE_WIDTH} />
                    删除部门
                  </button>
                  <p className="px-3 pb-1 pt-1 text-[11px] leading-4 text-[color:var(--text-subtle)]">
                    存在成员或业务引用时不可删除
                  </p>
                </div>
              ) : null}
            </div>
          </header>

          <section
            aria-label="部门关键统计"
            className="grid grid-cols-3 divide-x divide-[color:var(--border-soft)] border-b border-[color:var(--border-soft)] bg-[color:var(--surface-muted)]/60"
          >
            <SummaryMetric
              icon={UsersRound}
              label="直属成员"
              value={`${selectedDepartment.directMembers} 位`}
            />
            <SummaryMetric
              icon={Network}
              label="直接下级"
              value={`${selectedDepartment.children.length} 个`}
            />
            <SummaryMetric icon={Layers3} label="全部后代" value={`${descendantCount} 个`} />
          </section>

          <div className="grid grid-cols-[minmax(0,1.08fr)_minmax(320px,0.92fr)] gap-6 p-6 max-[1180px]:grid-cols-1">
            <section aria-labelledby="basic-info-title">
              <div className="flex items-center gap-2">
                <UserRound
                  className="size-[18px] text-[color:var(--brand)]"
                  strokeWidth={ICON_STROKE_WIDTH}
                />
                <h3 className="text-sm font-bold" id="basic-info-title">
                  基本信息
                </h3>
              </div>
              <dl className="mt-3 divide-y divide-[color:var(--border-soft)]">
                <DetailItem label="部门编码">
                  <code className="font-mono text-[13px]">{selectedDepartment.code}</code>
                </DetailItem>
                <DetailItem label="上级部门">
                  {selectedDepartment.parentName ?? '集团根节点'}
                </DetailItem>
                <DetailItem label="部门负责人">
                  {selectedDepartment.leader ? (
                    <span className="flex items-center gap-2.5">
                      <span className="grid size-8 place-items-center rounded-full bg-[color:var(--brand)] text-xs font-bold text-white">
                        {selectedDepartment.leader.initials}
                      </span>
                      <span>
                        <span className="block">{selectedDepartment.leader.name}</span>
                        <span className="mt-0.5 block text-xs font-normal text-[color:var(--text-muted)]">
                          {selectedDepartment.leader.username}
                        </span>
                      </span>
                    </span>
                  ) : (
                    '暂未设置'
                  )}
                </DetailItem>
                <DetailItem label="显示顺序">
                  <span className="tabular-nums">{selectedDepartment.sortOrder}</span>
                </DetailItem>
                <DetailItem label="部门备注">
                  <span className="leading-6 font-normal">{selectedDepartment.remark}</span>
                </DetailItem>
              </dl>

              <div className="mt-6 flex items-center justify-between rounded-lg border border-[color:var(--border-soft)] bg-[color:var(--surface-muted)] px-4 py-3">
                <div className="flex items-center gap-3">
                  <Clock3
                    className="size-[18px] text-[color:var(--text-muted)]"
                    strokeWidth={ICON_STROKE_WIDTH}
                  />
                  <div>
                    <p className="text-xs text-[color:var(--text-muted)]">最近更新</p>
                    <p className="mt-0.5 text-sm font-medium">
                      <span className="tabular-nums">{selectedDepartment.updatedAt}</span>，
                      {selectedDepartment.updatedBy}
                    </p>
                  </div>
                </div>
                <button
                  className="text-sm font-semibold text-[color:var(--brand)] hover:underline"
                  type="button"
                >
                  查看审计日志
                </button>
              </div>
            </section>

            <section aria-labelledby="impact-title">
              <div className="flex items-center gap-2">
                <ShieldCheck
                  className="size-[18px] text-[color:var(--brand)]"
                  strokeWidth={ICON_STROKE_WIDTH}
                />
                <h3 className="text-sm font-bold" id="impact-title">
                  组织影响
                </h3>
              </div>

              <div className="mt-3 rounded-lg border border-[color:var(--brand-border)] bg-[color:var(--brand-wash)] p-4">
                <div className="flex items-start gap-3">
                  <Info
                    className="mt-0.5 size-[18px] shrink-0 text-[color:var(--brand)]"
                    strokeWidth={ICON_STROKE_WIDTH}
                  />
                  <div>
                    <h4 className="text-sm font-bold">当前节点及下级数据范围</h4>
                    <p className="mt-1.5 text-xs leading-5 text-[color:var(--text-muted)]">
                      移动或停用部门会重新计算相关角色的数据范围。负责人身份不会自动获得管理权限。
                    </p>
                  </div>
                </div>
              </div>

              <div className="mt-3 space-y-2">
                <div className="flex items-center justify-between rounded-lg border border-[color:var(--border-soft)] px-4 py-3.5">
                  <span className="flex items-center gap-3 text-sm">
                    <UsersRound
                      className="size-[18px] text-[color:var(--brand)]"
                      strokeWidth={ICON_STROKE_WIDTH}
                    />
                    直属成员
                  </span>
                  <button
                    className="text-sm font-bold tabular-nums text-[color:var(--brand)] hover:underline"
                    type="button"
                  >
                    {selectedDepartment.directMembers} 位
                  </button>
                </div>
                <div className="flex items-center justify-between rounded-lg border border-[color:var(--border-soft)] px-4 py-3.5">
                  <span className="flex items-center gap-3 text-sm">
                    <ShieldCheck
                      className="size-[18px] text-[color:var(--brand)]"
                      strokeWidth={ICON_STROKE_WIDTH}
                    />
                    直接引用角色
                  </span>
                  <span className="text-sm font-bold tabular-nums">
                    {selectedDepartment.roleCount} 个
                  </span>
                </div>
                <div className="flex items-center justify-between rounded-lg border border-[color:var(--border-soft)] px-4 py-3.5">
                  <span className="flex items-center gap-3 text-sm">
                    <Database
                      className="size-[18px] text-[color:var(--brand)]"
                      strokeWidth={ICON_STROKE_WIDTH}
                    />
                    业务数据引用
                  </span>
                  <span className="text-sm font-bold tabular-nums">
                    {selectedDepartment.businessReferenceCount} 项
                  </span>
                </div>
              </div>

              <div className="mt-4 flex items-start gap-3 rounded-lg bg-[color:var(--warning-soft)] px-4 py-3 text-xs leading-5 text-[color:var(--warning)]">
                <CircleAlert className="mt-0.5 size-4 shrink-0" strokeWidth={ICON_STROKE_WIDTH} />
                当前部门存在成员和业务引用，暂不满足删除条件。请先完成成员迁移与业务交接。
              </div>
            </section>
          </div>
        </main>
      </div>

      {createOpen ? (
        <DepartmentDrawer
          onClose={() => setCreateOpen(false)}
          onCreate={createPrototypeDepartment}
          parent={selectedDepartment}
        />
      ) : null}
      {moveOpen ? (
        <MoveDepartmentDialog department={selectedDepartment} onClose={() => setMoveOpen(false)} />
      ) : null}
    </div>
  );
}
