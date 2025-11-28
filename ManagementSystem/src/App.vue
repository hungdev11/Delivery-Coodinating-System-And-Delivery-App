<template>
  <UApp>
    <LayoutResolver />
    <!-- PWA Components -->
    <PWAUpdateNotification />
    <PWAInstallPrompt />
  </UApp>
</template>

<script setup lang="ts">
import LayoutResolver from '@/common/components/LayoutResolver.vue'
import PWAUpdateNotification from '@/common/components/PWAUpdateNotification.vue'
import PWAInstallPrompt from '@/common/components/PWAInstallPrompt.vue'
</script>
