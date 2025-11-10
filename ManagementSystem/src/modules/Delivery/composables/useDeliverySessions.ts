/**
 * useDeliverySessions Composable
 *
 * Helper functions for loading delivery sessions and assignments
 */

import { ref } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import {
  getActiveAssignmentsForDeliveryMan,
  getAssignmentHistoryForDeliveryMan,
  getDeliverySessionDetail,
  getSessionDemoRoute,
} from '../api'
import { DeliverySessionDto, DeliveryAssignmentDto } from '../model.type'
import type { DeliveryAssignmentTask, AssignmentStatus } from '../model.type'
import type { DemoRouteResponseData } from '@/modules/Zones/routing.type'

export interface SessionRouteResult {
  code: string
  route?: DemoRouteResponseData['route']
  visitOrder?: DemoRouteResponseData['visitOrder']
  summary?: DemoRouteResponseData['summary']
}

export function useDeliverySessions() {
  const toast = useToast()

  const sessions = ref<DeliverySessionDto[]>([])
  const sessionsLoading = ref(false)
  const sessionMap = ref<Map<string, DeliverySessionDto>>(new Map())

  type RawAssignment = Partial<DeliveryAssignmentTask> & {
    id?: string
    assignmentId?: string
    assignmentStatus?: string
    createdAt?: string
    updatedAt?: string
    completedAt?: string
    failReason?: string | null
    scanedAt?: string
  }

  const normalizeAssignment = (assignment: RawAssignment): DeliveryAssignmentDto => {
    const fallbackId =
      assignment.id ??
      assignment.assignmentId ??
      `${assignment.sessionId ?? 'session'}-${assignment.parcelId ?? 'parcel'}`
    const parcelId = assignment.parcelId ?? ''
    const status =
      (assignment.status ??
        assignment.assignmentStatus ??
        'IN_PROGRESS') as AssignmentStatus

    return new DeliveryAssignmentDto({
      id: fallbackId,
      parcelId,
      status,
      failReason: assignment.failReason ?? null,
      scanedAt: assignment.scanedAt ?? assignment.createdAt ?? '',
      updatedAt: assignment.updatedAt ?? assignment.completedAt ?? assignment.createdAt ?? '',
    } as DeliveryAssignmentDto)
  }

  type RawSession = Partial<Omit<DeliverySessionDto, 'assignments'>> & {
    assignments?: RawAssignment[]
    deliveryManAssignedId?: string
    sessionStatus?: string
    createdAt?: string
  }

  const normalizeSession = (data: RawSession): DeliverySessionDto => {
    const assignments = Array.isArray(data.assignments)
      ? data.assignments.map(normalizeAssignment)
      : []

    const totalTasks = data.totalTasks ?? assignments.length
    const completedTasks =
      data.completedTasks ??
      assignments.filter((assignment) => assignment.status === 'COMPLETED').length
    const failedTasks =
      data.failedTasks ?? assignments.filter((assignment) => assignment.status === 'FAILED').length

    const sessionIdValue =
      data.id ?? (assignments.length > 0 ? `${assignments[0].id}-session` : 'unknown-session')
    const deliveryManId =
      data.deliveryManId ?? data.deliveryManAssignedId ?? 'unknown-delivery-man'

    const normalized = new DeliverySessionDto({
      id: sessionIdValue,
      deliveryManId,
      status: data.status ?? data.sessionStatus ?? 'IN_PROGRESS',
      startTime: data.startTime ?? data.createdAt,
      endTime: data.endTime,
      totalTasks,
      completedTasks,
      failedTasks,
      assignments,
    } as DeliverySessionDto)

    return normalized
  }

  const fetchSession = async (sessionId: string): Promise<DeliverySessionDto | null> => {
    try {
      const response = await getDeliverySessionDetail(sessionId)
      const raw = (response as { result?: RawSession })?.result ?? (response as RawSession)
      if (!raw) return null
      return normalizeSession(raw)
    } catch (error) {
      console.error('Failed to fetch session detail:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to fetch session detail',
        color: 'error',
      })
      return null
    }
  }

  const loadSessions = async (deliveryManId: string) => {
    sessionsLoading.value = true
    try {
      const [activeTasks, historyTasks] = await Promise.all([
        getActiveAssignmentsForDeliveryMan(deliveryManId, { page: 0, size: 50 }),
        getAssignmentHistoryForDeliveryMan(deliveryManId, { page: 0, size: 50 }),
      ])

      const taskGroups = new Map<string, DeliveryAssignmentTask[]>()

      const collect = (tasks?: DeliveryAssignmentTask[]) => {
        tasks?.forEach((task) => {
          if (!task.sessionId) return
          if (!taskGroups.has(task.sessionId)) {
            taskGroups.set(task.sessionId, [])
          }
          taskGroups.get(task.sessionId)!.push(task)
        })
      }

      collect(activeTasks?.content)
      collect(historyTasks?.content)

      const uniqueSessionIds = Array.from(taskGroups.keys())
      const fetchedSessions = await Promise.all(uniqueSessionIds.map(fetchSession))
      const validSessions = fetchedSessions.filter(
        (session): session is DeliverySessionDto => session !== null,
      )

      validSessions.forEach((session) => {
        sessionMap.value.set(session.id, session)
      })

      sessions.value = Array.from(sessionMap.value.values()).sort((a, b) => {
        return new Date(b.startTime).getTime() - new Date(a.startTime).getTime()
      })
    } finally {
      sessionsLoading.value = false
    }
  }

  const getSessionById = (sessionId: string): DeliverySessionDto | undefined => {
    return sessionMap.value.get(sessionId)
  }

  const loadSessionById = async (sessionId: string): Promise<DeliverySessionDto | null> => {
    const existing = sessionMap.value.get(sessionId)
    if (existing) {
      return existing
    }
    const session = await fetchSession(sessionId)
    if (session) {
      sessionMap.value.set(session.id, session)
      sessions.value = Array.from(sessionMap.value.values())
    }
    return session ?? null
  }

  const loadSessionRoute = async (sessionId: string): Promise<SessionRouteResult | null> => {
    try {
      const response = await getSessionDemoRoute(sessionId)
      const resultWrapper = response as { result?: SessionRouteResult }
      const payload = resultWrapper.result ?? (response as SessionRouteResult | null)
      return payload ?? null
    } catch (error) {
      console.error('Failed to load session route:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to load session route',
        color: 'error',
      })
      return null
    }
  }

  return {
    sessions,
    sessionsLoading,
    loadSessions,
    getSessionById,
    loadSessionById,
    loadSessionRoute,
  }
}
