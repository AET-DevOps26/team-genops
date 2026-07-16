import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Card, ErrorBanner, Field } from "~/components/ui";
import type { NormalizedError } from "~/api/errors";
import type {
  Education,
  Language,
  ProfileAggregateResponse,
  Skill,
  WorkExperience,
} from "~/api/schemas";
import {
  useAddEducationMutation,
  useAddLanguageMutation,
  useAddSkillMutation,
  useAddWorkExperienceMutation,
  useDeleteEducationMutation,
  useDeleteLanguageMutation,
  useDeleteSkillMutation,
  useDeleteWorkExperienceMutation,
  useGetProfileQuery,
  useUpdateEducationMutation,
  useUpdateLanguageMutation,
  useUpdateSkillMutation,
  useUpdateWorkExperienceMutation,
  useUpsertProfileMutation,
} from "~/services/profile/profileApi";

// Wire enums straight from the spec (lowercase, match the DB CHECK constraints).
const SKILL_LEVELS = [
  "beginner",
  "intermediate",
  "advanced",
  "expert",
] as const;
const PROFICIENCIES = ["basic", "conversational", "fluent", "native"] as const;

function SectionCard({
  title,
  action,
  children,
}: {
  title: string;
  action?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <Card className="p-5">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="font-display text-lg font-semibold tracking-tight">
          {title}
        </h2>
        {action}
      </div>
      {children}
    </Card>
  );
}

function AddButton({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="tag text-faint transition hover:text-fg"
    >
      + {label}
    </button>
  );
}

function RowActions({
  onEdit,
  onDelete,
}: {
  onEdit: () => void;
  onDelete: () => void;
}) {
  return (
    <span className="flex shrink-0 gap-3">
      <button
        className="tag text-faint transition hover:text-fg"
        onClick={onEdit}
      >
        Edit
      </button>
      <button
        className="tag text-faint transition hover:text-interview"
        onClick={onDelete}
      >
        Delete
      </button>
    </span>
  );
}

// ───────────────────────── Basics ─────────────────────────

function BasicsSection({
  data,
}: {
  data: ProfileAggregateResponse | undefined;
}) {
  const profile = data?.profile;
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({
    first_name: "",
    last_name: "",
    bio: "",
    location: "",
    phone: "",
    website: "",
  });
  const [upsert, { isLoading, error }] = useUpsertProfileMutation();

  useEffect(() => {
    if (profile) {
      setForm({
        first_name: profile.first_name,
        last_name: profile.last_name,
        bio: profile.bio ?? "",
        location: profile.location ?? "",
        phone: profile.phone ?? "",
        website: profile.website ?? "",
      });
    }
  }, [profile]);

  const set =
    (key: keyof typeof form) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm((f) => ({ ...f, [key]: e.target.value }));

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      await upsert({
        first_name: form.first_name,
        last_name: form.last_name,
        bio: form.bio || undefined,
        location: form.location || undefined,
        phone: form.phone || undefined,
        website: form.website || undefined,
      }).unwrap();
      setEditing(false);
    } catch {
      /* rendered from mutation state */
    }
  }

  if (!profile || editing) {
    return (
      <SectionCard title="Basics">
        <form onSubmit={onSubmit} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field
              id="first_name"
              label="First name"
              required
              value={form.first_name}
              onChange={set("first_name")}
            />
            <Field
              id="last_name"
              label="Last name"
              required
              value={form.last_name}
              onChange={set("last_name")}
            />
            <Field
              id="location"
              label="Location"
              value={form.location}
              onChange={set("location")}
            />
            <Field
              id="phone"
              label="Phone"
              value={form.phone}
              onChange={set("phone")}
            />
          </div>
          <Field
            id="website"
            label="Website"
            type="url"
            value={form.website}
            onChange={set("website")}
            placeholder="https://…"
          />
          <div>
            <label htmlFor="bio" className="tag mb-1.5 block text-dim">
              Summary
            </label>
            <textarea
              id="bio"
              rows={3}
              value={form.bio}
              onChange={set("bio")}
              placeholder="A short professional summary."
              className="field w-full rounded-lg px-3.5 py-2.5 text-[15px] text-fg placeholder:text-faint"
            />
          </div>
          <ErrorBanner error={error as NormalizedError | undefined} />
          <div className="flex gap-2">
            <Button
              type="submit"
              className="px-5 py-2 text-sm"
              loading={isLoading}
            >
              Save
            </Button>
            {profile && (
              <Button
                type="button"
                variant="ghost"
                className="px-5 py-2 text-sm"
                onClick={() => setEditing(false)}
              >
                Cancel
              </Button>
            )}
          </div>
        </form>
      </SectionCard>
    );
  }

  return (
    <SectionCard
      title="Basics"
      action={<AddButton label="Edit" onClick={() => setEditing(true)} />}
    >
      <p className="text-lg font-medium">
        {profile.first_name} {profile.last_name}
      </p>
      <p className="mt-0.5 text-sm text-dim">
        {[profile.location, profile.phone, profile.website]
          .filter(Boolean)
          .join(" · ") || "—"}
      </p>
      {profile.bio && <p className="mt-3 text-sm text-dim">{profile.bio}</p>}
    </SectionCard>
  );
}

// ───────────────────────── Experience ─────────────────────────

const EMPTY_EXPERIENCE = {
  company: "",
  role: "",
  location: "",
  start_date: "",
  end_date: "",
  is_current: false,
  description: "",
};

function ExperienceSection({
  items,
  disabled,
}: {
  items: WorkExperience[];
  disabled: boolean;
}) {
  const [form, setForm] = useState<typeof EMPTY_EXPERIENCE | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [add, addState] = useAddWorkExperienceMutation();
  const [update, updateState] = useUpdateWorkExperienceMutation();
  const [remove] = useDeleteWorkExperienceMutation();

  function startEdit(item: WorkExperience) {
    setEditingId(item.id);
    setForm({
      company: item.company,
      role: item.role,
      location: item.location ?? "",
      start_date: item.start_date,
      end_date: item.end_date ?? "",
      is_current: item.is_current,
      description: item.description ?? "",
    });
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!form) return;
    const body = {
      company: form.company,
      role: form.role,
      location: form.location || undefined,
      start_date: form.start_date,
      end_date: form.is_current || !form.end_date ? undefined : form.end_date,
      is_current: form.is_current,
      description: form.description || undefined,
    };
    try {
      if (editingId) await update({ id: editingId, body }).unwrap();
      else await add(body).unwrap();
      setForm(null);
      setEditingId(null);
    } catch {
      /* rendered from mutation state */
    }
  }

  return (
    <SectionCard
      title="Experience"
      action={
        !disabled ? (
          <AddButton
            label="Add role"
            onClick={() => {
              setEditingId(null);
              setForm(EMPTY_EXPERIENCE);
            }}
          />
        ) : undefined
      }
    >
      {items.length === 0 && !form && (
        <p className="text-sm text-faint">No roles yet.</p>
      )}
      <div className="divide-y divide-line">
        {items.map((item) => (
          <div
            key={item.id}
            className="flex items-start justify-between gap-4 py-3 first:pt-0"
          >
            <div className="min-w-0">
              <p className="text-sm font-medium">
                {item.role} · <span className="text-dim">{item.company}</span>
              </p>
              <p className="mt-0.5 font-mono text-xs text-faint">
                {item.start_date} –{" "}
                {item.is_current ? "present" : (item.end_date ?? "present")}
                {item.location ? ` · ${item.location}` : ""}
              </p>
              {item.description && (
                <p className="mt-1.5 text-sm text-dim">{item.description}</p>
              )}
            </div>
            <RowActions
              onEdit={() => startEdit(item)}
              onDelete={() => remove(item.id)}
            />
          </div>
        ))}
      </div>
      {form && (
        <form
          onSubmit={onSubmit}
          className="mt-4 space-y-4 rounded-lg border border-line bg-raised-2/40 p-4"
        >
          <div className="grid gap-4 sm:grid-cols-2">
            <Field
              id="exp-role"
              label="Job title"
              required
              value={form.role}
              onChange={(e) => setForm({ ...form, role: e.target.value })}
            />
            <Field
              id="exp-company"
              label="Company"
              required
              value={form.company}
              onChange={(e) => setForm({ ...form, company: e.target.value })}
            />
            <Field
              id="exp-start"
              label="Start"
              type="date"
              required
              value={form.start_date}
              onChange={(e) => setForm({ ...form, start_date: e.target.value })}
            />
            <Field
              id="exp-end"
              label="End"
              type="date"
              value={form.end_date}
              disabled={form.is_current}
              onChange={(e) => setForm({ ...form, end_date: e.target.value })}
            />
          </div>
          <label className="flex items-center gap-2 text-sm text-dim">
            <input
              type="checkbox"
              checked={form.is_current}
              onChange={(e) =>
                setForm({ ...form, is_current: e.target.checked })
              }
            />
            I currently work here
          </label>
          <Field
            id="exp-location"
            label="Location"
            value={form.location}
            onChange={(e) => setForm({ ...form, location: e.target.value })}
          />
          <div>
            <label htmlFor="exp-desc" className="tag mb-1.5 block text-dim">
              What you did
            </label>
            <textarea
              id="exp-desc"
              rows={3}
              value={form.description}
              onChange={(e) =>
                setForm({ ...form, description: e.target.value })
              }
              className="field w-full rounded-lg px-3.5 py-2.5 text-[15px] text-fg placeholder:text-faint"
            />
          </div>
          <ErrorBanner
            error={
              (addState.error ?? updateState.error) as
                NormalizedError | undefined
            }
          />
          <div className="flex gap-2">
            <Button
              type="submit"
              className="px-5 py-2 text-sm"
              loading={addState.isLoading || updateState.isLoading}
            >
              {editingId ? "Save" : "Add"}
            </Button>
            <Button
              type="button"
              variant="ghost"
              className="px-5 py-2 text-sm"
              onClick={() => {
                setForm(null);
                setEditingId(null);
              }}
            >
              Cancel
            </Button>
          </div>
        </form>
      )}
    </SectionCard>
  );
}

// ───────────────────────── Education ─────────────────────────

const EMPTY_EDUCATION = {
  institution: "",
  degree: "",
  field: "",
  start_date: "",
  end_date: "",
  description: "",
};

function EducationSection({
  items,
  disabled,
}: {
  items: Education[];
  disabled: boolean;
}) {
  const [form, setForm] = useState<typeof EMPTY_EDUCATION | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [add, addState] = useAddEducationMutation();
  const [update, updateState] = useUpdateEducationMutation();
  const [remove] = useDeleteEducationMutation();

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!form) return;
    const body = {
      institution: form.institution,
      degree: form.degree,
      field: form.field || undefined,
      start_date: form.start_date,
      end_date: form.end_date || undefined,
      description: form.description || undefined,
    };
    try {
      if (editingId) await update({ id: editingId, body }).unwrap();
      else await add(body).unwrap();
      setForm(null);
      setEditingId(null);
    } catch {
      /* rendered from mutation state */
    }
  }

  return (
    <SectionCard
      title="Education"
      action={
        !disabled ? (
          <AddButton
            label="Add degree"
            onClick={() => {
              setEditingId(null);
              setForm(EMPTY_EDUCATION);
            }}
          />
        ) : undefined
      }
    >
      {items.length === 0 && !form && (
        <p className="text-sm text-faint">No degrees yet.</p>
      )}
      <div className="divide-y divide-line">
        {items.map((item) => (
          <div
            key={item.id}
            className="flex items-start justify-between gap-4 py-3 first:pt-0"
          >
            <div className="min-w-0">
              <p className="text-sm font-medium">
                {item.degree}
                {item.field ? (
                  <span className="text-dim"> in {item.field}</span>
                ) : null}
              </p>
              <p className="mt-0.5 text-sm text-dim">{item.institution}</p>
              <p className="mt-0.5 font-mono text-xs text-faint">
                {item.start_date} – {item.end_date ?? "present"}
              </p>
            </div>
            <RowActions
              onEdit={() => {
                setEditingId(item.id);
                setForm({
                  institution: item.institution,
                  degree: item.degree,
                  field: item.field ?? "",
                  start_date: item.start_date,
                  end_date: item.end_date ?? "",
                  description: item.description ?? "",
                });
              }}
              onDelete={() => remove(item.id)}
            />
          </div>
        ))}
      </div>
      {form && (
        <form
          onSubmit={onSubmit}
          className="mt-4 space-y-4 rounded-lg border border-line bg-raised-2/40 p-4"
        >
          <div className="grid gap-4 sm:grid-cols-2">
            <Field
              id="edu-degree"
              label="Degree"
              required
              value={form.degree}
              onChange={(e) => setForm({ ...form, degree: e.target.value })}
              placeholder="MSc"
            />
            <Field
              id="edu-institution"
              label="Institution"
              required
              value={form.institution}
              onChange={(e) =>
                setForm({ ...form, institution: e.target.value })
              }
            />
            <Field
              id="edu-field"
              label="Field"
              value={form.field}
              onChange={(e) => setForm({ ...form, field: e.target.value })}
              placeholder="Computer Science"
            />
            <div className="grid grid-cols-2 gap-4">
              <Field
                id="edu-start"
                label="Start"
                type="date"
                required
                value={form.start_date}
                onChange={(e) =>
                  setForm({ ...form, start_date: e.target.value })
                }
              />
              <Field
                id="edu-end"
                label="End"
                type="date"
                value={form.end_date}
                onChange={(e) => setForm({ ...form, end_date: e.target.value })}
              />
            </div>
          </div>
          <ErrorBanner
            error={
              (addState.error ?? updateState.error) as
                NormalizedError | undefined
            }
          />
          <div className="flex gap-2">
            <Button
              type="submit"
              className="px-5 py-2 text-sm"
              loading={addState.isLoading || updateState.isLoading}
            >
              {editingId ? "Save" : "Add"}
            </Button>
            <Button
              type="button"
              variant="ghost"
              className="px-5 py-2 text-sm"
              onClick={() => {
                setForm(null);
                setEditingId(null);
              }}
            >
              Cancel
            </Button>
          </div>
        </form>
      )}
    </SectionCard>
  );
}

// ───────────────────── Skills & Languages ─────────────────────

function SkillsSection({
  items,
  disabled,
}: {
  items: Skill[];
  disabled: boolean;
}) {
  const [adding, setAdding] = useState(false);
  const [name, setName] = useState("");
  const [level, setLevel] =
    useState<(typeof SKILL_LEVELS)[number]>("intermediate");
  const [add, addState] = useAddSkillMutation();
  const [update] = useUpdateSkillMutation();
  const [remove] = useDeleteSkillMutation();

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      await add({ name, level }).unwrap();
      setName("");
      setAdding(false);
    } catch {
      /* rendered from mutation state */
    }
  }

  return (
    <SectionCard
      title="Skills"
      action={
        !disabled ? (
          <AddButton label="Add skill" onClick={() => setAdding(true)} />
        ) : undefined
      }
    >
      {items.length === 0 && !adding && (
        <p className="text-sm text-faint">No skills yet.</p>
      )}
      <div className="flex flex-wrap gap-2">
        {items.map((skill) => (
          <span
            key={skill.id}
            className="group flex items-center gap-2 rounded-full bg-raised-2 py-1 pl-3 pr-2 text-sm"
          >
            {skill.name}
            <select
              value={skill.level}
              onChange={(e) =>
                update({
                  id: skill.id,
                  body: {
                    name: skill.name,
                    level: e.target.value as Skill["level"],
                  },
                })
              }
              className="rounded bg-transparent font-mono text-[11px] uppercase tracking-wider text-faint"
              aria-label={`${skill.name} level`}
            >
              {SKILL_LEVELS.map((l) => (
                <option key={l} value={l}>
                  {l}
                </option>
              ))}
            </select>
            <button
              onClick={() => remove(skill.id)}
              className="text-faint transition hover:text-interview"
              aria-label={`Remove ${skill.name}`}
            >
              ×
            </button>
          </span>
        ))}
      </div>
      {adding && (
        <form
          onSubmit={onSubmit}
          className="mt-4 flex flex-wrap items-end gap-3"
        >
          <div className="min-w-40 flex-1">
            <Field
              id="skill-name"
              label="Skill"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>
          <select
            value={level}
            onChange={(e) =>
              setLevel(e.target.value as (typeof SKILL_LEVELS)[number])
            }
            className="field rounded-lg px-3 py-2.5 text-sm text-fg"
            aria-label="Skill level"
          >
            {SKILL_LEVELS.map((l) => (
              <option key={l} value={l}>
                {l}
              </option>
            ))}
          </select>
          <Button
            type="submit"
            className="px-4 py-2 text-sm"
            loading={addState.isLoading}
          >
            Add
          </Button>
          <Button
            type="button"
            variant="ghost"
            className="px-4 py-2 text-sm"
            onClick={() => setAdding(false)}
          >
            Cancel
          </Button>
        </form>
      )}
    </SectionCard>
  );
}

function LanguagesSection({
  items,
  disabled,
}: {
  items: Language[];
  disabled: boolean;
}) {
  const [adding, setAdding] = useState(false);
  const [name, setName] = useState("");
  const [proficiency, setProficiency] =
    useState<(typeof PROFICIENCIES)[number]>("fluent");
  const [add, addState] = useAddLanguageMutation();
  const [update] = useUpdateLanguageMutation();
  const [remove] = useDeleteLanguageMutation();

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      await add({ name, proficiency }).unwrap();
      setName("");
      setAdding(false);
    } catch {
      /* rendered from mutation state */
    }
  }

  return (
    <SectionCard
      title="Languages"
      action={
        !disabled ? (
          <AddButton label="Add language" onClick={() => setAdding(true)} />
        ) : undefined
      }
    >
      {items.length === 0 && !adding && (
        <p className="text-sm text-faint">No languages yet.</p>
      )}
      <div className="flex flex-wrap gap-2">
        {items.map((lang) => (
          <span
            key={lang.id}
            className="flex items-center gap-2 rounded-full bg-raised-2 py-1 pl-3 pr-2 text-sm"
          >
            {lang.name}
            <select
              value={lang.proficiency}
              onChange={(e) =>
                update({
                  id: lang.id,
                  body: {
                    name: lang.name,
                    proficiency: e.target.value as Language["proficiency"],
                  },
                })
              }
              className="rounded bg-transparent font-mono text-[11px] uppercase tracking-wider text-faint"
              aria-label={`${lang.name} proficiency`}
            >
              {PROFICIENCIES.map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </select>
            <button
              onClick={() => remove(lang.id)}
              className="text-faint transition hover:text-interview"
              aria-label={`Remove ${lang.name}`}
            >
              ×
            </button>
          </span>
        ))}
      </div>
      {adding && (
        <form
          onSubmit={onSubmit}
          className="mt-4 flex flex-wrap items-end gap-3"
        >
          <div className="min-w-40 flex-1">
            <Field
              id="lang-name"
              label="Language"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>
          <select
            value={proficiency}
            onChange={(e) =>
              setProficiency(e.target.value as (typeof PROFICIENCIES)[number])
            }
            className="field rounded-lg px-3 py-2.5 text-sm text-fg"
            aria-label="Proficiency"
          >
            {PROFICIENCIES.map((p) => (
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </select>
          <Button
            type="submit"
            className="px-4 py-2 text-sm"
            loading={addState.isLoading}
          >
            Add
          </Button>
          <Button
            type="button"
            variant="ghost"
            className="px-4 py-2 text-sm"
            onClick={() => setAdding(false)}
          >
            Cancel
          </Button>
        </form>
      )}
    </SectionCard>
  );
}

// ───────────────────────── Page ─────────────────────────

export default function ProfilePage() {
  const navigate = useNavigate();
  const { data, error, isLoading } = useGetProfileQuery();
  const noProfile = error != null && "status" in error && error.status === 404;

  if (isLoading) {
    return (
      <div className="grid h-full place-items-center text-sm text-dim">
        Loading…
      </div>
    );
  }

  return (
    <div className="h-full overflow-y-auto">
      <div className="mx-auto max-w-3xl space-y-5 px-8 py-8">
        <div>
          <h1 className="font-display text-2xl font-semibold tracking-tight">
            Profile
          </h1>
          <p className="mt-1 text-sm text-dim">
            The single source your tailored documents are built from
          </p>
        </div>

        {noProfile && (
          <Card className="p-5">
            <p className="text-sm text-dim">
              You haven&apos;t set up your profile yet — the guided setup takes
              two minutes.
            </p>
            <Button
              className="mt-4 px-4 py-2 text-sm"
              onClick={() => navigate("/onboarding")}
            >
              Start guided setup
            </Button>
          </Card>
        )}

        <BasicsSection data={data} />
        <ExperienceSection
          items={data?.work_experiences ?? []}
          disabled={!data}
        />
        <EducationSection items={data?.educations ?? []} disabled={!data} />
        <SkillsSection items={data?.skills ?? []} disabled={!data} />
        <LanguagesSection items={data?.languages ?? []} disabled={!data} />
      </div>
    </div>
  );
}
