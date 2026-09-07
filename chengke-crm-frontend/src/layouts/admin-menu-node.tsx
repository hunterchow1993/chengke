import {
  Building2,
  CircleGauge,
  ContactRound,
  DatabaseZap,
  Menu,
  Settings2,
  ShieldCheck,
  UsersRound,
} from 'lucide-react';
import type { ComponentType, SVGProps } from 'react';
import { NavLink } from 'react-router-dom';

import { resolveMenuPath } from '@/app/router/menu-paths';
import type { MenuItem } from '@/features/auth/schemas/auth-context.schema';
import { cn } from '@/lib/utils/cn';

type IconComponent = ComponentType<SVGProps<SVGSVGElement>>;

const iconMap: Record<string, IconComponent> = {
  dashboard: CircleGauge,
  users: UsersRound,
  departments: Building2,
  roles: ShieldCheck,
  leads: ContactRound,
  database: DatabaseZap,
  settings: Settings2,
};

interface AdminMenuNodeProps {
  collapsed: boolean;
  item: MenuItem;
  level?: number;
}

/**
 * 递归渲染后端授权菜单树，并只为已注册 routeKey 创建链接。
 * `item` 是菜单节点，`collapsed` 控制侧栏紧凑态，`level` 控制子级缩进。
 * 组件只读取路由活动状态，不发请求或修改全局状态。
 */
export function AdminMenuNode({ collapsed, item, level = 0 }: AdminMenuNodeProps) {
  const path = resolveMenuPath(item.routeKey);
  const Icon = iconMap[item.icon ?? ''] ?? Menu;

  return (
    <li>
      {path ? (
        <NavLink
          className={({ isActive }) =>
            cn(
              'group flex h-10 items-center gap-3 rounded-lg px-3 text-sm font-medium text-[color:var(--sidebar-muted)] transition hover:bg-white/8 hover:text-white',
              isActive &&
                'bg-[color:var(--sidebar-active)] text-[color:var(--sidebar-foreground)]',
              collapsed && 'justify-center px-0',
            )
          }
          style={collapsed ? {} : { paddingLeft: `${12 + level * 16}px` }}
          title={collapsed ? item.label : ''}
          to={path}
        >
          <Icon className="size-[18px] shrink-0" />
          {collapsed ? null : <span className="truncate">{item.label}</span>}
        </NavLink>
      ) : collapsed ? null : (
        <div className="px-3 pb-1 pt-4 text-[11px] font-bold tracking-[0.14em] text-slate-500 uppercase">
          {item.label}
        </div>
      )}
      {item.children.length ? (
        <ul className="space-y-1">
          {item.children.map((child) => (
            <AdminMenuNode collapsed={collapsed} item={child} key={child.id} level={level + 1} />
          ))}
        </ul>
      ) : null}
    </li>
  );
}
