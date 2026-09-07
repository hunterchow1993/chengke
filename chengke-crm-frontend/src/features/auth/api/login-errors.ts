const LOGIN_ERROR_MESSAGES: Record<string, string> = {
  INVALID_CREDENTIALS: '账号或密码错误，请重新输入',
  ACCOUNT_DISABLED: '账号已停用，请联系管理员',
  ROLE_UNAVAILABLE: '账号暂时无法访问系统，请联系管理员',
  CAPTCHA_REQUIRED: '请完成验证码后重试',
  CAPTCHA_INVALID: '验证码错误或已失效',
  ACCOUNT_TEMP_LOCKED: '账号临时锁定',
  RATE_LIMITED: '操作过于频繁，请稍后再试',
  SYSTEM_UNAVAILABLE: '系统暂时不可用，请稍后重试',
};

const CLEAR_PASSWORD_CODES = new Set(['INVALID_CREDENTIALS', 'ACCOUNT_DISABLED', 'NETWORK_ERROR']);

/**
 * 将登录业务 errorCode 映射为页面文案。未登记编码不猜测业务含义。
 * @param errorCode 后端 `data.errorCode`。
 */
export function messageForLoginError(errorCode: string | null | undefined): string {
  if (!errorCode) {
    return '登录响应格式异常，请联系管理员';
  }
  return LOGIN_ERROR_MESSAGES[errorCode] ?? '登录响应格式异常，请联系管理员';
}

/**
 * 登录失败后是否清空密码、保留账号。
 * @param errorCode 稳定业务码，含网络异常 `NETWORK_ERROR`。
 */
export function shouldClearLoginPassword(errorCode: string | undefined): boolean {
  return Boolean(errorCode && CLEAR_PASSWORD_CODES.has(errorCode));
}
