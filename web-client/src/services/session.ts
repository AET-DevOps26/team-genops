import { createAction } from '@reduxjs/toolkit'

// A neutral cross-cutting signal: "the session is gone, the cookie can't be refreshed."
//
// This breaks what would otherwise be a dependency cycle. The apiClient baseQuery
// (transport, shared) must NOT import authSlice (a specific feature) — the arrow would
// point the wrong way. Instead the base EMITS this neutral action; authSlice CONSUMES it
// (→ status 'anonymous'), and the store also reacts by wiping cached resource data.
//
//   apiClient ──dispatch──> sessionExpired <──listen── authSlice
//
// Same dependency-direction lesson as the backend's CookieProperties extraction.
export const sessionExpired = createAction('session/expired')
