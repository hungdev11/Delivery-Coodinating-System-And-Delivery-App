<script setup lang="ts">
/**
 * Parcel Detail Modal for Admin
 * Allows admin to view parcel details and resolve disputes
 */

import { ref, computed, defineAsyncComponent } from 'vue'
import type { ParcelDto } from '../model.type'
import {
  resolveDisputeAsMisunderstanding,
  resolveDisputeAsFault,
} from '../api'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { useConversations } from '@/modules/Communication/composables'
import { useRouter } from 'vue-router'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'
import { useOverlay } from '@nuxt/ui/runtime/composables/useOverlay.js'

const LazyParcelProofModal = defineAsyncComponent(
  () => import('./ParcelProofModal.vue'),
)

interface Props {
  parcel: ParcelDto
}

const props = defineProps<Props>()
const emit = defineEmits<{
  close: []
  resolved: []
}>()

const toast = useToast()
const router = useRouter()
const currentUser = getCurrentUser()
const overlay = useOverlay()
const { findOrCreateConversation } = useConversations()

const resolvingAs = ref<'MISUNDERSTANDING' | 'FAULT' | null>(null)

const canViewProofs = computed(() => {
  return ['DELIVERED', 'SUCCEEDED', 'DISPUTE'].includes(props.parcel.status)
})

const openProofModal = async () => {
  const modal = overlay.create(LazyParcelProofModal)
  const instance = modal.open({ parcelId: props.parcel.id, parcelCode: props.parcel.code })
  await instance.result
}

const isDispute = computed(() => props.parcel.status === 'DISPUTE')

const handleResolveAsMisunderstanding = async () => {
  resolvingAs.value = 'MISUNDERSTANDING'
  try {
    await resolveDisputeAsMisunderstanding(props.parcel.id)
    toast.add({
      title: 'Tranh chấp đã giải quyết',
      description: `Đơn hàng ${props.parcel.code} - Lỗi từ khách hàng`,
      color: 'success',
    })
    emit('resolved')
    emit('close')
  } catch (error) {
    console.error('Failed to resolve dispute:', error)
    toast.add({
      title: 'Lỗi',
      description: 'Không thể giải quyết tranh chấp',
      color: 'error',
    })
  } finally {
    resolvingAs.value = null
  }
}

const handleResolveAsFault = async () => {
  resolvingAs.value = 'FAULT'
  try {
    await resolveDisputeAsFault(props.parcel.id)
    toast.add({
      title: 'Tranh chấp đã giải quyết',
      description: `Đơn hàng ${props.parcel.code} - Lỗi từ shipper (LOST)`,
      color: 'warning',
    })
    emit('resolved')
    emit('close')
  } catch (error) {
    console.error('Failed to resolve dispute:', error)
    toast.add({
      title: 'Lỗi',
      description: 'Không thể giải quyết tranh chấp',
      color: 'error',
    })
  } finally {
    resolvingAs.value = null
  }
}

const handleChatWithClient = async () => {
  if (!currentUser?.id || !props.parcel.receiverId) {
    toast.add({
      title: 'Lỗi',
      description: 'Không thể mở chat: thiếu thông tin người dùng',
      color: 'error',
    })
    return
  }

  try {
    // Find or create conversation between admin and client
    const conversation = await findOrCreateConversation(currentUser.id, props.parcel.receiverId)

    if (!conversation || !conversation.conversationId) {
      toast.add({
        title: 'Lỗi',
        description: 'Không thể tạo cuộc trò chuyện',
        color: 'error',
      })
      return
    }

    // Close modal and navigate to chat
    emit('close')
    router.push({
      name: 'communication-chat',
      params: { conversationId: conversation.conversationId },
      query: { partnerId: props.parcel.receiverId },
    })
  } catch (error) {
    console.error('Failed to open chat:', error)
    toast.add({
      title: 'Lỗi',
      description: 'Không thể mở chat',
      color: 'error',
    })
  }
}

const getStatusColor = (status: string) => {
  const colorMap: Record<string, string> = {
    IN_WAREHOUSE: 'neutral',
    ON_ROUTE: 'primary',
    DELIVERED: 'success',
    SUCCEEDED: 'success',
    FAILED: 'error',
    DELAYED: 'warning',
    DISPUTE: 'error',
    LOST: 'error',
  }
  return colorMap[status] || 'neutral'
}
</script>

<template>
  <div class="space-y-6">
    <!-- Parcel Info -->
    <div class="space-y-4">
      <div class="flex items-center justify-between">
        <h3 class="text-lg font-semibold">Chi tiết đơn hàng</h3>
        <UBadge :color="getStatusColor(parcel.status)" size="lg">
          {{ parcel.displayStatus || parcel.status }}
        </UBadge>
      </div>

      <div class="grid grid-cols-2 gap-4 text-sm">
        <div>
          <span class="text-gray-500">Mã đơn:</span>
          <p class="font-mono font-semibold">{{ parcel.code }}</p>
        </div>
        <div>
          <span class="text-gray-500">Loại:</span>
          <p class="font-medium">{{ parcel.deliveryType }}</p>
        </div>
        <div>
          <span class="text-gray-500">Người gửi:</span>
          <p class="font-medium">{{ parcel.senderName || 'N/A' }}</p>
        </div>
        <div>
          <span class="text-gray-500">Người nhận:</span>
          <p class="font-medium">{{ parcel.receiverName || 'N/A' }}</p>
        </div>
        <div class="col-span-2">
          <span class="text-gray-500">Địa chỉ giao:</span>
          <p class="font-medium">{{ parcel.targetDestination || 'N/A' }}</p>
        </div>
        <div>
          <span class="text-gray-500">Ngày tạo:</span>
          <p class="font-medium">{{ new Date(parcel.createdAt).toLocaleString('vi-VN') }}</p>
        </div>
        <div v-if="parcel.deliveredAt">
          <span class="text-gray-500">Ngày giao:</span>
          <p class="font-medium">{{ new Date(parcel.deliveredAt).toLocaleString('vi-VN') }}</p>
        </div>
      </div>
    </div>

    <!-- Dispute Resolution (only for DISPUTE status) -->
    <div v-if="isDispute" class="border-t pt-4 space-y-4">
      <h4 class="font-semibold text-error-600">Giải quyết tranh chấp</h4>
      <p class="text-sm text-gray-600">
        Khách hàng báo chưa nhận được đơn hàng này. Vui lòng xác minh và giải quyết.
      </p>

      <div class="flex flex-col gap-3">
        <!-- Misunderstanding button -->
        <UButton
          color="success"
          variant="soft"
          block
          :loading="resolvingAs === 'MISUNDERSTANDING'"
          :disabled="resolvingAs !== null"
          @click="handleResolveAsMisunderstanding"
        >
          <template #leading>
            <UIcon name="i-heroicons-check-circle" />
          </template>
          Lỗi từ khách hàng (Chuyển SUCCEEDED)
        </UButton>

        <!-- Fault button -->
        <UButton
          color="error"
          variant="soft"
          block
          :loading="resolvingAs === 'FAULT'"
          :disabled="resolvingAs !== null"
          @click="handleResolveAsFault"
        >
          <template #leading>
            <UIcon name="i-heroicons-x-circle" />
          </template>
          Lỗi từ shipper (Chuyển LOST)
        </UButton>

        <!-- Chat with client -->
        <UButton color="primary" variant="outline" block @click="handleChatWithClient">
          <template #leading>
            <UIcon name="i-heroicons-chat-bubble-left-right" />
          </template>
          Chat với khách hàng
        </UButton>
      </div>
    </div>

    <!-- Chat with client (for non-dispute parcels) -->
    <div v-else-if="parcel.receiverId" class="border-t pt-4 space-y-3">
      <UButton color="primary" variant="outline" block @click="handleChatWithClient">
        <template #leading>
          <UIcon name="i-heroicons-chat-bubble-left-right" />
        </template>
        Chat với khách hàng
      </UButton>
      
      <!-- View proofs button (for DELIVERED, SUCCEEDED, DISPUTE) -->
      <UButton
        v-if="canViewProofs"
        color="secondary"
        variant="outline"
        block
        @click="openProofModal"
      >
        <template #leading>
          <UIcon name="i-heroicons-photo" />
        </template>
        Ảnh/video đơn hàng
      </UButton>
    </div>
    
    <!-- View proofs button (for dispute parcels) -->
    <div v-if="isDispute && canViewProofs" class="border-t pt-4">
      <UButton
        color="secondary"
        variant="outline"
        block
        @click="openProofModal"
      >
        <template #leading>
          <UIcon name="i-heroicons-photo" />
        </template>
        Ảnh/video đơn hàng
      </UButton>
    </div>

    <!-- Actions -->
    <div class="flex justify-end gap-2 border-t pt-4">
      <UButton color="neutral" variant="ghost" @click="emit('close')"> Đóng </UButton>
    </div>
  </div>
</template>
