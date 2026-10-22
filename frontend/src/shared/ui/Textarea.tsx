import type { ComponentProps } from 'react'

// 多行文本框，外观和 Input 一致。用法同 Input：`{...register('bio')}` 可以直接展开传进来。
export function Textarea({
  className = '',
  ...props
}: ComponentProps<'textarea'>) {
  return (
    <textarea
      className={`w-full resize-y rounded-xs border border-border-input bg-surface px-2.5 py-1.5 text-body-sm leading-5.5 text-text-strong transition-colors placeholder:text-text-faint focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/25 focus-visible:outline-none aria-invalid:border-danger ${className}`}
      {...props}
    />
  )
}
