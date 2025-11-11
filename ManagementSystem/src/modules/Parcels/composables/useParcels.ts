/**
 * useParcels Composable
 *
 * Business logic for parcel management
 */

import { ref } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { getParcelsV2, createParcel, updateParcel, deleteParcel, changeParcelStatus } from '../api'
import { ParcelDto, CreateParcelRequest, UpdateParcelRequest } from '../model.type'
import type { FilterGroup, SortConfig, FilterableColumn } from '@/common/types/filter'
import { createEmptyFilterGroup } from '@/common/utils/query-builder'
import { convertV1ToV2Filter } from '@/common/utils/filter-v2-converter'
import type { FilterGroupItemV2 } from '@/common/types/filter-v2'

export function useParcels() {
  const toast = useToast()

  const parcels = ref<ParcelDto[]>([])
  const loading = ref(false)
  const page = ref(0)
  const pageSize = ref(10)
  const total = ref(0)
  const searchQuery = ref('')

  // New filter/sort state
  const filters = ref<FilterGroup>(createEmptyFilterGroup())
  const sorts = ref<SortConfig[]>([])
  const useAdvancedSearch = ref(true)
  const useV2Api = ref(true) // Use V2 API by default

  /**
   * Load parcels
   */
  const loadParcels = async () => {
    loading.value = true
    try {
      // Convert V1 filter to V2 format if using V2 API
      let filtersToSend: FilterGroup | FilterGroupItemV2 | undefined = undefined

      if (filters.value.conditions.length > 0) {
        if (useV2Api.value) {
          // Convert V1 FilterGroup to V2 FilterGroupItemV2
          filtersToSend = convertV1ToV2Filter(filters.value)
        } else {
          // Use V1 format as-is
          filtersToSend = filters.value
        }
      }

      const params = {
        filters: filtersToSend,
        sorts: sorts.value.length > 0 ? sorts.value : undefined,
        page: page.value,
        size: pageSize.value,
        search: searchQuery.value || undefined,
      }

      // Use V2 API
      const response = await getParcelsV2(params)

      if (response.result) {
        parcels.value = response.result.data.map((p) => new ParcelDto(p))
        total.value = response.result.page.totalElements
      }
    } catch (error) {
      console.error('Failed to load parcels:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to load parcels',
        color: 'error',
      })
    } finally {
      loading.value = false
    }
  }

  /**
   * Create parcel
   */
  const create = async (data: CreateParcelRequest) => {
    try {
      const request = new CreateParcelRequest(data)
      await createParcel(request)

      toast.add({
        title: 'Success',
        description: 'Parcel created successfully',
        color: 'success',
      })

      loadParcels()
      return true
    } catch (error) {
      console.error('Failed to create parcel:', error)
      return false
    }
  }

  /**
   * Update parcel
   */
  const update = async (id: string, data: UpdateParcelRequest) => {
    try {
      const request = new UpdateParcelRequest(data)
      await updateParcel(id, request)

      toast.add({
        title: 'Success',
        description: 'Parcel updated successfully',
        color: 'success',
      })

      loadParcels()
      return true
    } catch (error) {
      console.error('Failed to update parcel:', error)
      return false
    }
  }

  /**
   * Delete parcel
   */
  const remove = async (id: string) => {
    try {
      await deleteParcel(id)

      toast.add({
        title: 'Success',
        description: 'Parcel deleted successfully',
        color: 'success',
      })

      loadParcels()
      return true
    } catch (error) {
      console.error('Failed to delete parcel:', error)
      return false
    }
  }

  /**
   * Change parcel status
   */
  const changeStatus = async (id: string, event: string) => {
    try {
      await changeParcelStatus(id, event)

      toast.add({
        title: 'Success',
        description: 'Parcel status updated successfully',
        color: 'success',
      })

      loadParcels()
      return true
    } catch (error) {
      console.error('Failed to change parcel status:', error)
      return false
    }
  }

  /**
   * Bulk delete parcels
   */
  const bulkDelete = async (ids: string[]) => {
    try {
      await Promise.all(ids.map((id) => deleteParcel(id)))

      toast.add({
        title: 'Success',
        description: `${ids.length} parcel(s) deleted successfully`,
        color: 'success',
      })

      loadParcels()
      return true
    } catch (error) {
      console.error('Failed to bulk delete parcels:', error)
      return false
    }
  }

  /**
   * Handle page change
   */
  const handlePageChange = (newPage: number) => {
    page.value = newPage
    loadParcels()
  }

  /**
   * Handle search
   */
  const handleSearch = (query: string) => {
    searchQuery.value = query
    page.value = 0
    loadParcels()
  }

  /**
   * Update filters
   */
  const updateFilters = (newFilters: FilterGroup) => {
    filters.value = newFilters
    page.value = 0
    loadParcels()
  }

  /**
   * Update sorts
   */
  const updateSorts = (newSorts: SortConfig[]) => {
    sorts.value = newSorts
    console.log('newSorts', newSorts)
    if (useAdvancedSearch.value) {
      loadParcels() // Reload data when using advanced search
    }
  }

  /**
   * Clear all filters
   */
  const clearFilters = () => {
    filters.value = createEmptyFilterGroup()
    page.value = 0
    loadParcels()
  }

  /**
   * Clear all sorts
   */
  const clearSorts = () => {
    sorts.value = []
    loadParcels()
  }

  /**
   * Toggle advanced search mode
   */
  const toggleAdvancedSearch = () => {
    useAdvancedSearch.value = !useAdvancedSearch.value
    if (!useAdvancedSearch.value) {
      // Clear filters when switching to simple mode
      clearFilters()
      clearSorts()
    }
    loadParcels()
  }

  /**
   * Get filterable columns configuration
   */
  const getFilterableColumns = (): FilterableColumn[] => {
    return [
      {
        field: 'code',
        label: 'Code',
        type: 'string',
        caseSensitive: false,
        filterable: true,
        filterType: 'text',
        filterConfig: {
          placeholder: 'Enter parcel code...',
        },
      },
      {
        field: 'senderId',
        label: 'Sender ID',
        type: 'string',
        caseSensitive: false,
        filterable: true,
        filterType: 'text',
        filterConfig: {
          placeholder: 'Enter sender ID...',
        },
      },
      {
        field: 'receiverId',
        label: 'Receiver ID',
        type: 'string',
        caseSensitive: false,
        filterable: true,
        filterType: 'text',
        filterConfig: {
          placeholder: 'Enter receiver ID...',
        },
      },
      {
        field: 'status',
        label: 'Status',
        type: 'enum',
        enumOptions: [
          { label: 'In Warehouse', value: 'IN_WAREHOUSE' },
          { label: 'On Route', value: 'ON_ROUTE' },
          { label: 'Delivered', value: 'DELIVERED' },
          { label: 'Succeeded', value: 'SUCCEEDED' },
          { label: 'Failed', value: 'FAILED' },
          { label: 'Delayed', value: 'DELAYED' },
          { label: 'Dispute', value: 'DISPUTE' },
          { label: 'Lost', value: 'LOST' },
        ],
        filterable: true,
        filterType: 'select',
        filterConfig: {
          placeholder: 'Select status...',
          multiple: false,
        },
      },
      {
        field: 'deliveryType',
        label: 'Delivery Type',
        type: 'enum',
        enumOptions: [
          { label: 'Economy', value: 'ECONOMY' },
          { label: 'Normal', value: 'NORMAL' },
          { label: 'Fast', value: 'FAST' },
          { label: 'Express', value: 'EXPRESS' },
          { label: 'Urgent', value: 'URGENT' },
        ],
        filterable: true,
        filterType: 'select',
        filterConfig: {
          placeholder: 'Select delivery type...',
          multiple: false,
        },
      },
      {
        field: 'weight',
        label: 'Weight',
        type: 'number',
        filterable: true,
        filterType: 'number',
        filterConfig: {
          placeholder: 'Enter weight...',
          min: 0,
          step: 0.1,
        },
      },
      {
        field: 'value',
        label: 'Value',
        type: 'number',
        filterable: true,
        filterType: 'number',
        filterConfig: {
          placeholder: 'Enter value...',
          min: 0,
          step: 0.01,
        },
      },
      {
        field: 'lat',
        label: 'Latitude',
        type: 'number',
        filterable: true,
        filterType: 'number',
        filterConfig: {
          placeholder: 'Enter latitude...',
          step: 0.000001,
        },
      },
      {
        field: 'lon',
        label: 'Longitude',
        type: 'number',
        filterable: true,
        filterType: 'number',
        filterConfig: {
          placeholder: 'Enter longitude...',
          step: 0.000001,
        },
      },
      {
        field: 'createdAt',
        label: 'Created At',
        type: 'date',
        filterable: true,
        filterType: 'date',
        filterConfig: {
          placeholder: 'Select date...',
        },
      },
      {
        field: 'updatedAt',
        label: 'Updated At',
        type: 'date',
        filterable: true,
        filterType: 'date',
        filterConfig: {
          placeholder: 'Select date...',
        },
      },
      {
        field: 'id',
        label: 'ID',
        type: 'string',
        filterable: false, // ID không nên filter được
      },
    ]
  }

  return {
    parcels,
    loading,
    page,
    pageSize,
    total,
    searchQuery,
    filters,
    sorts,
    useAdvancedSearch,
    useV2Api,
    loadParcels,
    create,
    update,
    remove,
    changeStatus,
    bulkDelete,
    handlePageChange,
    handleSearch,
    updateFilters,
    updateSorts,
    clearFilters,
    clearSorts,
    toggleAdvancedSearch,
    getFilterableColumns,
  }
}
