<script setup lang="ts">
/**
 * MapView Component
 *
 * Reusable map component with support for:
 * - Zone polygons
 * - Routing visualization
 * - Traffic overlay
 * - Custom markers
 * - Layer management
 */

import { ref, onMounted, watch, computed, type PropType } from 'vue'
import { useMap } from '@/common/composables/useMap'
import type {
  MapConfig,
  MapMarker,
  ZonePolygon,
  RouteData,
  TrafficData,
  MapViewState,
  ZoneLayerOptions,
  RoutingLayerOptions,
  TrafficLayerOptions,
} from '@/common/types/map.type'
import 'maplibre-gl/dist/maplibre-gl.css'

interface Props {
  // Map configuration
  config?: Partial<MapConfig>
  height?: string
  width?: string

  // Layer data
  zones?: ZonePolygon[]
  routes?: RouteData[]
  traffic?: TrafficData[]
  markers?: MapMarker[]

  // Layer options
  zoneOptions?: ZoneLayerOptions
  routingOptions?: RoutingLayerOptions
  trafficOptions?: TrafficLayerOptions

  // Layer visibility
  showZones?: boolean
  showRouting?: boolean
  showTraffic?: boolean

  // Auto-fit bounds
  autoFit?: boolean
  fitPadding?: number

  // Loading state
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  height: '600px',
  width: '100%',
  showZones: true,
  showRouting: true,
  showTraffic: false,
  autoFit: true,
  fitPadding: 50,
  loading: false,
})

interface Emits {
  (e: 'map-loaded'): void
  (e: 'map-click', data: { lngLat: [number, number]; point: [number, number] }): void
  (e: 'zone-click', data: { zoneId: string; zoneName: string; lngLat: [number, number] }): void
  (e: 'view-change', state: MapViewState): void
}

const emit = defineEmits<Emits>()

// Map instance
const mapContainer = ref<HTMLDivElement>()
const {
  map,
  loaded,
  initMap,
  addMarker,
  addZoneLayer,
  addRoutingLayer,
  addTrafficLayer,
  toggleLayer,
  fitBounds,
  clearMarkers,
} = useMap()

// Loading state
const isInitializing = ref(true)

/**
 * Initialize map on mount
 */
onMounted(async () => {
  if (!mapContainer.value) return

  try {
    const mapInstance = await initMap(mapContainer.value, props.config)

    // Map click handler
    mapInstance.on('click', (e) => {
      emit('map-click', {
        lngLat: [e.lngLat.lng, e.lngLat.lat],
        point: [e.point.x, e.point.y],
      })
    })

    // Zone click handler
    mapInstance.on('click', 'zones-layer', (e) => {
      const feature = e.features?.[0]
      if (feature?.properties) {
        emit('zone-click', {
          zoneId: feature.properties.id,
          zoneName: feature.properties.name,
          lngLat: [e.lngLat.lng, e.lngLat.lat],
        })
      }
    })

    // Change cursor on hover
    mapInstance.on('mouseenter', 'zones-layer', () => {
      mapInstance.getCanvas().style.cursor = 'pointer'
    })

    mapInstance.on('mouseleave', 'zones-layer', () => {
      mapInstance.getCanvas().style.cursor = ''
    })

    // View change handler
    mapInstance.on('moveend', () => {
      const center = mapInstance.getCenter()
      emit('view-change', {
        center: [center.lng, center.lat],
        zoom: mapInstance.getZoom(),
        bearing: mapInstance.getBearing(),
        pitch: mapInstance.getPitch(),
      })
    })

    emit('map-loaded')
    isInitializing.value = false

    // Initial data load
    updateLayers()
  } catch (error) {
    console.error('Failed to initialize map:', error)
    isInitializing.value = false
  }
})

/**
 * Update all layers
 */
const updateLayers = () => {
  if (!loaded.value) return

  // Update zones
  if (props.zones && props.zones.length > 0 && props.showZones) {
    addZoneLayer(props.zones, props.zoneOptions)
  }

  // Update routing
  if (props.routes && props.routes.length > 0 && props.showRouting) {
    addRoutingLayer(props.routes, props.routingOptions)
  }

  // Update traffic
  if (props.traffic && props.traffic.length > 0 && props.showTraffic) {
    addTrafficLayer(props.traffic, props.trafficOptions)
  }

  // Update markers
  clearMarkers()
  if (props.markers) {
    props.markers.forEach((marker) => addMarker(marker))
  }

  // Auto-fit bounds
  if (props.autoFit) {
    const allCoordinates: [number, number][] = []

    // Collect all coordinates
    props.zones?.forEach((zone) => {
      zone.coordinates.forEach((ring) => {
        allCoordinates.push(...ring)
      })
    })

    props.routes?.forEach((route) => {
      allCoordinates.push(...(route.coordinates as [number, number][]))
    })

    props.markers?.forEach((marker) => {
      allCoordinates.push(marker.coordinates)
    })

    if (allCoordinates.length > 0) {
      fitBounds(allCoordinates, { padding: props.fitPadding })
    }
  }
}

// Watch for data changes
watch(
  () => [props.zones, props.routes, props.traffic, props.markers],
  () => {
    if (loaded.value) {
      updateLayers()
    }
  },
  { deep: true },
)

// Watch layer visibility
watch(
  () => props.showZones,
  (visible) => {
    toggleLayer('zones-layer', visible)
  },
)

watch(
  () => props.showRouting,
  (visible) => {
    toggleLayer('routing-layer', visible)
  },
)

watch(
  () => props.showTraffic,
  (visible) => {
    toggleLayer('traffic-layer', visible)
  },
)

// Computed loading state
const isLoading = computed(() => props.loading || isInitializing.value)

/**
 * Expose map instance for parent components
 */
defineExpose({
  map,
  loaded,
})
</script>

<template>
  <div class="map-wrapper" :style="{ width, height }">
    <!-- Loading overlay -->
    <div v-if="isLoading" class="map-loading">
      <UIcon name="i-heroicons-arrow-path" class="w-8 h-8 animate-spin text-primary-500" />
      <p class="mt-2 text-sm text-gray-600">Loading map...</p>
    </div>

    <!-- Map container -->
    <div ref="mapContainer" class="map-container" :style="{ width, height }" />

    <!-- Control panel slot -->
    <div v-if="$slots.controls" class="map-controls">
      <slot name="controls" />
    </div>

    <!-- Legend slot -->
    <div v-if="$slots.legend" class="map-legend">
      <slot name="legend" />
    </div>

    <!-- Info panel slot -->
    <div v-if="$slots.info" class="map-info">
      <slot name="info" />
    </div>
  </div>
</template>

<style scoped>
.map-wrapper {
  position: relative;
  overflow: hidden;
  border-radius: 0.5rem;
  border: 1px solid rgb(229 231 235);
}

.map-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.map-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  z-index: 1000;
}

.map-controls {
  position: absolute;
  top: 1rem;
  right: 1rem;
  background: white;
  border-radius: 0.5rem;
  padding: 0.75rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 100;
}

.map-legend {
  position: absolute;
  bottom: 1rem;
  left: 1rem;
  background: white;
  border-radius: 0.5rem;
  padding: 0.75rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 100;
  max-width: 300px;
}

.map-info {
  position: absolute;
  top: 1rem;
  left: 1rem;
  background: white;
  border-radius: 0.5rem;
  padding: 0.75rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 100;
  max-width: 400px;
}

/* Map marker styles */
:deep(.map-marker) {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

:deep(.marker-center) {
  background-color: #3b82f6;
}

:deep(.marker-delivery) {
  background-color: #10b981;
}

:deep(.marker-warehouse) {
  background-color: #f59e0b;
}

:deep(.marker-custom) {
  background-color: #8b5cf6;
}

/* MapLibre popup customization */
:deep(.maplibregl-popup-content) {
  padding: 0.75rem;
  border-radius: 0.5rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

:deep(.maplibregl-popup-close-button) {
  font-size: 1.25rem;
  padding: 0.25rem 0.5rem;
}
</style>
