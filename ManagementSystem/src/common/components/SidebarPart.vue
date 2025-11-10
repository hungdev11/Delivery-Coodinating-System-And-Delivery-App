<template>
  <div class="flex h-screen bg-gray-50">
    <!-- Sidebar -->
    <div
      :class="[
        'bg-white shadow-lg transition-all duration-300 ease-in-out flex flex-col',
        isCollapsed ? 'w-20' : 'w-80',
      ]"
    >
      <!-- Header -->
      <div class="p-6 border-b border-gray-200 flex-shrink-0">
        <div class="flex items-center">
          <div class="flex-shrink-0">
            <div class="w-10 h-10 bg-orange-500 rounded-lg flex items-center justify-center">
              <UIcon name="i-heroicons-folder-open" class="w-6 h-6 text-white" />
            </div>
          </div>
          <div v-if="!isCollapsed" class="ml-4 flex-1">
            <h2 class="text-xl font-bold text-gray-900">CRM</h2>
            <p class="text-sm text-gray-500">Quản lý đơn hàng</p>
          </div>
        </div>
      </div>

      <!-- Navigation -->
      <div class="flex-shrink-0">
        <UNavigationMenu
          tooltip
          highlight
          highlight-color="primary"
          color="primary"
          orientation="vertical"
          :items="navigationItems"
          class="w-full mt-4 mx-auto"
          :collapsed="isCollapsed"
          :ui="{
            link: ['px-4 py-3 text-base font-medium', isCollapsed ? 'justify-center' : ''],
            linkLeadingIcon: 'w-5 h-5',
            childLink: 'px-4 py-2 text-sm',
            content: 'p-2',
          }"
        />
      </div>

      <!-- Spacer to push User Section to bottom -->
      <div class="flex-1"></div>

      <!-- User Section -->
      <div class="p-4 border-t border-gray-200 bg-white flex-shrink-0">
        <div class="flex items-center">
          <div class="flex-shrink-0">
            <div class="w-10 h-10 bg-orange-500 rounded-full flex items-center justify-center">
              <span class="text-base font-medium text-white">
                {{
                  currentUser?.firstName && currentUser?.lastName
                    ? currentUser.firstName.charAt(0) + currentUser.lastName.charAt(0)
                    : currentUser?.username?.charAt(0)?.toUpperCase() || 'U'
                }}
              </span>
            </div>
          </div>
          <div v-if="!isCollapsed" class="ml-4 flex-1">
            <p class="text-sm font-medium text-gray-900">
              {{
                currentUser?.firstName && currentUser?.lastName
                  ? currentUser.firstName + ' ' + currentUser.lastName
                  : currentUser?.username || 'Guest User'
              }}
            </p>
            <p class="text-xs text-gray-500">{{ currentUser?.email || 'No email' }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="flex-1 flex flex-col overflow-hidden">
      <!-- Top Navbar with Toggle Button -->
      <header class="bg-white shadow-sm border-b border-gray-200">
        <div class="flex items-center justify-between px-4 py-3">
          <div class="flex items-center">
            <UButton
              @click="toggleSidebar"
              variant="ghost"
              color="neutral"
              icon="i-heroicons-bars-3"
              class="mr-3"
            />
            <h1 class="text-lg font-semibold text-gray-900">{{ 'Dashboard' }}</h1>
          </div>
          <div class="flex items-center space-x-4">
            <div id="pageNavAction" class="flex justify-between items-center"></div>
            <!-- Placeholder for additional navbar items -->
            <UButton variant="ghost" color="neutral" icon="i-heroicons-bell" />
            <UButton variant="ghost" color="neutral" icon="i-heroicons-cog-6-tooth" />
          </div>
        </div>
      </header>

      <!-- Page Content -->
      <main class="flex-1 overflow-auto p-6">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useSidebarStore } from '@/common/store/sidebar.store'
import { getCurrentUser, getUserRoles } from '@/common/guards/roleGuard.guard'
import type { NavigationMenuItem } from '@nuxt/ui'
import { storeToRefs } from 'pinia'
import { computed } from 'vue'

const sidebarStore = useSidebarStore()
const { isCollapsed } = storeToRefs(sidebarStore)
const { toggleSidebar } = sidebarStore

// Get current user roles
const userRoles = computed(() => getUserRoles())
const isAdmin = computed(() => userRoles.value.includes('ADMIN'))

// Navigation items
const navigationItems = computed<NavigationMenuItem[][]>(() => {
  const communicationChildren: NavigationMenuItem[] = [
    {
      label: 'Conversations',
      to: '/communication',
      icon: 'i-heroicons-chat-bubble-left-right',
    },
  ]

  // Add Proposal Configs only for Admin
  if (isAdmin.value) {
    communicationChildren.push({
      label: 'Proposal Configs',
      to: '/communication/proposals/configs',
      icon: 'i-heroicons-cog-6-tooth',
    })
  }

  return [
    [
      {
        label: 'Dashboard',
        to: '/',
        icon: 'i-heroicons-home',
      },
      {
        label: 'Users',
        icon: 'i-heroicons-user-group',
        children: [
          {
            label: 'List',
            to: '/users',
            icon: 'i-heroicons-user-group',
          },
        ],
      },
      {
        label: 'Delivery',
        icon: 'i-heroicons-truck',
        children: [
          {
            label: 'Shippers',
            to: '/delivery/shippers',
            icon: 'i-heroicons-truck',
          },
        ],
      },
      {
        label: 'Zones',
        icon: 'i-heroicons-map',
        children: [
          {
            label: 'List',
            to: '/zones',
            icon: 'i-heroicons-rectangle-stack',
          },
          {
            label: 'Map',
            to: '/zones/map',
            icon: 'i-heroicons-map',
          },
          {
            label: 'Demo Routing',
            to: '/zones/map/demo-routing',
            icon: 'i-heroicons-arrow-path-rounded-square',
          },
        ],
      },
      {
        label: 'Addresses',
        icon: 'i-heroicons-map-pin',
        children: [
          {
            label: 'Picker',
            to: '/addresses/picker',
            icon: 'i-heroicons-map-pin',
          },
        ],
      },
      {
        label: 'Communication',
        icon: 'i-heroicons-chat-bubble-left-right',
        children: communicationChildren,
      },
      {
        label: 'Settings',
        to: '/settings',
        icon: 'i-heroicons-cog-6-tooth',
      },
    ],
  ]
})

const currentUser = computed(() => {
  try {
    return getCurrentUser()
  } catch (error) {
    console.warn('Error getting current user:', error)
    return null
  }
})
</script>

<style scoped>
/* Custom styles for smooth transitions */
.transition-all {
  transition-property: all;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 300ms;
}
</style>
