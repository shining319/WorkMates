/** 星期的写法和后端 `java.time.DayOfWeek` 的名字一致。 */
export const DAY_VALUES = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
] as const
export type Day = (typeof DAY_VALUES)[number]

/** 每周的一个时间段。`start`、`end` 是 `HH:mm`，结束时间可以是 `24:00`。 */
export interface TimeSlot {
  day: Day
  start: string
  end: string
}

export interface Tag {
  id: number
  name: string
}

/** `GET /api/students/me/profile` 和 `PUT` 返回的学生档案。没填的字段是 null。 */
export interface StudentProfile {
  name: string
  phone: string | null
  major: string | null
  yearOfStudy: string | null
  expectedHourlyRate: number | null
  tags: Tag[]
  timeSlots: TimeSlot[]
  /** 有标签、期望时薪和至少一个时间段才算完整 */
  complete: boolean
  /** 缺的部分，取值：tags、expectedHourlyRate、timeSlots */
  missing: string[]
}

/** `PUT /api/students/me/profile` 的请求体：整份档案，标签和时间段整组替换。 */
export interface StudentProfileRequest {
  name: string
  phone: string
  major: string
  yearOfStudy: string
  expectedHourlyRate: number | null
  tags: string[]
  timeSlots: TimeSlot[]
}

/** 雇主档案。请求体和返回的形状一样，没填的字段返回 null。 */
export interface EmployerProfile {
  displayName: string
  bio: string | null
  contactEmail: string
  phone: string | null
}

export interface EmployerProfileRequest {
  displayName: string
  bio: string
  contactEmail: string
  phone: string
}
