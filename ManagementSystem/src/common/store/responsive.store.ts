/**
 * Responsive Store (Pinia)
 *
 * Global store for responsive state management
 * Optimizes performance by centralizing window resize listeners
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useResponsiveStore = defineStore('responsive', () => {
  const windowWidth = ref<number>(typeof window !== 'undefined' ? window.innerWidth : 1024)
  const windowHeight = ref<number>(typeof window !== 'undefined' ? window.innerHeight : 768)

  // Breakpoints
  const isMobile = computed(() => windowWidth.value < 640) // sm
  const isTablet = computed(() => windowWidth.value >= 640 && windowWidth.value < 1024) // md
  const isDesktop = computed(() => windowWidth.value >= 1024) // lg
  const isAndroid = computed(() => {
    if (typeof navigator === 'undefined') return false
    return /Android/i.test(navigator.userAgent)
  })
  const isIOS = computed(() => {
    if (typeof navigator === 'undefined') return false
    const ua = navigator.userAgent
    return /iPad|iPhone|iPod/.test(ua) && !(window as unknown as { MSStream?: unknown }).MSStream
  })

  // Update window dimensions
  const updateDimensions = () => {
    if (typeof window === 'undefined') return
    windowWidth.value = window.innerWidth
    windowHeight.value = window.innerHeight
  }

  // Setup resize listener (only once globally)
  let resizeListenerAttached = false
  const attachResizeListener = () => {
    if (resizeListenerAttached || typeof window === 'undefined') return
    window.addEventListener('resize', updateDimensions, { passive: true })
    resizeListenerAttached = true
    // Initialize on mount
    updateDimensions()
  }

  const detachResizeListener = () => {
    if (!resizeListenerAttached || typeof window === 'undefined') return
    window.removeEventListener('resize', updateDimensions)
    resizeListenerAttached = false
  }

  return {
    // State
    windowWidth,
    windowHeight,

    // Computed
    isMobile,
    isTablet,
    isDesktop,
    isAndroid,
    isIOS,

    // Actions
    updateDimensions,
    attachResizeListener,
    detachResizeListener,
  }
})

