import { Link, Outlet, useLocation } from 'react-router'
import { ButtonLink } from '../../shared/ui/Button'

// 未登录页面（首页、登录、注册）的外壳：只有页头，页面内容由各页面自己决定宽度和边距
// （首页的深色横幅要铺满整行，登录和注册的卡片要居中）。
// Link 和 <a> 的区别：点击时只切换页面内容，不会刷新整个网页。
export function PublicLayout() {
  const { pathname } = useLocation()

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 border-b border-border bg-surface">
        {/* 首页的内容区更宽（1200px），其他页面是 1024px */}
        <div
          className={`mx-auto flex h-14 items-center gap-6 px-4 ${pathname === '/' ? 'max-w-300' : 'max-w-5xl'}`}
        >
          <Link
            to="/"
            className="flex-1 text-[17px] leading-none font-bold tracking-[-0.3px] text-text-strong"
          >
            WorkMates
          </Link>
          {/* 已经在登录页就不再显示"Log in"，注册页同理 */}
          <nav aria-label="Account" className="flex items-center gap-2">
            {pathname !== '/login' && (
              <ButtonLink to="/login" variant="ghost">
                Log in
              </ButtonLink>
            )}
            {pathname !== '/register' && (
              <ButtonLink to="/register" variant="utility">
                Register
              </ButtonLink>
            )}
          </nav>
        </div>
      </header>
      <main>
        {/* 当前路由对应的页面显示在这里 */}
        <Outlet />
      </main>
    </div>
  )
}
