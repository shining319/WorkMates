/** 通知类型（PRD E005），和后端 `NotificationType` 一致。P1 的类型以后随各自的故事加。 */
export type NotificationType =
  | 'APPLICATION_RECEIVED'
  | 'STATUS_CHANGED'
  | 'OFFER_ACCEPTED'
  | 'OFFER_DECLINED'
  | 'APPLICATION_COMPLETED'
  | 'POSITION_FILLED'
  | 'POSTING_FILLED'

/** 通知指向什么：`targetId` 是申请 ID 还是职位 ID */
export type NotificationTargetType = 'APPLICATION' | 'POSTING'

/**
 * `GET /api/notifications` 的一条。`message` 是后端拼好的英文句子，直接显示。
 * 叫 AppNotification 是为了不和浏览器自带的 `Notification` 重名。
 */
export interface AppNotification {
  id: number
  type: NotificationType
  message: string
  targetType: NotificationTargetType
  targetId: number
  unread: boolean
  createdAt: string
  /** 未读时是 null */
  readAt: string | null
}

/** `GET /api/notifications/unread-count` */
export interface UnreadCount {
  unreadCount: number
}
