/**
 * Composable for Task Management
 * Handles manual and auto assignment operations
 */

import { ref, computed } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import {
  createManualAssignment,
  createAutoAssignment,
  createSessionPrepared,
  createSessionWithAssignments,
} from '../api'
import type {
  ManualAssignmentRequest,
  AutoAssignmentRequest,
  CreateSessionRequest,
} from '../model.type'
import type {
  ManualAssignmentResponse,
  AutoAssignmentResponse,
  DeliverySessionDto,
} from '../model.type'
import { useProgressTrackerStore, type ProgressTask } from '@/stores/progressTrackerStore'

// Type alias for backward compatibility
type SessionResponse = DeliverySessionDto

export function useTaskManagement() {
  const toast = useToast()
  const loading = ref(false)
  const manualAssigning = ref(false)
  const autoAssigning = ref(false)
  const creatingSession = ref(false)

  /**
   * Create manual assignment
   * Automatically creates session (CREATED status) if not provided
   */
  const assignManually = async (
    request: ManualAssignmentRequest,
  ): Promise<ManualAssignmentResponse | null> => {
    manualAssigning.value = true
    try {
      // If sessionId not provided, create session first
      let sessionId = request.sessionId
      if (!sessionId) {
        const sessionResponse = await createSessionPrepared(request.shipperId)
        if (!sessionResponse.result?.id) {
          throw new Error('Không thể tạo session cho shipper')
        }
        sessionId = sessionResponse.result.id
      }

      // Create assignment with sessionId
      const assignmentRequest: ManualAssignmentRequest = {
        ...request,
        sessionId,
      }
      const response = await createManualAssignment(assignmentRequest)
      if (response.result) {
        toast.add({
          title: 'Gán task thành công',
          description: `Đã gán ${request.parcelIds.length} đơn hàng cho shipper`,
          color: 'success',
        })
        return response.result
      }
      return null
    } catch (error: any) {
      const message = error.response?.data?.message || error.message || 'Không thể gán task'
      toast.add({
        title: 'Lỗi gán task',
        description: message,
        color: 'error',
      })
      throw error
    } finally {
      manualAssigning.value = false
    }
  }

  /**
   * Create auto assignment using VRP solver
   * Automatically creates sessions (CREATED status) for each shipper if not provided
   * Non-blocking: returns immediately, tracks progress in global tracker
   */
  const assignAutomatically = async (
    request: AutoAssignmentRequest,
  ): Promise<void> => {
    autoAssigning.value = true
    
    // Generate task ID
    const taskId = `assignment-${Date.now()}`
    const trackerStore = useProgressTrackerStore()
    
    try {
      // Add task to global tracker
      trackerStore.addTask({
        id: taskId,
        type: 'assignment',
        title: 'VRP Auto Assignment',
        progress: 0,
        status: 'running',
        message: 'Đang tạo sessions cho shippers...',
        details: {
          currentStep: 1,
          totalSteps: 3,
          stepDescription: 'Tạo sessions cho shippers',
        },
        onClose: () => {
          trackerStore.removeTask(taskId)
        },
      })

      // Run assignment process asynchronously
      ;(async () => {
        try {
          // Step 1: Create sessions for shippers
          let shipperSessionMap = request.shipperSessionMap
          if (!shipperSessionMap && request.shipperIds && request.shipperIds.length > 0) {
            trackerStore.updateTask(taskId, {
              progress: 10,
              message: `Đang tạo sessions cho ${request.shipperIds.length} shippers...`,
              details: {
                currentStep: 1,
                totalSteps: 3,
                stepDescription: `Tạo sessions cho ${request.shipperIds.length} shippers`,
              },
            })
            
            shipperSessionMap = {}
            const totalShippers = request.shipperIds.length
            let completedShippers = 0
            
            // Create sessions for all shippers
            for (const shipperId of request.shipperIds) {
              try {
                const sessionResponse = await createSessionPrepared(shipperId)
                if (sessionResponse.result?.id) {
                  shipperSessionMap[shipperId] = sessionResponse.result.id
                }
                completedShippers++
                
                // Update progress
                const progress = 10 + Math.floor((completedShippers / totalShippers) * 30)
                trackerStore.updateTask(taskId, {
                  progress,
                  message: `Đã tạo session cho ${completedShippers}/${totalShippers} shippers...`,
                })
              } catch (error: any) {
                console.error(`Failed to create session for shipper ${shipperId}:`, error)
                completedShippers++
                // Continue with other shippers even if one fails
              }
            }
            
            if (Object.keys(shipperSessionMap).length === 0) {
              throw new Error('Không thể tạo sessions cho shippers')
            }
          }

          // Step 2: Run VRP assignment
          trackerStore.updateTask(taskId, {
            progress: 40,
            message: 'Đang chạy VRP solver...',
            details: {
              currentStep: 2,
              totalSteps: 3,
              stepDescription: 'Chạy VRP solver để phân bổ tối ưu',
            },
          })

          const assignmentRequest: AutoAssignmentRequest = {
            ...request,
            shipperSessionMap,
          }
          
          const response = await createAutoAssignment(assignmentRequest)
          
          // Step 3: Completed
          if (response.result) {
            const stats = response.result.statistics
            trackerStore.updateTask(taskId, {
              progress: 100,
              status: 'completed',
              message: `Hoàn thành: ${stats.totalParcels || 0} đơn hàng, ${stats.totalShippers || 0} shippers`,
              details: {
                currentStep: 3,
                totalSteps: 3,
                stepDescription: 'Hoàn thành',
              },
            })
            
            toast.add({
              title: 'Gán task tự động thành công',
              description: `Đã tạo assignments cho ${stats.totalParcels || 0} đơn hàng và ${stats.totalShippers || 0} shippers`,
              color: 'success',
            })
          } else {
            throw new Error('Không nhận được kết quả từ server')
          }
        } catch (error: any) {
          const message = error.response?.data?.message || error.message || 'Không thể gán task tự động'
          trackerStore.updateTask(taskId, {
            status: 'error',
            message,
          })
          
          toast.add({
            title: 'Lỗi gán task tự động',
            description: message,
            color: 'error',
          })
        } finally {
          autoAssigning.value = false
        }
      })()
    } catch (error: any) {
      const message = error.response?.data?.message || error.message || 'Không thể bắt đầu gán task tự động'
      trackerStore.updateTask(taskId, {
        status: 'error',
        message,
      })
      
      toast.add({
        title: 'Lỗi',
        description: message,
        color: 'error',
      })
      autoAssigning.value = false
    }
  }

  /**
   * Create session with assignments
   */
  const createSession = async (
    request: CreateSessionRequest,
  ): Promise<SessionResponse | null> => {
    creatingSession.value = true
    try {
      const response = await createSessionWithAssignments(request)
      if (response.result) {
        toast.add({
          title: 'Tạo session thành công',
          description: `Đã tạo session với ${request.assignmentsIds.length} assignments`,
          color: 'success',
        })
        return response.result as SessionResponse
      }
      return null
    } catch (error: any) {
      const message = error.response?.data?.message || error.message || 'Không thể tạo session'
      toast.add({
        title: 'Lỗi tạo session',
        description: message,
        color: 'error',
      })
      throw error
    } finally {
      creatingSession.value = false
    }
  }

  return {
    loading: computed(() => manualAssigning.value || autoAssigning.value || creatingSession.value),
    manualAssigning,
    autoAssigning,
    creatingSession,
    assignManually,
    assignAutomatically,
    createSession,
  }
}
