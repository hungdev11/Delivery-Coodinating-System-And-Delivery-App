<script setup lang="ts">
/**
 * Global Progress Tracker Component
 *
 * Draggable, minimizable floating component for tracking background tasks
 * Supports multiple task types: seed, assignment, etc.
 */

import { ref, computed, onUnmounted, onMounted } from 'vue'

export interface ProgressTask {
  id: string
  type: 'seed' | 'assignment' | 'custom'
  title: string
  progress?: number // 0-100
  status: 'running' | 'completed' | 'error'
  message?: string
  details?: {
    currentStep?: number
    totalSteps?: number
    stepDescription?: string
    [key: string]: any
  }
  onClose?: () => void
}

interface Props {
  task: ProgressTask
}

interface Emits {
  (e: 'close'): void
  (e: 'minimize'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// Minimize state
const isMinimized = ref(false)

// Drag state
const isDragging = ref(false)
const position = ref({ x: 0, y: 0 })
const dragStart = ref({ x: 0, y: 0 })
const containerRef = ref<HTMLElement | null>(null)

// Initialize position (bottom-right corner with some margin)
onMounted(() => {
  position.value = {
    x: window.innerWidth - 400 - 20, // 400px width + 20px margin
    y: window.innerHeight - (isMinimized.value ? 60 : 300) - 20, // Adjusted for minimize state
  }
})

// Drag handlers
const handleMouseDown = (e: MouseEvent) => {
  if (containerRef.value && !isMinimized.value) {
    isDragging.value = true
    const rect = containerRef.value.getBoundingClientRect()
    dragStart.value = {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
    }
    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
    e.preventDefault()
  }
}

const handleMouseMove = (e: MouseEvent) => {
  if (isDragging.value && containerRef.value) {
    position.value = {
      x: e.clientX - dragStart.value.x,
      y: e.clientY - dragStart.value.y,
    }

    // Keep within viewport bounds
    const width = isMinimized.value ? 200 : (containerRef.value.offsetWidth || 400)
    const height = isMinimized.value ? 50 : (containerRef.value.offsetHeight || 300)
    const maxX = window.innerWidth - width
    const maxY = window.innerHeight - height

    position.value.x = Math.max(0, Math.min(position.value.x, maxX))
    position.value.y = Math.max(0, Math.min(position.value.y, maxY))
  }
}

const handleMouseUp = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
}

// Cleanup on unmount
onUnmounted(() => {
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
})

// Toggle minimize
const toggleMinimize = () => {
  isMinimized.value = !isMinimized.value
  emit('minimize')
  
  // Adjust position when minimizing to keep bottom-right alignment
  if (containerRef.value) {
    const width = isMinimized.value ? 200 : (containerRef.value.offsetWidth || 400)
    const height = isMinimized.value ? 50 : (containerRef.value.offsetHeight || 300)
    position.value = {
      x: window.innerWidth - width - 20,
      y: window.innerHeight - height - 20,
    }
  }
}

// Computed values
const progress = computed(() => props.task.progress ?? 0)
const statusColor = computed(() => {
  switch (props.task.status) {
    case 'completed':
      return 'green'
    case 'error':
      return 'red'
    default:
      return 'yellow'
  }
})

const progressText = computed(() => {
  if (props.task.status === 'completed') {
    return 'Hoàn thành'
  }
  if (props.task.status === 'error') {
    return 'Lỗi'
  }
  return `${progress.value}%`
})

const canClose = computed(() => props.task.status === 'completed' || props.task.status === 'error')

// Dynamic styling
const containerStyle = computed(() => ({
  position: 'fixed',
  left: `${position.value.x}px`,
  top: `${position.value.y}px`,
  zIndex: 9999,
  width: isMinimized.value ? '200px' : '380px',
  cursor: isDragging.value ? 'grabbing' : isMinimized.value ? 'default' : 'grab',
  userSelect: 'none' as const,
  transition: isDragging.value ? 'none' : 'width 0.2s, height 0.2s',
}))
</script>

<template>
  <div
    ref="containerRef"
    :style="containerStyle"
    class="global-progress-tracker shadow-2xl rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 overflow-hidden"
  >
    <!-- Minimized View -->
    <div v-if="isMinimized" class="flex items-center justify-between p-3 cursor-pointer" @click="toggleMinimize">
      <div class="flex items-center gap-2 flex-1 min-w-0">
        <div
          class="w-2 h-2 rounded-full flex-shrink-0"
          :class="{
            'bg-green-500': task.status === 'completed',
            'bg-red-500': task.status === 'error',
            'bg-yellow-500 animate-pulse': task.status === 'running',
          }"
        ></div>
        <span class="text-sm font-medium truncate">{{ task.title }}</span>
      </div>
          <div class="flex items-center gap-1 flex-shrink-0">
        <component
          :is="UButton"
          icon="i-heroicons-chevron-up"
          variant="ghost"
          size="xs"
          @click.stop="toggleMinimize"
        />
        <component
          :is="UButton"
          v-if="canClose"
          icon="i-heroicons-x-mark"
          variant="ghost"
          size="xs"
          @click.stop="emit('close')"
        />
      </div>
    </div>

    <!-- Expanded View -->
    <component
      :is="UCard"
      v-else
      :ui="{ body: 'p-4', header: 'p-3', footer: 'p-3' }"
    >
      <template #header>
        <div
          class="flex items-center justify-between cursor-grab active:cursor-grabbing"
          @mousedown="handleMouseDown"
        >
          <div class="flex items-center gap-2">
            <div
              class="w-2 h-2 rounded-full"
              :class="{
                'bg-green-500': task.status === 'completed',
                'bg-red-500': task.status === 'error',
                'bg-yellow-500 animate-pulse': task.status === 'running',
              }"
            ></div>
            <h3 class="text-sm font-semibold">{{ task.title }}</h3>
          </div>
          <div class="flex items-center gap-1">
            <component
              :is="UButton"
              icon="i-heroicons-minus"
              variant="ghost"
              size="xs"
              @click="toggleMinimize"
            />
            <component
              :is="UButton"
              v-if="canClose"
              icon="i-heroicons-x-mark"
              variant="ghost"
              size="xs"
              @click="emit('close')"
            />
          </div>
        </div>
      </template>

      <div class="space-y-3">
        <!-- Progress Bar -->
        <div v-if="task.progress !== undefined">
          <div class="flex items-center justify-between mb-1.5">
            <span class="text-xs font-medium truncate flex-1 mr-2">
              {{ task.details?.stepDescription || task.message || 'Đang xử lý...' }}
            </span>
            <span class="text-xs text-gray-500 whitespace-nowrap">{{ progressText }}</span>
          </div>
          <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
            <div
              class="bg-primary h-2 rounded-full transition-all duration-300"
              :style="{ width: `${progress}%` }"
            ></div>
          </div>
        </div>

        <!-- Step Info -->
        <div v-if="task.details?.currentStep && task.details?.currentStep > 0" class="text-xs text-gray-600 dark:text-gray-400">
          Bước {{ task.details.currentStep }}/{{ task.details.totalSteps || 5 }}
        </div>

        <!-- Message -->
        <div v-if="task.message && !task.progress" class="text-xs text-gray-600 dark:text-gray-400">
          {{ task.message }}
        </div>

        <!-- Error Message -->
        <div
          v-if="task.status === 'error' && task.message"
          class="p-2 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded text-xs"
        >
          <div class="font-medium text-red-800 dark:text-red-200">Lỗi:</div>
          <div class="text-red-600 dark:text-red-300">{{ task.message }}</div>
        </div>

        <!-- Success Message -->
        <div
          v-if="task.status === 'completed'"
          class="p-2 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded text-xs text-green-800 dark:text-green-200"
        >
          Hoàn thành thành công!
        </div>
      </div>
    </component>
  </div>
</template>

<style scoped>
.global-progress-tracker {
  transition: box-shadow 0.2s;
}

.global-progress-tracker:hover {
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.global-progress-tracker:active {
  cursor: grabbing;
}
</style>
