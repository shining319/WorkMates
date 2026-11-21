import type { ApplicationStatus } from './constants'

// 申请状态每个一种颜色，和设计系统的 status-* 一致（index.css 的 --color-status-*）。
// StatusBadge 和步骤条的"当前一步"共用。
export const STATUS_COLOR: Record<ApplicationStatus, string> = {
  APPLIED: 'bg-status-applied-bg text-status-applied',
  REVIEWED: 'bg-status-reviewed-bg text-status-reviewed',
  INTERVIEW: 'bg-status-interview-bg text-status-interview',
  OFFER: 'bg-status-offer-bg text-status-offer',
  ACCEPTED: 'bg-status-accepted-bg text-status-accepted',
  COMPLETED: 'bg-status-completed-bg text-status-completed',
  REJECTED: 'bg-status-rejected-bg text-status-rejected',
  DECLINED: 'bg-status-declined-bg text-status-declined',
}
