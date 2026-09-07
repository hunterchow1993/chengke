import { describe, expect, it } from 'vitest';

import { messageForLoginError, shouldClearLoginPassword } from './login-errors';

describe('messageForLoginError', () => {
  it('maps invalid credentials to the PRD copy', () => {
    expect(messageForLoginError('INVALID_CREDENTIALS')).toBe('账号或密码错误，请重新输入');
  });

  it('does not invent copy for unknown codes', () => {
    expect(messageForLoginError('NOT_A_REAL_CODE')).toBe('登录响应格式异常，请联系管理员');
  });
});

describe('shouldClearLoginPassword', () => {
  it('clears password for credentials, disabled account and network errors', () => {
    expect(shouldClearLoginPassword('INVALID_CREDENTIALS')).toBe(true);
    expect(shouldClearLoginPassword('ACCOUNT_DISABLED')).toBe(true);
    expect(shouldClearLoginPassword('NETWORK_ERROR')).toBe(true);
  });

  it('keeps password for other login business codes', () => {
    expect(shouldClearLoginPassword('ROLE_UNAVAILABLE')).toBe(false);
  });
});
