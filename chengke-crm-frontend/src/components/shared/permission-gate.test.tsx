import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { PermissionGate } from './permission-gate';

describe('PermissionGate', () => {
  it('renders protected content when all permissions are granted', () => {
    render(
      <PermissionGate granted={new Set(['lead:view'])} required={['lead:view']}>
        <button type="button">查看线索</button>
      </PermissionGate>,
    );

    expect(screen.getByRole('button', { name: '查看线索' })).toBeInTheDocument();
  });

  it('renders the fallback when a permission is missing', () => {
    render(
      <PermissionGate
        fallback={<span>无权操作</span>}
        granted={new Set()}
        required={['lead:view']}
      >
        <button type="button">查看线索</button>
      </PermissionGate>,
    );

    expect(screen.getByText('无权操作')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '查看线索' })).not.toBeInTheDocument();
  });
});
