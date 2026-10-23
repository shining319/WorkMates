import { z } from 'zod'
import { AREA_VALUES, JOB_TYPE_VALUES } from '../../shared/lib/constants'
import { formatHours } from '../../shared/lib/format'
import { timeSlotsSchema } from '../profile/schemas'
import { totalHours } from '../profile/timeSlots'

export const DESCRIPTION_MAX_LENGTH = 2000

// 时薪：最多 6 位整数、2 位小数。每周工时：最多 3 位整数、1 位小数（和后端 @Digits 一致）。
const RATE_PATTERN = /^\d{1,6}(\.\d{1,2})?$/
const HOURS_PATTERN = /^\d{1,3}(\.\d)?$/
const HEADCOUNT_PATTERN = /^\d{1,9}$/

// 规则与后端 PostingRequest + PostingValidator 一致（US 2.1）。
// 数字都先当字符串收（输入框里就是字符串），提交时再转成数字。
export const postingSchema = z
  .object({
    title: z
      .string()
      .trim()
      .min(1, 'Enter a title.')
      .max(100, 'Use at most 100 characters.'),
    jobType: z.enum(JOB_TYPE_VALUES),
    hourlyRate: z
      .string()
      .trim()
      .refine(
        (value) => RATE_PATTERN.test(value) && Number(value) > 0,
        'Enter a rate above 0 with at most 2 decimals, like 13.50.',
      ),
    weeklyHours: z
      .string()
      .trim()
      .refine(
        (value) =>
          HOURS_PATTERN.test(value) &&
          Number(value) > 0 &&
          Number(value) % 0.5 === 0,
        'Enter hours above 0 in steps of 0.5, like 7.5.',
      ),
    area: z.enum(AREA_VALUES),
    address: z
      .string()
      .trim()
      .min(1, 'Enter an address.')
      .max(200, 'Use at most 200 characters.'),
    description: z
      .string()
      .max(
        DESCRIPTION_MAX_LENGTH,
        `Use at most ${DESCRIPTION_MAX_LENGTH} characters.`,
      ),
    headcount: z
      .string()
      .trim()
      .refine(
        (value) => HEADCOUNT_PATTERN.test(value) && Number(value) >= 1,
        'Enter a whole number of 1 or more.',
      ),
    tags: z.array(z.string()).min(1, 'Add at least one skill or language.'),
    timeSlots: timeSlotsSchema.min(1, 'Add at least one time slot.'),
  })
  // 跨字段的规则：每周工时不能超过时间段的总时长
  .superRefine((values, ctx) => {
    const hours = Number(values.weeklyHours)
    if (!HOURS_PATTERN.test(values.weeklyHours.trim()) || hours <= 0) return
    const total = totalHours(values.timeSlots)
    if (values.timeSlots.length > 0 && hours > total) {
      ctx.addIssue({
        code: 'custom',
        path: ['weeklyHours'],
        message: `Weekly hours cannot be more than the total length of the time slots (${formatHours(total)} h)`,
      })
    }
  })

export type PostingForm = z.infer<typeof postingSchema>
