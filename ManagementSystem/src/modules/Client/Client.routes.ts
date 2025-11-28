import type { RouteRecordRaw } from 'vue-router'

export const clientRoutes: RouteRecordRaw[] = [
  {
    path: '/client',
    name: 'client',
    meta: {
      requiresAuth: true,
      requiredRoles: ['CLIENT'],
      layout: 'default',
    },
    children: [
      {
        path: '',
        name: 'client-index',
        redirect: '/client/parcels',
      },
      {
        path: 'parcels',
        name: 'client-parcels',
        component: () => import('./MyParcelsView.vue'),
        meta: {
          requiresAuth: true,
          requiredRoles: ['CLIENT'],
        },
      },
      {
        path: 'parcels/create',
        name: 'client-create-parcel',
        component: () => import('./CreateParcelView.vue'),
        meta: {
          requiresAuth: true,
          requiredRoles: ['CLIENT'],
        },
      },
      {
        path: 'addresses',
        name: 'client-addresses',
        component: () => import('./MyAddressesView.vue'),
        meta: {
          requiresAuth: true,
          requiredRoles: ['CLIENT'],
        },
      },
      {
        path: 'addresses/create',
        name: 'client-create-address',
        component: () => import('./CreateAddressView.vue'),
        meta: {
          requiresAuth: true,
          requiredRoles: ['CLIENT'],
        },
      },
      {
        path: 'profile',
        name: 'client-profile',
        component: () => import('./ProfileView.vue'),
        meta: {
          requiresAuth: true,
          requiredRoles: ['CLIENT'],
        },
      },
    ],
  },
]
