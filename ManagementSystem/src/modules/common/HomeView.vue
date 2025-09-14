<script setup lang="ts">
import { onMounted } from 'vue'

onMounted(() => {
  document.title = 'Hệ thống quản lý đơn hàng'
})
</script>
<template>
  <div class="space-y-6">
    <h2 class="text-2xl font-bold text-gray-900">Welcome to Dashboard</h2>
    <p class="text-gray-600">
      This is a placeholder content area. You can add your page content here.
    </p>

    <!-- Placeholder cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div class="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <h3 class="text-lg font-semibold text-gray-900 mb-2">Quick Stats</h3>
        <p class="text-gray-600">Placeholder for statistics</p>
      </div>

      <div class="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <h3 class="text-lg font-semibold text-gray-900 mb-2">Recent Activity</h3>
        <p class="text-gray-600">Placeholder for recent activities</p>
      </div>

      <div class="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <h3 class="text-lg font-semibold text-gray-900 mb-2">Notifications</h3>
        <p class="text-gray-600">Placeholder for notifications</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-theme-1 {
  padding: 20px;
  background-color: #f0f8ff;
  border: 1px solid #d4e8ff;
  border-radius: 8px;
}
</style>
