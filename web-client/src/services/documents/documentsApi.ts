import { api } from '~/services/apiClient'
import type {
  CreateGeneratedDocumentRequest,
  GeneratedDocument,
  GeneratedDocumentList,
} from '~/api/schemas'

// AI-generated documents (cover letters / resumes) stored by the document service.
// The assistant writes them into the conversation; saving one is an explicit user action
// from the chat, so creation is a plain client call — no LLM in the save path.
const documentsApi = api.injectEndpoints({
  endpoints: (build) => ({
    getDocuments: build.query<GeneratedDocumentList, { applicationId?: string } | void>({
      query: (args) =>
        args && args.applicationId
          ? `/documents?application_id=${args.applicationId}`
          : '/documents',
      providesTags: ['CoverLetter'],
    }),

    createDocument: build.mutation<GeneratedDocument, CreateGeneratedDocumentRequest>({
      query: (body) => ({ url: '/documents', method: 'POST', body }),
      invalidatesTags: ['CoverLetter'],
    }),

    deleteDocument: build.mutation<void, string>({
      query: (id) => ({ url: `/documents/${id}`, method: 'DELETE', responseHandler: 'text' }),
      invalidatesTags: ['CoverLetter'],
    }),
  }),
})

export const {
  useGetDocumentsQuery,
  useCreateDocumentMutation,
  useDeleteDocumentMutation,
} = documentsApi
