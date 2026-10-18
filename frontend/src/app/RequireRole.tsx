import { Navigate, Outlet } from 'react-router'
import { useGetMeQuery } from '../features/auth/api'
import type { Role } from '../features/auth/types'
import { Button } from '../shared/ui/Button'
import { useAppSelector } from './store'

/**
 * 路由守卫（架构文档 §9.5）：包在需要登录的页面外面，通过才显示里面的页面。
 * - 没有令牌：跳到 /login。
 * - 传了 `allowed`（如 "STUDENT"）：角色不符的人只会看到"无权访问"的提示。
 *   后端每个请求还会再检查一次角色，这里只是为了界面体验。
 *
 * 不传 `allowed` 就只检查"是否登录"，所以路由里要用两层，见 router.tsx。
 */
export function RequireRole({ allowed }: { allowed?: Role }) {
  const token = useAppSelector((state) => state.auth.token)
  const user = useAppSelector((state) => state.auth.user)

  // 刷新页面后，令牌会从 localStorage 恢复，但 user 是空的，需要向后端请求一次。
  // skip 表示"跳过这次请求"：没有令牌，或者 user 已经有了，就不用请求。
  // 请求成功后，user 由 features/auth/api.ts 里的 onQueryStarted 写进 store。
  const { isError, refetch } = useGetMeQuery(undefined, {
    skip: !token || !!user,
  })

  if (!token) {
    return <Navigate to="/login" replace />
  }
  if (!user) {
    // 401 的情况上面已经退出登录了，走到这里说明是网络或服务器出错，给个重试按钮。
    return isError ? (
      <div role="alert" className="p-4">
        <p>We could not load your account.</p>
        <Button className="mt-3" onClick={() => refetch()}>
          Try again
        </Button>
      </div>
    ) : (
      <p role="status" className="p-4 text-text-muted">
        Loading…
      </p>
    )
  }
  if (allowed && user.role !== allowed) {
    return (
      <p role="alert" className="p-4">
        You do not have access to this page
      </p>
    )
  }
  // <Outlet /> 是"这里显示子路由的页面"。
  return <Outlet />
}
