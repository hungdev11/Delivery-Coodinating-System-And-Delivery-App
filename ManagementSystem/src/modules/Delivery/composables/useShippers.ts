/**
 * useShippers Composable
 *
 * Business logic for shipper management (admin-facing)
 */

import { ref } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import {
  getDeliveryMen,
  getDeliveryMenV2,
  createDeliveryMan,
  updateDeliveryMan,
  deleteDeliveryMan,
  getDeliveryManById,
} from '../api'
import {
  DeliveryManDto,
  CreateDeliveryManRequest,
  UpdateDeliveryManRequest,
} from '../model.type'
import type { FilterGroup, SortConfig, FilterableColumn } from '@/common/types/filter'
import { createEmptyFilterGroup } from '@/common/utils/query-builder'
import { convertV1ToV2Filter } from '@/common/utils/filter-v2-converter'
import type { FilterGroupItemV2 } from '@/common/types/filter-v2'

export function useShippers() {
  const toast = useToast()

  const shippers = ref<DeliveryManDto[]>([])
  const loading = ref(true)
  const page = ref(0)
  const pageSize = ref(10)
  const total = ref(0)
  const searchQuery = ref('')

  const filters = ref<FilterGroup>(createEmptyFilterGroup())
  const sorts = ref<SortConfig[]>([])
  const useAdvancedSearch = ref(true)
  const useV2Api = ref(true)

  const loadShippers = async () => {
    loading.value = true
    try {
      let filtersToSend: FilterGroup | FilterGroupItemV2 | undefined = undefined

      if (filters.value.conditions.length > 0) {
        filtersToSend = useV2Api.value
          ? convertV1ToV2Filter(filters.value)
          : filters.value
      }

      const params = {
        filters: filtersToSend,
        sorts: sorts.value.length > 0 ? sorts.value : undefined,
        page: page.value,
        size: pageSize.value,
        search: searchQuery.value || undefined,
      }

      const response = useV2Api.value
        ? await getDeliveryMenV2(params)
        : await getDeliveryMen(params)

      if (response.result) {
        shippers.value = response.result.data.map((shipper) => new DeliveryManDto(shipper))
        total.value = response.result.page.totalElements
      }
    } catch (error) {
      console.error('Failed to load shippers:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to load shippers',
        color: 'error',
      })
    } finally {
      loading.value = false
    }
  }

  const refreshShipper = async (id: string) => {
    try {
      const response = await getDeliveryManById(id)
      if (response.result) {
        const updated = new DeliveryManDto(response.result)
        const index = shippers.value.findIndex((shipper) => shipper.id === id)
        if (index !== -1) {
          shippers.value.splice(index, 1, updated)
        }
      }
    } catch (error) {
      console.error('Failed to refresh shipper:', error)
    }
  }

  const create = async (data: CreateDeliveryManRequest) => {
    try {
      const request = new CreateDeliveryManRequest(data)
      await createDeliveryMan(request)
      toast.add({
        title: 'Success',
        description: 'Shipper created successfully',
        color: 'success',
      })
      await loadShippers()
      return true
    } catch (error) {
      console.error('Failed to create shipper:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to create shipper',
        color: 'error',
      })
      return false
    }
  }

  const update = async (id: string, data: UpdateDeliveryManRequest) => {
    try {
      const request = new UpdateDeliveryManRequest(data)
      await updateDeliveryMan(id, request)
      toast.add({
        title: 'Success',
        description: 'Shipper updated successfully',
        color: 'success',
      })
      await refreshShipper(id)
      return true
    } catch (error) {
      console.error('Failed to update shipper:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to update shipper',
        color: 'error',
      })
      return false
    }
  }

  const remove = async (id: string) => {
    try {
      await deleteDeliveryMan(id)
      toast.add({
        title: 'Success',
        description: 'Shipper deleted successfully',
        color: 'success',
      })
      await loadShippers()
      return true
    } catch (error) {
      console.error('Failed to delete shipper:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to delete shipper',
        color: 'error',
      })
      return false
    }
  }

  const bulkDelete = async (ids: string[]) => {
    try {
      await Promise.all(ids.map((id) => deleteDeliveryMan(id)))
      toast.add({
        title: 'Success',
        description: `${ids.length} shipper(s) deleted successfully`,
        color: 'success',
      })
      await loadShippers()
      return true
    } catch (error) {
      console.error('Failed to bulk delete shippers:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to delete shippers',
        color: 'error',
      })
      return false
    }
  }

  const handlePageChange = (newPage: number) => {
    if (newPage < 0) return
    page.value = newPage
    loadShippers()
  }

  const handleSearch = (query: string) => {
    searchQuery.value = query
    page.value = 0
    loadShippers()
  }

  const updateFilters = (newFilters: FilterGroup) => {
    filters.value = newFilters
    page.value = 0
    loadShippers()
  }

  const updateSorts = (newSorts: SortConfig[]) => {
    sorts.value = newSorts
    if (useAdvancedSearch.value) {
      loadShippers()
    }
  }

  const clearFilters = () => {
    filters.value = createEmptyFilterGroup()
    page.value = 0
    loadShippers()
  }

  const clearSorts = () => {
    sorts.value = []
    loadShippers()
  }

  const toggleAdvancedSearch = () => {
    useAdvancedSearch.value = !useAdvancedSearch.value
    if (!useAdvancedSearch.value) {
      clearFilters()
      clearSorts()
    }
    loadShippers()
  }

  const getFilterableColumns = (): FilterableColumn[] => [
    {
      field: 'user.username',
      label: 'Username',
      type: 'string',
      filterType: 'text',
      filterConfig: { placeholder: 'Enter username...' },
    },
    {
      field: 'user.email',
      label: 'Email',
      type: 'string',
      filterType: 'text',
      filterConfig: { placeholder: 'Enter email address...' },
    },
    {
      field: 'vehicleType',
      label: 'Vehicle Type',
      type: 'string',
      filterType: 'text',
      filterConfig: { placeholder: 'Enter vehicle type...' },
    },
    {
      field: 'capacityKg',
      label: 'Capacity (kg)',
      type: 'number',
      filterType: 'number',
      filterConfig: {
        placeholder: 'Enter capacity...',
        min: 0,
        step: 5,
      },
    },
    {
      field: 'user.status',
      label: 'Status',
      type: 'enum',
      enumOptions: [
        { label: 'Active', value: 'ACTIVE' },
        { label: 'Inactive', value: 'INACTIVE' },
        { label: 'Suspended', value: 'SUSPENDED' },
        { label: 'Pending', value: 'PENDING' },
      ],
      filterType: 'select',
      filterConfig: {
        placeholder: 'Select status...',
      },
    },
    {
      field: 'createdAt',
      label: 'Created At',
      type: 'date',
      filterType: 'date',
      filterConfig: { placeholder: 'Select created date...' },
    },
    {
      field: 'updatedAt',
      label: 'Updated At',
      type: 'date',
      filterType: 'date',
      filterConfig: { placeholder: 'Select updated date...' },
    },
  ]

  return {
    shippers,
    loading,
    page,
    pageSize,
    total,
    searchQuery,
    filters,
    sorts,
    useAdvancedSearch,
    useV2Api,
    loadShippers,
    refreshShipper,
    create,
    update,
    remove,
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
