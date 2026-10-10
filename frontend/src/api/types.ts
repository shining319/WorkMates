/** Mirrors the backend `ErrorCode` enum (architecture §5.5). Keep both in step. */
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

/** `page` is 0-based. */
export interface PageInfo {
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** The body of every backend response (architecture §5.4). */
export interface Result<T> {
  success: boolean
  data: T
  errorCode: ErrorCode | null
  errorMsg: string | null
  page: PageInfo | null
}

/** What a paged endpoint returns after `baseQuery` has unwrapped the `Result`. */
export interface Paged<T> {
  items: T[]
  page: PageInfo
}

/**
 * The error shape of every RTK Query call. `errorCode` is null when the
 * response did not come from our backend (network failure, proxy error).
 */
export interface ApiError {
  status: number
  errorCode: ErrorCode | null
  errorMsg: string
}

export function isApiError(value: unknown): value is ApiError {
  return (
    typeof value === 'object' &&
    value !== null &&
    'status' in value &&
    'errorMsg' in value
  )
}
