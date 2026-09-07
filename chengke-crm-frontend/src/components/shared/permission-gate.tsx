import type { ReactNode } from 'react';

import { hasRequiredPermissions } from '@/app/store/permission/permission-policy';

interface PermissionGateProps {
  children: ReactNode;
  fallback?: ReactNode;
  granted: ReadonlySet<string>;
  required?: readonly string[];
}

/**
 * 按后端下发的功能权限码控制局部操作是否展示。
 * `required` 是操作需要的全部权限，`granted` 是当前授权集合，`fallback` 为无权限替代内容。
 * 组件不发请求、不修改全局状态，也不能替代后端接口鉴权。
 */
export function PermissionGate({
  children,
  fallback = null,
  granted,
  required,
}: PermissionGateProps): ReactNode {
  return hasRequiredPermissions(required, granted) ? children : fallback;
}
