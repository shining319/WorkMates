import { createBrowserRouter } from 'react-router'
import { AppLayout } from './layouts/AppLayout'
import { PublicLayout } from './layouts/PublicLayout'
import { LandingPage } from '../features/auth/pages/LandingPage'
import { LoginPage } from '../features/auth/pages/LoginPage'
import { RegisterPage } from '../features/auth/pages/RegisterPage'
import { PlaceholderPage } from './PlaceholderPage'
import { RequireRole } from './RequireRole'

// 路由表。目前只有注册和角色要用的几个路由，每个功能做的时候再按架构文档 §9.5 补自己的路由，
// 并把 PlaceholderPage（占位页）换成真正的页面。
//
// 怎么读：没有 `path` 只有 `element` 的项是"外壳"，它不对应网址，
// 只是把 `children` 包起来。外壳里的 <Outlet /> 就是子路由页面显示的位置。
// 嵌套的层次就是页面外面一层套一层：
//   公开页面：PublicLayout（页头只有登录/注册链接）
//   登录后的页面：RequireRole（没登录就跳走）
//                  └ AppLayout（页头有导航和退出按钮）
//                      └ RequireRole allowed="..."（再按角色放行）
//                          └ 具体页面
export const router = createBrowserRouter([
  {
    // 不用登录就能看的页面
    element: <PublicLayout />,
    children: [
      { path: '/', element: <LandingPage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
    ],
  },
  {
    // 第一层守卫：不传 allowed，只检查是否登录
    element: <RequireRole />,
    children: [
      {
        element: <AppLayout />,
        children: [
          {
            // 第二层守卫：只有学生能进
            element: <RequireRole allowed="STUDENT" />,
            children: [
              { path: '/jobs', element: <PlaceholderPage title="Jobs" /> },
              {
                path: '/profile',
                element: <PlaceholderPage title="Profile" />,
              },
            ],
          },
          {
            // 第二层守卫：只有雇主能进
            element: <RequireRole allowed="EMPLOYER" />,
            children: [
              {
                path: '/postings',
                element: <PlaceholderPage title="My postings" />,
              },
              {
                path: '/employer/profile',
                element: <PlaceholderPage title="Employer profile" />,
              },
            ],
          },
        ],
      },
    ],
  },
])
