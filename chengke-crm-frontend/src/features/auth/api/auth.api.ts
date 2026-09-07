import { z } from 'zod';

import { ApiError } from '@/lib/http/api-error';
import { apiRequest } from '@/lib/http/api-client';

import { messageForLoginError } from './login-errors';
import { authContextSchema, type AuthContext } from '../schemas/auth-context.schema';
import {
  loginResultSchema,
  type LoginSuccess,
} from '../schemas/login-result.schema';
import type { LoginValues } from '../schemas/login.schema';

const tokenResponseSchema = z.object({
  accessToken: z.string().min(1),
});

/**
 * 使用账号密码创建登录会话。
 * HTTP 200 时仍须读 `data.success`：业务失败不会改 HTTP 状态。
 * @param credentials 已通过表单 schema 校验的账号和密码。
 * @returns 成功且含访问凭证的结果。
 * @throws ApiError 或 ZodError。
 */
export async function login(credentials: LoginValues): Promise<LoginSuccess> {
  const result = loginResultSchema.parse(
    await apiRequest<unknown>({ method: 'POST', url: '/auth/login', data: credentials }),
  );

  if (!result.success) {
    throw new ApiError(result.errorCode ?? 'INVALID_CREDENTIALS', messageForLoginError(result.errorCode), 200);
  }

  if (!result.accessToken) {
    throw new ApiError('INVALID_REQUEST', '登录响应格式异常，请联系管理员');
  }

  return {
    accessToken: result.accessToken,
    forcePasswordChange: result.forcePasswordChange === true,
  };
}

/**
 * 获取当前用户、租户、菜单和权限的后端权威上下文。
 * @returns 经过 Zod 校验的授权上下文。
 * @throws ApiError 或 ZodError；函数不写 Redux，由调用方决定状态更新。
 */
export async function fetchAuthContext(): Promise<AuthContext> {
  const response = await apiRequest<unknown>({ method: 'GET', url: '/auth/context' });
  return authContextSchema.parse(response);
}

/**
 * 使用当前内存中的 Access Token（Authorization: Bearer）换取新的短期 Access Token。
 * @returns 新签发的 Access Token。
 * @throws ApiError 或 ZodError；无访问令牌时后端返回 401。
 */
export async function refreshAccessToken(): Promise<string> {
  const response = await apiRequest<unknown>({ method: 'POST', url: '/auth/refresh' });
  return tokenResponseSchema.parse(response).accessToken;
}

/**
 * 注销当前设备会话并请求后端撤销当前会话。
 * @returns 请求完成后的 Promise；成功时 `data` 为 null。
 * @throws ApiError；失败时调用方不得清除本地会话。
 */
export async function logout(): Promise<void> {
  await apiRequest<null>({ method: 'POST', url: '/auth/logout' });
}
