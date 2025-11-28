<script setup lang="ts">
/**
 * TypingIndicator Component
 *
 * Shows "User is typing..." animation
 */

import { computed } from 'vue'

interface Props {
  userName?: string
  show?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  userName: 'Someone',
  show: false,
})

const displayText = computed(() => {
  return `${props.userName} is typing...`
})
</script>

<template>
  <Transition
    enter-active-class="transition ease-out duration-200"
    enter-from-class="opacity-0 translate-y-1"
    enter-to-class="opacity-100 translate-y-0"
    leave-active-class="transition ease-in duration-150"
    leave-from-class="opacity-100 translate-y-0"
    leave-to-class="opacity-0 translate-y-1"
  >
    <div v-if="show" class="flex items-center space-x-2 px-4 py-2 text-sm text-gray-500">
      <div class="flex space-x-1">
        <div
          class="w-2 h-2 bg-neutral-400 rounded-full animate-bounce"
          style="animation-delay: 0ms"
        ></div>
        <div
          class="w-2 h-2 bg-neutral-400 rounded-full animate-bounce"
          style="animation-delay: 150ms"
        ></div>
        <div
          class="w-2 h-2 bg-neutral-400 rounded-full animate-bounce"
          style="animation-delay: 300ms"
        ></div>
      </div>
      <span class="italic">{{ displayText }}</span>
    </div>
  </Transition>
</template>

<style scoped>
@keyframes bounce {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-0.5rem);
  }
}

.animate-bounce {
  animation: bounce 1s infinite;
}
</style>
