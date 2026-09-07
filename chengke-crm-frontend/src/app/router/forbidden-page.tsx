import { ShieldX } from 'lucide-react';
import { Link } from 'react-router-dom';

import { useAppSelector } from '@/app/store/hooks';
import { Button } from '@/components/ui/button';

/**
 * 展示功能权限不足状态，并使用后端指定的授权首页作为返回目标。
 * 组件只读取 permission slice；点击按钮会进行站内路由跳转。
 */
export function ForbiddenPage() {
  const authorizedHome = useAppSelector((state) => state.permission.authorizedHome);

  return (
    <section className="grid min-h-[calc(100vh-64px)] place-items-center px-8">
      <div className="max-w-md text-center">
        <div className="mx-auto grid size-14 place-items-center rounded-2xl bg-[color:var(--warning-soft)] text-[color:var(--warning)]">
          <ShieldX className="size-7" />
        </div>
        <h1 className="mt-5 text-2xl font-bold">当前账号没有访问权限</h1>
        <p className="mt-2 text-sm leading-6 text-[color:var(--text-muted)]">
          菜单和数据范围由后端统一授权。如需访问，请联系管理员调整角色或数据权限。
        </p>
        <Button asChild className="mt-6" variant="secondary">
          <Link to={authorizedHome}>返回授权首页</Link>
        </Button>
      </div>
    </section>
  );
}
