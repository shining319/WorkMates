interface SwitchProps {
  label: string
  description?: string
  checked: boolean
  onChange: (checked: boolean) => void
  /** 禁用时开关变灰、点不动；用 description 告诉用户为什么 */
  disabled?: boolean
}

/**
 * 开关（设计稿的 Switch）。
 * 外面是 <label>：点文字也能切换，读屏软件会把文字当作开关的名字。
 * 里面的 <button role="switch" aria-checked> 告诉读屏软件"这是一个开关，现在是开/关"。
 */
export function Switch({
  label,
  description,
  checked,
  onChange,
  disabled,
}: SwitchProps) {
  return (
    <label
      className={`flex items-center justify-between gap-4 ${disabled ? 'cursor-not-allowed' : 'cursor-pointer'}`}
    >
      <span className="flex flex-col">
        <span
          className={`text-body-sm leading-5.5 text-text-strong ${disabled ? 'opacity-50' : ''}`}
        >
          {label}
        </span>
        {description && (
          <span className="text-caption text-text-muted">{description}</span>
        )}
      </span>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={`relative h-5 w-8.5 shrink-0 cursor-pointer rounded-full transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${checked ? 'bg-primary' : 'bg-black/15'}`}
      >
        <span
          className={`absolute top-0.5 left-0.5 size-4 rounded-full bg-white shadow-sm transition-transform ${checked ? 'translate-x-3.5' : ''}`}
        />
      </button>
    </label>
  )
}
