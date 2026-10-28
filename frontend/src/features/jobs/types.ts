import type { Area, JobType } from '../../shared/lib/constants'

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
