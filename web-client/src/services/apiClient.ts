import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'
import type { BaseQueryFn, FetchArgs, FetchBaseQueryError } from '@reduxjs/toolkit/query/react'
import { Mutex } from 'async-mutex'
import type { ApiError } from '~/api/schemas'
import type { NormalizedError } from '~/api/errors'
import { sessionExpired } from '~/services/session'

// The ONE shared transport. credentials:'include' sends the HttpOnly session cookies;
// the relative /api/v1 base is proxied to the auth service in dev (vite.config.ts).
const rawBaseQuery = fetchBaseQuery({ baseUrl: '/api/v1', credentials: 'include' })

/** Collapse RTK Query's FetchBaseQueryError into the app-wide NormalizedError shape. */
function normalize(error: FetchBaseQueryError): NormalizedError {
  if (typeof error.status !== 'number') {
    // FETCH_ERROR / TIMEOUT_ERROR / PARSING_ERROR — no HTTP response.
    return { status: 0, code: 'NETWORK', message: 'Network error' }
  }
  const body = (error.data ?? {}) as Partial<ApiError>
  return {
    status: error.status,
    code: body.code ?? `HTTP_${error.status}`,
    message: body.message ?? 'Request failed',
  }
}

// Single-flight guard: on access-cookie expiry, exactly ONE /refresh runs; every other
// in-flight request waits on this mutex and retries with the freshly-rotated cookie.
// Without it, parallel 401s would each call /refresh and race the rotation → spurious logout.
const mutex = new Mutex()

const baseQueryWithReauth: BaseQueryFn<string | FetchArgs, unknown, NormalizedError> = async (
  args,
  apiCtx,
  extra,
) => {
  await mutex.waitForUnlock() // if a refresh is mid-flight, wait for it first
  let result = await rawBaseQuery(args, apiCtx, extra)

  if (result.error && (result.error as FetchBaseQueryError).status === 401) {
    if (!mutex.isLocked()) {
      const release = await mutex.acquire()
      try {
        const refresh = await rawBaseQuery({ url: '/auth/refresh', method: 'POST' }, apiCtx, extra)
        if (refresh.error) {
          apiCtx.dispatch(sessionExpired())
          apiCtx.dispatch(api.util.resetApiState()) // wipe cached data on logout
        } else {
          result = await rawBaseQuery(args, apiCtx, extra) // retry the original
        }
      } finally {
        release()
      }
    } else {
      // Someone else is refreshing — wait it out, then retry once.
      await mutex.waitForUnlock()
      result = await rawBaseQuery(args, apiCtx, extra)
    }
  }

  if (result.error) return { error: normalize(result.error as FetchBaseQueryError) }
  return { data: result.data }
}

// The root API. Endpoints are injected by each service (services/<x>/<x>Api.ts) via
// `api.injectEndpoints` — this file stays domain-agnostic and owns only transport.
export const api = createApi({
  reducerPath: 'api',
  baseQuery: baseQueryWithReauth,
  tagTypes: ['Application', 'Profile', 'CoverLetter', 'Email', 'Chat', 'Messages'],
  endpoints: () => ({}),
})
