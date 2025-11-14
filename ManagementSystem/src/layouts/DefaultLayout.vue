<template>
  <SidebarPart>
    <RouterView />
    <!-- Global Proposal Popup -->
    <ProposalPopup />
  </SidebarPart>
</template>

<script setup lang="ts">
import SidebarPart from '@/common/components/SidebarPart.vue'
import ProposalPopup from '@/modules/Communication/components/ProposalPopup.vue'

// Initialize global chat service when layout mounts
import { useGlobalChat } from '@/modules/Communication/composables'
import { onMounted } from 'vue'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'

const globalChat = useGlobalChat()

onMounted(() => {
  const currentUser = getCurrentUser()
  if (currentUser?.id) {
    // Global chat will auto-initialize if user is logged in
    console.log('[DefaultLayout] Global chat service initialized')
  }
})
</script>
