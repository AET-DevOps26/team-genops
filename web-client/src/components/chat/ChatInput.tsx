import { useState, useRef, KeyboardEvent } from "react";

const COMMANDS = [
  {
    cmd: "/cover_letter",
    label: "Cover Letter",
    description: "Generate a tailored cover letter",
  },
  {
    cmd: "/resume_tailor",
    label: "Resume Tailor",
    description: "Tailor your resume for a role",
  },
  {
    cmd: "/fit_analysis",
    label: "Fit Analysis",
    description: "Analyse your fit and skill gaps",
  },
];

interface Props {
  onSend: (message: string) => void;
  disabled?: boolean;
  /** Initial input handed off by another page (e.g. an application's document tab). */
  prefill?: string;
}

export function ChatInput({ onSend, disabled, prefill }: Props) {
  const [value, setValue] = useState(prefill ?? "");
  const [showDropdown, setShowDropdown] = useState(false);
  const [activeCommand, setActiveCommand] = useState<
    (typeof COMMANDS)[0] | null
  >(
    prefill
      ? (COMMANDS.find((c) => prefill.toLowerCase().includes(c.cmd)) ?? null)
      : null,
  );
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  function handleChange(e: React.ChangeEvent<HTMLTextAreaElement>) {
    const val = e.target.value;
    setValue(val);

    // Keep dropdown open while typing a command (starts with / and no space yet)
    const trimmed = val.trimStart();
    setShowDropdown(trimmed.startsWith("/") && !trimmed.includes(" "));

    // Detect a fully typed command anywhere in the message
    const matched = COMMANDS.find((c) => val.toLowerCase().includes(c.cmd));
    setActiveCommand(matched ?? null);
  }

  function handleKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      submit();
    }
    if (e.key === "Escape") {
      setShowDropdown(false);
    }
  }

  function selectCommand(cmd: string) {
    setValue(cmd + " ");
    setShowDropdown(false);
    setActiveCommand(COMMANDS.find((c) => c.cmd === cmd) ?? null);
    textareaRef.current?.focus();
  }

  function submit() {
    const msg = value.trim();
    if (!msg || disabled) return;
    onSend(msg);
    setValue("");
    setActiveCommand(null);
    setShowDropdown(false);
  }

  return (
    <div className="border-t border-line p-4 relative">
      {/* Command dropdown */}
      {showDropdown &&
        (() => {
          const query = value.trimStart().toLowerCase();
          const matches = COMMANDS.filter((c) => c.cmd.startsWith(query));
          if (matches.length === 0) return null;
          return (
            <div className="absolute bottom-full left-4 right-4 mb-2 bg-raised border border-line rounded-xl overflow-hidden shadow-xl">
              {matches.map((c) => (
                <button
                  key={c.cmd}
                  onClick={() => selectCommand(c.cmd)}
                  className="w-full flex items-center gap-3 px-4 py-3 hover:bg-raised-2 transition-colors text-left"
                >
                  <span className="font-mono text-sm text-applied shrink-0">
                    {c.cmd}
                  </span>
                  <span className="text-xs text-dim">{c.description}</span>
                </button>
              ))}
            </div>
          );
        })()}

      {/* Active command badge */}
      {activeCommand && (
        <div className="flex items-center gap-2 mb-2">
          <span className="inline-flex items-center gap-1.5 bg-applied/15 border border-applied/30 text-applied text-xs font-mono px-3 py-1 rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-applied dot-live" />
            {activeCommand.cmd}
          </span>
          <span className="text-xs text-faint">
            {activeCommand.description}
          </span>
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
          style={{ maxHeight: 160, overflowY: "auto" }}
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
        Type <span className="font-mono text-dim">/</span> to see available
        commands · Shift+Enter for new line
      </p>
    </div>
  );
}
