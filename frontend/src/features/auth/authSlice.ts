import { createSlice, type PayloadAction } from '@reduxjs/toolkit'
import { readToken } from './tokenStorage'
import type { User } from './types'

// 登录状态：token 是登录令牌，user 是当前用户。没登录时两者都是 null。
export interface AuthState {
  token: string | null
  user: User | null
}

// 令牌写回 localStorage 的事由 app/store.ts 里的 store.subscribe 负责，
// 这样下面的 reducer 都保持纯函数。
// 启动时先从 localStorage 读令牌；user 要等向后端请求 /auth/me 后才有。
const initialState: AuthState = { token: readToken(), user: null }

// slice = 一块 state + 修改它的方法（reducers）。
// 这里可以直接写 `state.token = ...`，因为 Redux Toolkit 内部会把它转成不可变更新。
const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    /** 登录或注册成功后调用，同时保存令牌和用户。 */
    setCredentials(
      state,
      // PayloadAction<...> 描述调用时传入的参数类型：dispatch(setCredentials({ token, user }))
      action: PayloadAction<{ token: string; user: User }>,
    ) {
      state.token = action.payload.token
      state.user = action.payload.user
    },
    /** 页面刷新后，请求 `/api/auth/me` 拿到用户时调用。 */
    setUser(state, action: PayloadAction<User>) {
      state.user = action.payload
    },
    /** 退出登录。这个动作还会清空 RTK Query 的缓存，见 app/store.ts。 */
    logout(state) {
      state.token = null
      state.user = null
    },
  },
})

// createSlice 会自动生成同名的"动作创建函数"，用法：dispatch(logout())
export const { setCredentials, setUser, logout } = authSlice.actions
export default authSlice.reducer
