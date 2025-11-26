<script setup lang="ts">
/**
 * Date Time Range Picker Modal Component
 *
 * Modal for selecting a date and time range using Nuxt UI Calendar
 */

import { CalendarDate, DateFormatter, getLocalTimeZone } from '@internationalized/date'
import { computed, ref, shallowRef } from 'vue'

const emit = defineEmits<{
  close: [result: { startDate: string; startTime: string; endDate: string; endTime: string } | null]
}>()

const dateRange = shallowRef<{
  start?: CalendarDate
  end?: CalendarDate
}>({})

const startTime = ref<string>('')
const endTime = ref<string>('')
const showCalendar = ref(false)

const df = new DateFormatter('en-US', {
  dateStyle: 'medium',
})

/**
 * Format date range for display
 */
const formattedDateRange = computed(() => {
  if (dateRange.value.start) {
    if (dateRange.value.end) {
      return `${df.format(dateRange.value.start.toDate(getLocalTimeZone()))} - ${df.format(dateRange.value.end.toDate(getLocalTimeZone()))}`
    }
    return df.format(dateRange.value.start.toDate(getLocalTimeZone()))
  }
  return 'Pick a date range'
})

/**
 * Handle confirm
 */
const handleConfirm = () => {
  if (dateRange.value.start && dateRange.value.end && startTime.value && endTime.value) {
    // Convert CalendarDate to YYYY-MM-DD format
    const formatCalendarDate = (date: CalendarDate) => {
      const year = date.year
      const month = String(date.month).padStart(2, '0')
      const day = String(date.day).padStart(2, '0')
      return `${year}-${month}-${day}`
    }

    const startDateString = formatCalendarDate(dateRange.value.start)
    const endDateString = formatCalendarDate(dateRange.value.end)

    // Validate time range
    const [startHours, startMinutes] = startTime.value.split(':')
    const [endHours, endMinutes] = endTime.value.split(':')

    const startDateTime = new Date(startDateString)
    startDateTime.setHours(parseInt(startHours), parseInt(startMinutes))

    const endDateTime = new Date(endDateString)
    endDateTime.setHours(parseInt(endHours), parseInt(endMinutes))

    if (endDateTime <= startDateTime) {
      alert('End time must be after start time')
      return
    }

    emit('close', {
      startDate: startDateString,
      startTime: startTime.value,
      endDate: endDateString,
      endTime: endTime.value,
    })
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
const todayCalendarDate = new CalendarDate(today.getFullYear(), today.getMonth() + 1, today.getDate())
dateRange.value = {
  start: todayCalendarDate,
}
</script>

<template>
  <UModal title="Select Date and Time Range">
    <template #body>
      <div class="space-y-4 p-4">
        <div>
          <label class="block text-sm font-medium mb-2">Date Range</label>
          <UPopover v-model:open="showCalendar">
            <UButton
              color="neutral"
              variant="outline"
              block
              icon="i-heroicons-calendar"
              :label="formattedDateRange"
            />
            <template #panel>
              <UCalendar
                v-if="dateRange.start"
                v-model="dateRange"
                class="p-2"
                :number-of-months="2"
                range
              />
            </template>
          </UPopover>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div class="space-y-2">
            <h3 class="font-medium text-sm">Start Time</h3>
            <UInput v-model="startTime" type="time" />
          </div>
          <div class="space-y-2">
            <h3 class="font-medium text-sm">End Time</h3>
            <UInput v-model="endTime" type="time" />
          </div>
        </div>
        <div class="flex justify-end space-x-2">
          <UButton variant="ghost" @click="handleCancel"> Cancel </UButton>
          <UButton @click="handleConfirm"> Confirm </UButton>
        </div>
      </div>
    </template>
  </UModal>
</template>
