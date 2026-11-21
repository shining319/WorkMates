import { useState } from 'react'
import { Link } from 'react-router'
import { toast } from 'sonner'
import { isApiError } from '../../../api/types'
import { useAppSelector } from '../../../app/store'
import { formatEur, formatHours } from '../../../shared/lib/format'
import { Banner } from '../../../shared/ui/Banner'
import { ConfirmDialog } from '../../../shared/ui/ConfirmDialog'
import { useGetStudentProfileQuery } from '../../profile/api'
import { totalHours } from '../../profile/timeSlots'
import { useApplyToPostingMutation } from '../api'

// 档案里缺的部分，后端用字段名表示（StudentProfile.missing），这里换成人看得懂的说法
const MISSING_LABEL: Record<string, string> = {
  tags: 'skills',
  expectedHourlyRate: 'expected hourly rate',
  timeSlots: 'free time slots',
}

const list = new Intl.ListFormat('en', { type: 'conjunction' })

/** 雇主将看到的档案摘要（设计 06）。 */
function ProfileSummary() {
  const email = useAppSelector((state) => state.auth.user?.email)
  const { data: profile, isError } = useGetStudentProfileQuery()
  if (isError) {
    return (
      <p className="text-body-sm text-text-muted">
        We could not load your profile summary. You can still apply.
      </p>
    )
  }
  if (!profile) {
    return <p className="text-body-sm text-text-muted">Loading your profile…</p>
  }
  const contact = [email, profile.major, profile.yearOfStudy]
    .filter(Boolean)
    .join(' · ')
  const facts = [
    profile.expectedHourlyRate === null
      ? null
      : `Expects ${formatEur(profile.expectedHourlyRate)}/h`,
    `${profile.timeSlots.length} free ${profile.timeSlots.length === 1 ? 'slot' : 'slots'}`,
    `${formatHours(totalHours(profile.timeSlots))} h free per week`,
  ].filter(Boolean)

  return (
    <div className="flex flex-col gap-2.5 rounded-lg bg-surface-muted p-3.5">
      <p className="text-body-sm font-semibold text-text-strong">
        {profile.name}
      </p>
      <p className="text-caption wrap-break-word text-text-muted">{contact}</p>
      <ul className="flex flex-wrap gap-1.5">
        {profile.tags.map((tag) => (
          <li
            key={tag.id}
            className="rounded-xs bg-black/5 px-1.5 py-px text-caption leading-5 text-text"
          >
            {tag.name}
          </li>
        ))}
      </ul>
      <p className="text-caption text-text tabular-nums">{facts.join(' · ')}</p>
    </div>
  )
}

/**
 * 申请确认框（设计 06，US 4.1）。只在要显示的时候才渲染它（页面里写 `{open && <ApplyDialog … />}`），
 * 这样每次打开，上一次的错误提示都已清掉。
 *
 * 失败怎么处理：
 * - PROFILE_INCOMPLETE：框里显示缺什么和去档案页的链接，框不关。
 * - ALREADY_APPLIED、POSTING_CLOSED、职位不存在：弹 toast 并关框。职位详情在申请失败后也会重新请求，
 *   页面随之改成已申请的状态标签、"已关闭"提示，或"不可用"页。
 * - 其他错误（网络等）：弹 toast，框不关，可以重试。
 */
export function ApplyDialog({
  postingId,
  title,
  employerName,
  onClose,
}: {
  postingId: number
  title: string
  employerName: string
  onClose: () => void
}) {
  const [apply, { isLoading }] = useApplyToPostingMutation()
  const { data: profile } = useGetStudentProfileQuery()
  const [incomplete, setIncomplete] = useState<string | null>(null)

  async function onConfirm() {
    try {
      await apply(postingId).unwrap()
      toast.success('Application sent')
      onClose()
    } catch (error) {
      if (!isApiError(error)) {
        toast.error('Something went wrong. Please try again.')
      } else if (error.errorCode === 'PROFILE_INCOMPLETE') {
        // 档案已经加载过（摘要用了它）时，用它的 missing 列表写一句人话；没有就显示服务端的话
        setIncomplete(
          profile && profile.missing.length > 0
            ? `Complete your profile before applying. Add your ${list.format(profile.missing.map((m) => MISSING_LABEL[m] ?? m))}.`
            : error.errorMsg,
        )
      } else if (
        error.errorCode === 'ALREADY_APPLIED' ||
        error.errorCode === 'POSTING_CLOSED' ||
        error.status === 404
      ) {
        toast.error(error.errorMsg)
        onClose()
      } else {
        toast.error(error.errorMsg)
      }
    }
  }

  return (
    <ConfirmDialog
      open
      title={`Apply to ${title}?`}
      description={`${employerName} will see this profile and your contact details.`}
      confirmLabel="Apply"
      confirmVariant="primary"
      busy={isLoading}
      onConfirm={() => void onConfirm()}
      onCancel={onClose}
    >
      <div className="flex flex-col gap-3">
        {incomplete && (
          <Banner
            tone="warning"
            action={
              <Link to="/profile" className="font-medium underline">
                Go to profile
              </Link>
            }
          >
            {incomplete}
          </Banner>
        )}
        <ProfileSummary />
      </div>
    </ConfirmDialog>
  )
}
