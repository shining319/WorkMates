import { JOB_TYPE_LABEL, type JobType } from '../../../shared/lib/constants'

// 完整的类名要写出来（不能拼接），Tailwind 才会生成对应的样式。
const colorClass: Record<JobType, string> = {
  PART_TIME: 'bg-jobtype-part-time-bg text-jobtype-part-time',
  FULL_TIME: 'bg-jobtype-full-time-bg text-jobtype-full-time',
  INTERNSHIP: 'bg-jobtype-internship-bg text-jobtype-internship',
}

/** 工作类型徽标：Part-time / Full-time / Internship。 */
export function JobTypeBadge({ type }: { type: JobType }) {
  return (
    <span
      className={`inline-flex h-5.5 items-center rounded-full px-2.25 text-eyebrow font-semibold ${colorClass[type]}`}
    >
      {JOB_TYPE_LABEL[type]}
    </span>
  )
}
