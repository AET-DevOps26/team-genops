// Vitest global setup: adds jest-dom matchers (toBeInTheDocument, etc.) and
// clears the DOM + mocks between tests so cases stay isolated.
import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, vi } from "vitest";

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});
