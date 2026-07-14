import { api } from '~/services/apiClient'
import type {
  ApplicationEventList,
  ApplicationList,
  CreateApplicationRequest,
  JobApplication,
  RecommendationList,
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

    listApplicationEvents: build.query<ApplicationEventList, string>({
      query: (id) => `/applications/${id}/events`,
      // Broad 'Application' tag: any mutation that invalidates applications (incl.
      // stage edits, which append a timeline event) refetches the timeline too.
      providesTags: ['Application'],
    }),

    listRecommendations: build.query<RecommendationList, string>({
      query: (id) => `/applications/${id}/recommendations`,
      providesTags: ['Application'],
    }),

    deleteRecommendation: build.mutation<void, { applicationId: string; recommendationId: string }>({
      query: ({ applicationId, recommendationId }) => ({
        url: `/applications/${applicationId}/recommendations/${recommendationId}`,
        method: 'DELETE',
        responseHandler: 'text', // 204 No Content has no body
      }),
      invalidatesTags: ['Application'],
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
  useDeleteApplicationMutation,
  useListApplicationEventsQuery,
  useListRecommendationsQuery,
  useDeleteRecommendationMutation,
} = applicationsApi
