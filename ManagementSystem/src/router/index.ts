import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { roleGuard } from '@/common/guards/roleGuard.guard'
import { loginScreenRoutes } from '@/modules/LoginScreen/LoginScreen.routes'
import { routes } from '@/modules/common/routes'
import { usersRoutes } from '@/modules/Users/Users.routes'
import { deliveryRoutes } from '@/modules/Delivery/Delivery.routes'
import { zonesRoutes } from '@/modules/Zones/Zones.routes'
import { settingsRoutes } from '@/modules/Settings/Settings.routes'
import { addressesRoutes } from '@/modules/Addresses/Addresses.routes'
import { communicationRoutes } from '@/modules/Communication/Communication.routes'
import { parcelsRoutes } from '@/modules/Parcels/Parcels.routes'
import { clientRoutes } from '@/modules/Client/Client.routes'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    ...loginScreenRoutes,
    ...routes,
    ...usersRoutes,
    ...deliveryRoutes,
    ...zonesRoutes,
    ...settingsRoutes,
    ...addressesRoutes,
    ...communicationRoutes,
    ...parcelsRoutes,
    ...clientRoutes,
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
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
