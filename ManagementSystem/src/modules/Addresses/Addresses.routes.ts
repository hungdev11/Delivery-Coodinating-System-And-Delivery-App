export const addressesRoutes = [
  {
    path: '/addresses/picker',
    component: () => import('./AddressPickerView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Address Picker',
    },
  },
]
