import type { HTMLAttributes, ReactNode } from 'react'

interface Props extends HTMLAttributes<HTMLDivElement> {
  /** Optional header strip (the "application card" motif: status tag + id). */
  header?: ReactNode
  children: ReactNode
}

// The raised, lined card shell used as JobReady's primary surface.
export function Card({ header, className = '', children, ...props }: Props) {
  return (
    <div
      className={`overflow-hidden rounded-2xl border border-line bg-raised shadow-[0_30px_80px_-30px_rgba(0,0,0,0.8)] ${className}`}
      {...props}
    >
      {header && (
        <div className="flex items-center justify-between border-b border-line px-6 py-4">{header}</div>
      )}
      {children}
    </div>
  )
}
