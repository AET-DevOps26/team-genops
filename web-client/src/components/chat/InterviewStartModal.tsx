import { useState } from "react";
import type { NormalizedError } from "~/api/errors";
import { Button, Card, ErrorBanner, Tag } from "~/components/ui";
import { useListApplicationsQuery } from "~/services/applications/applicationsApi";
import {
  useCreateSessionMutation,
  type Session,
} from "~/services/chat/chatApi";

interface Props {
  onClose: () => void;
  onStarted: (session: Session) => void;
  /** Pre-select an application (e.g. launched from its detail drawer). */
  presetApplicationId?: string;
}

/**
 * Picks the application a mock interview will be tailored to, then creates the session.
 *
 * The interview cannot start without a job description on the application (and a complete
 * profile, enforced server-side): applications without one are shown disabled, and any
 * server-side precondition failure (422) surfaces its message inline.
 */
export function InterviewStartModal({ onClose, onStarted, presetApplicationId }: Props) {
  const { data, isLoading } = useListApplicationsQuery();
  const [createSession, { isLoading: creating }] = useCreateSessionMutation();
  const [selected, setSelected] = useState<string | null>(presetApplicationId ?? null);
  const [error, setError] = useState<NormalizedError | null>(null);

  const applications = data?.items ?? [];

  async function start() {
    if (!selected) return;
    setError(null);
    try {
      const session = await createSession({
        session_type: "mock_interview",
        application_id: selected,
      }).unwrap();
      onStarted(session);
    } catch (e) {
      setError(e as NormalizedError);
    }
  }

  return (
    <div
      className="fixed inset-0 z-40 flex items-center justify-center bg-black/50 p-4"
      onClick={onClose}
    >
      <Card
        className="anim-rise w-full max-w-md p-6"
        onClick={(e) => e.stopPropagation()}
        header={
          <>
            <Tag className="text-dim">Start a mock interview</Tag>
            <button
              onClick={onClose}
              className="tag text-faint transition hover:text-fg"
            >
              Close
            </button>
          </>
        }
      >
        <p className="mb-4 text-sm text-dim">
          Choose the role to interview for. Questions are tailored to that
          application's job description.
        </p>

        {error && (
          <div className="mb-4">
            <ErrorBanner error={error} />
          </div>
        )}

        {isLoading && <p className="text-sm text-faint">Loading applications…</p>}

        {!isLoading && applications.length === 0 && (
          <p className="text-sm text-faint">
            You have no job applications yet. Add one (with a job description)
            first, then come back to practise.
          </p>
        )}

        <div className="max-h-72 space-y-2 overflow-y-auto">
          {applications.map((app) => {
            const hasJd = Boolean(app.job_description?.trim());
            const isSelected = selected === app.id;
            return (
              <button
                key={app.id}
                type="button"
                disabled={!hasJd}
                onClick={() => setSelected(app.id)}
                className={`w-full rounded-lg border px-3 py-2.5 text-left transition ${
                  isSelected
                    ? "border-offer bg-offer/10"
                    : "border-line hover:bg-raised"
                } ${hasJd ? "" : "cursor-not-allowed opacity-50"}`}
              >
                <p className="text-sm font-medium text-fg">{app.job_title}</p>
                <p className="text-xs text-faint">{app.company}</p>
                {!hasJd && (
                  <p className="mt-1 text-xs text-interview">No job description</p>
                )}
              </button>
            );
          })}
        </div>

        <Button
          className="mt-5 w-full px-4 py-2.5 text-sm"
          disabled={!selected || creating}
          loading={creating}
          onClick={start}
        >
          Start interview
        </Button>
      </Card>
    </div>
  );
}
