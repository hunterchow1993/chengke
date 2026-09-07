import { describe, expect, it } from 'vitest';

import { ApiError } from '@/lib/http/api-error';

import { shouldRetryQuery } from './retry-policy';

describe('shouldRetryQuery', () => {
  it.each([401, 403, 409, 422])('does not retry HTTP %s failures', (status) => {
    expect(shouldRetryQuery(0, new ApiError('FAILED', '请求失败', status))).toBe(false);
  });

  it('retries a transient failure no more than twice', () => {
    const transientError = new Error('network unavailable');

    expect(shouldRetryQuery(0, transientError)).toBe(true);
    expect(shouldRetryQuery(1, transientError)).toBe(true);
    expect(shouldRetryQuery(2, transientError)).toBe(false);
  });
});
