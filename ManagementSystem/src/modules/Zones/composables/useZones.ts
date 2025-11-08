/**
 * useZonesStore Pinia Store
 *
 * Business logic for zone management
 */

import { ref } from 'vue'
import { defineStore } from 'pinia'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { getZonesV2, createZone, updateZone, deleteZone, getCenters } from '../api'
import { ZoneDto, CenterDto, CreateZoneRequest, UpdateZoneRequest } from '../model.type'
import type { FilterCondition, FilterGroup, SortConfig } from '@/common/types/filter'
import { convertV1ToV2Filter } from '@/common/utils/filter-v2-converter'

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
  const filters = ref<FilterGroup | undefined>(undefined)
  const sorts = ref<SortConfig[]>([])

  /**
   * Load zones
   */
  const loadZones = async (query?: {
    filters?: FilterGroup
    sorts?: SortConfig[]
    page?: number
    size?: number
    search?: string
  }) => {
    loading.value = true
    error.value = null
    try {
      const filtersToUse = query?.filters ?? filters.value
      const sortsToUse = query?.sorts ?? sorts.value
      const pageToUse = query?.page ?? page.value
      const sizeToUse = query?.size ?? pageSize.value
      const searchToUse = query?.search ?? searchQuery.value

      const mergeWithCenterFilter = (base?: FilterGroup): FilterGroup | undefined => {
        if (!selectedCenterId.value) {
          return base
        }

        const centerCondition: FilterCondition = {
          field: 'centerId',
          operator: 'eq',
          value: selectedCenterId.value,
        }

        if (!base) {
          return {
            logic: 'AND',
            conditions: [centerCondition],
          }
        }

        return {
          logic: 'AND',
          conditions: [
            {
              logic: base.logic,
              conditions: [...base.conditions],
            },
            centerCondition,
          ],
        }
      }

      const combinedFilters = mergeWithCenterFilter(filtersToUse)

      const v2Filters = combinedFilters && combinedFilters.conditions.length > 0
        ? convertV1ToV2Filter(combinedFilters)
        : undefined

      const response = await getZonesV2({
        page: pageToUse,
        size: sizeToUse,
        search: searchToUse || undefined,
        filters: v2Filters,
        sorts: sortsToUse && sortsToUse.length > 0 ? sortsToUse : undefined,
      })

      const result = response.result

      if (result?.data) {
        zones.value = result.data.map((zone) => new ZoneDto(zone))
      } else {
        zones.value = []
      }

      if (result?.page) {
        page.value = result.page.page
        pageSize.value = result.page.size
        total.value = result.page.totalElements
      } else {
        total.value = zones.value.length
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

  const setFilters = (filterGroup?: FilterGroup) => {
    if (filterGroup && filterGroup.conditions.length === 0) {
      filters.value = undefined
    } else {
      filters.value = filterGroup
    }
  }

  const setSorts = (sortConfigs: SortConfig[]) => {
    sorts.value = sortConfigs
  }

  const setPage = (pageIndex: number) => {
    page.value = pageIndex
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
    filters,
    sorts,
    loadZones,
    loadCenters,
    create,
    update,
    remove,
    bulkDelete,
    handlePageChange,
    handleSearch,
    filterByCenter,
    setFilters,
    setSorts,
    setPage,
  }
})
