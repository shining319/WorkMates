import { X } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import {
  APPLICATION_STATUS_LABEL,
  type ApplicationStatus,
} from '../../../shared/lib/constants'
import { Button } from '../../../shared/ui/Button'
import { ConfirmDialog } from '../../../shared/ui/ConfirmDialog'
import { useChangeApplicationStatusMutation } from '../../applications/api'
import { statusChangeError } from '../../applications/statusError'

// 雇主每次只能推进一步（设计 13 的按钮文字）。Offer 之后由学生回复；
// 学生接受后，雇主和学生都能标记完成（US 4.7）。
const NEXT: Partial<
  Record<ApplicationStatus, { to: ApplicationStatus; label: string }>
> = {
  APPLIED: { to: 'REVIEWED', label: 'Mark reviewed' },
  REVIEWED: { to: 'INTERVIEW', label: 'Invite to interview' },
  INTERVIEW: { to: 'OFFER', label: 'Send offer' },
  ACCEPTED: { to: 'COMPLETED', label: 'Mark as Completed' },
}

// 这些操作先确认：拒绝不能撤销，Offer 发出后不能撤回，Completed 之后不能再改
type Confirming = 'REJECTED' | 'OFFER' | 'COMPLETED'

// 只有这三个状态能拒绝；Offer 不能被雇主拒绝或撤回
const CAN_REJECT: ApplicationStatus[] = ['APPLIED', 'REVIEWED', 'INTERVIEW']

/**
 * 雇主改申请状态的按钮（US 4.3、4.7）：推进到下一步、拒绝，或把 Accepted 标记为完成。
 * 拒绝、发 Offer、标记完成前先确认；Mark reviewed、Invite to interview 直接执行。
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
  // 正在确认的目标状态
  const [confirming, setConfirming] = useState<Confirming | null>(null)
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
      toast.error(statusChangeError(error))
    }
    setConfirming(null)
  }

  function onNext() {
    if (!next) return
    if (next.to === 'OFFER' || next.to === 'COMPLETED') setConfirming(next.to)
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
      {confirming === 'COMPLETED' && (
        <ConfirmDialog
          open
          title={`Mark ${studentName}'s job as completed?`}
          description={`${studentName} will get a notification. A completed application cannot be changed.`}
          confirmLabel="Mark as Completed"
          confirmVariant="primary"
          busy={isLoading}
          onConfirm={() => void change('COMPLETED')}
          onCancel={() => setConfirming(null)}
        />
      )}
    </>
  )
}
