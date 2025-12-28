/**
 * useTickets Composable
 *
 * Business logic for ticket management
 */

import { ref } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import {
  getTicketsV2,
  getTicketById,
  resolveTicket,
  cancelTicket,
  reassignParcel,
  getTicketStatistics,
} from '../api'
import type {
  TicketDto,
  TicketStatus,
  TicketType,
  ReassignParcelRequest,
  TicketStatistics,
} from '../model.type'
import type { FilterGroup, SortConfig, FilterableColumn } from '@/common/types/filter'
import { createEmptyFilterGroup } from '@/common/utils/query-builder'
import { convertV1ToV2Filter } from '@/common/utils/filter-v2-converter'
import type { FilterGroupItemV2 } from '@/common/types/filter-v2'

export function useTickets() {
  const toast = useToast()

  const tickets = ref<TicketDto[]>([])
  const loading = ref(false)
  const page = ref(0)
  const pageSize = ref(20)
  const total = ref(0)
  const totalPages = ref(0)
  const searchQuery = ref('')

  // New filter/sort state
  const filters = ref<FilterGroup>(createEmptyFilterGroup())
  const sorts = ref<SortConfig[]>([])
  const useV2Api = ref(true) // Use V2 API by default

  // Legacy filter state (for backward compatibility)
  const statusFilter = ref<TicketStatus | ''>('')
  const typeFilter = ref<TicketType | ''>('')

  // Statistics
  const statistics = ref<TicketStatistics | null>(null)

  /**
   * Load tickets with V2 filter system
   */
  const loadTickets = async () => {
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
      const response = await getTicketsV2(params)

      if (response.result) {
        tickets.value = response.result.data
        total.value = response.result.page.totalElements
        totalPages.value = response.result.page.totalPages
      }
    } catch (error) {
      console.error('Failed to load tickets:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to load tickets',
        color: 'error',
      })
    } finally {
      loading.value = false
    }
  }

  /**
   * Load ticket by ID
   */
  const loadTicket = async (id: string): Promise<TicketDto | null> => {
    try {
      const response = await getTicketById(id)
      return response.result || null
    } catch (error) {
      console.error('Failed to load ticket:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to load ticket',
        color: 'error',
      })
      return null
    }
  }

  /**
   * Resolve ticket
   */
  const resolve = async (id: string, resolutionNotes?: string): Promise<boolean> => {
    try {
      await resolveTicket(id, resolutionNotes)
      toast.add({
        title: 'Success',
        description: 'Ticket resolved successfully',
        color: 'success',
      })
      await loadTickets()
      await loadStatistics()
      return true
    } catch (error) {
      console.error('Failed to resolve ticket:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to resolve ticket',
        color: 'error',
      })
      return false
    }
  }

  /**
   * Cancel ticket
   */
  const cancel = async (id: string): Promise<boolean> => {
    try {
      await cancelTicket(id)
      toast.add({
        title: 'Success',
        description: 'Ticket cancelled successfully',
        color: 'success',
      })
      await loadTickets()
      await loadStatistics()
      return true
    } catch (error) {
      console.error('Failed to cancel ticket:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to cancel ticket',
        color: 'error',
      })
      return false
    }
  }

  /**
   * Reassign parcel
   */
  const reassign = async (id: string, data: ReassignParcelRequest): Promise<boolean> => {
    try {
      await reassignParcel(id, data)
      toast.add({
        title: 'Success',
        description: 'Parcel reassigned successfully',
        color: 'success',
      })
      await loadTickets()
      return true
    } catch (error) {
      console.error('Failed to reassign parcel:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to reassign parcel',
        color: 'error',
      })
      return false
    }
  }

  /**
   * Load statistics
   */
  const loadStatistics = async () => {
    try {
      const response = await getTicketStatistics()
      if (response.result) {
        statistics.value = response.result
      }
    } catch (error) {
      console.error('Failed to load ticket statistics:', error)
    }
  }

  /**
   * Update filters
   */
  const updateFilters = (newFilters: FilterGroup) => {
    filters.value = newFilters
    page.value = 0
  }

  /**
   * Update sorts
   */
  const updateSorts = (newSorts: SortConfig[]) => {
    sorts.value = newSorts
    page.value = 0
  }

  /**
   * Clear filters
   */
  const clearFilters = () => {
    filters.value = createEmptyFilterGroup()
    sorts.value = []
    searchQuery.value = ''
    statusFilter.value = ''
    typeFilter.value = ''
    page.value = 0
  }

  /**
   * Handle search
   */
  const handleSearch = (query: string) => {
    searchQuery.value = query
    page.value = 0
  }

  /**
   * Get filterable columns
   */
  const getFilterableColumns = (): FilterableColumn[] => {
    return [
      {
        field: 'type',
        label: 'Type',
        type: 'enum',
        enumOptions: [
          { label: 'Delivery Failed', value: 'DELIVERY_FAILED' },
          { label: 'Not Received', value: 'NOT_RECEIVED' },
        ],
        filterType: 'select',
      },
      {
        field: 'status',
        label: 'Status',
        type: 'enum',
        enumOptions: [
          { label: 'Pending', value: 'PENDING' },
          { label: 'Resolved', value: 'RESOLVED' },
          { label: 'Cancelled', value: 'CANCELLED' },
        ],
        filterType: 'select',
      },
      {
        field: 'parcelId',
        label: 'Parcel ID',
        type: 'string',
        filterType: 'text',
      },
      {
        field: 'userId',
        label: 'User ID',
        type: 'string',
        filterType: 'text',
      },
      {
        field: 'description',
        label: 'Description',
        type: 'string',
        filterType: 'text',
      },
      {
        field: 'createdAt',
        label: 'Created At',
        type: 'date',
        filterType: 'daterange',
      },
    ]
  }

  /**
   * Legacy: Set status filter (for backward compatibility)
   */
  const setStatusFilter = (status: TicketStatus | '') => {
    statusFilter.value = status
    page.value = 0
  }

  /**
   * Legacy: Set type filter (for backward compatibility)
   */
  const setTypeFilter = (type: TicketType | '') => {
    typeFilter.value = type
    page.value = 0
  }

  /**
   * Handle page change
   */
  const handlePageChange = (newPage: number) => {
    page.value = newPage
    loadTickets()
  }

  return {
    tickets,
    loading,
    page,
    pageSize,
    total,
    totalPages,
    filters,
    sorts,
    searchQuery,
    statusFilter,
    typeFilter,
    statistics,
    loadTickets,
    loadTicket,
    resolve,
    cancel,
    reassign,
    loadStatistics,
    updateFilters,
    updateSorts,
    clearFilters,
    handleSearch,
    getFilterableColumns,
    setStatusFilter,
    setTypeFilter,
    handlePageChange,
  }
}
