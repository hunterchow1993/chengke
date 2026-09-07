import axios, { type AxiosError, type AxiosRequestConfig } from 'axios';

import { ApiError } from './api-error';
import { getAccessToken } from './access-token-store';

interface ApiEnvelope<T> {
  code: string;
  data: T;
  message: string;
  requestId: string;
  timestamp: string;
}

interface ErrorEnvelope {
  code?: string;
  message?: string;
  requestId?: string;
}

export const apiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 15_000,
  withCredentials: true,
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  config.headers.set('X-Request-Id', crypto.randomUUID());

  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ErrorEnvelope> | ApiError) => {
    if (error instanceof ApiError) {
      return Promise.reject(error);
    }

    if (!error.response) {
      return Promise.reject(new ApiError('NETWORK_ERROR', '网络连接异常，请稍后重试'));
    }

    const body = error.response.data;
    return Promise.reject(
      new ApiError(
        body?.code ?? 'HTTP_ERROR',
        body?.message ?? '请求失败，请稍后重试',
        error.response.status,
        body?.requestId,
      ),
    );
  },
);

/**
 * 通过全局 Axios 实例调用后端并解包统一响应结构。
 * @param config Axios 请求配置；不得写入敏感查询参数或日志。
 * @returns 响应 envelope 中经过泛型约束的 data。
 * @throws ApiError 网络、协议或后端业务错误会标准化后抛出。
 */
export async function apiRequest<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await apiClient.request<ApiEnvelope<T>>(config);
  return response.data.data;
}
