import { LoaderCircle } from 'lucide-react';
import { useEffect, useRef, type ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';

import { useAppDispatch, useAppSelector } from '@/app/store/hooks';
import { applyAnonymousSession, applyAuthContext } from '@/app/store/session-actions';
import { sessionLoading } from '@/app/store/auth/auth-slice';
import { bootstrapSession } from '@/features/auth';
import { NoAvailableMenuPage } from '@/features/auth/pages/no-available-menu-page';

interface SessionBoundaryProps {
  children: ReactNode;
}

/**
 * 在进入管理端前从后端恢复会话和授权上下文。
 * `children` 是已认证的管理端布局；内部状态来自 Redux，`startedRef` 防止 StrictMode 重复启动。
 * 副作用包括读取本标签页访问令牌、请求 `/auth/context`、必要时刷新 Token、更新 Redux，以及未登录时跳转登录页。
 */
export function SessionBoundary({ children }: SessionBoundaryProps) {
  const dispatch = useAppDispatch();
  const location = useLocation();
  const status = useAppSelector((state) => state.auth.status);
  const menus = useAppSelector((state) => state.permission.menus);
  const startedRef = useRef(false);

  useEffect(() => {
    if (status !== 'idle' || startedRef.current) {
      return;
    }

    startedRef.current = true;
    dispatch(sessionLoading());
    void bootstrapSession()
      .then((context) => applyAuthContext(dispatch, context))
      .catch(() => applyAnonymousSession(dispatch));
  }, [dispatch, status]);

  if (status === 'anonymous') {
    return <Navigate replace state={{ from: location.pathname }} to="/login" />;
  }

  if (status !== 'authenticated') {
    return (
      <main className="grid min-h-screen place-items-center bg-[color:var(--page)]">
        <div className="flex items-center gap-3 text-sm font-medium text-[color:var(--text-muted)]">
          <LoaderCircle className="size-5 animate-spin text-[color:var(--brand)]" />
          正在恢复安全会话…
        </div>
      </main>
    );
  }

  if (!menus.length) {
    return <NoAvailableMenuPage />;
  }

  return children;
}
