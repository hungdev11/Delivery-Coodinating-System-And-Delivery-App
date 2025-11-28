<template>
  <div class="relative min-h-screen">
    <RouterView />

    <!-- Floating Color Mode Toggle Button -->
    <UButton
      :icon="mode === 'dark' ? 'i-lucide-moon' : 'i-lucide-sun'"
      color="neutral"
      variant="ghost"
      size="lg"
      class="fixed top-6 right-6 z-50 shadow-lg hover:shadow-xl transition-shadow dark:bg-white bg-gray-900 dark:text-gray-900 text-white dark:hover:bg-gray-900 dark:hover:text-white hover:bg-white hover:text-gray-900"
      @click="mode = mode === 'dark' ? 'light' : 'dark'"
    />
  </div>
</template>

<script setup lang="ts">
import { useColorMode } from '@vueuse/core'

const mode = useColorMode({
  storageKey: 'erp-color-mode',
})
</script>
