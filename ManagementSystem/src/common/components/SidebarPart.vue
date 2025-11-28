<template>
  <div class="flex flex-col md:flex-row h-screen bg-gray-50 dark:bg-gray-950">
    <!-- Desktop Sidebar (hidden on mobile for client-only users) -->
    <div
      :class="[
        'bg-white dark:bg-gray-900 shadow-lg transition-all duration-300 ease-in-out flex-col',
        isCollapsed ? 'w-20' : 'w-80',
        isClientOnly ? 'hidden md:flex' : 'hidden md:flex',
      ]"
    >
      <!-- Header -->
      <div class="p-6 border-b border-gray-200 dark:border-gray-700 flex-shrink-0">
        <div class="flex items-center">
          <div class="flex-shrink-0">
            <div class="w-10 h-10 bg-orange-500 rounded-lg flex items-center justify-center cursor-pointer" @click="router.push('/')">
              <UIcon name="i-heroicons-folder-open" class="w-6 h-6 text-white" />
            </div>
          </div>
          <div v-if="!isCollapsed" class="ml-4 flex-1 cursor-pointer" @click="router.push('/')">
            <h2 class="text-xl font-bold text-gray-900 dark:text-gray-100">ERP</h2>
            <p class="text-sm text-gray-500 dark:text-gray-400">Quản lý đơn hàng</p>
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
      <div
        class="p-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 flex-shrink-0"
      >
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
            <p class="text-sm font-medium text-gray-900 dark:text-gray-100">
              {{
                currentUser?.firstName && currentUser?.lastName
                  ? currentUser.firstName + ' ' + currentUser.lastName
                  : currentUser?.username || 'Guest User'
              }}
            </p>
            <p class="text-xs text-gray-500 dark:text-gray-400">
              {{ currentUser?.email || 'No email' }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="flex-1 flex flex-col overflow-hidden">
      <!-- Top Navbar with Toggle Button (Desktop) -->
      <header
        class="bg-white dark:bg-gray-900 shadow-sm border-b border-gray-200 dark:border-gray-700"
      >
        <div class="flex items-center justify-between px-4 py-3">
          <div class="flex items-center">
            <UButton
              @click="toggleSidebar"
              variant="ghost"
              color="neutral"
              icon="i-heroicons-bars-3"
              class="mr-3 hidden md:flex"
            />
            <!-- Mobile: Show app name -->
            <div class="md:hidden flex items-center cursor-pointer" @click="router.push('/')">
              <div class="w-8 h-8 bg-orange-500 rounded-lg flex items-center justify-center mr-2">
                <UIcon name="i-heroicons-folder-open" class="w-5 h-5 text-white" />
              </div>
              <h1 class="text-lg font-semibold text-gray-900 dark:text-gray-100">ERP</h1>
            </div>
            <h1 class="text-lg font-semibold text-gray-900 dark:text-gray-100 hidden md:block">
              {{ 'Dashboard' }}
            </h1>
          </div>
          <div class="flex items-center space-x-2 md:space-x-4">
            <div id="pageNavAction" class="flex justify-between items-center"></div>
            <!-- Placeholder for additional navbar items -->
            <UButton
              :icon="mode === 'dark' ? 'i-lucide-moon' : 'i-lucide-sun'"
              color="neutral"
              variant="ghost"
              size="sm"
              class="md:size-md"
              @click="mode = mode === 'dark' ? 'light' : 'dark'"
            />
            <!-- Chat Notification Popup -->
            <ChatNotificationPopup ref="chatNotificationRef" />
            
            <UButton
              variant="ghost"
              color="neutral"
              icon="i-heroicons-bell"
              size="sm"
              class="md:size-md"
            />
            <UPopover :content="{ side: 'bottom', align: 'end' }">
              <UButton
                variant="ghost"
                color="neutral"
                icon="i-heroicons-cog-6-tooth"
                size="sm"
                class="md:size-md"
              />
              <template #content>
                <div class="p-2 min-w-48">
                  <div class="space-y-1">
                    <UButton
                      v-if="isClient"
                      variant="ghost"
                      color="neutral"
                      block
                      icon="i-heroicons-user"
                      @click="handleProfile"
                    >
                      My Profile
                    </UButton>
                    <UButton
                      v-if="showAdminMenu"
                      variant="ghost"
                      color="neutral"
                      block
                      icon="i-heroicons-cog-6-tooth"
                      @click="handleSettings"
                    >
                      Settings
                    </UButton>
                    <UButton
                      variant="ghost"
                      color="neutral"
                      block
                      icon="i-heroicons-arrow-right-on-rectangle"
                      @click="handleLogout"
                    >
                      Logout
                    </UButton>
                  </div>
                </div>
              </template>
            </UPopover>
          </div>
        </div>
      </header>

      <!-- Page Content -->
      <main
        :class="[
          'flex-1 overflow-auto p-4 md:p-6',
          isClientOnly || showAdminMenu ? 'pb-20 md:pb-6' : '',
        ]"
      >
        <slot />
      </main>
    </div>

    <!-- Mobile Bottom Navigation (Only for client-only users) -->
    <nav
      v-if="isClientOnly"
      class="md:hidden fixed bottom-0 left-0 right-0 bg-white dark:bg-gray-900 border-t border-gray-200 dark:border-gray-700 shadow-lg z-50 safe-area-bottom"
    >
      <div class="flex items-center justify-around h-16">
        <!-- Addresses -->
        <router-link
          to="/client/addresses"
          class="flex flex-col items-center justify-center flex-1 h-full"
          :class="[
            isActiveRoute('/client/addresses')
              ? 'text-orange-500'
              : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300',
          ]"
        >
          <UIcon name="i-heroicons-map-pin" class="w-6 h-6" />
          <span class="text-xs mt-1">Địa chỉ</span>
        </router-link>

        <!-- Chat -->
        <router-link
          to="/communication"
          class="flex flex-col items-center justify-center flex-1 h-full"
          :class="[
            isActiveRoute('/communication')
              ? 'text-orange-500'
              : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300',
          ]"
        >
          <UIcon name="i-heroicons-chat-bubble-left-right" class="w-6 h-6" />
          <span class="text-xs mt-1">Chat</span>
        </router-link>

        <!-- Parcel (Center - Primary) -->
        <router-link
          to="/client/parcels"
          class="flex flex-col items-center justify-center flex-1 h-full relative"
        >
          <div
            :class="[
              'w-14 h-14 rounded-full flex items-center justify-center -mt-6 shadow-lg',
              isActiveRoute('/client/parcels')
                ? 'bg-orange-500'
                : 'bg-orange-400 hover:bg-orange-500',
            ]"
          >
            <UIcon name="i-heroicons-cube" class="w-7 h-7 text-white" />
          </div>
          <span
            :class="[
              'text-xs mt-1',
              isActiveRoute('/client/parcels')
                ? 'text-orange-500 font-medium'
                : 'text-gray-500 dark:text-gray-400',
            ]"
          >
            Đơn hàng
          </span>
        </router-link>

        <!-- Profile -->
        <router-link
          to="/client/profile"
          class="flex flex-col items-center justify-center flex-1 h-full"
          :class="[
            isActiveRoute('/client/profile')
              ? 'text-orange-500'
              : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300',
          ]"
        >
          <UIcon name="i-heroicons-user" class="w-6 h-6" />
          <span class="text-xs mt-1">Hồ sơ</span>
        </router-link>

        <!-- More (Disabled) -->
        <div
          class="flex flex-col items-center justify-center flex-1 h-full text-gray-300 dark:text-gray-600 cursor-not-allowed"
        >
          <UIcon name="i-heroicons-ellipsis-horizontal" class="w-6 h-6" />
          <span class="text-xs mt-1">Thêm</span>
        </div>
      </div>
    </nav>

    <!-- Mobile Bottom Navigation (For admin users) -->
    <nav
      v-if="showAdminMenu"
      class="md:hidden fixed bottom-0 left-0 right-0 bg-white dark:bg-gray-900 border-t border-gray-200 dark:border-gray-700 shadow-lg z-50 safe-area-bottom"
    >
      <div class="flex items-center justify-around h-16">
        <!-- Dashboard -->
        <router-link
          to="/"
          class="flex flex-col items-center justify-center flex-1 h-full"
          :class="[
            isActiveRoute('/') && route.path === '/'
              ? 'text-orange-500'
              : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300',
          ]"
        >
          <UIcon name="i-heroicons-home" class="w-6 h-6" />
          <span class="text-xs mt-1">Dashboard</span>
        </router-link>

        <!-- Communication -->
        <router-link
          to="/communication"
          class="flex flex-col items-center justify-center flex-1 h-full"
          :class="[
            isActiveRoute('/communication')
              ? 'text-orange-500'
              : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300',
          ]"
        >
          <UIcon name="i-heroicons-chat-bubble-left-right" class="w-6 h-6" />
          <span class="text-xs mt-1">Chat</span>
        </router-link>

        <!-- Parcels (Center - Primary) -->
        <router-link
          to="/parcels"
          class="flex flex-col items-center justify-center flex-1 h-full relative"
        >
          <div
            :class="[
              'w-14 h-14 rounded-full flex items-center justify-center -mt-6 shadow-lg',
              isActiveRoute('/parcels') ? 'bg-orange-500' : 'bg-orange-400 hover:bg-orange-500',
            ]"
          >
            <UIcon name="i-heroicons-cube" class="w-7 h-7 text-white" />
          </div>
          <span
            :class="[
              'text-xs mt-1',
              isActiveRoute('/parcels')
                ? 'text-orange-500 font-medium'
                : 'text-gray-500 dark:text-gray-400',
            ]"
          >
            Đơn hàng
          </span>
        </router-link>

        <!-- Users -->
        <router-link
          to="/users"
          class="flex flex-col items-center justify-center flex-1 h-full"
          :class="[
            isActiveRoute('/users')
              ? 'text-orange-500'
              : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300',
          ]"
        >
          <UIcon name="i-heroicons-user-group" class="w-6 h-6" />
          <span class="text-xs mt-1">Người dùng</span>
        </router-link>

        <!-- Settings -->
        <router-link
          to="/settings"
          class="flex flex-col items-center justify-center flex-1 h-full"
          :class="[
            isActiveRoute('/settings')
              ? 'text-orange-500'
              : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300',
          ]"
        >
          <UIcon name="i-heroicons-cog-6-tooth" class="w-6 h-6" />
          <span class="text-xs mt-1">Cài đặt</span>
        </router-link>
      </div>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { useSidebarStore } from '@/common/store/sidebar.store'
import { getCurrentUser, getUserRoles, removeToken } from '@/common/guards/roleGuard.guard'
import type { NavigationMenuItem } from '@nuxt/ui'
import { storeToRefs } from 'pinia'
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { logout } from '@/modules/LoginScreen/api'
import { useColorMode } from '@vueuse/core'
import ChatNotificationPopup from './ChatNotificationPopup.vue'
import { useGlobalChat } from '@/modules/Communication/composables/useGlobalChat'
import { useChatStore } from '@/stores/chatStore'
import type { MessageResponse } from '@/modules/Communication/model.type'

const mode = useColorMode({
  storageKey: 'erp-color-mode',
})

const sidebarStore = useSidebarStore()
const { isCollapsed } = storeToRefs(sidebarStore)
const { toggleSidebar } = sidebarStore

const router = useRouter()
const route = useRoute()

// Chat notification
const chatNotificationRef = ref<InstanceType<typeof ChatNotificationPopup> | null>(null)
const globalChat = useGlobalChat()
const chatStore = useChatStore()

// Get current user roles
const userRoles = computed(() => getUserRoles())
const isAdmin = computed(() => userRoles.value.includes('ADMIN'))
const isClient = computed(() => userRoles.value.includes('CLIENT'))
const isShipper = computed(() => userRoles.value.includes('SHIPPER'))
// Show admin menu if user has ADMIN or SHIPPER role (even if they also have CLIENT)
const showAdminMenu = computed(() => isAdmin.value || isShipper.value)
// Check if user is client-only (has CLIENT role but not ADMIN or SHIPPER)
const isClientOnly = computed(() => isClient.value && !showAdminMenu.value)

// Check if current route is active
const isActiveRoute = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/')
}

// Handle new message notification
const handleNewMessage = (message: MessageResponse) => {
  // Only show notification if not in the active conversation
  if (!message.conversationId) return
  
  const activeConversationId = chatStore.activeConversationId
  if (activeConversationId === message.conversationId) {
    // Don't show notification if user is viewing this conversation
    return
  }

  // Get conversation info from store
  const conversation = chatStore.getConversation(message.conversationId)
  if (!conversation) return

  // Get current user to check if message is from them
  const currentUser = getCurrentUser()
  if (message.senderId === currentUser?.id) {
    // Don't show notification for own messages
    return
  }

  // Show notification
  if (chatNotificationRef.value) {
    const preview = message.type === 'TEXT' 
      ? (typeof message.content === 'string' ? message.content : JSON.stringify(message.content))
      : message.type === 'INTERACTIVE_PROPOSAL'
        ? 'New proposal'
        : message.type === 'DELIVERY_COMPLETED'
          ? 'Delivery completed'
          : 'New message'
    
    chatNotificationRef.value.show({
      conversationId: message.conversationId,
      partnerId: conversation.partnerId,
      partnerName: conversation.partnerName,
      preview: preview.substring(0, 100),
      message,
    })
  }
}

// Setup global chat listener
onMounted(() => {
  globalChat.addListener({
    onMessageReceived: handleNewMessage,
  })
})

onUnmounted(() => {
  globalChat.removeListener({
    onMessageReceived: handleNewMessage,
  })
})

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

  // Base menu items (visible to all authenticated users)
  const baseMenuItems: NavigationMenuItem[] = [
    {
      label: 'Dashboard',
      to: '/',
      icon: 'i-heroicons-home',
    },
    {
      label: 'Communication',
      icon: 'i-heroicons-chat-bubble-left-right',
      children: communicationChildren,
    },
  ]

  // Admin/Management menu items (hidden from CLIENT-only users, but visible to ADMIN/SHIPPER even if they have CLIENT role)
  const adminMenuItems: NavigationMenuItem[] = showAdminMenu.value
    ? [
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
          label: 'Parcels',
          icon: 'i-heroicons-cube',
          children: [
            {
              label: 'List',
              to: '/parcels',
              icon: 'i-heroicons-cube',
            },
          ],
        },
        {
          label: 'Settings',
          to: '/settings',
          icon: 'i-heroicons-cog-6-tooth',
        },
      ]
    : []

  // Client menu items (visible to CLIENT role)
  const clientMenuItems: NavigationMenuItem[] = isClient.value
    ? [
        {
          label: 'My Parcels',
          to: '/client/parcels',
          icon: 'i-heroicons-cube',
        },
        {
          label: 'My Addresses',
          to: '/client/addresses',
          icon: 'i-heroicons-map-pin',
        },
        {
          label: 'My Profile',
          to: '/client/profile',
          icon: 'i-heroicons-user',
        },
      ]
    : []

  return [[...baseMenuItems, ...adminMenuItems, ...clientMenuItems]]
})

const settingsPopoverOpen = ref(false)

const currentUser = computed(() => {
  try {
    return getCurrentUser()
  } catch (error) {
    console.warn('Error getting current user:', error)
    return null
  }
})

const handleProfile = () => {
  settingsPopoverOpen.value = false
  if (isClient.value) {
    router.push({ name: 'client-profile' })
  }
}

const handleSettings = () => {
  settingsPopoverOpen.value = false
  router.push({ name: 'settings' })
}

const handleLogout = async () => {
  settingsPopoverOpen.value = false
  try {
    // Try to get refresh token from localStorage (if it was stored during login)
    const refreshToken =
      localStorage.getItem('refresh_token') || sessionStorage.getItem('refresh_token')

    // Call logout API if refresh token exists
    if (refreshToken) {
      try {
        await logout(refreshToken)
      } catch (error) {
        console.warn('Logout API call failed, continuing with local logout:', error)
      }
    }

    // Clear local authentication data
    removeToken()

    // Clear refresh token if exists
    localStorage.removeItem('refresh_token')
    sessionStorage.removeItem('refresh_token')

    // Clear any stored redirect path
    localStorage.removeItem('auth_redirect')

    // Redirect to login (force navigation)
    router
      .push({ name: 'login' })
      .then(() => {
        // Force reload to ensure clean state
        window.location.href = '/login'
      })
      .catch(() => {
        // Fallback if router push fails
        window.location.href = '/login'
      })
  } catch (error) {
    console.error('Logout error:', error)
    // Still clear local data and redirect even if API call fails
    removeToken()
    localStorage.removeItem('refresh_token')
    sessionStorage.removeItem('refresh_token')
    localStorage.removeItem('auth_redirect')

    // Force redirect to login
    router
      .push({ name: 'login' })
      .then(() => {
        window.location.href = '/login'
      })
      .catch(() => {
        window.location.href = '/login'
      })
  }
}
</script>

<style scoped>
/* Custom styles for smooth transitions */
.transition-all {
  transition-property: all;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 300ms;
}

/* Safe area for bottom navigation on devices with notch/home indicator */
.safe-area-bottom {
  padding-bottom: env(safe-area-inset-bottom, 0);
}
</style>
