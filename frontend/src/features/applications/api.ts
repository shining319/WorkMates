import { baseApi } from '../../api/baseApi'
import type { Paged } from '../../api/types'
import type { ApplicationStatus } from '../../shared/lib/constants'
import type {
  ApplicationChanged,
  ApplicationDetail,
  ApplicationGroup,
  MyApplication,
} from './types'

export const applicationsApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    // 学生的申请，最近更新的在前，每页 20 条。不传 group 就是全部。page 从 0 开始。
    getMyApplications: build.query<
      Paged<MyApplication>,
      { group?: ApplicationGroup; page: number }
    >({
      query: (params) => ({ url: '/applications/mine', params }),
      providesTags: ['MyApplications'],
    }),

    // 申请详情和状态历史。学生看自己的，雇主看自己职位下的；别人的返回 404。
    getApplication: build.query<ApplicationDetail, number>({
      query: (id) => `/applications/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'Application', id }],
    }),

    // 所有手动改状态都走这一个接口。postingId 只用来让这个职位的计数（已录用、进行中）失效。
    // 失败时也让缓存失效：多半是别人已经改过了，重新请求后页面显示最新状态。
    // JobList：学生接受 Offer 可能让职位招满关闭，职位列表里要消失（US 4.5）。
    changeApplicationStatus: build.mutation<
      ApplicationChanged,
      { id: number; postingId: number; status: ApplicationStatus }
    >({
      query: ({ id, status }) => ({
        url: `/applications/${id}/status`,
        method: 'POST',
        body: { status },
      }),
      invalidatesTags: (_result, _error, { id, postingId }) => [
        { type: 'Application', id },
        { type: 'Posting', id: postingId },
        'Applicants',
        'MyApplications',
        'MyPostings',
        'JobList',
      ],
    }),
  }),
})

export const {
  useGetMyApplicationsQuery,
  useGetApplicationQuery,
  useChangeApplicationStatusMutation,
} = applicationsApi
