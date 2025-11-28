<script setup lang="ts">
/**
 * Shipper Sessions Modal
 *
 * Displays all sessions of a given shipper in a modal using UModal
 */

import { onMounted, computed, ref, h, resolveComponent } from 'vue'
import { useRouter } from 'vue-router'
import { getDeliverySessions } from '../api'
import type { DeliveryManDto, DeliverySessionDto } from '../model.type'
import type { FilterGroup } from '@/common/types/filter'
import type { TableColumn } from '@nuxt/ui'

const UButton = resolveComponent('UButton')
const UBadge = resolveComponent('UBadge')

interface Props {
  shipper: DeliveryManDto
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [result: unknown] }>()

const router = useRouter()
const sessions = ref<DeliverySessionDto[]>([])
const sessionsLoading = ref(false)
const showInactive = ref(false)

onMounted(() => {
  loadAllSessions()
})

const loadAllSessions = async () => {
  sessionsLoading.value = true
  try {
    // Query all sessions for this shipper
    // Note: deliveryManId in database is actually userId (Keycloak ID), not deliveryMan.id
    const filterGroup: FilterGroup = {
      logic: 'AND',
      conditions: [
        {
          field: 'deliveryManId',
          operator: 'eq',
          value: props.shipper.userId,
          logic: undefined,
        },
      ],
    }

    const response = await getDeliverySessions({
      filters: filterGroup,
      page: 0,
      size: 100, // Load up to 100 sessions
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

const totalSessions = computed(() => sessions.value.length)
const activeSessions = computed(() => sessions.value.filter((session) => session.isActive).length)
const inactiveSessions = computed(
  () => sessions.value.filter((session) => !session.isActive).length,
)

const displayedSessions = computed(() => {
  if (showInactive.value) {
    return sessions.value
  }
  return sessions.value.filter((session) => session.isActive)
})

const handleClose = () => {
  emit('close', null)
}

const navigateToSession = (session: DeliverySessionDto) => {
  emit('close', null)
  router.push({ name: 'delivery-session-detail', params: { sessionId: session.id } })
}

const formatDate = (value?: string | null) => {
  if (!value) return '—'
  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

// Table columns configuration (defined after functions to avoid hoisting issues)
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
  },
  {
    accessorKey: 'status',
    header: 'Status',
    cell: ({ row }) => {
      const status = row.original.status
      const color = status === 'COMPLETED' ? 'success' : status === 'FAILED' ? 'error' : 'warning'
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
    accessorKey: 'totalTasks',
    header: 'Tasks',
  },
  {
    accessorKey: 'completedTasks',
    header: 'Completed',
  },
  {
    accessorKey: 'failedTasks',
    header: 'Failed',
  },
  {
    accessorKey: 'startTime',
    header: 'Start Time',
    cell: ({ row }) => {
      return h('span', formatDate(row.original.startTime))
    },
  },
  {
    accessorKey: 'endTime',
    header: 'End Time',
    cell: ({ row }) => {
      return h('span', row.original.endTime ? formatDate(row.original.endTime) : '—')
    },
  },
]
</script>

<template>
  <UModal
    :title="`Sessions for ${shipper.displayName}`"
    :description="`Total sessions: ${totalSessions}`"
    :close="{ onClick: handleClose }"
    :ui="{
      content: 'min-w-[100vh] md:min-w-none sm:min-w-none sm:max-w-md md:max-w-lg',
      footer: 'justify-end',
    }"
  >
    <template #body>
      <div class="space-y-4">
        <div class="flex items-center justify-between text-sm text-gray-500">
          <span
            >Email: <strong>{{ shipper.email || 'N/A' }}</strong></span
          >
          <span
            >Phone: <strong>{{ shipper.phone || 'N/A' }}</strong></span
          >
          <span
            >Active: <strong>{{ activeSessions }}</strong> / Total:
            <strong>{{ totalSessions }}</strong></span
          >
        </div>

        <div v-if="inactiveSessions > 0" class="flex justify-end">
          <UButton
            :variant="showInactive ? 'solid' : 'outline'"
            size="sm"
            @click="showInactive = !showInactive"
          >
            {{ showInactive ? 'Hide' : 'Show' }} Inactive ({{ inactiveSessions }})
          </UButton>
        </div>

        <USkeleton v-if="sessionsLoading" class="h-48 w-full" />

        <div v-else>
          <UAlert
            v-if="displayedSessions.length === 0"
            color="neutral"
            variant="soft"
            title="No sessions found"
            :description="
              showInactive
                ? 'This shipper has no delivery sessions yet.'
                : 'No active sessions. Click Show Inactive to view completed sessions.'
            "
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
      <UButton variant="outline" color="neutral" @click="handleClose"> Close </UButton>
    </template>
  </UModal>
</template>
