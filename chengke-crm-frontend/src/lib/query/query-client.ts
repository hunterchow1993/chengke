import { QueryClient } from '@tanstack/react-query';

import { shouldRetryQuery } from './retry-policy';

/**
 * 创建应用级 QueryClient，集中规定缓存新鲜度、重试和窗口聚焦行为。
 * @returns 未共享缓存的新 QueryClient，测试可按用例独立创建。
 */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        retry: shouldRetryQuery,
        refetchOnWindowFocus: false,
      },
      mutations: {
        retry: false,
      },
    },
  });
}

export const queryClient = createQueryClient();
