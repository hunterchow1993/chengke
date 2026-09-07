import { createSlice, type PayloadAction } from '@reduxjs/toolkit';

import type { AuthContext, MenuItem } from '@/features/auth/schemas/auth-context.schema';

interface PermissionState {
  authorizedHome: string;
  dataScopes: string[];
  menus: MenuItem[];
  permissionCodes: string[];
  sensitiveFieldPermissions: string[];
}

const initialState: PermissionState = {
  authorizedHome: '/app/dashboard',
  dataScopes: [],
  menus: [],
  permissionCodes: [],
  sensitiveFieldPermissions: [],
};

const permissionSlice = createSlice({
  name: 'permission',
  initialState,
  reducers: {
    permissionLoaded(state, action: PayloadAction<AuthContext>) {
      state.authorizedHome = action.payload.authorizedHome;
      state.dataScopes = action.payload.dataScopes;
      state.menus = action.payload.menus;
      state.permissionCodes = action.payload.permissionCodes;
      state.sensitiveFieldPermissions = action.payload.sensitiveFieldPermissions;
    },
    permissionCleared() {
      return initialState;
    },
  },
});

export const { permissionCleared, permissionLoaded } = permissionSlice.actions;
export const permissionReducer = permissionSlice.reducer;
