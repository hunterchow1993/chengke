import { describe, expect, it } from 'vitest';

import { loginSchema } from './login.schema';

describe('loginSchema', () => {
  it('trims a valid account before submission', () => {
    const result = loginSchema.parse({ account: '  admin  ', password: 'Crm@2026!' });

    expect(result.account).toBe('admin');
  });

  it('rejects an empty account', () => {
    const result = loginSchema.safeParse({ account: '   ', password: 'Crm@2026!' });

    expect(result.success).toBe(false);
  });

  it('rejects a password shorter than eight characters', () => {
    const result = loginSchema.safeParse({ account: 'admin', password: '1234567' });

    expect(result.success).toBe(false);
  });

  it('rejects an account longer than 32 characters', () => {
    const result = loginSchema.safeParse({
      account: 'a'.repeat(33),
      password: 'Crm@2026!',
    });

    expect(result.success).toBe(false);
  });

  it('rejects a password longer than 32 characters', () => {
    const result = loginSchema.safeParse({
      account: 'admin',
      password: 'a'.repeat(33),
    });

    expect(result.success).toBe(false);
  });

  it('accepts account and password at the 32-character limit', () => {
    const result = loginSchema.safeParse({
      account: 'a'.repeat(32),
      password: 'p'.repeat(32),
    });

    expect(result.success).toBe(true);
  });
});
