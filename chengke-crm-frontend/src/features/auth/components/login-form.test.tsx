import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { LoginForm } from './login-form';

describe('LoginForm', () => {
  it('shows field guidance when an empty form is submitted', async () => {
    const user = userEvent.setup();

    render(<LoginForm isSubmitting={false} onSubmit={vi.fn()} />);
    await user.click(screen.getByRole('button', { name: '登录' }));

    expect(await screen.findByText('请输入账号')).toBeInTheDocument();
    expect(screen.getByText('密码至少需要 8 个字符')).toBeInTheDocument();
  });

  it('submits normalized credentials', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();

    render(<LoginForm isSubmitting={false} onSubmit={onSubmit} />);
    await user.type(screen.getByLabelText('账号'), ' admin ');
    await user.type(screen.getByLabelText('密码'), 'Crm@2026!');
    await user.click(screen.getByRole('button', { name: '登录' }));

    expect(onSubmit).toHaveBeenCalledWith(
      { account: 'admin', password: 'Crm@2026!' },
      expect.anything(),
    );
  });
});
