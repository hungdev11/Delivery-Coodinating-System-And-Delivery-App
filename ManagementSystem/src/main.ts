import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ui from '@nuxt/ui/vue-plugin'

import App from './App.vue'
import router from './router'
import { initializeStores } from './common/store/init.store'

import '@/assets/styles/main.css'

// Initialize the app
;(async function initApp() {
  const app = createApp(App)

  app.use(createPinia())
  await initializeStores()
  app.use(ui)
  app.use(router)

  app.mount('#app')
})()
