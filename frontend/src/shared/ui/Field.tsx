import { CircleAlert } from 'lucide-react'
import { useId, type ReactNode } from 'react'

// 表单控件需要的三个属性。Field 生成好，交给里面的 Input / Select。
interface ControlProps {
  id: string
  'aria-describedby': string | undefined
  'aria-invalid': boolean
}

interface FieldProps {
  label: string
  /** 提示文字，没有错误时显示 */
  hint?: string
  /** 错误文字。有错误时替换掉提示 */
  error?: string
  /** 写成函数，Field 把 id 等属性传进来，你再交给 Input：
   *  <Field label="Email">{(props) => <Input {...props} />}</Field> */
  children: (controlProps: ControlProps) => ReactNode
}

/**
 * 一个表单项：标签 + 控件 + 提示或错误文字。
 * - `label` 的 htmlFor 和控件的 id 对上，点标签就能聚焦输入框，读屏软件也会读出标签。
 * - `aria-describedby` 让读屏软件在读控件时，一并读出提示或错误。
 * - 错误文字有图标和文字，不只靠红色表示；`role="alert"` 让读屏软件出错时立刻播报。
 */
export function Field({ label, hint, error, children }: FieldProps) {
  const id = useId()
  const messageId = `${id}-message`
  const hasMessage = Boolean(error || hint)

  return (
    <div className="flex flex-col gap-1.5">
      <label
        htmlFor={id}
        className="text-caption leading-5 font-medium text-text-strong"
      >
        {label}
      </label>
      {children({
        id,
        'aria-describedby': hasMessage ? messageId : undefined,
        'aria-invalid': Boolean(error),
      })}
      {error ? (
        <span
          id={messageId}
          role="alert"
          className="flex items-center gap-1.5 text-caption text-danger"
        >
          <CircleAlert size={14} aria-hidden className="shrink-0" />
          {error}
        </span>
      ) : hint ? (
        <span id={messageId} className="text-caption text-text-muted">
          {hint}
        </span>
      ) : null}
    </div>
  )
}
