<script setup lang="ts">
/**
 * My Parcels View
 * Client view for managing their own parcels (as receiver)
 */

import { ref, onMounted, computed, h, resolveComponent } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import { getClientReceivedParcels, confirmParcelReceived } from '@/modules/Parcels/api'
import { ParcelDto, type ParcelStatus } from '@/modules/Parcels/model.type'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'
import { defineAsyncComponent } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { useConversations } from '@/modules/Communication/composables'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))
const LazyParcelQRModal = defineAsyncComponent(
  () => import('@/modules/Parcels/components/ParcelQRModal.vue'),
)

const UButton = resolveComponent('UButton')

const router = useRouter()
const toast = useToast()
const overlay = useOverlay()
const currentUser = getCurrentUser()
const { findOrCreateConversation } = useConversations()

const parcels = ref<ParcelDto[]>([])
const loading = ref(false)
const page = ref(0)
const pageSize = ref(10)
const total = ref(0)
const confirmingParcelId = ref<string | null>(null)

const paginationSummary = computed(() => {
  if (total.value === 0) {
    return { start: 0, end: 0 }
  }
  const start = page.value * pageSize.value + 1
  const end = Math.min((page.value + 1) * pageSize.value, total.value)
  return { start, end }
})

const loadParcels = async () => {
  if (!currentUser?.id) return

  loading.value = true
  try {
    const response = await getClientReceivedParcels({
      page: page.value,
      size: pageSize.value,
      sorts: [
        {
          field: 'createdAt',
          direction: 'desc',
        },
      ],
    })

    if (response.result) {
      parcels.value = response.result.data.map((p) => new ParcelDto(p))
      total.value = response.result.page.totalElements
    }
  } catch (error) {
    console.error('Failed to load parcels:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to load parcels',
      color: 'error',
    })
  } finally {
    loading.value = false
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
  if (newPage === page.value) return
  page.value = Math.max(newPage, 0)
  loadParcels()
}

const isConfirming = (parcelId: string) => confirmingParcelId.value === parcelId

const canConfirmParcel = (parcel: ParcelDto) => parcel.status === 'DELIVERED'

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
    await loadParcels()
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

/**
 * Open QR code modal
 */
const openQRModal = async (parcel: ParcelDto) => {
  const modal = overlay.create(LazyParcelQRModal)
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
      const canChatWithSender = canChat(parcel)

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
        // Confirm received button
        h(
          UButton,
          {
            size: 'sm',
            variant: canConfirm ? 'soft' : 'ghost',
            color: canConfirm ? 'primary' : 'neutral',
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
            return 'Xác nhận nhận hàng'
          },
        ),
      ])
    },
  },
]

onMounted(() => {
  loadParcels()
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
                  <p class="font-medium text-gray-900 truncate">{{ parcel.senderName || 'N/A' }}</p>
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
                <!-- Note: Confirm received button is hidden on mobile/Android as confirmation is handled in Android app -->
              </div>
            </div>
          </UCard>
        </template>
      </div>

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
  </div>
</template>
