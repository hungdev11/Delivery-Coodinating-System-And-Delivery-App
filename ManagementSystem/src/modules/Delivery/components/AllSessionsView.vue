<script setup lang="ts">
/**
 * All Sessions View
 *
 * View to display all delivery sessions for selected shippers in a table format
 */

import { onMounted, computed, ref, h, resolveComponent, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getDeliverySessions, getDeliveryMenV2, getActiveSessionForDeliveryMan } from '../api'
import { DeliveryManDto } from '../model.type'
import type { DeliverySessionDto } from '../model.type'
import type { FilterGroup } from '@/common/types/filter'
import type { TableColumn } from '@nuxt/ui'
import { defineAsyncComponent } from 'vue'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))
const UButton = resolveComponent('UButton')
const UBadge = resolveComponent('UBadge')
const USelectMenu = resolveComponent('USelectMenu')

const router = useRouter()

// Data
const shippers = ref<DeliveryManDto[]>([])
const sessions = ref<DeliverySessionDto[]>([])
const sessionsLoading = ref(false)
const shippersLoading = ref(false)
const selectedShipperId = ref<string>('') // Empty = show all
const activeSessionIds = ref<Set<string>>(new Set())
const showInactive = ref(false)

// Shipper options for USelectMenu
const shipperOptions = computed(() => {
  return [
    { label: 'Tất cả shippers', value: '' },
    ...shippers.value.map((s) => ({
      label: `${s.displayName} (${s.email})`,
      value: s.id,
    })),
  ]
})

// Selected shipper option for USelectMenu
const selectedShipperOption = computed(() => {
  return shipperOptions.value.find((opt) => opt.value === selectedShipperId.value) || shipperOptions.value[0]
})

// Check for query parameters (from redirect)
onMounted(async () => {
  await loadShippers()
  await loadAllSessions()

  // Check if we have a shipper filter from query params
  const route = router.currentRoute.value
  if (route.query.userId) {
    // Find shipper by userId
    const shipper = shippers.value.find((s) => s.userId === route.query.userId as string)
    if (shipper) {
      selectedShipperId.value = shipper.id
    }
  } else if (route.query.shipperId) {
    selectedShipperId.value = route.query.shipperId as string
  }
})

/**
 * Handle shipper selection change from USelectMenu
 */
const handleShipperSelectionChange = (opt: { label: string; value: string } | null) => {
  selectedShipperId.value = opt?.value || ''
}

// No need to watch selectedShipperId - we filter client-side
watch(showInactive, () => {
  // Just update displayed sessions
})

/**
 * Load all shippers
 */
const loadShippers = async () => {
  shippersLoading.value = true
  try {
    const response = await getDeliveryMenV2({
      page: 0,
      size: 100,
    })
    if (response.result?.data) {
      shippers.value = response.result.data.map((s: unknown) => new DeliveryManDto(s as DeliveryManDto))

      // Load active sessions for all shippers
      await loadActiveSessions()
    }
  } catch (error) {
    console.error('Failed to load shippers:', error)
  } finally {
    shippersLoading.value = false
  }
}

/**
 * Load active sessions for all shippers
 */
const loadActiveSessions = async () => {
  activeSessionIds.value.clear()
  for (const shipper of shippers.value) {
    try {
      const response = await getActiveSessionForDeliveryMan(shipper.userId)
      if (response.result?.id) {
        activeSessionIds.value.add(response.result.id)
      }
    } catch {
      // No active session for this shipper
    }
  }
}

/**
 * Load all sessions (load all, filter client-side)
 */
const loadAllSessions = async () => {
  sessionsLoading.value = true
  try {
    // Load all sessions without filter - we'll filter client-side
    const filterGroup: FilterGroup = {
      logic: 'AND',
      conditions: [],
    }

    const response = await getDeliverySessions({
      filters: filterGroup,
      page: 0,
      size: 200, // Load more sessions
      sorts: [
        {
          field: 'startTime',
          direction: 'desc',
        },
      ],
    })

    if (response.result?.data) {
      sessions.value = response.result.data
    }
  } catch (error) {
    console.error('Failed to load sessions:', error)
  } finally {
    sessionsLoading.value = false
  }
}

/**
 * Get shipper name by userId
 */
const getShipperName = (userId: string): string => {
  const shipper = shippers.value.find((s) => s.userId === userId)
  return shipper?.displayName || userId.substring(0, 8)
}

/**
 * Get shipper email by userId
 */
const getShipperEmail = (userId: string): string => {
  const shipper = shippers.value.find((s) => s.userId === userId)
  return shipper?.email || ''
}

/**
 * Filter displayed sessions (client-side filter for shipper and active/inactive)
 */
const displayedSessions = computed(() => {
  let filtered = sessions.value

  // Client-side filter: by shipper
  if (selectedShipperId.value) {
    const shipper = shippers.value.find((s) => s.id === selectedShipperId.value)
    if (shipper) {
      filtered = filtered.filter((session) => session.deliveryManId === shipper.userId)
    }
  }

  // Client-side filter: show active/inactive
  if (!showInactive.value) {
    filtered = filtered.filter((session) => activeSessionIds.value.has(session.id))
  }

  return filtered
})

// Statistics for displayed sessions (after shipper filter, before active/inactive filter)
const filteredByShipperSessions = computed(() => {
  if (!selectedShipperId.value) {
    return sessions.value
  }
  const shipper = shippers.value.find((s) => s.id === selectedShipperId.value)
  if (!shipper) {
    return sessions.value
  }
  return sessions.value.filter((session) => session.deliveryManId === shipper.userId)
})

const totalSessions = computed(() => filteredByShipperSessions.value.length)
const activeSessions = computed(() => filteredByShipperSessions.value.filter((session) => activeSessionIds.value.has(session.id)).length)
const inactiveSessions = computed(() => totalSessions.value - activeSessions.value)

/**
 * Navigate to session detail
 */
const navigateToSession = (session: DeliverySessionDto) => {
  router.push({ name: 'delivery-session-detail', params: { sessionId: session.id } })
}

/**
 * Format date for display
 */
const formatDate = (value?: string | null) => {
  if (!value) return '—'
  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

/**
 * Table columns configuration
 */
const columns: TableColumn<DeliverySessionDto>[] = [
  {
    accessorKey: 'actions',
    header: 'Thao tác',
    cell: ({ row }) => {
      const session = row.original
      return h(
        UButton,
        {
          size: 'xs',
          variant: 'ghost',
          icon: 'i-heroicons-arrow-top-right-on-square',
          onClick: () => navigateToSession(session),
        },
          () => 'Xem chi tiết',
      )
    },
  },
  {
    accessorKey: 'deliveryManId',
    header: 'Shipper',
    cell: ({ row }) => {
      const session = row.original
      return h('div', { class: 'space-y-1' }, [
        h('div', { class: 'font-medium' }, getShipperName(session.deliveryManId)),
        h('div', { class: 'text-xs text-gray-500' }, getShipperEmail(session.deliveryManId)),
      ])
    },
  },
  {
    accessorKey: 'id',
    header: 'ID phiên',
    cell: ({ row }) => {
      return h('span', { class: 'font-mono text-xs' }, row.original.id.substring(0, 8))
    },
  },
  {
    accessorKey: 'status',
    header: 'Trạng thái',
    cell: ({ row }) => {
      const session = row.original
      const status = session.status
      const isActive = activeSessionIds.value.has(session.id)
      const color = status === 'COMPLETED' ? 'success' : status === 'FAILED' ? 'error' : status === 'IN_PROGRESS' ? 'warning' : 'neutral'
      return h('div', { class: 'flex items-center gap-2' }, [
        h(
          UBadge,
          {
            variant: 'soft',
            color,
            class: 'capitalize',
          },
          () => status.toLowerCase(),
        ),
        isActive &&
          h(
            UBadge,
            {
              variant: 'solid',
              color: 'primary',
              class: 'text-xs',
            },
            () => 'ACTIVE',
          ),
      ])
    },
  },
  {
    accessorKey: 'totalTasks',
    header: 'Nhiệm vụ',
  },
  {
    accessorKey: 'completedTasks',
    header: 'Hoàn thành',
  },
  {
    accessorKey: 'failedTasks',
    header: 'Thất bại',
  },
  {
    accessorKey: 'startTime',
    header: 'Thời gian bắt đầu',
    cell: ({ row }) => {
      return h('span', formatDate(row.original.startTime))
    },
  },
  {
    accessorKey: 'endTime',
    header: 'Thời gian kết thúc',
    cell: ({ row }) => {
      return h('span', row.original.endTime ? formatDate(row.original.endTime) : '—')
    },
  },
]
</script>

<template>
  <div class="container mx-auto px-2 sm:px-4 py-4 sm:py-6">
    <PageHeader title="Tất cả phiên" description="Xem tất cả các phiên giao hàng">
      <template #actions>
        <UButton
          variant="soft"
          color="primary"
          icon="i-heroicons-truck"
          size="sm"
          class="md:size-md"
          @click="router.push({ name: 'delivery-shippers' })"
        >
          <span class="hidden sm:inline">Shipper</span>
          <span class="sm:hidden">Shipper</span>
        </UButton>
        <UButton
          variant="soft"
          color="primary"
          icon="i-heroicons-calendar-days"
          size="sm"
          class="md:size-md"
          @click="router.push({ name: 'delivery-shift-calendar' })"
        >
          <span class="hidden sm:inline">Lịch ca làm việc</span>
          <span class="sm:hidden">Lịch</span>
        </UButton>
      </template>
    </PageHeader>

    <!-- Filters -->
    <div class="mb-4 space-y-4">
      <div class="flex items-center gap-4">
        <label class="text-sm font-medium whitespace-nowrap">Chọn Shipper:</label>
        <USelectMenu
          v-model="selectedShipperOption"
          :items="shipperOptions"
          value-key="value"
          placeholder="Tất cả shippers"
          class="flex-1 max-w-md"
          @update:model-value="handleShipperSelectionChange"
        />
        <UButton
          variant="outline"
          size="sm"
          icon="i-heroicons-arrow-path"
          @click="loadAllSessions"
        >
          Làm mới
        </UButton>
      </div>

      <div class="flex items-center justify-between">
        <div class="text-sm text-gray-600">
          <span>Tổng: <strong>{{ totalSessions }}</strong></span>
          <span class="ml-4">Đang hoạt động: <strong>{{ activeSessions }}</strong></span>
          <span class="ml-4">Đã kết thúc: <strong>{{ inactiveSessions }}</strong></span>
        </div>
        <UButton
          v-if="inactiveSessions > 0"
          :variant="showInactive ? 'solid' : 'outline'"
          size="sm"
          @click="showInactive = !showInactive"
        >
          {{ showInactive ? 'Ẩn' : 'Hiện' }} Đã kết thúc ({{ inactiveSessions }})
        </UButton>
      </div>
    </div>

    <!-- Sessions Table -->
    <div class="bg-white dark:bg-gray-900 rounded-lg shadow">
      <div v-if="sessionsLoading" class="flex justify-center py-12">
        <div class="text-gray-500">Đang tải...</div>
      </div>
      <div v-else-if="displayedSessions.length === 0" class="p-12 text-center text-gray-500">
        <p class="text-lg font-medium mb-2">Không có session nào</p>
        <p class="text-sm">
          {{ selectedShipperId ? 'Shipper này chưa có session nào.' : 'Chưa có session nào trong hệ thống.' }}
        </p>
      </div>
      <UTable
        v-else
        :data="displayedSessions"
        :columns="columns"
        :ui="{
          empty: 'text-center py-12',
          root: 'min-h-[400px]',
          thead: 'sticky top-0 bg-white dark:bg-gray-800',
        }"
      />
    </div>
  </div>
</template>
