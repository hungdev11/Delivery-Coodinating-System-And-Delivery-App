<template>
  <component :is="layoutComponent">
    <RouterView />
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import BlankLayout from '@/layouts/BlankLayout.vue'

const route = useRoute()

// Layout components mapping
const layouts = {
  default: DefaultLayout,
  blank: BlankLayout,
}

// Get layout from route meta, default to 'default'
const layoutName = computed(() => {
  return (route.meta?.layout as string) || 'default'
})

// Get the actual layout component
const layoutComponent = computed(() => {
  return layouts[layoutName.value as keyof typeof layouts] || DefaultLayout
})
</script>
