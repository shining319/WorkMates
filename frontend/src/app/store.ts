import { combineReducers, configureStore } from '@reduxjs/toolkit'
import { setupListeners } from '@reduxjs/toolkit/query'
import { useDispatch, useSelector } from 'react-redux'
import { baseApi } from '../api/baseApi'
import authReducer, { logout } from '../features/auth/authSlice'
import { saveToken } from '../features/auth/tokenStorage'

// store 里的 state 分成两块：api（RTK Query 的请求缓存）和 auth（令牌和当前用户）。
// 新增 slice 时，在这里加一行即可。
const appReducer = combineReducers({
  [baseApi.reducerPath]: baseApi.reducer,
  auth: authReducer,
})

// 整个 state 的类型，由上面的 appReducer 自动推出来，不用手写。
export type RootState = ReturnType<typeof appReducer>

// 包一层 reducer：收到 logout 动作时，传 undefined 当旧 state，
// 所有 slice 都会回到初始值，包括 RTK Query 缓存。
// 这样换一个用户登录后，不会看到上一个用户的缓存数据。
// 其他动作原样交给 appReducer 处理。
const rootReducer: typeof appReducer = (state, action) =>
  logout.match(action)
    ? appReducer(undefined, action)
    : appReducer(state, action)

export const store = configureStore({
  reducer: rootReducer,
  // 加上 RTK Query 的中间件，它负责发请求和管理缓存。
  middleware: (getDefault) => getDefault().concat(baseApi.middleware),
})

export type AppDispatch = typeof store.dispatch

// 带类型的 useDispatch / useSelector。组件里用这两个，不要直接用 react-redux 的原版，
// 这样 `state.auth.token` 之类的写法才有类型提示和检查。
export const useAppDispatch = useDispatch.withTypes<AppDispatch>()
export const useAppSelector = useSelector.withTypes<RootState>()

// 窗口重新获得焦点时，让 RTK Query 自动重新请求（之后未读通知数的轮询要用）。
setupListeners(store.dispatch)

// 令牌变化时同步写进 localStorage，刷新页面后才不会掉线。
// 不在 reducer 里写，是因为 reducer 必须是纯函数（不能有读写存储这类副作用）。
// savedToken 记着上次写入的值，令牌没变时就不重复写。
let savedToken = store.getState().auth.token
store.subscribe(() => {
  const token = store.getState().auth.token
  if (token !== savedToken) {
    savedToken = token
    saveToken(token)
  }
})
