<script setup lang="ts">
/**
 * Proposal Message Component
 *
 * Displays an interactive proposal message with response buttons
 */

import type { InteractiveProposalResponseDTO } from '../model.type'
import { computed, ref, onMounted } from 'vue'
import { getAssignmentsBySessionId } from '../../Delivery/api'
import type { DeliveryAssignmentTask } from '../../Delivery/model.type'

interface Props {
  proposal: InteractiveProposalResponseDTO
  content: string
  sentAt: string
  currentUserId: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  respond: [proposalId: string, resultData: string]
}>()

// Affected parcels from session (only parcels of the client who sent the proposal)
const affectedAssignments = ref<DeliveryAssignmentTask[]>([])
const loadingAssignments = ref(false)

/**
 * Load assignments if proposal has sessionId
 * Only loads assignments where receiverId = proposerId (client's parcels)
 */
onMounted(async () => {
  if (props.proposal.sessionId && props.proposal.proposerId) {
    loadingAssignments.value = true
    try {
      const response = await getAssignmentsBySessionId(props.proposal.sessionId, {
        page: 0,
        size: 100,
      })
      if (response.content) {
        // Filter: only assignments where receiverId = proposerId (client who sent the proposal)
        affectedAssignments.value = response.content.filter(
          (assignment: DeliveryAssignmentTask) =>
            assignment.receiverId === props.proposal.proposerId,
        )
      }
    } catch (error) {
      console.error('Failed to load affected assignments:', error)
    } finally {
      loadingAssignments.value = false
    }
  }
})

/**
 * Format message time
 */
const formatMessageTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}

/**
 * Check if proposal can be responded to
 */
const canRespond = computed(() => {
  return props.proposal.status === 'PENDING' && props.proposal.recipientId === props.currentUserId
})

/**
 * Parse proposal data
 */
const parseProposalData = (data: string) => {
  try {
    return JSON.parse(data)
  } catch {
    return {}
  }
}

/**
 * Handle proposal response
 */
const handleResponse = (resultData: string) => {
  emit('respond', props.proposal.id, resultData)
}

/**
 * Get badge color based on status
 */
const getStatusColor = computed(() => {
  switch (props.proposal.status) {
    case 'ACCEPTED':
      return 'success'
    case 'REJECTED':
      return 'error'
    default:
      return 'warning'
  }
})

/**
 * Get border color based on status
 */
const getBorderColor = computed(() => {
  switch (props.proposal.status) {
    case 'ACCEPTED':
      return 'border-green-500'
    case 'REJECTED':
      return 'border-red-500'
    default:
      return 'border-yellow-500'
  }
})
</script>

<template>
  <div class="max-w-xs lg:max-w-md px-4 py-3 rounded-lg bg-white border-2" :class="getBorderColor">
    <div class="flex items-start justify-between mb-2">
      <div class="flex-1">
        <p class="text-sm font-semibold text-gray-900">
          {{ proposal.type }}
        </p>
        <p class="text-xs text-gray-600 mt-1">{{ content }}</p>
      </div>
      <UBadge :color="getStatusColor" variant="subtle" size="xs">
        {{ proposal.status }}
      </UBadge>
    </div>

    <!-- Proposal Data Display -->
    <div v-if="proposal.data" class="text-xs text-gray-500 mb-2">
      <div v-if="proposal.type === 'DISPUTE_APPEAL' || proposal.type === 'STATUS_CHANGE_NOTIFICATION'">
        <div v-for="(value, key) in parseProposalData(proposal.data)" :key="key" class="mb-1">
          <span class="font-medium">{{ key }}:</span>
          <span class="ml-1">{{ value }}</span>
        </div>
      </div>
      <pre v-else class="whitespace-pre-wrap break-words">{{
        JSON.stringify(parseProposalData(proposal.data), null, 2)
      }}</pre>
    </div>

    <!-- Proposal Response Buttons -->
    <div v-if="canRespond || (proposal.type === 'DISPUTE_APPEAL' && proposal.proposerId === currentUserId)" class="flex space-x-2 mt-3">
      <!-- ACCEPT_DECLINE action type -->
      <UButton
        v-if="proposal.actionType === 'ACCEPT_DECLINE'"
        size="xs"
        color="success"
        @click="handleResponse('ACCEPTED')"
      >
        Accept
      </UButton>
      <UButton
        v-if="proposal.actionType === 'ACCEPT_DECLINE'"
        size="xs"
        color="error"
        variant="outline"
        @click="handleResponse('REJECTED')"
      >
        Decline
      </UButton>

      <!-- TEXT_INPUT action type (e.g., "Xác nhận đã nhận đơn") -->
      <UButton
        v-if="proposal.actionType === 'TEXT_INPUT'"
        size="xs"
        color="success"
        @click="handleResponse('CONFIRMED')"
      >
        Xác nhận đã nhận đơn
      </UButton>

      <!-- DISPUTE_APPEAL type - Shipper can appeal with evidence (only if recipient) -->
      <UButton
        v-if="proposal.type === 'DISPUTE_APPEAL' && proposal.recipientId === currentUserId"
        size="xs"
        color="primary"
        @click="handleResponse('APPEALED')"
      >
        Kháng cáo với bằng chứng
      </UButton>

      <!-- DISPUTE_APPEAL type - Client view (read-only info) -->
      <div
        v-if="proposal.type === 'DISPUTE_APPEAL' && proposal.proposerId === currentUserId"
        class="text-xs text-gray-500 italic"
      >
        Bạn đã tạo tranh chấp. Shipper có thể kháng cáo với bằng chứng.
      </div>

      <!-- STATUS_CHANGE_NOTIFICATION type - Read-only notification -->
      <div v-if="proposal.type === 'STATUS_CHANGE_NOTIFICATION'" class="text-xs text-gray-500 italic">
        Đây là thông báo, không cần phản hồi
      </div>
    </div>

    <!-- Affected Parcels Display (if proposal has sessionId) -->
    <div
      v-if="proposal.sessionId && affectedAssignments.length > 0"
      class="mt-2 pt-2 border-t border-gray-200 text-xs"
    >
      <p class="font-medium text-gray-700 mb-1">
        Affected Parcels ({{ affectedAssignments.length }}):
      </p>
      <div class="space-y-1 max-h-32 overflow-y-auto">
        <div
          v-for="assignment in affectedAssignments"
          :key="assignment.parcelId"
          class="text-gray-600 bg-gray-50 px-2 py-1 rounded"
        >
          <span class="font-medium">{{ assignment.parcelCode || assignment.parcelId }}</span>
          <span class="text-gray-500 ml-2">- {{ assignment.status }}</span>
        </div>
      </div>
    </div>

    <!-- Result Data Display -->
    <div
      v-if="proposal.resultData"
      class="mt-2 pt-2 border-t border-gray-200 text-xs text-gray-600"
    >
      <p class="font-medium">Response:</p>
      <p>{{ proposal.resultData }}</p>
    </div>

    <p class="text-xs text-gray-400 mt-2">
      {{ formatMessageTime(sentAt) }}
    </p>
  </div>
</template>
