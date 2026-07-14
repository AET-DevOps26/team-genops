import { api } from '~/services/apiClient'
import type {
  ApplicationList,
  CreateApplicationRequest,
  JobApplication,
  JobPostingExtraction,
  JobPostingExtractRequest,
  UpdateApplicationRequest,
} from '~/api/schemas'

const applicationsApi = api.injectEndpoints({
  endpoints: (build) => ({
    listApplications: build.query<ApplicationList, void>({
      query: () => '/applications',
      providesTags: ['Application'],
    }),

    getApplication: build.query<JobApplication, string>({
      query: (id) => `/applications/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'Application', id }],
    }),

    createApplication: build.mutation<JobApplication, CreateApplicationRequest>({
      query: (body) => ({
        url: '/applications',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['Application'],
    }),

    updateApplication: build.mutation<JobApplication, { id: string; body: UpdateApplicationRequest }>({
      query: ({ id, body }) => ({
        url: `/applications/${id}`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => ['Application', { type: 'Application', id }],
    }),

    extractJobPosting: build.mutation<JobPostingExtraction, JobPostingExtractRequest>({
      query: (body) => ({
        url: '/job-postings/extract',
        method: 'POST',
        body,
      }),
    }),

    deleteApplication: build.mutation<void, string>({
      query: (id) => ({
        url: `/applications/${id}`,
        method: 'DELETE',
        responseHandler: 'text', // 204 No Content has no body; JSON.parse('') throws PARSING_ERROR
      }),
      invalidatesTags: ['Application'],
    }),
  }),
})

export const {
  useListApplicationsQuery,
  useGetApplicationQuery,
  useCreateApplicationMutation,
  useUpdateApplicationMutation,
  useExtractJobPostingMutation,
  useDeleteApplicationMutation,
} = applicationsApi
