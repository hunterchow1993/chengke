import { Slot } from '@radix-ui/react-slot';
import { cva, type VariantProps } from 'class-variance-authority';
import type { ButtonHTMLAttributes } from 'react';

import { cn } from '@/lib/utils/cn';

const buttonVariants = cva(
  'inline-flex h-10 items-center justify-center gap-2 rounded-md px-4 text-sm font-semibold transition-[background-color,color,box-shadow,transform] duration-200 outline-none focus-visible:ring-3 focus-visible:ring-[color:var(--brand-soft)] disabled:pointer-events-none disabled:opacity-50 active:translate-y-px',
  {
    variants: {
      variant: {
        primary:
          'bg-[color:var(--brand)] text-white shadow-[var(--shadow-primary)] hover:bg-[color:var(--brand-strong)]',
        secondary:
          'border border-[color:var(--border)] bg-white text-[color:var(--text)] hover:border-[color:var(--brand-border)] hover:bg-[color:var(--brand-wash)]',
        ghost:
          'text-[color:var(--text-muted)] hover:bg-[color:var(--surface-muted)] hover:text-[color:var(--text)]',
        danger: 'bg-[color:var(--danger)] text-white hover:bg-[color:var(--danger-strong)]',
      },
      size: {
        default: 'h-10 px-4',
        compact: 'h-9 px-3',
        icon: 'size-10 p-0',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'default',
    },
  },
);

interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

/**
 * 提供澄客设计令牌、键盘焦点和多种语义样式的基础按钮。
 * `variant` 控制语义外观，`size` 控制尺寸，`asChild` 将样式转交给子元素。
 * 组件无接口请求、路由和全局状态副作用。
 */
export function Button({ asChild = false, className, size, variant, ...props }: ButtonProps) {
  const Component = asChild ? Slot : 'button';

  return <Component className={cn(buttonVariants({ size, variant }), className)} {...props} />;
}
