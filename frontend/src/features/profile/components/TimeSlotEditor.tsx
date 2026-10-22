import { CircleAlert, Plus, X } from 'lucide-react'
import { useFieldArray, useFormContext, useWatch } from 'react-hook-form'
import { Button } from '../../../shared/ui/Button'
import { Select } from '../../../shared/ui/Select'
import {
  DAYS,
  END_OPTIONS,
  MAX_TIME_SLOTS,
  START_OPTIONS,
  totalHours,
} from '../timeSlots'
import type { TimeSlot } from '../types'

// 这个组件要求外层表单里有一个 timeSlots 字段（时间段数组）。
interface TimeSlotsForm {
  timeSlots: TimeSlot[]
}

function ErrorLine({ message }: { message: string }) {
  return (
    <p
      role="alert"
      className="flex items-center gap-1.5 text-caption text-danger"
    >
      <CircleAlert size={14} aria-hidden className="shrink-0" />
      {message}
    </p>
  )
}

/**
 * 每周空闲时间段的编辑器：每行一个时间段（星期、开始、结束），可删除，底下有添加按钮。
 * 全部用下拉框，键盘和触屏都能用，不需要拖拽。
 *
 * 用 `useFormContext` 拿到外层的 react-hook-form，所以外层要用 <FormProvider> 包起来。
 * `useFieldArray` 管理"一组可增删的行"。
 * 重叠、结束早于开始这些错误由 schemas.ts 的 Zod 规则产生，这里只负责显示在对应的行下面。
 * 服务端返回的 TIME_SLOT_INVALID 放在 `timeSlots.root`，显示在整个列表的底部。
 */
export function TimeSlotEditor() {
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext<TimeSlotsForm>()
  const { fields, append, remove } = useFieldArray({
    control,
    name: 'timeSlots',
  })
  const slots = useWatch({ control, name: 'timeSlots' })

  const rowErrors = errors.timeSlots
  const listError = rowErrors?.root?.message ?? rowErrors?.message
  const atLimit = fields.length >= MAX_TIME_SLOTS

  function addSlot() {
    // 新一行默认放在还没有时间段的第一天，减少一添加就重叠的情况
    const usedDays = new Set(slots.map((slot) => slot.day))
    const day = DAYS.find((d) => !usedDays.has(d.value))?.value ?? 'MONDAY'
    append({ day, start: '09:00', end: '12:00' })
  }

  return (
    <div className="flex flex-col gap-3.5">
      <div className="flex items-baseline justify-between gap-3">
        <h2 className="text-title">Free time slots</h2>
        <span className="text-caption text-text-muted tabular-nums">
          {fields.length} of {MAX_TIME_SLOTS} · {totalHours(slots)} h per week
        </span>
      </div>

      {fields.length === 0 && (
        <p className="text-body-sm text-text-muted">
          No free time slots yet. Add the times you can work each week.
        </p>
      )}

      <ul className="flex flex-col gap-3">
        {fields.map((field, index) => {
          const rowError = rowErrors?.[index]
          const endError = rowError?.end?.message
          const message = rowError?.message ?? endError
          const invalid = Boolean(message)
          const label = `slot ${index + 1}`
          return (
            <li key={field.id} className="flex flex-col gap-1.5">
              {/* 手机上分两行：第一行"星期 + 删除"，第二行"开始 to 结束"；宽屏（sm 以上）排成一行 */}
              <div className="grid grid-cols-[1fr_auto] items-center gap-2 sm:flex">
                <div className="sm:w-24">
                  <Select
                    aria-label={`Day, ${label}`}
                    aria-invalid={invalid}
                    options={DAYS}
                    {...register(`timeSlots.${index}.day`)}
                  />
                </div>
                <div className="col-span-2 row-start-2 flex items-center gap-2 sm:col-auto sm:row-auto">
                  <div className="flex-1 sm:w-28 sm:flex-none">
                    <Select
                      aria-label={`Start time, ${label}`}
                      aria-invalid={invalid}
                      options={START_OPTIONS}
                      {...register(`timeSlots.${index}.start`)}
                    />
                  </div>
                  <span className="text-caption text-text-muted">to</span>
                  <div className="flex-1 sm:w-28 sm:flex-none">
                    <Select
                      aria-label={`End time, ${label}`}
                      aria-invalid={invalid}
                      options={END_OPTIONS}
                      {...register(`timeSlots.${index}.end`)}
                    />
                  </div>
                </div>
                <Button
                  variant="ghost"
                  aria-label={`Remove ${label}`}
                  className="col-start-2 row-start-1 px-2.5! sm:col-auto sm:row-auto"
                  onClick={() => remove(index)}
                >
                  <X size={16} aria-hidden />
                </Button>
              </div>
              {message && <ErrorLine message={message} />}
            </li>
          )
        })}
      </ul>

      {listError && <ErrorLine message={listError} />}

      <div>
        <Button variant="utility" disabled={atLimit} onClick={addSlot}>
          <Plus size={16} aria-hidden />
          Add slot
        </Button>
      </div>
    </div>
  )
}
