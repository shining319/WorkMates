import { Bell } from 'lucide-react'
import { Link } from 'react-router'
import { useGetUnreadCountQuery } from '../api'

/**
 * 页头的铃铛（US 5.2，设计系统 IconButton 带数字的样子）：点了进入 /notifications。
 * 每 5 秒查一次未读数；标签页隐藏或窗口失焦时暂停（skipPollingIfUnfocused），
 * 回来时马上查一次（refetchOnFocus，需要 store.ts 里的 setupListeners）。
 * 标为已读后 UnreadCount 标签失效，数字马上更新，不用等下一次轮询。
 * 数字超过 99 显示 "99+"，0 时不显示；读屏软件读 aria-label 里的文字数量。
 * 数字标签按左边定位（left-4.5 = 18px）：个位数时在铃铛右上角，两位数、"99+" 时往右变宽，
 * 不会盖住铃铛（设计系统里是按右边定位、往左变宽）。底色用 danger 红色（用户决定）。
 */
export function NotificationBell() {
  const { data } = useGetUnreadCountQuery(undefined, {
    pollingInterval: 5000,
    skipPollingIfUnfocused: true,
    refetchOnFocus: true,
  })
  const count = data?.unreadCount ?? 0
  const label = count > 0 ? `Notifications, ${count} unread` : 'Notifications'

  return (
    <Link
      to="/notifications"
      aria-label={label}
      title={label}
      className="relative inline-flex size-9 items-center justify-center rounded-md text-text transition hover:bg-black/4 active:scale-90 active:bg-black/8"
    >
      <Bell size={20} aria-hidden />
      {count > 0 && (
        <span
          aria-hidden
          className="absolute top-0.5 left-4.5 h-4 min-w-4 rounded-full bg-danger px-1 text-center text-[10px] leading-4 font-semibold text-on-primary ring-2 ring-surface"
        >
          {count > 99 ? '99+' : count}
        </span>
      )}
    </Link>
  )
}
