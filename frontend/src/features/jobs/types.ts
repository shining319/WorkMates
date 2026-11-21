import type {
  Area,
  ApplicationStatus,
  JobType,
} from '../../shared/lib/constants'
import type { PostingDetail } from '../postings/types'
import type { TimeSlot } from '../profile/types'

/** `GET /api/postings` 的一张职位卡片。日期是 ISO 时间字符串。 */
export interface JobListItem {
  id: number
  title: string
  /** 雇主的显示名称 */
  employerName: string
  jobType: JobType
  hourlyRate: number
  weeklyHours: number
  area: Area
  createdAt: string
  /** 匹配度，0–100 的整数 */
  matchPercent: number
}

/** `GET /api/postings` 的查询条件。条件之间是"并且"，没填的不筛选。 */
export interface JobQuery {
  keyword: string
  areas: Area[]
  types: JobType[]
  /** 最低时薪（欧元）。没填就不传 */
  minRate?: number
  /** 只要空闲时间段覆盖职位时间的一半以上的职位 */
  fitsMyTime: boolean
  /** 从 0 开始 */
  page: number
}

/** 一段时间的覆盖情况。`start`、`end` 是 `HH:mm`。 */
export interface TimeRange {
  start: string
  end: string
  coverage: 'COVERED' | 'UNCOVERED'
}

/** 职位的一个时间段，和学生空闲时间重叠（covered）和没重叠（uncovered）的部分。 */
export interface SlotCoverage {
  jobSlot: TimeSlot
  covered: TimeRange[]
  uncovered: TimeRange[]
}

/** 学生看到的匹配明细。百分数都是整数。 */
export interface MatchBreakdown {
  overallPercent: number
  timePercent: number
  skillPercent: number
  payPercent: number
  matchedSkills: number
  requiredSkills: number
  matchedTags: string[]
  missingTags: string[]
  jobHourlyRate: number
  /** 学生没填期望时薪时是 null */
  expectedHourlyRate: number | null
  jobSlotCoverage: SlotCoverage[]
}

/**
 * 学生调用 `GET /api/postings/{id}` 得到的职位：职位详情加匹配明细和自己的申请情况。
 * 值为 null 的字段后端不返回，所以是可选的。
 */
export interface JobDetail extends PostingDetail {
  matchBreakdown: MatchBreakdown
  studentFreeSlots: TimeSlot[]
  alreadyApplied: boolean
  /** 已申请时才有 */
  applicationId?: number
  applicationStatus?: ApplicationStatus
  /** 职位开放且还没申请过 */
  canApply: boolean
  /** 职位已关闭时是 "This posting is closed" */
  closedNotice?: string
}

/** `POST /api/postings/{id}/applications` 返回的新申请。 */
export interface ApplicationCreated {
  id: number
  postingId: number
  status: ApplicationStatus
  createdAt: string
}
