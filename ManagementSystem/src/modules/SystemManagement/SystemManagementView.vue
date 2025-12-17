<script setup lang="ts">
/**
 * System Management View
 *
 * View for managing system health and OSRM data
 */

import { onMounted, ref, computed, resolveComponent } from 'vue'
import { PageHeader } from '@/common/components'
import {
  getAllServicesHealth,
  getOSRMStatus,
  generateV2OSRM,
  extractCompleteOSM,
  getOSRMContainerStatus,
  startOSRMContainer,
  stopOSRMContainer,
  restartOSRMContainer,
  type AllServicesHealth,
  type OSRMStatus,
  type ContainerStatus,
} from './api'

const UCard = resolveComponent('UCard')
const UButton = resolveComponent('UButton')
const UBadge = resolveComponent('UBadge')
const UAlert = resolveComponent('UAlert')
const UIcon = resolveComponent('UIcon')
const UProgress = resolveComponent('UProgress')

// State
const loading = ref(false)
const refreshing = ref(false)
const servicesHealth = ref<AllServicesHealth | null>(null)
const osrmStatus = ref<OSRMStatus | null>(null)
const containerStatuses = ref<ContainerStatus[]>([])
const error = ref<string | null>(null)
const actionLoading = ref<Record<string, boolean>>({})

// Computed
const overallStatus = computed(() => {
  if (!servicesHealth.value) return 'UNKNOWN'
  return servicesHealth.value.overallStatus
})

const statusColor = (status: string) => {
  switch (status?.toUpperCase()) {
    case 'UP':
      return 'green'
    case 'DOWN':
      return 'red'
    case 'DEGRADED':
      return 'yellow'
    default:
      return 'gray'
  }
}

const formatServiceName = (serviceName: string) => {
  return serviceName
    .split('-')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

const getServiceIcon = (serviceName: string) => {
  const iconMap: Record<string, string> = {
    'api-gateway': 'i-heroicons-server',
    'user-service': 'i-heroicons-user-group',
    'settings-service': 'i-heroicons-cog-6-tooth',
    'zone-service': 'i-heroicons-map',
    'parcel-service': 'i-heroicons-cube',
    'session-service': 'i-heroicons-truck',
    'communication-service': 'i-heroicons-chat-bubble-left-right',
  }
  return iconMap[serviceName] || 'i-heroicons-server'
}

// Methods
const loadData = async () => {
  loading.value = true
  error.value = null

  try {
    await Promise.all([
      loadServicesHealth(),
      loadOSRMStatus(),
      loadContainerStatuses(),
    ])
  } catch (e: any) {
    error.value = e.message || 'Failed to load system data'
    console.error('Failed to load system data:', e)
  } finally {
    loading.value = false
  }
}

const loadServicesHealth = async () => {
  try {
    const response = await getAllServicesHealth()
    servicesHealth.value = response.result
  } catch (e: any) {
    console.error('Failed to load services health:', e)
  }
}

const loadOSRMStatus = async () => {
  try {
    const response = await getOSRMStatus()
    osrmStatus.value = response.result
  } catch (e: any) {
    console.error('Failed to load OSRM status:', e)
  }
}

const loadContainerStatuses = async () => {
  try {
    const response = await getOSRMContainerStatus()
    containerStatuses.value = response.result || []
  } catch (e: any) {
    console.error('Failed to load container statuses:', e)
  }
}

const refresh = async () => {
  refreshing.value = true
  try {
    await loadData()
  } finally {
    refreshing.value = false
  }
}

const handleGenerateOSRM = async () => {
  actionLoading.value['generate'] = true
  error.value = null

  try {
    await generateV2OSRM()
    // Wait a bit then refresh status
    setTimeout(() => {
      loadOSRMStatus()
      loadContainerStatuses()
    }, 2000)
  } catch (e: any) {
    error.value = e.message || 'Failed to generate OSRM data'
    console.error('Failed to generate OSRM:', e)
  } finally {
    actionLoading.value['generate'] = false
  }
}

const handleExtractComplete = async () => {
  actionLoading.value['extract'] = true
  error.value = null

  try {
    await extractCompleteOSM()
    // Wait a bit then refresh status
    setTimeout(() => {
      loadOSRMStatus()
    }, 2000)
  } catch (e: any) {
    error.value = e.message || 'Failed to extract OSM data'
    console.error('Failed to extract OSM:', e)
  } finally {
    actionLoading.value['extract'] = false
  }
}

const handleContainerAction = async (model: string, action: 'start' | 'stop' | 'restart') => {
  const key = `${action}-${model}`
  actionLoading.value[key] = true
  error.value = null

  try {
    let result
    switch (action) {
      case 'start':
        result = await startOSRMContainer(model)
        break
      case 'stop':
        result = await stopOSRMContainer(model)
        break
      case 'restart':
        result = await restartOSRMContainer(model)
        break
    }
    
    // Refresh container statuses
    setTimeout(() => {
      loadContainerStatuses()
    }, 1000)
  } catch (e: any) {
    error.value = e.message || `Failed to ${action} container`
    console.error(`Failed to ${action} container:`, e)
  } finally {
    actionLoading.value[key] = false
  }
}

const getContainerStatus = (modelName: string) => {
  return containerStatuses.value.find(c => c.model === modelName)
}

// Lifecycle
onMounted(() => {
  loadData()

  // Auto-refresh every 30 seconds
  setInterval(() => {
    if (!loading.value && !refreshing.value) {
      refresh()
    }
  }, 30000)
})
</script>

<template>
  <div class="p-6">
    <PageHeader
      title="System Management"
      description="Monitor system health and manage OSRM routing data"
    >
      <template #actions>
        <UButton
          :loading="refreshing"
          @click="refresh"
        >
          <UIcon name="i-heroicons-arrow-path" class="w-4 h-4 mr-2" />
          Refresh
        </UButton>
      </template>
    </PageHeader>

    <div v-if="error" class="mb-4">
      <UAlert
        color="red"
        variant="soft"
        :title="error"
        @close="error = null"
      />
    </div>

    <div v-if="loading && !servicesHealth" class="flex justify-center items-center py-12">
      <UProgress />
    </div>

    <div v-else class="space-y-6">
      <!-- Services Health Section -->
      <UCard>
        <template #header>
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-semibold">Services Health</h3>
            <UBadge
              :color="statusColor(overallStatus)"
              variant="soft"
            >
              {{ overallStatus }}
            </UBadge>
          </div>
        </template>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div
            v-for="(health, serviceName) in servicesHealth?.services"
            :key="serviceName"
            class="p-4 border rounded-lg hover:shadow-md transition-shadow"
            :class="{
              'border-green-200 bg-green-50 dark:bg-green-950 dark:border-green-800': health.status === 'UP',
              'border-red-200 bg-red-50 dark:bg-red-950 dark:border-red-800': health.status === 'DOWN',
              'border-yellow-200 bg-yellow-50 dark:bg-yellow-950 dark:border-yellow-800': health.status === 'DEGRADED',
            }"
          >
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <UIcon
                  :name="getServiceIcon(serviceName)"
                  class="w-5 h-5"
                  :class="{
                    'text-green-600 dark:text-green-400': health.status === 'UP',
                    'text-red-600 dark:text-red-400': health.status === 'DOWN',
                    'text-yellow-600 dark:text-yellow-400': health.status === 'DEGRADED',
                  }"
                />
                <span class="font-medium capitalize">{{ formatServiceName(serviceName) }}</span>
              </div>
              <UBadge
                :color="statusColor(health.status)"
                variant="soft"
                size="sm"
              >
                {{ health.status }}
              </UBadge>
            </div>

            <div v-if="health.timestamp" class="text-xs text-gray-500 dark:text-gray-400 mb-2">
              <UIcon name="i-heroicons-clock" class="w-3 h-3 inline mr-1" />
              {{ new Date(health.timestamp).toLocaleString() }}
            </div>

            <div v-if="health.error" class="text-xs text-red-600 dark:text-red-400 mt-2 p-2 bg-red-100 dark:bg-red-900/20 rounded">
              <UIcon name="i-heroicons-exclamation-triangle" class="w-3 h-3 inline mr-1" />
              {{ health.error }}
            </div>

            <!-- Additional health details if available -->
            <div v-if="health.components" class="mt-2 text-xs text-gray-600 dark:text-gray-400">
              <div v-for="(component, key) in health.components" :key="key" class="flex items-center gap-1">
                <UIcon
                  :name="component.status === 'UP' ? 'i-heroicons-check-circle' : 'i-heroicons-x-circle'"
                  class="w-3 h-3"
                  :class="component.status === 'UP' ? 'text-green-600' : 'text-red-600'"
                />
                <span class="capitalize">{{ String(key).replace(/([A-Z])/g, ' $1').trim() }}</span>
              </div>
            </div>
          </div>
        </div>

        <template #footer>
          <div class="text-sm text-gray-500">
            Healthy: {{ servicesHealth?.healthyCount }}/{{ servicesHealth?.totalCount }}
          </div>
        </template>
      </UCard>

      <!-- OSRM Management Section -->
      <UCard>
        <template #header>
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-semibold">OSRM Data Management</h3>
            <UBadge
              v-if="osrmStatus"
              :color="osrmStatus.ready ? 'green' : 'yellow'"
              variant="soft"
            >
              {{ osrmStatus.ready ? 'Ready' : 'Incomplete' }}
            </UBadge>
          </div>
        </template>

        <div class="space-y-4">
          <!-- OSRM Models Status -->
          <div v-if="osrmStatus">
            <h4 class="font-medium mb-3">OSRM Models Status</h4>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div
                v-for="model in osrmStatus.models"
                :key="model.name"
                class="p-4 border rounded-lg"
                :class="{
                  'border-green-200 bg-green-50 dark:bg-green-950 dark:border-green-800': model.exists,
                  'border-red-200 bg-red-50 dark:bg-red-950 dark:border-red-800': !model.exists,
                }"
              >
                <div class="flex items-center justify-between mb-2">
                  <span class="font-medium">{{ model.name }}</span>
                  <UBadge
                    :color="model.exists ? 'green' : 'red'"
                    variant="soft"
                  >
                    {{ model.exists ? 'Ready' : 'Missing' }}
                  </UBadge>
                </div>
                <div class="text-sm text-gray-500">
                  Path: {{ model.path }}
                </div>
                
                <!-- Container Status -->
                <div v-if="getContainerStatus(model.name)" class="mt-2">
                  <div class="flex items-center gap-2 mb-2">
                    <UBadge
                      :color="getContainerStatus(model.name)?.status === 'running' ? 'green' : 'gray'"
                      variant="soft"
                      size="sm"
                    >
                      {{ getContainerStatus(model.name)?.status || 'unknown' }}
                    </UBadge>
                    <div v-if="getContainerStatus(model.name)?.health" class="flex items-center gap-1 text-xs">
                      <UIcon
                        :name="getContainerStatus(model.name)?.health?.healthy ? 'i-heroicons-check-circle' : 'i-heroicons-x-circle'"
                        class="w-3 h-3"
                        :class="getContainerStatus(model.name)?.health?.healthy ? 'text-green-600' : 'text-red-600'"
                      />
                      <span>{{ getContainerStatus(model.name)?.health?.healthy ? 'Healthy' : 'Unhealthy' }}</span>
                    </div>
                  </div>
                  
                  <!-- Container Actions -->
                  <div class="flex gap-1 mt-2">
                    <UButton
                      v-if="getContainerStatus(model.name)?.status !== 'running'"
                      size="xs"
                      color="green"
                      :loading="actionLoading[`start-${model.name}`]"
                      @click="handleContainerAction(model.name, 'start')"
                    >
                      Start
                    </UButton>
                    <UButton
                      v-if="getContainerStatus(model.name)?.status === 'running'"
                      size="xs"
                      color="red"
                      :loading="actionLoading[`stop-${model.name}`]"
                      @click="handleContainerAction(model.name, 'stop')"
                    >
                      Stop
                    </UButton>
                    <UButton
                      v-if="getContainerStatus(model.name)?.status === 'running'"
                      size="xs"
                      color="yellow"
                      :loading="actionLoading[`restart-${model.name}`]"
                      @click="handleContainerAction(model.name, 'restart')"
                    >
                      Restart
                    </UButton>
                  </div>
                </div>
                
                <!-- Build Status -->
                <div v-if="osrmStatus.buildStatus" class="mt-2 text-xs">
                  <div v-for="build in osrmStatus.buildStatus" :key="build.model">
                    <div v-if="build.model === model.name">
                      <div v-if="build.currentBuild" class="text-yellow-600 dark:text-yellow-400">
                        <UIcon name="i-heroicons-clock" class="w-3 h-3 inline mr-1" />
                        Building: {{ build.currentBuild.status }}
                      </div>
                      <div v-else-if="build.latestReady" class="text-green-600 dark:text-green-400">
                        <UIcon name="i-heroicons-check-circle" class="w-3 h-3 inline mr-1" />
                        Ready to deploy
                      </div>
                      <div v-if="build.latestDeployed" class="text-blue-600 dark:text-blue-400 mt-1">
                        <UIcon name="i-heroicons-rocket-launch" class="w-3 h-3 inline mr-1" />
                        Deployed
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="mt-4 p-3 bg-gray-50 dark:bg-gray-900 rounded-lg">
              <div class="text-sm text-gray-600 dark:text-gray-400">
                <strong>Status:</strong> {{ osrmStatus.existingCount }}/{{ osrmStatus.totalModels }} models ready
              </div>
            </div>
          </div>

          <!-- OSRM Actions -->
          <div class="border-t pt-4">
            <h4 class="font-medium mb-3">Actions</h4>
            <div class="flex flex-wrap gap-2">
              <UButton
                color="primary"
                :loading="actionLoading['extract']"
                @click="handleExtractComplete"
              >
                <UIcon name="i-heroicons-arrow-down-tray" class="w-4 h-4 mr-2" />
                Extract Complete OSM Data
              </UButton>
              <UButton
                color="primary"
                :loading="actionLoading['generate']"
                @click="handleGenerateOSRM"
              >
                <UIcon name="i-heroicons-cog-6-tooth" class="w-4 h-4 mr-2" />
                Generate OSRM Data (All Models)
              </UButton>
            </div>
            <div class="mt-2 text-sm text-gray-500">
              <div class="mb-1">
                <strong>Extract:</strong> Extract complete OSM data (routing + addresses) from source files.
              </div>
              <div>
                <strong>Generate:</strong> Generate all 4 OSRM models (osrm-full, osrm-rating-only, osrm-blocking-only, osrm-base) from the current database state.
              </div>
            </div>
          </div>
        </div>
      </UCard>
    </div>
  </div>
</template>
