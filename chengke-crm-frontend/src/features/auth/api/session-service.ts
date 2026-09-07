import { ApiError } from '@/lib/http/api-error';
import { clearAccessToken, getAccessToken, setAccessToken } from '@/lib/http/access-token-store';

import { fetchAuthContext, refreshAccessToken } from './auth.api';
import type { AuthContext } from '../schemas/auth-context.schema';

let refreshPromise: Promise<string> | null = null;

/**
 * 以 single-flight 方式刷新 Access Token，避免多个 401 同时触发刷新风暴。
 * @returns 所有并发调用共享的 Access Token Promise。
 * @throws ApiError；失败时清除内存 Token，并在 Promise 结束后释放刷新锁。
 */
export async function refreshSessionOnce(): Promise<string> {
  refreshPromise ??= refreshAccessToken()
    .then((token) => {
      setAccessToken(token);
      return token;
    })
    .catch((error: unknown) => {
      clearAccessToken();
      throw error;
    })
    .finally(() => {
      refreshPromise = null;
    });

  return refreshPromise;
}

/**
 * 初始化页面会话：必须已有 Access Token（内存或本标签页 sessionStorage），先读授权上下文，过期时用同一 Bearer 换票后再试一次。
 * @returns 已校验的后端授权上下文。
 * @throws ApiError；没有可恢复的访问令牌时直接失败，由会话边界进入登录页。
 * 副作用是刷新成功时更新内存与 sessionStorage 中的 Access Token。
 */
export async function bootstrapSession(): Promise<AuthContext> {
  if (!getAccessToken()) {
    throw new ApiError('AUTHENTICATION_REQUIRED', '请先登录', 401);
  }

  try {
    return await fetchAuthContext();
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401) {
      throw error;
    }

    await refreshSessionOnce();
    return fetchAuthContext();
  }
}
