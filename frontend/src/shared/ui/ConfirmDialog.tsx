import { useEffect, useId, useRef } from 'react'
import { Button } from './Button'

interface ConfirmDialogProps {
  open: boolean
  title: string
  description: string
  confirmLabel: string
  /** 请求进行中：确认按钮不可点，避免重复提交 */
  busy?: boolean
  onConfirm: () => void
  onCancel: () => void
}

/**
 * 确认框，用原生 <dialog>。`showModal()` 会让浏览器自己处理这几件事：
 * 焦点留在框里、Tab 不会跑到后面的页面、背后变暗。按 Esc 触发 `cancel` 事件，我们当作"取消"。
 * `open` 由页面控制，这里用 effect 把它同步给 <dialog>。
 */
export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  busy,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      aria-labelledby={titleId}
      // 默认的 Esc 会直接关掉 <dialog>；拦住，让 open 只由页面决定
      onCancel={(event) => {
        event.preventDefault()
        if (!busy) onCancel()
      }}
      className="m-auto w-[calc(100%-2rem)] max-w-120 rounded-xl border border-border bg-surface p-0 shadow-card backdrop:bg-black/40"
    >
      <div className="flex flex-col gap-1 px-6 pt-5">
        <h2 id={titleId} className="text-title">
          {title}
        </h2>
        <p className="text-body-sm text-text-muted">{description}</p>
      </div>
      <div className="flex justify-end gap-2 px-6 pt-5 pb-5">
        <Button variant="ghost" disabled={busy} onClick={onCancel}>
          Cancel
        </Button>
        <Button variant="danger" disabled={busy} onClick={onConfirm}>
          {confirmLabel}
        </Button>
      </div>
    </dialog>
  )
}
