export const routes = [
  {
    path: '/',
    component: () => import('./HomeView.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/unauthorized',
    component: () => import('./UnauthorizedView.vue'),
    meta: {
      requiresAuth: false,
      layout: 'blank',
    },
  },
]
