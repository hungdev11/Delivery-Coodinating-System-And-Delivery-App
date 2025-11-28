<script setup lang="ts">
/**
 * Chat Message Component
 *
 * Displays a single chat message (text or proposal)
 * Shows session and delivery info for shipper messages
 */

import { ref, onMounted, computed } from 'vue'
import type { MessageResponse } from '../model.type'
import MessageStatusIndicator from './MessageStatusIndicator.vue'
import DeliveryCompletedMessage from './DeliveryCompletedMessage.vue'
import DeliverySucceededMessage from './DeliverySucceededMessage.vue'
import PostponeMessage from './PostponeMessage.vue'
import {
  getActiveSessionForDeliveryMan,
  getAssignmentsBySessionId,
  getDeliverySessions,
} from '../../Delivery/api'
import type { DeliveryAssignmentTask } from '../../Delivery/model.type'
import type { DeliverySessionDto } from '../../Delivery/model.type'
import { getUserRoles } from '@/common/guards/roleGuard.guard'
import type { FilterGroup } from '@/common/types/filter'

interface Props {
  message: MessageResponse
  isMyMessage: boolean
  currentUserId?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  confirmDelivery: [parcelId: string, messageId: string, note?: string]
}>()

/**
 * Handle delivery confirmation
 */
const handleDeliveryConfirm = (parcelId: string, messageId: string, note?: string) => {
  emit('confirmDelivery', parcelId, messageId, note)
}

// Session and assignments info for shipper messages
const activeSessionId = ref<string | null>(null)
const sessionAssignments = ref<DeliveryAssignmentTask[]>([])
const loadingSession = ref(false)

// All sessions for admin view
const allSessions = ref<DeliverySessionDto[]>([])
const loadingAllSessions = ref(false)
const showAllSessions = ref(false)

// Check if current user is admin
const isAdmin = computed(() => {
  const roles = getUserRoles()
  return roles.includes('ADMIN')
})

/**
 * Load session info if message is from shipper (not from current user)
 */
onMounted(async () => {
  if (!props.isMyMessage && props.message.senderId) {
    await loadShipperSessionInfo()
    // If admin, also load all sessions
    if (isAdmin.value) {
      await loadAllShipperSessions()
    }
  }
})

/**
 * Load active session and assignments for shipper
 */
const loadShipperSessionInfo = async () => {
  if (!props.message.senderId) return

  loadingSession.value = true
  try {
    const sessionResponse = await getActiveSessionForDeliveryMan(props.message.senderId)

    if (sessionResponse.result && sessionResponse.result.id) {
      activeSessionId.value = sessionResponse.result.id

      // Get assignments in this session
      const assignmentsResponse = await getAssignmentsBySessionId(activeSessionId.value, {
        page: 0,
        size: 100,
      })

      if (assignmentsResponse.content) {
        sessionAssignments.value = assignmentsResponse.content
      }
    }
  } catch (error) {
    console.error('Failed to load shipper session info:', error)
  } finally {
    loadingSession.value = false
  }
}

/**
 * Load all sessions for shipper (admin only)
 */
const loadAllShipperSessions = async () => {
  if (!props.message.senderId || !isAdmin.value) return

  loadingAllSessions.value = true
  try {
    // Query sessions by deliveryManId
    const filterGroup: FilterGroup = {
      logic: 'AND',
      conditions: [
        {
          field: 'deliveryManId',
          operator: 'eq',
          value: props.message.senderId,
          logic: 'AND',
        },
      ],
    }

    const response = await getDeliverySessions({
      filters: filterGroup,
      page: 0,
      size: 20, // Load last 20 sessions
      sorts: [
        {
          field: 'startTime',
          direction: 'desc',
        },
      ],
    })

    if (response.result?.data) {
      allSessions.value = response.result.data
    }
  } catch (error) {
    console.error('Failed to load all shipper sessions:', error)
  } finally {
    loadingAllSessions.value = false
  }
}

/**
 * Format message time
 */
const formatMessageTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}

/**
 * Check if message is delivery completed notification
 * Check both message.type and content.type for backward compatibility
 */
const isDeliveryCompletedMessage = computed(() => {
  // First check message.type (new way)
  if (props.message.type === 'DELIVERY_COMPLETED') {
    return true
  }

  // Fallback: check content.type (old way for backward compatibility)
  if (!props.message.content || typeof props.message.content !== 'string') {
    return false
  }
  try {
    const messageData = JSON.parse(props.message.content)
    return messageData && messageData.type === 'DELIVERY_COMPLETED'
  } catch {
    return false
  }
})

/**
 * Check if message is delivery succeeded notification
 */
const isDeliverySucceededMessage = computed(() => {
  // First check message.type (new way)
  if (props.message.type === 'DELIVERY_SUCCEEDED') {
    return true
  }

  // Fallback: check content.type (old way for backward compatibility)
  if (!props.message.content || typeof props.message.content !== 'string') {
    return false
  }
  try {
    const messageData = JSON.parse(props.message.content)
    return messageData && messageData.type === 'DELIVERY_SUCCEEDED'
  } catch {
    return false
  }
})

/**
 * Parse delivery completed message data
 */
const deliveryCompletedData = computed(() => {
  if (!isDeliveryCompletedMessage.value) {
    return null
  }
  try {
    return JSON.parse(props.message.content)
  } catch {
    return null
  }
})

/**
 * Parse delivery succeeded message data
 */
const deliverySucceededData = computed(() => {
  if (!isDeliverySucceededMessage.value) {
    return null
  }
  try {
    return JSON.parse(props.message.content)
  } catch {
    return null
  }
})

/**
 * Check if message is postpone notification
 * Postpone messages are sent as TEXT type with JSON content containing postpone info
 */
const isPostponeMessage = computed(() => {
  if (props.message.type !== 'TEXT') {
    return false
  }
  if (!props.message.content || typeof props.message.content !== 'string') {
    return false
  }
  try {
    const messageData = JSON.parse(props.message.content)
    // Check if content contains postpone-related fields
    return (
      messageData &&
      (messageData.postponeDateTime !== undefined ||
        (messageData.parcelId !== undefined && messageData.reason !== undefined))
    )
  } catch {
    return false
  }
})

/**
 * Parse postpone message data
 */
const postponeData = computed(() => {
  if (!isPostponeMessage.value) {
    return null
  }
  try {
    return JSON.parse(props.message.content)
  } catch {
    return null
  }
})
</script>

<template>
  <!-- Delivery Completed Message -->
  <div v-if="isDeliveryCompletedMessage && deliveryCompletedData" class="max-w-xs lg:max-w-md">
    <DeliveryCompletedMessage
      :message-data="deliveryCompletedData"
      :sent-at="message.sentAt"
      :current-user-id="props.currentUserId"
      :message-id="message.id"
      @confirm="handleDeliveryConfirm"
    />
  </div>

  <!-- Delivery Succeeded Message -->
  <div v-if="isDeliverySucceededMessage && deliverySucceededData" class="max-w-xs lg:max-w-md">
    <DeliverySucceededMessage
      :message-data="deliverySucceededData"
      :sent-at="message.sentAt"
    />
  </div>

  <!-- Postpone Message -->
  <div v-else-if="isPostponeMessage && postponeData" class="max-w-xs lg:max-w-md">
    <PostponeMessage :message-data="postponeData" :sent-at="message.sentAt" />
  </div>

  <!-- Regular Text Message -->
  <div v-else-if="message.type === 'TEXT'" class="max-w-xs lg:max-w-md">
    <!-- Message Content -->
    <div
      class="px-4 py-2 rounded-lg"
      :class="
        isMyMessage
          ? 'bg-primary-500 text-white'
          : 'bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 border border-gray-200 dark:border-gray-700'
      "
    >
      <p class="text-sm whitespace-pre-wrap break-words">{{ message.content }}</p>
      <div class="flex items-center justify-between mt-1 space-x-2">
        <p class="text-xs" :class="isMyMessage ? 'text-blue-100' : 'text-gray-500'">
          {{ formatMessageTime(message.sentAt) }}
        </p>
        <!-- Status indicator for my messages -->
        <MessageStatusIndicator
          v-if="isMyMessage && message.status"
          :status="message.status"
          size="xs"
        />
      </div>
    </div>

    <!-- Shipper Session Info (only for messages from shipper) -->
    <div
      v-if="!isMyMessage && (activeSessionId || (isAdmin && allSessions.length > 0))"
      class="mt-2 space-y-2"
    >
      <!-- Active Session -->
      <div
        v-if="activeSessionId && sessionAssignments.length > 0"
        class="px-3 py-2 bg-gray-50 dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700"
      >
        <div class="flex items-center justify-between mb-1">
          <p class="text-xs font-semibold text-gray-700 dark:text-gray-300">
            Active Delivery Session
          </p>
          <UBadge variant="soft" color="primary" size="xs">
            {{ sessionAssignments.length }} parcels
          </UBadge>
        </div>
        <div class="text-xs text-gray-600 dark:text-gray-400 space-y-1">
          <p>Session ID: {{ activeSessionId.substring(0, 8) }}...</p>
          <div class="max-h-20 overflow-y-auto">
            <div class="space-y-0.5">
              <div
                v-for="assignment in sessionAssignments.slice(0, 5)"
                :key="assignment.parcelId"
                class="flex items-center justify-between"
              >
                <span class="font-medium">{{
                  assignment.parcelCode || assignment.parcelId.substring(0, 8)
                }}</span>
                <UBadge
                  :color="
                    assignment.status === 'COMPLETED'
                      ? 'success'
                      : assignment.status === 'FAILED'
                        ? 'error'
                        : 'warning'
                  "
                  variant="subtle"
                  size="xs"
                >
                  {{ assignment.status }}
                </UBadge>
              </div>
              <p v-if="sessionAssignments.length > 5" class="text-gray-500 text-xs mt-1">
                +{{ sessionAssignments.length - 5 }} more parcels
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- All Sessions (Admin Only) -->
      <div
        v-if="isAdmin && allSessions.length > 0"
        class="px-3 py-2 bg-primary-50 dark:bg-primary-900/20 rounded-lg border border-primary-200 dark:border-primary-800"
      >
        <div class="flex items-center justify-between mb-2">
          <p class="text-xs font-semibold text-blue-700 dark:text-blue-300">
            All Delivery Sessions ({{ allSessions.length }})
          </p>
          <UButton
            variant="ghost"
            size="xs"
            :icon="showAllSessions ? 'i-heroicons-chevron-up' : 'i-heroicons-chevron-down'"
            @click="showAllSessions = !showAllSessions"
          >
            {{ showAllSessions ? 'Hide' : 'Show' }}
          </UButton>
        </div>
        <div v-if="showAllSessions" class="space-y-2 max-h-60 overflow-y-auto">
          <div
            v-for="session in allSessions"
            :key="session.id"
            class="px-2 py-1.5 bg-white dark:bg-gray-800 rounded border border-gray-200 dark:border-gray-700"
          >
            <div class="flex items-center justify-between mb-1">
              <div class="flex items-center space-x-2">
                <span class="text-xs font-medium text-gray-700 dark:text-gray-300">
                  {{ session.id.substring(0, 8) }}...
                </span>
                <UBadge
                  :color="
                    session.status === 'COMPLETED'
                      ? 'success'
                      : session.status === 'FAILED'
                        ? 'error'
                        : 'warning'
                  "
                  variant="soft"
                  size="xs"
                >
                  {{ session.status }}
                </UBadge>
              </div>
              <span class="text-xs text-gray-500"> {{ session.totalTasks }} tasks </span>
            </div>
            <div class="text-xs text-gray-500 space-y-0.5">
              <p>
                Started:
                {{ session.startTime ? new Date(session.startTime).toLocaleString() : 'N/A' }}
              </p>
              <p v-if="session.endTime">Ended: {{ new Date(session.endTime).toLocaleString() }}</p>
              <div class="flex items-center space-x-2 mt-1">
                <span class="text-emerald-600 dark:text-emerald-400">
                  ✓ {{ session.completedTasks }} completed
                </span>
                <span v-if="session.failedTasks > 0" class="text-rose-600 dark:text-rose-400">
                  ✗ {{ session.failedTasks }} failed
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
