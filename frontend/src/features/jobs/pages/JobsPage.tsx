import { Search, SlidersHorizontal } from 'lucide-react'
import { useCallback, useState } from 'react'
import { useSearchParams } from 'react-router'
import {
  AREA_LABEL,
  AREA_VALUES,
  JOB_TYPE_LABEL,
  JOB_TYPE_VALUES,
} from '../../../shared/lib/constants'
import { formatDate } from '../../../shared/lib/format'
import { Banner } from '../../../shared/ui/Banner'
import { Button } from '../../../shared/ui/Button'
import { FilterChip } from '../../../shared/ui/FilterChip'
import { Input } from '../../../shared/ui/Input'
import { Pagination } from '../../../shared/ui/Pagination'
import { useGetStudentProfileQuery } from '../../profile/api'
import { useGetJobsQuery } from '../api'
import { JobCard } from '../components/JobCard'
import { JobFilters } from '../components/JobFilters'
import { useUrlText } from '../useUrlText'

// 最低时薪：整数或最多两位小数，和后端的时薪规则一致
const MONEY = /^\d+(\.\d{1,2})?$/

/** 网址里逗号分隔的值 -> 合法的值（按固定顺序，不认识的丢掉，免得后端返回 400） */
function parseList<T extends string>(
  raw: string | null,
  allowed: readonly T[],
): T[] {
  const values = (raw ?? '').split(',')
  return allowed.filter((value) => values.includes(value))
}

function EmptyState({ onClear }: { onClear: () => void }) {
  return (
    <div className="flex flex-col items-center gap-2 rounded-xl bg-surface px-8 py-12 text-center">
      <span className="mb-1 inline-flex size-11 items-center justify-center rounded-lg border border-border bg-surface">
        <Search size={20} aria-hidden className="text-text-muted" />
      </span>
      <p className="text-[16px] leading-6 font-semibold text-text-strong">
        No jobs found
      </p>
      <p className="max-w-90 text-body-sm text-text-muted">
        Try a different keyword or remove some filters.
      </p>
      <Button variant="utility" className="mt-2" onClick={onClear}>
        Clear all filters
      </Button>
    </div>
  )
}

// 学生的职位列表（设计 04 / 04b / 17，US 3.1、3.2）。
// 关键词、筛选条件和页码都放在网址里：
//   /jobs?keyword=barista&areas=ATHLONE,REMOTE&types=PART_TIME&minRate=12&fitsMyTime=true&page=2
// 所以后退、刷新、复制链接给别人，看到的都是同一个结果。网址里的页码从 1 开始，接口从 0 开始。
export function JobsPage() {
  const [params, setParams] = useSearchParams()
  const keyword = params.get('keyword') ?? ''
  const areas = parseList(params.get('areas'), AREA_VALUES)
  const types = parseList(params.get('types'), JOB_TYPE_VALUES)
  const minRateParam = (params.get('minRate') ?? '').trim()
  const fitsParam = params.get('fitsMyTime') === 'true'
  const page = Math.max(1, Math.trunc(Number(params.get('page'))) || 1)
  const minRate = MONEY.test(minRateParam) ? Number(minRateParam) : undefined

  // 学生没有空闲时间段时，"Fits my time" 不能用（结果一定为空）。
  // 网址里带着它也当作没开，免得分享来的链接是个空列表。
  const profile = useGetStudentProfileQuery()
  const hasSlots = profile.data ? profile.data.timeSlots.length > 0 : undefined
  const canFitMyTime = hasSlots !== false
  const fitsMyTime = fitsParam && canFitMyTime
  // 网址要求 Fits my time、但还不知道有没有空闲时间段：等档案回来再请求，免得先闪一下别的结果
  const waitForProfile = fitsParam && !profile.isSuccess && !profile.isError

  const jobs = useGetJobsQuery(
    {
      keyword: keyword.trim(),
      areas,
      types,
      minRate,
      fitsMyTime,
      page: page - 1,
    },
    { skip: waitForProfile },
  )

  const [filtersOpen, setFiltersOpen] = useState(false)

  /** 改网址里的条件（值为 undefined 就删掉），并回到第 1 页。replace：不往浏览器历史里加一条。 */
  const update = useCallback(
    (changes: Record<string, string | undefined>, replace = false) => {
      setParams(
        (previous) => {
          const next = new URLSearchParams(previous)
          for (const [name, value] of Object.entries(changes)) {
            if (value) next.set(name, value)
            else next.delete(name)
          }
          next.delete('page')
          return next
        },
        { replace },
      )
    },
    [setParams],
  )

  // 打字的两个输入框：停顿一下才写进网址，写入时用 replace，后退键不会一个字一个字地退
  const [keywordText, setKeywordText] = useUrlText(
    keyword,
    useCallback((value: string) => update({ keyword: value }, true), [update]),
  )
  const [minRateText, setMinRateText] = useUrlText(
    params.get('minRate') ?? '',
    useCallback((value: string) => update({ minRate: value }, true), [update]),
  )
  const minRateInvalid =
    minRateText.trim() !== '' && !MONEY.test(minRateText.trim())

  const chips = [
    ...areas.map((area) => ({
      key: `area-${area}`,
      label: AREA_LABEL[area],
      remove: () =>
        update({ areas: areas.filter((a) => a !== area).join(',') }),
    })),
    ...types.map((type) => ({
      key: `type-${type}`,
      label: JOB_TYPE_LABEL[type],
      remove: () =>
        update({ types: types.filter((t) => t !== type).join(',') }),
    })),
    ...(minRate === undefined
      ? []
      : [
          {
            key: 'minRate',
            label: `Min €${minRateParam}/h`,
            remove: () => update({ minRate: undefined }),
          },
        ]),
    ...(fitsMyTime
      ? [
          {
            key: 'fits',
            label: 'Fits my time',
            remove: () => update({ fitsMyTime: undefined }),
          },
        ]
      : []),
  ]

  function clearAll() {
    setParams({})
  }
  function selectPage(next: number) {
    const nextParams = new URLSearchParams(params)
    nextParams.set('page', String(next))
    setParams(nextParams)
  }

  const data = jobs.data
  const total = data?.page.totalElements ?? 0

  return (
    <div className="flex flex-col gap-5">
      <h1 className="text-h2">Jobs</h1>

      {/* 桌面：左栏筛选，右边搜索和结果。手机：一列，筛选面板在搜索框下面，点 Filters 展开 */}
      <div className="grid grid-cols-1 gap-x-6 gap-y-3.5 md:grid-cols-[220px_minmax(0,1fr)] md:grid-rows-[auto_1fr] md:items-start">
        <div className="flex gap-2 md:col-start-2 md:row-start-1">
          <div className="relative flex-1">
            <Search
              size={16}
              aria-hidden
              className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-text-muted"
            />
            <Input
              type="search"
              aria-label="Search jobs"
              placeholder="Search jobs"
              value={keywordText}
              onChange={(event) => setKeywordText(event.target.value)}
              className="pl-9"
            />
          </div>
          <Button
            variant="utility"
            className="md:hidden"
            aria-expanded={filtersOpen}
            aria-controls="job-filters"
            onClick={() => setFiltersOpen((open) => !open)}
          >
            <SlidersHorizontal size={16} aria-hidden />
            Filters
            {chips.length > 0 && (
              <span className="tabular-nums">{chips.length}</span>
            )}
          </Button>
        </div>

        <aside
          id="job-filters"
          className={`${filtersOpen ? 'block' : 'hidden'} md:col-start-1 md:row-span-2 md:row-start-1 md:block`}
        >
          <JobFilters
            areas={areas}
            types={types}
            minRateText={minRateText}
            minRateInvalid={minRateInvalid}
            fitsMyTime={fitsMyTime}
            canFitMyTime={canFitMyTime}
            activeCount={chips.length}
            onAreasChange={(next) => update({ areas: next.join(',') })}
            onTypesChange={(next) => update({ types: next.join(',') })}
            onMinRateTextChange={setMinRateText}
            onFitsMyTimeChange={(on) =>
              update({ fitsMyTime: on ? 'true' : undefined })
            }
          />
        </aside>

        <div className="flex flex-col gap-3.5 md:col-start-2 md:row-start-2">
          <div className="flex flex-wrap items-center gap-2">
            {data && !jobs.isError && total > 0 && (
              <span className="text-caption text-text-muted">
                {total} {total === 1 ? 'job' : 'jobs'} · newest first
              </span>
            )}
            {chips.map((chip) => (
              <FilterChip
                key={chip.key}
                label={chip.label}
                onRemove={chip.remove}
              />
            ))}
            {(chips.length > 0 || keyword.trim() !== '') && (
              <Button variant="ghost" size="sm" onClick={clearAll}>
                Clear all
              </Button>
            )}
          </div>

          {jobs.isError && (
            <div className="flex max-w-md flex-col items-start gap-3">
              <Banner tone="danger">We could not load the jobs.</Banner>
              <Button variant="utility" onClick={() => void jobs.refetch()}>
                Try again
              </Button>
            </div>
          )}
          {!data && !jobs.isError && (
            <p className="text-body-sm text-text-muted">Loading jobs…</p>
          )}

          {data && !jobs.isError && data.items.length === 0 && (
            <EmptyState onClear={clearAll} />
          )}

          {data && !jobs.isError && data.items.length > 0 && (
            <>
              {/* 换条件重新请求时，先留着上一次的结果并变淡，不让页面闪成空白 */}
              <ul
                aria-busy={jobs.isFetching}
                className={`grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-3.5 transition-opacity ${jobs.isFetching ? 'opacity-60' : ''}`}
              >
                {data.items.map((job) => (
                  <li key={job.id}>
                    <JobCard
                      to={`/jobs/${job.id}`}
                      title={job.title}
                      employerName={job.employerName}
                      jobType={job.jobType}
                      rate={job.hourlyRate}
                      weeklyHours={job.weeklyHours}
                      area={job.area}
                      postedAt={formatDate(job.createdAt)}
                    />
                  </li>
                ))}
              </ul>
              <Pagination
                page={page}
                totalPages={data.page.totalPages}
                onChange={selectPage}
              />
            </>
          )}
        </div>
      </div>
    </div>
  )
}
