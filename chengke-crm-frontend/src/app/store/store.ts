import { configureStore } from '@reduxjs/toolkit';

import { authReducer } from './auth/auth-slice';
import { permissionReducer } from './permission/permission-slice';
import { preferenceReducer } from './preference/preference-slice';

/**
 * 创建应用 Redux Store，便于生产和测试分别获得隔离实例。
 * @returns 包含会话、权限和界面偏好的 Redux Store。
 */
export function createAppStore() {
  return configureStore({
    reducer: {
      auth: authReducer,
      permission: permissionReducer,
      preference: preferenceReducer,
    },
    devTools: import.meta.env.DEV,
  });
}

export const store = createAppStore();
export type AppStore = typeof store;
export type RootState = ReturnType<AppStore['getState']>;
export type AppDispatch = AppStore['dispatch'];
