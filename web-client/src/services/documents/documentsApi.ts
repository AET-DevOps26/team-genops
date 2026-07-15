import { api } from "~/services/apiClient";
import type { GeneratedDocumentList } from "~/api/schemas";

// AI-generated documents (cover letters / resumes) persisted by the genai service.
// The web client only reads and deletes — creation happens through the assistant.
const documentsApi = api.injectEndpoints({
  endpoints: (build) => ({
    getDocuments: build.query<
      GeneratedDocumentList,
      { applicationId?: string } | void
    >({
      query: (args) =>
        args && args.applicationId
          ? `/documents?application_id=${args.applicationId}`
          : "/documents",
      providesTags: ["CoverLetter"],
    }),

    deleteDocument: build.mutation<void, string>({
      query: (id) => ({
        url: `/documents/${id}`,
        method: "DELETE",
        responseHandler: "text",
      }),
      invalidatesTags: ["CoverLetter"],
    }),
  }),
});

export const { useGetDocumentsQuery, useDeleteDocumentMutation } = documentsApi;
