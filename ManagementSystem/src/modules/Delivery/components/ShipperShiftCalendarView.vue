<script setup lang="ts">
/**
 * Shipper Shift Calendar View
 *
 * Calendar table view showing shipper working schedule
 * X-axis: Hours (8:00 - 20:00)
 * Y-axis: Days of week (Monday - Sunday)
 */

import { onMounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { resolveComponent } from 'vue'
import { getDeliverySessions, getDeliveryMenV2 } from '../api'
import { DeliveryManDto } from '../model.type'
import type { DeliverySessionDto } from '../model.type'
import type { FilterGroup } from '@/common/types/filter'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { defineAsyncComponent } from 'vue'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))
const UButton = resolveComponent('UButton')
const UCard = resolveComponent('UCard')
const USelectMenu = resolveComponent('USelectMenu')

const route = useRoute()
const router = useRouter()
const toast = useToast()

// Week data
const currentWeekStart = ref<Date>(new Date())
const selectedShipperId = ref<string>(route.query.shipperId as string || '')
const selectedShipperUserId = ref<string>(route.query.shipperUserId as string || '')

// Data
const shippers = ref<DeliveryManDto[]>([])
const sessions = ref<DeliverySessionDto[]>([])
const loading = ref(false)

// Shipper options for USelectMenu
const shipperOptions = computed(() => {
  return [
    { label: '-- Chọn shipper --', value: '' },
    ...shippers.value.map((s) => ({
      label: `${s.displayName} (${s.email})`,
      value: s.id,
    })),
  ]
})

// Selected shipper option for USelectMenu
const selectedShipperOption = computed(() => {
  if (!selectedShipperId.value) return shipperOptions.value[0]
  return shipperOptions.value.find((opt) => opt.value === selectedShipperId.value) || shipperOptions.value[0]
})

// Hours range (8:00 AM to 8:00 PM)
const hours = Array.from({ length: 13 }, (_, i) => i + 8) // [8, 9, 10, ..., 20]
const daysOfWeek = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']
const daysOfWeekVi = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ nhật']

/**
 * Get start of week (Monday)
 */
const getWeekStart = (date: Date): Date => {
  const d = new Date(date)
  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1) // Adjust when day is Sunday
  return new Date(d.setDate(diff))
}

/**
 * Get date for a day in current week
 */
const getDateForDay = (dayIndex: number): Date => {
  const weekStart = getWeekStart(currentWeekStart.value)
  const date = new Date(weekStart)
  date.setDate(date.getDate() + dayIndex)
  return date
}

/**
 * Format date as YYYY-MM-DD
 */
const formatDateKey = (date: Date): string => {
  return date.toISOString().split('T')[0]
}

/**
 * Get all dates in current week
 */
const weekDates = computed(() => {
  return daysOfWeek.map((_, index) => getDateForDay(index))
})

/**
 * Get sessions for a specific day
 */
const getSessionsForDay = (date: Date): DeliverySessionDto[] => {
  const dateKey = formatDateKey(date)
  return sessions.value.filter((session) => {
    const sessionDate = session.startTime ? new Date(session.startTime) : null
    if (!sessionDate) return false
    const sessionDateKey = formatDateKey(sessionDate)
    return sessionDateKey === dateKey
  })
}

/**
 * Get sessions for a specific hour and day
 */
const getSessionsForHourAndDay = (hour: number, date: Date): DeliverySessionDto[] => {
  const daySessions = getSessionsForDay(date)
  return daySessions.filter((session) => {
    if (!session.startTime) return false
    const startHour = new Date(session.startTime).getHours()
    return startHour === hour || (startHour < hour && session.endTime && new Date(session.endTime).getHours() >= hour)
  })
}

/**
 * Check if session spans multiple hours
 */
const getSessionTimeRange = (session: DeliverySessionDto): { startHour: number; endHour: number } => {
  const start = session.startTime ? new Date(session.startTime) : null
  const end = session.endTime ? new Date(session.endTime) : null

  if (!start) return { startHour: 8, endHour: 8 }
  const startHour = start.getHours()
  const endHour = end ? end.getHours() : 20 // If no end time, assume it goes until end of day

  return { startHour, endHour }
}

/**
 * Calculate session position and width for grid
 */
const getSessionStyle = (session: DeliverySessionDto, hour: number, date: Date): string => {
  const { startHour, endHour } = getSessionTimeRange(session)
  const sessionDate = session.startTime ? new Date(session.startTime) : null
  if (!sessionDate || formatDateKey(sessionDate) !== formatDateKey(date)) return ''

  if (hour < startHour || (hour > endHour && session.endTime)) return ''

  // Calculate width (number of hours the session spans)
  const spanHours = Math.max(1, endHour - startHour + 1)
  const widthPercent = (1 / spanHours) * 100

  return `width: ${widthPercent}%;`
}

/**
 * Load shippers
 */
const loadShippers = async () => {
  try {
    const response = await getDeliveryMenV2({
      page: 0,
      size: 100,
    })
    if (response.result?.data) {
      shippers.value = response.result.data.map((s: unknown) => new DeliveryManDto(s as DeliveryManDto))
      // Auto-select shipper from query if available
      if (selectedShipperUserId.value && shippers.value.length > 0) {
        const shipper = shippers.value.find((s) => s.userId === selectedShipperUserId.value)
        if (shipper) {
          selectedShipperId.value = shipper.id
        }
      }
    }
  } catch (error: unknown) {
    toast.add({
      title: 'Lỗi tải danh sách shippers',
      description: error instanceof Error ? error.message : 'Không thể tải danh sách shippers',
      color: 'error',
    })
  }
}

/**
 * Load sessions for selected shipper and week
 */
const loadSessions = async () => {
  if (!selectedShipperId.value) {
    sessions.value = []
    return
  }

  const shipper = shippers.value.find((s) => s.id === selectedShipperId.value)
  if (!shipper) {
    sessions.value = []
    return
  }

  loading.value = true
  try {
    const weekStart = getWeekStart(currentWeekStart.value)
    const weekEnd = new Date(weekStart)
    weekEnd.setDate(weekEnd.getDate() + 7)

    const filterGroup: FilterGroup = {
      logic: 'AND',
      conditions: [
        {
          field: 'deliveryManId',
          operator: 'eq',
          value: shipper.userId,
          logic: undefined,
        },
        {
          field: 'startTime',
          operator: 'gte',
          value: weekStart.toISOString(),
          logic: 'AND',
        },
        {
          field: 'startTime',
          operator: 'lt',
          value: weekEnd.toISOString(),
          logic: undefined,
        },
      ],
    }

    const response = await getDeliverySessions({
      filters: filterGroup,
      page: 0,
      size: 100,
      sorts: [
        {
          field: 'startTime',
          direction: 'asc',
        },
      ],
    })

    if (response.result?.data) {
      sessions.value = response.result.data
    }
  } catch (error: unknown) {
    toast.add({
      title: 'Lỗi tải lịch làm việc',
      description: error instanceof Error ? error.message : 'Không thể tải lịch làm việc',
      color: 'error',
    })
  } finally {
    loading.value = false
  }
}

/**
 * Navigate to previous week
 */
const previousWeek = () => {
  currentWeekStart.value = new Date(currentWeekStart.value)
  currentWeekStart.value.setDate(currentWeekStart.value.getDate() - 7)
  loadSessions()
}

/**
 * Navigate to next week
 */
const nextWeek = () => {
  currentWeekStart.value = new Date(currentWeekStart.value)
  currentWeekStart.value.setDate(currentWeekStart.value.getDate() + 7)
  loadSessions()
}

/**
 * Navigate to current week
 */
const goToCurrentWeek = () => {
  currentWeekStart.value = new Date()
  loadSessions()
}

/**
 * Navigate to session detail
 */
const navigateToSession = (sessionId: string) => {
  router.push({ name: 'delivery-session-detail', params: { sessionId } })
}

/**
 * Format hour for display
 */
const formatHour = (hour: number): string => {
  return `${hour.toString().padStart(2, '0')}:00`
}

/**
 * Get session status color
 */
const getSessionColor = (status: string): string => {
  switch (status) {
    case 'IN_PROGRESS':
      return 'bg-blue-500'
    case 'COMPLETED':
      return 'bg-green-500'
    case 'FAILED':
      return 'bg-red-500'
    default:
      return 'bg-gray-500'
  }
}

/**
 * Get session status text color
 */
const getSessionTextColor = (status: string): string => {
  switch (status) {
    case 'IN_PROGRESS':
      return 'text-blue-100'
    case 'COMPLETED':
      return 'text-green-100'
    case 'FAILED':
      return 'text-red-100'
    default:
      return 'text-gray-100'
  }
}

/**
 * Handle shipper selection change from USelectMenu
 */
const handleShipperSelectionChange = (opt: { label: string; value: string } | null) => {
  selectedShipperId.value = opt?.value || ''
}

watch(selectedShipperId, () => {
  loadSessions()
})

onMounted(async () => {
  currentWeekStart.value = getWeekStart(new Date())
  await loadShippers()
  await loadSessions()
})
</script>

<template>
  <div class="container mx-auto px-2 sm:px-4 py-4 sm:py-6">
    <PageHeader title="Lịch Làm Việc Shipper">
      <template #description>
        Xem lịch làm việc của shippers theo tuần
      </template>
      <template #actions>
        <UButton
          variant="soft"
          color="primary"
          icon="i-heroicons-clipboard-document-list"
          size="sm"
          class="md:size-md"
          @click="router.push({ name: 'delivery-tasks' })"
        >
          <span class="hidden sm:inline">Task Management</span>
          <span class="sm:hidden">Tasks</span>
        </UButton>
        <UButton
          variant="soft"
          color="primary"
          icon="i-heroicons-truck"
          size="sm"
          class="md:size-md"
          @click="router.push({ name: 'delivery-shippers' })"
        >
          <span class="hidden sm:inline">Shippers</span>
          <span class="sm:hidden">Shippers</span>
        </UButton>
      </template>
    </PageHeader>

    <!-- Controls -->
    <UCard class="mb-4">
      <div class="p-4 space-y-4">
        <!-- Shipper Selection -->
        <div class="flex items-center gap-4">
          <label class="text-sm font-medium whitespace-nowrap">Chọn Shipper:</label>
          <USelectMenu
            v-model="selectedShipperOption"
            :items="shipperOptions"
            value-key="value"
            placeholder="-- Chọn shipper --"
            class="flex-1"
            @update:model-value="handleShipperSelectionChange"
          />
        </div>

        <!-- Week Navigation -->
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <UButton variant="outline" size="sm" @click="previousWeek"> ← Tuần trước </UButton>
            <UButton variant="outline" size="sm" @click="goToCurrentWeek"> Tuần này </UButton>
            <UButton variant="outline" size="sm" @click="nextWeek"> Tuần sau → </UButton>
          </div>
          <div class="text-sm font-medium">
            Tuần {{ formatDateKey(getWeekStart(currentWeekStart)) }} -
            {{ formatDateKey(new Date(getWeekStart(currentWeekStart).getTime() + 6 * 24 * 60 * 60 * 1000)) }}
          </div>
        </div>
      </div>
    </UCard>

    <!-- Calendar Table -->
    <UCard>
      <div v-if="loading" class="flex justify-center py-12">
        <div class="text-gray-500">Đang tải...</div>
      </div>
      <div v-else-if="!selectedShipperId" class="flex justify-center py-12">
        <div class="text-gray-500">Vui lòng chọn shipper để xem lịch làm việc</div>
      </div>
      <div v-else class="overflow-x-auto">
        <table class="w-full border-collapse">
          <thead>
            <tr>
              <th class="border border-gray-300 p-2 bg-gray-100 sticky left-0 z-10">Giờ / Ngày</th>
              <th
                v-for="(day, index) in daysOfWeek"
                :key="day"
                class="border border-gray-300 p-2 bg-gray-100 min-w-[120px]"
              >
                <div class="font-medium">{{ daysOfWeekVi[index] }}</div>
                <div class="text-xs text-gray-600">{{ formatDateKey(weekDates[index]) }}</div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="hour in hours" :key="hour">
              <td class="border border-gray-300 p-2 bg-gray-50 sticky left-0 z-10 font-medium">
                {{ formatHour(hour) }}
              </td>
              <td
                v-for="(day, dayIndex) in daysOfWeek"
                :key="day"
                class="border border-gray-300 p-1 relative h-16"
              >
                <div class="relative h-full">
                  <div
                    v-for="session in getSessionsForHourAndDay(hour, weekDates[dayIndex])"
                    :key="session.id"
                    :class="[
                      'absolute left-0 right-0 rounded p-1 text-xs cursor-pointer hover:opacity-80 z-20',
                      getSessionColor(session.status),
                      getSessionTextColor(session.status),
                    ]"
                    :style="getSessionStyle(session, hour, weekDates[dayIndex])"
                    @click="navigateToSession(session.id)"
                  >
                    <div class="font-medium truncate">{{ session.id.substring(0, 8) }}</div>
                    <div class="text-xs opacity-90">
                      {{ session.totalTasks }} tasks
                    </div>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Legend -->
      <div class="p-4 border-t">
        <div class="flex items-center gap-4 text-sm">
          <span class="font-medium">Chú thích:</span>
          <div class="flex items-center gap-2">
            <div class="w-4 h-4 bg-blue-500 rounded"></div>
            <span>Đang diễn ra (IN_PROGRESS)</span>
          </div>
          <div class="flex items-center gap-2">
            <div class="w-4 h-4 bg-green-500 rounded"></div>
            <span>Hoàn thành (COMPLETED)</span>
          </div>
          <div class="flex items-center gap-2">
            <div class="w-4 h-4 bg-red-500 rounded"></div>
            <span>Thất bại (FAILED)</span>
          </div>
        </div>
      </div>
    </UCard>
  </div>
</template>
