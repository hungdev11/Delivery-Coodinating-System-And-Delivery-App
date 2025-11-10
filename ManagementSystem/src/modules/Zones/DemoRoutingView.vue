<script setup lang="ts">
/**
 * Demo Routing View
 *
 * Demo page for testing priority-based routing
 */

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import type { RadioGroupItem } from '@nuxt/ui'
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
  routingMode,
  routingStrategy,
  vehicleType,
} = storeToRefs(routingStore)

const {
  setStartPoint,
  addWaypoint,
  clearPriorityGroup,
  reset,
  calculate,
  getPriorityLabel,
  getPriorityColor,
} = routingStore

// UI State
const mapLoaded = ref(false)
const selectedPriority = ref<PriorityLevelType>(8) // Default: P8 EXPRESS
const useLegacyPriority = ref(false) // Toggle between 1-10 and 0-4 scale
const clickMode = ref<'start' | 'waypoint'>('start')
const selectedStep = ref<{ legIndex: number; stepIndex: number } | null>(null)
const mapViewRef = ref<InstanceType<typeof MapView>>()

// Priority options for selector (1-10 scale)
const priorityOptions = computed(() => [
  // High Priority (9-10)
  { label: PriorityLabel[10], value: 10 },
  { label: PriorityLabel[9], value: 9 },

  // Express (7-8)
  { label: PriorityLabel[8], value: 8 },
  { label: PriorityLabel[7], value: 7 },

  // Normal (4-6)
  { label: PriorityLabel[6], value: 6 },
  { label: PriorityLabel[5], value: 5 },
  { label: PriorityLabel[4], value: 4 },

  // Economy (2-3)
  { label: PriorityLabel[3], value: 3 },
  { label: PriorityLabel[2], value: 2 },

  // Low (1)
  { label: PriorityLabel[1], value: 1 },
])

// Legacy priority options (for backward compatibility)
const legacyPriorityOptions = computed(() => [
  { label: PriorityLabel[PriorityLevel.URGENT], value: PriorityLevel.URGENT },
  { label: PriorityLabel[PriorityLevel.EXPRESS], value: PriorityLevel.EXPRESS },
  { label: PriorityLabel[PriorityLevel.FAST], value: PriorityLevel.FAST },
  { label: PriorityLabel[PriorityLevel.NORMAL], value: PriorityLevel.NORMAL },
  { label: PriorityLabel[PriorityLevel.ECONOMY], value: PriorityLevel.ECONOMY },
])

// Click mode options for radio group
const clickModeItems = ref<RadioGroupItem[]>([
  { label: 'Set Start Point', value: 'start' },
  { label: 'Add Waypoint', value: 'waypoint' },
])

// Strategy options for radio group
const strategyItems = ref<RadioGroupItem[]>([
  { label: '🚨 Strict Urgent (URGENT phải giao đầu tiên)', value: 'strict_urgent' },
  { label: '🎯 Flexible (cân nhắc tất cả priority)', value: 'flexible' },
])

// Map configuration
const mapConfig = {
  center: [106.660172, 10.762622] as [number, number], // Ho Chi Minh City
  zoom: 12,
  style: `https://api.maptiler.com/maps/streets/style.json?key=${import.meta.env.VITE_MAPTILER_API_KEY || 'get_your_own_OpIi9ZULNHzrESv6T2vL'}`,
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
      const parcelId = waypoint.parcelId || ''
      const title = parcelId
        ? `${getPriorityLabel(group.priority)} - Parcel: ${parcelId}`
        : `${getPriorityLabel(group.priority)} - ${index + 1}`
      const popup = parcelId
        ? `<strong>${getPriorityLabel(group.priority)}</strong><br>Parcel ID: ${parcelId}<br>Priority: ${index + 1}`
        : `<strong>${getPriorityLabel(group.priority)}</strong><br>Priority: ${index + 1}`

      markers.push({
        id: `waypoint-${group.priority}-${index}`,
        coordinates: [waypoint.lon, waypoint.lat],
        type: 'custom',
        title,
        color: getPriorityColor(group.priority),
        label: parcelId ? parcelId.substring(0, 6) : `${index + 1}`, // Show first 6 chars of parcelId or index
        popup,
        parcelId: parcelId || undefined, // Store parcelId for reference
      })
    })
  })

  return markers
})

// Convert route result to map route data
const mapRoutes = computed((): RouteData[] => {
  if (!routeResult.value) return []

  // If a step is selected, show only that step
  if (selectedStep.value) {
    const { legIndex, stepIndex } = selectedStep.value
    const leg = routeResult.value.route.legs[legIndex]
    if (!leg) return []

    const step = leg.steps[stepIndex]
    if (!step || !step.geometry || !Array.isArray(step.geometry.coordinates)) return []

    return [
      {
        coordinates: step.geometry.coordinates as [number, number][],
        distance: step.distance,
        duration: step.duration,
        properties: {
          color: '#f59e0b', // Amber color for highlighted step
          width: 6,
          opacity: 1,
        },
      },
    ]
  }

  // Show full route when no step is selected
  const stepCoords: [number, number][] = []
  try {
    routeResult.value.route.legs.forEach((leg) => {
      leg.steps.forEach((step) => {
        if (step.geometry && Array.isArray(step.geometry.coordinates)) {
          stepCoords.push(...(step.geometry.coordinates as [number, number][]))
        }
      })
    })
  } catch {}

  const coords = stepCoords.length > 0
    ? stepCoords
    : parseRouteGeometry(routeResult.value.route.geometry)

  if (coords.length === 0) return []

  return [
    {
      coordinates: coords,
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
 * Generate a random parcel ID
 */
const generateRandomParcelId = (): string => {
  // Generate a more readable format: PARCEL-XXXXXX (6 random alphanumeric chars)
  const randomChars = Math.random().toString(36).substring(2, 8).toUpperCase()
  return `PARCEL-${randomChars}`
}

/**
 * Handle map click - set start point or add waypoint
 */
const handleMapClick = (data: { lngLat: [number, number] }) => {
  const waypoint: Waypoint = {
    lat: data.lngLat[1],
    lon: data.lngLat[0],
    // Always assign a random parcelId for new waypoints
    parcelId: generateRandomParcelId(),
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

/**
 * Handle step click - highlight and focus on the step
 */
const handleStepClick = (legIndex: number, stepIndex: number) => {
  // Toggle selection - if same step clicked, deselect and zoom out
  if (
    selectedStep.value &&
    selectedStep.value.legIndex === legIndex &&
    selectedStep.value.stepIndex === stepIndex
  ) {
    selectedStep.value = null
    zoomOutToFullRoute()
    return
  }

  // Select new step
  selectedStep.value = { legIndex, stepIndex }

  // Get step geometry to calculate bounds
  if (!routeResult.value) return
  const leg = routeResult.value.route.legs[legIndex]
  if (!leg) return

  const step = leg.steps[stepIndex]
  if (!step || !step.geometry || !Array.isArray(step.geometry.coordinates)) return

  // Calculate bounds from step coordinates
  const coords = step.geometry.coordinates as [number, number][]
  if (coords.length === 0) return

  // Get bounding box
  let minLng = coords[0][0]
  let maxLng = coords[0][0]
  let minLat = coords[0][1]
  let maxLat = coords[0][1]

  coords.forEach(([lng, lat]) => {
    minLng = Math.min(minLng, lng)
    maxLng = Math.max(maxLng, lng)
    minLat = Math.min(minLat, lat)
    maxLat = Math.max(maxLat, lat)
  })

  // Calculate bearing from start to end of step for direction
  const startCoord = coords[0]
  const endCoord = coords[coords.length - 1]
  const bearing = calculateBearing(startCoord, endCoord)

  // Access map instance through MapView ref and zoom to step bounds
  setTimeout(() => {
    const mapInstance = mapViewRef.value?.map
    if (mapInstance) {
      // Fit bounds to the step with minimal padding for very close-up view
      mapInstance.fitBounds(
        [
          [minLng, minLat],
          [maxLng, maxLat],
        ],
        {
          padding: 30, // Minimal padding for very close view
          bearing: bearing, // Rotate to direction of travel
          pitch: 60, // Higher tilt for better 3D view
          duration: 1500, // Smooth animation
          maxZoom: 19, // Very high zoom for street-level detail
        }
      )
    }
  }, 100)
}

/**
 * Calculate bearing between two points (in degrees)
 */
const calculateBearing = (start: [number, number], end: [number, number]): number => {
  const startLng = (start[0] * Math.PI) / 180
  const startLat = (start[1] * Math.PI) / 180
  const endLng = (end[0] * Math.PI) / 180
  const endLat = (end[1] * Math.PI) / 180

  const dLng = endLng - startLng

  const y = Math.sin(dLng) * Math.cos(endLat)
  const x = Math.cos(startLat) * Math.sin(endLat) - Math.sin(startLat) * Math.cos(endLat) * Math.cos(dLng)

  const bearing = Math.atan2(y, x)
  return ((bearing * 180) / Math.PI + 360) % 360
}

/**
 * Get total number of steps across all legs
 */
const getTotalSteps = computed(() => {
  if (!routeResult.value) return 0
  return routeResult.value.route.legs.reduce((sum, leg) => sum + leg.steps.length, 0)
})

/**
 * Get current step number (1-indexed)
 */
const getCurrentStepNumber = computed(() => {
  if (!selectedStep.value || !routeResult.value) return 0

  let stepNumber = 0
  for (let i = 0; i < routeResult.value.route.legs.length; i++) {
    if (i < selectedStep.value.legIndex) {
      stepNumber += routeResult.value.route.legs[i].steps.length
    } else if (i === selectedStep.value.legIndex) {
      stepNumber += selectedStep.value.stepIndex + 1
      break
    }
  }
  return stepNumber
})

/**
 * Navigate to a specific step by global step number (0-indexed)
 */
const navigateToStepNumber = (globalStepIndex: number) => {
  if (!routeResult.value) return

  let currentIndex = 0
  for (let legIndex = 0; legIndex < routeResult.value.route.legs.length; legIndex++) {
    const leg = routeResult.value.route.legs[legIndex]
    if (currentIndex + leg.steps.length > globalStepIndex) {
      const stepIndex = globalStepIndex - currentIndex
      handleStepClick(legIndex, stepIndex)
      return
    }
    currentIndex += leg.steps.length
  }
}

/**
 * Start the step-by-step navigation (go to first step)
 */
const startStepByStep = () => {
  navigateToStepNumber(0)
}

/**
 * Go to next step
 */
const nextStep = () => {
  if (!selectedStep.value) {
    startStepByStep()
    return
  }

  const currentGlobalIndex = getCurrentStepNumber.value - 1
  if (currentGlobalIndex < getTotalSteps.value - 1) {
    navigateToStepNumber(currentGlobalIndex + 1)
  }
}

/**
 * Go to previous step
 */
const previousStep = () => {
  if (!selectedStep.value) return

  const currentGlobalIndex = getCurrentStepNumber.value - 1
  if (currentGlobalIndex > 0) {
    navigateToStepNumber(currentGlobalIndex - 1)
  }
}

/**
 * Check if we can go to previous step
 */
const canGoPrevious = computed(() => {
  return selectedStep.value && getCurrentStepNumber.value > 1
})

/**
 * Check if we can go to next step
 */
const canGoNext = computed(() => {
  return selectedStep.value && getCurrentStepNumber.value < getTotalSteps.value
})

/**
 * Zoom out to show the full route
 */
const zoomOutToFullRoute = () => {
  if (!routeResult.value) return

  // Collect all coordinates from the full route
  const allCoords: [number, number][] = []

  routeResult.value.route.legs.forEach((leg) => {
    leg.steps.forEach((step) => {
      if (step.geometry && Array.isArray(step.geometry.coordinates)) {
        allCoords.push(...(step.geometry.coordinates as [number, number][]))
      }
    })
  })

  if (allCoords.length === 0) return

  // Calculate bounds
  let minLng = allCoords[0][0]
  let maxLng = allCoords[0][0]
  let minLat = allCoords[0][1]
  let maxLat = allCoords[0][1]

  allCoords.forEach(([lng, lat]) => {
    minLng = Math.min(minLng, lng)
    maxLng = Math.max(maxLng, lng)
    minLat = Math.min(minLat, lat)
    maxLat = Math.max(maxLat, lat)
  })

  // Fit to bounds with padding
  setTimeout(() => {
    const mapInstance = mapViewRef.value?.map
    if (mapInstance) {
      mapInstance.fitBounds(
        [
          [minLng, minLat],
          [maxLng, maxLat],
        ],
        {
          padding: 50,
          bearing: 0, // Reset bearing to north
          pitch: 0, // Reset pitch to flat
          duration: 1500, // Smooth animation
        }
      )
    }
  }, 100)
}

/**
 * Handle keyboard navigation
 */
const handleKeydown = (event: KeyboardEvent) => {
  // Only handle arrow keys when route result exists
  if (!routeResult.value) return

  // Prevent default behavior for arrow keys
  if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
    event.preventDefault()
  }

  switch (event.key) {
    case 'ArrowLeft':
      if (canGoPrevious.value) {
        previousStep()
      }
      break
    case 'ArrowRight':
      if (canGoNext.value) {
        nextStep()
      } else if (!selectedStep.value) {
        // If no step selected, start from first step
        startStepByStep()
      }
      break
  }
}

// Load initial data on mount
onMounted(() => {
  console.log('Demo Routing View mounted')

  // Add keyboard event listener
  window.addEventListener('keydown', handleKeydown)
})

// Cleanup on unmount
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
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
    <div class="grid grid-cols-1 lg:grid-cols-4 gap-4 h-[700px]">
      <!-- Sidebar Controls -->
      <div class="lg:col-span-1 space-y-4 overflow-y-auto">
        <!-- Routing Strategy (URGENT handling) -->
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">🚨 URGENT Strategy</h3>
          </template>

          <div class="space-y-3">
            <URadioGroup
              v-model="routingStrategy"
              :items="strategyItems"
            />
            <div class="text-xs text-gray-500">
              <span v-if="routingStrategy === 'strict_urgent'">
                URGENT orders MUST be delivered first, regardless of detour cost
              </span>
              <span v-else>
                Consider all priorities together, URGENT gets very high weight
              </span>
            </div>
          </div>
        </UCard>

        <!-- Vehicle Type Selector -->
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">Vehicle Type</h3>
          </template>

          <div class="space-y-3">
            <URadioGroup
              v-model="vehicleType"
              :items="[
                { label: '🏍️ Motorbike (Xe máy)', value: 'motorbike' },
                { label: '🚗 Car (Ô tô)', value: 'car' }
              ]"
            />
            <div class="text-xs text-gray-500">
              <span v-if="vehicleType === 'motorbike'">Motorbike routing - optimized for Vietnam traffic, no motorways</span>
              <span v-else>Car routing - full OSRM car.lua profile with dynamic weights (priority_factor, rating, flow)</span>
            </div>
          </div>
        </UCard>

        <!-- Routing Mode Selector -->
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">Routing Mode (V2)</h3>
          </template>

          <div class="space-y-3">
            <URadioGroup
              v-model="routingMode"
              :items="[
                { label: '⭐ V2 Full (Rating→Weight, Blocking→Speed)', value: 'v2-full' },
                { label: '👥 V2 Rating Only (User Feedback→Weight)', value: 'v2-rating-only' },
                { label: '🚦 V2 Blocking Only (Traffic→Speed)', value: 'v2-blocking-only' },
                { label: '🏍️ V2 Base (VN Motorbike Optimized)', value: 'v2-base' }
              ]"
            />

            <div class="text-xs text-gray-500 mt-3 p-2 bg-gray-50 dark:bg-gray-800 rounded">
              <span v-if="routingMode === 'v2-full'">✨ <strong>V2 Full:</strong> User rating affects weight (cost), traffic blocking affects speed (time). Best for production use.</span>
              <span v-else-if="routingMode === 'v2-rating-only'">✨ <strong>V2 Rating:</strong> Only user feedback affects weight, traffic conditions ignored. Useful for testing user feedback impact.</span>
              <span v-else-if="routingMode === 'v2-blocking-only'">✨ <strong>V2 Blocking:</strong> Only traffic affects speed, user feedback ignored. Useful for testing traffic impact.</span>
              <span v-else-if="routingMode === 'v2-base'">✨ <strong>V2 Base:</strong> VN motorbike profile (35km/h, easy turns, flexible oneways). No user feedback or traffic data.</span>
            </div>
          </div>
        </UCard>

        <!-- Click Mode Selector -->
        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <h3 class="text-lg font-semibold">Click Mode</h3>
              <UToggle
                v-model="useLegacyPriority"
                size="xs"
                color="neutral"
              >
                <template #label>
                  <span class="text-xs text-gray-500">Legacy (0-4)</span>
                </template>
              </UToggle>
            </div>
          </template>

          <div class="space-y-3">
            <URadioGroup v-model="clickMode" :items="clickModeItems" />

            <UFormField
              v-if="clickMode === 'waypoint'"
              :label="useLegacyPriority ? 'Priority Level (Legacy 0-4)' : 'Priority Level (1-10)'"
            >
              <USelect
                v-model="selectedPriority"
                :items="useLegacyPriority ? legacyPriorityOptions : priorityOptions"
              />
              <template #help>
                <span v-if="useLegacyPriority" class="text-xs text-gray-500">
                  Legacy scale: will be converted to 1-10 (0→10, 1→8, 2→6, 3→4, 4→2)
                </span>
                <span v-else class="text-xs text-gray-500">
                  New scale: Higher number = Higher priority (10=URGENT, 1=LOW)
                </span>
              </template>
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
              <span class="text-sm font-medium">{{ getPriorityLabel(group.priority) }}</span>
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
                  @click="clearPriorityGroup(group.priority)"
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

        <!-- Route Steps Details -->
        <UCard v-if="routeResult">
          <template #header>
            <div class="space-y-2">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <h3 class="text-lg font-semibold">Route Steps</h3>
                  <UBadge color="primary">
                    {{ routeResult.route.legs.reduce((sum, leg) => sum + leg.steps.length, 0) }} steps
                  </UBadge>
                </div>
              <UButton
                v-if="selectedStep"
                color="warning"
                variant="soft"
                size="xs"
                icon="i-heroicons-x-mark"
                @click="() => { selectedStep = null; zoomOutToFullRoute() }"
              >
                Clear
              </UButton>
              </div>
              <p class="text-xs text-gray-500 dark:text-gray-400">
                Click on any step to highlight and focus on the map
              </p>
            </div>
          </template>

          <div class="space-y-2 max-h-96 overflow-y-auto">
            <template v-for="(leg, legIndex) in routeResult.route.legs" :key="legIndex">
              <!-- Leg Header -->
              <div class="sticky top-0 bg-gray-50 dark:bg-gray-800 px-3 py-2 -mx-3 font-medium text-sm">
                Leg {{ legIndex + 1 }}
                <span class="text-gray-500 text-xs ml-2">
                  {{ formatDistance(leg.distance) }} · {{ formatDuration(leg.duration) }}
                  <span v-if="leg.parcelId" class="ml-2 text-primary-600 dark:text-primary-400">
                    (Parcel: {{ leg.parcelId }})
                  </span>
                </span>
              </div>

              <!-- Steps in this leg -->
              <div
                v-for="(step, stepIndex) in leg.steps"
                :key="`${legIndex}-${stepIndex}`"
                class="flex items-start gap-3 p-2 rounded cursor-pointer transition-all"
                :class="{
                  'bg-amber-50 dark:bg-amber-900/20 ring-2 ring-amber-400 dark:ring-amber-600':
                    selectedStep?.legIndex === legIndex && selectedStep?.stepIndex === stepIndex,
                  'hover:bg-gray-50 dark:hover:bg-gray-800':
                    !(selectedStep?.legIndex === legIndex && selectedStep?.stepIndex === stepIndex),
                }"
                @click="handleStepClick(legIndex, stepIndex)"
              >
                <!-- Step Number -->
                <div
                  class="flex-shrink-0 w-6 h-6 rounded-full flex items-center justify-center text-xs font-medium transition-colors"
                  :class="{
                    'bg-amber-100 dark:bg-amber-900 text-amber-600 dark:text-amber-400':
                      selectedStep?.legIndex === legIndex && selectedStep?.stepIndex === stepIndex,
                    'bg-primary-100 dark:bg-primary-900 text-primary-600 dark:text-primary-400':
                      !(selectedStep?.legIndex === legIndex && selectedStep?.stepIndex === stepIndex),
                  }"
                >
                  {{ stepIndex + 1 }}
                </div>

                <!-- Step Details -->
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {{ step.name || 'Continue' }}
                  </p>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    {{ formatDistance(step.distance) }} · {{ formatDuration(step.duration) }}
                  </p>
                  <div class="flex items-center gap-2 mt-1">
                    <UBadge
                      v-if="step.maneuver?.type"
                      color="neutral"
                      size="xs"
                    >
                      {{ step.maneuver.type }}
                    </UBadge>
                    <UBadge
                      v-if="step.maneuver?.modifier"
                      color="neutral"
                      size="xs"
                      variant="soft"
                    >
                      {{ step.maneuver.modifier }}
                    </UBadge>
                  </div>
                </div>

                <!-- Selected Indicator -->
                <div
                  v-if="selectedStep?.legIndex === legIndex && selectedStep?.stepIndex === stepIndex"
                  class="flex-shrink-0 text-amber-500 dark:text-amber-400"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                  </svg>
                </div>
              </div>
            </template>
          </div>
        </UCard>
      </div>

      <!-- Map Container -->
      <div class="lg:col-span-3 flex flex-col gap-4">
        <div class="map-container" style="height: 700px">
          <MapView
            ref="mapViewRef"
            :config="mapConfig"
            :markers="mapMarkers"
            :routes="mapRoutes"
            :loading="loading"
            :auto-fit="!selectedStep"
            :fit-padding="50"
            :show-zones="false"
            :show-routing="true"
            height="700px"
            @map-loaded="handleMapLoaded"
            @map-click="handleMapClick"
          />
        </div>

        <!-- Step Navigation Controls -->
        <UCard v-if="routeResult" class="step-navigation">
          <div class="flex items-center justify-between gap-4 flex-wrap">
            <!-- Start Button -->
            <UButton
              color="primary"
              variant="soft"
              icon="i-heroicons-play"
              :disabled="!routeResult"
              @click="startStepByStep"
            >
              Start
            </UButton>

            <!-- Navigation Controls -->
            <div class="flex items-center gap-3 flex-1 justify-center">
              <!-- Previous Button -->
              <UButton
                color="neutral"
                variant="outline"
                icon="i-heroicons-chevron-left"
                :disabled="!canGoPrevious"
                @click="previousStep"
              >
                Previous
              </UButton>

              <!-- Step Counter -->
              <div class="flex items-center gap-2 min-w-[120px] justify-center">
                <UBadge
                  v-if="selectedStep"
                  color="primary"
                  size="lg"
                  variant="solid"
                >
                  {{ getCurrentStepNumber }} / {{ getTotalSteps }}
                </UBadge>
                <span v-else class="text-sm text-gray-500">
                  {{ getTotalSteps }} steps
                </span>
              </div>

              <!-- Next Button -->
              <UButton
                color="neutral"
                variant="outline"
                icon="i-heroicons-chevron-right"
                trailing
                :disabled="!canGoNext"
                @click="nextStep"
              >
                Next
              </UButton>
            </div>

            <!-- Stop/Reset Button -->
            <UButton
              v-if="selectedStep"
              color="error"
              variant="soft"
              icon="i-heroicons-stop"
              @click="() => { selectedStep = null; zoomOutToFullRoute() }"
            >
              Stop
            </UButton>
            <div v-else style="width: 80px"></div>
          </div>

          <!-- Keyboard Hint -->
          <div class="mt-3 pt-3 border-t">
            <p class="text-xs text-gray-500 dark:text-gray-400 text-center flex items-center justify-center gap-2">
              <span>💡 Use</span>
              <kbd class="px-2 py-1 text-xs font-semibold text-gray-800 dark:text-gray-200 bg-gray-100 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded">←</kbd>
              <kbd class="px-2 py-1 text-xs font-semibold text-gray-800 dark:text-gray-200 bg-gray-100 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded">→</kbd>
              <span>arrow keys to navigate</span>
            </p>
          </div>

          <!-- Current Step Info -->
          <div
            v-if="selectedStep && routeResult"
            class="mt-3 pt-3 border-t flex items-start gap-3"
          >
            <div class="flex-shrink-0 w-8 h-8 rounded-full bg-amber-100 dark:bg-amber-900 text-amber-600 dark:text-amber-400 flex items-center justify-center text-sm font-bold">
              {{ getCurrentStepNumber }}
            </div>
            <div class="flex-1">
              <p class="text-sm font-semibold text-gray-900 dark:text-gray-100">
                {{
                  routeResult.route.legs[selectedStep.legIndex]?.steps[selectedStep.stepIndex]
                    ?.name || 'Continue'
                }}
              </p>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                {{
                  formatDistance(
                    routeResult.route.legs[selectedStep.legIndex]?.steps[selectedStep.stepIndex]
                      ?.distance || 0
                  )
                }}
                ·
                {{
                  formatDuration(
                    routeResult.route.legs[selectedStep.legIndex]?.steps[selectedStep.stepIndex]
                      ?.duration || 0
                  )
                }}
              </p>
            </div>
          </div>
        </UCard>
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
