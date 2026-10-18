import { ChevronDown } from 'lucide-react'
import type { ComponentProps } from 'react'

interface Option {
  value: string
  label: string
}

interface SelectProps extends ComponentProps<'select'> {
  options: Option[]
  /** 没选时显示的文字（对应一个 value 为空的选项） */
  placeholder?: string
}

// 下拉框。原生 <select> 的箭头在各浏览器长得不一样，所以先用 appearance-none 去掉，
// 再在右边盖一个自己的箭头图标（pointer-events-none：点箭头时仍然点到下面的 select）。
export function Select({
  options,
  placeholder,
  className = '',
  ...props
}: SelectProps) {
  return (
    <div className="relative">
      <select
        className={`w-full appearance-none rounded-xs border border-border-input bg-surface py-1.5 pr-8 pl-2.5 text-body-sm leading-5.5 text-text-strong transition-colors focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/25 focus-visible:outline-none aria-invalid:border-danger ${className}`}
        {...props}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <ChevronDown
        size={16}
        aria-hidden
        className="pointer-events-none absolute top-1/2 right-2.5 -translate-y-1/2 text-text-muted"
      />
    </div>
  )
}
