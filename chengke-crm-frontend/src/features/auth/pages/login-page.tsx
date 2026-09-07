import { ArrowRight, BadgeCheck, ChartNoAxesCombined, Network, ShieldCheck } from 'lucide-react';
import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';

import { useAppDispatch, useAppSelector } from '@/app/store/hooks';
import { applyAuthContext } from '@/app/store/session-actions';
import { ApiError } from '@/lib/http/api-error';
import { setAccessToken } from '@/lib/http/access-token-store';

import loginIllustrationUrl from '../../../../../modules/login/assets/login-org-illustration.png';
import { fetchAuthContext, login } from '../api/auth.api';
import { shouldClearLoginPassword } from '../api/login-errors';
import { LoginForm } from '../components/login-form';
import type { LoginValues } from '../schemas/login.schema';

const capabilities = [
  { icon: Network, label: '集团组织与数据范围统一治理' },
  { icon: ChartNoAxesCombined, label: '线索到客户的完整经营链路' },
  { icon: ShieldCheck, label: '菜单、操作与敏感字段分级授权' },
];

/**
 * 组合品牌展示、登录表单与认证请求的登录页面。
 * 页面读取当前会话状态；内部维护提交状态与通用错误提示。
 * 提交会调用登录和授权上下文接口、更新内存 Token/Redux，并跳转后端指定首页。
 */
export function LoginPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const status = useAppSelector((state) => state.auth.status);
  const authorizedHome = useAppSelector((state) => state.permission.authorizedHome);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [passwordResetKey, setPasswordResetKey] = useState(0);

  /**
   * 完成账号登录并加载后端权威授权上下文。
   * @param values 已通过 Zod 校验并规范化的登录凭据。
   * @returns 登录流程完成后的 Promise。
   * 副作用包括接口请求、内存 Token 更新、Redux 更新和路由跳转。
   */
  async function handleSubmit(values: LoginValues): Promise<void> {
    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const session = await login(values);
      if (session.forcePasswordChange) {
        setErrorMessage('请先完成密码修改');
        return;
      }
      setAccessToken(session.accessToken);
      const context = await fetchAuthContext();
      applyAuthContext(dispatch, context);
      void navigate(context.authorizedHome, { replace: true });
    } catch (error) {
      if (error instanceof ApiError) {
        setErrorMessage(error.message);
        if (shouldClearLoginPassword(error.code)) {
          setPasswordResetKey((key) => key + 1);
        }
        return;
      }
      setErrorMessage('登录响应格式异常，请联系管理员');
    } finally {
      setIsSubmitting(false);
    }
  }

  if (status === 'authenticated') {
    return <Navigate replace to={authorizedHome} />;
  }

  return (
    <main className="grid min-h-screen grid-cols-[minmax(0,1.18fr)_minmax(460px,0.82fr)] bg-white">
      <section className="relative isolate overflow-hidden bg-[linear-gradient(145deg,#0b253d_0%,#0e416a_55%,#0f6cbd_100%)] px-[clamp(48px,6vw,104px)] py-14 text-white">

        <div className="relative z-10 flex h-full max-w-3xl flex-col">
          <div className="flex items-center gap-3">
            <div className="grid size-10 place-items-center rounded-xl border border-white/20 bg-white/12 text-sm font-black shadow-xl backdrop-blur">
              CK
            </div>
            <div>
              <p className="font-['Songti_SC'] text-xl font-bold tracking-[0.08em]">澄客 CRM</p>
              <p className="text-[10px] font-semibold tracking-[0.22em] text-sky-200 uppercase">
                Customer clarity
              </p>
            </div>
          </div>

          <div className="my-auto py-12">
            <div className="animate-rise-in max-w-xl">
              <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/8 px-3 py-1.5 text-xs font-semibold text-sky-100 backdrop-blur">
                <BadgeCheck className="size-4 text-cyan-300" />
                可信客户经营工作台
              </div>
              <h1 className="font-['Songti_SC'] text-[clamp(40px,4.6vw,68px)] leading-[1.08] font-semibold tracking-[-0.035em]">
                让每一次客户连接，
                <span className="mt-2 block text-cyan-300">沉淀为清晰增长。</span>
              </h1>
              <p className="mt-7 max-w-lg text-base leading-8 text-slate-200/85">
                从组织权限到线索流转，用统一视角连接团队、过程和客户资产，让管理决策有据可循。
              </p>
            </div>

            <div className="mt-10 grid max-w-xl gap-3">
              {capabilities.map(({ icon: Icon, label }, index) => (
                <div
                  className="animate-rise-in flex items-center gap-3 rounded-xl border border-white/10 bg-white/[0.055] px-4 py-3 text-sm text-slate-100 backdrop-blur-sm"
                  key={label}
                  style={{ animationDelay: `${160 + index * 80}ms` }}
                >
                  <span className="grid size-8 place-items-center rounded-lg bg-cyan-300/12 text-cyan-300">
                    <Icon className="size-4" />
                  </span>
                  {label}
                </div>
              ))}
            </div>
          </div>

          <p className="text-xs text-slate-400">© 2026 澄客 CRM · 企业客户经营管理平台</p>
        </div>

        <img
          alt="集团组织、部门与客户关系的抽象协作插图"
          className="animate-drift absolute bottom-[-3%] right-[-5%] z-0 w-[46%] max-w-[520px] opacity-30 mix-blend-screen"
          height="520"
          src={loginIllustrationUrl}
          width="520"
        />
      </section>

      <section className="relative grid place-items-center px-[clamp(52px,7vw,112px)] py-16">
        <div className="absolute right-10 top-8 flex items-center gap-2 text-xs text-[color:var(--text-subtle)]">
          遇到问题
          <button className="font-semibold text-[color:var(--brand)] hover:underline" type="button">
            联系管理员
          </button>
        </div>
        <div className="animate-rise-in w-full max-w-[400px]">
          <p className="text-xs font-bold tracking-[0.18em] text-[color:var(--brand)] uppercase">
            Secure workspace
          </p>
          <h2 className="mt-3 font-['Songti_SC'] text-3xl font-bold tracking-[-0.03em]">欢迎回来</h2>
          <p className="mt-3 text-sm leading-6 text-[color:var(--text-muted)]">
            登录后将根据后端授权加载你的菜单、操作权限与数据范围。
          </p>

          <div className="mt-8">
            <LoginForm
              errorMessage={errorMessage}
              isSubmitting={isSubmitting}
              onSubmit={(values) => void handleSubmit(values)}
              passwordResetKey={passwordResetKey}
            />
          </div>

          <div className="mt-7 flex items-center justify-between border-t border-[color:var(--border-soft)] pt-5 text-xs text-[color:var(--text-subtle)]">
            <span className="flex items-center gap-1.5">
              <ShieldCheck className="size-3.5 text-[color:var(--success)]" />
              连接受安全策略保护
            </span>
            <button className="flex items-center gap-1 font-semibold hover:text-[color:var(--brand)]" type="button">
              隐私与安全
              <ArrowRight className="size-3" />
            </button>
          </div>
        </div>
      </section>
    </main>
  );
}

export const Component = LoginPage;
