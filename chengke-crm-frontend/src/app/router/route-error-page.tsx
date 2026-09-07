import { CircleAlert, RotateCcw } from 'lucide-react';
import { isRouteErrorResponse, useRouteError } from 'react-router-dom';

import { Button } from '@/components/ui/button';

/**
 * 隔离路由模块渲染或 loader 异常，并提供安全的重试入口。
 * 组件读取 React Router 错误状态；点击重试会刷新当前页面，不展示堆栈和内部实现。
 */
export function RouteErrorPage() {
  const error = useRouteError();
  const status = isRouteErrorResponse(error) ? error.status : 500;

  return (
    <main className="grid min-h-screen place-items-center bg-[color:var(--page)] px-6">
      <section className="w-full max-w-md rounded-xl border border-[color:var(--border-soft)] bg-white p-8 text-center shadow-[0_18px_50px_rgba(31,42,55,0.08)]">
        <CircleAlert className="mx-auto size-10 text-[color:var(--danger)]" />
        <p className="mt-5 text-xs font-bold tracking-[0.18em] text-[color:var(--text-subtle)]">
          ERROR {status}
        </p>
        <h1 className="mt-2 text-xl font-bold">当前模块暂时无法打开</h1>
        <p className="mt-2 text-sm leading-6 text-[color:var(--text-muted)]">
          请稍后重试。如果问题持续存在，请将发生时间反馈给管理员。
        </p>
        <Button className="mt-6" onClick={() => window.location.reload()}>
          <RotateCcw className="size-4" />
          重新加载
        </Button>
      </section>
    </main>
  );
}
