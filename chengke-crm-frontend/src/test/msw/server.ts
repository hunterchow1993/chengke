import { setupServer } from 'msw/node';

/**
 * 单元和组件测试共享的 MSW 服务；各 feature 测试按需注册 handlers。
 * 服务仅在 Vitest 进程中拦截请求，不影响开发和生产运行时。
 */
export const server = setupServer();
