/** 和后端的 `ErrorCode` 枚举一一对应（架构文档 §5.5）。两边要一起改。 */
export type ErrorCode =
  | 'VALIDATION_FAILED'
  | 'INVALID_EMAIL_DOMAIN'
  | 'STUDENT_EMAIL_FOR_EMPLOYER'
  | 'EMAIL_TAKEN'
  | 'BAD_CREDENTIALS'
  | 'UNAUTHORIZED'
  | 'ACCESS_DENIED'
  | 'NOT_FOUND'
  | 'TIME_SLOT_INVALID'
  | 'WEEKLY_HOURS_EXCEED_SLOTS'
  | 'PROFILE_INCOMPLETE'
  | 'ALREADY_APPLIED'
  | 'POSTING_CLOSED'
  | 'POSTING_NOT_EDITABLE'
  | 'HEADCOUNT_BELOW_FILLED'
  | 'INVALID_STATUS_TRANSITION'
  | 'POSITION_FILLED'
  | 'REVIEW_NOT_ALLOWED'
  | 'ALREADY_REVIEWED'
  | 'INTERNAL_ERROR'

/** 分页信息。`page` 从 0 开始。 */
export interface PageInfo {
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/**
 * 后端每个响应的外层格式（架构文档 §5.4）。
 * `<T>` 是泛型：表示 `data` 的类型由使用的地方决定，比如 `Result<User>` 的 data 就是 User。
 */
export interface Result<T> {
  success: boolean
  data: T
  errorCode: ErrorCode | null
  errorMsg: string | null
  page: PageInfo | null
}

/** 分页接口经 `baseQuery` 去掉 `Result` 外壳后，页面拿到的就是这个形状。 */
export interface Paged<T> {
  items: T[]
  page: PageInfo
}

/**
 * RTK Query 里所有请求失败后，错误都是这个形状。
 * `errorCode` 为 null 表示响应不是来自我们的后端（网络断开、代理出错等）。
 */
export interface ApiError {
  status: number
  errorCode: ErrorCode | null
  errorMsg: string
}

/**
 * 判断一个未知的错误是不是 `ApiError`。
 * 返回类型 `value is ApiError` 叫"类型守卫"：函数返回 true 时，
 * TypeScript 就会把调用处的变量当作 `ApiError` 来检查，之后可以直接读 `.errorCode` 等字段。
 * 例子见 shared/lib/fieldErrorMap.ts。
 */
export function isApiError(value: unknown): value is ApiError {
  return (
    typeof value === 'object' &&
    value !== null &&
    'status' in value &&
    'errorMsg' in value
  )
}
