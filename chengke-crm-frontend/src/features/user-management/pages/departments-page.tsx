import { PermissionRoute } from '@/app/router/permission-route';
import { ModulePlaceholder } from '@/components/shared/module-placeholder';

/** 部门管理占位页面；后续承载集团与部门树维护。 */
export function Component() {
  return (
    <PermissionRoute required={['system:department:view']}>
      <ModulePlaceholder description="维护集团组织层级、部门负责人和部门状态。" title="部门管理" />
    </PermissionRoute>
  );
}
