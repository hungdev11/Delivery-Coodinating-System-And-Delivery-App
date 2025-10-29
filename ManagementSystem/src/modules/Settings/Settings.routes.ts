/**
 * Settings Module Routes
 */

import type { RouteRecordRaw } from 'vue-router'

export const settingsRoutes: RouteRecordRaw[] = [
  {
    path: '/settings',
    name: 'settings',
    component: () => import('./SettingsView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Settings',
      roles: ['ADMIN'],
    },
  },
]
