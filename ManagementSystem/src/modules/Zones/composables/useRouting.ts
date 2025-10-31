/**
 * useRoutingStore Pinia Store
 *
 * State management for demo routing functionality
 */

import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { calculateDemoRoute } from '../routing.api'
import type {
  Waypoint,
  PriorityGroup,
  DemoRouteResponse,
  PriorityLevelType,
} from '../routing.type'
import { PriorityLevel, PriorityLabel } from '../routing.type'

export const useRoutingStore = defineStore('routing', () => {
  const toast = useToast()

  // State
  const startPoint = ref<Waypoint | null>(null)
  const priorityGroups = ref<PriorityGroup[]>([
    { priority: PriorityLevel.EXPRESS, waypoints: [] },
    { priority: PriorityLevel.FAST, waypoints: [] },
    { priority: PriorityLevel.NORMAL, waypoints: [] },
    { priority: PriorityLevel.ECONOMY, waypoints: [] },
  ])
  const routeResult = ref<DemoRouteResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Computed
  const hasStartPoint = computed(() => startPoint.value !== null)
  const totalWaypoints = computed(() => {
    return priorityGroups.value.reduce((sum, group) => sum + group.waypoints.length, 0)
  })
  const canCalculateRoute = computed(() => hasStartPoint.value && totalWaypoints.value > 0)

  /**
   * Set start point
   */
  const setStartPoint = (waypoint: Waypoint) => {
    startPoint.value = waypoint
  }

  /**
   * Clear start point
   */
  const clearStartPoint = () => {
    startPoint.value = null
  }

  /**
   * Add waypoint to priority group
   */
  const addWaypoint = (priority: PriorityLevelType, waypoint: Waypoint) => {
    const group = priorityGroups.value.find((g) => g.priority === priority)
    if (group) {
      group.waypoints.push(waypoint)
    }
  }

  /**
   * Remove waypoint from priority group
   */
  const removeWaypoint = (priority: PriorityLevelType, index: number) => {
    const group = priorityGroups.value.find((g) => g.priority === priority)
    if (group) {
      group.waypoints.splice(index, 1)
    }
  }

  /**
   * Clear all waypoints in a priority group
   */
  const clearPriorityGroup = (priority: PriorityLevelType) => {
    const group = priorityGroups.value.find((g) => g.priority === priority)
    if (group) {
      group.waypoints = []
    }
  }

  /**
   * Clear all waypoints
   */
  const clearAllWaypoints = () => {
    priorityGroups.value.forEach((group) => {
      group.waypoints = []
    })
  }

  /**
   * Reset all data
   */
  const reset = () => {
    startPoint.value = null
    clearAllWaypoints()
    routeResult.value = null
    error.value = null
  }

  /**
   * Calculate route
   */
  const calculate = async () => {
    if (!canCalculateRoute.value) {
      toast.add({
        title: 'Error',
        description: 'Please set start point and add at least one destination',
        color: 'error',
      })
      return
    }

    loading.value = true
    error.value = null

    try {
      // Filter out empty priority groups
      const nonEmptyGroups = priorityGroups.value.filter((g) => g.waypoints.length > 0)

      const result = await calculateDemoRoute({
        startPoint: startPoint.value!,
        priorityGroups: nonEmptyGroups,
        steps: true,
        annotations: true,
      })

      if (result.code === 'Ok') {
        routeResult.value = result
        toast.add({
          title: 'Success',
          description: 'Route calculated successfully',
          color: 'success',
        })
      } else {
        throw new Error('Failed to calculate route')
      }
    } catch (err) {
      console.error('Failed to calculate route:', err)
      error.value = err instanceof Error ? err.message : 'Failed to calculate route'
      toast.add({
        title: 'Error',
        description: 'Failed to calculate route',
        color: 'error',
      })
    } finally {
      loading.value = false
    }
  }

  /**
   * Get priority group label
   */
  const getPriorityLabel = (priority: PriorityLevelType): string => {
    return PriorityLabel[priority] || 'Unknown'
  }

  /**
   * Get priority group color
   */
  const getPriorityColor = (priority: PriorityLevelType): string => {
    const colors: Record<PriorityLevelType, string> = {
      [PriorityLevel.EXPRESS]: '#ef4444', // red
      [PriorityLevel.FAST]: '#f59e0b', // amber
      [PriorityLevel.NORMAL]: '#3b82f6', // blue
      [PriorityLevel.ECONOMY]: '#10b981', // green
    }
    return colors[priority] || '#6b7280'
  }

  return {
    // State
    startPoint,
    priorityGroups,
    routeResult,
    loading,
    error,
    // Computed
    hasStartPoint,
    totalWaypoints,
    canCalculateRoute,
    // Actions
    setStartPoint,
    clearStartPoint,
    addWaypoint,
    removeWaypoint,
    clearPriorityGroup,
    clearAllWaypoints,
    reset,
    calculate,
    getPriorityLabel,
    getPriorityColor,
  }
})
