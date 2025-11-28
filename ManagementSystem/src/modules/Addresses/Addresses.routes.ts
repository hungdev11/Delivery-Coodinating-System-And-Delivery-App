export const addressesRoutes = [
  {
    path: '/addresses/picker',
    name: 'addresses-picker',
    component: () => import('./AddressPickerView.vue'),
    meta: {
      requiresAuth: true,
      layout: 'default',
      title: 'Address Picker',
    },
  },
]
