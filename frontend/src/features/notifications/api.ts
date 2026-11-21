import { baseApi } from '../../api/baseApi'
import type { Paged } from '../../api/types'
import type { AppNotification, UnreadCount } from './types'

export const notificationsApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    // 本人的通知，最新在前，每页 20 条。page 从 0 开始。
    getNotifications: build.query<Paged<AppNotification>, number>({
      query: (page) => ({ url: '/notifications', params: { page } }),
      providesTags: ['Notifications'],
    }),

    // 铃铛上的未读数，NotificationBell 每 5 秒轮询一次
    getUnreadCount: build.query<UnreadCount, void>({
      query: () => '/notifications/unread-count',
      providesTags: ['UnreadCount'],
    }),

    // 标为已读后列表和未读数都重新请求，铃铛上的数字马上变
    markNotificationRead: build.mutation<AppNotification, number>({
      query: (id) => ({ url: `/notifications/${id}/read`, method: 'PATCH' }),
      invalidatesTags: ['Notifications', 'UnreadCount'],
    }),
    markAllNotificationsRead: build.mutation<
      { updated: number; unreadCount: number },
      void
    >({
      query: () => ({ url: '/notifications/read-all', method: 'PATCH' }),
      invalidatesTags: ['Notifications', 'UnreadCount'],
    }),
  }),
})

export const {
  useGetNotificationsQuery,
  useGetUnreadCountQuery,
  useMarkNotificationReadMutation,
  useMarkAllNotificationsReadMutation,
} = notificationsApi
