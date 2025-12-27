/**
 * Delivery Module Routes
 */

import type { RouteRecordRaw } from 'vue-router'

export const deliveryRoutes: RouteRecordRaw[] = [
  {
    path: '/delivery',
    name: 'delivery',
    redirect: '/delivery/shippers',
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Giao hàng',
      roles: ['ADMIN'],
    },
  },
  {
    path: '/delivery/shippers',
    name: 'delivery-shippers',
    component: () => import('./DeliveryShippersView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Shipper',
      roles: ['ADMIN'],
    },
  },
  {
    path: '/delivery/sessions/:sessionId',
    name: 'delivery-session-detail',
    component: () => import('./DeliverySessionDetailView.vue'),
    props: true,
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Chi tiết phiên',
      roles: ['ADMIN'],
    },
  },
  {
    path: '/delivery/tasks',
    name: 'delivery-tasks',
    component: () => import('./components/TaskManagementView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Quản lý nhiệm vụ',
      roles: ['ADMIN'],
    },
  },
  {
    path: '/delivery/shifts/calendar',
    name: 'delivery-shift-calendar',
    component: () => import('./components/ShipperShiftCalendarView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Lịch ca làm việc',
      roles: ['ADMIN'],
    },
  },
  {
    path: '/delivery/sessions',
    name: 'delivery-sessions',
    component: () => import('./components/AllSessionsView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Tất cả phiên',
      roles: ['ADMIN'],
    },
  },
]
