/**
 * Parcels Module Routes
 */

import type { RouteRecordRaw } from 'vue-router'

export const parcelsRoutes: RouteRecordRaw[] = [
  {
    path: '/parcels',
    name: 'parcels',
    component: () => import('./ParcelsView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Parcels',
      roles: ['ADMIN', 'MANAGER'],
    },
  },
]
