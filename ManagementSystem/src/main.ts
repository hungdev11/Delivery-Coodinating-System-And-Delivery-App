import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ui from '@nuxt/ui/vue-plugin'
import { useColorMode } from '@vueuse/core'

import App from './App.vue'
import router from './router'
import { initializeStores } from './common/store/init.store'

import '@/assets/styles/main.css'

// Initialize the app
;(async function initApp() {
  // Set default color mode to light (no auto/system detection)
  const colorMode = useColorMode({
    initialValue: 'light',
    storageKey: 'erp-color-mode',
  })
  colorMode.value = 'light'

  const app = createApp(App)

  // Global error handler for unhandled errors
  app.config.errorHandler = (err, instance, info) => {
    console.error('Vue error:', err, info)
    // Prevent the error from propagating further
    return false
  }

  app.use(createPinia())
  await initializeStores()
  app.use(ui)
  app.use(router)

  app.mount('#app')
})()
