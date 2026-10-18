import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { toast } from 'sonner'
import { isApiError, type ErrorCode } from '../../api/types'

// 哪些后端错误码属于某个表单字段（架构文档 §9.4）：错误码 -> 字段名。
// 这样 EMAIL_TAKEN 之类的错误会直接显示在邮箱输入框下面。
// 做岗位功能时，再把 WEEKLY_HOURS_EXCEED_SLOTS、HEADCOUNT_BELOW_FILLED 加进来。
//
// Partial<Record<ErrorCode, string>> 的意思：键只能是 ErrorCode 里的值，值是字符串，
// 并且不用把所有错误码都列出来（Partial 表示"可以缺项"）。
const fieldByErrorCode: Partial<Record<ErrorCode, string>> = {
  EMAIL_TAKEN: 'email',
  INVALID_EMAIL_DOMAIN: 'email',
  STUDENT_EMAIL_FOR_EMPLOYER: 'email',
  BAD_CREDENTIALS: 'root.server', // 不属于某个字段，是整个表单的错误
  TIME_SLOT_INVALID: 'slots',
}

/**
 * 把提交表单时失败的请求显示出来。参数 error 传 mutation 的 `unwrap()` 抛出的错误。
 * - 属于某个字段的错误：用 `setError` 显示在那个字段下面。
 * - 其他错误：用 toast 弹出提示。
 *
 * `<T extends FieldValues>` 是泛型：T 代表调用它的那个表单的字段类型
 * （如 `{ email: string; password: string }`），所以这个函数能给任何表单用。
 */
export function applyServerError<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
) {
  // 不是我们后端返回的错误（比如代码里的 bug），只能给个通用提示。
  if (!isApiError(error)) {
    toast.error('Something went wrong. Please try again.')
    return
  }
  const field = error.errorCode ? fieldByErrorCode[error.errorCode] : undefined
  if (field) {
    // 上面的字段名是普通字符串，而 setError 要求"这个表单真有的字段名"（Path<T>），
    // 所以用 as 告诉 TypeScript："我保证这个名字是对的"。
    setError(field as Path<T>, { type: 'server', message: error.errorMsg })
  } else {
    toast.error(error.errorMsg)
  }
}
