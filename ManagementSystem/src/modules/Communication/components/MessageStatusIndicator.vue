<script setup lang="ts">
/**
 * MessageStatusIndicator Component
 *
 * Displays message status icons (SENT, DELIVERED, READ)
 */

import { computed } from 'vue'

interface Props {
  status?: 'SENT' | 'DELIVERED' | 'READ'
  size?: 'xs' | 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  status: 'SENT',
  size: 'sm',
})

const statusIcon = computed(() => {
  switch (props.status) {
    case 'SENT':
      return 'i-heroicons-check'
    case 'DELIVERED':
      return 'i-heroicons-check-badge'
    case 'READ':
      return 'i-heroicons-check-badge-solid'
    default:
      return 'i-heroicons-clock'
  }
})

const statusColor = computed(() => {
  switch (props.status) {
    case 'SENT':
      return 'text-gray-400'
    case 'DELIVERED':
      return 'text-blue-500'
    case 'READ':
      return 'text-green-500'
    default:
      return 'text-gray-300'
  }
})

const statusTooltip = computed(() => {
  switch (props.status) {
    case 'SENT':
      return 'Sent'
    case 'DELIVERED':
      return 'Delivered'
    case 'READ':
      return 'Read'
    default:
      return 'Pending'
  }
})

const iconSize = computed(() => {
  switch (props.size) {
    case 'xs':
      return 'w-3 h-3'
    case 'sm':
      return 'w-4 h-4'
    case 'md':
      return 'w-5 h-5'
    case 'lg':
      return 'w-6 h-6'
    default:
      return 'w-4 h-4'
  }
})
</script>

<template>
  <UTooltip :text="statusTooltip">
    <UIcon :name="statusIcon" :class="[statusColor, iconSize]" class="flex-shrink-0" />
  </UTooltip>
</template>
