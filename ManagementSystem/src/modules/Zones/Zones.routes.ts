/**
 * Zones Module Routes
 */

import type { RouteRecordRaw } from 'vue-router'

export const zonesRoutes: RouteRecordRaw[] = [
  {
    path: '/zones',
    name: 'zones',
    component: () => import('./ZonesView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Zones',
      roles: ['ADMIN', 'MANAGER'],
    },
  },
  {
    path: '/zones/map',
    name: 'zones-map',
    component: () => import('./ZonesMapView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Zone Map',
      roles: ['ADMIN', 'MANAGER'],
    },
  },
  {
    path: '/zones/map/demo-routing',
    name: 'zones-demo-routing',
    component: () => import('./DemoRoutingView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Demo Routing',
      roles: ['ADMIN', 'MANAGER'],
    },
  },
  {
    path: '/zones/:id',
    name: 'zone-detail',
    component: () => import('./ZoneDetailView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Zone Detail',
      roles: ['ADMIN', 'MANAGER'],
    },
  },
]
