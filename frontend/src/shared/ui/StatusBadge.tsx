// 状态徽标：圆点 + 文字，不只靠颜色区分。
// 目前只有职位状态（Open / Closed）。做申请状态（F4.6.1）时，在这里加 APPLIED、REVIEWED 等。

type Status = 'OPEN' | 'CLOSED'

const style: Record<Status, { label: string; className: string }> = {
  OPEN: { label: 'Open', className: 'bg-success-bg text-success-text' },
  CLOSED: { label: 'Closed', className: 'bg-neutral-bg text-text-muted' },
}

const reasonLabel = { MANUAL: 'Manual', FILLED: 'Filled' }

export function StatusBadge({
  status,
  reason,
}: {
  status: Status
  /** 关闭原因，只有 Closed 的职位有 */
  reason?: keyof typeof reasonLabel | null
}) {
  const { label, className } = style[status]
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-sm px-2 py-0.5 text-caption leading-5 font-medium whitespace-nowrap ${className}`}
    >
      <span aria-hidden className="size-1.5 rounded-full bg-current" />
      {label}
      {reason && <span className="opacity-80">· {reasonLabel[reason]}</span>}
    </span>
  )
}
