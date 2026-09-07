import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { createAppStore } from '@/app/store/store';
import { server } from '@/test/msw/server';

import { NoAvailableMenuPage } from './no-available-menu-page';

function renderPage() {
  render(
    <Provider store={createAppStore()}>
      <MemoryRouter>
        <NoAvailableMenuPage />
      </MemoryRouter>
    </Provider>,
  );
}

describe('NoAvailableMenuPage', () => {
  it('shows the confirmed empty-menu copy and logout', () => {
    renderPage();

    expect(screen.getByRole('heading', { name: '暂无可用功能' })).toBeInTheDocument();
    expect(screen.getByText('当前账号尚未分配可访问功能，请联系管理员。')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '退出登录' })).toBeInTheDocument();
    expect(screen.queryByText('401')).not.toBeInTheDocument();
  });

  it('asks for confirmation before calling logout', async () => {
    const user = userEvent.setup();
    let logoutCalls = 0;
    server.use(
      http.post('*/api/v1/auth/logout', () => {
        logoutCalls += 1;
        return HttpResponse.json({ code: 'OK', message: 'success', data: null });
      }),
    );

    renderPage();
    await user.click(screen.getByRole('button', { name: '退出登录' }));

    expect(screen.getByRole('dialog', { name: '确认退出登录' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '取消' }));

    expect(screen.queryByRole('dialog', { name: '确认退出登录' })).not.toBeInTheDocument();
    expect(logoutCalls).toBe(0);
  });
});
