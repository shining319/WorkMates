import { SlidersHorizontal } from 'lucide-react'
import {
  AREA_OPTIONS,
  JOB_TYPE_OPTIONS,
  type Area,
  type JobType,
} from '../../../shared/lib/constants'
import { Card } from '../../../shared/ui/Card'
import { Checkbox } from '../../../shared/ui/Checkbox'
import { Input } from '../../../shared/ui/Input'
import { Switch } from '../../../shared/ui/Switch'

interface JobFiltersProps {
  areas: Area[]
  types: JobType[]
  /** 最低时薪输入框里的文字（没填完或写错时也要原样显示） */
  minRateText: string
  minRateInvalid: boolean
  fitsMyTime: boolean
  /** 学生没有空闲时间段时为 false：开关禁用并说明原因 */
  canFitMyTime: boolean
  /** 生效的条件数量，显示在标题旁 */
  activeCount: number
  onAreasChange: (areas: Area[]) => void
  onTypesChange: (types: JobType[]) => void
  onMinRateTextChange: (text: string) => void
  onFitsMyTimeChange: (value: boolean) => void
}

/** 勾上就加进列表，取消就移出去 */
function toggle<T>(list: T[], value: T, on: boolean): T[] {
  return on ? [...list, value] : list.filter((item) => item !== value)
}

const sectionClass = 'flex flex-col gap-2.5 border-b border-border p-4'
const eyebrowClass = 'text-eyebrow tracking-[0.4px] text-text-muted uppercase'

// 职位列表的筛选栏（设计 04 的左栏）。只负责显示和把改动交出去，条件存在网址里，由页面管理。
export function JobFilters({
  areas,
  types,
  minRateText,
  minRateInvalid,
  fitsMyTime,
  canFitMyTime,
  activeCount,
  onAreasChange,
  onTypesChange,
  onMinRateTextChange,
  onFitsMyTimeChange,
}: JobFiltersProps) {
  return (
    <Card className="overflow-hidden">
      <div className="flex items-center gap-2 border-b border-border px-4 py-3 text-text-strong">
        <SlidersHorizontal size={16} aria-hidden />
        <h2 className="flex-1 text-[15px] leading-5.5 font-semibold">
          Filters
        </h2>
        {activeCount > 0 && (
          <span className="rounded-full bg-text-strong px-2 py-0.5 text-eyebrow text-on-primary tabular-nums">
            {activeCount}
          </span>
        )}
      </div>

      <fieldset className={sectionClass}>
        <legend className="sr-only">Location area</legend>
        <span aria-hidden className={eyebrowClass}>
          Location area
        </span>
        {AREA_OPTIONS.map(({ value, label }) => (
          <Checkbox
            key={value}
            label={label}
            checked={areas.includes(value)}
            onChange={(on) => onAreasChange(toggle(areas, value, on))}
          />
        ))}
      </fieldset>

      <fieldset className={sectionClass}>
        <legend className="sr-only">Job type</legend>
        <span aria-hidden className={eyebrowClass}>
          Job type
        </span>
        {JOB_TYPE_OPTIONS.map(({ value, label }) => (
          <Checkbox
            key={value}
            label={label}
            checked={types.includes(value)}
            onChange={(on) => onTypesChange(toggle(types, value, on))}
          />
        ))}
      </fieldset>

      <div className="border-b border-border p-4">
        <label
          htmlFor="job-min-rate"
          className="mb-1.5 block text-caption text-text-strong"
        >
          Minimum hourly rate
        </label>
        <div className="flex items-center gap-2">
          <Input
            id="job-min-rate"
            inputMode="decimal"
            placeholder="0.00"
            value={minRateText}
            onChange={(event) => onMinRateTextChange(event.target.value)}
            aria-invalid={minRateInvalid}
            aria-describedby={minRateInvalid ? 'job-min-rate-error' : undefined}
          />
          <span className="text-caption whitespace-nowrap text-text-muted">
            €/h
          </span>
        </div>
        {minRateInvalid && (
          <p
            id="job-min-rate-error"
            className="mt-1.5 text-caption text-danger-text"
          >
            Enter an amount like 12 or 12.50.
          </p>
        )}
      </div>

      <div className="p-4">
        <Switch
          label="Fits my time"
          description={
            canFitMyTime
              ? 'Covers 50% or more of the shifts'
              : 'Add your free time slots to use this filter'
          }
          checked={canFitMyTime && fitsMyTime}
          disabled={!canFitMyTime}
          onChange={onFitsMyTimeChange}
        />
      </div>
    </Card>
  )
}
