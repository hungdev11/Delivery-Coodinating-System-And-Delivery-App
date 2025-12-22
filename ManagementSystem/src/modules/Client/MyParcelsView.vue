<script setup lang="ts">
/**
 * My Parcels View
 * Client view for managing their own parcels (as receiver)
 */

import { ref, onMounted, computed, h, resolveComponent, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import {
  getClientReceivedParcels,
  confirmParcelReceived,
  reportParcelNotReceived,
  retractDispute,
} from '@/modules/Parcels/api'
import { ParcelDto, type ParcelStatus } from '@/modules/Parcels/model.type'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'
import { defineAsyncComponent } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { useConversations } from '@/modules/Communication/composables'
import type { TabsItem } from '@nuxt/ui'
import type {
  FilterGroupItemV2,
  FilterConditionItemV2,
  FilterOperatorItemV2,
  FilterItemV2,
} from '@/common/types/filter-v2'
import { FilterItemType } from '@/common/types/filter-v2'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))
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
                        {{
                          disputingParcelId === parcel.id ? 'Đang gửi...' : 'Chưa nhận được'
                        }}
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
              </div>
            </div>
          </UCard>
        </template>
      </div>
          </div>
        </template>
      </UTabs>

      <div
        v-if="!loading && parcels.length === 0"
        class="text-center py-12"
      >
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
  </div>
</template>
