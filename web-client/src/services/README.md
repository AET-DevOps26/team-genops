# `services/` — the data layer (no UI here)

This folder is **only** API/request logic: RTK Query endpoints and, rarely, a slice for
client-only state. **All UI lives in `components/` or `pages/`** — never here.

## Layout

```
services/
  apiClient.ts        The ONE createApi: shared baseQuery, single-flight 401→refresh,
                      error normalization, tagTypes. Domain-agnostic. Endpoints injected.
  session.ts          Neutral `sessionExpired` action (breaks the apiClient↔auth cycle).
  auth/
    authSlice.ts      Hand-rolled session state machine. NOT RTK Query (auth isn't
                      cacheable data — it's a lifecycle). Listens for sessionExpired.
  <resource>/
    <resource>Api.ts  Injects endpoints into the shared api. Types come from ~/api/schemas.
```

## Adding a resource service (the additive move)

A new service is a new folder + one file. You never touch existing services.

1. Add its schemas to `api/openapi.yaml`, run `make -C api generate`
   (→ `generated/openapi.ts`), and alias the new types in `api/schemas.ts`.
2. Create `services/<resource>/<resource>Api.ts`:

```ts
import { api } from '~/services/apiClient'
import type { Schemas } from '~/api/schemas'

type Application = Schemas['Application'] // ← from the generated spec, never hand-written

export const applicationApi = api.injectEndpoints({
  endpoints: (build) => ({
    getApplications: build.query<Application[], void>({
      query: () => '/applications',
      providesTags: ['Application'],
    }),
    updateStatus: build.mutation<Application, { id: string; status: string }>({
      query: ({ id, ...body }) => ({ url: `/applications/${id}`, method: 'PUT', body }),
      invalidatesTags: ['Application'], // the list auto-refetches; no manual cache edits
    }),
  }),
})

export const { useGetApplicationsQuery, useUpdateStatusMutation } = applicationApi
```

3. Add any new `tagTypes` to `apiClient.ts`.
4. Build its UI in `pages/` (service views) or `components/<resource>/` (compositions) —
   **not** in this folder.

That's it: caching, loading flags, dedup, and `{ status, code, message }` error
normalization all come from the shared `apiClient`.
