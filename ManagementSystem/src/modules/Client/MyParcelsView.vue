<script setup lang="ts">
/**
 * My Parcels View
 * Client view for managing their own parcels (as receiver)
 */

import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { getClientReceivedParcels, confirmParcelReceived } from '@/modules/Parcels/api'
import { ParcelDto, type ParcelStatus } from '@/modules/Parcels/model.type'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'
import { defineAsyncComponent } from 'vue'

const PageHeader = defineAsyncComponent(() => import('@/common/components/PageHeader.vue'))

const router = useRouter()
const toast = useToast()
const currentUser = getCurrentUser()

const parcels = ref<ParcelDto[]>([])
const loading = ref(false)
const page = ref(0)
const pageSize = ref(10)
const total = ref(0)
const confirmingParcelId = ref<string | null>(null)

const paginationSummary = computed(() => {
  if (total.value === 0) {
    return { start: 0, end: 0 }
  }
  const start = page.value * pageSize.value + 1
  const end = Math.min((page.value + 1) * pageSize.value, total.value)
  return { start, end }
})

const loadParcels = async () => {
  if (!currentUser?.id) return

  loading.value = true
  try {
    const response = await getClientReceivedParcels({
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
  const colorMap: Record<ParcelStatus, 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'> = {
    IN_WAREHOUSE: 'neutral',
    ON_ROUTE: 'primary',
    DELIVERED: 'success',
    SUCCEEDED: 'success',
    FAILED: 'error',
    DELAYED: 'warning',
    DISPUTE: 'info',
    LOST: 'error',
  }
  return colorMap[status] || 'neutral'
}

const handlePageChange = (newPage: number) => {
  if (newPage === page.value) return
  page.value = Math.max(newPage, 0)
  loadParcels()
}

const isConfirming = (parcelId: string) => confirmingParcelId.value === parcelId

const canConfirmParcel = (parcel: ParcelDto) => parcel.status === 'DELIVERED'

const handleConfirmReceived = async (parcel: ParcelDto) => {
  if (!canConfirmParcel(parcel)) return
  confirmingParcelId.value = parcel.id
  try {
    await confirmParcelReceived(parcel.id, {
      confirmationSource: 'WEB_CLIENT',
    })
    toast.add({
      title: 'Parcel confirmed',
      description: `Parcel ${parcel.code} marked as received`,
      color: 'success',
    })
    await loadParcels()
  } catch (error) {
    console.error('Failed to confirm parcel:', error)
    toast.add({
      title: 'Error',
      description: 'Failed to confirm parcel delivery',
      color: 'error',
    })
  } finally {
    confirmingParcelId.value = null
  }
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

    <div class="space-y-4">
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
        :loading="loading"
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
          <div v-if="canConfirmParcel(row.original)" class="mt-2">
            <UButton
              size="xs"
              color="primary"
              :loading="isConfirming(row.original.id)"
              @click="handleConfirmReceived(row.original)"
            >
              {{ isConfirming(row.original.id) ? 'Confirming...' : 'Confirm received' }}
            </UButton>
          </div>
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

      <div v-if="!loading && parcels.length === 0" class="text-center py-12">
        <UIcon name="i-heroicons-cube" class="w-16 h-16 text-gray-400 mx-auto mb-4" />
        <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">
          No parcels yet
        </h3>
        <p class="text-gray-500 mb-4">Create your first parcel to get started</p>
        <UButton color="primary" icon="i-heroicons-plus" @click="goToCreateParcel">
          Create Parcel
        </UButton>
      </div>

      <div
        v-else
        class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between"
      >
        <div class="text-sm text-gray-600 dark:text-gray-400">
          Showing
          <span class="font-semibold">
            {{ paginationSummary.start }}–{{ paginationSummary.end }}
          </span>
          of {{ total }} parcels
        </div>
        <UPagination
          :model-value="page"
          :page-count="pageSize"
          :total="total"
          :max="7"
          @update:page="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>
