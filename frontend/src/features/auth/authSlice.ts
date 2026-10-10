import { createSlice, type PayloadAction } from '@reduxjs/toolkit'
import { readToken } from './tokenStorage'
import type { User } from './types'

export interface AuthState {
  token: string | null
  user: User | null
}

// The token is written back to localStorage by a store subscriber (app/store.ts),
// so the reducers stay pure.
const initialState: AuthState = { token: readToken(), user: null }

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    /** After login or register. */
    setCredentials(
      state,
      action: PayloadAction<{ token: string; user: User }>,
    ) {
      state.token = action.payload.token
      state.user = action.payload.user
    },
    /** After `/api/auth/me` on page load. */
    setUser(state, action: PayloadAction<User>) {
      state.user = action.payload
    },
    /** Also resets the API cache, see app/store.ts. */
    logout(state) {
      state.token = null
      state.user = null
    },
  },
})

export const { setCredentials, setUser, logout } = authSlice.actions
export default authSlice.reducer
