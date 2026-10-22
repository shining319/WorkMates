import type { Day } from './types'

// 时间段的公共部分：学生档案用，以后职位表单（F2.1.2）也用。规则和后端 TimeSlotRules 一致：
// 30 分钟一档，0–24 点，开始 < 结束，同一天不重叠，最多 21 个。

export const MAX_TIME_SLOTS = 21

export const DAYS: { value: Day; label: string }[] = [
  { value: 'MONDAY', label: 'Mon' },
  { value: 'TUESDAY', label: 'Tue' },
  { value: 'WEDNESDAY', label: 'Wed' },
  { value: 'THURSDAY', label: 'Thu' },
  { value: 'FRIDAY', label: 'Fri' },
  { value: 'SATURDAY', label: 'Sat' },
  { value: 'SUNDAY', label: 'Sun' },
]

/** `"14:30"` -> 870 */
export function toMinutes(time: string): number {
  const [hour, minute] = time.split(':').map(Number)
  return hour * 60 + minute
}

/** 870 -> `"14:30"`（1440 -> `"24:00"`） */
function formatMinutes(total: number): string {
  const hour = String(Math.floor(total / 60)).padStart(2, '0')
  const minute = String(total % 60).padStart(2, '0')
  return `${hour}:${minute}`
}

// 00:00, 00:30, ... , 24:00 共 49 个。开始时间不含 24:00，结束时间不含 00:00。
const allTimes = Array.from({ length: 49 }, (_, i) => formatMinutes(i * 30))
export const START_OPTIONS = allTimes.slice(0, -1).map(toOption)
export const END_OPTIONS = allTimes.slice(1).map(toOption)

function toOption(time: string) {
  return { value: time, label: time }
}

/** `"Wed 16:00–20:00"`，用在重叠提示里。 */
export function describeSlot(slot: {
  day: string
  start: string
  end: string
}): string {
  const day = DAYS.find((d) => d.value === slot.day)?.label ?? slot.day
  return `${day} ${slot.start}–${slot.end}`
}

/** 总时长（小时），最多保留一位小数：`15`、`7.5`。 */
export function totalHours(slots: { start: string; end: string }[]): number {
  const minutes = slots.reduce(
    (sum, slot) =>
      sum + Math.max(0, toMinutes(slot.end) - toMinutes(slot.start)),
    0,
  )
  return Number((minutes / 60).toFixed(1))
}
