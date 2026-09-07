import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { createAppStore } from '@/app/store/store';
import { clearAccessToken, getAccessToken, setAccessToken } from '@/lib/http/access-token-store';
import { queryClient } from '@/lib/query/query-client';
import { server } from '@/test/msw/server';

import { endLocalSession } from './end-local-session';

afterEach(() => {
  clearAccessToken();
  queryClient.clear();
  vi.restoreAllMocks();
});

describe('endLocalSession', () => {
  it('clears token, query cache and session only after logout succeeds', async () => {
    const store = createAppStore();
    const clearSpy = vi.spyOn(queryClient, 'clear');
    setAccessToken('token-a');
    server.use(
      http.post('*/api/v1/auth/logout', () =>
        HttpResponse.json({
          code: 'OK',
          message: 'success',
          data: null,
          requestId: 'logout-ok',
          timestamp: '2026-09-05T16:36:46.877099+08:00',
        }),
      ),
    );

    await endLocalSession(store.dispatch);

    expect(getAccessToken()).toBeNull();
    expect(clearSpy).toHaveBeenCalledOnce();
    expect(store.getState().auth.status).toBe('anonymous');
  });

  it('keeps the local session when logout fails', async () => {
    const store = createAppStore();
    const clearSpy = vi.spyOn(queryClient, 'clear');
    setAccessToken('token-a');
    server.use(http.post('*/api/v1/auth/logout', () => new HttpResponse(null, { status: 401 })));

    await expect(endLocalSession(store.dispatch)).rejects.toMatchObject({ status: 401 });

    expect(getAccessToken()).toBe('token-a');
    expect(clearSpy).not.toHaveBeenCalled();
    expect(store.getState().auth.status).toBe('idle');
  });
});
