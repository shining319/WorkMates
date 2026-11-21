// 状态徽标：圆点 + 文字，不只靠颜色区分。
// 职位状态（Open / Closed）和申请状态（Applied ... Completed）共用这一个徽标。
import {
  APPLICATION_STATUS_LABEL,
  type ApplicationStatus,
} from '../lib/constants'
import { STATUS_COLOR } from '../lib/statusColor'

type Status = 'OPEN' | 'CLOSED' | ApplicationStatus

const good = 'bg-success-bg text-success-text'
const neutral = 'bg-neutral-bg text-text-muted'

const label: Record<Status, string> = {
  OPEN: 'Open',
  CLOSED: 'Closed',
  ...APPLICATION_STATUS_LABEL,
}

const className: Record<Status, string> = {
  OPEN: good,
  CLOSED: neutral,
  ...STATUS_COLOR,
}

// 职位的关闭原因（MANUAL、FILLED）和申请被系统拒绝的原因（POSITION_FILLED）
const reasonLabel = {
  MANUAL: 'Manual',
  FILLED: 'Filled',
  POSITION_FILLED: 'Position filled',
}

export function StatusBadge({
  status,
  reason,
}: {
  status: Status
  /** Closed 职位的关闭原因，或招满时被系统拒绝的申请的原因 */
  reason?: keyof typeof reasonLabel | null
}) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-sm px-2 py-0.5 text-caption leading-5 font-medium whitespace-nowrap ${className[status]}`}
    >
      <span aria-hidden className="size-1.5 rounded-full bg-current" />
      {label[status]}
      {reason && <span className="opacity-80">· {reasonLabel[reason]}</span>}
    </span>
  )
}
