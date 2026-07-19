import { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Card, ErrorBanner, Field, Tag } from "~/components/ui";
import type { NormalizedError } from "~/api/errors";
import {
  useAddEducationMutation,
  useAddLanguageMutation,
  useAddSkillMutation,
  useAddWorkExperienceMutation,
  useGetProfileQuery,
  useUpsertProfileMutation,
} from "~/services/profile/profileApi";
import {
  useAuthorizeGmailMutation,
  useGetEmailConnectionQuery,
} from "~/services/email/emailApi";

const STEPS = [
  "Basics",
  "Experience",
  "Education",
  "Skills & languages",
  "Connect email",
];
const SKILL_LEVELS = [
  "beginner",
  "intermediate",
  "advanced",
  "expert",
] as const;
const PROFICIENCIES = ["basic", "conversational", "fluent", "native"] as const;

function StepHeader({ step }: { step: number }) {
  return (
    <div className="mb-8">
      <div className="mb-3 flex items-center justify-between">
        <Tag className="text-faint">
          Step {step + 1} of {STEPS.length}
        </Tag>
        <Tag className="text-dim">{STEPS[step]}</Tag>
      </div>
      <div className="flex gap-1.5">
        {STEPS.map((label, i) => (
          <div
            key={label}
            className={`h-1 flex-1 rounded-full transition ${i <= step ? "bg-offer" : "bg-line"}`}
          />
        ))}
      </div>
    </div>
  );
}

/**
 * Post-registration guided profile setup (mockup's onboarding, adapted to the
 * scoped tables: basics → experience → education → skills+languages). Each step
 * saves immediately — basics must exist first (sub-resources FK onto the profile),
 * every later step is skippable.
 */
export default function OnboardingWizard() {
  const navigate = useNavigate();
  const [step, setStep] = useState(0);

  // Prefill basics if a profile already exists (returning users can re-run the wizard).
  const { data: existing } = useGetProfileQuery();

  const [basics, setBasics] = useState({
    first_name: "",
    last_name: "",
    location: "",
    bio: "",
  });
  const [basicsTouched, setBasicsTouched] = useState(false);
  const [upsertProfile, upsertState] = useUpsertProfileMutation();

  const [experience, setExperience] = useState({
    role: "",
    company: "",
    start_date: "",
    end_date: "",
    is_current: false,
    description: "",
  });
  const [addExperience, expState] = useAddWorkExperienceMutation();
  const [experienceCount, setExperienceCount] = useState(0);

  const [education, setEducation] = useState({
    degree: "",
    institution: "",
    field: "",
    start_date: "",
    end_date: "",
  });
  const [addEducation, eduState] = useAddEducationMutation();
  const [educationCount, setEducationCount] = useState(0);

  const [skillName, setSkillName] = useState("");
  const [skillLevel, setSkillLevel] =
    useState<(typeof SKILL_LEVELS)[number]>("intermediate");
  const [addSkill] = useAddSkillMutation();
  const [skills, setSkills] = useState<string[]>([]);

  const [langName, setLangName] = useState("");
  const [langProficiency, setLangProficiency] =
    useState<(typeof PROFICIENCIES)[number]>("fluent");
  const [addLanguage] = useAddLanguageMutation();
  const [languages, setLanguages] = useState<string[]>([]);

  const { data: emailConnection } = useGetEmailConnectionQuery();
  const [authorizeGmail, authorizeState] = useAuthorizeGmailMutation();

  async function connectGmail() {
    try {
      const { authorization_url } = await authorizeGmail().unwrap();
      // Full-page redirect off the app to Google's consent screen. On return the
      // email service sends the browser to the dashboard (not back here), where
      // AppShell surfaces the success/error notice.
      window.location.assign(authorization_url);
    } catch {
      /* rendered from mutation state */
    }
  }

  const shownBasics =
    basicsTouched || !existing
      ? basics
      : {
          first_name: existing.profile.first_name,
          last_name: existing.profile.last_name,
          location: existing.profile.location ?? "",
          bio: existing.profile.bio ?? "",
        };

  async function submitBasics(e: FormEvent) {
    e.preventDefault();
    try {
      await upsertProfile({
        first_name: shownBasics.first_name,
        last_name: shownBasics.last_name,
        location: shownBasics.location || undefined,
        bio: shownBasics.bio || undefined,
      }).unwrap();
      setStep(1);
    } catch {
      /* rendered from mutation state */
    }
  }

  async function submitExperience(e: FormEvent) {
    e.preventDefault();
    try {
      await addExperience({
        company: experience.company,
        role: experience.role,
        start_date: experience.start_date,
        end_date:
          experience.is_current || !experience.end_date
            ? undefined
            : experience.end_date,
        is_current: experience.is_current,
        description: experience.description || undefined,
      }).unwrap();
      setExperienceCount((n) => n + 1);
      setExperience({
        role: "",
        company: "",
        start_date: "",
        end_date: "",
        is_current: false,
        description: "",
      });
    } catch {
      /* rendered from mutation state */
    }
  }

  async function submitEducation(e: FormEvent) {
    e.preventDefault();
    try {
      await addEducation({
        institution: education.institution,
        degree: education.degree,
        field: education.field || undefined,
        start_date: education.start_date,
        end_date: education.end_date || undefined,
      }).unwrap();
      setEducationCount((n) => n + 1);
      setEducation({
        degree: "",
        institution: "",
        field: "",
        start_date: "",
        end_date: "",
      });
    } catch {
      /* rendered from mutation state */
    }
  }

  async function submitSkill(e: FormEvent) {
    e.preventDefault();
    if (!skillName) return;
    try {
      await addSkill({ name: skillName, level: skillLevel }).unwrap();
      setSkills((s) => [...s, skillName]);
      setSkillName("");
    } catch {
      /* keep going — chips already added stay saved */
    }
  }

  async function submitLanguage(e: FormEvent) {
    e.preventDefault();
    if (!langName) return;
    try {
      await addLanguage({
        name: langName,
        proficiency: langProficiency,
      }).unwrap();
      setLanguages((s) => [...s, langName]);
      setLangName("");
    } catch {
      /* keep going */
    }
  }

  return (
    <div className="min-h-screen overflow-y-auto bg-ink px-5 py-10 text-fg">
      <div className="mx-auto max-w-xl">
        <div className="mb-8 flex items-center justify-between">
          <div className="flex items-center gap-2">
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
          {step > 0 && (
            <button
              onClick={() => navigate("/")}
              className="tag text-faint transition hover:text-fg"
            >
              Save & exit
            </button>
          )}
        </div>

        <Card className="p-7">
          <StepHeader step={step} />

          {step === 0 && (
            <form onSubmit={submitBasics} className="space-y-4">
              <h1 className="font-display text-xl font-semibold tracking-tight">
                Let&apos;s build your profile
              </h1>
              <p className="text-sm text-dim">
                Your profile powers tailored cover letters, resume tuning, and
                fit analysis.
              </p>
              <div className="grid gap-4 sm:grid-cols-2">
                <Field
                  id="ob-first"
                  label="First name"
                  required
                  value={shownBasics.first_name}
                  onChange={(e) => {
                    setBasicsTouched(true);
                    setBasics({ ...shownBasics, first_name: e.target.value });
                  }}
                />
                <Field
                  id="ob-last"
                  label="Last name"
                  required
                  value={shownBasics.last_name}
                  onChange={(e) => {
                    setBasicsTouched(true);
                    setBasics({ ...shownBasics, last_name: e.target.value });
                  }}
                />
              </div>
              <Field
                id="ob-location"
                label="Location"
                value={shownBasics.location}
                placeholder="Munich, Germany"
                onChange={(e) => {
                  setBasicsTouched(true);
                  setBasics({ ...shownBasics, location: e.target.value });
                }}
              />
              <div>
                <label htmlFor="ob-bio" className="tag mb-1.5 block text-dim">
                  Headline
                </label>
                <textarea
                  id="ob-bio"
                  rows={2}
                  value={shownBasics.bio}
                  placeholder="MSc CS student focused on backend & platform engineering."
                  onChange={(e) => {
                    setBasicsTouched(true);
                    setBasics({ ...shownBasics, bio: e.target.value });
                  }}
                  className="field w-full rounded-lg px-3.5 py-2.5 text-[15px] text-fg placeholder:text-faint"
                />
              </div>
              <ErrorBanner
                error={upsertState.error as NormalizedError | undefined}
              />
              <Button
                type="submit"
                className="w-full"
                loading={upsertState.isLoading}
              >
                Continue
              </Button>
            </form>
          )}

          {step === 1 && (
            <div>
              <h1 className="font-display text-xl font-semibold tracking-tight">
                Work experience
              </h1>
              <p className="mt-1 text-sm text-dim">
                Add the roles that matter — you can refine them later.
                {experienceCount > 0 && (
                  <span className="text-offer"> {experienceCount} added.</span>
                )}
              </p>
              <form onSubmit={submitExperience} className="mt-5 space-y-4">
                <div className="grid gap-4 sm:grid-cols-2">
                  <Field
                    id="ob-role"
                    label="Job title"
                    required
                    value={experience.role}
                    onChange={(e) =>
                      setExperience({ ...experience, role: e.target.value })
                    }
                  />
                  <Field
                    id="ob-company"
                    label="Company"
                    required
                    value={experience.company}
                    onChange={(e) =>
                      setExperience({ ...experience, company: e.target.value })
                    }
                  />
                  <Field
                    id="ob-exp-start"
                    label="Start"
                    type="date"
                    required
                    value={experience.start_date}
                    onChange={(e) =>
                      setExperience({
                        ...experience,
                        start_date: e.target.value,
                      })
                    }
                  />
                  <Field
                    id="ob-exp-end"
                    label="End"
                    type="date"
                    value={experience.end_date}
                    disabled={experience.is_current}
                    onChange={(e) =>
                      setExperience({ ...experience, end_date: e.target.value })
                    }
                  />
                </div>
                <label className="flex items-center gap-2 text-sm text-dim">
                  <input
                    type="checkbox"
                    checked={experience.is_current}
                    onChange={(e) =>
                      setExperience({
                        ...experience,
                        is_current: e.target.checked,
                      })
                    }
                  />
                  I currently work here
                </label>
                <div>
                  <label
                    htmlFor="ob-exp-desc"
                    className="tag mb-1.5 block text-dim"
                  >
                    What you did
                  </label>
                  <textarea
                    id="ob-exp-desc"
                    rows={2}
                    value={experience.description}
                    onChange={(e) =>
                      setExperience({
                        ...experience,
                        description: e.target.value,
                      })
                    }
                    className="field w-full rounded-lg px-3.5 py-2.5 text-[15px] text-fg placeholder:text-faint"
                  />
                </div>
                <ErrorBanner
                  error={expState.error as NormalizedError | undefined}
                />
                <Button
                  type="submit"
                  variant="ghost"
                  className="w-full py-2.5"
                  loading={expState.isLoading}
                >
                  Add role
                </Button>
              </form>
            </div>
          )}

          {step === 2 && (
            <div>
              <h1 className="font-display text-xl font-semibold tracking-tight">
                Education
              </h1>
              <p className="mt-1 text-sm text-dim">
                Degrees, thesis topics, and relevant coursework.
                {educationCount > 0 && (
                  <span className="text-offer"> {educationCount} added.</span>
                )}
              </p>
              <form onSubmit={submitEducation} className="mt-5 space-y-4">
                <div className="grid gap-4 sm:grid-cols-2">
                  <Field
                    id="ob-degree"
                    label="Degree"
                    required
                    value={education.degree}
                    placeholder="MSc"
                    onChange={(e) =>
                      setEducation({ ...education, degree: e.target.value })
                    }
                  />
                  <Field
                    id="ob-institution"
                    label="Institution"
                    required
                    value={education.institution}
                    onChange={(e) =>
                      setEducation({
                        ...education,
                        institution: e.target.value,
                      })
                    }
                  />
                  <Field
                    id="ob-field"
                    label="Thesis / focus"
                    value={education.field}
                    onChange={(e) =>
                      setEducation({ ...education, field: e.target.value })
                    }
                  />
                  <div className="grid grid-cols-2 gap-4">
                    <Field
                      id="ob-edu-start"
                      label="Start"
                      type="date"
                      required
                      value={education.start_date}
                      onChange={(e) =>
                        setEducation({
                          ...education,
                          start_date: e.target.value,
                        })
                      }
                    />
                    <Field
                      id="ob-edu-end"
                      label="End"
                      type="date"
                      value={education.end_date}
                      onChange={(e) =>
                        setEducation({ ...education, end_date: e.target.value })
                      }
                    />
                  </div>
                </div>
                <ErrorBanner
                  error={eduState.error as NormalizedError | undefined}
                />
                <Button
                  type="submit"
                  variant="ghost"
                  className="w-full py-2.5"
                  loading={eduState.isLoading}
                >
                  Add degree
                </Button>
              </form>
            </div>
          )}

          {step === 3 && (
            <div className="space-y-6">
              <div>
                <h1 className="font-display text-xl font-semibold tracking-tight">
                  Skills & languages
                </h1>
                <p className="mt-1 text-sm text-dim">
                  What should we highlight to employers?
                </p>
              </div>
              <div>
                <Tag className="mb-2 block text-faint">Skills</Tag>
                {skills.length > 0 && (
                  <div className="mb-3 flex flex-wrap gap-1.5">
                    {skills.map((s, i) => (
                      <span
                        key={`${s}-${i}`}
                        className="rounded-full bg-raised-2 px-2.5 py-1 text-xs text-dim"
                      >
                        {s}
                      </span>
                    ))}
                  </div>
                )}
                <form onSubmit={submitSkill} className="flex gap-2">
                  <div className="field flex flex-1 items-center rounded-lg px-3.5 py-2">
                    <input
                      value={skillName}
                      onChange={(e) => setSkillName(e.target.value)}
                      placeholder="e.g. Spring Boot"
                      aria-label="Skill"
                    />
                  </div>
                  <select
                    value={skillLevel}
                    onChange={(e) =>
                      setSkillLevel(
                        e.target.value as (typeof SKILL_LEVELS)[number],
                      )
                    }
                    className="field rounded-lg px-3 py-2 text-sm text-fg"
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
                    variant="ghost"
                    className="px-4 py-2 text-sm"
                  >
                    Add
                  </Button>
                </form>
              </div>
              <div>
                <Tag className="mb-2 block text-faint">Languages</Tag>
                {languages.length > 0 && (
                  <div className="mb-3 flex flex-wrap gap-1.5">
                    {languages.map((l, i) => (
                      <span
                        key={`${l}-${i}`}
                        className="rounded-full bg-raised-2 px-2.5 py-1 text-xs text-dim"
                      >
                        {l}
                      </span>
                    ))}
                  </div>
                )}
                <form onSubmit={submitLanguage} className="flex gap-2">
                  <div className="field flex flex-1 items-center rounded-lg px-3.5 py-2">
                    <input
                      value={langName}
                      onChange={(e) => setLangName(e.target.value)}
                      placeholder="e.g. German"
                      aria-label="Language"
                    />
                  </div>
                  <select
                    value={langProficiency}
                    onChange={(e) =>
                      setLangProficiency(
                        e.target.value as (typeof PROFICIENCIES)[number],
                      )
                    }
                    className="field rounded-lg px-3 py-2 text-sm text-fg"
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
                    variant="ghost"
                    className="px-4 py-2 text-sm"
                  >
                    Add
                  </Button>
                </form>
              </div>
            </div>
          )}

          {step === 4 && (
            <div className="space-y-5">
              <div>
                <h1 className="font-display text-xl font-semibold tracking-tight">
                  Connect your email
                </h1>
                <p className="mt-1 text-sm text-dim">
                  Let JobReady watch your inbox for application updates —
                  interview invites, offers, and rejections are detected
                  automatically and move your pipeline forward.
                </p>
              </div>
              {emailConnection?.connected ? (
                <div className="rounded-lg border border-offer/30 bg-offer/10 px-4 py-3 text-sm text-offer">
                  Gmail connected
                  {emailConnection.email_address
                    ? ` — ${emailConnection.email_address}`
                    : ""}
                  .
                </div>
              ) : (
                <>
                  <Button
                    className="w-full"
                    loading={authorizeState.isLoading}
                    onClick={connectGmail}
                  >
                    Connect Gmail
                  </Button>
                  <p className="text-center text-xs text-faint">
                    You&apos;ll be taken to Google to grant access, then landed
                    on your dashboard. You can also do this later from your
                    profile.
                  </p>
                  <ErrorBanner
                    error={authorizeState.error as NormalizedError | undefined}
                  />
                </>
              )}
            </div>
          )}

          {step > 0 && (
            <div className="mt-6 flex items-center justify-between border-t border-line pt-5">
              <button
                onClick={() => setStep(step - 1)}
                className="tag text-faint transition hover:text-fg"
              >
                Back
              </button>
              {step < STEPS.length - 1 ? (
                <Button
                  className="px-6 py-2 text-sm"
                  onClick={() => setStep(step + 1)}
                >
                  Continue
                </Button>
              ) : (
                <Button
                  className="px-6 py-2 text-sm"
                  onClick={() => navigate("/")}
                >
                  Finish
                </Button>
              )}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
