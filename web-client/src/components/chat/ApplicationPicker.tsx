import { useEffect, useState } from 'react'
import type { JobApplication } from '~/api/schemas'

interface Props {
  applications: JobApplication[]
  loading: boolean
  onSelect: (application: JobApplication | null) => void
  onDismiss: () => void
}

/**
 * Second stage of a document command: pick which application it is for.
 *
 * The assistant needs an application id to pull the job description and to attach the
 * finished document. Asking the user to paste a UUID is not a UI, so the command menu
 * hands off to this list — same keyboard model as the command dropdown above it.
 */
export function ApplicationPicker({ applications, loading, onSelect, onDismiss }: Props) {
  const [cursor, setCursor] = useState(0)
  // +1 for the trailing "no specific application" escape hatch
  const optionCount = applications.length + 1

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === 'ArrowDown') {
        e.preventDefault()
        setCursor((c) => (c + 1) % optionCount)
      } else if (e.key === 'ArrowUp') {
        e.preventDefault()
        setCursor((c) => (c - 1 + optionCount) % optionCount)
      } else if (e.key === 'Enter') {
        e.preventDefault()
        onSelect(cursor < applications.length ? applications[cursor] : null)
      } else if (e.key === 'Escape') {
        e.preventDefault()
        onDismiss()
      }
    }
    // Capture phase: the textarea below owns Enter, and it must not send a half-formed
    // command while the picker is the thing the user is actually looking at.
    window.addEventListener('keydown', onKey, true)
    return () => window.removeEventListener('keydown', onKey, true)
  }, [cursor, optionCount, applications, onSelect, onDismiss])

  return (
    <div className="absolute bottom-full left-4 right-4 mb-2 bg-raised border border-line rounded-xl overflow-hidden shadow-xl">
      <div className="px-4 py-2 border-b border-line flex items-center justify-between">
        <span className="text-xs text-dim">Which application is this for?</span>
        <span className="text-xs text-faint font-mono">↑↓ · enter · esc</span>
      </div>

      {loading && <p className="px-4 py-3 text-sm text-faint">Loading your applications…</p>}

      {!loading && applications.length === 0 && (
        <p className="px-4 py-3 text-sm text-faint">
          No applications yet — add one to generate a tailored document, or continue without it.
        </p>
      )}

      <div className="max-h-64 overflow-y-auto">
        {applications.map((app, i) => (
          <button
            key={app.id}
            onMouseEnter={() => setCursor(i)}
            onClick={() => onSelect(app)}
            className={`w-full flex items-center gap-3 px-4 py-3 text-left transition-colors ${
              i === cursor ? 'bg-raised-2' : 'hover:bg-raised-2'
            }`}
          >
            <span className="min-w-0 flex-1">
              <span className="block text-sm text-fg truncate">{app.job_title}</span>
              <span className="block text-xs text-dim truncate">{app.company}</span>
            </span>
            <span className="tag text-faint shrink-0">{app.stage}</span>
            {!app.job_description && (
              // Without a description the letter can only lean on role + company, so say so
              // here rather than letting the assistant ask for it after the fact.
              <span className="text-xs text-faint shrink-0">no description</span>
            )}
          </button>
        ))}

        <button
          onMouseEnter={() => setCursor(applications.length)}
          onClick={() => onSelect(null)}
          className={`w-full px-4 py-3 text-left text-sm text-dim border-t border-line transition-colors ${
            cursor === applications.length ? 'bg-raised-2' : 'hover:bg-raised-2'
          }`}
        >
          Continue without an application
        </button>
      </div>
    </div>
  )
}
