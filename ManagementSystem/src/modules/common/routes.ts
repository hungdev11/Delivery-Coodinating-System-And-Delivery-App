export const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('./HomeView.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/unauthorized',
    name: 'unauthorized',
    component: () => import('./UnauthorizedView.vue'),
    meta: {
      requiresAuth: false,
      layout: 'blank',
    },
  },
]
