<script setup lang="ts">
/**
 * Date Time Picker Modal Component
 *
 * Modal for selecting a single date and time using Nuxt UI Calendar
 */

import { CalendarDate, DateFormatter, getLocalTimeZone } from '@internationalized/date'
import { computed, ref, shallowRef } from 'vue'

interface Props {
  postponeType: 'SPECIFIC' | 'BEFORE' | 'AFTER' | null
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [result: { date: string; time: string } | null] }>()

const selectedDate = shallowRef<CalendarDate | null>(null)
const selectedTime = ref<string>('')
const showCalendar = ref(false)

const df = new DateFormatter('en-US', {
  dateStyle: 'medium',
})

/**
 * Get modal title based on postpone type
 */
const modalTitle = computed(() => {
  switch (props.postponeType) {
    case 'SPECIFIC':
      return 'Select Specific Date and Time'
    case 'BEFORE':
      return 'Select Date and Time (Before)'
    case 'AFTER':
      return 'Select Date and Time (After)'
    default:
      return 'Select Date and Time'
  }
})

/**
 * Format selected date for display
 */
const formattedDate = computed(() => {
  if (selectedDate.value) {
    return df.format(selectedDate.value.toDate(getLocalTimeZone()))
  }
  return 'Select a date'
})

/**
 * Handle confirm
 */
const handleConfirm = () => {
  if (selectedDate.value && selectedTime.value) {
    // Convert CalendarDate to YYYY-MM-DD format
    const year = selectedDate.value.year
    const month = String(selectedDate.value.month).padStart(2, '0')
    const day = String(selectedDate.value.day).padStart(2, '0')
    const dateString = `${year}-${month}-${day}`

    emit('close', { date: dateString, time: selectedTime.value })
  }
}

/**
 * Handle cancel
 */
const handleCancel = () => {
  emit('close', null)
}

// Initialize with today's date when component mounts
const today = new Date()
selectedDate.value = new CalendarDate(today.getFullYear(), today.getMonth() + 1, today.getDate())
</script>

<template>
  <UModal :title="modalTitle">
    <template #body>
      <div class="space-y-4 p-4">
        <div>
          <label class="block text-sm font-medium mb-2">Date</label>
          <UPopover v-model:open="showCalendar">
            <UButton
              color="neutral"
              variant="outline"
              block
              icon="i-heroicons-calendar"
              :label="formattedDate"
            />
            <template #panel>
              <UCalendar v-model="selectedDate" class="p-2" />
            </template>
          </UPopover>
        </div>
        <div>
          <label class="block text-sm font-medium mb-2">Time</label>
          <UInput v-model="selectedTime" type="time" />
        </div>
        <div class="flex justify-end space-x-2">
          <UButton variant="ghost" @click="handleCancel"> Cancel </UButton>
          <UButton @click="handleConfirm"> Confirm </UButton>
        </div>
      </div>
    </template>
  </UModal>
</template>
