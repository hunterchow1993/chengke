import type { MenuItem } from '@/features/auth/schemas/auth-context.schema';

/** 后端菜单 routeKey 到已注册站内路径的白名单。未知键不得生成链接。 */
export const menuRouteKeyMap: Record<string, string> = {
  dashboard: '/app/dashboard',
  'system.users': '/app/system/users',
  'system.departments': '/app/system/departments',
  'system.roles': '/app/system/roles',
  leads: '/app/leads',
  'lead-pools': '/app/lead-pools',
};

/**
 * 将后端菜单 routeKey 映射为前端白名单路径。
 * @param routeKey 后端返回的稳定菜单路由键。
 * @returns 已注册站内路径；未知或目录节点返回 null。
 */
export function resolveMenuPath(routeKey: string | null): string | null {
  return routeKey ? (menuRouteKeyMap[routeKey] ?? null) : null;
}

/**
 * 判断当前 URL 是否仍被授权菜单覆盖。
 * @param menus 后端授权菜单树。
 * @param pathname 当前站内路径。
 * @returns 菜单中存在对应该路径的白名单节点时为 true。
 */
export function menuCoversPath(menus: readonly MenuItem[], pathname: string): boolean {
  return menus.some((item) => {
    const path = resolveMenuPath(item.routeKey);
    return path === pathname || menuCoversPath(item.children, pathname);
  });
}
