interface CheckboxProps {
  label: string
  checked: boolean
  onChange: (checked: boolean) => void
}

/** 复选框。外面是 <label>：点文字也能切换，读屏软件会把文字当作复选框的名字。 */
export function Checkbox({ label, checked, onChange }: CheckboxProps) {
  return (
    <label className="flex cursor-pointer items-center gap-2.5 text-body-sm text-text-strong">
      <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="size-4 shrink-0 cursor-pointer accent-primary"
      />
      {label}
    </label>
  )
}
