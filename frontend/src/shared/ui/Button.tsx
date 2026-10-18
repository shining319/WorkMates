import type { ButtonHTMLAttributes } from 'react'

type Variant = 'primary' | 'secondary'

// 两种按钮样式：主要按钮（实心）和次要按钮（描边）。
// Record<Variant, string> 表示：每个 Variant 都要有一个对应的样式字符串，少写一个会报错。
const variantClass: Record<Variant, string> = {
  primary: 'bg-primary text-on-primary hover:bg-primary-hover',
  secondary: 'border border-border bg-surface text-text hover:bg-surface-muted',
}

// 继承原生 <button> 的所有属性（onClick、disabled 等），再加上自己的 variant。
interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
}

export function Button({
  variant = 'primary',
  // 原生 button 默认 type 是 "submit"，放在表单里会意外提交表单。
  // 所以默认改成 "button"，需要提交时再手动传 type="submit"。
  type = 'button',
  className = '',
  // ...props 是"其余所有属性"，下面用 {...props} 原样传给 <button>。
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      className={`rounded-control px-4 py-2 text-sm font-medium focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:cursor-not-allowed disabled:opacity-50 ${variantClass[variant]} ${className}`}
      {...props}
    />
  )
}
