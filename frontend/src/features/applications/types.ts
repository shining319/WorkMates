import type { ApplicationStatus } from '../../shared/lib/constants'

/** 我的申请的分组（US 4.6），和后端 `ApplicationGroup` 一致。 */
export type ApplicationGroup = 'IN_PROGRESS' | 'OFFER' | 'ACCEPTED' | 'FINISHED'

/** 系统改状态的原因。目前只有招满自动拒绝这一种；人改的状态是 null。 */
export type StatusReason = 'POSITION_FILLED'

/** 谁改的状态 */
export type ActorType = 'STUDENT' | 'EMPLOYER' | 'SYSTEM'

/** `GET /api/applications/mine` 的一行。日期是 ISO 时间字符串。 */
export interface MyApplication {
  id: number
  postingId: number
  postingTitle: string
  employerName: string
  status: ApplicationStatus
  statusReason: StatusReason | null
  createdAt: string
  /** 最后一次改状态的时间 */
  updatedAt: string
}

/** 状态历史的一步。第一步（创建申请）的 `fromStatus` 是 null。 */
export interface HistoryStep {
  fromStatus: ApplicationStatus | null
  toStatus: ApplicationStatus
  actorType: ActorType
  reason: StatusReason | null
  createdAt: string
}

export interface EmployerContact {
  name: string
  contactEmail: string
  /** 没填时是 null */
  phone: string | null
}

/** `GET /api/applications/{id}`：学生和雇主都能调。 */
export interface ApplicationDetail {
  id: number
  postingId: number
  postingTitle: string
  status: ApplicationStatus
  statusReason: StatusReason | null
  createdAt: string
  updatedAt: string
  /** 旧到新，第一步是 Applied */
  history: HistoryStep[]
  /** 只给学生；雇主调用时是 null */
  employerContact: EmployerContact | null
}

/** `POST /api/applications/{id}/status` 返回的申请 */
export interface ApplicationChanged {
  id: number
  postingId: number
  status: ApplicationStatus
  createdAt: string
}
