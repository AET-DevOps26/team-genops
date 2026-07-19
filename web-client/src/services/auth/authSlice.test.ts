import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { configureStore } from "@reduxjs/toolkit";
import authReducer, {
  login,
  register,
  logout,
  fetchMe,
  clearError,
} from "./authSlice";
import { sessionExpired } from "~/services/session";

// A fresh single-slice store per test so the auth state machine is exercised
// exactly as the app dispatches it (thunk → extraReducers), in isolation.
function makeStore() {
  return configureStore({ reducer: { auth: authReducer } });
}

type Store = ReturnType<typeof makeStore>;
const authOf = (store: Store) => store.getState().auth;

/** Stub the next fetch call with a given status + JSON body. */
function mockFetchOnce(status: number, body: unknown) {
  const ok = status >= 200 && status < 300;
  vi.mocked(fetch).mockResolvedValueOnce({
    ok,
    status,
    json: async () => body,
  } as Response);
}

const USER = { id: "u1", email: "a@b.com" };

describe("authSlice", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("starts in the unknown state", () => {
    expect(authOf(makeStore())).toEqual({
      status: "unknown",
      user: null,
      error: null,
    });
  });

  it("login success moves to authenticated and stores the user", async () => {
    const store = makeStore();
    mockFetchOnce(200, USER);

    await store.dispatch(login({ email: "a@b.com", password: "pw" }));

    expect(authOf(store)).toMatchObject({
      status: "authenticated",
      user: USER,
    });
  });

  it("login with bad credentials maps 401 to friendly copy and stays anonymous", async () => {
    const store = makeStore();
    mockFetchOnce(401, { code: "INVALID_CREDENTIALS" });

    await store.dispatch(login({ email: "a@b.com", password: "bad" }));

    expect(authOf(store).status).toBe("anonymous");
    expect(authOf(store).error).toBe(
      "Email or password don't match. Try again.",
    );
  });

  it("register maps a 422 to the password-length hint", async () => {
    const store = makeStore();
    mockFetchOnce(422, {});

    await store.dispatch(register({ email: "a@b.com", password: "short" }));

    expect(authOf(store).error).toBe("Password must be at least 8 characters.");
  });

  it("a network failure surfaces the transport copy", async () => {
    const store = makeStore();
    vi.mocked(fetch).mockRejectedValueOnce(new Error("offline"));

    await store.dispatch(login({ email: "a@b.com", password: "pw" }));

    expect(authOf(store).error).toBe(
      "Can't reach the server. Check your connection and try again.",
    );
  });

  it("fetchMe resolves the session from the cookie", async () => {
    const store = makeStore();
    mockFetchOnce(200, USER);

    await store.dispatch(fetchMe());

    expect(authOf(store)).toMatchObject({
      status: "authenticated",
      user: USER,
    });
  });

  it("fetchMe with no session becomes anonymous", async () => {
    const store = makeStore();
    mockFetchOnce(401, {});

    await store.dispatch(fetchMe());

    expect(authOf(store)).toMatchObject({ status: "anonymous", user: null });
  });

  it("logout clears the user", async () => {
    const store = makeStore();
    mockFetchOnce(200, USER);
    await store.dispatch(login({ email: "a@b.com", password: "pw" }));
    mockFetchOnce(204, {});

    await store.dispatch(logout());

    expect(authOf(store)).toMatchObject({ status: "anonymous", user: null });
  });

  it("sessionExpired wipes an authenticated session", async () => {
    const store = makeStore();
    mockFetchOnce(200, USER);
    await store.dispatch(login({ email: "a@b.com", password: "pw" }));

    store.dispatch(sessionExpired());

    expect(authOf(store)).toMatchObject({ status: "anonymous", user: null });
  });

  it("clearError resets a prior error", async () => {
    const store = makeStore();
    mockFetchOnce(401, { code: "INVALID_CREDENTIALS" });
    await store.dispatch(login({ email: "a@b.com", password: "bad" }));
    expect(authOf(store).error).not.toBeNull();

    store.dispatch(clearError());

    expect(authOf(store).error).toBeNull();
  });
});
