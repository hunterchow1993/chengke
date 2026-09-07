import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';

import { AppProviders } from '@/app/providers/app-providers';
import { router } from '@/app/router/router';
import { installPermissionVersionInterceptor } from '@/features/auth/api/permission-version-interceptor';
import '@/app/styles/index.css';

installPermissionVersionInterceptor();

const rootElement = document.getElementById('root');

if (!rootElement) {
  throw new Error('应用根节点 #root 不存在');
}

createRoot(rootElement).render(
  <StrictMode>
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>
  </StrictMode>,
);
