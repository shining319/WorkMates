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

/** 每周工时最多保留一位小数：`8` -> `"8"`，`7.5` -> `"7.5"` */
export function formatHours(hours: number): string {
  return String(Number(hours.toFixed(1)))
}
