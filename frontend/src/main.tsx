import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Provider } from 'react-redux'
import { RouterProvider } from 'react-router/dom'
import { Toaster } from 'sonner'
import { router } from './app/router'
import { store } from './app/store'
import './index.css'

// 前端的入口：把整个应用挂到 index.html 里 id 为 root 的节点上。
// 从外到内：
//   StrictMode     开发时多做一些检查（同一个组件会渲染两次，属正常现象）
//   Provider       让所有组件都能读写 Redux store
//   RouterProvider 根据网址显示 router.tsx 里对应的页面
//   Toaster        显示 toast 提示消息（sonner）
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Provider store={store}>
      <RouterProvider router={router} />
      <Toaster />
    </Provider>
  </StrictMode>,
)
