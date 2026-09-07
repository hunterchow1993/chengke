import type { AxiosError, InternalAxiosRequestConfig } from 'axios';

import { menuCoversPath } from '@/app/router/menu-paths';
import { router } from '@/app/router/router';
import { applyAnonymousSession, applyAuthContext } from '@/app/store/session-actions';
import { store } from '@/app/store/store';
import { apiClient } from '@/lib/http/api-client';

import { fetchAuthContext } from './auth.api';
import { isPermissionVersionChanged, shouldSkipPermissionVersionRecovery } from './permission-version';
import { refreshSessionOnce } from './session-service';

interface RecoverableRequestConfig extends InternalAxiosRequestConfig {
  permissionVersionRetried?: boolean;
}

interface ErrorEnvelope {
  code?: string;
  message?: string;
  requestId?: string;
}

let recoverPromise: Promise<void> | null = null;

/**
 * 刷新令牌并重拉授权上下文，供并发 409 共用同一趟恢复。
 */
async function recoverPermissionContext(): Promise<void> {
  recoverPromise ??= refreshSessionOnce()
    .then(() => fetchAuthContext())
    .then((context) => {
      applyAuthContext(store.dispatch, context);
      if (context.menus.length === 0) {
        return;
      }
      if (!menuCoversPath(context.menus, window.location.pathname)) {
        void router.navigate(context.authorizedHome, { replace: true });
      }
    })
    .catch((error: unknown) => {
      applyAnonymousSession(store.dispatch);
      throw error;
    })
    .finally(() => {
      recoverPromise = null;
    });

  return recoverPromise;
}

/**
 * 在 Axios 响应链上处理 409 PERMISSION_VERSION_CHANGED：换票、重拉菜单后再重试原请求。
 * 须在把 Axios 错误转成 ApiError 的拦截器之后注册，以便先看到原始 HTTP 响应。
 * @returns 拦截器 id，便于测试卸载。
 */
export function installPermissionVersionInterceptor(): number {
  return apiClient.interceptors.response.use(
    (response) => response,
    async (error: AxiosError<ErrorEnvelope>) => {
      const status = error.response?.status;
      const code = error.response?.data?.code;
      const config = error.config as RecoverableRequestConfig | undefined;

      if (!isPermissionVersionChanged(status, code) || !config) {
        return Promise.reject(error);
      }

      if (shouldSkipPermissionVersionRecovery(config.url) || config.permissionVersionRetried) {
        return Promise.reject(error);
      }

      await recoverPermissionContext();
      config.permissionVersionRetried = true;
      return apiClient.request(config);
    },
  );
}
