import { baseApi } from '../../api/baseApi'
import type { Paged } from '../../api/types'
import type { JobListItem, JobQuery } from './types'

export const jobsApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    // 学生的职位列表：只有 OPEN 的职位，最新的在前，每页 20 条。
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
  }),
})

export const { useGetJobsQuery } = jobsApi
