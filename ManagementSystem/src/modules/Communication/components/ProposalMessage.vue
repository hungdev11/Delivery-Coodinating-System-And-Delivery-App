<script setup lang="ts">
/**
 * Proposal Message Component
 *
 * Displays an interactive proposal message with response buttons
 */

import type { InteractiveProposalResponseDTO } from '../model.type'
import { computed } from 'vue';

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
  return (
    props.proposal.status === 'PENDING' &&
    props.proposal.recipientId === props.currentUserId
  )
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
  <div
    class="max-w-xs lg:max-w-md px-4 py-3 rounded-lg bg-white border-2"
    :class="getBorderColor"
  >
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
      <pre class="whitespace-pre-wrap break-words">{{
        JSON.stringify(parseProposalData(proposal.data), null, 2)
      }}</pre>
    </div>

    <!-- Proposal Response Buttons -->
    <div v-if="canRespond" class="flex space-x-2 mt-3">
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
