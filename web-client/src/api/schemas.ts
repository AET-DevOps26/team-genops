// The single sanctioned bridge to the generated OpenAPI types.
// This is the ONLY file allowed to import from `~/generated/openapi`.
// Everything else imports the aliases below — never `components['schemas']` directly,
// and never a hand-written `interface` that duplicates a spec type.
//
// These are aliases (pointers), not copies: delete a schema from openapi.yaml and the
// corresponding line here becomes a compile error — the single-source-of-truth working
// for you, not against you.
import type { components } from '~/generated/openapi'

/** Generic accessor for any schema: `Schemas['Application']`. */
export type Schemas = components['schemas']

// Named aliases for the high-traffic types used across services/components.
export type UserResponse = Schemas['UserResponse']
export type LoginRequest = Schemas['LoginRequest']
export type RegisterRequest = Schemas['RegisterRequest']

/** Unified backend error body: `{ code, message, details }`. */
export type ApiError = Schemas['Error']
