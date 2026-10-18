import type { ComponentProps } from 'react'

interface CardProps extends ComponentProps<'div'> {
  /** 抬起的卡片（登录、注册）带轻阴影；普通卡片只有一圈描边 */
  elevated?: boolean
}

// 白底、1px 描边、12px 圆角的卡片。内边距由使用的地方通过 className 决定（如 p-6）。
export function Card({ elevated, className = '', ...props }: CardProps) {
  return (
    <div
      className={`rounded-lg border border-border bg-surface ${elevated ? 'shadow-card' : ''} ${className}`}
      {...props}
    />
  )
}
