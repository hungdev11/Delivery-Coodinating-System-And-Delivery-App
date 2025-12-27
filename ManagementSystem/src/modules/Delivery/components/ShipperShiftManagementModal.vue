<script setup lang="ts">
/**
 * Shipper Shift Management Modal
 *
 * Modal for managing shipper working shifts (sessions)
 */

import { onMounted, computed, ref, h, resolveComponent } from 'vue'
import { useRouter } from 'vue-router'
import { CalendarDate } from '@internationalized/date'
import { getDeliverySessions, getActiveSessionForDeliveryMan } from '../api'
import type { DeliveryManDto, DeliverySessionDto } from '../model.type'
import type { FilterGroup, FilterCondition } from '@/common/types/filter'
import type { TableColumn } from '@nuxt/ui'

const UButton = resolveComponent('UButton')
const UBadge = resolveComponent('UBadge')
const UInputDate = resolveComponent('UInputDate')

interface Props {
  shipper: DeliveryManDto
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [result: unknown] }>()

const router = useRouter()
const sessions = ref<DeliverySessionDto[]>([])
const sessionsLoading = ref(false)
const showInactive = ref(false)
const activeSessionId = ref<string | null>(null)
const dateFilter = ref<CalendarDate | null>(null) // CalendarDate from @internationalized/date

onMounted(async () => {
  await loadActiveSession()
  await loadAllSessions()
})

const loadActiveSession = async () => {
  try {
    const response = await getActiveSessionForDeliveryMan(props.shipper.userId)
    if (response.result?.id) {
      activeSessionId.value = response.result.id
    }
  } catch {
    console.debug('No active session found for shipper:', props.shipper.userId)
    activeSessionId.value = null
  }
}

const loadAllSessions = async () => {
  sessionsLoading.value = true
  try {
    const conditions: (FilterCondition | FilterGroup)[] = [
      {
        field: 'deliveryManId',
        operator: 'eq' as const,
        value: props.shipper.userId,
      },
    ]

    if (dateFilter.value) {
      // Convert CalendarDate to YYYY-MM-DD string
      const dateStr = `${dateFilter.value.year}-${String(dateFilter.value.month).padStart(2, '0')}-${String(dateFilter.value.day).padStart(2, '0')}`
      conditions.push({
        field: 'startTime',
        operator: 'gte' as const,
        value: `${dateStr}T00:00:00`,
        logic: 'AND' as const,
      })
      conditions.push({
        field: 'startTime',
        operator: 'lt' as const,
        value: `${dateStr}T23:59:59`,
      })
    }

    const filterGroup: FilterGroup = {
      logic: 'AND',
      conditions,
    }

    const response = await getDeliverySessions({
      filters: filterGroup,
      page: 0,
      size: 100,
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

const handleDateFilterChange = () => {
  loadAllSessions()
}

const totalSessions = computed(() => sessions.value.length)
const activeSessions = computed(() => sessions.value.filter((session) => session.isActive).length)
const inactiveSessions = computed(
  () => sessions.value.filter((session) => !session.isActive).length,
)

const displayedSessions = computed(() => {
  let filtered = sessions.value
  if (!showInactive.value) {
    filtered = filtered.filter((session) => session.isActive)
  }
  return filtered
})

const handleClose = () => {
  emit('close', null)
}

const navigateToSession = (session: DeliverySessionDto) => {
  emit('close', null)
  router.push({ name: 'delivery-session-detail', params: { sessionId: session.id } })
}

const navigateToCalendar = () => {
  emit('close', null)
  router.push({
    name: 'delivery-shift-calendar',
    query: { shipperId: props.shipper.id, shipperUserId: props.shipper.userId },
  })
}

const formatDate = (value?: string | null) => {
  if (!value) return '—'
  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

const formatDuration = (startTime?: string, endTime?: string | null) => {
  if (!startTime) return '—'
  if (!endTime) return 'Đang diễn ra'
  const start = new Date(startTime)
  const end = new Date(endTime)
  const diffMs = end.getTime() - start.getTime()
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60))
  return `${diffHours}h ${diffMinutes}m`
}

// Table columns configuration
const columns: TableColumn<DeliverySessionDto>[] = [
  {
    accessorKey: 'actions',
    header: 'Actions',
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
        () => 'View detail',
      )
    },
  },
  {
    accessorKey: 'id',
    header: 'Session ID',
    cell: ({ row }) => {
      const session = row.original
      return h('span', { class: 'font-mono text-xs' }, session.id.substring(0, 8) + '...')
    },
  },
  {
    accessorKey: 'status',
    header: 'Status',
    cell: ({ row }) => {
      const session = row.original
      const status = session.status
      const isActive = activeSessionId.value === session.id
      const color =
        status === 'COMPLETED' ? 'success' : status === 'FAILED' ? 'error' : 'warning'
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
    accessorKey: 'startTime',
    header: 'Bắt đầu',
    cell: ({ row }) => {
      return h('span', formatDate(row.original.startTime))
    },
  },
  {
    accessorKey: 'endTime',
    header: 'Kết thúc',
    cell: ({ row }) => {
      return h('span', row.original.endTime ? formatDate(row.original.endTime) : '—')
    },
  },
  {
    accessorKey: 'duration',
    header: 'Thời gian',
    cell: ({ row }) => {
      return h('span', formatDuration(row.original.startTime, row.original.endTime))
    },
  },
  {
    accessorKey: 'totalTasks',
    header: 'Tasks',
  },
  {
    accessorKey: 'completedTasks',
    header: 'Hoàn thành',
  },
  {
    accessorKey: 'failedTasks',
    header: 'Thất bại',
  },
]
</script>

<template>
  <UModal
    :title="`Quản lý ca làm việc - ${shipper.displayName}`"
    :description="`Tổng số ca: ${totalSessions} | Đang hoạt động: ${activeSessions}`"
    :close="{ onClick: handleClose }"
    :ui="{
      content: 'min-w-[1200px] w-full md:min-w-none sm:min-w-none sm:max-w-md md:max-w-7xl',
      footer: 'justify-end w-full',
    }"
  >
    <template #body>
      <div class="space-y-4">
        <!-- Filters and Actions -->
        <div class="flex items-center justify-between gap-4">
          <div class="flex items-center gap-2">
            <label class="text-sm font-medium">Lọc theo ngày:</label>
            <UInputDate
              v-model="dateFilter"
              placeholder="Chọn ngày"
              @update:model-value="handleDateFilterChange"
            />
            <UButton variant="ghost" size="sm" @click="dateFilter = null; handleDateFilterChange()">
              Xóa filter
            </UButton>
          </div>
          <div class="flex gap-2">
            <UButton
              variant="outline"
              icon="i-heroicons-calendar-days"
              @click="navigateToCalendar"
            >
              Xem Lịch
            </UButton>
            <UButton
              :variant="showInactive ? 'solid' : 'outline'"
              size="sm"
              @click="showInactive = !showInactive"
            >
              {{ showInactive ? 'Ẩn' : 'Hiện' }} Đã kết thúc ({{ inactiveSessions }})
            </UButton>
          </div>
        </div>

        <!-- Shipper Info -->
        <div class="flex items-center justify-between rounded-lg bg-gray-50 p-3 text-sm text-gray-600">
          <span>Email: <strong>{{ shipper.email || 'N/A' }}</strong></span>
          <span>Phone: <strong>{{ shipper.phone || 'N/A' }}</strong></span>
          <span>Vehicle: <strong>{{ shipper.vehicleType || 'N/A' }}</strong></span>
          <span>Active: <strong>{{ activeSessions }}</strong> / Total: <strong>{{ totalSessions }}</strong></span>
        </div>

        <USkeleton v-if="sessionsLoading" class="h-48 w-full" />

        <div v-else>
          <UAlert
            v-if="displayedSessions.length === 0"
            color="neutral"
            variant="soft"
            title="Không tìm thấy ca làm việc"
            :description="showInactive ? 'Shipper này chưa có ca làm việc nào.' : 'Không có ca đang hoạt động. Click Hiện Đã kết thúc để xem lịch sử.'"
          />

          <UTable
            v-else
            :data="displayedSessions"
            :columns="columns"
            :ui="{
              empty: 'text-center py-12',
              root: 'h-[50vh]',
              thead: 'sticky top-0 bg-white dark:bg-gray-800',
            }"
          />
        </div>
      </div>
    </template>

    <template #footer>
      <UButton variant="outline" color="neutral" @click="handleClose"> Đóng </UButton>
    </template>
  </UModal>
</template>
