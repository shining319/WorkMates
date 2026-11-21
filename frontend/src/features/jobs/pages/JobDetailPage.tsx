import { ArrowLeft } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { isApiError } from '../../../api/types'
import { formatDate, formatEur, formatHours } from '../../../shared/lib/format'
import { Banner } from '../../../shared/ui/Banner'
import { Button, ButtonLink } from '../../../shared/ui/Button'
import { Card } from '../../../shared/ui/Card'
import { ItemUnavailable } from '../../../shared/ui/ItemUnavailable'
import { StatusBadge } from '../../../shared/ui/StatusBadge'
import { useGetJobQuery } from '../api'
import { ApplyDialog } from '../components/ApplyDialog'
import { JobTypeBadge } from '../components/JobTypeBadge'
import { MatchBreakdown } from '../components/MatchBreakdown'
import { PostingFields } from '../components/PostingFields'
import type { JobDetail } from '../types'

function JobDetailView({ job }: { job: JobDetail }) {
  const [applyOpen, setApplyOpen] = useState(false)

  return (
    // 手机上 Apply 固定在屏幕底部（设计 18），内容底部留出空间，不被它挡住
    <div className={job.canApply ? 'pb-20 lg:pb-0' : undefined}>
      <Link
        to="/jobs"
        className="mb-4 inline-flex items-center gap-1 text-caption text-text-muted hover:text-text"
      >
        <ArrowLeft size={14} aria-hidden />
        All jobs
      </Link>

      {/* 侧栏在文档里排在前面：手机上匹配度先于正文；桌面用 col / row 放到右边 */}
      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
        <aside className="flex flex-col gap-3 lg:col-start-2 lg:row-start-1 lg:sticky lg:top-6">
          <Card elevated className="flex flex-col gap-4 p-5">
            <MatchBreakdown breakdown={job.matchBreakdown} />

            {job.alreadyApplied && job.applicationStatus && (
              <div className="flex flex-col gap-2 border-t border-border pt-4">
                <div className="flex flex-wrap items-center gap-2 text-caption text-text-muted">
                  Your application:
                  <StatusBadge status={job.applicationStatus} />
                </div>
                {job.applicationId !== undefined && (
                  <Link
                    to={`/applications/${job.applicationId}`}
                    className="text-caption text-primary hover:underline"
                  >
                    View application
                  </Link>
                )}
              </div>
            )}
            {job.closedNotice && <Banner>{job.closedNotice}</Banner>}
            {job.canApply && (
              // 手机上 Apply 在底部固定栏里，卡片里的这个只在桌面显示。
              // 用外层 div 隐藏：Button 自带 inline-flex，会盖掉按钮上的 hidden。
              <div className="hidden lg:block">
                <Button
                  size="lg"
                  className="w-full"
                  onClick={() => setApplyOpen(true)}
                >
                  Apply
                </Button>
              </div>
            )}
          </Card>
          <p className="px-1 text-caption text-text-muted tabular-nums">
            {job.hiredCount} of {job.headcount}{' '}
            {job.headcount === 1 ? 'place' : 'places'} filled
            {job.status === 'OPEN' && ' · you can apply once'}
          </p>
        </aside>

        <div className="flex flex-col gap-6 lg:col-start-1 lg:row-start-1">
          <div className="flex flex-col gap-2.5">
            <div className="flex gap-2">
              <JobTypeBadge type={job.jobType} />
              <StatusBadge status={job.status} reason={job.closeReason} />
            </div>
            <h1 className="text-h1 wrap-break-word">{job.title}</h1>
            <p className="text-base text-text-muted">
              {job.employerName} · posted {formatDate(job.createdAt)}
            </p>
          </div>

          <PostingFields
            posting={job}
            match={{
              breakdown: job.matchBreakdown,
              freeSlots: job.studentFreeSlots,
            }}
          />

          <Card className="flex flex-col gap-1.5 p-5">
            <p className="text-eyebrow tracking-[0.4px] text-text-muted uppercase">
              About the employer
            </p>
            <p className="text-body-sm font-semibold text-text-strong">
              {job.employerName}
            </p>
            {job.employerBio && (
              <p className="text-body-sm whitespace-pre-line text-text">
                {job.employerBio}
              </p>
            )}
          </Card>
        </div>
      </div>

      {job.canApply && (
        <div className="fixed inset-x-0 bottom-0 z-10 flex items-center gap-3 border-t border-border bg-surface px-4 pt-3 pb-4 lg:hidden">
          <p className="flex-1 text-caption text-text-muted tabular-nums">
            <span className="text-body-sm font-semibold text-text-strong">
              {formatEur(job.hourlyRate)}/h
            </span>
            <br />
            {formatHours(job.weeklyHours)} h/week
          </p>
          <Button size="lg" onClick={() => setApplyOpen(true)}>
            Apply
          </Button>
        </div>
      )}

      {applyOpen && (
        <ApplyDialog
          postingId={job.id}
          title={job.title}
          employerName={job.employerName}
          onClose={() => setApplyOpen(false)}
        />
      )}
    </div>
  )
}

// 学生的职位详情（设计 05 / 05b / 18，US 3.4）；Apply 按钮是 US 4.1。
export function JobDetailPage() {
  const id = Number(useParams().id)
  // id 不是数字时不发请求，直接当作不存在
  const validId = Number.isInteger(id)
  const { data, isLoading, error, refetch } = useGetJobQuery(id, {
    skip: !validId,
  })

  if (isLoading) {
    return <p className="text-body-sm text-text-muted">Loading the job…</p>
  }
  if (!validId || (!data && isApiError(error) && error.status === 404)) {
    return (
      <ItemUnavailable>
        <ButtonLink to="/jobs" variant="utility">
          Back to jobs
        </ButtonLink>
      </ItemUnavailable>
    )
  }
  if (!data) {
    return (
      <div className="flex max-w-md flex-col items-start gap-3">
        <Banner tone="danger">We could not load this job.</Banner>
        <Button variant="utility" onClick={() => void refetch()}>
          Try again
        </Button>
      </div>
    )
  }
  return <JobDetailView job={data} />
}
