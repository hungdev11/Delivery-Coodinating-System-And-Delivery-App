<script setup lang="ts">
/**
 * UserSelect Component
 *
 * A searchable select component for selecting users by ID or name
 * Supports seed ID (allows direct ID input)
 * Uses USelectMenu for search functionality
 * Client-side filtering for better performance
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
const allUsers = ref<UserDto[]>([]) // All loaded users for client-side filtering
const loading = ref(false)
const initialLoading = ref(false) // Loading state for initial data fetch
const selectedUser = ref<UserDto | null>(null)
const selectedId = ref<string | undefined>(props.modelValue)

// Watch for external modelValue changes
watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue !== selectedId.value) {
      selectedId.value = newValue
      if (newValue) {
        findUserById(newValue)
      } else {
        selectedUser.value = null
      }
    }
  },
)

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

/**
 * Client-side filter users based on search query
 */
const filteredUsers = computed(() => {
  if (!searchQuery.value || searchQuery.value.length < 2) {
    return allUsers.value
  }

  const query = searchQuery.value.toLowerCase().trim()

  return allUsers.value.filter((user) => {
    // Search in ID
    if (user.id.toLowerCase().includes(query)) {
      return true
    }

    // Search in username
    if (user.username?.toLowerCase().includes(query)) {
      return true
    }

    // Search in full name
    const fullName = user.fullName?.toLowerCase() || ''
    if (fullName.includes(query)) {
      return true
    }

    // Search in first name
    if (user.firstName?.toLowerCase().includes(query)) {
      return true
    }

    // Search in last name
    if (user.lastName?.toLowerCase().includes(query)) {
      return true
    }

    // Search in email
    if (user.email?.toLowerCase().includes(query)) {
      return true
    }

    return false
  })
})

// Computed options for USelectMenu
const userOptions = computed(() => {
  const options = filteredUsers.value.map((user) => ({
    label: `${user.fullName} (${user.username})`,
    value: user.id,
    user: user as UserDto,
    description: user.email || `ID: ${user.id.substring(0, 8)}...`,
    // Avatar placeholder - can be enhanced with actual user avatar
    avatar: {
      src: undefined,
      alt: user.fullName || user.username,
    },
  }))

  // If allowSeedId is true, add the current ID as an option if not in list
  if (
    props.allowSeedId &&
    selectedId.value &&
    !allUsers.value.find((u) => u.id === selectedId.value)
  ) {
    options.unshift({
      label: `ID: ${selectedId.value}`,
      value: selectedId.value,
      user: undefined as unknown as UserDto,
      description: 'Direct ID input',
      avatar: {
        src: undefined,
        alt: 'ID',
      },
    })
  }

  return options
})

// Selected option for USelectMenu
const selectedOption = computed(() => {
  if (!selectedId.value) return undefined
  return userOptions.value.find((opt) => opt.value === selectedId.value)
})

/**
 * Load all users for client-side filtering
 */
const loadAllUsers = async () => {
  initialLoading.value = true
  try {
    const filters: FilterGroup = createEmptyFilterGroup()
    const params = {
      filters,
      page: 0,
      size: 1000, // Load up to 1000 users for client-side filtering
    }

    const response = await getUsers(params)

    if (response.result) {
      allUsers.value = response.result.data.map((u) => new UserDto(u))

      // If there are more users, load additional pages
      const totalPages = response.result.page.totalPages
      if (totalPages > 1) {
        const additionalPromises = []
        for (let page = 1; page < totalPages && page < 10; page++) {
          // Limit to 10 pages max (10,000 users) to avoid performance issues
          additionalPromises.push(
            getUsers({
              filters,
              page,
              size: 1000,
            }),
          )
        }

        const additionalResponses = await Promise.all(additionalPromises)
        additionalResponses.forEach((resp) => {
          if (resp.result) {
            const additionalUsers = resp.result.data.map((u) => new UserDto(u))
            allUsers.value.push(...additionalUsers)
          }
        })
      }

      // If modelValue is set, try to find the user
      if (props.modelValue) {
        findUserById(props.modelValue)
      }
    }
  } catch (error) {
    console.error('Failed to load users:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to load users',
      color: 'error',
    })
  } finally {
    initialLoading.value = false
  }
}

/**
 * Find user by ID in loaded users or load it if not found
 */
const findUserById = async (id: string) => {
  // First, try to find in already loaded users
  const foundUser = allUsers.value.find((u) => u.id === id)
  if (foundUser) {
    selectedUser.value = foundUser
    return
  }

  // If not found and allowSeedId is true, try to load from API
  if (props.allowSeedId) {
    try {
      const { getUserById } = await import('@/modules/Users/api')
      const response = await getUserById(id)

      if (response.result) {
        const user = new UserDto(response.result)
        selectedUser.value = user

        // Add to users list if not already there
        if (!allUsers.value.find((u) => u.id === user.id)) {
          allUsers.value.unshift(user)
        }
      }
    } catch (error) {
      console.error('Failed to load user by ID:', error)
      // If user not found, keep the ID as-is (for seed mode)
      selectedUser.value = null
    }
  }
}

/**
 * Handle search in USelectMenu
 * Client-side filtering - no API call needed
 */
const handleSearch = (query: string) => {
  searchQuery.value = query
  // No API call - filtering is done in computed property
}

/**
 * Handle selection change
 */
const handleSelectionChange = (value: unknown) => {
  // USelectMenu passes the option object, extract value if needed
  const selectedValue =
    typeof value === 'object' && value !== null && 'value' in value
      ? (value as { value: string }).value
      : (value as string | undefined)

  if (!selectedValue) {
    selectedUser.value = null
    selectedId.value = undefined
    emit('update:modelValue', undefined)
    return
  }

  // Find user in list
  const user = allUsers.value.find((u) => u.id === selectedValue)
  if (user) {
    selectedUser.value = user
  } else if (props.allowSeedId) {
    // Allow raw ID selection
    selectedId.value = selectedValue
    selectedUser.value = null
    emit('update:modelValue', selectedValue)
  }
}

// Load all users on mount
onMounted(() => {
  loadAllUsers()
})
</script>

<template>
  <UFormField :label="label" :name="`user-select-${label.toLowerCase().replace(/\s+/g, '-')}`">
    <USelectMenu
      :model-value="selectedOption"
      :items="userOptions"
      :placeholder="placeholder"
      :loading="initialLoading || loading"
      :disabled="disabled"
      searchable
      :searchable-placeholder="'Search by ID or name...'"
      icon="i-lucide-user"
      @update:model-value="handleSelectionChange"
      @update:search="handleSearch"
      class="w-full"
    />
  </UFormField>
</template>
