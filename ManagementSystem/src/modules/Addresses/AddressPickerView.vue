<script setup lang="ts">
/**
 * Address Picker View
 * - Clone-like of DemoRoutingView for selecting/creating addresses
 * - Features:
 *   1) Pick point on map, input name, create address
 *   2) Search address text (local + TrackAsia through API Gateway), jump to point, fine-tune by dragging map
 */

import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { PageHeader } from '@/common/components'
import MapView from '@/common/components/MapView.vue'
import { useAddresses } from './composables'
import type { ByPointResult, AddressDto } from './api'
import { storeToRefs } from 'pinia'
import { useParcels } from '@/modules/Parcels/composables'
import UserSelect from '@/common/components/UserSelect.vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { getOrCreateAddress, getAddressById } from './api'
import {
  getUserPrimaryAddress,
  getUserAddresses,
  createUserAddress,
  type CreateUserAddressRequest,
} from '@/modules/Users/api'

const router = useRouter()
const toast = useToast()

// Parcel creation state
const quickCreateParcelModalOpen = ref(false)
const quickParcelForm = ref({
  code: '',
  senderId: '',
  receiverId: '',
  receiveFrom: '',
  sendTo: '',
  weight: 0,
  value: 0,
  deliveryType: 'NORMAL' as const,
})

// Sender primary address state
const senderPrimaryAddress = ref<AddressDto | null>(null)
const loadingSenderAddress = ref(false)

// Map state
const mapViewRef = ref<InstanceType<typeof MapView>>()
const mapLoaded = ref(false)
const mapCenter = ref<[number, number]>([106.660172, 10.762622]) // [lng, lat]
const cursorLngLat = ref<[number, number] | null>(null)
let mapOnMove: (() => void) | null = null
let mapOnMouseMove: ((e: { lngLat?: { lng: number; lat: number } }) => void) | null = null

// UI state via composable
const { search, create } = useAddresses()

const addrStore = useAddresses()
const { success, error, searchTerm, searching, creating, searchResult } = storeToRefs(addrStore)

const createDialogOpen = ref(false)
const newAddressName = ref('')
const newAddressNote = ref('')
const lookupId = ref('')
const nearbyLoading = ref(false)
const nearbyResults = ref<Array<{
  source: 'local' | 'track-asia'
  id?: string
  name: string
  lat: number
  lon: number
  addressText?: string
}> | null>(null)

const mapConfig = {
  center: [mapCenter.value[0], mapCenter.value[1]] as [number, number],
  zoom: 15,
  style: `https://api.maptiler.com/maps/streets/style.json?key=${import.meta.env.VITE_MAPTILER_API_KEY || 'get_your_own_OpIi9ZULNHzrESv6T2vL'}`,
}

// Fixed center marker (visual only). We don't render via MapView markers to keep it locked to center
const mapMarkers = computed(() => {
  return []
})

const mapRoutes = computed(() => [])

const currentLat = computed(() => mapCenter.value[1])
const currentLon = computed(() => mapCenter.value[0])

const centerLabel = computed(() => `${currentLat.value.toFixed(6)}, ${currentLon.value.toFixed(6)}`)

const handleMapLoaded = () => {
  mapLoaded.value = true
  const map = mapViewRef.value?.map
  if (!map) return
  // Realtime center from native move event
  const onMove = () => {
    const c = map.getCenter()
    mapCenter.value = [c.lng, c.lat]
  }
  const onMouseMove = (e: { lngLat?: { lng: number; lat: number } }) => {
    if (e && e.lngLat) {
      cursorLngLat.value = [e.lngLat.lng, e.lngLat.lat]
    }
  }
  map.on('move', onMove)
  map.on('mousemove', onMouseMove)
  // store handlers for cleanup
  mapOnMove = onMove
  mapOnMouseMove = onMouseMove
}

// Keep mapCenter updated when user drags map
const handleMapMove = (data: { center: [number, number] }) => {
  mapCenter.value = data.center
}

// Click-to-center helper
const handleMapClick = (data: { lngLat: [number, number] }) => {
  const map = mapViewRef.value?.map
  if (map) {
    map.easeTo({ center: data.lngLat, duration: 800 })
  }
  // Sync reactive center immediately to avoid stale center when map-move event is not emitted
  mapCenter.value = [data.lngLat[0], data.lngLat[1]]
  // Load nearby suggestions at clicked point
  void loadNearby(data.lngLat[1], data.lngLat[0])
}

// Search via composable
const fetchSearch = async () => {
  await search(10)
}

const jumpTo = (lat: number, lon: number, name?: string) => {
  const map = mapViewRef.value?.map
  if (map) {
    map.easeTo({ center: [lon, lat], zoom: Math.max(map.getZoom(), 17), duration: 800 })
  }
  // Sync reactive center with programmatic move
  mapCenter.value = [lon, lat]
  if (name && !newAddressName.value) newAddressName.value = name
}

// Create address via composable
const createAddress = async () => {
  if (!newAddressName.value.trim()) {
    error.value = 'Please input address name'
    return
  }
  try {
    await create({
      name: newAddressName.value.trim(),
      addressText: newAddressNote.value.trim() || undefined,
      lat: currentLat.value,
      lon: currentLon.value,
    })
    createDialogOpen.value = false
  } catch {}
}
const fetchById = async () => {
  if (!lookupId.value.trim()) return
  try {
    const a = await addrStore.getAddressById(lookupId.value.trim())
    jumpTo(a.lat, a.lon, a.name)
    newAddressName.value = a.name
    newAddressNote.value = a.addressText || ''
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load address'
  }
}

const deleteSelected = async () => {
  const sel = addrStore.selected
  if (!sel || sel.source !== 'local' || !sel.id) return
  try {
    await addrStore.remove(sel.id)
    nearbyResults.value = (nearbyResults.value || []).filter((x) => x.id !== sel.id)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to delete address'
  }
}

// Update existing local address
const updateAddress = async () => {
  const sel = addrStore.selected
  if (!sel || sel.source !== 'local' || !sel.id) return
  try {
    await addrStore.update(sel.id, {
      name: newAddressName.value.trim() || undefined,
      addressText: newAddressNote.value.trim() || null,
      lat: currentLat.value,
      lon: currentLon.value,
    })
  } catch {}
}

// Initialize
onMounted(() => {
  // no-op
})

onUnmounted(() => {
  const map = mapViewRef.value?.map
  if (map && mapOnMove) map.off('move', mapOnMove)
  if (map && mapOnMouseMove) map.off('mousemove', mapOnMouseMove)
})

// Load nearby using gateway by-point endpoint
const loadNearby = async (lat: number, lon: number) => {
  nearbyLoading.value = true
  try {
    // Prefer small radius per requirement (5-15m)
    const res = await addrStore.findByPoint({ lat, lon, radius: 100, limit: 10 })
    const result = res && 'result' in res ? (res as { result?: ByPointResult }).result : undefined
    const items: typeof nearbyResults.value = []
    if (result && result.local) {
      for (const a of result.local) {
        items!.push({
          source: 'local',
          id: a.id,
          name: a.name,
          lat: a.lat,
          lon: a.lon,
          addressText: a.addressText || undefined,
        })
      }
    }
    if (result && result.external) {
      for (const e of result.external) {
        items!.push({
          source: 'track-asia',
          name: e.name,
          lat: e.lat,
          lon: e.lon,
          addressText: e.formattedAddress,
        })
      }
    }
    // Sort: local first for prioritization
    nearbyResults.value = items!.sort(
      (a, b) => (a.source === 'local' ? -1 : 1) - (b.source === 'local' ? -1 : 1),
    )
  } catch {
    nearbyResults.value = []
  } finally {
    nearbyLoading.value = false
  }
}

const selectNearby = (item: {
  id?: string
  source: 'local' | 'track-asia'
  name: string
  lat: number
  lon: number
  addressText?: string
}) => {
  jumpTo(item.lat, item.lon, item.name)
  newAddressName.value = item.name
  if (item.addressText) newAddressNote.value = item.addressText
  addrStore.select(item)
}

// Load sender primary address when senderId changes
watch(
  () => quickParcelForm.value.senderId,
  async (senderId) => {
    if (!senderId) {
      senderPrimaryAddress.value = null
      return
    }

    loadingSenderAddress.value = true
    try {
      const response = await getUserPrimaryAddress(senderId)
      if (response.result?.destinationId) {
        // Get address detail from zone-service
        const addressResponse = await getAddressById(response.result.destinationId)
        if (addressResponse.result) {
          senderPrimaryAddress.value = addressResponse.result
          // Update map center to sender's primary address
          jumpTo(
            addressResponse.result.lat,
            addressResponse.result.lon,
            addressResponse.result.name,
          )
          // Pre-fill receiveFrom with sender's primary address
          quickParcelForm.value.receiveFrom = addressResponse.result.name || ''
        }
      }
    } catch (error) {
      console.error('Failed to load sender primary address:', error)
      // Don't show error toast - user might not have a primary address yet
      senderPrimaryAddress.value = null
    } finally {
      loadingSenderAddress.value = false
    }
  },
)

// Quick create parcel from selected address
const openQuickCreateParcel = () => {
  // Pre-fill form with selected address info
  if (addrStore.selected) {
    quickParcelForm.value.sendTo = addrStore.selected.name || ''
  } else if (newAddressName.value) {
    quickParcelForm.value.sendTo = newAddressName.value
  }

  // If sender has primary address, use it for receiveFrom
  if (senderPrimaryAddress.value) {
    quickParcelForm.value.receiveFrom = senderPrimaryAddress.value.name || ''
  } else if (addrStore.selected) {
    quickParcelForm.value.receiveFrom = addrStore.selected.name || ''
  } else if (newAddressName.value) {
    quickParcelForm.value.receiveFrom = newAddressName.value
  }

  quickCreateParcelModalOpen.value = true
}

/**
 * Add address to client user (if not exists)
 */
const addAddressToClient = async (
  userId: string,
  destinationId: string,
  addressName: string,
  addressText?: string,
) => {
  try {
    // Check if client already has this address
    const addressesResponse = await getUserAddresses(userId)
    if (addressesResponse.result) {
      const existingAddress = addressesResponse.result.find(
        (addr) => addr.destinationId === destinationId,
      )
      if (existingAddress) {
        // Address already exists for client
        return existingAddress.destinationId
      }
    }

    // Create user address for client (not primary, just added)
    const createUserAddressRequest: CreateUserAddressRequest = {
      destinationId,
      note: addressText,
      tag: 'Added from Parcel',
      isPrimary: false,
    }

    const createUserAddressResponse = await createUserAddress(userId, createUserAddressRequest)

    if (createUserAddressResponse.result) {
      toast.add({
        title: 'Success',
        description: `Address added to client: ${addressName}`,
        color: 'success',
      })
      return createUserAddressResponse.result.destinationId
    }

    return destinationId
  } catch (error) {
    console.error('Failed to add address to client:', error)
    // Don't fail parcel creation if adding address fails
    return destinationId
  }
}

const handleQuickCreateParcel = async () => {
  if (
    !quickParcelForm.value.code ||
    !quickParcelForm.value.senderId ||
    !quickParcelForm.value.receiverId
  ) {
    toast.add({
      title: 'Error',
      description: 'Please fill in code, sender, and receiver',
      color: 'error',
    })
    return
  }

  try {
    // Get or create sender destination
    // Use sender's primary address if available, otherwise use current map center
    let senderDestinationId: string | null = null

    if (senderPrimaryAddress.value) {
      // Use sender's primary address
      senderDestinationId = senderPrimaryAddress.value.id
      quickParcelForm.value.receiveFrom =
        senderPrimaryAddress.value.name || quickParcelForm.value.receiveFrom
    } else {
      // Get or create sender destination from current map center
      const senderResponse = await getOrCreateAddress({
        name: quickParcelForm.value.receiveFrom || 'Sender Address',
        addressText: quickParcelForm.value.receiveFrom,
        lat: currentLat.value,
        lon: currentLon.value,
      })

      if (!senderResponse.result?.id) {
        toast.add({
          title: 'Error',
          description: 'Failed to get or create sender destination',
          color: 'error',
        })
        return
      }

      senderDestinationId = senderResponse.result.id
    }

    // Get or create receiver destination from selected address on map
    // Use current map center (where user selected the address)
    const receiverResponse = await getOrCreateAddress({
      name: quickParcelForm.value.sendTo || 'Receiver Address',
      addressText: quickParcelForm.value.sendTo,
      lat: currentLat.value,
      lon: currentLon.value,
    })

    if (!receiverResponse.result?.id) {
      toast.add({
        title: 'Error',
        description: 'Failed to get or create receiver destination',
        color: 'error',
      })
      return
    }

    // Add address to client (if receiver is a client user)
    // This ensures client has the address in their address list
    try {
      await addAddressToClient(
        quickParcelForm.value.receiverId,
        receiverResponse.result.id,
        receiverResponse.result.name,
        receiverResponse.result.addressText || undefined,
      )
    } catch (error) {
      console.error('Failed to add address to client:', error)
      // Continue anyway - address addition is optional
    }

    // Create parcel
    const { create } = useParcels()
    const success = await create({
      code: quickParcelForm.value.code,
      senderId: quickParcelForm.value.senderId,
      receiverId: quickParcelForm.value.receiverId,
      deliveryType: quickParcelForm.value.deliveryType,
      receiveFrom: quickParcelForm.value.receiveFrom,
      sendTo: quickParcelForm.value.sendTo,
      weight: quickParcelForm.value.weight,
      value: quickParcelForm.value.value,
      senderDestinationId: senderDestinationId,
      receiverDestinationId: receiverResponse.result.id,
    })

    if (success) {
      quickCreateParcelModalOpen.value = false
      // Reset form
      quickParcelForm.value = {
        code: '',
        senderId: '',
        receiverId: '',
        receiveFrom: '',
        sendTo: '',
        weight: 0,
        value: 0,
        deliveryType: 'NORMAL',
      }
      senderPrimaryAddress.value = null
    }
  } catch (error) {
    console.error('Failed to create parcel:', error)
    toast.add({
      title: 'Error',
      description: error instanceof Error ? error.message : 'Failed to create parcel',
      color: 'error',
    })
  }
}
</script>

<template>
  <div class="address-picker-view">
    <PageHeader
      title="Address Picker"
      description="Search, fine-tune on map center, and create address"
    >
      <template #actions>
        <UButton
          color="neutral"
          variant="outline"
          icon="i-heroicons-home"
          @click="router.push('/')"
        >
          Home
        </UButton>
      </template>
    </PageHeader>

    <div class="grid grid-cols-1 lg:grid-cols-4 gap-4 h-[700px]">
      <!-- Left Panel -->
      <div class="lg:col-span-1 space-y-4 overflow-y-auto">
        <!-- Search Box -->
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">Search Address</h3>
          </template>
          <div class="space-y-3">
            <UInput
              v-model="searchTerm"
              placeholder="Type address e.g. 'Landmark 81'"
              @keydown.enter.prevent="fetchSearch"
            />
            <div class="flex items-center gap-2">
              <UButton color="primary" :loading="searching" @click="fetchSearch">Search</UButton>
              <span class="text-xs text-gray-500" v-if="searchResult"
                >{{
                  (searchResult?.local?.length || 0) + (searchResult?.external?.length || 0)
                }}
                results</span
              >
            </div>
            <div class="space-y-2 max-h-64 overflow-y-auto">
              <div v-if="searchResult?.local?.length">
                <p class="text-xs font-medium text-gray-500 mb-1">Local</p>
                <div
                  v-for="(a, i) in searchResult.local"
                  :key="`loc-${i}`"
                  class="p-2 rounded border cursor-pointer hover:bg-gray-50"
                  @click="jumpTo(a.lat, a.lon, a.name)"
                >
                  <p class="text-sm font-medium">{{ a.name }}</p>
                  <p class="text-xs text-gray-500">{{ a.addressText || '' }}</p>
                </div>
              </div>
              <div v-if="searchResult?.external?.length">
                <p class="text-xs font-medium text-gray-500 mt-2 mb-1">Suggestions</p>
                <div
                  v-for="(e, i) in searchResult.external"
                  :key="`ext-${i}`"
                  class="p-2 rounded border cursor-pointer hover:bg-gray-50"
                  @click="jumpTo(e.lat, e.lon, e.name)"
                >
                  <p class="text-sm font-medium">{{ e.name }}</p>
                  <p class="text-xs text-gray-500">{{ e.formattedAddress || '' }}</p>
                </div>
              </div>
            </div>
          </div>
        </UCard>

        <!-- Nearby on map click -->
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">Nearby</h3>
          </template>
          <div class="space-y-2 max-h-64 overflow-y-auto">
            <div v-if="nearbyLoading" class="text-xs text-gray-500">Loading nearby points...</div>
            <div
              v-else-if="nearbyResults && nearbyResults.length === 0"
              class="text-xs text-gray-500"
            >
              No nearby points
            </div>
            <div v-else>
              <div
                v-for="(n, i) in nearbyResults || []"
                :key="`nb-${i}`"
                class="p-2 rounded border cursor-pointer"
                :class="
                  n.source === 'local'
                    ? 'bg-green-50 hover:bg-green-100 border-green-200'
                    : 'hover:bg-gray-50'
                "
                @click="selectNearby(n)"
              >
                <div class="flex items-center justify-between">
                  <p class="text-sm font-medium">{{ n.name }}</p>
                  <UBadge :color="n.source === 'local' ? 'primary' : 'neutral'" size="xs">{{
                    n.source
                  }}</UBadge>
                </div>
                <p v-if="n.addressText" class="text-xs text-gray-500">{{ n.addressText }}</p>
                <p class="text-[10px] text-gray-400">
                  {{ n.lat.toFixed(6) }}, {{ n.lon.toFixed(6) }}
                </p>
              </div>
            </div>
          </div>
        </UCard>

        <!-- Create / Update / Delete Address -->
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">Create Address</h3>
          </template>

          <div class="space-y-3">
            <div class="text-xs text-gray-500">
              Point is fixed at the map center. Drag the map to adjust.
            </div>
            <div class="text-sm"><strong>Center:</strong> {{ centerLabel }}</div>
            <UFormField label="Name">
              <UInput v-model="newAddressName" placeholder="e.g. Customer A - Gate" />
            </UFormField>
            <UFormField label="Note (optional)">
              <UInput v-model="newAddressNote" placeholder="Apartment, floor, details..." />
            </UFormField>
            <div class="flex items-center gap-2">
              <UButton
                v-if="!addrStore.selected || addrStore.selected.source !== 'local'"
                color="primary"
                :loading="creating"
                @click="createAddress"
              >
                Save Address
              </UButton>
              <UButton v-else color="warning" :loading="creating" @click="updateAddress">
                Update Address
              </UButton>
              <UButton
                v-if="
                  addrStore.selected &&
                  addrStore.selected.source === 'local' &&
                  addrStore.selected.id
                "
                color="error"
                variant="soft"
                :loading="creating"
                @click="deleteSelected"
              >
                Delete
              </UButton>
              <UButton
                v-if="addrStore.selected !== null"
                color="neutral"
                variant="ghost"
                @click="addrStore.clearSelection()"
              >
                Clear
              </UButton>
            </div>
            <!-- Quick Create Parcel Button -->
            <UButton
              color="primary"
              variant="soft"
              icon="i-heroicons-cube"
              @click="openQuickCreateParcel"
              class="w-full mt-2"
            >
              Quick Create Parcel
            </UButton>
          </div>
        </UCard>

        <!-- Demo: Get Address by ID -->
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">Get Address by ID</h3>
          </template>
          <div class="space-y-3">
            <UFormField label="Address ID">
              <UInput v-model="lookupId" placeholder="uuid" @keydown.enter.prevent="fetchById" />
            </UFormField>
            <div class="flex items-center gap-2">
              <UButton color="primary" :loading="creating" @click="fetchById">Load</UButton>
              <span class="text-xs text-gray-500">Prefills and centers map when found</span>
            </div>
          </div>
        </UCard>

        <UAlert v-if="error" color="error" variant="soft" title="Error" :description="error" />
        <UAlert
          v-if="success"
          color="success"
          variant="soft"
          title="Success"
          :description="success"
        />
      </div>

      <!-- Map Panel -->
      <div class="lg:col-span-3 relative">
        <div class="map-container" style="height: 700px">
          <MapView
            ref="mapViewRef"
            :config="mapConfig"
            :markers="mapMarkers"
            :routes="mapRoutes"
            :show-zones="false"
            :show-routing="false"
            height="700px"
            @map-loaded="handleMapLoaded"
            @map-click="handleMapClick"
            @map-move="handleMapMove"
          />
          <!-- fixed center marker overlay -->
          <div class="center-pin">
            <div class="pin" />
          </div>
          <div class="center-coords">
            {{ centerLabel }}
            <span v-if="cursorLngLat" class="ml-2 text-[10px] opacity-80">
              (cursor: {{ cursorLngLat[1].toFixed(6) }}, {{ cursorLngLat[0].toFixed(6) }})
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Create Parcel Modal -->
    <UModal
      v-model:open="quickCreateParcelModalOpen"
      title="Quick Create Parcel"
      description="Create a parcel quickly using the selected address"
      :ui="{ width: 'sm:max-w-md md:max-w-lg', footer: 'justify-end' }"
    >
      <template #body>
        <form @submit.prevent="handleQuickCreateParcel" class="space-y-4">
          <UFormField label="Parcel Code" required>
            <UInput v-model="quickParcelForm.code" placeholder="Enter parcel code" />
          </UFormField>

          <div class="grid grid-cols-2 gap-4">
            <UserSelect
              v-model="quickParcelForm.senderId"
              label="Sender"
              placeholder="Search sender..."
              :allow-seed-id="true"
            />
            <UserSelect
              v-model="quickParcelForm.receiverId"
              label="Receiver"
              placeholder="Search receiver..."
              :allow-seed-id="true"
            />
          </div>

          <UFormField label="Receive From">
            <UInput v-model="quickParcelForm.receiveFrom" placeholder="Sender address" />
          </UFormField>

          <UFormField label="Send To">
            <UInput v-model="quickParcelForm.sendTo" placeholder="Receiver address" />
          </UFormField>

          <div class="grid grid-cols-2 gap-4">
            <UFormField label="Weight (kg)">
              <UInput v-model.number="quickParcelForm.weight" type="number" placeholder="0" />
            </UFormField>
            <UFormField label="Value">
              <UInput v-model.number="quickParcelForm.value" type="number" placeholder="0" />
            </UFormField>
          </div>

          <div class="text-xs text-gray-500">📍 Address coordinates: {{ centerLabel }}</div>
        </form>
      </template>
      <template #footer>
        <UButton color="neutral" variant="ghost" @click="quickCreateParcelModalOpen = false">
          Cancel
        </UButton>
        <UButton color="primary" @click="handleQuickCreateParcel"> Create Parcel </UButton>
      </template>
    </UModal>
  </div>
</template>

<style scoped>
.address-picker-view {
  padding: 1rem;
}

.map-container {
  border-radius: 0.5rem;
  overflow: hidden;
  box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1);
  position: relative;
}

.center-pin {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -100%);
  pointer-events: none;
}

.center-pin .pin {
  width: 18px;
  height: 18px;
  background: #ef4444;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  position: relative;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.9);
}

.center-pin .pin:after {
  content: '';
  width: 8px;
  height: 8px;
  position: absolute;
  border-radius: 50%;
  left: 5px;
  top: 5px;
  background: #fff;
}

.center-coords {
  position: absolute;
  left: 50%;
  bottom: 10px;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
}
</style>
