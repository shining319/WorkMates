import { Briefcase, ChevronRight, Plus } from 'lucide-react'
import { Link, useSearchParams } from 'react-router'
import { JOB_TYPE_LABEL } from '../../../shared/lib/constants'
import { formatDate } from '../../../shared/lib/format'
import { Banner } from '../../../shared/ui/Banner'
import { Button, ButtonLink } from '../../../shared/ui/Button'
import { Card } from '../../../shared/ui/Card'
import { Pagination } from '../../../shared/ui/Pagination'
import { StatusBadge } from '../../../shared/ui/StatusBadge'
import { Tabs } from '../../../shared/ui/Tabs'
import { useGetMyPostingsQuery } from '../api'
import type { PostingListItem, PostingStatus } from '../types'

// 桌面宽度下，表头和每一行用同一套列宽
const columns =
  'md:grid-cols-[minmax(0,2fr)_1fr_1fr_1fr_1fr_auto] md:items-center md:gap-x-4'

function PostingRow({ posting }: { posting: PostingListItem }) {
  return (
    <li className={`grid gap-y-2 px-5 py-3 text-body-sm md:py-3 ${columns}`}>
      <div className="flex flex-col">
        <Link
          to={`/postings/${posting.id}`}
          className="font-medium text-text-strong hover:underline"
        >
          {posting.title}
        </Link>
        <span className="text-caption text-text-muted">
          Posted {formatDate(posting.createdAt)}
        </span>
      </div>
      {/* 手机上这四项排成一行；桌面上 contents 让它们直接成为表格的列 */}
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 md:contents">
        <span className="text-text-muted">
          {JOB_TYPE_LABEL[posting.jobType]}
        </span>
        <span>
          <StatusBadge status={posting.status} reason={posting.closeReason} />
        </span>
        <span className="text-text tabular-nums">
          <span className="text-text-muted md:hidden">Hired </span>
          {posting.hiredCount} / {posting.headcount}
        </span>
        <span className="text-text tabular-nums">
          {posting.inProgressCount}
          <span className="text-text-muted md:hidden"> in progress</span>
        </span>
      </div>
      <div className="flex gap-1.5 md:justify-end">
        {posting.status === 'OPEN' && (
          <ButtonLink
            to={`/postings/${posting.id}/edit`}
            variant="ghost"
            size="sm"
          >
            Edit
          </ButtonLink>
        )}
        <ButtonLink
          to={`/postings/${posting.id}/applicants`}
          variant="utility"
          size="sm"
        >
          Applicants
          <ChevronRight size={14} aria-hidden />
        </ButtonLink>
      </div>
    </li>
  )
}

function EmptyState({
  title,
  description,
  showNewButton,
}: {
  title: string
  description: string
  showNewButton: boolean
}) {
  return (
    <div className="flex flex-col items-center gap-2 rounded-xl bg-surface px-8 py-12 text-center">
      <span className="mb-1 inline-flex size-11 items-center justify-center rounded-lg border border-border bg-surface">
        <Briefcase size={20} aria-hidden className="text-text-muted" />
      </span>
      <p className="text-[16px] leading-6 font-semibold text-text-strong">
        {title}
      </p>
      <p className="max-w-90 text-body-sm text-text-muted">{description}</p>
      {showNewButton && (
        <ButtonLink to="/postings/new" className="mt-2">
          <Plus size={16} aria-hidden />
          New posting
        </ButtonLink>
      )}
    </div>
  )
}

// 我的职位（设计 11 / 11b，US 2.4）。标签页和页码都放在网址里：?status=CLOSED&page=2。
export function MyPostingsPage() {
  const [params, setParams] = useSearchParams()
  const status: PostingStatus =
    params.get('status') === 'CLOSED' ? 'CLOSED' : 'OPEN'
  // 网址里的页码从 1 开始，接口从 0 开始
  const page = Math.max(1, Math.trunc(Number(params.get('page'))) || 1)
  const otherStatus: PostingStatus = status === 'OPEN' ? 'CLOSED' : 'OPEN'

  const current = useGetMyPostingsQuery({ status, page: page - 1 })
  // 接口一次只返回一个 status 的总数。为了给另一个标签页也显示数量，再请求它的第一页，只取总数。
  const other = useGetMyPostingsQuery({ status: otherStatus, page: 0 })

  const counts = {
    [status]: current.currentData?.page.totalElements,
    [otherStatus]: other.currentData?.page.totalElements,
  }
  const tabs = [
    { id: 'OPEN', label: 'Open', count: counts.OPEN },
    { id: 'CLOSED', label: 'Closed', count: counts.CLOSED },
  ]

  function selectStatus(next: string) {
    setParams(next === 'CLOSED' ? { status: 'CLOSED' } : {})
  }
  function selectPage(next: number) {
    const nextParams = new URLSearchParams(params)
    nextParams.set('page', String(next))
    setParams(nextParams)
  }

  const data = current.currentData

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-3">
        <h1 className="flex-1 text-h2">My postings</h1>
        <ButtonLink to="/postings/new">
          <Plus size={16} aria-hidden />
          New posting
        </ButtonLink>
      </div>

      <Tabs
        label="Posting status"
        tabs={tabs}
        value={status}
        onChange={selectStatus}
      />

      {!data && current.isError && (
        <div className="flex max-w-md flex-col items-start gap-3">
          <Banner tone="danger">We could not load your postings.</Banner>
          <Button variant="utility" onClick={() => void current.refetch()}>
            Try again
          </Button>
        </div>
      )}
      {!data && !current.isError && (
        <p className="text-body-sm text-text-muted">Loading your postings…</p>
      )}

      {data && data.items.length === 0 && (
        <EmptyState
          title={
            status === 'CLOSED'
              ? 'You have no closed postings'
              : counts.CLOSED
                ? 'You have no open postings'
                : 'You have no postings yet'
          }
          description={
            status === 'CLOSED'
              ? 'Postings you close, or that fill up, show up here.'
              : 'Create a posting with pay, hours and shifts. It takes a few minutes.'
          }
          showNewButton={status === 'OPEN'}
        />
      )}

      {data && data.items.length > 0 && (
        <>
          <Card className="overflow-hidden">
            <div
              className={`hidden border-b border-border bg-surface-muted px-5 py-2.5 text-eyebrow tracking-[0.4px] text-text-muted uppercase md:grid ${columns}`}
            >
              <span>Title</span>
              <span>Type</span>
              <span>Status</span>
              <span>Hired</span>
              <span>In progress</span>
              {/* 最后一列是按钮，占一个和按钮一样宽的空位 */}
              <span className="w-45" />
            </div>
            <ul className="divide-y divide-border">
              {data.items.map((posting) => (
                <PostingRow key={posting.id} posting={posting} />
              ))}
            </ul>
          </Card>
          <Pagination
            page={page}
            totalPages={data.page.totalPages}
            onChange={selectPage}
          />
        </>
      )}
    </div>
  )
}
