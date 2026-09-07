import { ModulePlaceholder } from '@/components/shared/module-placeholder';
import { PermissionRoute } from '@/app/router/permission-route';

/** 用户管理路由页面；完整组织树、列表和抽屉交互将在用户管理 PRD 实现阶段接入。 */
export function UsersPage() {
  return <ModulePlaceholder description="按组织树定位集团、部门与人员，并执行用户新增、编辑和授权。" title="用户管理" />;
}

/** 路由导出负责声明用户查看权限，权限不足时由统一 403 页面处理。 */
export function Component() {
  return (
    <PermissionRoute required={['system:user:view']}>
      <UsersPage />
    </PermissionRoute>
  );
}
