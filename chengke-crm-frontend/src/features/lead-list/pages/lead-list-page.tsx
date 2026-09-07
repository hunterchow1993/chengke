import { PermissionRoute } from '@/app/router/permission-route';
import { ModulePlaceholder } from '@/components/shared/module-placeholder';

/** 线索列表占位页面；后续按独立 PRD 实现筛选、分配、跟进和转商机。 */
export function Component() {
  return (
    <PermissionRoute required={['lead:view']}>
      <ModulePlaceholder description="管理已归属线索的筛选、分配、跟进、判无效和转商机流程。" title="线索列表" />
    </PermissionRoute>
  );
}
