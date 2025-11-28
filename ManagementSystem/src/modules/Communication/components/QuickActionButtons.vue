<script setup lang="ts">
/**
 * QuickActionButtons Component
 *
 * Quick action buttons for proposals (Accept/Reject/Postpone)
 * Enables 2-touch interaction for shippers
 */

import { ref } from 'vue'

interface Props {
  proposalId: string
  currentStatus?: string
  disabled?: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  accept: []
  reject: []
  postpone: [data: { note?: string; postponeWindowStart?: string; postponeWindowEnd?: string }]
}>()

const loading = ref(false)

/**
 * Handle Accept action (1-touch)
 */
const handleAccept = async () => {
  if (loading.value || props.disabled) return

  loading.value = true
  try {
    emit('accept')
  } finally {
    loading.value = false
  }
}

/**
 * Handle Reject action (1-touch)
 */
const handleReject = async () => {
  if (loading.value || props.disabled) return

  loading.value = true
  try {
    emit('reject')
  } finally {
    loading.value = false
  }
}

/**
 * Handle Postpone action (2-touch: select time -> confirm)
 */
const handlePostpone = async () => {
  if (loading.value || props.disabled) return

  // TODO: Open modal for time selection
  // For now, emit with default values
  loading.value = true
  try {
    emit('postpone', {
      note: 'Postponed via quick action',
    })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex items-center space-x-2">
    <!-- Accept Button -->
    <UButton
      color="success"
      variant="soft"
      size="xs"
      icon="i-heroicons-check"
      :loading="loading"
      :disabled="disabled"
      @click="handleAccept"
    >
      Accept
    </UButton>

    <!-- Reject Button -->
    <UButton
      color="error"
      variant="soft"
      size="xs"
      icon="i-heroicons-x-mark"
      :loading="loading"
      :disabled="disabled"
      @click="handleReject"
    >
      Reject
    </UButton>

    <!-- Postpone Button -->
    <UButton
      color="warning"
      variant="soft"
      size="xs"
      icon="i-heroicons-clock"
      :loading="loading"
      :disabled="disabled"
      @click="handlePostpone"
    >
      Postpone
    </UButton>
  </div>
</template>
