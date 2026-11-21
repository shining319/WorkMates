import { ArrowLeft, ChevronRight, Pencil } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { toast } from 'sonner'
import { isApiError } from '../../../api/types'
import { useAppSelector } from '../../../app/store'
import { formatDate } from '../../../shared/lib/format'
import { Banner } from '../../../shared/ui/Banner'
import { Button, ButtonLink } from '../../../shared/ui/Button'
import { Card } from '../../../shared/ui/Card'
import { ConfirmDialog } from '../../../shared/ui/ConfirmDialog'
import { ItemUnavailable } from '../../../shared/ui/ItemUnavailable'
import { StatusBadge } from '../../../shared/ui/StatusBadge'
import { JobTypeBadge } from '../../jobs/components/JobTypeBadge'
import { PostingFields } from '../../jobs/components/PostingFields'
import { useClosePostingMutation, useGetPostingQuery } from '../api'
import type { PostingDetail } from '../types'

function closeDescription(posting: PostingDetail) {
  const n = posting.inProgressCount
  const keep =
    n > 0
      ? ` You can keep processing the ${n} ${n === 1 ? 'application' : 'applications'} in progress.`
      : ''
  return `${posting.title}. Students can no longer apply.${keep} A closed posting cannot be reopened.`
}

function PostingDetailView({ posting }: { posting: PostingDetail }) {
  const [closePosting, { isLoading: closing }] = useClosePostingMutation()
  const [confirmOpen, setConfirmOpen] = useState(false)
  const isOpen = posting.status === 'OPEN'

  async function onClose() {
    try {
      await closePosting(posting.id).unwrap()
      toast.success('Posting closed')
    } catch (error) {
      // 例如：在另一个标签页已经关过了。显示服务端的话，页面会随缓存刷新成 Closed。
      toast.error(
        isApiError(error)
          ? error.errorMsg
          : 'Something went wrong. Please try again.',
      )
    }
    setConfirmOpen(false)
  }

  return (
    <div>
      <Link
        to="/postings"
        className="mb-4 inline-flex items-center gap-1 text-caption text-text-muted hover:text-text"
      >
        <ArrowLeft size={14} aria-hidden />
        My postings
      </Link>

      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,1fr)_300px]">
        <div className="flex flex-col gap-6">
          <div className="flex flex-col gap-2.5">
            <div className="flex gap-2">
              <JobTypeBadge type={posting.jobType} />
              <StatusBadge
                status={posting.status}
                reason={posting.closeReason}
              />
            </div>
            <h1 className="text-h1 wrap-break-word">{posting.title}</h1>
            <p className="text-base text-text-muted">
              {posting.employerName} · posted {formatDate(posting.createdAt)}
            </p>
          </div>

          {!isOpen && (
            <Banner>
              This posting is closed and can no longer be changed.
            </Banner>
          )}

          <PostingFields posting={posting} />
        </div>

        <Card elevated className="flex flex-col gap-4 p-5 lg:sticky lg:top-6">
          <div>
            <p className="text-title tabular-nums">
              {posting.hiredCount} of {posting.headcount}
            </p>
            <p className="text-caption text-text-muted">
              places filled · {posting.inProgressCount} in progress
            </p>
          </div>
          <div className="flex flex-col gap-2">
            <ButtonLink
              to={`/postings/${posting.id}/applicants`}
              variant="utility"
            >
              Applicants
              <ChevronRight size={16} aria-hidden />
            </ButtonLink>
            {isOpen && (
              <>
                <ButtonLink
                  to={`/postings/${posting.id}/edit`}
                  variant="utility"
                >
                  <Pencil size={16} aria-hidden />
                  Edit posting
                </ButtonLink>
                <Button variant="ghost" onClick={() => setConfirmOpen(true)}>
                  Close posting
                </Button>
              </>
            )}
          </div>
        </Card>
      </div>

      <ConfirmDialog
        open={confirmOpen}
        title="Close this posting?"
        description={closeDescription(posting)}
        confirmLabel="Close posting"
        busy={closing}
        onConfirm={() => void onClose()}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  )
}

// 雇主的职位详情（US 2.4，关闭操作是 US 2.3）。
export function PostingDetailPage() {
  const userId = useAppSelector((state) => state.auth.user?.id)
  const id = Number(useParams().id)
  // id 不是数字时不发请求，直接当作不存在
  const validId = Number.isInteger(id)
  const { data, isLoading, error, refetch } = useGetPostingQuery(id, {
    skip: !validId,
  })

  if (isLoading) {
    return <p className="text-body-sm text-text-muted">Loading the posting…</p>
  }
  // 别人的职位：后端 GET 对所有登录用户开放，但这是"我的职位"的详情，和其他不存在的情况一样处理
  if (
    !validId ||
    (data && data.employerId !== userId) ||
    (!data && isApiError(error) && error.status === 404)
  ) {
    return (
      <ItemUnavailable>
        <ButtonLink to="/postings" variant="utility">
          Back to my postings
        </ButtonLink>
      </ItemUnavailable>
    )
  }
  if (!data) {
    return (
      <div className="flex max-w-md flex-col items-start gap-3">
        <Banner tone="danger">We could not load this posting.</Banner>
        <Button variant="utility" onClick={() => void refetch()}>
          Try again
        </Button>
      </div>
    )
  }
  return <PostingDetailView posting={data} />
}
