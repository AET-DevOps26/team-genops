import { useNavigate } from "react-router-dom";
import { Button, Card, Tag } from "~/components/ui";

// PLACEHOLDER DATA — the jobs page is frontend-only for now (no jobs backend).
// "Apply" pre-fills the create-application form on /applications.
const SAMPLE_JOBS = [
  {
    company: "Datadog",
    role: "Software Engineer, Backend",
    location: "Paris · Hybrid",
    type: "Full-time",
    tags: ["Go", "Kubernetes", "Distributed systems"],
    description:
      "Build and scale the ingestion pipeline processing trillions of events per day.",
  },
  {
    company: "Celonis",
    role: "Working Student, Platform",
    location: "Munich · On-site",
    type: "Working student",
    tags: ["Java", "Spring Boot", "PostgreSQL"],
    description:
      "Support the core platform team building process-mining infrastructure.",
  },
  {
    company: "Zalando",
    role: "Junior Backend Engineer",
    location: "Berlin · Hybrid",
    type: "Full-time",
    tags: ["Java", "AWS", "Microservices"],
    description: "Join the checkout team owning high-traffic order services.",
  },
  {
    company: "Personio",
    role: "Software Engineer, Intern",
    location: "Munich · Hybrid",
    type: "Internship",
    tags: ["Kotlin", "React", "CI/CD"],
    description:
      "Ship features end-to-end across the HR platform with a senior mentor.",
  },
  {
    company: "Stripe",
    role: "Backend Engineer, Payments",
    location: "Remote · EU",
    type: "Full-time",
    tags: ["Ruby", "Java", "APIs"],
    description:
      "Design APIs used by millions of businesses to move money safely.",
  },
  {
    company: "SAP",
    role: "DevOps Engineer, Cloud",
    location: "Walldorf · Hybrid",
    type: "Full-time",
    tags: ["Kubernetes", "Terraform", "Observability"],
    description:
      "Operate the delivery platform for one of the largest B2B clouds.",
  },
];

export default function JobsPage() {
  const navigate = useNavigate();

  function apply(job: (typeof SAMPLE_JOBS)[number]) {
    const params = new URLSearchParams({
      create: "1",
      company: job.company,
      role: job.role,
      description: job.description,
    });
    navigate(`/applications?${params}`);
  }

  return (
    <div className="h-full overflow-y-auto">
      <div className="mx-auto max-w-5xl px-8 py-8">
        <div className="mb-6">
          <h1 className="font-display text-2xl font-semibold tracking-tight">
            Jobs
          </h1>
          <p className="mt-1 text-sm text-dim">
            Curated sample roles — live job feeds are on the roadmap.{" "}
            <span className="text-faint">(placeholder data)</span>
          </p>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          {SAMPLE_JOBS.map((job) => (
            <Card
              key={`${job.company}-${job.role}`}
              className="flex flex-col p-5"
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h2 className="text-[15px] font-medium">{job.role}</h2>
                  <p className="mt-0.5 text-sm text-dim">{job.company}</p>
                </div>
                <Tag className="shrink-0 text-faint">{job.type}</Tag>
              </div>
              <p className="mt-3 text-sm text-dim">{job.description}</p>
              <div className="mt-3 flex flex-wrap gap-1.5">
                {job.tags.map((tag) => (
                  <span
                    key={tag}
                    className="rounded-full bg-raised-2 px-2.5 py-1 text-xs text-dim"
                  >
                    {tag}
                  </span>
                ))}
              </div>
              <div className="mt-4 flex items-center justify-between border-t border-line pt-4">
                <span className="text-xs text-faint">{job.location}</span>
                <Button
                  className="px-4 py-2 text-sm"
                  onClick={() => apply(job)}
                >
                  Apply with JobReady
                </Button>
              </div>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
