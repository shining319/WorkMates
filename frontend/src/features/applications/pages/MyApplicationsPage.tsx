import { ChevronRight, FileText } from 'lucide-react'
import { Link, useSearchParams } from 'react-router'
import { formatDate } from '../../../shared/lib/format'
import { Banner } from '../../../shared/ui/Banner'
import { Button, ButtonLink } from '../../../shared/ui/Button'
import { Card } from '../../../shared/ui/Card'
import { Pagination } from '../../../shared/ui/Pagination'
import { StatusBadge } from '../../../shared/ui/StatusBadge'
import { Tabs } from '../../../shared/ui/Tabs'
import { useGetMyApplicationsQuery } from '../api'
import { StudentActions } from '../components/StudentActions'
import type { ApplicationGroup, MyApplication } from '../types'

const GROUPS: { id: ApplicationGroup; label: string }[] = [
  { id: 'IN_PROGRESS', label: 'In progress' },
  { id: 'OFFER', label: 'Offer' },
  { id: 'ACCEPTED', label: 'Accepted' },
  { id: 'FINISHED', label: 'Finished' },
]

/** 网址里的 ?group=，不认识的值当作"全部" */
function parseGroup(value: string | null): ApplicationGroup | undefined {
  return GROUPS.find((g) => g.id === value)?.id
}

/**
 * 一行申请。整行都能点进详情，但按钮不能放在链接里面（HTML 不允许），所以链接只包标题，
 * 用 after:absolute after:inset-0 把点击区域撑满整行；按钮的外层是 relative，叠在上面，点按钮不会跳转。
 * Offer 时有 Accept / Decline，Accepted 时有 Mark as Completed（设计 08）。
 * 用网格排：第一行是 文字 | 状态 | 箭头；手机上按钮单独占第二行。
 * sm 以上按设计 08：状态和箭头之间固定留 240px（15rem）靠右的按钮区，没有按钮的行也留着，
 * 所以每行的状态标签对齐在同一列。
 */
function ApplicationRow({ application }: { application: MyApplication }) {
  const hasActions =
    application.status === 'OFFER' || application.status === 'ACCEPTED'
  return (
    <li className="relative grid grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-x-4 gap-y-2 px-5 py-3 hover:bg-black/2 sm:grid-cols-[minmax(0,1fr)_auto_15rem_auto]">
      <div className="flex flex-col">
        <Link
          to={`/applications/${application.id}`}
          className="text-body-sm font-medium wrap-break-word text-text-strong after:absolute after:inset-0"
        >
          {application.postingTitle}
        </Link>
        <span className="text-caption wrap-break-word text-text-muted">
          {application.employerName} · updated{' '}
          {formatDate(application.updatedAt)}
        </span>
      </div>
      <StatusBadge
        status={application.status}
        reason={application.statusReason}
      />
      {/* 手机上：有按钮时指定在第二行占满（箭头会自动排到第一行第三列），没有按钮时不显示 */}
      <div
        className={`relative justify-end sm:flex ${hasActions ? 'col-span-full row-start-2 sm:col-span-1 sm:row-start-auto' : 'hidden'}`}
      >
        <StudentActions
          compact
          applicationId={application.id}
          postingId={application.postingId}
          status={application.status}
          employerName={application.employerName}
        />
      </div>
      <ChevronRight
        size={16}
        aria-hidden
        className="shrink-0 text-text-faint"
      />
    </li>
  )
}

/** 每个标签页的数量：各请求一次第 0 页，只取总数（和"我的职位"一样的做法） */
function useGroupCounts() {
  const all = useGetMyApplicationsQuery({ page: 0 })
  const inProgress = useGetMyApplicationsQuery({
    group: 'IN_PROGRESS',
    page: 0,
  })
  const offer = useGetMyApplicationsQuery({ group: 'OFFER', page: 0 })
  const accepted = useGetMyApplicationsQuery({ group: 'ACCEPTED', page: 0 })
  const finished = useGetMyApplicationsQuery({ group: 'FINISHED', page: 0 })
  return {
    ALL: all.currentData?.page.totalElements,
    IN_PROGRESS: inProgress.currentData?.page.totalElements,
    OFFER: offer.currentData?.page.totalElements,
    ACCEPTED: accepted.currentData?.page.totalElements,
    FINISHED: finished.currentData?.page.totalElements,
  }
}

// 我的申请（设计 08，US 4.6）。分组和页码都放在网址里：?group=OFFER&page=2。
// 每行的 Accept / Decline 是 US 4.4，Mark as Completed 是 US 4.7。
export function MyApplicationsPage() {
  const [params, setParams] = useSearchParams()
  const group = parseGroup(params.get('group'))
  // 网址里的页码从 1 开始，接口从 0 开始
  const page = Math.max(1, Math.trunc(Number(params.get('page'))) || 1)

  const current = useGetMyApplicationsQuery({ group, page: page - 1 })
  const counts = useGroupCounts()

  const tabs = [
    { id: 'ALL', label: 'All', count: counts.ALL },
    ...GROUPS.map((g) => ({ ...g, count: counts[g.id] })),
  ]

  function selectGroup(next: string) {
    setParams(next === 'ALL' ? {} : { group: next })
  }
  function selectPage(next: number) {
    const nextParams = new URLSearchParams(params)
    nextParams.set('page', String(next))
    setParams(nextParams)
  }

  const data = current.currentData
  const groupLabel = GROUPS.find((g) => g.id === group)?.label

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-h2">My applications</h1>

      <Tabs
        label="Application status"
        tabs={tabs}
        value={group ?? 'ALL'}
        onChange={selectGroup}
      />

      {!data && current.isError && (
        <div className="flex max-w-md flex-col items-start gap-3">
          <Banner tone="danger">We could not load your applications.</Banner>
          <Button variant="utility" onClick={() => void current.refetch()}>
            Try again
          </Button>
        </div>
      )}
      {!data && !current.isError && (
        <p className="text-body-sm text-text-muted">
          Loading your applications…
        </p>
      )}

      {data && data.items.length === 0 && (
        <div className="flex flex-col items-center gap-2 rounded-xl bg-surface px-8 py-12 text-center">
          <span className="mb-1 inline-flex size-11 items-center justify-center rounded-lg border border-border bg-surface">
            <FileText size={20} aria-hidden className="text-text-muted" />
          </span>
          <p className="text-[16px] leading-6 font-semibold text-text-strong">
            {groupLabel
              ? `No applications in "${groupLabel}"`
              : 'You have not applied to any jobs yet'}
          </p>
          <p className="max-w-90 text-body-sm text-text-muted">
            Find a job that fits your free time and apply with your profile.
          </p>
          <ButtonLink to="/jobs" className="mt-2">
            Browse jobs
          </ButtonLink>
        </div>
      )}

      {data && data.items.length > 0 && (
        <>
          <Card className="overflow-hidden">
            <ul className="divide-y divide-border">
              {data.items.map((application) => (
                <ApplicationRow
                  key={application.id}
                  application={application}
                />
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
