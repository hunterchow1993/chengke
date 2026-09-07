import { Building2, ChevronLeft, ChevronRight, LogOut, UserRoundCog } from 'lucide-react';
import { Outlet } from 'react-router-dom';

import { useAppDispatch, useAppSelector } from '@/app/store/hooks';
import { sidebarToggled } from '@/app/store/preference/preference-slice';
import { Button } from '@/components/ui/button';
import { useConfirmLogout } from '@/features/auth/api/use-confirm-logout';
import { LogoutConfirmDialog } from '@/features/auth/components/logout-confirm-dialog';
import { AdminMenuNode } from '@/layouts/admin-menu-node';
import { cn } from '@/lib/utils/cn';

/**
 * 提供管理端侧栏、顶栏和业务页面 Outlet 布局。
 * 菜单、用户与租户来自 Redux 授权上下文，侧栏折叠由 preference slice 管理。
 * 空菜单由会话边界拦截，不会进入本布局。
 */
export function AdminLayout() {
  const dispatch = useAppDispatch();
  const collapsed = useAppSelector((state) => state.preference.sidebarCollapsed);
  const menus = useAppSelector((state) => state.permission.menus);
  const tenant = useAppSelector((state) => state.auth.tenant);
  const user = useAppSelector((state) => state.auth.user);
  const logout = useConfirmLogout();

  return (
    <div className="flex min-h-[100dvh] bg-[color:var(--page)]">
      <aside
        className={cn(
          'sticky top-0 flex h-[100dvh] shrink-0 flex-col overflow-hidden bg-[color:var(--sidebar)] text-white transition-[width] duration-300',
          collapsed ? 'w-[72px]' : 'w-[240px]',
        )}
      >
        <div
          className={cn(
            'flex h-16 items-center border-b border-white/8 px-5',
            collapsed && 'justify-center px-0',
          )}
        >
          <div className="grid size-9 shrink-0 place-items-center rounded-xl bg-[color:var(--brand)] text-sm font-black shadow-[0_8px_24px_rgba(15,108,189,0.35)]">
            CK
          </div>
          {collapsed ? null : (
            <div className="ml-3 min-w-0">
              <p className="font-['Songti_SC'] text-lg font-bold tracking-wide">澄客 CRM</p>
              <p className="truncate text-[10px] tracking-[0.16em] text-slate-500 uppercase">
                Customer clarity
              </p>
            </div>
          )}
        </div>

        <nav aria-label="主导航" className="flex-1 overflow-y-auto px-3 py-4">
          <ul className="space-y-1">
            {menus.map((item) => (
              <AdminMenuNode collapsed={collapsed} item={item} key={item.id} />
            ))}
          </ul>
        </nav>

        <button
          aria-label={collapsed ? '展开侧栏' : '收起侧栏'}
          className={cn(
            'flex h-12 items-center gap-3 px-4 text-[color:var(--sidebar-muted)] transition hover:bg-white/6 hover:text-white',
            collapsed ? 'justify-center' : 'justify-start',
          )}
          onClick={() => dispatch(sidebarToggled())}
          type="button"
        >
          {collapsed ? <ChevronRight className="size-4" /> : <ChevronLeft className="size-4" />}
          {collapsed ? null : <span className="text-sm font-medium">收起菜单</span>}
        </button>
      </aside>

      <div className="min-w-0 flex-1">
        <header className="sticky top-0 z-20 flex h-[68px] items-center justify-between border-b border-[color:var(--border)] bg-[color:var(--header)] px-6">
          <div className="flex items-center gap-3">
            <span className="grid size-9 place-items-center rounded-lg bg-[color:var(--brand-wash)] text-[color:var(--brand)]">
              <Building2 className="size-[18px]" />
            </span>
            <div>
              <p className="text-[11px] font-semibold text-[color:var(--text-subtle)]">当前组织</p>
              <p className="text-sm font-bold">{tenant?.name ?? '未识别组织'}</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="text-right">
              <p className="text-sm font-bold">{user?.displayName ?? '未知用户'}</p>
              <p className="text-[11px] text-[color:var(--text-subtle)]">已通过安全认证</p>
            </div>
            <span className="grid size-9 place-items-center rounded-full bg-[color:var(--brand)] text-white">
              <UserRoundCog className="size-[18px]" />
            </span>
            <Button
              aria-label="退出登录"
              onClick={logout.openDialog}
              size="icon"
              variant="ghost"
            >
              <LogOut className="size-[18px]" />
            </Button>
          </div>
        </header>
        <main className="min-h-[calc(100dvh-68px)]">
          <Outlet />
        </main>
      </div>
      <LogoutConfirmDialog
        errorMessage={logout.errorMessage}
        isSubmitting={logout.isSubmitting}
        onCancel={logout.closeDialog}
        onConfirm={() => void logout.confirmLogout()}
        open={logout.open}
      />
    </div>
  );
}
