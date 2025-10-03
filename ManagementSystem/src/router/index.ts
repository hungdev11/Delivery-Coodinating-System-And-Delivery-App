import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { roleGuard } from '@/common/guards/roleGuard.guard'
import { loginScreenRoutes } from '@/modules/LoginScreen/LoginScreen.routes'
import { routes } from '@/modules/common/routes'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    ...loginScreenRoutes,
    ...routes,
    {
      path: '/:pathMatch(.*)*',
      component: () => import('@/modules/common/NotFoundPage.vue'),
      meta: {
        requiresAuth: false,
        layout: 'blank',
      },
    },
  ] as RouteRecordRaw[],
})

router.beforeEach(roleGuard)

export default router
