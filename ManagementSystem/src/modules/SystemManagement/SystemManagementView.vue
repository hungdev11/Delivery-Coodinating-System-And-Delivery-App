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
  getOSRMHealth,
  getOSRMDeploymentStatus,
  buildOSRMInstance,
  buildAllOSRMInstances,
  startOSRMInstance,
  stopOSRMInstance,
  rollingRestartOSRM,
  type AllServicesHealth,
  type OSRMStatus,
  type OSRMHealth,
  type OSRMDeploymentStatus,
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
const osrmHealth = ref<OSRMHealth | null>(null)
const deploymentStatus = ref<OSRMDeploymentStatus | null>(null)
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

const osrmStatusColor = (status: string) => {
  switch (status?.toUpperCase()) {
    case 'RUNNING':
    case 'ACTIVE':
      return 'green'
    case 'STOPPED':
    case 'INACTIVE':
      return 'red'
    case 'BUILDING':
      return 'yellow'
    default:
      return 'gray'
  }
}

// Methods
const loadData = async () => {
  loading.value = true
  error.value = null

  try {
    await Promise.all([
      loadServicesHealth(),
      loadOSRMStatus(),
      loadOSRMHealth(),
      loadDeploymentStatus(),
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

const loadOSRMHealth = async () => {
  try {
    const response = await getOSRMHealth()
    osrmHealth.value = response.result
  } catch (e: any) {
    console.error('Failed to load OSRM health:', e)
  }
}

const loadDeploymentStatus = async () => {
  try {
    const response = await getOSRMDeploymentStatus()
    deploymentStatus.value = response.result
  } catch (e: any) {
    console.error('Failed to load deployment status:', e)
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

const handleBuild = async (instanceId?: number) => {
  const key = instanceId ? `build-${instanceId}` : 'build-all'
  actionLoading.value[key] = true

  try {
    if (instanceId) {
      await buildOSRMInstance(instanceId)
    } else {
      await buildAllOSRMInstances()
    }
    await loadData()
  } catch (e: any) {
    error.value = e.message || 'Failed to build OSRM instance'
    console.error('Failed to build:', e)
  } finally {
    actionLoading.value[key] = false
  }
}

const handleStart = async (instanceId: number) => {
  actionLoading.value[`start-${instanceId}`] = true

  try {
    await startOSRMInstance(instanceId)
    await loadData()
  } catch (e: any) {
    error.value = e.message || 'Failed to start OSRM instance'
    console.error('Failed to start:', e)
  } finally {
    actionLoading.value[`start-${instanceId}`] = false
  }
}

const handleStop = async (instanceId: number) => {
  actionLoading.value[`stop-${instanceId}`] = true

  try {
    await stopOSRMInstance(instanceId)
    await loadData()
  } catch (e: any) {
    error.value = e.message || 'Failed to stop OSRM instance'
    console.error('Failed to stop:', e)
  } finally {
    actionLoading.value[`stop-${instanceId}`] = false
  }
}

const handleRollingRestart = async () => {
  actionLoading.value['rolling-restart'] = true

  try {
    await rollingRestartOSRM()
    await loadData()
  } catch (e: any) {
    error.value = e.message || 'Failed to perform rolling restart'
    console.error('Failed to rolling restart:', e)
  } finally {
    actionLoading.value['rolling-restart'] = false
  }
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
              v-if="osrmHealth"
              :color="osrmHealth.overallHealthy ? 'green' : 'red'"
              variant="soft"
            >
              {{ osrmHealth.overallHealthy ? 'Healthy' : 'Unhealthy' }}
            </UBadge>
          </div>
        </template>

        <div class="space-y-4">
          <!-- OSRM Instances Status -->
          <div v-if="osrmStatus">
            <h4 class="font-medium mb-3">Instances Status</h4>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div
                v-for="instance in osrmStatus.instances"
                :key="instance.id"
                class="p-4 border rounded-lg"
              >
                <div class="flex items-center justify-between mb-2">
                  <span class="font-medium">Instance {{ instance.id }}</span>
                  <UBadge
                    :color="osrmStatusColor(instance.status)"
                    variant="soft"
                  >
                    {{ instance.status }}
                  </UBadge>
                </div>
                <div v-if="instance.port" class="text-sm text-gray-500">
                  Port: {{ instance.port }}
                </div>
                <div v-if="instance.dataPath" class="text-sm text-gray-500">
                  Path: {{ instance.dataPath }}
                </div>
                <div class="flex gap-2 mt-3">
                  <UButton
                    size="sm"
                    :loading="actionLoading[`start-${instance.id}`]"
                    @click="handleStart(instance.id)"
                  >
                    Start
                  </UButton>
                  <UButton
                    size="sm"
                    color="red"
                    variant="soft"
                    :loading="actionLoading[`stop-${instance.id}`]"
                    @click="handleStop(instance.id)"
                  >
                    Stop
                  </UButton>
                  <UButton
                    size="sm"
                    color="yellow"
                    variant="soft"
                    :loading="actionLoading[`build-${instance.id}`]"
                    @click="handleBuild(instance.id)"
                  >
                    Build
                  </UButton>
                </div>
              </div>
            </div>
          </div>

          <!-- OSRM Actions -->
          <div class="border-t pt-4">
            <h4 class="font-medium mb-3">Actions</h4>
            <div class="flex flex-wrap gap-2">
              <UButton
                :loading="actionLoading['build-all']"
                @click="handleBuild()"
              >
                Build All Instances
              </UButton>
              <UButton
                color="blue"
                :loading="actionLoading['rolling-restart']"
                @click="handleRollingRestart()"
              >
                Rolling Restart
              </UButton>
            </div>
          </div>

          <!-- Deployment Status -->
          <div v-if="deploymentStatus" class="border-t pt-4">
            <h4 class="font-medium mb-3">Deployment Status</h4>
            <div class="space-y-2">
              <div
                v-for="instance in deploymentStatus.instances"
                :key="instance.instanceId"
                class="p-3 border rounded-lg"
              >
                <div class="flex items-center justify-between">
                  <span class="font-medium">Instance {{ instance.instanceId }}</span>
                  <UBadge
                    :color="osrmStatusColor(instance.status)"
                    variant="soft"
                  >
                    {{ instance.status }}
                  </UBadge>
                </div>
                <div v-if="instance.lastBuild" class="text-sm text-gray-500 mt-1">
                  Last Build: {{ new Date(instance.lastBuild).toLocaleString() }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </UCard>
    </div>
  </div>
</template>
