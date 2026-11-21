import { ArrowLeft, Check, Mail, Phone } from 'lucide-react'
import { Link, useParams } from 'react-router'
import { isApiError } from '../../../api/types'
import {
  APPLICATION_STATUS_LABEL,
  AREA_LABEL,
  type ApplicationStatus,
} from '../../../shared/lib/constants'
import { formatEur, formatHours } from '../../../shared/lib/format'
import { Banner } from '../../../shared/ui/Banner'
import { Button, ButtonLink } from '../../../shared/ui/Button'
import { Card } from '../../../shared/ui/Card'
import { ItemUnavailable } from '../../../shared/ui/ItemUnavailable'
import { STATUS_COLOR } from '../../../shared/lib/statusColor'
import { StatusBadge } from '../../../shared/ui/StatusBadge'
import { useGetJobQuery } from '../../jobs/api'
import { describeSlot } from '../../profile/timeSlots'
import { useGetApplicationQuery } from '../api'
import { StatusHistory } from '../components/StatusHistory'
import { StudentActions } from '../components/StudentActions'
import type { ApplicationDetail } from '../types'

// 步骤条上的六步。Rejected 和 Declined 不在上面。
const STEPS: ApplicationStatus[] = [
  'APPLIED',
  'REVIEWED',
  'INTERVIEW',
  'OFFER',
  'ACCEPTED',
  'COMPLETED',
]

/**
 * 步骤条（设计 09）：已过的打勾，当前一步用这个状态自己的颜色（例如 Offer 是橙棕色），步骤之间用线连起来。
 * 被拒绝或婉拒时，步骤条停在之前那一步（最后一条历史的 fromStatus）。
 * 终态（Rejected、Declined、Completed）没有"当前一步"，停住的那一步也打勾。
 * 宽屏（lg）横排，连接线是横线；窄屏放不下六步，竖排，连接线是竖线。
 */
function StatusSteps({ application }: { application: ApplicationDetail }) {
  const ended = !STEPS.includes(application.status)
  const final = ended || application.status === 'COMPLETED'
  const last = application.history.at(-1)
  const reached = ended
    ? STEPS.indexOf(last?.fromStatus ?? 'APPLIED')
    : STEPS.indexOf(application.status)

  return (
    <Card className="p-5">
      <ol className="flex flex-col lg:flex-row lg:items-center">
        {STEPS.map((step, i) => {
          const done = i < reached || (final && i === reached)
          const current = !final && i === reached
          return (
            <li
              key={step}
              aria-current={current ? 'step' : undefined}
              className="flex flex-col lg:flex-1 lg:flex-row lg:items-center lg:last:flex-none"
            >
              <div className="flex items-center gap-2.5">
                <span
                  aria-hidden
                  className={`inline-flex size-7 shrink-0 items-center justify-center rounded-full text-caption font-semibold ${
                    done
                      ? 'bg-text-strong text-on-primary'
                      : current
                        ? `border-[1.5px] border-current ${STATUS_COLOR[step]}`
                        : 'border border-border-input bg-surface text-text-faint'
                  }`}
                >
                  {done ? <Check size={14} /> : i + 1}
                </span>
                <span
                  className={`text-body-sm whitespace-nowrap ${done || current ? 'font-medium text-text-strong' : 'text-text-faint'}`}
                >
                  {APPLICATION_STATUS_LABEL[step]}
                  {/* 颜色之外再用文字说明这一步的情况，给读屏软件 */}
                  <span className="sr-only">
                    {done ? ' (done)' : current ? ' (current)' : ''}
                  </span>
                </span>
              </div>
              {/* 连接线：窄屏是圆圈下面的竖线，宽屏是两步之间的横线 */}
              {i < STEPS.length - 1 && (
                <span
                  aria-hidden
                  className="my-1 ml-3.5 h-4 w-px bg-border-input lg:mx-4 lg:my-0 lg:h-px lg:w-auto lg:flex-1"
                />
              )}
            </li>
          )
        })}
      </ol>
    </Card>
  )
}

function EmployerContactCard({
  contact,
}: {
  contact: NonNullable<ApplicationDetail['employerContact']>
}) {
  return (
    <Card className="flex flex-col gap-2 p-5">
      <h2 className="text-eyebrow tracking-[0.4px] text-text-muted uppercase">
        Employer contact
      </h2>
      <p className="text-body-sm font-semibold text-text-strong">
        {contact.name}
      </p>
      <p className="flex items-center gap-2 text-body-sm text-text break-all">
        <Mail size={16} aria-hidden className="shrink-0 text-text-faint" />
        <a href={`mailto:${contact.contactEmail}`} className="hover:underline">
          {contact.contactEmail}
        </a>
      </p>
      {contact.phone && (
        <p className="flex items-center gap-2 text-body-sm text-text">
          <Phone size={16} aria-hidden className="shrink-0 text-text-faint" />
          {contact.phone}
        </p>
      )}
      <p className="text-caption text-text-muted">
        Interviews are arranged directly with the employer.
      </p>
    </Card>
  )
}

/**
 * Offer 卡片（设计 09）：职位的关键信息 + Accept offer / Decline offer。
 * 申请详情接口没有时薪、时长、时间段，另外请求一次职位详情；它还没回来或失败时只少这一行信息。
 */
function OfferCard({
  application,
  employerName,
}: {
  application: ApplicationDetail
  employerName: string
}) {
  const { currentData: job } = useGetJobQuery(application.postingId)
  const facts = job && [
    `${formatEur(job.hourlyRate)}/h`,
    `${formatHours(job.weeklyHours)} h/week`,
    job.timeSlots.map(describeSlot).join(', '),
    AREA_LABEL[job.area],
  ]

  return (
    <Card featured className="flex flex-col gap-3 p-5">
      <h2 className="text-title">{employerName} sent you an offer</h2>
      <p className="text-body-sm text-text tabular-nums">
        {facts && `${facts.join(' · ')}. `}
        There is no deadline, but the offer ends if the posting fills first.
      </p>
      <StudentActions
        applicationId={application.id}
        postingId={application.postingId}
        status={application.status}
        employerName={employerName}
      />
    </Card>
  )
}

/** Accepted 时的卡片：工作结束后由学生或雇主标记完成（US 4.7） */
function CompleteCard({
  application,
  employerName,
}: {
  application: ApplicationDetail
  employerName: string
}) {
  return (
    <Card className="flex flex-col gap-3 p-5">
      <h2 className="text-title">You accepted this job</h2>
      <p className="text-body-sm text-text">
        When the job is done, you or {employerName} can mark it as completed.
      </p>
      <StudentActions
        applicationId={application.id}
        postingId={application.postingId}
        status={application.status}
        employerName={employerName}
      />
    </Card>
  )
}

function ApplicationDetailView({
  application,
}: {
  application: ApplicationDetail
}) {
  const contact = application.employerContact
  const employerName = contact?.name ?? 'the employer'

  return (
    <div>
      <Link
        to="/applications"
        className="mb-4 inline-flex items-center gap-1 text-caption text-text-muted hover:text-text"
      >
        <ArrowLeft size={14} aria-hidden />
        My applications
      </Link>

      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div className="flex min-w-0 flex-col gap-1.5">
          <h1 className="text-h1 wrap-break-word">
            {application.postingTitle}
          </h1>
          <p className="text-base text-text-muted">
            {contact && `${contact.name} · `}
            <Link
              to={`/jobs/${application.postingId}`}
              className="text-primary hover:underline"
            >
              View job
            </Link>
          </p>
        </div>
        <StatusBadge
          status={application.status}
          reason={application.statusReason}
        />
      </div>

      {/* 步骤条占满整行（设计 09），六步横排才放得下 */}
      <div className="mb-6">
        <StatusSteps application={application} />
      </div>

      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,1fr)_300px]">
        <div className="flex flex-col gap-4">
          {application.status === 'OFFER' && (
            <OfferCard application={application} employerName={employerName} />
          )}
          {application.status === 'ACCEPTED' && (
            <CompleteCard
              application={application}
              employerName={employerName}
            />
          )}
          <StatusHistory
            history={application.history}
            actorName={{
              STUDENT: 'you',
              EMPLOYER: employerName,
              SYSTEM: 'WorkMates',
            }}
          />
        </div>
        {contact && <EmployerContactCard contact={contact} />}
      </div>
    </div>
  )
}

// 学生的申请详情（设计 09，US 4.6）。Offer 卡片的 Accept / Decline 是 US 4.4，Mark as Completed 是 US 4.7。
export function ApplicationDetailPage() {
  const id = Number(useParams().id)
  // id 不是数字时不发请求，直接当作不存在
  const validId = Number.isInteger(id)
  // 用 currentData 而不是 data：在页面内换到另一个 id 时，data 还留着上一个申请，
  // 新的请求失败（例如 404）也会继续显示旧内容。currentData 只对应当前的 id。
  const {
    currentData: data,
    isFetching,
    error,
    refetch,
  } = useGetApplicationQuery(id, { skip: !validId })

  if (!data && isFetching) {
    return (
      <p className="text-body-sm text-text-muted">Loading the application…</p>
    )
  }
  if (!validId || (!data && isApiError(error) && error.status === 404)) {
    return (
      <ItemUnavailable>
        <ButtonLink to="/applications" variant="utility">
          Back to my applications
        </ButtonLink>
      </ItemUnavailable>
    )
  }
  if (!data) {
    return (
      <div className="flex max-w-md flex-col items-start gap-3">
        <Banner tone="danger">We could not load this application.</Banner>
        <Button variant="utility" onClick={() => void refetch()}>
          Try again
        </Button>
      </div>
    )
  }
  return <ApplicationDetailView application={data} />
}
