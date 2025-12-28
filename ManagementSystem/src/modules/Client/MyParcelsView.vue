<script setup lang="ts">
/**
 * My Parcels View
 * Client view for managing their own parcels (as receiver)
 */

import { ref, onMounted, computed, h, resolveComponent, watch, defineAsyncComponent } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import {
  getClientReceivedParcels,
  confirmParcelReceived,
  reportParcelNotReceived,
  retractDispute,
  getLatestAssignmentForParcel,
  getParcelById,
  type LatestAssignmentResponse,
} from '@/modules/Parcels/api'
import { getSessionDemoRoute } from '@/modules/Delivery/api'
import type { DemoRouteResponse } from '@/modules/Zones/routing.type'
import { parseRouteGeometry } from '@/modules/Zones/utils/routingHelper'
import { ParcelDto, type ParcelStatus } from '@/modules/Parcels/model.type'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'
import type { TableColumn } from '@nuxt/ui'
import { useConversations, useWebSocket } from '@/modules/Communication/composables'
import type { TabsItem } from '@nuxt/ui'
import type {
  FilterGroupItemV2,
  FilterConditionItemV2,
  FilterOperatorItemV2,
  FilterItemV2,
} from '@/common/types/filter-v2'
import { FilterItemType } from '@/common/types/filter-v2'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))
const MapView = defineAsyncComponent(() => import('@/common/components/MapView.vue'))
const LazyParcelQRModal = defineAsyncComponent(
  () => import('@/modules/Parcels/components/ParcelQRModal.vue'),
)
const LazyParcelProofModal = defineAsyncComponent(
  () => import('@/modules/Parcels/components/ParcelProofModal.vue'),
)

const UButton = resolveComponent('UButton')

const router = useRouter()
const toast = useToast()
const overlay = useOverlay()
const currentUser = getCurrentUser()
const { findOrCreateConversation } = useConversations()
const { connected, connect, subscribeTo } = useWebSocket()

// Tab state - mỗi tab có state riêng
type TabState = {
  parcels: ParcelDto[]
  loading: boolean
  page: number
  total: number
}

const tabStates = ref<Record<string, TabState>>({
  pending: { parcels: [], loading: false, page: 0, total: 0 },
  delivering: { parcels: [], loading: false, page: 0, total: 0 },
  'need-confirm': { parcels: [], loading: false, page: 0, total: 0 },
  delivered: { parcels: [], loading: false, page: 0, total: 0 },
  cancelled: { parcels: [], loading: false, page: 0, total: 0 },
})

const pageSize = ref(10)
const confirmingParcelId = ref<string | null>(null)
const disputingParcelId = ref<string | null>(null)
const retractingDisputeParcelId = ref<string | null>(null)
const activeTab = ref<string | number>('pending')

// Tracking modal state
const trackingParcel = ref<ParcelDto | null>(null)
const trackingSessionId = ref<string | null>(null)
const trackingAssignmentId = ref<string | null>(null)
const trackingDeliveryManId = ref<string | null>(null)
const trackingAssignmentStatus = ref<string | null>(null)
const trackingLocation = ref<{ lat: number; lon: number; timestamp?: string } | null>(null)
const trackingLoading = ref(false)
const trackingRoute = ref<DemoRouteResponse['result'] | null>(null)
const trackingRouteLoading = ref(false)
const showTrackingModal = ref(false)
const mapRoutes = ref<Array<{ coordinates: [number, number][]; distance: number; duration: number; properties?: Record<string, unknown> }>>([])

type TrackingMarker = {
  id: string
  coordinates: [number, number]
  type: 'center' | 'delivery' | 'warehouse' | 'custom'
  title: string
  color: string
  popup?: string
  parcelId?: string
}

const trackingMarkers = computed<TrackingMarker[]>(() => {
  const markers: TrackingMarker[] = []

  // Add shipper location if available
  if (trackingLocation.value && 
      typeof trackingLocation.value.lat === 'number' && 
      typeof trackingLocation.value.lon === 'number' &&
      !isNaN(trackingLocation.value.lat) && !isNaN(trackingLocation.value.lon) &&
      trackingLocation.value.lat >= -90 && trackingLocation.value.lat <= 90 &&
      trackingLocation.value.lon >= -180 && trackingLocation.value.lon <= 180) {
    const timestamp = trackingLocation.value.timestamp 
      ? new Date(trackingLocation.value.timestamp).toLocaleString('vi-VN')
      : 'Vừa cập nhật'
    
    markers.push({
      id: 'shipper',
      coordinates: [trackingLocation.value.lon, trackingLocation.value.lat],
      type: 'custom',
      title: 'Vị trí shipper',
      color: '#ef4444',
      popup: `
        <div class="p-2">
          <h3 class="font-semibold text-sm mb-1">🚚 Vị trí shipper</h3>
          <p class="text-xs text-gray-600 mb-1">
            <strong>Vĩ độ:</strong> ${trackingLocation.value.lat.toFixed(6)}
          </p>
          <p class="text-xs text-gray-600 mb-1">
            <strong>Kinh độ:</strong> ${trackingLocation.value.lon.toFixed(6)}
          </p>
          <p class="text-xs text-gray-500">
            <strong>Cập nhật:</strong> ${timestamp}
          </p>
        </div>
      `,
    })
  }

  // Add destination (parcel destination address location)
  // Use parcel's lat/lon which should be the destination address coordinates
  // This matches Android behavior where endPoint is taken from route (parcel destination)
  if (trackingParcel.value?.lat != null && trackingParcel.value?.lon != null) {
    const lat = trackingParcel.value.lat
    const lon = trackingParcel.value.lon
    
    // Validate coordinates before adding marker
    if (typeof lat === 'number' && typeof lon === 'number' &&
        !isNaN(lat) && !isNaN(lon) &&
        lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
      const parcel = trackingParcel.value
      const receiverName = parcel.receiverName || parcel.receiverId || 'N/A'
      const parcelCode = parcel.code || parcel.id?.substring(0, 8) || 'N/A'
      const destination = parcel.targetDestination || 'N/A'
      
      markers.push({
        id: 'destination',
        coordinates: [lon, lat], // [lon, lat] format for MapLibre
        type: 'delivery',
        title: 'Địa chỉ giao hàng',
        color: '#3b82f6',
        popup: `
          <div class="p-2">
            <h3 class="font-semibold text-sm mb-1">📦 Địa chỉ giao hàng</h3>
            <p class="text-xs text-gray-600 mb-1">
              <strong>Người nhận:</strong> ${receiverName}
            </p>
            <p class="text-xs text-gray-600 mb-1">
              <strong>Mã đơn:</strong> ${parcelCode}
            </p>
            <p class="text-xs text-gray-600 mb-1">
              <strong>Địa chỉ:</strong> ${destination}
            </p>
            <p class="text-xs text-gray-500">
              <strong>Vị trí:</strong> ${lat.toFixed(6)}, ${lon.toFixed(6)}
            </p>
          </div>
        `,
        parcelId: parcel.id,
      })
    }
  }

  // Only show warehouse if shipper location is not available
  // Starting point should be shipper's real-time location (from WebSocket tracking)
  // If shipper location is not available yet, show warehouse as fallback
  if (!trackingLocation.value) {
    const warehouseLat = 10.82398098
    const warehouseLon = 106.79611036
    markers.push({
      id: 'warehouse',
      coordinates: [warehouseLon, warehouseLat], // [lon, lat]
      type: 'warehouse',
      title: 'Kho hàng',
      color: '#16a34a',
      popup: `
        <div class="p-2">
          <h3 class="font-semibold text-sm mb-1">🏭 Kho hàng</h3>
          <p class="text-xs text-gray-600 mb-1">
            <strong>Điểm xuất phát (dự phòng)</strong>
          </p>
          <p class="text-xs text-gray-500">
            <strong>Vị trí:</strong> ${warehouseLat.toFixed(6)}, ${warehouseLon.toFixed(6)}
          </p>
        </div>
      `,
    })
  }

  return markers
})

// Computed để lấy state của tab hiện tại
const currentTabState = computed(() => {
  const tabKey = String(activeTab.value)
  const state = tabStates.value[tabKey] || tabStates.value.pending
  return state || { parcels: [], loading: false, page: 0, total: 0 }
})

const parcels = computed(() => currentTabState.value.parcels)
const loading = computed(() => currentTabState.value.loading)
const page = computed(() => currentTabState.value.page)
const total = computed(() => currentTabState.value.total)

const paginationSummary = computed(() => {
  if (total.value === 0) {
    return { start: 0, end: 0 }
  }
  const start = page.value * pageSize.value + 1
  const end = Math.min((page.value + 1) * pageSize.value, total.value)
  return { start, end }
})

/**
 * Ensure WebSocket is connected for current user (client)
 * Waits for connection to be fully established before returning
 */
const ensureWebSocketConnected = async () => {
  if (!currentUser?.id) return
  
  // If already connected, return immediately
  if (connected.value) return
  
  // Start connection
  await connect(currentUser.id)
  
  // Wait for connection to be established (with timeout)
  const maxWaitTime = 10000 // 10 seconds
  const checkInterval = 100 // Check every 100ms
  const startTime = Date.now()
  
  while (!connected.value && (Date.now() - startTime) < maxWaitTime) {
    await new Promise(resolve => setTimeout(resolve, checkInterval))
  }
  
  if (!connected.value) {
    throw new Error('WebSocket connection timeout')
  }
}

/**
 * Get status filter for tab
 */
const getStatusFilterForTab = (tab: string | number) => {
  switch (tab) {
    case 'pending':
      return ['IN_WAREHOUSE']
    case 'delivering':
      return ['ON_ROUTE']
    case 'need-confirm':
      return ['DELIVERED', 'DISPUTE']
    case 'delivered':
      return ['SUCCEEDED']
    case 'cancelled':
      return ['FAILED']
    default:
      return undefined
  }
}

/**
 * Tab items configuration with computed badges
 */
const tabItems = computed<TabsItem[]>(() => [
  {
    label: 'Đơn chờ giao',
    icon: 'i-heroicons-clock',
    value: 'pending',
    badge: tabStates.value.pending.total || undefined,
  },
  {
    label: 'Đơn đang giao',
    icon: 'i-heroicons-truck',
    value: 'delivering',
    badge: tabStates.value.delivering.total || undefined,
  },
  {
    label: 'Đơn cần xác nhận',
    icon: 'i-heroicons-check-circle',
    value: 'need-confirm',
    badge: tabStates.value['need-confirm'].total || undefined,
  },
  {
    label: 'Đơn đã giao',
    icon: 'i-heroicons-check-badge',
    value: 'delivered',
    badge: tabStates.value.delivered.total || undefined,
  },
  {
    label: 'Đơn đã huỷ',
    icon: 'i-heroicons-x-circle',
    value: 'cancelled',
    badge: tabStates.value.cancelled.total || undefined,
  },
])

/**
 * Build filter for a tab
 */
const buildFilterForTab = (tab: string | number): FilterGroupItemV2 | undefined => {
  const statusFilter = getStatusFilterForTab(tab)
  if (!statusFilter || statusFilter.length === 0) return undefined

  if (statusFilter.length === 1) {
    // Single status - use EQUALS
    const condition: FilterConditionItemV2 = {
      type: FilterItemType.CONDITION,
      field: 'status',
      operator: 'EQUALS',
      value: statusFilter[0],
      caseSensitive: false,
    }
    return {
      type: FilterItemType.GROUP,
      items: [condition],
    }
  } else {
    // Multiple statuses - use nested group with OR operator
    const nestedItems: FilterItemV2[] = []
    statusFilter.forEach((status, index) => {
      if (index > 0) {
        // Add OR operator before each condition except the first
        const operator: FilterOperatorItemV2 = {
          type: FilterItemType.OPERATOR,
          value: 'OR',
        }
        nestedItems.push(operator)
      }
      const condition: FilterConditionItemV2 = {
        type: FilterItemType.CONDITION,
        field: 'status',
        operator: 'EQUALS',
        value: status,
        caseSensitive: false,
      }
      nestedItems.push(condition)
    })

    // Wrap in nested group
    const nestedGroup: FilterGroupItemV2 = {
      type: FilterItemType.GROUP,
      items: nestedItems,
    }

    // Wrap nested group in outer group
    return {
      type: FilterItemType.GROUP,
      items: [nestedGroup],
    }
  }
}

/**
 * Load count for a specific tab (without loading data)
 */
const loadTabCount = async (tabKey: string | number) => {
  if (!currentUser?.id) return

  const tabKeyStr = String(tabKey)
  const tabState = tabStates.value[tabKeyStr]
  if (!tabState) return

  try {
    const filters = buildFilterForTab(tabKey)
    const response = await getClientReceivedParcels({
      page: 0,
      size: 1, // Only need total, not data
      filters,
      sorts: [
        {
          field: 'createdAt',
          direction: 'desc',
        },
      ],
    })

    if (response.result) {
      tabState.total = response.result.page.totalElements
    }
  } catch (error) {
    console.error(`Failed to load count for tab ${tabKey}:`, error)
  }
}

/**
 * Load counts for all tabs
 */
const loadAllTabCounts = async () => {
  if (!currentUser?.id) return

  // Load all tab counts in parallel
  const tabKeys = ['pending', 'delivering', 'need-confirm', 'delivered', 'cancelled']
  await Promise.all(tabKeys.map((tabKey) => loadTabCount(tabKey)))
}

/**
 * Load route for tracking session
 */
const loadTrackingRoute = async (sessionId: string) => {
  if (!sessionId || trackingRouteLoading.value) return

  trackingRouteLoading.value = true
  try {
    const response = await getSessionDemoRoute(sessionId, {
      vehicle: 'bicycle',
      routingType: 'full',
    })
    if (response?.result) {
      trackingRoute.value = response.result
      
      // Parse route geometry to coordinates for map display
      const routeGeometry = response.result.route?.geometry
      if (routeGeometry) {
        const coordinates = parseRouteGeometry(routeGeometry)
        if (coordinates.length > 0) {
          mapRoutes.value = [
            {
              coordinates,
              distance: response.result.route?.distance ?? 0,
              duration: response.result.route?.duration ?? 0,
              properties: {
                color: '#1d4ed8',
                width: 5,
                opacity: 0.85,
              },
            },
          ]
        } else {
          mapRoutes.value = []
        }
      } else {
        mapRoutes.value = []
      }
    } else {
      mapRoutes.value = []
    }
  } catch (error) {
    console.error('Failed to load tracking route:', error)
    // Don't show error - route is optional
  } finally {
    trackingRouteLoading.value = false
  }
}

/**
 * Open real-time tracking for a parcel in delivering tab.
 * Client subscribes only when viewing their parcel.
 */
const handleTrackShipper = async (parcel: ParcelDto) => {
  if (!parcel.id) return
  
  // Open modal immediately
  trackingParcel.value = parcel
  showTrackingModal.value = true
  trackingLocation.value = null
  trackingSessionId.value = null
  trackingAssignmentId.value = null
  trackingDeliveryManId.value = null
  trackingAssignmentStatus.value = null
  trackingRoute.value = null
  mapRoutes.value = []
  trackingLoading.value = true

  try {
    await ensureWebSocketConnected()
    
    // Fetch fresh parcel info from API to ensure we have correct location (lat/lon)
    let freshParcel: ParcelDto | null = null
    try {
      const parcelResponse = await getParcelById(parcel.id)
      if (parcelResponse?.result) {
        freshParcel = new ParcelDto(parcelResponse.result)
        // Update trackingParcel with fresh data (especially lat/lon)
        trackingParcel.value = freshParcel
      }
    } catch (error) {
      console.warn('Failed to fetch fresh parcel info, using cached data:', error)
      // Continue with original parcel data if fetch fails
    }
    
    const latest: LatestAssignmentResponse | null = await getLatestAssignmentForParcel(parcel.id)
    if (!latest || !latest.sessionId) {
      toast.add({
        title: 'Không tìm thấy phiên giao hàng',
        description: 'Không thể xác định phiên giao hàng hiện tại cho đơn này.',
        color: 'warning',
      })
      trackingLoading.value = false
      trackingParcel.value = null
      return
    }

    // Store assignment information
    trackingSessionId.value = latest.sessionId
    trackingAssignmentId.value = latest.assignmentId
    trackingDeliveryManId.value = latest.deliveryManId
    trackingAssignmentStatus.value = latest.status

    // Load route for visualization
    if (latest.sessionId) {
      await loadTrackingRoute(latest.sessionId)
    }

    // Subscribe to session tracking topic
    const destination = `/topic/sessions/${latest.sessionId}/tracking`
    subscribeTo(destination, (event: { lat?: number; lon?: number; timestamp?: string } | null) => {
      if (!event || event.lat == null || event.lon == null) return
      trackingLocation.value = {
        lat: event.lat,
        lon: event.lon,
        timestamp: event.timestamp,
      }
    })
  } catch (error) {
    console.error('Failed to start shipper tracking:', error)
    toast.add({
      title: 'Lỗi',
      description: 'Không thể bắt đầu theo dõi vị trí shipper.',
      color: 'error',
    })
    trackingParcel.value = null
  } finally {
    trackingLoading.value = false
  }
}

const closeTrackingModal = () => {
  showTrackingModal.value = false
  trackingParcel.value = null
  trackingSessionId.value = null
  trackingAssignmentId.value = null
  trackingDeliveryManId.value = null
  trackingAssignmentStatus.value = null
  trackingLocation.value = null
  trackingRoute.value = null
  mapRoutes.value = []
  trackingLoading.value = false
  trackingRouteLoading.value = false
}

/**
 * Load parcels for a specific tab
 */
const loadParcels = async (tabKey?: string | number) => {
  if (!currentUser?.id) return

  const targetTab = tabKey || activeTab.value
  const tabKeyStr = String(targetTab)
  const tabState = tabStates.value[tabKeyStr]

  if (!tabState) return

  tabState.loading = true
  try {
    const filters = buildFilterForTab(targetTab)

    const response = await getClientReceivedParcels({
      page: tabState.page,
      size: pageSize.value,
      filters,
      sorts: [
        {
          field: 'createdAt',
          direction: 'desc',
        },
      ],
    })

    if (response.result) {
      tabState.parcels = response.result.data.map((p) => new ParcelDto(p))
      tabState.total = response.result.page.totalElements
    }
  } catch (error) {
    console.error('Failed to load parcels:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to load parcels',
      color: 'error',
    })
  } finally {
    tabState.loading = false
  }
}

const goToCreateParcel = () => {
  router.push({ name: 'client-create-parcel' })
}

const getStatusColor = (status: ParcelStatus) => {
  const colorMap: Record<
    ParcelStatus,
    'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
  > = {
    IN_WAREHOUSE: 'neutral',
    ON_ROUTE: 'primary',
    DELIVERED: 'success',
    SUCCEEDED: 'success',
    FAILED: 'error',
    DELAYED: 'warning',
    DISPUTE: 'info',
    LOST: 'error',
  }
  return colorMap[status] || 'neutral'
}

const handlePageChange = (newPage: number) => {
  const tabKeyStr = String(activeTab.value)
  const tabState = tabStates.value[tabKeyStr]
  if (!tabState || newPage === tabState.page) return
  tabState.page = Math.max(newPage, 0)
  loadParcels(activeTab.value)
}

// Watch for tab changes to load data
watch(activeTab, (newTab) => {
  const tabKeyStr = String(newTab)
  const tabState = tabStates.value[tabKeyStr]
  // Only load if tab has no data yet
  if (tabState && (!tabState.parcels || tabState.parcels.length === 0) && !tabState.loading) {
    loadParcels(newTab)
  }
})

const isConfirming = (parcelId: string) => confirmingParcelId.value === parcelId

const canConfirmParcel = (parcel: ParcelDto) => parcel.status === 'DELIVERED'

const canReportNotReceived = (parcel: ParcelDto) => parcel.status === 'DELIVERED'

const canRetractDispute = (parcel: ParcelDto) => parcel.status === 'DISPUTE'

const handleConfirmReceived = async (parcel: ParcelDto) => {
  if (!canConfirmParcel(parcel)) return
  confirmingParcelId.value = parcel.id
  try {
    await confirmParcelReceived(parcel.id, {
      confirmationSource: 'WEB_CLIENT',
    })
    toast.add({
      title: 'Parcel confirmed',
      description: `Parcel ${parcel.code} marked as received`,
      color: 'success',
    })
    await loadParcels(activeTab.value)
    // Refresh counts for all tabs after status change
    await loadAllTabCounts()
  } catch (error) {
    console.error('Failed to confirm parcel:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to confirm parcel delivery',
      color: 'error',
    })
  } finally {
    confirmingParcelId.value = null
  }
}

const handleReportNotReceived = async (parcel: ParcelDto) => {
  if (!canReportNotReceived(parcel)) return
  disputingParcelId.value = parcel.id
  try {
    await reportParcelNotReceived(parcel.id)
    toast.add({
      title: 'Báo cáo đã gửi',
      description: `Đã báo chưa nhận được đơn hàng ${parcel.code}`,
      color: 'warning',
    })
    await loadParcels(activeTab.value)
    // Refresh counts for all tabs after status change
    await loadAllTabCounts()
  } catch (error) {
    console.error('Failed to report not received:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to report parcel not received',
      color: 'error',
    })
  } finally {
    disputingParcelId.value = null
  }
}

const handleRetractDispute = async (parcel: ParcelDto) => {
  if (!canRetractDispute(parcel)) return
  retractingDisputeParcelId.value = parcel.id
  try {
    await retractDispute(parcel.id)
    toast.add({
      title: 'Đã xác nhận',
      description: `Đã xác nhận nhận được đơn hàng ${parcel.code}`,
      color: 'success',
    })
    await loadParcels(activeTab.value)
    // Refresh counts for all tabs after status change
    await loadAllTabCounts()
  } catch (error) {
    console.error('Failed to retract dispute:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to retract dispute',
      color: 'error',
    })
  } finally {
    retractingDisputeParcelId.value = null
  }
}

/**
 * Open QR code modal
 */
const openQRModal = async (parcel: ParcelDto) => {
  const modal = overlay.create(LazyParcelQRModal)
  const instance = modal.open({ parcelId: parcel.id, parcelCode: parcel.code })
  await instance.result
}

/**
 * Check if can view proofs (for DELIVERED, SUCCEEDED, DISPUTE statuses)
 */
const canViewProofs = (parcel: ParcelDto) => {
  return ['DELIVERED', 'SUCCEEDED', 'DISPUTE'].includes(parcel.status)
}

/**
 * Open proof modal
 */
const openProofModal = async (parcel: ParcelDto) => {
  const modal = overlay.create(LazyParcelProofModal)
  const instance = modal.open({ parcelId: parcel.id, parcelCode: parcel.code })
  await instance.result
}

/**
 * Open chat with sender
 */
const openChat = async (parcel: ParcelDto) => {
  if (!currentUser?.id || !parcel.senderId) {
    toast.add({
      title: 'Error',
      description: 'Cannot open chat: missing user or sender information',
      color: 'error',
    })
    return
  }

  try {
    // Find or create conversation between current user and sender
    const conversation = await findOrCreateConversation(currentUser.id, parcel.senderId)

    if (!conversation || !conversation.conversationId) {
      toast.add({
        title: 'Error',
        description: 'Failed to create or find conversation',
        color: 'error',
      })
      return
    }

    // Navigate to chat with conversationId as required param
    router.push({
      name: 'communication-chat',
      params: { conversationId: conversation.conversationId },
      query: { partnerId: parcel.senderId },
    })
  } catch (error) {
    console.error('Failed to open chat:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to open chat',
      color: 'error',
    })
  }
}

/**
 * Check if can chat with sender
 */
const canChat = (parcel: ParcelDto) => {
  return !!parcel.senderId && !!currentUser?.id
}

/**
 * Table columns configuration
 */
const columns: TableColumn<ParcelDto>[] = [
  {
    accessorKey: 'code',
    header: 'Code',
  },
  {
    accessorKey: 'senderName',
    header: 'Sender',
  },
  {
    accessorKey: 'targetDestination',
    header: 'Destination',
  },
  {
    accessorKey: 'status',
    header: 'Status',
    cell: ({ row }) => {
      const status = row.original.status
      const color = getStatusColor(status)
      return h('div', { class: 'flex flex-col gap-2' }, [
        h(
          'span',
          {
            class: 'inline-flex items-center px-2 py-1 rounded-md text-xs font-medium',
            style: {
              backgroundColor: `var(--color-${color}-50)`,
              color: `var(--color-${color}-700)`,
            },
          },
          row.original.displayStatus || status,
        ),
      ])
    },
  },
  {
    accessorKey: 'deliveryType',
    header: 'Type',
    cell: ({ row }) => {
      return h(
        'span',
        {
          class: 'inline-flex items-center px-2 py-1 rounded-md text-xs font-medium border',
        },
        row.original.deliveryType,
      )
    },
  },
  {
    accessorKey: 'createdAt',
    header: 'Created',
    cell: ({ row }) => {
      return h('span', new Date(row.original.createdAt).toLocaleString())
    },
  },
  {
    accessorKey: 'actions',
    header: 'Actions',
    cell: ({ row }) => {
      const parcel = row.original
      const canConfirm = canConfirmParcel(parcel)
      const canReport = canReportNotReceived(parcel)
      const canRetract = canRetractDispute(parcel)
      const canChatWithSender = canChat(parcel)

      const canViewProofsForParcel = canViewProofs(parcel)
      const isDelivering = parcel.status === 'ON_ROUTE'

      return h('div', { class: 'flex space-x-2' }, [
        // Chat with sender button
        h(UButton, {
          icon: 'i-heroicons-chat-bubble-left-right',
          size: 'sm',
          variant: 'ghost',
          disabled: !canChatWithSender,
          title: canChatWithSender ? 'Chat with sender' : 'Sender information not available',
          onClick: () => openChat(parcel),
        }),
        // QR Code button
        h(UButton, {
          icon: 'i-heroicons-qr-code',
          size: 'sm',
          variant: 'ghost',
          title: 'Show QR Code',
          onClick: () => openQRModal(parcel),
        }),
        // View proofs button (for DELIVERED, SUCCEEDED, DISPUTE)
        canViewProofsForParcel &&
          h(UButton, {
            icon: 'i-heroicons-photo',
            size: 'sm',
            variant: 'ghost',
            title: 'Xem ảnh/video đơn hàng',
            onClick: () => openProofModal(parcel),
          }),
        // Report not received button (for DELIVERED status)
        canReport &&
          h(
            UButton,
            {
              size: 'sm',
              variant: 'soft',
              color: 'warning',
              loading: disputingParcelId.value === parcel.id,
              title: 'Báo chưa nhận được hàng',
              onClick: () => handleReportNotReceived(parcel),
            },
            () => {
              if (disputingParcelId.value === parcel.id) {
                return 'Đang gửi...'
              }
              return 'Chưa nhận được'
            },
          ),
        // Confirm received button (for DELIVERED status)
        canConfirm &&
          h(
            UButton,
            {
              size: 'sm',
              variant: 'soft',
              color: 'primary',
              disabled: !canConfirm || isConfirming(parcel.id),
              loading: isConfirming(parcel.id),
              title: canConfirm
                ? 'Xác nhận đã nhận hàng'
                : 'Chỉ có thể xác nhận khi đơn hàng ở trạng thái DELIVERED',
              onClick: () => handleConfirmReceived(parcel),
              class: !canConfirm ? 'opacity-50 cursor-not-allowed' : '',
            },
            () => {
              if (isConfirming(parcel.id)) {
                return 'Đang xác nhận...'
              }
              if (!canConfirm) {
                return 'Chờ giao hàng'
              }
              return 'Đã nhận hàng'
            },
          ),
        // Retract dispute button (for DISPUTE status)
        canRetract &&
          h(
            UButton,
            {
              size: 'sm',
              variant: 'soft',
              color: 'success',
              loading: retractingDisputeParcelId.value === parcel.id,
              title: 'Tôi đã nhận được hàng',
              onClick: () => handleRetractDispute(parcel),
            },
            () => {
              if (retractingDisputeParcelId.value === parcel.id) {
                return 'Đang xử lý...'
              }
              return 'Đã nhận được hàng'
            },
          ),
        // Track shipper location button (only when parcel is being delivered)
        isDelivering &&
          h(
            UButton,
            {
              size: 'sm',
              variant: 'ghost',
              color: 'primary',
              title: 'Theo dõi vị trí shipper',
              onClick: () => handleTrackShipper(parcel),
            },
            () => 'Theo dõi',
          ),
      ])
    },
  },
]

onMounted(async () => {
  // Load counts for all tabs first
  await loadAllTabCounts()
  // Then load data for active tab
  await loadParcels(activeTab.value)
})
</script>

<template>
  <div class="container mx-auto px-2 md:px-4 py-4 md:py-6 space-y-4 md:space-y-6">
    <PageHeader title="Đơn hàng của tôi" description="Xem và quản lý đơn hàng">
      <template #actions>
        <UButton
          color="primary"
          icon="i-heroicons-plus"
          size="sm"
          class="md:size-md"
          @click="goToCreateParcel"
        >
          <span class="hidden sm:inline">Tạo đơn hàng</span>
          <span class="sm:hidden">Tạo</span>
        </UButton>
      </template>
    </PageHeader>

    <div class="space-y-4">
      <!-- Tabs for filtering parcels by status -->
      <UTabs
        v-model="activeTab"
        :items="tabItems"
        class="w-full"
        :ui="{
          list: 'overflow-x-auto whitespace-nowrap',
          trigger: 'min-w-[200px]',
          indicator: 'min-w-[200px]',
          root: 'w-full',
        }"
      >
        <template #content>
          <div class="space-y-4">
            <!-- Desktop Table View -->
            <div class="hidden md:block">
              <UTable
                :data="parcels"
                :columns="columns"
                :loading="loading"
                :ui="{
                  empty: 'text-center py-12',
                  root: 'h-[50vh]',
                  thead: 'sticky top-0 bg-white dark:bg-gray-800',
                }"
              >
                <template #cell(code)="{ row }">
                  <span class="font-mono text-sm">{{ row.original.code }}</span>
                </template>
              </UTable>
            </div>

            <!-- Mobile Card View -->
            <div class="md:hidden space-y-3">
              <template v-if="loading">
                <USkeleton v-for="i in 3" :key="i" class="h-40 w-full rounded-lg" />
              </template>
              <template v-else-if="parcels.length === 0">
                <div class="text-center py-12">
                  <UIcon name="i-heroicons-cube" class="w-16 h-16 text-gray-400 mx-auto mb-4" />
                  <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">
                    Chưa có đơn hàng
                  </h3>
                  <p class="text-gray-500">Không có đơn hàng nào trong danh mục này</p>
                </div>
              </template>
              <template v-else>
                <UCard v-for="parcel in parcels" :key="parcel.id" class="overflow-hidden">
                  <div class="space-y-3">
                    <!-- Header: Code and Status -->
                    <div class="flex items-center justify-between">
                      <span class="font-mono text-sm font-semibold text-gray-900">
                        {{ parcel.code }}
                      </span>
                      <UBadge :color="getStatusColor(parcel.status)" variant="soft" size="sm">
                        {{ parcel.displayStatus || parcel.status }}
                      </UBadge>
                    </div>

                    <!-- Info Grid -->
                    <div class="grid grid-cols-2 gap-2 text-sm">
                      <div>
                        <span class="text-gray-500">Người gửi:</span>
                        <p class="font-medium text-gray-900 truncate">
                          {{ parcel.senderName || 'N/A' }}
                        </p>
                      </div>
                      <div>
                        <span class="text-gray-500">Loại:</span>
                        <p class="font-medium text-gray-900">{{ parcel.deliveryType }}</p>
                      </div>
                    </div>

                    <!-- Destination -->
                    <div class="text-sm">
                      <span class="text-gray-500">Địa chỉ giao:</span>
                      <p class="font-medium text-gray-900 line-clamp-2">
                        {{ parcel.targetDestination || 'N/A' }}
                      </p>
                    </div>

                    <!-- Created Date -->
                    <div class="text-xs text-gray-500">
                      Tạo lúc: {{ new Date(parcel.createdAt).toLocaleString('vi-VN') }}
                    </div>

                    <!-- Actions -->
                    <div class="flex items-center justify-end gap-2 pt-2 border-t border-gray-100">
                      <UButton
                        v-if="canChat(parcel)"
                        icon="i-heroicons-chat-bubble-left-right"
                        size="xs"
                        variant="ghost"
                        color="neutral"
                        @click="openChat(parcel)"
                      >
                        Chat
                      </UButton>
                      <UButton
                        icon="i-heroicons-qr-code"
                        size="xs"
                        variant="ghost"
                        color="neutral"
                        @click="openQRModal(parcel)"
                      >
                        QR
                      </UButton>
                      <UButton
                        v-if="canViewProofs(parcel)"
                        icon="i-heroicons-photo"
                        size="xs"
                        variant="ghost"
                        color="neutral"
                        @click="openProofModal(parcel)"
                      >
                        Xem ảnh
                      </UButton>
                      <!-- Confirm received button (for DELIVERED status) -->
                      <UButton
                        v-if="canConfirmParcel(parcel)"
                        size="xs"
                        variant="soft"
                        color="primary"
                        :loading="isConfirming(parcel.id)"
                        :disabled="isConfirming(parcel.id)"
                        @click="handleConfirmReceived(parcel)"
                      >
                        {{ isConfirming(parcel.id) ? 'Đang xác nhận...' : 'Đã nhận hàng' }}
                      </UButton>
                      <!-- Report not received button (for DELIVERED status) -->
                      <UButton
                        v-if="canReportNotReceived(parcel)"
                        size="xs"
                        variant="soft"
                        color="warning"
                        :loading="disputingParcelId === parcel.id"
                        @click="handleReportNotReceived(parcel)"
                      >
                        {{ disputingParcelId === parcel.id ? 'Đang gửi...' : 'Chưa nhận được' }}
                      </UButton>
                      <!-- Retract dispute button (for DISPUTE status) -->
                      <UButton
                        v-if="canRetractDispute(parcel)"
                        size="xs"
                        variant="soft"
                        color="success"
                        :loading="retractingDisputeParcelId === parcel.id"
                        @click="handleRetractDispute(parcel)"
                      >
                        {{
                          retractingDisputeParcelId === parcel.id
                            ? 'Đang xử lý...'
                            : 'Đã nhận được hàng'
                        }}
                      </UButton>

                      <!-- Track shipper for ON_ROUTE parcels -->
                      <UButton
                        v-if="parcel.status === 'ON_ROUTE'"
                        size="xs"
                        variant="ghost"
                        color="primary"
                        @click="handleTrackShipper(parcel)"
                      >
                        Theo dõi
                      </UButton>
                    </div>
                  </div>
                </UCard>
              </template>
            </div>
          </div>
        </template>
      </UTabs>

      <div v-if="!loading && parcels.length === 0" class="text-center py-12">
        <UIcon name="i-heroicons-cube" class="w-16 h-16 text-gray-400 mx-auto mb-4" />
        <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">
          Chưa có đơn hàng
        </h3>
        <p class="text-gray-500 mb-4">Tạo đơn hàng đầu tiên để bắt đầu</p>
        <UButton color="primary" icon="i-heroicons-plus" @click="goToCreateParcel">
          Tạo đơn hàng
        </UButton>
      </div>

      <div
        v-else-if="parcels.length > 0"
        class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between"
      >
        <div class="text-sm text-gray-600 dark:text-gray-400">
          Hiển thị
          <span class="font-semibold">
            {{ paginationSummary.start }}–{{ paginationSummary.end }}
          </span>
          trên {{ total }} đơn hàng
        </div>
        <UPagination
          :model-value="page"
          :page-count="pageSize"
          :total="total"
          :max="5"
          @update:page="handlePageChange"
        />
      </div>
    </div>

    <!-- Tracking modal (aligned with other modals) -->
    <UModal
      :open="showTrackingModal"
      :title="trackingParcel ? `Theo dõi shipper - ${trackingParcel.code}` : 'Theo dõi shipper'"
      :description="trackingSessionId ? `Phiên giao: ${trackingSessionId}` : 'Đang kết nối vị trí shipper'"
      @update:open="showTrackingModal = $event"
      @close="closeTrackingModal"
      :ui="{ content: 'sm:max-w-2xl md:max-w-4xl', footer: 'justify-end' }"
    >
      <template #body>
        <div class="space-y-4">
          <!-- Loading state -->
          <div v-if="trackingLoading" class="text-center py-4">
            <UIcon name="i-heroicons-arrow-path" class="w-6 h-6 animate-spin text-gray-400 mx-auto mb-2" />
            <p class="text-gray-500">Đang kết nối tới vị trí shipper...</p>
          </div>

          <!-- Assignment info -->
          <div v-if="!trackingLoading && trackingSessionId" class="space-y-2 border-b pb-3">
            <div class="grid grid-cols-2 gap-2 text-sm">
              <div v-if="trackingAssignmentId">
                <span class="text-gray-500">Assignment ID:</span>
                <p class="font-mono text-xs">{{ trackingAssignmentId.substring(0, 8) }}...</p>
              </div>
              <div v-if="trackingAssignmentStatus">
                <span class="text-gray-500">Trạng thái:</span>
                <UBadge :color="trackingAssignmentStatus === 'IN_PROGRESS' ? 'primary' : 'neutral'" variant="soft" size="xs">
                  {{ trackingAssignmentStatus }}
                </UBadge>
              </div>
              <div v-if="trackingDeliveryManId">
                <span class="text-gray-500">Shipper ID:</span>
                <p class="font-mono text-xs">{{ trackingDeliveryManId.substring(0, 8) }}...</p>
              </div>
              <div v-if="trackingSessionId">
                <span class="text-gray-500">Session ID:</span>
                <p class="font-mono text-xs">{{ trackingSessionId.substring(0, 8) }}...</p>
              </div>
            </div>
          </div>

          <!-- Map and location info -->
          <div v-if="!trackingLoading" class="space-y-3">
            <div v-if="!trackingLocation && !trackingRoute" class="text-center py-4 text-gray-500">
              <UIcon name="i-heroicons-map-pin" class="w-8 h-8 mx-auto mb-2 text-gray-300" />
              <p>Chưa nhận được vị trí. Vui lòng giữ màn hình mở vài giây.</p>
            </div>
            <div v-else class="space-y-3">
              <MapView
                height="320px"
                :show-zones="false"
                :show-routing="mapRoutes.length > 0"
                :routes="mapRoutes"
                :markers="trackingMarkers"
                :auto-fit="true"
                :fit-padding="40"
              />
              
              <!-- Location details -->
              <div v-if="trackingLocation" class="space-y-2 border-t pt-3">
                <p class="text-sm">
                  <span class="font-semibold">Vị trí shipper:</span>
                  <span class="text-gray-600"> {{ trackingLocation.lat.toFixed(6) }}, {{ trackingLocation.lon.toFixed(6) }}</span>
                </p>
                <p v-if="trackingLocation.timestamp" class="text-sm">
                  <span class="font-semibold">Cập nhật lúc:</span>
                  <span class="text-gray-600"> {{ new Date(trackingLocation.timestamp).toLocaleString('vi-VN') }}</span>
                </p>
              </div>

              <!-- Route info -->
              <div v-if="trackingRoute?.route" class="space-y-2 border-t pt-3">
                <p class="text-sm">
                  <span class="font-semibold">Tuyến đường:</span>
                  <span class="text-gray-600"> {{ trackingRoute.route.distance ? `${(trackingRoute.route.distance / 1000).toFixed(2)} km` : 'Đang tính toán...' }}</span>
                </p>
                <p v-if="trackingRoute.route.duration" class="text-sm">
                  <span class="font-semibold">Thời gian dự kiến:</span>
                  <span class="text-gray-600"> {{ Math.round(trackingRoute.route.duration / 60) }} phút</span>
                </p>
                <p v-if="trackingRoute.summary" class="text-sm">
                  <span class="font-semibold">Tổng số điểm dừng:</span>
                  <span class="text-gray-600"> {{ trackingRoute.summary.totalWaypoints }}</span>
                </p>
              </div>
            </div>
          </div>
        </div>
      </template>
      <template #footer>
        <div class="flex justify-end gap-2">
          <UButton variant="soft" color="gray" @click="closeTrackingModal">Đóng</UButton>
        </div>
      </template>
    </UModal>
  </div>
</template>
