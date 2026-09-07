import { zodResolver } from '@hookform/resolvers/zod';
import { Eye, EyeOff, LoaderCircle, LockKeyhole, UserRound } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useForm, type SubmitHandler } from 'react-hook-form';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

import { loginSchema, type LoginValues } from '../schemas/login.schema';

interface LoginFormProps {
  errorMessage?: string | null;
  isSubmitting: boolean;
  onSubmit: SubmitHandler<LoginValues>;
  passwordResetKey?: number;
}

/**
 * 负责登录账号、密码输入和客户端 Zod 校验。
 * `isSubmitting` 控制提交锁定，`errorMessage` 展示后端通用失败，`onSubmit` 接收规范化凭据。
 * 内部仅维护密码可见状态；真正的登录请求和路由跳转由页面层处理。
 */
export function LoginForm({ errorMessage, isSubmitting, onSubmit, passwordResetKey = 0 }: LoginFormProps) {
  const [showPassword, setShowPassword] = useState(false);
  const {
    formState: { errors },
    handleSubmit,
    register,
    resetField,
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { account: '', password: '' },
  });

  useEffect(() => {
    if (passwordResetKey > 0) {
      resetField('password');
    }
  }, [passwordResetKey, resetField]);

  return (
    <form className="space-y-5" noValidate onSubmit={(event) => void handleSubmit(onSubmit)(event)}>
      {errorMessage ? (
        <div
          className="rounded-lg border border-[color:var(--danger-border)] bg-[color:var(--danger-soft)] px-3 py-2.5 text-sm text-[color:var(--danger-strong)]"
          role="alert"
        >
          {errorMessage}
        </div>
      ) : null}

      <div className="space-y-2">
        <label className="text-sm font-semibold text-[color:var(--text)]" htmlFor="account">
          账号
        </label>
        <div className="relative">
          <UserRound
            aria-hidden="true"
            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-[color:var(--text-subtle)]"
          />
          <Input
            aria-invalid={Boolean(errors.account)}
            autoComplete="username"
            className="pl-10"
            id="account"
            placeholder="请输入账号或手机号"
            {...register('account')}
          />
        </div>
        {errors.account ? (
          <p className="text-xs text-[color:var(--danger)]">{errors.account.message}</p>
        ) : null}
      </div>

      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <label className="text-sm font-semibold text-[color:var(--text)]" htmlFor="password">
            密码
          </label>
          <button
            className="text-xs font-medium text-[color:var(--brand)] hover:underline"
            type="button"
          >
            忘记密码？
          </button>
        </div>
        <div className="relative">
          <LockKeyhole
            aria-hidden="true"
            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-[color:var(--text-subtle)]"
          />
          <Input
            aria-invalid={Boolean(errors.password)}
            autoComplete="current-password"
            className="px-10"
            id="password"
            placeholder="请输入登录密码"
            type={showPassword ? 'text' : 'password'}
            {...register('password')}
          />
          <button
            aria-label={showPassword ? '隐藏密码' : '显示密码'}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-[color:var(--text-subtle)] hover:text-[color:var(--text)]"
            onClick={() => setShowPassword((visible) => !visible)}
            type="button"
          >
            {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        </div>
        {errors.password ? (
          <p className="text-xs text-[color:var(--danger)]">{errors.password.message}</p>
        ) : null}
      </div>

      <Button className="w-full" disabled={isSubmitting} type="submit">
        {isSubmitting ? <LoaderCircle className="size-4 animate-spin" /> : null}
        {isSubmitting ? '正在登录…' : '登录'}
      </Button>
    </form>
  );
}
