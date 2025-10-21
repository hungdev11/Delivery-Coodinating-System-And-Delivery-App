/**
 * useZones Composable
 *
 * Business logic for zone management
 */

import { ref } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { getZones, createZone, updateZone, deleteZone, getCenters } from '../api'
import { ZoneDto, CenterDto, CreateZoneRequest, UpdateZoneRequest } from '../model.type'

export function useZones() {
  const toast = useToast()

  const zones = ref<ZoneDto[]>([])
  const centers = ref<CenterDto[]>([])
  const loading = ref(false)
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
    try {
      const response = await getZones({
        page: page.value,
        size: pageSize.value,
        search: searchQuery.value || undefined,
        centerId: selectedCenterId.value,
      })

      if (response.result) {
        zones.value = response.result.data.map((z) => new ZoneDto(z))
        total.value = response.result.page.totalElements
      }
    } catch (error) {
      console.error('Failed to load zones:', error)
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
        centers.value = response.result.map((c) => new CenterDto(c))
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
}
