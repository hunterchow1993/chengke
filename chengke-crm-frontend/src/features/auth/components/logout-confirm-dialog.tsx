import { useEffect, useRef } from 'react';

import { Button } from '@/components/ui/button';

interface LogoutConfirmDialogProps {
  errorMessage?: string | null;
  isSubmitting: boolean;
  onCancel: () => void;
  onConfirm: () => void;
  open: boolean;
}

/**
 * 退出登录前的二次确认。确认后才请求退出接口；提交中不可取消或重复确认。
 * `open` 控制显隐，`isSubmitting` 锁定操作，`errorMessage` 展示本次请求失败。
 */
export function LogoutConfirmDialog({
  errorMessage,
  isSubmitting,
  onCancel,
  onConfirm,
  open,
}: LogoutConfirmDialogProps) {
  const dialogRef = useRef<HTMLElement>(null);

  useEffect(() => {
    if (open) {
      dialogRef.current?.focus();
    }
  }, [open]);

  useEffect(() => {
    if (!open) {
      return;
    }

    function handleKeyDown(event: KeyboardEvent): void {
      if (event.key === 'Escape' && !isSubmitting) {
        onCancel();
      }
    }

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isSubmitting, onCancel, open]);

  if (!open) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 z-50 grid place-items-center bg-[color:var(--overlay)] px-6"
      onClick={() => {
        if (!isSubmitting) {
          onCancel();
        }
      }}
      role="presentation"
    >
      <section
        aria-describedby="logout-confirm-description"
        aria-labelledby="logout-confirm-title"
        aria-modal="true"
        className="w-full max-w-[480px] rounded-xl bg-white shadow-[var(--shadow-overlay)] outline-none"
        onClick={(event) => event.stopPropagation()}
        ref={dialogRef}
        role="dialog"
        tabIndex={-1}
      >
        <header className="border-b border-[color:var(--border-soft)] px-6 py-5">
          <h2 className="text-lg font-bold" id="logout-confirm-title">
            确认退出登录
          </h2>
          <p className="mt-1 text-sm text-[color:var(--text-muted)]" id="logout-confirm-description">
            退出后当前会话将被撤销，需要重新登录才能继续使用系统。
          </p>
        </header>

        {errorMessage ? (
          <div
            className="mx-6 mt-5 rounded-lg border border-[color:var(--danger-border)] bg-[color:var(--danger-soft)] px-3 py-2.5 text-sm text-[color:var(--danger-strong)]"
            role="alert"
          >
            {errorMessage}
          </div>
        ) : null}

        <footer className="flex justify-end gap-3 px-6 py-5">
          <Button disabled={isSubmitting} onClick={onCancel} type="button" variant="secondary">
            取消
          </Button>
          <Button disabled={isSubmitting} onClick={onConfirm} type="button" variant="danger">
            {isSubmitting ? '正在退出…' : '确认退出'}
          </Button>
        </footer>
      </section>
    </div>
  );
}
