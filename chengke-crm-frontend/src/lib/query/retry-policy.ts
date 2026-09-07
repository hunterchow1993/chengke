import { ApiError } from '@/lib/http/api-error';

const NON_RETRYABLE_STATUSES = new Set([400, 401, 403, 404, 409, 422, 429]);

/**
 * 判断 TanStack Query 的幂等查询是否应自动重试。
 * @param failureCount 已经发生的失败次数，从 0 开始。
 * @param error 当前查询错误。
 * @returns 网络或服务端临时错误最多重试两次；权限和业务错误不重试。
 */
export function shouldRetryQuery(failureCount: number, error: unknown): boolean {
  if (error instanceof ApiError && error.status && NON_RETRYABLE_STATUSES.has(error.status)) {
    return false;
  }

  return failureCount < 2;
}
