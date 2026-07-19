import { describe, it, expect } from "vitest";
import { errorMessage, isNormalizedError } from "./errors";
import type { NormalizedError } from "./errors";

const err = (code: string, status = 400, message = "raw"): NormalizedError => ({
  code,
  status,
  message,
});

describe("isNormalizedError", () => {
  it("accepts an object carrying code + status", () => {
    expect(isNormalizedError(err("X"))).toBe(true);
  });

  it("rejects non-normalized values", () => {
    expect(isNormalizedError(null)).toBe(false);
    expect(isNormalizedError("boom")).toBe(false);
    expect(isNormalizedError({ code: "X" })).toBe(false);
  });
});

describe("errorMessage", () => {
  it("returns the generic fallback for a non-normalized error", () => {
    expect(errorMessage("nope")).toBe("Something went wrong. Try again.");
  });

  it("resolves a known code from the global map", () => {
    expect(errorMessage(err("INVALID_CREDENTIALS"))).toBe(
      "Email or password don't match. Try again.",
    );
  });

  it("prefers a per-call override over the global map", () => {
    expect(errorMessage(err("NOT_FOUND"), { NOT_FOUND: "No board yet." })).toBe(
      "No board yet.",
    );
  });

  it("falls back to the backend message for an unmapped code", () => {
    expect(errorMessage(err("HTTP_500", 500, "server exploded"))).toBe(
      "server exploded",
    );
  });
});
