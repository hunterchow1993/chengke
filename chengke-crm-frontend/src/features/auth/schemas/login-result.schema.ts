import { z } from 'zod';

/** 与 Swagger `LoginResultVO`、本地联调 HTTP 200 `data` 对齐。 */
export const loginResultSchema = z.object({
  success: z.boolean(),
  errorCode: z.string().nullable(),
  requiresCaptcha: z.boolean(),
  forcePasswordChange: z.boolean().nullable(),
  authContextRequired: z.boolean().nullable(),
  accessToken: z.string().min(1).nullable(),
});

export type LoginResult = z.infer<typeof loginResultSchema>;

export interface LoginSuccess {
  accessToken: string;
  forcePasswordChange: boolean;
}
