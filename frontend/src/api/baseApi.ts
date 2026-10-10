import {
  createApi,
  fetchBaseQuery,
  type BaseQueryFn,
  type FetchArgs,
  type FetchBaseQueryMeta,
} from '@reduxjs/toolkit/query/react'
import { logout, type AuthState } from '../features/auth/authSlice'
import type { ApiError, Result } from './types'

const rawBaseQuery = fetchBaseQuery({
  baseUrl: '/api',
  prepareHeaders: (headers, { getState }) => {
    // Cast instead of importing RootState from the store, which would be circular.
    const token = (getState() as { auth: AuthState }).auth.token
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }
    return headers
  },
})

const FALLBACK_MESSAGE = 'Something went wrong. Please try again.'

/**
 * Wraps `fetchBaseQuery` (architecture §9.3):
 * - success returns `data`, or `{ items, page }` for paged results;
 * - failure returns an `ApiError`, also for network errors and non-JSON replies;
 * - 401 `UNAUTHORIZED` logs the user out. `RequireRole` then sends them to /login.
 *   A 401 `BAD_CREDENTIALS` is left to the login form.
 */
const baseQuery: BaseQueryFn<
  string | FetchArgs,
  unknown,
  ApiError,
  object,
  FetchBaseQueryMeta
> = async (args, api, extraOptions) => {
  const result = await rawBaseQuery(args, api, extraOptions)

  if (result.error) {
    const e = result.error
    const status =
      typeof e.status === 'number'
        ? e.status
        : e.status === 'PARSING_ERROR'
          ? e.originalStatus
          : 0
    const body =
      typeof e.data === 'object' && e.data !== null
        ? (e.data as Partial<Result<unknown>>)
        : undefined
    const error: ApiError = {
      status,
      errorCode: body?.errorCode ?? null,
      errorMsg: body?.errorMsg ?? FALLBACK_MESSAGE,
    }
    if (status === 401 && error.errorCode === 'UNAUTHORIZED') {
      api.dispatch(logout())
    }
    return { error, meta: result.meta }
  }

  const body = result.data as Result<unknown>
  const data = body.page ? { items: body.data, page: body.page } : body.data
  return { data, meta: result.meta }
}

export const baseApi = createApi({
  reducerPath: 'api',
  baseQuery,
  // Add a tag here when the feature that uses it is built.
  tagTypes: ['Me'],
  endpoints: () => ({}),
})
