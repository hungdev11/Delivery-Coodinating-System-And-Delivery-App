<script setup lang="ts">
/**
 * Shipper Sessions Modal
 *
 * Displays all sessions of a given shipper in a modal using UModal
 */

import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useDeliverySessions } from '../composables'
import type { DeliveryManDto, DeliverySessionDto } from '../model.type'

interface Props {
  shipper: DeliveryManDto
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [result: unknown] }>()

const router = useRouter()
const { sessions, sessionsLoading, loadSessions } = useDeliverySessions()

onMounted(() => {
  loadSessions(props.shipper.id)
})

const totalSessions = computed(() => sessions.value.length)
const activeSessions = computed(() => sessions.value.filter((session) => session.isActive).length)

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
</script>

<template>
  <UModal
    :title="`Sessions for ${shipper.displayName}`"
    :description="`Total sessions: ${totalSessions}`"
    :close="{ onClick: handleClose }"
    :ui="{ footer: 'justify-end' }"
  >
    <template #body>
      <div class="space-y-4">
        <div class="flex items-center justify-between text-sm text-gray-500">
          <span>Email: <strong>{{ shipper.email || 'N/A' }}</strong></span>
          <span>Phone: <strong>{{ shipper.phone || 'N/A' }}</strong></span>
          <span>Active sessions: <strong>{{ activeSessions }}</strong></span>
        </div>

        <USkeleton v-if="sessionsLoading" class="h-48 w-full" />

        <div v-else>
          <UAlert
            v-if="sessions.length === 0"
            color="neutral"
            variant="soft"
            title="No sessions found"
            description="This shipper has no delivery sessions yet."
          />

          <UTable
            v-else
            :data="sessions"
            :columns="[
              { accessorKey: 'id', header: 'Session ID' },
              { accessorKey: 'status', header: 'Status' },
              { accessorKey: 'totalTasks', header: 'Tasks' },
              { accessorKey: 'completedTasks', header: 'Completed' },
              { accessorKey: 'failedTasks', header: 'Failed' },
              { accessorKey: 'startTime', header: 'Start Time' },
              { accessorKey: 'endTime', header: 'End Time' },
              { accessorKey: 'actions', header: 'Actions' },
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

            <template #cell(startTime)="{ row }">
              {{ formatDate(row.original.startTime) }}
            </template>

            <template #cell(endTime)="{ row }">
              {{ row.original.endTime ? formatDate(row.original.endTime) : '—' }}
            </template>

            <template #cell(actions)="{ row }">
              <UButton
                size="xs"
                variant="ghost"
                icon="i-heroicons-arrow-top-right-on-square"
                @click="navigateToSession(row.original)"
              >
                View detail
              </UButton>
            </template>
          </UTable>
        </div>
      </div>
    </template>

    <template #footer>
      <UButton variant="outline" color="neutral" @click="handleClose"> Close </UButton>
    </template>
  </UModal>
</template>
