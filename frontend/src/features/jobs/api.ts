import { baseApi } from '../../api/baseApi'
import type { Paged } from '../../api/types'
import type {
  ApplicationCreated,
  JobDetail,
  JobListItem,
  JobQuery,
} from './types'

export const jobsApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    // 学生的职位列表：只有 OPEN 的职位，按匹配度从高到低（同分新的在前），每页 20 条。
    // 多选的地区、类型用逗号拼成一个参数（后端两种写法都认）。
    getJobs: build.query<Paged<JobListItem>, JobQuery>({
      query: ({ keyword, areas, types, minRate, fitsMyTime, page }) => ({
        url: '/postings',
        // 值为 undefined 的参数不会出现在请求里
        params: {
          keyword: keyword || undefined,
          areas: areas.length > 0 ? areas.join(',') : undefined,
          types: types.length > 0 ? types.join(',') : undefined,
          minRate,
          fitsMyTime: fitsMyTime || undefined,
          page,
        },
      }),
      // 学生保存档案、雇主新建 / 编辑 / 关闭职位时，这个标签会失效，列表自动重新请求
      providesTags: ['JobList'],
    }),

    // 学生看到的职位详情：职位本身加匹配明细和自己的申请情况。
    // 带 JobList 标签：学生保存档案后，匹配明细也要重新算。
    getJob: build.query<JobDetail, number>({
      query: (id) => `/postings/${id}`,
      providesTags: (_result, _error, id) => [
        { type: 'Posting', id },
        'JobList',
      ],
    }),

    // 用保存的档案申请。成功和失败（重复申请、职位已关闭）后都让这个职位的详情重新请求，
    // 页面就会改成显示申请状态或"已关闭"。
    applyToPosting: build.mutation<ApplicationCreated, number>({
      query: (id) => ({ url: `/postings/${id}/applications`, method: 'POST' }),
      invalidatesTags: (_result, _error, id) => [{ type: 'Posting', id }],
    }),
  }),
})

export const { useGetJobsQuery, useGetJobQuery, useApplyToPostingMutation } =
  jobsApi
