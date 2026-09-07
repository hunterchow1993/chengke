import { describe, expect, it } from 'vitest';

import { hasRequiredPermissions } from './permission-policy';

describe('hasRequiredPermissions', () => {
  it('allows a route without permission requirements', () => {
    expect(hasRequiredPermissions(undefined, new Set())).toBe(true);
  });

  it('requires every permission declared by the route', () => {
    const granted = new Set(['lead:view']);

    expect(hasRequiredPermissions(['lead:view', 'lead:edit'], granted)).toBe(false);
  });

  it('allows a route when all permissions are granted', () => {
    const granted = new Set(['lead:view', 'lead:edit']);

    expect(hasRequiredPermissions(['lead:view', 'lead:edit'], granted)).toBe(true);
  });
});
