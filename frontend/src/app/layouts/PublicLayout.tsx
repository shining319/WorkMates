import { Link, Outlet } from 'react-router'

// 未登录页面（首页、登录、注册）的外壳：页头只有登录和注册链接。
// Link 和 <a> 的区别：点击时只切换页面内容，不会刷新整个网页。
export function PublicLayout() {
  return (
    <div className="min-h-screen">
      <header className="border-b border-border bg-surface">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-x-4 gap-y-2 px-4 py-3">
          <Link to="/" className="flex-1 font-bold text-primary">
            WorkMates
          </Link>
          <nav aria-label="Account" className="flex gap-4 text-sm">
            <Link to="/login">Log in</Link>
            <Link to="/register">Register</Link>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-6">
        {/* 当前路由对应的页面显示在这里 */}
        <Outlet />
      </main>
    </div>
  )
}
