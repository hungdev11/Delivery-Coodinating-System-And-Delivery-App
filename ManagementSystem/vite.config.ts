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
        id: '/',
        categories: ['business', 'productivity'],
        icons: [
          {
            src: 'pwa-192x192.png',
            sizes: '192x192',
            type: 'image/png',
            purpose: 'any',
          },
          {
            src: 'pwa-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'any',
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
        // Set to false to allow manual update control (user can choose when to update)
        skipWaiting: false,
        clientsClaim: false,
        // Disable navigation preload to avoid "preloadResponse cancelled" warnings
        // Navigation preload can cause issues when requests are cancelled before preloadResponse settles
        navigationPreload: false,
        // Service worker filename
        swDest: 'sw.js',
        // Maximum file size to precache (2MB - increased to handle large MapView CSS file)
        maximumFileSizeToCacheInBytes: 2 * 1024 * 1024,
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
  build: {
    rollupOptions: {
      output: {
        manualChunks: (id) => {
          // Split maplibre-gl into separate chunk (large library ~200KB)
          if (id.includes('maplibre-gl')) {
            return 'maplibre'
          }

          // Split @turf/turf into separate chunk (geospatial library)
          if (id.includes('@turf/turf') || id.includes('@turf/')) {
            return 'turf'
          }

          // Split @nuxt/ui into separate chunk (UI library)
          if (id.includes('@nuxt/ui') || id.includes('nuxt/ui')) {
            return 'nuxt-ui'
          }

          // Split vue-router into separate chunk
          if (id.includes('vue-router')) {
            return 'vue-router'
          }

          // Split pinia into separate chunk
          if (id.includes('pinia')) {
            return 'pinia'
          }

          // Split axios into separate chunk
          if (id.includes('axios')) {
            return 'axios'
          }

          // Split MapView and related map components into separate chunk
          if (id.includes('MapView') || id.includes('useMap')) {
            return 'map-components'
          }

          // Split large vendor libraries
          if (id.includes('node_modules')) {
            // Group other large libraries
            if (id.includes('@vueuse') || id.includes('vueuse')) {
              return 'vueuse'
            }
            if (id.includes('@stomp/stompjs') || id.includes('sockjs')) {
              return 'websocket'
            }
            if (id.includes('@tanstack/table')) {
              return 'table'
            }
            // Default vendor chunk for other node_modules
            return 'vendor'
          }
        },
      },
    },
    // Increase chunk size warning limit (since we're splitting manually)
    chunkSizeWarningLimit: 1000,
  },
})
