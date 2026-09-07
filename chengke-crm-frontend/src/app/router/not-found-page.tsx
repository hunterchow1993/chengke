import { ArrowLeft } from 'lucide-react';
import { Link } from 'react-router-dom';

import { Button } from '@/components/ui/button';

/**
 * 展示站内 404 状态并引导用户返回工作台。
 * 组件不读取接口或全局状态；链接会触发站内路由跳转。
 */
export function NotFoundPage() {
  return (
    <main className="grid min-h-screen place-items-center bg-[color:var(--page)]">
      <section className="text-center">
        <p className="font-[ui-monospace] text-7xl font-semibold tracking-[-0.08em] text-[color:var(--brand)]">
          404
        </p>
        <h1 className="mt-4 text-2xl font-bold">页面不存在或已经移动</h1>
        <p className="mt-2 text-sm text-[color:var(--text-muted)]">请检查地址，或返回授权工作台。</p>
        <Button asChild className="mt-6">
          <Link to="/app/dashboard">
            <ArrowLeft className="size-4" />
            返回工作台
          </Link>
        </Button>
      </section>
    </main>
  );
}
