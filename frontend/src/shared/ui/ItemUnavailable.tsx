import { Inbox } from 'lucide-react'
import type { ReactNode } from 'react'

/**
 * 详情页请求得到 404 时显示（职位、申请、通知指向的内容已经不存在，或不属于当前用户）。
 * 后端对这两种情况都返回 404，所以这里不区分，也不暴露"其实存在"。
 * `children` 放一个返回列表的链接，例如 <ButtonLink to="/jobs">Back to jobs</ButtonLink>。
 */
export function ItemUnavailable({ children }: { children?: ReactNode }) {
  return (
    <div
      role="status"
      className="flex flex-col items-center gap-3 rounded-xl bg-surface-muted px-6 py-12 text-center"
    >
      <Inbox size={28} aria-hidden className="text-text-faint" />
      <p className="text-title">This item is no longer available</p>
      {children}
    </div>
  )
}
