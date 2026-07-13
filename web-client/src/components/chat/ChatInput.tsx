import { useState, useRef, KeyboardEvent } from 'react'
import type { JobApplication } from '~/api/schemas'
import { useListApplicationsQuery } from '~/services/applications/applicationsApi'
import { ApplicationPicker } from './ApplicationPicker'

const COMMANDS = [
  { cmd: '/cover_letter', label: 'Cover Letter', description: 'Generate a tailored cover letter' },
  { cmd: '/resume_tailor', label: 'Resume Tailor', description: 'Tailor your resume for a role' },
  { cmd: '/fit_analysis', label: 'Fit Analysis', description: 'Analyse your fit and skill gaps' },
]

// Every command targets a specific job, so each one asks which application it is for.
const COMMANDS_NEEDING_APPLICATION = COMMANDS.map((c) => c.cmd)

const UUID_RE = /\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b/i

interface Props {
  onSend: (message: string) => void
  disabled?: boolean
  /** Initial input handed off by another page (e.g. an application's document tab). */
  prefill?: string
}

export function ChatInput({ onSend, disabled, prefill }: Props) {
  const [value, setValue] = useState(prefill ?? '')
  const [showDropdown, setShowDropdown] = useState(false)
  const [activeCommand, setActiveCommand] = useState<(typeof COMMANDS)[0] | null>(
    prefill ? (COMMANDS.find((c) => prefill.toLowerCase().includes(c.cmd)) ?? null) : null,
  )
  const [showPicker, setShowPicker] = useState(false)
  const [selectedApp, setSelectedApp] = useState<JobApplication | null>(null)
  // Whether the user has already answered "which application?" — including by declining.
  // Without this, "continue without an application" leaves no selection and submit() would
  // reopen the picker forever.
  const [applicationAsked, setApplicationAsked] = useState(false)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  // Only fetched once a command is in play — no request on an ordinary chat.
  const { data, isLoading } = useListApplicationsQuery(undefined, { skip: !activeCommand })
  const applications = data?.items ?? []

  function needsApplication(msg: string, command: string | null) {
    if (!command || !COMMANDS_NEEDING_APPLICATION.includes(command)) return false
    if (selectedApp || applicationAsked) return false
    // A prefill from an application page already carries the id; don't ask again.
    return !UUID_RE.test(msg)
  }

  function handleChange(e: React.ChangeEvent<HTMLTextAreaElement>) {
    const val = e.target.value
    setValue(val)

    // Keep dropdown open while typing a command (starts with / and no space yet)
    const trimmed = val.trimStart()
    setShowDropdown(trimmed.startsWith('/') && !trimmed.includes(' '))

    // Detect a fully typed command anywhere in the message
    const matched = COMMANDS.find((c) => val.toLowerCase().includes(c.cmd))
    setActiveCommand(matched ?? null)
    if (!matched) {
      setSelectedApp(null)
      setApplicationAsked(false)
    }
  }

  function handleKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      submit()
    }
    if (e.key === 'Escape') {
      setShowDropdown(false)
    }
  }

  function selectCommand(cmd: string) {
    setValue(cmd + ' ')
    setShowDropdown(false)
    setActiveCommand(COMMANDS.find((c) => c.cmd === cmd) ?? null)
    // Picking the command is exactly the moment to ask which job it is for.
    if (COMMANDS_NEEDING_APPLICATION.includes(cmd)) setShowPicker(true)
    textareaRef.current?.focus()
  }

  function pickApplication(application: JobApplication | null) {
    setSelectedApp(application)
    setApplicationAsked(true)
    setShowPicker(false)
    textareaRef.current?.focus()
  }

  function dismissPicker() {
    // Escaping counts as answering: the user has seen the question and moved on.
    setApplicationAsked(true)
    setShowPicker(false)
    textareaRef.current?.focus()
  }

  function submit() {
    const msg = value.trim()
    if (!msg || disabled) return

    // Typed the command by hand and never picked a job — ask before sending, rather than
    // letting the assistant produce a generic letter or interrogate the user for details
    // the app already has.
    if (needsApplication(msg, activeCommand?.cmd ?? null)) {
      setShowPicker(true)
      return
    }

    const withApplication =
      selectedApp && !UUID_RE.test(msg)
        ? `${msg} (application id: ${selectedApp.id})`
        : msg

    onSend(withApplication)
    setValue('')
    setActiveCommand(null)
    setSelectedApp(null)
    setApplicationAsked(false)
    setShowDropdown(false)
    setShowPicker(false)
  }

  return (
    <div className="border-t border-line p-4 relative">
      {showPicker && (
        <ApplicationPicker
          applications={applications}
          loading={isLoading}
          onSelect={pickApplication}
          onDismiss={dismissPicker}
        />
      )}

      {/* Command dropdown */}
      {showDropdown && !showPicker && (() => {
        const query = value.trimStart().toLowerCase()
        const matches = COMMANDS.filter((c) => c.cmd.startsWith(query))
        if (matches.length === 0) return null
        return (
          <div className="absolute bottom-full left-4 right-4 mb-2 bg-raised border border-line rounded-xl overflow-hidden shadow-xl">
            {matches.map((c) => (
              <button
                key={c.cmd}
                onClick={() => selectCommand(c.cmd)}
                className="w-full flex items-center gap-3 px-4 py-3 hover:bg-raised-2 transition-colors text-left"
              >
                <span className="font-mono text-sm text-applied shrink-0">{c.cmd}</span>
                <span className="text-xs text-dim">{c.description}</span>
              </button>
            ))}
          </div>
        )
      })()}

      {/* Active command badge + the job it is aimed at */}
      {activeCommand && (
        <div className="flex items-center gap-2 mb-2 flex-wrap">
          <span className="inline-flex items-center gap-1.5 bg-applied/15 border border-applied/30 text-applied text-xs font-mono px-3 py-1 rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-applied dot-live" />
            {activeCommand.cmd}
          </span>

          {selectedApp ? (
            <span className="inline-flex items-center gap-2 bg-raised-2 border border-line text-xs px-3 py-1 rounded-full">
              <span className="text-fg">{selectedApp.job_title}</span>
              <span className="text-faint">·</span>
              <span className="text-dim">{selectedApp.company}</span>
              <button
                onClick={() => setShowPicker(true)}
                className="text-faint hover:text-fg transition-colors"
                aria-label="Change application"
              >
                change
              </button>
            </span>
          ) : (
            <button
              onClick={() => setShowPicker(true)}
              className="text-xs text-faint hover:text-dim underline underline-offset-2"
            >
              choose an application
            </button>
          )}

          <span className="text-xs text-faint">{activeCommand.description}</span>
        </div>
      )}

      <div className="flex gap-3 items-end">
        <textarea
          ref={textareaRef}
          rows={1}
          value={value}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          disabled={disabled}
          placeholder="Message JobReady… or type / for commands"
          className="flex-1 resize-none bg-raised-2 text-fg placeholder:text-faint rounded-xl px-4 py-3 text-sm outline-none focus:ring-1 focus:ring-applied border border-line disabled:opacity-50"
          style={{ maxHeight: 160, overflowY: 'auto' }}
        />
        <button
          onClick={submit}
          disabled={disabled || !value.trim()}
          className="cta shrink-0 rounded-xl px-4 py-3 text-sm disabled:opacity-40 disabled:cursor-not-allowed disabled:shadow-none"
        >
          Send
        </button>
      </div>
      <p className="mt-2 text-xs text-faint">
        Type <span className="font-mono text-dim">/</span> to see available commands · Shift+Enter for new line
      </p>
    </div>
  )
}
