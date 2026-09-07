import { Navigate, createBrowserRouter } from 'react-router-dom';

import { AdminLayout } from '@/layouts/admin-layout';

import { NotFoundPage } from './not-found-page';
import { RouteErrorPage } from './route-error-page';
import { SessionBoundary } from './session-boundary';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Navigate replace to="/login" />,
    errorElement: <RouteErrorPage />,
  },
  {
    path: '/login',
    lazy: () => import('@/features/auth/pages/login-page'),
    errorElement: <RouteErrorPage />,
  },
  {
    path: '/app',
    element: (
      <SessionBoundary>
        <AdminLayout />
      </SessionBoundary>
    ),
    errorElement: <RouteErrorPage />,
    children: [
      { index: true, element: <Navigate replace to="dashboard" /> },
      { path: 'dashboard', lazy: () => import('@/features/dashboard/pages/dashboard-page') },
      { path: 'system/users', lazy: () => import('@/features/user-management/pages/users-page') },
      {
        path: 'system/departments',
        lazy: () => import('@/features/department-management/pages/departments-page'),
      },
      { path: 'system/roles', lazy: () => import('@/features/user-management/pages/roles-page') },
      { path: 'leads', lazy: () => import('@/features/lead-list/pages/lead-list-page') },
      { path: 'lead-pools', lazy: () => import('@/features/lead-pool/pages/lead-pool-page') },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
]);
