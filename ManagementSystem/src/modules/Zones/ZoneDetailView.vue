<script setup lang="ts">
/**
 * Zone Detail View
 *
 * View for displaying zone details with map
 */

import { ref, onMounted, computed, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import { PageHeader, MapView } from '@/common/components'
import { getZoneById, deleteZone } from './api'
import { ZoneDto } from './model.type'
import type { ZonePolygon, MapMarker } from '@/common/types/map.type'

// Lazy load modals
const LazyZoneFormModal = defineAsyncComponent(() => import('./components/ZoneFormModal.vue'))
const LazyZoneDeleteModal = defineAsyncComponent(() => import('./components/ZoneDeleteModal.vue'))

const route = useRoute()
const router = useRouter()
const toast = useToast()
const overlay = useOverlay()

const zoneId = computed(() => route.params.id as string)
const zone = ref<ZoneDto | null>(null)
const loading = ref(false)
const showMap = ref(true)

/**
 * Convert zone to map polygon
 */
const zonePolygons = computed<ZonePolygon[]>(() => {
  if (!zone.value || !zone.value.polygon) return []

  return [
    {
      id: zone.value.id,
      name: zone.value.displayName,
      coordinates: zone.value.polygon.coordinates as [number, number][][],
      fillColor: '#3b82f6',
      strokeColor: '#2563eb',
    },
  ]
})

/**
 * Center marker if location available
 */
const centerMarkers = computed<MapMarker[]>(() => {
  // Add center marker if location is available (future enhancement)
  return []
})

/**
 * Load zone details
 */
const loadZone = async () => {
  loading.value = true
  try {
    const response = await getZoneById(zoneId.value)
    if (response.result) {
      zone.value = new ZoneDto(response.result)
    }
  } catch (error) {
    console.error('Failed to load zone:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to load zone details',
      color: 'error',
    })
  } finally {
    loading.value = false
  }
}

/**
 * Open edit modal
 */
const openEditModal = async () => {
  if (!zone.value) return

  const modal = overlay.create(LazyZoneFormModal)
  const instance = modal.open({ mode: 'edit', zone: zone.value })
  const formData = await instance.result

  if (formData) {
    loadZone()
  }
}

/**
 * Open delete modal
 */
const openDeleteModal = async () => {
  if (!zone.value) return

  const modal = overlay.create(LazyZoneDeleteModal)
  const instance = modal.open({ zoneName: zone.value.displayName })
  const confirmed = await instance.result

  if (confirmed) {
    try {
      await deleteZone(zone.value.id)
      toast.add({
        title: 'Success',
        description: 'Zone deleted successfully',
        color: 'success',
      })
      router.push('/zones')
    } catch (error) {
      console.error('Failed to delete zone:', error)
    }
  }
}

onMounted(() => {
  loadZone()
})
</script>

<template>
  <div class="container mx-auto px-4 py-6">
    <PageHeader
      :title="zone?.displayName || 'Zone Details'"
      description="View and manage zone information"
      show-back
    >
      <template #actions>
        <UButton variant="outline" icon="i-heroicons-pencil" @click="openEditModal"> Edit </UButton>
        <UButton color="error" variant="soft" icon="i-heroicons-trash" @click="openDeleteModal">
          Delete
        </UButton>
      </template>
    </PageHeader>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <UIcon name="i-heroicons-arrow-path" class="w-8 h-8 animate-spin text-primary-500" />
    </div>

    <div v-else-if="zone" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Main Info Card -->
      <UCard class="lg:col-span-2">
        <template #header>
          <h3 class="text-lg font-semibold">Zone Information</h3>
        </template>

        <div class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="text-sm font-medium text-gray-500">Zone Code</label>
              <p class="mt-1 text-base font-mono">{{ zone.code }}</p>
            </div>

            <div>
              <label class="text-sm font-medium text-gray-500">Zone Name</label>
              <p class="mt-1 text-base">{{ zone.name }}</p>
            </div>
          </div>

          <div>
            <label class="text-sm font-medium text-gray-500">Distribution Center</label>
            <p class="mt-1 text-base">{{ zone.centerDisplayName }}</p>
          </div>

          <div>
            <label class="text-sm font-medium text-gray-500">Polygon</label>
            <div class="mt-1">
              <UBadge :color="zone.hasPolygon ? 'green' : 'gray'" variant="soft">
                {{ zone.hasPolygon ? 'Defined' : 'Not Defined' }}
              </UBadge>
            </div>
          </div>

          <!-- Map -->
          <div v-if="zone.hasPolygon" class="mt-4">
            <label class="text-sm font-medium text-gray-500 mb-2 block">Zone Boundaries</label>
            <MapView
              :zones="zonePolygons"
              :markers="centerMarkers"
              :show-zones="showMap"
              :show-routing="false"
              :show-traffic="false"
              :zone-options="{ fillOpacity: 0.25, strokeWidth: 2, showLabels: true }"
              height="500px"
            >
              <template #controls>
                <div class="space-y-2">
                  <UButton
                    :color="showMap ? 'primary' : 'gray'"
                    variant="soft"
                    size="sm"
                    icon="i-heroicons-map"
                    @click="showMap = !showMap"
                  >
                    {{ showMap ? 'Hide' : 'Show' }} Zone
                  </UButton>
                </div>
              </template>

              <template #legend>
                <div class="space-y-2">
                  <h4 class="font-semibold text-sm">Legend</h4>
                  <div class="flex items-center gap-2">
                    <div class="w-4 h-4 bg-primary-500/25 border-2 border-primary-600 rounded" />
                    <span class="text-xs">{{ zone.displayName }}</span>
                  </div>
                </div>
              </template>
            </MapView>
          </div>
        </div>
      </UCard>

      <!-- Meta Info Card -->
      <UCard>
        <template #header>
          <h3 class="text-lg font-semibold">Meta Information</h3>
        </template>

        <div class="space-y-4">
          <div>
            <label class="text-sm font-medium text-gray-500">Zone ID</label>
            <p class="mt-1 text-sm font-mono break-all">{{ zone.id }}</p>
          </div>

          <div>
            <label class="text-sm font-medium text-gray-500">Center ID</label>
            <p class="mt-1 text-sm font-mono break-all">{{ zone.centerId }}</p>
          </div>

          <div v-if="zone.centerCode">
            <label class="text-sm font-medium text-gray-500">Center Code</label>
            <p class="mt-1 text-sm font-mono">{{ zone.centerCode }}</p>
          </div>
        </div>
      </UCard>
    </div>

    <div v-else class="text-center py-12">
      <p class="text-gray-500">Zone not found</p>
    </div>
  </div>
</template>
