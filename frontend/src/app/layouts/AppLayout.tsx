import { NavLink, Outlet } from 'react-router'
import { logout } from '../../features/auth/authSlice'
import { Button } from '../../shared/ui/Button'
import { useAppDispatch, useAppSelector } from '../store'

// NavLink 的 className 可以传函数：isActive 为 true 表示当前网址就是这个链接，
// 此时用高亮样式，否则用普通样式。
const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  isActive
    ? 'font-semibold text-text-strong'
    : 'text-text-muted hover:text-text'

// 登录后所有页面的外壳：页头（导航 + 退出按钮）加下面的页面内容。
// 每做一个功能，就在这里加一个导航入口。通知铃铛也加在这里。
export function AppLayout() {
  const role = useAppSelector((state) => state.auth.user?.role)
  const dispatch = useAppDispatch()

  return (
    <div className="min-h-screen">
      <header className="border-b border-border bg-surface">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-x-4 gap-y-2 px-4 py-3">
          <span className="text-[17px] font-bold tracking-[-0.3px] text-text-strong">
            WorkMates
          </span>
          <nav aria-label="Main" className="flex flex-1 gap-4 text-sm">
            {/* 按角色显示不同的导航：学生看到 Jobs，雇主看到 Postings */}
            {role === 'STUDENT' && (
              <>
                <NavLink to="/jobs" className={navLinkClass}>
                  Jobs
                </NavLink>
                <NavLink to="/profile" className={navLinkClass}>
                  Profile
                </NavLink>
              </>
            )}
            {role === 'EMPLOYER' && (
              <>
                <NavLink to="/postings" className={navLinkClass}>
                  My postings
                </NavLink>
                <NavLink to="/employer/profile" className={navLinkClass}>
                  Profile
                </NavLink>
              </>
            )}
          </nav>
          {/* 退出登录：logout 动作会清掉令牌、用户和所有缓存 */}
          <Button variant="utility" onClick={() => dispatch(logout())}>
            Log out
          </Button>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-6">
        {/* 当前路由对应的页面显示在这里 */}
        <Outlet />
      </main>
    </div>
  )
}
