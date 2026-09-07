import { PermissionRoute } from '@/app/router/permission-route';
import { ModulePlaceholder } from '@/components/shared/module-placeholder';

/** 线索池占位页面；后续按独立 PRD 实现公共池视图、领取与回收。 */
export function Component() {
  return (
    <PermissionRoute required={['lead:pool:view']}>
      <ModulePlaceholder description="查看授权线索池，执行领取、分配、收回和成员管理。" title="线索池" />
    </PermissionRoute>
  );
}
