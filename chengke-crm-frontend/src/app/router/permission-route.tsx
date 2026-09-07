import type { ReactNode } from 'react';

import { useAppSelector } from '@/app/store/hooks';
import { hasRequiredPermissions } from '@/app/store/permission/permission-policy';

import { ForbiddenPage } from './forbidden-page';

interface PermissionRouteProps {
  children: ReactNode;
  required?: readonly string[];
}

/**
 * 对业务路由执行前端功能权限检查并渲染 403 状态。
 * `required` 是路由声明的全部权限码，`children` 是授权后页面。
 * 组件只读取 Redux 权限快照，不发请求，也不能替代后端鉴权。
 */
export function PermissionRoute({ children, required }: PermissionRouteProps) {
  const permissionCodes = useAppSelector((state) => state.permission.permissionCodes);
  const granted = new Set(permissionCodes);

  return hasRequiredPermissions(required, granted) ? children : <ForbiddenPage />;
}
