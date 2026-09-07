import { createSlice } from '@reduxjs/toolkit';

interface PreferenceState {
  sidebarCollapsed: boolean;
}

const initialState: PreferenceState = {
  sidebarCollapsed: false,
};

const preferenceSlice = createSlice({
  name: 'preference',
  initialState,
  reducers: {
    sidebarToggled(state) {
      state.sidebarCollapsed = !state.sidebarCollapsed;
    },
  },
});

export const { sidebarToggled } = preferenceSlice.actions;
export const preferenceReducer = preferenceSlice.reducer;
