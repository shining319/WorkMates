import { isApiError } from '../../api/types'
import {
  APPLICATION_STATUS_LABEL,
  APPLICATION_STATUS_VALUES,
  type ApplicationStatus,
} from '../../shared/lib/constants'

/**
 * 改申请状态失败时 toast 里显示的话：用服务端的消息，把末尾的状态枚举换成文字。
 * "This status change is not allowed. Current status: OFFER" -> "... Current status: Offer"。
 * `POSITION_FILLED` 的消息 "Sorry, this position has just been filled" 原样显示。
 */
export function statusChangeError(error: unknown): string {
  if (!isApiError(error)) return 'Something went wrong. Please try again.'
  return error.errorMsg.replace(/[A-Z]+$/, (word) =>
    (APPLICATION_STATUS_VALUES as readonly string[]).includes(word)
      ? APPLICATION_STATUS_LABEL[word as ApplicationStatus]
      : word,
  )
}
