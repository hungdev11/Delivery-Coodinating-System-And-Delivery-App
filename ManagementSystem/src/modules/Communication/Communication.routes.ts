/**
 * Communication Module Routes
 */

import type { RouteRecordRaw } from 'vue-router'

export const communicationRoutes: RouteRecordRaw[] = [
  {
    path: '/communication',
    name: 'communication-conversations',
    component: () => import('./ConversationsView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Conversations',
      roles: ['ADMIN', 'CLIENT', 'MANAGER', 'SHIPPER'],
    },
    children: [
      {
        path: 'chat/:conversationId',
        name: 'communication-chat',
        component: () => import('./ChatView.vue'),
        meta: {
          requiresAuth: true,
          layout: 'default',
          title: 'Chat',
          roles: ['ADMIN', 'CLIENT', 'MANAGER', 'SHIPPER'],
        },
      },
    ],
  },
  {
    path: '/communication/proposals/configs',
    name: 'communication-proposal-configs',
    component: () => import('./ProposalConfigView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Proposal Configurations',
      roles: ['ADMIN'],
    },
  },
]
