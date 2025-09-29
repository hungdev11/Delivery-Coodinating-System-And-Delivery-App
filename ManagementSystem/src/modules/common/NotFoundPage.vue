<template>
  <div class="flex justify-center items-center h-screen">
    <div class="text-center">
      <h1 class="text-4xl font-bold text-gray-800 mb-4">Trang không tồn tại</h1>
      <p class="text-lg text-gray-600">Trang bạn đang cố truy cập không tồn tại.</p>
      <router-link
        to="/"
        class="mt-4 inline-block bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
      >
        Về trang chủ
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'

onMounted(() => {
  document.title = 'Không tìm thấy trang'
})
</script>
