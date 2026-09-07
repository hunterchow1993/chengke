import { z } from 'zod';

export const loginSchema = z.object({
  account: z.string().trim().min(1, '请输入账号').max(32, '账号不能超过 32 个字符'),
  password: z.string().min(8, '密码至少需要 8 个字符').max(32, '密码不能超过 32 个字符'),
});

export type LoginValues = z.infer<typeof loginSchema>;
