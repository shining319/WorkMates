import { baseApi } from '../../api/baseApi'
import { setUser } from './authSlice'
import type { AuthResult, LoginRequest, RegisterRequest, User } from './types'

// 往 baseApi 里加认证相关的接口。其他功能照这个写法，各自一个 api.ts。
export const authApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    // build.query<返回的数据类型, 参数类型>：void 表示这个请求不需要参数。
    // 查询（GET）用 build.query；会修改数据的请求（POST 等）用 build.mutation。
    getMe: build.query<User, void>({
      query: () => '/auth/me',
      // 给这份缓存数据贴上 'Me' 标签。以后某个操作让 'Me' 失效，它会自动重新请求。
      providesTags: ['Me'],
      // 请求开始时会执行这个函数。这里等请求成功后，把用户存进 auth slice，
      // 其他地方（RequireRole、AppLayout）就能直接从 store 读用户。
      async onQueryStarted(_, { dispatch, queryFulfilled }) {
        try {
          const { data } = await queryFulfilled
          dispatch(setUser(data))
        } catch {
          // 失败的情况不用在这里处理：错误已经由 baseQuery 统一过，
          // 调用方可以通过 useGetMeQuery 返回的 isError 读到。
        }
      },
    }),
    // 注册：会修改数据的请求用 build.mutation。成功后由页面调用 setCredentials 保存令牌和用户。
    register: build.mutation<AuthResult, RegisterRequest>({
      query: (body) => ({ url: '/auth/register', method: 'POST', body }),
    }),
    login: build.mutation<AuthResult, LoginRequest>({
      query: (body) => ({ url: '/auth/login', method: 'POST', body }),
    }),
  }),
})

// RTK Query 根据接口名自动生成 hook：getMe -> useGetMeQuery。组件里直接用这个 hook。
export const { useGetMeQuery, useRegisterMutation, useLoginMutation } = authApi
