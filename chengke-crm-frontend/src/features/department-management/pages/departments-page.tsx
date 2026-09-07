import { PermissionRoute } from '@/app/router/permission-route';

import { DepartmentManagementPrototype } from '../components/department-management-prototype';

/**
 * 部门管理路由页面，负责权限门禁并承载组织树高保真原型。
 * 页面要求 `system:department:view`，内部原型暂不请求接口或写入全局状态。
 */
export function Component() {
  return (
    <PermissionRoute required={['system:department:view']}>
      <DepartmentManagementPrototype />
    </PermissionRoute>
  );
}
