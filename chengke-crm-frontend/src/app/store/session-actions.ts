import type { AppDispatch } from './store';
import { sessionAnonymous, sessionAuthenticated } from './auth/auth-slice';
import { permissionCleared, permissionLoaded } from './permission/permission-slice';
import type { AuthContext } from '@/features/auth/schemas/auth-context.schema';

/**
 * 将后端授权上下文同时写入会话与权限状态域。
 * @param dispatch Redux dispatch。
 * @param context 已通过 Zod 校验的授权上下文。
 * @returns 无返回值；副作用是更新 auth 和 permission slice。
 */
export function applyAuthContext(dispatch: AppDispatch, context: AuthContext): void {
  dispatch(sessionAuthenticated(context));
  dispatch(permissionLoaded(context));
}

/**
 * 将 Redux 会话切换为未登录并清空权限快照。
 * @param dispatch Redux dispatch。
 * @returns 无返回值；副作用是清空 auth 与 permission slice。
 */
export function applyAnonymousSession(dispatch: AppDispatch): void {
  dispatch(sessionAnonymous());
  dispatch(permissionCleared());
}
