import { createAsyncThunk, createSlice, isAnyOf } from '@reduxjs/toolkit'
import type { LoginRequest, RegisterRequest, UserResponse, ApiError } from '~/api/schemas'
import { errorMessage, isNormalizedError } from '~/api/errors'
import type { NormalizedError } from '~/api/errors'
import { sessionExpired } from '~/services/session'

// Auth is a hand-rolled state machine, NOT RTK Query: a session lifecycle isn't
// cacheable server data. It still produces the app-wide NormalizedError shape, so the
// error copy comes from the same ~/api/errors map every service uses.

const COMMON: RequestInit = {
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
}

async function send<T>(path: string, init: RequestInit): Promise<T | null> {
  let res: Response
  try {
    res = await fetch(path, { ...COMMON, ...init })
  } catch {
    throw { status: 0, code: 'NETWORK', message: 'Network error' } satisfies NormalizedError
  }

  if (!res.ok) {
    const body = (await res.json().catch(() => ({}))) as Partial<ApiError>
    throw {
      status: res.status,
      code: body.code ?? `HTTP_${res.status}`,
      message: body.message ?? 'Request failed',
    } satisfies NormalizedError
  }

  if (res.status === 204) return null
  return res.json() as Promise<T>
}

/** Auth-local copy: the shared map, plus the one mode-specific case (422 validation). */
function messageFor(err: unknown, mode: 'login' | 'register'): string {
  if (isNormalizedError(err) && err.status === 422) {
    return mode === 'register'
      ? 'Password must be at least 8 characters.'
      : 'Check the fields and try again.'
  }
  return errorMessage(err)
}

export const login = createAsyncThunk<UserResponse, LoginRequest, { rejectValue: string }>(
  'auth/login',
  async (creds, { rejectWithValue }) => {
    try {
      return (await send<UserResponse>('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify(creds),
      }))!
    } catch (err) {
      return rejectWithValue(messageFor(err, 'login'))
    }
  },
)

export const register = createAsyncThunk<UserResponse, RegisterRequest, { rejectValue: string }>(
  'auth/register',
  async (creds, { rejectWithValue }) => {
    try {
      return (await send<UserResponse>('/api/v1/auth/register', {
        method: 'POST',
        body: JSON.stringify(creds),
      }))!
    } catch (err) {
      return rejectWithValue(messageFor(err, 'register'))
    }
  },
)

/** App bootstrap: ask who we are. Resolves the session from the cookie, or 401s. */
export const fetchMe = createAsyncThunk<UserResponse | null>('auth/me', async () => {
  try {
    return await send<UserResponse>('/api/v1/auth/me', { method: 'GET' })
  } catch {
    return null
  }
})

export const logout = createAsyncThunk('auth/logout', async () => {
  await send('/api/v1/auth/logout', { method: 'POST' }).catch(() => null)
})

type Status = 'unknown' | 'anonymous' | 'authenticating' | 'authenticated'

interface AuthState {
  status: Status
  user: UserResponse | null
  error: string | null
}

const initialState: AuthState = { status: 'unknown', user: null, error: null }

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    clearError(state) {
      state.error = null
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchMe.fulfilled, (state, action) => {
        state.user = action.payload
        state.status = action.payload ? 'authenticated' : 'anonymous'
      })
      .addCase(logout.fulfilled, (state) => {
        state.user = null
        state.status = 'anonymous'
      })
      // The shared signal emitted by apiClient when a 401 can't be refreshed.
      .addCase(sessionExpired, (state) => {
        state.user = null
        state.status = 'anonymous'
      })
      .addMatcher(isAnyOf(login.pending, register.pending), (state) => {
        state.status = 'authenticating'
        state.error = null
      })
      .addMatcher(isAnyOf(login.fulfilled, register.fulfilled), (state, action) => {
        state.status = 'authenticated'
        state.user = action.payload
      })
      .addMatcher(isAnyOf(login.rejected, register.rejected), (state, action) => {
        state.status = 'anonymous'
        state.error = action.payload ?? 'Something went wrong. Try again.'
      })
  },
})

export const { clearError } = authSlice.actions
export default authSlice.reducer
