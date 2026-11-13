import { Check, Clock, Euro, MapPin, TriangleAlert, Users } from 'lucide-react'
import { AREA_LABEL } from '../../../shared/lib/constants'
import { formatEur, formatHours } from '../../../shared/lib/format'
import { DAYS } from '../../profile/timeSlots'
import type { Posting } from '../../postings/types'
import type { TimeSlot } from '../../profile/types'
import type { MatchBreakdown } from '../types'
import { SlotComparison } from './SlotComparison'

const dayLabel = Object.fromEntries(DAYS.map((d) => [d.value, d.label]))

/**
 * 职位的全部内容：关键数据、描述、每周时间段、所需技能。
 * 不含标题、状态、雇主信息和操作按钮，这些由各个页面自己放（雇主的职位详情，以后学生的职位详情）。
 * 学生版（US 3.4）传 `match`：时间段部分换成和空闲时间的对比，所需技能标出匹配和缺少的。
 * 雇主页面不传，显示不变。
 */
export function PostingFields({
  posting,
  match,
}: {
  posting: Posting
  match?: { breakdown: MatchBreakdown; freeSlots: TimeSlot[] }
}) {
  const missing = new Set(
    (match?.breakdown.missingTags ?? []).map((name) => name.toLowerCase()),
  )
  const facts = [
    { icon: Euro, text: `${formatEur(posting.hourlyRate)}/h` },
    { icon: Clock, text: `${formatHours(posting.weeklyHours)} h/week` },
    {
      icon: MapPin,
      text: `${AREA_LABEL[posting.area]} · ${posting.address}`,
    },
    {
      icon: Users,
      text: `${posting.headcount} ${posting.headcount === 1 ? 'place' : 'places'}`,
    },
  ]

  return (
    <div className="flex flex-col gap-6">
      <ul className="flex flex-wrap gap-x-5 gap-y-2">
        {facts.map(({ icon: Icon, text }) => (
          <li
            key={text}
            className="inline-flex items-center gap-1.5 text-body-sm text-text tabular-nums"
          >
            <Icon size={16} aria-hidden className="text-text-faint" />
            {text}
          </li>
        ))}
      </ul>

      {posting.description && (
        <p className="max-w-155 text-base whitespace-pre-line text-text">
          {posting.description}
        </p>
      )}

      {match ? (
        <SlotComparison
          coverage={match.breakdown.jobSlotCoverage}
          freeSlots={match.freeSlots}
          timePercent={match.breakdown.timePercent}
        />
      ) : (
        <section aria-labelledby="posting-slots">
          <h2 id="posting-slots" className="mb-2.5 text-title">
            Weekly time slots
          </h2>
          <ul className="divide-y divide-border rounded-md border border-border bg-surface">
            {posting.timeSlots.map((slot) => (
              <li
                key={`${slot.day}-${slot.start}`}
                className="flex gap-3 px-3 py-2 text-body-sm"
              >
                <span className="w-10 font-semibold text-text-strong">
                  {dayLabel[slot.day]}
                </span>
                <span className="text-text tabular-nums">
                  {slot.start}–{slot.end}
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}

      <section aria-labelledby="posting-skills">
        <h2 id="posting-skills" className="mb-2.5 text-title">
          Required skills
        </h2>
        <ul className="flex flex-wrap gap-1.5">
          {posting.tags.map((tag) => {
            // 学生版：缺少的技能用图标加 "missing" 标出，匹配的打勾
            const isMissing = missing.has(tag.name.toLowerCase())
            return (
              <li
                key={tag.id}
                className={`inline-flex items-center gap-1 rounded-xs px-1.5 py-px text-caption leading-5 ${isMissing ? 'bg-warning-bg text-warning-text' : 'bg-black/5 text-text'}`}
              >
                {match &&
                  (isMissing ? (
                    <TriangleAlert size={12} aria-hidden />
                  ) : (
                    <Check size={12} aria-hidden />
                  ))}
                {tag.name}
                {isMissing && <span className="font-medium">· missing</span>}
              </li>
            )
          })}
        </ul>
      </section>
    </div>
  )
}
