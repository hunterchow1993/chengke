import { describe, expect, it } from 'vitest';

import type { MenuItem } from '@/features/auth/schemas/auth-context.schema';

import { menuCoversPath, resolveMenuPath } from './menu-paths';

const usersMenu: MenuItem = {
  id: 'users',
  label: '用户管理',
  routeKey: 'system.users',
  icon: 'users',
  children: [],
};

describe('resolveMenuPath', () => {
  it('maps a whitelisted routeKey', () => {
    expect(resolveMenuPath('system.users')).toBe('/app/system/users');
  });

  it('rejects an unknown routeKey', () => {
    expect(resolveMenuPath('unknown.page')).toBeNull();
  });
});

describe('menuCoversPath', () => {
  it('finds a nested whitelist path', () => {
    const menus: MenuItem[] = [
      {
        id: 'system',
        label: '系统管理',
        routeKey: null,
        icon: 'settings',
        children: [usersMenu],
      },
    ];

    expect(menuCoversPath(menus, '/app/system/users')).toBe(true);
    expect(menuCoversPath(menus, '/app/leads')).toBe(false);
  });
});
