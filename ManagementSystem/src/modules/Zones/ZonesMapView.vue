<script setup lang="ts">
/**
 * Zones Map View
 *
 * Interactive map view for managing zones with polygon visualization
 */

import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { PageHeader } from '@/common/components'
import MapView from '@/common/components/MapView.vue'
import ZoneForm from './components/ZoneForm.vue'
import { useZonesStore } from './composables'
import type { ZoneDto, CreateZoneRequest, UpdateZoneRequest } from './model.type'
import type { ZonePolygon, MapMarker } from '@/common/types'
import { storeToRefs } from 'pinia'
import { useResponsiveStore } from '@/common/store/responsive.store'

const router = useRouter()

// Composables
const zoneStores = useZonesStore()
const { loadZones, loadCenters, create, update, remove, selectedCenterId, filterByCenter } =
  zoneStores

const { zones, centers, loading, error } = storeToRefs(zoneStores)

const responsiveStore = useResponsiveStore()

// Drawer direction: top for mobile/Android, bottom for desktop
const drawerDirection = computed(() => {
  return responsiveStore.isMobile || responsiveStore.isAndroid ? 'top' : 'bottom'
})

// Map state
const mapLoaded = ref(false)
const selectedZone = ref<ZoneDto | null>(null)
const drawerOpen = ref(false)
const isEditing = ref(false)

// Center filter options
const centerFilterOptions = computed(() => [
  { label: 'All Centers', value: undefined },
  ...centers.value.map((c) => ({
    label: c.displayName,
    value: c.center_id,
  })),
])

// Convert zones to map polygons with colors
const zonePolygons = computed((): ZonePolygon[] => {
  const colors = [
    '#3b82f6', // blue
    '#10b981', // emerald
    '#f59e0b', // amber
    '#ef4444', // red
    '#8b5cf6', // violet
    '#06b6d4', // cyan
    '#84cc16', // lime
    '#f97316', // orange
    '#ec4899', // pink
    '#6b7280', // gray
  ]

  return zones.value.map((zone, index) => ({
    id: zone.zone_id,
    name: zone.name,
    coordinates: zone.polygon?.coordinates as [number, number][][],
    color: colors[index % colors.length],
    opacity: 0.6,
    strokeColor: colors[index % colors.length],
    strokeWidth: 2,
  }))
})

// Map markers for zone centers with labels
const zoneMarkers = computed((): MapMarker[] => {
  return zones.value
    .filter((zone) => zone.polygon)
    .map((zone) => {
      // Calculate center of polygon
      const coords = zone.polygon?.coordinates[0] || []
      if (coords.length === 0) return null

      const centerLng = coords.reduce((sum, coord) => sum + coord[0], 0) / coords.length
      const centerLat = coords.reduce((sum, coord) => sum + coord[1], 0) / coords.length

      return {
        id: `marker-${zone.zone_id}`,
        coordinates: [centerLng, centerLat],
        type: 'zone-center',
        title: zone.name,
        description: zone.code,
        label: zone.name, // Hiển thị tên zone
        color: '#3b82f6', // Màu xanh cho marker
      }
    })
    .filter(Boolean) as MapMarker[]
})

// Map configuration
const mapConfig = {
  center: [106.660172, 10.762622] as [number, number], // Ho Chi Minh City
  zoom: 11,
  style: `https://api.maptiler.com/maps/streets/style.json?key=${import.meta.env.VITE_MAPTILER_API_KEY || 'get_your_own_OpIi9ZULNHzrESv6T2vL'}`,
}

/**
 * Handle map click events
 */
const handleMapClick = (data: { lngLat: [number, number]; point: [number, number] }) => {
  console.log('Map clicked:', data)
}

/**
 * Handle zone click events
 */
const handleZoneClick = (data: { zoneId: string; zoneName: string; lngLat: [number, number] }) => {
  const zone = zones.value.find((z) => z.zone_id === data.zoneId)
  if (zone) {
    selectedZone.value = zone
    isEditing.value = false
    drawerOpen.value = true
  }
}

/**
 * Handle map loaded
 */
const handleMapLoaded = () => {
  mapLoaded.value = true
  console.log('Map loaded successfully')
}

/**
 * Handle view change
 */
const handleViewChange = (state: {
  center: [number, number]
  zoom: number
  bearing: number
  pitch: number
}) => {
  console.log('View changed:', state)
}

/**
 * Refresh data
 */
const refreshData = async () => {
  await loadCenters()
  await loadZones()
}

/**
 * Open create zone drawer
 */
const openCreateZone = () => {
  selectedZone.value = null
  isEditing.value = false
  drawerOpen.value = true
}

/**
 * Handle zone save
 */
const handleZoneSave = async (formData: CreateZoneRequest | UpdateZoneRequest) => {
  try {
    if (isEditing.value && selectedZone.value) {
      await update(selectedZone.value.zone_id, formData as UpdateZoneRequest)
    } else {
      await create(formData as CreateZoneRequest)
    }
    drawerOpen.value = false
    selectedZone.value = null
  } catch (error) {
    console.error('Failed to save zone:', error)
  }
}

/**
 * Handle zone form cancel
 */
const handleZoneFormCancel = () => {
  drawerOpen.value = false
  selectedZone.value = null
  isEditing.value = false
}

/**
 * Handle zone delete
 */
const handleZoneDelete = async (zoneId: string) => {
  try {
    await remove(zoneId)
    drawerOpen.value = false
    selectedZone.value = null
  } catch (error) {
    console.error('Failed to delete zone:', error)
  }
}

// Load data on mount
onMounted(() => {
  loadCenters()
  loadZones()
})

// Watch for center filter changes
watch(selectedCenterId, (newValue) => {
  if (newValue !== undefined) {
    loadZones()
  }
})
</script>

<template>
  <div class="zones-map-view">
    <!-- Page Header -->
    <PageHeader title="Zones Map" description="Interactive map view for managing delivery zones">
      <template #actions>
        <UButton
          color="neutral"
          variant="outline"
          icon="i-heroicons-arrow-path"
          :loading="loading"
          @click="refreshData"
        >
          Refresh
        </UButton>
        <UButton
          color="neutral"
          variant="outline"
          icon="i-heroicons-table-cells"
          @click="router.push('/zones')"
        >
          Table View
        </UButton>
        <UButton color="primary" icon="i-heroicons-plus" @click="openCreateZone">
          Add Zone
        </UButton>
      </template>
    </PageHeader>

    <!-- Error Alert -->
    <UAlert
      v-if="error"
      color="error"
      variant="soft"
      title="Failed to load zones"
      :description="error"
      class="mb-4"
    />

    <!-- Filter Bar -->
    <div class="mb-4 flex items-center gap-4">
      <UFormField label="Filter by Center" class="w-64">
        <USelect
          :model-value="selectedCenterId"
          :items="centerFilterOptions"
          @update:model-value="(value) => filterByCenter(value as string | undefined)"
        />
      </UFormField>

      <div class="flex items-center gap-2 text-sm text-gray-600">
        <UIcon name="i-heroicons-information-circle" class="w-4 h-4" />
        <span>{{ zones.length }} zones loaded</span>
      </div>
    </div>

    <!-- Map Container -->
    <div class="map-container">
      <MapView
        :config="mapConfig"
        :zones="zonePolygons"
        :markers="zoneMarkers"
        :loading="loading"
        :auto-fit="true"
        :fit-padding="50"
        @map-loaded="handleMapLoaded"
        @map-click="handleMapClick"
        @zone-click="handleZoneClick"
        @view-change="handleViewChange"
      >
        <!-- Map Controls -->
        <template #controls>
          <div class="map-controls-panel">
            <div class="flex flex-col gap-2">
              <UButton
                color="neutral"
                variant="solid"
                size="sm"
                icon="i-heroicons-plus"
                @click="openCreateZone"
              >
                Add Zone
              </UButton>
              <UButton
                color="neutral"
                variant="solid"
                size="sm"
                icon="i-heroicons-arrow-path"
                :loading="loading"
                @click="refreshData"
              >
                Refresh
              </UButton>
            </div>
          </div>
        </template>

        <!-- Map Legend -->
        <template #legend>
          <div class="map-legend-panel">
            <h3 class="font-semibold text-sm mb-2">Zones</h3>
            <div class="space-y-1">
              <div
                v-for="(zone, index) in zones.slice(0, 5)"
                :key="zone.zone_id"
                class="flex items-center gap-2 text-xs"
              >
                <div
                  class="w-3 h-3 rounded"
                  :style="{ backgroundColor: zonePolygons[index]?.color }"
                />
                <span class="truncate">{{ zone.name }}</span>
              </div>
              <div v-if="zones.length > 5" class="text-xs text-gray-500">
                +{{ zones.length - 5 }} more zones
              </div>
            </div>
          </div>
        </template>
      </MapView>
    </div>

    <!-- Zone Detail Drawer -->
    <UDrawer
      :open="drawerOpen"
      :direction="drawerDirection"
      @update:open="drawerOpen = $event"
      :dismissible="false"
      :handle="false"
      :ui="{ header: 'flex items-center justify-between' }"
    >
      <template #header>
        <h2 class="text-highlighted font-semibold">
          {{ isEditing ? 'Edit Zone' : selectedZone ? 'Zone Details' : 'Create Zone' }}
        </h2>
        <UButton
          color="neutral"
          variant="ghost"
          icon="i-heroicons-x-mark"
          @click="drawerOpen = false"
        />
      </template>

      <template #body>
        <div class="p-4">
          <div class="space-y-4">
            <!-- Zone Details (View Mode) -->
            <div v-if="selectedZone && !isEditing" class="border-b pb-4">
              <h3 class="font-semibold text-lg">{{ selectedZone.name }}</h3>
              <p class="text-sm text-gray-600">{{ selectedZone.code }}</p>
              <p class="text-sm text-gray-600">{{ selectedZone.centerDisplayName }}</p>

              <div class="mt-4 flex gap-2">
                <UButton
                  color="primary"
                  variant="outline"
                  icon="i-heroicons-pencil"
                  @click="isEditing = true"
                >
                  Edit
                </UButton>
                <UButton
                  color="error"
                  variant="outline"
                  icon="i-heroicons-trash"
                  @click="handleZoneDelete(selectedZone.zone_id)"
                >
                  Delete
                </UButton>
              </div>
            </div>

            <!-- Zone Form (Create/Edit Mode) -->
            <ZoneForm
              :zone="selectedZone"
              :centers="centers"
              :mode="isEditing ? 'edit' : 'create'"
              :loading="loading"
              @save="handleZoneSave"
              @cancel="handleZoneFormCancel"
            />
          </div>
        </div>
      </template>
    </UDrawer>
  </div>
</template>
