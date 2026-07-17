import { configureStore } from "@reduxjs/toolkit";
import authReducer from "~/services/auth/authSlice";
import { api } from "~/services/apiClient";

export const store = configureStore({
  reducer: {
    // Auth is a hand-rolled state machine (session lifecycle, not cached data).
    auth: authReducer,
    // All resource services share this ONE RTK Query reducer; they inject endpoints.
    [api.reducerPath]: api.reducer,
  },
  // RTK Query's middleware powers caching, invalidation, and refetch.
  middleware: (getDefault) => getDefault().concat(api.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
