<template>
  <div class="flex justify-center items-center min-h-screen p-8">
    <UCard class="w-full max-w-lg text-center">
      <template #header>
        <div>
          <UIcon
            name="i-heroicons-exclamation-triangle"
            class="w-20 h-20 mx-auto mb-4 text-warning-500"
          />
          <h1 class="text-5xl font-bold mb-2">404</h1>
        </div>
      </template>

      <div class="space-y-4">
        <h2 class="text-2xl font-semibold">Trang không tồn tại</h2>
        <p class="text-base">Trang bạn đang cố truy cập không tồn tại.</p>
        <div class="mt-6">
          <UButton color="primary" variant="solid" to="/" icon="i-heroicons-home" size="lg">
            Về trang chủ
          </UButton>
        </div>
      </div>
    </UCard>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'

onMounted(() => {
  document.title = 'Không tìm thấy trang'
})
</script>
