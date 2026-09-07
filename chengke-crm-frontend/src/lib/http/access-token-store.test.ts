import { afterEach, describe, expect, it, vi } from 'vitest';

import { clearAccessToken, getAccessToken, setAccessToken } from './access-token-store';

afterEach(() => {
  clearAccessToken();
  sessionStorage.clear();
});

describe('access-token-store', () => {
  it('keeps the token in memory for the same process', () => {
    setAccessToken('token-a');
    expect(getAccessToken()).toBe('token-a');
  });

  it('restores the token from sessionStorage after a simulated reload', async () => {
    setAccessToken('token-a');
    vi.resetModules();
    const { getAccessToken: getAfterReload } = await import('./access-token-store');
    expect(getAfterReload()).toBe('token-a');
  });

  it('does not restore a token after logout', async () => {
    setAccessToken('token-a');
    clearAccessToken();
    vi.resetModules();
    const { getAccessToken: getAfterReload } = await import('./access-token-store');
    expect(getAfterReload()).toBeNull();
  });

  it('does not write the token to localStorage', () => {
    setAccessToken('token-a');
    expect(localStorage.length).toBe(0);
  });
});
