/**
 * Progress Tracker Store
 *
 * Global store for managing background task progress tracking
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
export interface ProgressTask {
  id: string
  type: 'seed' | 'assignment' | 'custom'
  title: string
  progress?: number // 0-100
  status: 'running' | 'completed' | 'error'
  message?: string
  details?: {
    currentStep?: number
    totalSteps?: number
    stepDescription?: string
    [key: string]: any
  }
  onClose?: () => void
}

export const useProgressTrackerStore = defineStore('progressTracker', () => {
  const tasks = ref<ProgressTask[]>([])

  /**
   * Add a new task to track
   */
  const addTask = (task: ProgressTask) => {
    // Remove existing task with same id if exists
    removeTask(task.id)
    tasks.value.push(task)
  }

  /**
   * Update an existing task
   */
  const updateTask = (id: string, updates: Partial<ProgressTask>) => {
    const task = tasks.value.find((t) => t.id === id)
    if (task) {
      Object.assign(task, updates)
    }
  }

  /**
   * Remove a task
   */
  const removeTask = (id: string) => {
    const index = tasks.value.findIndex((t) => t.id === id)
    if (index > -1) {
      tasks.value.splice(index, 1)
    }
  }

  /**
   * Get a task by id
   */
  const getTask = (id: string): ProgressTask | undefined => {
    return tasks.value.find((t) => t.id === id)
  }

  /**
   * Clear all completed/error tasks
   */
  const clearCompleted = () => {
    tasks.value = tasks.value.filter((t) => t.status === 'running')
  }

  return {
    tasks,
    addTask,
    updateTask,
    removeTask,
    getTask,
    clearCompleted,
  }
})
