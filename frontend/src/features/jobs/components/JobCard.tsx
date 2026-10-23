import { Clock, Euro, MapPin } from 'lucide-react'
import {
  AREA_LABEL,
  type Area,
  type JobType,
} from '../../../shared/lib/constants'
import { formatHours } from '../../../shared/lib/format'
import { Card } from '../../../shared/ui/Card'
import { JobTypeBadge } from './JobTypeBadge'

interface JobCardProps {
  title: string
  employerName: string
  jobType: JobType
  /** 时薪（欧元）。职位表单的预览里，还没填好时传 null，显示 "—" */
  rate: number | null
  weeklyHours: number | null
  area: Area
  /** 已经格式化好的文字，例如 "22 Sep"、"just now" */
  postedAt: string
}

/**
 * 职位卡片（设计稿的 JobCard）。
 *
 * 【目前只是"显示版"】US 2.1 的职位表单为了做 "How students see it" 预览先建了它，
 * 所以现在它只负责显示，没有点击跳转，也没有匹配度。
 * 做 US 3.1（F3.1.1 职位列表）时：加上点击跳转到 /jobs/:id（可以包一层 <Link>，或加 `to` 属性）。
 * 做 US 3.3（F3.3.1）时：加 `matchScore` 属性，卡片底部左边显示匹配度（现在底部左边是空的）。
 */
export function JobCard({
  title,
  employerName,
  jobType,
  rate,
  weeklyHours,
  area,
  postedAt,
}: JobCardProps) {
  return (
    <Card className="flex flex-col gap-3 p-5">
      <div className="flex items-start gap-3">
        <div className="min-w-0 flex-1">
          <div className="text-[17px] leading-6 font-semibold tracking-[-0.2px] wrap-break-word text-text-strong">
            {title}
          </div>
          <div className="text-caption text-text-muted">
            {employerName || ' '}
          </div>
        </div>
        <JobTypeBadge type={jobType} />
      </div>

      <div className="flex flex-wrap items-stretch gap-2">
        <span className="inline-flex items-center gap-1.5 rounded-md bg-info-bg px-2.5 py-1 text-primary-pressed">
          <Euro size={14} aria-hidden />
          <span className="text-body-sm font-semibold tabular-nums">
            {rate === null ? '—' : rate.toFixed(2)}
          </span>
          <span className="text-caption">/h</span>
        </span>
        <span className="inline-flex items-center gap-1.5 rounded-md bg-surface-muted px-2.5 py-1">
          <Clock size={14} aria-hidden className="text-text-muted" />
          <span className="text-body-sm font-semibold text-text-strong tabular-nums">
            {weeklyHours === null ? '—' : formatHours(weeklyHours)}
          </span>
          <span className="text-caption text-text-muted">h/week</span>
        </span>
        <span className="inline-flex items-center gap-1.5 rounded-md bg-surface-muted px-2.5 py-1">
          <MapPin size={14} aria-hidden className="text-text-muted" />
          <span className="text-body-sm font-semibold text-text-strong">
            {AREA_LABEL[area]}
          </span>
        </span>
      </div>

      {/* 底部左边留给匹配度（US 3.3），现在只有发布时间 */}
      <div className="flex items-center justify-end gap-3 border-t border-border pt-3 text-caption text-text-faint">
        {postedAt}
      </div>
    </Card>
  )
}
