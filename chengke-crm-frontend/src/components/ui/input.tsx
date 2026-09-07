import type { InputHTMLAttributes } from 'react';

import { cn } from '@/lib/utils/cn';

/**
 * 统一后台表单高度、边框和焦点反馈的输入框。
 * 接收原生 input 属性并透传 ref；组件不维护业务状态或产生副作用。
 */
export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        'h-10 w-full rounded-md border border-[color:var(--border)] bg-white px-3 text-sm text-[color:var(--text)] shadow-[0_1px_2px_rgba(26,41,58,0.03)] outline-none transition placeholder:text-[color:var(--text-subtle)] focus:border-[color:var(--brand)] focus:ring-3 focus:ring-[color:var(--brand-soft)] disabled:cursor-not-allowed disabled:bg-[color:var(--surface-muted)]',
        className,
      )}
      {...props}
    />
  );
}
