// 状态徽标：圆点 + 文字，不只靠颜色区分。
// 职位状态（Open / Closed）和申请状态（Applied ... Completed）共用这一个徽标。
import type { ApplicationStatus } from '../lib/constants'

type Status = 'OPEN' | 'CLOSED' | ApplicationStatus

const progress = 'bg-info-bg text-primary-pressed'
const good = 'bg-success-bg text-success-text'
const bad = 'bg-danger-bg text-danger-text'
const neutral = 'bg-neutral-bg text-text-muted'

const style: Record<Status, { label: string; className: string }> = {
  OPEN: { label: 'Open', className: good },
  CLOSED: { label: 'Closed', className: neutral },
  APPLIED: { label: 'Applied', className: progress },
  REVIEWED: { label: 'Reviewed', className: progress },
  INTERVIEW: { label: 'Interview', className: progress },
  OFFER: { label: 'Offer', className: good },
  ACCEPTED: { label: 'Accepted', className: good },
  DECLINED: { label: 'Declined', className: bad },
  REJECTED: { label: 'Rejected', className: bad },
  COMPLETED: { label: 'Completed', className: neutral },
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
