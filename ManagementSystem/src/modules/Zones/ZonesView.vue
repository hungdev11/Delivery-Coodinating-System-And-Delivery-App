<script setup lang="ts">
/**
 * Zones List View
 *
 * Main view for managing zones with proper Vue 3 + Nuxt UI v3 patterns
 */

import { computed, onMounted, defineAsyncComponent } from 'vue'
import { useRouter } from 'vue-router'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'
import { PageHeader, DataTable } from '@/common/components'
import { useZones } from './composables'
import type { ZoneDto } from './model.type'

// Lazy load modals
const LazyZoneFormModal = defineAsyncComponent(() => import('./components/ZoneFormModal.vue'))
const LazyZoneDeleteModal = defineAsyncComponent(() => import('./components/ZoneDeleteModal.vue'))

const router = useRouter()
const overlay = useOverlay()

// Composables
const {
  zones,
  centers,
  loading,
  page,
  pageSize,
  total,
  selectedCenterId,
  loadZones,
  loadCenters,
  create,
  update,
  remove,
  bulkDelete,
  handlePageChange,
  handleSearch,
  filterByCenter,
} = useZones()

// Table columns
const columns = [
  { key: 'code', label: 'Code', sortable: true },
  { key: 'name', label: 'Name', sortable: true },
  { key: 'centerName', label: 'Center' },
  { key: 'hasPolygon', label: 'Has Polygon' },
  { key: 'actions', label: 'Actions' },
]

// Table rows
const tableRows = computed(() => zones.value)

// Center filter options
const centerFilterOptions = computed(() => [
  { label: 'All Centers', value: undefined },
  ...centers.value.map((c) => ({
    label: c.displayName,
    value: c.id,
  })),
])

/**
 * Open create modal
 */
const openCreateModal = async () => {
  const modal = overlay.create(LazyZoneFormModal)
  const instance = modal.open({ mode: 'create' })
  const formData = await instance.result

  if (formData) {
    await create(formData)
  }
}

/**
 * Open edit modal
 */
const openEditModal = async (zone: ZoneDto) => {
  const modal = overlay.create(LazyZoneFormModal)
  const instance = modal.open({ mode: 'edit', zone })
  const formData = await instance.result

  if (formData) {
    await update(zone.id, formData)
  }
}

/**
 * Open delete modal
 */
const openDeleteModal = async (zone: ZoneDto) => {
  const modal = overlay.create(LazyZoneDeleteModal)
  const instance = modal.open({ zoneName: zone.displayName })
  const confirmed = await instance.result

  if (confirmed) {
    await remove(zone.id)
  }
}

/**
 * Handle bulk delete
 */
const handleBulkDelete = async (ids: string[]) => {
  const modal = overlay.create(LazyZoneDeleteModal)
  const instance = modal.open({ count: ids.length })
  const confirmed = await instance.result

  if (confirmed) {
    await bulkDelete(ids)
  }
}

/**
 * Navigate to zone detail
 */
const viewZoneDetail = (zone: ZoneDto) => {
  router.push(`/zones/${zone.id}`)
}

// Load data on mount
onMounted(() => {
  loadCenters()
  loadZones()
})
</script>

<template>
  <div class="container mx-auto px-4 py-6">
    <PageHeader title="Zones" description="Manage delivery zones and distribution areas">
      <template #actions>
        <UButton icon="i-heroicons-plus" @click="openCreateModal"> Add Zone </UButton>
      </template>
    </PageHeader>

    <!-- Filter Bar -->
    <div class="mb-4">
      <UFormField label="Filter by Center">
        <USelect
          :model-value="selectedCenterId"
          :options="centerFilterOptions"
          @update:model-value="filterByCenter"
          class="w-64"
        />
      </UFormField>
    </div>

    <DataTable
      :columns="columns"
      :rows="tableRows"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      :total="total"
      searchable
      selectable
      search-placeholder="Search zones..."
      empty-message="No zones found"
      @update:page="handlePageChange"
      @search="handleSearch"
      @bulk-delete="handleBulkDelete"
    >
      <!-- Has Polygon Column -->
      <template #hasPolygon-data="{ row }">
        <UBadge :color="row.hasPolygon ? 'green' : 'gray'" variant="soft">
          {{ row.hasPolygon ? 'Yes' : 'No' }}
        </UBadge>
      </template>

      <!-- Actions Column -->
      <template #actions-data="{ row }">
        <div class="flex space-x-2">
          <UButton icon="i-heroicons-eye" size="sm" variant="ghost" @click="viewZoneDetail(row)" />
          <UButton icon="i-heroicons-pencil" size="sm" variant="ghost" @click="openEditModal(row)" />
          <UButton
            icon="i-heroicons-trash"
            size="sm"
            variant="ghost"
            color="error"
            @click="openDeleteModal(row)"
          />
        </div>
      </template>
    </DataTable>
  </div>
</template>
