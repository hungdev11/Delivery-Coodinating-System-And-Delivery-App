<script setup lang="ts">
/**
 * Delivery Session Detail View
 *
 * Shows session summary, assignments, and map visualization for a delivery session
 */

import { defineAsyncComponent, ref, computed, onMounted, h, resolveComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import type { TableColumn } from '@nuxt/ui'
// Lazy load MapView to reduce initial bundle size
const MapView = defineAsyncComponent(() => import('@/common/components/MapView.vue'))
const LazyParcelProofModal = defineAsyncComponent(
  () => import('@/modules/Parcels/components/ParcelProofModal.vue'),
)
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
import { getDeliverySessions, getProofsByAssignment, type DeliveryProofDto } from './api'
import { getUserRoles } from '@/common/guards/roleGuard.guard'
import type { FilterGroup } from '@/common/types/filter'
import type { RouteSummary, VisitOrder, Route } from '@/modules/Zones/routing.type'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))
const UButton = resolveComponent('UButton')
const UBadge = resolveComponent('UBadge')

const route = useRoute()
const router = useRouter()
const overlay = useOverlay()
const { loadSessionById, loadSessionRoute } = useDeliverySessions()

const sessionId = computed(() => route.params.sessionId as string)

const session = ref<DeliverySessionDto | null>(null)
const loading = ref(true)
const proofsMap = ref<Map<string, DeliveryProofDto[]>>(new Map())
const routeLoading = ref(true)
const mapRoutes = ref<RouteData[]>([])
const mapMarkers = ref<MapMarker[]>([])
const routeSummary = ref<RouteSummary | null>(null)
const routeData = ref<Route | null>(null)
const demoRouteError = ref<string | null>(null)

// Routing options
const selectedVehicle = ref<'bicycle' | 'car'>('bicycle')
const selectedRoutingType = ref<'full' | 'rating-only' | 'blocking-only' | 'base'>('full')

const routingTypeOptions = [
  { value: 'full', label: 'Đầy đủ' },
  { value: 'rating-only', label: 'Cộng đồng' },
  { value: 'blocking-only', label: 'Đơn giản' },
  { value: 'base', label: 'Cơ bản' },
] as const

// Other sessions for admin view
const otherSessions = ref<DeliverySessionDto[]>([])
const loadingOtherSessions = ref(false)
const showOtherSessions = ref(false)

// Check if current user is admin
const isAdmin = computed(() => {
  const roles = getUserRoles()
  return roles.includes('ADMIN')
})

const loadSession = async () => {
  loading.value = true
  try {
    // Load session with assignments (includes deliveryMan and parcelInfo)
    session.value = await loadSessionById(sessionId.value)
    console.log(session.value);

    // Load proofs for each assignment
    if (session.value?.assignments && session.value.assignments.length > 0) {
      await loadProofsForAssignments(session.value.assignments)
    }

    console.debug('[DeliverySessionDetailView] Session loaded:', {
      hasDeliveryMan: !!session.value?.deliveryMan,
      deliveryMan: session.value?.deliveryMan,
      assignmentsCount: session.value?.assignments?.length,
    })
  } catch (error) {
    console.error('Failed to load session:', error)
  } finally {
    loading.value = false
  }
}

/**
 * Load proofs for all assignments in parallel
 */
const loadProofsForAssignments = async (assignments: DeliveryAssignmentDto[]) => {
  const proofPromises = assignments.map(async (assignment) => {
    try {
      const response = await getProofsByAssignment(assignment.id)
      if (response.result) {
        proofsMap.value.set(assignment.id, response.result)
      }
    } catch (error) {
      console.debug(`Failed to load proofs for assignment ${assignment.id}:`, error)
      proofsMap.value.set(assignment.id, [])
    }
  })

  await Promise.all(proofPromises)
}

const loadRoute = async () => {
  routeLoading.value = true
  demoRouteError.value = null
  mapRoutes.value = []
  mapMarkers.value = []
  routeSummary.value = null

  const result = await loadSessionRoute(sessionId.value, {
    vehicle: selectedVehicle.value,
    routingType: selectedRoutingType.value,
  })
  if (!result) {
    demoRouteError.value = 'Unable to calculate route for this session.'
    routeLoading.value = false
    return
  }

  routeSummary.value = result.summary ?? null
  routeData.value = result.route ?? null

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
  mapMarkers.value = visitOrder.map((visit: VisitOrder, index: number) => {
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
  // Load other sessions if admin (after session is loaded)
  if (isAdmin.value && session.value?.deliveryManId) {
    await loadOtherSessions()
  }
})

// Extended assignment type with enriched data
type EnrichedAssignment = DeliveryAssignmentDto & {
  parcelCode?: string
  deliveryLocation?: string
  value?: number
  deliveryType?: string
  receiverName?: string
  proofs?: Array<{
    id: string
    type: string
    mediaUrl: string
    createdAt: string
  }>
}

const assignments = computed<EnrichedAssignment[]>(() => {
  if (!session.value?.assignments || session.value.assignments.length === 0) {
    return []
  }

  return session.value.assignments.map((a) => {
    // Get proofs from map
    const proofs = proofsMap.value.get(a.id) || []

    // Map assignment with enriched data from parcelInfo (already included in response)
    const mappedAssignment: EnrichedAssignment = {
      id: a.id,
      parcelId: a.parcelId,
      status: a.status as any,
      failReason: a.failReason,
      scanedAt: a.scanedAt || '',
      updatedAt: a.updatedAt || '',
      // Map enriched data from parcelInfo
      parcelCode: (a as any).parcelInfo?.code,
      deliveryLocation: (a as any).parcelInfo?.targetDestination,
      value: (a as any).parcelInfo?.value,
      deliveryType: (a as any).parcelInfo?.deliveryType,
      receiverName: (a as any).parcelInfo?.receiverName,
      // Map proofs from loaded data
      proofs: proofs.map((p) => ({
        id: p.id,
        type: p.type,
        mediaUrl: p.mediaUrl,
        createdAt: p.createdAt,
      })),
    }

    return mappedAssignment
  })
})

const statusColor = computed(() => {
  if (!session.value) return 'neutral'
  if (session.value.status === 'COMPLETED') return 'success'
  if (session.value.status === 'FAILED') return 'error'
  return 'warning'
})

const backToList = () => {
  router.back()
}

/**
 * Load other sessions of the same shipper (admin only)
 */
const loadOtherSessions = async () => {
  if (!session.value?.deliveryManId || !isAdmin.value) return

  loadingOtherSessions.value = true
  try {
    // Query sessions by deliveryManId, excluding current session
    // Note: session.value.deliveryManId is already userId (Keycloak ID), so we can use it directly
    const filterGroup: FilterGroup = {
      logic: 'AND',
      conditions: [
        {
          field: 'deliveryManId',
          operator: 'eq',
          value: session.value.deliveryManId,
          logic: 'AND',
        },
        {
          field: 'id',
          operator: 'ne',
          value: sessionId.value,
          logic: undefined,
        },
      ],
    }

    const response = await getDeliverySessions({
      filters: filterGroup,
      page: 0,
      size: 20, // Load last 20 sessions
      sorts: [
        {
          field: 'startTime',
          direction: 'desc',
        },
      ],
    })

    if (response.result?.data) {
      otherSessions.value = response.result.data
    }
  } catch (error) {
    console.error('Failed to load other sessions:', error)
  } finally {
    loadingOtherSessions.value = false
  }
}

/**
 * Navigate to another session
 */
const navigateToSession = (targetSessionId: string) => {
  router.push({
    name: 'delivery-session-detail',
    params: { sessionId: targetSessionId },
  })
}

/**
 * Format session title: "Phiên <giờ - ngày>"
 */
const getSessionTitle = () => {
  if (!session.value?.startTime) {
    return `Session ${sessionId.value}`
  }
  const startDate = new Date(session.value.startTime)
  const timeStr = startDate.toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
  })
  const dateStr = startDate.toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
  return `Phiên ${timeStr} - ${dateStr}`
}

/**
 * Get session description with shipper info
 */
const getSessionDescription = () => {
  if (session.value?.deliveryMan) {
    const dm = session.value.deliveryMan as any
    return `Shipper: ${dm.name || dm.displayName || 'N/A'}${dm.phone ? ` | ${dm.phone}` : ''}${dm.email ? ` | ${dm.email}` : ''}`
  }
  return 'Detailed view of shipper\'s delivery session'
}

/**
 * Open proof modal for a parcel
 */
const openProofModal = async (parcelId: string, parcelCode?: string) => {
  const modal = overlay.create(LazyParcelProofModal)
  const instance = modal.open({ parcelId, parcelCode })
  await instance.result
}

/**
 * Helper functions to access enriched assignment properties
 */
const getAssignmentProofs = (assignment: DeliveryAssignmentDto) => {
  return (assignment as unknown as EnrichedAssignment).proofs || []
}

const getAssignmentParcelCode = (assignment: DeliveryAssignmentDto) => {
  return (assignment as unknown as EnrichedAssignment).parcelCode
}

const getAssignmentReceiverName = (assignment: DeliveryAssignmentDto) => {
  return (assignment as unknown as EnrichedAssignment).receiverName
}

const getAssignmentDeliveryLocation = (assignment: DeliveryAssignmentDto) => {
  return (assignment as unknown as EnrichedAssignment).deliveryLocation
}

/**
 * Format date for display
 */
const formatDate = (value?: string | null) => {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

/**
 * Table columns configuration for assignments
 */
const assignmentColumns: TableColumn<EnrichedAssignment>[] = [
  {
    accessorKey: 'parcelCode',
    header: 'Mã đơn',
    cell: ({ row }) => {
      const code = getAssignmentParcelCode(row.original) || row.original.parcelId
      return h('span', { class: 'font-mono text-sm' }, code || '—')
    },
  },
  {
    accessorKey: 'receiverName',
    header: 'Người nhận',
    cell: ({ row }) => {
      const name = getAssignmentReceiverName(row.original)
      return h('span', name || '—')
    },
  },
  {
    accessorKey: 'deliveryLocation',
    header: 'Địa chỉ giao',
    cell: ({ row }) => {
      const location = getAssignmentDeliveryLocation(row.original)
      return h(
        'span',
        {
          class: 'max-w-xs truncate block',
          title: location || undefined,
        },
        location || '—',
      )
    },
  },
  {
    accessorKey: 'status',
    header: 'Trạng thái',
    cell: ({ row }) => {
      const status = row.original.status
      const color =
        status === 'COMPLETED' ? 'success' : status === 'FAILED' ? 'error' : 'warning'
      return h(
        UBadge,
        {
          variant: 'soft',
          color,
          class: 'capitalize',
        },
        () => status.toLowerCase(),
      )
    },
  },
  {
    accessorKey: 'scanedAt',
    header: 'Thời gian scan',
    cell: ({ row }) => {
      return h('span', formatDate(row.original.scanedAt))
    },
  },
  {
    accessorKey: 'updatedAt',
    header: 'Thời gian giao',
    cell: ({ row }) => {
      return h('span', formatDate(row.original.updatedAt))
    },
  },
  {
    accessorKey: 'proofs',
    header: 'Bằng chứng',
    cell: ({ row }) => {
      const proofs = getAssignmentProofs(row.original)
      if (proofs.length > 0) {
        return h('div', { class: 'flex items-center gap-1' }, [
          h(
            UBadge,
            {
              variant: 'soft',
              color: 'success',
              size: 'xs',
            },
            () => proofs.length.toString(),
          ),
          h(
            UButton,
            {
              icon: 'i-heroicons-photo',
              size: 'xs',
              variant: 'ghost',
              class: 'cursor-pointer hover:text-primary',
              onClick: () =>
                openProofModal(
                  row.original.parcelId,
                  getAssignmentParcelCode(row.original),
                ),
            },
          ),
        ])
      }
      return h('span', { class: 'text-gray-400' }, '—')
    },
  },
]
</script>

<template>
  <div class="container mx-auto px-4 py-6 space-y-6">
    <PageHeader
      :title="getSessionTitle()"
      :description="getSessionDescription()"
    >
      <template #actions>
        <UButton variant="ghost" icon="i-heroicons-arrow-left" @click="backToList"> Back </UButton>
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
        <!-- Shipper Information Card -->
        <UCard v-if="session?.deliveryMan">
          <template #header>
            <div class="flex items-center justify-between">
              <h3 class="text-lg font-semibold">Thông tin Shipper</h3>
              <UIcon name="i-heroicons-user-circle" class="w-6 h-6 text-gray-400" />
            </div>
          </template>
          <div class="space-y-3">
            <div class="flex items-center gap-3">
              <div class="flex-1">
                <div class="text-sm text-gray-500 mb-1">Tên</div>
                <div class="font-medium text-gray-900 dark:text-gray-100">
                  {{
                    session.deliveryMan.name ||
                    session.deliveryMan.displayName ||
                    'N/A'
                  }}
                </div>
              </div>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <div class="text-sm text-gray-500 mb-1">Số điện thoại</div>
                <div class="font-medium text-gray-900 dark:text-gray-100">
                  {{ (session.deliveryMan as any)?.phone || '—' }}
                </div>
              </div>
              <div>
                <div class="text-sm text-gray-500 mb-1">Email</div>
                <div class="font-medium text-gray-900 dark:text-gray-100">
                  {{ (session.deliveryMan as any)?.email || '—' }}
                </div>
              </div>
            </div>
            <div v-if="(session.deliveryMan as any)?.vehicleType" class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <div class="text-sm text-gray-500 mb-1">Loại xe</div>
                <div class="font-medium text-gray-900 dark:text-gray-100">
                  {{ (session.deliveryMan as any).vehicleType }}
                </div>
              </div>
              <div v-if="(session.deliveryMan as any).capacityKg">
                <div class="text-sm text-gray-500 mb-1">Tải trọng</div>
                <div class="font-medium text-gray-900 dark:text-gray-100">
                  {{ (session.deliveryMan as any).capacityKg }} kg
                </div>
              </div>
            </div>
            <div v-if="session?.deliveryManId" class="text-xs text-gray-400">
              ID: {{ session.deliveryManId }}
            </div>
          </div>
        </UCard>

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
                  {{ session.startTime ? new Date(session.startTime).toLocaleString() : 'Unknown' }}
                </span>
              </div>
              <div>
                <span class="font-medium text-gray-800 dark:text-gray-200">Finished:</span>
                <span class="ml-2">
                  {{ session.endTime ? new Date(session.endTime).toLocaleString() : 'In progress' }}
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
              <div class="text-3xl font-semibold text-gray-900 dark:text-gray-100 capitalize">
                {{
                  routeData ? getCongestionLabel(routeData.trafficSummary?.congestionLevel) : '—'
                }}
              </div>
              <div class="text-xs text-gray-500">
                Avg speed:
                {{
                  routeData?.trafficSummary
                    ? formatSpeed(routeData.trafficSummary.averageSpeed)
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
            <!-- Routing Options -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 pb-4 border-b">
              <div>
                <label class="block text-sm font-medium mb-2">Phương tiện</label>
                <USelect
                  v-model="selectedVehicle"
                  :options="[
                    { value: 'bicycle', label: 'Xe máy' },
                    { value: 'car', label: 'Ô tô' },
                  ]"
                  @update:model-value="loadRoute"
                />
              </div>
              <div>
                <label class="block text-sm font-medium mb-2">Loại routing</label>
                <USelect
                  v-model="selectedRoutingType"
                  :options="routingTypeOptions"
                  @update:model-value="loadRoute"
                />
              </div>
            </div>
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
            :columns="assignmentColumns"
            :ui="{
              empty: 'text-center py-12',
              root: 'h-[50vh]',
              thead: 'sticky top-0 bg-white dark:bg-gray-800',
            }"
          />
        </UCard>

        <!-- Other Sessions (Admin Only) -->
        <UCard v-if="isAdmin && otherSessions.length > 0">
          <template #header>
            <div class="flex items-center justify-between">
              <div>
                <h3 class="text-lg font-semibold">Other Sessions by This Shipper</h3>
                <p class="text-xs text-gray-500 mt-1">
                  View other delivery sessions from the same shipper
                </p>
              </div>
              <UButton
                variant="ghost"
                size="sm"
                :icon="showOtherSessions ? 'i-heroicons-chevron-up' : 'i-heroicons-chevron-down'"
                @click="showOtherSessions = !showOtherSessions"
              >
                {{ showOtherSessions ? 'Hide' : 'Show' }} ({{ otherSessions.length }})
              </UButton>
            </div>
          </template>

          <div v-if="showOtherSessions" class="space-y-2">
            <USkeleton v-if="loadingOtherSessions" class="h-32 w-full" />
            <div v-else class="space-y-2 max-h-96 overflow-y-auto">
              <div
                v-for="otherSession in otherSessions"
                :key="otherSession.id"
                class="p-3 bg-gray-50 dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer transition-colors"
                @click="navigateToSession(otherSession.id)"
              >
                <div class="flex items-center justify-between mb-2">
                  <div class="flex items-center space-x-2">
                    <span class="text-sm font-medium text-gray-900 dark:text-gray-100">
                      Session {{ otherSession.id.substring(0, 8) }}...
                    </span>
                    <UBadge
                      :color="
                        otherSession.status === 'COMPLETED'
                          ? 'success'
                          : otherSession.status === 'FAILED'
                            ? 'error'
                            : 'warning'
                      "
                      variant="soft"
                      size="xs"
                    >
                      {{ otherSession.status }}
                    </UBadge>
                  </div>
                  <UButton
                    variant="ghost"
                    size="xs"
                    icon="i-heroicons-arrow-right"
                    @click.stop="navigateToSession(otherSession.id)"
                  >
                    View
                  </UButton>
                </div>
                <div
                  class="grid grid-cols-2 md:grid-cols-4 gap-2 text-xs text-gray-600 dark:text-gray-400"
                >
                  <div>
                    <span class="font-medium">Tasks:</span>
                    <span class="ml-1">{{ otherSession.totalTasks }}</span>
                  </div>
                  <div>
                    <span class="font-medium">Completed:</span>
                    <span class="ml-1 text-emerald-600 dark:text-emerald-400">
                      {{ otherSession.completedTasks }}
                    </span>
                  </div>
                  <div v-if="otherSession.failedTasks > 0">
                    <span class="font-medium">Failed:</span>
                    <span class="ml-1 text-rose-600 dark:text-rose-400">
                      {{ otherSession.failedTasks }}
                    </span>
                  </div>
                  <div>
                    <span class="font-medium">Started:</span>
                    <span class="ml-1">
                      {{
                        otherSession.startTime
                          ? new Date(otherSession.startTime).toLocaleDateString()
                          : 'N/A'
                      }}
                    </span>
                  </div>
                </div>
                <div v-if="otherSession.endTime" class="text-xs text-gray-500 mt-1">
                  Ended: {{ new Date(otherSession.endTime).toLocaleString() }}
                </div>
              </div>
            </div>
          </div>
        </UCard>
      </template>
    </template>
  </div>
</template>
