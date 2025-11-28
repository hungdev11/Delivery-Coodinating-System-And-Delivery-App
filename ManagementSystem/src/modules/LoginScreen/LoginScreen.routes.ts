export const loginScreenRoutes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('./LoginView.vue'),
    meta: {
      requiresAuth: false,
      layout: 'blank',
    },
  },
]
