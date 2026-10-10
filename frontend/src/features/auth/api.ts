import { baseApi } from '../../api/baseApi'
import { setUser } from './authSlice'
import type { User } from './types'

export const authApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getMe: build.query<User, void>({
      query: () => '/auth/me',
      providesTags: ['Me'],
      async onQueryStarted(_, { dispatch, queryFulfilled }) {
        try {
          const { data } = await queryFulfilled
          dispatch(setUser(data))
        } catch {
          // The error is already normalised by baseQuery; callers read it from the hook.
        }
      },
    }),
  }),
})

export const { useGetMeQuery } = authApi
