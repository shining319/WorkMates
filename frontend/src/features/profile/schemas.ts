import { z } from 'zod'
import { MAX_TIME_SLOTS, describeSlot, toMinutes } from './timeSlots'
import { DAY_VALUES } from './types'

// ---- 时间段 ----

const slotSchema = z
  .object({
    day: z.enum(DAY_VALUES),
    start: z.string(),
    end: z.string(),
  })
  .refine((slot) => toMinutes(slot.end) > toMinutes(slot.start), {
    path: ['end'],
    message: 'End time must be after the start time.',
  })

// 时间段列表：最多 21 个，同一天不能重叠。
// 重叠时在两行上各放一条提示（说明哪两段冲突），所以用户能看到是哪几行。
export const timeSlotsSchema = z
  .array(slotSchema)
  .max(MAX_TIME_SLOTS, `Add at most ${MAX_TIME_SLOTS} time slots.`)
  .superRefine((slots, ctx) => {
    for (let j = 0; j < slots.length; j++) {
      for (let i = 0; i < j; i++) {
        const a = slots[i]
        const b = slots[j]
        const overlap =
          a.day === b.day &&
          toMinutes(a.start) < toMinutes(b.end) &&
          toMinutes(b.start) < toMinutes(a.end)
        if (overlap) {
          ctx.addIssue({
            code: 'custom',
            path: [j],
            message: `${describeSlot(b)} overlaps ${describeSlot(a)}`,
          })
          ctx.addIssue({
            code: 'custom',
            path: [i],
            message: `${describeSlot(a)} overlaps ${describeSlot(b)}`,
          })
        }
      }
    }
  })

// ---- 学生档案 ----

export const MAX_TAGS = 20

// 时薪：最多 6 位整数、2 位小数，并且大于 0。空字符串表示没填。
const RATE_PATTERN = /^\d{1,6}(\.\d{1,2})?$/

export const studentProfileSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Enter your name.')
    .max(100, 'Use at most 100 characters.'),
  phone: z.string().trim().max(30, 'Use at most 30 characters.'),
  major: z.string().trim().max(100, 'Use at most 100 characters.'),
  yearOfStudy: z.string().trim().max(30, 'Use at most 30 characters.'),
  expectedHourlyRate: z
    .string()
    .trim()
    .refine(
      (value) =>
        value === '' || (RATE_PATTERN.test(value) && Number(value) > 0),
      'Enter a rate above 0 with at most 2 decimals, like 13.50.',
    ),
  tags: z.array(z.string()).max(MAX_TAGS, `Use at most ${MAX_TAGS} tags.`),
  timeSlots: timeSlotsSchema,
})

export type StudentProfileForm = z.infer<typeof studentProfileSchema>

// ---- 雇主档案 ----

export const BIO_MAX_LENGTH = 1000

export const employerProfileSchema = z.object({
  displayName: z
    .string()
    .trim()
    .min(1, 'Enter a display name.')
    .max(100, 'Use at most 100 characters.'),
  bio: z
    .string()
    .max(BIO_MAX_LENGTH, `Use at most ${BIO_MAX_LENGTH} characters.`),
  // 先 trim 再校验，和后端一致
  contactEmail: z
    .string()
    .trim()
    .min(1, 'Enter a contact email.')
    .pipe(
      z
        .email('Enter a valid email address.')
        .max(254, 'Use at most 254 characters.'),
    ),
  phone: z.string().trim().max(30, 'Use at most 30 characters.'),
})

export type EmployerProfileForm = z.infer<typeof employerProfileSchema>
