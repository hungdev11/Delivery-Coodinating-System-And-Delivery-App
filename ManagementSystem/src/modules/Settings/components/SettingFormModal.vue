<script setup lang="ts">
/**
 * Setting Form Modal
 *
 * Modal for creating and editing settings
 */

import { ref, computed, watch } from 'vue'
import { SystemSettingDto, UpsertSettingRequest } from '../model.type'
import type { DisplayMode } from '../model.type'

interface Props {
  mode: 'create' | 'edit'
  setting?: SystemSettingDto
}

const props = defineProps<Props>()

const emit = defineEmits<{
  result: [UpsertSettingRequest | null]
}>()

// Form data
const formData = ref<UpsertSettingRequest>({
  key: '',
  value: '',
  group: '',
  description: '',
  type: 'STRING' as const,
  displayMode: 'TEXT' as DisplayMode,
})

watch(
  () => props.setting,
  (setting) => {
    if (setting) {
      formData.value = {
        key: setting.key || '',
        value: setting.value || '',
        group: setting.group || '',
        description: setting.description || '',
        type: setting.type || 'STRING',
        displayMode: setting.displayMode || 'TEXT',
      }
    }
  },
  { immediate: true },
)

/**
 * Check if value is sensitive (password or other sensitive data)
 */
const isSensitiveValue = (setting: SystemSettingDto) => {
  return (
    setting.displayMode === 'PASSWORD' ||
    setting.key.toLowerCase().includes('password') ||
    setting.key.toLowerCase().includes('secret') ||
    setting.key.toLowerCase().includes('key') ||
    setting.key.toLowerCase().includes('token')
  )
}

// Form validation
const isValid = computed(() => {
  return (
    formData.value.key.trim() !== '' &&
    formData.value.value.trim() !== '' &&
    formData.value.group.trim() !== ''
  )
})

// Submit form
const handleSubmit = () => {
  if (isValid.value) {
    emit('result', { ...formData.value })
  }
}

// Cancel form
const handleCancel = () => {
  emit('result', null)
}

/**
 * Get input type based on display mode
 */
const getInputType = () => {
  const typeMap: Record<string, string> = {
    TEXT: 'text',
    NUMBER: 'number',
    EMAIL: 'email',
    URL: 'url',
    TOGGLE: 'checkbox',
  }
  return typeMap[formData.value.displayMode || 'TEXT'] || 'text'
}

/**
 * Get value placeholder based on display mode
 */
const getValuePlaceholder = () => {
  const placeholderMap: Record<string, string> = {
    TEXT: 'Enter text value',
    NUMBER: 'Enter number value',
    EMAIL: 'Enter email address',
    URL: 'Enter URL',
    PASSWORD: 'Enter password (will be masked)',
    CODE: 'Enter code value',
    TEXTAREA: 'Enter multi-line text',
    TOGGLE: 'true/false',
  }

  // For sensitive values in edit mode, show masked placeholder
  if (props.mode === 'edit' && props.setting && isSensitiveValue(props.setting)) {
    const originalLength = props.setting.value.length
    const maskedLength = Math.max(originalLength, 4)
    return '*'.repeat(maskedLength) + ' (enter new value)'
  }

  return placeholderMap[formData.value.displayMode || 'TEXT'] || 'Enter value'
}
</script>

<template>
  <UModal
    :title="mode === 'create' ? 'Tạo cài đặt' : 'Chỉnh sửa cài đặt'"
    :description="
      mode === 'create' ? 'Cấu hình một cài đặt hệ thống mới' : 'Cập nhật cài đặt hệ thống đã chọn'
    "
    :close="{ onClick: handleCancel }"
    :ui="{ width: 'sm:max-w-md md:max-w-lg', footer: 'justify-end space-x-2' }"
  >
    <template #body>
      <form class="space-y-4" @submit.prevent="handleSubmit">
        <UFormField label="Key" required>
          <UInput
            v-model="formData.key"
            placeholder="Enter setting key"
            :disabled="mode === 'edit'"
          />
        </UFormField>

        <UFormField label="Value" required>
          <div class="space-y-2">
            <UInput
              v-if="
                formData.displayMode !== 'PASSWORD' &&
                formData.displayMode !== 'TEXTAREA' &&
                formData.displayMode !== 'CODE'
              "
              v-model="formData.value"
              :placeholder="getValuePlaceholder()"
              :type="getInputType()"
            />
            <UTextarea
              v-else-if="formData.displayMode === 'TEXTAREA'"
              v-model="formData.value"
              :placeholder="getValuePlaceholder()"
              :rows="4"
            />
            <UInput
              v-else
              v-model="formData.value"
              :placeholder="getValuePlaceholder()"
              :type="formData.displayMode === 'PASSWORD' ? 'password' : 'text'"
            />
            <div
              v-if="mode === 'edit' && props.setting && isSensitiveValue(props.setting)"
              class="text-sm text-amber-600 dark:text-amber-400"
            >
              <UIcon name="i-heroicons-exclamation-triangle" class="inline mr-1" />
              Giá trị nhạy cảm - nhập giá trị mới để cập nhật
            </div>
          </div>
        </UFormField>

        <UFormField label="Group" required>
          <UInput
            v-model="formData.group"
            placeholder="Enter setting group"
            :disabled="mode === 'edit'"
          />
        </UFormField>

        <UFormField label="Description">
          <UTextarea v-model="formData.description" placeholder="Enter setting description" />
        </UFormField>

        <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
          <UFormField label="Type">
            <USelect
              v-model="formData.type"
              :items="[
                { label: 'String', value: 'STRING' },
                { label: 'Integer', value: 'INTEGER' },
                { label: 'Boolean', value: 'BOOLEAN' },
                { label: 'Double', value: 'DOUBLE' },
                { label: 'JSON', value: 'JSON' },
              ]"
            />
          </UFormField>

          <UFormField label="Display Mode">
            <USelect
              v-model="formData.displayMode"
              :items="[
                { label: 'Text', value: 'TEXT' },
                { label: 'Password', value: 'PASSWORD' },
                { label: 'Code', value: 'CODE' },
                { label: 'Number', value: 'NUMBER' },
                { label: 'Toggle', value: 'TOGGLE' },
                { label: 'Textarea', value: 'TEXTAREA' },
                { label: 'URL', value: 'URL' },
                { label: 'Email', value: 'EMAIL' },
              ]"
            />
          </UFormField>
        </div>
      </form>
    </template>

    <template #footer>
      <UButton variant="outline" color="neutral" @click="handleCancel"> Hủy </UButton>
      <UButton :disabled="!isValid" @click="handleSubmit">
        {{ mode === 'create' ? 'Tạo' : 'Cập nhật' }}
      </UButton>
    </template>
  </UModal>
</template>
