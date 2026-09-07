import { describe, expect, it } from 'vitest';

import { authContextSchema } from './auth-context.schema';

describe('authContextSchema', () => {
  it('accepts the backend authorization context contract', () => {
    const result = authContextSchema.safeParse({
      user: { id: '1001', displayName: '张三丰', avatarUrl: null },
      tenant: { id: 'tenant-1', name: '澄客集团' },
      menus: [
        {
          id: 'system',
          label: '系统管理',
          routeKey: null,
          icon: 'settings',
          children: [],
        },
      ],
      permissionCodes: ['system:user:view'],
      dataScopes: ['department_and_descendants'],
      sensitiveFieldPermissions: [],
      expiresAt: '2026-08-06T12:00:00+08:00',
      authorizedHome: '/app/dashboard',
    });

    expect(result.success).toBe(true);
  });

  it('rejects a non-application authorized home', () => {
    const result = authContextSchema.safeParse({
      user: { id: '1001', displayName: '张三丰', avatarUrl: null },
      tenant: { id: 'tenant-1', name: '澄客集团' },
      menus: [],
      permissionCodes: [],
      dataScopes: [],
      sensitiveFieldPermissions: [],
      expiresAt: '2026-08-06T12:00:00+08:00',
      authorizedHome: 'https://malicious.example',
    });

    expect(result.success).toBe(false);
  });
});
