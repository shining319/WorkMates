import { useState } from 'react'
import { toast } from 'sonner'
import type { ApplicationStatus } from '../../../shared/lib/constants'
import { Button } from '../../../shared/ui/Button'
import { ConfirmDialog } from '../../../shared/ui/ConfirmDialog'
import { useChangeApplicationStatusMutation } from '../api'
import { statusChangeError } from '../statusError'

type Answer = 'ACCEPTED' | 'DECLINED' | 'COMPLETED'

// 每个操作的确认框和成功提示
const ACTION: Record<
  Answer,
  {
    title: string
    confirmLabel: string
    confirmVariant: 'primary' | 'danger'
    done: string
    /** 确认框里第二句话 */
    note: string
  }
> = {
  ACCEPTED: {
    title: 'Accept this offer?',
    confirmLabel: 'Accept offer',
    confirmVariant: 'primary',
    done: 'Offer accepted',
    note: 'You cannot change your answer later.',
  },
  DECLINED: {
    title: 'Decline this offer?',
    confirmLabel: 'Decline offer',
    confirmVariant: 'danger',
    done: 'Offer declined',
    note: 'You cannot change your answer later.',
  },
  COMPLETED: {
    title: 'Mark this job as completed?',
    confirmLabel: 'Mark as Completed',
    confirmVariant: 'primary',
    done: 'Marked as completed',
    note: 'A completed application cannot be changed.',
  },
}

/**
 * 学生改自己申请状态的按钮：Offer 时接受或婉拒（US 4.4），Accepted 时标记完成（US 4.7）。
 * 每个操作都先确认。失败时（例如 "Sorry, this position has just been filled"）toast 显示服务端的话；
 * 成功、失败都会让缓存失效，页面刷新成最新状态（招满时变成 "Rejected · Position filled"）。
 * `compact`：我的申请列表每行里的小按钮；否则是申请详情卡片里的普通按钮。
 * 其他状态什么都不显示。
 */
export function StudentActions({
  applicationId,
  postingId,
  status,
  employerName,
  compact = false,
}: {
  applicationId: number
  postingId: number
  status: ApplicationStatus
  employerName: string
  compact?: boolean
}) {
  const [changeStatus, { isLoading }] = useChangeApplicationStatusMutation()
  const [confirming, setConfirming] = useState<Answer | null>(null)
  if (status !== 'OFFER' && status !== 'ACCEPTED') return null

  const size = compact ? 'sm' : 'md'

  async function change(to: Answer) {
    try {
      await changeStatus({ id: applicationId, postingId, status: to }).unwrap()
      toast.success(ACTION[to].done, {
        description: `${employerName} will get a notification.`,
      })
    } catch (error) {
      toast.error(statusChangeError(error))
    }
    setConfirming(null)
  }

  const action = confirming && ACTION[confirming]

  const accept = (
    // 设计 08、09 的 Accept 都是 8px 圆角，不是胶囊
    <Button
      size={size}
      shape="rounded"
      disabled={isLoading}
      onClick={() => setConfirming('ACCEPTED')}
    >
      {compact ? 'Accept' : 'Accept offer'}
    </Button>
  )
  const decline = (
    <Button
      variant="utility"
      size={size}
      disabled={isLoading}
      onClick={() => setConfirming('DECLINED')}
    >
      {compact ? 'Decline' : 'Decline offer'}
    </Button>
  )

  return (
    <>
      {status === 'OFFER' ? (
        // 设计 08 的列表行里 Decline 在左、Accept 在右；设计 09 的卡片里 Accept 在前。
        // 直接按显示顺序写，Tab 的顺序和看到的一致。
        <div className="flex flex-wrap gap-2">
          {compact ? (
            <>
              {decline}
              {accept}
            </>
          ) : (
            <>
              {accept}
              {decline}
            </>
          )}
        </div>
      ) : (
        <Button
          variant={compact ? 'utility' : 'primary'}
          size={size}
          className="self-start"
          disabled={isLoading}
          onClick={() => setConfirming('COMPLETED')}
        >
          Mark as Completed
        </Button>
      )}

      {confirming && action && (
        <ConfirmDialog
          open
          title={action.title}
          description={`${employerName} will get a notification. ${action.note}`}
          confirmLabel={action.confirmLabel}
          confirmVariant={action.confirmVariant}
          busy={isLoading}
          onConfirm={() => void change(confirming)}
          onCancel={() => setConfirming(null)}
        />
      )}
    </>
  )
}
