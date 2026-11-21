import { ArrowLeft, Users } from 'lucide-react'
import { Link, useParams, useSearchParams } from 'react-router'
import { isApiError } from '../../../api/types'
import type { ApplicationStatus } from '../../../shared/lib/constants'
import { formatDate } from '../../../shared/lib/format'
import { Banner } from '../../../shared/ui/Banner'
import { Button, ButtonLink } from '../../../shared/ui/Button'
import { Card } from '../../../shared/ui/Card'
import { ItemUnavailable } from '../../../shared/ui/ItemUnavailable'
import { MatchScore } from '../../../shared/ui/MatchScore'
import { Pagination } from '../../../shared/ui/Pagination'
import { SelectMenu } from '../../../shared/ui/SelectMenu'
import { StatusBadge } from '../../../shared/ui/StatusBadge'
import { Tabs } from '../../../shared/ui/Tabs'
import { useGetApplicantsQuery, useGetPostingQuery } from '../api'
import { StatusActions } from '../components/StatusActions'
import type { ApplicantRow, ApplicantSort, PostingDetail } from '../types'

// 状态筛选：网址里的 ?status= 写法 -> 显示的文字和请求时传的状态。
// Hired = Accepted + Completed，和页面顶部 "x of y places filled" 的 x 口径一致。
const FILTERS = {
  APPLIED: { label: 'Applied', statuses: ['APPLIED'] },
  REVIEWED: { label: 'Reviewed', statuses: ['REVIEWED'] },
  INTERVIEW: { label: 'Interview', statuses: ['INTERVIEW'] },
  OFFER: { label: 'Offer', statuses: ['OFFER'] },
  HIRED: { label: 'Hired', statuses: ['ACCEPTED', 'COMPLETED'] },
  REJECTED: { label: 'Rejected', statuses: ['REJECTED'] },
  DECLINED: { label: 'Declined', statuses: ['DECLINED'] },
} satisfies Record<string, { label: string; statuses: ApplicationStatus[] }>
type Filter = keyof typeof FILTERS

// 前 5 个是标签页；Rejected、Declined 放在最后一个标签页旁边的下拉框里
const TAB_FILTERS: Filter[] = [
  'APPLIED',
  'REVIEWED',
  'INTERVIEW',
  'OFFER',
  'HIRED',
]
const MORE_FILTERS: Filter[] = ['REJECTED', 'DECLINED']

const SORT_OPTIONS: { value: ApplicantSort; label: string }[] = [
  { value: 'matchScore', label: 'Match score' },
  { value: 'applicationDate', label: 'Application date' },
]

// 桌面宽度下，表头和每一行用同一套列宽。最后一列放按钮，宽度固定，
// 不然每行按钮文字长短不同，前面几列会对不齐。
const columns =
  'md:grid-cols-[minmax(0,2fr)_1fr_1fr_1fr_12rem] md:items-center md:gap-x-4'

/** 网址里的 ?status=，不认识的值当作"全部" */
function parseFilter(value: string | null): Filter | undefined {
  // 用 hasOwn 而不是 in：in 会把 "toString" 这类继承来的名字也当成有
  return value !== null && Object.hasOwn(FILTERS, value)
    ? (value as Filter)
    : undefined
}

/** 某个筛选（不传就是全部）的申请人数：请求第 0 页，只取总数 */
function useApplicantCount(postingId: number, filter?: Filter) {
  const { currentData } = useGetApplicantsQuery(
    {
      postingId,
      statuses: filter && FILTERS[filter].statuses,
      sortBy: 'matchScore',
      page: 0,
    },
    { skip: !Number.isInteger(postingId) },
  )
  return currentData?.page.totalElements
}

/** 每个标签页和下拉框选项的数量。写成 8 次调用，因为 hook 不能放在循环里。 */
function useFilterCounts(postingId: number) {
  return {
    ALL: useApplicantCount(postingId),
    APPLIED: useApplicantCount(postingId, 'APPLIED'),
    REVIEWED: useApplicantCount(postingId, 'REVIEWED'),
    INTERVIEW: useApplicantCount(postingId, 'INTERVIEW'),
    OFFER: useApplicantCount(postingId, 'OFFER'),
    HIRED: useApplicantCount(postingId, 'HIRED'),
    REJECTED: useApplicantCount(postingId, 'REJECTED'),
    DECLINED: useApplicantCount(postingId, 'DECLINED'),
  }
}

/**
 * 招满横幅（设计 14b，US 4.5）："Barista is filled and closed. 5 remaining applications were set to Rejected."
 * N 是招满时被系统自动拒绝的申请数；没有时只说第一句。
 */
function filledMessage(posting: PostingDetail) {
  const n = posting.autoRejectedCount
  const rest =
    n === 0
      ? ''
      : n === 1
        ? ' 1 remaining application was set to Rejected.'
        : ` ${n} remaining applications were set to Rejected.`
  return `${posting.title} is filled and closed.${rest}`
}

function ApplicantRowItem({
  postingId,
  applicant,
}: {
  postingId: number
  applicant: ApplicantRow
}) {
  return (
    <li className={`grid gap-y-2 px-5 py-3 text-body-sm ${columns}`}>
      <div className="flex min-w-0 flex-col">
        <Link
          to={`/postings/${postingId}/applicants/${applicant.applicationId}`}
          className="truncate font-medium text-text-strong hover:underline"
        >
          {applicant.studentName}
        </Link>
        {applicant.major && (
          <span className="truncate text-caption text-text-muted">
            {applicant.major}
          </span>
        )}
      </div>
      {/* 手机上这三项排成一行；桌面上 contents 让它们直接成为表格的列 */}
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 md:contents">
        <span>
          <MatchScore percent={applicant.matchPercent} label="" />
        </span>
        <span>
          <StatusBadge status={applicant.status} />
        </span>
        <span className="text-text-muted tabular-nums">
          <span className="md:hidden">Applied </span>
          {formatDate(applicant.applicationDate)}
        </span>
      </div>
      <div className="flex md:justify-end">
        <StatusActions
          compact
          applicationId={applicant.applicationId}
          postingId={postingId}
          studentName={applicant.studentName}
          status={applicant.status}
        />
      </div>
    </li>
  )
}

// 某个职位的申请人（设计 13，US 4.2）；每行的推进和拒绝是 US 4.3，Mark as Completed 是 US 4.7，
// 招满横幅是 US 4.5。
// 筛选、排序和页码都放在网址里：?status=HIRED&sort=applicationDate&page=2。
export function ApplicantsPage() {
  const postingId = Number(useParams().id)
  // id 不是数字时不发请求，直接当作不存在
  const validId = Number.isInteger(postingId)
  const [params, setParams] = useSearchParams()
  const filter = parseFilter(params.get('status'))
  const sortBy: ApplicantSort =
    params.get('sort') === 'applicationDate' ? 'applicationDate' : 'matchScore'
  // 网址里的页码从 1 开始，接口从 0 开始
  const page = Math.max(1, Math.trunc(Number(params.get('page'))) || 1)

  const current = useGetApplicantsQuery(
    {
      postingId,
      statuses: filter && FILTERS[filter].statuses,
      sortBy,
      page: page - 1,
    },
    { skip: !validId },
  )
  const posting = useGetPostingQuery(postingId, { skip: !validId })
  const counts = useFilterCounts(postingId)

  /** 改一个参数；换筛选或排序时回到第 1 页 */
  function update(key: 'status' | 'sort' | 'page', value: string | null) {
    const next = new URLSearchParams(params)
    if (value === null) next.delete(key)
    else next.set(key, value)
    if (key !== 'page') next.delete('page')
    setParams(next)
  }

  // 不是自己的职位，后端返回 404
  if (
    !validId ||
    (!current.currentData &&
      isApiError(current.error) &&
      current.error.status === 404)
  ) {
    return (
      <ItemUnavailable>
        <ButtonLink to="/postings" variant="utility">
          Back to my postings
        </ButtonLink>
      </ItemUnavailable>
    )
  }

  const tabs = [
    { id: 'ALL', label: 'All', count: counts.ALL },
    ...TAB_FILTERS.map((f) => ({
      id: f,
      label: FILTERS[f].label,
      count: counts[f],
    })),
  ]
  // 数量放在选项右边的灰色小字里
  const moreOptions = MORE_FILTERS.map((f) => ({
    value: f,
    label: FILTERS[f].label,
    hint: counts[f]?.toString(),
  }))
  const inMore = filter !== undefined && MORE_FILTERS.includes(filter)

  const data = current.currentData
  const info = posting.currentData

  return (
    <div className="flex flex-col gap-4">
      <Link
        to="/postings"
        className="inline-flex items-center gap-1 self-start text-caption text-text-muted hover:text-text"
      >
        <ArrowLeft size={14} aria-hidden />
        My postings
      </Link>

      <div className="flex flex-wrap items-end gap-3">
        <div className="flex min-w-0 flex-1 basis-64 flex-col gap-1">
          <h1 className="text-h2 wrap-break-word">
            {info ? `${info.title} · applicants` : 'Applicants'}
          </h1>
          {info && (
            <p className="text-body-sm text-text-muted tabular-nums">
              {info.hiredCount} of {info.headcount}{' '}
              {info.headcount === 1 ? 'place' : 'places'} filled ·{' '}
              {info.inProgressCount} in progress
            </p>
          )}
        </div>
        <SelectMenu
          variant="pill"
          prefix="Sort"
          align="end"
          options={SORT_OPTIONS}
          value={sortBy}
          onChange={(next) =>
            update(
              'sort',
              next === 'applicationDate' ? 'applicationDate' : null,
            )
          }
        />
      </div>

      {info?.closeReason === 'FILLED' && (
        <Banner tone="success">{filledMessage(info)}</Banner>
      )}

      <Tabs
        label="Applicant status"
        tabs={tabs}
        value={filter ?? 'ALL'}
        onChange={(id) => update('status', id === 'ALL' ? null : id)}
        extra={
          <SelectMenu
            variant="pill"
            size="sm"
            placeholder="More statuses"
            options={moreOptions}
            value={inMore ? filter : ''}
            onChange={(next) => update('status', next)}
          />
        }
      />

      {!data && current.isError && (
        <div className="flex max-w-md flex-col items-start gap-3">
          <Banner tone="danger">We could not load the applicants.</Banner>
          <Button variant="utility" onClick={() => void current.refetch()}>
            Try again
          </Button>
        </div>
      )}
      {!data && !current.isError && (
        <p className="text-body-sm text-text-muted">Loading the applicants…</p>
      )}

      {data && data.items.length === 0 && (
        <div className="flex flex-col items-center gap-2 rounded-xl bg-surface px-8 py-12 text-center">
          <span className="mb-1 inline-flex size-11 items-center justify-center rounded-lg border border-border bg-surface">
            <Users size={20} aria-hidden className="text-text-muted" />
          </span>
          <p className="text-[16px] leading-6 font-semibold text-text-strong">
            {filter
              ? `No applicants in "${FILTERS[filter].label}"`
              : 'No applications yet'}
          </p>
        </div>
      )}

      {data && data.items.length > 0 && (
        <>
          <Card className="overflow-hidden">
            <div
              className={`hidden border-b border-border bg-surface-muted px-5 py-2.5 text-eyebrow tracking-[0.4px] text-text-muted uppercase md:grid ${columns}`}
            >
              <span>Student</span>
              <span>Match</span>
              <span>Status</span>
              <span>Applied</span>
              {/* 最后一列是按钮，没有标题 */}
              <span />
            </div>
            <ul className="divide-y divide-border">
              {data.items.map((applicant) => (
                <ApplicantRowItem
                  key={applicant.applicationId}
                  postingId={postingId}
                  applicant={applicant}
                />
              ))}
            </ul>
          </Card>
          <Pagination
            page={page}
            totalPages={data.page.totalPages}
            onChange={(next) => update('page', String(next))}
          />
        </>
      )}
    </div>
  )
}
