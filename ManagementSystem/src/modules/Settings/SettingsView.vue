<script setup lang="ts">
/**
 * Settings View
 *
 * View for managing system settings grouped by service
 */

import { ref, onMounted, computed } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { PageHeader } from '@/common/components'
import { getSettingsByGroup, upsertSetting } from './api'
import { SystemSettingDto, UpsertSettingRequest } from './model.type'

const toast = useToast()

// State
const settings = ref<SystemSettingDto[]>([])
const loading = ref(false)
const selectedGroup = ref('zone-service')
const editingKey = ref<string | null>(null)
const editValue = ref('')

// Available groups
const groups = [
  { label: 'Zone Service', value: 'zone-service' },
  { label: 'User Service', value: 'user-service' },
  { label: 'System', value: 'system' },
]

/**
 * Load settings for selected group
 */
const loadSettings = async () => {
  loading.value = true
  try {
    const response = await getSettingsByGroup(selectedGroup.value)
    if (response.result) {
      settings.value = response.result.map((s) => new SystemSettingDto(s))
    }
  } catch (error) {
    console.error('Failed to load settings:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to load settings',
      color: 'error',
    })
  } finally {
    loading.value = false
  }
}

/**
 * Start editing a setting
 */
const startEdit = (setting: SystemSettingDto) => {
  if (!setting.isEditable) return
  editingKey.value = setting.key
  editValue.value = setting.value
}

/**
 * Cancel editing
 */
const cancelEdit = () => {
  editingKey.value = null
  editValue.value = ''
}

/**
 * Save setting
 */
const saveSetting = async (setting: SystemSettingDto) => {
  try {
    const request = new UpsertSettingRequest({
      key: setting.key,
      group: setting.group,
      description: setting.description,
      type: setting.type,
      value: editValue.value,
      level: setting.level,
      isReadOnly: setting.isReadOnly,
      displayMode: setting.displayMode,
    })

    await upsertSetting(setting.group, setting.key, request)

    toast.add({
      title: 'Success',
      description: 'Setting updated successfully',
      color: 'success',
    })

    editingKey.value = null
    loadSettings()
  } catch (error) {
    console.error('Failed to update setting:', error)
  }
}

/**
 * Handle group change
 */
const handleGroupChange = () => {
  loadSettings()
}

/**
 * Get type badge color
 */
const getTypeColor = (type: string) => {
  const colorMap: Record<string, string> = {
    STRING: 'blue',
    INTEGER: 'green',
    BOOLEAN: 'purple',
    DOUBLE: 'yellow',
    JSON: 'gray',
  }
  return colorMap[type] || 'gray'
}

// Grouped settings
const groupedSettings = computed(() => {
  return settings.value
})

onMounted(() => {
  loadSettings()
})
</script>

<template>
  <div class="container mx-auto px-4 py-6">
    <PageHeader title="Settings" description="Manage system configuration settings" />

    <!-- Group Selector -->
    <div class="mb-6">
      <UFormField label="Settings Group">
        <USelect
          v-model="selectedGroup"
          :options="groups"
          @update:model-value="handleGroupChange"
          class="w-64"
        />
      </UFormField>
    </div>

    <!-- Settings List -->
    <div v-if="loading" class="flex items-center justify-center h-64">
      <UIcon name="i-heroicons-arrow-path" class="w-8 h-8 animate-spin text-primary-500" />
    </div>

    <div v-else-if="groupedSettings.length > 0" class="space-y-4">
      <UCard v-for="setting in groupedSettings" :key="setting.key">
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-2">
              <h3 class="font-semibold">{{ setting.displayName }}</h3>
              <UBadge :color="getTypeColor(setting.type)" variant="soft" size="xs">
                {{ setting.type }}
              </UBadge>
              <UBadge v-if="setting.isReadOnly" color="gray" variant="soft" size="xs">
                Read-only
              </UBadge>
            </div>

            <p class="text-sm text-gray-600 mb-3">{{ setting.description }}</p>

            <div v-if="editingKey === setting.key" class="flex items-center gap-2">
              <UInput
                v-if="setting.displayMode === 'TEXT' || setting.displayMode === 'NUMBER'"
                v-model="editValue"
                :type="setting.displayMode === 'NUMBER' ? 'number' : 'text'"
                class="flex-1"
              />
              <UTextarea
                v-else-if="setting.displayMode === 'TEXTAREA' || setting.displayMode === 'JSON'"
                v-model="editValue"
                class="flex-1"
                :rows="3"
              />
              <UButton size="sm" @click="saveSetting(setting)">Save</UButton>
              <UButton size="sm" variant="outline" @click="cancelEdit">Cancel</UButton>
            </div>

            <div v-else class="flex items-center gap-2">
              <code class="text-sm bg-gray-100 px-2 py-1 rounded">{{ setting.value }}</code>
              <UButton
                v-if="setting.isEditable"
                size="sm"
                variant="ghost"
                icon="i-heroicons-pencil"
                @click="startEdit(setting)"
              />
            </div>

            <div class="mt-2 text-xs text-gray-500">
              <span>Key: {{ setting.key }}</span>
              <span class="mx-2">•</span>
              <span>Updated: {{ new Date(setting.updatedAt).toLocaleDateString() }}</span>
              <span v-if="setting.updatedBy" class="mx-2">•</span>
              <span v-if="setting.updatedBy">By: {{ setting.updatedBy }}</span>
            </div>
          </div>
        </div>
      </UCard>
    </div>

    <div v-else class="text-center py-12 text-gray-500">
      <UIcon name="i-heroicons-inbox" class="w-12 h-12 mx-auto mb-2" />
      <p>No settings found for this group</p>
    </div>
  </div>
</template>
