import { useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  Button,
  Card,
  ErrorBanner,
  Field,
  StatusDot,
  Tag,
} from "~/components/ui";
import type { NormalizedError } from "~/api/errors";
import type {
  ApplicationStage,
  GeneratedDocumentType,
  JobApplication,
} from "~/api/schemas";
import {
  useCreateApplicationMutation,
  useDeleteApplicationMutation,
  useDeleteRecommendationMutation,
  useExtractJobPostingMutation,
  useListApplicationEventsQuery,
  useListApplicationsQuery,
  useListRecommendationsQuery,
  useUpdateApplicationMutation,
} from "~/services/applications/applicationsApi";
import { STAGES, stageMeta } from "~/services/applications/stages";
import {
  useDeleteDocumentMutation,
  useGetDocumentsQuery,
} from "~/services/documents/documentsApi";

interface FormState {
  company: string;
  job_title: string;
  job_description: string;
  job_url: string;
  notes: string;
  stage: ApplicationStage;
}

const EMPTY_FORM: FormState = {
  company: "",
  job_title: "",
  job_description: "",
  job_url: "",
  notes: "",
  stage: "draft",
};

/** A subtle asterisk marking a mandatory field. */
function Req() {
  return <span className="text-interview"> *</span>;
}

/** Create/edit form, shown as an overlay panel. */
function ApplicationForm({
  initial,
  editing,
  onClose,
}: {
  initial: FormState;
  editing: JobApplication | null;
  onClose: () => void;
}) {
  const [form, setForm] = useState<FormState>(initial);
  const [create, { isLoading: creating, error: createError }] =
    useCreateApplicationMutation();
  const [update, { isLoading: updating, error: updateError }] =
    useUpdateApplicationMutation();
  const [
    extract,
    { isLoading: extracting, error: extractError, reset: resetExtract },
  ] = useExtractJobPostingMutation();

  const set =
    (key: keyof FormState) =>
    (
      e: React.ChangeEvent<
        HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
      >,
    ) =>
      setForm((f) => ({ ...f, [key]: e.target.value }));

  async function onAutofill() {
    if (!form.job_url.trim()) return;
    try {
      const extracted = await extract({ url: form.job_url.trim() }).unwrap();
      // Pre-fill only — every field stays editable, and nulls leave fields untouched.
      setForm((f) => ({
        ...f,
        company: extracted.company ?? f.company,
        job_title: extracted.job_title ?? f.job_title,
        job_description: extracted.job_description ?? f.job_description,
      }));
    } catch {
      // error is rendered from the mutation state; manual entry stays available
    }
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    const body = {
      company: form.company.trim(),
      job_title: form.job_title.trim(),
      job_description: form.job_description.trim(),
      job_url: form.job_url.trim() || undefined,
      notes: form.notes || undefined,
      stage: form.stage,
    };
    try {
      if (editing) {
        await update({ id: editing.id, body }).unwrap();
      } else {
        await create(body).unwrap();
      }
      onClose();
    } catch {
      // error is rendered from the mutation state
    }
  }

  return (
    <div
      className="fixed inset-0 z-40 flex justify-end bg-black/50"
      onClick={onClose}
    >
      <Card
        className="anim-rise h-full w-full max-w-lg overflow-y-auto rounded-none border-y-0 border-r-0 p-6"
        onClick={(e) => e.stopPropagation()}
        header={
          <>
            <Tag className="text-dim">
              {editing ? "Edit application" : "New application"}
            </Tag>
            <button
              onClick={onClose}
              className="tag text-faint transition hover:text-fg"
            >
              Close
            </button>
          </>
        }
      >
        <form onSubmit={onSubmit} className="space-y-4 px-1 py-5">
          {/* Optional first step: paste the posting URL and let the assistant fill the form. */}
          <div className="rounded-lg border border-line bg-raised-2/40 p-3.5">
            <Field
              id="job_url"
              label="Job URL — optional first step"
              type="url"
              value={form.job_url}
              onChange={(e) => {
                resetExtract();
                set("job_url")(e);
              }}
              placeholder="https://…"
              trailing={
                <Button
                  type="button"
                  variant="ghost"
                  className="shrink-0 px-3 py-1 text-xs"
                  loading={extracting}
                  disabled={!form.job_url.trim() || extracting}
                  onClick={onAutofill}
                >
                  Autofill
                </Button>
              }
            />
            <p className="mt-1.5 text-xs text-faint">
              Paste the posting link and the assistant fills in company, role,
              and description.
            </p>
            {extractError != null && (
              <div className="mt-2 space-y-1">
                <ErrorBanner error={extractError as NormalizedError} />
                <p className="text-xs text-faint">
                  You can still fill in the details manually below.
                </p>
              </div>
            )}
          </div>
          <Field
            id="company"
            label={
              <>
                Company
                <Req />
              </>
            }
            required
            value={form.company}
            onChange={set("company")}
          />
          <Field
            id="job_title"
            label={
              <>
                Role
                <Req />
              </>
            }
            required
            value={form.job_title}
            onChange={set("job_title")}
          />
          <div>
            <label
              htmlFor="job_description"
              className="tag mb-1.5 block text-dim"
            >
              Job description
              <Req />
            </label>
            <textarea
              id="job_description"
              rows={6}
              required
              value={form.job_description}
              onChange={set("job_description")}
              placeholder="Paste the posting — the assistant uses it to tailor documents."
              className="field w-full rounded-lg px-3.5 py-2.5 text-[15px] text-fg placeholder:text-faint"
            />
          </div>
          <div>
            <label htmlFor="notes" className="tag mb-1.5 block text-dim">
              Notes
            </label>
            <textarea
              id="notes"
              rows={2}
              value={form.notes}
              onChange={set("notes")}
              className="field w-full rounded-lg px-3.5 py-2.5 text-[15px] text-fg placeholder:text-faint"
            />
          </div>
          <div>
            <label htmlFor="stage" className="tag mb-1.5 block text-dim">
              Stage
            </label>
            <select
              id="stage"
              value={form.stage}
              onChange={set("stage")}
              className="field w-full rounded-lg px-3.5 py-2.5 text-[15px] text-fg"
            >
              {STAGES.map((s) => (
                <option key={s.value} value={s.value}>
                  {s.label}
                </option>
              ))}
            </select>
            {!editing && (
              <p className="mt-1.5 text-xs text-faint">
                Starts in Draft — you&apos;re preparing this application. Pick
                another stage if you&apos;ve already sent it.
              </p>
            )}
          </div>
          <ErrorBanner
            error={(createError ?? updateError) as NormalizedError | undefined}
          />
          <Button
            type="submit"
            className="w-full"
            loading={creating || updating}
          >
            {editing ? "Save changes" : "Track application"}
          </Button>
        </form>
      </Card>
    </div>
  );
}

/** Cover letter / resume tab body inside the detail drawer. */
function DocumentsTab({
  application,
  type,
}: {
  application: JobApplication;
  type: GeneratedDocumentType;
}) {
  const navigate = useNavigate();
  const { data, isLoading } = useGetDocumentsQuery({
    applicationId: application.id,
  });
  const [deleteDocument] = useDeleteDocumentMutation();
  const docs = (data?.items ?? []).filter((d) => d.type === type);
  const label = type === "cover_letter" ? "cover letter" : "resume";
  const command = type === "cover_letter" ? "/cover_letter" : "/resume_tailor";

  if (isLoading) return <p className="text-sm text-dim">Loading…</p>;

  if (docs.length === 0) {
    return (
      <div className="py-6 text-center">
        <p className="text-sm text-dim">No {label} yet for this application.</p>
        <Button
          className="mt-4 px-4 py-2 text-sm"
          onClick={() =>
            navigate("/chat", {
              state: {
                prefill: `${command} for my ${application.job_title} application at ${application.company} (application id: ${application.id}).${application.job_description ? ` Job description: ${application.job_description}` : ""}`,
              },
            })
          }
        >
          Generate with Assistant
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {docs.map((doc) => (
        <div
          key={doc.id}
          className="rounded-lg border border-line bg-raised-2/40 p-4"
        >
          <div className="mb-2 flex items-center justify-between">
            <Tag className="text-faint">
              {new Date(doc.created_at).toLocaleString()}
            </Tag>
            <span className="flex gap-3">
              <button
                className="tag text-faint transition hover:text-fg"
                onClick={() => navigator.clipboard.writeText(doc.content)}
              >
                Copy
              </button>
              <button
                className="tag text-faint transition hover:text-interview"
                onClick={() => deleteDocument(doc.id)}
              >
                Delete
              </button>
            </span>
          </div>
          <pre className="max-h-96 overflow-y-auto whitespace-pre-wrap font-sans text-sm text-fg">
            {doc.content}
          </pre>
        </div>
      ))}
    </div>
  );
}

/** Timeline of an application: email-detected and manual events, newest first. */
function TimelineTab({ application }: { application: JobApplication }) {
  const { data, isLoading } = useListApplicationEventsQuery(application.id);
  const events = data?.items ?? [];

  if (isLoading) return <p className="text-sm text-dim">Loading timeline…</p>;
  if (events.length === 0) {
    return (
      <p className="text-sm text-dim">
        No timeline events yet. Stage changes and detected emails will show up
        here with their exact dates.
      </p>
    );
  }

  return (
    <ol className="space-y-4">
      {events.map((event) => (
        <li
          key={event.id}
          className="rounded-lg border border-line bg-raised-2/40 p-4"
        >
          <div className="mb-1 flex items-center justify-between">
            <span className="text-sm font-medium">{event.title}</span>
            <Tag className="text-faint">
              {new Date(event.occurred_at).toLocaleString()}
            </Tag>
          </div>
          {event.description && (
            <p className="text-sm text-dim">{event.description}</p>
          )}
          <div className="mt-2 flex items-center gap-2">
            <Tag className="text-faint">
              {event.event_type.replace(/_/g, " ")}
            </Tag>
            <Tag className="text-faint">
              {event.source === "email" ? "from email" : "manual"}
            </Tag>
            {event.stage_from && event.stage_to && (
              <Tag className="text-dim">
                {stageMeta(event.stage_from).label} →{" "}
                {stageMeta(event.stage_to).label}
              </Tag>
            )}
          </div>
        </li>
      ))}
    </ol>
  );
}

/** Next-best-action items (recommendations), e.g. follow-ups suggested from detected emails. */
function ActionItemsTab({ application }: { application: JobApplication }) {
  const { data, isLoading } = useListRecommendationsQuery(application.id);
  const [deleteRecommendation] = useDeleteRecommendationMutation();
  const items = data?.items ?? [];

  if (isLoading)
    return <p className="text-sm text-dim">Loading action items…</p>;
  if (items.length === 0) {
    return (
      <p className="text-sm text-dim">
        No action items right now. Suggestions like following up or preparing
        for an interview appear here when they're derived from your emails.
      </p>
    );
  }

  return (
    <ul className="space-y-4">
      {items.map((item) => (
        <li
          key={item.id}
          className="rounded-lg border border-line bg-raised-2/40 p-4"
        >
          <div className="mb-1 flex items-center justify-between">
            <Tag className="text-faint">
              {new Date(item.created_at).toLocaleString()}
            </Tag>
            <button
              className="tag text-faint transition hover:text-interview"
              onClick={() =>
                deleteRecommendation({
                  applicationId: application.id,
                  recommendationId: item.id,
                })
              }
            >
              Dismiss
            </button>
          </div>
          <p className="text-sm font-medium">{item.recommended_action}</p>
          <p className="mt-1 text-sm text-dim">{item.insight}</p>
        </li>
      ))}
    </ul>
  );
}

type DrawerTab =
  "overview" | "timeline" | "actions" | "cover_letter" | "resume";

function DetailDrawer({
  application,
  onEdit,
  onClose,
}: {
  application: JobApplication;
  onEdit: () => void;
  onClose: () => void;
}) {
  const [tab, setTab] = useState<DrawerTab>("overview");
  const meta = stageMeta(application.stage);
  const navigate = useNavigate();

  const TABS: { key: DrawerTab; label: string }[] = [
    { key: "overview", label: "Overview" },
    { key: "timeline", label: "Timeline" },
    { key: "actions", label: "Action items" },
    { key: "cover_letter", label: "Cover letter" },
    { key: "resume", label: "Resume" },
  ];

  return (
    <div
      className="fixed inset-0 z-40 flex justify-end bg-black/50"
      onClick={onClose}
    >
      <Card
        className="anim-rise h-full w-full max-w-xl overflow-y-auto rounded-none border-y-0 border-r-0"
        onClick={(e) => e.stopPropagation()}
        header={
          <>
            <span className="flex items-center gap-2.5">
              <StatusDot color={meta.color} halo />
              <span>
                <span className="block text-sm font-medium">
                  {application.company}
                </span>
                <span className="block text-xs text-dim">
                  {application.job_title}
                </span>
              </span>
            </span>
            <span className="flex items-center gap-3">
              <Tag className="text-dim">{meta.label}</Tag>
              <button
                onClick={onClose}
                className="tag text-faint transition hover:text-fg"
              >
                Close
              </button>
            </span>
          </>
        }
      >
        <div className="flex gap-1 border-b border-line px-4 pt-3">
          {TABS.map((t) => (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`rounded-t-lg px-3.5 py-2 text-sm transition ${
                tab === t.key
                  ? "border border-b-0 border-line bg-raised-2 text-fg"
                  : "text-dim hover:text-fg"
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        <div className="p-6">
          {tab === "overview" && (
            <div className="space-y-5">
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <Tag className="text-faint">Applied</Tag>
                  <p className="mt-1">
                    {new Date(application.applied_at).toLocaleDateString()}
                  </p>
                </div>
                <div>
                  <Tag className="text-faint">Updated</Tag>
                  <p className="mt-1">
                    {new Date(application.updated_at).toLocaleDateString()}
                  </p>
                </div>
              </div>
              {application.job_url && (
                <div>
                  <Tag className="text-faint">Job posting</Tag>
                  <a
                    href={application.job_url}
                    target="_blank"
                    rel="noreferrer"
                    className="mt-1 block truncate text-sm text-offer hover:underline"
                  >
                    {application.job_url}
                  </a>
                </div>
              )}
              {application.job_description && (
                <div>
                  <Tag className="text-faint">Job description</Tag>
                  <p className="mt-1 max-h-64 overflow-y-auto whitespace-pre-wrap text-sm text-dim">
                    {application.job_description}
                  </p>
                </div>
              )}
              {application.notes && (
                <div>
                  <Tag className="text-faint">Notes</Tag>
                  <p className="mt-1 whitespace-pre-wrap text-sm text-dim">
                    {application.notes}
                  </p>
                </div>
              )}
              <div className="flex flex-wrap gap-2">
                <Button
                  variant="ghost"
                  className="px-4 py-2 text-sm"
                  onClick={onEdit}
                >
                  Edit application
                </Button>
                {application.job_description?.trim() && (
                  <Button
                    className="px-4 py-2 text-sm"
                    onClick={() =>
                      navigate("/chat", {
                        state: {
                          tab: "interview",
                          presetApplicationId: application.id,
                        },
                      })
                    }
                  >
                    Practice interview
                  </Button>
                )}
              </div>
            </div>
          )}
          {tab === "timeline" && <TimelineTab application={application} />}
          {tab === "actions" && <ActionItemsTab application={application} />}
          {tab === "cover_letter" && (
            <DocumentsTab application={application} type="cover_letter" />
          )}
          {tab === "resume" && (
            <DocumentsTab application={application} type="resume" />
          )}
        </div>
      </Card>
    </div>
  );
}

export default function ApplicationsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { data, isLoading } = useListApplicationsQuery();
  const [deleteApplication] = useDeleteApplicationMutation();

  const [filter, setFilter] = useState<ApplicationStage | "all">("all");
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<JobApplication | null>(null);
  const [openId, setOpenId] = useState<string | null>(null);

  const items = data?.items ?? [];
  // Client-side stage filter for now; switch to the server-side `stage` query
  // param once the application-service plan lands it.
  const filtered = useMemo(
    () => (filter === "all" ? items : items.filter((a) => a.stage === filter)),
    [items, filter],
  );
  const open = items.find((a) => a.id === openId) ?? null;

  // Entry points from Jobs page (?create=1&company=…&role=…) and Dashboard (?open=<id>).
  useEffect(() => {
    if (searchParams.get("create") === "1") {
      setEditing(null);
      setShowForm(true);
    }
    const openParam = searchParams.get("open");
    if (openParam) setOpenId(openParam);
  }, [searchParams]);

  const prefill: FormState = {
    ...EMPTY_FORM,
    company: searchParams.get("company") ?? "",
    job_title: searchParams.get("role") ?? "",
    job_description: searchParams.get("description") ?? "",
    job_url: searchParams.get("url") ?? "",
  };

  function closeOverlays() {
    setShowForm(false);
    setEditing(null);
    setOpenId(null);
    if ([...searchParams.keys()].length > 0)
      setSearchParams({}, { replace: true });
  }

  return (
    <div className="h-full overflow-y-auto">
      <div className="mx-auto max-w-5xl px-8 py-8">
        <div className="mb-6 flex items-end justify-between">
          <div>
            <h1 className="font-display text-2xl font-semibold tracking-tight">
              Applications
            </h1>
            <p className="mt-1 text-sm text-dim">
              Every application you&apos;re tracking
            </p>
          </div>
          <Button
            className="px-4 py-2 text-sm"
            onClick={() => {
              setEditing(null);
              setShowForm(true);
            }}
          >
            New application
          </Button>
        </div>

        <div className="mb-4 flex flex-wrap gap-2">
          {[
            { value: "all" as const, label: "All", color: "var(--color-dim)" },
            ...STAGES,
          ].map((s) => (
            <button
              key={s.value}
              onClick={() => setFilter(s.value)}
              className={`flex items-center gap-2 rounded-full border px-3.5 py-1.5 text-xs transition ${
                filter === s.value
                  ? "border-line bg-raised-2 text-fg"
                  : "border-transparent text-dim hover:text-fg"
              }`}
            >
              {s.value !== "all" && (
                <StatusDot color={s.color} className="h-2 w-2" />
              )}
              {s.label}
            </button>
          ))}
        </div>

        <Card>
          {isLoading && <p className="p-6 text-sm text-dim">Loading…</p>}
          {!isLoading && filtered.length === 0 && (
            <p className="p-8 text-center text-sm text-dim">
              {filter === "all"
                ? "Nothing tracked yet — add your first application."
                : "No applications in this stage yet."}
            </p>
          )}
          {filtered.length > 0 && (
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-line">
                  <th className="tag px-5 py-3 font-normal text-faint">
                    Company
                  </th>
                  <th className="tag px-5 py-3 font-normal text-faint">Role</th>
                  <th className="tag px-5 py-3 font-normal text-faint">
                    Stage
                  </th>
                  <th className="tag px-5 py-3 font-normal text-faint">
                    Applied
                  </th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {filtered.map((a) => {
                  const meta = stageMeta(a.stage);
                  return (
                    <tr
                      key={a.id}
                      className="cursor-pointer transition hover:bg-raised-2/40"
                      onClick={() => setOpenId(a.id)}
                    >
                      <td className="px-5 py-3.5 font-medium">{a.company}</td>
                      <td className="px-5 py-3.5 text-dim">{a.job_title}</td>
                      <td className="px-5 py-3.5">
                        <span className="flex items-center gap-2">
                          <StatusDot color={meta.color} />
                          <Tag className="text-dim">{meta.label}</Tag>
                        </span>
                      </td>
                      <td className="px-5 py-3.5 font-mono text-xs text-faint">
                        {new Date(a.applied_at).toLocaleDateString()}
                      </td>
                      <td className="px-5 py-3.5 text-right">
                        <span className="flex justify-end gap-3">
                          <button
                            className="tag text-faint transition hover:text-fg"
                            onClick={(e) => {
                              e.stopPropagation();
                              setEditing(a);
                              setShowForm(true);
                            }}
                          >
                            Edit
                          </button>
                          <button
                            className="tag text-faint transition hover:text-interview"
                            onClick={(e) => {
                              e.stopPropagation();
                              if (
                                window.confirm(
                                  `Delete the ${a.company} application?`,
                                )
                              ) {
                                deleteApplication(a.id);
                              }
                            }}
                          >
                            Delete
                          </button>
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </Card>
      </div>

      {showForm && (
        <ApplicationForm
          initial={
            editing
              ? {
                  company: editing.company,
                  job_title: editing.job_title,
                  job_description: editing.job_description ?? "",
                  job_url: editing.job_url ?? "",
                  notes: editing.notes ?? "",
                  stage: editing.stage,
                }
              : prefill
          }
          editing={editing}
          onClose={closeOverlays}
        />
      )}
      {open && !showForm && (
        <DetailDrawer
          application={open}
          onEdit={() => {
            setEditing(open);
            setShowForm(true);
          }}
          onClose={closeOverlays}
        />
      )}
    </div>
  );
}
