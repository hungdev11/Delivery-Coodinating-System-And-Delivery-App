<script setup lang="ts">
/**
 * Zone Form Component
 *
 * Form for creating and editing zones
 */

import { ref, computed, watch } from 'vue'
import type { ZoneDto, CreateZoneRequest, UpdateZoneRequest, CenterDto } from '../model.type'

interface Props {
  zone?: ZoneDto | null
  centers: CenterDto[]
  mode: 'create' | 'edit'
  loading?: boolean
}

interface Emits {
  (e: 'save', data: CreateZoneRequest | UpdateZoneRequest): void
  (e: 'cancel'): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
})

const emit = defineEmits<Emits>()

// Form state
const form = ref({
  code: '',
  name: '',
  centerId: '',
  polygon: null as any, // eslint-disable-line @typescript-eslint/no-explicit-any
  polygonInput: '', // String input for polygon data
})

// Form validation
const errors = ref<Record<string, string>>({})

// Center options
const centerOptions = computed(() =>
  props.centers.map((center) => ({
    label: center.displayName,
    value: center.id,
  })),
)

// Form validation
const isFormValid = computed(() => {
  return (
    form.value.code && form.value.name && form.value.centerId && !Object.keys(errors.value).length
  )
})

/**
 * Initialize form with zone data
 */
const initializeForm = () => {
  if (props.zone) {
    form.value = {
      code: props.zone.code,
      name: props.zone.name,
      centerId: props.zone.centerId,
      polygon: props.zone.polygon,
      polygonInput: props.zone.polygon ? JSON.stringify(props.zone.polygon, null, 2) : '',
    }
  } else {
    form.value = {
      code: '',
      name: '',
      centerId: '',
      polygon: null,
      polygonInput: '',
    }
  }
  errors.value = {}
}

/**
 * Parse polygon input string
 */
const parsePolygonInput = (input: string): any => {
   
  if (!input.trim()) return null

  try {
    // Try to parse as JSON first
    const parsed = JSON.parse(input)

    // Validate GeoJSON structure
    if (parsed.type === 'Polygon' && Array.isArray(parsed.coordinates)) {
      return parsed
    }

    // If it's an array of coordinates, wrap it in GeoJSON structure
    if (Array.isArray(parsed) && parsed.length > 0) {
      return {
        type: 'Polygon',
        coordinates: parsed,
      }
    }

    throw new Error('Invalid polygon format')
  } catch (error) {
    // Try to parse as array string (e.g., "[[[lng, lat], [lng, lat]]]")
    try {
      const coords = eval(input) // Note: eval is used here for parsing array strings
      if (Array.isArray(coords)) {
        return {
          type: 'Polygon',
          coordinates: coords,
        }
      }
    } catch {
      // If all parsing fails, return null
    }

    return null
  }
}

/**
 * Validate form
 */
const validateForm = () => {
  errors.value = {}

  if (!form.value.code) {
    errors.value.code = 'Zone code is required'
  }

  if (!form.value.name) {
    errors.value.name = 'Zone name is required'
  }

  if (!form.value.centerId) {
    errors.value.centerId = 'Distribution center is required'
  }

  // Validate polygon input if provided
  if (form.value.polygonInput.trim()) {
    const parsedPolygon = parsePolygonInput(form.value.polygonInput)
    if (!parsedPolygon) {
      errors.value.polygonInput =
        'Invalid polygon format. Please use GeoJSON or coordinate array format.'
    }
  }

  return Object.keys(errors.value).length === 0
}

/**
 * Handle form submit
 */
const handleSubmit = () => {
  if (!validateForm()) {
    return
  }

  // Parse polygon from input if provided
  const polygon = form.value.polygonInput.trim()
    ? parsePolygonInput(form.value.polygonInput)
    : form.value.polygon

  const formData = {
    code: form.value.code,
    name: form.value.name,
    centerId: form.value.centerId,
    polygon: polygon,
  }

  emit('save', formData)
}

/**
 * Handle form cancel
 */
const handleCancel = () => {
  emit('cancel')
}

// Initialize form when component mounts or zone changes
watch(
  () => props.zone,
  () => {
    initializeForm()
  },
  { immediate: true },
)

// Watch for form changes to clear errors
watch(
  () => form.value,
  () => {
    if (Object.keys(errors.value).length > 0) {
      validateForm()
    }
  },
  { deep: true },
)
</script>

<template>
  <div class="zone-form w-full">
    <UForm :state="form" @submit="handleSubmit">
      <div class="space-y-4">
        <!-- Zone Code -->
        <UFormField label="Zone Code" required :error="errors.code" class="w-full">
          <UInput
            class="w-1/2"
            v-model="form.code"
            placeholder="Enter zone code (e.g., ZONE001)"
            :disabled="props.loading"
          />
        </UFormField>

        <!-- Zone Name -->
        <UFormField label="Zone Name" required :error="errors.name">
          <UInput class="w-1/2" v-model="form.name" placeholder="Enter zone name" :disabled="props.loading" />
        </UFormField>

        <!-- Distribution Center -->
        <UFormField label="Distribution Center" required :error="errors.centerId">
          <USelect
            class="w-1/2"
            v-model="form.centerId"
            :items="centerOptions"
            placeholder="Select distribution center"
            :disabled="props.loading"
          />
        </UFormField>

        <!-- Polygon Input -->
        <UFormField label="Zone Polygon" :error="errors.polygonInput">
          <UTextarea
            v-model="form.polygonInput"
            placeholder="Enter polygon data in GeoJSON format or coordinate array..."
            :rows="6"
            class="w-full"
            size="lg"
            :disabled="props.loading"
          />
          <template #help>
            <div class="text-xs text-gray-500 mt-1">
              <p><strong>Supported formats:</strong></p>
              <ul class="list-disc list-inside mt-1 space-y-1">
                <li>
                  GeoJSON: <code>{"type": "Polygon", "coordinates": [[[lng, lat], ...]]}</code>
                </li>
                <li>Coordinate array: <code>[[[lng, lat], [lng, lat], ...]]</code></li>
                <li>Array string: <code>"[[[lng, lat], [lng, lat], ...]]"</code></li>
              </ul>
            </div>
          </template>
        </UFormField>

        <!-- Polygon Info -->
        <div class="border rounded-lg p-4 bg-gray-50">
          <h4 class="font-medium text-sm mb-2">Current Polygon Status</h4>
          <div
            v-if="
              form.polygon || (form.polygonInput.trim() && parsePolygonInput(form.polygonInput))
            "
            class="text-sm text-green-600"
          >
            <UIcon name="i-heroicons-check-circle" class="w-4 h-4 inline mr-1" />
            Polygon defined ({{
              (form.polygon || parsePolygonInput(form.polygonInput))?.coordinates[0]?.length || 0
            }}
            points)
          </div>
          <div v-else class="text-sm text-gray-500">
            <UIcon name="i-heroicons-exclamation-triangle" class="w-4 h-4 inline mr-1" />
            No polygon defined
          </div>
          <p class="text-xs text-gray-500 mt-1">
            Enter polygon data above or draw on the map to define zone boundaries.
          </p>
        </div>

        <!-- Form Actions -->
        <div class="flex gap-2 pt-4 border-t">
          <UButton type="submit" color="primary" :loading="props.loading" :disabled="!isFormValid">
            {{ props.mode === 'create' ? 'Create Zone' : 'Update Zone' }}
          </UButton>
          <UButton
            type="button"
            color="neutral"
            variant="outline"
            :disabled="props.loading"
            @click="handleCancel"
          >
            Cancel
          </UButton>
        </div>
      </div>
    </UForm>
  </div>
</template>

<script lang="ts">
export default {
  name: 'ZoneForm',
}
</script>
