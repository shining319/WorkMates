import {
  createApi,
  fetchBaseQuery,
  type BaseQueryFn,
  type FetchArgs,
  type FetchBaseQueryError,
} from '@reduxjs/toolkit/query/react'
import { toast } from 'sonner'
import { logout, type AuthState } from '../features/auth/authSlice'
import type { ApiError, Result } from './types'

// RTK Query 自带的请求函数：基础地址是 /api，并给每个请求带上登录令牌。
const rawBaseQuery = fetchBaseQuery({
  baseUrl: '/api',
  prepareHeaders: (headers, { getState }) => {
    // 这里不能从 store.ts 引入 RootState：store.ts 引入了本文件，会形成循环引用。
    // 所以只把 state 当作"带 auth 的对象"来读。
    const token = (getState() as { auth: AuthState }).auth.token
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }
    return headers
  },
})

const FALLBACK_MESSAGE = 'Something went wrong. Please try again.'

/**
 * 把 RTK Query 的各种错误统一成我们自己的 `ApiError`，页面只需要处理这一种形状。
 * 后端返回的错误是 `Result` 格式，里面有 errorCode 和 errorMsg。
 * 网络断开、代理返回 502、返回的不是 JSON，这几种情况没有 errorCode，用 null 和兜底文案。
 */
function toApiError(error: FetchBaseQueryError): ApiError {
  // 取 HTTP 状态码：正常情况下 status 就是数字。
  // 返回的不是 JSON 时，status 是字符串 'PARSING_ERROR'，真正的状态码在 originalStatus 里。
  // 其他情况（如网络断开）没有状态码，记为 0。
  let status = 0
  if (typeof error.status === 'number') {
    status = error.status
  } else if (error.status === 'PARSING_ERROR') {
    status = error.originalStatus
  }

  // 只有后端按 Result 格式返回时，error.data 才是对象，才能读到 errorCode 和 errorMsg。
  const body =
    typeof error.data === 'object' && error.data !== null
      ? (error.data as Partial<Result<unknown>>)
      : undefined

  return {
    status,
    errorCode: body?.errorCode ?? null,
    errorMsg: body?.errorMsg ?? FALLBACK_MESSAGE,
  }
}

/**
 * 在 `fetchBaseQuery` 外再包一层（架构文档 §9.3），做三件事：
 * 1. 请求成功：后端的响应都是 `Result` 格式，这里把里面的 `data` 取出来。
 *    如果是分页结果，就返回 `{ items, page }`。
 * 2. 请求失败：统一转成 `ApiError`（见上面的 `toApiError`）。
 * 3. 401 且 errorCode 是 `UNAUTHORIZED`（令牌无效或过期）：自动退出登录。
 *    之后 `RequireRole` 发现没有令牌，会把用户送到 /login，并弹出后端给的提示 "Please log in again."。
 *    401 但 errorCode 是 `BAD_CREDENTIALS`（密码错）不在这里处理，交给登录表单显示。
 *
 * `BaseQueryFn<请求参数, 成功时的数据, 失败时的错误>` 只是声明这个函数的输入输出类型，
 * 后面两个泛型参数用不到，所以没写。
 */
const baseQuery: BaseQueryFn<string | FetchArgs, unknown, ApiError> = async (
  args,
  api,
  extraOptions,
) => {
  const result = await rawBaseQuery(args, api, extraOptions)

  if (result.error) {
    const error = toApiError(result.error)
    if (error.status === 401 && error.errorCode === 'UNAUTHORIZED') {
      api.dispatch(logout())
      // 固定 id：几个请求同时过期时，只显示一条提示。
      toast.info(error.errorMsg, { id: 'session-expired' })
    }
    return { error, meta: result.meta }
  }

  const body = result.data as Result<unknown>
  const data = body.page ? { items: body.data, page: body.page } : body.data
  return { data, meta: result.meta }
}

// 所有接口共用这一个 api 对象。各功能用 `baseApi.injectEndpoints` 往里加自己的接口，
// 例子见 features/auth/api.ts。
export const baseApi = createApi({
  reducerPath: 'api',
  baseQuery,
  // 标签用来在数据变化后让相关查询自动重新请求。哪个功能用到，就在这里加哪个标签。
  // JobList 现在还没有人读，学生保存档案时先让它失效，职位列表做的时候直接接上。
  tagTypes: ['Me', 'StudentProfile', 'EmployerProfile', 'JobList'],
  endpoints: () => ({}),
})
