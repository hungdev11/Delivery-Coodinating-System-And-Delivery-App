<template>
  <div
    class="min-h-screen bg-gray-50 dark:bg-gray-950 flex flex-col items-center justify-center p-4 pb-6 safe-area-inset"
  >
    <div class="w-full max-w-md flex flex-col items-center space-y-4 sm:space-y-6">
      <UCard class="w-full shadow-xl">
        <template #header>
          <div class="text-center">
            <div class="flex justify-center mb-4">
              <div class="w-14 h-14 sm:w-16 sm:h-16 bg-primary-500 rounded-2xl flex items-center justify-center shadow-lg transition-transform hover:scale-105">
                <UIcon name="i-heroicons-folder-open" class="w-7 h-7 sm:w-8 sm:h-8 text-white" />
              </div>
            </div>
            <h1 class="text-2xl sm:text-3xl font-bold">Đăng nhập</h1>
            <p class="text-xs sm:text-sm text-gray-500 dark:text-gray-400 mt-2">
              Vui lòng nhập thông tin đăng nhập của bạn
            </p>
          </div>
        </template>

        <form @submit.prevent="login" class="space-y-4 sm:space-y-5">
          <UFormField label="Username / Tên đăng nhập">
            <UInput
              id="username"
              v-model="loginForm.username"
              type="text"
              placeholder="Nhập username hoặc tên đăng nhập"
              required
              size="lg"
              class="w-full"
              autocomplete="username"
            />
          </UFormField>

          <UFormField label="Mật khẩu">
            <UInput
              id="password"
              v-model="loginForm.password"
              type="password"
              placeholder="Nhập mật khẩu"
              required
              size="lg"
              class="w-full"
              autocomplete="current-password"
            />
          </UFormField>

          <div class="flex items-center pt-1 pb-2">
            <UCheckbox
              id="rememberMe"
              v-model="loginForm.rememberMe"
              label="Ghi nhớ đăng nhập"
            />
          </div>

          <UButton
            type="submit"
            :disabled="isLoading"
            :loading="isLoading"
            color="primary"
            block
            size="lg"
            class="mt-2 sm:mt-6"
          >
            <span v-if="!isLoading">Đăng nhập</span>
          </UButton>

          <UAlert
            v-if="error"
            color="error"
            variant="soft"
            :title="error"
            icon="i-heroicons-exclamation-circle"
            class="mt-2 sm:mt-4"
          />
        </form>
      </UCard>

      <!-- Copyright Footer -->
      <div class="w-full text-center px-4">
        <p class="text-xs text-gray-500 dark:text-gray-400">
          © {{ currentYear }} ERP Management System. All rights reserved.
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/common/store/auth.store'
import { login as loginAPI } from './api'
import { LoginForm } from './model.type'
import { Info, Warn, ErrorLog, DebugContexts } from '@/common/utils/debug'

const router = useRouter()
const authStore = useAuthStore()
const error = ref('')
const isLoading = ref(false)

// Current year for copyright
const currentYear = computed(() => new Date().getFullYear())

// Form data
const loginForm = ref({
  username: '',
  password: '',
  rememberMe: false,
})

const login = async () => {
  console.log(
    Info(
      'User Login Attempt started',
      {
        username: loginForm.value.username,
        hasPassword: !!loginForm.value.password,
        rememberMe: loginForm.value.rememberMe,
      },
      DebugContexts.AUTH,
    ),
  )

  if (!loginForm.value.username.trim()) {
    console.log(Warn('Login failed - no username provided', {}, DebugContexts.AUTH))
    error.value = 'Vui lòng nhập username hoặc tên đăng nhập'
    return
  }

  if (!loginForm.value.password.trim()) {
    console.log(Warn('Login failed - no password provided', {}, DebugContexts.AUTH))
    error.value = 'Vui lòng nhập mật khẩu'
    return
  }

  isLoading.value = true
  error.value = ''

  try {
    // Create LoginForm instance
    const loginFormData = new LoginForm(
      loginForm.value.username,
      loginForm.value.password,
      loginForm.value.rememberMe,
    )

    // Call API
    const response = await loginAPI(loginFormData)

    // Validate response
    if (!response.result) {
      throw new Error('Invalid login response')
    }

    if (!response.result.accessToken) {
      throw new Error('No access token received')
    }

    if (!response.result.user) {
      throw new Error('No user data received')
    }

    // Store token and user information using Pinia store
    // Use roles from response.user.roles if available, otherwise empty array
    const userRoles = response.result.user.roles || []
    authStore.setAuth(
      response.result.accessToken,
      response.result.user,
      userRoles,
    )

    console.log(
      Info(
        'Login successful',
        {
          user: response.result?.user.username,
          email: response.result?.user.email,
          status: response.result?.user.status,
          tokenType: response.result?.tokenType,
          expiresIn: response.result?.expiresIn,
        },
        DebugContexts.AUTH,
      ),
    )

    router.push('/')
  } catch (err) {
    console.log(ErrorLog('Login failed with exception', err, DebugContexts.AUTH))
    error.value = 'Đăng nhập thất bại: ' + (err as Error).message
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
/* Safe area support for devices with notches/home indicators */
.safe-area-inset {
  padding-bottom: max(1.5rem, env(safe-area-inset-bottom));
  padding-left: max(1rem, env(safe-area-inset-left));
  padding-right: max(1rem, env(safe-area-inset-right));
}
</style>
