import { api } from "~/services/apiClient";
import type {
  EmailConnectionStatus,
  GmailAuthorizeResponse,
} from "~/api/schemas";

// Email-integration endpoints (email service). The base query is rooted at
// `/api/v1`, so URLs here are `/email/...`. Connection status is cached under the
// shared `'Email'` tag; connect/disconnect invalidate it so the UI refreshes.
//
// `authorizeGmail` only returns a Google consent URL — the caller performs a
// full-page redirect to it, so there is nothing to invalidate here.
const emailApi = api.injectEndpoints({
  endpoints: (build) => ({
    getEmailConnection: build.query<EmailConnectionStatus, void>({
      query: () => "/email/connections",
      providesTags: ["Email"],
    }),

    authorizeGmail: build.mutation<GmailAuthorizeResponse, void>({
      query: () => ({
        url: "/email/connections/gmail/authorize",
        method: "POST",
      }),
    }),

    deleteEmailConnection: build.mutation<void, void>({
      query: () => ({
        url: "/email/connections",
        method: "DELETE",
        responseHandler: "text",
      }),
      invalidatesTags: ["Email"],
    }),
  }),
});

export const {
  useGetEmailConnectionQuery,
  useAuthorizeGmailMutation,
  useDeleteEmailConnectionMutation,
} = emailApi;
