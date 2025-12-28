/**
 * Tickets Module Routes
 */

import type { RouteRecordRaw } from 'vue-router'

export const ticketsRoutes: RouteRecordRaw[] = [
  {
    path: '/tickets',
    name: 'tickets',
    component: () => import('./TicketsView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Tickets',
      roles: ['ADMIN', 'MANAGER'],
    },
  },
]
