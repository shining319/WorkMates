import { formatEur, formatHours } from '../../../shared/lib/format'
import { MatchScore } from '../../../shared/ui/MatchScore'
import { toMinutes } from '../../profile/timeSlots'
import type { MatchBreakdown as Breakdown } from '../types'

/** 职位的每周总时长和已被空闲时间覆盖的时长（小时），给 "7 of 8 shift hours covered" 用 */
function shiftHours(breakdown: Breakdown) {
  const length = (range: { start: string; end: string }) =>
    toMinutes(range.end) - toMinutes(range.start)
  let total = 0
  let covered = 0
  for (const slot of breakdown.jobSlotCoverage) {
    total += length(slot.jobSlot)
    covered += slot.covered.reduce((sum, range) => sum + length(range), 0)
  }
  return { total: total / 60, covered: covered / 60 }
}

function Row({
  label,
  percent,
  detail,
}: {
  label: string
  percent: number
  detail: string
}) {
  return (
    <div className="flex flex-col gap-1">
      <div className="flex justify-between text-body-sm font-medium text-text-strong tabular-nums">
        <span>{label}</span>
        <span>{percent}%</span>
      </div>
      {/* 进度条只是装饰，数字和说明已经写出来了 */}
      <div aria-hidden className="h-1 rounded-xs bg-black/6">
        <div
          className="h-full rounded-xs bg-text-strong"
          style={{ width: `${percent}%` }}
        />
      </div>
      <span className="text-caption text-text-muted">{detail}</span>
    </div>
  )
}

/**
 * 学生看到的匹配明细（设计 05 侧栏）：总分圆环，加时间、技能、薪资三项。
 * 三项各占三分之一，总分是它们的平均。
 */
export function MatchBreakdown({ breakdown }: { breakdown: Breakdown }) {
  const hours = shiftHours(breakdown)
  const skills = `${breakdown.matchedSkills} of ${breakdown.requiredSkills} skills`
  const pay =
    breakdown.expectedHourlyRate === null
      ? `${formatEur(breakdown.jobHourlyRate)}/h · you have not set an expected rate`
      : `${formatEur(breakdown.jobHourlyRate)}/h vs ${formatEur(breakdown.expectedHourlyRate)} expected`

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-4">
        <MatchScore percent={breakdown.overallPercent} size="lg" />
        <p className="text-caption text-text-muted">
          Based on your free time, skills and expected pay.
        </p>
      </div>
      <Row
        label="Time"
        percent={breakdown.timePercent}
        detail={`${formatHours(hours.covered)} of ${formatHours(hours.total)} shift hours covered`}
      />
      <Row
        label="Skills"
        percent={breakdown.skillPercent}
        detail={
          breakdown.missingTags.length > 0
            ? `${skills} · missing: ${breakdown.missingTags.join(', ')}`
            : skills
        }
      />
      <Row label="Pay" percent={breakdown.payPercent} detail={pay} />
    </div>
  )
}
