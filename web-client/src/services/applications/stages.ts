import type { ApplicationStage } from "~/api/schemas";

/**
 * The pipeline vocabulary: wire value → label → stage color token.
 * Color is meaning — these are the only saturated colors in the UI.
 */
export const STAGES: {
  value: ApplicationStage;
  label: string;
  color: string;
}[] = [
  { value: "applied", label: "Applied", color: "var(--color-applied)" },
  { value: "follow_up", label: "Follow-up", color: "var(--color-screen)" },
  { value: "interview", label: "Interview", color: "var(--color-interview)" },
  { value: "offer", label: "Offer", color: "var(--color-offer)" },
  { value: "closed", label: "Closed", color: "var(--color-faint)" },
];

export function stageMeta(stage: ApplicationStage) {
  return STAGES.find((s) => s.value === stage) ?? STAGES[0];
}
