import { applyAnonymousSession } from '@/app/store/session-actions';
import type { AppDispatch } from '@/app/store/store';
import { clearAccessToken } from '@/lib/http/access-token-store';
import { queryClient } from '@/lib/query/query-client';

import { logout } from './auth.api';

/**
 * 请求后端撤销当前会话；仅成功后清除访问令牌、业务缓存和 Redux 授权快照。
 * @param dispatch Redux dispatch。
 * @returns 本地清理完成后的 Promise。
 * @throws ApiError 或其它请求错误；失败时不清除本地会话。
 */
export async function endLocalSession(dispatch: AppDispatch): Promise<void> {
  await logout();
  clearAccessToken();
  queryClient.clear();
  applyAnonymousSession(dispatch);
}
