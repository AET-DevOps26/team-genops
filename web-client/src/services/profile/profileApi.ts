import { api } from '~/services/apiClient'
import type {
  Education,
  EducationRequest,
  Language,
  LanguageRequest,
  Profile,
  ProfileAggregateResponse,
  ProfileRequest,
  Skill,
  SkillRequest,
  WorkExperience,
  WorkExperienceRequest,
} from '~/api/schemas'

// Reads go through the single aggregate (one `Profile` cache entry); every mutation
// invalidates it. A 404 from getProfile is the contract's "needs onboarding" signal —
// callers branch on `error.status === 404`, it is not a failure state.
const profileApi = api.injectEndpoints({
  endpoints: (build) => ({
    getProfile: build.query<ProfileAggregateResponse, void>({
      query: () => '/profile',
      providesTags: ['Profile'],
    }),

    upsertProfile: build.mutation<Profile, ProfileRequest>({
      query: (body) => ({ url: '/profile', method: 'PUT', body }),
      invalidatesTags: ['Profile'],
    }),

    addWorkExperience: build.mutation<WorkExperience, WorkExperienceRequest>({
      query: (body) => ({ url: '/profile/work-experiences', method: 'POST', body }),
      invalidatesTags: ['Profile'],
    }),
    updateWorkExperience: build.mutation<WorkExperience, { id: string; body: WorkExperienceRequest }>({
      query: ({ id, body }) => ({ url: `/profile/work-experiences/${id}`, method: 'PUT', body }),
      invalidatesTags: ['Profile'],
    }),
    deleteWorkExperience: build.mutation<void, string>({
      query: (id) => ({
        url: `/profile/work-experiences/${id}`,
        method: 'DELETE',
        responseHandler: 'text',
      }),
      invalidatesTags: ['Profile'],
    }),

    addEducation: build.mutation<Education, EducationRequest>({
      query: (body) => ({ url: '/profile/educations', method: 'POST', body }),
      invalidatesTags: ['Profile'],
    }),
    updateEducation: build.mutation<Education, { id: string; body: EducationRequest }>({
      query: ({ id, body }) => ({ url: `/profile/educations/${id}`, method: 'PUT', body }),
      invalidatesTags: ['Profile'],
    }),
    deleteEducation: build.mutation<void, string>({
      query: (id) => ({
        url: `/profile/educations/${id}`,
        method: 'DELETE',
        responseHandler: 'text',
      }),
      invalidatesTags: ['Profile'],
    }),

    addSkill: build.mutation<Skill, SkillRequest>({
      query: (body) => ({ url: '/profile/skills', method: 'POST', body }),
      invalidatesTags: ['Profile'],
    }),
    updateSkill: build.mutation<Skill, { id: string; body: SkillRequest }>({
      query: ({ id, body }) => ({ url: `/profile/skills/${id}`, method: 'PUT', body }),
      invalidatesTags: ['Profile'],
    }),
    deleteSkill: build.mutation<void, string>({
      query: (id) => ({ url: `/profile/skills/${id}`, method: 'DELETE', responseHandler: 'text' }),
      invalidatesTags: ['Profile'],
    }),

    addLanguage: build.mutation<Language, LanguageRequest>({
      query: (body) => ({ url: '/profile/languages', method: 'POST', body }),
      invalidatesTags: ['Profile'],
    }),
    updateLanguage: build.mutation<Language, { id: string; body: LanguageRequest }>({
      query: ({ id, body }) => ({ url: `/profile/languages/${id}`, method: 'PUT', body }),
      invalidatesTags: ['Profile'],
    }),
    deleteLanguage: build.mutation<void, string>({
      query: (id) => ({ url: `/profile/languages/${id}`, method: 'DELETE', responseHandler: 'text' }),
      invalidatesTags: ['Profile'],
    }),
  }),
})

export const {
  useGetProfileQuery,
  useUpsertProfileMutation,
  useAddWorkExperienceMutation,
  useUpdateWorkExperienceMutation,
  useDeleteWorkExperienceMutation,
  useAddEducationMutation,
  useUpdateEducationMutation,
  useDeleteEducationMutation,
  useAddSkillMutation,
  useUpdateSkillMutation,
  useDeleteSkillMutation,
  useAddLanguageMutation,
  useUpdateLanguageMutation,
  useDeleteLanguageMutation,
} = profileApi
