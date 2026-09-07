import { createSlice, type PayloadAction } from '@reduxjs/toolkit';

import type { AuthContext } from '@/features/auth/schemas/auth-context.schema';

export type SessionStatus = 'idle' | 'loading' | 'authenticated' | 'anonymous';

interface AuthState {
  expiresAt: string | null;
  status: SessionStatus;
  tenant: AuthContext['tenant'] | null;
  user: AuthContext['user'] | null;
}

const initialState: AuthState = {
  expiresAt: null,
  status: 'idle',
  tenant: null,
  user: null,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    sessionLoading(state) {
      state.status = 'loading';
    },
    sessionAuthenticated(state, action: PayloadAction<AuthContext>) {
      state.status = 'authenticated';
      state.user = action.payload.user;
      state.tenant = action.payload.tenant;
      state.expiresAt = action.payload.expiresAt;
    },
    sessionAnonymous() {
      return { ...initialState, status: 'anonymous' };
    },
    sessionCleared() {
      return initialState;
    },
  },
});

export const { sessionAnonymous, sessionAuthenticated, sessionCleared, sessionLoading } =
  authSlice.actions;
export const authReducer = authSlice.reducer;
