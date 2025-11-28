import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'
import ui from '@nuxt/ui/vite'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    vueDevTools(),
    ui({
      ui: {
        colors: {
          primary: 'orange',
          secondary: 'amber', // Brown/Amber tone for secondary elements
          neutral: 'slate', // Grayscale for neutral backgrounds, text, borders
        },
      },
    }),
    VitePWA({
      registerType: 'prompt',
      includeAssets: ['favicon.ico', 'pwa-192x192.png', 'pwa-512x512.png'],
      devOptions: {
        enabled: true,
        suppressWarnings: true,
        type: 'module',
      },
      manifest: {
        name: 'ERP - Quản lý đơn hàng',
        short_name: 'ERP',
        description: 'Hệ thống quản lý đơn hàng và giao vận',
        theme_color: '#f97316',
        background_color: '#ffffff',
        display: 'standalone',
        orientation: 'portrait',
        scope: '/',
        start_url: '/',
        icons: [
          {
            src: 'pwa-192x192.png',
            sizes: '192x192',
            type: 'image/png',
          },
          {
            src: 'pwa-512x512.png',
            sizes: '512x512',
            type: 'image/png',
          },
          {
            src: 'pwa-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
      },
      workbox: {
        // Cache patterns: JS, CSS, images, fonts, but exclude HTML files
        globPatterns: ['**/*.{js,css,ico,png,svg,woff2,woff,ttf,eot}'],
        // Do not cache HTML files (index.html, assets/index.html)
        globIgnores: ['**/index.html', '**/assets/index.html'],
        // Remove old cache entries when updating
        cleanupOutdatedCaches: true,
        // Skip waiting and claim clients immediately on update
        // Set to false to allow manual update control
        skipWaiting: false,
        clientsClaim: false,
        // Enable navigation preload for better performance
        navigationPreload: true,
        // Runtime caching rules
        runtimeCaching: [
          // Cache API calls (NetworkFirst)
          {
            urlPattern: /^https:\/\/api\..*/i,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-cache',
              expiration: {
                maxEntries: 100,
                maxAgeSeconds: 60 * 60 * 24, // 24 hours
              },
            },
          },
          // Do not cache upload files - always fetch from network
          {
            urlPattern: /\/upload\/.*/i,
            handler: 'NetworkOnly',
          },
          // Cache assets with versioning (stale while revalidate)
          // This ensures /assets/* files are removed when updated
          {
            urlPattern: /\/assets\/.*/i,
            handler: 'StaleWhileRevalidate',
            options: {
              cacheName: 'assets-cache',
              expiration: {
                maxEntries: 200,
                maxAgeSeconds: 60 * 60 * 24 * 30, // 30 days
              },
              // Force update when version changes
              cacheableResponse: {
                statuses: [0, 200],
              },
            },
          },
        ],
      },
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  define: {
    // Fix for sockjs-client: it expects 'global' to be defined (Node.js thing)
    // In browser, we need to map it to 'window'
    global: 'window',
  },
})
