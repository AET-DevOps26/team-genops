import { errorMessage } from '~/api/errors'
import type { NormalizedError } from '~/api/errors'

interface Props {
  /** A NormalizedError from any service, or a pre-resolved string. */
  error: NormalizedError | string | null | undefined
  /** Per-call code→copy overrides for domain-specific voice. */
  overrides?: Record<string, string>
}

// One way to render any service's failure. Components pass the normalized error;
// this resolves the copy via the shared ~/api/errors map. Renders nothing when clear.
export function ErrorBanner({ error, overrides }: Props) {
  if (!error) return null
  const message = typeof error === 'string' ? error : errorMessage(error, overrides)
  return (
    <div
      role="alert"
      className="anim-rise rounded-lg border border-interview/30 bg-interview/10 px-3.5 py-2.5 text-sm text-interview"
    >
      {message}
    </div>
  )
}
