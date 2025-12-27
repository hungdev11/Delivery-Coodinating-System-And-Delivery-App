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
   */
  const assignAutomatically = async (
    request: AutoAssignmentRequest,
  ): Promise<AutoAssignmentResponse | null> => {
    autoAssigning.value = true
    try {
      // If shipperSessionMap not provided, create sessions for all shippers
      let shipperSessionMap = request.shipperSessionMap
      if (!shipperSessionMap && request.shipperIds && request.shipperIds.length > 0) {
        shipperSessionMap = {}
        // Create sessions for all shippers
        for (const shipperId of request.shipperIds) {
          try {
            const sessionResponse = await createSessionPrepared(shipperId)
            if (sessionResponse.result?.id) {
              shipperSessionMap[shipperId] = sessionResponse.result.id
            }
          } catch (error: any) {
            console.error(`Failed to create session for shipper ${shipperId}:`, error)
            // Continue with other shippers even if one fails
          }
        }
        
        if (Object.keys(shipperSessionMap).length === 0) {
          throw new Error('Không thể tạo sessions cho shippers')
        }
      }

      // Create assignment with shipperSessionMap
      const assignmentRequest: AutoAssignmentRequest = {
        ...request,
        shipperSessionMap,
      }
      const response = await createAutoAssignment(assignmentRequest)
      if (response.result) {
        const stats = response.result.statistics
        toast.add({
          title: 'Gán task tự động thành công',
          description: `Đã tạo assignments cho ${stats.totalParcels || 0} đơn hàng và ${stats.totalShippers || 0} shippers`,
          color: 'success',
        })
        return response.result
      }
      return null
    } catch (error: any) {
      const message = error.response?.data?.message || error.message || 'Không thể gán task tự động'
      toast.add({
        title: 'Lỗi gán task tự động',
        description: message,
        color: 'error',
      })
      throw error
    } finally {
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
