import type { Session } from '~/services/chat/chatApi'

interface Props {
  sessions: Session[]
  activeId: string | null
  onSelect: (id: string) => void
  onNew: () => void
  onDelete: (id: string) => void
  loading?: boolean
}

export function SessionList({ sessions, activeId, onSelect, onNew, onDelete, loading }: Props) {
  return (
    <aside className="w-60 shrink-0 flex flex-col border-r border-line bg-ink">
      <div className="px-4 py-3 border-b border-line">
        <button
          onClick={onNew}
          className="cta w-full rounded-lg px-3 py-2 text-sm font-medium transition"
        >
          + New chat
        </button>
      </div>
      <div className="flex-1 overflow-y-auto py-2">
        {loading && <p className="px-4 py-3 text-xs text-faint">Loading…</p>}
        {sessions.map((s) => (
          <div
            key={s.id}
            onClick={() => onSelect(s.id)}
            className={`group relative flex items-center px-4 py-3 text-sm cursor-pointer transition-colors ${
              s.id === activeId
                ? 'bg-raised-2 text-fg'
                : 'text-dim hover:bg-raised hover:text-fg'
            }`}
          >
            <div className="flex-1 min-w-0 pr-6">
              <p className="truncate font-medium leading-snug">
                {s.first_message ?? 'New conversation'}
              </p>
              <span className="block font-mono text-xs text-faint mt-0.5">
                {new Date(s.created_at).toLocaleDateString()}
              </span>
            </div>
            <button
              onClick={(e) => { e.stopPropagation(); onDelete(s.id) }}
              aria-label="Delete session"
              className="absolute right-3 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 text-faint hover:text-fg transition-opacity"
            >
              <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden>
                <path d="M2 4h12M6 4V2h4v2M5 4l.5 9h5L11 4" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </button>
          </div>
        ))}
      </div>
    </aside>
  )
}
