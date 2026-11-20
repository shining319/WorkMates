import { X } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { isApiError } from '../../../api/types'
import {
  APPLICATION_STATUS_LABEL,
  APPLICATION_STATUS_VALUES,
  type ApplicationStatus,
} from '../../../shared/lib/constants'
import { Button } from '../../../shared/ui/Button'
import { ConfirmDialog } from '../../../shared/ui/ConfirmDialog'
import { useChangeApplicationStatusMutation } from '../../applications/api'

// 雇主每次只能推进一步（设计 13 的按钮文字）。Offer 及以后雇主不能再改（US 4.7 的 Completed 以后再加）。
const NEXT: Partial<
  Record<ApplicationStatus, { to: ApplicationStatus; label: string }>
> = {
  APPLIED: { to: 'REVIEWED', label: 'Mark reviewed' },
  REVIEWED: { to: 'INTERVIEW', label: 'Invite to interview' },
  INTERVIEW: { to: 'OFFER', label: 'Send offer' },
}

// 只有这三个状态能拒绝；Offer 不能被雇主拒绝或撤回
const CAN_REJECT: ApplicationStatus[] = ['APPLIED', 'REVIEWED', 'INTERVIEW']

/**
 * 服务端的话，把末尾的状态枚举换成文字：
 * "This status change is not allowed. Current status: OFFER" -> "... Current status: Offer"
 */
function errorMessage(error: unknown) {
  if (!isApiError(error)) return 'Something went wrong. Please try again.'
  return error.errorMsg.replace(/[A-Z]+$/, (word) =>
    (APPLICATION_STATUS_VALUES as readonly string[]).includes(word)
      ? APPLICATION_STATUS_LABEL[word as ApplicationStatus]
      : word,
  )
}

/**
 * 雇主改申请状态的按钮（US 4.3）：推进到下一步，或拒绝。
 * 拒绝和发 Offer 前先确认（Offer 发出后不能撤回）；Mark reviewed、Invite to interview 直接执行。
 * `compact`：申请人列表的每一行，小按钮加 X 图标；否则是申请人详情侧栏里的整宽按钮。
 * 当前状态没有可做的操作时什么都不显示。
 */
export function StatusActions({
  applicationId,
  postingId,
  studentName,
  status,
  compact = false,
}: {
  applicationId: number
  postingId: number
  studentName: string
  status: ApplicationStatus
  compact?: boolean
}) {
  const [changeStatus, { isLoading }] = useChangeApplicationStatusMutation()
  // 正在确认的目标状态：拒绝或发 Offer
  const [confirming, setConfirming] = useState<'REJECTED' | 'OFFER' | null>(
    null,
  )
  const next = NEXT[status]
  const canReject = CAN_REJECT.includes(status)
  if (!next && !canReject) return null

  async function change(to: ApplicationStatus) {
    try {
      await changeStatus({ id: applicationId, postingId, status: to }).unwrap()
      toast.success(`Status changed to ${APPLICATION_STATUS_LABEL[to]}`, {
        description: `${studentName} will get a notification.`,
      })
    } catch (error) {
      // 例如另一个标签页已经改过了。缓存已失效，页面会刷新成当前状态。
      toast.error(errorMessage(error))
    }
    setConfirming(null)
  }

  function onNext() {
    if (!next) return
    if (next.to === 'OFFER') setConfirming('OFFER')
    else void change(next.to)
  }

  return (
    <>
      <div
        className={
          compact ? 'flex items-center gap-1.5' : 'flex flex-col gap-2'
        }
      >
        {next && (
          <Button
            variant={compact ? 'utility' : 'primary'}
            size={compact ? 'sm' : 'md'}
            className={compact ? '' : 'w-full'}
            disabled={isLoading}
            onClick={onNext}
          >
            {next.label}
          </Button>
        )}
        {canReject &&
          (compact ? (
            <Button
              variant="ghost"
              size="sm"
              aria-label={`Reject ${studentName}`}
              title="Reject"
              disabled={isLoading}
              onClick={() => setConfirming('REJECTED')}
            >
              <X size={14} aria-hidden />
            </Button>
          ) : (
            <Button
              variant="ghost"
              className="w-full"
              disabled={isLoading}
              onClick={() => setConfirming('REJECTED')}
            >
              Reject
            </Button>
          ))}
      </div>

      {confirming === 'REJECTED' && (
        <ConfirmDialog
          open
          title={`Reject ${studentName}?`}
          description={`${studentName} will get a notification. A rejected application cannot be changed again.`}
          confirmLabel="Reject"
          busy={isLoading}
          onConfirm={() => void change('REJECTED')}
          onCancel={() => setConfirming(null)}
        />
      )}
      {confirming === 'OFFER' && (
        <ConfirmDialog
          open
          title={`Send an offer to ${studentName}?`}
          description={`You cannot withdraw an offer once it is sent. ${studentName} can accept or decline it.`}
          confirmLabel="Send offer"
          confirmVariant="primary"
          busy={isLoading}
          onConfirm={() => void change('OFFER')}
          onCancel={() => setConfirming(null)}
        />
      )}
    </>
  )
}
