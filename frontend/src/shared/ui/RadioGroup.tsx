import type { ComponentProps, ReactNode } from 'react'

interface Option {
  value: string
  label: string
  icon?: ReactNode
}

interface RadioGroupProps extends Omit<ComponentProps<'input'>, 'type'> {
  /** 这一组选项的名字，读屏软件会先读它 */
  legend: string
  options: Option[]
}

/**
 * 分段选择（设计稿里的 Student / Employer）。
 * 底层是原生 radio：键盘可以用方向键切换，Tab 聚焦，不用自己写键盘逻辑。
 * 原生圆点用 sr-only 藏起来（读屏软件仍能读到），再用 `peer-checked:` 给后面的文字块上样式。
 * `...props`（name、onChange、ref 等）原样传给每个 radio，所以 `{...register('role')}` 能直接用。
 */
export function RadioGroup({ legend, options, ...props }: RadioGroupProps) {
  return (
    <fieldset className="flex flex-col gap-1.5">
      <legend className="mb-1.5 text-caption leading-5 font-medium text-text-strong">
        {legend}
      </legend>
      <div className="grid auto-cols-fr grid-flow-col gap-0 rounded-md bg-black/6 p-0.75 ring-1 ring-black/4 ring-inset">
        {options.map((option) => (
          <label key={option.value} className="cursor-pointer">
            <input
              type="radio"
              value={option.value}
              className="peer sr-only"
              {...props}
            />
            <span className="flex items-center justify-center gap-1.5 rounded-sm px-4 py-1.75 text-caption leading-5 font-medium text-text-muted transition-colors peer-checked:bg-black peer-checked:font-semibold peer-checked:text-white peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-primary">
              {option.icon}
              {option.label}
            </span>
          </label>
        ))}
      </div>
    </fieldset>
  )
}
