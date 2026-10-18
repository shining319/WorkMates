import type { ButtonHTMLAttributes, ComponentProps } from 'react'
import { Link } from 'react-router'

type Variant = 'primary' | 'secondary' | 'utility' | 'ghost'
type Size = 'md' | 'lg'

// 四种按钮样式：
//   primary    蓝色实心药丸，页面上最重要的操作
//   secondary  白色药丸带轻阴影，和 primary 并排的次要操作
//   utility    8px 圆角加描边，页头、工具栏里的小按钮
//   ghost      没有底色，鼠标移上去才有浅灰底
// Record<Variant, string> 表示：每个 Variant 都要有一个对应的样式字符串，少写一个会报错。
const variantClass: Record<Variant, string> = {
  primary:
    'rounded-full bg-primary text-on-primary hover:bg-primary-hover active:bg-primary-pressed',
  secondary:
    'rounded-full bg-surface text-text-strong shadow-card hover:bg-surface-muted',
  utility:
    'rounded-md border border-border bg-surface text-text-strong hover:bg-black/4 active:bg-black/8',
  ghost: 'rounded-md text-text hover:bg-black/4 active:bg-black/8',
}

const sizeClass: Record<Size, string> = {
  md: 'px-4.5 py-2 text-body-sm',
  lg: 'px-6 py-3 text-base',
}

// Button 和 ButtonLink 外观一样，所以样式放在一个函数里共用。
// active:scale-96 是设计稿里的"按下去微微缩小"。
function buttonClass(variant: Variant, size: Size, className: string) {
  return `inline-flex items-center justify-center gap-2 font-medium whitespace-nowrap transition active:scale-96 disabled:cursor-not-allowed disabled:opacity-50 ${sizeClass[size]} ${variantClass[variant]} ${className}`
}

interface StyleProps {
  variant?: Variant
  size?: Size
}

// 继承原生 <button> 的所有属性（onClick、disabled 等），再加上自己的 variant 和 size。
interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>, StyleProps {}

export function Button({
  variant = 'primary',
  size = 'md',
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
      className={buttonClass(variant, size, className)}
      {...props}
    />
  )
}

/** 外观是按钮、实际是路由链接（点击切换页面，不刷新网页）。 */
export function ButtonLink({
  variant = 'primary',
  size = 'md',
  className = '',
  ...props
}: ComponentProps<typeof Link> & StyleProps) {
  return <Link className={buttonClass(variant, size, className)} {...props} />
}
