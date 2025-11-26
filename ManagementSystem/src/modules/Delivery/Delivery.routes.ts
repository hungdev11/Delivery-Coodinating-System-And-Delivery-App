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
      title: 'Delivery',
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
      title: 'Shippers',
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
      title: 'Session Detail',
      roles: ['ADMIN'],
    },
  },
]
