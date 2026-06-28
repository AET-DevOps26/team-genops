import { useState, type FormEvent } from 'react'
import { useAppDispatch, useAppSelector } from '~/store/hooks'
import { clearError, login, register } from '~/services/auth/authSlice'
import { Button, ErrorBanner } from '~/components/ui'

type Mode = 'login' | 'register'

/** The four stages of JobReady's pipeline. The auth card lives on this board. */
const STAGES = [
  { key: 'applied', label: 'Applied', color: 'var(--color-applied)', count: 12 },
  { key: 'screen', label: 'Screen', color: 'var(--color-screen)', count: 5 },
  { key: 'interview', label: 'Interview', color: 'var(--color-interview)', count: 3 },
  { key: 'offer', label: 'Offer', color: 'var(--color-offer)', count: 1 },
] as const

/** Left rail: an ambient demo of the board the person is signing in to.
   The pipeline is illustrative — it doesn't track the auth form's state. */
function BoardRail() {
  return (
    <aside className="board-grid relative hidden overflow-hidden border-r border-line bg-ink lg:flex lg:w-[46%] lg:flex-col">
      <div className="relative z-10 flex h-full flex-col justify-between p-10 xl:p-14">
        <div className="anim-rise" style={{ animationDelay: '60ms' }}>
          <div className="flex items-center gap-2.5">
            <span className="grid h-7 w-7 place-items-center rounded-md bg-offer/15 text-offer">
              {/* board glyph */}
              <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden>
                <rect x="1.5" y="1.5" width="4" height="13" rx="1" stroke="currentColor" strokeWidth="1.4" />
                <rect x="10.5" y="1.5" width="4" height="8" rx="1" stroke="currentColor" strokeWidth="1.4" />
              </svg>
            </span>
            <span className="font-display text-[17px] font-semibold tracking-tight text-fg">JobReady</span>
          </div>
          <h1 className="mt-9 max-w-[15ch] font-display text-[2.6rem] font-semibold leading-[1.04] tracking-[-0.02em] text-fg xl:text-[3rem]">
            Every application on one board.
          </h1>
          <p className="mt-5 max-w-[34ch] text-[15px] leading-relaxed text-dim">
            Track each role from applied to offer — with an AI co-pilot for the email
            triage and cover letters that drain you.
          </p>
        </div>

        {/* The pipeline itself */}
        <div className="anim-rise mt-10 space-y-3" style={{ animationDelay: '160ms' }}>
          {STAGES.map((s) => {
            const active = s.key === 'interview'
            return (
              <div key={s.key} className="flex items-center gap-3">
                <span
                  className={`h-2.5 w-2.5 shrink-0 rounded-full ${active ? 'dot-live' : ''}`}
                  style={{ background: s.color, boxShadow: active ? `0 0 0 4px color-mix(in oklab, ${s.color} 18%, transparent)` : 'none' }}
                />
                <span className="tag text-dim">{s.label}</span>
                <span className="h-px flex-1 bg-line" />
                <span className="tag text-faint">{String(s.count).padStart(2, '0')}</span>
                {active && (
                  <span
                    className="tag rounded px-1.5 py-0.5"
                    style={{ color: s.color, background: `color-mix(in oklab, ${s.color} 14%, transparent)` }}
                  >
                    you
                  </span>
                )}
              </div>
            )
          })}
        </div>

        <div className="anim-rise flex items-center gap-6" style={{ animationDelay: '260ms' }}>
          <Stat value="21" label="tracked" />
          <span className="h-8 w-px bg-line" />
          <Stat value="3" label="interviewing" accent="var(--color-interview)" />
          <span className="h-8 w-px bg-line" />
          <Stat value="1" label="offer" accent="var(--color-offer)" />
        </div>
      </div>
    </aside>
  )
}

function Stat({ value, label, accent }: { value: string; label: string; accent?: string }) {
  return (
    <div>
      <div className="font-display text-2xl font-semibold tracking-tight" style={{ color: accent ?? 'var(--color-fg)' }}>
        {value}
      </div>
      <div className="tag mt-0.5 text-faint">{label}</div>
    </div>
  )
}

export default function AuthPage() {
  const [mode, setMode] = useState<Mode>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPw, setShowPw] = useState(false)

  const dispatch = useAppDispatch()
  const status = useAppSelector((s) => s.auth.status)
  const error = useAppSelector((s) => s.auth.error)
  const loading = status === 'authenticating'
  const done = status === 'authenticated'

  const isRegister = mode === 'register'

  function switchMode(next: Mode) {
    setMode(next)
    dispatch(clearError())
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    // The thunk drives status/error in the store; the cookie is set by the
    // server and never touched here. On success status becomes 'authenticated',
    // which is where the full app routes to the board.
    dispatch(isRegister ? register({ email, password }) : login({ email, password }))
  }

  return (
    <div className="flex min-h-screen bg-ink text-fg">
      <BoardRail />

      {/* Form side */}
      <main className="relative flex flex-1 items-center justify-center px-5 py-10 sm:px-8">
        {/* ambient stage glow behind the card */}
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0"
          style={{
            background:
              'radial-gradient(60% 50% at 70% 30%, color-mix(in oklab, var(--color-offer) 9%, transparent), transparent 70%)',
          }}
        />

        <div className="anim-card relative w-full max-w-[400px]">
          {/* brand on mobile (rail is hidden) */}
          <div className="mb-6 flex items-center gap-2.5 lg:hidden">
            <span className="grid h-7 w-7 place-items-center rounded-md bg-offer/15 text-offer">
              <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden>
                <rect x="1.5" y="1.5" width="4" height="13" rx="1" stroke="currentColor" strokeWidth="1.4" />
                <rect x="10.5" y="1.5" width="4" height="8" rx="1" stroke="currentColor" strokeWidth="1.4" />
              </svg>
            </span>
            <span className="font-display text-[17px] font-semibold tracking-tight">JobReady</span>
          </div>

          {/* The auth panel, styled as an application card on the board */}
          <div className="overflow-hidden rounded-2xl border border-line bg-raised shadow-[0_30px_80px_-30px_rgba(0,0,0,0.8)]">
            {/* card header — status tag + id, the tracker vernacular */}
            <div className="flex items-center justify-between border-b border-line px-6 py-4">
              <span className="inline-flex items-center gap-2">
                <span
                  className={`h-2 w-2 rounded-full ${isRegister ? '' : 'dot-live'}`}
                  style={{
                    background: isRegister ? 'var(--color-applied)' : 'var(--color-offer)',
                  }}
                />
                <span className="tag text-dim">{isRegister ? 'New applicant' : 'Returning'}</span>
              </span>
              <span className="tag text-faint">#JR-{isRegister ? '0001' : '0148'}</span>
            </div>

            <div className="px-6 pb-7 pt-6 sm:px-7">
              {done ? (
                <SuccessState isRegister={isRegister} />
              ) : (
                <>
                  <h2 className="font-display text-[1.6rem] font-semibold leading-tight tracking-[-0.01em]">
                    {isRegister ? 'Start your board' : 'Pick up where you left off'}
                  </h2>
                  <p className="mt-1.5 text-sm text-dim">
                    {isRegister
                      ? 'Create an account to start tracking applications.'
                      : 'Sign in to your job board.'}
                  </p>

                  {/* mode toggle — styled like choosing a lane */}
                  <div className="mt-6 grid grid-cols-2 gap-1 rounded-lg border border-line bg-ink/60 p-1">
                    <ToggleBtn active={!isRegister} onClick={() => switchMode('login')}>
                      Sign in
                    </ToggleBtn>
                    <ToggleBtn active={isRegister} onClick={() => switchMode('register')}>
                      Create account
                    </ToggleBtn>
                  </div>

                  <form onSubmit={onSubmit} className="mt-5 space-y-4" noValidate>
                    <div>
                      <label htmlFor="email" className="tag mb-1.5 block text-dim">
                        Email
                      </label>
                      <div className="field rounded-lg px-3.5 py-2.5">
                        <input
                          id="email"
                          type="email"
                          autoComplete="email"
                          required
                          placeholder="you@email.com"
                          value={email}
                          onChange={(e) => setEmail(e.target.value)}
                        />
                      </div>
                    </div>

                    <div>
                      <div className="mb-1.5 flex items-baseline justify-between">
                        <label htmlFor="password" className="tag block text-dim">
                          Password
                        </label>
                        {!isRegister && (
                          <a href="#" className="text-xs text-faint transition-colors hover:text-dim">
                            Forgot?
                          </a>
                        )}
                      </div>
                      <div className="field flex items-center gap-2 rounded-lg px-3.5 py-2.5">
                        <input
                          id="password"
                          type={showPw ? 'text' : 'password'}
                          autoComplete={isRegister ? 'new-password' : 'current-password'}
                          required
                          minLength={8}
                          placeholder={isRegister ? 'At least 8 characters' : 'Your password'}
                          value={password}
                          onChange={(e) => setPassword(e.target.value)}
                        />
                        <button
                          type="button"
                          onClick={() => setShowPw((v) => !v)}
                          className="tag shrink-0 text-faint transition-colors hover:text-dim"
                          aria-label={showPw ? 'Hide password' : 'Show password'}
                        >
                          {showPw ? 'Hide' : 'Show'}
                        </button>
                      </div>
                    </div>

                    <ErrorBanner error={error} />

                    <Button type="submit" loading={loading} className="w-full">
                      {loading
                        ? isRegister
                          ? 'Creating your account…'
                          : 'Opening your board…'
                        : isRegister
                          ? 'Create account'
                          : 'Open my board'}
                    </Button>
                  </form>

                  <p className="mt-5 text-center text-sm text-dim">
                    {isRegister ? 'Already tracking? ' : 'New here? '}
                    <button
                      onClick={() => switchMode(isRegister ? 'login' : 'register')}
                      className="font-medium text-fg underline-offset-4 transition-colors hover:text-offer hover:underline"
                    >
                      {isRegister ? 'Sign in' : 'Create an account'}
                    </button>
                  </p>
                </>
              )}
            </div>
          </div>

          <p className="mt-5 text-center text-xs leading-relaxed text-faint">
            By continuing you agree to JobReady&apos;s{' '}
            <a href="#" className="underline-offset-2 hover:text-dim hover:underline">
              Terms
            </a>{' '}
            and{' '}
            <a href="#" className="underline-offset-2 hover:text-dim hover:underline">
              Privacy Policy
            </a>
            .
          </p>
        </div>
      </main>
    </div>
  )
}

function ToggleBtn({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`rounded-md py-2 text-sm font-medium transition-colors ${
        active ? 'bg-raised-2 text-fg shadow-sm' : 'text-dim hover:text-fg'
      }`}
    >
      {children}
    </button>
  )
}

function SuccessState({ isRegister }: { isRegister: boolean }) {
  return (
    <div className="anim-rise py-6 text-center">
      <div className="mx-auto grid h-12 w-12 place-items-center rounded-full bg-offer/15 text-offer">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden>
          <path d="M5 13l4 4L19 7" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </div>
      <h2 className="mt-4 font-display text-xl font-semibold tracking-tight">
        {isRegister ? "You're all set" : "You're in"}
      </h2>
      <p className="mt-1.5 text-sm text-dim">
        {isRegister ? 'Your first card is on the board. Loading…' : 'Taking you to your board…'}
      </p>
    </div>
  )
}
