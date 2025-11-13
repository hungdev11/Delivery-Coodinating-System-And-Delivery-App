<script setup lang="ts">
/**
 * UserSelect Component
 *
 * A searchable select component for selecting users by ID or name
 * Supports seed ID (allows direct ID input)
 */

import { ref, computed, watch, onMounted } from 'vue'
import { getUsers } from '@/modules/Users/api'
import { UserDto } from '@/modules/Users/model.type'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { createEmptyFilterGroup } from '@/common/utils/query-builder'
import type { FilterGroup } from '@/common/types/filter'

interface Props {
  modelValue?: string // User ID
  label?: string
  placeholder?: string
  allowSeedId?: boolean // Allow direct ID input (for seed/testing)
  searchable?: boolean // Enable search functionality
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  label: 'User',
  placeholder: 'Search by ID or name...',
  allowSeedId: true,
  searchable: true,
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string | undefined]
}>()

const toast = useToast()
const searchQuery = ref('')
const users = ref<UserDto[]>([])
const loading = ref(false)
const selectedUser = ref<UserDto | null>(null)
const selectedId = ref<string | undefined>(props.modelValue)

// Watch for external modelValue changes
watch(() => props.modelValue, (newValue) => {
  if (newValue !== selectedId.value) {
    selectedId.value = newValue
    if (newValue) {
      loadUserById(newValue)
    } else {
      selectedUser.value = null
    }
  }
})

// Watch for selected user changes
watch(selectedUser, (user) => {
  if (user) {
    selectedId.value = user.id
    emit('update:modelValue', user.id)
  } else {
    selectedId.value = undefined
    emit('update:modelValue', undefined)
  }
})

// Computed options for USelect
const userOptions = computed(() => {
  return users.value.map(user => ({
    label: `${user.fullName} (${user.username}) - ${user.id.substring(0, 8)}...`,
    value: user.id,
    user: user as UserDto,
  }))
})

  // If allowSeedId is true, add the current ID as an option if not in list
const selectOptions = computed(() => {
  const options = [...userOptions.value]

  if (props.allowSeedId && selectedId.value && !users.value.find(u => u.id === selectedId.value)) {
    options.unshift({
      label: `ID: ${selectedId.value}`,
      value: selectedId.value,
      user: undefined as unknown as UserDto,
    })
  }

  return options
})

/**
 * Load users with search query
 */
const loadUsers = async (query: string) => {
  if (!query || query.length < 2) {
    users.value = []
    return
  }

  loading.value = true
  try {
    // Build search filter - search by ID (exact or partial), username, firstName, lastName
    const filters: FilterGroup = createEmptyFilterGroup()

    // Check if query is a UUID (full or partial)
    const isIdQuery = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(query) ||
                      /^[0-9a-f-]+$/i.test(query)

    if (isIdQuery && props.allowSeedId) {
      // Search by ID
      filters.conditions.push({
        field: 'id',
        operator: 'contains',
        value: query,
      })
    } else {
      // Search by username - simplified to single field search
      // TODO: Support OR search across multiple fields (username, firstName, lastName)
      filters.conditions.push({
        field: 'username',
        operator: 'contains',
        value: query,
      })
    }

    const params = {
      filters,
      page: 0,
      size: 20, // Limit results for better UX
    }

      const response = await getUsers(params)

      if (response.result) {
        users.value = response.result.data.map((u) => new UserDto(u))

        // If query matches an ID exactly, try to load that user
        if (props.allowSeedId && isIdQuery && users.value.length === 0) {
          try {
            await loadUserById(query)
          } catch {
            // Ignore if user not found
          }
        }
      }
  } catch (error) {
    console.error('Failed to search users:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to search users',
      color: 'error',
    })
  } finally {
    loading.value = false
  }
}

/**
 * Load user by ID
 */
const loadUserById = async (id: string) => {
  try {
    const { getUserById } = await import('@/modules/Users/api')
    const response = await getUserById(id)

    if (response.result) {
      const user = new UserDto(response.result)
      selectedUser.value = user

      // Add to users list if not already there
      if (!users.value.find(u => u.id === user.id)) {
        users.value.unshift(user)
      }
    }
  } catch (error) {
    console.error('Failed to load user by ID:', error)
    // If user not found, keep the ID as-is (for seed mode)
    if (props.allowSeedId) {
      selectedUser.value = null
    }
  }
}

// Note: USelect in Nuxt UI v3 handles search internally
// We load users when options are requested via :items prop
// The search is handled automatically by USelect when searchable is true

/**
 * Handle selection change
 */
const handleSelectionChange = (value: string | undefined) => {
  if (!value) {
    selectedUser.value = null
    selectedId.value = undefined
    return
  }

  // Find user in list
  const user = users.value.find(u => u.id === value)
  if (user) {
    selectedUser.value = user
  } else if (props.allowSeedId) {
    // Allow raw ID selection
    selectedId.value = value
    selectedUser.value = null
  }
}

// Load user on mount if modelValue is set
onMounted(() => {
  if (props.modelValue) {
    loadUserById(props.modelValue)
  }
})
</script>

<template>
  <UFormField :label="label" :name="`user-select-${label.toLowerCase().replace(/\s+/g, '-')}`">
    <USelect
      :model-value="selectedId"
      :items="selectOptions"
      :placeholder="placeholder"
      :loading="loading"
      :disabled="disabled"
      @update:model-value="handleSelectionChange"
    />
  </UFormField>
</template>
