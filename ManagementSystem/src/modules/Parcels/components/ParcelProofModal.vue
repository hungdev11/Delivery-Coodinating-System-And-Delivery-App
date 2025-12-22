<script setup lang="ts">
/**
 * Parcel Proof Modal
 * Displays delivery proofs (images/videos) for a parcel
 */

import { ref, onMounted } from 'vue'
import { getProofsByParcel } from '@/modules/Delivery/api'
import type { DeliveryProofDto } from '@/modules/Delivery/api'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'

interface Props {
  parcelId: string
  parcelCode?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  close: []
}>()

const toast = useToast()
const proofs = ref<DeliveryProofDto[]>([])
const loading = ref(false)
const selectedProofIndex = ref<number | null>(null)
const isViewerOpen = ref(false)

const loadProofs = async () => {
  loading.value = true
  try {
    const response = await getProofsByParcel(props.parcelId)
    if (response.result) {
      proofs.value = response.result
    }
  } catch (error) {
    console.error('Failed to load proofs:', error)
    toast.add({
      title: 'Lỗi',
      description: 'Không thể tải bằng chứng giao hàng',
      color: 'error',
    })
  } finally {
    loading.value = false
  }
}

const isVideo = (url: string) => {
  if (!url) return false
  const lower = url.toLowerCase()
  return lower.endsWith('.mp4') || lower.endsWith('.mov') || lower.endsWith('.avi') || lower.includes('/video/')
}

/**
 * Get thumbnail URL for video from Cloudinary
 * Cloudinary video URLs can be converted to thumbnail by:
 * - Replacing /video/upload/ with /video/upload/so_0,w_300,h_300,c_fill/
 * - Changing extension from .mp4/.mov to .jpg
 */
const getThumbnailUrl = (url: string) => {
  if (!isVideo(url) || !url) {
    return url // Return original URL for images
  }

  // Cloudinary video URL format: https://res.cloudinary.com/.../video/upload/v1234567890/filename.mp4
  // Thumbnail format: https://res.cloudinary.com/.../video/upload/so_0,w_300,h_300,c_fill/v1234567890/filename.jpg

  try {
    // Check if it's a Cloudinary URL
    if (url.includes('cloudinary.com') && url.includes('/video/upload/')) {
      // Extract the path after /video/upload/
      const uploadIndex = url.indexOf('/video/upload/')
      if (uploadIndex !== -1) {
        const baseUrl = url.substring(0, uploadIndex + '/video/upload/'.length)
        const restOfUrl = url.substring(uploadIndex + '/video/upload/'.length)

        // Add thumbnail transformation: so_0 (start at 0s), w_300, h_300, c_fill
        let thumbnailUrl = baseUrl + 'so_0,w_300,h_300,c_fill/' + restOfUrl

        // Change extension to .jpg
        thumbnailUrl = thumbnailUrl.replace(/\.(mp4|mov|avi)$/i, '.jpg')

        return thumbnailUrl
      }
    }
  } catch (error) {
    // If transformation fails, return original URL
    console.debug('Failed to generate thumbnail URL:', error)
  }

  return url
}

const getProofTypeLabel = (type: string) => {
  return type === 'DELIVERED' ? 'Đã giao' : 'Trả kho'
}

const openProof = (index: number) => {
  selectedProofIndex.value = index
  isViewerOpen.value = true
}

const closeViewer = () => {
  selectedProofIndex.value = null
  isViewerOpen.value = false
}

const nextProof = () => {
  if (selectedProofIndex.value !== null && selectedProofIndex.value < proofs.value.length - 1) {
    selectedProofIndex.value++
  }
}

const prevProof = () => {
  if (selectedProofIndex.value !== null && selectedProofIndex.value > 0) {
    selectedProofIndex.value--
  }
}

onMounted(() => {
  loadProofs()
})
</script>

<template>
  <UModal
    title="Ảnh/video đơn hàng"
    :description="parcelCode ? `Mã đơn: ${parcelCode}` : undefined"
    :close="{ onClick: () => emit('close') }"
    :ui="{ content: 'sm:max-w-2xl md:max-w-4xl', footer: 'justify-end' }"
  >
    <template #body>
      <div class="space-y-4">

    <!-- Loading state -->
    <div v-if="loading" class="flex justify-center py-8">
      <UIcon name="i-heroicons-arrow-path" class="w-8 h-8 animate-spin text-gray-400" />
    </div>

    <!-- Empty state -->
    <div v-else-if="proofs.length === 0" class="text-center py-8 text-gray-500">
      <UIcon name="i-heroicons-photo" class="w-12 h-12 mx-auto mb-2 text-gray-300" />
      <p>Chưa có bằng chứng giao hàng</p>
    </div>

    <!-- Proofs grid -->
    <div v-else class="grid grid-cols-3 gap-4">
      <div
        v-for="(proof, index) in proofs"
        :key="proof.id"
        class="relative aspect-square rounded-lg overflow-hidden cursor-pointer hover:opacity-80 transition-opacity"
        @click="openProof(index)"
      >
        <img
          :src="getThumbnailUrl(proof.mediaUrl)"
          :alt="`Proof ${index + 1}`"
          class="w-full h-full object-cover"
        />
        <div
          v-if="isVideo(proof.mediaUrl)"
          class="absolute inset-0 flex items-center justify-center bg-black bg-opacity-30"
        >
          <UIcon name="i-heroicons-play-circle" class="w-16 h-16 text-white" />
        </div>

        <!-- Type badge -->
        <div class="absolute bottom-2 right-2">
          <UBadge
            :color="proof.type === 'DELIVERED' ? 'success' : 'warning'"
            size="xs"
          >
            {{ getProofTypeLabel(proof.type) }}
          </UBadge>
        </div>
      </div>
    </div>

    <!-- Proof info -->
    <div v-if="proofs.length > 0" class="text-sm text-gray-500 border-t pt-4">
      <p>Tổng cộng: {{ proofs.length }} bằng chứng</p>
    </div>

    <!-- Fullscreen viewer - Use Teleport to avoid nesting modals -->
    <Teleport to="body">
      <UModal
        v-model="isViewerOpen"
        :ui="{ content: 'w-full max-w-4xl' }"
        @close="closeViewer"
      >
      <div v-if="selectedProofIndex !== null && proofs[selectedProofIndex]" class="relative">
        <div class="flex items-center justify-between mb-4">
          <div>
            <p class="font-semibold">
              Bằng chứng {{ selectedProofIndex + 1 }} / {{ proofs.length }}
            </p>
            <p class="text-sm text-gray-500">
              {{ getProofTypeLabel(proofs[selectedProofIndex].type) }} -
              {{ new Date(proofs[selectedProofIndex].createdAt).toLocaleString('vi-VN') }}
            </p>
          </div>
          <UButton
            color="neutral"
            variant="ghost"
            icon="i-heroicons-x-mark"
            @click="closeViewer"
          />
        </div>

        <div class="relative bg-black rounded-lg overflow-hidden">
          <img
            v-if="!isVideo(proofs[selectedProofIndex].mediaUrl)"
            :src="proofs[selectedProofIndex].mediaUrl"
            alt="Proof"
            class="w-full max-h-[70vh] object-contain mx-auto"
          />
          <video
            v-else
            :src="proofs[selectedProofIndex].mediaUrl"
            :poster="getThumbnailUrl(proofs[selectedProofIndex].mediaUrl)"
            controls
            preload="metadata"
            class="w-full max-h-[70vh]"
          >
            Your browser does not support the video tag.
          </video>

          <!-- Navigation buttons -->
          <div class="absolute inset-y-0 left-0 flex items-center">
            <UButton
              v-if="selectedProofIndex > 0"
              color="neutral"
              variant="solid"
              icon="i-heroicons-chevron-left"
              size="lg"
              class="ml-2"
              @click="prevProof"
            />
          </div>
          <div class="absolute inset-y-0 right-0 flex items-center">
            <UButton
              v-if="selectedProofIndex < proofs.length - 1"
              color="neutral"
              variant="solid"
              icon="i-heroicons-chevron-right"
              size="lg"
              class="mr-2"
              @click="nextProof"
            />
          </div>
        </div>
      </div>
      </UModal>
    </Teleport>

      </div>
    </template>

    <template #footer>
      <UButton color="neutral" variant="ghost" @click="emit('close')"> Đóng </UButton>
    </template>
  </UModal>
</template>
