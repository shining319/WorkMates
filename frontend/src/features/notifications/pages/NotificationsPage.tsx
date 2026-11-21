import { Bell, CircleAlert, type LucideIcon } from 'lucide-react'
import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router'
import { toast } from 'sonner'
import { useAppSelector } from '../../../app/store'
import { isApiError } from '../../../api/types'
import { formatRelativeTime } from '../../../shared/lib/format'
import { Button } from '../../../shared/ui/Button'
import { Card } from '../../../shared/ui/Card'
import { ItemUnavailable } from '../../../shared/ui/ItemUnavailable'
import { Pagination } from '../../../shared/ui/Pagination'
import { useLazyGetApplicationQuery } from '../../applications/api'
import {
  useGetNotificationsQuery,
  useGetUnreadCountQuery,
  useMarkAllNotificationsReadMutation,
  useMarkNotificationReadMutation,
} from '../api'
import type { AppNotification } from '../types'

/** 空状态和加载失败共用的样子（设计 10b） */
function StateCard({
  icon: Icon,
  title,
  children,
}: {
  icon: LucideIcon
  title: string
  children?: ReactNode
}) {
  return (
    <div className="flex flex-col items-center gap-2 rounded-xl bg-surface px-8 py-12 text-center">
      <span className="mb-1 inline-flex size-11 items-center justify-center rounded-lg border border-border bg-surface">
        <Icon size={20} aria-hidden className="text-text-muted" />
      </span>
      <p className="text-[16px] leading-6 font-semibold text-text-strong">
        {title}
      </p>
      {children}
    </div>
  )
}

/**
 * 一条通知（设计 10）：未读是蓝色实心圆点、加粗、浅蓝底，时间前面写 "New · "，
 * 所以不只靠颜色区分。整行是一个按钮。
 */
function NotificationRow({
  notification,
  disabled,
  onOpen,
}: {
  notification: AppNotification
  disabled: boolean
  onOpen: () => void
}) {
  const { unread } = notification
  return (
    <li>
      <button
        type="button"
        disabled={disabled}
        onClick={onOpen}
        className={`flex w-full gap-3 px-5 py-3.5 text-left hover:bg-black/3 disabled:cursor-wait ${unread ? 'bg-primary/4' : ''}`}
      >
        <span
          aria-hidden
          className={`mt-1.75 size-2 shrink-0 rounded-full ${unread ? 'bg-primary' : 'border border-text-faint'}`}
        />
        <span className="flex min-w-0 flex-1 flex-col">
          <span
            className={`text-body-sm wrap-break-word text-text-strong ${unread ? 'font-medium' : ''}`}
          >
            {notification.message}
          </span>
          <span className="text-caption text-text-muted">
            {unread && 'New · '}
            {formatRelativeTime(notification.createdAt)}
          </span>
        </span>
      </button>
    </li>
  )
}

// 通知中心（设计 10、10b，US 5.2）。页码放在网址里：?page=2。学生和雇主共用。
export function NotificationsPage() {
  const role = useAppSelector((state) => state.auth.user?.role)
  const navigate = useNavigate()
  const location = useLocation()
  const [params, setParams] = useSearchParams()
  // 网址里的页码从 1 开始，接口从 0 开始
  const page = Math.max(1, Math.trunc(Number(params.get('page'))) || 1)

  const list = useGetNotificationsQuery(page - 1)
  const { refetch } = list
  // 和铃铛共用同一份缓存（铃铛每 5 秒轮询一次）：决定 "Mark all as read" 能不能点，
  // 也用来发现新通知
  const unread = useGetUnreadCountQuery().data?.unreadCount
  const unreadCount = unread ?? 0

  // 未读数变大说明来了新通知：重新请求当前页，新通知不用刷新页面就出现在列表里。
  // 变小是标了已读，列表已经随缓存失效刷新过，不用再请求。
  // 第一次拿到未读数（undefined -> n）不算，否则直接打开这一页时列表会多请求一次。
  const lastUnread = useRef(unread)
  useEffect(() => {
    if (
      unread !== undefined &&
      lastUnread.current !== undefined &&
      unread > lastUnread.current
    ) {
      void refetch()
    }
    lastUnread.current = unread
  }, [unread, refetch])
  const [markRead] = useMarkNotificationReadMutation()
  const [markAll, { isLoading: markingAll }] =
    useMarkAllNotificationsReadMutation()
  const [findApplication] = useLazyGetApplicationQuery()
  // 雇主点的申请类通知，查申请时得到 404：在这一页显示 ItemUnavailable。
  // 记下当时的 location.key，而不是一个布尔值：停在这个画面时点页头的铃铛（网址还是 /notifications，
  // 页面不会重新挂载）会产生新的 key，这个画面就自动消失，回到列表。
  const [unavailableAt, setUnavailableAt] = useState<string | null>(null)
  const unavailable = unavailableAt === location.key
  // 雇主的申请类通知要先查一次申请，期间不让重复点
  const [opening, setOpening] = useState(false)

  /**
   * 点一条通知：先标为已读（不等它返回），再按目标类型和角色跳转（backlog F5.2.2、待确认 4）。
   * 目标不存在时，目标页面自己显示 ItemUnavailable；只有雇主的申请要在这里先查，查不到就在本页显示。
   */
  async function open(notification: AppNotification) {
    if (notification.unread) void markRead(notification.id)
    const { targetType, targetId } = notification
    if (role === 'STUDENT') {
      navigate(
        targetType === 'APPLICATION'
          ? `/applications/${targetId}`
          : `/jobs/${targetId}`,
      )
      return
    }
    if (targetType === 'POSTING') {
      navigate(`/postings/${targetId}`)
      return
    }
    setOpening(true)
    try {
      const application = await findApplication(targetId).unwrap()
      navigate(`/postings/${application.postingId}/applicants/${targetId}`)
    } catch (error) {
      if (isApiError(error) && error.status === 404)
        setUnavailableAt(location.key)
      else toast.error('Something went wrong. Please try again.')
    } finally {
      setOpening(false)
    }
  }

  async function onMarkAll() {
    try {
      await markAll().unwrap()
    } catch {
      toast.error('Something went wrong. Please try again.')
    }
  }

  function selectPage(next: number) {
    setParams({ page: String(next) })
  }

  if (unavailable) {
    return (
      <div className="mx-auto max-w-180">
        <ItemUnavailable>
          <Button variant="utility" onClick={() => setUnavailableAt(null)}>
            Back to notifications
          </Button>
        </ItemUnavailable>
      </div>
    )
  }

  const data = list.currentData

  return (
    <div className="mx-auto flex max-w-180 flex-col gap-4">
      <div className="flex items-center gap-3">
        <h1 className="flex-1 text-h2">Notifications</h1>
        {data && data.items.length > 0 && (
          <Button
            variant="utility"
            size="sm"
            disabled={unreadCount === 0 || markingAll}
            onClick={() => void onMarkAll()}
          >
            Mark all as read
          </Button>
        )}
      </div>

      {!data && list.isError && (
        <StateCard
          icon={CircleAlert}
          title="Could not load notifications. Try again."
        >
          <Button
            variant="utility"
            className="mt-2"
            onClick={() => void list.refetch()}
          >
            Try again
          </Button>
        </StateCard>
      )}
      {!data && !list.isError && (
        <p className="text-body-sm text-text-muted">
          Loading your notifications…
        </p>
      )}

      {data && data.items.length === 0 && (
        <StateCard icon={Bell} title="You have no notifications yet">
          <p className="max-w-90 text-body-sm text-text-muted">
            Updates on your applications show up here.
          </p>
        </StateCard>
      )}

      {data && data.items.length > 0 && (
        <>
          <Card className="overflow-hidden">
            <ul className="divide-y divide-border">
              {data.items.map((notification) => (
                <NotificationRow
                  key={notification.id}
                  notification={notification}
                  disabled={opening}
                  onOpen={() => void open(notification)}
                />
              ))}
            </ul>
          </Card>
          <Pagination
            page={page}
            totalPages={data.page.totalPages}
            onChange={selectPage}
          />
        </>
      )}
    </div>
  )
}
