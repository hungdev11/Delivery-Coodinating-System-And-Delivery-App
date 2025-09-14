<template>
  <div class="unauthorized-view">
    <div class="error-container">
      <h1>401 - Unauthorized</h1>
      <p>You don't have permission to access this page.</p>
      <div class="user-info" v-if="currentUser">
        <h3>Your Current Permissions:</h3>
        <p><strong>Name:</strong> {{ currentUser.name }}</p>
        <p><strong>Roles:</strong> {{ currentUser.role?.join(', ') }}</p>
      </div>
      <div class="actions">
        <router-link to="/" class="btn btn-primary">Go Home</router-link>
        <router-link to="/login" class="btn btn-secondary">Login</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getCurrentUser } from '@/common/guards/roleGuard.guard'

const currentUser = computed(() => getCurrentUser())
</script>

<style scoped>
.unauthorized-view {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
  padding: 2rem;
}

.error-container {
  text-align: center;
  max-width: 500px;
}

h1 {
  color: #dc3545;
  margin-bottom: 1rem;
  font-size: 2.5rem;
}

p {
  color: #666;
  margin-bottom: 2rem;
  font-size: 1.1rem;
}

.user-info {
  margin: 2rem 0;
  padding: 1rem;
  background-color: #f8f9fa;
  border-radius: 8px;
  text-align: left;
}

.user-info h3 {
  color: #495057;
  margin-bottom: 0.5rem;
}

.actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-top: 2rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  text-decoration: none;
  border-radius: 4px;
  font-weight: bold;
  transition: background-color 0.2s;
}

.btn-primary {
  background-color: #007bff;
  color: white;
}

.btn-primary:hover {
  background-color: #0056b3;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background-color: #545b62;
}
</style>
