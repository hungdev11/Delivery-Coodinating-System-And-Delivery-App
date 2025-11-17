<script setup lang="ts">
/**
 * My Parcels View
 * Client view for managing their own parcels (as receiver)
 */

import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { getParcelsV2 } from '@/modules/Parcels/api'
import { ParcelDto, type ParcelStatus } from '@/modules/Parcels/model.type'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'
import { defineAsyncComponent } from 'vue'
import type { FilterGroup } from '@/common/types/filter'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))

const router = useRouter()
const toast = useToast()
const currentUser = getCurrentUser()

const parcels = ref<ParcelDto[]>([])
const loading = ref(false)
const page = ref(0)
const pageSize = ref(10)
const total = ref(0)

const loadParcels = async () => {
  if (!currentUser?.id) return

  loading.value = true
  try {
    // Filter parcels where receiverId = currentUserId
    const filterGroup: FilterGroup = {
      logic: 'AND',
      conditions: [
        {
          field: 'receiverId',
          operator: 'eq',
          value: currentUser.id,
          logic: 'AND',
        },
      ],
    }

    const response = await getParcelsV2({
      filters: filterGroup,
      page: page.value,
      size: pageSize.value,
      sorts: [
        {
          field: 'createdAt',
          direction: 'desc',
        },
      ],
    })

    if (response.result) {
      parcels.value = response.result.data.map((p) => new ParcelDto(p))
      total.value = response.result.page.totalElements
    }
  } catch (error) {
    console.error('Failed to load parcels:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to load parcels',
      color: 'error',
    })
  } finally {
    loading.value = false
  }
}

const goToCreateParcel = () => {
  router.push({ name: 'client-create-parcel' })
}

const getStatusColor = (status: ParcelStatus) => {
  const colorMap: Record<ParcelStatus, string> = {
    IN_WAREHOUSE: 'gray',
    ON_ROUTE: 'blue',
    DELIVERED: 'green',
    SUCCEEDED: 'success',
    FAILED: 'error',
    DELAYED: 'warning',
    DISPUTE: 'orange',
    LOST: 'red',
  }
  return colorMap[status] || 'gray'
}

const handlePageChange = (newPage: number) => {
  page.value = newPage - 1 // Convert from 1-indexed to 0-indexed
  loadParcels()
}

onMounted(() => {
  loadParcels()
})
</script>

<template>
  <div class="container mx-auto px-4 py-6 space-y-6">
    <PageHeader
      title="My Parcels"
      description="View and manage your parcels"
    >
      <template #actions>
        <UButton color="primary" icon="i-heroicons-plus" @click="goToCreateParcel">
          Create Parcel
        </UButton>
      </template>
    </PageHeader>

    <USkeleton v-if="loading" class="h-64 w-full" />

    <div v-else-if="parcels.length === 0" class="text-center py-12">
      <UIcon name="i-heroicons-cube" class="w-16 h-16 text-gray-400 mx-auto mb-4" />
      <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">
        No parcels yet
      </h3>
      <p class="text-gray-500 mb-4">Create your first parcel to get started</p>
      <UButton color="primary" icon="i-heroicons-plus" @click="goToCreateParcel">
        Create Parcel
      </UButton>
    </div>

    <div v-else class="space-y-4">
      <UTable
        :data="parcels"
        :columns="[
          { accessorKey: 'code', header: 'Code' },
          { accessorKey: 'senderName', header: 'Sender' },
          { accessorKey: 'targetDestination', header: 'Destination' },
          { accessorKey: 'status', header: 'Status' },
          { accessorKey: 'deliveryType', header: 'Type' },
          { accessorKey: 'createdAt', header: 'Created' },
        ]"
      >
        <template #cell(code)="{ row }">
          <span class="font-mono text-sm">{{ row.original.code }}</span>
        </template>
        <template #cell(status)="{ row }">
          <UBadge
            :color="getStatusColor(row.original.status)"
            variant="soft"
            class="capitalize"
          >
            {{ row.original.displayStatus }}
          </UBadge>
        </template>
        <template #cell(deliveryType)="{ row }">
          <UBadge variant="outline" class="capitalize">
            {{ row.original.deliveryType }}
          </UBadge>
        </template>
        <template #cell(createdAt)="{ row }">
          {{ new Date(row.original.createdAt).toLocaleString() }}
        </template>
      </UTable>

      <div class="flex justify-center">
        <UPagination
          v-model="page"
          :page-count="pageSize"
          :total="total"
          :max="7"
          @update:model-value="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>
