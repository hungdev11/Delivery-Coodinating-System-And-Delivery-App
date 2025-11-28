<script setup lang="ts">
/**
 * Zone Form Modal
 *
 * Modal for creating/editing zones
 * Usage with useOverlay()
 */

import { ref, computed, onMounted } from 'vue'
import type { ZoneDto, CenterDto, GeoJSONPolygon } from '../model.type'
import { getCenters } from '../api'

interface Props {
  zone?: ZoneDto
  mode: 'create' | 'edit'
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: [result: any] }>()

// Form state
const form = ref({
  code: props.zone?.code || '',
  name: props.zone?.name || '',
  centerId: props.zone?.centerId || '',
  polygon: props.zone?.polygon || null,
})

const submitting = ref(false)
const centers = ref<CenterDto[]>([])
const loadingCenters = ref(false)

const isEditMode = computed(() => props.mode === 'edit')

const centerOptions = computed(() =>
  centers.value.map((c) => ({
    label: c.displayName,
    value: c.id,
  })),
)

/**
 * Load centers
 */
const loadCenters = async () => {
  loadingCenters.value = true
  try {
    const response = await getCenters()
    if (response.result) {
      centers.value = response.result
    }
  } catch (error) {
    console.error('Failed to load centers:', error)
  } finally {
    loadingCenters.value = false
  }
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    emit('close', form.value)
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  emit('close', null)
}

onMounted(() => {
  loadCenters()
})
</script>

<template>
  <UModal
    :title="isEditMode ? 'Edit Zone' : 'Create Zone'"
    :close="{ onClick: handleCancel }"
    :ui="{ width: 'sm:max-w-md md:max-w-lg', footer: 'justify-end' }"
  >
    <template #body>
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <UFormField label="Zone Code" name="code" required>
          <UInput v-model="form.code" placeholder="Enter zone code (e.g., ZONE001)" />
        </UFormField>

        <UFormField label="Zone Name" name="name" required>
          <UInput v-model="form.name" placeholder="Enter zone name" />
        </UFormField>

        <UFormField label="Distribution Center" name="centerId" required>
          <USelect
            v-model="form.centerId"
            :items="centerOptions"
            :loading="loadingCenters"
            placeholder="Select a distribution center"
          />
        </UFormField>

        <UFormField label="Polygon" name="polygon" help="Define zone boundaries (optional)">
          <div class="text-sm text-gray-500 p-3 bg-gray-50 rounded">
            Map drawing tool will be available in the detail view
          </div>
        </UFormField>
      </form>
    </template>

    <template #footer>
      <UButton :disabled="submitting" variant="outline" color="neutral" @click="handleCancel">
        Cancel
      </UButton>
      <UButton :loading="submitting" @click="handleSubmit">
        {{ isEditMode ? 'Update' : 'Create' }}
      </UButton>
    </template>
  </UModal>
</template>
