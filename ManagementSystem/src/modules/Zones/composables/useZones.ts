/**
 * useZonesStore Pinia Store
 *
 * Business logic for zone management
 */

import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { getZones, createZone, updateZone, deleteZone, getCenters } from '../api'
import { ZoneDto, CenterDto, CreateZoneRequest, UpdateZoneRequest } from '../model.type'

export const useZonesStore = defineStore('zones', () => {
  const toast = useToast()

  const zones = ref<ZoneDto[]>([])
  const centers = ref<CenterDto[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const page = ref(0)
  const pageSize = ref(10)
  const total = ref(0)
  const searchQuery = ref('')
  const selectedCenterId = ref<string | undefined>(undefined)

  /**
   * Load zones
   */
  const loadZones = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await getZones({
        page: page.value,
        size: pageSize.value,
        search: searchQuery.value || undefined,
        centerId: selectedCenterId.value,
      })
      console.log('response', response)
      if (response?.data) {
        zones.value = response.data
        total.value = response.page.totalElements
      }
    } catch (err) {
      console.error('Failed to load zones:', err)
      error.value = err instanceof Error ? err.message : 'Failed to load zones'
      toast.add({
        title: 'Error',
        description: 'Failed to load zones',
        color: 'error',
      })
    } finally {
      loading.value = false
    }
  }

  /**
   * Load centers
   */
  const loadCenters = async () => {
    try {
      const response = await getCenters()
      if (response.result) {
        centers.value = response.result.data.map((c) => new CenterDto(c))
      }
    } catch (error) {
      console.error('Failed to load centers:', error)
    }
  }

  /**
   * Create zone
   */
  const create = async (data: CreateZoneRequest) => {
    try {
      const request = new CreateZoneRequest(data)
      await createZone(request)

      toast.add({
        title: 'Success',
        description: 'Zone created successfully',
        color: 'success',
      })

      loadZones()
      return true
    } catch (error) {
      console.error('Failed to create zone:', error)
      return false
    }
  }

  /**
   * Update zone
   */
  const update = async (id: string, data: UpdateZoneRequest) => {
    try {
      const request = new UpdateZoneRequest(data)
      await updateZone(id, request)

      toast.add({
        title: 'Success',
        description: 'Zone updated successfully',
        color: 'success',
      })

      loadZones()
      return true
    } catch (error) {
      console.error('Failed to update zone:', error)
      return false
    }
  }

  /**
   * Delete zone
   */
  const remove = async (id: string) => {
    try {
      await deleteZone(id)

      toast.add({
        title: 'Success',
        description: 'Zone deleted successfully',
        color: 'success',
      })

      loadZones()
      return true
    } catch (error) {
      console.error('Failed to delete zone:', error)
      return false
    }
  }

  /**
   * Bulk delete zones
   */
  const bulkDelete = async (ids: string[]) => {
    try {
      await Promise.all(ids.map((id) => deleteZone(id)))

      toast.add({
        title: 'Success',
        description: `${ids.length} zone(s) deleted successfully`,
        color: 'success',
      })

      loadZones()
      return true
    } catch (error) {
      console.error('Failed to bulk delete zones:', error)
      return false
    }
  }

  /**
   * Handle page change
   */
  const handlePageChange = (newPage: number) => {
    page.value = newPage
    loadZones()
  }

  /**
   * Handle search
   */
  const handleSearch = (query: string) => {
    searchQuery.value = query
    page.value = 0
    loadZones()
  }

  /**
   * Filter by center
   */
  const filterByCenter = (centerId: string | undefined) => {
    selectedCenterId.value = centerId
    page.value = 0
    loadZones()
  }

  return {
    zones,
    centers,
    loading,
    error,
    page,
    pageSize,
    total,
    searchQuery,
    selectedCenterId,
    loadZones,
    loadCenters,
    create,
    update,
    remove,
    bulkDelete,
    handlePageChange,
    handleSearch,
    filterByCenter,
  }
})
