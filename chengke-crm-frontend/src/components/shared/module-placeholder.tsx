import { Braces, CircleCheck, Construction } from 'lucide-react';

import { PageHeader } from './page-header';

interface ModulePlaceholderProps {
  description: string;
  title: string;
}

/**
 * 为尚未进入业务实现阶段的路由提供明确的架构占位状态。
 * `title` 和 `description` 标记目标模块；组件无接口或全局状态副作用。
 */
export function ModulePlaceholder({ description, title }: ModulePlaceholderProps) {
  return (
    <div className="animate-rise-in space-y-7 p-8">
      <PageHeader description={description} eyebrow="Module foundation" title={title} />
      <section className="grid grid-cols-[1.4fr_1fr] gap-5">
        <article className="rounded-xl border border-[color:var(--border-soft)] bg-white p-6 shadow-[0_8px_24px_rgba(31,42,55,0.04)]">
          <div className="flex items-center gap-3">
            <span className="grid size-10 place-items-center rounded-xl bg-[color:var(--brand-wash)] text-[color:var(--brand)]">
              <Construction className="size-5" />
            </span>
            <div>
              <h2 className="font-bold">业务页面待按 PRD 实现</h2>
              <p className="mt-0.5 text-xs text-[color:var(--text-subtle)]">当前路由和模块边界已就绪</p>
            </div>
          </div>
          <div className="mt-6 grid gap-3">
            {['独立 feature 目录与公开入口', '路由级懒加载和权限元数据', 'TanStack Query 服务端状态边界'].map(
              (item) => (
                <div
                  className="flex items-center gap-3 rounded-lg border border-[color:var(--border-soft)] px-4 py-3 text-sm"
                  key={item}
                >
                  <CircleCheck className="size-4 text-[color:var(--success)]" />
                  {item}
                </div>
              ),
            )}
          </div>
        </article>
        <aside className="rounded-xl border border-dashed border-[color:var(--brand-border)] bg-[color:var(--brand-wash)] p-6">
          <Braces className="size-6 text-[color:var(--brand)]" />
          <h2 className="mt-4 font-bold">下一步</h2>
          <p className="mt-2 text-sm leading-6 text-[color:var(--text-muted)]">
            根据对应模块 PRD，先补充 schema、query key 和失败测试，再实现表格、筛选、抽屉与业务交互。
          </p>
        </aside>
      </section>
    </div>
  );
}
