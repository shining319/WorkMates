// 和后端枚举 JobType、Area 一一对应（posting.entity）。两边要一起改。

export const JOB_TYPE_VALUES = ['PART_TIME', 'FULL_TIME', 'INTERNSHIP'] as const
export type JobType = (typeof JOB_TYPE_VALUES)[number]

export const JOB_TYPE_LABEL: Record<JobType, string> = {
  PART_TIME: 'Part-time',
  FULL_TIME: 'Full-time',
  INTERNSHIP: 'Internship',
}

export const AREA_VALUES = ['ATHLONE', 'LIMERICK', 'REMOTE'] as const
export type Area = (typeof AREA_VALUES)[number]

export const AREA_LABEL: Record<Area, string> = {
  ATHLONE: 'Athlone',
  LIMERICK: 'Limerick',
  REMOTE: 'Remote',
}

// 下拉框、分段选择用的 { value, label } 列表
export const JOB_TYPE_OPTIONS = JOB_TYPE_VALUES.map((value) => ({
  value,
  label: JOB_TYPE_LABEL[value],
}))
export const AREA_OPTIONS = AREA_VALUES.map((value) => ({
  value,
  label: AREA_LABEL[value],
}))
