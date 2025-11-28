<template>
  <div class="flex justify-center items-center min-h-[60vh] p-8">
    <UCard class="w-full max-w-lg">
      <template #header>
        <div class="text-center">
          <UIcon name="i-heroicons-shield-exclamation" class="w-16 h-16 mx-auto mb-4 text-error-500" />
          <h1 class="text-4xl font-bold text-error-500 dark:text-error-400 mb-2">
            401 - Unauthorized
          </h1>
        </div>
      </template>

      <div class="space-y-4 text-center">
        <p class="text-lg">
          You don't have permission to access this page.
        </p>

        <UCard v-if="currentUser" variant="soft" class="text-left">
          <template #header>
            <h3 class="font-semibold">Your Current Permissions:</h3>
          </template>
          <div class="space-y-2 text-sm">
            <p>
              <strong>Name:</strong> {{ currentUser.firstName && currentUser.lastName
                ? currentUser.firstName + ' ' + currentUser.lastName
                : currentUser.username || 'Guest User' }}
            </p>
            <p>
              <strong>Roles:</strong> {{ userRoles?.join(', ') || 'None' }}
            </p>
          </div>
        </UCard>

        <div class="flex gap-3 justify-center mt-6">
          <UButton
            color="primary"
            variant="solid"
            to="/"
            icon="i-heroicons-home"
          >
            Go Home
          </UButton>
          <UButton
            color="neutral"
            variant="outline"
            to="/login"
            icon="i-heroicons-arrow-right-on-rectangle"
          >
            Login
          </UButton>
        </div>
      </div>
    </UCard>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getCurrentUser, getUserRoles } from '@/common/guards/roleGuard.guard'

const currentUser = computed(() => getCurrentUser())
const userRoles = computed(() => getUserRoles())
</script>
