import { Clock, Euro, MapPin } from 'lucide-react'
import { Link } from 'react-router'
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
  /** 有它时整张卡片是链接（职位列表跳到职位详情）；职位表单的预览不传 */
  to?: string
}

/**
 * 职位卡片（设计稿的 JobCard）。
 *
 * 传 `to` 时整张卡片是链接（US 3.1 的职位列表用）；不传时只是显示（职位表单的预览用）。
 * 【还没有匹配度】做 US 3.3（F3.3.1）时：加 `matchScore` 属性，卡片底部左边显示匹配度（现在底部左边是空的）。
 */
export function JobCard({
  title,
  employerName,
  jobType,
  rate,
  weeklyHours,
  area,
  postedAt,
  to,
}: JobCardProps) {
  const card = (
    <Card
      className={`flex h-full flex-col gap-3 p-5 ${to ? 'transition-shadow group-hover:shadow-card' : ''}`}
    >
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
  if (!to) return card
  // group：鼠标移到链接上时，里面的卡片加阴影
  return (
    <Link to={to} className="group block h-full rounded-lg">
      {card}
    </Link>
  )
}
