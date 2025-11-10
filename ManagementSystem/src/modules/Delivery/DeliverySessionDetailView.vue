<script setup lang="ts">
/**
 * Delivery Session Detail View
 *
 * Shows session summary, assignments, and map visualization for a delivery session
 */

import {
  defineAsyncComponent,
  ref,
  computed,
  onMounted,
} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MapView from '@/common/components/MapView.vue'
import type { RouteData, MapMarker } from '@/common/types/map.type'
import {
  formatDistance,
  formatDuration,
  formatSpeed,
  parseRouteGeometry,
  getCongestionLabel,
} from '@/modules/Zones/utils/routingHelper'
import { useDeliverySessions } from './composables'
import type { DeliveryAssignmentDto, DeliverySessionDto } from './model.type'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))

const route = useRoute()
const router = useRouter()
const { loadSessionById, loadSessionRoute } = useDeliverySessions()

const sessionId = computed(() => route.params.sessionId as string)

const session = ref<DeliverySessionDto | null>(null)
const loading = ref(true)
const routeLoading = ref(true)
const mapRoutes = ref<RouteData[]>([])
const mapMarkers = ref<MapMarker[]>([])
const routeSummary = ref<any>(null)
const demoRouteError = ref<string | null>(null)

const loadSession = async () => {
  loading.value = true
  session.value = await loadSessionById(sessionId.value)
  loading.value = false
}

const loadRoute = async () => {
  routeLoading.value = true
  demoRouteError.value = null
  mapRoutes.value = []
  mapMarkers.value = []
  routeSummary.value = null

  const result = await loadSessionRoute(sessionId.value)
  if (!result) {
    demoRouteError.value = 'Unable to calculate route for this session.'
    routeLoading.value = false
    return
  }

  routeSummary.value = result.summary

  const routeGeometry = result.route?.geometry
  if (routeGeometry) {
    const coordinates = parseRouteGeometry(routeGeometry)
    if (coordinates.length > 0) {
      mapRoutes.value = [
        {
          coordinates,
          distance: result.route?.distance ?? 0,
          duration: result.route?.duration ?? 0,
          properties: {
            color: '#1d4ed8',
            width: 5,
            opacity: 0.85,
          },
        },
      ]
    }
  }

  const visitOrder = result.visitOrder ?? []
  mapMarkers.value = visitOrder.map((visit: any, index: number) => {
    const waypoint = visit.waypoint ?? visit
    return {
      id: `waypoint-${index}`,
      coordinates: [waypoint.lon, waypoint.lat],
      type: index === 0 ? 'warehouse' : 'delivery',
      label: visit.priorityLabel ?? `Stop ${index + 1}`,
      title: visit.priorityLabel ?? `Stop ${index + 1}`,
      popup: `Parcel: ${waypoint.parcelId ?? 'N/A'}<br/>Priority: ${visit.priority ?? '-'}`,
      color: index === 0 ? '#16a34a' : '#1d4ed8',
    }
  })

  routeLoading.value = false
}

onMounted(async () => {
  await loadSession()
  await loadRoute()
})

const assignments = computed<DeliveryAssignmentDto[]>(() => session.value?.assignments ?? [])

const statusColor = computed(() => {
  if (!session.value) return 'neutral'
  if (session.value.status === 'COMPLETED') return 'success'
  if (session.value.status === 'FAILED') return 'error'
  return 'warning'
})

const backToList = () => {
  router.back()
}
</script>

<template>
  <div class="container mx-auto px-4 py-6 space-y-6">
    <PageHeader
      :title="`Session ${sessionId}`"
      description="Detailed view of shipper's delivery session"
    >
      <template #actions>
        <UButton variant="ghost" icon="i-heroicons-arrow-left" @click="backToList">
          Back
        </UButton>
      </template>
    </PageHeader>

    <USkeleton v-if="loading" class="h-64 w-full" />

    <template v-else>
      <UAlert
        v-if="!session"
        color="error"
        variant="soft"
        title="Session not found"
        description="We couldn't find this delivery session. It may have been removed."
      />

      <template v-else>
        <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
          <UCard>
            <template #header>
              <div class="flex items-center justify-between">
                <span class="text-sm text-gray-500">Status</span>
                <UBadge :color="statusColor" variant="soft" class="capitalize">
                  {{ session.status.toLowerCase() }}
                </UBadge>
              </div>
            </template>
            <div class="space-y-2 text-sm text-gray-600">
              <div>
                <span class="font-medium text-gray-800 dark:text-gray-200">Started:</span>
                <span class="ml-2">
                  {{
                    session.startTime
                      ? new Date(session.startTime).toLocaleString()
                      : 'Unknown'
                  }}
                </span>
              </div>
              <div>
                <span class="font-medium text-gray-800 dark:text-gray-200">Finished:</span>
                <span class="ml-2">
                  {{
                    session.endTime ? new Date(session.endTime).toLocaleString() : 'In progress'
                  }}
                </span>
              </div>
            </div>
          </UCard>

          <UCard>
            <template #header>
              <span class="text-sm text-gray-500">Assignments</span>
            </template>
            <div class="space-y-2">
              <div class="text-3xl font-semibold text-gray-900 dark:text-gray-100">
                {{ session.totalTasks }}
              </div>
              <div class="text-xs text-gray-500">
                Completed:
                <strong class="text-emerald-600">{{ session.completedTasks }}</strong>
                · Failed:
                <strong class="text-rose-600">{{ session.failedTasks }}</strong>
              </div>
            </div>
          </UCard>

          <UCard>
            <template #header>
              <span class="text-sm text-gray-500">Route Distance</span>
            </template>
            <div class="space-y-2">
              <div class="text-3xl font-semibold text-gray-900 dark:text-gray-100">
                {{ routeSummary ? formatDistance(routeSummary.totalDistance) : '—' }}
              </div>
              <div class="text-xs text-gray-500">
                Duration:
                {{ routeSummary ? formatDuration(routeSummary.totalDuration) : '—' }}
              </div>
            </div>
          </UCard>

          <UCard>
            <template #header>
              <span class="text-sm text-gray-500">Current Congestion</span>
            </template>
            <div class="space-y-2">
              <div
                class="text-3xl font-semibold text-gray-900 dark:text-gray-100 capitalize"
              >
                {{
                  routeSummary
                    ? getCongestionLabel(routeSummary.trafficSummary?.congestionLevel)
                    : '—'
                }}
              </div>
              <div class="text-xs text-gray-500">
                Avg speed:
                {{
                  routeSummary?.trafficSummary
                    ? formatSpeed(routeSummary.trafficSummary.averageSpeed)
                    : '—'
                }}
              </div>
            </div>
          </UCard>
        </div>

        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <div>
                <h3 class="text-lg font-semibold">Route Overview</h3>
                <p class="text-xs text-gray-500">
                  Automatically generated from delivery assignments for this session
                </p>
              </div>
              <UButton
                variant="ghost"
                icon="i-heroicons-arrow-path"
                :loading="routeLoading"
                @click="loadRoute"
              >
                Recalculate
              </UButton>
            </div>
          </template>

          <div class="space-y-4">
            <USkeleton v-if="routeLoading" class="h-[480px] w-full" />

            <UAlert
              v-else-if="demoRouteError"
              color="warning"
              variant="soft"
              :title="demoRouteError"
            />

            <MapView
              v-else
              height="480px"
              :routes="mapRoutes"
              :markers="mapMarkers"
              :show-zones="false"
              :show-routing="true"
              :auto-fit="true"
              :fit-padding="60"
            />
          </div>
        </UCard>

        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <h3 class="text-lg font-semibold">Assignments</h3>
              <UBadge variant="soft" color="primary">{{ assignments.length }} tasks</UBadge>
            </div>
          </template>

          <UTable
            :data="assignments"
            :columns="[
              { accessorKey: 'parcelId', header: 'Parcel' },
              { accessorKey: 'status', header: 'Status' },
              { accessorKey: 'scanedAt', header: 'Scanned At' },
              { accessorKey: 'updatedAt', header: 'Updated At' },
              { accessorKey: 'failReason', header: 'Fail Reason' },
            ]"
          >
            <template #cell(status)="{ row }">
              <UBadge
                :color="
                  row.original.status === 'COMPLETED'
                    ? 'success'
                    : row.original.status === 'FAILED'
                      ? 'error'
                      : 'warning'
                "
                variant="soft"
                class="capitalize"
              >
                {{ row.original.status.toLowerCase() }}
              </UBadge>
            </template>
            <template #cell(scanedAt)="{ row }">
              {{
                row.original.scanedAt ? new Date(row.original.scanedAt).toLocaleString() : '—'
              }}
            </template>
            <template #cell(updatedAt)="{ row }">
              {{
                row.original.updatedAt
                  ? new Date(row.original.updatedAt).toLocaleString()
                  : '—'
              }}
            </template>
            <template #cell(failReason)="{ row }">
              {{ row.original.failReason || '—' }}
            </template>
          </UTable>
        </UCard>
      </template>
    </template>
  </div>
</template>
