import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { toast } from 'sonner'
import { isApiError, type ErrorCode } from '../../api/types'

// Server errors that belong to a form field (architecture §9.4).
// Add the posting codes (WEEKLY_HOURS_EXCEED_SLOTS, HEADCOUNT_BELOW_FILLED) with that feature.
const fieldByErrorCode: Partial<Record<ErrorCode, string>> = {
  EMAIL_TAKEN: 'email',
  INVALID_EMAIL_DOMAIN: 'email',
  STUDENT_EMAIL_FOR_EMPLOYER: 'email',
  BAD_CREDENTIALS: 'root.server', // form-level message
  TIME_SLOT_INVALID: 'slots',
}

/**
 * Shows a failed mutation on the form. Pass the error from `unwrap()`.
 * Field errors go to `setError`; anything else becomes a toast.
 */
export function applyServerError<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
) {
  if (!isApiError(error)) {
    toast.error('Something went wrong. Please try again.')
    return
  }
  const field = error.errorCode ? fieldByErrorCode[error.errorCode] : undefined
  if (field) {
    setError(field as Path<T>, { type: 'server', message: error.errorMsg })
  } else {
    toast.error(error.errorMsg)
  }
}
