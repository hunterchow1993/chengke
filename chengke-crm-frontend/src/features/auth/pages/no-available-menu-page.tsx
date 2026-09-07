import { ShieldOff } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { useConfirmLogout } from '@/features/auth/api/use-confirm-logout';
import { LogoutConfirmDialog } from '@/features/auth/components/logout-confirm-dialog';

/**
 * 已登录但授权菜单为空时的独立全屏提示，不渲染管理端侧栏。
 * 页面只提供说明文案与退出登录；「联系管理员」不可点击。
 */
export function NoAvailableMenuPage() {
  const logout = useConfirmLogout();

  return (
    <main className="grid min-h-[100dvh] place-items-center bg-[color:var(--page)] px-8">
      <div className="max-w-md text-center">
        <div className="mx-auto grid size-14 place-items-center rounded-2xl bg-[color:var(--warning-soft)] text-[color:var(--warning)]">
          <ShieldOff className="size-7" />
        </div>
        <h1 className="mt-5 text-2xl font-bold">暂无可用功能</h1>
        <p className="mt-2 text-sm leading-6 text-[color:var(--text-muted)]">
          当前账号尚未分配可访问功能，请联系管理员。
        </p>
        <Button className="mt-6" onClick={logout.openDialog} type="button">
          退出登录
        </Button>
      </div>
      <LogoutConfirmDialog
        errorMessage={logout.errorMessage}
        isSubmitting={logout.isSubmitting}
        onCancel={logout.closeDialog}
        onConfirm={() => void logout.confirmLogout()}
        open={logout.open}
      />
    </main>
  );
}
