import type {
  ApplicationStatus,
  Area,
  JobType,
} from '../../shared/lib/constants'
import type { MatchBreakdown } from '../jobs/types'
import type { StudentProfile, Tag, TimeSlot } from '../profile/types'

export type PostingStatus = 'OPEN' | 'CLOSED'
/** 关闭原因：雇主手动关闭，或招满自动关闭 */
export type CloseReason = 'MANUAL' | 'FILLED'

/** `POST`、`PUT /api/postings` 和 `.../close` 返回的职位。日期是 ISO 时间字符串。 */
export interface Posting {
  id: number
  employerId: number
  title: string
  jobType: JobType
  hourlyRate: number
  weeklyHours: number
  area: Area
  address: string
  /** 没写描述时是 null */
  description: string | null
  headcount: number
  status: PostingStatus
  closeReason: CloseReason | null
  closedAt: string | null
  createdAt: string
  tags: Tag[]
  timeSlots: TimeSlot[]
}

/** `GET /api/postings/{id}`：职位加雇主信息和申请人数。 */
export interface PostingDetail extends Posting {
  employerName: string
  employerBio: string | null
  /** Accepted + Completed：已经录用的人数 */
  hiredCount: number
  /** Applied、Reviewed、Interview、Offer */
  inProgressCount: number
  /** 招满后被系统自动拒绝的申请数 */
  autoRejectedCount: number
}

/** `GET /api/postings/mine` 的一行。 */
export interface PostingListItem {
  id: number
  title: string
  jobType: JobType
  status: PostingStatus
  closeReason: CloseReason | null
  headcount: number
  hiredCount: number
  inProgressCount: number
  createdAt: string
}

/** `POST`、`PUT /api/postings` 的请求体：整个职位，标签和时间段整组替换。 */
export interface PostingRequest {
  title: string
  jobType: JobType
  hourlyRate: number
  weeklyHours: number
  area: Area
  address: string
  description: string
  headcount: number
  tags: string[]
  timeSlots: TimeSlot[]
}

/** 申请人列表的排序：匹配度或申请日期，都是降序 */
export type ApplicantSort = 'matchScore' | 'applicationDate'

/** `GET /api/postings/{id}/applicants` 的一行。匹配度按学生当前的档案计算。 */
export interface ApplicantRow {
  applicationId: number
  studentId: number
  studentName: string
  /** 没填时是 null */
  major: string | null
  matchPercent: number
  status: ApplicationStatus
  applicationDate: string
}

/** `GET /api/postings/{id}/applicants/{applicationId}`：学生当前的档案、联系方式和匹配明细。 */
export interface ApplicantDetail {
  applicationId: number
  postingId: number
  studentId: number
  studentName: string
  campusEmail: string
  /** 没填时是 null */
  phone: string | null
  status: ApplicationStatus
  applicationDate: string
  profile: StudentProfile
  matchBreakdown: MatchBreakdown
}
