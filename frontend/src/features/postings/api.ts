import { baseApi } from '../../api/baseApi'
import type { Paged } from '../../api/types'
import type { ApplicationStatus } from '../../shared/lib/constants'
import type {
  ApplicantDetail,
  ApplicantRow,
  ApplicantSort,
  Posting,
  PostingDetail,
  PostingListItem,
  PostingRequest,
  PostingStatus,
} from './types'

export const postingsApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    // 我的职位。page 从 0 开始。返回里的 page.totalElements 是这个 status 下的职位总数。
    getMyPostings: build.query<
      Paged<PostingListItem>,
      { status: PostingStatus; page: number }
    >({
      query: (params) => ({ url: '/postings/mine', params }),
      providesTags: ['MyPostings'],
    }),

    getPosting: build.query<PostingDetail, number>({
      query: (id) => `/postings/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'Posting', id }],
    }),

    // 新建、编辑、关闭之后：我的职位列表要重新请求；学生的职位列表（JobList）也会变。
    createPosting: build.mutation<Posting, PostingRequest>({
      query: (body) => ({ url: '/postings', method: 'POST', body }),
      invalidatesTags: ['MyPostings', 'JobList'],
    }),
    updatePosting: build.mutation<
      Posting,
      { id: number; body: PostingRequest }
    >({
      query: ({ id, body }) => ({
        url: `/postings/${id}`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: 'Posting', id },
        'MyPostings',
        'JobList',
      ],
    }),
    closePosting: build.mutation<Posting, number>({
      query: (id) => ({ url: `/postings/${id}/close`, method: 'POST' }),
      invalidatesTags: (_result, _error, id) => [
        { type: 'Posting', id },
        'MyPostings',
        'JobList',
      ],
    }),

    // 某个职位的申请人，只有职位所有者能看（别人的返回 404）。每页 20 条，page 从 0 开始。
    // statuses 可以有多个（Hired = Accepted + Completed），用逗号拼成一个 status 参数；不传就是全部。
    getApplicants: build.query<
      Paged<ApplicantRow>,
      {
        postingId: number
        statuses?: ApplicationStatus[]
        sortBy: ApplicantSort
        page: number
      }
    >({
      query: ({ postingId, statuses, sortBy, page }) => ({
        url: `/postings/${postingId}/applicants`,
        params: { status: statuses?.join(','), sortBy, page },
      }),
      providesTags: ['Applicants'],
    }),

    getApplicant: build.query<
      ApplicantDetail,
      { postingId: number; applicationId: number }
    >({
      query: ({ postingId, applicationId }) =>
        `/postings/${postingId}/applicants/${applicationId}`,
      providesTags: ['Applicants'],
    }),
  }),
})

export const {
  useGetMyPostingsQuery,
  useGetPostingQuery,
  useCreatePostingMutation,
  useUpdatePostingMutation,
  useClosePostingMutation,
  useGetApplicantsQuery,
  useGetApplicantQuery,
} = postingsApi
