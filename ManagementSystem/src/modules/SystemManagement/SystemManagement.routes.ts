/**
 * System Management Module Routes
 */

import type { RouteRecordRaw } from 'vue-router'

export const systemManagementRoutes: RouteRecordRaw[] = [
  {
    path: '/system',
    name: 'system-management',
    component: () => import('./SystemManagementView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'System Management',
      roles: ['ADMIN'],
    },
  },
]
