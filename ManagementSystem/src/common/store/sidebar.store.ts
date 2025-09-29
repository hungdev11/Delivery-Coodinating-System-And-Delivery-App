 import { defineStore } from 'pinia'
import { ref } from 'vue'

const SIDEBAR_COLLAPSED_KEY = 'sidebar_collapsed'

export const useSidebarStore = defineStore('sidebar', () => {
  // Initialize from localStorage or default to false
  const getInitialCollapsedState = (): boolean => {
    try {
      const stored = localStorage.getItem(SIDEBAR_COLLAPSED_KEY)
      return stored ? JSON.parse(stored) : false
    } catch (error) {
      console.warn('Failed to parse sidebar collapsed state from localStorage:', error)
      return false
    }
  }

  const isCollapsed = ref(getInitialCollapsedState())

  // Save to localStorage whenever the state changes
  const saveToLocalStorage = (collapsed: boolean) => {
    try {
      localStorage.setItem(SIDEBAR_COLLAPSED_KEY, JSON.stringify(collapsed))
    } catch (error) {
      console.warn('Failed to save sidebar collapsed state to localStorage:', error)
    }
  }

  const toggleSidebar = () => {
    isCollapsed.value = !isCollapsed.value
    saveToLocalStorage(isCollapsed.value)
  }

  const expandSidebar = () => {
    isCollapsed.value = false
    saveToLocalStorage(isCollapsed.value)
  }

  const collapseSidebar = () => {
    isCollapsed.value = true
    saveToLocalStorage(isCollapsed.value)
  }

  return {
    isCollapsed,
    toggleSidebar,
    expandSidebar,
    collapseSidebar,
  }
})
