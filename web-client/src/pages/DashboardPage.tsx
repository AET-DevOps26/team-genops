import { Link, useNavigate } from "react-router-dom";
import { Button, Card, StatusDot, Tag } from "~/components/ui";
import { useListApplicationsQuery } from "~/services/applications/applicationsApi";
import { STAGES, stageMeta } from "~/services/applications/stages";
import type { JobApplication } from "~/api/schemas";

function StatTile({
  label,
  value,
  detail,
}: {
  label: string;
  value: string;
  detail: string;
}) {
  return (
    <Card className="p-5">
      <Tag className="text-faint">{label}</Tag>
      <p className="mt-2 font-display text-3xl font-semibold tracking-tight">
        {value}
      </p>
      <p className="mt-1 text-xs text-dim">{detail}</p>
    </Card>
  );
}

// Counts are derived client-side from the list for now. Once the application
// service ships GET /applications/summary (application-service plan), swap the
// derivation for that endpoint.
function deriveStats(items: JobApplication[]) {
  const byStage = Object.fromEntries(STAGES.map((s) => [s.value, 0])) as Record<
    string,
    number
  >;
  for (const item of items)
    byStage[item.stage] = (byStage[item.stage] ?? 0) + 1;
  const active = items.length - byStage.closed;
  const responded = byStage.interview + byStage.offer;
  const responseRate = items.length
    ? Math.round((responded / items.length) * 100)
    : 0;
  return { byStage, active, responseRate };
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const { data, isLoading } = useListApplicationsQuery();
  const items = data?.items ?? [];
  const { byStage, active, responseRate } = deriveStats(items);
  const recent = items.slice(0, 5);

  return (
    <div className="h-full overflow-y-auto">
      <div className="mx-auto max-w-5xl px-8 py-8">
        <div className="mb-7 flex items-end justify-between">
          <div>
            <h1 className="font-display text-2xl font-semibold tracking-tight">
              Dashboard
            </h1>
            <p className="mt-1 text-sm text-dim">Your job search at a glance</p>
          </div>
          <Button
            className="px-4 py-2 text-sm"
            onClick={() => navigate("/applications?create=1")}
          >
            New application
          </Button>
        </div>

        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          <StatTile
            label="Active"
            value={String(active)}
            detail={`${items.length} total application${items.length === 1 ? "" : "s"}`}
          />
          <StatTile
            label="Interviews"
            value={String(byStage.interview)}
            detail="in the pipeline"
          />
          <StatTile
            label="Offers"
            value={String(byStage.offer)}
            detail="on the table"
          />
          <StatTile
            label="Response rate"
            value={`${responseRate}%`}
            detail="reached interview or offer"
          />
        </div>

        <div className="mt-8 mb-3 flex items-baseline justify-between">
          <h2 className="font-display text-lg font-semibold tracking-tight">
            Pipeline
          </h2>
          <Link
            to="/applications"
            className="tag text-faint transition hover:text-fg"
          >
            View all
          </Link>
        </div>
        <div className="grid grid-cols-2 gap-3 md:grid-cols-5">
          {STAGES.map((stage) => {
            const inStage = items.filter((a) => a.stage === stage.value);
            return (
              <Card key={stage.value} className="p-4">
                <div className="flex items-center justify-between">
                  <span className="flex items-center gap-2">
                    <StatusDot color={stage.color} />
                    <Tag className="text-dim">{stage.label}</Tag>
                  </span>
                  <span className="font-mono text-sm text-dim">
                    {inStage.length}
                  </span>
                </div>
                <div className="mt-3 space-y-1">
                  {inStage.slice(0, 4).map((a) => (
                    <p key={a.id} className="truncate text-xs text-dim">
                      {a.company}
                    </p>
                  ))}
                  {inStage.length === 0 && (
                    <p className="text-xs text-faint">—</p>
                  )}
                </div>
              </Card>
            );
          })}
        </div>

        <div className="mt-8 grid gap-4 lg:grid-cols-[1fr_320px]">
          <Card className="p-5">
            <h2 className="mb-4 font-display text-lg font-semibold tracking-tight">
              Recent applications
            </h2>
            {isLoading && <p className="text-sm text-dim">Loading…</p>}
            {!isLoading && recent.length === 0 && (
              <p className="text-sm text-dim">
                Nothing tracked yet.{" "}
                <Link
                  to="/applications?create=1"
                  className="text-offer hover:underline"
                >
                  Add your first application
                </Link>{" "}
                or{" "}
                <Link to="/jobs" className="text-offer hover:underline">
                  browse jobs
                </Link>
                .
              </p>
            )}
            <div className="divide-y divide-line">
              {recent.map((a) => {
                const meta = stageMeta(a.stage);
                return (
                  <Link
                    key={a.id}
                    to={`/applications?open=${a.id}`}
                    className="flex items-center gap-3 py-3 transition hover:bg-raised-2/40"
                  >
                    <StatusDot color={meta.color} />
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm">
                        {a.company}
                      </span>
                      <span className="block truncate text-xs text-dim">
                        {a.job_title}
                      </span>
                    </span>
                    <Tag className="text-dim">{meta.label}</Tag>
                    <span className="font-mono text-xs text-faint">
                      {new Date(a.applied_at).toLocaleDateString()}
                    </span>
                  </Link>
                );
              })}
            </div>
          </Card>

          <Card className="p-5">
            <Tag className="text-faint">Assistant</Tag>
            <p className="mt-2 text-sm text-dim">
              Draft a tailored cover letter, tune your resume, or check your fit
              for a role.
            </p>
            <Button
              className="mt-4 w-full px-4 py-2.5 text-sm"
              onClick={() => navigate("/chat")}
            >
              Open AI Assistant
            </Button>
          </Card>
        </div>
      </div>
    </div>
  );
}
