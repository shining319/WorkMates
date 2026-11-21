import type { ComponentProps } from 'react'

// 文本输入框。ComponentProps<'input'> 表示"原生 input 的所有属性"（包括 ref），
// 所以 react-hook-form 的 `{...register('email')}` 可以直接展开传进来。
// 出错时 Field 会传 aria-invalid，边框就变红。
export function Input({ className = '', ...props }: ComponentProps<'input'>) {
  return (
    <input
      className={`w-full rounded-xs border border-border-input bg-surface px-2.5 py-1.5 text-body-sm leading-5.5 text-text-strong transition-colors placeholder:text-text-faint focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/25 focus-visible:outline-none aria-invalid:border-danger ${className}`}
      {...props}
    />
  )
}
