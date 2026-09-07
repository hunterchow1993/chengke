import { PermissionRoute } from '@/app/router/permission-route';
import { ModulePlaceholder } from '@/components/shared/module-placeholder';

/** 角色管理占位页面；后续承载功能权限、数据范围和敏感字段授权。 */
export function Component() {
  return (
    <PermissionRoute required={['system:role:view']}>
      <ModulePlaceholder description="配置菜单、按钮、数据范围和敏感字段权限。" title="角色管理" />
    </PermissionRoute>
  );
}
