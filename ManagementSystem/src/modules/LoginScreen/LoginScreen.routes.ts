export const loginScreenRoutes = [
  {
    path: '/login',
    component: () => import('./LoginView.vue'),
    meta: {
      requiresAuth: false,
      layout: 'blank',
    },
  },
]
