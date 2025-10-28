<script setup lang="ts">
/**
 * Demo Routing View
 *
 * Demo page for testing priority-based routing
 */

import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { PageHeader } from '@/common/components'
import MapView from '@/common/components/MapView.vue'
import { useRoutingStore } from './composables'
import { PriorityLevel, PriorityLabel } from './routing.type'
import type { Waypoint, PriorityLevelType } from './routing.type'
import type { MapMarker, RouteData } from '@/common/types'
import {
  formatDistance,
  formatDuration,
  formatSpeed,
  parseRouteGeometry,
  getCongestionColor,
  getCongestionLabel,
} from './utils/routingHelper'

const router = useRouter()

// Composables
const routingStore = useRoutingStore()
const {
  startPoint,
  priorityGroups,
  routeResult,
  loading,
  error,
  hasStartPoint,
  totalWaypoints,
  canCalculateRoute,
} = storeToRefs(routingStore)

const {
  setStartPoint,
  clearStartPoint,
  addWaypoint,
  removeWaypoint,
  clearPriorityGroup,
  reset,
  calculate,
  getPriorityLabel,
  getPriorityColor,
} = routingStore

// UI State
const mapLoaded = ref(false)
const selectedPriority = ref<PriorityLevelType>(PriorityLevel.EXPRESS)
const clickMode = ref<'start' | 'waypoint'>('start')

// Priority options for selector
const priorityOptions = computed(() => [
  { label: PriorityLabel[PriorityLevel.EXPRESS], value: PriorityLevel.EXPRESS },
  { label: PriorityLabel[PriorityLevel.FAST], value: PriorityLevel.FAST },
  { label: PriorityLabel[PriorityLevel.NORMAL], value: PriorityLevel.NORMAL },
  { label: PriorityLabel[PriorityLevel.ECONOMY], value: PriorityLevel.ECONOMY },
])

// Map configuration
const mapConfig = {
  center: [106.660172, 10.762622] as [number, number], // Ho Chi Minh City
  zoom: 12,
  style: `https://api.maptiler.com/maps/streets/style.json?key=${import.meta.env.MAPTILER_API_KEY || 'get_your_own_OpIi9ZULNHzrESv6T2vL'}`,
}

// Convert waypoints to map markers
const mapMarkers = computed((): MapMarker[] => {
  const markers: MapMarker[] = []

  // Start point marker
  if (startPoint.value) {
    markers.push({
      id: 'start-point',
      coordinates: [startPoint.value.lon, startPoint.value.lat],
      type: 'custom',
      title: 'Start Point',
      color: '#22c55e',
      label: 'Start',
    })
  }

  // Waypoint markers grouped by priority
  priorityGroups.value.forEach((group) => {
    group.waypoints.forEach((waypoint, index) => {
      markers.push({
        id: `waypoint-${group.priority}-${index}`,
        coordinates: [waypoint.lon, waypoint.lat],
        type: 'custom',
        title: `${getPriorityLabel(group.priority as PriorityLevelType)} - ${index + 1}`,
        color: getPriorityColor(group.priority as PriorityLevelType),
        label: `${index + 1}`,
      })
    })
  })

  return markers
})

// Convert route result to map route data
const mapRoutes = computed((): RouteData[] => {
  if (!routeResult.value) return []

  const geometry = parseRouteGeometry(routeResult.value.route.geometry)
  if (geometry.length === 0) return []

  return [
    {
      coordinates: geometry,
      distance: routeResult.value.route.distance,
      duration: routeResult.value.route.duration,
      properties: {
        color: '#3b82f6',
        width: 4,
        opacity: 0.8,
      },
    },
  ]
})

/**
 * Handle map click - set start point or add waypoint
 */
const handleMapClick = (data: { lngLat: [number, number] }) => {
  const waypoint: Waypoint = {
    lat: data.lngLat[1],
    lon: data.lngLat[0],
  }

  if (clickMode.value === 'start') {
    setStartPoint(waypoint)
  } else {
    addWaypoint(selectedPriority.value, waypoint)
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
 * Reset all data
 */
const handleReset = () => {
  if (confirm('Are you sure you want to reset all data?')) {
    reset()
  }
}

/**
 * Calculate route
 */
const handleCalculate = async () => {
  await calculate()
}

// Load initial data on mount
onMounted(() => {
  console.log('Demo Routing View mounted')
})
</script>

<template>
  <div class="demo-routing-view">
    <!-- Page Header -->
    <PageHeader
      title="Demo Routing"
      description="Test priority-based routing with express/fast/normal/economy groups"
    >
      <template #actions>
        <UButton
          color="neutral"
          variant="outline"
          icon="i-heroicons-arrow-path"
          @click="handleReset"
        >
          Reset
        </UButton>
        <UButton
          color="neutral"
          variant="outline"
          icon="i-heroicons-map"
          @click="router.push('/zones/map')"
        >
          Zones Map
        </UButton>
      </template>
    </PageHeader>

    <!-- Error Alert -->
    <UAlert
      v-if="error"
      color="error"
      variant="soft"
      title="Route calculation failed"
      :description="error"
      class="mb-4"
    />

    <!-- Main Content -->
    <div class="grid grid-cols-1 lg:grid-cols-4 gap-4">
      <!-- Sidebar Controls -->
      <div class="lg:col-span-1 space-y-4">
        <!-- Click Mode Selector -->
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">Click Mode</h3>
          </template>

          <div class="space-y-3">
            <URadioGroup v-model="clickMode">
              <URadio value="start" label="Set Start Point" />
              <URadio value="waypoint" label="Add Waypoint" />
            </URadioGroup>

            <UFormField v-if="clickMode === 'waypoint'" label="Priority Level">
              <USelect v-model="selectedPriority" :items="priorityOptions" />
            </UFormField>
          </div>
        </UCard>

        <!-- Waypoints Summary -->
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">Waypoints</h3>
          </template>

          <div class="space-y-3">
            <!-- Start Point -->
            <div class="flex items-center justify-between">
              <span class="text-sm font-medium">Start Point</span>
              <UBadge :color="hasStartPoint ? 'success' : 'neutral'">
                {{ hasStartPoint ? 'Set' : 'Not Set' }}
              </UBadge>
            </div>

            <!-- Priority Groups -->
            <div
              v-for="group in priorityGroups"
              :key="group.priority"
              class="flex items-center justify-between"
            >
              <span class="text-sm font-medium">{{ getPriorityLabel(group.priority as PriorityLevelType) }}</span>
              <div class="flex items-center gap-2">
                <UBadge :color="group.waypoints.length > 0 ? 'primary' : 'neutral'">
                  {{ group.waypoints.length }}
                </UBadge>
                <UButton
                  v-if="group.waypoints.length > 0"
                  color="neutral"
                  variant="ghost"
                  size="xs"
                  icon="i-heroicons-x-mark"
                  @click="clearPriorityGroup(group.priority as PriorityLevelType)"
                />
              </div>
            </div>

            <!-- Total -->
            <div class="pt-3 border-t flex items-center justify-between">
              <span class="text-sm font-semibold">Total Waypoints</span>
              <UBadge color="primary" size="lg">
                {{ totalWaypoints }}
              </UBadge>
            </div>
          </div>
        </UCard>

        <!-- Calculate Button -->
        <UButton
          color="primary"
          size="lg"
          block
          :loading="loading"
          :disabled="!canCalculateRoute"
          @click="handleCalculate"
        >
          Calculate Route
        </UButton>

        <!-- Route Result Summary -->
        <UCard v-if="routeResult">
          <template #header>
            <h3 class="text-lg font-semibold">Route Summary</h3>
          </template>

          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">Distance</span>
              <span class="text-sm font-medium">
                {{ formatDistance(routeResult.summary.totalDistance) }}
              </span>
            </div>

            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">Duration</span>
              <span class="text-sm font-medium">
                {{ formatDuration(routeResult.summary.totalDuration) }}
              </span>
            </div>

            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">Avg Speed</span>
              <span class="text-sm font-medium">
                {{ formatSpeed(routeResult.route.trafficSummary.averageSpeed) }}
              </span>
            </div>

            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">Traffic</span>
              <UBadge
                :color="
                  routeResult.route.trafficSummary.congestionLevel === 'FREE_FLOW'
                    ? 'success'
                    : routeResult.route.trafficSummary.congestionLevel === 'NORMAL'
                      ? 'primary'
                      : 'warning'
                "
              >
                {{ getCongestionLabel(routeResult.route.trafficSummary.congestionLevel) }}
              </UBadge>
            </div>

            <!-- Priority Breakdown -->
            <div class="pt-3 border-t">
              <p class="text-sm font-medium mb-2">Priority Breakdown</p>
              <div class="space-y-1">
                <div
                  v-for="(count, label) in routeResult.summary.priorityCounts"
                  :key="label"
                  class="flex items-center justify-between text-xs"
                >
                  <span class="capitalize">{{ label }}</span>
                  <span>{{ count }} points</span>
                </div>
              </div>
            </div>
          </div>
        </UCard>
      </div>

      <!-- Map Container -->
      <div class="lg:col-span-3">
        <div class="map-container" style="height: 700px">
          <MapView
            :config="mapConfig"
            :markers="mapMarkers"
            :routes="mapRoutes"
            :loading="loading"
            :auto-fit="true"
            :fit-padding="50"
            :show-zones="false"
            :show-routing="true"
            height="700px"
            @map-loaded="handleMapLoaded"
            @map-click="handleMapClick"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.demo-routing-view {
  padding: 1rem;
}

.map-container {
  border-radius: 0.5rem;
  overflow: hidden;
  box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1);
}
</style>
