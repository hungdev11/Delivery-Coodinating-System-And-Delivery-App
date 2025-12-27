<script setup lang="ts">
/**
 * Global Progress Tracker Container
 *
 * Container component that renders all active progress trackers
 * Should be placed at the app root level
 */

import { computed } from 'vue'
import { useProgressTrackerStore } from '@/stores/progressTrackerStore'
import type { ProgressTask } from '@/stores/progressTrackerStore'
import GlobalProgressTracker from './GlobalProgressTracker.vue'

const store = useProgressTrackerStore()

const handleClose = (task: ProgressTask) => {
  if (task.onClose) {
    task.onClose()
  }
  store.removeTask(task.id)
}

const handleMinimize = (task: ProgressTask) => {
  // Minimize is handled internally by the component
  // This is just a placeholder for any future logic
}
</script>

<template>
  <div class="global-progress-tracker-container">
    <GlobalProgressTracker
      v-for="task in store.tasks"
      :key="task.id"
      :task="task"
      @close="handleClose(task)"
      @minimize="handleMinimize(task)"
    />
  </div>
</template>

<style scoped>
.global-progress-tracker-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
  pointer-events: none;
  z-index: 9998;
}

.global-progress-tracker-container > * {
  pointer-events: all;
}
</style>
