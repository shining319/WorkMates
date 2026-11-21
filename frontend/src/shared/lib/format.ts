// 显示用的格式化函数。金额用欧元，时间一律按都柏林时区显示（后端存的是 UTC 时间点）。

const eur = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
})

/** `13.5` -> `"€13.50"` */
export function formatEur(amount: number): string {
  return eur.format(amount)
}

const dublinDate = new Intl.DateTimeFormat('en-IE', {
  day: 'numeric',
  month: 'short',
  timeZone: 'Europe/Dublin',
})

/** ISO 时间 -> `"22 Sep"`（都柏林时间）。月份固定取前三个字母，因为 en-IE 会把 9 月写成 "Sept"。 */
export function formatDate(iso: string): string {
  const parts = dublinDate.formatToParts(new Date(iso))
  const day = parts.find((p) => p.type === 'day')?.value
  const month = parts.find((p) => p.type === 'month')?.value.slice(0, 3)
  return `${day} ${month}`
}

const dublinTime = new Intl.DateTimeFormat('en-IE', {
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
  timeZone: 'Europe/Dublin',
})

/** ISO 时间 -> `"24 Sep, 09:12"`（都柏林时间，24 小时制） */
export function formatDateTime(iso: string): string {
  return `${formatDate(iso)}, ${dublinTime.format(new Date(iso))}`
}

/** 每周工时最多保留一位小数：`8` -> `"8"`，`7.5` -> `"7.5"` */
export function formatHours(hours: number): string {
  return String(Number(hours.toFixed(1)))
}

const dublinDay = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Europe/Dublin',
})

/**
 * 通知时间（设计 10）：1 分钟内 "Just now"，1 小时内 "12 min ago"，24 小时内 "2 h ago"，
 * 都柏林时间的昨天 "Yesterday, 14:02"，更早 "12 Sep"。`now` 只在测试或特殊场合传。
 */
export function formatRelativeTime(iso: string, now = new Date()): string {
  const date = new Date(iso)
  const minutes = Math.floor((now.getTime() - date.getTime()) / 60_000)
  if (minutes < 1) return 'Just now'
  if (minutes < 60) return `${minutes} min ago`
  if (minutes < 24 * 60) return `${Math.floor(minutes / 60)} h ago`
  // en-CA 的日期格式是 2026-09-12，用来比较是不是同一天
  const yesterday = new Date(now.getTime() - 24 * 60 * 60_000)
  if (dublinDay.format(date) === dublinDay.format(yesterday)) {
    return `Yesterday, ${dublinTime.format(date)}`
  }
  return formatDate(iso)
}
