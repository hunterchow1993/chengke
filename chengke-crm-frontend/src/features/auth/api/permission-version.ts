/**
 * 判断 409 PERMISSION_VERSION_CHANGED 是否应由 HTTP 层恢复授权上下文。
 * 授权上下文、刷新令牌和退出本身不得再走恢复，否则会循环。
 */

const SKIP_PATHS = [
  '/auth/context',
  '/auth/refresh',
  '/auth/logout',
  '/auth/login',
  '/auth/captcha',
  '/auth/first-password-change',
];

/**
 * @param url Axios 请求 url，可能是相对 `/auth/context` 的路径。
 * @returns 该请求不应触发权限版本恢复。
 */
export function shouldSkipPermissionVersionRecovery(url: string | undefined): boolean {
  if (!url) {
    return false;
  }
  return SKIP_PATHS.some((path) => url.includes(path));
}

/**
 * @param status HTTP 状态。
 * @param code 后端稳定业务码。
 * @returns 是否为权限版本已变化。
 */
export function isPermissionVersionChanged(status: number | undefined, code: string | undefined): boolean {
  return status === 409 && code === 'PERMISSION_VERSION_CHANGED';
}
