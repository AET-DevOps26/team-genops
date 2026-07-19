import { describe, it, expect } from "vitest";
import { STAGES, stageMeta } from "./stages";

describe("stages", () => {
  it("exposes the full pipeline vocabulary in order", () => {
    expect(STAGES.map((s) => s.value)).toEqual([
      "draft",
      "applied",
      "follow_up",
      "interview",
      "offer",
      "closed",
    ]);
  });

  it("every stage has a label and a color token", () => {
    for (const s of STAGES) {
      expect(s.label).toBeTruthy();
      expect(s.color).toMatch(/^var\(--/);
    }
  });

  it("stageMeta resolves a known stage to its metadata", () => {
    expect(stageMeta("interview")).toEqual({
      value: "interview",
      label: "Interview",
      color: "var(--color-interview)",
    });
  });

  it("stageMeta falls back to the first stage for an unknown value", () => {
    // Cast: exercising the runtime fallback for a value outside the union.
    expect(stageMeta("bogus" as never)).toBe(STAGES[0]);
  });
});
