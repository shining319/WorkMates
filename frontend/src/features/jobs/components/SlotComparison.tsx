import { Check, Minus, X } from 'lucide-react'
import type { CSSProperties } from 'react'
import { DAYS, toMinutes } from '../../profile/timeSlots'
import type { TimeSlot } from '../../profile/types'
import type { SlotCoverage } from '../types'

// 一周网格：每天一列，左边是时间刻度。
//   浅绿底：你的空闲时间
//   深色实心块（里面写时间）：职位时间段里已被空闲时间覆盖的部分
//   斜线块：职位时间段里没被覆盖的部分
// 网格只是图，同样的信息都写在下面的列表里，所以网格对读屏软件隐藏（aria-hidden）。
// 手机宽度放不下 7 列，那时只显示列表。
const PX_PER_HOUR = 22
const MIN_HEIGHT = 200
const hatch =
  'repeating-linear-gradient(45deg, var(--color-text-strong) 0 1.5px, var(--color-surface) 1.5px 6px)'

type Range = { start: string; end: string }

/** `"14:00"` -> `"14"`，`"14:30"` 不变。块里地方小，用短写法。 */
function shortTime(time: string) {
  return time.endsWith(':00') ? String(Number(time.slice(0, 2))) : time
}

function Block({
  range,
  from,
  total,
  className,
  style,
  label,
}: {
  range: Range
  from: number
  total: number
  className: string
  style?: CSSProperties
  label?: boolean
}) {
  // 超出纵轴范围的部分裁掉
  const start = Math.max(toMinutes(range.start), from)
  const end = Math.min(toMinutes(range.end), from + total)
  if (end <= start) return null
  return (
    <div
      className={`absolute ${className}`}
      style={{
        top: `${((start - from) / total) * 100}%`,
        height: `${((end - start) / total) * 100}%`,
        ...style,
      }}
    >
      {label && (
        <span className="text-[11px] leading-none font-semibold">
          {shortTime(range.start)}–{shortTime(range.end)}
        </span>
      )}
    </div>
  )
}

function Grid({
  coverage,
  freeSlots,
}: {
  coverage: SlotCoverage[]
  freeSlots: TimeSlot[]
}) {
  // 纵轴只看职位的时间段，前后各留 1 小时，限制在 0–24 点
  const starts = coverage.map((c) => toMinutes(c.jobSlot.start))
  const ends = coverage.map((c) => toMinutes(c.jobSlot.end))
  const fromHour = Math.max(0, Math.floor(Math.min(...starts) / 60) - 1)
  const toHour = Math.min(24, Math.ceil(Math.max(...ends) / 60) + 1)
  const from = fromHour * 60
  const total = (toHour - fromHour) * 60
  // 时间范围很短时，网格也别太矮
  const height = Math.max((toHour - fromHour) * PX_PER_HOUR, MIN_HEIGHT)
  // 刻度每 2 小时一个
  const marks = Array.from({ length: toHour - fromHour + 1 }, (_, i) => ({
    hour: fromHour + i,
    top: `${(i / (toHour - fromHour)) * 100}%`,
  })).filter((_, i) => i % 2 === 0)

  return (
    <div
      aria-hidden
      className="hidden grid-cols-[40px_repeat(7,minmax(0,1fr))] gap-1.5 sm:grid"
    >
      <div />
      {DAYS.map((day) => (
        <div
          key={day.value}
          className="pb-1.5 text-center text-eyebrow tracking-[0.4px] text-text-muted uppercase"
        >
          {day.label}
        </div>
      ))}
      <div className="relative" style={{ height }}>
        {marks.map((mark) => (
          <span
            key={mark.hour}
            className="absolute right-1 -translate-y-1/2 text-[11px] leading-none text-text-faint tabular-nums"
            style={{ top: mark.top }}
          >
            {String(mark.hour).padStart(2, '0')}:00
          </span>
        ))}
      </div>
      {DAYS.map((day) => (
        <div
          key={day.value}
          className="relative rounded-sm bg-surface-muted"
          style={{ height }}
        >
          {freeSlots
            .filter((slot) => slot.day === day.value)
            .map((slot) => (
              <Block
                key={`free-${slot.start}`}
                range={slot}
                from={from}
                total={total}
                className="right-0 left-0 border-y border-success/40 bg-success-bg"
              />
            ))}
          {coverage
            .filter((c) => c.jobSlot.day === day.value)
            .flatMap((c) => [
              ...c.covered.map((range) => (
                <Block
                  key={`cov-${range.start}`}
                  range={range}
                  from={from}
                  total={total}
                  label
                  className="right-1.5 left-1.5 flex items-center justify-center rounded-xs bg-text-strong text-on-primary"
                />
              )),
              ...c.uncovered.map((range) => (
                <Block
                  key={`unc-${range.start}`}
                  range={range}
                  from={from}
                  total={total}
                  className="right-1.5 left-1.5 rounded-xs border-[1.5px] border-text-strong"
                  style={{ background: hatch }}
                />
              )),
            ])}
        </div>
      ))}
    </div>
  )
}

function Legend() {
  return (
    <ul className="hidden flex-wrap gap-x-5 gap-y-1 border-t border-border pt-3 text-caption text-text-muted sm:flex">
      <li className="inline-flex items-center gap-1.5">
        <span className="h-2.5 w-3.5 border border-success/40 bg-success-bg" />
        Your free time
      </li>
      <li className="inline-flex items-center gap-1.5">
        <span className="h-2.5 w-3.5 rounded-xs bg-text-strong" />
        Shift, covered
      </li>
      <li className="inline-flex items-center gap-1.5">
        <span
          className="h-2.5 w-3.5 rounded-xs border-[1.5px] border-text-strong"
          style={{ background: hatch }}
        />
        Shift, not covered
      </li>
    </ul>
  )
}

const dayLabel = Object.fromEntries(DAYS.map((d) => [d.value, d.label]))
const dayOrder = Object.fromEntries(DAYS.map((d, i) => [d.value, i]))

/** 一个职位时间段的覆盖说明：全覆盖、部分覆盖（写出空闲的那一段）、没覆盖。文字加图标，不只靠颜色。 */
function Coverage({ slot }: { slot: SlotCoverage }) {
  const free = slot.covered.map((r) => `${r.start}–${r.end}`).join(', ')
  if (slot.uncovered.length === 0) {
    return (
      <span className="inline-flex items-center gap-1 text-success-text">
        <Check size={14} aria-hidden />
        Fully covered
      </span>
    )
  }
  if (slot.covered.length === 0) {
    return (
      <span className="inline-flex items-center gap-1 text-danger-text">
        <X size={14} aria-hidden />
        Not covered
      </span>
    )
  }
  return (
    <span className="inline-flex items-center gap-1 text-warning-text">
      <Minus size={14} aria-hidden />
      Partly covered · {free} free
    </span>
  )
}

/**
 * 职位时间段和学生空闲时间的对比（设计 05）：一周网格、图例、每个时间段一行的覆盖说明。
 * 职位至少有一个时间段（后端要求），所以 `coverage` 不会是空的。
 */
export function SlotComparison({
  coverage,
  freeSlots,
  timePercent,
}: {
  coverage: SlotCoverage[]
  freeSlots: TimeSlot[]
  timePercent: number
}) {
  const sorted = [...coverage].sort(
    (a, b) =>
      dayOrder[a.jobSlot.day] - dayOrder[b.jobSlot.day] ||
      toMinutes(a.jobSlot.start) - toMinutes(b.jobSlot.start),
  )
  return (
    <section aria-labelledby="slot-comparison">
      <div className="mb-3 flex items-baseline justify-between gap-3">
        <h2 id="slot-comparison" className="text-title">
          Shifts vs your free time
        </h2>
        <span className="text-caption text-text-muted tabular-nums">
          {timePercent}% of shift time covered
        </span>
      </div>
      <div className="flex flex-col gap-3 rounded-lg border border-border bg-surface p-4 sm:pb-3">
        <Grid coverage={sorted} freeSlots={freeSlots} />
        <Legend />
        <ul className="divide-y divide-border rounded-md border border-border">
          {sorted.map((slot) => (
            <li
              key={`${slot.jobSlot.day}-${slot.jobSlot.start}`}
              className="flex flex-wrap items-center gap-x-3 gap-y-1 px-3 py-2 text-body-sm"
            >
              <span className="w-10 font-semibold text-text-strong">
                {dayLabel[slot.jobSlot.day]}
              </span>
              <span className="text-text tabular-nums">
                {slot.jobSlot.start}–{slot.jobSlot.end}
              </span>
              <Coverage slot={slot} />
            </li>
          ))}
        </ul>
        {freeSlots.length === 0 && (
          <p className="text-caption text-text-muted">
            You have no free time slots yet. Add them in your profile to see how
            well the shifts fit.
          </p>
        )}
      </div>
    </section>
  )
}
