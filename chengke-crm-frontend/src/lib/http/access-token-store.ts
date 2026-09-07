const ACCESS_TOKEN_STORAGE_KEY = 'chengke.crm.accessToken';

let accessToken: string | null = readStoredAccessToken();

/**
 * 读取当前 Access Token：优先内存，没有则从本标签页 sessionStorage 恢复。
 * @returns 当前 Access Token；未登录、已退出或关闭标签页后为 null。
 */
export function getAccessToken(): string | null {
  if (accessToken) {
    return accessToken;
  }

  accessToken = readStoredAccessToken();
  return accessToken;
}

/**
 * 更新内存中的 Access Token，并写入本标签页 sessionStorage，以便刷新后继续携带 Bearer。
 * @param token 后端签发的短期 Access Token。
 * @returns 无返回值；不写入 localStorage、Cookie 或日志。
 */
export function setAccessToken(token: string): void {
  accessToken = token;
  writeStoredAccessToken(token);
}

/**
 * 清除内存与本标签页 sessionStorage 中的 Access Token。
 * @returns 无返回值。
 */
export function clearAccessToken(): void {
  accessToken = null;
  removeStoredAccessToken();
}

function readStoredAccessToken(): string | null {
  try {
    return sessionStorage.getItem(ACCESS_TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
}

function writeStoredAccessToken(token: string): void {
  try {
    sessionStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, token);
  } catch {
    // 隐私模式或配额不足时仍保留内存令牌，本页后续请求可继续携带。
  }
}

function removeStoredAccessToken(): void {
  try {
    sessionStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY);
  } catch {
    // 忽略不可用的 Web Storage。
  }
}
