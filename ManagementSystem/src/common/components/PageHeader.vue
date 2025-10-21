<script setup lang="ts">
import router from '@/router';

/**
 * PageHeader Component
 *
 * Page header with title, description, and actions
 *
 * @example
 * ```vue
 * <PageHeader
 *   title="Users"
 *   description="Manage system users"
 * >
 *   <template #actions>
 *     <UButton>Add User</UButton>
 *   </template>
 * </PageHeader>
 * ```
 */

interface Props {
  /** Page title */
  title: string
  /** Page description */
  description?: string
  /** Show back button */
  showBack?: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  back: []
}>()

const handleBack = () => {
  emit('back')
  router.back()
}
</script>

<template>
  <div class="border-b border-gray-200 pb-5 mb-6">
    <div class="flex items-center justify-between">
      <div class="flex items-center space-x-3">
        <UButton
          v-if="showBack"
          icon="i-heroicons-arrow-left"
          variant="ghost"
          @click="handleBack"
          aria-label="Go back"
        />
        <div>
          <h1 class="text-3xl font-bold text-gray-900">{{ title }}</h1>
          <p v-if="description" class="mt-1 text-sm text-gray-600">
            {{ description }}
          </p>
        </div>
      </div>

      <div class="flex items-center space-x-3">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>
