import { QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Provider } from 'react-redux';

import { queryClient } from '@/lib/query/query-client';

import { store } from '../store/store';

interface AppProvidersProps {
  children: ReactNode;
}

/**
 * 组合应用唯一的 Redux 和 TanStack Query Provider。
 * `children` 为路由树；Provider 持有全局会话/权限状态与服务端缓存。
 * 组件自身不请求接口，但其单例 QueryClient 会在退出登录时被显式清空。
 */
export function AppProviders({ children }: AppProvidersProps) {
  return (
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </Provider>
  );
}
