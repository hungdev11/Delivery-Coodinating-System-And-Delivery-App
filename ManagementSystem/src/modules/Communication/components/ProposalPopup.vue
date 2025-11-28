<script setup lang="ts">
/**
 * ProposalPopup Component
 *
 * Popup dialog to display proposal messages app-wide
 * Similar to ProposalPopupDialog in Android app
 */

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useGlobalChat } from '../composables/useGlobalChat'
import { useProposals } from '../composables/useProposals'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'
import type { MessageResponse } from '../model.type'

const router = useRouter()
const globalChat = useGlobalChat()
const { respond: respondToProposal } = useProposals()

const showPopup = ref(false)
const currentProposal = ref<MessageResponse | null>(null)
const autoDismissTimer = ref<number | null>(null)

/**
 * Handle proposal received
 */
const handleProposalReceived = (proposal: MessageResponse) => {
  console.log('[ProposalPopup] Proposal received:', proposal.id)

  // Close previous popup if any
  if (currentProposal.value) {
    closePopup()
  }

  currentProposal.value = proposal
  showPopup.value = true

  // Auto-dismiss after 30 seconds
  if (autoDismissTimer.value) {
    clearTimeout(autoDismissTimer.value)
  }
  autoDismissTimer.value = window.setTimeout(() => {
    if (showPopup.value) {
      closePopup()
    }
  }, 30000)
}

/**
 * Close popup
 */
const closePopup = () => {
  showPopup.value = false
  currentProposal.value = null
  if (autoDismissTimer.value) {
    clearTimeout(autoDismissTimer.value)
    autoDismissTimer.value = null
  }
}

/**
 * Handle proposal action
 */
const handleProposalAction = async (action: string) => {
  if (!currentProposal.value || !currentProposal.value.proposal) {
    closePopup()
    return
  }

  const proposalId = currentProposal.value.proposal.id

  // Get current user ID
  const currentUser = getCurrentUser()
  const currentUserId = currentUser?.id

  if (!currentUserId) {
    console.error('[ProposalPopup] No user ID available')
    closePopup()
    return
  }

  try {
    // Map action types
    let resultData = action

    // For text input proposals, might need additional data
    if (action === 'CONFIRM' && currentProposal.value.proposal.actionType === 'TEXT_INPUT') {
      // TODO: Show input modal for text proposals
      resultData = 'CONFIRMED'
    }

    await respondToProposal(proposalId, currentUserId, {
      resultData,
    })

    console.log('[ProposalPopup] Proposal response sent:', action)
    closePopup()

    // Navigate to conversation if needed
    if (currentProposal.value.conversationId) {
      router.push({
        name: 'communication-chat',
        params: { conversationId: currentProposal.value.conversationId },
      })
    }
  } catch (error) {
    console.error('[ProposalPopup] Error responding to proposal:', error)
    // Keep popup open on error
  }
}

/**
 * Parse proposal data
 */
const proposalTitle = computed(() => {
  if (!currentProposal.value?.proposal?.data) return 'Yêu cầu mới'

  try {
    const data =
      typeof currentProposal.value.proposal.data === 'string'
        ? JSON.parse(currentProposal.value.proposal.data)
        : currentProposal.value.proposal.data

    return data.title || 'Yêu cầu mới'
  } catch {
    return 'Yêu cầu mới'
  }
})

const proposalContent = computed(() => {
  if (currentProposal.value?.content) {
    return currentProposal.value.content
  }

  if (currentProposal.value?.proposal?.data) {
    try {
      const data =
        typeof currentProposal.value.proposal.data === 'string'
          ? JSON.parse(currentProposal.value.proposal.data)
          : currentProposal.value.proposal.data

      return data.content || ''
    } catch {
      return ''
    }
  }

  return ''
})

const proposalActionType = computed(() => {
  return currentProposal.value?.proposal?.actionType || 'ACCEPT_DECLINE'
})

const proposalSender = computed(() => {
  const senderId = currentProposal.value?.senderId || ''
  return senderId.substring(0, Math.min(8, senderId.length))
})

const proposalTime = computed(() => {
  const sentAt = currentProposal.value?.sentAt
  if (!sentAt) return ''

  try {
    const date = new Date(sentAt)
    const now = new Date()
    const diff = now.getTime() - date.getTime()

    // Less than 1 minute
    if (diff < 60000) return 'Vừa xong'

    // Less than 1 hour
    if (diff < 3600000) {
      const minutes = Math.floor(diff / 60000)
      return `${minutes} phút trước`
    }

    // Less than 24 hours
    if (diff < 86400000) {
      const hours = Math.floor(diff / 3600000)
      return `${hours} giờ trước`
    }

    // Format as date
    return date.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return sentAt.substring(0, 19)
  }
})

// Setup listener - use same reference for add/remove
const listener = {
  onProposalReceived: handleProposalReceived,
}

onMounted(() => {
  globalChat.addListener(listener)
})

// Cleanup on unmount
onUnmounted(() => {
  globalChat.removeListener(listener)
  if (autoDismissTimer.value) {
    clearTimeout(autoDismissTimer.value)
  }
})
</script>

<template>
  <UModal v-model:open="showPopup" :ui="{ content: 'sm:max-w-sm md:max-w-md' }">
    <template #header>
      <div class="flex items-center justify-between">
        <h3 class="text-lg font-semibold">{{ proposalTitle }}</h3>
        <UButton
          icon="i-heroicons-x-mark"
          variant="ghost"
          color="neutral"
          size="xs"
          @click="closePopup"
        />
      </div>
    </template>

    <template #body>
      <div class="space-y-4">
        <!-- Content -->
        <p v-if="proposalContent" class="text-sm text-gray-700">
          {{ proposalContent }}
        </p>

        <!-- Sender and time -->
        <div class="flex items-center text-xs text-gray-500">
          <span v-if="proposalSender">Từ: {{ proposalSender }}</span>
          <span v-if="proposalSender && proposalTime"> • </span>
          <span v-if="proposalTime">{{ proposalTime }}</span>
        </div>
      </div>
    </template>
    <template #footer>
      <!-- Action buttons -->
      <div class="flex items-center justify-end space-x-2 pt-2">
        <UButton
          v-if="proposalActionType === 'ACCEPT_DECLINE' || proposalActionType !== 'TEXT_INPUT'"
          color="success"
          @click="handleProposalAction(proposalActionType === 'TEXT_INPUT' ? 'CONFIRM' : 'ACCEPT')"
        >
          {{ proposalActionType === 'TEXT_INPUT' ? 'Xác nhận' : 'Chấp nhận' }}
        </UButton>
        <UButton
          v-if="proposalActionType === 'ACCEPT_DECLINE'"
          color="neutral"
          variant="outline"
          @click="handleProposalAction('DECLINE')"
        >
          Từ chối
        </UButton>
        <UButton color="neutral" variant="ghost" @click="closePopup"> Bỏ qua </UButton>
      </div>
    </template>
  </UModal>
</template>
