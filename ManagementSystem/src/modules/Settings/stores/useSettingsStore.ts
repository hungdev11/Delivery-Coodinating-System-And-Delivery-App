/**
 * Settings Pinia Store
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { SystemSettingDto, UpsertSettingRequest, type SettingType, type DisplayMode } from '../model.type'
import { listSettings, getSettingByKey, upsertSetting, deleteSetting } from '../api'

export const useSettingsStore = defineStore('settings', () => {
  // State
  const settings = ref<SystemSettingDto[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const selected = ref<string[]>([])
  const searchValue = ref('')
  const sorting = ref<{ field: string; direction: 'asc' | 'desc' }[]>([])
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
  const loadSettings = async (query?: any) => {
    loading.value = true
    error.value = null

    try {
      const response = await listSettings({
        page: pagination.value.page,
        size: pagination.value.size,
        sorts: sorting.value,
        search: searchValue.value,
        ...query
      })

      settings.value = response.result.data.map((s: any) => new SystemSettingDto(s))

      // Update pagination from response if available
      if (response.result.page) {
        pagination.value = {
          page: response.result.page.page,
          size: response.result.page.size,
          totalElements: response.result.page.totalElements,
          totalPages: response.result.page.totalPages
        }
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to load settings'
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
    } catch (err: any) {
      error.value = err.message || 'Failed to load setting'
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

      // Update local state
      const existingIndex = settings.value.findIndex(s => s.key === data.key && s.group === data.group)
      if (existingIndex >= 0) {
        settings.value[existingIndex] = new SystemSettingDto(response.result as any)
      } else {
        settings.value.push(new SystemSettingDto(response.result as any))
      }

      return response.result
    } catch (err: any) {
      error.value = err.message || 'Failed to save setting'
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
    } catch (err: any) {
      error.value = err.message || 'Failed to delete setting'
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
    } catch (err: any) {
      error.value = err.message || 'Failed to delete settings'
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

  const setSorting = (sorts: { field: string; direction: 'asc' | 'desc' }[]) => {
    sorting.value = sorts
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
    setPage,
    setPageSize,
    clearFilters,
    reset
  }
})
