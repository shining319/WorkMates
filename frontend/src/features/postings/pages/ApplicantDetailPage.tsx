import { ArrowLeft, Check, Mail, Phone } from 'lucide-react'
import { Link, useParams } from 'react-router'
import { isApiError } from '../../../api/types'
import { formatDate, formatEur } from '../../../shared/lib/format'
import { Banner } from '../../../shared/ui/Banner'
import { Button, ButtonLink } from '../../../shared/ui/Button'
import { Card } from '../../../shared/ui/Card'
import { ItemUnavailable } from '../../../shared/ui/ItemUnavailable'
import { StatusBadge } from '../../../shared/ui/StatusBadge'
import { useGetApplicationQuery } from '../../applications/api'
import { StatusHistory } from '../../applications/components/StatusHistory'
import { MatchBreakdown } from '../../jobs/components/MatchBreakdown'
import { SlotComparison } from '../../jobs/components/SlotComparison'
import { useGetApplicantQuery, useGetPostingQuery } from '../api'
import { StatusActions } from '../components/StatusActions'
import type { ApplicantDetail } from '../types'

/** 状态历史单独一个请求（`GET /api/applications/{id}`），失败时只影响这一块 */
function ApplicantHistory({
  applicationId,
  studentName,
}: {
  applicationId: number
  studentName: string
}) {
  const {
    currentData: data,
    isError,
    refetch,
  } = useGetApplicationQuery(applicationId)
  if (data) {
    return (
      <StatusHistory
        history={data.history}
        actorName={{
          STUDENT: studentName,
          EMPLOYER: 'you',
          SYSTEM: 'WorkMates',
        }}
      />
    )
  }
  if (isError) {
    return (
      <div className="flex flex-col items-start gap-3">
        <Banner tone="danger">We could not load the status history.</Banner>
        <Button variant="utility" onClick={() => void refetch()}>
          Try again
        </Button>
      </div>
    )
  }
  return (
    <p className="text-body-sm text-text-muted">Loading the status history…</p>
  )
}

function ApplicantDetailView({
  applicant,
  postingTitle,
}: {
  applicant: ApplicantDetail
  postingTitle?: string
}) {
  const { profile, matchBreakdown } = applicant
  const matched = new Set(
    matchBreakdown.matchedTags.map((name) => name.toLowerCase()),
  )
  const subtitle = [
    profile.major,
    profile.yearOfStudy,
    `applied ${formatDate(applicant.applicationDate)}`,
  ]
    .filter(Boolean)
    .join(' · ')

  return (
    <div>
      <Link
        to={`/postings/${applicant.postingId}/applicants`}
        className="mb-4 inline-flex items-center gap-1 text-caption text-text-muted hover:text-text"
      >
        <ArrowLeft size={14} aria-hidden />
        {postingTitle ? `${postingTitle} · applicants` : 'Applicants'}
      </Link>

      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div className="flex min-w-0 flex-col gap-1.5">
          <h1 className="text-h1 wrap-break-word">{applicant.studentName}</h1>
          <p className="text-base text-text-muted">{subtitle}</p>
        </div>
        <StatusBadge status={applicant.status} />
      </div>

      {/* 侧栏在文档里排在前面：手机上匹配度和操作先于档案；桌面用 col / row 放到右边 */}
      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
        <aside className="flex flex-col gap-4 lg:col-start-2 lg:row-start-1">
          <Card elevated className="flex flex-col gap-4 p-5">
            <MatchBreakdown breakdown={matchBreakdown} forEmployer />
            <StatusActions
              applicationId={applicant.applicationId}
              postingId={applicant.postingId}
              studentName={applicant.studentName}
              status={applicant.status}
            />
          </Card>
          <Card className="flex flex-col gap-2 p-5">
            <h2 className="text-eyebrow tracking-[0.4px] text-text-muted uppercase">
              Contact
            </h2>
            <p className="flex items-center gap-2 text-body-sm break-all text-text">
              <Mail
                size={16}
                aria-hidden
                className="shrink-0 text-text-faint"
              />
              <a
                href={`mailto:${applicant.campusEmail}`}
                className="hover:underline"
              >
                {applicant.campusEmail}
              </a>
            </p>
            {applicant.phone && (
              <p className="flex items-center gap-2 text-body-sm text-text">
                <Phone
                  size={16}
                  aria-hidden
                  className="shrink-0 text-text-faint"
                />
                {applicant.phone}
              </p>
            )}
          </Card>
        </aside>

        <div className="flex flex-col gap-6 lg:col-start-1 lg:row-start-1">
          <Card className="flex flex-col gap-5 p-5">
            <section aria-labelledby="applicant-skills">
              <h2
                id="applicant-skills"
                className="mb-2 text-eyebrow tracking-[0.4px] text-text-muted uppercase"
              >
                Skills and languages
              </h2>
              {profile.tags.length > 0 ? (
                <ul className="flex flex-wrap gap-1.5">
                  {profile.tags.map((tag) => {
                    // 职位要求的技能打勾
                    const isMatched = matched.has(tag.name.toLowerCase())
                    return (
                      <li
                        key={tag.id}
                        className="inline-flex items-center gap-1 rounded-xs bg-black/5 px-1.5 py-px text-caption leading-5 text-text"
                      >
                        {isMatched && <Check size={12} aria-hidden />}
                        {tag.name}
                        {isMatched && (
                          <span className="sr-only">
                            (required for this posting)
                          </span>
                        )}
                      </li>
                    )
                  })}
                </ul>
              ) : (
                <p className="text-body-sm text-text-muted">No skills added.</p>
              )}
            </section>
            <section aria-labelledby="applicant-pay">
              <h2
                id="applicant-pay"
                className="mb-1 text-eyebrow tracking-[0.4px] text-text-muted uppercase"
              >
                Expected pay
              </h2>
              <p className="text-body-sm font-semibold text-text-strong tabular-nums">
                {profile.expectedHourlyRate === null
                  ? 'Not set'
                  : `${formatEur(profile.expectedHourlyRate)}/h`}
              </p>
            </section>
          </Card>

          <SlotComparison
            coverage={matchBreakdown.jobSlotCoverage}
            freeSlots={profile.timeSlots}
            timePercent={matchBreakdown.timePercent}
            forEmployer
          />

          <ApplicantHistory
            applicationId={applicant.applicationId}
            studentName={applicant.studentName}
          />
        </div>
      </div>
    </div>
  )
}

// 申请人详情（设计 14，US 4.2）；推进和拒绝是 US 4.3。设计里的 "Reviews from employers" 是 US 6.3，以后再加。
export function ApplicantDetailPage() {
  const params = useParams()
  const postingId = Number(params.id)
  const applicationId = Number(params.applicationId)
  // id 不是数字时不发请求，直接当作不存在
  const validId = Number.isInteger(postingId) && Number.isInteger(applicationId)
  // 用 currentData 而不是 data：在页面内换到另一个申请人时，data 还留着上一个，
  // 新的请求失败（例如 404）也会继续显示旧内容。
  const {
    currentData: data,
    isFetching,
    error,
    refetch,
  } = useGetApplicantQuery({ postingId, applicationId }, { skip: !validId })
  // 只用来在返回链接里显示职位标题
  const posting = useGetPostingQuery(postingId, { skip: !validId })

  if (!data && isFetching) {
    return (
      <p className="text-body-sm text-text-muted">Loading the applicant…</p>
    )
  }
  // 不是自己职位下的申请，后端返回 404
  if (!validId || (!data && isApiError(error) && error.status === 404)) {
    return (
      <ItemUnavailable>
        <ButtonLink
          to={validId ? `/postings/${postingId}/applicants` : '/postings'}
          variant="utility"
        >
          {validId ? 'Back to applicants' : 'Back to my postings'}
        </ButtonLink>
      </ItemUnavailable>
    )
  }
  if (!data) {
    return (
      <div className="flex max-w-md flex-col items-start gap-3">
        <Banner tone="danger">We could not load this applicant.</Banner>
        <Button variant="utility" onClick={() => void refetch()}>
          Try again
        </Button>
      </div>
    )
  }
  return (
    <ApplicantDetailView
      applicant={data}
      postingTitle={posting.currentData?.title}
    />
  )
}
