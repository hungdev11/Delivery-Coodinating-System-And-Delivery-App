/**
 * Users Module Routes
 */

import type { RouteRecordRaw } from 'vue-router'

export const usersRoutes: RouteRecordRaw[] = [
  {
    path: '/users',
    name: 'users',
    component: () => import('./UsersView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Users',
      roles: ['ADMIN', 'MANAGER'],
    },
  },
  {
    path: '/users/:id',
    name: 'user-detail',
    component: () => import('./UserDetailView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'User Detail',
      roles: ['ADMIN', 'MANAGER'],
    },
  },
]
