import { ArrowUpRight, ContactRound, DatabaseZap, ShieldCheck, UsersRound } from 'lucide-react';

import { useAppSelector } from '@/app/store/hooks';
import { PageHeader } from '@/components/shared/page-header';

const foundationItems = [
  { icon: ShieldCheck, label: '后端菜单权限', value: '已接入架构' },
  { icon: DatabaseZap, label: 'Query 数据缓存', value: '30 秒基线' },
  { icon: UsersRound, label: '组织数据范围', value: '后端权威' },
  { icon: ContactRound, label: '客户经营模块', value: '待业务接入' },
];

/**
 * 展示登录后的工作台欢迎信息和前端基础能力状态。
 * 用户与租户来自 Redux 会话快照；当前不请求业务统计，避免展示伪造数据。
 */
export function DashboardPage() {
  const tenant = useAppSelector((state) => state.auth.tenant);
  const user = useAppSelector((state) => state.auth.user);

  return (
    <div className="animate-rise-in space-y-7 p-8">
      <PageHeader
        description={`欢迎回到 ${tenant?.name ?? '澄客 CRM'}。业务统计将在对应后端接口就绪后接入。`}
        eyebrow="Operating desk"
        title={`${user?.displayName ?? '你好'}，今天也要把客户经营得更清晰`}
      />

      <section className="grid grid-cols-4 gap-4">
        {foundationItems.map(({ icon: Icon, label, value }) => (
          <article
            className="group rounded-xl border border-[color:var(--border-soft)] bg-white p-5 shadow-[0_8px_24px_rgba(31,42,55,0.035)] transition hover:-translate-y-0.5 hover:border-[color:var(--brand-border)] hover:shadow-[0_14px_32px_rgba(15,108,189,0.08)]"
            key={label}
          >
            <div className="flex items-start justify-between">
              <span className="grid size-10 place-items-center rounded-xl bg-[color:var(--brand-wash)] text-[color:var(--brand)]">
                <Icon className="size-5" />
              </span>
              <ArrowUpRight className="size-4 text-[color:var(--text-subtle)] transition group-hover:text-[color:var(--brand)]" />
            </div>
            <p className="mt-5 text-xs font-semibold text-[color:var(--text-subtle)]">{label}</p>
            <p className="mt-1 text-lg font-bold">{value}</p>
          </article>
        ))}
      </section>

      <section className="grid grid-cols-[1.45fr_1fr] gap-5">
        <article className="min-h-64 rounded-xl border border-[color:var(--border-soft)] bg-white p-6">
          <p className="text-sm font-bold">今日经营概览</p>
          <div className="mt-12 text-center">
            <p className="text-sm font-semibold text-[color:var(--text-muted)]">等待后端统计接口</p>
            <p className="mt-2 text-xs text-[color:var(--text-subtle)]">
              不在前端硬编码模拟业务数字，接入后由 TanStack Query 管理缓存。
            </p>
          </div>
        </article>
        <article className="rounded-xl bg-[linear-gradient(145deg,#123653,#0f6cbd)] p-6 text-white shadow-[0_18px_40px_rgba(15,108,189,0.18)]">
          <p className="text-xs font-bold tracking-[0.16em] text-sky-200 uppercase">Architecture ready</p>
          <h2 className="mt-4 font-['Songti_SC'] text-2xl font-bold leading-9">基础设施已经就位，业务实现从 PRD 与失败测试开始。</h2>
          <p className="mt-4 text-sm leading-6 text-sky-100/75">
            登录、权限、路由、请求、缓存和设计令牌已形成统一入口。
          </p>
        </article>
      </section>
    </div>
  );
}

export const Component = DashboardPage;
