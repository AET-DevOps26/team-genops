import type { ReactNode } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAppDispatch, useAppSelector } from "~/store/hooks";
import { logout } from "~/services/auth/authSlice";
import { Tag } from "~/components/ui";
import { useListApplicationsQuery } from "~/services/applications/applicationsApi";
import { useGetProfileQuery } from "~/services/profile/profileApi";

function NavItem({
  to,
  end,
  icon,
  label,
  badge,
}: {
  to: string;
  end?: boolean;
  icon: ReactNode;
  label: string;
  badge?: number;
}) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        `flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition ${
          isActive
            ? "bg-raised-2 text-fg"
            : "text-dim hover:bg-raised-2/60 hover:text-fg"
        }`
      }
    >
      <span className="grid h-4 w-4 place-items-center text-current">
        {icon}
      </span>
      <span className="flex-1">{label}</span>
      {badge !== undefined && badge > 0 && (
        <span className="rounded-full bg-raised-2 px-2 py-0.5 font-mono text-[11px] text-dim">
          {badge}
        </span>
      )}
    </NavLink>
  );
}

const ICONS = {
  dashboard: (
    <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden>
      <rect
        x="1.5"
        y="1.5"
        width="5.5"
        height="5.5"
        rx="1"
        stroke="currentColor"
        strokeWidth="1.3"
      />
      <rect
        x="9"
        y="1.5"
        width="5.5"
        height="5.5"
        rx="1"
        stroke="currentColor"
        strokeWidth="1.3"
      />
      <rect
        x="1.5"
        y="9"
        width="5.5"
        height="5.5"
        rx="1"
        stroke="currentColor"
        strokeWidth="1.3"
      />
      <rect
        x="9"
        y="9"
        width="5.5"
        height="5.5"
        rx="1"
        stroke="currentColor"
        strokeWidth="1.3"
      />
    </svg>
  ),
  applications: (
    <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden>
      <rect
        x="1.5"
        y="3"
        width="13"
        height="10.5"
        rx="1.5"
        stroke="currentColor"
        strokeWidth="1.3"
      />
      <path
        d="M5.5 3V2a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v1"
        stroke="currentColor"
        strokeWidth="1.3"
      />
    </svg>
  ),
  jobs: (
    <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden>
      <circle cx="7" cy="7" r="5" stroke="currentColor" strokeWidth="1.3" />
      <path
        d="m11 11 3.5 3.5"
        stroke="currentColor"
        strokeWidth="1.3"
        strokeLinecap="round"
      />
    </svg>
  ),
  profile: (
    <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden>
      <circle cx="8" cy="5" r="3" stroke="currentColor" strokeWidth="1.3" />
      <path
        d="M2.5 14c.8-2.6 3-4 5.5-4s4.7 1.4 5.5 4"
        stroke="currentColor"
        strokeWidth="1.3"
        strokeLinecap="round"
      />
    </svg>
  ),
  assistant: (
    <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden>
      <path
        d="M2 3.5A1.5 1.5 0 0 1 3.5 2h9A1.5 1.5 0 0 1 14 3.5v6a1.5 1.5 0 0 1-1.5 1.5H8l-3.5 3v-3h-1A1.5 1.5 0 0 1 2 9.5v-6Z"
        stroke="currentColor"
        strokeWidth="1.3"
        strokeLinejoin="round"
      />
    </svg>
  ),
};

/**
 * The product shell (per jobready-mockup.html): fixed sidebar with Overview and
 * Account sections, user footer, and the routed page in the main area.
 */
export function AppShell() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const user = useAppSelector((s) => s.auth.user);

  const { data: applications } = useListApplicationsQuery();
  // 404 = no profile yet → surface the onboarding banner (not a hard redirect).
  const { error: profileError } = useGetProfileQuery();
  const needsOnboarding =
    profileError != null &&
    "status" in profileError &&
    profileError.status === 404;

  return (
    <div className="flex h-screen bg-ink text-fg">
      <aside className="flex w-60 shrink-0 flex-col border-r border-line bg-raised/60 px-3 py-5">
        <div className="mb-6 flex items-center gap-2 px-3">
          <span className="grid h-7 w-7 place-items-center rounded-md bg-offer/15 text-offer">
            <svg
              width="15"
              height="15"
              viewBox="0 0 16 16"
              fill="none"
              aria-hidden
            >
              <rect
                x="1.5"
                y="1.5"
                width="4"
                height="13"
                rx="1"
                stroke="currentColor"
                strokeWidth="1.4"
              />
              <rect
                x="10.5"
                y="1.5"
                width="4"
                height="8"
                rx="1"
                stroke="currentColor"
                strokeWidth="1.4"
              />
            </svg>
          </span>
          <span className="font-display text-[15px] font-semibold tracking-tight">
            JobReady
          </span>
        </div>

        <Tag className="mb-2 px-3 text-faint">Overview</Tag>
        <nav className="mb-6 space-y-0.5">
          <NavItem to="/" end icon={ICONS.dashboard} label="Dashboard" />
          <NavItem
            to="/applications"
            icon={ICONS.applications}
            label="Applications"
            badge={applications?.items.length}
          />
          <NavItem to="/jobs" icon={ICONS.jobs} label="Jobs" />
        </nav>

        <Tag className="mb-2 px-3 text-faint">Account</Tag>
        <nav className="space-y-0.5">
          <NavItem to="/profile" icon={ICONS.profile} label="Profile" />
          <NavItem to="/chat" icon={ICONS.assistant} label="Assistant" />
        </nav>

        {needsOnboarding && (
          <button
            onClick={() => navigate("/onboarding")}
            className="anim-rise mt-6 rounded-lg border border-offer/30 bg-offer/10 px-3 py-2.5 text-left text-xs text-offer transition hover:bg-offer/15"
          >
            <span className="font-medium">Complete your profile</span>
            <span className="mt-0.5 block text-offer/70">
              Unlock tailored cover letters and fit analysis.
            </span>
          </button>
        )}

        <div className="flex-1" />

        <div className="flex items-center gap-2.5 border-t border-line px-3 pt-4">
          <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-raised-2 font-mono text-xs uppercase text-dim">
            {user?.email?.slice(0, 2)}
          </span>
          <div className="min-w-0 flex-1">
            <p className="truncate text-xs text-dim">{user?.email}</p>
            <button
              onClick={() => dispatch(logout())}
              className="tag text-faint transition hover:text-fg"
            >
              Sign out
            </button>
          </div>
        </div>
      </aside>

      <main className="h-screen min-w-0 flex-1 overflow-hidden">
        <Outlet />
      </main>
    </div>
  );
}
