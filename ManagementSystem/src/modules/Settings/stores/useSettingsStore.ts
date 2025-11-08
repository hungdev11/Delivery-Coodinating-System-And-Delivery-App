/**
 * Settings Pinia Store
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { SystemSettingDto, UpsertSettingRequest } from '../model.type'
import { listSettingsV2, getSettingByKey, upsertSetting, deleteSetting } from '../api'
import type { FilterGroup, SortConfig } from '@/common/types/filter'
import { convertV1ToV2Filter } from '@/common/utils/filter-v2-converter'

export const useSettingsStore = defineStore('settings', () => {
  // State
  const settings = ref<SystemSettingDto[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const selected = ref<string[]>([])
  const searchValue = ref('')
  const sorting = ref<SortConfig[]>([])
  const filters = ref<FilterGroup | undefined>(undefined)
  const pagination = ref({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0
  })

  // Getters - removed filteredSettings, will be handled in component

  const selectedSettings = computed(() =>
    settings.value.filter(setting => selected.value.includes(setting.key))
  )

  const settingsByGroup = computed(() => {
    const groups: Record<string, SystemSettingDto[]> = {}
    settings.value.forEach(setting => {
      if (!groups[setting.group]) {
        groups[setting.group] = []
      }
      groups[setting.group].push(setting)
    })
    return groups
  })

  const groups = computed(() => Object.keys(settingsByGroup.value))

  // Actions
  const loadSettings = async (query?: {
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
      const sortsToUse = query?.sorts ?? sorting.value
      const pageToUse = query?.page ?? pagination.value.page
      const sizeToUse = query?.size ?? pagination.value.size
      const searchToUse = query?.search ?? searchValue.value

      const v2Filters = filtersToUse && filtersToUse.conditions.length > 0
        ? convertV1ToV2Filter(filtersToUse)
        : undefined

      const response = await listSettingsV2({
        page: pageToUse,
        size: sizeToUse,
        sorts: sortsToUse && sortsToUse.length > 0 ? sortsToUse : undefined,
        search: searchToUse || undefined,
        filters: v2Filters,
      })

      const result = response.result

      if (result?.data) {
        settings.value = result.data.map((setting: SystemSettingDto) => new SystemSettingDto(setting))
      } else {
        settings.value = []
      }

      // Update pagination from response if available
      if (result?.page) {
        pagination.value = {
          page: result.page.page,
          size: result.page.size,
          totalElements: result.page.totalElements,
          totalPages: result.page.totalPages
        }
      } else {
        pagination.value = {
          page: 0,
          size: sizeToUse,
          totalElements: 0,
          totalPages: 0
        }
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load settings'
      error.value = message
      console.error('Failed to load settings:', err)
    } finally {
      loading.value = false
    }
  }

  const loadSetting = async (key: string, group: string) => {
    loading.value = true
    error.value = null

    try {
      const response = await getSettingByKey(group, key)
      return response.result
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load setting'
      error.value = message
      console.error('Failed to load setting:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  const saveSetting = async (data: UpsertSettingRequest) => {
    loading.value = true
    error.value = null

    try {
      const response = await upsertSetting(data.group, data.key, data)
      const resultSetting = response.result

      if (resultSetting) {
        const dto = new SystemSettingDto(resultSetting)
        const existingIndex = settings.value.findIndex(s => s.key === data.key && s.group === data.group)
        if (existingIndex >= 0) {
          settings.value[existingIndex] = dto
        } else {
          settings.value.push(dto)
        }
      }

      return resultSetting
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to save setting'
      error.value = message
      console.error('Failed to save setting:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  const removeSetting = async (key: string, group: string) => {
    loading.value = true
    error.value = null

    try {
      await deleteSetting(group, key)

      // Update local state
      const index = settings.value.findIndex(s => s.key === key && s.group === group)
      if (index >= 0) {
        settings.value.splice(index, 1)
      }

      // Remove from selected if present
      const selectedIndex = selected.value.indexOf(key)
      if (selectedIndex >= 0) {
        selected.value.splice(selectedIndex, 1)
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to delete setting'
      error.value = message
      console.error('Failed to delete setting:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  const bulkDelete = async (keys: string[]) => {
    loading.value = true
    error.value = null

    try {
      const deletePromises = keys.map(key => {
        const setting = settings.value.find(s => s.key === key)
        if (setting) {
          return deleteSetting(setting.group, key)
        }
        return Promise.resolve()
      })

      await Promise.all(deletePromises)

      // Update local state
      settings.value = settings.value.filter(s => !keys.includes(s.key))
      selected.value = selected.value.filter(key => !keys.includes(key))
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to delete settings'
      error.value = message
      console.error('Failed to delete settings:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  const setSelected = (keys: string[]) => {
    selected.value = keys
  }

  const toggleSelected = (key: string) => {
    const index = selected.value.indexOf(key)
    if (index >= 0) {
      selected.value.splice(index, 1)
    } else {
      selected.value.push(key)
    }
  }

  const clearSelected = () => {
    selected.value = []
  }

  const setSearch = (value: string) => {
    searchValue.value = value
  }

  const setSorting = (sorts: SortConfig[]) => {
    sorting.value = sorts
  }

  const setFilters = (filterGroup?: FilterGroup) => {
    if (filterGroup && filterGroup.conditions.length === 0) {
      filters.value = undefined
    } else {
      filters.value = filterGroup
    }
  }

  const setPage = (page: number) => {
    pagination.value.page = page
  }

  const setPageSize = (size: number) => {
    pagination.value.size = size
  }

  const clearFilters = () => {
    searchValue.value = ''
    sorting.value = []
    selected.value = []
    filters.value = undefined
  }

  const reset = () => {
    settings.value = []
    loading.value = false
    error.value = null
    selected.value = []
    searchValue.value = ''
    sorting.value = []
    pagination.value = {
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0
    }
  }

  return {
    // State
    settings,
    loading,
    error,
    selected,
    searchValue,
    sorting,
    filters,
    pagination,

    // Getters
    selectedSettings,
    settingsByGroup,
    groups,

    // Actions
    loadSettings,
    loadSetting,
    saveSetting,
    removeSetting,
    bulkDelete,
    setSelected,
    toggleSelected,
    clearSelected,
    setSearch,
    setSorting,
    setFilters,
    setPage,
    setPageSize,
    clearFilters,
    reset
  }
})
