<script setup lang="ts">
/**
 * Shipper Sessions Statistics Modal
 *
 * Displays session statistics for a shipper and provides redirect to All Sessions page
 */

import { onMounted, computed, ref, resolveComponent } from 'vue'
import { useRouter } from 'vue-router'
import { getDeliverySessions, getActiveSessionForDeliveryMan } from '../api'
import type { DeliveryManDto, DeliverySessionDto } from '../model.type'
import type { FilterGroup } from '@/common/types/filter'

const UButton = resolveComponent('UButton')
const UBadge = resolveComponent('UBadge')
const UCard = resolveComponent('UCard')

interface Props {
  shipper: DeliveryManDto
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [result: unknown] }>()

const router = useRouter()
const sessions = ref<DeliverySessionDto[]>([])
const sessionsLoading = ref(false)
const activeSessionId = ref<string | null>(null)

onMounted(async () => {
  await Promise.all([loadActiveSession(), loadAllSessions()])
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
    const filterGroup: FilterGroup = {
      logic: 'AND',
      conditions: [
        {
          field: 'deliveryManId',
          operator: 'eq',
          value: props.shipper.userId,
        },
      ],
    }

    const response = await getDeliverySessions({
      filters: filterGroup,
      page: 0,
      size: 100, // Load up to 100 sessions for statistics
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

// Statistics
const totalSessions = computed(() => sessions.value.length)
const activeSessions = computed(() => sessions.value.filter((session) => session.isActive).length)
const completedSessions = computed(() =>
  sessions.value.filter((session) => session.status === 'COMPLETED').length,
)
const failedSessions = computed(() =>
  sessions.value.filter((session) => session.status === 'FAILED').length,
)
const inProgressSessions = computed(() =>
  sessions.value.filter((session) => session.status === 'IN_PROGRESS').length,
)

const totalTasks = computed(() =>
  sessions.value.reduce((sum, session) => sum + (session.totalTasks || 0), 0),
)
const completedTasks = computed(() =>
  sessions.value.reduce((sum, session) => sum + (session.completedTasks || 0), 0),
)
const failedTasks = computed(() =>
  sessions.value.reduce((sum, session) => sum + (session.failedTasks || 0), 0),
)

const lastSessionStartTime = computed(() => {
  if (sessions.value.length === 0) return null
  const sorted = [...sessions.value].sort((a, b) => {
    if (!a.startTime) return 1
    if (!b.startTime) return -1
    return new Date(b.startTime).getTime() - new Date(a.startTime).getTime()
  })
  return sorted[0].startTime
})

const handleClose = () => {
  emit('close', null)
}

const navigateToAllSessions = () => {
  emit('close', null)
  // Navigate to All Sessions page with shipper filter
  router.push({
    name: 'delivery-sessions',
    query: {
      shipperId: props.shipper.id,
      userId: props.shipper.userId,
    },
  })
}

const formatDate = (value?: string | null) => {
  if (!value) return '—'
  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
</script>

<template>
  <UModal
    :title="`Session Statistics - ${shipper.displayName}`"
    :description="`Overview of delivery sessions for this shipper`"
    :close="{ onClick: handleClose }"
    :ui="{
      content: 'min-w-[600px] w-full md:min-w-none sm:min-w-none sm:max-w-md md:max-w-2xl',
      footer: 'justify-end w-full',
    }"
  >
    <template #body>
      <div v-if="sessionsLoading" class="flex justify-center py-12">
        <div class="text-gray-500">Loading statistics...</div>
      </div>

      <div v-else class="space-y-6">
        <!-- Shipper Info -->
        <div class="flex items-center justify-between text-sm text-gray-600 border-b pb-3">
          <div class="flex items-center gap-4">
            <span>Email: <strong>{{ shipper.email || 'N/A' }}</strong></span>
            <span>Phone: <strong>{{ shipper.phone || 'N/A' }}</strong></span>
          </div>
          <UBadge
            v-if="shipper.hasActiveSession"
            variant="solid"
            color="success"
            class="text-xs"
          >
            ACTIVE SESSION
          </UBadge>
        </div>

        <!-- Session Statistics -->
        <div class="grid grid-cols-2 gap-4">
          <UCard>
            <div class="p-4">
              <div class="text-sm text-gray-500 mb-1">Total Sessions</div>
              <div class="text-2xl font-bold">{{ totalSessions }}</div>
            </div>
          </UCard>

          <UCard>
            <div class="p-4">
              <div class="text-sm text-gray-500 mb-1">Active Sessions</div>
              <div class="text-2xl font-bold text-green-600">{{ activeSessions }}</div>
            </div>
          </UCard>

          <UCard>
            <div class="p-4">
              <div class="text-sm text-gray-500 mb-1">Completed</div>
              <div class="text-2xl font-bold text-blue-600">{{ completedSessions }}</div>
            </div>
          </UCard>

          <UCard>
            <div class="p-4">
              <div class="text-sm text-gray-500 mb-1">Failed</div>
              <div class="text-2xl font-bold text-red-600">{{ failedSessions }}</div>
            </div>
          </UCard>
        </div>

        <!-- Task Statistics -->
        <div class="grid grid-cols-3 gap-4">
          <UCard>
            <div class="p-4">
              <div class="text-sm text-gray-500 mb-1">Total Tasks</div>
              <div class="text-2xl font-bold">{{ totalTasks }}</div>
            </div>
          </UCard>

          <UCard>
            <div class="p-4">
              <div class="text-sm text-gray-500 mb-1">Completed Tasks</div>
              <div class="text-2xl font-bold text-green-600">{{ completedTasks }}</div>
            </div>
          </UCard>

          <UCard>
            <div class="p-4">
              <div class="text-sm text-gray-500 mb-1">Failed Tasks</div>
              <div class="text-2xl font-bold text-red-600">{{ failedTasks }}</div>
            </div>
          </UCard>
        </div>

        <!-- Last Session Info -->
        <UCard v-if="lastSessionStartTime">
          <div class="p-4">
            <div class="text-sm text-gray-500 mb-1">Last Session Started</div>
            <div class="text-lg font-medium">{{ formatDate(lastSessionStartTime) }}</div>
          </div>
        </UCard>

        <!-- Empty State -->
        <div v-if="totalSessions === 0" class="text-center py-8">
          <div class="text-gray-400 mb-2">No sessions found for this shipper</div>
          <div class="text-sm text-gray-500">This shipper has not started any delivery sessions yet.</div>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-between w-full">
        <UButton variant="ghost" color="neutral" @click="handleClose"> Close </UButton>
        <UButton
          variant="solid"
          color="primary"
          icon="i-heroicons-arrow-top-right-on-square"
          :disabled="totalSessions === 0"
          @click="navigateToAllSessions"
        >
          View All Sessions
        </UButton>
      </div>
    </template>
  </UModal>
</template>
