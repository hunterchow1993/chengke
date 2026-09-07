import { describe, expect, it } from 'vitest';

import { loginResultSchema } from './login-result.schema';

describe('loginResultSchema', () => {
  it('accepts a successful login payload', () => {
    const result = loginResultSchema.safeParse({
      success: true,
      errorCode: null,
      requiresCaptcha: false,
      forcePasswordChange: false,
      authContextRequired: true,
      accessToken: 'token-value',
    });

    expect(result.success).toBe(true);
  });

  it('accepts a business failure with null token', () => {
    const result = loginResultSchema.safeParse({
      success: false,
      errorCode: 'INVALID_CREDENTIALS',
      requiresCaptcha: false,
      forcePasswordChange: null,
      authContextRequired: null,
      accessToken: null,
    });

    expect(result.success).toBe(true);
  });

  it('rejects a payload that only has accessToken', () => {
    const result = loginResultSchema.safeParse({ accessToken: 'token-value' });

    expect(result.success).toBe(false);
  });
});
