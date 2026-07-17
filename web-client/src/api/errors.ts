// One normalized failure shape for the whole app, plus one code→copy map.
// The apiClient baseQuery normalizes every RTK Query failure into `NormalizedError`
// (see services/apiClient.ts); auth's thunks reuse the same shape. Components never
// read `err.status` or parse `err.data` — they call `errorMessage(err.code)`.

/** Every failure in the app, regardless of which service produced it. */
export interface NormalizedError {
  /** HTTP status, or 0 for a network/transport failure. */
  status: number;
  /** Backend `Error.code`, or a synthetic code (`NETWORK`, `HTTP_500`). */
  code: string;
  /** Backend `Error.message`, kept as a last-resort fallback. */
  message: string;
}

export function isNormalizedError(e: unknown): e is NormalizedError {
  return typeof e === "object" && e !== null && "code" in e && "status" in e;
}

/** Global code→copy map in JobReady's voice. Services may pass `overrides`. */
const BASE_COPY: Record<string, string> = {
  NETWORK: "Can't reach the server. Check your connection and try again.",
  INVALID_CREDENTIALS: "Email or password don't match. Try again.",
  EMAIL_TAKEN: "That email already has a board. Sign in instead.",
  FORBIDDEN: "You don't have access to that.",
  NOT_FOUND: "We couldn't find that.",
};

/**
 * Resolve a failure to human copy. Order: per-call `overrides` → global map →
 * the backend message → a generic fallback.
 */
export function errorMessage(
  err: NormalizedError | unknown,
  overrides?: Record<string, string>,
): string {
  if (!isNormalizedError(err)) return "Something went wrong. Try again.";
  return (
    overrides?.[err.code] ??
    BASE_COPY[err.code] ??
    err.message ??
    "Something went wrong. Try again."
  );
}
