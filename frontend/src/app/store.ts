import { combineReducers, configureStore } from '@reduxjs/toolkit'
import { setupListeners } from '@reduxjs/toolkit/query'
import { useDispatch, useSelector } from 'react-redux'
import { baseApi } from '../api/baseApi'
import authReducer, { logout } from '../features/auth/authSlice'
import { saveToken } from '../features/auth/tokenStorage'

const appReducer = combineReducers({
  [baseApi.reducerPath]: baseApi.reducer,
  auth: authReducer,
})

export type RootState = ReturnType<typeof appReducer>

// On logout, start every slice from scratch so the next user never sees cached data.
const rootReducer: typeof appReducer = (state, action) =>
  logout.match(action)
    ? appReducer(undefined, action)
    : appReducer(state, action)

export const store = configureStore({
  reducer: rootReducer,
  middleware: (getDefault) => getDefault().concat(baseApi.middleware),
})

export type AppDispatch = typeof store.dispatch

export const useAppDispatch = useDispatch.withTypes<AppDispatch>()
export const useAppSelector = useSelector.withTypes<RootState>()

// Refetch on window focus (needed later by the unread-count polling).
setupListeners(store.dispatch)

// Keep the token in localStorage in step with the store.
let savedToken = store.getState().auth.token
store.subscribe(() => {
  const token = store.getState().auth.token
  if (token !== savedToken) {
    savedToken = token
    saveToken(token)
  }
})
