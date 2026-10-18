import { CircleAlert, Info, type LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'

type Tone = 'danger' | 'info'

// 每种语气对应一个图标和一组颜色。图标加文字，不只靠颜色区分。
const toneStyle: Record<Tone, { icon: LucideIcon; className: string }> = {
  danger: { icon: CircleAlert, className: 'bg-danger-bg text-danger-text' },
  info: { icon: Info, className: 'bg-info-bg text-primary' },
}

/**
 * 一条整行的提示（表单顶部的错误、页面里的说明）。
 * 错误用 role="alert"，读屏软件会立刻播报；其他用 role="status"，等读完当前内容再播报。
 */
export function Banner({
  tone = 'info',
  children,
}: {
  tone?: Tone
  children: ReactNode
}) {
  const { icon: Icon, className } = toneStyle[tone]
  return (
    <div
      role={tone === 'danger' ? 'alert' : 'status'}
      className={`flex items-center gap-2.5 rounded-md px-3.5 py-2.5 text-body-sm ${className}`}
    >
      <Icon size={16} aria-hidden className="shrink-0" />
      <div className="flex-1">{children}</div>
    </div>
  )
}
