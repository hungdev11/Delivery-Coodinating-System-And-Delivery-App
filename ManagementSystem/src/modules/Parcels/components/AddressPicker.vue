<script setup lang="ts">
/**
 * Address Picker Component
 *
 * Simplified address picker for embedding in forms
 * Allows picking coordinates on map and searching addresses
 */

import { ref, computed, onUnmounted, watch } from 'vue'
import MapView from '@/common/components/MapView.vue'
import { useAddresses } from '@/modules/Addresses/composables'
import type { ByPointResult } from '@/modules/Addresses/api'
import { storeToRefs } from 'pinia'

interface Props {
  modelValue?: {
    lat?: number
    lon?: number
    addressText?: string
  }
  label?: string
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  label: 'Address',
  placeholder: 'Enter address or pick on map',
})

const emit = defineEmits<{
  'update:modelValue': [value: { lat?: number; lon?: number; addressText?: string }]
}>()

// Map state
const mapViewRef = ref<InstanceType<typeof MapView>>()
const mapLoaded = ref(false)
const mapCenter = ref<[number, number]>([106.660172, 10.762622]) // [lng, lat]
const cursorLngLat = ref<[number, number] | null>(null)
let mapOnMove: (() => void) | null = null
let mapOnMouseMove: ((e: { lngLat?: { lng: number; lat: number } }) => void) | null = null

// Address composable
const addrStore = useAddresses()
const { searchTerm, searching, searchResult } = storeToRefs(addrStore)

// Form state
const addressText = ref(props.modelValue?.addressText || '')
const showMap = ref(false)
const nearbyLoading = ref(false)
const nearbyResults = ref<Array<{
  source: 'local' | 'track-asia'
  id?: string
  name: string
  lat: number
  lon: number
  addressText?: string
}> | null>(null)

// Initialize map center from modelValue
watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue?.lat && newValue?.lon) {
      mapCenter.value = [newValue.lon, newValue.lat]
      if (newValue.addressText) {
        addressText.value = newValue.addressText
      }
    } else if (newValue?.addressText && !addressText.value) {
      addressText.value = newValue.addressText
    }
  },
  { immediate: true },
)

// Watch addressText changes from parent
watch(
  () => props.modelValue?.addressText,
  (newText) => {
    if (newText && newText !== addressText.value) {
      addressText.value = newText
    }
  },
)

const mapConfig = computed(() => ({
  center: [mapCenter.value[0], mapCenter.value[1]] as [number, number],
  zoom: 15,
  style: `https://api.maptiler.com/maps/streets/style.json?key=${import.meta.env.VITE_MAPTILER_API_KEY || 'get_your_own_OpIi9ZULNHzrESv6T2vL'}`,
}))

const mapMarkers = computed(() => [])
const mapRoutes = computed(() => [])

const currentLat = computed(() => mapCenter.value[1])
const currentLon = computed(() => mapCenter.value[0])

const centerLabel = computed(() => `${currentLat.value.toFixed(6)}, ${currentLon.value.toFixed(6)}`)

const handleMapLoaded = () => {
  mapLoaded.value = true
  const map = mapViewRef.value?.map
  if (!map) return

  const onMove = () => {
    const c = map.getCenter()
    mapCenter.value = [c.lng, c.lat]
    updateValue()
  }

  const onMouseMove = (e: { lngLat?: { lng: number; lat: number } }) => {
    if (e && e.lngLat) {
      cursorLngLat.value = [e.lngLat.lng, e.lngLat.lat]
    }
  }

  map.on('move', onMove)
  map.on('mousemove', onMouseMove)
  mapOnMove = onMove
  mapOnMouseMove = onMouseMove
}

const handleMapMove = (data: { center: [number, number] }) => {
  mapCenter.value = data.center
  updateValue()
}

const handleMapClick = (data: { lngLat: [number, number] }) => {
  const map = mapViewRef.value?.map
  if (map) {
    map.easeTo({ center: data.lngLat, duration: 800 })
  }
  mapCenter.value = [data.lngLat[0], data.lngLat[1]]
  updateValue()
  void loadNearby(data.lngLat[1], data.lngLat[0])
}

const updateValue = () => {
  emit('update:modelValue', {
    lat: currentLat.value,
    lon: currentLon.value,
    addressText: addressText.value || undefined,
  })
}

const handleAddressTextChange = () => {
  updateValue()
}

const fetchSearch = async () => {
  await addrStore.search(10)
}

const jumpTo = (lat: number, lon: number, name?: string, addrText?: string) => {
  const map = mapViewRef.value?.map
  if (map) {
    map.easeTo({ center: [lon, lat], zoom: Math.max(map.getZoom(), 17), duration: 800 })
  }
  mapCenter.value = [lon, lat]
  if (name && !addressText.value) addressText.value = name
  if (addrText && !addressText.value) addressText.value = addrText
  updateValue()
}

const selectNearby = (item: {
  id?: string
  source: 'local' | 'track-asia'
  name: string
  lat: number
  lon: number
  addressText?: string
}) => {
  jumpTo(item.lat, item.lon, item.name, item.addressText)
}

const loadNearby = async (lat: number, lon: number) => {
  nearbyLoading.value = true
  try {
    const res = await addrStore.findByPoint({ lat, lon, radius: 100, limit: 10 })
    const result = (res && 'result' in res ? (res as { result?: ByPointResult }).result : undefined)
    const items: typeof nearbyResults.value = []
    if (result && result.local) {
      for (const a of result.local) {
        items!.push({
          source: 'local',
          id: a.id,
          name: a.name,
          lat: a.lat,
          lon: a.lon,
          addressText: a.addressText || undefined,
        })
      }
    }
    if (result && result.external) {
      for (const e of result.external) {
        items!.push({
          source: 'track-asia',
          name: e.name,
          lat: e.lat,
          lon: e.lon,
          addressText: e.formattedAddress,
        })
      }
    }
    nearbyResults.value = items!.sort((a, b) => (a.source === 'local' ? -1 : 1) - (b.source === 'local' ? -1 : 1))
  } catch {
    nearbyResults.value = []
  } finally {
    nearbyLoading.value = false
  }
}

const useCurrentLocation = () => {
  updateValue()
  void loadNearby(currentLat.value, currentLon.value)
}

onUnmounted(() => {
  const map = mapViewRef.value?.map
  if (map && mapOnMove) map.off('move', mapOnMove)
  if (map && mapOnMouseMove) map.off('mousemove', mapOnMouseMove)
})
</script>

<template>
  <div class="address-picker">
    <UFormField :label="label">
      <div class="space-y-2">
        <!-- Address Text Input -->
        <UInput
          :model-value="addressText"
          :placeholder="placeholder"
          @update:model-value="addressText = $event; handleAddressTextChange()"
        />

        <!-- Coordinates Display -->
        <div v-if="currentLat && currentLon" class="text-xs text-gray-500">
          Coordinates: {{ centerLabel }}
        </div>

        <!-- Toggle Map Button -->
        <UButton
          size="xs"
          variant="outline"
          color="primary"
          @click="showMap = !showMap"
        >
          {{ showMap ? 'Hide Map' : 'Pick on Map' }}
        </UButton>

        <!-- Map Section -->
        <div v-if="showMap" class="mt-4 space-y-4">
          <div class="border rounded-lg overflow-hidden" style="height: 400px; position: relative">
            <MapView
              ref="mapViewRef"
              :config="mapConfig"
              :markers="mapMarkers"
              :routes="mapRoutes"
              :show-zones="false"
              :show-routing="false"
              height="400px"
              @map-loaded="handleMapLoaded"
              @map-click="handleMapClick"
              @map-move="handleMapMove"
            />
            <!-- Fixed center marker overlay -->
            <div class="center-pin">
              <div class="pin" />
            </div>
            <div class="center-coords">
              {{ centerLabel }}
            </div>
          </div>

          <!-- Search Address -->
          <UCard>
            <template #header>
              <h4 class="text-sm font-semibold">Search Address</h4>
            </template>
            <div class="space-y-2">
              <div class="flex gap-2">
                <UInput
                  v-model="searchTerm"
                  placeholder="Search address..."
                  size="sm"
                  @keydown.enter.prevent="fetchSearch"
                />
                <UButton size="sm" :loading="searching" @click="fetchSearch">Search</UButton>
              </div>
              <div v-if="searchResult" class="space-y-1 max-h-32 overflow-y-auto">
                <div
                  v-for="(a, i) in searchResult.local || []"
                  :key="`loc-${i}`"
                  class="p-2 rounded border cursor-pointer hover:bg-gray-50 text-xs"
                  @click="jumpTo(a.lat, a.lon, a.name, a.addressText || undefined)"
                >
                  <p class="font-medium">{{ a.name }}</p>
                  <p class="text-gray-500">{{ a.addressText || '' }}</p>
                </div>
                <div
                  v-for="(e, i) in searchResult.external || []"
                  :key="`ext-${i}`"
                  class="p-2 rounded border cursor-pointer hover:bg-gray-50 text-xs"
                  @click="jumpTo(e.lat, e.lon, e.name, e.formattedAddress)"
                >
                  <p class="font-medium">{{ e.name }}</p>
                  <p class="text-gray-500">{{ e.formattedAddress || '' }}</p>
                </div>
              </div>
            </div>
          </UCard>

          <!-- Nearby Results -->
          <UCard>
            <template #header>
              <div class="flex items-center justify-between">
                <h4 class="text-sm font-semibold">Nearby</h4>
                <UButton size="xs" variant="ghost" @click="useCurrentLocation">Use Current</UButton>
              </div>
            </template>
            <div class="space-y-1 max-h-32 overflow-y-auto">
              <div v-if="nearbyLoading" class="text-xs text-gray-500">Loading...</div>
              <div v-else-if="nearbyResults && nearbyResults.length === 0" class="text-xs text-gray-500">
                No nearby points. Click on map to search.
              </div>
              <div
                v-for="(n, i) in nearbyResults || []"
                :key="`nb-${i}`"
                class="p-2 rounded border cursor-pointer text-xs"
                :class="n.source === 'local' ? 'bg-green-50 hover:bg-green-100 border-green-200' : 'hover:bg-gray-50'"
                @click="selectNearby(n)"
              >
                <div class="flex items-center justify-between">
                  <p class="font-medium">{{ n.name }}</p>
                  <UBadge :color="n.source === 'local' ? 'primary' : 'neutral'" size="xs">{{ n.source }}</UBadge>
                </div>
                <p v-if="n.addressText" class="text-gray-500">{{ n.addressText }}</p>
              </div>
            </div>
          </UCard>
        </div>
      </div>
    </UFormField>
  </div>
</template>

<style scoped>
.center-pin {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -100%);
  pointer-events: none;
  z-index: 10;
}

.center-pin .pin {
  width: 18px;
  height: 18px;
  background: #ef4444;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  position: relative;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.9);
}

.center-pin .pin:after {
  content: '';
  width: 8px;
  height: 8px;
  position: absolute;
  border-radius: 50%;
  left: 5px;
  top: 5px;
  background: #fff;
}

.center-coords {
  position: absolute;
  left: 50%;
  bottom: 10px;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 4px;
  z-index: 10;
}
</style>
