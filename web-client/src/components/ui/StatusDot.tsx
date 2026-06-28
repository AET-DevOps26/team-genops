interface Props {
  /** A CSS color — pass a pipeline token, e.g. `var(--color-offer)`. Color is meaning. */
  color: string
  /** Pulse to signal a live/active stage. */
  live?: boolean
  /** Render the soft ring halo around the dot (used on the board rail). */
  halo?: boolean
  className?: string
}

export function StatusDot({ color, live = false, halo = false, className = '' }: Props) {
  return (
    <span
      className={`inline-block h-2.5 w-2.5 shrink-0 rounded-full ${live ? 'dot-live' : ''} ${className}`}
      style={{
        background: color,
        boxShadow: halo ? `0 0 0 4px color-mix(in oklab, ${color} 18%, transparent)` : undefined,
      }}
    />
  )
}
