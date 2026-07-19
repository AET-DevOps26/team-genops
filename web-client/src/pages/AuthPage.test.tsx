import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Provider } from "react-redux";
import { MemoryRouter } from "react-router-dom";
import { configureStore } from "@reduxjs/toolkit";
import authReducer from "~/services/auth/authSlice";
import AuthPage from "./AuthPage";

// Renders the real page against a real (single-slice) store + router, so the
// test exercises the actual sign-in interaction, not a stub.
function renderPage() {
  const store = configureStore({ reducer: { auth: authReducer } });
  render(
    <Provider store={store}>
      <MemoryRouter>
        <AuthPage />
      </MemoryRouter>
    </Provider>,
  );
  return store;
}

function mockFetchOnce(status: number, body: unknown) {
  vi.mocked(fetch).mockResolvedValueOnce({
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as Response);
}

describe("AuthPage", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("defaults to the sign-in lane", () => {
    renderPage();
    expect(
      screen.getByRole("heading", { name: /pick up where you left off/i }),
    ).toBeInTheDocument();
  });

  it("signs a user in and shows the success state", async () => {
    const user = userEvent.setup();
    renderPage();
    mockFetchOnce(200, { id: "u1", email: "a@b.com" });

    await user.type(screen.getByLabelText("Email"), "a@b.com");
    await user.type(screen.getByLabelText("Password"), "password1");
    await user.click(screen.getByRole("button", { name: /open my board/i }));

    expect(await screen.findByText(/you're in/i)).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledWith(
      "/api/v1/auth/login",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("surfaces a friendly error on bad credentials", async () => {
    const user = userEvent.setup();
    renderPage();
    mockFetchOnce(401, { code: "INVALID_CREDENTIALS" });

    await user.type(screen.getByLabelText("Email"), "a@b.com");
    await user.type(screen.getByLabelText("Password"), "wrongpass");
    await user.click(screen.getByRole("button", { name: /open my board/i }));

    expect(
      await screen.findByText(/email or password don't match/i),
    ).toBeInTheDocument();
  });

  it("switches to the create-account lane", async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole("button", { name: /create account/i }));

    await waitFor(() =>
      expect(
        screen.getByRole("heading", { name: /start your board/i }),
      ).toBeInTheDocument(),
    );
  });
});
