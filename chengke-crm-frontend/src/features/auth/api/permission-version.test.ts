import { describe, expect, it } from 'vitest';

import { isPermissionVersionChanged, shouldSkipPermissionVersionRecovery } from './permission-version';

describe('shouldSkipPermissionVersionRecovery', () => {
  it('skips context, refresh and logout', () => {
    expect(shouldSkipPermissionVersionRecovery('/auth/context')).toBe(true);
    expect(shouldSkipPermissionVersionRecovery('/auth/refresh')).toBe(true);
    expect(shouldSkipPermissionVersionRecovery('/auth/logout')).toBe(true);
  });

  it('recovers ordinary business APIs', () => {
    expect(shouldSkipPermissionVersionRecovery('/system/users')).toBe(false);
  });
});

describe('isPermissionVersionChanged', () => {
  it('requires both HTTP 409 and the stable code', () => {
    expect(isPermissionVersionChanged(409, 'PERMISSION_VERSION_CHANGED')).toBe(true);
    expect(isPermissionVersionChanged(403, 'PERMISSION_VERSION_CHANGED')).toBe(false);
    expect(isPermissionVersionChanged(409, 'SESSION_REVOKED')).toBe(false);
  });
});
