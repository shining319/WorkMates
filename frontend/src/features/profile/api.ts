import { baseApi } from '../../api/baseApi'
import type {
  EmployerProfile,
  EmployerProfileRequest,
  StudentProfile,
  StudentProfileRequest,
  Tag,
} from './types'

export const profileApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    // 标签候选词：参数是输入的前缀，返回最多 10 个。
    getTags: build.query<Tag[], string>({
      query: (q) => ({ url: '/tags', params: { q } }),
    }),

    getStudentProfile: build.query<StudentProfile, void>({
      query: () => '/students/me/profile',
      providesTags: ['StudentProfile'],
    }),
    updateStudentProfile: build.mutation<StudentProfile, StudentProfileRequest>(
      {
        query: (body) => ({
          url: '/students/me/profile',
          method: 'PUT',
          body,
        }),
        // 'Me'：姓名会同步到账号。'JobList'：档案变了，职位匹配结果也要重新算。
        invalidatesTags: ['StudentProfile', 'Me', 'JobList'],
      },
    ),

    getEmployerProfile: build.query<EmployerProfile, void>({
      query: () => '/employers/me/profile',
      providesTags: ['EmployerProfile'],
    }),
    updateEmployerProfile: build.mutation<
      EmployerProfile,
      EmployerProfileRequest
    >({
      query: (body) => ({
        url: '/employers/me/profile',
        method: 'PUT',
        body,
      }),
      invalidatesTags: ['EmployerProfile'],
    }),
  }),
})

export const {
  useGetTagsQuery,
  useGetStudentProfileQuery,
  useUpdateStudentProfileMutation,
  useGetEmployerProfileQuery,
  useUpdateEmployerProfileMutation,
} = profileApi
