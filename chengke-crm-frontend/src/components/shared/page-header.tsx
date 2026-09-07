import type { ReactNode } from 'react';

interface PageHeaderProps {
  actions?: ReactNode;
  description: string;
  eyebrow?: string;
  title: string;
}

/**
 * 统一业务页面标题、说明和右侧操作区的空间层级。
 * `title` 与 `description` 描述页面，`eyebrow` 提供模块线索，`actions` 放置主要操作。
 * 组件无接口、路由或全局状态副作用。
 */
export function PageHeader({ actions, description, eyebrow, title }: PageHeaderProps) {
  return (
    <header className="flex items-start justify-between gap-8">
      <div>
        {eyebrow ? (
          <p className="mb-2 text-xs font-bold tracking-[0.16em] text-[color:var(--brand)] uppercase">
            {eyebrow}
          </p>
        ) : null}
        <h1 className="text-2xl font-bold tracking-[-0.02em] text-[color:var(--text)]">{title}</h1>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-[color:var(--text-muted)]">
          {description}
        </p>
      </div>
      {actions ? <div className="shrink-0">{actions}</div> : null}
    </header>
  );
}
